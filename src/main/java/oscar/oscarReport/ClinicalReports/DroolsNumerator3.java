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


package oscar.oscarReport.ClinicalReports;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;

import org.drools.RuleBase;
import org.drools.io.RuleBaseLoader;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.MiscUtils;

import oscar.OscarProperties;
import oscar.oscarEncounter.oscarMeasurements.MeasurementFlowSheet;
import oscar.oscarEncounter.oscarMeasurements.util.MeasurementDSHelper;
import oscar.util.ConversionUtils;

/**
 * @author jay
 */
public class DroolsNumerator3 implements Numerator {
    String name = null;
    String id = null;
    String file = null;
    String[] outputfields = null;
    Hashtable outputValues = null;

    /**
     * Creates a new instance of DroolsNumerator
     */
    public DroolsNumerator3() {
    }

    public String getId() {
        return id;
    }

    public String getNumeratorName() {
        return name;
    }

    public void setNumeratorName(String name) {
        this.name = name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean evaluate(LoggedInInfo loggedInInfo, String demographicNo) {
        boolean evalTrue = false;
        try {

            Iterator terator = replaceableValues.entrySet().iterator();
            while (terator.hasNext()) {
                Entry en = (Entry) terator.next();
                MiscUtils.getLogger().debug("IN DROOLS3 key " + en.getKey() + " val " + en.getValue());
            }

            String measurement = (String) replaceableValues.get("measurements");
            String startDate = (String) replaceableValues.get("startDate");
            String endDate = (String) replaceableValues.get("endDate");

            Date startDateAsDate = ConversionUtils.fromDateString(startDate, "yyyy-MM-dd");
            Date endDateAsDate = ConversionUtils.fromDateString(endDate, "yyyy-MM-dd");


            MeasurementDSHelper dshelper = new MeasurementDSHelper(loggedInInfo, demographicNo);
            boolean a = dshelper.setMeasurement(measurement, startDateAsDate, endDateAsDate);

            return !a;


        } catch (Exception e) {
            MiscUtils.getLogger().error("Error", e);
        }
        return evalTrue;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getFile() {
        return file;
    }


    public RuleBase loadMeasurementRuleBase(String string) {
        RuleBase measurementRuleBase = null;
        try {
            boolean fileFound = false;
            String measurementDirPath = OscarProperties.getInstance().getProperty("MEASUREMENT_DS_DIRECTORY");

            if (measurementDirPath != null) {
                //if (measurementDirPath.charAt(measurementDirPath.length()) != /)
                File file = new File(OscarProperties.getInstance().getProperty("MEASUREMENT_DS_DIRECTORY") + string);
                if (file.isFile() || file.canRead()) {
                    MiscUtils.getLogger().debug("Loading from file " + file.getName());
                    FileInputStream fis = new FileInputStream(file);
                    measurementRuleBase = RuleBaseLoader.loadFromInputStream(fis);
                    fileFound = true;
                }
            }

            if (!fileFound) {
                URL url = MeasurementFlowSheet.class.getResource("/oscar/oscarEncounter/oscarMeasurements/flowsheets/decisionSupport/" + string);  //TODO: change this so it is configurable;
                MiscUtils.getLogger().debug("loading from URL " + url.getFile());
                measurementRuleBase = RuleBaseLoader.loadFromUrl(url);
            }
        } catch (Exception e) {
            MiscUtils.getLogger().error("Error", e);
        }
        return measurementRuleBase;
    }

    public Hashtable getOutputValues() {
        return outputValues;
    }

    public void parseOutputFields(String str) {
        if (str != null) {
            try {
                if (str.indexOf(",") != -1) {
                    outputfields = str.split(",");
                } else {
                    outputfields = new String[1];
                    outputfields[0] = str;
                }
            } catch (Exception e) {
                MiscUtils.getLogger().error("Error", e);
            }
        }
    }

    public String[] getOutputFields() {
        return outputfields;
    }


    /////NEW FIELDS
    String[] replaceKeys = null;
    Hashtable replaceableValues = null;

    public String[] getReplaceableKeys() {
        return replaceKeys;
    }

    public void parseReplaceValues(String str) {
        if (str != null) {
            try {
                MiscUtils.getLogger().debug("parsing string " + str);
                if (str.indexOf(",") != -1) {
                    replaceKeys = str.split(",");
                } else {
                    replaceKeys = new String[1];
                    replaceKeys[0] = str;
                }
            } catch (Exception e) {
                MiscUtils.getLogger().error("Error", e);
            }
        }
    }

    public boolean hasReplaceableValues() {
        boolean repVal = false;
        if (replaceKeys != null) {
            repVal = true;
        }
        return repVal;
    }

    public void setReplaceableValues(Hashtable vals) {
        replaceableValues = vals;
    }

    public Hashtable getReplaceableValues() {
        return replaceableValues;
    }


}
