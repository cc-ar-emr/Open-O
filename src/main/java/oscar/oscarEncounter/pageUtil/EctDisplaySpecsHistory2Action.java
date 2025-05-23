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


package oscar.oscarEncounter.pageUtil;


import org.oscarehr.eyeform.dao.EyeformSpecsHistoryDao;
import org.oscarehr.eyeform.model.EyeformSpecsHistory;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SpringUtils;
import oscar.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class EctDisplaySpecsHistory2Action extends EctDisplayAction {
    private static final String cmd = "specshistory";

    private EyeformSpecsHistoryDao specsHistoryDao = (EyeformSpecsHistoryDao) SpringUtils.getBean(EyeformSpecsHistoryDao.class);

    public boolean getInfo(EctSessionBean bean, HttpServletRequest request, NavBarDisplayDAO Dao) {
        if (!securityInfoManager.hasPrivilege(LoggedInInfo.getLoggedInInfoFromSession(request), "_eyeform", "r", null)) {
            throw new SecurityException("missing required security object (_eyeform)");
        }

        try {

            String appointmentNo = request.getParameter("appointment_no");

            //Set lefthand module heading and link
            String winName = "SpecsHistory" + bean.demographicNo;
            String pathview, pathedit;

            pathview = request.getContextPath() + "/eyeform/SpecsHistory.do?method=list&demographicNo=" + bean.demographicNo;
            pathedit = request.getContextPath() + "/eyeform/SpecsHistory.do?specs.demographicNo=" + bean.demographicNo + "&specs.appointmentNo=" + appointmentNo;


            String url = "popupPage(500,900,'" + winName + "','" + pathview + "')";
            Dao.setLeftHeading(getText("global.viewSpecsHistory"));
            Dao.setLeftURL(url);

            //set right hand heading link
            winName = "AddSpecsHistory" + bean.demographicNo;
            url = "popupPage(500,600,'" + winName + "','" + pathedit + "'); return false;";
            Dao.setRightURL(url);
            Dao.setRightHeadingID(cmd); //no menu so set div id to unique id for this action

            List<EyeformSpecsHistory> shs = specsHistoryDao.getByDemographicNo(Integer.parseInt(bean.demographicNo));

            for (EyeformSpecsHistory sh : shs) {
                NavBarDisplayDAO.Item item = NavBarDisplayDAO.Item();
                item.setDate(sh.getDate());

                String title = sh.getType() + " - " + sh.getDoctor();
                String itemHeader = StringUtils.maxLenString(title, MAX_LEN_TITLE, CROP_LEN_TITLE, ELLIPSES);
                item.setTitle(itemHeader);

                item.setValue(sh.getId().toString());
                item.setLinkTitle(sh.toString3());

                int hash = Math.abs(winName.hashCode());
                url = "popupPage(500,900,'" + hash + "','" + request.getContextPath() + "/eyeform/SpecsHistory.do?specs.id=" + sh.getId() + "'); return false;";
                item.setURL(url);
                Dao.addItem(item);
            }

            //Dao.sortItems(NavBarDisplayDAO.DATESORT);
        } catch (Exception e) {
            MiscUtils.getLogger().error("Error", e);
            return false;
        }
        return true;

    }

    public String getCmd() {
        return cmd;
    }
}
