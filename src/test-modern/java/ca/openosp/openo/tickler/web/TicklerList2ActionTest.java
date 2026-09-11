package ca.openosp.openo.tickler.web;

import ca.openosp.openo.commn.dao.PatientLabRoutingDao;
import ca.openosp.openo.commn.dao.TicklerDocsDao;
import ca.openosp.openo.commn.model.CustomFilter;
import ca.openosp.openo.commn.model.Tickler;
import ca.openosp.openo.commn.model.TicklerDocs;
import ca.openosp.openo.documentManager.DocumentAttachmentManager;
import ca.openosp.openo.managers.SecurityInfoManager;
import ca.openosp.openo.managers.TicklerManager;
import ca.openosp.openo.test.base.OpenOWebTestBase;
import ca.openosp.openo.tickler.dto.TicklerCommentDTO;
import ca.openosp.openo.tickler.dto.TicklerListDTO;
import ca.openosp.openo.tickler.pageUtil.TicklerList2Action;
import ca.openosp.openo.utility.LoggedInInfo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Web layer tests for {@link TicklerList2Action}.
 *
 * <p>Validates the DataTables server-side JSON endpoint including privilege
 * enforcement, paging inputs, filter parameters, and JSON response shape.
 *
 * @since 2026-02-06
 */
@DisplayName("TicklerList2Action Web Layer Tests")
@Tag("web")
class TicklerList2ActionTest extends OpenOWebTestBase {

    @Mock
    private TicklerManager mockTicklerManager;

    @Mock
    private TicklerDocsDao mockTicklerDocsDao;

    @Mock
    private PatientLabRoutingDao mockPatientLabRoutingDao;

    @Mock
    private DocumentAttachmentManager mockDocumentAttachmentManager;

    private TicklerList2Action action;

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String TEST_PROVIDER = "999998";

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        replaceSpringUtilsBean(TicklerManager.class, mockTicklerManager);
        replaceSpringUtilsBean(SecurityInfoManager.class, mockSecurityInfoManager);

        when(mockLoggedInInfo.getLoggedInProviderNo()).thenReturn(TEST_PROVIDER);
        setSessionAttribute("user", TEST_PROVIDER);
        String loggedInInfoKey = LoggedInInfo.class.getName() + ".LOGGED_IN_INFO_KEY";
        setSessionAttribute(loggedInInfoKey, mockLoggedInInfo);

        // Default: return empty results
        when(mockTicklerManager.getNumTicklers(any(LoggedInInfo.class), any(CustomFilter.class)))
                .thenReturn(0);
        when(mockTicklerManager.getTicklerDTOs(any(LoggedInInfo.class), any(CustomFilter.class), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(mockTicklerManager.getTicklerDTOs(any(LoggedInInfo.class), any(CustomFilter.class)))
                .thenReturn(Collections.emptyList());

        // Default: no attachments, so link-unrelated tests do not need to stub the attachment path
        when(mockTicklerDocsDao.findByTicklerIds(anyList())).thenReturn(Collections.emptyList());
        when(mockPatientLabRoutingDao.findByLabNos(anyList())).thenReturn(Collections.emptyList());

        action = new TicklerList2Action();
        injectField("ticklerManager", mockTicklerManager);
        injectField("securityInfoManager", mockSecurityInfoManager);
        injectField("ticklerDocsDao", mockTicklerDocsDao);
        injectField("patientLabRoutingDao", mockPatientLabRoutingDao);
        injectField("documentAttachmentManager", mockDocumentAttachmentManager);
    }

    // ── Privilege Enforcement ──────────────────────────────────────────

    @Test
    @DisplayName("Should return 403 JSON error when _tickler read privilege denied")
    void shouldReturn403_whenPrivilegeDenied() throws Exception {
        denyPrivilege("_tickler", "r");

        executeAction(action);

        assertThat(getMockResponse().getStatus()).isEqualTo(403);
        JsonNode json = parseResponse();
        assertThat(json.get("error").asText()).isEqualTo("Access denied");
        verifySecurityCheck("_tickler", "r");
    }

    @Test
    @DisplayName("Should return JSON when _tickler read privilege allowed")
    void shouldReturnJson_whenPrivilegeAllowed() throws Exception {
        allowPrivilege("_tickler", "r");

        executeAction(action);

        assertThat(getMockResponse().getContentType()).contains("application/json");
    }

    // ── JSON Response Shape ────────────────────────────────────────────

    @Test
    @DisplayName("Should return correct top-level JSON structure")
    void shouldReturnCorrectJsonStructure_whenEmptyResults() throws Exception {
        allowPrivilege("_tickler", "r");
        addRequestParameter("draw", "3");

        executeAction(action);

        JsonNode json = parseResponse();
        assertThat(json.get("draw").asInt()).isEqualTo(3);
        assertThat(json.get("recordsTotal").asInt()).isEqualTo(0);
        assertThat(json.get("recordsFiltered").asInt()).isEqualTo(0);
        assertThat(json.get("data").isArray()).isTrue();
        assertThat(json.get("data").size()).isZero();
        assertThat(json.has("comments")).isTrue();
    }

    @Test
    @DisplayName("Should include all required fields in data rows")
    void shouldIncludeAllFields_whenTicklerReturned() throws Exception {
        allowPrivilege("_tickler", "r");

        TicklerListDTO dto = createTestTickler(1, "Follow up on labs");
        stubPaginatedResults(1, List.of(dto));

        executeAction(action);

        JsonNode row = parseResponse().get("data").get(0);
        assertThat(row.get("id").asInt()).isEqualTo(1);
        assertThat(row.get("demoNo").asInt()).isEqualTo(1001);
        assertThat(row.get("demoLastName").asText()).isEqualTo("Smith");
        assertThat(row.get("demoFirstName").asText()).isEqualTo("John");
        assertThat(row.get("creator").asText()).isEqualTo("Doctor, Jane");
        assertThat(row.get("serviceDate").asText()).isNotEmpty();
        assertThat(row.get("createDate").asText()).isNotEmpty();
        assertThat(row.get("priority").asText()).isEqualTo("Normal");
        assertThat(row.get("assignee").asText()).isEqualTo("Nurse, Bob");
        assertThat(row.get("status").asText()).isNotEmpty();
        assertThat(row.get("message").asText()).isEqualTo("Follow up on labs");
        assertThat(row.has("warning")).isTrue();
        assertThat(row.get("links").isArray()).isTrue();
    }

    @Test
    @DisplayName("Should set recordsTotal and recordsFiltered to total count")
    void shouldSetRecordCounts_whenResultsExist() throws Exception {
        allowPrivilege("_tickler", "r");

        stubPaginatedResults(42, List.of(createTestTickler(1, "test")));
        addRequestParameter("draw", "5");

        executeAction(action);

        JsonNode json = parseResponse();
        assertThat(json.get("recordsTotal").asInt()).isEqualTo(42);
        assertThat(json.get("recordsFiltered").asInt()).isEqualTo(42);
        assertThat(json.get("draw").asInt()).isEqualTo(5);
    }

    // ── Comments in Response ───────────────────────────────────────────

    @Test
    @DisplayName("Should include comments as separate map keyed by tickler ID")
    void shouldIncludeCommentsMap_whenTicklerHasComments() throws Exception {
        allowPrivilege("_tickler", "r");

        TicklerListDTO dto = createTestTickler(10, "Needs review");
        TicklerCommentDTO comment = new TicklerCommentDTO(
                100, 10, "Reviewed by specialist", new Date(1700000000000L), "Specialist", "Alice");
        dto.setComments(List.of(comment));
        stubPaginatedResults(1, List.of(dto));

        executeAction(action);

        JsonNode json = parseResponse();
        JsonNode comments = json.get("comments");
        assertThat(comments.has("10")).isTrue();

        JsonNode commentArray = comments.get("10");
        assertThat(commentArray.isArray()).isTrue();
        assertThat(commentArray.size()).isEqualTo(1);
        assertThat(commentArray.get(0).get("creator").asText()).isEqualTo("Specialist, Alice");
        assertThat(commentArray.get(0).get("message").asText()).isEqualTo("Reviewed by specialist");
        assertThat(commentArray.get(0).has("createDate")).isTrue();
    }

    @Test
    @DisplayName("Should omit tickler from comments map when it has no comments")
    void shouldOmitFromCommentsMap_whenNoComments() throws Exception {
        allowPrivilege("_tickler", "r");

        TicklerListDTO dto = createTestTickler(20, "No comments here");
        dto.setComments(Collections.emptyList());
        stubPaginatedResults(1, List.of(dto));

        executeAction(action);

        JsonNode comments = parseResponse().get("comments");
        assertThat(comments.has("20")).isFalse();
    }

    // ── Links in Response ──────────────────────────────────────────────

    @Test
    @DisplayName("Should include links array in data rows")
    void shouldIncludeLinks_whenTicklerHasAttachments() throws Exception {
        allowPrivilege("_tickler", "r");

        TicklerListDTO dto = createTestTickler(5, "Document attached");
        stubPaginatedResults(1, List.of(dto));
        stubAttachments(ticklerDoc(5, 999, TicklerDocs.DOCTYPE_DOC));

        executeAction(action);

        JsonNode links = parseResponse().get("data").get(0).get("links");
        assertThat(links.size()).isEqualTo(1);
        assertThat(links.get(0).get("tableName").asText()).isEqualTo("DOC");
        assertThat(links.get(0).get("tableId").asLong()).isEqualTo(999L);
    }

    @Test
    @DisplayName("Should not repeat a lab number when the same lab is attached to several ticklers")
    void shouldDeduplicateLabNumbers_whenLabAttachedToSeveralTicklers() throws Exception {
        allowPrivilege("_tickler", "r");

        stubPaginatedResults(2, List.of(
                createTestTickler(1, "First"),
                createTestTickler(2, "Second")));
        stubAttachments(
                ticklerDoc(1, 999, TicklerDocs.DOCTYPE_LAB),
                ticklerDoc(2, 999, TicklerDocs.DOCTYPE_LAB));

        executeAction(action);

        // The same lab on two ticklers would otherwise pad the IN (:labNos) list
        ArgumentCaptor<List<Integer>> captor = ArgumentCaptor.forClass(List.class);
        verify(mockPatientLabRoutingDao).findByLabNos(captor.capture());
        assertThat(captor.getValue()).containsExactly(999);
    }

    // ── Form Attachments ───────────────────────────────────────────────

    @Test
    @DisplayName("Should include formName when a form attachment resolves to a name")
    void shouldIncludeFormName_whenFormAttachmentResolves() throws Exception {
        allowPrivilege("_tickler", "r");

        stubPaginatedResults(1, List.of(createTestTickler(5, "Form attached")));
        stubAttachments(ticklerDoc(5, 7, TicklerDocs.DOCTYPE_FORM));
        when(mockDocumentAttachmentManager.getFormNamesByDemographic(any(LoggedInInfo.class), anyCollection()))
                .thenReturn(Map.of(1001, Map.of("7", "Annual")));

        executeAction(action);

        JsonNode link = parseResponse().get("data").get(0).get("links").get(0);
        assertThat(link.get("tableName").asText()).isEqualTo("FORM");
        assertThat(link.get("formName").asText()).isEqualTo("Annual");
    }

    @Test
    @DisplayName("Should omit formName when the form id no longer resolves")
    void shouldOmitFormName_whenFormIdDoesNotResolve() throws Exception {
        allowPrivilege("_tickler", "r");

        stubPaginatedResults(1, List.of(createTestTickler(5, "Deleted form attached")));
        stubAttachments(ticklerDoc(5, 7, TicklerDocs.DOCTYPE_FORM));
        when(mockDocumentAttachmentManager.getFormNamesByDemographic(any(LoggedInInfo.class), anyCollection()))
                .thenReturn(Map.of(1001, Map.of("42", "Annual")));

        executeAction(action);

        // Absent rather than empty, so the client falls back to an unlinked icon
        JsonNode link = parseResponse().get("data").get(0).get("links").get(0);
        assertThat(link.get("tableName").asText()).isEqualTo("FORM");
        assertThat(link.has("formName")).isFalse();
    }

    @Test
    @DisplayName("Should still render the list when form names cannot be read")
    void shouldOmitFormName_whenFormPrivilegeDenied() throws Exception {
        allowPrivilege("_tickler", "r");

        stubPaginatedResults(1, List.of(createTestTickler(5, "Form attached")));
        stubAttachments(ticklerDoc(5, 7, TicklerDocs.DOCTYPE_FORM));
        // Missing "_form" read makes the lookup return nothing rather than throwing
        when(mockDocumentAttachmentManager.getFormNamesByDemographic(any(LoggedInInfo.class), anyCollection()))
                .thenReturn(Collections.emptyMap());

        executeAction(action);

        JsonNode json = parseResponse();
        assertThat(json.get("data").size()).isEqualTo(1);
        assertThat(json.get("data").get(0).get("links").get(0).has("formName")).isFalse();
    }

    @Test
    @DisplayName("Should resolve each form name against its own patient when a page spans patients")
    void shouldResolveFormNamesPerDemographic_whenPageSpansPatients() throws Exception {
        allowPrivilege("_tickler", "r");

        // Same formId for two patients: form ids are only unique within one form's table, so a
        // lookup keyed by document number alone would cross-label these two rows.
        TicklerListDTO first = createTestTickler(1, "Patient A form");
        TicklerListDTO second = new TicklerListDTO(
                2, "Patient B form", new Date(), new Date(),
                Tickler.STATUS.A, Tickler.PRIORITY.Normal,
                2002, "Jones", "Mary",
                "Doctor", "Jane",
                "Nurse", "Bob");
        stubPaginatedResults(2, List.of(first, second));
        stubAttachments(
                ticklerDoc(1, 7, TicklerDocs.DOCTYPE_FORM),
                ticklerDoc(2, 7, TicklerDocs.DOCTYPE_FORM));

        when(mockDocumentAttachmentManager.getFormNamesByDemographic(any(LoggedInInfo.class), anyCollection()))
                .thenReturn(Map.of(
                        1001, Map.of("7", "Annual"),
                        2002, Map.of("7", "Rourke2020")));

        executeAction(action);

        JsonNode data = parseResponse().get("data");
        assertThat(data.get(0).get("links").get(0).get("formName").asText()).isEqualTo("Annual");
        assertThat(data.get(1).get("links").get(0).get("formName").asText()).isEqualTo("Rourke2020");
    }

    @Test
    @DisplayName("Should not look up form names when no form is attached")
    void shouldNotLookUpFormNames_whenNoFormAttachments() throws Exception {
        allowPrivilege("_tickler", "r");

        stubPaginatedResults(1, List.of(createTestTickler(5, "Document attached")));
        stubAttachments(ticklerDoc(5, 999, TicklerDocs.DOCTYPE_DOC));

        executeAction(action);

        verify(mockDocumentAttachmentManager, never())
                .getFormNamesByDemographic(any(LoggedInInfo.class), anyCollection());
    }

    @Test
    @DisplayName("Should resolve every patient on the page in a single batched call")
    void shouldLookUpFormNamesInOneCall_whenPageSpansPatients() throws Exception {
        allowPrivilege("_tickler", "r");

        TicklerListDTO other = new TicklerListDTO(
                2, "Second", new Date(), new Date(),
                Tickler.STATUS.A, Tickler.PRIORITY.Normal,
                2002, "Jones", "Mary",
                "Doctor", "Jane",
                "Nurse", "Bob");
        stubPaginatedResults(2, List.of(createTestTickler(1, "First"), other));
        stubAttachments(
                ticklerDoc(1, 7, TicklerDocs.DOCTYPE_FORM),
                ticklerDoc(2, 8, TicklerDocs.DOCTYPE_FORM));
        when(mockDocumentAttachmentManager.getFormNamesByDemographic(any(LoggedInInfo.class), anyCollection()))
                .thenReturn(Map.of(1001, Map.of("7", "Annual"), 2002, Map.of("8", "Annual")));

        executeAction(action);

        // One call for the whole page, carrying both patients: reading the form config per patient
        // is what made this path slow.
        ArgumentCaptor<java.util.Collection<Integer>> captor = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(mockDocumentAttachmentManager, times(1))
                .getFormNamesByDemographic(any(LoggedInInfo.class), captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(1001, 2002);
    }

    // ── Paging Parameters ──────────────────────────────────────────────

    @Test
    @DisplayName("Should pass start and length to TicklerManager for pagination")
    void shouldPassPagingParams_whenProvided() throws Exception {
        allowPrivilege("_tickler", "r");

        addRequestParameter("start", "25");
        addRequestParameter("length", "50");

        executeAction(action);

        verify(mockTicklerManager).getTicklerDTOs(
                any(LoggedInInfo.class), any(CustomFilter.class), eq(25), eq(50));
    }

    @Test
    @DisplayName("Should default to start=0 and length=50")
    void shouldUseDefaults_whenPagingParamsMissing() throws Exception {
        allowPrivilege("_tickler", "r");

        executeAction(action);

        verify(mockTicklerManager).getTicklerDTOs(
                any(LoggedInInfo.class), any(CustomFilter.class), eq(0), eq(50));
    }

    @Test
    @DisplayName("Should clamp negative start to zero")
    void shouldClampStart_whenNegative() throws Exception {
        allowPrivilege("_tickler", "r");

        addRequestParameter("start", "-10");

        executeAction(action);

        verify(mockTicklerManager).getTicklerDTOs(
                any(LoggedInInfo.class), any(CustomFilter.class), eq(0), anyInt());
    }

    @Test
    @DisplayName("Should cap length at 500")
    void shouldCapLength_whenExceedsMax() throws Exception {
        allowPrivilege("_tickler", "r");

        addRequestParameter("length", "1000");

        executeAction(action);

        verify(mockTicklerManager).getTicklerDTOs(
                any(LoggedInInfo.class), any(CustomFilter.class), anyInt(), eq(500));
    }

    @Test
    @DisplayName("Should fetch without a limit when length is -1")
    void shouldShowAll_whenLengthIsNegative() throws Exception {
        allowPrivilege("_tickler", "r");

        when(mockTicklerManager.getNumTicklers(any(LoggedInInfo.class), any(CustomFilter.class)))
                .thenReturn(150);
        addRequestParameter("length", "-1");

        executeAction(action);

        // limit 0 means unbounded (TicklerDaoImpl only applies a limit when it is > 0), so DataTables
        // "All" returns every row rather than being truncated at MAX_PAGE_SIZE
        verify(mockTicklerManager).getTicklerDTOs(
                any(LoggedInInfo.class), any(CustomFilter.class), eq(0), eq(0));
    }

    @Test
    @DisplayName("Should default draw to 1 when not provided")
    void shouldDefaultDraw_whenMissing() throws Exception {
        allowPrivilege("_tickler", "r");

        executeAction(action);

        JsonNode json = parseResponse();
        assertThat(json.get("draw").asInt()).isEqualTo(1);
    }

    // ── Filter Parameters ──────────────────────────────────────────────

    @Test
    @DisplayName("Should pass ticklerview status to filter")
    void shouldPassStatus_whenTicklerviewProvided() throws Exception {
        allowPrivilege("_tickler", "r");

        addRequestParameter("ticklerview", "C");

        executeAction(action);

        ArgumentCaptor<CustomFilter> captor = ArgumentCaptor.forClass(CustomFilter.class);
        verify(mockTicklerManager).getNumTicklers(any(LoggedInInfo.class), captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("C");
    }

    @Test
    @DisplayName("Should default ticklerview to Active")
    void shouldDefaultToActive_whenTicklerviewMissing() throws Exception {
        allowPrivilege("_tickler", "r");

        executeAction(action);

        ArgumentCaptor<CustomFilter> captor = ArgumentCaptor.forClass(CustomFilter.class);
        verify(mockTicklerManager).getNumTicklers(any(LoggedInInfo.class), captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("A");
    }

    @ParameterizedTest
    @DisplayName("Should pass sort direction to filter")
    @CsvSource({"asc, asc", "desc, desc", "invalid, desc", "'', desc"})
    void shouldPassSortDirection_whenOrderDirProvided(String input, String expected) throws Exception {
        allowPrivilege("_tickler", "r");

        if (input != null && !input.isEmpty()) {
            addRequestParameter("order[0][dir]", input);
        }

        executeAction(action);

        ArgumentCaptor<CustomFilter> captor = ArgumentCaptor.forClass(CustomFilter.class);
        verify(mockTicklerManager).getNumTicklers(any(LoggedInInfo.class), captor.capture());
        assertThat(captor.getValue().getSort_order()).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should clear provider filters when demographic_no is set")
    void shouldClearProviderFilters_whenDemographicSpecified() throws Exception {
        allowPrivilege("_tickler", "r");

        addRequestParameter("demographic_no", "1001");
        addRequestParameter("providerview", "999998");
        addRequestParameter("assignedTo", "999999");
        addRequestParameter("mrpview", "999997");

        executeAction(action);

        ArgumentCaptor<CustomFilter> captor = ArgumentCaptor.forClass(CustomFilter.class);
        verify(mockTicklerManager).getNumTicklers(any(LoggedInInfo.class), captor.capture());
        CustomFilter filter = captor.getValue();
        assertThat(filter.getDemographicNo()).isEqualTo("1001");
        assertThat(filter.getMrp()).isNull();
    }

    @Test
    @DisplayName("Should handle non-numeric parameter values gracefully")
    void shouldHandleGracefully_whenParamsAreNonNumeric() throws Exception {
        allowPrivilege("_tickler", "r");

        addRequestParameter("draw", "abc");
        addRequestParameter("start", "xyz");
        addRequestParameter("length", "!!!");

        executeAction(action);

        JsonNode json = parseResponse();
        // Should fall back to defaults: draw=1, start=0, length=50
        assertThat(json.get("draw").asInt()).isEqualTo(1);
        verify(mockTicklerManager).getTicklerDTOs(
                any(LoggedInInfo.class), any(CustomFilter.class), eq(0), eq(50));
    }

    // ── Invalid Date Handling ─────────────────────────────────────────

    @Test
    @DisplayName("Should return 400 JSON error when date format is invalid")
    void shouldReturn400_whenDateFormatInvalid() throws Exception {
        allowPrivilege("_tickler", "r");

        addRequestParameter("xml_vdate", "not-a-date");

        executeAction(action);

        assertThat(getMockResponse().getStatus()).isEqualTo(400);
        JsonNode json = parseResponse();
        assertThat(json.get("error").asText()).contains("date");
    }

    // ── Warning Flag ───────────────────────────────────────────────────

    @Test
    @DisplayName("Should set warning=false when service date is in the future")
    void shouldNotWarn_whenServiceDateIsFuture() throws Exception {
        allowPrivilege("_tickler", "r");

        TicklerListDTO dto = createTestTickler(1, "future tickler");
        // Service date far in the future — no warn period can trigger
        dto.setServiceDate(new Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000));
        stubPaginatedResults(1, List.of(dto));

        executeAction(action);

        JsonNode row = parseResponse().get("data").get(0);
        assertThat(row.get("warning").asBoolean()).isFalse();
    }

    // ── Null/Empty Dates ───────────────────────────────────────────────

    @Test
    @DisplayName("Should handle null serviceDate and createDate gracefully")
    void shouldReturnEmptyStrings_whenDatesAreNull() throws Exception {
        allowPrivilege("_tickler", "r");

        TicklerListDTO dto = createTestTickler(1, "no dates");
        dto.setServiceDate(null);
        dto.setCreateDate(null);
        stubPaginatedResults(1, List.of(dto));

        executeAction(action);

        JsonNode row = parseResponse().get("data").get(0);
        assertThat(row.get("serviceDate").asText()).isEmpty();
        assertThat(row.get("createDate").asText()).isEmpty();
    }

    @Test
    @DisplayName("Should handle null comment updateDate")
    void shouldReturnEmptyCreateDate_whenCommentUpdateDateIsNull() throws Exception {
        allowPrivilege("_tickler", "r");

        TicklerListDTO dto = createTestTickler(1, "test");
        TicklerCommentDTO comment = new TicklerCommentDTO(1, 1, "comment", null, "Last", "First");
        dto.setComments(List.of(comment));
        stubPaginatedResults(1, List.of(dto));

        executeAction(action);

        JsonNode commentNode = parseResponse().get("comments").get("1").get(0);
        assertThat(commentNode.get("createDate").asText()).isEmpty();
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private TicklerListDTO createTestTickler(int id, String message) {
        return new TicklerListDTO(
                id, message, new Date(), new Date(),
                Tickler.STATUS.A, Tickler.PRIORITY.Normal,
                1001, "Smith", "John",
                "Doctor", "Jane",
                "Nurse", "Bob");
    }

    private TicklerDocs ticklerDoc(int ticklerId, int documentNo, String docType) {
        return new TicklerDocs(ticklerId, documentNo, docType, TEST_PROVIDER);
    }

    private void stubAttachments(TicklerDocs... ticklerDocs) {
        when(mockTicklerDocsDao.findByTicklerIds(anyList())).thenReturn(List.of(ticklerDocs));
    }

    private void stubPaginatedResults(int total, List<TicklerListDTO> ticklers) {
        when(mockTicklerManager.getNumTicklers(any(LoggedInInfo.class), any(CustomFilter.class)))
                .thenReturn(total);
        when(mockTicklerManager.getTicklerDTOs(any(LoggedInInfo.class), any(CustomFilter.class), anyInt(), anyInt()))
                .thenReturn(ticklers);
        when(mockTicklerManager.getTicklerDTOs(any(LoggedInInfo.class), any(CustomFilter.class)))
                .thenReturn(ticklers);
    }

    private JsonNode parseResponse() throws Exception {
        return mapper.readTree(getMockResponse().getContentAsString());
    }

    private void injectField(String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = TicklerList2Action.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(action, value);
    }
}
