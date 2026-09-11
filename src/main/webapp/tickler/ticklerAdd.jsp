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

    ticklerAdd.jsp - Add a new tickler reminder

    Purpose:
    Provides a form for creating new tickler reminders for a patient, with support
    for quick-pick date selection, suggested text templates, and optional write-to-encounter.

    Features:
    - Accumulative quick-pick date selector (years, months, weeks, days offset)
    - Suggested text templates for common tickler messages
    - Write-to-encounter option for chart documentation
    - Multisite and CAISI program provider assignment support
    - Patient demographic search and selection

    Parameters:
    - demographic_no:       Patient demographic number
    - xml_appointment_date: Initial service/appointment date (YYYY-MM-DD)
    - taskTo:               Default task assignee provider number
    - priority:             Tickler priority (High/Normal/Low)
    - parentAjaxId:         Encounter navbar element ID for update notification
    - updateParent:         Whether to update the parent encounter window (true/false)
    - recall:               If present, marks this as a recall tickler
    - docType:              Optional document type for linking
    - docId:                Optional document ID for linking

--%>

<%@ page import="ca.openosp.openo.PMmodule.dao.ProviderDao" %>
<%@ page import="ca.openosp.openo.commn.dao.DemographicDao" %>
<%@ page import="ca.openosp.openo.commn.dao.OscarAppointmentDao" %>
<%@ page import="ca.openosp.openo.commn.dao.TicklerTextSuggestDao" %>
<%@ page import="ca.openosp.openo.commn.dao.UserPropertyDAO" %>
<%@ page import="ca.openosp.openo.commn.model.Demographic" %>
<%@ page import="ca.openosp.openo.commn.model.TicklerTextSuggest" %>
<%@ page import="ca.openosp.openo.commn.model.UserProperty" %>
<%@ page import="ca.openosp.openo.utility.SpringUtils" %>
<%@ page import="ca.openosp.MyDateFormat" %>
<%@ page import="ca.openosp.OscarProperties" %>
<%@ page import="java.util.Calendar" %>
<%@ page import="java.util.GregorianCalendar" %>
<%@ page import="org.owasp.encoder.Encode" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
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
  ProviderDao providerDao = SpringUtils.getBean(ProviderDao.class);
  DemographicDao demographicDao = SpringUtils.getBean(DemographicDao.class);
  UserPropertyDAO propertyDao = (UserPropertyDAO) SpringUtils.getBean(UserPropertyDAO.class);
  OscarAppointmentDao appointmentDao = SpringUtils.getBean(OscarAppointmentDao.class);
  TicklerTextSuggestDao ticklerTextSuggestDao = SpringUtils.getBean(TicklerTextSuggestDao.class);
%>

<%
    String user_no = (String) session.getAttribute("user");
    int nItems = 0;
    String strLimit1 = "0";
    String strLimit2 = "5";
    if (request.getParameter("limit1") != null) strLimit1 = request.getParameter("limit1");
    if (request.getParameter("limit2") != null) strLimit2 = request.getParameter("limit2");
    boolean bFirstDisp = true; //this is the first time to display the window
    if (request.getParameter("bFirstDisp") != null) bFirstDisp = (request.getParameter("bFirstDisp")).equals("true");
    String ChartNo;
    String demoMRP = "";
    String demoName = request.getParameter("name");
    String defaultTaskAssignee = "";

    Demographic demographic = demographicDao.getDemographic(request.getParameter("demographic_no"));
    if (demographic != null) {
        demoName = demographic.getFormattedName();
        demoMRP = demographic.getProviderNo();
        bFirstDisp = false;
    }

    if (demoName == null) {
        demoName = "";
    }

//    LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
//    boolean caisiEnabled = OscarProperties.getInstance().isPropertyActive("caisi");
//    Integer defaultProgramId = null;
//    List<ProgramProvider> programProviders = new ArrayList<ProgramProvider>();
//
//    if (caisiEnabled) {
//        ProgramProviderDAO programProviderDao = SpringUtils.getBean(ProgramProviderDAO.class);
//        programProviders = programProviderDao.getProgramProviderByProviderNo(loggedInInfo.getLoggedInProviderNo());
//        if (programProviders.size() == 1) {
//            defaultProgramId = programProviders.get(0).getProgram().getId();
//        }
//    }

    String parentAjaxId;
    if (request.getParameter("parentAjaxId") != null)
        parentAjaxId = request.getParameter("parentAjaxId");
    else
        parentAjaxId = "";

    String updateParent;
    if (request.getParameter("updateParent") != null)
        updateParent = request.getParameter("updateParent");
    else
        updateParent = "true";

    boolean recall = false;
    String taskTo = user_no; //default current user
    String priority = "Normal";
    if (request.getParameter("taskTo") != null) taskTo = request.getParameter("taskTo");
    if (request.getParameter("priority") != null) priority = request.getParameter("priority");
    if (request.getParameter("recall") != null) recall = true;

    UserProperty prop = propertyDao.getProp(user_no, UserProperty.TICKLER_TASK_ASSIGNEE);
    //don't override taskTo query param
    if (request.getParameter("taskTo") == null) {

        if (prop != null) {
            defaultTaskAssignee = prop.getValue();
            if (!"mrp".equals(defaultTaskAssignee)) {
                taskTo = defaultTaskAssignee;
            } else {
                taskTo = demoMRP;
            }
        }

    }

    GregorianCalendar now = new GregorianCalendar();
    int curYear = now.get(Calendar.YEAR);
    int curMonth = (now.get(Calendar.MONTH) + 1);
    int curDay = now.get(Calendar.DAY_OF_MONTH);

    String xml_vdate = request.getParameter("xml_vdate") == null ? "" : request.getParameter("xml_vdate");
    String xml_appointment_date = request.getParameter("xml_appointment_date") == null ? MyDateFormat.getMysqlStandardDate(curYear, curMonth, curDay) : request.getParameter("xml_appointment_date");


   pageContext.setAttribute("providers", providerDao.getActiveProviders());
   pageContext.setAttribute("taskTo", taskTo);
%>

<!DOCTYPE html>
<html>
    <head>
        <title><fmt:setBundle basename="oscarResources"/><fmt:message key="tickler.ticklerAdd.title"/></title>

      <style>
        *:not(h2):not(.btn) {
          line-height: 1 !important;
          font-size: 12px !important;
        }
        table tr td {
          border:none !important;
        }
      </style>
      <link href="<%=request.getContextPath()%>/share/css/dateTimeQuickPick.css" rel="stylesheet" type="text/css" />
      <link href="<%= request.getContextPath() %>/library/bootstrap/3.0.0/css/bootstrap.css" rel="stylesheet" type="text/css">
      <script type="text/javascript" src="<%=request.getContextPath()%>/share/javascript/dateTimeQuickPick.js"></script>

      <script>
        function pasteMessageText() {
            let selectedIdx = document.serviceform.suggestedText.selectedIndex;
            document.getElementById("ticklerMessage").value = document.serviceform.suggestedText.options[selectedIdx].text;
        }

        // function addQuickPick() {
        //
        //     const dateInput = document.querySelector('input[name="xml_appointment_date"]');
        //     const container = document.getElementById('quickPickDateOptions');
        //     if (!dateInput || !container) return;
        //
        //     container.innerHTML = ''; // Clear existing buttons
        //
        //     const optionsByRow = [
        //         // Years row
        //         [
        //             { label: '1y', years: 1 },
        //             { label: '2y', years: 2 },
        //             { label: '3y', years: 3 },
        //             { label: '5y', years: 5 },
        //             { label: '10y', years: 10 },
        //         ],
        //         // Months row
        //         [
        //             { label: '1m', months: 1 },
        //             { label: '2m', months: 2 },
        //             { label: '3m', months: 3 },
        //             { label: '6m', months: 6 },
        //         ],
        //         // Weeks and Days row
        //         [
        //             { label: '1w', weeks: 1 },
        //             { label: '2w', weeks: 2 },
        //             { label: '3w', weeks: 3 },
        //             { label: '1d', days: 1 },
        //             { label: '2d', days: 2 },
        //             { label: '3d', days: 3 },
        //             { label: 'Clear', isClear: true },
        //         ],
        //     ];
        //
        //     let baseDate = null;
        //     // Accumulate all added offsets here, by type
        //     let totalYears = 0;
        //     let totalMonths = 0;
        //     let totalWeeks = 0;
        //     let totalDays = 0;
        //
        //     const display = document.createElement('div');
        //     display.style.margin = '5px 0';
        //     display.style.fontSize = '0.9em';
        //     display.style.color = '#336';
        //     display.innerHTML = '&nbsp;'; // Reserve vertical space
        //     display.style.minHeight = '1.5em'; // Adjust height to match expected line height
        //     container.parentNode.insertBefore(display, container);
        //
        //     function parseDateInput() {
        //         const val = dateInput.value;
        //         const d = new Date(val);
        //         return isNaN(d) ? new Date() : d;
        //     }
        //     baseDate = parseDateInput();
        //
        //     function updateDisplayAndDate() {
        //         if (!baseDate) return;
        //
        //         // Calculate total days from weeks + days
        //         const daysFromWeeks = totalWeeks * 7;
        //         let date = new Date(baseDate);
        //
        //         // Add years
        //         date.setFullYear(date.getFullYear() + totalYears);
        //         // Add months
        //         date.setMonth(date.getMonth() + totalMonths);
        //         // Add weeks and days
        //         date.setDate(date.getDate() + daysFromWeeks + totalDays);
        //
        //         dateInput.value = date.toISOString().split('T')[0];
        //
        //         // Build display string for total offset
        //         const parts = [];
        //         if (totalYears) parts.push(totalYears + 'y');
        //         if (totalMonths) parts.push(totalMonths + 'm');
        //         if (totalWeeks) parts.push(totalWeeks + 'w');
        //         if (totalDays) parts.push(totalDays + 'd');
        //         if (parts.length === 0) parts.push('0d');
        //
        //         display.innerHTML = "From " + baseDate.toISOString().split('T')[0] + ":&nbsp;&nbsp;&nbsp;&nbsp;<strong>" + parts.join(' ') + "</strong>";
        //     }
        //
        //     function resetTotals() {
        //         totalYears = 0;
        //         totalMonths = 0;
        //         totalWeeks = 0;
        //         totalDays = 0;
        //     }
        //
        //     optionsByRow.forEach(rowOptions => {
        //         const row = document.createElement('div');
        //         rowOptions.forEach(opt => {
        //             const btn = document.createElement('button');
        //             btn.textContent = opt.label;
        //             btn.title = 'Click to add. Shift+Click or Right-click to subtract.';
        //
        //             const handleOffset = (delta) => {
        //                 if (baseDate === null) {
        //                     baseDate = parseDateInput();
        //                     resetTotals();
        //                 }
        //                 if (opt.isClear) {
        //                     const now = new Date();
        //                     const localDate = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        //                     baseDate = localDate;
        //                     resetTotals();
        //                     dateInput.value = localDate.toISOString().split('T')[0];
        //                     display.innerHTML = 'Reset to today: <strong>0d</strong>';
        //                     return;
        //                 }
        //
        //                 // Add or subtract from correct total
        //                 const sign = delta < 0 ? -1 : 1;
        //                 if (opt.years) totalYears += sign * Math.abs(delta);
        //                 if (opt.months) totalMonths += sign * Math.abs(delta);
        //                 if (opt.weeks) totalWeeks += sign * Math.abs(delta);
        //                 if (opt.days) totalDays += sign * Math.abs(delta);
        //
        //                 updateDisplayAndDate();
        //             };
        //
        //             btn.addEventListener('click', e => {
        //                 e.preventDefault();
        //                 const delta = e.shiftKey ? -1 : 1;
        //                 // delta multiplied by the actual unit amount
        //                 const multiplier = opt.years || opt.months || opt.weeks || opt.days || 0;
        //                 handleOffset(delta * multiplier);
        //             });
        //
        //             btn.addEventListener('contextmenu', e => {
        //                 e.preventDefault();
        //                 if (opt.isClear) {
        //                     baseDate = null;
        //                     resetTotals();
        //                     const today = new Date();
        //                     dateInput.value = today.toISOString().split('T')[0];
        //                     display.innerHTML = 'Reset to today: <strong>0d</strong>';
        //                 } else {
        //                     // Subtract the value for this button
        //                     const multiplier = opt.years || opt.months || opt.weeks || opt.days || 0;
        //                     handleOffset(-1 * multiplier);
        //                 }
        //             });
        //
        //             row.appendChild(btn);
        //         });
        //         container.appendChild(row);
        //     });
        // }

        function popupPage(vheight, vwidth, varpage) { //open a new popup window
            let page = "" + varpage;
            windowprops = "height=" + vheight + ",width=" + vwidth + ",location=no,scrollbars=yes,menubars=no,toolbars=no,resizable=yes";
            let popup = window.open(page, "attachment", windowprops);
            if (popup != null) {
                if (popup.opener == null) {
                    popup.opener = self;
                }
            }
        }

        function selectprovider(s) {
            if (self.location.href.lastIndexOf("&providerview=") > 0) a = self.location.href.substring(0, self.location.href.lastIndexOf("&providerview="));
            else a = self.location.href;
            self.location.href = a + "&providerview=" + s.options[s.selectedIndex].value;
        }

        function openBrWindow(theURL, winName, features) {
            window.open(theURL, winName, features);
        }

        function setfocus() {
            this.focus();
            document.ADDAPPT.keyword.focus();
            document.ADDAPPT.keyword.select();
        }

        function initResize() {
            window.addEventListener("resize", resizeTextMessage);
            resizeTextMessage();
        }

        /****
         * This function resizes the messageBox so that the overall browser window is filled.
         ****/
        function resizeTextMessage() {
            const messageBox = document.getElementById("ticklerMessage");
            //this formula checks for empty space at the bottom, less the 20 px that corresponds to the margin-bottom
            const newHeight = messageBox.offsetHeight + window.innerHeight - document.body.clientHeight - 20;
            //only resize if the new height will be greater than 50 pixels, the original default height.
            if (newHeight > 50) messageBox.style.height = newHeight + "px";
        }

        function validate(form, writeToEncounter = false) {
            if (validateDemoNo()) {
                if (writeToEncounter) {
                    form.action = "<%= request.getContextPath() %>/tickler/dbTicklerAdd.jsp?writeToEncounter=true";
                } else {
                    form.action = "<%= request.getContextPath() %>/tickler/dbTicklerAdd.jsp?updateTicklerNav=true";
                }
                form.submit();
            }
        }

        <%--function validateSelectedProgram() {--%>
        <%--    if (document.serviceform.program_assigned_to.value === "none") {--%>
        <%--        document.getElementById("error").insertAdjacentText("beforeend", '<fmt:setBundle basename="oscarResources"/><fmt:message key="tickler.ticklerAdd.msgNoProgramSelected"/>');--%>
        <%--        document.getElementById("error").style.display = 'block';--%>
        <%--        return false;--%>
        <%--    }--%>
        <%--    return true;--%>
        <%--}--%>

        function IsDate(value) {
            let dateWrapper = new Date(value);
            return !isNaN(dateWrapper.getDate());
        }

        function validateDemoNo() {
            let errorDiv = document.getElementById("error");
            if (document.serviceform.demographic_no.value === "") {
                errorDiv.textContent = '<fmt:setBundle basename="oscarResources"/><fmt:message key="tickler.ticklerAdd.msgInvalidDemographic"/>';
                errorDiv.style.display = 'block';
                return false;
            } else {
                if (document.serviceform.xml_appointment_date.value === "" || !IsDate(document.serviceform.xml_appointment_date.value)) {
                    errorDiv.textContent = '<fmt:setBundle basename="oscarResources"/><fmt:message key="tickler.ticklerAdd.msgMissingDate"/>';
                    errorDiv.style.display = 'block';
                    return false;
                }
                <% if (ca.openosp.openo.commn.IsPropertiesOn.isMultisitesEnable()) { %>
                else if (document.serviceform.site.value === "none" || document.serviceform.site.value === "0") {
                    errorDiv.textContent = '<fmt:message key="tickler.ticklerAdd.msgNoProviderAssigned"/>';
                    errorDiv.style.display = 'block';
                    return false;
                }
                <% } %>
                else {
                    return true;
                }
            }
        }

        function refresh() {
            let u = self.location.href;
            if (u.lastIndexOf("view=1") > 0) {
                self.location.href = u.substring(0, u.lastIndexOf("view=1")) + "view=0" + u.substring(u.lastIndexOf("view=1") + 6);
            } else {
                history.go(0);
            }
        }

        // Prefill the message from the opener (same-origin, in-memory) instead of
        // a URL parameter, since it may contain PHI.
        function applyOpenerTicklerPrefill() {
          try {
            if (window.opener && typeof window.opener._eformTicklerPrefillMessage === 'string') {
              var textarea = document.getElementById('ticklerMessage');
              if (textarea) {
                textarea.value = window.opener._eformTicklerPrefillMessage;
              }
              delete window.opener._eformTicklerPrefillMessage;
            }
          } catch (e) {
            // Cross-origin or no opener: nothing to prefill.
          }
        }

        // on load
        document.addEventListener('DOMContentLoaded', function() {
          addQuickPick();
          setfocus();
          initResize();
          applyOpenerTicklerPrefill();
        });
        </script>

<%--        <style media="all">--%>
<%--            .tickler-label {--%>
<%--                color: #003366;--%>
<%--                font-weight: bold;--%>
<%--            }--%>

<%--            table {--%>
<%--                width: 100%;--%>
<%--            }--%>

<%--            * {--%>
<%--                font-size: 12px !important;--%>
<%--            }--%>
<%--            #quickPickDateOptions {--%>
<%--                display: block !important;--%>
<%--            }--%>
<%--            #quickPickDateOptions > div {--%>
<%--                display: flex;--%>
<%--                gap: 6px;--%>
<%--                margin-bottom: 6px;--%>
<%--            }--%>
<%--            #quickPickDateOptions button {--%>
<%--                font-size: 0.7em;--%>
<%--                padding: 3px 6px;--%>
<%--                cursor: pointer;--%>
<%--            }--%>
<%--        </style>--%>
    </head>

    <body>
    <div class="container">
<%--    <table>--%>
<%--        <tr style="background-color: black">--%>
<%--            <td class="table-condensed"--%>
<%--                style="text-align:left; padding:10px; font-weight: 900; height:40px; font-size: large; font-family: arial, sans-serif; color: white">--%>
<%--                Add <fmt:setBundle basename="oscarResources"/><fmt:message key="tickler.ticklerAdd.msgTickler"/></td>--%>
<%--        </tr>--%>
<%--    </table>--%>

    <h2>
      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-feather"
           viewBox="0 0 16 16">
        <path d="M15.807.531c-.174-.177-.41-.289-.64-.363a3.765 3.765 0 0 0-.833-.15c-.62-.049-1.394 0-2.252.175C10.365.545 8.264 1.415 6.315 3.1c-1.95 1.686-3.168 3.724-3.758 5.423-.294.847-.44 1.634-.429 2.268.005.316.05.62.154.88.017.04.035.082.056.122A68.362 68.362 0 0 0 .08 15.198a.528.528 0 0 0 .157.72.504.504 0 0 0 .705-.16 67.606 67.606 0 0 1 2.158-3.26c.285.141.616.195.958.182.513-.02 1.098-.188 1.723-.49 1.25-.605 2.744-1.787 4.303-3.642l1.518-1.55a.528.528 0 0 0 0-.739l-.729-.744 1.311.209a.504.504 0 0 0 .443-.15c.222-.23.444-.46.663-.684.663-.68 1.292-1.325 1.763-1.892.314-.378.585-.752.754-1.107.163-.345.278-.773.112-1.188a.524.524 0 0 0-.112-.172ZM3.733 11.62C5.385 9.374 7.24 7.215 9.309 5.394l1.21 1.234-1.171 1.196a.526.526 0 0 0-.027.03c-1.5 1.789-2.891 2.867-3.977 3.393-.544.263-.99.378-1.324.39a1.282 1.282 0 0 1-.287-.018Zm6.769-7.22c1.31-1.028 2.7-1.914 4.172-2.6a6.85 6.85 0 0 1-.4.523c-.442.533-1.028 1.134-1.681 1.804l-.51.524-1.581-.25Zm3.346-3.357C9.594 3.147 6.045 6.8 3.149 10.678c.007-.464.121-1.086.37-1.806.533-1.535 1.65-3.415 3.455-4.976 1.807-1.561 3.746-2.36 5.31-2.68a7.97 7.97 0 0 1 1.564-.173Z"/>
      </svg>
      <fmt:message key="tickler.ticklerAdd.title"/>
    </h2>

  <div id="error" class="alert alert-danger" style="display:none;"></div>
        <%
            String searchMode = request.getParameter("search_mode");
            if (searchMode == null || searchMode.isEmpty()) {
                searchMode = OscarProperties.getInstance().getProperty("default_search_mode", "search_name");
            }
            ChartNo = bFirstDisp ? "" : request.getParameter("chart_no") == null ? "" : request.getParameter("chart_no");
        %>
        <form name="ADDAPPT" method="post" action="<%= request.getContextPath() %>/appointment/appointmentcontrol.jsp">
            <input type="hidden" name="orderby" value="last_name">
            <input type="hidden" name="search_mode" value="<%=Encode.forHtmlAttribute(searchMode)%>">
            <input type="hidden" name="originalpage" value="<%= request.getContextPath() %>/tickler/ticklerAdd.jsp">
            <input type="hidden" name="limit1" value="0">
            <input type="hidden" name="limit2" value="5">
            <input type="hidden" name="displaymode" value="Search ">
            <input type="hidden" name="appointment_date" value="2002-10-01">
            <input type="hidden" name="status" value="t">
            <input type="hidden" name="start_time" value="10:45">
            <input type="hidden" name="type" value="">
            <input type="hidden" name="duration" value="15">
            <input type="hidden" name="end_time" value="10:59">
            <input type="hidden" name="demographic_no" readonly value="">
            <input type="hidden" name="location" tabindex="4" value="">
            <input type="hidden" name="resources" tabindex="5" value="">
            <input type="hidden" name="user_id" readonly value="oscardoc, doctor">
            <input type="hidden" name="dboperation" value="search_demorecord">
            <input type="hidden" name="createdatetime" readonly value="2002-10-1 17:53:50">
            <input type="hidden" name="provider_no" value="115">
            <input type="hidden" name="creator" value="oscardoc, doctor">
            <input type="hidden" name="remarks" value="">
            <input type="hidden" name="parentAjaxId" value="<%=Encode.forHtmlAttribute(parentAjaxId)%>">
            <input type="hidden" name="updateParent" value="<%=Encode.forHtmlAttribute(updateParent)%>">

          <table class="table table-condensed">
                <tr>
                    <td style="border:none;">
                      <label for="keyword">
                      <fmt:setBundle basename="oscarResources"/><fmt:message key="tickler.ticklerAdd.formDemoName"/>:
                      </label>
                    </td>
                    <td style="border:none;">
                        <div class="input-group">
                            <input type="text" class="form-control" id="keyword" name="keyword" placeholder="Search Demographic" value="<%=Encode.forHtmlAttribute(demoName)%>">
                            <span class="input-group-btn">
                                <input type="submit" name="Submit" class="btn btn-default" value="<fmt:setBundle basename="oscarResources"/><fmt:message key="tickler.ticklerAdd.btnSearch"/>">
                            </span>
                        </div>

                    </td>
                </tr>
            </table>
        </form>
        <form name="serviceform" method="post">
            <input type="hidden" name="parentAjaxId" value="<%=Encode.forHtmlAttribute(parentAjaxId)%>">
            <input type="hidden" name="updateParent" value="<%=Encode.forHtmlAttribute(updateParent)%>">
            <input type="hidden" name="user_no" value="<%=Encode.forHtmlAttribute(user_no)%>">
            <input type="hidden" name="demographic_no" id="demogrpahic_no" value="<%=bFirstDisp ? "" : request.getParameter("demographic_no").isEmpty() ? "" : Encode.forHtmlAttribute(request.getParameter("demographic_no"))%>">
<%--            <input type="hidden" name="writeToEncounter" value="<%=Encode.forHtmlAttribute(writeToEncounter.toString())%>">--%>
            <table class="table table-condensed">

<%--                <tr>--%>
<%--                  <td><label for="demogrpahic_no"> <fmt:setBundle basename="oscarResources"/><fmt:message key="tickler.ticklerAdd.formChartNo"/>:</label></td>--%>
<%--                    <td>--%>
<%--                      <span>--%>

<%--                      <%=Encode.forHtmlContent(ChartNo)%>--%>
<%--                    </span>--%>
<%--                    </td>--%>
<%--                </tr>--%>

                <tr>
                    <td>
                      <label for="xml_appointment_date">
                        <fmt:setBundle basename="oscarResources"/><fmt:message key="tickler.ticklerAdd.formServiceDate"/>:
                      </label>
                    </td>
                    <td>
                      <input type="date" class="form-control" name="xml_appointment_date" id="xml_appointment_date"
                               value="<%=Encode.forHtmlAttribute(xml_appointment_date)%>">
                        </td>
                </tr>
              <tr>
                <td></td>
                <td>
                  <div id="todayButton" class="today-button" onclick="addTime(0, 'days')">Today</div>
                  <div id="quickPickDateOptions" class="grid">
                    <!-- Quick pick will be added here using JavaScript -->
                  </div>

                </td>
              </tr>
                <tr>
                    <td><label for="priority">
                      <fmt:setBundle basename="oscarResources"/><fmt:message key="tickler.ticklerMain.Priority"/>:
                    </label></td>
                    <td>
                        <select name="priority" id="priority" class="form-control">
                            <option value="<fmt:setBundle basename="oscarResources"/><fmt:message key="tickler.ticklerMain.priority.high"/>" <%=priority.equals("High")?"selected":""%>><fmt:setBundle basename="oscarResources"/><fmt:message key="tickler.ticklerMain.priority.high"/>
                            <option value="<fmt:setBundle basename="oscarResources"/><fmt:message key="tickler.ticklerMain.priority.normal"/>" <%=priority.equals("Normal")?"selected":""%>><fmt:setBundle basename="oscarResources"/><fmt:message key="tickler.ticklerMain.priority.normal"/>
                            <option value="<fmt:setBundle basename="oscarResources"/><fmt:message key="tickler.ticklerMain.priority.low"/>" <%=priority.equals("Low")?"selected":""%>><fmt:setBundle basename="oscarResources"/><fmt:message key="tickler.ticklerMain.priority.low"/>
                        </select>
                    </td>
                </tr>

                <tr>
                    <td >
                      <label for="task_assigned_to"><fmt:setBundle basename="oscarResources"/><fmt:message key="tickler.ticklerAdd.assignTaskTo"/>:</label></td>
                    <td>

                        <div id="selectWrapper">
                            <select name="task_assigned_to" id="task_assigned_to" class="form-control">
                              <c:forEach items="${ providers }" var="provider">
                                <option value="${ provider.providerNo }" ${provider.providerNo == taskTo ? 'selected' : ''}>
                                  <c:out value="${ provider.formattedName }"/>
                                </option>
                              </c:forEach>
                            </select>

                            <h4 id="preferenceLink" style="display:none"><small><a href="#" onclick="toggleWrappers()">[preference]</a></small>
                            </h4>
                        </div>

<%--                        <div id="nameWrapper" style="display:none">--%>
<%--                            <h4><%=Encode.forHtml(taskToName)%> <small><a href="#" onclick="toggleWrappers()">[change]</a></small></h4>--%>
<%--                            <input type="hidden" id="taskToBin" value="<%=Encode.forHtmlAttribute(taskTo)%>">--%>
<%--                            <input type="hidden" id="taskToNameBin" value="<%=Encode.forHtmlAttribute(taskToName)%>">--%>
<%--                        </div>--%>


<%--                        <% if (prop != null) {%>--%>
<%--                        <script>--%>
<%--                            //prop exists so hide selectWrapper--%>
<%--                            document.getElementById("selectWrapper").style.display = "none";--%>
<%--                            document.getElementById("nameWrapper").style.display = "block";--%>
<%--                            document.getElementById("preferenceLink").style.display = "inline-block";--%>

<%--                            let taskToValue = document.getElementById("taskToBin").value;--%>
<%--                            let taskToName = document.getElementById('taskToNameBin').value;--%>

<%--                            function toggleWrappers() {--%>
<%--                                if (document.getElementById("selectWrapper").style.display == "none") {--%>
<%--                                    document.getElementById("selectWrapper").style.display = "block";--%>
<%--                                    document.getElementById("nameWrapper").style.display = "none";--%>
<%--                                } else {--%>
<%--                                    document.getElementById("selectWrapper").style.display = "none";--%>
<%--                                    document.getElementById("nameWrapper").style.display = "block";--%>
<%--                                }--%>
<%--                            }--%>

<%--                            _providers.push("<option value=\"" + taskToValue + "\" selected>" + taskToName + "</option>");--%>

<%--                            let newItemKey = _providers.length - 1;--%>

<%--                            let selSite = document.getElementById('site');--%>
<%--                            let optSite = document.createElement('option');--%>
<%--                            optSite.appendChild(document.createTextNode("** preference **"));--%>
<%--                            optSite.value = newItemKey;--%>
<%--                            optSite.setAttribute('selected', 'selected');--%>
<%--                            selSite.appendChild(optSite);--%>
<%--                            changeSite(selSite);--%>
<%--                        </script>--%>
<%--                        <%}%>--%>

<%--                        <% // multisite end ==========================================--%>
<%--                        } else {--%>
<%--                        %>--%>

<%--                        <select name="task_assigned_to" class="form-control">--%>
<%--                            <% String proFirst = "";--%>
<%--                                String proLast = "";--%>
<%--                                String proOHIP = "";--%>

<%--                                for (Provider p : providerDao.getActiveProviders()) {--%>

<%--                                    proFirst = p.getFirstName();--%>
<%--                                    proLast = p.getLastName();--%>
<%--                                    proOHIP = p.getProviderNo();--%>

<%--                            %>--%>
<%--                            <option value="<%=Encode.forHtmlAttribute(proOHIP)%>" <%=taskTo.equals(proOHIP) ? "selected" : ""%>><%=Encode.forHtmlContent(proLast)%>--%>
<%--                                , <%=Encode.forHtmlContent(proFirst)%>--%>
<%--                            </option>--%>
<%--                            <%--%>
<%--                                }--%>
<%--                            %>--%>
<%--                        </select>--%>
<%--                        <% } %>--%>

                        <input type="hidden" name="docType" value="<%=Encode.forHtmlAttribute(request.getParameter("docType") != null ? request.getParameter("docType") : "")%>">
                        <input type="hidden" name="docId" value="<%=Encode.forHtmlAttribute(request.getParameter("docId") != null ? request.getParameter("docId") : "")%>">
                    </td>
                </tr>
                <tr>
                    <td >
                      <a href="#" onclick="openBrWindow('./ticklerSuggestedText.jsp','','width=680,height=400')" >
                      <label for="suggestedText"><fmt:setBundle basename="oscarResources"/><fmt:message key="tickler.ticklerEdit.suggestedText"/>:</label>
                      </a>
                    </td>
                    <td>
                        <select name="suggestedText" id="suggestedText" class="form-control" onchange="pasteMessageText()">
                            <option value="">---</option>
                            <%

                                for (TicklerTextSuggest tTextSuggest : ticklerTextSuggestDao.getActiveTicklerTextSuggests()) {
                            %>
                            <option><%=Encode.forHtmlContent(tTextSuggest.getSuggestedText())%></option>
                            <% } %>
                        </select>
                    </td>
                </tr>

                <tr>
                    <td>
                      <label for="ticklerMessage"><fmt:setBundle basename="oscarResources"/><fmt:message key="tickler.ticklerAdd.formReminder"/>:</label>
                      </td>
                    <td>
                      <textarea name="ticklerMessage" id="ticklerMessage" class="form-control"></textarea>
                    </td>
                </tr>
                <tr>
                    <td colspan="2"><input type="button" name="Button" class="btn btn-primary"
                               value="<fmt:setBundle basename="oscarResources"/><fmt:message key="tickler.ticklerAdd.btnSubmit"/>"
                               onclick="validate(this.form);">
                        <input type="button" name="Button" class="btn btn-danger"
                               value="<fmt:setBundle basename="oscarResources"/><fmt:message key="tickler.ticklerAdd.btnCancel"/>"
                               onclick="window.close()">
                    </td>
                </tr>

            </table>
        </form>
    </div>
    </body>
</html>
