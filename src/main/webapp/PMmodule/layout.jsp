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
<c:set var="ctx" value="${pageContext.request.contextPath}"
       scope="request"/>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
    <head>
        <script type="text/javascript" src="<%= request.getContextPath() %>/js/global.js"></script>
        <title>Program Management Module</title>
        <link rel="stylesheet" type="text/css"
              href='${request.contextPath}/css/tigris.css'/>
        <link rel="stylesheet" type="text/css"
              href='${request.contextPath}/css/displaytag.css'/>

        <link rel="stylesheet" type="text/css"
              href='${request.contextPath}/share/calendar/skins/aqua/theme.css'/>

        <link rel="stylesheet" type="text/css"
              href='${request.contextPath}/css/topnav.css'/>

        <!--   style type="text/css">
			@import "${request.contextPath}/css/tigris.css";
			@import "${request.contextPath}/css/displaytag.css";
			@import "${request.contextPath}/jsCalendar/skins/aqua/theme.css";
		</style -->
        <script type="text/javascript"
                src="${request.contextPath}/share/calendar/calendar.js"/>
        </script>
        <script type="text/javascript"
                src="${request.contextPath}/share/calendar/lang/calendar-en.js"></script>
        <script type="text/javascript"
                src="${request.contextPath}/share/calendar/calendar-setup.js"></script>

        <script type="text/javascript">
            var djConfig = {
                isDebug: false,
                parseWidgets: false,
                searchIds: ["addPopupTimePicker"]
            };
        </script>

        <script type="text/javascript"
                src="${request.contextPath}/dojoAjax/dojo.js">
        </script>

        <script type="text/javascript" language="JavaScript">
            dojo.require("dojo.date.format");
            dojo.require("dojo.widget.*");
            dojo.require("dojo.validate.*");
        </script>

        <script type="text/javascript"
                src="${request.contextPath}/js/genericIntake.js"></script>

        <script type="text/javascript" src="${request.contextPath}/js/jquery.js"></script>
        <script type="text/javascript" src="${request.contextPath}/js/topnav.js"></script>

        <base href="<%= request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/" %>">
    </head>
    <body style="margin-top:0;margin-bottom:0;margin-right:0;margin-left:0;">

    <jsp:include page="topnav.jsp" />

    <div style="height:20px"></div>

    <div class="composite">
        <table border="0" cellspacing="0" cellpadding="0" width="100%">
            <tr valign="top">
                <td id="leftcol" width="20%">
                    <jsp:include page="/PMmodule/navigation_blank.jsp"/>
                </td>
                <td width="80%">
                    <div class="body">
                            <jsp:include page="/PMmodule/Home.jsp"/>
                </td>
            </tr>
        </table>
    </div>
    </body>
</html>
