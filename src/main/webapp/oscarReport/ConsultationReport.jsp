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

<%@ page import="java.util.*,oscar.oscarReport.data.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>


<link rel="stylesheet" type="text/css"
      href="<%= request.getContextPath() %>/oscarEncounter/encounterStyles.css">
<%

    String curUser_no, userfirstname, userlastname;
    curUser_no = (String) session.getAttribute("user");


    String mons = "1";
    String pros = curUser_no;
    if (request.getParameter("numMonth") != null) {
        mons = request.getParameter("numMonth");
    }

    if (request.getParameter("proNo") != null) {
        pros = request.getParameter("proNo");
    }

    oscar.oscarReport.data.RptConsultReportData conData = new oscar.oscarReport.data.RptConsultReportData();
    conData.consultReportGenerate(pros, mons);
    ArrayList proList = conData.providerList();
%>
<%!
    String selled(String i, String mons) {
        String retval = "";
        if (i.equals(mons)) {
            retval = "selected";
        }
        return retval;
    }
%>


<html>
    <head>
        <script type="text/javascript" src="<%= request.getContextPath() %>/js/global.js"></script>
        <title><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarReport.ConsultationReport.title"/>
            <%= mons %>
        </title>
        <link rel="stylesheet" type="text/css" media="all" href="<%= request.getContextPath() %>/share/css/extractedFromPages.css"/>

        <script type="text/javascript">
            var remote = null;

            function rs(n, u, w, h, x) {
                args = "width=" + w + ",height=" + h + ",resizable=yes,scrollbars=yes,status=0,top=60,left=30";
                remote = window.open(u, n, args);
                // if (remote != null) {
                //    if (remote.opener == null)
                //        remote.opener = self;
                // }
                // if (x == 1) { return remote; }
            }

            function popupOscarConsultationConfig(vheight, vwidth, varpage) { //open a new popup window
                var page = varpage;
                windowprops = "height=" + vheight + ",width=" + vwidth + ",location=no,scrollbars=yes,menubars=no,toolbars=no,resizable=yes,screenX=0,screenY=0,top=0,left=0";
                var popup = window.open(varpage, "OscarConsultationConfig", windowprops);
                if (popup != null) {
                    if (popup.opener == null) {
                        popup.opener = self;
                    }
                }
            }
        </script>

    </head>

    <body class="BodyStyle" vlink="#0000FF">
    <!--  -->
    <table class="MainTable" id="scrollNumber1" name="encounterTable">
        <tr class="MainTableTopRow">
            <td class="MainTableTopRowLeftColumn"><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarReport.ConsultationReport.msgReport"/></td>
            <td class="MainTableTopRowRightColumn">
                <table class="TopStatusBar">
                    <form action="ConsultationReport.jsp">
                        <tr>
                            <td><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarReport.ConsultationReport.msgTitle"/></td>
                            <td><select name="numMonth">
                                <option value="1" <%=selled("1", mons)%>>1 <fmt:setBundle basename="oscarResources"/><fmt:message key="oscarReport.ConsultationReport.formMonth"/></option>
                                <option value="2" <%=selled("2", mons)%>>2 <fmt:setBundle basename="oscarResources"/><fmt:message key="oscarReport.ConsultationReport.formMonths"/></option>
                                <option value="3" <%=selled("3", mons)%>>3 <fmt:setBundle basename="oscarResources"/><fmt:message key="oscarReport.ConsultationReport.formMonths"/></option>
                                <option value="4" <%=selled("4", mons)%>>4 <fmt:setBundle basename="oscarResources"/><fmt:message key="oscarReport.ConsultationReport.formMonths"/></option>
                                <option value="5" <%=selled("5", mons)%>>5 <fmt:setBundle basename="oscarResources"/><fmt:message key="oscarReport.ConsultationReport.formMonths"/></option>
                                <option value="6" <%=selled("6", mons)%>>6 <fmt:setBundle basename="oscarResources"/><fmt:message key="oscarReport.ConsultationReport.formMonths"/></option>
                                <option value="7" <%=selled("7", mons)%>>7 <fmt:setBundle basename="oscarResources"/><fmt:message key="oscarReport.ConsultationReport.formMonths"/></option>
                                <option value="8" <%=selled("8", mons)%>>8 <fmt:setBundle basename="oscarResources"/><fmt:message key="oscarReport.ConsultationReport.formMonths"/></option>
                                <option value="9" <%=selled("9", mons)%>>9 <fmt:setBundle basename="oscarResources"/><fmt:message key="oscarReport.ConsultationReport.formMonths"/></option>
                                <option value="10" <%=selled("10", mons)%>>10 <fmt:setBundle basename="oscarResources"/><fmt:message key="oscarReport.ConsultationReport.formMonths"/></option>
                                <option value="11" <%=selled("11", mons)%>>11 <fmt:setBundle basename="oscarResources"/><fmt:message key="oscarReport.ConsultationReport.formMonths"/></option>
                                <option value="12" <%=selled("12", mons)%>>12 <fmt:setBundle basename="oscarResources"/><fmt:message key="oscarReport.ConsultationReport.formMonths"/></option>
                            </select> <select name="proNo">
                                <option value="-1" <%=selled("-1", pros)%>><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarReport.ConsultationReport.formAllProviders"/></option>
                                <%
                                    for (int i = 0; i < proList.size(); i++) {
                                        ArrayList w = (ArrayList) proList.get(i);
                                        String proNum = (String) w.get(0);
                                        String proName = (String) w.get(1);
                                %>
                                <option value="<%=proNum%>" <%=selled(proNum, pros)%>><%=proName%>
                                </option>
                                <%
                                    }
                                %>
                            </select> <input type=submit
                                             value="<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarReport.ConsultationReport.btnUpdateReport"/>"/>
                            </td>
                            <td style="text-align: right"><a
                                    href="javascript:popupStart(300,400,'About.jsp')"><fmt:setBundle basename="oscarResources"/><fmt:message key="global.about"/></a> | <a
                                    href="javascript:popupStart(300,400,'License.jsp')"><fmt:setBundle basename="oscarResources"/><fmt:message key="global.license"/></a></td>
                        </tr>
                    </form>
                </table>
            </td>
        </tr>
        <tr>
            <td class="MainTableLeftColumn">&nbsp;</td>
            <td class="MainTableRightColumn">
                <table border=0 cellspacing=4 width=900>
                    <%
                        oscar.oscarReport.data.RptConsultReportData.DemoConsultDataStruct demoData;
                        for (int i = 0; i < conData.demoList.size(); i++) {
                            demoData = (RptConsultReportData.DemoConsultDataStruct) conData.demoList.get(i);
                    %>
                    <tr>
                        <td bgcolor="#eeeeff" width=900>
                            <table border=0 cellspacing=2>
                                <tr>
                                    <td class="nameBox" colspan=2>Patient Name: <%= demoData.getDemographicName()%>
                                    </td>
                                </tr>
                                <tr>

                                    <td valign=top width=600 class=sideLine>

                                        <table border=0 cellspacing=3>
                                            <td colspan=4 class=nameBox>Consultations</td>
                                            <tr>
                                                <th width=100 class="subTitles" align=left>Referal Date</th>
                                                <th width=120 class="subTitles" align=left>Service</th>
                                                <th width=280 class="subTitles" align=left>Specialist</th>
                                                <th width=100 class="subTitles" align=left>App. Date</th>
                                            </tr>
                                            <%
                                                RptConsultReportData.DemoConsultDataStruct.Consult demoCon;
                                                java.util.ArrayList conL = demoData.getConsults();
                                                for (int j = 0; j < conL.size(); j++) {
                                                    demoCon = (RptConsultReportData.DemoConsultDataStruct.Consult) conL.get(j);
                                            %>
                                            <tr>
                                                <td class="fieldBox" bgcolor="#ddddff"><a
                                                        href="javascript:popupOscarConsultationConfig(700,960,'ShowCo
nsult.do?requestId=<%=demoCon.requestId%>')"><%=demoCon.referalDate%>
                                                </td>
                                                </a>
                                                <td class="fieldBox"
                                                    bgcolor="#ddddff"><%=demoCon.getService(demoCon.serviceId)%>
                                                </td>
                                                <td class="fieldBox"
                                                    bgcolor="#ddddff"><%=demoCon.getSpecialist(demoCon.specialist)%>
                                                </td>
                                                <td class="fieldBox" bgcolor="#ddddff"><%=demoCon.appDate %>
                                                </td>
                                            </tr>
                                            <%
                                                }
                                            %>
                                        </table>
                                    </td>
                                    <td valign=top width=300>
                                        <table border=0 cellspacing=3>
                                            <tr>
                                                <td class=nameBox colspan=2><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarReport.ConsultationReport.msgConsDoc"/></td>
                                            </tr>
                                            <tr>
                                                <th width=200 class="subTitles" align=left><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarReport.ConsultationReport.msgDocDesc"/></th>
                                                <th width=100 class="subTitles" align=left><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarReport.ConsultationReport.msgDate"/></th>
                                            </tr>
                                            <%
                                                RptConsultReportData.DemoConsultDataStruct.ConLetter demoLetter;
                                                java.util.ArrayList letL = demoData.getConReplys();
                                                for (int j = 0; j < letL.size(); j++) {
                                                    demoLetter = (RptConsultReportData.DemoConsultDataStruct.ConLetter) letL.get(j);

                                            %>
                                            <tr>
                                                <td class="fieldBox" bgcolor="#deddff"><a href=#
                                                                                          onclick="javascript:rs('new','../documentManager/documentGetFile.jsp?document=<%=demoLetter.docfileName%>&type=active&doc_no=<%=demoLetter.document_no%>', 480,480,1)"><%=demoLetter.docdesc%>
                                                </a>
                                                </td>
                                                <td class="fieldBox"
                                                    bgcolor="#deddff"><%=demoLetter.docDate.toString()%>
                                                </td>
                                            </tr>
                                            <%
                                                }
                                            %>
                                        </table>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    <%
                        }
                    %>
                </table>

            </td>
        </tr>
        <tr>
            <td class="MainTableBottomRowLeftColumn"></td>
            <td class="MainTableBottomRowRightColumn"></td>
        </tr>
    </table>
    </body>
</html>
