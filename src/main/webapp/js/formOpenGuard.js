/**
 * Warns before the Add Form menu starts a blank form the patient already has one of, so a misclick
 * does not leave a completed form hidden behind an empty one.
 *
 * Clicking an entry asks formExists.do whether the patient has that form, by the name on the entry.
 * The server only says yes for forms kept up to date across visits, such as the Rourke or an
 * antenatal record; a snapshot form such as the Annual opens blank without asking. When the answer
 * is yes, the warning offers to open the existing record instead. Everything else, including the
 * check failing, opens the blank form as before.
 */
(function () {
    "use strict";

    /** Entries of the forms box Add Form menu, and nothing in the other navigation boxes. */
    const MENU_ENTRY = "#forms .menu a";
    const CHECK_PATH = "/oscarEncounter/formExists.do";
    const POPUP_NAME = "openoGuardExisting";
    const POPUP_HEIGHT = 700;
    const POPUP_WIDTH = 960;
    const DIALOG_WIDTH = 460;

    const contextPath = (() => {
        const script = document.currentScript;
        return script ? script.src.replace(/\/js\/formOpenGuard\.js.*$/, "") : "";
    })();

    /** The entry whose click is being replayed, so the replay is not warned about again. */
    let replaying = null;

    /** Entries whose check is in flight, so a double click on one asks only once. */
    const pending = new Set();

    const canShowDialog = () => Boolean(window.jQuery && jQuery.fn && jQuery.fn.dialog);

    /**
     * Opens a blank form by replaying the click, letting the entry's own handler do the work.
     *
     * @param {HTMLAnchorElement} entry the menu entry that was clicked
     */
    const openBlankForm = (entry) => {
        replaying = entry;
        entry.click();
        replaying = null;
    };

    /**
     * Opens the patient's latest record of the form.
     *
     * @param {string} url address of the existing record
     */
    const openExistingForm = (url) => {
        if (typeof popupPage === "function") {
            popupPage(POPUP_HEIGHT, POPUP_WIDTH, POPUP_NAME, url);
        } else {
            window.open(url, POPUP_NAME);
        }
    };

    /**
     * Turns the server's dd-MM-yyyy HH:mm:ss stamp into something a person reads, such as
     * "20 Aug 2026 at 2:32 PM". A date-only stamp (the created date, sent when the record has
     * no edited date) reads as "20 Aug 2026". Hands back whatever it was given if it is not
     * either shape.
     *
     * @param {string} stamp when the existing record was last edited
     * @returns {string} the same moment, written out
     */
    const readableDate = (stamp) => {
        const parts = /^(\d{2})-(\d{2})-(\d{4})(?: (\d{2}):(\d{2}))?/.exec(stamp || "");
        if (!parts) {
            return stamp;
        }

        const [, day, month, year, hours, minutes] = parts;
        const date = new Date(year, month - 1, day, hours || 0, minutes || 0);
        const dayText = date.toLocaleDateString("en-GB", {day: "numeric", month: "short", year: "numeric"});
        if (hours === undefined) {
            return dayText;
        }
        return dayText + " at " + date.toLocaleTimeString("en-US", {hour: "numeric", minute: "2-digit"});
    };

    /**
     * Keeps the stock jQuery UI look. Ocean's stylesheet, on clinics that have it, turns every
     * dialog's title bar blue and centres the title; styles set on the elements win over it.
     *
     * @param {jQuery} dialog the dialog's content element
     */
    const keepStockLook = (dialog) => {
        const widget = dialog.dialog("widget");
        widget.find(".ui-dialog-titlebar").css({background: "#e9e9e9", border: "1px solid #ddd"});
        widget.find(".ui-dialog-title").css({color: "#333", textAlign: "left", width: "90%"});
        widget.find(".ui-dialog-buttonpane").css({textAlign: "right"});
        widget.find(".ui-dialog-buttonpane button")
            .css({background: "#f6f6f6", border: "1px solid #c5c5c5", color: "#454545", fontWeight: "normal"});
    };

    /**
     * Asks whether to open the record the patient already has, or start a blank form anyway.
     *
     * @param {HTMLAnchorElement} entry the menu entry that was clicked
     * @param {{lastEdited: string, url: string}} existing the record the patient already has
     */
    const warn = (entry, existing) => {
        const when = readableDate(existing.lastEdited);
        const opening = when
            ? `This form already exists for this patient, and was last updated on ${when}.`
            : "This form already exists for this patient.";

        const dialog = jQuery("<div>").append(
            jQuery("<p>").text(opening),
            jQuery("<p>").text("Opening and editing the existing version will include previously "
                + "submitted information."),
            jQuery("<p>").text("Creating a new version will not include that information.")
        );

        dialog.dialog({
            title: "Warning - Is a New Form Needed?",
            modal: true,
            resizable: false,
            width: DIALOG_WIDTH,
            open: () => keepStockLook(dialog),
            close: () => dialog.dialog("destroy").remove(),
            buttons: {
                "Open the existing version": () => {
                    dialog.dialog("close");
                    openExistingForm(existing.url);
                },
                "Create a new blank version": () => {
                    dialog.dialog("close");
                    openBlankForm(entry);
                },
                "Cancel": () => {
                    dialog.dialog("close");
                }
            }
        });
    };

    /**
     * Asks the server whether the patient already has the clicked form.
     *
     * @param {HTMLAnchorElement} entry the menu entry that was clicked
     */
    const check = (entry) => {
        pending.add(entry);
        jQuery.getJSON(`${contextPath}${CHECK_PATH}`, {formName: entry.textContent.trim()}).done((existing) => {
            if (existing && existing.exists) {
                warn(entry, existing);
            } else {
                openBlankForm(entry);
            }
        }).fail(() => {
            openBlankForm(entry);
        }).always(() => {
            pending.delete(entry);
        });
    };

    // capture, because the entry's own onclick would otherwise open the form before we can ask
    document.addEventListener("click", (event) => {
        const entry = event.target && event.target.closest
            ? event.target.closest(MENU_ENTRY)
            : null;

        if (!entry || entry === replaying || !canShowDialog()) {
            return;
        }

        event.preventDefault();
        event.stopPropagation();

        if (!pending.has(entry)) {
            check(entry);
        }
    }, true);
}());
