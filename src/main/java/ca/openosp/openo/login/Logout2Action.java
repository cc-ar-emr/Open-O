//CHECKSTYLE:OFF
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

package ca.openosp.openo.login;

import ca.openosp.openo.integration.dhdr.OmdGateway;
import ca.openosp.openo.integration.ohcms.CMSManager;
import ca.openosp.openo.integration.oneId.OneIdGatewayData;
import ca.openosp.openo.log.LogAction;
import ca.openosp.openo.log.LogConst;
import ca.openosp.openo.managers.EhrConnectivityManager;
import ca.openosp.openo.managers.SecurityInfoManager;
import ca.openosp.openo.utility.LoggedInInfo;
import ca.openosp.openo.utility.MiscUtils;
import ca.openosp.openo.utility.SpringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.Logger;
import org.apache.struts2.ActionSupport;
import org.apache.struts2.ServletActionContext;

public class Logout2Action extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();

    private static final Logger logger = MiscUtils.getLogger();
    private final EhrConnectivityManager ehrConnectivityManager = SpringUtils.getBean(EhrConnectivityManager.class);
    private final SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);


    @Override
    public String execute() {
        String method = request.getParameter("method");

        if ("ssoLogout".equals(method)) {
            return ssoLogout();
        } else if ("sessionTimeout".equals(method)) {
            return sessionTimeout();
        } else if ("failure".equals(method)) {
            return failure();
        }

        // Default to logout method if unspecified
        return logout();
    }

    public String ssoLogout() {
        String result = logout();
        if (NONE.equals(result)) {
            // logout() redirected to the ONE ID End Session endpoint and committed the response.
            // Forwarding the SAML result on top of that fails with "cannot forward after the
            // response has been committed", so the ONE ID sign-out is the one that stands.
            return NONE;
        }
        return "logoutsso";
    }

    public String sessionTimeout() {
        return logout();
    }

    public String failure() {
        return logout();
    }

    public String logout() {

        HttpSession session = request.getSession(false);
        String login = request.getParameter("login");
        String errorMessage = request.getParameter("errorMessage");
        String nameId = request.getParameter("nameId");

        // End the ONE ID session (revoke + remove) while the tokens are still readable.
        String oneIdEndSessionUrl = endOneIdSession(session);

        if (session != null) {
            String user = (String) session.getAttribute("user");
            session.invalidate();
            if (user != null) {
                LogAction.addLog(user, LogConst.LOGOUT, LogConst.CON_LOGIN, "", request.getRemoteAddr());
            }
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                // Sanitize cookie name to prevent HTTP response splitting
                String cookieName = cookie.getName().replaceAll("[\\r\\n]", "");
                Cookie expiredCookie = new Cookie(cookieName, "");
                expiredCookie.setMaxAge(0);
                expiredCookie.setPath("/");
                response.addCookie(expiredCookie);
            }
        }

        if (oneIdEndSessionUrl != null) {
            try {
                response.sendRedirect(oneIdEndSessionUrl);
                // Only a redirect that was actually written ends the request here. Returning NONE
                // for an attempt that threw would leave the caller with nothing to navigate to,
                // and ssoLogout reads this result to decide whether its own is still needed.
                return NONE;
            } catch (Exception e) {
                logger.error("Failed to redirect to the ONE ID End Session endpoint", e);
            }
        }
        return SUCCESS;
    }

    private String endOneIdSession(HttpSession session) {
        if (session == null) {
            return null;
        }
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(session);
        if (loggedInInfo == null) {
            return null;
        }
        OneIdGatewayData gatewayData = loggedInInfo.getOneIdGatewayData();
        if (gatewayData == null) {
            return null;
        }
        String idTokenHint = gatewayData.getIdTokenStr();
        boolean hasEhrServiceAccess = securityInfoManager.hasPrivilege(loggedInInfo, "_ehr.connectivity", SecurityInfoManager.READ, null);
        OmdGateway omdGateway = new OmdGateway();
        try {
            CMSManager.userLogout(loggedInInfo);
        } catch (Exception e) {
            logger.error("ONE ID CMS context clear on logout failed\n" + OmdGateway.stackTraceWithoutMessages(e));
        }
        try {
            omdGateway.revokeToken(loggedInInfo, gatewayData);
        } catch (Exception e) {
            logger.error("ONE ID token revoke failed\n" + OmdGateway.stackTraceWithoutMessages(e));
        }
        try {
            ehrConnectivityManager.removeOwnOneIdSession(loggedInInfo);
        } catch (Exception e) {
            logger.error("ONE ID session removal failed\n" + OmdGateway.stackTraceWithoutMessages(e));
            // The stored session outlived the attempt to delete it, so the next login restores it
            // along with its tokens. Recording a sign-out here would say something happened that
            // did not.
            LogAction.addLog(loggedInInfo.getLoggedInProviderNo(), LogConst.NORIGHT, "ONE ID",
                    "sign out did not complete: the stored session could not be removed",
                    request.getRemoteAddr());
        }
        gatewayData.clearGatewayData();
        try {
            return omdGateway.buildEndSessionUrl(idTokenHint, hasEhrServiceAccess);
        } catch (Exception e) {
            logger.error("Failed to build the ONE ID End Session URL", e);
            return null;
        }
    }
}
