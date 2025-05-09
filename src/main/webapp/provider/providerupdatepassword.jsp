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
	if(session.getValue("user") == null)
    response.sendRedirect("../logout.jsp");
  String curUser_no = (String) session.getAttribute("user");
%>

<%@ page
	import="java.lang.*, java.util.*, java.text.*,java.security.*, oscar.*"
	errorPage="/errorpage.jsp"%>
<%@ page import="org.oscarehr.util.SpringUtils" %>
<%@ page import="org.oscarehr.common.model.Security" %>
<%@ page import="org.oscarehr.common.dao.SecurityDao" %>
<%@ page import="org.oscarehr.util.LoggedInInfo" %>
<%@ page import="org.oscarehr.managers.SecurityManager" %>
<%@ page import="org.oscarehr.myoscar.utils.MyOscarLoggedInInfo" %>
<%@ page import="oscar.oscarProvider.data.ProviderMyOscarIdData" %>
<%@ page import="org.oscarehr.phr.util.MyOscarUtils" %>
<%
	LoggedInInfo loggedInInfo = LoggedInInfo.getLoggedInInfoFromSession(request);
	SecurityManager securityManager = SpringUtils.getBean(SecurityManager.class);

%>
<%@ page import="oscar.log.LogAction" %>
<%@ page import="oscar.log.LogConst" %>

<%
	SecurityDao securityDao = SpringUtils.getBean(SecurityDao.class);
	List<Security> ss = securityDao.findByProviderNo(curUser_no);
	for(Security s:ss) {

	     boolean pinUpdateRequired = false;
	     String errorMsg = "";
	     //check if the user will change the  PIN
	     if (request.getParameter("pin") != null && request.getParameter("pin").length() > 0 && 
	         request.getParameter("newpin") != null && request.getParameter("newpin").length() > 0 && 
	         request.getParameter("confirmpin") != null && request.getParameter("confirmpin").length() > 0) {
	    	
	    	 String pin =    request.getParameter("pin");
	    	 String newPin =  request.getParameter("newpin");
	         String confPin = request.getParameter("confirmpin");  
	    	 
	    	 if (!pin.equals(s.getPin())) {
	    		 errorMsg = "PIN Update Error: PIN doesn't match the exisitng one in the system. ";
	    	 } else if (!newPin.equals(confPin)) {
	    		 errorMsg = "PIN Update Error: New PIN doesn't match the Confirm PIN. ";
	    	 } else if (newPin.equals(s.getPin())) {
	    		 errorMsg = "PIN Update Error: New PIN must be different from the existing PIN. ";
	    	 } else { 	    	 
		    	 pinUpdateRequired = true;
		    	 s.setPin(newPin);
		    	 s.setPinUpdateDate(new java.util.Date());
	    	 }
	     }
		
	     boolean passwordUpdateRequired = false;
		 String oldPassword = request.getParameter("oldpassword");
		 String newPassword = request.getParameter("mypassword");
		 String confirmPassword = request.getParameter("confirmpassword");

	     //check if the user will change the  Password
	     if (oldPassword != null && !oldPassword.isEmpty() &&
				 newPassword != null && !newPassword.isEmpty() &&
				 confirmPassword != null && !confirmPassword.isEmpty()) {

             if (securityManager.matchesPassword(oldPassword, s.getPassword()) && newPassword.equals(confirmPassword)) {
		     	if (securityManager.matchesPassword(newPassword, s.getPassword())) {
		    	   errorMsg = errorMsg + " Password Update Error: New Password must be different from the existing Password. ";
		       } else {
		         s.setPassword(securityManager.encodePassword(newPassword));
		         s.setPasswordUpdateDate(new java.util.Date());
		         passwordUpdateRequired = true;
		       }  
		     } else {
		       errorMsg = errorMsg + " Password Update Error: Password doesn't match the existing one in the system. ";
		     }
	     }
	     
	     //Persist it if one of them has gone thru.
         if (passwordUpdateRequired || pinUpdateRequired) {
        	 
           	 if(securityManager.checkPasswordAgainstPrevious(newPassword, s.getProviderNo())) {
        		 errorMsg = errorMsg + " Password Update Error: Password cannot be one of your previous records"; 
        	 } else {
	        	 securityManager.updateSecurityRecord(loggedInInfo, s);
	        	 
	        	 //Log the action
	        	 String ip = request.getRemoteAddr();
	        	 LogAction.addLog(curUser_no, LogConst.UPDATE, "Password/PIN update.", "", ip);
        	 }
         }
	     
         if(ProviderMyOscarIdData.idIsSet(curUser_no)){
        	 	MyOscarLoggedInInfo.setLoggedInInfo(request.getSession(), null);
		    MyOscarUtils.attemptMyOscarAutoLoginIfNotAlreadyLoggedInAsynchronously(loggedInInfo, true);
         }
	     
	     //In case of the error for any reason go back.
	     if (!errorMsg.isEmpty()) {
	    	 if (passwordUpdateRequired) {
	    		 errorMsg = "Password Update Sucsessful However, " + errorMsg; 
	    	 }
	    	 if (pinUpdateRequired) {
	    		 errorMsg = "PIN Update Sucsessfull However, " + errorMsg; 
	    	 }	    	 
	    	 response.sendRedirect("providerchangepassword.jsp?errormsg="+errorMsg);
	     }
	     
	     out.println("<script language='javascript'>self.close();</script>");
	     
	}
%>
<html>
<head>
	<title>Password/PIN Changed</title>
	<script language='javascript'>self.close();</script>
</head>
	<body>
		<h3>Changes saved.</h3>
		<br/>
		<input type="button" value="Close Window" onClick="self.close();"/>
	</body>
<body>

</body>
</html>