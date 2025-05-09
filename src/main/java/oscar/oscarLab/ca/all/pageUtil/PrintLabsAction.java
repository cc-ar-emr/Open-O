/**
 * Copyright (c) 2001-2002. Department of Family Medicine, McMaster University. All Rights Reserved.
 * This software is published under the GPL GNU General Public License.
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. 
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * This software was written for the
 * Department of Family Medicine
 * McMaster University
 * Hamilton
 * Ontario, Canada
 */


/*
 * PrintLabsAction.java
 *
 * Created on November 27, 2007, 9:42 AM
 *
 */

package oscar.oscarLab.ca.all.pageUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.oscarehr.managers.SecurityInfoManager;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.SpringUtils;

import oscar.log.LogAction;
import oscar.log.LogConst;
import oscar.oscarLab.ca.all.parsers.Factory;
import oscar.oscarLab.ca.all.parsers.MessageHandler;

/**
 *
 * @author wrighd
 */
public class PrintLabsAction extends Action{
    
    Logger logger = org.oscarehr.util.MiscUtils.getLogger();
    private SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);
    
    /** Creates a new instance of PrintLabsAction */
    public PrintLabsAction() {
    }
    
    public ActionForward execute(ActionMapping mapping,ActionForm form,HttpServletRequest request,HttpServletResponse response){
        
    	if(!securityInfoManager.hasPrivilege(LoggedInInfo.getLoggedInInfoFromSession(request), "_lab", "r", null)) {
			throw new SecurityException("missing required security object (_lab)");
		}
    	
    	LogAction.addLog((String) request.getSession().getAttribute("user"), LogConst.READ, LogConst.CON_HL7_LAB, request.getParameter("segmentID"), request.getRemoteAddr(),"");
    	 
        try {
            MessageHandler handler = Factory.getHandler(request.getParameter("segmentID"));
            if(handler.getHeaders().get(0).equals("CELLPATHR")){//if it is a VIHA RTF lab
                response.setContentType("text/rtf");  //octet-stream
                response.setHeader("Content-Disposition", "attachment; filename=\""+handler.getPatientName().replaceAll("\\s", "_")+"_LabReport.rtf\"");
                LabPDFCreator pdf = new LabPDFCreator(request, response.getOutputStream());
                pdf.printRtf();
            } else {
	            response.setContentType("application/pdf");  //octet-stream
	            response.setHeader("Content-Disposition", "attachment; filename=\""+handler.getPatientName().replaceAll("\\s", "_")+"_LabReport.pdf\"");
	            
	            //first write to a file
	            File f = File.createTempFile("lab"+request.getParameter("segmentID"),"pdf");
	            FileOutputStream fos = new FileOutputStream(f);
	            LabPDFCreator pdf = new LabPDFCreator(request,fos);
	            pdf.printPdf();
	            pdf.addEmbeddedDocuments(f,response.getOutputStream());
	            
	            f.delete();
            }
        }catch(IOException ioe) {
            logger.error("IOException occurred inside PrintLabsAction", ioe);
            request.setAttribute("printError", new Boolean(true));
            return mapping.findForward("error");
        }catch(Exception e){
            logger.error("Unknown Exception occurred inside PrintLabsAction", e);
            request.setAttribute("printError", new Boolean(true));
            return mapping.findForward("error");
        }
        
        return null;
        
    }
    
    
}
