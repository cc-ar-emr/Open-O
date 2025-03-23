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
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<style>
    input.checkStyle {
        background: #888 !important;
        border: none !important;
        color: #000 !important;
    }
</style>

<div ng-show="demographicReadAccess">


    <h2>
        <fmt:setBundle basename="uiResources" var="uiBundle"/>
        <fmt:message key="patientsearch.title" bundle="${uiBundle}"/>
    </h2>
    <div class="row">
        <div class="col-xs-6">
            <form role="form" class="form-inline">
                <div class="form-group">
                    <select ng-model="search.type" ng-change="clearButMaintainSearchType()" style="width:auto;"
                            class="form-control selectWidth" ng-init="search.type='Name'">
                        <option value="Name"><fmt:message key="patientsearch.type.name" bundle="${uiBundle}"/></option>
                        <option value="Phone"><fmt:message key="patientsearch.type.phone" bundle="${uiBundle}"/></option>
                        <option value="DOB"><fmt:message key="patientsearch.type.dob" bundle="${uiBundle}"/></option>
                        <option value="Address"><fmt:message key="patientsearch.type.address" bundle="${uiBundle}"/></option>
                        <option value="HIN"><fmt:message key="patientsearch.type.hin" bundle="${uiBundle}"/></option>
                        <option value="ChartNo"><fmt:message key="patientsearch.type.chartNo" bundle="${uiBundle}"/></option>
                        <option value="DemographicNo"><fmt:message key="patientsearch.type.demographicNo" bundle="${uiBundle}"/></option>
                    </select>
                    <div class="btn-group">
                        <a class="btn dropdown-toggle" data-toggle="dropdown">

                            <span class="caret"></span>
                        </a>
                        <ul class="dropdown-menu">
                            <li>
                                <input ng-model="search.active" type="checkbox" class="checkStyle"
                                       ng-init="search.active=true"/>&nbsp;<fmt:message
                                    key="patientsearch.showActiveOnly" bundle="${uiBundle}"/>

                            </li>
                            <li>
                                <input ng-model="search.integrator" ng-init="search.integrator=false" type="checkbox"
                                       class="checkStyle"/>&nbsp;<fmt:message key="patientsearch.includeIntegrator"
                                                                               bundle="${uiBundle}"/>
                            </li>
                            <li>
                                <input ng-model="search.outofdomain" ng-init="search.outofdomain=true" type="checkbox"
                                       class="checkStyle"/>&nbsp;<fmt:message key="patientsearch.outOfDomain"
                                                                               bundle="${uiBundle}"/>
                            </li>

                        </ul>
                    </div>

                    <input ng-model="search.term" type="text" class="form-control" style="width:auto"
                           placeholder={{searchTermPlaceHolder}} ng-init="search.term=''"/>
                    <button class="btn btn-primary" ng-click="doSearch()"><fmt:message key="global.search"
                                                                                        bundle="${uiBundle}"/></button>
                    <button class="btn" ng-click="doClear()"><fmt:message key="global.clear" bundle="${uiBundle}"/></button>

                </div>
            </form>
        </div>
        <div class="col-xs-6">
            <button class="btn btn-warning" ng-show="integratorResults != null && integratorResults.total > 0"
                    ng-click="showIntegratorResults()"><span
                    class="glyphicon glyphicon-exclamation-sign"></span><fmt:message key="patientsearch.remoteMatches"
                                                                                      bundle="${uiBundle}"/></button>
        </div>
    </div>

    <div style="height:10px"></div>


    <table ng-table="tableParams" show-filter="false" class="table">
        <tbody>
        <tr ng-repeat="patient in $data" ng-mouseover="patient.$selected=true" ng-mouseout="patient.$selected=false"
            ng-class="{'active': patient.$selected}" ng-click="loadRecord(patient.demographicNo)">

            <td data-title="'<fmt:message key="patientsearch.header.id" bundle="${uiBundle}"/>'" sortable="'DemographicNo'">
                {{patient.demographicNo}}
            </td>
            <td data-title="'<fmt:message key="patientsearch.header.name" bundle="${uiBundle}"/>'" sortable="'Name'">
                {{patient.lastName}}, {{patient.firstName}}
            </td>
            <td data-title="'<fmt:message key="patientsearch.header.chartNo" bundle="${uiBundle}"/>'" sortable="'ChartNo'">
                {{patient.chartNo}}
            </td>
            <td data-title="'<fmt:message key="patientsearch.header.gender" bundle="${uiBundle}"/>'" class="text-center"
                sortable="'Sex'">
                {{patient.sex}}
            </td>
            <td data-title="'<fmt:message key="patientsearch.header.dob" bundle="${uiBundle}"/>'" class="text-center"
                sortable="'DOB'">
                {{patient.dob | date: 'yyyy-MM-dd'}}
            </td>
            <td data-title="'<fmt:message key="patientsearch.header.doctor" bundle="${uiBundle}"/>'" sortable="'ProviderName'">
                {{patient.providerName}}
            </td>
            <td data-title="'<fmt:message key="patientsearch.header.rosterStatus" bundle="${uiBundle}"/>'" class="text-center"
                sortable="'RS'">
                {{patient.rosterStatus}}
            </td>
            <td data-title="'<fmt:message key="patientsearch.header.patientStatus" bundle="${uiBundle}"/>'" class="text-center"
                sortable="'PS'">
                {{patient.patientStatus}}
            </td>
            <td data-title="'<fmt:message key="patientsearch.header.phone" bundle="${uiBundle}"/>'" sortable="'Phone'">
                {{patient.phone}}
            </td>
        </tr>
        </tbody>

    </table>

    <!--
   <pre>{{search}}</pre>

   <pre>{{lastResponse}}</pre>
   -->

</div>


<div ng-show="demographicReadAccess != null && !demographicReadAccess">
    <h3 class="text-danger"><span class="glyphicon glyphicon-warning-sign"></span><fmt:message
            key="patientsearch.access_denied" bundle="${uiBundle}"/></h3>
</div>