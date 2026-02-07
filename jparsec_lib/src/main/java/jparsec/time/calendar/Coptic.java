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
 * Implements the Coptic calendar. See Calendrical Calculations for reference.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Coptic extends BaseCalendar
{
	private static final long serialVersionUID = 3103648666347143692L;
	
	/**
	 * Calendar epoch.
	 */
	public static final long EPOCH = new Julian(284, 8, 29).fixed;

	/**
	 * Week day names.
	 */
	public static final String DAY_OF_WEEK_NAMES[] = {
		"Tkyriaka", "Pesnau", "Pshoment", "Peftoou", "Ptiou", "Psoou", "Psabbaton"
	};

	/**
	 * Month names.
	 */
	public static final String MONTH_NAMES[] = {
		"Tut", "Babah", "Hatur", "Kiyahk", "Tubah", "Amshir", "Baramhat",
		"Baramundah", "Bashans", "Ba'unah", "Abib", "Misra", "al-Nasi"
	};

	/**
	 * Fixed day constructor.
	 *
	 * @param fixed Julian day.
	 */
	public Coptic(final long fixed) {
		super(EPOCH, fixed);
	}

	Coptic(final long epoch, final long fixed) {
		super(epoch, fixed);
	}

	/**
	 * Julian day constructor.
	 *
	 * @param julianDay Julian day.
	 */
	public Coptic(final double julianDay) {
		super(EPOCH, julianDay);
	}

	Coptic(final long epoch, final double julianDay) {
		super(epoch, julianDay);
	}

	/**
	 * Explicit constructor.
	 *
	 * @param year Year.
	 * @param month Month.
	 * @param day Day.
	 */
	public Coptic(final long year, final int month, final int day) {
		super (EPOCH, year, month, day);
	}

	Coptic(final long epoch, final long year, final int month, final int day) {
		super (epoch, year, month, day);
	}

	/**
	 * To fixed day..
	 *
	 * @param year Year.
	 * @param month Month.
	 * @param day Day.
	 * @return Fixed day.
	 */
	@Override
	long toFixed(final long year, final int month, final int day) {
		return (this.epoch - 1) + 365 * (year - 1) + (year / 4) + 30 * (month - 1) + day;
	}

	@Override
	long yearFromFixed() {
		return (long) Math.floor((4 * (fixed - getEpoch()) + 1463.0) / 1461.0);
	}

	@Override
	int monthFromFixed(final long year) {
		return 1 + (int) ((this.fixed - toFixed(year, 1, 1)) / 30);
	}

	@Override
	int dayFromFixed(final long year, final int month) {
		return 1 + (int) (this.fixed - toFixed(year, month, 1));
	}

	/**
	 * Returns if the year is a leap one.
	 *
	 * @param year Year.
	 * @return True if it is a leap year.
	 */
	public static boolean isLeapYear(final long year) {
		return (year % 4) == 3;
	}
}
