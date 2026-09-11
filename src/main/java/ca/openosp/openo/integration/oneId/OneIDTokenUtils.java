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

import ca.openosp.openo.integration.dhdr.OmdGateway;
import ca.openosp.openo.utility.LoggedInInfo;
import ca.openosp.openo.utility.MiscUtils;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map.Entry;

public class OneIDTokenUtils {

  static Logger logger = MiscUtils.getLogger();

  public static String urlEncode(String toEncode) {
    if (toEncode == null) {
      return "";
    }
    String encoded = toEncode.replace("%", "%25");
    encoded = encoded.replace(" ", "%20");
    encoded = encoded.replace("!", "%21");
    encoded = encoded.replace("#", "%23");
    encoded = encoded.replace("$", "%24");
    encoded = encoded.replace("&", "%26");
    encoded = encoded.replace("'", "%27");
    encoded = encoded.replace("(", "%28");
    encoded = encoded.replace(")", "%29");
    encoded = encoded.replace("*", "%2A");
    encoded = encoded.replace("+", "%2B");
    encoded = encoded.replace(",", "%2C");
    encoded = encoded.replace("/", "%2F");
    encoded = encoded.replace(":", "%3A");
    encoded = encoded.replace(";", "%3B");
    encoded = encoded.replace("=", "%3D");
    encoded = encoded.replace("?", "%3F");
    encoded = encoded.replace("@", "%40");
    encoded = encoded.replace("[", "%5B");
    encoded = encoded.replace("]", "%5D");
    return encoded;
  }

  public static String debugTokens(HttpSession session) {
    String tokenAttr = (String) session.getAttribute("oneid_token");

    if (tokenAttr == null) {
      logger.warn("tokenAttr is null");
      return "ERROR no token";
    }
    StringBuilder sb = new StringBuilder(
        "===============================\nDEBUG ONEID TOKEN\n=======================\n");
    try {
      JSONObject tokens = new JSONObject(tokenAttr);
      sb.append("\n" + tokens.toString(3));

      String accessToken = tokens.getString("access_token");

      if (accessToken == null) {
        logger.warn("accessToken is null");
        return "ERROR no access token";
      }
      sb.append("\n\nACCESS TOKEN\n");
      DecodedJWT decodedJWT = JWT.decode(accessToken);
      for (Entry<String, Claim> entry : decodedJWT.getClaims().entrySet()) {
        sb.append("\t entry:" + entry.getKey() + "  " + entry.getValue().asString() + "\n");
      }

      decodedJWT = JWT.decode(tokens.getString("refresh_token"));
      sb.append("\n\nRefresh TOKEN\n");
      for (Entry<String, Claim> entry : decodedJWT.getClaims().entrySet()) {
        sb.append("\t entry:" + entry.getKey() + "  " + entry.getValue().asString() + "\n");

      }

      decodedJWT = JWT.decode(tokens.getString("id_token"));
      sb.append("\n\nID TOKEN\n");
      for (Entry<String, Claim> entry : decodedJWT.getClaims().entrySet()) {
        sb.append("\t entry:" + entry.getKey() + "  " + entry.getValue().asString() + "\n");
      }
    } catch (Exception e) {
      sb.append("Error parsing Token " + tokenAttr);
    }
    sb.append("\n=================================\n");

    return sb.toString();
  }

  /**
   * Makes sure the provider has an access token the gateway will accept, refreshing an expired one.
   *
   * <p>A provider who has not signed in to ONE ID has no gateway data at all, and every caller
   * reads the access token off it on the next line. Left unguarded that is a NullPointerException,
   * which reaches the user as whatever the caller says about an unexpected failure - on the DHDR
   * search, "the service could not be reached", pointing at Ontario Health for something a sign-in
   * fixes. No session and a dead session call for the same thing from the user, so they raise the
   * same exception and land on the same prompt.</p>
   *
   * <p>The expiry read here is only what decides whether to go and refresh. A provider's refreshes
   * are serialized in {@link OmdGateway#refreshToken}, which re-reads the expiry once it holds the
   * lock, so two requests arriving together do not both exchange the same refresh token.</p>
   *
   * @param loggedInInfo LoggedInInfo the current session, used to persist a refreshed token
   * @param oneIdGatewayData OneIdGatewayData the provider's ONE ID tokens, or null when they have
   *     not signed in
   * @throws TokenExpiredException when there is no session, or the refresh token is dead too
   */
  public static void verifyAccessTokenIsValid(LoggedInInfo loggedInInfo,
                                              OneIdGatewayData oneIdGatewayData) throws TokenExpiredException {
    if (oneIdGatewayData == null) {
      throw new TokenExpiredException("No ONE ID session. Sign in to ONE ID and try again.");
    }
    if (oneIdGatewayData.isAccessTokenExpired()) {
      refreshToken(loggedInInfo, oneIdGatewayData);
    }
  }

  private static void refreshToken(LoggedInInfo loggedInInfo, OneIdGatewayData oneIdGatewayData)
      throws TokenExpiredException {

    if (oneIdGatewayData.isRefreshTokenExpired()) {
      logger.info("Token was expired" + oneIdGatewayData);
      throw new TokenExpiredException();
    }
    OmdGateway omdGateway = new OmdGateway();
    omdGateway.refreshToken(loggedInInfo, oneIdGatewayData);
  }

  public static String getCompleteURL(HttpServletRequest request) {
    StringBuffer requestURL = request.getRequestURL();
    if (request.getQueryString() != null) {
      requestURL.append("?").append(request.getQueryString());
    }
    return requestURL.toString();
  }
}
