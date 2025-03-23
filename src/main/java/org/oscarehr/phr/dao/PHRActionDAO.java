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


package org.oscarehr.phr.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.oscarehr.phr.model.PHRAction;
import org.oscarehr.util.MiscUtils;
import org.springframework.orm.hibernate5.HibernateCallback;
import org.springframework.orm.hibernate5.HibernateTemplate;
import org.springframework.orm.hibernate5.support.HibernateDaoSupport;

public class PHRActionDAO extends HibernateDaoSupport {
    private static Logger logger = MiscUtils.getLogger();

    public List<PHRAction> getQueuedActions(String providerNo) {
        String sql = "from PHRAction a where (a.senderOscar = ?0 OR (a.receiverOscar = ?1 AND phr_classification = ?2)) and a.status = ?3";
        Object[] params = new Object[]{providerNo, providerNo, "RELATIONSHIP", PHRAction.STATUS_SEND_PENDING};
        List<PHRAction> list = (List<PHRAction>) getHibernateTemplate().find(sql, params);
        return list;
    }

    public List<PHRAction> getActionByPhrIndex(String phrIndex) {
        String sql = "from PHRAction a where a.phrIndex=?0";
        List<PHRAction> list = (List<PHRAction>) getHibernateTemplate().find(sql, new String(phrIndex));
        if (list == null || list.isEmpty()) {
            return null;
        } else return list;
    }

    public Boolean isActionPresentByPhrIndex(String phrIndex) {
        List<PHRAction> l = getActionByPhrIndex(phrIndex);
        if (l == null) return false;
        else return true;

    }

    public PHRAction getActionById(String id) {
        String sql = "from PHRAction a where a.id = ?0 ";

        List<PHRAction> list = (List<PHRAction>) getHibernateTemplate().find(sql, new Integer(id));

        if (list == null || list.size() == 0) {
            return null;
        }

        return list.get(0);
    }

    // actionType = -1 for all actions
    public List<PHRAction> getPendingActionsByProvider(String classification, int actionType, String providerNo) {
        String sql = "FROM PHRAction a WHERE a.phrClassification = ?0 AND a.senderOscar = ?1 AND a.status != ?2 AND a.status != ?3";
        if (actionType != -1) {
            sql = sql + " AND a.actionType = ?4";
        }
        Object[] params = (actionType != -1) ?
        new Object[]{classification, providerNo, PHRAction.STATUS_SENT, PHRAction.STATUS_NOT_SENT_DELETED, actionType} :
        new Object[]{classification, providerNo, PHRAction.STATUS_SENT, PHRAction.STATUS_NOT_SENT_DELETED};
        List<PHRAction> list = (List<PHRAction>) getHibernateTemplate().find(sql, params);

        if (list == null) {
            return new ArrayList<PHRAction>();
        }
        logger.debug("Found pending actions: " + list.size());
        return list;
    }

    public List<PHRAction> getPendingActionsByProvider(int actionType, String providerNo) {
        String sql = "FROM PHRAction a WHERE a.senderOscar = ?0 AND a.status != ?1 AND a.status != ?2";
        if (actionType != -1) {
            sql = sql + " AND a.actionType = ?3";
        }
        Object[] params = (actionType != -1) ?
        new Object[]{providerNo, PHRAction.STATUS_SENT, PHRAction.STATUS_NOT_SENT_DELETED, actionType} :
        new Object[]{providerNo, PHRAction.STATUS_SENT, PHRAction.STATUS_NOT_SENT_DELETED};
        List<PHRAction> list = (List<PHRAction>) getHibernateTemplate().find(sql, params);

        if (list == null) {
            return new ArrayList<PHRAction>();
        }
        logger.debug("Found pending actions: " + list.size());
        return list;

    }

    public List<PHRAction> getActionsByStatus(int status, String providerNo) {
        String sql = "FROM PHRAction a WHERE a.receiverOscar = ?0 AND a.status = ?1";
        Object[] params = new Object[]{providerNo, status};
        List<PHRAction> list = (List<PHRAction>) getHibernateTemplate().find(sql, params);

        if (list == null) {
            return new ArrayList<PHRAction>();
        }
        return list;
    }

    public List<PHRAction> getActionsByStatus(int status, String providerNo, String classification) {
        String sql = "FROM PHRAction a WHERE a.receiverOscar = ?0 AND a.phrClassification = ?1 AND a.status = ?2";
        Object[] params = new Object[]{providerNo, classification, status};
        List<PHRAction> list = (List<PHRAction>) getHibernateTemplate().find(sql, params);

        if (list == null) {
            return new ArrayList<PHRAction>();
        }
        return list;
    }

    public List<PHRAction> getActionsByStatus(List<Integer> statuses, String providerNo, String classification) {
        String sql = "FROM PHRAction a WHERE a.receiverOscar = ?0 AND a.phrClassification = ?1";
        if (statuses != null && !statuses.isEmpty()) {
            sql += " AND (";
            for (int i = 0; i < statuses.size(); i++) {
                sql += "a.status = " + statuses.get(i);
                if (i != statuses.size() - 1) sql += " OR ";
            }
            sql += ")";
        }
        sql += "ORDER BY a.status";

        Object[] params = new Object[]{providerNo, classification};
        List<PHRAction> list = (List<PHRAction>) getHibernateTemplate().find(sql, params);

        if (list == null) {
            return new ArrayList<PHRAction>();

        }
        return list;
    }


    public void save(PHRAction action) {
        this.getHibernateTemplate().save(action);
    }

    public Integer saveAndGetId(PHRAction action) {
        this.save(action);
        this.getHibernateTemplate().refresh(action);
        return action.getId();
    }

    public void update(PHRAction action) {
        this.getHibernateTemplate().update(action);
    }

    public void updatePhrIndexes(String classification, String oscarId, String providerNo, String newPhrIndex) {
        HibernateTemplate ht = getHibernateTemplate();
        String sql = "FROM PHRAction a WHERE a.phrClassification = ?0 AND a.oscarId = ?1 AND a.senderOscar = ?2 AND a.status = ?3";
        Object[] params = new Object[]{classification, oscarId, providerNo, PHRAction.STATUS_SEND_PENDING};
        List<PHRAction> list = (List<PHRAction>) ht.find(sql, params);
        for (PHRAction action : list) {
            action.setPhrIndex(newPhrIndex);
            ht.update(action);
        }
    }

    // get indivo idx

    // checks to see whether this document has been sent to indivo before (for update/add decision)
    public boolean isIndivoRegistered(final String classification, final String oscarId) {
        Long num = (Long) getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
                Query q = session.createQuery("select count(*) from PHRAction a where a.phrClassification = ?1 and a.oscarId = ?2");
                q.setParameter(1, classification);
                q.setParameter(2, oscarId);
                q.setCacheable(true);
                return q.uniqueResult();
            }
        });
        if (num > 0) return true;
        return false;

    }

    public String getPhrIndex(final String classification, final String oscarId) {
        List<PHRAction> list = (List<PHRAction>) getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
                Criteria criteria = session.createCriteria(PHRAction.class);
                criteria.add(Restrictions.eq("phrClassification", classification));
                criteria.add(Restrictions.eq("oscarId", oscarId));
                criteria.add(Restrictions.eq("status", PHRAction.STATUS_SENT));
                criteria.setMaxResults(1);
                return criteria.list();
            }
        });
        if (list.size() > 0) return list.get(0).getPhrIndex();
        return null;

        /*
         * Criteria criteria = this.getSession().createCriteria(PHRAction.class); criteria.add(Restrictions.eq("phrClassification", classification)); criteria.add(Restrictions.eq("oscarId", oscarId)); criteria.add(Restrictions.eq("sent",
         * PHRAction.STATUS_SENT)); criteria.setMaxResults(1); List<PHRAction> list = criteria.list(); if (list.size() > 0) return list.get(0).getPhrIndex(); return null;
         */
    }

    /*
     * public String getPhrIndex(String classification, String oscarId) { String sql = "FROM PHRAction a WHERE a.phrClassification = ? AND a.oscarId = ? AND a.sent = 1 LIMIT 1"; String[] f = new String[2]; f[0] = classification; f[1] = oscarId;
     * List<PHRAction> list = this.getHibernateTemplate().find(sql, f); if (list.size() > 0) return list.get(0).getPhrIndex(); return null; }
     */
}
