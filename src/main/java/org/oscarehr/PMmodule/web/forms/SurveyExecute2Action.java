package org.oscarehr.PMmodule.web.forms;

import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.Logger;
import org.oscarehr.PMmodule.dao.SurveySecurityDao;
import org.oscarehr.PMmodule.dao.SurveySecurityDaoImpl;
import org.oscarehr.PMmodule.service.AdmissionManager;
import org.oscarehr.PMmodule.service.ClientManager;
import org.oscarehr.PMmodule.service.SurveyManager;
import org.oscarehr.common.model.Provider;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SessionConstants;
import org.oscarehr.util.SpringUtils;
import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;

public class SurveyExecute2Action extends ActionSupport {
    private static final Logger log = MiscUtils.getLogger();
    private static final Pattern SAFE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$"); // Only allow safe characters

    private final HttpServletRequest request = ServletActionContext.getRequest();
    private final HttpServletResponse response = ServletActionContext.getResponse();
    private final SurveyManager surveyManager = (SurveyManager) SpringUtils.getBean(SurveyManager.class);
    private final ClientManager clientManager = (ClientManager) SpringUtils.getBean(ClientManager.class);
    private final AdmissionManager admissionManager = (AdmissionManager) SpringUtils.getBean(AdmissionManager.class);

    protected String getProviderNo(HttpServletRequest request) {
        Provider provider = getProvider(request);
        return (provider != null) ? provider.getProviderNo() : null;
    }

    protected Provider getProvider(HttpServletRequest request) {
        return (Provider) request.getSession().getAttribute(SessionConstants.PROVIDER);
    }

    public String forwardToClientManager(String clientId) {
        if (!isValidInput(clientId)) {
            log.warn("Invalid client ID received");
            return ERROR;
        }
        request.setAttribute("demographicNo", clientId);
        request.setAttribute("clientId", clientId);
        request.setAttribute("tab.override", "Forms");
        return SUCCESS;
    }

    public String execute() {
        String clientId = request.getParameter("clientId");
        if (!isValidInput(clientId)) {
            log.warn("Invalid clientId received in execute()");
            return ERROR;
        }
        return forwardToClientManager(clientId);
    }

    protected long getUserId(HttpServletRequest request) {
        String value = (String) request.getSession().getAttribute(SessionConstants.USER);
        try {
            return (value != null && value.matches("\\d+")) ? Long.parseLong(value) : -1;
        } catch (NumberFormatException e) {
            log.error("Invalid user ID format", e);
            return -1;
        }
    }

    protected String getUsername(HttpServletRequest request) {
        Provider provider = (Provider) request.getSession().getAttribute(SessionConstants.PROVIDER);
        return (provider != null) ? provider.getFormattedName() : null;
    }

    public String survey() {
        SurveySecurityDao securityDao = new SurveySecurityDaoImpl();
        boolean hasAccess = false;

        try {
            String username = (String) request.getSession().getAttribute(SessionConstants.USER);
            if (username != null) {
                hasAccess = securityDao.checkPrivilege("SURVEY_ACCESS", username);
            }
        } catch (Exception e) {
            log.error("Access check failed", e);
        }

        if (!hasAccess) {
            return handleAccessDeny();
        }

        return refresh();
    }

    private String handleAccessDeny() {
        log.warn("Access denied for survey execution");
        return "access_denied";
    }

    private String refresh() {
        log.info("Refreshing survey view");
        return SUCCESS;
    }

    private boolean isValidInput(String input) {
        return input != null && SAFE_ID_PATTERN.matcher(input).matches();
    }

    private boolean validateAndStoreSessionAttribute(String key, String value) {
        if (value != null && SAFE_ID_PATTERN.matcher(value).matches()) {
            request.getSession().setAttribute(key, value);
            return true;
        } else {
            log.warn("Attempt to store unsafe value in session: " + key);
            return false;
        }
    }

    private void regenerateSession() {
        request.getSession().invalidate();
        request.getSession(true); // Generate a new session ID
    }
}
