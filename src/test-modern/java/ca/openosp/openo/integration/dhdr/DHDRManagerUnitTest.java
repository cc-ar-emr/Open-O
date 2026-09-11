package ca.openosp.openo.integration.dhdr;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DHDRManager#describesOperationOutcome}, the guard that decides whether a
 * failed DHDR search can be rendered by the viewer or must be raised as a {@link
 * DHDRServiceException}.
 *
 * <p>This matters clinically, not just structurally. A non-2xx response the viewer cannot render
 * reaches it as a bundle with no entries, and is reported as "No records found" - telling the
 * clinician the patient has no dispensed medications when in fact the service errored (DHDR14.01).
 */
@Tag("unit")
@Tag("dhdr")
class DHDRManagerUnitTest {

  @Test
  @DisplayName("should treat a bare OperationOutcome as renderable by the viewer")
  void shouldTreatAsRenderable_whenBodyIsABareOperationOutcome() {
    assertThat(DHDRManager.describesOperationOutcome(
        "{\"resourceType\":\"OperationOutcome\",\"issue\":[]}")).isTrue();
  }

  @Test
  @DisplayName("should treat an OperationOutcome carried inside a bundle as renderable")
  void shouldTreatAsRenderable_whenBundleContainsAnOperationOutcome() {
    assertThat(DHDRManager.describesOperationOutcome(
        "{\"resourceType\":\"Bundle\",\"entry\":[{\"resource\":{\"resourceType\":\"OperationOutcome\"}}]}"))
        .isTrue();
  }

  @Test
  @DisplayName("should not treat a gateway error page as renderable")
  void shouldNotTreatAsRenderable_whenBodyIsAnErrorPage() {
    assertThat(DHDRManager.describesOperationOutcome("<html><body>502 Bad Gateway</body></html>"))
        .isFalse();
  }

  @Test
  @DisplayName("should not treat an empty result bundle as renderable failure detail")
  void shouldNotTreatAsRenderable_whenBodyIsAnEmptyBundle() {
    assertThat(DHDRManager.describesOperationOutcome(
        "{\"resourceType\":\"Bundle\",\"entry\":[]}")).isFalse();
  }

  @Test
  @DisplayName("should not treat an absent body as renderable")
  void shouldNotTreatAsRenderable_whenBodyIsNull() {
    assertThat(DHDRManager.describesOperationOutcome(null)).isFalse();
  }

  @Test
  @DisplayName("should carry the service status code for the DHDR14.01 error code element")
  void shouldCarryStatusCode_whenServiceExceptionIsRaised() {
    DHDRServiceException exception = new DHDRServiceException(502);

    assertThat(exception.getHttpCode()).isEqualTo(502);
    assertThat(exception.getMessage()).doesNotContain("resourceType");
  }
}
