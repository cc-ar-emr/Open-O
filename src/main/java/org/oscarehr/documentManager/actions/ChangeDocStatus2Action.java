//CHECKSTYLE:OFF
/**
 * Copyright (c) 2008-2012 Indivica Inc.
 * <p>
 * This software is made available under the terms of the
 * GNU General Public License, Version 2, 1991 (GPLv2).
 * License details are available via "indivica.ca/gplv2"
 * and "gnu.org/licenses/gpl-2.0.html".
 */

package org.oscarehr.documentManager.actions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.oscarehr.documentManager.EDocUtil;
import org.oscarehr.managers.SecurityInfoManager;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.SpringUtils;

import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;

public class ChangeDocStatus2Action extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();


    private SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);

    public String execute() {
        if (!securityInfoManager.hasPrivilege(LoggedInInfo.getLoggedInInfoFromSession(request), "_edoc", "w", null)) {
            throw new SecurityException("missing required security object (_edoc)");
        }

        if (!StringUtils.isEmpty(docTypeD)  && !StringUtils.isEmpty(statusD)) {
            EDocUtil.changeDocTypeStatusSQL(docTypeD, "Demographic", statusD);
        }
        if (!StringUtils.isEmpty(docTypeP) && !StringUtils.isEmpty(statusP)) {
            EDocUtil.changeDocTypeStatusSQL(docTypeP, "Provider", statusP);
        }

        return SUCCESS;
    }

    private String docTypeD = "";
    private String docTypeP = "";
    private String statusD = "";
    private String statusP = "";

    public String getDocTypeD() {
        return docTypeD;
    }

    public String getDocTypeP() {
        return docTypeP;
    }

    public String getStatusD() {
        return statusD;
    }

    public String getStatusP() {
        return statusP;
    }

    public void setDocTypeD(String docTypeD) {
        this.docTypeD = docTypeD;
    }

    public void setDocTypeP(String docTypeP) {
        this.docTypeP = docTypeP;
    }

    public void setStatusD(String statusD) {
        this.statusD = statusD;
    }

    public void setStatusP(String statusP) {
        this.statusP = statusP;
    }

}
