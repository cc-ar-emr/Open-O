package ca.openosp.openo.commn.dao;

import ca.openosp.openo.commn.model.LlmDocumentText;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class LlmDocumentTextDaoImpl implements LlmDocumentTextDao {

    @PersistenceContext(unitName = "persistenceUnit")
    private EntityManager entityManager;

    @Override
    public Optional<LlmDocumentText> findByDocumentNo(Integer documentNo) {
        TypedQuery<LlmDocumentText> q = entityManager.createQuery(
            "SELECT l FROM LlmDocumentText l WHERE l.documentNo = :docNo",
            LlmDocumentText.class);
        q.setParameter("docNo", documentNo);
        List<LlmDocumentText> results = q.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<LlmDocumentText> findByDemographicNo(Integer demographicNo) {
        TypedQuery<LlmDocumentText> q = entityManager.createQuery(
            "SELECT l FROM LlmDocumentText l WHERE l.demographicNo = :demoNo",
            LlmDocumentText.class);
        q.setParameter("demoNo", demographicNo);
        return q.getResultList();
    }

    @Override
    public LlmDocumentText save(LlmDocumentText entity) {
        if (entity.getId() == null) {
            entityManager.persist(entity);
            return entity;
        }
        return entityManager.merge(entity);
    }

    @Override
    public void deleteByDocumentNo(Integer documentNo) {
        entityManager.createQuery(
            "DELETE FROM LlmDocumentText l WHERE l.documentNo = :docNo")
            .setParameter("docNo", documentNo)
            .executeUpdate();
    }
}