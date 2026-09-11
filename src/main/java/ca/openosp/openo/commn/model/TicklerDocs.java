//CHECKSTYLE:OFF
/**
 * Copyright (c) 2001-2002. Department of Family Medicine, McMaster University. All Rights Reserved.
 * This software is published under the GPL GNU General Public License.
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * <p>
 * This software was written for the
 * Department of Family Medicine
 * McMaster University
 * Hamilton
 * Ontario, Canada
 */


package ca.openosp.openo.commn.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Links a document (document, lab, eForm, HRM report or encounter form) to a Tickler.
 *
 * <p>This is the Tickler counterpart of {@link ConsultDocs} and {@code EFormDocs}; it backs the
 * shared document attachment component (see {@code documentManager/attachDocument.jsp}). Detached
 * attachments are soft-deleted by setting {@link #deleted} to {@link #DELETED} rather than being
 * physically removed.</p>
 *
 * @since 2026-06-12
 */
@Entity
@Table(name = "ticklerdocs")
public class TicklerDocs extends AbstractModel<Integer> {
    public static final String DOCTYPE_DOC = "D";
    public static final String DOCTYPE_EFORM = "E";
    public static final String DOCTYPE_LAB = "L";

    public static final String DOCTYPE_FORM = "F";
    public static final String DOCTYPE_HRM = "H";
    public static final String DELETED = "Y";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "tickler_id")
    private int ticklerId;

    @Column(name = "document_no")
    private int documentNo;

    @Column(name = "doctype")
    private String docType;

    private String deleted;

    @Column(name = "attach_date")
    @Temporal(TemporalType.DATE)
    private Date attachDate;

    @Column(name = "provider_no")
    private String providerNo;

    public TicklerDocs() {
    }

    public TicklerDocs(int ticklerId, int documentNo, String docType, String providerNo) {
        setTicklerId(ticklerId);
        setDocumentNo(documentNo);
        setDocType(docType);
        setProviderNo(providerNo);
        setAttachDate(new Date());
    }

    @Override
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getTicklerId() {
        return ticklerId;
    }

    public void setTicklerId(int ticklerId) {
        this.ticklerId = ticklerId;
    }

    public int getDocumentNo() {
        return documentNo;
    }

    public void setDocumentNo(int documentNo) {
        this.documentNo = documentNo;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getDeleted() {
        return deleted;
    }

    public void setDeleted(String deleted) {
        this.deleted = deleted;
    }

    public Date getAttachDate() {
        return attachDate;
    }

    public void setAttachDate(Date attachDate) {
        this.attachDate = attachDate;
    }

    public String getProviderNo() {
        return providerNo;
    }

    public void setProviderNo(String providerNo) {
        this.providerNo = providerNo;
    }
}
