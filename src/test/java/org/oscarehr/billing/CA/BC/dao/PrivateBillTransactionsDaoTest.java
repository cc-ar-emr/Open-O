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
package org.oscarehr.billing.CA.BC.dao;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.oscarehr.billing.CA.BC.model.BillingPrivateTransactions;
import org.oscarehr.common.dao.DaoTestFixtures;
import org.oscarehr.common.dao.utils.SchemaUtils;
import org.oscarehr.util.SpringUtils;

import oscar.entities.PrivateBillTransaction;
import oscar.oscarBilling.ca.bc.data.PrivateBillTransactionsDAO;

public class PrivateBillTransactionsDaoTest extends DaoTestFixtures {
    ;

    public PrivateBillTransactionsDAO dao = SpringUtils.getBean(PrivateBillTransactionsDAO.class);

    @Before
    public void before() throws Exception {
        SchemaUtils.restoreTable("billing_private_transactions", "billing_payment_type");
    }

    @Test
    public void testAllAtOnce() {
        BillingPrivateTransactions tx = dao.savePrivateBillTransaction(99999, 100.00, 5);
        assertTrue(tx.isPersistent());

        List<PrivateBillTransaction> txCheck = dao.getPrivateBillTransactions(String.valueOf(tx.getBillingmasterNo()));
        boolean match = false;
        for (PrivateBillTransaction pbt : txCheck) {
            if (pbt.getId() == tx.getId()) {
                match = true;
                break;
            }
        }
        assertTrue(match);
    }
}
