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


package org.oscarehr.phr.web;

import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.Logger;
import org.oscarehr.common.dao.ProviderPreferenceDao;
import org.oscarehr.common.model.ProviderPreference;
import org.oscarehr.myoscar.client.ws_manager.AccountManager;
import org.oscarehr.myoscar.utils.MyOscarLoggedInInfo;
import org.oscarehr.myoscar_server.ws.LoginResultTransfer3;
import org.oscarehr.myoscar_server.ws.NotAuthorisedException_Exception;
import org.oscarehr.phr.util.MyOscarUtils;
import org.oscarehr.util.EncryptionUtils;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SpringUtils;
import org.oscarehr.util.WebUtils;

import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;

import java.nio.charset.StandardCharsets;

public class PHRLogin2Action extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();

    private static Logger logger = MiscUtils.getLogger();

    public PHRLogin2Action() {
    }

    public String execute() throws Exception {
        HttpSession session = request.getSession();

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        String forwardTo = request.getParameter("forwardto");

        request.setAttribute("forwardToOnSuccess", request.getParameter("forwardToOnSuccess"));

        if (MyOscarUtils.getMyOscarUserNameFromOscar(providerNo) == null) {
            request.setAttribute("phrUserLoginErrorMsg", "You have not registered for MyOSCAR");
            request.setAttribute("phrTechLoginErrorMsg", "No MyOSCAR information in the database");
            return forwardTo;
        }

        String myoscarPassword = request.getParameter("phrPassword");
        try {
            String myOscarUserName = MyOscarUtils.getMyOscarUserNameFromOscar(providerNo);

            LoginResultTransfer3 loginResultTransfer = AccountManager.login(MyOscarLoggedInInfo.getMyOscarServerBaseUrl(), myOscarUserName, myoscarPassword);

            if (loginResultTransfer == null) {
                request.setAttribute("phrUserLoginErrorMsg", "Incorrect user/password");
                return forwardTo;
            } else {

                if (loginResultTransfer.getPerson().getRole() != null && !loginResultTransfer.getPerson().getRole().equals("PATIENT")) {

                    MyOscarLoggedInInfo myOscarLoggedInInfo = new MyOscarLoggedInInfo(loginResultTransfer.getPerson().getId(), loginResultTransfer.getSecurityTokenKey(), request.getSession().getId(), request.getLocale());
                    MyOscarLoggedInInfo.setLoggedInInfo(request.getSession(), myOscarLoggedInInfo);

                    boolean saveMyOscarPassword = WebUtils.isChecked(request, "saveMyOscarPassword");
                    if (saveMyOscarPassword) saveMyOscarPassword(session, providerNo, myoscarPassword);
                } else {
                    logger.error("ERROR :" + loginResultTransfer.getPerson().getUserName() + " can not login with role " + loginResultTransfer.getPerson().getRole());
                    request.setAttribute("phrUserLoginErrorMsg", "Invalid Role: Patient Account can not be used");
                }
            }
        } catch (NotAuthorisedException_Exception e) {
            logger.error("Error", e);
            request.setAttribute("phrUserLoginErrorMsg", "Invalid user/password.");
            return forwardTo;
        } catch (Exception e) {
            logger.error("Error", e);
            request.setAttribute("phrUserLoginErrorMsg", "Error contacting MyOSCAR server, please try again later");
            request.setAttribute("phrTechLoginErrorMsg", e.getMessage());
            return forwardTo;
        }

        logger.debug("Correct user/pass, auth success");
        return forwardTo;
    }

    private void saveMyOscarPassword(HttpSession session, String providerNo, String myoscarPassword) {
        try {
            SecretKeySpec key = MyOscarUtils.getDeterministicallyMangledPasswordSecretKeyFromSession(session);
            byte[] encryptedMyOscarPassword = EncryptionUtils.encrypt(key, myoscarPassword.getBytes(StandardCharsets.UTF_8));

            ProviderPreferenceDao providerPreferenceDao = (ProviderPreferenceDao) SpringUtils.getBean(ProviderPreferenceDao.class);
            ProviderPreference providerPreference = providerPreferenceDao.find(providerNo);
            providerPreference.setEncryptedMyOscarPassword(encryptedMyOscarPassword);
            providerPreferenceDao.merge(providerPreference);
        } catch (Exception e) {
            logger.error("Error saving myoscarPassword.", e);
        }
    }
}
