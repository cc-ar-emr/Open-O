//CHECKSTYLE:OFF


package ca.openosp.openo.commn.dao;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.Query;

import ca.openosp.openo.commn.NativeSql;
import ca.openosp.openo.commn.model.ConsultationRequest;
import ca.openosp.openo.commn.model.ConsultationRequestExt;
import ca.openosp.openo.commn.model.ProfessionalSpecialist;
import ca.openosp.openo.commn.model.Provider;
import ca.openosp.openo.consultation.dto.ConsultationListDTO;

@SuppressWarnings("unchecked")
public class ConsultationRequestDaoImpl extends AbstractDaoImpl<ConsultationRequest> implements ConsultationRequestDao {

    public ConsultationRequestDaoImpl() {
        super(ConsultationRequest.class);
    }

    public int getCountReferralsAfterCutOffDateAndNotCompleted(Date referralDateCutoff) {
        Query query = entityManager.createNativeQuery("select count(*) from consultationRequests where referalDate < ?1 and status != 4");
        query.setParameter(1, referralDateCutoff);

        return ((BigInteger) query.getSingleResult()).intValue();
    }

    public int getCountReferralsAfterCutOffDateAndNotCompleted(Date referralDateCutoff, String sendto) {
        Query query = entityManager.createNativeQuery("select count(*) from consultationRequests where referalDate < ?1 and status != 4 and sendto = ?2");
        query.setParameter(1, referralDateCutoff);
        query.setParameter(2, sendto);

        return ((BigInteger) query.getSingleResult()).intValue();
    }

    public List<ConsultationRequest> getConsults(Integer demoNo) {
        StringBuilder sql = new StringBuilder("select cr from ConsultationRequest cr, Demographic d, Provider p where d.DemographicNo = cr.demographicId and p.ProviderNo = cr.providerNo and cr.demographicId = ?1");
        Query query = entityManager.createQuery(sql.toString());
        query.setParameter(1, demoNo);

        List<ConsultationRequest> results = query.getResultList();
        return results;
    }


    public List<ConsultationRequest> getConsults(String team, boolean showCompleted, Date startDate, Date endDate, String orderby, String desc, String searchDate, Integer offset, Integer limit) {

        	StringBuilder sql = new StringBuilder("SELECT cr " +
					"FROM ConsultationRequest cr " +
                    "LEFT JOIN cr.professionalSpecialist specialist " +
                    "LEFT JOIN ConsultationServices service ON cr.serviceId = service.serviceId " +
                    "LEFT JOIN ConsultationRequestExt ext ON cr.id = ext.requestId AND ext.key = 'ereferral_service' " +
					"LEFT JOIN Demographic d on cr.demographicId = d.DemographicNo " +
					"LEFT JOIN Provider p on d.ProviderNo = p.ProviderNo WHERE 1=1 ");

        if (!showCompleted) {
            sql.append("and cr.status != 4 ");
        }

        if (!team.isEmpty()) {
            sql.append("and cr.sendTo = :team ");
        }

        if (startDate != null) {
            if (searchDate != null && searchDate.equals("1")) {
                sql.append("and cr.appointmentDate >= :startDate ");
            } else {
                sql.append("and cr.referralDate >= :startDate ");
            }
        }

        if (endDate != null) {
            if (searchDate != null && searchDate.equals("1")) {
                sql.append("and cr.appointmentDate <= :endDate ");
            } else {
                sql.append("and cr.referralDate <= :endDate ");
            }
        }

        if (orderby == null) {
            sql.append("order by cr.referralDate desc ");
        } else {
            String orderDesc = desc != null && desc.equals("1") ? "DESC" : "";
            String service = ", service.serviceDesc";
            switch (orderby) {
                case "1": sql.append("order by cr.status ").append(orderDesc).append(service); break;
                case "2": sql.append("order by cr.sendTo ").append(orderDesc).append(service); break;
                case "3": sql.append("order by d.LastName ").append(orderDesc).append(service); break;
                case "4": sql.append("order by p.LastName ").append(orderDesc).append(service); break;
                case "5": sql.append("order by service.serviceDesc ").append(orderDesc); break;
                case "6": sql.append("order by specialist.lastName ").append(orderDesc).append(service); break;
                case "7": sql.append("order by cr.referralDate ").append(orderDesc); break;
                case "8": sql.append("order by cr.appointmentDate ").append(orderDesc); break;
                case "9": sql.append("order by cr.followUpDate ").append(orderDesc); break;
                default: sql.append("order by cr.referralDate desc"); break;
            }
        }


        Query query = entityManager.createQuery(sql.toString());
        if (!team.isEmpty()) {
            query.setParameter("team", team);
        }
        if (startDate != null) {
            query.setParameter("startDate", startDate);
        }
        if (endDate != null) {
            query.setParameter("endDate", endDate);
        }
        query.setFirstResult(offset != null ? offset : 0);

        //need to never send more than MAX_LIST_RETURN_SIZE
        int myLimit = limit != null ? limit : DEFAULT_CONSULT_REQUEST_RESULTS_LIMIT;
        query.setMaxResults(Math.min(myLimit, MAX_LIST_RETURN_SIZE));

        return query.getResultList();
    }


    public List<ConsultationRequest> getConsultationsByStatus(Integer demographicNo, String status) {
        Query query = entityManager.createQuery("SELECT c FROM ConsultationRequest c where c.demographicId = ?1 and c.status = ?2");
        query.setParameter(1, demographicNo);
        query.setParameter(2, status);


        List<ConsultationRequest> results = query.getResultList();
        return results;
    }

    public ConsultationRequest getConsultation(Integer requestId) {
        return this.find(requestId);
    }


    public List<ConsultationRequest> getReferrals(String providerId, Date cutoffDate) {
        Query query = createQuery("cr", "cr.referralDate <= ?1 AND cr.status = '1' and cr.providerNo = ?2");
        query.setParameter(1, cutoffDate);
        query.setParameter(2, providerId);
        return query.getResultList();
    }

    public List<Object[]> findRequests(Date timeLimit, String providerNo) {
        StringBuilder sql = new StringBuilder("SELECT DISTINCT d.LastName, c.demographicId FROM ConsultationRequest c, Demographic d " +
                "WHERE c.referralDate >= ?1" +
                "AND c.demographicId = d.DemographicNo");
        if (providerNo != null) {
            sql.append(" AND d.ProviderNo = ?2");
        }
        sql.append(" ORDER BY d.LastName");

        Query query = entityManager.createQuery(sql.toString());
        query.setParameter(1, timeLimit);
        if (providerNo != null) {
            query.setParameter(2, providerNo);
        }
        return query.getResultList();
    }

    public List<ConsultationRequest> findRequestsByDemoNo(Integer demoId, Date cutoffDate) {
        Query query = createQuery("cr", "cr.referralDate <= ?1 AND cr.demographicId = ?2");
        query.setParameter(1, cutoffDate);
        query.setParameter(2, demoId);
        return query.getResultList();
    }

    public List<ConsultationRequest> findByDemographicAndService(Integer demographicNo, String serviceName) {
        String sql = "SELECT cr FROM ConsultationRequest cr, ConsultationServices cs WHERE cr.serviceId = cs.serviceId and cr.demographicId = ?1 and cs.serviceDesc = ?2";
        Query query = entityManager.createQuery(sql);
        query.setParameter(1, demographicNo);
        query.setParameter(2, serviceName);

        return query.getResultList();
    }

    public List<ConsultationRequest> findByDemographicAndServices(Integer demographicNo, List<String> serviceNameList) {
        String sql = "SELECT cr FROM ConsultationRequest cr, ConsultationServices cs WHERE cr.serviceId = cs.serviceId and cr.demographicId = ?1 and cs.serviceDesc IN (?2)";
        Query query = entityManager.createQuery(sql);
        query.setParameter(1, demographicNo);
        query.setParameter(2, serviceNameList);

        return query.getResultList();
    }

    @NativeSql("consultationRequests")
    public List<Integer> findNewConsultationsSinceDemoKey(String keyName) {

        String sql = "select distinct dr.demographicNo from consultationRequests dr,demographic d,demographicExt e where dr.demographicNo = d.demographic_no and d.demographic_no = e.demographic_no and e.key_val=?1 and dr.lastUpdateDate > e.value";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, keyName);
        return query.getResultList();
    }

    /**
     * JPQL SELECT clause for DTO constructor projection. Field order must exactly match
     * the {@link ConsultationListDTO} constructor parameter order.
     */
    private static final String DTO_SELECT = "SELECT NEW ca.openosp.openo.consultation.dto.ConsultationListDTO(" +
            "cr.id, cr.status, cr.urgency, " +
            "cr.demographicId, d.LastName, d.FirstName, " +
            "d.ProviderNo, mrp.LastName, mrp.FirstName, " +
            "cr.providerNo, cp.LastName, cp.FirstName, " +
            "cr.serviceId, svc.serviceDesc, " +
            "specialist.lastName, specialist.firstName, " +
            "cr.referralDate, cr.appointmentDate, cr.appointmentTime, " +
            "cr.patientWillBook, cr.followUpDate, " +
            "cr.sendTo, cr.siteName) ";

    /**
     * JPQL FROM clause with LEFT JOINs for DTO projection. Joins:
     * <ul>
     *   <li>Demographic (patient name, provider number)</li>
     *   <li>Provider as mrp (Most Responsible Provider from demographic)</li>
     *   <li>Provider as cp (consulting provider from consultation request)</li>
     *   <li>ConsultationServices (service description)</li>
     *   <li>ProfessionalSpecialist (specialist name via entity relationship)</li>
     * </ul>
     */
    private static final String DTO_FROM = "FROM ConsultationRequest cr " +
            "LEFT JOIN Demographic d ON d.DemographicNo = cr.demographicId " +
            "LEFT JOIN Provider mrp ON mrp.ProviderNo = d.ProviderNo " +
            "LEFT JOIN Provider cp ON cp.ProviderNo = cr.providerNo " +
            "LEFT JOIN ConsultationServices svc ON svc.serviceId = cr.serviceId " +
            "LEFT JOIN cr.professionalSpecialist specialist ";

    /** Escape character used by LIKE patterns; see {@link #escapeLikeWildcards(String)}. */
    private static final String LIKE_ESCAPE_CHAR = "!";

    /** Named parameter prefix for the consultant search terms ("term0", "term1", ...). */
    private static final String NAME_TERM_PARAM = "term";

    /** Number of name terms {@link #CONSULTANT_SEARCH_QUERY} matches; extra terms are ignored. */
    private static final int MAX_NAME_SEARCH_TERMS = 4;

    /**
     * JPQL for the consultant autocomplete search. Each term is matched against the specialist name
     * as the autocomplete displays it ("lastName, firstName"), lowercased for case-insensitive
     * matching, with nested two-argument CONCAT to stay within the JPA spec. Requiring every term
     * to appear somewhere in that string lets terms match in any order and keeps the ", " separator
     * from blocking a match. Term slots are fixed and fully parameterized; unused slots bind "%".
     */
    private static final String CONSULTANT_SEARCH_QUERY = """
            SELECT DISTINCT specialist FROM ConsultationRequest cr
            JOIN cr.professionalSpecialist specialist
            WHERE LOWER(CONCAT(CONCAT(specialist.lastName, ', '), specialist.firstName)) LIKE :term0 ESCAPE '!'
            AND LOWER(CONCAT(CONCAT(specialist.lastName, ', '), specialist.firstName)) LIKE :term1 ESCAPE '!'
            AND LOWER(CONCAT(CONCAT(specialist.lastName, ', '), specialist.firstName)) LIKE :term2 ESCAPE '!'
            AND LOWER(CONCAT(CONCAT(specialist.lastName, ', '), specialist.firstName)) LIKE :term3 ESCAPE '!'
            ORDER BY specialist.lastName, specialist.firstName""";

    /**
     * {@inheritDoc}
     *
     * @since 2026-02-03
     */
    @Override
    public List<ConsultationListDTO> getConsultationDTOs(String team, boolean showCompleted, Date startDate, Date endDate, String orderby, String desc, String searchDate, Integer offset, Integer limit) {
        return getConsultationDTOs(team, showCompleted, startDate, endDate, orderby, desc, searchDate, offset, limit, null, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConsultationListDTO> getConsultationDTOs(String team, boolean showCompleted, Date startDate, Date endDate, String orderby, String desc, String searchDate, Integer offset, Integer limit, Integer consultantId, String filterProviderNo) {
        List<Object> paramList = new ArrayList<>();
        String sql = buildConsultationDTOQuery(paramList, team, showCompleted, startDate, endDate, orderby, desc, searchDate, consultantId, filterProviderNo);

        Query query = entityManager.createQuery(sql, ConsultationListDTO.class);
        for (int i = 0; i < paramList.size(); i++) {
            query.setParameter(i + 1, paramList.get(i));
        }
        query.setFirstResult(offset != null ? offset : 0);
        int myLimit = limit != null ? limit : DEFAULT_CONSULT_REQUEST_RESULTS_LIMIT;
        query.setMaxResults(Math.min(myLimit, MAX_LIST_RETURN_SIZE));

        List<ConsultationListDTO> dtos = query.getResultList();
        loadExtensionsForDTOs(dtos);
        return dtos;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2026-02-03
     */
    @Override
    public List<ConsultationListDTO> getConsultationDTOsByDemographic(Integer demoNo) {
        String sql = DTO_SELECT + DTO_FROM +
                "WHERE cr.demographicId = ?1 " +
                "ORDER BY cr.referralDate ASC";

        Query query = entityManager.createQuery(sql, ConsultationListDTO.class);
        query.setParameter(1, demoNo);

        List<ConsultationListDTO> dtos = query.getResultList();
        loadExtensionsForDTOs(dtos);
        return dtos;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ProfessionalSpecialist> searchDistinctConsultants(String keyword, int maxResults) {
        List<String> terms = tokenizeNameKeyword(keyword);
        if (terms.isEmpty()) {
            return Collections.emptyList();
        }

        Query query = entityManager.createQuery(CONSULTANT_SEARCH_QUERY, ProfessionalSpecialist.class);
        for (int i = 0; i < MAX_NAME_SEARCH_TERMS; i++) {
            // unused term slots match anything, so a keyword with fewer terms is not narrowed
            query.setParameter(NAME_TERM_PARAM + i, i < terms.size() ? "%" + terms.get(i) + "%" : "%");
        }
        query.setMaxResults(maxResults);
        return query.getResultList();
    }

    /**
     * Splits a name search keyword into lowercase terms on whitespace and commas, so the keyword is
     * matched term by term rather than as one literal string. This makes spacing and punctuation
     * around the "lastName, firstName" separator irrelevant: "Smith,B", "Smith,   B", "Smith B" and
     * "B Smith" all match "Smith, Brian". Terms past {@link #MAX_NAME_SEARCH_TERMS} are ignored,
     * which only ever broadens the result set.
     *
     * @param keyword String the raw user-typed keyword (null or punctuation-only yields no terms)
     * @return List of lowercase terms, LIKE wildcards escaped, in the order typed
     */
    private static List<String> tokenizeNameKeyword(String keyword) {
        List<String> terms = new ArrayList<>();
        if (keyword == null) {
            return terms;
        }
        for (String term : keyword.toLowerCase().split("[\\s,]+")) {
            if (!term.isEmpty()) {
                terms.add(escapeLikeWildcards(term));
                if (terms.size() == MAX_NAME_SEARCH_TERMS) {
                    break;
                }
            }
        }
        return terms;
    }

    /**
     * Escapes LIKE wildcards so a user-typed "%" or "_" matches literally instead of acting as a
     * wildcard. Only valid against a LIKE carrying {@code ESCAPE '<LIKE_ESCAPE_CHAR>'}.
     *
     * @param term String the search term to escape
     * @return String the term with the escape character, "%" and "_" escaped
     */
    private static String escapeLikeWildcards(String term) {
        return term.replace(LIKE_ESCAPE_CHAR, LIKE_ESCAPE_CHAR + LIKE_ESCAPE_CHAR)
                .replace("%", LIKE_ESCAPE_CHAR + "%")
                .replace("_", LIKE_ESCAPE_CHAR + "_");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Provider> getDistinctConsultProviders() {
        return entityManager.createQuery("SELECT DISTINCT mrp FROM ConsultationRequest cr JOIN Demographic d ON d.DemographicNo = cr.demographicId JOIN Provider mrp ON mrp.ProviderNo = d.ProviderNo ORDER BY mrp.LastName, mrp.FirstName", Provider.class).getResultList();
    }

    /**
     * Builds the complete JPQL query string for DTO projection with parameterized filters and sorting.
     * Uses positional parameters to prevent SQL injection (replacing the previous string concatenation pattern).
     *
     * @param paramList List to populate with positional query parameter values
     * @param team String the team filter (null or empty to skip)
     * @param showCompleted boolean whether to include completed consultations
     * @param startDate Date the start date bound (null to skip)
     * @param endDate Date the end date bound (null to skip)
     * @param orderby String the sort column identifier (1-9)
     * @param desc String "1" for descending, otherwise ascending
     * @param searchDate String "1" to filter on appointment date instead of referral date
     * @param consultantId Integer the ProfessionalSpecialist id to filter by (null to skip)
     * @param filterProviderNo String the patient MRP provider number to filter by (null/empty to skip)
     * @return String the complete JPQL query
     */
    private String buildConsultationDTOQuery(List<Object> paramList, String team, boolean showCompleted, Date startDate, Date endDate, String orderby, String desc, String searchDate, Integer consultantId, String filterProviderNo) {
        int paramIndex = 1;
        StringBuilder sql = new StringBuilder(DTO_SELECT);
        sql.append(DTO_FROM);
        sql.append("WHERE 1=1 ");

        if (!showCompleted) {
            sql.append("AND cr.status != '4' ");
        }

        if (team != null && !team.isEmpty()) {
            sql.append("AND cr.sendTo = ?").append(paramIndex++).append(" ");
            paramList.add(team);
        }

        if (consultantId != null) {
            sql.append("AND specialist.id = ?").append(paramIndex++).append(" ");
            paramList.add(consultantId);
        }

        if (filterProviderNo != null && !filterProviderNo.isEmpty()) {
            sql.append("AND mrp.ProviderNo = ?").append(paramIndex++).append(" ");
            paramList.add(filterProviderNo);
        }

        if (startDate != null) {
            if ("1".equals(searchDate)) {
                sql.append("AND cr.appointmentDate >= ?").append(paramIndex++).append(" ");
            } else {
                sql.append("AND cr.referralDate >= ?").append(paramIndex++).append(" ");
            }
            paramList.add(startDate);
        }

        if (endDate != null) {
            if ("1".equals(searchDate)) {
                sql.append("AND cr.appointmentDate <= ?").append(paramIndex++).append(" ");
            } else {
                sql.append("AND cr.referralDate <= ?").append(paramIndex++).append(" ");
            }
            paramList.add(endDate);
        }

        if (orderby == null) {
            sql.append("ORDER BY cr.referralDate DESC ");
        } else {
            String orderDesc = desc != null && desc.equals("1") ? "DESC" : "";
            String svcSort = ", svc.serviceDesc";
            switch (orderby) {
                case "1": sql.append("ORDER BY cr.status ").append(orderDesc).append(svcSort); break;
                case "2": sql.append("ORDER BY cr.sendTo ").append(orderDesc).append(svcSort); break;
                case "3": sql.append("ORDER BY d.LastName ").append(orderDesc).append(svcSort); break;
                case "4": sql.append("ORDER BY mrp.LastName ").append(orderDesc).append(svcSort); break;
                case "5": sql.append("ORDER BY svc.serviceDesc ").append(orderDesc); break;
                case "6": sql.append("ORDER BY specialist.lastName ").append(orderDesc).append(svcSort); break;
                case "7": sql.append("ORDER BY cr.referralDate ").append(orderDesc); break;
                case "8": sql.append("ORDER BY cr.appointmentDate ").append(orderDesc); break;
                case "9": sql.append("ORDER BY cr.followUpDate ").append(orderDesc); break;
                default: sql.append("ORDER BY cr.referralDate DESC"); break;
            }
        }

        return sql.toString();
    }

    /**
     * Batch-loads all {@link ConsultationRequestExt} records for the given DTOs in a single
     * {@code IN(:ids)} query, then groups them by request ID and applies each group to its
     * corresponding DTO via {@link ConsultationListDTO#applyExtensions(Map)}.
     * <p>
     * This handles eReferral extension fields (ereferral_ref, ereferral_service, ereferral_doctor)
     * that were previously fetched individually per consultation request in the N+1 loop.
     * </p>
     *
     * @param dtos List of ConsultationListDTO to enrich with extension data
     */
    private void loadExtensionsForDTOs(List<ConsultationListDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }

        List<Integer> consultIds = dtos.stream()
                .map(ConsultationListDTO::getId)
                .collect(Collectors.toList());

        List<ConsultationRequestExt> allExts = entityManager
                .createQuery("SELECT e FROM ConsultationRequestExt e WHERE e.requestId IN (:ids)", ConsultationRequestExt.class)
                .setParameter("ids", consultIds)
                .getResultList();

        Map<Integer, Map<String, String>> extsByRequest = new HashMap<>();
        for (ConsultationRequestExt ext : allExts) {
            extsByRequest
                    .computeIfAbsent(ext.getRequestId(), k -> new HashMap<>())
                    .put(ext.getKey(), ext.getValue());
        }

        for (ConsultationListDTO dto : dtos) {
            dto.applyExtensions(extsByRequest.getOrDefault(dto.getId(), Collections.emptyMap()));
        }
    }
}
