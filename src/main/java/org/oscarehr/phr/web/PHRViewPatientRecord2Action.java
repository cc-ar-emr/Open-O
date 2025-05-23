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


/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.oscarehr.phr.web;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.oscarehr.common.dao.DemographicDao;
import org.oscarehr.common.dao.PHRVerificationDao;
import org.oscarehr.common.model.Demographic;
import org.oscarehr.managers.DemographicManager;
import org.oscarehr.managers.SecurityInfoManager;
import org.oscarehr.myoscar.client.ws_manager.AccountManager;
import org.oscarehr.myoscar.utils.MyOscarLoggedInInfo;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.SpringUtils;

import oscar.OscarProperties;
import oscar.log.LogAction;
import oscar.log.LogConst;
import oscar.oscarDemographic.data.DemographicData;
import oscar.util.UtilDateUtilities;

/**
 * @author apavel
 */
import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;

public class PHRViewPatientRecord2Action extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();


    private SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);

    @Override
    public String execute() throws Exception {
        if ("viewMyOscarRecord".equals(request.getParameter("method"))) {
            return viewMyOscarRecord();
        }
        if ("saveNewVerification".equals(request.getParameter("method"))) {
            return saveNewVerification();
        }
        return super.execute();
    }

    public String viewMyOscarRecord() throws Exception {
        String demographicNo = request.getParameter("demographic_no");
        if (demographicNo == null) { //shouldn't really happy, but just in case
            response.getWriter().println("Error: Lost demographic number.  Please try again");
            return null;
        }

        if (!securityInfoManager.hasPrivilege(LoggedInInfo.getLoggedInInfoFromSession(request), "_demographic", "r", demographicNo)) {
            throw new SecurityException("missing required security object (_demgoraphic)");
        }

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        MyOscarLoggedInInfo myOscarLoggedInInfo = MyOscarLoggedInInfo.getLoggedInInfo(request.getSession());

        if (myOscarLoggedInInfo == null || !myOscarLoggedInInfo.isLoggedIn()) {
            request.setAttribute("forwardToOnSuccess", request.getContextPath() + "/demographic/viewPhrRecord.do?demographic_no=" + demographicNo);
            return "login";
        } else {

            //Check if patient has been verified
            DemographicManager demographicManager = SpringUtils.getBean(DemographicManager.class);

            if (!demographicManager.getPhrVerificationLevelByDemographicId(loggedInInfo, Integer.parseInt(demographicNo))) { //Prompt for verification
                request.setAttribute("forwardToOnSuccess", "/demographic/viewPhrRecord.do?demographic_no=" + demographicNo);
                request.setAttribute("demographicNo", demographicNo);
                return "viewVerification";//verifyAndRedirect");

            }

            String phrPath = OscarProperties.getInstance().getProperty("myOSCAR.url"); //http://130.113.106.96:8080/myoscar_client/
            if (phrPath == null) {
                response.getWriter().println("Error: 'myOSCAR.url' property is not configured... please contact support to have this feature enabled");
                return null;
            }

            DemographicDao demographicDao = (DemographicDao) SpringUtils.getBean(DemographicDao.class);
            Demographic demographic = demographicDao.getDemographicById(Integer.parseInt(demographicNo));
            Long myOscarUserId = AccountManager.getUserId(myOscarLoggedInInfo, demographic.getMyOscarUserName());

            LogAction.addLog((String) request.getSession().getAttribute("user"), LogConst.READ, LogConst.CON_PHR, String.valueOf(myOscarUserId), request.getRemoteAddr(), demographicNo, "");
//            request.setAttribute("userid", auth.getUserId());
//            request.setAttribute("ticket", auth.getToken());
            request.setAttribute("userName", myOscarLoggedInInfo.getLoggedInPerson().getUserName());
            request.setAttribute("password", myOscarLoggedInInfo.getLoggedInPersonSecurityToken());
            request.setAttribute("viewpatient", myOscarUserId);
            request.setAttribute("url", phrPath + (phrPath.endsWith("/") ? "" : "/") + "patient_view_action.jsp");
            return "phr";
        }

    }


    public String saveNewVerification() throws Exception {
        String demographicNo = request.getParameter("demographic_no");
        PHRVerificationDao phrVerificationDao = (PHRVerificationDao) SpringUtils.getBean(PHRVerificationDao.class);

        org.oscarehr.common.model.PHRVerification phrA = new org.oscarehr.common.model.PHRVerification();

        boolean photoId = false;
        try {
            photoId = Boolean.parseBoolean(request.getParameter("photoId"));
        } catch (Exception e) {/*anything but true should be considered false*/}

        boolean parentGuardian = false;
        try {
            parentGuardian = Boolean.parseBoolean(request.getParameter("parentGuardian"));
        } catch (Exception e2) {/*Anything but true should be considered false*/}


        Date verificationDate = null;
        try {
            verificationDate = UtilDateUtilities.getDateFromString(request.getParameter("verificationDate"), "yyyy-MM-dd");
        } catch (Exception e) {
            verificationDate = new Date();
        }

        DemographicData demographicData = new DemographicData();
        phrA.setPhrUserName(demographicData.getDemographic(LoggedInInfo.getLoggedInInfoFromSession(request), demographicNo).getMyOscarUserName());
        phrA.setVerificationLevel(request.getParameter("verificationLevel"));
        phrA.setVerificationDate(verificationDate);// fix me
        phrA.setPhotoId(photoId);
        phrA.setParentGuardian(parentGuardian);
        phrA.setComments(request.getParameter("comments"));

        phrA.setDemographicNo(Integer.parseInt(demographicNo));
        phrA.setVerificationBy((String) request.getSession().getAttribute("user"));
        phrA.setArchived(false);
        phrA.setCreatedDate(new java.util.Date());
        phrVerificationDao.persist(phrA);

        LogAction.addLog((String) request.getSession().getAttribute("user"), LogConst.VERIFY, LogConst.CON_PHR, phrA.getPhrUserName(), request.getRemoteAddr(), demographicNo, "");

        String url = "/phr/PHRVerification.jsp?demographic_no=" + demographicNo;

        if (request.getParameter("forwardToOnSuccess") != null) {
            url = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + request.getParameter("forwardToOnSuccess");
        }
        response.sendRedirect(url);
        return NONE;
    }
}
