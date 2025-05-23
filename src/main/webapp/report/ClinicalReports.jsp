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


<%@page import="org.apache.commons.lang3.StringUtils" %>
<%@ taglib uri="/WEB-INF/security.tld" prefix="security" %>
<%
    String roleName$ = (String) session.getAttribute("userrole") + "," + (String) session.getAttribute("user");
    boolean authed = true;
%>
<security:oscarSec roleName="<%=roleName$%>" objectName="_report,_admin.reporting" rights="r" reverse="<%=true%>">
    <%authed = false; %>
    <%response.sendRedirect("../securityError.jsp?type=_report&type=_admin.reporting");%>
</security:oscarSec>
<%
    if (!authed) {
        return;
    }
%>

<%@page import="org.oscarehr.util.LoggedInInfo" %>
<%@page import="oscar.oscarReport.data.DemographicSets, oscar.oscarDemographic.data.*,java.util.*,oscar.oscarPrevention.*,oscar.oscarProvider.data.*,oscar.util.*,oscar.oscarReport.ClinicalReports.*,oscar.oscarEncounter.oscarMeasurements.*,oscar.oscarEncounter.oscarMeasurements.bean.*" %>
<%@page import="com.Ostermiller.util.CSVPrinter,java.io.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="/WEB-INF/oscar-tag.tld" prefix="oscar" %>

<%
    String provider = (String) session.getValue("user");

    String numeratorId = (String) request.getAttribute("numeratorId");
    String denominatorId = (String) request.getAttribute("denominatorId");

    ClinicalReportManager reports = ClinicalReportManager.getInstance();

    List<Denominator> denominatorList = reports.getDenominatorList();
    List<Numerator> numeratorList = reports.getNumeratorList();

    Hashtable rep = new Hashtable();
    Hashtable repNum = new Hashtable();


    Integer max_numerator = (Integer) request.getAttribute("max_numerator");

    Hashtable arrRepNum[] = new Hashtable[11];
    for (int x = 0; x < 11; x++) {
        arrRepNum[x] = new Hashtable();
    }
    Object numer_val[] = new Object[11];
    Object numer_startDate[] = new Object[11];
    Object numer_endDate[] = new Object[11];

    String arrNumeratorId[] = new String[11];
    for (int x = 0; x < 11; x++) {
        arrNumeratorId[x] = (String) request.getAttribute("numerator" + x + "Id");
    }


    if (request.getParameter("clear") != null && request.getParameter("clear").equals("yes")) {
        session.removeAttribute("ClinicalReports");
    }

    EctMeasurementTypesBeanHandler hd = new EctMeasurementTypesBeanHandler();
    Vector<EctMeasurementTypesBean> vec = hd.getMeasurementTypeVector();

    HashMap measurementTitles = new HashMap();
    for (EctMeasurementTypesBean measurementTypes : vec) {
        measurementTitles.put(measurementTypes.getType(), measurementTypes.getTypeDisplayName());
    }


    DemographicSets demoSets = new DemographicSets();
    List<String> demoSetList = demoSets.getDemographicSets();

    List<Map<String, String>> providers = ProviderData.getProviderList();

    String[] headings = (String[]) request.getAttribute("showfields");
%>


<html>

    <head>
        <title><fmt:setBundle basename="oscarResources"/><fmt:message key="report.ClinicalReports.title"/></title>
        <base href="<%= request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/" %>">
        <link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/share/css/OscarStandardLayout.css">
        <link rel="stylesheet" type="text/css" media="all" href="<%= request.getContextPath() %>/share/calendar/calendar.css" title="win2k-cold-1"/>

        <script type="text/javascript" src="<%= request.getContextPath() %>/share/calendar/calendar.js"></script>
        <script type="text/javascript"
                src="<%= request.getContextPath() %>/share/calendar/lang/<fmt:setBundle basename="oscarResources"/><fmt:message key="global.javascript.calendar"/>"></script>
        <script type="text/javascript" src="<%= request.getContextPath() %>/share/calendar/calendar-setup.js"></script>

        <script type="text/javascript" src="<%=request.getContextPath() %>/js/jquery-1.12.3.js"></script>


        <style type="text/css">
            table.outline {
                margin-top: 50px;
                border-bottom: 1pt solid #888888;
                border-left: 1pt solid #888888;
                border-top: 1pt solid #888888;
                border-right: 1pt solid #888888;
            }

            table.grid {
                border-bottom: 1pt solid #888888;
                border-left: 1pt solid #888888;
                border-top: 1pt solid #888888;
                border-right: 1pt solid #888888;
            }

            td.gridTitles {
                border-bottom: 2pt solid #888888;
                font-weight: bold;
                text-align: center;
            }

            td.gridTitlesWOBottom {
                font-weight: bold;
                text-align: center;
            }

            td.middleGrid {
                border-left: 1pt solid #888888;
                border-right: 1pt solid #888888;
                text-align: center;
            }
        </style>

        <style type="text/css">

            table.results {
                margin-top: 3px;
                margin-left: 3px;
                /*border-bottom: 1pt solid #888888;
                border-left: 1pt solid #888888;
                border-top: 1pt solid #888888;
                border-right: 1pt solid #888888;*/
                border: 1pt solid #888888;
                border-collapse: collapse;
            }

            table.results th {
                border: 1px solid grey;
                padding: 2px;
                text-decoration: none;
            }

            table.results td {
                border: 1px solid lightgrey;
                padding-left: 2px;
                padding-right: 2px;
            }

            tr.red td {
                background-color: red;
                padding-left: 2px;
                padding-right: 2px;
            }
        </style>


        <script type="text/javascript">
            var denominator_fields = new Array();
            var denom_xtras;
            denominator_fields[denominator_fields.length] = "denominator_provider_no";
            denominator_fields[1] = "denominator_patientSet";


            function processExtraFields(t) {
                var currentDenom = t.options[t.selectedIndex].value;
                //console.log(currentDenom);
                //Hide all extra denom fields
                for (i = 0; i < denominator_fields.length; i++) {
                    document.getElementById(denominator_fields[i]).style.display = 'none';
                }
                try {
                    var fields_to_turn_on = denom_xtras[currentDenom];
                    //console.log("fields to turn on " + fields_to_turn_on[0]);
                    //get list of extra
                    for (i = 0; i < fields_to_turn_on.length; i++) {
                        //console.log(i+" "+document.getElementById(fields_to_turn_on[i]).style.display);
                        document.getElementById(fields_to_turn_on[i]).style.display = '';
                        //console.log(i+" "+document.getElementById(fields_to_turn_on[i]).style.display);
                    }
                } catch (e) {

                }

                //console.log("going out");
            }


            var numerator_fields = new Array();
            var numer_xtras;
            numerator_fields[numerator_fields.length] = "numerator_measurements";
            numerator_fields[1] = "numerator_value";
            numerator_fields[2] = "numerator_startDate";
            numerator_fields[3] = "numerator_endDate";


            function processExtraFieldsNumerator(t) {
                var currentDenom = t.options[t.selectedIndex].value;
                //console.log(currentDenom);
                //Hide all extra denom fields
                for (i = 0; i < numerator_fields.length; i++) {
                    document.getElementById(numerator_fields[i]).style.display = 'none';
                }
                try {
                    var fields_to_turn_on = numer_xtras[currentDenom];
                    //console.log("fields to turn on " + fields_to_turn_on[0]);
                    //get list of extra
                    for (i = 0; i < fields_to_turn_on.length; i++) {
                        //console.log(i+" "+document.getElementById(fields_to_turn_on[i]).style.display);
                        document.getElementById(fields_to_turn_on[i]).style.display = '';
                        //console.log(i+" "+document.getElementById(fields_to_turn_on[i]).style.display);
                    }
                } catch (e) {

                }

                //console.log("going out");
            }


            <%
            for(int x=0;x<11;x++) {
            %>
            var numerator<%=x%>_fields = new Array();
            var numer<%=x%>_xtras;
            numerator<%=x%>_fields[numerator<%=x%>_fields.length] = "numerator<%=x%>_measurements";
            numerator<%=x%>_fields[1] = "numerator<%=x%>_value";
            numerator<%=x%>_fields[2] = "numerator<%=x%>_startDate";
            numerator<%=x%>_fields[3] = "numerator<%=x%>_endDate";


            function processExtraFieldsNumerator<%=x%>(t) {
                var currentDenom = t.options[t.selectedIndex].value;

                for (i = 0; i < numerator<%=x%>_fields.length; i++) {
                    document.getElementById(numerator<%=x%>_fields[i]).style.display = 'none';
                }

                try {
                    var fields_to_turn_on = numer<%=x%>_xtras[currentDenom];

                    for (i = 0; i < fields_to_turn_on.length; i++) {
                        document.getElementById(fields_to_turn_on[i]).style.display = '';
                    }
                } catch (e) {

                }
            }

            <% } %>


            var num_numerators = 0;

            function showNextNumerator() {
                if (num_numerators <= 11) {
                    $("#fieldNumerator" + num_numerators).show();
                    num_numerators++;
                    if (num_numerators > 11) {
                        $("#addNumeratorBtn").hide();
                    }
                }
            }

            <%if(max_numerator != null) {%>
            $(document).ready(function () {
                var maxNumerator = <%=max_numerator%>;
                if (maxNumerator != -1) {
                    for (var x = 0; x <= maxNumerator; x++) {
                        showNextNumerator();
                    }
                }
            });
            <%}%>
        </script>

    </head>

    <body class="BodyStyle" vlink="#0000FF">
    <!--  -->
    <table class="MainTable" id="scrollNumber1" name="encounterTable">
        <tr class="MainTableTopRow">
            <td class="MainTableTopRowLeftColumn" width="100">
                <fmt:setBundle basename="oscarResources"/><fmt:message key="report.ClinicalReports.title"/>
            </td>
            <td class="MainTableTopRowRightColumn">
                <table class="TopStatusBar">
                    <tr>
                        <td>
                            <%=  request.getAttribute("name") != null ? request.getAttribute("name") : ""%>
                        </td>
                        <td>&nbsp;

                        </td>
                        <td style="text-align:right">
                            <a
                                href="javascript:popupStart(300,400,'About.jsp')"><fmt:setBundle basename="oscarResources"/><fmt:message key="global.about"/></a>
                            | <a href="javascript:popupStart(300,400,'License.jsp')"><fmt:setBundle basename="oscarResources"/><fmt:message key="global.license"/></a>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
        <tr>
            <td class="MainTableLeftColumn" valign="top">
                &nbsp;
                <%
                    ArrayList<ReportEvaluator> arrList = (ArrayList) session.getAttribute("ClinicalReports");
                    if (arrList != null) {
                %>
                <a href="report/ClinicalReports.jsp?clear=yes"><fmt:setBundle basename="oscarResources"/><fmt:message key="report.ClinicalReports.msgClear"/></a>
                <ul style="list-style-type:square; margin-left:1px;padding-left:4px;padding-top:2px;margin-top:2px;">
                    <% for (int i = 0; i < arrList.size(); i++) {
                        ReportEvaluator re = arrList.get(i);
                    %>
                    <li title="<%=re.getName()%>"><%=re.getNumeratorCount()%> / <%=re.getDenominatorCount()%>&nbsp;
                        <a style="text-decoration:none;" target="_blank" href="report/reportExport.jsp?id=<%=i%>"><fmt:setBundle basename="oscarResources"/><fmt:message key="report.ClinicalReports.msgCSV"/></a>&nbsp;
                        <a style="text-decoration:none;" href="report/RemoveClinicalReport.do?id=<%=i%>"><fmt:setBundle basename="oscarResources"/><fmt:message key="report.ClinicalReports.msgDel"/></a>
                    </li>
                    <% }%>
                </ul>
                <a style="text-decoration:none;" target="_blank" href="report/reportExport.jsp"><fmt:setBundle basename="oscarResources"/><fmt:message key="report.ClinicalReports.msgCSV"/></a>
                <%}%>

            </td>
            <td valign="top" class="MainTableRightColumn">
                <div>
                    <fieldset>
                        <form action="${pageContext.request.contextPath}//RunClinicalReport.do" method="post">
                            <!--
                            <label for="asOfDate" >As Of Date:</label><input type="text" name="asOfDate" id="asOfDate" value="<%=""%>" size="9" > <a id="date"><img title="Calendar" src="<%= request.getContextPath() %>/images/cal.gif" alt="Calendar" border="0" /></a> <br>
                            -->
                            <fieldset>
                                <legend><fmt:setBundle basename="oscarResources"/><fmt:message key="report.ClinicalReports.msgNumerator"/></legend>

                                <select name="numerator" id="numerator"
                                        onchange="javascript:processExtraFieldsNumerator(this)">
                                    <%
                                        for (Numerator n : numeratorList) {
                                            if (n.hasReplaceableValues()) {
                                                repNum.put(n.getId(), n.getReplaceableKeys());
                                            }

                                    %>
                                    <option value="<%=n.getId()%>"  <%=sel(numeratorId, n.getId())%> ><%=n.getNumeratorName()%>
                                    </option>
                                    <%}%>
                                </select>

                                <select id="numerator_measurements" name="numerator_measurements">
                                    <% for (EctMeasurementTypesBean measurementTypes : vec) {%>
                                    <option value="<%=measurementTypes.getType()%>"  <%=sel(measurementTypes.getType(), "" + request.getAttribute("numerator_measurements"))%>   ><%=measurementTypes.getTypeDisplayName()%>
                                        (<%=measurementTypes.getType()%>) (<%=measurementTypes.getMeasuringInstrc() %>)
                                    </option>
                                    <% }%>
                                </select>
                                <div id="numerator_value">
                                    <%
                                        numer_val[0] = "";
                                        if (request.getAttribute("numerator_value") != null) {
                                            numer_val[0] = request.getAttribute("numerator_value");
                                        }
                                    %>
                                    <fmt:setBundle basename="oscarResources"/><fmt:message key="report.ClinicalReports.msgValue"/> : <input type="text"
                                                                                                   name="numerator_value"
                                                                                                   value="<%=numer_val[0]%>"><br>

                                </div>
                                <div id="numerator_startDate">
                                    <%
                                        numer_startDate[0] = "";
                                        if (request.getAttribute("numerator_startDate") != null) {
                                            numer_startDate[0] = request.getAttribute("numerator_startDate");
                                        }
                                    %>
                                    Start Date : <input type="text" name="numerator_startDate"
                                                        value="<%=numer_startDate[0]%>"><br>

                                </div>
                                <div id="numerator_endDate">
                                    <%
                                        numer_endDate[0] = "";
                                        if (request.getAttribute("numerator_endDate") != null) {
                                            numer_endDate[0] = request.getAttribute("numerator_endDate");
                                        }
                                    %>
                                    End Date : <input type="text" name="numerator_endDate"
                                                      value="<%=numer_endDate[0]%>"><br>

                                </div>

                                <br/>
                            </fieldset>


                            <!-- start of extra numerators -->

                            <%for (int i = 0; i < 11; i++) { %>

                            <fieldset style="display:none" id="fieldNumerator<%=i%>">
                                <legend><fmt:setBundle basename="oscarResources"/><fmt:message key="report.ClinicalReports.msgNumerator"/></legend>

                                <select name="numerator<%=i %>" id="numerator<%=i %>"
                                        onchange="javascript:processExtraFieldsNumerator<%=i %>(this)">
                                    <option value="">Select Below</option>
                                    <%
                                        for (Numerator n : numeratorList) {
                                            if (n.hasReplaceableValues()) {
                                                arrRepNum[i].put(n.getId(), n.getReplaceableKeys());
                                            }

                                    %>
                                    <option value="<%=n.getId()%>"  <%=sel(arrNumeratorId[i], n.getId())%> ><%=n.getNumeratorName()%>
                                    </option>
                                    <%}%>
                                </select>

                                <select id="numerator<%=i %>_measurements" name="numerator<%=i %>_measurements">
                                    <% for (EctMeasurementTypesBean measurementTypes : vec) {%>
                                    <option value="<%=measurementTypes.getType()%>"  <%=sel(measurementTypes.getType(), "" + request.getAttribute("numerator" + i + "_measurements"))%>   ><%=measurementTypes.getTypeDisplayName()%>
                                        (<%=measurementTypes.getType()%>) (<%=measurementTypes.getMeasuringInstrc() %>)
                                    </option>
                                    <% }%>
                                </select>
                                <div id="numerator<%=i %>_value">
                                    <%
                                        numer_val[i] = "";
                                        if (request.getAttribute("numerator" + i + "_value") != null) {
                                            numer_val[i] = request.getAttribute("numerator" + i + "_value");
                                        }
                                    %>
                                    <fmt:setBundle basename="oscarResources"/><fmt:message key="report.ClinicalReports.msgValue"/> : <input type="text"
                                                                                                   name="numerator<%=i %>_value"
                                                                                                   value="<%=numer_val[i]%>"><br>

                                </div>
                                <div id="numerator<%=i %>_startDate">
                                    <%
                                        numer_startDate[i] = "";
                                        if (request.getAttribute("numerator" + i + "_startDate") != null) {
                                            numer_startDate[i] = request.getAttribute("numerator" + i + "_startDate");
                                        }
                                    %>
                                    Start Date : <input type="text" name="numerator<%=i %>_startDate"
                                                        value="<%=numer_startDate[i]%>"><br>

                                </div>
                                <div id="numerator<%=i %>_endDate">
                                    <%
                                        numer_endDate[i] = "";
                                        if (request.getAttribute("numerator" + i + "_endDate") != null) {
                                            numer_endDate[i] = request.getAttribute("numerator" + i + "_endDate");
                                        }
                                    %>
                                    End Date : <input type="text" name="numerator<%=i %>_endDate"
                                                      value="<%=numer_endDate[i]%>"><br>

                                </div>
                                <br/>
                            </fieldset>

                            <% } %>
                            <!-- end of extra numerators -->

                            <input type="button" value="Add Numerator" onClick="showNextNumerator()"
                                   id="addNumeratorBtn"/>
                            <br/>
                            <br/>

                            <fieldset>
                                <legend><fmt:setBundle basename="oscarResources"/><fmt:message key="report.ClinicalReports.msgDenominator"/></legend>

                                <select id="denominator" name="denominator"
                                        onchange="javascript:processExtraFields(this)">
                                    <%
                                        for (Denominator d : denominatorList) {
                                            if (d.hasReplaceableValues()) {
                                                rep.put(d.getId(), d.getReplaceableKeys());
                                            }
                                    %>
                                    <option value="<%=d.getId()%>" <%=sel(denominatorId, d.getId())%> ><%=d.getDenominatorName()%>
                                    </option>
                                    <%}%>
                                </select>

                                <%
                                    String defaultDenominatorProviderNo = provider;
                                    if (request.getAttribute("denominator_provider_no") != null) {
                                        defaultDenominatorProviderNo = (String) request.getAttribute("denominator_provider_no");
                                    }
                                %>
                                <select id="denominator_provider_no" name="denominator_provider_no">
                                    <%for (Map<String, String> h : providers) {%>
                                    <option value="<%= h.get("providerNo")%>" <%= (h.get("providerNo").equals(defaultDenominatorProviderNo) ? " selected" : "")%>><%= h.get("lastName")%> <%= h.get("firstName")%>
                                    </option>
                                    <%}%>
                                </select>
                                <div id="denominator_patientSet" name="denominator_patientSet">
                                    <%
                                        String defaultDenominatorPatientSet = "";
                                        if (request.getAttribute("denominator_patientSet") != null) {
                                            defaultDenominatorPatientSet = (String) request.getAttribute("denominator_patientSet");
                                        }
                                    %>
                                    <%for (String listName : demoSetList) {%>
                                    <input type="checkbox" name="denominator_patientSet"
                                           value="<%=listName%>" <%=listName.equals(defaultDenominatorPatientSet) ? " checked=\"checked\" " : "" %>><%=listName%>
                                    <br>
                                    <%}%>
                                </div>

                            </fieldset>

                            <fieldset>
                                <legend><fmt:setBundle basename="oscarResources"/><fmt:message key="report.ClinicalReports.Fieldstoinclude"/></legend>
                                <input type="checkbox"
                                       name="showfields" <%=dchecked((String[]) request.getAttribute("showfields"), "lastName")%>
                                       value="lastName"><fmt:setBundle basename="oscarResources"/><fmt:message key="report.ClinicalReports.msgLastName"/></input>
                                <input type="checkbox"
                                       name="showfields" <%=dchecked((String[]) request.getAttribute("showfields"), "firstName")%>
                                       value="firstName"><fmt:setBundle basename="oscarResources"/><fmt:message key="report.ClinicalReports.msgFirstName"/></input>
                                <input type="checkbox"
                                       name="showfields" <%=dchecked((String[]) request.getAttribute("showfields"), "sex")%>
                                       value="sex"><fmt:setBundle basename="oscarResources"/><fmt:message key="report.ClinicalReports.msgSex"/></input>
                                <input type="checkbox"
                                       name="showfields" <%=dchecked((String[]) request.getAttribute("showfields"), "phone")%>
                                       value="phone"><fmt:setBundle basename="oscarResources"/><fmt:message key="report.ClinicalReports.msgPhone"/></input>
                                <input type="checkbox"
                                       name="showfields" <%=dchecked((String[]) request.getAttribute("showfields"), "address")%>
                                       value="address"><fmt:setBundle basename="oscarResources"/><fmt:message key="report.ClinicalReports.msgAddress"/></input>
                                <br/>
                                <%for (int rm = 0; rm < 3; rm++) {%>
                                <select name="report_measurement<%=rm%>">
                                    <option value="-1"><fmt:setBundle basename="oscarResources"/><fmt:message key="report.ClinicalReports.msgNoMeasurements"/></option>
                                    <% for (EctMeasurementTypesBean measurementTypes : vec) {
                                        String measInst = measurementTypes.getMeasuringInstrc();
                                        if (measInst.length() > 25) {
                                            measInst = StringUtils.abbreviate(measurementTypes.getMeasuringInstrc(), 25);
                                        }
                                    %>
                                    <option value="<%=measurementTypes.getType()%>" <%=sel(measurementTypes.getType(), "" + request.getAttribute("report_measurement" + rm))%> ><%=measurementTypes.getTypeDisplayName()%>
                                        (<%=measurementTypes.getType()%>) (<%=measInst %>)
                                    </option>
                                    <% }%>
                                </select>
                                <%}%>


                            </fieldset>

                            <%
                                String includeNonPositiveResultsCheck = "";
                                if (request.getAttribute("includeNonPositiveResults") != null && (Boolean) request.getAttribute("includeNonPositiveResults") == true) {
                                    includeNonPositiveResultsCheck = " checked=\"checked\" ";
                                }
                            %>
                            <input type="checkbox" name="includeNonPositiveResults"
                                   id="includeNonPositiveResults" <%=includeNonPositiveResultsCheck %>/>&nbsp;Include All Results (includes results where numerator evaluates to false)

                            <br/>
                            <input type="submit" value="<fmt:setBundle basename="oscarResources"/><fmt:message key="report.ClinicalReports.btnEvaluate"/>"/>
                        </form>
                    </fieldset>

                </div>

                <% if (request.getAttribute("denominator") != null) {%>
                <div>
                    <H3><fmt:setBundle basename="oscarResources"/><fmt:message key="report.ClinicalReports.msgResults"/></H3>
                    <ul>
                        <li><fmt:setBundle basename="oscarResources"/><fmt:message key="report.ClinicalReports.msgNumerator"/>:   <%=request.getAttribute("numerator")%>
                        </li>
                        <li><fmt:setBundle basename="oscarResources"/><fmt:message key="report.ClinicalReports.msgDenominator"/>: <%=request.getAttribute("denominator")%>
                        </li>
                        <li><fmt:setBundle basename="oscarResources"/><fmt:message key="report.ClinicalReports.msgPercentage"/>:  <%=request.getAttribute("percentage")%> %
                        </li>
                    </ul>
                    CSV:<input type="text" size="30" value="<%=request.getAttribute("csv")%>"/>
                </div>
                <%}%>


                <%

                    String[] outputfields = {"_demographic_no", "_report_result"};

                    Hashtable forView = new Hashtable();
                    if (request.getAttribute("extraValues") != null) {
                        List<org.apache.commons.collections.keyvalue.DefaultKeyValue> extraValues = (List) request.getAttribute("extraValues");

                        for (org.apache.commons.collections.keyvalue.DefaultKeyValue kv : extraValues) {
                            String[] temp = new String[outputfields.length + 1];
                            System.arraycopy(outputfields, 0, temp, 0, outputfields.length);
                            temp[outputfields.length] = "" + kv.getKey();
                            outputfields = temp;

                            forView.put(kv.getKey(), kv.getValue());
                        }
                    }

                    if (request.getAttribute("list") != null) {
                        DemographicNameAgeString deName = DemographicNameAgeString.getInstance();
                        DemographicData demoData = new DemographicData();
                        StringWriter swr = new StringWriter();
                        CSVPrinter csvp = new CSVPrinter(swr);
                %>

                <table class="sortable tabular_list results" id="results_table">
                    <thead>
                    <tr>
                        <%
                            for (String heading : headings) {
                                csvp.write(head(heading));
                        %>
                        <th><%=head(heading)%>
                        </th>
                        <%}%>

                        <%
                            for (int i = 0; i < outputfields.length; i++) {
                                csvp.write(replaceHeading(outputfields[i], forView, measurementTitles));
                        %>
                        <th><%=replaceHeading(outputfields[i], forView, measurementTitles)%>
                        </th>
                        <%}%>
                    </tr>
                    </thead>
                    <%
                        csvp.writeln();
                        ArrayList<Hashtable> list = (ArrayList) request.getAttribute("list");
                        for (Hashtable h : list) {

                            Hashtable demoHash = deName.getNameAgeSexHashtable(LoggedInInfo.getLoggedInInfoFromSession(request), "" + h.get("_demographic_no"));
                            org.oscarehr.common.model.Demographic demoObj = demoData.getDemographic(LoggedInInfo.getLoggedInInfoFromSession(request), "" + h.get("_demographic_no"));

                            String colour = "";
                            if (h.get("_report_result") != null && ("" + h.get("_report_result")).equals("false")) {
                                colour = "class=red";
                            }
                    %>
                    <tr <%=colour%> >

                        <%
                            for (String heading : headings) {
                                csvp.write(commonRow(heading, demoHash, demoObj));
                        %>
                        <td><%=commonRow(heading, demoHash, demoObj)%>
                        </td>
                        <%}%>

                        <%
                            for (String outputfield : outputfields) {
                                csvp.write("" + display(h.get(outputfield)));
                        %>
                        <td><%=display(h.get(outputfield))%>
                        </td>
                        <%}%>
                    </tr>
                    <%
                            csvp.writeln();
                        }%>
                </table>
                <%

                        session.setAttribute("clinicalReportCSV", swr.toString());
                    }
                %>
                <form target="_new" action="report/ClinicalExport.jsp">
                    <input type="submit" name="getCSV"
                           value="<fmt:setBundle basename="oscarResources"/><fmt:message key="report.ClinicalReports.msgExporttoCSV"/>">
                    <input type="submit" name="getXLS"
                           value="<fmt:setBundle basename="oscarResources"/><fmt:message key="report.ClinicalReports.msgExporttoXLS"/>">
                </form>
            </td>
        </tr>
        <tr>
            <td class="MainTableBottomRowLeftColumn">
                &nbsp;
            </td>
            <td class="MainTableBottomRowRightColumn" valign="top">
                &nbsp;
            </td>
        </tr>
    </table>
    <!-- div>
       ToDos
       <ul>
          <li>-Show values of values in question.  ie Date of last BP measurement. Value of last A1C</li>
          <li>-export PDF pretty version</li>

       </ul>
    </div  -->
    <script type="text/javascript">
        //Calendar.setup( { inputField : "asOfDate", ifFormat : "%Y-%m-%d", showsTime :false, button : "date", singleClick : true, step : 1 } );
        denom_xtras = new Array();
        numer_xtras = new Array();

        <%
            for(int x=0;x<11;x++) {
                %>numer<%=x%>_xtras = new Array();
        <%
            }
        %>
        <% Enumeration e = rep.keys();
           while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            String[] repValues = (String[]) rep.get(key);%>
        var repVal<%=key%> = new Array();
        <%for (int i = 0; i < repValues.length; i++) {%>
        repVal<%=key%>[<%=i%>] = "denominator_<%=repValues[i]%>";
        <%}%>
        denom_xtras['<%=key%>'] = repVal<%=key%>;
        <% }%>

        <% Enumeration e2 = repNum.keys();
           while (e2.hasMoreElements()) {
            String key = (String) e2.nextElement();
            String[] repNumValues = (String[]) repNum.get(key);%>
        var repNumVal<%=key%> = new Array();
        <%for (int i = 0; i < repNumValues.length; i++) {%>
        repNumVal<%=key%>[<%=i%>] = "numerator_<%=repNumValues[i]%>";
        <%}%>
        numer_xtras['<%=key%>'] = repNumVal<%=key%>;
        <% }%>



        <% for(int x=0;x<arrRepNum.length;x++) {
            Enumeration en = arrRepNum[x].keys();
            while (en.hasMoreElements()) {
                 String key = (String) en.nextElement();
                 String[] repNumValues = (String[]) arrRepNum[x].get(key);%>
        var repNumVal<%=x%><%=key%> = new Array();
        <%for (int i = 0; i < repNumValues.length; i++) {%>
        repNumVal<%=x%><%=key%>[<%=i%>] = "numerator<%=x%>_<%=repNumValues[i]%>";
        <%}%>
        numer<%=x%>_xtras['<%=key%>'] = repNumVal<%=x%><%=key%>;
        <%	}
         } %>


        processExtraFields(document.getElementById('denominator'));
        processExtraFieldsNumerator(document.getElementById('numerator'));

        <%for(int x=0;x<11;x++) {%>
        processExtraFieldsNumerator<%=x%>(document.getElementById('numerator<%=x%>'));
        <%}%>

    </script>
    <script language="javascript" src="<%= request.getContextPath() %>/commons/scripts/sort_table/css.js"></script>
    <script language="javascript" src="<%= request.getContextPath() %>/commons/scripts/sort_table/common.js"></script>
    <script language="javascript" src="<%= request.getContextPath() %>/commons/scripts/sort_table/standardista-table-sorting.js"></script>
    </body>
</html>
<%!
    Object display(Object obj) {
        if (obj == null) {
            return "";
        }

        if (obj instanceof oscar.oscarEncounter.oscarMeasurements.bean.EctMeasurementsDataBean) {
            oscar.oscarEncounter.oscarMeasurements.bean.EctMeasurementsDataBean md = (oscar.oscarEncounter.oscarMeasurements.bean.EctMeasurementsDataBean) obj;
            return md.getDateObserved() + ": " + md.getDataField();
        }


        return obj;
    }

    String completed(boolean b) {
        String ret = "";
        if (b) {
            ret = "checked";
        }
        return ret;
    }

    String refused(boolean b) {
        String ret = "";
        if (!b) {
            ret = "checked";
        }
        return ret;
    }

    String str(String first, String second) {
        String ret = "";
        if (first != null) {
            ret = first;
        } else if (second != null) {
            ret = second;
        }
        return ret;
    }

    String checked(String first, String second) {
        String ret = "";
        if (first != null && second != null) {
            if (first.equals(second)) {
                ret = "checked";
            }
        }
        return ret;
    }

    String dchecked(String[] first, String second) {
        String ret = "checked";
        if (first != null && second != null) {
            boolean found = false;
            for (String s : first) {
                if (s.equals(second)) {
                    found = true;
                }
            }
            if (!found) {
                ret = "";
            }
        }
        return ret;
    }

    String sel(String s1, String s2) {
        String ret = "";
        if (s1 != null && s2 != null && s1.equals(s2)) {
            ret = "selected";
        }
        return ret;
    }

    String replaceHeading(String s, Map h, Map mTitle) {
        if (s != null && s.equals("_demographic_no")) {
            return "Demographic #";
        } else if (s.equals("_report_result")) {
            return "Report Result";
        } else if (s.startsWith("report_measurement")) {
            String type = (String) h.get(s);
            return "Latest " + mTitle.get(type);
        }
        return s;
    }

    String head(String heading) {
        if (heading == null) {
            return "";
        }

        if ("lastName".equals(heading)) {
            return "Last Name";
        } else if ("firstName".equals(heading)) {
            return "First Name";
        } else if ("sex".equals(heading)) {
            return "Sex";
        } else if ("phone".equals(heading)) {
            return "Phone #";
        } else if ("address".equals(heading)) {
            return "Address";
        }
        return heading;
    }


    String commonRow(String heading, Hashtable demoHash, org.oscarehr.common.model.Demographic demoObj) {
        if (heading == null) {
            return "";
        }

        if ("lastName".equals(heading)) {
            return "" + demoHash.get("lastName");
        } else if ("firstName".equals(heading)) {
            return "" + demoHash.get("firstName");
        } else if ("sex".equals(heading)) {
            return "" + demoHash.get("sex");
        } else if ("phone".equals(heading)) {
            return demoObj.getPhone();
        } else if ("address".equals(heading)) {
            return demoObj.getAddress() + " " + demoObj.getCity() + " " + demoObj.getProvince() + " " + demoObj.getPostal();
        }
        return heading;
    }


%>
