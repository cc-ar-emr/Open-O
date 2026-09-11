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

import ca.openosp.openo.PMmodule.dao.ProviderDao;
import ca.openosp.openo.commn.model.Provider;
import ca.openosp.openo.integration.oneId.OneIdViewlet;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Administrative screen for the Viewlet registry. Lists the configured Viewlets and supports
 * adding, editing, enabling and disabling entries, including each Viewlet's interface type
 * (modal or non-modal). Restricted to administrators.
 *
 * @since 2026-07-06
 */
public class ViewletAdminAction extends ActionSupport {

    private static final Logger logger = MiscUtils.getLogger();
    private static final String SEC_OBJECT = "_admin.ehrConnectivity";
    private static final String MODE_MODAL = "modal";
    private static final String MODE_NON_MODAL = "non-modal";

    private final HttpServletRequest request = ServletActionContext.getRequest();
    private final HttpServletResponse response = ServletActionContext.getResponse();

    private final SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);
    private final EhrConnectivityManager ehrConnectivityManager = SpringUtils.getBean(EhrConnectivityManager.class);
    private final ProviderDao providerDao = SpringUtils.getBean(ProviderDao.class);

    @Override
    public String execute() {
        LoggedInInfo loggedInInfo = loggedInInfo();
        checkPrivilege(loggedInInfo, "r");
        List<OneIdViewlet> viewletList = ehrConnectivityManager.findAllViewlets(loggedInInfo);
        request.setAttribute("viewletList", viewletList);
        request.setAttribute("updatedByNames", resolveProviderNames(viewletList));
        Object flash = request.getSession().getAttribute("viewletAdminError");
        if (flash != null) {
            request.setAttribute("viewletAdminError", flash);
            request.getSession().removeAttribute("viewletAdminError");
        }
        return "success";
    }

    private Map<String, String> resolveProviderNames(List<OneIdViewlet> viewletList) {
        Map<String, String> names = new HashMap<>();
        for (OneIdViewlet viewlet : viewletList) {
            String providerNo = viewlet.getUpdatedBy();
            if (providerNo != null && !names.containsKey(providerNo)) {
                Provider provider = providerDao.getProvider(providerNo);
                names.put(providerNo, provider != null ? provider.getFormattedName() : providerNo);
            }
        }
        return names;
    }

    public String add() {
        LoggedInInfo loggedInInfo = loggedInInfo();
        checkPrivilege(loggedInInfo, "w");
        if (rejectNonPost()) {
            return NONE;
        }
        String name = trimToNull(request.getParameter("name"));
        String keyValue = trimToNull(request.getParameter("keyValue"));
        boolean showInEchart = "true".equals(request.getParameter("showInEchart"));
        String displayMode = normalizeDisplayMode(request.getParameter("displayMode"));

        if (name != null && keyValue != null) {
            if (!lengthsFit(name, keyValue)) {
                redirectToList();
                return NONE;
            }
            if (keyInUse(loggedInInfo, keyValue, null)) {
                flashKeyInUse();
                redirectToList();
                return NONE;
            }
            OneIdViewlet viewlet = new OneIdViewlet();
            viewlet.setName(name);
            viewlet.setKeyValue(keyValue);
            viewlet.setShowInEchart(showInEchart);
            viewlet.setDisplayMode(displayMode);
            viewlet.setDeleted(false);
            viewlet.setUpdatedBy(loggedInInfo.getLoggedInProviderNo());
            viewlet.setUpdateTime(new Date());
            ehrConnectivityManager.createViewlet(loggedInInfo, viewlet);
            audit(loggedInInfo, LogConst.ADD, viewlet.getId(), "after=" + describe(viewlet));
        }
        redirectToList();
        return NONE;
    }

    public String update() {
        LoggedInInfo loggedInInfo = loggedInInfo();
        checkPrivilege(loggedInInfo, "w");
        if (rejectNonPost()) {
            return NONE;
        }
        OneIdViewlet viewlet = findViewlet(loggedInInfo, request.getParameter("id"));
        String name = trimToNull(request.getParameter("name"));
        String keyValue = trimToNull(request.getParameter("keyValue"));

        if (viewlet != null && name != null && keyValue != null) {
            if (!lengthsFit(name, keyValue)) {
                redirectToList();
                return NONE;
            }
            if (keyInUse(loggedInInfo, keyValue, viewlet.getId())) {
                flashKeyInUse();
                redirectToList();
                return NONE;
            }
            String before = describe(viewlet);
            viewlet.setName(name);
            viewlet.setKeyValue(keyValue);
            viewlet.setShowInEchart("true".equals(request.getParameter("showInEchart")));
            viewlet.setDisplayMode(normalizeDisplayMode(request.getParameter("displayMode")));
            viewlet.setUpdatedBy(loggedInInfo.getLoggedInProviderNo());
            viewlet.setUpdateTime(new Date());
            ehrConnectivityManager.updateViewlet(loggedInInfo, viewlet);
            audit(loggedInInfo, LogConst.UPDATE, viewlet.getId(), "before=" + before + " after=" + describe(viewlet));
        }
        redirectToList();
        return NONE;
    }

    public String disable() {
        LoggedInInfo loggedInInfo = loggedInInfo();
        checkPrivilege(loggedInInfo, "w");
        if (rejectNonPost()) {
            return NONE;
        }
        OneIdViewlet viewlet = findViewlet(loggedInInfo, request.getParameter("id"));
        if (viewlet != null && !viewlet.isDeleted()) {
            String before = describe(viewlet);
            viewlet.setDeleted(true);
            viewlet.setUpdatedBy(loggedInInfo.getLoggedInProviderNo());
            viewlet.setUpdateTime(new Date());
            ehrConnectivityManager.updateViewlet(loggedInInfo, viewlet);
            audit(loggedInInfo, LogConst.DELETE, viewlet.getId(), "before=" + before + " after={disabled}");
        }
        redirectToList();
        return NONE;
    }

    public String enable() {
        LoggedInInfo loggedInInfo = loggedInInfo();
        checkPrivilege(loggedInInfo, "w");
        if (rejectNonPost()) {
            return NONE;
        }
        OneIdViewlet viewlet = findViewlet(loggedInInfo, request.getParameter("id"));
        if (viewlet != null && viewlet.isDeleted()) {
            // The key was free when this one was disabled, so another Viewlet may have taken it
            // since. Two active rows on one key make a launch resolve to whichever has the lower
            // id, which is this one.
            if (keyInUse(loggedInInfo, viewlet.getKeyValue(), viewlet.getId())) {
                flashKeyInUse();
                redirectToList();
                return NONE;
            }
            viewlet.setDeleted(false);
            viewlet.setUpdatedBy(loggedInInfo.getLoggedInProviderNo());
            viewlet.setUpdateTime(new Date());
            ehrConnectivityManager.updateViewlet(loggedInInfo, viewlet);
            audit(loggedInInfo, LogConst.UPDATE, viewlet.getId(), "before={disabled} after=" + describe(viewlet));
        }
        redirectToList();
        return NONE;
    }

    /**
     * Whether an active Viewlet already holds this toolbar key.
     *
     * <p>Compared without regard to case, because the column's collation is case-insensitive and
     * so is the toolbar lookup that resolves a launch. Comparing exactly let "DHDR" through beside
     * an existing "dhdr", and the launch then picked whichever row came first.
     *
     * @param loggedInInfo LoggedInInfo the acting administrator
     * @param keyValue String the key being claimed
     * @param excludeId Integer the row being edited, or null when adding
     * @return boolean true when another active row already holds the key
     */
    private boolean keyInUse(LoggedInInfo loggedInInfo, String keyValue, Integer excludeId) {
        for (OneIdViewlet existing : ehrConnectivityManager.findAllViewlets(loggedInInfo)) {
            if (!existing.isDeleted() && keyValue.equalsIgnoreCase(existing.getKeyValue())
                    && !existing.getId().equals(excludeId)) {
                return true;
            }
        }
        return false;
    }

    /** The most characters the OneIdViewlet name column holds. */
    private static final int MAX_NAME_LENGTH = 100;

    /** The most characters the OneIdViewlet keyValue column holds. */
    private static final int MAX_KEY_LENGTH = 50;

    /**
     * Whether the submitted name and key fit the columns that store them.
     *
     * <p>Checked here rather than left to the database, which answers an over-long value with a
     * truncation error the screen cannot explain, or by silently cutting it short depending on the
     * server's strict-mode setting. The form caps both too, but only in the browser.
     *
     * @param name String the submitted name
     * @param keyValue String the submitted toolbar key
     * @return boolean true when both fit
     */
    private boolean lengthsFit(String name, String keyValue) {
        if (name.length() > MAX_NAME_LENGTH) {
            flash("The Viewlet name must be " + MAX_NAME_LENGTH + " characters or fewer.");
            return false;
        }
        if (keyValue.length() > MAX_KEY_LENGTH) {
            flash("The toolbar key must be " + MAX_KEY_LENGTH + " characters or fewer.");
            return false;
        }
        return true;
    }

    private void flash(String message) {
        request.getSession().setAttribute("viewletAdminError", message);
    }

    private void flashKeyInUse() {
        flash("A Viewlet with that toolbar key already exists.");
    }

    private OneIdViewlet findViewlet(LoggedInInfo loggedInInfo, String idValue) {
        Integer id = parseId(idValue);
        if (id == null) {
            return null;
        }
        return ehrConnectivityManager.findViewlet(loggedInInfo, id);
    }

    private void audit(LoggedInInfo loggedInInfo, String action, Integer viewletId, String beforeAfter) {
        LogAction.addLog(loggedInInfo.getLoggedInProviderNo(), action, "viewlet-config",
                viewletId == null ? null : String.valueOf(viewletId), request.getRemoteAddr(), null, beforeAfter);
    }

    private static String describe(OneIdViewlet viewlet) {
        return "{name=" + viewlet.getName() + ", key=" + viewlet.getKeyValue()
                + ", showInEchart=" + viewlet.isShowInEchart() + ", displayMode=" + viewlet.getDisplayMode() + "}";
    }

    private static String normalizeDisplayMode(String value) {
        return MODE_MODAL.equals(value) ? MODE_MODAL : MODE_NON_MODAL;
    }

    /**
     * Refuses a state change that did not arrive as a POST, sending the caller back to the list.
     *
     * @return boolean true when the request was refused and a redirect written
     */
    private boolean rejectNonPost() {
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        redirectToList();
        return true;
    }

    private void redirectToList() {
        try {
            response.sendRedirect(request.getContextPath() + "/admin/viewletAdmin.do");
        } catch (Exception e) {
            logger.error("Failed to redirect to the Viewlet admin list", e);
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
