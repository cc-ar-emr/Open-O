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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.persistence.Query;

import org.oscarehr.common.model.CustomFilter;
import org.oscarehr.common.model.Provider;
import org.oscarehr.common.model.Tickler;
import org.springframework.stereotype.Repository;

@Repository
public class TicklerDaoImpl extends AbstractDaoImpl<Tickler> implements TicklerDao {

    public TicklerDaoImpl() {
        super(Tickler.class);
    }

    @Override
    public Tickler find(Integer id) {
        Tickler tickler = super.find(id);
        tickler.getUpdates().size();
        return tickler;
    }

    @Override
    public List<Tickler> findActiveByMessageForPatients(List<Integer> demographicNos, String remString) {

        //weird logic here, beware.
        if (demographicNos.isEmpty())
            demographicNos.add(0);

        Query query = entityManager.createQuery("select t from Tickler t where t.demographicNo in (?1) and t.status = 'A' and t.message like ?2");
        query.setParameter(1, demographicNos);
        query.setParameter(2, "%" + remString + "%");

        @SuppressWarnings("unchecked")
        List<Tickler> results = query.getResultList();

        return results;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Tickler> findActiveByDemographicNoAndMessage(Integer demoNo, String message) {

        Query query = entityManager.createQuery("select t from Tickler t where t.demographicNo = ?1 and t.message = ?2 and t.status = 'A'");
        query.setParameter(1, demoNo);
        query.setParameter(2, message);

        List<Tickler> results = query.getResultList();

        return results;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Tickler> findActiveByDemographicNo(Integer demoNo) {

        Query query = entityManager.createQuery("select t from Tickler t where t.demographicNo = ?1 and t.status = 'A' order by t.serviceDate desc");
        query.setParameter(1, demoNo);

        List<Tickler> results = query.getResultList();

        return results;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Tickler> findByTicklerNoDemo(Integer ticklerNo, Integer demoNo) {

        Query query = entityManager.createQuery("select t from Tickler t where t.id = ?1 AND t.demographicNo = ?2 AND t.status != 'D'");
        query.setParameter(1, ticklerNo);
        query.setParameter(2, demoNo);

        List<Tickler> results = query.getResultList();

        return results;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Tickler> findByTicklerNoAssignedTo(Integer ticklerNo, String assignedTo, Integer demoNo) {

        Query query = entityManager.createQuery("select t from Tickler t where t.id = ?1 AND t.taskAssignedTo = ?2 AND t.demographicNo = ?3 AND t.status != 'D'");
        query.setParameter(1, ticklerNo);
        query.setParameter(2, assignedTo);
        query.setParameter(3, demoNo);

        List<Tickler> results = query.getResultList();

        return results;
    }

    /**
     * Finds all ticklers for the specified demographic, assigned to and message fields.
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Tickler> findByDemographicIdTaskAssignedToAndMessage(Integer demographicNo, String taskAssignedTo, String message) {
        Query query = entityManager.createQuery("select t from Tickler t where t.demographicNo = ?1 AND t.taskAssignedTo = ?2 and t.message = ?3");
        query.setParameter(1, demographicNo);
        query.setParameter(2, taskAssignedTo);
        query.setParameter(3, message);

        List<Tickler> results = query.getResultList();

        return results;
    }


    @SuppressWarnings("unchecked")
    @Override
    public List<Tickler> search_tickler_bydemo(Integer demographicNo, String status, Date beginDate, Date endDate) {
        Query query = entityManager.createQuery("SELECT t FROM Tickler t WHERE t.demographicNo = ?1 and t.status = ?2 and t.serviceDate >= ?3 and t.serviceDate <= ?4 order by t.serviceDate desc");
        query.setParameter(1, demographicNo);
        query.setParameter(2, this.convertStatus(status));
        query.setParameter(3, beginDate);
        query.setParameter(4, endDate);

        List<Tickler> results = query.getResultList();

        return results;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Tickler> search_tickler(Integer demographicNo, Date endDate) {
        Query query = entityManager.createQuery("SELECT t FROM Tickler t WHERE t.demographicNo = ?1 and t.status = 'A' and t.serviceDate <= ?2 order by t.serviceDate desc");
        query.setParameter(1, demographicNo);
        query.setParameter(2, endDate);


        List<Tickler> results = query.getResultList();

        return results;
    }

    /**
     * Finds all ticklers for the specified demographic, and date range.
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Tickler> listTicklers(Integer demographicNo, Date beginDate, Date endDate) {
        Query query = entityManager.createQuery("select t FROM Tickler t where t.status = 'A' and (t.serviceDate >= ?1 and t.serviceDate <= ?2) and t.demographicNo = ?3 order by t.serviceDate desc");
        query.setParameter(1, beginDate);
        query.setParameter(2, endDate);
        query.setParameter(3, demographicNo);

        List<Tickler> results = query.getResultList();

        return results;
    }

    @Override
    public int getActiveTicklerCount(String providerNo) {
        Query query = entityManager.createQuery("select count(t) FROM Tickler t where t.status = 'A' and t.serviceDate <= ?1 and (t.taskAssignedTo  = ?2 or t.taskAssignedTo='All Providers')");
        query.setParameter(1, new Date());
        query.setParameter(2, providerNo);

        Long result = (Long) query.getSingleResult();

        return result.intValue();
    }

    @Override
    public int getActiveTicklerByDemoCount(Integer demographicNo) {
        Query query = entityManager.createQuery("select count(t) FROM Tickler t where t.status = 'A' and t.serviceDate <= ?1 and t.demographicNo  = ?2 ");
        query.setParameter(1, new Date());
        query.setParameter(2, demographicNo);

        Long result = (Long) query.getSingleResult();

        return result.intValue();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Tickler> getTicklers(CustomFilter filter, int offset, int limit) {
        String sql = "select t";
        ArrayList<Object> paramList = new ArrayList<Object>();
        sql = getTicklerQueryString(sql, paramList, filter);

        Query query = entityManager.createQuery(sql);
        for (int x = 0; x < paramList.size(); x++) {
            query.setParameter(x + 1, paramList.get(x));
        }
        query.setFirstResult(offset);
        setLimit(query, limit);
        return query.getResultList();
    }


    /**
     * @param filter
     * @return
     * @Deprecated Get Ticklers.
     * <p>
     * Warning..this will limit you to TicklerDao.MAX_LIST_RETURN_SIZE
     */
    @Override
    public List<Tickler> getTicklers(CustomFilter filter) {
        return getTicklers(filter, 0, TicklerDao.MAX_LIST_RETURN_SIZE);
    }


    @Override
    public int getNumTicklers(CustomFilter filter) {
        List<Object> paramList = new ArrayList<Object>();
        String sql = "select count(t)";
        sql = getTicklerQueryString(sql, paramList, filter);

        Query query = entityManager.createQuery(sql);
        for (int x = 0; x < paramList.size(); x++) {
            query.setParameter(x + 1, paramList.get(x));
        }

        Long result = (Long) query.getSingleResult();

        return result.intValue();
    }

    /**
     * selectQuery is in the form of "SELECT t"
     *
     * @param selectQuery
     * @param paramList
     * @param filter
     * @return
     */
    private String getTicklerQueryString(String selectQuery, List<Object> paramList, CustomFilter filter) {
//		String tickler_date_order = filter.getSort_order();

        String query = selectQuery + " FROM Tickler t WHERE 1=1 ";
        int paramIndex = 1;
        boolean includeMRPClause = true;
        boolean includeProviderClause = true;
        boolean includeAssigneeClause = true;
        boolean includeStatusClause = true;
        boolean includeClientClause = true;
        boolean includeDemographicClause = true;
        boolean includeProgramClause = true;
        boolean includeMessage = true;
        boolean includePriorityClause = true;
        boolean includeServiceStartDateClause = false;
        boolean includeServiceEndDateClause = false;

        if (filter.getStartDate() != null) {
            includeServiceStartDateClause = true;
        }
        if (filter.getEndDate() != null) {
            includeServiceEndDateClause = true;
        }

        if (filter.getProgramId() == null || "".equals(filter.getProgramId()) || filter.getProgramId().equals("All Programs")) {
            includeProgramClause = false;
        }
        if (filter.getProvider() == null || filter.getProvider().equals("All Providers") || filter.getProvider().equals("")) {
            includeProviderClause = false;
        }
        if (filter.getAssignee() == null || filter.getAssignee().equals("All Providers") || filter.getAssignee().equals("")) {
            includeAssigneeClause = false;
        }
        if (filter.getClient() == null || filter.getClient().equals("All Clients")) {
            includeClientClause = false;
        }
        if (filter.getDemographicNo() == null || filter.getDemographicNo().equals("") || filter.getDemographicNo().equalsIgnoreCase("All Clients")) {
            includeDemographicClause = false;
        }
        if (filter.getStatus().equals("") || filter.getStatus().equals("Z")) {
            includeStatusClause = false;
        }
        if (filter.getPriority() == null || "".equals(filter.getPriority())) {
            includePriorityClause = false;
        }
        if (filter.getMrp() == null || filter.getMrp().equals("All Providers") || filter.getMrp().equals("")) {
            includeMRPClause = false;
        }
        if (filter.getMessage() == null || filter.getMessage().trim().isEmpty()) {
            includeMessage = false;
        }

        if (includeMRPClause) {
            query = selectQuery + " FROM Tickler t, Demographic d where d.DemographicNo = t.demographicNo and d.ProviderNo = ?" + paramIndex++;
            paramList.add(filter.getMrp());
        }

        if (includeServiceStartDateClause) {
            query = query + " and t.serviceDate >= ?" + paramIndex++;
            paramList.add(filter.getStartDate());
        }

        if (includeServiceEndDateClause) {
            query = query + " and t.serviceDate <= ?" + paramIndex++;

            Calendar cal = Calendar.getInstance();
            cal.setTime(filter.getEndDate());

            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);

            paramList.add(new Date(cal.getTime().getTime()));
        }

        //TODO: IN clause
        if (includeProviderClause) {
            query = query + " and t.creator IN (";
            Set<Provider> pset = filter.getProviders();
            Provider[] providers = pset.toArray(new Provider[pset.size()]);
            for (int x = 0; x < providers.length; x++) {
                if (x > 0) {
                    query += ",";
                }
                query += "?" + paramIndex++;
                paramList.add(providers[x].getProviderNo());
            }
            query += ")";
        }

        //TODO: IN clause
        if (includeAssigneeClause) {
            query = query + " and t.taskAssignedTo IN (";
            Set<Provider> pset = filter.getAssignees();
            Provider[] providers = pset.toArray(new Provider[pset.size()]);
            for (int x = 0; x < providers.length; x++) {
                if (x > 0) {
                    query += ",";
                }
                query += "?" + paramIndex++;
                paramList.add(providers[x].getProviderNo());
            }
            query += ")";
        }

        if (includeProgramClause) {
            query = query + " and t.programId = ?" + paramIndex++;
            paramList.add(Integer.valueOf(filter.getProgramId()));
        }
        if (includeStatusClause) {
            query = query + " and t.status = ?" + paramIndex++;
            paramList.add(convertStatus(filter.getStatus()));
        }

        if (includePriorityClause) {
            query = query + " and t.priority = ?" + paramIndex++;
            paramList.add(convertPriority(filter.getPriority()));
        }

        if (includeClientClause) {
            query = query + " and t.demographicNo = ?" + paramIndex++;
            paramList.add(Integer.parseInt(filter.getClient()));
        }
        if (includeDemographicClause) {
            query = query + " and t.demographicNo = ?" + paramIndex++;
            paramList.add(Integer.parseInt(filter.getDemographicNo()));
        }
        if (includeMessage) {
            query = query + " and t.message = ?" + paramIndex++;
            paramList.add(filter.getMessage());
        }

        return query;
    }

    private Tickler.STATUS convertStatus(String status) {
        Tickler.STATUS result = Tickler.STATUS.A;
        if (status != null && status.startsWith("C"))
            result = Tickler.STATUS.C;
        if (status != null && status.startsWith("D"))
            result = Tickler.STATUS.D;
        return result;
    }

    private Tickler.PRIORITY convertPriority(String priority) {
        Tickler.PRIORITY result = Tickler.PRIORITY.Normal;
        if (priority != null && priority.equals("High"))
            result = Tickler.PRIORITY.High;
        if (priority != null && priority.equals("Low"))
            result = Tickler.PRIORITY.Low;
        return result;
    }
}
