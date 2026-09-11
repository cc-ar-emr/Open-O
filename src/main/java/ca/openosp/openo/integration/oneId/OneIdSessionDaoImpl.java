package ca.openosp.openo.integration.oneId;

import ca.openosp.openo.commn.dao.AbstractDaoImpl;
import org.springframework.stereotype.Repository;

import javax.persistence.Query;

@Repository
public class OneIdSessionDaoImpl extends AbstractDaoImpl<OneIdSession> implements OneIdSessionDao{
    public OneIdSessionDaoImpl() {
        super(OneIdSession.class);
    }

    @Override
    public int clearSessionUaoIfWithdrawn(String providerNo, String uaoValue) {
        Query query = entityManager.createQuery(
                "update OneIdSession s set s.uaoUpi = null, s.uaoName = null"
                        + " where s.providerNo = :providerNo and s.uaoUpi = :uaoValue"
                        + " and not exists (select u.id from UAO u where u.providerNo = :providerNo"
                        + " and u.name = :uaoValue and u.active = true)");
        query.setParameter("providerNo", providerNo);
        query.setParameter("uaoValue", uaoValue);
        return query.executeUpdate();
    }
}
