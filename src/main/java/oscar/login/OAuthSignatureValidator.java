package oscar.login;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.Logger;
import org.oscarehr.app.AppOAuth1Config;
import org.oscarehr.util.MiscUtils;
import org.springframework.stereotype.Component;

import com.github.scribejava.core.services.HMACSha1SignatureService;
import com.github.scribejava.core.services.SignatureService;
import com.github.scribejava.core.utils.OAuthEncoder;

/**
 * Validates OAuth 1.0a signatures using ScribeJava.
 */
@Component
public class OAuthSignatureValidator {

    private static final Logger logger = MiscUtils.getLogger();

    public boolean validateSignature(HttpServletRequest request, AppOAuth1Config config) {
        try {
            Map<String, String> oauthParams = extractOAuthParameters(request);

            String receivedSignature = oauthParams.get("oauth_signature");
            if (receivedSignature == null) {
                logger.warn("Missing oauth_signature parameter");
                return false;
            }

            // Remove signature for base string construction
            oauthParams.remove("oauth_signature");

            // Add any other query/form parameters
            request.getParameterMap().forEach((key, values) -> {
                if (!key.startsWith("oauth_") && values.length > 0) {
                    oauthParams.put(key, values[0]);
                }
            });

            // Build the base string
            String method = request.getMethod().toUpperCase();
            String url = buildBaseUrl(request);
            String baseString = method + "&" +
                    OAuthEncoder.encode(url) + "&" +
                    OAuthEncoder.encode(encodeSortedParams(oauthParams));

            logger.debug("OAuth base string: {}", baseString);

            // Use ScribeJava's HMAC-SHA1 signature service
            SignatureService signatureService = new HMACSha1SignatureService();
            String expectedSignature = signatureService.getSignature(
                    baseString,
                    config.getConsumerSecret(),
                    "" // Token secret is blank at request token stage
            );

            boolean valid = expectedSignature.equals(receivedSignature);

            if (!valid) {
                logger.warn("Invalid OAuth signature. Expected: {}, Got: {}", expectedSignature, receivedSignature);
            } else {
                logger.debug("OAuth signature validated successfully.");
            }

            return valid;

        } catch (Exception e) {
            logger.error("Error validating OAuth signature: {}", e.getMessage(), e);
            return false;
        }
    }

    private Map<String, String> extractOAuthParameters(HttpServletRequest request) {
        Map<String, String> params = new TreeMap<>();

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("OAuth ")) {
            parseAuthorizationHeader(authHeader, params);
        }

        // Include any direct parameters already in the query or form
        request.getParameterMap().forEach((key, values) -> {
            if (key.startsWith("oauth_") && values.length > 0) {
                params.put(key, values[0]);
            }
        });

        return params;
    }

    private void parseAuthorizationHeader(String authHeader, Map<String, String> params) {
        String[] pairs = authHeader.substring(6).split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.trim().split("=", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = keyValue[1].replaceAll("^\"|\"$", "");
                params.put(key, value);
            }
        }
    }

    private String buildBaseUrl(HttpServletRequest request) throws URISyntaxException {
        String scheme = request.isSecure() ? "https" : "http";
        String host = request.getServerName();
        int port = request.getServerPort();
        String path = request.getRequestURI();

        StringBuilder sb = new StringBuilder();
        sb.append(scheme).append("://").append(host);

        if ((scheme.equals("http") && port != 80) || (scheme.equals("https") && port != 443)) {
            sb.append(":").append(port);
        }

        sb.append(path);
        return new URI(sb.toString()).normalize().toString();
    }

    private String encodeSortedParams(Map<String, String> params) {
    return params.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(entry -> OAuthEncoder.encode(entry.getKey()) + "=" + OAuthEncoder.encode(entry.getValue()))
        .reduce((a, b) -> a + "&" + b)
        .orElse("");
}

}
