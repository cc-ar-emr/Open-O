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
<fmt:setBundle basename="uiResources" var="uiBundle"/>
<div class="modal-header">
    <h4><fmt:message bundle="${uiBundle}" key="tickler.note.title"/></h4>
</div>
<div class="modal-body">
    <form>

        <textarea style="width:100%;height:80%" ng-model="ticklerNote.note"></textarea>
        <span ng-show="ticklerNote.revision>0">
			<fmt:message bundle="${uiBundle}" key="tickler.note.date"/>: <span>{{ticklerNote.observationDate | date: 'yyyy-MM-dd'}}</span> <fmt:setBundle basename="oscarResources"/><fmt:message key="tickler.note.revision"/> <a target="note_history"
                                                             href="<%= request.getContextPath() %>/CaseManagementEntry.do?method=notehistory&noteId={{ticklerNote.noteId}}"><span>{{ticklerNote.revision}}</span></a><br>
			<fmt:message bundle="${uiBundle}" key="tickler.note.editor"/>: <span>{{ticklerNote.editor}}</span>
			</span>
    </form>

</div>
<div class="modal-footer">
    <button class="btn" ng-click="save()"><fmt:message bundle="${uiBundle}" key="global.save"/></button>
    <button class="btn" ng-click="close()"><fmt:message bundle="${uiBundle}" key="global.close"/></button>
</div>





