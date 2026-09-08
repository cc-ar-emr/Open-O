package ca.openosp.openo.commn.dao;

import ca.openosp.openo.commn.model.LlmDocumentText;
import java.util.List;
import java.util.Optional;

/**
 * Data access for pre-extracted PDF text used in LLM patient exports.
 */
public interface LlmDocumentTextDao {

    /**
     * Find cached extraction for a specific document.
     * Returns empty if not yet extracted.
     */
    Optional<LlmDocumentText> findByDocumentNo(Integer documentNo);

    /**
     * Find all cached extractions for a patient.
     * Used to bulk-load all document texts for a demographic in one query
     * rather than N individual lookups.
     */
    List<LlmDocumentText> findByDemographicNo(Integer demographicNo);

    /**
     * Persist a new extraction record or update an existing one.
     * Callers should use this for both insert and re-extraction on hash mismatch.
     */
    LlmDocumentText save(LlmDocumentText entity);

    /**
     * Remove a cached record — used when the source document is deleted.
     */
    void deleteByDocumentNo(Integer documentNo);
}