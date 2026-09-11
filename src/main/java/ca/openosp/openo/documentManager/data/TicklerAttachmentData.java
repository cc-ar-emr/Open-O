//CHECKSTYLE:OFF
package ca.openosp.openo.documentManager.data;

import ca.openosp.openo.commn.model.enumerator.DocumentType;

/**
 * A tickler attachment's type, id and human-readable display name.
 *
 * @since 2026-07-19
 */
public class TicklerAttachmentData {
    private DocumentType documentType;
    private String documentId;
    private String displayName;

    public TicklerAttachmentData() {
    }

    public TicklerAttachmentData(DocumentType documentType, String documentId, String displayName) {
        this.documentType = documentType;
        this.documentId = documentId;
        this.displayName = displayName;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
