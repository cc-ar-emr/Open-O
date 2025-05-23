//CHECKSTYLE:OFF
/**
 * Copyright (c) 2008-2012 Indivica Inc.
 * <p>
 * This software is made available under the terms of the
 * GNU General Public License, Version 2, 1991 (GPLv2).
 * License details are available via "indivica.ca/gplv2"
 * and "gnu.org/licenses/gpl-2.0.html".
 */

package com.indivica.olis.parameters;

/**
 * Performing Laboratory
 *
 * @author jen
 */
public class ZBR6 implements Parameter {
    private String universalId;
    private String universalIdType;

    public ZBR6(String universalId, String universalIdType) {
        this.universalId = universalId;
        this.universalIdType = universalIdType;
    }

    @Override
    public String toOlisString() {
        return getQueryCode() + ".6.2" + (universalId != null ? universalId : "") + "~" +
                getQueryCode() + ".6.3" + (universalIdType != null ? universalIdType : "");
    }

    @Override
    public void setValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setValue(Integer part, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setValue(Integer part, Integer part2, Object value) {
        if (part == 6 && part2 == 2)
            universalId = (String) value;
        else if (part == 6 && part2 == 3)
            universalIdType = (String) value;
    }

    @Override
    public String getQueryCode() {
        return "@ZBR.6";
    }

}
