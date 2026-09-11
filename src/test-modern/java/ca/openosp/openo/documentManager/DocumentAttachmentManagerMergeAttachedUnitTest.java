package ca.openosp.openo.documentManager;

import ca.openosp.openo.commn.model.EFormData;
import ca.openosp.openo.documentManager.data.AttachmentSections;
import ca.openosp.openo.utility.LoggedInInfo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DocumentAttachmentManagerImpl#mergeAttachedIntoSections}
 * — the classification function that decides which section (patient / provider
 * public / provider private / eForms) an attached item lands in when the attachment
 * manager reopens a saved consult or eForm. Docs are exercised one at a time via a
 * single-element attached list; no Spring context.
 *
 * Covers the full matrix of (module type, public flag, ownership, deleted state)
 * combinations that drive the UI's section placement and "(other provider)" label,
 * plus deleted/current attached eForms.
 */
@DisplayName("DocumentAttachmentManagerImpl.mergeAttachedIntoSections")
@Tag("unit")
@Tag("document")
class DocumentAttachmentManagerMergeAttachedUnitTest {

    private static final String CURRENT_PROVIDER = "42";
    private static final String OTHER_PROVIDER = "99";

    private final DocumentAttachmentManagerImpl manager = new DocumentAttachmentManagerImpl();

    private LoggedInInfo loggedInInfo;
    private AttachmentSections sections;
    private List<EDoc> allDocuments;
    private List<EDoc> providerPrivateDocs;
    private List<EDoc> providerPublicDocs;
    private Set<String> attachedDocumentIds;
    private Set<String> foreignPrivateDocIds;

    @BeforeEach
    void resetSinks() {
        loggedInInfo = mock(LoggedInInfo.class);
        when(loggedInInfo.getLoggedInProviderNo()).thenReturn(CURRENT_PROVIDER);
        useSections(new AttachmentSections(null, null, null, null));
    }

    @Nested
    @DisplayName("patient docs (module = demographic)")
    class PatientDocs {

        @Test
        @DisplayName("active patient doc already listed in allDocuments is not added again")
        void shouldNotDuplicate_whenActivePatientDocAlreadyListed() {
            EDoc doc = patientDoc("1", 'A');
            useSections(new AttachmentSections(Collections.singletonList(doc), null, null, null));
            merge(patientDoc("1", 'A'));
            assertThat(allDocuments).containsExactly(doc);
            assertThat(providerPrivateDocs).isEmpty();
            assertThat(providerPublicDocs).isEmpty();
        }

        @Test
        @DisplayName("active patient doc missing from allDocuments (e.g. moved to another patient) is added so it can be detached")
        void shouldAdd_whenActivePatientDocNotListed() {
            EDoc doc = patientDoc("12", 'A');
            merge(doc);
            assertThat(allDocuments).containsExactly(doc);
            assertThat(foreignPrivateDocIds).isEmpty();
        }

        @Test
        @DisplayName("deleted patient doc is re-injected into allDocuments so the section still renders it")
        void shouldReInject_whenDeletedPatientDoc() {
            EDoc doc = patientDoc("2", 'D');
            merge(doc);
            assertThat(allDocuments).containsExactly(doc);
            assertThat(providerPrivateDocs).isEmpty();
            assertThat(providerPublicDocs).isEmpty();
            assertThat(foreignPrivateDocIds).isEmpty();
        }
    }

    @Nested
    @DisplayName("public provider docs (module = providers, docPublic = 1)")
    class PublicProviderDocs {

        @Test
        @DisplayName("active public provider doc already listed in providerPublicDocs is not added again")
        void shouldNotDuplicate_whenActivePublicProviderDocAlreadyListed() {
            EDoc doc = providerDoc("3", 'A', true, OTHER_PROVIDER);
            useSections(new AttachmentSections(null, null, Collections.singletonList(doc), null));
            merge(providerDoc("3", 'A', true, OTHER_PROVIDER));
            assertThat(providerPublicDocs).containsExactly(doc);
            assertThat(allDocuments).isEmpty();
            assertThat(providerPrivateDocs).isEmpty();
            assertThat(foreignPrivateDocIds).isEmpty();
        }

        @Test
        @DisplayName("deleted public provider doc is re-injected into providerPublicDocs")
        void shouldReInject_whenDeletedPublicProviderDoc() {
            EDoc doc = providerDoc("4", 'D', true, OTHER_PROVIDER);
            merge(doc);
            assertThat(providerPublicDocs).containsExactly(doc);
            assertThat(allDocuments).isEmpty();
            assertThat(providerPrivateDocs).isEmpty();
            assertThat(foreignPrivateDocIds).isEmpty();
        }
    }

    @Nested
    @DisplayName("private provider docs owned by the current provider")
    class OwnPrivateProviderDocs {

        @Test
        @DisplayName("active own private doc already listed in providerPrivateDocs is not added again")
        void shouldNotDuplicate_whenActiveOwnPrivateDocAlreadyListed() {
            EDoc doc = providerDoc("5", 'A', false, CURRENT_PROVIDER);
            useSections(new AttachmentSections(null, Collections.singletonList(doc), null, null));
            merge(providerDoc("5", 'A', false, CURRENT_PROVIDER));
            assertThat(providerPrivateDocs).containsExactly(doc);
            assertThat(allDocuments).isEmpty();
            assertThat(providerPublicDocs).isEmpty();
            assertThat(foreignPrivateDocIds).isEmpty();
        }

        @Test
        @DisplayName("deleted own private doc is re-injected into providerPrivateDocs (not marked foreign)")
        void shouldReInject_whenDeletedOwnPrivateDoc() {
            EDoc doc = providerDoc("6", 'D', false, CURRENT_PROVIDER);
            merge(doc);
            assertThat(providerPrivateDocs).containsExactly(doc);
            assertThat(foreignPrivateDocIds)
                    .as("doc belongs to current provider, should not be flagged foreign")
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("private provider docs owned by a different provider (cross-provider)")
    class ForeignPrivateProviderDocs {

        @Test
        @DisplayName("active foreign private doc is merged into providerPrivateDocs AND flagged foreign")
        void shouldMergeAndFlag_whenActiveForeignPrivateDoc() {
            EDoc doc = providerDoc("7", 'A', false, OTHER_PROVIDER);
            merge(doc);
            assertThat(providerPrivateDocs).containsExactly(doc);
            assertThat(foreignPrivateDocIds).containsExactly("7");
            assertThat(allDocuments).isEmpty();
            assertThat(providerPublicDocs).isEmpty();
        }

        @Test
        @DisplayName("deleted foreign private doc is merged into providerPrivateDocs AND flagged foreign")
        void shouldMergeAndFlag_whenDeletedForeignPrivateDoc() {
            EDoc doc = providerDoc("8", 'D', false, OTHER_PROVIDER);
            merge(doc);
            assertThat(providerPrivateDocs).containsExactly(doc);
            assertThat(foreignPrivateDocIds).containsExactly("8");
        }
    }

    @Nested
    @DisplayName("edge cases")
    class EdgeCases {

        @Test
        @DisplayName("no current provider means foreign private docs aren't misclassified as owned")
        void shouldFlagForeign_whenCurrentProviderIsNull() {
            when(loggedInInfo.getLoggedInProviderNo()).thenReturn(null);
            EDoc doc = providerDoc("9", 'A', false, OTHER_PROVIDER);
            merge(doc);
            assertThat(providerPrivateDocs).containsExactly(doc);
            assertThat(foreignPrivateDocIds).containsExactly("9");
        }

        @Test
        @DisplayName("legacy 'provider' module spelling is treated the same as 'providers'")
        void shouldTreatProviderAndProvidersIdentically() {
            EDoc legacy = new EDoc();
            legacy.setDocId("10");
            legacy.setStatus('A');
            legacy.setModule("provider");
            legacy.setModuleId(OTHER_PROVIDER);
            legacy.setDocPublic("0");

            merge(legacy);

            assertThat(providerPrivateDocs).containsExactly(legacy);
            assertThat(foreignPrivateDocIds).containsExactly("10");
        }

        @Test
        @DisplayName("every attached doc's id is collected into attachedDocumentIds regardless of classification")
        void shouldCollectId_whenDocIsAttached() {
            merge(patientDoc("11", 'A'));
            assertThat(attachedDocumentIds).containsExactly("11");
        }

        @Test
        @DisplayName("empty attached list is a no-op across all section lists and id sets")
        void shouldBeNoOp_whenAttachedListIsEmpty() {
            manager.mergeAttachedIntoSections(loggedInInfo, Collections.emptyList(), Collections.emptyList(), sections);
            assertSectionListsEmpty();
            assertThat(attachedDocumentIds).isEmpty();
            assertThat(sections.getEForms().getItems()).isEmpty();
            assertThat(sections.getAttachedEFormIds()).isEmpty();
        }

        @Test
        @DisplayName("null attached lists are a no-op")
        void shouldBeNoOp_whenAttachedListsNull() {
            manager.mergeAttachedIntoSections(loggedInInfo, null, null, sections);
            assertSectionListsEmpty();
            assertThat(attachedDocumentIds).isEmpty();
            assertThat(sections.getEForms().getItems()).isEmpty();
        }

        @Test
        @DisplayName("the id sets handed to the view are read-only")
        void shouldRejectChanges_whenIdSetsModifiedByCaller() {
            merge(providerDoc("13", 'A', false, OTHER_PROVIDER));
            assertThatThrownBy(() -> sections.getAttachedDocumentIds().add("99")).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> sections.getForeignPrivateDocIds().clear()).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> sections.getAttachedEFormIds().add(99)).isInstanceOf(UnsupportedOperationException.class);
            assertThat(attachedDocumentIds).containsExactly("13");
            assertThat(foreignPrivateDocIds).containsExactly("13");
        }
    }

    @Nested
    @DisplayName("eForms")
    class EForms {

        @Test
        @DisplayName("current attached eForm is already listed — not added again, but its id is collected")
        void shouldNotDuplicate_whenAttachedEFormCurrent() {
            EFormData current = eForm(1, true);
            AttachmentSections withEForms = new AttachmentSections(null, null, null, Collections.singletonList(current));

            manager.mergeAttachedIntoSections(loggedInInfo, null, Collections.singletonList(eForm(1, true)), withEForms);

            assertThat(withEForms.getEForms().getItems()).containsExactly(current);
            assertThat(withEForms.getAttachedEFormIds()).containsExactly(1);
        }

        @Test
        @DisplayName("deleted attached eForm is appended after the current eForms so it can be detached")
        void shouldAppendDeletedEForm_whenAttachedEFormDeleted() {
            EFormData current = eForm(1, true);
            EFormData deleted = eForm(2, false);
            AttachmentSections withEForms = new AttachmentSections(null, null, null, Collections.singletonList(current));

            manager.mergeAttachedIntoSections(loggedInInfo, null, Arrays.asList(current, deleted), withEForms);

            assertThat(withEForms.getEForms().getItems()).containsExactly(current, deleted);
            assertThat(withEForms.getAttachedEFormIds()).containsExactlyInAnyOrder(1, 2);
        }

        @Test
        @DisplayName("eForm attached twice is only added once")
        void shouldAddOnce_whenEFormAttachedTwice() {
            manager.mergeAttachedIntoSections(loggedInInfo, null, Arrays.asList(eForm(3, false), eForm(3, false)), sections);

            assertThat(sections.getEForms().getItems()).extracting(EFormData::getId).containsExactly(3);
        }

        @Test
        @DisplayName("several deleted eForms keep their attached order")
        void shouldKeepAttachedOrder_whenSeveralDeletedEForms() {
            manager.mergeAttachedIntoSections(loggedInInfo, null, Arrays.asList(eForm(5, false), eForm(4, false)), sections);

            assertThat(sections.getEForms().getItems()).extracting(EFormData::getId).containsExactly(5, 4);
        }

        @Test
        @DisplayName("attached eForms don't touch the doc sections")
        void shouldLeaveDocSectionsAlone_whenOnlyEFormsAttached() {
            manager.mergeAttachedIntoSections(loggedInInfo, null, Collections.singletonList(eForm(6, false)), sections);

            assertSectionListsEmpty();
            assertThat(attachedDocumentIds).isEmpty();
        }
    }

    // --- helpers ---------------------------------------------------------

    private void useSections(AttachmentSections newSections) {
        sections = newSections;
        allDocuments = sections.getPatientDocuments().getItems();
        providerPrivateDocs = sections.getProviderPrivateDocuments().getItems();
        providerPublicDocs = sections.getProviderPublicDocuments().getItems();
        attachedDocumentIds = sections.getAttachedDocumentIds();
        foreignPrivateDocIds = sections.getForeignPrivateDocIds();
    }

    private void merge(EDoc doc) {
        manager.mergeAttachedIntoSections(loggedInInfo, Collections.singletonList(doc), null, sections);
    }

    private void assertSectionListsEmpty() {
        assertThat(allDocuments).isEmpty();
        assertThat(providerPrivateDocs).isEmpty();
        assertThat(providerPublicDocs).isEmpty();
        assertThat(foreignPrivateDocIds).isEmpty();
    }

    private static EDoc patientDoc(String docId, char status) {
        EDoc d = new EDoc();
        d.setDocId(docId);
        d.setStatus(status);
        d.setModule("demographic");
        d.setModuleId("2001");
        d.setDocPublic("0");
        return d;
    }

    private static EFormData eForm(int fdid, boolean current) {
        EFormData eForm = new EFormData();
        eForm.setId(fdid);
        eForm.setCurrent(current);
        return eForm;
    }

    private static EDoc providerDoc(String docId, char status, boolean isPublic, String ownerProviderNo) {
        EDoc d = new EDoc();
        d.setDocId(docId);
        d.setStatus(status);
        d.setModule("providers");
        d.setModuleId(ownerProviderNo);
        d.setDocPublic(isPublic ? "1" : "0");
        return d;
    }
}
