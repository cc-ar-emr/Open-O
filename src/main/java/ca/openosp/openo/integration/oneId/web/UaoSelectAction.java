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

import ca.openosp.openo.commn.model.UAO;
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
import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * Chooses the "Under Authority Of" (UAO) value the acting provider signs in under. After a ONE ID
 * login the provider is sent here: with no UAO values they proceed straight in, with exactly one it
 * is selected automatically, and with several they pick one. The chosen value is attached to the
 * ONE ID session so every gateway call carries it.
 *
 * @since 2026-07-02
 */
public class UaoSelectAction extends ActionSupport {

    private static final Logger logger = MiscUtils.getLogger();
    private static final String SEC_OBJECT = "_ehr.connectivity";

    private final HttpServletRequest request = ServletActionContext.getRequest();
    private final HttpServletResponse response = ServletActionContext.getResponse();

    private final SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);
    private final EhrConnectivityManager ehrConnectivityManager = SpringUtils.getBean(EhrConnectivityManager.class);

    @Override
    public String execute() {
        LoggedInInfo loggedInInfo = loggedInInfo();
        checkPrivilege(loggedInInfo, "r");
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        List<UAO> uaoList = ehrConnectivityManager.findUaosByProvider(loggedInInfo, providerNo);
        if (uaoList == null || uaoList.isEmpty()) {
            clearUao(loggedInInfo, providerNo);
            redirectHome();
            return NONE;
        }
        if (uaoList.size() == 1) {
            applyUao(loggedInInfo, providerNo, uaoList.get(0));
            redirectHome();
            return NONE;
        }

        request.setAttribute("uaoList", uaoList);
        request.setAttribute("currentUao", currentUaoName(loggedInInfo));
        return "success";
    }

    public String select() {
        LoggedInInfo loggedInInfo = loggedInInfo();
        checkPrivilege(loggedInInfo, "w");
        // The authority is only changed on a POST, so a crafted link or image cannot switch the
        // custodian a clinician is acting under. A plain GET changes nothing and returns to the
        // picker, where the choice can be made properly.
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            redirectToPicker();
            return NONE;
        }
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UAO uao = ownActiveUao(loggedInInfo, providerNo, parseId(request.getParameter("id")));
        if (uao == null) {
            // The authority went out of use between the picker being drawn and the choice arriving.
            // Going home would leave the previous one on the session, where it still names a
            // custodian to the CMS and still satisfies the check that lets a launch through. The
            // picker re-reads what is left and clears the session when there is nothing.
            redirectToPicker();
            return NONE;
        }
        applyUao(loggedInInfo, providerNo, uao);
        redirectHome();
        return NONE;
    }

    /**
     * Finds one of the acting provider's own values that is still in use.
     *
     * <p>The id arrives on the request, so it is matched against the same list the picker was built
     * from rather than fetched on its own. Only a value that list offered can be applied: not one
     * belonging to another provider, and not one that has been taken out of use.
     *
     * @param loggedInInfo LoggedInInfo the acting provider's session information
     * @param providerNo   String the acting provider
     * @param id           Integer the submitted value's id, which may be absent or unparseable
     * @return UAO the matching value, or null when the id names none of theirs
     */
    private UAO ownActiveUao(LoggedInInfo loggedInInfo, String providerNo, Integer id) {
        if (id == null) {
            return null;
        }
        List<UAO> uaoList = ehrConnectivityManager.findUaosByProvider(loggedInInfo, providerNo);
        if (uaoList == null) {
            return null;
        }
        for (UAO candidate : uaoList) {
            if (id.equals(candidate.getId())) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Drops the authority a provider was acting under, from the request and from the stored session.
     *
     * <p>Withdrawing a provider's last authority only deactivates the row. The value they last
     * selected stays on their session, where it still names a custodian to the CMS and still
     * satisfies the check that lets an EHR service launch, so it is cleared as soon as this screen
     * finds nothing left to offer them.
     *
     * @param loggedInInfo LoggedInInfo the acting provider's session information
     * @param providerNo   String the acting provider
     */
    private void clearUao(LoggedInInfo loggedInInfo, String providerNo) {
        OneIdGatewayData gatewayData = loggedInInfo.getOneIdGatewayData();
        if (gatewayData != null) {
            gatewayData.setUao(null);
            gatewayData.setUaoFriendlyName(null);
        }
        ehrConnectivityManager.setSessionUao(loggedInInfo, providerNo, null, null);
    }

    private void applyUao(LoggedInInfo loggedInInfo, String providerNo, UAO uao) {
        OneIdGatewayData gatewayData = loggedInInfo.getOneIdGatewayData();
        if (gatewayData != null) {
            gatewayData.setUao(uao.getName());
            gatewayData.setUaoFriendlyName(uao.getFriendlyName());
            gatewayData.setUpdateUAOInCMS(true);
        }
        ehrConnectivityManager.setSessionUao(loggedInInfo, providerNo, uao.getName(), uao.getFriendlyName());
        ehrConnectivityManager.setDefaultUao(loggedInInfo, uao, providerNo);
        LogAction.addLog(providerNo, LogConst.UPDATE, "UAO", providerNo + ":" + uao.getName(), request.getRemoteAddr());
    }

    private String currentUaoName(LoggedInInfo loggedInInfo) {
        OneIdGatewayData gatewayData = loggedInInfo.getOneIdGatewayData();
        if (gatewayData == null) {
            return null;
        }
        String friendlyName = gatewayData.getUaoFriendlyName();
        // Rows saved before the friendly name was required carry none, and the value itself is the
        // only other thing that tells one authority from another.
        return (friendlyName == null || friendlyName.trim().isEmpty())
                ? gatewayData.getUao() : friendlyName;
    }

    /**
     * Returns to the authority picker without touching the session. Used when a change arrives on
     * anything but a POST, which must leave everything as it was.
     */
    private void redirectToPicker() {
        try {
            response.sendRedirect(request.getContextPath() + "/uaoSelect.do");
        } catch (Exception e) {
            logger.error("Failed to redirect after a non-POST authority change", e);
        }
    }

    private void redirectHome() {
        try {
            // A step-up sign-in leaves the page that started it in the session; return there,
            // otherwise land on the provider home page.
            HttpSession session = request.getSession(false);
            if (session != null) {
                String returnUrl = OneIdLoginAction.safeLocalPath(
                        (String) session.getAttribute(OneIdLoginAction.SESSION_RETURN_URL));
                session.removeAttribute(OneIdLoginAction.SESSION_RETURN_URL);
                if (returnUrl != null) {
                    response.sendRedirect(returnUrl);
                    return;
                }
            }
            response.sendRedirect(request.getContextPath() + "/provider/providercontrol.jsp");
        } catch (Exception e) {
            logger.error("Failed to redirect after UAO selection", e);
        }
    }

    private LoggedInInfo loggedInInfo() {
        return LoggedInInfo.getLoggedInInfoFromSession(request);
    }

    private void checkPrivilege(LoggedInInfo loggedInInfo, String right) {
        if (!securityInfoManager.hasPrivilege(loggedInInfo, SEC_OBJECT, right, null)) {
            throw new SecurityException("missing required sec object (" + SEC_OBJECT + ")");
        }
    }

    private static Integer parseId(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
