package ca.openosp.openo.integration.dhdr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The physician's decision on a DHDR temporary consent unblock, as it is stored and as it is shown.
 *
 * <p>Every decision reaches the gateway log with {@code externalSystem = "PCOI"} and the decision
 * itself carried in {@code transactionType}, but it gets there by one of two routes. A decision the
 * clinician takes at the consent-block prompt is written by the consent-override path, which stores
 * these values directly. A decision the clinician takes by going ahead - the prompt's Continue
 * button opens the PCOI viewlet - is not seen by that path at all: the viewlet's outcome comes back
 * through the shared viewlet result endpoint, which writes its own vocabulary.
 *
 * <p>This enum owns what those values mean and how they are shown. {@link #storedValues()} is what
 * the consent-override endpoint accepts and stores; {@link #reportTransactionTypes()} is what the
 * report's query selects, being those values together with the viewlet result types; and
 * {@link #labelFor(String, Boolean)} resolves a row from either route for display.
 *
 * <p>It does not own the viewlet result vocabulary itself. Those values belong to the endpoint that
 * writes them and are named on {@link OmdGateway}, which both sides read - the reader matching a
 * literal the writer could change is how the completed unblock went missing from this report in the
 * first place.
 *
 * <p>DHDR13.02 speaks of a continue / refuse / cancel choice. {@link #FAILED} and {@link #UNKNOWN}
 * are operational states with no requirement vocabulary of their own, written when the override did
 * not complete (DHDR11.01.b) and when the viewlet's response carried no code the EMR recognises;
 * both are surfaced plainly. UNKNOWN must stay in this enum for the attempt to reach the report at
 * all - {@link #reportTransactionTypes()} is the query's whitelist, so a decision missing from here
 * is written to the audit table and then never shown.
 *
 * @since 2026-07-09
 */
public enum ConsentOverrideChoice {

  /**
   * The patient's consent block was temporarily unblocked and the DHDR query proceeded.
   *
   * <p>Nothing stores this value: the unblock happens inside the PCOI viewlet, so the row that
   * records it arrives as a confirmed {@link OmdGateway#VIEWLET_RESULT}. The constant is what that row
   * resolves to, and what the report labels it.
   */
  OVERWRITE("Overwrite", "Continue (Unblock)"),

  /** The unblock was refused at the consent-block prompt. */
  REFUSED("Refused", "Refused"),

  /** The unblock was cancelled, either at the prompt or inside the PCOI viewlet. */
  CANCELLED("Cancelled", "Cancelled"),

  /** The unblock was attempted but did not complete; the block remains in force. */
  FAILED("Failed", "Failed (did not complete)"),

  /** The viewlet answered with no code the EMR recognises, so no outcome could be asserted. */
  UNKNOWN("Unknown", "Unknown (unrecognised response)");

  private static final List<String> STORED_VALUES;

  private static final List<String> REPORT_TRANSACTION_TYPES;

  static {
    List<String> values = new ArrayList<String>();
    for (ConsentOverrideChoice choice : values()) {
      values.add(choice.storedValue);
    }
    STORED_VALUES = Collections.unmodifiableList(values);

    List<String> reportTypes = new ArrayList<String>(values);
    reportTypes.add(OmdGateway.VIEWLET_RESULT);
    reportTypes.add(OmdGateway.VIEWLET_RESULT_CANCELLED);
    reportTypes.add(OmdGateway.VIEWLET_RESULT_NO_RESPONSE);
    reportTypes.add(OmdGateway.VIEWLET_RESULT_PARTIAL);
    REPORT_TRANSACTION_TYPES = Collections.unmodifiableList(reportTypes);
  }

  private final String storedValue;
  private final String label;

  ConsentOverrideChoice(String storedValue, String label) {
    this.storedValue = storedValue;
    this.label = label;
  }

  /**
   * @return String the value written to {@code OMDGatewayTransactionLog.transactionType}
   */
  public String getStoredValue() {
    return storedValue;
  }

  /**
   * @return String the human-readable label for the report
   */
  public String getLabel() {
    return label;
  }

  /**
   * Every stored decision value: the vocabulary the consent-override endpoint accepts and writes.
   *
   * <p>This is not the report's whitelist - see {@link #reportTransactionTypes()}, which is wider.
   * A decision reaching the log through the viewlet result endpoint is stored under that endpoint's
   * vocabulary, not one of these.
   *
   * @return List&lt;String&gt; an unmodifiable list of the stored values, in declaration order
   */
  public static List<String> storedValues() {
    return STORED_VALUES;
  }

  /**
   * Every {@code transactionType} the report must select to show the whole picture: the stored
   * decision values plus the viewlet result types.
   *
   * <p>Selecting on {@link #storedValues()} alone is what the report used to do, and it silently
   * omitted every completed unblock. Nothing writes {@link #OVERWRITE}: a clinician who goes ahead
   * with the unblock does so inside the PCOI viewlet, whose outcome is recorded by the shared
   * viewlet result endpoint under {@link OmdGateway#VIEWLET_RESULT}. The report showed refusals and
   * cancellations at the prompt and nothing else - the one event DHDR13.02 exists to record was the
   * one event missing from it.
   *
   * <p>The {@code consentViewletLaunch} row the same external system writes stays excluded. It says
   * the viewlet was opened, which is not a decision.
   *
   * @return List&lt;String&gt; an unmodifiable list of the selectable transaction types
   */
  public static List<String> reportTransactionTypes() {
    return REPORT_TRANSACTION_TYPES;
  }

  /**
   * Resolves one gateway log row to the decision it records, by either route.
   *
   * @param transactionType String the row's stored {@code transactionType}, may be {@code null}
   * @param success Boolean the row's success column, used only to tell a confirmed
   *     {@link OmdGateway#VIEWLET_RESULT} from a failed one; may be {@code null}
   * @return ConsentOverrideChoice the decision, or {@code null} when the value is null or belongs
   *     to neither vocabulary
   */
  public static ConsentOverrideChoice fromTransactionLog(String transactionType, Boolean success) {
    if (OmdGateway.VIEWLET_RESULT.equals(transactionType)) {
      // The only row whose meaning needs the success column: logDataReceived writes it true for a
      // confirmed outcome, and the endpoint's catch-all failure branch writes the same type false.
      return Boolean.TRUE.equals(success) ? OVERWRITE : FAILED;
    }
    if (OmdGateway.VIEWLET_RESULT_CANCELLED.equals(transactionType)) {
      return CANCELLED;
    }
    if (OmdGateway.VIEWLET_RESULT_PARTIAL.equals(transactionType)) {
      // The viewlet confirmed the consent call but not the drug service behind it, so the unblock
      // did not complete - which is what FAILED states (DHDR11.01.b).
      return FAILED;
    }
    if (OmdGateway.VIEWLET_RESULT_NO_RESPONSE.equals(transactionType)) {
      // The window closed without answering. Nobody observed an outcome, which is UNKNOWN, not a
      // refusal: the override may well have gone through at Ontario Health.
      return UNKNOWN;
    }
    return fromStoredValue(transactionType);
  }

  /**
   * Resolves a stored {@code transactionType} back to its choice.
   *
   * @param storedValue String the raw stored value, may be {@code null}
   * @return ConsentOverrideChoice the matching choice, or {@code null} if the value is null or
   *     unrecognised
   */
  public static ConsentOverrideChoice fromStoredValue(String storedValue) {
    for (ConsentOverrideChoice choice : values()) {
      if (choice.storedValue.equals(storedValue)) {
        return choice;
      }
    }
    return null;
  }

  /**
   * Renders a stored {@code transactionType} for display.
   *
   * <p>An unrecognised value is passed through unchanged rather than blanked. Note this is a
   * defensive fallback and not a route by which a new decision reaches the report: the query's
   * whitelist is {@link #reportTransactionTypes()}, built from this same enum, so a value the enum
   * does not carry is never selected in the first place. Adding the constant here is what makes a
   * decision visible; this method only stops one that did arrive from rendering as an empty cell.
   *
   * <p>This overload reads the decision vocabulary only. A row that reached the log through the
   * viewlet result endpoint needs {@link #labelFor(String, Boolean)}, which reads both.
   *
   * @param storedValue String the raw stored value, may be {@code null}
   * @return String the label, the raw value if unrecognised, or {@code ""} if {@code null}
   */
  public static String labelFor(String storedValue) {
    if (storedValue == null) {
      return "";
    }
    ConsentOverrideChoice choice = fromStoredValue(storedValue);
    return choice == null ? storedValue : choice.label;
  }

  /**
   * Renders one gateway log row's decision for display, by either route.
   *
   * @param transactionType String the row's stored {@code transactionType}, may be {@code null}
   * @param success Boolean the row's success column, used only to tell a confirmed
   *     {@link OmdGateway#VIEWLET_RESULT} from a failed one; may be {@code null}
   * @return String the label, the raw transaction type if unrecognised, or {@code ""} if
   *     {@code null}
   */
  public static String labelFor(String transactionType, Boolean success) {
    if (transactionType == null) {
      return "";
    }
    ConsentOverrideChoice choice = fromTransactionLog(transactionType, success);
    return choice == null ? transactionType : choice.label;
  }
}
