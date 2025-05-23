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
package org.oscarehr.sharingcenter.actions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.marc.shic.cda.level1.PhrExtractDocument;
import org.marc.shic.cda.utils.CdaUtils;
import org.oscarehr.common.dao.DemographicDao;
import org.oscarehr.managers.SecurityInfoManager;
import org.oscarehr.sharingcenter.DocumentType;
import org.oscarehr.sharingcenter.dao.DemographicExportDao;
import org.oscarehr.sharingcenter.model.DemographicExport;
import org.oscarehr.sharingcenter.util.CDADocumentUtil;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.SpringUtils;

/**
 * The backend servlet to handle the export process for documents.
 *
 * @author oscar
 */
import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;

public class DemographicExportServlet2Action extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();


    private SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);

    @Override
    public String execute() throws Exception {

        HttpSession session = request.getSession();

        String demographicNo = request.getParameter("demographic_no");

        if (!securityInfoManager.hasPrivilege(LoggedInInfo.getLoggedInInfoFromSession(request), "_demographic", "r", null)) {
            throw new SecurityException("missing required security object (_demographic)");
        }

        String documentType = request.getParameter("documentType");

        String affinityDomain = request.getParameter("affinityDomain");

        PhrExtractDocument phrExtractDocument = CDADocumentUtil.createDoc(demographicNo, (String) session.getAttribute("user"));

        String documentExportString = null;

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("<?xml version='1.0' encoding='utf-8'?>");

        String document = CdaUtils.toXmlString(phrExtractDocument.getDocument(), true);

        stringBuilder.append(document);

        documentExportString = stringBuilder.toString();

        DemographicExportDao demographicExportDao = SpringUtils.getBean(DemographicExportDao.class);
        DemographicDao demographicDao = SpringUtils.getBean(DemographicDao.class);

        DemographicExport demographicExport = new DemographicExport();

        demographicExport.setDemographic(demographicDao.getDemographic(demographicNo));

        if (documentType.equals(DocumentType.XPHR.name())) {
            demographicExport.setDocumentType(DocumentType.XPHR.name());
        } else if (documentType.equals(DocumentType.NEXJ.name())) {
            demographicExport.setDocumentType(DocumentType.NEXJ.name());
        }

        demographicExport.setDocument(documentExportString.getBytes());

        DemographicExport export = demographicExportDao.saveEntity(demographicExport);

        response.sendRedirect("export.jsp?domain=" + affinityDomain + "&type=" + documentType + "&demographic_no=" + demographicNo + "&documents=" + export.getId());

        return null;
    }
}
