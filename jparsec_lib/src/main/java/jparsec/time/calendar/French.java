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
 * Implements the French revolution calendar.
 * <p>
 * This calendar was instituted by the National Convention of the French Republic in 1793.
 * It was used until the end of Gregorian year 1805.
 * <p>
 * See Calendrical Calculations for reference.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class French extends BaseCalendar
{
	private static final long serialVersionUID = 6342987068941343569L;
	
	/**
	 * Calendar epoch.
	 */
	public static final long EPOCH = new Gregorian(1792, 9, 22).fixed;

	/**
	 * Paris location.
	 */
	public static final CityElement PARIS = new CityElement("Paris, France", Calendar.angle(2, 20, 15), Calendar.angle(48, 50, 11), 1.0, 27);

	/**
	 * Month poetic names coined by Fabre day'Eglantine.
	 */
	public static final String MONTH_NAMES[] = {
		"Vendemiaire", "Brumaire", "Frimaire", "Nivose", "Pluviose", "Ventose", "Germinal",
		"Floreal", "Prairial", "Messidor", "Thermidor", "Fructidor", "Sansculottides"
	};

	/**
	 * Day of week names.
	 */
	public static final String DAY_OF_WEEK_NAMES[] = {
		"Primidi", "Duodi", "Tridi", "Quartidi", "Quintidi", "Sextidi", "Septidi", "Octidi", "Nonidi", "Decadi"
	};

	/**
	 * Special day names.
	 */
	public static final String SPECIAL_DAY_NAMES[] = {
		"Jour de la Vertu", "Jour du Genie", "Jour du Labour", "Jour de la Raison", "Jour de la Recompense", "Jour de la Revolution"
	};

	/**
	 * Decade names.
	 */
	public static final String DECADE_NAMES[] = { "I", "II", "III" };

	private transient long newYear;

	/**
	 * Midnight in Paris.
	 *
	 * @param fixed Fixed day.
	 * @return Midnight time.
	 */
	public static double midnightInParis(final long fixed) {
		return Calendar.universalFromStandard(Calendar.midnight(fixed + 1L, PARIS), PARIS);
	}

	/**
	 * New year before certain fixed date.
	 *
	 * @param fixed Fixed date.
	 * @return New year fixed date.
	 */
	public static long newYearOnOrBefore(final long fixed) {
		double day = Calendar.estimatePriorSolarLongitude(midnightInParis(fixed), Calendar.AUTUMN) - 1.0;
		long newYear = (long) (Math.floor(day));

		while (Calendar.AUTUMN > Calendar.solarLongitude(midnightInParis(newYear))) {
			newYear++;
		}

		return newYear;
	}

	/**
	 * Constructs a French date with a fixed day
	 *
	 * @param fixed Julian day
	 */
	public French(final long fixed) {
		super(EPOCH, fixed);
	}

	/**
	 * Constructs a French date with a Julian day
	 *
	 * @param julianDay Julian day
	 */
	public French(final double julianDay) {
		super(EPOCH, julianDay);
	}

	/**
	 * Explicit constructor.
	 *
	 * @param year Year.
	 * @param month Month.
	 * @param day Day.
	 */
	public French(final long year, final int month, final int day) {
		super(EPOCH, year, month, day);
	}

	@Override
	long toFixed(final long year, final int month, final int day) {
		long l1 = newYearOnOrBefore((long) Math.floor(this.epoch + 180 + Calendar.TROPICAL_YEAR * (year - 1)));

		return l1 - 1 + 30 * (month - 1) + day;
	}

	@Override
	long yearFromFixed() {
		newYear = newYearOnOrBefore(this.fixed);
		return Math.round((newYear - this.epoch) / Calendar.TROPICAL_YEAR) + 1;
	}

	@Override
	int monthFromFixed(long year) {
		return 1 + (int) (fixed - newYear) / 30;
	}

	@Override
	int dayFromFixed(long year, int month) {
		return 1 + (int) (fixed - newYear) % 30;
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
