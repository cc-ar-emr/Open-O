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


<%@ include file="/taglibs.jsp" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"
       scope="request"/>

<div class="page-header">
    <h4>Program Activity Report Generator</h4>
</div>

<form action="<%=request.getContextPath() %>/PMmodule/Reports/ProgramActivityReport.do" id="actForm" class="well form-inline">
    <input type="hidden" name="method" value="generate"/>
    <input type="text" id="sdate" name="form.startDate" size="15"/>
    <input type="text" id="edate" name="form.endDate" size="15"/>
    <input type="submit" name="submit" value="Generate Report" class="btn btn-primary"/>
</form>

<script>
    var startDt = $("#sdate").datepicker({
        format: "yyyy-mm-dd"
    });

    var endDt = $("#edate").datepicker({
        format: "yyyy-mm-dd"
    });
    $(document).ready(function () {
        $('#actForm').validate({
            rules: {
                sdate: {
                    required: true,
                    oscarDate: true
                },
                edate: {
                    required: true,
                    oscarDate: true
                }
            }
        });
    });

    registerFormSubmit('actForm', 'dynamic-content');
</script>
