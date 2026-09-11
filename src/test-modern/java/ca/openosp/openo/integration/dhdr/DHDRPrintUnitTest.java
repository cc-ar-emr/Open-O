package ca.openosp.openo.integration.dhdr;

import static org.assertj.core.api.Assertions.assertThat;

import ca.openosp.openo.commn.model.Demographic;
import ca.openosp.openo.managers.DemographicManager;
import ca.openosp.openo.test.unit.OpenOUnitTestBase;
import java.util.List;
import java.util.Locale;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the value-rendering helpers in {@link DHDRPrint}.
 *
 * <p>This class earned a test file. Seven DHDR defects were fixed on 2026-07-29/30 and five of them
 * were here, every one a cell that rendered the wrong thing rather than a crash: a phone number under
 * a fax heading, the four-character text "null" where a value was absent, a phantom frequency reading
 * "every  - ", an empty bracket where a licence was missing, and a date in a second format. None would
 * fail a smoke test, and none did - they were found by comparing printed output against a requirement's
 * element list.
 *
 * <p>So these tests assert the *rendered strings*, not that a method returns non-null. Each group
 * names the defect it guards, so a future change that reintroduces one fails here with a message that
 * explains itself.
 *
 * <p>The helpers are package-private for this reason, as {@link DHDRManager#fhirBirthDate} already is.
 * {@link OpenOUnitTestBase} is needed only because {@code DHDRPrint} resolves a
 * {@code DemographicManager} through {@code SpringUtils} in a field initialiser; none of the helpers
 * under test touch it.
 */
@Tag("unit")
@Tag("dhdr")
class DHDRPrintUnitTest extends OpenOUnitTestBase {

  private DHDRPrint print;

  private static Locale defaultLocale;

  /**
   * Pins the default locale for the date assertions in this class.
   *
   * <p>{@code displayDate} and {@code searchPeriodText} format through
   * {@code SimpleDateFormat("MMM d, yyyy")}, which resolves month names against the JVM default
   * locale. Every date assertion below names an English month, so on a French or Polish default they
   * would fail while the rendering itself was correct.
   *
   * <p>Pinned in the test rather than in {@code DHDRPrint}: on a bilingual EMR a French install
   * arguably should print French month names, so the fixed language is a property of these
   * assertions and not a requirement on the printed output.
   */
  @BeforeAll
  static void pinDefaultLocaleForDateAssertions() {
    defaultLocale = Locale.getDefault();
    Locale.setDefault(Locale.ENGLISH);
  }

  @AfterAll
  static void restoreDefaultLocale() {
    Locale.setDefault(defaultLocale);
  }

  @BeforeEach
  void createPrinter() {
    // DHDRPrint resolves this in a field initialiser, so it must be registered before construction
    // even though none of the helpers under test call it.
    createAndRegisterMock(DemographicManager.class);
    print = new DHDRPrint();
  }

  /** A dispense/prescription payload built from alternating key/value pairs. */
  private static JSONObject med(Object... keysAndValues) throws Exception {
    JSONObject med = new JSONObject();
    for (int i = 0; i < keysAndValues.length; i += 2) {
      med.put((String) keysAndValues[i], keysAndValues[i + 1]);
    }
    return med;
  }

  @Nested
  @DisplayName("optText - absence must not render as data")
  class OptTextTests {

    @Test
    @DisplayName("should return an empty string when a serialised null arrives as the text \"null\"")
    void shouldReturnEmpty_whenValueIsStringifiedNull() throws Exception {
      // The EMR payload carries nullable boxed types; a serialised null read back through optString
      // surfaces as "null" and printed verbatim reads as a recorded value.
      assertThat(print.optText(med("refillQuantity", "null"), "refillQuantity")).isEmpty();
      assertThat(print.optText(med("refillQuantity", "NULL"), "refillQuantity")).isEmpty();
    }

    @Test
    @DisplayName("should return an empty string when the key is absent")
    void shouldReturnEmpty_whenKeyAbsent() throws Exception {
      assertThat(print.optText(med(), "nothingHere")).isEmpty();
    }

    @Test
    @DisplayName("should trim surrounding whitespace")
    void shouldTrim_whenValueIsPadded() throws Exception {
      assertThat(print.optText(med("strength", "  50  "), "strength")).isEqualTo("50");
    }

    @Test
    @DisplayName("should preserve a value that merely contains the word null")
    void shouldPreserve_whenValueOnlyContainsNull() throws Exception {
      assertThat(print.optText(med("frequency", "nullify daily"), "frequency"))
          .isEqualTo("nullify daily");
    }
  }

  @Nested
  @DisplayName("isMeaningful - falsy for zero, matching the screen's ng-if")
  class IsMeaningfulTests {

    @Test
    @DisplayName("should treat zero as absent, as ng-if does")
    void shouldTreatAsAbsent_whenValueIsZero() {
      // Treating "0" as present printed "0 (0 / 0 days)" where the screen showed "0".
      assertThat(print.isMeaningful("0")).isFalse();
      assertThat(print.isMeaningful("0.0")).isFalse();
      assertThat(print.isMeaningful(" 0 ")).isFalse();
    }

    @Test
    @DisplayName("should treat a non-zero number as present")
    void shouldTreatAsPresent_whenValueIsNonZero() {
      assertThat(print.isMeaningful("1")).isTrue();
      assertThat(print.isMeaningful("0.5")).isTrue();
    }

    @Test
    @DisplayName("should treat free text as present, since it carries information")
    void shouldTreatAsPresent_whenValueIsNotNumeric() {
      assertThat(print.isMeaningful("mg")).isTrue();
    }

    @Test
    @DisplayName("should treat null and blank as absent")
    void shouldTreatAsAbsent_whenValueIsNullOrBlank() {
      assertThat(print.isMeaningful(null)).isFalse();
      assertThat(print.isMeaningful("   ")).isFalse();
    }
  }

  @Nested
  @DisplayName("joinValueAndUnit - no stray separator when a part is missing")
  class JoinValueAndUnitTests {

    @Test
    @DisplayName("should join a value and its unit with a single space")
    void shouldJoin_whenBothPresent() {
      assertThat(print.joinValueAndUnit("50", "mg")).isEqualTo("50 mg");
    }

    @Test
    @DisplayName("should return the value alone when no unit is recorded")
    void shouldReturnValueAlone_whenUnitAbsent() {
      assertThat(print.joinValueAndUnit("50", "")).isEqualTo("50");
      assertThat(print.joinValueAndUnit("50", null)).isEqualTo("50");
    }

    @Test
    @DisplayName("should return the unit alone rather than a leading space")
    void shouldReturnUnitAlone_whenValueAbsent() {
      assertThat(print.joinValueAndUnit("", "mg")).isEqualTo("mg");
    }

    @Test
    @DisplayName("should return an empty string when neither part is recorded")
    void shouldReturnEmpty_whenBothAbsent() {
      assertThat(print.joinValueAndUnit(null, null)).isEmpty();
    }
  }

  @Nested
  @DisplayName("pharmacistName - name only, and both separators conditional")
  class PharmacistNameTests {

    private JSONObject pharmacist(String family, String given, String licence) throws Exception {
      JSONObject med = med();
      if (family != null) {
        med.put("pharmacistLastname", family);
      }
      if (given != null) {
        med.put("pharmacistFirstname", given);
      }
      if (licence != null) {
        med.put("pharmacistLicenceNumber", new JSONObject().put("value", licence));
      }
      return med;
    }

    @Test
    @DisplayName("should render surname and given name")
    void shouldRenderName_whenBothPartsPresent() throws Exception {
      assertThat(print.pharmacistName(pharmacist("Sway", "Ken", null))).isEqualTo("Sway, Ken");
    }

    @Test
    @DisplayName("should not render the licence number, which is not an element of any DHDR view")
    void shouldNotRenderLicence_evenWhenOneIsSupplied() throws Exception {
      // DHDR04.01, DHDR06.01(g) and DHDR07.01(g) each specify the pharmacist as name only, and
      // "pharmacist" appears nowhere else in the specification. The print used to append the licence
      // where the screen's pharmacy-service tables never did.
      assertThat(print.pharmacistName(pharmacist("Sway", "Ken", "200087")))
          .isEqualTo("Sway, Ken")
          .doesNotContain("200087");
    }

    @Test
    @DisplayName("should not render a leading comma when no surname is recorded")
    void shouldOmitComma_whenSurnameAbsent() throws Exception {
      assertThat(print.pharmacistName(pharmacist(null, "Ken", "200087"))).isEqualTo("Ken");
    }

    @Test
    @DisplayName("should render the surname alone when no given name is recorded")
    void shouldRenderSurnameAlone_whenGivenNameAbsent() throws Exception {
      assertThat(print.pharmacistName(pharmacist("Sway", null, null))).isEqualTo("Sway");
    }

    @Test
    @DisplayName("should render nothing when the viewer resolved no pharmacist at all")
    void shouldRenderNothing_whenNoPharmacistResolved() throws Exception {
      // The viewer only resolves a pharmacist from a Practitioner carrying ca-on-license-pharmacist,
      // so an event without one leaves every part undefined. This used to print ", ()".
      assertThat(print.pharmacistName(pharmacist(null, null, null))).isEmpty();
    }
  }

  @Nested
  @DisplayName("displayDate - one format across the views (DHDR03.06)")
  class DisplayDateTests {

    @Test
    @DisplayName("should format epoch milliseconds as the screen's medium date")
    void shouldFormatMedium_whenValueIsEpochMillis() {
      // The EMR transfer objects carry rxDate as epoch millis, and printing it raw emitted the
      // millisecond number (#60).
      //
      // Derived from a local date rather than hardcoded, because displayDate formats in the JVM's
      // default zone - as the screen's Angular date filter does. A hardcoded UTC-midnight epoch
      // renders as the previous day in any zone behind UTC, which is what a first draft of this test
      // asserted wrongly. The zone dependency is correct behaviour, not a defect: only the EMR side
      // supplies epoch values and those come from local calendar dates, while the DHDR service
      // supplies ISO dates that never go through this branch.
      long localMidnight = java.time.LocalDate.of(2016, 6, 23)
          .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
      assertThat(print.displayDate(String.valueOf(localMidnight))).isEqualTo("Jun 23, 2016");
    }

    @Test
    @DisplayName("should format an ISO date as the screen's medium date")
    void shouldFormatMedium_whenValueIsIsoDate() {
      assertThat(print.displayDate("1984-08-08")).isEqualTo("Aug 8, 1984");
    }

    @Test
    @DisplayName("should take the date part of an ISO date-time")
    void shouldFormatMedium_whenValueIsIsoDateTime() {
      assertThat(print.displayDate("2019-10-02T11:45:17.000-04:00")).isEqualTo("Oct 2, 2019");
    }

    @Test
    @DisplayName("should print a partial FHIR date verbatim rather than as 1970")
    void shouldPrintVerbatim_whenValueIsPartialFhirDate() {
      // FHIR `date` permits yyyy and yyyy-MM, and computeAge accepts both. Parsing any all-digit
      // string as epoch millis turned a birthDate of "1984" into new Date(1984L) -> "Jan 1, 1970",
      // printed directly above an Age cell derived correctly from the same value.
      assertThat(print.displayDate("1984")).isEqualTo("1984");
      assertThat(print.displayDate("1984-08")).isEqualTo("1984-08");
      // The header must not contradict itself: the age is still derivable from the same string.
      // Assert a whole number of years rather than merely non-empty, so a computeAge that returned
      // "0" or "-" beside a 1984 birthDate would still fail. Bounded below rather than pinned, since
      // the value moves with the clock.
      // Bounded to three digits rather than \d+: an age in years cannot be wider than that, so the
      // assertion is tighter, and it also removes the only way the parse below could throw instead
      // of failing with a readable message.
      String age = print.computeAge("1984");
      assertThat(age).matches("\\d{1,3}");
      assertThat(Integer.parseInt(age)).isGreaterThan(40);
    }

    @Test
    @DisplayName("should return an empty string for an absent, blank or stringified-null value")
    void shouldReturnEmpty_whenValueIsAbsentOrNull() {
      assertThat(print.displayDate(null)).isEmpty();
      assertThat(print.displayDate("")).isEmpty();
      assertThat(print.displayDate("   ")).isEmpty();
      assertThat(print.displayDate("null")).isEmpty();
    }

    @Test
    @DisplayName("should return an unrecognised value unchanged rather than blanking it")
    void shouldReturnUnchanged_whenValueIsNotADate() {
      // An unrecognised value is still information; blanking it would hide data from the clinician.
      assertThat(print.displayDate("N/A")).isEqualTo("N/A");
      assertThat(print.displayDate("sometime in 2016")).isEqualTo("sometime in 2016");
    }
  }

  @Nested
  @DisplayName("frequencyText - absence must not read as a frequency")
  class FrequencyTextTests {

    @Test
    @DisplayName("should return an empty string when no dosage instruction was supplied")
    void shouldReturnEmpty_whenNoPartSupplied() throws Exception {
      // dosageInstruction is optional under the consumer profile and the IG's own pharmacy-service
      // example carries none; concatenating unconditionally printed " every  - ".
      assertThat(print.frequencyText(med())).isEmpty();
    }

    @Test
    @DisplayName("should compose the frequency when the parts are supplied")
    void shouldCompose_whenPartsSupplied() throws Exception {
      assertThat(print.frequencyText(
          med("frequency", "1", "period", "1", "periodMax", "2", "periodUnit", "d")))
          .isEqualTo("1 every 1 - 2 d");
    }

    @Test
    @DisplayName("should still compose when only some parts are supplied")
    void shouldCompose_whenOnlySomePartsSupplied() throws Exception {
      assertThat(print.frequencyText(med("frequency", "3", "periodUnit", "d")))
          .isEqualTo("3 every  -  d");
    }
  }

  @Nested
  @DisplayName("serviceTypeWithPin - the product identifier is optional")
  class ServiceTypeWithPinTests {

    @Test
    @DisplayName("should append the PIN when one is present")
    void shouldAppendPin_whenPresent() throws Exception {
      assertThat(print.serviceTypeWithPin(
          new JSONObject().put("display", "MedsCheck").put("code", "MEDSCHECK")))
          .isEqualTo("MedsCheck (PIN: MEDSCHECK)");
    }

    @Test
    @DisplayName("should render the display alone when no PIN is present")
    void shouldRenderDisplayAlone_whenPinAbsent() throws Exception {
      assertThat(print.serviceTypeWithPin(new JSONObject().put("display", "MedsCheck")))
          .isEqualTo("MedsCheck");
    }

    @Test
    @DisplayName("should render the PIN alone when there is no display text")
    void shouldRenderPinAlone_whenDisplayAbsent() throws Exception {
      assertThat(print.serviceTypeWithPin(new JSONObject().put("code", "MEDSCHECK")))
          .isEqualTo("PIN: MEDSCHECK");
    }

    @Test
    @DisplayName("should return an empty string when there is no brandName at all")
    void shouldReturnEmpty_whenBrandNameAbsent() {
      assertThat(print.serviceTypeWithPin(null)).isEmpty();
    }
  }

  @Nested
  @DisplayName("EMR prescription cells (DHDR05.02)")
  class EmrCellTests {

    @Test
    @DisplayName("should fall back generic then brand then custom for the medication name")
    void shouldFallBackInOrder_whenNamingTheMedication() throws Exception {
      assertThat(print.emrMedicationName(
          med("genericName", "ASA", "brandName", "Novasen", "customName", "Something")))
          .isEqualTo("ASA");
      assertThat(print.emrMedicationName(med("brandName", "Novasen", "customName", "Something")))
          .isEqualTo("Novasen");
      assertThat(print.emrMedicationName(med("customName", "Something"))).isEqualTo("Something");
      assertThat(print.emrMedicationName(med())).isEmpty();
    }

    @Test
    @DisplayName("should render a dose range when the maximum differs from the minimum")
    void shouldRenderRange_whenMaxDiffersFromMin() throws Exception {
      assertThat(print.emrDose(med("takeMin", "1", "takeMax", "2", "unit", "tab")))
          .isEqualTo("1 - 2 tab");
    }

    @Test
    @DisplayName("should render a single dose when minimum and maximum agree")
    void shouldRenderSingleDose_whenMinEqualsMax() throws Exception {
      assertThat(print.emrDose(med("takeMin", "1", "takeMax", "1", "unit", "tab")))
          .isEqualTo("1 tab");
    }

    @Test
    @DisplayName("should omit a dosage unit that merely repeats the strength unit")
    void shouldOmitUnit_whenItRepeatsTheStrengthUnit() throws Exception {
      // OpenO stores the dosage unit as free text and records often repeat the strength there, which
      // printed "50 mg" under Dosage beside "50 mg" under Strength and read as a second strength.
      assertThat(print.emrDose(med("takeMin", "50", "unit", "mg", "strengthUnit", "MG")))
          .isEqualTo("50");
    }

    @Test
    @DisplayName("should render nothing when the recorded dose is zero")
    void shouldRenderNothing_whenDoseIsZero() throws Exception {
      assertThat(print.emrDose(med("takeMin", "0", "unit", "tab"))).isEmpty();
    }

    @Test
    @DisplayName("should fall back to a countable drug form when the unit adds nothing")
    void shouldFallBackToForm_whenUnitAddsNothing() throws Exception {
      // The half the print used to omit. The screen resolves the unit in two steps and this stopped
      // after the first, so the same prescription read "2 tablet" on screen and "2" on paper.
      assertThat(print.emrDose(med("takeMin", "2", "form", "TABLET", "strengthUnit", "mg")))
          .isEqualTo("2 tablet");
      assertThat(print.emrDose(
          med("takeMin", "1", "unit", "mg", "strengthUnit", "MG", "form", "Capsule, Extended")))
          .isEqualTo("1 capsule");
    }

    @Test
    @DisplayName("should match a countable form recorded with either globule spelling")
    void shouldMatchForm_whenSpelledEitherWay() throws Exception {
      // "globule" was inherited as "grobule". Both are listed, so correcting the typo in a record
      // does not silently cost it its unit.
      assertThat(print.emrDoseUnit(med("form", "grobule"))).isEqualTo("grobule");
      assertThat(print.emrDoseUnit(med("form", "globule"))).isEqualTo("globule");
    }

    @Test
    @DisplayName("should lower-case the dose unit so print and screen read alike")
    void shouldLowerCaseUnit_soPrintMatchesScreen() throws Exception {
      // The screen compares in lower case and returns what it compared; returning it raw here
      // printed "MG" for a drug the screen showed as "mg".
      assertThat(print.emrDoseUnit(med("unit", "TAB"))).isEqualTo("tab");
    }

    @Test
    @DisplayName("should render no unit when neither the unit nor the form names one")
    void shouldRenderNoUnit_whenNothingNamesOne() throws Exception {
      assertThat(print.emrDoseUnit(med("form", "solution", "strengthUnit", "mg"))).isEmpty();
      assertThat(print.emrDoseUnit(med())).isEmpty();
      assertThat(print.emrDose(med("takeMin", "5", "form", "cream"))).isEqualTo("5");
    }

    @Test
    @DisplayName("should append the duration to the first fill quantity when one is recorded")
    void shouldAppendDuration_whenRecorded() throws Exception {
      assertThat(print.emrQuantityAndDuration(
          med("quantity", "30", "duration", "30", "durationUnit", "days")))
          .isEqualTo("30 / 30 days");
    }

    @Test
    @DisplayName("should render the quantity alone when the duration is absent or zero")
    void shouldRenderQuantityAlone_whenDurationAbsentOrZero() throws Exception {
      assertThat(print.emrQuantityAndDuration(med("quantity", "30"))).isEqualTo("30");
      assertThat(print.emrQuantityAndDuration(med("quantity", "30", "duration", "0")))
          .isEqualTo("30");
    }

    @Test
    @DisplayName("should bracket the refill detail after the repeat count")
    void shouldBracketRefillDetail_whenRecorded() throws Exception {
      assertThat(print.emrRefills(
          med("repeats", "3", "refillQuantity", "30", "refillDuration", "30")))
          .isEqualTo("3 (30 / 30 days)");
    }

    @Test
    @DisplayName("should render the repeat count alone rather than an empty bracket")
    void shouldRenderCountAlone_whenNoRefillDetail() throws Exception {
      assertThat(print.emrRefills(med("repeats", "3"))).isEqualTo("3");
      assertThat(print.emrRefills(med("repeats", "0"))).isEqualTo("0");
    }

    @Test
    @DisplayName("should not print the text null for an absent boxed refill value")
    void shouldNotPrintNull_whenRefillValuesAreSerialisedNulls() throws Exception {
      assertThat(print.emrRefills(
          med("repeats", "2", "refillQuantity", "null", "refillDuration", "null")))
          .isEqualTo("2");
    }
  }

  @Nested
  @DisplayName("licenceBody - licensing college by name, not by system URI")
  class LicenceBodyTests {

    @Test
    @DisplayName("should map a known licensing system to the college name")
    void shouldMapToCollegeName_whenSystemIsKnown() {
      assertThat(print.licenceBody(
          "https://fhir.infoway-inforoute.ca/NamingSystem/ca-on-license-physician"))
          .isEqualTo("College of Physicians and Surgeons of Ontario");
      assertThat(print.licenceBody(
          "https://fhir.infoway-inforoute.ca/NamingSystem/ca-on-license-pharmacist"))
          .isEqualTo("Ontario College of Pharmacists");
    }

    @Test
    @DisplayName("should map every code in the IG's practitioner-identifier CodeSystem")
    void shouldMapEveryCode_whenSystemComesFromTheCodeSystem() {
      // Spelled exactly as CodeSystem.ca-on-practitioner-identifier-system publishes them. Two of
      // these were transcribed wrong and so matched nothing: out-of-province carried a stray space
      // before the hyphen, and chiropodist used the "license" stem where the IG says "registration".
      // Neither appeared in any fixture, so both printed with no licensing body at all.
      String base = "https://fhir.infoway-inforoute.ca/NamingSystem/";
      assertThat(print.licenceBody(base + "ca-out-of-province-prescriber"))
          .isEqualTo("Out-of-Province Prescriber");
      assertThat(print.licenceBody(base + "ca-on-registration-chiropodist"))
          .isEqualTo("College of Chiropodists of Ontario");
      assertThat(print.licenceBody(base + "ca-on-license-optometrist"))
          .isEqualTo("College of Optometrists of Ontario");
      assertThat(print.licenceBody(base + "ca-on-license-naturopath"))
          .isEqualTo("College of Naturopaths of Ontario");
      assertThat(print.licenceBody(base + "ca-on-license-midwife"))
          .isEqualTo("College of Midwives of Ontario");
      assertThat(print.licenceBody(base + "ca-on-license-nurse"))
          .isEqualTo("College of Nurses of Ontario");
      assertThat(print.licenceBody(base + "ca-on-license-dental-surgeon"))
          .isEqualTo("Royal College of Dental Surgeons of Ontario");
      assertThat(print.licenceBody(base + "ca-on-unknown-prescriber"))
          .isEqualTo("Unknown Prescriber");
    }

    @Test
    @DisplayName("should return an empty string for an unknown or absent system")
    void shouldReturnEmpty_whenSystemUnknownOrAbsent() {
      assertThat(print.licenceBody("https://example.org/NamingSystem/made-up")).isEmpty();
      assertThat(print.licenceBody(null)).isEmpty();
      assertThat(print.licenceBody("")).isEmpty();
      // The pre-fix spelling must not keep working, or the typo can quietly come back.
      assertThat(print.licenceBody(
          "https://fhir.infoway-inforoute.ca/NamingSystem/ca-out-of-province -prescriber"))
          .isEmpty();
    }
  }

  @Nested
  @DisplayName("Ages and dates of birth")
  class AgeAndBirthDateTests {

    @Test
    @DisplayName("should compute an age from a full, partial or year-only birth date")
    void shouldComputeAge_whenBirthDateIsFullOrPartial() {
      assertThat(print.computeAge("1984-08-08")).isNotEmpty().containsOnlyDigits();
      assertThat(print.computeAge("1984-08")).isNotEmpty().containsOnlyDigits();
      assertThat(print.computeAge("1984")).isNotEmpty().containsOnlyDigits();
    }

    @Test
    @DisplayName("should return an empty string when the birth date cannot be parsed")
    void shouldReturnEmpty_whenBirthDateUnparseable() {
      // Blank rather than "N/A", so it matches the DOB cell it is derived from.
      assertThat(print.computeAge("")).isEmpty();
      assertThat(print.computeAge("N/A")).isEmpty();
      assertThat(print.computeAge("1984-13-45")).isEmpty();
    }

    @Test
    @DisplayName("should return an empty string when the birth date has not happened yet")
    void shouldReturnEmpty_whenBirthDateIsInTheFuture() {
      // A well-formed but future date parses, so it reached the period arithmetic and printed an
      // age beside the DOB. Both distances matter and they fail differently: years ahead gives a
      // negative count, months ahead gives 0 - the second reads as a newborn rather than as bad data.
      java.time.LocalDate today = java.time.LocalDate.now();
      assertThat(print.computeAge(today.plusYears(2).toString())).isEmpty();
      assertThat(print.computeAge(today.plusDays(1).toString())).isEmpty();
    }

    @Test
    @DisplayName("should render an unpadded demographic birth date in the display format")
    void shouldRenderMedium_whenDemographicColumnsAreUnpadded() {
      // getBirthDayAsString() joins the three columns without padding and can yield 1984-8-8, which
      // no formatter here parses; the padding comes from DHDRManager.fhirBirthDate.
      Demographic demo = new Demographic();
      demo.setYearOfBirth("1984");
      demo.setMonthOfBirth("8");
      demo.setDateOfBirth("8");
      assertThat(print.emrBirthDate(demo)).isEqualTo("Aug 8, 1984");
    }

    @Test
    @DisplayName("should fall back to the raw joined value when the birth columns are unusable")
    void shouldFallBackToRaw_whenBirthColumnsUnusable() {
      Demographic demo = new Demographic();
      demo.setYearOfBirth("");
      demo.setMonthOfBirth("");
      demo.setDateOfBirth("");
      // Whatever getBirthDayAsString() produces, the point is that it is not silently blanked.
      assertThat(print.emrBirthDate(demo)).isEqualTo(demo.getBirthDayAsString());
    }
  }

  @Nested
  @DisplayName("Column lists - the refactor's single point of failure")
  class ColumnListTests {

    /**
     * These two assertions exist because collapsing the duplicated tables onto one column list moved
     * the risk rather than removing it: two hand-built copies can drift from each other, but one list
     * can be reordered or relabelled in a single edit that compiles and renders a plausible-looking
     * table. The labels and their order are the contract with the requirement's element list, so they
     * are asserted literally.
     */
    @Test
    @DisplayName("should declare the pharmacy service columns in DHDR07.01's order")
    void shouldDeclarePharmacyServiceColumns_inRequirementOrder() {
      assertThat(print.pharmacyServiceColumns().stream().map(DHDRPrint.Column::label).toList())
          .containsExactly(
              "Dispensed Date",
              "Pickup Date",
              "Pharmacy Service Type",
              "Pharmacy Service Description",
              "Rx Number",
              "Therapeutic Class/Sub-class",
              "Pharmacy Name",
              "Pharmacist",
              "Pharmacy Fax");
    }

    @Test
    @DisplayName("should declare the EMR prescription columns in DHDR05.02's order")
    void shouldDeclareEmrColumns_inRequirementOrder() {
      assertThat(print.emrPrescriptionColumns().stream().map(DHDRPrint.Column::label).toList())
          .containsExactly(
              "Start Date",
              "Medication",
              "Strength",
              "Dosage",
              "Frequency",
              "Prescriber",
              "DIN",
              "Qty / Duration",
              "Refills");
    }

    @Test
    @DisplayName("should name the pharmacy fax column the fax, not the phone (DHDR07.01(h))")
    void shouldNameTheFaxColumnTheFax() throws Exception {
      // The Comparative print showed dispensingPharmacyPhoneNumber under a "Pharmacy #" heading while
      // the Summary print's copy of the same table was correct. Assert both halves: the label, and
      // that the extractor reads the fax field.
      List<DHDRPrint.Column> columns = print.pharmacyServiceColumns();
      DHDRPrint.Column fax = columns.get(columns.size() - 1);
      assertThat(fax.label()).isEqualTo("Pharmacy Fax");
      assertThat(fax.value().apply(med(
          "dispensingPharmacyFaxNumber", "416-652-4553",
          "dispensingPharmacyPhoneNumber", "416-539-5532")))
          .isEqualTo("416-652-4553");
    }

    @Test
    @DisplayName("should render every column without throwing on an empty event")
    void shouldRenderAllColumns_whenEventIsEmpty() throws Exception {
      // A sparse but legal record must not abort a print; every extractor has to tolerate absence.
      JSONObject empty = med();
      for (DHDRPrint.Column column : print.pharmacyServiceColumns()) {
        assertThat(column.value().apply(empty)).isNotNull();
      }
      for (DHDRPrint.Column column : print.emrPrescriptionColumns()) {
        assertThat(column.value().apply(empty)).isNotNull();
      }
    }
  }

  /**
   * The printed "Date Range" line. Previously it concatenated {@code start + " to " + end}
   * unconditionally, so a cleared start date produced a dangling {@code " to Jul 30, 2026"} - a range
   * the search never used. BP6 requires the period be described however it is bounded.
   */
  @Nested
  @DisplayName("searchPeriodText - a missing bound must not produce a dangling range")
  class SearchPeriodTextTests {

    @Test
    @DisplayName("should render both bounds when the range is closed")
    void shouldRenderRange_whenBothBoundsPresent() {
      assertThat(print.searchPeriodText("2026-04-01", "2026-07-30"))
          .isEqualTo("Apr 1, 2026 to Jul 30, 2026");
    }

    @Test
    @DisplayName("should describe an open lower bound rather than a dangling range")
    void shouldDescribeOpenStart_whenStartMissing() {
      // The #81 case: the printout stated " to Jul 30, 2026" for a search with no lower bound.
      assertThat(print.searchPeriodText("", "2026-07-30"))
          .isEqualTo("all events up to Jul 30, 2026");
    }

    @Test
    @DisplayName("should describe an open upper bound as onwards")
    void shouldDescribeOpenEnd_whenEndMissing() {
      assertThat(print.searchPeriodText("2016-01-02", "")).isEqualTo("Jan 2, 2016 onwards");
    }

    @Test
    @DisplayName("should describe an entirely unbounded search")
    void shouldDescribeUnbounded_whenNeitherBoundPresent() {
      assertThat(print.searchPeriodText("", "")).isEqualTo("all available events");
    }
  }

  /**
   * BP4 requires the search parameters the service reports it used be checked. The viewer does the
   * comparison and passes any mismatch here, so the printout does not assert the requested range
   * unqualified - the paper outlives the screen that carries the warning.
   */
  @Nested
  @DisplayName("serviceReportedPeriod - the printout must not overstate the range searched")
  class ServiceReportedPeriodTests {

    @Test
    @DisplayName("should say nothing when the service reported the range that was requested")
    void shouldReturnEmpty_whenNoMismatchRecorded() {
      // The common case by far. An empty string is the signal to omit the line entirely: printing
      // "reported searching: " on every document would train the reader to ignore it.
      assertThat(print.serviceReportedPeriod(null)).isEmpty();
    }

    @Test
    @DisplayName("should render both bounds in the document's date format")
    void shouldRenderRange_whenBothBoundsReported() throws Exception {
      // DHDR03.06: the ISO the payload carries must not reach the page. This is why the raw bounds
      // are sent rather than the string the screen already formatted.
      assertThat(print.serviceReportedPeriod(
          med("startDate", "2026-04-01", "endDate", "2026-07-30")))
          .isEqualTo("Apr 1, 2026 to Jul 30, 2026");
    }

    @Test
    @DisplayName("should describe a lower bound with no upper bound as open-ended")
    void shouldRenderOnwards_whenOnlyStartReported() throws Exception {
      // A real shape: BundleResponseSample.json echoes ge with no le at all.
      assertThat(print.serviceReportedPeriod(med("startDate", "2016-01-02")))
          .isEqualTo("Jan 2, 2016 onwards");
    }

    @Test
    @DisplayName("should describe an upper bound with no lower bound as everything up to it")
    void shouldRenderUpTo_whenOnlyEndReported() throws Exception {
      assertThat(print.serviceReportedPeriod(med("endDate", "2019-02-25")))
          .isEqualTo("all events up to Feb 25, 2019");
    }

    @Test
    @DisplayName("should quote the viewer's wording when the service used a prefix we never send")
    void shouldQuoteViewerText_whenNoBoundsToFormat() throws Exception {
      // The IG's own DHDR-MedDis.json echoes `eq`, which yields no ge/le to format. Printing nothing
      // would silently drop the warning from the document that outlives the screen.
      assertThat(print.serviceReportedPeriod(med("used", "eq2016-01-02")))
          .isEqualTo("eq2016-01-02");
    }

    @Test
    @DisplayName("should say nothing when a mismatch object carries no usable detail")
    void shouldReturnEmpty_whenMismatchObjectIsBare() throws Exception {
      // Nothing to report is preferable to a line reading "reported searching: ".
      assertThat(print.serviceReportedPeriod(med())).isEmpty();
    }
  }

  /**
   * The EMR half of the Comparative printout was the one array that reached the page unsorted. The
   * browser posts it in {@code DrugDao.findByDemographicId}'s order - {@code createDate DESC} - while
   * the column printed as "Start Date" reads {@code rxDate}, a different field, so a back-dated or
   * bulk-entered prescription landed out of order. DHDR05.02 makes descending chronological order a
   * MUST by default and DHDR13.01(d) carries the View's element set onto the paper.
   *
   * <p>The screen hid this: its {@code orderBy} sorts at render time and never touches the underlying
   * array, so the two halves disagreed only on paper.
   */
  @Nested
  @DisplayName("sortByDateDesc")
  class SortByDateDescTests {

    private JSONArray arrayOf(String... dates) throws Exception {
      JSONArray arr = new JSONArray();
      for (String d : dates) {
        JSONObject o = new JSONObject();
        if (d != null) {
          o.put("rxDate", d);
        }
        arr.put(o);
      }
      return arr;
    }

    private List<String> datesOf(JSONArray arr) {
      return java.util.stream.IntStream.range(0, arr.length())
          .mapToObj(i -> arr.optJSONObject(i).optString("rxDate", ""))
          .toList();
    }

    @Test
    @DisplayName("should order EMR prescriptions most-recent-first by the printed Start Date field")
    void shouldOrderByRxDateDescending_notPostedOrder() throws Exception {
      // Deliberately not already sorted, and not reverse-sorted either: an implementation that simply
      // reversed the posted order would pass a two-element fixture.
      JSONArray sorted =
          print.sortByDateDesc(
              arrayOf("2024-02-01", "2026-01-15", "2019-11-30", "2025-06-05"),
              "rxDate",
              "EMR prescription");

      assertThat(datesOf(sorted))
          .containsExactly("2026-01-15", "2025-06-05", "2024-02-01", "2019-11-30");
    }

    @Test
    @DisplayName("should sort prescriptions with no start date last rather than first")
    void shouldSortMissingDatesLast() throws Exception {
      JSONArray sorted =
          print.sortByDateDesc(
              arrayOf(null, "2020-01-01", null, "2023-08-09"), "rxDate", "EMR prescription");

      assertThat(datesOf(sorted)).containsExactly("2023-08-09", "2020-01-01", "", "");
    }

    @Test
    @DisplayName("should leave the posted array unmodified")
    void shouldNotMutateTheSourceArray() throws Exception {
      JSONArray source = arrayOf("2020-01-01", "2026-01-01");

      print.sortByDateDesc(source, "rxDate", "EMR prescription");

      assertThat(datesOf(source)).containsExactly("2020-01-01", "2026-01-01");
    }

    @Test
    @DisplayName("should skip a malformed entry rather than lose the whole printout")
    void shouldSkipNonObjectEntries() throws Exception {
      // Same isolation the rendering loops give: one bad element costs one row, not the PDF.
      JSONArray source = arrayOf("2020-01-01", "2026-01-01");
      source.put("not an object");

      JSONArray sorted = print.sortByDateDesc(source, "rxDate", "EMR prescription");

      assertThat(datesOf(sorted)).containsExactly("2026-01-01", "2020-01-01");
    }

    @Test
    @DisplayName("should still order DHDR events by dispense date, as the whenPrepared caller expects")
    void shouldOrderByAnyNamedDateField() throws Exception {
      JSONArray arr = new JSONArray();
      JSONObject older = new JSONObject();
      older.put("whenPrepared", "2021-03-03");
      JSONObject newer = new JSONObject();
      newer.put("whenPrepared", "2022-04-04");
      arr.put(older);
      arr.put(newer);

      JSONArray sorted = print.sortByDateDesc(arr, "whenPrepared", "drug");

      assertThat(sorted.optJSONObject(0).optString("whenPrepared")).isEqualTo("2022-04-04");
    }
  }

  /**
   * BP14 clause 2 requires every event whose patient metadata disagrees with the EMR to be flagged. The
   * Detailed printout carried no patient block at all and took its identity from the result-set
   * headline, so a divergent event was printed under the wrong name with the mismatch marking lost.
   */
  @Nested
  @DisplayName("event patient cell")
  class EventPatientCellTests {

    @Test
    @DisplayName("should mark a value that disagrees with the EMR record")
    void shouldAppendMarker_whenFieldUnmatched() {
      assertThat(print.eventPatientCellValue("5259156783", true)).isEqualTo("5259156783 (UNMATCHED)");
    }

    @Test
    @DisplayName("should leave a matching value unmarked")
    void shouldLeaveValueAlone_whenFieldMatches() {
      assertThat(print.eventPatientCellValue("5259156783", false)).isEqualTo("5259156783");
    }

    @Test
    @DisplayName("should not mark a field the DHDR did not supply")
    void shouldNotMarkAnEmptyValue() {
      // "not recorded in the DHDR" and "recorded differently" are different facts; a bare
      // "(UNMATCHED)" beside no value asserts a comparison that was never made.
      assertThat(print.eventPatientCellValue("", true)).isEmpty();
    }
  }

  /**
   * The viewer derives one nameUnmatched flag from both name parts, and the Detailed view's modal
   * shows the name as a single "Last, First" line. The print splitting it across two rows meant one
   * flag marked two fields, so a surname that differed marked the given name as well - asserting a
   * mismatch on a field that in fact agreed with the EMR. Raised in AI review.
   */
  @Nested
  @DisplayName("eventPatientName")
  class EventPatientNameTests {

    private JSONObject patient(String last, String first) throws Exception {
      JSONObject o = new JSONObject();
      if (last != null) {
        o.put("lastName", last);
      }
      if (first != null) {
        o.put("firstName", first);
      }
      return o;
    }

    @Test
    @DisplayName("should join both parts the way the modal shows them")
    void shouldJoinLastAndFirst() throws Exception {
      assertThat(print.eventPatientName(patient("Kirby", "Susan"))).isEqualTo("Kirby, Susan");
    }

    @Test
    @DisplayName("should render a surname alone without a dangling separator")
    void shouldOmitSeparator_whenFirstNameMissing() throws Exception {
      assertThat(print.eventPatientName(patient("Kirby", ""))).isEqualTo("Kirby");
    }

    @Test
    @DisplayName("should render a given name alone without a leading separator")
    void shouldOmitSeparator_whenLastNameMissing() throws Exception {
      assertThat(print.eventPatientName(patient(null, "Susan"))).isEqualTo("Susan");
    }

    @Test
    @DisplayName("should be empty when the DHDR recorded no name at all")
    void shouldBeEmpty_whenNeitherPartRecorded() throws Exception {
      assertThat(print.eventPatientName(patient(null, null))).isEmpty();
    }

    @Test
    @DisplayName("should mark the whole name once when either part differs from the EMR")
    void shouldMarkTheCombinedNameOnce() throws Exception {
      // The single flag now governs a single row, so a differing surname can no longer mark a
      // given name that matches.
      String name = print.eventPatientName(patient("Kirbey", "Susan"));
      assertThat(print.eventPatientCellValue(name, true)).isEqualTo("Kirbey, Susan (UNMATCHED)");
    }
  }

  /**
   * The page header joins the same name the Detailed view's patient block does. It used to do so with
   * its own copy of the logic, which appended ", " to an empty surname - so a patient recorded with a
   * given name and no family name headed the page with a dangling separator. Both now share
   * eventPatientName, so the header and the table below it cannot render one patient two ways.
   */
  @Nested
  @DisplayName("buildDhdrDemoLine")
  class DhdrDemoLineTests {

    private JSONObject patient(String last, String first, String dob, String hin) throws Exception {
      JSONObject o = new JSONObject();
      o.put("lastName", last == null ? "" : last);
      o.put("firstName", first == null ? "" : first);
      o.put("dob", dob == null ? "" : dob);
      o.put("hin", hin == null ? "" : hin);
      return o;
    }

    @Test
    @DisplayName("should head the page with the name, DOB, age and HIN")
    void shouldRenderTheFullLine() throws Exception {
      String line = print.buildDhdrDemoLine(patient("Kirby", "Susan", "1980-04-02", "5259156783"));
      assertThat(line).startsWith("DHDR EHR Service - Kirby, Susan");
      assertThat(line).contains("DOB: Apr 2, 1980").contains("HIN: 5259156783");
    }

    @Test
    @DisplayName("should not head the page with a dangling separator when there is no surname")
    void shouldNotLeadWithASeparator_whenLastNameMissing() throws Exception {
      String line = print.buildDhdrDemoLine(patient("", "Susan", "1980-04-02", null));
      assertThat(line).startsWith("DHDR EHR Service - Susan");
      assertThat(line).doesNotContain("- , ");
    }

    @Test
    @DisplayName("should render nothing when the DHDR resolved no patient at all")
    void shouldBeEmpty_whenNothingIdentifiesThePatient() throws Exception {
      assertThat(print.buildDhdrDemoLine(patient("", "", "", ""))).isEmpty();
      assertThat(print.buildDhdrDemoLine(null)).isEmpty();
    }
  }

  /**
   * BP17 says the DHDR maintains Therapeutic Class / Sub-Class for pharmacy services and not for drug
   * dispenses, and the drug grid printed a bare "/" for every row. The first fix removed the column,
   * which was wrong: the IG's own example bundles carry AHFS codings on drug dispenses, so a service
   * that supplies them would have had them hidden. Rendering the separator conditionally is correct
   * either way, and it also covers a pharmacy service that carries only one of the pair.
   */
  @Nested
  @DisplayName("therapeuticClassText")
  class TherapeuticClassTests {

    private JSONObject med(String cls, String sub) throws Exception {
      JSONObject o = new JSONObject();
      if (cls != null) {
        o.put("ahfsClass", cls);
      }
      if (sub != null) {
        o.put("ahfsSubClass", sub);
      }
      return o;
    }

    @Test
    @DisplayName("should join class and sub-class when both are present")
    void shouldJoinBoth() throws Exception {
      assertThat(print.therapeuticClassText(med("CENTRAL NERVOUS SYSTEM DRUG", "NONSTEROIDAL ANTI-INFLAMMATORY AGENTS")))
          .isEqualTo("CENTRAL NERVOUS SYSTEM DRUG / NONSTEROIDAL ANTI-INFLAMMATORY AGENTS");
    }

    @Test
    @DisplayName("should render the class alone rather than a trailing separator")
    void shouldRenderClassAlone() throws Exception {
      assertThat(print.therapeuticClassText(med("CENTRAL NERVOUS SYSTEM DRUG", null)))
          .isEqualTo("CENTRAL NERVOUS SYSTEM DRUG");
    }

    @Test
    @DisplayName("should render the sub-class alone rather than a leading separator")
    void shouldRenderSubClassAlone() throws Exception {
      assertThat(print.therapeuticClassText(med(null, "NONSTEROIDAL ANTI-INFLAMMATORY AGENTS")))
          .isEqualTo("NONSTEROIDAL ANTI-INFLAMMATORY AGENTS");
    }

    @Test
    @DisplayName("should render nothing at all when the DHDR supplied neither")
    void shouldRenderNothing_whenNeitherPresent() throws Exception {
      // The reported defect: every drug row carried an orphaned "/".
      assertThat(print.therapeuticClassText(med(null, null))).isEmpty();
    }
  }
}
