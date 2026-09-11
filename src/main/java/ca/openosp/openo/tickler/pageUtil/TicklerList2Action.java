package ca.openosp.openo.tickler.pageUtil;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.ServletActionContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opensymphony.xwork2.ActionSupport;

import ca.openosp.OscarProperties;
import ca.openosp.openo.commn.dao.PatientLabRoutingDao;
import ca.openosp.openo.commn.dao.TicklerDocsDao;
import ca.openosp.openo.commn.model.CustomFilter;
import ca.openosp.openo.commn.model.PatientLabRouting;
import ca.openosp.openo.commn.model.TicklerDocs;
import ca.openosp.openo.documentManager.DocumentAttachmentManager;
import ca.openosp.openo.lab.ca.on.LabResultData;
import ca.openosp.openo.log.LogAction;
import ca.openosp.openo.managers.SecurityInfoManager;
import ca.openosp.openo.managers.TicklerManager;
import ca.openosp.openo.tickler.dto.TicklerCommentDTO;
import ca.openosp.openo.tickler.dto.TicklerListDTO;
import ca.openosp.openo.utility.LoggedInInfo;
import ca.openosp.openo.utility.SpringUtils;

/**
 * Struts2 action that returns paginated tickler data as JSON for DataTables
 * server-side processing. Returns raw data objects; HTML rendering is handled
 * by client-side DataTables column render functions.
 *
 * @since 2026-02-05
 */
public class TicklerList2Action extends ActionSupport {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int MAX_PAGE_SIZE = 500;

    private TicklerManager ticklerManager = SpringUtils.getBean(TicklerManager.class);
    private SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);
    private TicklerDocsDao ticklerDocsDao = SpringUtils.getBean(TicklerDocsDao.class);
    private PatientLabRoutingDao patientLabRoutingDao = SpringUtils.getBean(PatientLabRoutingDao.class);
    private DocumentAttachmentManager documentAttachmentManager = SpringUtils.getBean(DocumentAttachmentManager.class);

    /**
     * Handles DataTables server-side processing requests. Accepts standard
     * DataTables parameters (draw, start, length) plus tickler filter parameters.
     * Returns JSON with raw data fields for client-side rendering.
     *
     * @return null since the response is written directly
     * @throws IOException if writing the JSON response fails
     */
    @Override
    public String execute() throws IOException {
        HttpServletRequest request = ServletActionContext.getRequest();
        HttpServletResponse response = ServletActionContext.getResponse();

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);

        if (!securityInfoManager.hasPrivilege(loggedInInfo, "_tickler", "r", null)) {
            writeJsonError(response, HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return null;
        }

        int draw = parseIntParam(request, "draw", 1);
        int start = Math.max(0, parseIntParam(request, "start", 0));
        int length = parseIntParam(request, "length", 50);
        if (length > 0) {
            length = Math.min(length, MAX_PAGE_SIZE);
        }

        Locale locale = request.getLocale();

        CustomFilter filter;
        try {
            filter = buildFilterFromRequest(request);
        } catch (IllegalArgumentException e) {
            writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid date format, use yyyy-MM-dd");
            return null;
        }

        int totalRecords = ticklerManager.getNumTicklers(loggedInInfo, filter);

        List<TicklerListDTO> ticklers;
        if (length <= 0) {
            ticklers = ticklerManager.getTicklerDTOs(loggedInInfo, filter, 0, 0);
        } else {
            ticklers = ticklerManager.getTicklerDTOs(loggedInInfo, filter, start, length);
        }

        LogAction.addLogSynchronous(loggedInInfo, "TicklerList2Action.execute",
                "ticklers=" + ticklers.size() + ",total=" + totalRecords);

        long ticklerWarnDays = getTicklerWarnDays();
        DateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale);
        DateFormat dateOnlyFormat = new SimpleDateFormat("yyyy-MM-dd", locale);
        DateFormat timeOnlyFormat = new SimpleDateFormat("HH:mm:ss", locale);

        Map<Integer, List<TicklerDocs>> ticklerDocsByTicklerId = loadTicklerDocsByTicklerId(ticklers);
        Map<Integer, String> labTypeByDocumentNo = loadLabTypesByDocumentNo(ticklerDocsByTicklerId);
        Map<Integer, Map<String, String>> formNamesByDemographic =
                loadFormNamesByDemographic(loggedInInfo, ticklers, ticklerDocsByTicklerId);

        ArrayNode dataArray = objectMapper.createArrayNode();
        ObjectNode commentsMap = objectMapper.createObjectNode();

        for (TicklerListDTO tickler : ticklers) {
            boolean warning = isWarning(tickler.getServiceDate(), ticklerWarnDays);
            List<TicklerDocs> ticklerDocs = ticklerDocsByTicklerId.getOrDefault(tickler.getId(), Collections.emptyList());
            Map<String, String> formNames = formNamesByDemographic
                    .getOrDefault(tickler.getDemographicNo(), Collections.emptyMap());
            dataArray.add(buildTicklerRow(tickler, warning, datetimeFormat, dateOnlyFormat, locale, ticklerDocs, labTypeByDocumentNo, formNames));

            List<TicklerCommentDTO> tcomments = tickler.getComments();
            if (tcomments != null && !tcomments.isEmpty()) {
                commentsMap.set(String.valueOf(tickler.getId()),
                        buildCommentsArray(tcomments, datetimeFormat, timeOnlyFormat));
            }
        }

        ObjectNode result = objectMapper.createObjectNode();
        result.put("draw", draw);
        result.put("recordsTotal", totalRecords);
        result.put("recordsFiltered", totalRecords);
        result.set("data", dataArray);
        result.set("comments", commentsMap);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(result.toString());

        return null;
    }

    /**
     * Checks whether a tickler's service date exceeds the configured warning period.
     *
     * @param serviceDate Date the tickler service date
     * @param warnDays long the warning threshold in days, 0 or negative disables warnings
     * @return boolean true if the service date is past the warning threshold
     */
    private boolean isWarning(java.util.Date serviceDate, long warnDays) {
        if (serviceDate == null || warnDays <= 0) {
            return false;
        }
        LocalDateTime service = serviceDate.toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDateTime();
        long daysDifference = Duration.between(service, LocalDateTime.now()).toDays();
        return daysDifference >= warnDays;
    }

    /**
     * Builds a JSON object node representing a single tickler data row.
     *
     * @param tickler TicklerListDTO the tickler data
     * @param warning boolean whether this tickler has triggered a warning
     * @param datetimeFormat DateFormat for full datetime display
     * @param dateOnlyFormat DateFormat for date-only display
     * @param locale Locale for localized status text
     * @param ticklerDocs List&lt;TicklerDocs&gt; this tickler's attachments, pre-fetched in bulk by
     *        {@link #loadTicklerDocsByTicklerId}
     * @param labTypeByDocumentNo Map&lt;Integer, String&gt; lab document number to lab sub-type,
     *        pre-fetched in bulk by {@link #loadLabTypesByDocumentNo}
     * @param formNames Map&lt;String, String&gt; form id to form name for <em>this tickler's patient</em>,
     *        pre-fetched by {@link #loadFormNamesByDemographic}
     * @return ObjectNode the JSON row
     */
    private ObjectNode buildTicklerRow(TicklerListDTO tickler, boolean warning,
                                       DateFormat datetimeFormat, DateFormat dateOnlyFormat,
                                       Locale locale, List<TicklerDocs> ticklerDocs,
                                       Map<Integer, String> labTypeByDocumentNo,
                                       Map<String, String> formNames) {
        ObjectNode row = objectMapper.createObjectNode();
        row.put("id", tickler.getId());
        row.put("demoNo", tickler.getDemographicNo());
        row.put("demoLastName", tickler.getDemographicLastName());
        row.put("demoFirstName", tickler.getDemographicFirstName());
        row.put("creator", tickler.getCreatorFormattedName());
        row.put("serviceDate",
                tickler.getServiceDate() != null ? dateOnlyFormat.format(tickler.getServiceDate()) : "");
        row.put("createDate",
                tickler.getCreateDate() != null ? datetimeFormat.format(tickler.getCreateDate()) : "");
        row.put("priority", String.valueOf(tickler.getPriority()));
        row.put("assignee", tickler.getAssigneeFormattedName());
        row.put("status", tickler.getStatusDesc(locale));
        row.put("message", tickler.getMessage());
        row.put("warning", warning);

        ArrayNode linksArray = objectMapper.createArrayNode();
        for (TicklerDocs td : ticklerDocs) {
            ObjectNode linkNode = objectMapper.createObjectNode();
            linkNode.put("tableName", resolveAttachmentType(td, labTypeByDocumentNo));
            linkNode.put("tableId", td.getDocumentNo());
            if (TicklerDocs.DOCTYPE_FORM.equals(td.getDocType())) {
                // Left absent when the form no longer resolves; the client then renders an
                // unlinked icon rather than a URL that cannot identify a form.
                String formName = formNames.get(String.valueOf(td.getDocumentNo()));
                if (formName != null) {
                    linkNode.put("formName", formName);
                }
            }
            linksArray.add(linkNode);
        }
        row.set("links", linksArray);

        return row;
    }

    /**
     * Batch-loads {@link TicklerDocs} attachments for a page of tickler results in a single query,
     * grouped by tickler id, to avoid an N+1 query pattern when rendering the list.
     *
     * @param ticklers List&lt;TicklerListDTO&gt; the page of ticklers being rendered
     * @return Map&lt;Integer, List&lt;TicklerDocs&gt;&gt; attachments grouped by tickler id
     */
    private Map<Integer, List<TicklerDocs>> loadTicklerDocsByTicklerId(List<TicklerListDTO> ticklers) {
        List<Integer> ticklerIds = ticklers.stream()
                .map(TicklerListDTO::getId)
                .collect(Collectors.toList());
        List<TicklerDocs> ticklerDocs = ticklerDocsDao.findByTicklerIds(ticklerIds);
        return ticklerDocs.stream().collect(Collectors.groupingBy(TicklerDocs::getTicklerId));
    }

    /**
     * Batch-loads lab sub-types (MDS/CML/HL7/BC) for every lab attachment across a page of tickler
     * results in a single query, to avoid an N+1 query pattern when resolving attachment link types.
     *
     * @param ticklerDocsByTicklerId Map&lt;Integer, List&lt;TicklerDocs&gt;&gt; attachments grouped by
     *        tickler id, as returned by {@link #loadTicklerDocsByTicklerId}
     * @return Map&lt;Integer, String&gt; lab document number to lab sub-type
     */
    private Map<Integer, String> loadLabTypesByDocumentNo(Map<Integer, List<TicklerDocs>> ticklerDocsByTicklerId) {
        List<Integer> labDocumentNos = ticklerDocsByTicklerId.values().stream()
                .flatMap(List::stream)
                .filter(td -> isLabDocType(td.getDocType()))
                .map(TicklerDocs::getDocumentNo)
                .distinct()
                .collect(Collectors.toList());
        List<PatientLabRouting> routings = patientLabRoutingDao.findByLabNos(labDocumentNos);
        return routings.stream().collect(Collectors.toMap(
                PatientLabRouting::getLabNo, PatientLabRouting::getLabType, (first, second) -> first));
    }

    /**
     * Loads encounter form names for every patient on this page that has a form attachment.
     *
     * <p>Keyed by demographic, not by document number like {@link #loadLabTypesByDocumentNo}: form
     * ids are unique only within one form's table, so a flat map would label one patient's form
     * with another patient's form name. Resolved in one batched call, since reading the encounter
     * form configuration per patient measured ~100x slower across a full page. Skipped entirely
     * when the page has no form attachments.</p>
     *
     * @param loggedInInfo LoggedInInfo the current user's session information
     * @param ticklers List&lt;TicklerListDTO&gt; the page of ticklers being rendered
     * @param ticklerDocsByTicklerId Map&lt;Integer, List&lt;TicklerDocs&gt;&gt; attachments grouped by
     *        tickler id, as returned by {@link #loadTicklerDocsByTicklerId}
     * @return Map&lt;Integer, Map&lt;String, String&gt;&gt; demographic number to that patient's form
     *         id to form name lookup
     */
    private Map<Integer, Map<String, String>> loadFormNamesByDemographic(
            LoggedInInfo loggedInInfo, List<TicklerListDTO> ticklers,
            Map<Integer, List<TicklerDocs>> ticklerDocsByTicklerId) {
        Set<Integer> demographicNos = ticklers.stream()
                .filter(tickler -> hasFormAttachment(ticklerDocsByTicklerId.get(tickler.getId())))
                .map(TicklerListDTO::getDemographicNo)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (demographicNos.isEmpty()) {
            return Collections.emptyMap();
        }
        return documentAttachmentManager.getFormNamesByDemographic(loggedInInfo, demographicNos);
    }

    /**
     * Checks whether any of a tickler's attachments is an encounter form.
     *
     * @param ticklerDocs List&lt;TicklerDocs&gt; the tickler's attachments, may be null
     * @return boolean true if at least one attachment is an encounter form
     */
    private boolean hasFormAttachment(List<TicklerDocs> ticklerDocs) {
        return ticklerDocs != null && ticklerDocs.stream()
                .anyMatch(td -> TicklerDocs.DOCTYPE_FORM.equals(td.getDocType()));
    }

    /**
     * Checks whether a {@link TicklerDocs} doctype represents a lab attachment (as opposed to a
     * document, HRM report, eForm, or encounter form).
     *
     * @param docType String the doctype to check (see {@link TicklerDocs} DOCTYPE_* constants)
     * @return boolean true if the doctype is a lab attachment
     */
    private boolean isLabDocType(String docType) {
        return !TicklerDocs.DOCTYPE_DOC.equals(docType)
                && !TicklerDocs.DOCTYPE_HRM.equals(docType)
                && !TicklerDocs.DOCTYPE_EFORM.equals(docType)
                && !TicklerDocs.DOCTYPE_FORM.equals(docType);
    }

    /**
     * Resolves the attachment type string used by the client-side link renderer for a
     * tickler document attachment. Lab attachments are resolved to their originating lab
     * sub-type (MDS/CML/HL7/BC) via a pre-fetched batch lookup.
     *
     * @param ticklerDoc TicklerDocs the attachment to resolve
     * @param labTypeByDocumentNo Map&lt;Integer, String&gt; lab document number to lab sub-type,
     *        pre-fetched in bulk by {@link #loadLabTypesByDocumentNo}
     * @return String the attachment type understood by buildAttachmentLink() in ticklerMain.jsp
     */
    private String resolveAttachmentType(TicklerDocs ticklerDoc, Map<Integer, String> labTypeByDocumentNo) {
        String docType = ticklerDoc.getDocType();
        if (TicklerDocs.DOCTYPE_DOC.equals(docType)) {
            return "DOC";
        }
        if (TicklerDocs.DOCTYPE_HRM.equals(docType)) {
            return "HRM";
        }
        if (TicklerDocs.DOCTYPE_EFORM.equals(docType)) {
            return "EFORM";
        }
        if (TicklerDocs.DOCTYPE_FORM.equals(docType)) {
            return "FORM";
        }
        String labType = labTypeByDocumentNo.get(ticklerDoc.getDocumentNo());
        if (LabResultData.MDS.equals(labType)) {
            return "MDS";
        }
        if (LabResultData.CML.equals(labType)) {
            return "CML";
        }
        if (LabResultData.HL7TEXT.equals(labType)) {
            return "HL7";
        }
        return "BC";
    }

    /**
     * Builds a JSON array of comment objects for a tickler.
     *
     * @param comments List of TicklerCommentDTO the comments to serialize
     * @param datetimeFormat DateFormat for full datetime display
     * @param timeOnlyFormat DateFormat for time-only display (used for today's comments)
     * @return ArrayNode the JSON array of comments
     */
    private ArrayNode buildCommentsArray(List<TicklerCommentDTO> comments,
                                         DateFormat datetimeFormat, DateFormat timeOnlyFormat) {
        ArrayNode commentArray = objectMapper.createArrayNode();
        for (TicklerCommentDTO tc : comments) {
            ObjectNode commentObj = objectMapper.createObjectNode();
            commentObj.put("creator", tc.getProviderFormattedName());
            if (tc.getUpdateDate() == null) {
                commentObj.put("createDate", "");
            } else if (tc.isUpdateDateToday()) {
                commentObj.put("createDate", timeOnlyFormat.format(tc.getUpdateDate()));
            } else {
                commentObj.put("createDate", datetimeFormat.format(tc.getUpdateDate()));
            }
            commentObj.put("message", tc.getMessage());
            commentArray.add(commentObj);
        }
        return commentArray;
    }

    /**
     * Parses request parameters and constructs a CustomFilter for the tickler query.
     * Handles both general and demographic-specific filtering through a single path.
     *
     * @param request HttpServletRequest the current request
     * @return CustomFilter populated from request parameters
     * @throws IllegalArgumentException if date parameters are in an invalid format
     */
    private CustomFilter buildFilterFromRequest(HttpServletRequest request) {
        String ticklerview = getStringParam(request, "ticklerview", "A");
        String providerview = getStringParam(request, "providerview", "all");
        String assignedTo = getStringParam(request, "assignedTo", "all");
        String mrpview = getStringParam(request, "mrpview", "all");
        String dateBegin = getStringParam(request, "xml_vdate", "1950-01-01");
        String dateEnd = getStringParam(request, "xml_appointment_date", "");
        int targetDemographic = parseIntParam(request, "demographic_no", 0);

        if (targetDemographic > 0) {
            if (dateEnd.isEmpty()) {
                dateEnd = "8888-12-31";
            }
        } else {
            if (dateEnd.isEmpty()) {
                dateEnd = new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
            }
        }

        CustomFilter filter = new CustomFilter();
        filter.setPriority(null);
        filter.setStatus(ticklerview);
        filter.setStartDateWeb(dateBegin);
        filter.setEndDateWeb(dateEnd);

        if (targetDemographic > 0) {
            filter.setDemographicNo(String.valueOf(targetDemographic));
            filter.setMrp(null);
            filter.setProvider(null);
            filter.setAssignee(null);
        } else {
            if (!mrpview.isEmpty() && !"all".equals(mrpview)) {
                filter.setMrp(mrpview);
            }
            if (!providerview.isEmpty() && !"all".equals(providerview)) {
                filter.setProvider(providerview);
            }
            if (!assignedTo.isEmpty() && !"all".equals(assignedTo)) {
                filter.setAssignee(assignedTo);
            }
        }

        String sortDir = getStringParam(request, "order[0][dir]", "desc");
        if (!"asc".equalsIgnoreCase(sortDir)) {
            sortDir = "desc";
        }
        filter.setSort_order(sortDir);
        return filter;
    }

    /**
     * Writes a JSON error response with the given HTTP status code.
     *
     * @param response HttpServletResponse the response to write to
     * @param statusCode int the HTTP status code
     * @param message String the error message
     * @throws IOException if writing fails
     */
    private void writeJsonError(HttpServletResponse response, int statusCode,
                                String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", message);
        response.getWriter().write(error.toString());
    }

    /**
     * Reads the tickler warning period from application properties.
     *
     * @return the number of days after which a tickler triggers a warning, or 0 if not configured
     */
    private long getTicklerWarnDays() {
        String numDaysUntilWarn = OscarProperties.getInstance().getProperty("tickler_warn_period");
        if (numDaysUntilWarn == null || numDaysUntilWarn.isEmpty()) {
            return 0;
        }
        try {
            return Long.parseLong(numDaysUntilWarn);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int parseIntParam(HttpServletRequest request, String name, int defaultValue) {
        String val = request.getParameter(name);
        if (val == null || val.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String getStringParam(HttpServletRequest request, String name, String defaultValue) {
        String val = request.getParameter(name);
        if (val == null || val.isEmpty()) {
            return defaultValue;
        }
        return val;
    }
}
