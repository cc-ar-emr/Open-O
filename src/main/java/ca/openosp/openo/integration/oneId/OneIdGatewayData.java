/**
 * Copyright (c) 2001-2002. Department of Family Medicine, McMaster University. All Rights Reserved.
 * This software is published under the GPL GNU General Public License. This program is free
 * software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 * <p>
 * This software was written for the Department of Family Medicine McMaster University Hamilton
 * Ontario, Canada
 */
package ca.openosp.openo.integration.oneId;

import ca.openosp.openo.utility.MiscUtils;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.Serializable;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.Map.Entry;
import java.util.UUID;

public class OneIdGatewayData implements Serializable {

  static Logger logger = MiscUtils.getLogger();
  String oneIdString = null;
  JSONObject endPointToolbar = null;
  JSONObject tokens = null;
  DecodedJWT accessToken = null;
  DecodedJWT refreshToken = null;
  DecodedJWT idToken = null;

  private String accessTokenStr = null;
  private String refreshTokenStr = null;
  private String idTokenStr = null;
  private String hubTopic = null;
  private String authorizationId = null;
  private String cmsLoggedIn = null;
  private String cmsPatientInContext = null;
  private String ctxSessionId;
  private String uao = null;
  private String uaoFriendlyName = null;
  private boolean doubleCheckUAO = false;
  private boolean updateUAOInCMS = false;
  private String cmsUrl = null;
  private String pcoiUrl = null;
  private String fhirIss = null;
  private Date lastKeptActive = null;
  private boolean isSso = false;

  private String scope = "openid"; // the base scope.
  private String _profile = "";
  private String uniqueSessionId = null;

  public static String[] fullScope = {"openid", "user/MedicationDispense.read", "toolbar",
      "user/Context.read", "user/Context.write", "user/Consent.write", "user/Immunization.read",
      "user/Immunization.write", "user/Patient.read", "azs"};
  public static String[] fullProfile = {
      "http://ehealthontario.ca/StructureDefinition/ca-on-dhdr-profile-MedicationDispense",
      "http://ehealthontario.ca/fhir/StructureDefinition/ca-on-consent-pcoi-profile-Consent",
      "http://ehealthontario.ca/StructureDefinition/ca-on-dhir-profile-Immunization",
      "http://ehealthontario.ca/StructureDefinition/ca-on-dhir-profile-Patient"};

  public OneIdGatewayData() {
    this.uniqueSessionId = UUID.randomUUID().toString();
  }

  public OneIdGatewayData(String oneIdString) {
    this.uniqueSessionId = UUID.randomUUID().toString();
    processOneIdString(oneIdString);
  }

  public void clearGatewayData() {
    oneIdString = null;
    endPointToolbar = null;
    tokens = null;
    accessToken = null;
    refreshToken = null;
    idToken = null;
    accessTokenStr = null;
    refreshTokenStr = null;
    idTokenStr = null;
    hubTopic = null;
    authorizationId = null;
    cmsLoggedIn = null;
    cmsPatientInContext = null;
    ctxSessionId = null;
    uao = null;
    uaoFriendlyName = null;
  }

  public void processOneIdString(String oneIdString) {
    if (oneIdString != null) {
      this.oneIdString = oneIdString;
      try {
        tokens = new JSONObject(oneIdString);
        accessTokenStr = tokens.optString("access_token");
        refreshTokenStr = tokens.optString("refresh_token");
        idTokenStr = tokens.optString("id_token");
        String toolbarStr = tokens.getString("toolbar");
        setAuthorizationId(tokens.optString("authzid"));
        processAccessToken(accessTokenStr);
        processRefreshToken(refreshTokenStr);
        processIdToken(idTokenStr);
        processToolBar(toolbarStr);
        if (tokens.has("hub.topic")) {
          hubTopic = tokens.getString("hub.topic");
        }
      } catch (Exception e) {
        logger.error("Error parsing ONE ID token response", e);
      }
    }
  }


  public String getProviderUPI() {
    return uao;
  }

  public String getAccessToken() {
    return accessTokenStr;
  }


  public void processAccessToken(String accessTokenStr) {
    this.accessTokenStr = accessTokenStr;
    accessToken = JWT.decode(accessTokenStr);
  }

  public void processRefreshToken(String refreshTokenStr) {
    refreshToken = JWT.decode(refreshTokenStr);
  }

  public String getRefreshTokenString() {
    return refreshTokenStr;
  }

  public void processIdToken(String idTokenStr) {
    idToken = JWT.decode(idTokenStr);
  }

  public void processToolBar(String toolbarStr) throws JSONException {
    logger.debug("toolbar process " + toolbarStr);
    String toolbarStrDecoded = new String(Base64.decodeBase64(toolbarStr));
    logger.debug("toolbarStrDecoded: " + toolbarStrDecoded);
    toolbarStrDecoded = toolbarStrDecoded.replaceAll("[\\u201C\\u201D]",
        "\"");
    logger.debug("toolbarStrDecoded: " + toolbarStrDecoded);
    endPointToolbar = new JSONObject(toolbarStrDecoded);
    logger.debug("endPointToolbar " + endPointToolbar);
  }

  static final String PCOI_URL = "pcoi_url";
  static final String CMS_URL = "cms_url";
  static final String FHIR_iss = "FHIR_iss";

  public boolean isAccessTokenExpired() {
    long iat = accessToken.getClaim("iat").asLong();
    int expiresIn = accessToken.getClaim("expires_in").asInt();

    logger.debug("iat=" + iat);
    logger.debug("expires_in=" + expiresIn);
    Date date = Date.from(Instant.ofEpochSecond(iat));
    logger.debug("date=" + date);

    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    cal.add(Calendar.SECOND, expiresIn);

    Date expires = cal.getTime();

    Calendar cal2 = Calendar.getInstance();
    cal2.add(Calendar.SECOND, 15);

    Date inTheFuture = cal2.getTime();

    logger.debug("IAT=" + date);
    logger.debug("expires=" + expires);
    logger.debug("inTheFuture=" + inTheFuture);

    if (expires.after(inTheFuture)) {
      logger.info("access token is not expired");
      return false;
    }
    return true;
  }

  public boolean isRefreshTokenExpired() {

    long iat = refreshToken.getClaim("iat").asLong();
    int expiresIn = refreshToken.getClaim("expires_in").asInt();

    Date date = Date.from(Instant.ofEpochSecond(iat));

    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    cal.add(Calendar.SECOND, expiresIn);

    Date expires = cal.getTime();

    if (expires.before(new Date())) {
      return true;
    }
    return false;
  }

  public Date getAccessTokenExpireDate() {
    return Date.from(Instant.ofEpochSecond(accessToken.getClaim("exp").asLong()));
  }

  public Date getRefreshTokenExpireDate() {
    return Date.from(Instant.ofEpochSecond(refreshToken.getClaim("exp").asLong()));
  }

  public int howLongSinceRefreshTokenWasIssued() {
    try {
      Date refreshTokenIatDate = Date.from(
          Instant.ofEpochSecond(refreshToken.getClaim("iat").asLong()));
      Date currentDate = new Date();
      long refreshTokenTimeMillis = refreshTokenIatDate.getTime();
      long currentTime = currentDate.getTime();
      long numMillisActive = currentTime - refreshTokenTimeMillis;
      long numMinutesActive = numMillisActive / 1000 / 60;
      return (int) numMinutesActive;
    } catch (Exception ignored) {
    }
    return 0;
  }

  public long howLongUntilAccessTokenIsExpired() {
    if (accessToken == null || accessToken.getClaim("iat") == null) {
      return -1L;
    }
    Date accessTokenDate = Date.from(Instant.ofEpochSecond(accessToken.getClaim("exp").asLong()));
    Date now = new Date();
    return (accessTokenDate.getTime() - now.getTime()) / 1000;

  }

  private void debugDecodedJWT(StringBuilder sb, String heading, DecodedJWT decodedJWT) {
    sb.append("\n\n" + heading + "\n");
    for (Entry<String, Claim> entry : decodedJWT.getClaims().entrySet()) {
      sb.append("\t entry:" + entry.getKey() + "  " + entry.getValue().asString() + "\n");
    }
  }

  public String debug() {

    StringBuilder sb = new StringBuilder(
        "===============================\nDEBUG ONEID TOKEN\n=======================\n");

    if (tokens == null) {
      sb.append("ERROR no token");
    } else {
        try {
            sb.append("\n" + tokens.toString(3));
        } catch (JSONException e) {
            // do nothing
        }

        if (accessToken == null) {
        sb.append("ERROR no access token");
      } else {
        debugDecodedJWT(sb, "Access TOKEN", accessToken);
        debugDecodedJWT(sb, "Refresh TOKEN", refreshToken);
        debugDecodedJWT(sb, "Id TOKEN", idToken);
      }
    }
    if (endPointToolbar != null) {
      java.util.Iterator<?> keys = endPointToolbar.keys();
      while (keys.hasNext()) {
        String key = (String) keys.next();
        logger.debug("E " + key + "=" + endPointToolbar.opt(key));
        logger.debug("class " + endPointToolbar.opt(key).getClass());
      }
    }

    sb.append("\n=================================\n");
    return sb.toString();
  }

  public String getAccessTokenStr() {
    return accessTokenStr;
  }

  public void setAccessTokenStr(String accessTokenStr) {
    this.accessTokenStr = accessTokenStr;
  }

  public void setRefreshTokenStr(String refreshTokenStr) {
    this.refreshTokenStr = refreshTokenStr;
  }

  public void setIdTokenStr(String idTokenStr) {
    this.idTokenStr = idTokenStr;
  }

  public String getIdTokenStr() {
    return idTokenStr;
  }

  public void setHubTopic(String hubTopicResponseBody) {
    this.hubTopic = hubTopicResponseBody;
  }

  public String getHubTopic() {
    return this.hubTopic;
  }

  public String getCmsLoggedIn() {
    return cmsLoggedIn;
  }

  public void setCmsLoggedIn(String cmsLoggedIn) {
    this.cmsLoggedIn = cmsLoggedIn;
  }

  public String getCmsPatientInContext() {
    return cmsPatientInContext;
  }

  public void setCmsPatientInContext(String cmsPatientInContext) {
    this.cmsPatientInContext = cmsPatientInContext;
  }

  public String getCtxSessionId() {
    return ctxSessionId;
  }

  public void setCtxSessionId(String ctxSessionId) {
    this.ctxSessionId = ctxSessionId;
  }

  public String getUao() {
    return uao;
  }

  public void setUao(String uao) {
    this.uao = uao;
  }

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public String get_profile() {
    if (_profile != null) {
      return _profile.trim();
    }
    return _profile;
  }

  public boolean hasScope(String... scopes) {
    StringBuilder sb = new StringBuilder(scope);
    boolean hadScope = verifyScopeIsPresent(sb, scopes);
    scope = sb.toString();
    return hadScope;
  }

  public boolean hasProfile(String... scopes) {
    StringBuilder sb = new StringBuilder(_profile);
    boolean hadScope = verifyScopeIsPresent(sb, scopes);
    _profile = sb.toString();
    return hadScope;
  }

  private boolean verifyScopeIsPresent(StringBuilder valueLine, String... scopes) {
    boolean hadScope = true;
    for (String newScope : scopes) {
      logger.info(
          "valueLine " + valueLine + " new scope " + newScope + " index " + valueLine.indexOf(
              newScope));
      if (valueLine.indexOf(newScope) == -1) {
        hadScope = false;
        valueLine.append(" " + newScope);
      }
    }
    logger.info("Scope leaving " + valueLine + " scopes " + scopes);
    return hadScope;
  }

  public String getUniqueSessionId() {
    return uniqueSessionId;
  }

  public boolean isDoubleCheckUAO() {
    return doubleCheckUAO;
  }

  public void setDoubleCheckUAO(boolean doubleCheckUAO) {
    this.doubleCheckUAO = doubleCheckUAO;
  }

  public String getUaoFriendlyName() {
    return uaoFriendlyName;
  }

  public void setUaoFriendlyName(String uaoFriendlyName) {
    this.uaoFriendlyName = uaoFriendlyName;
  }

  public boolean isUpdateUAOInCMS() {
    return updateUAOInCMS;
  }

  public void setUpdateUAOInCMS(boolean updateUAOInCMS) {
    this.updateUAOInCMS = updateUAOInCMS;
  }

  public String getAuthorizationId() {
    return authorizationId;
  }

  public void setAuthorizationId(String authorizationId) {
    this.authorizationId = authorizationId;
  }

  public String getCmsUrl() {
    return cmsUrl;
  }

  public void setCmsUrl(String cmsUrl) {
    this.cmsUrl = cmsUrl;
  }

  public String getPcoiUrl() {
    return pcoiUrl;
  }

  public void setPcoiUrl(String pcoiUrl) {
    this.pcoiUrl = pcoiUrl;
  }

  public String getFhirIss() {
    return fhirIss;
  }

  public void setFhirIss(String fhirIss) {
    this.fhirIss = fhirIss;
  }

  public Date getLastKeptActive() {
    return lastKeptActive;
  }

  public void setLastKeptActive(Date lastKeptActive) {
    this.lastKeptActive = lastKeptActive;
  }

  public boolean isSso() {
    return isSso;
  }

  public void setSso(boolean sso) {
    isSso = sso;
  }
}
