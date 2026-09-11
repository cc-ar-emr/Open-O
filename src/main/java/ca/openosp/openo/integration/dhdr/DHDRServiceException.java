package ca.openosp.openo.integration.dhdr;

/**
 * Raised when the DHDR EHR Service reports a failure that carries no FHIR {@code OperationOutcome}
 * to describe it.
 *
 * <p>The service's normal error channel is an OperationOutcome, whose issues the viewer renders
 * directly. This exception covers the remainder - a non-2xx response with some other payload - so
 * that the HTTP status code reaches the EMR user, as DHDR14.01 requires, instead of the response
 * being mistaken for an empty result set.
 *
 * <p>The response body is deliberately not carried: it is already on the gateway audit row, and it
 * has no place in a message shown to a user.
 *
 * @since 2026-07-10
 */
public class DHDRServiceException extends Exception {

  private static final long serialVersionUID = 1L;

  private final int httpCode;

  /**
   * @param httpCode int the HTTP status code the DHDR EHR Service returned
   */
  public DHDRServiceException(int httpCode) {
    super("DHDR EHR Service returned HTTP " + httpCode);
    this.httpCode = httpCode;
  }

  /**
   * @return int the HTTP status code the DHDR EHR Service returned (DHDR14.01 error code)
   */
  public int getHttpCode() {
    return httpCode;
  }
}
