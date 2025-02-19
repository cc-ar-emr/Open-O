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


/*
 * InsideLabUpload2Action.java
 *
 * Created on June 28, 2007, 1:11 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package oscar.oscarLab.ca.all.pageUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload;
import org.oscarehr.managers.SecurityInfoManager;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SpringUtils;

import oscar.oscarLab.FileUploadCheck;
import oscar.oscarLab.ca.all.upload.HandlerClassFactory;
import oscar.oscarLab.ca.all.upload.handlers.MessageHandler;
import oscar.oscarLab.ca.all.util.Utilities;

import org.apache.struts2.ActionSupport;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.ActionContext;

public class InsideLabUpload2Action extends ActionSupport {
    ActionContext context = ActionContext.getContext();
    HttpServletRequest request = (HttpServletRequest) context.get(ServletActionContext.HTTP_REQUEST);
    HttpServletResponse response = (HttpServletResponse) context.get(ServletActionContext.HTTP_RESPONSE);

    private enum FileStatus {
        COMPLETED,
        FAILED,
        INVALID,
        EXISTS
    }

    @Override
    public String execute() {
        String success = SUCCESS;
        if (!JakartaServletFileUpload.isMultipartContent(request)) {
            return success;
        }

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        checkUserPrivilege(loggedInInfo);

        List<FileItem> fileItems = getFiles(request);
        if (fileItems == null) {
            return success;
        }
        Map<String, FileStatus> filesStatusMap = processFiles(loggedInInfo, request, fileItems);
        request.setAttribute("filesStatusMap", filesStatusMap);

        return success;
    }

    private void checkUserPrivilege(LoggedInInfo loggedInInfo) {
        SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);
        if (!securityInfoManager.hasPrivilege(loggedInInfo, "_lab", "w", null)) {
            throw new SecurityException("missing required security object (_lab)");
        }
    }

    private List<FileItem> getFiles(HttpServletRequest request) {
        DiskFileItemFactory diskFileItemFactory = DiskFileItemFactory.builder().setPath(System.getProperty("java.io.tmpdir")).get();
        JakartaServletFileUpload servletFileUpload = new JakartaServletFileUpload(diskFileItemFactory);

        try {
            List<FileItem> fileItems = servletFileUpload.parseRequest(request);
            List<FileItem> filteredFileItems = new ArrayList<>();

            for (FileItem fileItem : fileItems) {
                if (fileItem.getFieldName().equals("importFiles")) {
                    filteredFileItems.add(fileItem);
                }
            }
            return filteredFileItems;
        } catch (FileUploadException e) {
            MiscUtils.getLogger().error("Error occurred while uploading HL7 labs", e);
            return null;
        }
    }

    private Map<String, FileStatus> processFiles(LoggedInInfo loggedInInfo, HttpServletRequest request, List<FileItem> fileItems) {
        Map<String, FileStatus> filesStatusMap = new HashMap<>();
        String fileType = getFileType(request);

        for (FileItem fileItem : fileItems) {
            String fileName = fileItem.getName();
            String filePath = saveFile(fileItem);
            if (filePath == null) {
                filesStatusMap.put(fileName, FileStatus.FAILED);
                break;
            }
            FileStatus fileStatus = processFile(loggedInInfo, request, filePath, fileType);
            filesStatusMap.put(fileName, fileStatus);
        }

        return filesStatusMap;
    }

    private String getFileType(HttpServletRequest request) {
        String fileType = request.getParameter("type");
        String otherFileType = request.getParameter("otherType");
        if (fileType != null && !fileType.equals("OTHER")) {
            return fileType;
        }
        if (otherFileType != null) {
            return otherFileType;
        }

        return null;
    }

    private String saveFile(FileItem fileItem) {
        try (InputStream formFileInputStream = fileItem.getInputStream()) {
            return Utilities.saveFile(formFileInputStream, fileItem.getName());
        } catch (IOException e) {
            MiscUtils.getLogger().error("Error occurred while saving " + fileItem.getName() + " file", e);
            return null;
        }
    }

    private FileStatus processFile(LoggedInInfo loggedInInfo, HttpServletRequest request, String filePath, String fileType) {
        Path path = Paths.get(filePath);
        String fileName = path.getFileName().toString();
        int checkFileUploadedSuccessfully;

        try (InputStream localFileInputStream = Files.newInputStream(path)) {
            String providerNumber = (String) request.getSession().getAttribute("user");
            checkFileUploadedSuccessfully = FileUploadCheck.addFile(fileName, localFileInputStream, providerNumber);
            if (checkFileUploadedSuccessfully == FileUploadCheck.UNSUCCESSFUL_SAVE) {
                return FileStatus.EXISTS;
            }
        } catch (IOException e) {
            MiscUtils.getLogger().error("Error occurred while processing " + fileName + " file", e);
            return FileStatus.FAILED;
        }

        MessageHandler msgHandler = HandlerClassFactory.getHandler(fileType);
        if ((msgHandler.parse(loggedInInfo, getClass().getSimpleName(), filePath, checkFileUploadedSuccessfully, request.getRemoteAddr())) != null) {
            return FileStatus.COMPLETED;
        }
        return FileStatus.INVALID;
    }
}
