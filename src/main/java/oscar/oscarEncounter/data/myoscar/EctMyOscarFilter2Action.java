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
package oscar.oscarEncounter.data.myoscar;

import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.oscarehr.common.model.Measurement;
import org.oscarehr.common.service.myoscar.MeasurementsManager;
import org.oscarehr.myoscar.commons.MedicalDataType;
import org.oscarehr.myoscar.utils.MyOscarLoggedInInfo;
import org.oscarehr.util.DateRange;

import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;
import oscar.util.ConversionUtils;
import oscar.util.DateUtils;

public class EctMyOscarFilter2Action extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();


    @Override
    public String execute() throws Exception {
        MyOscarMeasurements measurements = getMeasurements();
        if (measurements != null) {
            DateRange range = this.getDateRange();
            measurements.filter(range);
        }
        request.setAttribute("measurements", measurements);

        return SUCCESS;
    }

    private MyOscarMeasurements getMeasurements() {
        MyOscarLoggedInInfo myOscarLoggedInInfo = MyOscarLoggedInInfo.getLoggedInInfo(request.getSession());

        Map<MedicalDataType, List<Measurement>> mm = MeasurementsManager.getMeasurementsFromMyOscar(myOscarLoggedInInfo,
                this.getDemoNoAsInt(), new MedicalDataType[]{this.getMedicalDataType()});


        List<Measurement> measurementList = mm.get(this.getMedicalDataType());
        List<MyOscarMeasurement> moms = new ArrayList<MyOscarMeasurement>();
        if (measurementList != null) {
            for (Measurement m : measurementList) {
                moms.add(toMyOscarMeasurement(this.getMedicalDataType(), m));
            }
        }
        Collections.sort(moms);

        MyOscarMeasurements result = new MyOscarMeasurements(this.getMedicalDataType(), moms);
        return result;
    }

    private MyOscarMeasurement toMyOscarMeasurement(MedicalDataType mdt, Measurement m) {
        MyOscarMeasurement result = null;
        switch (mdt) {
            case GLUCOSE:
                result = new GlucoseMeasurement();
                break;
            case HEIGHT_AND_WEIGHT:
                result = new WeightMeasurement();
                break;
            case BLOOD_PRESSURE:
                result = new BloodPressureMeasurement();
                break;
            default:
                break;
        }

        if (result != null) {
            result.setMeasurement(m);
        }

        return result;
    }
    private String type;

    private String demoNo;

    private String from;

    private String to;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public MedicalDataType getMedicalDataType() {
        if (getType() == null) {
            return null;
        }

        String type = getType().toLowerCase();

        for (MedicalDataType m : MedicalDataType.values()) {
            if (m.name().toLowerCase().equals(type)) {
                return m;
            }
        }

        return null;
    }

    public DateRange getDateRange() {
        Date fromDate = null;
        if (getFrom() != null && !getFrom().isEmpty()) {
            try {
                fromDate = DateUtils.toDate(getFrom());
            } catch (Exception e) {
                // swallow
            }
        }

        Date toDate = null;
        if (getTo() != null && !getTo().isEmpty()) {
            try {
                toDate = DateUtils.toDate(getTo());
            } catch (Exception e) {
                // swallow
            }
        }

        DateRange range = new DateRange(fromDate, toDate);
        return range;
    }

    public String getDemoNo() {
        return demoNo;
    }

    public Integer getDemoNoAsInt() {
        return ConversionUtils.fromIntString(getDemoNo());
    }

    public void setDemoNo(String demoNo) {
        this.demoNo = demoNo;
    }

}
