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


package org.oscarehr.documentManager.actions;

import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.oscarehr.PMmodule.model.ProgramProvider;
import org.oscarehr.casemgmt.model.CaseManagementNote;
import org.oscarehr.casemgmt.model.CaseManagementNoteLink;
import org.oscarehr.casemgmt.service.CaseManagementManager;
import org.oscarehr.documentManager.EDoc;
import org.oscarehr.documentManager.EDocUtil;
import org.oscarehr.documentManager.data.AddEditDocument2Form;
import org.oscarehr.managers.ProgramManager2;
import org.oscarehr.managers.SecurityInfoManager;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SpringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import oscar.util.UtilDateUtilities;

import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;

public class AddEditHtml2Action extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();

    private SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);

    /**
     * Creates a new instance of AddLinkAction
     */
    public String execute() {

        if (!securityInfoManager.hasPrivilege(LoggedInInfo.getLoggedInInfoFromSession(request), "_edoc", "w", null)) {
            throw new SecurityException("missing required security object (_edoc)");
        }

        Hashtable errors = new Hashtable();
        String fileName = "";
        if (!EDocUtil.getDoctypes(form.getFunction()).contains(form.getDocType())) {
            EDocUtil.addDocTypeSQL(form.getDocType(), form.getFunction());
        }
        if ((form.getDocDesc().length() == 0) || (form.getDocDesc().equals("Enter Title"))) {
            errors.put("descmissing", "dms.error.descriptionInvalid");
            request.setAttribute("linkhtmlerrors", errors);
            request.setAttribute("completedForm", form);
            request.setAttribute("function", request.getParameter("function"));
            request.setAttribute("functionid", request.getParameter("functionid"));
            request.setAttribute("editDocumentNo", form.getMode());
            return "failed";
        }
        if (form.getDocType().length() == 0) {
            errors.put("typemissing", "dms.error.typeMissing");
            request.setAttribute("linkhtmlerrors", errors);
            request.setAttribute("completedForm", form);
            request.setAttribute("function", request.getParameter("function"));
            request.setAttribute("functionid", request.getParameter("functionid"));
            request.setAttribute("editDocumentNo", form.getMode());
            return "failed";
        }
        if (form.getHtml().length() == 0) {
            errors.put("urlmissing", "dms.error.htmlMissing");
            request.setAttribute("linkhtmlerrors", errors);
            request.setAttribute("completedForm", form);
            request.setAttribute("function", request.getParameter("function"));
            request.setAttribute("functionid", request.getParameter("functionid"));

            return "failed";
        }
        if (form.getMode().equals("addLink")) {
            //the 'html' variable is the url
            //checks for http://
            String html = form.getHtml();
            if (html.indexOf("http://") == -1) {
                html = "http://" + html;
            }
            html = "<script type=\"text/javascript\" language=\"Javascript\">\n" +
                    "window.location='" + html + "'\n" +
                    "</script>";
            form.setDocDesc(form.getDocDesc() + " (link)");
            form.setHtml(html);
            fileName = "link";
        } else if (form.getMode().equals("addHtml")) {
            fileName = "html";
        }

        String reviewerId = filled(form.getReviewerId()) ? form.getReviewerId() : "";
        String reviewDateTime = filled(form.getReviewDateTime()) ? form.getReviewDateTime() : "";

        if (!filled(reviewerId) && form.getReviewDoc()) {
            reviewerId = (String) request.getSession().getAttribute("user");
            reviewDateTime = UtilDateUtilities.DateToString(new Date(), EDocUtil.REVIEW_DATETIME_FORMAT);
        }
        EDoc currentDoc;
        MiscUtils.getLogger().debug("mode: " + form.getMode());
        if (form.getMode().indexOf("add") != -1) {
            currentDoc = new EDoc(form.getDocDesc(), form.getDocType(), fileName, form.getHtml(), form.getDocCreator(), form.getResponsibleId(), form.getSource(), 'H', form.getObservationDate(), reviewerId, reviewDateTime, form.getFunction(), form.getFunctionId());
            currentDoc.setContentType("text/html");
            currentDoc.setDocPublic(form.getDocPublic());
            currentDoc.setDocClass(form.getDocClass());
            currentDoc.setDocSubClass(form.getDocSubClass());

            // if the document was added in the context of a program
            ProgramManager2 programManager = SpringUtils.getBean(ProgramManager2.class);
            LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
            ProgramProvider pp = programManager.getCurrentProgramInDomain(loggedInInfo, loggedInInfo.getLoggedInProviderNo());
            if (pp != null && pp.getProgramId() != null) {
                currentDoc.setProgramId(pp.getProgramId().intValue());
            }

            String docId = EDocUtil.addDocumentSQL(currentDoc);

            /* Save annotation */
            String attrib_name = request.getParameter("annotation_attrib");
            HttpSession se = request.getSession();
            WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(se.getServletContext());
            CaseManagementManager cmm = (CaseManagementManager) ctx.getBean(CaseManagementManager.class);
            if (attrib_name != null) {
                CaseManagementNote cmn = (CaseManagementNote) se.getAttribute(attrib_name);
                if (cmn != null) {
                    cmm.saveNoteSimple(cmn);
                    CaseManagementNoteLink cml = new CaseManagementNoteLink();
                    cml.setTableName(CaseManagementNoteLink.DOCUMENT);
                    cml.setTableId(Long.valueOf(docId));
                    cml.setNoteId(cmn.getId());
                    cmm.saveNoteLink(cml);

                    se.removeAttribute(attrib_name);
                }
            }
        } else {
            currentDoc = new EDoc(form.getDocDesc(), form.getDocType(), "", form.getHtml(), form.getDocCreator(), form.getResponsibleId(), form.getSource(), 'H', form.getObservationDate(), reviewerId, reviewDateTime, form.getFunction(), form.getFunctionId());
            currentDoc.setDocId(form.getMode());
            currentDoc.setContentType("text/html");
            currentDoc.setDocPublic(form.getDocPublic());
            currentDoc.setDocClass(form.getDocClass());
            currentDoc.setDocSubClass(form.getDocSubClass());
            EDocUtil.editDocumentSQL(currentDoc, form.getReviewDoc());
        }
        StringBuffer redirect = new StringBuffer("/documentManager/documentReport.jsp");
        redirect.append("?function=").append(request.getParameter("function"));
        redirect.append("&functionid=").append(request.getParameter("functionid"));
        try {
            response.sendRedirect(redirect.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return NONE;
    }

    private boolean filled(String s) {
        return (s != null && s.trim().length() > 0);
    }

    private AddEditDocument2Form form;

    public AddEditDocument2Form getForm() {
        return form;
    }

    public void setForm(AddEditDocument2Form form) {
        this.form = form;
    }
}
