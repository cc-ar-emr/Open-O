package ca.openosp.openo.integration.oneId;

import ca.openosp.openo.commn.dao.AbstractDao;

public interface OneIdSessionDao extends AbstractDao<OneIdSession> {

    /**
     * Clears the authority on a provider's session, but only while it is still the one they are on
     * and no active assignment still grants it.
     *
     * <p>The database tests both conditions inside the update itself; nothing is read back into
     * Java and checked there. That is the point of it. A check made here and a write made after it
     * leave a gap the provider can fill by picking another authority, and another administrator can
     * fill by adding back an assignment carrying the same value. Either would leave this clearing
     * something it should not have.
     *
     * @param providerNo String the provider whose session is being cleared
     * @param uaoValue String the withdrawn authority
     * @return int how many rows were cleared: 1 when it was cleared, 0 when it was not
     */
    int clearSessionUaoIfWithdrawn(String providerNo, String uaoValue);
}
