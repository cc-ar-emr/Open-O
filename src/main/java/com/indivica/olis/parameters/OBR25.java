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
 * Test Request Status
 *
 * @author jen
 */
public class OBR25 implements Parameter {

    private String value;

    public OBR25(String value) {
        this.value = value;
    }

    @Override
    public String toOlisString() {
        return getQueryCode() + "^" + (value != null ? value : "");
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof String)
            this.value = (String) value;
    }

    @Override
    public void setValue(Integer part, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setValue(Integer part, Integer part2, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getQueryCode() {
        return "@OBR.25";
    }

}
