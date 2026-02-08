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
 * Implements the Hindu Lunar calendar. See Calendrical Calculations for
 * reference.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class HinduLunar implements Serializable
{
	private static final long serialVersionUID = 8237202254694851751L;
	
	/**
	 * Lunar era.
	 */
	public static final int LUNAR_ERA = 3044;

	/**
	 * Synodic month length in days.
	 */
	public static final double SYNODIC_MONTH = 29.530587946071719D;

	/**
	 * Sidereal month length in days.
	 */
	public static final double SIDEREAL_MONTH = 27.321674162683866D;

	/**
	 * Anomalistic month length in days.
	 */
	public static final double ANOMALISTIC_MONTH = 27.554597974680476D;

	/**
	 * Year.
	 */
	public long year;

	/**
	 * Month.
	 */
	public int month;

	/**
	 * Leap month?
	 */
	public boolean leapMonth;

	/**
	 * Day.
	 */
	public int day;

	/**
	 * Leap day?
	 */
	public boolean leapDay;

	/**
	 * Default constructor.
	 */
	public HinduLunar() {}

	/**
	 * Fixed day constructor.
	 *
	 * @param l Fixed date.
	 */
	public HinduLunar(long l)
	{
		fromFixed(l);
	}

	/**
	 * Julian day constructor.
	 *
	 * @param jd Julian day.
	 */
	public HinduLunar(double jd)
	{
		fromJulianDay(jd);
	}

	/**
	 * Explicit constructor.
	 *
	 * @param hyear Year.
	 * @param hmonth Month.
	 * @param hleapMonth Leap month.
	 * @param hday Day.
	 * @param hleapDay Leap day.
	 */
	public HinduLunar(long hyear, int hmonth, boolean hleapMonth, int hday, boolean hleapDay)
	{
		year = hyear;
		month = hmonth;
		leapMonth = hleapMonth;
		day = hday;
		leapDay = hleapDay;
	}

	/**
	 * To fixed day.
	 *
	 * @param year Year.
	 * @param month Month.
	 * @param leapMonth Leap month.
	 * @param day Day.
	 * @param leapDay Leap day.
	 * @return Fixed date.
	 */
	public static long toFixed(long year, int month, boolean leapMonth, int day, boolean leapDay)
	{
		return (new HinduLunar(year, month, leapMonth, day, leapDay)).toFixed();
	}

	/**
	 * To fixed day.
	 *
	 * @return Fixed date.
	 */
	public long toFixed()
	{
		int day = this.day - 1;
		double d = (double) HinduOldSolar.EPOCH + HinduSolar.SIDEREAL_YEAR * ((double) (year + 3044L) + (double) (month - 1) / 12D);
		long l = (long) Math.floor(d - (1.0 / 360.0) * HinduSolar.SIDEREAL_YEAR * (Calendar
				.mod((HinduSolar.solarLongitude(d) - (double) (month - 1) * 30.0) + 180.0,
						360.0) - 180.0));
		int i = lunarDay((double) l + 0.25D);
		long l1;
		if (i > 3 && i < 27)
		{
			l1 = i;
		} else
		{
			HinduLunar hindulunar = new HinduLunar(l - 15L);
			if (hindulunar.month < month || hindulunar.leapMonth && !leapMonth)
				l1 = Calendar.mod(i + 15, 30) - 15;
			else
				l1 = Calendar.mod(i - 15, 30) + 15;
		}
		long l2 = (l + (long) day) - l1;
		long l3 = (l2 - (long) Calendar.mod((lunarDay((double) l2 + 0.25D) - day) + 15, 30)) + 15L;
		long l4;
		for (l4 = l3 - 1L; !onOrBefore(this, new HinduLunar(l4)); l4++)
			;
		return l4;
	}

	/**
	 * Sets the date from the fixed day.
	 *
	 * @param l Fixed day.
	 */
	public void fromFixed(long l)
	{
		double d = HinduSolar.sunrise(l);
		day = lunarDay(d);
		leapDay = day == lunarDay(HinduSolar.sunrise(l - 1L));
		double d1 = newMoonBefore(d);
		double d2 = newMoonBefore(Math.floor(d1) + 35D);
		int i = HinduSolar.zodiac(d1);
		leapMonth = i == HinduSolar.zodiac(d2);
		month = Calendar.adjustedMod(i + 1, 12);
		year = HinduSolar.calendarYear(d2) - 3044L - (long) (!leapMonth || month != 1 ? 0 : -1);
		day ++;
	}

	/**
	 * Gets the previous new Moon.
	 *
	 * @param d Fixed day.
	 * @return New Moon.
	 */
	public static double newMoonBefore(double d)
	{
		double d1 = Math.pow(2D, -34D);
		double d2 = d - (1.0D / 360.0) * lunarPhase(d) * SYNODIC_MONTH;
		double d3 = d2 - 1.0D;
		double d4 = Math.min(d, d2 + 1.0D);
		double d5 = d3;
		double d6 = d4;
		double d7;
		for (d7 = (d6 + d5) / 2D; HinduSolar.zodiac(d5) != HinduSolar.zodiac(d6) && d6 - d5 >= d1; d7 = (d6 + d5) / 2D)
			if (lunarPhase(d7) < 180.0)
				d6 = d7;
			else
				d5 = d7;

		return d7;
	}

	/**
	 * Returns true if the second date is on or before the first.
	 *
	 * @param hindulunar HinduLunar instance.
	 * @param hindulunar1 HinduLunar instance.
	 * @return True or false.
	 */
	public static boolean onOrBefore(HinduLunar hindulunar, HinduLunar hindulunar1)
	{
		return hindulunar.year < hindulunar1.year || hindulunar.year == hindulunar1.year && (hindulunar.month < hindulunar1.month || hindulunar.month == hindulunar1.month && (hindulunar.leapMonth && !hindulunar1.leapMonth || hindulunar.leapMonth == hindulunar1.leapMonth && (hindulunar.day < hindulunar1.day || hindulunar.day == hindulunar1.day && (!hindulunar.leapDay || hindulunar1.leapDay))));
	}

	/**
	 * Returns lunar day.
	 *
	 * @param d Date.
	 * @param d1 Fraction of day.
	 * @return Lunar date.
	 */
	public static double lunarDayAfter(double d, double d1)
	{
		double d2 = Math.pow(2D, -17D);
		double d3 = (d1 - 1.0D) * 12D;
		double d4 = d + 0.0027777777777777779D * Calendar.mod(d3 - lunarPhase(d), 360.0) * SYNODIC_MONTH;
		double d5 = Math.max(d, d4 - 2D);
		double d6 = d4 + 2D;
		double d7 = d5;
		double d8 = d6;
		double d9;
		for (d9 = (d8 + d7) / 2D; d8 - d7 >= d2; d9 = (d8 + d7) / 2D)
			if (Calendar.mod(lunarPhase(d9) - d3, 360D) < 180.0)
				d8 = d9;
			else
				d7 = d9;

		return d9;
	}

	/**
	 * Gets Moon longitude.
	 *
	 * @param d Date.
	 * @return Moon longitude.
	 */
	public static double lunarLongitude(double d)
	{
		return HinduSolar.truePosition(d, SIDEREAL_MONTH, 0.088888888888888892D, ANOMALISTIC_MONTH,
				0.010416666666666666D);
	}

	/**
	 * Gets Moon phase.
	 *
	 * @param d Date.
	 * @return Moon phase.
	 */
	public static double lunarPhase(double d)
	{
		return Calendar.mod(lunarLongitude(d) - HinduSolar.solarLongitude(d), 360D);
	}

	/**
	 * Gets Moon day.
	 *
	 * @param d Date.
	 * @return Moon day.
	 */
	public static int lunarDay(double d)
	{
		return (int) Calendar.quotient(lunarPhase(d), 12.0) + 1;
	}

	/**
	 * Gets lunar station.
	 *
	 * @param l Date.
	 * @return Moon station.
	 */
	public static int lunarStation(long l)
	{
		double d = HinduSolar.sunrise(l);
		return (int) Calendar.quotient(lunarLongitude(d), 800.0 / 60.0) + 1;
	}

	/**
	 * Gets Moon longitude.
	 *
	 * @param l Date.
	 * @return Moon longitude.
	 */
	public static long newYear(long l)
	{
		long l1 = new Gregorian(l, 1, 1).fixed;
		double d = HinduSolar.solarLongitudeAfter(l1, 330.0);
		double d1 = lunarDayAfter(d, 1.0D);
		long l2 = (long) Math.floor(d1);
		double d2 = HinduSolar.sunrise(l2);
		return l2 + (long) (d1 >= d2 && lunarDay(HinduSolar.sunrise(l2 + 1L)) != 2 ? 1 : 0);
	}

	/**
	 * Gets Karana.
	 *
	 * @param i Days.
	 * @return Karana.
	 */
	public static int karana(int i)
	{
		if (i == 1)
			return 0;
		if (i > 57)
			return i - 50;
		else
			return Calendar.adjustedMod(i - 1, 7);
	}

	/**
	 * Gets Yoga.
	 *
	 * @param l Date.
	 * @return Yoga.
	 */
	public static int yoga(long l)
	{
		return (int) Math.floor(Calendar.mod(((HinduSolar.solarLongitude(l) + lunarLongitude(l)) * 60.0) / 800.0, 27.0)) + 1;
	}

	/**
	 * Transforms a Hindu date into a Julian day
	 *
	 * @param year Year.
	 * @param month Month.
	 * @param leapMonth Leap month.
	 * @param day Day.
	 * @param leapDay Leap day.
	 * @return Julian day.
	 */
	public static int toJulianDay(int year, int month, boolean leapMonth, int day, boolean leapDay)
	{
		return (int) (toFixed(year, month, leapMonth, day, leapDay) + Gregorian.EPOCH);
	}

	/**
	 * Transforms a Hindu date into a Julian day
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
