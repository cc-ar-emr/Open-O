package ca.openosp.openo.provider.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the DHDR search-window validation on the provider preference screen (DHDR02.03).
 *
 * <p>Covers {@link ProviderProperty2Action#isValidSearchDays} alone. The action itself is not
 * exercised - it reads the servlet request in a field initializer - so the validation is
 * package-private and tested directly, as {@code ConsentOverrideReport2Action.parseDate} is.
 *
 * @since 2026-08-04
 */
@Tag("unit")
@Tag("dhdr")
@DisplayName("DHDR provider search-window validation")
class ProviderPropertyDhdrSearchDaysUnitTest {

  @Test
  @DisplayName("should accept an ordinary search window")
  void shouldAccept_whenValueIsAnOrdinaryWindow() {
    assertThat(ProviderProperty2Action.isValidSearchDays("120")).isTrue();
    assertThat(ProviderProperty2Action.isValidSearchDays("1")).isTrue();
  }

  @Test
  @DisplayName("should reject a value too wide for an int rather than discarding it silently")
  void shouldReject_whenValueExceedsIntegerRange() {
    // All digits and greater than zero, so the page-level check passes it. Integer.parseInt then
    // threw, the preference was cleared, and the page still reported success.
    assertThat(ProviderProperty2Action.isValidSearchDays("99999999999")).isFalse();
  }

  @Test
  @DisplayName("should reject a window longer than the accepted maximum")
  void shouldReject_whenValueExceedsMaximum() {
    // Beyond this the viewer's start.setDate(end.getDate() - days) yields an Invalid Date and the
    // search loses its start bound without saying so.
    assertThat(ProviderProperty2Action.isValidSearchDays("36500")).isTrue();
    assertThat(ProviderProperty2Action.isValidSearchDays("36501")).isFalse();
  }

  @Test
  @DisplayName("should reject zero, a negative window and a non-number")
  void shouldReject_whenValueIsNotAUsableCount() {
    assertThat(ProviderProperty2Action.isValidSearchDays("0")).isFalse();
    assertThat(ProviderProperty2Action.isValidSearchDays("-30")).isFalse();
    assertThat(ProviderProperty2Action.isValidSearchDays("thirty")).isFalse();
    assertThat(ProviderProperty2Action.isValidSearchDays("30.5")).isFalse();
  }

  @Test
  @DisplayName("should advertise the configured clinic default when the viewer will honour it")
  void shouldAdvertiseConfiguredDefault_whenViewerWillUseIt() {
    assertThat(ProviderProperty2Action.normalizeClinicDefault("90")).isEqualTo("90");
    assertThat(ProviderProperty2Action.normalizeClinicDefault("  90  ")).isEqualTo("90");
  }

  @Test
  @DisplayName("should advertise 120 when the configured clinic default is unusable")
  void shouldAdvertiseSuggestedDefault_whenConfiguredValueIsUnusable() {
    // The placeholder promises what happens when the field is left empty. dhdr/index.jsp keeps 120
    // for each of these, so showing the raw value advertised a fallback the viewer would not apply.
    assertThat(ProviderProperty2Action.normalizeClinicDefault("abc")).isEqualTo("120");
    assertThat(ProviderProperty2Action.normalizeClinicDefault("0")).isEqualTo("120");
    assertThat(ProviderProperty2Action.normalizeClinicDefault("-30")).isEqualTo("120");
    assertThat(ProviderProperty2Action.normalizeClinicDefault("")).isEqualTo("120");
    assertThat(ProviderProperty2Action.normalizeClinicDefault(null)).isEqualTo("120");
    assertThat(ProviderProperty2Action.normalizeClinicDefault("99999999999")).isEqualTo("120");
  }

  @Test
  @DisplayName("should advertise 120 for a clinic default the viewer will not honour either")
  void shouldAdvertiseSuggestedDefault_whenConfiguredValueExceedsTheMaximum() {
    // The placeholder and the viewer have to agree on the whole rule, not only its lower half:
    // dhdr/index.jsp bounds this property at the same century of days, so advertising a wider value
    // would state a fallback the viewer refuses to apply.
    assertThat(ProviderProperty2Action.normalizeClinicDefault("36500")).isEqualTo("36500");
    assertThat(ProviderProperty2Action.normalizeClinicDefault("36501")).isEqualTo("120");
    assertThat(ProviderProperty2Action.normalizeClinicDefault("2147483647")).isEqualTo("120");
  }
}
