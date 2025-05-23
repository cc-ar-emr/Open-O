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

<%
    if (session.getValue("patient") == null) response.sendRedirect("logout.jsp");
%>

<html>
<head>
    <script type="text/javascript" src="<%= request.getContextPath() %>/js/global.js"></script>
    <title>PATIENT</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/web.css">
</head>
<body onLoad="setfocus()" topmargin="0" leftmargin="0" rightmargin="0">

<table border="0" cellspacing="0" cellpadding="0" width="100%">
    <tr bgcolor="#486ebd">
        <th align=CENTER NOWRAP><font face="Helvetica" color="#FFFFFF">MAIL
            US</font></th>
    </tr>
</table>

<P>
<center>
    <p>Your Message:</p>
    <p><textarea name="textfield" cols="70" rows="8"></textarea></p>
    <p><input type="submit" name="Submit" value="Submit"
              onClick="window.location='patient_screen.jsp'"> <input
            type="button" name="button" value="Cancel"
            onClick="window.location='patient_screen.jsp'"></p>
</center>
</body>
</html>
