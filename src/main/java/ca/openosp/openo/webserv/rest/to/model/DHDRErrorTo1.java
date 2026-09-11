package ca.openosp.openo.webserv.rest.to.model;

/**
 * A PHI-free notice describing a failed interaction with the DHDR EHR Service, returned to the DHDR
 * viewer so it can inform the EMR user (DHDR14.01).
 *
 * <p>DHDR14.01 requires that the error code, a description, the severity, and the date and time of
 * the incident all be presented to the user, and that no administrator role be needed to see them.
 * The fields here carry exactly that, and nothing else: the underlying exception and its stack trace
 * stay in the gateway audit record, which is where troubleshooting detail belongs.
 *
 * <p>Errors the DHDR EHR Service itself reports are returned as a FHIR {@code OperationOutcome} and
 * rendered from its issues; this transfer object covers the cases where no OperationOutcome can be
 * obtained - a misconfigured gateway, an expired session, or a service that does not respond.
 *
 * @since 2026-07-10
 */
public class DHDRErrorTo1 {

  private Integer httpCode;
  private String httpMessage;
  private String severity;
  private String dateTime;
  private String moreInformation;

  public Integer getHttpCode() {
    return httpCode;
  }

  public void setHttpCode(Integer httpCode) {
    this.httpCode = httpCode;
  }

  public String getHttpMessage() {
    return httpMessage;
  }

  public void setHttpMessage(String httpMessage) {
    this.httpMessage = httpMessage;
  }

  public String getSeverity() {
    return severity;
  }

  public void setSeverity(String severity) {
    this.severity = severity;
  }

  public String getDateTime() {
    return dateTime;
  }

  public void setDateTime(String dateTime) {
    this.dateTime = dateTime;
  }

  public String getMoreInformation() {
    return moreInformation;
  }

  public void setMoreInformation(String moreInformation) {
    this.moreInformation = moreInformation;
  }
}
