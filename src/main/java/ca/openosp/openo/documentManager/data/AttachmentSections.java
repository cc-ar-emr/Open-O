package ca.openosp.openo.documentManager.data;

import ca.openosp.openo.commn.model.EFormData;
import ca.openosp.openo.documentManager.EDoc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * The selectable sections of the attachment window (attachDocument.jsp) that need attached items
 * merged into them, plus the id sets the view uses to pre-check attached items and label
 * other providers' private docs.
 *
 * <p>Built from the base lists (active patient docs, the current provider's private docs, public
 * provider docs, current eForms), then filled in by
 * {@link ca.openosp.openo.documentManager.DocumentAttachmentManager#mergeAttachedIntoSections}
 * with any attached item its section doesn't already list.</p>
 *
 * @since 2026-09-10
 */
public class AttachmentSections {

    /**
     * One section of the attachment window: its items, and the ids already listed so an
     * attached item is only added when it's missing.
     *
     * @param <T> the item type (EDoc or EFormData)
     */
    public static final class Section<T> {
        private final List<T> items;
        private final Function<T, ?> idOf;
        private final Set<Object> listedIds = new HashSet<>();

        private Section(List<T> items, Function<T, ?> idOf) {
            this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
            this.idOf = idOf;
            for (T item : this.items) {
                listedIds.add(idOf.apply(item));
            }
        }

        /**
         * Appends the item unless an item with the same id is already listed.
         *
         * @param item T the item to add
         */
        public void addIfAbsent(T item) {
            if (listedIds.add(idOf.apply(item))) {
                items.add(item);
            }
        }

        /**
         * @return List&lt;T&gt; a read-only view of the section's items
         */
        public List<T> getItems() {
            return Collections.unmodifiableList(items);
        }
    }

    private final Section<EDoc> patientDocuments;
    private final Section<EDoc> providerPrivateDocuments;
    private final Section<EDoc> providerPublicDocuments;
    private final Section<EFormData> eForms;
    private final Set<String> attachedDocumentIds = new HashSet<>();
    private final Set<String> foreignPrivateDocIds = new HashSet<>();
    private final Set<Integer> attachedEFormIds = new HashSet<>();

    /**
     * Creates the sections from the base lists. Each list is copied, so unmodifiable lists are fine.
     *
     * @param patientDocuments         List&lt;EDoc&gt; the patient's active documents; may be null
     * @param providerPrivateDocuments List&lt;EDoc&gt; the current provider's active private eDocs; may be null
     * @param providerPublicDocuments  List&lt;EDoc&gt; active public provider eDocs; may be null
     * @param eForms                   List&lt;EFormData&gt; the patient's current eForms; may be null
     */
    public AttachmentSections(List<EDoc> patientDocuments, List<EDoc> providerPrivateDocuments, List<EDoc> providerPublicDocuments, List<EFormData> eForms) {
        this.patientDocuments = new Section<>(patientDocuments, EDoc::getDocId);
        this.providerPrivateDocuments = new Section<>(providerPrivateDocuments, EDoc::getDocId);
        this.providerPublicDocuments = new Section<>(providerPublicDocuments, EDoc::getDocId);
        this.eForms = new Section<>(eForms, EFormData::getId);
    }

    /**
     * Picks the section a doc belongs in: patient docs, or the provider public/private library.
     *
     * @param doc EDoc the doc to place
     * @return Section&lt;EDoc&gt; the doc's section
     */
    public Section<EDoc> sectionFor(EDoc doc) {
        if (!doc.isProviderScoped()) {
            return patientDocuments;
        }
        return doc.isPublicDoc() ? providerPublicDocuments : providerPrivateDocuments;
    }

    /**
     * @return Section&lt;EDoc&gt; the patient documents section
     */
    public Section<EDoc> getPatientDocuments() {
        return patientDocuments;
    }

    /**
     * @return Section&lt;EDoc&gt; the provider private eDocs section
     */
    public Section<EDoc> getProviderPrivateDocuments() {
        return providerPrivateDocuments;
    }

    /**
     * @return Section&lt;EDoc&gt; the provider public eDocs section
     */
    public Section<EDoc> getProviderPublicDocuments() {
        return providerPublicDocuments;
    }

    /**
     * @return Section&lt;EFormData&gt; the eForms section
     */
    public Section<EFormData> getEForms() {
        return eForms;
    }

    /**
     * Records a doc as attached, so the view pre-checks it.
     *
     * @param docId String the attached doc's id
     */
    public void recordAttachedDocument(String docId) {
        attachedDocumentIds.add(docId);
    }

    /**
     * Records an attached doc as another provider's private doc, so the view labels it.
     *
     * @param docId String the attached doc's id
     */
    public void recordForeignPrivateDoc(String docId) {
        foreignPrivateDocIds.add(docId);
    }

    /**
     * Records an eForm as attached, so the view pre-checks it.
     *
     * @param fdid Integer the attached eForm's fdid
     */
    public void recordAttachedEForm(Integer fdid) {
        attachedEFormIds.add(fdid);
    }

    /**
     * @return Set&lt;String&gt; a read-only view of the ids of every attached doc, used to pre-check them
     */
    public Set<String> getAttachedDocumentIds() {
        return Collections.unmodifiableSet(attachedDocumentIds);
    }

    /**
     * @return Set&lt;String&gt; a read-only view of the ids of attached private docs owned by another provider
     */
    public Set<String> getForeignPrivateDocIds() {
        return Collections.unmodifiableSet(foreignPrivateDocIds);
    }

    /**
     * @return Set&lt;Integer&gt; a read-only view of the fdids of every attached eForm, used to pre-check them
     */
    public Set<Integer> getAttachedEFormIds() {
        return Collections.unmodifiableSet(attachedEFormIds);
    }
}
