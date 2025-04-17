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
package org.caisi.core.web;                                                                                                                                     
                                                                                                                                                                
import com.opensymphony.xwork2.ActionSupport;                                                                                                                   
import org.apache.commons.lang3.StringUtils;                                                                                                                    
import org.apache.logging.log4j.Logger;                                                                                                                         
import org.apache.struts2.ServletActionContext;                                                                                                                 
import org.caisi.service.InfirmBedProgramManager;                                                                                                               
import org.oscarehr.PMmodule.service.AdmissionManager;                                                                                                          
import org.oscarehr.PMmodule.service.ProgramManager;                                                                                                            
import org.oscarehr.common.model.Admission;                                                                                                                     
import org.oscarehr.util.LoggedInInfo;                                                                                                                          
import org.oscarehr.util.MiscUtils;                                                                                                                             
import org.oscarehr.util.SessionConstants;                                                                                                                      
import org.oscarehr.util.SpringUtils;                                                                                                                           
import oscar.OscarProperties;                                                                                                                                   
import oscar.util.LabelValueBean;                                                                                                                               
                                                                                                                                                                
import javax.servlet.http.HttpServletRequest;                                                                                                                   
import javax.servlet.http.HttpServletResponse;                                                                                                                  
import javax.servlet.http.HttpSession;                                                                                                                          
import java.io.IOException;                                                                                                                                     
import java.io.PrintWriter;                                                                                                                                     
import java.util.ArrayList;                                                                                                                                     
import java.util.Date;                                                                                                                                          
import java.util.Iterator;                                                                                                                                      
import java.util.List;                                                                                                                                          
                                                                                                                                                                
public class Infirm2Action extends ActionSupport {                                                                                                              
    private static final long serialVersionUID = 1L;                                                                                                            
                                                                                                                                                                
    private final HttpServletRequest request = ServletActionContext.getRequest();                                                                               
    private final HttpServletResponse response = ServletActionContext.getResponse();                                                                            
                                                                                                                                                                
    private static final Logger logger = MiscUtils.getLogger();                                                                                                 
    private static final InfirmBedProgramManager bpm = SpringUtils.getBean(InfirmBedProgramManager.class);                                                      
    private final ProgramManager pm = SpringUtils.getBean(ProgramManager.class);                                                                                
    private final AdmissionManager mgr = SpringUtils.getBean(AdmissionManager.class);                                                                           
                                                                                                                                                                
    public static void updateCurrentProgram(String programId, String providerNo) {                                                                              
        if (StringUtils.isBlank(programId) || StringUtils.isBlank(providerNo)) {                                                                                
            return;                                                                                                                                             
        }                                                                                                                                                       
                                                                                                                                                                
        try {                                                                                                                                                   
            int programIdInt = Integer.parseInt(programId);                                                                                                     
            logger.debug("updating the current program to " + programId);                                                                                       
            bpm.setDefaultProgramId(providerNo, programIdInt);                                                                                                  
        } catch (NumberFormatException e) {                                                                                                                     
            logger.error("Error parsing programId to set a user's default program", e);                                                                         
        }                                                                                                                                                       
    }                                                                                                                                                           
                                                                                                                                                                
    @Override                                                                                                                                                   
    public String execute() throws Exception {                                                                                                                  
        String action = request.getParameter("action");                                                                                                         
        if ("getProgramList".equals(action)) {                                                                                                                  
            return getProgramList();                                                                                                                            
        } else if ("getSig".equals(action)) {                                                                                                                   
            return getSig();                                                                                                                                    
        } else if ("toggleSig".equals(action)) {                                                                                                                
            return toggleSig();                                                                                                                                 
        }                                                                                                                                                       
        return showProgram();                                                                                                                                   
    }                                                                                                                                                           
                                                                                                                                                                
    public String showProgram() throws Exception {                                                                                                              
        logger.debug("====> inside showProgram action.");                                                                                                       
                                                                                                                                                                
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);                                                                           
        if (loggedInInfo == null) {                                                                                                                             
            logger.error("No logged in info found in session");                                                                                                 
            return null;                                                                                                                                        
        }                                                                                                                                                       
                                                                                                                                                                
        HttpSession se = request.getSession();                                                                                                                  
        se.setAttribute("infirmaryView_initflag", "true");                                                                                                      
        String providerNo = (String) se.getAttribute("user");                                                                                                   
        String archiveView = (String) request.getSession().getAttribute("archiveView");                                                                         
                                                                                                                                                                
        Integer facilityId = null;                                                                                                                              
                                                                                                                                                                
        // facility filtering                                                                                                                                   
        if (OscarProperties.getInstance().getBooleanProperty("FILTER_ON_FACILITY", "true")) {                                                                   
            facilityId = loggedInInfo.getCurrentFacility().getId();                                                                                             
        }                                                                                                                                                       
                                                                                                                                                                
        List<LabelValueBean> programBeans = bpm.getProgramBeans(providerNo, facilityId);                                                                        
                                                                                                                                                                
        //this one seems to be for the caisi case management page, and caseload screens                                                                         
        se.setAttribute("infirmaryView_programBeans", programBeans);                                                                                            
                                                                                                                                                                
        //set default program                                                                                                                                   
        int defaultProgramId = bpm.getDefaultProgramId(providerNo);                                                                                             
        boolean defaultInList = false;                                                                                                                          
                                                                                                                                                                
        for (LabelValueBean programBean : programBeans) {                                                                                                       
            try {                                                                                                                                               
                int id = Integer.parseInt(programBean.getValue());                                                                                              
                if (defaultProgramId == id) {                                                                                                                   
                    defaultInList = true;                                                                                                                       
                    break;                                                                                                                                      
                }                                                                                                                                               
            } catch (NumberFormatException e) {                                                                                                                 
                logger.error("Invalid program ID format", e);                                                                                                   
            }                                                                                                                                                   
        }                                                                                                                                                       
                                                                                                                                                                
        if (!defaultInList) {                                                                                                                                   
            defaultProgramId = 0;                                                                                                                               
        }                                                                                                                                                       
                                                                                                                                                                
        int oriProgramId = 0;                                                                                                                                   
        if (!programBeans.isEmpty()) {                                                                                                                          
            try {                                                                                                                                               
                oriProgramId = Integer.parseInt(programBeans.get(0).getValue());                                                                                
            } catch (NumberFormatException e) {                                                                                                                 
                logger.error("Invalid program ID format in first program bean", e);                                                                             
            }                                                                                                                                                   
        }                                                                                                                                                       
                                                                                                                                                                
        int programId = 0;                                                                                                                                      
        if (defaultProgramId != 0 && oriProgramId != 0) {                                                                                                       
            programId = defaultProgramId;                                                                                                                       
        } else {                                                                                                                                                
            if (oriProgramId == 0) {                                                                                                                            
                programId = 0;                                                                                                                                  
            }                                                                                                                                                   
            if (defaultProgramId == 0 && oriProgramId != 0) {                                                                                                   
                programId = oriProgramId;                                                                                                                       
            }                                                                                                                                                   
        }                                                                                                                                                       
                                                                                                                                                                
        // Override with session value if present                                                                                                               
        String sessionProgramId = (String) se.getAttribute(SessionConstants.CURRENT_PROGRAM_ID);                                                                
        if (sessionProgramId != null) {                                                                                                                         
            try {                                                                                                                                               
                programId = Integer.parseInt(sessionProgramId);                                                                                                 
            } catch (NumberFormatException e) {                                                                                                                 
                logger.error("Invalid program ID format in session", e);                                                                                        
            }                                                                                                                                                   
        }                                                                                                                                                       
                                                                                                                                                                
        se.setAttribute(SessionConstants.CURRENT_PROGRAM_ID, String.valueOf(programId));                                                                        
                                                                                                                                                                
        updateCurrentProgram(String.valueOf(programId), loggedInInfo.getLoggedInProviderNo());                                                                  
                                                                                                                                                                
        if (programId != 0) {                                                                                                                                   
            se.setAttribute("case_program_id", String.valueOf(programId));                                                                                      
        }                                                                                                                                                       
                                                                                                                                                                
        if (programId != 0) {                                                                                                                                   
            se.setAttribute("program_client_statuses", pm.getProgramClientStatuses(programId));                                                                 
        }                                                                                                                                                       
                                                                                                                                                                
        String[] programInfo = bpm.getProgramInformation(programId);                                                                                            
        if (programInfo[0] != null) {                                                                                                                           
            se.setAttribute("infirmaryView_programAddress", programInfo[0].replaceAll("\\n", "<br>"));                                                          
        } else {                                                                                                                                                
            se.setAttribute("infirmaryView_programAddress", "");                                                                                                
        }                                                                                                                                                       
        if (programInfo[1] != null) {                                                                                                                           
            se.setAttribute("infirmaryView_programTel", programInfo[1]);                                                                                        
        } else {                                                                                                                                                
            se.setAttribute("infirmaryView_programTel", "");                                                                                                    
        }                                                                                                                                                       
        if (programInfo[2] != null) {                                                                                                                           
            se.setAttribute("infirmaryView_programFax", programInfo[2]);                                                                                        
        } else {                                                                                                                                                
            se.setAttribute("infirmaryView_programFax", "");                                                                                                    
        }                                                                                                                                                       
                                                                                                                                                                
        Date dt;                                                                                                                                                
                                                                                                                                                                
        if (se.getAttribute("infirmaryView_date") != null) {                                                                                                    
            dt = (Date) se.getAttribute("infirmaryView_date");                                                                                                  
        } else {                                                                                                                                                
            dt = new Date();                                                                                                                                    
        }                                                                                                                                                       
                                                                                                                                                                
        //lets not load the demographic beans when in appointment view..no point                                                                                
        org.oscarehr.PMmodule.model.Program p = this.pm.getProgram(programId);                                                                                  
        if (p != null) {                                                                                                                                        
            String exclusiveView = p.getExclusiveView();                                                                                                        
                                                                                                                                                                
            if (exclusiveView != null && exclusiveView.equals("appointment")) {                                                                                 
                return null;                                                                                                                                    
            }                                                                                                                                                   
        }                                                                                                                                                       
                                                                                                                                                                
        List<LabelValueBean> demographicBeans = bpm.getDemographicByBedProgramIdBeans(programId, dt, archiveView);                                              
        List<LabelValueBean> filteredDemographicBeans = new ArrayList<>();                                                                                      
                                                                                                                                                                
        String clientStatusId = request.getParameter("infirmaryView_clientStatusId");                                                                           
        if (clientStatusId != null) {                                                                                                                           
            if ("0".equals(clientStatusId)) {                                                                                                                   
                filteredDemographicBeans = demographicBeans;                                                                                                    
            } else {                                                                                                                                            
                for (LabelValueBean bean : demographicBeans) {                                                                                                  
                    String demographicNo = bean.getValue();                                                                                                     
                    if (StringUtils.isBlank(demographicNo)) {                                                                                                   
                        continue;                                                                                                                               
                    }                                                                                                                                           
                                                                                                                                                                
                    try {                                                                                                                                       
                        Integer demoNo = Integer.parseInt(demographicNo);                                                                                       
                        String programIdStr = String.valueOf(programId);                                                                                        
                                                                                                                                                                
                        if ("true".equals(archiveView)) {                                                                                                       
                            List<Admission> admissions = mgr.getAdmissions_archiveView(programIdStr, demoNo);                                                   
                            for (Admission admission : admissions) {                                                                                            
                                Integer csi = admission.getClientStatusId();                                                                                    
                                if (csi == null) {                                                                                                              
                                    csi = 0;                                                                                                                    
                                }                                                                                                                               
                                if (clientStatusId.equals(csi.toString())) {                                                                                    
                                    filteredDemographicBeans.add(bean);                                                                                         
                                    break;                                                                                                                      
                                }                                                                                                                               
                            }                                                                                                                                   
                        } else {                                                                                                                                
                            Admission admission = mgr.getCurrentAdmission(programIdStr, demoNo);                                                                
                            if (admission != null) {                                                                                                            
                                Integer csi = admission.getClientStatusId();                                                                                    
                                if (csi == null) {                                                                                                              
                                    csi = 0;                                                                                                                    
                                }                                                                                                                               
                                if (clientStatusId.equals(csi.toString())) {                                                                                    
                                    filteredDemographicBeans.add(bean);                                                                                         
                                }                                                                                                                               
                            }                                                                                                                                   
                        }                                                                                                                                       
                    } catch (NumberFormatException e) {                                                                                                         
                        logger.error("Invalid demographic number format", e);                                                                                   
                    }                                                                                                                                           
                }                                                                                                                                               
                request.setAttribute("infirmaryView_clientStatusId", clientStatusId);                                                                           
            }                                                                                                                                                   
        } else {                                                                                                                                                
            filteredDemographicBeans = demographicBeans;                                                                                                        
        }                                                                                                                                                       
                                                                                                                                                                
        se.setAttribute("infirmaryView_demographicBeans", filteredDemographicBeans);                                                                            
                                                                                                                                                                
        try {                                                                                                                                                   
            response.getOutputStream().print(" ");                                                                                                              
        } catch (IOException e) {                                                                                                                               
            logger.error("Error writing to output stream", e);                                                                                                  
        }                                                                                                                                                       
                                                                                                                                                                
        return null;                                                                                                                                            
    }                                                                                                                                                           
                                                                                                                                                                
    public static void loadProgram(HttpServletRequest request, HttpServletResponse response) {                                                                  
        try {                                                                                                                                                   
            logger.debug("====> inside loadProgram action.");                                                                                                   
                                                                                                                                                                
            LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);                                                                       
            if (loggedInInfo == null) {                                                                                                                         
                logger.error("No logged in info found in session");                                                                                             
                return;                                                                                                                                         
            }                                                                                                                                                   
                                                                                                                                                                
            HttpSession se = request.getSession();                                                                                                              
            se.setAttribute("infirmaryView_initflag", "true");                                                                                                  
            String providerNo = (String) se.getAttribute("user");                                                                                               
            String archiveView = (String) request.getSession().getAttribute("archiveView");                                                                     
                                                                                                                                                                
            Integer facilityId = null;                                                                                                                          
                                                                                                                                                                
            // facility filtering                                                                                                                               
            if (OscarProperties.getInstance().getBooleanProperty("FILTER_ON_FACILITY", "true")) {                                                               
                facilityId = loggedInInfo.getCurrentFacility().getId();                                                                                         
            }                                                                                                                                                   
                                                                                                                                                                
            List<LabelValueBean> programBeans = bpm.getProgramBeans(providerNo, facilityId);                                                                    
                                                                                                                                                                
            //this one seems to be for the caisi case management page, and caseload screens                                                                     
            se.setAttribute("infirmaryView_programBeans", programBeans);                                                                                        
                                                                                                                                                                
            //set default program                                                                                                                               
            int defaultProgramId = bpm.getDefaultProgramId(providerNo);                                                                                         
            boolean defaultInList = false;                                                                                                                      
                                                                                                                                                                
            for (LabelValueBean programBean : programBeans) {                                                                                                   
                try {                                                                                                                                           
                    int id = Integer.parseInt(programBean.getValue());                                                                                          
                    if (defaultProgramId == id) {                                                                                                               
                        defaultInList = true;                                                                                                                   
                        break;                                                                                                                                  
                    }                                                                                                                                           
                } catch (NumberFormatException e) {                                                                                                             
                    logger.error("Invalid program ID format", e);                                                                                               
                }                                                                                                                                               
            }                                                                                                                                                   
                                                                                                                                                                
            if (!defaultInList) {                                                                                                                               
                defaultProgramId = 0;                                                                                                                           
            }                                                                                                                                                   
                                                                                                                                                                
            int oriProgramId = 0;                                                                                                                               
            if (!programBeans.isEmpty()) {                                                                                                                      
                try {                                                                                                                                           
                    oriProgramId = Integer.parseInt(programBeans.get(0).getValue());                                                                            
                } catch (NumberFormatException e) {                                                                                                             
                    logger.error("Invalid program ID format in first program bean", e);                                                                         
                }                                                                                                                                               
            }                                                                                                                                                   
                                                                                                                                                                
            int programId = 0;                                                                                                                                  
            if (defaultProgramId != 0 && oriProgramId != 0) {                                                                                                   
                programId = defaultProgramId;                                                                                                                   
            } else {                                                                                                                                            
                if (oriProgramId == 0) {                                                                                                                        
                    programId = 0;                                                                                                                              
                }                                                                                                                                               
                if (defaultProgramId == 0 && oriProgramId != 0) {                                                                                               
                    programId = oriProgramId;                                                                                                                   
                }                                                                                                                                               
            }                                                                                                                                                   
                                                                                                                                                                
            // Override with session value if present                                                                                                           
            String sessionProgramId = (String) se.getAttribute(SessionConstants.CURRENT_PROGRAM_ID);                                                            
            if (sessionProgramId != null) {                                                                                                                     
                try {                                                                                                                                           
                    programId = Integer.parseInt(sessionProgramId);                                                                                             
                } catch (NumberFormatException e) {                                                                                                             
                    logger.error("Invalid program ID format in session", e);                                                                                    
                }                                                                                                                                               
            }                                                                                                                                                   
                                                                                                                                                                
            se.setAttribute(SessionConstants.CURRENT_PROGRAM_ID, String.valueOf(programId));                                                                    
                                                                                                                                                                
            updateCurrentProgram(String.valueOf(programId), loggedInInfo.getLoggedInProviderNo());                                                              
                                                                                                                                                                
            ProgramManager pm = SpringUtils.getBean(ProgramManager.class);                                                                                      
            AdmissionManager mgr = SpringUtils.getBean(AdmissionManager.class);                                                                                 
                                                                                                                                                                
            if (programId != 0) {                                                                                                                               
                se.setAttribute("case_program_id", String.valueOf(programId));                                                                                  
            }                                                                                                                                                   
                                                                                                                                                                
            if (programId != 0) {                                                                                                                               
                se.setAttribute("program_client_statuses", pm.getProgramClientStatuses(programId));                                                             
            }                                                                                                                                                   
                                                                                                                                                                
            String[] programInfo = bpm.getProgramInformation(programId);                                                                                        
            if (programInfo[0] != null) {                                                                                                                       
                se.setAttribute("infirmaryView_programAddress", programInfo[0].replaceAll("\\n", "<br>"));                                                      
            } else {                                                                                                                                            
                se.setAttribute("infirmaryView_programAddress", "");                                                                                            
            }                                                                                                                                                   
            if (programInfo[1] != null) {                                                                                                                       
                se.setAttribute("infirmaryView_programTel", programInfo[1]);                                                                                    
            } else {                                                                                                                                            
                se.setAttribute("infirmaryView_programTel", "");                                                                                                
            }                                                                                                                                                   
            if (programInfo[2] != null) {                                                                                                                       
                se.setAttribute("infirmaryView_programFax", programInfo[2]);                                                                                    
            } else {                                                                                                                                            
                se.setAttribute("infirmaryView_programFax", "");                                                                                                
            }                                                                                                                                                   
                                                                                                                                                                
            Date dt;                                                                                                                                            
                                                                                                                                                                
            if (se.getAttribute("infirmaryView_date") != null) {                                                                                                
                dt = (Date) se.getAttribute("infirmaryView_date");                                                                                              
            } else {                                                                                                                                            
                dt = new Date();                                                                                                                                
            }                                                                                                                                                   
                                                                                                                                                                
            //lets not load the demographic beans when in appointment view..no point                                                                            
            org.oscarehr.PMmodule.model.Program p = pm.getProgram(programId);                                                                                   
            if (p != null) {                                                                                                                                    
                String exclusiveView = p.getExclusiveView();                                                                                                    
                                                                                                                                                                
                if (exclusiveView != null && exclusiveView.equals("appointment")) {                                                                             
                    return;                                                                                                                                     
                }                                                                                                                                               
            }                                                                                                                                                   
                                                                                                                                                                
            List<LabelValueBean> demographicBeans = bpm.getDemographicByBedProgramIdBeans(programId, dt, archiveView);                                          
            List<LabelValueBean> filteredDemographicBeans = new ArrayList<>();                                                                                  
                                                                                                                                                                
            String clientStatusId = request.getParameter("infirmaryView_clientStatusId");                                                                       
            if (clientStatusId != null) {                                                                                                                       
                if ("0".equals(clientStatusId)) {                                                                                                               
                    filteredDemographicBeans = demographicBeans;                                                                                                
                } else {                                                                                                                                        
                    for (LabelValueBean bean : demographicBeans) {                                                                                              
                        String demographicNo = bean.getValue();                                                                                                 
                        if (StringUtils.isBlank(demographicNo)) {                                                                                               
                            continue;                                                                                                                           
                        }                                                                                                                                       
                                                                                                                                                                
                        try {                                                                                                                                   
                            Integer demoNo = Integer.parseInt(demographicNo);                                                                                   
                            String programIdStr = String.valueOf(programId);                                                                                    
                                                                                                                                                                
                            if ("true".equals(archiveView)) {                                                                                                   
                                List<Admission> admissions = mgr.getAdmissions_archiveView(programIdStr, demoNo);                                               
                                for (Admission admission : admissions) {                                                                                        
                                    Integer csi = admission.getClientStatusId();                                                                                
                                    if (csi == null) {                                                                                                          
                                        csi = 0;                                                                                                                
                                    }                                                                                                                           
                                    if (clientStatusId.equals(csi.toString())) {                                                                                
                                        filteredDemographicBeans.add(bean);                                                                                     
                                        break;                                                                                                                  
                                    }                                                                                                                           
                                }                                                                                                                               
                            } else {                                                                                                                            
                                Admission admission = mgr.getCurrentAdmission(programIdStr, demoNo);                                                            
                                if (admission != null) {                                                                                                        
                                    Integer csi = admission.getClientStatusId();                                                                                
                                    if (csi == null) {                                                                                                          
                                        csi = 0;                                                                                                                
                                    }                                                                                                                           
                                    if (clientStatusId.equals(csi.toString())) {                                                                                
                                        filteredDemographicBeans.add(bean);                                                                                     
                                    }                                                                                                                           
                                }                                                                                                                               
                            }                                                                                                                                   
                        } catch (NumberFormatException e) {                                                                                                     
                            logger.error("Invalid demographic number format", e);                                                                               
                        }                                                                                                                                       
                    }                                                                                                                                           
                    request.setAttribute("infirmaryView_clientStatusId", clientStatusId);                                                                       
                }                                                                                                                                               
            } else {                                                                                                                                            
                filteredDemographicBeans = demographicBeans;                                                                                                    
            }                                                                                                                                                   
                                                                                                                                                                
            se.setAttribute("infirmaryView_demographicBeans", filteredDemographicBeans);                                                                        
        } catch (Exception e) {                                                                                                                                 
            logger.error("Error in loadProgram", e);                                                                                                            
        }                                                                                                                                                       
    }                                                                                                                                                           
                                                                                                                                                                
    public String getSig() {                                                                                                                                    
        logger.debug("====> inside getSig action.");                                                                                                            
        String providerNo = request.getParameter("providerNo");                                                                                                 
        if (providerNo == null) {                                                                                                                               
            providerNo = (String) request.getSession().getAttribute("user");                                                                                    
        }                                                                                                                                                       
                                                                                                                                                                
        if (StringUtils.isNotBlank(providerNo)) {                                                                                                               
            Boolean onsig = bpm.getProviderSig(providerNo);                                                                                                     
            request.getSession().setAttribute("signOnNote", onsig);                                                                                             
            int pid = bpm.getDefaultProgramId(providerNo);                                                                                                      
                                                                                                                                                                
            request.getSession().setAttribute("programId_oscarView", "0");                                                                                      
                                                                                                                                                                
            String ppid = (String) request.getSession().getAttribute("case_program_id");                                                                        
            if (ppid == null) {                                                                                                                                 
                request.getSession().setAttribute("case_program_id", String.valueOf(pid));                                                                      
            } else {                                                                                                                                            
                request.getSession().setAttribute("case_program_id", ppid);                                                                                     
            }                                                                                                                                                   
        }                                                                                                                                                       
                                                                                                                                                                
        return null;                                                                                                                                            
    }                                                                                                                                                           
                                                                                                                                                                
    public String toggleSig() {                                                                                                                                 
        logger.debug("====> inside toggleSig action.");                                                                                                         
        String providerNo = request.getParameter("providerNo");                                                                                                 
        if (providerNo == null) {                                                                                                                               
            providerNo = (String) request.getSession().getAttribute("user");                                                                                    
        }                                                                                                                                                       
                                                                                                                                                                
        if (StringUtils.isNotBlank(providerNo)) {                                                                                                               
            Boolean onsig = bpm.getProviderSig(providerNo);                                                                                                     
            request.getSession().setAttribute("signOnNote", onsig);                                                                                             
        }                                                                                                                                                       
                                                                                                                                                                
        return null;                                                                                                                                            
    }                                                                                                                                                           
                                                                                                                                                                
    public String getProgramList() {                                                                                                                            
        String providerNo = request.getParameter("providerNo");                                                                                                 
        String facilityId = request.getParameter("facilityId");                                                                                                 
                                                                                                                                                                
        StringBuilder outTxt = new StringBuilder("<option value='all'>All Programs</option>");                                                                  
        PrintWriter out;                                                                                                                                        
        try {                                                                                                                                                   
            response.setContentType("text/html;charset=UTF-8");                                                                                                 
            out = response.getWriter();                                                                                                                         
            out.print(outTxt.toString());                                                                                                                       
        } catch (IOException e) {                                                                                                                               
            logger.error("Error writing to output stream", e);                                                                                                  
            return null;                                                                                                                                        
        }                                                                                                                                                       
                                                                                                                                                                
        if (StringUtils.isBlank(providerNo) || StringUtils.isBlank(facilityId)) {                                                                               
            return null;                                                                                                                                        
        }                                                                                                                                                       
                                                                                                                                                                
        int facilityIdInt = 1;                                                                                                                                  
        try {                                                                                                                                                   
            facilityIdInt = Integer.parseInt(facilityId);                                                                                                       
        } catch (NumberFormatException e) {                                                                                                                     
            logger.error("Invalid facility ID format", e);                                                                                                      
            return null;                                                                                                                                        
        }                                                                                                                                                       
                                                                                                                                                                
        List<LabelValueBean> programBeans;                                                                                                                      
        if ("all".equalsIgnoreCase(providerNo)) {                                                                                                               
            programBeans = bpm.getProgramBeansByFacilityId(facilityIdInt);                                                                                      
        } else {                                                                                                                                                
            programBeans = bpm.getProgramBeans(providerNo, facilityIdInt);                                                                                      
        }                                                                                                                                                       
                                                                                                                                                                
        for (LabelValueBean pb : programBeans) {                                                                                                                
            String value = StringUtils.defaultString(pb.getValue());                                                                                            
            String label = StringUtils.defaultString(pb.getLabel());                                                                                            
            // Escape HTML to prevent XSS                                                                                                                       
            label = label.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")                                                                       
                    .replace("\"", "&quot;").replace("'", "&#39;");                                                                                             
            out.print(String.format("<option value='%s'>%s</option>", value, label));                                                                           
        }                                                                                                                                                       
                                                                                                                                                                
        return null;                                                                                                                                            
    }                                                                                                                                                           
}   