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
<script type="text/JavaScript">
    function popupLabel(page) { //open a new popup window
        var windowprops = "height=500,width=800,location=no,scrollbars=yes,menubars=no,toolbars=no,resizable=yes,screenX=0,screenY=0,top=0,left=0";
        var popup = window.open(page, "QuatroShelter", windowprops);
        if (popup != null) {
            if (popup.opener == null) {
                popup.opener = self;
            }
            //   popup.focus();
        }
    }
</script>
<%@ include file="/taglibs.jsp" %>
<div id="projecttools" class="toolgroup">
    <div class="label"><strong>Navigator</strong></div>
    <div class="body">
        <div><span><c:out value="${client.formattedName}"/>(<c:out
                value="${client.demographicNo}"/>)</span> <c:choose>
            <c:when test="${'C' eq tabSummary}">
                <div><b>Summary</b></div>
            </c:when>
            <c:when test="${'V' eq tabSummary}">
                <div><a href="${pageContext.request.contextPath}/PMmodule/QuatroClientSummary.do"
                                name="actionParam" style="color:Navy;text-decoration:none;">Summary</a>
                </div>
            </c:when>
            <c:otherwise>
                &nbsp;
            </c:otherwise>
        </c:choose> <c:choose>
            <c:when test="${'C' eq tabHistory}">
                <div><b>History</b></div>
            </c:when>
            <c:when test="${'V' eq tabHistory}">
                <div><a href="${pageContext.request.contextPath}/PMmodule/ClientHistory.do"
                                name="actionParam" style="color:Navy;text-decoration:none;">History</a>
                </div>
            </c:when>
            <c:otherwise>
                &nbsp;
            </c:otherwise>
        </c:choose> <c:choose>
            <c:when test="${'C' eq tabIntake}">
                <div><b>Intake</b></div>
            </c:when>
            <c:when test="${'V' eq tabIntake}">
                <div><a href="${pageContext.request.contextPath}/PMmodule/QuatroIntake.do"
                                name="actionParam" style="color:Navy;text-decoration:none;">Intake</a>
                </div>
            </c:when>
            <c:otherwise>
                &nbsp;
            </c:otherwise>
        </c:choose> <c:choose>
            <c:when test="${'C' eq tabAdmission}">
                <div><b>Admission</b></div>
            </c:when>
            <c:when test="${'V' eq tabAdmission}">
                <div><a href="${pageContext.request.contextPath}/PMmodule/QuatroAdmission.do"
                                name="actionParam" style="color:Navy;text-decoration:none;">Admission</a>
                </div>
            </c:when>
            <c:otherwise>
                &nbsp;
            </c:otherwise>
        </c:choose> <c:choose>
            <c:when test="${'C' eq tabRefer}">
                <div><b>Referral</b></div>
            </c:when>
            <c:when test="${'V' eq tabRefer}">
                <div><a action="${pageContext.request.contextPath}/PMmodule/QuatroRefer.do"
                                name="actionParam" style="color:Navy;text-decoration:none;"> Referral</a>
                </div>
            </c:when>
            <c:otherwise>
                &nbsp;
            </c:otherwise>
        </c:choose> <c:choose>
            <c:when test="${'C' eq tabDischarge}">
                <div><b>Discharge</b></div>
            </c:when>
            <c:when test="${'V' eq tabDischarge}">
                <div><a href="${pageContext.request.contextPath}/PMmodule/QuatroDischarge.do"
                                name="actionParam" style="color:Navy;text-decoration:none;">Discharge</a>
                </div>
            </c:when>
            <c:otherwise>
                &nbsp;
            </c:otherwise>
        </c:choose> <c:choose>
            <c:when test="${'C' eq tabRestriction}">
                <div><b>Service Restriction</b></div>
            </c:when>
            <c:when test="${'V' eq tabRestriction}">
                <div><a href="${pageContext.request.contextPath}/PMmodule/QuatroServiceRestriction.do"
                                name="actionParam"
                                style="color:Navy;text-decoration:none;">Service Restriction</a>
                </div>
            </c:when>
            <c:otherwise>
                &nbsp;
            </c:otherwise>
        </c:choose> <c:choose>
            <c:when test="${'C' eq tabComplaint}">
                <div><b>Complaint</b></div>
            </c:when>
            <c:when test="${'V' eq tabComplaint}">
                <div><a href="${pageContext.request.contextPath}/PMmodule/QuatroComplaint.do"
                                name="actionParam" style="color:Navy;text-decoration:none;">Complaint</a>
                </div>
            </c:when>
            <c:otherwise>
                &nbsp;
            </c:otherwise>
        </c:choose> <c:choose>
            <c:when test="${'C' eq tabConsent}">
                <div><b>Consent</b></div>
            </c:when>
            <c:when test="${'V' eq tabConsent}">
                <div><a href="${pageContext.request.contextPath}/PMmodule/QuatroConsent.do"
                                name="actionParam" style="color:Navy;text-decoration:none;">Consent</a>
                </div>
            </c:when>
            <c:otherwise>
                &nbsp;
            </c:otherwise>
        </c:choose> <c:choose>
            <c:when test="${'C' eq tabCase}">
                <div><b>Case Management</b></div>
            </c:when>
            <c:when test="${'V' eq tabCase}">
                <div><a href="${pageContext.request.contextPath}/CaseManagementView2.do"
                                name="actionParam" style="color:Navy;text-decoration:none;">Case Management</a>
                </div>
            </c:when>
            <c:otherwise>
                &nbsp;
            </c:otherwise>
        </c:choose>
            <c:choose>
                <c:when test="${'C' eq tabAttachment}">
                    <div><b>Attachment</b></div>
                </c:when>
                <c:when test="${'V' eq tabAttachment}">
                    <div><a href="${pageContext.request.contextPath}/PMmodule/UploadFile.do"
                                    name="actionParam" style="color:Navy;text-decoration:none;">Attachment</a>
                    </div>
                </c:when>
                <c:otherwise>
                    &nbsp;
                </c:otherwise>
            </c:choose>
            <c:choose>
                <c:when test="${'C' eq tabTask}">
                    <div><b>Tasks</b></div>
                </c:when>
                <c:when test="${'V' eq tabTask}">
                    <div><a href="${pageContext.request.contextPath}/PMmodule/Task.do"
                                    name="actionParam" style="color:Navy;text-decoration:none;">Tasks</a>
                    </div>
                </c:when>
                <c:otherwise>
                    &nbsp;
                </c:otherwise>
            </c:choose>
            <c:choose>
                <c:when test="${'C' eq tabPrintLabel}">
                    <div><b>Print Label</b></div>
                </c:when>
                <c:when test="${'V' eq tabPrintLabel}">
                    <div>
                        <a href="javascript:popupLabel('${pageContext.request.contextPath}/demographic/printBarcodeAction.do?clientId=${clientId}')">
                            Print Label</a>
                    </div>
                </c:when>
                <c:otherwise>
                    &nbsp;
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>
