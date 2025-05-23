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
<%@page import="org.oscarehr.common.model.PharmacyInfo" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/oscarProperties-tag.tld" prefix="oscarProp" %>
<%@ page import="oscar.oscarRx.data.*" %>
<%@page import="org.oscarehr.util.SpringUtils" %>
<%@page import="java.util.List" %>
<%@page import="org.oscarehr.util.LoggedInInfo" %>
<%@page import="java.util.ArrayList,oscar.util.*,java.util.*,org.oscarehr.common.model.Drug,org.oscarehr.common.dao.*" %>
<%@ taglib uri="/WEB-INF/security.tld" prefix="security" %>
<%
    String roleName$ = (String) session.getAttribute("userrole") + "," + (String) session.getAttribute("user");
    boolean authed = true;
%>
<security:oscarSec roleName="<%=roleName$%>" objectName="_rx" rights="r" reverse="<%=true%>">
    <%authed = false; %>
    <%response.sendRedirect("../securityError.jsp?type=_rx");%>
</security:oscarSec>
<%
    if (!authed) {
        return;
    }
%>

<%
    oscar.oscarRx.pageUtil.RxSessionBean bean = (oscar.oscarRx.pageUtil.RxSessionBean) pageContext.findAttribute("bean");

    String userfirstname = (String) session.getAttribute("userfirstname");
    String userlastname = (String) session.getAttribute("userlastname");
    String demographicNo = (String) request.getParameter("demographicNo");
    LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
    oscar.oscarRx.data.RxPatientData.Patient patient = RxPatientData.getPatient(loggedInInfo, demographicNo);
    request.setAttribute("patient", patient);
%>

<% RxPharmacyData pharmacyData = new RxPharmacyData();

    List<PharmacyInfo> pharmacyList = pharmacyData.getPharmacyFromDemographic(demographicNo);
    String prefPharmacy = "";
    if (pharmacyList != null && !pharmacyList.isEmpty()) {
        prefPharmacy = pharmacyList.get(0).getName();
    }
%>
<html>
    <head>
        <script type="text/javascript" src="<%= request.getContextPath()%>/js/global.js"></script>
        <title>Print Drug Profile</title>
        <link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/oscarRx/styles.css">

        <base href="<%= request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/" %>">

        <link rel="stylesheet" type="text/css" media="all" href="<%= request.getContextPath() %>/share/css/extractedFromPages.css"/>

    </head>

    <%
        boolean showall = false;

        if (request.getParameter("show") != null) {
            if (request.getParameter("show").equals("all")) {
                showall = true;
            }
        }
    %>

    <body topmargin="0" leftmargin="0" vlink="#0000FF">
    <table border="0" cellpadding="0" cellspacing="0"
           style="border-collapse: collapse" bordercolor="#111111" width="100%"
           id="AutoNumber1" height="100%">
        <tr>
            <td width="100%" height="100%" valign="top"><!--Column Two Row Two-->
                <table cellpadding="0" cellspacing="2"
                       style="border-collapse: collapse" bordercolor="#111111" width="100%"
                       height="100%">
                    <!----Start new rows here-->
                    <tr>
                        <td align="right"></td>
                    </tr>
                    <tr>
                        <td>
                            <div class="DivContentSectionHead"><fmt:setBundle basename="oscarResources"/><fmt:message key="SearchDrug.section1Title"/></div>
                        </td>
                    </tr>
                    <tr>
                        <td><!-- <b><fmt:setBundle basename="oscarResources"/><fmt:message key="SearchDrug.nameText"/></b>-->
                            <c:out value="${patient.surname}"/>,
                            <c:out value="${patient.firstName}"/>
                            <br/>

                            <c:out value="${patient.address}"/>
                            <br/>
                            <c:out value="${patient.city}"/>, <c:out value="${patient.province}"/> <c:out
                                    value="${patient.postal}"/>
                            <br/>
                            <br/>


                        </td>
                    </tr>
                    <tr>
                        <td>
                            <b><fmt:setBundle basename="oscarResources"/><fmt:message key="SearchDrug.ageText"/></b> <c:out value="${patient.age}"/>
                            <b>Gender:</b> <c:out value="${patient.sex}"/> <b>HC:</b> <c:out value="${patient.hin}"/>
                            <br/>
                            <b>User:</b> <%=userlastname%>, <%=userfirstname %><br/>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <table cellspacing="0" width="100%" cellpadding="0">
                                <tr>
                                    <td>
                                        <div class="DivContentSectionHead"><fmt:setBundle basename="oscarResources"/><fmt:message key="SearchDrug.section2Title"/></div>
                                    </td>
                                    <td align="right" class="noPrint">
                                        <div class="DivContentSectionHead">
                                            <% if (showall) { %>
                                            <a href="oscarRx/PrintDrugProfile.jsp">Show Current</a>
                                            <% } else { %>
                                            <a href="oscarRx/PrintDrugProfile.jsp?show=all">Show All</a>
                                            <% } %>
                                            | <a href="javascript:void(0);window.print();">Print</a>
                                        </div>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <table>
                                <tr>
                                    <td width="100%"><!--<div class="Step1Text" style="width:100%">-->
                                        <table width="100%" cellpadding="3">
                                            <tr>
                                                <th align=left width=20%><b><fmt:setBundle basename="oscarResources"/><fmt:message key="SearchDrug.msgRxDate"/></b></th>
                                                <th align=left width=100%><b><fmt:setBundle basename="oscarResources"/><fmt:message key="SearchDrug.msgPrescription"/></b></th>
                                            </tr>

                                            <%

                                                DrugDao drugDao = (DrugDao) SpringUtils.getBean(DrugDao.class);
                                                List<Drug> prescriptDrugs = drugDao.getPrescriptions("" + patient.getDemographicNo(), showall);


                                                for (Drug drug : prescriptDrugs) {
                                                    String styleColor = "";
                                            %>
                                            <tr>
                                                <td width=20% valign="top">
                                                    <a <%= styleColor%>
                                                            href="oscarRx/StaticScript2.jsp?regionalIdentifier=<%=drug.getRegionalIdentifier()%>&cn=<%=response.encodeURL(drug.getCustomName())%>&bn=<%=response.encodeURL(drug.getBrandName())%>"><%=drug.getRxDate()%>
                                                    </a>
                                                </td>
                                                <td width=100%>
                                                    <a <%= styleColor%>
                                                            href="oscarRx/StaticScript2.jsp?regionalIdentifier=<%= drug.getRegionalIdentifier()%>&cn=<%= response.encodeURL(drug.getCustomName())%>&bn=<%=response.encodeURL(drug.getBrandName())%>"><%= drug.getFullOutLine().replaceAll(";", " ")%>
                                                    </a>
                                                </td>
                                            </tr>
                                            <%
                                                }
                                            %>
                                        </table>

                                        </div>

                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>

                    <tr>
                        <td align="right" class="noPrint">
                            <div class="DivContentSectionHead">

                                <% if (showall) { %>
                                <a href="oscarRx/PrintDrugProfile.jsp">Show Current</a>
                                <% } else { %>
                                <a href="oscarRx/PrintDrugProfile.jsp?show=all">Show All</a>
                                <% } %>
                                | <a href="javascript:void(0);window.print();">Print</a>
                            </div>
                        </td>
                    </tr>


                    <!----End new rows here-->
                    <tr height="100%">
                        <td></td>
                    </tr>
                </table>
            </td>
        </tr>


    </table>


    </body>
</html>
