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

package org.oscarehr.phr.web;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Calendar;

import org.oscarehr.PMmodule.caisi_integrator.ConformanceTestHelper;

import org.oscarehr.util.DateUtils;
import org.oscarehr.util.LoggedInInfo;

import org.oscarehr.documentManager.EDoc;
import org.oscarehr.documentManager.EDocUtil;
import org.oscarehr.documentManager.actions.AddEditDocument2Action;
import oscar.log.LogAction;
import oscar.log.LogConst;

public class PHRMessageAction {
    public static void saveAttachmentToEchartDocuments(LoggedInInfo loggedInInfo, Integer demographicNo, String messageSubject, Calendar messageSentDate, String filename, String mimeType, byte[] fileBytes) throws Exception {
        String description = "Attachment : " + messageSubject;

        String date = DateUtils.getIsoDate(messageSentDate);
        date = date.replaceAll("-", "/");

        EDoc newDoc = new EDoc(description, "others", filename, "", loggedInInfo.getLoggedInProviderNo(), "", "", 'A', date, "", "", "demographic", demographicNo.toString());

        // new file name with date attached
        String fileName2 = newDoc.getFileName();

        // save local file
        ByteArrayInputStream bais = new ByteArrayInputStream(fileBytes);
        File file = AddEditDocument2Action.writeLocalFile(bais, fileName2);

        newDoc.setContentType(mimeType);
        if ("application/pdf".equals(mimeType)) {
            int numberOfPages = AddEditDocument2Action.countNumOfPages(fileName2);
            newDoc.setNumberOfPages(numberOfPages);
        }

        String doc_no = EDocUtil.addDocumentSQL(newDoc);
        if (ConformanceTestHelper.enableConformanceOnlyTestFeatures) {
            AddEditDocument2Action.storeDocumentInDatabase(file, Integer.parseInt(doc_no));
        }
        LogAction.addLog(loggedInInfo.getLoggedInProviderNo(), LogConst.ADD, LogConst.CON_DOCUMENT, doc_no);
    }
}
