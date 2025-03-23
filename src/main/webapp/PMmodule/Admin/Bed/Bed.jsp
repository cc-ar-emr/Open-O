<%--


    Copyright (c) 2005-2012. Centre for Research on Inner City Health, St. Michael's Hospital, Toronto. All Rights Reserved.
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

    This software was written for
    Centre for Research on Inner City Health, St. Michael's Hospital,
    Toronto, Ontario, Canada

--%>
<%--<%@ include file="/taglibs.jsp"%>--%>

<script>
    function ConfirmDelete(name) {
        return confirm("Are you sure you want to delete " + name + " ?");
    }

    function roomFilter() {
        document.forms[0].action = '<%=request.getContextPath() %>/PMmodule/BedManager.do?method=doRoomFilter';
        document.forms[0].submit();
    }

    function bedFilter() {
        document.forms[0].action = '<%=request.getContextPath() %>/PMmodule/BedManager.do?method=doBedFilter';
        document.forms[0].submit();
    }

    function addBeds() {
        var num = bedManagerForm.numBeds.value;
        if (num >= 10) {
            alert("You cannot add more than 10 beds at a time.");
            return;
        } else {
            bedManagerForm.method.value = 'addBeds';
            var obj = document.getElementsByName("submit.addBed")[0]
            obj.value = 'Add Beds';
            bedManagerForm.submit();
        }
    }

    function addRooms() {
        var num = bedManagerForm.numRooms.value;
        if (num >= 10) {
            alert("You cannot add more than 10 rooms at a time.");
            return;
        } else {
            bedManagerForm.method.value = 'addRooms';
            var obj = document.getElementsByName("submit.addRoom")[0]
            obj.value = 'Add Rooms';
            bedManagerForm.submit();
        }
    }

    function saveBeds() {
        var i;
        var obj;
        if (bedManagerForm.bedslines.value != null) {
            for (i = 0; i < parseInt(bedManagerForm.bedslines.value); i++) {
                obj = document.getElementsByName("beds[" + i + "].name")[0]
                if (obj.value == "") {
                    alert("Bed name can not be empty.");
                    return;
                }
            }
            bedManagerForm.method.value = 'saveBeds';
            obj = document.getElementsByName("submit.saveBed")[0]
            obj.value = 'Save Beds';
            bedManagerForm.submit();
        }
    }

    function saveRooms() {
        var i;
        var obj;
        if (bedManagerForm.roomslines.value != null) {
            for (i = 0; i < parseInt(bedManagerForm.roomslines.value); i++) {
                obj = document.getElementsByName("rooms[" + i + "].name")[0]
                if (obj.value == "") {
                    alert("Room name can not be empty.");
                    return;
                }
            }
            bedManagerForm.method.value = 'saveRooms';
            obj = document.getElementsByName("submit.saveRoom")[0]
            obj.value = 'Save Rooms';
            bedManagerForm.submit();
        }
    }

    function deleteRoom() {
        var doDelete = confirm("Are you sure you want to delete the room?");
        if (doDelete) {
            document.forms[0].action = '<%=request.getContextPath() %>/PMmodule/BedManager.do?method=deleteRoom';
            document.forms[0].submit();
        }
    }

    function deleteBed() {
        var doDelete = confirm("Are you sure you want to delete the bed?");
        if (doDelete) {
            document.forms[0].action = '<%=request.getContextPath() %>/PMmodule/BedManager.do?method=deleteBed';
            document.forms[0].submit();
        }
    }


</script>
<%@ include file="/common/messages.jsp" %>
<div class="tabs" id="tabs">
    <table cellpadding="3" cellspacing="0" border="0">
        <tr>
            <th title="Manage beds">Manage beds and rooms for facility "<c:out
                    value="${sessionScope.bedManagerForm.facility.name}"/>"
            </th>
        </tr>
    </table>
</div>

<form action="${pageContext.request.contextPath}/PMmodule/BedManager.do" method="post">

    <table width="100%" summary="Manage rooms and beds">

        <input type="hidden" name="facilityId" id="facilityId"/>
        <input type="hidden" name="roomToDelete" id="roomToDelete"/>
        <input type="hidden" name="bedToDelete" id="bedToDelete"/>

        <tr>
            <td width="80%">
                <div class="tabs">
                    <table cellpadding="3" cellspacing="0" border="0">
                        <tr>
                            <th>Rooms</th>
                        </tr>
                    </table>
                </div>

                <!-- begin room status & bed program filter -->
                <table width="100%">
                    <tr>
                        <td width="40%"></td>
                        <td width="30%" style="font-weight: bold">Room Status<br/>
                            <select name="roomStatusFilter" name="bedManagerForm"
                                         onchange="roomFilter();">
                                <c:forEach var="roomStatusName" items="${roomStatusNames}">
                                    <option value="${roomStatusName.key}">
                                            ${roomStatusName.value}
                                    </option>
                                </c:forEach>
                            </select></td>
                        <td width="30%" align="right" style="font-weight: bold">Bed
                            Program<br/>
                            <select name="bedProgramFilterForRoom"
                                         name="bedManagerForm" onchange="roomFilter();">
                                <option value="0">Any</option>
                                <c:forEach var="program" items="${programs}">
                                    <option value="${program.id}">
                                            ${program.name}
                                    </option>
                                </c:forEach>
                            </select></td>
                    </tr>
                </table>

                <!-- end room status & bed program filter --> <display:table
                    class="simple" name="sessionScope.bedManagerForm.rooms" id="room"
                    requestURI="/PMmodule/BedManager.do" summary="Edit rooms">
                <display:column title="Name" sortable="true">
                    <input type="text"
                           name="rooms[<c:out value="${room_rowNum - 1}" />].name"
                           value="<c:out value="${room.name}" />"/>
                </display:column>
                <display:column title="Floor" sortable="true">
                    <input type="text"
                           name="rooms[<c:out value="${room_rowNum - 1}" />].floor"
                           value="<c:out value="${room.floor}" />"/>
                </display:column>
                <display:column title="Type">
                    <select
                            name="rooms[<c:out value="${room_rowNum - 1}" />].roomTypeId">
                        <c:forEach var="roomType" items="${bedManagerForm.roomTypes}">
                            <c:choose>
                                <c:when test="${roomType.id == room.roomTypeId}">
                                    <option value="<c:out value="${roomType.id}"/>"
                                            selected="selected"><c:out value="${roomType.name}"/>
                                    </option>
                                </c:when>
                                <c:otherwise>
                                    <option value="<c:out value="${roomType.id}"/>"><c:out
                                            value="${roomType.name}"/></option>
                                </c:otherwise>
                            </c:choose>
                        </c:forEach>
                    </select>
                </display:column>
                <display:column title="Assigned Beds">
                    <select
                            name="rooms[<c:out value="${room_rowNum - 1}" />].assignedBed">
                        <c:choose>
                            <c:when test="${room.assignedBed != 1}">
                                <option value="1">Y</option>
                                <option value="0" selected>N</option>
                            </c:when>
                            <c:otherwise>
                                <option value="1" selected>Y</option>
                                <option value="0">N</option>
                            </c:otherwise>
                        </c:choose>
                    </select>
                </display:column>
                <display:column title="Room Capacity">
                    <select
                            name="rooms[<c:out value="${room_rowNum - 1}" />].occupancy">

                        <c:forEach var="num" begin="1" end="50" step="1">
                            <c:choose>
                                <c:when test="${room.occupancy == num}">
                                    <option value="<c:out value='${num}' />" selected="selected">
                                        <c:out value="${num}"/></option>
                                </c:when>
                                <c:otherwise>
                                    <option value="<c:out value='${num}' />"><c:out
                                            value="${num}"/></option>
                                </c:otherwise>
                            </c:choose>
                        </c:forEach>
                    </select>
                </display:column>
                <display:column title="Program">
                    <select
                            name="rooms[<c:out value="${room_rowNum - 1}" />].programId">
                        <c:forEach var="program" items="${bedManagerForm.programs}">
                            <c:choose>
                                <c:when test="${program.id == room.programId}">
                                    <option value="<c:out value="${program.id}"/>"
                                            selected="selected"><c:out value="${program.name}"/>
                                    </option>
                                </c:when>
                                <c:otherwise>
                                    <option value="<c:out value="${program.id}"/>"><c:out
                                            value="${program.name}"/></option>
                                </c:otherwise>
                            </c:choose>
                        </c:forEach>
                    </select>
                </display:column>
                <display:column title="Active" sortable="true">
                    <c:choose>
                        <c:when test="${room.active}">
                            <input type="checkbox"
                                   name="rooms[<c:out value="${room_rowNum - 1}" />].active"
                                   checked="checked"/>
                        </c:when>
                        <c:otherwise>
                            <input type="checkbox"
                                   name="rooms[<c:out value="${room_rowNum - 1}" />].active"/>
                        </c:otherwise>
                    </c:choose>
                </display:column>

                <display:column title="Delete" sortable="true">
                    <input type="button" name="submit.deleteRoom" value="Delete"
                           onclick='bedManagerForm.roomToDelete.value="<c:out value="${room.id}"/>"; deleteRoom();'/>
                </display:column>

            </display:table></td>
            <td width="20%"><br/>
                <input type=hidden name="roomslines" value="<c:out value="${room_rowNum}" />"> 
                <button type="button" onclick="saveRooms();" data-property="submit.saveRooms">Save Rooms</button>
                <input type=hidden name="submit.saveRoom" value=""></td>
        </tr>
        <tr>
            <td><input type="text" name="numRooms" id="numRooms" /> 
                <button type="button" onclick="addRooms();" data-property="submit.addRooms">Add Rooms</button>
                <input type=hidden name="submit.addRoom" value=""></td>
        </tr>
        <tr>
            <td><br/>
            </td>
        </tr>
        <tr>
            <td width="80%">
                <div class="tabs">
                    <table cellpadding="3" cellspacing="0" border="0">
                        <tr>
                            <th>Beds</th>
                        </tr>
                    </table>
                </div>

                <!-- begin bed status & bedRoom filter -->
                <table width="100%">
                    <tr>
                        <td width="40%"></td>
                        <td width="30%" style="font-weight: bold">Bed Status<br/>
                            <select name="bedStatusFilter"
                            <select name="bedStatusFilter"
                                         onchange="bedFilter();">
                                <c:forEach var="bedStatusName" items="${bedStatusNames}">
                                    <option value="${bedStatusName.key}">
                                            ${bedStatusName.value}
                                    </option>
                                </c:forEach>
                            </select></td>
                        <td width="30%" align="right" style="font-weight: bold">Room<br/>
                            <select name="bedRoomFilterForBed" onchange="bedFilter();">
                                <c:forEach var="assignedBedRoom" items="${assignedBedRooms}">
                                    <option value="${assignedBedRoom.id}">
                                            ${assignedBedRoom.name}
                                    </option>
                                </c:forEach>
                            </select></td>
                    </tr>
                </table>
                <!-- end bed status & bedRoom filter --> <display:table
                    class="simple" name="sessionScope.bedManagerForm.beds" id="bed"
                    requestURI="/PMmodule/BedManager.do" summary="Edit beds">

                <display:column title="Name" sortable="true">
                    <input type="text"
                           name="beds[<c:out value="${bed_rowNum - 1}" />].name"
                           value="<c:out value="${bed.name}" />"/>
                </display:column>
                <display:column title="Type">
                    <select name="beds[<c:out value="${bed_rowNum - 1}" />].bedTypeId">
                        <c:forEach var="bedType" items="${bedManagerForm.bedTypes}">
                            <c:choose>
                                <c:when test="${bedType.id == bed.bedTypeId}">
                                    <option value="<c:out value="${bedType.id}"/>"
                                            selected="selected"><c:out value="${bedType.name}"/>
                                    </option>
                                </c:when>
                                <c:otherwise>
                                    <option value="<c:out value="${bedType.id}"/>"><c:out
                                            value="${bedType.name}"/></option>
                                </c:otherwise>
                            </c:choose>
                        </c:forEach>
                    </select>
                </display:column>
                <display:column title="Room">
                    <select name="beds[<c:out value="${bed_rowNum - 1}" />].roomId">
                        <c:forEach var="room" items="${bedManagerForm.assignedBedRooms}">
                            <c:choose>
                                <c:when test="${room.id == bed.roomId}">
                                    <option value="<c:out value="${room.id}"/>" selected="selected">
                                        <c:out value="${room.name}"/></option>
                                </c:when>
                                <c:otherwise>
                                    <option value="<c:out value="${room.id}"/>"><c:out
                                            value="${room.name}"/></option>
                                </c:otherwise>
                            </c:choose>
                        </c:forEach>
                    </select>
                </display:column>
                <display:column property="reservationEnd" sortable="true"
                                title="Reserved Until"/>
                <display:column title="Active" sortable="true">
                    <c:choose>
                        <c:when test="${bed.active}">
                            <input type="checkbox"
                                   name="beds[<c:out value="${bed_rowNum - 1}" />].active"
                                   checked="checked"/>
                        </c:when>
                        <c:otherwise>
                            <input type="checkbox"
                                   name="beds[<c:out value="${bed_rowNum - 1}" />].active"/>
                        </c:otherwise>
                    </c:choose>
                </display:column>

                <display:column title="Delete" sortable="true">
                    <input type="button" name="submit.deleteBed" value="Delete"
                           onclick='bedManagerForm.bedToDelete.value="<c:out value="${bed.id}"/>"; deleteBed();'/>
                </display:column>

            </display:table></td>
            <td width="20%"><br/>
                <button type="button" onclick="saveBeds();" data-property="submit.saveBeds">Save Beds</button>
                <input type=hidden name="submit.saveBed" value=""></td>
        </tr>
        <tr>
            <td><input type=hidden name="bedslines"
                       value="<c:out value="${bed_rowNum}" />"> <c:choose>
                <c:when test="${not empty bedManagerForm.rooms}">
                    <input type="text" name="numBeds" id="numBeds" />
                    <button type="button" onclick="addBeds();" data-property="submit.addBeds">Add Beds</button>
                    <input type=hidden name="submit.addBed" value="">
                </c:when>
                <c:otherwise>
                    <input type="submit" name="submit" value="Add beds" disabled="true" />
                </c:otherwise>
            </c:choose></td>
        </tr>
    </table>
    <div>
        <p><a href="<%=request.getContextPath() %>/PMmodule/FacilityManager.do?method=list">Return
            to facilities list</a></p>
    </div>

</form>
