/**
 * Copyright (c) 2001-2002. Department of Family Medicine, McMaster University. All Rights Reserved.
 * This software is published under the GPL GNU General Public License. This program is free
 * software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 * <p>
 * This software was written for the Department of Family Medicine McMaster University Hamilton
 * Ontario, Canada
 */
package ca.openosp.openo.integration.ohcms;

import ca.openosp.openo.commn.dao.DemographicDao;
import ca.openosp.openo.commn.model.Demographic;
import ca.openosp.openo.integration.dhdr.OmdGateway;
import ca.openosp.openo.integration.fhircast.Event;
import ca.openosp.openo.integration.fhircast.UserLogin;
import ca.openosp.openo.integration.oneId.OneIdGatewayData;
import ca.openosp.openo.managers.EhrConnectivityManager;
import ca.openosp.openo.utility.LoggedInInfo;
import ca.openosp.openo.utility.MiscUtils;
import ca.openosp.openo.utility.SpringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hl7.fhir.r4.model.Location;

import javax.annotation.Nullable;
import javax.ws.rs.core.Response;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CMSManager {

  static Logger logger = MiscUtils.getLogger();

  /** One lock per provider, guarding the patient held in their CMS context. */
  private static final ConcurrentMap<String, Object> contextLocks =
      new ConcurrentHashMap<String, Object>();

  /**
   * The lock guarding a provider's CMS patient context.
   *
   * <p>Keyed by provider rather than taken on the gateway data, which the session filter replaces
   * whenever the stored session moves on: two requests can then hold different instances and
   * synchronize on nothing.
   *
   * <p>A caller whose work depends on the context staying put - composing a launch URL for the
   * patient just put in context - holds this across both steps. Setting the context and then
   * launching under it are one operation; between them a second request can move the context to
   * another patient, and the launch opens the wrong record.
   *
   * @param loggedInInfo LoggedInInfo the acting provider session
   * @return Object the monitor to synchronize on
   */
  public static Object contextLock(LoggedInInfo loggedInInfo) {
    String providerNo = loggedInInfo == null ? null : loggedInInfo.getLoggedInProviderNo();
    return contextLocks.computeIfAbsent(providerNo == null ? "" : providerNo, key -> new Object());
  }

  public static String createHubTopic(LoggedInInfo loggedInInfo) throws Exception {
    OmdGateway omdGateway = new OmdGateway();
    OneIdGatewayData oneIdGatewayData = loggedInInfo.getOneIdGatewayData();
    WebClient createHubTopic = omdGateway.getWebClientWholeURL(loggedInInfo,
        oneIdGatewayData.getCmsUrl() + "/createHubTopic");
    Response hubTopicResponse = omdGateway.doPost(loggedInInfo, createHubTopic,
        new Event(UUID.randomUUID().toString(), "hubTopic", "createHubTopic"));
    String hubTopicResponseBody = hubTopicResponse.readEntity(String.class);
    JSONObject responseB = new JSONObject(hubTopicResponseBody);
    logStatus("hubTopicResponse", hubTopicResponse);
    String hubTopic = responseB.getString("hub.topic");
    oneIdGatewayData.setHubTopic(hubTopic);
    // The hub.topic is the CMS-issued identifier of this context session; carrying it as the
    // context session id puts it on every context audit row.
    oneIdGatewayData.setCtxSessionId(hubTopic);
    EhrConnectivityManager ehrConnectivityManager = SpringUtils.getBean(EhrConnectivityManager.class);
    ehrConnectivityManager.setOwnSessionHubTopic(loggedInInfo, hubTopic);
    return null;
  }

  @Nullable
  public static String userLogin(LoggedInInfo loggedInInfo) throws Exception {
    FhirResources fhirResources = new FhirResources();
    OmdGateway omdGateway = new OmdGateway();
    OneIdGatewayData oneIdGatewayData = loggedInInfo.getOneIdGatewayData();
    if (oneIdGatewayData.getHubTopic() == null) {
      createHubTopic(loggedInInfo);
    }
    WebClient createHubTopic = omdGateway.getWebClientWholeURL(loggedInInfo,
        oneIdGatewayData.getCmsUrl());
    String uuid = UUID.randomUUID().toString();
    UserLogin userLogin = new UserLogin(uuid, oneIdGatewayData.getHubTopic());
    try {
      // The context is made up of four profiles: Organization, Location, Practitioner and Patient.
      // The first three are set here, at sign-in; Patient is sent separately in patientOpen,
      // immediately before an EHR service launches, so the patient in context is the one on screen.
      // Organization is the custodian the provider acts for and changes with their authority;
      // Location is this clinic, and there is one of it.
      userLogin.addContext("organization",
          fhirResources.getString(fhirResources.getOrganization(loggedInInfo)));
      Location location = fhirResources.getLocation(loggedInInfo);
      if (location != null) {
        userLogin.addContext("location", fhirResources.getString(location));
      }
      userLogin.addContext("practitioner",
          fhirResources.getString(fhirResources.getPractitioner(loggedInInfo)));
      String language = loggedInInfo.getLocale().getLanguage();
      if (!"en".equals(language) && !"fr".equals(language)) {
        logger.info("unsupported CMS language " + language + ", defaulting to en");
        language = "en";
      }
      userLogin.addContext("parameters", fhirResources.getString(
          fhirResources.getLanguageParameter(UUID.randomUUID().toString(), language)));

    } catch (CMSException cme) {
      omdGateway.logError(loggedInInfo, "CMS", "userLogin configuration error",
          cme.getLocalizedMessage());
      throw (cme);
    }
    Response hubTopicResponse = omdGateway.doPost(loggedInInfo, createHubTopic, userLogin);
    String hubTopicResponseBody = hubTopicResponse.readEntity(String.class);
    logStatus("userLoginResponse", hubTopicResponse);
    if (hubTopicResponse.getStatus() >= 200 && hubTopicResponse.getStatus() < 300) {
      oneIdGatewayData.setCmsLoggedIn(hubTopicResponseBody);
      oneIdGatewayData.setUpdateUAOInCMS(false);
    } else if (hubTopicResponse.getStatus() >= 400 && hubTopicResponseBody != null) {
      throw new CMSException(hubTopicResponseBody);
    } else {
      throw new CMSException();
    }
    return null;
  }

  @Nullable
  public static String organizationChange(LoggedInInfo loggedInInfo) throws Exception {
    OneIdGatewayData oneIdGatewayData = loggedInInfo.getOneIdGatewayData();
    if (oneIdGatewayData.getCmsLoggedIn() == null) {
      return null;
    }
    FhirResources fhirResources = new FhirResources();
    OmdGateway omdGateway = new OmdGateway();
    WebClient createHubTopic = omdGateway.getWebClientWholeURL(loggedInInfo,
        oneIdGatewayData.getCmsUrl());
    String uuid = UUID.randomUUID().toString();
    Event event = new Event(uuid, oneIdGatewayData.getHubTopic(), "OH.Organization-change");
    event.addContext("organization",
        fhirResources.getString(fhirResources.getOrganization(loggedInInfo)));
    // The custodian the provider acts for has changed. The clinic they are working from has not,
    // but it is re-sent with it so the pair in context always belongs together.
    Location location = fhirResources.getLocation(loggedInInfo);
    if (location != null) {
      event.addContext("location", fhirResources.getString(location));
    }
    Response hubTopicResponse = omdGateway.doPost(loggedInInfo, createHubTopic, event);
    String hubTopicResponseBody = hubTopicResponse.readEntity(String.class);
    logStatus("OH.Organization-change", hubTopicResponse);
    if (hubTopicResponse.getStatus() >= 200 && hubTopicResponse.getStatus() < 300) {
      oneIdGatewayData.setUpdateUAOInCMS(false);
    } else if (hubTopicResponse.getStatus() >= 400 && hubTopicResponseBody != null) {
      throw new CMSException(hubTopicResponseBody);
    } else {
      throw new CMSException();
    }
    return null;
  }

  @Nullable
  public static String patientOpen(LoggedInInfo loggedInInfo, int demographicNo) throws Exception {
    DemographicDao demographicDao = SpringUtils.getBean(DemographicDao.class);
    Demographic demographic = demographicDao.getDemographicById(demographicNo);
    FhirResources fhirResources = new FhirResources();
    OmdGateway omdGateway = new OmdGateway();
    OneIdGatewayData oneIdGatewayData = loggedInInfo.getOneIdGatewayData();
    if (oneIdGatewayData.getCmsLoggedIn() == null) {
      userLogin(loggedInInfo);
    }
    WebClient createHubTopic = omdGateway.getWebClientWholeURL(loggedInInfo,
        oneIdGatewayData.getCmsUrl());
    String uuid = UUID.randomUUID().toString();
    Event event = new Event(uuid, oneIdGatewayData.getHubTopic(), "Patient-open");
    try {
      event.addContext("patient",
          fhirResources.getString(fhirResources.getPatient(demographic)));
    } catch (CMSException cme) {
      omdGateway.logError(loggedInInfo, "CMS", "Patient-open error", cme.getLocalizedMessage());
      throw (cme);
    }
    Response hubTopicResponse = omdGateway.doPost(loggedInInfo, createHubTopic, event);
    String hubTopicResponseBody = hubTopicResponse.readEntity(String.class);
    logStatus("patientOpen", hubTopicResponse);

    if (hubTopicResponse.getStatus() >= 200 && hubTopicResponse.getStatus() < 300) {
      oneIdGatewayData.setCmsPatientInContext("" + demographicNo);
    } else if (hubTopicResponse.getStatus() >= 400 && hubTopicResponseBody != null) {
      throw new CMSException(hubTopicResponseBody);
    } else {
      throw new CMSException();
    }
    return null;
  }

  public static String patientClose(LoggedInInfo loggedInInfo, int demographicNo) throws Exception {
    DemographicDao demographicDao = SpringUtils.getBean(DemographicDao.class);
    Demographic demographic = demographicDao.getDemographicById(demographicNo);
    FhirResources fhirResources = new FhirResources();
    OmdGateway omdGateway = new OmdGateway();
    OneIdGatewayData oneIdGatewayData = loggedInInfo.getOneIdGatewayData();
    // Same lock as patientChange: the close must not interleave with a concurrent
    // open for another patient in the same session.
    synchronized (contextLock(loggedInInfo)) {
      if (oneIdGatewayData.getHubTopic() == null) {
        createHubTopic(loggedInInfo);
      }
      WebClient createHubTopic = omdGateway.getWebClientWholeURL(loggedInInfo,
          oneIdGatewayData.getCmsUrl());
      String uuid = UUID.randomUUID().toString();
      Event event = new Event(uuid, oneIdGatewayData.getHubTopic(), "Patient-close");
      event.addContext("patient",
          fhirResources.getString(fhirResources.getPatient(demographic)));
      Response hubTopicResponse = omdGateway.doPost(loggedInInfo, createHubTopic, event);
      String hubTopicResponseBody = hubTopicResponse.readEntity(String.class);
      logStatus("patientClose", hubTopicResponse);
      if (hubTopicResponse.getStatus() >= 200 && hubTopicResponse.getStatus() < 300) {
        oneIdGatewayData.setCmsPatientInContext(null);
      } else if (hubTopicResponse.getStatus() >= 400 && hubTopicResponseBody != null) {
        throw new CMSException(hubTopicResponseBody);
      } else {
        throw new CMSException();
      }
      return null;
    }
  }

  /**
   * Makes the given patient the one in CMS context: opens it when no patient is in context, or
   * closes the current patient and opens the new one when a different patient is in context. When
   * the acting provider's UAO has changed since the CMS login, the organization context is
   * refreshed first so the CMS reflects the current authority.
   *
   * @param loggedInInfo LoggedInInfo the acting provider session
   * @param demographicNo int the patient to put in context
   * @throws Exception CMSException when the CMS does not acknowledge the context change
   */
  public static void patientChange(LoggedInInfo loggedInInfo, int demographicNo) throws Exception {
    OneIdGatewayData oneIdGatewayData = loggedInInfo.getOneIdGatewayData();
    if (oneIdGatewayData == null) {
      // No ONE ID session, so there is no context to move. Raised as a CMS refusal because that is
      // what the callers here already handle; left to fall through it is a NullPointerException,
      // which reaches the clinician as a server error instead of a prompt to sign in.
      throw new CMSException("No ONE ID session. Sign in to ONE ID and try again.");
    }
    // The read-then-write of the patient context must not interleave between concurrent
    // requests in one session, or two launches can leave the wrong patient in context. A caller
    // whose next step depends on the context holds the same lock across both.
    synchronized (contextLock(loggedInInfo)) {
      if (oneIdGatewayData.getCmsLoggedIn() != null && oneIdGatewayData.isUpdateUAOInCMS()) {
        organizationChange(loggedInInfo);
      }
      String patientInContext = oneIdGatewayData.getCmsPatientInContext();
      if (patientInContext == null) {
        patientOpen(loggedInInfo, demographicNo);
      } else if (Integer.parseInt(patientInContext) != demographicNo) {
        patientClose(loggedInInfo, Integer.parseInt(patientInContext));
        patientOpen(loggedInInfo, demographicNo);
      }
    }
  }

  @Nullable
  public static String consentTargetChange(
      LoggedInInfo loggedInInfo,
      int demographicNo,
      String param
  ) throws Exception {
    OneIdGatewayData oneIdGatewayData = loggedInInfo.getOneIdGatewayData();
    patientChange(loggedInInfo, demographicNo);
    OmdGateway omdGateway = new OmdGateway();
    FhirResources fhirResources = new FhirResources();
    WebClient createHubTopic = omdGateway.getWebClientWholeURL(loggedInInfo,
        oneIdGatewayData.getCmsUrl());
    String uuid = UUID.randomUUID().toString();
    Event event = new Event(uuid, oneIdGatewayData.getHubTopic(), "OH.consentTargetChange");

    event.addContext("parameters", fhirResources.getString(
        fhirResources.getConsentTargetParameter(UUID.randomUUID().toString(),
            param)));
    Response hubTopicResponse = omdGateway.doPost(loggedInInfo, createHubTopic, event);
    String hubTopicResponseBody = hubTopicResponse.readEntity(String.class);
    logStatus("consentTargetChange", hubTopicResponse);
    if (hubTopicResponse.getStatus() >= 200 && hubTopicResponse.getStatus() < 300) {
      return null;
    } else if (hubTopicResponse.getStatus() >= 400 && hubTopicResponseBody != null) {
      throw new CMSException(hubTopicResponseBody);
    } else {
      throw new CMSException();
    }
  }

  @Nullable
  public static String legacyLaunch(
      LoggedInInfo loggedInInfo,
      int demographicNo,
      String param
  )
      throws Exception {
    OneIdGatewayData oneIdGatewayData = loggedInInfo.getOneIdGatewayData();
    patientChange(loggedInInfo, demographicNo);
    OmdGateway omdGateway = new OmdGateway();
    FhirResources fhirResources = new FhirResources();
    WebClient createHubTopic = omdGateway.getWebClientWholeURL(loggedInInfo,
        oneIdGatewayData.getCmsUrl());
    String uuid = UUID.randomUUID().toString();
    Event event = new Event(uuid, oneIdGatewayData.getHubTopic(), "OH.legacyLaunch");
    event.addContext("parameters", fhirResources.getString(
        fhirResources.getContextSessionIdParameter(UUID.randomUUID().toString(),
            param)));
    Response hubTopicResponse = omdGateway.doPost(loggedInInfo, createHubTopic, event);
    String hubTopicResponseBody = hubTopicResponse.readEntity(String.class);
    logStatus("legacyLaunch", hubTopicResponse);
    if (hubTopicResponse.getStatus() >= 200 && hubTopicResponse.getStatus() < 300) {
      return null;
    } else if (hubTopicResponse.getStatus() >= 400 && hubTopicResponseBody != null) {
      throw new CMSException(hubTopicResponseBody);
    } else {
      throw new CMSException();
    }
  }



  @Nullable
  //Should check to see if the user is logged in. Also check to see if a patient is still in context
  public static String userLogout(LoggedInInfo loggedInInfo) throws Exception {
    if (loggedInInfo != null) {
      OmdGateway omdGateway = new OmdGateway();
      OneIdGatewayData oneIdGatewayData = loggedInInfo.getOneIdGatewayData();
      if (oneIdGatewayData != null && oneIdGatewayData.getHubTopic() != null) {
        //TODO:Need to check if a patient is still in context
        WebClient createHubTopic = omdGateway.getWebClientWholeURL(loggedInInfo,
            oneIdGatewayData.getCmsUrl());
        Response hubTopicResponse = omdGateway.doPost(loggedInInfo, createHubTopic,
            new Event(UUID.randomUUID().toString(), oneIdGatewayData.getHubTopic(), "userLogout"));
        logStatus("userLogout", hubTopicResponse);
      }
    }
    return null;
  }

  /**
   * Records the outcome of a CMS call without its response body. When the CMS refuses a context
   * change its body quotes back the patient just sent, so only the status is logged.
   *
   * @param label String the call being recorded
   * @param response Response the response returned by the CMS
   */
  private static void logStatus(String label, Response response) {
    logger.debug(label + ": status " + response.getStatus());
  }

}
