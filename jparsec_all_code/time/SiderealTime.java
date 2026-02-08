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

import java.math.BigDecimal;

import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.Functions;
import jparsec.ephem.Nutation;
import jparsec.ephem.Obliquity;
import jparsec.ephem.PhysicalParameters;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.moons.MoonPhysicalParameters;
import jparsec.ephem.planets.EphemElement;
import jparsec.math.Constant;
import jparsec.math.FastMath;
import jparsec.observer.ObserverElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.util.Configuration;
import jparsec.util.DataBase;
import jparsec.util.JPARSECException;

/**
 * Calculate mean or apparent sidereal time through different methods. Depending
 * on the ephemeris properties the methods in this class will use algorithms according
 * to different methods or IAU resolutions: IAU 1976, 2000/2006/2009 (the three treated in the same
 * way), WILLIAMS/SIMON 1994 (both treated in the same way), and DE403 method.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class SiderealTime
{
	// private constructor so that this class cannot be instantiated.
	private SiderealTime() {}

	/**
	 * Calculates the Greenwich mean sidereal time.
	 * <P>
	 * This function returns mean Greenwich sidereal time for the given Julian
	 * day in UT1.
	 * <P>
	 * Note: at epoch J2000.0, the 16 decimal precision of IEEE double precision
	 * numbers limits time resolution measured by Julian date to approximately
	 * 50 microseconds. References:
	 * <P>
	 * SOFA subroutine GMST2000.f.
	 * <P>
	 * Capitaine et al., Astronomy & Astrophysics 412, 567-586, EQ. (42).
	 * <P>
	 * James G. Williams, "Contributions to the Earth's obliquity rate,
	 * precession, and nutation," Astron. J. 108, 711-724 (1994).
	 * <P>
	 * J. H. Lieske, T. Lederle, W. Fricke, and B. Morando, "Expressions for the
	 * Precession Quantities Based upon the IAU (1976) System of Astronomical
	 * Constants," Astronomy and Astrophysics 58, 1-16 (1977).
	 * <P>
	 *
	 * @param time Time object containing the date.
	 * @param obs Observer object containing the observer position.
	 * @param eph Ephemeris object.
	 * @return Greenwich mean sidereal time in radians.
	 * @throws JPARSECException If the date is invalid.
	 */
	private static double greenwichMeanSiderealTimeDoublePrecisionMode(TimeElement time, ObserverElement obs, EphemerisElement eph)
			throws JPARSECException
	{
		// Obtain julian day in Universal Time
		double jd = TimeScale.getJD(time, obs, eph, SCALE.UNIVERSAL_TIME_UT1);
		if (lastJD == jd) return lastGMST;

		double gmst = 0.0, msday = 0.0;

		/* Correct Julian day to express it refered to the previous midnight */
		double jd0 = Math.floor(jd - 0.5) + 0.5;

		/* Julian centuries from J2000 at 0h UT */
		double T0 = (jd0 - Constant.J2000) / Constant.JULIAN_DAYS_PER_CENTURY;

		/* Obtain seconds elapsed from midnight */
		double secs = (jd - jd0) * (double) Constant.SECONDS_PER_DAY;

		if (eph.ephemMethod == EphemerisElement.REDUCTION_METHOD.LASKAR_1986)
		{
			/* This is the 1976 IAU formula. */
			gmst = ((-6.2e-6 * T0 + 9.3104e-2) * T0 + 8640184.812866) * T0 + 24110.54841;

			/* mean solar days per sidereal day at date T0 */
			msday = 1.0 + ((-1.86e-5 * T0 + 0.186208) * T0 + 8640184.812866) / (86400. * 36525.);
		} else {
			if (eph.ephemMethod.isIAU20xx()) {

				// Compute Earth rotation angle
				double DT0 = jd - Constant.J2000;
				gmst = 2.0 * Math.PI * (secs / Constant.SECONDS_PER_DAY + 0.5 + 0.7790572732640 + (Constant.SIDEREAL_DAY_LENGTH - 1.0) * DT0);

				// Obtain julian day in Dynamical Time
				double jd_tdb = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
				double DT = (jd_tdb - Constant.J2000) / Constant.JULIAN_DAYS_PER_CENTURY;

				if (eph.ephemMethod != EphemerisElement.REDUCTION_METHOD.IAU_2000) {
					// Precession contributions from Capitaine et al. 2005
					gmst += (0.014506 + (4612.156534 + (+1.3915817 + (-0.00000044 + (-0.000029956 + (-0.0000000368) * DT) * DT) * DT) * DT) * DT) * Constant.ARCSEC_TO_RAD;
				} else {
					// Precession contributions from Capitaine et al. 2003
					gmst += (0.014506 + (4612.15739966 + (+1.39667721 + (-0.00009344 + (+0.00001882) * DT) * DT) * DT) * DT) * Constant.ARCSEC_TO_RAD;
				}
				lastJD = jd;
				lastGMST = Functions.normalizeRadians(gmst);
				return lastGMST;
			} else {
				/*
				 * J. G. Williams, "Contributions to the Earth's obliquity rate,
				 * precession, and nutation," Astronomical Journal 108, p. 711
				 * (1994)
				 */
				gmst = (((-2.0e-6 * T0 - 3.e-7) * T0 + 9.27695e-2) * T0 + 8640184.7928613) * T0 + 24110.54841;

				/* mean solar (er, UT) days per sidereal day at date T0 */
				msday = (((-(4. * 2.0e-6) * T0 - (3. * 3.e-7)) * T0 + (2. * 9.27695e-2)) * T0 + 8640184.7928613) / (86400. * 36525.) + 1.0;
			}
		}

		/* Greenwich mean sidereal time at given UT */
		gmst = gmst + msday * secs;

		/* To radians */
		gmst = Functions.normalizeRadians(gmst * (15.0 / 3600.0) * Constant.DEG_TO_RAD);

		lastJD = jd;
		lastGMST = gmst;
		return lastGMST;
	}

	/**
	 * Calculates the Greenwich mean sidereal time.
	 * <P>
	 * This function returns mean Greenwich sidereal time for the given Julian
	 * day in UT1.
	 * <P>
	 * References:
	 * <P>
	 * SOFA subroutine GMST2000.f.
	 * <P>
	 * Capitaine et al., Astronomy &amp; Astrophysics 412, 567-586, EQ. (42).
	 * <P>
	 * James G. Williams, "Contributions to the Earth's obliquity rate,
	 * precession, and nutation," Astron. J. 108, 711-724 (1994).
	 * <P>
	 * J. H. Lieske, T. Lederle, W. Fricke, and B. Morando, "Expressions for the
	 * Precession Quantities Based upon the IAU (1976) System of Astronomical
	 * Constants," Astronomy and Astrophysics 58, 1-16 (1977).
	 *
	 * @param time Time object containing the date.
	 * @param obs Observer object containing the observer position.
	 * @param eph Ephemeris object.
	 * @return Greenwich mean sidereal time in radians.
	 * @throws JPARSECException If the date is invalid.
	 */
	public static double greenwichMeanSiderealTime(TimeElement time, ObserverElement obs, EphemerisElement eph)
			throws JPARSECException
	{
		if (!eph.preferPrecisionInEphemerides)
			return greenwichMeanSiderealTimeDoublePrecisionMode(time, obs, eph);

		// Obtain julian day in Universal Time
		BigDecimal jd = TimeScale.getExactJD(time, obs, eph, SCALE.UNIVERSAL_TIME_UT1);
		if (lastBigDecimal != null && lastBigDecimal.doubleValue() == jd.doubleValue()) return lastBigGMST;

		BigDecimal gmst = new BigDecimal(0.0);
		BigDecimal msday = new BigDecimal(0.0);

		/* Correct Julian day to express it referred to the previous midnight */
		double jd0 = Math.floor(jd.doubleValue() - 0.5) + 0.5;

		/* Julian centuries from J2000 at 0h UT */
		BigDecimal T0 = Functions.toCenturies(new BigDecimal(jd0));

		/* Obtain seconds elapsed from midnight */
		BigDecimal secs = (jd.subtract(new BigDecimal(jd0))).multiply(new BigDecimal(Constant.SECONDS_PER_DAY));

		if (eph.ephemMethod == EphemerisElement.REDUCTION_METHOD.LASKAR_1986 || eph.ephemMethod == EphemerisElement.REDUCTION_METHOD.IAU_1976)
		{
			/* This is the 1976 IAU formula. */
			gmst = (((((new BigDecimal(-6.2e-6).multiply(T0)).add(new BigDecimal(9.3104e-2))).multiply(T0)).add(new BigDecimal(8640184.812866))).multiply(T0)).add(new BigDecimal(24110.54841));

			/* mean solar days per sidereal day at date T0 */
			msday = new BigDecimal(1.0).add(((((new BigDecimal(-1.86e-5).multiply(T0)).add(new BigDecimal(0.186208))).multiply(T0)).add(new BigDecimal(8640184.812866))).divide(new BigDecimal(Constant.SECONDS_PER_DAY * Constant.JULIAN_DAYS_PER_CENTURY), Configuration.BIG_DECIMAL_PRECISION_DECIMAL_PLACES, Configuration.BIG_DECIMAL_PRECISION_ROUNDING_MODE));
		} else {
			if (eph.ephemMethod.isIAU20xx()) {
				// Compute Earth rotation angle
				BigDecimal DT0 = jd.subtract(new BigDecimal(Constant.J2000));
				gmst = Constant.BIG_TWO_PI.multiply(((secs.divide(new BigDecimal(Constant.SECONDS_PER_DAY), Configuration.BIG_DECIMAL_PRECISION_DECIMAL_PLACES, Configuration.BIG_DECIMAL_PRECISION_ROUNDING_MODE)).add(new BigDecimal(1.2790572732640))).add(new BigDecimal(Constant.SIDEREAL_DAY_LENGTH - 1.0).multiply(DT0)));

				// Obtain julian day in Dynamical Time
				double DT = Functions.toCenturies(TimeScale.getExactJD(time, obs, eph, SCALE.TERRESTRIAL_TIME)).doubleValue();
				if (eph.ephemMethod != EphemerisElement.REDUCTION_METHOD.IAU_2000) {
					// Precession contributions from Capitaine et al. 2005
					gmst = gmst.add(new BigDecimal((0.014506 + (4612.156534 + (+1.3915817 + (-0.00000044 + (-0.000029956 + (-0.0000000368) * DT) * DT) * DT) * DT) * DT) * Constant.ARCSEC_TO_RAD));
				} else {
					// Precession contributions from Capitaine et al. 2003
					gmst = gmst.add(new BigDecimal((0.014506 + (4612.15739966 + (+1.39667721 + (-0.00009344 + (+0.00001882) * DT) * DT) * DT) * DT) * Constant.ARCSEC_TO_RAD));
				}

				lastBigDecimal = jd;
				lastBigGMST = Functions.normalizeRadians(gmst).doubleValue();
				return lastBigGMST;
			} else {
				if (eph.ephemMethod == EphemerisElement.REDUCTION_METHOD.WILLIAMS_1994 ||
						eph.ephemMethod == EphemerisElement.REDUCTION_METHOD.SIMON_1994) {
					/*
					 * J. G. Williams, "Contributions to the Earth's obliquity rate,
					 * precession, and nutation," Astronomical Journal 108, p. 711
					 * (1994)
					 */
					gmst = (((((((T0.multiply(new BigDecimal(-2.0e-6))).add(new BigDecimal(-3.e-7))).multiply(T0)).add(new BigDecimal(9.27695e-2))).multiply(T0)).add(new BigDecimal(8640184.7928613))).multiply(T0)).add(new BigDecimal(24110.54841));

					/* mean solar (er, UT) days per sidereal day at date T0 */
					msday = (((((((T0.multiply(new BigDecimal(-8.0e-6))).add(new BigDecimal(-9.e-7))).multiply(T0)).add(new BigDecimal(2. * 9.27695e-2))).multiply(T0)).add(new BigDecimal(8640184.7928613))).divide(new BigDecimal(Constant.SECONDS_PER_DAY * Constant.JULIAN_DAYS_PER_CENTURY), Configuration.BIG_DECIMAL_PRECISION_DECIMAL_PLACES, Configuration.BIG_DECIMAL_PRECISION_ROUNDING_MODE)).add(new BigDecimal(1.0));
				} else {
					/*
					 * Williams values updated to DE403
					 */
					gmst = (((((((T0.multiply(new BigDecimal(-2.0e-6))).add(new BigDecimal(-3.e-7))).multiply(T0)).add(new BigDecimal(9.27701e-2))).multiply(T0)).add(new BigDecimal(8640184.7942063))).multiply(T0)).add(new BigDecimal(24110.54841));

					/* mean solar (er, UT) days per sidereal day at date T0 */
					msday = (((((((T0.multiply(new BigDecimal(-8.0e-6))).add(new BigDecimal(-9.e-7))).multiply(T0)).add(new BigDecimal(2. * 9.27701e-2))).multiply(T0)).add(new BigDecimal(8640184.7942063))).divide(new BigDecimal(Constant.SECONDS_PER_DAY * Constant.JULIAN_DAYS_PER_CENTURY), Configuration.BIG_DECIMAL_PRECISION_DECIMAL_PLACES, Configuration.BIG_DECIMAL_PRECISION_ROUNDING_MODE)).add(new BigDecimal(1.0));
				}
			}
		}

		/* Greenwich mean sidereal time at given UT */
		gmst = gmst.add(msday.multiply(secs));

		/* To radians */
		gmst = gmst.multiply(new BigDecimal((15.0 / 3600.0) * Constant.DEG_TO_RAD));
		gmst = Functions.normalizeRadians(gmst);

		lastBigDecimal = jd;
		lastBigGMST = gmst.doubleValue();
		return lastBigGMST;
	}
	private static BigDecimal lastBigDecimal;
	private static double lastJD;
	private static double lastBigGMST, lastGMST;

	/**
	 * Returns apparent sidereal time of the observer. The mother object set in the observer 
	 * object can be different from Earth, in that case the sidereal time for that body will 
	 * be returned, but you may want to correct the value for the planet vernal equinox using 
	 * {@linkplain Ephem#getZeroRightAscension(TimeElement, ObserverElement, EphemerisElement, double)}. 
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @return Apparent Sidereal Time in radians.
	 * @throws JPARSECException If the date is invalid.
	 */
	public static double apparentSiderealTime(TimeElement time, ObserverElement obs, EphemerisElement eph)
			throws JPARSECException
	{
		if (obs.getMotherBody() == TARGET.EARTH) {
			// Obtain local apparent sidereal time
			double lst = SiderealTime.greenwichMeanSiderealTime(time, obs, eph) + obs.getLongitudeRad() + SiderealTime
					.equationOfEquinoxes(time, obs, eph);
			return lst;
		} else {
			EphemerisElement ephIn = eph.clone();
			ephIn.targetBody = obs.getMotherBody();
			double JD_TDB = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
			double st = 0;
			if (obs.getMotherBody().isNaturalSatellite()) {
				st = MoonPhysicalParameters.getBodySiderealTimeAt0Lon(JD_TDB, ephIn);
				return st - obs.getLongitudeRad() + Math.PI;
			} else {
				st = PhysicalParameters.getBodySiderealTimeAt0Lon(JD_TDB, ephIn);
				return st - obs.getLongitudeRad() + Math.PI;
			}
		}
	}

	/**
	 * Returns apparent sidereal time for Greenwich.
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @return Apparent Sidereal Time in radians.
	 * @throws JPARSECException If the date is invalid.
	 */
	public static double greenwichApparentSiderealTime(TimeElement time, ObserverElement obs, EphemerisElement eph)
			throws JPARSECException
	{
		// Obtain apparent sidereal time
		double lst = SiderealTime.greenwichMeanSiderealTime(time, obs, eph) + SiderealTime
				.equationOfEquinoxes(time, obs, eph);

		return lst;
	}

	/**
	 * Returns the equation of time for a given instant.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object. If defined, the algorithm, set of reduction
	 * algorithms, and the geocentric/topocentric flag are taken from it. Default
	 * values are Moshier, IAU 2006, and geocentric. The correction flags to
	 * optimize for speed or accuracy are also considered.
	 * @return Equation of time in radians.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double equationOfTime(TimeElement time, ObserverElement obs, EphemerisElement eph)
	throws JPARSECException {
		double t = Functions.toCenturies(TimeScale.getExactJD(time, obs, eph, SCALE.TERRESTRIAL_TIME)).doubleValue();
		double lon = Functions.normalizeDegrees(280.4664567 + 36000.76982779 * t + .0003032028 * t * t + t * t * t / 49931000.0 -
				t * t * t * t / 152990000.0 - t * t * t * t * t / 198800000000.0) * Constant.DEG_TO_RAD;

		EphemerisElement eph2= new EphemerisElement(TARGET.SUN, EphemerisElement.COORDINATES_TYPE.APPARENT,
				EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.GEOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2009,
				EphemerisElement.FRAME.FK5, EphemerisElement.ALGORITHM.MOSHIER);
		if (eph.algorithm != null) eph2.algorithm = eph.algorithm;
		if (eph.ephemMethod != null) eph2.ephemMethod = eph.ephemMethod;
		if (eph.isTopocentric != eph2.isTopocentric) eph2.isTopocentric = eph.isTopocentric;
		eph2.correctEOPForDiurnalSubdiurnalTides = eph.correctEOPForDiurnalSubdiurnalTides;
		eph2.correctEquatorialCoordinatesForRefraction = eph.correctEquatorialCoordinatesForRefraction;
		eph2.correctForEOP = eph.correctForEOP;
		eph2.correctForExtinction = eph.correctForExtinction;
		eph2.correctForPolarMotion = eph.correctForPolarMotion;
		EphemElement ephem = Ephem.getEphemeris(time, obs, eph2, false);

		Nutation.calcNutation(t, eph2);
		double eps = Obliquity.trueObliquity(t, eph2);

		// See Meeus, chapter 27
		double eqTime = lon - 0.0057183 * Constant.DEG_TO_RAD - ephem.rightAscension + Nutation.getNutationInLongitude() * Math.cos(eps);
		eqTime = Functions.normalizeRadians(eqTime);
		if (eqTime > Math.PI) eqTime -= Constant.TWO_PI;

		return eqTime;
	}

	/**
	 * Returns the equation of time and the declination of the Sun for a given instant.
	 * These are the values required to construct an analemma chart.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object. If defined, the algorithm, set of reduction
	 * algorithms, and the geocentric/topocentric flag are taken from it. Default
	 * values are Moshier, IAU 2006, and geocentric. The correction flags to
	 * optimize for speed or accuracy are also considered.
	 * @return Equation of time and declination of the Sun in radians.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double[] equationOfTimeAndSunDeclination(TimeElement time, ObserverElement obs, EphemerisElement eph)
	throws JPARSECException {
		double t = Functions.toCenturies(TimeScale.getExactJD(time, obs, eph, SCALE.TERRESTRIAL_TIME)).doubleValue();
		double lon = Functions.normalizeDegrees(280.4664567 + 36000.76982779 * t + .0003032028 * t * t + t * t * t / 49931000.0 -
				t * t * t * t / 152990000.0 - t * t * t * t * t / 198800000000.0) * Constant.DEG_TO_RAD;

		EphemerisElement eph2= new EphemerisElement(TARGET.SUN, EphemerisElement.COORDINATES_TYPE.APPARENT,
				EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.GEOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2009,
				EphemerisElement.FRAME.FK5, EphemerisElement.ALGORITHM.MOSHIER);
		if (eph.algorithm != null) eph2.algorithm = eph.algorithm;
		if (eph.ephemMethod != null) eph2.ephemMethod = eph.ephemMethod;
		if (eph.isTopocentric != eph2.isTopocentric) eph2.isTopocentric = eph.isTopocentric;
		eph2.correctEOPForDiurnalSubdiurnalTides = eph.correctEOPForDiurnalSubdiurnalTides;
		eph2.correctEquatorialCoordinatesForRefraction = eph.correctEquatorialCoordinatesForRefraction;
		eph2.correctForEOP = eph.correctForEOP;
		eph2.correctForExtinction = eph.correctForExtinction;
		eph2.correctForPolarMotion = eph.correctForPolarMotion;
		EphemElement ephem = Ephem.getEphemeris(time, obs, eph2, false);

		Nutation.calcNutation(t, eph2);
		double eps = Obliquity.trueObliquity(t, eph2);

		// See Meeus, chapter 27
		double eqTime = lon - 0.0057183 * Constant.DEG_TO_RAD - ephem.rightAscension + Nutation.getNutationInLongitude() * Math.cos(eps);
		eqTime = Functions.normalizeRadians(eqTime);
		if (eqTime > Math.PI) eqTime -= Constant.TWO_PI;

		return new double[] {eqTime, ephem.declination};
	}

	/**
	 * Returns equation of equinoxes. Complementary terms are included in case
	 * of IAU 2000/2006/2009 reduction methods. In case the ephemeris properties
	 * are set with the flag {@linkplain EphemerisElement#preferPrecisionInEphemerides}
	 * disabled (and the date is between years -3000 to +3000), a low precision but
	 * fast method is used to return the equation of equinoxes with an accuracy around 1 ms.
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @return Equation of Equinoxes in radians.
	 * @throws JPARSECException If the date is invalid.
	 */
	public static double equationOfEquinoxes(TimeElement time, ObserverElement obs, EphemerisElement eph)
			throws JPARSECException
	{
		if (!eph.preferPrecisionInEphemerides && Math.abs(time.astroDate.getYear()) < 3000) {
			// See Henning Umland's page at http://www2.arnes.si/~gljsentvid10/longterm.htm
			double JDE = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
			double TE = (JDE - 2451545.0) / 36525.0;
			double TE2 = TE * TE;
			double TE3 = TE * TE2;

			// Mean elongation of the moon
			double D = 297.85036 + 445267.111480 * TE - 0.0019142 * TE2 + TE3 / 189474;
			D = Functions.normalizeDegrees(D);

			// Mean anomaly of the sun
			double M = 357.52772 + 35999.050340 * TE - 0.0001603 * TE2 - TE3 / 300000;
			M = Functions.normalizeDegrees(M);

			// Mean anomaly of the moon
			double Mm = 134.96298 + 477198.867398 * TE + 0.0086972 * TE2 + TE3 / 56250;
			Mm = Functions.normalizeDegrees(Mm);

			// Mean distance of the moon from ascending node
			double F = 93.27191 + 483202.017538 * TE - 0.0036825 * TE2 + TE3 / 327270;
			F = Functions.normalizeDegrees(F);

			// Longitude of the ascending node of the moon
			double omega = 125.04452 - 1934.136261 * TE + 0.0020708 * TE2 + TE3 / 450000;
			omega = Functions.normalizeDegrees(omega);

			D *= Constant.DEG_TO_RAD;
			M *= Constant.DEG_TO_RAD;
			Mm *= Constant.DEG_TO_RAD;
			F *= Constant.DEG_TO_RAD;
			omega *= Constant.DEG_TO_RAD;

			// Periodic terms for nutation
			double dp = (-171996 - 174.2 * TE) * FastMath.sin(omega);
			double de = (92025 + 8.9 * TE) * FastMath.cos(omega);

			dp += (-13187 - 1.6 * TE) * FastMath.sin(-2 * D + 2 * F + 2 * omega);
			de += (5736 - 3.1 * TE) * FastMath.cos(-2 * D + 2 * F + 2 * omega);
			dp += (-2274 - 0.2 * TE) * FastMath.sin(2 * F + 2 * omega);
			de += (977 - 0.5 * TE) * FastMath.cos(2 * F + 2 * omega);
			dp += (2062 + 0.2 * TE) * FastMath.sin(2 * omega);
			de += (-895 + 0.5 * TE) * FastMath.cos(2 * omega);
			dp += (1426 - 3.4 * TE) * FastMath.sin(M);
			de += (54 - 0.1 * TE) * FastMath.cos(M);
			dp += (712 + 0.1 * TE) * FastMath.sin(Mm);
			de += -7 * FastMath.cos(Mm);
			dp += (-517 + 1.2 * TE)	* FastMath.sin(-2 * D + M + 2 * F + 2 * omega);
			de += (224 - 0.6 * TE) * FastMath.cos(-2 * D + M + 2 * F + 2 * omega);
			dp += (-386 - 0.4 * TE) * FastMath.sin(2 * F + omega);
			de += 200 * FastMath.cos(2 * F + omega);
			dp += -301 * FastMath.sin(Mm + 2 * F + 2 * omega);
			de += (129 - 0.1 * TE) * FastMath.cos(Mm + 2 * F + 2 * omega);
			dp += (217 - 0.5 * TE) * FastMath.sin(-2 * D - M + 2 * F + 2 * omega);
			de += (-95 + 0.3 * TE) * FastMath.cos(-2 * D - M + 2 * F + 2 * omega);
			dp += -158 * FastMath.sin(-2 * D + Mm);
			dp += (129 + 0.1 * TE) * FastMath.sin(-2 * D + 2 * F + omega);
			de += -70 * FastMath.cos(-2 * D + 2 * F + omega);
			dp += 123 * FastMath.sin(-Mm + 2 * F + 2 * omega);
			de += -53 * FastMath.cos(-Mm + 2 * F + 2 * omega);
			dp += 63 * FastMath.sin(2 * D);
			dp += (63 + 0.1 * TE) * FastMath.sin(Mm + omega);
			de += -33 * FastMath.cos(Mm + omega);
			dp += -59 * FastMath.sin(2 * D - Mm + 2 * F + 2 * omega);
			de += 26 * FastMath.cos(2 * D - Mm + 2 * F + 2 * omega);
			dp += (-58 - 0.1 * TE) * FastMath.sin(-Mm + omega);
			de += 32 * FastMath.cos(-Mm + omega);
			dp += -51 * FastMath.sin(Mm + 2 * F + omega);
			de += 27 * FastMath.cos(Mm + 2 * F + omega);
			dp += 48 * FastMath.sin(-2 * D + 2 * Mm);
			dp += 46 * FastMath.sin(-2 * Mm + 2 * F + omega);
			de += -24 * FastMath.cos(-2 * Mm + 2 * F + omega);
			dp += -38 * FastMath.sin(2 * D + 2 * F + 2 * omega);
			de += 16 * FastMath.cos(2 * D + 2 * F + 2 * omega);
			dp += -31 * FastMath.sin(2 * Mm + 2 * F + 2 * omega);
			de += 13 * FastMath.cos(2 * Mm + 2 * F + 2 * omega);
			dp += 29 * FastMath.sin(2 * Mm);
			dp += 29 * FastMath.sin(-2 * D + Mm + 2 * F + 2 * omega);
			de += -12 * FastMath.cos(-2 * D + Mm + 2 * F + 2 * omega);
			dp += 26 * FastMath.sin(2 * F);
			dp += -22 * FastMath.sin(-2 * D + 2 * F);
			dp += 21 * FastMath.sin(-Mm + 2 * F + omega);
			de += -10 * FastMath.cos(-Mm + 2 * F + omega);
			dp += (17 - 0.1 * TE) * FastMath.sin(2 * M);
			dp += 16 * FastMath.sin(2 * D - Mm + omega);
			de += -8 * FastMath.cos(2 * D - Mm + omega);
			dp += (-16 + 0.1 * TE) * FastMath.sin(-2 * D + 2 * M + 2 * F + 2 * omega);
			de += 7 * FastMath.cos(-2 * D + 2 * M + 2 * F + 2 * omega);
			dp += -15 * FastMath.sin(M + omega);
			de += 9 * FastMath.cos(M + omega);
			dp += -13 * FastMath.sin(-2 * D + Mm + omega);
			de += 7 * FastMath.cos(-2 * D + Mm + omega);
			dp += -12 * FastMath.sin(-M + omega);
			de += 6 * FastMath.cos(-M + omega);
			dp += 11 * FastMath.sin(2 * Mm - 2 * F);
			dp += -10 * FastMath.sin(2 * D - Mm + 2 * F + omega);
			de += 5 * FastMath.cos(2 * D - Mm + 2 * F + omega);
			dp += -8 * FastMath.sin(2 * D + Mm + 2 * F + 2 * omega);
			de += 3 * FastMath.cos(2 * D + Mm + 2 * F + 2 * omega);
			dp += 7 * FastMath.sin(M + 2 * F + 2 * omega);
			de += -3 * FastMath.cos(M + 2 * F + 2 * omega);
			dp += -7 * FastMath.sin(-2 * D + M + Mm);
			dp += -7 * FastMath.sin(-M + 2 * F + 2 * omega);
			de += 3 * FastMath.cos(-M + 2 * F + 2 * omega);
			dp += -7 * FastMath.sin(2 * D + 2 * F + omega);
			de += 3 * FastMath.cos(2 * D + 2 * F + omega);
			dp += 6 * FastMath.sin(2 * D + Mm);
			dp += 6 * FastMath.sin(-2 * D + 2 * Mm + 2 * F + 2 * omega);
			de += -3 * FastMath.cos(-2 * D + 2 * Mm + 2 * F + 2 * omega);
			dp += 6 * FastMath.sin(-2 * D + Mm + 2 * F + omega);
			de += -3 * FastMath.cos(-2 * D + Mm + 2 * F + omega);
			dp += -6 * FastMath.sin(2 * D - 2 * Mm + omega);
			de += 3 * FastMath.cos(2 * D - 2 * Mm + omega);
			dp += -6 * FastMath.sin(2 * D + omega);
			de += 3 * FastMath.cos(2 * D + omega);
			dp += 5 * FastMath.sin(-M + Mm);
			dp += -5 * FastMath.sin(-2 * D - M + 2 * F + omega);
			de += 3 * FastMath.cos(-2 * D - M + 2 * F + omega);
			dp += -5 * FastMath.sin(-2 * D + omega);
			de += 3 * FastMath.cos(-2 * D + omega);
			dp += -5 * FastMath.sin(2 * Mm + 2 * F + omega);
			/*
			de += 3 * FastMath.cos(2 * Mm + 2 * F + omega);
			dp += 4 * FastMath.sin(-2 * D + 2 * Mm + omega);
			dp += 4 * FastMath.sin(-2 * D + M + 2 * F + omega);
			dp += 4 * FastMath.sin(Mm - 2 * F);
			dp += -4 * FastMath.sin(-D + Mm);
			dp += -4 * FastMath.sin(-2 * D + M);
			dp += -4 * FastMath.sin(D);
			dp += 3 * FastMath.sin(Mm + 2 * F);
			dp += -3 * FastMath.sin(-2 * Mm + 2 * F + 2 * omega);
			dp += -3 * FastMath.sin(-D - M + Mm);
			dp += -3 * FastMath.sin(M + Mm);
			dp += -3 * FastMath.sin(-M + Mm + 2 * F + 2 * omega);
			dp += -3 * FastMath.sin(2 * D - M - Mm + 2 * F + 2 * omega);
			dp += -3 * FastMath.sin(3 * Mm + 2 * F + 2 * omega);
			dp += -3 * FastMath.sin(2 * D - M + 2 * F + 2 * omega);
			 */

			// Nutation in longitude
			double delta_psi = dp / 36000000.0;

			// Nutation in obliquity
			double delta_eps = de / 36000000.0;

			// Mean obliquity of the ecliptic
			double eps0 = (84381.448 - 46.815 * TE - 0.00059 * TE2 + 0.001813 * TE3) / 3600.0;

			// True obliquity of the ecliptic
			double eps = eps0 + delta_eps;

			// Equation of the equinoxes
			double EoE = 240 * delta_psi * FastMath.cos(eps * Constant.DEG_TO_RAD);

			return EoE * 15.0 * Constant.ARCSEC_TO_RAD;
		}


		// Obtain mean obliquity
		double t = Functions.toCenturies(TimeScale.getExactJD(time, obs, eph, SCALE.TERRESTRIAL_TIME)).doubleValue();
		if (t == lastT) {
			boolean c = eph.ephemMethod.isIAU20xx();
			if (c == ct) return lastEQEQ;
		}

		double epsilon = Obliquity.meanObliquity(t, eph);

		// Calculate Nutation
		Nutation.calcNutation(t, eph);

		// Obtain nutation in RA
		double eq_eq = Nutation.getNutationInLongitude() * Math.cos(epsilon);

		// Add complementary terms if necessary
		ct = false;
		if (eph.ephemMethod.isIAU20xx()) {
			eq_eq += SiderealTime.eect(t);
			ct = true;
		}

		lastEQEQ = eq_eq;
		lastT = t;

		return eq_eq;
	}
	private static double lastT, lastEQEQ;
	private static boolean ct = false;

	/**
	 * Complementary terms of equation of equinoxes from SOFA library.
	 *
	 * @param T Julian centuries from J2000 in dynamical time.
	 * @return Value in radians.
	 */
	private static double eect(double T)
	{
		double EECT_last_value = 0.0;
		double EECT_last_calc_T = -1E100;
		Object o = DataBase.getData("EECT", true);
		if (o != null) {
			double d[] = (double[]) o;
			EECT_last_value = d[0];
			EECT_last_calc_T = d[1];
		}

		if (T == EECT_last_calc_T)
			return EECT_last_value;
		EECT_last_calc_T = T;

		double ct = 0.0;

		/* Fundamental (Delaunay) arguments (from IERS Conventions 2003) */

		// * Mean anomaly of the Moon.
		double EL = Functions
				.mod3600(485868.249036 + T * (715923.2178 + T * (31.8792 + T * (0.051635 + T * (-0.00024470))))) * Constant.ARCSEC_TO_RAD + Functions
				.module(1325.0 * T, 1.0) * 2.0 * Math.PI;

		// * Mean anomaly of the Sun.
		double ELP = Functions
				.mod3600(1287104.793048 + T * (129596581.0481 + T * (-0.5532 + T * (0.000136 + T * (-0.00001149))))) * Constant.ARCSEC_TO_RAD + Functions
				.module(99.0 * T, 1.0) * 2.0 * Math.PI;

		// * Mean argument of the latitude of the Moon.
		double F = Functions
				.mod3600(335779.526232 + T * (295262.8478 + T * (-12.7512 + T * (-0.001037 + T * (0.00000417))))) * Constant.ARCSEC_TO_RAD + Functions
				.module(1342.0 * T, 1.0) * 2.0 * Math.PI;

		// * Mean elongation of the Moon from the Sun.
		double D = Functions
				.mod3600(1072260.70369 + T * (1105601.2090 + T * (-6.3706 + T * (0.006593 + T * (-0.00003169))))) * Constant.ARCSEC_TO_RAD + Functions
				.module(1236.0 * T, 1.0) * 2.0 * Math.PI;

		// * Mean longitude of the ascending node of the Moon.
		double OM = Functions
				.mod3600(450160.398036 + T * (-482890.5431 + T * (7.4722 + T * (0.007702 + T * (-0.00005939))))) * Constant.ARCSEC_TO_RAD - Functions
				.module(5.0 * T, 1.0) * 2.0 * Math.PI;

		// * Planetary longitudes, Mercury through Pluto.
		double ALME = Functions.normalizeRadians(4.402608842 + 2608.7903141574 * T);
		double ALVE = Functions.normalizeRadians(3.176146697 + 1021.3285546211 * T);
		double ALEA = Functions.normalizeRadians(1.753470314 + 628.3075849991 * T);
		double ALMA = Functions.normalizeRadians(6.203480913 + 334.0612426700 * T);
		double ALJU = Functions.normalizeRadians(0.599546497 + 52.9690962641 * T);
		double ALSA = Functions.normalizeRadians(0.874016757 + 21.3299104960 * T);
		double ALUR = Functions.normalizeRadians(5.481293872 + 7.4781598567 * T);
		double ALNE = Functions.normalizeRadians(5.311886287 + 3.8133035638 * T);
		double ALPL = Functions.normalizeRadians(0.024381750 + 0.00000538691 * T);

		double FA[] =
		{ EL, ELP, F, D, OM, ALME, ALVE, ALEA, ALMA, ALJU, ALSA, ALUR, ALNE, ALPL };

		// Evaluate the EE complementary terms.
		double S0 = 0.0;
		double S1 = 0.0;

		for (int i = 32; i >= 0; i--)
		{
			double A = 0.0;
			for (int j = 0; j <= 13; j++)
			{
				A = A + eect00.KE0[i][j] * FA[j];
			}
			S0 = S0 + (eect00.SE0[i][0] * Math.sin(A) + eect00.SE0[i][1] * Math.cos(A));
		}

		for (int i = 0; i >= 0; i--)
		{
			double A = 0.0;
			for (int j = 0; j <= 13; j++)
			{
				A = A + eect00.KE1[j] * FA[j];
			}
			S1 = S1 + (eect00.SE1[0] * Math.sin(A) + eect00.SE1[1] * Math.cos(A));
		}

		ct = (S0 + S1 * T) * Constant.ARCSEC_TO_RAD;

		EECT_last_value = ct;

		DataBase.addData("EECT", new double[] {EECT_last_value, EECT_last_calc_T}, true);
		return ct;
	}
}

final class eect00
{
	// Argument coefficients for t^0
	static double KE0[][] =
	{
		// DATA ( ( KE0(I,J), I=1,14), J = 1, 10 ),
		{ 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
		{ 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
		{ 0, 0, 2, -2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
		{ 0, 0, 2, -2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
		{ 0, 0, 2, -2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
		{ 0, 0, 2, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
		{ 0, 0, 2, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
		{ 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
		{ 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
		{ 0, 1, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
		// DATA ( ( KE0(I,J), I=1,14), J = 11, 20 ),
		{ 1, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
		{ 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
		{ 0, 1, 2, -2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
		{ 0, 1, 2, -2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
		{ 0, 0, 4, -4, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
		{ 0, 0, 1, -1, 1, 0, -8, 12, 0, 0, 0, 0, 0, 0 },
		{ 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
		{ 0, 0, 2, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
		{ 1, 0, 2, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
		{ 1, 0, 2, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
		// DATA ( ( KE0(I,J), I=1,14), J = 21, 30 ),
		{ 0, 0, 2, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
		{ 0, 1, -2, 2, -3, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
		{ 0, 1, -2, 2, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
		{ 0, 0, 0, 0, 0, 0, 8, -13, 0, 0, 0, 0, 0, -1 },
		{ 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
		{ 2, 0, -2, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
		{ 1, 0, 0, -2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
		{ 0, 1, 2, -2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
		{ 1, 0, 0, -2, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
		{ 0, 0, 4, -2, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
		// DATA ( ( KE0(I,J), I=1,14), J = 31, NE0 ),
		{ 0, 0, 2, -2, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
		{ 1, 0, -2, 0, -3, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
		{ 1, 0, -2, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0 }
	};

	// Argument coefficients for t^1
	static double KE1[] = { 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

	// Sine and cosine coefficients for t^0
	static double SE0[][] =
	{
		// DATA ( ( SE0(I,J), I=1,2), J = 1, 10 ),
		{ +2640.96e-6, -0.39e-6 },
		{ +63.52e-6, -0.02e-6 },
		{ +11.75e-6, +0.01e-6 },
		{ +11.21e-6, +0.01e-6 },
		{ -4.55e-6, +0.00e-6 },
		{ +2.02e-6, +0.00e-6 },
		{ +1.98e-6, +0.00e-6 },
		{ -1.72e-6, +0.00e-6 },
		{ -1.41e-6, -0.01e-6 },
		{ -1.26e-6, -0.01e-6 },
		// DATA ( ( SE0(I,J), I=1,2), J = 11, 20 ),
		{ -0.63e-6, +0.00e-6 },
		{ -0.63e-6, +0.00e-6 },
		{ +0.46e-6, +0.00e-6 },
		{ +0.45e-6, +0.00e-6 },
		{ +0.36e-6, +0.00e-6 },
		{ -0.24e-6, -0.12e-6 },
		{ +0.32e-6, +0.00e-6 },
		{ +0.28e-6, +0.00e-6 },
		{ +0.27e-6, +0.00e-6 },
		{ +0.26e-6, +0.00e-6 },
		// DATA ( ( SE0(I,J), I=1,2), J = 21, 30 ),
		{ -0.21e-6, +0.00e-6 },
		{ +0.19e-6, +0.00e-6 },
		{ +0.18e-6, +0.00e-6 },
		{ -0.10e-6, +0.05e-6 },
		{ +0.15e-6, +0.00e-6 },
		{ -0.14e-6, +0.00e-6 },
		{ +0.14e-6, +0.00e-6 },
		{ -0.14e-6, +0.00e-6 },
		{ +0.14e-6, +0.00e-6 },
		{ +0.13e-6, +0.00e-6 },
		// DATA ( ( SE0(I,J), I=1,2), J = 31, NE0 ),
		{ -0.11e-6, +0.00e-6 },
		{ +0.11e-6, +0.00e-6 },
		{ +0.11e-6, +0.00e-6 },
	};

	// Sine and cosine coefficients for t^1
	static double SE1[] = { -0.87e-6, +0.00e-6, };
}
