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

package org.oscarehr.PMmodule.web;

import com.quatro.service.LookupManager;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.logging.log4j.Logger;
import org.oscarehr.PMmodule.dao.ProgramDao;
import org.oscarehr.PMmodule.dao.ProviderDao;
import org.oscarehr.PMmodule.dao.VacancyDao;
import org.oscarehr.PMmodule.dao.VacancyTemplateDao;
import org.oscarehr.PMmodule.model.Program;
import org.oscarehr.PMmodule.service.*;
import org.oscarehr.PMmodule.wlmatch.MatchingManager;
import org.oscarehr.casemgmt.service.CaseManagementManager;
import org.oscarehr.common.dao.*;
import org.oscarehr.common.model.Admission;
import org.oscarehr.common.model.CdsClientForm;
import org.oscarehr.common.model.OcanStaffForm;
import org.oscarehr.common.model.Provider;
import org.oscarehr.managers.BedDemographicManager;
import org.oscarehr.managers.BedManager;
import org.oscarehr.managers.RoomDemographicManager;
import org.oscarehr.managers.RoomManager;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SpringUtils;
import org.springframework.beans.factory.annotation.Required;
import oscar.OscarProperties;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ClientManagerAction {

    private static final Logger logger = MiscUtils.getLogger();

    private HealthSafetyManager healthSafetyManager;
    private ClientRestrictionManager clientRestrictionManager;
    private SurveyManager surveyManager = (SurveyManager) SpringUtils.getBean(SurveyManager.class);
    private LookupManager lookupManager;
    private CaseManagementManager caseManagementManager;
    private AdmissionManager admissionManager;
    private GenericIntakeManager genericIntakeManager;
    private BedDemographicManager bedDemographicManager = SpringUtils.getBean(BedDemographicManager.class);
    private BedManager bedManager = SpringUtils.getBean(BedManager.class);
    private ClientManager clientManager;
    private ProgramManager programManager;
    private ProviderManager providerManager;
    private ProgramQueueManager programQueueManager;
    private IntegratorConsentDao integratorConsentDao;
    private CdsClientFormDao cdsClientFormDao;
    private static AdmissionDao admissionDao = (AdmissionDao) SpringUtils.getBean(AdmissionDao.class);
    private static ProviderDao providerDao = (ProviderDao) SpringUtils.getBean(ProviderDao.class);
    private static ProgramDao programDao = (ProgramDao) SpringUtils.getBean(ProgramDao.class);
    private OcanStaffFormDao ocanStaffFormDao = (OcanStaffFormDao) SpringUtils.getBean(OcanStaffFormDao.class);
    private RemoteReferralDao remoteReferralDao = (RemoteReferralDao) SpringUtils.getBean(RemoteReferralDao.class);
    private VacancyDao vacancyDao = (VacancyDao) SpringUtils.getBean(VacancyDao.class);
    private VacancyTemplateDao vacancyTemplateDao = (VacancyTemplateDao) SpringUtils.getBean(VacancyTemplateDao.class);
    private MatchingManager matchingManager = new MatchingManager();

    private RoomDemographicManager roomDemographicManager = SpringUtils.getBean(RoomDemographicManager.class);
    private RoomManager roomManager = SpringUtils.getBean(RoomManager.class);

    public static String getEscapedAdmissionSelectionDisplay(int admissionId) {
        Admission admission = admissionDao.getAdmission((long) admissionId);

        StringBuilder sb = new StringBuilder();
        if (admission != null) {
            sb.append(admission.getProgramName());
            sb.append(" ( ");
            sb.append(DateFormatUtils.ISO_DATE_FORMAT.format(admission.getAdmissionDate()));
            sb.append(" - ");
            if (admission.getDischargeDate() == null) sb.append("current");
            else sb.append(DateFormatUtils.ISO_DATE_FORMAT.format(admission.getDischargeDate()));
            sb.append(" )");
        }
        return (StringEscapeUtils.escapeHtml(sb.toString()));
    }

    public static String getEscapedProviderDisplay(String providerNo) {
        Provider provider = providerDao.getProvider(providerNo);

        return (StringEscapeUtils.escapeHtml(provider.getFormattedName()));
    }

    public static String getEscapedDateDisplay(Date d) {
        String display = DateFormatUtils.ISO_DATE_FORMAT.format(d);

        return (StringEscapeUtils.escapeHtml(display));
    }

    @Required
    public void setClientRestrictionManager(ClientRestrictionManager clientRestrictionManager) {
        this.clientRestrictionManager = clientRestrictionManager;
    }

    public void setHealthSafetyManager(HealthSafetyManager healthSafetyManager) {
        this.healthSafetyManager = healthSafetyManager;
    }

    public void setLookupManager(LookupManager lookupManager) {
        this.lookupManager = lookupManager;
    }

    public void setCaseManagementManager(CaseManagementManager caseManagementManager) {
        this.caseManagementManager = caseManagementManager;
    }

    public void setAdmissionManager(AdmissionManager mgr) {
        this.admissionManager = mgr;
    }

    public void setGenericIntakeManager(GenericIntakeManager genericIntakeManager) {
        this.genericIntakeManager = genericIntakeManager;
    }

    public void setBedDemographicManager(BedDemographicManager demographicBedManager) {
        this.bedDemographicManager = demographicBedManager;
    }

    public void setRoomDemographicManager(RoomDemographicManager roomDemographicManager) {
        this.roomDemographicManager = roomDemographicManager;
    }

    public void setBedManager(BedManager bedManager) {
        this.bedManager = bedManager;
    }

    public void setClientManager(ClientManager mgr) {
        this.clientManager = mgr;
    }

    public void setProgramManager(ProgramManager mgr) {
        this.programManager = mgr;
    }

    public void setProgramQueueManager(ProgramQueueManager mgr) {
        this.programQueueManager = mgr;
    }

    public void setProviderManager(ProviderManager mgr) {
        this.providerManager = mgr;
    }

    public void setRoomManager(RoomManager roomManager) {
        this.roomManager = roomManager;
    }

    private void populateCdsData(HttpServletRequest request, Integer demographicNo, Integer facilityId) {
        List<Admission> admissions = admissionDao.getAdmissions(demographicNo);
        List<Program> domain = null;


        ArrayList<CdsClientForm> allLatestCdsForms = new ArrayList<CdsClientForm>();

        boolean restrict = "true".equals(OscarProperties.getInstance().getProperty("caisi.cds.restrict_by_program_domain", "false"));
        if (restrict) {
            domain = programManager.getProgramDomain(LoggedInInfo.getLoggedInInfoFromSession(request).getLoggedInProviderNo());
        }

        for (Admission admission : admissions) {
            CdsClientForm cdsClientForm = cdsClientFormDao.findLatestByFacilityAdmissionId(facilityId, admission.getId().intValue(), null);
            if (cdsClientForm != null) {
                if (restrict) {
                    if (isAdmissionInDomain(admission, domain)) {
                        allLatestCdsForms.add(cdsClientForm);
                    }
                } else {
                    allLatestCdsForms.add(cdsClientForm);
                }
            }
        }

        request.setAttribute("allLatestCdsForms", allLatestCdsForms);
    }

    private boolean isAdmissionInDomain(Admission admission, List<Program> domain) {
        for (Program p : domain) {
            if (p.getId().intValue() == admission.getProgramId().intValue()) {
                return true;
            }
        }
        return false;
    }

    public static String getCdsProgramDisplayString(CdsClientForm cdsClientForm) {
        Admission admission = admissionDao.getAdmission(cdsClientForm.getAdmissionId());
        Program program = programDao.getProgram(admission.getProgramId());

        String displayString = program.getName() + " : " + DateFormatUtils.ISO_DATE_FORMAT.format(admission.getAdmissionDate());
        return (StringEscapeUtils.escapeHtml(displayString));
    }

    public static String getCbiProgramDisplayString(OcanStaffForm ocanStaffForm) {
        Admission admission = admissionDao.getAdmission(ocanStaffForm.getAdmissionId());
        Program program = programDao.getProgram(admission.getProgramId());

        String displayString = program.getName() + " : " + DateFormatUtils.ISO_DATE_FORMAT.format(admission.getAdmissionDate());
        return (StringEscapeUtils.escapeHtml(displayString));
    }
}
