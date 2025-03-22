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
<%--
Liberated by Dennis Warren @ Colcamex 
Created by RJ
Required Parameters to plug-in: 

	disabled : returns true or false.
	quickList : default quick list name by parameter

 --%>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic"%>


<table id="dxCodeQuicklist">
	<tr>
		<td class="heading">
		${ quickList }
			<div class="panel panel-default">
	  			<div class="panel-body">
					<bean:message key="oscarResearch.oscarDxResearch.quickList" />
					<small class="pull-right">
						<a class="oscar-dialog-link" href="dxResearchCustomization.jsp">
							add/edit
						</a>
					</small>
				</div>
			</div>
		</td>
	</tr>
	<tr>
		<td class="quickList">	
			<html:select styleClass="form-control" style="overflow:auto" property="quickList" onchange="javascript:changeList(this,'${ demographicNo }','${ providerNo }');" >			
				<logic:iterate id="quickLists" name="allQuickLists" property="dxQuickListBeanVector" >				
					<option value="${ quickLists.quickListName }" ${ quickLists.quickListName eq param.quickList || quickLists.lastUsed eq 'Selected' ? 'selected' : '' } >			
						<bean:write	name="quickLists" property="quickListName" />
					</option>
				</logic:iterate>
			</html:select>	
 				<ul class="list-group">
  				<logic:iterate id="item" name="allQuickListItems" property="dxQuickListItemsVector">
  					<li class="list-group-item">
  					<span class="pull-right">
 						<html:link href="#" title="${ item.dxSearchCode }"
						onclick="javascript:submitform( '${ item.dxSearchCode }', '${ item.type }' )" >
							add
					</html:link>
					</span>
  					<bean:write name="item" property="type" />: 
  					<bean:write name="item" property="description" />	
					</li>
				</logic:iterate>
			</ul>
		</td>
	</tr>		
</table>

<script type="text/javascript">
	function changeList(quickList, demographicNo, providerNo){       
	    location.href = 'setupDxResearch.do?demographicNo='
	    		+demographicNo
	    		+'&quickList='
	    		+quickList.value
	    		+'&providerNo='
	    		+providerNo;
	}
</script>
