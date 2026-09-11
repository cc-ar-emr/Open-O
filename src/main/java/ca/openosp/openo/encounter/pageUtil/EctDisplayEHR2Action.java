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
package ca.openosp.openo.encounter.pageUtil;

import ca.openosp.openo.commn.dao.DemographicDao;
import ca.openosp.openo.commn.model.Demographic;
import ca.openosp.openo.integration.oneId.OneIdViewlet;
import ca.openosp.openo.managers.EhrConnectivityManager;
import ca.openosp.openo.util.StringUtils;
import ca.openosp.openo.utility.LoggedInInfo;
import ca.openosp.openo.utility.SpringUtils;
import org.owasp.encoder.Encode;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * eChart left-navbar module listing the provincial EHR services (Viewlets) available to launch
 * for the open patient. Each active Viewlet shows by name; launching goes through the Viewlet
 * launch endpoint, honouring the Viewlet's configured interface type. A patient record missing
 * the fields the EHR context needs blocks the launch with a message listing them.
 */
public class EctDisplayEHR2Action extends EctDisplayAction {

    private static final String cmd = "ehr";

    private final EhrConnectivityManager ehrConnectivityManager = SpringUtils.getBean(EhrConnectivityManager.class);
    private final DemographicDao demographicDao = SpringUtils.getBean(DemographicDao.class);

    public boolean getInfo(EctSessionBean bean, HttpServletRequest request, NavBarDisplayDAO Dao) {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);

        if (!securityInfoManager.hasPrivilege(loggedInInfo, "_ehr.connectivity", "r", null)) {
            return true;
        }

        List<OneIdViewlet> viewlets = ehrConnectivityManager.findEchartViewlets(loggedInInfo);
        if (viewlets.isEmpty()) {
            // No configured services; skip the widget rather than render it empty.
            return true;
        }

        Dao.setLeftHeading("Provincial EHR Services");
        Dao.setRightHeadingID(cmd);

        Demographic demographic = demographicDao.getDemographicById(Integer.parseInt(bean.demographicNo));
        String missingFields = missingCmsDemographicFields(demographic);

        for (OneIdViewlet viewlet : viewlets) {
            NavBarDisplayDAO.Item item = NavBarDisplayDAO.Item();
            // LeftNavBarDisplay.jsp writes this into title='...', so it needs attribute encoding
            // and not the HTML-body encoding the visible text below gets: a name carrying an
            // apostrophe closes the attribute otherwise.
            item.setLinkTitle(Encode.forHtmlAttribute("Open " + viewlet.getName()));
            // LeftNavBarDisplay.jsp writes the title straight out, so an administrator's Viewlet
            // name reaches the chart as markup unless it is encoded. Truncated first, so the
            // crop lands on the name rather than in the middle of an entity.
            item.setTitle(Encode.forHtml(
                    StringUtils.maxLenString(viewlet.getName(), MAX_LEN_TITLE, CROP_LEN_TITLE, ELLIPSES)));
            String url;
            if (missingFields == null) {
                url = "launchViewlet('" + request.getContextPath() + "'," + bean.demographicNo + ",'"
                        + Encode.forJavaScript(viewlet.getKeyValue()) + "','"
                        + Encode.forJavaScript(viewlet.getDisplayMode()) + "');return false;";
            } else {
                url = "alert('" + Encode.forJavaScript(missingFields) + "');return false;";
            }
            item.setURL(url);
            Dao.addItem(item);
        }
        return true;
    }

    /**
     * Returns a message listing the demographic fields the EHR context needs but the patient
     * record is missing, or null when the record carries them all.
     *
     * @param demographic Demographic the patient record
     * @return String the missing-fields message, or null
     */
    private String missingCmsDemographicFields(Demographic demographic) {
        if (demographic == null) {
            return "Unable to launch Context-Aware EHR service: the demographic record was not found.";
        }
        StringBuilder missing = new StringBuilder();
        if (isBlank(demographic.getHin())) {
            missing.append("\n- health card number");
        }
        if (isBlank(demographic.getFirstName())) {
            missing.append("\n- first name");
        }
        if (isBlank(demographic.getLastName())) {
            missing.append("\n- last name");
        }
        if (isBlank(demographic.getSex())) {
            missing.append("\n- gender");
        }
        if (isBlank(demographic.getFormattedDob())) {
            missing.append("\n- DOB");
        }
        if (missing.length() == 0) {
            return null;
        }
        return "Unable to launch Context-Aware EHR service due to missing data in demographic record." + missing;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public String getCmd() {
        return cmd;
    }
}
