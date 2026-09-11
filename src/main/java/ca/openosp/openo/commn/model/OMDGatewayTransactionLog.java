/**
 * Copyright (c) 2001-2002. Department of Family Medicine, McMaster University. All Rights Reserved.
 * This software is published under the GPL GNU General Public License. This program is free
 * software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 * <p>
 * This software was written for the Department of Family Medicine McMaster University Hamilton
 * Ontario, Canada
 */
package ca.openosp.openo.commn.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

@Entity
public class OMDGatewayTransactionLog extends AbstractModel<Integer> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  @Temporal(TemporalType.TIMESTAMP)
  private Date started;
  @Temporal(TemporalType.TIMESTAMP)
  private Date ended;
  private String initiatingProviderNo;
  private String transactionType;
  private String externalSystem;
  private Integer demographicNo;
  private Integer resultCode;
  
  // The outcome code the EHR service itself returned, for example IN_0045 or CONSENT_EXISTS.
  // resultCode holds the HTTP status, which says the call was rejected but not why; this holds
  // the reason, as a value that can be filtered on rather than text buried in the error body.
  private String ehrResultCode;

  // Resource ids from the FHIR message the interaction carried. The message id is one value; a
  // search returns many dispenses under a single transaction, so those are held as a comma
  // separated list. They are a record rather than a lookup: nothing queries or counts on them.
  private String messageHeaderId;
  private String medicationDispenseIds;
  private Boolean success;
  private String error;
  private String dataSent;
  private String dataRecieved;
  private String headers;
  private String uao;
  private String oscarSessionId;
  private String contextSessionId;
  private String uniqueSessionId;
  private String xRequestId;
  private String xLobTxId;
  private String xCorrelationId;
  private String xGtwyClientId;
  private Long secondsLeft;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Date getStarted() {
    return started;
  }

  public void setStarted(Date started) {
    this.started = started;
  }

  public String getInitiatingProviderNo() {
    return initiatingProviderNo;
  }

  public void setInitiatingProviderNo(String initiatingProviderNo) {
    this.initiatingProviderNo = initiatingProviderNo;
  }

  public String getTransactionType() {
    return transactionType;
  }

  public void setTransactionType(String transactionType) {
    this.transactionType = transactionType;
  }

  public String getExternalSystem() {
    return externalSystem;
  }

  public void setExternalSystem(String externalSystem) {
    this.externalSystem = externalSystem;
  }

  public Integer getDemographicNo() {
    return demographicNo;
  }

  public void setDemographicNo(Integer demographicNo) {
    this.demographicNo = demographicNo;
  }

  public Integer getResultCode() {
    return resultCode;
  }

  public void setResultCode(Integer resultCode) {
    this.resultCode = resultCode;
  }

  /**
   * @return String the EHR service's own outcome code, or null when the response carried none
   * @since 2026-08-04
   */
  public String getEhrResultCode() {
    return ehrResultCode;
  }

  public void setEhrResultCode(String ehrResultCode) {
    this.ehrResultCode = ehrResultCode;
  }

  /**
   * @return String the id of the MessageHeader resource the response carried, or null when it
   *     carried none
   * @since 2026-08-12
   */
  public String getMessageHeaderId() {
    return messageHeaderId;
  }

  public void setMessageHeaderId(String messageHeaderId) {
    this.messageHeaderId = messageHeaderId;
  }

  /**
   * @return String the dispense ids the interaction returned, as a comma separated list; a list too
   *     long for the column ends with a count of the ids left out, so what is returned may not be
   *     the whole set
   * @since 2026-08-12
   */
  public String getMedicationDispenseIds() {
    return medicationDispenseIds;
  }

  /**
   * Records the dispense ids an interaction returned, as a comma separated list.
   *
   * <p>A list longer than the column holds is cut back to whole ids and ends with a count of the
   * ones left out, so a row never carries half an id and never reads as complete when it is not.
   *
   * @param medicationDispenseIds String the comma separated ids, which may be absent
   */
  public void setMedicationDispenseIds(String medicationDispenseIds) {
    this.medicationDispenseIds = boundedIdList(medicationDispenseIds);
  }

  /** Longest id list the column holds, in characters. */
  private static final int MAX_DISPENSE_IDS_LENGTH = 16000;

  private static String boundedIdList(String ids) {
    if (ids == null || ids.length() <= MAX_DISPENSE_IDS_LENGTH) {
      return ids;
    }
    String[] all = ids.split(",");
    StringBuilder kept = new StringBuilder();
    int keptCount = 0;
    for (String id : all) {
      String candidate = keptCount == 0 ? id : kept + "," + id;
      // leave room for the marker that says how many were left out
      if (candidate.length() > MAX_DISPENSE_IDS_LENGTH - 32) {
        break;
      }
      if (keptCount > 0) {
        kept.append(',');
      }
      kept.append(id);
      keptCount++;
    }
    return kept + ",(" + (all.length - keptCount) + " more not recorded)";
  }

  public Boolean getSuccess() {
    return success;
  }

  public void setSuccess(Boolean success) {
    this.success = success;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public String getHeaders() {
    return headers;
  }

  public void setHeaders(String headers) {
    this.headers = headers;
  }

  public String getUao() {
    return uao;
  }

  public void setUao(String uao) {
    this.uao = uao;
  }

  public String getOscarSessionId() {
    return oscarSessionId;
  }

  public void setOscarSessionId(String oscarSessionId) {
    this.oscarSessionId = oscarSessionId;
  }

  public String getContextSessionId() {
    return contextSessionId;
  }

  public void setContextSessionId(String contextSessionId) {
    this.contextSessionId = contextSessionId;
  }

  public Long getSecondsLeft() {
    return secondsLeft;
  }

  public void setSecondsLeft(Long secondsLeft) {
    this.secondsLeft = secondsLeft;
  }

  public String getDataSent() {
    return dataSent;
  }

  public void setDataSent(String dataSent) {
    this.dataSent = dataSent;
  }

  public String getUniqueSessionId() {
    return uniqueSessionId;
  }

  public void setUniqueSessionId(String uniqueSessionId) {
    this.uniqueSessionId = uniqueSessionId;
  }

  public String getDataRecieved() {
    return dataRecieved;
  }

  public void setDataRecieved(String dataRecieved) {
    this.dataRecieved = dataRecieved;
  }

  public Date getEnded() {
    return ended;
  }

  public void setEnded(Date ended) {
    this.ended = ended;
  }

  public String getxRequestId() {
    return xRequestId;
  }

  public void setxRequestId(String xRequestId) {
    this.xRequestId = xRequestId;
  }

  public String getxLobTxId() {
    return xLobTxId;
  }

  public void setxLobTxId(String xLobTxId) {
    this.xLobTxId = xLobTxId;
  }

  public String getxCorrelationId() {
    return xCorrelationId;
  }

  public void setxCorrelationId(String xCorrelationId) {
    this.xCorrelationId = xCorrelationId;
  }

  public String getxGtwyClientId() {
    return xGtwyClientId;
  }

  public void setxGtwyClientId(String xGtwyClientId) {
    this.xGtwyClientId = xGtwyClientId;
  }
}