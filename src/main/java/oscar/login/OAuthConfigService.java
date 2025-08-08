// src/main/java/oscar/login/OAuthConfigService.java
package oscar.login;

import java.io.IOException;
import java.net.URI;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.oscarehr.app.AppOAuth1Config;
import org.oscarehr.common.dao.AppDefinitionDao;
import org.oscarehr.common.model.AppDefinition;
import org.oscarehr.util.MiscUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OAuthConfigService {
    private static final Logger logger = MiscUtils.getLogger();

    @Autowired
    private AppDefinitionDao appDefinitionDao;

    /**
     * Retrieves OAuth application configuration for the given consumer key.
     * 
     * @param consumerKey The OAuth consumer key
     * @return AppOAuth1Config or null if not found
     */
    public AppOAuth1Config getOAuthConfiguration(String consumerKey) {
        try {
            AppDefinition app = appDefinitionDao.findByConsumerKey(consumerKey);
            if (app == null) {
                logger.warn("No OAuth configuration found for consumer key: {}", consumerKey);
                return null;
            }
            
            String configStr = app.getConfig();
            if (configStr == null) {
                logger.error("Configuration document missing for app id {}", app.getId());
                return null;
            }
            
            AppOAuth1Config cfg;
            try {
                cfg = AppOAuth1Config.fromDocument(configStr);
            } catch (Exception e) {
                logger.error("Failed to parse OAuth config for app id {}: {}", app.getId(), e.getMessage());
                return null;
            }
            if (cfg == null
             || cfg.getConsumerKey() == null
             || cfg.getConsumerSecret() == null
             || cfg.getBaseURL() == null) {
                logger.error("Invalid OAuth config in app id {}", app.getId());
                return null;
            }
            
            return cfg;
        } catch (Exception e) {
            logger.error("Error retrieving OAuth configuration for consumer key: {}", consumerKey, e);
            return null;
        }
    }

    /**
     * Loads the OAuth configuration for the given application.
     * If the configuration is missing or invalid, it sends an error response.
     * 
     * @param app  The application definition containing the configuration
     * @param resp The HTTP response to send errors if configuration is invalid
     * @return The parsed AppOAuth1Config or null if an error occurs
     * @throws IOException If an error occurs while sending the error response
     */
    public AppOAuth1Config loadConfig(AppDefinition app, HttpServletResponse resp) throws IOException {
        String configStr = app.getConfig();
        if (configStr == null) {
            logger.error("Configuration document missing for app id {}", app.getId());
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Configuration retrieval error");
            return null;
        }
        AppOAuth1Config cfg;
        try {
            cfg = AppOAuth1Config.fromDocument(configStr);
        } catch (Exception e) {
            logger.error("Failed to parse OAuth config for app id {}: {}", app.getId(), e.getMessage());
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Configuration parsing error");
            return null;
        }
        if (cfg == null
         || cfg.getConsumerKey() == null
         || cfg.getConsumerSecret() == null
         || cfg.getBaseURL() == null) {
            logger.error("Invalid OAuth config in app id {}", app.getId());
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid OAuth configuration");
            return null;
        }
        return cfg;
    }

    /**
     * Validates the callback URL against the app's registered base URL.
     * Allows "oob" for out-of-band flows. Otherwise, ensures the callback is rooted at the baseURL.
     */
    public boolean isValidCallback(AppOAuth1Config config, String callbackUrl) {
        if ("oob".equalsIgnoreCase(callbackUrl)) {
            return true;
        }

        if (callbackUrl == null || config.getBaseURL() == null) {
            return false;
        }

        try {
            URI callbackUri = new URI(callbackUrl);
            URI baseUri = new URI(config.getBaseURL());

            boolean matchesHost = callbackUri.getHost().equalsIgnoreCase(baseUri.getHost());
            boolean matchesScheme = callbackUri.getScheme().equalsIgnoreCase(baseUri.getScheme());

            return matchesHost && matchesScheme;

        } catch (Exception e) {
            logger.warn("Invalid callback URI: {}", callbackUrl, e);
            return false;
        }
    }


    /**
     * Validates the callback URL against the registered application configuration.
     * If the URL is valid, it returns true; otherwise, it sends an error response.
     */
    public boolean validateRegisteredCallback(AppOAuth1Config cfg, String callbackUrl, HttpServletResponse resp) throws IOException {
        if (isValidCallback(cfg, callbackUrl)) {
            return true;
        }
        logger.error("Callback URL '{}' is not allowed by configuration", callbackUrl);
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid callback URL");
        return false;
    }

    /**
     * Validates the format of the callback URL.
     * If the URL is invalid, it sends an error response.
     *
     * @param callbackUrl the callback URL to validate
     * @param resp the HTTP response to send an error message if validation fails
     * @return true if the callback URL is valid, false otherwise
     * @throws IOException if an I/O error occurs while sending the error response
     */
    public boolean validateCallbackFormat(String callbackUrl, HttpServletResponse resp) throws IOException {
        try {
            URI uri = new URI(callbackUrl);
            uri.toURL();
            return true;
        } catch (Exception e) {
            logger.error("Invalid callback URL: {}", callbackUrl);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid callback URL");
            return false;
        }
    }

    /**
     * Validates the application definition.
     * If the app is null, it sends an error response.
     *
     * @param app  the application definition to validate
     * @param resp the HTTP response to send an error message if validation fails
     * @return true if the application definition is valid; false otherwise
     * @throws IOException if an I/O error occurs while sending the error response
     */
    public boolean validateApp(AppDefinition app, HttpServletResponse resp) throws IOException {
        if (app == null) {
            logger.error("AppDefinition is null");
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Missing application configuration");
            return false;
        }
        return true;
    }

}
