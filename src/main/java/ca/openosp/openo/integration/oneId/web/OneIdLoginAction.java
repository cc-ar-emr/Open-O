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
package ca.openosp.openo.integration.oneId.web;

import ca.openosp.openo.integration.dhdr.OmdGateway;
import ca.openosp.openo.integration.oneId.OneIdGatewayData;
import ca.openosp.openo.log.LogAction;
import ca.openosp.openo.log.LogConst;
import ca.openosp.openo.utility.MiscUtils;
import org.apache.logging.log4j.Logger;
import org.apache.struts2.ActionSupport;
import org.apache.struts2.ServletActionContext;
import org.owasp.encoder.Encode;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

/**
 * Entry point that starts the ONE ID OAuth2/OIDC login. It generates the anti-forgery state, the
 * id-token nonce, and a PKCE code verifier, keeps them server-side in the session, and redirects
 * the browser to the Ontario Health authorize endpoint. It serves both the pre-authentication
 * login from the login page and the mid-session step-up for an already signed-in provider; an
 * optional local return URL is kept in the session so the callback can send the provider back to
 * the page that started the step-up. No privilege check runs here because there may be no
 * logged-in session yet; the handshake's own state, nonce, and id-token signature checks
 * establish trust on the callback.
 *
 * @since 2026-07-02
 */
public class OneIdLoginAction extends ActionSupport {

    private static final Logger logger = MiscUtils.getLogger();

    static final String SESSION_STATE = "oneIdState";
    static final String SESSION_NONCE = "oneIdNonce";
    static final String SESSION_VERIFIER = "oneIdVerifier";
    static final String SESSION_RETURN_URL = "oneIdReturnUrl";

    private final HttpServletRequest request = ServletActionContext.getRequest();
    private final HttpServletResponse response = ServletActionContext.getResponse();

    @Override
    public String execute() {
        try {
            OmdGateway omdGateway = new OmdGateway();
            String state = UUID.randomUUID().toString();
            String nonce = UUID.randomUUID().toString();
            String verifier = omdGateway.generateVerifier();

            HttpSession session = request.getSession(true);
            session.setAttribute(SESSION_STATE, state);
            session.setAttribute(SESSION_NONCE, nonce);
            session.setAttribute(SESSION_VERIFIER, verifier);

            String returnUrl = safeLocalPath(request.getParameter("returnUrl"));
            if (returnUrl != null) {
                session.setAttribute(SESSION_RETURN_URL, returnUrl);
            } else {
                session.removeAttribute(SESSION_RETURN_URL);
            }

            OneIdGatewayData gatewayData = new OneIdGatewayData();
            gatewayData.hasScope(OneIdGatewayData.fullScope);
            gatewayData.hasProfile(OneIdGatewayData.fullProfile);

            String authorizeUrl = omdGateway.buildAuthorizeUrl(gatewayData, state, nonce, verifier);
            response.sendRedirect(authorizeUrl);
            return NONE;
        } catch (Exception e) {
            logger.error("Failed to start ONE ID login", e);
            ConnectivityErrorHelper.ConnectivityError error = ConnectivityErrorHelper.map(e);
            LogAction.addLog("", LogConst.LOGIN, "failed", error.getUserMessage(), request.getRemoteAddr());
            try {
                response.sendRedirect(request.getContextPath() + "/loginfailed.jsp?errormsg="
                        + Encode.forUriComponent(error.getUserMessage() + " " + error.getNextStep()));
            } catch (Exception ignored) {
                // response already committed
            }
            return NONE;
        }
    }

    /**
     * Accepts only an application-local path for the post-login return redirect, so a crafted
     * link cannot turn the login flow into an open redirect. Control characters are refused
     * because a browser drops them from a URL before resolving it, which would let a path such
     * as "/&lt;tab&gt;/example.com" reach another host.
     *
     * @param value String the candidate return path
     * @return String the path when it is a plain local path, otherwise null
     */
    static String safeLocalPath(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        if (!value.startsWith("/") || value.startsWith("//")) {
            return null;
        }
        if (value.contains("\\") || value.contains(":")) {
            return null;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x20 || c == 0x7f) {
                return null;
            }
        }
        return value;
    }
}
