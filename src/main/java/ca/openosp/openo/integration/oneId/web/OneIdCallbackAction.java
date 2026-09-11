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
package ca.openosp.openo.integration.oneId.web;

import ca.openosp.openo.commn.model.Security;
import ca.openosp.openo.integration.dhdr.OmdGateway;
import ca.openosp.openo.integration.oneId.IdTokenValidationException;
import ca.openosp.openo.integration.oneId.OneIdJwksProvider;
import ca.openosp.openo.integration.oneId.OneIdSession;
import ca.openosp.openo.integration.oneId.web.ConnectivityErrorHelper.ConnectivityError;
import ca.openosp.openo.log.LogAction;
import ca.openosp.openo.log.LogConst;
import ca.openosp.openo.managers.EhrConnectivityManager;
import ca.openosp.openo.managers.SsoAuthenticationManager;
import ca.openosp.openo.utility.LoggedInInfo;
import ca.openosp.openo.utility.MiscUtils;
import ca.openosp.openo.utility.SpringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.struts2.ActionSupport;
import org.apache.struts2.ServletActionContext;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Callback for the ONE ID OAuth2/OIDC login. It verifies the anti-forgery state, exchanges the
 * authorization code for tokens, validates the id_token signature and claims, resolves the linked
 * provider, establishes a fully authenticated session, and persists the ONE ID session. An
 * unlinked identity is sent to the login page to bind once. When the callback arrives on a session
 * that is already authenticated it is a mid-session step-up: the existing login is kept, the
 * verified identity must belong to the same provider, and only the ONE ID session is attached
 * before returning to the page that started the step-up. There is no privilege check because this
 * can be what establishes the session; trust comes from the state, nonce, and id-token validation.
 *
 * @since 2026-07-02
 */
public class OneIdCallbackAction extends ActionSupport {

    private static final Logger logger = MiscUtils.getLogger();

    private final HttpServletRequest request = ServletActionContext.getRequest();
    private final HttpServletResponse response = ServletActionContext.getResponse();

    private final OneIdJwksProvider oneIdJwksProvider = SpringUtils.getBean(OneIdJwksProvider.class);
    private final EhrConnectivityManager ehrConnectivityManager = SpringUtils.getBean(EhrConnectivityManager.class);
    private final SsoAuthenticationManager ssoAuthenticationManager = SpringUtils.getBean(SsoAuthenticationManager.class);

    @Override
    public String execute() {
        HttpSession session = request.getSession(false);
        // A live authenticated session means this callback is a mid-session step-up, not a login.
        String stepUpProviderNo = (session == null) ? null : (String) session.getAttribute("user");
        String returnUrl = (session == null) ? null
                : OneIdLoginAction.safeLocalPath((String) session.getAttribute(OneIdLoginAction.SESSION_RETURN_URL));
        try {
            if (session == null) {
                return fail("Your sign-in session could not be found.", "Please try signing in again.");
            }

            String expectedState = (String) session.getAttribute(OneIdLoginAction.SESSION_STATE);
            String nonce = (String) session.getAttribute(OneIdLoginAction.SESSION_NONCE);
            String verifier = (String) session.getAttribute(OneIdLoginAction.SESSION_VERIFIER);
            session.removeAttribute(OneIdLoginAction.SESSION_STATE);
            session.removeAttribute(OneIdLoginAction.SESSION_NONCE);
            session.removeAttribute(OneIdLoginAction.SESSION_VERIFIER);

            String returnedState = request.getParameter("state");
            String code = request.getParameter("code");

            if (expectedState == null || returnedState == null || !expectedState.equals(returnedState)) {
                if (stepUpProviderNo != null) {
                    return stepUpError(session, returnUrl, "verifyFailed");
                }
                return fail("Your sign-in could not be verified.", "Please try signing in again.");
            }

            // The broker reports a refusal by redirecting back with error/error_description
            // instead of a code; the code (e.g. CSV-nnn) is what the OH help desk asks for,
            // so it is audited and shown.
            String brokerError = request.getParameter("error");
            if (brokerError != null && !brokerError.isEmpty()) {
                String brokerCode = safeBrokerText(brokerError, 40);
                String brokerDescription = safeBrokerText(request.getParameter("error_description"), 200);
                String auditDetail = "ONE ID broker error " + brokerCode
                        + (brokerDescription.isEmpty() ? "" : ": " + brokerDescription);
                if (stepUpProviderNo != null) {
                    return stepUpError(session, returnUrl, "error", auditDetail);
                }
                return redirectToLoginFailed("Ontario Health could not complete the sign-in ("
                        + brokerCode + "). Please try again or contact your administrator.", auditDetail);
            }

            if (code == null || code.isEmpty()) {
                if (stepUpProviderNo != null) {
                    return stepUpError(session, returnUrl, "error");
                }
                return fail("Ontario Health did not return an authorization code.", "Please try signing in again.");
            }

            LoggedInInfo loggedInInfo = new LoggedInInfo();
            loggedInInfo.setSession(session);

            OmdGateway omdGateway = new OmdGateway();
            JSONObject tokens;
            // Closed on every path, including the refusals that return without reading a body.
            // Each sign-in leaks a pooled connection otherwise.
            Response tokenResponse = omdGateway.exchangeCodeForTokens(loggedInInfo, code, verifier);
            try {
                if (tokenResponse.getStatus() != 200) {
                    if (stepUpProviderNo != null) {
                        return stepUpError(session, returnUrl, "error");
                    }
                    return failWith(ConnectivityErrorHelper.mapStatus(tokenResponse.getStatus()));
                }
                tokens = new JSONObject(tokenResponse.readEntity(String.class));
            } finally {
                tokenResponse.close();
            }
            String idToken = tokens.optString("id_token", null);
            if (idToken == null) {
                if (stepUpProviderNo != null) {
                    return stepUpError(session, returnUrl, "error");
                }
                return fail("Ontario Health did not return an identity token.", "Please try signing in again.");
            }

            String subject = oneIdJwksProvider.verifyIdToken(idToken, nonce);

            List<Security> matches = ehrConnectivityManager.findProvidersByOneId(subject);

            if (stepUpProviderNo != null) {
                return completeStepUp(session, stepUpProviderNo, returnUrl, subject, matches, tokens);
            }

            if (matches == null || matches.isEmpty()) {
                // Not linked yet: keep the verified subject and send the provider to log in once to bind.
                // The email goes with it because the sign-in form has no verified one to post, and
                // the moment is stamped so an abandoned attempt cannot be picked up much later by
                // whoever sits down at the browser next.
                session.setAttribute("oneIdSubject", subject);
                session.setAttribute("oneIdSubjectEmail", tokens.optString("email", null));
                session.setAttribute("oneIdSubjectAt", Long.valueOf(System.currentTimeMillis()));
                response.sendRedirect(request.getContextPath() + "/index.jsp?oneIdLink=true");
                return NONE;
            }
            if (matches.size() > 1) {
                return fail("More than one account is linked to this ONE ID.", "Contact your administrator.");
            }

            Map<String, Object> sessionData = ssoAuthenticationManager.checkOneIdLogin(subject);
            if (sessionData == null || sessionData.isEmpty()) {
                return fail("Could not sign you in with ONE ID.", "Please try again or contact your administrator.");
            }

            session.invalidate();
            HttpSession newSession = request.getSession(true);
            newSession.setMaxInactiveInterval(7200);
            newSession.setAttribute("oscar_context_path", request.getContextPath());
            newSession.setAttribute("fullSite", "true");
            for (String key : sessionData.keySet()) {
                newSession.setAttribute(key, sessionData.get(key));
            }

            String providerNo = (String) newSession.getAttribute("user");
            LogAction.addLog(providerNo, LogConst.LOGIN, LogConst.CON_LOGIN, "", request.getRemoteAddr());
            ehrConnectivityManager.saveOneIdSession(buildOneIdSession(providerNo, subject, tokens));

            response.sendRedirect(request.getContextPath() + "/uaoSelect.do");
            return NONE;
        } catch (IdTokenValidationException e) {
            logger.warn("ONE ID id-token validation failed");
            if (stepUpProviderNo != null) {
                return stepUpError(session, returnUrl, "verifyFailed");
            }
            return fail("Your sign-in could not be verified.", "Please try signing in again.");
        } catch (Exception e) {
            logger.error("ONE ID callback failed (" + e.getClass().getSimpleName() + ")");
            if (stepUpProviderNo != null) {
                return stepUpError(session, returnUrl, "error");
            }
            return failWith(ConnectivityErrorHelper.map(e));
        }
    }

    /**
     * Finishes a mid-session step-up: the verified ONE ID identity must be the one linked to the
     * provider who is already signed in, and on a match only the ONE ID session is attached - the
     * existing login is never rebuilt or invalidated. The session filter restores the gateway data
     * from the saved row on the next request.
     *
     * @param session HttpSession the live authenticated session
     * @param providerNo String the signed-in provider number
     * @param returnUrl String the local path that started the step-up, or null
     * @param subject String the verified id-token subject
     * @param matches List&lt;Security&gt; the accounts linked to the subject
     * @param tokens JSONObject the token-endpoint response
     * @return String the Struts result (always NONE; the response is a redirect)
     * @throws Exception when the redirect cannot be written
     */
    private String completeStepUp(HttpSession session, String providerNo, String returnUrl, String subject,
                                  List<Security> matches, JSONObject tokens) throws Exception {
        if (matches == null || matches.isEmpty()) {
            return stepUpError(session, returnUrl, "notLinked");
        }
        if (matches.size() > 1) {
            return stepUpError(session, returnUrl, "error");
        }
        if (!providerNo.equals(matches.get(0).getProviderNo())) {
            return stepUpError(session, returnUrl, "differentAccount");
        }
        ehrConnectivityManager.saveOneIdSession(buildOneIdSession(providerNo, subject, tokens));
        LogAction.addLog(providerNo, LogConst.LOGIN, LogConst.CON_LOGIN, "ONE ID step-up", request.getRemoteAddr());
        response.sendRedirect(request.getContextPath() + "/uaoSelect.do");
        return NONE;
    }

    /**
     * Ends a failed step-up without touching the existing login: the failure is audited and the
     * provider is sent back to the page that started the step-up with an error code the page can
     * explain, or to the login-failed page when no return path is known.
     *
     * @param session HttpSession the live authenticated session
     * @param returnUrl String the local path that started the step-up, or null
     * @param code String the machine-readable failure code for the return page
     * @return String the Struts result (always NONE; the response is a redirect)
     */
    private String stepUpError(HttpSession session, String returnUrl, String code) {
        return stepUpError(session, returnUrl, code, null);
    }

    private String stepUpError(HttpSession session, String returnUrl, String code, String auditDetail) {
        session.removeAttribute(OneIdLoginAction.SESSION_RETURN_URL);
        String message = stepUpMessage(code);
        LogAction.addLog("", LogConst.LOGIN, "failed",
                auditDetail != null ? auditDetail : "ONE ID step-up: " + message, request.getRemoteAddr());
        try {
            if (returnUrl != null) {
                String separator = returnUrl.contains("?") ? "&" : "?";
                response.sendRedirect(returnUrl + separator + "oneIdStepUpError=" + code);
            } else {
                response.sendRedirect(request.getContextPath() + "/loginfailed.jsp?errormsg="
                        + URLEncoder.encode(message, "UTF-8"));
            }
        } catch (Exception e) {
            logger.error("Failed to redirect after a ONE ID step-up failure", e);
        }
        return NONE;
    }

    private static String stepUpMessage(String code) {
        switch (code) {
            case "notLinked":
                return "Your ONE ID is not linked to this account. Log out and sign in with ONE ID once to link it, or contact your administrator.";
            case "differentAccount":
                return "This ONE ID is linked to a different account. Use the ONE ID linked to your user.";
            case "verifyFailed":
                return "Your ONE ID sign-in could not be verified. Please try again.";
            default:
                return "The ONE ID sign-in could not be completed. Please try again.";
        }
    }

    private OneIdSession buildOneIdSession(String providerNo, String subject, JSONObject tokens) {
        OneIdSession oneIdSession = new OneIdSession();
        oneIdSession.setProviderNo(providerNo);
        oneIdSession.setAccessToken(tokens.optString("access_token", null));
        oneIdSession.setRefreshToken(tokens.optString("refresh_token", null));
        oneIdSession.setIdToken(tokens.optString("id_token", null));
        oneIdSession.setSubject(subject);
        oneIdSession.setEmail(tokens.optString("email", null));
        oneIdSession.setToolbar(tokens.optString("toolbar", null));
        oneIdSession.setAuthorizationId(tokens.optString("authzid", null));
        oneIdSession.setLastKeptActive(new Date());
        oneIdSession.setTimestamp(System.currentTimeMillis());
        oneIdSession.setSso(true);
        return oneIdSession;
    }

    private String fail(String message, String nextStep) {
        return redirectToLoginFailed(message + " " + nextStep);
    }

    private String failWith(ConnectivityError error) {
        return redirectToLoginFailed(error.getUserMessage() + " " + error.getNextStep());
    }

    private String redirectToLoginFailed(String message) {
        return redirectToLoginFailed(message, message);
    }

    private String redirectToLoginFailed(String message, String auditDetail) {
        LogAction.addLog("", LogConst.LOGIN, "failed", auditDetail, request.getRemoteAddr());
        try {
            response.sendRedirect(request.getContextPath() + "/loginfailed.jsp?errormsg="
                    + URLEncoder.encode(message, "UTF-8"));
        } catch (Exception e) {
            logger.error("Failed to redirect to the login-failed page", e);
        }
        return NONE;
    }

    /**
     * Reduces broker-supplied error text to a safe, bounded token for logs and messages: only
     * plain identifier characters survive, so response text can never smuggle markup or control
     * characters into a page or an audit row.
     *
     * @param value String the raw broker parameter
     * @param maxLength int the maximum length to keep
     * @return String the sanitized text, empty when the input is null
     */
    private static String safeBrokerText(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String cleaned = value.replaceAll("[^A-Za-z0-9 _.:/-]", "");
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength) : cleaned;
    }
}
