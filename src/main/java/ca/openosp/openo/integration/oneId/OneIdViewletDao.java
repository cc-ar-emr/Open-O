/**
 * Copyright (c) 2021 WELL EMR Group Inc. This software is made available under the terms of the GNU
 * General Public License, Version 2, 1991 (GPLv2). License details are available via
 * "gnu.org/licenses/gpl-2.0.html".
 */
package ca.openosp.openo.integration.oneId;

import ca.openosp.openo.commn.dao.AbstractDao;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OneIdViewletDao extends AbstractDao<OneIdViewlet> {

  OneIdViewlet queryOneIdViewletForKey(final String key);

  List<OneIdViewlet> findAllActiveAndShowInEchartTrue();

  List<OneIdViewlet> findAllOrderByName();
}
