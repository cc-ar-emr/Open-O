package oscar.login;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.Logger;
import org.oscarehr.util.MiscUtils;
import org.springframework.stereotype.Component;

/**
 * Provides core OAuth 1.0a security validations including:
 * - Timestamp freshness checks
 * - Nonce reuse prevention
 * - HTTPS enforcement (including reverse proxy support)
 */
@Component
public class OAuthSecurityValidator {

    private static final Logger logger = MiscUtils.getLogger();

    // Maximum allowed age of timestamps (5 minutes)
    private static final long MAX_TIMESTAMP_AGE_MS = 5 * 60 * 1000;

    // Interval for nonce cleanup (in minutes)
    private static final int CLEANUP_INTERVAL_MINUTES = 10;

    private final ConcurrentHashMap<String, Long> usedNonces = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public OAuthSecurityValidator() {
        scheduler.scheduleAtFixedRate(this::cleanupExpiredNonces,
            CLEANUP_INTERVAL_MINUTES,
            CLEANUP_INTERVAL_MINUTES,
            TimeUnit.MINUTES);
    }

    /**
     * Validates OAuth timestamp and nonce to prevent replay attacks.
     *
     * @param timestamp   The OAuth timestamp (epoch seconds)
     * @param nonce       The OAuth nonce value
     * @param consumerKey The consumer key (used for namespacing and logging)
     * @return true if valid, false if stale or reused
     */
    public boolean validateTimestampAndNonce(String timestamp, String nonce, String consumerKey) {
        if (timestamp == null || nonce == null) {
            logger.warn("Missing timestamp or nonce for consumer: {}", consumerKey);
            return false;
        }

        long timestampMs;
        try {
            timestampMs = Long.parseLong(timestamp) * 1000;
        } catch (NumberFormatException e) {
            logger.warn("Invalid timestamp format '{}' for consumer: {}", timestamp, consumerKey);
            return false;
        }

        long now = System.currentTimeMillis();
        long age = Math.abs(now - timestampMs);

        if (age > MAX_TIMESTAMP_AGE_MS) {
            logger.warn("Timestamp too old or in the future ({}ms) for consumer: {}", age, consumerKey);
            return false;
        }

        String nonceKey = consumerKey + ":" + nonce;
        if (usedNonces.putIfAbsent(nonceKey, timestampMs) != null) {
            logger.warn("Nonce '{}' reused for consumer: {} (possible replay)", nonce, consumerKey);
            return false;
        }

        logger.debug("Timestamp and nonce validated for consumer: {}", consumerKey);
        return true;
    }

    /**
     * Ensures the request was received over HTTPS, considering proxy headers.
     *
     * @param request The incoming HTTP request
     * @return true if HTTPS is used or enforced by proxy headers
     */
    public boolean validateHttps(HttpServletRequest request) {
        if (request.isSecure()) return true;

        String proto = request.getHeader("X-Forwarded-Proto");
        String sslHeader = request.getHeader("X-Forwarded-SSL");

        boolean isForwardedSecure = "https".equalsIgnoreCase(proto) || "on".equalsIgnoreCase(sslHeader);

        if (!isForwardedSecure) {
            logger.warn("Insecure OAuth request received from {}", request.getRemoteAddr());
            return false;
        }

        return true;
    }

    /**
     * Removes expired nonce entries to prevent memory leaks.
     */
    private void cleanupExpiredNonces() {
        long cutoff = System.currentTimeMillis() - MAX_TIMESTAMP_AGE_MS;
        int removed = 0;

        for (var entry : usedNonces.entrySet()) {
            if (entry.getValue() < cutoff) {
                usedNonces.remove(entry.getKey());
                removed++;
            }
        }

        if (removed > 0) {
            logger.debug("Cleaned up {} expired nonces", removed);
        }
    }

    /**
     * Ensures graceful shutdown of the scheduler.
     */
    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
    }
}
