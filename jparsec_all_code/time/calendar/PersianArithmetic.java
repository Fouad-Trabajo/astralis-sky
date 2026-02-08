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
 * Implements the Arithmetic Persian calendar.
 * <P>
 * This is the old Persian calendar from the 11st century, designed by a
 * committee of astronomers including Omar Khayyam.
 *
 * See Calendrical Calculations for reference.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class PersianArithmetic extends Persian
{
	private static final long serialVersionUID = -2018929499903623808L;

	/**
	 * Constructor using a fixed day.
	 *
	 * @param fixed fixed day.
	 */
	public PersianArithmetic(final long fixed)
	{
		super(fixed);
	}

	/**
	 * Constructor using a Julian day.
	 *
	 * @param julianDay Julian day.
	 */
	public PersianArithmetic(final double julianDay)
	{
		super(julianDay);
	}

	/**
	 * Constructor using year, month, day.
	 *
	 * @param year Year.
	 * @param month Month.
	 * @param day Day.
	 */
	public PersianArithmetic(final long year, final int month, final int day)
	{
		super(year, month, day);
	}

	/**
	 * Pass to fixed date.
	 *
	 * @param year Year.
	 * @param month Month.
	 * @param day Day.
	 * @return Fixed day number.
	 */
	public static long toFixedDay(final long year, final int month, final int day)
	{
		long l1 = year <= 0L ? year - 473 : year - 474;
		long l2 = (l1 % 2820) + 474;

		return (Persian.EPOCH - 1) + 0xfb75fL * (l1 / 2820) + 365 * (l2 - 1) + (682 * l2 - 110) / 2816 + (month > 7 ? 30 * (month - 1) + 6 : 31 * (month - 1)) + day;
	}

	@Override
	long toFixed(final long year, final int month, final int day) {
		return toFixedDay(year, month, day);
	}

	@Override
	long yearFromFixed() {
		return yearFromFixed(this.fixed);
	}

	@Override
	int monthFromFixed(final long year) {
		long days = 1 + this.fixed - toFixed(year, 1, 1);

		return (int) (days >= 186 ? Math.ceil((double) (days - 6) / 30D) : Math.ceil((double) days / 31D));
	}

	/**
	 * Gets the year, month, day of the instance from the fixed day.
	 *
	 * @param fixed Fixed day number.
	 */
	public void fromFixed(final long fixed)
	{
	}

	/**
	 * True if the year is a leap one, false otherwise.
	 *
	 * @param year Year.
	 * @return True or false.
	 */
	public static boolean isLeapYear(final long year)
	{
		long l1 = year <= 0L ? year - 473L : year - 474L;
		long l2 = Calendar.mod(l1, 2820L) + 474L;
		return Calendar.mod((l2 + 38L) * 682L, 2816L) < 682L;
	}

	/**
	 * Gets the year from a fixed date.
	 *
	 * @param fixed Fixed day number.
	 * @return Current year.
	 */
	public static long yearFromFixed(final long fixed)
	{
		long l1 = fixed - toFixedDay(475, 1, 1);
		long l2 = l1 / 1029983L;
		long l3 = Calendar.mod(l1, 1029983L);
		long l4 = l3 != 1029982L ? (2816 * l3 + 1031337L) / 1028522L : 2820;
		long l5 = 474 + 2820 * l2 + l4;

		if (l5 > 0)
			return l5;
		else
			return l5 - 1;
	}
}
