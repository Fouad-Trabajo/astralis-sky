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
 * Implements the Gregorian calendar.
 * <p>
 * This solar calendar was designed by a commission assembled by Pope Gregory XII in the 16th century.
 * The main author is the astronomer Aloysius Lillius,
 * who changed the rules for century leap years from the old Julian calendar.
 * <p>
 * See Calendrical Calculations for reference.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Gregorian extends BaseCalendar
{
	private static final long serialVersionUID = -1355689199554686175L;
	
	/**
	 * Gregorian epoch.
	 */
	public static final long EPOCH = 1721424;

	/**
	 * Names of the days of the week.
	 */
	public static final String DAY_OF_WEEK_NAMES[] = { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };

	/**
	 * Names of the months.
	 */
	public static final String MONTH_NAMES[] = {
		"January", "February", "March", "April", "May", "June", "July",
		"August", "September", "October", "November", "December"
	};

	/**
	 * Create a Gregorian date from a fixed day.
	 *
	 * @param fixed fixed day.
	 */
	public Gregorian(final long fixed) {
		super(EPOCH, fixed);
	}

	/**
	 * Create a Gregorian date from a Julian day.
	 *
	 * @param jd Julian day.
	 */
	public Gregorian(final double jd) {
		super(EPOCH, jd);
	}

	/**
	 * Explicit constructor.
	 *
	 * @param year Year.
	 * @param month Month.
	 * @param day Day.
	 */
	public Gregorian(final long year, final int month, final int day) {
		super(EPOCH, year, month, day);
	}

	/**
	 * To fixed date.
	 *
	 * @param year Year.
	 * @param month Month.
	 * @param day Day.
	 * @return Fixed day.
	 */
	@Override
	long toFixed(final long year, final int month, final int day)
	{
		long y = year - 1;
		return 365 * y + (y / 4) - (y / 100) + (y / 400) + ((367 * month - 362) / 12) + (month > 2 ? isLeapYear(year) ? -1 : -2 : 0) + day;
	}

	@Override
	long yearFromFixed() {
		return yearFromFixed(this.fixed);
	}

	@Override
	int monthFromFixed(long year) {
		long daysInYear = this.fixed - toFixed(year, 1, 1);
		int i = this.fixed >= toFixed(year, 3, 1) ? ((isLeapYear(year) ? 1 : 2)) : 0;

		return (int) ((12 * (daysInYear + i) + 373) / 367);
	}

	@Override
	int dayFromFixed(long year, int month) {
		return 1 + (int) (this.fixed - toFixed(year, month, 1));
	}

	@Override
	public int getDayOfWeek() {
		return (int) Calendar.dayOfWeekFromFixed(fixed);
	}

	/**
	 * Is this a leap year?
	 *
	 * @param year Year.
	 * @return True if it is a leap year.
	 */
	public static boolean isLeapYear(final long year) {
		if ((year % 4) == 0) {
			long l1 = year % 400;

			return (l1 != 100 && l1 != 200 && l1 != 300);
		}

		return false;
	}

	/**
	 * Gets the last day of the month.
	 *
	 * @return Number of days in this month.
	 */
	public int lastDayOfMonth() {
		switch (month) {
		case 2:
			return !isLeapYear(year) ? 28 : 29;

		case 4:
		case 6:
		case 9:
		case 11:
			return 30;

		default:
			return 31;
		}
	}

	/**
	 * Gets day number in current year..
	 *
	 * @return Day number.
	 */
	public long dayNumber() {
		return Calendar.difference(toFixed(year - 1, 12, 31), this.fixed);
	}

	/**
	 * Get days remaining until end of year.
	 *
	 * @return Number of days.
	 */
	public long daysRemaining() {
		return Calendar.difference(this.fixed, toFixed(year, 12, 31));
	}

	/**
	 * Gets independence day.
	 *
	 * @param year Year.
	 * @return Such date.
	 */
	public static long independenceDay(final long year) {
		return new Gregorian(year, 7, 4).fixed;
	}

	/**
	 * Gets labor day.
	 *
	 * @param year Year.
	 * @return Such date.
	 */
	public static long laborDay(final long year) {
		return Calendar.firstKDay(1, new Gregorian(year, 9, 1).fixed);
	}

	/**
	 * Gets memorial day.
	 *
	 * @param year Year.
	 * @return Such date.
	 */
	public static long memorialDay(final long year) {
		return Calendar.lastKDay(1, new Gregorian(year, 5, 31).fixed);
	}

	/**
	 * Gets election day.
	 *
	 * @param year Year.
	 * @return Such date.
	 */
	public static long electionDay(final long year) {
		return Calendar.firstKDay(2, new Gregorian(year, 11, 2).fixed);
	}

	/**
	 * Gets Christmas day.
	 *
	 * @param year Year.
	 * @return Such date.
	 */
	public static long christmas(final long year) {
		return new Gregorian(year, 12, 25).fixed;
	}

	/**
	 * Gets advent day.
	 *
	 * @param year Year.
	 * @return Such date.
	 */
	public static long advent(final long year) {
		return Calendar.kDayNearest(new Gregorian(year, 11, 30).fixed, 0);
	}

	/**
	 * Gets epiphany day.
	 *
	 * @param year Year.
	 * @return Such date.
	 */
	public static long epiphany(final long year) {
		return Calendar.firstKDay(0, new Gregorian(year, 1, 2).fixed);
	}

	/**
	 * Gets the year from a fixed date.
	 *
	 * @param fixed Year.
	 * @return Year.
	 */
	public static long yearFromFixed(final long fixed) {
		long l1 = fixed - 1;

		long l2 = l1 / 146097;
		long l3 = l1 % 146097;

		long l4 = l3 / 36524;
		long l5 = l3 % 36524;

		long l6 = l5 / 1461;
		long l7 = l5 % 1461;

		long l8 = l7 / 365;

		long l9 = 400 * l2 + 100 * l4 + 4 * l6 + l8;

		if (l4 == 4 || l8 == 4) {
			return l9;
		}
		else {
			return l9 + 1;
		}
	}
}
