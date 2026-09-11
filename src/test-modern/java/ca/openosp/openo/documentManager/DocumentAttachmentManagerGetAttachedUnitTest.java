package ca.openosp.openo.documentManager;

import ca.openosp.openo.commn.dao.ConsultationRequestDao;
import ca.openosp.openo.commn.dao.EFormDataDao;
import ca.openosp.openo.commn.model.ConsultationRequest;
import ca.openosp.openo.commn.model.EFormData;
import ca.openosp.openo.utility.LoggedInInfo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the patient check on the attached-item getters of
 * {@link DocumentAttachmentManagerImpl}: {@code getAttachedDocsForConsult},
 * {@code getAttachedEFormsForConsult}, {@code getAttachedDocsForEForm} and
 * {@code getAttachedEFormsForEForm}.
 *
 * <p>The attached-item queries filter by consultation/eForm id only, so each getter must
 * return nothing unless that consultation or eForm belongs to the requested patient. Only the
 * rejecting paths are covered here: they return before the static {@code EDocUtil} /
 * {@code EFormUtil} queries, which need a Spring context.</p>
 */
@DisplayName("DocumentAttachmentManagerImpl attached-item getters")
@Tag("unit")
@Tag("fast")
@Tag("document")
@Tag("manager")
class DocumentAttachmentManagerGetAttachedUnitTest {

    private static final String PATIENT_DEMO_NO = "42";
    private static final Integer OTHER_PATIENT_DEMO_NO = 43;
    private static final String REQUEST_ID = "700";
    private static final String FDID = "800";

    private ConsultationRequestDao consultationRequestDao;
    private EFormDataDao eFormDataDao;
    private LoggedInInfo loggedInInfo;
    private DocumentAttachmentManagerImpl manager;

    @BeforeEach
    void setUp() throws Exception {
        consultationRequestDao = mock(ConsultationRequestDao.class);
        eFormDataDao = mock(EFormDataDao.class);
        loggedInInfo = mock(LoggedInInfo.class);

        manager = new DocumentAttachmentManagerImpl();
        inject("consultationRequestDao", consultationRequestDao);
        inject("eFormDataDao", eFormDataDao);
    }

    private void inject(String field, Object value) throws Exception {
        Field f = DocumentAttachmentManagerImpl.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(manager, value);
    }

    @Nested
    @DisplayName("consultation getters")
    class Consultation {

        @Test
        @DisplayName("returns nothing when the consultation belongs to another patient")
        void shouldReturnEmpty_whenConsultBelongsToAnotherPatient() {
            ConsultationRequest consult = new ConsultationRequest();
            consult.setDemographicId(OTHER_PATIENT_DEMO_NO);
            when(consultationRequestDao.find(Integer.valueOf(REQUEST_ID))).thenReturn(consult);

            assertThat(manager.getAttachedDocsForConsult(loggedInInfo, PATIENT_DEMO_NO, REQUEST_ID)).isEmpty();
            assertThat(manager.getAttachedEFormsForConsult(PATIENT_DEMO_NO, REQUEST_ID)).isEmpty();
        }

        @Test
        @DisplayName("returns nothing when the consultation doesn't exist")
        void shouldReturnEmpty_whenConsultNotFound() {
            when(consultationRequestDao.find(Integer.valueOf(REQUEST_ID))).thenReturn(null);

            assertThat(manager.getAttachedDocsForConsult(loggedInInfo, PATIENT_DEMO_NO, REQUEST_ID)).isEmpty();
            assertThat(manager.getAttachedEFormsForConsult(PATIENT_DEMO_NO, REQUEST_ID)).isEmpty();
        }

        @Test
        @DisplayName("returns nothing without a lookup when there is no request id (new consultation)")
        void shouldSkipLookup_whenRequestIdNull() {
            assertThat(manager.getAttachedDocsForConsult(loggedInInfo, PATIENT_DEMO_NO, null)).isEmpty();
            assertThat(manager.getAttachedEFormsForConsult(PATIENT_DEMO_NO, null)).isEmpty();
            verifyNoInteractions(consultationRequestDao);
        }
    }

    @Nested
    @DisplayName("eForm getters")
    class EForm {

        @Test
        @DisplayName("returns nothing when the eForm belongs to another patient")
        void shouldReturnEmpty_whenEFormBelongsToAnotherPatient() {
            EFormData eForm = new EFormData();
            eForm.setDemographicId(OTHER_PATIENT_DEMO_NO);
            when(eFormDataDao.find(Integer.valueOf(FDID))).thenReturn(eForm);

            assertThat(manager.getAttachedDocsForEForm(loggedInInfo, PATIENT_DEMO_NO, FDID)).isEmpty();
            assertThat(manager.getAttachedEFormsForEForm(PATIENT_DEMO_NO, FDID)).isEmpty();
        }

        @Test
        @DisplayName("returns nothing when the eForm doesn't exist")
        void shouldReturnEmpty_whenEFormNotFound() {
            when(eFormDataDao.find(Integer.valueOf(FDID))).thenReturn(null);

            assertThat(manager.getAttachedDocsForEForm(loggedInInfo, PATIENT_DEMO_NO, FDID)).isEmpty();
            assertThat(manager.getAttachedEFormsForEForm(PATIENT_DEMO_NO, FDID)).isEmpty();
        }

        @Test
        @DisplayName("returns nothing without a lookup when there is no fdid (new eForm)")
        void shouldSkipLookup_whenFdidNull() {
            assertThat(manager.getAttachedDocsForEForm(loggedInInfo, PATIENT_DEMO_NO, null)).isEmpty();
            assertThat(manager.getAttachedEFormsForEForm(PATIENT_DEMO_NO, null)).isEmpty();
            verifyNoInteractions(eFormDataDao);
        }
    }
}
