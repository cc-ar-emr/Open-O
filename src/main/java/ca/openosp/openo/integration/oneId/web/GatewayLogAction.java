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

import ca.openosp.openo.commn.model.OMDGatewayTransactionLog;
import ca.openosp.openo.managers.EhrConnectivityManager;
import ca.openosp.openo.managers.SecurityInfoManager;
import ca.openosp.openo.utility.LoggedInInfo;
import ca.openosp.openo.utility.SpringUtils;
import org.apache.struts2.ActionSupport;
import org.apache.struts2.ServletActionContext;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Read-only viewer of the ONE ID gateway transaction log. Shows the most recent rows, optionally
 * filtered by provider or external system. Restricted to administrators.
 *
 * @since 2026-07-01
 */
public class GatewayLogAction extends ActionSupport {

    private final HttpServletRequest request = ServletActionContext.getRequest();

    private final SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);
    private final EhrConnectivityManager ehrConnectivityManager =
            SpringUtils.getBean(EhrConnectivityManager.class);

    private static final String SEC_OBJECT = "_admin.ehrConnectivity";
    private static final int MAX_ROWS = 500;

    @Override
    public String execute() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        if (!securityInfoManager.hasPrivilege(loggedInInfo, SEC_OBJECT, "r", null)) {
            throw new SecurityException("missing required sec object (" + SEC_OBJECT + ")");
        }

        String providerNo = trimToNull(request.getParameter("providerNo"));
        String externalSystem = trimToNull(request.getParameter("externalSystem"));

        List<OMDGatewayTransactionLog> logs = ehrConnectivityManager.getRecentLogs(loggedInInfo, providerNo, externalSystem, MAX_ROWS);

        request.setAttribute("logs", logs);
        request.setAttribute("providerNo", providerNo);
        request.setAttribute("externalSystem", externalSystem);
        request.setAttribute("rowLimit", MAX_ROWS);
        return "success";
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        value = value.trim();
        return value.isEmpty() ? null : value;
    }
}
