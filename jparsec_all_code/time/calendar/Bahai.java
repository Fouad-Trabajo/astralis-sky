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
 * Implements the Bahai calendar.
 * <P>
 * A calendar based on the 19 year cycle 1844-1863 of the B&aacute;b, the martyred
 * forerunner of Bah&aacute;'u'l&aacute;lh and co-founder of the Bah&aacute;'&iacute; faith. 
 * Days begin at sunset.
 * <P>
 * See Calendrical Calculations for reference.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Bahai extends BaseCalendar
{
	private static final long serialVersionUID = -2219563471051861858L;
	
	/**
	 * Calendar epoch.
	 */
	public static final long EPOCH = new Gregorian(1844L, 3, 21).getFixed();

	/**
	 * Ayyam I ha.
	 */
	public static final int AYYAM_I_HA = 0;

	/**
	 * Name of the days of the week.
	 */
	public static final String DAY_OF_WEEK_NAMES[] = { "Jamal", "Kamal", "Fidal", "`Idal", "Istijlal", "Istiqlal", "Jalal" };

	/**
	 * Name of the days of the month.
	 */
	public static final String DAY_OF_MONTH_NAMES[] = {
		"Baha'", "Jalal", "Jamal", "`Azamat", "Nur", "Rahmat", "Kalimat", "Kamal", "Asma'", "`Izzat",
		"Mashiyyat", "`Ilm", "Qudrat", "Qawl", "Masa'il", "Sharaf", "Sultan", "Mulk", "`Ala'"
	};

	/**
	 * Name of the months.
	 */
	public static final String MONTH_NAMES[] = {
		"Ayyam-i-Ha", "Baha'", "Jalal", "Jamal", "`Azamat", "Nur", "Rahmat", "Kalimat", "Kamal", "Asma'", "`Izzat",
		"Mashiyyat", "`Ilm", "Qudrat", "Qawl", "Masa'il", "Sharaf", "Sultan", "Mulk", "`Ala'"
	};

	/**
	 * Name of the years.
	 */
	public static final String YEAR_NAMES[] = {
		"Alif", "Ba'", "Ab", "Dal", "Bab", "Vav", "Abad", "Jad", "Baha'", "Hubb", "Bahhaj", "Javab", "Ahad", "Vahhab",
		"Vidad", "Badi'", "Bahi", "Abha", "Vahid"
	};

	/**
	 * Haifa location.
	 */
	//public static final CityElement HAIFA = new CityElement("Haifa, Israel", 35, 32.82, 2, 0);

	/**
	 * Tehran location.
	 */
	public static final CityElement TEHRAN = new CityElement("Tehran, Iran", 51.43, 35.67, 3.5, 1353);

	/**
	 * Major.
	 */
	public long major;

	/**
	 * Cycle number.
	 */
	public int cycle;

	/**
	 * Constructs a Bahai date with a fixed date.
	 *
	 * @param fixedDate fixed date.
	 */
	public Bahai(final long fixedDate) {
		super(EPOCH, fixedDate);
	}

	/**
	 * Constructs a Bahai date with a Julian day.
	 *
	 * @param julianDay Julian day.
	 */
	public Bahai(final double julianDay) {
		super(EPOCH, julianDay);
	}

	/**
	 * Constructor using year, month, day.
	 *
	 * @param y Year.
	 * @param m Month.
	 * @param d Day.
	 */
	public Bahai(final int y, final int m, final int d) {
		super(EPOCH, y, m, d);

		yearFromFixed();
	}

	/**
	 * Constructor using major, cycle, year, month, day.
	 *
	 * @param mj Major.
	 * @param c Cycle.
	 * @param y Year.
	 * @param m Month.
	 * @param d Day.
	 */
	public Bahai(final long mj, final int c, final int y, final int m, final int d) {
		this((int) (((mj - 1) * 19) + c - 1) * 19 + y, m, d);
	}

	@Override
	long toYear(final long year) {
		return 1 + ((year - 1) % 19);
	}

	/**
	 * To fixed day..
	 *
	 * @param yr Year.
	 * @param month Month.
	 * @param day Day.
	 * @return Fixed day.
	 */
	@Override
	long toFixed(final long yr, final int month, final int day) {
		if (yr > 171) {
			return toFixedFuture(yr, month, day);
		}

		long y = yr - 1 + Gregorian.yearFromFixed(EPOCH);
		long monthOffset;

		if (month < 19) {
			monthOffset = 19 * (month - 1);
		}
		else {
			monthOffset = Gregorian.isLeapYear(y + 1) ? 347 : 346;
		}

		return new Gregorian(y, 3, 20).fixed + monthOffset + (long) day;
	}

	@Override
	long yearFromFixed() {
		long f = Gregorian.yearFromFixed(fixed);
		long e = Gregorian.yearFromFixed(EPOCH);

		long y = f - e - (this.fixed > new Gregorian(f, 3, 20).fixed ? 0 : 1);

		this.major = 1 + y / 361;
		this.cycle = 1 + (int) (y % 361) / 19;

		return 1 + (y % 19);
	}

	@Override
	int monthFromFixed(final long year) {
		long y = (((this.major - 1) * 19) + this.cycle - 1) * 19 + this.year;

		if (this.fixed >= toFixed(y, 19, 1)) {
			return 19;
		}
		else {
			int daysInYear = (int) (this.fixed - toFixed(y, 1, 1)) + 1;
			return 1 + (daysInYear / 19);
		}
	}

	@Override
	int dayFromFixed(final long year, final int month) {
		long y = (((this.major - 1) * 19) + this.cycle - 1) * 19 + this.year;
		return 1 + (int) ((fixed - toFixed(y, month, 1)) % 19);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName())
				.append(' ')
				.append(major)
				.append('/')
				.append(cycle)
				.append('/')
				.append(year)
				.append('/')
				.append(month)
				.append('/')
				.append(day);

		return builder.toString();
	}

	private long toFixedFuture(final long yr, final int month, final int day) {
		long s0;
		double s1;

		if (month < 19) {
			s0 = (month - 1) * 19 - 1;
			s1 = (double) yr - 0.5D;
		}
		else {
			s0 = -21;
			s1 = (double) yr + 0.5D;
		}

		return newYearOnOrBefore(Bahai.EPOCH + (long) Math.floor(Calendar.TROPICAL_YEAR * s1)) + s0 + (long) day;
	}

	/**
	 * New year before certain date.
	 *
	 * @param fixed Fixed day.
	 * @return Previous new year.
	 */
	private long newYearOnOrBefore(long fixed)
	{
		double sunsetTime = Calendar.estimatePriorSolarLongitude(sunsetInTehran(fixed), Calendar.SPRING);
		long l1 = (long) Math.floor(sunsetTime) - 1;

		while (Calendar.solarLongitude(sunsetInTehran(l1)) > Calendar.SPRING + 2.0) {
			l1++;
		}

		return l1;
	}

	/**
	 * Sunset in Haifa.
	 *
	 * @param fixed Fixed day.
	 * @return Sunset time.
	 */
	private double sunsetInTehran(final long fixed)
	{
		try {
			return Calendar.universalFromStandard(Calendar.sunset(fixed, TEHRAN), TEHRAN);
		} catch (Exception ex) {
			return 0.0;
		}
	}
}
