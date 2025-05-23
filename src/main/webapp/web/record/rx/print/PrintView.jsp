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
<%@page import="oscar.oscarRx.data.RxPatientData" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="/WEB-INF/oscarProperties-tag.tld" prefix="oscar" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page import="oscar.oscarProvider.data.ProSignatureData, oscar.oscarProvider.data.ProviderData" %>
<%@ page import="oscar.log.*,oscar.oscarRx.data.*" %>
<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<%@ page import="org.apache.logging.log4j.Logger,java.util.List" %>
<%@ page import="org.apache.logging.log4j.LogManager" %>

<%@ page import="oscar.*,
                 java.lang.*,
                 java.util.Date,
                 java.text.SimpleDateFormat,
                 oscar.oscarRx.util.RxUtil,
                 org.springframework.web.context.WebApplicationContext,
                 org.springframework.web.context.support.WebApplicationContextUtils,
                 org.oscarehr.common.dao.UserPropertyDAO,
                 org.oscarehr.common.model.UserProperty" %>
<%@ page import="org.oscarehr.util.SpringUtils" %>

<!-- Classes needed for signature injection -->
<%@page import="org.oscarehr.util.SessionConstants" %>
<%@page import="org.oscarehr.common.dao.*,org.oscarehr.managers.PrescriptionManager" %>
<%@page import="org.oscarehr.common.model.*" %>
<%@page import="org.oscarehr.util.LoggedInInfo" %>
<%@page import="org.oscarehr.util.DigitalSignatureUtils" %>
<%@page import="org.oscarehr.ui.servlet.ImageRenderingServlet" %>
<!-- end -->
<%@ page import="org.owasp.encoder.Encode" %>
<%
    LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
    String providerNo = loggedInInfo.getLoggedInProviderNo();
    String scriptId = request.getParameter("scriptId");
    String rx_enhance = OscarProperties.getInstance().getProperty("rx_enhance");
    PrescriptionManager prescriptionManager = SpringUtils.getBean(PrescriptionManager.class);
    Prescription prescription = prescriptionManager.getPrescription(loggedInInfo, Integer.parseInt(scriptId));
%>


<%@page import="org.oscarehr.web.PrescriptionQrCodeUIBean" %>

<%@ taglib uri="/WEB-INF/security.tld" prefix="security" %>
<%
    String roleName$ = (String) session.getAttribute("userrole") + "," + (String) session.getAttribute("user");
    boolean authed = true;
%>
<security:oscarSec roleName="<%=roleName$%>" objectName="_rx" rights="r" reverse="<%=true%>">
    <%authed = false; %>
    <%response.sendRedirect("../securityError.jsp?type=_rx");%>
</security:oscarSec>
<%
    if (!authed) {
        return;
    }
%>


<html locale="true">
<head>
    <script type="text/javascript" src="<%= request.getContextPath() %>/js/global.js"></script>
    <script type="text/javascript" src="<%= request.getContextPath() %>/share/javascript/prototype.js"></script>
    <script type="text/javascript" src="<%= request.getContextPath() %>/share/javascript/Oscar.js"/>
    </script>
    <title><fmt:setBundle basename="oscarResources"/><fmt:message key="RxPreview.title"/></title>
    <style type="text/css" media="print">
        .noprint {
            display: none;
        }
    </style>
    <style type="text/css">
        .watermark {
            background: url("../../../../ws/rs/rx/<%=prescription.getDemographicId()%>/watermark/<%=scriptId%>");
        }

    </style>
    <base href="<%= request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/" %>">


    <link rel="stylesheet" type="text/css" href="styles.css">
    <script type="text/javascript" language="Javascript">


        function onPrint2(method) {

            document.getElementById("preview2Form").action = "../form/createcustomedpdf?__title=Rx&__method=" + method;
            document.getElementById("preview2Form").target = "_blank";
            document.getElementById("preview2Form").submit();
            return true;
        }
    </script>

</head>
<body topmargin="0" leftmargin="0" vlink="#0000FF">

<%
    Date rxDate = prescription.getDatePrescribed();
//String rePrint = request.getParameter("rePrint");
    String rePrint = (String) request.getSession().getAttribute("rePrint");
//String rePrint = (String)request.getSession().getAttribute("rePrint");
    if (rePrint == null) {
        rePrint = "";
    }
    oscar.oscarRx.data.RxProviderData.Provider provider;
    String signingProvider = loggedInInfo.getLoggedInProviderNo();
    if (rePrint != null && rePrint.equalsIgnoreCase("true")) {
        //bean = (oscar.oscarRx.pageUtil.RxSessionBean)session.getAttribute("tmpBeanRX");
        //signingProvider = bean.getStashItem(0).getProviderNo();
        //rxDate = bean.getStashItem(0).getRxDate();
        //provider = new oscar.oscarRx.data.RxProviderData().getProvider(signingProvider);

//    String ip = request.getRemoteAddr();

    }
    provider = new oscar.oscarRx.data.RxProviderData().getProvider(signingProvider);


    oscar.oscarRx.data.RxPatientData.Patient patient = RxPatientData.getPatient(loggedInInfo, prescription.getDemographicId());
    String patientAddress = patient.getAddress() == null ? "" : patient.getAddress();
    String patientCity = patient.getCity() == null ? "" : patient.getCity();
    String patientProvince = patient.getProvince() == null ? "" : patient.getProvince();
    String patientPostal = patient.getPostal() == null ? "" : patient.getPostal();
    String patientPhone = patient.getPhone() == null ? "" : patient.getPhone();
    String patientHin = patient.getHin() == null ? "" : patient.getHin();


    oscar.oscarRx.data.RxPrescriptionData.Prescription rx = null;
    int i;
    ProSignatureData sig = new ProSignatureData();
    boolean hasSig = sig.hasSignature(signingProvider);
    String doctorName = "";
    if (hasSig) {
        doctorName = sig.getSignature(signingProvider);
    } else {
        doctorName = (provider.getFirstName() + ' ' + provider.getSurname());
    }

//doctorName = doctorName.replaceAll("\\d{6}","");
//doctorName = doctorName.replaceAll("\\-","");

    OscarProperties props = OscarProperties.getInstance();

    String pracNo = provider.getPractitionerNo();
    String strUser = (String) session.getAttribute("user");
    ProviderData user = new ProviderData(strUser);
    String pharmaFax = "";
    String pharmaFax2 = "";
    String pharmaName = "";
    RxPharmacyData pharmacyData = new RxPharmacyData();
    PharmacyInfo pharmacy;
    String pharmacyId = request.getParameter("pharmacyId");

    if (pharmacyId != null && !"null".equalsIgnoreCase(pharmacyId)) {
        pharmacy = pharmacyData.getPharmacy(pharmacyId);
        if (pharmacy != null) {
            pharmaFax = pharmacy.getFax();
            pharmaFax2 = "<fmt:setBundle basename='oscarResources'/><fmt:message key='RxPreview.msgFax'/>" + ": " + pharmacy.getFax();
            pharmaName = pharmacy.getName();
        }
    }

    String patientDOBStr = RxUtil.DateToString(patient.getDOB(), "MMM d, yyyy");
    boolean showPatientDOB = false;

//check if user prefer to show dob in print
    WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(request.getSession().getServletContext());
    UserPropertyDAO userPropertyDAO = (UserPropertyDAO) ctx.getBean(UserPropertyDAO.class);
    UserProperty prop = userPropertyDAO.getProp(signingProvider, UserProperty.RX_SHOW_PATIENT_DOB);
    if (prop != null && prop.getValue().equalsIgnoreCase("yes")) {
        showPatientDOB = true;
    }
%>
<form action="<%=request.getContextPath() %>/form/formname.do" id="preview2Form">
    <input type="hidden" name="demographic_no" value="<%=prescription.getDemographicId()%>"/>
    <p id="pharmInfo" style="float:right;">
    </p>
    <table class="watermark">

        <tr>
            <td>
                <table id="pwTable" width="400px" height="500px" cellspacing=0 cellpadding=10 border=2>
                    <tr>
                        <td valign=top height="100px"><input type="image"
                                                             src="<%= request.getContextPath() %>/images/rx.gif" border="0" alt="[Submit]"
                                                             name="submit" title="Print in a half letter size paper"
                                                             onclick="<%=rePrint.equalsIgnoreCase("true") ? "javascript:return onPrint2('rePrint');" : "javascript:return onPrint2('print');"  %>"/>
                            <!--input type="hidden" name="printPageSize" value="PageSize.A6" /--> <%
                                String clinicTitle = provider.getClinicName().replaceAll("\\(\\d{6}\\)", "") + "<br>";

                                clinicTitle += provider.getClinicAddress() + "<br>";
                                clinicTitle += provider.getClinicCity() + "   " + provider.getClinicPostal();

                                if (rx_enhance != null && rx_enhance.equals("true")) {

                                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
                                    String patientDOB = patient.getDOB() == null ? "" : formatter.format(patient.getDOB());

                                    String docInfo = doctorName + "\n" + provider.getClinicName().replaceAll("\\(\\d{6}\\)", "")
                                            + "<fmt:setBundle basename='oscarResources'/><fmt:message key='RxPreview.PractNo'/>" + pracNo
                                            + "\n" + provider.getClinicAddress() + "\n"
                                            + provider.getClinicCity() + "   "
                                            + provider.getClinicPostal() + "\n"
                                            + "<fmt:setBundle basename='oscarResources'/><fmt:message key='RxPreview.msgTel'/>" + ": "
                                            + provider.getClinicPhone() + "\n"
                                            + "<fmt:setBundle basename='oscarResources'/><fmt:message key='RxPreview.msgFax'/>" + ": "
                                            + provider.getClinicFax();

                                    String patientInfo = patient.getFirstName() + " "
                                            + patient.getSurname() + "\n"
                                            + patientAddress + "\n"
                                            + patientCity + "   "
                                            + patientPostal + "\n"
                                            + "<fmt:setBundle basename='oscarResources'/><fmt:message key='RxPreview.msgTel'/>" + ": " + patientPhone
                                            + (patientDOB != null && !patientDOB.trim().equals("") ? "\n"
                                            + "<fmt:setBundle basename='oscarResources'/><fmt:message key='RxPreview.msgDOB'/>" + ": " + patientDOB : "")
                                            + (!patientHin.trim().equals("") ? "\n" + "<fmt:setBundle basename='oscarResources'/><fmt:message key='oscar.oscarRx.hin'/>" + ": " + patientHin : "");
                                }

                            %> <input type="hidden" name="doctorName"
                                      value="<%= StringEscapeUtils.escapeHtml(doctorName) %>"/> <c:choose>
                                <c:when test="${empty infirmaryView_programAddress}">
                                    <%
                                        UserProperty phoneProp = userPropertyDAO.getProp(provider.getProviderNo(), "rxPhone");


                                        String finalPhone = provider.getClinicPhone();

                                        //if(providerPhone != null) {
                                        //	finalPhone = providerPhone;
                                        //}
                                        if (phoneProp != null && phoneProp.getValue().length() > 0) {
                                            finalPhone = phoneProp.getValue();
                                        }


                                        request.setAttribute("phone", finalPhone);

                                    %>
                                    <input type="hidden" name="clinicName"
                                           value="<%= StringEscapeUtils.escapeHtml(clinicTitle.replaceAll("(<br>)","\\\n")) %>"/>
                                    <input type="hidden" name="clinicPhone"
                                           value="<%= StringEscapeUtils.escapeHtml(finalPhone) %>"/>
                                    <input type="hidden" id="finalFax" name="clinicFax"
                                           value=""/>
                                </c:when>
                                <c:otherwise>
                                    <%
                                        UserProperty phoneProp = userPropertyDAO.getProp(provider.getProviderNo(), "rxPhone");
                                        UserProperty faxProp = userPropertyDAO.getProp(provider.getProviderNo(), "faxnumber");

                                        String finalPhone = (String) session.getAttribute("infirmaryView_programTel");
                                        String finalFax = (String) session.getAttribute("infirmaryView_programFax");

                                        //if(providerPhone != null) {
                                        //	finalPhone = providerPhone;
                                        //}
                                        if (phoneProp != null && phoneProp.getValue().length() > 0) {
                                            finalPhone = phoneProp.getValue();
                                        }

                                        if (faxProp != null && faxProp.getValue().length() > 0) {
                                            finalFax = faxProp.getValue();
                                        }

                                        request.setAttribute("phone", finalPhone);

                                    %>
                                    <input type="hidden" name="clinicName"
                                           value="<c:out value="${infirmaryView_programAddress}"/>"/>
                                    <input type="hidden" name="clinicPhone" value="<%=finalPhone%>"/>
                                    <input type="hidden" id="finalFax" name="clinicFax" value=""/>
                                </c:otherwise>
                            </c:choose>

                            <input type="hidden" name="patientName"
                                   value="<%= StringEscapeUtils.escapeHtml(patient.getFirstName())+ " " +StringEscapeUtils.escapeHtml(patient.getSurname()) %>"/>
                            <input type="hidden" name="patientDOB"
                                   value="<%= StringEscapeUtils.escapeHtml(patientDOBStr) %>"/>
                            <input type="hidden" name="pharmaFax" value="<%=pharmaFax%>"/>
                            <input type="hidden" name="pharmaName" value="<%=pharmaName%>"/>
                            <input type="hidden" name="pracNo" value="<%= StringEscapeUtils.escapeHtml(pracNo) %>"/>
                            <input type="hidden" name="showPatientDOB" value="<%=showPatientDOB%>"/>
                            <input type="hidden" name="pdfId" id="pdfId" value=""/>
                            <input type="hidden" name="patientAddress"
                                   value="<%= StringEscapeUtils.escapeHtml(patientAddress) %>"/>
                            <%
                                int check = (patientCity.trim().length() > 0 ? 1 : 0) | (patientProvince.trim().length() > 0 ? 2 : 0);
                                String patientCityPostal = String.format("%s%s%s %s",
                                        patientCity,
                                        check == 3 ? ", " : check == 2 ? "" : " ",
                                        patientProvince,
                                        patientPostal);

                                String ptChartNo = "";
                                if (props.getProperty("showRxChartNo", "").equalsIgnoreCase("true")) {
                                    ptChartNo = patient.getChartNo() == null ? "" : patient.getChartNo();
                                }
                            %>
                            <input type="hidden" name="patientCityPostal"
                                   value="<%= StringEscapeUtils.escapeHtml(patientCityPostal)%>"/>
                            <input type="hidden" name="patientHIN"
                                   value="<%= StringEscapeUtils.escapeHtml(patientHin) %>"/>
                            <input type="hidden" name="patientChartNo"
                                   value="<%=StringEscapeUtils.escapeHtml(ptChartNo)%>"/>
                            <input type="hidden" name="patientPhone"
                                   value="<fmt:setBundle basename="oscarResources"/><fmt:message key="RxPreview.msgTel"/><%=StringEscapeUtils.escapeHtml(patientPhone) %>"/>

                            <input type="hidden" name="rxDate"
                                   value="<%= StringEscapeUtils.escapeHtml(oscar.oscarRx.util.RxUtil.DateToString(rxDate, "MMMM d, yyyy")) %>"/>
                            <input type="hidden" name="sigDoctorName"
                                   value="<%= StringEscapeUtils.escapeHtml(doctorName) %>"/>
                            <!--img src="img/rx.gif" border="0"-->
                        </td>
                        <td valign=top height="100px" id="clinicAddress"><b><%=doctorName%>
                        </b><br>
                            <c:choose>
                                <c:when test="${empty infirmaryView_programAddress}">
                                    <%= provider.getClinicName().replaceAll("\\(\\d{6}\\)", "") %><br>
                                    <%= provider.getClinicAddress() %><br>
                                    <%= provider.getClinicCity() %>&nbsp;&nbsp;<%=provider.getClinicProvince()%>&nbsp;&nbsp;
                                    <%= provider.getClinicPostal() %>
                                    <% if (provider.getPractitionerNo() != null && !provider.getPractitionerNo().equals("")) { %>
                                    <br><fmt:setBundle basename="oscarResources"/><fmt:message key="RxPreview.PractNo"/>:<%= provider.getPractitionerNo() %><% } %>
                                    <br>
                                    <%
                                        UserProperty phoneProp = userPropertyDAO.getProp(provider.getProviderNo(), "rxPhone");
                                        UserProperty faxProp = userPropertyDAO.getProp(provider.getProviderNo(), "faxnumber");

                                        String finalPhone = provider.getClinicPhone();
                                        String finalFax = provider.getClinicFax();
                                        //if(providerPhone != null) {
                                        //	finalPhone = providerPhone;
                                        //}
                                        if (phoneProp != null && phoneProp.getValue().length() > 0) {
                                            finalPhone = phoneProp.getValue();
                                        }

                                        if (faxProp != null && faxProp.getValue().length() > 0) {
                                            finalFax = faxProp.getValue();
                                        }

                                        request.setAttribute("phone", finalPhone);

                                    %>
                                    <fmt:setBundle basename="oscarResources"/><fmt:message key="RxPreview.msgTel"/>: <%= finalPhone %><br>
                                    <oscar:oscarPropertiesCheck property="RXFAX" value="yes">
                                        <fmt:setBundle basename="oscarResources"/><fmt:message key="RxPreview.msgFax"/>: <%= finalFax %><br>
                                    </oscar:oscarPropertiesCheck>
                                </c:when>
                                <c:otherwise>
                                    <%
                                        UserProperty phoneProp = userPropertyDAO.getProp(provider.getProviderNo(), "rxPhone");
                                        UserProperty faxProp = userPropertyDAO.getProp(provider.getProviderNo(), "faxnumber");

                                        String finalPhone = (String) session.getAttribute("infirmaryView_programTel");
                                        String finalFax = (String) session.getAttribute("infirmaryView_programFax");

                                        //if(providerPhone != null) {
                                        //	finalPhone = providerPhone;
                                        //}
                                        if (phoneProp != null && phoneProp.getValue().length() > 0) {
                                            finalPhone = phoneProp.getValue();
                                        }

                                        if (faxProp != null && faxProp.getValue().length() > 0) {
                                            finalFax = faxProp.getValue();
                                        }

                                        request.setAttribute("phone", finalPhone);

                                    %>
                                    <c:out value="${infirmaryView_programAddress}" escapeXml="false"/>
                                    <br/>
                                    <fmt:setBundle basename="oscarResources"/><fmt:message key="RxPreview.msgTel"/>: <%=finalPhone %>
                                    <br/>
                                    <oscar:oscarPropertiesCheck property="RXFAX" value="yes">
                                        <fmt:setBundle basename="oscarResources"/><fmt:message key="RxPreview.msgFax"/>: <%=finalFax %>
                                    </oscar:oscarPropertiesCheck>
                                </c:otherwise>
                            </c:choose></td>
                    </tr>
                    <tr>
                        <td colspan=2 valign=top height="75px">
                            <table width=100% cellspacing=0 cellpadding=0>
                                <tr>
                                    <td align=left valign=top><br>
                                        <%= Encode.forHtmlContent(patient.getFirstName()) %> <%= Encode.forHtmlContent(patient.getSurname()) %> <%if (showPatientDOB) {%>
                                        <br>DOB:<%= Encode.forHtmlContent(StringEscapeUtils.escapeHtml(patientDOBStr)) %> <%}%>
                                        <br>
                                        <%= patientAddress %><br>
                                        <%= patientCityPostal %><br>
                                        <%= patientPhone %><br>
                                        <b><% if (!props.getProperty("showRxHin", "").equals("false")) { %>
                                            <fmt:setBundle basename="oscarResources"/><fmt:message key="oscar.oscarRx.hin"/><%= patientHin %> <% } %>
                                        </b><br>
                                        <% if (props.getProperty("showRxChartNo", "").equalsIgnoreCase("true")) { %>
                                        <fmt:setBundle basename="oscarResources"/><fmt:message key="oscar.oscarRx.chartNo"/><%=ptChartNo%><% } %></td>
                                    <td align=right valign=top>
                                        <b><%= oscar.oscarRx.util.RxUtil.DateToString(rxDate, "MMMM d, yyyy", request.getLocale()) %>
                                        </b></td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    <tr>
                        <td colspan=2 valign=top height="275px">
                            <table height=100% width=100%>
                                <tr valign=top>
                                    <td colspan=2 height=225px>
                                        <%
                                            String strRx = "";
                                            StringBuilder strRxNoNewLines = new StringBuilder();

                                            List<Drug> drugList = prescriptionManager.getDrugsByScriptNo(loggedInInfo, prescription.getId(), null);
                                            Logger logger = LogManager.getLogger("preview_jsp");
                                            logger.error("DRUG LIST SIZE$$$ " + drugList.size() + "for script ID " + prescription.getId());
                                            for (Drug drug : drugList) {

                                                String fullOutLine = drug.getFullOutLine().replaceAll(";", "<br />");

                                                if (fullOutLine == null || fullOutLine.length() <= 6) {
                                                    logger.error("drug full outline was null");
                                                    fullOutLine = "<span style=\"color:red;font-size:16;font-weight:bold\">An error occurred, please write a new prescription.</span><br />" + fullOutLine;
                                                }
                                        %>
                                        <%=fullOutLine%>
                                        <hr>
                                        <%
                                                strRx += drug.getFullOutLine() + ";;";
                                                strRxNoNewLines.append(drug.getFullOutLine().replaceAll(";", " ") + "\n");
                                            }
                                        %> <input type="hidden" name="rx"
                                                  value="<%= StringEscapeUtils.escapeHtml(strRx.replaceAll(";","\\\n")) %>"/>
                                        <input type="hidden" name="rx_no_newlines"
                                               value="<%= strRxNoNewLines.toString() %>"/>
                                        <input type="hidden" name="additNotes" value=""/>
                                    </td>

                                </tr>

                                <tr valign="bottom">
                                    <td colspan="2" id="additNotes">

                                    </td>

                                </tr>


                                <% if (oscar.OscarProperties.getInstance().getProperty("RX_FOOTER") != null) {
                                    out.write(oscar.OscarProperties.getInstance().getProperty("RX_FOOTER"));
                                }%>


                                <tr valign=bottom>
                                    <td height=25px width=25%><fmt:setBundle basename="oscarResources"/><fmt:message key="RxPreview.msgSignature"/>:</td>
                                    <td height=25px width=75%
                                        style="border-width: 0; border-bottom-width: 1; border-style: solid;">
                                        <%
                                            String signatureRequestId = null;
                                            String imageUrl = null;
                                            String startimageUrl = null;
                                            String statusUrl = null;

                                            signatureRequestId = loggedInInfo.getLoggedInProviderNo();
                                            imageUrl = request.getContextPath() + "/imageRenderingServlet?source=" + ImageRenderingServlet.Source.signature_preview.name() + "&" + DigitalSignatureUtils.SIGNATURE_REQUEST_ID_KEY + "=" + signatureRequestId;
                                            startimageUrl = request.getContextPath() + "/images/1x1.gif";
                                            statusUrl = request.getContextPath() + "/PMmodule/ClientManager/check_signature_status.jsp?" + DigitalSignatureUtils.SIGNATURE_REQUEST_ID_KEY + "=" + signatureRequestId;
                                        %>
                                        <input type="hidden" name="<%=DigitalSignatureUtils.SIGNATURE_REQUEST_ID_KEY%>"
                                               value="<%=signatureRequestId%>"/>

                                        <img id="signature" style="width:300px; height:60px" src="<%=startimageUrl%>"
                                             alt="digital_signature"/>
                                        <input type="hidden" name="imgFile" id="imgFile" value=""/>
                                        <script type="text/javascript">

                                            var POLL_TIME = 2500;
                                            var counter = 0;

                                            function refreshImage() {
                                                counter = counter + 1;
                                                var img = document.getElementById("signature");
                                                img.src = '<%=imageUrl%>&rand=' + counter;

                                                var request = dojo.io.bind({
                                                    url: '<%=statusUrl%>',
                                                    method: "post",
                                                    mimetype: "text/html",
                                                    load: function (type, data, evt) {
                                                        var x = data.trim();
                                                        //document.getElementById('signature_status').value=x;
                                                    }
                                                });

                                            }
                                        </script>

                                        &nbsp;
                                    </td>
                                </tr>
                                <tr valign=bottom>
                                    <td height=25px>
                                        <% if (props.getProperty("signature_tablet", "").equals("yes")) { %>
                                        <input type="button" value=
                                        <fmt:setBundle basename="oscarResources"/><fmt:message key="RxPreview.digitallySign"/> class="noprint"
                                               onclick="setInterval('refreshImage()', POLL_TIME); document.location='<%=request.getContextPath()%>/signature_pad/topaz_signature_pad.jnlp.jsp?<%=DigitalSignatureUtils.SIGNATURE_REQUEST_ID_KEY%>=<%=signatureRequestId%>'"/>
                                        <% } %>
                                    </td>
                                    <td height=25px>
                                        &nbsp; <%= doctorName%> <% if (pracNo != null && !pracNo.equals("") && !pracNo.equalsIgnoreCase("null")) { %>
                                        <br/> &nbsp; <fmt:setBundle basename="oscarResources"/><fmt:message key="RxPreview.PractNo"/> <%= pracNo%> <% } %>
                                    </td>
                                </tr>
                                <%
                                    if (rePrint.equalsIgnoreCase("true") && rx != null) {
                                %>
                                <tr valign=bottom style="font-size: 6px;">
                                    <td height=25px colspan="2"><fmt:setBundle basename="oscarResources"/><fmt:message key="RxPreview.msgReprintBy"/> <%=ProviderData.getProviderName(strUser)%><span
                                            style="float: left;">
		                                                            <fmt:setBundle basename="oscarResources"/><fmt:message key="RxPreview.msgOrigPrinted"/>:&nbsp;<%=rx.getPrintDate()%></span>
                                        <span
                                                style="float: right;"><fmt:setBundle basename="oscarResources"/><fmt:message key="RxPreview.msgTimesPrinted"/>:&nbsp;<%=String.valueOf(rx.getNumPrints())%></span>
                                        <input type="hidden" name="origPrintDate" value="<%=rx.getPrintDate()%>"/>
                                        <input type="hidden" name="numPrints"
                                               value="<%=String.valueOf(rx.getNumPrints())%>"/>
                                    </td>
                                </tr>
                                <%
                                    }

                                    if (PrescriptionQrCodeUIBean.isPrescriptionQrCodeEnabledForProvider(providerNo)) {
                                %>
                                <tr>
                                    <td colspan="2">
                                        <img src="<%=request.getContextPath()%>/contentRenderingServlet/prescription_qr_code_<%=rx.getScript_no()%>.png?source=prescriptionQrCode&prescriptionId=<%=rx.getScript_no()%>"
                                             alt="qr_code"/>
                                    </td>
                                </tr>
                                <%
                                    }


                                    if (oscar.OscarProperties.getInstance().getProperty("FORMS_PROMOTEXT") != null) {
                                %>
                                <tr valign=bottom align="center" style="font-size: 9px">
                                    <td height=25px colspan="2"></br>
                                        <%= oscar.OscarProperties.getInstance().getProperty("FORMS_PROMOTEXT") %>
                                    </td>
                                </tr>
                                <%
                                    }
                                %>
                            </table>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</form>
</body>
</html>
