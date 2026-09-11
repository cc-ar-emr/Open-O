//CHECKSTYLE:OFF

package ca.openosp.openo.commn.dao;

import java.util.Collections;
import java.util.List;

import javax.persistence.Query;

import ca.openosp.openo.commn.model.TicklerDocs;
import org.springframework.stereotype.Repository;

/**
 * Default {@link TicklerDocsDao} implementation. Mirrors {@code ConsultDocsDaoImpl}; every query
 * filters out soft-deleted rows with {@code x.deleted is NULL}.
 *
 * @since 2026-06-12
 */
@Repository
@SuppressWarnings("unchecked")
public class TicklerDocsDaoImpl extends AbstractDaoImpl<TicklerDocs> implements TicklerDocsDao {

    public TicklerDocsDaoImpl() {
        super(TicklerDocs.class);
    }

    @Override
    public List<TicklerDocs> findByTicklerIdDocNoDocType(Integer ticklerId, Integer documentNo, String docType) {
        String sql = "select x from TicklerDocs x where x.ticklerId=?1 and x.documentNo=?2 and x.docType=?3 and x.deleted is NULL";
        Query query = entityManager.createQuery(sql);
        query.setParameter(1, ticklerId);
        query.setParameter(2, documentNo);
        query.setParameter(3, docType);

        List<TicklerDocs> results = query.getResultList();
        return results;
    }

    @Override
    public List<TicklerDocs> findByTicklerIdDocType(Integer ticklerId, String docType) {
        String sql = "select x from TicklerDocs x where x.ticklerId=?1 and x.docType=?2 and x.deleted is NULL";
        Query query = entityManager.createQuery(sql);
        query.setParameter(1, ticklerId);
        query.setParameter(2, docType);

        List<TicklerDocs> results = query.getResultList();
        return results;
    }

    @Override
    public List<TicklerDocs> findByTicklerId(Integer ticklerId) {
        String sql = "select x from TicklerDocs x where x.ticklerId=?1 and x.deleted is NULL";
        Query query = entityManager.createQuery(sql);
        query.setParameter(1, ticklerId);

        List<TicklerDocs> results = query.getResultList();
        return results;
    }

    @Override
    public List<TicklerDocs> findByTicklerIds(List<Integer> ticklerIds) {
        if (ticklerIds == null || ticklerIds.isEmpty()) {
            return Collections.emptyList();
        }
        String sql = "select x from TicklerDocs x where x.ticklerId in (:ticklerIds) and x.deleted is NULL";
        Query query = entityManager.createQuery(sql);
        query.setParameter("ticklerIds", ticklerIds);

        List<TicklerDocs> results = query.getResultList();
        return results;
    }
}
