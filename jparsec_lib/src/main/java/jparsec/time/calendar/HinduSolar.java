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
 * Implements the Hindu Solar calendar. See Calendrical Calculations for
 * reference.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class HinduSolar extends HinduOldSolar
{
	private static final long serialVersionUID = -8096198193668204567L;

	/**
	 * Sidereal year length.
	 */
	public static final double SIDEREAL_YEAR = 365.2587564814815;

	/**
	 * Creation date.
	 */
	public static final double CREATION = (double) HinduOldSolar.EPOCH - 714402296627D;

	/**
	 * Anomalistic year length.
	 */
	public static final double ANOMALISTIC_YEAR = 365.25878920258134;

	/**
	 * Solar era.
	 */
	public static final int SOLAR_ERA = 3179;

	/**
	 * Ujjain location.
	 */
	public static final CityElement UJJAIN = new CityElement("Ujjain, India", Calendar.angle(75, 46, 0),
			Calendar.angle(23, 9, 0), 5.0512222222222221, 0);

	/**
	 * Hindu locale site, currently equal to Ujjain.
	 */
	public static final CityElement HINDU_LOCALE = UJJAIN;

	private static final double RS[] = { 0.92777777777777781, 0.99722222222222223, 1.075, 1.075, 0.99722222222222223D, 0.92777777777777781 };

	private transient double sunrise;
	
	private static final double CTE = 49.0 / 1260.0;

	/**
	 * Fixed day constructor.
	 *
	 * @param fixed Fixed day.
	 */
	public HinduSolar(final long fixed) {
		super(fixed);
	}

	/**
	 * Julian day constructor.
	 *
	 * @param jd Julian day.
	 */
	public HinduSolar(final double jd)
	{
		super(jd);
	}

	/**
	 * Explicit constructor.
	 *
	 * @param y Year.
	 * @param m Month.
	 * @param d Day.
	 */
	public HinduSolar(long y, int m, int d)
	{
		super(y, m, d);
	}

	private final static double d = 360.0 / SIDEREAL_YEAR;
	
	/**
	 * To fixed day.
	 *
	 * @param hs The instance of HinduSolar.
	 * @param year Year.
	 * @param month Month.
	 * @param day Day.
	 * @return Fixed day.
	 */
	public static long toFixedDay(final HinduSolar hs, final long year, final int month, final int day)
	{
		long fixed = ((long) Math.floor((year + 3179 + (month - 1.0) / 12.0) * SIDEREAL_YEAR)) + EPOCH + day - 2;
		double d1 = (month - 1) * 30 + (day - 2) * d;
		double d2 = Calendar.mod((solarLongitude(fixed + 0.25) - d1) + 180.0, 360.0) - 180.0;
		long l1 = fixed - (long) Math.ceil(d2 / d) - 2;

		while (!onOrBefore(hs, new HinduSolar(l1))) {
			l1++;
		}

		return l1;
	}

	/**
	 * To fixed day.
	 *
	 * @return Fixed day.
	 */
	@Override
	long toFixed(final long year, final int month, final int day)
	{
		return toFixedDay(this, year, month, day);
	}

	@Override
	long yearFromFixed() {
		this.sunrise = sunrise(this.fixed + 2);

		return calendarYear(sunrise) - 3179;
	}

	@Override
	int monthFromFixed(long year) {
		return zodiac(this.sunrise);
	}

	@Override
	int dayFromFixed(long year, int month) {
		long l1 = fixed - 3 - (long) (Math.floor(solarLongitude(this.sunrise)) % 30.0);
		long l2 = l1;

		while (zodiac(sunrise(l2 + 1)) != this.month) {
			l2++;
		}

		return 2 + (int) (fixed - l2);
	}

	private static double hinduSineTable(int i)
	{
		double d = 3438D * Calendar.sinDegrees(((double) i * 225D) / 60D);
		double d1 = 0.215D * (double) Calendar.signum(d) * (double) Calendar.signum(Math.abs(d) - 1716D);
		return (double) Math.round(d + d1) / 3438D;
	}

	private static double hinduSine(double d)
	{
		double d1 = (d * 60D) / 225D;
		double d2 = Calendar.mod(d1, 1.0D);
		return d2 * hinduSineTable((int) Math.ceil(d1)) + (1.0D - d2) * hinduSineTable((int) Math.floor(d1));
	}

	private static double hinduArcsin(double d)
	{
		boolean flag = d < 0.0D;
		if (flag)
			d = -d;
		int i;
		for (i = 0; d > hinduSineTable(i); i++)
			;
		double d1 = hinduSineTable(i - 1);
		double d2 = 3.75D * ((double) (i - 1) + (d - d1) / (hinduSineTable(i) - d1));
		if (flag)
			d2 = -d2;
		return d2;
	}

	private static double meanPosition(double d, double d1)
	{
		return 360.0 * Calendar.mod((d - CREATION) / d1, 1.0D);
	}

	/**
	 * Returns solar true position.
	 *
	 * @param d
	 * @param d1
	 * @param d2
	 * @param d3
	 * @param d4
	 * @return Sun's true position.
	 */
	static double truePosition(double d, double d1, double d2, double d3, double d4)
	{
		double d5 = meanPosition(d, d1);
		double d6 = hinduSine(meanPosition(d, d3));
		double d7 = Math.abs(d6) * d4 * d2;
		double d8 = hinduArcsin(d6 * (d2 - d7));
		return Calendar.mod(d5 - d8, 360D);
	}

	/**
	 * Gets the solar longitude.
	 *
	 * @param d Fixed day.
	 * @return Solar longitude.
	 */
	public static double solarLongitude(double d)
	{
		return truePosition(d, SIDEREAL_YEAR, CTE, ANOMALISTIC_YEAR, 0.023809523809523808D);
	}

	/**
	 * Returns zodiac sign.
	 *
	 * @param d Fixed day.
	 * @return Zodiac sign integer.
	 */
	public static int zodiac(double d)
	{
		return (int) Calendar.quotient(solarLongitude(d), 30.0) + 1;
	}

	private static boolean onOrBefore(HinduSolar hindusolar, HinduSolar hindusolar1)
	{
		return hindusolar.year < hindusolar1.year || hindusolar.year == hindusolar1.year && (hindusolar.month < hindusolar1.month || hindusolar.month == hindusolar1.month && hindusolar.day <= hindusolar1.day);
	}

	/**
	 * Gets calendar day.
	 *
	 * @param d Fixed date.
	 * @return Calendar day.
	 */
	public static long calendarYear(double d)
	{
		return Math.round((d - (double) HinduOldSolar.EPOCH) / SIDEREAL_YEAR - solarLongitude(d) / 360.0);
	}

	private static double equationOfTime(long fixed)
	{
		double d = hinduSine(meanPosition(fixed, ANOMALISTIC_YEAR));
		double d1 = ((d * 3438D) / 60D) * (Math.abs(d) / 1080D - CTE);
		return (((dailyMotion(fixed) / 360D) * d1) / 360D) * SIDEREAL_YEAR;
	}

	private static double ascensionalDifference(long fixed, CityElement location)
	{
		double d = 0.40634089586969169D * hinduSine(tropicalLongitude(fixed));
		double d1 = location.latitude;
		double d2 = hinduSine(90.0 + hinduArcsin(d));
		double d3 = hinduSine(d1) / hinduSine(90.0 + d1);
		double d4 = d * d3;
		return hinduArcsin(-(d4 / d2));
	}

	private static double tropicalLongitude(long fixed)
	{
		long l1 = (long) Math.floor(fixed - HinduOldSolar.EPOCH);
		double d = 27.0 - Math.abs(54.0 - Calendar.mod(
				27.0 + 108.0 * 3.8024793772721099E-007D * (double) l1, 108D));
		return Calendar.mod(solarLongitude(fixed) - d, 360D);
	}

	private static double risingSign(long fixed)
	{
		int i = (int) Calendar.mod(Calendar.quotient(tropicalLongitude(fixed), 30.0), 6L);
		return RS[i];
	}

	private static double dailyMotion(long fixed)
	{
		double d = 360.0 / SIDEREAL_YEAR;
		double d1 = meanPosition(fixed, ANOMALISTIC_YEAR);
		double d2 = CTE - Math.abs(hinduSine(d1)) / 1080D;
		int i = (int) Calendar.quotient(d1, 225.0 / 60.0);
		double d3 = hinduSineTable(i + 1) - hinduSineTable(i);
		double d4 = d3 * -15D * d2;
		return d * (d4 + 1.0D);
	}

	private static double solarSiderealDifference(long fixed)
	{
		return dailyMotion(fixed) * risingSign(fixed);
	}

	/**
	 * Gets the sunrise time.
	 *
	 * @param fixed Fixed day.
	 * @return Sunrise time.
	 */
	public static double sunrise(long fixed)
	{
		return (double) fixed + 0.25D + (UJJAIN.longitude - HINDU_LOCALE.longitude) / 360.0 + equationOfTime(fixed) + (0.99726968985094955 / 360.0) * (ascensionalDifference(fixed, HINDU_LOCALE) + 0.25D * solarSiderealDifference(fixed));
	}

	/**
	 * Gets the solar longitude after some time from certain instant.
	 *
	 * @param d Fixed date.
	 * @param d1 Time to add.
	 * @return Solar longitude.
	 */
	public static double solarLongitudeAfter(double d, double d1)
	{
		double d2 = 0.000001;
		double d3 = d + 1.0146076568930043D * Calendar.mod(d1 - solarLongitude(d), 360.0);
		double d4 = Math.max(d, d3 - 5D);
		double d5 = d3 + 5D;
		double d6 = d4;
		double d7 = d5;
		double d8;
		for (d8 = (d7 + d6) / 2D; d7 - d6 >= d2; d8 = (d7 + d6) / 2D)
			if (Calendar.mod(solarLongitude(d8) - d1, 360D) < 180.0)
				d7 = d8;
			else
				d6 = d8;

		return d8;
	}

	/**
	 * Gets Mesha Samkranti.
	 *
	 * @param fixed Fixed day.
	 * @return Such date.
	 */
	public static double meshaSamkranti(long fixed)
	{
		long l1 = new Gregorian(fixed, 1, 1).fixed;
		return solarLongitudeAfter(l1, 0.0);
	}
}
