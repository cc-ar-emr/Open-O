package ca.openosp.openo.commn.model;

import javax.persistence.*;
import java.util.Date;

/**
 * Stores pre-extracted plain text from patient PDF documents for LLM ingestion.
 *
 * Extraction is performed once and cached here. Re-extraction is triggered only
 * when the source PDF changes, detected via SHA-256 content hash comparison.
 *
 * Foreign keys:
 *   document_no    → ctl_document(document_no)
 *   demographic_no → demographic(demographic_no)
 */
@Entity
@Table(name = "llm_document_text", indexes = {
    @Index(name = "idx_ldt_document_no",    columnList = "document_no"),
    @Index(name = "idx_ldt_demographic_no", columnList = "demographic_no")
})
public class LlmDocumentText {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /** FK → ctl_document.document_no */
    @Column(name = "document_no", nullable = false)
    private Integer documentNo;

    /** FK → demographic.demographic_no — denormalised for fast per-patient queries */
    @Column(name = "demographic_no", nullable = false)
    private Integer demographicNo;

    /** Plain text extracted from the PDF. Null if extraction failed or PDF had no text layer. */
    @Column(name = "extracted_text", columnDefinition = "MEDIUMTEXT")
    private String extractedText;

    /**
     * SHA-256 hex digest of the raw PDF bytes at extraction time.
     * Used to detect whether the source document has changed since last extraction.
     */
    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "extracted_at", nullable = false)
    private Date extractedAt;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public LlmDocumentText() {}

    public LlmDocumentText(Integer documentNo, Integer demographicNo,
                           String extractedText, String contentHash) {
        this.documentNo    = documentNo;
        this.demographicNo = demographicNo;
        this.extractedText = extractedText;
        this.contentHash   = contentHash;
        this.extractedAt   = new Date();
    }

    // -------------------------------------------------------------------------
    // Getters / setters
    // -------------------------------------------------------------------------

    public Integer getId()                          { return id; }
    public void    setId(Integer id)                { this.id = id; }

    public Integer getDocumentNo()                  { return documentNo; }
    public void    setDocumentNo(Integer documentNo){ this.documentNo = documentNo; }

    public Integer getDemographicNo()                      { return demographicNo; }
    public void    setDemographicNo(Integer demographicNo) { this.demographicNo = demographicNo; }

    public String  getExtractedText()                      { return extractedText; }
    public void    setExtractedText(String extractedText)  { this.extractedText = extractedText; }

    public String  getContentHash()                        { return contentHash; }
    public void    setContentHash(String contentHash)      { this.contentHash = contentHash; }

    public Date    getExtractedAt()                        { return extractedAt; }
    public void    setExtractedAt(Date extractedAt)        { this.extractedAt = extractedAt; }
}