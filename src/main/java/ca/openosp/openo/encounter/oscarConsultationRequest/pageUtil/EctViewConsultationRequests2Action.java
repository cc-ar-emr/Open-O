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


package ca.openosp.openo.encounter.oscarConsultationRequest.pageUtil;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import ca.openosp.openo.commn.dao.ConsultationRequestDao;
import ca.openosp.openo.commn.dao.ConsultationRequestDaoImpl;
import ca.openosp.openo.commn.model.ProfessionalSpecialist;
import ca.openosp.openo.managers.SecurityInfoManager;
import ca.openosp.openo.utility.LoggedInInfo;
import ca.openosp.openo.utility.MiscUtils;
import ca.openosp.openo.utility.SpringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.struts2.ActionSupport;
import org.apache.struts2.ServletActionContext;

public class EctViewConsultationRequests2Action extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();

    private static SecurityInfoManager securityInfoManager = SpringUtils.getBean(SecurityInfoManager.class);
    private static final Logger logger = MiscUtils.getLogger();
    private static final int CONSULTANT_SEARCH_MAX_RESULTS = 20;

    private ConsultationRequestDao consultationRequestDao = SpringUtils.getBean(ConsultationRequestDao.class);

    private String sendTo;
    private String currentTeam;

    private String startDate;
    private String endDate;
    private String includeCompleted;
    private String orderby;
    private String desc;
    private String searchDate = null;
    private Integer offset;
    private Integer limit = ConsultationRequestDaoImpl.DEFAULT_CONSULT_REQUEST_RESULTS_LIMIT;
    private Integer consultantId;
    private String filterProviderNo;

    public String execute() throws ServletException, IOException {
        if (!securityInfoManager.hasPrivilege(LoggedInInfo.getLoggedInInfoFromSession(request), "_con", "r", null)) {
            throw new SecurityException("missing required sec object (_con)");
        }

        String defaultPattern = "yyyy-MM-dd";
        String includeCompleted = null;
        boolean includedComp = false;

        if (includeCompleted != null && includeCompleted.equals("include")) {
            includedComp = true;
        }

        SimpleDateFormat simpleDateFormat = null;

        try {
            if (startDate != null && !startDate.isEmpty()) {
                simpleDateFormat = new SimpleDateFormat(defaultPattern);
                request.setAttribute("startDate", simpleDateFormat.parse(startDate));
            }

            if (endDate != null && !endDate.isEmpty()) {
                if (simpleDateFormat == null) {
                    simpleDateFormat = new SimpleDateFormat(defaultPattern);
                }
                request.setAttribute("endDate", simpleDateFormat.parse(endDate));
            }
        } catch (Exception e) {
            logger.error("Cannot parse start date " + startDate + " and/or end date " + endDate + " for consultation request report. ", e);
        }

        request.setAttribute("includeCompleted", Boolean.valueOf(includedComp));
        request.setAttribute("teamVar", sendTo);
        request.setAttribute("orderby", orderby);
        request.setAttribute("desc", desc);
        request.setAttribute("searchDate", searchDate);
        request.setAttribute("consultantId", consultantId);
        request.setAttribute("filterProviderNo", filterProviderNo);
        return SUCCESS;
    }

    /**
     * Returns consultants (professional specialists) whose name matches the given keyword, as a
     * JSON array of {@code {label, value}} pairs for the Consultants autocomplete filter on the
     * Consultations page. Results are capped at {@link #CONSULTANT_SEARCH_MAX_RESULTS} so the
     * response stays small regardless of how many consultants are registered.
     *
     * @throws IOException if writing the JSON response fails
     * @since 2026-07-10
     */
    public void searchConsultants() throws IOException {
        if (!securityInfoManager.hasPrivilege(LoggedInInfo.getLoggedInInfoFromSession(request), "_con", "r", null)) {
            throw new SecurityException("missing required sec object (_con)");
        }

        String keyword = request.getParameter("keyword");
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode results = mapper.createArrayNode();

        if (keyword != null && !keyword.trim().isEmpty()) {
            List<ProfessionalSpecialist> matches = consultationRequestDao.searchDistinctConsultants(keyword.trim(), CONSULTANT_SEARCH_MAX_RESULTS);
            for (ProfessionalSpecialist specialist : matches) {
                ObjectNode node = mapper.createObjectNode();
                node.put("label", specialist.getFormattedName());
                node.put("value", specialist.getId() != null ? specialist.getId().toString() : "");
                results.add(node);
            }
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(results.toString());
    }

    public String getSendTo() {
        return sendTo;
    }

    public void setSendTo(String str) {
        sendTo = str;
    }

    public String getCurrentTeam() {
        if (currentTeam == null)
            currentTeam = new String();
        return currentTeam;
    }

    public void setCurrentTeam(String str) {
        currentTeam = str;
    }

    /**
     * Getter for property startDate.
     *
     * @return Value of property startDate.
     */
    public String getStartDate() {
        return startDate;
    }

    /**
     * Setter for property startDate.
     *
     * @param startDate New value of property startDate.
     */
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    /**
     * Getter for property endDate.
     *
     * @return Value of property endDate.
     */
    public String getEndDate() {
        return endDate;
    }

    /**
     * Setter for property endDate.
     *
     * @param endDate New value of property endDate.
     */
    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    /**
     * Getter for property includeCompleted.
     *
     * @return Value of property includeCompleted.
     */
    public String getIncludeCompleted() {
        return includeCompleted;
    }

    /**
     * Setter for property includeCompleted.
     *
     * @param includeCompleted New value of property includeCompleted.
     */
    public void setIncludeCompleted(String includeCompleted) {
        this.includeCompleted = includeCompleted;
    }

    /**
     * Getter for property orderby.
     *
     * @return Value of property orderby.
     */
    public String getOrderby() {
        return orderby;
    }

    /**
     * Setter for property orderby.
     *
     * @param orderby New value of property orderby.
     */
    public void setOrderby(String orderby) {
        this.orderby = orderby;
    }

    /**
     * Getter for property desc.
     *
     * @return Value of property desc.
     */
    public String getDesc() {
        return desc;
    }

    /**
     * Setter for property desc.
     *
     * @param desc New value of property desc.
     */
    public void setDesc(String desc) {
        this.desc = desc;
    }

    /**
     * Getter for property searchDate.
     *
     * @return Value of property searchDate.
     */
    public String getSearchDate() {
        if (searchDate == null) {
            searchDate = "0";
        }
        return searchDate;
    }

    /**
     * Setter for property searchDate.
     *
     * @param searchDate New value of property searchDate.
     */
    public void setSearchDate(String searchDate) {
        this.searchDate = searchDate;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    /**
     * @return Integer the consultant the list is filtered to, null for all consultants
     * @since 2026-06-19
     */
    public Integer getConsultantId() {
        return consultantId;
    }

    /**
     * @param consultantId Integer the consultant to filter by, null for all consultants
     * @since 2026-06-19
     */
    public void setConsultantId(Integer consultantId) {
        this.consultantId = consultantId;
    }

    /**
     * @return String the provider number the list is filtered to, null or empty for all providers
     * @since 2026-06-19
     */
    public String getFilterProviderNo() {
        return filterProviderNo;
    }

    /**
     * @param filterProviderNo String the provider number to filter by, null or empty for all
     * @since 2026-06-19
     */
    public void setFilterProviderNo(String filterProviderNo) {
        this.filterProviderNo = filterProviderNo;
    }
}
