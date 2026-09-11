/**
 * Copyright (c) 2001-2002. Department of Family Medicine, McMaster University. All Rights Reserved.
 * This software is published under the GPL GNU General Public License. This program is free
 * software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 * <p>
 * This software was written for the Department of Family Medicine McMaster University Hamilton
 * Ontario, Canada
 */
package ca.openosp.openo.commn.dao;

import ca.openosp.openo.commn.model.OMDGatewayTransactionLog;
import org.springframework.stereotype.Repository;

import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Repository
public class OMDGatewayTransactionLogDao extends AbstractDaoImpl<OMDGatewayTransactionLog> {

  public OMDGatewayTransactionLogDao() {
    super(OMDGatewayTransactionLog.class);
  }

  @SuppressWarnings("unchecked")
  public List<OMDGatewayTransactionLog> findByOscarSessionId(String id) {
    Query query = entityManager.createQuery(
        "select x from OMDGatewayTransactionLog x where x.oscarSessionId=?");
    query.setParameter(1, id);
    return (List<OMDGatewayTransactionLog>) query.getResultList();
  }

  @SuppressWarnings("unchecked")
  public List<OMDGatewayTransactionLog> findByUniqueSessionId(String id) {
    Query query = entityManager.createQuery(
        "select x from OMDGatewayTransactionLog x where x.uniqueSessionId=?");
    query.setParameter(1, id);
    return (List<OMDGatewayTransactionLog>) query.getResultList();
  }

  /**
   * Finds interactions matching whichever of the two filters were given, newest first.
   *
   * <p>Both are applied together. Filtering on one and dropping the other would list rows that
   * contradict what the screen says it is showing.</p>
   *
   * @param providerNo     String the provider who started the interaction, or null for any
   * @param externalSystem String the EHR service the interaction was with, or null for any
   * @param maxRows        int the most rows to return, or 0 for all of them
   * @return List&lt;OMDGatewayTransactionLog&gt; the matching rows
   */
  @SuppressWarnings("unchecked")
  public List<OMDGatewayTransactionLog> find(String providerNo, String externalSystem, int maxRows) {
    List<String> conditions = new ArrayList<>();
    if (providerNo != null) {
      conditions.add("x.initiatingProviderNo=:providerNo");
    }
    if (externalSystem != null) {
      conditions.add("x.externalSystem=:externalSystem");
    }
    String where = conditions.isEmpty() ? "" : " where " + String.join(" and ", conditions);

    Query query = entityManager.createQuery(
        "select x from OMDGatewayTransactionLog x" + where + " ORDER BY x.started desc");
    if (providerNo != null) {
      query.setParameter("providerNo", providerNo);
    }
    if (externalSystem != null) {
      query.setParameter("externalSystem", externalSystem);
    }
    return (List<OMDGatewayTransactionLog>) bounded(query, maxRows).getResultList();
  }

  /** Applies a row cap to an ordered query, so the database returns only what is displayed. */
  private static Query bounded(Query query, int maxRows) {
    if (maxRows > 0) {
      query.setMaxResults(maxRows);
    }
    return query;
  }

  /**
   * Counts the rows carrying a correlation id, for one provider, patient and external system, whose
   * transaction type is in the given set.
   *
   * <p>This is how a reply about a Viewlet is tied back to the launch that asked for it. The
   * correlation id is minted server-side and handed to the browser, so a reply quoting one that
   * matches no launch row of this provider's, for this patient, on this service, did not come from
   * a launch the EMR made.
   *
   * @param correlationId String the id the launch was recorded under
   * @param providerNo String the provider the launch belonged to
   * @param demographicNo Integer the patient the launch named
   * @param externalSystem String the EHR service the launch was for
   * @param transactionTypes Collection&lt;String&gt; the transaction types to count; must not be empty
   * @return long how many rows match
   */
  public long countByCorrelation(String correlationId, String providerNo, Integer demographicNo,
      String externalSystem, Collection<String> transactionTypes) {
    Query query = entityManager.createQuery(
        "select count(x) from OMDGatewayTransactionLog x where x.xCorrelationId = ?1"
            + " and x.initiatingProviderNo = ?2 and x.demographicNo = ?3"
            + " and x.externalSystem = ?4 and x.transactionType in (?5)");
    query.setParameter(1, correlationId);
    query.setParameter(2, providerNo);
    query.setParameter(3, demographicNo);
    query.setParameter(4, externalSystem);
    query.setParameter(5, transactionTypes);
    return ((Number) query.getSingleResult()).longValue();
  }

  /**
   * Finds the log records for one external system whose transaction type is in the given set,
   * within an inclusive date range, most recent first.
   *
   * <p>The transaction-type whitelist is supplied by the caller rather than fixed here, because the
   * meaning of a transaction type belongs to the integration that writes it. The DHDR consent
   * unblock report, for example, passes {@code "PCOI"} with every type that records a decision -
   * both its own override values and the viewlet result types - which excludes the
   * {@code "consentViewletLaunch"} row that the same external system also writes.
   *
   * @param externalSystem String the {@code externalSystem} discriminator to match exactly
   * @param transactionTypes Collection&lt;String&gt; the transaction types to include; must not be
   *     empty
   * @param from Date the inclusive lower bound on the event timestamp ({@code started})
   * @param to Date the inclusive upper bound on the event timestamp ({@code started})
   * @return List&lt;OMDGatewayTransactionLog&gt; the matching records, newest first
   */
  @SuppressWarnings("unchecked")
  public List<OMDGatewayTransactionLog> findByExternalSystemAndTransactionTypes(
      String externalSystem, Collection<String> transactionTypes, Date from, Date to) {
    Query query = entityManager.createQuery(
        "select x from OMDGatewayTransactionLog x where x.externalSystem = ?1 and x.transactionType in (?2) and x.started between ?3 and ?4 order by x.started desc");
    query.setParameter(1, externalSystem);
    query.setParameter(2, transactionTypes);
    query.setParameter(3, from);
    query.setParameter(4, to);
    return (List<OMDGatewayTransactionLog>) query.getResultList();
  }
}
