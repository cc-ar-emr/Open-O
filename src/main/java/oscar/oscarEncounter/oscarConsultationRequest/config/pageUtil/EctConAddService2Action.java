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


package oscar.oscarEncounter.oscarConsultationRequest.config.pageUtil;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.oscarehr.common.dao.ConsultationServiceDao;
import org.oscarehr.common.model.ConsultationServices;
import org.oscarehr.managers.SecurityInfoManager;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.SpringUtils;

import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;

public class EctConAddService2Action extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();

    private ConsultationServiceDao consultationServiceDao = (ConsultationServiceDao) SpringUtils.getBean(ConsultationServiceDao.class);
    private static SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);


    public String execute()
            throws ServletException, IOException {
        if (!securityInfoManager.hasPrivilege(LoggedInInfo.getLoggedInInfoFromSession(request), "_con", "w", null)) {
            throw new SecurityException("missing required security object (_con)");
        }

        ConsultationServices cs = new ConsultationServices();
        cs.setActive(consultationServiceDao.ACTIVE);
        cs.setServiceDesc(service);
        consultationServiceDao.persist(cs);
        request.setAttribute("SERVADD", service);

        EctConConstructSpecialistsScriptsFile constructSpecialistsScriptsFile = new EctConConstructSpecialistsScriptsFile();
        constructSpecialistsScriptsFile.makeString(request.getLocale());
        return SUCCESS;
    }

    public String getService() {
        if (service == null)
            service = new String();
        return service;
    }

    public void setService(String str) {
        service = str;
    }

    String service;
}
