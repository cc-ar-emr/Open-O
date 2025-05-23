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
package org.oscarehr.admin.reports;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.oscarehr.common.dao.OnCallClinicDao;
import org.oscarehr.common.model.OnCallClinic;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SpringUtils;

import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;

public class SaveOnCallClinic2Action extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();

    public String execute() {
        if ("Save".equals(request.getParameter("method"))) {
            return Save();
        }
        if ("Delete".equals(request.getParameter("method"))) {
            return Delete();
        }
        return Load();
    }


    public String Save() {
        String json = "{\"error\" : \"false\"}";
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.getSerializationConfig().setDateFormat(simpleDateFormat);
            OnCallClinic onCallClinic = objectMapper.readValue(request.getParameter("event"), OnCallClinic.class);

            OnCallClinicDao onCallClinicDao = SpringUtils.getBean(OnCallClinicDao.class);
            onCallClinicDao.persist(onCallClinic);
        } catch (Exception e) {
            MiscUtils.getLogger().error("ERROR SAVING ", e);
            json = "{\"error\" : \"true\"}";
        }

        try {
            PrintWriter printWriter = response.getWriter();
            printWriter.print(json);
            printWriter.flush();
        } catch (IOException e) {

        }

        return null;
    }


    public String Delete() {
        OnCallClinicDao onCallClinicDao = SpringUtils.getBean(OnCallClinicDao.class);
        String json = "{\"error\" : \"false\"}";
        try {
            String strId = request.getParameter("id");
            Long id = Long.valueOf(strId);
            if (!onCallClinicDao.remove(id)) {
                json = "{\"error\" : \"true\"}";
            }
        } catch (Exception e) {
            MiscUtils.getLogger().error("ERROR DELETING ", e);
            json = "{\"error\" : \"true\"}";
        }

        try {
            PrintWriter printWriter = response.getWriter();
            printWriter.print(json);
            printWriter.flush();
        } catch (IOException e) {

        }
        return null;
    }

    public String Load() {

        OnCallClinicDao onCallClinicDao = SpringUtils.getBean(OnCallClinicDao.class);

        List<OnCallClinic> onCallClinics = onCallClinicDao.findAll(null, null);

        ObjectMapper mapper = new ObjectMapper();

        try {
            OutputStream out = response.getOutputStream();
            SerializationConfig serializationConfig = mapper.getSerializationConfig();
            serializationConfig.enable(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.setSerializationConfig(serializationConfig);
            mapper.writeValue(out, onCallClinics);
            out.flush();
        } catch (IOException e) {

        }

        return null;
    }

}
