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


package oscar.form.study;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.oscarehr.managers.SecurityInfoManager;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.SpringUtils;

import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;

public final class FrmStudy2Action extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();

    private SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);

    public String execute()
            throws ServletException, IOException {

        if (!securityInfoManager.hasPrivilege(LoggedInInfo.getLoggedInInfoFromSession(request), "_form", "w", null)) {
            throw new SecurityException("missing required security object (_form)");
        }

        int newID = 0;
        FrmStudyRecord rec = null;
        String where = "";
        try {
            FrmStudyRecordFactory recorder = new FrmStudyRecordFactory();
            rec = recorder.factory(request.getParameter("study_name"));
            Properties props = new Properties();
            String name;
            for (Enumeration varEnum = request.getParameterNames(); varEnum.hasMoreElements(); props.setProperty(name, request.getParameter(name)))
                name = (String) varEnum.nextElement();

            newID = rec.saveFormRecord(props);

            String strAction = rec.findActionValue(request.getParameter("submit"));

            if("exit".equals(strAction)) {
                where = "/oscarEncounter/close.jsp";
            } else if("print".equals(strAction)) {
                where = "/oscarEncounter/formType2DiabetesPrint.jsp";
            } else if("save".equals(strAction)) {
                where = "/form/study/forwardstudyname.jsp";
            } else if("failure".equals(strAction)) {
                where = "/oscarEncounter/error.jsp";
            }
            where = rec.createActionURL(where, strAction, request.getParameter("demographic_no"), "" + newID, request.getParameter("study_no"), request.getParameter("study_link"));

        } catch (Exception ex) {
            throw new ServletException(ex);
        }

        response.sendRedirect(where);
        return NONE;
    }



/*        int newID = 0;
        try {
            EctType2DiabetesRecord rec = new EctType2DiabetesRecord();
            Properties props = new Properties();
            String name;
            for(Enumeration varEnum = request.getParameterNames(); varEnum.hasMoreElements(); props.setProperty(name, request.getParameter(name)))
                name = (String)varEnum.nextElement();

            newID = rec.saveType2DiabetesRecord(props);
        } catch(SQLException ex) {
            throw new ServletException(ex);
        }
        String where = "";
        if(request.getParameter("submit").equalsIgnoreCase("print")) {
            ActionForward af = mapping.findForward("print");
            where = af.getPath();
            where = where+"?demoNo="+request.getParameter("demographic_no")+"&formId="+newID;
        } else if(request.getParameter("submit").equalsIgnoreCase("save")) {
            ActionForward af = mapping.findForward("save");
            where = af.getPath();
            where = where+"?demographic_no="+request.getParameter("demographic_no")+"&formId="+newID;
        } else if(request.getParameter("submit").equalsIgnoreCase("exit")) {
            ActionForward af = mapping.findForward("exit");
            where = af.getPath();
        } else {
            ActionForward af = mapping.findForward("failure");
            where = af.getPath();
        }
        return new ActionForward(where);
    }
*/
}
