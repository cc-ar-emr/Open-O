package ca.openosp.openo.integration.dhdr;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ActionSupport;
import org.apache.struts2.ServletActionContext;

import ca.openosp.openo.integration.dhdr.ConsentOverrideReportService.Row;
import ca.openosp.openo.managers.SecurityInfoManager;
import ca.openosp.openo.utility.LoggedInInfo;
import ca.openosp.openo.utility.SpringUtils;

/**
 * Renders the DHDR Temporary Consent Unblock report (DHDR13.02).
 *
 * <p>The report lists every temporary consent-unblock (override) decision recorded against the
 * Digital Health Drug Repository. This action binds the search parameters, guards access, and hands
 * off to {@link ConsentOverrideReportService}, which builds the rows; see that class for the
 * report's data source and its deliberately cross-patient scope.
 *
 * <p>The report itself is a read-only query; the only write is the {@link AuditInfo#VIEW} row this
 * action records, because the report is the one DHDR surface that discloses PHI for patients the
 * user did not select (DHDR15.01).
 *
 * <p><b>Access requires both {@code _rx} and {@code _report}.</b> {@code _rx} alone is not enough:
 * it means "may use the prescribing module", which every DHDR viewer and REST endpoint also checks -
 * but those check it <em>against the demographic being viewed</em>, while this surface has no
 * demographic to check against and returns every patient's row. Gating a cross-patient report on the
 * clinical object alone would make it inseparable from prescribing, so a site could not withhold this
 * report without withdrawing the ability to prescribe. {@code _report} is the object OpenO already
 * uses for exactly this - every cross-patient report under {@code ca.openosp.openo.report} gates on
 * it with a null scope - so requiring both keeps the clinical domain check and adds the
 * cross-patient capability as a separately grantable decision.
 *
 * <p>The breadth itself is mandated, not chosen: DHDR13.02 requires a report on <em>all</em>
 * temporary consent unblock requests, and names the patient's health card number among the elements
 * it must carry. Narrowing the rows to the caller's own patients would fail the requirement, and
 * would also make the report self-auditing - the overrides most in need of independent review are
 * the ones the reviewer would no longer see.
 *
 * @since 2026-07-08
 */
public class ConsentOverrideReport2Action extends ActionSupport {

  private static final String SECURITY_OBJECT = "_rx";
  private static final String REPORT_SECURITY_OBJECT = "_report";
  private static final String DATE_FORMAT = "yyyy-MM-dd";

  private HttpServletRequest request = ServletActionContext.getRequest();

  private SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);
  private ConsentOverrideReportService reportService =
      SpringUtils.getBean(ConsentOverrideReportService.class);

  /**
   * Builds the consent-override report, applying the optional last-name / Unique-ID search, and
   * forwards the resolved rows (plus the echoed search inputs) to the view.
   *
   * @return String the Struts result name ({@code success})
   */
  @Override
  public String execute() {
    LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
    if (!securityInfoManager.hasPrivilege(loggedInInfo, SECURITY_OBJECT, "r", null)
        || !securityInfoManager.hasPrivilege(loggedInInfo, REPORT_SECURITY_OBJECT, "r", null)) {
      throw new SecurityException("missing required security object (_rx and _report)");
    }

    String searchLastName = StringUtils.trimToNull(request.getParameter("searchLastName"));
    String searchUniqueId = StringUtils.trimToNull(request.getParameter("searchUniqueId"));
    String dateFromStr = StringUtils.trimToNull(request.getParameter("dateFrom"));
    String dateToStr = StringUtils.trimToNull(request.getParameter("dateTo"));

    // Empty date filters mean "all history": the form's date fields are blank on first load, so an
    // unbounded lower bound keeps what the user sees (no dates) consistent with what they get. A
    // supplied-but-unparseable date is not silently widened into that default - it is reported.
    List<String> invalidDates = new ArrayList<String>();
    Date from = new Date(0L);
    if (dateFromStr != null) {
      Date parsed = parseDate(dateFromStr, false);
      if (parsed == null) {
        invalidDates.add("From");
      } else {
        from = parsed;
      }
    }
    Date to = new Date();
    if (dateToStr != null) {
      Date parsed = parseDate(dateToStr, true);
      if (parsed == null) {
        invalidDates.add("To");
      } else {
        to = parsed;
      }
    }

    // A window whose start is after its end is not a filter that happens to match nothing - it is a
    // window nothing can fall inside. Querying it produces an empty report indistinguishable from
    // "no overrides were requested in this range", so the user is told instead.
    boolean reversedRange = from.after(to);

    List<Row> rows = reversedRange
        ? new ArrayList<Row>()
        : reportService.findRows(loggedInInfo, searchLastName, searchUniqueId, from, to);

    // The report discloses names and Health Card Numbers across patients, so the read is audited.
    // demographicNo is null: the row records the report as a whole, not one patient (DHDR15.01).
    new OmdGateway().logInteraction(loggedInInfo, AuditInfo.DHDR, AuditInfo.VIEW, null);

    request.setAttribute("rows", rows);
    List<String> warnings = new ArrayList<String>();
    if (!invalidDates.isEmpty()) {
      warnings.add("Ignored invalid date filter: " + String.join(", ", invalidDates)
          + ". Showing the unrestricted range for that bound.");
    }
    if (reversedRange) {
      warnings.add("The From date is after the To date, so the search window is empty."
          + " Swap the dates to search.");
    }
    if (!warnings.isEmpty()) {
      request.setAttribute("dateWarning", String.join(" ", warnings));
    }
    // Echo the raw search inputs so the form stays populated; the JSP encodes them on output.
    request.setAttribute("searchLastName", searchLastName);
    request.setAttribute("searchUniqueId", searchUniqueId);
    request.setAttribute("dateFrom", dateFromStr);
    request.setAttribute("dateTo", dateToStr);
    return "success";
  }

  /**
   * Parses a {@code yyyy-MM-dd} bound from the search form.
   *
   * @param value String the raw parameter value (non-{@code null})
   * @param endOfDay boolean {@code true} to push the result to 23:59:59.999 so an upper bound
   *     includes the whole day
   * @return Date the parsed bound, or {@code null} if the value is not a valid date
   */
  static Date parseDate(String value, boolean endOfDay) {
    SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
    // Strict parsing: without this, "2026-13-45" would roll over into a valid but unintended date.
    format.setLenient(false);
    // Parsed through a ParsePosition so the whole value has to be consumed. parse(String) stops at
    // the first character it cannot use and reports success on what it read, so "2026-01-01junk"
    // silently became 2026-01-01 - the user was shown a filtered report having typed something the
    // form never accepted, with nothing to say so. setLenient(false) does not cover this: it
    // governs rollover within the fields, not trailing text after them.
    ParsePosition position = new ParsePosition(0);
    Date parsed = format.parse(value, position);
    if (parsed == null || position.getIndex() != value.length()) {
      return null;
    }
    if (!endOfDay) {
      return parsed;
    }
    Calendar cal = Calendar.getInstance();
    cal.setTime(parsed);
    cal.set(Calendar.HOUR_OF_DAY, 23);
    cal.set(Calendar.MINUTE, 59);
    cal.set(Calendar.SECOND, 59);
    cal.set(Calendar.MILLISECOND, 999);
    return cal.getTime();
  }
}
