package ca.openosp.openo.documentManager;

import ca.openosp.openo.commn.dao.ConsultationRequestDao;
import ca.openosp.openo.commn.dao.EFormDataDao;
import ca.openosp.openo.commn.model.ConsultationRequest;
import ca.openosp.openo.commn.model.EFormData;
import ca.openosp.openo.test.unit.OpenOUnitTestBase;
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
 * return nothing unless that consultation or eForm belongs to the requested patient. The check
 * itself ({@code consultBelongsTo} / {@code eFormBelongsTo}) is tested directly, allowed and
 * rejected. The getters are tested on the rejecting path only: on the allowed path they call
 * the static {@code EDocUtil} / {@code EFormUtil} queries, which need a Spring context.</p>
 */
@DisplayName("DocumentAttachmentManagerImpl attached-item patient check")
@Tag("unit")
@Tag("fast")
@Tag("document")
@Tag("manager")
class DocumentAttachmentManagerGetAttachedUnitTest extends OpenOUnitTestBase {

    private static final Integer PATIENT = 42;
    private static final Integer OTHER_PATIENT = 43;
    private static final Integer REQUEST_NO = 700;
    private static final Integer FDID_NO = 800;

    private static final String PATIENT_DEMO_NO = String.valueOf(PATIENT);
    private static final String REQUEST_ID = String.valueOf(REQUEST_NO);
    private static final String FDID = String.valueOf(FDID_NO);

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
    @DisplayName("consultBelongsTo")
    class ConsultBelongsTo {

        @Test
        @DisplayName("is true when the consultation belongs to the patient")
        void shouldBeTrue_whenConsultBelongsToPatient() {
            when(consultationRequestDao.find(REQUEST_NO)).thenReturn(consultFor(PATIENT));
            assertThat(manager.consultBelongsTo(REQUEST_ID, PATIENT_DEMO_NO)).isTrue();
        }

        @Test
        @DisplayName("is false when the consultation belongs to another patient")
        void shouldBeFalse_whenConsultBelongsToAnotherPatient() {
            when(consultationRequestDao.find(REQUEST_NO)).thenReturn(consultFor(OTHER_PATIENT));
            assertThat(manager.consultBelongsTo(REQUEST_ID, PATIENT_DEMO_NO)).isFalse();
        }

        @Test
        @DisplayName("is false when the consultation doesn't exist")
        void shouldBeFalse_whenConsultNotFound() {
            when(consultationRequestDao.find(REQUEST_NO)).thenReturn(null);
            assertThat(manager.consultBelongsTo(REQUEST_ID, PATIENT_DEMO_NO)).isFalse();
        }

        @Test
        @DisplayName("is false without a lookup when there is no request id (new consultation)")
        void shouldBeFalse_whenRequestIdNull() {
            assertThat(manager.consultBelongsTo(null, PATIENT_DEMO_NO)).isFalse();
            verifyNoInteractions(consultationRequestDao);
        }
    }

    @Nested
    @DisplayName("eFormBelongsTo")
    class EFormBelongsTo {

        @Test
        @DisplayName("is true when the eForm belongs to the patient")
        void shouldBeTrue_whenEFormBelongsToPatient() {
            when(eFormDataDao.find(FDID_NO)).thenReturn(eFormFor(PATIENT));
            assertThat(manager.eFormBelongsTo(FDID, PATIENT_DEMO_NO)).isTrue();
        }

        @Test
        @DisplayName("is false when the eForm belongs to another patient")
        void shouldBeFalse_whenEFormBelongsToAnotherPatient() {
            when(eFormDataDao.find(FDID_NO)).thenReturn(eFormFor(OTHER_PATIENT));
            assertThat(manager.eFormBelongsTo(FDID, PATIENT_DEMO_NO)).isFalse();
        }

        @Test
        @DisplayName("is false when the eForm doesn't exist")
        void shouldBeFalse_whenEFormNotFound() {
            when(eFormDataDao.find(FDID_NO)).thenReturn(null);
            assertThat(manager.eFormBelongsTo(FDID, PATIENT_DEMO_NO)).isFalse();
        }

        @Test
        @DisplayName("is false without a lookup when there is no fdid (new eForm)")
        void shouldBeFalse_whenFdidNull() {
            assertThat(manager.eFormBelongsTo(null, PATIENT_DEMO_NO)).isFalse();
            verifyNoInteractions(eFormDataDao);
        }
    }

    @Nested
    @DisplayName("getters")
    class Getters {

        @Test
        @DisplayName("consultation getters return nothing for another patient's consultation")
        void shouldReturnEmpty_whenConsultBelongsToAnotherPatient() {
            when(consultationRequestDao.find(REQUEST_NO)).thenReturn(consultFor(OTHER_PATIENT));

            assertThat(manager.getAttachedDocsForConsult(loggedInInfo, PATIENT_DEMO_NO, REQUEST_ID)).isEmpty();
            assertThat(manager.getAttachedEFormsForConsult(PATIENT_DEMO_NO, REQUEST_ID)).isEmpty();
        }

        @Test
        @DisplayName("eForm getters return nothing for another patient's eForm")
        void shouldReturnEmpty_whenEFormBelongsToAnotherPatient() {
            when(eFormDataDao.find(FDID_NO)).thenReturn(eFormFor(OTHER_PATIENT));

            assertThat(manager.getAttachedDocsForEForm(loggedInInfo, PATIENT_DEMO_NO, FDID)).isEmpty();
            assertThat(manager.getAttachedEFormsForEForm(PATIENT_DEMO_NO, FDID)).isEmpty();
        }

        @Test
        @DisplayName("all getters return nothing when there is no id")
        void shouldReturnEmpty_whenIdNull() {
            assertThat(manager.getAttachedDocsForConsult(loggedInInfo, PATIENT_DEMO_NO, null)).isEmpty();
            assertThat(manager.getAttachedEFormsForConsult(PATIENT_DEMO_NO, null)).isEmpty();
            assertThat(manager.getAttachedDocsForEForm(loggedInInfo, PATIENT_DEMO_NO, null)).isEmpty();
            assertThat(manager.getAttachedEFormsForEForm(PATIENT_DEMO_NO, null)).isEmpty();
        }
    }

    private static ConsultationRequest consultFor(Integer demographicNo) {
        ConsultationRequest consult = new ConsultationRequest();
        consult.setDemographicId(demographicNo);
        return consult;
    }

    private static EFormData eFormFor(Integer demographicNo) {
        EFormData eForm = new EFormData();
        eForm.setDemographicId(demographicNo);
        return eForm;
    }
}
