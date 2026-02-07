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

import static jparsec.time.calendar.HinduOldSolar.ARYA_SOLAR_MONTH;
import static jparsec.time.calendar.HinduOldSolar.ARYA_SOLAR_YEAR;

/**
 * Implements the Old Hindu Lunar calendar. See Calendrical Calculations for
 * reference.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class HinduOldLunar implements Serializable
{
	private static final long serialVersionUID = 5553870750103742268L;

	/**
	 * Year.
	 */
	public long year;

	/**
	 * Month.
	 */
	public int month;

	/**
	 * Leap month.
	 */
	public boolean leapMonth;

	/**
	 * Day.
	 */
	public int day;

	/**
	 * Arya lunar month.
	 */
	public static final double ARYA_LUNAR_MONTH = 29.530581807581694D;

	/**
	 * Arya lunar day.
	 */
	public static final double ARYA_LUNAR_DAY = 0.9843527269193898D;

	/**
	 * Day of week names.
	 */
	public static final String DAY_OF_WEEK_NAMES[] =
			{ "Ravivara", "Chandravara", "Mangalavara", "Buddhavara", "Brihaspatvara", "Sukravara", "Sanivara" };

	/**
	 * Month names.
	 */
	public static final String MONTH_NAMES[] =
			{ "Chaitra", "Vaisakha", "Jyaishtha", "Ashadha", "Sravana", "Bhadrapada", "Asvina", "Kartika", "Margasirsha",
					"Pausha", "Magha", "Phalguna" };

	/**
	 * Default constructor.
	 */
	public HinduOldLunar() {}

	/**
	 * Julian day constructor.
	 *
	 * @param jd Julian day.
	 */
	public HinduOldLunar(double jd)
	{
		fromJulianDay(jd);
	}

	/**
	 * Explicit constructor.
	 *
	 * @param hyear Year.
	 * @param hmonth Month.
	 * @param hleap Leap month.
	 * @param hday Day.
	 */
	public HinduOldLunar(long hyear, int hmonth, boolean hleap, int hday)
	{
		year = hyear;
		month = hmonth;
		leapMonth = hleap;
		day = hday;
	}

	/**
	 * To fixed day.
	 *
	 * @param year Year.
	 * @param month Month.
	 * @param leap Leap month.
	 * @param day Day.
	 * @return Fixed day.
	 */
	public static long toFixed(long year, int month, boolean leap, int day)
	{
		day--;
		double mina = (double) (12L * year - 1L) * ARYA_SOLAR_MONTH;
		double lunarNewYear = ARYA_LUNAR_MONTH * (double) (Calendar.quotient(mina, ARYA_LUNAR_MONTH) + 1L);
		long temp = leap || Math.ceil((lunarNewYear - mina) / ARYA_LUNAR_DAY) > month ? month - 1L : month;

		return (long) Math.floor(
				(double) HinduOldSolar.EPOCH +
						lunarNewYear +
						ARYA_LUNAR_MONTH * temp +
						(double) (day - 1) * ARYA_LUNAR_DAY + 0.75D);
	}

	/**
	 * To fixed day.
	 *
	 * @return Fixed day.
	 */
	public long toFixed()
	{
		return toFixed(year, month, leapMonth, day);
	}

	/**
	 * Sets the date from fixed date.
	 *
	 * @param l Fixed day.
	 */
	public void fromFixed(long l)
	{
		double d = (double) HinduOldSolar.dayCount(l) + 0.25D;
		double d1 = d - Calendar.mod(d, ARYA_LUNAR_MONTH);
		leapMonth = Calendar.mod(d1, ARYA_SOLAR_MONTH) <= 0.90764157204793605D &&
				Calendar.mod(d1, ARYA_SOLAR_MONTH) > 0.0D;
		month = 1 + (int) Calendar.mod(Math.ceil(d1 / ARYA_SOLAR_MONTH), 12D);
		day = 1 + (int) Calendar.mod(Calendar.quotient(d, ARYA_LUNAR_DAY), 30L);
		year = (long) Math.ceil((d1 + ARYA_SOLAR_MONTH) / ARYA_SOLAR_YEAR) - 1L;
		day++;
	}

	/**
	 * Informs if the year is or not a leap one.
	 *
	 * @param y Year.
	 * @return True or false.
	 */
	public static boolean isLeapYear(long y)
	{
		return Calendar.mod((double) y * ARYA_SOLAR_YEAR - ARYA_SOLAR_MONTH, ARYA_LUNAR_MONTH) >= 18.638882943006465D;
	}

	/**
	 * Transforms a Hindu date into a Julian day.
	 *
	 * @param year Year.
	 * @param month Month.
	 * @param leap Leap month.
	 * @param day Day.
	 * @return Julian day.
	 */
	public static int toJulianDay(int year, int month, boolean leap, int day)
	{
		return (int) (toFixed(year, month, leap, day) + Gregorian.EPOCH);
	}

	/**
	 * Transforms a Hindu date into a Julian day.
	 *
	 * @return Julian day.
	 */
	public double toJulianDay()
	{
		return 0.5 + (int) (toFixed() + Gregorian.EPOCH);
	}

	/**
	 * Sets a Hindu date with a given Julian day.
	 *
	 * @param jd Julian day.
	 */
	public void fromJulianDay(double jd)
	{
		fromFixed((long) Math.floor(jd - 0.5) - Gregorian.EPOCH);
	}
}
