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

import com.opensymphony.xwork2.ActionSupport;
import net.sf.json.JSONObject;
import org.apache.logging.log4j.Logger;
import org.apache.struts2.ServletActionContext;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.dom4j.io.SAXValidator;
import org.dom4j.util.XMLErrorHandler;
import org.oscarehr.common.model.Dashboard;
import org.oscarehr.common.model.IndicatorTemplate;
import org.oscarehr.managers.DashboardManager;
import org.oscarehr.managers.DashboardManager.ObjectName;
import org.oscarehr.managers.SecurityInfoManager;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SpringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageDashboard2Action extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();


    private static Logger logger = MiscUtils.getLogger();
    private SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);
    private static DashboardManager dashboardManager = SpringUtils.getBean(DashboardManager.class);

    public String execute() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);

        if (!securityInfoManager.hasPrivilege(loggedInInfo, "_dashboardManager", SecurityInfoManager.WRITE, null)) {
            return "unauthorized";
        }

        String method = request.getParameter("method");
        if ("importTemplate".equals(method)) {
            return importTemplate();
        } else if ("importTemplate".equals(method)) {
            return importTemplate();
        } else if ("assignDashboard".equals(method)) {
            return assignDashboard();
        } else if ("saveDashboard".equals(method)) {
            return saveDashboard();
        } else if ("toggleActive".equals(method)) {
            return toggleActive();
        }

        setRequest(loggedInInfo, request);

        return SUCCESS;
    }

    public String importTemplate() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String message = "";

        if (!securityInfoManager.hasPrivilege(loggedInInfo, "_dashboardManager", SecurityInfoManager.WRITE, null)) {
            return "unauthorized";
        }

        byte[] filebytes = null;
        JSONObject json = null;

        if (indicatorTemplateFile != null) {
            // Check file size before processing
            if (indicatorTemplateFile.length() > 5 * 1024 * 1024) { // 5MB limit
                json = new JSONObject();
                json.put("status", "error");
                json.put("message", "File size exceeds maximum allowed (5MB)");
                MiscUtils.getLogger().warn("File upload rejected - size exceeds limit: " + 
                    indicatorTemplateFile.length() + " bytes from user " + loggedInInfo.getLoggedInProviderNo());
            } else {
                try {
                    filebytes = Files.readAllBytes(indicatorTemplateFile.toPath());
                } catch (Exception e) {
                    json = new JSONObject();
                    json.put("status", "error");
                    json.put("message", "Error processing uploaded file");
                    MiscUtils.getLogger().error("Failed to transfer file from user " + 
                        loggedInInfo.getLoggedInProviderNo(), e);
                }
            }
        }

        // TODO add a checksum: Uploaded templates will be checksum hashed, the hash will be stored in an
        // indicatorTemplate column.  Every IndicatorTemplate checksum will be compared to the upload for duplicates.
        // The DashboarManager will contain a method. This class will return an error to the user.

        // TODO run the contained MySQL queries to check for syntax errors and whatever else may be broken.
        // The DashboarManager will contain a method. This class will return an error to the user.

        if (filebytes != null) {
            Boolean isOscarXml = false;
            URL schemaSource = Thread.currentThread().getContextClassLoader().getResource("indicatorXMLTemplates/IndicatorXMLTemplateSchema.xsd");

            try {
                XMLErrorHandler errorHandler = new XMLErrorHandler();
                SAXParserFactory factory = SAXParserFactory.newInstance();
                
                // Secure parser configuration to prevent XXE attacks
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                
                factory.setValidating(true);
                factory.setNamespaceAware(true);
                
                SAXParser parser = factory.newSAXParser();
                SAXReader xmlReader = new SAXReader();
                
                // Configure SAXReader to prevent XXE
                xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                xmlReader.setFeature("http://xml.org/sax/features/external-general-entities", false);
                xmlReader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                
                Document xmlDocument = (Document) xmlReader.read(Files.newBufferedReader(indicatorTemplateFile.toPath()));
                
                parser.setProperty(
                        "http://java.sun.com/xml/jaxp/properties/schemaLanguage",
                        "http://www.w3.org/2001/XMLSchema");
                parser.setProperty(
                        "http://java.sun.com/xml/jaxp/properties/schemaSource",
                        "file:" + schemaSource.getPath());
                
                SAXValidator validator = new SAXValidator(parser.getXMLReader());
                validator.setErrorHandler(errorHandler);
                validator.validate(xmlDocument);
                
                if (!errorHandler.getErrors().hasContent()) {
                    isOscarXml = true;
                }
            } catch (Exception e) {
                MiscUtils.getLogger().error("XML validation failed for user " + 
                    loggedInInfo.getLoggedInProviderNo(), e);
            }

            if (isOscarXml) {
                message = dashboardManager.importIndicatorTemplate(loggedInInfo, filebytes);
                json = JSONObject.fromObject(message);
            } else {
                json = new JSONObject();
                json.put("status", "error");
                json.put("message", "There is a validation error");
            }
        }

        Map<String, String> messageMap = new HashMap<String, String>();
        messageMap.put("status", json.getString("status"));
        messageMap.put("message", json.getString("message"));

        setRequest(loggedInInfo, request, messageMap);

        return SUCCESS;
    }


    @SuppressWarnings("unused")
    public String exportTemplate() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);

        if (!securityInfoManager.hasPrivilege(loggedInInfo, "_dashboardManager", SecurityInfoManager.READ, null)) {
            MiscUtils.getLogger().warn("Unauthorized. Missing _dasboardManager object for user "
                    + loggedInInfo.getLoggedInProviderNo() + " while downloading indicator template.");
            return "unauthorized";
        }

        String indicator = request.getParameter("indicatorId");
        String indicatorName = request.getParameter("indicatorName");

        int indicatorId = 0;
        String xmlTemplate = null;
        OutputStream outputStream = null;

        // Validate indicatorId parameter
        if (indicator == null || !indicator.matches("^\\d+$")) {
            MiscUtils.getLogger().warn("Invalid indicatorId parameter: " + indicator + 
                " from user " + loggedInInfo.getLoggedInProviderNo());
            return ERROR;
        }
        
        // Sanitize indicatorName to prevent header injection and path traversal
        if (indicatorName == null || indicatorName.isEmpty()) {
            indicatorName = "indicator_template-" + System.currentTimeMillis() + ".xml";
        } else {
            // Remove any characters that could be used for path traversal or header injection
            indicatorName = indicatorName.replaceAll("[^a-zA-Z0-9_\\-\\.]", "_");
            indicatorName = indicatorName + ".xml";
        }

        try {
            indicatorId = Integer.parseInt(indicator);
            xmlTemplate = dashboardManager.exportIndicatorTemplate(loggedInInfo, indicatorId);
        } catch (NumberFormatException e) {
            MiscUtils.getLogger().error("Failed to parse indicator ID: " + indicator, e);
            return ERROR;
        }

        if (xmlTemplate != null) {

            response.setContentType("text/xml");
            response.setHeader("Content-disposition", "attachment; filename=" + indicatorName);

            try {
                // Set security headers
                response.setHeader("Content-Security-Policy", "default-src 'self'");
                response.setHeader("X-Content-Type-Options", "nosniff");
                response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                response.setHeader("Pragma", "no-cache");
                
                outputStream = response.getOutputStream();
                outputStream.write(xmlTemplate.getBytes());
            } catch (Exception e) {
                MiscUtils.getLogger().error("Error writing template to output stream for user " + 
                    loggedInInfo.getLoggedInProviderNo(), e);
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.flush();
                        outputStream.close();
                    } catch (IOException e) {
                        logger.error("Failed to close output stream", e);
                    }
                }
            }
        }

        return null;
    }

    @SuppressWarnings("unused")
    public String assignDashboard() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);

        if (!securityInfoManager.hasPrivilege(loggedInInfo, "_dashboardManager", SecurityInfoManager.WRITE, null)) {
            return "unauthorized";
        }
        
        String dashboard = request.getParameter("indicatorDashboardId");
        String indicator = request.getParameter("indicatorId");
        int dashboardId = 0;
        int indicatorId = 0;

        JSONObject jsonObject = new JSONObject();
        
        // Validate dashboard parameter
        if (dashboard != null && !dashboard.isEmpty()) {
            if (!dashboard.matches("^\\d+$")) {
                jsonObject.put("success", "false");
                jsonObject.put("message", "Invalid dashboard ID format");
                MiscUtils.getLogger().warn("Invalid dashboard ID format: " + dashboard + 
                    " from user " + loggedInInfo.getLoggedInProviderNo());
                try {
                    jsonObject.write(response.getWriter());
                    return null;
                } catch (IOException e) {
                    MiscUtils.getLogger().error("JSON response failed", e);
                    return ERROR;
                }
            }
            try {
                dashboardId = Integer.parseInt(dashboard);
            } catch (NumberFormatException e) {
                MiscUtils.getLogger().error("Failed to parse dashboard ID: " + dashboard, e);
                jsonObject.put("success", "false");
                jsonObject.put("message", "Invalid dashboard ID");
                try {
                    jsonObject.write(response.getWriter());
                    return null;
                } catch (IOException ex) {
                    MiscUtils.getLogger().error("JSON response failed", ex);
                    return ERROR;
                }
            }
        }

        // Validate indicator parameter
        if (indicator != null && !indicator.isEmpty()) {
            if (!indicator.matches("^\\d+$")) {
                jsonObject.put("success", "false");
                jsonObject.put("message", "Invalid indicator ID format");
                MiscUtils.getLogger().warn("Invalid indicator ID format: " + indicator + 
                    " from user " + loggedInInfo.getLoggedInProviderNo());
                try {
                    jsonObject.write(response.getWriter());
                    return null;
                } catch (IOException e) {
                    MiscUtils.getLogger().error("JSON response failed", e);
                    return ERROR;
                }
            }
            try {
                indicatorId = Integer.parseInt(indicator);
            } catch (NumberFormatException e) {
                MiscUtils.getLogger().error("Failed to parse indicator ID: " + indicator, e);
                jsonObject.put("success", "false");
                jsonObject.put("message", "Invalid indicator ID");
                try {
                    jsonObject.write(response.getWriter());
                    return null;
                } catch (IOException ex) {
                    MiscUtils.getLogger().error("JSON response failed", ex);
                    return ERROR;
                }
            }
        }

        if (dashboardManager.assignIndicatorToDashboard(loggedInInfo, dashboardId, indicatorId)) {
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


    @SuppressWarnings("unused")
    public String saveDashboard() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String action = null;

        if (!securityInfoManager.hasPrivilege(loggedInInfo, "_dashboardManager", SecurityInfoManager.WRITE, null)) {
            return "unauthorized";
        }

        String dashboardId = request.getParameter("dashboardId");
        String dashboardName = request.getParameter("dashboardName");
        String dashboardActive = request.getParameter("dashboardActive");
        String dashboardDescription = request.getParameter("dashboardDescription");
        Integer id = null;
        Boolean active = Boolean.FALSE;

        // Validate dashboardId parameter
        if (dashboardId != null && !dashboardId.isEmpty()) {
            if (!dashboardId.matches("^\\d+$")) {
                MiscUtils.getLogger().warn("Invalid dashboardId parameter: " + dashboardId + 
                    " from user " + loggedInInfo.getLoggedInProviderNo());
                return ERROR;
            }
            
            try {
                id = Integer.parseInt(dashboardId);
            } catch (NumberFormatException e) {
                MiscUtils.getLogger().error("Failed to parse dashboard ID: " + dashboardId, e);
                return ERROR;
            }
        }
        
        // Validate dashboardName parameter
        if (dashboardName == null || dashboardName.trim().isEmpty()) {
            MiscUtils.getLogger().warn("Missing or empty dashboardName parameter from user " + 
                loggedInInfo.getLoggedInProviderNo());
            return ERROR;
        }
        
        // Sanitize input parameters
        dashboardName = dashboardName.trim();
        if (dashboardDescription != null) {
            dashboardDescription = dashboardDescription.trim();
        }

        if (dashboardActive != null && !dashboardActive.isEmpty()) {
            active = Boolean.TRUE;
        }

        // TODO check the current dashboard table for duplicates.
        // strip out all punctuation and spaces, all names to lower case.
        // Method in DashboardManager, return message to user from this class.

        Dashboard dashboard = new Dashboard();
        dashboard.setId(id);
        dashboard.setName(dashboardName);
        dashboard.setDescription(dashboardDescription);
        dashboard.setActive(active);
        dashboard.setCreator(loggedInInfo.getLoggedInProviderNo());
        dashboard.setEdited(new Date(System.currentTimeMillis()));
        dashboard.setLocked(Boolean.FALSE);

        if (dashboardManager.addDashboard(loggedInInfo, dashboard)) {
            setRequest(loggedInInfo, request);
            action = SUCCESS;
        } else {
            action = ERROR;
        }

        return action;
    }

    @SuppressWarnings("unused")
    public String toggleActive() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);

        if (!securityInfoManager.hasPrivilege(loggedInInfo, "_dashboardManager", SecurityInfoManager.WRITE, null)) {
            return "unauthorized";
        }

        String objectClassName = request.getParameter("objectClassName");
        String objectId = request.getParameter("objectId");
        String activeState = request.getParameter("active");
        Boolean active = Boolean.FALSE;
        int id = 0;

        // Validate objectClassName parameter
        if (objectClassName == null || objectClassName.isEmpty()) {
            MiscUtils.getLogger().warn("Missing objectClassName parameter from user " + 
                loggedInInfo.getLoggedInProviderNo());
            return ERROR;
        }
        
        // Validate objectClassName is a valid enum value
        try {
            ObjectName.valueOf(objectClassName);
        } catch (IllegalArgumentException e) {
            MiscUtils.getLogger().warn("Invalid objectClassName parameter: " + objectClassName + 
                " from user " + loggedInInfo.getLoggedInProviderNo());
            return ERROR;
        }

        // Validate objectId parameter
        if (objectId != null && !objectId.isEmpty()) {
            if (!objectId.matches("^\\d+$")) {
                MiscUtils.getLogger().warn("Invalid objectId parameter: " + objectId + 
                    " from user " + loggedInInfo.getLoggedInProviderNo());
                return ERROR;
            }
            
            try {
                id = Integer.parseInt(objectId);
            } catch (NumberFormatException e) {
                MiscUtils.getLogger().error("Failed to parse object ID: " + objectId, e);
                return ERROR;
            }
        }

        if (activeState != null) {
            active = Boolean.parseBoolean(activeState);
        }

        if (id > 0 && objectClassName != null) {
            try {
                dashboardManager.toggleStatus(loggedInInfo, id, ObjectName.valueOf(objectClassName), active);
            } catch (Exception e) {
                MiscUtils.getLogger().error("Error toggling status for " + objectClassName + " with ID " + id, e);
                return ERROR;
            }
        }

        return null;
    }

    private void setRequest(LoggedInInfo loggedInInfo, HttpServletRequest request) {
        setRequest(loggedInInfo, request, null);
    }

    /**
     * Helper method to set a response object into the request.
     */
    private void setRequest(LoggedInInfo loggedInInfo, HttpServletRequest request, Object message) {

        List<Dashboard> dashboards = dashboardManager.getDashboards(loggedInInfo);
        List<IndicatorTemplate> indicatorTemplates = dashboardManager.getIndicatorLibrary(loggedInInfo);

        if (dashboards != null) {
            request.setAttribute("dashboards", dashboards);
        }

        if (indicatorTemplates != null) {
            request.setAttribute("indicatorTemplates", indicatorTemplates);
        }

        if (message != null) {
            request.setAttribute("message", message);
        }
    }

    private File indicatorTemplateFile;

    public File getIndicatorTemplateFile() {
        return indicatorTemplateFile;
    }

    public void setIndicatorTemplateFile(File indicatorTemplateFile) {
        this.indicatorTemplateFile = indicatorTemplateFile;
    }
}
