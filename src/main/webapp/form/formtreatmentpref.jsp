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
<%@ taglib uri="/WEB-INF/security.tld" prefix="security" %>
<%
    String roleName2$ = (String) session.getAttribute("userrole") + "," + (String) session.getAttribute("user");
    boolean authed = true;
%>
<security:oscarSec roleName="<%=roleName2$%>" objectName="_form" rights="r" reverse="<%=true%>">
    <%authed = false; %>
    <%response.sendRedirect("../securityError.jsp?type=_form");%>
</security:oscarSec>
<%
    if (!authed) {
        return;
    }
%>

<%@ page import="oscar.util.*, oscar.form.*, oscar.form.data.*" %>
<%@page import="org.oscarehr.util.LoggedInInfo" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%
    String formClass = "TreatmentPref";
    String formLink = "formtreatmentpref.jsp";

    int demoNo = Integer.parseInt(request.getParameter("demographic_no"));
    int formId = Integer.parseInt(request.getParameter("formId"));
    int provNo = Integer.parseInt((String) session.getAttribute("user"));
    FrmRecord rec = (new FrmRecordFactory()).factory(formClass);
    java.util.Properties props = rec.getFormRecord(LoggedInInfo.getLoggedInInfoFromSession(request), demoNo, formId);

    //FrmData fd = new FrmData();    String resource = fd.getResource(); resource = resource + "ob/riskinfo/";

    //get project_home
    String project_home = request.getContextPath().substring(1);
%>
<%
    boolean bView = false;
    if (request.getParameter("view") != null && request.getParameter("view").equals("1")) bView = true;
%>
<html>
    <head>
        <script type="text/javascript" src="<%= request.getContextPath() %>/js/global.js"></script>
        <title>Treatment Preference</title>
        <base href="<%= request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/" %>">
        <link rel="stylesheet" type="text/css" media="all" href="<%= request.getContextPath() %>/share/css/extractedFromPages.css"/>
    </head>


    <script type="text/javascript" language="Javascript">

        var choiceFormat = new Array(7, 9);
        var allNumericField = null;
        var allMatch = null;
        var action = "/<%=project_home%>/form/formname.do";

    </script>
    <script type="text/javascript" src="formScripts.js">

    </script>


    <body bgproperties="fixed" topmargin="0" leftmargin="0" rightmargin="0"
          onload="window.resizeTo(768,768)">
    <!--
    @oscar.formDB Table="formAdf"
    @oscar.formDB Field="ID" Type="int(10)" Null="NOT NULL" Key="PRI" Default="" Extra="auto_increment"
    @oscar.formDB Field="demographic_no" Type="int(10)" Null="NOT NULL" Default="'0'"
    @oscar.formDB Field="provider_no" Type="int(10)" Null="" Default="NULL"
    @oscar.formDB Field="formCreated" Type="date" Null="" Default="NULL"
    @oscar.formDB Field="formEdited" Type="timestamp"
    -->
    <form action="${pageContext.request.contextPath}/form/formname.do" method="post">
        <input type="hidden" name="demographic_no"
               value="<%= props.getProperty("demographic_no", "0") %>"/>
        <input type="hidden" name="formCreated"
               value="<%= props.getProperty("formCreated", "") %>"/>
        <input type="hidden" name="form_class" value="<%=formClass%>"/>
        <input type="hidden" name="form_link" value="<%=formLink%>"/>
        <input type="hidden" name="formId" value="<%=formId%>"/>
        <input type="hidden" name="submit" value="exit"/>

        <table border="0" cellspacing="0" cellpadding="0" width="740px"
               height="95%">
            <tr>
                <td>
                    <table border="0" cellspacing="0" cellpadding="0" width="740px"
                           height="10%">
                        <tr>
                            <th class="subject">Treatment Preference</th>
                        </tr>
                    </table>
                </td>
            </tr>
            <tr>
                <td valign="top">
                    <table border="0" cellspacing="0" cellpadding="0" height="85%"
                           width="740px" id="page1">
                        <tr>
                            <td colspan="2" valign="top">
                                <table width="740px" height="200px" border="0" cellspacing="0"
                                       cellpadding="0">
                                    <tr>
                                        <td width="5%" valign="top" align="right"></td>
                                        <td valign="top">You will be randomly assigned to a
                                            treatment group or a control group. <br>
                                            We are interested in your preference which will in no way
                                            influence which group you are ultimately assigned by chance
                                        </td>
                                    </tr>
                                    <tr>
                                    <tr bgcolor="white">
                                        <td width="5%" align="right"></td>
                                        <td width="95%"><input type="checkbox" class="checkbox"
                                                               name="treatmentGr" <%= props.getProperty("treatmentGr", "") %> />
                                            I would prefer to be assigned to the treatment group
                                        </td>
                                    </tr>
                                    <tr bgcolor="white">
                                        <td width="5%" align="right"></td>
                                        <td width="95%"><input type="checkbox" class="checkbox"
                                                               name="controlGr" <%= props.getProperty("controlGr", "") %> />
                                            I
                                            would prefer to be assigned to the control group
                                        </td>
                                    </tr>
                                    <tr bgcolor="white">
                                        <td width="5%" align="right"></td>
                                        <td width="95%"><input type="checkbox" class="checkbox"
                                                               name="eitherGr" <%= props.getProperty("eitherGr", "") %> />
                                            I
                                            have no preference for either group
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>

            <tr>
                <td valign="top">
                    <table class="Head" class="hidePrint" height="5%">
                        <tr>
                            <td align="left">
                                <%
                                    if (!bView) {
                                %> <input type="submit" value="Save"
                                          onclick="javascript: return onSave();"/> <input type="submit"
                                                                                          value="Save and Exit"
                                                                                          onclick="javascript: return onSaveExit();"/>
                                <%
                                    }
                                %> <input type="button" value="Exit"
                                          onclick="javascript:return onExit();"/> <input type="button"
                                                                                         value="Print"
                                                                                         onclick="javascript:window.print();"/>
                            </td>
                            <td align="right">Study ID: <%= props.getProperty("studyID", "N/A") %>
                                <input type="hidden" name="studyID"
                                       value="<%= props.getProperty("studyID", "N/A") %>"/></td>
                        </tr>
                    </table>
                </td>
            </tr>
        </table>
    </form>
    </body>
</html>
