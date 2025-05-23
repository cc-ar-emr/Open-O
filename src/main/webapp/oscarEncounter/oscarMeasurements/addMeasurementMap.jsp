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

<%@ taglib uri="/WEB-INF/oscar-tag.tld" prefix="oscar" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>


<%@ page
        import="java.util.*, oscar.oscarEncounter.oscarMeasurements.data.MeasurementMapConfig, oscar.OscarProperties, oscar.util.StringUtils" %>
<%@ page import="org.oscarehr.common.model.MeasurementMap" %>

<%

    MeasurementMapConfig mmc = new MeasurementMapConfig();
%>

<link rel="stylesheet" type="text/css"
      href="<%= request.getContextPath() %>/oscarMDS/encounterStyles.css">

<html>
<head>
    <script type="text/javascript" src="<%= request.getContextPath() %>/js/global.js"></script>
    <title>Measurement Mapping Configuration</title>

    <script type="text/javascript" language=javascript>

        function popupStart(vheight, vwidth, varpage, windowname) {
            var page = varpage;
            windowprops = "height=" + vheight + ",width=" + vwidth + ",location=no,scrollbars=yes,menubars=no,toolbars=no,resizable=yes";
            var popup = window.open(varpage, windowname, windowprops);
        }

        function newWindow(varpage, windowname) {
            var page = varpage;
            windowprops = "fullscreen=yes,toolbar=yes,directories=no,resizable=yes,dependent=yes,scrollbars=yes,location=yes,status=yes,menubar=yes";
            var popup = window.open(varpage, windowname, windowprops);
        }

        function reloadPage() {
            document.CONFIG.action = 'addMeasurementMap.jsp';
            return true;
        }


        <%String outcome = request.getParameter("outcome");
        if (outcome != null){
            if (outcome.equals("success")){
                %>
        alert("Successfully updated the measurement mappings");
        <%
    }else if (outcome.equals("failedcheck")){
        %>
        alert("Unable to update measurement mappings: A measurement is already mapped to the specified code for that measurement type");
        <%
    }else{
        %>
        alert("Failed to update the measurement mappings");
        <%
    }
}%>

    </script>
    <link rel="stylesheet" type="text/css" media="all" href="<%= request.getContextPath() %>/share/css/extractedFromPages.css"/>
</head>

<body>
<form method="post" name="CONFIG" action="AddMeasurementMap.do">
    <table width="100%" height="100%" border="0">
        <tr class="MainTableTopRow">
            <td class="MainTableTopRow" colspan="9" align="left">
                <table width="100%">
                    <tr>
                        <td align="left"><input type="button"
                                                value=" <fmt:setBundle basename="oscarResources"/><fmt:message key="global.btnClose"/> "
                                                onClick="window.close()"></td>
                        <td align="right"><a
                                href="javascript:popupStart(300,400,'../About.jsp')"><fmt:setBundle basename="oscarResources"/><fmt:message key="global.about"/></a> | <a
                                href="javascript:popupStart(300,400,'../License.jsp')"><fmt:setBundle basename="oscarResources"/><fmt:message key="global.license"/></a></td>
                    </tr>
                </table>
            </td>
        </tr>
        <tr>
            <td valign="top">
                <center>
                    <table width="80%">
                        <tr>
                            <td colspan="2" valign="bottom" class="Header">Map Unmapped Identifier Codes</td>
                        </tr>
                        <tr>
                            <td class="Cell" width="20%">Select unmapped code:</td>
                            <td class="Cell" width="80%"><select name="identifier">
                                <option value="0">None Selected</option>
                                <%
                                    String identifier = request.getParameter("identifier");
                                    if (identifier == null) identifier = "";
                                    ArrayList measurements = mmc.getUnmappedMeasurements("");
                                    for (int i = 0; i < measurements.size(); i++) {
                                        HashMap ht = (HashMap) measurements.get(i);
                                        String value = (String) ht.get("identifier") + "," + (String) ht.get("type") + "," + (String) ht.get("name");
                                %>
                                <option value="<%= value %>"
                                        <%= value.equals(identifier) ? "selected" : "" %>><%= "(" + (String) ht.get("type") + ") " + (String) ht.get("identifier") + " - " + ((String) ht.get("name")).trim() %>
                                </option>
                                <% }%>
                            </select></td>
                        </tr>
                        <tr>
                            <td class="Cell" width="20%">Search codes by name:</td>
                            <td class="Cell" width="80%">
                                <%
                                    String searchstring = request.getParameter("searchstring");
                                    if (searchstring == null)
                                        searchstring = "";
                                %>
                                <input type="text" size="30" name="searchstring"
                                       value="<%= searchstring %>"/> <input type="submit" value="Search"
                                                                            onclick="return reloadPage()"/></td>
                        <tr>
                            <td class="Cell" width="20%">Select code to map to:</td>
                            <td class="Cell" width="80%"><select name="loinc_code">
                                <option value="0">None Selected</option>
                                <% List<MeasurementMap> loincCodes = mmc.getLoincCodes(searchstring);
                                    for (MeasurementMap loincCode : loincCodes) {%>
                                <option value="<%= loincCode.getLoincCode() %>"><%= loincCode.getLoincCode() + " - " + loincCode.getName().trim()%>
                                </option>
                                <% }%>
                            </select></td>
                        </tr>
                        <tr>
                            <td colspan="2" class="Cell" align="center"><input
                                    type="submit" value=" Update Measurement Mapping "> <input
                                    type="button" value=" Add New Loinc Code "
                                    onclick="javascript:popupStart('300','600','newMeasurementMap.jsp','Add_New_Loinc_Code')">
                            </td>
                        </tr>
                        <tr>
                            <td colspan="2" class="Cell" align="center">NOTE: It
                                is suggested that you refer to approrpiate referece material to determine
                                correct loinc codes.
                                <span class="HelpAboutLogout">
				</span>
                            </td>
                        </tr>
                    </table>
                </center>
            </td>
        </tr>
    </table>
</form>
</body>
</html>