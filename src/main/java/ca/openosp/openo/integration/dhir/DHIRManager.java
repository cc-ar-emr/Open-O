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
package ca.openosp.openo.integration.dhir;

import ca.openosp.openo.PMmodule.dao.SecUserRoleDao;
import ca.openosp.openo.PMmodule.model.SecUserRole;
import ca.openosp.openo.commn.dao.DHIRTransactionLogDao;
import ca.openosp.openo.commn.dao.SecObjPrivilegeDao;
import ca.openosp.openo.commn.dao.SystemPreferencesDao;
import ca.openosp.openo.commn.model.DHIRTransactionLog;
import ca.openosp.openo.commn.model.Demographic;
import ca.openosp.openo.commn.model.OMDGatewayTransactionLog;
import ca.openosp.openo.commn.model.OscarMsgType;
import ca.openosp.openo.commn.model.SecObjPrivilege;
import ca.openosp.openo.commn.model.SystemPreferences;
import ca.openosp.openo.integration.dhdr.AuditInfo;
import ca.openosp.openo.integration.dhdr.OmdGateway;
import ca.openosp.openo.integration.oneId.TokenExpiredException;
import ca.openosp.openo.managers.MessagingManager;
import ca.openosp.openo.messenger.data.MessengerSystemMessage;
import ca.openosp.openo.messenger.data.MsgMessageData;
import ca.openosp.openo.messenger.data.MsgProviderData;
import ca.openosp.openo.utility.LoggedInInfo;
import ca.openosp.openo.utility.MiscUtils;
import ca.openosp.openo.utility.SpringUtils;
import ca.uhn.fhir.context.FhirContext;
import org.apache.cxf.jaxrs.client.WebClient;

import org.apache.logging.log4j.Logger;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

public class DHIRManager extends OmdGateway {

  private static HashSet<String> doNotSentMsgForOuttage = new HashSet<String>();
  Logger logger = MiscUtils.getLogger();
  private final DHIRTransactionLogDao dhirTransactionLogDao = SpringUtils.getBean(
      DHIRTransactionLogDao.class);
  private final SystemPreferencesDao systemPreferencesDao = SpringUtils.getBean(
      SystemPreferencesDao.class);
  private static final MessagingManager messagingManager = SpringUtils.getBean(MessagingManager.class);

  protected static void notifyDHIRError(LoggedInInfo loggedInInfo, String errorMsg) {
    HashSet<String> sendToProviderList = new HashSet<String>();

    if (loggedInInfo != null && loggedInInfo.getLoggedInProvider() != null) {
      String providerNoTemp = loggedInInfo.getLoggedInProviderNo();
      if (!doNotSentMsgForOuttage.contains(providerNoTemp)) {
        sendToProviderList.add(providerNoTemp);
      }
    }

    SecObjPrivilegeDao secObjPrivilegeDao = SpringUtils.getBean(SecObjPrivilegeDao.class);
    SecUserRoleDao secUserRoleDao = SpringUtils.getBean(SecUserRoleDao.class);

    for (SecObjPrivilege sop : secObjPrivilegeDao.findByObjectName("_dhir.administrator")) {
      if ("x".equals(sop.getPrivilege()) || "w".equals(sop.getPrivilege()) || "r".equals(
          sop.getPrivilege())) {
        for (SecUserRole sur : secUserRoleDao.getSecUserRolesByRoleName(
            sop.getId().getRoleUserGroup())) {
          if (sur.getActive()) {

            if (!doNotSentMsgForOuttage.contains(sur.getProviderNo())) {
              sendToProviderList.add(sur.getProviderNo());
            }
          }
        }

      }
    }

    if (sendToProviderList.size() == 0) {
      String providerNoTemp = "999998";
      if (!doNotSentMsgForOuttage.contains(providerNoTemp)) {
        sendToProviderList.add(providerNoTemp);
      }
    }

    if (sendToProviderList.size() == 0) {
      return;
    }

    String message = "OSCAR attempted to communicate with DHIR at " + new Date()
        + " but there was an error during the task.\n\nSee below and DHIR log for further details:\n"
        + errorMsg;


    MessengerSystemMessage messengerSystemMessage = new MessengerSystemMessage(sendToProviderList.toArray(new String[0]));
    messengerSystemMessage.setSubject("DHIR Communication Error");
    messengerSystemMessage.setMessage(message);
    messagingManager.sendSystemMessage(loggedInInfo, messengerSystemMessage);

  }

  public Response submitImmunizations(LoggedInInfo loggedInInfo, String bundleJSON,
      Integer demographicNo, String uuid) throws Exception {
    String submissionURL = systemPreferencesDao.findPreferenceByName(SystemPreferences.ONEID_KEYS.dhir_submissionUrl).getName();
    WebClient wc = getWebClientWholeURL(loggedInInfo, submissionURL);

    String consumerKey = getConsumerKey();
    String consumerSecret = systemPreferencesDao.findPreferenceByName(SystemPreferences.ONEID_KEYS.oag_client_secret).getName();
    if (loggedInInfo.getOneIdGatewayData().isAccessTokenExpired()) {
      throw new TokenExpiredException();
    }
    String accessToken = loggedInInfo.getOneIdGatewayData().getAccessToken();
    String externalSystem = "DHIR";
    String transactionType = "SUBMISSION";

    OMDGatewayTransactionLog omdGatewayTransactionLog = getOMDGatewayTransactionLog(loggedInInfo,
        demographicNo, externalSystem, transactionType);
    omdGatewayTransactionLog.setDataSent(bundleJSON);
    omdGatewayTransactionLog.setxGtwyClientId(consumerKey);
    transactionLogDao.persist(omdGatewayTransactionLog);
    Response response2;
    try {
      response2 = wc.header("Authorization", "Bearer " + accessToken)
          .header("X-Gtwy-Client-Id", consumerKey)
          .header("X-Gtwy-Client-Secret", consumerSecret)
          .header("X-Request-Id", uuid)
          .header("Content-Type", "application/json")
          .post(bundleJSON);
      completeLog(omdGatewayTransactionLog, response2, true);
      transactionLogDao.merge(omdGatewayTransactionLog);
    } catch (Exception e) {
      e.getMessage();
      omdGatewayTransactionLog.setError(e.getLocalizedMessage());
      transactionLogDao.merge(omdGatewayTransactionLog);
      throw (e);
    }
    return response2;
  }

  public Bundle search(HttpServletRequest request, Demographic demographic, Date startDate,
                       Date endDate, List<String> searchParamsUsed) throws Exception {
    String hin = demographic.getHin();
    String dob = demographic.getFormattedDob();
    String gender = demographic.getSex();
    String lastName = demographic.getLastName();
    String firstName = demographic.getFirstName();

    List<OperationOutcome> outcomes = new ArrayList<OperationOutcome>();

    Bundle bundle = this.getImmunizationsByHINAndDOB(request, demographic.getDemographicNo(), hin,
        dob, startDate, endDate, outcomes);
    searchParamsUsed.add("HIN:" + hin);
    searchParamsUsed.add("Date Of Birth:" + dob);

    if (outcomes.isEmpty() && bundle != null) {
      return bundle;
    }

    if (!outcomes.isEmpty()) {
      OperationOutcome outcome = outcomes.get(0);
      if ("OperationOutcome/multiple.records".equals(outcome.getId())) {
        logger.info("multiple.records found..trying with gender and name as well");
        outcomes.clear();
        bundle = this.getImmunizationsByHINAndDOBAndGenderAndName(request,
            demographic.getDemographicNo(), hin, dob, gender, lastName, firstName, startDate,
            endDate, outcomes);
        searchParamsUsed.add("Gender:" + gender);
        searchParamsUsed.add("Last name:" + lastName);
        searchParamsUsed.add("First name:" + firstName);

        if (outcomes.isEmpty() && bundle != null) {
          return bundle;
        }

        if (!outcomes.isEmpty()) {
          outcome = outcomes.get(0);
          logger.info(outcome.getId());
        }


      }
    }

    if (!outcomes.isEmpty()) {
      OperationOutcome outcome = outcomes.get(0);
      for (OperationOutcomeIssueComponent ooic : outcome.getIssue()) {
        if (ooic.getDetails() != null) {
          throw new DHIRException(ooic.getDetails().getText());
        }
      }
      logger.error(outcome);
      throw new DHIRException("An Unknown Error Occurred");
    }
    return bundle;
  }

  public Bundle getImmunizationsByHINAndDOBAndGenderAndName(HttpServletRequest request,
      Integer demographicNo, String hin, String dob, String gender, String lastName,
      String firstName, Date startDate, Date endDate, List<OperationOutcome> outcomes)
      throws Exception {
    LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
    String dhirEndpoint = systemPreferencesDao.findPreferenceByName(SystemPreferences.ONEID_KEYS.dhir_endpoint).getName();

    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
    FhirContext ctx = FhirContext.forR4();

    logger.info("searching DHIR by HIN, DOB, Gender, Name");

    WebClient wc = getWebClient(loggedInInfo, dhirEndpoint);

    wc.query("patient.identifier",
        "http://ehealthontario.ca/fhir/NamingSystem/ca-on-patient-hcn|" + hin);
    wc.query("patient.birthdate", dob);
    wc.query("patient.gender", mapGender(gender));
    wc.query("patient.family", lastName);
    wc.query("patient.given", firstName);
    wc.query("_include", "Immunization:patient");
    wc.query("_include", "Immunization:performer");
    wc.query("_revinclude:recurse", "ImmunizationRecommendation:patient");
    wc.query("_format", "application/fhir+json");

    if (startDate != null && endDate == null) {
      wc.query("date", "ge" + fmt.format(startDate));
    }
    if (startDate == null && endDate != null) {
      wc.query("date", "le" + fmt.format(endDate));
    }
    if (startDate != null && endDate != null) {
      wc.query("date", "ge" + fmt.format(startDate), "le" + fmt.format(endDate));
    }

    DHIRTransactionLog log = generateInitialLog(demographicNo, loggedInInfo.getLoggedInProviderNo(),
        "IMMUNIZATION.READ");
    dhirTransactionLogDao.persist(log);

    AuditInfo auditInfo = new AuditInfo(AuditInfo.DHIR, AuditInfo.RETRIEVAL, demographicNo);
    Response response2 = doGet(loggedInInfo, wc, auditInfo);
    String body = response2.readEntity(String.class);

    completeLog(log, response2, body);
    dhirTransactionLogDao.merge(log);

    if (response2.getStatus() >= 200 && response2.getStatus() < 300) {
      Bundle bundle = (Bundle) ctx.newJsonParser().parseResource(body);
      hasConsentBlock(bundle);
      outcomes.addAll(hasOperationOutcome(bundle));
      return bundle;
    } else if (response2.getStatus() >= 400 && response2.getStatus() < 600 && body != null) {
      OperationOutcome outcome = ctx.newJsonParser().parseResource(OperationOutcome.class, body);
      if (outcome != null) {
        outcomes.add(outcome);
      } else {
        notifyDHIRError(loggedInInfo,
            "An error occurred retrieving the data. (" + response2.getStatus() + ":" + (
                (body != null) ? body : "") + ")");
        throw new DHIRException(
            "An error occurred retrieving the data. (" + response2.getStatus() + ":" + (
                (body != null) ? body : "") + ")");
      }
    } else {
      logger.error(response2.getStatus());
      logger.error(body);
      notifyDHIRError(loggedInInfo,
          "An error occurred retrieving the data. (" + response2.getStatus() + ":" + ((body != null)
              ? body : "") + ")");
      throw new DHIRException(
          "An error occurred retrieving the data. (" + response2.getStatus() + ":" + ((body != null)
              ? body : "") + ")");
    }
    return null;
  }

  public Bundle getImmunizationsByHINAndDOB(HttpServletRequest request, Integer demographicNo,
      String hin, String dob, Date startDate, Date endDate, List<OperationOutcome> outcomes)
      throws Exception {
    LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
    String dhirEndpoint = systemPreferencesDao.findPreferenceByName(SystemPreferences.ONEID_KEYS.dhir_endpoint).getName();

    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
    FhirContext ctx = FhirContext.forR4();

    WebClient wc = getWebClient(loggedInInfo, dhirEndpoint);

    wc.query("patient.identifier",
        "http://ehealthontario.ca/fhir/NamingSystem/ca-on-patient-hcn|" + hin);
    wc.query("patient.birthdate", dob);
    wc.query("_include", "Immunization:patient");
    wc.query("_include", "Immunization:performer");
    wc.query("_revinclude:recurse", "ImmunizationRecommendation:patient");
    wc.query("_format", "application/fhir+json");

    if (startDate != null && endDate == null) {
      wc.query("date", "ge" + fmt.format(startDate));
    }
    if (startDate == null && endDate != null) {
      wc.query("date", "le" + fmt.format(endDate));
    }
    if (startDate != null && endDate != null) {
      wc.query("date", "ge" + fmt.format(startDate), "le" + fmt.format(endDate));
    }

    DHIRTransactionLog log = generateInitialLog(demographicNo, loggedInInfo.getLoggedInProviderNo(),
        "IMMUNIZATION.READ");
    dhirTransactionLogDao.persist(log);

    AuditInfo auditInfo = new AuditInfo(AuditInfo.DHIR, AuditInfo.RETRIEVAL, demographicNo);
    Response response2 = doGet(loggedInInfo, wc, auditInfo);
    String body = response2.readEntity(String.class);

    completeLog(log, response2, body);
    dhirTransactionLogDao.merge(log);

    if (response2.getStatus() >= 200 && response2.getStatus() < 300) {
      Bundle bundle = (Bundle) ctx.newJsonParser().parseResource(body);
      hasConsentBlock(bundle);
      outcomes.addAll(hasOperationOutcome(bundle));
      return bundle;
    } else if (response2.getStatus() >= 400 && response2.getStatus() < 600 && body != null) {
      logger.info("got status 400, returning null, and passing back outcome");
      logger.info("body=" + body);
      OperationOutcome outcome = ctx.newJsonParser().parseResource(OperationOutcome.class, body);
      if (outcome != null) {
        outcomes.add(outcome);
      } else {
        notifyDHIRError(loggedInInfo,
            "An error occurred retrieving the data. (" + response2.getStatus() + ":" + (
                (body != null) ? body : "") + ")");
        throw new DHIRException(
            "An error occurred retrieving the data. (" + response2.getStatus() + ":" + (
                (body != null) ? body : "") + ")");
      }
    } else {
      logger.error("status=" + response2.getStatus());
      logger.error("body=" + body);
      notifyDHIRError(loggedInInfo,
          "An error occurred retrieving the data. (" + response2.getStatus() + ":" + ((body != null)
              ? body : "") + ")");
      throw new DHIRException(
          "An error occurred retrieving the data. (" + response2.getStatus() + ":" + ((body != null)
              ? body : "") + ")");

    }
    return null;
  }

  private boolean hasConsentBlock(Bundle bundle) throws ConsentBlockException {
    for (BundleEntryComponent comp : bundle.getEntry()) {
      Resource resource = comp.getResource();
      if (resource.getResourceType() == ResourceType.OperationOutcome) {
        OperationOutcome oo = (OperationOutcome) resource;
        for (OperationOutcomeIssueComponent issue : oo.getIssue()) {
          if (issue.getCode() == IssueType.SUPPRESSED) {
            if (issue.getDetails() != null && issue.getDetails().getText() != null) {
              throw new ConsentBlockException(issue.getDetails().getText());
            } else {
              throw new ConsentBlockException();
            }
          }
        }
      }
    }
    return false;
  }

  private String mapGender(String sex) {
    if ("m".equalsIgnoreCase(sex)) {
      return "male";
    }
    if ("f".equalsIgnoreCase(sex)) {
      return "female";
    }
    if ("o".equalsIgnoreCase(sex)) {
      return "other";
    }
    if ("u".equalsIgnoreCase(sex)) {
      return "unknown";
    }
    return "unknown";
  }

  private DHIRTransactionLog generateInitialLog(Integer demographicNo, String providerNo,
      String transactionType) {
    DHIRTransactionLog log = new DHIRTransactionLog();
    log.setDemographicNo(demographicNo);
    log.setExternalSystem("DHIR");
    log.setInitiatingProviderNo(providerNo);
    log.setStarted(new Date());
    log.setTransactionType(transactionType);
    return log;
  }

  private void completeLog(DHIRTransactionLog log, Response response2, String body) {
    log.setResultCode(response2.getStatus());
    log.setSuccess(true);

    if (response2.getStatus() >= 300) {
      log.setError(body);
      log.setSuccess(false);
    }

    String headers = "";
    for (String headerName : response2.getHeaders().keySet()) {
      headers += headerName + ":" + response2.getHeaderString(headerName) + "\n";
    }
    log.setHeaders(headers);
  }
}
