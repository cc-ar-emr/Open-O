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

import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.Logger;
import org.oscarehr.PMmodule.exception.AdmissionException;
import org.oscarehr.PMmodule.exception.ProgramFullException;
import org.oscarehr.PMmodule.exception.ServiceRestrictionException;
import org.oscarehr.PMmodule.model.Intake;
import org.oscarehr.PMmodule.model.IntakeNodeJavascript;
import org.oscarehr.PMmodule.model.Program;
import org.oscarehr.PMmodule.service.*;
import org.oscarehr.PMmodule.web.formbean.GenericIntakeEditFormBean;
import org.oscarehr.casemgmt.dao.ClientImageDAO;
import org.oscarehr.casemgmt.service.CaseManagementManager;
import org.oscarehr.common.model.Admission;
import org.oscarehr.common.model.Demographic;
import org.oscarehr.common.model.JointAdmission;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SpringUtils;
import oscar.OscarProperties;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class GenericIntakeEditAction {

    private static Logger LOG = MiscUtils.getLogger();
    // Forwards
    private static final String EDIT = "edit";
    private static final String PRINT = "print";
    private static final String CLIENT_EDIT = "clientEdit";
    private static final String EFORM_ADD = "eformAdd";
    private static final String APPT = "appointment";
    protected static final String CLIENT_EDIT_ID = "id";

    private ClientImageDAO clientImageDAO = null;
    private SurveyManager surveyManager = (SurveyManager) SpringUtils.getBean("surveyManager2");
    //private IMatchManager matchManager = new MatchManager();

    protected static final String PROGRAM_ID = "programId";
    protected static final String TYPE = "type";
    protected static final String CLIENT = "client";
    protected static final String APPOINTMENT = "appointment";
    protected static final String DEMOGRAPHIC_NO = "demographic_no";
    protected static final String FORM_ID = "fid";
    protected static final String CLIENT_ID = "clientId";
    protected static final String INTAKE_ID = "intakeId";


    private GenericIntakeManager genericIntakeManager;
    protected ClientManager clientManager;
    protected ProgramManager programManager;
    protected AdmissionManager admissionManager;
    protected CaseManagementManager caseManagementManager;


    public void setGenericIntakeManager(GenericIntakeManager genericIntakeManager) {
        this.genericIntakeManager = genericIntakeManager;
    }

    protected Integer getProgramId(HttpServletRequest request) {
        Integer programId = null;

        String programIdParam = request.getParameter(PROGRAM_ID);

        if (programIdParam != null) {
            try {
                programId = Integer.valueOf(programIdParam);
            } catch (NumberFormatException e) {
                LOG.error("Error", e);
            }
        }

        return programId;
    }

    public void setOscarSurveyManager(SurveyManager mgr) {
        this.surveyManager = mgr;
    }

    public void setClientImageDAO(ClientImageDAO clientImageDAO) {
        this.clientImageDAO = clientImageDAO;
    }

    private Set<Program> getActiveProviderPrograms(String providerNo) {
        Set<Program> activeProviderPrograms = new HashSet<Program>();

        for (Program providerProgram : programManager.getProgramDomain(providerNo)) {
            if (providerProgram != null && providerProgram.isActive()) {
                activeProviderPrograms.add(providerProgram);
            }
        }

        return activeProviderPrograms;
    }

    public List<Program> getBedPrograms(Set<Program> providerPrograms, String providerNo) {
        List<Program> bedPrograms = new ArrayList<Program>();

        for (Program program : programManager.getBedPrograms()) {
            if (providerPrograms.contains(program)) {
                if (OscarProperties.getInstance().isTorontoRFQ()) {
                    if (caseManagementManager.hasAccessRight("perform admissions", "access", providerNo, "", String.valueOf(program.getId()))) {
                        bedPrograms.add(program);
                    }
                } else {
                    bedPrograms.add(program);
                }
            }
        }

        return bedPrograms;
    }

    public List<Program> getCommunityPrograms() {
        List<Program> communityPrograms = new ArrayList<Program>();

        for (Program program : programManager.getCommunityPrograms()) {
            communityPrograms.add(program);
        }

        return communityPrograms;
    }

    public List<Program> getServicePrograms(Set<Program> providerPrograms, String providerNo) {
        List<Program> servicePrograms = new ArrayList<Program>();

        for (Object o : programManager.getServicePrograms()) {
            Program program = (Program) o;

            if (providerPrograms.contains(program)) {
                servicePrograms.add(program);
            }
        }

        return servicePrograms;
    }


    private List<Program> getExternalPrograms(Set<Program> providerPrograms) {
        List<Program> externalPrograms = new ArrayList<Program>();

        for (Program program : programManager.getExternalPrograms()) {
            externalPrograms.add(program);
        }
        return externalPrograms;
    }

    private List<Program> getProgramsInDomain(Set<Program> providerPrograms) {
        List<Program> programsInDomain = new ArrayList<Program>();

        for (Program program : providerPrograms) {
            programsInDomain.add(program);
        }
        return programsInDomain;
    }

    private Integer getOscarClinicDefaultCommunityProgramId(String communityProgram) {
        Integer communityProgramId = null;
        communityProgramId = programManager.getProgramIdByProgramName(communityProgram);
        return communityProgramId;
    }

    private Integer getCurrentBedCommunityProgramId(Integer clientId) {
        Integer currentProgramId = null;

        Admission bedProgramAdmission = admissionManager.getCurrentBedProgramAdmission(clientId);
        Admission communityProgramAdmission = admissionManager.getCurrentCommunityProgramAdmission(clientId);

        if (bedProgramAdmission != null) {
            currentProgramId = bedProgramAdmission.getProgramId();
        } else if (communityProgramAdmission != null) {
            currentProgramId = communityProgramAdmission.getProgramId();
        }

        return currentProgramId;
    }

    private Integer getCurrentBedProgramId(Integer clientId) {
        Integer currentProgramId = null;

        Admission bedProgramAdmission = admissionManager.getCurrentBedProgramAdmission(clientId);

        if (bedProgramAdmission != null) {
            currentProgramId = bedProgramAdmission.getProgramId();
        }

        return currentProgramId;
    }

    private Integer getCurrentCommunityProgramId(Integer clientId) {
        Integer currentProgramId = null;

        Admission communityProgramAdmission = admissionManager.getCurrentCommunityProgramAdmission(clientId);

        if (communityProgramAdmission != null) {
            currentProgramId = communityProgramAdmission.getProgramId();
        }

        return currentProgramId;
    }

    private Integer getCurrentExternalProgramId(Integer clientId) {
        Integer currentProgramId = null;

        Admission externalProgramAdmission = admissionManager.getCurrentExternalProgramAdmission(clientId);

        if (externalProgramAdmission != null) {
            currentProgramId = externalProgramAdmission.getProgramId();
        }

        return currentProgramId;
    }

    private SortedSet<Integer> getCurrentServiceProgramIds(Integer clientId) {
        SortedSet<Integer> currentProgramIds = new TreeSet<Integer>();

        List<?> admissions = admissionManager.getCurrentServiceProgramAdmission(clientId);
        if (admissions != null) {
            for (Object o : admissions) {
                Admission serviceProgramAdmission = (Admission) o;
                currentProgramIds.add(serviceProgramAdmission.getProgramId());
            }
        }

        return currentProgramIds;
    }

    private void saveClient(Demographic client, String providerNo) {

        String strSaveMrp = OscarProperties.getInstance().getProperty("caisi.registration_intake.updateMRPOnSave", "true");
        if ("true".equals(strSaveMrp)) {
            client.setProviderNo(providerNo);
        }

        clientManager.saveClient(client);
        //this is slowing things down, and AFAIK waitlist isn't being used anywhere
		/*
		try {
			log.info("Processing client creation event with MatchManager..." + 
					matchManager.<Demographic>processEvent(client, IMatchManager.Event.CLIENT_CREATED));
		} catch (Exception e) {
			MiscUtils.getLogger().error("Error while processing MatchManager.processEvent(Client)",e);
		}
		*/
    }

    private void admitExternalProgram(Integer clientId, String providerNo, Integer externalProgramId) throws ProgramFullException, AdmissionException, ServiceRestrictionException {
        Program externalProgram = null;
        Integer currentExternalProgramId = getCurrentExternalProgramId(clientId);

        if (externalProgramId != null) {
            externalProgram = programManager.getProgram(externalProgramId);
        }

        if (externalProgram != null) {
            if (currentExternalProgramId == null) {
                admissionManager.processAdmission(clientId, providerNo, externalProgram, "intake external discharge", "intake external admit");
            } else if (!currentExternalProgramId.equals(externalProgramId)) {
                /*
                 * if (programManager.getProgram(externalProgramId).isExternal()) { if (externalProgram.isExternal()) { admissionManager.processAdmission(clientId, providerNo,
                 * externalProgram, "intake external discharge", "intake external admit"); } }
                 */
                admissionManager.processDischarge(currentExternalProgramId, clientId, "intake external discharge", "0");
                admissionManager.processAdmission(clientId, providerNo, externalProgram, "intake external discharge", "intake external admit");
            }
        }
    }

    public void admitBedCommunityProgram(Integer clientId, String providerNo, Integer bedCommunityProgramId, String saveWhich, String admissionText, Date admissionDate) throws ProgramFullException,
            AdmissionException, ServiceRestrictionException {
        Program bedCommunityProgram = null;
        Integer currentBedCommunityProgramId = getCurrentBedCommunityProgramId(clientId);

        if (admissionText == null) admissionText = "intake admit";

        if ("RFQ_notAdmit".equals(saveWhich) && bedCommunityProgramId == null && currentBedCommunityProgramId == null) {
            return;
        }
        if (bedCommunityProgramId == null && currentBedCommunityProgramId == null) {
            bedCommunityProgram = programManager.getHoldingTankProgram();
        } else if (bedCommunityProgramId != null) {
            bedCommunityProgram = programManager.getProgram(bedCommunityProgramId);
        }

        boolean isFamilyHead = false;
        boolean isFamilyDependent = false;
        JointAdmission clientsJadm = null;
        List<JointAdmission> dependentList = null;
        Integer[] dependentIds = null;

        if (clientManager != null && clientId != null) {
            dependentList = clientManager.getDependents(Integer.valueOf(clientId));
            clientsJadm = clientManager.getJointAdmission(Integer.valueOf(clientId));
        }
        if (clientsJadm != null && clientsJadm.getHeadClientId() != null) {
            isFamilyDependent = true;
        }
        if (dependentList != null && dependentList.size() > 0) {
            isFamilyHead = true;
        }
        if (dependentList != null) {
            dependentIds = new Integer[dependentList.size()];
            for (int i = 0; i < dependentList.size(); i++) {
                dependentIds[i] = new Integer(dependentList.get(i).getClientId().intValue());
            }
        }

        if (isFamilyDependent) {
            throw new AdmissionException("you cannot admit a dependent family/group member, you must remove the dependent status or admit the family head");

        } else if (isFamilyHead && dependentIds != null && dependentIds.length >= 1) {
            Integer[] familyIds = new Integer[dependentIds.length + 1];
            familyIds[0] = clientId;
            for (int i = 0; i < dependentIds.length; i++) {
                familyIds[i + 1] = dependentIds[i];
            }
            for (int i = 0; i < familyIds.length; i++) {
                Integer familyId = familyIds[i];
                if (bedCommunityProgram != null) {
                    if (currentBedCommunityProgramId == null) {
                        admissionManager.processAdmission(familyId, providerNo, bedCommunityProgram, "intake discharge", admissionText, admissionDate);
                    } else if (!currentBedCommunityProgramId.equals(bedCommunityProgramId)) {
                        if (programManager.getProgram(currentBedCommunityProgramId).isBed()) {
                            if (bedCommunityProgram.isBed()) {
                                admissionManager.processAdmission(familyId, providerNo, bedCommunityProgram, "intake discharge", admissionText, admissionDate);
                            } else {
                                admissionManager.processDischargeToCommunity(bedCommunityProgramId, familyId, providerNo, "intake discharge", "0", admissionDate);
                            }
                        } else {
                            if (bedCommunityProgram.isCommunity()) {
                                admissionManager.processDischargeToCommunity(bedCommunityProgramId, familyId, providerNo, "intake discharge", "0", admissionDate);
                            } else {
                                admissionManager.processDischarge(currentBedCommunityProgramId, familyId, "intake discharge", "0", admissionDate);
                                admissionManager.processAdmission(familyId, providerNo, bedCommunityProgram, "intake discharge", admissionText, admissionDate);
                            }
                        }
                    }
                }
            }

            // throw new AdmissionException(
            // "If you admit the family head, all dependents will also be admitted to this program and discharged from their current programs. Are you sure you wish to proceed?");

        } else {

            if (bedCommunityProgram != null) {
                if (currentBedCommunityProgramId == null) {
                    admissionManager.processAdmission(clientId, providerNo, bedCommunityProgram, "intake discharge", admissionText, admissionDate);
                } else if (!currentBedCommunityProgramId.equals(bedCommunityProgramId)) {
                    if (programManager.getProgram(currentBedCommunityProgramId).isBed()) {
                        if (bedCommunityProgram.isBed()) {
                            // automatic discharge from one bed program to another bed program.
                            admissionManager.processAdmission(clientId, providerNo, bedCommunityProgram, "intake discharge", admissionText, admissionDate);
                        } else {
                            admissionManager.processDischargeToCommunity(bedCommunityProgramId, clientId, providerNo, "intake discharge", "0", admissionDate);
                        }
                    } else {
                        if (bedCommunityProgram.isCommunity()) {
                            admissionManager.processDischargeToCommunity(bedCommunityProgramId, clientId, providerNo, "intake discharge", "0", admissionDate);
                        } else {
                            admissionManager.processDischarge(currentBedCommunityProgramId, clientId, "intake discharge", "0", admissionDate);
                            admissionManager.processAdmission(clientId, providerNo, bedCommunityProgram, "intake discharge", admissionText, admissionDate);
                        }
                    }
                }
            }
        }
    }

    public void admitServicePrograms(Integer clientId, String providerNo, Set<Integer> serviceProgramIds, String admissionText, Date admissionDate) throws ProgramFullException, AdmissionException,
            ServiceRestrictionException {
        SortedSet<Integer> currentServicePrograms = getCurrentServiceProgramIds(clientId);

        if (admissionText == null) admissionText = "intake admit";

        //only allow to discharge the programs for which you are staff of.
        Set<Program> programsInDomain = getActiveProviderPrograms(providerNo);
        List<Integer> programDomainIds = new ArrayList<Integer>();
        for (Program p : programsInDomain) {
            programDomainIds.add(p.getId());
        }

        //discharge from all
        if (serviceProgramIds.isEmpty()) {
            for (Object programId : currentServicePrograms) {
                if (programDomainIds.contains(programId)) {
                    admissionManager.processDischarge((Integer) programId, clientId, "intake discharge", "0", admissionDate);
                }
            }
            return;
        }

        //remove the ones selected, and discharge the ones not selected
        Collection<?> discharge = CollectionUtils.subtract(currentServicePrograms, serviceProgramIds);

        for (Object programId : discharge) {
            if (programDomainIds.contains(programId)) {
                admissionManager.processDischarge((Integer) programId, clientId, "intake discharge", "0", admissionDate);
            }
        }


        Collection<?> admit = CollectionUtils.subtract(serviceProgramIds, currentServicePrograms);

        for (Object programId : admit) {
            Program program = programManager.getProgram((Integer) programId);
            admissionManager.processAdmission(clientId, providerNo, program, "intake discharge", admissionText, admissionDate);
        }
    }

    private void saveIntake(Intake intake, Integer clientId) {
        intake.setClientId(clientId);
        genericIntakeManager.saveIntake(intake);
    }

    private void saveUpdateIntake(Intake intake, Integer clientId) {
        intake.setClientId(clientId);

        genericIntakeManager.saveUpdateIntake(intake);
    }

    public Set<Program> getActiveProviderProgramsInFacility(LoggedInInfo loggedInInfo, String providerNo, Integer facilityId) {
        Set<Program> programs = new HashSet<Program>();
        Set<Program> programsInDomain = getActiveProviderPrograms(providerNo);
        if (facilityId == null) return programs;

        for (Program p : programManager.getProgramDomainInCurrentFacilityForCurrentProvider(loggedInInfo, false)) {
            if (programsInDomain.contains(p)) {
                programs.add(p);
            }
        }

        return programs;
    }

    // Bean

    private void setBeanProperties(LoggedInInfo loggedInInfo, GenericIntakeEditFormBean formBean, Intake intake, Demographic client, String providerNo, boolean bedProgramsVisible,
                                   boolean serviceProgramsVisible, boolean externalProgramsVisible, Integer currentBedProgramId, SortedSet<Integer> currentServiceProgramIds,
                                   Integer currentExternalProgramId, Integer facilityId, Integer nodeId, List<IntakeNodeJavascript> javascriptLocation, boolean communityProgramsVisible, Integer currentCommunityProgramId) {
        formBean.setIntake(intake);
        formBean.setClient(client);
        formBean.setNodeId(nodeId);
        formBean.setJsLocation(javascriptLocation);

        if (bedProgramsVisible || communityProgramsVisible || serviceProgramsVisible || externalProgramsVisible) {
            Set<Program> providerPrograms = getActiveProviderProgramsInFacility(loggedInInfo, providerNo, facilityId);

            if (bedProgramsVisible) {
                formBean.setBedPrograms(getBedPrograms(providerPrograms, providerNo));
                formBean.setSelectedBedProgramId(currentBedProgramId);
            }

            if (communityProgramsVisible) {
                formBean.setCommunityPrograms(getCommunityPrograms());
                formBean.setSelectedCommunityProgramId(currentCommunityProgramId);
            }

            if (serviceProgramsVisible) {
                formBean.setServicePrograms(getServicePrograms(providerPrograms, providerNo));
                formBean.setSelectedServiceProgramIds(currentServiceProgramIds);
            }

            if (externalProgramsVisible) {
                formBean.setExternalPrograms(getExternalPrograms(providerPrograms));
                formBean.setSelectedExternalProgramId(currentExternalProgramId);
            }

            formBean.setProgramsInDomain(getProgramsInDomain(providerPrograms));

            String intakeLocation = "";
            if (intake != null) {
                intakeLocation = String.valueOf(intake.getIntakeLocation());
            }
            if (intakeLocation == null || "".equals(intakeLocation) || "null".equals(intakeLocation)) {
                formBean.setSelectedProgramInDomainId(0);
            } else {
                formBean.setSelectedProgramInDomainId(Integer.valueOf(intakeLocation));
            }
        }
    }

    protected Demographic getClient(HttpServletRequest request) {
        Demographic client = (Demographic) getSessionAttribute(request, CLIENT);
        return (client != null) ? client : new Demographic();
    }

    public void setClientManager(ClientManager mgr) {
        this.clientManager = mgr;
    }

    public void setProgramManager(ProgramManager mgr) {
        this.programManager = mgr;
    }

    public void setAdmissionManager(AdmissionManager mgr) {
        this.admissionManager = mgr;
    }

    public void setCaseManagementManager(CaseManagementManager caseManagementManager) {
        this.caseManagementManager = caseManagementManager;
    }

    protected Integer getClientIdAsInteger(HttpServletRequest request) {
        Integer clientId = null;
        String clientId_str = request.getParameter(CLIENT_ID);
        if (clientId_str != null) {
            try {
                clientId = Integer.valueOf(clientId_str);
            } catch (NumberFormatException e) {
                LOG.error("Error", e);
            }
        }
        return clientId;

    }

    protected Integer getIntakeId(HttpServletRequest request) {
        return Integer.valueOf(request.getParameter(INTAKE_ID));
    }

    protected Object getSessionAttribute(HttpServletRequest request, String attributeName) {
        Object attribute = request.getSession().getAttribute(attributeName);

        if (attribute != null) {
            request.getSession().removeAttribute(attributeName);
        }

        return attribute;
    }
}
