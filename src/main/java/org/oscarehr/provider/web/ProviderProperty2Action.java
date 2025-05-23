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


package org.oscarehr.provider.web;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.lang3.StringUtils;
import org.oscarehr.PMmodule.dao.ProviderDao;
import org.oscarehr.common.dao.CtlBillingServiceDao;
import org.oscarehr.common.dao.QueueDao;
import org.oscarehr.common.dao.UserPropertyDAO;
import org.oscarehr.common.model.Facility;
import org.oscarehr.common.model.Provider;
import org.oscarehr.common.model.UserProperty;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.MiscUtils;
import org.oscarehr.util.SpringUtils;
import oscar.eform.EFormUtil;
import oscar.log.LogAction;
import oscar.oscarEncounter.oscarConsultationRequest.pageUtil.EctConsultationFormRequestUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * @author rjonasz
 */
import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.ServletActionContext;
import oscar.util.LabelValueBean;

public class ProviderProperty2Action extends ActionSupport {
    HttpServletRequest request = ServletActionContext.getRequest();
    HttpServletResponse response = ServletActionContext.getResponse();

    private UserPropertyDAO userPropertyDAO = SpringUtils.getBean(UserPropertyDAO.class);

    public String execute() {
        String method = request.getParameter("method");
        if ("OscarMsgRecvd".equals(method)) {
            return OscarMsgRecvd();
        } else if ("remove".equals(method)) {
            return remove();
        } else if ("save".equals(method)) {
            return save();
        } else if ("viewDefaultSex".equals(method)) {
            return viewDefaultSex();
        } else if ("saveDefaultSex".equals(method)) {
            return saveDefaultSex();
        } else if ("viewHCType".equals(method)) {
            return viewHCType();
        }
        return view();
    }

    public String OscarMsgRecvd() {


        userPropertyDAO.saveProp(request.getParameter("provider_no"), UserProperty.OSCAR_MSG_RECVD, request.getParameter("value"));

        return null;
    }

    public String remove() {
        
        UserProperty prop = this.getDateProperty();
        UserProperty prop2 = this.getSingleViewProperty();

        this.userPropertyDAO.delete(prop);
        this.userPropertyDAO.delete(prop2);

        request.setAttribute("status", "success");

        return SUCCESS;
    }

    public String view() {
        
        String provider = LoggedInInfo.getLoggedInInfoFromSession(request).getLoggedInProviderNo();
        UserProperty prop = this.userPropertyDAO.getProp(provider, UserProperty.STALE_NOTEDATE);

        if (prop == null) {
            prop = new UserProperty();
            prop.setProviderNo(provider);
            prop.setName(UserProperty.STALE_NOTEDATE);
        }
        
        this.setDateProperty(prop);

        UserProperty prop2 = this.userPropertyDAO.getProp(provider, UserProperty.STALE_FORMAT);

        if (prop2 == null) {
            prop2 = new UserProperty();
            prop2.setProviderNo(provider);
            prop2.setName(UserProperty.STALE_FORMAT);
        }

        this.setSingleViewProperty(prop2);

        return SUCCESS;
    }


    public String save() {

        UserProperty prop = this.getDateProperty();

        this.userPropertyDAO.saveProp(prop);

        UserProperty prop2 = this.getSingleViewProperty();

        this.userPropertyDAO.saveProp(prop2);

        request.setAttribute("status", "success");
        return SUCCESS;
    }

    /**
     * typically set from inside the JSP class providerupdatepreference.jsp
     *
     * @param request
     */
    public static void updateOrCreateProviderProperties(HttpServletRequest request) {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        UserPropertyDAO propertyDAO = SpringUtils.getBean(UserPropertyDAO.class);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        List<UserProperty> userProperties = new ArrayList<>();
        String propertyValue;
        UserProperty property;

        propertyValue = StringUtils.trimToNull(request.getParameter(UserProperty.SCHEDULE_WEEK_VIEW_WEEKENDS));
        property = propertyDAO.getProp(providerNo, UserProperty.SCHEDULE_WEEK_VIEW_WEEKENDS);
        if (property == null) {
            property = new UserProperty();
            property.setProviderNo(providerNo);
            property.setName(UserProperty.SCHEDULE_WEEK_VIEW_WEEKENDS);
        }
        property.setValue(String.valueOf(Boolean.parseBoolean(propertyValue)));
        propertyDAO.saveProp(property);

    }

    public String viewDefaultSex() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();


        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.DEFAULT_SEX);

        if (prop == null) {
            prop = new UserProperty();
        }

        ArrayList<LabelValueBean> serviceList = new ArrayList<LabelValueBean>();
        serviceList.add(new LabelValueBean("M", "M"));
        serviceList.add(new LabelValueBean("F", "F"));

        request.setAttribute("dropOpts", serviceList);

        request.setAttribute("dateProperty", prop);

        request.setAttribute("providertitle", "provider.setDefaultSex.title");
        request.setAttribute("providermsgPrefs", "provider.setDefaultSex.msgPrefs");
        request.setAttribute("providermsgProvider", "provider.setDefaultSex.msgDefaultSex");
        request.setAttribute("providermsgEdit", "provider.setDefaultSex.msgEdit");
        request.setAttribute("providerbtnSubmit", "provider.setDefaultSex.btnSubmit");
        request.setAttribute("providermsgSuccess", "provider.setDefaultSex.msgSuccess");
        request.setAttribute("method", "saveDefaultSex");

        this.setDateProperty(prop);
        return "gen";
    }

    public String saveDefaultSex() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();


        UserProperty prop = this.getDateProperty();
        String fmt = prop != null ? prop.getValue() : "";
        UserProperty saveProperty = this.userPropertyDAO.getProp(providerNo, UserProperty.DEFAULT_SEX);

        if (saveProperty == null) {
            saveProperty = new UserProperty();
            saveProperty.setProviderNo(providerNo);
            saveProperty.setName(UserProperty.DEFAULT_SEX);
        }

        saveProperty.setValue(fmt);
        this.userPropertyDAO.saveProp(saveProperty);

        request.setAttribute("status", "success");
        request.setAttribute("providertitle", "provider.setDefaultSex.title");
        request.setAttribute("providermsgPrefs", "provider.setDefaultSex.msgPrefs");
        request.setAttribute("providermsgProvider", "provider.setDefaultSex.msgDefaultSex");
        request.setAttribute("providermsgEdit", "provider.setDefaultSex.msgEdit");
        request.setAttribute("providerbtnSubmit", "provider.btnSubmit");
        request.setAttribute("providermsgSuccess", "provider.setDefaultSex.msgSuccess");
        request.setAttribute("method", "saveDefaultSex");

        return "gen";
    }
    /////

    public String viewHCType() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();


        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.HC_TYPE);

        if (prop == null) {
            prop = new UserProperty();
        }

        // Add all provinces / states to serviceList
        ArrayList<LabelValueBean> serviceList = constructProvinceList();

        request.setAttribute("dropOpts", serviceList);

        request.setAttribute("dateProperty", prop);

        request.setAttribute("providertitle", "provider.setHCType.title");
        request.setAttribute("providermsgPrefs", "provider.setHCType.msgPrefs");
        request.setAttribute("providermsgProvider", "provider.setHCType.msgHCType");
        request.setAttribute("providermsgEdit", "provider.setHCType.msgEdit");
        request.setAttribute("providerbtnSubmit", "provider.setHCType.btnSubmit");
        request.setAttribute("providermsgSuccess", "provider.setHCType.msgSuccess");
        request.setAttribute("method", "saveHCType");

        this.setDateProperty(prop);
        return "gen";
    }

    public String saveHCType() {


        UserProperty prop = this.getDateProperty();
        String fmt = prop != null ? prop.getValue() : "";
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty saveProperty = this.userPropertyDAO.getProp(providerNo, UserProperty.HC_TYPE);

        if (saveProperty == null) {
            saveProperty = new UserProperty();
            saveProperty.setProviderNo(providerNo);
            saveProperty.setName(UserProperty.HC_TYPE);
        }

        saveProperty.setValue(fmt);
        this.userPropertyDAO.saveProp(saveProperty);

        request.setAttribute("status", "success");
        request.setAttribute("providertitle", "provider.setHCType.title");
        request.setAttribute("providermsgPrefs", "provider.setHCType.msgPrefs");
        request.setAttribute("providermsgProvider", "provider.setHCType.msgHCType");
        request.setAttribute("providermsgEdit", "provider.setHCType.msgEdit");
        request.setAttribute("providerbtnSubmit", "provider.setHCType.btnSubmit");
        request.setAttribute("providermsgSuccess", "provider.setHCType.msgSuccess");
        request.setAttribute("method", "saveHCType");

        return "gen";
    }


    /////

    public String viewMyDrugrefId() {


        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.MYDRUGREF_ID);


        if (prop == null) {
            prop = new UserProperty();
        }

        request.setAttribute("dateProperty", prop);


        request.setAttribute("providertitle", "provider.setmyDrugrefId.title"); //=Set myDrugref ID
        request.setAttribute("providermsgPrefs", "provider.setmyDrugrefId.msgPrefs"); //=Preferences"); //
        request.setAttribute("providermsgProvider", "provider.setmyDrugrefId.msgProvider"); //=myDrugref ID
        request.setAttribute("providermsgEdit", "provider.setmyDrugrefId.msgEdit"); //=Enter your desired login for myDrugref
        request.setAttribute("providerbtnSubmit", "provider.setmyDrugrefId.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.setmyDrugrefId.msgSuccess"); //=myDrugref Id saved
        request.setAttribute("method", "saveMyDrugrefId");

        this.setDateProperty(prop);
        return "gen";
    }

    public String viewRxPageSize() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.RX_PAGE_SIZE);


        if (prop == null) {
            prop = new UserProperty();
        }

        request.setAttribute("providertitle", "provider.setRxPageSize.title"); //=Set Rx Script Page Size
        request.setAttribute("providermsgPrefs", "provider.setRxPageSize.msgPrefs"); //=Preferences"); //
        request.setAttribute("providermsgProvider", "provider.setRxPageSize.msgPageSize"); //=Rx Script Page Size
        request.setAttribute("providermsgEdit", "provider.setRxPageSize.msgEdit"); //=Select your desired page size
        request.setAttribute("providerbtnSubmit", "provider.setRxPageSize.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.setRxPageSize.msgSuccess"); //=Rx Script Page Size saved
        request.setAttribute("method", "saveRxPageSize");

        this.setRxPageSizeProperty(prop);
        return "genRxPageSize";
    }

    public String saveRxPageSize() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();


        UserProperty UPageSize = getRxPageSizeProperty();
        String rxPageSize = "";
        if (UPageSize != null)
            rxPageSize = UPageSize.getValue();
        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.RX_PAGE_SIZE);
        if (prop == null) {
            prop = new UserProperty();
            prop.setName(UserProperty.RX_PAGE_SIZE);
            prop.setProviderNo(providerNo);
        }
        prop.setValue(rxPageSize);
        this.userPropertyDAO.saveProp(prop);

        request.setAttribute("status", "success");
        request.setAttribute("rxPageSizeProperty", prop);
        request.setAttribute("providertitle", "provider.setRxPageSize.title"); //=Set Rx Script Page Size
        request.setAttribute("providermsgPrefs", "provider.setRxPageSize.msgPrefs"); //=Preferences"); //
        request.setAttribute("providermsgProvider", "provider.setRxPageSize.msgPageSize"); //=Rx Script Page Size
        request.setAttribute("providermsgEdit", "provider.setRxPageSize.msgEdit"); //=Select your desired page size
        request.setAttribute("providerbtnSubmit", "provider.setRxPageSize.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.setRxPageSize.msgSuccess"); //=Rx Script Page Size saved
        request.setAttribute("method", "saveRxPageSize");
        return "genRxPageSize";
    }

    public String saveDefaultDocQueue() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();


        UserProperty existingQ = this.getExistingDefaultDocQueueProperty();
        UserProperty newQ = this.getNewDefaultDocQueueProperty();
        String mode = request.getParameter("chooseMode");
        String defaultQ = "";
        if (mode.equalsIgnoreCase("new") && newQ != null)
            defaultQ = newQ.getValue();
        else if (mode.equalsIgnoreCase("existing") && existingQ != null)
            defaultQ = existingQ.getValue();
        else {
            request.setAttribute("status", "success");
            request.setAttribute("providertitle", "provider.setDefaultDocumentQueue.title"); //=Set Default Document Queue
            request.setAttribute("providermsgPrefs", "provider.setDefaultDocumentQueue.msgPrefs"); //=Preferences
            request.setAttribute("providermsgProvider", "provider.setDefaultDocumentQueue.msgProfileView"); //=Default Document Queue
            request.setAttribute("providermsgSuccess", "provider.setDefaultDocumentQueue.msgNotSaved"); //=Default Document Queue has NOT been saved
            request.setAttribute("method", "saveDefaultDocQueue");
            return "genDefaultDocQueue";
        }
        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.DOC_DEFAULT_QUEUE);
        if (prop == null) {
            prop = new UserProperty();
            prop.setName(UserProperty.DOC_DEFAULT_QUEUE);
            prop.setProviderNo(providerNo);
        }
        if (mode.equals("new")) {
            //save and get most recent id
            QueueDao queueDao = (QueueDao) SpringUtils.getBean(QueueDao.class);
            queueDao.addNewQueue(defaultQ);
            String lastId = queueDao.getLastId();
            prop.setValue(lastId);
            this.userPropertyDAO.saveProp(prop);
        } else {
            prop.setValue(defaultQ);
            this.userPropertyDAO.saveProp(prop);
        }
        request.setAttribute("status", "success");
        request.setAttribute("defaultDocQueueProperty", prop);
        request.setAttribute("providertitle", "provider.setDefaultDocumentQueue.title"); //=Set Default Document Queue
        request.setAttribute("providermsgPrefs", "provider.setDefaultDocumentQueue.msgPrefs"); //=Preferences
        request.setAttribute("providermsgProvider", "provider.setDefaultDocumentQueue.msgProfileView"); //=Default Document Queue
        request.setAttribute("providermsgSuccess", "provider.setDefaultDocumentQueue.msgSuccess"); //=Default Document Queue saved
        request.setAttribute("method", "saveDefaultDocQueue");
        return "genDefaultDocQueue";
    }

    //public String viewDefaultDocQueue(){
    //    return "genDefaultDocQueue";
    //}
    public String viewDefaultDocQueue() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();


        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.DOC_DEFAULT_QUEUE);
        UserProperty propNew = new UserProperty();

        if (prop == null) {
            prop = new UserProperty();
        }
        QueueDao queueDao = (QueueDao) SpringUtils.getBean(QueueDao.class);
        List<Hashtable> queues = queueDao.getQueues();
        Collection<LabelValueBean> viewChoices = new ArrayList<LabelValueBean>();
        viewChoices.add(new LabelValueBean("None", "-1"));
        for (Hashtable ht : queues) {
            viewChoices.add(new LabelValueBean((String) ht.get("queue"), (String) ht.get("id")));
        }
        request.setAttribute("viewChoices", viewChoices);
        request.setAttribute("providertitle", "provider.setDefaultDocumentQueue.title"); //=Set Default Document Queue
        request.setAttribute("providermsgPrefs", "provider.setDefaultDocumentQueue.msgPrefs"); //=Preferences
        request.setAttribute("providermsgProvider", "provider.setDefaultDocumentQueue.msgProfileView"); //=Default Document Queue
        request.setAttribute("providermsgEditFromExisting", "provider.setDefaultDocumentQueue.msgEditFromExisting"); //=Choose a default queue from existing queues
        request.setAttribute("providermsgEditSaveNew", "provider.setDefaultDocumentQueue.msgEditSaveNew"); //=Save a new default queue
        request.setAttribute("providerbtnSubmit", "provider.setDefaultDocumentQueue.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.setDefaultDocumentQueue.msgSuccess"); //=Default Document Queue saved
        request.setAttribute("method", "saveDefaultDocQueue");
        this.setExistingDefaultDocQueueProperty(prop);
        this.setNewDefaultDocQueueProperty(propNew);
        return "genDefaultDocQueue";
    }

    public String viewRxProfileView() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();


        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.RX_PROFILE_VIEW);

        String propValue = "";
        if (prop == null) {
            prop = new UserProperty();
        } else {
            propValue = prop.getValue();
        }

        String[] propertyArray = new String[7];
        String[] va = {" show_current ", " show_all ", " active ", " inactive ", " all ", " longterm_acute ", " longterm_acute_inactive_external "};

        for (int i = 0; i < propertyArray.length; i++) {
            if (propValue.contains(va[i])) {
                propertyArray[i] = va[i].trim();
            }//element of array has to match exactly with viewChoices values
        }
        prop.setValueArray(propertyArray);
        Collection<LabelValueBean> viewChoices = new ArrayList<LabelValueBean>();
        viewChoices.add(new LabelValueBean("Current", "show_current"));
        viewChoices.add(new LabelValueBean("All", "show_all"));
        viewChoices.add(new LabelValueBean("Active", "active"));
        viewChoices.add(new LabelValueBean("Expired", "inactive"));
        viewChoices.add(new LabelValueBean("Longterm/Acute", "longterm_acute"));
        viewChoices.add(new LabelValueBean("Longterm/Acute/Inactive/External", "longterm_acute_inactive_external"));
        request.setAttribute("viewChoices", viewChoices);
        request.setAttribute("providertitle", "provider.setRxProfileView.title"); //=Set Rx Profile View
        request.setAttribute("providermsgPrefs", "provider.setRxProfileView.msgPrefs"); //=Preferences
        request.setAttribute("providermsgProvider", "provider.setRxProfileView.msgProfileView"); //=Rx Profile View
        request.setAttribute("providermsgEdit", "provider.setRxProfileView.msgEdit"); //=Select your desired display
        request.setAttribute("providerbtnSubmit", "provider.setRxProfileView.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.setRxProfileView.msgSuccess"); //=Rx Profile View saved
        request.setAttribute("method", "saveRxProfileView");


        this.setRxProfileViewProperty(prop);

        return "genRxProfileView";
    }

    public String saveRxProfileView() {

        try {
            LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
            String providerNo = loggedInInfo.getLoggedInProviderNo();


            UserProperty UProfileView = this.getRxProfileViewProperty();
            String[] va = null;
            if (UProfileView != null)
                va = UProfileView.getValueArray();
            UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.RX_PROFILE_VIEW);
            if (prop == null) {
                prop = new UserProperty();
                prop.setName(UserProperty.RX_PROFILE_VIEW);
                prop.setProviderNo(providerNo);
            }

            String rxProfileView = "";
            if (va != null) {
                for (int i = 0; i < va.length; i++) {
                    rxProfileView += " " + va[i] + " ";
                }
            }
            prop.setValue(rxProfileView);
            this.userPropertyDAO.saveProp(prop);

            request.setAttribute("status", "success");
            request.setAttribute("defaultDocQueueProperty", prop);
            request.setAttribute("providertitle", "provider.setRxProfileView.title"); //=Set Rx Profile View
            request.setAttribute("providermsgPrefs", "provider.setRxProfileView.msgPrefs"); //=Preferences
            request.setAttribute("providermsgProvider", "provider.setRxProfileView.msgProfileView"); //=Rx Profile View
            request.setAttribute("providermsgEdit", "provider.setRxProfileView.msgEdit"); //=Select your desired display
            request.setAttribute("providerbtnSubmit", "provider.setRxProfileView.btnSubmit"); //=Save
            request.setAttribute("providermsgSuccess", "provider.setRxProfileView.msgSuccess"); //=Rx Profile View saved
            request.setAttribute("method", "saveRxProfileView");
        } catch (Exception e) {
            MiscUtils.getLogger().error("Error", e);
        }

        return "genRxProfileView";
    }

    public String viewShowPatientDOB() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();


        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.RX_SHOW_PATIENT_DOB);

        String propValue = "";
        if (prop == null) {
            prop = new UserProperty();
        } else {
            propValue = prop.getValue();
        }

        //String [] propertyArray= new String[7];
        boolean checked;
        if (propValue.equalsIgnoreCase("yes"))
            checked = true;
        else
            checked = false;

        prop.setChecked(checked);
        request.setAttribute("rxShowPatientDOBProperty", prop);
        request.setAttribute("providertitle", "provider.setShowPatientDOB.title"); //=Select if you want to use Rx3
        request.setAttribute("providermsgPrefs", "provider.setShowPatientDOB.msgPrefs"); //=Preferences
        request.setAttribute("providermsgProvider", "provider.setShowPatientDOB.msgProfileView"); //=Use Rx3
        request.setAttribute("providermsgEdit", "provider.setShowPatientDOB.msgEdit"); //=Do you want to use Rx3?
        request.setAttribute("providerbtnSubmit", "provider.setShowPatientDOB.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.setShowPatientDOB.msgSuccess"); //=Rx3 Selection saved
        request.setAttribute("method", "saveShowPatientDOB");

        this.setRxShowPatientDOBProperty(prop);
        return "genSho";
    }

    public String saveShowPatientDOB() {

        UserProperty UShowPatientDOB = this.getRxShowPatientDOBProperty();

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        boolean checked = false;
        if (UShowPatientDOB != null)
            checked = UShowPatientDOB.isChecked();
        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.RX_SHOW_PATIENT_DOB);
        if (prop == null) {
            prop = new UserProperty();
            prop.setName(UserProperty.RX_SHOW_PATIENT_DOB);
            prop.setProviderNo(providerNo);
        }
        String showPatientDOB = "no";
        if (checked)
            showPatientDOB = "yes";
        prop.setValue(showPatientDOB);
        this.userPropertyDAO.saveProp(prop);

        request.setAttribute("status", "success");
        request.setAttribute("rxShowPatientDOBProperty", prop);
        request.setAttribute("providertitle", "provider.setShowPatientDOB.title"); //=Select if you want to use Rx3
        request.setAttribute("providermsgPrefs", "provider.setShowPatientDOB.msgPrefs"); //=Preferences
        request.setAttribute("providermsgProvider", "provider.setShowPatientDOB.msgProfileView"); //=Use Rx3
        request.setAttribute("providermsgEdit", "provider.setShowPatientDOB.msgEdit"); //=Do you want to use Rx3?
        request.setAttribute("providerbtnSubmit", "provider.setShowPatientDOB.btnSubmit"); //=Save
        if (checked)
            request.setAttribute("providermsgSuccess", "provider.setShowPatientDOB.msgSuccess_selected"); //=Rx3 is selected
        else
            request.setAttribute("providermsgSuccess", "provider.setShowPatientDOB.msgSuccess_unselected"); //=Rx3 is unselected
        request.setAttribute("method", "saveShowPatientDOB");
        return "genSho";
    }

    public String viewUseMyMeds() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();


        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.USE_MYMEDS);
        if (prop == null) prop = new UserProperty();

        String propValue = prop.getValue();
        boolean checked = Boolean.parseBoolean(propValue);

        prop.setChecked(checked);
        request.setAttribute("useMyMedsProperty", prop);
        request.setAttribute("providertitle", "provider.setUseMyMeds.title"); //=Select if you want to use MyMeds
        request.setAttribute("providermsgPrefs", "provider.setUseMyMeds.msgPrefs"); //=Preferences
        request.setAttribute("providermsgProvider", "provider.setUseMyMeds.msgProfileView"); //=Use MyMeds
        request.setAttribute("providermsgEdit", "provider.setUseMyMeds.msgEdit"); //=Do you want to use MyMeds?
        request.setAttribute("providerbtnSubmit", "provider.setUseMyMeds.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.setUseMyMeds.msgSuccess"); //=MyMeds Selection saved
        request.setAttribute("method", "saveUseMyMeds");

        this.setUseMyMedsProperty(prop);
        return "genUseMyMeds";
    }

    public String saveUseMyMeds() {

        UserProperty UUseMyMeds = this.getUseMyMedsProperty();
        //UserProperty UUseRx3=(UserProperty)request.getAttribute("rxUseRx3Property");

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        boolean checked = false;
        if (UUseMyMeds != null)
            checked = UUseMyMeds.isChecked();
        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.USE_MYMEDS);
        if (prop == null) {
            prop = new UserProperty();
            prop.setName(UserProperty.USE_MYMEDS);
            prop.setProviderNo(providerNo);
        }
        prop.setValue(String.valueOf(checked));
        this.userPropertyDAO.saveProp(prop);

        request.setAttribute("status", "success");
        request.setAttribute("useMyMedsProperty", prop);
        request.setAttribute("providertitle", "provider.setUseMyMeds.title"); //=Select if you want to use Rx3
        request.setAttribute("providermsgPrefs", "provider.setUseMyMeds.msgPrefs"); //=Preferences
        request.setAttribute("providermsgProvider", "provider.setUseMyMeds.msgProfileView"); //=Use Rx3
        request.setAttribute("providermsgEdit", "provider.setUseMyMeds.msgEdit"); //=Check if you want to use Rx3
        request.setAttribute("providerbtnSubmit", "provider.setUseMyMeds.btnSubmit"); //=Save
        if (checked)
            request.setAttribute("providermsgSuccess", "provider.setUseMyMeds.msgSuccess_selected"); //=Rx3 is selected
        else
            request.setAttribute("providermsgSuccess", "provider.setUseMyMeds.msgSuccess_unselected"); //=Rx3 is unselected
        request.setAttribute("method", "saveUseMyMeds");
        return "genUseMyMeds";
    }

    public String viewUseRx3() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();


        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.RX_USE_RX3);

        String propValue = "";
        if (prop == null) {
            prop = new UserProperty();
        } else {
            propValue = prop.getValue();
        }

        //String [] propertyArray= new String[7];
        boolean checked;
        if (propValue.equalsIgnoreCase("yes"))
            checked = true;
        else
            checked = false;

        prop.setChecked(checked);
        request.setAttribute("rxUseRx3Property", prop);
        request.setAttribute("providertitle", "provider.setRxRxUseRx3.title"); //=Select if you want to use Rx3
        request.setAttribute("providermsgPrefs", "provider.setRxRxUseRx3.msgPrefs"); //=Preferences
        request.setAttribute("providermsgProvider", "provider.setRxRxUseRx3.msgProfileView"); //=Use Rx3
        request.setAttribute("providermsgEdit", "provider.setRxUseRx3.msgEdit"); //=Do you want to use Rx3?
        request.setAttribute("providerbtnSubmit", "provider.setRxUseRx3.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.setRxUseRx3.msgSuccess"); //=Rx3 Selection saved
        request.setAttribute("method", "saveUseRx3");

        this.setRxUseRx3Property(prop);
        return "genRxUseRx3";
    }

    public String saveUseRx3() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();


        UserProperty UUseRx3 = this.getRxUseRx3Property();

        boolean checked = false;
        if (UUseRx3 != null)
            checked = UUseRx3.isChecked();
        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.RX_USE_RX3);
        if (prop == null) {
            prop = new UserProperty();
            prop.setName(UserProperty.RX_USE_RX3);
            prop.setProviderNo(providerNo);
        }
        String useRx3 = "no";
        if (checked)
            useRx3 = "yes";
        prop.setValue(useRx3);
        this.userPropertyDAO.saveProp(prop);

        request.setAttribute("status", "success");
        request.setAttribute("rxUseRx3Property", prop);
        request.setAttribute("providertitle", "provider.setRxRxUseRx3.title"); //=Select if you want to use Rx3
        request.setAttribute("providermsgPrefs", "provider.setRxRxUseRx3.msgPrefs"); //=Preferences
        request.setAttribute("providermsgProvider", "provider.setRxRxUseRx3.msgProfileView"); //=Use Rx3
        request.setAttribute("providermsgEdit", "provider.setRxUseRx3.msgEdit"); //=Check if you want to use Rx3
        request.setAttribute("providerbtnSubmit", "provider.setRxUseRx3.btnSubmit"); //=Save
        if (checked)
            request.setAttribute("providermsgSuccess", "provider.setRxUseRx3.msgSuccess_selected"); //=Rx3 is selected
        else
            request.setAttribute("providermsgSuccess", "provider.setRxUseRx3.msgSuccess_unselected"); //=Rx3 is unselected
        request.setAttribute("method", "saveUseRx3");
        return "genRxUseRx3";
    }

    public String viewDefaultQuantity() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();


        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.RX_DEFAULT_QUANTITY);


        if (prop == null) {
            prop = new UserProperty();
        }

        //request.setAttribute("propert",propertyToSet);
        request.setAttribute("rxDefaultQuantityProperty", prop);
        request.setAttribute("providertitle", "provider.setRxDefaultQuantity.title"); //=Set Rx Default Quantity
        request.setAttribute("providermsgPrefs", "provider.setRxDefaultQuantity.msgPrefs"); //=Preferences"); //
        request.setAttribute("providermsgProvider", "provider.setRxDefaultQuantity.msgDefaultQuantity"); //=Rx Default Quantity
        request.setAttribute("providermsgEdit", "provider.setRxDefaultQuantity.msgEdit"); //=Enter your desired quantity
        request.setAttribute("providerbtnSubmit", "provider.setRxDefaultQuantity.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.setRxDefaultQuantity.msgSuccess"); //=Rx Default Quantity saved
        request.setAttribute("method", "saveDefaultQuantity");

        this.setRxDefaultQuantityProperty(prop);

        return "genRxDefaultQuantity";
    }

    public String saveDefaultQuantity() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();


        UserProperty UDefaultQuantity = this.getRxDefaultQuantityProperty();
        String rxDefaultQuantity = "";
        if (UDefaultQuantity != null)
            rxDefaultQuantity = UDefaultQuantity.getValue();
        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.RX_DEFAULT_QUANTITY);
        if (prop == null) {
            prop = new UserProperty();
            prop.setName(UserProperty.RX_DEFAULT_QUANTITY);
            prop.setProviderNo(providerNo);
        }
        prop.setValue(rxDefaultQuantity);
        this.userPropertyDAO.saveProp(prop);

        request.setAttribute("status", "success");
        request.setAttribute("rxDefaultQuantityProperty", prop);
        request.setAttribute("providertitle", "provider.setRxDefaultQuantity.title"); //=Set Rx Default Quantity
        request.setAttribute("providermsgPrefs", "provider.setRxDefaultQuantity.msgPrefs"); //=Preferences"); //
        request.setAttribute("providermsgProvider", "provider.setRxDefaultQuantity.msgDefaultQuantity"); //=Rx Default Quantity
        request.setAttribute("providermsgEdit", "provider.setRxDefaultQuantity.msgEdit"); //=Enter your desired quantity
        request.setAttribute("providerbtnSubmit", "provider.setRxDefaultQuantity.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.setRxDefaultQuantity.msgSuccess"); //=Rx Default Quantity saved
        request.setAttribute("method", "saveDefaultQuantity");
        return "genRxDefaultQuantity";

    }

    public String saveMyDrugrefId() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();


        UserProperty UdrugrefId = this.getDateProperty();
        String drugrefId = "";

        if (UdrugrefId != null) {
            drugrefId = UdrugrefId.getValue();
        }

        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.MYDRUGREF_ID);

        if (prop == null) {
            prop = new UserProperty();
            prop.setName(UserProperty.MYDRUGREF_ID);
            prop.setProviderNo(providerNo);
        }
        prop.setValue(drugrefId);

        this.userPropertyDAO.saveProp(prop);

        request.setAttribute("status", "success");
        request.setAttribute("dateProperty", prop);
        request.setAttribute("providertitle", "provider.setmyDrugrefId.title"); //=Set myDrugref ID
        request.setAttribute("providermsgPrefs", "provider.setmyDrugrefId.msgPrefs"); //=Preferences"); //
        request.setAttribute("providermsgProvider", "provider.setmyDrugrefId.msgProvider"); //=myDrugref ID
        request.setAttribute("providermsgEdit", "provider.setmyDrugrefId.msgEdit"); //=Enter your desired login for myDrugref
        request.setAttribute("providerbtnSubmit", "provider.setmyDrugrefId.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.setmyDrugrefId.msgSuccess"); //=myDrugref Id saved
        request.setAttribute("method", "saveMyDrugrefId");
        return "gen";
    }
    /////


    /*ontario md*/
    public String viewOntarioMDId() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.ONTARIO_MD_USERNAME);
        UserProperty prop2 = this.userPropertyDAO.getProp(providerNo, UserProperty.ONTARIO_MD_PASSWORD);

        if (prop == null) {
            prop = new UserProperty();
        }

        if (prop2 == null) {
            prop2 = new UserProperty();
        }

        //request.setAttribute("propert",propertyToSet);
        request.setAttribute("dateProperty", prop);
        request.setAttribute("dateProperty2", prop2);


        request.setAttribute("providertitle", "provider.setOntarioMD.title"); //=Set myDrugref ID
        request.setAttribute("providermsgPrefs", "provider.setOntarioMD.msgPrefs"); //=Preferences"); //
        request.setAttribute("providermsgProvider", "provider.setOntarioMD.msgProvider"); //=myDrugref ID
        request.setAttribute("providermsgEdit", "provider.setOntarioMD.msgEdit"); //=Enter your desired login for myDrugref
        request.setAttribute("providerbtnSubmit", "provider.setOntarioMD.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.setOntarioMD.msgSuccess"); //=myDrugref Id saved
        request.setAttribute("method", "saveOntarioMDId");

        this.setDateProperty(prop);
        return "gen";
    }


    public String saveOntarioMDId() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();


        UserProperty UdrugrefId = this.getDateProperty();
        String drugrefId = "";

        if (UdrugrefId != null) {
            drugrefId = UdrugrefId.getValue();
        }

        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.ONTARIO_MD_USERNAME);

        if (prop == null) {
            prop = new UserProperty();
            prop.setName(UserProperty.ONTARIO_MD_USERNAME);
            prop.setProviderNo(providerNo);
        }
        prop.setValue(drugrefId);

        this.userPropertyDAO.saveProp(prop);


        UserProperty UdrugrefId2 = this.getDateProperty2();
        String drugrefId2 = "";

        if (UdrugrefId2 != null) {
            drugrefId2 = UdrugrefId2.getValue();
        }

        UserProperty prop2 = this.userPropertyDAO.getProp(providerNo, UserProperty.ONTARIO_MD_PASSWORD);

        if (prop2 == null) {
            prop2 = new UserProperty();
            prop2.setName(UserProperty.ONTARIO_MD_PASSWORD);
            prop2.setProviderNo(providerNo);
        }
        prop2.setValue(drugrefId2);

        this.userPropertyDAO.saveProp(prop2);


        request.setAttribute("status", "success");
        request.setAttribute("dateProperty", prop);
        request.setAttribute("providertitle", "provider.setOntarioMD.title"); //=Set myDrugref ID
        request.setAttribute("providermsgPrefs", "provider.setOntarioMD.msgPrefs"); //=Preferences"); //
        request.setAttribute("providermsgProvider", "provider.setOntarioMD.msgProvider"); //=myDrugref ID
        request.setAttribute("providermsgEdit", "provider.setOntarioMD.msgEdit"); //=Enter your desired login for myDrugref
        request.setAttribute("providerbtnSubmit", "provider.setOntarioMD.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.setOntarioMD.msgSuccess"); //=myDrugref Id saved
        request.setAttribute("method", "saveOntarioMDId");
        return "gen";
    }
    /*ontario md*/

    public String viewConsultationRequestCuffOffDate() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.CONSULTATION_TIME_PERIOD_WARNING);

        if (prop == null) {
            prop = new UserProperty();
        }

        request.setAttribute("dateProperty", prop);

        request.setAttribute("providertitle", "provider.setConsultationCutOffDate.title"); //=Set myDrugref ID
        request.setAttribute("providermsgPrefs", "provider.setConsultationCutOffDate.msgPrefs"); //=Preferences"); //
        request.setAttribute("providermsgProvider", "provider.setConsultationCutOffDate.msgProvider"); //=myDrugref ID
        request.setAttribute("providermsgEdit", "provider.setConsultationCutOffDate.msgEdit"); //=Enter your desired login for myDrugref
        request.setAttribute("providerbtnSubmit", "provider.setConsultationCutOffDate.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.setConsultationCutOffDate.msgSuccess"); //=myDrugref Id saved
        request.setAttribute("method", "saveConsultationRequestCuffOffDate");

        this.setDateProperty(prop);
        return "gen";
    }


    public String saveConsultationRequestCuffOffDate() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty UdrugrefId = this.getDateProperty();
        String drugrefId = "";

        if (UdrugrefId != null) {
            drugrefId = UdrugrefId.getValue();
        }

        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.CONSULTATION_TIME_PERIOD_WARNING);

        if (prop == null) {
            prop = new UserProperty();
            prop.setName(UserProperty.CONSULTATION_TIME_PERIOD_WARNING);
            prop.setProviderNo(providerNo);
        }
        prop.setValue(drugrefId);

        this.userPropertyDAO.saveProp(prop);

        request.setAttribute("status", "success");
        request.setAttribute("dateProperty", prop);
        request.setAttribute("providertitle", "provider.setConsultationCutOffDate.title"); //=Set myDrugref ID
        request.setAttribute("providermsgPrefs", "provider.setConsultationCutOffDate.msgPrefs"); //=Preferences"); //
        request.setAttribute("providermsgProvider", "provider.setConsultationCutOffDate.msgProvider"); //=myDrugref ID
        request.setAttribute("providermsgEdit", "provider.setConsultationCutOffDate.msgEdit"); //=Enter your desired login for myDrugref
        request.setAttribute("providerbtnSubmit", "provider.setConsultationCutOffDate.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.setConsultationCutOffDate.msgSuccess"); //=myDrugref Id saved
        request.setAttribute("method", "saveConsultationRequestCuffOffDate");
        return "gen";
    }


    //// CONSULT TEAM

    public String viewConsultationRequestTeamWarning() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.CONSULTATION_TEAM_WARNING);

        if (prop == null) {
            prop = new UserProperty();
        }

        EctConsultationFormRequestUtil conUtil = new EctConsultationFormRequestUtil();
        conUtil.estTeams();
        Vector<String> vect = conUtil.teamVec;

        ArrayList<LabelValueBean> serviceList = new ArrayList<LabelValueBean>();
        serviceList.add(new LabelValueBean("All", "-1"));
        for (String s : vect) {
            serviceList.add(new LabelValueBean(s, s));
        }
        serviceList.add(new LabelValueBean("None", ""));


        //conUtil.teamVec.add("All");
        //conUtil.teamVec.add("None");

        request.setAttribute("dropOpts", serviceList);

        request.setAttribute("dateProperty", prop);

        request.setAttribute("providertitle", "provider.setConsultationTeamWarning.title"); //=Set myDrugref ID
        request.setAttribute("providermsgPrefs", "provider.setConsultationTeamWarning.msgPrefs"); //=Preferences"); //
        request.setAttribute("providermsgProvider", "provider.setConsultationTeamWarning.msgProvider"); //=myDrugref ID
        request.setAttribute("providermsgEdit", "provider.setConsultationTeamWarning.msgEdit"); //=Enter your desired login for myDrugref
        request.setAttribute("providerbtnSubmit", "provider.setConsultationTeamWarning.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.setConsultationTeamWarning.msgSuccess"); //=myDrugref Id saved
        request.setAttribute("method", "saveConsultationRequestTeamWarning");

        this.setDateProperty(prop);
        return "gen";
    }


    public String saveConsultationRequestTeamWarning() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty UdrugrefId = this.getDateProperty();
        String drugrefId = "";

        if (UdrugrefId != null) {
            drugrefId = UdrugrefId.getValue();
        }

        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.CONSULTATION_TEAM_WARNING);

        if (prop == null) {
            prop = new UserProperty();
            prop.setName(UserProperty.CONSULTATION_TEAM_WARNING);
            prop.setProviderNo(providerNo);
        }
        prop.setValue(drugrefId);

        this.userPropertyDAO.saveProp(prop);


        EctConsultationFormRequestUtil conUtil = new EctConsultationFormRequestUtil();
        conUtil.estTeams();
        Vector<String> vect = conUtil.teamVec;

        ArrayList<LabelValueBean> serviceList = new ArrayList<LabelValueBean>();
        serviceList.add(new LabelValueBean("All", "-1"));
        for (String s : vect) {
            serviceList.add(new LabelValueBean(s, s));
        }
        serviceList.add(new LabelValueBean("None", ""));
        request.setAttribute("dropOpts", serviceList);


        request.setAttribute("status", "success");
        request.setAttribute("dateProperty", prop);
        request.setAttribute("providertitle", "provider.setConsultationTeamWarning.title"); //=Set myDrugref ID
        request.setAttribute("providermsgPrefs", "provider.setConsultationTeamWarning.msgPrefs"); //=Preferences"); //
        request.setAttribute("providermsgProvider", "provider.setConsultationTeamWarning.msgProvider"); //=myDrugref ID
        request.setAttribute("providermsgEdit", "provider.setConsultationTeamWarning.msgEdit"); //=Enter your desired login for myDrugref
        request.setAttribute("providerbtnSubmit", "provider.setConsultationTeamWarning.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.setConsultationTeamWarning.msgSuccess"); //=myDrugref Id saved
        request.setAttribute("method", "saveConsultationRequestTeamWarning");
        return "gen";
    }


    //WORKLOAD MANAGEMENT SCREEN PROPERTY
    public String viewWorkLoadManagement() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.WORKLOAD_MANAGEMENT);

        if (prop == null)
            prop = new UserProperty();

        ArrayList<LabelValueBean> serviceList = new ArrayList<LabelValueBean>();
        CtlBillingServiceDao dao = SpringUtils.getBean(CtlBillingServiceDao.class);
        for (Object[] service : dao.getUniqueServiceTypes())
            serviceList.add(new LabelValueBean(String.valueOf(service[0]), String.valueOf(service[1])));
        serviceList.add(new LabelValueBean("None", ""));

        request.setAttribute("dropOpts", serviceList);

        request.setAttribute("dateProperty", prop);

        request.setAttribute("providertitle", "provider.setWorkLoadManagement.title"); //=Set myDrugref ID
        request.setAttribute("providermsgPrefs", "provider.setWorkLoadManagement.msgPrefs"); //=Preferences"); //
        request.setAttribute("providermsgProvider", "provider.setWorkLoadManagement.msgProvider"); //=myDrugref ID
        request.setAttribute("providermsgEdit", "provider.setWorkLoadManagement.msgEdit"); //=Enter your desired login for myDrugref
        request.setAttribute("providerbtnSubmit", "provider.setWorkLoadManagement.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.setWorkLoadManagement.msgSuccess"); //=myDrugref Id saved
        request.setAttribute("method", "saveWorkLoadManagement");

        this.setDateProperty(prop);
        return "gen";
    }


    public String saveWorkLoadManagement() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty UdrugrefId = this.getDateProperty();
        String drugrefId = "";

        if (UdrugrefId != null) {
            drugrefId = UdrugrefId.getValue();
        }

        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.WORKLOAD_MANAGEMENT);

        if (prop == null) {
            prop = new UserProperty();
            prop.setName(UserProperty.WORKLOAD_MANAGEMENT);
            prop.setProviderNo(providerNo);
        }
        prop.setValue(drugrefId);

        this.userPropertyDAO.saveProp(prop);

        ArrayList<LabelValueBean> serviceList = new ArrayList<LabelValueBean>();
        CtlBillingServiceDao dao = SpringUtils.getBean(CtlBillingServiceDao.class);
        for (Object[] service : dao.getUniqueServiceTypes())
            serviceList.add(new LabelValueBean(String.valueOf(service[0]), String.valueOf(service[1])));

        request.setAttribute("dropOpts", serviceList);

        request.setAttribute("status", "success");
        request.setAttribute("dateProperty", prop);
        request.setAttribute("providertitle", "provider.setWorkLoadManagement.title"); //=Set myDrugref ID
        request.setAttribute("providermsgPrefs", "provider.setWorkLoadManagement.msgPrefs"); //=Preferences"); //
        request.setAttribute("providermsgProvider", "provider.setWorkLoadManagement.msgProvider"); //=myDrugref ID
        request.setAttribute("providermsgEdit", "provider.setWorkLoadManagement.msgEdit"); //=Enter your desired login for myDrugref
        request.setAttribute("providerbtnSubmit", "provider.setWorkLoadManagement.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.setWorkLoadManagement.msgSuccess"); //=myDrugref Id saved
        request.setAttribute("method", "saveWorkLoadManagement");
        return "gen";
    }

    //How does cpp paste into consult request property
    public String viewConsultPasteFmt() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.CONSULTATION_REQ_PASTE_FMT);

        if (prop == null) {
            prop = new UserProperty();
        }

        ArrayList<LabelValueBean> serviceList = new ArrayList<LabelValueBean>();
        serviceList.add(new LabelValueBean("Single Line", "single"));
        serviceList.add(new LabelValueBean("Multi Line", "multi"));

        request.setAttribute("dropOpts", serviceList);

        request.setAttribute("dateProperty", prop);

        request.setAttribute("providertitle", "provider.setConsulReqtPasteFmt.title");
        request.setAttribute("providermsgPrefs", "provider.setConsulReqtPasteFmt.msgPrefs");
        request.setAttribute("providermsgProvider", "provider.setConsulReqtPasteFmt.msgProvider");
        request.setAttribute("providermsgEdit", "provider.setConsulReqtPasteFmt.msgEdit");
        request.setAttribute("providerbtnSubmit", "provider.setConsulReqtPasteFmt.btnSubmit");
        request.setAttribute("providermsgSuccess", "provider.setConsulReqtPasteFmt.msgSuccess");
        request.setAttribute("method", "saveConsultPasteFmt");

        this.setDateProperty(prop);
        return "gen";
    }

    public String saveConsultPasteFmt() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();


        UserProperty prop = this.getDateProperty();
        String fmt = prop != null ? prop.getValue() : "";

        UserProperty saveProperty = this.userPropertyDAO.getProp(providerNo, UserProperty.CONSULTATION_REQ_PASTE_FMT);

        if (saveProperty == null) {
            saveProperty = new UserProperty();
            saveProperty.setProviderNo(providerNo);
            saveProperty.setName(UserProperty.CONSULTATION_REQ_PASTE_FMT);
        }

        saveProperty.setValue(fmt);
        this.userPropertyDAO.saveProp(saveProperty);

        request.setAttribute("status", "success");
        request.setAttribute("providertitle", "provider.setConsulReqtPasteFmt.title");
        request.setAttribute("providermsgPrefs", "provider.setConsulReqtPasteFmt.msgPrefs");
        request.setAttribute("providermsgProvider", "provider.setConsulReqtPasteFmt.msgProvider");
        request.setAttribute("providermsgEdit", "provider.setConsulReqtPasteFmt.msgEdit");
        request.setAttribute("providerbtnSubmit", "provider.setConsulReqtPasteFmt.btnSubmit");
        request.setAttribute("providermsgSuccess", "provider.setConsulReqtPasteFmt.msgSuccess");
        request.setAttribute("method", "saveConsultPasteFmt");

        return "gen";
    }

    // Constructs a list of LabelValueBeans, to be used as the dropdown list
    // when viewing a HCType preference
    public ArrayList<LabelValueBean> constructProvinceList() {

        ArrayList<LabelValueBean> provinces = new ArrayList<LabelValueBean>();

        provinces.add(new LabelValueBean("AB-Alberta", "AB"));
        provinces.add(new LabelValueBean("BC-British Columbia", "BC"));
        provinces.add(new LabelValueBean("MB-Manitoba", "MB"));
        provinces.add(new LabelValueBean("NB-New Brunswick", "NB"));
        provinces.add(new LabelValueBean("NL-Newfoundland", "NL"));
        provinces.add(new LabelValueBean("NT-Northwest Territory", "NT"));
        provinces.add(new LabelValueBean("NS-Nova Scotia", "NS"));
        provinces.add(new LabelValueBean("NU-Nunavut", "NU"));
        provinces.add(new LabelValueBean("ON-Ontario", "ON"));
        provinces.add(new LabelValueBean("PE-Prince Edward Island", "PE"));
        provinces.add(new LabelValueBean("QC-Quebec", "QC"));
        provinces.add(new LabelValueBean("SK-Saskatchewan", "SK"));
        provinces.add(new LabelValueBean("YT-Yukon", "YK"));
        provinces.add(new LabelValueBean("US resident", "US"));
        provinces.add(new LabelValueBean("US-AK-Alaska", "US-AK"));
        provinces.add(new LabelValueBean("US-AL-Alabama", "US-AL"));
        provinces.add(new LabelValueBean("US-AR-Arkansas", "US-AR"));
        provinces.add(new LabelValueBean("US-AZ-Arizona", "US-AZ"));
        provinces.add(new LabelValueBean("US-CA-California", "US-CA"));
        provinces.add(new LabelValueBean("US-CO-Colorado", "US-CO"));
        provinces.add(new LabelValueBean("US-CT-Connecticut", "US-CT"));
        provinces.add(new LabelValueBean("US-CZ-Canal Zone", "US-CZ"));
        provinces.add(new LabelValueBean("US-DC-District of Columbia", "US-DC"));
        provinces.add(new LabelValueBean("US-DE-Delaware", "US-DE"));
        provinces.add(new LabelValueBean("US-FL-Florida", "US-FL"));
        provinces.add(new LabelValueBean("US-GA-Georgia", "US-GA"));
        provinces.add(new LabelValueBean("US-GU-Guam", "US-GU"));
        provinces.add(new LabelValueBean("US-HI-Hawaii", "US-HI"));
        provinces.add(new LabelValueBean("US-IA-Iowa", "US-IA"));
        provinces.add(new LabelValueBean("US-ID-Idaho", "US-ID"));
        provinces.add(new LabelValueBean("US-IL-Illinois", "US-IL"));
        provinces.add(new LabelValueBean("US-IN-Indiana", "US-IN"));
        provinces.add(new LabelValueBean("US-KS-Kansas", "US-KS"));
        provinces.add(new LabelValueBean("US-KY-Kentucky", "US-KY"));
        provinces.add(new LabelValueBean("US-LA-Louisiana", "US-LA"));
        provinces.add(new LabelValueBean("US-MA-Massachusetts", "US-MA"));
        provinces.add(new LabelValueBean("US-MD-Maryland", "US-MD"));
        provinces.add(new LabelValueBean("US-ME-Maine", "US-ME"));
        provinces.add(new LabelValueBean("US-MI-Michigan", "US-MI"));
        provinces.add(new LabelValueBean("US-MN-Minnesota", "US-MN"));
        provinces.add(new LabelValueBean("US-MO-Missouri", "US-MO"));
        provinces.add(new LabelValueBean("US-MS-Mississippi", "US-MS"));
        provinces.add(new LabelValueBean("US-MT-Montana", "US-MT"));
        provinces.add(new LabelValueBean("US-NC-North Carolina", "US-NC"));
        provinces.add(new LabelValueBean("US-ND-North Dakota", "US-ND"));
        provinces.add(new LabelValueBean("US-NE-Nebraska", "US-NE"));
        provinces.add(new LabelValueBean("US-NH-New Hampshire", "US-NH"));
        provinces.add(new LabelValueBean("US-NJ-New Jersey", "US-NJ"));
        provinces.add(new LabelValueBean("US-NM-New Mexico", "US-NM"));
        provinces.add(new LabelValueBean("US-NU-Nunavut", "US-NU"));
        provinces.add(new LabelValueBean("US-NV-Nevada", "US-NV"));
        provinces.add(new LabelValueBean("US-NY-New York", "US-NY"));
        provinces.add(new LabelValueBean("US-OH-Ohio", "US-OH"));
        provinces.add(new LabelValueBean("US-OK-Oklahoma", "US-OK"));
        provinces.add(new LabelValueBean("US-OR-Oregon", "US-OR"));
        provinces.add(new LabelValueBean("US-PA-Pennsylvania", "US-PA"));
        provinces.add(new LabelValueBean("US-PR-Puerto Rico", "US-PR"));
        provinces.add(new LabelValueBean("US-RI-Rhode Island", "US-RI"));
        provinces.add(new LabelValueBean("US-SC-South Carolina", "US-SC"));
        provinces.add(new LabelValueBean("US-SD-South Dakota", "US-SD"));
        provinces.add(new LabelValueBean("US-TN-Tennessee", "US-TN"));
        provinces.add(new LabelValueBean("US-TX-Texas", "US-TX"));
        provinces.add(new LabelValueBean("US-UT-Utah", "US-UT"));
        provinces.add(new LabelValueBean("US-VA-Virginia", "US-VA"));
        provinces.add(new LabelValueBean("US-VI-Virgin Islands", "US-VI"));
        provinces.add(new LabelValueBean("US-VT-Vermont", "US-VT"));
        provinces.add(new LabelValueBean("US-WA-Washington", "US-WA"));
        provinces.add(new LabelValueBean("US-WI-Wisconsin", "US-WI"));
        provinces.add(new LabelValueBean("US-WV-West Virginia", "US-WV"));
        provinces.add(new LabelValueBean("US-WY-Wyoming", "US-WY"));

        return provinces;
    }


    public String viewFavouriteEformGroup() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.EFORM_FAVOURITE_GROUP);

        if (prop == null) {
            prop = new UserProperty();
        }

        this.setDateProperty(prop);
        ArrayList<HashMap<String, String>> groups = EFormUtil.getEFormGroups();
        ArrayList<LabelValueBean> groupList = new ArrayList<LabelValueBean>();
        String name;
        groupList.add(new LabelValueBean("None", ""));
        for (HashMap<String, String> h : groups) {
            name = h.get("groupName");
            groupList.add(new LabelValueBean(name, name));
        }

        request.setAttribute("dropOpts", groupList);

        request.setAttribute("dateProperty", prop);

        request.setAttribute("providertitle", "provider.setFavEfrmGrp.title"); //=Set Favourite Eform Group
        request.setAttribute("providermsgPrefs", "provider.setFavEfrmGrp.msgPrefs"); //=Preferences"); //
        request.setAttribute("providermsgProvider", "provider.setFavEfrmGrp.msgProvider"); //=Default Eform Group
        request.setAttribute("providermsgEdit", "provider.setFavEfrmGrp.msgEdit"); //=Select your favourite Eform Group
        request.setAttribute("providerbtnSubmit", "provider.setFavEfrmGrp.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.setFavEfrmGrp.msgSuccess"); //=Favourite Eform Group saved
        request.setAttribute("method", "saveFavouriteEformGroup");
        return "gen";
    }

    public String saveFavouriteEformGroup() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty prop = this.getDateProperty();
        String group = prop != null ? prop.getValue() : "";

        UserProperty saveProperty = this.userPropertyDAO.getProp(providerNo, UserProperty.EFORM_FAVOURITE_GROUP);

        if (saveProperty == null) {
            saveProperty = new UserProperty();
            saveProperty.setProviderNo(providerNo);
            saveProperty.setName(UserProperty.EFORM_FAVOURITE_GROUP);
        }

        if (group.equalsIgnoreCase("")) {
            this.userPropertyDAO.delete(saveProperty);
        } else {
            saveProperty.setValue(group);
            this.userPropertyDAO.saveProp(saveProperty);
        }

        request.setAttribute("status", "success");
        request.setAttribute("providertitle", "provider.setFavEfrmGrp.title"); //=Set Favourite Eform Group
        request.setAttribute("providermsgPrefs", "provider.setFavEfrmGrp.msgPrefs"); //=Preferences"); //
        request.setAttribute("providermsgProvider", "provider.setFavEfrmGrp.msgProvider"); //=Default Eform Group
        request.setAttribute("providermsgEdit", "provider.setFavEfrmGrp.msgEdit"); //=Select your favourite Eform Group
        request.setAttribute("providerbtnSubmit", "provider.setFavEfrmGrp.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.setFavEfrmGrp.msgSuccess"); //=Favourite Eform Group saved
        request.setAttribute("method", "saveFavouriteEformGroup");

        return "gen";
    }

    public String viewCppSingleLine() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.CPP_SINGLE_LINE);

        String propValue = "";
        if (prop == null) {
            prop = new UserProperty();
        } else {
            propValue = prop.getValue();
        }

        boolean checked;
        if (propValue.equalsIgnoreCase("yes"))
            checked = true;
        else
            checked = false;

        prop.setChecked(checked);
        request.setAttribute("cppSingleLineProperty", prop);
        request.setAttribute("providertitle", "provider.setCppSingleLine.title"); //=Select if you want to use Rx3
        request.setAttribute("providermsgPrefs", "provider.setCppSingleLine.msgPrefs"); //=Preferences
        request.setAttribute("providermsgProvider", "provider.setCppSingleLine.msgProfileView"); //=Use Rx3
        request.setAttribute("providermsgEdit", "provider.setCppSingleLine.msgEdit"); //=Do you want to use Rx3?
        request.setAttribute("providerbtnSubmit", "provider.setCppSingleLine.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.setCppSingleLine.msgSuccess"); //=Rx3 Selection saved
        request.setAttribute("method", "saveUseCppSingleLine");

        this.setCppSingleLineProperty(prop);

        return "genCppSingleLine";
    }


    public String saveUseCppSingleLine() {
        UserProperty UUseRx3 = this.getCppSingleLineProperty();

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        boolean checked = false;
        if (UUseRx3 != null)
            checked = UUseRx3.isChecked();
        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.CPP_SINGLE_LINE);
        if (prop == null) {
            prop = new UserProperty();
            prop.setName(UserProperty.CPP_SINGLE_LINE);
            prop.setProviderNo(providerNo);
        }
        String useRx3 = "no";
        if (checked)
            useRx3 = "yes";

        prop.setValue(useRx3);
        this.userPropertyDAO.saveProp(prop);

        request.setAttribute("status", "success");
        request.setAttribute("cppSingleLineProperty", prop);
        request.setAttribute("providertitle", "provider.setCppSingleLine.title"); //=Select if you want to use Rx3
        request.setAttribute("providermsgPrefs", "provider.setCppSingleLine.msgPrefs"); //=Preferences
        request.setAttribute("providermsgProvider", "provider.setCppSingleLine.msgProfileView"); //=Use Rx3
        request.setAttribute("providermsgEdit", "provider.setCppSingleLine.msgEdit"); //=Check if you want to use Rx3
        request.setAttribute("providerbtnSubmit", "provider.setCppSingleLine.btnSubmit"); //=Save
        if (checked)
            request.setAttribute("providermsgSuccess", "provider.setCppSingleLine.msgSuccess_selected"); //=Rx3 is selected
        else
            request.setAttribute("providermsgSuccess", "provider.setCppSingleLine.msgSuccess_unselected"); //=Rx3 is unselected
        request.setAttribute("method", "saveUseCppSingleLine");

        return "genCppSingleLine";
    }

    public String viewEDocBrowserInDocumentReport() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();
        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.EDOC_BROWSER_IN_DOCUMENT_REPORT);

        String propValue = "";
        if (prop == null) {
            prop = new UserProperty();
        } else {
            propValue = prop.getValue();
        }

        boolean checked;
        if (propValue.equalsIgnoreCase("yes"))
            checked = true;
        else
            checked = false;

        prop.setChecked(checked);
        request.setAttribute("eDocBrowserInDocumentReportProperty", prop);
        request.setAttribute("providertitle", "provider.setEDocBrowserInDocumentReport.title");
        request.setAttribute("providermsgPrefs", "provider.setEDocBrowserInDocumentReport.msgPrefs");
        request.setAttribute("providermsgProvider", "provider.setEDocBrowserInDocumentReport.msgProfileView");
        request.setAttribute("providermsgEdit", "provider.setEDocBrowserInDocumentReport.msgEdit");
        request.setAttribute("providerbtnSubmit", "provider.setEDocBrowserInDocumentReport.btnSubmit");
        request.setAttribute("providermsgSuccess", "provider.setEDocBrowserInDocumentReport.msgSuccess");
        request.setAttribute("method", "saveEDocBrowserInDocumentReport");

        this.seteDocBrowserInDocumentReportProperty(prop);

        return "genEDocBrowserInDocumentReport";
    }


    public String saveEDocBrowserInDocumentReport() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty Uprop = this.geteDocBrowserInDocumentReportProperty();

        boolean checked = false;
        if (Uprop != null)
            checked = Uprop.isChecked();
        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.EDOC_BROWSER_IN_DOCUMENT_REPORT);
        if (prop == null) {
            prop = new UserProperty();
            prop.setName(UserProperty.EDOC_BROWSER_IN_DOCUMENT_REPORT);
            prop.setProviderNo(providerNo);
        }
        String propValue = "no";
        if (checked)
            propValue = "yes";

        prop.setValue(propValue);
        this.userPropertyDAO.saveProp(prop);

        request.setAttribute("status", "success");
        request.setAttribute("eDocBrowserInDocumentReportProperty", prop);
        request.setAttribute("providertitle", "provider.setEDocBrowserInDocumentReport.title");
        request.setAttribute("providermsgPrefs", "provider.setEDocBrowserInDocumentReport.msgPrefs");
        request.setAttribute("providermsgProvider", "provider.setEDocBrowserInDocumentReport.msgProfileView");
        request.setAttribute("providermsgEdit", "provider.setEDocBrowserInDocumentReport.msgEdit");
        request.setAttribute("providerbtnSubmit", "provider.setEDocBrowserInDocumentReport.btnSubmit");
        if (checked)
            request.setAttribute("providermsgSuccess", "provider.setEDocBrowserInDocumentReport.msgSuccess_selected");
        else
            request.setAttribute("providermsgSuccess", "provider.setEDocBrowserInDocumentReport.msgSuccess_unselected");
        request.setAttribute("method", "saveEDocBrowserInDocumentReport");

        return "genEDocBrowserInDocumentReport";
    }

    public String viewEDocBrowserInMasterFile() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.EDOC_BROWSER_IN_MASTER_FILE);

        String propValue = "";
        if (prop == null) {
            prop = new UserProperty();
        } else {
            propValue = prop.getValue();
        }

        boolean checked;
        if (propValue.equalsIgnoreCase("yes"))
            checked = true;
        else
            checked = false;

        prop.setChecked(checked);
        request.setAttribute("eDocBrowserInMasterFileProperty", prop);
        request.setAttribute("providertitle", "provider.setEDocBrowserInMasterFile.title"); //=Select if you want to use Rx3
        request.setAttribute("providermsgPrefs", "provider.setEDocBrowserInMasterFile.msgPrefs"); //=Preferences
        request.setAttribute("providermsgProvider", "provider.setEDocBrowserInMasterFile.msgProfileView"); //=Use Rx3
        request.setAttribute("providermsgEdit", "provider.setEDocBrowserInMasterFile.msgEdit"); //=Do you want to use Rx3?
        request.setAttribute("providerbtnSubmit", "provider.setEDocBrowserInMasterFile.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.setEDocBrowserInMasterFile.msgSuccess"); //=Rx3 Selection saved
        request.setAttribute("method", "saveEDocBrowserInMasterFile");

        this.seteDocBrowserInMasterFileProperty(prop);

        return "genEDocBrowserInMasterFile";
    }


    public String saveEDocBrowserInMasterFile() {
        UserProperty Uprop = this.geteDocBrowserInMasterFileProperty();

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        boolean checked = false;
        if (Uprop != null)
            checked = Uprop.isChecked();
        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.EDOC_BROWSER_IN_MASTER_FILE);
        if (prop == null) {
            prop = new UserProperty();
            prop.setName(UserProperty.EDOC_BROWSER_IN_MASTER_FILE);
            prop.setProviderNo(providerNo);
        }
        String propValue = "no";
        if (checked)
            propValue = "yes";

        prop.setValue(propValue);
        this.userPropertyDAO.saveProp(prop);

        request.setAttribute("status", "success");
        request.setAttribute("eDocBrowserInMasterFileProperty", prop);
        request.setAttribute("providertitle", "provider.setEDocBrowserInMasterFile.title");
        request.setAttribute("providermsgPrefs", "provider.setEDocBrowserInMasterFile.msgPrefs");
        request.setAttribute("providermsgProvider", "provider.setEDocBrowserInMasterFile.msgProfileView");
        request.setAttribute("providermsgEdit", "provider.setEDocBrowserInMasterFile.msgEdit");
        request.setAttribute("providerbtnSubmit", "provider.setEDocBrowserInMasterFile.btnSubmit");
        if (checked)
            request.setAttribute("providermsgSuccess", "provider.setEDocBrowserInMasterFile.msgSuccess_selected");
        else
            request.setAttribute("providermsgSuccess", "provider.setEDocBrowserInMasterFile.msgSuccess_unselected");
        request.setAttribute("method", "saveEDocBrowserInMasterFile");

        return "genEDocBrowserInMasterFile";
    }

    public String viewCommentLab() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.LAB_ACK_COMMENT);

        String propValue = "";
        if (prop == null) {
            prop = new UserProperty();
        } else {
            propValue = prop.getValue();
        }

        boolean checked;
        if (propValue.equalsIgnoreCase("yes"))
            checked = true;
        else
            checked = false;

        prop.setChecked(checked);
        request.setAttribute("labAckComment", prop);
        request.setAttribute("providertitle", "provider.setAckComment.title");
        request.setAttribute("providermsgPrefs", "provider.setAckComment.msgPrefs");
        request.setAttribute("providermsgProvider", "provider.setAckComment.msgProfileView");
        request.setAttribute("providermsgEdit", "provider.setAckComment.msgEdit");
        request.setAttribute("providerbtnSubmit", "provider.setAckComment.btnSubmit");
        request.setAttribute("providermsgSuccess", "provider.setAckComment.msgSuccess");
        request.setAttribute("method", "saveCommentLab");

        this.setLabAckCommentProperty(prop);

        return "genAckCommentLab";
    }

    public String saveCommentLab() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty Uprop = this.getLabAckCommentProperty();

        boolean checked = false;
        if (Uprop != null)
            checked = Uprop.isChecked();
        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.LAB_ACK_COMMENT);
        if (prop == null) {
            prop = new UserProperty();
            prop.setName(UserProperty.LAB_ACK_COMMENT);
            prop.setProviderNo(providerNo);
        }
        String disableComment = "no";
        if (checked)
            disableComment = "yes";

        prop.setValue(disableComment);
        this.userPropertyDAO.saveProp(prop);

        request.setAttribute("status", "success");
        request.setAttribute("labAckComment", prop);
        request.setAttribute("providertitle", "provider.setAckComment.title");
        request.setAttribute("providermsgPrefs", "provider.setAckComment.msgPrefs");
        request.setAttribute("providermsgProvider", "provider.setAckComment.msgProfileView");
        request.setAttribute("providermsgEdit", "provider.setAckComment.msgEdit");
        request.setAttribute("providerbtnSubmit", "provider.setAckComment.btnSubmit");
        request.setAttribute("providermsgSuccess", "provider.setAckComment.msgSuccess");
        request.setAttribute("method", "saveCommentLab");

        if (checked)
            request.setAttribute("providermsgSuccess", "provider.setAckComment.msgSuccess_selected");
        else
            request.setAttribute("providermsgSuccess", "provider.setAckComment.msgSuccess_unselected");


        return "genAckCommentLab";
    }

    @SuppressWarnings("unchecked")
    public String viewLabRecall() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty delegate = this.userPropertyDAO.getProp(providerNo, UserProperty.LAB_RECALL_DELEGATE);
        UserProperty subject = this.userPropertyDAO.getProp(providerNo, UserProperty.LAB_RECALL_MSG_SUBJECT);
        UserProperty ticklerAssignee = this.userPropertyDAO.getProp(providerNo, UserProperty.LAB_RECALL_TICKLER_ASSIGNEE);
        UserProperty priority = this.userPropertyDAO.getProp(providerNo, UserProperty.LAB_RECALL_TICKLER_PRIORITY);

        if (delegate == null) {
            delegate = new UserProperty();
        }

        if (subject == null) {
            subject = new UserProperty();
        }

        String defaultToDelegate = "";
        if (ticklerAssignee == null) {
            ticklerAssignee = new UserProperty();
        } else {
            defaultToDelegate = ticklerAssignee.getValue();
        }

        boolean checked;
        if (defaultToDelegate.equalsIgnoreCase("yes")) {
            checked = true;
        } else {
            checked = false;
        }

        if (priority == null) {
            priority = new UserProperty();
        }

        ArrayList<LabelValueBean> providerList = new ArrayList<LabelValueBean>();
        providerList.add(new LabelValueBean("Select", "")); //key , value

        ProviderDao dao = SpringUtils.getBean(ProviderDao.class);
        List<Provider> ps = dao.getProviders();
        Collections.sort(ps, new BeanComparator("lastName"));
        try {

            for (Provider p : ps) {
                if (!p.getProviderNo().equals("-1")) {
                    providerList.add(new LabelValueBean(p.getLastName() + ", " + p.getFirstName(), p.getProviderNo()));
                }
            }


        } catch (Exception e) {
            MiscUtils.getLogger().error("Error", e);
        }
        request.setAttribute("providerSelect", providerList);

        ArrayList<LabelValueBean> priorityList = new ArrayList<LabelValueBean>();
        priorityList.add(new LabelValueBean("Select", "")); //key , value
        priorityList.add(new LabelValueBean("High", "High"));
        priorityList.add(new LabelValueBean("Normal", "Normal"));
        priorityList.add(new LabelValueBean("Low", "Low"));

        request.setAttribute("prioritySelect", priorityList);

        request.setAttribute("labRecallDelegate", delegate);
        request.setAttribute("labRecallMsgSubject", subject);

        ticklerAssignee.setChecked(checked);
        request.setAttribute("labRecallTicklerAssignee", ticklerAssignee);

        request.setAttribute("labRecallTicklerPriority", priority);

        request.setAttribute("providertitle", "provider.setLabRecall.title");
        request.setAttribute("providermsgPrefs", "provider.setLabRecall.msgPrefs");
        request.setAttribute("providermsgProvider", "provider.setLabRecall.msgProfileView");
        request.setAttribute("providermsgEdit", "provider.setLabRecall.msgEdit");
        request.setAttribute("providerbtnSubmit", "provider.setLabRecall.btnSubmit");
        request.setAttribute("providermsgSuccess", "provider.setLabRecall.msgSuccess");
        request.setAttribute("method", "saveLabRecallPrefs");

        this.setLabRecallDelegate(delegate);
        this.setLabRecallMsgSubject(subject);
        this.setLabRecallTicklerAssignee(ticklerAssignee);
        this.setLabRecallTicklerPriority(priority);

        return "genLabRecallPrefs";
    }

    public String saveLabRecallPrefs() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty d = this.getLabRecallDelegate();
        UserProperty s = this.getLabRecallMsgSubject();
        UserProperty a = this.getLabRecallTicklerAssignee();
        UserProperty p = this.getLabRecallTicklerPriority();

        String delegate = d != null ? d.getValue() : "";
        String subject = s != null ? s.getValue() : "";

        boolean checked = a != null ? a.isChecked() : false;

        String priority = p != null ? p.getValue() : "";

        boolean delete = false;
        if (delegate.equals("")) {
            delete = true;
        }

        UserProperty dProperty = this.userPropertyDAO.getProp(providerNo, UserProperty.LAB_RECALL_DELEGATE);
        if (dProperty == null) {
            dProperty = new UserProperty();
            dProperty.setProviderNo(providerNo);
            dProperty.setName(UserProperty.LAB_RECALL_DELEGATE);
        }

        if (delete) {
            userPropertyDAO.delete(dProperty);
        } else {
            dProperty.setValue(delegate);
            userPropertyDAO.saveProp(dProperty);
        }

        UserProperty sProperty = this.userPropertyDAO.getProp(providerNo, UserProperty.LAB_RECALL_MSG_SUBJECT);
        if (sProperty == null) {
            sProperty = new UserProperty();
            sProperty.setProviderNo(providerNo);
            sProperty.setName(UserProperty.LAB_RECALL_MSG_SUBJECT);
        }
        if (delete) {
            userPropertyDAO.delete(sProperty);
        } else {
            sProperty.setValue(subject);
            userPropertyDAO.saveProp(sProperty);
        }

        String defaultToDelegate = "no";
        UserProperty aProperty = this.userPropertyDAO.getProp(providerNo, UserProperty.LAB_RECALL_TICKLER_ASSIGNEE);
        if (aProperty == null) {
            aProperty = new UserProperty();
            aProperty.setProviderNo(providerNo);
            aProperty.setName(UserProperty.LAB_RECALL_TICKLER_ASSIGNEE);
        }
        if (delete) {
            userPropertyDAO.delete(aProperty);
        } else {
            if (checked) {
                defaultToDelegate = "yes";
            }

            aProperty.setValue(defaultToDelegate);
            userPropertyDAO.saveProp(aProperty);
        }

        UserProperty pProperty = this.userPropertyDAO.getProp(providerNo, UserProperty.LAB_RECALL_TICKLER_PRIORITY);
        if (pProperty == null) {
            pProperty = new UserProperty();
            pProperty.setProviderNo(providerNo);
            pProperty.setName(UserProperty.LAB_RECALL_TICKLER_PRIORITY);
        }
        if (delete) {
            userPropertyDAO.delete(pProperty);
        } else {
            pProperty.setValue(priority);
            userPropertyDAO.saveProp(pProperty);
        }

        request.setAttribute("status", "success");
        request.setAttribute("providertitle", "provider.setLabRecall.title");
        request.setAttribute("providermsgPrefs", "provider.setLabRecall.msgPrefs");
        request.setAttribute("providermsgProvider", "provider.setLabRecall.msgProfileView");
        request.setAttribute("providermsgEdit", "provider.setLabRecall.msgEdit");
        request.setAttribute("providerbtnSubmit", "provider.setLabRecall.btnSubmit");

        String msgSuccess = "provider.setLabRecall.msgSuccess";
        if (delete) {
            msgSuccess = "provider.setLabRecall.msgDeleted";
        }
        request.setAttribute("providermsgSuccess", msgSuccess);

        request.setAttribute("method", "saveLabRecallPrefs");

        return "genLabRecallPrefs";
    }

    public String viewTicklerTaskAssignee() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty ticklerTaskAssignee = this.userPropertyDAO.getProp(providerNo, UserProperty.TICKLER_TASK_ASSIGNEE);

        String defaultTo = "";
        if (ticklerTaskAssignee == null) {
            ticklerTaskAssignee = new UserProperty();
            ticklerTaskAssignee.setValue("default");
            defaultTo = ticklerTaskAssignee.getValue();
        } else if (ticklerTaskAssignee.getValue().equals("mrp")) {
            defaultTo = "mrp";
        } else {
            defaultTo = "provider";
        }

        ArrayList<LabelValueBean> providerList = new ArrayList<LabelValueBean>();
        providerList.add(new LabelValueBean("Select", ""));

        ProviderDao dao = SpringUtils.getBean(ProviderDao.class);
        List<Provider> ps = dao.getProviders();
        Collections.sort(ps, new BeanComparator("lastName"));
        try {

            for (Provider p : ps) {
                if (!p.getProviderNo().equals("-1")) {
                    providerList.add(new LabelValueBean(p.getLastName() + ", " + p.getFirstName(), p.getProviderNo()));
                }
            }

        } catch (Exception e) {
            MiscUtils.getLogger().error("Error", e);
        }
        request.setAttribute("providerSelect", providerList);

        request.setAttribute("providertitle", "provider.ticklerPreference.title");
        request.setAttribute("providermsgPrefs", "provider.ticklerPreference.msgPrefs"); //=Preferences
        request.setAttribute("providerbtnSubmit", "provider.ticklerPreference.btnSubmit"); //=Save
        request.setAttribute("providerbtnCancel", "provider.ticklerPreference.btnCancel"); //=Cancel
        request.setAttribute("method", "saveTicklerTaskAssignee");

        request.setAttribute("taskAssigneeSelection", ticklerTaskAssignee);
        this.setTaskAssigneeSelection(ticklerTaskAssignee);

        request.setAttribute("providerMsg", "");

        request.setAttribute("taskAssigneeMRP", defaultTo);

        UserProperty t = this.getTaskAssigneeMRP();
        t.setValue(defaultTo);

        return SUCCESS;
    }


    public String saveTicklerTaskAssignee() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty a = this.getTaskAssigneeSelection();
        String tickerTaskAssignee = a != null ? a.getValue() : "";

        boolean delete = false;
        if (tickerTaskAssignee.equals("")) {
            delete = true;
        }

        UserProperty property = this.userPropertyDAO.getProp(providerNo, UserProperty.TICKLER_TASK_ASSIGNEE);
        if (property == null) {
            property = new UserProperty();
            property.setProviderNo(providerNo);
            property.setName(UserProperty.TICKLER_TASK_ASSIGNEE);
        }

        try {
            if (delete) {
                if (property.getId() != null) {
                    userPropertyDAO.delete(property);
                }
            } else {
                property.setValue(tickerTaskAssignee);
                userPropertyDAO.saveProp(property);
            }
        } catch (Exception e) {
            // Return to the success page even though the pereference is not changed from default
            // Avoid the error displays
            request.setAttribute("status", "success");
            return "complete";
        }


        request.setAttribute("status", "success");

        request.setAttribute("providertitle", "provider.ticklerPreference.title");
        request.setAttribute("providermsgPrefs", "provider.ticklerPreference.msgPrefs"); //=Preferences
        request.setAttribute("providerbtnSubmit", "provider.ticklerPreference.btnSubmit"); //=Save
        request.setAttribute("providerbtnCancel", "provider.ticklerPreference.btnCancel"); //=Cancel

        request.setAttribute("providerbtnClose", "provider.ticklerPreference.providerbtnClose"); //=Close Window

        request.setAttribute("providerMsg", "provider.ticklerPreference.savedMsg");

        request.setAttribute("method", "saveTicklerTaskAssignee");

        return "complete";

    }

    public String viewEncounterWindowSize() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty width = this.userPropertyDAO.getProp(providerNo, "encounterWindowWidth");
        UserProperty height = this.userPropertyDAO.getProp(providerNo, "encounterWindowHeight");
        UserProperty maximize = this.userPropertyDAO.getProp(providerNo, "encounterWindowMaximize");

        if (width == null) {
            width = new UserProperty();
        }
        if (height == null) {
            height = new UserProperty();
        }
        if (maximize == null) {
            maximize = new UserProperty();
        }
        if (maximize.getValue() != null) {
            maximize.setChecked(maximize.getValue().equals("yes") ? true : false);
        }

        request.setAttribute("width", width);
        request.setAttribute("height", height);
        request.setAttribute("maximize", maximize);


        request.setAttribute("providertitle", "provider.encounterWindowSize.title"); //=Set myDrugref ID
        request.setAttribute("providermsgPrefs", "provider.encounterWindowSize.msgPrefs"); //=Preferences"); //
        request.setAttribute("providermsgProvider", "provider.encounterWindowSize.msgProvider"); //=myDrugref ID
        request.setAttribute("providermsgEdit", "provider.encounterWindowSize.msgEdit"); //=Enter your desired login for myDrugref
        request.setAttribute("providerbtnSubmit", "provider.encounterWindowSize.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.encounterWindowSize.msgSuccess"); //=myDrugref Id saved
        request.setAttribute("method", "saveEncounterWindowSize");

        this.setEncounterWindowWidth(width);
        this.setEncounterWindowHeight(height);
        this.setEncounterWindowMaximize(maximize);

        return "genEncounterWindowSize";
    }

    public String saveEncounterWindowSize() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty w = this.getEncounterWindowWidth();
        UserProperty h = this.getEncounterWindowHeight();
        UserProperty m = this.getEncounterWindowMaximize();

        String width = w != null ? w.getValue() : "";
        String height = h != null ? h.getValue() : "";
        boolean maximize = m != null ? m.isChecked() : false;

        UserProperty wProperty = this.userPropertyDAO.getProp(providerNo, "encounterWindowWidth");
        if (wProperty == null) {
            wProperty = new UserProperty();
            wProperty.setProviderNo(providerNo);
            wProperty.setName("encounterWindowWidth");
        }
        wProperty.setValue(width);
        userPropertyDAO.saveProp(wProperty);

        UserProperty hProperty = this.userPropertyDAO.getProp(providerNo, "encounterWindowHeight");
        if (hProperty == null) {
            hProperty = new UserProperty();
            hProperty.setProviderNo(providerNo);
            hProperty.setName("encounterWindowHeight");
        }
        hProperty.setValue(height);
        userPropertyDAO.saveProp(hProperty);

        UserProperty mProperty = this.userPropertyDAO.getProp(providerNo, "encounterWindowMaximize");
        if (mProperty == null) {
            mProperty = new UserProperty();
            mProperty.setProviderNo(providerNo);
            mProperty.setName("encounterWindowMaximize");
        }
        mProperty.setValue(maximize ? "yes" : "no");
        userPropertyDAO.saveProp(mProperty);

        request.setAttribute("status", "success");
        request.setAttribute("providertitle", "provider.encounterWindowSize.title"); //=Set myDrugref ID
        request.setAttribute("providermsgPrefs", "provider.encounterWindowSize.msgPrefs"); //=Preferences"); //
        request.setAttribute("providermsgProvider", "provider.encounterWindowSize.msgProvider"); //=myDrugref ID
        request.setAttribute("providermsgEdit", "provider.encounterWindowSize.msgEdit"); //=Enter your desired login for myDrugref
        request.setAttribute("providerbtnSubmit", "provider.encounterWindowSize.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.encounterWindowSize.msgSuccess"); //=myDrugref Id saved
        request.setAttribute("method", "saveEncounterWindowSize");

        return "genEncounterWindowSize";
    }

    public String viewQuickChartSize() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty size = this.userPropertyDAO.getProp(providerNo, "quickChartSize");

        if (size == null) {
            size = new UserProperty();
        }


        request.setAttribute("size", size);


        request.setAttribute("providertitle", "provider.quickChartSize.title"); //=Set myDrugref ID
        request.setAttribute("providermsgPrefs", "provider.quickChartSize.msgPrefs"); //=Preferences"); //
        request.setAttribute("providermsgProvider", "provider.quickChartSize.msgProvider"); //=myDrugref ID
        request.setAttribute("providermsgEdit", "provider.quickChartSize.msgEdit"); //=Enter your desired login for myDrugref
        request.setAttribute("providerbtnSubmit", "provider.quickChartSize.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.quickChartSize.msgSuccess"); //=myDrugref Id saved
        request.setAttribute("method", "saveQuickChartSize");

        this.setQuickChartSize(size);

        return "genQuickChartSize";
    }

    public String saveQuickChartSize() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty s = this.getQuickChartSize();

        String size = s != null ? s.getValue() : "";

        UserProperty wProperty = this.userPropertyDAO.getProp(providerNo, "quickChartSize");
        if (wProperty == null) {
            wProperty = new UserProperty();
            wProperty.setProviderNo(providerNo);
            wProperty.setName("quickChartsize");
        }
        wProperty.setValue(size);
        userPropertyDAO.saveProp(wProperty);


        request.setAttribute("status", "success");
        request.setAttribute("providertitle", "provider.quickChartSize.title"); //=Set myDrugref ID
        request.setAttribute("providermsgPrefs", "provider.quickChartSize.msgPrefs"); //=Preferences"); //
        request.setAttribute("providermsgProvider", "provider.quickChartSize.msgProvider"); //=myDrugref ID
        request.setAttribute("providermsgEdit", "provider.quickChartSize.msgEdit"); //=Enter your desired login for myDrugref
        request.setAttribute("providerbtnSubmit", "provider.quickChartSize.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.quickChartSize.msgSuccess"); //=myDrugref Id saved
        request.setAttribute("method", "saveQuickChartSize");

        return "genQuickChartSize";
    }

    public String viewIntegratorProperties() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        Facility facility = loggedInInfo.getCurrentFacility();
        UserProperty[] integratorProperties = new UserProperty[21];

        integratorProperties[0] = this.userPropertyDAO.getProp(UserProperty.INTEGRATOR_DEMOGRAPHIC_SYNC);
        integratorProperties[1] = this.userPropertyDAO.getProp(UserProperty.INTEGRATOR_DEMOGRAPHIC_ADMISSIONS);
        integratorProperties[2] = this.userPropertyDAO.getProp(UserProperty.INTEGRATOR_DEMOGRAPHIC_ALLERGIES);
        integratorProperties[3] = this.userPropertyDAO.getProp(UserProperty.INTEGRATOR_DEMOGRAPHIC_APPOINTMENTS);
        integratorProperties[4] = this.userPropertyDAO.getProp(UserProperty.INTEGRATOR_DEMOGRAPHIC_BILLING);
        integratorProperties[5] = this.userPropertyDAO.getProp(UserProperty.INTEGRATOR_DEMOGRAPHIC_CONSENT);
        integratorProperties[6] = this.userPropertyDAO.getProp(UserProperty.INTEGRATOR_DEMOGRAPHIC_DOCUMENTS);
        integratorProperties[7] = this.userPropertyDAO.getProp(UserProperty.INTEGRATOR_DEMOGRAPHIC_DRUGS);
        integratorProperties[8] = this.userPropertyDAO.getProp(UserProperty.INTEGRATOR_DEMOGRAPHIC_DXRESEARCH);
        integratorProperties[9] = this.userPropertyDAO.getProp(UserProperty.INTEGRATOR_DEMOGRAPHIC_EFORMS);
        integratorProperties[10] = this.userPropertyDAO.getProp(UserProperty.INTEGRATOR_DEMOGRAPHIC_ISSUES);
        integratorProperties[11] = this.userPropertyDAO.getProp(UserProperty.INTEGRATOR_DEMOGRAPHIC_LABREQ);
        integratorProperties[12] = this.userPropertyDAO.getProp(UserProperty.INTEGRATOR_DEMOGRAPHIC_MEASUREMENTS);
        integratorProperties[13] = this.userPropertyDAO.getProp(UserProperty.INTEGRATOR_DEMOGRAPHIC_NOTES);
        integratorProperties[14] = this.userPropertyDAO.getProp(UserProperty.INTEGRATOR_DEMOGRAPHIC_PREVENTIONS);
        integratorProperties[15] = this.userPropertyDAO.getProp(UserProperty.INTEGRATOR_FACILITY);
        integratorProperties[16] = this.userPropertyDAO.getProp(UserProperty.INTEGRATOR_PROGRAMS);
        integratorProperties[17] = this.userPropertyDAO.getProp(UserProperty.INTEGRATOR_PROVIDERS);
        integratorProperties[18] = this.userPropertyDAO.getProp(UserProperty.INTEGRATOR_FULL_PUSH + facility.getId());
        integratorProperties[19] = this.userPropertyDAO.getProp(UserProperty.INTEGRATOR_LAST_PUSH);
        integratorProperties[20] = this.userPropertyDAO.getProp(UserProperty.INTEGRATOR_PATIENT_CONSENT);

        request.setAttribute("integratorProperties", integratorProperties);
        return "genIntegrator";
    }

    public String saveIntegratorProperties() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        Facility facility = loggedInInfo.getCurrentFacility();
        this.userPropertyDAO.saveProp(UserProperty.INTEGRATOR_DEMOGRAPHIC_ADMISSIONS, request.getParameter("integrator_demographic_admissions"));
        this.userPropertyDAO.saveProp(UserProperty.INTEGRATOR_DEMOGRAPHIC_ALLERGIES, request.getParameter("integrator_demographic_allergies"));
        this.userPropertyDAO.saveProp(UserProperty.INTEGRATOR_DEMOGRAPHIC_APPOINTMENTS, request.getParameter("integrator_demographic_appointments"));
        this.userPropertyDAO.saveProp(UserProperty.INTEGRATOR_DEMOGRAPHIC_BILLING, request.getParameter("integrator_demographic_billing"));
        this.userPropertyDAO.saveProp(UserProperty.INTEGRATOR_DEMOGRAPHIC_CONSENT, request.getParameter("integrator_demographic_consent"));
        this.userPropertyDAO.saveProp(UserProperty.INTEGRATOR_DEMOGRAPHIC_DOCUMENTS, request.getParameter("integrator_demographic_documents"));
        this.userPropertyDAO.saveProp(UserProperty.INTEGRATOR_DEMOGRAPHIC_DRUGS, request.getParameter("integrator_demographic_drugs"));
        this.userPropertyDAO.saveProp(UserProperty.INTEGRATOR_DEMOGRAPHIC_DXRESEARCH, request.getParameter("integrator_demographic_dxresearch"));
        this.userPropertyDAO.saveProp(UserProperty.INTEGRATOR_DEMOGRAPHIC_EFORMS, request.getParameter("integrator_demographic_eforms"));
        this.userPropertyDAO.saveProp(UserProperty.INTEGRATOR_DEMOGRAPHIC_ISSUES, request.getParameter("integrator_demographic_issues"));
        this.userPropertyDAO.saveProp(UserProperty.INTEGRATOR_DEMOGRAPHIC_LABREQ, request.getParameter("integrator_demographic_labreq"));
        this.userPropertyDAO.saveProp(UserProperty.INTEGRATOR_DEMOGRAPHIC_MEASUREMENTS, request.getParameter("integrator_demographic_measurements"));
        this.userPropertyDAO.saveProp(UserProperty.INTEGRATOR_DEMOGRAPHIC_NOTES, request.getParameter("integrator_demographic_notes"));
        this.userPropertyDAO.saveProp(UserProperty.INTEGRATOR_DEMOGRAPHIC_PREVENTIONS, request.getParameter("integrator_demographic_preventions"));
        this.userPropertyDAO.saveProp(UserProperty.INTEGRATOR_DEMOGRAPHIC_SYNC, request.getParameter("integrator_demographic_sync"));
        this.userPropertyDAO.saveProp(UserProperty.INTEGRATOR_FACILITY, request.getParameter("integrator_facility"));
        this.userPropertyDAO.saveProp(UserProperty.INTEGRATOR_PROGRAMS, request.getParameter("integrator_programs"));
        this.userPropertyDAO.saveProp(UserProperty.INTEGRATOR_PROVIDERS, request.getParameter("integrator_providers"));

        this.userPropertyDAO.saveProp(UserProperty.INTEGRATOR_PATIENT_CONSENT,
                (request.getParameter(UserProperty.INTEGRATOR_PATIENT_CONSENT) != null) ? request.getParameter(UserProperty.INTEGRATOR_PATIENT_CONSENT) : "0");

        this.userPropertyDAO.saveProp(UserProperty.INTEGRATOR_FULL_PUSH + facility.getId(), request.getParameter("integrator_full_push"));

        request.setAttribute("saved", true);

        return viewIntegratorProperties();
    }

    public String viewPatientNameLength() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty length = this.userPropertyDAO.getProp(providerNo, UserProperty.PATIENT_NAME_LENGTH);

        if (length == null) {
            length = new UserProperty();
        }


        request.setAttribute("patientnameLength", length);


        request.setAttribute("providertitle", "provider.patientNameLength.title");
        request.setAttribute("providermsgPrefs", "provider.patientNameLength.msgPrefs");
        request.setAttribute("providermsgProvider", "provider.patientNameLength.msgProvider");
        request.setAttribute("providermsgEdit", "provider.patientNameLength.msgEdit");
        request.setAttribute("providerbtnSubmit", "provider.patientNameLength.btnSubmit");
        request.setAttribute("providermsgSuccess", "provider.patientNameLength.msgSuccess");
        request.setAttribute("method", "savePatientNameLength");

        this.setPatientNameLength(length);

        return "genPatientNameLength";
    }

    public String savePatientNameLength() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();
        UserProperty s = this.getPatientNameLength();

        String length = s != null ? s.getValue() : "";

        UserProperty wProperty = this.userPropertyDAO.getProp(providerNo, UserProperty.PATIENT_NAME_LENGTH);
        if (wProperty == null) {
            wProperty = new UserProperty();
            wProperty.setProviderNo(providerNo);
            wProperty.setName(UserProperty.PATIENT_NAME_LENGTH);
        }
        wProperty.setValue(length);
        userPropertyDAO.saveProp(wProperty);


        request.setAttribute("status", "success");
        request.setAttribute("providertitle", "provider.patientNameLength.title"); //=Set myDrugref ID
        request.setAttribute("providermsgPrefs", "provider.patientNameLength.msgPrefs"); //=Preferences"); //
        request.setAttribute("providermsgProvider", "provider.patientNameLength.msgProvider"); //=myDrugref ID
        request.setAttribute("providermsgEdit", "provider.patientNameLength.msgEdit"); //=Enter your desired login for myDrugref
        request.setAttribute("providerbtnSubmit", "provider.patientNameLength.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.patientNameLength.msgSuccess"); //=myDrugref Id saved
        request.setAttribute("method", "savePatientNameLength");

        return "genPatientNameLength";
    }

    public String viewDisplayDocumentAs() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();
        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.DISPLAY_DOCUMENT_AS);

        if (prop == null) {
            prop = new UserProperty();
        }

        ArrayList<LabelValueBean> serviceList = new ArrayList<LabelValueBean>();
        serviceList.add(new LabelValueBean(UserProperty.PDF, UserProperty.PDF));
        serviceList.add(new LabelValueBean(UserProperty.IMAGE, UserProperty.IMAGE));

        request.setAttribute("dropOpts", serviceList);

        request.setAttribute("displayDocumentAsProperty", prop);

        request.setAttribute("providertitle", "provider.displayDocumentAs.title");
        request.setAttribute("providermsgPrefs", "provider.displayDocumentAs.msgPrefs");
        request.setAttribute("providermsgProvider", "provider.displayDocumentAs.msgProvider");
        request.setAttribute("providermsgEdit", "provider.displayDocumentAs.msgEdit");
        request.setAttribute("providerbtnSubmit", "provider.displayDocumentAs.btnSubmit");
        request.setAttribute("providermsgSuccess", "provider.displayDocumentAs.msgSuccess");
        request.setAttribute("method", "saveDisplayDocumentAs");

        this.setDisplayDocumentAsProperty(prop);
        return "genDisplayDocumentAs";
    }

    public String saveDisplayDocumentAs() {

        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty prop = this.getDisplayDocumentAsProperty();
        String fmt = prop != null ? prop.getValue() : "";
        UserProperty saveProperty = this.userPropertyDAO.getProp(providerNo, UserProperty.DISPLAY_DOCUMENT_AS);

        if (saveProperty == null) {
            saveProperty = new UserProperty();
            saveProperty.setProviderNo(providerNo);
            saveProperty.setName(UserProperty.DISPLAY_DOCUMENT_AS);
        }

        saveProperty.setValue(fmt);
        this.userPropertyDAO.saveProp(saveProperty);

        request.setAttribute("status", "success");
        request.setAttribute("providertitle", "provider.displayDocumentAs.title");
        request.setAttribute("providermsgPrefs", "provider.displayDocumentAs.msgPrefs");
        request.setAttribute("providermsgProvider", "provider.displayDocumentAs.msgProvider");
        request.setAttribute("providermsgEdit", "provider.displayDocumentAs.msgEdit");
        request.setAttribute("providerbtnSubmit", "provider.btnSubmit");
        request.setAttribute("providermsgSuccess", "provider.displayDocumentAs.msgSuccess");
        request.setAttribute("method", "saveDisplayDocumentAs");

        return "genDisplayDocumentAs";
    }


    public String viewCobalt() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();
        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.COBALT);

        String propValue = "";
        if (prop == null) {
            prop = new UserProperty();
        } else {
            propValue = prop.getValue();
        }

        boolean checked;
        if (propValue.equalsIgnoreCase("yes"))
            checked = true;
        else
            checked = false;

        prop.setChecked(checked);
        request.setAttribute("cobaltProperty", prop);
        request.setAttribute("providertitle", "provider.cobalt.title"); //=Select if you want to use Rx3
        request.setAttribute("providermsgPrefs", "provider.cobalt.msgPrefs"); //=Preferences
        request.setAttribute("providermsgProvider", "provider.cobalt.msgProfileView"); //=Use Rx3
        request.setAttribute("providermsgEdit", "provider.cobalt.msgEdit"); //=Do you want to use Rx3?
        request.setAttribute("providerbtnSubmit", "provider.cobalt.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.cobalt.msgSuccess"); //=Rx3 Selection saved
        request.setAttribute("method", "saveCobalt");

        this.setCobaltProperty(prop);

        return "genCobalt";
    }


    public String saveCobalt() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();
        UserProperty Uprop = this.getCobaltProperty();

        boolean checked = false;
        if (Uprop != null)
            checked = Uprop.isChecked();
        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.COBALT);
        if (prop == null) {
            prop = new UserProperty();
            prop.setName(UserProperty.COBALT);
            prop.setProviderNo(providerNo);
        }
        String propValue = "no";
        if (checked)
            propValue = "yes";

        prop.setValue(propValue);
        this.userPropertyDAO.saveProp(prop);

        request.setAttribute("status", "success");
        request.setAttribute("cobaltProperty", prop);
        request.setAttribute("providertitle", "provider.cobalt.title");
        request.setAttribute("providermsgPrefs", "provider.cobalt.msgPrefs");
        request.setAttribute("providermsgProvider", "provider.cobalt.msgProfileView");
        request.setAttribute("providermsgEdit", "provider.cobalt.msgEdit");
        request.setAttribute("providerbtnSubmit", "provider.cobalt.btnSubmit");
        if (checked)
            request.setAttribute("providermsgSuccess", "provider.cobalt.msgSuccess_selected");
        else
            request.setAttribute("providermsgSuccess", "provider.cobalt.msgSuccess_unselected");
        request.setAttribute("method", "saveCobalt");

        return "genCobalt";
    }


    public String viewHideOldEchartLinkInAppt() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();
        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.HIDE_OLD_ECHART_LINK_IN_APPT);

        String propValue = "";
        if (prop == null) {
            prop = new UserProperty();
        } else {
            propValue = prop.getValue();
        }

        boolean checked;
        if (propValue.equals("Y"))
            checked = true;
        else
            checked = false;

        prop.setChecked(checked);
        request.setAttribute("hideOldEchartLinkInApptProperty", prop);
        request.setAttribute("providertitle", "provider.hideOldEchartLinkInAppt.title"); //=Hide Old Echart Link in Appointment
        request.setAttribute("providermsgPrefs", "provider.hideOldEchartLinkInAppt.msgPrefs"); //=Preferences
        request.setAttribute("providerbtnSubmit", "provider.hideOldEchartLinkInAppt.btnSubmit"); //=Save
        request.setAttribute("providerbtnCancel", "provider.hideOldEchartLinkInAppt.btnCancel"); //=Cancel
        request.setAttribute("method", "saveHideOldEchartLinkInAppt");

        this.setHideOldEchartLinkInApptProperty(prop);

        return "genHideOldEchartLinkInAppt";
    }


    public String saveHideOldEchartLinkInAppt() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();
        UserProperty Uprop = this.getHideOldEchartLinkInApptProperty();

        boolean checked = false;
        if (Uprop != null)
            checked = Uprop.isChecked();
        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.HIDE_OLD_ECHART_LINK_IN_APPT);
        if (prop == null) {
            prop = new UserProperty();
            prop.setName(UserProperty.HIDE_OLD_ECHART_LINK_IN_APPT);
            prop.setProviderNo(providerNo);
        }
        String propValue = "N";
        if (checked) propValue = "Y";

        prop.setValue(propValue);
        this.userPropertyDAO.saveProp(prop);

        request.setAttribute("status", "success");
        request.setAttribute("hideOldEchartLinkInApptProperty", prop);
        request.setAttribute("providertitle", "provider.hideOldEchartLinkInAppt.title"); //=Hide Old Echart Link in Appointment
        request.setAttribute("providermsgPrefs", "provider.hideOldEchartLinkInAppt.msgPrefs"); //=Preferences
        request.setAttribute("providerbtnClose", "provider.hideOldEchartLinkInAppt.btnClose"); //=Close
        if (checked)
            request.setAttribute("providermsgSuccess", "provider.hideOldEchartLinkInAppt.msgSuccess_selected"); //=Old Echart Link Hidden in Appointment
        else
            request.setAttribute("providermsgSuccess", "provider.hideOldEchartLinkInAppt.msgSuccess_unselected"); //Old Echart Link Shown in Appointment
        request.setAttribute("method", "saveHideOldEchartLinkInAppt");

        return "genHideOldEchartLinkInAppt";
    }


    public String viewDashboardPrefs() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();
        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.DASHBOARD_SHARE);

        String propValue = "";
        if (prop == null) {
            prop = new UserProperty();
        } else {
            propValue = prop.getValue();
        }

        boolean checked;
        if (propValue.equals("true"))
            checked = true;
        else
            checked = false;

        prop.setChecked(checked);
        request.setAttribute("dashboardShareProperty", prop);
        request.setAttribute("providertitle", "provider.dashboardPrefs.title");
        request.setAttribute("providermsgPrefs", "provider.dashboardPrefs.msgPrefs"); //=Preferences
        request.setAttribute("providerbtnSubmit", "provider.dashboardPrefs.btnSubmit"); //=Save
        request.setAttribute("providerbtnCancel", "provider.dashboardPrefs.btnCancel"); //=Cancel
        request.setAttribute("method", "saveDashboardPrefs");

        this.setDashboardShareProperty(prop);

        return "genDashboardPrefs";
    }

    public String saveDashboardPrefs() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();
        UserProperty Uprop = this.getDashboardShareProperty();

        boolean checked = false;
        if (Uprop != null)
            checked = Uprop.isChecked();
        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.DASHBOARD_SHARE);
        if (prop == null) {
            prop = new UserProperty();
            prop.setName(UserProperty.DASHBOARD_SHARE);
            prop.setProviderNo(providerNo);
        }
        String propValue = "false";
        if (checked) propValue = "true";

        prop.setValue(propValue);
        this.userPropertyDAO.saveProp(prop);

        request.setAttribute("status", "success");
        request.setAttribute("dashboardShareProperty", prop);
        request.setAttribute("providertitle", "provider.dashboardPrefs.title");
        request.setAttribute("providermsgPrefs", "provider.dashboardPrefs.msgPrefs"); //=Preferences
        request.setAttribute("providerbtnClose", "provider.dashboardPrefs.btnClose"); //=Close
        if (checked)
            request.setAttribute("providermsgSuccess", "provider.dashboardPrefs.msgSuccess_selected");
        else
            request.setAttribute("providermsgSuccess", "provider.dashboardPrefs.msgSuccess_unselected");
        request.setAttribute("method", "saveDashboardPrefs");

        return "genDashboardPrefs";
    }

    public String viewAppointmentCardPrefs() {

        String provider = (String) request.getSession().getAttribute("user");

        UserProperty name = this.userPropertyDAO.getProp(provider, "APPT_CARD_NAME");
        UserProperty phone = this.userPropertyDAO.getProp(provider, "APPT_CARD_PHONE");
        UserProperty fax = this.userPropertyDAO.getProp(provider, "APPT_CARD_FAX");

        if (name == null) {
            name = new UserProperty();
        }
        if (phone == null) {
            phone = new UserProperty();
        }
        if (fax == null) {
            fax = new UserProperty();
        }

        request.setAttribute("appointmentCardName", name);
        request.setAttribute("appointmentCardPhone", phone);
        request.setAttribute("appointmentCardFax", fax);


        request.setAttribute("providertitle", "provider.appointmentCardPrefs.title"); //=Set myDrugref ID
        request.setAttribute("providermsgPrefs", "provider.appointmentCardPrefs.msgPrefs"); //=Preferences"); //
        request.setAttribute("providermsgProvider", "provider.appointmentCardPrefs.msgProvider"); //=myDrugref ID
        request.setAttribute("providermsgEdit", "provider.appointmentCardPrefs.msgEdit"); //=Enter your desired login for myDrugref
        request.setAttribute("providerbtnSubmit", "provider.appointmentCardPrefs.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.appointmentCardPrefs.msgSuccess"); //=myDrugref Id saved
        request.setAttribute("method", "saveAppointmentCardPrefs");

        this.setAppointmentCardName(name);
        this.setAppointmentCardPhone(phone);
        this.setAppointmentCardFax(fax);

        return "genAppointmentCardPrefs";
    }

    public String saveAppointmentCardPrefs() {

        UserProperty n = this.getAppointmentCardName();
        UserProperty p = this.getAppointmentCardPhone();
        UserProperty f = this.getAppointmentCardFax();

        String name = n != null ? n.getValue() : "";
        String phone = p != null ? p.getValue() : "";
        String fax = f != null ? f.getValue() : "";

        String provider = (String) request.getSession().getAttribute("user");

        UserProperty wProperty = this.userPropertyDAO.getProp(provider, "APPT_CARD_NAME");
        if (wProperty == null) {
            wProperty = new UserProperty();
            wProperty.setProviderNo(provider);
            wProperty.setName("APPT_CARD_NAME");
        }
        wProperty.setValue(name);

        userPropertyDAO.saveProp(wProperty);

        UserProperty hProperty = this.userPropertyDAO.getProp(provider, "APPT_CARD_PHONE");
        if (hProperty == null) {
            hProperty = new UserProperty();
            hProperty.setProviderNo(provider);
            hProperty.setName("APPT_CARD_PHONE");
        }
        hProperty.setValue(phone);
        userPropertyDAO.saveProp(hProperty);

        UserProperty mProperty = this.userPropertyDAO.getProp(provider, "APPT_CARD_FAX");
        if (mProperty == null) {
            mProperty = new UserProperty();
            mProperty.setProviderNo(provider);
            mProperty.setName("APPT_CARD_FAX");
        }
        mProperty.setValue(fax);
        userPropertyDAO.saveProp(mProperty);


        request.setAttribute("status", "success");
        request.setAttribute("providertitle", "provider.appointmentCardPrefs.title"); //=Set myDrugref ID
        request.setAttribute("providermsgPrefs", "provider.appointmentCardPrefs.msgPrefs"); //=Preferences"); //
        request.setAttribute("providermsgProvider", "provider.appointmentCardPrefs.msgProvider"); //=myDrugref ID
        request.setAttribute("providermsgEdit", "provider.appointmentCardPrefs.msgEdit"); //=Enter your desired login for myDrugref
        request.setAttribute("providerbtnSubmit", "provider.appointmentCardPrefs.btnSubmit"); //=Save
        request.setAttribute("providermsgSuccess", "provider.appointmentCardPrefs.msgSuccess"); //=myDrugref Id saved
        request.setAttribute("method", "saveAppointmentCardPrefs");

        return "genAppointmentCardPrefs";
    }

    public String viewBornPrefs() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();
        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.DISABLE_BORN_PROMPTS);

        String propValue = "";
        if (prop == null) {
            prop = new UserProperty();
        } else {
            propValue = prop.getValue();
        }

        boolean checked;
        if (propValue.equals("Y"))
            checked = true;
        else
            checked = false;

        prop.setChecked(checked);
        request.setAttribute("bornPromptsProperty", prop);
        request.setAttribute("providertitle", "provider.bornPrefs.title");
        request.setAttribute("providermsgPrefs", "provider.bornPrefs.msgPrefs"); //=Preferences
        request.setAttribute("providerbtnSubmit", "provider.bornPrefs.btnSubmit"); //=Save
        request.setAttribute("providerbtnCancel", "provider.bornPrefs.btnCancel"); //=Cancel
        request.setAttribute("method", "saveBornPrefs");

        this.setBornPromptsProperty(prop);

        return "genBornPrefs";
    }

    public String saveBornPrefs() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();
        UserProperty Uprop = this.getBornPromptsProperty();

        boolean checked = false;
        if (Uprop != null)
            checked = Uprop.isChecked();
        UserProperty prop = this.userPropertyDAO.getProp(providerNo, UserProperty.DISABLE_BORN_PROMPTS);
        if (prop == null) {
            prop = new UserProperty();
            prop.setName(UserProperty.DISABLE_BORN_PROMPTS);
            prop.setProviderNo(providerNo);
        }
        String propValue = "N";
        if (checked) propValue = "Y";

        prop.setValue(propValue);
        this.userPropertyDAO.saveProp(prop);

        request.setAttribute("status", "success");
        request.setAttribute("bornPromptsProperty", prop);
        request.setAttribute("providertitle", "provider.bornPrefs.title");
        request.setAttribute("providermsgPrefs", "provider.bornPrefs.msgPrefs"); //=Preferences
        request.setAttribute("providerbtnClose", "provider.bornPrefs.btnClose"); //=Close
        if (checked)
            request.setAttribute("providermsgSuccess", "provider.bornPrefs.msgSuccess_selected");
        else
            request.setAttribute("providermsgSuccess", "provider.bornPrefs.msgSuccess_unselected");
        request.setAttribute("method", "saveBornPrefs");

        return "genBornPrefs";
    }


    public String viewPreventionPrefs() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty prop = loadProperty(providerNo, UserProperty.PREVENTION_SSO_WARNING);
        UserProperty prop2 = loadProperty(providerNo, UserProperty.PREVENTION_ISPA_WARNING);
        UserProperty prop3 = loadProperty(providerNo, UserProperty.PREVENTION_NON_ISPA_WARNING);

        request.setAttribute("preventionSSOWarningProperty", prop);
        request.setAttribute("preventionISPAWarningProperty", prop2);
        request.setAttribute("preventionNonISPAWarningProperty", prop3);

        request.setAttribute("providertitle", "provider.preventionPrefs.title");
        request.setAttribute("providermsgPrefs", "provider.preventionPrefs.msgPrefs"); //=Preferences
        request.setAttribute("providerbtnSubmit", "provider.preventionPrefs.btnSubmit"); //=Save
        request.setAttribute("providerbtnCancel", "provider.preventionPrefs.btnCancel"); //=Cancel
        request.setAttribute("method", "savePreventionPrefs");

        this.setPreventionSSOWarningProperty(prop);
        this.setPreventionISPAWarningProperty(prop2);
        this.setPreventionNonISPAWarningProperty(prop3);

        return "genPreventionPrefs";
    }


    private UserProperty saveProperty(String providerNo, UserProperty Uprop, String name) {
        boolean checked = false;
        if (Uprop != null)
            checked = Uprop.isChecked();
        UserProperty prop = this.userPropertyDAO.getProp(providerNo, name);
        if (prop == null) {
            prop = new UserProperty();
            prop.setName(name);
            prop.setProviderNo(providerNo);
        }
        String propValue = "false";
        if (checked) propValue = "true";

        prop.setValue(propValue);
        this.userPropertyDAO.saveProp(prop);

        return prop;

    }

    private UserProperty loadProperty(String providerNo, String name) {
        UserProperty prop = this.userPropertyDAO.getProp(providerNo, name);
        String propValue = "";
        if (prop == null) {
            prop = new UserProperty();
        } else {
            propValue = prop.getValue();
        }

        boolean checked;
        if (propValue.equals("true"))
            checked = true;
        else
            checked = false;

        prop.setChecked(checked);

        return prop;
    }

    public String savePreventionPrefs() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty prop = saveProperty(providerNo, getPreventionSSOWarningProperty(), UserProperty.PREVENTION_SSO_WARNING);
        UserProperty prop2 = saveProperty(providerNo, getPreventionISPAWarningProperty(), UserProperty.PREVENTION_ISPA_WARNING);
        UserProperty prop3 = saveProperty(providerNo, getPreventionNonISPAWarningProperty(), UserProperty.PREVENTION_NON_ISPA_WARNING);

        request.setAttribute("status", "success");
        request.setAttribute("preventionSSOWarningProperty", prop);
        request.setAttribute("preventionISPAWarningProperty", prop2);
        request.setAttribute("preventionNonISPAWarningProperty", prop3);

        request.setAttribute("providertitle", "provider.preventionPrefs.title");
        request.setAttribute("providermsgPrefs", "provider.preventionPrefs.msgPrefs"); //=Preferences
        request.setAttribute("providerbtnClose", "provider.preventionPrefs.btnClose"); //=Close

        request.setAttribute("method", "savePreventionPrefs");

        return "genPreventionPrefs";
    }


    public String viewClinicalConnectPrefs() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty prop = loadProperty(providerNo, UserProperty.CLINICALCONNECT_DISABLE_CLOSE_WINDOW);
        UserProperty prop2 = loadProperty(providerNo, UserProperty.CLINICALCONNECT_DISABLE_LOGOUT_WARNING);

        request.setAttribute("clinicalConnectDisableCloseWindow", prop);
        request.setAttribute("clinicalConnectDisableLogoutWarning", prop2);

        request.setAttribute("providertitle", "provider.clinicalConnectPrefs.title");
        request.setAttribute("providermsgPrefs", "provider.clinicalConnectPrefs.msgPrefs"); //=Preferences
        request.setAttribute("providerbtnSubmit", "provider.clinicalConnectPrefs.btnSubmit"); //=Save
        request.setAttribute("providerbtnCancel", "provider.clinicalConnectPrefs.btnCancel"); //=Cancel
        request.setAttribute("method", "saveClinicalConnectPrefs");

        this.setClinicalConnectDisableCloseWindow(prop);
        this.setClinicalConnectDisableLogoutWarning(prop2);

        return "genClinicalConnectPrefs";
    }


    public String saveClinicalConnectPrefs() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty prop = saveProperty(providerNo, getClinicalConnectDisableCloseWindow(), UserProperty.CLINICALCONNECT_DISABLE_CLOSE_WINDOW);
        UserProperty prop2 = saveProperty(providerNo, getClinicalConnectDisableLogoutWarning(), UserProperty.CLINICALCONNECT_DISABLE_LOGOUT_WARNING);

        LogAction.addLog(LoggedInInfo.getLoggedInInfoFromSession(request), "ClinicalConnectPreferences", "clinicalConnectDisableCloseWindow", "", null, prop.getValue());
        LogAction.addLog(LoggedInInfo.getLoggedInInfoFromSession(request), "ClinicalConnectPreferences", "clinicalConnectDisableLogoutWarning", "", null, prop.getValue());

        request.setAttribute("status", "success");
        request.setAttribute("clinicalConnectDisableCloseWindow", prop);
        request.setAttribute("clinicalConnectDisableLogoutWarning", prop2);

        request.setAttribute("providertitle", "provider.clinicalConnectPrefs.title");
        request.setAttribute("providermsgPrefs", "provider.clinicalConnectPrefs.msgPrefs"); //=Preferences
        request.setAttribute("providerbtnClose", "provider.clinicalConnectPrefs.btnClose"); //=Close
        request.setAttribute("providermsgSuccess", "provider.clinicalConnectPrefs.msgSuccess");

        request.setAttribute("method", "saveClinicalConnectPrefs");

        return "genClinicalConnectPrefs";

    }

    public String viewLabMacroPrefs() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty prop = loadProperty(providerNo, UserProperty.LAB_MACRO_JSON);

        request.setAttribute("labMacroJSON", prop);

        request.setAttribute("providertitle", "provider.labMacroPrefs.title");
        request.setAttribute("providermsgPrefs", "provider.labMacroPrefs.msgPrefs"); //=Preferences
        request.setAttribute("providerbtnSubmit", "provider.labMacroPrefs.btnSubmit"); //=Save
        request.setAttribute("providerbtnCancel", "provider.labMacroPrefs.btnCancel"); //=Cancel
        request.setAttribute("method", "saveLabMacroPrefs");

        this.setLabMacroJSON(prop);

        return "genLabMacroPrefs";
    }


    public String saveLabMacroPrefs() {
        LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
        String providerNo = loggedInInfo.getLoggedInProviderNo();

        UserProperty s = this.getLabMacroJSON();

        String length = s != null ? s.getValue() : "";

        UserProperty wProperty = this.userPropertyDAO.getProp(providerNo, UserProperty.LAB_MACRO_JSON);
        if (wProperty == null) {
            wProperty = new UserProperty();
            wProperty.setProviderNo(providerNo);
            wProperty.setName(UserProperty.LAB_MACRO_JSON);
        }
        wProperty.setValue(length);
        userPropertyDAO.saveProp(wProperty);

        LogAction.addLog(LoggedInInfo.getLoggedInInfoFromSession(request), "LabMacroPreferences", "labMacroJSON", "", null, wProperty.getValue());

        request.setAttribute("status", "success");
        request.setAttribute("labMacroJSON", wProperty);

        request.setAttribute("providertitle", "provider.labMacroPrefs.title");
        request.setAttribute("providermsgPrefs", "provider.labMacroPrefs.msgPrefs"); //=Preferences
        request.setAttribute("providerbtnClose", "provider.labMacroPrefs.btnClose"); //=Close
        request.setAttribute("providermsgSuccess", "provider.labMacroPrefs.msgSuccess");

        request.setAttribute("method", "saveLabMacroPrefs");

        return "genLabMacroPrefs";
    }

    private UserProperty dateProperty;
    private UserProperty singleViewProperty;
    private UserProperty rxPageSizeProperty;
    private UserProperty existingDefaultDocQueueProperty;
    private UserProperty newDefaultDocQueueProperty;
    private UserProperty rxProfileViewProperty;
    private UserProperty rxShowPatientDOBProperty;
    private UserProperty useMyMedsProperty;
    private UserProperty rxUseRx3Property;
    private UserProperty rxDefaultQuantityProperty;
    private UserProperty dateProperty2;
    private UserProperty cppSingleLineProperty;
    private UserProperty eDocBrowserInDocumentReportProperty;
    private UserProperty eDocBrowserInMasterFileProperty;
    private UserProperty labAckCommentProperty;
    private UserProperty labRecallDelegate;
    private UserProperty labRecallMsgSubject;
    private UserProperty labRecallTicklerAssignee;
    private UserProperty labRecallTicklerPriority;
    private UserProperty taskAssigneeSelection;
    private UserProperty taskAssigneeMRP;
    private UserProperty encounterWindowWidth;
    private UserProperty encounterWindowHeight;
    private UserProperty encounterWindowMaximize;
    private UserProperty quickChartSize;
    private UserProperty patientNameLength;
    private UserProperty displayDocumentAsProperty;
    private UserProperty cobaltProperty;
    private UserProperty hideOldEchartLinkInApptProperty;
    private UserProperty dashboardShareProperty;
    private UserProperty appointmentCardName;
    private UserProperty appointmentCardPhone;
    private UserProperty appointmentCardFax;
    private UserProperty bornPromptsProperty;
    private UserProperty preventionSSOWarningProperty;
    private UserProperty preventionISPAWarningProperty;
    private UserProperty preventionNonISPAWarningProperty;
    private UserProperty clinicalConnectDisableCloseWindow;
    private UserProperty clinicalConnectDisableLogoutWarning;
    private UserProperty labMacroJSON;

    public UserProperty getDateProperty() {
        return dateProperty;
    }

    public void setDateProperty(UserProperty dateProperty) {
        this.dateProperty = dateProperty;
    }

    public UserProperty getSingleViewProperty() {
        return singleViewProperty;
    }

    public void setSingleViewProperty(UserProperty singleViewProperty) {
        this.singleViewProperty = singleViewProperty;
    }

    public UserProperty getRxPageSizeProperty() {
        return rxPageSizeProperty;
    }

    public void setRxPageSizeProperty(UserProperty rxPageSizeProperty) {
        this.rxPageSizeProperty = rxPageSizeProperty;
    }

    public UserProperty getExistingDefaultDocQueueProperty() {
        return existingDefaultDocQueueProperty;
    }

    public void setExistingDefaultDocQueueProperty(UserProperty existingDefaultDocQueueProperty) {
        this.existingDefaultDocQueueProperty = existingDefaultDocQueueProperty;
    }

    public UserProperty getNewDefaultDocQueueProperty() {
        return newDefaultDocQueueProperty;
    }

    public void setNewDefaultDocQueueProperty(UserProperty newDefaultDocQueueProperty) {
        this.newDefaultDocQueueProperty = newDefaultDocQueueProperty;
    }

    public UserProperty getRxProfileViewProperty() {
        return rxProfileViewProperty;
    }

    public void setRxProfileViewProperty(UserProperty rxProfileViewProperty) {
        this.rxProfileViewProperty = rxProfileViewProperty;
    }

    public UserProperty getRxShowPatientDOBProperty() {
        return rxShowPatientDOBProperty;
    }

    public void setRxShowPatientDOBProperty(UserProperty rxShowPatientDOBProperty) {
        this.rxShowPatientDOBProperty = rxShowPatientDOBProperty;
    }

    public UserProperty getUseMyMedsProperty() {
        return useMyMedsProperty;
    }

    public void setUseMyMedsProperty(UserProperty useMyMedsProperty) {
        this.useMyMedsProperty = useMyMedsProperty;
    }

    public UserProperty getRxUseRx3Property() {
        return rxUseRx3Property;
    }

    public void setRxUseRx3Property(UserProperty rxUseRx3Property) {
        this.rxUseRx3Property = rxUseRx3Property;
    }

    public UserProperty getRxDefaultQuantityProperty() {
        return rxDefaultQuantityProperty;
    }

    public void setRxDefaultQuantityProperty(UserProperty rxDefaultQuantityProperty) {
        this.rxDefaultQuantityProperty = rxDefaultQuantityProperty;
    }

    public UserProperty getDateProperty2() {
        return dateProperty2;
    }

    public void setDateProperty2(UserProperty dateProperty2) {
        this.dateProperty2 = dateProperty2;
    }

    public UserProperty getCppSingleLineProperty() {
        return cppSingleLineProperty;
    }

    public void setCppSingleLineProperty(UserProperty cppSingleLineProperty) {
        this.cppSingleLineProperty = cppSingleLineProperty;
    }

    public UserProperty geteDocBrowserInDocumentReportProperty() {
        return eDocBrowserInDocumentReportProperty;
    }

    public void seteDocBrowserInDocumentReportProperty(UserProperty eDocBrowserInDocumentReportProperty) {
        this.eDocBrowserInDocumentReportProperty = eDocBrowserInDocumentReportProperty;
    }

    public UserProperty geteDocBrowserInMasterFileProperty() {
        return eDocBrowserInMasterFileProperty;
    }

    public void seteDocBrowserInMasterFileProperty(UserProperty eDocBrowserInMasterFileProperty) {
        this.eDocBrowserInMasterFileProperty = eDocBrowserInMasterFileProperty;
    }

    public UserProperty getLabAckCommentProperty() {
        return labAckCommentProperty;
    }

    public void setLabAckCommentProperty(UserProperty labAckCommentProperty) {
        this.labAckCommentProperty = labAckCommentProperty;
    }

    public UserProperty getLabRecallDelegate() {
        return labRecallDelegate;
    }

    public void setLabRecallDelegate(UserProperty labRecallDelegate) {
        this.labRecallDelegate = labRecallDelegate;
    }

    public UserProperty getLabRecallMsgSubject() {
        return labRecallMsgSubject;
    }

    public void setLabRecallMsgSubject(UserProperty labRecallMsgSubject) {
        this.labRecallMsgSubject = labRecallMsgSubject;
    }

    public UserProperty getLabRecallTicklerAssignee() {
        return labRecallTicklerAssignee;
    }

    public void setLabRecallTicklerAssignee(UserProperty labRecallTicklerAssignee) {
        this.labRecallTicklerAssignee = labRecallTicklerAssignee;
    }

    public UserProperty getLabRecallTicklerPriority() {
        return labRecallTicklerPriority;
    }

    public void setLabRecallTicklerPriority(UserProperty labRecallTicklerPriority) {
        this.labRecallTicklerPriority = labRecallTicklerPriority;
    }

    public UserProperty getTaskAssigneeSelection() {
        return taskAssigneeSelection;
    }

    public void setTaskAssigneeSelection(UserProperty taskAssigneeSelection) {
        this.taskAssigneeSelection = taskAssigneeSelection;
    }

    public UserProperty getTaskAssigneeMRP() {
        return taskAssigneeMRP;
    }

    public void setTaskAssigneeMRP(UserProperty taskAssigneeMRP) {
        this.taskAssigneeMRP = taskAssigneeMRP;
    }

    public UserProperty getEncounterWindowWidth() {
        return encounterWindowWidth;
    }

    public void setEncounterWindowWidth(UserProperty encounterWindowWidth) {
        this.encounterWindowWidth = encounterWindowWidth;
    }

    public UserProperty getEncounterWindowHeight() {
        return encounterWindowHeight;
    }

    public void setEncounterWindowHeight(UserProperty encounterWindowHeight) {
        this.encounterWindowHeight = encounterWindowHeight;
    }

    public UserProperty getEncounterWindowMaximize() {
        return encounterWindowMaximize;
    }

    public void setEncounterWindowMaximize(UserProperty encounterWindowMaximize) {
        this.encounterWindowMaximize = encounterWindowMaximize;
    }

    public UserProperty getQuickChartSize() {
        return quickChartSize;
    }

    public void setQuickChartSize(UserProperty quickChartSize) {
        this.quickChartSize = quickChartSize;
    }

    public UserProperty getPatientNameLength() {
        return patientNameLength;
    }

    public void setPatientNameLength(UserProperty patientNameLength) {
        this.patientNameLength = patientNameLength;
    }

    public UserProperty getDisplayDocumentAsProperty() {
        return displayDocumentAsProperty;
    }

    public void setDisplayDocumentAsProperty(UserProperty displayDocumentAsProperty) {
        this.displayDocumentAsProperty = displayDocumentAsProperty;
    }

    public UserProperty getCobaltProperty() {
        return cobaltProperty;
    }

    public void setCobaltProperty(UserProperty cobaltProperty) {
        this.cobaltProperty = cobaltProperty;
    }

    public UserProperty getHideOldEchartLinkInApptProperty() {
        return hideOldEchartLinkInApptProperty;
    }

    public void setHideOldEchartLinkInApptProperty(UserProperty hideOldEchartLinkInApptProperty) {
        this.hideOldEchartLinkInApptProperty = hideOldEchartLinkInApptProperty;
    }

    public UserProperty getDashboardShareProperty() {
        return dashboardShareProperty;
    }

    public void setDashboardShareProperty(UserProperty dashboardShareProperty) {
        this.dashboardShareProperty = dashboardShareProperty;
    }

    public UserProperty getAppointmentCardName() {
        return appointmentCardName;
    }

    public void setAppointmentCardName(UserProperty appointmentCardName) {
        this.appointmentCardName = appointmentCardName;
    }

    public UserProperty getAppointmentCardPhone() {
        return appointmentCardPhone;
    }

    public void setAppointmentCardPhone(UserProperty appointmentCardPhone) {
        this.appointmentCardPhone = appointmentCardPhone;
    }

    public UserProperty getAppointmentCardFax() {
        return appointmentCardFax;
    }

    public void setAppointmentCardFax(UserProperty appointmentCardFax) {
        this.appointmentCardFax = appointmentCardFax;
    }

    public UserProperty getBornPromptsProperty() {
        return bornPromptsProperty;
    }

    public void setBornPromptsProperty(UserProperty bornPromptsProperty) {
        this.bornPromptsProperty = bornPromptsProperty;
    }

    public UserProperty getPreventionSSOWarningProperty() {
        return preventionSSOWarningProperty;
    }

    public void setPreventionSSOWarningProperty(UserProperty preventionSSOWarningProperty) {
        this.preventionSSOWarningProperty = preventionSSOWarningProperty;
    }

    public UserProperty getPreventionISPAWarningProperty() {
        return preventionISPAWarningProperty;
    }

    public void setPreventionISPAWarningProperty(UserProperty preventionISPAWarningProperty) {
        this.preventionISPAWarningProperty = preventionISPAWarningProperty;
    }

    public UserProperty getPreventionNonISPAWarningProperty() {
        return preventionNonISPAWarningProperty;
    }

    public void setPreventionNonISPAWarningProperty(UserProperty preventionNonISPAWarningProperty) {
        this.preventionNonISPAWarningProperty = preventionNonISPAWarningProperty;
    }

    public UserProperty getClinicalConnectDisableCloseWindow() {
        return clinicalConnectDisableCloseWindow;
    }

    public void setClinicalConnectDisableCloseWindow(UserProperty clinicalConnectDisableCloseWindow) {
        this.clinicalConnectDisableCloseWindow = clinicalConnectDisableCloseWindow;
    }

    public UserProperty getClinicalConnectDisableLogoutWarning() {
        return clinicalConnectDisableLogoutWarning;
    }

    public void setClinicalConnectDisableLogoutWarning(UserProperty clinicalConnectDisableLogoutWarning) {
        this.clinicalConnectDisableLogoutWarning = clinicalConnectDisableLogoutWarning;
    }

    public UserProperty getLabMacroJSON() {
        return labMacroJSON;
    }

    public void setLabMacroJSON(UserProperty labMacroJSON) {
        this.labMacroJSON = labMacroJSON;
    }
}
