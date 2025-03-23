//CHECKSTYLE:OFF
/**
 * Copyright (c) 2024. Magenta Health. All Rights Reserved.
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
 * <p>
 * Modifications made by Magenta Health in 2024.
 */
package org.oscarehr.common.dao;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.persistence.Query;

import org.oscarehr.common.model.BornTransmissionLog;
import org.springframework.stereotype.Repository;

import oscar.util.UtilDateUtilities;

@Repository
public class BornTransmissionLogDaoImpl extends AbstractDaoImpl<BornTransmissionLog> implements BornTransmissionLogDao {

    public BornTransmissionLogDaoImpl() {
        super(BornTransmissionLog.class);
    }

    @Override
    public Long getSeqNoToday(String filenameStart, Integer id) {
        String today = UtilDateUtilities.getToday("yyyy-MM-dd");
        Date todayDate = UtilDateUtilities.StringToDate(today, "yyyy-MM-dd");
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(todayDate);
        cal.add(Calendar.DATE, 1);
        String tomorrow = UtilDateUtilities.DateToString(cal.getTime(), "yyyy-MM-dd");
        Date tomorrowDate = UtilDateUtilities.StringToDate(tomorrow, "yyyy-MM-dd");

        String sql = "select count(*) from BornTransmissionLog b" +
            " where b.filename like ?1 and b.id < ?2" +
            " and b.submitDateTime >= ?3 and b.submitDateTime < ?4";
        Query query = entityManager.createQuery(sql);
        query.setParameter(1, "%" + filenameStart + "%");
        query.setParameter(2, id);
        query.setParameter(3, todayDate);
        query.setParameter(4, tomorrowDate);

        return (Long) query.getSingleResult() + 1;
    }
}
