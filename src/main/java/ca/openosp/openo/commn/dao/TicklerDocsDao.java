//CHECKSTYLE:OFF

package ca.openosp.openo.commn.dao;

import java.util.List;

import ca.openosp.openo.commn.model.TicklerDocs;

/**
 * Data access for {@link TicklerDocs} (Tickler document attachments).
 *
 * <p>Tickler counterpart of {@code ConsultDocsDao}/{@code EFormDocsDao}. All finders exclude
 * soft-deleted rows ({@code deleted is NULL}).</p>
 *
 * @since 2026-06-12
 */
public interface TicklerDocsDao extends AbstractDao<TicklerDocs> {
    /**
     * Finds a specific non-deleted attachment for a tickler.
     *
     * @param ticklerId Integer the tickler's unique identifier
     * @param documentNo Integer the attached document/lab/eForm/HRM/form identifier
     * @param docType String the attachment type (see {@link TicklerDocs} DOCTYPE_* constants)
     * @return List&lt;TicklerDocs&gt; matching, non-deleted attachments (empty if none found)
     */
    List<TicklerDocs> findByTicklerIdDocNoDocType(Integer ticklerId, Integer documentNo, String docType);

    /**
     * Finds all non-deleted attachments of a given type for a tickler.
     *
     * @param ticklerId Integer the tickler's unique identifier
     * @param docType String the attachment type (see {@link TicklerDocs} DOCTYPE_* constants)
     * @return List&lt;TicklerDocs&gt; matching, non-deleted attachments (empty if none found)
     */
    List<TicklerDocs> findByTicklerIdDocType(Integer ticklerId, String docType);

    /**
     * Finds all non-deleted attachments for a tickler, regardless of type.
     *
     * @param ticklerId Integer the tickler's unique identifier
     * @return List&lt;TicklerDocs&gt; all non-deleted attachments for the tickler (empty if none found)
     */
    List<TicklerDocs> findByTicklerId(Integer ticklerId);

    /**
     * Batch-finds all non-deleted attachments for a set of ticklers in a single query, to avoid
     * per-row lookups when rendering a page of tickler list results.
     *
     * @param ticklerIds List&lt;Integer&gt; the tickler identifiers to fetch attachments for
     * @return List&lt;TicklerDocs&gt; all non-deleted attachments across the given ticklers (empty if none found)
     */
    List<TicklerDocs> findByTicklerIds(List<Integer> ticklerIds);
}
