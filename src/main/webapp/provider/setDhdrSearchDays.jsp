<%--
    Sets this provider's default search window, in days, for the DHDR medication viewer
    (DHDR02.03). Reached from provider preferences via
    setProviderStaleDate.do?method=viewDhdrSearchDays, and posts back to saveDhdrSearchDays.

    Leaving the field empty clears the provider's own value so the instance-wide
    dhdr.default_search_days property applies again; that fallback is shown as the
    placeholder. A non-empty value that cannot be used is reported rather than stored.

    Numeric validation is shared with the other provider preference pages via
    provider_form_validations.js, which keys off the numericFormField id. That check is
    necessary but not sufficient here - it accepts any run of digits above zero, including
    values wider than an int - so the action validates again and re-renders this page with
    the same error element when it rejects.

    @since 2026-07-27
--%>

<%@ page contentType="text/html; charset=UTF-8" %>
<%@ include file="/casemgmt/taglibs.jsp" %>
<%@ page import="java.util.ResourceBundle" %>
<%@ page import="org.owasp.encoder.Encode" %>

<%
    if (session.getAttribute("user") == null)
        response.sendRedirect(request.getContextPath() + "/logout.htm");

    ResourceBundle bundle = ResourceBundle.getBundle("oscarResources", request.getLocale());

    String providertitle = (String) request.getAttribute("providertitle");
    String providermsgPrefs = (String) request.getAttribute("providermsgPrefs");
    String providermsgProvider = (String) request.getAttribute("providermsgProvider");
    String providermsgEdit = (String) request.getAttribute("providermsgEdit");
    String providerbtnSubmit = (String) request.getAttribute("providerbtnSubmit");
    String providermsgSuccess = (String) request.getAttribute("providermsgSuccess");
%>
<!DOCTYPE html>
<c:set var="ctx" value="${pageContext.request.contextPath}" scope="request"/>
<html>
    <head>
        <base href="<%= request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/" %>">
        <title><%=Encode.forHtml(String.valueOf(bundle.getString(providertitle)))%></title>

        <script src="<c:out value="${ctx}"/>/js/global.js"></script>
        <%-- Injects the CSRF token into the form below. This page POSTs a change to a stored
             provider preference, and CsrfGuard's ProtectedMethods covers POST, but the token has
             to be added per page - there is no filter that does it, and ~50 other form-bearing
             JSPs include this the same way. Without it the form submits no token at all. --%>
        <script src="<c:out value="${ctx}"/>/csrfguard" type="text/javascript"></script>
        <%-- Loaded only alongside the form. The script binds to providerForm during window.onload
             without checking that it is there, and the success state below renders no form - the
             three sibling preference pages that share the script have the same shape. --%>
        <% if (request.getAttribute("status") == null) { %>
        <script src="<c:out value="${ctx}"/>/share/javascript/provider_form_validations.js"></script>
        <% } %>
        <link href="<c:out value="${ctx}"/>/css/bootstrap.css" rel="stylesheet" type="text/css"><!-- Bootstrap 2.3.1 -->

    </head>

    <body class="BodyStyle">

    <table class="MainTable" id="scrollNumber1" name="encounterTable">
        <tr class="MainTableTopRow">
            <td class="MainTableTopRowLeftColumn"><h4><%=Encode.forHtml(String.valueOf(bundle.getString(providermsgPrefs)))%></h4></td>
            <td class="MainTableTopRowRightColumn"><h4>&nbsp;&nbsp;<%=Encode.forHtml(String.valueOf(bundle.getString(providermsgProvider)))%></h4></td>
        </tr>
        <tr>
            <td class="MainTableLeftColumn">&nbsp;</td>
            <td class="MainTableRightColumn">
                <%if (request.getAttribute("status") == null) {%> <%=Encode.forHtml(String.valueOf(bundle.getString(providermsgEdit)))%>
                <form id="providerForm" action="${pageContext.request.contextPath}/setProviderStaleDate.do" method="post">
                    <input type="hidden" name="method" value="<c:out value="${method}"/>">
                    <input type="text" id="numericFormField" name="dhdrSearchDaysProperty.value"
                           value="<c:out value='${dhdrSearchDays.value}'/>"
                           placeholder="<c:out value='${dhdrClinicDefault}'/>"/>
                    <%-- Shown by provider_form_validations.js on a client-side rejection, and
                         rendered visible here when the server rejected the submission - a value
                         can pass the page check and still be unusable (all digits, greater than
                         zero, but wider than an int). Same element and wording either way, so the
                         two paths cannot disagree about what the provider is told. --%>
                    <p id="errorMessage" class="alert alert-danger"
                       style="display: <%= request.getAttribute("inputError") != null ? "block" : "none" %>; color: red;">
                        Invalid input.
                    </p>
                    <br>
                    <input type="submit" value="<%=Encode.forHtmlAttribute(String.valueOf(bundle.getString(providerbtnSubmit)))%>"/>
                </form> <%} else {%>
                <div class="alert alert-success"><%=Encode.forHtml(String.valueOf(bundle.getString(providermsgSuccess)))%></div>
                <br>
                <%}%>
            </td>
        </tr>
        <tr>
            <td class="MainTableBottomRowLeftColumn"></td>
            <td class="MainTableBottomRowRightColumn"></td>
        </tr>
    </table>
    </body>
</html>
