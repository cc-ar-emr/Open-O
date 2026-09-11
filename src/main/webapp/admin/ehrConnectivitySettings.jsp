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
    ONE ID gateway settings: enter the client credentials, endpoints, JWKS/issuer and OAG public
    key used to connect to Ontario Health, and upload the gateway keystore. Rendered by
    EhrConnectivitySettingsAction. Secret fields are blank on load and only saved when a new value
    is entered, and the keystore is only replaced when a file is chosen.

    @since 2026-07-01
--%>
<%@ taglib uri="/WEB-INF/security.tld" prefix="security" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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
    <title>EHR Connectivity - Gateway Settings</title>
    <script src="${pageContext.request.contextPath}/csrfguard" type="text/javascript"></script>
    <link href="${pageContext.request.contextPath}/library/bootstrap/5.0.2/css/bootstrap.min.css" rel="stylesheet"/>
</head>
<body>
<div class="container my-4">
    <h4 class="mb-3">ONE ID Gateway Settings</h4>
    <p class="text-muted">Client credentials, endpoints and keystore used to connect to Ontario
        Health. Secret fields are shown blank; leave them blank to keep the stored value.</p>

    <c:if test="${saved and empty problems}">
        <div class="alert alert-success" role="alert">Settings saved.</div>
    </c:if>

    <c:if test="${not empty problems}">
        <div class="alert alert-danger" role="alert">
            <c:forEach var="problem" items="${problems}">
                <div>${e:forHtmlContent(problem)}</div>
            </c:forEach>
        </div>
    </c:if>

    <form name="ehrConnectivitySettingsForm" method="post" enctype="multipart/form-data"
          action="${pageContext.request.contextPath}/admin/ehrConnectivitySettingsSave.do">
        <c:forEach var="setting" items="${settings}">
            <div class="row mb-3">
                <label for="${e:forHtmlAttribute(setting.key)}" class="col-sm-3 col-form-label">
                    ${e:forHtmlContent(setting.label)}
                </label>
                <div class="col-sm-9">
                    <c:choose>
                        <c:when test="${setting.type eq 'secret'}">
                            <input type="password" class="form-control" id="${e:forHtmlAttribute(setting.key)}"
                                   name="${e:forHtmlAttribute(setting.key)}" value=""
                                   maxlength="${valueMaxLength}"
                                   placeholder="(unchanged)" autocomplete="new-password"/>
                        </c:when>
                        <c:when test="${setting.type eq 'textarea'}">
                            <textarea class="form-control" rows="4" id="${e:forHtmlAttribute(setting.key)}"
                                      maxlength="${valueMaxLength}"
                                      name="${e:forHtmlAttribute(setting.key)}">${e:forHtmlContent(setting.value)}</textarea>
                        </c:when>
                        <c:when test="${setting.type eq 'switch'}">
                            <div class="form-check form-switch mt-2">
                                <input type="checkbox" role="switch" class="form-check-input" value="true"
                                       id="${e:forHtmlAttribute(setting.key)}"
                                       name="${e:forHtmlAttribute(setting.key)}"
                                       <c:if test="${setting.value eq 'true'}">checked="checked"</c:if>/>
                            </div>
                        </c:when>
                        <c:when test="${setting.type eq 'file'}">
                            <input type="file" class="form-control" id="${e:forHtmlAttribute(setting.key)}"
                                   name="keystoreUpload" accept=".jks,.p12,.pfx,.keystore"/>
                            <div class="form-text">
                                <c:choose>
                                    <c:when test="${not empty setting.value}">
                                        Stored at ${e:forHtmlContent(setting.value)}. Choose a file
                                        to replace it; leave empty to keep it.
                                    </c:when>
                                    <c:otherwise>No keystore has been uploaded yet.</c:otherwise>
                                </c:choose>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <input type="text" class="form-control" id="${e:forHtmlAttribute(setting.key)}"
                                   maxlength="${valueMaxLength}"
                                   name="${e:forHtmlAttribute(setting.key)}" value="${e:forHtmlAttribute(setting.value)}"/>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </c:forEach>
        <button type="submit" class="btn btn-primary">Save</button>
    </form>
</div>
<script src="${pageContext.request.contextPath}/library/bootstrap/5.0.2/js/bootstrap.bundle.min.js"></script>
</body>
</html>
</security:oscarSec>
