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
<%@page import="org.apache.commons.lang.StringUtils" %>
<%@page import="oscar.oscarEncounter.pageUtil.EctDisplayLabAction2" %>
<%@page import="org.oscarehr.util.MiscUtils" %>
<%@page import="java.net.URLEncoder" %>
<%@page import="oscar.oscarLab.ca.all.web.LabDisplayHelper" %>
<%@page import="org.oscarehr.util.LoggedInInfo" %>
<%@ page import="java.util.*" %>
<%@ page import="oscar.oscarLab.ca.on.LabResultData" %>
<%@ page import="oscar.oscarMDS.data.*,oscar.oscarLab.ca.on.*" %>
<%@ page import="oscar.util.DateUtils" %>
<%@ page import="oscar.oscarLab.ca.all.Hl7textResultsData" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/security.tld" prefix="security" %>
<%
    String roleName$ = (String) session.getAttribute("userrole") + "," + (String) session.getAttribute("user");
    boolean authed = true;
%>
<security:oscarSec roleName="<%=roleName$%>" objectName="_lab" rights="r" reverse="<%=true%>">
    <%authed = false; %>
    <%response.sendRedirect("../../securityError.jsp?type=_lab");%>
</security:oscarSec>
<%
    if (!authed) {
        return;
    }
%>

<%


    //oscar.oscarMDS.data.MDSResultsData mDSData = new oscar.oscarMDS.data.MDSResultsData();
    CommonLabResultData comLab = new CommonLabResultData();
    //String providerNo = request.getParameter("providerNo");
    String providerNo = (String) session.getAttribute("user");
    String searchProviderNo = request.getParameter("searchProviderNo");
    String ackStatus = request.getParameter("status");
    String demographicNo = request.getParameter("demographicNo"); // used when searching for labs by patient instead of provider

    if (ackStatus == null) {
        ackStatus = "N";
    } // default to only new lab reports
    if (providerNo == null) {
        providerNo = "";
    }
    if (searchProviderNo == null) {
        searchProviderNo = providerNo;
    }

    ArrayList<LabResultData> labs = comLab.populateLabResultsData(LoggedInInfo.getLoggedInInfoFromSession(request), "", demographicNo, "", "", "", "U");

    LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
    if (loggedInInfo.getCurrentFacility().isIntegratorEnabled()) {
        ArrayList<LabResultData> remoteResults = CommonLabResultData.getRemoteLabs(loggedInInfo, Integer.parseInt(demographicNo));
        labs.addAll(remoteResults);
    }


    int pageNum = 1;
    if (request.getParameter("pageNum") != null) {
        pageNum = Integer.parseInt(request.getParameter("pageNum"));
    }

    LabResultData result;

    // Comment out this code and instead using the correct method to
    // get latest lab versions, which is the below.
    // LinkedHashMap<String,LabResultData> accessionMap = new LinkedHashMap<String,LabResultData>();
    // for (int i = 0; i < labs.size(); i++) {
    // 	result = labs.get(i);
    // 	if (result.accessionNumber == null || result.accessionNumber.equals("")) {
    // 		accessionMap.put("noAccessionNum" + i + result.labType, result);
    // 	} else {
    // 		if (!accessionMap.containsKey(result.accessionNumber + result.labType)) accessionMap.put(result.accessionNumber + result.labType, result);
    // 	}
    // }
    // labs = new ArrayList<LabResultData>(accessionMap.values());

    //First, getting the latest versions of the lab results and then sorting them ensures 
    //that they will be displayed in the correct date order in the encounter window.
    labs = getLatestLabVersions(labs);
    Collections.sort(labs);


%>
<html>
<head>
    <script type="text/javascript" src="<%= request.getContextPath() %>/js/global.js"></script>
    <title><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarMDS.index.title"/> Page <%=pageNum%>
    </title>
    <base href="<%= request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/" %>">

    <!-- link rel="stylesheet" type="text/css" href="encounterStyles.css" -->
    <link rel="stylesheet" type="text/css"
          href="<%= request.getContextPath() %>/share/css/OscarStandardLayout.css">
    <link rel="stylesheet" type="text/css" media="all" href="<%= request.getContextPath() %>/share/css/extractedFromPages.css"/>


    <script type="text/javascript" language=javascript>

        function popupStart(vheight, vwidth, varpage) {
            popupStart(vheight, vwidth, varpage, "helpwindow");
        }

        function popupStart(vheight, vwidth, varpage, windowname) {
            var page = varpage;
            windowprops = "height=" + vheight + ",width=" + vwidth + ",location=no,scrollbars=yes,menubars=no,toolbars=no,resizable=yes";
            var popup = window.open(varpage, windowname, windowprops);
        }

        function reportWindow(page) {
            windowprops = "height=660, width=960, location=no, scrollbars=yes, menubars=no, toolbars=no, resizable=yes, top=0, left=0";
            var popup = window.open(page, "labreport", windowprops);
            popup.focus();
        }

        function checkSelected() {
            aBoxIsChecked = false;
            if (document.reassignForm.flaggedLabs.length == undefined) {
                if (document.reassignForm.flaggedLabs.checked == true) {
                    aBoxIsChecked = true;
                }
            } else {
                for (i = 0; i < document.reassignForm.flaggedLabs.length; i++) {
                    if (document.reassignForm.flaggedLabs[i].checked == true) {
                        aBoxIsChecked = true;
                    }
                }
            }
            if (aBoxIsChecked) {
                popupStart(300, 400, 'SelectProvider.jsp', 'providerselect');
            } else {
                alert('<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarMDS.index.msgSelectOneLab"/>');
            }
        }

        function submitFile() {
            console.log("File from: DemographicLab.jsp");
            aBoxIsChecked = false;
            if (document.reassignForm.flaggedLabs.length == undefined) {
                if (document.reassignForm.flaggedLabs.checked == true) {
                    aBoxIsChecked = true;
                }
            } else {
                for (i = 0; i < document.reassignForm.flaggedLabs.length; i++) {
                    if (document.reassignForm.flaggedLabs[i].checked == true) {
                        aBoxIsChecked = true;
                    }
                }
            }
            if (aBoxIsChecked) {
                document.reassignForm.action = '../oscarLab/FileLabs.do';
                document.reassignForm.submit();
            }
        }

        function checkAll(formId) {
            var f = document.getElementById(formId);
            var val = f.checkA.checked;
            for (i = 0; i < f.flaggedLabs.length; i++) {
                f.flaggedLabs[i].checked = val;
            }
        }
    </script>

    <link rel="stylesheet" type="text/css"
          href="<%= request.getContextPath() %>/js/jquery_css/smoothness/jquery-ui-1.10.2.custom.min.css"/>
    <script type="text/javascript" src="<%= request.getContextPath() %>/js/jquery-1.9.1.js"></script>
    <script type="text/javascript" src="<%= request.getContextPath() %>/js/jquery-ui-1.10.2.custom.min.js"></script>


    <script>
        $(function () {
            $(document).tooltip();
        });
    </script>

    <style>
        .visLink {
            color: white;
        }
    </style>
</head>

<body oldclass="BodyStyle" vlink="#0000FF">
<form name="reassignForm" method="post" action="ReportReassign.do"
      id="lab_form">
    <table oldclass="MainTable" id="scrollNumber1" border="0"
           name="encounterTable" cellspacing="0" cellpadding="3" width="100%">
        <tr oldclass="MainTableTopRow">
            <td class="MainTableTopRowRightColumn" colspan="9" align="left">
                <table width="100%">
                    <tr>
                        <td align="left" valign="center" width="30%"><input
                                type="hidden" name="providerNo" value="<%= providerNo %>">
                            <input type="hidden" name="searchProviderNo"
                                   value="<%= searchProviderNo %>"> <%= (request.getParameter("lname") == null ? "" : "<input type=\"hidden\" name=\"lname\" value=\"" + request.getParameter("lname") + "\">") %>
                            <%= (request.getParameter("fname") == null ? "" : "<input type=\"hidden\" name=\"fname\" value=\"" + request.getParameter("fname") + "\">") %>
                            <%= (request.getParameter("hnum") == null ? "" : "<input type=\"hidden\" name=\"hnum\" value=\"" + request.getParameter("hnum") + "\">") %>
                            <input type="hidden" name="status" value="<%= ackStatus %>">
                            <input type="hidden" name="selectedProviders"> <% if (demographicNo == null) { %>
                            <input type="button" class="smallButton"
                                   value="<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarMDS.index.btnSearch"/>"
                                   onClick="window.location='Search.jsp?providerNo=<%= providerNo %>'">
                            <% } %> <input type="button" class="smallButton"
                                           value="<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarMDS.index.btnClose"/>"
                                           onClick="window.close()">

                            <% if (demographicNo != null) { %>
                            <input type="button" class="smallButton"
                                   value="Search OLIS"
                                   onClick="popupStart('1000','1200','<%=request.getContextPath() %>/olis/Search.jsp?demographicNo=<%=demographicNo %>','OLIS_SEARCH')">
                            <% } %>

                            <% if (demographicNo == null && request.getParameter("fname") != null) { %>
                            <input type="button" class="smallButton"
                                   value="<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarMDS.index.btnDefaultView"/>"
                                   onClick="window.location='DemographicLab.jsp?providerNo=<%= providerNo %>'">
                            <% } %> <% if (demographicNo == null && labs.size() > 0) { %>
                            <!-- <input type="button" class="smallButton" value="Reassign" onClick="popupStart(300, 400, 'SelectProvider.jsp', 'providerselect')"> -->
                            <input type="button" class="smallButton"
                                   value="<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarMDS.index.btnForward"/>"
                                   onClick="checkSelected()"> <input type="button"
                                                                     class="smallButton" value="File"
                                                                     onclick="submitFile()"/>
                            <span title="<fmt:setBundle basename="oscarResources"/><fmt:message key="global.uploadWarningBody"/>"
                                  style="vertical-align:middle;font-family:arial;font-size:20px;font-weight:bold;color:#ABABAB;cursor:pointer"><img
                                    border="0" src="<%= request.getContextPath() %>/images/icon_alertsml.gif"/></span></span>

                            <% } %>
                        </td>
                        <td align="center" valign="center" width="40%" class="Nav">
                            &nbsp;&nbsp;&nbsp; <% if (demographicNo == null) { %> <span
                                class="white"> <% if (ackStatus.equals("N")) {%> <fmt:setBundle basename="oscarResources"/><fmt:message key="oscarMDS.index.msgNewLabReportsFor"/> <%} else if (ackStatus.equals("A")) {%>
				<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarMDS.index.msgAcknowledgedLabReportsFor"/> <%} else {%>
				<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarMDS.index.msgAllLabReportsFor"/> <%}%>&nbsp;
				<% if (searchProviderNo.equals("")) {%> <fmt:setBundle basename="oscarResources"/><fmt:message key="oscarMDS.index.msgAllPhysicians"/> <%} else if (searchProviderNo.equals("0")) {%>
				<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarMDS.index.msgUnclaimed"/> <%} else {%> <%=ProviderData.getProviderName(searchProviderNo)%>
				<%}%> &nbsp;&nbsp;&nbsp; Page : <%=pageNum%> </span> <% } %>
                        </td>
                        <td align="right" valign="center" width="30%"><a
                                href="javascript:popupStart(300,400,'../oscarEncounter/About.jsp')"><fmt:setBundle basename="oscarResources"/><fmt:message key="global.about"/></a></td>
                    </tr>
                </table>
            </td>
        </tr>
        <tr>
            <th class="cell"><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarMDS.index.msgDateTest"/></th>
            <th class="cell">
                <fmt:setBundle basename="oscarResources"/><fmt:message key="oscarMDS.index.msgLabel"/>
            </th>
            <th class="cell"><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarMDS.index.msgRequestingClient"/></th>
            <th class="cell"><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarMDS.index.msgResultStatus"/></th>

            <th class="cell"><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarMDS.index.msgReportStatus"/></th>
            <th class="cell">
                <fmt:setBundle basename="oscarResources"/><fmt:message key="oscarMDS.index.msgDiscipline"/>
            </th>
        </tr>

        <%
            int startIndex = 0;
            if (request.getParameter("startIndex") != null) {
                startIndex = Integer.parseInt(request.getParameter("startIndex"));
            }
            int endIndex = startIndex + 20;
            if (labs.size() < endIndex) {
                endIndex = labs.size();
            }

            for (int i = startIndex; i < endIndex; i++) {


                result = (LabResultData) labs.get(i);

                String segmentID = (String) result.segmentID;
                String status = (String) result.acknowledgedStatus;

                String resultStatus = (String) result.resultStatus;

                String bgcolor = i % 2 == 0 ? "#e0e0ff" : "#ccccff";
                if (!result.isMatchedToPatient()) {
                    bgcolor = "#FFCC00";
                }
        %>

        <tr bgcolor="<%=bgcolor%>" class="<%= (result.isAbnormal() ? "AbnormalRes" : "NormalRes" ) %>">
            <td>
                <%
                    Date d1 = getServiceDate(loggedInInfo, result);
                    String formattedDate = DateUtils.getDate(d1);

                %>
                <%=formattedDate %>
            </td>
            <td>
                <%
                    String remoteFacilityIdQueryString = "";
                    if (result.getRemoteFacilityId() != null) {
                        try {
                            remoteFacilityIdQueryString = "&remoteFacilityId=" + result.getRemoteFacilityId();
                            String remoteLabKey = LabDisplayHelper.makeLabKey(Integer.parseInt(result.getLabPatientId()), result.getSegmentID(), result.labType, result.getDateTime());
                            remoteFacilityIdQueryString = remoteFacilityIdQueryString + "&remoteLabKey=" + URLEncoder.encode(remoteLabKey, "UTF-8");
                        } catch (Exception e) {
                            MiscUtils.getLogger().error("Error", e);
                        }
                    }

                    if (result.isMDS()) { %> <a
                    href="javascript:reportWindow('../oscarMDS/SegmentDisplay.jsp?demographicId=<%=demographicNo%>&segmentID=<%=segmentID%>&providerNo=<%=providerNo%>&searchProviderNo=<%=searchProviderNo%>&status=<%=status%><%=remoteFacilityIdQueryString%>')"><%= result.getDiscipline()%>
            </a>
                <% } else if (result.isCML()) { %> <a
                    href="javascript:reportWindow('../lab/CA/ON/CMLDisplay.jsp?demographicId=<%=demographicNo%>&segmentID=<%=segmentID%>&providerNo=<%=providerNo%>&searchProviderNo=<%=searchProviderNo%>&status=<%=status%><%=remoteFacilityIdQueryString%>')"><%=(String) result.getDiscipline()%>
            </a>
                <% } else if (result.isHL7TEXT()) {%>
                <a href="javascript:reportWindow('../lab/CA/ALL/labDisplay.jsp?demographicId=<%=demographicNo%>&segmentID=<%=segmentID%>&providerNo=<%=providerNo%>&searchProviderNo=<%=searchProviderNo%>&status=<%=status%><%=remoteFacilityIdQueryString%>')">
                    <%=StringUtils.trimToEmpty(result.getLabel())%>
                </a>
                <% } else {%>
                <a href="javascript:reportWindow('../lab/CA/BC/labDisplay.jsp?demographicId=<%=demographicNo%>&segmentID=<%=segmentID%>&providerNo=<%=providerNo%>&searchProviderNo=<%=searchProviderNo%>&status=<%=status%><%=remoteFacilityIdQueryString%>')">
                    <%=StringUtils.trimToEmpty(result.getLabel())%>
                </a>
                <% }%>
            </td>

            <td><%= StringUtils.trimToEmpty(result.getRequestingClient())%>
            </td>
            <td><%= (result.isAbnormal() ? "Abnormal" : "") %>
            </td>

            <!--td >
                    <%= result.getPriority()%>
                </td-->


            <td><%= ((result.isFinal() ? "Final" : "Partial"))%>
            </td>
            <td><%=StringUtils.trimToEmpty(result.getDiscipline()) %>
            </td>
        </tr>
        <% }

            if (endIndex == 0) { %>
        <tr>
            <td colspan="9" align="center"><i><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarMDS.index.msgNoReports"/></i></td>
        </tr>
        <% } %>
        <tr class="MainTableBottomRow">
            <td class="MainTableBottomRowRightColumn" bgcolor="#003399"
                colspan="9" align="left">
                <table width="100%">
                    <tr>
                        <td align="left" valign="middle" width="30%">
                            <% if (demographicNo == null) { %> <input type="button"
                                                                      class="smallButton"
                                                                      value="<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarMDS.index.btnSearch"/>"
                                                                      onClick="window.location='Search.jsp?providerNo=<%= providerNo %>'">
                            <% } %> <input type="button" class="smallButton"
                                           value="<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarMDS.index.btnClose"/>"
                                           onClick="window.close()"> <% if (request.getParameter("fname") != null) { %>
                            <input type="button" class="smallButton"
                                   value="<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarMDS.index.btnDefaultView"/>"
                                   onClick="window.location='DemographicLab.jsp?providerNo=<%= providerNo %>'">
                            <% } %> <% if (demographicNo == null && labs.size() > 0) { %>
                            <!-- <input type="button" class="smallButton" value="Reassign" onClick="popupStart(300, 400, 'SelectProvider.jsp', 'providerselect')"> -->
                            <input type="button" class="smallButton"
                                   value="<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarMDS.index.btnForward"/>"
                                   onClick="checkSelected()"> <input type="button"
                                                                     class="smallButton" value="File"
                                                                     onclick="submitFile()"/> <% } %>
                        </td>
                        <td align="center" valign="middle" width="40%">
                            <div class="Nav">
                                <% if (pageNum > 1 || labs.size() > endIndex) {
                                    if (pageNum > 1) { %> <a class="visLink"
                                                             href="DemographicLab.jsp?providerNo=<%=providerNo%><%= (demographicNo == null ? "" : "&demographicNo="+demographicNo ) %>&searchProviderNo=<%=searchProviderNo%>&status=<%=ackStatus%><%= (request.getParameter("lname") == null ? "" : "&lname="+request.getParameter("lname")) %><%= (request.getParameter("fname") == null ? "" : "&fname="+request.getParameter("fname")) %><%= (request.getParameter("hnum") == null ? "" : "&hnum="+request.getParameter("hnum")) %>&pageNum=<%=pageNum-1%>&startIndex=<%=startIndex-20%>"><
                                <fmt:setBundle basename="oscarResources"/><fmt:message key="oscarMDS.index.msgPrevious"/></a> <% } else { %>
                                &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                                <% } %> <%
                                int count = 1;
                                for (int i = 0; i < labs.size(); i = i + 20) {
                            %>
                                <a style="text-decoration: none;" class="visLink"
                                   href="DemographicLab.jsp?providerNo=<%=providerNo%><%= (demographicNo == null ? "" : "&demographicNo="+demographicNo ) %>&searchProviderNo=<%=searchProviderNo%>&status=<%=ackStatus%><%= (request.getParameter("lname") == null ? "" : "&lname="+request.getParameter("lname")) %><%= (request.getParameter("fname") == null ? "" : "&fname="+request.getParameter("fname")) %><%= (request.getParameter("hnum") == null ? "" : "&hnum="+request.getParameter("hnum")) %>&pageNum=<%=count%>&startIndex=<%=i%>">[<%=count%>
                                    ]</a>
                                <%
                                        count++;
                                    }
                                %> <% if (labs.size() > endIndex) { %>
                                <a
                                        class="visLink"
                                        href="DemographicLab.jsp?providerNo=<%=providerNo%><%= (demographicNo == null ? "" : "&demographicNo="+demographicNo ) %>&searchProviderNo=<%=searchProviderNo%>&status=<%=ackStatus%><%= (request.getParameter("lname") == null ? "" : "&lname="+request.getParameter("lname")) %><%= (request.getParameter("fname") == null ? "" : "&fname="+request.getParameter("fname")) %><%= (request.getParameter("hnum") == null ? "" : "&hnum="+request.getParameter("hnum")) %>&pageNum=<%=pageNum+1%>&startIndex=<%=startIndex+20%>"><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarMDS.index.msgNext"/> ></a> <% } else { %>
                                &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                                <% }
                                } %>
                            </div>
                        </td>
                        <td align="right" width="30%">&nbsp;</td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</form>
</body>
</html>
<%!
    public Date getServiceDate(LoggedInInfo loggedInInfo, LabResultData labData) {
        EctDisplayLabAction2.ServiceDateLoader loader = new EctDisplayLabAction2.ServiceDateLoader(labData);
        Date resultDate = loader.determineResultDate(loggedInInfo);
        if (resultDate != null) {
            return resultDate;
        }
        return labData.getDateObj();
    }

    public ArrayList<LabResultData> getLatestLabVersions(ArrayList<LabResultData> labs) {
        List<String> allLabIds = new ArrayList<>();
        ArrayList<LabResultData> latestLabVersions = new ArrayList<>();
        for (LabResultData lab : labs) {
            if (allLabIds.contains(lab.getSegmentID())) {
                continue;
            }

            String[] allLabVersionIdsOfLab = Hl7textResultsData.getMatchingLabs(lab.getSegmentID()).split(",");
            allLabIds.addAll(Arrays.asList(allLabVersionIdsOfLab));

            for (LabResultData labResultData : labs) {
                if (allLabVersionIdsOfLab[allLabVersionIdsOfLab.length - 1].equals(labResultData.getSegmentID())) {
                    latestLabVersions.add(labResultData);
                    break;
                }
            }
        }
        return latestLabVersions;
    }
%>
