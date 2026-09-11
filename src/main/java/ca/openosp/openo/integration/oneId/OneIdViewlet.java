/**
 * Copyright (c) 2021 WELL EMR Group Inc. This software is made available under the terms of the GNU
 * General Public License, Version 2, 1991 (GPLv2). License details are available via
 * "gnu.org/licenses/gpl-2.0.html".
 */
package ca.openosp.openo.integration.oneId;

import ca.openosp.openo.commn.model.AbstractModel;
import com.google.gson.annotations.Expose;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Date;

@Entity
public class OneIdViewlet extends AbstractModel<Object> {

    @Expose @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Expose private String name;
    @Expose private String keyValue;
    @Expose private String updatedBy;
    @Expose private Date updateTime;
    @Expose private boolean showInEchart;
    @Expose private boolean deleted;
    @Expose private String displayMode;

    public OneIdViewlet() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getKeyValue() { return keyValue; }
    public void setKeyValue(String keyValue) { this.keyValue = keyValue; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    public boolean isShowInEchart() { return showInEchart; }
    public void setShowInEchart(boolean showInEchart) { this.showInEchart = showInEchart; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public String getDisplayMode() { return displayMode; }
    public void setDisplayMode(String displayMode) { this.displayMode = displayMode; }
}
