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
package jparsec.ephem;

import java.awt.geom.Point2D;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.math.Constant;
import jparsec.math.FastMath;
import jparsec.observer.LocationElement;
import jparsec.util.Configuration;
import jparsec.util.JPARSECException;

/**
 * Provides methods for manipulating angles or vectors, and formatting strings.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Functions
{
	// private constructor so that this class cannot be instantiated.
	private Functions() {}

	/**
	 * Convert a Julian day value to Julian centuries referenced to epoch J2000.
	 *
	 * @param jd Julian day number.
	 * @return Julian centuries from J2000.
	 */
	public static double toCenturies(double jd)
	{
		return (jd - Constant.J2000) / Constant.JULIAN_DAYS_PER_CENTURY;
	}

	/**
	 * Convert to Julian day a given number of centuries offset from J2000.
	 *
	 * @param centuries Centuries from J2000.
	 * @return Julian day.
	 */
	public static double toJD(double centuries)
	{
		return Constant.JULIAN_DAYS_PER_CENTURY * centuries + Constant.J2000;
	}

	/**
	 * Convert a Julian day value to Julian centuries referenced to epoch J2000.
	 *
	 * @param jd Julian day number.
	 * @return Julian centuries from J2000.
	 */
	public static BigDecimal toCenturies(BigDecimal jd)
	{
		return (jd.subtract(new BigDecimal(Constant.J2000)).divide(new BigDecimal(Constant.JULIAN_DAYS_PER_CENTURY),
				Configuration.BIG_DECIMAL_PRECISION_DECIMAL_PLACES, Configuration.BIG_DECIMAL_PRECISION_ROUNDING_MODE));
	}

	/**
	 * Reduce an angle in degrees to the range (0 &lt;= &lt; 360).
	 *
	 * @param d Value in degrees.
	 * @return The reduced degree value.
	 */
	public static double normalizeDegrees(double d)
	{
		if (d < 0 && d >= -360) return d + 360.0;
		if (d >= 360.0 && d < 720) return d - 360.0;
		if (d >= 0 && d < 360.0) return d;

		d -= Constant.DEGREES_PER_CIRCLE * Math.floor(d / Constant.DEGREES_PER_CIRCLE);
		// Can't use Math.IEEEremainder here because remainder differs
		// from modulus for negative numbers.
		if (d < 0.)	d += Constant.DEGREES_PER_CIRCLE;

		return d;
	}

	/**
	 * Reduce an angle in radians to the range (0 - 2 Pi).
	 *
	 * @param r Value in radians.
	 * @return The reduced radian value.
	 */
	public static double normalizeRadians(double r)
	{
		if (r < 0 && r >= -Constant.TWO_PI) return r + Constant.TWO_PI;
		if (r >= Constant.TWO_PI && r < Constant.FOUR_PI) return r - Constant.TWO_PI;
		if (r >= 0 && r < Constant.TWO_PI) return r;

		double d = r - Constant.TWO_PI * Math.floor(r / Constant.TWO_PI);
		// Can't use Math.IEEEremainder here because remainder differs
		// from modulus for negative numbers.
		if (d < 0.)	d += Constant.TWO_PI;

		return d;
	}

	/**
	 * Reduce an angle in radians to the range (0 - 2 Pi).
	 *
	 * @param r Value in radians.
	 * @return The reduced radian value.
	 */
	public static BigDecimal normalizeRadians(BigDecimal r)
	{
		BigDecimal f = new BigDecimal(Math.floor(r.doubleValue() / Constant.TWO_PI));
		r = r.subtract(Constant.BIG_TWO_PI.multiply(f));
		if (r.doubleValue() < 0.) r.add(Constant.BIG_TWO_PI);
		return r;
	}

	/**
	 * Returns the quadrant (0, 1, 2, or 3) of the specified angle.
	 * <BR>
	 * This function is useful in figuring out dates of lunar phases and
	 * solstices/equinoxes. If the solar longitude is in one quadrant at the
	 * start of a day, but in a different quadrant at the end of a day, then we
	 * know that there must have been a solstice or equinox during that day.
	 * Also, if (lunar longitude - solar longitude) changes quadrants between
	 * the start of a day and the end of a day, we know there must have been a
	 * lunar phase change during that day.
	 *
	 * @param radians Angle in Radians.
	 * @return Quadrant of angle (0, 1, 2, or 3).
	 */
	public static int getQuadrant(double radians)
	{
		return (int) (Functions.normalizeRadians(radians) / Constant.PI_OVER_TWO);
	}

	/**
	 * Obtain the module of the integer division.
	 *
	 * @param val1 Number to divide
	 * @param val2 Number that val1 will be divided by
	 * @return The rest of the integer division
	 */
	public static double module(double val1, double val2)
	{
		double val3 = val1 / val2;
		double rest = val1 - val2 * Math.floor(val3);

		return rest;
	}

	/**
	 * Module operation in arcseconds.
	 *
	 * @param x Value in arcseconds.
	 * @return module.
	 */
	public static double mod3600(double x)
	{
		x = x - 1296000. * Math.floor(x / 1296000.);
		return x;
	}

	/**
	 * Format right ascension. Significant digits are adapted to common
	 * ephemeris precision.
	 *
	 * @param ra Right ascension in radians.
	 * @return String with the format ##h ##m ##.####s.
	 */
	public static String formatRA(double ra)
	{
		String out = "";
		DecimalFormat formatter = new DecimalFormat("00.0000");
		DecimalFormat formatter0 = new DecimalFormat("00");
		ra = Functions.normalizeRadians(ra);
		double ra_h = ra * Constant.RAD_TO_HOUR;
		double ra_m = (ra_h - Math.floor(ra_h)) * 60.0;
		double ra_s = (ra_m - Math.floor(ra_m)) * 60.0;
		ra_h = Math.floor(ra_h);
		ra_m = Math.floor(ra_m);

		out = "" + formatter0.format(ra_h) + "h " + formatter0.format(ra_m) + "m " + formatter.format(ra_s) + "s";
		out = DataSet.replaceAll(out, ",", ".", false);

		return out;
	}

	/**
	 * Format declination. Significant digits are adapted to common ephemeris
	 * precision.
	 *
	 * @param dec Declination in radians. Must be in the range -Pi/2 to +Pi/2.
	 * @return String with the format $##&deg; ##' ##.###'' ($ is the sign).
	 */
	public static String formatDEC(double dec)
	{
		String out = "";
		DecimalFormat formatter = new DecimalFormat("00.000");
		DecimalFormat formatter0 = new DecimalFormat("00");
		double dec_d = Math.abs(dec) * Constant.RAD_TO_DEG;
		double dec_m = (dec_d - Math.floor(dec_d)) * 60.0;
		double dec_s = (dec_m - Math.floor(dec_m)) * 60.0;
		dec_d = Math.floor(dec_d);
		dec_m = Math.floor(dec_m);

		out = "" + formatter0.format(dec_d) + "\u00b0 " + formatter0.format(dec_m) + "' " + formatter.format(dec_s) + "\"";
		out = DataSet.replaceAll(out, ",", ".", false);
		if (dec < 0.0 && Functions.parseDeclination(out) != 0.0)
			out = "-" + out;

		return out;
	}

	/**
	 * Format right ascension. Significant digits are adapted to common
	 * ephemeris precision.
	 *
	 * @param ra Right ascension in radians.
	 * @param nsec Number of decimal places in seconds of time.
	 * @return String with the format ##h ##m ##.##...s.
	 */
	public static String formatRA(double ra, int nsec)
	{
		DecimalFormat formatter;
		if (nsec > 0) {
			String dec = DataSet.repeatString("0", nsec);
			formatter = new DecimalFormat("00." + dec);
		} else {
			formatter = new DecimalFormat("00");
		}
		DecimalFormat formatter0 = new DecimalFormat("00");
		ra = Functions.normalizeRadians(ra);
		double ra_h = ra * Constant.RAD_TO_HOUR;
		double ra_m = (ra_h - Math.floor(ra_h)) * 60.0;
		double ra_s = (ra_m - Math.floor(ra_m)) * 60.0;
		ra_h = Math.floor(ra_h);
		ra_m = Math.floor(ra_m);

		String out = "" + formatter0.format(ra_h) + "h " + formatter0.format(ra_m) + "m " + formatter.format(ra_s) + "s";
		out = DataSet.replaceAll(out, ",", ".", false);

		return out;
	}

	/**
	 * Format right ascension. Significant digits are adapted to common
	 * ephemeris precision. In case the RA is negative a minus sign is used.
	 *
	 * @param ra0 Right ascension in radians.
	 * @param ndec Number of decimal places in minutes or seconds of time.
	 * @param onlyMinutes True to use only minutes in the formatted text.
	 * @return String with the format ##h ##m ##.##...s.
	 */
	public static String formatRAWithNegativeTime(double ra0, int ndec, boolean onlyMinutes)
	{
		DecimalFormat formatter, formatter0;
		if (ndec > 0) {
			String dec = DataSet.repeatString("0", ndec);
			formatter = new DecimalFormat("00." + dec);
			formatter0 = new DecimalFormat("00");			
		} else {
			formatter = new DecimalFormat("00");
			formatter0 = formatter;
		}
		double ra = Math.abs(ra0);
		double ra_h = ra * Constant.RAD_TO_HOUR;
		double ra_m = (ra_h - Math.floor(ra_h)) * 60.0;
		ra_h = Math.floor(ra_h);
		String out;
		if (!onlyMinutes) {
			double ra_s = (ra_m - Math.floor(ra_m)) * 60.0;
			ra_m = Math.floor(ra_m);
			out = "" + formatter0.format(ra_h) + "h " + formatter0.format(ra_m) + "m " + formatter.format(ra_s) + "s";
		} else {
			out = "" + formatter0.format(ra_h) + "h " + formatter.format(ra_m) + "m";
		}
		out = DataSet.replaceAll(out, ",", ".", false);
		if (ra0 < 0) out = "-"+out;
		return out;
	}

	/**
	 * Format right ascension with hours and minutes of time.
	 *
	 * @param ra Right ascension in radians.
	 * @param nmin Number of decimal places in minutes of time.
	 * @return String with the format ##h ##.##...m.
	 */
	public static String formatRAOnlyMinutes(double ra, int nmin)
	{
		DecimalFormat formatter, formatter0;
		if (nmin > 0) {
			String dec = DataSet.repeatString("0", nmin);
			formatter = new DecimalFormat("00." + dec);
			formatter0 = new DecimalFormat("00");			
		} else {
			formatter = new DecimalFormat("00");
			formatter0 = formatter;
		}
		ra = Functions.normalizeRadians(ra);
		double ra_h = ra * Constant.RAD_TO_HOUR;
		double ra_m = (ra_h - Math.floor(ra_h)) * 60.0;
		ra_h = Math.floor(ra_h);

		String out = "" + formatter0.format(ra_h) + "h " + formatter.format(ra_m) + "m";
		out = DataSet.replaceAll(out, ",", ".", false);

		return out;
	}

	/**
	 * Format declination.
	 *
	 * @param dec Declination in radians. Must be in the range -Pi/2 to +Pi/2.
	 * @param nsec Number of decimal places in arcseconds.
	 * @return String with the format $##&deg; ##' ##.##...'' ($ is the sign).
	 */
	public static String formatDEC(double dec, int nsec)
	{
		DecimalFormat formatter, formatter0;
		if (nsec > 0) {
			String decimal = DataSet.repeatString("0", nsec);
			formatter = new DecimalFormat("00." + decimal);
			formatter0 = new DecimalFormat("00");			
		} else {
			formatter = new DecimalFormat("00");
			formatter0 = formatter;
		}
		double dec_d = Math.abs(dec) * Constant.RAD_TO_DEG;
		double dec_m = (dec_d - Math.floor(dec_d)) * 60.0;
		double dec_s = (dec_m - Math.floor(dec_m)) * 60.0;
		dec_d = Math.floor(dec_d);
		dec_m = Math.floor(dec_m);

		String out = "" + formatter0.format(dec_d) + "\u00b0 " + formatter0.format(dec_m) + "' " + formatter.format(dec_s) + "\"";
		out = DataSet.replaceAll(out, ",", ".", false);
		if (dec < 0.0 && Functions.parseDeclination(out) != 0.0)
			out = "-" + out;

		return out;
	}

	/**
	 * Format declination with degrees and minutes only.
	 *
	 * @param dec Declination in radians. Must be in the range -Pi/2 to +Pi/2.
	 * @param nmin Number of decimal places in arcminutes.
	 * @return String with the format $##&deg; ##.##...' ($ is the sign).
	 */
	public static String formatDECOnlyMinutes(double dec, int nmin)
	{
		DecimalFormat formatter, formatter0;
		if (nmin > 0) {
			String decimal = DataSet.repeatString("0", nmin);
			formatter = new DecimalFormat("00." + decimal);
			formatter0 = new DecimalFormat("00");			
		} else {
			formatter = new DecimalFormat("00");
			formatter0 = formatter;
		}
		double dec_d = Math.abs(dec) * Constant.RAD_TO_DEG;
		double dec_m = (dec_d - Math.floor(dec_d)) * 60.0;
		dec_d = Math.floor(dec_d);

		String out = "" + formatter0.format(dec_d) + "\u00b0 " + formatter.format(dec_m) + "'";
		out = DataSet.replaceAll(out, ",", ".", false);
		if (dec < 0.0 && Functions.parseDeclination(out) != 0.0)
			out = "-" + out;

		return out;
	}

	/**
	 * Format a real number.
	 *
	 * @param val Numerical value
	 * @param decimals Number of decimal places.
	 * @return String with the adequate format.
	 */
	public static String formatValue(double val, int decimals)
	{
		DecimalFormat formatter;
		if (decimals > 0) {
			String out = DataSet.repeatString("0", decimals);
			formatter = new DecimalFormat("##0." + out);
		} else {
			formatter = new DecimalFormat("##0");
		}

		String out = formatter.format(val);
		out = DataSet.replaceAll(out, ",", ".", false);

		return out;
	}

	/**
	 * Format a real number.
	 *
	 * @param val Numerical value
	 * @param decimals Number of decimal places.
	 * @param nondecimals Number of non-decimal places.
	 * @param roundUp True to round up the value in case the
	 * number of non-decimal places is lower than the required to
	 * represent the entire value (so rounding is needed). If false,
	 * the value will be rounded down in case the last significant digit
	 * is below 5, and up if it is 5 or greater. If unsure, select false.
	 * @return String with the adequate format.
	 */
	public static String formatValue(double val, int decimals, int nondecimals, boolean roundUp)
	{
		String in = DataSet.repeatString("0", nondecimals);
		int l = (""+(int)Math.abs(val)).length();
		if (l >= nondecimals && decimals == 0) {
			double v = Math.abs(val);
			double fac = FastMath.multiplyBy10ToTheX(1.0, l-nondecimals);
			v = v / fac;
			v = Math.floor(v + 0.5 + ((roundUp && v != (int) v)? 0.5:0));
			v = v * fac * FastMath.sign(val);
			val = v;
		}

		DecimalFormat formatter;
		if (decimals > 0) {
			String out = DataSet.repeatString("0", decimals);
			formatter = new DecimalFormat(in + "." + out);
		} else {
			formatter = new DecimalFormat(in);
		}

		String out = formatter.format(val);
		out = DataSet.replaceAll(out, ",", ".", false);

		return out;
	}

	/**
	 * Formats an angle. For low values the degrees/minutes parts are skipped
	 * when are null. For angles above +260 degrees, the value is represented as
	 * a negative angle.
	 *
	 * @param val Numerical value representing an angle
	 * @param secDecimals Number of decimal places in the arcseconds.
	 * @return String with the adequate format.
	 */
	public static String formatAngle(double val, int secDecimals)
	{
		String out = "";

		val = Functions.normalizeDegrees(val * Constant.RAD_TO_DEG);
		if (val > 260.0)
			val = val - 360.0;

		DecimalFormat formatter;
		if (secDecimals > 0) {
			out = DataSet.repeatString("0", secDecimals);
			formatter = new DecimalFormat("##0." + out);
		} else {
			formatter = new DecimalFormat("###");
		}

		double val_d = Math.abs(val);
		double val_m = (val_d - Math.floor(val_d)) * 60.0;
		double val_s = (val_m - Math.floor(val_m)) * 60.0;

		val_d = (int) val_d;
		val_m = (int) val_m;

		// Round up to arcminutes properly
		if (secDecimals == 0)
		{
			val_s = Math.floor(val_s + 0.5);
			if (val_s == 60.0)
			{
				val_s = 0.0;
				val_m++;
				if (val_m == 60.0)
				{
					val_m = 0.0;
					val_d++;
				}
			}
		}

		if (val_d == 0.0)
		{
			if (val_m == 0.0)
			{
				out = "" + formatter.format(val_s) + "\"";
			} else
			{
				out = "" + (int) val_m + "' " + formatter.format(val_s) + "\"";
			}
		} else
		{
			out = "" + (int) val_d + "\u00b0 " + (int) val_m + "' " + formatter.format(val_s) + "\"";
		}
		out = DataSet.replaceAll(out, ",", ".", false);
		if (val < 0.0 && Functions.parseDeclination(out) != 0.0)
			out = "-" + out;

		return out;
	}

	/**
	 * Formats an angle as degrees only. For angles above +260 degrees
	 * the value is represented as a negative angle.
	 *
	 * @param val Numerical value representing an angle in radians.
	 * @param decimals Number of decimal places in the degrees.
	 * @return String with the adequate format.
	 */
	public static String formatAngleAsDegrees(double val, int decimals)
	{
		val = Functions.normalizeDegrees(val * Constant.RAD_TO_DEG);
		if (val > 260.0)
			val = val - 360.0;

		DecimalFormat formatter;
		if (decimals > 0) {
			String out = DataSet.repeatString("0", decimals);
			formatter = new DecimalFormat("##0." + out);
		} else {
			formatter = new DecimalFormat("##0");
		}

		double val_d = Math.abs(val);

		String out = "" + formatter.format(val_d);
		out = DataSet.replaceAll(out, ",", ".", false);
		if (val < 0.0 && DataSet.parseDouble(out) != 0.0)
			out = "-" + out;

		return out;
	}

	/**
	 * Returns declination in radians given degrees, minutes, and arcseconds. A
	 * minus sign can be set in degrees for southern positions.
	 *
	 * @param decg Degrees.
	 * @param min Arcminutes.
	 * @param sec Arcseconds
	 * @return Declination in radians
	 */
	public static double parseDeclination(String decg, double min, double sec)
	{
		double g = DataSet.parseDouble(decg);
		double dec = Math.abs(g) + min / Constant.SECONDS_PER_MINUTE + sec / Constant.SECONDS_PER_DEGREE;
		dec = dec * Constant.DEG_TO_RAD;
		if (decg.indexOf("-") >= 0)
			dec = -dec;
		return dec;
	}

	/**
	 * Returns right ascension in radians given hours, minutes, and seconds of
	 * time.
	 *
	 * @param hour Hours.
	 * @param min Minutes.
	 * @param sec Seconds.
	 * @return Right ascension value in radians.
	 */
	public static double parseRightAscension(double hour, double min, double sec)
	{
		double ra = Math.abs(hour) + min / Constant.SECONDS_PER_MINUTE + sec / Constant.SECONDS_PER_DEGREE;
		ra = ra / Constant.RAD_TO_HOUR;
		return ra;

	}

	/**
	 * Returns right ascension in radians given a formatted string.
	 *
	 * @param ra Right Ascension as a string ##h ##m ##.#...s, or ##h #.#...m. Numbers
	 * separated by blank spaces are also supported, in the order hour, minutes, seconds.
	 * @return Right ascension value in radians.
	 */
	public static double parseRightAscension(String ra)
	{
		ra = ra.toLowerCase().trim();
		double rah = 0.0;
		double ram = 0.0;
		double ras = 0.0;

		int h = ra.indexOf("h");
		int m = ra.indexOf("m");
		int s = ra.indexOf("s");

		if (h < 0 && m < 0 && s < 0) {
			int n = FileIO.getNumberOfFields(ra, " ", true);
			rah = DataSet.parseDouble(FileIO.getField(1, ra, " ", true));
			if (n > 1) ram = DataSet.parseDouble(FileIO.getField(2, ra, " ", true));
			if (n > 2) ras = DataSet.parseDouble(FileIO.getField(3, ra, " ", true));
		} else {
			if (h > 0)
				rah = DataSet.parseDouble(ra.substring(0, h).trim());
			if (m > 0)
				ram = DataSet.parseDouble(ra.substring(h + 1, m).trim());
			if (s > 0)
				ras = DataSet.parseDouble(ra.substring(m + 1, s).trim());
		}

		double ss = 1;
		if (ra.startsWith("-")) ss = -1;
		double ra_val = Math.abs(rah) + (Math.abs(ram) + Math.abs(ras) / 60.0) / 60.0;
		ra_val = ss * ra_val / Constant.RAD_TO_HOUR;
		return ra_val;
	}

	/**
	 * Returns declination in radians given a formatted string. Numbers
	 * separated by blank spaces are also supported, in the order degrees, minutes, seconds.
	 *
	 * @param dec Declination as a string ##&deg; ##' ##.#...'', or ##&deg; #.#...'. 'd'
	 *        is allowed instead of '&deg;'.
	 * @return Declination value in radians.
	 */
	public static double parseDeclination(String dec)
	{
		dec = dec.toLowerCase().trim();
		double decg = 0.0;
		double decm = 0.0;
		double decs = 0.0;

		int g = dec.indexOf("\u00b0");
		if (g < 0) g = dec.indexOf("\u00ba");
		if (g < 0)
			g = dec.indexOf("d");
		int m = dec.indexOf("'");
		int s = dec.indexOf("''");
		if (s < 0) s = dec.indexOf("\"");

		if (g < 0 && m < 0 && s < 0) {
			int n = FileIO.getNumberOfFields(dec, " ", true);
			decg = DataSet.parseDouble(FileIO.getField(1, dec, " ", true));
			if (n > 1) decm = DataSet.parseDouble(FileIO.getField(2, dec, " ", true));
			if (n > 2) decs = DataSet.parseDouble(FileIO.getField(3, dec, " ", true));
		} else {
			if (g > 0)
				decg = DataSet.parseDouble(dec.substring(0, g).trim());
			if (m > 0)
				decm = DataSet.parseDouble(dec.substring(g + 1, m).trim());
			if (s > 0)
				decs = DataSet.parseDouble(dec.substring(m + 1, s).trim());
		}

		double dec_val = Math.abs(decg) + (Math.abs(decm) + Math.abs(decs) / 60.0) / 60.0;
		if (dec.startsWith("-"))
			dec_val = -dec_val;
		dec_val = dec_val * Constant.DEG_TO_RAD;
		return dec_val;
	}

	/**
	 * Returns the hours of RA from a formatted string.
	 * @param ra Right ascension.
	 * @return Hours.
	 */
	public static String getHoursFromFormattedRA (String ra)
	{
		int h = ra.indexOf("h");
		String hour = ra.substring(0, h);
		return hour;
	}
	/**
	 * Returns the minutes of RA from a formatted string.
	 * @param ra Right ascension.
	 * @return Minutes.
	 */
	public static String getMinutesFromFormattedRA (String ra)
	{
		int h = ra.indexOf("h");
		int m = ra.indexOf("m");
		String min = ra.substring(h+1, m).trim();
		return min;
	}
	/**
	 * Returns the seconds of RA from a formatted string.
	 * @param ra Right ascension.
	 * @return Seconds.
	 */
	public static String getSecondsFromFormattedRA (String ra)
	{
		int m = ra.indexOf("m");
		int s = ra.indexOf("s");
		String sec = ra.substring(m+1, s).trim();
		return sec;
	}

	/**
	 * Returns the degrees of DEC from a formatted string.
	 * @param dec Declination
	 * @return Degrees.
	 */
	public static String getDegreesFromFormattedDEC (String dec)
	{
		int h = dec.indexOf("\u00b0");
		if (h < 0) h = dec.indexOf("\u00ba");
		if (h < 0) h = dec.indexOf("d");
		String deg = dec.substring(0, h);
		if (!deg.startsWith("-")) deg = "+"+deg;
		return deg;
	}
	/**
	 * Returns the minutes of DEC from a formatted string.
	 * @param dec Declination
	 * @return Minutes.
	 */
	public static String getMinutesFromFormattedDEC (String dec)
	{
		int h = dec.indexOf("\u00b0");
		if (h < 0) h = dec.indexOf("\u00ba");
		if (h < 0) h = dec.indexOf("d");
		int m = dec.indexOf("'");
		String min = dec.substring(h+1, m).trim();
		return min;
	}
	/**
	 * Returns the seconds of DEC from a formatted string.
	 * @param dec Declination
	 * @return Seconds.
	 */
	public static String getSecondsFromFormattedDEC (String dec)
	{
		int m = dec.indexOf("'");
		int s = dec.indexOf("''");
		if (s < 0) s = dec.indexOf("\"");
		String sec = dec.substring(m+1, s).trim();
		return sec;
	}

	/**
	 * Separates a RA value into hours, minutes, and seconds of time.
	 * @param ra The input right ascension in radians.
	 * @return H, M, and S of time.
	 */
	public static double[] getHMS(double ra) {
		double out[] = new double[3];
		double rah = ra * Constant.RAD_TO_HOUR;
		out[0] = (int) rah;
		double ram = (rah - out[0]) * 60.0;
		out[1] = (int) ram;
		out[2] = (ram - out[1]) * 60.0;
		return out;
	}

	/**
	 * Separates a DEC value into degrees, minutes, and seconds of arc.
	 * @param dec The input declination in radians.
	 * @return D, M, and S of arc, and sign in the fourth component. The
	 * sign is 1 if DEC is positive, and -1 if it is negative.
	 */
	public static double[] getDMSs(double dec) {
		double out[] = new double[4];
		double ded = Math.abs(dec * Constant.RAD_TO_DEG);
		out[0] = (int) ded;
		double dem = (ded - out[0]) * 60.0;
		out[1] = (int) dem;
		out[2] = (dem - out[1]) * 60.0;
		out[3] = 1;
		if (dec < 0.0) out[3] = -1;
		return out;
	}

	/**
	 * Substracts one vector from another one.
	 *
	 * @param v1 Array (x, y, z) or (x, y, z, vx, vy, vz).
	 * @param v2 Array (x, y, z) or (x, y, z, vx, vy, vz).
	 * @return v1 - v2.
	 */
	public static double[] substract(double v1[], double[] v2)
	{
		int n1 = v1.length;
		int n2 = v2.length;
		int n = n1;
		if (n2 < n1)
			n = n2;
		double v3[] = new double[n];
		for (int i = 0; i < n; i++)
		{
			v3[i] = v1[i] - v2[i];
		}
		return v3;
	}

	/**
	 * Multiply the components of a vector by a constant.
	 *
	 * @param v Array (x, y, z) or (x, y, z, vx, vy, vz).
	 * @param val Numerical value.
	 * @return (x * val, y * val, z * val, ...).
	 */
	public static double[] scalarProduct(double v[], double val)
	{
		double v_out[] = new double[v.length];
		for (int i = 0; i < v.length; i++)
		{
			v_out[i] = v[i] * val;
		}
		return v_out;
	}

	/**
	 * Returns the cross product of two vectors of length 3.
	 * @param v0 Vector 1.
	 * @param v1 Vector 2.
	 * @return Cross product.
	 * @throws JPARSECException If the size of any of the input vectors is not 3.
	 */
	public static double[] crossProduct(double v0[], double v1[]) throws JPARSECException
	{
	  if (v0.length != 3 || v1.length != 3) throw new JPARSECException("The size of the vectors must be 3.");
	  double crossProduct[] = new double[3];
	  crossProduct[0] = v0[1] * v1[2] - v0[2] * v1[1];
	  crossProduct[1] = v0[2] * v1[0] - v0[0] * v1[2];
	  crossProduct[2] = v0[0] * v1[1] - v0[1] * v1[0];
	  return crossProduct;
	}

	/**
	 * Scalar product of two vectors.
	 *
	 * @param v1 Array (x, y, z) or with arbitrary number of components.
	 * @param v2 Array (x, y, z) or with arbitrary number of components.
	 * @return Scalar product.
	 * @throws JPARSECException If the vectors have different lengths.
	 */
	public static double scalarProduct(double v1[], double v2[]) throws JPARSECException
	{
		if (v1.length != v2.length) throw new JPARSECException("vectors should have the same length, not "+v1.length+" and "+v2.length);
		double out = 0.0;
		for (int i = 0; i < v1.length; i++)
		{
			out += v1[i] * v2[i];
		}

		return out;
	}

	/**
	 * Sums all the components of a vector.
	 *
	 * @param v Array (x, y, z) or (x, y, z, vx, vy, vz).
	 * @return Sum of components.
	 */
	public static double sumComponents(double v[])
	{
		int n = v.length;
		double sum = 0.0;
		for (int i = 0; i < n; i++)
		{
			sum += v[i];
		}
		return sum;
	}

	/**
	 * Sums two vectors.
	 *
	 * @param v1 Array (x, y, z) or (x, y, z, vx, vy, vz).
	 * @param v2 Array (x, y, z) or (x, y, z, vx, vy, vz).
	 * @return Sum of both vectors.
	 */
	public static double[] sumVectors(double v1[], double[] v2)
	{
		int n1 = v1.length;
		int n2 = v2.length;
		int n = n1;
		if (n2 < n1)
			n = n2;
		double v3[] = new double[n];
		for (int i = 0; i < n; i++)
		{
			v3[i] = v1[i] + v2[i];
		}
		return v3;
	}

	/**
	 * Rotate a set of rectangular coordinates from X axis. Used for rotating
	 * from ecliptic to equatorial or back.
	 *
	 * @param c Array (x, y, z) or (x, y, z, vx, vy, vz).
	 * @param angle Rotation angle in radians.
	 * @return Rotated vector.
	 */
	public static double[] rotateX(double c[], double angle)
	{
		double coords[] = c.clone();

		double tmp = coords[1] * Math.cos(angle) - coords[2] * Math.sin(angle);
		coords[2] = coords[1] * Math.sin(angle) + coords[2] * Math.cos(angle);
		coords[1] = tmp;

		// Treat velocities if they are present
		if (coords.length > 3)
		{
			tmp = coords[4] * Math.cos(angle) - coords[5] * Math.sin(angle);
			coords[5] = coords[4] * Math.sin(angle) + coords[5] * Math.cos(angle);
			coords[4] = tmp;
		}

		return coords;
	}

	/**
	 * Rotate a set of rectangular coordinates from Y axis.
	 *
	 * @param c Array (x, y, z) or (x, y, z, vx, vy, vz).
	 * @param angle Rotation angle in radians.
	 * @return Rotated vector.
	 */
	public static double[] rotateY(double c[], double angle)
	{
		double coords[] = c.clone();

		double tmp = coords[0] * Math.cos(angle) + coords[2] * Math.sin(angle);
		coords[2] = -coords[0] * Math.sin(angle) + coords[2] * Math.cos(angle);
		coords[0] = tmp;

		// Treat velocities if they are present
		if (coords.length > 3)
		{
			tmp = coords[3] * Math.cos(angle) + coords[5] * Math.sin(angle);
			coords[5] = -coords[3] * Math.sin(angle) + coords[5] * Math.cos(angle);
			coords[3] = tmp;
		}

		return coords;
	}

	/**
	 * Rotate a set of rectangular coordinates from Z axis.
	 *
	 * @param c Array (x, y, z) or (x, y, z, vx, vy, vz).
	 * @param angle Rotation angle in radians.
	 * @return Rotated vector.
	 */
	public static double[] rotateZ(double c[], double angle)
	{
		double coords[] = c.clone();

		double tmp = coords[0] * Math.cos(angle) - coords[1] * Math.sin(angle);
		coords[1] = coords[0] * Math.sin(angle) + coords[1] * Math.cos(angle);
		coords[0] = tmp;

		// Treat velocities if they are present
		if (coords.length > 3)
		{
			tmp = coords[3] * Math.cos(angle) - coords[4] * Math.sin(angle);
			coords[4] = coords[3] * Math.sin(angle) + coords[4] * Math.cos(angle);
			coords[3] = tmp;
		}

		return coords;
	}

	/**
	 * Check if the components of two vectors are equal or not. Velocities are
	 * also check if they are present. The sizes of the vectors are not check, since
	 * one could have velocities and the other no, for example.
	 *
	 * @param v1 First vector
	 * @param v2 Second vector.
	 * @return True if the components are equal, false otherwise.
	 */
	public static boolean equalVectors(double v1[], double v2[])
	{
		int s1 = v1.length;
		int s2 = v2.length;
		int s = s1;
		if (s2 < s)
			s = s2;
		boolean IsEqual = true;

		for (int i = 0; i < s; i++)
		{
			if (v1[i] != v2[i])
				IsEqual = false;
		}

		return IsEqual;
	}

	/**
	 * Astronomical units to arcseconds conversion.
	 *
	 * @param ua Value in astronomical units.
	 * @param distance Distance to the source in pc.
	 * @return Value in arcseconds.
	 */
	public static double ua2sec(double ua, double distance)
	{
		return (Math.atan(ua * Constant.AU * 1000.0 / (distance * Constant.PARSEC)) / Constant.ARCSEC_TO_RAD);
	}

	/**
	 * Arcseconds to astronomical units conversion.
	 *
	 * @param sec Value in arcseconds.
	 * @param distance Distance to the source in pc.
	 * @return Value in astronomical units.
	 */
	public static double sec2ua(double sec, double distance)
	{
		return (Math.tan(sec * Constant.ARCSEC_TO_RAD) * distance * Constant.PARSEC / (Constant.AU * 1000.0));
	}

	/**
	 * Returns the norm of a 3d vector (square root of the sum
	 * of the squares of the three components).
	 * @param v The vector.
	 * @return It's norm.
	 */
	public static double getNorm(double v[]) {
		return Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
	}

	/**
	*  Rounds a floating point number up to the desired decimal place.
	*  Example:  1346.4667 rounded up to the 2nd place = 1400.
	* <BR>Method by Joseph A. Huwaldt.
	*
	*  @param  value  The value to be rounded up.
	*  @param  place  Number of decimal places to round value to.
	*                 A place of 1 rounds to 10's place, 2 to 100's
	*                 place, -2 to 1/100th place, et cetera.
	*  @return The rounded value.
	**/
	public static double roundUpToPlace(double value, int place)  {
		//	If the value is zero, just pass the number back out.
		if (value != 0.) {
	        //  If the place is zero, round to the one's place.
		    if (place == 0)
		        value = Math.ceil(value);
		    else {
			    double pow10 = FastMath.multiplyBy10ToTheX(1.0, place); //DataSet.parseDouble("1E"+place);	//	= 10 ^ place
			    double holdvalue = value/pow10;

			    value = Math.ceil(holdvalue);			// Round number up to nearest integer
			    value *= pow10;
		    }
		}

		return value;
	}

	/**
	*  Rounds a floating point number to the desired decimal place.
	*  Example:  1346.4667 rounded to the 2nd place = 1300.
	* <BR>Method by Joseph A. Huwaldt.
	*
	*  @param  value  The value to be rounded.
	*  @param  place  Number of decimal places to round value to.
	*                 A place of 1 rounds to 10's place, 2 to 100's
	*                 place, -2 to 1/100th place, et cetera.
	*  @return The rounded value.
	**/
	public static double roundToPlace(double value, int place)  {
		//	If the value is zero, just pass the number back out.
		if (value != 0.) {
	        //  If the place is zero, round to the one's place.
		    if (place == 0)
		        value = Math.floor(value+0.5);
		    else {
			    double pow10 = FastMath.multiplyBy10ToTheX(1.0, place); //DataSet.parseDouble("1E"+place);	//	= 10 ^ place
			    double holdvalue = value/pow10;

			    value = Math.floor(holdvalue+0.5);		// Round number to nearest integer
			    value *= pow10;
		    }
		}

		return value;
	}

	/**
	*  Rounds a floating point number down to the desired decimal place.
	*  Example:  1346.4667 rounded down to the 1st place = 1340.
	*  Method by Joseph A. Huwaldt.
	*
	*  @param  value  The value to be rounded down.
	*  @param  place  Number of decimal places to round value to.
	*                 A place of 1 rounds to 10's place, 2 to 100's
	*                 place, -2 to 1/100th place, et cetera.
	*  @return The rounded value.
	**/
	public static double roundDownToPlace(double value, int place)  {
		//	If the value is zero, just pass the number back out.
		if (value != 0.) {

	        //  If the place is zero, round to the one's place.
		    if (place == 0)
		        value = Math.floor(value);
		    else {
			    double pow10 = FastMath.multiplyBy10ToTheX(1.0, place); //DataSet.parseDouble("1E"+place);	//	= 10 ^ place
			    double holdvalue = value/pow10;

			    value = Math.floor(holdvalue);			// Round number down to nearest integer
			    value *= pow10;
		    }
		}

		return value;
	}

/**
   * Rounds a double and converts it into String.
   * <BR>Method by Joseph A. Huwaldt.
   *
   * @param value the double value
   * @param afterDecimalPoint the (maximum) number of digits permitted
   * after the decimal point
   * @return the double as a formatted string
   */
  public static String doubleToString(double value, int afterDecimalPoint) {
    StringBuffer stringBuffer;
    double temp;
    int dotPosition;
    long precisionValue;

    temp = FastMath.multiplyBy10ToTheX(value, afterDecimalPoint);
    if (Math.abs(temp) < Long.MAX_VALUE) {
      precisionValue = 	(temp > 0) ? (long)(temp + 0.5)
                                   : -(long)(Math.abs(temp) + 0.5);
      if (precisionValue == 0) {
    	  stringBuffer = new StringBuffer(String.valueOf(0));
      } else {
    	  stringBuffer = new StringBuffer(String.valueOf(precisionValue));
      }
      if (afterDecimalPoint == 0) {
    	  return stringBuffer.toString();
      }
      dotPosition = stringBuffer.length() - afterDecimalPoint;
      while (((precisionValue < 0) && (dotPosition < 1)) ||
	     (dotPosition < 0)) {
		if (precisionValue < 0) {
		  stringBuffer.insert(1, '0');
		} else {
		  stringBuffer.insert(0, '0');
		}
		dotPosition++;
      }
      stringBuffer.insert(dotPosition, '.');
      if ((precisionValue < 0) && (stringBuffer.charAt(1) == '.')) {
    	  stringBuffer.insert(1, '0');
      } else if (stringBuffer.charAt(0) == '.') {
    	  stringBuffer.insert(0, '0');
      }
      int currentPos = stringBuffer.length() - 1;
      while ((currentPos > dotPosition) &&
	     (stringBuffer.charAt(currentPos) == '0')) {
    	  stringBuffer.setCharAt(currentPos--, ' ');
      }
      if (stringBuffer.charAt(currentPos) == '.') {
    	  stringBuffer.setCharAt(currentPos, ' ');
      }

      return stringBuffer.toString().trim();
    }
    return new String("" + value);
  }

/**
   * Rounds a double and converts it into a formatted decimal-justified String.
   * Trailing 0's are replaced with spaces.
   * <BR>Method by Joseph A. Huwaldt.
   *
   * @param value the double value
   * @param width the width of the string
   * @param afterDecimalPoint the number of digits after the decimal point
   * @return the double as a formatted string
   */
  public static String doubleToString(double value, int width,
				      int afterDecimalPoint) {
    String tempString = doubleToString(value, afterDecimalPoint);
    char[] result;
    int dotPosition;

    if ((afterDecimalPoint >= width)
        || (tempString.indexOf('E') != -1)) { // Protects sci notation
      return tempString;
    }

    // Initialize result
    result = new char[width];
    for (int i = 0; i < result.length; i++) {
      result[i] = ' ';
    }

    if (afterDecimalPoint > 0) {
      // Get position of decimal point and insert decimal point
      dotPosition = tempString.indexOf('.');
      if (dotPosition == -1) {
    	  dotPosition = tempString.length();
      } else {
    	  result[width - afterDecimalPoint - 1] = '.';
      }
    } else {
      dotPosition = tempString.length();
    }

    int offset = width - afterDecimalPoint - dotPosition;
    if (afterDecimalPoint > 0) {
      offset--;
    }

    // Not enough room to decimal align within the supplied width
    if (offset < 0) {
      return tempString;
    }

    // Copy characters before decimal point
    for (int i = 0; i < dotPosition; i++) {
      result[offset + i] = tempString.charAt(i);
    }

    // Copy characters after decimal point
    for (int i = dotPosition + 1; i < tempString.length(); i++) {
      result[offset + i] = tempString.charAt(i);
    }

    return new String(result);
  }

  /**
   * Computes the minimum spherical circle containing three celestial
   * bodies located quite close to each other.
   * @param lo1 Location of first object.
   * @param lo2 Location of second object.
   * @param lo3 Location of third object.
   * @return The location of the center of the triangle, and its radius
   * in radians.
   */
  public static LocationElement getCircleContainingThreeObjects(LocationElement lo1, LocationElement lo2,
		  LocationElement lo3) {
	  LocationElement loc1 = lo1.clone();
	  LocationElement loc2 = lo2.clone();
	  LocationElement loc3 = lo3.clone();
	  loc1.setRadius(1.0);
	  loc2.setRadius(1.0);
	  loc3.setRadius(1.0);

	  double d12 = LocationElement.getAngularDistance(loc1, loc2) * 0.5;
	  double d13 = LocationElement.getAngularDistance(loc1, loc3) * 0.5;
	  double d23 = LocationElement.getAngularDistance(loc2, loc3) * 0.5;
	  double maxD = Math.max(Math.max(d12, d13), d23);
	  double a = maxD, b = 0.0, c = 0.0;

	  if (maxD == d12) {
		  b = d13;
		  c = d23;
	  }
	  if (maxD == d13) {
		  b = d12;
		  c = d23;
	  }
	  if (maxD == d23) {
		  b = d12;
		  c = d13;
	  }
	  double h = Math.sqrt(b * b + c * c);

	  double r = 0;
	  if (maxD > h) {
		  // type I: radius = a, and center position = midpoint between those two
		  r = a;
		  LocationElement l1 = loc1, l2 = loc2;
		  if (r == d13) l2 = loc3;
		  if (r == d23) l1 = loc3;

		  LocationElement l = LocationElement.getMidPoint(l1, l2);
		  l.setRadius(r);
		  return l;
	  } else {
		  // type II
		  r = 2.0 * a * b * c / Math.sqrt((a + b + c) * (a + b - c) * (-a + b + c) * (a - b + c));
		  double dr = d12 + d23 + d13;
		  do {
			  loc1 = LocationElement.getMidPoint(loc1, loc2);
			  loc2 = LocationElement.getMidPoint(loc2, loc3);
			  loc3 = LocationElement.getMidPoint(loc3, loc1);

			  d12 = LocationElement.getAngularDistance(loc1, loc2) * 0.5;
			  d13 = LocationElement.getAngularDistance(loc1, loc3) * 0.5;
			  d23 = LocationElement.getAngularDistance(loc2, loc3) * 0.5;
			  dr = d12 + d23 + d13;
		  } while (dr > 1.0E-8); // arbitrary precision of 2 mas
		  loc1.setRadius(r);
		  return loc1;
	  }
  }

  /**
   * Computes the circle containing three positions.
   * @param p1 Location of first object.
   * @param p2 Location of second object.
   * @param p3 Location of third object.
   * @return Three values: center x, center y, and radius.
   */
  public static double[] getCircleContainingThreeObjects(Point2D p1, Point2D p2, Point2D p3) {
	  double offset = Math.pow(p2.getX(),2) + Math.pow(p2.getY(),2);
	  double bc =   ( Math.pow(p1.getX(),2) + Math.pow(p1.getY(),2) - offset )/2.0;
	  double cd =   (offset - Math.pow(p3.getX(), 2) - Math.pow(p3.getY(), 2))/2.0;
	  double det =  (p1.getX() - p2.getX()) * (p2.getY() - p3.getY()) - (p2.getX() - p3.getX())* (p1.getY() - p2.getY());

	  if (Math.abs(det) < 1.0E-8) {
		  //return null; // Straight line case
		  double x = (p1.getX() + p2.getX() + p3.getX()) / 3.0;
		  double y = (p1.getY() + p2.getY() + p3.getY()) / 3.0;
		  Point2D c = new Point2D.Double(x, y);
		  double r1 = c.distance(p1);
		  double r2 = c.distance(p2);
		  double r3 = c.distance(p3);
		  double r = r1;
		  if (r2 > r) r = r2;
		  if (r3 > r) r = r3;
		  return new double[] {x, y, r};
	  }

	  double idet = 1/det;

	  double centerx =  (bc * (p2.getY() - p3.getY()) - cd * (p1.getY() - p2.getY())) * idet;
	  double centery =  (cd * (p1.getX() - p2.getX()) - bc * (p2.getX() - p3.getX())) * idet;
	  double radius = Math.sqrt( Math.pow(p2.getX() - centerx,2) + Math.pow(p2.getY()-centery,2));

	  return new double[] {centerx, centery, radius};
  }

	/**
	 * Returns a value proportional to the distance of three bodies to the same great circle
	 * on the celestial sphere. Maybe used to get the time when three bodies are in straight line.
	 * See Meeus, chapter 18.
	 * @param loc1 Position of object 1.
	 * @param loc2 Position of object 2.
	 * @param loc3 Position of object 3.
	 * @return A value that will be 0 if the three bodies are in straight line.
	 */
	public static double getDistanceToSameGreatCircle(LocationElement loc1, LocationElement loc2, LocationElement loc3) {
		return
			Math.tan(loc1.getLatitude()) * Math.sin(loc2.getLongitude() - loc3.getLongitude()) +
			Math.tan(loc2.getLatitude()) * Math.sin(loc3.getLongitude() - loc1.getLongitude()) +
			Math.tan(loc3.getLatitude()) * Math.sin(loc1.getLongitude() - loc2.getLongitude());
	}

	/**
	 * Format a String representation of an integer at the specified
	 * width.
	 * <BR>
	 * Note that this function will return an incorrect representation if the
	 * integer is wider than the specified width. For example:<BR>
	 *  fmt( 1, 3 ) will return "001".<BR>
	 *  fmt( 12, 3 ) will return "012".<BR>
	 *  fmt( 1234, 3 ) will return "<B>234</B>."
	 *
	 * @param i The integer to format.
	 * @param w The format width.
	 * @return A formatted String.
	 */
	public static String fmt(int i, int w)
	{
		StringBuffer sb = new StringBuffer();
		while (w-- > 0)
		{
			sb.append((char) ('0' + (i % 10)));
			i /= 10;
		}
		sb.reverse();
		return sb.toString();
	}

	/**
	 * Format a String representation of an integer at the specified
	 * width, and add the specified suffix.
	 * <BR>
	 * Note that this will return an incorrect representation if the integer is
	 * wider than the specified width. For example:<BR>
	 *  fmt( 1, 3, ':' ) will return "001:".<BR>
	 *  fmt( 12, 3, ':' ) will return "012:".<BR>
	 *  fmt( 1234, 3, ':' ) will return "<B>234:</B>."
	 *
	 * @param i The integer to format.
	 * @param w The format width.
	 * @param suffix The character to append.
	 * @return A formatted String.
	 */
	public static String fmt(int i, int w, char suffix)
	{
		return fmt(i, w) + suffix;
	}

	/**
	  * Returns the RGBA color given their components in the
	  * range 0-255.
	  * @param r Red component.
	  * @param g Green component.
	  * @param b Blue component.
	  * @param a Alpha component. 0 is transparent.
	  * @return RGBA.
	  */
	public static int getColor(int r, int g, int b, int a) {
		if (r < 0) r = 0;
		if (g < 0) g = 0;
		if (b < 0) b = 0;
		if (a < 0) a = 0;
		if (r > 255) r = 255;
		if (g > 255) g = 255;
		if (b > 255) b = 255;
		if (a > 255) a = 255;

		return a<<24 | r<<16 | g<<8 | b;
	}

	/**
	  * Returns the RGBA colors.
	  * @param color The color.
	  * @return RGBA components.
	  */
	public static int[] getColorComponents(int color) {
		return new int[] {(color>>16)&255, (color>>8)&255, color&255, (color>>24)&255};
	}

  /**
   * Computes the minimum enclosing circle for an arbitrary number of points.
   * @param points A list containing the set of points in 2d.
   * @return Three values: center x, center y, and radius.
   */
	public static double[] getCircleContainingObjects(ArrayList<Point2D> points) {
		ArrayList<Point> a = new ArrayList<Point>();
		for (int i=0; i<points.size(); i++) {
			Point2D p = points.get(i);
			a.add(new Point(p.getX(), p.getY()));
		}
		Circle c = makeCircle(a);
		return new double[] {c.c.x, c.c.y, c.r};
	}

	private static Circle makeCircle(List<Point> points) {
		// Clone list to preserve the caller's data, randomize order
		List<Point> shuffled = new ArrayList<Point>(points);
		Collections.shuffle(shuffled, new Random());

		// Progressively add points to circle or recompute circle
		Circle c = null;
		for (int i = 0; i < shuffled.size(); i++) {
			Point p = shuffled.get(i);
			if (c == null || !c.contains(p))
				c = makeCircleOnePoint(shuffled.subList(0, i + 1), p);
		}
		return c;
	}

	// One boundary point known
	private static Circle makeCircleOnePoint(List<Point> points, Point p) {
		Circle c = new Circle(p, 0);
		for (int i = 0; i < points.size(); i++) {
			Point q = points.get(i);
			if (!c.contains(q)) {
				if (c.r == 0)
					c = makeDiameter(p, q);
				else
					c = makeCircleTwoPoints(points.subList(0, i + 1), p, q);
			}
		}
		return c;
	}

	// Two boundary points known
	private static Circle makeCircleTwoPoints(List<Point> points, Point p, Point q) {
		Circle temp = makeDiameter(p, q);
		if (temp.contains(points))
			return temp;

		Circle left = null;
		Circle right = null;
		for (Point r : points) {  // Form a circumcircle with each point
			Point pq = q.subtract(p);
			double cross = pq.cross(r.subtract(p));
			Circle c = makeCircumcircle(p, q, r);
			if (c == null)
				continue;
			else if (cross > 0 && (left == null || pq.cross(c.c.subtract(p)) > pq.cross(left.c.subtract(p))))
				left = c;
			else if (cross < 0 && (right == null || pq.cross(c.c.subtract(p)) < pq.cross(right.c.subtract(p))))
				right = c;
		}
		return right == null || left != null && left.r <= right.r ? left : right;
	}

	private static Circle makeDiameter(Point a, Point b) {
		return new Circle(new Point((a.x + b.x)/ 2, (a.y + b.y) / 2), a.distance(b) / 2);
	}

	private static Circle makeCircumcircle(Point a, Point b, Point c) {
		// Mathematical algorithm from Wikipedia: Circumscribed circle
		double d = (a.x * (b.y - c.y) + b.x * (c.y - a.y) + c.x * (a.y - b.y)) * 2;
		if (d == 0)
			return null;
		double x = (a.norm() * (b.y - c.y) + b.norm() * (c.y - a.y) + c.norm() * (a.y - b.y)) / d;
		double y = (a.norm() * (c.x - b.x) + b.norm() * (a.x - c.x) + c.norm() * (b.x - a.x)) / d;
		Point p = new Point(x, y);
		return new Circle(p, p.distance(a));
	}
}

class Circle {

	private static double EPSILON = 1e-12;

	public final Point c;   // Center
	public final double r;  // Radius

	public Circle(Point c, double r) {
		this.c = c;
		this.r = r;
	}

	public boolean contains(Point p) {
		return c.distance(p) <= r + EPSILON;
	}

	public boolean contains(Collection<Point> ps) {
		for (Point p : ps) {
			if (!contains(p))
				return false;
		}
		return true;
	}

	public String toString() {
		return String.format("Circle(x=%g, y=%g, r=%g)", c.x, c.y, r);
	}
}

class Point {

	public final double x;
	public final double y;

	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public Point subtract(Point p) {
		return new Point(x - p.x, y - p.y);
	}

	public double distance(Point p) {
		return FastMath.hypot(x - p.x, y - p.y);
	}

	// Signed area / determinant thing
	public double cross(Point p) {
		return x * p.y - y * p.x;
	}

	// Magnitude squared
	public double norm() {
		return x * x + y * y;
	}

	public String toString() {
		return String.format("Point(%g, %g)", x, y);
	}
}
