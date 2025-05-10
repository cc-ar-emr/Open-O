<%-- Copyright (c) 2006-. OSCARservice, OpenSoft System. All Rights Reserved. This software is published under the GPL
    GNU General Public License. This program is free software; you can redistribute it and/or modify it under the terms
    of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY
    WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
    General Public License for more details. You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
    02111-1307, USA. --%>
    <%@page import="org.apache.commons.lang.StringEscapeUtils" %>
        <%@ page import="java.util.*, java.sql.*, oscar.*, java.text.*, java.lang.*,java.net.*, oscar.appt.*, org.oscarehr.common.dao.AppointmentTypeDao, org.oscarehr.common.model.AppointmentType,   
 org.oscarehr.util.SpringUtils, org.owasp.encoder.Encode" %>
            <%@ page errorPage="../appointment/errorpage.jsp" %>
                <!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
                <% // Validate session and check for authentication if (session==null ||
                    session.getAttribute("user")==null) { response.sendRedirect("../logout.jsp"); return; } // Get
                    appointment types AppointmentTypeDao appDao=(AppointmentTypeDao)
                    SpringUtils.getBean(AppointmentTypeDao.class); List<AppointmentType> types = appDao.listAll();

                    // Safely get and validate the type parameter
                    String typeParam = request.getParameter("type");
                    if (typeParam == null) {
                    typeParam = "";
                    }
                    // Sanitize the type parameter to prevent XSS
                    typeParam = Encode.forHtml(typeParam);
                    %>
                    <html>

                    <head>
                        <title>Appointment Type</title>
                        <script type="text/javascript">
                            var dur = '';
                            var reason = '';
                            var loc = '';
                            var notes = '';
                            var resources = '';
                            var names = '';                                                                                                                                                                       
                            <%   for (int j = 0; j < types.size(); j++) {                                                                                                                                           
                                String duration = types.get(j).getDuration() != null ? types.get(j).getDuration() : "";                                                                                         
                                String typeReason = types.get(j).getReason() != null ? types.get(j).getReason() : "";                                                                                           
                                String location = types.get(j).getLocation() != null ? types.get(j).getLocation() : "";                                                                                         
                                String typeNotes = types.get(j).getNotes() != null ? types.get(j).getNotes() : "";                                                                                              
                                String typeResources = types.get(j).getResources() != null ? types.get(j).getResources() : "";                                                                                  
                                String typeName = types.get(j).getName() != null ? types.get(j).getName() : "";                                                                                                 
                            %>
                                                        dur = dur + '<%= StringEscapeUtils.escapeJavaScript(duration) %>' + ',';
                                                    reason = reason + '<%= StringEscapeUtils.escapeJavaScript(typeReason) %>' + ',';
                                                    loc = loc + '<%= StringEscapeUtils.escapeJavaScript(location) %>' + ',';
                                                    notes = notes + '<%= StringEscapeUtils.escapeJavaScript(typeNotes) %>' + ',';
                                                    resources = resources + '<%= StringEscapeUtils.escapeJavaScript(typeResources) %>' + ',';
                                                    names = names + '<%= StringEscapeUtils.escapeJavaScript(typeName) %>' + ',';                                                                                                          
                            <%   } %>
        var durArray = dur.split(",");
                            var reasonArray = reason.split(",");
                            var locArray = loc.split(",");
                            var notesArray = notes.split(",");
                            var resArray = resources.split(",");
                            var nameArray = names.split(",");

                            var typeSel = '';
                            var reasonSel = '';
                            var locSel = '';
                            var durSel = 15;
                            var notesSel = '';
                            var resSel = '';

                            function getFields(idx) {
                                if (idx > 0) {
                                    // Set values safely with proper escaping                                                                                                                                     
                                    typeSel = nameArray[idx - 1];
                                    durSel = durArray[idx - 1];
                                    reasonSel = reasonArray[idx - 1];
                                    locSel = locArray[idx - 1];
                                    notesSel = notesArray[idx - 1];
                                    resSel = resArray[idx - 1];

                                    // Update display with escaped HTML                                                                                                                                           
                                    document.getElementById('durId').textContent = durArray[idx - 1];
                                    document.getElementById('reasonId').textContent = reasonArray[idx - 1];
                                    document.getElementById('locId').textContent = locArray[idx - 1];
                                    document.getElementById('notesId').textContent = notesArray[idx - 1];
                                    document.getElementById('resId').textContent = resArray[idx - 1];
                                }
                            }
                        </script>
                    </head>

                    <body bgcolor="#EEEEFF" bgproperties="fixed" topmargin="0" leftmargin="0" rightmargin="0">
                        <table width="100%">
                            <tr>
                                <td width="100">Type</td>
                                <td width="200">
                                    <select id="typesId" width="25" maxsize="50"
                                        onchange="getFields(this.selectedIndex)">
                                        <option value="-1">Select type</option>
                                        <% for (int i=0; i < types.size(); i++) { String typeName=types.get(i).getName()
                                            !=null ? types.get(i).getName() : "" ; boolean
                                            isSelected=!typeParam.isEmpty() && typeParam.equals(typeName); %>
                                            <option value="<%= i %>" <%=isSelected ? " selected" : "" %>><%=
                                                    Encode.forHtmlContent(typeName) %>
                                            </option>
                                            <% } %>
                                    </select>
                                </td>
                                <td><input type="button" name="Select" value="Select"
                                        onclick="window.opener.setType(typeSel,reasonSel,locSel,durSel,notesSel,resSel); window.close()">
                            </tr>
                            <tr>
                                <td>Duration</td>
                                <td colspan="2">
                                    <div id="durId"></div>
                                </td>
                            </tr>
                            <tr>
                                <td>Reason</td>
                                <td colspan="2"><span id="reasonId"></span></td>
                            </tr>
                            <tr>
                                <td>Location</td>
                                <td colspan="2"><span id="locId"></span></td>
                            </tr>
                            <tr>
                                <td>Notes</td>
                                <td colspan="2"><span id="notesId"></span></td>
                            </tr>
                            <tr>
                                <td>Resources</td>
                                <td colspan="2"><span id="resId"></span></td>
                            </tr>
                        </table>
                    </body>
                    <script type="text/javascript">
                        getFields(document.getElementById('typesId').selectedIndex);
                    </script>

                    </html>