//CHECKSTYLE:OFF
/**
 * Copyright (c) 2008-2012 Indivica Inc.
 * <p>
 * This software is made available under the terms of the
 * GNU General Public License, Version 2, 1991 (GPLv2).
 * License details are available via "indivica.ca/gplv2"
 * and "gnu.org/licenses/gpl-2.0.html".
 */
package org.oscarehr.olis;

import java.util.TimerTask;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.oscarehr.managers.SecurityInfoManager;
import org.oscarehr.olis.dao.OLISSystemPreferencesDao;
import org.oscarehr.olis.model.OLISSystemPreferences;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SpringUtils;
import org.springframework.scheduling.concurrent.ScheduledExecutorTask;
//import org.springframework.scheduling.timer.ScheduledTimerTask;

import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;

public class OLISPreferences2Action extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();


    private SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);

    @Override
    public String execute() throws Exception {
        if (!securityInfoManager.hasPrivilege(LoggedInInfo.getLoggedInInfoFromSession(request), "_admin", "r", null)) {
            throw new SecurityException("missing required security object (_admin)");
        }

        DateTimeFormatter input = DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss Z");
        DateTimeFormatter output = DateTimeFormat.forPattern("YYYYMMddHHmmssZ");
        DateTime date;
        String startTime = oscar.Misc.getStr(request.getParameter("startTime"), "").trim();
        if (!startTime.equals("")) {
            date = input.parseDateTime(startTime);
            startTime = date.toString(output);
        }
        String endTime = oscar.Misc.getStr(request.getParameter("endTime"), "").trim();
        if (!endTime.equals("")) {
            date = input.parseDateTime(endTime);
            endTime = date.toString(output);
        }

        Integer pollFrequency = oscar.Misc.getInt(request.getParameter("pollFrequency"), 30);
        String filterPatients = request.getParameter("filter_patients");
        OLISSystemPreferencesDao olisPrefDao = (OLISSystemPreferencesDao) SpringUtils.getBean(OLISSystemPreferencesDao.class);
        ;
        OLISSystemPreferences olisPrefs = olisPrefDao.getPreferences();

        try {
            olisPrefs.setStartTime(startTime);
            olisPrefs.setEndTime(endTime);
            olisPrefs.setFilterPatients((filterPatients != null) ? true : false);
            boolean restartTimer = !pollFrequency.equals(olisPrefs.getPollFrequency());
            olisPrefs.setPollFrequency(pollFrequency);
            olisPrefDao.save(olisPrefs);
            request.setAttribute("success", true);

            if (restartTimer) {
	     		/* 	ScheduledTimerTask task = (ScheduledTimerTask)SpringUtils.getBean(ScheduledExecutorTask.class);		
	     			TimerTask tt = task.getTimerTask();
	     			Thread t = new Thread(tt);
	     			t.start();*/
                ScheduledExecutorTask task = (ScheduledExecutorTask) SpringUtils.getBean(ScheduledExecutorTask.class);
                Runnable tt = task.getRunnable();
                Thread t = new Thread(tt);
                t.start();
            }

        } catch (Exception e) {
            MiscUtils.getLogger().error("Changing Preferences failed", e);
            request.setAttribute("success", false);
        }
        return SUCCESS;

    }
}
