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

import java.math.BigDecimal;

import jparsec.ephem.EphemerisElement.COORDINATES_TYPE;
import jparsec.ephem.EphemerisElement.FRAME;
import jparsec.ephem.EphemerisElement.REDUCTION_METHOD;
import jparsec.ephem.planets.EphemElement;
import jparsec.ephem.stars.StarElement;
import jparsec.ephem.stars.StarEphem;
import jparsec.ephem.stars.StarEphemElement;
import jparsec.math.Constant;
import jparsec.math.matrix.Matrix;
import jparsec.observer.EarthOrientationParameters;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.SiderealTime;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.time.TimeScale;
import jparsec.util.DataBase;
import jparsec.util.JPARSECException;

/**
 * A set of methods to calculate matrices related to IAU2006 resolutions
 * and coordinates transformations. Please note that IAU 2006 resolutions
 * are supported in each individual ephemeris theory using equinox-based
 * calculations, the methods in this class allows to obtain CIO-based
 * coordinates of the objects. CIO-based calculations are currently not
 * used actively inside JPARSEC.
 * See Wallace and Capitaine, A&amp;A 459, 981 (2006).
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class IAU2006
{
	// private constructor so that this class cannot be instantiated.
	private IAU2006() {}

	private static final double S_PLUS_HALF_XY_SERIES[] = new double[] {
		1, 0, -2640.73, +0.39, 0, 0, 0, 0, 1, 0, 0, 0,
		2, 0, 0, +94., 0, 0, 0, 0, 0, 0, 0, 0,
		3, 0, -63.53, +0.02, 0, 0, 0, 0, 2, 0, 0, 0,
		4, 0, -11.75, -0.01, 0, 0, 2, -2, 3, 0, 0, 0,
		5, 0, -11.21, -0.01, 0, 0, 2, -2, 1, 0, 0, 0,
		6, 0, +4.57, +0.00, 0, 0, 2, -2, 2, 0, 0, 0,
		7, 0, -2.02, +0.00, 0, 0, 2, 0, 3, 0, 0, 0,
		8, 0, -1.98, +0.00, 0, 0, 2, 0, 1, 0, 0, 0,
		9, 0, +1.72, +0.00, 0, 0, 0, 0, 3, 0, 0, 0,
		10, 0, +1.41, +0.01, 0, 1, 0, 0, 1, 0, 0, 0,
		11, 0, +1.26, +0.01, 0, 1, 0, 0, -1, 0, 0, 0,
		12, 0, +0.63, +0.00, 1, 0, 0, 0, -1, 0, 0, 0,
		13, 0, +0.63, +0.00, 1, 0, 0, 0, 1, 0, 0, 0,
		14, 0, -0.46, +0.00, 0, 1, 2, -2, 3, 0, 0, 0,
		15, 0, -0.45, +0.00, 0, 1, 2, -2, 1, 0, 0, 0,
		16, 0, -0.36, +0.00, 0, 0, 4, -4, 4, 0, 0, 0,
		17, 0, +0.24, +0.12, 0, 0, 1, -1, 1, -8, 12, 0,
		18, 0, -0.32, +0.00, 0, 0, 2, 0, 0, 0, 0, 0,
		19, 0, -0.28, +0.00, 0, 0, 2, 0, 2, 0, 0, 0,
		20, 0, -0.27, +0.00, 1, 0, 2, 0, 3, 0, 0, 0,
		21, 0, -0.26, +0.00, 1, 0, 2, 0, 1, 0, 0, 0,
		22, 0, +0.21, +0.00, 0, 0, 2, -2, 0, 0, 0, 0,
		23, 0, -0.19, +0.00, 0, 1, -2, 2, -3, 0, 0, 0,
		24, 0, -0.18, +0.00, 0, 1, -2, 2, -1, 0, 0, 0,
		25, 0, +0.10, -0.05, 0, 0, 0, 0, 0, 8, -13, -1,
		26, 0, -0.15, +0.00, 0, 0, 0, 2, 0, 0, 0, 0,
		27, 0, +0.14, +0.00, 2, 0, -2, 0, -1, 0, 0, 0,
		28, 0, +0.14, +0.00, 0, 1, 2, -2, 2, 0, 0, 0,
		29, 0, -0.14, +0.00, 1, 0, 0, -2, 1, 0, 0, 0,
		30, 0, -0.14, +0.00, 1, 0, 0, -2, -1, 0, 0, 0,
		31, 0, -0.13, +0.00, 0, 0, 4, -2, 4, 0, 0, 0,
		32, 0, +0.11, +0.00, 0, 0, 2, -2, 4, 0, 0, 0,
		33, 0, -0.11, +0.00, 1, 0, -2, 0, -3, 0, 0, 0,
		34, 0, -0.11, +0.00, 1, 0, -2, 0, -1, 0, 0, 0,
		35, 1, 0, +3808.65, 0, 0, 0, 0, 0, 0, 0, 0,
		36, 1, -0.07, +3.57, 0, 0, 0, 0, 2, 0, 0, 0,
		37, 1, +1.73, -0.03, 0, 0, 0, 0, 1, 0, 0, 0,
		38, 1, +0.00, +0.48, 0, 0, 2, -2, 3, 0, 0, 0,
		39, 2, +743.52, -0.17, 0, 0, 0, 0, 1, 0, 0, 0,
		40, 2, 0, -122.68, 0, 0, 0, 0, 0, 0, 0, 0,
		41, 2, +56.91, +0.06, 0, 0, 2, -2, 2, 0, 0, 0,
		42, 2, +9.84, -0.01, 0, 0, 2, 0, 2, 0, 0, 0,
		43, 2, -8.85, +0.01, 0, 0, 0, 0, 2, 0, 0, 0,
		44, 2, -6.38, -0.05, 0, 1, 0, 0, 0, 0, 0, 0,
		45, 2, -3.07, +0.00, 1, 0, 0, 0, 0, 0, 0, 0,
		46, 2, +2.23, +0.00, 0, 1, 2, -2, 2, 0, 0, 0,
		47, 2, +1.67, +0.00, 0, 0, 2, 0, 1, 0, 0, 0,
		48, 2, +1.30, +0.00, 1, 0, 2, 0, 2, 0, 0, 0,
		49, 2, +0.93, +0.00, 0, 1, -2, 2, -2, 0, 0, 0,
		50, 2, +0.68, +0.00, 1, 0, 0, -2, 0, 0, 0, 0,
		51, 2, -0.55, +0.00, 0, 0, 2, -2, 1, 0, 0, 0,
		52, 2, +0.53, +0.00, 1, 0, -2, 0, -2, 0, 0, 0,
		53, 2, -0.27, +0.00, 0, 0, 0, 2, 0, 0, 0, 0,
		54, 2, -0.27, +0.00, 1, 0, 0, 0, 1, 0, 0, 0,
		55, 2, -0.26, +0.00, 1, 0, -2, -2, -2, 0, 0, 0,
		56, 2, -0.25, +0.00, 1, 0, 0, 0, -1, 0, 0, 0,
		57, 2, +0.22, +0.00, 1, 0, 2, 0, 1, 0, 0, 0,
		58, 2, -0.21, +0.00, 2, 0, 0, -2, 0, 0, 0, 0,
		59, 2, +0.20, +0.00, 2, 0, -2, 0, -1, 0, 0, 0,
		60, 2, +0.17, +0.00, 0, 0, 2, 2, 2, 0, 0, 0,
		61, 2, +0.13, +0.00, 2, 0, 2, 0, 2, 0, 0, 0,
		62, 2, -0.13, +0.00, 2, 0, 0, 0, 0, 0, 0, 0,
		63, 2, -0.12, +0.00, 1, 0, 2, -2, 2, 0, 0, 0,
		64, 2, -0.11, +0.00, 0, 0, 2, 0, 0, 0, 0, 0,
		65, 3, 0, -72574.11, 0, 0, 0, 0, 0, 0, 0, 0,
		66, 3, +0.30, -23.42, 0, 0, 0, 0, 1, 0, 0, 0,
		67, 3, -0.03, -1.46, 0, 0, 2, -2, 2, 0, 0, 0,
		68, 3, -0.01, -0.25, 0, 0, 2, 0, 2, 0, 0, 0,
		69, 3, +0.00, +0.23, 0, 0, 0, 0, 2, 0, 0, 0,
		70, 4, 0, +27.98, 0, 0, 0, 0, 0, 0, 0, 0,
		71, 4, -0.26, -0.01, 0, 0, 0, 0, 1, 0, 0, 0,
		72, 5, 0, +15.62, 0, 0, 0, 0, 0, 0, 0, 0
	};

	/**
	 * Returns the Nutation-Precesion-Bias matrix (NPB)
	 * for a given instant. See the 'reference method' by Capitaine et al. at
	 * http://syrte.obspm.fr/iau2006/aa06_459.IAU06prec.pdf. Agreement is in
	 * the microarcsecond level.
	 * @param time Time object.
	 * @param observer Observer object.
	 * @param eph Ephemeris object.
	 * @return The NPB matrix.
	 * @throws JPARSECException If an error occurs.
	 */
	public static Matrix getNPB(TimeElement time, ObserverElement observer, EphemerisElement eph)
	throws JPARSECException {
		BigDecimal JD_TT = TimeScale.getExactJD(time, observer, eph, SCALE.TERRESTRIAL_TIME);

		Object o = DataBase.getData("NPB", true);
		if (o != null) {
			Object oo[] = (Object[]) o;
			double jd = (Double) oo[0];
			if (jd == JD_TT.doubleValue()) {
				double matrix[][] = (double[][]) oo[1];
				return new Matrix(matrix);
			}
		}

		double T = Functions.toCenturies(JD_TT).doubleValue();

		double XI0 = -0.016617140689 * Constant.ARCSEC_TO_RAD; // More digits!!!
		double ETA0 = -0.0068192 * Constant.ARCSEC_TO_RAD;
		double DA0 = -0.01460 * Constant.ARCSEC_TO_RAD;
		double EPS0 = 84381.406;

		EphemerisElement.REDUCTION_METHOD type = eph.ephemMethod;
		double PSIA = 0, OMEGAA = 0, CHIA = 0;
		if (type.isIAU20xx()) {
			// IAU2006/9 or maybe IAU2000 or Vondrak
			double angles[] = Precession.getAngles(false, JD_TT.doubleValue(), eph);
			PSIA = angles[0];
			OMEGAA = angles[1];
			CHIA = angles[2];
			EPS0 = angles[3];
		} else { // IAU2006
			PSIA = ((((-0.0000000951 * T + 0.000132851) * T - 0.00114045) * T - 1.0790069) * T + 5038.481507) * T;
			OMEGAA = ((((+0.0000003337 * T - 0.000000467) * T - 0.00772503) * T + 0.0512623) * T - 0.025754) * T + EPS0;
			CHIA = ((((-0.0000000560 * T + 0.000170663) * T - 0.00121197) * T - 2.3814292) * T + 10.556403) * T;
			PSIA *= Constant.ARCSEC_TO_RAD;
			OMEGAA *= Constant.ARCSEC_TO_RAD;
			CHIA *= Constant.ARCSEC_TO_RAD;
			EPS0 *= Constant.ARCSEC_TO_RAD;
		}
		double EPSA = 84378.576696215 * Constant.ARCSEC_TO_RAD;

		double JD_UTC = TimeScale.getJD(time, observer, eph, SCALE.UNIVERSAL_TIME_UTC);

		//boolean eop = eph.correctForEOP, eopP = eph.correctForPolarMotion;
		//eph.correctForEOP = true;
		//eph.correctForPolarMotion = true;
		EarthOrientationParameters.obtainEOP(JD_UTC, eph);
		//eph.correctForEOP = eop;
		//eph.correctForPolarMotion = eopP;
		Nutation.clearPreviousCalculation();
		EphemerisElement eph0 = eph.clone();
		eph0.ephemMethod = REDUCTION_METHOD.IAU_2006;
		Nutation.calcNutation(T, eph0);

		Matrix npb = Matrix.getR1(-EPSA - Nutation.getNutationInObliquity());
		npb = npb.times(Matrix.getR3(-Nutation.getNutationInLongitude()));
		npb = npb.times(Matrix.getR1(EPSA));
		npb = npb.times(Matrix.getR3(CHIA));
		npb = npb.times(Matrix.getR1(-OMEGAA));
		npb = npb.times(Matrix.getR3(-PSIA));
		npb = npb.times(Matrix.getR1(EPS0-ETA0));
		npb = npb.times(Matrix.getR2(XI0));
		npb = npb.times(Matrix.getR3(DA0));

		DataBase.addData("NPB", new Object[] {JD_TT.doubleValue(), npb.getArray()}, true);
		return npb;
	}

	/**
	 * Returns to matrix to transform coordinates in the GCRS (geocentric)
	 * to CIRS. See the 'reference method' by Capitaine et al. at
	 * http://syrte.obspm.fr/iau2006/aa06_459.IAU06prec.pdf. Agreement is in
	 * the microarcsecond level.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @return The matrix.
	 * @throws JPARSECException If an error occurs.
	 */
	public static Matrix getGCRS_to_CIRS(TimeElement time, ObserverElement obs, EphemerisElement eph)
	throws JPARSECException {
		BigDecimal jd_TT = TimeScale.getExactJD(time, obs, eph, SCALE.TERRESTRIAL_TIME);

		Object o = DataBase.getData("GCRS-CIRS", true);
		if (o != null) {
			Object oo[] = (Object[]) o;
			double jd = (Double) oo[0];
			if (jd == jd_TT.doubleValue()) {
				double matrix[][] = (double[][]) oo[1];
				return new Matrix(matrix);
			}
		}

		double T = Functions.toCenturies(jd_TT).doubleValue();

		Matrix npb = getNPB(time, obs, eph);

/*		double gam_C[] = new double[] {-0.052928, 10.556378, 0.4932044, -0.00031238, -0.000002788,
				0.0000000206};
		double psi_C[] = new double[] {-0.041775, 5038.481484, 1.5584175, -0.00018522, -0.000026452,
				-0.0000000148};
		double epsA_C[] = new double[] {84381.406, -46.836769, -0.0001831, 0.00200340, -0.000000576,
				-0.0000000434};
		double phi_C[] = new double[] {84381.412819, -46.811016, 0.0511268, 0.00053289, -0.000026452,
				-0.0000000148};

		double psi_ = 0, gam_ = 0, phi_ = 0, epsA = 0;
		for (int i=0; i<psi_C.length; i++) {
			psi_ += psi_C[i] * Math.pow(T, i);
			gam_ += gam_C[i] * Math.pow(T, i);
			phi_ += phi_C[i] * Math.pow(T, i);
			epsA += epsA_C[i] * Math.pow(T, i);
		}
		psi_ *= Constant.ARCSEC_TO_RAD;
		gam_ *= Constant.ARCSEC_TO_RAD;
		phi_ *= Constant.ARCSEC_TO_RAD;
		epsA *= Constant.ARCSEC_TO_RAD;
*/

		double cip[] = npb.getRow(2);
		double x = cip[0];
		double y = cip[1];
		double z = cip[2]; // = Math.sqrt(1.0 - x * x - y * y);
		double a = 1.0 / (1.0 + z);

		double s = getSPlusHalfXY(T) - x * y * 0.5;
		double sins = Math.sin(s), coss = Math.cos(s);
		double[][] cio = new double[][] {
				new double[] {
						coss + a * x * (y * sins - x * coss), -sins + a * y * (y * sins - x * coss), -(x * coss - y * sins)},
				new double[] {
						sins - a * x * (y * coss + x * sins), coss - a * y * (y * coss + x * sins), -(y * coss + x * sins)},
				new double[] {x, y, z}
		};
		Matrix NPB_CIO = new Matrix(cio); // NPB_CIO is the GCRS to CIRS matrix

		DataBase.addData("GCRS-CIRS", new Object[] {jd_TT.doubleValue(), NPB_CIO.getArray()}, true);
		return NPB_CIO;
	}

	/**
	 * Returns to matrix to transform coordinates in the GCRS (geocentric)
	 * to TIRS. See the 'reference method' by Capitaine et al. at
	 * http://syrte.obspm.fr/iau2006/aa06_459.IAU06prec.pdf. Agreement is in
	 * the microarcsecond level.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @return The matrix.
	 * @throws JPARSECException If an error occurs.
	 */
	public static Matrix getGCRS_to_TIRS(TimeElement time, ObserverElement obs, EphemerisElement eph)
	throws JPARSECException {
		Matrix NPB_CIO = IAU2006.getGCRS_to_CIRS(time, obs, eph);

		BigDecimal UT1 = TimeScale.getExactJD(time, obs, eph, SCALE.UNIVERSAL_TIME_UT1).subtract(new BigDecimal(Constant.J2000));

		BigDecimal ERA = Constant.BIG_TWO_PI.multiply(new BigDecimal("0.7790572732640").add(new BigDecimal("1.00273781191135448").multiply(UT1)));
		ERA = Functions.normalizeRadians(ERA);

		Matrix R3_ERA = Matrix.getR3(ERA.doubleValue());
		Matrix R = R3_ERA.times(NPB_CIO); // GCRS to TIRS
		return R;
	}

	/**
	 * Returns to matrix to transform coordinates in the GCRS (geocentric)
	 * to ITRS. See the 'reference method' by Capitaine et al. at
	 * http://syrte.obspm.fr/iau2006/aa06_459.IAU06prec.pdf. Agreement is in
	 * the microarcsecond level. Note correction for diurnal aberration is not included.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @return The matrix.
	 * @throws JPARSECException If an error occurs.
	 */
	public static Matrix getGCRS_to_ITRS(TimeElement time, ObserverElement obs, EphemerisElement eph)
	throws JPARSECException {
		Matrix R = IAU2006.getGCRS_to_TIRS(time, obs, eph);
		Matrix mat = IAU2006.getPolarMotionCorrectionMatrix(time, obs, eph);
		return mat.times(R);
	}

	/**
	 * Corrects coordinates from GCRS to topocentric, applying the GCRS to ICRS transform
	 * plus parallax and diurnal aberration.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param input Input coordinates as right ascension, declination, and distance in GCRS.
	 * Distance in AU.
	 * @return Output coordinates as hour angle and declination.
	 * @throws JPARSECException If an error occurs.
	 */
	public static LocationElement GCRS_to_topocentric(TimeElement time, ObserverElement obs, EphemerisElement eph,
			LocationElement input) throws JPARSECException {
		double p[] = input.getRectangularCoordinates();
		Matrix mat = IAU2006.getGCRS_to_ITRS(time, obs, eph); // ERA, polar motion, but still not longitude, aberration neither parallax
		double true_eq[] = mat.times(new Matrix(p)).getColumn(0);
		LocationElement loc = LocationElement.parseRectangularCoordinates(true_eq);
		loc.setLongitude(obs.getLongitudeRad()-loc.getLongitude()); // hour angle

		double lst = SiderealTime.apparentSiderealTime(time, obs, eph);
		loc.setLongitude(lst-loc.getLongitude()); // RA
		EphemElement ephem = new EphemElement();
		ephem.setEquatorialLocation(loc);
		EphemerisElement eph0 = eph.clone();
		eph0.ephemType = COORDINATES_TYPE.APPARENT;
		eph0.frame = FRAME.ICRF;
		ephem = Ephem.topocentricCorrection(time, obs, eph0, ephem);
		loc = ephem.getEquatorialLocation();
		loc.setLongitude(lst-loc.getLongitude()); // hour angle
		return loc;
	}

	/**
	 * Corrects coordinates from GCRS to apparent, applying the GCRS to ICRS transform
	 * plus parallax, diurnal aberration, and refraction.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param input Input coordinates as right ascension, declination, and distance in GCRS.
	 * Distance in AU.
	 * @return Output coordinates as hour angle and declination, corrected for refraction.
	 * @throws JPARSECException If an error occurs.
	 */
	public static LocationElement GCRS_to_apparent(TimeElement time, ObserverElement obs, EphemerisElement eph,
			LocationElement input) throws JPARSECException {
		LocationElement out = IAU2006.GCRS_to_topocentric(time, obs, eph, input);

		// Set RA in loc. Note this is correct despite RA is referred to CIO, since later I will only
		// need the correct hour angle, already calculated. I simply use the same LST later used in
		// the called function
		double lst = SiderealTime.apparentSiderealTime(time, obs, eph);
		out.setLongitude(lst-out.getLongitude());

		out = Ephem.correctEquatorialCoordinatesForRefraction(time, obs, eph, out);
		out.setLongitude(lst-out.getLongitude());
		return out;
	}

	/**
	 * The different possible ephemerides results referred to CIO.
	 */
	public enum CIO_EPHEMERIS {
		/** ID constant to return GCRS coordinates. */
		GCRS,
		/** ID constant to return CIRS coordinates. */
		CIRS,
		/** ID constant to return ITRS coordinates, corrected for
		 * Earth rotation angle and polar motion. */
		ITRS,
		/** ID constant to return topocentric coordinates, equal
		 * to ITRS coordinates corrected for parallax and diurnal aberration. */
		topocentric,
		/** ID constant to return apparent coordinates, equal
		 * to topocentric with refraction correction included. */
		apparent
	};

	/**
	 * Returns ephemerides using matrices. Input objects
	 * should be valid to call {@linkplain Ephem#getEphemeris(TimeElement, ObserverElement, EphemerisElement, boolean)}.
	 * The full ephem flag is set to false when calling it.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param output The type of coordinates to return in the right ascension
	 * and declination fields of the ephemerides object.
	 * @return The ephemerides. In case of topocentric or apparent output the
	 * right ascension is set using as local sidereal time the output from
	 * {@linkplain SiderealTime#apparentSiderealTime(TimeElement, ObserverElement, EphemerisElement)},
	 * otherwise RA and DEC fields are set to the output from the corresponding
	 * transformation matrix from GCRS.
	 * @throws JPARSECException In case of any error.
	 */
	public static EphemElement getEphemerisWithRespectCIO(TimeElement time, ObserverElement obs, EphemerisElement eph,
			CIO_EPHEMERIS output) throws JPARSECException {
		EphemElement ephem = Ephem.getEphemeris(time, obs, eph, false);

		Object o = DataBase.getData("GCRS", true);
		if (o == null) throw new JPARSECException("Cannot retrieve GCRS coordinates!");
		DataBase.addData("GCRS", null, true);

		double gcrs[] = (double[]) o;
		LocationElement loc = LocationElement.parseRectangularCoordinates(gcrs);
		Matrix mat = null;
		switch (output) {
		case CIRS:
			mat = IAU2006.getGCRS_to_CIRS(time, obs, eph);
			double out[] = mat.times(new Matrix(gcrs)).getColumn(0);
			loc = LocationElement.parseRectangularCoordinates(out);
			break;
		case ITRS:
			mat = IAU2006.getGCRS_to_ITRS(time, obs, eph);
			out = mat.times(new Matrix(gcrs)).getColumn(0);
			loc = LocationElement.parseRectangularCoordinates(out);
			break;
		case topocentric:
			loc = IAU2006.GCRS_to_topocentric(time, obs, eph, loc);
			break;
		case apparent:
			loc = IAU2006.GCRS_to_apparent(time, obs, eph, loc);
			break;
		case GCRS:
			break;
		}

		ephem.rightAscension = loc.getLongitude();
		ephem.declination = loc.getLatitude();
		if (output == CIO_EPHEMERIS.topocentric || output == CIO_EPHEMERIS.apparent) {
			double lst = SiderealTime.apparentSiderealTime(time, obs, eph);
			ephem.rightAscension = Functions.normalizeRadians(lst - loc.getLongitude());
		}
		return ephem;
	}

	/**
	 * Returns ephemerides for a star using matrices. Input objects
	 * should be valid to call {@linkplain StarEphem#starEphemeris(TimeElement, ObserverElement, EphemerisElement, StarElement, boolean)}.
	 * The full ephem flag is set to false when calling it.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param star The star object. In case frame is not ICRF, it is transformed to ICRF and J2000 equinox and epoch.
	 * @param output The type of coordinates to return in the right ascension
	 * and declination fields of the ephemerides object.
	 * @return The ephemerides. In case of topocentric or apparent output the
	 * right ascension is set using as local sidereal time the output from
	 * {@linkplain SiderealTime#apparentSiderealTime(TimeElement, ObserverElement, EphemerisElement)},
	 * otherwise RA and DEC fields are set to the output from the corresponding
	 * transformation matrix from GCRS.
	 * @throws JPARSECException In case of any error.
	 */
	public static StarEphemElement getStarEphemerisWithRespectCIO(TimeElement time, ObserverElement obs, EphemerisElement eph,
			StarElement star, CIO_EPHEMERIS output) throws JPARSECException {
		StarElement s = star.clone();
		if (star.frame != FRAME.ICRF) s = StarEphem.transformStarElementsToOutputEquinoxAndFrame(s, FRAME.ICRF, Constant.J2000, Constant.J2000);
		StarEphemElement ephem = StarEphem.starEphemeris(time, obs, eph, s, false);

		Object o = DataBase.getData("GCRS", true);
		if (o == null) throw new JPARSECException("Cannot retrieve GCRS coordinates! Maybe the eph object is not set to apparent coordinates?");
		DataBase.addData("GCRS", null, true);

		double gcrs[] = (double[]) o;
		LocationElement loc = LocationElement.parseRectangularCoordinates(gcrs);
		Matrix mat = null;
		switch (output) {
		case CIRS:
			mat = IAU2006.getGCRS_to_CIRS(time, obs, eph);
			double out[] = mat.times(new Matrix(gcrs)).getColumn(0);
			loc = LocationElement.parseRectangularCoordinates(out);
			break;
		case ITRS:
			mat = IAU2006.getGCRS_to_ITRS(time, obs, eph);
			out = mat.times(new Matrix(gcrs)).getColumn(0);
			loc = LocationElement.parseRectangularCoordinates(out);
			break;
		case topocentric:
			loc = IAU2006.GCRS_to_topocentric(time, obs, eph, loc);
			break;
		case apparent:
			loc = IAU2006.GCRS_to_apparent(time, obs, eph, loc);
			break;
		case GCRS:
			break;
		}

		ephem.rightAscension = loc.getLongitude();
		ephem.declination = loc.getLatitude();
		if (output == CIO_EPHEMERIS.topocentric || output == CIO_EPHEMERIS.apparent) {
			double lst = SiderealTime.apparentSiderealTime(time, obs, eph);
			ephem.rightAscension = Functions.normalizeRadians(lst - loc.getLongitude());
		}
		return ephem;
	}

	/**
	 * Evaluates coefficients for s + X Y / 2.
	 * @param T Centuries from J2000.
	 * @return S + x * y * 0.5.
	 */
	public static double getSPlusHalfXY(double T) {
		double sPlusHalfXY = 0.0;

		// * Mean anomaly of the Moon.
		double l = Functions.normalizeRadians(2.35555598 + 8328.6914269554 * T);

		// * Mean anomaly of the Sun.
		double lp = Functions.normalizeRadians(6.24006013 + 628.301955 * T);

		// * Mean argument of the latitude of the Moon.
		double f = Functions.normalizeRadians(1.627905234 + 8433.466158131 * T);

		// * Mean elongation of the Moon from the Sun.
		double d = Functions.normalizeRadians(5.198466741 + 7771.3771468121 * T);

		// * Mean longitude of the ascending node of the Moon.
		double om = Functions.normalizeRadians(2.18243920 - 33.757045 * T);

		// * General accumulated precession in longitude.
		double pa = (0.02438175 + 0.00000538691 * T) * T;

		// * Planetary longitudes, Venus and Earth (Souchay et al. 1999).
		double lv = Functions.normalizeRadians(3.176146697 + 1021.3285546211 * T);
		double le = Functions.normalizeRadians(1.753470314 + 628.3075849991 * T);

		for (int i = 0; i< 72; i++) {
			int index = i * 12;
			double j = S_PLUS_HALF_XY_SERIES[index + 1];
			double S_i = S_PLUS_HALF_XY_SERIES[index + 2];
			double C_i = S_PLUS_HALF_XY_SERIES[index + 3];

			double arg = S_PLUS_HALF_XY_SERIES[index + 4] * l + S_PLUS_HALF_XY_SERIES[index + 5] * lp +
				S_PLUS_HALF_XY_SERIES[index + 6] * f + S_PLUS_HALF_XY_SERIES[index + 7] * d +
				S_PLUS_HALF_XY_SERIES[index + 8] * om + S_PLUS_HALF_XY_SERIES[index + 9] * lv +
				S_PLUS_HALF_XY_SERIES[index + 10] * le + S_PLUS_HALF_XY_SERIES[index + 11] * pa;

			sPlusHalfXY += Math.pow(T, j) * (S_i * Math.sin(arg) + C_i * Math.cos(arg));
		}

		sPlusHalfXY *= 1.0E-6 * Constant.ARCSEC_TO_RAD;
		return sPlusHalfXY;
	}

	/**
	 * Returns the matrix that corrects positions for polar motion.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @return The matrix.
	 * @throws JPARSECException If an error occurs.
	 */
	public static Matrix getPolarMotionCorrectionMatrix(TimeElement time, ObserverElement obs, EphemerisElement eph) throws JPARSECException {
		double jd_UTC = TimeScale.getJD(time, obs, eph, SCALE.UNIVERSAL_TIME_UTC);
		boolean eopC = eph.correctForEOP, eopD = eph.correctEOPForDiurnalSubdiurnalTides;
		eph.correctForEOP = true;
		eph.correctEOPForDiurnalSubdiurnalTides = true;
		double eop[] = new double[] {0.0, 0.0, 0.0, 0.0, 0.0};
		if (eph.correctForEOP) eop = EarthOrientationParameters.obtainEOP(jd_UTC, eph);
		eph.correctForEOP = eopC;
		eph.correctEOPForDiurnalSubdiurnalTides = eopD;
		double x = eop[2] * Constant.ARCSEC_TO_RAD, y = eop[3] * Constant.ARCSEC_TO_RAD;
		// s' -> Approximate longitude of TIO using eq. 10 of Lambert & Bizouard, A&A 394, 317-321
		double sp = -47.0E-6 * Constant.ARCSEC_TO_RAD * Functions.toCenturies(TimeScale.getJD(time, obs, eph, SCALE.TERRESTRIAL_TIME));
		Matrix m = Matrix.getR3(-sp);
		m = m.times(Matrix.getR2(x));
		m = m.times(Matrix.getR1(y));
		return m.inverse();
	}
}
