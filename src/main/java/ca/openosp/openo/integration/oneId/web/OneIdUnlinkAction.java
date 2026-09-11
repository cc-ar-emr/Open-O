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
import ca.openosp.openo.integration.oneId.OneIdSession;
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
 * Removes the ONE ID association from the acting provider's own account and ends the ONE ID session
 * for that provider. Clearing the stored key means a later ONE ID login no longer resolves to this
 * account until it is bound again. The provider stays signed into OpenO.
 *
 * @since 2026-07-02
 */
public class OneIdUnlinkAction extends ActionSupport {

    private static final Logger logger = MiscUtils.getLogger();
    private static final String SEC_OBJECT = "_ehr.connectivity";

    private final HttpServletRequest request = ServletActionContext.getRequest();
    private final HttpServletResponse response = ServletActionContext.getResponse();

    private final SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);
    private final EhrConnectivityManager ehrConnectivityManager = SpringUtils.getBean(EhrConnectivityManager.class);

    /**
     * Rebuilds just enough gateway data from the provider's stored ONE ID session to end it at
     * Ontario Health: the tokens the revoke needs and the hub topic the context clear names.
     *
     * <p>Used when the session filter has not put the gateway data on the request, which is what
     * happens on the first request after a restart and whenever the rehydration failed. The data is
     * attached to the acting session because the CMS context clear reads it from there.
     *
     * @param loggedInInfo LoggedInInfo the acting provider's session information
     * @param providerNo   String the acting provider
     * @return OneIdGatewayData the rebuilt data, or null when there is no stored session to rebuild from
     */
    private OneIdGatewayData gatewayDataFromStoredSession(LoggedInInfo loggedInInfo, String providerNo) {
        try {
            OneIdSession stored = ehrConnectivityManager.findOneIdSession(loggedInInfo, providerNo);
            if (stored == null) {
                return null;
            }
            OneIdGatewayData gatewayData = new OneIdGatewayData();
            // Decoded only when there is one. A row holding a hub topic and no access token still
            // has a CMS context to clear, and refusing to build anything for it left that context
            // standing; the revoke that needs the token fails on its own and is best-effort.
            String accessToken = stored.getAccessToken();
            if (accessToken != null && !accessToken.isEmpty()) {
                gatewayData.setAccessTokenStr(accessToken);
                gatewayData.processAccessToken(accessToken);
            }
            gatewayData.setIdTokenStr(stored.getIdToken());
            gatewayData.setHubTopic(stored.getHubTopic());
            gatewayData.setCtxSessionId(stored.getHubTopic());
            gatewayData.setUao(stored.getUaoUpi());
            gatewayData.setCmsUrl(storedCmsUrl(stored));
            loggedInInfo.setOneIdGatewayData(gatewayData);
            return gatewayData;
        } catch (Exception e) {
            logger.error("Could not rebuild the ONE ID session for unlink teardown", e);
            return null;
        }
    }

    /**
     * The CMS endpoint held in a stored session's toolbar, preferring hub.url and falling back to
     * cms_url, the way the session filter resolves it.
     *
     * <p>Kept off the rest of the rebuild. The toolbar is Base64 JSON decoded on read, so a stored
     * value that will not parse throws, and a failure to find one address would otherwise cost the
     * whole gateway data and with it the token revoke, which needs none of this.
     *
     * @param stored OneIdSession the provider's stored ONE ID session
     * @return String the CMS endpoint, or empty when the toolbar carries none or will not decode
     */
    private String storedCmsUrl(OneIdSession stored) {
        try {
            String cmsUrl = stored.getUrlFromToolbar(OmdGateway.ToolbarKeys.HUB_URL.key);
            if (cmsUrl == null || cmsUrl.isEmpty()) {
                cmsUrl = stored.getUrlFromToolbar(OmdGateway.ToolbarKeys.CMS_URL.key);
            }
            return cmsUrl == null ? "" : cmsUrl;
        } catch (Exception e) {
            logger.error("Could not read the CMS endpoint from the stored ONE ID toolbar ("
                    + e.getClass().getSimpleName() + "); the remote context cannot be cleared");
            return "";
        }
    }

    @Override
    public String execute() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        if (!securityInfoManager.hasPrivilege(loggedInInfo, SEC_OBJECT, "w", null)) {
            throw new SecurityException("missing required sec object (" + SEC_OBJECT + ")");
        }

        // Unlinking only runs on a POST, so a crafted link or image cannot sever the binding;
        // a plain GET just returns to the preference page untouched.
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            try {
                response.sendRedirect(request.getContextPath() + "/provider/providerpreference.jsp");
            } catch (Exception e) {
                logger.error("Failed to redirect a non-POST unlink request", e);
            }
            return NONE;
        }

        String providerNo = loggedInInfo.getLoggedInProviderNo();
        // End the session at Ontario Health first. Severing the binding locally leaves the EHR
        // context and the access token standing, and the tokens needed to withdraw them are about
        // to be discarded.
        OneIdGatewayData gatewayData = loggedInInfo.getOneIdGatewayData();
        if (gatewayData == null) {
            // Nothing on the session does not mean nothing at the far end: the stored row still
            // holds live tokens, and severing the binding without them leaves an Ontario Health
            // session and an access token standing that nothing can withdraw afterwards.
            gatewayData = gatewayDataFromStoredSession(loggedInInfo, providerNo);
        }
        if (gatewayData != null) {
            OneIdSessionTeardown.endRemoteSession(loggedInInfo, gatewayData);
        }

        if (ehrConnectivityManager.clearOneIdBinding(loggedInInfo, providerNo)) {
            LogAction.addLog(providerNo, LogConst.UNLINK, "ONE ID", "", request.getRemoteAddr());
        }

        try {
            ehrConnectivityManager.removeOneIdSession(loggedInInfo, providerNo);
        } catch (Exception e) {
            // The binding is already gone, so the row left behind is rehydrated on the next request
            // as a session whose tokens have just been revoked. Recorded rather than thrown: an
            // uncaught failure here would also lose the redirect and leave the provider on an
            // error page with the unlink half done and nothing said about it.
            logger.error("ONE ID session removal on unlink failed", e);
            LogAction.addLog(providerNo, LogConst.NORIGHT, "ONE ID",
                    "unlink did not complete: the stored session could not be removed",
                    request.getRemoteAddr());
        }
        if (loggedInInfo.getOneIdGatewayData() != null) {
            // The emptied object is detached as well; left on the session it would make the
            // provider look signed in to ONE ID with no usable tokens.
            loggedInInfo.getOneIdGatewayData().clearGatewayData();
            loggedInInfo.setOneIdGatewayData((OneIdGatewayData) null);
            if (request.getSession(false) != null) {
                request.getSession(false).removeAttribute(LoggedInInfo.OH_GATEWAY_DATA);
            }
        }

        try {
            response.sendRedirect(request.getContextPath() + "/provider/providerpreference.jsp");
        } catch (Exception e) {
            logger.error("Failed to redirect after ONE ID unlink", e);
        }
        return NONE;
    }
}
