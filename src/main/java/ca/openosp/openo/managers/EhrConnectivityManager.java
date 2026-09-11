/**
 * Copyright (c) 2001-2002. Department of Family Medicine, McMaster University. All Rights Reserved.
 * This software is published under the GPL GNU General Public License.
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * <p>
 * This software was written for the
 * Department of Family Medicine
 * McMaster University
 * Hamilton
 * Ontario, Canada
 */
package ca.openosp.openo.managers;

import ca.openosp.openo.commn.model.OMDGatewayTransactionLog;
import ca.openosp.openo.commn.model.Security;
import ca.openosp.openo.commn.model.SystemPreferences;
import ca.openosp.openo.commn.model.UAO;
import ca.openosp.openo.integration.oneId.OneIdSession;
import ca.openosp.openo.integration.oneId.OneIdViewlet;
import ca.openosp.openo.utility.LoggedInInfo;

import java.util.List;

/**
 * Shared service for the ONE ID / EHR connectivity feature. Provides the gateway configuration
 * (stored as {@code SystemPreferences} rows) and read access to the gateway transaction log, for use
 * by the connectivity admin and provider actions.
 *
 * @since 2026-07-01
 */
public interface EhrConnectivityManager {

    /**
     * Returns the stored configuration row for a gateway setting.
     *
     * @param key Enum the configuration key
     * @return SystemPreferences the stored row, or null when the setting has never been saved
     */
    SystemPreferences getConfig(Enum<?> key);

    /**
     * Returns the stored value for a gateway setting.
     *
     * @param key Enum the configuration key
     * @return String the stored value, or null when the setting has never been saved
     */
    String getConfigValue(Enum<?> key);

    /**
     * Returns the stored value for a gateway setting, or a default when it is missing or blank.
     *
     * @param key Enum the configuration key
     * @param defaultValue String the value to return when the setting is missing or blank
     * @return String the stored value, or defaultValue
     */
    String getConfigValue(Enum<?> key, String defaultValue);

    /**
     * Returns a boolean gateway setting, treating the literal "true" as true.
     *
     * @param key Enum the configuration key
     * @param defaultValue boolean the value to return when the setting is missing or blank
     * @return boolean the stored flag, or defaultValue
     */
    boolean getConfigFlag(Enum<?> key, boolean defaultValue);

    /**
     * Creates or updates the stored value for a gateway setting.
     *
     * @param loggedInInfo LoggedInInfo the acting user, checked for the admin privilege
     * @param key Enum the configuration key
     * @param value String the value to store
     * @return SystemPreferences the saved row
     */
    SystemPreferences saveConfig(LoggedInInfo loggedInInfo, Enum<?> key, String value);

    /**
     * Returns the most recent gateway transaction log rows, optionally filtered by provider or,
     * when no provider is given, by external system.
     *
     * @param loggedInInfo LoggedInInfo the acting user, checked for the admin privilege
     * @param providerNo String restrict to one provider, or null
     * @param externalSystem String restrict to one external system when providerNo is null, or null
     * @param maxRows int the maximum number of rows to return
     * @return List&lt;OMDGatewayTransactionLog&gt; the matching rows, newest first, capped at maxRows
     */
    List<OMDGatewayTransactionLog> getRecentLogs(LoggedInInfo loggedInInfo, String providerNo, String externalSystem, int maxRows);

    /**
     * Returns the provider security records linked to a ONE ID subject. Called only from the ONE ID
     * callback before a session exists, so it carries no privilege check; the trust boundary is the
     * OAuth state and nonce plus the id-token signature that produced the subject.
     *
     * @param subject String the ONE ID subject (sub), stored as the provider's oneIdKey
     * @return List&lt;Security&gt; the matching security records, empty when none are linked
     */
    List<Security> findProvidersByOneId(String subject);

    /**
     * Creates or updates the persisted ONE ID session for a provider. Called only from the ONE ID
     * callback while it establishes the session, so it carries no privilege check; the trust boundary
     * is the OAuth handshake that just authenticated the provider.
     *
     * @param oneIdSession OneIdSession the session row to store, keyed by provider number
     */
    void saveOneIdSession(OneIdSession oneIdSession);

    /**
     * Removes the persisted ONE ID session for a provider so it is no longer rehydrated on later
     * requests. No-op when no session exists.
     *
     * @param loggedInInfo LoggedInInfo the acting user, allowed as an admin or the owning provider
     * @param providerNo String the provider number
     */
    void removeOneIdSession(LoggedInInfo loggedInInfo, String providerNo);

    /**
     * Removes the acting provider's own ONE ID session.
     *
     * <p>Authorized on identity rather than on a privilege: a provider clearing their own session
     * needs no grant, because establishing it needed none. Logout has to work for every provider,
     * including one whose role was never granted EHR connectivity or had it withdrawn after they
     * signed in, and a session left behind is rehydrated on their next login.</p>
     *
     * @param loggedInInfo LoggedInInfo the acting provider's session information
     */
    void removeOwnOneIdSession(LoggedInInfo loggedInInfo);

    /**
     * Clears the ONE ID association (key and email) from a provider's security record.
     *
     * @param loggedInInfo LoggedInInfo the acting user, allowed as an admin or the owning provider
     * @param providerNo String the provider number
     * @return boolean true when a security record was found and cleared
     */
    boolean clearOneIdBinding(LoggedInInfo loggedInInfo, String providerNo);

    /**
     * Returns a provider's active UAO values, the default first.
     *
     * @param loggedInInfo LoggedInInfo the acting user, allowed as an admin or the owning provider
     * @param providerNo String the provider number
     * @return List&lt;UAO&gt; the active UAO values, default-first
     */
    List<UAO> findUaosByProvider(LoggedInInfo loggedInInfo, String providerNo);

    /**
     * Returns a single UAO value by id, or null when none exists.
     *
     * @param loggedInInfo LoggedInInfo the acting user, allowed as an admin or the value's owning provider
     * @param id Integer the UAO id
     * @return UAO the value, or null
     */
    UAO findUao(LoggedInInfo loggedInInfo, Integer id);

    /**
     * Persists a new UAO value.
     *
     * @param loggedInInfo LoggedInInfo the acting user, checked for the admin privilege
     * @param uao UAO the value to create
     */
    void createUao(LoggedInInfo loggedInInfo, UAO uao);

    /**
     * Updates an existing UAO value.
     *
     * @param loggedInInfo LoggedInInfo the acting user, checked for the admin privilege
     * @param uao UAO the value to update
     */
    void updateUao(LoggedInInfo loggedInInfo, UAO uao);

    /**
     * Makes one of a provider's UAO values the default and clears the flag on the others.
     *
     * @param loggedInInfo LoggedInInfo the acting user, allowed as an admin or the owning provider
     * @param uao UAO the value to make default
     * @param providerNo String the provider number
     */
    void setDefaultUao(LoggedInInfo loggedInInfo, UAO uao, String providerNo);

    /**
     * Attaches a UAO to the provider's persisted ONE ID session so it survives across requests and
     * every gateway call carries it. No-op when the provider has no ONE ID session.
     *
     * @param loggedInInfo LoggedInInfo the acting user, allowed as an admin or the owning provider
     * @param providerNo String the provider number
     * @param uaoValue String the UAO value sent to the gateway
     * @param uaoFriendlyName String the human-readable UAO label
     */
    void setSessionUao(LoggedInInfo loggedInInfo, String providerNo, String uaoValue, String uaoFriendlyName);

    /**
     * Returns a provider's stored ONE ID session, or null when they have none.
     *
     * @param loggedInInfo LoggedInInfo the acting user
     * @param providerNo   String the provider whose session is wanted
     * @return OneIdSession the stored session, or null
     */
    OneIdSession findOneIdSession(LoggedInInfo loggedInInfo, String providerNo);

    /**
     * Clears the authority on a provider's ONE ID session, but only while it is still the one they
     * are on and no active assignment still grants it.
     *
     * @param loggedInInfo LoggedInInfo the acting user, allowed as an admin or the owning provider
     * @param providerNo String the provider whose session is being cleared
     * @param uaoValue String the withdrawn authority
     * @return boolean true when the session was cleared
     */
    boolean clearSessionUaoIfWithdrawn(LoggedInInfo loggedInInfo, String providerNo, String uaoValue);

    /**
     * Persists the CMS-generated context topic onto the provider's ONE ID session so it survives
     * across requests and every context call is made against the same topic. No-op when the provider
     * has no ONE ID session.
     *
     * @param loggedInInfo LoggedInInfo the acting user, allowed as an admin or the owning provider
     * @param providerNo String the provider number
     * @param hubTopic String the CMS context topic (hub.topic)
     */
    void setSessionHubTopic(LoggedInInfo loggedInInfo, String providerNo, String hubTopic);

    /**
     * Records the CMS-issued hub topic on the acting provider's own ONE ID session.
     *
     * <p>No grant beyond being that provider, for the same reason as
     * {@link #removeOwnOneIdSession(LoggedInInfo)}: the topic is issued by the CMS partway through
     * a launch the provider is already authorized for, and demanding a write grant here would
     * refuse a provider holding read-only EHR connectivity after the CMS had already created it,
     * leaving the topic only in memory.</p>
     *
     * @param loggedInInfo LoggedInInfo the acting provider's session information
     * @param hubTopic String the topic the CMS issued
     */
    void setOwnSessionHubTopic(LoggedInInfo loggedInInfo, String hubTopic);

    /**
     * Returns every Viewlet registry entry, including disabled ones, ordered by name.
     *
     * @param loggedInInfo LoggedInInfo the acting user, checked for the admin privilege
     * @return List&lt;OneIdViewlet&gt; the registry entries
     */
    List<OneIdViewlet> findAllViewlets(LoggedInInfo loggedInInfo);

    /**
     * Returns a single Viewlet registry entry by id, or null when none exists.
     *
     * @param loggedInInfo LoggedInInfo the acting user, checked for the admin privilege
     * @param id Integer the Viewlet id
     * @return OneIdViewlet the entry, or null
     */
    OneIdViewlet findViewlet(LoggedInInfo loggedInInfo, Integer id);

    /**
     * Persists a new Viewlet registry entry.
     *
     * @param loggedInInfo LoggedInInfo the acting user, checked for the admin privilege
     * @param viewlet OneIdViewlet the entry to create
     */
    void createViewlet(LoggedInInfo loggedInInfo, OneIdViewlet viewlet);

    /**
     * Updates an existing Viewlet registry entry.
     *
     * @param loggedInInfo LoggedInInfo the acting user, checked for the admin privilege
     * @param viewlet OneIdViewlet the entry to update
     */
    void updateViewlet(LoggedInInfo loggedInInfo, OneIdViewlet viewlet);

    /**
     * Returns the active Viewlets shown in the eChart launch list.
     *
     * @param loggedInInfo LoggedInInfo the acting user, checked for the provider or admin privilege
     * @return List&lt;OneIdViewlet&gt; the active eChart Viewlets, empty when none are configured
     */
    List<OneIdViewlet> findEchartViewlets(LoggedInInfo loggedInInfo);

    /**
     * Returns the active Viewlet registered under a toolbar key, or null when the key is unknown
     * or the Viewlet is disabled.
     *
     * @param loggedInInfo LoggedInInfo the acting user, checked for the provider or admin privilege
     * @param key String the Viewlet's toolbar key
     * @return OneIdViewlet the active entry, or null
     */
    OneIdViewlet findActiveViewletByKey(LoggedInInfo loggedInInfo, String key);

    /**
     * Reads whether the provider is warned before a launch that may open a second simultaneous
     * EHR-service window. A provider with no stored choice is warned; admin-or-owner guarded.
     *
     * @param loggedInInfo LoggedInInfo the acting provider's session information
     * @param providerNo String the provider whose setting is read
     * @return boolean true when the warning is enabled
     */
    boolean isMultiWindowNoticeEnabled(LoggedInInfo loggedInInfo, String providerNo);

    /**
     * Turns the multi-window warning on or off for the provider; admin-or-owner guarded.
     *
     * @param loggedInInfo LoggedInInfo the acting provider's session information
     * @param providerNo String the provider whose setting is changed
     * @param enabled boolean the new state
     * @return boolean the previous state, for the caller's audit record
     */
    boolean setMultiWindowNoticeEnabled(LoggedInInfo loggedInInfo, String providerNo, boolean enabled);
}
