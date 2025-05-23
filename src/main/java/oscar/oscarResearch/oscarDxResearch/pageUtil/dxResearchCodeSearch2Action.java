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


package oscar.oscarResearch.oscarDxResearch.pageUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.oscarehr.managers.SecurityInfoManager;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.SpringUtils;

import oscar.oscarResearch.oscarDxResearch.bean.dxCodeSearchBeanHandler;

import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;

public final class dxResearchCodeSearch2Action extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();

    private static SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);

    public String execute()
            throws Exception {

        if (!securityInfoManager.hasPrivilege(LoggedInInfo.getLoggedInInfoFromSession(request), "_dxresearch", "r", null)) {
            throw new RuntimeException("missing required security object (_dxresearch)");
        }

        //String demographicNo = request.getParameter("demographicNo");
        String[] xml_research = new String[5];
        xml_research[0] = request.getParameter("xml_research1");
        xml_research[1] = request.getParameter("xml_research2");
        xml_research[2] = request.getParameter("xml_research3");
        xml_research[3] = request.getParameter("xml_research4");
        xml_research[4] = request.getParameter("xml_research5");
        String codeType = request.getParameter("codeType");

        dxCodeSearchBeanHandler hd = new dxCodeSearchBeanHandler(codeType, xml_research);
        HttpSession session = request.getSession();
        session.setAttribute("allMatchedCodes", hd);
        session.setAttribute("codeType", codeType);

        return SUCCESS;
    }
}
