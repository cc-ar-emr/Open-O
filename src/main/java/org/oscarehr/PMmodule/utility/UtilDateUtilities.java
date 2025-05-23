//CHECKSTYLE:OFF
/**
 * Copyright (c) 2005-2012. Centre for Research on Inner City Health, St. Michael's Hospital, Toronto. All Rights Reserved.
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
 * This software was written for
 * Centre for Research on Inner City Health, St. Michael's Hospital,
 * Toronto, Ontario, Canada
 */

package org.oscarehr.PMmodule.utility;

import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @deprecated 2013-04-28 use org.oscarehr.util.DateUtils instead
 */
public class UtilDateUtilities {

    public static Date StringToDate(String s) {
        return StringToDate(s, defaultPattern, defaultLocale);
    }

    public static Date StringToDate(String s, Locale locale) {
        return StringToDate(s, defaultPattern, locale);
    }

    public static Date StringToDate(String s, String spattern) {
        return StringToDate(s, spattern, defaultLocale);
    }

    public static Date StringToDate(String s, String spattern, Locale locale) {
        try {
            SimpleDateFormat simpledateformat = new SimpleDateFormat(spattern, locale);
            return simpledateformat.parse(s);
        } catch (Exception exception) {
            return null;
        }
    }

    public static String DateToString(Date date) {
        return DateToString(date, defaultPattern, defaultLocale);
    }

    public static String DateToString(Date date, Locale locale) {
        return DateToString(date, defaultPattern, locale);
    }

    public static String DateToString(Date date, String spattern) {
        return DateToString(date, spattern, defaultLocale);
    }

    public static String DateToString(Date date, String spattern, Locale locale) {
        if (date != null) {
            SimpleDateFormat simpledateformat = new SimpleDateFormat(spattern, locale);
            return simpledateformat.format(date);
        } else {
            return "";
        }
    }

    public static Date calcDate(String s, String s1, String s2) {
        if (s == null || s1 == null || s2 == null) return (null);

        int i = Integer.parseInt(s);
        int j = Integer.parseInt(s1) - 1;
        int k = Integer.parseInt(s2);
        GregorianCalendar gregoriancalendar = new GregorianCalendar(i, j, k);
        return gregoriancalendar.getTime();
    }

    public static String calcAge(Date DOB) {
        return calcAgeAtDate(DOB, new GregorianCalendar().getTime());
    }


    /**
     * This returns the Patients Age string at a point in time.  IE. How old the patient will be right now or how old will they be on march.31 of this year.
     *
     * @param DOB         Demographics Date of birth
     * @param pointInTime The date you would like to calculate there age at.
     * @return age string ( ie 2 months, 4 years .etc )
     */
    public static String calcAgeAtDate(Date DOB, Date pointInTime) {
        if (DOB == null) return (null);

        GregorianCalendar now = new GregorianCalendar();
        now.setTime(pointInTime);
        int curYear = now.get(Calendar.YEAR);
        int curMonth = now.get(Calendar.MONTH) + 1;
        int curDay = now.get(Calendar.DAY_OF_MONTH);

        GregorianCalendar birthDate = new GregorianCalendar();
        birthDate.setTime(DOB);
        int birthYear = birthDate.get(Calendar.YEAR);
        int birthMonth = birthDate.get(Calendar.MONTH) + 1;
        int birthDay = birthDate.get(5);

        int ageInYears = curYear - birthYear;
        String result = ageInYears + " " + ResourceBundle.getBundle("oscarResources").getString("global.years");


        if (curMonth > birthMonth || curMonth == birthMonth && curDay >= birthDay) {
            ageInYears = curYear - birthYear;
            result = ageInYears + " " + ResourceBundle.getBundle("oscarResources").getString("global.years");
        } else {
            ageInYears = curYear - birthYear - 1;
            result = ageInYears + " " + ResourceBundle.getBundle("oscarResources").getString("global.years");
        }
        if (ageInYears < 2) {
            int yearDiff = curYear - birthYear;
            int ageInDays;
            if (yearDiff == 2) {
                ageInDays = (birthDate.getActualMaximum(Calendar.DAY_OF_YEAR) - birthDate.get(Calendar.DAY_OF_YEAR)) + now.get(Calendar.DAY_OF_YEAR) + 365;
            } else if (yearDiff == 1) {
                ageInDays = (birthDate.getActualMaximum(Calendar.DAY_OF_YEAR) - birthDate.get(Calendar.DAY_OF_YEAR)) + now.get(Calendar.DAY_OF_YEAR);
            } else {
                ageInDays = now.get(Calendar.DAY_OF_YEAR) - birthDate.get(Calendar.DAY_OF_YEAR);
            }
            if (ageInDays / 7 > 9) {
                result = ageInDays / 30 + " " + ResourceBundle.getBundle("oscarResources").getString("global.months");
            } else if (ageInDays >= 14) {
                result = ageInDays / 7 + " " + ResourceBundle.getBundle("oscarResources").getString("global.weeks");
            } else {
                result = ageInDays + " " + ResourceBundle.getBundle("oscarResources").getString("global.days");
            }
        }
        return result;
    }


    public static int calcAge(String year_of_birth, String month_of_birth, String date_of_birth) {
        GregorianCalendar now = new GregorianCalendar();
        int curYear = now.get(Calendar.YEAR);
        int curMonth = (now.get(Calendar.MONTH) + 1);
        int curDay = now.get(Calendar.DAY_OF_MONTH);
        int age = 0;

        if (curMonth > Integer.parseInt(month_of_birth)) {
            age = curYear - Integer.parseInt(year_of_birth);
        } else {
            if (curMonth == Integer.parseInt(month_of_birth) && curDay > Integer.parseInt(date_of_birth)) {
                age = curYear - Integer.parseInt(year_of_birth);
            } else {
                age = curYear - Integer.parseInt(year_of_birth) - 1;
            }
        }
        return age;
    }

    private static String defaultPattern = "yyyy-MM-dd";
    //    private static String dateTimePattern = "yyyy-MM-dd HH:mm:ss"; timeStampPattern = "yyyyMMddHHmmss";
    private static Locale defaultLocale = Locale.CANADA;

    public static String getToday(String datePattern) {
        Format formatter = new SimpleDateFormat(datePattern);
        return formatter.format(new Date());
    }

    /**
     * For Parsing Dates.
     *
     * @param dateStr     The date string to be parsed
     * @param datePattern The date pattern to use to parse the date string
     * @return Date object. If date was unable to be parsed the object will be null
     */
    public static Date getDateFromString(String dateStr, String datePattern) {
        Date date = null;
        try {
            // Some examples
            DateFormat formatter = new SimpleDateFormat(datePattern);
            date = formatter.parse(dateStr);
        } catch (ParseException e) {

        }
        return date;
    }


    //This if probably not the most effiecent way to calcu

    /**
     * Gets the number of months between two date objects
     *
     * @param dStart Start Date
     * @param dEnd   End Date
     * @return the number of months
     */
    public static int getNumMonths(Date dStart, Date dEnd) {
        int i = 0;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dStart);
        while (calendar.getTime().before(dEnd) || calendar.getTime().equals(dEnd)) {
            calendar.add(Calendar.MONTH, 1);
            i++;
        }
        i--;
        if (i < 0) {
            i = 0;
        }
        return i;
    }


    /**
     * Gets the number of years between two date objects
     *
     * @param dStart Start Date
     * @param dEnd   End Date
     * @return Number of year between
     */
    public static int getNumYears(Date dStart, Date dEnd) {
        GregorianCalendar now = new GregorianCalendar();
        now.setTime(dEnd);
        int curYear = now.get(Calendar.YEAR);
        int curMonth = now.get(Calendar.MONTH) + 1;
        int curDay = now.get(Calendar.DAY_OF_MONTH);

        GregorianCalendar birthDate = new GregorianCalendar();
        birthDate.setTime(dStart);
        int birthYear = birthDate.get(Calendar.YEAR);
        int birthMonth = birthDate.get(Calendar.MONTH) + 1;
        int birthDay = birthDate.get(5);

        int ageInYears = curYear - birthYear;

        if (curMonth > birthMonth || curMonth == birthMonth && curDay >= birthDay) {
            ageInYears = curYear - birthYear;
        } else {
            ageInYears = curYear - birthYear - 1;
        }
        return ageInYears;
    }


    /**
     * Gets the number of months between two Calendar objects
     *
     * @param dStart start date
     * @param dEnd   end date
     * @return number of months between
     */
    public static int getNumMonths(Calendar dStart, Calendar dEnd) {
        return getNumMonths(dStart.getTime(), dEnd.getTime());
    }


}
