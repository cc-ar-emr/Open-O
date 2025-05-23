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

<%@page contentType="text/html" %>
<%@ include file="/casemgmt/taglibs.jsp" %>
<%@page import="java.util.*" %>
<%@ page import="java.util.ResourceBundle"%>
<%
    if (session.getValue("user") == null)
        response.sendRedirect("../logout.htm");

    ResourceBundle bundle = ResourceBundle.getBundle("oscarResources", request.getLocale());

    String providertitle = (String) request.getAttribute("providertitle");
    String providermsgPrefs = (String) request.getAttribute("providermsgPrefs");
    String providermsgProvider = (String) request.getAttribute("providermsgProvider");
    String providermsgEdit = (String) request.getAttribute("providermsgEdit");
    String providermsgSuccess = (String) request.getAttribute("providermsgSuccess");
%>

<!DOCTYPE html>
<c:set var="ctx" value="${pageContext.request.contextPath}" scope="request"/>
<html>
    <head>
        <base href="<%= request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/" %>">
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title><%=bundle.getString(providertitle)%></title>
        <script src="<c:out value="${ctx}"/>/share/javascript/provider_form_validations.js"></script>
        <script src="<c:out value="${ctx}"/>/js/global.js"></script>
        <link href="<c:out value="${ctx}"/>/css/bootstrap.css" rel="stylesheet"> <!-- Bootstrap 2.3.1 -->

    </head>

    <body class="BodyStyle">

    <table class="MainTable" id="scrollNumber1" name="encounterTable">
        <tr class="MainTableTopRow">
            <td class="MainTableTopRowLeftColumn">
                <h4><%=bundle.getString(providermsgPrefs)%></h4>
            </td>
            <td class="MainTableTopRowRightColumn">
                <h4>&nbsp;&nbsp;<%=bundle.getString(providermsgProvider)%></h4>
            </td>
        </tr>
        <tr>
            <td class="MainTableLeftColumn">&nbsp;</td>
            <td class="MainTableRightColumn">
                <%if (request.getAttribute("status") == null) {%>
                <%=bundle.getString(providermsgEdit)%>

                <form styleId="providerForm" action="${pageContext.request.contextPath}/setProviderStaleDate.do" method="post">
                    <input type="hidden" name="method" value="<c:out value="${method}"/>">
                    <p id="errorMessage" class="alert alert-danger" style="display: none; color: red;">
                        Invalid input.
                    </p>
                    <fmt:setBundle basename="oscarResources"/><fmt:message key="provider.patientNameLength.title"/>
                    <input type="text" id="numericFormField" name="patientNameLength.value"/>
                    <br/>
                    <input type="submit" name="submit" value="Apple" class="btn btn-primary" />
                </form>

                <%} else {%>
                <div class="alert alert-success" style="width:100%"><%=bundle.getString(providermsgSuccess)%></div>
                <br>
                <%}%>
            </td>
        </tr>
        <tr>
            <td class="MainTableBottomRowLeftColumn"></td>
            <td class="MainTableBottomRowRightColumn"></td>
        </tr>
    </table>
    </body>
</html>