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


package oscar.form;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.oscarehr.util.DbConnectionFilter;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.MiscUtils;

import oscar.OscarProperties;
import oscar.oscarDB.DBHandler;
import oscar.util.UtilDateUtilities;

public class FrmONAREnhancedRecord extends FrmRecord {


    protected String _newDateFormat = "yyyy-MM-dd"; //handles both date formats, but yyyy/MM/dd is displayed to avoid deprecation


    public FrmONAREnhancedRecord() {
        this.dateFormat = "yyyy/MM/dd";
    }

    public Properties getFormRecord(LoggedInInfo loggedInInfo, int demographicNo, int existingID) throws SQLException {
        Properties props = new Properties();

        if (existingID <= 0) {

            this.setDemoProperties(loggedInInfo, demographicNo, props);
            props.setProperty("c_lastName", StringUtils.trimToEmpty(demographic.getLastName()));
            props.setProperty("c_firstName", StringUtils.trimToEmpty(demographic.getFirstName()));
            props.setProperty("c_hin", demographic.getHin());

            if ("ON".equals(demographic.getHcType())) {
                props.setProperty("c_hinType", "OHIP");
            } else if ("QC".equals(demographic.getHcType())) {
                props.setProperty("c_hinType", "RAMQ");
            } else {
                props.setProperty("c_hinType", "OTHER");
            }
            props.setProperty("c_fileNo", StringUtils.trimToEmpty(demographic.getChartNo()));

            props.setProperty("pg1_homePhone", StringUtils.trimToEmpty(demographic.getPhone()));
            props.setProperty("pg1_workPhone", StringUtils.trimToEmpty(demographic.getPhone2()));

            props.setProperty("formCreated", UtilDateUtilities.DateToString(new Date(), dateFormat));
            props.setProperty("pg1_formDate", UtilDateUtilities.DateToString(new Date(), dateFormat));
        } else {
            //join it up so the resulting props have values from all 3 tables
            String sql = "SELECT * FROM " + TABLE_ONAR_RECORD + " rec, " + TABLE_ONAR_EXT1 + " ext1, " + TABLE_ONAR_EXT2 + " ext2 WHERE rec.ID = ext1.ID and rec.ID = ext2.ID and rec.demographic_no = ? AND rec.ID = ?";
            props = (new FrmRecordHelp()).getFormRecord(sql, demographicNo, existingID);
        }

        return props;
    }

    public int saveFormRecord(Properties props) throws SQLException {
        //get the names/types of each column in the 3 tables
        List<String> namesA = getColumnNames(TABLE_ONAR_RECORD);
        List<String> namesB = getColumnNames(TABLE_ONAR_EXT1);
        List<String> namesC = getColumnNames(TABLE_ONAR_EXT2);

        //insert the initial record, and grab the ID to do the inserts on the other 2 tables
        int id = addRecord(props, TABLE_ONAR_RECORD, namesA, null);
        addRecord(props, TABLE_ONAR_EXT1, namesB, id);
        addRecord(props, TABLE_ONAR_EXT2, namesC, id);

        return id;
    }

    public Properties getPrintRecord(int demographicNo, int existingID) throws SQLException {
        //join the 3 tables
        String sql = "SELECT * FROM " + TABLE_ONAR_RECORD + " rec, " + TABLE_ONAR_EXT1 + " ext1, " + TABLE_ONAR_EXT2 + " ext2 WHERE rec.ID = ext1.ID and rec.ID = ext2.ID and rec.demographic_no = ? AND rec.ID = ?";
        return ((new FrmRecordHelp()).getPrintRecord(sql, demographicNo, existingID));
    }

    public String findActionValue(String submit) throws SQLException {
        return ((new FrmRecordHelp()).findActionValue(submit));
    }

    public String createActionURL(String where, String action, String demoId, String formId) throws SQLException {
        return ((new FrmRecordHelp()).createActionURL(where, action, demoId, formId));
    }

    public boolean isSendToPing(String demoNo) throws SQLException {
        boolean ret = false;

        if ("yes".equalsIgnoreCase(OscarProperties.getInstance().getProperty("PHR", ""))) {

            if (this.demographic != null) {
                String demoEmail = this.demographic.getEmail();
                if (demoEmail != null && demoEmail.length() > 5 && demoEmail.matches(".*@.*")) {
                    ret = true;
                }
            }
        }
        return ret;
    }


    // Define valid table names as constants to prevent SQL injection
    private static final String TABLE_ONAR_RECORD = "formONAREnhancedRecord";
    private static final String TABLE_ONAR_EXT1 = "formONAREnhancedRecordExt1";
    private static final String TABLE_ONAR_EXT2 = "formONAREnhancedRecordExt2";
    
    private List<String> getColumnNames(String table) throws SQLException {
        // Validate table name against constants to prevent SQL injection
        String validatedTable;
        if (TABLE_ONAR_RECORD.equals(table)) {
            validatedTable = TABLE_ONAR_RECORD;
        } else if (TABLE_ONAR_EXT1.equals(table)) {
            validatedTable = TABLE_ONAR_EXT1;
        } else if (TABLE_ONAR_EXT2.equals(table)) {
            validatedTable = TABLE_ONAR_EXT2;
        } else {
            throw new IllegalArgumentException("Invalid table name: " + table);
        }
        
        List<String> result = new ArrayList<String>();
        ResultSet rs2 = null;
        Statement stmt = null;

        try {
            stmt = DbConnectionFilter.getThreadLocalDbConnection().createStatement();
            // Using the validated constant table name
            rs2 = stmt.executeQuery("SELECT * FROM " + validatedTable + " LIMIT 1");

            ResultSetMetaData md = rs2.getMetaData();

            for (int i = 1; i <= md.getColumnCount(); i++) {
                String name = md.getColumnName(i);
                String type = md.getColumnTypeName(i);
                result.add(name + "|" + type);
            }
        } finally {
            if (rs2 != null) {
                rs2.close();
            }
            if (stmt != null) {
                stmt.close();
            }
        }

        return result;
    }

    int addRecord(Properties props, String table, List<String> namesA, Integer id) throws SQLException {
        // Validate table name against constants to prevent SQL injection
        String validatedTable;
        if (TABLE_ONAR_RECORD.equals(table)) {
            validatedTable = TABLE_ONAR_RECORD;
        } else if (TABLE_ONAR_EXT1.equals(table)) {
            validatedTable = TABLE_ONAR_EXT1;
        } else if (TABLE_ONAR_EXT2.equals(table)) {
            validatedTable = TABLE_ONAR_EXT2;
        } else {
            throw new IllegalArgumentException("Invalid table name: " + table);
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ");
        sb.append(validatedTable);
        sb.append(" (");
        for (String name : namesA) {
            sb.append(name.split("\\|")[0] + ",");
        }
        sb.deleteCharAt(sb.length() - 1);

        sb.append(") VALUES (");

        for (String name : namesA) {
            sb.append("?,");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(");");

        PreparedStatement preparedStmt = null;
        try {
            preparedStmt = DbConnectionFilter.getThreadLocalDbConnection().prepareStatement(sb.toString(), Statement.RETURN_GENERATED_KEYS);

            for (int x = 0; x < namesA.size(); x++) {
                String t = namesA.get(x);
                String theName = t.split("\\|")[0];
                String type = t.split("\\|")[1];

                if ("ID".equals(theName)) {
                    if (id == null) {
                        preparedStmt.setNull(x + 1, Types.INTEGER);
                    } else {
                        preparedStmt.setInt(x + 1, id.intValue());
                    }
                    continue;
                }


                if ("VARCHAR".equals(type) || "CHAR".equals(type) || "TEXT".equals(type)) {
                    String value = props.getProperty(theName);
                    if (value == null) {
                        preparedStmt.setNull(x + 1, getType(type));
                    } else {
                        preparedStmt.setString(x + 1, value);
                    }
                } else if ("INT".equals(type) || "TINYINT".equals(type) || "BIT".equals(type)) {
                    String value = props.getProperty(theName);
                    if (value != null && value.isEmpty()) {
                        MiscUtils.getLogger().info("empty value for " + theName);
                    }
                    if (value == null || value.isEmpty()) {
                        preparedStmt.setInt(x + 1, 0);
                    } else if (value.equalsIgnoreCase("on") || value.equalsIgnoreCase("checked='checked'")) {
                        preparedStmt.setInt(x + 1, 1);
                    } else {
                        try {
                            preparedStmt.setInt(x + 1, Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            MiscUtils.getLogger().error("Invalid number format for " + theName + ": " + value, e);
                            preparedStmt.setInt(x + 1, 0);
                        }
                    }
                } else if ("DATE".equals(type)) {
                    String value = props.getProperty(theName);
                    Date d = null;

                    if (theName.equalsIgnoreCase("formEdited")) {
                        d = new Date();
                    } else {
                        if ((value == null) || (value.indexOf('/') != -1))
                            d = UtilDateUtilities.StringToDate(value, dateFormat);
                        else
                            d = UtilDateUtilities.StringToDate(value, _newDateFormat);
                    }
                    if (d == null)
                        preparedStmt.setNull(x + 1, Types.DATE);
                    else
                        preparedStmt.setDate(x + 1, new java.sql.Date(d.getTime()));

                } else if ("TIMESTAMP".equals(type)) {
                    Date d;
                    if (theName.equalsIgnoreCase("formEdited")) {
                        d = new Date();
                    } else {
                        d = UtilDateUtilities.StringToDate(props.getProperty(theName), "yyyyMMddHHmmss");
                    }
                    if (d == null)
                        preparedStmt.setNull(x + 1, Types.TIMESTAMP);
                    else
                        preparedStmt.setTimestamp(x + 1, new java.sql.Timestamp(d.getTime()));


                } else {
                    MiscUtils.getLogger().error("missing type handler for this column " + theName + " of type " + type, new Exception());
                }

            }

            preparedStmt.executeUpdate();

            if (id == null) {
                ResultSet rs = null;
                try {
                    rs = preparedStmt.getGeneratedKeys();

                    if (rs.next()) {
                        id = rs.getInt(1);
                    }
                } catch (SQLException e) {
                    MiscUtils.getLogger().error("Error retrieving generated keys", e);
                    throw e;
                } finally {
                    if (rs != null) {
                        try {
                            rs.close();
                        } catch (SQLException e) {
                            MiscUtils.getLogger().error("Error closing result set", e);
                        }
                    }
                }
            }
        } finally {
            if (preparedStmt != null) {
                preparedStmt.close();
            }
        }

        return id;
    }


    private int getType(String type) {
        if ("VARCHAR".equals(type)) {
            return Types.VARCHAR;
        }
        if ("CHAR".equals(type)) {
            return Types.CHAR;
        }
        return -1;
    }

}

