//CHECKSTYLE:OFF

package ca.openosp.openo.managers;


import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import ca.openosp.openo.commn.dao.EFormDao.EFormSortOrder;
import ca.openosp.openo.commn.model.EForm;
import ca.openosp.openo.commn.model.EFormData;
import ca.openosp.openo.commn.model.EncounterForm;
import ca.openosp.openo.utility.LoggedInInfo;
import ca.openosp.openo.utility.PDFGenerationException;

import ca.openosp.openo.form.util.FormTransportContainer;
import ca.openosp.openo.encounter.data.EctFormData;
import ca.openosp.openo.encounter.data.EctFormData.PatientForm;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface FormsManager {


    public static final String EFORM = "eform";
    public static final String FORM = "form";

    public List<EForm> findByStatus(LoggedInInfo loggedInInfo, boolean status, EFormSortOrder sortOrder);

    public List<EForm> getEfromInGroupByGroupName(LoggedInInfo loggedInInfo, String groupName);


    public List<String> getGroupNames();


    public List<EFormData> findByDemographicId(LoggedInInfo loggedInInfo, Integer demographicId);

    public List<EncounterForm> getAllEncounterForms();

    public List<EncounterForm> getSelectedEncounterForms();

    public List<PatientForm> getEncounterFormsbyDemographicNumber(LoggedInInfo loggedInInfo, Integer demographicId, boolean getAllVersions, boolean getOnlyPDFReadyForms);

    /**
     * Batched form of {@link #getEncounterFormsbyDemographicNumber}, grouped by demographic.
     *
     * <p>The encounter form configuration is read once for the whole batch instead of once per
     * patient, which is what makes this worth using when rendering a list spanning many patients.</p>
     *
     * @param loggedInInfo LoggedInInfo the current user's session information
     * @param demographicIds Collection&lt;Integer&gt; the patients to look up; may be empty
     * @param getAllVersions boolean true to return every version, false for the most recent only
     * @param getOnlyPDFReadyForms boolean true to restrict to PDF-ready forms
     * @return Map&lt;Integer, List&lt;PatientForm&gt;&gt; forms grouped by demographic number
     */
    public Map<Integer, List<PatientForm>> getEncounterFormsByDemographicNumbers(LoggedInInfo loggedInInfo, Collection<Integer> demographicIds, boolean getAllVersions, boolean getOnlyPDFReadyForms);

    public Integer saveFormDataAsEDoc(LoggedInInfo loggedInInfo, FormTransportContainer formTransportContainer);

    public Path renderForm(HttpServletRequest request, HttpServletResponse response, Integer formId, Integer demographicNo) throws PDFGenerationException;

    public Path renderForm(LoggedInInfo loggedInInfo, FormTransportContainer formTransportContainer);

    public Path renderForm(HttpServletRequest request, HttpServletResponse response, EctFormData.PatientForm form) throws PDFGenerationException;

    public PatientForm getFormById(LoggedInInfo loggedInInfo, Integer formId, Integer demographicNo);
}

