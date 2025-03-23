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
    <h4><fmt:message bundle="${uiBundle}" key="tickler.comments.title"/></h4>
</div>
<div class="modal-body">
    <table class="table">
        <thead>
        <tr>
            <th><fmt:message bundle="${uiBundle}" key="tickler.comments.provider"/></th>
            <th><fmt:message bundle="${uiBundle}" key="tickler.comments.updateDate"/></th>
            <th><fmt:message bundle="${uiBundle}" key="tickler.comments.comment"/></th>
        </tr>
        </thead>
        <tr ng-repeat="c in tickler.ticklerComments | orderBy:'updateDate':true">
            <td>{{c.providerName}}</td>
            <td>{{c.updateDate | date: 'yyyy-MM-dd HH:mm'}}</td>
            <td>{{c.message}}</td>
        </tr>
    </table>

</div>
<div class="modal-footer">
    <button class="btn" ng-click="close()"><fmt:message bundle="${uiBundle}" key="global.close"/></button>
</div>





