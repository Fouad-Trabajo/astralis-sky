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
 * Implements the Roman calendar. See Calendrical Calculations for reference.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Roman implements Serializable
{
	private static final long serialVersionUID = -2139264862771877008L;
	
	/**
	 * Calendar epoch.
	 */
	public static final long EPOCH = 0;

	/**
	 * Kalends ID.
	 */
	public static final int KALENDS = 1;

	/**
	 * Nones ID.
	 */
	public static final int NONES = 2;

	/**
	 * Ides ID.
	 */
	public static final int IDES = 3;

	/**
	 * Count names.
	 */
	public static final String COUNT_NAMES[] = {
		"", "pridie ", "ante diem iii ", "ante diem iv ", "ante diem v ", "ante diem vi ", "ante diem vii ",
		"ante diem viii ", "ante diem ix ", "ante diem x ", "ante diem xi ", "ante diem xii ", "ante diem xiii ",
		"ante diem xiv ", "ante diem xv ", "ante diem xvi ", "ante diem xvii ", "ante diem xviii ", "ante diem xix "
	};

	/**
	 * Event names.
	 */
	public static final String EVENT_NAMES[] = { "Kalends", "Nones", "Ides" };

	/**
	 * Year.
	 */
	public long year;

	/**
	 * Month.
	 */
	public int month;

	/**
	 * Event.
	 */
	public int event;

	/**
	 * Count.
	 */
	public int count;

	/**
	 * Leap day.
	 */
	public boolean leapDay;

	/**
	 * Default constructor.
	 */
	public Roman() { }

	/**
	 * Julian day constructor.
	 *
	 * @param julianDay Julian day.
	 */
	public Roman(final double julianDay) {
		fromJulianDay(julianDay);
	}

	/**
	 * Explicit constructor.
	 *
	 * @param ryear Year.
	 * @param rmonth Month.
	 * @param revent Event.
	 * @param rcount Count.
	 * @param rleapDay Leap day.
	 */
	public Roman(final long ryear, final int rmonth, final int revent, final int rcount, final boolean rleapDay) {
		year = ryear;
		month = rmonth;
		event = revent;
		count = rcount;
		leapDay = rleapDay;
	}

	/**
	 * To fixed day.
	 *
	 * @param year Year.
	 * @param month Month.
	 * @param event Event.
	 * @param count Count.
	 * @param leapDay Leap day.
	 * @return Fixed date.
	 */
	public static long toFixed(final long year, final int month, final int event, final int count, final boolean leapDay) {
		long l1 = 0;

		if (event == 1)
			l1 = new Julian(year, month, 1).fixed;
		else if (event == 2)
			l1 = new Julian(year, month, nonesOfMonth(month)).fixed;
		else if (event == 3)
			l1 = new Julian(year, month, idesOfMonth(month)).fixed;

		return l1 - count + (!Julian.isLeapYear(year) || month != 3 || event != 1 || count > 16 || count < 6
				? 1 : 0) + (leapDay ? 1 : 0);
	}

	/**
	 * To fixed day.
	 *
	 * @return Fixed date.
	 */
	public long toFixed() {
		return toFixed(year, month, event, count, leapDay);
	}

	/**
	 * Transforms a Roman date into a Julian day.
	 *
	 * @param year Year.
	 * @param month Month.
	 * @param event Event.
	 * @param count Count.
	 * @param leap Leap day.
	 * @return Julian day.
	 */
	public static double toJulianDay(final int year, final int month, final int event, final int count, final boolean leap) {
		return toFixed(year, month, event, count, leap) + Gregorian.EPOCH;
	}

	/**
	 * Transforms a Roman date into a Julian day.
	 *
	 * @return Julian day.
	 */
	public double toJulianDay() {
		return toFixed() + Gregorian.EPOCH + 0.5;
	}

	/**
	 * Sets a Roman date with a given Julian day.
	 *
	 * @param julianDay Julian day.
	 */
	public void fromJulianDay(final double julianDay) {
		fromFixed((long) Math.floor(julianDay - 0.5) - Gregorian.EPOCH);
	}

	/**
	 * Sets the date from fixed date.
	 *
	 * @param fixed Fixed day.
	 */
	public void fromFixed(final long fixed) {
		Julian julian = new Julian(fixed);
		int i = julian.month;
		int j = julian.day;
		long l1 = julian.year;
		int k = Calendar.adjustedMod(i + 1, 12);
		long l2 = k != 1 ? l1 : l1 + 1L;
		long l3 = toFixed(l2, k, 1, 1, false);

		if (j == 1) {
			year = l1;
			month = i;
			event = KALENDS;
			count = 1;
			leapDay = false;
		}
		else if (j <= nonesOfMonth(i)) {
			year = l1;
			month = i;
			event = NONES;
			count = (nonesOfMonth(i) - j) + 1;
			leapDay = false;
		}
		else if (j <= idesOfMonth(i)) {
			year = l1;
			month = i;
			event = IDES;
			count = (idesOfMonth(i) - j) + 1;
			leapDay = false;
		}
		else if (i != 2 || !Julian.isLeapYear(l1)) {
			year = l2;
			month = k;
			event = KALENDS;
			count = (int) ((l3 - fixed) + 1L);
			leapDay = false;
		}
		else if (j < 25) {
			year = l1;
			month = 3;
			event = KALENDS;
			count = 30 - j;
			leapDay = false;
		}
		else {
			year = l1;
			month = 3;
			event = KALENDS;
			count = 31 - j;
			leapDay = j == 25;
		}
	}

	/**
	 * Return ides.
	 *
	 * @param month Month.
	 * @return Ides, 13 if month = 3,5,7, or 10, and 15 otherwise.
	 */
	private static int idesOfMonth(final int month) {
		return month != 3 && month != 5 && month != 7 && month != 10 ? 13 : 15;
	}

	/**
	 * Return nones of month.
	 *
	 * @param month Month.
	 * @return Nones = ides - 8.
	 */
	private static int nonesOfMonth(final int month) {
		return idesOfMonth(month) - 8;
	}
}
