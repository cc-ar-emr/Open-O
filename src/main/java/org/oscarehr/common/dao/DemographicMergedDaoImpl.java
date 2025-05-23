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

import java.util.List;

import javax.persistence.Query;

import org.oscarehr.common.model.DemographicMerged;
import org.springframework.stereotype.Repository;

@Repository
public class DemographicMergedDaoImpl extends AbstractDaoImpl<DemographicMerged> implements DemographicMergedDao {

    public DemographicMergedDaoImpl() {
        super(DemographicMerged.class);
    }

    @Override
    public List<DemographicMerged> findCurrentByMergedTo(int demographicNo) {
        Query q = entityManager.createQuery("select d from DemographicMerged d where d.mergedTo=?1 and d.deleted=0");
        q.setParameter(1, demographicNo);

        @SuppressWarnings("unchecked")
        List<DemographicMerged> results = q.getResultList();

        return results;
    }

    @Override
    public List<DemographicMerged> findCurrentByDemographicNo(int demographicNo) {
        Query q = entityManager.createQuery("select d from DemographicMerged d where d.demographicNo=?1 and d.deleted=0");
        q.setParameter(1, demographicNo);

        @SuppressWarnings("unchecked")
        List<DemographicMerged> results = q.getResultList();

        return results;
    }

    @Override
    public List<DemographicMerged> findByDemographicNo(int demographicNo) {
        Query q = entityManager.createQuery("select d from DemographicMerged d where d.demographicNo=?1");
        q.setParameter(1, demographicNo);

        @SuppressWarnings("unchecked")
        List<DemographicMerged> results = q.getResultList();

        return results;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DemographicMerged> findByParentAndChildIds(Integer parentId, Integer childId) {
        Query q = createQuery("d", "d.demographicNo = ?1 AND d.mergedTo = ?2");
        q.setParameter(1, parentId);
        q.setParameter(2, childId);
        return q.getResultList();
    }
}
