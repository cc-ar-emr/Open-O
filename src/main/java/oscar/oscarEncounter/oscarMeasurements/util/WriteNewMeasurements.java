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

package oscar.oscarEncounter.oscarMeasurements.util;

//used by eforms for writing measurements

import org.apache.commons.validator.GenericValidator;
import org.oscarehr.common.dao.MeasurementDao;
import org.oscarehr.common.dao.MeasurementDaoImpl.SearchCriteria;
import org.oscarehr.common.dao.MeasurementTypeDao;
import org.oscarehr.common.model.Measurement;
import org.oscarehr.common.model.MeasurementType;
import org.oscarehr.common.model.Validations;
import org.oscarehr.util.SpringUtils;
import oscar.oscarEncounter.oscarMeasurements.pageUtil.EctValidation;
import oscar.util.ConversionUtils;
import oscar.util.UtilDateUtilities;

import java.util.*;

public class WriteNewMeasurements {

    private static MeasurementTypeDao measurementTypeDao = SpringUtils.getBean(MeasurementTypeDao.class);
    private static MeasurementDao dao = SpringUtils.getBean(MeasurementDao.class);

    static private int getMeasurement(Vector measures, String type) {
        for (int i = 0; i < measures.size(); i++) {
            Hashtable curmeasure = (Hashtable) measures.get(i);
            if (((String) curmeasure.get("type")).equals(type)) {
                return i;
            }
        }
        return -1;
    }

    static private void preProcess(Vector measures) {
        // fills in required values

        for (int i = 0; i < measures.size(); i++) {
            Hashtable curmeasure = (Hashtable) measures.get(i);
            String type = (String) curmeasure.get("type");
            String measuringInst = (String) curmeasure.get("measuringInstruction");
            String comments = (String) curmeasure.get("comments");
            String dateObserved = (String) curmeasure.get("dateObserved");
            java.util.Date now = new Date();
            String dateEntered = UtilDateUtilities.DateToString(now, "yyyy-MM-dd HH:mm:ss");
            String sql;
            if (measuringInst == null || measuringInst.equals("")) {
                List<MeasurementType> tmp = measurementTypeDao.findByType(type);
                if (tmp.size() > 0) {
                    measuringInst = tmp.get(0).getMeasuringInstruction();
                    curmeasure.put("measuringInstruction", measuringInst);
                } else {
                    continue;
                }

            }
            if (comments == null) {
                curmeasure.put("comments", "");
            }
            if (dateObserved == null || dateObserved.equals("")) {
                curmeasure.put("dateObserved", dateEntered);
            }
            curmeasure.put("dateEntered", dateEntered);
        }

    }

    static public void write(Vector measures, String demographicNo, String providerNo) {

        for (int i = 0; i < measures.size(); i++) {
            Hashtable measure = (Hashtable) measures.get(i);

            String inputValue = (String) measure.get("value");
            String inputType = (String) measure.get("type");
            String mInstrc = (String) measure.get("measuringInstruction");
            String comments = (String) measure.get("comments");
            String dateObserved = (String) measure.get("dateObserved");
            String dateEntered = (String) measure.get("dateEntered");
            // write....
            Measurement m = new Measurement();
            m.setType(inputType);
            m.setDemographicId(Integer.parseInt(demographicNo));
            m.setProviderNo(providerNo);
            m.setDataField(inputValue);
            m.setMeasuringInstruction(mInstrc);
            m.setComments(comments);
            m.setDateObserved(ConversionUtils.fromTimestampString(dateObserved));
            dao.persist(m);

        }

    }

    static public void write(Hashtable measure, String demographicNo, String providerNo) {
        String inputValue = (String) measure.get("value");
        String inputType = (String) measure.get("type");
        String mInstrc = (String) measure.get("measuringInstruction");
        String comments = (String) measure.get("comments");
        String dateObserved = (String) measure.get("dateObserved");
        String dateEntered = (String) measure.get("dateEntered");
        // write....
        Measurement m = new Measurement();
        m.setType(inputType);
        m.setDemographicId(Integer.parseInt(demographicNo));
        m.setProviderNo(providerNo);
        m.setDataField(inputValue);
        m.setMeasuringInstruction(mInstrc);
        m.setComments(comments);
        m.setDateObserved(ConversionUtils.fromTimestampString(dateObserved));
        dao.persist(m);
    }

    public void write(final String followUpType, final String followUpValue, final String demographicNo,
                      final String providerNo, final java.util.Date dateObserved, final String comment) {
        Hashtable measure = new Hashtable();
        measure.put("value", followUpValue);
        measure.put("type", followUpType);
        measure.put("measuringInstruction", "");
        measure.put("comments", comment == null ? "" : comment);
        measure.put("dateObserved", UtilDateUtilities.DateToString(dateObserved, "yyyy-MM-dd HH:mm:ss"));
        measure.put("dateEntered", UtilDateUtilities.DateToString(new Date(), "yyyy-MM-dd HH:mm:ss"));
        write(measure, demographicNo, providerNo);
    }
}
