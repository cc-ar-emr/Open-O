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
<%@ page import="ca.openosp.openo.lab.ca.on.LabResultData" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="ca.openosp.openo.lab.ca.all.Hl7textResultsData" %>
<%@ page import="ca.openosp.openo.utility.MiscUtils" %>
<%@ taglib uri="/WEB-INF/security.tld" prefix="security" %>
<%
    String roleName$ = (String) session.getAttribute("userrole") + "," + (String) session.getAttribute("user");
    boolean authed = true;
%>
<security:oscarSec roleName="<%=roleName$%>" objectName="_con" rights="w" reverse="<%=true%>">
    <%authed = false; %>
    <%response.sendRedirect(request.getContextPath() + "/securityError.jsp?type=_con");%>
</security:oscarSec>
<%
    if (!authed) {
        return;
    }
%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="https://www.owasp.org/index.php/OWASP_Java_Encoder_Project" prefix="e" %>

<!DOCTYPE html >
<html>
<head>
    <title><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.oscarConsultationRequest.AttachDocPopup.title"/></title>

    <style>

        .attachmentContainer * {
            font-family: "Helvetica Neue", Helvetica, Arial, sans-serif !important;
            font-size: 12px !important;
            color: black;
        }

        .attachmentContainer table {
            border-collapse: collapse;
            border: none;
        }

        .attachmentContainer table, .attachmentContainer table tr td {
            background-color: white !important;
        }

        /* Pin weights and margins so the picker renders the same in every host page
           (Consult loads no Bootstrap; the Tickler windows load Bootstrap 3). */
        .attachmentContainer h2 {
            font-weight: bold;
            margin: 0.83em 0;
        }

        .attachmentContainer label {
            font-weight: normal;
            display: inline;
            margin-bottom: 0;
            max-width: none;
        }

        .attachmentContainer ul {
            margin: 1em 0;
        }

        .attachmentContainer {
            display: flex;
            width: 95vw;
            background-color: white !important;
        }

        @media (min-width: 2400px) {
            .attachmentContainer {
                width: 97vw;
            }
        }

        .attachmentList {
            overflow-y: scroll;
            min-width: 320px;
            min-height: 580px;
            width: 100%;
            flex-basis: 40%;
        }

        @media (min-height: 1600px) {
            .attachmentList {
                height: 97vh;
            }
        }

        #attachDocumentsPanel {
            width: 100%;
            font-size: x-small;
        }

        .preview-button {
            padding: 2px 4px;
            color: black;
            border: none;
            border-radius: 2px;
            cursor: pointer;
        }

        #attachDocumentsForm li.selectAllHeading {
            justify-content: space-between;
            align-items: center;
            border-bottom: black thin inset
        }

        .show-all-button {
            padding: 2px 4px;
            color: black;
            border: none;
            border-radius: 2px;
            cursor: pointer;
            margin-left: auto;
            background-color: lightblue;
        }

        #pdfPreview {
            width: 100%;
            display: flex;
            justify-content: center;
            align-items: center;
            flex-basis: 60%;
        }

        #pdfObject {
            width: 100%;
            height: 100%;
        }

        .preview-filler {
            border: 2px solid black;
            border-radius: 7px;
            padding: 10px;
        }

        .preview-pane {
            background-color: lightgray;
        }

        .flex {
            display: flex !important;
        }

        .hide {
            display: none !important;
        }

        .attachmentContainer ul li:nth-of-type(odd) {
            background-color: white;
        }

        .attachmentContainer ul li:nth-of-type(even) {
            background-color: whitesmoke;
        }

        .attachmentContainer .collapse-arrow {
            cursor: pointer;
        }

        .attachmentContainer .collapsible-content {
            display: none;
            padding: 0 0 0 20px !important;
        }

        .attachmentContainer .caret {
            display: inline-block;
            width: 6px;
            height: 6px;
            border: solid black;
            border-width: 0 3px 3px 0;
            transform: rotate(-45deg);
            border-radius: 0px;
        }

        .attachmentContainer .caret-down {
            transform: rotate(45deg) !important;
        }

        .deleted-doc label,
        .foreign-doc label {
            color: #888 !important;
            font-style: italic;
        }
    </style>

    <script type="text/javascript">
        function toggleLabVersionList(collapseBtn) {
            jQuery(collapseBtn).toggleClass('caret-down');
            jQuery(collapseBtn).parent().find('.collapsible-content').slideToggle(100);
        }

        function expandLabVersionList(collapseBtn) {
            jQuery(collapseBtn).addClass('caret-down');
            jQuery(collapseBtn).parent().find('.collapsible-content').slideDown(100);
        }

        if (typeof pdfCache == 'undefined') {
            var pdfCache = []; //because this is a global variable, only redeclare it if it doesn't exist. This is relevant when opening the attachment window multiple times in sequence
        }

        function toggleSelectAll(element, startClassName) {
            jQuery("[class^='" + startClassName + "']:not(input[disabled='disabled'])").prop('checked', jQuery(element).prop("checked"));
        }

        function addPdfAttachment(attachmentName, attachmentId, base64Data) {
            const newAttachment = {
                attachmentName,
                attachmentId,
                base64Data
            };
            pdfCache.push(newAttachment);
        }

        function getPdfAttachment(attachmentName, attachmentId) {
            const foundAttachment = pdfCache.find(
                attachment =>
                    attachment.attachmentName === attachmentName &&
                    attachment.attachmentId === attachmentId
            );

            return foundAttachment ? foundAttachment.base64Data : null;
        }

        function showPDF(base64Data) {
            if (!base64Data) {
                showError();
                return;
            }

            const previewFiller = document.getElementById('preview-filler');
            previewFiller.classList.add('hide');
            const pdfObject = document.getElementById('pdfObject');
            let newPdfObject = document.createElement('object');
            newPdfObject.setAttribute('data', "data:application/pdf;base64," + base64Data);
            newPdfObject.type = "application/pdf";
            newPdfObject.id = "pdfObject";
            pdfObject.parentNode.replaceChild(newPdfObject, pdfObject);
            HideSpin();
        }

        function showError(errorMessage) {
            if (errorMessage) {
                alert("A preview of this document could not be generated.\n\n" + errorMessage);
            } else {
                alert("A preview of this document could not be generated.");
            }
            HideSpin();
        }

        function getPdf(attachmentName, attachmentId, parameters) {
            // Please include "<%=request.getContextPath()%>/images/spinner.jsp" into the parent page to control the visibility of the spinner (show/hide).
            ShowSpin(true);
            const base64Data = getPdfAttachment(attachmentName, attachmentId);
            if (base64Data !== null) {
                showPDF(base64Data);
                return;
            }

            jQuery.ajax({
                type: 'GET',
                url: "${ pageContext.request.contextPath }/previewDocs.do?" + parameters,
                dataType: "json",
                success: function (data) {
                    if (data.base64Data) {
                        addPdfAttachment(attachmentName, attachmentId, data.base64Data);
                        showPDF(data.base64Data);
                    } else {
                        showError(data.errorMessage);
                    }
                },
                error: function (xhr, status, error) {
                    showError("");
                }
            });
        }

        function showAll(showButton, attachmentType) {
            let hiddenAttachments = document.getElementsByClassName(attachmentType);
            Array.from(hiddenAttachments).forEach(function (attachment) {
                attachment.classList.remove('hide');
            });
            showButton.classList.add('hide');
            showButton.parentNode.classList.remove('flex');
        }

        /**
         * Helpers shared by consult and eForm attach flows. Defined here so both
         * parents can call them from their beforeClose handlers without needing
         * a separate script include. Safe because the dialog is guaranteed loaded
         * when beforeClose fires.
         */

        /** Warn on new (not pre-attached) private eDoc selections; returns false to abort the close. */
        function confirmPrivateDocsIfAny(formSelector) {
            var newPrivate = jQuery(formSelector)
                .find(".providerPrivateDocument_check:checked:not(input[disabled='disabled'])")
                .not('[data-pre-attached="true"]');
            if (newPrivate.length === 0) return true;
            return confirm("You have selected " + newPrivate.length +
                " private eDoc(s) for attachment.\n\n" +
                "Private documents are personal to your account. " +
                "Attaching them will make them visible to anyone with access to this patient's record.\n\n" +
                "Select OK to confirm, or Cancel to go back.");
        }

        /** Build a hidden delegate input from a checked attachment checkbox. */
        function buildDelegateInput($element) {
            var input = jQuery("<input />", {
                type: 'hidden',
                name: $element.attr('name'),
                value: $element.val(),
                id: "delegate_" + $element.attr('id'),
                class: 'delegateAttachment'
            });
            if ($element.attr('name') === 'docNo') input.attr('data-delegate-type', 'doc');
            return input;
        }

        /**
         * Uncheck server-pre-checked boxes whose delegate was removed in an earlier
         * dialog session. The delegate container (#attachDocumentList for eForm,
         * $mainForm for consult) is the source of truth for unsaved changes.
         * Pass swapClass=true for the consult flow which tracks state via
         * <type>_check vs <type>_pre_check class pairs.
         */
        function syncPreCheckedToDelegates(delegateContainer, swapClass) {
            jQuery('#attachDocumentsForm').find('[data-pre-attached="true"]').each(function () {
                var $cb = jQuery(this);
                var hasDelegate = jQuery(delegateContainer).find(
                    '.delegateAttachment[name="' + this.name + '"][value="' + this.value + '"]'
                ).length > 0;
                if (hasDelegate) return;
                $cb.prop('checked', false).removeAttr('data-pre-attached');
                if (!swapClass) return;
                var tokens = ($cb.attr('class') || '').split(' ');
                for (var i = 0; i < tokens.length; i++) {
                    if (tokens[i].indexOf('_pre_check') > 0) {
                        $cb.removeClass(tokens[i]).addClass(tokens[i].split('_')[0] + '_check');
                        break;
                    }
                }
            });
        }
    </script>

</head>
<body>
<form id="attachDocumentsForm">
    <div class="attachmentContainer">
        <div class="attachmentList">
            <table id="attachDocumentsPanel">
                <c:if test="${not empty allEForms }">
                    <tr>
                        <td><h2>eForms (excluding attachments)</h2></td>
                    </tr>
                    <tr>
                        <td>
                            <ul id="eFormList" style="list-style-type: none;padding:0;">
                                <li class="selectAllHeading ${allEForms.size() > 5 ? 'flex' : ''}">
                                    <input id="selectAllEForms" type="checkbox"
                                           onclick="toggleSelectAll(this, 'eForm_');" value="eForm_check"
                                           title="Select/un-select all eForms."/>
                                    <label for="selectAllEForms">Select all</label>
                                    <button class="show-all-button ${allEForms.size() > 5 ? '' : 'hide'}" type="button"
                                            title="Show ${allEForms.size() - 5} More eForms"
                                            onclick="showAll(this, 'eForm')">Show ${allEForms.size() - 5} More eForms
                                    </button>
                                </li>
                                <c:forEach items="${ allEForms }" var="eForm" varStatus="loop">
                                    <li class="eForm ${loop.index > 4 ? 'hide' : ''}">
                                        <input class="eForm_check attachable_check" type="checkbox" name="eFormNo"
                                               id="eFormNo${ eForm.id }" value="${eForm.id}" title="${e:forHtmlAttribute(eForm.formName)}"/>
                                        <label for="eFormNo${eForm.id}">
                                            <c:out value="${eForm.subject.length() > 0 ? eForm.subject : eForm.formName} ${ eForm.getFormDate() }"/>
                                        </label>
                                        <button class="preview-button" type="button" title="Preview"
                                                onclick="getPdf('EFORM', '${eForm.id}', 'method=renderEFormPDF&eFormId=${eForm.id}')">
                                            Preview
                                        </button>
                                    </li>
                                </c:forEach>
                            </ul>
                        </td>
                    </tr>
                </c:if>

                <c:if test="${not empty allDocuments }">
                    <tr>
                        <td><h2>Patient Documents</h2></td>
                    </tr>
                    <tr>
                        <td>
                            <ul id="documentList" style="list-style-type: none;padding:0px;">
                                <li class="selectAllHeading ${allDocuments.size() > 20 ? 'flex' : ''}">
                                    <input id="selectAllDocuments" type="checkbox"
                                           onclick="toggleSelectAll(this, 'document_');" value="document_check"
                                           title="Select/un-select all patient documents."/>
                                    <label for="selectAllDocuments">Select all</label>
                                    <button class="show-all-button ${allDocuments.size() > 20 ? '' : 'hide'}"
                                            type="button" title="Show ${allDocuments.size() - 20} More Patient Documents"
                                            onclick="showAll(this, 'doc')">Show ${allDocuments.size() - 20} More
                                        Patient Documents
                                    </button>
                                </li>
                                <c:forEach items="${ allDocuments }" var="document" varStatus="loop">
                                    <c:set var="isDeleted" value="${fn:contains(document.status, 'D')}"/>
                                    <c:set var="isAttached" value="${not empty attachedDocumentIds and attachedDocumentIds.contains(document.docId)}"/>
                                    <li class="doc ${loop.index > 19 ? 'hide' : ''} ${isDeleted ? 'deleted-doc' : ''}">
                                        <input class="document_${isAttached ? 'pre_check' : 'check'} attachable_check" type="checkbox" name="docNo"
                                               id="docNo${document.docId}" value="${document.docId}"
                                               ${isAttached ? 'checked="checked" data-pre-attached="true"' : ''}
                                               title="${e:forHtmlAttribute(document.description)}"/>
                                        <label for="docNo${document.docId}"><c:out
                                                value="${ document.description } ${ document.observationDate }"/><c:if test="${isDeleted}"> (deleted)</c:if></label>
                                        <button class="preview-button" type="button" title="Preview"
                                                onclick="getPdf('DOC', '${document.docId}', 'method=renderEDocPDF&eDocId=${document.docId}')">
                                            Preview
                                        </button>
                                    </li>
                                </c:forEach>
                            </ul>
                        </td>
                    </tr>
                </c:if>

                <c:if test="${not empty allLabsSortedByVersions }">
                    <tr>
                        <td><h2>Labs</h2></td>
                    </tr>
                    <tr>
                        <td>
                            <ul id="labList" style="list-style-type: none;padding:0px;">
                                <li class="selectAllHeading ${allLabsSortedByVersions.size() > 20 ? 'flex' : ''}">
                                    <input id="selectAllLabs" type="checkbox" onclick="toggleSelectAll(this, 'lab_');"
                                           value="lab_check" title="Select/un-select all documents."/>
                                    <label for="selectAllLabs">Select all</label>
                                    <button class="show-all-button ${allLabsSortedByVersions.size() > 20 ? '' : 'hide'}"
                                            type="button" title="Show ${allLabsSortedByVersions.size() - 20} More Labs"
                                            onclick="showAll(this, 'lab')">Show ${allLabsSortedByVersions.size() - 20}
                                        More Labs
                                    </button>
                                </li>
                                <c:forEach items="${ allLabsSortedByVersions }" var="lab" varStatus="loop">
                                    <c:set var="labName" value="${fn:substring(lab.labName, 0, 30)}"/>
                                    <c:set var="totalVersions" value="${fn:length(lab.labVersionIds)}"/>
                                    <li class="lab ${loop.index > 19 ? 'hide' : ''}">
                                        <input class="lab_check attachable_check" type="checkbox" name="labNo"
                                               id="labNo${ lab.segmentID }" value="${lab.segmentID}"
                                               title="<c:out value='${ labName }' />"/>
                                        <label for="labNo${lab.segmentID}" title="<c:out value='${ labName }' />"><c:out
                                                value="${ labName }"/>&nbsp;</label>
                                        <label for="labNo${lab.segmentID}"
                                               class="lab-date">${lab.labDateFormated}</label>
                                        <c:if test="${not empty lab.labVersionIds}">
                                            &nbsp;<i class="caret collapse-arrow" onclick="toggleLabVersionList(this)"></i>&nbsp;
                                        </c:if>
                                        <button class="preview-button" type="button" title="Preview"
                                                onclick="getPdf('LAB', '${lab.segmentID}', 'method=renderLabPDF&segmentId=${lab.segmentID}')">
                                            Preview
                                        </button>
                                        <ul class="collapsible-content" style="list-style-type: none;padding:0px;">
                                            <c:forEach items="${ lab.labVersionIds }" var="version"
                                                       varStatus="versionLoop">
                                                <li>
                                                    <input class="lab_check attachable_check"
                                                           data-version="${totalVersions - versionLoop.index}"
                                                           type="checkbox" name="labNo" id="labNo${ version.key }"
                                                           value="${version.key}"
                                                           title="v${totalVersions - versionLoop.index} ${ labName }"/>
                                                    <em>
                                                        <label for="labNo${version.key}"
                                                               title="v${totalVersions - versionLoop.index} ${ labName }">
                                                            <c:out value="Earlier Version ${ totalVersions - versionLoop.index } of ${ totalVersions + 1 }"/>&nbsp;
                                                        </label>
                                                        <label for="labNo${version.key}"
                                                               class="lab-date">(${version.value})</label>
                                                    </em>
                                                    <button class="preview-button" type="button" title="Preview"
                                                            onclick="getPdf('LAB', '${version.key}', 'method=renderLabPDF&segmentId=${version.key}')">
                                                        Preview
                                                    </button>
                                                </li>
                                            </c:forEach>
                                        </ul>
                                    </li>
                                </c:forEach>
                            </ul>
                        </td>
                    </tr>
                </c:if>

                <c:if test="${not empty allHRMDocuments }">
                    <tr>
                        <td><h2>HRM</h2></td>
                    </tr>
                    <tr>
                        <td>
                            <ul id="hrmList" style="list-style-type: none;padding:0;">
                                <li class="selectAllHeading ${allHRMDocuments.size() > 20 ? 'flex' : ''}">
                                    <input id="selectAllHRMS" type="checkbox" onclick="toggleSelectAll(this, 'hrm_');"
                                           value="hrm_check" title="Select/un-select all HRM documents."/>
                                    <label for="selectAllHRMS">Select all</label>
                                    <button class="show-all-button ${allHRMDocuments.size() > 20 ? '' : 'hide'}"
                                            type="button" title="Show ${allHRMDocuments.size() - 20} More HRM"
                                            onclick="showAll(this, 'hrm')">Show ${allHRMDocuments.size() - 20} More HRM
                                    </button>
                                </li>
                                <c:forEach items="${ allHRMDocuments }" var="hrm" varStatus="loop">
                                    <li class="hrm ${loop.index > 19 ? 'hide' : ''}">
                                        <input class="hrm_check attachable_check" type="checkbox" name="hrmNo" id="hrmNo${ hrm['id'] }"
                                               value="${hrm['id']}" title="${e:forHtmlAttribute(hrm['name'])}"/>
                                        <label for="hrmNo${hrm['id']}">
                                            <c:out value="${ hrm['name'] } ${ hrm['report_date'] }"/>
                                        </label>
                                        <button class="preview-button" type="button" title="Preview"
                                                onclick="getPdf('HRM', '${hrm.id}', 'method=renderHrmPDF&hrmId=${hrm.id}')">
                                            Preview
                                        </button>
                                    </li>
                                </c:forEach>
                            </ul>
                        </td>
                    </tr>
                </c:if>

                <c:if test="${not empty allForms }">
                    <tr>
                        <td><h2>Forms (current only)</h2></td>
                    </tr>
                    <tr>
                        <td>
                            <ul id="formList" style="list-style-type: none;padding:0;">
                                <li class="selectAllHeading ${allForms.size() > 20 ? 'flex' : ''}">
                                    <input id="selectAllForms" type="checkbox" onclick="toggleSelectAll(this, 'form_');"
                                           value="form_check" title="Select/un-select all forms."/>
                                    <label for="selectAllForms">Select all</label>
                                    <button class="show-all-button ${allForms.size() > 20 ? '' : 'hide'}" type="button"
                                            title="Show ${allForms.size() - 20} More Forms"
                                            onclick="showAll(this, 'form')">Show ${allForms.size() - 20} More Forms
                                    </button>
                                </li>
                                <c:forEach items="${ allForms }" var="form" varStatus="loop">
                                    <li class="form ${loop.index > 19 ? 'hide' : ''}">
                                        <input class="form_check attachable_check" type="checkbox" name="formNo"
                                               id="formNo${ form.formId }" value="${form.formId}"
                                               title="${e:forHtmlAttribute(form.formName)}"/>
                                        <label for="formNo${form.formId}">
                                            <c:out value="${ form.formName } ${ form.getEdited() }"/>
                                        </label>
                                        <button class="preview-button" type="button" title="Preview"
                                                onclick="getPdf('FORM', '${form.formId}', 'method=renderFormPDF&formId=${form.formId}&formName=${form.formName}&demographicNo=${form.getDemoNo()}')">
                                            Preview
                                        </button>
                                    </li>
                                </c:forEach>
                            </ul>
                        </td>
                    </tr>
                </c:if>

                <c:if test="${not empty providerPublicDocs }">
                    <tr>
                        <td><h2>Public eDocs</h2></td>
                    </tr>
                    <tr>
                        <td>
                            <ul id="providerPublicDocumentList" style="list-style-type: none;padding:0px;">
                                <li class="selectAllHeading ${providerPublicDocs.size() > 20 ? 'flex' : ''}">
                                    <input id="selectAllProviderPublicDocuments" type="checkbox"
                                           onclick="toggleSelectAll(this, 'providerPublicDocument_');" value="providerPublicDocument_check"
                                           title="Select/un-select all public eDocs."/>
                                    <label for="selectAllProviderPublicDocuments">Select all</label>
                                    <button class="show-all-button ${providerPublicDocs.size() > 20 ? '' : 'hide'}"
                                            type="button" title="Show ${providerPublicDocs.size() - 20} More Public eDocs"
                                            onclick="showAll(this, 'providerPublicDoc')">Show ${providerPublicDocs.size() - 20} More
                                        Public eDocs
                                    </button>
                                </li>
                                <c:forEach items="${ providerPublicDocs }" var="document" varStatus="loop">
                                    <c:set var="isDeleted" value="${fn:contains(document.status, 'D')}"/>
                                    <c:set var="isAttached" value="${not empty attachedDocumentIds and attachedDocumentIds.contains(document.docId)}"/>
                                    <li class="providerPublicDoc ${loop.index > 19 ? 'hide' : ''} ${isDeleted ? 'deleted-doc' : ''}">
                                        <input class="providerPublicDocument_${isAttached ? 'pre_check' : 'check'} attachable_check" type="checkbox" name="docNo"
                                               id="publicDocNo${document.docId}" value="${document.docId}"
                                               ${isAttached ? 'checked="checked" data-pre-attached="true"' : ''}
                                               title="${e:forHtmlAttribute(document.description)}"/>
                                        <label for="publicDocNo${document.docId}"><c:out
                                                value="${ document.description } ${ document.observationDate }"/><c:if test="${isDeleted}"> (deleted)</c:if></label>
                                        <button class="preview-button" type="button" title="Preview"
                                                onclick="getPdf('DOC', '${document.docId}', 'method=renderEDocPDF&eDocId=${document.docId}')">
                                            Preview
                                        </button>
                                    </li>
                                </c:forEach>
                            </ul>
                        </td>
                    </tr>
                </c:if>

                <c:if test="${not empty providerPrivateDocs }">
                    <tr>
                        <td><h2>Private eDocs</h2></td>
                    </tr>
                    <tr>
                        <td>
                            <ul id="providerPrivateDocumentList" style="list-style-type: none;padding:0px;">
                                <li class="selectAllHeading ${providerPrivateDocs.size() > 20 ? 'flex' : ''}">
                                    <input id="selectAllProviderPrivateDocuments" type="checkbox"
                                           onclick="toggleSelectAll(this, 'providerPrivateDocument_');" value="providerPrivateDocument_check"
                                           title="Select/un-select all private eDocs."/>
                                    <label for="selectAllProviderPrivateDocuments">Select all</label>
                                    <button class="show-all-button ${providerPrivateDocs.size() > 20 ? '' : 'hide'}"
                                            type="button" title="Show ${providerPrivateDocs.size() - 20} More Private eDocs"
                                            onclick="showAll(this, 'providerPrivateDoc')">Show ${providerPrivateDocs.size() - 20} More
                                        Private eDocs
                                    </button>
                                </li>
                                <c:forEach items="${ providerPrivateDocs }" var="document" varStatus="loop">
                                    <c:set var="isDeleted" value="${fn:contains(document.status, 'D')}"/>
                                    <c:set var="isAttached" value="${not empty attachedDocumentIds and attachedDocumentIds.contains(document.docId)}"/>
                                    <c:set var="isForeign" value="${not empty foreignPrivateDocIds and foreignPrivateDocIds.contains(document.docId)}"/>
                                    <li class="providerPrivateDoc ${loop.index > 19 ? 'hide' : ''} ${isDeleted ? 'deleted-doc' : ''} ${isForeign ? 'foreign-doc' : ''}">
                                        <input class="providerPrivateDocument_${isAttached ? 'pre_check' : 'check'} attachable_check" type="checkbox" name="docNo"
                                               id="privateDocNo${document.docId}" value="${document.docId}"
                                               ${isAttached ? 'checked="checked" data-pre-attached="true"' : ''}
                                               title="${e:forHtmlAttribute(document.description)}"/>
                                        <label for="privateDocNo${document.docId}"><c:out
                                                value="${ document.description } ${ document.observationDate }"/><c:if test="${isDeleted}"> (deleted)</c:if><c:if test="${isForeign}"> (attached by another provider)</c:if></label>
                                        <button class="preview-button" type="button" title="Preview"
                                                onclick="getPdf('DOC', '${document.docId}', 'method=renderEDocPDF&eDocId=${document.docId}')">
                                            Preview
                                        </button>
                                    </li>
                                </c:forEach>
                            </ul>
                        </td>
                    </tr>
                </c:if>
            </table>
        </div>

        <div id="pdfPreview" class="preview-pane">
            <object id="pdfObject" class="hide" type="application/pdf" data="">
            </object>
            <div id="preview-filler" class="preview-filler">
                Click any item to preview
            </div>
        </div>
    </div>
</form>
</body>
</html>
