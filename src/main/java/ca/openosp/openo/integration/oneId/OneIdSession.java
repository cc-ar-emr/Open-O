/**
 * Copyright (c) 2021 WELL EMR Group Inc. This software is made available under the terms of the GNU
 * General Public License, Version 2, 1991 (GPLv2). License details are available via
 * "gnu.org/licenses/gpl-2.0.html".
 */
package ca.openosp.openo.integration.oneId;


import ca.openosp.openo.commn.model.AbstractModel;
import ca.openosp.openo.util.MapUtils;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * OneID session entity for managing OAuth 2.0 authentication tokens and user information.
 * Stores provider session data including access tokens, refresh tokens, and user metadata.
 *
 * @since 2021-01-01
 */
@Entity
public class OneIdSession extends AbstractModel<Object> {

  private static final Logger log = LogManager.getLogger(OneIdSession.class);
  private static final int MILLISECONDS = 1000;

  @Id
  private String providerNo;
  private String accessToken;
  private String refreshToken;
  private String idToken;
  private String subject;
  private String email;
  private String serviceEntitlements;
  private String hubTopic;
  private String uaoUpi;
  private String uaoName;
  private String authorizationId;
  private String toolbar;
  private long timestamp;
  private Date lastKeptActive;
  private boolean sso;

  /**
   * Default constructor for JPA.
   */
  public OneIdSession() {
  }
  @Override
  public Object getId() {
    return null;
  }

  /**
   * Gets the provider number.
   *
   * @return String the provider number
   */
  public String getProviderNo() {
    return providerNo;
  }

  /**
   * Sets the provider number.
   *
   * @param providerNo String the provider number
   */
  public void setProviderNo(String providerNo) {
    this.providerNo = providerNo;
  }

  /**
   * Gets the access token.
   *
   * @return String the OAuth 2.0 access token
   */
  public String getAccessToken() {
    return accessToken;
  }

  /**
   * Sets the access token.
   *
   * @param accessToken String the OAuth 2.0 access token
   */
  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  /**
   * Gets the refresh token.
   *
   * @return String the OAuth 2.0 refresh token
   */
  public String getRefreshToken() {
    return refreshToken;
  }

  /**
   * Sets the refresh token.
   *
   * @param refreshToken String the OAuth 2.0 refresh token
   */
  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  /**
   * Gets the ID token.
   *
   * @return String the OpenID Connect ID token
   */
  public String getIdToken() {
    return idToken;
  }

  /**
   * Sets the ID token.
   *
   * @param idToken String the OpenID Connect ID token
   */
  public void setIdToken(String idToken) {
    this.idToken = idToken;
  }

  /**
   * Gets the subject identifier.
   *
   * @return String the subject identifier from the token
   */
  public String getSubject() {
    return subject;
  }

  /**
   * Sets the subject identifier.
   *
   * @param subject String the subject identifier from the token
   */
  public void setSubject(String subject) {
    this.subject = subject;
  }

  /**
   * Gets the user's email address.
   *
   * @return String the email address
   */
  public String getEmail() {
    return email;
  }

  /**
   * Sets the user's email address.
   *
   * @param email String the email address
   */
  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * Gets the service entitlements.
   *
   * @return String the service entitlements
   */
  public String getServiceEntitlements() {
    return serviceEntitlements;
  }

  /**
   * Sets the service entitlements.
   *
   * @param serviceEntitlements String the service entitlements
   */
  public void setServiceEntitlements(String serviceEntitlements) {
    this.serviceEntitlements = serviceEntitlements;
  }

  /**
   * Gets the hub topic.
   *
   * @return String the hub topic
   */
  public String getHubTopic() {
    return hubTopic;
  }

  /**
   * Sets the hub topic.
   *
   * @param hubTopic String the hub topic
   */
  public void setHubTopic(String hubTopic) {
    this.hubTopic = hubTopic;
  }

  /**
   * Gets the UAO UPI.
   *
   * @return String the UAO UPI
   */
  public String getUaoUpi() {
    return uaoUpi;
  }

  /**
   * Sets the UAO UPI.
   *
   * @param uaoUpi String the UAO UPI
   */
  public void setUaoUpi(String uaoUpi) {
    this.uaoUpi = uaoUpi;
  }

  /**
   * Gets the UAO name.
   *
   * @return String the UAO name
   */
  public String getUaoName() {
    return uaoName;
  }

  /**
   * Sets the UAO name.
   *
   * @param uaoName String the UAO name
   */
  public void setUaoName(String uaoName) {
    this.uaoName = uaoName;
  }

  /**
   * Gets the authorization ID.
   *
   * @return String the authorization ID
   */
  public String getAuthorizationId() {
    return authorizationId;
  }

  /**
   * Sets the authorization ID.
   *
   * @param authorizationId String the authorization ID
   */
  public void setAuthorizationId(String authorizationId) {
    this.authorizationId = authorizationId;
  }

  /**
   * Gets the toolbar configuration.
   *
   * @return String the base64-encoded toolbar JSON
   */
  public String getToolbar() {
    return toolbar;
  }

  /**
   * Sets the toolbar configuration.
   *
   * @param toolbar String the base64-encoded toolbar JSON
   */
  public void setToolbar(String toolbar) {
    this.toolbar = toolbar;
  }

  /**
   * Gets the timestamp.
   *
   * @return long the timestamp
   */
  public long getTimestamp() {
    return timestamp;
  }

  /**
   * Sets the timestamp.
   *
   * @param timestamp long the timestamp
   */
  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  /**
   * Gets the last kept active date.
   *
   * @return Date the last kept active date
   */
  public Date getLastKeptActive() {
    return lastKeptActive;
  }

  /**
   * Sets the last kept active date.
   *
   * @param lastKeptActive Date the last kept active date
   */
  public void setLastKeptActive(Date lastKeptActive) {
    this.lastKeptActive = lastKeptActive;
  }

  /**
   * Checks if SSO is enabled.
   *
   * @return boolean true if SSO is enabled, false otherwise
   */
  public boolean isSso() {
    return sso;
  }

  /**
   * Sets the SSO flag.
   *
   * @param sso boolean true to enable SSO, false otherwise
   */
  public void setSso(boolean sso) {
    this.sso = sso;
  }

  /**
   * Checks if the session token is expired.
   *
   * @return boolean true if expired, false otherwise
   */
  public boolean isExpired() {
    return this.accessToken == null || isTokenExpired();
  }

  /**
   * Checks if the access token is expired based on JWT claims.
   *
   * @return boolean true if token is expired, false otherwise
   */
  private boolean isTokenExpired() {
    DecodedJWT decodedAccessToken = JWT.decode(this.accessToken);
    Date issueDate = getIssueDate(decodedAccessToken);
    Date expiresDate = getExpirationDate(decodedAccessToken, issueDate);
    Date futureTime = getFutureTime();
    boolean expired = expiresDate.before(futureTime);
    if (log.isDebugEnabled()) {
      log.debug(String.format(
          "isExpired() - %s - Issued: %s, Expires: %s, Future: %s",
          expired, issueDate, expiresDate, futureTime
      ));
    }
    return expired;
  }

  /**
   * Extracts the issue date from the JWT token.
   *
   * @param accessToken DecodedJWT the decoded access token
   * @return Date the issue date
   */
  private Date getIssueDate(final DecodedJWT accessToken) {
    long iat = accessToken.getClaim("iat").asLong();
    return new Date(iat * MILLISECONDS);
  }

  /**
   * Calculates the expiration date from the JWT token and issue date.
   *
   * @param accessToken DecodedJWT the decoded access token
   * @param issueDate Date the issue date
   * @return Date the expiration date
   */
  private Date getExpirationDate(final DecodedJWT accessToken, final Date issueDate) {
    int expires = accessToken.getClaim("expires_in").asInt();
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(issueDate);
    calendar.add(Calendar.SECOND, expires);
    return calendar.getTime();
  }

  /**
   * Gets a future time 15 seconds from now for token expiration checking.
   *
   * @return Date the future time
   */
  private Date getFutureTime() {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.SECOND, 15);
    return calendar.getTime();
  }

  /**
   * Decodes the toolbar JSON string and returns the URL for the given key.
   *
   * @param key String the configurable key to get the URL from the toolbar
   * @return String a URL or an empty string if the key is not found
   */
  public String getUrlFromToolbar(final String key) {
    return MapUtils.getOrDefault(decodeToolbar(), key, "").toString();
  }

  /**
   * Returns the keys present in the decoded toolbar. These are endpoint and service configuration
   * keys (no PHI or secrets), suitable for logging when an expected key is absent.
   *
   * @return Set<String> the toolbar keys, empty when the toolbar is missing or unparseable
   */
  public Set<String> getToolbarKeys() {
    return decodeToolbar().keySet();
  }

  /**
   * Decodes the Base64 toolbar JSON string into a key/URL map.
   *
   * @return Map<String, Object> the decoded toolbar, empty when the toolbar is missing or unparseable
   */
  @SuppressWarnings("unchecked")
  private Map<String, Object> decodeToolbar() {
    if (toolbar == null || toolbar.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<String, Object> toolbarMap =
        new Gson()
            .fromJson(
                new String(DatatypeConverter.parseBase64Binary(toolbar), StandardCharsets.UTF_8),
                Map.class);
    return toolbarMap == null ? Collections.emptyMap() : toolbarMap;
  }
}
