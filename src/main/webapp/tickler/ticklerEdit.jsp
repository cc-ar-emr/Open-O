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
<%@page import="java.util.Set" %>
<%@page import="java.util.List" %>
<%@page import="java.util.GregorianCalendar" %>
<%@page import="java.util.Calendar" %>
<%@page import="java.util.Locale" %>
<%@page import="java.text.DateFormat" %>
<%@page import="java.text.SimpleDateFormat" %>
<%@page import="org.owasp.encoder.Encode" %>
<%@page import="ca.openosp.openo.commn.dao.TicklerTextSuggestDao" %>
<%@page import="ca.openosp.openo.utility.LocaleUtils" %>
<%@page import="ca.openosp.openo.commn.dao.TicklerTextSuggestDao" %>
<%@page import="ca.openosp.openo.PMmodule.dao.ProviderDao" %>
<%@page import="ca.openosp.openo.commn.model.Provider" %>
<%@page import="ca.openosp.openo.commn.model.Demographic" %>
<%@page import="ca.openosp.openo.commn.model.TicklerTextSuggest" %>
<%@page import="ca.openosp.openo.commn.model.Tickler" %>
<%@page import="ca.openosp.openo.commn.model.TicklerComment" %>
<%@page import="ca.openosp.openo.utility.SpringUtils" %>
<%@page import="ca.openosp.openo.utility.LoggedInInfo" %>
<%@page import="ca.openosp.openo.managers.TicklerManager" %>
<%@page import="ca.openosp.openo.managers.DemographicManager" %>
<%@page import="ca.openosp.openo.documentManager.DocumentAttachmentManager" %>
<%@page import="ca.openosp.openo.documentManager.data.TicklerAttachmentData" %>
<%@page import="ca.openosp.openo.commn.model.enumerator.DocumentType" %>
<%@page import="ca.openosp.OscarProperties" %>
<%
    TicklerManager ticklerManager = SpringUtils.getBean(TicklerManager.class);
    DemographicManager demographicManager = SpringUtils.getBean(DemographicManager.class);
    DocumentAttachmentManager documentAttachmentManager = SpringUtils.getBean(DocumentAttachmentManager.class);
    LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/oscar-tag.tld" prefix="oscar" %>
<%@ taglib prefix="e" uri="https://www.owasp.org/index.php/OWASP_Java_Encoder_Project" %>
<%@ taglib uri="/WEB-INF/security.tld" prefix="security" %>
<%
    String roleName$ = session.getAttribute("userrole") + "," + session.getAttribute("user");
    boolean authed = true;
%>

<fmt:setBundle basename="oscarResources"/>

<security:oscarSec roleName="<%=roleName$%>" objectName="_tickler" rights="w" reverse="<%=true%>">
    <%authed = false; %>
    <%response.sendRedirect(request.getContextPath() + "/securityError.jsp?type=_tickler");%>
</security:oscarSec>
<%
    if (!authed) {
        return;
    }
%>
<%
    boolean caisiEnabled = OscarProperties.getInstance().isPropertyActive("caisi");
    String ticklerNoStr = request.getParameter("tickler_no");

    Integer ticklerNo = null;
    try {
        ticklerNo = Integer.valueOf(ticklerNoStr);
    } catch (NumberFormatException ignored) {
    }

    Tickler t = null;
    Demographic d = null;
    if (ticklerNo != null) {
        t = ticklerManager.getTickler(loggedInInfo, ticklerNo);
        d = demographicManager.getDemographicWithExt(loggedInInfo, t.getDemographicNo());
    } else {
        response.sendRedirect(request.getContextPath() + "/errorpage.jsp");
    }

    java.util.Locale vLocale = request.getLocale();

    String selected = "";
    String stActive = LocaleUtils.getMessage(request.getLocale(), "tickler.ticklerMain.stActive");
    String stComplete = LocaleUtils.getMessage(request.getLocale(), "tickler.ticklerMain.stComplete");
    String stDeleted = LocaleUtils.getMessage(request.getLocale(), "tickler.ticklerMain.stDeleted");

    String prHigh = LocaleUtils.getMessage(request.getLocale(), "tickler.ticklerMain.priority.high");
    String prNormal = LocaleUtils.getMessage(request.getLocale(), "tickler.ticklerMain.priority.normal");
    String prLow = LocaleUtils.getMessage(request.getLocale(), "tickler.ticklerMain.priority.low");

    GregorianCalendar now = new GregorianCalendar();
    int curYear = now.get(Calendar.YEAR);
    int curMonth = (now.get(Calendar.MONTH) + 1);
    int curDay = now.get(Calendar.DAY_OF_MONTH);

    Locale locale = request.getLocale();
    DateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale);
    DateFormat dateOnlyFormat = new SimpleDateFormat("yyyy-MM-dd", locale);
    DateFormat timeOnlyFormat = new SimpleDateFormat("HH:mm:ss", locale);
%>
<!DOCTYPE html>
<html>
    <head>
        <style>
            .tickler-comment-row:nth-child(odd) td {
                background-color: whitesmoke;
            }

            .tickler-comment-row:nth-child(even) td {
                background-color: white;
            }

            /* Don't compress the attachment picker dialog — it styles itself. */
            *:not(h2):not(.btn):not(.ui-dialog):not(.ui-dialog *) {
              line-height: 1 !important;
              font-size: 12px !important;
            }

            /* Shrink the quick-pick date grid so all 10 columns fit the edit window's width. */
            #quickPickDateOptions {
                grid-gap: 1px;
                width: 400px;
            }
            #quickPickDateOptions a {
                padding: 2px 4px;
                min-width: 0;
            }

            /* Keep the "Manage Attachments" button and its count badge on one line. */
            .attachments-cell {
                white-space: nowrap;
            }

            /* Visible list of attachment names below the "Manage Attachments" button. */
            #attachmentNames {
                white-space: normal;
                margin-top: 6px;
            }

            #attachmentNames .attachment-group-heading {
                font-weight: bold;
                margin-top: 4px;
            }

            #attachmentNames ul {
                list-style: none;
                margin: 2px 0 4px;
                padding-left: 1.5em;
            }

            #attachmentNames li {
                padding: 2px 0;
            }

            /* jQuery UI renders the dialog close control as a fixed ~20px square, which
               crushes the "Save and Close" label onto multiple overflowing lines. Let it
               grow to fit the text (matches the Consult/eForm attachment dialog). */
            .save-and-close-button {
                width: auto !important;
                white-space: nowrap;
                padding: 0 8px !important;
            }

            /*.grid {*/
            /*    display: grid;*/
            /*    grid-template-columns: repeat(10, 1fr);*/
            /*    grid-gap: 2px;*/
            /*    width: 270px;*/
            /*}*/

            /*.grid a, .today-button {*/
            /*    background-color: #E6E6FA;*/
            /*    text-align: center;*/
            /*    width: auto;*/
            /*    height: auto;*/
            /*    padding: 2px;*/
            /*    margin: 1px;*/
            /*    display: flex;*/
            /*    justify-content: center;*/
            /*    text-decoration: none;*/
            /*    color: black;*/
            /*    font-size: 11px !important;*/
            /*    border-radius: 3px;*/
            /*}*/

            /*.grid a:hover, .today-button:hover {*/
            /*    background-color: #EE82EE;*/
            /*    color: white;*/
            /*}*/

            /*.today-button {*/
            /*    width: 125px;*/
            /*    cursor: pointer;*/
            /*}*/
        </style>
        <title><fmt:message key="tickler.ticklerEdit.title"/></title>

        <link href="<%=request.getContextPath()%>/share/css/dateTimeQuickPick.css" rel="stylesheet" type="text/css" />
        <script type="text/javascript" src="<%=request.getContextPath()%>/share/javascript/dateTimeQuickPick.js"></script>

        <script type="application/javascript">
            //open a new popup window
            function popupPage(vheight, vwidth, varpage) {
                var page = "" + varpage;
                windowprops = "height=" + vheight + ",width=" + vwidth + ",location=no,scrollbars=yes,menubars=no,toolbars=no,resizable=yes";
                var popup = window.open(page, "attachment", windowprops);
                if (popup != null) {
                    if (popup.opener == null) {
                        popup.opener = self;
                    }
                }
            }

            function pasteMessageText() {
                var selectedIdx = document.serviceform.suggestedText.selectedIndex;
                document.serviceform.newMessage.value = document.serviceform.suggestedText.options[selectedIdx].text;
            }

            function openBrWindow(theURL, winName, features) {
                window.open(theURL, winName, features);
            }

            // // Add options 1 to 10 for days, weeks, months, and years
            // function addQuickPick() {
            //     const quickPickDiv = document.getElementById('quickPickDateOptions');
            //     for (let i = 0; i < 40; i++) {
            //         const linkButton = document.createElement('a');
            //         linkButton.href = '#';
            //         switch (Math.floor(i / 10)) {
            //             case 0:
            //                 linkButton.innerText = (i % 10) + 1 + "d";
            //                 linkButton.onclick = function () {
            //                     addTime((i % 10) + 1, "days");
            //                 };
            //                 break;//1 through 10 days
            //             case 1:
            //                 linkButton.innerText = (i % 10) + 1 + "w";
            //                 linkButton.onclick = function () {
            //                     addTime(((i % 10) + 1) * 7, "days");
            //                 };
            //                 break;//1 through 10 weeks
            //             case 2:
            //                 linkButton.innerText = (i % 10) + 1 + "m";
            //                 linkButton.onclick = function () {
            //                     addTime((i % 10) + 1, "months");
            //                 };
            //                 break;//1 through 10 months
            //             case 3:
            //                 linkButton.innerText = (i % 10) + 1 + "y";
            //                 linkButton.onclick = function () {
            //                     addTime(((i % 10) + 1) * 12, "months");
            //                 };
            //                 break;//1 through 10 years
            //         }
            //         quickPickDiv.appendChild(linkButton);
            //     }
            // }
            //
            // function addTime(num, type) {
            //     let currentDate = new Date();
            //     if (type === "months") {
            //         currentDate.setMonth(currentDate.getMonth() + num);
            //     } else {
            //         currentDate.setDate(currentDate.getDate() + num);
            //     }
            //     const year = currentDate.getFullYear();
            //     const month = String(currentDate.getMonth() + 1).padStart(2, '0');
            //     const day = String(currentDate.getDate()).padStart(2, '0');
            //     document.serviceform.xml_appointment_date.value = year + "-" + month + "-" + day;
            // }

            function DateAdd(startDate, numDays, numMonths, numYears) {
                var returnDate = new Date(startDate.getTime());
                var yearsToAdd = numYears;

                var month = returnDate.getMonth() + numMonths;
                if (month > 11) {
                    yearsToAdd = Math.floor((month + 1) / 12);
                    month -= 12 * yearsToAdd;
                    yearsToAdd += numYears;
                }
                returnDate.setMonth(month);
                returnDate.setFullYear(returnDate.getFullYear() + yearsToAdd);

                returnDate.setTime(returnDate.getTime() + 60000 * 60 * 24 * numDays);

                return returnDate;
            }

            Date.prototype.addMonths = function (months) {
                var dat = new Date(this.valueOf());
                dat.setMonth(dat.getMonth() + months);
                return dat;
            }

            function validate(form, writeToEncounter) {
                if (validateDate(form) <%=caisiEnabled?"&& validateSelectedProgram()":""%>) {
                    if (writeToEncounter) {
                        window.open('<%=request.getContextPath()%>/oscarEncounter/IncomingEncounter.do?demographicNo=<%=Encode.forUriComponent(String.valueOf(d.getDemographicNo()))%>&providerNo=<%=Encode.forUriComponent(String.valueOf(loggedInInfo.getLoggedInProviderNo()))%>&curDate=<%=Encode.forUriComponent(String.valueOf(curYear))%>-<%=Encode.forUriComponent(String.valueOf(curMonth))%>-<%=Encode.forUriComponent(String.valueOf(curDay))%>&encType=&status=', '', 'height=700,width=960');
                    }
                    form.submit();
                    return true;
                }
                return false;
            }

            function IsDate(value) {
                let dateWrapper = new Date(value);
                return !isNaN(dateWrapper.getDate());
            }

            function validateDate(form) {
                let errorDiv = document.getElementById("error");
                if (form.xml_appointment_date.value === "" || !IsDate(form.xml_appointment_date.value)) {
                    errorDiv.textContent = "<fmt:message key="tickler.ticklerAdd.msgMissingDate"/>";
                    errorDiv.style.display = 'block';
                    return false;
                } else {
                    return true;
                }
            }

            // on load
            document.addEventListener('DOMContentLoaded', function() {
              addQuickPick();
            });
        </script>
        <link href="<%= request.getContextPath() %>/library/bootstrap/3.0.0/css/bootstrap.css" rel="stylesheet"
              type="text/css">

        <script type="text/javascript" src="<%=request.getContextPath()%>/library/jquery/jquery-3.6.4.min.js"></script>
        <script type="text/javascript" src="<%=request.getContextPath()%>/library/jquery/jquery-ui-1.12.1.min.js"></script>
        <script type="text/javascript" src="<%=request.getContextPath()%>/js/jquery_oscar_defaults.js"></script>
        <link href="<%=request.getContextPath() %>/library/jquery/jquery-ui-1.12.1.min.css" rel="stylesheet"
              type="text/css">

    </head>

    <body>
    <div class="container">
        <form id="ticklerEditForm" name="serviceform" action="${pageContext.request.contextPath}/tickler/EditTickler.do" method="post">
            <input type="hidden" name="method" value="editTickler"/>
            <input type="hidden" name="ticklerNo" value="<%=Encode.forHtmlAttribute(String.valueOf(ticklerNo))%>"/>
            <input type="hidden" name="parentAjaxId" value="<e:forHtml value='${param.parentAjaxId}' />"/>
            <h2>
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-feather"
                   viewBox="0 0 16 16">
                <path d="M15.807.531c-.174-.177-.41-.289-.64-.363a3.765 3.765 0 0 0-.833-.15c-.62-.049-1.394 0-2.252.175C10.365.545 8.264 1.415 6.315 3.1c-1.95 1.686-3.168 3.724-3.758 5.423-.294.847-.44 1.634-.429 2.268.005.316.05.62.154.88.017.04.035.082.056.122A68.362 68.362 0 0 0 .08 15.198a.528.528 0 0 0 .157.72.504.504 0 0 0 .705-.16 67.606 67.606 0 0 1 2.158-3.26c.285.141.616.195.958.182.513-.02 1.098-.188 1.723-.49 1.25-.605 2.744-1.787 4.303-3.642l1.518-1.55a.528.528 0 0 0 0-.739l-.729-.744 1.311.209a.504.504 0 0 0 .443-.15c.222-.23.444-.46.663-.684.663-.68 1.292-1.325 1.763-1.892.314-.378.585-.752.754-1.107.163-.345.278-.773.112-1.188a.524.524 0 0 0-.112-.172ZM3.733 11.62C5.385 9.374 7.24 7.215 9.309 5.394l1.21 1.234-1.171 1.196a.526.526 0 0 0-.027.03c-1.5 1.789-2.891 2.867-3.977 3.393-.544.263-.99.378-1.324.39a1.282 1.282 0 0 1-.287-.018Zm6.769-7.22c1.31-1.028 2.7-1.914 4.172-2.6a6.85 6.85 0 0 1-.4.523c-.442.533-1.028 1.134-1.681 1.804l-.51.524-1.581-.25Zm3.346-3.357C9.594 3.147 6.045 6.8 3.149 10.678c.007-.464.121-1.086.37-1.806.533-1.535 1.65-3.415 3.455-4.976 1.807-1.561 3.746-2.36 5.31-2.68a7.97 7.97 0 0 1 1.564-.173Z"/>
              </svg>
              <fmt:message key="tickler.ticklerEdit.title"/>
            </h2>
            <div id="error" class="alert alert-danger" style="display:none;"></div>

            <table class="table table-condensed">

                <tr>
                    <th style="background-color: #EEEEFF"><fmt:message key="tickler.ticklerEdit.demographicName"/></th>
                    <td><a href="javascript:void(0)"
                           onClick="popupPage(600,800,'<%=request.getContextPath() %>/demographic/demographiccontrol.jsp?demographic_no=<%=Encode.forUriComponent(String.valueOf(d.getDemographicNo()))%>&displaymode=edit&dboperation=search_detail')">
                        <%=Encode.forHtmlContent(d.getLastName())%>,<%=Encode.forHtmlContent(d.getFirstName())%>
                    </a></td>
                    <th style="background-color: #EEEEFF"><fmt:message key="tickler.ticklerEdit.phoneNumbers"/></th>
                    <td>(H) <%=Encode.forHtmlContent(d.getPhone())%><br>(W) <%=Encode.forHtmlContent(d.getPhone2())%>
                        <br>(C) <%=Encode.forHtmlContent(d.getCellPhone())%>
                    </td>
                </tr>
                <tr>
                    <th style="background-color: #EEEEFF"><fmt:message key="tickler.ticklerEdit.chartNo"/></th>
                    <td><%=Encode.forHtml(String.valueOf(d.getChartNo()))%>
                    </td>
                    <th style="background-color: #EEEEFF"><fmt:message key="tickler.ticklerEdit.phoneComments"/></th>
                    <td><%=Encode.forHtmlContent(d.getPhoneComment())%>
                    </td>
                </tr>
                <tr>
                    <th style="background-color: #EEEEFF"><fmt:message key="tickler.ticklerEdit.age"/></th>
                    <td><%=Encode.forHtml(String.valueOf(d.getAge()))%>(<%=Encode.forHtml(String.valueOf(d.getFormattedDob()))%>)</td>
                    <th style="background-color: #EEEEFF"><fmt:message key="tickler.ticklerEdit.email"/></th>
                    <td><%=Encode.forHtmlContent(d.getEmail())%>
                    </td>
                </tr>

                <tr>
                    <td colspan="4" style="padding-bottom:1em;"></td>
                </tr>
                <tr>
                    <th colspan="2" style="background-color: #336666;color:white;"><fmt:message key="tickler.ticklerEdit.messages"/></th>
                    <th style="background-color: #336666;color:white;"><fmt:message key="tickler.ticklerEdit.addedBy"/>
                    <th style="background-color: #336666;color:white;"><fmt:message key="tickler.ticklerEdit.dateAdded"/>
                </tr>

                <tr>
                    <td colspan="2"
                        style="font-weight: bold; white-space:pre-wrap;"><%=Encode.forHtmlContent(t.getMessage())%>
                    </td>
                    <td style="font-weight: bold"><%=Encode.forHtmlContent(t.getProvider().getLastName())%>
                        ,<%=Encode.forHtmlContent(t.getProvider().getFirstName())%>
                    </td>
                    <td style="font-weight: bold"><%=Encode.forHtml(String.valueOf(datetimeFormat.format(t.getCreateDate())))%>
                    </td>
                </tr>

                <%
                    Set<TicklerComment> tComments = t.getComments();
                    for (TicklerComment tc : tComments) {
                %>
                <tr class="tickler-comment-row">
                    <td colspan="2" style="white-space:pre-wrap;"><%=Encode.forHtmlContent(tc.getMessage())%>
                    </td>
                    <td><%=Encode.forHtmlContent(tc.getProvider().getLastName())%>
                        ,<%=Encode.forHtmlContent(tc.getProvider().getFirstName())%>
                    </td>
                    <td><%=Encode.forHtml(String.valueOf(datetimeFormat.format(tc.getUpdateDate())))%>
                    </td>
                </tr>
                <%}%>

                <tr>
                    <td colspan="4" style="padding-top:1em;"></td>
                </tr>
                <tr>
                    <th colspan="2" style="background-color: #666699;color:white;"><fmt:message key="tickler.ticklerEdit.newMessage"/></th>
                    <th colspan="2" style="background-color: #666699;color:white;"><label for="status"><fmt:message key="tickler.ticklerEdit.status"/></label></th>
                </tr>
                <tr>
                    <td><label for="suggestedText"><a href="javascript:void(0)"
                                                      onclick="openBrWindow('./ticklerSuggestedText.jsp','tickler_suggested_text','width=680,height=400')"
                                                      style="font-weight:bold">
                        <fmt:message key="tickler.ticklerEdit.suggestedText"/></a>:</label></td>
                    <td>
                        <select class="form-control" name="suggestedText" id="suggestedText">
                            <option value="">---</option>
                            <%
                                TicklerTextSuggestDao ticklerTextSuggestDao = (TicklerTextSuggestDao) SpringUtils.getBean(TicklerTextSuggestDao.class);
                                for (TicklerTextSuggest tTextSuggest : ticklerTextSuggestDao.getActiveTicklerTextSuggests()) { %>
                            <option><%=Encode.forHtmlContent(tTextSuggest.getSuggestedText())%>
                            </option>
                            <% } %>
                        </select>
                    </td>

                    <td colspan="2">
                        <select class="form-control" name="status" id="status">
                                    <% if (t.getStatusDesc(vLocale).equals(stActive)){selected="selected";}else{selected="";}%>
                            <option <%=selected%> value="A"><fmt:message key="tickler.ticklerMain.stActive"/></option>
                                    <% if (t.getStatusDesc(vLocale).equals(stComplete)){selected="selected";}else{selected="";}%>
                            <option <%=selected%> value="C"><fmt:message key="tickler.ticklerMain.stComplete"/></option>
                                    <% if (t.getStatusDesc(vLocale).equals(stDeleted)){selected="selected";}else{selected="";}%>
                            <option <%=selected%> value="D"><fmt:message key="tickler.ticklerMain.stDeleted"/></option>
                        </select>
                    </td>
                </tr>
                <tr>
                    <td colspan="2" style="border: none;"><label for="newMessage"><fmt:message key="tickler.ticklerEdit.messageText"/>:</label></td>
                    <th colspan="2" style="background-color: #666699;color:white;"><label for="priority"><fmt:message key="tickler.ticklerEdit.priority"/></label></th>
                </tr>

                <tr>
                    <td colspan="2" rowspan="8" style="border: none;">
                        <textarea class="form-control" rows="23" style="width:100%;" id="newMessage"
                                  name="newMessage"></textarea>
                        <input type="button" class="btn" name="pasteMessage" onclick="pasteMessageText()"
                               value="<fmt:message key="tickler.ticklerEdit.pasteMessage"/>"/>
                    </td>

                    <td colspan="2">
                        <select class="form-control" name="priority" id="priority">
                            <% if (t.getPriorityWeb().equals(prHigh)) {
                                selected = "selected";
                            } else {
                                selected = "";
                            }%>
                            <option <%=selected%> value="<fmt:message key="tickler.ticklerMain.priority.high"/>">
                                <fmt:message key="tickler.ticklerMain.priority.high"/></option>
                            <% if (t.getPriorityWeb().equals(prNormal)) {
                                selected = "selected";
                            } else {
                                selected = "";
                            }%>
                            <option <%=selected%> value="<fmt:message key="tickler.ticklerMain.priority.normal"/>">
                                <fmt:message key="tickler.ticklerMain.priority.normal"/></option>
                            <% if (t.getPriorityWeb().equals(prLow)) {
                                selected = "selected";
                            } else {
                                selected = "";
                            }%>
                            <option <%=selected%> value="<fmt:message key="tickler.ticklerMain.priority.low"/>">
                                <fmt:message key="tickler.ticklerMain.priority.low"/></option>
                        </select>
                    </td>
                </tr>

                <tr>
                    <th colspan="2" style="background-color: #666699;color:white;">
                        <label for="assignedToProviders"><fmt:message key="tickler.ticklerEdit.assignedTo"/></label>
                    </th>
                </tr>
                <tr>
                    <td colspan="2">
                        <select class="form-control" name="assignedToProviders" id="assignedToProviders">
                            <%
                                ProviderDao providerDao = (ProviderDao) SpringUtils.getBean(ProviderDao.class);
                                List<Provider> providers = providerDao.getActiveProviders();
                                for (Provider p : providers) {

                                    if (p.equals(t.getAssignee())) {
                                        selected = "selected";
                                    } else {
                                        selected = "";
                                    }
                            %>
                            <option <%=selected%>
                                    value="<%=Encode.forHtmlAttribute(String.valueOf(p.getProviderNo()))%>"><%=Encode.forHtmlContent(p.getLastName())%>
                                ,<%=Encode.forHtmlContent(p.getFirstName())%>
                            </option>
                            <% } %>
                        </select>
                    </td>
                </tr>

                <tr>
                    <th colspan="2" style="background-color: #666699;color:white;">
                        <fmt:message key="tickler.ticklerEdit.serviceDate"/>
                    </th>
                </tr>
                <tr>
                    <%


                        DateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd");
                        String strDate = dateformat.format(t.getServiceDate());


                    %>
                    <td colspan="2" style="border: none;">
                        <label for="xml_appointment_date"><fmt:message key="tickler.ticklerEdit.calendarLookup"/></label>
                        <input name="xml_appointment_date" class="form-control" id="xml_appointment_date" type="date"
                               maxlength="10" value="<%=Encode.forHtmlAttribute(String.valueOf(strDate))%>"/>
                    </td>

                </tr>
                <tr>
                    <td colspan="2" style="border: none;">
                        <!-- Today button placed before the grid -->
                        <div id="todayButton" class="today-button" onclick="addTime(0, 'days')">Today</div>
                        <div id="quickPickDateOptions" class="grid">
                            <!-- Quick pick will be added here using JavaScript -->
                        </div>
                    </td>
                </tr>
                <%
                    // Ids build hidden "delegate" inputs mirroring the picker's checkbox names so the
                    // dialog can pre-check them; names build the visible attachment list.
                    java.util.LinkedHashMap<DocumentType, String[]> ticklerAttachTypes = new java.util.LinkedHashMap<DocumentType, String[]>();
                    ticklerAttachTypes.put(DocumentType.EFORM, new String[]{"eFormNo", "eForms"});
                    ticklerAttachTypes.put(DocumentType.DOC, new String[]{"docNo", "Documents"});
                    ticklerAttachTypes.put(DocumentType.LAB, new String[]{"labNo", "Labs"});
                    ticklerAttachTypes.put(DocumentType.HRM, new String[]{"hrmNo", "HRM"});
                    ticklerAttachTypes.put(DocumentType.FORM, new String[]{"formNo", "Forms"});

                    List<TicklerAttachmentData> ticklerAttachments = documentAttachmentManager.getTicklerAttachmentDetails(loggedInInfo, ticklerNo, t.getDemographicNo());

                    StringBuilder delegateInputs = new StringBuilder();
                    for (TicklerAttachmentData ticklerAttachment : ticklerAttachments) {
                        String paramName = ticklerAttachTypes.get(ticklerAttachment.getDocumentType())[0];
                        delegateInputs.append("<input type=\"hidden\" class=\"delegateAttachment\" name=\"")
                                .append(paramName)
                                .append("\" value=\"").append(Encode.forHtmlAttribute(ticklerAttachment.getDocumentId()))
                                .append("\" id=\"delegate_").append(Encode.forHtmlAttribute(paramName + ticklerAttachment.getDocumentId()))
                                .append("\">");
                    }
                    int attachmentCount = ticklerAttachments.size();
                %>
                <tr>
                    <th colspan="2" style="background-color: #666699;color:white;">Attachments</th>
                </tr>
                <tr>
                    <td colspan="2">
                        <span class="attachments-cell">
                            <button type="button" class="btn" id="manageAttachmentsBtn"
                                    title="Manage Attachments"
                                    data-poload="${pageContext.request.contextPath}/previewDocs.do?method=fetchTicklerDocuments&amp;demographicNo=<%=Encode.forHtmlAttribute(String.valueOf(d.getDemographicNo()))%>&amp;ticklerId=<%=Encode.forHtmlAttribute(String.valueOf(ticklerNo))%>">
                                <i class="glyphicon glyphicon-paperclip"></i> Manage Attachments
                            </button>
                            <span id="attachmentCount" class="badge"><%=attachmentCount%></span>
                        </span>
                        <div id="attachmentNames">
                            <%
                                for (java.util.Map.Entry<DocumentType, String[]> attachEntry : ticklerAttachTypes.entrySet()) {
                                    String paramName = attachEntry.getValue()[0];
                                    String groupLabel = attachEntry.getValue()[1];
                                    StringBuilder groupItems = new StringBuilder();
                                    for (TicklerAttachmentData ticklerAttachment : ticklerAttachments) {
                                        if (ticklerAttachment.getDocumentType() != attachEntry.getKey()) {
                                            continue;
                                        }
                                        groupItems.append("<li>").append(Encode.forHtmlContent(ticklerAttachment.getDisplayName())).append("</li>");
                                    }
                            %>
                            <div class="attachment-group" id="attachmentGroup_<%=paramName%>" <%=groupItems.length() == 0 ? "style=\"display:none;\"" : ""%>>
                                <div class="attachment-group-heading"><%=groupLabel%></div>
                                <ul id="attachmentNames_<%=paramName%>"><%=groupItems.toString()%></ul>
                            </div>
                            <% } %>
                        </div>
                        <%=delegateInputs.toString()%>
                    </td>
                </tr>
                <tr>
                    <td colspan="2" style="vertical-align: bottom;text-align:right; padding-top:15px; border:none;">
                        <oscar:oscarPropertiesCheck property="tickler_email_enabled" value="true">
                            <input type="checkbox" name="emailDemographic" value="true" /><fmt:message key="tickler.ticklerEdit.emailDemographic"/>
                        </oscar:oscarPropertiesCheck>

                        <input type="button" class="btn btn-primary" name="updateTickler"
                               value="<fmt:message key="tickler.ticklerEdit.update"/>" onClick="validate(this.form, false)"/>
                        <input type="button" class="btn" name="cancelChangeTickler"
                               value="<fmt:message key="tickler.ticklerEdit.cancel"/>" onClick="window.close()"/>

                    </td>
                </tr>
            </table>
            <div id="attachDocumentDisplay" style="display:none;"></div>
        </form>
    </div>

    <jsp:include page="/images/spinner.jsp" flush="true"/>

    <script type="text/javascript">
        /**
         * Tickler document attachment dialog.
         *
         * Loads the shared attachment picker (attachDocument.jsp) into a jQuery UI dialog. Existing
         * attachments are pre-checked from the hidden ".delegateAttachment" inputs rendered in the
         * tickler form. On "Save and Close" the full set of checked items is written back as
         * ".delegateAttachment" hidden inputs, which EditTickler2Action reads to synchronise the
         * tickler's attachments (the manager soft-deletes any that were removed).
         */
        jQuery(document).on('click', '#manageAttachmentsBtn', function () {
            var $form = jQuery('#ticklerEditForm');
            var trigger = jQuery(this);
            var title = trigger.attr("title");

            jQuery("#attachDocumentDisplay").load(trigger.data('poload'), function (response, status, xhr) {
                if (status === "success") {
                    // Pre-check the picker boxes for attachments already on this tickler.
                    $form.find(".delegateAttachment").each(function () {
                        var checkboxId = this.name + this.value;
                        jQuery('#attachDocumentsForm').find('#' + jQuery.escapeSelector(checkboxId)).prop('checked', true);
                    });
                }
            }).dialog({
                title: title,
                modal: true,
                closeText: "Save and Close",
                height: 'auto',
                width: 'auto',
                resizable: true,
                open: function (event, ui) {
                    jQuery(this).parent().css({top: 0, left: 0});
                    var closeBtn = jQuery(this).parent().find(".ui-dialog-titlebar-close");
                    closeBtn.removeClass("ui-button-icon-only");
                    closeBtn.addClass("save-and-close-button");
                    closeBtn.html("Save and Close");
                },
                beforeClose: function (event, ui) {
                    // Rebuild the tickler form's attachment set from the picker's checked boxes.
                    // attachToTickler() diffs this against the stored set, so submitting the full
                    // current selection per type is sufficient for both adds and removals.
                    $form.find(".delegateAttachment").remove();
                    jQuery('#attachmentNames ul').empty();
                    jQuery('#attachDocumentsForm')
                        .find(".document_check:checked, .lab_check:checked, .form_check:checked, .eForm_check:checked, .hrm_check:checked")
                        .each(function () {
                            var element = jQuery(this);
                            jQuery("<input />", {
                                type: 'hidden',
                                name: element.attr('name'),
                                value: element.val(),
                                id: "delegate_" + element.attr('name') + element.val(),
                                "class": 'delegateAttachment'
                            }).appendTo($form);
                            jQuery("<li>").text(element.attr('title')).appendTo('#attachmentNames_' + element.attr('name'));
                        });
                    jQuery('#attachmentNames .attachment-group').each(function () {
                        jQuery(this).toggle(jQuery(this).find('li').length > 0);
                    });
                    jQuery('#attachmentCount').text($form.find(".delegateAttachment").length);
                }
            });
        });
    </script>

    </body>
</html>
