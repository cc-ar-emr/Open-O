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

<%@ taglib uri="/WEB-INF/security.tld" prefix="security" %>
<%
    String roleName$ = (String) session.getAttribute("userrole") + "," + (String) session.getAttribute("user");
    boolean authed = true;
%>
<security:oscarSec roleName="<%=roleName$%>" objectName="_appointment" rights="u" reverse="<%=true%>">
    <%authed = false; %>
    <%response.sendRedirect("../securityError.jsp?type=_appointment");%>
</security:oscarSec>
<%
    if (!authed) {
        return;
    }
%>

<%
    // Validate session and check for authentication
    if (session.getAttribute("user") == null) {
        response.sendRedirect("../logout.jsp");
        return;
    }
    
    // Generate CSRF token if not already present
    if (session.getAttribute("csrf_token") == null) {
        java.security.SecureRandom secureRandom = new java.security.SecureRandom();
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String csrfToken = java.util.Base64.getEncoder().encodeToString(bytes);
        session.setAttribute("csrf_token", csrfToken);
    }
    
    String deepcolor = "#CCCCFF", weakcolor = "#EEEEFF", tableTitle = "#99ccff";
    
    // Validate appointment_no parameter
    String appointmentNoParam = request.getParameter("appointment_no");
    boolean bEdit = false;
    if (appointmentNoParam != null) {
        // Validate appointment_no is numeric
        if (!appointmentNoParam.matches("^\\d+$")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid appointment number");
            return;
        }
        bEdit = true;
    }
%>
<%@ page import="java.util.*, oscar.*, oscar.util.*, java.sql.*"
         errorPage="/errorpage.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>


<%@page import="org.oscarehr.common.dao.AppointmentArchiveDao" %>
<%@page import="org.oscarehr.common.dao.OscarAppointmentDao" %>
<%@page import="org.oscarehr.common.model.Appointment" %>
<%@page import="org.oscarehr.util.SpringUtils" %>
<%@page import="java.text.SimpleDateFormat" %>
<%
    AppointmentArchiveDao appointmentArchiveDao = (AppointmentArchiveDao) SpringUtils.getBean(AppointmentArchiveDao.class);
    OscarAppointmentDao appointmentDao = (OscarAppointmentDao) SpringUtils.getBean(OscarAppointmentDao.class);
    SimpleDateFormat dayFormatter = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");
    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
%>
<%!
    GregorianCalendar addDateByYMD(GregorianCalendar cal, String unit, int n) {
        if (unit.equals("day")) {
            cal.add(Calendar.DATE, n);
        } else if (unit.equals("month")) {
            cal.add(Calendar.MONTH, n);
        } else if (unit.equals("year")) {
            cal.add(Calendar.YEAR, n);
        }
        return cal;
    }
%>
<%
    if (request.getParameter("groupappt") != null) {
        // Verify CSRF token
        String csrfToken = request.getParameter("csrf_token");
        String sessionToken = (String) session.getAttribute("csrf_token");
        
        if (csrfToken == null || sessionToken == null || !csrfToken.equals(sessionToken)) {
            oscar.OscarLogger.getInstance().log("appointment_control", "warn", 
                "CSRF token validation failed for user: " + session.getAttribute("user") + 
                " IP: " + request.getRemoteAddr());
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Security validation failed");
            return;
        }
        
        boolean bSucc = false;
        String createdDateTime = UtilDateUtilities.DateToString(new java.util.Date(), "yyyy-MM-dd HH:mm:ss");
        String userName = (String) session.getAttribute("userlastname") + ", " + (String) session.getAttribute("userfirstname");
        
        // Validate and sanitize input parameters
        String everyNum = request.getParameter("everyNum");
        if (everyNum == null || !everyNum.matches("^[1-9][0-9]?$")) {
            everyNum = "1"; // Default to 1 if invalid
        }
        
        String everyUnit = request.getParameter("everyUnit");
        if (everyUnit == null || !(everyUnit.equals("day") || everyUnit.equals("week") || 
                                  everyUnit.equals("month") || everyUnit.equals("year"))) {
            everyUnit = "day"; // Default to day if invalid
        }
        
        String endDate = request.getParameter("endDate");
        if (endDate == null || !endDate.matches("^\\d{2}/\\d{2}/\\d{4}$")) {
            endDate = UtilDateUtilities.DateToString(new java.util.Date(), "dd/MM/yyyy");
        }
        
        int delta;
        try {
            delta = Integer.parseInt(everyNum);
            if (delta <= 0) delta = 1; // Ensure positive value
            if (delta > 99) delta = 99; // Limit to reasonable value
        } catch (NumberFormatException e) {
            delta = 1; // Default to 1 if parsing fails
        }
        
        if (everyUnit.equals("week")) {
            delta = delta * 7;
            everyUnit = "day";
        }
        GregorianCalendar gCalDate = new GregorianCalendar();
        GregorianCalendar gEndDate = (GregorianCalendar) gCalDate.clone();
        gEndDate.setTime(UtilDateUtilities.StringToDate(endDate, "dd/MM/yyyy"));

        // repeat adding
        if (request.getParameter("groupappt").equals("Add Group Appointment")) {
            String[] param = new String[19];
            int rowsAffected = 0, datano = 0;

            // Validate appointment_date parameter
            String appointmentDate = request.getParameter("appointment_date");
            if (appointmentDate == null || !appointmentDate.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid appointment date format");
                return;
            }
            
            java.util.Date iDate;
            try {
                iDate = ConversionUtils.fromDateString(appointmentDate);
                if (iDate == null) {
                    throw new IllegalArgumentException("Invalid date");
                }
            } catch (Exception e) {
                oscar.OscarLogger.getInstance().log("appointment_control", "error", 
                    "Error parsing appointment date: " + e.getMessage());
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid appointment date");
                return;
            }

            while (true) {
                Appointment a = new Appointment();
                // Validate and sanitize input parameters
                String providerNo = request.getParameter("provider_no");
                if (providerNo == null || providerNo.isEmpty()) {
                    providerNo = (String) session.getAttribute("user");
                }
                
                String startTime = request.getParameter("start_time");
                if (startTime == null || !startTime.matches("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                    oscar.OscarLogger.getInstance().log("appointment_control", "error", 
                        "Invalid start time format: " + startTime);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid start time format");
                    return;
                }
                
                String endTime = request.getParameter("end_time");
                if (endTime == null || !endTime.matches("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                    oscar.OscarLogger.getInstance().log("appointment_control", "error", 
                        "Invalid end time format: " + endTime);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid end time format");
                    return;
                }
                
                // Set appointment properties with validated inputs
                a.setProviderNo(providerNo);
                a.setAppointmentDate(iDate);
                a.setStartTime(ConversionUtils.fromTimeStringNoSeconds(startTime));
                a.setEndTime(ConversionUtils.fromTimeStringNoSeconds(endTime));
                a.setName(org.apache.commons.lang.StringEscapeUtils.escapeHtml(request.getParameter("keyword")));
                a.setNotes(org.apache.commons.lang.StringEscapeUtils.escapeHtml(request.getParameter("notes")));
                a.setReason(org.apache.commons.lang.StringEscapeUtils.escapeHtml(request.getParameter("reason")));
                a.setLocation(org.apache.commons.lang.StringEscapeUtils.escapeHtml(request.getParameter("location")));
                a.setResources(org.apache.commons.lang.StringEscapeUtils.escapeHtml(request.getParameter("resources")));
                a.setType(org.apache.commons.lang.StringEscapeUtils.escapeHtml(request.getParameter("type")));
                a.setStyle(org.apache.commons.lang.StringEscapeUtils.escapeHtml(request.getParameter("style")));
                a.setBilling(org.apache.commons.lang.StringEscapeUtils.escapeHtml(request.getParameter("billing")));
                a.setStatus(org.apache.commons.lang.StringEscapeUtils.escapeHtml(request.getParameter("status")));
                a.setCreateDateTime(new java.util.Date());
                a.setCreator(userName);
                a.setRemarks(org.apache.commons.lang.StringEscapeUtils.escapeHtml(request.getParameter("remarks")));
                
                // Validate demographic_no parameter
                String demographicNo = request.getParameter("demographic_no");
                if (demographicNo != null && !demographicNo.isEmpty()) {
                    if (!demographicNo.matches("^\\d+$")) {
                        oscar.OscarLogger.getInstance().log("appointment_control", "error", 
                            "Invalid demographic number format: " + demographicNo);
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid demographic number");
                        return;
                    }
                    a.setDemographicNo(Integer.parseInt(demographicNo));
                } else {
                    a.setDemographicNo(0);
                }

                // Validate programId parameter
                String programId = (String) request.getSession().getAttribute("programId_oscarView");
                if (programId == null || !programId.matches("^\\d+$")) {
                    programId = "0"; // Default to 0 if invalid
                }
                a.setProgramId(Integer.parseInt(programId));
                
                // Validate urgency parameter
                String urgency = request.getParameter("urgency");
                if (urgency != null) {
                    urgency = org.apache.commons.lang.StringEscapeUtils.escapeHtml(urgency);
                }
                a.setUrgency(urgency);
                
                // Validate reasonCode parameter
                String reasonCode = request.getParameter("reasonCode");
                if (reasonCode == null || !reasonCode.matches("^-?\\d+$")) {
                    reasonCode = "-1"; // Default to -1 if invalid
                }
                a.setReasonCode(Integer.parseInt(reasonCode));
                
                // Persist the appointment
                appointmentDao.persist(a);
                
                // Log the appointment creation
                oscar.OscarLogger.getInstance().log("appointment_control", "info", 
                    "Created repeat appointment for date: " + UtilDateUtilities.DateToString(iDate) + 
                    ", provider: " + providerNo);


                gCalDate.setTime(UtilDateUtilities.StringToDate(param[1], "yyyy-MM-dd"));
                gCalDate = addDateByYMD(gCalDate, everyUnit, delta);

                if (gCalDate.after(gEndDate))
                    break;
                else
                    iDate = gCalDate.getTime();
            }
            bSucc = true;
        }


        // repeat updating
        if (request.getParameter("groupappt").equals("Group Update") || request.getParameter("groupappt").equals("Group Cancel") ||
                request.getParameter("groupappt").equals("Group Delete")) {
            int rowsAffected = 0, datano = 0;

            Object[] paramE = new Object[10];
            Appointment aa = appointmentDao.find(Integer.parseInt(request.getParameter("appointment_no")));
            if (aa != null) {
                paramE[0] = ConversionUtils.toDateString(aa.getAppointmentDate());
                paramE[1] = aa.getProviderNo();
                paramE[2] = ConversionUtils.toTimeStringNoSeconds(aa.getStartTime());
                paramE[3] = ConversionUtils.toTimeStringNoSeconds(aa.getEndTime());
                paramE[4] = aa.getName();
                paramE[5] = aa.getNotes();
                paramE[6] = aa.getReason();
                paramE[7] = ConversionUtils.toTimestampString(aa.getCreateDateTime());
                paramE[8] = aa.getCreator();
                paramE[9] = String.valueOf(aa.getDemographicNo());

            }

            // group cancel
            if (request.getParameter("groupappt").equals("Group Cancel")) {
                // Verify user has permission to cancel appointments
                LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
                if (loggedInInfo == null) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
                    return;
                }
            
                // Check if user has permission to cancel appointments
                if (!loggedInInfo.getCurrentProvider().hasAdminRole() && 
                    !loggedInInfo.getLoggedInProviderNo().equals(paramE[1])) {
                    oscar.OscarLogger.getInstance().log("appointment_control", "warn", 
                        "Unauthorized attempt to cancel appointments for provider: " + paramE[1] + 
                        " by user: " + loggedInInfo.getLoggedInProviderNo());
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Not authorized to cancel these appointments");
                    return;
                }
            
                Object[] param = new Object[13];
                param[0] = "C";
                param[1] = createdDateTime;
                param[2] = userName;
                for (int k = 0; k < paramE.length; k++) param[k + 3] = paramE[k];

                // repeat doing
                while (true) {
                    Appointment appt = appointmentDao.find(Integer.parseInt(request.getParameter("appointment_no")));
                    appointmentArchiveDao.archiveAppointment(appt);

                    List<Appointment> appts = appointmentDao.find(dayFormatter.parse((String) param[3]), (String) param[4], ConversionUtils.fromTimeStringNoSeconds((String) param[5]), ConversionUtils.fromTimeStringNoSeconds((String) param[6]),
                            (String) param[7], (String) param[8], (String) param[9], ConversionUtils.fromTimestampString((String) param[10]), (String) param[11], Integer.parseInt((String) param[12]));

                    for (Appointment a : appts) {
                        a.setStatus("C");
                        a.setUpdateDateTime(ConversionUtils.fromTimestampString(createdDateTime));
                        a.setLastUpdateUser(userName);
                        appointmentDao.merge(a);
                        rowsAffected++;
                    }

                    gCalDate.setTime(UtilDateUtilities.StringToDate((String) param[3], "yyyy-MM-dd"));
                    gCalDate = addDateByYMD(gCalDate, everyUnit, delta);

                    if (gCalDate.after(gEndDate)) break;
                    else param[3] = UtilDateUtilities.DateToString(gCalDate.getTime(), "yyyy-MM-dd");
                }
                bSucc = true;
            }

            // group delete
            if (request.getParameter("groupappt").equals("Group Delete")) {
                // Verify user has permission to delete appointments
                LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
                if (loggedInInfo == null) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
                    return;
                }
            
                // Check if user has permission to delete appointments
                if (!loggedInInfo.getCurrentProvider().hasAdminRole() && 
                    !loggedInInfo.getLoggedInProviderNo().equals(paramE[1])) {
                    oscar.OscarLogger.getInstance().log("appointment_control", "warn", 
                        "Unauthorized attempt to delete appointments for provider: " + paramE[1] + 
                        " by user: " + loggedInInfo.getLoggedInProviderNo());
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Not authorized to delete these appointments");
                    return;
                }
            
                Object[] param = new Object[10];
                for (int k = 0; k < paramE.length; k++) param[k] = paramE[k];

                // repeat doing
                while (true) {

                    List<Appointment> appts = appointmentDao.find(dayFormatter.parse((String) param[0]), (String) param[1], ConversionUtils.fromTimeStringNoSeconds((String) param[2]), ConversionUtils.fromTimeStringNoSeconds((String) param[3]),
                            (String) param[4], (String) param[5], (String) param[6], ConversionUtils.fromTimestampString((String) param[7]), (String) param[8], Integer.parseInt((String) param[9]));
                    for (Appointment appt : appts) {
                        appointmentArchiveDao.archiveAppointment(appt);
                        appointmentDao.remove(appt.getId());
                        rowsAffected++;
                    }

                    gCalDate.setTime(UtilDateUtilities.StringToDate((String) param[0], "yyyy-MM-dd"));
                    gCalDate = addDateByYMD(gCalDate, everyUnit, delta);

                    if (gCalDate.after(gEndDate)) break;
                    else param[0] = UtilDateUtilities.DateToString(gCalDate.getTime(), "yyyy-MM-dd");
                }
                bSucc = true;
            }

            if (request.getParameter("groupappt").equals("Group Update")) {
                // Verify user has permission to update appointments
                LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
                if (loggedInInfo == null) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
                    return;
                }
            
                // Check if user has permission to update appointments
                if (!loggedInInfo.getCurrentProvider().hasAdminRole() && 
                    !loggedInInfo.getLoggedInProviderNo().equals(paramE[1])) {
                    oscar.OscarLogger.getInstance().log("appointment_control", "warn", 
                        "Unauthorized attempt to update appointments for provider: " + paramE[1] + 
                        " by user: " + loggedInInfo.getLoggedInProviderNo());
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Not authorized to update these appointments");
                    return;
                }
            
                // Validate input parameters
                String startTime = request.getParameter("start_time");
                if (startTime == null || !startTime.matches("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                    oscar.OscarLogger.getInstance().log("appointment_control", "error", 
                        "Invalid start time format: " + startTime);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid start time format");
                    return;
                }
            
                String endTime = request.getParameter("end_time");
                if (endTime == null || !endTime.matches("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                    oscar.OscarLogger.getInstance().log("appointment_control", "error", 
                        "Invalid end time format: " + endTime);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid end time format");
                    return;
                }
            
                // Validate demographic_no parameter
                String demographicNo = request.getParameter("demographic_no");
                if (demographicNo != null && !demographicNo.isEmpty() && !demographicNo.matches("^\\d+$")) {
                    oscar.OscarLogger.getInstance().log("appointment_control", "error", 
                        "Invalid demographic number format: " + demographicNo);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid demographic number");
                    return;
                }
            
                // Validate reasonCode parameter
                String reasonCode = request.getParameter("reasonCode");
                if (reasonCode == null || !reasonCode.matches("^-?\\d+$")) {
                    reasonCode = "-1"; // Default to -1 if invalid
                }
            
                Object[] param = new Object[22];
                param[0] = MyDateFormat.getTimeXX_XX_XX(startTime);
                param[1] = MyDateFormat.getTimeXX_XX_XX(endTime);
                param[2] = org.apache.commons.lang.StringEscapeUtils.escapeHtml(request.getParameter("keyword"));
                param[3] = demographicNo;
                param[4] = org.apache.commons.lang.StringEscapeUtils.escapeHtml(request.getParameter("notes"));
                param[5] = org.apache.commons.lang.StringEscapeUtils.escapeHtml(request.getParameter("reason"));
                param[6] = org.apache.commons.lang.StringEscapeUtils.escapeHtml(request.getParameter("location"));
                param[7] = org.apache.commons.lang.StringEscapeUtils.escapeHtml(request.getParameter("resources"));
                param[8] = createdDateTime;
                param[9] = userName;
                param[10] = org.apache.commons.lang.StringEscapeUtils.escapeHtml(request.getParameter("urgency"));
                param[11] = reasonCode;
                for (int k = 0; k < paramE.length; k++)
                    param[k + 12] = paramE[k];

                // repeat doing
                while (true) {
                    List<Appointment> appts = appointmentDao.find(dayFormatter.parse((String) paramE[0]), (String) paramE[1], ConversionUtils.fromTimeStringNoSeconds((String) paramE[2]), ConversionUtils.fromTimeStringNoSeconds((String) paramE[3]),
                            (String) paramE[4], (String) paramE[5], (String) paramE[6], ConversionUtils.fromTimestampString((String) paramE[7]), (String) paramE[8], Integer.parseInt((String) paramE[9]));
                    for (Appointment appt : appts) {
                        appointmentArchiveDao.archiveAppointment(appt);
                        // Validate and sanitize input parameters
                        String startTime = request.getParameter("start_time");
                        if (startTime == null || !startTime.matches("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                            startTime = "00:00"; // Default if invalid
                        }
                        
                        String endTime = request.getParameter("end_time");
                        if (endTime == null || !endTime.matches("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                            endTime = "00:15"; // Default if invalid
                        }
                        
                        // Validate reasonCode parameter
                        String reasonCode = request.getParameter("reasonCode");
                        if (reasonCode == null || !reasonCode.matches("^-?\\d+$")) {
                            reasonCode = "-1"; // Default to -1 if invalid
                        }
                        
                        // Set appointment properties with validated inputs
                        appt.setStartTime(ConversionUtils.fromTimeString(MyDateFormat.getTimeXX_XX_XX(startTime)));
                        appt.setEndTime(ConversionUtils.fromTimeString(MyDateFormat.getTimeXX_XX_XX(endTime)));
                        appt.setName(org.apache.commons.lang.StringEscapeUtils.escapeHtml(request.getParameter("keyword")));
                        
                        // Validate demographic_no from paramE
                        if (paramE[9] != null && paramE[9].toString().matches("^\\d+$")) {
                            appt.setDemographicNo(Integer.parseInt((String) paramE[9]));
                        } else {
                            appt.setDemographicNo(0);
                        }
                        
                        appt.setNotes(org.apache.commons.lang.StringEscapeUtils.escapeHtml(request.getParameter("notes")));
                        appt.setReason(org.apache.commons.lang.StringEscapeUtils.escapeHtml(request.getParameter("reason")));
                        appt.setLocation(org.apache.commons.lang.StringEscapeUtils.escapeHtml(request.getParameter("location")));
                        appt.setResources(org.apache.commons.lang.StringEscapeUtils.escapeHtml(request.getParameter("resources")));
                        appt.setUpdateDateTime(ConversionUtils.fromTimestampString(createdDateTime));
                        appt.setLastUpdateUser(userName);
                        appt.setUrgency(org.apache.commons.lang.StringEscapeUtils.escapeHtml(request.getParameter("urgency")));
                        appt.setReasonCode(Integer.parseInt(reasonCode));
                        appointmentDao.merge(appt);
                        rowsAffected++;
                    }


                    gCalDate.setTime(UtilDateUtilities.StringToDate((String) param[12], "yyyy-MM-dd"));
                    gCalDate = addDateByYMD(gCalDate, everyUnit, delta);

                    if (gCalDate.after(gEndDate)) break;
                    else param[12] = UtilDateUtilities.DateToString(gCalDate.getTime(), "yyyy-MM-dd");
                }
                bSucc = true;
            }
        }

        if (bSucc) {
%>
<h1><fmt:setBundle basename="oscarResources"/><fmt:message key="appointment.appointmentgrouprecords.msgAddSuccess"/></h1>
<script LANGUAGE="JavaScript">
    self.opener.refresh();
    self.close();
</script>
<%
} else {
%>
<p>
<h1><fmt:setBundle basename="oscarResources"/><fmt:message key="appointment.appointmentgrouprecords.msgAddFailure"/></h1>

<%
        }
        return;
    }
%>
<html>
    <head>
        <script type="text/javascript" src="<%= request.getContextPath() %>/js/global.js"></script>
        <title><fmt:setBundle basename="oscarResources"/><fmt:message key="appointment.appointmentgrouprecords.title"/></title>
        <script language="JavaScript">
            <!--

            function onCheck(a, b) {
                if (a.checked) {
                    document.getElementById("everyUnit").value = b;
                    //document.groupappt.everyUnit.value = b;
                }
            }


            function onExit() {
                if (confirm("<fmt:setBundle basename="oscarResources"/><fmt:message key="appointment.appointmentgrouprecords.msgExitConfirmation"/>")) {
                    window.close()
                }
            }

            var saveTemp = 0;

            function onButDelete() {
                saveTemp = 1;
            }

            function onSub() {
                if (saveTemp == 1) {
                    return (confirm("<fmt:setBundle basename="oscarResources"/><fmt:message key="appointment.appointmentgrouprecords.msgDeleteConfirmation"/>"));
                }
            }

            //-->
        </script>
        <!-- calendar stylesheet -->
        <link rel="stylesheet" type="text/css" media="all"
              href="<%= request.getContextPath() %>/share/calendar/calendar.css" title="win2k-cold-1"/>

        <!-- main calendar program -->
        <script type="text/javascript" src="<%= request.getContextPath() %>/share/calendar/calendar.js"></script>

        <!-- language for the calendar -->
        <script type="text/javascript"
                src="<%= request.getContextPath() %>/share/calendar/lang/<fmt:setBundle basename="oscarResources"/><fmt:message key="global.javascript.calendar"/>"></script>

        <!-- the following script defines the Calendar.setup helper function, which makes
               adding a calendar a matter of 1 or 2 lines of code. -->
        <script type="text/javascript" src="<%= request.getContextPath() %>/share/calendar/calendar-setup.js"></script>
    </head>

    <body bgcolor="ivory" onLoad="setfocus()" topmargin="0" leftmargin="0"
          rightmargin="0">
    <form name="groupappt" method="POST"
          action="appointmenteditrepeatbooking.jsp" onSubmit="return ( onSub());">
        <INPUT TYPE="hidden" NAME="groupappt" value="">
        <INPUT TYPE="hidden" NAME="csrf_token" value="<%= session.getAttribute("csrf_token") %>">
        <table width="100%" BGCOLOR="silver">
            <tr>
                <TD>
                    <% if (bEdit) { %> <INPUT TYPE="button"
                                              onclick="document.forms['groupappt'].groupappt.value='Group Update'; document.forms['groupappt'].submit();"
                                              VALUE="<fmt:setBundle basename="oscarResources"/><fmt:message key="appointment.appointmentgrouprecords.btnGroupUpdate"/>">
                    <INPUT TYPE="button"
                           onclick="document.forms['groupappt'].groupappt.value='Group Cancel'; document.forms['groupappt'].submit();"
                           VALUE="<fmt:setBundle basename="oscarResources"/><fmt:message key="appointment.appointmentgrouprecords.btnGroupCancel"/>">
                    <INPUT TYPE="button"
                           onclick="document.forms['groupappt'].groupappt.value='Group Delete'; document.forms['groupappt'].submit();"
                           VALUE="<fmt:setBundle basename="oscarResources"/><fmt:message key="appointment.appointmentgrouprecords.btnGroupDelete"/>"
                           onClick="onButDelete()"> <% } else { %> <INPUT
                        TYPE="button"
                        onclick="document.forms['groupappt'].groupappt.value='Add Group Appointment'; document.forms['groupappt'].submit();"
                        VALUE="<fmt:setBundle basename="oscarResources"/><fmt:message key="appointment.appointmentgrouprecords.btnAddGroupAppt"/>">
                    <% } %>
                </TD>
                <TD align="right"><INPUT TYPE="button"
                                         VALUE=" <fmt:setBundle basename="oscarResources"/><fmt:message key="global.btnBack"/> "
                                         onClick="window.history.go(-1);return false;"> <INPUT
                        TYPE="button" VALUE=" <fmt:setBundle basename="oscarResources"/><fmt:message key="global.btnExit"/> "
                        onClick="onExit()"></TD>
            </tr>
        </table>

        <table border=0 cellspacing=0 cellpadding=0 width="100%">
            <tr bgcolor="<%=deepcolor%>">
                <th><font face="Helvetica"><fmt:setBundle basename="oscarResources"/><fmt:message key="appointment.appointmenteditrepeatbooking.title"/></font>
                </th>
            </tr>
        </table>

        <table border="0" cellspacing="1" cellpadding="2" width="100%">
            <tr>
                <td width="20%"></td>
                <td nowrap><fmt:setBundle basename="oscarResources"/><fmt:message key="appointment.appointmenteditrepeatbooking.howoften"/></td>
            </tr>
            <tr>
                <td></td>
                <td nowrap>&nbsp;&nbsp;&nbsp;

                    <input type="radio" name="dateUnit" value="<fmt:setBundle basename="oscarResources"/><fmt:message key="day"/>"   <%="checked"%>
                           onclick='onCheck(this, "day")'><fmt:setBundle basename="oscarResources"/><fmt:message key="day"/> &nbsp;&nbsp;
                    <input type="radio" name="dateUnit" value="<fmt:setBundle basename="oscarResources"/><fmt:message key="week"/>"  <%=""%>
                           onclick='onCheck(this, "week")'><fmt:setBundle basename="oscarResources"/><fmt:message key="week"/> &nbsp;&nbsp;
                    <input type="radio" name="dateUnit" value="<fmt:setBundle basename="oscarResources"/><fmt:message key="month"/>" <%=""%>
                           onclick='onCheck(this, "month")'><fmt:setBundle basename="oscarResources"/><fmt:message key="month"/> &nbsp;&nbsp;
                    <input type="radio" name="dateUnit" value="<fmt:setBundle basename="oscarResources"/><fmt:message key="year"/>"  <%=""%>
                           onclick='onCheck(this, "year")'><fmt:setBundle basename="oscarResources"/><fmt:message key="year"/></td>
            </tr>
        </table>

        <table border="0" cellspacing="1" cellpadding="2" width="100%">
            <tr>
                <td width="20%"></td>
                <td width="16%" nowrap><fmt:setBundle basename="oscarResources"/><fmt:message key="appointment.appointmenteditrepeatbooking.every"/></td>
                <td nowrap><select name="everyNum">
                    <%
                        for (int i = 1; i < 12; i++) {
                    %>
                    <option value="<%=i%>"><%=i%>
                    </option>
                    <%
                        }
                    %>
                </select> <input type="text" name="everyUnit" id="everyUnit" size="10"
                                 value="<%="day"%>" readonly></td>
            </tr>
            <tr>
                <td></td>
                <td><fmt:setBundle basename="oscarResources"/><fmt:message key="appointment.appointmenteditrepeatbooking.endon"/> &nbsp;&nbsp;
                    <button type="button" id="f_trigger_b">...</button>
                    <br>
                    <font size="-1"><fmt:setBundle basename="oscarResources"/><fmt:message key="ddmmyyyy"/></font></td>
                <td nowrap valign="top"><input type="text" name="endDate"
                                               id="endDate" size="10"
                                               value="<%=UtilDateUtilities.DateToString(new java.util.Date(),"dd/MM/yyyy")%>"
                                               readonly></td>
            </tr>
        </table>
        <%
            String temp = null;
            for (Enumeration e = request.getParameterNames(); e.hasMoreElements(); ) {
                temp = e.nextElement().toString();
                if (temp.equals("dboperation") || temp.equals("displaymode") || temp.equals("search_mode") || temp.equals("chart_no"))
                    continue;
                
                // Sanitize parameter names and values to prevent XSS
                String paramName = org.owasp.encoder.Encode.forHtmlAttribute(temp);
                String paramValue = org.owasp.encoder.Encode.forHtmlAttribute(request.getParameter(temp));
                
                out.println("<input type='hidden' name='" + paramName + "' value=\"" + paramValue + "\">");
            }
        %>
    </form>

    <script type="text/javascript">
        // Add CSRF token validation to form submission
        function validateForm() {
            var csrfToken = document.querySelector('input[name="csrf_token"]');
            if (!csrfToken || csrfToken.value.trim() === '') {
                alert("Security validation failed. Please refresh the page and try again.");
                return false;
            }
            return true;
        }
            
        // Override form submission to include validation
        var form = document.forms['groupappt'];
        if (form) {
            var originalSubmit = form.onsubmit;
            form.onsubmit = function(event) {
                if (!validateForm()) {
                    event.preventDefault();
                    return false;
                }
                if (originalSubmit) {
                    return originalSubmit.call(this, event);
                }
                return true;
            };
        }
            
        Calendar.setup({
            inputField: "endDate",      // id of the input field
            ifFormat: "%d/%m/%Y",       // format of the input field
            showsTime: false,            // will display a time selector
            button: "f_trigger_b",   // trigger for the calendar (button ID)
            singleClick: true,           // double-click mode
            step: 1                // show all years in drop-down boxes (instead of every other year as default)
        });
    </script>

    </body>
</html>
