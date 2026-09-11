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
package ca.openosp.openo.integration.oneId.web;

import ca.openosp.openo.integration.dhdr.OmdGateway;
import ca.openosp.openo.integration.ohcms.CMSManager;
import ca.openosp.openo.integration.oneId.OneIdGatewayData;
import ca.openosp.openo.utility.LoggedInInfo;
import ca.openosp.openo.utility.MiscUtils;
import org.apache.logging.log4j.Logger;

/**
 * Ends a provider's ONE ID session at the far end, for the two actions that finish one: signing out
 * of ONE ID, and unlinking the account.
 *
 * @since 2026-08-12
 */
final class OneIdSessionTeardown {

    private static final Logger logger = MiscUtils.getLogger();

    private OneIdSessionTeardown() {
    }

    /**
     * Clears the provider and patient from the EHR context and revokes the access token.
     *
     * <p>Both calls run against Ontario Health, and both are made before any local state is
     * deleted, because each needs the tokens that deletion discards. Either can fail without
     * stopping the caller: a session the far end still holds is worse left alongside local state
     * that says the provider is signed out.
     *
     * @param loggedInInfo LoggedInInfo the acting provider's session information
     * @param gatewayData  OneIdGatewayData the gateway data holding the token being revoked
     */
    static void endRemoteSession(LoggedInInfo loggedInInfo, OneIdGatewayData gatewayData) {
        try {
            CMSManager.userLogout(loggedInInfo);
        } catch (Exception e) {
            logger.error("ONE ID CMS context clear failed\n" + OmdGateway.stackTraceWithoutMessages(e));
        }
        try {
            new OmdGateway().revokeToken(loggedInInfo, gatewayData);
        } catch (Exception e) {
            logger.error("ONE ID token revoke failed\n" + OmdGateway.stackTraceWithoutMessages(e));
        }
    }
}
