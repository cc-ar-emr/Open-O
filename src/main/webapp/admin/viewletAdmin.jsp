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
    Administrative screen for the Viewlet registry: the EHR services available to launch,
    each with its toolbar key and interface type (modal or non-modal).
    Rendered by ViewletAdminAction.

    @since 2026-07-06
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
    <title>EHR Connectivity - Viewlets</title>
    <script src="${pageContext.request.contextPath}/csrfguard" type="text/javascript"></script>
    <link href="${pageContext.request.contextPath}/library/bootstrap/5.0.2/css/bootstrap.min.css" rel="stylesheet"/>
</head>
<body>
<div class="container-fluid my-4">
    <h4 class="mb-1">Viewlets</h4>
    <p class="text-muted">Configure the EHR services available to launch: each Viewlet's name, its toolbar key, whether it shows in the eChart, and its interface type.</p>

    <c:if test="${not empty viewletAdminError}">
        <div class="alert alert-warning">${e:forHtmlContent(viewletAdminError)}</div>
    </c:if>

    <div class="card mb-4">
        <div class="card-header">Add a Viewlet</div>
        <div class="card-body">
            <form method="post" action="${pageContext.request.contextPath}/admin/viewletAdminAdd.do" class="row g-2 align-items-center">
                <div class="col-md-3">
                    <input type="text" class="form-control" name="name" placeholder="Viewlet name" maxlength="100" required/>
                </div>
                <div class="col-md-3">
                    <input type="text" class="form-control" name="keyValue" placeholder="Toolbar key" maxlength="50" required/>
                </div>
                <div class="col-auto">
                    <select class="form-select" name="displayMode">
                        <option value="non-modal">Non-modal (window)</option>
                        <option value="modal">Modal (dialog)</option>
                    </select>
                </div>
                <div class="col-auto form-check">
                    <input type="checkbox" class="form-check-input" name="showInEchart" value="true" id="addShowInEchart" checked/>
                    <label class="form-check-label" for="addShowInEchart">Show in eChart</label>
                </div>
                <div class="col-auto">
                    <button type="submit" class="btn btn-success">Add</button>
                </div>
            </form>
        </div>
    </div>

    <table class="table table-sm table-striped table-bordered align-middle">
        <thead>
        <tr>
            <th>Name</th>
            <th>Toolbar key</th>
            <th>Interface</th>
            <th>eChart</th>
            <th>Status</th>
            <th>Updated</th>
            <th class="text-end">Actions</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="viewlet" items="${viewletList}">
            <tr>
                <td>
                    <input type="text" class="form-control form-control-sm" name="name" form="viewletForm${viewlet.id}"
                           value="${e:forHtmlAttribute(viewlet.name)}" maxlength="100" required/>
                </td>
                <td>
                    <input type="text" class="form-control form-control-sm" name="keyValue" form="viewletForm${viewlet.id}"
                           value="${e:forHtmlAttribute(viewlet.keyValue)}" maxlength="50" required/>
                </td>
                <td>
                    <select class="form-select form-select-sm" name="displayMode" form="viewletForm${viewlet.id}">
                        <option value="non-modal" <c:if test="${viewlet.displayMode ne 'modal'}">selected</c:if>>Non-modal (window)</option>
                        <option value="modal" <c:if test="${viewlet.displayMode eq 'modal'}">selected</c:if>>Modal (dialog)</option>
                    </select>
                </td>
                <td class="text-center">
                    <input type="checkbox" class="form-check-input" name="showInEchart" value="true" form="viewletForm${viewlet.id}"
                           <c:if test="${viewlet.showInEchart}">checked</c:if>/>
                </td>
                <td>
                    <c:choose>
                        <c:when test="${viewlet.deleted}"><span class="badge bg-secondary">Disabled</span></c:when>
                        <c:otherwise><span class="badge bg-success">Active</span></c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <small><c:if test="${not empty viewlet.updatedBy}">${e:forHtmlContent(updatedByNames[viewlet.updatedBy])} @ </c:if><fmt:formatDate value="${viewlet.updateTime}" pattern="yyyy-MM-dd HH:mm"/></small>
                </td>
                <td class="text-end">
                    <button type="submit" class="btn btn-sm btn-outline-primary" form="viewletForm${viewlet.id}">Save</button>
                    <c:choose>
                        <c:when test="${viewlet.deleted}">
                            <form method="post" action="${pageContext.request.contextPath}/admin/viewletAdminEnable.do" class="d-inline">
                                <input type="hidden" name="id" value="${e:forHtmlAttribute(viewlet.id)}"/>
                                <button type="submit" class="btn btn-sm btn-outline-success">Enable</button>
                            </form>
                        </c:when>
                        <c:otherwise>
                            <form method="post" action="${pageContext.request.contextPath}/admin/viewletAdminDisable.do" class="d-inline"
                                  onsubmit="return confirm('Disable this Viewlet?');">
                                <input type="hidden" name="id" value="${e:forHtmlAttribute(viewlet.id)}"/>
                                <button type="submit" class="btn btn-sm btn-outline-danger">Disable</button>
                            </form>
                        </c:otherwise>
                    </c:choose>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty viewletList}">
            <tr><td colspan="7" class="text-center text-muted">No Viewlets configured.</td></tr>
        </c:if>
        </tbody>
    </table>

    <c:forEach var="viewlet" items="${viewletList}">
        <form id="viewletForm${viewlet.id}" method="post" action="${pageContext.request.contextPath}/admin/viewletAdminUpdate.do">
            <input type="hidden" name="id" value="${e:forHtmlAttribute(viewlet.id)}"/>
        </form>
    </c:forEach>
</div>
</body>
</html>
</security:oscarSec>
