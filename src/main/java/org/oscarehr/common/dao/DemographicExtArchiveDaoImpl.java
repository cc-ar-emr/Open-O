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

import org.oscarehr.common.model.DemographicExt;
import org.oscarehr.common.model.DemographicExtArchive;
import org.springframework.stereotype.Repository;

@Repository
public class DemographicExtArchiveDaoImpl extends AbstractDaoImpl<DemographicExtArchive>
        implements DemographicExtArchiveDao {

    public DemographicExtArchiveDaoImpl() {
        super(DemographicExtArchive.class);
    }

    @Override
    public List<DemographicExtArchive> getDemographicExtArchiveByDemoAndKey(Integer demographicNo, String key) {
        Query query = entityManager.createQuery(
                "SELECT d from DemographicExtArchive d where d.demographicNo=?1 and d.key = ?2 order by d.dateCreated DESC");
        query.setParameter(1, demographicNo);
        query.setParameter(2, key);

        @SuppressWarnings("unchecked")
        List<DemographicExtArchive> results = query.getResultList();
        return results;
    }

    @Override
    public DemographicExtArchive getDemographicExtArchiveByArchiveIdAndKey(Long archiveId, String key) {
        Query query = entityManager.createQuery(
                "SELECT d from DemographicExtArchive d where d.archiveId=?1 and d.key = ?2 order by d.dateCreated DESC");
        query.setParameter(1, archiveId);
        query.setParameter(2, key);

        return this.getSingleResultOrNull(query);
    }

    @Override
    public List<DemographicExtArchive> getDemographicExtArchiveByArchiveId(Long archiveId) {
        Query query = entityManager.createQuery("SELECT d from DemographicExtArchive d where d.archiveId=?1");
        query.setParameter(1, archiveId);

        @SuppressWarnings("unchecked")
        List<DemographicExtArchive> results = query.getResultList();
        return results;
    }

    @Override
    public List<DemographicExtArchive> getDemographicExtArchiveByDemoReverseCronological(Integer demographicNo) {
        Query query = entityManager.createQuery(
                "SELECT d from DemographicExtArchive d where d.demographicNo=?1 order by d.dateCreated ASC");
        query.setParameter(1, demographicNo);

        @SuppressWarnings("unchecked")
        List<DemographicExtArchive> results = query.getResultList();
        return results;
    }

    @Override
    public Integer archiveDemographicExt(DemographicExt de) {
        DemographicExtArchive dea = new DemographicExtArchive(de);
        persist(dea);
        return dea.getId();
    }
}
