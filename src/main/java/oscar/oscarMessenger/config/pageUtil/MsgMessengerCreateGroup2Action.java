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

package oscar.oscarMessenger.config.pageUtil;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.oscarehr.common.dao.GroupsDao;
import org.oscarehr.common.model.Groups;
import org.oscarehr.managers.SecurityInfoManager;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.SpringUtils;

import oscar.oscarMessenger.data.MsgAddressBookMaker;

import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;

public class MsgMessengerCreateGroup2Action extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();


    private GroupsDao dao = SpringUtils.getBean(GroupsDao.class);
    private SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);

    public String execute() throws IOException, ServletException {

        if (!securityInfoManager.hasPrivilege(LoggedInInfo.getLoggedInInfoFromSession(request), "_admin", "w", null)) {
            throw new SecurityException("missing required security object (_admin)");
        }

        String grpName = this.getGroupName();
        String parentID = this.getParentID();
        String type = this.getType2();

        grpName = grpName.trim();

        if (!grpName.equals("")) {
            if (type.equals("1")) {
                GroupsDao gd = SpringUtils.getBean(GroupsDao.class);
                Groups g = new Groups();
                g.setParentId(Integer.parseInt(parentID));
                g.setGroupDesc(grpName);
                gd.persist(g);

                MsgAddressBookMaker addMake = new MsgAddressBookMaker();
                addMake.updateAddressBook();
            } else if (type.equals("2")) {
                Groups g = dao.find(Integer.parseInt(parentID));
                if (g != null) {
                    g.setGroupDesc(grpName);
                    dao.merge(g);
                }
                MsgAddressBookMaker addMake = new MsgAddressBookMaker();
                addMake.updateAddressBook();
            }
        }
        request.setAttribute("groupNo", parentID);
        return SUCCESS;
    }

    String groupName;
    String parentID;
    String type;

    public void setType2(String type) {

        this.type = type;
    }

    public String getType2() {

        if (this.type == null) {
            this.type = new String();
        }
        return this.type;
    }

    public void setParentID(String parentID) {
        this.parentID = parentID;
    }

    public String getParentID() {
        if (this.parentID == null) {
            this.parentID = new String();
        }
        return this.parentID;

    }


    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupName() {
        if (this.groupName == null) {
            this.groupName = new String();
        }
        return this.groupName;
    }
}
