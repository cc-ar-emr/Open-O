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
<%@ include file="/taglibs.jsp" %>

<%@ taglib uri="/WEB-INF/security.tld" prefix="security" %>
<%
    String roleName$ = (String) session.getAttribute("userrole") + "," + (String) session.getAttribute("user");
    boolean authed = true;
%>
<security:oscarSec roleName="<%=roleName$%>" objectName="_eChart" rights="w" reverse="<%=true%>">
    <%authed = false; %>
    <%response.sendRedirect("../securityError.jsp?type=_eChart");%>
</security:oscarSec>
<%
    if (!authed) {
        return;
    }
%>

<html>
<head>
    <title></title>
    <link rel="stylesheet" type="text/css" href='${request.contextPath}/jsCalendar/skins/aqua/theme.css'/>

    <link rel="stylesheet" type="text/css" media="all" href="<%=request.getContextPath()%>/share/calendar/calendar.css"
          title="win2k-cold-1"/>
    <script type="text/javascript" src="<%=request.getContextPath()%>/share/calendar/calendar.js"></script>
    <script type="text/javascript"
            src="<%=request.getContextPath()%>/share/calendar/lang/<fmt:setBundle basename="oscarResources"/><fmt:message key="global.javascript.calendar"/>"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/share/calendar/calendar-setup.js"></script>

</head>
<body>
Enter Ocular Procedure
<br/>

<form action="${pageContext.request.contextPath}/eyeform/OcularProc.do" method="post">
    <table style="margin-left:auto;margin-right:auto;background-color:#f0f0f0;border-collapse:collapse">
        <input type="hidden" name="method" value="save"/>

        <input type="hidden" name="demographicNo" id="demographicNo"/>
        <input type="hidden" name="appointmentNo" id="appointmentNo"/>
        <input type="hidden" name="id" id="id"/>
        <input type="hidden" name="status" id="status"/>


        <tr>
            <td class="genericTableHeader">Date</td>
            <td class="genericTableData">
                <input type="text" name="proc.dateStr" size="10" id="pdate"/> <img
                    src="<%=request.getContextPath()%>/images/cal.gif" id="pdate_cal">
            </td>
            <script type="text/javascript">
                Calendar.setup({
                    inputField: "pdate",
                    ifFormat: "%Y-%m-%d",
                    showsTime: false,
                    button: "pdate_cal",
                    singleClick: true,
                    step: 1
                });
            </script>
        </tr>
        <tr>
            <td class="genericTableHeader">Eye</td>
            <td class="genericTableData">
                <select name="eye">
                    <option value="OD">OD</option>
                    <option value="OS">OS</option>
                    <option value="OU">OU</option>
                </select>
            </td>
        </tr>
        <tr>
            <td class="genericTableHeader">Procedure</td>
            <td class="genericTableData">
                <input type="checkbox" name="proc.procedureName" size="50" />
            </td>
        </tr>
        <tr>
            <td class="genericTableHeader">Doctor Name</td>
            <td class="genericTableData">
                <select name="doctor">
                    <c:forEach var="provider" items="${providers}">
                        <option value="${provider.providerNo}">
                                ${provider.formattedName}
                        </option>
                    </c:forEach>
                </select>
            </td>
        </tr>
        <tr>
            <td class="genericTableHeader">Location</td>
            <td class="genericTableData">
                <input type="checkbox" name="proc.location" size="35" />
            </td>
        </tr>
        <tr>
            <td class="genericTableHeader">Procedure Notes</td>
            <td class="genericTableData">
                <textarea rows="5" cols="40" name="procedureNote" id="procedureNote"></textarea>
            </td>
        </tr>

        <tr style="background-color:white">
            <td colspan="2">
                <br/>


                &nbsp;&nbsp;&nbsp;&nbsp;
                <input type="submit" value="Submit" />

                &nbsp;&nbsp;&nbsp;&nbsp;
                <input type="button" name="cancel" value="Cancel" onclick="window.close()"/>

            </td>
        </tr>
    </table>

</form>

</body>
</html>
