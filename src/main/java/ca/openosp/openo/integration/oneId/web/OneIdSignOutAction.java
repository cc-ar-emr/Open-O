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

import ca.openosp.openo.integration.oneId.OneIdGatewayData;
import ca.openosp.openo.log.LogAction;
import ca.openosp.openo.log.LogConst;
import ca.openosp.openo.managers.EhrConnectivityManager;
import ca.openosp.openo.managers.SecurityInfoManager;
import ca.openosp.openo.utility.LoggedInInfo;
import ca.openosp.openo.utility.MiscUtils;
import ca.openosp.openo.utility.SpringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.struts2.ActionSupport;
import org.apache.struts2.ServletActionContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Ends the acting provider's ONE ID session while leaving them signed into OpenO. The patient and
 * user are cleared from the EHR context, the access token is revoked, and the stored ONE ID session
 * is removed, so a later EHR service launch starts a fresh sign-in. The account stays linked; use
 * the unlink action to sever the binding itself.
 *
 * @since 2026-08-03
 */
public class OneIdSignOutAction extends ActionSupport {

    private static final Logger logger = MiscUtils.getLogger();
    private static final String SEC_OBJECT = "_ehr.connectivity";

    private final HttpServletRequest request = ServletActionContext.getRequest();
    private final HttpServletResponse response = ServletActionContext.getResponse();

    private final SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);
    private final EhrConnectivityManager ehrConnectivityManager = SpringUtils.getBean(EhrConnectivityManager.class);

    @Override
    public String execute() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        if (!securityInfoManager.hasPrivilege(loggedInInfo, SEC_OBJECT, "w", null)) {
            throw new SecurityException("missing required sec object (" + SEC_OBJECT + ")");
        }

        // Signing out only runs on a POST, so a crafted link or image cannot end the session;
        // a plain GET just returns to the preference page untouched.
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            redirectToPreferences();
            return NONE;
        }

        String providerNo = loggedInInfo.getLoggedInProviderNo();
        OneIdGatewayData gatewayData = loggedInInfo.getOneIdGatewayData();
        if (gatewayData == null) {
            redirectToPreferences();
            return NONE;
        }

        OneIdSessionTeardown.endRemoteSession(loggedInInfo, gatewayData);
        boolean removed = true;
        try {
            ehrConnectivityManager.removeOneIdSession(loggedInInfo, providerNo);
        } catch (Exception e) {
            removed = false;
            logger.error("ONE ID session removal on sign out failed", e);
        }

        clearGatewayData(loggedInInfo, gatewayData);
        if (removed) {
            LogAction.addLog(providerNo, LogConst.LOGOUT, "ONE ID", "", request.getRemoteAddr());
        } else {
            // The stored session outlived the attempt to delete it, so the next request restores
            // it and the provider is signed in again. The grant was already withdrawn, so what
            // comes back is a session whose tokens no longer work. Recording a sign-out here
            // would say something happened that did not.
            LogAction.addLog(providerNo, LogConst.NORIGHT, "ONE ID",
                    "sign out did not complete: the stored session could not be removed",
                    request.getRemoteAddr());
        }
        redirectToPreferences();
        return NONE;
    }

    /**
     * Clears the gateway data and detaches it from the HTTP session. Leaving the emptied object on
     * the session would make the provider look signed in to ONE ID with no usable tokens.
     *
     * @param loggedInInfo LoggedInInfo the acting provider's session information
     * @param gatewayData  OneIdGatewayData the gateway data being discarded
     */
    private void clearGatewayData(LoggedInInfo loggedInInfo, OneIdGatewayData gatewayData) {
        gatewayData.clearGatewayData();
        loggedInInfo.setOneIdGatewayData((OneIdGatewayData) null);
        if (request.getSession(false) != null) {
            request.getSession(false).removeAttribute(LoggedInInfo.OH_GATEWAY_DATA);
        }
    }

    private void redirectToPreferences() {
        try {
            response.sendRedirect(request.getContextPath() + "/provider/providerpreference.jsp");
        } catch (Exception e) {
            logger.error("Failed to redirect after ONE ID sign out", e);
        }
    }
}
