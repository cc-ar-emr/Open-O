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
    Administrative screen for the "Under Authority Of" (UAO) values assigned to a provider.
    Rendered by UaoAdminAction.

    @since 2026-07-02
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
    <title>EHR Connectivity - Under Authority Of</title>
    <link href="${pageContext.request.contextPath}/library/bootstrap/5.0.2/css/bootstrap.min.css" rel="stylesheet"/>
    <script src="${pageContext.request.contextPath}/csrfguard" type="text/javascript"></script>
</head>
<body>
<div class="container-fluid my-4">
    <h4 class="mb-1">Under Authority Of (UAO)</h4>
    <p class="text-muted">Assign the "on behalf of" values a provider may act under at the ONE ID gateway.</p>

    <form method="get" action="${pageContext.request.contextPath}/admin/uaoAdmin.do" class="row g-2 mb-4">
        <div class="col-auto">
            <input type="text" class="form-control" name="providerNo" placeholder="Provider #"
                   value="${e:forHtmlAttribute(providerNo)}"/>
        </div>
        <div class="col-auto">
            <button type="submit" class="btn btn-primary">Load</button>
        </div>
    </form>

    <c:if test="${not empty uaoAdminError}">
        <div class="alert alert-warning">${e:forHtmlContent(uaoAdminError)}</div>
    </c:if>

    <c:if test="${not empty providerNo}">
        <div class="card mb-4">
            <div class="card-header">Add a UAO for provider ${e:forHtmlContent(providerNo)}</div>
            <div class="card-body">
                <form method="post" action="${pageContext.request.contextPath}/admin/uaoAdminAdd.do" class="row g-2 align-items-center">
                    <input type="hidden" name="providerNo" value="${e:forHtmlAttribute(providerNo)}"/>
                    <div class="col-md-4">
                        <input type="text" class="form-control" name="name" placeholder="UAO value" required/>
                    </div>
                    <div class="col-md-4">
                        <input type="text" class="form-control" name="friendlyName" placeholder="Friendly name" required/>
                    </div>
                    <div class="col-md-4">
                        <input type="text" class="form-control" name="address" placeholder="Street address"/>
                    </div>
                    <div class="col-md-3">
                        <input type="text" class="form-control" name="city" placeholder="City"/>
                    </div>
                    <div class="col-md-3">
                        <input type="text" class="form-control" name="province" placeholder="Province"/>
                    </div>
                    <div class="col-md-2">
                        <input type="text" class="form-control" name="postal" placeholder="Postal code"/>
                    </div>
                    <div class="col-auto form-check">
                        <input type="checkbox" class="form-check-input" name="defaultUAO" value="true" id="defaultUAO"/>
                        <label class="form-check-label" for="defaultUAO">Default</label>
                    </div>
                    <div class="col-auto">
                        <button type="submit" class="btn btn-success">Add</button>
                    </div>
                </form>
            </div>
        </div>

        <table class="table table-sm table-striped table-bordered">
            <thead>
            <tr>
                <th>UAO value</th>
                <th>Friendly name</th>
                <th>Address</th>
                <th>Default</th>
                <th>Added by</th>
                <th class="text-end">Actions</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="uao" items="${uaoList}">
                <tr>
                    <td>${e:forHtmlContent(uao.name)}</td>
                    <td>${e:forHtmlContent(uao.friendlyName)}</td>
                    <td>
                        ${e:forHtmlContent(uao.address)}
                        <c:if test="${not empty uao.city}">, ${e:forHtmlContent(uao.city)}</c:if>
                        <c:if test="${not empty uao.province}">, ${e:forHtmlContent(uao.province)}</c:if>
                        <c:if test="${not empty uao.postal}">, ${e:forHtmlContent(uao.postal)}</c:if>
                    </td>
                    <td>
                        <c:choose>
                            <c:when test="${uao.defaultUAO}"><span class="badge bg-primary">Default</span></c:when>
                            <c:otherwise>
                                <form method="post" action="${pageContext.request.contextPath}/admin/uaoAdminSetDefault.do" class="d-inline">
                                    <input type="hidden" name="providerNo" value="${e:forHtmlAttribute(providerNo)}"/>
                                    <input type="hidden" name="id" value="${e:forHtmlAttribute(uao.id)}"/>
                                    <button type="submit" class="btn btn-sm btn-outline-secondary">Set default</button>
                                </form>
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td>${e:forHtmlContent(uao.addedBy)}</td>
                    <td class="text-end">
                        <form method="post" action="${pageContext.request.contextPath}/admin/uaoAdminDelete.do" class="d-inline"
                              onsubmit="return confirm('Remove this UAO?');">
                            <input type="hidden" name="providerNo" value="${e:forHtmlAttribute(providerNo)}"/>
                            <input type="hidden" name="id" value="${e:forHtmlAttribute(uao.id)}"/>
                            <button type="submit" class="btn btn-sm btn-outline-danger">Remove</button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty uaoList}">
                <tr><td colspan="5" class="text-center text-muted">No UAO values for this provider.</td></tr>
            </c:if>
            </tbody>
        </table>
    </c:if>
</div>
</body>
</html>
</security:oscarSec>
