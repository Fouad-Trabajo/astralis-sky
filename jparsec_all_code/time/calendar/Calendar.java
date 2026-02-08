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

import jparsec.graph.DataSet;
import jparsec.math.Constant;
import jparsec.observer.CityElement;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;
import jparsec.util.Translate.LANGUAGE;

/**
 * A support class for the calendars, based on Calendrical Calculations book.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Calendar
{
	// private constructor so that this class cannot be instantiated.
	private Calendar() {}

	private static class ob
	{
		private static final double coeffObliquity[] =
		{ 0, Calendar.angle(0.0, 0.0, -46.815), Calendar.angle(0.0, 0.0, -0.00059),
				Calendar.angle(0.0, 0.0, 0.001813) };
	}

	private static class ec
	{
		private static final double coeff19th[] =
		{ -2.E-005, 0.000297, 0.025184, -0.181133,
				0.55304, -0.861938, 0.677066, -0.212591 };

		private static final double coeff18th[] =
		{ -9.0E-006, 0.00384400, 0.083563, 0.865736,
				4.867575, 15.845535, 31.332267, 38.291999, 28.316289,
				11.636204, 2.043794 };

		private static final double coeff17th[] =
		{ 8.118780842, -0.005092142, 0.00333612, -2.66484E-005 };

		private static final double coeff16th[] =
		{ 196.58333, -4.0675, 0.0219167 };
	}

	private static class et
	{
		private static final double coeffLongitude[] = new double[]
		{ 280.46645, 36000.76983, 0.0003032 };

		private static final double coeffAnomaly[] = new double[]
		{ 357.5291, 35999.0503, -0.0001559, -4.8E-007 };

		private static final double coeffEccentricity[] = new double[]
		{ 0.016708617, -4.2037E-005, -1.236E-007 };
	}

	private static class sl
	{
		private static final int coefficients[] =
		{ 0x627ce, 0x2fa87, 0x1d289, 0x1b708, 3891, 2819, 1721, 660, 350, 334, 314, 268, 242, 234, 158, 132, 129, 114,
				99, 93, 86, 78, 72, 68, 64, 46, 38, 37, 32, 29, 28, 27, 27, 25, 24, 21, 21, 20, 18, 17, 14, 13, 13, 13,
				12, 10, 10, 10, 10 };

		private static final double multipliers[] =
		{ 0.9287892, 35999.1376958, 35999.4089666, 35998.7287385, 71998.20261,
				71998.4403, 36000.35726, 71997.4812, 32964.4678,
				-19.441, 445267.1117, 45036.884, 3.1008, 22518.4434, -19.9739,
				65928.9345, 9038.0293, 3034.7684, 33718.148,
				3034.448, -2280.773, 29929.992, 31556.493,
				149.588, 9037.75, 107997.405, -4444.176, 151.771,
				67555.316, 31556.08, -4561.54, 107996.706, 1221.655,
				62894.167, 31437.369, 14578.298, -31931.757,
				34777.243, 1221.999, 62894.511, -4442.039, 107997.909, 119.066,
				16859.071, -4.578, 26895.292, -39.1270, 12297.536,
				90073.778 };

		private static final double addends[] =
		{ 270.54861, 340.19128, 63.91854, 331.2622, 317.843, 86.631,
				240.052, 310.26, 247.23, 260.87, 297.82,
				343.14, 166.79, 81.5300, 3.5, 132.75, 182.95,
				162.03, 29.8000, 266.4, 249.2, 157.6,
				257.800, 185.1, 69.9, 8, 197.1,
				250.400, 65.3, 162.7, 341.5, 291.600, 98.5,
				146.7, 110, 5.2, 342.600, 230.900,
				256.100, 45.3, 242.900, 115.2, 151.800,
				285.300, 53.3, 126.6, 205.7,
				85.9, 146.1 };
	}

	private static class nu
	{
		private static final double coeffa[] = new double[]
		{ 124.900, -1934.134, 0.002063 };

		private static final double coeffb[] = new double[]
		{ 201.110, 72001.5377, 0.00057 };
	}

	private static class llon
	{
		private static final double coeffMeanMoon[] = new double[]
		{ 218.3164591, 481267.88134236, -0.0013268, 1.855835023689734E-006,
				-1.5338834862103876E-008 };

		private static final double coeffElongation[] = new double[]
		{ 297.8502042, 445267.1115168, -0.00163, 1.8319447192361523E-006,
				-8.8444699951355417E-009 };

		private static final double coeffSolarAnomaly[] = new double[]
		{ 357.5291092, 35999.0502909, -0.0001536, 4.0832993058391183E-008 };

		private static final double coeffLunarAnomaly[] = new double[]
		{ 134.9634114, 477198.8676313, 0.008997, 1.4347408140719379E-005,
				-6.7971723762914631E-008 };

		private static final double coeffMoonNode[] = new double[]
		{ 93.2720993, 483202.0175273, -0.0034029, -2.8360748723766307E-007,
				1.1583324645839848E-009 };

		private static final double coeffCapE[] =
		{ 1.0, -0.002516, -7.40E-006 };

		private static final byte argsLunarElongation[] =
		{ 0, 2, 2, 0, 0, 0, 2, 2, 2, 2, 0, 1, 0, 2, 0, 0, 4, 0, 4, 2, 2, 1, 1, 2, 2, 4, 2, 0, 2, 2, 1, 2, 0, 0, 2, 2,
				2, 4, 0, 3, 2, 4, 0, 2, 2, 2, 4, 0, 4, 1, 2, 0, 1, 3, 4, 2, 0, 1, 2 };

		private static final byte argsSolarAnomaly[] =
		{ 0, 0, 0, 0, 1, 0, 0, -1, 0, -1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, -1, 0, 0, 0, 1, 0, -1, 0, -2, 1, 2,
				-2, 0, 0, -1, 0, 0, 1, -1, 2, 2, 1, -1, 0, 0, -1, 0, 1, 0, 1, 0, 0, -1, 2, 1, 0 };

		private static final byte argsLunarAnomaly[] =
		{ 1, -1, 0, 2, 0, 0, -2, -1, 1, 0, -1, 0, 1, 0, 1, 1, -1, 3, -2, -1, 0, -1, 0, 1, 2, 0, -3, -2, -1, -2, 1, 0,
				2, 0, -1, 1, 0, -1, 2, -1, 1, -2, -1, -1, -2, 0, 1, 4, 0, -2, 0, 2, 1, -2, -3, 2, 1, -1, 3 };

		private static final byte argsMoonFromNode[] =
		{ 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, -2, 2, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0,
				-2, 2, 0, 2, 0, 0, 0, 0, 0, 0, -2, 0, 0, 0, 0, -2, -2, 0, 0, 0, 0, 0, 0, 0 };

		private static final int sineCoefficients[] =
		{ 0x5ff586, 0x1370ab, 0xa0b8a, 0x34272, 0xfffd2ce4, 0xfffe4164, 58793, 57066, 53322, 45758, -40923, -34720,
				-30383, 15327, -12528, 10980, 10675, 10034, 8548, -7888, -6766, -5163, 4987, 4036, 3994, 3861, 3665,
				-2689, -2602, 2390, -2348, 2236, -2120, -2069, 2048, -1773, -1595, 1215, -1110, -892, -810, 759, -713,
				-700, 691, 596, 549, 537, 520, -487, -399, -381, 351, -340, 330, 327, -323, 299, 294 };
	}

	private static class nm
	{
		private static final double coeffApprox[] =
		{ 730125.59765, 36524.908822833051, 0.0001337, -1.5E-007, 7.3E-010 };

		private static final double coeffCapE[] =
		{ 1.0, -0.002516, -7.40E-006 };

		private static final double coeffSolarAnomaly[] = new double[]
		{ 2.5534, 35998.960422026496, -2.18E-005, -1.1E-007 };

		private static final double coeffLunarAnomaly[] = new double[]
		{ 201.5643, 477197.67640106793, 0.0107438, 1.239E-005, -5.80E-008 };

		private static final double coeffMoonArgument[] = new double[]
		{ 160.7108, 483200.81131396897, -0.00163410, -2.28E-006,
				1.1E-008 };

		private static final double coeffCapOmega[] =
		{ 124.7746, -1934.13136123, 0.00207, 2.15E-006 };

		private static final byte EFactor[] =
		{ 0, 1, 0, 0, 1, 1, 2, 0, 0, 1, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

		private static final byte solarCoeff[] =
		{ 0, 1, 0, 0, -1, 1, 2, 0, 0, 1, 0, 1, 1, -1, 2, 0, 3, 1, 0, 1, -1, -1, 1, 0 };

		private static final byte lunarCoeff[] =
		{ 1, 0, 2, 0, 1, 1, 0, 1, 1, 2, 3, 0, 0, 2, 1, 2, 0, 1, 2, 1, 1, 1, 3, 4 };

		private static final byte moonCoeff[] =
		{ 0, 0, 0, 2, 0, 0, 0, -2, 2, 0, 0, 2, -2, 0, 0, -2, 0, -2, 2, 2, 2, -2, 0, 0 };

		private static final double sineCoeff[] =
		{ -0.407200, 0.172410, 0.01608, 0.01039, 0.00739,
				-0.00514, 0.00208, -0.00111, -0.00057,
				0.00056, -0.00042, 0.00042, 0.00038,
				-0.000240, -7.0E-005, 4.00E-005,
				4.00E-005, 3.0E-005, 3.0E-005,
				-3.0E-005, 3.0E-005, -2.0E-005,
				-2.0E-005, 2.0E-005 };

		private static final double addConst[] =
		{ 251.88, 251.830, 349.420, 84.66, 141.740,
				207.14, 154.84, 34.52, 207.19, 291.34, 161.72, 239.56,
				331.550 };

		private static final double addCoeff[] =
		{ 0.016321, 26.641886, 36.412478, 18.206239, 53.303771, 2.453732,
				7.30686, 27.261239, 0.121824, 1.844379, 24.198154, 25.513099,
				3.592518 };

		private static final double addFactor[] =
		{ 0.000165, 0.000164, 0.000126, 0.00011, 6.20E-005, 6.0E-005,
				5.6E-005, 4.7E-005, 4.2E-005, 4.00E-005,
				3.7E-005, 3.5E-005, 2.3E-005 };

		private static final double extra[] = new double[]
		{ 299.77, 132.8475848, -0.009173 };
	}

	private static class llat
	{
		private static final double coeffLongitude[] = new double[]
		{ 218.3164591, 481267.88134236, -0.0013268, 1.855835023689734E-006,
				-1.5338834862103876E-008 };

		private static final double coeffElongation[] = new double[]
		{ 297.8502042, 445267.1115168, -0.00163, 1.8319447192361523E-006,
				-8.8444699951355417E-009 };

		private static final double coeffSolarAnomaly[] = new double[]
		{ 357.5291092, 35999.0502909, -0.0001536, 4.0832993058391183E-008 };

		private static final double coeffLunarAnomaly[] = new double[]
		{ 134.9634114, 477198.8676313, 0.008997, 1.4347408140719379E-005,
				-6.7971723762914631E-008 };

		private static final double coeffMoonNode[] = new double[]
		{ 93.2720993, 483202.0175273, -0.0034029, -2.8360748723766307E-007,
				1.1583324645839848E-009 };

		private static final double coeffCapE[] =
		{ 1.0, -0.002516, -7.40E-006 };

		private static final byte argsLunarElongation[] =
		{ 0, 0, 0, 2, 2, 2, 2, 0, 2, 0, 2, 2, 2, 2, 2, 2, 2, 0, 4, 0, 0, 0, 1, 0, 0, 0, 1, 0, 4, 4, 0, 4, 2, 2, 2, 2,
				0, 2, 2, 2, 2, 4, 2, 2, 0, 2, 1, 1, 0, 2, 1, 2, 0, 4, 4, 1, 4, 1, 4, 2 };

		private static final byte argsSolarAnomaly[] =
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 1, -1, -1, -1, 1, 0, 1, 0, 1, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, -1,
				0, 0, 0, 0, 1, 1, 0, -1, -2, 0, 1, 1, 1, 1, 1, 0, -1, 1, 0, -1, 0, 0, 0, -1, -2 };

		private static final byte argsLunarAnomaly[] =
		{ 0, 1, 1, 0, -1, -1, 0, 2, 1, 2, 0, -2, 1, 0, -1, 0, -1, -1, -1, 0, 0, -1, 0, 1, 1, 0, 0, 3, 0, -1, 1, -2, 0,
				2, 1, -2, 3, 2, -3, -1, 0, 0, 1, 0, 1, 1, 0, 0, -2, -1, 1, -2, 2, -2, -1, 1, 1, -2, 0, 0 };

		private static final byte argsMoonNode[] =
		{ 1, 1, -1, -1, 1, -1, 1, 1, -1, -1, -1, -1, 1, -1, 1, 1, -1, -1, -1, 1, 3, 1, 1, 1, -1, -1, -1, 1, -1, 1, -3,
				1, -3, -1, -1, 1, -1, 1, -1, 1, 1, 1, 1, -1, 3, -1, -1, 1, -1, -1, 1, -1, 1, -1, -1, -1, -1, -1, -1, 1 };

		private static final int sineCoefficients[] =
		{ 5128122, 280602, 277693, 173237, 55413, 46271, 32573, 17198, 9266, 8822, 8216, 4324, 4200, -3359, 2463,
				2211, 2065, -1870, 1828, -1794, -1749, -1565, -1491, -1475, -1410, -1344, -1335, 1107, 1021, 833, 777,
				671, 607, 596, 491, -451, 439, 422, 421, -366, -351, 331, 315, 302, -283, -229, 223, 223, -220, -220,
				-185, 181, -177, 176, 166, -164, 132, -119, 115, 107 };
	}

	private static class sfm
	{
		private static final double siderealCoeff[] = new double[]
		{ 280.46061837, 13185000.770053742, 0.000387933, 2.5833118057349522E-8 };
	}

	/** J2000 epoch. */
	private static final double J2000 = hr(12) + (double) new Gregorian(2000, 1, 1).fixed;

	/**
	 * Difference between lunar and solar longitudes in new moon phase.
	 */
	private static final double NEW = 0.0;

	/**
	 * Difference between lunar and solar longitudes in first quarter phase.
	 */
	private static final double FIRST_QUARTER = 90;

	/**
	 * Longitude of the Sun when spring starts.
	 */
	public static final double SPRING = 0.0;

	/**
	 * Longitude of the Sun when summer starts.
	 */
	public static final double SUMMER = 9.0;

	/**
	 * Longitude of the Sun when autumn starts.
	 */
	public static final double AUTUMN = 180;

	/**
	 * Longitude of the Sun when winter starts.
	 */
	public static final double WINTER = 270;

	/**
	 * Jerusalem location.
	 */
	public static final CityElement JERUSALEM = new CityElement("Jerusalem, Israel", 35.2, 31.8, 2, 800);

	/** Length of tropical year in days. */
	public static double TROPICAL_YEAR = 365.242189;
	
	/** Length of lunar cycle in days. */
	public static double LUNAR_CYCLE = 29.530588853;
	
	/**
	 * Returns difference between two dates.
	 *
	 * @param l Date.
	 * @param l1 Date.
	 * @return l1 - l.
	 */
	public static long difference(long l, long l1)
	{
		return l1 - l;
	}

	/**
	 * Returns module result.
	 *
	 * @param d Value.
	 * @param d1 Value.
	 * @return d % d1.
	 */
	public static double mod(double d, double d1)
	{
		return d - d1 * Math.floor(d / d1);
	}

	/**
	 * Returns module result.
	 *
	 * @param i Value.
	 * @param j Value.
	 * @return i % j.
	 */
	public static int mod(int i, int j)
	{
		return (int) ((double) i - (double) j * Math.floor((double) i / (double) j));
	}

	/**
	 * Returns module result.
	 *
	 * @param l Value.
	 * @param l1 Value.
	 * @return l % l1.
	 */
	public static long mod(long l, long l1)
	{
		return (long) ((double) l - (double) l1 * Math.floor((double) l / (double) l1));
	}

	/**
	 * Returns quotient.
	 *
	 * @param d Value.
	 * @param d1 Value.
	 * @return d/d1.
	 */
	public static long quotient(double d, double d1)
	{
		return (long) Math.floor(d / d1);
	}

	/**
	 * Returns adjusted module.
	 *
	 * @param i Value.
	 * @param j Value.
	 * @return j + i % -j.
	 */
	public static int adjustedMod(int i, int j)
	{
		return j + mod(i, -j);
	}

	/**
	 * Returns adjusted module.
	 *
	 * @param l Value.
	 * @param l1 Value.
	 * @return l1 + l % -l1.
	 */
	public static long adjustedMod(long l, long l1)
	{
		return l1 + mod(l, -l1);
	}

	/**
	 * Returns adjusted module.
	 *
	 * @param d Value.
	 * @param d1 Value.
	 * @return d1 + d % -d1.
	 */
	public static double adjustedMod(double d, double d1)
	{
		return d1 + mod(d, -d1);
	}

	/**
	 * Returns day of week from fixed day.
	 *
	 * @param l Fixed date.
	 * @return Day of week.
	 */
	public static long dayOfWeekFromFixed(long l)
	{
		return mod(l, 7L);
	}

	private static long kDayOnOrBefore(long l, int k)
	{
		return l - dayOfWeekFromFixed(l - (long) k);
	}

	/**
	 * Returns nearest k day.
	 *
	 * @param l Fixed day.
	 * @param k K day.
	 * @return Nearest day.
	 */
	public static long kDayNearest(long l, int k)
	{
		return kDayOnOrBefore(l + 3L, k);
	}

	/**
	 * Returns k day after.
	 *
	 * @param l Fixed day.
	 * @param k K day.
	 * @return After day.
	 */
	public static long kDayAfter(long l, int k)
	{
		return kDayOnOrBefore(l + 7L, k);
	}

	private static long kDayBefore(long l, int k)
	{
		return kDayOnOrBefore(l - 1L, k);
	}

	/**
	 * Returns nth K day.
	 *
	 * @param n n. If positive will be before l, otherwise after.
	 * @param k K day
	 * @param l Fixed day.
	 * @return Nth K day.
	 */
	public static long nthKDay(int n, int k, long l)
	{
		if (n > 0)
			return kDayBefore(l, k) + (long) (7 * n);
		else
			return kDayAfter(l, k) + (long) (7 * n);
	}

	/**
	 * Returns first k day.
	 *
	 * @param k K day.
	 * @param l Fixed day.
	 * @return Such day.
	 */
	public static long firstKDay(int k, long l)
	{
		return nthKDay(1, k, l);
	}

	/**
	 * Returns last k day.
	 *
	 * @param k K day.
	 * @param l Fixed day.
	 * @return Such day.
	 */
	public static long lastKDay(int k, long l)
	{
		return nthKDay(-1, k, l);
	}

	/**
	 * Returns the sign of a number.
	 * @param d The number.
	 * @return -1 if it is negative, 1 if it is positive, and 0 if it is 0.
	 */
	public static int signum(double d)
	{
		if (d < 0.0)
			return -1;
		return d <= 0.0 ? 0 : 1;
	}

	private static double square(double d)
	{
		return d * d;
	}

	private static double poly(double d, double ad[])
	{
		double d1 = ad[0];
		for (int i = 1; i < ad.length; i++)
			d1 += ad[i] * Math.pow(d, i);

		return d1;
	}

	private static double hr(double d)
	{
		return d / 24.0;
	}

	/**
	 * Returns degrees.
	 *
	 * @param d Degrees.
	 * @param d1 Minutes.
	 * @param d2 Arcseconds.
	 * @return Degrees.
	 */
	public static double angle(double d, double d1, double d2)
	{
		return d + (d1 + d2 / 60.0) / 60.0;
	}

	/**
	 * Sin function using input angle in degrees.
	 *
	 * @param d Angle.
	 * @return Sine.
	 */
	public static double sinDegrees(double d)
	{
		return Math.sin(Constant.DEG_TO_RAD * d);
	}

	private static double cosDegrees(double d)
	{
		return Math.cos(Constant.DEG_TO_RAD * d);
	}

	private static double tanDegrees(double d)
	{
		return Math.tan(Constant.DEG_TO_RAD * d);
	}

	private static double arcTanDegrees(double d, int i)
	{
		double d1 = Constant.RAD_TO_DEG * Math.atan(d);
		return mod(i != 1 && i != 4 ? d1 + 180 : d1, 360);
	}

	private static double arcSinDegrees(double d)
	{
		return Constant.RAD_TO_DEG * Math.asin(d);
	}

	private static double arcCosDegrees(double d)
	{
		return Constant.RAD_TO_DEG * Math.acos(d);
	}

	private static double momentFromJD(double d)
	{
		return d - 1721424.5;
	}

	/**
	 * Returns fixed date from Julian day.
	 *
	 * @param d Julian day.
	 * @return Fixed date.
	 */
	public static long fixedFromJD(double d)
	{
		return (long) Math.floor(momentFromJD(d));
	}

	/**
	 * Returns Julian day from a fixed day.
	 *
	 * @param l Fixed day.
	 * @return Julian day.
	 */
	 public static double jdFromFixed(long l) { return jdFromMoment(l); }

	 private static double jdFromMoment(double d) {return d +1721424.5; }

	private static double julianCenturies(double d)
	{
		return (dynamicalFromUniversal(d) - J2000) / 36525.0;
	}

	private static double obliquity(double d)
	{
		double d1 = julianCenturies(d);
		return angle(23, 26, 21.448) + poly(d1, ob.coeffObliquity);
	}

	private static double universalFromDynamical(double d)
	{
		return d - ephemerisCorrection(d);
	}

	private static double dynamicalFromUniversal(double d)
	{
		return d + ephemerisCorrection(d);
	}

	private static double ephemerisCorrection(double d)
	{
		long l = Gregorian.yearFromFixed((long) Math.floor(d));
		double d1 = (double) difference(new Gregorian(1900, 1, 1).fixed, new Gregorian(l, 7, 1).fixed) / Constant.JULIAN_DAYS_PER_CENTURY;
		double d2;
		if (l >= 1988L && l <= 2019L)
			d2 = (double) (l - 1933L) / Constant.SECONDS_PER_DAY;
		else if (l >= 1900L && l <= 1987L)
			d2 = poly(d1, ec.coeff19th);
		else if (l >= 1800L && l <= 1899L)
			d2 = poly(d1, ec.coeff18th);
		else if (l >= 1700L && l <= 1799L)
			d2 = poly(l - 1700L, ec.coeff17th) / Constant.SECONDS_PER_DAY;
		else if (l >= 1620L && l <= 1699L)
		{
			d2 = poly(l - 1600L, ec.coeff16th) / Constant.SECONDS_PER_DAY;
		} else
		{
			double d3 = hr(12) + (double) difference(new Gregorian(1810, 1, 1).fixed, new Gregorian(l, 1, 1).fixed);
			return ((d3 * d3) / 41048480.0 - 15.0) / Constant.SECONDS_PER_DAY;
		}
		return d2;
	}

	private static double equationOfTime(double d)
	{
		double d1 = julianCenturies(d);
		double d2 = poly(d1, et.coeffLongitude);
		double d3 = poly(d1, et.coeffAnomaly);
		double d4 = poly(d1, et.coeffEccentricity);
		double d5 = obliquity(d);
		double d6 = square(tanDegrees(d5 / 2.0));
		double d7 = 0.15915494309189535 * (d6 * sinDegrees(2.0 * d2) + -2.0 * d4 * sinDegrees(d3) + 4.0 * d4 * d6 * sinDegrees(d3) * cosDegrees(2.0 * d2) - 0.5 * d6 * d6 * sinDegrees(4 * d2) - 1.25 * d4 * d4 * sinDegrees(2.0 * d3));
		return (double) signum(d7) * Math.min(Math.abs(d7), hr(12));
	}

	private static double localFromApparent(double d)
	{
		return d - equationOfTime(d);
	}

	/**
	 * Apparent time from local.
	 *
	 * @param d Julian day.
	 * @return Equation of time.
	 */
	public static double apparentFromLocal(double d)
	{
		return d + equationOfTime(d);
	}

	/**
	 * Solar longitude.
	 *
	 * @param d Julian day.
	 * @return Solar longitude.
	 */
	public static double solarLongitude(double d)
	{
		double d1 = julianCenturies(d);
		double d2 = 0.0;
		for (int i = 0; i < sl.coefficients.length; i++)
			d2 += (double) sl.coefficients[i] * sinDegrees(sl.multipliers[i] * d1 + sl.addends[i]);

		double d3 = 282.7771834 + 36000.76953744 * d1 + 5.7295779513082322E-006 * d2;
		return mod(d3 + aberration(d) + nutation(d), 360.0);
	}

	private static double nutation(double d)
	{
		double d1 = julianCenturies(d);
		double d2 = poly(d1, nu.coeffa);
		double d3 = poly(d1, nu.coeffb);
		return -0.004778 * sinDegrees(d2) + -0.0003667 * sinDegrees(d3);
	}

	private static double aberration(double d)
	{
		double d1 = julianCenturies(d);
		return 9.74E-5 * cosDegrees(177.63 + 35999.01848 * d1) - 0.005575;
	}

	private static double lunarLongitude(double d)
	{
		double d1 = julianCenturies(d);
		double d2 = poly(d1, llon.coeffMeanMoon);
		double d3 = poly(d1, llon.coeffElongation);
		double d4 = poly(d1, llon.coeffSolarAnomaly);
		double d5 = poly(d1, llon.coeffLunarAnomaly);
		double d6 = poly(d1, llon.coeffMoonNode);
		double d7 = poly(d1, llon.coeffCapE);
		double d8 = 0.0;
		for (int i = 0; i < llon.argsLunarElongation.length; i++)
		{
			double d9 = llon.argsSolarAnomaly[i];
			d8 += (double) llon.sineCoefficients[i] * Math.pow(d7, Math.abs(d9)) * sinDegrees((double) llon.argsLunarElongation[i] * d3 + d9 * d4 + (double) llon.argsLunarAnomaly[i] * d5 + (double) llon.argsMoonFromNode[i] * d6);
		}

		double d10 = (1.0 / 1000000.0) * d8;
		double d11 = (3958.0 / 1000000.0) * sinDegrees(119.75 + d1 * 131.849);
		double d12 = (318.0 / 1000000.0) * sinDegrees(53.09 + d1 * 479264.29);
		double d13 = (1962.0 / 1000000.0) * sinDegrees(d2 - d6);
		return mod(d2 + d10 + d11 + d12 + d13 + nutation(d), 360.0);
	}

	private static double nthNewMoon(long l)
	{
		double d = l - 24724L;
		double d1 = d / 1236.85;
		double d2 = poly(d1, nm.coeffApprox);
		double d3 = poly(d1, nm.coeffCapE);
		double d4 = poly(d1, nm.coeffSolarAnomaly);
		double d5 = poly(d1, nm.coeffLunarAnomaly);
		double d6 = poly(d1, nm.coeffMoonArgument);
		double d7 = poly(d1, nm.coeffCapOmega);
		double d8 = -0.00017 * sinDegrees(d7);
		for (int i = 0; i < nm.sineCoeff.length; i++)
			d8 += nm.sineCoeff[i] * Math.pow(d3, nm.EFactor[i]) * sinDegrees((double) nm.solarCoeff[i] * d4 + (double) nm.lunarCoeff[i] * d5 + (double) nm.moonCoeff[i] * d6);

		double d9 = 0.0;
		for (int j = 0; j < nm.addConst.length; j++)
			d9 += nm.addFactor[j] * sinDegrees(nm.addConst[j] + nm.addCoeff[j] * d);

		double d10 = 0.000325 * sinDegrees(poly(d1, nm.extra));
		return universalFromDynamical(d2 + d8 + d10 + d9);
	}

	private static double lunarPhase(double d)
	{
		return mod(lunarLongitude(d) - solarLongitude(d), 360);
	}

	private static double lunarLatitude(double d)
	{
		double d1 = julianCenturies(d);
		double d2 = poly(d1, llat.coeffLongitude);
		double d3 = poly(d1, llat.coeffElongation);
		double d4 = poly(d1, llat.coeffSolarAnomaly);
		double d5 = poly(d1, llat.coeffLunarAnomaly);
		double d6 = poly(d1, llat.coeffMoonNode);
		double d7 = poly(d1, llat.coeffCapE);
		double d8 = 0.0;
		for (int i = 0; i < llat.argsLunarElongation.length; i++)
		{
			double d9 = llat.argsSolarAnomaly[i];
			d8 += (double) llat.sineCoefficients[i] * Math.pow(d7, Math.abs(d9)) * sinDegrees((double) llat.argsLunarElongation[i] * d3 + d9 * d4 + (double) llat.argsLunarAnomaly[i] * d5 + (double) llat.argsMoonNode[i] * d6);
		}

		d8 /= 1000000.0;
		double d10 = (175.0 / 1000000.0) * (sinDegrees(119.75 + d1 * 131.849 + d6) + sinDegrees((119.75 + d1 * 131.849) - d6));
		double d11 = (-2235.0 / 1000000.0) * sinDegrees(d2) + (127.0 / 1000000.0) * sinDegrees(d2 - d5) + (-115.0 / 1000000.0) * sinDegrees(d2 + d5);
		double d12 = (382.0 / 1000000.0) * sinDegrees(313.45 + d1 * 481266.484);
		return mod(d8 + d10 + d11 + d12, 360);
	}

	private static double siderealFromMoment(double d)
	{
		double d1 = (d - J2000) / 36525.0;
		return mod(poly(d1, sfm.siderealCoeff), 360.0);
	}

	private static double dawn(long l, CityElement location, double d) throws JPARSECException
	{
		double d1;
		try
		{
			d1 = momentFromDepression((double) l + 0.25, location, d);
		} catch (Exception _ex)
		{
			d1 = l;
		}
		double d2 = momentFromDepression(d1, location, d);
		return standardFromLocal(d2, location);
	}

	private static double dusk(long l, CityElement location, double d) throws JPARSECException
	{
		double d1;
		try
		{
			d1 = momentFromDepression((double) l + 0.75, location, d);
		} catch (Exception _ex)
		{
			d1 = (double) l + 0.99;
		}
		double d2 = momentFromDepression(d1, location, d);
		return standardFromLocal(d2, location);
	}

	private static double momentFromDepression(double d, CityElement location, double d1) throws JPARSECException
	{
		double d2 = location.latitude;
		double d3 = universalFromLocal(d, location);
		double d4 = arcSinDegrees(sinDegrees(obliquity(d3)) * sinDegrees(solarLongitude(d3)));
		boolean flag = mod(d, 1.0) < 0.5;
		double d5 = tanDegrees(d2) * tanDegrees(d4) + sinDegrees(d1) / (cosDegrees(d4) * cosDegrees(d2));
		double d6 = mod(0.5 + arcSinDegrees(d5) / 360.0, 1.0) - 0.5;
		if (Math.abs(d5) > 1.0)
			throw new JPARSECException("Inacceptable result in calculations.");
		else
			return localFromApparent(Math.floor(d) + (flag ? 0.25 - d6 : 0.75 + d6));
	}

	private static boolean visibleCrescent(long l, CityElement location) throws JPARSECException
	{
		double d = universalFromStandard(dusk(l - 1L, location, 4.5), location);
		double d1 = lunarPhase(d);
		double d2 = lunarAltitude(d, location);
		double d3 = arcCosDegrees(cosDegrees(lunarLatitude(d)) * cosDegrees(d1));
		return NEW < d1 && d1 < FIRST_QUARTER && 10.6 <= d3 && d3 <= 90.0 && d2 > 4.1;
	}

	private static double lunarAltitude(double d, CityElement location)
	{
		double d1 = location.latitude;
		double d2 = location.longitude;
		double d3 = obliquity(d);
		double d4 = lunarLongitude(d);
		double d5 = lunarLatitude(d);
		double d6 = arcTanDegrees((sinDegrees(d4) * cosDegrees(d3) - tanDegrees(d5) * sinDegrees(d3)) / cosDegrees(d4),
				(int) quotient(d4, 90.0) + 1);
		double d7 = arcSinDegrees(sinDegrees(d5) * cosDegrees(d3) + cosDegrees(d5) * sinDegrees(d3) * sinDegrees(d4));
		double d8 = siderealFromMoment(d);
		double d9 = mod((d8 + d2) - d6, 360);
		double d10 = arcSinDegrees(sinDegrees(d1) * sinDegrees(d7) + cosDegrees(d1) * cosDegrees(d7) * cosDegrees(d9));
		return mod(d10 + 180.0, 360) - 180.0;
	}

	/**
	 * Gets the time of some solar longitude.
	 *
	 * @param d Fixed day before now, usually the beginning of the year.
	 * @param d1 Longitude of the Sun to calculate. Some constants defined in this
	 * class, like {@linkplain Calendar#SPRING}, {@linkplain Calendar#SUMMER}, and so on.
	 * @return Fixed day when the Sun has the desired longitude in this year.
	 */
	public static double solarLongitudeAfter(double d, double d1)
	{
		double d2 = 1.0E-005;
		double d3 = TROPICAL_YEAR / 360.0;
		double d4 = d + d3 * mod(d1 - solarLongitude(d), 360);
		double d5 = Math.max(d, d4 - 5);
		double d6 = d4 + 5;
		double d7 = d5;
		double d8 = d6;
		double d9;
		for (d9 = (d8 + d7) / 2.0; d8 - d7 > d2; d9 = (d8 + d7) / 2.0)
			if (mod(solarLongitude(d9) - d1, 360) < 180.0)
				d8 = d9;
			else
				d7 = d9;

		return d9;
	}

	/**
	 * Gets previous new Moon.
	 *
	 * @param d Fixed day.
	 * @return New Moon.
	 */
	public static double newMoonBefore(double d)
	{
		double d1 = nthNewMoon(0L);
		double d2 = lunarPhase(d);
		long l = Math.round((d - d1) / LUNAR_CYCLE - d2 / 360.0);
		long l1;
		for (l1 = l - 1L; nthNewMoon(l1) < d; l1++)
			;
		l1--;
		return nthNewMoon(l1);
	}

	/**
	 * Gets next new Moon.
	 *
	 * @param d Fixed day.
	 * @return New Moon.
	 */
	public static double newMoonAfter(double d)
	{
		double d1 = nthNewMoon(0L);
		double d2 = lunarPhase(d);
		long l = Math.round((d - d1) / LUNAR_CYCLE - d2 / 360.0);
		long l1;
		for (l1 = l; nthNewMoon(l1) < d; l1++)
			;
		return nthNewMoon(l1);
	}

	/**
	 * Gets lunar phase.
	 *
	 * @param d Fixed date.
	 * @param d1 Phase.
	 * @return Next lunar phase.
	 */
	public static double lunarPhaseAfter(double d, double d1)
	{
		double d2 = 1.0E-005;
		double d3 = d + 0.082029413480555563 * mod(d1 - lunarPhase(d), 360.0);
		double d4 = Math.max(d, d3 - 2);
		double d5 = d3 + 2;
		double d6 = d4;
		double d7 = d5;
		double d8;
		for (d8 = (d7 + d6) / 2.0; d7 - d6 > d2; d8 = (d7 + d6) / 2.0)
			if (mod(lunarPhase(d8) - d1, 360) < 180.0)
				d7 = d8;
			else
				d6 = d8;

		return d8;
	}

	/**
	 * Returns name from number.
	 *
	 * @param l Number.
	 * @param as Name.
	 * @return Name.
	 */
	public static String nameFromNumber(long l, String as[])
	{
		return as[(int) adjustedMod(l, as.length) - 1];
	}

	/**
	 * Returns name from day of week.
	 *
	 * @param l Day of week.
	 * @param as Name.
	 * @return Name.
	 */
	public static String nameFromDayOfWeek(long l, String as[])
	{
		return nameFromNumber(l + 1L, as);
	}

	/**
	 * Returns name from month.
	 *
	 * @param l Month.
	 * @param as Set of names.
	 * @return Name.
	 */
	public static String nameFromMonth(long l, String as[])
	{
		return nameFromNumber(l, as);
	}

	/**
	 * Returns the index of a name in a given set.
	 * @param name Name.
	 * @param val The set of names.
	 * @return The index, or -1 if match is not found. Match
	 * is performed for the exact entire name or the start of
	 * the name in the set of values, without considering
	 * lower/upper cases.
	 */
	public static int indexFromName(String name, String val[]) {
		if (Translate.getDefaultLanguage() != LANGUAGE.ENGLISH) val = Translate.translate(val);
		int index = DataSet.getIndex(val, name);
		if (index < 0) index = DataSet.getIndexStartingWith(val, name);
		return index;
	}

	/**
	 * Gets the time of certain previous solar longitude.
	 *
	 * @param d Julian day.
	 * @param d1 Solar longitude.
	 * @return Fixed day.
	 */
	public static double estimatePriorSolarLongitude(double d, double d1)
	{
		double d2 = TROPICAL_YEAR / 360.0;
		double d3 = d - d2 * mod(solarLongitude(d) - d1, 360.0);
		double d4 = mod((solarLongitude(d3) - d1) + 180.0, 360.0) - 180.0;
		return Math.min(d, d3 - d2 * d4);
	}

	/**
	 * Transforms time scale.
	 *
	 * @param d Fixed day.
	 * @param location Location.
	 * @return Resulting day.
	 */
	public static double standardFromUniversal(double d, CityElement location)
	{
		return d + location.timeZone / 24.0;
	}

	/**
	 * Transforms time scale.
	 *
	 * @param d Fixed day.
	 * @param location Location.
	 * @return Resulting day.
	 */
	public static double universalFromStandard(double d, CityElement location)
	{
		return d - location.timeZone / 24.0;
	}

	/**
	 * Transforms time scale.
	 *
	 * @param d Fixed day.
	 * @param location Location.
	 * @return Resulting day.
	 */
	public static double localFromUniversal(double d, CityElement location)
	{
		return d + location.longitude / 360.0;
	}

	/**
	 * Transforms time scale.
	 *
	 * @param d Fixed day.
	 * @param location Location.
	 * @return Resulting day.
	 */
	public static double universalFromLocal(double d, CityElement location)
	{
		return d - location.longitude / 360.0;
	}

	/**
	 * Transforms time scale.
	 *
	 * @param d Fixed day.
	 * @param location Location.
	 * @return Resulting day.
	 */
	public static double standardFromLocal(double d, CityElement location)
	{
		return standardFromUniversal(universalFromLocal(d, location), location);
	}

	/**
	 * Transforms time scale.
	 *
	 * @param d Fixed day.
	 * @param location Location.
	 * @return Resulting day.
	 */
	public static double localFromStandard(double d, CityElement location)
	{
		return localFromUniversal(universalFromStandard(d, location), location);
	}

	/**
	 * Gets midday standard time.
	 *
	 * @param l Fixed day.
	 * @param location Location.
	 * @return Midday.
	 */
	public static double midday(long l, CityElement location)
	{
		return standardFromLocal(localFromApparent((double) l + hr(12.0)), location);
	}

	/**
	 * Gets midnight standard time.
	 *
	 * @param l Fixed day.
	 * @param location Location.
	 * @return Midnight.
	 */
	public static double midnight(long l, CityElement location)
	{
		return standardFromLocal(localFromApparent(l), location);
	}

	/**
	 * Gets sunrise time.
	 *
	 * @param l Fixed day.
	 * @param location Location.
	 * @return Sunrise.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double sunrise(long l, CityElement location) throws JPARSECException
	{
		double d = Math.max(0.0, location.height);
		double d1 = 6372000;
		double d2 = arcCosDegrees(d1 / (d1 + d));
		double d3 = angle(0.0, 50, 0.0) + d2;
		return dawn(l, location, d3);
	}

	/**
	 * Gets sunset time.
	 *
	 * @param l Fixed day.
	 * @param location Location.
	 * @return Sunset.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double sunset(long l, CityElement location) throws JPARSECException
	{
		double d = Math.max(0.0, location.height);
		double d1 = 6372000;
		double d2 = arcCosDegrees(d1 / (d1 + d));
		double d3 = angle(0.0, 50, 0.0) + d2;
		return dusk(l, location, d3);
	}

	/**
	 * Gets previous lunar phase.
	 *
	 * @param l Fixed day.
	 * @param location Location.
	 * @return Lunar phase event date.
	 * @throws JPARSECException If an error occurs.
	 */
	public static long phasisOnOrBefore(long l, CityElement location) throws JPARSECException
	{
		long l1 = (long) ((double) l - Math.floor((lunarPhase(l) / 360.0) * LUNAR_CYCLE));
		long l2 = l - l1 > 3L || visibleCrescent(l, location) ? l1 - 2L : l1 - 30L;
		long l3;
		for (l3 = l2; !visibleCrescent(l3, location); l3++)
			;
		return l3;
	}
}
