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
 * Implements the Old Hindu Solar calendar. See Calendrical Calculations for
 * reference.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class HinduOldSolar extends BaseCalendar
{
	private static final long serialVersionUID = 3611438619478769275L;

	/**
	 * Calendar epoch.
	 */
	public static final long EPOCH = new Julian(-3102, 2, 18).fixed;

	/**
	 * Arya solar year.
	 */
	public static final double ARYA_SOLAR_YEAR = 365.25868055555554D;

	/**
	 * Arya solar month.
	 */
	public static final double ARYA_SOLAR_MONTH = 30.43822337962963D;

	/**
	 * Arya Jupiter period.
	 */
	public static final double ARYA_JOVIAN_PERIOD = 4332.2721731681604D;

	/**
	 * Day of week names.
	 */
	public static final String DAY_OF_WEEK_NAMES[] = {
		"Ravivara", "Chandravara", "Mangalavara", "Buddhavara", "Brihaspatvara", "Sukravara", "Sanivara"
	};

	/**
	 * Month names.
	 */
	public static final String MONTH_NAMES[] = {
		"Mesha", "Vrishabha", "Mithuna", "Karka", "Simha", "Kanya", "Tula", "Vrischika", "Dhanu", "Makara", "Kumbha", "Mina"
	};

	private transient double elapsedDays;

	/**
	 * Fixed day constructor.
	 *
	 * @param fixed Fixed date.
	 */
	public HinduOldSolar(final long fixed) {
		super(EPOCH, fixed);
	}

	/**
	 * Julian day constructor.
	 *
	 * @param julianDay Julian day.
	 */
	public HinduOldSolar(final double julianDay) {
		super(EPOCH, julianDay);
	}

	/**
	 * Explicit constructor.
	 *
	 * @param year Year.
	 * @param month Month.
	 * @param day Day.
	 */
	public HinduOldSolar(final long year, final int month, final int day)
	{
		super(EPOCH, year, month, day);
	}

	/**
	 * To fixed day.
	 *
	 * @param year Year.
	 * @param month Month.
	 * @param day Day.
	 * @return Fixed day.
	 */
	public static long toFixedDay(final long year, final int month, final int day)
	{
		return (long) Math.ceil(EPOCH + year * ARYA_SOLAR_YEAR + (month - 1) * ARYA_SOLAR_MONTH + day - 2.25D);
	}

	@Override
	long toFixed(final long year, final int month, final int day) {
		return toFixedDay(year, month, day);
	}

	@Override
	long yearFromFixed() {
		this.elapsedDays = (double) dayCount(fixed) + 0.25;

		return (long) (this.elapsedDays / ARYA_SOLAR_YEAR);
	}

	@Override
	int monthFromFixed(final long year) {
		return 1 + (int) Calendar.mod(Calendar.quotient(elapsedDays, ARYA_SOLAR_MONTH), 12L);
	}

	@Override
	int dayFromFixed(final long year, final int month) {
		return 2 + (int) Math.floor(Calendar.mod(this.elapsedDays, ARYA_SOLAR_MONTH));
	}
	
	/**
	 * Gets days elapsed since epoch.
	 *
	 * @param fixed Fixed day.
	 * @return Elapsed days.
	 */
	public static long dayCount(final long fixed) {
		return fixed - EPOCH;
	}

	/**
	 * Gets the jovian year.
	 *
	 * @param fixed Fixed day.
	 * @return Jovian year.
	 */
	public static int jovianYear(final long fixed) {
		return 1 + (int) ((long) (dayCount(fixed) / 361.02268109734672D) % 60);
	}
}
