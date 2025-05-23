//CHECKSTYLE:OFF
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
package org.oscarehr.dashboard.admin;

import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.oscarehr.common.model.Provider;
import org.oscarehr.common.model.Tickler;
import org.oscarehr.common.model.TicklerCategory;
import org.oscarehr.common.model.TicklerTextSuggest;
import org.oscarehr.dashboard.handler.TicklerHandler;
import org.oscarehr.managers.ProviderManager2;
import org.oscarehr.managers.SecurityInfoManager;
import org.oscarehr.managers.TicklerManager;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SpringUtils;

import net.sf.json.JSONObject;

import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;

public class AssignTickler2Action extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();


    private SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);
    private TicklerManager ticklerManager = SpringUtils.getBean(TicklerManager.class);
    private ProviderManager2 providerManager = SpringUtils.getBean(ProviderManager2.class);

    public String execute() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);

        if (!securityInfoManager.hasPrivilege(loggedInInfo, "_tickler", SecurityInfoManager.WRITE, null)) {
            return "unauthorized";
        }

        if ("saveTickler".equals(request.getParameter("method"))) {
            return saveTickler();
        }

        String demographics = request.getParameter("demographics");
        Tickler.PRIORITY[] priorities = Tickler.PRIORITY.values();
        List<TicklerTextSuggest> textSuggestions = ticklerManager.getActiveTextSuggestions(loggedInInfo);
        List<Provider> providers = providerManager.getProviders(loggedInInfo, Boolean.TRUE);
        List<TicklerCategory> ticklerCategories = ticklerManager.getActiveTicklerCategories(loggedInInfo);

        request.setAttribute("priorities", priorities);
        request.setAttribute("textSuggestions", textSuggestions);
        request.setAttribute("providers", providers);
        request.setAttribute("ticklerCategories", ticklerCategories);
        request.setAttribute("demographics", demographics);

        return SUCCESS;
    }


    @SuppressWarnings({"unchecked", "unused"})
    public String saveTickler() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);

        if (!securityInfoManager.hasPrivilege(loggedInInfo, "_tickler", SecurityInfoManager.WRITE, null)) {
            return "unauthorized";
        }

        TicklerHandler ticklerHandler = new TicklerHandler(loggedInInfo, ticklerManager);
        ticklerHandler.createMasterTickler(request.getParameterMap());
        JSONObject jsonObject = new JSONObject();

        if (ticklerHandler.addTickler(request.getParameter("demographics"))) {
            jsonObject.put("success", "true");
        } else {
            jsonObject.put("success", "false");
        }

        try {
            jsonObject.write(response.getWriter());
        } catch (IOException e) {
            MiscUtils.getLogger().error("JSON response failed", e);
            return "error";
        }

        return null;
    }
}
