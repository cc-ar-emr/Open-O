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


package org.oscarehr.provider.web;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.oscarehr.common.model.OscarLog;
import org.oscarehr.util.LoggedInInfo;

import oscar.log.LogAction;

import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;

public class CppPreferences2Action extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();


    @Override
    public String execute() {
        if ("save".equals(request.getParameter("method"))) {
            return save();
        }
        return view();
    }

    public String view() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        CppPreferencesUIBean bean = new CppPreferencesUIBean(loggedInInfo.getLoggedInProviderNo());
        bean.loadValues();
        request.setAttribute("bean", bean);
        return "form";
    }

    public String save() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        @SuppressWarnings("unchecked")
        HashMap<String, String[]> parameters = new HashMap<String, String[]>(request.getParameterMap());
        CppPreferencesUIBean bean = new CppPreferencesUIBean(loggedInInfo.getLoggedInProviderNo());
        bean.deserializeParams(parameters);
        bean.saveValues();
        bean.loadValues();

        OscarLog oscarLog = new OscarLog();
        oscarLog.setAction("SAVE_CUSTOM_CPP");
        oscarLog.setContent("");
        oscarLog.setContentId(null);
        oscarLog.setData(bean.toString());
        oscarLog.setDemographicId(null);
        oscarLog.setIp(request.getRemoteAddr());
        oscarLog.setProviderNo(loggedInInfo.getLoggedInProviderNo());
        LogAction.addLogSynchronous(oscarLog);

        request.setAttribute("bean", bean);
        return "exit";
    }
}
