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
 */
package ca.openosp.openo.integration.oneId;

import ca.openosp.openo.commn.model.SystemPreferences.ONEID_KEYS;
import ca.openosp.openo.managers.EhrConnectivityManager;
import ca.openosp.openo.test.unit.OpenOUnitTestBase;
import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the id_token security gate: a valid token yields its subject, and every tampered, forged,
 * expired, or mismatched token is rejected. A local HTTP server publishes a JWKS built from a test
 * key so the real fetch/parse/verify path runs end to end.
 *
 * @since 2026-07-02
 */
@Tag("unit")
class OneIdJwksProviderUnitTest extends OpenOUnitTestBase {

    private static final String ISSUER = "https://oneid.example.ontariohealth.ca";
    private static final String AUDIENCE = "openo-test-client";
    private static final String SUBJECT = "oneid-subject-0001";
    private static final String NONCE = "login-nonce-abc123";
    private static final String KID = "test-key-1";

    private static KeyPair signingKeyPair;
    private static KeyPair attackerKeyPair;
    private static HttpServer jwksServer;
    private static String jwksUrl;
    private static final AtomicInteger jwksFetches = new AtomicInteger();

    private EhrConnectivityManager ehrConnectivityManager;
    private OneIdJwksProvider provider;

    @BeforeAll
    static void startJwksServer() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        signingKeyPair = generator.generateKeyPair();
        attackerKeyPair = generator.generateKeyPair();

        String jwks = jwksJson((RSAPublicKey) signingKeyPair.getPublic(), KID);
        jwksServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        jwksServer.createContext("/jwks", exchange -> {
            jwksFetches.incrementAndGet();
            byte[] body = jwks.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        jwksServer.start();
        jwksUrl = "http://127.0.0.1:" + jwksServer.getAddress().getPort() + "/jwks";
    }

    @AfterAll
    static void stopJwksServer() {
        jwksServer.stop(0);
    }

    @BeforeEach
    void setUp() {
        ehrConnectivityManager = mock(EhrConnectivityManager.class);
        when(ehrConnectivityManager.getConfigValue(ONEID_KEYS.endpoint_jwks)).thenReturn(jwksUrl);
        when(ehrConnectivityManager.getConfigValue(ONEID_KEYS.oneid_issuer)).thenReturn(ISSUER);
        when(ehrConnectivityManager.getConfigValue(ONEID_KEYS.oag_client_id)).thenReturn(AUDIENCE);

        provider = new OneIdJwksProvider();
        ReflectionTestUtils.setField(provider, "ehrConnectivityManager", ehrConnectivityManager);
        jwksFetches.set(0);
    }

    @Test
    @DisplayName("should return subject when the token is valid")
    void shouldReturnSubject_whenTokenValid() throws Exception {
        String token = validTokenBuilder().compact();

        assertThat(provider.verifyIdToken(token, NONCE)).isEqualTo(SUBJECT);
    }

    @Test
    @DisplayName("should reject when signed by a key that is not in the JWKS")
    void shouldReject_whenSignedByForeignKey() {
        String token = validTokenBuilder().signWith(attackerKeyPair.getPrivate(), Jwts.SIG.RS256).compact();

        assertThatThrownBy(() -> provider.verifyIdToken(token, NONCE))
                .isInstanceOf(IdTokenValidationException.class);
    }

    @Test
    @DisplayName("should reject when the signature is tampered")
    void shouldReject_whenSignatureTampered() {
        String[] parts = validTokenBuilder().compact().split("\\.");
        // Tamper with the first character of the signature, not the last. An RSA-2048 signature is
        // 256 bytes, which is 342 base64url characters: the final character carries only two
        // significant bits and its remaining four are discarded on decode, so changing it leaves
        // roughly one signature in four byte-identical and the token verifies. Every bit of the
        // first character survives.
        char first = parts[2].charAt(0);
        parts[2] = (first == 'A' ? 'B' : 'A') + parts[2].substring(1);
        String tampered = parts[0] + "." + parts[1] + "." + parts[2];

        assertThatThrownBy(() -> provider.verifyIdToken(tampered, NONCE))
                .isInstanceOf(IdTokenValidationException.class);
    }

    @Test
    @DisplayName("should reject when the issuer does not match")
    void shouldReject_whenIssuerWrong() {
        String token = signedTokenBuilder().issuer("https://evil.example.com").compact();

        assertThatThrownBy(() -> provider.verifyIdToken(token, NONCE))
                .isInstanceOf(IdTokenValidationException.class);
    }

    @Test
    @DisplayName("should reject when the audience does not match")
    void shouldReject_whenAudienceWrong() {
        String token = signedTokenBuilder().audience().add("some-other-client").and().compact();

        assertThatThrownBy(() -> provider.verifyIdToken(token, NONCE))
                .isInstanceOf(IdTokenValidationException.class);
    }

    @Test
    @DisplayName("should reject when the token has expired")
    void shouldReject_whenExpired() {
        Date past = new Date(System.currentTimeMillis() - 3600_000L);
        String token = signedTokenBuilder().expiration(past).notBefore(past).compact();

        assertThatThrownBy(() -> provider.verifyIdToken(token, NONCE))
                .isInstanceOf(IdTokenValidationException.class);
    }

    @Test
    @DisplayName("should reject when the token is not yet valid")
    void shouldReject_whenNotYetValid() {
        Date future = new Date(System.currentTimeMillis() + 3600_000L);
        String token = signedTokenBuilder().notBefore(future).expiration(new Date(future.getTime() + 3600_000L)).compact();

        assertThatThrownBy(() -> provider.verifyIdToken(token, NONCE))
                .isInstanceOf(IdTokenValidationException.class);
    }

    @Test
    @DisplayName("should reject when the nonce does not match the login nonce")
    void shouldReject_whenNonceMismatch() {
        String token = validTokenBuilder().compact();

        assertThatThrownBy(() -> provider.verifyIdToken(token, "a-different-nonce"))
                .isInstanceOf(IdTokenValidationException.class);
    }

    @Test
    @DisplayName("should reject when the token carries no nonce")
    void shouldReject_whenNonceMissing() {
        String token = signedTokenBuilder().compact();

        assertThatThrownBy(() -> provider.verifyIdToken(token, NONCE))
                .isInstanceOf(IdTokenValidationException.class);
    }

    @Test
    @DisplayName("should reject when the key id is not in the JWKS")
    void shouldReject_whenKidUnknown() {
        String token = signedTokenBuilder().header().keyId("unknown-kid").and()
                .claim("nonce", NONCE).compact();

        assertThatThrownBy(() -> provider.verifyIdToken(token, NONCE))
                .isInstanceOf(IdTokenValidationException.class);
    }

    @Test
    @DisplayName("should reject an unsigned (alg none) token")
    void shouldReject_whenAlgNone() {
        String token = Jwts.builder().header().keyId(KID).and()
                .issuer(ISSUER).audience().add(AUDIENCE).and().subject(SUBJECT).claim("nonce", NONCE)
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 300_000L))
                .compact();

        assertThatThrownBy(() -> provider.verifyIdToken(token, NONCE))
                .isInstanceOf(IdTokenValidationException.class);
    }

    @Test
    @DisplayName("should reject when the token has no subject")
    void shouldReject_whenSubjectMissing() {
        Date now = new Date();
        String token = Jwts.builder().header().keyId(KID).and()
                .issuer(ISSUER).audience().add(AUDIENCE).and().claim("nonce", NONCE)
                .issuedAt(now).notBefore(now).expiration(new Date(now.getTime() + 300_000L))
                .signWith(signingKeyPair.getPrivate(), Jwts.SIG.RS256).compact();

        assertThatThrownBy(() -> provider.verifyIdToken(token, NONCE))
                .isInstanceOf(IdTokenValidationException.class);
    }

    @Test
    @DisplayName("should reject when the JWKS endpoint is not configured")
    void shouldReject_whenJwksEndpointNotConfigured() {
        when(ehrConnectivityManager.getConfigValue(ONEID_KEYS.endpoint_jwks)).thenReturn("  ");
        String token = validTokenBuilder().compact();

        assertThatThrownBy(() -> provider.verifyIdToken(token, NONCE))
                .isInstanceOf(IdTokenValidationException.class);
    }

    /** A fully-valid, RS256-signed token with the login nonce. */
    private JwtBuilder validTokenBuilder() {
        return signedTokenBuilder().claim("nonce", NONCE);
    }

    /** A signed token with valid issuer/audience/subject/times but no nonce claim. */
    private JwtBuilder signedTokenBuilder() {
        Date now = new Date();
        return Jwts.builder()
                .header().keyId(KID).and()
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .subject(SUBJECT)
                .issuedAt(now)
                .notBefore(now)
                .expiration(new Date(now.getTime() + 300_000L))
                .signWith(signingKeyPair.getPrivate(), Jwts.SIG.RS256);
    }

    private static String jwksJson(RSAPublicKey publicKey, String kid) {
        String n = base64Url(publicKey.getModulus());
        String e = base64Url(publicKey.getPublicExponent());
        return "{\"keys\":[{\"kty\":\"RSA\",\"use\":\"sig\",\"alg\":\"RS256\",\"kid\":\"" + kid
                + "\",\"n\":\"" + n + "\",\"e\":\"" + e + "\"}]}";
    }

    private static String base64Url(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Test
    @DisplayName("should fetch the key set once when two tokens are verified in the cache window")
    void shouldFetchOnce_whenTwoTokensVerifiedWithinCacheWindow() throws Exception {
        provider.verifyIdToken(validTokenBuilder().compact(), NONCE);
        provider.verifyIdToken(validTokenBuilder().compact(), NONCE);

        assertThat(jwksFetches.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("should not refetch the key set when an unknown key id arrives inside the refresh interval")
    void shouldNotRefetch_whenKeyIdUnknownWithinRefreshInterval() throws Exception {
        // Warms the cache, so the unknown key id below is the only thing that could force a fetch.
        provider.verifyIdToken(validTokenBuilder().compact(), NONCE);
        assertThat(jwksFetches.get()).isEqualTo(1);

        String unknownKid = signedTokenBuilder().header().keyId("unknown-kid").and()
                .claim("nonce", NONCE).compact();
        assertThatThrownBy(() -> provider.verifyIdToken(unknownKid, NONCE))
                .isInstanceOf(IdTokenValidationException.class);

        // A key id the held set does not carry asks for a refresh. Inside the interval the held
        // set is used instead, so a run of such tokens cannot fetch on every one of them.
        assertThat(jwksFetches.get()).isEqualTo(1);
    }

}
