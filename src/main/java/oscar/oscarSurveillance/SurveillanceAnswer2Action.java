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
 * SurveillanceAnswer2Action.java
 *
 * Created on September 10, 2004, 8:07 PM
 */

package oscar.oscarSurveillance;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.oscarehr.util.MiscUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Jay Gallagher
 */
import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;

public class SurveillanceAnswer2Action extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();

    private static Logger log = MiscUtils.getLogger();

    /**
     * Creates a new instance of SurveillanceAnswer2Action
     */
    public SurveillanceAnswer2Action() {
    }

    public String execute() throws IOException, ServletException {

        //String answer = frm.getAnswer();
        //String surveyId = frm.getSurveyId();
        String demographic = this.getDemographicNo();
        String provider = (String) request.getSession().getAttribute("user");
        //String currentSurveyNum = frm.getCurrentSurveyNum();

        SurveillanceMaster sir = SurveillanceMaster.getInstance();

        Survey survey = sir.getSurveyById(surveyId);


        survey.processAnswer(provider, demographic, answer);

        log.debug("Survey: " + surveyId + " answer " + answer);

        //String proceed = frm.getProceed();
        String proceedURL = URLDecoder.decode(proceed, "UTF-8");

        if (currentSurveyNum != null) {
            try {
                int num = Integer.parseInt(currentSurveyNum);
                if (num < SurveillanceMaster.numSurveys()) {
                    request.setAttribute("currentSurveyNum", currentSurveyNum);
                    proceedURL = "/oscarSurveillance/CheckSurveillance.do?demographicNo=" + demographic + "&proceed=" + URLEncoder.encode(proceed, "UTF-8");
                    log.debug("sending to: " + proceedURL);
                }
            } catch (Exception e) {
            }
        }

        if (StringUtils.isBlank(proceedURL)) {
            proceedURL = "close.jsp";
        }

        response.sendRedirect(proceedURL);
        return NONE;
    }

    String answer = null;

    String proceed = null;
    String demographicNo = null;
    String surveyId = null;
    String currentSurveyNum = null;

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getProceed() {
        return proceed;
    }

    public void setProceed(String proceed) {
        this.proceed = proceed;
    }

    public String getDemographicNo() {
        return demographicNo;
    }

    public void setDemographicNo(String demographicNo) {
        this.demographicNo = demographicNo;
    }

    public String getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(String surveyId) {
        this.surveyId = surveyId;
    }

    public String getCurrentSurveyNum() {
        return currentSurveyNum;
    }

    public void setCurrentSurveyNum(String currentSurveyNum) {
        this.currentSurveyNum = currentSurveyNum;
    }
}
