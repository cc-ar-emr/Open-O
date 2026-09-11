package ca.openosp.openo.integration.oneId;

import ca.openosp.openo.commn.model.SystemPreferences;
import ca.openosp.openo.integration.dhdr.OmdGateway;
import ca.openosp.openo.managers.EhrConnectivityManager;
import ca.openosp.openo.utility.LoggedInInfo;
import ca.openosp.openo.utility.MiscUtils;
import ca.openosp.openo.utility.SessionConstants;
import ca.openosp.openo.utility.SpringUtils;
import org.apache.logging.log4j.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.io.IOException;

public class OneIdFilter implements Filter {

    private static final Logger logger = MiscUtils.getLogger();
  private final EhrConnectivityManager ehrConnectivityManager =
      SpringUtils.getBean(EhrConnectivityManager.class);
  private final OneIdSessionDao oneIdSessionDao = SpringUtils.getBean(OneIdSessionDao.class);
  private final OneIdViewletDao oneIdViewletDao = SpringUtils.getBean(OneIdViewletDao.class);

    public void init(FilterConfig config) throws ServletException {
        logger.info("Starting OneID Filter");
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpSession session = httpRequest.getSession(false);
        String loggedInUser =
            (session != null) ? (String) session.getAttribute("user") : null;

        // Every request reaches this filter, including each script, stylesheet and image a page
        // pulls in. Those cannot act on a ONE ID session, so they do not need one restored, and
        // rehydrating for each of them is one database read per asset per page.
        if (loggedInUser != null && !isStaticResource(httpRequest.getRequestURI())) {
            try {
                rehydrateOneIdSession(session, loggedInUser);
            } catch (Exception e) {
                // A failure restoring the ONE ID session must never break the request chain;
                // local (non-ONE ID) login has to keep working regardless.
                logger.warn("OneIdFilter: skipping ONE ID session rehydration - " + e.getMessage());
            }
        }
        // If no ONE ID session exists, the user is accessing the EMR locally with no intent of
        // authenticating with ONE ID; the request proceeds unchanged.
        chain.doFilter(request, response);
    }

    /** Requests for these are served as files and never act on a ONE ID session. */
    private static final Set<String> STATIC_EXTENSIONS = new HashSet<>(Arrays.asList(
            "css", "js", "map", "png", "jpg", "jpeg", "gif", "svg", "ico",
            "woff", "woff2", "ttf", "eot"));

    /**
     * Whether the path names a static file rather than something that acts on a session.
     *
     * @param path String the request URI, which carries no query string
     * @return boolean true when the request is for a static file
     */
    private static boolean isStaticResource(String path) {
        if (path == null) {
            return false;
        }
        int dot = path.lastIndexOf('.');
        if (dot < 0 || dot == path.length() - 1) {
            return false;
        }
        return STATIC_EXTENSIONS.contains(path.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    /**
     * Restores the ONE ID gateway data onto the HTTP session for a logged-in provider that has a
     * persisted {@link OneIdSession}. No-op when the provider has no ONE ID session.
     *
     * @param session      HttpSession the current session (non-null for a logged-in provider)
     * @param loggedInUser String the logged-in provider number
     */
    private void rehydrateOneIdSession(HttpSession session, String loggedInUser) {
        OneIdSession oneIdSession = oneIdSessionDao.find(loggedInUser);
        if (oneIdSession == null) {
            session.removeAttribute(LoggedInInfo.OH_GATEWAY_DATA);
            attachToLoggedInInfo(session, null);
            return;
        }
        OneIdGatewayData gatewayData = (OneIdGatewayData) session.getAttribute(LoggedInInfo.OH_GATEWAY_DATA);
        // Compared null-safely: a stored session may carry no lastKeptActive or access token, and
        // an exception raised here is swallowed by the caller, leaving ONE ID quietly not working.
        if (gatewayData != null && ((gatewayData.getUao() != null
                && !gatewayData.getUao().equals(oneIdSession.getUaoUpi())) ||
                !Objects.equals(gatewayData.getLastKeptActive(), oneIdSession.getLastKeptActive()) ||
                !Objects.equals(gatewayData.getAccessTokenStr(), oneIdSession.getAccessToken()))) {
            gatewayData = null;
        }
        if (gatewayData == null) {
            // If the session exists and is valid, add the data to the OSCAR session

            // Get One ID session info here and fill OMDGatewayData
            String pcoiKey = getPcoiKey();
            gatewayData = new OneIdGatewayData();
            gatewayData.setAccessTokenStr(oneIdSession.getAccessToken());
            gatewayData.processAccessToken(oneIdSession.getAccessToken());
            gatewayData.setRefreshTokenStr(oneIdSession.getRefreshToken());
            gatewayData.processRefreshToken(oneIdSession.getRefreshToken());
            gatewayData.setIdTokenStr(oneIdSession.getIdToken());
            gatewayData.processIdToken(oneIdSession.getIdToken());
            gatewayData.setAuthorizationId(oneIdSession.getAuthorizationId());
            gatewayData.setHubTopic(oneIdSession.getHubTopic());
            gatewayData.setCtxSessionId(oneIdSession.getHubTopic());
            gatewayData.setUao(oneIdSession.getUaoUpi());
            gatewayData.setUaoFriendlyName(oneIdSession.getUaoName());
            gatewayData.setCmsUrl(resolveCmsEndpoint(oneIdSession));
            gatewayData.setPcoiUrl(oneIdSession.getUrlFromToolbar(pcoiKey));
            gatewayData.setFhirIss(oneIdSession.getUrlFromToolbar(OmdGateway.ToolbarKeys.FHIR_ISS.key));
            gatewayData.setLastKeptActive(oneIdSession.getLastKeptActive());
            gatewayData.setSso(oneIdSession.isSso());

            session.setAttribute(LoggedInInfo.OH_GATEWAY_DATA, gatewayData);
        }

        attachToLoggedInInfo(session, gatewayData);
    }

    /**
     * Puts the ONE ID gateway data on the logged-in info held by the session. The logged-in info is
     * rebuilt on every request, so the data is attached each time rather than only on the request
     * that reads it from the database.
     *
     * @param session     HttpSession the current session
     * @param gatewayData OneIdGatewayData the resolved gateway data, or null when the provider has
     *                    no ONE ID session
     */
    private void attachToLoggedInInfo(HttpSession session, OneIdGatewayData gatewayData) {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(session);
        if (loggedInInfo != null) {
            loggedInInfo.setOneIdGatewayData(gatewayData);
            LoggedInInfo.setLoggedInInfoIntoSession(session, loggedInInfo);
        }
    }

    /**
     * Resolves the CMS server endpoint from the ONE ID toolbar, preferring the "hub.url" key and
     * falling back to "cms_url". When neither key is present the endpoint is empty and the toolbar
     * keys that are present are logged.
     *
     * @param oneIdSession OneIdSession the persisted ONE ID session carrying the toolbar
     * @return String the CMS endpoint URL, or an empty string when the toolbar carries neither key
     */
    private String resolveCmsEndpoint(OneIdSession oneIdSession) {
        String endpoint = oneIdSession.getUrlFromToolbar(OmdGateway.ToolbarKeys.HUB_URL.key);
        if (endpoint == null || endpoint.isEmpty()) {
            endpoint = oneIdSession.getUrlFromToolbar(OmdGateway.ToolbarKeys.CMS_URL.key);
        }
        if (endpoint == null || endpoint.isEmpty()) {
            logger.warn("ONE ID toolbar carried no CMS endpoint under '"
                + OmdGateway.ToolbarKeys.HUB_URL.key + "' or '" + OmdGateway.ToolbarKeys.CMS_URL.key
                + "'; toolbar keys present: " + oneIdSession.getToolbarKeys());
        }
        return endpoint;
    }

    private String getPcoiKey() {
        String configuredKey = ehrConnectivityManager.getConfigValue(SystemPreferences.ONEID_KEYS.pcoi_key, "");
        if (configuredKey == null || configuredKey.trim().isEmpty()) {
            return "";
        }
        OneIdViewlet oneIdViewlet = oneIdViewletDao.queryOneIdViewletForKey(configuredKey.trim());
        return oneIdViewlet == null ? "" : oneIdViewlet.getKeyValue();
    }
}
