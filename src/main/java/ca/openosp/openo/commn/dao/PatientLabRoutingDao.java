//CHECKSTYLE:OFF


package ca.openosp.openo.commn.dao;

import java.util.Date;
import java.util.List;

import ca.openosp.openo.commn.model.PatientLabRouting;

public interface PatientLabRoutingDao extends AbstractDao<PatientLabRouting> {

    public static final Integer UNMATCHED = 0;
    public static final String HL7 = "HL7";

    public PatientLabRouting findDemographicByLabId(Integer labId);

    public PatientLabRouting findDemographics(String labType, Integer labNo);

    public List<PatientLabRouting> findDocByDemographic(Integer docNum);

    public PatientLabRouting findByLabNo(int labNo);

    /**
     * Batch-finds routing records for a set of lab numbers in a single query, to avoid
     * per-row lookups when resolving lab sub-types for a page of results.
     *
     * @param labNos List&lt;Integer&gt; the lab numbers to fetch routing records for
     * @return List&lt;PatientLabRouting&gt; routing records matching the given lab numbers (empty if none found)
     */
    public List<PatientLabRouting> findByLabNos(List<Integer> labNos);

    public List<PatientLabRouting> findByLabNoAndLabType(int labNo, String labType);

    public List<Object[]> findUniqueTestNames(Integer demoId, String labType);

    public List<Object[]> findTests(Integer demoId, String labType);

    public List<Object[]> findUniqueTestNamesForPatientExcelleris(Integer demoNo, String labType);

    public List<PatientLabRouting> findByDemographicAndLabType(Integer demoNo, String labType);

    public List<Object[]> findRoutingsAndTests(Integer demoNo, String labType, String testName);

    public List<Object[]> findRoutingsAndTests(Integer demoNo, String labType);

    public List<Object[]> findMdsRoutings(Integer demoNo, String testName, String labType);

    public List<Object[]> findHl7InfoForRoutingsAndTests(Integer demoNo, String labType, String testName);

    public List<Object[]> findRoutingsAndConsultDocsByRequestId(Integer reqId, String docType);

    public List<Object[]> findResultsByDemographicAndLabType(Integer demographicNo, String labType);

    public List<Object[]> findRoutingAndPhysicianInfoByTypeAndDemoNo(String labType, Integer demographicNo);

    public List<Object[]> findRoutingsAndMdsMshByDemoNo(Integer demographicNo);

    public List<PatientLabRouting> findLabNosByDemographic(Integer demographicNo, String[] labTypes);

    public List<Integer> findDemographicIdsSince(Date date);

    public List<Integer> findLabNosForDemographic(Integer demographicNo, List<Integer> labNos);

}
