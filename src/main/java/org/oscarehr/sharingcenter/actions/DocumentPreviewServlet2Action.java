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

import org.oscarehr.sharingcenter.DocumentType;

import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;

public class DocumentPreviewServlet2Action extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();



    public String execute() throws Exception {
        // get parameters
        String docType = request.getParameter("doc_type");
        String document = request.getParameter("doc_no");
        String provider = request.getParameter("prov_no");

        // figure out the destination by the document type
        String destination = "";
        if (DocumentType.CDS.name().equalsIgnoreCase(docType)) {
            destination = String.format(request.getContextPath() + "/documentManager/ManageDocument.do?method=downloadCDS&doc_no=%s", document);

        } else if (DocumentType.XPHR.name().equalsIgnoreCase(docType) || DocumentType.NEXJ.name().equalsIgnoreCase(docType)) {
            destination = String.format("cdaExport.jsp?document_no=%s", document);

        } else if ("eforms".equalsIgnoreCase(docType)) {
            destination = String.format(request.getContextPath() + "/eform/efmshowform_data.jsp?fdid=%s", document);

        } else if ("edocs".equalsIgnoreCase(docType)) {
            destination = String.format(request.getContextPath() + "/documentManager/ManageDocument.do?method=display&doc_no=%s&providerNo=", document, provider);

        }

        response.sendRedirect(destination);

        return null;
    }

}
