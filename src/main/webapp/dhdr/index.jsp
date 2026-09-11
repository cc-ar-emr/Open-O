<%--

    Copyright (c) 2001-2002. Department of Family Medicine, McMaster University. All Rights Reserved.
    This software is published under the GPL GNU General Public License.
    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public License
    as published by the Free Software Foundation; either version 2
    of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.

    This software was written for the
    Department of Family Medicine
    McMaster University
    Hamilton
    Ontario, Canada

--%>
<%@ taglib uri="/WEB-INF/security.tld" prefix="security"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@page import="ca.openosp.openo.utility.SpringUtils" %>
<%@page import="ca.openosp.openo.commn.dao.SystemPreferencesDao" %>
<%@page import="ca.openosp.openo.commn.model.SystemPreferences" %>
<%@page import="ca.openosp.OscarProperties" %>
<%@page import="ca.openosp.openo.commn.dao.UserPropertyDAO" %>
<%@page import="ca.openosp.openo.commn.model.UserProperty" %>
<%@page import="org.owasp.encoder.Encode" %>
<%@page import="ca.openosp.openo.integration.dhdr.ConsentOverrideChoice" %>
<fmt:setBundle basename="oscarResources"/>
<%
    String roleName$ = (String)session.getAttribute("userrole") + "," + (String) session.getAttribute("user");
	boolean authed=true;
%><security:oscarSec roleName="<%=roleName$%>" objectName="_rx" rights="r" reverse="<%=true%>">
	<%authed=false; %>
	<%response.sendRedirect("../securityError.jsp?type=_rx");%>
</security:oscarSec><%
	if(!authed) {
		return;
	}

	// DHDR12.01-12.02: the ODB Formulary ("Check Medication Coverage - Drug Formulary") and EAP
	// links. EAP requests are submitted through the SADIE portal, so the EAP link targets SADIE.
	// Both URLs are admin-configurable in oscar_mcmaster.properties (dhdr.odb_formulary_url /
	// dhdr.eap_url); the OMD-published addresses are used when the properties are unset.
	String odbUrl = OscarProperties.getInstance().getProperty("dhdr.odb_formulary_url", "https://www.ontario.ca/check-medication-coverage/");
	String eapUrl = OscarProperties.getInstance().getProperty("dhdr.eap_url", "https://www.ontario.ca/page/sadie-special-authorization-digital-information-exchange");

	// DHDR-04: PCOI viewlet "not responding" timeout in milliseconds, derived from the shared
	// viewlet_timeout system preference, which is held in seconds; default 300000 (5 minutes) when
	// unset or invalid.
	// The preference is seeded at 65 seconds, which is a sensible wait for an HTTP round trip. This
	// timer instead waits on a clinician filling in a consent-override form, so the modal applies
	// its own multiple rather than the raw value: at the seeded 65 s the notice appears after 325 s,
	// near the 5 minutes this timer used before.
	// Zero and negative are unusable rather than merely unusual: the timer they produce fires
	// immediately, so every override would be reported as not responding while the viewlet is still
	// on screen. They fall back to the default alongside a non-numeric value, as does a value large
	// enough to overflow the millisecond conversion.
	final int MODAL_TIMEOUT_MULTIPLE = 5;
	int viewletTimeout = 300000;
	SystemPreferencesDao systemPreferencesDao = SpringUtils.getBean(SystemPreferencesDao.class);
	SystemPreferences viewletTimeoutPref = systemPreferencesDao.findPreferenceByName(SystemPreferences.ONEID_KEYS.viewlet_timeout);
	if (viewletTimeoutPref != null && viewletTimeoutPref.getValue() != null && !viewletTimeoutPref.getValue().trim().isEmpty()) {
		try {
			int configuredSeconds = Integer.parseInt(viewletTimeoutPref.getValue().trim());
			long configuredMillis = (long) configuredSeconds * 1000L * MODAL_TIMEOUT_MULTIPLE;
			if (configuredSeconds > 0 && configuredMillis <= Integer.MAX_VALUE) {
				viewletTimeout = (int) configuredMillis;
			}
		} catch (NumberFormatException e) {
			// keep the 300000 default
		}
	}

	// DHDR02.03: the default "last N days" search window. A provider sets their own on the provider
	// Preferences screen (dhdr_default_search_days); when they have not, the instance-wide
	// oscar_mcmaster.properties key dhdr.default_search_days applies, and the requirement's suggested
	// 120 when neither is set or either is invalid. Read once here, and only ever read — nothing on
	// this page writes back into the default, which DHDR02.03 forbids.
	//
	// "Invalid" includes anything above a century of days. The range is derived below as
	// start.setDate(end.getDate() - days), which on an unbounded value yields an Invalid Date: the
	// search then runs with no start bound at all and reports no error. A provider's own value is
	// already held to this bound when it is saved; the instance-wide property is not edited through
	// a form, so it is bounded on the way in here instead.
	int maxSearchDays = 36500;
	int defaultSearchDays = 120;
	String configuredSearchDays = OscarProperties.getInstance()
		.getProperty("dhdr.default_search_days", String.valueOf(defaultSearchDays));
	UserPropertyDAO userPropertyDao = SpringUtils.getBean(UserPropertyDAO.class);
	String providerSearchDays = userPropertyDao.getStringValue((String) session.getAttribute("user"),
		UserProperty.DHDR_DEFAULT_SEARCH_DAYS);
	if (providerSearchDays != null && !providerSearchDays.trim().isEmpty()) {
		configuredSearchDays = providerSearchDays;
	}
	try {
		int configured = Integer.parseInt(configuredSearchDays.trim());
		if (configured > 0 && configured <= maxSearchDays) {
			defaultSearchDays = configured;
		}
	} catch (NumberFormatException e) {
		// keep the default above
	}
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">
<html ng-app="dhdrView">
<head>
	<title>DHDR Search</title>
	<link href="<%=request.getContextPath() %>/library/bootstrap/3.0.0/css/bootstrap.css" rel="stylesheet">
	<link rel="stylesheet" href="<%=request.getContextPath() %>/css/font-awesome.min.css">
	<script type="text/javascript" src="<%=request.getContextPath() %>/library/angular.min.js"></script>	
	<script type="text/javascript" src="<%=request.getContextPath() %>/library/ui-bootstrap-tpls-0.11.0.js"></script>
	<script src="<%=request.getContextPath() %>/web/common/demographicServices.js"></script>	
	<script src="<%=request.getContextPath() %>/web/common/providerServices.js"></script>	
	<script src="<%=request.getContextPath() %>/web/common/dhdrServices.js"></script>	
	<script src="<%=request.getContextPath() %>/web/common/rxServices.js"></script>	
	<script src="<%=request.getContextPath() %>/web/common/filters.js"></script>
	<%-- DHDR11.01: the shared ONE ID viewlet helpers. parseViewletCompletion reads the ONE Access
	     Viewlet Framework completion message and reportViewletResult records the outcome, so the
	     consent viewlet is read and audited by the same code as every other EHR service rather than
	     by a second implementation maintained here. --%>
	<script src="<%=request.getContextPath() %>/share/javascript/oneid.js"></script>
	<style>
		.modal-lg{
			width:1700px;
		}
		.container {
			max-width: 1750px;
		}
		.pcoi-frame {
			width: -webkit-fill-available;
		}
		.startDate {
			min-width: 90px;
		}
		.table-overflow-x {
			overflow-x: auto;
		}
		.modal-lg {
			width: 95vw;
		}
		.modal-lg iframe {
			height: 80vh;
		}
	</style>
</head>

<body vlink="#0000FF" class="BodyStyle">
	<div ng-controller="dhdrView">
	<div class="page-header container" style="margin-top: 0px; margin-bottom: 0px;">
		<div class="row">
			<div class="col-xs-12">
				<h1 class="patientHeaderName" style="margin-top: 0px;" ng-cloak>
				<b>{{demographic.lastName}}, {{demographic.firstName}}</b>  <span ng-show="demographic.hin">({{demographic.hin}})</span>

				<small class="patientHeaderExt pull-right">
					<i><fmt:message key="demographic.patient.context.born"/>: </i>
					<b>{{demographicDobText()}}</b> (<b>{{demographic.age | age}}</b>) &nbsp;&nbsp; <i><fmt:message key="demographic.patient.context.sex"/>:</i> <b>{{demographic.sex}}</b>
					<i> &nbsp;&nbsp; <fmt:message key="Appointment.msgTelephone"/>:</i> <b>{{demographic.phone}}</b>
					<!-- <span class="glyphicon glyphicon-new-window"></span>-->
				</small>
			</h1>
			</div>
		</div>
	</div>
	<div class="container" ng-show="dhdrPatientResolved" ng-cloak>
		<%-- DHDR03.02: identify the DHDR-side patient whenever events are shown, flagging
		     fields that do not match the EMR demographic. --%>
		<div class="alert" ng-class="dhdrPatientDataUnmatched ? 'alert-warning' : 'alert-info'" role="alert" style="margin-bottom: 8px;">
			<strong>DHDR EHR Service patient:</strong>
			{{dhdrPatient.lastName}}, {{dhdrPatient.firstName}}{{matchNote(dhdrPatient.nameUnmatched, dhdrPatient.nameMissing)}}{{variantNote('lastName')}}{{variantNote('firstName')}}
			&nbsp;&middot;&nbsp; HIN: {{dhdrPatient.hin}}{{headlineNote('hin', dhdrPatient.hin, dhdrPatient.hinUnmatched, dhdrPatient.hinMissing)}}{{variantNote('hin')}}
			&nbsp;&middot;&nbsp; DOB: {{dhdrPatient.dob | date}}{{headlineNote('dob', dhdrPatient.dob, dhdrPatient.dobUnmatched, dhdrPatient.dobMissing)}}{{variantNote('dob')}}
			&nbsp;&middot;&nbsp; Sex: {{dhdrPatient.gender}}{{headlineNote('gender', dhdrPatient.gender, dhdrPatient.genderUnmatched, dhdrPatient.genderMissing)}}{{variantNote('gender')}}
			<div ng-show="dhdrPatientDataUnmatched"><em>Some of this patient's DHDR details differ from the EMR record, or are not recorded in it.</em></div>
			<%-- DHDR03.02: where the returned records disagree with each other, say so. Otherwise
			     the banner shows one value and silently drops the rest, which is how the mismatch
			     warning came to depend on which record arrived last. --%>
			<div ng-show="anyVariants()"><em>The DHDR EHR Service returned more than one value for some of this patient's details; every value shown was compared against the EMR.</em></div>
		</div>
	</div>
	<div class="container">
		<h4>More Information:</h4>
		<input type="button" class="btn btn-default" value="Ontario Drug Benefit" ng-click="openWindow('<%=Encode.forJavaScript(odbUrl)%>')"/>
		<input type="button" class="btn btn-default" value="Exceptional Access Program" ng-click="openWindow('<%=Encode.forJavaScript(eapUrl)%>')"/>
		<%-- DHDR13.02: entry point to the cross-patient Temporary Consent Unblock report (opens in a new window). --%>
		<input type="button" class="btn btn-default" value="Consent Override Report" ng-click="openWindow('<%=Encode.forJavaScript(request.getContextPath() + "/dhdr/consentOverrideReport.do")%>')"/>
	</div>
	<hr/>
	<div class="container">
		<div class="row">
		 	<div class="col-xs-12" >
		 		<form class="form-inline">
				  <div class="form-group">
				    <label for="exampleInputName2">Start Date</label>
				    <input type="date" class="form-control" id="exampleInputName2" placeholder="2020-01-01" ng-model="searchConfig.startDate" ng-change="clearSearchDays();">
				  </div>
				  <div class="form-group">
				    <label for="exampleInputEmail2">End Date</label>
				    <input type="date" class="form-control" id="exampleInputEmail2" placeholder="2020-03-31" ng-model="searchConfig.endDate" ng-change="clearSearchDays();">
				  </div>
				  <%-- DHDR02.03(a): search by number of days before today. Fills the two date fields
				       above, so the range stays visible and hand-editable afterwards. --%>
				  <div class="form-group">
				    <label for="dhdrSearchDays">Last</label>
				    <input type="number" min="1" class="form-control" id="dhdrSearchDays" style="width:6em;"
				           ng-model="searchDays" ng-change="applySearchDays();">
				    <label for="dhdrSearchDays">days</label>
				  </div>

				  <button type="submit" class="btn btn-default" ng-click="callSearch();" ng-disabled="buttonDisabled" style="vertical-align: bottom;">Search</button>
				  <button type="submit" class="btn btn-default" ng-click="setSearchDateToAll();" ng-disabled="buttonDisabled" style="vertical-align: bottom;">Search All</button>
				</form>
		 		
		 	</div>
		 </div>
		 <div class="row" style="margin-bottom:2px;">
		 	<div class="col-xs-12" >
		 	<i>DHDR is being searched with HIN: {{demographic.hin}}  DOB: {{demographicDobText()}}</i>
	 	<br/>
	 	<!-- DHDR02.05: display the search period used alongside the results. -->
	 	<%-- Formatted in searchPeriodText() so the period reads in the same format as the tables
	 	     (DHDR03.06). A cleared start date sends no lower bound, so describe that rather than
	 	     render a dangling "  to <end>" range the search never actually used (BP6). --%>
	 	<i>Search period: {{searchPeriodText()}}</i>
	 	<%-- BP4: placed against the line it contradicts, because the two must be read together - the
	 	     period above is what was asked for, and this says the service reported using another. --%>
	 	<%-- The service stated it matched more resources than it delivered, and offered no further
	 	     page. What is on screen is part of the record, so say so where the period is stated
	 	     rather than leaving the result counts to imply completeness (DHDR05.01). --%>
	 	<div class="alert alert-danger" role="alert" style="margin:6px 0 0 0;" ng-if="resultShortfall">
	 		<strong>This list is incomplete.</strong>
	 		<div>The DHDR EHR Service reported {{resultShortfall.expected}} matching records but returned {{resultShortfall.received}}, with no further pages offered. Records are missing from the list below.</div>
	 	</div>
	 	<div class="alert alert-warning" role="alert" style="margin:6px 0 0 0;" ng-if="searchPeriodEchoMismatch">
	 		<strong>The DHDR EHR Service reported searching a different period.</strong>
	 		<div>Requested {{searchPeriodEchoMismatch.requested}}; the service reported using {{searchPeriodEchoMismatch.used}}. The events below may not cover the whole period shown above.</div>
	 	</div>
		 	</div>
		 </div>
		 
		 <div class="row" style="margin-bottom:10px;">
		 	<div class="col-xs-12" >
		 		<!-- DHDR03.03: non-intrusive disclaimer - always rendered, no dismiss control (v3.0 retired the DHDR15.03 suppress path). -->
		 		<div class="alert alert-info" role="alert">
		 				<i>Warning: Limited to Drug and Pharmacy Service Information available in the Digital Health Drug Repository (DHDR) EHR Service. 
		 					To ensure a Best Possible Medication History, please review this information with the patient/family and use other available sources of medication 
		 					information in addition to the DHDR EHR Service. For more details on the information available in the DHDR EHR Service, 
		 					please click <a class="alert-link" href="https://forms.mgcs.gov.on.ca/en/dataset/014-5056-87" target="_blank">https://forms.mgcs.gov.on.ca/en/dataset/014-5056-87</a>.</i>
		 		</div>
		 		
		 		<div ng-show="searching">
					Searching...
				</div>

		 		<%-- DHDR14.01: a PHI-free notice for every error/warning the EMR could not obtain an
		 		     OperationOutcome for - including a DHDR EHR Service that does not respond. Shows the
		 		     error code, description, severity and the date/time of the incident. No admin role is
		 		     required to see it, and no technical detail is exposed: that stays in the audit log. --%>
		 		<div ng-repeat="serviceError in serviceErrors" class="alert" ng-class="serviceErrorClass(serviceError)" role="alert">
		 			<strong>{{serviceError.httpMessage}}</strong>
		 			<div>{{noticeCodeLabel(serviceError.severity)}} {{serviceError.httpCode}} &middot; Severity: {{serviceError.severity}} &middot; {{serviceError.dateTime | date:'medium'}}</div>
		 			<div ng-if="serviceError.moreInformation">{{serviceError.moreInformation}}</div>
		 		</div>

		 		<div ng-repeat="outs in outcomes" >
		 			<div ng-repeat="issue in outs.issues"  class="alert" ng-class="issueClass(issue)" role="alert">
		 				{{issue.details.text}}
		 				<%-- DHDR14.01: an error received from the DHDR EHR Service must present its code,
		 				     description, severity and the date/time of the incident. The 'suppressed' issue is
		 				     the normal consent-block workflow (handled just below), not an error, so it is
		 				     excluded from the error line. --%>
		 				<div class="small" ng-if="issue.code !== 'suppressed'">{{noticeCodeLabel(issue.severity)}} {{dhdrCode(issue)}} &middot; Severity: {{issue.severity}} &middot; {{outs.receivedAt | date:'medium'}}</div>
		 				<span ng-if="issue.code === 'suppressed'">
		 					<!-- DHDR09.03: the EMR renders the mandated consent-block message itself (not reliant on the OperationOutcome text). -->
		 					<div>Access to Drug and Pharmacy Service information has been blocked by the patient.</div>
		 					<button type="button" class="btn btn-danger" ng-click="callConsentBlock();" ng-disabled="buttonDisabled">Temporary Consent Unblock</button>
		 					<button type="button" class="btn btn-default" ng-click="logOverrideStatus(outs.id, null, '<%=ConsentOverrideChoice.CANCELLED.getStoredValue()%>');">Cancel</button>
		 					<button type="button" class="btn btn-default" ng-click="logOverrideStatus(outs.id, null, '<%=ConsentOverrideChoice.REFUSED.getStoredValue()%>');">Refused</button>
		 				</span>
		 			</div>
		 		</div>
				
				<!-- DHDR09.05: refuse/cancel outcome message - shown briefly, then the viewer closes automatically. -->
				<div class="alert alert-warning" role="alert" ng-show="overrideResultMessage">{{overrideResultMessage}}</div>

				<!-- DHDR02.04: a valid search returning zero events must inform the user. Distinct from the
			     PCR patient-not-found / consent-suppressed cases, which surface via outcomes above. -->
			<div class="alert alert-warning" role="alert" ng-show="searchComplete && !searching && meds.length === 0 && services.length === 0 && serviceErrors.length === 0 && !hasBlockingOutcome()">
				No records found for the specified search date period.
			</div>

				<ul class="nav nav-pills nav-justified">
				  <li role="presentation" ng-class="currentView('summary')"><a ng-click="showSummary()">Summary</a></li>
				  <li role="presentation" ng-class="currentView('comp')"><a href="#" ng-click="showComp()">Comparative</a></li>  
				</ul>
			</div>
		 </div>
		<div ng-show="viewWhen('summary')">
			<div class="row">
				<div class="col-xs-12">
					<h6>DHDR Drugs <small><a ng-click="showHideFilter()">Filter</a></small></h6>
				</div>
			</div>
			<div class="row">
				<div class="col-xs-12">
					<div ng-show="showFilter()" >
						<form class="form-horizontal">
						  <div class="form-group">
							<label class="col-sm-2 control-label">Generic name</label>
							<div class="col-sm-10">
							  <input ng-model="searchtxt.genericName" type="text" placeholder="type to filter" class="form-control" list="sumDrugGenericName" />
							  <datalist id="sumDrugGenericName"><option ng-repeat="v in drugFilterValues.genericName" value="{{v}}"></datalist>
							</div>
						  </div>
						  <div class="form-group">
							<label class="col-sm-2 control-label">Brand name</label>
							<div class="col-sm-10">
							  <input ng-model="searchtxt.brandNameDisplay" type="text" placeholder="type to filter" class="form-control" list="sumDrugBrandName"/>
							  <datalist id="sumDrugBrandName"><option ng-repeat="v in drugFilterValues.brandName" value="{{v}}"></datalist>
							</div>
						  </div>
						  <div class="form-group">
							<label class="col-sm-2 control-label">Dispensed date</label>
							<div class="col-sm-10">
							  <input ng-model="searchtxt.whenPreparedDisplay" type="text" placeholder="type to filter" class="form-control" list="sumDrugDate"/>
							  <datalist id="sumDrugDate"><option ng-repeat="v in drugFilterValues.whenPrepared" value="{{v}}"></datalist>
							</div>
						  </div>
						  <div class="form-group">
							<label class="col-sm-2 control-label">Pharmacy Name</label>
							<div class="col-sm-10">
							  <input ng-model="searchtxt.dispensingPharmacy" type="text" placeholder="type to filter" class="form-control" list="sumDrugPharmacy" />
							  <datalist id="sumDrugPharmacy"><option ng-repeat="v in drugFilterValues.dispensingPharmacy" value="{{v}}"></datalist>
							</div>
						  </div>
						  <div class="form-group">
							<label class="col-sm-2 control-label">Prescriber Name</label>
							<div class="col-sm-10">
							  <input ng-model="searchtxt.prescriberName" type="text" placeholder="type to filter" class="form-control" list="sumDrugPrescriber" />
							  <datalist id="sumDrugPrescriber"><option ng-repeat="v in drugFilterValues.prescriberName" value="{{v}}"></datalist>
							</div>
						  </div>

						</form>
					</div>
				</div>
			</div>
			<div class="row">
				<div class="col-xs-12">
					<%-- DHDR03.01: the total is indicated for the view whether or not it returned anything.
					     Inside the table it was gated on the same ng-show as the table, so a patient with
					     pharmacy service events but no drug dispenses saw the heading and then nothing at
					     all - no table, no zero - while the both-empty case fell through to the "No records
					     found" message below. Held back until the walk has finished so it does not read 0
					     mid-search. --%>
					<div style="margin-bottom:4px;" ng-show="searchComplete && !searching">
						{{meds.length}} results returned
						<span ng-if="meds.length > 0">
							<button type="button" class="btn btn-default btn-xs" ng-click="toggleExpandAll()">{{expandAll ? 'Collapse All' : 'Expand All'}}</button>  <button type="button" class="btn btn-default btn-xs" ng-click="printSummary()">Print</button>
						</span>
					</div>
					<table class="table table-condensed table-striped table-bordered" ng-show="meds.length > 0">
						<thead>
							<tr>
								<th>
									<a ng-click="orderByField='whenPrepared'; reverseSort = !reverseSort">
										Dispensed Date
										<span ng-show="orderByField == 'whenPrepared'">
											<span ng-show="!reverseSort">^</span>
											<span ng-show="reverseSort">v</span>
										</span>
									</a>
								</th>
								<th>
									<a ng-click="orderByField='pickUpDate'; reverseSort = !reverseSort">
										Pickup Date
										<span ng-show="orderByField == 'pickUpDate'">
											<span ng-show="!reverseSort">^</span>
											<span ng-show="reverseSort">v</span>
										</span>
									</a>
								</th>
								<th>
									<a ng-click="orderByField='genericName'; reverseSort = !reverseSort">
										Generic
										<span ng-show="orderByField == 'genericName'">
											<span ng-show="!reverseSort">^</span>
											<span ng-show="reverseSort">v</span>
										</span>
									</a>
								</th>
								<th>
									<a ng-click="orderByField='brandName.display'; reverseSort = !reverseSort">
										Brand<span ng-show="orderByField == 'brandName.display'">
										<span ng-show="!reverseSort">^</span>
										<span ng-show="reverseSort">v</span>
									</span>
									</a>
								</th>
								<th>Therapeutic Class/Sub-class</th>
								<th>Dosage</th>
								<th>Frequency</th>
								<th>Quantity</th>
								<th>Supply / Refills / Qty Remaining</th>
								<th>
									<a ng-click="orderByField='prescriberLastname'; reverseSort = !reverseSort">
										Prescriber
										<span ng-show="orderByField == 'prescriberLastname'">
											<span ng-show="!reverseSort">^</span>
											<span ng-show="reverseSort">v</span>
										</span>
									</a>
								</th>
								<th>
									<a ng-click="orderByField='dispensingPharmacy'; reverseSort = !reverseSort">
										Pharmacy
										<span ng-show="orderByField == 'dispensingPharmacy'">
											<span ng-show="!reverseSort">^</span>
											<span ng-show="reverseSort">v</span>
										</span>
									</a>
								</th>
								<th>Rx Group Count</th>
							</tr>
						</thead>

						<tbody>
							<%-- DHDR03.04: while a filter is active the grouped heads give way to the full event
							     list, so a filter value the datalist offered always finds its events. The
							     offered values are the distinct ones the service returned, which includes
							     events sitting inside a collapsed group; matching only heads would let the
							     filter return nothing for a value the user had just picked from the list.
							     Grouping is unchanged as the default presentation (DHDR04.03). --%>
							<tr ng-repeat="med in ((expandAll || drugFilterActive()) ? meds : uniqMeds) | filter : searchtxt | orderBy:orderByField:reverseSort" ng-hide="!expandAll && !drugFilterActive() && med.hide" ng-class="getRowClass(med)">
								<th scope="row">{{med.whenPrepared | date}}</th>
								<th>{{med.pickUpDate | date}}</th>
								<td ng-click="getDetailView(med);">
									<a href="#">
										{{med.genericName}} <span ng-if="med.eventPatient.anyUnmatched" class="label label-warning" title="{{med.eventPatient.unmatchedSummary}}">Patient data differs</span> <span ng-if="med.statusNotCompleted" class="label label-danger" title="This dispense event is not in a completed state and may have been reversed or recorded in error.">{{med.status}}</span>
									</a>
								</td>
								<td>{{med.brandName.display}} {{med.dispensedDrugStrength}} {{med.drugDosageForm}}</td>
								<td>{{med | dhdrTherapeutic}}</td>
								<td>{{med.dose}} {{med.doseUnit}}</td>
								<td>{{med | dhdrFrequency}}</td>
								<td>{{med.dispensedQuantity}} {{med.dispensedQuantityUnit}}</td>
								<td>
									Est Days Supply:{{med.estimatedDaysSupply}}
									Refills Remaining:{{med.refillsRemaining}}
									Quantity Remaining:{{med.quantityRemaining}}
								</td>
								<td>{{med.prescriberLastname}}, {{med.prescriberFirstname}} <span ng-if="med.prescriberPhoneNumber">Tel:{{med.prescriberPhoneNumber}}</span></td>
								<td>{{med.dispensingPharmacy}} <span ng-if="med.dispensingPharmacyFaxNumber">Fax:{{med.dispensingPharmacyFaxNumber}}</span></td>
								<td ng-click="showGroupedMeds2(medsWithGroupedDups[med.getUniqVal()])"><span ng-if="med.headRecord && drugGroupSize(med) > 1"><a>{{drugGroupSize(med)}}</a></span><!-- {{med | json}}  --></td>

							</tr>
						</tbody>
					</table>
				</div>
			</div>
			<div class="row" >
				<div class="col-xs-12">
					<%-- services  --%>
					<h6>DHDR Pharmacy Service<small> <a ng-click="showHideServiceFilter()">Filter</a></small></h6>
				</div>
			</div>
			<div class="row">
				<div class="col-xs-12">
					<div ng-show="showServiceFilter()" >
						<form class="form-horizontal">
						  <div class="form-group">
							<label class="col-sm-2 control-label">Pharmacy Service Description</label>
							<div class="col-sm-10">
							  <input ng-model="searchServicetxt.genericName" type="text" placeholder="type to filter" class="form-control" list="sumSvcDescription" />
							  <datalist id="sumSvcDescription"><option ng-repeat="v in serviceFilterValues.genericName" value="{{v}}"></datalist>
							</div>
						  </div>
						  <div class="form-group">
							<label class="col-sm-2 control-label">Pharmacy Service Type</label>
							<div class="col-sm-10">
							  <input ng-model="searchServicetxt.brandNameDisplay" type="text" placeholder="type to filter" class="form-control" list="sumSvcType" />
							  <datalist id="sumSvcType"><option ng-repeat="v in serviceFilterValues.brandName" value="{{v}}"></datalist>
							</div>
						  </div>
						  <div class="form-group">
							<label class="col-sm-2 control-label">Therapeutic Class</label>
							<div class="col-sm-10">
							  <input ng-model="searchServicetxt.ahfsClass" type="text" placeholder="type to filter" class="form-control" list="sumSvcAhfs" />
							  <datalist id="sumSvcAhfs"><option ng-repeat="v in serviceFilterValues.ahfsClass" value="{{v}}"></datalist>
							</div>
						  </div>
						  <div class="form-group">
							<label class="col-sm-2 control-label">Dispensed Date</label>
							<div class="col-sm-10">
							  <input ng-model="searchServicetxt.whenPreparedDisplay" type="text" placeholder="type to filter" class="form-control" list="sumSvcDate" />
							  <datalist id="sumSvcDate"><option ng-repeat="v in serviceFilterValues.whenPrepared" value="{{v}}"></datalist>
							</div>
						  </div>
						  <div class="form-group">
							<label class="col-sm-2 control-label">Pharmacy Name</label>
							<div class="col-sm-10">
							  <input ng-model="searchServicetxt.dispensingPharmacy" type="text" placeholder="type to filter" class="form-control" list="sumSvcPharmacy" />
							  <datalist id="sumSvcPharmacy"><option ng-repeat="v in serviceFilterValues.dispensingPharmacy" value="{{v}}"></datalist>
							</div>
						  </div>
						  <div class="form-group">
							<label class="col-sm-2 control-label">Pharmacist Name</label>
							<div class="col-sm-10">
							  <input ng-model="searchServicetxt.pharmacistName" type="text" placeholder="type to filter" class="form-control" list="sumSvcPharmacist" />
							  <datalist id="sumSvcPharmacist"><option ng-repeat="v in serviceFilterValues.pharmacistName" value="{{v}}"></datalist>
							</div>
						  </div>
						  <div class="form-group">
							<label class="col-sm-2 control-label">Pharmacy Fax</label>
							<div class="col-sm-10">
							  <input ng-model="searchServicetxt.dispensingPharmacyFaxNumber" type="text" placeholder="type to filter" class="form-control" list="sumSvcFax" />
							  <datalist id="sumSvcFax"><option ng-repeat="v in serviceFilterValues.fax" value="{{v}}"></datalist>
							</div>
						  </div>
						</form>
					</div>
				</div>
			</div>
			<div class="row">
				<div class="col-xs-12">
					<%-- DHDR03.01: the total is indicated for the view whether or not it returned anything.
					     Inside the table it was gated on the same ng-show as the table, so a patient with
					     pharmacy service events but no drug dispenses saw the heading and then nothing at
					     all - no table, no zero - while the both-empty case fell through to the "No records
					     found" message below. Held back until the walk has finished so it does not read 0
					     mid-search. --%>
					<div style="margin-bottom:4px;" ng-show="searchComplete && !searching">
						{{services.length}} results returned
						<span ng-if="services.length > 0">
							<button type="button" class="btn btn-default btn-xs" ng-click="toggleExpandAllServices()">{{expandAllServices ? 'Collapse All' : 'Expand All'}}</button>  <button type="button" class="btn btn-default btn-xs" ng-click="printSummary()">Print</button>
						</span>
					</div>
					<table class="table table-condensed table-striped table-bordered" ng-show="services.length > 0">
						<thead>
							<tr>
								<th>
									<a ng-click="serviceOrderByField='whenPrepared'; serviceReverseSort = !serviceReverseSort">Dispensed Date <span ng-show="serviceOrderByField == 'whenPrepared'"><span ng-show="!serviceReverseSort">^</span><span ng-show="serviceReverseSort">v</span></span></a>
								</th>
								<th>
									<a ng-click="serviceOrderByField='whenHandedOver'; serviceReverseSort = !serviceReverseSort">Pickup Date <span ng-show="serviceOrderByField == 'whenHandedOver'"><span ng-show="!serviceReverseSort">^</span><span ng-show="serviceReverseSort">v</span></span></a>
								</th>
								<th>
									<a ng-click="serviceOrderByField='brandName.display'; serviceReverseSort = !serviceReverseSort">Pharmacy Service Type<span ng-show="serviceOrderByField == 'brandName.display'"><span ng-show="!serviceReverseSort">^</span><span ng-show="serviceReverseSort">v</span></span></a>
								</th>
								<th>
									<a ng-click="serviceOrderByField='genericName'; serviceReverseSort = !serviceReverseSort">Pharmacy Service Description<span ng-show="serviceOrderByField == 'genericName'"><span ng-show="!serviceReverseSort">^</span><span ng-show="serviceReverseSort">v</span></span></a>
								</th>
								<th>Rx Number</th>
								<th>Therapeutic Class/Sub-class</th>
								<th>
									<a ng-click="serviceOrderByField='dispensingPharmacy'; serviceReverseSort = !serviceReverseSort">Pharmacy Name<span ng-show="serviceOrderByField == 'dispensingPharmacy'"><span ng-show="!serviceReverseSort">^</span><span ng-show="serviceReverseSort">v</span></span></a>
								</th>
								<th>
									<a ng-click="serviceOrderByField='pharmacistLastname'; serviceReverseSort = !serviceReverseSort">Pharmacist<span ng-show="serviceOrderByField == 'pharmacistLastname'"><span ng-show="!serviceReverseSort">^</span><span ng-show="serviceReverseSort">v</span></span></a>
								</th>
								<th>
									<a ng-click="serviceOrderByField='dispensingPharmacyFaxNumber'; serviceReverseSort = !serviceReverseSort">Pharmacy Fax<span ng-show="serviceOrderByField == 'dispensingPharmacyFaxNumber'"><span ng-show="!serviceReverseSort">^</span><span ng-show="serviceReverseSort">v</span></span></a>
								</th>
								<th>Service Group Count</th>
							</tr>
						</thead>

						<tbody>
							<%-- DHDR03.04: see the drug table above - an active filter searches every returned
							     event, not just the group heads. --%>
							<tr ng-repeat="med in ((expandAllServices || serviceFilterActive()) ? services : uniqServices) | filter : searchServicetxt | orderBy:serviceOrderByField:serviceReverseSort" ng-hide="!expandAllServices && !serviceFilterActive() && med.hide" ng-class="getRowClass(med)">
								<th scope="row">{{med.whenPrepared | date}}</th>
								<td scope="row">{{med.whenHandedOver | date}}</td>
								<td>{{med.brandName.display}}</td>
								<td>{{med.genericName}} <span ng-if="med.eventPatient.anyUnmatched" class="label label-warning" title="{{med.eventPatient.unmatchedSummary}}">Patient data differs</span> <span ng-if="med.statusNotCompleted" class="label label-danger" title="This dispense event is not in a completed state and may have been reversed or recorded in error.">{{med.status}}</span></td>
								<td>{{med.rxNumber}}</td>
								<td scope="row">{{med | dhdrTherapeutic}}</td>
								<td>{{med.dispensingPharmacy}}</td>
								<td>{{med | dhdrPharmacist}}</td>
								<td>{{med.dispensingPharmacyFaxNumber}}</td>
								<td ng-click="showGroupedServices2(servicesWithGroupedDups[med.serviceGroupKey])"><span ng-if="med.headRecord && serviceGroupSize(med) > 1"><a>{{serviceGroupSize(med)}}</a></span><!-- {{med | json}}  --></td>

							</tr>
						</tbody>
					</table>
				</div>
			</div> <!--  end services -->
		</div>
		<div ng-show="viewWhen('comp')">	<!-- comparative view start -->
	 		<div class="row">
		 		<div class="col-xs-12" >
		 			<button type="button" class="btn btn-default btn-xs" ng-click="hideShowDhirPharma()"><span ng-if="hideShowDhirPharmaVal">Hide</span><span ng-if="!hideShowDhirPharmaVal">Show</span> DHDR PharmaServices</button>
		 			<button type="button" class="btn btn-default btn-xs" ng-click="hideShowDhirDrug()"><span ng-if="hideShowDhirDrugVal">Hide</span><span ng-if="!hideShowDhirDrugVal">Show</span> DHDR Drugs</button>
		 			<button type="button" class="btn btn-default btn-xs" ng-click="printComparative()">Print</button>
		 		</div>
		 	</div>
			<div class="row">
				<div class="col-xs-6">
					<div ng-if="hideShowDhirDrugVal">
						<h4>DHDR Drugs <small><a ng-click="showHideFilter()">Filter</a></small></h4>
						<h6>Medication Dispense</h6>
						<div ng-show="showFilter()">
							<form class="form-horizontal">
								<div class="form-group">
									<label class="col-sm-2 control-label">Brand name</label>
									<div class="col-sm-10">
										<input ng-model="searchtxt.brandNameDisplay" type="text" placeholder="type to filter" class="form-control" list="compDrugBrandName"/>
										<datalist id="compDrugBrandName"><option ng-repeat="v in drugFilterValues.brandName" value="{{v}}"></datalist>
									</div>
								</div>
								<div class="form-group">
									<label class="col-sm-2 control-label">Generic name</label>
									<div class="col-sm-10">
										<input ng-model="searchtxt.genericName" type="text" placeholder="type to filter" class="form-control" list="compDrugGenericName"/>
										<datalist id="compDrugGenericName"><option ng-repeat="v in drugFilterValues.genericName" value="{{v}}"></datalist>
									</div>
								</div>
								<div class="form-group">
									<label class="col-sm-2 control-label">Dispensed date</label>
									<div class="col-sm-10">
										<input ng-model="searchtxt.whenPreparedDisplay" type="text" placeholder="type to filter" class="form-control" list="compDrugDate"/>
										<datalist id="compDrugDate"><option ng-repeat="v in drugFilterValues.whenPrepared" value="{{v}}"></datalist>
									</div>
								</div>
								<div class="form-group">
									<label class="col-sm-2 control-label">Pharmacy Name</label>
									<div class="col-sm-10">
										<input ng-model="searchtxt.dispensingPharmacy" type="text" placeholder="type to filter" class="form-control" list="compDrugPharmacy" />
										<datalist id="compDrugPharmacy"><option ng-repeat="v in drugFilterValues.dispensingPharmacy" value="{{v}}"></datalist>
									</div>
								</div>
								<div class="form-group">
									<label class="col-sm-2 control-label">Prescriber Name</label>
									<div class="col-sm-10">
										<input ng-model="searchtxt.prescriberName" type="text" placeholder="type to filter" class="form-control" list="compDrugPrescriber" />
										<datalist id="compDrugPrescriber"><option ng-repeat="v in drugFilterValues.prescriberName" value="{{v}}"></datalist>
									</div>
								</div>

							</form>
						</div>
					</div>
				</div>
				<div class="col-xs-6">
					<h4>EMR Local Data</h4>
				</div>
			</div>
	 		<div class="row">
		 		<div class="col-xs-6 table-overflow-x ">
			 		<div ng-if="hideShowDhirDrugVal">
						<table class="table table-condensed table-striped table-bordered" >
				 			<thead>
								<tr>
									<td colspan="6">
										{{meds.length}} results returned
									</td>
								</tr>
				 				<tr> 
				 					<th>
										<a ng-click="orderByField='whenPrepared'; reverseSort = !reverseSort">
											Dispensed Date
											<span ng-show="orderByField == 'whenPrepared'">
												<span ng-show="!reverseSort">^</span>
												<span ng-show="reverseSort">v</span>
											</span>
										</a>
									</th>
									<th>
										<a ng-click="orderByField='pickUpDate'; reverseSort = !reverseSort">
											Pickup Date
											<span ng-show="orderByField == 'pickUpDate'">
												<span ng-show="!reverseSort">^</span>
												<span ng-show="reverseSort">v</span>
											</span>
										</a>
									</th>
									<th>
										<a ng-click="orderByField='brandName.display'; reverseSort = !reverseSort">
											Brand
											<span ng-show="orderByField == 'brandName.display'">
												<span ng-show="!reverseSort">^</span>
												<span ng-show="reverseSort">v</span>
											</span>
										</a>
										/
										<a ng-click="orderByField='genericName'; reverseSort = !reverseSort">
											Generic
											<span ng-show="orderByField == 'genericName'">
												<span ng-show="!reverseSort">^</span>
												<span ng-show="reverseSort">v</span>
											</span>
										</a>
									</th>
									<th>Therapeutic Class/Sub-class</th>
									<th>Quantity</th>
									<th>Dosage</th>
									<th>Frequency</th>
				 					<th>Supply / Refills / Qty Remaining</th>
									<th>
										<a ng-click="orderByField='prescriberLastname'; reverseSort = !reverseSort">
											Prescriber
											<span ng-show="orderByField == 'prescriberLastname'">
												<span ng-show="!reverseSort">^</span>
												<span ng-show="reverseSort">v</span>
											</span>
										</a>
									</th>
									<th>
										<a ng-click="orderByField='dispensingPharmacy'; reverseSort = !reverseSort">
											Pharmacy
											<span ng-show="orderByField == 'dispensingPharmacy'">
												<span ng-show="!reverseSort">^</span>
												<span ng-show="reverseSort">v</span>
											</span>
										</a>
									</th>
								</tr> 
							</thead> 
				 			<tbody> 
				 				<tr ng-repeat="med in meds | filter: searchtxt | orderBy: orderByField:reverseSort" ng-class="getRowClass(med)">
				 					<th scope="row">{{med.whenPrepared | date}}</th> 
				 					<th>{{med.pickUpDate | date}}</th>
									<td ng-click="getDetailView(med);">
										<a href="#">
											{{med.brandName.display}} {{med.dispensedDrugStrength}} {{med.drugDosageForm}} ({{med.genericName}}) <span ng-if="med.eventPatient.anyUnmatched" class="label label-warning" title="{{med.eventPatient.unmatchedSummary}}">Patient data differs</span> <span ng-if="med.statusNotCompleted" class="label label-danger" title="This dispense event is not in a completed state and may have been reversed or recorded in error.">{{med.status}}</span>
										</a>
									</td>
									<td>{{med | dhdrTherapeutic}}</td>
				 					<td>{{med.dispensedQuantity}} {{med.dispensedQuantityUnit}}</td>
									<td>{{med.dose}}  {{med.doseUnit}}</td>
									<td>{{med | dhdrFrequency}}</td>
				 					<td>
				 						Est Days Supply:{{med.estimatedDaysSupply}}
				 						Refills Remaining:{{med.refillsRemaining}}
										Quantity Remaining:{{med.quantityRemaining}}
				 					</td>
				 					<td>{{med.prescriberLastname}}, {{med.prescriberFirstname}} <span ng-if="med.prescriberPhoneNumber">Tel:{{med.prescriberPhoneNumber}}</span></td>
				 					<td>{{med.dispensingPharmacy}} <span ng-if="med.dispensingPharmacyFaxNumber">Fax:{{med.dispensingPharmacyFaxNumber}}</span></td>
				 					
				 					
				 				</tr>
				 			</tbody> 
				 		</table>
			 		</div>
		 		</div>
		 		<div class="col-xs-6 table-overflow-x " >
		 			<div ng-include="'emrMedsTable.html'"></div>
		 		</div>
		 	</div>
			<div class="row">
				<div class="col-xs-6 table-overflow-x ">
					<div ng-if="hideShowDhirPharmaVal">
						<h6>DHDR Pharmacy Service <small><a ng-click="showHideServiceFilter()">Filter</a></small></h6>
						<div ng-show="showServiceFilter()" >
							<form class="form-horizontal">
								<div class="form-group">
									<label class="col-sm-2 control-label">Pharmacy Service Description</label>
									<div class="col-sm-10">
										<input ng-model="searchServicetxt.genericName" type="text" placeholder="type to filter" class="form-control" list="compSvcDescription" />
										<datalist id="compSvcDescription"><option ng-repeat="v in serviceFilterValues.genericName" value="{{v}}"></datalist>
									</div>
								</div>
								<div class="form-group">
									<label class="col-sm-2 control-label">Pharmacy Service Type</label>
									<div class="col-sm-10">
										<input ng-model="searchServicetxt.brandNameDisplay" type="text" placeholder="type to filter" class="form-control" list="compSvcType" />
										<datalist id="compSvcType"><option ng-repeat="v in serviceFilterValues.brandName" value="{{v}}"></datalist>
									</div>
								</div>
								<div class="form-group">
									<label class="col-sm-2 control-label">Therapeutic Class</label>
									<div class="col-sm-10">
										<input ng-model="searchServicetxt.ahfsClass" type="text" placeholder="type to filter" class="form-control" list="compSvcAhfs" />
										<datalist id="compSvcAhfs"><option ng-repeat="v in serviceFilterValues.ahfsClass" value="{{v}}"></datalist>
									</div>
								</div>
								<div class="form-group">
									<label class="col-sm-2 control-label">Dispensed Date</label>
									<div class="col-sm-10">
										<input ng-model="searchServicetxt.whenPreparedDisplay" type="text" placeholder="type to filter" class="form-control" list="compSvcDate" />
										<datalist id="compSvcDate"><option ng-repeat="v in serviceFilterValues.whenPrepared" value="{{v}}"></datalist>
									</div>
								</div>
								<div class="form-group">
									<label class="col-sm-2 control-label">Pharmacy Name</label>
									<div class="col-sm-10">
										<input ng-model="searchServicetxt.dispensingPharmacy" type="text" placeholder="type to filter" class="form-control" list="compSvcPharmacy" />
										<datalist id="compSvcPharmacy"><option ng-repeat="v in serviceFilterValues.dispensingPharmacy" value="{{v}}"></datalist>
									</div>
								</div>
								<div class="form-group">
									<label class="col-sm-2 control-label">Pharmacist Name</label>
									<div class="col-sm-10">
										<input ng-model="searchServicetxt.pharmacistName" type="text" placeholder="type to filter" class="form-control" list="compSvcPharmacist" />
										<datalist id="compSvcPharmacist"><option ng-repeat="v in serviceFilterValues.pharmacistName" value="{{v}}"></datalist>
									</div>
								</div>
							</form>
						</div>
						<%-- DHDR03.01: see the summary view - the count must survive an empty section. --%>
						<div style="margin-bottom:4px;" ng-show="searchComplete && !searching">
							{{services.length}} results returned
						</div>
						<table class="table table-condensed table-striped table-bordered" ng-show="services.length > 0">
							<thead>
							<tr>
								<th>
									<a ng-click="serviceOrderByField='whenPrepared'; serviceReverseSort = !serviceReverseSort">
										Dispensed Date
										<span ng-show="serviceOrderByField == 'whenPrepared'">
												 <span ng-show="!serviceReverseSort">&#9650;</span>
												 <span ng-show="serviceReverseSort">&#9660;</span>
											 </span>
									</a>
								</th>
								<th>
									<a ng-click="serviceOrderByField='whenHandedOver'; serviceReverseSort = !serviceReverseSort">
										Pickup Date
										<span ng-show="serviceOrderByField == 'whenHandedOver'">
												 <span ng-show="!serviceReverseSort">&#9650;</span>
												 <span ng-show="serviceReverseSort">&#9660;</span>
											 </span>
									</a>
								</th>
								<th>
									<a ng-click="serviceOrderByField='brandName.display'; serviceReverseSort = !serviceReverseSort">
										Pharmacy Service Type
										<span ng-show="serviceOrderByField == 'brandName.display'">
												 <span ng-show="!serviceReverseSort">&#9650;</span>
												 <span ng-show="serviceReverseSort">&#9660;</span>
											 </span>
									</a>
								</th>
								<th>
									<a ng-click="serviceOrderByField='genericName'; serviceReverseSort = !serviceReverseSort">
										Pharmacy Service Description
										<span ng-show="serviceOrderByField == 'genericName'">
												 <span ng-show="!serviceReverseSort">&#9650;</span>
												 <span ng-show="serviceReverseSort">&#9660;</span>
											 </span>
									</a>
								</th>
								<th>Rx Number</th>
								<th>Therapeutic Class/Sub-class</th>
								<th>
									<a ng-click="serviceOrderByField='dispensingPharmacy'; serviceReverseSort = !serviceReverseSort">
										Pharmacy Name<span ng-show="serviceOrderByField == 'dispensingPharmacy'">
											 <span ng-show="!serviceReverseSort">&#9650;</span>
											 <span ng-show="serviceReverseSort">&#9660;</span>
										 </span>
									</a>
								</th>
								<th>
									<a ng-click="serviceOrderByField='pharmacistLastname'; serviceReverseSort = !serviceReverseSort">
										Pharmacist<span ng-show="serviceOrderByField == 'pharmacistLastname'">
											 <span ng-show="!serviceReverseSort">&#9650;</span>
											 <span ng-show="serviceReverseSort">&#9660;</span>
										 </span>
									</a>
								</th>
							</tr>
							</thead>

							<tbody>
							<tr ng-repeat="med in services | filter : searchServicetxt | orderBy:serviceOrderByField:serviceReverseSort">
								<th scope="row">{{med.whenPrepared | date}}</th>
								<td scope="row">{{med.whenHandedOver | date}}</td>
								<td>{{med.brandName.display}}</td>
								<td>{{med.genericName}} <span ng-if="med.eventPatient.anyUnmatched" class="label label-warning" title="{{med.eventPatient.unmatchedSummary}}">Patient data differs</span> <span ng-if="med.statusNotCompleted" class="label label-danger" title="This dispense event is not in a completed state and may have been reversed or recorded in error.">{{med.status}}</span></td>
								<td>{{med.rxNumber}}</td>
								<td>{{med | dhdrTherapeutic}}</td>
								<td>{{med.dispensingPharmacy}} - Fax:{{med.dispensingPharmacyFaxNumber}}</td>
								<td>{{med | dhdrPharmacist}}</td>

							</tr>
							</tbody>
						</table>
					</div>
				</div>
				<div class="col-xs-6 table-overflow-x ">
					<h4>EMR Local Data</h4>
					<%-- DHDR08.01(a): the EMR side carries the EMR's medication records (per DHDR05.02),
					     the same table the drug Comparative shows. --%>
					<div ng-include="'emrMedsTable.html'"></div>
				</div>
			</div>
	 	</div> <!-- comparitive view end -->
	</div>
	<%-- DHDR05.02: the EMR medication records. Rendered in both the drug Comparative and
	     the Pharmacy Service Comparative (DHDR08.01a), so it lives in one template. --%>
	<script type="text/ng-template" id="emrMedsTable.html">
		 			<%-- Rendered even when the patient has no EMR medications: the Comparative view is a
		 			     side-by-side (DHDR05.01 / DHDR08.01a), and hiding this table outright left an empty
		 			     column that reads as a failure to load rather than as "nothing on file". Keeping the
		 			     header also keeps the result count visible, which is 0 rather than absent. --%>
		 			<table class="table table-condensed table-striped table-bordered">
		 			   	<thead>
							<tr>
								<td colspan="12">
									{{compLocalMeds.length}} results returned
								</td>
							</tr>
			 				<tr>
			 					<%-- DHDR03.05: sortable by dispensed date and generic name, with the
			 					     active element indicated, as on the DHDR tables. --%>
			 					<th>
			 						<a ng-click="emrSort.field='rxDate'; emrSort.reverse = !emrSort.reverse">
			 							Start Date
			 							<span ng-show="emrSort.field == 'rxDate'">
			 								<span ng-show="!emrSort.reverse">^</span>
			 								<span ng-show="emrSort.reverse">v</span>
			 							</span>
			 						</a>
			 					</th>
			 					<th>
			 						<a ng-click="emrSort.field='medication'; emrSort.reverse = !emrSort.reverse">
			 							Medication
			 							<span ng-show="emrSort.field == 'medication'">
			 								<span ng-show="!emrSort.reverse">^</span>
			 								<span ng-show="emrSort.reverse">v</span>
			 							</span>
			 						</a>
			 					</th>
			 					<th>Strength</th>
			 					<th>Dosage</th>
			 					<th>Frequency</th>
			 					<th>Prescriber</th>
			 					<th>DIN</th>
			 					<th>Qty / Duration</th>
			 					<th>Refills</th>
							</tr> 
						</thead> 
		 				<tbody> 
			 				<tr ng-repeat="med in compLocalMeds | orderBy: emrSortValue : emrSort.reverse">
			 					<th scope="row" class="startDate">{{med.rxDate | date}}</th>
			 					<td>{{med.genericName || med.brandName || med.customName}}</td>
			 					<td>{{med.strength}} {{med.strengthUnit}}</td>
			 					<%-- DHDR05.02(e) value + unit of measure; see emrDoseUnit(). --%>
			 					<td><span ng-if="med.takeMin">{{med.takeMin}}<span ng-if="med.takeMax && med.takeMax !== med.takeMin"> - {{med.takeMax}}</span><span ng-if="emrDoseUnit(med)"> {{emrDoseUnit(med)}}</span></span></td>
			 					<td>{{med.frequency}}</td>
			 					<td>{{med.providerName}}</td>
			 					<td>{{med.regionalIdentifier}}</td>
			 					<td>{{med.quantity}}<span ng-if="med.duration"> / {{med.duration}} {{med.durationUnit}}</span></td>
			 					<%-- DHDR05.02(i): refill duration is always days, per the Rx screen's
			 					     own validation, so the unit is a fixed label. --%>
			 					<td>{{med.repeats}}<span ng-if="med.refillQuantity"> ({{med.refillQuantity}}<span ng-if="med.refillDuration"> / {{med.refillDuration}} days</span>)</span></td>
			 				</tr>
			 				<tr ng-if="compLocalMeds.length === 0">
			 					<td colspan="12">No medications are recorded in the EMR for this patient.</td>
			 				</tr>
			 			</tbody>
		 			</table>
	</script>
	<script type="text/ng-template" id="myModalContent.html">
        <div class="modal-header">
			<div class="row">
				<div class="col-md-6">
					<h3 class="modal-title" id="modal-title">{{med.genericName}} - {{med.whenPrepared | date}}</h3>
				</div>
				<div class="col-md-6 text-right">
					<button class="btn btn-primary" type="button" ng-click="printDetail()">Print</button>
					<button class="btn btn-warning" type="button" ng-click="cancel()">Cancel</button>
				</div>
			</div>
        </div>
        <div class="modal-body" id="modal-body">
            <%-- A print failure has to be reported inside the modal: this template runs under
                 ModalInstanceCtrl, which cannot reach the controller's serviceErrors list. --%>
            <div class="alert alert-danger" role="alert" ng-if="printError">{{printError}}</div>
            <div class="md-dialog-content" id="dialogContentApptProvider">
            
            <div class="row">
                <div class="col-xs-11">



                    <table class="table table-bordered table striped" >
                       
						<tr> 
		 					<th>Dispensed Date</th> 
							<th scope="row">{{med.whenPrepared | date}}</th>
						</tr>
						<%-- MedicationDispense.status is 1..1 and mustSupport, so the Detailed view states it
						     always, not only when abnormal. Shown conditionally, an absent row is ambiguous:
						     a reader cannot tell "this dispense is valid" from "this EMR does not display
						     status". Only the styling is conditional - a normal status must not compete for
						     attention with one that invalidates the event. --%>
						<tr>
							<th>Dispense Status</th>
							<td>
								<span ng-if="!med.statusNotCompleted">{{med.status}}</span>
								<span ng-if="med.statusNotCompleted">
									<span class="label label-danger">{{med.status}}</span>
									Not a completed dispense - may have been reversed or recorded in error.
								</span>
							</td>
						</tr>
						<tr>
							<th>Pickup Date</th>
							<th scope="row">{{med.pickUpDate | date}}</th>
						</tr>
						<tr>
		 					<th>Generic</th> 
							<td>{{med.genericName}}</td>
</tr>
						<tr>
		 					<th>Brand</th>
							<td>{{med.brandName.display}}</td>
 </tr>
						<tr>
		 					<th>DIN/PIN</th>
							<td>{{med.brandName.code}}</td>
 						</tr>
						<%-- BP17: not maintained for drug dispenses, so these rendered as two empty rows on
						     every drug event. Rendered only when a value is actually present - the same
						     treatment DHDRPrint.printDetail already gives them. --%>
						<tr ng-if="med.ahfsClass">
		 					<th>Therapeutic Class</th>
							<td>{{med.ahfsClass}}</td>
 						</tr>
						<tr ng-if="med.ahfsSubClass">
		 					<th>Therapeutic Sub-Class</th>
							<td>{{med.ahfsSubClass}}</td>
 						</tr>
						<tr>
		 					<th>Rx Number</th>
							<td>{{med.rxNumber}}</td>
 						</tr>
						<tr>
							<th>Medical Condition/Reason for Use</th>
							<td>
								<%-- DHDR06.01(c): each reasonCode is a CodeableConcept, so the code and display
							     live on its codings - iterating the concept itself walks its properties
							     instead and yields nothing. Falls back to the concept's plain text when it
							     carries no coding. --%>
								<div ng-repeat="rcode in med.reasonCode">
									<div ng-repeat="reason in rcode.coding">({{reason.code}}) -- {{reason.display}}</div>
									<div ng-if="!rcode.coding.length && rcode.text">{{rcode.text}}</div>
								</div>
							</td>
						</tr>

						<tr>
		 					<th>Strength</th>	
							<td>{{med.dispensedDrugStrength}}</td>
						</tr>
						<tr>
		 					<th>Dosage Form</th>
							<td>{{med.drugDosageForm}}</td>
						</tr>
						<tr>
							<th>Dosage</th>
							<td>{{med.dose}} {{med.doseUnit}}</td>
						</tr>
						<tr>
							<th>Frequency</th>
							<td>{{med | dhdrFrequency}}</td>
						</tr>
						<tr>

							<th>Quantity</th>
							<td>{{med.dispensedQuantity}} {{med.dispensedQuantityUnit}}</td>
						</tr>
						<tr>
		 					<th>Est Days Supply</th>
							<td>{{med.estimatedDaysSupply}}</td>
						</tr>

						<tr>
		 					<th>Refills Remaining</th>
							<td>{{med.refillsRemaining}}</td>
						</tr>
						<tr>
		 					<th>Quantity Remaining</th>
							<td>{{med.quantityRemaining}}</td>
						</tr>
						<tr>
							<th>Patient Name</th>
							<td>
								{{med.eventPatient.lastName}}, {{med.eventPatient.firstName}}
								<span ng-show="med.eventPatient.nameUnmatched"> (UNMATCHED)</span>
							</td>
						</tr>
						<tr>
							<th>Patient Gender</th>
							<td>
								{{med.eventPatient.gender}}
								<span ng-show="med.eventPatient.genderUnmatched"> (UNMATCHED)</span>
							</td>
						</tr>
						<tr>
							<th>Patient DOB</th>
							<td>
								{{med.eventPatient.dob | date}}
								<span ng-show="med.eventPatient.dobUnmatched"> (UNMATCHED)</span>
							</td>
						</tr>
						<tr>
							<th>Patient HIN</th>
							<td>
								{{med.eventPatient.hin}}
								<span ng-show="med.eventPatient.hinUnmatched"> (UNMATCHED)</span>
							</td>
						</tr>
						<tr>
		 					<th>Prescriber</th>
							<td>{{med.prescriberLastname}}, {{med.prescriberFirstname}} ({{med.prescriberLicenceNumber.value}})</td>
						</tr>
						<tr>
		 					<th>Prescriber ID</th>
							<td title="{{getLicence(med.prescriberLicenceNumber.system)}}">{{getLicenceMnemonic(med.prescriberLicenceNumber.system)}} ({{med.prescriberLicenceNumber.value}})</td>
						</tr>
						<tr>
		 					<th>Prescriber Phone</th>
							<td>{{med.prescriberPhoneNumber}}</td>
						</tr>	
						<tr>
		 					<th>Pharmacy</th>
							<td>{{med.dispensingPharmacy}}</td>
						</tr>
						<tr>
		 					<th>Pharmacy Fax</th>
							<td>{{med.dispensingPharmacyFaxNumber}}</td>
						</tr> 
						<tr>
		 					<th>Pharmacy Phone</th>
							<td>{{med.dispensingPharmacyPhoneNumber}}</td> 
						</tr> 
						<tr>
							<th>Pharmacist</th>
							<td>{{med | dhdrPharmacist}}</td>
						</tr>
					
<!-- tr>
<td colspan=2>
<pre>{{med}}</pre>
</td>
</tr -->
                        
                    </table>
                </div>
            </div>
        </div>
        </div>
    </script>
    <script type="text/ng-template" id="pcoi.html">
<div class="modal-body" id="modal-body">
		<a ng-if="showUntilLoaded" ng-click="reload()"> Failed to load? click here</a>	
		<a ng-if="viewletNotResponding" ng-click="cancel()"> Viewlet not responding? click here</a>	
		<%-- allow-modals is load-bearing, not incidental: window.print() is gated by the sandbox's
		     modals flag, so dropping it would stop the viewlet printing to a local printer and
		     break DHDR10.04. --%>
		<iframe id="pcoi-frame" class="pcoi-frame" src="{{pcoiUrl}}" sandbox="allow-forms allow-scripts allow-same-origin allow-modals"
				width="540" height="600" ng-onload="loadingResult(state,message)"></iframe>
		
<div>
	</script>
	<script type="text/ng-template" id="drugDupsContent.html">
        <div class="modal-header">
            <h3 class="modal-title" id="modal-title">{{med.genericName}} - {{med.whenPrepared | date}}</h3>
        </div>
        <div class="modal-body" id="modal-body">
            <div class="md-dialog-content" id="dialogContentApptProvider">
            
            <div class="row">
                <div class="col-xs-12">
					<table class="table table-condensed table-striped table-bordered" ng-show="meds.length > 0"> 		 			
		 			<thead> 
		 				<tr> 
		 					<th>Dispensed Date</th>
							<th>Pickup Date</th>
		 					<th>Generic</th> 
		 					<th>Brand</th> 
		 					<th>Strength</th>
		 					<th>Dosage Form</th>
							<th>Dosage</th>
							<th>Frequency</th>
		 					<th>Quantity</th>
		 					<th>Est Days Supply</th>
		 					<th>Refills Remaining</th>
							<th>Quantity Remaining</th>
		 					<th>Prescriber</th>
		 					<th>Pharmacy</th>
						</tr> 
					</thead> 
					
		 			<tbody> 
		 				<%-- Order comes from the group itself, which is sorted most-recent-first when the
		 				     search finishes (DHDR04.02). An orderBy here cannot do it: this modal runs on its
		 				     own scope, so a sort field named from the main controller reads as undefined and
		 				     AngularJS then returns the list untouched. --%>
		 				<tr ng-repeat="med in meds | filter : searchtxt">
		 					<th scope="row">{{med.whenPrepared | date}}</th>
							<th>{{med.pickUpDate | date}}</th>
		 					<td ng-click="getDetailView(med);">{{med.genericName}} <span ng-if="med.eventPatient.anyUnmatched" class="label label-warning" title="{{med.eventPatient.unmatchedSummary}}">Patient data differs</span> <span ng-if="med.statusNotCompleted" class="label label-danger" title="This dispense event is not in a completed state and may have been reversed or recorded in error.">{{med.status}}</span></td>
		 					<td>{{med.brandName.display}}</td>
		 					<td>{{med.dispensedDrugStrength}}</td>
		 					<td>{{med.drugDosageForm}}</td>
							<td>{{med.dose}} {{med.doseUnit}}</td>
							<td>{{med.frequency}}</td>
		 					<td>{{med.dispensedQuantity}} {{med.dispensedQuantityUnit}}</td>
		 					<td>{{med.estimatedDaysSupply}}</td>
		 					<td>{{med.refillsRemaining}}</td>
							<td>{{med.quantityRemaining}}</td>
		 					<td>{{med.prescriberLastname}}, {{med.prescriberFirstname}} <span ng-if="med.prescriberPhoneNumber">Tel:{{med.prescriberPhoneNumber}}</span></td>
		 					<td>{{med.dispensingPharmacy}} <span ng-if="med.dispensingPharmacyFaxNumber">Fax:{{med.dispensingPharmacyFaxNumber}}</span></td>
		 				</tr> 
		 			</tbody> 
		 		</table>
                </div>
            </div>
        </div>
        </div>
        <div class="modal-footer">
            <button class="btn btn-warning" type="button" ng-click="cancel()">Cancel</button>
        </div>
    </script>
	<script type="text/ng-template" id="pharmaDupsContent.html">
        <div class="modal-header">
            <h3 class="modal-title" id="modal-title">{{med.genericName}} - {{med.whenPrepared | date}}</h3>
        </div>
        <div class="modal-body" id="modal-body">
            <div class="md-dialog-content" id="dialogContentApptProvider">
            
            <div class="row">
                <div class="col-xs-12">
					<table class="table table-condensed table-striped table-bordered" ng-show="services.length > 0"> 		 			
		 			<thead> 
		 				<tr> 
		 					<th>Dispensed Date </th> 
		 					<th>Pickup Date</th> 
		 					<th>Pharmacy Service Type</th> 
		 					<th>Pharmacy Service Description</th> 
							<th>Pharmacy Name</th>
		 					<th>Pharmacist</th>
		 					<th>Pharmacy Fax</th>
						</tr> 
					</thead> 
					 
		 			<tbody> 
		 				<%-- Ordered by the group, as in the drug modal above (DHDR07.02). --%>
		 				<tr ng-repeat="med in services | filter : searchServicetxt" >
		 					<th scope="row">{{med.whenPrepared | date}}</th> 
		 					<td scope="row">{{med.whenHandedOver | date}}</td>
		 					<td>{{med.brandName.display}}</td> 
		 					<td>{{med.genericName}} </td>
		 					<td>{{med.dispensingPharmacy}}</td>
		 					<td>{{med | dhdrPharmacist}}</td>
		 					<td>{{med.dispensingPharmacyFaxNumber}}</td>
		 				</tr> 
		 			</tbody> 
		 		</table>
                </div>
            </div>
        </div>
        </div>
        <div class="modal-footer">
            <button class="btn btn-warning" type="button" ng-click="cancel()">Cancel</button>
        </div>
    </script>
	<script>
		var app = angular.module("dhdrView", ['demographicServices','providerServices','dhdrServices','oscarFilters','ui.bootstrap','rxServices']);
		
		//app.config(['$locationProvider'],function($locationProvider ) {
		//	$locationProvider.html5Mode(true);
		//});

		// DHDR04.01: the frequency cell is built from four parts, none of which the DHDR service
		// is obliged to send - dosageInstruction is optional under the consumer profile, and the
		// IG's own pharmacy-service example carries none. Interpolating the parts directly left a
		// record with no dosage instruction reading "every  -", which looks like a frequency
		// rather than like the absence of one. Composed only when there is something to compose.
		// A filter rather than a scope method because the detail view renders in its own modal
		// scope, where a helper hung off the main controller would silently resolve to nothing.
		app.filter('dhdrFrequency', function(){
			return function(med){
				if(!med){ return ""; }
				var parts = [med.frequency, med.period, med.periodMax, med.periodUnit];
				var present = parts.some(function(p){
					return p !== undefined && p !== null && String(p).trim() !== "";
				});
				if(!present){ return ""; }
				// Same shape as before for a record that does carry the parts, so nothing that
				// already printed changes.
				return (med.frequency || "") + " every " + (med.period || "")
						+ " - " + (med.periodMax || "") + " " + (med.periodUnit || "");
			};
		});


		// Name only. DHDR04.01, DHDR06.01(g) and DHDR07.01(g) each specify the pharmacist as
		// [Practitioner.name.given], [Practitioner.name.family] and nothing more, and "pharmacist"
		// appears nowhere else in the specification - so the licence number the detail view used to
		// append is not an element of any view. The prescriber does get an ID under DHDR06.01(e)/(f);
		// the pharmacist deliberately does not.
		//
		// Both separators are conditional: four cells composed this inline and all four dropped the
		// guards, so a service whose pharmacist carries no OCP licence read as ", ". A filter for the
		// same reason as dhdrFrequency above: two of the four cells render in their own modal scope.
		// BP17 says the DHDR maintains Therapeutic Class / Sub-Class for pharmacy services and not for
		// drug dispenses, and the drug tables duly rendered a bare "/" for every row. Removing the column
		// was wrong, though: the IG's own examples DO carry AHFS codings on drug dispenses, so the column
		// hides real data whenever the service supplies it. Render the separator only when there is
		// something on both sides of it, which is right in either case.
		app.filter('dhdrTherapeutic', function(){
			return function(med){
				if(!med){ return ""; }
				var cls = (med.ahfsClass || "").trim();
				var sub = (med.ahfsSubClass || "").trim();
				return cls ? (sub ? cls + " / " + sub : cls) : sub;
			};
		});

		app.filter('dhdrPharmacist', function(){
			return function(med){
				if(!med){ return ""; }
				var last = (med.pharmacistLastname || "").trim();
				var first = (med.pharmacistFirstname || "").trim();
				return last ? (first ? last + ", " + first : last) : first;
			};
		});


		app.controller("dhdrView", function($scope,demographicService,providerService,dhdrService,rxService,$location,$window,$modal,$http,$filter,$timeout) {

			$scope.demographicNo = $location.search().demographic_no;
			
			//if($scope.demographicNo == undefined){
				var urlParams = new URLSearchParams(window.location.search);
				$scope.demographicNo = urlParams.get("demographic_no");
			//}
			$scope.demographic = {};
			activeProvidersHash = {};
			$scope.meds = [];
			$scope.uniqMeds = [];
			$scope.uniqServices = [];
			$scope.services = [];
			// DHDR03.04: distinct values offered by the filter datalists. Empty until a search lands.
			$scope.drugFilterValues = {};
			$scope.serviceFilterValues = {};
			$scope.outcomes = [];
			// DHDR14.01: notices about the DHDR EHR Service itself (unreachable, unresponsive,
			// expired session). Distinct from outcomes, which are the issues the service reported.
			$scope.serviceErrors = [];
			// DHDR02.03: the provider's default search window, resolved server-side. Read-only here:
			// editing the range on screen must never write back into the default.
			defaultDaysToSearch = <%= defaultSearchDays %>;
			// The widest window the range arithmetic below can express. Past it the subtraction
			// yields an Invalid Date and the search loses its start bound silently.
			maxDaysToSearch = <%= maxSearchDays %>;
			// The bounds are yyyy-MM-dd strings, never Dates: a Date is an instant, and converting a
			// zone-less calendar date to one shifts it a day behind UTC. Convert once, here.
			asSearchDate = function(d){ return $filter('date')(d, "yyyy-MM-dd"); };
			// Joins the parts directly - new Date(y+"-"+m+"-"+d) reads as ISO, so UTC midnight.
			// Null when a part is missing, so callers cannot build a "null-null-null" bound.
			partsAsSearchDate = function(y, m, d){
				if (!y || !m || !d) { return null; }
				let pad = function(v){ return (String(v).length < 2 ? "0" : "") + v; };
				return y + "-" + pad(m) + "-" + pad(d);
			};
			// DHDR03.06: dates must read the same across every DHDR view, so the patient header and
			// the "searched with" line render through the same default medium format the tables use
			// rather than the raw yyyy-MM-dd parts. Keeps the placeholder when no date of birth is
			// recorded, so the gap stays visible instead of collapsing to nothing.
			$scope.demographicDobText = function(){
				let iso = partsAsSearchDate($scope.demographic.dobYear,
						$scope.demographic.dobMonth, $scope.demographic.dobDay);
				return iso ? $filter('date')(iso) : "--";
			};
			// DHDR03.02: how a differing field reads in the patient banner. Absent from the EMR and
			// disagreeing with the EMR are both flagged, but they call for different action.
			$scope.matchNote = function(unmatched, missing){
				if (!unmatched) { return ""; }
				return missing ? " (NOT IN EMR)" : " (UNMATCHED)";
			};
			// The marker belongs on the value it describes. `unmatched` above is a set-level
			// verdict - true when ANY returned value differs - so pinning it to the headline mislabels
			// the headline whenever the disagreeing value is one of the others. These two ask the
			// question per value instead.
			$scope.headlineNote = function(field, headline, unmatched, missing){
				if (!unmatched) { return ""; }
				if (missing) { return " (NOT IN EMR)"; }
				let bad = $scope.dhdrPatientUnmatchedValues
						? $scope.dhdrPatientUnmatchedValues[field] : null;
				// No per-value record (name spans two fields, so it keeps the set-level answer) falls
				// back to the old behaviour rather than silently dropping the marker.
				if (!bad) { return " (UNMATCHED)"; }
				return bad.indexOf(headline) >= 0 ? " (UNMATCHED)" : "";
			};
			// DHDR03.02: the headline value is one of several when the returned records disagree.
			// List the others beside it rather than dropping them - they were compared, so they should
			// be visible.
			$scope.variantNote = function(field){
				var seen = $scope.dhdrPatientVariants ? $scope.dhdrPatientVariants[field] : null;
				if (!seen || seen.length < 2) { return ""; }
				var bad = $scope.dhdrPatientUnmatchedValues
						? $scope.dhdrPatientUnmatchedValues[field] : null;
				// mark the differing ones where they are listed, so the reader can tell which of
				// several returned values is the one that disagrees with the chart.
				var rest = seen.slice(1).map(function(v){
					return (bad && bad.indexOf(v) >= 0) ? v + " (UNMATCHED)" : v;
				});
				return " [also in DHDR: " + rest.join(", ") + "]";
			};
			$scope.anyVariants = function(){
				var v = $scope.dhdrPatientVariants;
				if (!v) { return false; }
				for (var k in v) {
					if (v.hasOwnProperty(k) && v[k] && v[k].length > 1) { return true; }
				}
				return false;
			};
			// DHDR02.03(c) searches all available events, normally from the patient's birth. Where
			// there is no date of birth to start from, this floor stands in: dropping the lower
			// bound is not equivalent, because the service then applies its own 120-day default
			// (BP5) and "Search All" would quietly become a 120-day search.
			let earliestSearchDate = "1900-01-01";
			$scope.searchConfig = {};
			// UI-only, deliberately NOT on searchConfig: that object is posted verbatim to
			// searchByDemographicNo2, and an unknown field there fails Jackson deserialization.
			$scope.searchDays = defaultDaysToSearch;
			// DHDR02.03(a): fill the range from a number of days before today. Writes only into the
			// two date fields, never back into defaultDaysToSearch - DHDR02.03 requires that editing
			// the range while searching must not change the configured default.
			$scope.applySearchDays = function(){
				let days = parseInt($scope.searchDays, 10);
				// Leave the range untouched rather than rewrite it from a number that cannot
				// produce one - the same answer a blank or non-numeric entry gets.
				if (isNaN(days) || days < 1 || days > maxDaysToSearch) { return; }
				let end = new Date();
				let start = new Date(end);
				start.setDate(end.getDate() - days);
				$scope.searchConfig.endDate = asSearchDate(end);
				$scope.searchConfig.startDate = asSearchDate(start);
			};
			// Hand-editing a date, or searching all events, means the range is no longer the one the
			// days box describes, so clear it rather than leave a number that misstates the period.
			$scope.clearSearchDays = function(){ $scope.searchDays = null; };
			$scope.applySearchDays();

			// DHDR02.05: state the period actually searched. With no start date the query carries no
			// lower bound, so saying "<blank> to <end>" would advertise a period we never asked for.
			// Shared with the BP4 echo check below so both describe a range identically.
			let periodText = function(startDate, endDate){
				// DHDR03.06: shown in the tables' format. Safe on a yyyy-MM-dd string - the date
				// filter reads it as local, unlike new Date(), which would land a day earlier.
				let shown = function(d){ return d ? $filter('date')(d) : d; };
				let start = shown(startDate);
				let end = shown(endDate);
				if (!start && !end) { return "all available events"; }
				if (!start) { return "all events up to " + end; }
				if (!end) { return start + " onwards"; }
				return start + " to " + end;
			};
			$scope.searchPeriodText = function(){
				return periodText($scope.searchConfig.startDate, $scope.searchConfig.endDate);
			};
			$scope.searching = false;
			$scope.showSummaryProductFilter = false;
			$scope.showSummaryServiceFilter = false;
			$scope.hideShowDhirPharmaVal = true;
			$scope.hideShowDhirDrugVal = true;
			$scope.dhdrPatient = {
				firstName: String,
				lastName: String,
				gender: String,
				dob: String,
				hin: String,
				nameUnmatched: false,
				genderUnmatched: false,
				dobUnmatched: false,
				hinUnmatched: false
			};
			// DHDR03.02: one response can carry more than one patient identity. BestPractice 15
			// explains why - the DHDR query runs on HCN alone, so it returns every record for that HCN
			// including historic ones whose name/DOB/gender the dispensing pharmacy recorded
			// differently. OMD's own validation captures show both: three spellings of a given name in
			// one response, and 10 male / 6 female rows for one patient. Every distinct value has to be
			// compared, not just whichever record happened to arrive last.
			$scope.dhdrPatientVariants = {firstName: [], lastName: [], gender: [], dob: [], hin: []};
			// which of those values disagree with the EMR, so the marker lands on the right one.
			$scope.dhdrPatientUnmatchedValues = {firstName: [], lastName: [], gender: [], dob: [], hin: []};
			$scope.dhdrPatientResolved = false;
			$scope.dhdrPatientData = "";
			$scope.patientDataUnmatched = false;
			$scope.dhdrPatientDataUnmatched = false;
			// BP4: set when the service's `self` link reports a date range other than the one requested.
			$scope.searchPeriodEchoMismatch = null;
			$scope.resultShortfall = null;
			$scope.collectedEntries = 0;
			$scope.searchComplete = false;
			$scope.buttonDisabled = false;

			
			$scope.hideShowDhirPharma = function(){
				if($scope.hideShowDhirPharmaVal){
					$scope.hideShowDhirPharmaVal = false;
				}else{
					$scope.hideShowDhirPharmaVal = true;
				}
			}
			
			$scope.hideShowDhirDrug = function(){
				if($scope.hideShowDhirDrugVal){
					$scope.hideShowDhirDrugVal = false;
				}else{
					$scope.hideShowDhirDrugVal = true;
				}
			}
			
			$scope.showHideFilter = function(){
				if($scope.showSummaryProductFilter){
					$scope.searchtxt = {};
					$scope.showSummaryProductFilter =false;
				}else{
					$scope.showSummaryProductFilter = true;
				}
			}
			
			$scope.showFilter = function(){
				return $scope.showSummaryProductFilter;
			}
			
			$scope.showHideServiceFilter = function(){
				if($scope.showSummaryServiceFilter){
					$scope.searchServicetxt = {};
					$scope.showSummaryServiceFilter =false;
				}else{
					$scope.showSummaryServiceFilter = true;
				}
			}
			
			$scope.showServiceFilter = function(){
				return $scope.showSummaryServiceFilter;
			}
			
			
			
			
			getAllActiveProviders = function(){
	    			providerService.getAllActiveProviders().then(function(data){
		    			$scope.activeProviders = data;
		    			angular.forEach($scope.activeProviders, function(provider) {
		    				activeProvidersHash[provider.providerNo] = provider;
		    			});
				});
	    		};
	    		
	    		getAllActiveProviders();
	    		
	    		$scope.getProviderName = function(providerNumber){
	    			provider = activeProvidersHash[providerNumber];
	    			if(provider == null){ return providerNumber+" N/A inactive"}
	    			return provider.lastName+", "+provider.firstName;
	    		}

			// DHDR05.02(e): dose unit for an EMR med. Not med.doseUnit - that is the DHDR
			// side's, off doseQuantity.unit, and can be a mass. Prefer drugs.unit, but it
			// often just repeats the strength unit, so fall back to the drug form where the
			// form names a countable unit (DemographicExportHelper's list). Anything else
			// has no unit to show, and DHDR05.02 asks for these "if available".
			// "globule" was inherited as "grobule"; both are listed so a dosage form recorded with the
			// original typo still matches, rather than silently losing its unit on correction.
			let emrDoseUnitForms = ["capsule","drop","dosing","globule","grobule","granule","patch","pellet","pill","tablet"];
			$scope.emrDoseUnit = function(med){
				if (!med) { return ""; }
				let unit = med.unit ? med.unit.toLowerCase() : "";
				let strengthUnit = med.strengthUnit ? med.strengthUnit.toLowerCase() : "";
				if (unit && unit !== strengthUnit) { return unit; }
				let form = med.form ? med.form.toLowerCase() : "";
				for (let i = 0; i < emrDoseUnitForms.length; i++) {
					if (form.indexOf(emrDoseUnitForms[i]) !== -1) { return emrDoseUnitForms[i]; }
				}
				return "";
			}

			// DHDR03.05: the EMR side must sort by the same two elements as the DHDR side - generic
			// name and dispensed date - and show which one is active. Held on an object because the
			// EMR table is ng-included twice, and a primitive assigned from either include would
			// shadow the parent scope instead of writing through to it. Defaults to date descending,
			// which is what DHDR03.01 asks the view to open on.
			$scope.emrSort = { field: "rxDate", reverse: true };
			// Sorts on the value the Medication column actually displays, which falls back through
			// brand and custom name when the EMR holds no generic name.
			$scope.emrSortValue = function(med){
				if ($scope.emrSort.field === "medication") {
					return (med.genericName || med.brandName || med.customName || "").toUpperCase();
				}
				return med.rxDate;
			};

			$scope.orderByField = 'whenPrepared';
			$scope.reverseSort = true;
			
			$scope.serviceOrderByField = 'whenPrepared';
			$scope.serviceReverseSort = true;
			
			
			////////
			currentViewValue = 'summary'
			
			$scope.showSummary = function(){
				currentViewValue = 'summary';
			}
			
			// A print that fails leaves the user with nothing to act on if it is announced through an
			// alert() that says only "Error getting printout". Not a DHDR14.01 case - this is the EMR's
			// own PDF build, not a message from the DHDR EHR Service - but the notice list is where the
			// reader already looks, and it carries the code, severity, time and what to do next.
			var printFailed = function(view){
				$scope.serviceErrors.push({
					httpMessage: "The " + view + " printout could not be produced.",
					httpCode: "DHDR13.01",
					severity: "error",
					dateTime: new Date(),
					moreInformation: "The records shown on screen are unaffected. Try printing again; if it"
						+ " keeps failing, report it to your EMR administrator with the time shown here."
				});
			};

			$scope.printSummary = function(){
				var toPrint = {};
				toPrint.meds = $scope.meds;
				toPrint.services = $scope.services;
				// DHDR13.01.b: the DHDR-side patient demographic printed on each page
				toPrint.dhdrPatient = $scope.dhdrPatient;
				// Already yyyy-MM-dd; re-filtering could shift the day.
				toPrint.startDate = $scope.searchConfig.startDate;
				toPrint.endDate   = $scope.searchConfig.endDate;
				// BP4: where the service reported applying a different range, the printout must not
				// assert the requested one unqualified - the paper outlives the screen carrying the
				// warning, and the range it states is a DHDR13.01/02.05 claim.
				toPrint.serviceReportedPeriod = $scope.searchPeriodEchoMismatch;

				$http.post('../ws/rs/dhdr/'+$scope.demographicNo+'/print/summary',toPrint,{ responseType: 'arraybuffer' }).then(function (response) {
					
				       var file = new Blob([response.data], {type: 'application/pdf'});
				       var fileURL = URL.createObjectURL(file);
				       window.open(fileURL);
				}, function(errorMessage) {
					printFailed("Summary");
				});	
				//window.open('../ws/rs/dhdr/'+$scope.demographicNo+'/print/summary','_blank');
			}
			
			$scope.printComparative = function(){
				var toPrint = {};
				toPrint.meds = $scope.meds;
				toPrint.services = $scope.services;
				toPrint.localData = $scope.compLocalMeds;
				// DHDR13.01.b: the DHDR-side patient demographic printed on each page
				toPrint.dhdrPatient = $scope.dhdrPatient;
				// Already yyyy-MM-dd; re-filtering could shift the day.
				toPrint.startDate = $scope.searchConfig.startDate;
				toPrint.endDate   = $scope.searchConfig.endDate;
				// BP4: where the service reported applying a different range, the printout must not
				// assert the requested one unqualified - the paper outlives the screen carrying the
				// warning, and the range it states is a DHDR13.01/02.05 claim.
				toPrint.serviceReportedPeriod = $scope.searchPeriodEchoMismatch;

				$http.post('../ws/rs/dhdr/'+$scope.demographicNo+'/print/comparative',toPrint,{ responseType: 'arraybuffer' }).then(function (response) {
					
				       var file = new Blob([response.data], {type: 'application/pdf'});
				       var fileURL = URL.createObjectURL(file);
				       window.open(fileURL);
				}, function(errorMessage) {
					printFailed("Comparative");
				});	
				//window.open('../ws/rs/dhdr/'+$scope.demographicNo+'/print/summary','_blank');
			}
			
			$scope.showComp = function() {
				currentViewValue = 'comp';	
					rxService.getMedications($scope.demographicNo, "").then(function(data) {
						// The drug list arrives as `drug`, not `content`: DrugSearchResponse annotates
						// getContent() with @XmlElement(name="drug") inside @XmlElementWrapper("content"),
						// and the wrapper only survives in XML - the JSON this endpoint returns is
						// {"drug":[...]}. Reading `content` left the EMR side of the comparison empty.
						$scope.compLocalMeds = data.data.drug;

						angular.forEach($scope.compLocalMeds,function(med){
							med.providerName = $scope.getProviderName(med.providerNo);
						});
						
					}, function(errorMessage) {
						// The point of this view is to set the provincial record beside the local one, so an
						// EMR column that is empty because the load failed must not read as "this patient has
						// no medications". Empty the list so the count is honest, and raise it where the
						// reader already looks for service problems.
						$scope.compLocalMeds = [];
						$scope.serviceErrors.push({
							httpMessage: "The EMR medication list could not be loaded, so the EMR side of this"
								+ " comparison is empty. It does not mean the patient has no medications recorded.",
							httpCode: "DHDR05.02",
							severity: "error",
							dateTime: new Date(),
							moreInformation: "Re-open the comparative view to try again. Use the patient's"
								+ " medication record in the EMR to compare against the DHDR results below."
						});
					});	
				
			}
			
			$scope.currentView = function(view){
				if(currentViewValue === view){
					return "active";
				}
			}
			
			$scope.viewWhen = function(view){
			
				if(currentViewValue === view){
					return true;
				}
			}
			

			
			
			
			$scope.setSearchDateToAll = function(){

				
				$scope.clearSearchDays();
				$scope.searchConfig.startDate = partsAsSearchDate($scope.demographic.dobYear,
					$scope.demographic.dobMonth, $scope.demographic.dobDay) || earliestSearchDate;
				$scope.searchConfig.endDate = asSearchDate(new Date());
				$scope.callSearch();
			}
			
			$scope.medsWithGroupedDups = [];

			// DHDR04.03: groups are collapsed by default (only the most-recent event per
			// group shows via uniqMeds). This single-action toggle expands every group at
			// once by switching the drug table to the full flat event list (meds), then
			// collapses back to the grouped heads. Per-group expand stays on the Rx Group Count
			// modal (showGroupedMeds2).
			$scope.expandAll = false;
			$scope.toggleExpandAll = function(){
				$scope.expandAll = !$scope.expandAll;
			}

			// DHDR07.03: pharmacy service groups collapse by default (only the group head
			// shows via uniqServices). This single-action toggle expands every pharmacy
			// group at once by switching the service table to the full flat event list
			// (services), then collapses back to the grouped heads. Mirrors the drug
			// expand-all; per-group expand stays on the service-count modal
			// (showGroupedServices2).
			$scope.expandAllServices = false;
			$scope.toggleExpandAllServices = function(){
				$scope.expandAllServices = !$scope.expandAllServices;
			}


			// DHDR14.01: one severity-to-style map for every notice. An informational outcome - a
			// successful temporary unblock (CONSENT_TEMP_UNBLOCK) or COVaxON being unavailable - is
			// not an error and must not read as a red one; only error/fatal are danger.
			$scope.noticeClass = function(severity){
				if(severity === "warning"){ return "alert-warning"; }
				if(severity === "information" || severity === "informational"){ return "alert-info"; }
				return "alert-danger";
			}
			// The leading label on the code line, so an informational notice does not announce an
			// "Error code" for an event that is not an error.
			$scope.noticeCodeLabel = function(severity){
				if(severity === "warning"){ return "Warning code:"; }
				if(severity === "information" || severity === "informational"){ return "Notice code:"; }
				return "Error code:";
			}
			$scope.issueClass = function(issue){
				return $scope.noticeClass(issue.severity);
			}

			// DHDR14.01: the actionable code is in issue.details.coding.code or issue.diagnostics;
			// issue.code is only the FHIR issue-type token.
			$scope.dhdrCode = function(issue){
				if(issue.details && issue.details.coding && issue.details.coding.length > 0 && issue.details.coding[0].code){
					return issue.details.coding[0].code;
				}
				return issue.diagnostics ? issue.diagnostics : issue.code;
			}

			// DHDR02.04: error/fatal and consent-block issues suppress the empty-state; an
			// informational outcome (e.g. COVaxON down) must not.
			$scope.hasBlockingOutcome = function(){
				for(var oi = 0; oi < $scope.outcomes.length; oi++){
					var iss = $scope.outcomes[oi].issues || [];
					for(var ii = 0; ii < iss.length; ii++){
						if(iss[ii].severity === "error" || iss[ii].severity === "fatal" || iss[ii].code === "suppressed"){
							return true;
						}
					}
				}
				return false;
			}

			$scope.serviceErrorClass = function(serviceError){
				return $scope.noticeClass(serviceError.severity);
			}
			
			
			$scope.compDhirMeds = [];
			$scope.compLocalMeds = [];
			
			/*$scope.fillCompView = function(){
				var compsearchConfig = {};
				compsearchConfig.startDate = new Date($scope.demographic.dobYear+"-"+$scope.demographic.dobMonth+"-"+$scope.demographic.dobDay);
				compsearchConfig.endDate = new Date();
			
				dhdrService.searchByDemographicNo2($scope.demographicNo,compsearchConfig).then(function(response){
					$scope.searching = false;
					$scope.compDhirMeds = [];
					$scope.outcomes = [];
					for (x of  response.entry) {
						if(x.resource.resourceType === "OperationOutcome"){
							var o = new OperationOutcome(x);
							$scope.outcomes.push(o);
						}else if(x.resource.resourceType === "MedicationDispense"){
							var d = new MedicationDispense(x);
							$scope.compDhirMeds.push(d);
						}
					}
				},function(reason){
					$scope.searching = false;
					alert(reason);
				});
				
				
				rxService.getMedications($scope.demographicNo, "").then(function(data) {
					// See showComp: the endpoint returns the list as `drug`, not `content`.
					$scope.compLocalMeds = data.data.drug;
				}, function(errorMessage) {
					//rxComp.error = errorMessage;
				});
				
			};
			*/
			
			
			/*
			$scope.$watch('location.search()', function() {
		        $scope.demographicNo = ($location.search()).demographicNo;
		        getDemo();
		    }, true);
*/
			
			
			$scope.showGroupedMeds = function(med) {
				hiddenGroup = $scope.medsWithGroupedDups[med.getUniqVal()];
				//for (x of  hiddenGroup) {
				//	x.hide = false;
				//}
				
				var currentlyHasHiddenItems = false; 
				for (x of  hiddenGroup) {
					if(x.hide){
						currentlyHasHiddenItems = true;
					}
				}
				
				if(currentlyHasHiddenItems){
					for (x of  hiddenGroup) {
						x.hide = false;
					}
				}else{
					for (x of  hiddenGroup) {
						if(x.hiddenRecord){
							x.hide = true;	
						}
					}
				}
				
			}
			
			$scope.showGroupedService = function(med){
				hiddenGroup = $scope.servicesWithGroupedDups[med.serviceGroupKey];
				var currentlyHasHiddenItems = false; 
				for (x of  hiddenGroup) {
					if(x.hide){
						currentlyHasHiddenItems = true;
					}
				}
				if(currentlyHasHiddenItems){
					for (x of  hiddenGroup) {
						x.hide = false;
					}
				}else{
					for (x of  hiddenGroup) {
						if(x.hiddenRecord){
							x.hide = true;	
						}
					}
				}
			}
			
			$scope.getRowClass = function(med){
				// Highlight the events whose own patient metadata disagrees with the EMR, not every
				// event in a result set that happens to contain one (BP14 clause 2).
				//
				// This read the aggregate flag, which was right when the aggregate was the only
				// comparison there was. Once the per-event decoration landed it painted every row
				// while only the differing rows carried the badge, so one row carried two signals
				// saying different things and the badge stopped meaning anything.
				//
				// Narrowing it loses no warning: every value in the aggregate came from some event,
				// so anything the aggregate flags is flagged by the event it came from.
				if(med && med.eventPatient && med.eventPatient.anyUnmatched){
					return "warning";
				}
			}
			
			// Hoisted out of processEntries so the per-event patient decoration can reuse them
			// rather than carry a second copy.
			function getFirstName(nameArray) {
				if (Array.isArray(nameArray) && nameArray.length > 0) {
					const givenNames = nameArray[0].given || [];
					return givenNames[0] || "";
				}
				return "";
			}
			function getLastName(nameArray) {
				if (Array.isArray(nameArray) && nameArray.length > 0) {
					return nameArray[0].family || "";
				}
				return "";
			}
			function getPatientIdentifier(identifierArray) {
				if (Array.isArray(identifierArray) && identifierArray.length > 0) {
					return identifierArray[0].value || "";
				}
				return "";
			}

			processEntries = function(entries){
				var skipped = 0;
				for (x of entries) {
				  try {
					if(x.resource.resourceType === "OperationOutcome"){
						var o = new OperationOutcome(x);
						$scope.outcomes.push(o);
					}else if(x.resource.resourceType === "MedicationDispense"){
						var d = new MedicationDispense(x);
						if (d.patient) {
							// DHDR03.02: record every distinct value rather than overwriting. The
							// first one seen stays the banner's headline value - not because it is the
							// most recent (we ask for -whenprepared but the service may ignore it) but
							// because it is deterministic, which is exactly what the old last-writer-wins
							// assignment was not. collectVariant returns that headline value.
							$scope.dhdrPatient.firstName =
									collectVariant("firstName", getFirstName(d.patient.name));
							$scope.dhdrPatient.lastName =
									collectVariant("lastName", getLastName(d.patient.name));
							$scope.dhdrPatient.gender = collectVariant("gender", d.patient.gender || "");
							$scope.dhdrPatient.dob = collectVariant("dob", d.patient.birthDate || "");
							$scope.dhdrPatient.hin =
									collectVariant("hin", getPatientIdentifier(d.patient.identifier));
							$scope.dhdrPatientResolved = true;
						}

						// Case- and padding-insensitive dedupe, matching how the values are compared
						// against the EMR below: "YULIA" and "Yulia" are one identity, "YULIUA" is not.
						function collectVariant(field, value) {
							var seen = $scope.dhdrPatientVariants[field];
							var normalised = String(value === null || value === undefined ? "" : value)
									.trim().toUpperCase();
							var already = false;
							for (var vi = 0; vi < seen.length; vi++) {
								if (String(seen[vi]).trim().toUpperCase() === normalised) {
									already = true;
									break;
								}
							}
							if (!already) {
								seen.push(value);
							}
							return seen[0];
						}

						if(d.categoryCode === "service"){
							$scope.services.push(d);

							//if ($scope.medsWithGroupedDups.indexOf(d.getUniqVal()) === -1) {
							// Fall back when the event carries no brand identifier, so the deref cannot throw
							// and abort the loop. Key is stored so the badge and expand handler agree.
							var svcKey = (d.brandName && d.brandName.display) ? d.brandName.display : (d.genericName || 'Unknown Product');
							d.serviceGroupKey = svcKey;
							// Only collect here. Which member heads the group, and which are hidden behind
							// it, is decided once by regroupByMostRecent below - deciding it here too
							// would mean picking whichever event happened to arrive first.
							if($scope.servicesWithGroupedDups[svcKey] === undefined){
								$scope.servicesWithGroupedDups[svcKey] = [];
							}
							$scope.servicesWithGroupedDups[svcKey].push(d);

						}else{

							///
							$scope.meds.push(d);

							//if ($scope.medsWithGroupedDups.indexOf(d.getUniqVal()) === -1) {
							if($scope.medsWithGroupedDups[d.getUniqVal()] === undefined){
								$scope.medsWithGroupedDups[d.getUniqVal()] = [];
							}
							$scope.medsWithGroupedDups[d.getUniqVal()].push(d);
						}
					}
				  } catch (e) {
					// One malformed entry must not truncate the whole list. Without this the loop aborts and
					// every later entry is silently lost, leaving a result set that still looks complete - so
					// count the record and surface it below rather than dropping it quietly. Structural detail
					// only, never the record's contents, so no PHI reaches the console.
					skipped++;
					console.error("DHDR: skipped unreadable entry - " + e.message);
				  }
				}

				// Reported through the same notice list as the consent and search errors, so a record the
				// viewer could not read is raised where the reader already looks for service problems.
				if (skipped > 0) {
					$scope.serviceErrors.push({
						httpMessage: skipped + (skipped === 1 ? " record was" : " records were")
							+ " returned by DHDR but could not be displayed.",
						httpCode: "DHDR02.01",
						severity: "error",
						// DHDR03.06: a Date, rendered through the same filter as every other date in
						// the viewer. toLocaleString() followed the workstation locale, so this one
						// timestamp read 6/5/2026 or 05/06/2026 or 2026-06-05 depending on the machine
						// - the only ambiguous date on screen, beside an outcome notice already using
						// date:'medium'.
						dateTime: new Date(),
						moreInformation: "The data for these records was incomplete or in an unexpected format."
							+ " The remaining records are shown. Use other available sources of medication"
							+ " information to confirm this patient's history."
					});
				}

				// Groups are only complete once this page's entries are in, and a later page can carry
				// an event newer than the head chosen so far, so re-derive rather than patch.
				regroupByMostRecent($scope.medsWithGroupedDups, $scope.uniqMeds);
				regroupByMostRecent($scope.servicesWithGroupedDups, $scope.uniqServices);

				// DHDR03.04: filterable copies of the two columns that render a composed value, so the
				// filter matches the text on screen rather than the underlying model. Without these the
				// date filter only answered to the raw yyyy-MM-dd the user never sees, and the prescriber
				// filter only to the family name, leaving 03.04(d)'s given name unreachable.
				decorateForFilters($scope.meds, 'prescriber');
				decorateForFilters($scope.services, 'pharmacist');
				// Rebuilt from the full event lists, not the grouped heads, so values that only occur
				// inside a collapsed group are still offered. Paging calls this again as pages land.
				buildDistinctFilterValues();

				//If a block record is found the other warnings are dumped.  Probably a bad idea but OMD's requirement.
				for(outcome of $scope.outcomes) {
					var replaceIssue = null;
					for(issue of outcome.issues){	
						if	(issue.code === 'suppressed'){
							replaceIssue = issue;
						}
					}
					if(replaceIssue != null){
						outcome.issues = [];
						outcome.issues.push(replaceIssue);
					}
				}
				
			};

			// DHDR03.04: true when any filter box holds a value. The filter models are plain objects
			// whose keys appear as the user types, and brandName is nested one level, so test the leaf
			// values rather than the key count - an empty box leaves an empty string behind.
			anyFilterSet = function(model){
				if(!model) { return false; }
				for(var k in model){
					var v = model[k];
					if(v && typeof v === 'object'){
						if(anyFilterSet(v)) { return true; }
					} else if(v !== null && v !== undefined && ('' + v).trim() !== ''){
						return true;
					}
				}
				return false;
			};
			$scope.drugFilterActive = function(){ return anyFilterSet($scope.searchtxt); };
			$scope.serviceFilterActive = function(){ return anyFilterSet($scope.searchServicetxt); };

			// DHDR04.02/07.02: "a group MUST be identified differently than an event that is not part
			// of any group". regroupByMostRecent marks every head with headRecord, including a group
			// of one, so headRecord cannot tell the two apart - the size test is what distinguishes
			// them. Both conditions are needed: headRecord still keeps the count off the other members
			// once a group is expanded. The count is then its own marker - a row carries one only when
			// it stands for events hidden behind it.
			$scope.drugGroupSize = function(med){
				var group = $scope.medsWithGroupedDups[med.getUniqVal()];
				return group ? group.length : 0;
			};
			$scope.serviceGroupSize = function(med){
				var group = $scope.servicesWithGroupedDups[med.serviceGroupKey];
				return group ? group.length : 0;
			};

			// DHDR03.04: mirror the two composed columns onto the event so they can be filtered as
			// displayed. The date uses the same $filter('date') default the tables use, so the two can
			// only ever agree; the name uses the same "Last, First" the column renders, which makes one
			// substring test cover both [Practitioner.name.family] and [Practitioner.name.given].
			decorateForFilters = function(list, namePrefix){
				for(var i = 0; i < list.length; i++){
					var item = list[i];
					item.whenPreparedDisplay = item.whenPrepared ? $filter('date')(item.whenPrepared) : '';
					// Flattened because a nested filter expression - {brandName:{display:'x'}}, which is
					// what binding straight through to the nested property would build - does not filter
					// in this Angular version: it silently matches every row rather than none. Verified
					// against $filter('filter') directly.
					item.brandNameDisplay = (item.brandName && item.brandName.display) ? item.brandName.display : '';
					var last = item[namePrefix + 'Lastname'] || '';
					var first = item[namePrefix + 'Firstname'] || '';
					item[namePrefix + 'Name'] = (last || first) ? (last + ', ' + first) : '';
				}
			};

			// DHDR03.04: "MUST pre-populate the filters with distinct values received from the DHDR EHR
			// Service." Each list holds the distinct non-empty values actually present in this result set,
			// in the same form the column displays, so a chosen value always matches something.
			distinctValues = function(list, path){
				var seen = {};
				var out = [];
				for(var i = 0; i < list.length; i++){
					var v = path.split('.').reduce(function(o, k){ return (o === null || o === undefined) ? o : o[k]; }, list[i]);
					if(v === null || v === undefined) { continue; }
					v = ('' + v).trim();
					if(!v || seen[v]) { continue; }
					seen[v] = true;
					out.push(v);
				}
				out.sort();
				return out;
			};

			// Dates are offered most recent first, matching the tables' default order. Sorting the
			// display strings would order them alphabetically - "Feb, Jan, Mar" - so pair each with its
			// raw value, sort on that, and emit the display form.
			distinctDates = function(list){
				var seen = {};
				var pairs = [];
				for(var i = 0; i < list.length; i++){
					var shown = list[i].whenPreparedDisplay;
					if(!shown || seen[shown]) { continue; }
					seen[shown] = true;
					pairs.push({ shown: shown, raw: list[i].whenPrepared });
				}
				pairs.sort(function(a, b){ return (a.raw < b.raw) ? 1 : (a.raw > b.raw) ? -1 : 0; });
				return pairs.map(function(p){ return p.shown; });
			};

			buildDistinctFilterValues = function(){
				$scope.drugFilterValues = {
					genericName:      distinctValues($scope.meds, 'genericName'),
					brandName:        distinctValues($scope.meds, 'brandName.display'),
					whenPrepared:     distinctDates($scope.meds),
					dispensingPharmacy: distinctValues($scope.meds, 'dispensingPharmacy'),
					prescriberName:   distinctValues($scope.meds, 'prescriberName')
				};
				$scope.serviceFilterValues = {
					genericName:      distinctValues($scope.services, 'genericName'),
					brandName:        distinctValues($scope.services, 'brandName.display'),
					ahfsClass:        distinctValues($scope.services, 'ahfsClass'),
					whenPrepared:     distinctDates($scope.services),
					dispensingPharmacy: distinctValues($scope.services, 'dispensingPharmacy'),
					pharmacistName:   distinctValues($scope.services, 'pharmacistName'),
					fax:              distinctValues($scope.services, 'dispensingPharmacyFaxNumber')
				};
			};

			// DHDR04.02 / DHDR07.02: a group is represented by its most recent event, and the events
			// inside it are listed most recent first. Both are settled here rather than by the order
			// entries arrived in, because that order belongs to the service: `_sort=-whenprepared` is
			// a hint a FHIR server MAY ignore, and pages arrive one at a time, so a group's newest
			// event can land after its head was already chosen. Sorts through the same orderBy the
			// tables use, so the view has one ordering rule rather than two that could drift.
			// Rewrites uniq in place - the tables bind to that array, so it must keep its identity.
			regroupByMostRecent = function(groups, uniq){
				uniq.length = 0;
				for(var key in groups){
					var members = $filter('orderBy')(groups[key], 'whenPrepared', true);
					if(!members.length){ continue; }
					groups[key] = members;
					for(var i = 0; i < members.length; i++){
						// The head is the row the Summary view shows; the rest stay hidden behind it.
						// hiddenRecord marks a member as non-head for good. Only showGroupedMeds and
						// showGroupedService read it, and no template calls either any more, so it is
						// carried rather than relied on - dropping it would leave those two silently
						// wrong about which rows to re-hide if they are ever wired back up.
						members[i].headRecord = (i === 0);
						members[i].hide = (i > 0);
						members[i].hiddenRecord = (i > 0);
					}
					uniq.push(members[0]);
				}
			};

			$scope.openWindow = function(url) {
				window.open(url, '_blank');
			}
			
			$scope.callSearch = function(){

				// A days value applySearchDays refused - zero, negative, or past the arithmetic's ceiling -
				// left the two date fields as they were, so the box would otherwise go on displaying a period
				// this search does not ask for. Cleared here rather than as it is typed: applySearchDays runs
				// on every keystroke, and 40000 passes through 4, 40, 400 on the way in.
				let enteredDays = parseInt($scope.searchDays, 10);
				if (!isNaN(enteredDays) && (enteredDays < 1 || enteredDays > maxDaysToSearch)) {
					$scope.clearSearchDays();
				}

				$scope.buttonDisabled = true;
				$scope.meds = [];
				$scope.services = [];
				// Cleared with the results they describe, so a new search never offers stale values.
				$scope.drugFilterValues = {};
				$scope.serviceFilterValues = {};
				$scope.outcomes = [];
				$scope.serviceErrors = [];
				$scope.uniqMeds = [];
				$scope.uniqServices = [];
				$scope.expandAll = false;
				$scope.expandAllServices = false;
				$scope.overrideResultMessage = '';

				$scope.medsWithGroupedDups = [];
				$scope.servicesWithGroupedDups = [];
				$scope.searchConfig.searchId = null;
				$scope.searchConfig.pageId = null;
				$scope.searchComplete = false;
				$scope.dhdrPatientResolved = false;
				// Cleared per search: the notice describes the range this search asked for.
				$scope.searchPeriodEchoMismatch = null;
				$scope.resultShortfall = null;
				$scope.collectedEntries = 0;
				// Cleared per search, not per page: a paged walk calls processEntries once per page and
				// the variants must accumulate across all of them.
				$scope.dhdrPatientVariants = {firstName: [], lastName: [], gender: [], dob: [], hin: []};
			// Which of those values disagree with the EMR, so the marker lands on the right one.
			$scope.dhdrPatientUnmatchedValues = {firstName: [], lastName: [], gender: [], dob: [], hin: []};

				// DHDR02.02: the HCN is mandatory in the request and must not be sent absent. The
				// viewer auto-fires on load, so refuse to dispatch rather than send an empty hcn| query.
				if(!$scope.demographic || !$scope.demographic.hin || !((''+$scope.demographic.hin).trim())){
					$scope.buttonDisabled = false;
					$scope.searching = false;
					$scope.serviceErrors.push({
						httpMessage: "Cannot search the DHDR: this patient has no Health Card Number on file.",
						httpCode: "DHDR02.02",
						severity: "error",
						dateTime: new Date(),
						moreInformation: "A Health Card Number is required to query the DHDR EHR Service. Add the HCN to the patient record and try again."
					});
					return;
				}

				search($scope.demographicNo,$scope.searchConfig);
			
			}
		
			// BP4: the FHIR search specification requires that an application check the parameters the
			// server reports it actually used, which come back in the bundle's `self` link. Only the date
			// range is checked here. The sort hint cannot hurt us - the tables order client-side rather
			// than trusting `_sort` - but a dropped whenprepared bound would silently substitute the
			// service's own 120-day default (BP5), leaving the "Search period" line above advertising a
			// window the results did not come from.
			let whenPreparedBounds = function(selfUrl){
				// `bounds` keyed by FHIR prefix for comparison, `raw` in order for the description: a
				// service answering with a prefix we never send (the IG's own sample echoes eq) has no
				// ge/le to describe, and reporting that as "all available events" would misstate it.
				let out = {bounds: {}, raw: []};
				let q = selfUrl.indexOf("?");
				if (q < 0) { return out; }
				selfUrl.substring(q + 1).split("&").forEach(function(part){
					let eq = part.indexOf("=");
					if (eq < 0) { return; }
					if (decodeURIComponent(part.substring(0, eq)) !== "whenprepared") { return; }
					let v = decodeURIComponent(part.substring(eq + 1));
					// Keep the prefix rather than only the date: we send ge/le, and a service answering
					// eq/gt/lt has not applied the bound as asked even when the date matches. An absent
					// prefix means eq in FHIR.
					let m = /^(ge|le|gt|lt|eq|sa|eb|ne)?(.*)$/.exec(v);
					out.bounds[m[1] || "eq"] = m[2];
					out.raw.push(v);
				});
				return out;
			};
			let checkSearchPeriodEcho = function(response){
				let selfUrl = null;
				(response.link || []).forEach(function(l){
					if (l && l.relation === "self" && l.url) { selfUrl = l.url; }
				});
				// No self link is not a mismatch - the check is on values the service did return.
				if (!selfUrl) { return; }
				let echoed = whenPreparedBounds(selfUrl);
				// Only bounds the viewer actually asked for are asserted. Where a date field was left
				// blank the service supplies its own, and echoing that contradicts nothing.
				let wantGe = $scope.searchConfig.startDate;
				let wantLe = $scope.searchConfig.endDate;
				if (!(wantGe && echoed.bounds.ge !== wantGe) && !(wantLe && echoed.bounds.le !== wantLe)) {
					return;
				}
				let used = (echoed.bounds.ge || echoed.bounds.le)
						? periodText(echoed.bounds.ge, echoed.bounds.le)
						: (echoed.raw.length ? echoed.raw.join(", ") : "no date range");
				$scope.searchPeriodEchoMismatch = {
					requested: periodText(wantGe, wantLe),
					used: used,
					// Raw bounds as well, for the printout: it formats dates itself (DHDR03.06) rather
					// than reprinting a string this screen has already formatted.
					startDate: echoed.bounds.ge || "",
					endDate: echoed.bounds.le || ""
				};
			};

			// A searchset states how many resources matched the whole search, and the viewer pages
			// purely on the presence of a `next` link. Nothing checked that what arrived is what was
			// matched: a service that caps the page size and omits the link hands back a partial
			// medication history, and the "N results returned" counter then reports what we received
			// rather than what exists. A missing dispense can hide an interaction, and nothing on
			// screen would suggest anything was absent (DHDR05.01).
			let checkResultCompleteness = function(response){
				$scope.collectedEntries += (response.entry ? response.entry.length : 0);
				// Bundle.total describes the search, not the page, so it is only conclusive once the
				// walk has ended - mid-walk the running count is legitimately short of it.
				if (response.link && response.link.some(function(l){ return l.relation === "next"; })) {
					return;
				}
				let total = response.total;
				if (typeof total !== "number" || $scope.collectedEntries >= total) { return; }
				$scope.resultShortfall = { received: $scope.collectedEntries, expected: total };
			};

			// Ceiling on the paged walk in search(). At _count=1000 a page is 1000 events, so this is far
			// above any real patient history; reaching it means the service is not terminating the walk.
			const MAX_SEARCH_PAGES = 50;

			search = function(demographicNo,searchConfig){
				$scope.searching = true;
				// Released once the whole page walk has ended, not once a page has arrived. A paged
				// search fetches the next page from inside this handler, so re-enabling the controls
				// here would leave them live between pages: a second search started in that gap
				// resets searchId and pageId under the walk still running, and its entries land in
				// arrays that are cleared per search rather than per page.
				let walkFinished = function(){
					$scope.buttonDisabled = false;
					$scope.searching = false;
				};
				dhdrService.searchByDemographicNo2(demographicNo,searchConfig).then(function(response){

					if(angular.isUndefined(response.entry)){
						if(angular.isDefined(response.resourceType) && response.resourceType === "OperationOutcome"){
							var o = new OperationOutcome(response);
							$scope.outcomes.push(o);
							walkFinished();
							return;
						} else if (angular.isDefined(response.httpCode)) {
							// DHDR14.01: the service could not be reached. Render the notice rather
							// than alert()ing it, and stop - there is no bundle to process, and the
							// empty-state message (DHDR02.04) would misreport the failure as "no
							// records found".
							$scope.serviceErrors.push(response);
							walkFinished();
							return;
						}
					}
						
					// A valid search with zero results may omit the entry array; guard so the empty-state
					// (DHDR02.04) renders instead of throwing on an undefined entry list.
					if (response.entry) {
						processEntries(response.entry);
					}

					$scope.searchComplete = true;

					// Only run the EMR<->DHDR patient-match comparison once processEntries has populated
					// dhdrPatient from a returned record; on a zero-result search its fields are still the
					// placeholder String constructor and .toUpperCase() would throw (breaks DHDR02.04).
					if ($scope.dhdrPatient && typeof $scope.dhdrPatient.firstName === "string") {
						// Built the same way as a search bound, so the parts are zero-padded before
						// being compared with the DHDR record's yyyy-MM-dd birthDate.
						let demographicDob = partsAsSearchDate($scope.demographic.dobYear,
								$scope.demographic.dobMonth, $scope.demographic.dobDay);
						// DHDR03.02 asks that anything not matching be identified, so every difference
						// is flagged - including a field the EMR simply does not hold. That case reads
						// differently in the banner: a missing value needs filling in, a disagreeing
						// one needs reconciling, and they are not the same instruction to the reader.
						let blank = function(v){
							return v === null || v === undefined || String(v).trim() === "";
						};
						// The EMR holds a name as the clinic typed it and the DHDR record as the ministry
						// holds it, so compare them folded: a difference in case or padding is not a
						// difference in identity, and flagging one sends the reader to reconcile two
						// records that already agree.
						// A value the DHDR record does not carry is UNKNOWN, not in conflict. Without this
						// guard a dispense whose subject is an external reference - the shape the IG's own
						// BundleSearch.json ships, carrying no contained Patient - compares every EMR value
						// against "" and reports all four fields as disagreeing, when the record said
						// nothing about the patient at all. Flagging those drowns the genuine mismatches
						// this comparison exists to surface.
						let differs = function(emr, dhdr){
							if (blank(dhdr)) { return false; }
							return String(blank(emr) ? "" : emr).trim().toUpperCase()
									!== String(dhdr).trim().toUpperCase();
						};
						// Same rule for the fields compared verbatim rather than case-folded (date of
						// birth, health card number), so all four agree on what absence means.
						let valueDiffers = function(emr, dhdr){
							return blank(dhdr) ? false : emr !== dhdr;
						};

						// DHDR03.02 requires "any patient information that does not match" be
						// identified, so each field is tested against EVERY distinct value the response
						// carried, not just the headline one. Comparing only the headline made the MUST
						// order-dependent: with OMD's WILLIAM_KINDRED capture (10 male / 6 female rows,
						// one patient) reordering the entries alone turned the warning on and off.
						// Records WHICH values differ, not just whether any did. The set-level answer alone
						// put the "(UNMATCHED)" marker beside the headline whatever the headline was, so a
						// response whose first value agrees and whose later value does not marked the
						// agreeing one. Every value is still compared; the verdict
						// is just kept at the granularity it gets displayed at.
						let anyDiffers = function(field, compare){
							let seen = $scope.dhdrPatientVariants[field];
							$scope.dhdrPatientUnmatchedValues[field] = [];
							if (!seen || seen.length === 0) { return false; }
							for (let vi = 0; vi < seen.length; vi++) {
								if (compare(seen[vi])) {
									$scope.dhdrPatientUnmatchedValues[field].push(seen[vi]);
								}
							}
							return $scope.dhdrPatientUnmatchedValues[field].length > 0;
						};

						$scope.dhdrPatient.nameMissing = blank($scope.demographic.firstName)
								|| blank($scope.demographic.lastName);
						$scope.dhdrPatient.nameUnmatched =
								anyDiffers("firstName", function(v){
									return differs($scope.demographic.firstName, v); })
								|| anyDiffers("lastName", function(v){
									return differs($scope.demographic.lastName, v); });

						$scope.dhdrPatient.genderMissing = blank($scope.demographic.sex);
						$scope.dhdrPatient.genderUnmatched = anyDiffers("gender", function(v){
							// The EMR stores a single-letter sex, the DHDR record a FHIR gender token.
							let initial = (v === null || v === undefined || v === "")
									? "" : String(v).charAt(0).toUpperCase();
							return differs($scope.demographic.sex, initial);
						});

						$scope.dhdrPatient.dobMissing = blank(demographicDob);
						$scope.dhdrPatient.dobUnmatched = anyDiffers("dob", function(v){
							return valueDiffers(demographicDob, v); });

						$scope.dhdrPatient.hinMissing = blank($scope.demographic.hin);
						$scope.dhdrPatient.hinUnmatched = anyDiffers("hin", function(v){
							return valueDiffers($scope.demographic.hin, v); });

						$scope.dhdrPatientDataUnmatched = ($scope.dhdrPatient.nameUnmatched
								|| $scope.dhdrPatient.hinUnmatched || $scope.dhdrPatient.dobUnmatched
								|| $scope.dhdrPatient.genderUnmatched);

						// The Detailed view is scoped to ONE event, so it must show that event's
						// patient metadata rather than the whole result set's headline. Each event's
						// contained Patient is already captured (this.patient) and was only being read to
						// build the variant list. Decorated onto the event itself rather than exposed as a
						// controller helper: the Detail template is a $modal template and cannot see
						// controller scope, which is why the block was bound to dhdrPatient in the first
						// place. Same comparison functions as above, so the two cannot drift.
						let decorate = function(list){
							angular.forEach(list, function(m){
								let p = m.patient || {};
								let genderInitial = blank(p.gender)
										? "" : String(p.gender).charAt(0).toUpperCase();
								m.eventPatient = {
									firstName: getFirstName(p.name),
									lastName: getLastName(p.name),
									gender: blank(p.gender) ? "" : p.gender,
									dob: p.birthDate || "",
									hin: getPatientIdentifier(p.identifier),
									nameUnmatched: differs($scope.demographic.firstName, getFirstName(p.name))
											|| differs($scope.demographic.lastName, getLastName(p.name)),
									genderUnmatched: differs($scope.demographic.sex, genderInitial),
									dobUnmatched: valueDiffers(demographicDob, p.birthDate || ""),
									hinUnmatched: valueDiffers($scope.demographic.hin,
											getPatientIdentifier(p.identifier))
								};
								// BP14 clause 2 requires every event whose patient metadata disagrees with
								// the EMR be flagged, not only the ones a reader thinks to open. Derived
								// once here so the four result-set templates do not each re-encode the
								// same four-way condition and drift apart.
								let ep = m.eventPatient;
								let differing = [];
								if (ep.nameUnmatched) { differing.push("name"); }
								if (ep.genderUnmatched) { differing.push("gender"); }
								if (ep.dobUnmatched) { differing.push("date of birth"); }
								if (ep.hinUnmatched) { differing.push("health card number"); }
								ep.anyUnmatched = differing.length > 0;
								// Named in the tooltip so the row answers "what differs" without being
								// opened; the Detailed view remains the place the values themselves are.
								ep.unmatchedSummary = differing.length
										? "This event's DHDR patient details differ from the EMR record: "
												+ differing.join(", ") + "."
										: "";
							});
						};
						decorate($scope.meds);
						decorate($scope.services);
					}

					checkSearchPeriodEcho(response);
					checkResultCompleteness(response);

					if(response.link && response.link.some(function(l){ return l.relation === "next"; })){
						$scope.searchConfig.searchId = response.id;
						if($scope.searchConfig.pageId == null){
							$scope.searchConfig.pageId = 2;
						
						}else{
							$scope.searchConfig.pageId = $scope.searchConfig.pageId+1;
						}
						// A service that always advertises a next page - misbehaving, or looping on a cursor
						// it does not recognise - would recurse until the browser gave out, hammering the
						// endpoint on the way. At _count=1000 the cap is far above any real result set, so
						// reaching it means the walk is not terminating. Stop, and say so: a truncated list
						// shown silently is the failure this notice exists to prevent, same as the
						// resultShortfall case.
						if($scope.searchConfig.pageId > MAX_SEARCH_PAGES){
							walkFinished();
							$scope.serviceErrors.push({
								httpMessage: "The DHDR EHR Service kept offering further pages of results;"
									+ " the list below was stopped after " + MAX_SEARCH_PAGES + " pages and may be incomplete.",
								httpCode: "DHDR02.01",
								severity: "error",
								dateTime: new Date(),
								moreInformation: "Narrow the search date range and try again. Use other available"
									+ " sources of medication information to confirm this patient's history."
							});
							return;
						}
						search($scope.demographicNo,$scope.searchConfig);
					} else {
						walkFinished();
					}
					
					
					
				},function(reason){
					walkFinished();
					$scope.serviceErrors.push(reason);
				});
			}

			getDemo = function(){
				demographicService.getDemographic($scope.demographicNo).then(function(response){
					$scope.demographic = response;
					//search($scope.demographicNo,$scope.searchConfig);
					$scope.callSearch();
				},function(reason){
					// Was alert(reason), which rendered the rejection object as [object Object] and then
					// left the viewer silent - callSearch() never runs, so the page simply sits empty.
					$scope.serviceErrors.push({
						httpMessage: "The patient record could not be loaded, so no DHDR search was sent.",
						httpCode: "DHDR02.02",
						severity: "error",
						dateTime: new Date(),
						moreInformation: "Close this window and re-open the DHDR view from the patient's"
							+ " prescription screen. If it recurs, report it to your EMR administrator."
					});
				});
			};
			
			getDemo();
			
			$scope.getDetailView = function(med,$event){
			    
	    		    var modalInstance = $modal.open({
	    		      
	    		      templateUrl: 'myModalContent.html',
	    		      controller: 'ModalInstanceCtrl',
	    		      controllerAs: 'mpa',
	    		      parent: angular.element(document.body),
	    		      size: 'lg',
	    		      appendTo: $event,
	    		      resolve: {
	    		    	  	
	    		    	  		med: function () {
	    		          		return med;
	    		        		},
	    		        		demoNo: function () {
	    		          		return $scope.demographicNo;
	    		        		},
						  		dhdrPatient: function() {
								return $scope.dhdrPatient;
								}
	    		      }
	    		    });

	    		    modalInstance.result.then(function (selectedItem) {
	    		      selected = selectedItem;
	    		    }, function () {
	    		      // Dismissal is not an error; the empty handler keeps AngularJS from
	    		      // reporting the dismissed modal as an unhandled rejection.
	    		    });
    		  };
	    	
    		  
    		  $scope.showGroupedMeds2 =function(meds,$event){
				// The cell stays clickable once its count is hidden, so guard here rather than in the
				// template - a lone event is not a group and has nothing to expand (DHDR04.02).
				if(!meds || meds.length < 2){ return; }
				var modalInstance = $modal.open({
	    		      
	    		      templateUrl: 'drugDupsContent.html',
	    		      controller: 'DrugDupsInstanceCtrl',
	    		      controllerAs: 'ddpa',
	    		      parent: angular.element(document.body),
	    		      size: 'lg',
	    		      appendTo: $event,
	    		      resolve: {
	    		    	  	
	    		    	  		meds: function () {
	    		          		return meds;
	    		        		},
	    		        		getDetailView: function () {
	    		          		return $scope.getDetailView;
	    		        		} 
	    		      }
	    		    });

	    		    modalInstance.result.then(function (selectedItem) {
	    		      selected = selectedItem;
	    		    }, function () {
	    		      // Dismissal is not an error; the empty handler keeps AngularJS from
	    		      // reporting the dismissed modal as an unhandled rejection.
	    		    });
    		  };
    		  
    		  
    		  $scope.showGroupedServices2 =function(services,$event){
				// See showGroupedMeds2: same guard, DHDR07.02.
				if(!services || services.length < 2){ return; }
  				var modalInstance = $modal.open({
  	    		      
  	    		      templateUrl: 'pharmaDupsContent.html',
  	    		      controller: 'PharmaDupsInstanceCtrl',
  	    		      controllerAs: 'pdpa',
  	    		      parent: angular.element(document.body),
  	    		      size: 'lg',
  	    		      appendTo: $event,
  	    		      resolve: {
  	    		    	  	
  	    		    	  		services: function () {
  	    		          		return services;
  	    		        		},
  	    		        		getDetailView: function () {
  	    		          		return $scope.getDetailView;
  	    		        		} 
  	    		      }
  	    		    });

  	    		    modalInstance.result.then(function (selectedItem) {
  	    		      selected = selectedItem;
  	    		    }, function () {
  	    		      // Dismissal is not an error; the empty handler keeps AngularJS from
  	    		      // reporting the dismissed modal as an unhandled rejection.
  	    		    });
      		  };

			  $scope.logOverrideStatus = function(uuid, data, status) {
				  let reason = {};
				  if (status === 'Refused' || status === 'Cancelled') {
					  let reasonPrompt = prompt("Please provide a reason (optional):");
					  if (reasonPrompt != null) {
						  reason.name = "Reason";
						  reason.value = reasonPrompt;
						  data = reason;
					  }
				  }

				  // DHDR09.05: the message reflects the user decision, not the audit-log POST result.
				  var showOverrideResult = function() {
					  if (status === "Refused" || status === "Cancelled") {
						  $scope.overrideResultMessage = (status === "Refused")
								  ? "Access to Drug and Pharmacy Service Information has been refused."
								  : "Access to Drug and Pharmacy Service Information has been cancelled.";
						  $timeout(function() { window.close(); }, 4000);
					  }
				  };
				  dhdrService.logConsentOverride($scope.demographicNo, uuid, data, status)
						  .then(showOverrideResult, showOverrideResult);
			  }
			
    		  // DHDR10.01 / DHDR14.01: every path that fails to bring up the PCOI viewlet reports through
    		  // the notice list, with a code, a severity, a timestamp and a next step the EMR user can take
    		  // without an admin role.
    		  var viewletLaunchFailed = function(detail){
				  $scope.serviceErrors.push({
					  httpMessage: "The Temporary Consent Unblock could not be started.",
					  httpCode: "DHDR10.01",
					  severity: "error",
					  dateTime: new Date(),
					  moreInformation: (detail ? detail + " " : "")
						  + "The patient's consent block remains in force and no drug or pharmacy service"
						  + " information was retrieved. Try again; if it keeps failing, report it to your"
						  + " EMR administrator with the time shown here."
				  });
			  };

    		  $scope.callConsentBlock = function($event){
				  	$scope.buttonDisabled = true;
    				dhdrService.getConsentOveride($scope.demographicNo, "PCOI").then(function(response){
						$scope.buttonDisabled = false;
    					if(response.status == 268){
    						// DHDR14.01: "check the log" is not direction a clinician can act on, and the log is
    						// admin-gated - the requirement states the EMR user MUST NOT need admin role access
    						// to be notified of an error. PHI-free: the summary is a fixed notice, and the
    						// service's own response stays in the audit row.
    						viewletLaunchFailed(response.data ? response.data.summary : null);
    						return;
    					}
    					
    					var med = response.data;

							if (!med || !med.referenceURL) {
								viewletLaunchFailed("The service returned no viewlet address.");
								return;
							}
    					
    					//window.open(med.referenceURL);  only for testing in chrome
    					
    					
    					var modalInstance = $modal.open({
    		    		      
    		    		      templateUrl: 'pcoi.html',
    		    		      controller: 'PcoiInstanceCtrl',
    		    		      controllerAs: 'mpcoi',
    		    		      parent: angular.element(document.body),
    		    		      size: 'lg',
    		    		      appendTo: $event,
    		    		      resolve: {
    		    		    	  	
    		    		    	  		med: function () {
    		    		          		return med;
    		    		        		}
    		    		      }
    		    		    });
    					// DHDR11.01, DHDR15.02: record the outcome of the consent viewlet through the shared
    					// ONE ID reporting endpoint, the same one every other EHR service launch reports to.
    					//
    					// onRecorded runs once the audit write has been answered. DHDR15.02 makes that row the
    					// record that the unblock happened, so a caller that re-queries on the strength of the
    					// outcome waits for it: re-querying first risks disclosing temporarily unblocked data
    					// with nothing recording the decision that released it.
    					//
    					// The callback arrives from a plain XMLHttpRequest, outside AngularJS, so anything it
    					// touches on the scope is applied through $timeout. The old path was an $http promise
    					// and was inside the digest for free.
    					var reportOutcome = function(status, message, onRecorded) {
    						reportViewletResult('<%=request.getContextPath()%>', $scope.demographicNo, 'PCOI',
    							med.uuid, status, message, function(recorded) {
    								$timeout(function() { onRecorded(recorded); });
    							});
    					};

    					modalInstance.result.then(function (completion) {
    						// The viewlet answered nothing: the clinician gave up on it through the "not
    						// responding" link. Reported as 'noresponse' rather than left unlogged - silence is
    						// not refusal, and the override may well have gone through at Ontario Health.
    						if (!completion) {
    							reportOutcome('noresponse', 'The consent viewlet was closed without a response.',
    								function() { $scope.callSearch(); });
    							return;
    						}

    						// DHDR11.01.a/b, DHDR15.02: audit the outcome that was observed, never a blanket
    						// successful override. The audit row is the evidence that an override happened, so
    						// asserting a success nobody observed is the one wrong answer here.
    						//
    						// Deliberately not a list of known statuses. Only 'success' means the DHDR override
    						// was confirmed; everything else is reported as it came back and claims nothing. The
    						// shared parser gains outcomes over time - a reply naming no service at all is moving
    						// out of 'success' into its own - and a status this page has not heard of must fall
    						// through to the cautious branch, not the confirmed one.
    						var cancelled = completion.cancelled;
    						// An error entry makes the whole reply a failure, even alongside a DHDR success
    						// entry. That is the shared parser's reading rather than the one this page used,
    						// and it is the more cautious of the two.
    						var failed = !cancelled && completion.status === 'failure';

    						reportOutcome(completion.status, completion.message, function(recorded) {
    							if (cancelled || failed) {
    								// The block stays on screen and there is nothing new to load.
    								return;
    							}
    							if (!recorded) {
    								$scope.overrideResultMessage = "The temporary consent unblock could not be "
    									+ "recorded, so Drug and Pharmacy Service Information was not re-loaded. "
    									+ "Please try again.";
    								return;
    							}
    							// DHDR11.02 + OAVF B.4.2.5 / B.5: re-load the DHDR query - it is the source of
    							// truth for whether access opened, and answers with the CONSENT_TEMP_UNBLOCK
    							// notice. An unrecognised-but-successful response still surfaces the data here,
    							// it just was not logged as a confirmed override.
    							$scope.callSearch();
    						});

    						if (cancelled) {
    							// Cancelled inside the viewlet (OAVF utility PCOI_CONSENT_CANCELLED); the
    							// existing block stays shown.
    							return;
    						}
    						if (failed) {
    							// DHDR11.01.b: the override did not complete - leave the existing block shown
    							// and inform the user.
    							$scope.overrideResultMessage = "The temporary consent unblock did not complete. "
    								+ "Access to Drug and Pharmacy Service Information remains blocked.";
    						}
    		    		    }, function () {
    		    		      // No resolving message (backdrop / esc close): nothing was observed, so nothing is
    		    		      // claimed. Per OAVF B.4.2.5 the search still re-runs in case the override succeeded.
    		    		      reportOutcome('noresponse', 'The consent viewlet was dismissed without a response.',
    		    		        function() { $scope.callSearch(); });
    		    		    });
    					
    				},function(reason){
						$scope.buttonDisabled = false;
    					viewletLaunchFailed(null);
    				});
    				
    		  }
	
			
			
			
		});
		
		function OperationOutcome(operationOutcome){

			this.outcomme = operationOutcome;
			this.issues = [];
			var resource = angular.isDefined(this.outcomme.resource) ? this.outcomme.resource : this.outcomme;
			if(angular.isDefined(resource.issue)){
				this.issues = resource.issue;
			}
			// DHDR15.02: correlation id for a consent override logged from this notice.
			this.id = angular.isDefined(resource.id) ? resource.id : null;
			// DHDR14.01 incident time: the service's own timestamp when it supplied one.
			this.receivedAt = (resource.meta && resource.meta.lastUpdated)
				? new Date(resource.meta.lastUpdated) : new Date();
				
			
			
		}

		
		function MedicationDispense(medication){
			this.med = medication;
			this.hide = false;
			
			/* uniq value
			 a) Generic name of the dispensed drug [Medication.code.coding[2].display]
			 b) Dispensed drug strength [Medication.extension[1].valueString]
			 c) Drug dosage form (e.g., tablet, capsule, injection) [Medication.form.coding.display]
			*/
			this.getUniqVal = function(){
				 return this.genericName+":"+this.dispensedDrugStrength+":"+this.drugDosageForm;
			 }
			
			 
			 /*
<pre>

Prescriber Information
g) Prescriber ID (e.g., practitioner license or CPSO number) [Practitioner.identifier.value]
h) ID Reference [Practitioner.identifier.system]
Pharmacy Information
i) Pharmacist Name [Practitioner.name.given] [Practitioner.name.family]
j) Pharmacy Phone Number [Organization.telecom[1].value]
</pre>
			 
			 */
			
			// DHDR06.01(d) names the Current Rx Number. The consumer profile carries identifier 1..1, so a
			// conformant response holds only that one - but the submission profile allows a second (the
			// Original Rx Number), the two are told apart only by the trailing NamingSystem id, and array
			// order is not guaranteed. Taking whichever was serialised last could therefore display the
			// original as though it were the current one.
			//
			// Three suffixes occur: the IG examples and the FHIR-to-CDS mapping use "-rx-number", while
			// the submission profile's invariant permits "-current-rx-number" and "-original-rx-number".
			// So prefer an explicit current one, otherwise take anything that is not explicitly the
			// original, and only fall back to the original if it is all there is. Matched on the suffix
			// for the same reason as the contained Medication codings below.
			if(angular.isDefined(this.med.resource.identifier)){
				var rxFallback;
				for (ident of  this.med.resource.identifier) {
					if(!angular.isDefined(ident.value)){
						continue;
					}
					var system = angular.isDefined(ident.system) ? ident.system : "";
					if(system.endsWith("-current-rx-number")){
						this.rxNumber = ident.value;
						break;
					}
					if(!system.endsWith("-original-rx-number")){
						// "-rx-number", or no system at all. First one wins, so the choice is deterministic.
						if(!angular.isDefined(this.rxNumber)){
							this.rxNumber = ident.value;
						}
					}else if(!angular.isDefined(rxFallback)){
						rxFallback = ident.value;
					}
				}
				if(!angular.isDefined(this.rxNumber)){
					this.rxNumber = rxFallback;
				}
			} 
			 
			 /*
			 "extension": [
          {
            "url": "http://ehealthontario.ca/fhir/StructureDefinition/ca-on-medications-ext-refills-remaining",
            "valueInteger": 1
          },
          {
            "url": "http://ehealthontario.ca/fhir/StructureDefinition/ca-on-medications-ext-quantity-remaining",
            "valueQuantity": {
              "value": 120,
              "unit": "tsp",
              "system": "http://snomed.info/sct",
              "code": "SOL"
            }
          }
        ],
        */
			
			if(angular.isDefined(this.med.resource.extension)){
				
				for (ext of  this.med.resource.extension) {
					if(angular.isDefined(ext.url) && ext.url === "http://ehealthontario.ca/fhir/StructureDefinition/ca-on-medications-ext-refills-remaining"){
						
						this.refillsRemaining = ext.valueInteger;
					}else if(angular.isDefined(ext.url) && ext.url === "http://ehealthontario.ca/fhir/StructureDefinition/ca-on-medications-ext-quantity-remaining"){
						this.quantityRemaining = ext.valueQuantity.value+" "+ext.valueQuantity.unit;
					}
				}
			}
        
			// MedicationDispense.status is 1..1 in FHIR and mustSupport in the DHDR profile, so it
			// cannot be left unread. The value set is not fixed to "completed": the FHIR-to-CDS mapping
			// declares the full medication-dispense-status set, which includes cancelled,
			// entered-in-error, stopped and declined. An event in one of those states is NOT a valid
			// dispense, and rendering it like one puts a medication in the history that was reversed or
			// recorded in error.
			this.status = this.med.resource.status;
			// Anything other than completed needs to be visible rather than silently normal. Kept as a
			// derived flag so the templates do not each re-encode which tokens count as valid.
			this.statusNotCompleted = angular.isDefined(this.status)
					&& this.status !== null && String(this.status).toLowerCase() !== "completed";

			this.whenPrepared = this.med.resource.whenPrepared;
			if (angular.isDefined(this.med.resource.whenHandedOver)) {
				// Drug tables bind pickUpDate, pharmacy tables bind whenHandedOver; same source.
				this.pickUpDate = this.med.resource.whenHandedOver;
				this.whenHandedOver = this.med.resource.whenHandedOver;
			}
			if(angular.isDefined(this.med.resource.quantity) && angular.isDefined(this.med.resource.quantity.value)){
				this.dispensedQuantity = this.med.resource.quantity.value;
				this.dispensedQuantityUnit = this.med.resource.quantity.unit;
			}
			if(angular.isDefined(this.med.resource.daysSupply) && angular.isDefined(this.med.resource.daysSupply.value)){
				this.estimatedDaysSupply = this.med.resource.daysSupply.value;
			}
			if(angular.isDefined(this.med.resource.reasonCode)){
				this.reasonCode = this.med.resource.reasonCode;
			}
			if(angular.isDefined(this.med.resource.category) && angular.isDefined(this.med.resource.category.coding)){
				for(coding of this.med.resource.category.coding) {

					// DHDR-02: route on the OH dispense-category code. The normative code set
					// (device/drug/product/service) is stable, but the v4.0.3 IG emits this category
					// under three different eHealth Ontario system URLs across its own search examples:
					//   .../NamingSystem/ca-on-medication-dispense-category   (legacy)
					//   .../CodeSystem/medication-dispense-category           (transitional)
					//   .../CodeSystem/dispense-category                      (canonical)
					// Accept the code from any of them (tolerant to OH's URL churn and to future code
					// additions), but ignore codings from other categorization systems - e.g. the HL7
					// base medicationdispense-category, whose tokens (inpatient/outpatient/...) differ -
					// so a foreign system's code is never read as ours and mis-routes an event.
					if(angular.isDefined(coding.system)
							&& coding.system.startsWith("http://ehealthontario.ca/fhir/")
							&& coding.system.endsWith("dispense-category")) {
						this.categoryCode = coding.code;
						this.categoryDisplay = coding.display
					}
				}

			}

			// Every level here is optional in the consumer profile, and each one that was assumed
			// present threw during construction. In the summary path processEntries catches that and
			// counts the record as unreadable; in the comparative path (searchByDemographicNo2)
			// nothing does, so the throw escapes the loop and takes every later entry with it - and
			// because it throws inside a .then success handler, the sibling error callback never
			// fires and the reader is told nothing. The three shapes that did it are all legal FHIR
			// we simply had never been sent: an empty dosageInstruction array, a dose expressed as
			// doseRange instead of doseQuantity (dose[x] is a choice type), and a timing with no
			// repeat (Timing.repeat is 0..1). A missing dose or frequency must leave those cells
			// blank, not lose the event.
			// != null throughout: it rejects both null and undefined while still admitting a value of
			// 0, which a truthiness test would drop.
			var dosageInstruction = angular.isArray(this.med.resource.dosageInstruction)
					? this.med.resource.dosageInstruction[0] : null;
			if (dosageInstruction != null) {
				var doseAndRate = angular.isArray(dosageInstruction.doseAndRate)
						? dosageInstruction.doseAndRate[0] : null;
				var doseQuantity = doseAndRate != null ? doseAndRate.doseQuantity : null;
				if (doseQuantity != null) {
					this.dose = doseQuantity.value != null ? doseQuantity.value : "";
					// Guard first, then prepend the space inside the value: + binds tighter than ?:.
					this.doseUnit = doseQuantity.unit != null ? (" " + doseQuantity.unit) : "";
				}
				var repeat = dosageInstruction.timing != null ? dosageInstruction.timing.repeat : null;
				if (repeat != null) {
					this.frequency = repeat.frequency;
					this.period = repeat.period;
					this.periodMax = repeat.periodMax;
					this.periodUnit = repeat.periodUnit;
				}
			}

			
			// contained is optional: a dispense can carry no Medication/Practitioner/Patient at all.
			var contained = angular.isDefined(this.med.resource.contained) ? this.med.resource.contained : [];
			for (res of contained) {
				
				if(res.resourceType === "Medication") {
					
					if(res.code != null) {
						
						if(angular.isDefined(res.form) && angular.isDefined(res.form.text)){
							this.drugDosageForm = res.form.text	
						}
						
						if(angular.isDefined(res.extension)){
							for(ext of res.extension) {
								if("http://ehealthontario.ca/fhir/StructureDefinition/ca-on-medications-ext-medication-strength" === ext.url){
									this.dispensedDrugStrength = ext.valueString;
								}
							}
						}
				
						
						if (angular.isDefined(res.code.coding)) {
							for(coding of res.code.coding) {
								if(!angular.isDefined(coding.system)) {
									continue;
								}
								// Match on the trailing NamingSystem id, not the full URL: the consumer profile
								// fixes these to https:// while the IG examples use http://.
								if(coding.system.endsWith("ca-drug-gen-name")) {
									this.genericName = coding.display;
								}else if(coding.system.endsWith("ca-on-drug-class-ahfs")) {
									this.ahfsClass = coding.display;
								}else if(coding.system.endsWith("ca-on-drug-subclass-ahfs")) {
									this.ahfsSubClass = coding.display;
								}else if(coding.system.endsWith("ca-hc-din")) {
									// Health Canada DIN - the preferred brand identifier.
									this.brandName = coding;
								}else if(angular.isDefined(coding.code) && !angular.isDefined(this.brandName)) {
									// A non-DIN identifier (PIN, or a CCDD product code such as a COVaxON
									// immunization). Only when no DIN was captured, so a DIN always wins.
									this.brandName = coding;
								}

							}
						}
					}else {
						// Never interpolate the resource here - it carries PHI.
						console.warn("DHDR: contained Medication resource has no code element");
					}
				
				}else if(res.resourceType ===  "Organization") {
					this.dispensingPharmacy = res.name;
					if(angular.isDefined(res.telecom)){
						for(tele of res.telecom){
							if("fax" === tele.system){
								this.dispensingPharmacyFaxNumber = tele.value;		
							}
							if("phone" === tele.system){
								this.dispensingPharmacyPhoneNumber = tele.value;		
							}
	
						}
					}
				}else if(res.resourceType ===  "MedicationRequest") {
					this.reasonCode = [];
					if(angular.isDefined(res.reasonCode)){
						for(code of res.reasonCode){	
							// Push, so reasonCode stays an array - the print reads it with getJSONArray.
							this.reasonCode.push(code);
						}
					}	
					
				}else if(res.resourceType ==="Practitioner") {
					
					if (angular.isDefined(res.identifier)) {
						for(identifier of res.identifier) {
							if(!angular.isDefined(identifier.system)) {
								continue;
							}
							if(identifier.system.endsWith("ca-on-license-pharmacist")) {
								this.pharmacistLicenceNumber = identifier;
								if(angular.isDefined(res.name)){
									for( humanName of res.name) {
										this.pharmacistLastname = humanName.family;
										if(angular.isDefined(humanName.given)){
											this.pharmacistFirstname = humanName.given[0];
										}
									}
								}
							}else if(identifier.system.indexOf("ca-on-license") !== -1
									|| identifier.system.indexOf("ca-on-registration") !== -1
									|| identifier.system.indexOf("prescriber") !== -1) {
								// Any prescriber licensing college, not physicians only. Pharmacist handled above.
								// Not every college uses the "license" stem - the IG's practitioner-identifier
								// CodeSystem spells the chiropodist one ca-on-registration-chiropodist, and
								// matching only "ca-on-license" dropped that prescriber's name and licence
								// entirely rather than merely mislabelling the college.
								this.prescriberLicenceNumber = identifier;
								if(angular.isDefined(res.name)){
									for( humanName of res.name) {
										this.prescriberLastname = humanName.family;
										if(angular.isDefined(humanName.given)){
											this.prescriberFirstname = humanName.given[0];
										}
									}
								}

								if (angular.isDefined(res.telecom)) {
									for(tele of res.telecom){
										if("phone" === tele.system){
											this.prescriberPhoneNumber = tele.value;
										}
									}
								}
							}

						}
					}
				} else if (res.resourceType === "Patient") {
					this.patient = res;
				} else {
				}

			}

			// DHDR07.01(c)(d) and DHDR07.02 name MedicationDispense.type as the source of the pharmacy
			// service type and description, and as the grouping key. No profile in either published IG
			// constrains that element and no example populates it, so nothing can be read from it today -
			// but the requirement is on us to read it, not on the service to send it. Read it when it
			// arrives; otherwise leave the contained-Medication codings the parse above already set,
			// which is what the Business View's Figure 4 illustrates for these same two columns.
			//
			// Scoped to service events: on a drug dispense these fields are Brand and Generic Name, which
			// DHDR04.01 sources from the contained Medication and which this element does not govern.
			if (this.categoryCode === "service" && angular.isDefined(this.med.resource.type)) {
				var dispenseType = this.med.resource.type;
				var typeCodings = angular.isDefined(dispenseType.coding) ? dispenseType.coding : [];
				// Prefer coding.display over the sibling text: display is what the service columns render
				// everywhere else, so a populated type reads consistently with a fallback one.
				var typeDisplay = (typeCodings.length > 0 && angular.isDefined(typeCodings[0].display))
						? typeCodings[0].display
						: dispenseType.text;
				if (angular.isDefined(typeDisplay) && typeDisplay !== null && (""+typeDisplay).trim() !== "") {
					// Carries a display and deliberately no code, unlike the coding object the DIN branch
					// assigns. The print appends brandName.code to the service type labelled "PIN:", and a
					// service-type code is not a product identifier - it would print
					// "MedsCheck (PIN: MEDSCHECK)". DHDR07.01 lists no product identifier for a pharmacy
					// service event at all, so there is nothing to preserve here, only a wrong label to
					// avoid. Keeping the brandName key means the display bindings, the filter and sort
					// keys and the DHDR07.02 grouping key still follow without touching the templates.
					this.brandName = { display: typeDisplay };

					// (d) is sourced from this same element by the requirement, so it is only reachable
					// when (c) was. Where a second coding is present take it as the description; a
					// single-coding type leaves the generic-name fallback rather than blanking the column.
					if (typeCodings.length > 1 && angular.isDefined(typeCodings[1].display)) {
						this.genericName = typeCodings[1].display;
					}
				}
			}

		}

		app.controller('ModalInstanceCtrl', function ModalInstanceCtrl($scope, $modal, $modalInstance, med, demoNo,
																	   dhdrPatient, $http){
			$scope.med = med;
			$scope.dhdrPatient = dhdrPatient;
			
			$scope.cancel = function(){
				
				$modalInstance.close(false);	
			}
			
			$scope.getLicence = function(val){
				if(val == null){
					return "N/A";
				}
				
				if(val.endsWith("ca-on-license-physician")){
					return "College of Physicians and Surgeons of Ontario";
				}else if(val.endsWith("ca-on-license-dental-surgeon")){
					return "Royal College of Dental Surgeons of Ontario";
				}else if(val.endsWith("ca-out-of-province-prescriber")){
					return "Out-of-Province Prescriber";
				}else if(val.endsWith("ca-on-registration-chiropodist")){
					return "College of Chiropodists of Ontario";
				}else if(val.endsWith("ca-on-license-midwife")){
					return "College of Midwives of Ontario";
				}else if(val.endsWith("ca-on-license-pharmacist")){
					return "Ontario College of Pharmacists";
				}else if(val.endsWith("ca-on-license-optometrist")){
					return "College of Optometrists of Ontario";
				}else if(val.endsWith("ca-on-license-nurse")){
					return "College of Nurses of Ontario";
				}else if(val.endsWith("ca-on-license-naturopath")){
					return "College of Naturopaths of Ontario";
				}else if(val.endsWith("ca-on-unknown-prescriber")){
					return "Unknown Prescriber";
				}
				return "N/A";
			}
			
			// Licensing-body mnemonic for the GUI, with the full name on hover. Only well-established
			// abbreviations; anything else falls back to the full body name.
			$scope.getLicenceMnemonic = function(val){
				if(val == null){ return "N/A"; }
				if(val.endsWith("ca-on-license-physician")){ return "CPSO"; }
				if(val.endsWith("ca-on-license-dental-surgeon")){ return "RCDSO"; }
				if(val.endsWith("ca-on-license-pharmacist")){ return "OCP"; }
				if(val.endsWith("ca-on-license-nurse")){ return "CNO"; }
				if(val.endsWith("ca-on-license-midwife")){ return "CMO"; }
				if(val.endsWith("ca-on-license-optometrist")){ return "COO"; }
				if(val.endsWith("ca-on-license-naturopath")){ return "CONO"; }
				// No widely-used abbreviation - show the full body name instead.
				return $scope.getLicence(val);
			}

			$scope.printDetail = function(){
					$scope.printError = null;
					var toPrint = {};
					toPrint.med = $scope.med;
					// DHDR13.01(b) / BP14: this view is scoped to ONE event, so the DHDR-side identity on the
					// paper must be that event's own contained Patient - the same one the modal shows above,
					// with its per-field match flags - not the result set's headline. One HCN search can
					// legitimately return several recorded identities, which is why the banner collects
					// variants; sending the headline made the printout assert the wrong one for a divergent
					// event and drop the mismatch flag with it. eventPatient carries the same five fields
					// buildDhdrDemoLine reads, plus the flags the print now renders.
					toPrint.dhdrPatient = $scope.med.eventPatient;

					$http.post('../ws/rs/dhdr/'+demoNo+'/print/detail',toPrint,{ responseType: 'arraybuffer' }).then(function (response) {
						
					       var file = new Blob([response.data], {type: 'application/pdf'});
					       var fileURL = URL.createObjectURL(file);
					       window.open(fileURL);
					}, function(errorMessage) {
						// Reported on the modal itself: this runs in a $modal scope, which cannot see the
						// controller's serviceErrors list. Same reason the patient block had to be decorated
						// onto the event rather than exposed as a controller helper.
						$scope.printError = "The Detailed printout could not be produced. The record shown here"
							+ " is unaffected. Try again; if it keeps failing, report it to your EMR"
							+ " administrator, noting the time.";
					});	
					//window.open('../ws/rs/dhdr/'+$scope.demographicNo+'/print/summary','_blank');
			
			}
			
		});
		
		app.controller('DrugDupsInstanceCtrl', function ModalInstanceCtrl($scope, $modal, $modalInstance,meds,getDetailView,$http){
			$scope.meds = meds;
			$scope.getDetailView = getDetailView;
			
			$scope.cancel = function(){
				
				$modalInstance.close(false);	
			}
			
			
			
		});
		
		app.controller('PharmaDupsInstanceCtrl', function ModalInstanceCtrl($scope, $modal, $modalInstance,services,getDetailView,$http){
			$scope.services = services;
			$scope.getDetailView = getDetailView;
			
			$scope.cancel = function(){
				
				$modalInstance.close(false);	
			}
			
			
			
		});
		
		
		
		
		app.controller('PcoiInstanceCtrl', function ModalInstanceCtrl($scope, $modal, $modalInstance,med,$sce,$window,$http,$timeout){
			// MessageEvent.origin is scheme + host + port only, never a path. The viewlet URL keeps
			// its path (.../main), so stripping only the query string left a value that could not
			// equal e.origin - the message was discarded and DHDR11.01 / DHDR15.02 never ran.
			const PCOI_ORIGIN_URL = (function(rawUrl) {
				try {
					return new URL(rawUrl, $window.location.href).origin;
				} catch (e) {
					var parser = document.createElement('a');
					parser.href = rawUrl;
					return parser.protocol + '//' + parser.host;
				}
			})(med.referenceURL);

			var onPcoiMessage = function(e) {
				if (e.origin !== PCOI_ORIGIN_URL) {
					return;
				}
				// Only a completion message ends the launch. The viewlet's own origin can post
				// other messages, and closing on the first of those would end the override while
				// the clinician was still working through it. parseViewletCompletion answers null
				// for anything that is not a completion, which is the test used here.
				var completion = parseViewletCompletion(e.data, 'DHDR');
				if (completion) {
					$modalInstance.close(completion);
				}
			};
			$window.addEventListener('message', onPcoiMessage);
			// The listener outlives the modal otherwise, and each unblock attempt adds another one
			// holding a closed $modalInstance.
			$scope.$on('$destroy', function() {
				$window.removeEventListener('message', onPcoiMessage);
			});

			$scope.showUntilLoaded = true;
			$scope.viewletNotResponding = false;
			

			$timeout(function() {
				$scope.viewletNotResponding = true;
			}, <%= viewletTimeout %>); <%-- DHDR-04: PCOI viewlet not-responding timeout, viewlet_timeout pref in seconds times the modal multiple (default 300000) --%>
			
			
			
			$scope.med = med;
			$scope.pcoiUrl = $sce.trustAsResourceUrl(med.referenceURL);
		
			$scope.reload = function(){
				
				$scope.pcoiUrl = $sce.trustAsResourceUrl(med.referenceURL);
			}
		
			$scope.loadingResult = function(e){
				$scope.showUntilLoaded = false;
				$scope.$apply();
			}
			
			$scope.cancel = function(){
				
				$modalInstance.close(false);	
			}
		});
		
		app.directive("ngOnload", function elementOnloadDirective() {
	        return {
	            restrict: "A",
	            scope: {
	                callback: "&ngOnload"
	            },
	            link: function link(scope, element, attrs) {
	                // hooking up the onload event - calling the callback on load event.
	                //
	                // bind, not one: jqLite gained one() in AngularJS 1.4 and this page is on 1.2.3, so
	                // one() threw "element.one is not a function" out of link and the handler was never
	                // attached at all - which is why the "Failed to load? click here" link never cleared
	                // once the viewlet had plainly loaded. Firing on every load is right here anyway,
	                // since reload() re-points the frame and clearing the notice again is harmless.
	                element.bind("load", function (state,message) {
	                	
	                    var contentLocation = element.length > 0 && element[0].contentWindow ? element[0].contentWindow.location : undefined;
	                    scope.callback({
	                        contentLocation: contentLocation
	                    });
	                });
	            }
	        };
	    });
	
		
	</script>
	</body>
</html>	    			