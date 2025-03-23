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
import java.util.GregorianCalendar;
import java.util.List;

import javax.persistence.Query;

import org.oscarehr.common.model.SurveyData;
import org.springframework.stereotype.Repository;

@Repository
@SuppressWarnings("unchecked")
public class SurveyDataDaoImpl extends AbstractDaoImpl<SurveyData> implements SurveyDataDao {

    public SurveyDataDaoImpl() {
        super(SurveyData.class);
    }

    @Override
    public int getMaxProcessed(String surveyId) {
        Query q = entityManager.createQuery("SELECT MAX(s.processed) FROM SurveyData s WHERE s.surveyId = ?1");
        q.setParameter(1, surveyId);
        Object result = q.getSingleResult();
        if (result == null) {
            return 0;
        }
        return (Integer) result;
    }

    @Override
    public int getProcessCount(String surveyId) {
        String sql = "SELECT COUNT(s.id) FROM SurveyData s " +
                "WHERE s.surveyId = ?1 " +
                "AND s.processed IS NULL " +
                "AND s.status = 'A'";
        Query query = entityManager.createQuery(sql);
        query.setParameter(1, surveyId);
        Object result = query.getSingleResult();
        if (result == null) {
            return 0;
        }
        return ((Long) result).intValue();
    }

    @Override
    public List<SurveyData> findByDemoSurveyIdAndPeriod(Integer demoNo, String surveyId, int cutoffInDays) {
        Query query = createQuery("sd", "sd.surveyId = ?1 AND sd.demographicNo = ?2 AND sd.surveyDate >= ?3");
        query.setParameter(1, surveyId);
        query.setParameter(2, demoNo);
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.roll(Calendar.DAY_OF_YEAR, cutoffInDays * (-1));
        query.setParameter(3, calendar.getTime());
        return query.getResultList();
    }

    @Override
    public List<Object[]> countStatuses(String surveyId) {
        String sql = "SELECT sd.status , COUNT(sd.status) FROM SurveyData sd WHERE sd.surveyId = ?1 GROUP BY sd.status";
        Query query = entityManager.createQuery(sql);
        query.setParameter(1, surveyId);
        return query.getResultList();
    }

    @Override
    public List<Object[]> countAnswers(String surveyId) {
        String sql = "SELECT sd.answer, COUNT(sd.answer) FROM SurveyData sd WHERE sd.surveyId = ?1 AND sd.status = 'A' GROUP BY sd.answer";
        Query query = entityManager.createQuery(sql);
        query.setParameter(1, surveyId);
        return query.getResultList();
    }
}
