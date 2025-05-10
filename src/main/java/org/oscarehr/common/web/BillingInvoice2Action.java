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
package org.oscarehr.common.web;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.oscarehr.common.service.PdfRecordPrinter;
import org.oscarehr.managers.BillingONManager;
import org.oscarehr.managers.SecurityInfoManager;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SpringUtils;

import oscar.util.ConcatPDF;
import oscar.util.UtilDateUtilities;

/**
 * @author mweston4
 */
import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;

public class BillingInvoice2Action extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();

    private SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);

    public String execute() throws Exception {
        String method = request.getParameter("method");
        if ("getPrintPDF".equals(method)) {
            return getPrintPDF();
        } else if ("getListPrintPDF".equals(method)) {
            return getListPrintPDF();
        } else if ("sendEmail".equals(method)) {
            return sendEmail();
        } else if ("sendEmail".equals(method)) {
            return sendEmail();
        }
        return getPrintPDF();
    }

    public String getPrintPDF() throws IOException {
        String invoiceNo = request.getParameter("invoiceNo");
        String actionResult = "failure";

        if (!securityInfoManager.hasPrivilege(LoggedInInfo.getLoggedInInfoFromSession(request), "_billing", "r", null)) {
            throw new SecurityException("missing required security object (_billing)");
        }

        // Validate invoiceNo parameter
        if (invoiceNo != null && invoiceNo.matches("^\\d+$")) {
            try {
                // Set security headers
                response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
                response.setHeader("Pragma", "no-cache");
                response.setHeader("X-Content-Type-Options", "nosniff");
                
                response.setContentType("application/pdf");
                String safeFilename = "BillingInvoice" + invoiceNo + "_" + UtilDateUtilities.getToday("yyyy-MM-dd.hh.mm.ss") + ".pdf";
                response.setHeader("Content-Disposition", "attachment; filename=\"" + safeFilename + "\"");
                
                boolean bResult = processPrintPDF(Integer.parseInt(invoiceNo), request.getLocale(), response.getOutputStream());
                if (bResult) {
                    actionResult = "success";
                }
            } catch (NumberFormatException e) {
                MiscUtils.getLogger().error("Invalid invoice number format: " + invoiceNo, e);
            } catch (Exception e) {
                MiscUtils.getLogger().error("Error generating PDF for invoice: " + invoiceNo, e);
            }
        } else {
            MiscUtils.getLogger().warn("Invalid or missing invoice number parameter");
        }
        
        return actionResult;
    }

    public String getListPrintPDF() throws IOException {

        if (!securityInfoManager.hasPrivilege(LoggedInInfo.getLoggedInInfoFromSession(request), "_billing", "r", null)) {
            throw new SecurityException("missing required security object (_billing)");
        }

        String actionResult = "failure";
        String[] invoiceNos = request.getParameterValues("invoiceAction");

        ArrayList<Object> fileList = new ArrayList<Object>();
        OutputStream fos = null;
        
        // Get and validate INVOICE_DIR
        String invoiceDir = oscar.OscarProperties.getInstance().getProperty("INVOICE_DIR");
        if (invoiceDir == null || invoiceDir.isEmpty()) {
            MiscUtils.getLogger().error("INVOICE_DIR property not set");
            return actionResult;
        }
        
        // Validate directory exists and is writable
        File invoiceDirFile = new File(invoiceDir);
        if (!invoiceDirFile.exists() || !invoiceDirFile.isDirectory() || !invoiceDirFile.canWrite()) {
            MiscUtils.getLogger().error("INVOICE_DIR is not a writable directory: " + invoiceDir);
            return actionResult;
        }
        
        if (invoiceNos != null) {
            for (String invoiceNoStr : invoiceNos) {
                try {
                    // Validate invoice number format
                    if (!invoiceNoStr.matches("^\\d+$")) {
                        MiscUtils.getLogger().warn("Invalid invoice number format: " + invoiceNoStr);
                        continue;
                    }
                    
                    Integer invoiceNo = Integer.parseInt(invoiceNoStr);
                    String timestamp = UtilDateUtilities.getToday("yyyy-MM-dd.hh.mm.ss");
                    String filename = "BillingInvoice" + invoiceNo + "_" + timestamp + ".pdf";
                    
                    // Create a safe file path
                    File outputFile = new File(invoiceDirFile, filename);
                    String savePath = outputFile.getAbsolutePath();
                    
                    // Validate the file path is within the expected directory
                    if (!outputFile.getCanonicalPath().startsWith(invoiceDirFile.getCanonicalPath())) {
                        MiscUtils.getLogger().error("Path traversal attempt detected: " + filename);
                        continue;
                    }
                    
                    fos = new FileOutputStream(savePath);
                    processPrintPDF(invoiceNo, request.getLocale(), fos);
                    fileList.add(savePath);
                } catch (NumberFormatException e) {
                    MiscUtils.getLogger().error("Invalid invoice number: " + invoiceNoStr, e);
                } catch (Exception e) {
                    MiscUtils.getLogger().error("Error processing invoice: " + invoiceNoStr, e);
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            MiscUtils.getLogger().error("Error closing output stream", e);
                        }
                        fos = null;
                    }
                }
            }
        }
        
        if (!fileList.isEmpty()) {
            try {
                // Set security headers
                response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
                response.setHeader("Pragma", "no-cache");
                response.setHeader("X-Content-Type-Options", "nosniff");
                
                response.setContentType("application/pdf");
                String safeFilename = "BillingInvoices_" + UtilDateUtilities.getToday("yyyy-MM-dd.hh.mm.ss") + ".pdf";
                response.setHeader("Content-Disposition", "attachment; filename=\"" + safeFilename + "\"");
                
                ConcatPDF.concat(fileList, response.getOutputStream());
                actionResult = "listSuccess";
                
                // Clean up temporary files
                for (Object filePath : fileList) {
                    try {
                        File file = new File(filePath.toString());
                        if (file.exists() && file.isFile()) {
                            if (!file.delete()) {
                                MiscUtils.getLogger().warn("Failed to delete temporary file: " + filePath);
                            }
                        }
                    } catch (Exception e) {
                        MiscUtils.getLogger().error("Error deleting temporary file: " + filePath, e);
                    }
                }
            } catch (Exception e) {
                MiscUtils.getLogger().error("Error generating combined PDF", e);
            }
        }

        return actionResult;
    }

    /*
     * The sendInvoiceEmailNotification method in BillingManager is no longer supported.
     * For more details, please refer to the sendInvoiceEmailNotification method.
     */
    @Deprecated
    public String sendEmail() {
        throw new UnsupportedOperationException("This method is no longer supported.");
        //  if(!securityInfoManager.hasPrivilege(LoggedInInfo.getLoggedInInfoFromSession(request), "_billing", "w", null)) {
        //  	throw new SecurityException("missing required security object (_billing)");
        //  }

        // String invoiceNoStr = request.getParameter("invoiceNo");
        // Integer invoiceNo = Integer.parseInt(invoiceNoStr);
        // Locale locale = request.getLocale();
        // String actionResult = "failure";

        // if (invoiceNo != null) {
        //     BillingONManager billingManager = (BillingONManager) SpringUtils.getBean(BillingONManager.class);
        //     billingManager.sendInvoiceEmailNotification(invoiceNo, locale);
        //     billingManager.addEmailedBillingComment(invoiceNo, locale); 
        //     actionResult = "success";
        // }

        // ActionRedirect redirect = new ActionRedirect(mapping.findForward(actionResult));
        // redirect.addParameter("billing_no", invoiceNo);
        // return redirect;
    }

    /*
     * The sendInvoiceEmailNotification method in BillingManager is no longer supported.
     * For more details, please refer to the sendInvoiceEmailNotification method.
     */
    @Deprecated
    public String sendListEmail() {
        throw new UnsupportedOperationException("This method is no longer supported.");
        //  if(!securityInfoManager.hasPrivilege(LoggedInInfo.getLoggedInInfoFromSession(request), "_billing", "w", null)) {
        //  	throw new SecurityException("missing required security object (_billing)");
        //  }

        // String actionResult = "failure";       
        // String[] invoiceNos = request.getParameterValues("invoiceAction");
        // Locale locale = request.getLocale();

        // if (invoiceNos != null) {
        //     for (String invoiceNoStr : invoiceNos) {
        //         Integer invoiceNo = Integer.parseInt(invoiceNoStr);
        //         BillingONManager billingManager = (BillingONManager) SpringUtils.getBean(BillingONManager.class);
        //         billingManager.sendInvoiceEmailNotification(invoiceNo, locale);
        //         billingManager.addEmailedBillingComment(invoiceNo, locale);               
        //     }
        //     actionResult = "listSuccess";
        // }

        // return mapping.findForward(actionResult);
    }

    private boolean processPrintPDF(Integer invoiceNo, Locale locale, OutputStream os) {
        boolean bResult = false;

        if (invoiceNo != null && invoiceNo > 0) {
            try {
                // Create PDF of the invoice
                PdfRecordPrinter printer = new PdfRecordPrinter(os);
                printer.printBillingInvoice(invoiceNo, locale);

                BillingONManager billingManager = SpringUtils.getBean(BillingONManager.class);
                billingManager.addPrintedBillingComment(invoiceNo, locale);
                bResult = true;
            } catch (Exception e) {
                MiscUtils.getLogger().error("Error generating PDF for invoice: " + invoiceNo, e);
                bResult = false;
            }
        } else {
            MiscUtils.getLogger().warn("Invalid invoice number: " + invoiceNo);
        }

        return bResult;
    }

}
