package ca.openosp.openo.integration.dhdr;

import ca.openosp.openo.commn.dao.DemographicDao;
import ca.openosp.openo.commn.dao.OMDGatewayTransactionLogDao;
import ca.openosp.openo.commn.model.Demographic;
import ca.openosp.openo.commn.model.OMDGatewayTransactionLog;
import ca.openosp.openo.commn.model.Provider;
import ca.openosp.openo.integration.dhdr.ConsentOverrideReportService.Row;
import ca.openosp.openo.managers.ProviderManager2;
import ca.openosp.openo.utility.LoggedInInfo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the DHDR Temporary Consent Unblock report (DHDR13.02).
 *
 * <p>Covers the three pieces of logic the report actually owns: the stored-to-displayed decision
 * vocabulary ({@link ConsentOverrideChoice}), the search-form date parsing
 * ({@link ConsentOverrideReport2Action#parseDate}), and the row resolution plus DHDR13.02 search
 * ({@link ConsentOverrideReportService}). The action itself is not exercised - it reads the servlet
 * request in a field initializer - so the report's logic deliberately lives outside it.
 *
 * @since 2026-07-09
 */
@Tag("unit")
@Tag("dhdr")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DHDR consent override report")
class ConsentOverrideReportUnitTest {

  @Nested
  @DisplayName("choice vocabulary")
  class ChoiceVocabulary {

    @Test
    @DisplayName("should expose every stored value so the query and the display cannot drift")
    void shouldExposeEveryStoredValue_whenListingStoredValues() {
      assertThat(ConsentOverrideChoice.storedValues())
          .containsExactly("Overwrite", "Refused", "Cancelled", "Failed", "Unknown")
          .hasSameSizeAs(ConsentOverrideChoice.values());
    }

    @Test
    @DisplayName("should keep the Unknown outcome selectable and labelled by both routes")
    void shouldStoreUnknownOutcome_whenViewletResponseUnrecognised() {
      // The viewer does not write "Unknown" itself - a viewlet that closes without answering is
      // reported as viewletResultNoResponse, and that is what resolves to UNKNOWN. The constant
      // still has to exist for the attempt to be selectable and to have a label at all.
      assertThat(ConsentOverrideChoice.storedValues()).contains("Unknown");
      assertThat(ConsentOverrideChoice.labelFor("Unknown"))
          .isEqualTo("Unknown (unrecognised response)");
      assertThat(ConsentOverrideChoice.labelFor("viewletResultNoResponse", Boolean.FALSE))
          .isEqualTo("Unknown (unrecognised response)");
    }

    @Test
    @DisplayName("should select the viewlet result types too, since nothing ever stores Overwrite")
    void shouldSelectViewletResultTypes_whenListingReportTransactionTypes() {
      // The regression this guards. Selecting on storedValues() alone showed refusals and
      // cancellations taken at the prompt and hid every completed unblock: a clinician who goes
      // ahead does so inside the PCOI viewlet, and that outcome reaches the log through the shared
      // viewlet result endpoint under its own vocabulary. "Overwrite" is written by nothing.
      assertThat(ConsentOverrideChoice.reportTransactionTypes())
          .containsAll(ConsentOverrideChoice.storedValues())
          .contains("viewletResult", "viewletResultCancelled", "viewletResultNoResponse",
              "viewletResultPartial")
          .doesNotContain("consentViewletLaunch");
    }

    @Test
    @DisplayName("should read a confirmed viewlet result as the completed unblock")
    void shouldResolveToOverwrite_whenViewletResultSucceeded() {
      assertThat(ConsentOverrideChoice.fromTransactionLog("viewletResult", Boolean.TRUE))
          .isEqualTo(ConsentOverrideChoice.OVERWRITE);
      assertThat(ConsentOverrideChoice.labelFor("viewletResult", Boolean.TRUE))
          .isEqualTo("Continue (Unblock)");
    }

    @Test
    @DisplayName("should read an unsuccessful viewlet result as a failed override")
    void shouldResolveToFailed_whenViewletResultDidNotSucceed() {
      // viewletResult is written for both a confirmed outcome and a failed one, so the success
      // column is the only thing separating them - a row that never recorded one included.
      assertThat(ConsentOverrideChoice.fromTransactionLog("viewletResult", Boolean.FALSE))
          .isEqualTo(ConsentOverrideChoice.FAILED);
      assertThat(ConsentOverrideChoice.fromTransactionLog("viewletResult", null))
          .isEqualTo(ConsentOverrideChoice.FAILED);
    }

    @Test
    @DisplayName("should map each remaining viewlet outcome to the decision it records")
    void shouldMapViewletOutcomes_whenResolvingTheRemainingTypes() {
      assertThat(ConsentOverrideChoice.fromTransactionLog("viewletResultCancelled", Boolean.TRUE))
          .isEqualTo(ConsentOverrideChoice.CANCELLED);
      assertThat(ConsentOverrideChoice.fromTransactionLog("viewletResultPartial", Boolean.FALSE))
          .isEqualTo(ConsentOverrideChoice.FAILED);
      assertThat(ConsentOverrideChoice.fromTransactionLog("viewletResultNoResponse", Boolean.FALSE))
          .isEqualTo(ConsentOverrideChoice.UNKNOWN);
    }

    @Test
    @DisplayName("should map the unblock decision to the DHDR13.02 continue wording")
    void shouldMapToContinueLabel_whenChoiceIsOverwrite() {
      assertThat(ConsentOverrideChoice.labelFor("Overwrite")).isEqualTo("Continue (Unblock)");
    }

    @Test
    @DisplayName("should surface a failed override as its own operational state")
    void shouldSurfaceFailedState_whenOverrideDidNotComplete() {
      assertThat(ConsentOverrideChoice.labelFor("Failed")).isEqualTo("Failed (did not complete)");
    }

    @Test
    @DisplayName("should pass an unrecognised stored value through rather than hide the row")
    void shouldPassThroughValue_whenStoredValueUnrecognised() {
      assertThat(ConsentOverrideChoice.labelFor("SomeFutureChoice")).isEqualTo("SomeFutureChoice");
    }

    @Test
    @DisplayName("should render a missing stored value as blank")
    void shouldRenderBlank_whenStoredValueIsNull() {
      assertThat(ConsentOverrideChoice.labelFor(null)).isEmpty();
    }

    @Test
    @DisplayName("should not resolve a choice when the stored value is unknown")
    void shouldReturnNull_whenStoredValueUnknown() {
      assertThat(ConsentOverrideChoice.fromStoredValue("nope")).isNull();
      assertThat(ConsentOverrideChoice.fromStoredValue("Cancelled"))
          .isEqualTo(ConsentOverrideChoice.CANCELLED);
    }
  }

  @Nested
  @DisplayName("date filter parsing")
  class DateFilterParsing {

    @Test
    @DisplayName("should parse a valid lower bound at the start of the day")
    void shouldParseStartOfDay_whenLowerBoundSupplied() {
      Date parsed = ConsentOverrideReport2Action.parseDate("2026-07-09", false);

      assertThat(parsed).isNotNull();
      assertThat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(parsed))
          .isEqualTo("2026-07-09 00:00:00");
    }

    @Test
    @DisplayName("should extend a valid upper bound to the end of the day so it is inclusive")
    void shouldExtendToEndOfDay_whenUpperBoundSupplied() {
      Date parsed = ConsentOverrideReport2Action.parseDate("2026-07-09", true);

      assertThat(parsed).isNotNull();
      Calendar cal = Calendar.getInstance();
      cal.setTime(parsed);
      assertThat(cal.get(Calendar.HOUR_OF_DAY)).isEqualTo(23);
      assertThat(cal.get(Calendar.MINUTE)).isEqualTo(59);
      assertThat(cal.get(Calendar.SECOND)).isEqualTo(59);
      assertThat(cal.get(Calendar.MILLISECOND)).isEqualTo(999);
    }

    @Test
    @DisplayName("should reject a non-date value rather than silently widening the range")
    void shouldReturnNull_whenValueIsNotADate() {
      assertThat(ConsentOverrideReport2Action.parseDate("garbage", false)).isNull();
    }

    @Test
    @DisplayName("should reject an out-of-range date instead of rolling it over")
    void shouldReturnNull_whenDateComponentsOutOfRange() {
      // Lenient parsing would quietly turn this into 2027-02-14.
      assertThat(ConsentOverrideReport2Action.parseDate("2026-13-45", false)).isNull();
    }
  }

  @Nested
  @DisplayName("DHDR13.02 search")
  class SearchFilters {

    @Test
    @DisplayName("should match every row when no filter is supplied")
    void shouldMatchRow_whenNoFilterSupplied() {
      assertThat(ConsentOverrideReportService.matchesSearch(row("Smith", "42"), null, null)).isTrue();
    }

    @Test
    @DisplayName("should match the last name regardless of case")
    void shouldMatchRow_whenLastNameDiffersOnlyByCase() {
      assertThat(ConsentOverrideReportService.matchesSearch(row("Smith", "42"), "sMiTh", null))
          .isTrue();
    }

    @Test
    @DisplayName("should match a partial last name")
    void shouldMatchRow_whenLastNameIsAPrefix() {
      assertThat(ConsentOverrideReportService.matchesSearch(row("Smith", "42"), "smi", null)).isTrue();
    }

    @Test
    @DisplayName("should not match a last name search against the patient's first name")
    void shouldNotMatchRow_whenSearchTermIsTheFirstName() {
      Row row = row("Smith", "42");
      row.setPatientName("Smith, John");

      assertThat(ConsentOverrideReportService.matchesSearch(row, "john", null)).isFalse();
    }

    @Test
    @DisplayName("should match the Unique ID exactly, not as a prefix")
    void shouldNotMatchRow_whenUniqueIdIsOnlyAPrefix() {
      assertThat(ConsentOverrideReportService.matchesSearch(row("Smith", "421"), null, "42"))
          .isFalse();
      assertThat(ConsentOverrideReportService.matchesSearch(row("Smith", "42"), null, "42")).isTrue();
    }

    @Test
    @DisplayName("should require both filters when both are supplied")
    void shouldNotMatchRow_whenOnlyOneFilterMatches() {
      assertThat(ConsentOverrideReportService.matchesSearch(row("Smith", "42"), "smith", "99"))
          .isFalse();
    }

    private Row row(String lastName, String uniqueId) {
      Row row = new Row();
      row.setLastName(lastName);
      row.setUniqueId(uniqueId);
      return row;
    }
  }

  @Nested
  @DisplayName("row resolution")
  class RowResolution {

    @Mock private OMDGatewayTransactionLogDao transactionLogDao;
    @Mock private DemographicDao demographicDao;
    @Mock private ProviderManager2 providerManager;
    @Mock private LoggedInInfo loggedInInfo;

    private ConsentOverrideReportService service;

    @BeforeEach
    void setUp() {
      service = new ConsentOverrideReportService(transactionLogDao, demographicDao, providerManager);
    }

    @Test
    @DisplayName("should query the PCOI decision records over the supplied date range")
    void shouldQueryPcoiDecisions_whenBuildingTheReport() {
      when(transactionLogDao.findByExternalSystemAndTransactionTypes(
              anyString(), anyCollection(), any(Date.class), any(Date.class)))
          .thenReturn(List.of());
      Date from = new Date(0L);
      Date to = new Date();

      service.findRows(loggedInInfo, null, null, from, to);

      verify(transactionLogDao).findByExternalSystemAndTransactionTypes(
          eq("PCOI"), eq(ConsentOverrideChoice.reportTransactionTypes()), eq(from), eq(to));
    }

    @Test
    @DisplayName("should resolve the stored keys into the six DHDR13.02 display fields")
    void shouldResolveKeysToDisplayFields_whenRecordHasProviderAndDemographic() {
      when(transactionLogDao.findByExternalSystemAndTransactionTypes(
              anyString(), anyCollection(), any(Date.class), any(Date.class)))
          .thenReturn(List.of(log("Overwrite", "101", 42)));
      when(providerManager.getProvider(loggedInInfo, "101")).thenReturn(provider("Who", "Doctor"));
      when(demographicDao.getDemographics(anyList()))
          .thenReturn(List.of(demographic(42, "Smith", "John", "1234567890")));

      List<Row> rows = service.findRows(loggedInInfo, null, null, new Date(0L), new Date());

      assertThat(rows).hasSize(1);
      Row row = rows.get(0);
      assertThat(row.getDateTime()).isNotEmpty();
      assertThat(row.getEmrUser()).isEqualTo("Who, Doctor");
      assertThat(row.getUniqueId()).isEqualTo("42");
      assertThat(row.getPatientName()).isEqualTo("Smith, John");
      assertThat(row.getHcn()).isEqualTo("1234567890");
      assertThat(row.getChoice()).isEqualTo("Continue (Unblock)");
    }

    @Test
    @DisplayName("should look a provider up once even when they raised several overrides")
    void shouldLookUpProviderOnce_whenSameProviderRaisedManyOverrides() {
      when(transactionLogDao.findByExternalSystemAndTransactionTypes(
              anyString(), anyCollection(), any(Date.class), any(Date.class)))
          .thenReturn(List.of(log("Overwrite", "101", 42), log("Refused", "101", 43)));
      when(providerManager.getProvider(loggedInInfo, "101")).thenReturn(provider("Who", "Doctor"));
      when(demographicDao.getDemographics(anyList()))
          .thenReturn(List.of(demographic(42, "Smith", "John", "1234567890"),
              demographic(43, "Smith", "John", "1234567890")));

      List<Row> rows = service.findRows(loggedInInfo, null, null, new Date(0L), new Date());

      assertThat(rows).hasSize(2);
      verify(providerManager, times(1)).getProvider(loggedInInfo, "101");
    }

    @Test
    @DisplayName("should batch the patient lookup so the report survives past the DAO's id limit")
    @SuppressWarnings("unchecked")
    void shouldBatchDemographicLookup_whenRangeHoldsMoreThanTheDaoLimit() {
      // DemographicDao.getDemographics rejects a list longer than 500 outright rather than
      // truncating it, so resolving every patient in one call made the report throw as soon as an
      // unbounded DHDR13.02 range covered more than 500 of them.
      List<OMDGatewayTransactionLog> logs = new ArrayList<OMDGatewayTransactionLog>();
      for (int demographicNo = 1; demographicNo <= 501; demographicNo++) {
        logs.add(log("Overwrite", "101", demographicNo));
      }
      when(transactionLogDao.findByExternalSystemAndTransactionTypes(
              anyString(), anyCollection(), any(Date.class), any(Date.class)))
          .thenReturn(logs);
      when(providerManager.getProvider(loggedInInfo, "101")).thenReturn(provider("Who", "Doctor"));
      when(demographicDao.getDemographics(anyList())).thenReturn(List.of());

      List<Row> rows = service.findRows(loggedInInfo, null, null, new Date(0L), new Date());

      assertThat(rows).hasSize(501);
      ArgumentCaptor<List<Integer>> batches = ArgumentCaptor.forClass(List.class);
      verify(demographicDao, times(2)).getDemographics(batches.capture());
      assertThat(batches.getAllValues()).extracting(List::size).containsExactly(500, 1);
    }

    @Test
    @DisplayName("should keep the override visible when the patient record cannot be resolved")
    void shouldKeepRow_whenDemographicIsMissing() {
      when(transactionLogDao.findByExternalSystemAndTransactionTypes(
              anyString(), anyCollection(), any(Date.class), any(Date.class)))
          .thenReturn(List.of(log("Cancelled", "101", 42)));
      when(providerManager.getProvider(loggedInInfo, "101")).thenReturn(provider("Who", "Doctor"));
      when(demographicDao.getDemographics(anyList())).thenReturn(List.of());

      List<Row> rows = service.findRows(loggedInInfo, null, null, new Date(0L), new Date());

      assertThat(rows).hasSize(1);
      assertThat(rows.get(0).getUniqueId()).isEqualTo("42");
      assertThat(rows.get(0).getPatientName()).isEmpty();
      assertThat(rows.get(0).getChoice()).isEqualTo("Cancelled");
    }

    @Test
    @DisplayName("should fall back to the provider number when the provider is unknown")
    void shouldFallBackToProviderNo_whenProviderIsUnknown() {
      when(transactionLogDao.findByExternalSystemAndTransactionTypes(
              anyString(), anyCollection(), any(Date.class), any(Date.class)))
          .thenReturn(List.of(log("Refused", "999", 42)));
      when(providerManager.getProvider(loggedInInfo, "999")).thenReturn(null);
      when(demographicDao.getDemographics(anyList()))
          .thenReturn(List.of(demographic(42, "Smith", "John", "1")));

      List<Row> rows = service.findRows(loggedInInfo, null, null, new Date(0L), new Date());

      assertThat(rows.get(0).getEmrUser()).isEqualTo("999");
    }

    @Test
    @DisplayName("should drop rows the search excludes")
    void shouldDropRow_whenSearchDoesNotMatch() {
      when(transactionLogDao.findByExternalSystemAndTransactionTypes(
              anyString(), anyCollection(), any(Date.class), any(Date.class)))
          .thenReturn(List.of(log("Overwrite", "101", 42), log("Overwrite", "101", 43)));
      when(providerManager.getProvider(loggedInInfo, "101")).thenReturn(provider("Who", "Doctor"));
      when(demographicDao.getDemographics(anyList()))
          .thenReturn(List.of(demographic(42, "Smith", "John", "1"),
              demographic(43, "Jones", "Ann", "2")));

      List<Row> rows = service.findRows(loggedInInfo, "jones", null, new Date(0L), new Date());

      assertThat(rows).hasSize(1);
      assertThat(rows.get(0).getPatientName()).isEqualTo("Jones, Ann");
    }

    @Test
    @DisplayName("should resolve the patients in one query however many rows are in range")
    void shouldQueryDemographicsOnce_whenManyRowsAreInRange() {
      when(transactionLogDao.findByExternalSystemAndTransactionTypes(
              anyString(), anyCollection(), any(Date.class), any(Date.class)))
          .thenReturn(List.of(log("Overwrite", "101", 42), log("Refused", "102", 43),
              log("Cancelled", "103", 42)));
      when(providerManager.getProvider(eq(loggedInInfo), anyString()))
          .thenReturn(provider("Who", "Doctor"));
      when(demographicDao.getDemographics(anyList()))
          .thenReturn(List.of(demographic(42, "Smith", "John", "1"),
              demographic(43, "Jones", "Ann", "2")));

      List<Row> rows = service.findRows(loggedInInfo, null, null, new Date(0L), new Date());

      assertThat(rows).hasSize(3);
      // One query for three rows, and the two rows sharing patient 42 do not re-fetch them.
      verify(demographicDao, times(1)).getDemographics(List.of(42, 43));
    }

    @Test
    @DisplayName("should not resolve patients the Unique-ID search has already excluded")
    void shouldNotResolveExcludedPatients_whenSearchingByUniqueId() {
      when(transactionLogDao.findByExternalSystemAndTransactionTypes(
              anyString(), anyCollection(), any(Date.class), any(Date.class)))
          .thenReturn(List.of(log("Overwrite", "101", 42), log("Refused", "101", 43)));
      when(providerManager.getProvider(loggedInInfo, "101")).thenReturn(provider("Who", "Doctor"));
      when(demographicDao.getDemographics(anyList()))
          .thenReturn(List.of(demographic(43, "Jones", "Ann", "2")));

      List<Row> rows = service.findRows(loggedInInfo, null, "43", new Date(0L), new Date());

      assertThat(rows).hasSize(1);
      assertThat(rows.get(0).getUniqueId()).isEqualTo("43");
      // The Unique ID is on the log row, so the excluded patient is never looked up at all - the
      // filter removes the work rather than discarding its result.
      verify(demographicDao, times(1)).getDemographics(List.of(43));
    }

    @Test
    @DisplayName("should not query for patients at all when nothing is in range")
    void shouldNotQueryDemographics_whenNoRowsAreInRange() {
      when(transactionLogDao.findByExternalSystemAndTransactionTypes(
              anyString(), anyCollection(), any(Date.class), any(Date.class)))
          .thenReturn(List.of());

      assertThat(service.findRows(loggedInInfo, null, null, new Date(0L), new Date())).isEmpty();

      verify(demographicDao, never()).getDemographics(anyList());
    }

    @Test
    @DisplayName("should show a completed unblock, which reaches the log as a viewlet result")
    void shouldShowCompletedUnblock_whenOutcomeCameFromTheViewlet() {
      when(transactionLogDao.findByExternalSystemAndTransactionTypes(
              anyString(), anyCollection(), any(Date.class), any(Date.class)))
          .thenReturn(List.of(log("viewletResult", "101", 42, Boolean.TRUE)));
      when(providerManager.getProvider(loggedInInfo, "101")).thenReturn(provider("Who", "Doctor"));
      when(demographicDao.getDemographics(anyList()))
          .thenReturn(List.of(demographic(42, "Smith", "John", "1234567890")));

      List<Row> rows = service.findRows(loggedInInfo, null, null, new Date(0L), new Date());

      assertThat(rows).hasSize(1);
      assertThat(rows.get(0).getChoice()).isEqualTo("Continue (Unblock)");
      assertThat(rows.get(0).getPatientName()).isEqualTo("Smith, John");
    }

    @Test
    @DisplayName("should tell a failed viewlet outcome apart from a completed one")
    void shouldLabelFailedOutcome_whenViewletResultWasNotSuccessful() {
      // Same transaction type as the row above; only the success column differs.
      when(transactionLogDao.findByExternalSystemAndTransactionTypes(
              anyString(), anyCollection(), any(Date.class), any(Date.class)))
          .thenReturn(List.of(log("viewletResult", "101", 42, Boolean.FALSE)));
      when(providerManager.getProvider(loggedInInfo, "101")).thenReturn(provider("Who", "Doctor"));
      when(demographicDao.getDemographics(anyList())).thenReturn(List.of());

      List<Row> rows = service.findRows(loggedInInfo, null, null, new Date(0L), new Date());

      assertThat(rows.get(0).getChoice()).isEqualTo("Failed (did not complete)");
    }

    /**
     * A row from the consent-override path, which stores a decision value directly. Note that
     * "Overwrite" is a value the report can render but that nothing writes - a completed unblock
     * arrives by the other route, so use the four-argument overload for those.
     */
    private OMDGatewayTransactionLog log(String transactionType, String providerNo,
        Integer demographicNo) {
      return log(transactionType, providerNo, demographicNo, null);
    }

    /** A row carrying a success column, as every row written by the viewlet result endpoint does. */
    private OMDGatewayTransactionLog log(String transactionType, String providerNo,
        Integer demographicNo, Boolean success) {
      OMDGatewayTransactionLog log = new OMDGatewayTransactionLog();
      log.setStarted(new Date());
      log.setTransactionType(transactionType);
      log.setInitiatingProviderNo(providerNo);
      log.setDemographicNo(demographicNo);
      log.setSuccess(success);
      return log;
    }

    private Provider provider(String lastName, String firstName) {
      Provider provider = new Provider();
      provider.setLastName(lastName);
      provider.setFirstName(firstName);
      return provider;
    }

    private Demographic demographic(Integer demographicNo, String lastName, String firstName,
        String hin) {
      Demographic demographic = new Demographic();
      demographic.setDemographicNo(demographicNo);
      demographic.setLastName(lastName);
      demographic.setFirstName(firstName);
      demographic.setHin(hin);
      return demographic;
    }
  }
}
