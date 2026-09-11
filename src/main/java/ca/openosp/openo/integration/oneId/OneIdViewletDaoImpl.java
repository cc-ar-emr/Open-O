package ca.openosp.openo.integration.oneId;

import ca.openosp.openo.commn.dao.AbstractDao;
import ca.openosp.openo.commn.dao.AbstractDaoImpl;
import ca.openosp.openo.commn.model.Clinic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.List;

@Repository
public class OneIdViewletDaoImpl extends AbstractDaoImpl<OneIdViewlet>  implements OneIdViewletDao{
    private static final Logger log = LoggerFactory.getLogger(OneIdViewletDao.class);
    public OneIdViewletDaoImpl() {
        super(OneIdViewlet.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public OneIdViewlet queryOneIdViewletForKey(final String key) {
        Query query = entityManager.createQuery(
                "SELECT o FROM OneIdViewlet o WHERE o.keyValue = ?1 ORDER BY o.deleted ASC, o.id ASC");
        query.setParameter(1, key);
        query.setMaxResults(1);
        List<OneIdViewlet> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<OneIdViewlet> findAllOrderByName() {
        Query query = entityManager.createQuery("SELECT o FROM OneIdViewlet o ORDER BY o.name");
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<OneIdViewlet> findAllActiveAndShowInEchartTrue() {
        Query query = entityManager.createQuery(
                "SELECT o FROM OneIdViewlet o WHERE o.deleted = false AND o.showInEchart = true");
        try {
            return query.getResultList();
        } catch (NoResultException nre) {
            log.debug("No active viewlets to display in the eChart | " + nre);
            return null;
        }
    }
}
