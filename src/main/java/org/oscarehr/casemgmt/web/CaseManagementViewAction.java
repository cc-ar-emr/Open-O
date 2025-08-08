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

package org.oscarehr.casemgmt.web;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.logging.log4j.Logger;
import org.oscarehr.PMmodule.caisi_integrator.CaisiIntegratorManager;
import org.oscarehr.PMmodule.dao.SecUserRoleDao;
import org.oscarehr.PMmodule.model.SecUserRole;
import org.oscarehr.caisi_integrator.ws.CachedDemographicIssue;
import org.oscarehr.caisi_integrator.ws.CachedFacility;
import org.oscarehr.casemgmt.common.Colour;
import org.oscarehr.casemgmt.dao.CaseManagementNoteDAO;
import org.oscarehr.casemgmt.dao.IssueDAO;
import org.oscarehr.casemgmt.model.CaseManagementIssue;
import org.oscarehr.casemgmt.model.CaseManagementNote;
import org.oscarehr.casemgmt.model.CaseManagementNoteExt;
import org.oscarehr.casemgmt.model.Issue;
import org.oscarehr.casemgmt.service.CaseManagementManager;
import org.oscarehr.casemgmt.service.NoteService;
import org.oscarehr.common.dao.BillingONCHeader1Dao;
import org.oscarehr.common.dao.CaseManagementIssueNotesDao;
import org.oscarehr.common.dao.DemographicDao;
import org.oscarehr.common.dao.GroupNoteDao;
import org.oscarehr.common.model.BillingONCHeader1;
import org.oscarehr.eyeform.EyeformInit;
import org.oscarehr.managers.TicklerManager;
import org.oscarehr.provider.web.CppPreferencesUIBean;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SpringUtils;
import oscar.OscarProperties;
import oscar.util.OscarRoleObjectPrivilege;

import java.net.MalformedURLException;
import java.util.*;

/*
 * Updated by Eugene Petruhin on 21 jan 2009 while fixing missing "New Note" link
 */
public class CaseManagementViewAction {

    private static final Integer MAX_INVOICES = 20;
    private static Logger logger = MiscUtils.getLogger();
    private CaseManagementManager caseManagementManager = (CaseManagementManager) SpringUtils.getBean(CaseManagementManager.class);
    private IssueDAO issueDao = (IssueDAO) SpringUtils.getBean(IssueDAO.class);
    private CaseManagementNoteDAO caseManagementNoteDao = (CaseManagementNoteDAO) SpringUtils.getBean(CaseManagementNoteDAO.class);
    private SecUserRoleDao secUserRoleDao = (SecUserRoleDao) SpringUtils.getBean(SecUserRoleDao.class);
    private GroupNoteDao groupNoteDao = (GroupNoteDao) SpringUtils.getBean(GroupNoteDao.class);
    private DemographicDao demographicDao = (DemographicDao) SpringUtils.getBean(DemographicDao.class);
    private CaseManagementIssueNotesDao cmeIssueNotesDao = (CaseManagementIssueNotesDao) SpringUtils.getBean(CaseManagementIssueNotesDao.class);
    private BillingONCHeader1Dao billingONCHeader1Dao = (BillingONCHeader1Dao) SpringUtils.getBean(BillingONCHeader1Dao.class);
    private NoteService noteService = SpringUtils.getBean(NoteService.class);
    private TicklerManager ticklerManager = SpringUtils.getBean(TicklerManager.class);

    static {
        //temporary..need something generic;
        EyeformInit.init();
    }

    public static class IssueDisplay {
        public boolean writeAccess = true;
        public String codeType = null;
        public String code = null;
        public String description = null;
        public String location = null;
        public String acute = null;
        public String certain = null;
        public String major = null;
        public String resolved = null;
        public String role = null;
        public String priority = null;
        public Integer sortOrderId = null;

        public Integer getSortOrderId() {
            return sortOrderId;
        }

        public void setSortOrderId(Integer sortOrderId) {
            this.sortOrderId = sortOrderId;
        }

        public String getCodeType() {
            return codeType;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public String getLocation() {
            return location;
        }

        public String getAcute() {
            return acute;
        }

        public String getCertain() {
            return certain;
        }

        public String getMajor() {
            return major;
        }

        public String getResolved() {
            return resolved;
        }

        public String getRole() {
            return role;
        }

        public String getPriority() {
            return priority;
        }

        public boolean isWriteAccess() {
            return writeAccess;
        }

        public void setWriteAccess(boolean writeAccess) {
            this.writeAccess = writeAccess;
        }

        public void setCodeType(String codeType) {
            this.codeType = codeType;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public void setAcute(String acute) {
            this.acute = acute;
        }

        public void setCertain(String certain) {
            this.certain = certain;
        }

        public void setMajor(String major) {
            this.major = major;
        }

        public void setResolved(String resolved) {
            this.resolved = resolved;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }

        public String toString() {
            return (ReflectionToStringBuilder.toString(this));
        }
    }

    private void sortIssues(ArrayList<CheckBoxBean> checkBoxBeanList) {
        Comparator<CheckBoxBean> cbbComparator = new Comparator<CheckBoxBean>() {
            public int compare(CheckBoxBean o1, CheckBoxBean o2) {
                if (o1.getIssueDisplay() != null && o2.getIssueDisplay() != null && o1.getIssueDisplay().code != null) {
                    return (o1.getIssueDisplay().code.compareTo(o2.getIssueDisplay().code));
                } else return (0);
            }
        };

        Collections.sort(checkBoxBeanList, cbbComparator);
    }

    public void sortIssuesByOrderId(ArrayList<CheckBoxBean> checkBoxBeanList) {
        Comparator<CheckBoxBean> cbbComparator = new Comparator<CheckBoxBean>() {
            public int compare(CheckBoxBean o1, CheckBoxBean o2) {
                if (o1.getIssueDisplay() != null && o2.getIssueDisplay() != null && o1.getIssueDisplay().sortOrderId != null && o2.getIssueDisplay().sortOrderId != null) {
                    return (o1.getIssueDisplay().sortOrderId.compareTo(o2.getIssueDisplay().sortOrderId));
                } else return (0);
            }
        };

        Collections.sort(checkBoxBeanList, cbbComparator);
    }

    private void fetchInvoices(ArrayList<NoteDisplay> notes, String demographicNo) {
        List<BillingONCHeader1> bills = billingONCHeader1Dao.getInvoices(Integer.parseInt(demographicNo), MAX_INVOICES);

        for (BillingONCHeader1 h1 : bills) {
            notes.add(new NoteDisplayNonNote(h1));
        }
    }

    private List<CaseManagementNote> applyRoleFilter(List<CaseManagementNote> notes, String[] roleId) {

        // if no filter return everything
        if (Arrays.binarySearch(roleId, "a") >= 0) return notes;

        List<CaseManagementNote> filteredNotes = new ArrayList<CaseManagementNote>();

        for (Iterator<CaseManagementNote> iter = notes.listIterator(); iter.hasNext(); ) {
            CaseManagementNote note = iter.next();

            if (Arrays.binarySearch(roleId, note.getReporter_caisi_role()) >= 0) filteredNotes.add(note);
        }

        return filteredNotes;
    }

    private List<CaseManagementNote> applyIssueFilter(List<CaseManagementNote> notes, String[] issueId) {

        // if no filter return everything
        if (Arrays.binarySearch(issueId, "a") >= 0) return notes;

        boolean none = (Arrays.binarySearch(issueId, "n") >= 0) ? true : false;

        List<CaseManagementNote> filteredNotes = new ArrayList<CaseManagementNote>();

        for (Iterator<CaseManagementNote> iter = notes.listIterator(); iter.hasNext(); ) {
            CaseManagementNote note = iter.next();
            List<CaseManagementIssue> issues = cmeIssueNotesDao.getNoteIssues((Integer.valueOf(note.getId().toString())));
            if (issues.size() == 0 && none) {
                filteredNotes.add(note);
            } else {
                for (CaseManagementIssue issue : issues) {
                    if (Arrays.binarySearch(issueId, String.valueOf(issue.getId())) >= 0) {
                        filteredNotes.add(note);
                        break;
                    }
                }
            }
        }

        return filteredNotes;
    }

    private List<CaseManagementNote> manageLockedNotes(List<CaseManagementNote> notes, boolean removeLockedNotes, Map<Long, Boolean> unlockedNotesMap) {
        List<CaseManagementNote> notesNoLocked = new ArrayList<CaseManagementNote>();
        for (CaseManagementNote note : notes) {
            if (note.isLocked()) {
                if (unlockedNotesMap.get(note.getId()) != null) {
                    note.setLocked(false);
                }
            }
            if (removeLockedNotes && !note.isLocked()) {
                notesNoLocked.add(note);
            }
        }
        if (removeLockedNotes) {
            return notesNoLocked;
        }
        return notes;
    }

    private List<CaseManagementNote> applyProviderFilters(List<CaseManagementNote> notes, String[] providerNo) {
        boolean filter = false;
        List<CaseManagementNote> filteredNotes = new ArrayList<CaseManagementNote>();

        if (providerNo != null && Arrays.binarySearch(providerNo, "a") < 0) {
            filter = true;
        }

        for (Iterator<CaseManagementNote> iter = notes.iterator(); iter.hasNext(); ) {
            CaseManagementNote note = iter.next();
            if (!filter) {
                // no filter, add all
                filteredNotes.add(note);

            } else {
                if (Arrays.binarySearch(providerNo, note.getProviderNo()) >= 0)
                    // correct provider
                    filteredNotes.add(note);
            }
        }

        return filteredNotes;
    }

    private static boolean hasRole(List<SecUserRole> roles, String role) {
        if (roles == null) return (false);

        logger.debug("Note Role : " + role);

        for (SecUserRole roleTmp : roles) {
            logger.debug("Provider Roles : " + roleTmp.getRoleName());
            if (roleTmp.getRoleName().equals(role)) return (true);
        }

        return (false);
    }

    public static boolean hasSameAttributes(IssueDisplay issueDisplay1, IssueDisplay issueDisplay2) {
        if (issueDisplay1.code != null && !issueDisplay1.code.equals(issueDisplay2.code)) return (false);
        if (issueDisplay1.acute != null && !issueDisplay1.acute.equals(issueDisplay2.acute)) return (false);
        if (issueDisplay1.certain != null && !issueDisplay1.certain.equals(issueDisplay2.certain)) return (false);
        if (issueDisplay1.major != null && !issueDisplay1.major.equals(issueDisplay2.major)) return (false);
        if (issueDisplay1.priority != null && !issueDisplay1.priority.equals(issueDisplay2.priority)) return (false);
        if (issueDisplay1.resolved != null && !issueDisplay1.resolved.equals(issueDisplay2.resolved)) return (false);

        return (true);
    }

    private IssueDisplay getIssueToDisplay(LoggedInInfo loggedInInfo, CachedDemographicIssue cachedDemographicIssue) throws MalformedURLException {
        IssueDisplay issueDisplay = new IssueDisplay();

        issueDisplay.writeAccess = true;
        issueDisplay.acute = cachedDemographicIssue.isAcute() ? "acute" : "chronic";
        issueDisplay.certain = cachedDemographicIssue.isCertain() ? "certain" : "uncertain";
        issueDisplay.code = cachedDemographicIssue.getFacilityDemographicIssuePk().getIssueCode();
        issueDisplay.codeType = "ICD10"; // temp hard coded hack till issue is resolved

        Issue issue = null;
        // temp hard coded icd hack till issue is resolved
        if ("ICD10".equalsIgnoreCase(OscarProperties.getInstance().getProperty("COMMUNITY_ISSUE_CODETYPE").toUpperCase())) {
            issue = issueDao.findIssueByCode(cachedDemographicIssue.getFacilityDemographicIssuePk().getIssueCode());
        }

        if (issue != null) {
            issueDisplay.description = issue.getDescription();
            issueDisplay.priority = issue.getPriority();
            issueDisplay.role = issue.getRole();
        } else {
            issueDisplay.description = "Not Available";
            issueDisplay.priority = "Not Available";
            issueDisplay.role = "Not Available";
        }

        Integer remoteFacilityId = cachedDemographicIssue.getFacilityDemographicIssuePk().getIntegratorFacilityId();
        CachedFacility remoteFacility = CaisiIntegratorManager.getRemoteFacility(loggedInInfo, loggedInInfo.getCurrentFacility(), remoteFacilityId);
        if (remoteFacility != null) issueDisplay.location = "remote: " + remoteFacility.getName();
        else issueDisplay.location = "remote: name unavailable";

        issueDisplay.major = cachedDemographicIssue.isMajor() ? "major" : "not major";
        issueDisplay.resolved = cachedDemographicIssue.isResolved() ? "resolved" : "unresolved";

        return (issueDisplay);
    }

    protected void addLocalIssues(String providerNo, ArrayList<CheckBoxBean> checkBoxBeanList, Integer demographicNo, boolean hideInactiveIssues, Integer programId) {
        List<CaseManagementIssue> localIssues = caseManagementManager.getIssues(demographicNo, hideInactiveIssues ? false : null);

        for (CaseManagementIssue cmi : localIssues) {
            CheckBoxBean checkBoxBean = new CheckBoxBean();

            checkBoxBean.setIssue(cmi);

            IssueDisplay issueDisplay = getIssueDisplay(providerNo, programId, cmi);
            checkBoxBean.setIssueDisplay(issueDisplay);

            checkBoxBean.setUsed(caseManagementNoteDao.haveIssue(cmi.getIssue().getCode(), demographicNo));

            checkBoxBeanList.add(checkBoxBean);
        }
    }

    protected IssueDisplay getIssueDisplay(String providerNo, Integer programId, CaseManagementIssue cmi) {
        IssueDisplay issueDisplay = new IssueDisplay();

        if (programId != null) issueDisplay.writeAccess = cmi.isWriteAccess(providerNo, programId);

        issueDisplay.acute = cmi.isAcute() ? "acute" : "chronic";
        issueDisplay.certain = cmi.isCertain() ? "certain" : "uncertain";

        long issueId = cmi.getIssue_id();
        Issue issue = issueDao.getIssue(issueId);

        issueDisplay.code = issue.getCode();
        issueDisplay.codeType = OscarProperties.getInstance().getProperty("COMMUNITY_ISSUE_CODETYPE").toUpperCase();
        issueDisplay.description = issue.getDescription();
        issueDisplay.location = "local";
        issueDisplay.major = cmi.isMajor() ? "major" : "not major";
        issueDisplay.priority = issue.getPriority();
        issueDisplay.resolved = cmi.isResolved() ? "resolved" : "unresolved";
        issueDisplay.role = issue.getRole();
        issueDisplay.sortOrderId = issue.getSortOrderId();

        return issueDisplay;
    }

    private static boolean hasRole(String[] roleId, String role) {
        for (String s : roleId) {
            if (s.equals(role)) return (true);
        }

        return (false);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public boolean hasPrivilege(String objectName, String roleName) {
        Vector v = OscarRoleObjectPrivilege.getPrivilegeProp(objectName);
        return OscarRoleObjectPrivilege.checkPrivilege(roleName, (Properties) v.get(0), (Vector) v.get(1));
    }

    public static String getNoteColour(NoteDisplay noteDisplay) {
        // set all colors
        String blackColour = "FFFFFF";
        String documentColour = "color:#" + blackColour + ";background-color:#" + Colour.getInstance().documents + ";";
        //String diseaseColour = "color:#" + blackColour + ";background-color:#" + Colour.getInstance().disease + ";";
        String eFormsColour = "color:#" + blackColour + ";background-color:#" + Colour.getInstance().eForms + ";";
        String formsColour = "color:#" + blackColour + ";background-color:#" + Colour.getInstance().forms + ";";
        //String labsColour = "color:#" + blackColour + ";background-color:#" + Colour.getInstance().labs + ";";
        //String measurementsColour = "color:#" + blackColour + ";background-color:#" + Colour.getInstance().measurements + ";";
        //String messagesColour = "color:#" + blackColour + ";background-color:#" + Colour.getInstance().messages + ";";
        //String preventionColour = "color:#" + blackColour + ";background-color:#" + Colour.getInstance().prevention + ";";
        //String ticklerColour = "color:#" + blackColour + ";background-color:#" + Colour.getInstance().tickler + ";";
        String rxColour = "color:#" + blackColour + ";background-color:#" + Colour.getInstance().rx + ";";
        String invoiceColour = "color:#" + blackColour + ";background-color:#" + Colour.getInstance().invoices + ";";
        String ticklerNoteColour = "color:#" + blackColour + ";background-color:#" + Colour.getInstance().ticklerNotes + ";";
        String externalNoteColour = "color:#" + blackColour + ";background-color:#" + Colour.getInstance().externalNotes + ";";
        String emailNoteColour = "color:#" + blackColour + ";background-color:#" + Colour.getInstance().emailNotes + ";";

        String bgColour = "color:#000000;background-color:#CCCCFF;";

        if (noteDisplay.isCpp()) {
            bgColour = "color:#FFFFFF;background-color:#" + getCppColour(noteDisplay) + ";";
            if (noteDisplay.isTicklerNote()) {
                bgColour = ticklerNoteColour;
            } else if (noteDisplay.isExternalNote()) {
                bgColour = externalNoteColour;
            }
        } else if (noteDisplay.isDocument()) {
            bgColour = documentColour;
        } else if (noteDisplay.isRxAnnotation()) {
            bgColour = rxColour;
        } else if (noteDisplay.isEformData()) {
            bgColour = eFormsColour;
        } else if (noteDisplay.isEncounterForm()) {
            bgColour = formsColour;
        } else if (noteDisplay.isInvoice()) {
            bgColour = invoiceColour;
        } else if (noteDisplay.isEmailNote()) {
            bgColour = emailNoteColour;
        }

        return (bgColour);
    }

    private static String getCppColour(NoteDisplay noteDisplay) {
        Colour colour = Colour.getInstance();

        if (noteDisplay.containsIssue("OMeds")) return (colour.omed);
        else if (noteDisplay.containsIssue("FamHistory")) return (colour.familyHistory);
        else if (noteDisplay.containsIssue("RiskFactors")) return (colour.riskFactors);
        else if (noteDisplay.containsIssue("SocHistory")) return (colour.socialHistory);
        else if (noteDisplay.containsIssue("MedHistory")) return (colour.medicalHistory);
        else if (noteDisplay.containsIssue("Concerns")) return (colour.ongoingConcerns);
        else if (noteDisplay.containsIssue("Reminders")) return (colour.reminders);
        else return colour.prevention;

    }

    public static CaseManagementNote getLatestCppNote(String demographicNo, long issueId, int appointmentNo, boolean filterByAppointment) {
        CaseManagementManager caseManagementMgr = (CaseManagementManager) SpringUtils.getBean(CaseManagementManager.class);
        Collection<CaseManagementNote> notes = caseManagementMgr.getActiveNotes(demographicNo, new String[]{String.valueOf(issueId)});
        List<CaseManagementNote> filteredNotes = new ArrayList<CaseManagementNote>();

        if (notes.size() == 0) {
            return null;
        }
        if (filterByAppointment) {
            for (CaseManagementNote note : notes) {
                if (note.getAppointmentNo() == appointmentNo) {
                    filteredNotes.add(note);
                }
            }
            if (filteredNotes.size() == 0) {
                return null;
            }
        } else {
            filteredNotes.addAll(notes);
        }
        return filteredNotes.iterator().next();
    }

    public static String getCppAdditionalData(Long noteId, String issueCode, List<CaseManagementNoteExt> noteExts, CppPreferencesUIBean prefsBean) {
        if (prefsBean.getEnable() == null || !prefsBean.getEnable().equals("on")) {
            return new String();
        }
        String issueCodeArr[] = issueCode.split(";");
        StringBuilder sb = new StringBuilder();
        if (issueCodeArr[1].equals("SocHistory")) {
            if (prefsBean.getSocialHxStartDate().equals("on")) {
                sb.append("Start Date:" + getNoteExt(noteId, "Start Date", noteExts));
            }
            if (prefsBean.getSocialHxResDate().equals("on")) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append("Resolution Date:" + getNoteExt(noteId, "Resolution Date", noteExts));
            }
        }
        if (issueCodeArr[1].equals("Reminders")) {
            if (prefsBean.getRemindersStartDate().equals("on")) {
                sb.append("Start Date:" + getNoteExt(noteId, "Start Date", noteExts));
            }
            if (prefsBean.getRemindersResDate().equals("on")) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append("Resolution Date:" + getNoteExt(noteId, "Resolution Date", noteExts));
            }
        }
        if (issueCodeArr[1].equals("Concerns")) {
            if (prefsBean.getOngoingConcernsStartDate().equals("on")) {
                sb.append("Start Date:" + getNoteExt(noteId, "Start Date", noteExts));
            }
            if (prefsBean.getOngoingConcernsResDate().equals("on")) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append("Resolution Date:" + getNoteExt(noteId, "Resolution Date", noteExts));
            }
            if (prefsBean.getOngoingConcernsProblemStatus().equals("on")) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append("Status:" + getNoteExt(noteId, "Problem Status", noteExts));
            }
        }
        if (issueCodeArr[1].equals("MedHistory")) {
            if (prefsBean.getMedHxStartDate().equals("on")) {
                sb.append("Start Date:" + getNoteExt(noteId, "Start Date", noteExts));
            }
            if (prefsBean.getMedHxResDate().equals("on")) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append("Resolution Date:" + getNoteExt(noteId, "Resolution Date", noteExts));
            }
            if (prefsBean.getMedHxProcedureDate().equals("on")) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append("Procedure Date:" + getNoteExt(noteId, "Procedure Date", noteExts));
            }
            if (prefsBean.getMedHxTreatment().equals("on")) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append("Treatment:" + getNoteExt(noteId, "Treatment", noteExts));
            }
        }

        if (issueCodeArr[1].equals("RiskFactors")) {
            if (prefsBean.getRiskFactorsStartDate().equals("on")) {
                sb.append("Start Date:" + getNoteExt(noteId, "Start Date", noteExts));
            }
            if (prefsBean.getRiskFactorsResDate().equals("on")) {
                sb.append("Resolution Date:" + getNoteExt(noteId, "Resolution Date", noteExts));
            }
        }

        if (issueCodeArr[1].equals("OMeds")) {
            if (prefsBean.getOtherMedsStartDate().equals("on")) {
                sb.append("Start Date:" + getNoteExt(noteId, "Start Date", noteExts));
            }
            if (prefsBean.getOtherMedsResDate().equals("on")) {
                sb.append("Resolution Date:" + getNoteExt(noteId, "Resolution Date", noteExts));
            }
        }

        if (issueCodeArr[1].equals("FamHistory")) {
            if (prefsBean.getFamilyHistoryStartDate().equals("on")) {
                sb.append("Start Date:" + getNoteExt(noteId, "Start Date", noteExts));
            }
            if (prefsBean.getFamilyHistoryResDate().equals("on")) {
                sb.append("Resolution Date:" + getNoteExt(noteId, "Resolution Date", noteExts));
            }
            if (prefsBean.getFamilyHistoryTreatment().equals("on")) {
                sb.append("Treatment:" + getNoteExt(noteId, "Treatment", noteExts));
            }
            if (prefsBean.getFamilyHistoryRelationship().equals("on")) {
                sb.append("Relationship:" + getNoteExt(noteId, "Relationship", noteExts));
            }
        }


        if (sb.length() > 0) {
            sb.insert(0, " (");
            sb.append(")");
        }
        return sb.toString();
    }

    static String getNoteExt(Long noteId, String key, List<CaseManagementNoteExt> lcme) {
        for (CaseManagementNoteExt cme : lcme) {
            if (cme.getNoteId().equals(noteId) && cme.getKeyVal().equals(key)) {
                String val = null;

                if (key.contains(" Date")) {
                    val = oscar.util.UtilDateUtilities.DateToString(cme.getDateValue(), "yyyy-MM-dd");
                } else {
                    val = org.apache.commons.text.StringEscapeUtils.escapeEcmaScript(cme.getValue());
                }
                return val;
            }
        }
        return "";
    }
}
