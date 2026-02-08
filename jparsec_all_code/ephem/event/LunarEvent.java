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
package jparsec.ephem.event;

import jparsec.astronomy.CoordinateSystem;
import jparsec.astronomy.Star;
import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.EphemerisElement.ALGORITHM;
import jparsec.ephem.Functions;
import jparsec.ephem.IAU2006;
import jparsec.ephem.Nutation;
import jparsec.ephem.Obliquity;
import jparsec.ephem.RiseSetTransit;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.event.MainEvents.EVENT_TIME;
import jparsec.ephem.event.SimpleEventElement.EVENT;
import jparsec.ephem.planets.EphemElement;
import jparsec.ephem.planets.JPLEphemeris;
import jparsec.ephem.planets.PlanetEphem;
import jparsec.ephem.planets.imcce.Elp2000;
import jparsec.math.Constant;
import jparsec.math.DoubleVector;
import jparsec.math.matrix.Matrix;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.AstroDate;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.time.TimeScale;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;
import jparsec.util.Translate.LANGUAGE;

/**
 * A class to calculate lunar events.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class LunarEvent
{
	// private constructor so that this class cannot be instantiated.
	private LunarEvent() {}

	/**
	 * Obtains the orientation of the Moon according to Eckhardt's analytical theory.
	 * The results in current dates are about 0.1 deg from those obtained using IAU
	 * recommendations, and it seems the values from Eckhardt's theory are closer
	 * to those obtained using JPL ephemerides, for a few centuries around year 2000.
	 * IAU lunar rotation model is no longer used in JPARSEC.<P>
	 *
	 * An adequate ephemeris object should be provided, with the algorithm to apply to
	 * obtain the lunar position. The returning values will be referred to the geocenter
	 * or the topocentric place depending on it.<P>
	 *
	 * Reference:<P>
	 *
	 * Eckhardt, D.H., "Theory of the Libration of the Moon", Moon and planets 25, 3 (1981).
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @return An array with the total librations (longitude and planetocentric
	 * latitude), and the position angle of axis as the 3rd element. In radians, for
	 * mean equinox of date.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double[] getEckhardtMoonLibrations(TimeElement time, ObserverElement obs,
			EphemerisElement eph)
	throws JPARSECException {
		// Obtain Moon true position
		EphemerisElement ephCopy = eph.clone();
		ephCopy.targetBody = TARGET.Moon;
		ephCopy.ephemType = EphemerisElement.COORDINATES_TYPE.APPARENT;
		EphemElement ephem = Ephem.getEphemeris(time, obs, ephCopy, false, false);
		double jd = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);

		return LunarEvent.getEckhardtMoonLibrations(jd, eph.ephemMethod, ephem.getEquatorialLocation());
	}

	/**
	 * Obtains the orientation of the Moon according to Eckhardt's analytical theory.<P>
	 * The results in current dates are about 0.1 deg from those obtained using IAU
	 * recommendations, and it seems the values from Eckhardt's theory are closer
	 * to those obtained using JPL ephemerides.<P>
	 *
	 * An adequate ephemeris object should be provided, with the algorithm to apply to
	 * obtain the lunar position. The returning values will be refered to the geocenter
	 * or the topocentric place depending on it.<P>
	 *
	 * Reference:<P>
	 *
	 * Eckhardt, D.H., "Theory of the Libration of the Moon", Moon and planets 25, 3 (1981).
	 *
	 * @param jd Julian day in TT.
	 * @param ephemMethod Ephem method to apply, constants defined in {@linkplain EphemerisElement}.
	 * @param locEquatorial Equatorial apparent position of the Moon for date jd.
	 * @return An array with the total librations (longitude and planetocentric
	 * latitude), and the position angle of axis as the 3rd element. In radians, for
	 * mean equinox of date.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double[] getEckhardtMoonLibrations(double jd, EphemerisElement.REDUCTION_METHOD ephemMethod,
			LocationElement locEquatorial)
	throws JPARSECException {
		double t = Functions.toCenturies(jd);

		EphemerisElement eph = new EphemerisElement();
		eph.ephemMethod = ephemMethod;
		eph.correctForEOP = false;
		double eps = Obliquity.trueObliquity(t, eph);
		LocationElement locEcliptic = CoordinateSystem.equatorialToEcliptic(locEquatorial, eps, false);
		double moonLon = locEcliptic.getLongitude();
		double moonLat = locEcliptic.getLatitude();

		// Obtain mean parameters for the Moon.
		double k1 = (119.75 + 131.849 * t) * Constant.DEG_TO_RAD;
		double k2 = (72.56 + 20.186 * t) * Constant.DEG_TO_RAD;
		// Mean elongation of Moon
		double D = (297.8502042 + 445267.1115168 * t - 0.0016300 * t * t + t * t * t / 545868.0 - t * t * t * t / 113065000.0) * Constant.DEG_TO_RAD;
		// Sun's mean anomaly
		double M = (357.5291092 + 35999.0502909 * t - 0.0001536 * t * t + t * t * t / 24490000.0) * Constant.DEG_TO_RAD;
		// Moon's mean anomaly
		double Mp = (134.9634114 + 477198.8676313 * t + 0.0089979 * t * t + t * t * t / 69699.0 - t * t * t * t / 14712000.0) * Constant.DEG_TO_RAD;
		// Earth's eccentricity
		double E = 1.0 - 0.002516 * t - 0.0000074 * t * t;
		// Moon's argument of latitude
		double F = (93.2720993 + 483202.0175273 * t - 0.0034029 * t * t - t * t * t / 3526000.0 + t * t * t * t / 863310000.0) * Constant.DEG_TO_RAD;
		// Moon's inclination
		double I = 1.54242 * Constant.DEG_TO_RAD;
		// Moon's mean ascending node longitude
		double omega = (125.0445550 - 1934.1361849 * t + 0.0020762 * t * t + t * t * t / 467410.0 - t * t * t * t / 18999000.0) * Constant.DEG_TO_RAD;

		// Obtain optical librations
		Nutation.calcNutation(t, eph);
		double nutLon = Nutation.getNutationInLongitude();
		double W = moonLon - nutLon - omega;
		double sinA = Math.sin(W) * Math.cos(moonLat) * Math.cos(I) - Math.sin(moonLat) * Math.sin(I);
		double cosA = Math.cos(W) * Math.cos(moonLat);
		double A = Math.atan2(sinA, cosA);
		double lp = Functions.normalizeRadians(A - F);
		double sinbp = - Math.sin(W) * Math.cos(moonLat) * Math.sin(I) - Math.sin(moonLat) * Math.cos(I);
		double bp = Math.asin(sinbp);

		// Obtain rho, sigma, and tau parameters
		double rho = -0.02752 * Math.cos(Mp) - 0.02245 * Math.sin(F) + 0.00684 * Math.cos(Mp - 2.0 * F);
		rho -= 0.00293 * Math.cos(2.0 * F) + 0.00085 * Math.cos(2.0 * F - 2.0 * D) + 0.00054 * Math.cos(Mp - 2.0 * D);
		rho -= 0.00020 * Math.sin(Mp + F) + 0.00020 * Math.cos(Mp + 2.0 * F) + 0.00020 * Math.cos(Mp - F);
		rho += 0.00014 * Math.cos(Mp + 2.0 * F - 2.0 * D);

		double sigma = -0.02816 * Math.sin(Mp) + 0.02244 * Math.cos(F) - 0.00682 * Math.sin(Mp -2.0 * F);
		sigma += -0.00279 * Math.sin(2.0 * F) - 0.00083 * Math.sin(2.0 * F - 2.0 * D) + 0.00069 * Math.sin(Mp - 2.0 * D);
		sigma += 0.00040 * Math.cos(Mp + F) - 0.00025 * Math.sin(2.0 * Mp) - 0.00023 * Math.sin(Mp + 2.0 * F);
		sigma += 0.00020 * Math.cos(Mp - F) - 0.00019 * Math.sin(Mp - F) + 0.00013 * Math.sin(Mp + 2.0 * F - 2.0 * D);
		sigma += -0.00010 * Math.cos(Mp - 3.0 * F);

		double tau = 0.02520 * E * Math.sin(M) + 0.00473 * Math.sin(2.0 * Mp - 2.0 * F) - 0.00467 * Math.sin(Mp);
		tau += 0.00396 * Math.sin(k1) + 0.00276 * Math.sin(2.0 * Mp -2.0 * D) + 0.00196 * Math.sin(omega);
		tau += -0.00183 * Math.cos(Mp - F) + 0.00115 * Math.sin(Mp - 2.0 * D) - 0.00096 * Math.sin(Mp - D);
		tau += 0.00046 * Math.sin(2.0 * F - 2.0 * D) - 0.00039 * Math.sin(Mp - F) - 0.00032 * Math.sin(Mp - M - D);
		tau += 0.00027 * Math.sin(2.0 * Mp - M - 2.0 * D) + 0.00023 * Math.sin(k2) - 0.00014 * Math.sin(2.0 * D);
		tau += 0.00014 * Math.cos(2.0 * Mp - 2.0 * F) - 0.00012 * Math.sin(Mp - 2.0 * F) - 0.00012 * Math.sin(2.0 * Mp);
		tau += 0.00011 * Math.sin(2.0 * Mp - 2.0 * M - 2.0 * D);

		rho = rho * Constant.DEG_TO_RAD;
		sigma = sigma * Constant.DEG_TO_RAD;
		tau = tau * Constant.DEG_TO_RAD;

		// Obtain physical librations
		double lpp = -tau + (rho * Math.cos(A) + sigma * Math.sin(A)) * Math.tan(bp);
		double bpp = sigma * Math.cos(A) - rho * Math.sin(A);

		// Obtain total librations
		double l = lp + lpp;
		double b = bp + bpp;

		// Obtain position angle of axis
		double v = omega + nutLon + sigma / Math.sin(I);
		double x = Math.sin(I + rho) * Math.sin(v);
		double y = Math.sin(I + rho) * Math.cos(v) * Math.cos(eps) - Math.cos(I + rho) * Math.sin(eps);
		double w = Math.atan2(x, y);
		double sinp = Math.sqrt(x*x + y*y) * Math.cos(locEquatorial.getLongitude() - w) / Math.cos(b);
		double p = Math.asin(sinp);

		return new double[] {l, b, p};
	}

	// See Astronomical Algorithms by J. Meeus.
    private static double[][] moonPerigeeCoefficients = {
        {2, 0, 0, -1.6769, 0},
        {4, 0, 0, 0.4589, 0},
        {6, 0, 0, -0.1856, 0},
        {8, 0, 0, 0.0883, 0},
        {2, -1, 0, -0.0773, 0.00019},
        {0, 1, 0, 0.0502, -0.00013},
        {10, 0, 0, -0.0460, 0},
        {4, -1, 0, 0.0422, -0.00011},
        {6, -1, 0, -0.0256, 0},
        {12, 0, 0, 0.0253, 0},
        {1, 0, 0, 0.0237, 0},
        {8, -1, 0, 0.0162, 0},
        {14, 0, 0, -0.0145, 0},
        {0, 0, 2, 0.0129, 0},
        {3, 0, 0, -0.0112, 0},
        {10, -1, 0, -0.0104, 0},
        {16, 0, 0, 0.0086, 0},
        {12, -1, 0, 0.0069, 0},
        {5, 0, 0, 0.0066, 0},
        {2, 0, 2, -0.0053, 0},
        {18, 0, 0, -0.0052, 0},
        {14, -1, 0, -0.0046, 0},
        {7, 0, 0, -0.0041, 0},
        {2, 1, 0, 0.0040, 0},
        {20, 0, 0, 0.0032, 0},
        {1, 1, 0, -0.0032, 0},
        {16, -1, 0, 0.0031, 0},
        {4, 1, 0, -0.0029, 0},
        {9, 0, 0, 0.0027, 0},
        {4, 0, 2, 0.0027, 0},
        {2, -2, 0, -0.0027, 0},
        {4, -2, 0, 0.0024, 0},
        {6, -2, 0, -0.0021, 0},
        {22, 0, 0, -0.0021, 0},
        {18, -1, 0, -0.0021, 0},
        {6, 1, 0, 0.0019, 0},
        {11, 0, 0, -0.0018, 0},
        {8, 1, 0, -0.0014, 0},
        {4, 0, -2, -0.0014, 0},
        {6, 0, 2, -0.0014, 0},
        {3, 1, 0, 0.0014, 0},
        {5, 1, 0, -0.0014, 0},
        {13, 0, 0, 0.0013, 0},
        {20, -1, 0, 0.0013, 0},
        {3, 2, 0, 0.0011, 0},
        {4, -2, 2, -0.0011, 0},
        {1, 2, 0, -0.0010, 0},
        {22, -1, 0, -0.0009, 0},
        {0, 0, 4, -0.0008, 0},
        {6, 0, -2, 0.0008, 0},
        {2, 1, -2, 0.0008, 0},
        {0, 2, 0, 0.0007, 0},
        {0, -1, 2, 0.0007, 0},
        {2, 0, 4, 0.0007, 0},
        {0, -2, 2, -0.0006, 0},
        {2, 2, -2, -0.0006, 0},
        {24, 0, 0, 0.0006, 0},
        {4, 0, -4, 0.0005, 0},
        {2, 2, 0, 0.0005, 0},
        {1, -1, 0, -0.0004, 0}
    };
    private static double[][] moonApogeeCoefficients = {
        {2, 0, 0, 0.4392, 0},
        {4, 0, 0, 0.0684, 0},
        {0, 1, 0, 0.0456, -0.00011},
        {2, -1, 0, 0.0426, -0.00011},
        {0, 0, 2, 0.0212, 0},
        {1, 0, 0, -0.0189, 0},
        {6, 0, 0, 0.0144, 0},
        {4, -1, 0, 0.0113, 0},
        {2, 0, 2, 0.0047, 0},
        {1, 1, 0, 0.0036, 0},
        {8, 0, 0, 0.0035, 0},
        {6, -1, 0, 0.0034, 0},
        {2, 0, -2, -0.0034, 0},
        {2, -2, 0, 0.0022, 0},
        {3, 0, 0, -0.0017, 0},
        {4, 0, 2, 0.0013, 0},
        {8, -1, 0, 0.0011, 0},
        {4, -2, 0, 0.0010, 0},
        {10, 0, 0, 0.0009, 0},
        {3, 1, 0, 0.0007, 0},
        {0, 2, 0, 0.0006, 0},
        {2, 1, 0, 0.0005, 0},
        {2, 2, 0, 0.0005, 0},
        {6, 0, 2, 0.0004, 0},
        {6, -2, 0, 0.0004, 0},
        {10, -1, 0, 0.0004, 0},
        {5, 0, 0, -0.0004, 0},
        {4, 0, -2, -0.0004, 0},
        {0, 1, 2, 0.0003, 0},
        {12, 0, 0, 0.0003, 0},
        {2, -1, 2, 0.0003, 0},
        {1, -1, 0, -0.0003, 0}
    };
    private static double[][] moonPerigeeParallaxCoefficients = {
        {2, 0, 0, 63.224, 0},
        {4, 0, 0, -6.990, 0},
        {2, -1, 0, 2.834, -0.0071},
        {6, 0, 0, 1.927, 0},
        {1, 0, 0, -1.263, 0},
        {8, 0, 0, -0.702, 0},
        {0, 1, 0, 0.696, -0.0017},
        {0, 0, 2, -0.690, 0},
        {4, -1, 0, 0.629, 0.0016},
        {2, 0, -2, -0.392, 0},
        {10, 0, 0, 0.297, 0},
        {6, -1, 0, 0.260, 0},
        {3, 0, 0, 0.201, 0},
        {2, 1, 0, -0.161, 0},
        {1, 1, 0, 0.157, 0},
        {12, 0, 0, -0.138, 0},
        {8, -1, 0, -0.127, 0},
        {2, 0, 2, 0.104, 0},
        {2, -2, 0, 0.104, 0},
        {5, 0, 0, -0.079, 0},
        {14, 0, 0, 0.068, 0},
        {10, -1, 0, 0.067, 0},
        {4, 1, 0, 0.054, 0},
        {12, -1, 0, -0.038, 0},
        {4, -2, 0, -0.038, 0},
        {7, 0, 0, 0.037, 0},
        {4, 0, 2, -0.037, 0},
        {16, 0, 0, -0.035, 0},
        {3, 1, 0, -0.030, 0},
        {1, -1, 0, 0.029, 0},
        {6, 1, 0, -0.025, 0},
        {0, 2, 0, 0.023, 0},
        {14, -1, 0, 0.023, 0},
        {2, 2, 0, -0.023, 0},
        {6, -2, 0, 0.022, 0},
        {2, -1, -2, -0.021, 0},
        {9, 0, 0, -0.020, 0},
        {18, 0, 0, 0.019, 0},
        {6, 0, 2, 0.017, 0},
        {0, -1, 2, 0.014, 0},
        {16, -1, 0, -0.014, 0},
        {4, 0, -2, 0.013, 0},
        {8, 1, 0, 0.012, 0},
        {11, 0, 0, 0.011, 0},
        {5, 1, 0, 0.010, 0},
        {20, 0, 0, -0.010, 0}
    };
    private static double[][] moonApogeeParalllaxCoefficients = {
        {2, 0, 0, -9.147, 0},
        {1, 0, 0, -0.841, 0},
        {0, 0, 2, 0.697, 0},
        {0, 1, 0, -0.656, 0.0016},
        {4, 0, 0, 0.355, 0},
        {2, -1, 0, 0.159, 0},
        {1, 1, 0, 0.127, 0},
        {4, -1, 0, 0.065, 0},
        {6, 0, 0, 0.052, 0},
        {2, 1, 0, 0.043, 0},
        {2, 0, 2, 0.031, 0},
        {2, 0, -2, -0.023, 0},
        {2, -2, 0, 0.022, 0},
        {2, 2, 0, 0.019, 0},
        {0, 2, 0, -0.016, 0},
        {6, -1, 0, 0.014, 0},
        {8, 0, 0, 0.010, 0}
    };

    private static double calcK(double jd) throws JPARSECException {
		AstroDate astro = new AstroDate(jd);
		double year = astro.getAstronomicalYear() + (astro.getMonth() - 1.0 + astro.getDayPlusFraction() / (1.0 + astro.getDaysInMonth())) / 12.0;

        double kapprox = (year - 1999.973) * 13.2555;
        return kapprox;
    }

    private static double meanPerigeeApogee(double k) {
        double T = k / 1325.55;
        double Tsquared = T * T;
        double Tcubed = Tsquared * T;
        double T4 = Tcubed * T;

        return 2451534.6698 + 27.55454988 * k - 0.0006886 * Tsquared - 0.000001098 * Tcubed + 0.0000000052 * T4;
    }

	private static double round(double kapprox, double delta, EVENT_TIME eventType) {
		double k = delta + Math.floor(kapprox);
		if (eventType == EVENT_TIME.NEXT && k < kapprox) k ++;
		if (eventType == EVENT_TIME.PREVIOUS && k > kapprox) k --;
		if (eventType == EVENT_TIME.CLOSEST && k < kapprox - 0.5 && delta == 0.0) k ++;
		return k;
	}

    /**
     * Calculates the instant of the Moon's perigee, following Meeus' Astronomical
     * Algorithms, chapter 48. Largest error compared to ELP2000 is 30 minutes.
     * Time is corrected for Moon secular acceleration.
     * @param jd The starting Julian day of calculations (TDB).
     * @param eventType Next, closest, or previous events, as defined in this constants
     * of {@linkplain MainEvents} class. The use of the closest option is recommended when
     * possible, since next/previous events could give incorrect events for a given date
     * far from J2000.
     * @return The event. In the details field the value of the equatorial horizontal
     * parallax in radians is provided. The distance in km can be calculated as
     * d = 6378.14 / Math.sin(parallax).
     * @throws JPARSECException If an error occurs.
     */
    public static SimpleEventElement getPerigee(double jd, EVENT_TIME eventType) throws JPARSECException {
        double k = calcK(jd);
        k = LunarEvent.round(k, 0.0, eventType);

        double MeanJD = meanPerigeeApogee(k);
		if (MeanJD > jd && eventType == EVENT_TIME.PREVIOUS) {
			k --;
			MeanJD = meanPerigeeApogee(k);
		}
		if (MeanJD < jd && eventType == EVENT_TIME.NEXT) {
			k ++;
			MeanJD = meanPerigeeApogee(k);
		}
		if (MeanJD > jd && eventType != EVENT_TIME.PREVIOUS) {
			double km = k - 1;
			double MeanJDm = meanPerigeeApogee(km);
			if ((MeanJDm > jd && eventType == EVENT_TIME.NEXT) ||
					(Math.abs(jd-MeanJDm) < Math.abs(jd-MeanJD) && eventType == EVENT_TIME.CLOSEST)) {
				k = km;
				MeanJD = MeanJDm;
			}
		}
		if (MeanJD < jd && eventType != EVENT_TIME.NEXT) {
			double kp = k + 1;
			double MeanJDp = meanPerigeeApogee(kp);
			if ((MeanJDp < jd && eventType == EVENT_TIME.PREVIOUS) ||
					(Math.abs(jd-MeanJDp) < Math.abs(jd-MeanJD) && eventType == EVENT_TIME.CLOSEST)) {
				k = kp;
				MeanJD = MeanJDp;
			}
		}


        double T = k / 1325.55;
        double Tsquared = T * T;
        double Tcubed = Tsquared * T;
        double T4 = Tcubed * T;

        double D = Functions.normalizeDegrees(171.9179 + 335.9106046 * k - 0.0100250 * Tsquared - 0.00001156 * Tcubed + 0.000000055 * T4);
        D *= Constant.DEG_TO_RAD;
        double M = Functions.normalizeDegrees(347.3477 + 27.1577721 * k - 0.0008323 * Tsquared - 0.0000010 * Tcubed);
        M *= Constant.DEG_TO_RAD;
        double F = Functions.normalizeDegrees(316.6109 + 364.5287911 * k - 0.0125131 * Tsquared - 0.0000148 * Tcubed);
        F *= Constant.DEG_TO_RAD;

        int nPerigeeCoefficients = moonPerigeeCoefficients.length;
        double Sigma = 0.0;
        for (int i = 0; i < nPerigeeCoefficients; i++) {
            Sigma += (moonPerigeeCoefficients[i][3] + T
                    * moonPerigeeCoefficients[i][4])
                    * Math.sin(D * moonPerigeeCoefficients[i][0] + M
                    * moonPerigeeCoefficients[i][1] + F
                    * moonPerigeeCoefficients[i][2]);
        }
        jd = MeanJD + Sigma;

        nPerigeeCoefficients = moonPerigeeParallaxCoefficients.length;
        double Parallax = 3629.215;
        for (int i = 0; i < nPerigeeCoefficients; i++) {
            Parallax += (moonPerigeeParallaxCoefficients[i][3] + T
                    * moonPerigeeParallaxCoefficients[i][4])
                    * Math.cos(D * moonPerigeeParallaxCoefficients[i][0] + M
                    * moonPerigeeParallaxCoefficients[i][1] + F
                    * moonPerigeeParallaxCoefficients[i][2]);
        }
        double p = (Parallax / 3600.0) * Constant.DEG_TO_RAD;

		// Correct Meeus results for secular acceleration
		double deltaT = Elp2000.timeCorrectionForSecularAcceleration(jd) - jd;
		jd -= deltaT;

        SimpleEventElement se = new SimpleEventElement(jd, SimpleEventElement.EVENT.MOON_PERIGEE, ""+p);
        se.body = TARGET.Moon.getName();
        return se;
    }

    /**
     * Calculates the instant of the next Moon's apogee, following Meeus' Astronomical
     * Algorithms, chapter 48. Largest error compared to ELP2000 is only 3 minutes.
     * Time is corrected for Moon secular acceleration.
     * @param jd The starting Julian day of calculations (TDB).
     * @param eventType Next, closest, or previous events, as defined in this constants
     * of {@linkplain MainEvents} class. The use of the closest option is recommended when
     * possible, since next/previous events could give incorrect events for a given date
     * far from J2000.
     * @return The event. In the details field the value of the equatorial horizontal
     * parallax in radians is provided. The distance in km can be calculated as
     * d = 6378.14 / Math.sin(parallax).
     * @throws JPARSECException If an error occurs.
     */
    public static SimpleEventElement getApogee(double jd, EVENT_TIME eventType) throws JPARSECException {
        double k = calcK(jd);
        k = LunarEvent.round(k, 0.5, eventType);

        double MeanJD = meanPerigeeApogee(k);
		if (MeanJD > jd && eventType == EVENT_TIME.PREVIOUS) {
			k --;
			MeanJD = meanPerigeeApogee(k);
		}
		if (MeanJD < jd && eventType == EVENT_TIME.NEXT) {
			k ++;
			MeanJD = meanPerigeeApogee(k);
		}
		if (MeanJD > jd && eventType != EVENT_TIME.PREVIOUS) {
			double km = k - 1;
			double MeanJDm = meanPerigeeApogee(km);
			if ((MeanJDm > jd && eventType == EVENT_TIME.NEXT) ||
					(Math.abs(jd-MeanJDm) < Math.abs(jd-MeanJD) && eventType == EVENT_TIME.CLOSEST)) {
				k = km;
				MeanJD = MeanJDm;
			}
		}
		if (MeanJD < jd && eventType != EVENT_TIME.NEXT) {
			double kp = k + 1;
			double MeanJDp = meanPerigeeApogee(kp);
			if ((MeanJDp < jd && eventType == EVENT_TIME.PREVIOUS) ||
					(Math.abs(jd-MeanJDp) < Math.abs(jd-MeanJD) && eventType == EVENT_TIME.CLOSEST)) {
				k = kp;
				MeanJD = MeanJDp;
			}
		}


        double T = k / 1325.55;
        double Tsquared = T * T;
        double Tcubed = Tsquared * T;
        double T4 = Tcubed * T;

        double D = Functions.normalizeDegrees(171.9179 + 335.9106046 * k - 0.0100250 * Tsquared - 0.00001156 * Tcubed + 0.000000055 * T4);
        D *= Constant.DEG_TO_RAD;
        double M = Functions.normalizeDegrees(347.3477 + 27.1577721 * k - 0.0008323 * Tsquared - 0.0000010 * Tcubed);
        M *= Constant.DEG_TO_RAD;
        double F = Functions.normalizeDegrees(316.6109 + 364.5287911 * k - 0.0125131 * Tsquared - 0.0000148 * Tcubed);
        F *= Constant.DEG_TO_RAD;

        int nApogeeCoefficients = moonApogeeCoefficients.length;
        double Sigma = 0.0;
        for (int i = 0; i < nApogeeCoefficients; i++) {
            Sigma += (moonApogeeCoefficients[i][ 3] + T
                    * moonApogeeCoefficients[i][4])
                    * Math.sin(D * moonApogeeCoefficients[i][ 0] + M
                    * moonApogeeCoefficients[i][1] + F
                    * moonApogeeCoefficients[i][ 2]);
        }
        jd = MeanJD + Sigma;

        nApogeeCoefficients = moonApogeeParalllaxCoefficients.length;
        double Parallax = 3245.251;
        for (int i = 0; i < nApogeeCoefficients; i++) {
            Parallax += (moonApogeeParalllaxCoefficients[i][3] + T
                    * moonApogeeParalllaxCoefficients[i][4])
                    * Math.cos(D * moonApogeeParalllaxCoefficients[i][0] + M
                    * moonApogeeParalllaxCoefficients[i][1] + F
                    * moonApogeeParalllaxCoefficients[i][2]);
        }
        double p = (Parallax / 3600.0) * Constant.DEG_TO_RAD;

		// Correct Meeus results for secular acceleration
		double deltaT = Elp2000.timeCorrectionForSecularAcceleration(jd) - jd;
		jd -= deltaT;

        SimpleEventElement se = new SimpleEventElement(jd, SimpleEventElement.EVENT.MOON_APOGEE, ""+p);
        se.body = TARGET.Moon.getName();
        return se;
    }

    /**
     * Returns the mean longitude of the ascending node of the Moon following
     * Chapront et al. 1988 (see Meeus, chapter 45).
     * @param time Time object.
     * @param obs Observer object.
     * @param eph Ephemeris object.
     * @return Time as a Julian day.
     * @throws JPARSECException If an error occurs.
     */
    public static double getMeanLongitudeOfAscendingNode(TimeElement time, ObserverElement obs,
			EphemerisElement eph) throws JPARSECException {
		double jd = TimeScale.getJD(time, obs, eph, SCALE.TERRESTRIAL_TIME);
    	double T = Functions.toCenturies(jd);
    	double omega = 125.0445550 - 1934.1361849 * T + 0.0020762 * T * T + T * T * T / 467410.0 - T * T * T * T / 60616000.0;
    	return Functions.normalizeRadians(omega * Constant.DEG_TO_RAD);
    }
    /**
     * Returns the mean longitude of the perigee of the lunar orbit following
     * Chapront et al. 1988 (see Meeus, chapter 45).
     * @param time Time object.
     * @param obs Observer object.
     * @param eph Ephemeris object.
     * @return Time as a Julian day.
     * @throws JPARSECException If an error occurs.
     */
    public static double getMeanLongitudeOfPerigee(TimeElement time, ObserverElement obs,
			EphemerisElement eph) throws JPARSECException {
		double jd = TimeScale.getJD(time, obs, eph, SCALE.TERRESTRIAL_TIME);
    	double T = Functions.toCenturies(jd);
    	double omega = 83.3532430 + 4096.0137111 * T - 0.0103238 * T * T - T * T * T / 80053.0 + T * T * T * T / 18999000.0;
    	return Functions.normalizeRadians(omega * Constant.DEG_TO_RAD);
    }

    /**
     * Calculates the instant of the a pass of the Moon through its descending node,
     * following Meeus. Accuracy is usually better than 1 minute, and always better than 3.
     * Time is corrected for Moon secular acceleration.
     * @param jd The starting Julian day of calculations (TDB).
     * @param eventType Next, closest, or previous events, as defined in this constants
     * of {@linkplain MainEvents} class. The use of the closest option is recommended when
     * possible, since next/previous events could give incorrect events for a given date
     * far from J2000.
     * @return The event.
     * @throws JPARSECException If an error occurs.
     */
    public static SimpleEventElement getPassThroughDescendingNode(double jd, EVENT_TIME eventType) throws JPARSECException {
		AstroDate astro = new AstroDate(jd);
		double year = astro.getAstronomicalYear() + (astro.getMonth() - 1.0 + astro.getDayPlusFraction() / (1.0 + astro.getDaysInMonth())) / 12.0;

        double k = (year - 2000.05) * 13.4223;
		double kapprox = k;
		k = 0.5 + Math.floor(k + 0.5);
		if (eventType == MainEvents.EVENT_TIME.PREVIOUS) k --;
		if (eventType == MainEvents.EVENT_TIME.CLOSEST) {
			double dif1 = Math.abs(kapprox-k);
			double dif2 = Math.abs(kapprox-(k-1));
			if (dif2 < dif1) k--;
		}

		// Fix k value if required
		double T = k / 1342.23;
		double jde = 2451565.1619 + 27.212220817 * k + 0.0002572 * T * T + 0.000000021 * T * T * T - 0.000000000088 * T * T * T * T;
		if (jde > jd && eventType == EVENT_TIME.PREVIOUS) k --;
		if (jde < jd && eventType == EVENT_TIME.NEXT) k ++;
		if (jde > jd && eventType != EVENT_TIME.PREVIOUS) {
			double km = k - 1;
			T = km / 1342.23;
			double newjde = 2451565.1619 + 27.212220817 * km + 0.0002572 * T * T + 0.000000021 * T * T * T - 0.000000000088 * T * T * T * T;
			if ((newjde > jd && eventType == EVENT_TIME.NEXT) ||
					(Math.abs(jd-newjde) < Math.abs(jd-jde) && eventType == EVENT_TIME.CLOSEST)) k = km;
		}
		if (jde < jd && eventType != EVENT_TIME.NEXT) {
			double kp = k + 1;
			T = kp / 1342.23;
			double newjde = 2451565.1619 + 27.212220817 * kp + 0.0002572 * T * T + 0.000000021 * T * T * T - 0.000000000088 * T * T * T * T;
			if ((newjde < jd && eventType == EVENT_TIME.PREVIOUS) ||
					(Math.abs(jd-newjde) < Math.abs(jd-jde) && eventType == EVENT_TIME.CLOSEST)) k = kp;
		}
		T = k / 1342.23;


		double D = 183.6380 + 331.73735691 * k + 0.001505 * T * T + 0.00000209 * T * T * T - 0.000000010 * T * T * T * T;
		double M = 17.4006 + 26.82037250 * k + 0.0000999 * T * T + 0.00000006 * T * T * T;
		double Mp = 38.3776 + 355.52747322 * k + 0.0123577 * T * T + 0.000014628 * T * T * T - 0.000000069 * T * T * T * T;
		double omega = 123.9767 - 1.44098949 * k + 0.0020625 * T * T + 0.00000214 * T * T * T - 0.000000016 * T * T * T * T;
		double V = 299.75 + 132.85 * T - 0.009173 * T * T;
		double P = omega + 272.75 - 2.3 * T;

		D *= Constant.DEG_TO_RAD;
		M *= Constant.DEG_TO_RAD;
		Mp *= Constant.DEG_TO_RAD;
		omega *= Constant.DEG_TO_RAD;
		V *= Constant.DEG_TO_RAD;
		P *= Constant.DEG_TO_RAD;
		double E = 1.0 - 0.002516 * T - 0.0000074 * T * T;

		jd = 2451565.1619 + 27.212220817 * k + 0.0002572 * T * T + 0.000000021 * T * T * T - 0.000000000088 * T * T * T * T -
			0.4721 * Math.sin(Mp) - 0.1649 * Math.sin(2.0 * D) - 0.0868 * Math.sin(2.0 * D - Mp) +
			0.0084 * Math.sin(2.0 * D + Mp) - 0.0083 * E * Math.sin(2.0 * D - M) - 0.0039 * E * Math.sin(2.0 * D - M - Mp) +
			0.0034 * Math.sin(2.0 * Mp) - 0.0031 * Math.sin(2.0 * D - 2.0 * Mp) + 0.0030 * E * Math.sin(2.0 * D + M) +
			0.0028 * E * Math.sin(M - Mp) + 0.0026 * E * Math.sin(M) + 0.0025 * Math.sin(4.0 * D) + 0.0024 * Math.sin(D) +
			0.0022 * E * Math.sin(M + Mp) + 0.0017 * Math.sin(omega) + 0.0014 * Math.sin(4.0 * D - Mp) +
			0.0005 * E * Math.sin(2.0 * D + M - Mp) + 0.0004 * E * Math.sin(2.0 * D - M + Mp) -
			0.0003 * E * Math.sin(2.0 * D - 2.0 * M) + 0.0003 * E * Math.sin(4.0 * D - M) + 0.0003 * (Math.sin(V) + Math.sin(P));

		// Correct Meeus results for secular acceleration
		double deltaT = Elp2000.timeCorrectionForSecularAcceleration(jd) - jd;
		jd -= deltaT;

        SimpleEventElement se = new SimpleEventElement(jd, SimpleEventElement.EVENT.MOON_DESCENDING_NODE, "");
        se.body = TARGET.Moon.getName();
        return se;
    }

    /**
     * Calculates the instant of the a pass of the Moon through its ascending node,
     * following Meeus. Accuracy is usually better than 1 minute, and always better than 3.
     * Time is corrected for Moon secular acceleration.
     * @param jd The starting Julian day of calculations (TDB).
     * @param eventType Next, closest, or previous events, as defined in this constants
     * of {@linkplain MainEvents} class. The use of the closest option is recommended when
     * possible, since next/previous events could give incorrect events for a given date
     * far from J2000.
     * @return The event.
     * @throws JPARSECException If an error occurs.
     */
    public static SimpleEventElement getPassThroughAscendingNode(double jd, EVENT_TIME eventType) throws JPARSECException {
		AstroDate astro = new AstroDate(jd);
		double year = astro.getAstronomicalYear() + (astro.getMonth() - 1.0 + astro.getDayPlusFraction() / (1.0 + astro.getDaysInMonth())) / 12.0;

        double k = (year - 2000.05) * 13.4223;
		double kapprox = k;
		k = 1.0 + Math.floor(k);
		if (eventType == MainEvents.EVENT_TIME.PREVIOUS) k --;
		if (eventType == MainEvents.EVENT_TIME.CLOSEST) {
			double dif1 = Math.abs(kapprox-k);
			double dif2 = Math.abs(kapprox-(k-1));
			if (dif2 < dif1) k--;
		}

		// Fix k value if required
		double T = k / 1342.23;
		double jde = 2451565.1619 + 27.212220817 * k + 0.0002572 * T * T + 0.000000021 * T * T * T - 0.000000000088 * T * T * T * T;
		if (jde > jd && eventType == EVENT_TIME.PREVIOUS) k --;
		if (jde < jd && eventType == EVENT_TIME.NEXT) k ++;
		if (jde > jd && eventType != EVENT_TIME.PREVIOUS) {
			double km = k - 1;
			T = km / 1342.23;
			double newjde = 2451565.1619 + 27.212220817 * km + 0.0002572 * T * T + 0.000000021 * T * T * T - 0.000000000088 * T * T * T * T;
			if ((newjde > jd && eventType == EVENT_TIME.NEXT) ||
					(Math.abs(jd-newjde) < Math.abs(jd-jde) && eventType == EVENT_TIME.CLOSEST)) k = km;
		}
		if (jde < jd && eventType != EVENT_TIME.NEXT) {
			double kp = k + 1;
			T = kp / 1342.23;
			double newjde = 2451565.1619 + 27.212220817 * kp + 0.0002572 * T * T + 0.000000021 * T * T * T - 0.000000000088 * T * T * T * T;
			if ((newjde < jd && eventType == EVENT_TIME.PREVIOUS) ||
					(Math.abs(jd-newjde) < Math.abs(jd-jde) && eventType == EVENT_TIME.CLOSEST)) k = kp;
		}
		T = k / 1342.23;

		double D = 183.6380 + 331.73735691 * k + 0.001505 * T * T + 0.00000209 * T * T * T - 0.000000010 * T * T * T * T;
		double M = 17.4006 + 26.82037250 * k + 0.0000999 * T * T + 0.00000006 * T * T * T;
		double Mp = 38.3776 + 355.52747322 * k + 0.0123577 * T * T + 0.000014628 * T * T * T - 0.000000069 * T * T * T * T;
		double omega = 123.9767 - 1.44098949 * k + 0.0020625 * T * T + 0.00000214 * T * T * T - 0.000000016 * T * T * T * T;
		double V = 299.75 + 132.85 * T - 0.009173 * T * T;
		double P = omega + 272.75 - 2.3 * T;
		double E = 1.0 - 0.002516 * T - 0.0000074 * T * T;

		D *= Constant.DEG_TO_RAD;
		M *= Constant.DEG_TO_RAD;
		Mp *= Constant.DEG_TO_RAD;
		omega *= Constant.DEG_TO_RAD;
		V *= Constant.DEG_TO_RAD;
		P *= Constant.DEG_TO_RAD;

		jd = 2451565.1619 + 27.212220817 * k + 0.0002572 * T * T + 0.000000021 * T * T * T - 0.000000000088 * T * T * T * T -
			0.4721 * Math.sin(Mp) - 0.1649 * Math.sin(2.0 * D) - 0.0868 * Math.sin(2.0 * D - Mp) +
			0.0084 * Math.sin(2.0 * D + Mp) - 0.0083 * E * Math.sin(2.0 * D - M) - 0.0039 * E * Math.sin(2.0 * D - M - Mp) +
			0.0034 * Math.sin(2.0 * Mp) - 0.0031 * Math.sin(2.0 * D - 2.0 * Mp) + 0.0030 * E * Math.sin(2.0 * D + M) +
			0.0028 * E * Math.sin(M - Mp) + 0.0026 * E * Math.sin(M) + 0.0025 * Math.sin(4.0 * D) + 0.0024 * Math.sin(D) +
			0.0022 * E * Math.sin(M + Mp) + 0.0017 * Math.sin(omega) + 0.0014 * Math.sin(4.0 * D - Mp) +
			0.0005 * E * Math.sin(2.0 * D + M - Mp) + 0.0004 * E * Math.sin(2.0 * D - M + Mp) -
			0.0003 * E * Math.sin(2.0 * D - 2.0 * M) + 0.0003 * E * Math.sin(4.0 * D - M) + 0.0003 * (Math.sin(V) + Math.sin(P));

		// Correct Meeus results for secular acceleration
		double deltaT = Elp2000.timeCorrectionForSecularAcceleration(jd) - jd;
		jd -= deltaT;

        SimpleEventElement se = new SimpleEventElement(jd, SimpleEventElement.EVENT.MOON_ASCENDING_NODE, "");
        se.body = TARGET.Moon.getName();
        return se;
    }

	/**
	 * Calculates the instant of a given lunar maximum geocentric declination,
	 * following Meeus's Astronomical Algorithms. Error is a few minutes at J2000, and could
	 * reach half an hour 3000 years after or before J2000 epoch. Time is corrected for Moon
	 * secular acceleration.
	 * @param jd The starting Julian day of calculations.
	 * @param eventType The event type (next, last, or closest to input date).  The use of the
	 * closest option is recommended when possible, since next/previous events could give incorrect
	 * events for a given date far from J2000.
	 * @return The event. The details field will contains the value of the declination in degrees.
	 * @throws JPARSECException If an error occurs.
	 */
	public static SimpleEventElement MoonMaximumDeclination(double jd, EVENT_TIME eventType)
	throws JPARSECException {
		AstroDate astro = new AstroDate(jd);
		double year = astro.getAstronomicalYear() + (astro.getMonth() - 1.0 + astro.getDayPlusFraction() / (1.0 + astro.getDaysInMonth())) / 12.0;

		double kapprox = (year - 2000.03) * 13.3686;
		double k = Math.round(kapprox);
		if (eventType == EVENT_TIME.PREVIOUS && k > kapprox) k --;
		if (eventType == EVENT_TIME.NEXT && k < kapprox) k ++;

		double T = k / 1336.86;
		double jde = 2451562.5897 + 27.321582241 * k + 0.000100692 * T * T - 0.000000141 * T * T * T;
		if (jde > jd && eventType == EVENT_TIME.PREVIOUS) k --;
		if (jde < jd && eventType == EVENT_TIME.NEXT) k ++;
		if (jde > jd && eventType != EVENT_TIME.PREVIOUS) {
			double km = k - 1;
			T = km / 1336.86;
			double newjde = 2451562.5897 + 27.321582241 * km + 0.000100692 * T * T - 0.000000141 * T * T * T;
			if ((newjde > jd && eventType == EVENT_TIME.NEXT) ||
					(Math.abs(jd-newjde) < Math.abs(jd-jde) && eventType == EVENT_TIME.CLOSEST)) k = km;
		}
		if (jde < jd && eventType != EVENT_TIME.NEXT) {
			double kp = k + 1;
			T = kp / 1336.86;
			double newjde = 2451562.5897 + 27.321582241 * kp + 0.000100692 * T * T - 0.000000141 * T * T * T;
			if ((newjde < jd && eventType == EVENT_TIME.PREVIOUS) ||
					(Math.abs(jd-newjde) < Math.abs(jd-jde) && eventType == EVENT_TIME.CLOSEST)) k = kp;
		}
		T = k / 1336.86;

		double D = 152.3039 + 333.0705546 * k - 0.0004025 * T * T + 0.00000011 * T * T * T;
		double M = 14.8591 + 26.9281592 * k - 0.0000544 * T * T - 0.00000010 * T * T * T;
		double Mp = 4.6881 + 356.9562795 * k + 0.0103126 * T * T + 0.00001251 * T * T * T;
		double F = 325.8867 + 1.4467806 * k - 0.0020708 * T * T - 0.00000215 * T * T * T;

		D *= Constant.DEG_TO_RAD;
		M *= Constant.DEG_TO_RAD;
		Mp *= Constant.DEG_TO_RAD;
		F *= Constant.DEG_TO_RAD;

		jde = 2451562.5897 + 27.321582241 * k + 0.000100692 * T * T - 0.000000141 * T * T * T;
		double E = 1.0 - 0.002516 * T - 0.0000074 * T * T;
		double coefs[] = new double[] {
				0.8975, -0.4726, -0.1030, -0.0976, -0.0462, -0.0461, -0.0438, 0.0162, -0.0157,
				0.0145, 0.0136, -0.0095, -0.0091, -0.0089, 0.0075, -0.0068, 0.0061, -0.0047,
				-0.0043, -0.0040, -0.0037, 0.0031, 0.0030, -0.0029, -0.0029, -0.0027, 0.0024,
				-0.0021, 0.0019, 0.0018, 0.0018, 0.0017, 0.0017, -0.0014, 0.0013, 0.0013, 0.0012,
				0.0011, -0.0011, 0.0010, 0.0010, -0.0009, 0.0007, -0.0007
		};

		jde += coefs[0] * Math.cos(F) + coefs[1] * Math.sin(Mp) + coefs[2] * Math.sin(2.0*F) +
				coefs[3] * Math.sin(2.0*D-Mp) + coefs[4] * Math.cos(Mp-F) + coefs[5] * Math.cos(Mp+F) +
				coefs[6] * Math.sin(2.0*D) + coefs[7] * E * Math.sin(M) + coefs[8] * Math.cos(3.0*F) +
				coefs[9] * Math.sin(Mp+2.0*F) + coefs[10] * Math.cos(2.0*D-F) + coefs[11] * Math.cos(2.0*D-Mp-F) +
				coefs[12] * Math.cos(2.0*D-Mp+F) + coefs[13] * Math.cos(2.0*D+F) + coefs[14] * Math.sin(2.0*Mp) +
				coefs[15] * Math.sin(Mp-2.0*F) + coefs[16] * Math.cos(2.0*Mp-F) + coefs[17] * Math.sin(Mp+3.0*F) +
				coefs[18] * E * Math.sin(2.0*D-M-Mp) + coefs[19] * Math.cos(Mp-2.0*F) + coefs[20] * Math.sin(2.0*D-2.0*Mp) +
				coefs[21] * Math.sin(F) + coefs[22] * Math.sin(2.0*D+Mp) + coefs[23] * Math.cos(Mp+2.0*F) +
				coefs[24] * E * Math.sin(2.0*D-M) + coefs[25] * Math.sin(Mp+F) + coefs[26] * E * Math.sin(M-Mp) +
				coefs[27] * Math.sin(Mp-3.0*F) + coefs[28] * Math.sin(2.0*Mp+F) + coefs[29] * Math.cos(2.0*D-2.0*Mp-F) +
				coefs[30] * Math.sin(3.0*F) + coefs[31] * Math.cos(Mp+3.0*F) + coefs[32] * Math.cos(2.0*Mp) +
				coefs[33] * Math.cos(2.0*D-Mp) + coefs[34] * Math.cos(2.0*D+Mp+F) + coefs[35] * Math.cos(Mp) +
				coefs[36] * Math.sin(3.0*Mp+F) + coefs[37] * Math.sin(2.0*D-Mp+F) + coefs[38] * Math.cos(2.0*D-2.0*Mp) +
				coefs[39] * Math.cos(D+F) + coefs[40] * E * Math.sin(M+Mp) + coefs[41] * Math.sin(2.0*D-2.0*F) +
				coefs[42] * Math.cos(2.0*Mp+F) + coefs[43] * Math.cos(3.0*Mp+F);

		double dec = 23.6961 - 0.013004 * T;

		coefs = new double[] {
				5.1093, 0.2658, 0.1448, -0.0322, 0.0133, 0.0125, -0.0124, -0.0101, 0.0097, -0.0087,
				0.0074, 0.0067, 0.0063, 0.0060, -0.0057, -0.0056, 0.0052, 0.0041, -0.0040, 0.0038,
				-0.0034, -0.0029, 0.0029, -0.0028, -0.0028, -0.0023, -0.0021, 0.0019, 0.0018, 0.0017,
				0.0015, 0.0014, -0.0012, -0.0012, -0.0010, -0.0010, 0.0006
		};

		dec += coefs[0] * Math.sin(F) + coefs[1] * Math.cos(2.0*F) + coefs[2] * Math.sin(2.0*D-F) +
				coefs[3] * Math.sin(3.0*F) + coefs[4] * Math.cos(2.0*D-2.0*F) + coefs[5] * Math.cos(2.0*D) +
				coefs[6] * Math.sin(Mp-F) + coefs[7] * Math.sin(Mp+2.0*F) + coefs[8] * Math.cos(F) +
				coefs[9] * E * Math.sin(2.0*D+M-F) + coefs[10] * Math.sin(Mp+3.0*F) + coefs[11] * Math.sin(D+F) +
				coefs[12] * Math.sin(Mp-2.0*F) + coefs[13] * E * Math.sin(2.0*D-M-F) + coefs[14] * Math.sin(2.0*D-Mp-F) +
				coefs[15] * Math.cos(Mp+F) + coefs[16] * Math.cos(Mp+2.0*F) + coefs[17] * Math.cos(2.0*Mp+F) +
				coefs[18] * Math.cos(Mp-3.0*F) + coefs[19] * Math.cos(2.0*Mp-F) + coefs[20] * Math.cos(Mp-2.0*F) +
				coefs[21] * Math.sin(2.0*Mp) + coefs[22] * Math.sin(3.0*Mp+F) + coefs[23] * E * Math.cos(2.0*D+M-F) +
				coefs[24] * Math.cos(Mp-F) + coefs[25] * Math.cos(3.0*F) + coefs[26] * Math.sin(2.0D+F) +
				coefs[27] * Math.cos(Mp+3.0*F) + coefs[28] * Math.cos(D+F) + coefs[29] * Math.sin(2.0*Mp-F) +
				coefs[30] * Math.cos(3.0*Mp+F) + coefs[31] * Math.cos(2.0*D+2.0*Mp+F) + coefs[32] * Math.sin(2.0*D-2.0*Mp-F) +
				coefs[33] * Math.cos(2.0*Mp) + coefs[34] * Math.cos(Mp) + coefs[35] * Math.sin(2.0*F) +
				coefs[36] * Math.sin(Mp+F);

		// Correct Meeus results for secular acceleration
		double deltaT = Elp2000.timeCorrectionForSecularAcceleration(jde) - jde;
		jde -= deltaT;

		SimpleEventElement see = new SimpleEventElement(jde, SimpleEventElement.EVENT.MOON_MAXIMUM_DECLINATION, ""+dec);
		see.body = TARGET.Moon.getName();
		return see;
	}

	/**
	 * Calculates the instant of a given lunar minimum geocentric declination,
	 * following Meeus's Astronomical Algorithms. Error is a few minutes at J2000, and could
	 * reach half an hour 3000 years after or before J2000 epoch. Time is corrected for Moon
	 * secular acceleration.
	 * @param jd The starting Julian day of calculations.
	 * @param eventType The event type (next, last, or closest to input date).  The use of the
	 * closest option is recommended when possible, since next/previous events could give incorrect
	 * events for a given date far from J2000.
	 * @return The event. The details field will contains the value of the declination in degrees.
	 * @throws JPARSECException If an error occurs.
	 */
	public static SimpleEventElement MoonMinimumDeclination(double jd, EVENT_TIME eventType)
	throws JPARSECException {
		AstroDate astro = new AstroDate(jd);
		double year = astro.getAstronomicalYear() + (astro.getMonth() - 1.0 + astro.getDayPlusFraction() / (1.0 + astro.getDaysInMonth())) / 12.0;

		double kapprox = (year - 2000.03) * 13.3686;
		double k = Math.round(kapprox);
		if (eventType == EVENT_TIME.PREVIOUS && k > kapprox) k --;
		if (eventType == EVENT_TIME.NEXT && k < kapprox) k ++;

		double T = k / 1336.86;
		double jde = 2451548.9289 + 27.321582241 * k + 0.000100692 * T * T - 0.000000141 * T * T * T;
		if (jde > jd && eventType == EVENT_TIME.PREVIOUS) k --;
		if (jde < jd && eventType == EVENT_TIME.NEXT) k ++;
		if (jde > jd && eventType != EVENT_TIME.PREVIOUS) {
			double km = k - 1;
			T = km / 1336.86;
			double newjde = 2451548.9289 + 27.321582241 * km + 0.000100692 * T * T - 0.000000141 * T * T * T;
			if ((newjde > jd && eventType == EVENT_TIME.NEXT) ||
					(Math.abs(jd-newjde) < Math.abs(jd-jde) && eventType == EVENT_TIME.CLOSEST)) k = km;
		}
		if (jde < jd && eventType != EVENT_TIME.NEXT) {
			double kp = k + 1;
			T = kp / 1336.86;
			double newjde = 2451548.9289 + 27.321582241 * kp + 0.000100692 * T * T - 0.000000141 * T * T * T;
			if ((newjde < jd && eventType == EVENT_TIME.PREVIOUS) ||
					(Math.abs(jd-newjde) < Math.abs(jd-jde) && eventType == EVENT_TIME.CLOSEST)) k = kp;
		}
		T = k / 1336.86;

		double D = 345.6676 + 333.0705546 * k - 0.0004025 * T * T + 0.00000011 * T * T * T;
		double M = 1.3951 + 26.9281592 * k - 0.0000544 * T * T - 0.00000010 * T * T * T;
		double Mp = 186.2100 + 356.9562795 * k + 0.0103126 * T * T + 0.00001251 * T * T * T;
		double F = 145.1633 + 1.4467806 * k - 0.0020708 * T * T - 0.00000215 * T * T * T;

		D *= Constant.DEG_TO_RAD;
		M *= Constant.DEG_TO_RAD;
		Mp *= Constant.DEG_TO_RAD;
		F *= Constant.DEG_TO_RAD;

		jde = 2451548.9289 + 27.321582241 * k + 0.000100692 * T * T - 0.000000141 * T * T * T;

		double E = 1.0 - 0.002516 * T - 0.0000074 * T * T;
		double coefs[] = new double[] {
				-0.8975, -0.4726, -0.1030, -0.0976, 0.0541, 0.0516, -0.0438, 0.0112, 0.0157,
				0.0023, -0.0136, 0.0110, 0.0091, 0.0089, 0.0075, -0.0030, -0.0061, -0.0047,
				-0.0043, 0.0040, -0.0037, -0.0031, 0.0030, 0.0029, -0.0029, -0.0027, 0.0024,
				-0.0021, -0.0019, -0.0006, -0.0018, -0.0017, 0.0017, 0.0014, -0.0013, -0.0013, 0.0012,
				0.0011, 0.0011, 0.0010, 0.0010, -0.0009, -0.0007, -0.0007
		};

		jde += coefs[0] * Math.cos(F) + coefs[1] * Math.sin(Mp) + coefs[2] * Math.sin(2.0*F) +
				coefs[3] * Math.sin(2.0*D-Mp) + coefs[4] * Math.cos(Mp-F) + coefs[5] * Math.cos(Mp+F) +
				coefs[6] * Math.sin(2.0*D) + coefs[7] * E * Math.sin(M) + coefs[8] * Math.cos(3.0*F) +
				coefs[9] * Math.sin(Mp+2.0*F) + coefs[10] * Math.cos(2.0*D-F) + coefs[11] * Math.cos(2.0*D-Mp-F) +
				coefs[12] * Math.cos(2.0*D-Mp+F) + coefs[13] * Math.cos(2.0*D+F) + coefs[14] * Math.sin(2.0*Mp) +
				coefs[15] * Math.sin(Mp-2.0*F) + coefs[16] * Math.cos(2.0*Mp-F) + coefs[17] * Math.sin(Mp+3.0*F) +
				coefs[18] * E * Math.sin(2.0*D-M-Mp) + coefs[19] * Math.cos(Mp-2.0*F) + coefs[20] * Math.sin(2.0*D-2.0*Mp) +
				coefs[21] * Math.sin(F) + coefs[22] * Math.sin(2.0*D+Mp) + coefs[23] * Math.cos(Mp+2.0*F) +
				coefs[24] * E * Math.sin(2.0*D-M) + coefs[25] * Math.sin(Mp+F) + coefs[26] * E * Math.sin(M-Mp) +
				coefs[27] * Math.sin(Mp-3.0*F) + coefs[28] * Math.sin(2.0*Mp+F) + coefs[29] * Math.cos(2.0*D-2.0*Mp-F) +
				coefs[30] * Math.sin(3.0*F) + coefs[31] * Math.cos(Mp+3.0*F) + coefs[32] * Math.cos(2.0*Mp) +
				coefs[33] * Math.cos(2.0*D-Mp) + coefs[34] * Math.cos(2.0*D+Mp+F) + coefs[35] * Math.cos(Mp) +
				coefs[36] * Math.sin(3.0*Mp+F) + coefs[37] * Math.sin(2.0*D-Mp+F) + coefs[38] * Math.cos(2.0*D-2.0*Mp) +
				coefs[39] * Math.cos(D+F) + coefs[40] * E * Math.sin(M+Mp) + coefs[41] * Math.sin(2.0*D-2.0*F) +
				coefs[42] * Math.cos(2.0*Mp+F) + coefs[43] * Math.cos(3.0*Mp+F);

		double dec = 23.6961 - 0.013004 * T;

		coefs = new double[] {
				-5.1093, 0.2658, -0.1448, 0.0322, 0.0133, 0.0125, -0.0015, 0.0101, -0.0097, 0.0087,
				0.0074, 0.0067, -0.0063, -0.0060, 0.0057, -0.0056, -0.0052, -0.0041, -0.0040, -0.0038,
				0.0034, -0.0029, 0.0029, 0.0028, -0.0028, 0.0023, 0.0021, 0.0019, 0.0018, -0.0017,
				0.0015, 0.0014, 0.0012, -0.0012, 0.0010, -0.0010, 0.0037
		};

		dec += coefs[0] * Math.sin(F) + coefs[1] * Math.cos(2.0*F) + coefs[2] * Math.sin(2.0*D-F) +
				coefs[3] * Math.sin(3.0*F) + coefs[4] * Math.cos(2.0*D-2.0*F) + coefs[5] * Math.cos(2.0*D) +
				coefs[6] * Math.sin(Mp-F) + coefs[7] * Math.sin(Mp+2.0*F) + coefs[8] * Math.cos(F) +
				coefs[9] * E * Math.sin(2.0*D+M-F) + coefs[10] * Math.sin(Mp+3.0*F) + coefs[11] * Math.sin(D+F) +
				coefs[12] * Math.sin(Mp-2.0*F) + coefs[13] * E * Math.sin(2.0*D-M-F) + coefs[14] * Math.sin(2.0*D-Mp-F) +
				coefs[15] * Math.cos(Mp+F) + coefs[16] * Math.cos(Mp+2.0*F) + coefs[17] * Math.cos(2.0*Mp+F) +
				coefs[18] * Math.cos(Mp-3.0*F) + coefs[19] * Math.cos(2.0*Mp-F) + coefs[20] * Math.cos(Mp-2.0*F) +
				coefs[21] * Math.sin(2.0*Mp) + coefs[22] * Math.sin(3.0*Mp+F) + coefs[23] * E * Math.cos(2.0*D+M-F) +
				coefs[24] * Math.cos(Mp-F) + coefs[25] * Math.cos(3.0*F) + coefs[26] * Math.sin(2.0D+F) +
				coefs[27] * Math.cos(Mp+3.0*F) + coefs[28] * Math.cos(D+F) + coefs[29] * Math.sin(2.0*Mp-F) +
				coefs[30] * Math.cos(3.0*Mp+F) + coefs[31] * Math.cos(2.0*D+2.0*Mp+F) + coefs[32] * Math.sin(2.0*D-2.0*Mp-F) +
				coefs[33] * Math.cos(2.0*Mp) + coefs[34] * Math.cos(Mp) + coefs[35] * Math.sin(2.0*F) +
				coefs[36] * Math.sin(Mp+F);

		dec = -dec;

		// Correct Meeus results for secular acceleration
		double deltaT = Elp2000.timeCorrectionForSecularAcceleration(jde) - jde;
		jde -= deltaT;

		SimpleEventElement see = new SimpleEventElement(jde, SimpleEventElement.EVENT.MOON_MINIMUM_DECLINATION, ""+dec);
		see.body = TARGET.Moon.getName();
		return see;
	}

	/**
	 * Returns the lunar libration angles for JPL or Moshier ephemerides. The conversion from
	 * Mean Earth system to Principal Axis system is performed using Williams et al. 2008 formulae
	 * if ephemerides algorithm is DE422, and Seidelmann et al. 2007 (Konopliv et al. 2001)
	 * for the rest of DE4xx and Moshier's method. Results are very close to Eckhardt's theory,
	 * and better than those reported by IAU approximations for the lunar north pole or rotation.
	 * @param time Time object.
	 * @param obs Observer.
	 * @param eph Ephemerides properties. They are forced to be apparent and for the equinox
	 * of date. Algorithm must be Moshier or JPL DE403/405/413/414/422methods.
	 * @return An array with total libration (physical + optical) in longitude and latitude, and axis
	 * position angle. Units are radians, referred to mean equinox of date.
	 * @throws JPARSECException If an error occurs.
	 */
    public static double[] getJPLMoonLibrations(TimeElement time, ObserverElement obs,
    		EphemerisElement eph) throws JPARSECException {
		EphemerisElement eph_copy = eph.clone();
		eph_copy.ephemType = EphemerisElement.COORDINATES_TYPE.APPARENT;
		eph_copy.equinox = EphemerisElement.EQUINOX_OF_DATE;
		eph_copy.targetBody = TARGET.Moon;
		eph_copy.preferPrecisionInEphemerides = false;
		EphemElement ephemMoon = Ephem.getEphemeris(time, obs, eph_copy, false);

		return LunarEvent.getJPLMoonLibrations(time, obs, eph, ephemMoon.getEquatorialLocation());
    }

	/**
	 * Returns the lunar libration angles for JPL or Moshier ephemerides. The conversion from
	 * Mean Earth system to Principal Axis system is performed using Williams et al. 2008 formulae
	 * if ephemerides algorithm is DE422, and Seidelmann et al. 2007 (Konopliv et al. 2001)
	 * for the rest of DE4xx and Moshier's method. Results are very close to Eckhardt's theory,
	 * and better than those reported by IAU approximations for the lunar north pole or rotation.
	 * @param time Time object.
	 * @param obs Observer.
	 * @param eph Ephemerides properties. They are forced to be apparent and for the equinox
	 * of date. Algorithm must be Moshier or JPL DE403/405/413/414/422 methods.
	 * @param locEquatorial Equatorial apparent position of the Moon for date jd.
	 * @return An array with total libration (physical + optical) in longitude and latitude, and axis
	 * position angle. Units are radians, referred to mean equinox of date.
	 * @throws JPARSECException If an error occurs.
	 */
    public static double[] getJPLMoonLibrations(TimeElement time, ObserverElement obs,
    		EphemerisElement eph, LocationElement locEquatorial) throws JPARSECException {
		double jd = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		LocationElement eclMoon = CoordinateSystem.equatorialToEcliptic(locEquatorial, time, obs, eph);

		double I = 5553.6 * Constant.ARCSEC_TO_RAD;

		double t = Functions.toCenturies(jd); // - locEquatorial.getRadius() * Constant.LIGHT_TIME_DAYS_PER_AU); // Neglecting this since produces slowdown for nutation calculation

		// Mean longitude of the ascending node of the Moon.
		double om = Functions
				.mod3600(450160.398036 + t * (-6962890.5431 + t * (7.4722 + t * (0.007702 + t * (-0.00005939))))) * Constant.ARCSEC_TO_RAD;

		// Mean longitude of the Moon.
		double lm = Functions
		.mod3600(218.31664563 * 3600.0 + t * (1732564372.30470 + t * (-5.2790 + t * (0.006665 + t * (-0.00005522))))) * Constant.ARCSEC_TO_RAD;

		t = Functions.toCenturies(jd);
		double eps = Obliquity.trueObliquity(t, eph);

		double N = Nutation.getNutationInLongitude();
		double coscos = Math.cos(eclMoon.getLatitude()) * Math.cos(eclMoon.getLongitude() - om - N);
		double cossin = Math.cos(I) * Math.cos(eclMoon.getLatitude()) * Math.sin(eclMoon.getLongitude() - om - N) - Math.sin(I) * Math.sin(eclMoon.getLatitude());
		double sinb = - Math.sin(I) * Math.cos(eclMoon.getLatitude()) * Math.sin(eclMoon.getLongitude() - om - N) - Math.cos(I) * Math.sin(eclMoon.getLatitude());

		double l = Math.atan2(cossin, coscos) - lm + om;
		double b = Math.asin(sinb);

		double sinsin = -Math.sin(eps) * Math.sin(om + N);
		cossin = Math.sin(I) * Math.cos(eps) - Math.cos(I) * Math.sin(eps) * Math.cos(om + N);
		//double delta = Math.atan2(sinsin, cossin);

		//double cosi = Math.cos(I) * Math.cos(eps) + Math.sin(I) * Math.sin(eps) * Math.cos(om+N);
		sinsin = -Math.sin(I) * Math.sin(om + N);
		cossin = Math.cos(I) * Math.sin(eps) - Math.sin(I) * Math.cos(eps) * Math.cos(om + N);
		double omp = Math.atan2(sinsin, cossin);
		double i = Math.asin(sinsin / Math.sin(omp));

		cossin = -Math.sin(i) * Math.cos(omp - locEquatorial.getLongitude());
		coscos = Math.cos(locEquatorial.getLatitude()) * Math.cos(i) - Math.sin(locEquatorial.getLatitude()) * Math.sin(i) * Math.sin(omp-locEquatorial.getLongitude());
		double Cp = Math.atan2(cossin, coscos); // axis PA

		// PA -> ME system rotation
		// Seidelmann et al. 2007 (Konopliv et al. 2001, page 7): values for DE403/Moshier.
		// Sorry I don't have the values for DE405/413/414, so I use DE403 values.
		// I also assume DE422/4 values = DE421 ones
		double ra1 = 0.1462, ra2 = 79.0768, ra3 = 63.8986; // WGCCRE by IAU, MEP system
		double lib[] = null;
		switch (eph.algorithm) {
		case MOSHIER:
		case SERIES96_MOSHIERForMoon:
			lib = PlanetEphem.getHeliocentricEclipticPositionJ2000(jd, TARGET.Libration);
			break;
		case JPL_DE403:
		case JPL_DE405:
		case JPL_DE406:
		case JPL_DE413:
		case JPL_DE414: // Valid also for DE421, following IAU 2015 report
			ra1 = 0.30; // Williams et al. 2008, page 10, DE403
			ra2 = 78.56;
			ra3 = 67.92;
			break;
		case JPL_DE422:
		case JPL_DE424:
			// https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/planets/de430_moon_coord.pdf, page 14, DE421
			ra1 = 0.290; // JPL's PA system
			ra2 = 78.513;
			ra3 = 67.753;
			break;
		case JPL_DE430:
		case JPL_DE431:
			// https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/planets/de430_moon_coord.pdf, page 13, DE430
			ra1 = 0.285;
			ra2 = 78.58;
			ra3 = 67.573;
			break;
		default:
			if (!eph.algorithm.isJPL() || eph.algorithm == ALGORITHM.JPL_DE200) 
				throw new JPARSECException("Unsupported reduction method.");
			// Use DE430 values
			ra1 = 0.285;
			ra2 = 78.58;
			ra3 = 67.573;
		}
		
		if (eph.algorithm.isJPL()) {
			JPLEphemeris jpl = new JPLEphemeris(eph.algorithm);
			lib = jpl.getPositionAndVelocity(jd, TARGET.Libration);			
		}

		// Now again following http://astro.ukho.gov.uk/data/tn/naotn74.pdf
		Matrix r1 = Matrix.getR1(ra1 * Constant.ARCSEC_TO_RAD);
		Matrix r2 = Matrix.getR2(ra2 * Constant.ARCSEC_TO_RAD);
		Matrix r3 = Matrix.getR3(ra3 * Constant.ARCSEC_TO_RAD);
		Matrix rSelTor1 = r3.times(r2);
		rSelTor1 = rSelTor1.times(r1);
		//rSelTor1.print(11, 9);

		double phi = lib[0], theta = lib[1], psi = lib[2];
		r1 = Matrix.getR3(-psi);
		r2 = Matrix.getR1(-theta);
		r3 = Matrix.getR3(-phi);
		Matrix r1Tor2 = r3.times(r2);
		r1Tor2 = r1Tor2.times(r1);
		//r1Tor2.print(11, 9);

		Matrix NPB = IAU2006.getNPB(time, obs, eph);
		//NPB.print(11, 9);

		Matrix r1Eps = Matrix.getR1(eps);
		//r1Eps.print(11, 9);

		r2 = r1Tor2.times(rSelTor1);
		Matrix rDate = (r1Eps.times(NPB)).times(r2);
		//rDate.print(11, 9);

		double zDate[] = rDate.getColumn(2);
		DoubleVector zd = new DoubleVector(zDate);
		DoubleVector omegaI = zd.crossProduct(new DoubleVector(new double[] {1, 0, 0}));
		omegaI = omegaI.times(1.0 / omegaI.norm2());
		DoubleVector omegaJ = zd.crossProduct(new DoubleVector(new double[] {0, 1, 0}));
		omegaJ = omegaJ.times(1.0 / omegaJ.norm2());

		DoubleVector omega = zd.crossProduct(new DoubleVector(new double[] {0, 0, 1}));
		omega = omega.times(1.0 / omega.norm2());
		double xDate[] = rDate.getColumn(0);
		DoubleVector xd = new DoubleVector(xDate);
		double cosPSI = omega.innerProduct(xd);
		double sinPSI = (zd.crossProduct(omega)).innerProduct(xd);

		double phiC = Math.atan2(omega.get(1), omega.get(0));
		double thetaC = Math.acos(zDate[2]);
		double psiC = Math.atan2(sinPSI, cosPSI);

		om = phiC;
		I = thetaC;
		lm = psiC + phiC - Math.PI;
		N = 0.0;

		coscos = Math.cos(eclMoon.getLatitude()) * Math.cos(eclMoon.getLongitude() - om - N);
		cossin = Math.cos(I) * Math.cos(eclMoon.getLatitude()) * Math.sin(eclMoon.getLongitude() - om - N) - Math.sin(I) * Math.sin(eclMoon.getLatitude());
		sinb = - Math.sin(I) * Math.cos(eclMoon.getLatitude()) * Math.sin(eclMoon.getLongitude() - om - N) - Math.cos(I) * Math.sin(eclMoon.getLatitude());

		l = Math.atan2(cossin, coscos) - lm + om;
		b = Math.asin(sinb);

		sinsin = -Math.sin(eps) * Math.sin(om + N);
		cossin = Math.sin(I) * Math.cos(eps) - Math.cos(I) * Math.sin(eps) * Math.cos(om + N);
		//delta = Math.atan2(sinsin, cossin);

		//cosi = Math.cos(I) * Math.cos(eps) + Math.sin(I) * Math.sin(eps) * Math.cos(om+N);
		sinsin = -Math.sin(I) * Math.sin(om + N);
		cossin = Math.cos(I) * Math.sin(eps) - Math.sin(I) * Math.cos(eps) * Math.cos(om + N);
		omp = Math.atan2(sinsin, cossin);
		i = Math.asin(sinsin / Math.sin(omp));

		cossin = -Math.sin(i) * Math.cos(omp - locEquatorial.getLongitude());
		coscos = Math.cos(locEquatorial.getLatitude()) * Math.cos(i) - Math.sin(locEquatorial.getLatitude()) * Math.sin(i) * Math.sin(omp-locEquatorial.getLongitude());
		Cp = Math.atan2(cossin, coscos); // axis PA

		return new double[] {l, b, Cp};
    }

	/**
	 * Returns the number in Ernest W. Brown's numbered series of lunar
	 * cycles for the specified JD. The base Julian day for Ernest W.
	 * Brown's numbered series of lunations is 1923 Jan 17 02:41 UT.
	 * This date has been widely quoted as "Jan 16 1923" and indeed it
	 * was (in EST) at Yale University where Prof. Brown worked.<P>
	 *
	 * A lunation starts with a new Moon.<P>
	 *
	 * The output of this method is the correct lunation number for the
	 * specified Julian day in TT, in case a new lunation number started
	 * just before the input date. Note Meeus also introduced another
	 * lunation number starting from the first new moon in year 2000
	 * (Jan 6, 18:14 UTC). Meeus lunation = Brown lunation - 953.
	 *
	 * @param jd Julian day in TDB.
	 * @return Lunation number.
	 */
	public static int getBrownLunationNumber(double jd) {
		return Star.getBrownLunationNumber(jd);
	}

	/**
	 * Checks if a Neomenia could be visible right after the next new Moon. A Neomenia is 
	 * defined here as a Moon with more than 9 hrs of age and less than 33 hrs of age, 
	 * visible only right after sunset.
	 * The Indian method described at http://astro.ukho.gov.uk/download/NAOTN69.pdf is used.
	 * @param time The time object used as reference to check for the next new Moon.
	 * @param obs The observer.
	 * @param eph The ephemeris properties to obtain the position of the Moon. This input object 
	 * is not changed.
	 * @return The event, or null in case no Neomenia will be visible by the observer in the 
	 * next new Moon. Otherwise event start time will be the estimated best time to observe, 
	 * and end time will be the moon set.
	 * @throws JPARSECException If an error occurs.
	 */
	public static SimpleEventElement Neomenia(TimeElement time, ObserverElement obs, EphemerisElement eph)
	throws JPARSECException {
		// Get next new Moon
		double jd = TimeScale.getJD(time, obs, eph, SCALE.TERRESTRIAL_TIME);
		SimpleEventElement s = MainEvents.MoonPhaseOrEclipse(jd, SimpleEventElement.EVENT.MOON_NEW, MainEvents.EVENT_TIME.NEXT);
		if (s == null) return s;

		// Define a custom set without considering disk size and refraction
		RiseSetTransit.TWILIGHT tw = RiseSetTransit.TWILIGHT.CUSTOM;
		tw.considerObjectAngularRadius = false;
		tw.horizonElevation = 0;

		// Get next sunset after new Moon
		EphemerisElement eph0 = eph.clone();
		eph0.targetBody = TARGET.SUN;
		eph0.isTopocentric = true;
		eph0.optimizeForSpeed();
		TimeElement time0 = new TimeElement(s.time, SCALE.TERRESTRIAL_TIME);
		EphemElement ephem = Ephem.getEphemeris(time0, obs, eph0, false);
		double jdLocal = TimeScale.getJD(time0, obs, eph0, SCALE.LOCAL_TIME);
		int niter = 0;
		while (niter < 3 && ephem.set == null) {
			ephem = RiseSetTransit.obtainNextRiseSetTransit(time0, obs, eph0, ephem, tw);
			if (ephem.set != null && ephem.set[0] < jdLocal) ephem.set = null;
			time0.add(0.5);
			niter ++;
		}
		
		if (ephem.set == null || ephem.set[0] < jdLocal) return null;

		// Following Joseph Ashbrook of S&T it is not possible to see a Moon 
		// younger than 9 hrs. And the Sun must be set or almost set.
		double elapsedHrs0 = (ephem.set[0] - jdLocal) * 24;
		if (elapsedHrs0 < 9 || elapsedHrs0 > 24+9) return null;
		
		// Now get next Moon set
		time0 = new TimeElement(ephem.set[0], SCALE.LOCAL_TIME);
		eph0.targetBody = TARGET.Moon;
		EphemElement ephemMoon = Ephem.getEphemeris(time0, obs, eph0, false);
		if (ephemMoon.elevation < 0) return null;
		ephemMoon = RiseSetTransit.obtainNextRiseSetTransit(time0, obs, eph0, ephemMoon, tw);
		if (ephemMoon.set[0] > ephem.set[0]) { // Moon set must be after sunset
			double moonset = ephemMoon.set[0];
			// Final check following the Indian method, see http://astro.ukho.gov.uk/download/NAOTN69.pdf
			double bestTime = (ephem.set[0] * 5 + ephemMoon.set[0] * 4) / 9.0;
			time0 = new TimeElement(bestTime, SCALE.LOCAL_TIME);
			eph0.targetBody = TARGET.SUN;
			ephem = Ephem.getEphemeris(time0, obs, eph0, false);
			eph0.targetBody = TARGET.Moon;
			ephemMoon = Ephem.getEphemeris(time0, obs, eph0, false);
			// Geocentric Az/El ? They mean geometric ?
			ephem.elevation = CoordinateSystem.equatorialToHorizontal(ephem.getEquatorialLocation(), time0, obs, eph0).getLatitude();
			ephemMoon.elevation = CoordinateSystem.equatorialToHorizontal(ephemMoon.getEquatorialLocation(), time0, obs, eph0).getLatitude();
			double difAlt = (ephemMoon.elevation - ephem.elevation) * Constant.RAD_TO_DEG;
			double difAz = Math.abs(ephemMoon.azimuth - ephem.azimuth);
			double w = ephemMoon.angularRadius * Constant.RAD_TO_DEG * 60.0 * (1.0 - Math.cos(difAz));
			double limit = 11.8371 - 6.3226 * w + 0.7319 * w * w - 0.1018 * w * w * w;
			if (difAlt < limit) return null;

			// Return event
			double elapsedHrs1 = (bestTime - jdLocal) * 24;
			boolean spa = Translate.getDefaultLanguage() == LANGUAGE.SPANISH;
			String details = "Neomenia ("+(spa ? "edad luna " : "Moon age ");
			details += Functions.formatValue(elapsedHrs0, 1)+" - "+Functions.formatValue(elapsedHrs1, 1)+" hr)";
			s = new SimpleEventElement(TimeScale.getJD(new TimeElement(bestTime, SCALE.LOCAL_TIME), obs, eph0, SCALE.TERRESTRIAL_TIME), EVENT.OTHER, details);
			s.endTime = TimeScale.getJD(new TimeElement(moonset, SCALE.LOCAL_TIME), obs, eph0, SCALE.TERRESTRIAL_TIME);
			s.body = TARGET.Moon.getName();

			return s;
		}

		return null;
	}
	
	/**
	 * Checks if a Neomenia could be visible right before the next new Moon. A Neomenia is 
	 * defined here as a Moon with an age more than 9 hrs and less than 33 hrs before the 
	 * new Moon, visible only right before sunrise.
	 * The Indian method described at http://astro.ukho.gov.uk/download/NAOTN69.pdf is used.
	 * @param time The time object used as reference to check for the next new Moon.
	 * @param obs The observer.
	 * @param eph The ephemeris properties to obtain the position of the Moon. This input object 
	 * is not changed.
	 * @return The event, or null in case no Neomenia will be visible by the observer in the 
	 * next new Moon. Otherwise event start time will be the estimated best time to observe, 
	 * and end time will be the moon rise.
	 * @throws JPARSECException If an error occurs.
	 */
	public static SimpleEventElement NeomeniaBeforeNewMoon(TimeElement time, ObserverElement obs, EphemerisElement eph)
	throws JPARSECException {
		// Get next new Moon
		double jd = TimeScale.getJD(time, obs, eph, SCALE.TERRESTRIAL_TIME);
		SimpleEventElement s = MainEvents.MoonPhaseOrEclipse(jd, SimpleEventElement.EVENT.MOON_NEW, MainEvents.EVENT_TIME.NEXT);
		if (s == null) return s;

		// Define a custom set without considering disk size and refraction
		RiseSetTransit.TWILIGHT tw = RiseSetTransit.TWILIGHT.CUSTOM;
		tw.considerObjectAngularRadius = false;
		tw.horizonElevation = 0;

		// Get previous sunrise before new Moon
		EphemerisElement eph0 = eph.clone();
		eph0.targetBody = TARGET.SUN;
		eph0.isTopocentric = true;
		eph0.optimizeForSpeed();
		TimeElement time0 = new TimeElement(s.time, SCALE.TERRESTRIAL_TIME);
		EphemElement ephem = Ephem.getEphemeris(time0, obs, eph0, false);
		double jdLocal = TimeScale.getJD(time0, obs, eph0, SCALE.LOCAL_TIME);
		int niter = 0;
		while (niter < 3 && ephem.set == null) {
			ephem = RiseSetTransit.obtainPreviousRiseSetTransit(time0, obs, eph0, ephem, tw);
			if (ephem.rise != null && (ephem.rise[0] <= 0 || ephem.rise[0] > jdLocal)) ephem.rise = null;
			time0.add(-0.5);
			niter ++;
		}
		
		if (ephem.rise == null || ephem.rise[0] <= 0 || ephem.rise[0] > jdLocal) return null;

		// Following Joseph Ashbrook of S&T it is not possible to see a Moon 
		// younger than 9 hrs. And the Sun must be set or almost set.
		double elapsedHrs0 = -(ephem.rise[0] - jdLocal) * 24;
		if (elapsedHrs0 < 9 || elapsedHrs0 > 24+9) return null;
		
		// Now get previous Moon rise
		time0 = new TimeElement(ephem.rise[0], SCALE.LOCAL_TIME);
		eph0.targetBody = TARGET.Moon;
		EphemElement ephemMoon = Ephem.getEphemeris(time0, obs, eph0, false);
		if (ephemMoon.elevation < 0) return null;
		ephemMoon = RiseSetTransit.obtainPreviousRiseSetTransit(time0, obs, eph0, ephemMoon, tw);
		if (ephemMoon.rise[0] > 0 && ephemMoon.rise[0] < ephem.rise[0]) { // Moon set must be after sunset
			double moonrise = ephemMoon.rise[0];
			// Final check following the Indian method, see http://astro.ukho.gov.uk/download/NAOTN69.pdf
			double bestTime = (ephem.rise[0] * 5 + ephemMoon.rise[0] * 4) / 9.0;
			time0 = new TimeElement(bestTime, SCALE.LOCAL_TIME);
			eph0.targetBody = TARGET.SUN;
			ephem = Ephem.getEphemeris(time0, obs, eph0, false);
			eph0.targetBody = TARGET.Moon;
			ephemMoon = Ephem.getEphemeris(time0, obs, eph0, false);
			// Geocentric Az/El ? They mean geometric ?
			ephem.elevation = CoordinateSystem.equatorialToHorizontal(ephem.getEquatorialLocation(), time0, obs, eph0).getLatitude();
			ephemMoon.elevation = CoordinateSystem.equatorialToHorizontal(ephemMoon.getEquatorialLocation(), time0, obs, eph0).getLatitude();
			double difAlt = (ephemMoon.elevation - ephem.elevation) * Constant.RAD_TO_DEG;
			double difAz = Math.abs(ephemMoon.azimuth - ephem.azimuth);
			double w = ephemMoon.angularRadius * Constant.RAD_TO_DEG * 60.0 * (1.0 - Math.cos(difAz));
			double limit = 11.8371 - 6.3226 * w + 0.7319 * w * w - 0.1018 * w * w * w;
			if (difAlt < limit) return null;

			// Return event
			double elapsedHrs1 = (bestTime - jdLocal) * 24;
			boolean spa = Translate.getDefaultLanguage() == LANGUAGE.SPANISH;
			String details = "Neomenia ("+(spa ? "edad luna " : "Moon age ");
			details += Functions.formatValue(-elapsedHrs0, 1)+" - "+Functions.formatValue(elapsedHrs1, 1)+" hr)";
			s = new SimpleEventElement(TimeScale.getJD(new TimeElement(bestTime, SCALE.LOCAL_TIME), obs, eph0, SCALE.TERRESTRIAL_TIME), EVENT.OTHER, details);
			s.endTime = TimeScale.getJD(new TimeElement(moonrise, SCALE.LOCAL_TIME), obs, eph0, SCALE.TERRESTRIAL_TIME);
			s.body = TARGET.Moon.getName();

			return s;
		}

		return null;
	}
}
