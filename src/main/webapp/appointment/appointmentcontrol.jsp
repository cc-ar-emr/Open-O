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

<%@ page import="java.util.*, oscar.*, oscar.util.*, org.owasp.encoder.Encode, org.oscarehr.util.LoggedInInfo" %>
<%@ taglib uri="/WEB-INF/security.tld" prefix="security" %>
<%
    // Check for session timeout and redirect if needed
    if (session.getAttribute("userrole") == null) {
        response.sendRedirect("../logout.jsp");
        return;
    }
    
    // Verify user is logged in
    LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
    if (loggedInInfo == null) {
        response.sendRedirect("../logout.jsp");
        return;
    }
    
    String roleName$ = (String) session.getAttribute("userrole") + "," + (String) session.getAttribute("user");
%>
<security:oscarSec roleName="<%=roleName$%>" objectName="_appointment"
                   rights="r" reverse="<%=true%>">
    <%
        response.sendRedirect("../logout.jsp");
        return;
    %>
</security:oscarSec>

<%
    // Set security headers
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("X-Frame-Options", "DENY");
    response.setHeader("X-XSS-Protection", "1; mode=block");
%>
<%
    // associate each operation with an output JSP file -- displaymode
    // Using a whitelist approach for security
    String[][] opToFile = new String[][]{
            {"Add Appointment", "appointmentaddarecord.jsp"},
            {"Group Appt", "appointmentgrouprecords.jsp"},
            {"Group Action", "appointmentgrouprecords.jsp"},
            {"Add Appt & PrintPreview", "appointmentaddrecordprint.jsp"},
            {"Add Appt & PrintCard", "appointmentaddrecordcard.jsp"},
            {"PrintCard", "appointmentviewrecordcard.jsp"},
            {"TicklerSearch", "../tickler/ticklerAdd.jsp"},
            {"Search ", "../demographic/demographiccontrol.jsp"},
            {"Search", "appointmentsearchrecords.jsp"},
            {"edit", "editappointment.jsp"},
            {"Update Appt", "appointmentupdatearecord.jsp"},
            {"Delete Appt", "appointmentdeletearecord.jsp"},
            {"Cut", "appointmentcutrecord.jsp"},
            {"Copy", "appointmentcopyrecord.jsp"}
    };
    
    // Define operations that require CSRF protection (state-changing operations)
    Set<String> csrfProtectedOps = new HashSet<String>();
    csrfProtectedOps.add("Add Appointment");
    csrfProtectedOps.add("Group Appt");
    csrfProtectedOps.add("Group Action");
    csrfProtectedOps.add("Add Appt & PrintPreview");
    csrfProtectedOps.add("Add Appt & PrintCard");
    csrfProtectedOps.add("Update Appt");
    csrfProtectedOps.add("Delete Appt");
    csrfProtectedOps.add("Cut");
    csrfProtectedOps.add("Copy");

    try {
        // create an operation-to-file dictionary
        UtilDict opToFileDict = new UtilDict();
        opToFileDict.setDef(opToFile);
    
        // create a request parameter name-to-value dictionary
        UtilDict requestParamDict = new UtilDict();
        requestParamDict.setDef(request);
    
        // get operation name from request and sanitize it
        String operation = requestParamDict.getDef("displaymode", "");
        if (operation == null) {
            operation = "";
        }
        
        // Log the requested operation for audit purposes
        String remoteAddr = request.getRemoteAddr();
        String user = (String) session.getAttribute("user");
        oscar.OscarLogger.getInstance().log("appointment_control", "info", 
            "User: " + user + " IP: " + remoteAddr + " Operation: " + Encode.forHtml(operation));
        
        // Check CSRF token for state-changing operations
        if (csrfProtectedOps.contains(operation)) {
            String csrfToken = request.getParameter("csrf_token");
            String sessionToken = (String) session.getAttribute("csrf_token");
            
            if (csrfToken == null || sessionToken == null || !csrfToken.equals(sessionToken)) {
                oscar.OscarLogger.getInstance().log("appointment_control", "warn", 
                    "CSRF token validation failed for user: " + user + " IP: " + remoteAddr + 
                    " Operation: " + Encode.forHtml(operation));
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Security validation failed");
                return;
            }
            
            // Additional security check for secure actions
            String secureAction = request.getParameter("secure_action");
            if (secureAction == null || !secureAction.equals("true")) {
                // Log potential tampering attempt
                oscar.OscarLogger.getInstance().log("appointment_control", "warn", 
                    "Missing secure_action parameter for user: " + user + " IP: " + remoteAddr + 
                    " Operation: " + Encode.forHtml(operation));
                // Continue processing but log the event
            }
        }
        
        // Get the target JSP file
        String targetJsp = opToFileDict.getDef(operation, "");
        
        // Validate the target JSP (prevent path traversal)
        if (targetJsp == null || targetJsp.isEmpty() || 
            targetJsp.contains("..") || targetJsp.contains("\\") || 
            !targetJsp.endsWith(".jsp")) {
            
            oscar.OscarLogger.getInstance().log("appointment_control", "warn", 
                "Invalid target JSP requested: " + Encode.forHtml(targetJsp) + 
                " by user: " + user + " IP: " + remoteAddr);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid operation");
            return;
        }
        
        // Forward to the appropriate JSP
        pageContext.forward(targetJsp);
    } catch (Exception e) {
        oscar.OscarLogger.getInstance().log("appointment_control", "error", 
            "Error in appointmentcontrol.jsp: " + e.getMessage());
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred");
    }
%>
