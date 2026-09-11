<%--

    Copyright (c) 2001-2002. Department of Family Medicine, McMaster University. All Rights Reserved.
    This software is published under the GPL GNU General Public License.
    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public License
    as published by the Free Software Foundation; either version 2
    of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.

    This software was written for the
    Department of Family Medicine
    McMaster University
    Hamilton
    Ontario, Canada

--%>

<%@ taglib uri="/WEB-INF/rewrite-tag.tld" prefix="rewrite" %>

<%@ page import="org.springframework.web.context.support.WebApplicationContextUtils" %>
<%@ page import="org.springframework.web.context.WebApplicationContext" %>
<%@ page import="ca.openosp.openo.utility.SpringUtils" %>
<%@ page import="ca.openosp.openo.commn.model.Tickler" %>
<%@ page import="ca.openosp.openo.commn.model.enumerator.DocumentType" %>
<%@ page import="ca.openosp.openo.documentManager.DocumentAttachmentManager" %>
<%@ page import="ca.openosp.openo.util.UtilDateUtilities" %>
<%@page import="ca.openosp.openo.utility.MiscUtils" %>
<%@ page import="ca.openosp.openo.utility.LoggedInInfo" %>
<%@ page import="ca.openosp.openo.managers.TicklerManager" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.time.LocalDateTime" %>
<%@ page import="org.owasp.encoder.Encode" %>

<%@ taglib uri="/WEB-INF/security.tld" prefix="security" %>
<%
    String roleName$ = (String) session.getAttribute("userrole") + "," + (String) session.getAttribute("user");
    boolean authed = true;
%>
<security:oscarSec roleName="<%=roleName$%>" objectName="_tickler" rights="w" reverse="<%=true%>">
    <%authed = false; %>
    <%response.sendRedirect(request.getContextPath() + "/securityError.jsp?type=_tickler");%>
</security:oscarSec>
<%
    if (!authed) {
        return;
    }
%>

<%!
    TicklerManager ticklerManager = SpringUtils.getBean(TicklerManager.class);

%>

<%
    LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
    String module = "", module_id = "", doctype = "", docdesc = "", docxml = "", doccreator = "", docdate = "", docfilename = "", docpriority = "", docassigned = "";
    module_id = request.getParameter("demographic_no");
    doccreator = request.getParameter("user_no");
    docdate = request.getParameter("xml_appointment_date");
    docfilename = request.getParameter("ticklerMessage");
    docpriority = request.getParameter("priority");
    docassigned = request.getParameter("task_assigned_to");

    String docType = request.getParameter("docType");
    String docId = request.getParameter("docId");


    Tickler tickler = new Tickler();
    tickler.setDemographicNo(Integer.parseInt(module_id));
    tickler.setUpdateDate(new java.util.Date());
    if (docpriority != null && docpriority.equalsIgnoreCase("High")) {
        tickler.setPriority(Tickler.PRIORITY.High);
    }
    if (docpriority != null && docpriority.equalsIgnoreCase("Low")) {
        tickler.setPriority(Tickler.PRIORITY.Low);
    }
    tickler.setTaskAssignedTo(docassigned);
    tickler.setCreator(doccreator);
    tickler.setMessage(docfilename);
    Date serviceDate = UtilDateUtilities.StringToDate(docdate);
    if (serviceDate == null) {
        serviceDate = new Date();
    }
    tickler.setServiceDate(serviceDate);
    tickler.setCreateDate(new Date());

    ticklerManager.addTickler(loggedInInfo, tickler);

    int ticklerNo = tickler.getId();
    if (ticklerNo > 0) {
        try {
            DocumentAttachmentManager documentAttachmentManager = SpringUtils.getBean(DocumentAttachmentManager.class);

            // Map the legacy forward-from-document docType (a table_name code: DOC / HRM / HL7 / MDS /
            // CML / ...) to a DocumentType so it can be merged into the matching picker selection.
            // Anything that is not a document or HRM report is treated as a lab.
            DocumentType forwardedType = null;
            if (docType != null && docId != null && !docType.trim().isEmpty()
                    && !docId.trim().isEmpty() && !docId.equalsIgnoreCase("null")) {
                if ("DOC".equalsIgnoreCase(docType)) {
                    forwardedType = DocumentType.DOC;
                } else if ("HRM".equalsIgnoreCase(docType)) {
                    forwardedType = DocumentType.HRM;
                } else {
                    forwardedType = DocumentType.LAB;
                }
            }

            // The interactive attachment picker (ticklerAdd.jsp) submits one parameter per document
            // type. attachToTickler performs a full per-type sync (it soft-deletes any stored doc of
            // that type not in the submitted array), so each type is synced exactly once and the
            // forwarded document is unioned into its type's list rather than attached separately.
            java.util.LinkedHashMap<DocumentType, String> attachParams = new java.util.LinkedHashMap<DocumentType, String>();
            attachParams.put(DocumentType.DOC, "docNo");
            attachParams.put(DocumentType.LAB, "labNo");
            attachParams.put(DocumentType.EFORM, "eFormNo");
            attachParams.put(DocumentType.HRM, "hrmNo");
            attachParams.put(DocumentType.FORM, "formNo");

            for (java.util.Map.Entry<DocumentType, String> entry : attachParams.entrySet()) {
                DocumentType attachmentType = entry.getKey();
                String[] picked = request.getParameterValues(entry.getValue());

                java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<String>();
                if (picked != null) {
                    for (String id : picked) {
                        if (id != null && !id.trim().isEmpty()) {
                            ids.add(id.trim());
                        }
                    }
                }
                if (forwardedType == attachmentType) {
                    ids.add(docId.trim());
                }

                // Audit the authenticated provider, not the submitted user_no, which reaches us
                // through a hidden field and so is client-controlled. Matches EditTickler2Action.
                documentAttachmentManager.attachToTickler(loggedInInfo, attachmentType,
                        ids.toArray(new String[0]), loggedInInfo.getLoggedInProviderNo(), ticklerNo, tickler.getDemographicNo());
            }
        } catch (Exception e) {
            MiscUtils.getLogger().error("Unable to attach documents to tickler " + ticklerNo, e);
        }
    }

    boolean rowsAffected = true;

    String parentAjaxId = request.getParameter("parentAjaxId");
    String updateParent = request.getParameter("updateParent");
    String updateTicklerNav = request.getParameter("updateTicklerNav");

    if (rowsAffected) {
%>
<script LANGUAGE="JavaScript">

    var parentId = "<%=Encode.forJavaScript(String.valueOf(parentAjaxId))%>";
    var updateParent = <%=Boolean.parseBoolean(updateParent)%>;
    var demo = "<%=Encode.forJavaScript(String.valueOf(module_id))%>";
    var updateTicklerNav = <%=Boolean.parseBoolean(updateTicklerNav)%>;
    var Url = window.opener.URLs;

    /*because the url for demomaintickler is truncated by the delete action, we need
      to reconstruct it if necessary
    */
    if (parentId != "" && updateParent == true && !window.opener.closed) {
        if (updateTicklerNav != "" && updateTicklerNav == true) {
            window.opener.reloadNav(parentId);
            window.close();
        } else {
            var ref = window.opener.location.href;
            if (ref.indexOf("?") > -1 && ref.indexOf("updateParent") == -1)
                ref = ref + "&updateParent=true";
            else if (ref.indexOf("?") == -1)
                ref = ref + "?demoview=" + demo + "&parentAjaxId=" + parentId + "&updateParent=true";

            window.opener.location = ref;
        }
    } else if (parentId != "" && !window.opener.closed) {
        if (window.opener.document.forms['encForm']) {
            window.opener.document.forms['encForm'].elements['reloadDiv'].value = parentId;
        }
        window.opener.updateNeeded = true;
    } else if (updateParent == true && !window.opener.closed)
        window.opener.location.reload();

    self.close();
</script>
<%}%>
