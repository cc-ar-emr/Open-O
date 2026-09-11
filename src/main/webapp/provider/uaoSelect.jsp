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
<%--
    Lets the acting provider choose the "Under Authority Of" (UAO) value to sign in under.
    Rendered by UaoSelectAction when the provider has more than one value.

    @since 2026-07-02
--%>
<%@ taglib uri="/WEB-INF/security.tld" prefix="security" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="e" uri="https://www.owasp.org/index.php/OWASP_Java_Encoder_Project" %>

<c:set var="roleName" value="${userrole},${user}"/>

<security:oscarSec roleName="${roleName}" objectName="_ehr.connectivity" rights="r" reverse="true">
    <c:redirect url="/oscar/securityError.jsp?type=_ehr.connectivity"/>
</security:oscarSec>

<security:oscarSec roleName="${roleName}" objectName="_ehr.connectivity" rights="r" reverse="false">
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title>ONE ID - Select Authority</title>
    <link href="${pageContext.request.contextPath}/library/bootstrap/5.0.2/css/bootstrap.min.css" rel="stylesheet"/>
    <script src="${pageContext.request.contextPath}/csrfguard" type="text/javascript"></script>
</head>
<body>
<div class="container" style="max-width: 640px;">
    <div class="my-5">
        <h4 class="mb-1">On whose authority are you signing in?</h4>
        <p class="text-muted">Choose the value you are acting under for this session.</p>
        <c:if test="${not empty currentUao}">
            <p class="text-muted">Current: ${e:forHtmlContent(currentUao)}</p>
        </c:if>

        <div class="d-grid gap-2 mt-3">
            <c:forEach var="uao" items="${uaoList}">
                <form method="post" action="${pageContext.request.contextPath}/uaoSelectApply.do">
                    <input type="hidden" name="id" value="${e:forHtmlAttribute(uao.id)}"/>
                    <button type="submit" class="btn btn-primary btn-lg w-100"><c:choose><c:when test="${not empty uao.friendlyName}">${e:forHtmlContent(uao.friendlyName)}</c:when><c:otherwise>${e:forHtmlContent(uao.name)}</c:otherwise></c:choose></button>
                </form>
            </c:forEach>
        </div>
    </div>
</div>
</body>
</html>
</security:oscarSec>
