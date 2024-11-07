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
    String roleName2$ = session.getAttribute("userrole") + "," + session.getAttribute("user");
    boolean authed = true;
%>
<security:oscarSec roleName="<%=roleName2$%>" objectName="_allergy" rights="r" reverse="<%=true%>">
    <%authed = false; %>
    <%response.sendRedirect("../securityError.jsp?type=_allergy");%>
</security:oscarSec>
<%
    if (!authed) {
        return;
    }
%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@page import="java.util.*" %>
<%@page import="org.oscarehr.common.model.Allergy" %>
<html:html lang="en">
    <head>
        <script type="text/javascript" src="<%= request.getContextPath() %>/js/global.js"></script>
        <script type="text/javascript" src="<%= request.getContextPath() %>/share/javascript/prototype.js"></script>
        <script type="text/javascript" src="<%= request.getContextPath() %>/share/javascript/screen.js"></script>
        <script type="text/javascript" src="<%= request.getContextPath() %>/share/javascript/rx.js"></script>
        <script type="text/javascript" src="<%= request.getContextPath() %>/share/javascript/scriptaculous.js"></script>
        <script type="text/javascript" src="<%= request.getContextPath() %>/share/javascript/effects.js"></script>

        <title><bean:message key="ChooseAllergy.title"/></title>
        <html:base/>

        <c:if test="${empty sessionScope.RxSessionBean}">
            <c:redirect url="error.html"/>
        </c:if>

        <c:if test="${not empty sessionScope.RxSessionBean}">
            <c:set var="bean" value="${sessionScope.RxSessionBean}" scope="page"/>

            <c:if test="${!bean.valid}">
                <c:redirect url="error.html"/>
            </c:if>
        </c:if>


        <%
            oscar.oscarRx.pageUtil.RxSessionBean bean = (oscar.oscarRx.pageUtil.RxSessionBean) pageContext.findAttribute("bean");
        %>

        <link rel="stylesheet" type="text/css" href="styles.css">


        <script type="text/javascript">
            function isEmpty() {
                if (document.RxSearchAllergyForm.searchString.value.length == 0) {
                    alert("Search Field is Empty");
                    document.RxSearchAllergyForm.searchString.focus();
                    return false;
                }
                return true;
            }

            function addCustomAllergy() {
                var name = document.getElementById('searchString').value;
                if (isEmpty() == true) {
                    name = name.toUpperCase();
                    alert(name);
                    window.location = "addReaction2.do?ID=0&type=0&name=" + name;
                }
            }

            function addPenicillinAllergy() {
                window.location = "addReaction2.do?ID=44452&name=PENICILLINS&type=10";
            }

            function addSulfonamideAllergy() {
                window.location = "addReaction2.do?ID=44159&name=SULFONAMIDES&type=10";
            }

            function addCustomNKDA() {
                window.location = "addReaction2.do?ID=0&type=0&name=NKDA";
            }

            function toggleSection(typecode) {
                var imgsrc = document.getElementById(typecode + "_img").src;
                if (imgsrc.indexOf('expander') != -1) {
                    document.getElementById(typecode + "_img").src = '../images/collapser.png';
                    Effect.BlindDown(document.getElementById(typecode + "_content"), {duration: 0.1});
                } else {
                    document.getElementById(typecode + "_img").src = '../images/expander.png';
                    Effect.BlindUp(document.getElementById(typecode + "_content"), {duration: 0.1});
                }

            }
        </script>
    </head>
    <body topmargin="0" leftmargin="0" vlink="#0000FF">

    <table border="0" cellpadding="0" cellspacing="0"
           style="border-collapse: collapse" bordercolor="#111111" width="100%"
           id="AutoNumber1" height="100%">
        <%@ include file="TopLinks.jsp"%><!-- Row One included here-->
        <tr>
            <%@ include file="SideLinksNoEditFavorites2.jsp"%><!-- <td></td>Side Bar File --->
            <td width="100%" style="border-left: 2px solid #A9A9A9;" height="100%"
                valign="top">
                <table cellpadding="0" cellspacing="2"
                       style="border-collapse: collapse" bordercolor="#111111" width="100%"
                       height="100%">
                    <tr>
                        <td width="0%" valign="top">
                            <div class="DivCCBreadCrumbs"><a href="SearchDrug3.jsp"> <bean:message
                                    key="SearchDrug.title"/></a>&nbsp;&gt;&nbsp; <a
                                    href="ShowAllergies2.jsp"> <bean:message
                                    key="EditAllergies.title"/></a>&nbsp;&gt;&nbsp; <b><bean:message
                                    key="ChooseAllergy.title"/></b></div>
                        </td>
                    </tr>
                    <!----Start new rows here-->
                    <tr>
                        <td>
                            <div class="DivContentTitle"><bean:message
                                    key="ChooseAllergy.title"/></div>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <div class="DivContentSectionHead"><bean:message
                                    key="ChooseAllergy.section1Title"/></div>
                        </td>
                    </tr>
                    <tr>
                        <td><html:form action="/oscarRx/searchAllergy2"
                                       focus="searchString" onsubmit="return isEmpty()">
                            <table>
                                <tr valign="center">
                                    <td>Search:</td>
                                    <td><html:text property="searchString" size="16" styleId="searchString"
                                                   maxlength="16"/></td>
                                </tr>
                                <tr>
                                    <td><html:submit property="submit" value="Search"
                                                     styleClass="ControlPushButton"/></td>
                                    <td><input type=button class="ControlPushButton"
                                               onclick="javascript:document.forms.RxSearchAllergyForm.searchString.value='';document.forms.RxSearchAllergyForm.searchString.focus();"
                                               value="Reset"/>
                                        <input type=button class="ControlPushButton"
                                               onclick="javascript:addCustomAllergy();" value="Custom Allergy"/>
                                        <input type=button class="ControlPushButton"
                                               onclick="javascript:addCustomNKDA();" value="NKDA"/>
                                        <input type=button class="ControlPushButton"
                                               onclick="javascript:addPenicillinAllergy();" value="Penicillin"/>
                                        <input type=button class="ControlPushButton"
                                               onclick="javascript:addSulfonamideAllergy();" value="Sulfa"/>
                                    </td>
                                </tr>
                            </table>
                            &nbsp;
                            <table bgcolor="#F5F5F5" cellspacing=2
                                   cellpadding=2>
                                <tr>
                                    <td colspan=4>Search the following categories: <i>(Listed
                                        general to specific)</i></td>
                                </tr>
                                <tr>
                                    <td><html:checkbox property="type4"/> Drug Classes</td>
                                    <td><html:checkbox property="type3"/> Ingredients</td>
                                    <td><html:checkbox property="type2"/> Generic Names</td>
                                    <td><html:checkbox property="type1"/> Brand Names</td>
                                </tr>
                                <tr>
                                    <td colspan=4>
                                        <script language=javascript>
                                            function typeSelect() {
                                                var frm = document.forms.RxSearchAllergyForm;

                                                frm.type1.checked = true;
                                                frm.type2.checked = true;
                                                frm.type3.checked = true;
                                                frm.type4.checked = true;

                                            }

                                            function typeClear() {
                                                var frm = document.forms.RxSearchAllergyForm;

                                                frm.type1.checked = false;
                                                frm.type2.checked = false;
                                                frm.type3.checked = false;
                                                frm.type4.checked = false;

                                            }
                                        </script>
                                        <input type=button
                                               class="ControlPushButton" onclick="javascript:typeSelect();"
                                               value="Select All"/> <input type=button
                                                                           class="ControlPushButton"
                                                                           onclick="javascript:typeClear();"
                                                                           value="Clear All"/></td>
                                </tr>
                            </table>
                        </html:form></td>
                    </tr>
                    <tr>
                        <td id="searchResultsContainer">
                            <table>
                                <tr>
                                    <td>
                                        <div class="DivContentSectionHead"><bean:message
                                                key="ChooseAllergy.section2Title"/></div>
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        <%
                                            int newSection = 0;
                                            Hashtable drugClassHash = (Hashtable) request.getAttribute("drugClasses");
                                        %>
                                        <div class="LeftMargin">
                                            <c:choose>
                                                <c:when test="${empty allergies}">
                                                    Search returned no results. Revise your search and try again.
                                                </c:when>
                                                <c:otherwise>
                                                    <c:set var="flatResults" value="${oscarProperties.getProperty('allergies.flat_results', 'false') == 'true'}" />

                                                    <!-- 判断是否为平铺结果显示 -->
                                                    <c:if test="${flatResults}">
                                                        <c:forEach var="allergy" items="${flatMap}">
                                                            <a href="addReaction.do?ID=${allergy.value.drugrefId}&name=${fn:escapeXml(allergy.value.description)}&type=${allergy.value.typeCode}">
                                                                    ${allergy.value.description}
                                                            </a>

                                                            <!-- 显示药物分类 -->
                                                            <c:forEach var="drugClass" items="${drugClassHash[allergy.value.drugrefId]}">
                                                                &nbsp;&nbsp;&nbsp;
                                                                <a style="color: orange" href="addReaction.do?ID=${drugClass[0]}&name=${fn:escapeXml(drugClass[1])}&type=10">
                                                                        ${drugClass[1]}
                                                                </a>
                                                            </c:forEach>
                                                            <br/>
                                                        </c:forEach>
                                                    </c:if>

                                                    <!-- 分层显示不同类型过敏分类 -->
                                                    <c:if test="${!flatResults}">
                                                        <c:forEach var="type" items="${[8, 10, 13, 11, 12, 14]}">
                                                            <c:if test="${not empty allergyResults[type]}">
                                                                <div class="DivContentSectionHead">
                                                                    <a href="javascript:void(0)" onclick="toggleSection('${type}');return false;">
                                                                        <img border="0" id="${type}_img" src="../images/${type == 8 || type == 10 || type == 13 || type == 14 ? 'collapser' : 'expander'}.png"/>
                                                                    </a>
                                                                        ${type == 8 ? 'ATC Class' : type == 10 ? 'AHFS Class' : type == 13 ? 'Brand Name' : type == 11 ? 'Generic Name' : type == 12 ? 'Compound' : 'Ingredient'}
                                                                </div>

                                                                <div id="${type}_content" style="display: ${type == 11 || type == 12 ? 'none' : 'block'}">
                                                                    <c:forEach var="allergy" items="${allergyResults[type]}">
                                                                        <a href="addReaction.do?ID=${allergy.drugrefId}&name=${fn:escapeXml(allergy.description)}&type=${allergy.typeCode}">
                                                                                ${allergy.description}
                                                                        </a>

                                                                        <!-- 显示药物分类 -->
                                                                        <c:forEach var="drugClass" items="${drugClassHash[allergy.drugrefId]}">
                                                                            &nbsp;&nbsp;&nbsp;
                                                                            <a style="color: orange" href="addReaction.do?ID=${drugClass[0]}&name=${fn:escapeXml(drugClass[1])}&type=10">
                                                                                    ${drugClass[1]}
                                                                            </a>
                                                                        </c:forEach>
                                                                        <br/>
                                                                    </c:forEach>
                                                                </div>
                                                            </c:if>
                                                        </c:forEach>
                                                    </c:if>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>

                                        <%
                                            String sBack = "ShowAllergies2.jsp";
                                        %> <input type=button class="ControlPushButton"
                                                  onclick="javascript:window.location.href='<%=sBack%>';"
                                                  value="Back to View Allergies"/></td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    <!----End new rows here-->
                    <tr height="100%">
                        <td></td>
                    </tr>
                </table>
            </td>
        </tr>
        <tr>
            <td height="0%"
                style="border-bottom: 2px solid #A9A9A9; border-top: 2px solid #A9A9A9;"></td>
            <td height="0%"
                style="border-bottom: 2px solid #A9A9A9; border-top: 2px solid #A9A9A9;"></td>
        </tr>
        <tr>
            <td width="100%" height="0%" colspan="2">&nbsp;</td>
        </tr>
        <tr>
            <td width="100%" height="0%" style="padding: 5" bgcolor="#DCDCDC"
                colspan="2"></td>
        </tr>
    </table>

    </body>

</html:html>