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
package ca.openosp.openo.utility;

import java.util.Calendar;

import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.PeriodType;

public class AgeCalculator {

    /**
     * Calculates a patient's age from their date of birth.
     *
     * <p>A patient may have no recorded date of birth, in which case there is no age to report and
     * null is returned. Without this guard {@code LocalDate.fromCalendarFields(null)} throws, and
     * {@link ca.openosp.openo.commn.model.Demographic#getBirthDay()} returns null whenever any of
     * the three birth columns is unset.</p>
     *
     * @param birthDate Calendar the date of birth, or null when none is recorded
     * @return Age the age in years, months and days, or null when birthDate is null
     */
    public static Age calculateAge(Calendar birthDate) {
        if (birthDate == null) {
            return null;
        }

        LocalDate birthdate = LocalDate.fromCalendarFields(birthDate);
        LocalDate now = new LocalDate();                    //Today's date
        Period period = new Period(birthdate, now, PeriodType.yearMonthDay());

        return new Age(period.getDays(), period.getMonths(), period.getYears());
    }
}
