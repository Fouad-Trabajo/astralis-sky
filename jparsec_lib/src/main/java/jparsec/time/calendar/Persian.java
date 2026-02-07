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

import jparsec.observer.CityElement;

/**
 * Implements the Persian calendar.
 * <P>
 * The modern Persian solar calendar, adopted in 1925, and based on the old
 * arithmetic calendar.
 *
 * See Calendrical Calculations for reference.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Persian extends BaseCalendar
{
	private static final long serialVersionUID = 8315300467974129047L;
	
	/**
	 * Calendar epoch.
	 */
	public static final long EPOCH = new Julian(622, 3, 19).fixed;

	/**
	 * Tehran location.
	 */
	public static final CityElement TEHRAN = new CityElement("Tehran, Iran", 51.42, 35.68, 3.5, 1100);

	/**
	 * Day of week names.
	 */
	public static final String DAY_OF_WEEK_NAMES[] = {
		"Yek-shanbeh", "Do-shanbeh", "Se-shanbeh", "Char-shanbeh", "Panj-shanbeh", "Jom`eh", "Shanbeh"
	};

	/**
	 * Month names.
	 */
	public static final String MONTH_NAMES[] = {
		"Farvardin", "Ordibehesht", "Xordad", "Tir", "Mordad", "Shahrivar", "Mehr", "Aban", "Azar", "Dey", "Bahman", "Esfand"
	};

	/**
	 * Fixed day constructor.
	 *
	 * @param fixed fixed day.
	 */
	public Persian(final long fixed) {
		super(EPOCH, fixed);
	}

	/**
	 * Julian day constructor.
	 *
	 * @param julianDay Julian day.
	 */
	public Persian(final double julianDay) {
		super(EPOCH, julianDay);
	}

	/**
	 * Explicit constructor.
	 *
	 * @param year Year.
	 * @param month Month.
	 * @param day Day.
	 */
	public Persian(final long year, final int month, final int day) {
		super(EPOCH, year, month, day);
	}

	/**
	 * To fixed day.
	 *
	 * @param year Year.
	 * @param month Month.
	 * @param day Day.
	 * @return Fixed date.
	 */
	public static long toFixedDay(final long year, final int month, final int day)
	{
		long l1 = newYearOnOrBefore(EPOCH + 180 + (long) Math.floor(Calendar.TROPICAL_YEAR * (year <= 0 ? year : year - 1)));
		return (l1 - 1) + (long) (month > 7 ? 30 * (month - 1) + 6 : 31 * (month - 1)) + day;
	}

	@Override
	long toFixed(final long year, final int month, final int day) {
		return toFixedDay(year, month, day);
	}

	@Override
	long yearFromFixed() {
		long l1 = newYearOnOrBefore(this.fixed);
		long l2 = 1 + Math.round((double) (l1 - EPOCH) / Calendar.TROPICAL_YEAR);

		return l2 <= 0 ? l2 - 1 : l2;
	}

	@Override
	int monthFromFixed(final long year) {
		long l3 = (1 + this.fixed) - toFixed(year, 1, 1);

		return l3 >= 186 ? (int) Math.ceil((double) (l3 - 6) / 30) : (int) Math.ceil((double) l3 / 31);
	}

	@Override
	int dayFromFixed(final long year, final int month) {
		return 1 + (int) (this.fixed - toFixed(year, month, 1));
	}

	/**
	 * Gets the midday in Tehran.
	 *
	 * @param fixed Fixed day.
	 * @return Midday time.
	 */
	public static double middayInTehran(final long fixed) {
		return Calendar.universalFromStandard(Calendar.midday(fixed, TEHRAN), TEHRAN);
	}

	/**
	 * Gets previous new year.
	 *
	 * @param fixed Fixed day.
	 * @return Last new year.
	 */
	public static long newYearOnOrBefore(final long fixed) {
		double d = Calendar.estimatePriorSolarLongitude(middayInTehran(fixed), Calendar.SPRING);
		long l1;
		for (l1 = (long) Math.floor(d) - 1L; Calendar.solarLongitude(middayInTehran(l1)) > Calendar.SPRING + 2.0; l1++)
			;
		return l1;
	}

	/**
	 * Gets new year.
	 *
	 * @param fixed Fixed day.
	 * @return New Year.
	 */
	public static long nawRuz(final long fixed) {
		long l1 = (1 + fixed) - Gregorian.yearFromFixed(EPOCH);

		return toFixedDay(l1 > 0L ? l1 : l1 - 1L, 1, 1);
	}
}
