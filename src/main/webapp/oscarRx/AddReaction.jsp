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
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ page import="org.oscarehr.util.LoggedInInfo" %>
<%@ taglib uri="/WEB-INF/security.tld" prefix="security" %>
<%
    String roleName2$ = (String) session.getAttribute("userrole") + "," + (String) session.getAttribute("user");
    boolean authed = true;
%>
<security:oscarSec roleName="<%=roleName2$%>" objectName="_allergy" rights="r" reverse="<%=true%>">
    <%authed = false; %>
    <%response.sendRedirect("../securityError.jsp?type=_allergy");%>
</security:oscarSec>
<%
    if (!authed) {
        return;
    }
%>

<html>
    <head>
        <script type="text/javascript" src="<%= request.getContextPath() %>/js/global.js"></script>
        <title><fmt:setBundle basename="oscarResources"/><fmt:message key="AddReaction.title"/></title>
        <base href="<%= request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/" %>">

        <c:if test="${empty RxSessionBean}">
            <c:redirect url="error.html"/>
        </c:if>
        <c:if test="${not empty RxSessionBean}">
            <c:set var="bean" value="${RxSessionBean}" scope="session"/>
            <c:if test="${bean.valid == false}">
                <c:redirect url="error.html"/>
            </c:if>
        </c:if>

        <%
            oscar.oscarRx.pageUtil.RxSessionBean bean = (oscar.oscarRx.pageUtil.RxSessionBean) pageContext.findAttribute("bean");
            String name = (String) request.getAttribute("name");
            String type = (String) request.getAttribute("type");
            String allergyId = (String) request.getAttribute("allergyId");
        %>

        <link rel="stylesheet" type="text/css" href="oscarRx/styles.css">
    </head>
    <body topmargin="0" leftmargin="0" vlink="#0000FF">

    <table border="0" cellpadding="0" cellspacing="0"
           style="border-collapse: collapse" bordercolor="#111111" width="100%"
           id="AutoNumber1" height="100%">
        <%@ include file="TopLinks.jsp"%><!-- Row One included here-->
        <tr>
            <%@ include file="SideLinksNoEditFavorites.jsp"%><!-- <td></td>Side Bar File --->
            <td width="100%" style="border-left: 2px solid #A9A9A9;" height="100%"
                valign="top">
                <table cellpadding="0" cellspacing="2"
                       style="border-collapse: collapse" bordercolor="#111111" width="100%"
                       height="100%">
                    <tr>
                        <td width="0%" valign="top">
                            <div class="DivCCBreadCrumbs"><a href="SearchDrug.jsp"> <fmt:setBundle basename="oscarResources"/><fmt:message key="SearchDrug.title"/></a>&nbsp;&gt;&nbsp; <a
                                    href="ShowAllergies.jsp"> <fmt:setBundle basename="oscarResources"/><fmt:message key="EditAllergies.title"/></a>&nbsp;&gt;&nbsp; <b><fmt:setBundle basename="oscarResources"/><fmt:message key="AddReaction.title"/></b></div>
                        </td>
                    </tr>
                    <!----Start new rows here-->
                    <tr>
                        <td>
                            <div class="DivContentSectionHead"><%=name%>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td><form action="oscarRx/addAllergy.do" focus="reactionDescription" name="RxAddAllergyForm" id="RxAddAllergyForm">
                            <table>
                                <tr valign="center">

                                    <td colspan="2">
                                        <textarea name="reactionDescription" cols="40" rows="3"></textarea>
                                        <input type="hidden" name="ID" id="ID" value="<%=allergyId%>"/>
                                        <input type="hidden" name="name" id="name" value="<%=name%>"/>
                                        <input type="hidden" name="type" id="type" value="<%=type%>"/></td>
                                </tr>

                                <tr valign="center">
                                    <td colspan="2">Start Date: <input type="text"
                                            name="startDate" size="10" maxlength="10"/>
                                        (yyyy-mm-dd OR yyyy-mm OR yyyy)
                                    </td>

                                </tr>

                                <tr valign="center">
                                    <td colspan="2">Age Of Onset: <input type="text"
                                            name="ageOfOnset" size="4" maxlength="4"/></td>

                                </tr>

                                <tr valign="center">

                                    <td colspan="2">Severity Of Reaction : <select
                                            name="severityOfReaction">
                                        <option value="1">Mild</option>
                                        <option value="2">Moderate</option>
                                        <option value="3">Severe</option>
                                        <option value="4">Unknown</option>
                                    </select></td>

                                </tr>

                                <tr valign="center">

                                    <td colspan="2">Onset Of Reaction: <select
                                            name="onSetOfReaction">
                                        <option value="1">Immediate</option>
                                        <option value="2">Gradual</option>
                                        <option value="3">Slow</option>
                                        <option value="4">Unknown</option>
                                    </select></td>

                                </tr>


                                <tr>
                                    <td colspan="2">
                                        <input type="submit" name="submit" value="Add Allergy" class="ControlPushButton"/>
                                        <input
                                                type=button class="ControlPushButton"
                                                onclick="javascript:document.forms.RxAddAllergyForm.reactionDescription.value='';document.forms.RxAddAllergyForm.startDate.value='';document.forms.RxAddAllergyForm.ageOfOnset.value='';document.forms.RxAddAllergyForm.reactionDescription.focus();"
                                                value="Reset"/></td>
                                </tr>
                            </table>
                            &nbsp;

                        </form></td>
                    </tr>

                    <tr>
                        <td>
                            <%
                                String sBack = "ShowAllergies.jsp";
                            %> <input type=button class="ControlPushButton"
                                      onclick="javascript:window.location.href='oscarRx/<%=sBack%>';"
                                      value="Back to View Allergies"/></td>
                    </tr>
                    <!----End new rows here-->
                    <tr height="100%">
                        <td></td>
                    </tr>
                </table>
            </td>
        </tr>
        <tr>
            <td height="0%"
                style="border-bottom: 2px solid #A9A9A9; border-top: 2px solid #A9A9A9;"></td>
            <td height="0%"
                style="border-bottom: 2px solid #A9A9A9; border-top: 2px solid #A9A9A9;"></td>
        </tr>
        <tr>
            <td width="100%" height="0%" colspan="2">&nbsp;</td>
        </tr>
        <tr>
            <td width="100%" height="0%" style="padding: 5" bgcolor="#DCDCDC"
                colspan="2"></td>
        </tr>
    </table>

    </body>

</html>
