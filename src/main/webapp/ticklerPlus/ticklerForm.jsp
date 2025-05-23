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

<%@ taglib uri="/WEB-INF/security.tld" prefix="security" %>
<%
    String roleName2$ = (String) session.getAttribute("userrole") + "," + (String) session.getAttribute("user");
    boolean authed = true;
%>
<security:oscarSec roleName="<%=roleName2$%>" objectName="_tickler" rights="r" reverse="<%=true%>">
    <%authed = false; %>
    <%response.sendRedirect("../securityError.jsp?type=_tickler");%>
</security:oscarSec>
<%
    if (!authed) {
        return;
    }
%>

<%-- Updated by Eugene Petruhin on 18 dec 2008 while fixing #2422864 & #2317933 & #2379840 --%>
<%-- Updated by Eugene Petruhin on 20 feb 2009 while fixing check_date() error --%>

<%@include file="/ticklerPlus/header.jsp" %>

<%@page import="java.util.GregorianCalendar" %>
<%@page import="java.util.Calendar" %>

<%
    GregorianCalendar now = new GregorianCalendar();

    int curYear = now.get(Calendar.YEAR);
    int curMonth = now.get(Calendar.MONTH);
    int curDay = now.get(Calendar.DAY_OF_MONTH);
    int curHour = now.get(Calendar.HOUR);
    int curMinute = now.get(Calendar.MINUTE);

    boolean curAm = (now.get(Calendar.HOUR_OF_DAY) <= 12) ? true : false;
%>
<script type="text/javascript" src="<%= request.getContextPath() %>/js/checkDate.js"></script>
<script type="text/javascript">
    function check_tickler_service_date() {
        return check_date('tickler.serviceDateWeb');
    }

    <%--
            function search_demographic() {
                var popup = window.open('<c:out value="${ctx}"/>/ticklerPlus/demographicSearch.jsp?query=' + document.ticklerForm.elements['tickler.demographic_webName'].value,'demographic_search');

                if (popup != null) {
                        if (popup.opener == null) {
                          popup.opener = self;
                        }
                        popup.focus();
                      }
            }

            function search_provider() {
                url = '<c:out value="${ctx}"/>/provider/receptionistfindprovider.jsp';
                url += '?caisi=true&pyear=<%=curYear%>&pmonth=<%=curMonth%>&pday=<%=curDay%>&providername=';
                var popup = window.open(url + document.ticklerForm.elements['tickler.taskAssignedToName'].value,'provider_search');
            }
    --%>

    function validateTicklerForm(form) {
        if (form.elements['tickler.taskAssignedTo'].value == 'none') {
            alert('You must assign the task to a valid provider');
            return false;
        }

        if (form.elements['tickler.serviceDateWeb'].value == '') {
            alert('You must provide a valid service date');
            return false;
        }

        if (form.elements['tickler.message'].value == '') {
            alert('You must provide a message');
            return false;
        }

        return check_tickler_service_date();
    }
</script>

<tr>
    <td class="searchTitle" colspan="4">Create New Tickler</td>
</tr>
</table>

<%
    String demographicName = request.getParameter("tickler.demographic_webName");
    if (demographicName == null || "undefined".equals(demographicName)) {
        demographicName = (String) request.getAttribute("demographicName");
    }
%>
<%@ include file="/common/messages.jsp" %>

<table width="60%" border="0" cellpadding="0" cellspacing="1" bgcolor="#C0C0C0">
    <form action="${pageContext.request.contextPath}/Tickler.do" method="post" focus="tickler.demographicNo" onsubmit="return validateTicklerForm(this);">

        <input type="hidden" name="method" value="save"/>
        <input type="hidden" name="creator" id="creator" value='<%=(String) session.getAttribute("user")%>'/>
        <input type="hidden" name="id" id="id"/>

        <tr>
            <td class="fieldTitle">
                Demographic:
            </td>
            <td class="fieldValue">
                <input type="hidden" name="demographicNo" id="demographicNo"/>
                <%=demographicName%>
            </td>
        </tr>
        <tr>
            <td class="fieldTitle">Program:</td>
            <td class="fieldValue"><c:out value="${requestScope.program_name}"/></td>
        </tr>
        <tr>
            <td class="fieldTitle">Service Date:</td>
            <%
                Calendar rightNow = Calendar.getInstance();
                int year = rightNow.get(Calendar.YEAR);
                int month = rightNow.get(Calendar.MONTH) + 1;
                int day = rightNow.get(Calendar.DAY_OF_MONTH);
                String formattedDate = year + "-" + month + "-" + day;
            %>
            <td class="fieldValue">
                <input type="text" name="tickler.serviceDateWeb" value="<%=formattedDate%>" maxlength="10"/>
                <span onClick="openBrWindow('calendar/oscarCalendarPopup.jsp?type=caisi&openerForm=ticklerForm&amp;openerElement=tickler.serviceDateWeb&amp;year=<%=year%>&amp;month=<%=month%>','','width=300,height=300')">
					<img border="0" src="images/calendar.jpg"/>
				</span>
            </td>
        </tr>
        <tr>
            <td class="fieldTitle">
                Service Time:
            </td>
            <td class="fieldValue">
                <select name="tickler.service_hour">
                    <%
                        for (int x = 1; x < 13; x++) {
                            String selected = "";

                            if (curHour == x) {
                                selected = "selected";
                            }
                    %>
                    <option value="<%=x%>" <%=selected%>><%=x%>
                    </option>
                    <%
                        }
                    %>
                </select> : <select name="tickler.service_minute">
                <%
                    for (int x = 0; x < 60; x += 15) {
                        String selected = "";

                        if (curMinute >= x && curMinute < (x + 15)) {
                            selected = "selected";
                        }

                        String val = String.valueOf(x);

                        if (val.equals("0")) {
                            val = "00";
                        }
                %>
                <option value="<%=val%>" <%=selected %>><%=val%>
                </option>
                <%
                    }
                %>
            </select> &nbsp; <select name="tickler.service_ampm">
                <option value="AM">AM</option>
                <%
                    if (!curAm) {
                %>
                <option value="PM" selected>PM</option>
                <%
                } else {
                %>
                <option value="PM">PM</option>
                <%
                    }
                %>
            </select>
            </td>
        </tr>
        <tr>
            <td class="fieldTitle">
                Priority:
            </td>
            <td class="fieldValue">
                <select name="tickler.priorityWeb" id="tickler.priorityWeb">
                    <option value="Normal">Normal</option>
                    <option value="High">High</option>
                    <option value="Low">Low</option>
                </select>
            </td>
        </tr>
        <tr>
            <td class="fieldTitle">
                Task Assigned To:
            </td>
            <td class="fieldValue">
                <input type="hidden" name="taskAssignedToName" id="taskAssignedToName"/>
                <select name="taskAssignedTo" value="none">
                    <option value="none">- select -</option>
                    <c:forEach var="provider" items="${providers}">
                        <option value="${provider.providerNo}">
                                ${provider.formattedName}
                        </option>
                    </c:forEach>
                </select>
                    <%--input type="hidden" name= property="tickler.taskAssignedTo" />
                    <input type="text" name="tickler.taskAssignedToName" id="tickler.taskAssignedToName" />
                    <input type="button" value="Search" onclick="search_provider();" /--%>
            </td>
        </tr>
        <tr>
            <td class="fieldTitle">
                Status:
            </td>
            <td class="fieldValue">
                <select name="tickler.statusWeb" id="tickler.statusWeb">
                    <option value="A">Active</option>
                    <option value="C">Completed</option>
                    <option value="D">Deleted</option>
                </select>
            </td>
        </tr>
        <tr>
            <td class="fieldTitle">
                Message:
            </td>
            <td class="fieldValue">
                <textarea cols="40" rows="10" name="message"></textarea>
            </td>
        </tr>
        <!--
        <tr>
        <td class="fieldTitle">
        Post to eChart:
        </td>
        <td class="fieldValue">
        <input name="echart" value="true" type="checkbox" />
        </td>
        </tr>
        -->
        <tr>
            <td class="fieldValue" colspan="3" align="left">
                <input type="hidden" name="docType" value="<%=request.getParameter("docType")%>"/>
                <input type="hidden" name="docId" value="<%=request.getParameter("docId")%>"/>
                <input type="submit" class="button" value="Save" />
                <input type="button" value="Cancel" onclick="window.close()"/>
            </td>
        </tr>
    </form>
</table>

<c:if test="${requestScope.from ne 'CaseMgmt'}">
    </body>
    </html>
</c:if>
