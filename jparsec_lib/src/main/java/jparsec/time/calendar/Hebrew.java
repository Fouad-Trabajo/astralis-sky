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
package jparsec.time.calendar;

/**
 * Implements the Hebrew calendar.
 * <p>
 * This lunar calendar was promulgated by the Patriarch, Hillel II, in the mid-fourth century.
 * It is attributed to Mosaic revelation by Maimonides.
 * Days begin at sunset.
 * <p>
 * See Calendrical Calculations for reference.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Hebrew extends BaseCalendar
{
	private static final long serialVersionUID = 7665359541472029309L;

	/**
	 * Calendar epoch.
	 */
	public static final long EPOCH = new Julian(-3761, 10, 7).fixed;

	/**
	 * Day of week names.
	 */
	public static final String DAY_OF_WEEK_NAMES[] = {
		"yom rishon", "yom sheni", "yom shelishi", "yom revi`i", "yom hamishi", "yom shishi", "yom shabbat"
	};

	/**
	 * Month names.
	 */
	public static final String MONTH_NAMES[] = {
		"Nisan", "Iyyar", "Sivan", "Tammuz", "Av", "Elul", "Tishri", "Marheshvan", "Kislev", "Tevet", "Shevat", "Adar"
	};

	public static final String LEAPYEAR_MONTH_NAMES[] = {
		"Nisan", "Iyyar", "Sivan", "Tammuz", "Av", "Elul", "Tishri", "Marheshvan", "Kislev", "Tevet", "Shevat", "Adar I", "Adar II"
	};

	/**
	 * Constructs a Hebrew date with a fixed day
	 *
	 * @param fixed fixed day
	 */
	public Hebrew(final long fixed) {
		super(EPOCH, fixed);
	}

	/**
	 * Constructs a Hebrew date with a Julian day
	 *
	 * @param jd Julian day
	 */
	public Hebrew(final double jd) {
		super(EPOCH, jd);
	}

	/**
	 * Explicit constructor.
	 *
	 * @param year Year.
	 * @param month Month.
	 * @param day Day.
	 */
	public Hebrew(final long year, final int month, final int day) {
		super(EPOCH, year, month, day);
	}

	/**
	 * To fixed day..
	 *
	 * @param year Year.
	 * @param month Month.
	 * @param day Day.
	 * @return Fixed day.
	 */
	public static long toFixedDay(final long year, final int month, final int day) {
		long l1 = newYear(year) + day - 1;

		if (month < 7) {
			for (int k = 7; k <= lastMonthOfYear(year); k++)
				l1 += lastDayOfMonth(k, year);

			for (int j1 = 1; j1 < month; j1++)
				l1 += lastDayOfMonth(j1, year);
		}
		else {
			for (int i1 = 7; i1 < month; i1++)
				l1 += lastDayOfMonth(i1, year);
		}

		return l1;
	}

	/**
	 * To fixed day..
	 *
	 * @param year Year.
	 * @param month Month.
	 * @param day Day.
	 * @return Fixed day.
	 */
	@Override
	long toFixed(final long year, final int month, final int day) {
		return toFixedDay(year, month, day);
	}

	@Override
	long yearFromFixed() {
		long y = (long) ((this.fixed - EPOCH) / 365.24682220597794D);

		while (newYear(y) <= this.fixed) {
			y++;
		}

		return --y;
	}

	@Override
	int monthFromFixed(final long year) {
		int m = this.fixed >= toFixed(year, 1, 1) ? 1 : 7;

		while (this.fixed > toFixed(year, m, lastDayOfMonth(m, year))) {
			m++;
		}

		return m;
	}

	@Override
	int dayFromFixed(long year, int month) {
		return 1 + (int) (this.fixed - toFixed(year, month, 1));
	}

	/**
	 * Is this a leap year?
	 *
	 * @param year Year.
	 * @return True if it is a leap year.
	 */
	public static boolean isLeapYear(final long year)
	{
		return Calendar.mod(1L + 7L * year, 19L) < 7L;
	}

	/**
	 * Gets the last month in this year.
	 *
	 * @param year Year.
	 * @return Number of months.
	 */
	public static int lastMonthOfYear(final long year)
	{
		return !isLeapYear(year) ? 12 : 13;
	}

	/**
	 * Gets the last day of month.
	 *
	 * @param month Month.
	 * @param year Year.
	 * @return Number of days in this month.
	 */
	public static int lastDayOfMonth(final int month, final long year)
	{
		return month != 2 && month != 4 && month != 6 && month != 10 && month != 13 && (month != 12 || isLeapYear(year)) && (month != 8 || hasLongMarheshvan(year)) && (month != 9 || !hasShortKislev(year))
				? 30 : 29;
	}

	/**
	 * Get elapsed days from fixed epoch.
	 *
	 * @param year Year.
	 * @return Elapsed days.
	 */
	public static long calendarElapsedDays(final long year)
	{
		long l1 = Calendar.quotient(235L * year - 234L, 19D);
		double d = 12084D + 13753D * (double) l1;
		long l2 = 29L * l1 + Calendar.quotient(d, 25920D);
		if (Calendar.mod(3L * (l2 + 1L), 7L) < 3L)
			return l2 + 1L;
		else
			return l2;
	}

	/**
	 * Gets the new year.
	 *
	 * @param year Year.
	 * @return Next new year fixed day.
	 */
	public static long newYear(final long year)
	{
		return EPOCH + calendarElapsedDays(year) + (long) newYearDelay(year);
	}

	/**
	 * Gets the delay until next new year.
	 *
	 * @param year Year.
	 * @return Delay.
	 */
	public static int newYearDelay(final long year)
	{
		long l1 = calendarElapsedDays(year - 1L);
		long l2 = calendarElapsedDays(year);
		long l3 = calendarElapsedDays(year + 1L);
		if (l3 - l2 == 356L)
			return 2;
		return l2 - l1 != 382L ? 0 : 1;
	}

	/**
	 * Gets number of days in this year.
	 *
	 * @param year Year.
	 * @return Number of days.
	 */
	public static int daysInYear(final long year)
	{
		return (int) (newYear(year + 1) - newYear(year));
	}

	/**
	 * Returns true if this year has long Marheshvan.
	 *
	 * @param year Year.
	 * @return True or false.
	 */
	public static boolean hasLongMarheshvan(final long year)
	{
		int i = daysInYear(year);
		return i == 355 || i == 385;
	}

	/**
	 * Returns true if this year has short Kislev.
	 *
	 * @param year Year.
	 * @return True or false.
	 */
	public static boolean hasShortKislev(final long year)
	{
		int i = daysInYear(year);
		return i == 353 || i == 383;
	}

	/**
	 * Returns Yom Kippur feast.
	 *
	 * @param year Year.
	 * @return Such date.
	 */
	public static long yomKippur(final long year)
	{
		long y = (1 + year) - Gregorian.yearFromFixed(EPOCH);
		return toFixedDay(y, 7, 10);
	}

	/**
	 * Returns Passover feast.
	 *
	 * @param year Year.
	 * @return Such date.
	 */
	public static long passover(final long year)
	{
		long y = year - Gregorian.yearFromFixed(EPOCH);
		return toFixedDay(y, 1, 15);
	}
}
