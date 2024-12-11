//CHECKSTYLE:OFF
/**
 * Copyright (c) 2008-2012 Indivica Inc.
 * <p>
 * This software is made available under the terms of the
 * GNU General Public License, Version 2, 1991 (GPLv2).
 * License details are available via "indivica.ca/gplv2"
 * and "gnu.org/licenses/gpl-2.0.html".
 * <p>
 * PrintLabs2Action.java
 * <p>
 * Created on November 27, 2007, 9:42 AM
 * Author: Adam Balanga
 * <p>
 * PrintLabs2Action.java
 * <p>
 * Created on November 27, 2007, 9:42 AM
 * Author: Adam Balanga
 */

/**
 * PrintLabs2Action.java
 *
 * Created on November 27, 2007, 9:42 AM
 * Author: Adam Balanga
 */

package oscar.oscarLab.ca.all.pageUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.oscarehr.managers.SecurityInfoManager;
import org.oscarehr.olis.OLISResults2Action;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SpringUtils;

import oscar.oscarLab.ca.all.Hl7textResultsData;
import oscar.oscarLab.ca.all.parsers.Factory;
import oscar.oscarLab.ca.all.parsers.OLISHL7Handler;

/**
 *
 * @author wrighd
 */
import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;

public class PrintOLISLabs2Action extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();


    Logger logger = org.oscarehr.util.MiscUtils.getLogger();
    private SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);

    public String execute() {

        if (!securityInfoManager.hasPrivilege(LoggedInInfo.getLoggedInInfoFromSession(request), "_lab", "r", null)) {
            throw new SecurityException("missing required security object (_lab)");
        }

        try {
            String segmentID = request.getParameter("segmentID");
            String resultUuid = request.getParameter("uuid");


            int obr = Integer.valueOf(request.getParameter("obr"));
            int obx = Integer.valueOf(request.getParameter("obx"));

            if ("true".equals(request.getParameter("showLatest"))) {
                String multiLabId = Hl7textResultsData.getMatchingLabs(segmentID);
                segmentID = multiLabId.split(",")[multiLabId.split(",").length - 1];
            }

            OLISHL7Handler handler = null;

            if (resultUuid != null && !"".equals(resultUuid)) {
                handler = OLISResults2Action.searchResultsMap.get(resultUuid);

            } else {
                handler = (OLISHL7Handler) Factory.getHandler(segmentID);
            }

            if (handler != null) {
                handler.processEncapsulatedData(request, response, obr, obx);
            }
        } catch (Exception e) {
            MiscUtils.getLogger().error("error", e);
        }
        return null;
    }
}
