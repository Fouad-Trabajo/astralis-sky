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
 * Implements the Islamic arithmetic calendar.
 * <P>
 * The lunar calendar used by Moslems. Days begin at sunset.
 * <P>
 * See Calendrical Calculations for reference.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Islamic extends BaseCalendar
{
	private static final long serialVersionUID = -4912008118083746225L;
	
	/**
	 * Calendar epoch.
	 */
	public static final long EPOCH = new Julian(622, 7, 16).fixed;

	/**
	 * Mecca location.
	 */
	public static final CityElement MECCA = new CityElement("Mecca, Saudi Arabia", Calendar.angle(39, 49, 24),
			Calendar.angle(21, 25, 24), 2, 1000);

	/**
	 * Day of week names.
	 */
	public static final String DAY_OF_WEEK_NAMES[] = {
		"yaum al-ahad", "yaum al-ithnayna", "yaum ath-thalatha'", "yaum al-arba`a'", "yaum al-hamis", "yaum al-jum`a",
		"yaum as-sabt"
	};

	/**
	 * Month names.
	 */
	public static final String MONTH_NAMES[] = {
		"Muharram", "Safar", "Rabi I", "Rabi II", "Jumada I", "Jumada II", "Rajab", "Sha`ban", "Ramadan", "Shawwal",
		"Dhu al-Qa`da", "Dhu al-Hijja"
	};

	/**
	 * Fixed day constructor.
	 *
	 * @param fixed fixed day.
	 */
	public Islamic(final long fixed) {
		super(EPOCH, fixed);
	}

	/**
	 * Julian day constructor.
	 *
	 * @param jd Julian day.
	 */
	public Islamic(final double jd) {
		super(EPOCH, jd);
	}

	/**
	 * Explicit constructor.
	 *
	 * @param year Year.
	 * @param month Month.
	 * @param day Day.
	 */
	public Islamic(final long year, final int month, final int day) {
		super(EPOCH, year, month, day);
	}

	/**
	 * To fixed date.
	 *
	 * @param year Year.
	 * @param month Month.
	 * @param day Day.
	 * @return Fixed day.
	 */
	public static long toFixedDay(final long year, final int month, final int day) {
		return ((long) (day + 29 * (month - 1)) + (6 * month - 1) / 11 + (year - 1) * 354 + (3 + 11 * year) / 30) + EPOCH - 1;
	}

	@Override
	long toFixed(final long year, final int month, final int day) {
		return toFixedDay(year, month, day);
	}

	@Override
	long yearFromFixed() {
		return (30 * (this.fixed - EPOCH) + 10646) / 10631;
	}

	@Override
	int monthFromFixed(final long year) {
		int days = (int) (this.fixed - toFixed(year, 1, 1));

		return (11 * days + 330) / 325;
	}

	@Override
	int dayFromFixed(final long year, final int month) {
		return 1 + (int) (this.fixed - toFixed(year, month, 1));
	}

	/**
	 * To know if the year is a leap one.
	 *
	 * @return True if it is a leap year.
	 */
	public boolean isLeapYear() {
		return ((11 * this.fixed + 14) % 30) < 11;
	}
}
