//CHECKSTYLE:OFF
/**
 * Copyright (c) 2005-2012. Centre for Research on Inner City Health, St. Michael's Hospital, Toronto. All Rights Reserved.
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
 * This software was written for
 * Centre for Research on Inner City Health, St. Michael's Hospital,
 * Toronto, Ontario, Canada
 */

package org.oscarehr.PMmodule.exporter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SecurityUtils;
import org.oscarehr.util.WebUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Servlet for exporting DATIS data
 */
public class TestServlet extends HttpServlet {

    private static final long serialVersionUID = 1471337270123160719L;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        // Verify user is logged in
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        if (loggedInInfo == null) {
            MiscUtils.getLogger().error("Unauthorized access attempt to TestServlet from IP: " + request.getRemoteAddr());
            try {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
                return;
            } catch (IOException e) {
                throw new ServletException(e);
            }
        }

        // Set security headers
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        
        try {
            // Validate CSRF token for all requests
            if (!WebUtils.isValidCsrfToken(request)) {
                MiscUtils.getLogger().warn("Invalid CSRF token in TestServlet request from provider: " + 
                                         loggedInInfo.getLoggedInProviderNo() + " IP: " + request.getRemoteAddr());
                SecurityUtils.logSecurityEvent("CSRF_VIOLATION", loggedInInfo.getLoggedInProviderNo(), 
                                             "Invalid CSRF token in TestServlet");
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid security token");
                return;
            }

            AbstractIntakeExporter exporter = null;
            
            // Validate file parameter
            String fileParam = request.getParameter("file");
            if (fileParam == null || !StringUtils.isNumeric(fileParam)) {
                MiscUtils.getLogger().warn("Invalid file parameter: " + fileParam);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file parameter");
                return;
            }
            int fileno = Integer.parseInt(fileParam);
            
            // Validate client parameter
            int clientId = 0;
            String clientParam = request.getParameter("c");
            if (clientParam != null) {
                if (!StringUtils.isNumeric(clientParam)) {
                    MiscUtils.getLogger().warn("Invalid client parameter: " + clientParam);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid client parameter");
                    return;
                }
                clientId = Integer.parseInt(clientParam);
                
                // Check if user has permission to access this client
                if (!SecurityUtils.hasAccessToClient(loggedInInfo, clientId)) {
                    MiscUtils.getLogger().warn("Unauthorized access attempt to client: " + clientId + 
                                             " by provider: " + loggedInInfo.getLoggedInProviderNo());
                    SecurityUtils.logSecurityEvent("UNAUTHORIZED_CLIENT_ACCESS", loggedInInfo.getLoggedInProviderNo(), 
                                                 "Attempted to access client: " + clientId);
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied to this client");
                    return;
                }
            }

            // Validate facility parameter
            String facilityParam = request.getParameter("f");
            if (facilityParam == null || !StringUtils.isNumeric(facilityParam)) {
                MiscUtils.getLogger().warn("Invalid facility parameter: " + facilityParam);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid facility parameter");
                return;
            }
            int facilityId = Integer.parseInt(facilityParam);
            
            // Check if user has permission to access this facility
            if (!SecurityUtils.hasAccessToFacility(loggedInInfo, facilityId)) {
                MiscUtils.getLogger().warn("Unauthorized access attempt to facility: " + facilityId + 
                                         " by provider: " + loggedInInfo.getLoggedInProviderNo());
                SecurityUtils.logSecurityEvent("UNAUTHORIZED_FACILITY_ACCESS", loggedInInfo.getLoggedInProviderNo(), 
                                             "Attempted to access facility: " + facilityId);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied to this facility");
                return;
            }

            // Limit file number to valid range
            if (fileno < 1 || fileno > 6) {
                MiscUtils.getLogger().warn("Invalid file number: " + fileno);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file number");
                return;
            }
            
            // Use a whitelist approach for file selection
            switch (fileno) {
                case 1:
                    exporter = (DATISAgencyInformation) WebApplicationContextUtils.getWebApplicationContext(
                        request.getSession().getServletContext()).getBean(DATISAgencyInformation.class);
                    break;
                case 2:
                    exporter = (DATISListOfPrograms) WebApplicationContextUtils.getWebApplicationContext(
                        request.getSession().getServletContext()).getBean(DATISListOfPrograms.class);
                    break;
                case 3:
                    exporter = (DATISMain) WebApplicationContextUtils.getWebApplicationContext(
                        request.getSession().getServletContext()).getBean(DATISMain.class);
                    break;
                case 4:
                    exporter = (DATISProgramInformation) WebApplicationContextUtils.getWebApplicationContext(
                        request.getSession().getServletContext()).getBean(DATISProgramInformation.class);
                    break;
                case 5:
                    exporter = (DATISGamingForm) WebApplicationContextUtils.getWebApplicationContext(
                        request.getSession().getServletContext()).getBean(DATISGamingForm.class);
                    break;
                case 6:
                    exporter = (DATISNonClientService) WebApplicationContextUtils.getWebApplicationContext(
                        request.getSession().getServletContext()).getBean(DATISNonClientService.class);
                    break;
                default:
                    // This should never happen due to the range check above
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file number");
                    return;
            }

            if (clientId != 0) {
                List<Integer> clients = new ArrayList<Integer>();
                clients.add(clientId);
                exporter.setClients(clients);
            }

            exporter.setFacilityId(facilityId);
            
            // Set appropriate content type
            response.setContentType("text/plain;charset=UTF-8");
            
            // Log the export operation
            MiscUtils.getLogger().info("DATIS export performed - file: " + fileno + 
                                     ", client: " + (clientId != 0 ? clientId : "none") + 
                                     ", facility: " + facilityId + 
                                     ", provider: " + loggedInInfo.getLoggedInProviderNo());
            
            response.getWriter().print(exporter.export());
            
        } catch (NumberFormatException e) {
            MiscUtils.getLogger().error("Error parsing parameters", e);
            try {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameter format");
            } catch (IOException ex) {
                throw new ServletException(ex);
            }
        } catch (Exception e) {
            MiscUtils.getLogger().error("Error in TestServlet", e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error occurred");
            } catch (IOException ex) {
                throw new ServletException(ex);
            }
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        // POST requests should be rejected to prevent CSRF attacks
        try {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "POST method not allowed");
        } catch (IOException e) {
            throw new ServletException(e);
        }
    }
}
