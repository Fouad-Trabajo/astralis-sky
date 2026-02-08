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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import jparsec.ephem.Functions;
import jparsec.ephem.RiseSetTransit;
import jparsec.math.Constant;
import jparsec.observer.City;
import jparsec.observer.Country.COUNTRY;
import jparsec.time.TimeElement.SCALE;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;

/**
 * A simple set of date and time formatters.
 *
 * @author M. Huss
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class TimeFormat
{
	// private constructor so that this class cannot be instantiated.
	private TimeFormat() {}

	/**
	 * Return 'now' as a date String using the default Locale.
	 *
	 * @param dateFmt The java.text.DateFormat constant to use (FULL,
	 *        LONG, MEDIUM, or SHORT).
	 * @return The current, formatted, date as a String.
	 */
	public static String dateNow(int dateFmt)
	{
		return DateFormat.getDateInstance(dateFmt).format(new Date());
	}

	/**
	 * Return 'now' as a date String using the default Locale and the
	 * DateFormat.MEDIUM size.
	 *
	 * @return The current formatted date as a String.
	 */
	public static String dateNow()
	{
		return dateNow(DateFormat.MEDIUM);
	}

	/**
	 * Return 'now' as a date and time String using the default
	 * Locale and the DateFormat.MEDIUM size.
	 *
	 * @return The current, formatted, date and time as a String.
	 */
	public static String dateTimeNow()
	{
		return DateFormat.getInstance().format(new Date());
	}

	/**
	 * Convert a Date into a date String using the default
	 * Locale.
	 *
	 * @param d The java.util.Date to convert.
	 * @param dateFmt The java.text.DateFormat constant to use (FULL,
	 *        LONG, MEDIUM, or SHORT).
	 * @return The date formatted as a String.
	 */
	public static String date(Date d, int dateFmt)
	{
		return DateFormat.getDateInstance(dateFmt).format(d);
	}

	/**
	 * Convert a Date into a date String using the default
	 * Locale and the DateFormat.MEDIUM size.
	 *
	 * @param d The java.util.Date to convert.
	 * @return The date formatted as a String.
	 */
	public static String date(Date d)
	{
		return date(d, DateFormat.MEDIUM);
	}

	/**
	 * Convert a Date into a time String using the default
	 * Locale.
	 *
	 * @param d The java.util.Date to convert.
	 * @param dateFmt The java.text.DateFormat constant to use (FULL,
	 *        LONG, MEDIUM, or SHORT).
	 * @return The time formatted as a String.
	 */
	public static String time(Date d, int dateFmt)
	{
		return DateFormat.getTimeInstance(dateFmt).format(d);
	}

	/**
	 * Convert a Date into a time String using the default
	 * Locale and the DateFormat.MEDIUM size.
	 *
	 * @param d The java.util.Date to convert.
	 * @return The time formatted as a String.
	 */
	public static String time(Date d)
	{
		return time(d, DateFormat.MEDIUM);
	}

	/**
	 * Convert a Date into a date and time String using
	 * the default Locale.
	 *
	 * @param d The java.util.Date to convert.
	 * @param dtFmt The java.text.DateFormat constant to use (FULL,
	 *        LONG, MEDIUM, or SHORT).
	 * @return The date and time formatted as a String.
	 */
	public static String dateTime(Date d, int dtFmt)
	{
		long t = d.getTime();
		String ms = ""+t;
		ms = ms.substring(ms.length()-3);
		if (Integer.parseInt(ms) > 500) // round up second
			d = new Date(t+1001-Integer.parseInt(ms));

		String f = "";
		if (dtFmt == DateFormat.MEDIUM) {
			SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
			f = format.format(d);
		} else {
			f = DateFormat.getDateTimeInstance(dtFmt, dtFmt).format(d);
		}
		return f;
	}

	/**
	 * Convert a Date into a date and time String using
	 * the default Locale.
	 *
	 * @param d The java.util.Date to convert.
	 * @param dtFmt The java.text.DateFormat constant to use (FULL,
	 *        LONG, MEDIUM, or SHORT).
	 * @param locale The Locale to use.
	 * @return The date and time formatted as a String.
	 */
	public static String dateTime(Date d, int dtFmt, Locale locale)
	{
		long t = d.getTime();
		String ms = ""+t;
		ms = ms.substring(ms.length()-3);
		if (Integer.parseInt(ms) > 500) // round up second
			d = new Date(t+1001-Integer.parseInt(ms));

		String f = "";
		if (dtFmt == DateFormat.MEDIUM) {
			SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", locale);
			f = format.format(d);
		} else {
			f = DateFormat.getDateTimeInstance(dtFmt, dtFmt).format(d);
		}
		return f;
	}

	/**
	 * Convert a Date into a date and time String using
	 * the default Locale and the DateFormat.MEDIUM size.
	 *
	 * @param d The java.util.Date to convert.
	 * @return The date and time formatted as a String.
	 */
	public static String dateTime(Date d)
	{
		return dateTime(d, DateFormat.MEDIUM);
	}

	/**
	 * Convert a Calendar into a date and time String
	 * using the default Locale and the DateFormat.MEDIUM size.
	 *
	 * @param c The java.util.Calendar to convert.
	 * @return The date and time formatted as a String.
	 */
	public static String dateTime(Calendar c)
	{
		String date = dateTime(c.getTime(), DateFormat.MEDIUM);
		if (c.get(GregorianCalendar.ERA) == GregorianCalendar.BC) {
			date += " ("+Translate.translate(1068)+")";
		}
		return date;
	}

	/**
	 * Converts a julian day into a date and time String.
	 *
	 * @param jd The julian day. Must be greater than 0.
	 * @param scale The time scale to add its abbreviation at the end, or null to avoid it.
	 * @return The date and time formatted as a String.
	 * @throws JPARSECException If the Julian day is invalid.
	 */
	public static String formatJulianDayAsDateAndTime(double jd, SCALE scale) throws JPARSECException
	{
		if (jd == RiseSetTransit.CIRCUMPOLAR)
			return Translate.translate(818);
		if (jd == RiseSetTransit.ALWAYS_BELOW_HORIZON)
			return Translate.translate(820);
		if (jd == RiseSetTransit.NO_RISE_SET_TRANSIT)
			return Translate.translate(1118);
		//if (jd <= 0.0) return Translate.translate(819); //"UNKNOWN");

		TimeElement time = new TimeElement(jd, scale);
		boolean dmy = false, monthAsString = false, addTime = true;
		String format = getFormattedDate(time, dmy, monthAsString, addTime);

		if (scale != null) format += " "+ TimeElement.getTimeScaleAbbreviation(scale);
		return format;
	}

	/**
	 * Converts a julian day into a date and time String.
	 *
	 * @param time Time object.
	 * @param dmy True to format as day-month-year, false for year-month-day.
	 * @param monthAsString True to write the month as a string.
	 * @return The date and time formatted as a String.
	 * @throws JPARSECException If the Julian day is invalid.
	 */
	public static String formatJulianDayAsDateAndTime(TimeElement time, boolean dmy, boolean monthAsString) throws JPARSECException
	{
		double jd = time.astroDate.jd();
		if (jd == RiseSetTransit.CIRCUMPOLAR)
			return Translate.translate(818);
		if (jd == RiseSetTransit.ALWAYS_BELOW_HORIZON)
			return Translate.translate(820);
		if (jd == RiseSetTransit.NO_RISE_SET_TRANSIT)
			return Translate.translate(1118);
		//if (jd <= 0.0) return Translate.translate(819); //"UNKNOWN");

		String format = getFormattedDate(time, dmy, monthAsString, true);

		if (time.timeScale != null) format += " "+ TimeElement.getTimeScaleAbbreviation(time.timeScale);
		return format;
	}

	private static String getFormattedDate(TimeElement time, boolean dmy, boolean monthAsString, boolean addTime) throws JPARSECException {
		try
		{
			String format = "";

			AstroDate astro = time.astroDate;
			String month = Functions.formatValue(astro.getMonth(), 0, 2, false);
			if (monthAsString) month = astro.getMonthName().toLowerCase().substring(0, 3);
			if (dmy) {
				format = Functions.formatValue(astro.getDay(), 0, 2, false)+"-"+month+"-"+Functions.formatValue(Math.abs(astro.getYear()), 0, 4, false)+" ";
			} else {
				format = Functions.formatValue(Math.abs(astro.getYear()), 0, 4, false)+"-"+month+"-"+Functions.formatValue(astro.getDay(), 0, 2, false)+" ";
			}
			if (addTime) {
				if (time.decimalsInSeconds == 0) {
					astro = new AstroDate(astro.jd() + 0.5 / Constant.SECONDS_PER_DAY); // round up second

					month = Functions.formatValue(astro.getMonth(), 0, 2, false);
					if (monthAsString) month = astro.getMonthName().toLowerCase().substring(0, 3);
					if (dmy) {
						format = Functions.formatValue(astro.getDay(), 0, 2, false)+"-"+month+"-"+Functions.formatValue(Math.abs(astro.getYear()), 0, 4, false)+" ";
					} else {
						format = Functions.formatValue(Math.abs(astro.getYear()), 0, 4, false)+"-"+month+"-"+Functions.formatValue(astro.getDay(), 0, 2, false)+" ";
					}
				}
				format += Functions.fmt(astro.getHour(), 2, ':');
				double sec = astro.getSeconds();
				if (time.decimalsInSeconds == 0) sec = (int) sec;
				format += Functions.fmt(astro.getMinute(), 2, ':') + Functions.formatValue(sec, time.decimalsInSeconds, 2, false);
			}
			if (astro.getYear() < 0) format = format.trim() + " "+Translate.translate(1068);
			return format;
		} catch (Exception ve)
		{
			throw new JPARSECException(ve);
		}
	}

	/**
	 * Converts a julian day into a date and time String.
	 *
	 * @param jd The julian day.
	 * @param scale The time scale to add its abbreviation at the end, or null to avoid it.
	 * @return The date and time formatted as a String.
	 * @throws JPARSECException If the Julian day is invalid.
	 */
	public static String formatJulianDayAsDateAndTimeOnlyMinutes(double jd, SCALE scale) throws JPARSECException
	{
		String t = TimeFormat.formatJulianDayAsDateAndTime(jd, scale);
		int d = t.indexOf(":");
		if (d < 0) return t;
		String bc = "", t1068 = Translate.translate(1068);
		if (t.indexOf(t1068) > 0) bc = " "+t1068;
		t = t.substring(0, d);
		t = t.substring(0, t.lastIndexOf(" "));

		// Add rounded time
		AstroDate astro = new AstroDate(jd);
		t += " " + DateTimeOps.twoDigits(astro.getHour()) + ":" + DateTimeOps.twoDigits(astro.getRoundedMinute());
		
		t += bc;
		if (scale != null) t += " "+ TimeElement.getTimeScaleAbbreviation(scale);
		return t;
	}

	/**
	 * Converts a julian day into a date and time String.
	 *
	 * @param time Time object.
	 * @param dmy True to format as day-month-year, false for year-month-day.
	 * @param monthAsString True to write the month as a string.
	 * @return The date and time formatted as a String.
	 * @throws JPARSECException If the Julian day is invalid.
	 */
	public static String formatJulianDayAsDateAndTimeOnlyMinutes(TimeElement time, boolean dmy, boolean monthAsString) throws JPARSECException
	{
		String t = TimeFormat.formatJulianDayAsDateAndTime(time, dmy, monthAsString);
		int d = t.indexOf(":");
		if (d < 0) return t;
		String bc = "", t1068 = Translate.translate(1068);
		if (t.indexOf(t1068) > 0) bc = " "+t1068;
		t = t.substring(0, d);
		t = t.substring(0, t.lastIndexOf(" "));

		// Add rounded time
		t += " " + DateTimeOps.twoDigits(time.astroDate.getHour()) + ":" + DateTimeOps.twoDigits(time.astroDate.getRoundedMinute());
		
		t += bc;
		if (time.timeScale != null) t += " "+ TimeElement.getTimeScaleAbbreviation(time.timeScale);
		return t;
	}

	/**
	 * Converts a julian day into a date String.
	 *
	 * @param jd The julian day.
	 * @return The date formatted as a String.
	 * @throws JPARSECException If the Julian day is invalid.
	 */
	public static String formatJulianDayAsDate(double jd) throws JPARSECException
	{
		if (jd == RiseSetTransit.CIRCUMPOLAR)
			return Translate.translate(818);
		if (jd == RiseSetTransit.ALWAYS_BELOW_HORIZON)
			return Translate.translate(820);
		if (jd == RiseSetTransit.NO_RISE_SET_TRANSIT)
			return Translate.translate(1118);
		//if (jd <= 0.0) return Translate.translate(819); //"UNKNOWN");

		TimeElement time = new TimeElement(jd, SCALE.LOCAL_TIME);
		boolean dmy = false, monthAsString = false, addTime = false;
		String format = getFormattedDate(time, dmy, monthAsString, addTime);

		return format;
	}

	/**
	 * Converts a {@linkplain TimeElement} object into a date and time String.
	 *
	 * @param time {@linkplain TimeElement} object.
	 * @return The date and time formatted as a String.
	 * @throws JPARSECException  If the Julian day is invalid.
	 */
	public static String formatTime(TimeElement time) throws JPARSECException
	{
		boolean dmy = false, monthAsString = false, addTime = true;
		String date = getFormattedDate(time, dmy, monthAsString, addTime);
		date += " " + getTimeScale(time);
		return date;
	}

	/**
	 * Returns the time scale abbreviation.
	 *
	 * @param time {@linkplain TimeElement} object.
	 * @return The time scale abbreviation.
	 */
	public static String getTimeScale(TimeElement time)
	{
		String scale = "";
		switch (time.timeScale)
		{
		case LOCAL_TIME:
			scale += "LT";
			break;
		case UNIVERSAL_TIME_UT1:
			scale += "UT";
			break;
		case UNIVERSAL_TIME_UTC:
			scale += "UTC";
			break;
		case TERRESTRIAL_TIME:
			scale += "TT";
			break;
		case BARYCENTRIC_DYNAMICAL_TIME:
			scale += "TDB";
			break;
		}
		return scale;
	}
	/**
	 * Returns the Locale for a given city.
	 * @param city The city name.
	 * @return The Locale, or null if no Locale is available.
	 * @throws JPARSECException If an error occurs.
	 */
	public static Locale getLocale(String city)
	throws JPARSECException {
		Locale locale = null;

		COUNTRY countryID = City.findCountry(city);
		switch (countryID)
		{
		case Canada:
			locale = Locale.CANADA;
			break;
		case China:
			locale = Locale.CHINA;
			break;
		case France:
			locale = Locale.FRANCE;
			break;
		case Germany:
			locale = Locale.GERMANY;
			break;
		case Italy:
			locale = Locale.ITALY;
			break;
		case Japan:
			locale = Locale.JAPAN;
			break;
		case Korea_of_North:
		case Korea_of_South:
			locale = Locale.KOREA;
			break;
		case Taiwan:
			locale = Locale.TAIWAN;
			break;
		default:
			break;
		}
		return locale;
	}
}
