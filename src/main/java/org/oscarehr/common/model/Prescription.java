package org.oscarehr.common.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.lang.StringUtils;

@Entity
@Table(name="prescription")
public class Prescription extends AbstractModel<Integer> implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "script_no")
	private Integer id;

	@Column(name = "provider_no")
	private String providerNo;

	@Column(name = "demographic_no")
	private Integer demographicId;

	@Column(name = "date_prescribed")
	@Temporal(TemporalType.DATE)
	private Date datePrescribed;

	@Column(name = "date_printed")
	@Temporal(TemporalType.DATE)
	private Date datePrinted;

	@Column(name = "dates_reprinted")
	private String datesReprinted;

	private String textView;

	@Column(name = "rx_comments")
	private String comments;

	@Temporal(TemporalType.TIMESTAMP)
	private Date lastUpdateDate;
	
	@PreRemove
	protected void jpaPreventDelete() {
		throw (new UnsupportedOperationException("Remove is not allowed for this type of item."));
	}

	@PreUpdate
	@PrePersist
	protected void autoSetUpdateTime()
	{
		lastUpdateDate=new Date();
	}

	@Override
	public Integer getId() {
		return id;
	}

	public String getProviderNo() {
		return (providerNo);
	}

	public void setProviderNo(String providerNo) {
		this.providerNo = providerNo;
	}

	public Integer getDemographicId() {
		return (demographicId);
	}

	public void setDemographicId(Integer demographicId) {
		this.demographicId = demographicId;
	}

	public Date getDatePrescribed() {
		return (datePrescribed);
	}

	public void setDatePrescribed(Date datePrescribed) {
		this.datePrescribed = datePrescribed;
	}

	public Date getDatePrinted() {
		return (datePrinted);
	}

	public void setDatePrinted(Date datePrinted) {
		this.datePrinted = datePrinted;
	}

	public String getDatesReprinted() {
		return (datesReprinted);
	}

	public void setDatesReprinted(String datesReprinted) {
		this.datesReprinted = StringUtils.trimToNull(datesReprinted);
	}

	public String getTextView() {
		return (textView);
	}

	public void setTextView(String textView) {
		this.textView = StringUtils.trimToNull(textView);
	}

	public String getComments() {
		return (comments);
	}

	public void setComments(String comments) {
		this.comments = StringUtils.trimToNull(comments);
	}

	public Date getLastUpdateDate() {
    	return (lastUpdateDate);
    }

}