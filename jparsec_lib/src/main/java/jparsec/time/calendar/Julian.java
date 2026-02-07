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
 * Implements the Julian calendar.
 * <p>
 * This solar calendar was instituted on January, 1, 709 A.U.C. (45 B.C.E.) by
 * Julius Caesar, with the help of the astronomer Sosigenes.
 * <p>
 * See Calendrical Calculations for reference.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Julian extends BaseCalendar
{
	private static final long serialVersionUID = 4135806601319748936L;
	
	/**
	 * Calendar epoch.
	 */
	public static final long EPOCH = -1;

	/**
	 * Fixed day constructor.
	 *
	 * @param fixed fixed day.
	 */
	public Julian(final long fixed) {
		super(EPOCH, fixed);
	}

	/**
	 * Julian day constructor.
	 *
	 * @param julianDay Julian day.
	 */
	public Julian(final double julianDay) {
		super(EPOCH, julianDay);
	}

	/**
	 * Explicit constructor.
	 *
	 * @param year Year.
	 * @param month Month.
	 * @param day Day.
	 */
	public Julian(final long year, final int month, final int day) {
		super(EPOCH, year, month, day);
	}

	/**
	 * To fixed date.
	 *
	 * @param year Year.
	 * @param month Month.
	 * @param day Day.
	 * @return Fixed date.
	 */
	@Override
	long toFixed(final long year, final int month, final int day) {
		long l1 = year >= 0 ? year : year + 1;
		l1--;

		return EPOCH - 1 + 365 * l1 + l1 / 4 + (367 * month - 362) / 12 + (month > 2 ? (isLeapYear(year) ? -1 : -2) : 0) + day;
	}

	@Override
	long yearFromFixed() {
		long y = (long) Math.floor((4 * (fixed - EPOCH) + 1464.0) / 1461.0);

		return y > 0 ? y : y - 1;
	}

	@Override
	int monthFromFixed(final long year) {
		long days = this.fixed - toFixed(year, 1, 1);
		int i = this.fixed >= toFixed(year, 3, 1) ? ((isLeapYear(year) ? 1 : 2)) : 0;

		return (int) ((12 * (days + i) + 373) / 367);
	}

	@Override
	int dayFromFixed(final long year, final int month) {
		return 1 + (int) (this.fixed - toFixed(year, month, 1));
	}

	@Override
	public int getDayOfWeek() {
		return (int) Calendar.dayOfWeekFromFixed(fixed);
	}

	/**
	 * Gives true if the year is a leap one.
	 *
	 * @param year Year.
	 * @return True or false.
	 */
	public static boolean isLeapYear(final long year) {
		return (year % 4) == (year <= 0 ? 3 : 0);
	}
}
