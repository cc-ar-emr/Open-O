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


package oscar.oscarProvider.pageUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.oscarehr.util.LoggedInInfo;

import oscar.oscarProvider.data.ProviderMyOscarIdData;


import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;

public class ProEditMyOscarId2Action extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();


    @Override
    public String execute()
            throws Exception {
        String forward;
        String providerNo = LoggedInInfo.getLoggedInInfoFromSession(request).getLoggedInProviderNo();
        if (providerNo == null)
            return "eject";

        //DynaActionForm frm = (DynaActionForm) form;
        String loginId = myOscarLoginId;

        if (loginId != null) {
            int indexOfAt = loginId.indexOf('@');
            if (indexOfAt != -1) {
                loginId = loginId.substring(0, indexOfAt);
            }
        }

        if (ProviderMyOscarIdData.getMyOscarId(providerNo).equals(loginId)) {
            addActionError(getText("provider.setPHRLogin.msgNotUnique"));
            forward = "failure";
        } else if (ProviderMyOscarIdData.setId(providerNo, loginId)) {
            request.setAttribute("status", "complete");
            forward = SUCCESS;
        } else {
            forward = ERROR;
        }
        return forward;
    }

    private String myOscarLoginId;

    public String getMyOscarLoginId() {
        return myOscarLoginId;
    }

    public void setMyOscarLoginId(String myOscarLoginId) {
        this.myOscarLoginId = myOscarLoginId;
    }
}
