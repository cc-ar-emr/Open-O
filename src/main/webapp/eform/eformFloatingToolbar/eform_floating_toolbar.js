document.addEventListener("DOMContentLoaded", function(){


    /**
     * Trigger these functions every time this page loads.
     */
    removeElements();
    hideElements();
    addNavElement();
    disableTextareaResize();
    moveSubjectReverse();
    // Add eForm attachments
    addEFormAttachments();

    // If download EForm
    const isDownload = document.getElementById("isDownloadEForm") ? document.getElementById("isDownloadEForm").value : "false";
    if (isDownload && isDownload === "true") {
        downloadEForm();
    }

    // Handle EForm errors
    const error = document.getElementById("error") ? document.getElementById("error").value : "false";
    const errorMessage = document.getElementById("errorMessage") ? document.getElementById("errorMessage").value : "";
    if (error === "true") {
        showError(errorMessage);
    }

		// add listener to the subject element
		if(document.forms[0].elements["subject"]) {
			document.forms[0].elements["subject"].addEventListener("input", function () {
				document.getElementById("remote_eform_subject").value = this.value;
			})
			document.forms[0].elements["subject"].addEventListener("click", function () {
				document.getElementById("remote_eform_subject").value = this.value;
			})
		}

	const isSuccessAndAutoclose = document.getElementById("isSuccess_Autoclose") &&
		document.getElementById("isSuccess_Autoclose").value === 'true';
	if (isSuccessAndAutoclose) {
		showSuccessAlert(remoteClose);
	}

	// Tickler integration: wrap toolbar actions if eForm has tickler tags
	if (hasTicklerTags()) {
		// Tickler flow state: "idle", "inProgress", or "proceeding".
		// - "idle": no tickler flow active, next action triggers the dialog
		// - "inProgress": dialog is open, block any new toolbar actions
		// - "proceeding": dialog completed, allow the original action through
		//   (e.g. Print→Save chain where Save is called internally by Print)
		// Resets to "idle" on cancellation. Page reload naturally resets via new JS context.
		let _ticklerState = "idle";

		// Minimal API for dialog handlers to interact with tickler state.
		// Single window property to avoid polluting the global namespace.
		// Must be defined before initTicklerDialogs() which references it.
		window._ticklerIntegration = {
			resetHandled: function() { _ticklerState = "idle"; },
			markDialogButton: null // set by initTicklerDialogs closure
		};

		initTicklerDialogs();

		const _origSave = remoteSave;
		const _origPrint = remotePrint;
		const _origDownload = remoteDownload;
		const _origFax = remoteFax;
		const _origEmail = remoteEmail;
		const _origEdocument = remoteEdocument;

		function wrapWithTicklerCheck(originalFn) {
			return function() {
				if (_ticklerState === "proceeding") {
					return originalFn.apply(this, arguments);
				}
				if (_ticklerState === "inProgress") {
					return; // block — tickler dialog is still open
				}
				// Check mode at action time so dynamically toggled tags are respected
				const currentMode = getTicklerMode();
				if (!currentMode) {
					return originalFn.apply(this, arguments);
				}
				_ticklerState = "inProgress";
				const args = arguments;
				const self = this;
				const proceedCallback = function() {
					_ticklerState = "proceeding";
					try {
						originalFn.apply(self, args);
					} finally {
						window._ticklerIntegration.resetHandled();
					}
				};
				if (currentMode === "autoOpen") {
					promptTicklerAutoOpen(proceedCallback);
				} else if (currentMode === "autoSave") {
					promptTicklerAutoSave(proceedCallback);
				}
			};
		}

		window.remoteSave = wrapWithTicklerCheck(_origSave);
		window.remotePrint = wrapWithTicklerCheck(_origPrint);
		window.remoteDownload = wrapWithTicklerCheck(_origDownload);
		window.remoteFax = wrapWithTicklerCheck(_origFax);
		window.remoteEmail = wrapWithTicklerCheck(_origEmail);
		window.remoteEdocument = wrapWithTicklerCheck(_origEdocument);
	}
	});

window.onerror = function uncaughtExceptionHandler(message, source, lineNumber, colno, error) {
    // return alert('This eForm contains source code errors that will cause a failure of functionality or loss of data.\n\n' +
    // 	'Please go to OSCARGalaxy.org for an updated version of this eForm, or  if a new version is not available, contact info@oscarbc.ca to request a repair.\n\n' +
    // 	'E-forms are a community project managed by OSCAR BC; eForm collections are hosted on OSCAR Galaxy for download and import.\n\n' +
    // 	'Error Message:' + message);
    let eform = {};
    eform.formId = document.getElementById("fid").value;
    eform.error = message;
    let context = document.getElementById("context").value;
    jQuery.post(context + "/eform/logEformError.do", eform);
}

function getEForm() {
	let ef = document.forms['saveEForm'];
	if (!ef) {
		ef = Array.prototype.find.call(
			document.forms,
			f => f.action && f.action.includes('addEForm.do')
		);
	}
	return ef;
}

function submitEForm() {
	const ef = getEForm();
	if (!ef) {
		showErrorAlert();
		return false;
	}
	ef.submit();
	return true;
}

	/**
	 * Triggers the eForm save/submit function
	 */
function remoteSave() {

	try {
		// bind the spinner to the form submit event.
		jQuery('form').on('submit', function(e) {
			ShowSpin(true);
		});

		appendImageInputs();

		moveSubject();

		if (typeof saveRTL === "function") {
			window["saveRTL"]();
			document.RichTextLetter.submit();
			return true;
		}

		if (document.getElementsByName("SubmitButton") && document.getElementsByName("SubmitButton")[0]) {
			try {
				document.getElementsByName("SubmitButton")[0].click();
				return true;
			} catch (error) {
				showErrorAlert();
			}
		}

		if(typeof releaseDirtyFlag === "function")
		{
			window["releaseDirtyFlag"]();
		}

		if (typeof submission === "function") {
			try {
				window["submission"]();
				return submitEForm();
			} catch (e) {
				showErrorAlert();
			}
		}

		try {
			return submitEForm();
		} catch (e) {
			showErrorAlert();
		}

		HideSpin();
	} catch (e) {
		showErrorAlert();
	}

	return false;
}

/**
 * Triggers the eForm attach function
 */
jQuery(document).on('click', '*[data-poload]', function () {
    const demographicNo = document.getElementById("demographicNo").value;
    const fdid = document.getElementById("fdid").value;
    const context = document.getElementById("context").value;
    let trigger = jQuery(this);
    trigger.data('poload', context + '/previewDocs.do?method=fetchEFormDocuments&demographicNo=' + demographicNo + '&fdid=' + fdid);
    trigger.off('click');
    let title = trigger.attr("title");
    jQuery("#attachDocumentDisplay").load(trigger.data('poload'), function (response, status, xhr) {
        if (status === "success") {
            // Disable the floating toolbar when the attachment window opens
            const eformFloatingToolbar = document.getElementById("eform_floating_toolbar");
            eformFloatingToolbar.classList.add("disabled-toolbar");

            jQuery('#attachDocumentList').find(".delegateAttachment").each(function (index, data) {
                let delegateKey = this.id.split("_")[1];

                // DOC sections share name="docNo" with distinct ids; look up by name+value.
                // Skip if server-pre-attached; else mark as unsaved client-side selection.
                if (jQuery(this).data("delegate-type") === "doc") {
                    let docValue = this.value;
                    let matches = jQuery('#attachDocumentsForm').find('input[name="docNo"][value="' + docValue + '"]');
                    if (matches.filter('[data-pre-attached="true"]').length > 0) {
                        return;
                    }
                    matches.prop("checked", true).attr("data-pre-attached", "true");
                    return;
                }

                let delegate = "#" + delegateKey;
                let element = jQuery('#attachDocumentsForm').find(delegate);
                if (element.length === 0) {
                    element = addFormIfNotFound(data, demographicNo, delegate);
                }
                element.prop("checked", true).attr("data-pre-attached", "true");

                // Expand list if selected lab is older version
                if (element.attr('data-version')) {
                    expandLabVersionList(element.parent().parent().parent().find('.collapse-arrow'));
                }
            });

            syncPreCheckedToDelegates('#attachDocumentList', false);
        }
    }).dialog({
        title: title,
        modal: true,
        closeText: "Save and Close",
        height: 'auto',
        width: 'auto',
        resizable: true,
        open: function (event, ui) {
            jQuery(this).parent().css({
                top: 0,
                left: 0
            });

            let closeBtn = jQuery(this).parent().find(".ui-dialog-titlebar-close");
            closeBtn.removeClass("ui-button-icon-only");
            closeBtn.addClass("save-and-close-button");
            closeBtn.html("Save and Close");
        },

        beforeClose: function (event, ui) {
            // before the dialog is closed:

            if (!confirmPrivateDocsIfAny('#attachDocumentsForm')) {
                return false;
            }

            // check if list exists, if yes then empty it otherwise create new
            if (jQuery('#attachDocumentList').length === 0) {
                const attachDocumentList = jQuery('<div>', {'id': 'attachDocumentList'});
                jQuery('form:first').append(attachDocumentList);
            }
            jQuery('#attachDocumentList').empty();

            // Cross-section dedupe: same docNo can render in patient + provider sections.
            const seenDelegates = new Set();
            jQuery('#attachDocumentsForm').find(
                ".attachable_check:checkbox:checked:not(input[disabled='disabled'])"
            ).each(function () {
                const $el = jQuery(this);
                const key = $el.attr('name') + "::" + $el.val();
                if (seenDelegates.has(key)) return;
                seenDelegates.add(key);
                jQuery('#attachDocumentList').append(buildDelegateInput($el));
            });

            // show total attachments
            jQuery('#remoteTotalAttachments').empty().append(jQuery('.delegateAttachment').length);

            // Enable the floating toolbar when the attachment window closes
            const eformFloatingToolbar = document.getElementById("eform_floating_toolbar");
            eformFloatingToolbar.classList.remove("disabled-toolbar");
        }
    });
});

/**
 * This function adds the old form to the attachment window only if that form is displayed in the consultForm/eForm attachments.
 * The attachment window only displays the latest (updated) forms.
 */
function addFormIfNotFound(form, demographicNo, delegate) {
    const checkboxName = form.getAttribute('name');
    const formValue = form.getAttribute('value');
    const formId = "formNo" + formValue;
    const formName = document.getElementById("entry_" + formId).getAttribute('data-formName');
    const formDate = document.getElementById("entry_" + formId).getAttribute('data-formDate');

    const checkbox = jQuery('<input>', {
        class: 'form_check attachable_check',
        type: 'checkbox',
        name: checkboxName,
        id: formId,
        value: formValue,
        title: formName
    });

    const label = jQuery('<label>', {
        for: formId,
        text: "(Not Latest Version) " + formName + " " + formDate
    });

    const previewButton = jQuery('<button>', {
        class: 'preview-button',
        type: 'button',
        text: 'Preview',
        title: 'Preview'
    }).click(function () {
        getPdf('FORM', formValue, 'method=renderFormPDF&formId=' + formValue + '&formName=' + formName + '&demographicNo=' + demographicNo);
    });

    const newLiFormElement = jQuery('<li>', {
        class: 'form',
    }).append(checkbox).append(label).append(previewButton);
    jQuery('#formList').find('.selectAllHeading').after(newLiFormElement);

    return jQuery('#attachDocumentsForm').find(delegate);
}

function addEFormAttachments() {
    const eFormAttachments = jQuery('.delegateAttachment');
    const attachDocumentList = jQuery('<div>', {'id': 'attachDocumentList'});
    jQuery('form:first').append(attachDocumentList);
    eFormAttachments.appendTo(attachDocumentList);

    // Old form versions
    const oldVersionForms = jQuery('.delegateOldFormAttachment');
    const eForm = jQuery('#FormName');
    oldVersionForms.appendTo(eForm);
}

/**
 * Adds a hidden input field into the eForm form with instructions to
 * open 'Save as' window dialog
 */
function remoteDownload() {
    ShowSpin(true);
    const newElement = document.createElement("input");
    newElement.setAttribute("id", "saveAndDownloadEForm");
    newElement.setAttribute("name", "saveAndDownloadEForm");
    newElement.setAttribute("value", "true");
    newElement.setAttribute("type", "hidden");
    document.forms[0].appendChild(newElement);

    remoteSave();
}

function downloadEForm() {
    const eFormPDF = document.getElementById("eFormPDF").value;
    const eFormPDFName = document.getElementById("eFormPDFName").value;
    if (!eFormPDF && !eFormPDFName) {
        return;
    }
    const pdfData = new Uint8Array(atob(eFormPDF).split('').map(char => char.charCodeAt(0)));
    const pdfBlob = new Blob([pdfData], {type: 'application/pdf'});
    const downloadLink = document.createElement('a');
    downloadLink.href = URL.createObjectURL(pdfBlob);
    downloadLink.download = eFormPDFName;
    downloadLink.click();
    URL.revokeObjectURL(downloadLink.href);
    document.getElementById("eFormPDF").value = "";
    document.getElementById("eFormPDFName").value = "";
}

/**
 * Adds a hidden input field into the eForm form with instructions to
 * open the Oscar Fax dialog.
 */
function remoteFax() {
    const newElement = document.createElement("input");
    newElement.setAttribute("id", "faxAction");
    newElement.setAttribute("name", "faxEForm");
    newElement.setAttribute("value", "true");
    newElement.setAttribute("type", "hidden");
    document.forms[0].appendChild(newElement);

    /*
     * This helps carry forward the select list values of fax recipients
     * from the eForm.
     */
    const faxnumList = document.getElementById("faxnumList");
    if (faxnumList) {
        const selectedOption = faxnumList.options[faxnumList.options.selectedIndex];
        const recipientFaxNumber = selectedOption.getAttribute("value");
        const recipient = selectedOption.getAttribute('name');

        if (recipientFaxNumber) {
            const recipientElement = document.createElement("input");
            recipientElement.setAttribute("id", "recipient");
            recipientElement.setAttribute("name", "recipient");
            recipientElement.setAttribute("value", recipient);
            recipientElement.setAttribute("type", "hidden");

            document.forms[0].appendChild(recipientElement);

            const recipientNumberElement = document.createElement("input");
            recipientNumberElement.setAttribute("id", "recipientFaxNumber");
            recipientNumberElement.setAttribute("name", "recipientFaxNumber");
            recipientNumberElement.setAttribute("value", recipientFaxNumber);
            recipientNumberElement.setAttribute("type", "hidden");

            document.forms[0].appendChild(recipientNumberElement);
        }
    }

    remoteSave();
}

/**
 * Adds a hidden input field into the eForm form with instructions to
 * open the Oscar Email dialog.
 */
function remoteEmail() {
    if (!document.getElementById("hasValidRecipient") || !document.getElementById("emailConsentStatus") || !document.getElementById("emailConsentName")) {
        alert("Valid recipient or consent parameter is not defined in the EForm.");
        return;
    }

    const hasValidRecipient = document.getElementById("hasValidRecipient").value;
    const emailConsentStatus = document.getElementById("emailConsentStatus").value;
    const emailConsentName = document.getElementById("emailConsentName").value;

    if (hasValidRecipient === "false") {
        alert("Sorry - this patient does not have a valid email address in their demographic. Please update their demographic and try again.");
        return;
    }

    if (emailConsentStatus !== "Explicit Opt-In") {
        const userResponse = prompt("This patient has not explicitly opted-in: [" + emailConsentName + "]\nType 'Yes' to acknowledge you understand the risks before proceeding.", "No");
        if (userResponse === null || userResponse.toLowerCase() !== 'yes') {
            return;
        }
    }

    const newElement = document.createElement("input");
    newElement.setAttribute("id", "emailAction");
    newElement.setAttribute("name", "emailEForm");
    newElement.setAttribute("value", "true");
    newElement.setAttribute("type", "hidden");
    document.forms[0].appendChild(newElement);
    remoteSave();

}

/**
 * Triggers the eForm print function
 */
function remotePrint() {

    if (typeof formPrint === "function") {
        try {
            console.log("Printing document remotely with formPrint method");
            formPrint();
        } catch (e) {
            console.log("Eform returns fatal error while using formPrint function " + e);
            hailMary();
        }
    } else if (typeof printLetter === "function") {
        try {
            console.log("Printing document remotely with printLetter method")
            printLetter();
        } catch (e) {
            console.log("Eform returns fatal error while using printLetter function " + e);
            hailMary();
        }
    } else if (document.getElementsByName("PrintButton") && document.getElementsByName("PrintButton")[0]) {
        try {
            console.log(document.getElementsByName("PrintButton"));
            console.log("Remotely clicking button with name PrintButton");
            document.getElementsByName("PrintButton")[0].click();
        } catch (e) {
            console.log("Error locating PrintButton " + e);
            hailMary();
        }
    } else if (document.getElementById('edit')) {
        try {
            console.log("Content has been edited and no print method was found. Executing window.print");
            document.getElementById('edit').contentWindow.print();
        } catch (e) {
            console.log("Error locating PrintButton " + e);
            hailMary();
        }
    } else {
        hailMary()
    }

		/*
		 * Needs to be saved if this is
		 * a new eForm or it has been altered.
		 */
		if(typeof needToConfirm !== 'undefined' && needToConfirm) {
			console.log("eForm needs to be saved.")
			remoteSave();
		}

		/*
		 * either the eForm has no dirty form detection, or dirty form
		 * detection did not flag a change; confirm before saving.
		 */
		else if(confirm("You haven't manually edited this document, would you like to save a copy to the patient's chart regardless?")) {
			remoteSave();
		}
}

function hailMary() {
    console.log("Just do window print.")
    try {
        window.print();
    } catch {
        alert("Cannot print. Try the print button on the eForm.");
    }
}

/**
 * Adds a hidden input field into the eForm form with instructions to
 * to generate a PDF of this form and then to
 * save it into the eChart Documents directory.
 */
function remoteEdocument() {

    const edocElement = document.getElementById("saveAsEdoc");
    if (edocElement) {
        edocElement.value = 'true';
    } else {
        const newElement = document.createElement("input");
        newElement.setAttribute("id", "saveAsEdoc");
        newElement.setAttribute("name", "saveAsEdoc");
        newElement.setAttribute("value", "true");
        newElement.setAttribute("type", "hidden");
        document.forms[0].appendChild(newElement);
    }

    remoteSave();

}

/**
 * Close the entire eForm window.
 */
function remoteClose() {
    window.close();
}

// ============================================================
// Tickler Integration for eForms
// Supports two modes via hidden input tags in the eForm HTML:
//   - ticklerAutoOpen: interactive popup (ticklerAdd.jsp)
//   - ticklerAutoSave: silent REST creation with confirmation
// ============================================================

/**
 * Detects whether this eForm has tickler integration tags.
 * Returns true if either ticklerAutoOpen or ticklerAutoSave elements exist,
 * false otherwise. Skips if the eForm Generator's tickler function is present.
 */
function hasTicklerTags() {
    if (typeof setAtickler === 'function') {
        return false;
    }
    return !!(document.getElementById("ticklerAutoOpen") || document.getElementById("ticklerAutoSave"));
}

/**
 * Returns the current tickler mode by checking hidden input values at call time.
 * This allows eForms to toggle tickler mode dynamically (e.g. via a checkbox)
 * after page load. Returns "autoOpen", "autoSave", or null.
 */
function getTicklerMode() {
    const autoOpen = document.getElementById("ticklerAutoOpen");
    if (autoOpen && autoOpen.value && autoOpen.value.toLowerCase() === "true") {
        return "autoOpen";
    }
    const autoSave = document.getElementById("ticklerAutoSave");
    if (autoSave && autoSave.value && autoSave.value.toLowerCase() === "true") {
        return "autoSave";
    }
    return null;
}

/**
 * Reads a hidden input value from the eForm, returning a default if not found.
 */
function getTicklerFieldValue(fieldId, defaultValue) {
    const el = document.getElementById(fieldId);
    if (el && el.value && el.value.trim() !== "") {
        return el.value.trim();
    }
    return defaultValue;
}

/**
 * Collects tickler data from hidden inputs in the eForm.
 */
function collectTicklerData() {
    const today = new Date();
    const yyyy = today.getFullYear();
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const dd = String(today.getDate()).padStart(2, '0');
    const todayStr = yyyy + '-' + mm + '-' + dd;

    return {
        demographicNo: getTicklerFieldValue("demographicNo", ""),
        serviceDate: getTicklerFieldValue("ticklerServiceDate", todayStr),
        priority: getTicklerFieldValue("ticklerPriority", "Normal"),
        taskAssignedTo: getTicklerFieldValue("ticklerAssignedTo", getTicklerFieldValue("providerNo", "")),
        message: getTicklerFieldValue("ticklerMessage", "")
    };
}

/**
 * Creates a div element with the given id and title attribute, used as a
 * container for jQuery UI dialogs (the title becomes the dialog's titlebar).
 */
function createDialogDiv(id, title) {
    const div = document.createElement("div");
    div.id = id;
    div.title = title;
    return div;
}

/**
 * Creates and initializes the jQuery UI dialogs used by the tickler integration.
 */
function initTicklerDialogs() {
    if (document.getElementById("ticklerConfirmDialog")) return;

    // Scoped overlay style so we don't override every jQuery UI dialog overlay
    // app-wide. Applied only to overlays we tag with the tickler-overlay class.
    const overlayStyle = document.createElement("style");
    overlayStyle.textContent = ".tickler-overlay { background: #000 !important; opacity: 0.4 !important; }";
    document.head.appendChild(overlayStyle);

    // Confirm dialog (auto-open mode)
    const confirmDiv = createDialogDiv("ticklerConfirmDialog", "Create Tickler");
    const confirmP = document.createElement("p");
    confirmP.id = "ticklerConfirmMessage";
    confirmDiv.appendChild(confirmP);
    document.body.appendChild(confirmDiv);

    // Proceed dialog (after popup closes)
    const proceedDiv = createDialogDiv("ticklerProceedDialog", "Tickler Window Closed");
    const proceedP = document.createElement("p");
    proceedP.textContent = "The tickler window has closed. Ready to proceed?";
    proceedDiv.appendChild(proceedP);
    document.body.appendChild(proceedDiv);

    // Auto-save success dialog
    const successDiv = createDialogDiv("ticklerAutoSaveSuccessDialog", "Tickler Created");
    const successP1 = document.createElement("p");
    successP1.textContent = "A tickler has been automatically created. ";
    const viewLink = document.createElement("a");
    viewLink.id = "ticklerViewLink";
    viewLink.href = "#";
    viewLink.target = "_blank";
    viewLink.textContent = "Click here to view it.";
    successP1.appendChild(viewLink);
    successDiv.appendChild(successP1);
    const successP2 = document.createElement("p");
    successP2.textContent = "Click OK to proceed.";
    successDiv.appendChild(successP2);
    document.body.appendChild(successDiv);

    // Auto-save error dialog
    const errorDiv = createDialogDiv("ticklerAutoSaveErrorDialog", "Tickler Error");
    const errorP1 = document.createElement("p");
    errorP1.textContent = "Tickler creation failed: ";
    const errorDetail = document.createElement("span");
    errorDetail.id = "ticklerErrorDetail";
    errorP1.appendChild(errorDetail);
    errorDiv.appendChild(errorP1);
    const errorP2 = document.createElement("p");
    errorP2.textContent = "Click OK to proceed anyway, or Cancel to stop.";
    errorDiv.appendChild(errorP2);
    document.body.appendChild(errorDiv);

    // Tracks whether a dialog was closed by a button callback (true) vs the
    // X button or Escape key (false). Scoped to this closure to avoid globals.
    let _dialogButtonClicked = false;

    window._ticklerIntegration.markDialogButton = function() { _dialogButtonClicked = true; };

    function ticklerDialogClose() {
        if (!_dialogButtonClicked) {
            window._ticklerIntegration.resetHandled();
        }
        _dialogButtonClicked = false;
    }

    jQuery("#ticklerConfirmDialog").dialog({
        autoOpen: false,
        modal: true,
        width: 420,
        buttons: {},
        close: ticklerDialogClose
    });

    jQuery("#ticklerProceedDialog").dialog({
        autoOpen: false,
        modal: true,
        width: 420,
        buttons: {},
        close: ticklerDialogClose
    });

    jQuery("#ticklerAutoSaveSuccessDialog").dialog({
        autoOpen: false,
        modal: true,
        width: 420,
        buttons: {},
        close: ticklerDialogClose
    });

    jQuery("#ticklerAutoSaveErrorDialog").dialog({
        autoOpen: false,
        modal: true,
        width: 420,
        buttons: {},
        close: ticklerDialogClose
    });

    // Style dialog close (X) buttons to fix Bootstrap/jQuery UI CSS conflict
    styleTicklerDialogCloseButton("#ticklerConfirmDialog");
    styleTicklerDialogCloseButton("#ticklerProceedDialog");
    styleTicklerDialogCloseButton("#ticklerAutoSaveSuccessDialog");
    styleTicklerDialogCloseButton("#ticklerAutoSaveErrorDialog");
}

/**
 * Styles a jQuery UI dialog close button with an X character instead of the
 * default icon sprite. Resolves the Bootstrap/jQuery UI CSS conflict where
 * the close icon renders as an empty square.
 */
function styleTicklerDialogCloseButton(dialogSelector) {
    jQuery(dialogSelector).on("dialogopen", function() {
        // Tag the overlay so our scoped CSS rule applies (jQuery UI creates
        // a fresh overlay element each time a modal dialog opens).
        jQuery(".ui-widget-overlay").last().addClass("tickler-overlay");

        const closeBtn = jQuery(this).parent().find(".ui-dialog-titlebar-close");
        const titlebar = jQuery(this).parent().find(".ui-dialog-titlebar");
        titlebar.css("padding", "10px 14px");
        closeBtn.empty()
            .removeClass("ui-button-icon-only ui-button-icon ui-icon ui-icon-closethick")
            .attr("aria-label", "Close")
            .text("×")
            .css({
                "width": "24px",
                "height": "24px",
                "font-size": "18px",
                "line-height": "1",
                "top": "50%",
                "margin-top": "-12px"
            });
    });
}

/**
 * Auto-open flow: shows a confirm dialog, then opens ticklerAdd.jsp in a popup.
 * After the popup closes, asks the user if they want to proceed with the original action.
 */
function promptTicklerAutoOpen(proceedCallback) {
    jQuery("#ticklerConfirmDialog").dialog("option", "buttons", {
        "Yes": function() {
            window._ticklerIntegration.markDialogButton();
            jQuery(this).dialog("close");
            openTicklerPopup(proceedCallback);
        },
        "No": function() {
            window._ticklerIntegration.markDialogButton();
            jQuery(this).dialog("close");
            proceedCallback();
        }
    });
    document.getElementById("ticklerConfirmMessage").textContent =
        getTicklerFieldValue("ticklerPromptMessage", "Do you want to create a tickler first?");
    jQuery("#ticklerConfirmDialog").dialog("open");
}

/**
 * Opens ticklerAdd.jsp in a popup window with pre-populated params.
 * Passes updateParent=false so that dbTicklerAdd.jsp does not reload the
 * opener window (which would destroy our poll timer and proceed callback).
 * Polls for popup close, then shows the proceed dialog.
 */
function openTicklerPopup(proceedCallback) {
    const data = collectTicklerData();
    const contextPath = getTicklerFieldValue("context", "");
    const params = "demographic_no=" + encodeURIComponent(data.demographicNo)
        + "&taskTo=" + encodeURIComponent(data.taskAssignedTo)
        + "&priority=" + encodeURIComponent(data.priority)
        + "&xml_appointment_date=" + encodeURIComponent(data.serviceDate)
        + "&updateParent=false"
        + "&parentAjaxId=";

    // Pass the tickler message via opener (same-origin) instead of the URL,
    // since it may contain PHI that would otherwise leak into access logs and
    // browser history. ticklerAdd.jsp reads window.opener._eformTicklerPrefillMessage.
    window._eformTicklerPrefillMessage = data.message;

    const url = contextPath + "/tickler/ticklerAdd.jsp?" + params;
    const popup = window.open(url, "ticklerPopup", "height=600,width=800,scrollbars=yes,resizable=yes");

    if (!popup) {
        alert("The tickler popup was blocked by your browser. Please allow popups for this site and try again.");
        window._ticklerIntegration.resetHandled();
        return;
    }

    // Keep the modal overlay visible while the tickler popup is open, so the
    // parent page stays visually "locked" between the confirm dialog and the
    // proceed dialog (which would otherwise leave a gap with no overlay).
    showTicklerWaitingOverlay();

    const pollTimer = setInterval(function() {
        if (popup.closed) {
            clearInterval(pollTimer);
            hideTicklerWaitingOverlay();
            showProceedDialog(proceedCallback);
        }
    }, 500);
}

/**
 * Adds a manual overlay div mirroring jQuery UI's modal overlay, used to keep
 * the parent page dimmed while the tickler popup window is open (no jQuery UI
 * dialog is active during that phase, so its overlay would otherwise vanish).
 */
function showTicklerWaitingOverlay() {
    if (document.getElementById("ticklerWaitingOverlay")) return;
    const overlay = document.createElement("div");
    overlay.id = "ticklerWaitingOverlay";
    overlay.className = "ui-widget-overlay tickler-overlay";
    document.body.appendChild(overlay);
}

function hideTicklerWaitingOverlay() {
    const overlay = document.getElementById("ticklerWaitingOverlay");
    if (overlay) overlay.parentNode.removeChild(overlay);
}

/**
 * After the tickler popup closes, asks the user if they want to proceed.
 */
function showProceedDialog(proceedCallback) {
    jQuery("#ticklerProceedDialog").dialog("option", "buttons", {
        "Yes, proceed": function() {
            window._ticklerIntegration.markDialogButton();
            jQuery(this).dialog("close");
            proceedCallback();
        },
        "No, cancel": function() {
            window._ticklerIntegration.markDialogButton();
            jQuery(this).dialog("close");
            window._ticklerIntegration.resetHandled();
        }
    });
    jQuery("#ticklerProceedDialog").dialog("open");
}

/**
 * Auto-save flow: silently creates a tickler via REST, then shows success or error dialog.
 */
function promptTicklerAutoSave(proceedCallback) {
    const data = collectTicklerData();
    const contextPath = getTicklerFieldValue("context", "");

    // Append local timezone offset so Jackson interprets the date as local midnight,
    // not UTC midnight (which would shift to the previous day in negative UTC offsets).
    const tzOffset = new Date().getTimezoneOffset();
    const tzSign = tzOffset <= 0 ? "+" : "-";
    const tzHours = String(Math.floor(Math.abs(tzOffset) / 60)).padStart(2, '0');
    const tzMinutes = String(Math.abs(tzOffset) % 60).padStart(2, '0');
    const serviceDateWithTz = data.serviceDate + "T00:00:00" + tzSign + tzHours + ":" + tzMinutes;

    const payload = {
        demographicNo: data.demographicNo,
        message: data.message,
        taskAssignedTo: data.taskAssignedTo,
        priority: data.priority,
        serviceDate: serviceDateWithTz,
        status: "A"
    };

    jQuery.ajax({
        url: contextPath + "/ws/rs/tickler/add",
        type: "POST",
        contentType: "application/json",
        data: JSON.stringify(payload),
        dataType: "json",
        success: function(resp) {
            if (resp && resp.success) {
                const ticklerId = (resp.message != null) ? String(resp.message).trim() : "";
                if (!ticklerId) {
                    showTicklerError("Server reported success but did not return a tickler id.", proceedCallback);
                    return;
                }
                const viewUrl = contextPath + "/tickler/ticklerEdit.jsp?tickler_no=" + encodeURIComponent(ticklerId);
                jQuery("#ticklerViewLink").attr("href", viewUrl);
                jQuery("#ticklerAutoSaveSuccessDialog").dialog("option", "buttons", {
                    "OK": function() {
                        window._ticklerIntegration.markDialogButton();
                        jQuery(this).dialog("close");
                        proceedCallback();
                    }
                });
                jQuery("#ticklerAutoSaveSuccessDialog").dialog("open");
            } else {
                const detail = (resp && resp.message)
                    ? resp.message
                    : "Server returned unsuccessful response.";
                showTicklerError(detail, proceedCallback);
            }
        },
        error: function(xhr) {
            let detail = "HTTP " + xhr.status;
            if (xhr.responseText) {
                try {
                    const errResp = JSON.parse(xhr.responseText);
                    if (errResp.message) detail = errResp.message;
                } catch (e) {
                    // use default detail
                }
            }
            showTicklerError(detail, proceedCallback);
        }
    });
}

/**
 * Shows the error dialog for auto-save failures.
 */
function showTicklerError(detail, proceedCallback) {
    jQuery("#ticklerErrorDetail").text(detail);
    jQuery("#ticklerAutoSaveErrorDialog").dialog("option", "buttons", {
        "OK (proceed anyway)": function() {
            window._ticklerIntegration.markDialogButton();
            jQuery(this).dialog("close");
            proceedCallback();
        },
        "Cancel": function() {
            window._ticklerIntegration.markDialogButton();
            jQuery(this).dialog("close");
            window._ticklerIntegration.resetHandled();
        }
    });
    jQuery("#ticklerAutoSaveErrorDialog").dialog("open");
}

/**
 * Move the eForm subject value from the remote tool bar into the
 * eForm form.
 * Should be done just before the save process.
 */
function moveSubject() {
    let remoteSubject = document.getElementById("remote_eform_subject");
    let remoteSubjectValue;

    if (remoteSubject) {
        remoteSubjectValue = remoteSubject.value;
    }

    let localSubject = document.forms[0].elements["subject"];
    if (localSubject) {
        localSubject.value = remoteSubjectValue;
    }
}

function moveSubjectReverse() {
    let subjectElement = document.forms[0].elements["subject"];
    let subjectElementValue;

    if (subjectElement) {
        subjectElementValue = subjectElement.value;
    } else // create the subject element for later
    {
        subjectElement = document.createElement("input");
        subjectElement.id = "subject";
        subjectElement.name = "subject";
        subjectElement.type = "hidden";
        document.forms[0].appendChild(subjectElement);
    }

    let localSubject = document.getElementById("remote_eform_subject");
    if (localSubject) {
        localSubject.value = subjectElementValue;
    }
}

/**
 * Close this toolbar. Exposes buttons and text that is
 * hidden underneath.
 * A button is still visible on the right side to
 * restore the toolbar.
 */
function closeToolbar() {

    let toolbarContainer = document.getElementById("eform_floating_toolbar");
    let toolbarNav = document.getElementById("eform_floating_toolbar_nav");
    if (toolbarContainer && toolbarNav) {
        toolbarNav.style.display = "none";

        toolbarContainer.style.display = "table";
        toolbarContainer.style.position = "fixed";
        toolbarContainer.style.opacity = "100%";
        toolbarContainer.style.zIndex = "1029";
        toolbarContainer.style.bottom = "0";
        toolbarContainer.style.right = "0";
        toolbarContainer.style.marginBottom = "0";

        const openToolbarButton = document.getElementById("openToolbarButton");
        openToolbarButton.style.display = "table";
        openToolbarButton.style.minHeight = "50px";

    }
}

/**
 * Restore the floating toolbar.
 * @returns
 */
function openToolbar() {
    const openToolbarButton = document.getElementById("openToolbarButton");
    const toolbarNav = document.getElementById("eform_floating_toolbar_nav");
    const toolbarContainer = document.getElementById("eform_floating_toolbar");
    if (toolbarContainer && openToolbarButton && toolbarNav) {
        toolbarContainer.removeAttribute("style");
        toolbarNav.removeAttribute("style");
        openToolbarButton.style.display = "none";
    }
}

/**
 * Remove all fax control buttons from the current
 * eform to avoid any confusion on what fax system is being used.
 */
function removeElements() {
    let element = document.getElementById("faxControl");

    if (element) {
        element.parentNode.removeChild(element);
    }

    element = document.querySelectorAll("script");
    const scriptArray = Array.from(element);

    if (scriptArray.length > 0) {
        const script = scriptArray.find(script => script.src.includes("faxControl.js"))
        if (script) {
            script.parentNode.removeChild(script);
        }
    }

    element = document.getElementById("fax_button");

    if (element) {
        element.parentNode.removeChild(element);

        /*
         * add a dummy placeholder back in because the eForm developers
         * created a hard dependency on the existence of this element.
         */
        const inputElement = document.createElement("input");
        inputElement.setAttribute("type", "hidden");
        inputElement.setAttribute("id", "fax_button");
        document.forms[0].appendChild(inputElement);
    }

    element = document.getElementById("faxSave_button");

    if (element) {
        element.parentNode.removeChild(element);

        /*
         * add a dummy placeholder back in because the eForm developers
         * created a hard dependency on the existence of this element.
         */
        const inputElement = document.createElement("input");
        inputElement.setAttribute("type", "hidden");
        inputElement.setAttribute("id", "faxSave_button");
        document.forms[0].appendChild(inputElement);
    }

    element = document.getElementById("faxEForm");

    if (element) {
        element.parentNode.removeChild(element);

        /*
         * add a dummy placeholder back in because the eForm developers
         * created a hard dependency on the existence of this element.
         */
        const inputElement = document.createElement("input");
        inputElement.setAttribute("type", "hidden");
        inputElement.setAttribute("id", "faxEForm");
        document.forms[0].appendChild(inputElement);
    }

    /*
     * sometimes these are in there too.
     */
    let inputElement = document.createElement("input");
    inputElement.setAttribute("type", "hidden");
    inputElement.setAttribute("id", "otherFaxInput");
    document.forms[0].appendChild(inputElement);
}

/**
 * A wrapper function to dismiss uncaught exceptions for when
 * this function contained in the removed faxControl.js file is
 * called.
 * Do nothing.
 */
function AddOtherFax() {
    // do nothing
    return false;
}

/**
 * Many eforms will already have various buttons for printing, submitting, etc.
 * These buttons should not necessarily be removed because remotesave() and remoteprint() may rely on these buttons
 * To avoid user confusion as to which button to click, this function hides these buttons
 */
function hideElements() {
    const idsOfButtonsToHide = ["SubmitButton", "ResetButton", "PrintButton", "PrintSubmitButton"];
    for (let i = 0; i < idsOfButtonsToHide.length; i++) {
        let el = document.getElementById(idsOfButtonsToHide[i]);

        if (!el) {
            el = document.getElementsByName(idsOfButtonsToHide[i]);
        }

        if (el && el.constructor === NodeList && el.length > 0) {
            for (let i = 0; i < el.length; i++) {
                el[i].style.display = "none";
            }
        } else if (el && el.constructor !== NodeList) {
            el.style.display = "none";
        }
    }
}

/**
 * A javascript includes method
 * @returns
 */
function includeHTML(elmnt) {
    const file = "../eform/eformFloatingToolbar/eform_floating_toolbar.jspf";
    const xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function () {
        if (this.readyState === 4) {

            if (this.status === 200) {
                let toolbarWrapper = document.createElement("div");
                toolbarWrapper.setAttribute("id", "toolbarWrapper");
                toolbarWrapper.setAttribute("class", "hidden-print DoNotPrint no-print");
                toolbarWrapper.innerHTML = this.responseText;
                elmnt.append(toolbarWrapper);

                // After adding floating toolbar update number of attachments
                jQuery('#remoteTotalAttachments').empty().append(jQuery('.delegateAttachment').length);

					// Check email privilege and if not, hide the Email button.
					handleEmailPrivilege();
            }

            if (this.status === 404) {
                elmnt.append("eForm tool bar not found.");
            }
        }
    }
    xhttp.open("GET", file, true);
    xhttp.send();
    /* Exit the function: */
    return;

}

/**
 * Insert additional elements into the eForm to support
 * launch of the floating toolbar.
 */
function addNavElement() {

    /*
     * Get the total height of the current eform
     */
    let body = document.body;
    let html = document.documentElement;
    let documentheight = Math.max(body.scrollHeight, body.offsetHeight,
        html.clientHeight, html.scrollHeight, html.offsetHeight);

    /*
     * Include the eForm tool bar overlay
     */
    includeHTML(body);

    /*
     * Add a wedge to the bottom of the eform that will add
     * 65 pixels to the bottom so that the eForm clears the remote button
     * panel
     */
    let formelement = document.getElementsByTagName("form");
    let spacer = document.createElement("div");
    spacer.setAttribute("id", "eformPageSpacer");
    spacer.setAttribute("class", "hidden-print DoNotPrint no-print");
    spacer.style.position = "absolute";
    spacer.style.left = 0;
    spacer.style.top = documentheight + 50;
    spacer.style.width = "100%";
    spacer.style.margin = 0;
    spacer.style.padding = 0;
    spacer.style.height = "1px";
    formelement[0].appendChild(spacer);

    /*
     * Place the new CSS style into the parent page.
     */
    let headelement = document.getElementsByTagName("head");
    let style = document.createElement("link");
    style.setAttribute("rel", "stylesheet");
    style.setAttribute("type", "text/css");
    style.setAttribute("href", "../library/bootstrap/3.0.0/css/eform_floating_toolbar_bootstrap_custom.min.css");
    headelement[0].appendChild(style);

}

function showError(message) {
    if (!message) {
        message = "Failed to process eForm. Please refer to the server logs for more details."
    }
    alert(message.replace(/\\n/g, "\n"));
}

/*
 * Show or hide the loading spinner
 * if locked is true: can't click away
 * if locked is false: can click away from it
 */
function ShowSpin(locked) {
    let screen = document.getElementById("oscar-spinner-screen");
    let spinner = document.getElementById("oscar-spinner");

    screen.classList.add("active-oscar-spinner");
    spinner.classList.add("active-oscar-spinner");

    if (locked) {
        screen.removeEventListener("click", HideSpin);
    } else {
        screen.addEventListener("click", HideSpin);
    }
    return true;
}

function HideSpin() {
    let screen = document.getElementById("oscar-spinner-screen");
    let spinner = document.getElementById("oscar-spinner");

    screen.classList.remove("active-oscar-spinner");
    spinner.style.opacity = "0";

    setTimeout(function () {
        spinner.classList.remove("active-oscar-spinner");
        spinner.style.opacity = "1";
    }, 300);
}

	/**
	 * A counter hack for a hack.
	 * This method moves the image SRC values into hidden place-holders in the Form element
	 * A counter-measure to ensure images that are set by Javascript methods are captured
	 * when the form is saved or rendered into a pdf.
	 */
	function appendImageInputs() {
		jQuery("form[method='POST'] img").each(function () {
			const id = jQuery(this).attr('id');
			const src = jQuery(this).attr('src') || "";

			// Skip image if it doesn't have an ID
			if (!id || id.trim() === "") {
				return true;
			}

			const inputId = 'openosp-img-' + id;

			// Remove any existing hidden input for this image
			jQuery("input[type='hidden'][id='" + inputId + "']").remove();

			// Add a fresh hidden input
			jQuery('<input>', {
				id: inputId,
				name: 'openosp-image-link',
				value: JSON.stringify({ id: id, value: src }),
				type: 'hidden'
			}).appendTo("form[method='POST']");
		});
	}

	jQuery(window).on('load', function() {
		appendImageInputs();
	})

	/**
	 * Disables resizing on all textarea elements to prevent content from being
	 * truncated during PDF generation.
	 */
	function disableTextareaResize() {
		if (document.getElementById("eform-disable-textarea-resize")) {
			return;
		}

		const style = document.createElement("style");
		style.id = "eform-disable-textarea-resize";
		style.textContent = "textarea { resize: none !important; }";
		document.head.appendChild(style);
	}

	function handleEmailPrivilege() {
		// Get the value of the element with ID 'hasEmailPrivilege'
		const hasEmailPrivilege = document.getElementById('hasEmailPrivilege');

		if (hasEmailPrivilege) {
			const value = hasEmailPrivilege.value.toLowerCase();
			if (value === 'false') {
				// If 'hasEmailPrivilege' is false, hide the 'remoteEmailButton'
				document.getElementById('remoteEmailButton').style.display = 'none';
			}
		}
	}
