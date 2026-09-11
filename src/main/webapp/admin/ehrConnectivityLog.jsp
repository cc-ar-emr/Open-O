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
    Read-only viewer of the ONE ID gateway transaction log. Rendered by GatewayLogAction.

    @since 2026-07-01
--%>
<%@ taglib uri="/WEB-INF/security.tld" prefix="security" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="e" uri="https://www.owasp.org/index.php/OWASP_Java_Encoder_Project" %>

<c:set var="roleName" value="${userrole},${user}"/>

<security:oscarSec roleName="${roleName}" objectName="_admin.ehrConnectivity" rights="r" reverse="true">
    <c:redirect url="/oscar/securityError.jsp?type=_admin.ehrConnectivity"/>
</security:oscarSec>

<security:oscarSec roleName="${roleName}" objectName="_admin.ehrConnectivity" rights="r" reverse="false">
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title>EHR Connectivity - Gateway Log</title>
    <link href="${pageContext.request.contextPath}/library/bootstrap/5.0.2/css/bootstrap.min.css" rel="stylesheet"/>
</head>
<body>
<div class="container-fluid my-4">
    <h4 class="mb-3">ONE ID Gateway Transaction Log</h4>

    <form method="get" action="${pageContext.request.contextPath}/admin/ehrConnectivityLog.do" class="row g-2 mb-3">
        <div class="col-auto">
            <input type="text" class="form-control" name="providerNo" placeholder="Provider #"
                   value="${e:forHtmlAttribute(providerNo)}"/>
        </div>
        <div class="col-auto">
            <input type="text" class="form-control" name="externalSystem" placeholder="System (e.g. CMS)"
                   value="${e:forHtmlAttribute(externalSystem)}"/>
        </div>
        <div class="col-auto">
            <button type="submit" class="btn btn-primary">Filter</button>
            <a href="${pageContext.request.contextPath}/admin/ehrConnectivityLog.do" class="btn btn-outline-secondary">Reset</a>
        </div>
    </form>

    <p class="text-muted">Showing up to ${rowLimit} most recent rows.</p>

    <table class="table table-sm table-striped table-bordered">
        <thead>
        <tr>
            <th>Started</th>
            <th>Provider</th>
            <th>System</th>
            <th>Transaction</th>
            <th>Status</th>
            <th>EHR Outcome</th>
            <th>UAO</th>
            <th>X-Request-Id</th>
            <th>X-Correlation-Id</th>
            <th>X-LobTxId</th>
            <th>Error</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="log" items="${logs}">
            <tr>
                <td><fmt:formatDate value="${log.started}" pattern="yyyy-MM-dd HH:mm:ss"/></td>
                <td>${e:forHtmlContent(log.initiatingProviderNo)}</td>
                <td>${e:forHtmlContent(log.externalSystem)}</td>
                <td>${e:forHtmlContent(log.transactionType)}</td>
                <td>
                    <c:choose>
                        <%-- A row that carries no outcome, such as a viewlet launch, whose result
                             is recorded separately against the same correlation id. Tested first,
                             because an unset value reads as false once it is used as a boolean. --%>
                        <c:when test="${empty log.success}"><span class="badge bg-secondary">UNKNOWN</span></c:when>
                        <c:when test="${log.success}"><span class="badge bg-success">OK</span></c:when>
                        <c:otherwise><span class="badge bg-danger">FAIL</span></c:otherwise>
                    </c:choose>
                    ${e:forHtmlContent(log.resultCode)}
                </td>
                <td>${e:forHtmlContent(log.ehrResultCode)}</td>
                <td>${e:forHtmlContent(log.uao)}</td>
                <td>${e:forHtmlContent(log.xRequestId)}</td>
                <td>${e:forHtmlContent(log.xCorrelationId)}</td>
                <td>${e:forHtmlContent(log.xLobTxId)}</td>
                <td>${e:forHtmlContent(log.error)}</td>
            </tr>
        </c:forEach>
        <c:if test="${empty logs}">
            <tr><td colspan="11" class="text-center text-muted">No gateway transactions recorded.</td></tr>
        </c:if>
        </tbody>
    </table>
</div>
</body>
</html>
</security:oscarSec>
