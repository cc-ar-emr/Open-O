//CHECKSTYLE:OFF
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


package oscar.oscarBilling.ca.bc.pageUtil;

/*
 *
 * Copyright (c) 2001-2002. Department of Family Medicine, McMaster University. All Rights Reserved. *
 * This software is published under the GPL GNU General Public License.
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. * * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA. *
 *
 * <OSCAR TEAM>
 *
 * This software was written for the
 * Department of Family Medicine
 * McMaster University
 * Hamilton
 * Ontario, Canada
 */
import oscar.util.StringUtils;

import javax.servlet.http.HttpServletRequest;

public final class BillingReProcessBill2Form {
    String billingmasterNo = null;
    String insurerCode = null;
    String provider_no = null;
    String demoNo = null;
    String dependentNo = null;
    String afterHours = null;
    String status = null;
    String submit = null;
    String billNumber = null;

    String locationVisit = null;
    String anatomicalArea = null;
    String newProgram = null;
    String service_code = null;
    String billing_unit = null;
    String billing_amount = null;
    String billingUnit = null;
    String billingAmount = null;
    String dx1 = null;
    String dx2 = null;
    String dx3 = null;

    String paymentMode = null;
    String submissionCode = null;
    String serviceDate = null;
    String serviceToDay = null;
    String serviceLocation = null;
    String referalPracCD1 = null;
    String referalPrac1 = null;
    String referalPracCD2 = null;
    String referalPrac2 = null;


    String timeCallRec = null;
    String startTime = null;
    String finishTime = null;
    String correspondenceCode = null;
    String mvaClaim = null;
    String shortComment = null;
    String icbcClaim = null;
    String facilityNum = null;
    String facilitySubNum = null;

    String notes = null;
    String dependent = null;

    String messageNotes = null;

    String debitRequestSeqNum = null;
    String debitRequestDate = null;
    String adjAmount;
    private String adjType;
    
  /*
   
     //
   *
   
   
   
   
   
   
        String afterHour;//f
   
   
   
        String exSubmissionCode;//f
   
        String dxExpansion;//f
        String serviceLocation;//f
   
        String timeCall ;//f
        String serviceStartTime;//f
        String serviceEndTime ;//f
        String birthDate ;//f
        String office_number;
        String correspondenceCode ;//f
        String claimComment ;//f
   
        String billingStatus;//f
   
   
        String oinInsurerCode;//f
     //
   
xml_appointment_date
   
hc_sex
   
facilityNum
startTime
referalPracCD
submissionCode=
icbcClaim
xml_dob
demo_address
visittype
mvaClaim
correspondenceCode
xml_diagnostic_code
timeCallRec
address4
address3
address2
address1
finishTime
demo_name
serviceToDay
xml_clinic_ref_code
   
submit
sex
paymentMode
serviceDate
registrationNum
billing_amount
xml_hin
billing_unit
birthdate
xml_vdate
billingamount
status
shortComment
xml_status
demo_province
service_code
xml_billing_no
firstName
billingunit
surname
facilitySubNum
referalPrac
xml_visitdate
update_date
xml_visittype
clinic_ref_code
   

    /**
     * Getter for property billingmasterNo.
     *
     * @return Value of property billingmasterNo.
     */
    public String getBillingmasterNo() {
        return billingmasterNo;
    }

    /**
     * Setter for property billingmasterNo.
     *
     * @param billingmasterNo New value of property billingmasterNo.
     */
    public void setBillingmasterNo(String billingmasterNo) {
        this.billingmasterNo = billingmasterNo;
    }

    /**
     * Getter for property insurerCode.
     *
     * @return Value of property insurerCode.
     */
    public String getInsurerCode() {
        return insurerCode;
    }

    /**
     * Setter for property insurerCode.
     *
     * @param insurerCode New value of property insurerCode.
     */
    public void setInsurerCode(String insurerCode) {
        this.insurerCode = insurerCode;
    }

    /**
     * Getter for property provider_no.
     *
     * @return Value of property provider_no.
     */
    public String getProviderNo() {
        return provider_no;
    }

    /**
     * Setter for property provider_no.
     *
     * @param provider_no New value of property provider_no.
     */
    public void setProviderNo(String provider_no) {
        this.provider_no = provider_no;
    }

    /**
     * Getter for property demoNo.
     *
     * @return Value of property demoNo.
     */
    public String getDemoNo() {
        return demoNo;
    }

    /**
     * Setter for property demoNo.
     *
     * @param demoNo New value of property demoNo.
     */
    public void setDemoNo(String demoNo) {
        this.demoNo = demoNo;
    }

    /**
     * Getter for property dependentNo.
     *
     * @return Value of property dependentNo.
     */
    public String getDependentNo() {
        return dependentNo;
    }

    /**
     * Setter for property dependentNo.
     *
     * @param dependentNo New value of property dependentNo.
     */
    public void setDependentNo(String dependentNo) {
        this.dependentNo = dependentNo;
    }

    /**
     * Getter for property billing_unit.
     *
     * @return Value of property billing_unit.
     */
    public String getBilling_unit() {
        return billing_unit;
    }

    /**
     * Setter for property billing_unit.
     *
     * @param billing_unit New value of property billing_unit.
     */
    public void setBilling_unit(String billing_unit) {
        this.billing_unit = billing_unit;
    }

    /**
     * Getter for property locationVisit.
     *
     * @return Value of property locationVisit.
     */
    public String getLocationVisit() {
        return locationVisit;
    }

    /**
     * Setter for property locationVisit.
     *
     * @param locationVisit New value of property locationVisit.
     */
    public void setLocationVisit(String locationVisit) {
        this.locationVisit = locationVisit;
    }

    /**
     * Getter for property anatomicalArea.
     *
     * @return Value of property anatomicalArea.
     */
    public String getAnatomicalArea() {
        return anatomicalArea;
    }

    /**
     * Setter for property anatomicalArea.
     *
     * @param anatomicalArea New value of property anatomicalArea.
     */
    public void setAnatomicalArea(String anatomicalArea) {
        this.anatomicalArea = anatomicalArea;
    }

    /**
     * Getter for property newProgram.
     *
     * @return Value of property newProgram.
     */
    public String getNewProgram() {
        return newProgram;
    }

    /**
     * Setter for property newProgram.
     *
     * @param newProgram New value of property newProgram.
     */
    public void setNewProgram(String newProgram) {
        this.newProgram = newProgram;
    }

    /**
     * Getter for property service_code.
     *
     * @return Value of property service_code.
     */
    public String getService_code() {
        return service_code;
    }

    /**
     * Setter for property service_code.
     *
     * @param service_code New value of property service_code.
     */
    public void setService_code(String service_code) {
        this.service_code = service_code;
    }

    /**
     * Getter for property billing_amount.
     *
     * @return Value of property billing_amount.
     */
    public String getBilling_amount() {
        return billing_amount;
    }

    /**
     * Setter for property billing_amount.
     *
     * @param billing_amount New value of property billing_amount.
     */
    public void setBilling_amount(String billing_amount) {
        this.billing_amount = billing_amount;
    }

    /**
     * Getter for property dx1.
     *
     * @return Value of property dx1.
     */
    public String getDx1() {
        return dx1;
    }

    /**
     * Setter for property dx1.
     *
     * @param dx1 New value of property dx1.
     */
    public void setDx1(String dx1) {
        this.dx1 = dx1;
    }

    /**
     * Getter for property dx2.
     *
     * @return Value of property dx2.
     */
    public String getDx2() {
        return dx2;
    }

    /**
     * Setter for property dx2.
     *
     * @param dx2 New value of property dx2.
     */
    public void setDx2(String dx2) {
        this.dx2 = dx2;
    }

    /**
     * Getter for property dx3.
     *
     * @return Value of property dx3.
     */
    public String getDx3() {
        return dx3;
    }

    /**
     * Setter for property dx3.
     *
     * @param dx3 New value of property dx3.
     */
    public void setDx3(String dx3) {
        this.dx3 = dx3;
    }

    /**
     * Getter for property paymentMode.
     *
     * @return Value of property paymentMode.
     */
    public String getPaymentMode() {
        return paymentMode;
    }

    /**
     * Setter for property paymentMode.
     *
     * @param paymentMode New value of property paymentMode.
     */
    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    /**
     * Getter for property submissionCode.
     *
     * @return Value of property submissionCode.
     */
    public String getSubmissionCode() {
        return submissionCode;
    }

    /**
     * Setter for property submissionCode.
     *
     * @param submissionCode New value of property submissionCode.
     */
    public void setSubmissionCode(String submissionCode) {
        this.submissionCode = submissionCode;
    }

    /**
     * Getter for property serviceDate.
     *
     * @return Value of property serviceDate.
     */
    public String getServiceDate() {
        return serviceDate;
    }

    /**
     * Setter for property serviceDate.
     *
     * @param serviceDate New value of property serviceDate.
     */
    public void setServiceDate(String serviceDate) {
        this.serviceDate = serviceDate;
    }

    /**
     * Getter for property serviceToDay.
     *
     * @return Value of property serviceToDay.
     */
    public String getServiceToDay() {
        return serviceToDay;
    }

    /**
     * Setter for property serviceToDay.
     *
     * @param serviceToDay New value of property serviceToDay.
     */
    public void setServiceToDay(String serviceToDay) {
        this.serviceToDay = serviceToDay;
    }

    /**
     * Getter for property referalPracCD1.
     *
     * @return Value of property referalPracCD1.
     */
    public String getReferalPracCD1() {
        return referalPracCD1;
    }

    /**
     * Setter for property referalPracCD1.
     *
     * @param referalPracCD1 New value of property referalPracCD1.
     */
    public void setReferalPracCD1(String referalPracCD1) {
        this.referalPracCD1 = referalPracCD1;
    }

    /**
     * Getter for property referalPrac1.
     *
     * @return Value of property referalPrac1.
     */
    public String getReferalPrac1() {
        return referalPrac1;
    }

    /**
     * Setter for property referalPrac1.
     *
     * @param referalPrac1 New value of property referalPrac1.
     */
    public void setReferalPrac1(String referalPrac1) {
        this.referalPrac1 = referalPrac1;
    }

    /**
     * Getter for property referalPracCD2.
     *
     * @return Value of property referalPracCD2.
     */
    public String getReferalPracCD2() {
        return referalPracCD2;
    }

    /**
     * Setter for property referalPracCD2.
     *
     * @param referalPracCD2 New value of property referalPracCD2.
     */
    public void setReferalPracCD2(String referalPracCD2) {
        this.referalPracCD2 = referalPracCD2;
    }

    /**
     * Getter for property referalPrac2.
     *
     * @return Value of property referalPrac2.
     */
    public String getReferalPrac2() {
        return referalPrac2;
    }

    /**
     * Setter for property referalPrac2.
     *
     * @param referalPrac2 New value of property referalPrac2.
     */
    public void setReferalPrac2(String referalPrac2) {
        this.referalPrac2 = referalPrac2;
    }

    /**
     * Getter for property timeCallRec.
     *
     * @return Value of property timeCallRec.
     */
    public String getTimeCallRec() {
        return timeCallRec;
    }

    /**
     * Setter for property timeCallRec.
     *
     * @param timeCallRec New value of property timeCallRec.
     */
    public void setTimeCallRec(String timeCallRec) {
        this.timeCallRec = timeCallRec;
    }

    /**
     * Getter for property startTime.
     *
     * @return Value of property startTime.
     */
    public String getStartTime() {
        return startTime;
    }

    /**
     * Setter for property startTime.
     *
     * @param startTime New value of property startTime.
     */
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    /**
     * Getter for property finishTime.
     *
     * @return Value of property finishTime.
     */
    public String getFinishTime() {
        return finishTime;
    }

    /**
     * Setter for property finishTime.
     *
     * @param finishTime New value of property finishTime.
     */
    public void setFinishTime(String finishTime) {
        this.finishTime = finishTime;
    }

    /**
     * Getter for property correspondenceCode.
     *
     * @return Value of property correspondenceCode.
     */
    public String getCorrespondenceCode() {
        return correspondenceCode;
    }

    /**
     * Setter for property correspondenceCode.
     *
     * @param correspondenceCode New value of property correspondenceCode.
     */
    public void setCorrespondenceCode(String correspondenceCode) {
        this.correspondenceCode = correspondenceCode;
    }

    /**
     * Getter for property mvaClaim.
     *
     * @return Value of property mvaClaim.
     */
    public String getMvaClaim() {
        return mvaClaim;
    }

    /**
     * Setter for property mvaClaim.
     *
     * @param mvaClaim New value of property mvaClaim.
     */
    public void setMvaClaim(String mvaClaim) {
        this.mvaClaim = mvaClaim;
    }

    /**
     * Getter for property shortComment.
     *
     * @return Value of property shortComment.
     */
    public String getShortComment() {
        return shortComment;
    }

    /**
     * Setter for property shortComment.
     *
     * @param shortComment New value of property shortComment.
     */
    public void setShortComment(String shortComment) {
        this.shortComment = shortComment;
    }

    /**
     * Getter for property icbcClaim.
     *
     * @return Value of property icbcClaim.
     */
    public String getIcbcClaim() {
        return icbcClaim;
    }

    /**
     * Setter for property icbcClaim.
     *
     * @param icbcClaim New value of property icbcClaim.
     */
    public void setIcbcClaim(String icbcClaim) {
        this.icbcClaim = icbcClaim;
    }

    /**
     * Getter for property facilityNum.
     *
     * @return Value of property facilityNum.
     */
    public String getFacilityNum() {
        return facilityNum;
    }

    /**
     * Setter for property facilityNum.
     *
     * @param facilityNum New value of property facilityNum.
     */
    public void setFacilityNum(String facilityNum) {
        this.facilityNum = facilityNum;
    }

    /**
     * Getter for property facilitySubNum.
     *
     * @return Value of property facilitySubNum.
     */
    public String getFacilitySubNum() {
        return facilitySubNum;
    }

    /**
     * Setter for property facilitySubNum.
     *
     * @param facilitySubNum New value of property facilitySubNum.
     */
    public void setFacilitySubNum(String facilitySubNum) {
        this.facilitySubNum = facilitySubNum;
    }

    /**
     * Getter for property afterHours.
     *
     * @return Value of property afterHours.
     */
    public String getAfterHours() {
        return afterHours;
    }

    /**
     * Setter for property afterHours.
     *
     * @param afterHours New value of property afterHours.
     */
    public void setAfterHours(String afterHours) {
        this.afterHours = afterHours;
    }

    /**
     * Getter for property serviceLocation.
     *
     * @return Value of property serviceLocation.
     */
    public String getServiceLocation() {
        return serviceLocation;
    }

    /**
     * Setter for property serviceLocation.
     *
     * @param serviceLocation New value of property serviceLocation.
     */
    public void setServiceLocation(String serviceLocation) {
        this.serviceLocation = serviceLocation;
    }

    /**
     * Getter for property status.
     *
     * @return Value of property status.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Setter for property status.
     *
     * @param status New value of property status.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Getter for property submit.
     *
     * @return Value of property submit.
     */
    public String getSubmit() {
        return submit;
    }

    /**
     * Setter for property submit.
     *
     * @param submit New value of property submit.
     */
    public void setSubmit(String submit) {
        this.submit = submit;
    }

    /**
     * Getter for property billNumber.
     *
     * @return Value of property billNumber.
     */
    public String getBillNumber() {
        return billNumber;
    }

    /**
     * Setter for property billNumber.
     *
     * @param billNumber New value of property billNumber.
     */
    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber;
    }

    /**
     * Getter for property billingUnit.
     *
     * @return Value of property billingUnit.
     */
    public String getBillingUnit() {
        return billingUnit;
    }

    /**
     * Setter for property billingUnit.
     *
     * @param billingUnit New value of property billingUnit.
     */
    public void setBillingUnit(String billingUnit) {
        this.billingUnit = billingUnit;
    }

    /**
     * Getter for property billingAmount.
     *
     * @return Value of property billingAmount.
     */
    public String getBillingAmount() {
        return billingAmount;
    }

    /**
     * Setter for property billingAmount.
     *
     * @param billingAmount New value of property billingAmount.
     */
    public void setBillingAmount(String billingAmount) {
        this.billingAmount = billingAmount;
    }

    /**
     * Getter for property notes.
     *
     * @return Value of property notes.
     */
    public String getNotes() {
        return notes;
    }

    /**
     * Setter for property notes.
     *
     * @param notes New value of property notes.
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Getter for property dependent.
     *
     * @return Value of property dependent.
     */
    public String getDependent() {
        return dependent;
    }

    /**
     * Setter for property dependent.
     *
     * @param dependent New value of property dependent.
     */
    public void setDependent(String dependent) {
        this.dependent = dependent;
    }

    /**
     * Getter for property debitRequestSeqNum.
     *
     * @return Value of property debitRequestSeqNum.
     */
    public String getDebitRequestSeqNum() {
        return debitRequestSeqNum;
    }

    /**
     * Setter for property debitRequestSeqNum.
     *
     * @param debitRequestSeqNum New value of property debitRequestSeqNum.
     */
    public void setDebitRequestSeqNum(String debitRequestSeqNum) {
        this.debitRequestSeqNum = debitRequestSeqNum;
    }

    /**
     * Getter for property debitRequestDate.
     *
     * @return Value of property debitRequestDate.
     */
    public String getDebitRequestDate() {
        return debitRequestDate;
    }

    /**
     * Setter for property debitRequestDate.
     *
     * @param debitRequestDate New value of property debitRequestDate.
     */
    public void setDebitRequestDate(String debitRequestDate) {
        this.debitRequestDate = debitRequestDate;
    }

    /**
     * Getter for property messageNotes.
     *
     * @return Value of property messageNotes.
     */
    public String getMessageNotes() {
        return messageNotes;
    }

    public String getAdjAmount() {
        return adjAmount;
    }

    public String getAdjType() {
        return adjType;
    }

    /**
     * Setter for property messageNotes.
     *
     * @param messageNotes New value of property messageNotes.
     */
    public void setMessageNotes(String messageNotes) {
        this.messageNotes = messageNotes;
    }

    public void setAdjAmount(String adjAmount) {
        this.adjAmount = adjAmount;
    }

    public void setAdjType(String adjType) {
        this.adjType = adjType;
    }

    /**
     * Validate the properties that have been set from this HTTP request,
     * and return an <code>ActionErrors</code> object that encapsulates any
     * validation errors that have been found.  If no errors are found, return
     * <code>null</code> or an <code>ActionErrors</code> object with no
     * recorded error messages.
     *
     * @param mapping The mapping used to select this instance
     * @param request The servlet request we are processing
     * @return fill in later
     */
    //public ActionErrors validate(ActionMapping mapping,
    //                               HttpServletRequest request) {

    //   ActionErrors errors = new ActionErrors();

    //   if (message == null || message.length() == 0){
    //      errors.add("message", new ActionError("error.message.missing"));
    //   }

    //   if (provider == null || provider.length == 0){
    //      errors.add(ActionErrors.GLOBAL_ERROR,
    //              new ActionError("error.provider.missing"));
    //   }

    //   return errors;

    //}

}//CreateMessageForm
