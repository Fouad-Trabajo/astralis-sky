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
import java.math.BigDecimal;
import java.util.GregorianCalendar;

import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.time.calendar.Calendar;
import jparsec.time.calendar.Gregorian;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;
import jparsec.util.Translate.LANGUAGE;

/**
 * An adequate class for storing the time of an observer, defined as a Julian day ({@linkplain AstroDate}
 * instance) plus the identifier of the time scale of that date.
 * <P>
 * This library uses AstroDate class, with the Gregorian calendar for dates equal or after October,
 * 15, 1582. For dates before that the library uses the Julian Calendar. The
 * Before Christ era (B.C.) is automatically selected by setting a negative year as the year in
 * the AstroDate instance. Year 0 does not exists.
 * <P>
 * The time can be Barycentric Dynamical Time, Terrestrial Time, Universal Time
 * UT1/UTC, or Local Time. In the last
 * case the time will be corrected when making any kind of calculations using
 * the time zone field in the corresponding observer element instance. Beware
 * that for observatories this field should be set explicity, unless you perform
 * calculations without using local time.
 *
 * @see jparsec.observer.ObserverElement
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class TimeElement implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a default time object with the current computer's date
	 * and time scale set to local time.
	 */
	public TimeElement()
	{
		astroDate = new AstroDate();
		timeScale = SCALE.LOCAL_TIME;
	}

	/**
	 * Creates a time object by giving the values of the fields.
	 *
	 * @param obs_date Gregorian Calendar object with the date.
	 * @param ts Time scale ID constant.
	 * @deprecated Dates cannot be represented in a {@linkplain GregorianCalendar}
	 * with a precision better than 1 ms.
	 */
	@Deprecated
	public TimeElement(GregorianCalendar obs_date, SCALE ts)
	{
		astroDate = new AstroDate(obs_date);
		timeScale = ts;
	}

	/**
	 * Creates a time object from an {@linkplain AstroDate} instance.
	 *
	 * @param astro {@linkplain AstroDate} instance.
	 * @param ts Time scale ID constant.
	 */
	public TimeElement(AstroDate astro, SCALE ts)
	{
		astroDate = new AstroDate (astro.getYear(), astro.getMonth(), astro.getDayPlusFraction());
		timeScale = ts;
	}

	/**
	 * Creates a time object from a Julian day.
	 *
	 * @param jd Julian day.
	 * @param ts Time scale ID constant.
	 * @throws JPARSECException Thrown if the Julian day is invalid.
	 */
	public TimeElement(double jd, SCALE ts) throws JPARSECException
	{
		astroDate = new AstroDate(jd);
		timeScale = ts;
	}

	/**
	 * Creates a time object from a Julian day.
	 *
	 * @param jd Julian day.
	 * @param ts Time scale ID constant.
	 * @throws JPARSECException Thrown if the Julian day is invalid.
	 */
	public TimeElement(BigDecimal jd, SCALE ts) throws JPARSECException
	{
		astroDate = new AstroDate(jd);
		timeScale = ts;
	}

	/**
	 * An AstroDate object for storing the date.
	 */
	public AstroDate astroDate;

	/**
	 * Time scale that defines the date object.
	 */
	public SCALE timeScale;

	/**
	 * This value sets the desired precision when
	 * writing the output date (number of decimal
	 * positions in the field seconds). Default
	 * value is 0.
	 */
	public int decimalsInSeconds = 0;

	/**
	 * The set of available time scales for input dates. Note
	 * {@linkplain TimeScale} class provides methods to transform
	 * other time scales (TCB, TCG, TAI) to one of these.
	 */
	public enum SCALE {
		/** ID Constant for obtaining Julian day in Local Time. */
		LOCAL_TIME,
		/**
		 * Universal Time (UT1) ID constant for the time scale. UTC is the common
		 * scale given in hour signals, and differs from UT1 by as much as one
		 * second. UT1 is the scale commonly used in astronomical calculations.
		 * Correction from UTC-UT1 difference is applied when necessary, depending
		 * on the last time the file IERS_EOP.txt was updated. If there is no
		 * information available, then it will be supposed to be equal: UT1 = UTC.
		 * This happends before 1960 and always also in the future.
		 */
		UNIVERSAL_TIME_UT1,
		/**
		 * Universal Time Coordinate ID constant for the time scale. UTC is the
		 * common scale given in hour signals, and differs from UT1 by as much as
		 * one second. Correction from UTC-UT1 difference is applied, depending on
		 * the last time the file IERS_EOP.txt was updated. If there is no
		 * information available, then it will be supposed to be equal: UT1 = UTC.
		 */
		UNIVERSAL_TIME_UTC,
		/** ID Constant for obtaining Julian day in Terrestrial Time. */
		TERRESTRIAL_TIME,
		/** ID Constant for obtaining Julian day in Barycentric Dynamical Time.
		 * This time scale is only dependent on the gravitational potential of the Earth,
		 * and it is also called Teph (the time scale of the JPL ephemerides). To obtain
		 * the barycentric coordinate time, independent of any potentials in the Solar
		 * System, use {@linkplain TimeScale#getTCBminusTDB(double)}. */
		BARYCENTRIC_DYNAMICAL_TIME
	};

	/**
	 * List of available time scales.
	 */
	public static final String TIME_SCALES[] = new String[] {"Local time",
		"Universal Time UT1", "Universal time UTC", "Terrestrial time",
		"Barycentric dynamical time"};

	/**
	 * List of available time scales in abbreviated form.
	 */
	public static final String TIME_SCALES_ABBREVIATED[] = new String[] {"LT",
		"UT1", "UTC", "TT", "TDB"};

	/**
	 * Returns the time scale as string.
	 * @param sc The time scale.
	 * @return The abbreviation of the time scale.
	 */
	public static String getTimeScaleAbbreviation(SCALE sc) {
		if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH && sc == SCALE.LOCAL_TIME) return "TL";
		return TIME_SCALES_ABBREVIATED[sc.ordinal()];
	}

	/**
	 * Returns the time scale from its abbreviation.
	 * @param ts Time scale abbreviation.
	 * @return The time scale.
	 * @throws JPARSECException If the input abbreviation in invalid.
	 */
	public static SCALE fromTimeScaleAbbreviation(String ts) throws JPARSECException {
		if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH && ts.equals("TL")) return SCALE.LOCAL_TIME;
		if (ts.equals("LT")) return SCALE.LOCAL_TIME;
		if (ts.equals("TDB")) return SCALE.BARYCENTRIC_DYNAMICAL_TIME;
		if (ts.equals("TT")) return SCALE.TERRESTRIAL_TIME;
		if (ts.equals("UT1")) return SCALE.UNIVERSAL_TIME_UT1;
		if (ts.equals("UTC")) return SCALE.UNIVERSAL_TIME_UTC;
		throw new JPARSECException("Invalid abbreviation "+ts);
	}

	/**
	 * Returns the time scale as string.
	 * @return The abbreviation of the time scale.
	 */
	public String getTimeScaleAbbreviation() {
		if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH && timeScale == SCALE.LOCAL_TIME) return "TL";
		if (timeScale == null) return "";
		return TIME_SCALES_ABBREVIATED[timeScale.ordinal()];
	}

	/**
	 * Returns the time scale as string.
	 * @return The time scale.
	 */
	public String getTimeScale() {
		if (timeScale == null) return "";
		return Translate.translate(268 + timeScale.ordinal());
	}

	/**
	 * Adds a given amount of time to the current instance.
	 * @param days The number of days.
	 * @throws JPARSECException If an error occurs.
	 */
	public void add(double days) throws JPARSECException {
		this.astroDate = new AstroDate(this.astroDate.jd() + days);
	}

	/**
	 * Clones this instance.
	 */
	@Override
	public TimeElement clone()
	{
		TimeElement time;
		try {
			time = new TimeElement(astroDate, this.timeScale);
			time.decimalsInSeconds = this.decimalsInSeconds;
		} catch (Exception e) {
			return null;
		}
		return time;
	}
	/**
	 * Returns whether the input object is equals to this instance.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof TimeElement)) return false;

		TimeElement that = (TimeElement) o;

		if (decimalsInSeconds != that.decimalsInSeconds) return false;
		if (astroDate != null ? !astroDate.equals(that.astroDate) : that.astroDate != null) return false;

		return timeScale == that.timeScale;
	}

	@Override
	public int hashCode() {
		int result = astroDate != null ? astroDate.hashCode() : 0;
		result = 31 * result + (timeScale != null ? timeScale.hashCode() : 0);
		result = 31 * result + decimalsInSeconds;
		return result;
	}

	/**
	 * Returns a simple String representation of this instant.
	 */
	@Override
	public String toString() {
		try {
			return astroDate.toString(this.decimalsInSeconds)+" "+getTimeScaleAbbreviation();
		} catch (Exception e) { // Should never happen
			return null;
		}
	}

	/**
	 * Returns a simple String representation of this instant with the time
	 * represented with hours and minutes, without seconds..
	 * @return The string.
	 */
	public String toMinString() {
		try {
			return astroDate.toMinString()+" "+getTimeScaleAbbreviation();
		} catch (Exception e) { // Should never happen
			return null;
		}
	}

	/**
	 * Constructor for a given date expressed as a String.
	 * @param date The date, in the same format as it is given
	 * by {@linkplain TimeElement#toString()} or
	 * {@linkplain TimeFormat#formatJulianDayAsDateAndTime(double, TimeElement.SCALE)}.
	 * @throws JPARSECException If the input date cannot be parsed.
	 */
	public TimeElement(String date) throws JPARSECException {
		String date1 = FileIO.getField(1, date, " ", true);
		String date2 = FileIO.getField(2, date, " ", true);
		String date3 = FileIO.getField(3, date, " ", true);
		String[] TIME_SCALES_ABBREVIATED = TimeElement.TIME_SCALES_ABBREVIATED.clone();
		if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) TIME_SCALES_ABBREVIATED[0] = "TL";
		String sec = FileIO.getField(3, date2, ":", true).trim();
		int p = sec.indexOf(".");
		int nsec = 0;
		if (p >= 0) nsec = sec.substring(p+1).length();
		this.decimalsInSeconds = nsec;
		try {
			astroDate = new AstroDate(
				Integer.parseInt(FileIO.getField(1, date1, "-", true)),
				Integer.parseInt(FileIO.getField(2, date1, "-", true)),
				Integer.parseInt(FileIO.getField(3, date1, "-", true)),
				Integer.parseInt(FileIO.getField(1, date2, ":", true)),
				Integer.parseInt(FileIO.getField(2, date2, ":", true)),
				DataSet.parseDouble(FileIO.getField(3, date2, ":", true))
			);
			if (astroDate.getDay() > 31 || astroDate.getMonth() > 12) throw new Exception("Not this format!");
			timeScale = SCALE.values() [DataSet.getIndex(TIME_SCALES_ABBREVIATED, date3)];
		} catch (Exception exc) {
			// Example: 15-jul-2012 01:51:01 TDB
			try {
				int year = 0, month = 0, day = 0;
				String m = FileIO.getField(2, date1, "-", false);
				if (DataSet.isDoubleFastCheck(m)) {
					month = Integer.parseInt(m);
				} else {
					month = Calendar.indexFromName(m, Gregorian.MONTH_NAMES);
					if (month == -1) throw new JPARSECException("Cannot recognize month '"+m+"'.");
					month ++;
				}
				if (FileIO.getField(1, date1, "-", true).length() > FileIO.getField(3, date1, "-", true).length()) {
					year = Integer.parseInt(FileIO.getField(1, date1, "-", false));
					day = Integer.parseInt(FileIO.getField(3, date1, "-", false));
				} else {
					year = Integer.parseInt(FileIO.getField(3, date1, "-", false));
					day = Integer.parseInt(FileIO.getField(1, date1, "-", false));
				}

				astroDate = new AstroDate(year, month, day);
				astroDate.setDayFraction(0.0);
				if (!date2.equals("")) {
					if (date2.equals(Translate.translate(1068))) {
						astroDate.setYear(-astroDate.getYear());
						date2 = FileIO.getField(3, date, " ", true);
					}
					if (date2.equals(Translate.translate(1124))) date2 = FileIO.getField(3, date, " ", true);
					if (!date2.equals("")) astroDate.setDayFraction((DataSet.parseDouble(FileIO.getField(1, date2, ":", true).trim())+DataSet.parseDouble(FileIO.getField(2, date2, ":", true).trim())/60.0+DataSet.parseDouble(sec)/3600.0)/24.0);
				}
				if (!date3.equals("")) {
					if (date3.equals(Translate.translate(1068))) {
						astroDate.setYear(-astroDate.getYear());
						date3 = FileIO.getField(4, date, " ", true);
					}
					if (date3.equals(Translate.translate(1124))) date3 = FileIO.getField(4, date, " ", true);
					if (!date3.equals("")) timeScale = SCALE.values() [DataSet.getIndex(TIME_SCALES_ABBREVIATED, date3)];
				}
			} catch (Exception exc2) {
				exc2.printStackTrace();
				throw new JPARSECException("could not parse the date "+date, exc2);
			}
		}
	}
}
