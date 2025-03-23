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

<!-- this CSS makes it so the modals don't have the vertical sliding animation. Not sure if I will keep this or how I will use this yet -->
<style>
    .modal.fade {
        opacity: 1;
    }

    .modal.fade .modal-dialog, .modal.in .modal-dialog {
        -webkit-transform: translate(0, 0);
        -ms-transform: translate(0, 0);
        transform: translate(0, 0);
    }

    .state1 {
    }

    .state2 {
        background-color: #e6e6e6 !important;
    }

    /*#f5f5f5*/
    .state3 {
        background-color: #d9d9d9 !important;
    }

    /*#e6e6e6*/
    .state6 {
        background-color: #cccccc !important;
    }

    /*cccccc*/

    .table tbody tr:hover td, .table tbody tr:hover th {
        background-color: #FFFFAA;
    }
</style>
<fmt:setBundle basename="uiResources" var="uiBundle"/>
<div ng-show="consultReadAccess" class="col-lg-12">


    <form name="searchForm" id="searchForm">

        <div class="row">
            <div class="col-xs-2">
                <input ng-model="search.referralStartDate" type="text"
                       id="referralStartDate" name="referralStartDate"
                       class="form-control" uib-datepicker-popup="yyyy-MM-dd"
                       datepicker-append-to-body="true" is-open="data.isOpen"
                       ng-click="data.isOpen = true"
                       placeholder="<fmt:message bundle="${uiBundle}" key="consult.list.referralStartDate"/>">
            </div>
            <div class="col-xs-2">
                <input ng-model="search.referralEndDate" type="text"
                       id="referralEndDate" name="referralEndDate" class="form-control"
                       uib-datepicker-popup="yyyy-MM-dd" datepicker-append-to-body="true"
                       is-open="data2.isOpen" ng-click="data2.isOpen = true"
                       placeholder="<fmt:message bundle="${uiBundle}" key="consult.list.referralEndDate"/>">
            </div>
            <div class="col-xs-2">
                <select class="form-control" ng-model="search.status" name="status" id="status"
                        ng-options="status.value as status.name for status in statuses">
                    <option value=""><fmt:message bundle="${uiBundle}" key="consult.list.status.all"/></option>
                </select>
            </div>
            <div class="col-xs-2">
                <select ng-model="search.team" name="team" id="team"
                        class="form-control"
                        ng-init="search.team='<fmt:message bundle="${uiBundle}" key="consult.list.team.all"/>'"
                        ng-options="t for t in teams">
                </select>
            </div>
        </div>


        <div style="height: 5px"></div>

        <div class="row">
            <div class="col-xs-2">
                <input ng-model="search.appointmentStartDate" type="text"
                       id="appointmentStartDate" name="appointmentStartDate"
                       class="form-control" uib-datepicker-popup="yyyy-MM-dd"
                       datepicker-append-to-body="true" is-open="data3.isOpen"
                       ng-click="data3.isOpen = true"
                       placeholder="<fmt:message bundle="${uiBundle}" key="consult.list.appointmentStartDate"/>">
            </div>
            <div class="col-xs-2">
                <input ng-model="search.appointmentEndDate" type="text"
                       id="appointmentEndDate" name="appointmentEndDate"
                       class="form-control" uib-datepicker-popup="yyyy-MM-dd"
                       datepicker-append-to-body="true" is-open="data4.isOpen"
                       ng-click="data4.isOpen = true"
                       placeholder="<fmt:message bundle="${uiBundle}" key="consult.list.appointmentEndDate"/>">
            </div>

            <div class="col-xs-2" ng-hide="hideSearchPatient">
                <div class="input-group">
                    <div class="input-group-addon"><span class="glyphicon glyphicon-remove"
                                                         ng-click="removeDemographicAssignment()"></span></div>
                    <input type="text" ng-model="consult.demographicName"
                           placeholder="<fmt:message bundle="${uiBundle}" key="consult.list.patient"/>"
                           typeahead="pt.demographicNo as pt.name for pt in searchPatients($viewValue)"
                           typeahead-on-select="updateDemographicNo($item, $model, $label)"
                           class="form-control"/>
                </div>
            </div>

            <div class="col-xs-2">
                <div class="input-group">
                    <div class="input-group-addon"><span class="glyphicon glyphicon-remove"
                                                         ng-click="removeMrpAssignment()"></span></div>
                    <input type="text" ng-model="consult.mrpName"
                           placeholder="<fmt:message bundle="${uiBundle}" key="consult.list.mrp"/>"
                           typeahead="pvd as pvd.name for pvd in searchMrps($viewValue)"
                           typeahead-on-select="updateMrpNo($model)"
                           class="form-control"/>
                </div>
            </div>
        </div>

        <div style="height: 5px"></div>

        <div class="row">
            <div class="col-xs-12">
                <button class="btn btn-primary" type="button" ng-click="doSearch()">
                    <fmt:message bundle="${uiBundle}" key="global.search"/>
                </button>
                <button class="btn btn-default" type="button" ng-click="clear()">
                    <fmt:message bundle="${uiBundle}" key="global.clear"/>
                </button>
                <button class="btn btn-success" type="button" title=
                <fmt:message bundle="${uiBundle}" key="consult.list.newRemindFill"/> ng-click="addConsult()"
                        ng-disabled="search.demographicNo==null">
                    <fmt:message bundle="${uiBundle}" key="consult.list.newResponse"/>
                </button>
            </div>
        </div>
    </form>

    <div style="height: 15px"></div>

    <table ng-table="tableParams" show-filter="false" class="table">
        <tbody>

        <tr ng-repeat="consult in $data" ng-mouseover="consult.$selected=true" ng-mouseout="consult.$selected=false"
            ng-class="{'active': consult.$selected}" class="state{{consult.status}}">

            <td>
                <!-- Ronnie: Temporarily hidden until batch operations is created
                                    <input type="checkbox" ng-model="consult.checked">
                 -->
            </td>
            <td><a
                    ng-click="editConsult(consult)" class="hand-hover"><fmt:message bundle="${uiBundle}" key="global.edit"/></a>
            </td>
            <td data-title="'<fmt:message bundle="${uiBundle}" key="consult.list.header.patient"/>'" sortable="'Demographic'">
                {{consult.demographic.formattedName}}
            </td>
            <td data-title="'<fmt:message bundle="${uiBundle}" key="consult.list.header.referringDoctor"/>'"
                sortable="'ReferringDoctor'">{{consult.referringDoctor.formattedName}}
            </td>
            <td data-title="'<fmt:message bundle="${uiBundle}" key="consult.list.header.team"/>'" sortable="'Team'">
                {{consult.teamName}}
            </td>
            <td data-title="'<fmt:message bundle="${uiBundle}" key="consult.list.header.status"/>'" sortable="'Status'">
                {{consult.statusDescription}}
            </td>
            <td data-title="'<fmt:message bundle="${uiBundle}" key="consult.list.header.priority"/>'"
                class="{{consult.urgencyColor}}" sortable="'Urgency'">{{consult.urgencyDescription}}
            </td>
            <td data-title="'<fmt:message bundle="${uiBundle}" key="consult.list.header.mrp"/>'" sortable="'Provider'">
                {{consult.provider.formattedName}}
            </td>

            <td data-title="'<fmt:message bundle="${uiBundle}" key="consult.list.header.appointmentDate"/>'"
                sortable="'AppointmentDate'">
                {{consult.appointmentDate | date: 'yyyy-MM-dd HH:mm'}}
            </td>
            <td data-title="'<fmt:message bundle="${uiBundle}" key="consult.list.header.lastFollowUp"/>'"
                sortable="'FollowUpDate'">
                {{consult.lastFollowUp | date: 'yyyy-MM-dd'}}
            </td>
            <td data-title="'<fmt:message bundle="${uiBundle}" key="consult.list.header.referralDate"/>'"
                sortable="'ReferralDate'">
                {{consult.referralDate | date: 'yyyy-MM-dd'}}
            </td>
            <td data-title="'<fmt:message bundle="${uiBundle}" key="consult.list.header.responseDate"/>'"
                sortable="'ResponseDate'">
                {{consult.responseDate | date: 'yyyy-MM-dd'}}
            </td>
        </tr>
        </tbody>

        <tfoot>
        <!-- <tr>
				<td colspan="12" class="white">
					<a ng-click="checkAll()" class="hand-hover"><fmt:message bundle="${uiBundle}" key="consult.list.checkAll"/></a> - <a ng-click="checkNone()" class="hand-hover"><fmt:message bundle="${uiBundle}" key="consult.list.checkNone"/></a>								
				</td>
			</tr> -->
        </tfoot>

    </table>


    <!--
<pre>{{search}}</pre>
<pre>{{lastResponse}}</pre>
-->

</div>


<div ng-show="consultReadAccess != null && consultReadAccess == false"
     class="col-lg-12">
    <h3 class="text-danger">
        <span class="glyphicon glyphicon-warning-sign"></span><fmt:message bundle="${uiBundle}" key="consult.list.access_denied"/>
    </h3>
</div>

