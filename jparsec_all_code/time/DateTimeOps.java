/*
 * This file is part of JPARSEC library.
 *
 * (C) Copyright 2006-2020 by T. Alonso Albi - OAN (Spain).
 *
 * Project Info:  http://conga.oan.es/~alonso/jparsec/jparsec.html
 *
 * JPARSEC library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JPARSEC library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package jparsec.time;

import java.util.Calendar;
import java.util.GregorianCalendar;

import jparsec.math.Constant;
import jparsec.util.JPARSECException;

/**
 * Contains miscellaneous time and date-related functions.
 * <P>
 * Functions related to Daylight Saving Time work using
 * the current system settings, so its depends on the computer.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @author M. Huss
 * @version 1.0
 */
public class DateTimeOps
{
	// private constructor so that this class cannot be instantiated.
	private DateTimeOps() {}

	/**
	 * Calculate the current Daylight Time offset ( 0 or -1 ). <BR>
	 * Add the result of this function to the current time to adjust.
	 *
	 * @param cal A java.util.Calendar object which is used to get
	 *        the DST_OFFSET from (e.g., java.util.GregorianCalendar).
	 * @return DST_OFFSET in hours if Daylight time is in effect, 0
	 *         otherwise.
	 */
	public static float dstOffset(Calendar cal)
	{
		return (float) (cal.get(Calendar.DST_OFFSET) / Constant.MILLISECONDS_PER_HOUR);
	}

	/**
	 * Calculate the current Daylight Time offset. <BR>
	 * Add the result of this function to the current time to adjust.<BR>
	 * This function uses a GregorianCalendar object.
	 *
	 * @return DST_OFFSET in hours if Daylight time is in effect, 0
	 *         otherwise.
	 */
	public static float dstOffset()
	{
		return dstOffset(new GregorianCalendar());
	}

	/**
	 * Calculate the current Daylight Time offset in fractional days. <BR>
	 * Add the result of this function to the current time to adjust.
	 *
	 * @param cal A java.util.Calendar object which is used to get
	 *        the DST_OFFSET.
	 * @return DST_OFFSET in days if Daylight time is in effect, 0
	 *         otherwise.
	 */
	public static double dstOffsetInDays(Calendar cal)
	{
		return (double) dstOffset(cal) * Constant.DAYS_PER_HOUR;
	}

	/**
	 * Calculate the current Daylight Time offset in fractional days. <BR>
	 * Add the result of this function to the current time to adjust.<BR>
	 * This function uses a GregorianCalendar object.
	 *
	 * @return DST_OFFSET in days if Daylight time is in effect, 0
	 *         otherwise.
	 */
	public static double dstOffsetInDays()
	{
		return (double) dstOffset() * Constant.DAYS_PER_HOUR;
	}

	/**
	 * Determine the absolute time zone offset from UTC in hours (-12 to +12)
	 * for the spec'd Calendar.
	 *
	 * @param cal The Calendar to use.
	 * @return The offset in hours.
	 */
	public static float tzOffset(Calendar cal)
	{
		return (float) (cal.get(Calendar.ZONE_OFFSET) / Constant.MILLISECONDS_PER_HOUR);
	}

	/**
	 * Determine the absolute time zone offset from UTC in hours (-12 to +12)
	 * using the local timezone.
	 *
	 * @return The offset in hours.
	 */
	public static float tzOffset()
	{
		return tzOffset(new GregorianCalendar());
	}

	/**
	 * Determine the absolute time zone offset from UTC in fractional days (-0.5
	 * to +0.5).
	 *
	 * @param cal The Calendar to use.
	 * @return The offset in decimal day.
	 */
	public static double tzOffsetInDays(Calendar cal)
	{
		return (double) tzOffset(cal) * Constant.DAYS_PER_HOUR;
	}

	/**
	 * Determine the absolute time zone offset from UTC in fractional days (-0.5
	 * to +0.5).
	 *
	 * @return The offset in decimal day.
	 */
	public static double tzOffsetInDays()
	{
		return (double) tzOffset() * Constant.DAYS_PER_HOUR;
	}

	/**
	 * Format a time as a String using the format HH:MM.
	 * <BR>
	 * The returned string will be "--:--" if the time is invalid.
	 *
	 * @param t The time to format.
	 * @return The formatted String.
	 */
	public static String formatTime(double t)
	{
		String ft = "--:--";

		if (t >= 0D)
		{
			// round up to nearest minute
			int minutes = (int) (t * Constant.HOURS_PER_DAY * Constant.MINUTES_PER_HOUR + Constant.ROUND_UP);
			ft = twoDigits(minutes / (int) Constant.MINUTES_PER_HOUR) + ":" + twoDigits(minutes % (int) Constant.MINUTES_PER_HOUR);
		}
		return ft;
	}

	/**
	 * Returns a string version of two digit number, with leading zero if needed
	 * The input is expected to be in the range 0 to 99.
	 * @param i The value.
	 * @return The string representation with two digits.
	 */
	public static String twoDigits(double i)
	{
		return (i >= 10) ? "" + i : "0" + i;
	}
	/**
	 * Returns a string version of two digit number, with leading zero if needed
	 * The input is expected to be in the range 0 to 99.
	 * @param i The value.
	 * @return The string representation with two digits.
	 */
	public static String twoDigits(int i)
	{
		return (i >= 10) ? "" + i : "0" + i;
	}
	/**
	 * Returns a string version of two digit number, with leading zero if needed
	 * The input is expected to be in the range 0 to 99.
	 * @param i The value.
	 * @return The string representation with two digits.
	 */
	public static String twoDigits(float i)
	{
		return (i >= 10) ? "" + i : "0" + i;
	}

	/**
	 * Returns true if the year is a leap year in the Gregorian Calendar.
	 *
	 * @param year Year value, as it is given by an AstroDate instance
	 * (-2 = 2 B.C. not the 'astronomical' year).
	 * @return true if it is a leap year, false otherwise.
	 */
	public static boolean isLeapYear(int year)
	{
		boolean isLeap = false;
		int aux1;
		int aux2;
		int aux3;

		if (year < 0) year ++;
		aux1 = year % 4;
		aux2 = year % 100;
		aux3 = year % 400;

		if (aux1 == 0 && (aux2 == 0 && aux3 == 0 || aux2 != 0))
		{
			isLeap = true;
		}

		return isLeap;
	}

	/**
	 * Number of days in a month for Gregorian/Julian calendars.
	 */
	private static final int daysPerMonth[] = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31, 0 };

	/**
	 * Returns the number of days in a given month in the Gregorian calendar.
	 * @param year Year value, as it is given by an AstroDate instance
	 * (-2 = 2 B.C. not the 'astronomical' year).
	 * @param month Month.
	 * @return Days in month.
	 */
	public static int getDaysInMonth(int year, int month)
	{
		int n = daysPerMonth[month - 1];
		if (month == 2 && isLeapYear(year)) n++;
		return n;
	}

	private static long dmyToDay(int year, int month, int day) throws JPARSECException {
		AstroDate astro = new AstroDate(year, month, day, 12, 0, 0);
		return (long) astro.jd();
	}

	/**
	 * Calculate the Julian day number for the date when Daylight time starts in
	 * a given year in some countries of the north hemisphere, or ends in other
	 * countries in the southern one.
	 *
	 * @param year The year of interest.
	 * @return The corresponding Julian day.
	 * @throws JPARSECException If the day is invalid.
	 */
	public static double getLastSundayOfApril(int year) throws JPARSECException
	{
		// last Sunday in April
		long jd = dmyToDay(year, 4, 31);
		while (6 != (jd % 7))
			// Sunday
			jd--;

		return (double) (jd - 0.5);
	}

	/**
	 * Calculate the Julian day number for the date when Daylight time ends in
	 * some countries of the north hemisphere, or starts in others in the
	 * southern one.
	 *
	 * @param year The year of interest.
	 * @return The corresponding Julian day.
	 * @throws JPARSECException If the day is invalid.
	 */
	public static double getLastSundayOfNovember(int year) throws JPARSECException
	{
		// last Sunday in November
		long jd = dmyToDay(year, 11, 31);
		while (6 != (jd % 7))
			// Sunday
			jd--;

		return (double) (jd - 0.5);
	}

	/**
	 * Calculate the Julian day number for the date when Daylight time starts in
	 * a given year in some countries in the north hemisphere, or ends in others
	 * in the southern one.
	 *
	 * @param year The year of interest.
	 * @return The corresponding Julian day.
	 * @throws JPARSECException If the day is invalid.
	 */
	public static double getLastSundayOfMarch(int year) throws JPARSECException
	{
		// last Sunday in March
		long jd = dmyToDay(year, 3, 31);
		while (6 != (jd % 7))
			// Sunday
			jd--;

		return (double) (jd - 0.5);
	}

	/**
	 * Calculate the Julian day number for the date when Daylight time ended in
	 * a given year (before 2007) in United States and other countries.
	 * <P>
	 * Note that as of August 2005 law, approved by the congress of the USA, now
	 * the DST ends in the first Sunday of November in that country and Canada,
	 * except in the following regions that maintain the previous rule: Arizona,
	 * Hawai, Puerto Rico, Virgin Islands, and American Samoa.
	 *
	 * @param year The year of interest.
	 * @return The corresponding Julian day.
	 * @throws JPARSECException If the day is invalid.
	 */
	public static double getLastSundayOfOctober(int year) throws JPARSECException
	{
		// last Sunday in October
		long jd = dmyToDay(year, 10, 31);
		while (6 != (jd % 7))
			// Sunday
			jd--;

		return (double) (jd - 0.5);
	}

	/**
	 * Calculate the Julian day number for the date when Daylight time ends in a
	 * given year (2007 and after) in United States.
	 *
	 * @param year The year of interest.
	 * @return The corresponding Julian day.
	 * @throws JPARSECException If the day is invalid.
	 */
	public static double getFirstSundayOfNovember(int year) throws JPARSECException
	{
		// first Sunday in November
		long jd = dmyToDay(year, 11, 1);
		while (6 != (jd % 7))
			// Sunday
			jd++;

		return (double) (jd - 0.5);
	}

	/**
	 * Calculate the Julian day number for the date when Daylight time starts in
	 * a given year (2007 and after) in United States.
	 *
	 * @param year The year of interest.
	 * @return The corresponding Julian day.
	 * @throws JPARSECException If the day is invalid.
	 */
	public static double getSecondSundayOfMarch(int year) throws JPARSECException
	{
		// first Monday in April
		long jd = dmyToDay(year, 3, 1);
		while (6 != (jd % 7))
			// Monday
			jd++;

		return (double) (jd - 0.5 + 7.0);
	}

	/**
	 * Calculate the Julian day number for the date when Daylight time started
	 * in a given year (before 2007) in United States and Canada. Note that
	 * as of August 2005 law, approved by the congress of the USA, now the DST
	 * starts in the second Sunday of March.
	 * <P>
	 * Please note that the new 'rule' is not applied in the whole USA, for
	 * example in Arizona, Hawai, Puerto Rico, Virgin Islands, and American
	 * Samoa.
	 *
	 * @param year The year of interest.
	 * @return The corresponding Julian day.
	 * @throws JPARSECException If the day is invalid.
	 */
	public static double getFirstSundayOfApril(int year) throws JPARSECException
	{
		// first Sunday in April
		long jd = dmyToDay(year, 4, 1);
		while (6 != (jd % 7))
			// Sunday
			jd++;

		return (double) (jd - 0.5);
	}
}
