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


package org.oscarehr.common.web;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.Spring;

import org.apache.logging.log4j.Logger;
import org.oscarehr.common.dao.FlowSheetDrugDao;
import org.oscarehr.common.dao.FlowSheetDxDao;
import org.oscarehr.common.model.FlowSheetDrug;
import org.oscarehr.common.model.FlowSheetDx;
import org.oscarehr.managers.SecurityInfoManager;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SpringUtils;

import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;

public class FlowSheetDrug2Action extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();

    private static final Logger log2 = MiscUtils.getLogger();
    private FlowSheetDrugDao flowSheetDrugDao = SpringUtils.getBean(FlowSheetDrugDao.class);
    private FlowSheetDxDao flowSheetDxDao = SpringUtils.getBean(FlowSheetDxDao.class);
    private SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);

    @Override
    public String execute() throws Exception {
        if ("save".equals(request.getParameter("method"))) {
            return save();
        }
        if ("dxSave".equals(request.getParameter("method"))) {
            return dxSave();
        }
        log2.debug("AnnotationAction-unspec");
        //return setup();
        return null;
    }

    public String save() throws Exception {
        String flowsheet = request.getParameter("flowsheet");
        String demographicNo = request.getParameter("demographic");
        String atcCode = request.getParameter("atcCode");

        FlowSheetDrug cust = new FlowSheetDrug();
        cust.setFlowsheet(flowsheet);
        cust.setAtcCode(atcCode);
        cust.setProviderNo((String) request.getSession().getAttribute("user"));
        cust.setDemographicNo(Integer.parseInt(demographicNo));
        cust.setCreateDate(new Date());

        if (!securityInfoManager.hasPrivilege(LoggedInInfo.getLoggedInInfoFromSession(request), "_demographic", "w", demographicNo)) {
            throw new SecurityException("missing required security object (_demographic)");
        }
        log2.debug("SAVE " + cust);

        flowSheetDrugDao.persist(cust);

        /*ActionForward ff = mapping.findForward("success");
        //ff.setRedirect(true);
        ActionForward af = new ActionForward(ff.getPath() + "?demographic_no=" + demographicNo + "&template=" + flowsheet, true);


        //request.setAttribute("demographic", demographicNo);
        //request.setAttribute("flowsheet", flowsheet);
        return af;*/
        return "/oscarEncounter/oscarMeasurements/TemplateFlowSheet.jsp?demographic_no=" + demographicNo + "&template=" + flowsheet;
    }

    public String dxSave() throws Exception {
        String flowsheet = request.getParameter("flowsheet");
        String demographicNo = request.getParameter("demographic");
        String dxCode = request.getParameter("dxCode");
        String dxType = request.getParameter("dxCodeType");

        FlowSheetDx cust = new FlowSheetDx();
        cust.setFlowsheet(flowsheet);
        cust.setDxCode(dxCode);
        cust.setDxCodeType(dxType);
        cust.setProviderNo((String) request.getSession().getAttribute("user"));
        cust.setDemographicNo(Integer.parseInt(demographicNo));
        cust.setCreateDate(new Date());

        log2.debug("SAVE " + cust);

        if (!securityInfoManager.hasPrivilege(LoggedInInfo.getLoggedInInfoFromSession(request), "_demographic", "w", demographicNo)) {
            throw new SecurityException("missing required security object (_demographic)");
        }

        flowSheetDxDao.persist(cust);

        /*ActionForward ff = mapping.findForward("success");
        //ff.setRedirect(true);
        ActionForward af = new ActionForward(ff.getPath() + "?demographic_no=" + demographicNo + "&template=" + flowsheet, true);


        //request.setAttribute("demographic", demographicNo);
        //request.setAttribute("flowsheet", flowsheet);
        return af;*/
        return "/oscarEncounter/oscarMeasurements/TemplateFlowSheet.jsp?demographic_no=" + demographicNo + "&template=" + flowsheet;
    }


}
