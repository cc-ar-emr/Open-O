//CHECKSTYLE:OFF
package org.oscarehr.contactRegistry;

import com.opensymphony.xwork2.ActionSupport;
import net.sf.json.JSONObject;
import org.apache.struts2.ServletActionContext;
import org.oscarehr.common.model.ProfessionalSpecialist;
import org.oscarehr.managers.ProfessionalSpecialistsManager;
import org.oscarehr.util.JsonUtil;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.SpringUtils;
import oscar.form.JSONUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class ProfessionalSpecialist2Action extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();

    private final ProfessionalSpecialistsManager professionalSpecialistsManager = SpringUtils.getBean(ProfessionalSpecialistsManager.class);

    public String execute() {

        /*
         * Designed for backwards compatibility.
         * Otherwise use the dispatch action methods.
         */
        if ("/getProfessionalSpecialist".equals(actionType)) {
            this.get();
        }

        if ("/searchProfessionalSpecialist".equals(actionType)) {
            this.search();
        }

        return null;
    }

    public void get() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String specialistId = request.getParameter("id");
        ProfessionalSpecialist professionalSpecialist = null;

        if (specialistId != null && !specialistId.isEmpty()) {
            professionalSpecialist = professionalSpecialistsManager.getProfessionalSpecialist(loggedInInfo, Integer.parseInt(specialistId));
        }

        if (professionalSpecialist != null) {
            JSONObject professionalSpecialistJSON = JsonUtil.pojoToJson(professionalSpecialist);
            JSONUtil.jsonResponse(response, professionalSpecialistJSON.toString());
        }
    }

    public void search() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String search_keyword = request.getParameter("keyword");
        List<ProfessionalSpecialist> professionalSpecialist = null;

        if (search_keyword != null && !search_keyword.isEmpty()) {
            professionalSpecialist = professionalSpecialistsManager.searchProfessionalSpecialist(loggedInInfo, search_keyword);
        }

        if (professionalSpecialist != null) {
            String professionalSpecialistJSON = JsonUtil.pojoCollectionToJson(professionalSpecialist);
            JSONUtil.jsonResponse(response, professionalSpecialistJSON);
        }
    }
    private String actionType; // Determines the type of action
    // Getter and Setter for actionType
    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }
}
