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
 * Implements the ISO calendar. See Calendrical Calculations for reference.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class ISO extends BaseCalendar
{
	private static final long serialVersionUID = -1051296989077946358L;

	/**
	 * Week.
	 */
	protected int week;

	/**
	 * Calendar epoch.
	 */
	public static final long EPOCH = new Gregorian(4714, 2, 7).fixed;

	/**
	 * fixed day constructor.
	 *
	 * @param fixed Julian day.
	 */
	public ISO(final long fixed)
	{
		super(EPOCH, fixed);
	}

	/**
	 * Julian day constructor.
	 *
	 * @param julianDay Julian day.
	 */
	public ISO(final double julianDay)
	{
		super(EPOCH, julianDay);
	}

	/**
	 * Explicit constructor.
	 *
	 * @param year Year.
	 * @param week Week.
	 * @param day Day.
	 */
	public ISO(final long year, int week, int day)
	{
		super(EPOCH, toFixedDay(year, week, day));
	}

	/**
	 * To fixed date.
	 *
	 * @param year Year.
	 * @param week Week.
	 * @param day Day.
	 * @return Fixed day.
	 */
	public static long toFixedDay(long year, int week, int day)
	{
		return Calendar.nthKDay(week, 0, new Gregorian(year - 1, 12, 28).fixed) + day;
	}

	@Override
	long toFixed(final long year, final int month, final int day) {
		return toFixedDay(year, month, day);
	}

	@Override
	long yearFromFixed() {
		long y = Gregorian.yearFromFixed(this.fixed - 3);
		y = this.fixed < toFixed(y + 1, 1, 1) ? y : y + 1;
		this.week = 1 + (int) ((this.fixed - toFixed(y, 1, 1)) / 7);

		return y;
	}

	@Override
	int monthFromFixed(final long year) {
		return 0;
	}

	@Override
	int dayFromFixed(long year, int month) {
		return (int) Calendar.adjustedMod(this.fixed, 7);
	}

	@Override
	public String toString() {
		return "ISO " + this.year + "/W" + this.week + '/' + this.day;
	}

	@Override
	public int getDayOfWeek() {
		return day;
	}

	public int getWeek() {
		return week;
	}
}
