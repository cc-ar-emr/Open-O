/**
 * Copyright (c) 2006-. OSCARservice, OpenSoft System. All Rights Reserved.
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
 */

package oscar.appt.status.web;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.oscarehr.common.model.AppointmentStatus;
import org.oscarehr.util.MiscUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import oscar.appt.status.service.AppointmentStatusMgr;
import oscar.appt.status.service.impl.AppointmentStatusMgrImpl;

import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;

public class AppointmentStatus2Action extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();


    private static final Logger logger = MiscUtils.getLogger();

    public String view() {
        logger.warn("view");
        populateAllStatus(request);
        return SUCCESS;
    }

    public String reset() {
        logger.warn("reset");
        AppointmentStatusMgr apptStatusMgr = getApptStatusMgr();
        apptStatusMgr.reset();
        populateAllStatus(request);
        return SUCCESS;
    }

    public String changestatus() {
        logger.warn("changestatus");
        AppointmentStatusMgr apptStatusMgr = getApptStatusMgr();
        int ID = Integer.parseInt(request.getParameter("statusID"));
        int iActive = Integer.parseInt(request.getParameter("iActive"));
        apptStatusMgr.changeStatus(ID, iActive);
        populateAllStatus(request);
        return SUCCESS;
    }

    public String modify() {
        logger.warn("modify");
        AppointmentStatusMgr apptStatusMgr = getApptStatusMgr();
        int ID = Integer.parseInt(request.getParameter("statusID"));
        AppointmentStatus appt = apptStatusMgr.getStatus(ID);

        this.setID(ID);
        this.setApptStatus(appt.getStatus());
        this.setApptDesc(appt.getDescription());
        this.setApptOldColor(appt.getColor());

        return "edit";
    }

    public String update() {
        logger.warn("update");
        AppointmentStatusMgr apptStatusMgr = getApptStatusMgr();

        int ID = this.getID();
        String strDesc = this.getApptDesc();
        String strColor = this.getApptStatus();
        if (null == strColor || strColor.equals(""))
            strColor = this.getApptOldColor();
        apptStatusMgr.modifyStatus(ID, strDesc, strColor);
        populateAllStatus(request);
        return SUCCESS;
    }

    public WebApplicationContext getApptContext() {
        return WebApplicationContextUtils.getRequiredWebApplicationContext(ServletActionContext.getServletContext());
    }

    public AppointmentStatusMgr getApptStatusMgr() {
        return new AppointmentStatusMgrImpl();
    }

    private void populateAllStatus(HttpServletRequest request) {
        AppointmentStatusMgr apptStatusMgr = getApptStatusMgr();
        List allStatus = apptStatusMgr.getAllStatus();
        request.setAttribute("allStatus", allStatus);
        int iUseStatus = apptStatusMgr.checkStatusUsuage(allStatus);
        if (iUseStatus > 0) {
            request.setAttribute("useStatus", apptStatusMgr.getStatus(iUseStatus + 1).getStatus());
        }
    }

    private int ID;
    private String apptStatus;
    private String apptDesc;
    private String apptOldColor;

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getApptStatus() {
        return apptStatus;
    }

    public void setApptStatus(String apptStatus) {
        this.apptStatus = apptStatus;
    }

    public String getApptDesc() {
        return apptDesc;
    }

    public void setApptDesc(String apptDesc) {
        this.apptDesc = apptDesc;
    }

    public String getApptOldColor() {
        return apptOldColor;
    }

    public void setApptOldColor(String apptOldColor) {
        this.apptOldColor = apptOldColor;
    }
}
