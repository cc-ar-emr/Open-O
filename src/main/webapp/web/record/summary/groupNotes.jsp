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

<!-- #996633 cpp -->
<!-- #006600 famhx -->
<!-- #306754 othermeds -->
<!-- not sure colours are staying -->
<!--ng-style="page.code == 'famhx' && {'background-color' : '#006600'} || page.code == 'othermeds' && {'background-color' : '#306754'}"-->
<style>
    .group-note-selected {
        background-color: yellow;
    }
</style>

<!-- make div layout more fluid see medical history as an example -->
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<div class="modal-content" style="background-color: #996633; color: white;   border:0px;  border-radius: 0px;"
     ng-click="checkAction($event)" ng-keypress="checkAction($event)">

    <div class="modal-header"> <!-- ng-style="setColor(note.cpp)" -->
        <button type="button" class="close" data-dismiss="modal" aria-label="<fmt:setBundle basename="oscarResources"/><fmt:message key="global.close"/>"
                ng-click="cancel()"><span aria-hidden="true">&times;</span></button>
        <h4 class="modal-title">{{page.title}}</h4>
    </div>

    <div class="modal-body" style="background-color:#fff;color:#333">

        <!-- TODO: what happens here if there is a long list of notes??? -->
        <div class="well well-note">
            <ul data-brackets-id="12674" id="sortable" class="list-unstyled ui-sortable">


                <li class="cpp-note-list" ng-repeat="item in page.items" ng-click="changeNote(item,item.id)"
                    ng-class="isSelected(item)">
                    <small class="pull-left text-muted">{{item.editor}}</small>
                    <small class="pull-right text-muted">
                        <span class="glyphicon glyphicon-calendar"></span> {{item.date | date: 'dd-MMM-yyyy'}}
                    </small>
                    <br>
                    {{item.displayName}} <small ng-show="item.classification">({{item.classification}})</small>
                </li>
                <span class="text-muted" ng-if="page.items==null">No entries</span>
            </ul>
        </div><!-- well -->

        <hr>

        <div id="form-wrapper"> <!-- ng-if="action == 'add'" -->

            <form class="form-inline" id="frmIssueNotes">

                <!-- hidden not needed -->
                <input type="hidden" id="issueChange" name="issueChange" value="">

                <input type="hidden" id="annotation_attrib" name="annotation_attrib"
                       ng-model="groupNotesForm.encounterNote.annotation_attrib">


                <input type="hidden" name="id" ng-model="groupNotesForm.encounterNote.id">

                <em><small><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.editors.title"/>:
                    <span>{{ groupNotesForm.encounterNote.providerName }}</span></small></em>
                <div class="pull-right"><em><small><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.encounterDate.title"/>:
                    <span>{{groupNotesForm.encounterNote.updateDate | date: 'dd-MMM-yyyy'}}</span> <fmt:message key="oscarEncounter.noteRev.title"/>: 
                    <a href="javascript:void(0)" ng-click="openRevisionHistory(groupNotesForm.encounterNote)">{{groupNotesForm.encounterNote.revision}}</a></small></em>
                </div>

                <textarea class="form-control" rows="5" placeholder="Enter Note"
                          ng-model="groupNotesForm.encounterNote.note" ng-change="setEditingNoteFlag()"
                          style="margin-bottom:6px;" required></textarea>


                <div class="row"
                     ng-if="groupNotesForm.assignedCMIssues != null && groupNotesForm.assignedCMIssues.length > 0">
                    <div class="col-lg-12">
                        <label>Assigned Issues:</label>
                        <table class="table">
                            <tr ng-repeat="i in groupNotesForm.assignedCMIssues">
                                <td>
                                    <input type="button" value="restore" ng-click="restoreIssue(i)"
                                           ng-if="i.unchecked!=null && i.unchecked"/>
                                    <input type="button" value="remove" ng-click="removeIssue(i)"
                                           ng-if="i.unchecked==null || i.unchecked==false"/>
                                </td>
                                <td>{{i.issue.description}} ({{i.issue.code}}) <a ng-click="addToDxRegistry(i.issue)">(
                                    add to dx registry )</a></td>
                            </tr>

                        </table>
                    </div>

                </div>

                <div ng-if="page.code == 'ongoingconcerns' " class="row">
                    <div class="col-lg-6">
                        <label><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.problemdescription.title"/></label>
                        <input type="text" class="form-control" id="problemdescription" name="problemdescription"
                               ng-model="groupNotesForm.groupNoteExt.problemDesc"
                               placeholder="<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.problemdescription.title"/>"/>
                    </div><!-- col-lg-6 -->

                    <div class=" col-lg-6">
                        <label><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.problemStatus.title"/> <span
                                class="glyphicon glyphicon-info-sign" tooltip-placement="right"
                                uib-tooltip="Examples: <fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.problemStatusExample.msg"/>"></span></label>
                        <input type="text" class="form-control" id="problemstatus" name="problemstatus"
                               ng-model="groupNotesForm.groupNoteExt.problemStatus"
                               placeholder="<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.problemStatus.title"/> "/>

                        <!-- example: <fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.problemStatusExample.msg"/> -->
                    </div><!-- col-lg-6 -->

                </div><!-- row -->


                <div class="row">
                    <div class="col-lg-6">
                        <label><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.startdate.title"/></label>
                        <div class="input-group">
                            <input type="text" class="form-control"
                                   placeholder="<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.startdate.title"/>"
                                   ng-model="groupNotesForm.groupNoteExt.startDate"
                                   uib-datepicker-popup="yyyy-MM-dd"
                                   uib-datepicker-append-to-body="false"
                                   is-open="startDatePicker"
                                   ng-click="startDatePicker = true"
                                   placeholder="YYYY-MM-DD"
                            />
                            <span class="input-group-addon"><span class="glyphicon glyphicon-calendar"></span></span>
                        </div>
                    </div><!-- col-lg-6 -->


                    <div class="col-lg-6">
                        <label><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.resolutionDate.title"/></label>
                        <div class="input-group">
                            <input type="text" class="form-control"
                                   placeholder="<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.resolutionDate.title"/>"
                                   ng-model="groupNotesForm.groupNoteExt.resolutionDate"
                                   uib-datepicker-popup="yyyy-MM-dd"
                                   datepicker-append-to-body="false"
                                   is-open="resolutionDatePicker"
                                   ng-click="resolutionDatePicker = true"
                                   placeholder="YYYY-MM-DD"
                            />
                            <span class="input-group-addon"><span class="glyphicon glyphicon-calendar"></span></span>
                        </div>
                    </div><!-- col-lg-6 -->
                </div><!-- row -->

                <div class="row">

                    <div ng-if="page.code == 'famhx' || page.code == 'riskfactors'" class="col-lg-6">
                        <label><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.ageAtOnset.title"/></label>
                        <input ng-if="page.code == 'famhx' || page.code == 'riskfactors' " type="text"
                               class="form-control" id="ageatonset" name="ageatonset"
                               ng-model="groupNotesForm.groupNoteExt.ageAtOnset"
                               placeholder="<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.ageAtOnset.title"/>"/>
                    </div>
                    <div ng-if="page.code == 'medhx'" class="col-lg-6">
                        <label><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.procedureDate.title"/></label>
                        <div class="input-group">
                            <input type="text" class="form-control" id="proceduredate" name="proceduredate"
                                   placeholder="<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.procedureDate.title"/>"
                                   ng-model="groupNotesForm.groupNoteExt.procedureDate"
                                   uib-datepicker-popup="yyyy-MM-dd"
                                   datepicker-append-to-body="false"
                                   is-open="procedureDatePicker"
                                   ng-click="procedureDatePicker = true"
                                   placeholder="YYYY-MM-DD"
                            />
                            <span class="input-group-addon"><span class="glyphicon glyphicon-calendar"></span></span>
                        </div>
                    </div>

                    <div ng-if="page.code == 'riskfactors' " class="col-lg-6">
                        <label><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.exposureDetail.title"/></label>
                        <input type="text" class="form-control" id="exposuredetail" name="exposuredetail"
                               ng-model="groupNotesForm.groupNoteExt.exposureDetail"
                               placeholder="<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.exposureDetail.title"/>"/>
                    </div>


                    <div ng-if="page.code == 'medhx' || page.code == 'famhx' " class="col-lg-6">
                        <label><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.treatment.title"/></label>
                        <input type="text" class="form-control" id="treatment" name="treatment"
                               ng-model="groupNotesForm.groupNoteExt.treatment"
                               placeholder="<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.treatment.title"/>"/>
                    </div>

                    <div ng-if="page.code == 'famhx'" class="col-lg-6">
                        <label><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.relationship.title"/></label>
                        <input type="text" class="form-control" id="relationship" name="relationship"
                               ng-model="groupNotesForm.groupNoteExt.relationship"
                               placeholder="<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.relationship.title"/>"/>
                    </div>

                </div><!--row-->


                <div class="row">
                    <div ng-if="page.code == 'medhx' || page.code == 'famhx' || page.code == 'ongoingconcerns' || page.code == 'riskfactors' "
                         class="col-lg-6">
                        <label><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.lifestage.title"/></label>
                        <select class="form-control" name="lifestage" id="lifestage"
                                ng-model="groupNotesForm.groupNoteExt.lifeStage">
                            <option value="">
                                <fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.lifestage.opt.notset"/>
                            </option>
                            <option value="N">
                                <fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.lifestage.opt.newborn"/>
                            </option>
                            <option value="I">
                                <fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.lifestage.opt.infant"/>
                            </option>
                            <option value="C">
                                <fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.lifestage.opt.child"/>
                            </option>
                            <option value="T">
                                <fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.lifestage.opt.adolescent"/>
                            </option>
                            <option value="A">
                                <fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.lifestage.opt.adult"/>
                            </option>
                        </select>
                    </div><!-- col-lg-6 -->


                </div><!-- row -->


                <div class="row">

                    <div class="col-lg-6">
                        <!-- TODO: most likely a typeahead and display assigned issues below using the badges or labels-->
                        <label><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.Index.assnIssue"/></label>
                        <input type="text" class="form-control"
                               placeholder="<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.Index.assnIssue"/>"
                               uib-typeahead="i.issueId as i.code for i in searchIssues($viewValue)"
                               typeahead-on-select="assignIssue($item, $model, $label);selectedIssue='';"
                               ng-model="selectedIssue"
                               typeahead-loading="loadingIssues"
                               typeahead-min-length="3"
                        />

                    </div><!-- col-lg-6 -->

                    <div class="col-lg-3">
                        <label><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.Index.btnPosition"/></label>
                        <select class="form-control" id="position" ng-model="groupNotesForm.encounterNote.position">
                            <option ng-value="i" ng-repeat="i in availablePositions">{{i}}</option>
                        </select>
                    </div> <!-- col-lg-4 -->

                    <div class="col-lg-3">
                        <!--shouldn't this just be a single checkbox and the answer is always no unless checked?-->
                        <label><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.hideFromPrint.title"/></label>
                        <div class="form-group" ng-init="groupNotesForm.groupNoteExt.hideCpp=0">
                            <label class="radio-inline" id="hidecpp" name="hidecpp">
                                <input type="radio" id="hidecpp" name="hidecpp"
                                       ng-model="groupNotesForm.groupNoteExt.hideCpp" value="0"> No
                            </label>
                            <label class="radio-inline">
                                <input type="radio" id="hidecpp" name="hidecpp"
                                       ng-model="groupNotesForm.groupNoteExt.hideCpp" value="1"> Yes
                            </label>
                        </div><!-- form-group -->

                    </div><!-- col-lg-4 -->

                </div> <!-- row -->

                <div class="row">
                    <div class="col-sm-12">
                        <label><fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.addFromDxReg.title"/></label>
                        <br/>
                        <div class="btn-group dropup" ng-repeat="qlist in page.quickLists">
                            <button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown"
                                    aria-haspopup="true" aria-expanded="false">
                                {{qlist.label}} <span class="caret"></span>
                            </button>
                            <ul class="dropdown-menu">
                                <li><a ng-repeat="item in qlist.dxList"
                                       ng-click="addDxItem(item)">{{item.description}}</a></li>
                            </ul>
                        </div>
                    </div>
                </div>
                <!--
                                <div class="row">
                                  <div class="col-lg-12" style="margin-top:6px;">
                                <div class="checkbox" >
                                    <label>
                                      <input type="checkbox" ng-model="groupNotesForm.issue.issueId" ng-checked="true" ng-true-value="'{{page.issueId}}'" ng-false-value="'0'">  <em>{{page.title}}</em>  as part of cpp

                                    </label>
                                </div>
                                  </div>
                                </div> -->

            </form>

        </div><!-- form-wrapper -->

    </div><!-- modal-body -->

    <div class="modal-footer" style="background-color:#eeeeee;margin-top:0px">
        <!-- TODO: see what of these can be functions inline or maybe obsolete???
 
 				<input type="image"	src="<c:out value="${ctx}/oscarEncounter/graphics/copy.png"/>" title='<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.Index.btnCopy"/>' onclick="copyCppToCurrentNote(); return false;"> 
 				<input type="image"	src="<c:out value="${ctx}/oscarEncounter/graphics/annotation.png"/>" title='<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.Index.btnAnnotation"/>' id="anno" style="padding-right: 10px;"> 
 				<input type="image"	src="<c:out value="${ctx}/oscarEncounter/graphics/edit-cut.png"/>" title='<fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.Index.btnArchive"/>' onclick="$('archived').value='true';" style="padding-right: 10px;">

 -->

        <br>
        <button ng-click="archiveGroupNotes()" type="button" class="btn btn-danger" ng-hide="page.cannotChange">
            <fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.Index.btnArchive"/></button>
        <button ng-click="saveGroupNotes()" type="button" class="btn btn-primary" ng-hide="page.cannotChange">
            <fmt:setBundle basename="oscarResources"/><fmt:message key="oscarEncounter.Index.btnSignSave"/></button>
        <button ng-click="cancel()" type="button" class="btn">
            <fmt:setBundle basename="uiResources" var="uiBundle"/>
            <fmt:message bundle="${uiBundle}" key="modal.newPatient.close" />
        </button>
    </div>
</div>
