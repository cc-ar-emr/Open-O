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
package org.oscarehr.common.dao;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.oscarehr.common.dao.utils.SchemaUtils;
import org.oscarehr.common.model.CtlBillingService;
import org.oscarehr.util.SpringUtils;

public class CtlBillingServiceDaoTest extends DaoTestFixtures {

    protected CtlBillingServiceDao dao = SpringUtils.getBean(CtlBillingServiceDao.class);

    @Before
    public void before() throws Exception {
        SchemaUtils.restoreTable("ctl_billingservice");
    }

    @Test
    public void testAllServiceTypes() {
        List<Object[]> serviceTypes = dao.getAllServiceTypes();
        assertNotNull(serviceTypes);
        assertFalse(serviceTypes.isEmpty());
    }

    @Test
    public void testFindByServiceGroupAndServiceType() {
        List<CtlBillingService> services = dao.findByServiceGroupAndServiceType("Group2", null);
        assertFalse(services.isEmpty());

        services = dao.findByServiceGroupAndServiceType("Group1", "MFP");
        assertFalse(services.isEmpty());
    }

    @Test
    public void testFindUniqueServiceTypesByCode() {
        assertNotNull(dao.findUniqueServiceTypesByCode("CODE"));
    }

    @Test
    public void testFindServiceTypes() {
        assertNotNull(dao.findServiceTypes());
    }

    @Test
    public void testFindServiceCodesByType() {
        assertNotNull(dao.findServiceCodesByType("SRV_TY"));
    }

    @Test
    public void testFindServiceTypesByStatus() {
        assertNotNull(dao.findServiceTypesByStatus("A"));
    }
}
