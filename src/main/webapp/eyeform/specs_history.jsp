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
<%@ taglib uri="/WEB-INF/security.tld" prefix="security" %>
<%
    String roleName$ = (String) session.getAttribute("userrole") + "," + (String) session.getAttribute("user");
    boolean authed = true;
%>
<security:oscarSec roleName="<%=roleName$%>" objectName="_eChart" rights="r" reverse="<%=true%>">
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
Specs History
<br/>

<form action="${pageContext.request.contextPath}/eyeform/SpecsHistory.do" method="post">
    <table border="0" cellspacing="5" width="100%">
        <tbody>
        <tr>
            <td>Date:</td>

            <td>
                <input type="text" name="specs.dateStr" size="10" id="pdate"/> <img
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
            <td>Doctor Name:</td>
            <td>
                <input type="text" name="specs.doctor" id="specs.doctor" />

            </td>

        </tr>

        <tr>
            <td>Specs Type:</td>
            <td>
                <select name="type">
                    <option value="distance">distance</option>
                    <option value="bifocal">bifocal</option>
                    <option value="invisible bifocal">invisible bifocal</option>
                    <option value="reading">reading</option>
                </select>
            </td>
        </tr>

        <tr>
            <td colspan="2" rowspan="3">
                <table border="1" width="80%">
                    <tbody>
                    <tr>
                        <td width="10%"></td>

                        <td width="18%">Sph</td>
                        <td width="18%">Cyl</td>
                        <td width="18%">Axis</td>
                        <td width="18%">Add</td>
                        <td width="18%">Prism</td>
                    </tr>

                    <tr>
                        <td width="10%">OD</td>
                        <td width="18%"><input type="checkbox" name="specs.odSph" size="8" /></td>
                        <td width="18%"><input type="checkbox" name="specs.odCyl" size="8" /></td>
                        <td width="18%"><input type="checkbox" name="specs.odAxis" size="8" /></td>
                        <td width="18%"><input type="checkbox" name="specs.odAdd" size="8" /></td>
                        <td width="18%"><input type="checkbox" name="specs.odPrism" size="8" /></td>
                    </tr>

                    <tr>
                        <td width="10%">OS</td>
                        <td width="18%"><input type="checkbox" name="specs.osSph" size="8" /></td>
                        <td width="18%"><input type="checkbox" name="specs.osCyl" size="8" /></td>
                        <td width="18%"><input type="checkbox" name="specs.osAxis" size="8" /></td>
                        <td width="18%"><input type="checkbox" name="specs.osAdd" size="8" /></td>
                        <td width="18%"><input type="checkbox" name="specs.osPrism" size="8" /></td>
                    </tr>

                    </tbody>
                </table>
            </td>
        </tr>


        </tbody>
    </table>
    <table width="80%">
        <tbody>
        <tr>
            <td colspan="2" align="right">
                <input type="submit" name="submit" value="save" onclick="this.form.method.value='save'; return validate(this);"/>
            </td>
        </tr>
        </tbody>
    </table>


    <input type="hidden" name="method" value="save"/>

    <input type="hidden" name="demographicNo" id="demographicNo"/>
    <input type="hidden" name="appointmentNo" id="appointmentNo"/>
    <input type="hidden" name="id" id="id"/>


</form>

</body>
</html>
