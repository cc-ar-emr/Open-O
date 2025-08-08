// src/main/java/oscar/login/OAuthServiceFactory.java
package oscar.login;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.oscarehr.app.AppOAuth1Config;
import org.oscarehr.util.MiscUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi10a;
import com.github.scribejava.core.oauth.OAuth10aService;

/**
 * Factory for creating OAuth 1.0a services using ScribeJava.
 */
@Component
public class OAuthServiceFactory {
    private static final Logger logger = MiscUtils.getLogger();

    /**
     * Builds an OAuth 1.0a service using the provided configuration.
     * 
     * @param config OAuth application configuration
     * @param callbackUrl The callback URL for this request
     * @param response HTTP response for error handling
     * @return Configured OAuth10aService or null if error
     */
    public OAuth10aService buildService(AppOAuth1Config config, String callbackUrl, HttpServletResponse response) {
        try {
            return new ServiceBuilder(config.getConsumerKey())
                    .apiSecret(config.getConsumerSecret())
                    .callback(callbackUrl)
                    .build(new DefaultApi10a() {
                        @Override
                        public String getRequestTokenEndpoint() {
                            return config.getBaseURL() + "/oauth/request_token";
                        }

                        @Override
                        public String getAccessTokenEndpoint() {
                            return config.getBaseURL() + "/oauth/access_token";
                        }

                        @Override
                        public String getAuthorizationBaseUrl() {
                            return config.getBaseURL() + "/oauth/authorize";
                        }
                    });
        } catch (Exception e) {
            logger.error("Failed to build OAuth service: {}", e.getMessage(), e);
            try {
                if (!response.isCommitted()) {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                        "OAuth service configuration error");
                }
            } catch (IOException ioException) {
                logger.error("Failed to send error response: {}", ioException.getMessage());
            }
            return null;
        }
    }
}
