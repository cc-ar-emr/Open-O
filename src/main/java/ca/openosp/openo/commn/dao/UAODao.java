//CHECKSTYLE:OFF

package ca.openosp.openo.commn.dao;

import ca.openosp.openo.commn.model.UAO;

import java.util.List;

/**
 * Data access for a provider's "Under Authority Of" (UAO) values.
 *
 * <p>A UAO names the Health Information Custodian a provider is acting for. A provider can hold
 * several and one of them is their default, which is the one a session starts under.
 *
 * @since 2026-07-02
 */
public interface UAODao extends AbstractDao<UAO> {

    /**
     * Returns a provider's active UAO values, the default first.
     *
     * @param providerNo String the provider number
     * @return List&lt;UAO&gt; the active UAO values, default-first
     */
    List<UAO> findByProvider(String providerNo);

    /**
     * Marks one of a provider's UAO values as the default and clears the flag on the others.
     *
     * @param uao UAO the value to make default
     * @param providerNo String the provider number
     */
    void setAsDefault(UAO uao, String providerNo);
}
