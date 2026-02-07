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

import java.io.Serializable;

/**
 * Implements the Mayan Haab calendar. See Calendrical Calculations for
 * reference.
 * <P>
 * Note that it is not possible to pass from a given date to a Julian day, since
 * no year exists in this calendar.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class MayanHaab implements Serializable
{
	private static final long serialVersionUID = -2455524153259697510L;
	
	/**
	 * Calendar epoch.
	 */
	public static final long EPOCH = -1137490; // MayanLongCount.EPOCH - (long) ordinal(new MayanHaab(18, 8));

	/**
	 * Month names.
	 */
	public static final String MONTH_NAMES[] = {
		"Pop", "Uo", "Zip", "Zotz", "Tzec", "Xul", "Yaxkin", "Mol", "Chen", "Yax", "Zac", "Ceh", "Mac", "Kankin", "Muan",
		"Pax", "Kayab", "Cumku", "Uayeb"
	};

	/**
	 * Month number. 1 is Pop, index 0 in the months array.
	 */
	public int month;

	/**
	 * Day number.
	 */
	public int day;

	/**
	 * Default constructor.
	 */
	public MayanHaab() {}

	/**
	 * Fixed day constructor.
	 *
	 * @param fixed Fixed day.
	 */
	public MayanHaab(final long fixed) {
		fromFixed(fixed);
	}

	/**
	 * Julian day constructor.
	 *
	 * @param julianDay Julian day.
	 */
	public MayanHaab(final double julianDay) {
		fromJulianDay(julianDay);
	}

	/**
	 * Explicit constructor.
	 *
	 * @param m Month.
	 * @param d Day.
	 */
	public MayanHaab(int m, int d) {
		month = m;
		day = d;
	}

	/**
	 * Sets the date from fixed day.
	 *
	 * @param fixed Fixed day.
	 */
	public void fromFixed(final long fixed) {
		long l1 = Calendar.mod(fixed - EPOCH, 365);
		day = (int) (l1 % 20);
		month = 1 + (int) (l1 / 20);
	}

	/**
	 * Sets a Mayan date with a given Julian day
	 *
	 * @param julianDay Julian day.
	 */
	public void fromJulianDay(final double julianDay) {
		fromFixed((long) Math.floor(julianDay - 0.5) - Gregorian.EPOCH);
	}

	/**
	 * Gets elapsed days since last new year.
	 *
	 * @param mayanhaab MayanHaab instance.
	 * @return Ordinal.
	 */
//	private static int ordinal(final MayanHaab mayanhaab) {
//		return (mayanhaab.month - 1) * 20 + mayanhaab.day;
//	}

	/**
	 * Gets last new year.
	 *
	 * @param mayanhaab MayanHaab instance.
	 * @param fixed Fixed day.
	 * @return Such date.
	 */
//	private static long onOrBefore(final MayanHaab mayanhaab, final long fixed) {
//		return fixed - (fixed - EPOCH - ordinal(mayanhaab)) % 365;
//	}
}
