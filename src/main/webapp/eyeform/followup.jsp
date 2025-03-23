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

<%@page import="org.oscarehr.eyeform.model.EyeformSpecsHistory" %>


<%@ include file="/taglibs.jsp" %>


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
Follow Up/Consult
<br/><br/><br/>

<form action="${pageContext.request.contextPath}/eyeform/FollowUp.do" method="post">


    <input type="hidden" name="method" value="save"/>

    <input type="hidden" name="demographicNo" id="demographicNo"/>

    <input type="hidden" name="appointmentNo" id="appointmentNo"/>


    <table border="0">
        <tbody>
        <tr>
            <td width="25%">
                <select name="followup.type">
                    <option value="followup">Follow up</option>
                    <option value="consult">Consult</option>
                </select>
            </td>
            <td width="25%">
                Doctor:
                <select name="followupProvider">
                    <c:forEach var="item" items="${providers}">
                        <option value="<c:out value="${item.providerNo}"/>"><c:out
                                value="${item.formattedName}"/></option>
                    </c:forEach>
                </select>
            </td>
        </tr>
        <tr>
            <td colspan="2">


                <input type="text" name="followup.timespan" size="4" id="width: 25px;" class="special"/>

                <select property="followup.timeframe" styleId="width: 50px;" styleClass="special">
                    <option value="days">days</option>
                    <option value="weeks">weeks</option>
                    <option value="months">months</option>
                </select>
            </td>
        </tr>
        <tr>
            <td>
                <select name="urgency" styleId="width: 50px;" style="special">
                    <option value="routine">routine</option>
                    <option value="asap">ASAP</option>
                </select>
            </td>
        </tr>
        <tr>
            <td>
                Comment:<textarea rows="5" cols="40" name="comment" id="comment"></textarea>
            </td>
        </tr>
        <tr>
            <td><br/><input type="submit" value="submit" /></td>
        </tr>
        </tbody>
    </table>


</form>

</body>
</html>
