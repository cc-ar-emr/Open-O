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

<html>
    <head>
        <script type="text/javascript" src="<%= request.getContextPath() %>/js/global.js"></script>
        <title>Oscar Forms</title>
        <c:set var="ctx" value="${pageContext.request.contextPath}" scope="request"/>

        <link rel="stylesheet" href="<c:out value="${ctx}"/>/css/execute.css" type="text/css">
        <script>
            function clickTab(name) {
                if (!validateForm(document.surveyExecuteForm, document.surveyExecuteForm.elements['view.tab'].value, false)) {
                    return;
                }
                document.surveyExecuteForm.elements['view.tab'].value = name;
                document.surveyExecuteForm.method.value = 'refresh';
                document.surveyExecuteForm.submit();
            }

            function init() {
                setInterval("autoSave()", 300000);
                /*5minutes*/
            }

            function autoSave() {
                document.surveyExecuteForm.elements['view.tab'].value = name;
                document.surveyExecuteForm.method.value = 'tmpsave_survey';
                document.surveyExecuteForm.submit();
            }

            function init1() {
                setTimer();
                window.opener.location.reload(true);
            }

            function setTimer() {
                setTimeout("autoSave()", 300000);
                <%--5minutes --%>
            }

            function autoSave1() {

                setTimer();
            }
        </script>
        <script type="text/javascript" src="<c:out value="${ctx}"/>/jsCalendar/calendar.js"></script>
        <script type="text/javascript" src="<c:out value="${ctx}"/>/jsCalendar/lang/calendar-en.js"></script>
        <script type="text/javascript" src="<c:out value="${ctx}"/>/jsCalendar/calendar-setup.js"></script>
        <c:if test="${not empty sessionScope.validation_file}">
            <script type="text/javascript"
                    src="<c:out value="${ctx}"/>/survey/scripts/<c:out value="${sessionScope.validation_file}"/>.js"></script>
        </c:if>
        <link rel="stylesheet" type="text/css" href="<c:out value="${ctx}"/>/jsCalendar/skins/aqua/theme.css">

        <c:if test="${empty sessionScope.validation_file}">
            <script>
                function validateForm(form, tab, submit) {
                    return true;
                }
            </script>
        </c:if>

    </head>

    <body onload="init()">

    <%@ include file="/common/messages.jsp" %>
    <form action="${pageContext.request.contextPath}/PMmodule/Forms/SurveyExecute.do" method="post"
               onsubmit="return validateForm(this,document.surveyExecuteForm.elements['view.tab'].value,true);">
        <input type="hidden" name="tab" id="tab"/>
        <input type="hidden" name="id" id="id"/>
        <input type="hidden" name="admissionId" id="admissionId"/>
        <input type="hidden" name="method" value="save_survey"/>
        <input type="hidden" name="type" value="<c:out value="${type}"/>"/>

        <br/>

        <div class="tabs" id="tabs">
            <table cellpadding="0" cellspacing="0" border="0">
                <tr>

                    <c:forEach var="tab" items="${tabs}">
                        <c:choose>
                            <c:when test="${tab eq currentTab}">
                                <td style="background-color: #555;"><c:out value="${tab}"/></td>
                            </c:when>
                            <c:otherwise>
                                <td><a style="" href="javascript:void(0)"
                                       onclick="javascript:clickTab('<c:out value="${tab}"/>');return false;"><c:out
                                        value="${tab}"/></a></td>
                            </c:otherwise>
                        </c:choose>
                    </c:forEach>
                </tr>
            </table>
        </div>

        <c:if test="${requestScope.introduction != null}">
            <p>
            <pre>
				<c:out value="${introduction.text }"/>
			</pre>
            </p>
        </c:if>

        <br/>
        <table border="0" width="100%" cellspacing="2" cellpadding="2">
            <c:forEach var="qcontainer" items="${page.QContainerArray}">
                <tr>
                    <c:choose>
                        <c:when test="${qcontainer.question != null}">
                            <c:set var="sectionId" value="0" scope="request"/>
                            <c:set var="question" value="${qcontainer.question}" scope="request"/>
                            <jsp:include page="question.jsp"/>
                        </c:when>
                        <c:otherwise>
                            <c:set var="section" value="${qcontainer.section}" scope="request"/>
                            <jsp:include page="section.jsp"/>
                        </c:otherwise>
                    </c:choose>
                </tr>
            </c:forEach>
        </table>
        <br/>
        <c:if test="${requestScope.closing != null}">
            <p>
            <pre>
				<c:out value="${closing.text }"/>
			</pre>
            </p>
        </c:if>
        <br/>
        <table width="50%">
            <tr>
                <td colspan="2">
                    <input type="submit" value="Temporary Save"
                           onclick="this.form.method.value='tmpsave_survey'; return true;"/>
                    <input type="submit" value="Save" />
                    <button type="button" onclick="window.history.back();">Cancel</button>

                </td>
            </tr>
        </table>

    </form>
    </body>
</html>
