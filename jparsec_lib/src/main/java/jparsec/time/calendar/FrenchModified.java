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
 * Implements the Modified French calendar.
 * <P>
 * Used between May, 6-23, 1871.
 * <P>
 * See Calendrical Calculations for reference.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class FrenchModified extends French
{
	private static final long serialVersionUID = 8486808378839034546L;

	/**
	 * Julian day constructor.
	 *
	 * @param jd Julian day.
	 */
	public FrenchModified(final double jd) {
		super(jd);
	}

	/**
	 * Fixed day constructor.
	 *
	 * @param fixed fixed day.
	 */
	public FrenchModified(final long fixed) {
		super(fixed);
	}

	/**
	 * Explicit constructor.
	 *
	 * @param y Year.
	 * @param m Month.
	 * @param d Day.
	 */
	public FrenchModified(final long y, final int m, final int d) {
		super(y, m, d);
	}

	/**
	 * To fixed day.
	 *
	 * @param year Year.
	 * @param month Month.
	 * @param day Day.
	 */
	@Override
	long toFixed(final long year, final int month, final int day) {
		long y = year - 1;

		return French.EPOCH - 1 + 365L * y + (y / 4) - (y / 100) + (y / 400) - (y / 4000) + 30 * (month - 1) + day;
	}

	@Override
	long yearFromFixed() {
		long y = 1 + (long) ((this.fixed - French.EPOCH + 2) / 365.24225D);

		return this.fixed >= toFixed(y, 1, 1) ? y : y - 1;
	}

	@Override
	int monthFromFixed(long year) {
		return 1 + (int) ((this.fixed - toFixed(year, 1, 1)) / 30);
	}

	@Override
	int dayFromFixed(long year, int month) {
		return 1 + (int) (this.fixed - toFixed(year, month, 1));
	}

	/**
	 * Gets if the current year is a leap one.
	 *
	 * @param year Fixed day.
	 * @return True or false.
	 */
	public static boolean isLeapYear(final long year) {
		if ((year % 4) == 0) {
			long l1 = year % 400;

			if (l1 != 100 && l1 != 200 && l1 != 300 && (year % 4000) != 0) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Gets the day of the week.
	 *
	 * @return Day of week.
	 */
	public int getDayOfWeek() {
		String days[] = month == 13 ? French.SPECIAL_DAY_NAMES : French.DAY_OF_WEEK_NAMES;
		int day = this.day % days.length;
		if (day > days.length) day -= days.length;
		if (day < 0) day += days.length;
		return day;
	}

	/**
	 * Gets the decadi.
	 *
	 * @return Decadi index (0, 1, 2), or -1 for no decadi.
	 */
	public int getDecadi() {
		int week = -1;
		if (month < 13) week = (day - 1) / 10 + 1;
		if (week > 2) week = week - 3;
		return week;
	}
}
