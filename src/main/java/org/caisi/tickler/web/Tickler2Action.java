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

package org.caisi.tickler.web;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import org.caisi.service.DemographicManagerTickler;
import org.caisi.tickler.prepared.PreparedTickler;
import org.caisi.tickler.prepared.PreparedTicklerManager;
import org.oscarehr.PMmodule.model.Program;
import org.oscarehr.PMmodule.service.ProgramManager;
import org.oscarehr.PMmodule.service.ProviderManager;
import org.oscarehr.common.dao.EChartDao;
import org.oscarehr.common.model.CustomFilter;
import org.oscarehr.common.model.Demographic;
import org.oscarehr.common.model.EChart;
import org.oscarehr.common.model.Provider;
import org.oscarehr.common.model.Tickler;
import org.oscarehr.common.model.TicklerLink;
import org.oscarehr.managers.SecurityInfoManager;
import org.oscarehr.managers.TicklerManager;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SessionConstants;
import org.oscarehr.util.SpringUtils;

import oscar.OscarProperties;

import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;

public class Tickler2Action extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();

    private static Logger log = MiscUtils.getLogger();
    private TicklerManager ticklerManager = SpringUtils.getBean(TicklerManager.class);
    private ProviderManager providerMgr = SpringUtils.getBean(ProviderManager.class);
    private PreparedTicklerManager preparedTicklerMgr = SpringUtils.getBean(PreparedTicklerManager.class);
    private DemographicManagerTickler demographicMgr = SpringUtils.getBean(DemographicManagerTickler.class);
    private EChartDao echartDao = SpringUtils.getBean(EChartDao.class);
    private ProgramManager programMgr = SpringUtils.getBean(ProgramManager.class);

    private SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);

    public void setDemographicManager(DemographicManagerTickler demographicManager) {
        this.demographicMgr = demographicManager;
    }

    public void setProviderManager(ProviderManager providerMgr) {
        this.providerMgr = providerMgr;
    }

    public void setPreparedTicklerManager(PreparedTicklerManager preparedTicklerMgr) {
        this.preparedTicklerMgr = preparedTicklerMgr;
    }

    public void setProgramManager(ProgramManager programMgr) {
        this.programMgr = programMgr;
    }

    String getProviderNo(HttpServletRequest request) {
        return (String) request.getSession().getAttribute("user");
    }

    /* default to 'list' */
    public String execute() throws Exception {
        String method = request.getParameter("method");
        if ("view".equals(method)) {
            return view();
        } else if ("my_tickler_filter".equals(method)) {
            return my_tickler_filter();
        } else if ("run_custom_filter".equals(method)) {
            return run_custom_filter();
        } else if ("reassign".equals(method)) {
            return reassign();
        } else if ("delete".equals(method)) {
            return delete();
        } else if ("add_comment".equals(method)) {
            return add_comment();
        } else if ("complete".equals(method)) {
            return complete();
        } else if ("edit".equals(method)) {
            return edit();
        } else if ("save".equals(method)) {
            return save();
        } else if ("prepared_tickler_list".equals(method)) {
            return prepared_tickler_list();
        } else if ("prepared_tickler_edit".equals(method)) {
            return prepared_tickler_edit();
        } else if ("update_status".equals(method)) {
            return update_status();
        } else if ("print".equals(method)) {
            return print();
        } 

        return filter();
    }

    public String getFrom(HttpServletRequest request) {
        String from = request.getParameter("from");
        if (from == null) {
            from = (String) request.getAttribute("from");
        }
        return from;
    }

    /* show a tickler */
    public String view() {
        log.debug("view");
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);

        String tickler_id = request.getParameter("id");
        Tickler tickler = ticklerManager.getTickler(loggedInInfo, tickler_id);
        request.setAttribute("tickler", tickler);

        // only active providers listed in the program stuff can be assigned
        if (tickler.getProgramId() != null)
            request.setAttribute("providers", providerMgr.getActiveProviders(null, tickler.getProgramId().toString()));
        else request.setAttribute("providers", providerMgr.getActiveProviders());

        request.setAttribute("from", getFrom(request));

        return "view";
    }

    /* run a filter */
    /* show all ticklers */
    public String filter() {
        log.debug("filter");

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);

        CustomFilter filter = this.getFilter();

        //view tickler from CME
        String filter_clientId = filter.getDemographicNo();
        String filter_clientName = filter.getDemographic_webName();
        if (filter_clientId != null && !"".equals(filter_clientId)) {
            if (filter_clientName == null || "".equals(filter_clientName)) {
                filter.setDemographic_webName(demographicMgr.getDemographic(filter_clientId).getFormattedName());
            }
        } else {
            filter_clientName = "";
            filter.setDemographic_webName("");
        }

        String providerId = (String) request.getSession().getAttribute("user");
        String programId = "";

        List<Program> programs = programMgr.getProgramDomainInCurrentFacilityForCurrentProvider(loggedInInfo, true);
        request.setAttribute("programs", programs);

        List<Tickler> ticklers = ticklerManager.getTicklers(loggedInInfo, filter, providerId, programId);

        List<CustomFilter> cf = ticklerManager.getCustomFilters(this.getProviderNo(request));
        // make my tickler filter
        boolean myticklerexisted = false;
        for (int i = 0; i < cf.size(); i++) {
            if ((cf.get(i).getName()).equals("*Myticklers*")) {
                myticklerexisted = true;
            }
        }
        if (!myticklerexisted) {

            CustomFilter myfilter = new CustomFilter();
            myfilter.setName("*Myticklers*");
            myfilter.setStartDateWeb("");
            // myfilter.setEnd_date(new Date(System.currentTimeMillis()));
            myfilter.setEndDateWeb("");
            myfilter.setProviderNo(this.getProviderNo(request));
            myfilter.setStatus("A");
            myfilter.setPriority("");
            myfilter.setClient("");
            myfilter.setAssignee((String) request.getSession().getAttribute("user"));
            myfilter.setDemographic_webName("");
            myfilter.setDemographicNo("");
            myfilter.setProgramId("");
            ticklerManager.saveCustomFilter(myfilter);
        }

        String filter_order = (String) request.getSession().getAttribute("filter_order");
        request.getSession().setAttribute("ticklers", ticklers);
        request.setAttribute("providers", providerMgr.getProviders());
        if (OscarProperties.getInstance().getBooleanProperty("clientdropbox", "on")) {
            request.setAttribute("demographics", demographicMgr.getDemographics());
        }

        request.setAttribute("customFilters", ticklerManager.getCustomFilters(this.getProviderNo(request)));
        request.setAttribute("from", getFrom(request));
        request.getSession().setAttribute("filter_order", filter_order);
        return "list";
    }

    /* run myfilter */
    /* show myticklers */
    public String my_tickler_filter() {
        log.debug("my_tickler_filter");

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);

        CustomFilter filter = this.getFilter();
        filter.setStartDate(null);
        filter.setEndDate(new Date(System.currentTimeMillis()));
        filter.setProvider(null);
        filter.setStatus("A");
        filter.setPriority(null);
        filter.setClient(null);
        filter.setAssignee((String) request.getSession().getAttribute("user"));
        filter.setDemographic_webName(null);
        filter.setProgramId(null);
        String providerId = (String) request.getSession().getAttribute("user");
        String programId = "";
        List<Tickler> ticklers = ticklerManager.getTicklers(loggedInInfo, filter, providerId, programId);
        request.getSession().setAttribute("ticklers", ticklers);
        request.setAttribute("providers", providerMgr.getProviders());
        if (OscarProperties.getInstance().getBooleanProperty("clientdropbox", "on")) {
            request.setAttribute("demographics", demographicMgr.getDemographics());
        }

        request.setAttribute("programs", programMgr.getProgramDomainInCurrentFacilityForCurrentProvider(loggedInInfo, true));

        request.setAttribute("customFilters", ticklerManager.getCustomFilters(this.getProviderNo(request)));
        request.setAttribute("from", getFrom(request));
        return "list";
    }

    public String run_custom_filter() {
        log.debug("run_custom_filter");

        CustomFilter filter = this.getFilter();
        String name = filter.getName();
        // CustomFilter newFilter = ticklerMgr.getCustomFilter(name);
        CustomFilter newFilter = ticklerManager.getCustomFilter(name, this.getProviderNo(request));

        /*
         * String filterId = Long.toString(filter.getId()); CustomFilter newFilter = ticklerMgr.getCustomFilterById(Integer.valueOf(filterId));
         */
        if (newFilter == null) {
            newFilter = new CustomFilter();
        }
        this.setFilter(newFilter);
        return filter();
    }

    /* ningys-reassign a ticker */
    public String reassign() {
        log.debug("reassign");
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);

        String id = request.getParameter("id");
        String reassignee = request.getParameter("tickler.taskAssignedTo");
        log.debug("reassign by" + id);

        ticklerManager.reassign(loggedInInfo, Integer.parseInt(id), getProviderNo(request), reassignee);


        this.setTickler(new Tickler());

        return view();
    }

    /* delete a tickler */
    public String delete() {
        log.debug("delete");
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);

        String[] checks = request.getParameterValues("checkbox");

        for (int x = 0; x < checks.length; x++) {
            ticklerManager.deleteTickler(loggedInInfo, Integer.parseInt(checks[x]), getProviderNo(request));
        }
        return filter();
    }

    /* add a comment to a tickler */
    public String add_comment() {
        log.debug("add_comment");
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);

        String id = request.getParameter("id");
        String message = request.getParameter("comment");
        log.debug("add_comment:" + id + "," + message);

        ticklerManager.addComment(loggedInInfo, Integer.parseInt(id), getProviderNo(request), message);

        return view();
    }

    /* complete a tickler */
    public String complete() {
        log.debug("complete");
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);

        String[] checks = request.getParameterValues("checkbox");

        for (int x = 0; x < checks.length; x++) {
            ticklerManager.completeTickler(loggedInInfo, Integer.parseInt(checks[x]), getProviderNo(request));
        }
        return filter();
    }

    /* edit a tickler */
    public String edit() {
        log.debug("edit");
        String programId = (String) request.getSession().getAttribute(SessionConstants.CURRENT_PROGRAM_ID);
        if (programId == null) {
            programId = String.valueOf(programMgr.getProgramIdByProgramName("OSCAR"));
        }
        request.setAttribute("providers", providerMgr.getActiveProviders(null, programId));
        request.setAttribute("program_name", programMgr.getProgramName(programId));
        request.setAttribute("from", getFrom(request));

        String demographicNo = request.getParameter("tickler.demographicNo");
        if (!StringUtils.isEmpty(demographicNo)) {
            Demographic demo = demographicMgr.getDemographic(demographicNo);
            if (demo != null) {
                request.setAttribute("demographicName", demo.getFormattedName());
            }
        }

        return "edit";
    }

    /* save a tickler */
    public String save() throws Exception {
        log.debug("save");
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);

        Provider user = providerMgr.getProvider(getProviderNo(request));

        Tickler tickler = this.getTickler();

        //Set the document if it's coming from the inbox
        String docType = request.getParameter("docType");
        String docId = request.getParameter("docId");

        // set the program which the tickler was written in if there is a program.
        String programIdStr = (String) request.getSession().getAttribute(SessionConstants.CURRENT_PROGRAM_ID);
        if (programIdStr != null) tickler.setProgramId(Integer.valueOf(programIdStr));

        /* get service time */
        String service_hour = request.getParameter("tickler.service_hour");
        String service_minute = request.getParameter("tickler.service_minute");
        String service_ampm = request.getParameter("tickler.service_ampm");
        tickler.setServiceTime(service_hour + ":" + service_minute + " " + service_ampm);

        tickler.setUpdateDate(new java.util.Date());
        tickler.setId(null);

        ticklerManager.addTickler(loggedInInfo, tickler);


        if (docType != null && docId != null && !docType.trim().equals("") && !docId.trim().equals("") && !docId.equalsIgnoreCase("null")) {

            int ticklerNo = tickler.getId();
            if (ticklerNo > 0) {
                try {
                    TicklerLink tLink = new TicklerLink();
                    tLink.setTableId(Long.parseLong(docId));
                    tLink.setTableName(docType);
                    tLink.setTicklerNo(new Long(ticklerNo).intValue());

                    ticklerManager.addTicklerLink(loggedInInfo, tLink);
                } catch (Exception e) {
                    MiscUtils.getLogger().error("No link with this tickler", e);
                }
            }

        }


        String echart = request.getParameter("echart");
        if (echart != null && echart.equals("true")) {
            Provider assignee = providerMgr.getProvider(tickler.getTaskAssignedTo());

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat formatter2 = new SimpleDateFormat("MM/dd/yy : hh:mm a");

            /* get current chart */
            EChart tempChart = echartDao.getLatestChart(tickler.getDemographicNo());
            String postedDate = "";
            if (tempChart != null) {
                postedDate = formatter.format(tempChart.getTimestamp());
            }

            /* create new object */
            EChart chart = new EChart();
            if (tempChart != null) {
                BeanUtils.copyProperties(chart, tempChart);
            } else {
                String curUser_no = (String) request.getSession().getAttribute("user");
                chart.setProviderNo(curUser_no);
            }
            String today = formatter.format(new Date());

            String e = chart.getEncounter();
            StringBuilder buf;
            if (e != null) {
                buf = new StringBuilder(e);
            } else {
                buf = new StringBuilder();
            }
            buf.append("\n\n");
            if (!today.equals(postedDate)) {
                buf.append("__________________________________________________\n");
                buf.append("[" + today + " .: ]");
                buf.append("\n");
            }
            buf.append("Message from  [" + user.getFormattedName() + "] to [" + assignee.getFormattedName() + "] [assigned " + formatter2.format(tickler.getUpdateDate()) + "]\n");
            buf.append("'" + tickler.getMessage() + "'");
            chart.setEncounter(buf.toString());
            chart.setId(null);
            echartDao.persist(chart);
        }

        addActionMessage(getText("tickler.saved"));
        CustomFilter filter = new CustomFilter();
        filter.setDemographicNo(tickler.getDemographicNo().toString());
        filter.setDemographic_webName(tickler.getDemographic_webName());
        filter.setEndDate(null);
        this.setFilter(filter);
        this.setTickler(new Tickler());
        //  return filter();
        response.sendRedirect("/Tickler.do?tickler.demographic_webName=" + tickler.getDemographic_webName() + "&tickler.demographicNo=" + tickler.getDemographicNo());
        return NONE;
    }

    /* get a list of prepared ticklers */
    public String prepared_tickler_list() {
        log.debug("prepared_tickler_list");
        String path = ServletActionContext.getServletContext().getRealPath("/");
        preparedTicklerMgr.setPath(path);
        request.setAttribute("preparedTicklers", preparedTicklerMgr.getTicklers());
        request.setAttribute("from", getFrom(request));
        return "preparedTicklerList";
    }

    public String prepared_tickler_edit() throws Exception {
        log.debug("prepared_tickler_edit");

        String name = request.getParameter("id");
        PreparedTickler pt = preparedTicklerMgr.getTickler(name);
        return prepared_tickler_list();
    }

    /* complete a tickler */
    public String update_status() {
        log.debug("update_status");
        char status = request.getParameter("status").charAt(0);
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);

        String id = request.getParameter("id");

        switch (status) {
            case 'A':
                ticklerManager.activateTickler(loggedInInfo, Integer.parseInt(id), getProviderNo(request));
                break;
            case 'C':
                ticklerManager.completeTickler(loggedInInfo, Integer.parseInt(id), getProviderNo(request));
                break;
            case 'D':
                ticklerManager.deleteTickler(loggedInInfo, Integer.parseInt(id), getProviderNo(request));
                break;
        }
        return this.view();
    }

    public boolean isModuleLoaded(HttpServletRequest request, String moduleName) {

        OscarProperties proper = OscarProperties.getInstance();

        if (proper.getProperty(moduleName, "").equalsIgnoreCase("yes") || proper.getProperty(moduleName, "").equalsIgnoreCase("true") || proper.getProperty(moduleName, "").equalsIgnoreCase("on")) {
            return true;
        }

        return false;
    }

    public String print() throws Exception {

        if (!securityInfoManager.hasPrivilege(LoggedInInfo.getLoggedInInfoFromSession(request), "_tickler", "r", null)) {
            throw new RuntimeException("Access Denied");
        }

        Tickler t = ticklerManager.getTickler(LoggedInInfo.getLoggedInInfoFromSession(request), request.getParameter("id"));
        if (t != null) {
            response.setHeader("Content-Disposition", "inline; filename=\"" + t.getDemographic().getLastName() + t.getDemographic().getFirstName() + t.getId() + ".pdf\"");
            response.setHeader("Expires", "0");
            response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
            response.setHeader("Pragma", "public");
            response.setContentType("application/pdf");

            TicklerPrinter tp = new TicklerPrinter(t, response.getOutputStream());
            tp.start();
            tp.printDocHeaderFooter();
            tp.printTicklerInfo();
            tp.finish();
        }

        return null;
    }

    private CustomFilter filter;
    private Tickler tickler;
    public CustomFilter getFilter() {
        return filter;
    }

    public void setFilter(CustomFilter filter) {
        this.filter = filter;
    }

    public Tickler getTickler() {
        return tickler;
    }

    public void setTickler(Tickler tickler) {
        this.tickler = tickler;
    }
}
