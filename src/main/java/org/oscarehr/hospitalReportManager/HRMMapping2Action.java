//CHECKSTYLE:OFF
/**
 * Copyright (c) 2008-2012 Indivica Inc.
 * <p>
 * This software is made available under the terms of the
 * GNU General Public License, Version 2, 1991 (GPLv2).
 * License details are available via "indivica.ca/gplv2"
 * and "gnu.org/licenses/gpl-2.0.html".
 */
package org.oscarehr.hospitalReportManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.oscarehr.hospitalReportManager.dao.HRMCategoryDao;
import org.oscarehr.hospitalReportManager.dao.HRMSubClassDao;
import org.oscarehr.hospitalReportManager.model.HRMSubClass;
import org.oscarehr.managers.SecurityInfoManager;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SpringUtils;

import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;

public class HRMMapping2Action extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();


    private SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);

    public String execute() {

        HRMCategoryDao hrmCategoryDao = (HRMCategoryDao) SpringUtils.getBean(HRMCategoryDao.class);
        HRMSubClassDao hrmSubClassDao = (HRMSubClassDao) SpringUtils.getBean(HRMSubClassDao.class);

        if (!securityInfoManager.hasPrivilege(LoggedInInfo.getLoggedInInfoFromSession(request), "_admin", "r", null)) {
            throw new SecurityException("missing required security object (_admin)");
        }

        try {
            if (request.getParameter("deleteMappingId") != null && request.getParameter("deleteMappingId").trim().length() > 0) {
                hrmSubClassDao.remove(Integer.parseInt(request.getParameter("deleteMappingId")));
                return SUCCESS;
            }

            String className = request.getParameter("class");
            String subClass = request.getParameter("subclass");
            String mnemonic = request.getParameter("mnemonic");
            String description = request.getParameter("description");
            String sendingFacilityId = request.getParameter("sendingFacilityId");
            String categoryId = request.getParameter("category");


            HRMSubClass hrmSubClass = new HRMSubClass();
            hrmSubClass.setClassName(className);
            hrmSubClass.setSubClassName(subClass);
            hrmSubClass.setSendingFacilityId(sendingFacilityId);
            hrmSubClass.setSubClassMnemonic(mnemonic);
            hrmSubClass.setSubClassDescription(description);
            hrmSubClass.setHrmCategory(hrmCategoryDao.findById(Integer.parseInt(categoryId)).get(0));

            hrmSubClassDao.merge(hrmSubClass);
            request.setAttribute("success", true);
        } catch (Exception e) {
            MiscUtils.getLogger().error("Couldn't set up sub class mapping", e);
            request.setAttribute("success", false);
        }

        return SUCCESS;
    }

}
