-- Add LLM document text cache table for AI patient export feature
CREATE TABLE IF NOT EXISTS llm_document_text (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    document_no     INT(6) NOT NULL,
    demographic_no  INT NOT NULL,
    extracted_text  MEDIUMTEXT,
    content_hash    VARCHAR(64) NOT NULL,
    extracted_at    DATETIME NOT NULL,
    INDEX idx_document_no   (document_no),
    INDEX idx_demographic_no (demographic_no),
    FOREIGN KEY (document_no)   REFERENCES ctl_document(document_no),
    FOREIGN KEY (demographic_no) REFERENCES demographic(demographic_no)
);
