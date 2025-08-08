//CHECKSTYLE:OFF
/**
 * Copyright (c) 2001-2002. Department of Family Medicine, McMaster University. All Rights Reserved.
 * This software is published under the GPL GNU General Public License.
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * <p>
 * This software was written for the
 * Department of Family Medicine
 * McMaster University
 * Hamilton
 * Ontario, Canada
 * 
 * Migrated from Apache CXF to ScribeJava OAuth1 implementation.
 */
package oscar.login;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.Logger;
import org.oscarehr.app.AppOAuth1Config;
import org.oscarehr.util.MiscUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi10a;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.oauth.OAuth10aService;

import oscar.login.OAuthRequestParams.OAuthParameterException;

/**
 * OAuth 1.0a Request Token Service using ScribeJava.
 * 
 * This REST service issues temporary request tokens to clients which will be
 * later authorized and exchanged for access tokens. It replaces the CXF-based
 * OAuth implementation with ScribeJava.
 */
@Component
@Path("/initiate")
public class OscarRequestTokenService {

    private static final Logger logger = MiscUtils.getLogger();

    @Autowired
    private OscarOAuthDataProvider dataProvider;

    @Autowired
    private OAuthConfigService configService;

    /**
     * Handles GET requests for OAuth 1.0a request tokens.
     * 
     * @param request HTTP servlet request
     * @param response HTTP servlet response
     * @return Response containing the request token or error
     */
    @GET
    @Produces("application/x-www-form-urlencoded")
    public Response getRequestTokenWithGET(@Context HttpServletRequest request, 
                                          @Context HttpServletResponse response) {
        return getRequestToken(request, response);
    }

    /**
     * Handles POST requests for OAuth 1.0a request tokens.
     * 
     * This method implements the OAuth 1.0a request token endpoint as specified in RFC 5849.
     * It validates the consumer key, generates a temporary request token, stores it, and
     * returns it to the client in form-encoded format.
     * 
     * @param request HTTP servlet request
     * @param response HTTP servlet response
     * @return Response containing the request token or error
     */
    @POST
    @Produces("application/x-www-form-urlencoded")
    public Response getRequestToken(@Context HttpServletRequest request, 
                                   @Context HttpServletResponse response) {
        try {
            logger.info("Processing OAuth 1.0a request token request");

            // Parse & validate parameters
            OAuthRequestParams params = OAuthRequestParams.parseAndValidate(request);

            // Fetch configuration
            AppOAuth1Config appConfig = configService.getOAuthConfiguration(params.getConsumerKey());
            if (appConfig == null) {
                logger.warn("Unknown consumer key: {}", params.getConsumerKey());
                return buildErrorResponse(Response.Status.UNAUTHORIZED, 
                    "consumer_key_unknown", null);
            }

            // Validate callback URL against registered callback
            if (!configService.isValidCallback(appConfig, params.getCallbackUrl())) {
                logger.warn("Invalid callback URL: {} for consumer: {}", 
                    params.getCallbackUrl(), params.getConsumerKey());
                return buildErrorResponse(Response.Status.BAD_REQUEST, 
                    "parameter_rejected", "oauth_callback");
            }

            // Invoke ScribeJava logic
            OAuth10aService service = buildOAuthService(appConfig, params.getCallbackUrl());
            OAuth1RequestToken requestToken = service.getRequestToken();
            
            if (requestToken == null || requestToken.getToken() == null) {
                logger.error("Failed to generate request token for consumer: {}", params.getConsumerKey());
                return buildErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                    "internal_error", null);
            }

            // Store the request token 
            // null is passed here for scopes as AppOAuth1Config does not have a getScopes method
            // Reason: oauth1.0a does not typically use scopes like OAuth2, but this can be extended if needed
            dataProvider.createRequestToken(
                requestToken.getToken(), 
                requestToken.getTokenSecret(), 
                null // scopes - AppOAuth1Config doesn't have getScopes method (since OAuth1 doesn't use scopes)
            );

            // Build and return the response
            String responseBody = String.format(
                "oauth_token=%s&oauth_token_secret=%s&oauth_callback_confirmed=true",
                requestToken.getToken(),
                requestToken.getTokenSecret()
            );

            logger.info("Successfully issued request token: {} for consumer: {}", 
                requestToken.getToken(), params.getConsumerKey());

            return Response.ok(responseBody)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        } catch (OAuthParameterException e) {
            logger.warn("Parameter validation failed: {}", e.getMessage());
            return buildErrorResponse(e.getStatus(), e.getProblem(), e.getAdvice());
        } catch (IOException | InterruptedException | ExecutionException e) {
            logger.error("Error processing request token: {}", e.getMessage(), e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return buildErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                "internal_error", null);
        } catch (Exception e) {
            logger.error("Unexpected error processing request token: {}", e.getMessage(), e);
            return buildErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                "internal_error", null);
        }
    }

    /**
     * Builds the OAuth 1.0a service using ScribeJava.
     * 
     * @param appConfig OAuth application configuration
     * @param callbackUrl The callback URL for this request
     * @return Configured OAuth10aService
     */
    private OAuth10aService buildOAuthService(AppOAuth1Config appConfig, String callbackUrl) {
        return new ServiceBuilder(appConfig.getConsumerKey())
                .apiSecret(appConfig.getConsumerSecret())
                .callback(callbackUrl)
                .build(new DefaultApi10a() {
                    @Override
                    public String getRequestTokenEndpoint() {
                        return appConfig.getBaseURL() + "/oauth/request_token";
                    }

                    @Override
                    public String getAccessTokenEndpoint() {
                        return appConfig.getBaseURL() + "/oauth/access_token";
                    }

                    @Override
                    public String getAuthorizationBaseUrl() {
                        return appConfig.getBaseURL() + "/oauth/authorize";
                    }
                });
    }

    /**
     * Builds an OAuth error response in the standard format.
     * 
     * @param status HTTP status code
     * @param problem OAuth problem identifier
     * @param advice Additional problem advice (optional)
     * @return Response with OAuth error
     */
    private Response buildErrorResponse(Response.Status status, String problem, String advice) {
        StringBuilder errorBody = new StringBuilder();
        errorBody.append("oauth_problem=").append(problem);
        
        if (advice != null && !advice.isEmpty()) {
            errorBody.append("&oauth_problem_advice=").append(advice);
        }

        return Response.status(status)
            .entity(errorBody.toString())
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build();
    }

}
