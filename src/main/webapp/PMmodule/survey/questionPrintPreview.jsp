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
<%@ page import="java.util.*" %>
<%@ page import="org.oscarehr.survey.web.formbean.SurveyExecuteDataBean" %>
<%@ page import="org.oscarehr.surveymodel.*" %>
<c:set var="id" scope="page">
    <c:out value="${question.id}"/>
</c:set>
<%
    //This little piece of code is needed to expose the value-map from the formbean
    //to the scriptlets - too bad html doesn't support the id attribute.
%>
<script>
    function select_checkbox(name) {
        var element = document.surveyExecuteForm.elements['data.value(' + name + ')'];
        var hiddenElement = document.surveyExecuteForm.elements['data.value(checkbox_' + name + ')'];
        if (element.checked == true) {
            hiddenElement.value = "checked";
        } else {
            hiddenElement.value = "";
        }
    }

    function select_program(ctl) {
        var programId = ctl.options[ctl.selectedIndex].value;
        document.surveyExecuteForm.elements['view.admissionId'].value = programId;
        document.surveyExecuteForm.method.value = 'refresh';
        document.surveyExecuteForm.submit();
    }
</script>
<td>
    <table>
        <tR>
            <c:choose>

                <c:when test="${question.type.openEnded != null}">
                    <c:choose>
                        <c:when test="${question.type.openEnded.rows == 1}">
                            <td>
                                <c:if test="${question.bold eq 'true'}">
                                <b>
                                    </c:if>
                                    <c:if test="${question.underline eq 'true'}">
                                    <u>
                                        </c:if>
                                        <c:if test="${question.italics eq 'true'}">
                                        <i>
                                            </c:if>
                                                <c:out value="${question.description}"/>
                                            <c:if
                                                    test="${question.bold eq 'true'}">
                                </b>
                                </c:if> <c:if test="${question.underline eq 'true'}">
                                </u>
                            </c:if> <c:if test="${question.italics eq 'true'}">
                                </i>
                            </c:if></td>
                            <td><input type="text"
                                    name="data.value(${pageNumber}_${sectionId}_${question.id})"
                                    size="90"/></td>
                        </c:when>
                        <c:otherwise>
                            <td colspan="2">
                                <c:if test="${question.bold eq 'true'}">
                                <b>
                                    </c:if>
                                    <c:if test="${question.underline eq 'true'}">
                                    <u>
                                        </c:if>
                                        <c:if test="${question.italics eq 'true'}">
                                        <i>
                                            </c:if>
                                                <c:out value="${question.description}"/>
                                            <c:if
                                                    test="${question.bold eq 'true'}">
                                </b>
                                </c:if> <c:if test="${question.underline eq 'true'}">
                                </u>
                            </c:if> <c:if test="${question.italics eq 'true'}">
                                </i>
                            </c:if> <br/>
                                <textarea
                                        name="data.value(${pageNumber}_${sectionId}_${question.id})"
                                        rows="5" cols="90"></textarea></td>
                        </c:otherwise>
                    </c:choose>
                </c:when>

                <c:when test="${question.type.date != null}">
                    <!-- calendar -->
                    <td>
                        <c:if test="${question.bold eq 'true'}">
                        <b>
                            </c:if>
                            <c:if test="${question.underline eq 'true'}">
                            <u>
                                </c:if>
                                <c:if test="${question.italics eq 'true'}">
                                <i>
                                    </c:if>
                                    <c:choose>
                                    <c:when test="${not empty question.color}">
                                    <span style="color:<c:out value="${question.color}"/>"><c:out
                                            value="${question.description}"/></span>
                                    </c:when>
                                    <c:otherwise>
                                        <c:out value="${question.description}"/>
                                    </c:otherwise>
                                    </c:choose>
                                    <c:if test="${question.bold eq 'true'}">
                        </b>
                        </c:if> <c:if test="${question.underline eq 'true'}">
                        </u>
                    </c:if> <c:if test="${question.italics eq 'true'}">
                        </i>
                    </c:if> &nbsp;(<c:out value="${question.type.date}"/>)&nbsp;
                    </td>
                    <td><input type="text"
                            name="data.value(${pageNumber}_${sectionId}_${question.id})"
                            styleId="f_date_c"/> &nbsp;
                    </td>
                </c:when>

                <c:when test="${question.type.select != null}">
                    <td valign="top">
                        <c:if test="${question.bold eq 'true'}">
                        <b>
                            </c:if>
                            <c:if test="${question.underline eq 'true'}">
                            <u>
                                </c:if>
                                <c:if test="${question.italics eq 'true'}">
                                <i>
                                    </c:if>
                                        <c:out value="${question.description}"/>
                                    <c:if
                                            test="${question.bold eq 'true'}">
                        </b>
                        </c:if> <c:if test="${question.underline eq 'true'}">
                        </u>
                    </c:if> <c:if test="${question.italics eq 'true'}">
                        </i>
                    </c:if></td>
                    <td><c:choose>
                        <c:when test="${question.type.select.renderType eq 'select'}">

                            <c:choose>
                                <c:when test="${question.caisiObject eq 'Program Selector'}">
                                    <select
                                            name="data.value(${pageNumber}_${sectionId}_${question.id})"
                                            onchange="select_program(this);">
                                        <option value="0">&nbsp;</option>
                                        <c:forEach var="admission" items="${admissions}">
                                            <option value="${admission.id}">
                                                <c:out value="${admission.programName}"/>
                                            </option>
                                        </c:forEach>
                                    </select>
                                </c:when>
                                <c:otherwise>
                                    <select
                                            name="data.value(${pageNumber}_${sectionId}_${question.id})">
                                        <c:forEach var="answer"
                                                   items="${question.type.select.possibleAnswers.answerArray}">
                                            <option value="${answer}">
                                                <c:out value="${answer}"/>
                                            </option>
                                        </c:forEach>
                                    </select>
                                </c:otherwise>
                            </c:choose>

                        </c:when>
                        <c:when test="${question.type.select.renderType eq 'radio'}">
                            <table width="100%">
                                <c:choose>
                                    <c:when
                                            test="${question.type.select.orientation eq 'horizontal'}">
                                        <tr>
                                            <c:forEach var="answer"
                                                       items="${question.type.select.possibleAnswers.answerArray}">
                                                <td><input type="radio"
                                                        name="data.value(${pageNumber}_${sectionId}_${question.id})"
                                                        value="${answer}"/>&nbsp;<c:out value="${answer}"/></td>
                                            </c:forEach>
                                            <c:if test="${question.type.select.otherAnswer eq true}">
                                                <td><input type="radio"
                                                        name="data.value(${pageNumber}_${sectionId}_${question.id})"
                                                        value="other"/>&nbsp;Other&nbsp;<input type="text"
                                                        name="data.value(${pageNumber}_${sectionId}_${question.id}_other_value)"/></td>
                                            </c:if>
                                        </tr>
                                    </c:when>
                                    <c:otherwise>
                                        <c:forEach var="answer"
                                                   items="${question.type.select.possibleAnswers.answerArray}">
                                            <tr>
                                                <td><input type="radio"
                                                        name="data.value(${pageNumber}_${sectionId}_${question.id})"
                                                        value="${answer}"/>&nbsp;<c:out value="${answer}"/></td>
                                            </tr>
                                        </c:forEach>
                                        <c:if test="${question.type.select.otherAnswer eq true}">
                                            <tr>
                                                <td><input type="radio"
                                                        name="data.value(${pageNumber}_${sectionId}_${question.id})"
                                                        value="other"/>&nbsp;Other&nbsp;<input type="text"
                                                        name="data.value(${pageNumber}_${sectionId}_${question.id}_other_value)"/></td>
                                            </tr>
                                        </c:if>
                                    </c:otherwise>
                                </c:choose>
                            </table>
                        </c:when>
                        <c:when test="${question.type.select.renderType eq 'checkbox'}">
                            <table width="100%">
                                <c:choose>
                                    <c:when
                                            test="${question.type.select.orientation eq 'horizontal'}">
                                        <tr>
                                        <c:forEach var="answer"
                                                   items="${question.type.select.possibleAnswers.answerArray}">
                                            <td><input type="hidden"
                                                    name="data.value(checkbox_${pageNumber}_${sectionId}_${question.id}_${answer})"/>
                                                <input type="checkbox"
                                                        name="data.value(${pageNumber}_${sectionId}_${question.id}_${answer})"
                                                        value="${answer}"
                                                        onclick="select_checkbox('${pageNumber}_${sectionId}_${question.id}_${answer}')"/>&nbsp;<c:out
                                                        value="${answer}"/></td>
                                        </c:forEach>

                                        <c:if test="${question.type.select.otherAnswer eq true}">
                                            <td><input type="hidden"
                                                    name="data.value(checkbox_${pageNumber}_${sectionId}_${question.id}_other)"/>
                                                <input type="checkbox"
                                                        name="data.value(${pageNumber}_${sectionId}_${question.id}_other)"
                                                        value="other"
                                                        onclick="select_checkbox('${pageNumber}_${sectionId}_${question.id}_other')"/>&nbsp;Other&nbsp;<input type="text"
                                                        name="data.value(${pageNumber}_${sectionId}_${question.id}_other_value)"/>
                                            </td>
                                        </c:if>
                                    </c:when>
                                    <c:otherwise>
                                        <c:forEach var="answer"
                                                   items="${question.type.select.possibleAnswers.answerArray}">
                                            <tr>
                                                <td><input type="hidden"
                                                    name="data.value(checkbox_${pageNumber}_${sectionId}_${question.id}_${answer})"/>
                                                    <input type="checkbox"
                                                            name="data.value(${pageNumber}_${sectionId}_${question.id}_${answer})"
                                                            value="${answer}"
                                                            onclick="select_checkbox('${pageNumber}_${sectionId}_${question.id}_${answer}')"/>&nbsp;<c:out
                                                            value="${answer}"/></td>
                                            </tr>
                                        </c:forEach>

                                        <c:if test="${question.type.select.otherAnswer eq true}">
                                            <tr>
                                                <td><input type="hidden"
                                                    name="data.value(checkbox_${pageNumber}_${sectionId}_${question.id}_other)"/>
                                                    <input type="checkbox"
                                                            name="data.value(${pageNumber}_${sectionId}_${question.id}_other)"
                                                            value="other"
                                                            onclick="select_checkbox('${pageNumber}_${sectionId}_${question.id}_other')"/>&nbsp;Other&nbsp;<input type="text"
                                                            name="data.value(${pageNumber}_${sectionId}_${question.id}_other_value)"/>
                                                </td>
                                            </tr>
                                        </c:if>
                                    </c:otherwise>
                                </c:choose>
                            </table>
                        </c:when>
                    </c:choose></td>
                </c:when>


            </c:choose>
        </tr>
    </table>
</td>
