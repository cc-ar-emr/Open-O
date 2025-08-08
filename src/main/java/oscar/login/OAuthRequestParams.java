package oscar.login;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

/**
 * Handles parsing and validation of OAuth request parameters.
 */
public class OAuthRequestParams {

    public static final String SIGNATURE_METHOD_HMAC_SHA1 = "HMAC-SHA1";
    public static final String SIGNATURE_METHOD_RSA_SHA1 = "RSA-SHA1";
    public static final String SIGNATURE_METHOD_PLAINTEXT = "PLAINTEXT";


    private final String consumerKey;
    private final String callbackUrl;
    private final String timestamp;
    private final String nonce;
    private final String signature;
    private final String signatureMethod;
    private final String version;

    private OAuthRequestParams(String consumerKey, String callbackUrl, String timestamp, 
                              String nonce, String signature, String signatureMethod, String version) {
        this.consumerKey = consumerKey;
        this.callbackUrl = callbackUrl;
        this.timestamp = timestamp;
        this.nonce = nonce;
        this.signature = signature;
        this.signatureMethod = signatureMethod;
        this.version = version;
    }

    /**
     * Parses and validates OAuth parameters from the HTTP request.
     * 
     * @param request The HTTP servlet request
     * @return OAuthRequestParams if the parameters are valid
     * @throws OAuthParameterException if required parameters are missing or invalid
     */
    public static OAuthRequestParams parseAndValidate(HttpServletRequest request) 
            throws OAuthParameterException {
        
        // Extract all OAuth parameters
        String consumerKey = getParameter(request, "oauth_consumer_key");
        String callbackUrl = getParameter(request, "oauth_callback");
        String timestamp = getParameter(request, "oauth_timestamp");
        String nonce = getParameter(request, "oauth_nonce");
        String signature = getParameter(request, "oauth_signature");
        String signatureMethod = getParameter(request, "oauth_signature_method");
        String version = getParameter(request, "oauth_version");

        // Validate required parameters
        if (consumerKey == null || consumerKey.trim().isEmpty()) {
            throw new OAuthParameterException(Response.Status.BAD_REQUEST,
                    "parameter_absent", "oauth_consumer_key");
        }

        if (callbackUrl == null || callbackUrl.trim().isEmpty()) {
            throw new OAuthParameterException(Response.Status.BAD_REQUEST,
                    "parameter_absent", "oauth_callback");
        }
        
        if (timestamp == null || timestamp.trim().isEmpty()) {
            throw new OAuthParameterException(Response.Status.BAD_REQUEST,
                    "parameter_absent", "oauth_timestamp");
        }
        
        if (nonce == null || nonce.trim().isEmpty()) {
            throw new OAuthParameterException(Response.Status.BAD_REQUEST,
                    "parameter_absent", "oauth_nonce");
        }
        
        if (signature == null || signature.trim().isEmpty()) {
            throw new OAuthParameterException(Response.Status.BAD_REQUEST,
                    "parameter_absent", "oauth_signature");
        }
        
        if (signatureMethod == null || signatureMethod.trim().isEmpty()) {
            throw new OAuthParameterException(Response.Status.BAD_REQUEST,
                    "parameter_absent", "oauth_signature_method");
        }
        
        // Validate signature method
        if (!SIGNATURE_METHOD_HMAC_SHA1.equals(signatureMethod) && 
            !SIGNATURE_METHOD_RSA_SHA1.equals(signatureMethod) && 
            !SIGNATURE_METHOD_PLAINTEXT.equals(signatureMethod)) {
            throw new OAuthParameterException(Response.Status.BAD_REQUEST,
                    "signature_method_rejected", signatureMethod);
        }
        
        // Validate OAuth version (should be 1.0)
        if (version != null && !"1.0".equals(version)) {
            throw new OAuthParameterException(Response.Status.BAD_REQUEST,
                    "version_rejected", version);
        }

        return new OAuthRequestParams(consumerKey, callbackUrl, timestamp, nonce, 
                                    signature, signatureMethod, version);
    }
    
    /**
     * Gets parameter from request, checking both query parameters and Authorization header.
     */
    private static String getParameter(HttpServletRequest request, String paramName) {
        // First check regular parameters
        String value = request.getParameter(paramName);
        if (value != null) {
            return value;
        }
        
        // Check Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("OAuth ")) {
            return extractFromAuthHeader(authHeader, paramName);
        }
        
        return null;
    }
    
    /**
     * Extracts parameter from OAuth Authorization header.
     */
    private static String extractFromAuthHeader(String authHeader, String paramName) {
        String oauthParams = authHeader.substring(6); // Remove "OAuth "
        String[] pairs = oauthParams.split(",");
        
        for (String pair : pairs) {
            String[] keyValue = pair.trim().split("=", 2);
            if (keyValue.length == 2 && keyValue[0].trim().equals(paramName)) {
                String value = keyValue[1].trim();
                // Remove quotes if present
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                return value;
            }
        }
        
        return null;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public String getNonce() {
        return nonce;
    }
    
    public String getSignature() {
        return signature;
    }
    
    public String getSignatureMethod() {
        return signatureMethod;
    }
    
    public String getVersion() {
        return version;
    }

    /**
     * Exception thrown when OAuth parameter validation fails.
     */
    public static class OAuthParameterException extends Exception {
        private final Response.Status status;
        private final String problem;
        private final String advice;

        public OAuthParameterException(Response.Status status, String problem, String advice) {
            super("OAuth parameter validation failed: " + problem);
            this.status = status;
            this.problem = problem;
            this.advice = advice;
        }

        public Response.Status getStatus() { return status; }
        public String getProblem() { return problem; }
        public String getAdvice() { return advice; }
    }
}
