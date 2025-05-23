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
<security:oscarSec roleName="<%=roleName$%>" objectName="_admin,_admin.consult" rights="r" reverse="<%=true%>">
    <%authed = false; %>
    <%response.sendRedirect("../../../securityError.jsp?type=_admin&type=_admin.consult");%>
</security:oscarSec>
<%
    if (!authed) {
        return;
    }
%>

<%@ page import="java.util.ResourceBundle" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>


<!DOCTYPE html>
<html>

    <jsp:useBean id="showAllServicesUtil" scope="session"
                 class="oscar.oscarEncounter.oscarConsultationRequest.config.pageUtil.EctConShowAllServicesUtil"/>


    <head>
        <script type="text/javascript" src="<%= request.getContextPath() %>/js/global.js"></script>
        <title><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.oscarConsultationRequest.config.ShowAllServices.title"/>
        </title>
        <base href="<%= request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/" %>">
        <script>
            function BackToOscar() {
                window.close();
            }
        </script>
        <link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/css/encounterStyles.css">
    </head>

    <body class="BodyStyle" vlink="#0000FF">
    <% 
    java.util.List<String> actionErrors = (java.util.List<String>) request.getAttribute("actionErrors");
    if (actionErrors != null && !actionErrors.isEmpty()) {
%>
    <div class="action-errors">
        <ul>
            <% for (String error : actionErrors) { %>
                <li><%= error %></li>
            <% } %>
        </ul>
    </div>
<% } %>
    <div id="service-providers-wrapper" style="margin:auto 10px;">
        <table class="MainTable" id="scrollNumber1" name="encounterTable">
            <tr class="MainTableTopRow">
                <td class="MainTableTopRowLeftColumn">Consultation</td>
                <td class="MainTableTopRowRightColumn">
                    <table class="TopStatusBar">
                        <tr>
                            <td class="Header"><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.oscarConsultationRequest.config.ShowAllServices.title"/>
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>
            <tr style="vertical-align: top">
                <td class="MainTableLeftColumn">
                    <%
                        oscar.oscarEncounter.oscarConsultationRequest.config.pageUtil.EctConTitlebar titlebar = new oscar.oscarEncounter.oscarConsultationRequest.config.pageUtil.EctConTitlebar(request);
                        out.print(titlebar.estBar(request));
                    %>
                </td>
                <td class="MainTableRightColumn">
                    <table cellpadding="0" cellspacing="2"
                           style="border-collapse: collapse" bordercolor="#111111" width="100%">

                        <!----Start new rows here-->
                        <tr>
                            <td>

                                <table>
                                    <form action="${pageContext.request.contextPath}/oscarEncounter/AddService.do" method="post">
                                        <tr>
                                            <td><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.oscarConsultationRequest.config.ShowAllServices.services"/>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td>

                                                <table>
                                                    <%
                                                        //out.print("<a href=\"/ShowAllServices.do?serviceId="+id+"\">"+desc+"</a>");
                                                        showAllServicesUtil.estServicesVectors();
                                                        for (int i = 0; i < showAllServicesUtil.serviceIdVec.size(); i++) {
                                                            String id = (String) showAllServicesUtil.serviceIdVec.elementAt(i);
                                                            String desc = (String) showAllServicesUtil.serviceDescVec.elementAt(i);
                                                    %>
                                                    <tr>
                                                        <td>
                                                            <%
                                                                String contextPath = request.getContextPath();
                                                                String url = contextPath + "/oscarEncounter/ShowAllServices.do?serviceId=" + id + "&serviceDesc=" + desc;
                                                                out.print("<a href=\"" + url + "\">" + desc + "</a>");
                                                            %>
                                                        </td>
                                                    </tr>
                                                    <%}%>
                                                </table>
                                            </td>
                                        </tr>
                                    </form>
                                </table>
                            </td>
                        </tr>

                    </table>
                </td>
            </tr>
            <tr>
                <td class="MainTableBottomRowLeftColumn"></td>
                <td class="MainTableBottomRowRightColumn"></td>
            </tr>
        </table>
    </div>
    </body>
</html>
