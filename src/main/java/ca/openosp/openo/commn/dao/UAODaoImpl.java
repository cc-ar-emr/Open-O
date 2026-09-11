//CHECKSTYLE:OFF

package ca.openosp.openo.commn.dao;

import ca.openosp.openo.commn.model.UAO;
import org.springframework.stereotype.Repository;

import javax.persistence.Query;
import java.util.List;

/**
 * JPA implementation of {@link UAODao}.
 *
 * <p>Method contracts are on the interface. Both queries are scoped to one provider, and only
 * active values are returned, so a value taken out of use cannot be listed or made default.
 *
 * @since 2026-07-02
 */
@Repository
public class UAODaoImpl extends AbstractDaoImpl<UAO> implements UAODao {

    public UAODaoImpl() {
        super(UAO.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<UAO> findByProvider(String providerNo) {
        Query query = entityManager.createQuery(
                "select u from UAO u where u.providerNo = :providerNo and u.active = true order by u.defaultUAO desc");
        query.setParameter("providerNo", providerNo);
        return (List<UAO>) query.getResultList();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Written as two statements rather than a row at a time from a list read beforehand. An
     * update reads the rows as they are now, so two changes to one provider's default queue behind
     * each other and the last one wins; working from a list read earlier let both proceed from the
     * same starting point and leave two rows flagged default.
     *
     * <p>A value that is not this provider's, or has been taken out of use, changes nothing at all.
     * Clearing first and discovering afterwards that there was nothing to set would leave the
     * provider with no default, so ownership decides whether either statement runs.
     */
    @Override
    public void setAsDefault(UAO uao, String providerNo) {
        if (!isOwnedAndActive(uao.getId(), providerNo)) {
            return;
        }

        Query clearOthers = entityManager.createQuery(
                "update UAO u set u.defaultUAO = false"
                        + " where u.providerNo = :providerNo and u.active = true and u.id <> :id");
        clearOthers.setParameter("providerNo", providerNo);
        clearOthers.setParameter("id", uao.getId());
        clearOthers.executeUpdate();

        Query setChosen = entityManager.createQuery(
                "update UAO u set u.defaultUAO = true"
                        + " where u.id = :id and u.providerNo = :providerNo and u.active = true");
        setChosen.setParameter("id", uao.getId());
        setChosen.setParameter("providerNo", providerNo);
        setChosen.executeUpdate();
    }

    /**
     * Whether a value is one this provider holds and still in use.
     *
     * <p>Asked as its own statement rather than folded into the updates, because MySQL will not
     * accept a subquery against the table an UPDATE is writing.
     *
     * @param id         Integer the value's id, which may be absent
     * @param providerNo String the provider the value must belong to
     * @return boolean true when the value is this provider's and active
     */
    private boolean isOwnedAndActive(Integer id, String providerNo) {
        if (id == null || providerNo == null) {
            return false;
        }
        Query query = entityManager.createQuery(
                "select count(u) from UAO u"
                        + " where u.id = :id and u.providerNo = :providerNo and u.active = true");
        query.setParameter("id", id);
        query.setParameter("providerNo", providerNo);
        return ((Number) query.getSingleResult()).longValue() > 0;
    }
}
