/**
 *
 * This software is published under the GPL GNU General Public License.
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * <p>
 * This software was written for
 * Magenta Health
 * Toronto, Ontario, Canada
 */
package ca.openosp.openo.documentManager;

import ca.openosp.openo.commn.dao.DocumentDao;
import ca.openosp.openo.commn.dao.EFormDataDao;
import ca.openosp.openo.commn.dao.TicklerDocsDao;
import ca.openosp.openo.commn.model.Document;
import ca.openosp.openo.commn.model.EFormData;
import ca.openosp.openo.commn.model.TicklerDocs;
import ca.openosp.openo.commn.model.enumerator.DocumentType;
import ca.openosp.openo.documentManager.data.TicklerAttachmentData;
import ca.openosp.openo.encounter.data.EctFormData;
import ca.openosp.openo.hospitalReportManager.HRMUtil;
import ca.openosp.openo.managers.FormsManager;
import ca.openosp.openo.managers.SecurityInfoManager;
import ca.openosp.openo.test.unit.OpenOUnitTestBase;
import ca.openosp.openo.utility.LoggedInInfo;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DocumentAttachmentManagerImpl#getTicklerAttachmentDetails}.
 *
 * <p>The LAB branch is not covered: it depends on lab infrastructure
 * ({@code CommonLabResultData}) that cannot be mocked meaningfully here.</p>
 *
 * @since 2026-07-19
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentAttachmentManager Tickler Attachment Details Unit Tests")
@Tag("unit")
@Tag("fast")
@Tag("manager")
@Tag("tickler")
public class DocumentAttachmentManagerUnitTest extends OpenOUnitTestBase {

    private static final Integer TICKLER_ID = 42;
    private static final Integer DEMOGRAPHIC_NO = 12345;
    private static final String PROVIDER_NO = "999990";

    @Mock
    private TicklerDocsDao mockTicklerDocsDao;

    @Mock
    private DocumentDao mockDocumentDao;

    @Mock
    private EFormDataDao mockEFormDataDao;

    @Mock
    private FormsManager mockFormsManager;

    @Mock
    private SecurityInfoManager mockSecurityInfoManager;

    @Mock
    private LoggedInInfo mockLoggedInInfo;

    private DocumentAttachmentManagerImpl manager;

    @BeforeEach
    void setUp() {
        // HRMUtil's static initializer fetches these beans; register them before mockStatic(HRMUtil)
        registerMock(SecurityInfoManager.class, mockSecurityInfoManager);
        createAndRegisterMock(ca.openosp.openo.hospitalReportManager.dao.HRMDocumentDao.class);
        createAndRegisterMock(ca.openosp.openo.hospitalReportManager.dao.HRMDocumentToDemographicDao.class);
        createAndRegisterMock(ca.openosp.openo.hospitalReportManager.dao.HRMDocumentToProviderDao.class);
        createAndRegisterMock(ca.openosp.openo.hospitalReportManager.dao.HRMSubClassDao.class);
        createAndRegisterMock(ca.openosp.openo.hospitalReportManager.dao.HRMDocumentSubClassDao.class);
        createAndRegisterMock(ca.openosp.openo.hospitalReportManager.dao.HRMCategoryDao.class);
        createAndRegisterMock(ca.openosp.openo.commn.dao.IncomingLabRulesDao.class);
        createAndRegisterMock(ca.openosp.openo.managers.NioFileManager.class);

        // Security manager grants the "_tickler" read privilege by default
        lenient().when(mockSecurityInfoManager.hasPrivilege(any(), anyString(), anyString(), anyInt()))
                .thenReturn(true);
        // The "_form" check passes a null demographic, so stub the String overload as well
        lenient().when(mockSecurityInfoManager.hasPrivilege(any(), anyString(), anyString(), nullable(String.class)))
                .thenReturn(true);

        manager = new DocumentAttachmentManagerImpl();

        injectDependency(manager, "ticklerDocsDao", mockTicklerDocsDao);
        injectDependency(manager, "documentDao", mockDocumentDao);
        injectDependency(manager, "eFormDataDao", mockEFormDataDao);
        injectDependency(manager, "formsManager", mockFormsManager);
        injectDependency(manager, "securityInfoManager", mockSecurityInfoManager);
    }

    @Nested
    @DisplayName("Security")
    class Security {

        @Test
        @DisplayName("should throw exception when user lacks tickler read privilege")
        void shouldThrowException_whenUserLacksTicklerReadPrivilege() {
            // Given
            when(mockSecurityInfoManager.hasPrivilege(any(), anyString(), anyString(), anyInt()))
                    .thenReturn(false);

            // When / Then
            assertThatThrownBy(() -> manager.getTicklerAttachmentDetails(mockLoggedInInfo, TICKLER_ID, DEMOGRAPHIC_NO))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("_tickler");
            verifyNoInteractions(mockTicklerDocsDao);
        }
    }

    @Nested
    @DisplayName("Display Name Resolution")
    class DisplayNameResolution {

        @Test
        @DisplayName("should return empty list when tickler has no attachments")
        void shouldReturnEmptyList_whenTicklerHasNoAttachments() {
            // Given
            when(mockTicklerDocsDao.findByTicklerId(TICKLER_ID)).thenReturn(Collections.emptyList());

            // When
            List<TicklerAttachmentData> details = manager.getTicklerAttachmentDetails(mockLoggedInInfo, TICKLER_ID, DEMOGRAPHIC_NO);

            // Then
            assertThat(details).isEmpty();
        }

        @Test
        @DisplayName("should resolve document description when a document is attached")
        void shouldResolveDocumentDescription_whenDocAttached() {
            // Given
            when(mockTicklerDocsDao.findByTicklerId(TICKLER_ID)).thenReturn(Collections.singletonList(
                    new TicklerDocs(TICKLER_ID, 12, TicklerDocs.DOCTYPE_DOC, PROVIDER_NO)));
            Document document = new Document();
            document.setDocdesc("Discharge Summary");
            when(mockDocumentDao.getDocument("12")).thenReturn(document);

            // When
            List<TicklerAttachmentData> details = manager.getTicklerAttachmentDetails(mockLoggedInInfo, TICKLER_ID, DEMOGRAPHIC_NO);

            // Then
            assertThat(details).hasSize(1);
            assertThat(details.get(0).getDocumentType()).isEqualTo(DocumentType.DOC);
            assertThat(details.get(0).getDocumentId()).isEqualTo("12");
            assertThat(details.get(0).getDisplayName()).isEqualTo("Discharge Summary");
        }

        @Test
        @DisplayName("should resolve eForm name when an eForm is attached")
        void shouldResolveEFormName_whenEFormAttached() {
            // Given
            when(mockTicklerDocsDao.findByTicklerId(TICKLER_ID)).thenReturn(Collections.singletonList(
                    new TicklerDocs(TICKLER_ID, 33, TicklerDocs.DOCTYPE_EFORM, PROVIDER_NO)));
            EFormData eForm = new EFormData();
            eForm.setFormName("Diabetes Care Plan");
            when(mockEFormDataDao.find(33)).thenReturn(eForm);

            // When
            List<TicklerAttachmentData> details = manager.getTicklerAttachmentDetails(mockLoggedInInfo, TICKLER_ID, DEMOGRAPHIC_NO);

            // Then
            assertThat(details).hasSize(1);
            assertThat(details.get(0).getDocumentType()).isEqualTo(DocumentType.EFORM);
            assertThat(details.get(0).getDisplayName()).isEqualTo("Diabetes Care Plan");
        }

        @Test
        @DisplayName("should resolve encounter form name when a form is attached")
        void shouldResolveFormName_whenEncounterFormAttached() {
            // Given
            when(mockTicklerDocsDao.findByTicklerId(TICKLER_ID)).thenReturn(Collections.singletonList(
                    new TicklerDocs(TICKLER_ID, 55, TicklerDocs.DOCTYPE_FORM, PROVIDER_NO)));
            EctFormData.PatientForm patientForm = mock(EctFormData.PatientForm.class);
            when(patientForm.getFormId()).thenReturn("55");
            when(patientForm.getFormName()).thenReturn("Rourke Baby Record");
            when(mockFormsManager.getEncounterFormsByDemographicNumbers(eq(mockLoggedInInfo), anyCollection(), eq(true), eq(true)))
                    .thenReturn(Map.of(DEMOGRAPHIC_NO, Collections.singletonList(patientForm)));

            // When
            List<TicklerAttachmentData> details = manager.getTicklerAttachmentDetails(mockLoggedInInfo, TICKLER_ID, DEMOGRAPHIC_NO);

            // Then
            assertThat(details).hasSize(1);
            assertThat(details.get(0).getDocumentType()).isEqualTo(DocumentType.FORM);
            assertThat(details.get(0).getDisplayName()).isEqualTo("Rourke Baby Record");
        }

        @Test
        @DisplayName("should resolve HRM report name when an HRM document is attached")
        void shouldResolveHrmName_whenHrmAttached() {
            // Given
            when(mockTicklerDocsDao.findByTicklerId(TICKLER_ID)).thenReturn(Collections.singletonList(
                    new TicklerDocs(TICKLER_ID, 7, TicklerDocs.DOCTYPE_HRM, PROVIDER_NO)));
            HashMap<String, Object> hrmDocument = new HashMap<>();
            hrmDocument.put("id", 7);
            hrmDocument.put("name", "Cardiology Consult Report");
            ArrayList<HashMap<String, ? extends Object>> allHrmDocuments = new ArrayList<>();
            allHrmDocuments.add(hrmDocument);

            try (MockedStatic<HRMUtil> hrmUtilMock = mockStatic(HRMUtil.class)) {
                hrmUtilMock.when(() -> HRMUtil.listHRMDocuments(any(), anyString(), anyBoolean(), anyString(), anyBoolean()))
                        .thenReturn(allHrmDocuments);

                // When
                List<TicklerAttachmentData> details = manager.getTicklerAttachmentDetails(mockLoggedInInfo, TICKLER_ID, DEMOGRAPHIC_NO);

                // Then
                assertThat(details).hasSize(1);
                assertThat(details.get(0).getDocumentType()).isEqualTo(DocumentType.HRM);
                assertThat(details.get(0).getDisplayName()).isEqualTo("Cardiology Consult Report");
            }
        }

        @Test
        @DisplayName("should fall back to type and id when the referenced document is missing")
        void shouldFallBackToTypeAndId_whenDocumentMissing() {
            // Given
            when(mockTicklerDocsDao.findByTicklerId(TICKLER_ID)).thenReturn(Collections.singletonList(
                    new TicklerDocs(TICKLER_ID, 12, TicklerDocs.DOCTYPE_DOC, PROVIDER_NO)));
            when(mockDocumentDao.getDocument("12")).thenReturn(null);

            // When
            List<TicklerAttachmentData> details = manager.getTicklerAttachmentDetails(mockLoggedInInfo, TICKLER_ID, DEMOGRAPHIC_NO);

            // Then
            assertThat(details).hasSize(1);
            assertThat(details.get(0).getDisplayName()).isEqualTo("doc #12");
        }

        @Test
        @DisplayName("should skip attachments with an unknown doctype")
        void shouldSkipAttachment_whenDocTypeUnknown() {
            // Given
            when(mockTicklerDocsDao.findByTicklerId(TICKLER_ID)).thenReturn(Collections.singletonList(
                    new TicklerDocs(TICKLER_ID, 99, "X", PROVIDER_NO)));

            // When
            List<TicklerAttachmentData> details = manager.getTicklerAttachmentDetails(mockLoggedInInfo, TICKLER_ID, DEMOGRAPHIC_NO);

            // Then
            assertThat(details).isEmpty();
        }
    }

    /**
     * Covers the shared form-name recovery that makes attached encounter forms addressable.
     * Attachment tables store only {@code (document_no, doctype)}, which cannot identify a form.
     */
    @Nested
    @DisplayName("Form Name Recovery")
    class FormNameRecovery {

        @Test
        @DisplayName("should map form ids to form names for the patient")
        void shouldMapFormIdsToNames_whenPatientHasForms() {
            // Given
            List<EctFormData.PatientForm> forms =
                    List.of(patientForm("7", "Annual"), patientForm("9", "Rourke2020"));
            when(mockFormsManager.getEncounterFormsByDemographicNumbers(eq(mockLoggedInInfo), anyCollection(), eq(true), eq(true)))
                    .thenReturn(Map.of(DEMOGRAPHIC_NO, forms));

            // When
            Map<String, String> formNames = manager.getFormNamesByFormId(mockLoggedInInfo, DEMOGRAPHIC_NO);

            // Then
            assertThat(formNames).containsExactlyInAnyOrderEntriesOf(
                    Map.of("7", "Annual", "9", "Rourke2020"));
        }

        @Test
        @DisplayName("should request every version so a superseded form still resolves")
        void shouldRequestAllVersions_whenResolvingFormNames() {
            // Given
            when(mockFormsManager.getEncounterFormsByDemographicNumbers(any(), anyCollection(), anyBoolean(), anyBoolean()))
                    .thenReturn(Collections.emptyMap());

            // When
            manager.getFormNamesByFormId(mockLoggedInInfo, DEMOGRAPHIC_NO);

            // Then
            verify(mockFormsManager).getEncounterFormsByDemographicNumbers(
                    eq(mockLoggedInInfo), anyCollection(), eq(true), eq(true));
        }

        @Test
        @DisplayName("should omit a form id shared by two form types rather than guess")
        void shouldOmitFormId_whenTwoFormTypesShareIt() {
            // Given
            List<EctFormData.PatientForm> forms =
                    List.of(patientForm("7", "Annual"), patientForm("7", "Rourke2020"),
                            patientForm("9", "Annual"));
            when(mockFormsManager.getEncounterFormsByDemographicNumbers(eq(mockLoggedInInfo), anyCollection(), eq(true), eq(true)))
                    .thenReturn(Map.of(DEMOGRAPHIC_NO, forms));

            // When
            Map<String, String> formNames = manager.getFormNamesByFormId(mockLoggedInInfo, DEMOGRAPHIC_NO);

            // Then
            assertThat(formNames).containsExactlyInAnyOrderEntriesOf(Map.of("9", "Annual"));
        }

        @Test
        @DisplayName("should return empty map when user lacks form read privilege")
        void shouldReturnEmptyMap_whenFormPrivilegeDenied() {
            // Given
            when(mockSecurityInfoManager.hasPrivilege(any(), eq("_form"), anyString(), nullable(String.class)))
                    .thenReturn(false);

            // When
            Map<String, String> formNames = manager.getFormNamesByFormId(mockLoggedInInfo, DEMOGRAPHIC_NO);

            // Then
            assertThat(formNames).isEmpty();
            verifyNoInteractions(mockFormsManager);
        }

        @Test
        @DisplayName("should return empty map when demographic is null")
        void shouldReturnEmptyMap_whenDemographicIsNull() {
            // When
            Map<String, String> formNames = manager.getFormNamesByFormId(mockLoggedInInfo, null);

            // Then
            assertThat(formNames).isEmpty();
            verifyNoInteractions(mockFormsManager);
        }

        private EctFormData.PatientForm patientForm(String formId, String formName) {
            EctFormData.PatientForm form = mock(EctFormData.PatientForm.class);
            when(form.getFormId()).thenReturn(formId);
            when(form.getFormName()).thenReturn(formName);
            return form;
        }
    }

    /**
     * Helper method to inject dependencies into the manager using reflection.
     */
    private void injectDependency(Object target, String fieldName, Object dependency) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, dependency);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject " + fieldName, e);
        }
    }
}
