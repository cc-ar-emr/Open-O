<%--


    Copyright (c) 2005-2012. Centre for Research on Inner City Health, St. Michael's Hospital, Toronto. All Rights Reserved.
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

    This software was written for
    Centre for Research on Inner City Health, St. Michael's Hospital,
    Toronto, Ontario, Canada

--%>
<%@ include file="/taglibs.jsp" %>
<%@ taglib uri="/WEB-INF/caisi-tag.tld" prefix="caisi" %>
<%@page import="org.oscarehr.PMmodule.web.formbean.*" %>
<%@page import="org.oscarehr.PMmodule.web.utils.UserRoleUtils" %>
<%@page import="org.springframework.web.context.WebApplicationContext" %>
<%@page import="org.springframework.web.context.support.WebApplicationContextUtils" %>
<%@page import="org.oscarehr.PMmodule.service.ClientManager" %>
<%@page import="org.oscarehr.common.model.Demographic" %>
<%@page import="org.oscarehr.common.model.DemographicExt" %>

<form action="${pageContext.request.contextPath}/PMmodule/ClientManager.do" method="post">

    <%@ include file="/common/messages.jsp" %>

    <input type="hidden" name="method" value="override_restriction"/>

    <b style="color: red">The client currently has a service
        restriction in effect on the program you are trying to admit the client
        into.</b>

    <c:if test="${requestScope.hasOverridePermission}">
        <b>You have permission to override this restriction. To do so
            click on the "Override" below. Otherwise, click on "Cancel".</b>
    </c:if>

    <table cellpadding="3" cellspacing="0" border="0">
        <tr>
            <th title="Restriction details">Service restriction details</th>
        </tr>
    </table>
    <table width="100%" border="1" cellspacing="2" cellpadding="3">
        <tr class="b">
            <td width="20%">Client name:</td>
            <td><c:out value="${clientManagerForm.serviceRestriction.client.formattedName}"/></td>
        </tr>
        <tr class="b">
            <td width="20%">Restricted program:</td>
            <td><c:out value="${clientManagerForm.serviceRestriction.program.name}"/></td>
        </tr>
        <tr class="b">
            <td width="20%">Service restriction creator:</td>
            <td><c:out value="${clientManagerForm.serviceRestriction.provider.formattedName}"/></td>
        </tr>
        <tr class="b">
            <td width="20%">Comments:</td>
            <td><c:out value="${clientManagerForm.serviceRestriction.comments}"/></td>
        </tr>

        <tr class="b">
            <td width="20%">Start date:</td>
            <td><c:out value="${clientManagerForm.serviceRestriction.startDate}"/></td>
        </tr>

        <tr class="b">
            <td width="20%">End date:</td>
            <td><c:out value="${clientManagerForm.serviceRestriction.endDate}"/></td>
        </tr>

        <tr class="b">
            <td width="20%">Days remaining:</td>
            <td><c:out value="${clientManagerForm.serviceRestriction.daysRemaining}"/></td>
        </tr>

        <tr>
            <td colspan="2"><c:if
                    test="${requestScope.hasOverridePermission}">
                <input type="submit" name="submit" value="Override“/>
            </c:if> <button type="button" onclick="window.history.back();">Cancel</button>
        </td>
        </tr>

    </table>
</form>
