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


package oscar.oscarLab.ca.on.CML.Upload;

import com.opensymphony.xwork2.ActionSupport;
import org.apache.logging.log4j.Logger;
import org.apache.struts2.ServletActionContext;
import org.oscarehr.managers.SecurityInfoManager;
import org.oscarehr.util.DbConnectionFilter;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SpringUtils;
import oscar.OscarProperties;
import oscar.oscarLab.FileUploadCheck;
import oscar.oscarLab.ca.on.CML.ABCDParser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.util.Date;

public class LabUpload2Action extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();

    private SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);
    Logger _logger = org.oscarehr.util.MiscUtils.getLogger();

    public String execute() {
        if (!securityInfoManager.hasPrivilege(LoggedInInfo.getLoggedInInfoFromSession(request), "_lab", "w", null)) {
            throw new SecurityException("missing required security object (_lab)");
        }

        String filename = "";
        String proNo = (String) request.getSession().getAttribute("user");
        if (proNo == null) {
            proNo = "";
        }
        String key = request.getParameter("key");
        String keyToMatch = OscarProperties.getInstance().getProperty("CML_UPLOAD_KEY");
        MiscUtils.getLogger().debug("key=" + key);
        String outcome = "";

        //Checks to verify key is matched and file should be saved locally.
        if (key != null && keyToMatch != null && keyToMatch.equals(key)) {

            try {

                MiscUtils.getLogger().debug("Lab Upload content type = " + importFile.getName());
                InputStream is = Files.newInputStream(importFile.toPath());
                filename = importFile.getName();

                String localFileName = saveFile(is, filename);
                is.close();


                boolean fileUploadedSuccessfully = false;
                if (localFileName != null) {
                    InputStream fis = new FileInputStream(localFileName);
                    int check = FileUploadCheck.UNSUCCESSFUL_SAVE;
                    try {
                        check = FileUploadCheck.addFile(filename, fis, proNo);
                        if (check != FileUploadCheck.UNSUCCESSFUL_SAVE) {
                            outcome = "uploadedPreviously";
                        }
                    } catch (Exception addFileEx) {
                        MiscUtils.getLogger().error("Error", addFileEx);
                        outcome = "databaseNotStarted";
                    }
                    MiscUtils.getLogger().debug("Was file uploaded successfully ?" + fileUploadedSuccessfully);
                    fis.close();
                    if (check != FileUploadCheck.UNSUCCESSFUL_SAVE) {
                        BufferedReader in = new BufferedReader(new FileReader(localFileName));
                        ABCDParser abc = new ABCDParser();
                        abc.parse(in);

                        abc.save(DbConnectionFilter.getThreadLocalDbConnection());
                        outcome = "uploaded";
                    }
                } else {
                    outcome = "accessDenied";  //file could not save
                    MiscUtils.getLogger().debug("Could not save file :" + filename + " to disk");
                }

            } catch (Exception e) {
                MiscUtils.getLogger().error("Error", e);
                outcome = "exception";
            }

        } else {
            outcome = "accessDenied";
        }
        request.setAttribute("outcome", outcome);
        MiscUtils.getLogger().debug("forwarding outcome " + outcome);
        return SUCCESS;
    }


    public LabUpload2Action() {
    }


    /**
     * Save a Jakarta FormFile to a preconfigured place.
     *
     * @param stream
     * @param filename
     * @return String
     */
    private static String saveFile(InputStream stream, String filename) {
        String retVal = null;

        try {
            OscarProperties props = OscarProperties.getInstance();
            //properties must exist
            String place = props.getProperty("DOCUMENT_DIR");

            if (!place.endsWith("/"))
                place = new StringBuilder(place).insert(place.length(), "/").toString();
            retVal = place + "LabUpload." + filename + "." + (new Date()).getTime();
            MiscUtils.getLogger().debug(retVal);

            //write the  file to the file specified
            OutputStream bos = new FileOutputStream(retVal);
            int bytesRead = 0;
            while ((bytesRead = stream.read()) != -1) {
                bos.write(bytesRead);
            }
            bos.close();

            //close the stream
            stream.close();
        } catch (FileNotFoundException fnfe) {

            MiscUtils.getLogger().debug("File not found");
            MiscUtils.getLogger().error("Error", fnfe);
            return retVal;

        } catch (IOException ioe) {
            MiscUtils.getLogger().error("Error", ioe);
            return retVal;
        }
        return retVal;
    }

    private File importFile;

    public File getImportFile() {
        return importFile;
    }

    public void setImportFile(File importFile) {
        this.importFile = importFile;
    }
}
