//CHECKSTYLE:OFF
/**
 * Copyright (c) 2024. Magenta Health. All Rights Reserved.
 * Copyright (C) 2007  Heart & Stroke Foundation
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

import java.util.List;
import javax.persistence.Query;

import org.oscarehr.common.model.Hsfo2Patient;
import org.springframework.stereotype.Repository;

@Repository
public class Hsfo2PatientDaoImpl extends AbstractDaoImpl<Hsfo2Patient> implements Hsfo2PatientDao {
    public Hsfo2PatientDaoImpl() {
        super(Hsfo2Patient.class);
    }

    public Hsfo2Patient getHsfoPatientByPatientId(String patientId) {
        String sqlCommand = "select x from Hsfo2Patient x where x.Patient_Id=?1 order by x.id desc ";
        Query query = entityManager.createQuery(sqlCommand);
        query.setParameter(1, patientId);
        List<Hsfo2Patient> results = query.getResultList();
        if (results != null && results.size() > 0)
            return results.get(0);
        else
            return null;
    }

    public List<Hsfo2Patient> getAllHsfoPatients() {
        String sqlCommand = "select x from Hsfo2Patient x";
        Query query = entityManager.createQuery(sqlCommand);

        @SuppressWarnings("unchecked")
        List<Hsfo2Patient> results = query.getResultList();

        return results;
    }
}
