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


package org.oscarehr.ws.transfer_objects;

import java.util.Calendar;
import java.util.List;

import org.oscarehr.common.model.Appointment.BookingSource;
import org.oscarehr.common.model.AppointmentArchive;
import org.springframework.beans.BeanUtils;

import oscar.util.DateUtils;

public final class AppointmentArchiveTransfer {
    private Integer id;
    private Integer appointmentId;
    private String providerNo;
    private Calendar appointmentStartDateTime;
    private Calendar appointmentEndDateTime;
    private String name;
    private int demographicNo;
    private int programId;
    private String notes;
    private String reason;
    private String location;
    private String resources;
    private String type;
    private String style;
    private String billing;
    private String status;
    private Calendar createDateTime;
    private Calendar updateDateTime;
    private String creator;
    private String lastUpdateUser;
    private String remarks;
    private String urgency;
    private BookingSource bookingSource;

    public Integer getId() {
        return (id);
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAppointmentId() {
        return (appointmentId);
    }

    public void setAppointmentId(Integer appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getProviderNo() {
        return (providerNo);
    }

    public void setProviderNo(String providerNo) {
        this.providerNo = providerNo;
    }

    public Calendar getAppointmentStartDateTime() {
        return (appointmentStartDateTime);
    }

    public void setAppointmentStartDateTime(Calendar appointmentStartDateTime) {
        this.appointmentStartDateTime = appointmentStartDateTime;
    }

    public Calendar getAppointmentEndDateTime() {
        return (appointmentEndDateTime);
    }

    public void setAppointmentEndDateTime(Calendar appointmentEndDateTime) {
        this.appointmentEndDateTime = appointmentEndDateTime;
    }

    public String getName() {
        return (name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDemographicNo() {
        return (demographicNo);
    }

    public void setDemographicNo(int demographicNo) {
        this.demographicNo = demographicNo;
    }

    public int getProgramId() {
        return (programId);
    }

    public void setProgramId(int programId) {
        this.programId = programId;
    }

    public String getNotes() {
        return (notes);
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getReason() {
        return (reason);
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getLocation() {
        return (location);
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getResources() {
        return (resources);
    }

    public void setResources(String resources) {
        this.resources = resources;
    }

    public String getType() {
        return (type);
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStyle() {
        return (style);
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getBilling() {
        return (billing);
    }

    public void setBilling(String billing) {
        this.billing = billing;
    }

    public String getStatus() {
        return (status);
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Calendar getCreateDateTime() {
        return (createDateTime);
    }

    public void setCreateDateTime(Calendar createDateTime) {
        this.createDateTime = createDateTime;
    }

    public Calendar getUpdateDateTime() {
        return (updateDateTime);
    }

    public void setUpdateDateTime(Calendar updateDateTime) {
        this.updateDateTime = updateDateTime;
    }

    public String getCreator() {
        return (creator);
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getLastUpdateUser() {
        return (lastUpdateUser);
    }

    public void setLastUpdateUser(String lastUpdateUser) {
        this.lastUpdateUser = lastUpdateUser;
    }

    public String getRemarks() {
        return (remarks);
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getUrgency() {
        return (urgency);
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public BookingSource getBookingSource() {
        return (bookingSource);
    }

    public void setBookingSource(BookingSource bookingSource) {
        this.bookingSource = bookingSource;
    }

    public static AppointmentArchiveTransfer toTransfer(AppointmentArchive appointment, boolean useGMTTime) {
        if (appointment == null) return (null);

        AppointmentArchiveTransfer appointmentTransfer = new AppointmentArchiveTransfer();

        String[] ignored = {"appointmentDate", "startTime", "endTime", "createDateTime", "updateDateTime", "creatorSecurityId"};
        BeanUtils.copyProperties(appointment, appointmentTransfer, ignored);

        Calendar cal = DateUtils.toGregorianCalendar(appointment.getAppointmentDate(), appointment.getStartTime());
        cal = AppointmentTransfer.setToGMTIfRequired(cal, useGMTTime);
        appointmentTransfer.setAppointmentStartDateTime(cal);

        cal = DateUtils.toGregorianCalendar(appointment.getAppointmentDate(), appointment.getEndTime());
        cal = AppointmentTransfer.setToGMTIfRequired(cal, useGMTTime);
        appointmentTransfer.setAppointmentEndDateTime(cal);

        cal = DateUtils.toGregorianCalendar(appointment.getCreateDateTime());
        cal = AppointmentTransfer.setToGMTIfRequired(cal, useGMTTime);
        appointmentTransfer.setCreateDateTime(cal);

        cal = DateUtils.toGregorianCalendar(appointment.getUpdateDateTime());
        cal = AppointmentTransfer.setToGMTIfRequired(cal, useGMTTime);
        appointmentTransfer.setUpdateDateTime(cal);

        return (appointmentTransfer);
    }

    public static AppointmentArchiveTransfer[] toTransfers(List<AppointmentArchive> appointments, boolean useGMTTime) {
        AppointmentArchiveTransfer[] result = new AppointmentArchiveTransfer[appointments.size()];

        for (int i = 0; i < appointments.size(); i++) {
            result[i] = toTransfer(appointments.get(i), useGMTTime);
        }

        return (result);
    }
}
