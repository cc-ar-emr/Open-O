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
import org.owasp.encoder.Encode;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;

/**
 * Administrative screen for managing the "Under Authority Of" (UAO) values assigned to a provider.
 * Lists a provider's active values and supports adding, removing, and choosing the default.
 * Restricted to administrators.
 *
 * @since 2026-07-02
 */
public class UaoAdminAction extends ActionSupport {

    private static final Logger logger = MiscUtils.getLogger();
    private static final String SEC_OBJECT = "_admin.ehrConnectivity";

    private final HttpServletRequest request = ServletActionContext.getRequest();
    private final HttpServletResponse response = ServletActionContext.getResponse();

    private final SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);
    private final EhrConnectivityManager ehrConnectivityManager = SpringUtils.getBean(EhrConnectivityManager.class);

    @Override
    public String execute() {
        LoggedInInfo loggedInInfo = loggedInInfo();
        checkPrivilege(loggedInInfo, "r");
        String providerNo = trimToNull(request.getParameter("providerNo"));
        if (providerNo != null) {
            List<UAO> uaoList = ehrConnectivityManager.findUaosByProvider(loggedInInfo, providerNo);
            request.setAttribute("uaoList", uaoList);
        }
        request.setAttribute("providerNo", providerNo);
        // Shown once and dropped, the way the Viewlet screen carries its own message across the
        // redirect that follows a change.
        Object flash = request.getSession().getAttribute("uaoAdminError");
        if (flash != null) {
            request.setAttribute("uaoAdminError", flash);
            request.getSession().removeAttribute("uaoAdminError");
        }
        return "success";
    }

    public String add() {
        LoggedInInfo loggedInInfo = loggedInInfo();
        checkPrivilege(loggedInInfo, "w");
        String providerNo = trimToNull(request.getParameter("providerNo"));
        if (rejectNonPost(providerNo)) {
            return NONE;
        }
        String name = trimToNull(request.getParameter("name"));
        String friendlyName = trimToNull(request.getParameter("friendlyName"));
        String address = trimToNull(request.getParameter("address"));
        String city = trimToNull(request.getParameter("city"));
        String province = trimToNull(request.getParameter("province"));
        String postal = trimToNull(request.getParameter("postal"));
        boolean makeDefault = "true".equals(request.getParameter("defaultUAO"));

        if (providerNo != null && name != null) {
            UAO uao = new UAO();
            uao.setProviderNo(providerNo);
            uao.setName(name);
            uao.setFriendlyName(friendlyName);
            uao.setAddress(address);
            uao.setCity(city);
            uao.setProvince(province);
            uao.setPostal(postal);
            uao.setActive(true);
            uao.setDefaultUAO(makeDefault);
            uao.setAddedBy(loggedInInfo.getLoggedInProviderNo());
            uao.setDateCreated(new Date());
            uao.setDateUpdated(new Date());
            ehrConnectivityManager.createUao(loggedInInfo, uao);
            if (makeDefault) {
                ehrConnectivityManager.setDefaultUao(loggedInInfo, uao, providerNo);
            }
            audit(loggedInInfo, LogConst.ADD, providerNo,
                    "after={name=" + name + ", friendly=" + friendlyName
                            + ", address=" + address + ", city=" + city
                            + ", province=" + province + ", postal=" + postal
                            + ", default=" + makeDefault + "}");
        }
        redirectToList(providerNo);
        return NONE;
    }

    public String delete() {
        LoggedInInfo loggedInInfo = loggedInInfo();
        checkPrivilege(loggedInInfo, "w");
        String providerNo = trimToNull(request.getParameter("providerNo"));
        if (rejectNonPost(providerNo)) {
            return NONE;
        }
        UAO uao = findOwned(loggedInInfo, request.getParameter("id"), providerNo);
        if (uao != null) {
            String uaoName = uao.getName();
            boolean wasDefault = Boolean.TRUE.equals(uao.getDefaultUAO());
            uao.setActive(false);
            uao.setDefaultUAO(false);
            uao.setDateUpdated(new Date());
            ehrConnectivityManager.updateUao(loggedInInfo, uao);
            boolean sessionSettled = clearWithdrawnSessionUao(loggedInInfo, providerNo, uaoName);
            audit(loggedInInfo, LogConst.DELETE, providerNo,
                    "before={name=" + uaoName + ", default=" + wasDefault + "} after={removed"
                            + (sessionSettled ? "" : ", session not cleared") + "}");
            if (!sessionSettled) {
                // The row is withdrawn but the provider may still be acting under it, so the audit
                // line above says so rather than reading as a clean removal, and the administrator
                // is told the removal has not fully taken effect.
                request.getSession().setAttribute("uaoAdminError",
                        "The authority was withdrawn, but the provider's ONE ID session could not be"
                                + " updated. They may keep acting under it until they sign in again.");
            }
        }
        redirectToList(providerNo);
        return NONE;
    }

    /**
     * Takes a withdrawn authority off the provider's stored ONE ID session.
     *
     * <p>Deactivating the row stops it being offered, but the value the provider selected earlier
     * stays on their session, where it still names a custodian to the CMS and still satisfies the
     * check that lets a gateway call through. They would go on acting under an authority that has
     * been taken away from them until they next sign in.
     *
     * <p>Only when it is the one they are on. Withdrawing an authority they are not using changes
     * nothing about the one they are.
     *
     * @param loggedInInfo LoggedInInfo the acting administrator's session information
     * @param providerNo   String the provider the authority belonged to
     * @param uaoName      String the withdrawn authority's value
     */
    private boolean clearWithdrawnSessionUao(LoggedInInfo loggedInInfo, String providerNo, String uaoName) {
        if (uaoName == null) {
            return true;
        }
        try {
            // One statement, carrying both conditions. A provider can hold two assignments with the
            // same value, so withdrawing one leaves the other in force and there is nothing to
            // clear; and either the provider picking a different authority or another administrator
            // adding that value back would slip between a check made here and the write that
            // followed it.
            ehrConnectivityManager.clearSessionUaoIfWithdrawn(loggedInInfo, providerNo, uaoName);
            return true;
        } catch (Exception e) {
            logger.error("Could not clear the withdrawn authority from the provider's ONE ID session", e);
            return false;
        }
    }

    public String setDefault() {
        LoggedInInfo loggedInInfo = loggedInInfo();
        checkPrivilege(loggedInInfo, "w");
        String providerNo = trimToNull(request.getParameter("providerNo"));
        if (rejectNonPost(providerNo)) {
            return NONE;
        }
        UAO uao = findOwned(loggedInInfo, request.getParameter("id"), providerNo);
        if (uao != null) {
            String previousDefault = currentDefaultName(loggedInInfo, providerNo);
            ehrConnectivityManager.setDefaultUao(loggedInInfo, uao, providerNo);
            audit(loggedInInfo, LogConst.UPDATE, providerNo,
                    "before={default=" + previousDefault + "} after={default=" + uao.getName() + "}");
        }
        redirectToList(providerNo);
        return NONE;
    }

    /**
     * Finds one of a provider's authorities that is still in use.
     *
     * <p>A withdrawn value is not returned. The list this screen works from only shows active
     * values, so an id naming a withdrawn one comes from a page left open while someone else
     * changed it, and acting on it would remove what is already removed or record a default that
     * was never set.
     *
     * @param loggedInInfo LoggedInInfo the acting administrator's session information
     * @param idValue      String the submitted id, which may be absent or unparseable
     * @param providerNo   String the provider the value must belong to
     * @return UAO the matching value, or null when the id names none of theirs
     */
    private UAO findOwned(LoggedInInfo loggedInInfo, String idValue, String providerNo) {
        Integer id = parseId(idValue);
        if (id == null || providerNo == null) {
            return null;
        }
        UAO uao = ehrConnectivityManager.findUao(loggedInInfo, id);
        if (uao != null && providerNo.equals(uao.getProviderNo())
                && Boolean.TRUE.equals(uao.getActive())) {
            return uao;
        }
        return null;
    }

    private void audit(LoggedInInfo loggedInInfo, String action, String targetProviderNo, String beforeAfter) {
        LogAction.addLog(loggedInInfo.getLoggedInProviderNo(), action, "uao-assignment",
                targetProviderNo, request.getRemoteAddr(), null, beforeAfter);
    }

    private String currentDefaultName(LoggedInInfo loggedInInfo, String providerNo) {
        List<UAO> list = ehrConnectivityManager.findUaosByProvider(loggedInInfo, providerNo);
        if (list != null) {
            for (UAO current : list) {
                if (Boolean.TRUE.equals(current.getDefaultUAO())) {
                    return current.getName();
                }
            }
        }
        return "none";
    }

    /**
     * Refuses a state change that did not arrive as a POST, sending the caller back to the list.
     *
     * @param providerNo String the provider whose list to return to, which may be absent
     * @return boolean true when the request was refused and a redirect written
     */
    private boolean rejectNonPost(String providerNo) {
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        redirectToList(providerNo);
        return true;
    }

    private void redirectToList(String providerNo) {
        try {
            String url = request.getContextPath() + "/admin/uaoAdmin.do";
            if (providerNo != null) {
                url += "?providerNo=" + Encode.forUriComponent(providerNo);
            }
            response.sendRedirect(url);
        } catch (Exception e) {
            logger.error("Failed to redirect to the UAO admin list", e);
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

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        value = value.trim();
        return value.isEmpty() ? null : value;
    }
}
