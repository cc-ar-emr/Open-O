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

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import org.oscarehr.documentManager.EDocUtil;
import org.oscarehr.managers.SecurityInfoManager;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SpringUtils;

import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;

public class AddDocumentType2Action extends ActionSupport {
    private HttpServletRequest request = ServletActionContext.getRequest();
    private SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);

    @Override
    public String execute() {
        if (!securityInfoManager.hasPrivilege(LoggedInInfo.getLoggedInInfoFromSession(request), "_admin,_admin.document", "r", null)) {
            throw new SecurityException("missing required security object (_admin,_admin.document)");
        }
        HashMap<String, String> errors = new HashMap<>();
        boolean doctypeadded = false;
        if (docType.length() == 0) {
            errors.put("typemissing", "dms.error.typeMissing");
            request.setAttribute("doctypeerrors", errors);
            return "failed";
        } else if (function.length() == 0) {
            errors.put("modulemissing", "dms.error.moduleMissing");
            request.setAttribute("doctypeerrors", errors);
            return "failed";
        }
        // If a new document type is added, include it in the database to create filters
        if (!EDocUtil.getDoctypes(function).contains(docType)) {
            EDocUtil.addDocTypeSQL(docType, function, "A");
            doctypeadded = true;
            MiscUtils.getLogger().info("new Doc Type added " + doctypeadded);
            return SUCCESS;
        }
        return "failed";
    }

    private String function = "";
    private String docType = "";

    public String getFunction() {
        return function;
    }

    public String getDocType() {
        return docType;
    }


    public void setFunction(String function) {
        this.function = function;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }
}
