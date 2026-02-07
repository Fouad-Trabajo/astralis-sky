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
package jparsec.time;


import java.io.Serializable;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import jparsec.ephem.Functions;
import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.math.Constant;
import jparsec.observer.ObserverElement;
import jparsec.util.Configuration;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;
import jparsec.util.Translate.LANGUAGE;

/**
 * A class to store date/time values with a precision of one second or better. Support to
 * retrieve the Julian day number is given for the Gregorian and Julian calendars.
 * <P>
 * Selectable dates are not limited in years, but there are some invalid dates (non
 * existent) in the civil calendar, between October 5, 1582 and October 14, 1582.
 * <P>
 * Note: This library uses GregorianCalendar for dates equal or after October,
 * 15, 1582. For dates before that the library uses the Julian Calendar. The
 * Before Christ era (B.C.) is automatically selected by setting a zero or
 * negative year as the year in the GregorianCalendar instance. Year 0
 * corresponds to year 1 B.C. (year 0 does not exist by itself). When using an
 * instance of {@linkplain AstroDate} occurs the same. The only difference is that here month
 * '1' is January. In GregorianCalendar January is month '0'. To avoid
 * confusion it is recommended to use always {@linkplain AstroDate} as the instance for
 * dates, as well as the provided symbolic constants for the months.
 * <P>
 * In the constructors the year is entered considering that year 0 does not exist, so year
 * -1 is 1 B.C. When retrieving the year the value returned follows the same rule. But
 * internally, in the instance, the year is hold in the astronomical way, where 1 B.C. is
 * year 0.
 * <P>
 * Date/time can be specified either in the Gregorian Calendar or the Julian
 * Calendar for any instant using {@linkplain DateTimeOps}.
 *
 * @see java.util.GregorianCalendar
 * @see DateTimeOps
 * @author T. Alonso Albi - OAN (Spain)
 * @author M. Huss
 * @version 1.0
 */
public class AstroDate implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor for current instant.
	 * Precision is 1 ms.
	 */
	public AstroDate()
	{
		this(new GregorianCalendar());
	}

	/**
	 * Literal (member by member) constructor.
	 * @param year Year. Should not be zero.
	 * @param month Month of the year (1..12).
	 * @param day Day of the month (1...31).
	 * @param seconds Time in seconds past midnight. This should be in the range
	 *        from 0 to {@linkplain Constant#SECONDS_PER_DAY}.
	 */
	private AstroDate(int year, int month, int day, double seconds)
	{
		if (year == 0)
			try {
				JPARSECException.addWarning("Year should never be 0. Assumed to be -1 = 1 b.C.");
			} catch (Exception exc) {
				if (JPARSECException.isTreatWarningsAsErrors()) throw new RuntimeException("Year should never be 0.");
			}

		if (year < 0) year++;
		this.day = day;
		this.month = month;
		this.year = year;
		this.second = seconds;
	}

	/**
	 * Explicit day, month, year, hour, minute, and second constructor.
	 * @param year Year. Should not be zero.
	 * @param month Month of the year (1..12).
	 * @param day Day of the month (1...31).
	 * @param hour Hour of the day (0...23).
	 * @param min Minute of the hour (0...59).
	 * @param sec Second of the minute (0...59).
	 */
	public AstroDate(int year, int month, int day, int hour, int min, double sec)
	{
		if (year == 0)
			try {
				JPARSECException.addWarning("Year should never be 0. Assumed to be -1 = 1 b.C.");
			} catch (Exception exc) {
				if (JPARSECException.isTreatWarningsAsErrors()) throw new RuntimeException("Year should never be 0.");
			}

		if (year < 0) year++;
		this.day = day;
		this.month = month;
		this.year = year;
		this.second = (hour * Constant.SECONDS_PER_HOUR + min * Constant.SECONDS_PER_MINUTE + sec);
	}

	/**
	 * Year, month, day constructor (time defaults to 00:00:00 = midnight).
	 * @param year Year. Should not be zero.
	 * @param month Month of the year (1..12).
	 * @param day Day of the month (1...31).
	 */
	public AstroDate(int year, int month, int day)
	{
		if (year == 0)
			try {
				JPARSECException.addWarning("Year should never be 0. Assumed to be -1 = 1 b.C.");
			} catch (Exception exc) {
				if (JPARSECException.isTreatWarningsAsErrors()) throw new RuntimeException("Year should never be 0.");
			}

		if (year < 0) year++;
		this.day = day;
		this.month = month;
		this.year = year;
		this.second = 0;
	}

	/**
	 * Creates an instance from a date expressed in a common way,
	 * like the standardized format Fri, 29 Jul 2011 23:30:44 +0200,
	 * a simple way like 2001 Jan 1 or Feb 17 2005 10:00:00, MJD50000.125,
	 * and so on.<P>
	 * The date is parsed by means of the cds package.
	 * @param cdsDate Date.
	 * @throws JPARSECException If an error occurs.
	 */
	public AstroDate(String cdsDate)
	throws JPARSECException {
		
		// Attempt to read using the standardized format: Fri, 29 Jul 2011 23:30:44 +0200
		try {
			String date = cdsDate;
			if (date.indexOf(",") > 0) date = date.substring(date.indexOf(",")+1).trim();
			date = FileIO.getTextBeforeField(5, date, " ", true);
			day = Integer.parseInt(FileIO.getField(1, date, " ", true));
			String mo = FileIO.getField(2, date, " ", true);
			month = -1;
			for (int i=1; i<= 12; i++) {
				if (mo.equals(Translate.translate(40 + i))) {
					month = i;
					break;
				}
			}
			if (month == -1) {
				if (Translate.getDefaultLanguage() != LANGUAGE.ENGLISH) {
					for (int i=1; i<= 12; i++) {
						if (mo.equals(Translate.getEntry(40 + i, LANGUAGE.ENGLISH))) {
							month = i;
							break;
						}
					}
				}
				if (month == -1) throw new JPARSECException("Cannot recognize month '"+mo+"'.");
			}
			year = Integer.parseInt(FileIO.getField(3, date, " ", true));
			String time = FileIO.getField(4, date, " ", true);
			int h = Integer.parseInt(FileIO.getField(1, time, ":", false));
			int m = Integer.parseInt(FileIO.getField(2, time, ":", false));
			int s = 0;
			if (FileIO.getNumberOfFields(time, ":", false) > 2) s = Integer.parseInt(FileIO.getField(3, time, ":", false));
			second = s + m * Constant.SECONDS_PER_MINUTE + h * Constant.SECONDS_PER_HOUR;

			if (year == 0) JPARSECException.addWarning("Year should never be 0. Assumed to be -1 = 1 b.C.");
			if (year < 0) year ++;
			return;
		} catch (Exception exc) {}
		
		// Attempt to read using the long full format: September 5, 2012 [, 00:00:00.000]
		try {
			if (cdsDate.indexOf(",") > 0) {
				String f[] = DataSet.toStringArray(cdsDate, ",", false);
				int mo = 1 + DataSet.getIndex(MONTH_NAMES, FileIO.getField(1, f[0], " ", true).trim());
				if (mo < 1) throw new Exception("Maybe Spanish ?");
				int da = Integer.parseInt(FileIO.getField(2, f[0], " ", true).trim());
				int yr = Integer.parseInt(f[1].trim());
				int hr = 0, mn = 0;
				double se = 0;
				if (f.length > 2) {
					String f2[] = DataSet.toStringArray(f[2], ":", false);
					hr = Integer.parseInt(f2[0].trim());
					mn = Integer.parseInt(f2[1].trim());
					se = Double.parseDouble(f2[2].trim());
				}
				year = yr;
				month = mo;
				day = da;
				second = se + mn * Constant.SECONDS_PER_MINUTE + hr * Constant.SECONDS_PER_HOUR;
				return;
			}
		} catch (Exception exc) {
			try {
				//if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) {
					String f[] = DataSet.toStringArray(cdsDate, ",", false);
					String sm = FileIO.getField(3, f[0], " ", true).trim();
					int mo = -1;
					for (int m=0;m<MONTH_NAMES.length;m++) {
						String ms = Translate.translate(MONTH_NAMES[m], LANGUAGE.ENGLISH, LANGUAGE.SPANISH);
						if (ms.toLowerCase().equals(sm.toLowerCase())) {
							mo = 1 + m;
							return;
						}
					}
					if (sm.toLowerCase().equals("mayo")) mo = 5;
					if (mo > 0) {
						int da = Integer.parseInt(FileIO.getField(1, f[0], " ", true).trim());
						int yr = Integer.parseInt(FileIO.getField(5, f[0], " ", true).trim());
						int hr = 0, mn = 0;
						double se = 0;
						if (f.length > 1) {
							String f2[] = DataSet.toStringArray(f[1], ":", false);
							hr = Integer.parseInt(f2[0].trim());
							mn = Integer.parseInt(f2[1].trim());
							se = Double.parseDouble(f2[2].trim());
						}
						year = yr;
						month = mo;
						day = da;
						second = se + mn * Constant.SECONDS_PER_MINUTE + hr * Constant.SECONDS_PER_HOUR;
						return;
					}
				//}
			} catch (Exception exc1) {
				exc1.printStackTrace();
				throw new JPARSECException(exc1);
			}
			exc.printStackTrace(); 
		}
		
		// Attempt to read using CDS for other formats
		try {
			Class<?> c = Class.forName("cds.astro.Astrotime");
			Object t = c.newInstance();
			Method m = c.getMethod("set", String.class);
			m.invoke(t, cdsDate);
			m = c.getMethod("getJD");
			Object o = m.invoke(t);

			AstroDate astro = new AstroDate((Double) o);
			this.year = astro.year;
			this.month = astro.month;
			this.day = astro.day;
			this.second = astro.second;

			if (year == 0) JPARSECException.addWarning("Year should never be 0. Assumed to be -1 = 1 b.C.");
			if (year < 0) year ++;
		} catch (Exception e)
		{
			throw new JPARSECException("CDS library is not in classpath, or could not understand "+cdsDate+" as a date.", e);
		}
	}

	/**
	 * Day (with decimals), Month, Year constructor (time defaults to 00:00:00 =
	 * midnight).
	 * @param year Year.
	 * @param month Month of the year (1..12).
	 * @param day Day of the month (1...31) and fraction of days.
	 */
	public AstroDate(int year, int month, double day)
	{
		if (year == 0)
			try {
				JPARSECException.addWarning("Year should never be 0. Assumed to be -1 = 1 b.C.");
			} catch (Exception exc) {
				if (JPARSECException.isTreatWarningsAsErrors()) throw new RuntimeException("Year should never be 0.");
			}

		if (year < 0) year++;
		this.day = (int) day;
		this.month = month;
		this.year = year;
		this.second = ((day - (int) day) * Constant.SECONDS_PER_DAY);
	}

	/**
	 * Convert a GregorianCalendar instance to an {@linkplain AstroDate}.<BR>
	 *
	 * @param cal An instance of java.util.GregorianCalendar.
	 * @deprecated Dates cannot be represented in a {@linkplain GregorianCalendar}
	 * with a precision better than 1 ms.
	 */
	@Deprecated
	public AstroDate(GregorianCalendar cal)
	{
		this.year = cal.get(GregorianCalendar.YEAR);
		this.month = cal.get(GregorianCalendar.MONTH) + 1;
		this.day = cal.get(GregorianCalendar.DATE);

		this.second = (cal.get(GregorianCalendar.HOUR_OF_DAY) * Constant.SECONDS_PER_HOUR + cal
				.get(GregorianCalendar.MINUTE) * Constant.SECONDS_PER_MINUTE + cal.get(GregorianCalendar.SECOND) + cal
				.get(GregorianCalendar.MILLISECOND) / 1000.0);

		if (cal.get(GregorianCalendar.ERA) == GregorianCalendar.BC)
		{
			this.year = -cal.get(GregorianCalendar.YEAR) + 1;
		}
	}

	/**
	 * Constructor for a date given as a long value, representing
	 * the number of milliseconds elapsed from 1970, January 1, at
	 * 0 h.
	 * @param t Time from 1970-1-1 in milliseconds.
	 * @throws JPARSECException If an error occurs.
	 */
	public AstroDate(long t) throws JPARSECException {
		AstroDate tmp = new AstroDate(1970, 1, 1, 0.0);
		double jd = tmp.jd() + t / (1000.0 * Constant.SECONDS_PER_DAY);
		tmp = new AstroDate(jd);

		this.year = tmp.year;
		this.month = tmp.month;
		this.day = tmp.day;
		this.second = tmp.second;
	}

	/**
	 * Julian Day constructor of a Gregorian {@linkplain AstroDate}. <BR>
	 *
	 * @param jd Julian day number.
	 */
	public AstroDate(double jd)
	{
		// The conversion formulas are from Meeus,
		// Chapter 7.
		double Z = Math.floor(jd + 0.5);
		double F = jd + 0.5 - Z;
		double A = Z;
		if (Z >= 2299161D)
		{
			int a = (int) ((Z - 1867216.25) / 36524.25);
			A += 1 + a - a / 4;
		}
		double B = A + 1524;
		int C = (int) ((B - 122.1) / 365.25);
		int D = (int) (C * 365.25);
		int E = (int) ((B - D) / 30.6001);

		double exactDay = F + B - D - (int) (30.6001 * E);
		day = (int) exactDay;
		month = (E < 14) ? E - 1 : E - 13;
		year = C - 4715;
		if (month > 2)
			year--;

		second = ((exactDay - day) * Constant.SECONDS_PER_DAY);
	}

	/**
	 * Julian Day constructor of a Gregorian {@linkplain AstroDate}.
	 *
	 * @param jd Julian day number as a big decimal. Internal precision
	 * of this class (the seconds from midnight are stored as a double)
	 * is around 0.5 nanoseconds or better (precision will be close to
	 * the femptosecond close to midnight, which is second 0).
	 */
	public AstroDate(BigDecimal jd)
	{
		// The conversion formulas are from Meeus,
		// Chapter 7.
		double Z = Math.floor(jd.doubleValue() + 0.5);
		BigDecimal F = jd.add(new BigDecimal(0.5 - Z));
		double A = Z;
		if (Z >= 2299161D)
		{
			int a = (int) ((Z - 1867216.25) / 36524.25);
			A += 1 + a - a / 4;
		}
		double B = A + 1524;
		int C = (int) ((B - 122.1) / 365.25);
		int D = (int) (C * 365.25);
		int E = (int) ((B - D) / 30.6001);

		BigDecimal exactDay = F.add(new BigDecimal(B - D - (int) (30.6001 * E)));
		day = exactDay.intValue();
		month = (E < 14) ? E - 1 : E - 13;
		year = C - 4715;
		if (month > 2)
			year--;

		second = (exactDay.subtract(new BigDecimal(day))).multiply(new BigDecimal(Constant.SECONDS_PER_DAY)).doubleValue();
	}

	/**
	 * Convert an {@linkplain AstroDate} to a Julian Day. See Meeus, Astronomical
	 * Algorithms, chapter 7.
	 *
	 * @param ad The date to convert.
	 * @param julian true = Julian calendar, else Gregorian.
	 * @return The Julian Day that corresponds to the specified {@linkplain AstroDate}.
	 */
	public static double jd(AstroDate ad, boolean julian)
	{
		// The conversion formulas are from Meeus, chapter 7.
		int D = ad.day;
		int M = ad.month;
		int Y = ad.year;
		if (M < 3)
		{
			Y--;
			M += 12;
		}
		int A = Y / 100;
		int B = julian ? 0 : 2 - A + A / 4;

		double dayFraction = ad.second / Constant.SECONDS_PER_DAY;

		double jd = dayFraction + (int) (365.25D * (Y + 4716)) + (int) (30.6001 * (M + 1)) + D + B - 1524.5;

		return jd;
	}

	/**
	 * Convert an {@linkplain AstroDate} to a Julian Day.
	 * <P>
	 * Assumes the {@linkplain AstroDate} is specified using the Gregorian
	 * calendar if the {@linkplain AstroDate} is October, 15, 1582 or later. The Julian
	 * calendar will be used for dates before October, 5, 1582. Intermediate
	 * dates will throw an error.
	 *
	 * @param ad The date to convert.
	 * @return The Julian Day that corresponds to the specified {@linkplain AstroDate}.
	 */
	public static double jd(AstroDate ad)
	{
		int D = ad.day;
		int M = ad.month;
		int Y = ad.year;

		boolean julian = false;

		if (Y < 1582) julian = true;
		if (Y == 1582 && M < 10) julian = true;
		if (Y == 1582 && M == 10 && D < 15) julian = true;

		double jd = jd(ad, julian);

		return jd;
	}

	/**
	 * Adds some days to the current date.
	 * @param days The days to add.
	 */
	public void add(double days) {
		if (days == 0) return;
		AstroDate astro = new AstroDate(this.jd() + days);
		this.year = astro.year;
		this.month = astro.month;
		this.day = astro.day;
		this.second = astro.second;
	}

	/**
	 * Convert this instance of {@linkplain AstroDate} to a Julian Day.
	 *
	 * @param julian true = Julian calendar, else Gregorian.
	 * @return The Julian Day that corresponds to this {@linkplain AstroDate}
	 *         instance.
	 */
	public double jd(boolean julian)
	{
		return jd(this, julian);
	}

	/**
	 * Convert an {@linkplain AstroDate} to a Julian Day. See Meeus, Astronomical
	 * Algorithms, chapter 7.
	 *
	 * @param ad The date to convert.
	 * @param julian true = Julian calendar, else Gregorian.
	 * @return The Julian Day that corresponds to the specified {@linkplain AstroDate}.
	 */
	public static BigDecimal exactJD(AstroDate ad, boolean julian)
	{
		// The conversion formulas are from Meeus, chapter 7.
		int D = ad.day;
		int M = ad.month;
		int Y = ad.year;
		if (M < 3)
		{
			Y--;
			M += 12;
		}
		int A = Y / 100;
		int B = julian ? 0 : 2 - A + A / 4;

		BigDecimal dayFraction = new BigDecimal(ad.second).divide(new BigDecimal(Constant.SECONDS_PER_DAY),
				Configuration.BIG_DECIMAL_PRECISION_DECIMAL_PLACES, Configuration.BIG_DECIMAL_PRECISION_ROUNDING_MODE);

		BigDecimal jd = dayFraction.add(new BigDecimal((int) (365.25D * (Y + 4716)) + (int) (30.6001 * (M + 1)) + D + B - 1524.5));

		return jd;
	}

	/**
	 * Convert an {@linkplain AstroDate} to a Julian Day.
	 * <P>
	 * Assumes the {@linkplain AstroDate} is specified using the Gregorian
	 * calendar if the {@linkplain AstroDate} is October, 15, 1582 or later. The Julian
	 * calendar will be used for dates before October, 5, 1582. Intermediate
	 * dates will throw an error.
	 *
	 * @param ad The date to convert.
	 * @return The Julian Day that corresponds to the specified {@linkplain AstroDate}.
	 */
	public static BigDecimal exactJD(AstroDate ad)
	{
		int D = ad.day;
		int M = ad.month;
		int Y = ad.year;

		boolean julian = false;

		if (Y < 1582) julian = true;
		if (Y == 1582 && M < 10) julian = true;
		if (Y == 1582 && M == 10 && D < 15) julian = true;

		return exactJD(ad, julian);
	}

	/**
	 * Convert this instance of {@linkplain AstroDate} to a Julian Day.
	 *
	 * @param julian true = Julian calendar, else Gregorian.
	 * @return The Julian Day that corresponds to this {@linkplain AstroDate}
	 *         instance.
	 */
	public BigDecimal exactJD(boolean julian)
	{
		return exactJD(this, julian);
	}

	/**
	 * Returns the number of milliseconds from January 1, 1970.
	 * @return Milliseconds from January 1, 1970.
	 * @throws JPARSECException If an error occurs.
	 */
	public long msFrom1970() throws JPARSECException {
		BigDecimal dt = (this.exactJD().subtract(new BigDecimal(2440587.5))).multiply(new BigDecimal(24.0 * 3600.0 * 1000.0));
		return dt.longValue();
	}

	/**
	 * Returns the number of nanoseconds from January 1, 1970.
	 * @return Nanoseconds from January 1, 1970.
	 * @throws JPARSECException If an error occurs.
	 */
	public long nsFrom1970() throws JPARSECException {
		BigDecimal dt = (this.exactJD().subtract(new BigDecimal(2440587.5))).multiply(new BigDecimal(24.0 * 3600.0 * 1000000000.0));
		return dt.longValue();
	}

	/**
	 * Convert this instance of {@linkplain AstroDate} to a Julian Day.
	 * <P>
	 * Assumes the {@linkplain AstroDate} is specified using the Gregorian
	 * calendar if the {@linkplain AstroDate} is October, 15, 1582 or later. The Julian
	 * calendar will be used for dates before October, 5, 1582. Intermediate
	 * dates will throw an error.
	 *
	 * @return The Julian Day that corresponds to this {@linkplain AstroDate}
	 *         instance.
	 */
	public double jd()
	{
		int D = this.day;
		int M = this.month;
		int Y = this.year;

		boolean julian = false;

		if (Y < 1582)
			julian = true;
		if (Y == 1582 && M < 10)
			julian = true;
		if (Y == 1582 && M == 10 && D < 15)
			julian = true;

		return jd(this, julian);
	}

	/**
	 * Convert this instance of {@linkplain AstroDate} to a Julian Day.
	 * <P>
	 * Assumes the {@linkplain AstroDate} is specified using the Gregorian
	 * calendar if the {@linkplain AstroDate} is October, 15, 1582 or later. The Julian
	 * calendar will be used for dates before October, 5, 1582. Intermediate
	 * dates will throw an error.
	 *
	 * @return The Julian Day that corresponds to this {@linkplain AstroDate}
	 *         instance.
	 */
	public BigDecimal exactJD()
	{
		int D = this.day;
		int M = this.month;
		int Y = this.year;

		boolean julian = false;

		if (Y < 1582)
			julian = true;
		if (Y == 1582 && M < 10)
			julian = true;
		if (Y == 1582 && M == 10 && D < 15)
			julian = true;

		return exactJD(this, julian);
	}

	/**
	 * Check if the {@linkplain AstroDate} instance contains an invalid date. A date is
	 * invalid between October 5, 1582 and October 14, 1582.
	 *
	 * @return true if the date is invalid, false otherwise.
	 */
	public boolean isInvalid()
	{
		int D = this.day;
		int M = this.month;
		int Y = this.year;

		boolean invalid = false;

		if (Y == 1582 && M == 10 && (D >= 5 && D < 15))
			invalid = true;

		return invalid;
	}

	/**
	 * Gets the year.
	 *
	 * @return The year part of this instance of {@linkplain AstroDate}.
	 */
	public int getYear()
	{
		int y = year;
		if (y <= 0) y--;
		return y;
	}

	/**
	 * Gets the year following astronomical criteria, where year 0 exists.
	 *
	 * @return The year part of this instance of {@linkplain AstroDate}.
	 */
	public int getAstronomicalYear()
	{
		int y = year;
		return y;
	}

	/**
	 * Sets the year.
	 *
	 * @param y The year part of this instance of {@linkplain AstroDate}.
	 */
	public void setYear(int y)
	{
		if (y < 0) y++;
		year = y;
	}

	/**
	 * Gets the month.
	 *
	 * @return The month part of this instance of {@linkplain AstroDate} (1..12).
	 */
	public int getMonth()
	{
		return month;
	}

	/**
	 * Sets the month.
	 *
	 * @param m The month part of this instance of {@linkplain AstroDate} (1..12)<BR>.
	 *        Value is not checked!
	 */
	public void setMonth(int m)
	{
		month = m;
	}

	/**
	 * Gets the day.
	 *
	 * @return The day part of this instance of {@linkplain AstroDate} (1..31).
	 */
	public int getDay()
	{
		return day;
	}

	/**
	 * Gets the day plus fraction of days.
	 *
	 * @return The day plus fraction of day part of this instance of {@linkplain AstroDate}
	 *         [1.0 ... 32.0).
	 */
	public double getDayPlusFraction()
	{
		return day + second / Constant.SECONDS_PER_DAY;
	}

	/**
	 * Sets the day.
	 *
	 * @param d The day part of this instance of {@linkplain AstroDate} (1..31).
	 *        Value is not checked!
	 */
	public void setDay(int d)
	{
		day = d;
	}

	/**
	 * Get the Hour. <BR>
	 * This function truncates, and does not round up to nearest hour. For
	 * example, this function will return '1' at all times from 01:00:00 to
	 * 01:59:59 inclusive.
	 *
	 * @return The hour of the day for this instance of {@linkplain AstroDate},
	 *         not rounded.
	 */
	public int getHour()
	{
		double hour = 24.0 * this.second / Constant.SECONDS_PER_DAY;
		return (int) hour;
	}
	/**
	 * Get the minute. <BR>
	 * This function truncates, and does not round up to nearest minute. For
	 * example, this function will return 20 at all times from 1:20:00 to
	 * 1:20:59 inclusive.
	 *
	 * @return The minute of the hour for this instance of {@linkplain AstroDate},
	 *         not rounded.
	 */
	public int getMinute()
	{
		double hour = 24.0 * this.second / Constant.SECONDS_PER_DAY;
		double min = (hour - (int) hour) * 60.0;
		return (int) min;
	}
	/**
	 * Returns the seconds (elapsed from last minute) for this instance.
	 * @return Seconds.
	 */
	public double getSeconds()
	{
		return (second - (getHour() * Constant.SECONDS_PER_HOUR) - (getMinute() * Constant.SECONDS_PER_MINUTE));
	}

	/**
	 * Get the rounded hour. <BR>
	 * Returns the hour of the day rounded to nearest hour. For example, this
	 * function will return '1' at times 01:00:00 to 01:29:59, and '2' at times
	 * 01:30:00 to 01:59:59.
	 *
	 * @return The hour of the day for this instance of {@linkplain AstroDate},
	 *         rounded to the nearest hour.
	 */
	public int getRoundedHour()
	{
		return (int) ((second / Constant.SECONDS_PER_HOUR) + Constant.ROUND_UP);
	}

	/**
	 * Get the rounded minute. <BR>
	 * Returns the minute of the hour for this instance of {@linkplain AstroDate},
	 * rounded to nearest minute. For example, this function will return 20 at
	 * times 1:20:00 to 1:20:29, and 21 at times 1:20:30 to 1:20:59.
	 *
	 * @return The minute of the hour for this instance of {@linkplain AstroDate},
	 *         rounded to the nearest minute.
	 */
	public int getRoundedMinute()
	{
		return (int) (((second - (getHour() * Constant.SECONDS_PER_HOUR)) / Constant.SECONDS_PER_MINUTE) + Constant.ROUND_UP);
	}

	/**
	 * Get the rounded second. <BR>
	 *
	 * @return The second of the day for this instance of {@linkplain AstroDate},
	 *         rounded to the nearest second.
	 */
	public int getRoundedSecond()
	{
		return (int) (getSeconds() + Constant.ROUND_UP);
	}

	/**
	 * Sets the fraction of day.
	 * @param f Fraction of day.
	 */
	public void setDayFraction(double f)
	{
		second = f * Constant.SECONDS_PER_DAY;
	}

	/**
	 * Sets the fraction of day.
	 * @param f Fraction of day.
	 */
	public void setDayFraction(BigDecimal f)
	{
		second = f.multiply(new BigDecimal(Constant.SECONDS_PER_DAY)).doubleValue();
	}

	/**
	 * Returns the maximum number of days in the month of this instance.
	 * @return Days in the month.
	 */
	public int getDaysInMonth() {
		return DateTimeOps.getDaysInMonth(this.year, this.month);
	}

	/**
	 * Returns the fraction of day elapsed from previous midnight.
	 * @param jd Julian day.
	 * @return Day fraction.
	 */
	public static double getDayFraction(double jd)
	{
		double frac = jd - (int) jd + 0.5;
		if (frac > 1.0) frac = frac - 1.0;
		return frac;
	}

	/**
	 * Convert this {@linkplain AstroDate} instance to a GregorianCalendar.
	 *
	 * @return An instance of java.util.GregorianCalendar built using
	 *         this instance of {@linkplain AstroDate}.
	 */
	public GregorianCalendar toGCalendar()
	{
		int myyear = year;
		int era = GregorianCalendar.AD;
		if (myyear <= 0)
		{
			myyear = -(myyear - 1);
			era = GregorianCalendar.BC;
		}

		GregorianCalendar cal = new GregorianCalendar(myyear, month - 1, day, getHour(), getMinute(), (int) getSeconds());
		cal.set(GregorianCalendar.ERA, era);

		// Account for second fractions
		long time_in_ms = cal.getTimeInMillis();
		long new_time_in_ms = (long) (time_in_ms + (second - (int) second) * 1000.0);
		cal.setTimeInMillis(new_time_in_ms);
		return cal;
	}

	/**
	 * Convert this {@linkplain AstroDate} instance to a String,
	 * formatted to the minute. This function rounds the exact time to the
	 * nearest minute.
	 * <P>
	 * The format of the returned string is YYYY-MM-DD hh:mm.
	 *
	 * @return A formatted date/time String.
	 */
	public String toMinString()
	{
		AstroDate astro = new AstroDate(this.jd() + 0.5 / 1440.0); // round up minute
		return ""+astro.getYear() + "-" + Functions.fmt(astro.month, 2, '-') + Functions.fmt(astro.day, 2, ' ') + Functions.fmt(astro.getHour(), 2, ':') + Functions.fmt(astro.getMinute(), 2);
	}

	/**
	 * Convert this {@linkplain AstroDate} instance to a String,
	 * formatted like 'September 5, 2012 [, 00:00]'.
	 *
	 * @param showTime true to show the time.
	 * @return A formatted date/time String.
	 * @throws JPARSECException If the calendar month is invalid.
	 */
	public String toStringDate(boolean showTime) throws JPARSECException
	{
		if (!showTime) return toStringDate();
		AstroDate astro = new AstroDate(this.jd() + 0.5 / 1440.0); // round up minute
		String date = astro.toStringDate();
		if (showTime) date += ", "+Functions.fmt(astro.getHour(), 2, ':') + Functions.fmt(astro.getMinute(), 2);
		return date;
	}
	
	/**
	 * Convert this {@linkplain AstroDate} instance to a String,
	 * formatted like 'September 5, 2012 [, 00:00:00.000]'.
	 *
	 * @param showTime true to show the time.
	 * @param nsec Decimal places for the seconds.
	 * @return A formatted date/time String.
	 * @throws JPARSECException If the calendar month is invalid.
	 */
	public String toStringDateWithSeconds(boolean showTime, int nsec) throws JPARSECException
	{
		if (!showTime) return toStringDate();
		String date = toStringDate();
		if (showTime) date += ", "+Functions.fmt(getHour(), 2, ':') + Functions.fmt(getMinute(), 2, ':') + 
				Functions.formatValue(getSeconds(), nsec, 2, false);
		return date;
	}

	private String toStringDate() throws JPARSECException {
		String date = "";
		if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) {
			date = ""+day+" de "+getMonthName()+" de "+this.getYear();
		} else {
			date = getMonthName()+" "+day+", "+this.getYear();
		}
		return date;
	}

	/**
	 * Convert this {@linkplain AstroDate} instance to a String formatted to the
	 * minute, with Time Zone indicator. <BR>
	 * Rounds the time to the nearest minute.
	 * <P>
	 * The format of the returned string is YYYY-MM-DD hh:mm TZ,
	 * where TZ is a locale-specific timezone name (e.g., "EST").
	 *
	 * @return A formatted date/time String.
	 */
	public String toMinStringTZ()
	{
		TimeZone tz = TimeZone.getDefault();
		return toMinString() + ' ' + tz.getDisplayName(DateTimeOps.dstOffset(toGCalendar()) != 0, TimeZone.SHORT);
	}

	/**
	 * Convert <B>this</B> {@linkplain AstroDate} instance to a String.
	 * <P>
	 * The format of the returned string is YYYY-MM-DD hh:mm:ss.
	 * @return A formatted date/time String.
	 */
	@Override
	public String toString()
	{
		try {
			AstroDate astro = new AstroDate(this.jd() + Constant.ROUND_UP / Constant.SECONDS_PER_DAY);
			astro.second = (int) astro.second;
			return astro.getString(0);
		} catch (Exception e) {
			try {
				return this.getString(0);
			} catch (Exception e1) {	}
			return null; // should never happen
		}
	}
	/**
	 * Convert <B>this</B> {@linkplain AstroDate} instance to a String.
	 * <P>
	 * The format of the returned string is YYYY-MM-DD hh:mm:ss[.ssss].
	 * @param nsec Number of decimal places in the seconds field. Set to -1
	 * to show the time up to the rounded minute.
	 * @return A formatted date/time String.
	 */
	public String toString(int nsec)
	{
		return this.getString(nsec);
	}

	private String getString(int nsec)
	{
		String out = "";
		double jd = this.jd();
		if (nsec < 0 && jd > 0) {
			AstroDate astro = new AstroDate(jd + 0.5 / 1440.0); // round up minute
			int year = Math.abs(astro.getYear());
			out = ""+year+"-";
			if (year <= 9999) out = Functions.fmt(year, 4, '-');
			out += Functions.fmt(astro.month, 2, '-') + Functions.fmt(astro.day, 2, ' ') + Functions.fmt(astro.getHour(), 2, ':');
			out += Functions.fmt(astro.getMinute(), 2);
			if (astro.getYear() < 0) out += " "+Translate.translate(1068);
		} else {
			AstroDate astro = this;
			if (nsec == 0 && jd > 0) astro = new AstroDate(jd + 0.5 / Constant.SECONDS_PER_DAY); // round up second
			int year = Math.abs(astro.getYear());
			out = ""+year+"-";
			if (year <= 9999) out = Functions.fmt(year, 4, '-');
			out += Functions.fmt(astro.month, 2, '-') + Functions.fmt(astro.day, 2, ' ') + Functions.fmt(astro.getHour(), 2, ':');
			double sec = astro.getSeconds();
			if (nsec == 0) sec = (int) sec;
			out += Functions.fmt(astro.getMinute(), 2, ':') + Functions.formatValue(sec, nsec, 2, false);
			if (this.getYear() < 0) out += " "+Translate.translate(1068);
		}
		return out;
	}

	/**
	 * Returns current date in a standard format. For instance,
	 * Fri, 29 Jul 2011 23:30:44 +0200
	 * @param obs The observer object to obtain time zone.
	 * @return The formatted date.
	 * @throws JPARSECException If an error occurs.
	 */
	public String toStandarizedString(ObserverElement obs) throws JPARSECException {
		String m = Translate.translate(40 + this.getMonth());
		String week = getDayOfWeekName();
		week = week.substring(0, 3);
		String utc = "GMT";
		if (obs != null) {
			if (obs.getTimeZone() != 0.0) {
				utc = "+";
				if (obs.getTimeZone() < 0) utc = "-";
				double tz = Math.abs(obs.getTimeZone());
				utc += Functions.fmt((int) tz, 2);
				int p = (int) ((tz - (int) tz) * 60 + 0.5);
				if (p != 0) {
					utc += Functions.fmt(p, 2);
				} else {
					utc += "00";
				}
			}
		}
		String out = week+", "+this.getDay()+" "+m+" "+this.getYear()+" "+ Functions.fmt(getHour(), 2, ':') + Functions.fmt(getMinute(), 2, ':') +
				Functions.fmt((int) (getSeconds()+0.5), 2)+" "+utc;
		return out;
	}

	/**
	 * Convert <B>this</B> {@linkplain AstroDate} instance to a String,
	 * with Time Zone indicator.
	 * <P>
	 * The format of the returned string is YYYY-MM-DD hh:mm:ss TZ,
	 * where TZ is a locale-specific timezone name (e.g., "EST").
	 *
	 * @return A formatted date/time String.
	 */
	public String toStringTZ()
	{
		TimeZone tz = TimeZone.getDefault();
		return toString() + ' ' + tz.getDisplayName(DateTimeOps.dstOffset(toGCalendar()) != 0, TimeZone.SHORT);
	}

	/**
	 * Returns the month name.
	 * @return Month name.
	 */
	public String getMonthName() {
		if (month < 5) return DataSet.capitalize(Translate.translate(52 + month), false);
		if (month > 5) return DataSet.capitalize(Translate.translate(51 + month), false);
		String out = MONTH_NAMES[month-1];
		if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) out += "o";
		return out;
	}

	/**
	 * Returns the day of week name.
	 * @return Day of week name.
	 */
	public String getDayOfWeekName() {
		int n = ((int) (jd() + 0.5) % 7);
		if (n < 0) n += 7;
		if (n == 6) n -= 7;
		return Translate.translate(823 + n);
	}

	/**
	 * Returns the month name.
	 * @param month Month number, January is 1.
	 * @return Month name.
	 */
	public static String getMonthName(int month) {
		if (month < 5) return DataSet.capitalize(Translate.translate(52 + month), false);
		if (month > 5) return DataSet.capitalize(Translate.translate(51 + month), false);
		String out = MONTH_NAMES[month-1];
		if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) out += "o";
		return out;
	}

	/**
	 * Returns the day of week name.
	 * @param jd Julian day.
	 * @return Day of week name.
	 */
	public static String getDayOfWeekName(double jd) {
		int n = ((int) (jd + 0.5) % 7);
		if (n < 0) n += 7;
		if (n == 6) n -= 7;
		return Translate.translate(823 + n);
	}

	/**
	 * Names of the days of the week.
	 */
	public static final String DAY_OF_WEEK_NAMES[] =
		{ "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday" };

	/**
	 * Names of the months.
	 */
	public static final String MONTH_NAMES[] =
		{ "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November",
			"December" };

	/**
	 * Day of the month.
	 */
	private int day;

	/**
	 * Month of the year.
	 */
	private int month;

	/**
	 * Year.
	 */
	private int year;

	/**
	 * Seconds past midnight == day fraction.
	 */
	private double second;

	/**
	 * ID Constant for month January.
	 */
	public static final int JANUARY = 1;

	/**
	 * ID Constant for month February.
	 */
	public static final int FEBRUARY = 2;

	/**
	 * ID Constant for month March.
	 */
	public static final int MARCH = 3;

	/**
	 * ID Constant for month April.
	 */
	public static final int APRIL = 4;

	/**
	 * ID Constant for month May.
	 */
	public static final int MAY = 5;

	/**
	 * ID Constant for month June.
	 */
	public static final int JUNE = 6;

	/**
	 * ID Constant for month July.
	 */
	public static final int JULY = 7;

	/**
	 * ID Constant for month August.
	 */
	public static final int AUGUST = 8;

	/**
	 * ID Constant for month September.
	 */
	public static final int SEPTEMBER = 9;

	/**
	 * ID Constant for month October.
	 */
	public static final int OCTOBER = 10;

	/**
	 * ID Constant for month November.
	 */
	public static final int NOVEMBER = 11;

	/**
	 * ID Constant for month December.
	 */
	public static final int DECEMBER = 12;

	/**
	 * Returns if this instance is equals to another one.
	 * @param astro The other date.
	 * @return True if both dates are the same.
	 */
	@Override
	public boolean equals(Object astro) {
		if (this == astro) return true;
		if (!(astro instanceof AstroDate)) return false;

		AstroDate astroDate = (AstroDate) astro;

		if (day != astroDate.day) return false;
		if (month != astroDate.month) return false;
		if (year != astroDate.year) return false;

		return Double.compare(astroDate.second, second) == 0;
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		result = day;
		result = 31 * result + month;
		result = 31 * result + year;
		temp = Double.doubleToLongBits(second);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
	/**
	 * Clones this instance.
	 * @return A copy of this date.
	 */
	public AstroDate clone() {
		return new AstroDate(year, month, day, second);
	}
}
