package ca.openosp.openo.integration.dhdr;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ca.openosp.openo.commn.dao.DemographicDao;
import ca.openosp.openo.commn.dao.OMDGatewayTransactionLogDao;
import ca.openosp.openo.commn.model.Demographic;
import ca.openosp.openo.commn.model.OMDGatewayTransactionLog;
import ca.openosp.openo.commn.model.Provider;
import ca.openosp.openo.managers.ProviderManager2;
import ca.openosp.openo.utility.DateUtils;
import ca.openosp.openo.utility.LoggedInInfo;

/**
 * Builds the rows of the DHDR Temporary Consent Unblock report (DHDR13.02).
 *
 * <p>Consent-override decisions are logged against the gateway as keys, not display values: the
 * initiating provider and the patient are recorded as a provider number and a demographic number.
 * This service selects the decision records over a date range, resolves those keys into the fields
 * DHDR13.02 requires (EMR user's name; patient Unique ID, name and Health Card Number), and applies
 * the report's patient last-name and Unique-ID search.
 *
 * <p><strong>The report is deliberately cross-patient.</strong> Unlike the per-patient DHDR viewer,
 * it lists every override in the date range regardless of the viewer's per-demographic privileges,
 * so that consent overrides remain reviewable as a whole. Patient records are therefore read
 * straight from {@link DemographicDao} rather than through {@code DemographicManager}: the manager
 * enforces a per-demographic {@code _demographic} check that would abort the entire report on the
 * first restricted patient, and a patient-scoped no-rights entry there also flags the session as
 * locked. Neither is a sensible outcome for a report whose row set the user did not choose. The
 * caller is responsible for guarding access to the report as a whole.
 *
 * @since 2026-07-09
 */
@Service
public class ConsentOverrideReportService {

  /**
   * The {@code externalSystem} discriminator every consent decision carries, whichever route it
   * took: the consent-override path writes it, and so does the shared viewlet result endpoint when
   * the viewlet it reports on is the consent viewlet. The same system also writes a
   * {@code consentViewletLaunch} record, which {@link ConsentOverrideChoice#reportTransactionTypes()}
   * excludes - opening the viewlet is not a decision.
   */
  static final String EXTERNAL_SYSTEM = "PCOI";

  private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm";

  private final OMDGatewayTransactionLogDao transactionLogDao;
  private final DemographicDao demographicDao;
  private final ProviderManager2 providerManager;

  @Autowired
  public ConsentOverrideReportService(OMDGatewayTransactionLogDao transactionLogDao,
      DemographicDao demographicDao, ProviderManager2 providerManager) {
    this.transactionLogDao = transactionLogDao;
    this.demographicDao = demographicDao;
    this.providerManager = providerManager;
  }

  /**
   * Selects the consent-override decisions in the date range, resolves them for display, and
   * applies the DHDR13.02 search.
   *
   * @param loggedInInfo LoggedInInfo the current session, used for the provider lookup
   * @param searchLastName String the patient last-name filter, or {@code null} for no filter
   * @param searchUniqueId String the patient Unique-ID filter, or {@code null} for no filter
   * @param from Date the inclusive lower bound on the decision timestamp
   * @param to Date the inclusive upper bound on the decision timestamp
   * @return List&lt;Row&gt; the matching rows, newest first (never {@code null})
   */
  public List<Row> findRows(LoggedInInfo loggedInInfo, String searchLastName, String searchUniqueId,
      Date from, Date to) {
    List<OMDGatewayTransactionLog> logs = transactionLogDao.findByExternalSystemAndTransactionTypes(
        EXTERNAL_SYSTEM, ConsentOverrideChoice.reportTransactionTypes(), from, to);

    // The Unique-ID filter needs no lookup at all: it compares the demographicNo already carried on
    // the log row. Applying it here removes the resolution work rather than performing it and then
    // discarding the result - searching for one patient used to resolve the entire history first,
    // so the filters bought no reduction in work whatsoever.
    List<OMDGatewayTransactionLog> candidates = new ArrayList<OMDGatewayTransactionLog>();
    for (OMDGatewayTransactionLog log : logs) {
      if (searchUniqueId == null || searchUniqueId.equals(uniqueIdOf(log))) {
        candidates.add(log);
      }
    }

    // One provider typically raises many overrides; resolve each provider's name at most once.
    Map<String, String> providerNames = new HashMap<String, String>();
    // Likewise for patients, but they need a query rather than a cache lookup: one patient can hold
    // several override decisions, and each was re-fetching the same demographic. The default range
    // is unbounded, so "every row in the report" is the normal case here, not the worst one.
    Map<Integer, Demographic> demographics = loadDemographics(candidates);
    List<Row> rows = new ArrayList<Row>();
    for (OMDGatewayTransactionLog log : candidates) {
      Row row = toRow(loggedInInfo, log, providerNames, demographics);
      // The last-name filter genuinely needs the demographic, so it stays after resolution - but
      // after batching that costs one query regardless of how many rows are in range.
      if (matchesSearch(row, searchLastName, searchUniqueId)) {
        rows.add(row);
      }
    }
    return rows;
  }

  /**
   * Returns the log record's patient Unique ID as the report renders it, for comparison against the
   * search input before any lookup is done.
   *
   * @param log OMDGatewayTransactionLog the consent-override decision record
   * @return String the Unique ID, or {@code null} when the record carries no demographic
   */
  private static String uniqueIdOf(OMDGatewayTransactionLog log) {
    Integer demographicNo = log.getDemographicNo();
    return demographicNo == null ? null : String.valueOf(demographicNo);
  }

  /**
   * The most demographic numbers {@link DemographicDao#getDemographics(List)} accepts in one call.
   * It rejects a longer list outright rather than truncating it, so the caller must chunk.
   */
  private static final int DEMOGRAPHIC_LOOKUP_BATCH = 500;

  /**
   * Loads every distinct patient referenced by the given decision records, in as few queries as the
   * DAO's per-call limit allows.
   *
   * <p>The report's date range is unbounded by default (DHDR13.02 asks for all temporary consent
   * unblock requests), so the number of distinct patients in one report is not something this
   * service can cap. Batching keeps the query count proportional to that number rather than to the
   * row count, without handing the DAO a list it refuses.
   *
   * @param logs List&lt;OMDGatewayTransactionLog&gt; the decision records to be rendered
   * @return Map&lt;Integer, Demographic&gt; the patients by demographic number; a number with no
   *     matching patient is simply absent, which {@code toRow} renders the same as before
   */
  private Map<Integer, Demographic> loadDemographics(List<OMDGatewayTransactionLog> logs) {
    Set<Integer> demographicNos = new LinkedHashSet<Integer>();
    for (OMDGatewayTransactionLog log : logs) {
      if (log.getDemographicNo() != null) {
        demographicNos.add(log.getDemographicNo());
      }
    }
    Map<Integer, Demographic> byDemographicNo = new HashMap<Integer, Demographic>();
    if (demographicNos.isEmpty()) {
      return byDemographicNo;
    }
    List<Integer> ids = new ArrayList<Integer>(demographicNos);
    for (int start = 0; start < ids.size(); start += DEMOGRAPHIC_LOOKUP_BATCH) {
      int end = Math.min(start + DEMOGRAPHIC_LOOKUP_BATCH, ids.size());
      for (Demographic demographic : demographicDao.getDemographics(ids.subList(start, end))) {
        if (demographic != null && demographic.getDemographicNo() != null) {
          byDemographicNo.put(demographic.getDemographicNo(), demographic);
        }
      }
    }
    return byDemographicNo;
  }

  /**
   * Resolves one gateway log record into a display row, looking up the provider and demographic.
   *
   * @param loggedInInfo LoggedInInfo the current session, used for the provider lookup
   * @param log OMDGatewayTransactionLog the consent-override decision record
   * @param providerNames Map&lt;String, String&gt; a per-report providerNo to display-name cache,
   *     read and populated by this method
   * @param demographics Map&lt;Integer, Demographic&gt; the patients for this report, loaded in one
   *     query by {@code loadDemographics}
   * @return Row the resolved display row (never {@code null})
   */
  private Row toRow(LoggedInInfo loggedInInfo, OMDGatewayTransactionLog log,
      Map<String, String> providerNames, Map<Integer, Demographic> demographics) {
    Row row = new Row();
    // DateUtils.format renders null as the empty string, which is what an unstamped record shows.
    row.setDateTime(DateUtils.format(TIMESTAMP_FORMAT, log.getStarted(), null));
    row.setChoice(ConsentOverrideChoice.labelFor(log.getTransactionType(), log.getSuccess()));

    String providerNo = log.getInitiatingProviderNo();
    if (StringUtils.isNotBlank(providerNo)) {
      String name = providerNames.get(providerNo);
      if (name == null) {
        Provider provider = providerManager.getProvider(loggedInInfo, providerNo);
        name = provider == null ? providerNo : provider.getFormattedName();
        providerNames.put(providerNo, name);
      }
      row.setEmrUser(name);
    }

    Integer demographicNo = log.getDemographicNo();
    if (demographicNo != null) {
      row.setUniqueId(String.valueOf(demographicNo));
      Demographic demographic = demographics.get(demographicNo);
      if (demographic != null) {
        row.setLastName(demographic.getLastName());
        row.setPatientName(demographic.getFullName());
        row.setHcn(demographic.getHin());
      }
    }
    return row;
  }

  /**
   * Applies the report's DHDR13.02 search filters: patient Unique ID (exact) and patient last name
   * (case-insensitive substring of the surname alone, so a first-name match cannot satisfy it).
   *
   * @param row Row the resolved row under test
   * @param lastName String the last-name filter, or {@code null} for no filter
   * @param uniqueId String the Unique-ID filter, or {@code null} for no filter
   * @return boolean {@code true} if the row satisfies both supplied filters
   */
  static boolean matchesSearch(Row row, String lastName, String uniqueId) {
    if (uniqueId != null && !uniqueId.equals(row.getUniqueId())) {
      return false;
    }
    if (lastName != null) {
      // Locale.ROOT, not the server default: a Turkish or Azeri default folds "I" to the dotless
      // "ı", so a surname search would silently stop matching any name containing an I. The
      // fold is for comparison only, never displayed, so it wants fixed rules rather than the
      // server's.
      String surname =
          row.getLastName() == null ? "" : row.getLastName().toLowerCase(Locale.ROOT);
      if (!surname.contains(lastName.toLowerCase(Locale.ROOT))) {
        return false;
      }
    }
    return true;
  }

  /**
   * A single resolved report row. Getters expose raw (unencoded) display values; the view is
   * responsible for output encoding. {@code lastName} backs the surname search and is not displayed
   * on its own - {@code patientName} carries the rendered "Last, First".
   */
  public static class Row {
    private String dateTime = "";
    private String emrUser = "";
    private String uniqueId = "";
    private String lastName = "";
    private String patientName = "";
    private String hcn = "";
    private String choice = "";

    public String getDateTime() {
      return dateTime;
    }

    public void setDateTime(String dateTime) {
      this.dateTime = dateTime;
    }

    public String getEmrUser() {
      return emrUser;
    }

    public void setEmrUser(String emrUser) {
      this.emrUser = emrUser;
    }

    public String getUniqueId() {
      return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
      this.uniqueId = uniqueId;
    }

    public String getLastName() {
      return lastName;
    }

    public void setLastName(String lastName) {
      this.lastName = lastName;
    }

    public String getPatientName() {
      return patientName;
    }

    public void setPatientName(String patientName) {
      this.patientName = patientName;
    }

    public String getHcn() {
      return hcn;
    }

    public void setHcn(String hcn) {
      this.hcn = hcn;
    }

    public String getChoice() {
      return choice;
    }

    public void setChoice(String choice) {
      this.choice = choice;
    }
  }
}
