/**
 * Copyright (c) 2001-2002. Department of Family Medicine, McMaster University. All Rights Reserved.
 * This software is published under the GPL GNU General Public License.
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * This software was written for the
 * Department of Family Medicine
 * McMaster University
 * Hamilton
 * Ontario, Canada
 */


package ca.openosp.openo.encounter.oscarConsultationRequest.pageUtil;

import ca.openosp.openo.commn.dao.ConsultationRequestDao;
import ca.openosp.openo.commn.model.Provider;
import ca.openosp.openo.consultation.dto.ConsultationListDTO;
import ca.openosp.openo.utility.LoggedInInfo;
import ca.openosp.openo.utility.MiscUtils;
import ca.openosp.openo.utility.SpringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Utility class that populates parallel lists of consultation request data for JSP display.
 * <p>
 * Uses {@link ConsultationRequestDao#getConsultationDTOs} and
 * {@link ConsultationRequestDao#getConsultationDTOsByDemographic} to fetch all display fields
 * via a single JPQL constructor projection with batch-loaded extensions, replacing the previous
 * N+1 pattern that individually queried Demographics, Providers, Services, and Extensions per row.
 * </p>
 *
 * @since 2001-08-17
 */
public class EctViewConsultationRequestsUtil {
   public List<String> ids;
   public List<String> status;
   public List<String> patient;
   public List<String> teams;
   public List<String> provider;
   public List<String> service;
   public List<String> vSpecialist;
   public List<String> date;
   public List<String> demographicNo;
   public List<String> apptDate;
   public List<String> patientWillBook;
   public List<String> urgency;
   public List<String> followUpDate;
   public List<String> providerNo;
   public List<String> siteName;
   public List<Provider> consultProvider;
   public List<Boolean> eReferral;

   /**
    * Populates consultation list data filtered by team with default parameters.
    *
    * @param loggedInInfo LoggedInInfo the current user session
    * @param team String the team/sendTo filter value
    * @return boolean true if data was loaded successfully, false on error
    */
   public boolean estConsultationVecByTeam(LoggedInInfo loggedInInfo,String team) {
      return estConsultationVecByTeam(loggedInInfo,team,false,null,null);
   }
   /** @see #estConsultationVecByTeam(LoggedInInfo, String, boolean, Date, Date, String, String, String, Integer, Integer) */
   public boolean estConsultationVecByTeam(LoggedInInfo loggedInInfo,String team,boolean showCompleted) {
      return estConsultationVecByTeam(loggedInInfo,team,showCompleted,null,null);
   }
   /** @see #estConsultationVecByTeam(LoggedInInfo, String, boolean, Date, Date, String, String, String, Integer, Integer) */
   public boolean estConsultationVecByTeam(LoggedInInfo loggedInInfo, String team,boolean showCompleted,Date startDate, Date endDate) {
      return estConsultationVecByTeam(loggedInInfo,team,showCompleted,startDate,endDate,null);
   }
   /** @see #estConsultationVecByTeam(LoggedInInfo, String, boolean, Date, Date, String, String, String, Integer, Integer) */
   public boolean estConsultationVecByTeam(LoggedInInfo loggedInInfo, String team,boolean showCompleted,Date startDate, Date endDate,String orderby) {
      return estConsultationVecByTeam(loggedInInfo,team,showCompleted,startDate,endDate,orderby,null);
   }
   /** @see #estConsultationVecByTeam(LoggedInInfo, String, boolean, Date, Date, String, String, String, Integer, Integer) */
   public boolean estConsultationVecByTeam(LoggedInInfo loggedInInfo, String team,boolean showCompleted,Date startDate, Date endDate,String orderby,String desc) {
      return estConsultationVecByTeam(loggedInInfo,team,showCompleted,startDate,endDate,orderby,desc,null,null,null);
   }

   /**
    * Populates parallel lists of consultation request data filtered by team and optional criteria.
    * <p>
    * Delegates to {@link ConsultationRequestDao#getConsultationDTOs} which executes a single JPQL
    * constructor projection query with LEFT JOINs, followed by one batch extension query.
    * </p>
    *
    * @param loggedInInfo LoggedInInfo the current user session
    * @param team String the team/sendTo filter value (empty string for all teams)
    * @param showCompleted boolean whether to include completed (status 4) consultations
    * @param startDate Date the start date filter (null for no lower bound)
    * @param endDate Date the end date filter (null for no upper bound)
    * @param orderby String the sort column identifier (1-9), null for default referral date desc
    * @param desc String "1" for descending sort, null/other for ascending
    * @param searchDate String "1" to filter on appointment date instead of referral date
    * @param offset Integer the pagination offset (null defaults to 0)
    * @param limit Integer the page size (null defaults to {@link ConsultationRequestDao#DEFAULT_CONSULT_REQUEST_RESULTS_LIMIT})
    * @return boolean true if data was loaded successfully, false on error
    */
   public boolean estConsultationVecByTeam(LoggedInInfo loggedInInfo, String team,boolean showCompleted,Date startDate, Date endDate,String orderby,String desc,String searchDate, Integer offset, Integer limit) {
      return estConsultationVecByTeam(loggedInInfo, team, showCompleted, startDate, endDate, orderby, desc, searchDate, offset, limit, null, null);
   }

   /**
    * Same as {@link #estConsultationVecByTeam(LoggedInInfo, String, boolean, Date, Date, String, String, String, Integer, Integer)}
    * but additionally filters by an optional consultant (specialist) and/or patient provider (MRP).
    * Both filters are optional and combine with each other and the other filters.
    *
    * @param consultantId Integer the ProfessionalSpecialist id to filter by (null for no consultant filter)
    * @param filterProviderNo String the patient MRP provider number to filter by (null/empty for no provider filter)
    * @return boolean true if data was loaded successfully, false on error
    */
   public boolean estConsultationVecByTeam(LoggedInInfo loggedInInfo, String team,boolean showCompleted,Date startDate, Date endDate,String orderby,String desc,String searchDate, Integer offset, Integer limit, Integer consultantId, String filterProviderNo) {
      initLists();

      boolean verdict = true;
      try {
          ConsultationRequestDao consultReqDao = SpringUtils.getBean(ConsultationRequestDao.class);
          List<ConsultationListDTO> dtos = consultReqDao.getConsultationDTOs(team, showCompleted, startDate, endDate, orderby, desc, searchDate, offset, limit, consultantId, filterProviderNo);

          for (ConsultationListDTO dto : dtos) {
              ids.add(dto.getId() != null ? dto.getId().toString() : "");
              status.add(dto.getStatus());
              patient.add(dto.getPatientFormattedName());
              provider.add(dto.getMrpFormattedName());
              providerNo.add(isBlank(dto.getDemographicProviderNo()) ? "-1" : dto.getDemographicProviderNo());
              service.add(dto.getEffectiveServiceDescription());
              vSpecialist.add(dto.getSpecialistFormattedName());
              urgency.add(dto.getUrgency());
              date.add(dto.getReferralDateFormatted());
              demographicNo.add(dto.getDemographicNo() != null ? dto.getDemographicNo().toString() : "0");
              siteName.add(dto.getSiteName());
              teams.add(dto.getSendTo());
              eReferral.add(dto.isEReferral());
              apptDate.add(dto.getAppointmentDateFormatted());
              patientWillBook.add(String.valueOf(dto.isPatientWillBook()));
              followUpDate.add(dto.getFollowUpDateFormatted());

              consultProvider.add(buildConsultProvider(dto));
          }

      } catch(Exception e) {
         MiscUtils.getLogger().error("Error loading consultation list for team: " + (team != null ? team : "all"), e);
         verdict = false;
      }
      return verdict;
   }


   /**
    * Populates parallel lists of consultation request data for a specific patient.
    * <p>
    * Delegates to {@link ConsultationRequestDao#getConsultationDTOsByDemographic} which executes
    * a single JPQL constructor projection query with batch-loaded extensions.
    * </p>
    *
    * @param loggedInInfo LoggedInInfo the current user session
    * @param demoNo String the demographic number of the patient
    * @return boolean true if data was loaded successfully, false on error
    */
   public boolean estConsultationVecByDemographic(LoggedInInfo loggedInInfo, String demoNo) {
      initLists();

      boolean verdict = true;
      try {
          int demographicId = Integer.parseInt(demoNo);
          ConsultationRequestDao consultReqDao = SpringUtils.getBean(ConsultationRequestDao.class);
          List<ConsultationListDTO> dtos = consultReqDao.getConsultationDTOsByDemographic(demographicId);

          for (ConsultationListDTO dto : dtos) {
              ids.add(dto.getId() != null ? dto.getId().toString() : "");
              status.add(dto.getStatus());
              patient.add(dto.getPatientFormattedName());
              provider.add(dto.getMrpFormattedName());
              providerNo.add(isBlank(dto.getDemographicProviderNo()) ? "-1" : dto.getDemographicProviderNo());
              service.add(dto.getEffectiveServiceDescription());
              vSpecialist.add(dto.getSpecialistFormattedName());
              urgency.add(dto.getUrgency());
              patientWillBook.add(String.valueOf(dto.isPatientWillBook()));
              date.add(dto.getReferralDateFormatted());
              demographicNo.add(dto.getDemographicNo() != null ? dto.getDemographicNo().toString() : "0");
              siteName.add(dto.getSiteName());
              teams.add(dto.getSendTo());
              eReferral.add(dto.isEReferral());
              apptDate.add(dto.getAppointmentDateFormatted());
              followUpDate.add(dto.getFollowUpDateFormatted());

              consultProvider.add(buildConsultProvider(dto));
          }

      } catch (NumberFormatException e) {
         MiscUtils.getLogger().error("Invalid demographic number: non-numeric value provided", e);
         verdict = false;
      } catch(Exception e) {
         MiscUtils.getLogger().error("Error loading consultations for demographic: " + demoNo, e);
         verdict = false;
      }
      return verdict;
   }

   /**
    * Builds a Provider object from DTO consulting provider fields, returning null when no provider data exists
    * so JSPs can render blank instead of "null, null".
    */
   private Provider buildConsultProvider(ConsultationListDTO dto) {
      if (dto.getConsultProviderLastName() == null && dto.getConsultProviderFirstName() == null) {
         return null;
      }
      Provider cProv = new Provider();
      cProv.setLastName(dto.getConsultProviderLastName() != null ? dto.getConsultProviderLastName() : "");
      cProv.setFirstName(dto.getConsultProviderFirstName() != null ? dto.getConsultProviderFirstName() : "");
      return cProv;
   }

   private boolean isBlank(String value) {
      return value == null || value.trim().isEmpty();
   }

   /**
    * Initializes all parallel lists to empty ArrayLists.
    */
   private void initLists() {
      ids = new ArrayList<>();
      status = new ArrayList<>();
      patient = new ArrayList<>();
      provider = new ArrayList<>();
      providerNo = new ArrayList<>();
      teams = new ArrayList<>();
      service = new ArrayList<>();
      vSpecialist = new ArrayList<>();
      urgency = new ArrayList<>();
      date = new ArrayList<>();
      demographicNo = new ArrayList<>();
      siteName = new ArrayList<>();
      patientWillBook = new ArrayList<>();
      apptDate = new ArrayList<>();
      followUpDate = new ArrayList<>();
      consultProvider = new ArrayList<>();
      eReferral = new ArrayList<>();
   }
}
