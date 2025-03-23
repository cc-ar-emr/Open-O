<%--

    Copyright (c) 2005, 2009 IBM Corporation and others.
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

    Contributors:
        <Quatro Group Software Systems inc.>  <OSCAR Team>

--%>


<%@ taglib uri="http://displaytag.sf.net" prefix="display" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html>
<head>
    <script type="text/javascript" src="<%= request.getContextPath() %>/js/global.js"></script>
    <title>Lookup</title>
    <c:set var="ctx" value="${pageContext.request.contextPath}" scope="request"/>
    <script type="text/javascript" src='<c:out value="${ctx}"/>/js/quatroReport.js'></script>
    <script type="text/javascript" src='<c:out value="${ctx}"/>/js/quatroLookup.js'></script>
    <style type="text/css">
        @import "${request.contextPath}/css/core.css";

        <
        style type

        =
        "text/css"
        >

        @import "${request.contextPath}/css/displaytag.css";
    </style>
    <style type="text/css">
        .clsAlignLeft {
            text-align: left
        }
    </style>
</head>
<body>
<form action="${pageContext.request.contextPath}/Lookup/LookupList.do" method="post">
    <input type="hidden" name="tableId" id="tableId"/>
    <input type="hidden" name="openerForm" id="openerForm"/>
    <input type="hidden" name="codeName" id="codeName"/>
    <input type="hidden" name="descName" id="descName"/>
    <input type="hidden" name="parentCode" id="parentCode"/>
    <table width="100%" border="0">
        <tr>
            <th class="pageTitle" align="center"><span
                    id="_ctl0_phBody_lblTitle" align="left">Lookup Tables &nbsp;-&nbsp;
			<c:out value="${lookupListForm.tableDef.description}"/>
			</span></th>
        </tr>
        <tr>
            <td width="80%">Description: <input type="text" name="keywordName" style="width:100%;" maxlength="80"/></td>
            <td width="20%"><input type="submit" name="submit" value="search"/></td>
        </tr>
        <tr>
            <td colspan="2">
                <display:table class="simple" style="width:100%;" cellspacing="0" cellpadding="0" id="lookup"
                               name="lookupListForm.lookups" export="false" pagesize="0"
                               requestURI="/PMmodule/Reports/QuatroReportList">
                    <display:setProperty name="paging.banner.placement" value="bottom"/>
                    <display:setProperty name="paging.banner.item_name" value="agency"/>
                    <display:setProperty name="paging.banner.items_name" value="facilities"/>
                    <display:setProperty name="basic.msg.empty_list" value="No records found."/>

                    <display:column sortable="false" title="Code" headerClass="clsAlignLeft">
                        <c:out value="${lookup.code}"/>
                    </display:column>
                    <display:column sortable="false" title="Description" headerClass="clsAlignLeft">
                        <a href='javascript:selectMe("<c:out value="${lookup.code}" />", "<c:out value="${lookup.descriptionJs}" />",
        "<c:out value="${lookupListForm.openerForm}" />", 
        "<c:out value="${lookupListForm.codeName}" />", 
        "<c:out value="${lookupListForm.descName}" />");'><c:out value="${lookup.description}"/> </a>
                    </display:column>

                </display:table>
            </td>
        </tr>
    </table>
</form>
</body>
</html>
