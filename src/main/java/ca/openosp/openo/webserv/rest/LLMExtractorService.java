package ca.openosp.openo.webserv.rest;

import ca.openosp.openo.casemgmt.model.CaseManagementNote;
import ca.openosp.openo.casemgmt.service.NoteSelectionCriteria;
import ca.openosp.openo.casemgmt.service.NoteSelectionResult;
import ca.openosp.openo.casemgmt.service.NoteService;
import ca.openosp.openo.commn.model.Allergy;
import ca.openosp.openo.commn.model.Demographic;
import ca.openosp.openo.commn.model.Document;
import ca.openosp.openo.commn.model.Drug;
import ca.openosp.openo.commn.model.Measurement;
import ca.openosp.openo.commn.model.Prescription;
import ca.openosp.openo.managers.AllergyManager;
import ca.openosp.openo.managers.DemographicManager;
import ca.openosp.openo.managers.DocumentManager;
import ca.openosp.openo.managers.MeasurementManager;
import ca.openosp.openo.managers.PrescriptionManager;
import ca.openosp.openo.PMmodule.dao.SecUserRoleDao;
import ca.openosp.openo.PMmodule.model.SecUserRole;
import ca.openosp.openo.utility.LoggedInInfo;
import ca.openosp.openo.utility.MiscUtils;
import ca.openosp.openo.managers.LlmDocumentTextService;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * Builds an AI-ready patient export as a structured JSON string for LLM ingestion.
 *
 * Design goals:
 *  - Every field is typed (null instead of "N/A", ISO-8601 dates, booleans as booleans)
 *  - Arrays of objects let the LLM count, filter, and compare without re-parsing text
 *  - A top-level "context" block gives the model orientation before it reads patient data
 *  - A "summary_hints" block pre-surfaces counts so the model can answer cheap questions
 *    without scanning the full arrays (e.g. "does this patient have allergies?")
 *  - Notes are kept as structured objects; the note body is still free text, but metadata
 *    (date, provider, type, signed) are typed fields so the model can sort/filter them
 */
@Component
public class LLMExtractorService {

    private static final Logger logger = MiscUtils.getLogger();
    private static final Date EARLIEST_DATE = new Date(0L);
    private static final String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    @Autowired private DemographicManager demographicManager;
    @Autowired private AllergyManager allergyManager;
    @Autowired private PrescriptionManager prescriptionManager;
    @Autowired private DocumentManager documentManager;
    @Autowired private MeasurementManager measurementManager;
    @Autowired private NoteService noteService;
    @Autowired private SecUserRoleDao secUserRoleDao;
    @Autowired private LlmDocumentTextService llmDocumentTextService;


    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns a JSON string representing the full patient export, optimised for
     * LLM ingestion. Use this payload as the context block in your system prompt.
     *
     * @param loggedInInfo current OSCAR logged-in context
     * @param demographicId target patient demographic id
     * @return JSON string — always valid JSON, never null
     */
    public String generatePatientExport(LoggedInInfo loggedInInfo, Integer demographicId) {
        if (demographicId == null || loggedInInfo == null) {
            return toJson(errorPayload("demographicId and loggedInInfo are required"));
        }

        // --- load all data up front so we can populate summary_hints ---
        Demographic demographic = loadDemographic(loggedInInfo, demographicId);
        List<Allergy> allergies         = getAllergies(loggedInInfo, demographicId);
        List<Prescription> prescriptions = getPrescriptions(loggedInInfo, demographicId);
        List<Drug> medications           = getMedications(loggedInInfo, demographicId);
        List<Document> documents         = getDocuments(loggedInInfo, demographicId);
        List<Measurement> measurements   = getMeasurements(loggedInInfo, demographicId);
        List<CaseManagementNote> notes   = getNotes(loggedInInfo, demographicId);

        // --- assemble top-level payload ---
        Map<String, Object> payload = new LinkedHashMap<>();

        // Context block: orients the LLM before it reads any data
        payload.put("context", buildContext(demographicId));

        // Counts so the model can answer cheap questions without scanning arrays
        payload.put("summary_hints", buildSummaryHints(
                demographic, allergies, prescriptions, medications,
                documents, measurements, notes));

        payload.put("demographics",  buildDemographics(demographic));
        payload.put("allergies",     buildAllergies(allergies));
        payload.put("medications",   buildMedications(medications));
        payload.put("prescriptions", buildPrescriptions(prescriptions));
        payload.put("measurements",  buildMeasurements(measurements));
        payload.put("documents",     buildDocuments(loggedInInfo, demographicId, documents));
        payload.put("encounter_notes", buildNotes(notes));

        return toJson(payload);
    }

    // -------------------------------------------------------------------------
    // Context + summary
    // -------------------------------------------------------------------------

    private Map<String, Object> buildContext(Integer demographicId) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("system",          "OSCAR EMR patient export");
        ctx.put("export_version",  "1.0");
        ctx.put("exported_at",     isoNow());
        ctx.put("demographic_id",  demographicId);
        ctx.put("instructions",
            "This payload contains structured clinical data for a single patient. " +
            "Use it to answer physician questions accurately. " +
            "Dates are ISO-8601 UTC. Null fields indicate data not recorded. " +
            "Do not infer diagnoses. Cite specific records when relevant.");
        return ctx;
    }

    private Map<String, Object> buildSummaryHints(
            Demographic d,
            List<Allergy> allergies,
            List<Prescription> prescriptions,
            List<Drug> medications,
            List<Document> documents,
            List<Measurement> measurements,
            List<CaseManagementNote> notes) {

        Map<String, Object> h = new LinkedHashMap<>();
        h.put("patient_name",          d != null ? d.getDisplayName() : null);
        h.put("allergy_count",         allergies.size());
        h.put("has_allergies",         !allergies.isEmpty());
        h.put("active_medication_count", medications.stream().filter(m -> !m.isArchived()).count());
        h.put("archived_medication_count", medications.stream().filter(Drug::isArchived).count());
        h.put("prescription_count",    prescriptions.size());
        h.put("measurement_count",     measurements.size());
        h.put("document_count",        documents.size());
        h.put("encounter_note_count",  notes.size());
        h.put("signed_note_count",     notes.stream().filter(CaseManagementNote::isSigned).count());
        return h;
    }

    // -------------------------------------------------------------------------
    // Section builders
    // -------------------------------------------------------------------------

    private Map<String, Object> buildDemographics(Demographic d) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (d == null) {
            m.put("error", "Demographic record not found or inaccessible");
            return m;
        }
        m.put("demographic_no",  d.getDemographicNo());
        m.put("full_name",       nullIfBlank(d.getDisplayName()));
        m.put("chart_no",        nullIfBlank(d.getChartNo()));
        m.put("date_of_birth",   buildDob(d));        // "YYYY-MM-DD" or null
        m.put("sex",             nullIfBlank(d.getSex()));
        m.put("health_id_number", nullIfBlank(d.getHin()));
        m.put("phone_home",      nullIfBlank(d.getPhone()));
        m.put("phone_work",      nullIfBlank(d.getPhone2()));
        m.put("email",           nullIfBlank(d.getEmail()));
        m.put("address",         nullIfBlank(d.getAddress()));
        m.put("city",            nullIfBlank(d.getCity()));
        m.put("province",        nullIfBlank(d.getProvince()));
        m.put("postal_code",     nullIfBlank(d.getPostal()));
        return m;
    }

    private List<Map<String, Object>> buildAllergies(List<Allergy> allergies) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Allergy a : allergies) {
            Map<String, Object> m = new LinkedHashMap<>();
            String typeCode = a.getTypeCode() != null ? a.getTypeCode().toString() : null;
            m.put("type_code",    typeCode);
            m.put("type_label",   a.getTypeCode() != null ? Allergy.getTypeDesc(a.getTypeCode()) : null);
            m.put("description",  nullIfBlank(a.getDescription()));
            m.put("reaction",     nullIfBlank(a.getReaction()));
            m.put("start_date",   isoDate(a.getStartDate()));
            m.put("updated_at",   isoDate(a.getLastUpdateDate()));
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> buildMedications(List<Drug> medications) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Drug d : medications) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name",          resolveDrugName(d));
            m.put("brand_name",    nullIfBlank(d.getBrandName()));
            m.put("generic_name",  nullIfBlank(d.getGenericName()));
            m.put("dosage",        nullIfBlank(d.getDosage()));
            m.put("frequency",     nullIfBlank(d.getFreqCode()));
            m.put("route",         nullIfBlank(d.getRoute()));
            m.put("special_instructions", nullIfBlank(d.getSpecial()));
            m.put("script_no",     d.getScriptNo());
            m.put("rx_date",       isoDate(d.getRxDate()));
            m.put("end_date",      isoDate(d.getEndDate()));
            m.put("archived",      d.isArchived());
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> buildPrescriptions(List<Prescription> prescriptions) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Prescription p : prescriptions) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",               p.getId());
            m.put("date_prescribed",  isoDate(p.getDatePrescribed()));
            m.put("last_updated_at",  isoDate(p.getLastUpdateDate()));
            m.put("comments",         nullIfBlank(p.getComments()));
            m.put("text",             nullIfBlank(p.getTextView()));
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> buildMeasurements(List<Measurement> measurements) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Measurement meas : measurements) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type",              nullIfBlank(meas.getType()));
            m.put("value",             nullIfBlank(meas.getDataField()));
            m.put("unit_or_instruction", nullIfBlank(meas.getMeasuringInstruction()));
            m.put("observed_at",       isoDate(meas.getDateObserved()));
            m.put("entered_at",        isoDate(meas.getCreateDate()));
            m.put("comments",          nullIfBlank(meas.getComments()));
            m.put("provider_no",       nullIfBlank(meas.getProviderNo()));
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> buildDocuments(LoggedInInfo loggedInInfo, Integer demographicNo, List<Document> documents) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (documents.isEmpty()) return list;
    
        // Bulk-load whatever is already cached in the DB for this patient in one query,
        // rather than hitting the DB once per document inside the loop.
        Map<Integer, String> preloaded = llmDocumentTextService.getPreloadedTexts(demographicNo);
    
        for (Document d : documents) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("document_no",      d.getDocumentNo());
            m.put("title",            firstNonBlank(d.getDocdesc(), d.getDocfilename()));
            m.put("content_type",     nullIfBlank(d.getContenttype()));
            m.put("observation_date", isoDate(d.getObservationdate()));
            m.put("updated_at",       isoDate(d.getUpdatedatetime()));
            m.put("creator",          nullIfBlank(d.getDoccreator()));
            m.put("source",           nullIfBlank(d.getSource()));
    
            String ct = d.getContenttype();
            if (ct != null && ct.toLowerCase().contains("pdf")) {
                Integer docNo = d.getDocumentNo();
    
                if (preloaded.containsKey(docNo)) {
                    // Already in DB cache — use it, no file I/O needed
                    m.put("extracted_text", preloaded.get(docNo));
                } else {
                    // Not yet cached — extract now, service will persist it
                    String text = llmDocumentTextService.getExtractedText(loggedInInfo, d, demographicNo);
                    m.put("extracted_text", text);
                }
            } else {
                m.put("extracted_text", null);
            }
    
            list.add(m);
        }
        return list;
    }


    private List<Map<String, Object>> buildNotes(List<CaseManagementNote> notes) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (CaseManagementNote n : notes) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("observed_at",     isoDate(n.getObservation_date()));
            m.put("updated_at",      isoDate(n.getUpdate_date()));
            m.put("provider_no",     nullIfBlank(n.getProviderNo()));
            m.put("encounter_type",  nullIfBlank(n.getEncounter_type()));
            m.put("signed",          n.isSigned());
            // Note body: trim and preserve internal newlines for readability;
            // the LLM handles multi-line strings natively in JSON
            String body = n.getNote();
            m.put("note", (body != null && !body.trim().isEmpty()) ? body.trim() : null);
            list.add(m);
        }
        return list;
    }

    // -------------------------------------------------------------------------
    // Data loaders (unchanged logic, same error handling pattern)
    // -------------------------------------------------------------------------

    private Demographic loadDemographic(LoggedInInfo li, Integer id) {
        try { return demographicManager.getDemographic(li, id); }
        catch (Exception e) { logger.error("Failed to load demographic {}", id, e); return null; }
    }

    private List<Allergy> getAllergies(LoggedInInfo li, Integer id) {
        try {
            List<Allergy> l = allergyManager.getByDemographicIdUpdatedAfterDate(li, id, EARLIEST_DATE);
            return l != null ? l : Collections.emptyList();
        } catch (Exception e) { logger.error("Failed to load allergies for {}", id, e); return Collections.emptyList(); }
    }

    private List<Prescription> getPrescriptions(LoggedInInfo li, Integer id) {
        try {
            List<Prescription> l = prescriptionManager.getPrescriptionByDemographicIdUpdatedAfterDate(li, id, EARLIEST_DATE);
            return l != null ? l : Collections.emptyList();
        } catch (Exception e) { logger.error("Failed to load prescriptions for {}", id, e); return Collections.emptyList(); }
    }

    private List<Drug> getMedications(LoggedInInfo li, Integer id) {
        try {
            List<Drug> l = prescriptionManager.getMedicationsByDemographicNo(li, id, false);
            return l != null ? l : Collections.emptyList();
        } catch (Exception e) { logger.error("Failed to load medications for {}", id, e); return Collections.emptyList(); }
    }

    private List<Document> getDocuments(LoggedInInfo li, Integer id) {
        try {
            List<Document> l = documentManager.getDocumentsByDemographicIdUpdateAfterDate(li, id, EARLIEST_DATE);
            return l != null ? l : Collections.emptyList();
        } catch (Exception e) { logger.error("Failed to load documents for {}", id, e); return Collections.emptyList(); }
    }

    private List<Measurement> getMeasurements(LoggedInInfo li, Integer id) {
        try {
            List<Measurement> l = measurementManager.getMeasurementByDemographicIdAfter(li, id, EARLIEST_DATE);
            return l != null ? l : Collections.emptyList();
        } catch (Exception e) { logger.error("Failed to load measurements for {}", id, e); return Collections.emptyList(); }
    }

    private List<CaseManagementNote> getNotes(LoggedInInfo li, Integer id) {
        try {
            NoteSelectionCriteria criteria = new NoteSelectionCriteria();
            criteria.setDemographicId(id);
            criteria.setMaxResults(200);
            criteria.setFirstResult(0);
            criteria.setNoteSort("observation_date_desc");
            criteria.setSliceFromEndOfList(false);

            List<SecUserRole> userRoles = secUserRoleDao.getUserRoles(li.getLoggedInProviderNo());
            if (userRoles != null && !userRoles.isEmpty()) {
                criteria.setUserRole(SecUserRole.getRoleNameAsCsv(userRoles));
            }
            criteria.setUserName(li.getLoggedInProviderNo());

            NoteSelectionResult result = noteService.findNotes(li, criteria);
            if (result == null) return Collections.emptyList();

            return result.getNotes().stream()
                .map(nd -> {
                    CaseManagementNote n = new CaseManagementNote();
                    n.setNote(nd.getNote());
                    n.setObservation_date(nd.getObservationDate());
                    n.setUpdate_date(nd.getUpdateDate());
                    n.setProviderNo(nd.getProviderNo());
                    n.setEncounter_type(nd.getEncounterType());
                    n.setSigned(nd.isSigned());
                    return n;
                })
                .collect(Collectors.toList());

        } catch (Exception e) { logger.error("Failed to load notes for {}", id, e); return Collections.emptyList(); }
    }

    // -------------------------------------------------------------------------
    // JSON serialisation (no external dependency — hand-rolled for OSCAR compat)
    // -------------------------------------------------------------------------

    /**
     * Serialises a Map/List/primitive tree to a JSON string.
     * Handles: Map, List, String, Number, Boolean, null.
     * This avoids pulling Jackson/Gson into a codebase that may not have them
     * on the classpath at the webservice layer — swap for ObjectMapper if available.
     */
    @SuppressWarnings("unchecked")
    private String toJson(Object value) {
        if (value == null)             return "null";
        if (value instanceof Boolean)  return value.toString();
        if (value instanceof Number)   return value.toString();
        if (value instanceof String)   return jsonString((String) value);
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) return "[]";
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(toJson(list.get(i)));
            }
            return sb.append(']').toString();
        }
        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> e : map.entrySet()) {
                if (!first) sb.append(',');
                sb.append(jsonString(e.getKey())).append(':').append(toJson(e.getValue()));
                first = false;
            }
            return sb.append('}').toString();
        }
        return jsonString(value.toString());
    }

    /** Escapes a string value for JSON. */
    private String jsonString(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private Map<String, Object> errorPayload(String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("error", message);
        m.put("exported_at", isoNow());
        return m;
    }

    /** Formats a Date as ISO-8601 UTC, or returns null. */
    private String isoDate(Date date) {
        if (date == null) return null;
        // Epoch (0) is the EARLIEST_DATE sentinel — treat as "not recorded"
        if (date.getTime() == 0L) return null;
        SimpleDateFormat sdf = new SimpleDateFormat(ISO_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    private String isoNow() {
        return isoDate(new Date());
    }

    /** Returns "YYYY-MM-DD" built from the three demographic date fields, or null. */
    private String buildDob(Demographic d) {
        String day   = d.getDateOfBirth();
        String month = d.getMonthOfBirth();
        String year  = d.getYearOfBirth();
        if (!isBlank(year) && !isBlank(month) && !isBlank(day)) {
            return year + "-" + zeroPad(month) + "-" + zeroPad(day);
        }
        return null;
    }

    private String zeroPad(String s) {
        return (s != null && s.length() == 1) ? "0" + s : s;
    }

    private String resolveDrugName(Drug d) {
        return firstNonBlank(d.getBrandName(), d.getGenericName(), d.getCustomName());
    }

    private String nullIfBlank(String s) {
        return isBlank(s) ? null : s.trim();
    }

    private String nullIfBlank(Object o) {
        if (o == null) return null;
        return nullIfBlank(String.valueOf(o));
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) if (!isBlank(v)) return v.trim();
        return null;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}