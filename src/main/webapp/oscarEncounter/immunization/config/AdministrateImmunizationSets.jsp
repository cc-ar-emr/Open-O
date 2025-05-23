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

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>


<%@ taglib uri="/WEB-INF/security.tld" prefix="security" %>
<%
    String roleName$ = (String) session.getAttribute("userrole") + "," + (String) session.getAttribute("user");
    boolean authed = true;
%>
<security:oscarSec roleName="<%=roleName$%>" objectName="_eChart" rights="r" reverse="<%=true%>">
    <%authed = false; %>
    <%response.sendRedirect("../../../securityError.jsp?type=_eChart");%>
</security:oscarSec>
<%
    if (!authed) {
        return;
    }
%>

<%
    int stat = 0;
    stat = request.getParameter("stat") != null ? Integer.parseInt(request.getParameter("stat")) : stat;
    stat = request.getAttribute("stat") != null ? Integer.parseInt((String) request.getAttribute("stat")) : stat;
    boolean deletedList = stat == 0 ? false : true;

    oscar.oscarEncounter.immunization.config.data.EctImmImmunizationSetData immuSets = new oscar.oscarEncounter.immunization.config.data.EctImmImmunizationSetData();
    immuSets.estImmunizationVecs(stat);

%>

<html>


    <head>
        <script type="text/javascript" src="<%= request.getContextPath() %>/js/global.js"></script>
        <title><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.immunization.config.administrativeImmunizationSets.title"/>
        </title>
        <base href="<%= request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/" %>">

        <link rel="stylesheet" type="text/css" media="all" href="<%= request.getContextPath() %>/share/css/extractedFromPages.css"/>
    </head>
    <script language="javascript">
        function BackToOscar() {
            window.close();
        }

        function goURL(s) {
            self.location.href = s;
        }

        function popupImmunizationSet(vheight, vwidth, varpage) { //open a new popup window
            var page = varpage;
            windowprops = "height=" + vheight + ",width=" + vwidth + ",location=no,scrollbars=yes,menubars=no,toolbars=no,resizable=yes,screenX=0,screenY=0,top=0,left=0";
            var popup = window.open(varpage, "immSet", windowprops);
            if (popup != null) {
                if (popup.opener == null) {
                    popup.opener = self;
                }
            }

        }
    </script>
    <link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/css/encounterStyles.css">


    <body class="BodyStyle" vlink="#0000FF" onload="window.focus();">
    <!--  -->
    <table class="MainTable" id="scrollNumber1" name="encounterTable">
        <tr class="MainTableTopRow">
            <td class="MainTableTopRowLeftColumn"><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.immunization.config.administrativeImmunizationSets.msgImm"/>
            </td>
            <td class="MainTableTopRowRightColumn">
                <table class="TopStatusBar">
                    <tr>
                        <td></td>
                        <td></td>
                        <td style="text-align: right"><a
                                href="javascript:history.go(-1);"><fmt:setBundle basename="oscarResources"/><fmt:message key="global.btnBack"/></a> | <a href="javascript:window.close();"><fmt:setBundle basename="oscarResources"/><fmt:message key="global.btnClose"/></a></td>
                    </tr>
                </table>
            </td>
        </tr>
        <tr>
            <td class="MainTableLeftColumn"></td>
            <td class="MainTableRightColumn"><form action="${pageContext.request.contextPath}/oscarEncounter/immunization/config/deleteImmunizationSet.do" method="post">
                <table width="50%" border=0 cellspacing=1>
                    <tr>
                        <th>&nbsp;</th>
                        <th><font color="red"><%=deletedList ? "(Deleted)" : ""%>
                        </font> <fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.immunization.config.administrativeImmunizationSets.msgImmName"/>
                        </th>
                        <th><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.immunization.config.administrativeImmunizationSets.msgDateCreated"/>
                        </th>
                    </tr>
                    <%
                        for (int i = 0; i < immuSets.setNameVec.size(); i++) {
                            String name = (String) immuSets.setNameVec.elementAt(i);
                            String id = (String) immuSets.setIdVec.elementAt(i);
                            String createDate = (String) immuSets.createDateVec.elementAt(i);
                    %>
                    <tr bgcolor="<%= i%2==0? "#EEEEFF" : "#CCCCFF"%>">
                        <td width="3%"><input type="checkbox" name="chkSetId"
                                              value="<%=id%>"/></td>
                        <td width="70%"><a
                                href="javascript:popupImmunizationSet(768,1024,'oscarEncounter/immunization/config/ImmunizationSetDisplay.do?setId=<%=id%>')">
                            <%=name%>
                        </a></td>
                        <td align="center"><%=createDate%>
                        </td>
                    </tr>
                    <%}%>
                </table>
                <br/>
                <table width="50%" border=0 cellspacing=1>
                    <tr>
                        <td>
                            <% if (deletedList == true) { %> <input type="submit" name="action"
                                                                    value="<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.immunization.config.administrativeImmunizationSets.btnRestore"/>">
                            <% } else { %> <input type="submit" name="action"
                                                  value="<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.immunization.config.administrativeImmunizationSets.btnDelete"/>">
                            <% } %>
                        </td>
                        <td align="right"><input type="button" name="Button"
                                                 value="<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.immunization.config.administrativeImmunizationSets.btnAddNew"/>"
                                                 onClick="javascript:goURL('oscarEncounter/immunization/config/CreateImmunizationSetInit.jsp');">
                            <% if (deletedList == true) { %> <input type="button" name="action"
                                                                    value="<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.immunization.config.administrativeImmunizationSets.btnSetlist"/>"
                                                                    onClick="goURL('oscarEncounter/immunization/config/AdministrateImmunizationSets.jsp');"> <% } else { %>
                            <input type="button" name="action"
                                   value="<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.immunization.config.administrativeImmunizationSets.btnDelList"/>"
                                   onClick="goURL('oscarEncounter/immunization/config/AdministrateImmunizationSets.jsp?stat=2');">
                            <% } %>
                        </td>
                    </tr>
                </table>
            </form></td>
        </tr>
        <tr>
            <td class="MainTableBottomRowLeftColumn"></td>
            <td class="MainTableBottomRowRightColumn"></td>
        </tr>
    </table>


    </body>
</html>
