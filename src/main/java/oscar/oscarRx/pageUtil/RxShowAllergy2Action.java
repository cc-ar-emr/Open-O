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


package oscar.oscarRx.pageUtil;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.oscarehr.common.dao.AllergyDao;
import org.oscarehr.common.dao.UserPropertyDAO;
import org.oscarehr.common.model.Allergy;
import org.oscarehr.common.model.UserProperty;
import org.oscarehr.managers.SecurityInfoManager;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SpringUtils;

import oscar.OscarProperties;
import oscar.oscarRx.data.RxPatientData;

import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;

public final class RxShowAllergy2Action extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();

    private SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);

    private AllergyDao allergyDao = (AllergyDao) SpringUtils.getBean(AllergyDao.class);


    public String reorder() {
        // Validate demographic number before processing
        String demographicNo = request.getParameter("demographicNo");
        if (demographicNo == null || !demographicNo.matches("^\\d+$")) {
            MiscUtils.getLogger().error("Invalid demographic number in reorder: " + demographicNo);
            return ERROR;
        }
        
        reorder(request);
        //ActionForward fwd = mapping.findForward("success-redirect");
        try {
            response.sendRedirect("/oscarRx/ShowAllergies.jsp?demographicNo=" + demographicNo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return NONE;
    }

    public String unspecified()
            throws IOException, ServletException {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        if (!securityInfoManager.hasPrivilege(loggedInInfo, "_allergy", "r", null)) {
            throw new RuntimeException("missing required security object (_allergy)");
        }

        boolean useRx3 = false;
        String rx3 = OscarProperties.getInstance().getProperty("RX3");
        if (rx3 != null && rx3.equalsIgnoreCase("yes")) {
            useRx3 = true;
        }
        UserPropertyDAO userPropertyDAO = (UserPropertyDAO) SpringUtils.getBean(UserPropertyDAO.class);
        String provider = (String) request.getSession().getAttribute("user");
        UserProperty propUseRx3 = userPropertyDAO.getProp(provider, UserProperty.RX_USE_RX3);
        if (propUseRx3 != null && propUseRx3.getValue().equalsIgnoreCase("yes"))
            useRx3 = true;


        String sanitizedUserNo = StringEscapeUtils.escapeHtml4(user_no);
        String sanitizedDemoNo = StringEscapeUtils.escapeHtml4(demo_no);
        String sanitizedView = view != null ? StringEscapeUtils.escapeHtml4(view) : null;

        // Setup bean
        RxSessionBean bean;

        if (request.getSession().getAttribute("RxSessionBean") != null) {
            bean = (oscar.oscarRx.pageUtil.RxSessionBean) request.getSession().getAttribute("RxSessionBean");
            if (!bean.getProviderNo().equals(sanitizedUserNo) || bean.getDemographicNo() != Integer.parseInt(sanitizedDemoNo)) {
                bean = new RxSessionBean();
            }
        } else {
            bean = new RxSessionBean();
        }

        bean.setProviderNo(sanitizedUserNo);
        bean.setDemographicNo(Integer.parseInt(sanitizedDemoNo));
        if (sanitizedView != null) {
            bean.setView(sanitizedView);
        }

        request.getSession().setAttribute("RxSessionBean", bean);

        if (request.getParameter("method") != null && request.getParameter("method").equals("reorder")) {
            reorder(request);
        }

        RxPatientData.Patient patient = RxPatientData.getPatient(loggedInInfo, bean.getDemographicNo());

        String forward = "/oscarRx/ShowAllergies.jsp";
        if (useRx3) {
            forward = "/oscarRx/ShowAllergies2.jsp";
        }
        if (patient != null) {
            request.getSession().setAttribute("Patient", patient);
            response.sendRedirect(forward);
        } else {
            response.sendRedirect("error.html");
        }
        return null;
    }

    private void reorder(HttpServletRequest request) {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);

        String direction = request.getParameter("direction");
        String demographicNo = request.getParameter("demographicNo");
        String allergyIdStr = request.getParameter("allergyId");
        
        // Validate input parameters
        if (direction == null || (!direction.equals("up") && !direction.equals("down"))) {
            MiscUtils.getLogger().error("Invalid direction parameter: " + direction);
            return;
        }
        
        if (demographicNo == null || !demographicNo.matches("^\\d+$")) {
            MiscUtils.getLogger().error("Invalid demographic number: " + demographicNo);
            return;
        }
        
        if (allergyIdStr == null || !allergyIdStr.matches("^\\d+$")) {
            MiscUtils.getLogger().error("Invalid allergy ID: " + allergyIdStr);
            return;
        }
        
        int allergyId = Integer.parseInt(allergyIdStr);
        try {
            Allergy[] allergies = RxPatientData.getPatient(loggedInInfo, demographicNo).getActiveAllergies();
            for (int x = 0; x < allergies.length; x++) {
                if (allergies[x].getAllergyId() == allergyId) {
                    if (direction.equals("up")) {
                        if (x == 0) {
                            continue;
                        }
                        //move ahead
                        int myPosition = allergies[x].getPosition();
                        int swapPosition = allergies[x - 1].getPosition();
                        allergies[x].setPosition(swapPosition);
                        allergies[x - 1].setPosition(myPosition);
                        allergyDao.merge(allergies[x]);
                        allergyDao.merge(allergies[x - 1]);
                    }
                    if (direction.equals("down")) {
                        if (x == (allergies.length - 1)) {
                            continue;
                        }
                        int myPosition = allergies[x].getPosition();
                        int swapPosition = allergies[x + 1].getPosition();
                        allergies[x].setPosition(swapPosition);
                        allergies[x + 1].setPosition(myPosition);
                        allergyDao.merge(allergies[x]);
                        allergyDao.merge(allergies[x + 1]);
                    }
                }
            }

        } catch (Exception e) {
            MiscUtils.getLogger().error("error", e);
        }

    }
}
