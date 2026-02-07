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

import jparsec.ephem.EphemerisElement.FRAME;
import jparsec.math.Constant;
import jparsec.observer.EarthOrientationParameters;
import jparsec.observer.ObserverElement;
import jparsec.time.AstroDate;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.time.TimeScale;
import jparsec.util.DataBase;
import jparsec.util.JPARSECException;

/**
 * Nutation in longitude and obliquity.
 * <P>
 * This class performs nutation correction using IAU1980 theory of nutation or
 * IAU2000A theory (with adjustments for IAU 2006 precession when necessary).
 * The method can be selected by passing the adequate ID
 * constant to the nutation calculation method, as it is done by using any of
 * the calculation methods allowed by {@linkplain EphemerisElement}. Otherwise, each of the
 * methods can be directly called. See references in each method.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Nutation
{
	// private constructor so that this class cannot be instantiated.
	private Nutation() {}

	/**
	 * Array to hold sines of multiple angles
	 */
	private static double ss[][] = new double[5][8];

	/**
	 * Array to hold cosines of multiple angles
	 */
	private static double cc[][] = new double[5][8];

	/**
	 * Returns the nutation in longitude.
	 * @return Nutation in longitude, in radians.
	 */
	public static double getNutationInLongitude() {
		Object o = DataBase.getData("Nutation", true);
		if (o == null) return 0;
		double[] data = (double[]) o;
		return data[0];
	}
	/**
	 * Returns the nutation in obliquity.
	 * @return Nutation in obliquity, in radians.
	 */
	public static double getNutationInObliquity() {
		Object o = DataBase.getData("Nutation", true);
		if (o == null) return 0;
		double[] data = (double[]) o;
		return data[1];
	}
	private static double[] getLastCalc() {
		Object o = DataBase.getData("Nutation", true);
		if (o == null) return null;
		double[] data = (double[]) o;
		return data;

	}

	/**
	 * Returns the nutation in longitude and obliquity following Peter Duffett's
	 * book Astronomy with your Personal Computer.
	 * @param jd Julian day in dynamical time.
	 * @return Nutation in longitude and obliquity in radians.
	 */
	public static double[] getFastNutation(double jd) {
		double t, DJ, T2, aq, b, L1, L2, D1, D2, M1, M2, N1, N2, DP, DOSR;

		DJ = jd - 2415020.0;
		t = DJ / 36525.0;
		T2 = t * t;
		aq = 100.0021358 * t;
		b = 360.0 * (aq - Math.floor(aq));
		L1 = 279.6967 + .000303 * T2 + b;
		L2 = 2.0 * L1 * Constant.DEG_TO_RAD;
		aq = 1336.855231 * t;
		b = 360.0 * (aq - Math.floor(aq));
		D1 = 270.4342 - .001133 * T2 + b;
		D2 = 2.0 * D1 * Constant.DEG_TO_RAD;
		aq = 99.99736056 * t;
		b = 360.0 * (aq - Math.floor(aq));
		M1 = 358.4758 - .00015 * T2 + b;
		M1 = M1 * Constant.DEG_TO_RAD;
		aq = 1325.552359 * t;
		b = 360.0 * (aq - Math.floor(aq));
		M2 = 296.1046 + .009192 * T2 + b;
		M2 = M2 * Constant.DEG_TO_RAD;
		aq = 5.372616667 * t;
		b = 360.0 * (aq - Math.floor(aq));
		N1 = 259.1833 + .002078 * T2 - b;
		N1 = Constant.DEG_TO_RAD * N1;
		N2 = 2.0 * N1; // * Constant.DEG_TO_RAD // A bug in Peter Duffett's code !?

		DP = (-17.2327 - .01737 * t) * Math.sin(N1);
		DP = DP + (-1.2729 - .00013 * t) * Math.sin(L2) + .2088 * Math.sin(N2);
		DP = DP - .2037 * Math.sin(D2) + (.1261 - .00031 * t) * Math.sin(M1);
		DP = DP + .0675 * Math.sin(M2) - (.0497 - .00012 * t) * Math.sin(L2 + M1);
		DP = DP - .0342 * Math.sin(D2 - N1) - .0261 * Math.sin(D2 + M2);
		DP = DP + .0214 * Math.sin(L2 - M1) - .0149 * Math.sin(L2 - D2 + M2);
		DP = DP + .0124 * Math.sin(L2 - N1) + .0114 * Math.sin(D2 - M2);

		DOSR = (9.21 + .00091 * t) * Math.cos(N1);
		DOSR = DOSR + (.5522 - .00029 * t) * Math.cos(L2) - .0904 * Math.cos(N2);
		DOSR = DOSR + .0884 * Math.cos(D2) + .0216 * Math.cos(L2 + M1);
		DOSR = DOSR + .0183 * Math.cos(D2 - N1) + .0113 * Math.cos(D2 + M2);
		DOSR = DOSR - .0093 * Math.cos(L2 - M1) - .0066 * Math.cos(L2 - N1);

		DP = DP * Constant.ARCSEC_TO_RAD;
		DOSR = DOSR * Constant.ARCSEC_TO_RAD;

		return new double[] {DP, DOSR};
	}

	/**
	 * Calculate nutation in longitude and obliquity. Results are saved in
	 * {@linkplain DataBase}, using as identifier 'Nutation' for the array
	 * containing the values.
	 *
	 * @param T Julian centuries from J2000 epoch in dynamical time.
	 * @param eph Ephemeris properties including if EOP correction should be
	 * applied and the ephem method selection: IAU2006/2009, IAU2000, or any other
	 * (for IAU1980 nutation).
	 * @return The two values calculated for nutation in longitude and in obliquity.
	 * @throws JPARSECException If an error occurs accesing EOP files when required.
	 */
	public static synchronized double[] calcNutation(double T, EphemerisElement eph) throws JPARSECException
	{
		EphemerisElement.REDUCTION_METHOD type = eph.ephemMethod;
		double data[] = getLastCalc();
		if (data != null) {
			if (data[3] == type.ordinal() && Math.abs(T - data[2]) == 0) return new double[] {data[0], data[1]};
		}

		// This code is to ensure EOP is called before calculating nutation
		// Note in case EOP is not considered the nutation correction could be 
		// later reused (to save computing time) without EOP even in case EOP 
		// is enabled later
		if (eph.correctForEOP)
			try {
				double jd = Constant.J2000 + T * Constant.JULIAN_DAYS_PER_CENTURY;
				double UT12TT = TimeScale.getTTminusUT1(new TimeElement(jd, SCALE.BARYCENTRIC_DYNAMICAL_TIME), new ObserverElement()) / Constant.SECONDS_PER_DAY;
				EphemerisElement eph2 = new EphemerisElement();
				eph2.ephemMethod = type;
				eph2.frame = FRAME.ICRF;
				EarthOrientationParameters.obtainEOP(jd - UT12TT, eph2);
			} catch (Exception exc) {}
		

		double nutationInLongitude = 0, nutationInObliquity = 0;
		switch (type)
		{
		case IAU_1976:
		case LASKAR_1986:
		case SIMON_1994:
		case WILLIAMS_1994:
		case JPL_DE4xx:
			double n[] = calcNutation_IAU1980(T, eph);
			nutationInLongitude = n[0];
			nutationInObliquity = n[1];
			break;
		case IAU_2006:
		case IAU_2009:
		case IAU_2015:
			n = calcNutation_IAU2000(T, eph);
			nutationInLongitude = n[0];
			nutationInObliquity = n[1];

			// Apply precession adjustments, see Wallace & Capitaine, 2006, Eqs.5
			nutationInLongitude = nutationInLongitude + nutationInLongitude * (0.4697E-6 - 2.7774E-6 * T);
			nutationInObliquity = nutationInObliquity - nutationInObliquity * (2.7774E-6 * T);
			break;
		default: // IAU_2000
			n = calcNutation_IAU2000(T, eph);
			nutationInLongitude = n[0];
			nutationInObliquity = n[1];
			break;
		}

		double[] lastCalc = new double[] {nutationInLongitude, nutationInObliquity, T, type.ordinal()};

		DataBase.addData("Nutation", lastCalc, true);

		return new double[] {nutationInLongitude, nutationInObliquity};
	}

	/**
	 * Clears previous calculation so that the next call to
	 * nutation calculation will be processed calling the
	 * algorithms instead of using previous calculated values
	 * to save time (if results are expected to be the same).
	 * In case of enabling/disabling EOP frequently by hand you 
	 * may need to call this function.
	 */
	public static void clearPreviousCalculation() {
		double data[] = getLastCalc();
		if (data != null) DataBase.addData("Nutation", null, true);
	}

	/**
	 * Nutates equatorial coordinates from mean dynamical equator and equinox of date to true
	 * equator and equinox, or the opposite. See AA Explanatory Supplement, page 114-115.
	 * For dates between 1900 and 2100 and the flag to prefer precision in the ephemeris
	 * object set to false, the approximate code by Peter Duffet for nutation is used.
	 * @param jd_tt Julian day in TT.
	 * @param eph Ephemeris properties.
	 * @param in Input equatorial coordinates.
	 * @param meanToTrue True to nutate from mean to true position, false for true to mean.
	 * @return Output equatorial coordinates.
	 * @throws JPARSECException In case t is outside range a warning is thrown, and in case
	 * warnings should be treated as errors an exception will be thrown.
	 */
	public static double[] nutateInEquatorialCoordinates(double jd_tt, EphemerisElement eph,
			double[] in, boolean meanToTrue) throws JPARSECException {
		double t = Functions.toCenturies(jd_tt);
		double nut[] = (eph.preferPrecisionInEphemerides || t > 1) ? 
				Nutation.calcNutation(t, eph) : getFastNutation(jd_tt);
		double oblm = Obliquity.meanObliquity(t, eph);
		double oblt = oblm + nut[1];
		double dpsi = nut[0];

		double cobm = Math.cos(oblm), sobm = Math.sin(oblm);
		double cobt = Math.cos(oblt), sobt = Math.sin(oblt);
		double cpsi = Math.cos(dpsi), spsi = Math.sin(dpsi);

		// Compute elements of nutation matrix
		double xx = cpsi;
		double yx = -spsi * cobm;
		double zx = -spsi * sobm;
		double xy = spsi * cobt;
		double yy = cpsi * cobm * cobt + sobm * sobt;
		double zy = cpsi * sobm * cobt - cobm * sobt;
		double xz = spsi * sobt;
		double yz = cpsi * cobm * sobt - sobm * cobt;
		double zz = cpsi * sobm * sobt + cobm * cobt;

		double out[] = new double[in.length];
		if (meanToTrue) {
			out[0] = xx * in[0] + yx * in[1] + zx * in[2];
			out[1] = xy * in[0] + yy * in[1] + zy * in[2];
			out[2] = xz * in[0] + yz * in[1] + zz * in[2];
			if (out.length == 6) {
				out[3] = xx * in[3] + yx * in[4] + zx * in[5];
				out[4] = xy * in[3] + yy * in[4] + zy * in[5];
				out[5] = xz * in[3] + yz * in[4] + zz * in[5];
			}
		} else {
			out[0] = xx * in[0] + xy * in[1] + xz * in[2];
			out[1] = yx * in[0] + yy * in[1] + yz * in[2];
			out[2] = zx * in[0] + zy * in[1] + zz * in[2];
			if (out.length == 6) {
				out[3] = xx * in[3] + yx * in[4] + zx * in[5];
				out[4] = xy * in[3] + yy * in[4] + zy * in[5];
				out[5] = xz * in[3] + yz * in[4] + zz * in[5];
			}
		}

		return out;
	}

	/**
	 * IAU1980 Theory of Nutation, with optional support for free core nutation.
	 * Results are set in fields {@linkplain Nutation#nutationInLongitude} and
	 * {@linkplain Nutation#nutationInObliquity}.
	 * <P>
	 * Each term in the expansion has a trigonometric argument given by
	 * <P>
	 * W = i*MM + j*MS + k*FF + l*DD + m*OM
	 * <P>
	 * where the variables are defined below.
	 * <P>
	 * The nutation in longitude is a sum of terms of the form (a + bT) *
	 * sin(W). The terms for nutation in obliquity are of the form (c + dT) *
	 * cos(W). The coefficients are arranged in the tabulation as follows:
	 * <P>
	 * Coefficient:
	 * <P>
	 *
	 * <pre>
	 * i  j  k  l  m      a      b      c     d
	 * <BR>
	 * 0, 0, 0, 0, 1, -171996, -1742, 92025, 89
	 * </pre>
	 *
	 * <P>
	 * The first line of the table, above, is done separately since two of the
	 * values do not fit into 16 bit integers. The values a and c are arc
	 * seconds times 10000. b and d are arc seconds per Julian century times
	 * 100000. i through m are integers. See the program for interpretation of
	 * MM, MS, etc., which are mean orbital elements of the Sun and Moon.
	 * <P>
	 * If terms with coefficient less than X are omitted, the peak errors will
	 * be:
	 * <P>
	 *
	 * <pre>
	 *   omit a &lt;    error in longitude,     omit c &lt;    error in obliquity
	 * <BR>
	 *   .0005&quot;      .0100&quot;                  .0008&quot;      .0094&quot;
	 * <BR>
	 *   .0046       .0492                   .0095       .0481
	 * <BR>
	 *   .0123       .0880                   .0224       .0905
	 * <BR>
	 *   .0386       .1808                   .0895       .1129
	 * <P>
	 *</pre>
	 *
	 * References:
	 * <P>
	 * "Summary of 1980 IAU Theory of Nutation (Final Report of the IAU Working
	 * Group on Nutation)", P. K. Seidelmann et al., in Transactions of the IAU
	 * Vol. XVIII A, Reports on Astronomy, P. A. Wayman, ed.; D. Reidel Pub.
	 * Co., 1982.
	 * <P>
	 * "Nutation and the Earth's Rotation", I.A.U. Symposium No. 78, May, 1977,
	 * page 256. I.A.U., 1980.
	 * <P>
	 * Woolard, E.W., "A redevelopment of the theory of nutation", The
	 * Astronomical Journal, 58, 1-3 (1953).
	 * <P>
	 * This program implements all of the 1980 IAU nutation series. Results
	 * checked at 100 points against the 1986 Astronomical Almanac, all agreed.
	 * <P>
	 * Translated to Java from code by S. L. Moshier, November 1987
	 * <P>
	 * October, 1992 - typo fixed in nutation matrix
	 * <P>
	 * October, 1995 - fixed typo in node argument, tested against JPL DE403
	 * ephemeris file.
	 *
	 * @param T Time in Julian centuries from J2000.
	 * @param eph Ephemeris properties.
	 * @throws JPARSECException If an error occurs.
	 */
	private static double[] calcNutation_IAU1980(double T, EphemerisElement eph) throws JPARSECException
	{
		double f, g, T2, T10;
		double MM, MS, FF, DD, OM;
		double cu, su, cv, sv, sw;
		double C, D;
		int i, j, k = 0, k1, m;
		int p;

		T2 = T * T;
		T10 = T / 10.0;

		/* Fundamental arguments in the FK5 reference system. */

		/*
		 * longitude of the mean ascending node of the lunar orbit on the
		 * ecliptic, measured from the mean equinox of date
		 */
		OM = (Functions.mod3600(-6962890.539 * T + 450160.280) + (0.008 * T + 7.455) * T2) * Constant.ARCSEC_TO_RAD;

		/*
		 * mean longitude of the Sun minus the mean longitude of the Sun's
		 * perigee
		 */
		MS = (Functions.mod3600(129596581.224 * T + 1287099.804) - (0.012 * T + 0.577) * T2) * Constant.ARCSEC_TO_RAD;

		/*
		 * mean longitude of the Moon minus the mean longitude of the Moon's
		 * perigee
		 */
		MM = (Functions.mod3600(1717915922.633 * T + 485866.733) + (0.064 * T + 31.310) * T2) * Constant.ARCSEC_TO_RAD;

		/*
		 * mean longitude of the Moon minus the mean longitude of the Moon's
		 * node
		 */
		FF = (Functions.mod3600(1739527263.137 * T + 335778.877) + (0.011 * T - 13.257) * T2) * Constant.ARCSEC_TO_RAD;

		/*
		 * mean elongation of the Moon from the Sun.
		 */
		DD = (Functions.mod3600(1602961601.328 * T + 1072261.307) + (0.019 * T - 6.891) * T2) * Constant.ARCSEC_TO_RAD;

		/*
		 * Calculate sin( i*MM ), etc. for needed multiple angles
		 */
		sscc(0, MM, 3);
		sscc(1, MS, 2);
		sscc(2, FF, 4);
		sscc(3, DD, 4);
		sscc(4, OM, 2);

		C = 0.0;
		D = 0.0;
		p = -1; /* point to start of table */

		for (i = 0; i < 105; i++)
		{
			/* argument of sine and cosine */
			k1 = 0;
			cv = 0.0;
			sv = 0.0;
			for (m = 0; m < 5; m++)
			{
				p++;
				j = IAU1980_NT.nt[p];
				if (j != 0)
				{
					k = j;
					if (j < 0)
						k = -k;
					su = ss[m][k - 1]; /* sin(k*angle) */
					if (j < 0)
						su = -su;
					cu = cc[m][k - 1];
					if (k1 == 0)
					{ /* set first angle */
						sv = su;
						cv = cu;
						k1 = 1;
					} else
					{ /* combine angles */
						sw = su * cv + cu * sv;
						cv = cu * cv - su * sv;
						sv = sw;
					}
				}
			}
			/* longitude coefficient */
			p++;
			f = IAU1980_NT.nt[p];
			p++;
			k = IAU1980_NT.nt[p];
			if ((k != 0))
				f += T10 * k;

			/* obliquity coefficient */
			p++;
			g = IAU1980_NT.nt[p];
			p++;
			k = IAU1980_NT.nt[p];
			if ((k != 0))
				g += T10 * k;

			/* accumulate the terms */
			C += f * sv;
			D += g * cv;
		}

		/* first terms, not in table: */
		C += (-1742. * T10 - 171996.) * ss[4][0]; /* sin(OM) */
		D += (89. * T10 + 92025.) * cc[4][0]; /* cos(OM) */

		/* Save answers, expressed in radians */
		double nutationInLongitude = 0.0001 * Constant.ARCSEC_TO_RAD * C;
		double nutationInObliquity = 0.0001 * Constant.ARCSEC_TO_RAD * D;

		// * Add free core nutation.
		if (eph.correctForEOP) {
			Object o = DataBase.getData("EOP", true);
			double eop[] = new double[] {0.0, 0.0, 0.0, 0.0, 0}; // dPsi, dEpsilon, UT1-UTC, lastJD, lastMethod
			if (o != null) {
				eop = (double[]) o;
			} else {
				double JD_UT = T * Constant.JULIAN_DAYS_PER_CENTURY + Constant.J2000;
				JD_UT -= TimeScale.getTTminusUT1(new AstroDate(JD_UT));
				eop = EarthOrientationParameters.obtainEOP(JD_UT, eph); // UT approx to UTC
			}
			nutationInLongitude += eop[0] * Constant.ARCSEC_TO_RAD;
			nutationInObliquity += eop[1] * Constant.ARCSEC_TO_RAD;
		}

		return new double[] {nutationInLongitude, nutationInObliquity};
	}

	/*
	 * Prepare lookup table of sin and cos ( i*Lj ) for required multiple angles
	 */
	private static void sscc(int k, double arg, int n)
	{
		double cu, su, cv, sv, s;
		int i;

		su = Math.sin(arg);
		cu = Math.cos(arg);
		ss[k][0] = su; /* sin(L) */
		cc[k][0] = cu; /* cos(L) */
		sv = 2.0 * su * cu;
		cv = cu * cu - su * su;
		ss[k][1] = sv; /* sin(2L) */
		cc[k][1] = cv;

		for (i = 2; i < n; i++)
		{
			s = su * cv + cu * sv;
			cv = cu * cv - su * sv;
			sv = s;
			ss[k][i] = sv; /* sin( i+1 L ) */
			cc[k][i] = cv;
		}
	}

	/**
	 * Nutation, IAU 2000A model (MHB2000 luni-solar and planetary nutation with
	 * free core nutation omitted) plus optional support for free core nutation.
	 * This method is based on SOFA (Standards of Astronomy) software library.
	 * Results are set in fields {@linkplain Nutation#nutationInLongitude} and
	 * {@linkplain Nutation#nutationInObliquity}.
	 * <P>
	 * The nutation components in longitude and obliquity are with respect to
	 * the equinox and ecliptic of date. The obliquity at J2000 is assumed to be
	 * the Lieske et al. (1977) value of 84381.448 arcsec.
	 * <P>
	 * Both the luni-solar and planetary nutations are included. The latter are
	 * due to direct planetary nutations and the perturbations of the lunar and
	 * terrestrial orbits.
	 * <P>
	 * The routine computes the MHB2000 nutation series with the associated
	 * corrections for planetary nutations. It is an implementation of the
	 * nutation part of the IAU 2000A precession- nutation model, formally
	 * adopted by the IAU General Assembly in 2000, namely MHB2000 (Mathews et
	 * al. 2002), but with the free core nutation (FCN) omitted.
	 * <P>
	 * The full MHB2000 model also contains contributions to the nutations in
	 * longitude and obliquity due to the free-excitation of the
	 * free-core-nutation during the period 1979-2000. These FCN terms, which
	 * are time-dependent and unpredictable, are included in the present routine
	 * if {@linkplain EarthOrientationParameters#obtainEOP(double, jparsec.ephem.EphemerisElement.REDUCTION_METHOD)} is previously called
	 * (note this should always happen since this method is needed by
	 * {@linkplain TimeScale#getJD(jparsec.time.TimeElement, jparsec.observer.ObserverElement, EphemerisElement, jparsec.time.TimeElement.SCALE)}).
	 * With the FCN corrections included, the present
	 * routine delivers a pole which is at current epochs accurate to a few
	 * hundred microarcseconds. The omission of FCN introduces further errors of
	 * about that size.
	 * <P>
	 * The present routine provides classical nutation. The MHB2000 algorithm,
	 * from which it is adapted, deals also with (i) the offsets between the
	 * GCRS and mean poles and (ii) the adjustments in longitude and obliquity
	 * due to the changed precession rates. These additional functions, namely
	 * frame bias and precession adjustments, are applied independently in this
	 * package. Bias correction is made automatically in this library when
	 * selecting ICRS frame. The precession adjustments are applied in the
	 * precession methods from Capitaine and IAU2000 models.
	 * <P>
	 * References:
	 * <P>
	 * Mathews, P.M., Herring, T.A., Buffet, B.A., "Modeling of nutation and
	 * precession New nutation series for nonrigid Earth and insights into the
	 * Earth's interior", J.Geophys.Res., 107, B4, 2002. The MHB2000 code itself
	 * was obtained on 9th September 2002 from <A target="_blank" href = "
	 * ftp://maia.usno.navy.mil/conv2000/chapter5/IAU2000A">
	 * ftp://maia.usno.navy.mil/conv2000/chapter5/IAU2000A</A>.
	 * <P>
	 * Souchay, J., Loysel, B., Kinoshita, H., Folgueira, M., A&A Supp. Ser.
	 * 135, 111 (1999).
	 * <P>
	 * Wallace, P.T., "Software for Implementing the IAU 2000 Resolutions", in
	 * IERS Workshop 5.1 (2002).
	 * <P>
	 * Chapront, J., Chapront-Touze, M. & Francou, G., Astron.Astrophys., 387,
	 * 700 (2002).
	 * <P>
	 * Lieske, J.H., Lederle, T., Fricke, W. & Morando, B., "Expressions for the
	 * precession quantities based upon the IAU (1976) System of Astronomical
	 * Constants", Astron.Astrophys., 58, 1-16 (1977).
	 * <P>
	 * Simon, J.-L., Bretagnon, P., Chapront, J., Chapront-Touze, M., Francou,
	 * G., Laskar, J., A&A282, 663-683 (1994).
	 * <P>
	 * This revision: 2005 August 24.
	 * <P>
	 * Copyright (C) 2005 IAU SOFA Review Board.
	 *
	 * @param T Time in Julian centuries from J2000.
	 * @param eph Ephemeris properties.
	 * @throws JPARSECException If an error occurs.
	 */
	private static double[] calcNutation_IAU2000(double T, EphemerisElement eph) throws JPARSECException
	{
		// * Initialize the nutation values.
		double DP = 0.0, DE = 0.0;
		double nutationInLongitude = 0.0;
		double nutationInObliquity = 0.0;

		/*
		 * ------------------- LUNI-SOLAR NUTATION -------------------
		 */

		/*
		 * Fundamental (Delaunay) arguments from Simon et al. (1994)
		 */

		// * Mean anomaly of the Moon.
		double EL = (485868.249036 + T * (1717915923.2178 + T * (31.8792 + T * (0.051635 + T * (-0.00024470))))) * Constant.ARCSEC_TO_RAD;

		// * Mean anomaly of the Sun.
		double ELP = (1287104.79305 + T * (129596581.0481 + T * (-0.5532 + T * (0.000136 + T * (-0.00001149))))) * Constant.ARCSEC_TO_RAD;

		// * Mean argument of the latitude of the Moon.
		double F = (335779.526232 + T * (1739527262.8478 + T * (-12.7512 + T * (-0.001037 + T * (0.00000417))))) * Constant.ARCSEC_TO_RAD;

		// * Mean elongation of the Moon from the Sun.
		double D = (1072260.70369 + T * (1602961601.2090 + T * (-6.3706 + T * (0.006593 + T * (-0.00003169))))) * Constant.ARCSEC_TO_RAD;

		// * Mean longitude of the ascending node of the Moon.
		double OM = (450160.398036 + T * (-6962890.5431 + T * (7.4722 + T * (0.007702 + T * (-0.00005939))))) * Constant.ARCSEC_TO_RAD;

		// * Summation of luni-solar nutation series (in reverse order).
		for (int I = 677; I >= 0; I--)
		{
			int NALS_index = I * 5 - 1;

			// * Argument and functions.
			double ARG = (IAU2000_NALS.NALS[NALS_index + 1] * EL + IAU2000_NALS.NALS[NALS_index + 2] * ELP + IAU2000_NALS.NALS[NALS_index + 3] * F + IAU2000_NALS.NALS[NALS_index + 4] * D + IAU2000_NALS.NALS[NALS_index + 5] * OM);
			double SARG = Math.sin(ARG);
			double CARG = Math.cos(ARG);

			int CLS_index = I * 6 - 1;

			// * Term.
			DP = DP + (IAU2000_CLS.CLS[CLS_index + 1] + IAU2000_CLS.CLS[CLS_index + 2] * T) * SARG + IAU2000_CLS.CLS[CLS_index + 3] * CARG;
			DE = DE + (IAU2000_CLS.CLS[CLS_index + 4] + IAU2000_CLS.CLS[CLS_index + 5] * T) * CARG + IAU2000_CLS.CLS[CLS_index + 6] * SARG;

		}

		// * Convert from 0.1 microarcsec units to radians.
		nutationInLongitude = DP * Constant.ARCSEC_TO_RAD / 1.0e7;
		nutationInObliquity = DE * Constant.ARCSEC_TO_RAD / 1.0e7;

		/*
		 * ------------------ PLANETARY NUTATION ------------------
		 */

		/*
		 * n.b. The MHB2000 code computes the luni-solar and planetary nutation
		 * in different routines, using slightly different Delaunay arguments in
		 * the two cases. This behaviour is faithfully reproduced here. Use of
		 * the Simon et al. expressions for both cases leads to negligible
		 * changes, well below 0.1 microarcsecond.
		 */

		// * Mean anomaly of the Moon.
		double AL = 2.35555598 + 8328.6914269554 * T;

		// * Mean anomaly of the Sun.
		double ALSU = 6.24006013 + 628.301955 * T;

		// * Mean argument of the latitude of the Moon.
		double AF = 1.627905234 + 8433.466158131 * T;

		// * Mean elongation of the Moon from the Sun.
		double AD = 5.198466741 + 7771.3771468121 * T;

		// * Mean longitude of the ascending node of the Moon.
		double AOM = 2.18243920 - 33.757045 * T;

		// * General accumulated precession in longitude.
		double APA = (0.02438175 + 0.00000538691 * T) * T;

		// * Planetary longitudes, Mercury through Neptune (Souchay et al.
		// 1999).
		double ALME = 4.402608842 + 2608.7903141574 * T;
		double ALVE = 3.176146697 + 1021.3285546211 * T;
		double ALEA = 1.753470314 + 628.3075849991 * T;
		double ALMA = 6.203480913 + 334.0612426700 * T;
		double ALJU = 0.599546497 + 52.9690962641 * T;
		double ALSA = 0.874016757 + 21.3299104960 * T;
		double ALUR = 5.481293871 + 7.4781598567 * T;
		double ALNE = 5.321159000 + 3.8127774000 * T;

		// * Initialize the nutation values.
		DP = 0.0;
		DE = 0.0;

		// * Summation of planetary nutation series (in reverse order).
		for (int I = 686; I >= 0; I--)
		{

			int NAPL_index = I * 14 - 1;

			// * Argument and functions.
			double ARG = (IAU2000_NAPL.NAPL[NAPL_index + 1] * AL + IAU2000_NAPL.NAPL[NAPL_index + 2] * ALSU + IAU2000_NAPL.NAPL[NAPL_index + 3] * AF + IAU2000_NAPL.NAPL[NAPL_index + 4] * AD + IAU2000_NAPL.NAPL[NAPL_index + 5] * AOM + IAU2000_NAPL.NAPL[NAPL_index + 6] * ALME + IAU2000_NAPL.NAPL[NAPL_index + 7] * ALVE + IAU2000_NAPL.NAPL[NAPL_index + 8] * ALEA + IAU2000_NAPL.NAPL[NAPL_index + 9] * ALMA + IAU2000_NAPL.NAPL[NAPL_index + 10] * ALJU + IAU2000_NAPL.NAPL[NAPL_index + 11] * ALSA + IAU2000_NAPL.NAPL[NAPL_index + 12] * ALUR + IAU2000_NAPL.NAPL[NAPL_index + 13] * ALNE + IAU2000_NAPL.NAPL[NAPL_index + 14] * APA);
			double SARG = Math.sin(ARG);
			double CARG = Math.cos(ARG);

			int ICPL_index = I * 4 - 1;

			// * Term.
			DP = DP + IAU2000_ICPL.ICPL[ICPL_index + 1] * SARG + IAU2000_ICPL.ICPL[ICPL_index + 2] * CARG;
			DE = DE + IAU2000_ICPL.ICPL[ICPL_index + 3] * SARG + IAU2000_ICPL.ICPL[ICPL_index + 4] * CARG;
		}

		// * Add luni-solar and planetary components converting from 0.1
		// microarcsecond.
		nutationInLongitude += DP * Constant.ARCSEC_TO_RAD / 1.0e7;
		nutationInObliquity += DE * Constant.ARCSEC_TO_RAD / 1.0e7;

		// * Add free core nutation.
		if (eph.correctForEOP) {
			Object o = DataBase.getData("EOP", true);
			double eop[] = new double[] {0.0, 0.0, 0.0, 0.0, 0}; // dPsi, dEpsilon, UT1-UTC, lastJD, lastMethod
			if (o != null) {
				eop = (double[]) o;
			} else {
				double JD_UT = T * Constant.JULIAN_DAYS_PER_CENTURY + Constant.J2000;
				JD_UT -= TimeScale.getTTminusUT1(new AstroDate(JD_UT));
				eop = EarthOrientationParameters.obtainEOP(JD_UT, eph); // UT approx to UTC
			}
			nutationInLongitude += eop[0] * Constant.ARCSEC_TO_RAD;
			nutationInObliquity += eop[1] * Constant.ARCSEC_TO_RAD;
		}

		return new double[] {nutationInLongitude, nutationInObliquity};
	}
}

/**
 * IAU1980 model
 */
final class IAU1980_NT
{
	/**
	 * IAU 1980 Nuation series
	 */
	static int nt[] =
	{ 0, 0, 0, 0, 2, 2062, 2, -895, 5, -2, 0, 2, 0, 1, 46, 0, -24, 0, 2, 0, -2, 0, 0, 11, 0, 0, 0, -2, 0, 2, 0, 2, -3,
			0, 1, 0, 1, -1, 0, -1, 0, -3, 0, 0, 0, 0, -2, 2, -2, 1, -2, 0, 1, 0, 2, 0, -2, 0, 1, 1, 0, 0, 0, 0, 0, 2,
			-2, 2, -13187, -16, 5736, -31, 0, 1, 0, 0, 0, 1426, -34, 54, -1, 0, 1, 2, -2, 2, -517, 12, 224, -6, 0, -1,
			2, -2, 2, 217, -5, -95, 3, 0, 0, 2, -2, 1, 129, 1, -70, 0, 2, 0, 0, -2, 0, 48, 0, 1, 0, 0, 0, 2, -2, 0,
			-22, 0, 0, 0, 0, 2, 0, 0, 0, 17, -1, 0, 0, 0, 1, 0, 0, 1, -15, 0, 9, 0, 0, 2, 2, -2, 2, -16, 1, 7, 0, 0,
			-1, 0, 0, 1, -12, 0, 6, 0, -2, 0, 0, 2, 1, -6, 0, 3, 0, 0, -1, 2, -2, 1, -5, 0, 3, 0, 2, 0, 0, -2, 1, 4, 0,
			-2, 0, 0, 1, 2, -2, 1, 4, 0, -2, 0, 1, 0, 0, -1, 0, -4, 0, 0, 0, 2, 1, 0, -2, 0, 1, 0, 0, 0, 0, 0, -2, 2,
			1, 1, 0, 0, 0, 0, 1, -2, 2, 0, -1, 0, 0, 0, 0, 1, 0, 0, 2, 1, 0, 0, 0, -1, 0, 0, 1, 1, 1, 0, 0, 0, 0, 1, 2,
			-2, 0, -1, 0, 0, 0, 0, 0, 2, 0, 2, -2274, -2, 977, -5, 1, 0, 0, 0, 0, 712, 1, -7, 0, 0, 0, 2, 0, 1, -386,
			-4, 200, 0, 1, 0, 2, 0, 2, -301, 0, 129, -1, 1, 0, 0, -2, 0, -158, 0, -1, 0, -1, 0, 2, 0, 2, 123, 0, -53,
			0, 0, 0, 0, 2, 0, 63, 0, -2, 0, 1, 0, 0, 0, 1, 63, 1, -33, 0, -1, 0, 0, 0, 1, -58, -1, 32, 0, -1, 0, 2, 2,
			2, -59, 0, 26, 0, 1, 0, 2, 0, 1, -51, 0, 27, 0, 0, 0, 2, 2, 2, -38, 0, 16, 0, 2, 0, 0, 0, 0, 29, 0, -1, 0,
			1, 0, 2, -2, 2, 29, 0, -12, 0, 2, 0, 2, 0, 2, -31, 0, 13, 0, 0, 0, 2, 0, 0, 26, 0, -1, 0, -1, 0, 2, 0, 1,
			21, 0, -10, 0, -1, 0, 0, 2, 1, 16, 0, -8, 0, 1, 0, 0, -2, 1, -13, 0, 7, 0, -1, 0, 2, 2, 1, -10, 0, 5, 0, 1,
			1, 0, -2, 0, -7, 0, 0, 0, 0, 1, 2, 0, 2, 7, 0, -3, 0, 0, -1, 2, 0, 2, -7, 0, 3, 0, 1, 0, 2, 2, 2, -8, 0, 3,
			0, 1, 0, 0, 2, 0, 6, 0, 0, 0, 2, 0, 2, -2, 2, 6, 0, -3, 0, 0, 0, 0, 2, 1, -6, 0, 3, 0, 0, 0, 2, 2, 1, -7,
			0, 3, 0, 1, 0, 2, -2, 1, 6, 0, -3, 0, 0, 0, 0, -2, 1, -5, 0, 3, 0, 1, -1, 0, 0, 0, 5, 0, 0, 0, 2, 0, 2, 0,
			1, -5, 0, 3, 0, 0, 1, 0, -2, 0, -4, 0, 0, 0, 1, 0, -2, 0, 0, 4, 0, 0, 0, 0, 0, 0, 1, 0, -4, 0, 0, 0, 1, 1,
			0, 0, 0, -3, 0, 0, 0, 1, 0, 2, 0, 0, 3, 0, 0, 0, 1, -1, 2, 0, 2, -3, 0, 1, 0, -1, -1, 2, 2, 2, -3, 0, 1, 0,
			-2, 0, 0, 0, 1, -2, 0, 1, 0, 3, 0, 2, 0, 2, -3, 0, 1, 0, 0, -1, 2, 2, 2, -3, 0, 1, 0, 1, 1, 2, 0, 2, 2, 0,
			-1, 0, -1, 0, 2, -2, 1, -2, 0, 1, 0, 2, 0, 0, 0, 1, 2, 0, -1, 0, 1, 0, 0, 0, 2, -2, 0, 1, 0, 3, 0, 0, 0, 0,
			2, 0, 0, 0, 0, 0, 2, 1, 2, 2, 0, -1, 0, -1, 0, 0, 0, 2, 1, 0, -1, 0, 1, 0, 0, -4, 0, -1, 0, 0, 0, -2, 0, 2,
			2, 2, 1, 0, -1, 0, -1, 0, 2, 4, 2, -2, 0, 1, 0, 2, 0, 0, -4, 0, -1, 0, 0, 0, 1, 1, 2, -2, 2, 1, 0, -1, 0,
			1, 0, 2, 2, 1, -1, 0, 1, 0, -2, 0, 2, 4, 2, -1, 0, 1, 0, -1, 0, 4, 0, 2, 1, 0, 0, 0, 1, -1, 0, -2, 0, 1, 0,
			0, 0, 2, 0, 2, -2, 1, 1, 0, -1, 0, 2, 0, 2, 2, 2, -1, 0, 0, 0, 1, 0, 0, 2, 1, -1, 0, 0, 0, 0, 0, 4, -2, 2,
			1, 0, 0, 0, 3, 0, 2, -2, 2, 1, 0, 0, 0, 1, 0, 2, -2, 0, -1, 0, 0, 0, 0, 1, 2, 0, 1, 1, 0, 0, 0, -1, -1, 0,
			2, 1, 1, 0, 0, 0, 0, 0, -2, 0, 1, -1, 0, 0, 0, 0, 0, 2, -1, 2, -1, 0, 0, 0, 0, 1, 0, 2, 0, -1, 0, 0, 0, 1,
			0, -2, -2, 0, -1, 0, 0, 0, 0, -1, 2, 0, 1, -1, 0, 0, 0, 1, 1, 0, -2, 1, -1, 0, 0, 0, 1, 0, -2, 2, 0, -1, 0,
			0, 0, 2, 0, 0, 2, 0, 1, 0, 0, 0, 0, 0, 2, 4, 2, -1, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, };
}

/**
 * Nutation, IAU 2000A model (MHB2000 luni-solar and planetary nutation with
 * free core nutation omitted).
 */
final class IAU2000_NALS
{
	/*
	 * Luni-Solar argument multipliers L L' F D Om
	 */
	static double[] NALS =
	{
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J= 1, 10 )/
			0, 0, 0, 0, 1, 0, 0, 2, -2, 2, 0, 0, 2, 0, 2, 0, 0, 0, 0, 2, 0, 1, 0, 0, 0,
			0, 1, 2, -2, 2, 1, 0, 0, 0, 0, 0, 0, 2, 0, 1, 1, 0, 2, 0, 2, 0, -1, 2, -2, 2,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J= 11, 20 )/
			0, 0, 2, -2, 1, -1, 0, 2, 0, 2, -1, 0, 0, 2, 0, 1, 0, 0, 0, 1, -1, 0, 0, 0, 1,
			-1, 0, 2, 2, 2, 1, 0, 2, 0, 1, -2, 0, 2, 0, 1, 0, 0, 0, 2, 0, 0, 0, 2, 2, 2,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J= 21, 30 )/
			0, -2, 2, -2, 2, -2, 0, 0, 2, 0, 2, 0, 2, 0, 2, 1, 0, 2, -2, 2, -1, 0, 2, 0, 1,
			2, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 1, 0, 0, 1, -1, 0, 0, 2, 1, 0, 2, 2, -2, 2,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J= 31, 40 )/
			0, 0, -2, 2, 0, 1, 0, 0, -2, 1, 0, -1, 0, 0, 1, -1, 0, 2, 2, 1, 0, 2, 0, 0, 0,
			1, 0, 2, 2, 2, -2, 0, 2, 0, 0, 0, 1, 2, 0, 2, 0, 0, 2, 2, 1, 0, -1, 2, 0, 2,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J= 41, 50 )/
			0, 0, 0, 2, 1, 1, 0, 2, -2, 1, 2, 0, 2, -2, 2, -2, 0, 0, 2, 1, 2, 0, 2, 0, 1,
			0, -1, 2, -2, 1, 0, 0, 0, -2, 1, -1, -1, 0, 2, 0, 2, 0, 0, -2, 1, 1, 0, 0, 2, 0,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J= 51, 60 )/
			0, 1, 2, -2, 1, 1, -1, 0, 0, 0, -2, 0, 2, 0, 2, 3, 0, 2, 0, 2, 0, -1, 0, 2, 0,
			1,	-1, 2, 0, 2, 0, 0, 0, 1, 0, -1, -1, 2, 2, 2, -1, 0, 2, 0, 0, 0, -1, 2, 2, 2,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J= 61, 70 )/
			-2, 0, 0, 0, 1, 1, 1, 2, 0, 2, 2, 0, 0, 0, 1, -1, 1, 0, 1, 0, 1, 1, 0, 0, 0,
			1, 0, 2, 0, 0, -1, 0, 2, -2, 1, 1, 0, 0, 0, 2, -1, 0, 0, 1, 0, 0, 0, 2, 1, 2,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J= 71, 80 )/
			-1, 0, 2, 4, 2, -1, 1, 0, 1, 1, 0, -2, 2, -2, 1, 1, 0, 2, 2, 1, -2, 0, 2, 2, 2,
			-1, 0, 0, 0, 2, 1, 1, 2, -2, 2, -2, 0, 2, 4, 2, -1, 0, 4, 0, 2, 2, 0, 2, -2, 1,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J= 81, 90 )/
			2, 0, 2, 2, 2, 1, 0, 0, 2, 1, 3, 0, 0, 0, 0, 3, 0, 2, -2, 2, 0, 0, 4, -2, 2, 0,
			1, 2, 0, 1, 0, 0, -2, 2, 1, 0, 0, 2, -2, 3, -1, 0, 0, 4, 0, 2, 0, -2, 0, 1,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J= 91,100 )/
			 -2, 0, 0, 4, 0, -1, -1, 0, 2, 1, -1, 0, 0, 1, 1, 0, 1, 0, 0, 2, 0, 0, -2, 0, 1,
			 0, -1, 2, 0, 1, 0, 0, 2, -1, 2, 0, 0, 2, 4, 2, -2, -1, 0, 2, 0, 1, 1, 0, -2, 1,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=101,110 )/
			-1, 1, 0, 2, 0, -1, 1, 0, 1, 2, 1, -1, 0, 0, 1, 1, -1, 2, 2, 2, -1, 1, 2, 2, 2,
			3, 0, 2, 0, 1, 0, 1, -2, 2, 0, -1, 0, 0, -2, 1, 0, 1, 2, 2, 2, -1, -1, 2, 2, 1,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=111,120 )/
			0, -1, 0, 0, 2, 1, 0, 2, -4, 1, -1, 0, -2, 2, 0, 0, -1, 2, 2, 1, 2, -1, 2, 0, 2,
			0, 0, 0, 2, 2, 1, -1, 2, 0, 1, -1, 1, 2, 0, 2, 0, 1, 0, 2, 0, 0, -1, -2, 2, 0,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=121,130 )/
			0, 3, 2, -2, 2, 0, 0, 0, 1, 1, -1, 0, 2, 2, 0, 2, 1, 2, 0, 2, 1, 1, 0, 0, 1,
			1, 1, 2, 0, 1, 2, 0, 0, 2, 0, 1, 0, -2, 2, 0, -1, 0, 0, 2, 2, 0, 1, 0, 1, 0,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=131,140 )/
			0, 1, 0, -2, 1, -1, 0, 2, -2, 2, 0, 0, 0, -1, 1, -1, 1, 0, 0, 1, 1, 0, 2, -1, 2,
			1, -1, 0, 2, 0, 0, 0, 0, 4, 0, 1, 0, 2, 1, 2, 0, 0, 2, 1, 1, 1, 0, 0, -2, 2,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=141,150 )/
			-1, 0, 2, 4, 1, 1, 0, -2, 0, 1, 1, 1, 2, -2, 1, 0, 0, 2, 2, 0, -1, 0, 2, -1, 1,
			-2, 0, 2, 2, 1, 4, 0, 2, 0, 2, 2, -1, 0, 0, 0, 2, 1, 2, -2, 2, 0, 1, 2, 1, 2,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=151,160 )/
			1, 0, 4, -2, 2, -1, -1, 0, 0, 1, 0, 1, 0, 2, 1, -2, 0, 2, 4, 1, 2, 0, 2, 0, 0,
			1, 0, 0, 1, 0, -1, 0, 0, 4, 1, -1, 0, 4, 0, 1, 2, 0, 2, 2, 1, 0, 0, 2, -3, 2,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=161,170 )/
			-1, -2, 0, 2, 0, 2, 1, 0, 0, 0, 0, 0, 4, 0, 2, 0, 0, 0, 0, 3, 0, 3, 0, 0, 0,
			0, 0, 2, -4, 1, 0, -1, 0, 2, 1, 0, 0, 0, 4, 1, -1, -1, 2, 4, 2, 1, 0, 2, 4, 2,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=171,180 )/
			-2, 2, 0, 2, 0, -2, -1, 2, 0, 1, -2, 0, 0, 2, 2, -1, -1, 2, 0, 2, 0, 0, 4, -2,
			1, 3, 0, 2, -2, 1, -2, -1, 0, 2, 1, 1, 0, 0, -1, 1, 0, -2, 0, 2, 0, -2, 0, 0, 4, 1,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=181,190 )/
			-3, 0, 0, 0, 1, 1, 1, 2, 2, 2, 0, 0, 2, 4, 1, 3, 0, 2, 2, 2, -1, 1, 2, -2, 1,
			2, 0, 0, -4, 1, 0, 0, 0, -2, 2, 2, 0, 2, -4, 1, -1, 1, 0, 2, 1, 0, 0, 2, -1, 1,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=191,200 )/
			0, -2, 2, 2, 2, 2, 0, 0, 2, 1, 4, 0, 2, -2, 2, 2, 0, 0, -2, 2, 0, 2, 0, 0, 1,
			1, 0, 0, -4, 1, 0, 2, 2, -2, 1, -3, 0, 0, 4, 0, -1, 1, 2, 0, 1, -1, -1, 0, 4, 0,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=201,210 )/
			-1, -2, 2, 2, 2, -2, -1, 2, 4, 2, 1, -1, 2, 2, 1, -2, 1, 0, 2, 0, -2, 1, 2, 0, 1,
			2, 1, 0, -2, 1, -3, 0, 2, 0, 1, -2, 0, 2, -2, 1, -1, 1, 0, 2, 2, 0, -1, 2, -1, 2,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=211,220 )/
			-1, 0, 4, -2, 2, 0, -2, 2, 0, 2, -1, 0, 2, 1, 2, 2, 0, 0, 0, 2, 0, 0, 2, 0, 3,
			-2, 0, 4, 0, 2, -1, 0, -2, 0, 1, -1, 1, 2, 2, 1, 3, 0, 0, 0, 1, -1, 0, 2, 3, 2,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=221,230 )/
			2, -1, 2, 0, 1, 0, 1, 2, 2, 1, 0, -1, 2, 4, 2, 2, -1, 2, 2, 2, 0, 2, -2, 2, 0,
			-1, -1, 2, -1, 1, 0, -2, 0, 0, 1, 1, 0, 2, -4, 2, 1, -1, 0, -2, 1, -1, -1, 2, 0, 1,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=231,240 )/
			1, -1, 2, -2, 2, -2, -1, 0, 4, 0, -1, 0, 0, 3, 0, -2, -1, 2, 2, 2, 0, 2, 2, 0,
			2, 1, 1, 0, 2, 0, 2, 0, 2, -1, 2, 1, 0, 2, 1, 1, 4, 0, 0, 0, 0, 2, 1, 2, 0, 1,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=241,250 )/
			3, -1, 2, 0, 2, -2, 2, 0, 2, 1, 1, 0, 2, -3, 1, 1, 1, 2, -4, 1, -1, -1, 2, -2,  1,
			0, -1, 0, -1, 1, 0, -1, 0, -2, 1, -2, 0, 0, 0, 2, -2, 0, -2, 2, 0, -1, 0, -2, 4, 0,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=251,260 )/
			1, -2, 0, 0, 0, 0, 1, 0, 1, 1, -1, 2, 0, 2, 0, 1, -1, 2, -2, 1, 1, 2, 2, -2, 2,
			2, -1, 2, -2, 2, 1, 0, 2, -1, 1, 2, 1, 2, -2, 1, -2, 0, 0, -2, 1, 1, -2, 2, 0, 2,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=261,270 )/
			0, 1, 2, 1, 1, 1, 0, 4, -2, 1, -2, 0, 4, 2, 2, 1, 1, 2, 1, 2, 1, 0, 0, 4, 0,
			1, 0, 2, 2, 0, 2, 0, 2, 1, 2, 3, 1, 2, 0, 2, 4, 0, 2, 0, 1, -2, -1, 2, 0, 0,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=271,280 )/
			0, 1, -2, 2, 1, 1, 0, -2, 1, 0, 0, -1, -2, 2, 1, 2, -1, 0, -2, 1, -1, 0, 2, -1, 2,
			1, 0, 2, -3, 2, 0, 1, 2, -2, 3, 0, 0, 2, -3, 1, -1, 0, -2, 2, 1, 0, 0, 2, -4, 2,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=281,290 )/
			-2, 1, 0, 0, 1, -1, 0, 0, -1, 1, 2, 0, 2, -4, 2, 0, 0, 4, -4, 4, 0, 0, 4, -4, 2,
			-1, -2, 0, 2, 1, -2, 0, 0, 3, 0, 1, 0, -2, 2, 1, -3, 0, 2, 2, 2, -3, 0, 2, 2, 1,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=291,300 )/
			-2, 0, 2, 2, 0, 2, -1, 0, 0, 1, -2, 1, 2, 2, 2, 1, 1, 0, 1, 0, 0, 1, 4, -2, 2,
			-1, 1, 0, -2, 1, 0, 0, 0, -4, 1, 1, -1, 0, 2, 1, 1, 1, 0, 2, 1, -1, 2, 2, 2, 2,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=301,310 )/
			3, 1, 2, -2, 2, 0, -1, 0, 4, 0, 2, -1, 0, 2, 0, 0, 0, 4, 0, 1, 2, 0, 4, -2, 2,
			-1, -1, 2, 4, 1, 1, 0, 0, 4, 1, 1, -2, 2, 2, 2, 0, 0, 2, 3, 2, -1, 1, 2, 4, 2,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=311,320 )/
			3, 0, 0, 2, 0, -1, 0, 4, 2, 2, 1, 1, 2, 2, 1, -2, 0, 2, 6, 2, 2, 1, 2, 2, 2,
			-1, 0, 2, 6, 2, 1, 0, 2, 4, 1, 2, 0, 2, 4, 2, 1, 1, -2, 1, 0, -3, 1, 2, 1, 2,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=321,330 )/
			2, 0, -2, 0, 2, -1, 0, 0, 1, 2, -4, 0, 2, 2, 1, -1, -1, 0, 1, 0, 0, 0, -2, 2, 2,
			1, 0, 0, -1, 2, 0, -1, 2, -2, 3, -2, 1, 2, 0, 0, 0, 0, 2, -2, 4, -2, -2, 0, 2, 0,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=331,340 )/
			-2, 0, -2, 4, 0, 0, -2, -2, 2, 0, 1, 2, 0, -2, 1, 3, 0, 0, -4, 1, -1, 1, 2, -2, 2,
			1, -1, 2, -4, 1, 1, 1, 0, -2, 2, -3, 0, 2, 0, 0, -3, 0, 2, 0, 2, -2, 0, 0, 1, 0,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=341,350 )/
			0, 0, -2, 1, 0, -3, 0, 0, 2, 1, -1, -1, -2, 2, 0, 0, 1, 2, -4, 1, 2, 1, 0, -4, 1,
			0, 2, 0, -2, 1, 1, 0, 0, -3, 1, -2, 0, 2, -2, 2, -2, -1, 0, 0, 1, -4, 0, 0, 2, 0,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=351,360 )/
			1, 1, 0, -4, 1, -1, 0, 2, -4, 1, 0, 0, 4, -4, 1, 0, 3, 2, -2, 2, -3, -1, 0, 4, 0,
			-3, 0, 0, 4, 1, 1, -1, -2, 2, 0, -1, -1, 0, 2, 2, 1, -2, 0, 0, 1, 1, -1, 0, 0, 2,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=361,370 )/
			0, 0, 0, 1, 2, -1, -1, 2, 0, 0, 1, -2, 2, -2, 2, 0, -1, 2, -1, 1, -1, 0, 2, 0, 3,
			1, 1, 0, 0, 2, -1, 1, 2, 0, 0, 1, 2, 0, 0, 0, -1, 2, 2, 0, 2, -1, 0, 4, -2, 1,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=371,380 )/
			3, 0, 2, -4, 2, 1, 2, 2, -2, 1, 1, 0, 4, -4, 2, -2, -1, 0, 4, 1, 0, -1, 0, 2, 2,
			-2, 1, 0, 4, 0, -2, -1, 2, 2, 1, 2, 0, -2, 2, 0, 1, 0, 0, 1, 1, 0, 1, 0, 2, 2,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=381,390 )/
			1, -1, 2, -1, 2, -2, 0, 4, 0, 1, 2, 1, 0, 0, 1, 0, 1, 2, 0, 0, 0, -1, 4, -2, 2,
			0, 0, 4, -2, 4, 0, 2, 2, 0, 1, -3, 0, 0, 6, 0, -1, -1, 0, 4, 1, 1, -2, 0, 2, 0,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=391,400 )/
			-1, 0, 0, 4, 2, -1, -2, 2, 2, 1, -1, 0, 0, -2, 2, 1, 0, -2, -2, 1, 0, 0, -2, -2,
			1, -2, 0, -2, 0, 1, 0, 0, 0, 3, 1, 0, 0, 0, 3, 0, -1, 1, 0, 4, 0, -1, -1, 2, 2, 0,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=401,410 )/
			-2, 0, 2, 3, 2, 1, 0, 0, 2, 2, 0, -1, 2, 1, 2, 3, -1, 0, 0, 0, 2, 0, 0, 1, 0,
			1, -1, 2, 0, 0, 0, 0, 2, 1, 0, 1, 0, 2, 0, 3, 3, 1, 0, 0, 0, 3, -1, 2, -2, 2,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=411,420 )/
			2, 0, 2, -1, 1, 1, 1, 2, 0, 0, 0, 0, 4, -1, 2, 1, 2, 2, 0, 2, -2, 0, 0, 6, 0,
			0, -1, 0, 4, 1, -2, -1, 2, 4, 1, 0, -2, 2, 2, 1, 0, -1, 2, 2, 0, -1, 0, 2, 3, 1,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=421,430 )/
			-2, 1, 2, 4, 2, 2, 0, 0, 2, 2, 2, -2, 2, 0, 2, -1, 1, 2, 3, 2, 3, 0, 2, -1, 2,
			4, 0, 2, -2, 1, -1, 0, 0, 6, 0, -1, -2, 2, 4, 2, -3, 0, 2, 6, 2, -1, 0, 2, 4, 0,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=431,440 )/
			3, 0, 0, 2, 1, 3, -1, 2, 0, 1, 3, 0, 2, 0, 0, 1, 0, 4, 0, 2, 5, 0, 2, -2, 2, 0,
			-1, 2, 4, 1, 2, -1, 2, 2, 1, 0, 1, 2, 4, 2, 1, -1, 2, 4, 2, 3, -1, 2, 2, 2,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=441,450 )/
			3, 0, 2, 2, 1, 5, 0, 2, 0, 2, 0, 0, 2, 6, 2, 4, 0, 2, 2, 2, 0, -1, 1, -1, 1,
			-1, 0, 1, 0, 3, 0, -2, 2, -2, 3, 1, 0, -1, 0, 1, 2, -2, 0, -2, 1, -1, 0, 1, 0, 2,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=451,460 )/
			-1, 0, 1, 0, 1, -1, -1, 2, -1, 2, -2, 2, 0, 2, 2, -1, 0, 1, 0, 0, -4, 1, 2, 2, 2,
			-3, 0, 2, 1, 1, -2, -1, 2, 0, 2, 1, 0, -2, 1, 1, 2, -1, -2, 0, 1, -4, 0, 2, 2, 0,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=461,470 )/
			-3, 1, 0, 3, 0, -1, 0, -1, 2, 0, 0, -2, 0, 0, 2, 0, -2, 0, 0, 2, -3, 0, 0, 3, 0,
			-2, -1, 0, 2, 2, -1, 0, -2, 3, 0, -4, 0, 0, 4, 0, 2, 1, -2, 0, 1, 2, -1, 0, -2, 2,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=471,480 )/
			0, 0, 1, -1, 0, -1, 2, 0, 1, 0, -2, 1, 2, 0, 2, 1, 1, 0, -1, 1, 1, 0, 1, -2, 1, 0,
			2, 0, 0, 2, 1, -1, 2, -3, 1, -1, 1, 2, -1, 1, -2, 0, 4, -2, 2, -2, 0, 4, -2, 1,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=481,490 )/
			-2, -2, 0, 2, 1, -2, 0, -2, 4, 0, 1, 2, 2, -4, 1, 1, 1, 2, -4, 2, -1, 2, 2, -2, 1,
			2, 0, 0, -3, 1, -1, 2, 0, 0, 1, 0, 0, 0, -2, 0, -1, -1, 2, -2, 2, -1, 1, 0, 0, 2,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=491,500 )/
			0, 0, 0, -1, 2, -2, 1, 0, 1, 0, 1, -2, 0, -2, 1, 1, 0, -2, 0, 2, -3, 1, 0, 2, 0,
			-1, 1, -2, 2, 0, -1, -1, 0, 0, 2, -3, 0, 0, 2, 0, -3, -1, 0, 2, 0, 2, 0, 2, -6, 1,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=501,510 )/
			0, 1, 2, -4, 2, 2, 0, 0, -4, 2, -2, 1, 2, -2, 1, 0, -1, 2, -4, 1, 0, 1, 0, -2, 2,
			-1, 0, 0, -2, 0, 2, 0, -2, -2, 1, -4, 0, 2, 0, 1, -1, -1, 0, -1, 1, 0, 0, -2, 0, 2,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=511,520 )/
			-3, 0, 0, 1, 0, -1, 0, -2, 1, 0, -2, 0, -2, 2, 1, 0, 0, -4, 2, 0, -2, -1, -2, 2,
			0, 1, 0, 2, -6, 1, -1, 0, 2, -4, 2, 1, 0, 0, -4, 2, 2, 1, 2, -4, 2, 2, 1, 2, -4, 1,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=521,530 )/
			0, 1, 4, -4, 4, 0, 1, 4, -4, 2, -1, -1, -2, 4, 0, -1, -3, 0, 2, 0, -1, 0, -2, 4,
			1, -2, -1, 0, 3, 0, 0, 0, -2, 3, 0, -2, 0, 0, 3, 1, 0, -1, 0, 1, 0, -3, 0, 2, 2, 0,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=531,540 )/
			1, 1, -2, 2, 0, -1, 1, 0, 2, 2, 1, -2, 2, -2, 1, 0, 0, 1, 0, 2, 0, 0, 1, 0, 1, 0,
			0, 1, 0, 0, -1, 2, 0, 2, 1, 0, 0, 2, 0, 2, -2, 0, 2, 0, 2, 2, 0, 0, -1, 1,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=541,550 )/
			3, 0, 0, -2, 1, 1, 0, 2, -2, 3, 1, 2, 0, 0, 1, 2, 0, 2, -3, 2, -1, 1, 4, -2, 2,
			-2, -2, 0, 4, 0, 0, -3, 0, 2, 0, 0, 0, -2, 4, 0, -1, -1, 0, 3, 0, -2, 0, 0, 4, 2,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=551,560 )/
			-1, 0, 0, 3, 1, 2, -2, 0, 0, 0, 1, -1, 0, 1, 0, -1, 0, 0, 2, 0, 0, -2, 2, 0, 1,
			-1, 0, 1, 2, 1, -1, 1, 0, 3, 0, -1, -1, 2, 1, 2, 0, -1, 2, 0, 0, -2, 1, 2, 2, 1,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=561,570 )/
			2, -2, 2, -2, 2, 1, 1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0, 0, 0, 2, 0, 2, 0, 2,
			-1, 2, -2, 1, 0, -1, 4, -2, 1, 0, 0, 4, -2, 3, 0, 1, 4, -2, 1, 4, 0, 2, -4, 2,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=571,580 )/
			2, 2, 2, -2, 2, 2, 0, 4, -4, 2, -1, -2, 0, 4, 0, -1, -3, 2, 2, 2, -3, 0, 2, 4, 2,
			-3, 0, 2, -2, 1, -1, -1, 0, -2, 1, -3, 0, 0, 0, 2, -3, 0, -2, 2, 0, 0, 1, 0, -4, 1,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=581,590 )/
			-2, 1, 0, -2, 1, -4, 0, 0, 0, 1, -1, 0, 0, -4, 1, -3, 0, 0, -2, 1, 0, 0, 0, 3, 2,
			-1, 1, 0, 4, 1, 1, -2, 2, 0, 1, 0, 1, 0, 3, 0, -1, 0, 2, 2, 3, 0, 0, 2, 2, 2,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=591,600 )/
			-2, 0, 2, 2, 2, -1, 1, 2, 2, 0, 3, 0, 0, 0, 2, 2, 1, 0, 1, 0, 2, -1, 2, -1, 2, 0,
			0, 2, 0, 1, 0, 0, 3, 0, 3, 0, 0, 3, 0, 2, -1, 2, 2, 2, 1, -1, 0, 4, 0, 0,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=601,610 )/
			1, 2, 2, 0, 1, 3, 1, 2, -2, 1, 1, 1, 4, -2, 2, -2, -1, 0, 6, 0, 0, -2, 0, 4, 0, -2,
			0, 0, 6, 1, -2, -2, 2, 4, 2, 0, -3, 2, 2, 2, 0, 0, 0, 4, 2, -1, -1, 2, 3, 2,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=611,620 )/
			-2, 0, 2, 4, 0, 2, -1, 0, 2, 1, 1, 0, 0, 3, 0, 0, 1, 0, 4, 1, 0, 1, 0, 4, 0, 1, -1,
			2, 1, 2, 0, 0, 2, 2, 3, 1, 0, 2, 2, 2, -1, 0, 2, 2, 2, -2, 0, 4, 2, 1,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=621,630 )/
			2, 1, 0, 2, 1, 2, 1, 0, 2, 0, 2, -1, 2, 0, 0, 1, 0, 2, 1, 0, 0, 1, 2, 2, 0, 2, 0,
			2, 0, 3, 3, 0, 2, 0, 2, 1, 0, 2, 0, 2, 1, 0, 3, 0, 3, 1, 1, 2, 1, 1,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=631,640 )/
			0, 2, 2, 2, 2, 2, 1, 2, 0, 0, 2, 0, 4, -2, 1, 4, 1, 2, -2, 2, -1, -1, 0, 6, 0, -3,
			-1, 2, 6, 2, -1, 0, 0, 6, 1, -3, 0, 2, 6, 1, 1, -1, 0, 4, 1, 1, -1, 0, 4, 0,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=641,650 )/
			-2, 0, 2, 5, 2, 1, -2, 2, 2, 1, 3, -1, 0, 2, 0, 1, -1, 2, 2, 0, 0, 0, 2, 3, 1, -1,
			1, 2, 4, 1, 0, 1, 2, 3, 2, -1, 0, 4, 2, 1, 2, 0, 2, 1, 1, 5, 0, 0, 0, 0,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=651,660 )/
			2, 1, 2, 1, 2, 1, 0, 4, 0, 1, 3, 1, 2, 0, 1, 3, 0, 4, -2, 2, -2, -1, 2, 6, 2, 0, 0,
			0, 6, 0, 0, -2, 2, 4, 2, -2, 0, 2, 6, 1, 2, 0, 0, 4, 1, 2, 0, 0, 4, 0,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=661,670 )/
			2, -2, 2, 2, 2, 0, 0, 2, 4, 0, 1, 0, 2, 3, 2, 4, 0, 0, 2, 0, 2, 0, 2, 2, 0, 0, 0,
			4, 2, 2, 4, -1, 2, 0, 2, 3, 0, 2, 1, 2, 2, 1, 2, 2, 1, 4, 1, 2, 0, 2,
			// DATA ( ( NALS(I,J)/ I=1,5 )/ J=671,678 )/
			-1, -1, 2, 6, 2, -1, 0, 2, 6, 1, 1, -1, 2, 4, 1, 1, 1, 2, 4, 2, 3, 1, 2, 2, 2, 5,
			0, 2, 0, 1, 2, -1, 2, 4, 2, 2, 0, 2, 4, 1, };
}

final class IAU2000_CLS
{
	/*
	 * Luni-Solar nutation coefficients, unit 1e-7 arcsec longitude (sin, t*sin,
	 * cos) / obliquity (cos, t*cos, sin)
	 */

	static double[] CLS =
	{ // DATA ( ( CLS(I,J) / I=1,6 ) / J= 1, 10 ) /
			-172064161.0, -174666.0, 33386.0, 92052331.0, 9086.0, 15377.0, -13170906.0, -1675.0, -13696.0, 5730336.0, -3015.0, -4587.0, -2276413.0, -234.0, 2796.0, 978459.0, -485.0, 1374.0, 2074554.0, 207.0, -698.0, -897492.0, 470.0, -291.0, 1475877.0, -3633.0, 11817.0, 73871.0, -184.0, -1924.0, -516821.0, 1226.0, -524.0, 224386.0, -677.0, -174.0, 711159.0, 73.0, -872.0, -6750.0, 0.0, 358.0, -387298.0, -367.0, 380.0, 200728.0, 18.0, 318.0, -301461.0, -36.0, 816.0, 129025.0, -63.0, 367.0, 215829.0, -494.0, 111.0, -95929.0, 299.0, 132.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J= 11, 20 ) /
			 128227.0, 137.0, 181.0, -68982.0, -9.0, 39.0, 123457.0, 11.0, 19.0, -53311.0, 32.0, -4.0, 156994.0, 10.0, -168.0, -1235.0, 0.0, 82.0, 63110.0, 63.0, 27.0, -33228.0, 0.0, -9.0, -57976.0, -63.0, -189.0, 31429.0, 0.0, -75.0, -59641.0, -11.0, 149.0, 25543.0, -11.0, 66.0, -51613.0, -42.0, 129.0, 26366.0, 0.0, 78.0, 45893.0, 50.0, 31.0, -24236.0, -10.0, 20.0, 63384.0, 11.0, -150.0, -1220.0, 0.0, 29.0, -38571.0, -1.0, 158.0, 16452.0, -11.0, 68.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J= 21, 30 ) /
			 32481.0, 0.0, 0.0, -13870.0, 0.0, 0.0, -47722.0, 0.0, -18.0, 477.0, 0.0, -25.0, -31046.0, -1.0, 131.0, 13238.0, -11.0, 59.0, 28593.0, 0.0, -1.0, -12338.0, 10.0, -3.0, 20441.0, 21.0, 10.0, -10758.0, 0.0, -3.0, 29243.0, 0.0, -74.0, -609.0, 0.0, 13.0, 25887.0, 0.0, -66.0, -550.0, 0.0, 11.0, -14053.0, -25.0, 79.0, 8551.0, -2.0, -45.0, 15164.0, 10.0, 11.0, -8001.0, 0.0, -1.0, -15794.0, 72.0, -16.0, 6850.0, -42.0, -5.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J= 31, 40 ) /
			 21783.0, 0.0, 13.0, -167.0, 0.0, 13.0, -12873.0, -10.0, -37.0, 6953.0, 0.0, -14.0, -12654.0, 11.0, 63.0, 6415.0, 0.0, 26.0, -10204.0, 0.0, 25.0, 5222.0, 0.0, 15.0, 16707.0, -85.0, -10.0, 168.0, -1.0, 10.0, -7691.0, 0.0, 44.0, 3268.0, 0.0, 19.0, -11024.0, 0.0, -14.0, 104.0, 0.0, 2.0, 7566.0, -21.0, -11.0, -3250.0, 0.0, -5.0, -6637.0, -11.0, 25.0, 3353.0, 0.0, 14.0, -7141.0, 21.0, 8.0, 3070.0, 0.0, 4.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J= 41, 50 ) /
			 -6302.0, -11.0, 2.0, 3272.0, 0.0, 4.0, 5800.0, 10.0, 2.0, -3045.0, 0.0, -1.0, 6443.0, 0.0, -7.0, -2768.0, 0.0, -4.0, -5774.0, -11.0, -15.0, 3041.0, 0.0, -5.0, -5350.0, 0.0, 21.0, 2695.0, 0.0, 12.0, -4752.0, -11.0, -3.0, 2719.0, 0.0, -3.0, -4940.0, -11.0, -21.0, 2720.0, 0.0, -9.0, 7350.0, 0.0, -8.0, -51.0, 0.0, 4.0, 4065.0, 0.0, 6.0, -2206.0, 0.0, 1.0, 6579.0, 0.0, -24.0, -199.0, 0.0, 2.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J= 51, 60 ) /
			 3579.0, 0.0, 5.0, -1900.0, 0.0, 1.0, 4725.0, 0.0, -6.0, -41.0, 0.0, 3.0, -3075.0, 0.0, -2.0, 1313.0, 0.0, -1.0, -2904.0, 0.0, 15.0, 1233.0, 0.0, 7.0, 4348.0, 0.0, -10.0, -81.0, 0.0, 2.0, -2878.0, 0.0, 8.0, 1232.0, 0.0, 4.0, -4230.0, 0.0, 5.0, -20.0, 0.0, -2.0, -2819.0, 0.0, 7.0, 1207.0, 0.0, 3.0, -4056.0, 0.0, 5.0, 40.0, 0.0, -2.0, -2647.0, 0.0, 11.0, 1129.0, 0.0, 5.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J= 61, 70 ) /
			 -2294.0, 0.0, -10.0, 1266.0, 0.0, -4.0, 2481.0, 0.0, -7.0, -1062.0, 0.0, -3.0, 2179.0, 0.0, -2.0, -1129.0, 0.0, -2.0, 3276.0, 0.0, 1.0, -9.0, 0.0, 0.0, -3389.0, 0.0, 5.0, 35.0, 0.0, -2.0, 3339.0, 0.0, -13.0, -107.0, 0.0, 1.0, -1987.0, 0.0, -6.0, 1073.0, 0.0, -2.0, -1981.0, 0.0, 0.0, 854.0, 0.0, 0.0, 4026.0, 0.0, -353.0, -553.0, 0.0, -139.0, 1660.0, 0.0, -5.0, -710.0, 0.0, -2.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J= 71, 80 ) /
			 -1521.0, 0.0, 9.0, 647.0, 0.0, 4.0, 1314.0, 0.0, 0.0, -700.0, 0.0, 0.0, -1283.0, 0.0, 0.0, 672.0, 0.0, 0.0, -1331.0, 0.0, 8.0, 663.0, 0.0, 4.0, 1383.0, 0.0, -2.0, -594.0, 0.0, -2.0, 1405.0, 0.0, 4.0, -610.0, 0.0, 2.0, 1290.0, 0.0, 0.0, -556.0, 0.0, 0.0, -1214.0, 0.0, 5.0, 518.0, 0.0, 2.0, 1146.0, 0.0, -3.0, -490.0, 0.0, -1.0, 1019.0, 0.0, -1.0, -527.0, 0.0, -1.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J= 81, 90 ) /
			 -1100.0, 0.0, 9.0, 465.0, 0.0, 4.0, -970.0, 0.0, 2.0, 496.0, 0.0, 1.0, 1575.0, 0.0, -6.0, -50.0, 0.0, 0.0, 934.0, 0.0, -3.0, -399.0, 0.0, -1.0, 922.0, 0.0, -1.0, -395.0, 0.0, -1.0, 815.0, 0.0, -1.0, -422.0, 0.0, -1.0, 834.0, 0.0, 2.0, -440.0, 0.0, 1.0, 1248.0, 0.0, 0.0, -170.0, 0.0, 1.0, 1338.0, 0.0, -5.0, -39.0, 0.0, 0.0, 716.0, 0.0, -2.0, -389.0, 0.0, -1.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J= 91,100 ) /
			 1282.0, 0.0, -3.0, -23.0, 0.0, 1.0, 742.0, 0.0, 1.0, -391.0, 0.0, 0.0, 1020.0, 0.0, -25.0, -495.0, 0.0, -10.0, 715.0, 0.0, -4.0, -326.0, 0.0, 2.0, -666.0, 0.0, -3.0, 369.0, 0.0, -1.0, -667.0, 0.0, 1.0, 346.0, 0.0, 1.0, -704.0, 0.0, 0.0, 304.0, 0.0, 0.0, -694.0, 0.0, 5.0, 294.0, 0.0, 2.0, -1014.0, 0.0, -1.0, 4.0, 0.0, -1.0, -585.0, 0.0, -2.0, 316.0, 0.0, -1.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=101,110 ) /
			 -949.0, 0.0, 1.0, 8.0, 0.0, -1.0, -595.0, 0.0, 0.0, 258.0, 0.0, 0.0, 528.0, 0.0, 0.0, -279.0, 0.0, 0.0, -590.0, 0.0, 4.0, 252.0, 0.0, 2.0, 570.0, 0.0, -2.0, -244.0, 0.0, -1.0, -502.0, 0.0, 3.0, 250.0, 0.0, 2.0, -875.0, 0.0, 1.0, 29.0, 0.0, 0.0, -492.0, 0.0, -3.0, 275.0, 0.0, -1.0, 535.0, 0.0, -2.0, -228.0, 0.0, -1.0, -467.0, 0.0, 1.0, 240.0, 0.0, 1.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=111,120 ) /
			 591.0, 0.0, 0.0, -253.0, 0.0, 0.0, -453.0, 0.0, -1.0, 244.0, 0.0, -1.0, 766.0, 0.0, 1.0, 9.0, 0.0, 0.0, -446.0, 0.0, 2.0, 225.0, 0.0, 1.0, -488.0, 0.0, 2.0, 207.0, 0.0, 1.0, -468.0, 0.0, 0.0, 201.0, 0.0, 0.0, -421.0, 0.0, 1.0, 216.0, 0.0, 1.0, 463.0, 0.0, 0.0, -200.0, 0.0, 0.0, -673.0, 0.0, 2.0, 14.0, 0.0, 0.0, 658.0, 0.0, 0.0, -2.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=121,130 ) /
			 -438.0, 0.0, 0.0, 188.0, 0.0, 0.0, -390.0, 0.0, 0.0, 205.0, 0.0, 0.0, 639.0, -11.0, -2.0, -19.0, 0.0, 0.0, 412.0, 0.0, -2.0, -176.0, 0.0, -1.0, -361.0, 0.0, 0.0, 189.0, 0.0, 0.0, 360.0, 0.0, -1.0, -185.0, 0.0, -1.0, 588.0, 0.0, -3.0, -24.0, 0.0, 0.0, -578.0, 0.0, 1.0, 5.0, 0.0, 0.0, -396.0, 0.0, 0.0, 171.0, 0.0, 0.0, 565.0, 0.0, -1.0, -6.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=131,140 ) /
			 -335.0, 0.0, -1.0, 184.0, 0.0, -1.0, 357.0, 0.0, 1.0, -154.0, 0.0, 0.0, 321.0, 0.0, 1.0, -174.0, 0.0, 0.0, -301.0, 0.0, -1.0, 162.0, 0.0, 0.0, -334.0, 0.0, 0.0, 144.0, 0.0, 0.0, 493.0, 0.0, -2.0, -15.0, 0.0, 0.0, 494.0, 0.0, -2.0, -19.0, 0.0, 0.0, 337.0, 0.0, -1.0, -143.0, 0.0, -1.0, 280.0, 0.0, -1.0, -144.0, 0.0, 0.0, 309.0, 0.0, 1.0, -134.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=141,150 ) /
			 -263.0, 0.0, 2.0, 131.0, 0.0, 1.0, 253.0, 0.0, 1.0, -138.0, 0.0, 0.0, 245.0, 0.0, 0.0, -128.0, 0.0, 0.0, 416.0, 0.0, -2.0, -17.0, 0.0, 0.0, -229.0, 0.0, 0.0, 128.0, 0.0, 0.0, 231.0, 0.0, 0.0, -120.0, 0.0, 0.0, -259.0, 0.0, 2.0, 109.0, 0.0, 1.0, 375.0, 0.0, -1.0, -8.0, 0.0, 0.0, 252.0, 0.0, 0.0, -108.0, 0.0, 0.0, -245.0, 0.0, 1.0, 104.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=151,160 ) /
			 243.0, 0.0, -1.0, -104.0, 0.0, 0.0, 208.0, 0.0, 1.0, -112.0, 0.0, 0.0, 199.0, 0.0, 0.0, -102.0, 0.0, 0.0, -208.0, 0.0, 1.0, 105.0, 0.0, 0.0, 335.0, 0.0, -2.0, -14.0, 0.0, 0.0, -325.0, 0.0, 1.0, 7.0, 0.0, 0.0, -187.0, 0.0, 0.0, 96.0, 0.0, 0.0, 197.0, 0.0, -1.0, -100.0, 0.0, 0.0, -192.0, 0.0, 2.0, 94.0, 0.0, 1.0, -188.0, 0.0, 0.0, 83.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=161,170 ) /
			 276.0, 0.0, 0.0, -2.0, 0.0, 0.0, -286.0, 0.0, 1.0, 6.0, 0.0, 0.0, 186.0, 0.0, -1.0, -79.0, 0.0, 0.0, -219.0, 0.0, 0.0, 43.0, 0.0, 0.0, 276.0, 0.0, 0.0, 2.0, 0.0, 0.0, -153.0, 0.0, -1.0, 84.0, 0.0, 0.0, -156.0, 0.0, 0.0, 81.0, 0.0, 0.0, -154.0, 0.0, 1.0, 78.0, 0.0, 0.0, -174.0, 0.0, 1.0, 75.0, 0.0, 0.0, -163.0, 0.0, 2.0, 69.0, 0.0, 1.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=171,180 ) /
			 -228.0, 0.0, 0.0, 1.0, 0.0, 0.0, 91.0, 0.0, -4.0, -54.0, 0.0, -2.0, 175.0, 0.0, 0.0, -75.0, 0.0, 0.0, -159.0, 0.0, 0.0, 69.0, 0.0, 0.0, 141.0, 0.0, 0.0, -72.0, 0.0, 0.0, 147.0, 0.0, 0.0, -75.0, 0.0, 0.0, -132.0, 0.0, 0.0, 69.0, 0.0, 0.0, 159.0, 0.0, -28.0, -54.0, 0.0, 11.0, 213.0, 0.0, 0.0, -4.0, 0.0, 0.0, 123.0, 0.0, 0.0, -64.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=181,190 ) /
			 -118.0, 0.0, -1.0, 66.0, 0.0, 0.0, 144.0, 0.0, -1.0, -61.0, 0.0, 0.0, -121.0, 0.0, 1.0, 60.0, 0.0, 0.0, -134.0, 0.0, 1.0, 56.0, 0.0, 1.0, -105.0, 0.0, 0.0, 57.0, 0.0, 0.0, -102.0, 0.0, 0.0, 56.0, 0.0, 0.0, 120.0, 0.0, 0.0, -52.0, 0.0, 0.0, 101.0, 0.0, 0.0, -54.0, 0.0, 0.0, -113.0, 0.0, 0.0, 59.0, 0.0, 0.0, -106.0, 0.0, 0.0, 61.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=191,200 ) /
			 -129.0, 0.0, 1.0, 55.0, 0.0, 0.0, -114.0, 0.0, 0.0, 57.0, 0.0, 0.0, 113.0, 0.0, -1.0, -49.0, 0.0, 0.0, -102.0, 0.0, 0.0, 44.0, 0.0, 0.0, -94.0, 0.0, 0.0, 51.0, 0.0, 0.0, -100.0, 0.0, -1.0, 56.0, 0.0, 0.0, 87.0, 0.0, 0.0, -47.0, 0.0, 0.0, 161.0, 0.0, 0.0, -1.0, 0.0, 0.0, 96.0, 0.0, 0.0, -50.0, 0.0, 0.0, 151.0, 0.0, -1.0, -5.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=201,210 ) /
			 -104.0, 0.0, 0.0, 44.0, 0.0, 0.0, -110.0, 0.0, 0.0, 48.0, 0.0, 0.0, -100.0, 0.0, 1.0, 50.0, 0.0, 0.0, 92.0, 0.0, -5.0, 12.0, 0.0, -2.0, 82.0, 0.0, 0.0, -45.0, 0.0, 0.0, 82.0, 0.0, 0.0, -45.0, 0.0, 0.0, -78.0, 0.0, 0.0, 41.0, 0.0, 0.0, -77.0, 0.0, 0.0, 43.0, 0.0, 0.0, 2.0, 0.0, 0.0, 54.0, 0.0, 0.0, 94.0, 0.0, 0.0, -40.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=211,220 ) /
			 -93.0, 0.0, 0.0, 40.0, 0.0, 0.0, -83.0, 0.0, 10.0, 40.0, 0.0, -2.0, 83.0, 0.0, 0.0, -36.0, 0.0, 0.0, -91.0, 0.0, 0.0, 39.0, 0.0, 0.0, 128.0, 0.0, 0.0, -1.0, 0.0, 0.0, -79.0, 0.0, 0.0, 34.0, 0.0, 0.0, -83.0, 0.0, 0.0, 47.0, 0.0, 0.0, 84.0, 0.0, 0.0, -44.0, 0.0, 0.0, 83.0, 0.0, 0.0, -43.0, 0.0, 0.0, 91.0, 0.0, 0.0, -39.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=221,230 ) /
			 -77.0, 0.0, 0.0, 39.0, 0.0, 0.0, 84.0, 0.0, 0.0, -43.0, 0.0, 0.0, -92.0, 0.0, 1.0, 39.0, 0.0, 0.0, -92.0, 0.0, 1.0, 39.0, 0.0, 0.0, -94.0, 0.0, 0.0, 0.0, 0.0, 0.0, 68.0, 0.0, 0.0, -36.0, 0.0, 0.0, -61.0, 0.0, 0.0, 32.0, 0.0, 0.0, 71.0, 0.0, 0.0, -31.0, 0.0, 0.0, 62.0, 0.0, 0.0, -34.0, 0.0, 0.0, -63.0, 0.0, 0.0, 33.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=231,240 ) /
			 -73.0, 0.0, 0.0, 32.0, 0.0, 0.0, 115.0, 0.0, 0.0, -2.0, 0.0, 0.0, -103.0, 0.0, 0.0, 2.0, 0.0, 0.0, 63.0, 0.0, 0.0, -28.0, 0.0, 0.0, 74.0, 0.0, 0.0, -32.0, 0.0, 0.0, -103.0, 0.0, -3.0, 3.0, 0.0, -1.0, -69.0, 0.0, 0.0, 30.0, 0.0, 0.0, 57.0, 0.0, 0.0, -29.0, 0.0, 0.0, 94.0, 0.0, 0.0, -4.0, 0.0, 0.0, 64.0, 0.0, 0.0, -33.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=241,250 ) /
			 -63.0, 0.0, 0.0, 26.0, 0.0, 0.0, -38.0, 0.0, 0.0, 20.0, 0.0, 0.0, -43.0, 0.0, 0.0, 24.0, 0.0, 0.0, -45.0, 0.0, 0.0, 23.0, 0.0, 0.0, 47.0, 0.0, 0.0, -24.0, 0.0, 0.0, -48.0, 0.0, 0.0, 25.0, 0.0, 0.0, 45.0, 0.0, 0.0, -26.0, 0.0, 0.0, 56.0, 0.0, 0.0, -25.0, 0.0, 0.0, 88.0, 0.0, 0.0, 2.0, 0.0, 0.0, -75.0, 0.0, 0.0, 0.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=251,260 ) /
			 85.0, 0.0, 0.0, 0.0, 0.0, 0.0, 49.0, 0.0, 0.0, -26.0, 0.0, 0.0, -74.0, 0.0, -3.0, -1.0, 0.0, -1.0, -39.0, 0.0, 0.0, 21.0, 0.0, 0.0, 45.0, 0.0, 0.0, -20.0, 0.0, 0.0, 51.0, 0.0, 0.0, -22.0, 0.0, 0.0, -40.0, 0.0, 0.0, 21.0, 0.0, 0.0, 41.0, 0.0, 0.0, -21.0, 0.0, 0.0, -42.0, 0.0, 0.0, 24.0, 0.0, 0.0, -51.0, 0.0, 0.0, 22.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=261,270 ) /
			 -42.0, 0.0, 0.0, 22.0, 0.0, 0.0, 39.0, 0.0, 0.0, -21.0, 0.0, 0.0, 46.0, 0.0, 0.0, -18.0, 0.0, 0.0, -53.0, 0.0, 0.0, 22.0, 0.0, 0.0, 82.0, 0.0, 0.0, -4.0, 0.0, 0.0, 81.0, 0.0, -1.0, -4.0, 0.0, 0.0, 47.0, 0.0, 0.0, -19.0, 0.0, 0.0, 53.0, 0.0, 0.0, -23.0, 0.0, 0.0, -45.0, 0.0, 0.0, 22.0, 0.0, 0.0, -44.0, 0.0, 0.0, -2.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=271,280 ) /
			 -33.0, 0.0, 0.0, 16.0, 0.0, 0.0, -61.0, 0.0, 0.0, 1.0, 0.0, 0.0, 28.0, 0.0, 0.0, -15.0, 0.0, 0.0, -38.0, 0.0, 0.0, 19.0, 0.0, 0.0, -33.0, 0.0, 0.0, 21.0, 0.0, 0.0, -60.0, 0.0, 0.0, 0.0, 0.0, 0.0, 48.0, 0.0, 0.0, -10.0, 0.0, 0.0, 27.0, 0.0, 0.0, -14.0, 0.0, 0.0, 38.0, 0.0, 0.0, -20.0, 0.0, 0.0, 31.0, 0.0, 0.0, -13.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=281,290 ) /
			 -29.0, 0.0, 0.0, 15.0, 0.0, 0.0, 28.0, 0.0, 0.0, -15.0, 0.0, 0.0, -32.0, 0.0, 0.0, 15.0, 0.0, 0.0, 45.0, 0.0, 0.0, -8.0, 0.0, 0.0, -44.0, 0.0, 0.0, 19.0, 0.0, 0.0, 28.0, 0.0, 0.0, -15.0, 0.0, 0.0, -51.0, 0.0, 0.0, 0.0, 0.0, 0.0, -36.0, 0.0, 0.0, 20.0, 0.0, 0.0, 44.0, 0.0, 0.0, -19.0, 0.0, 0.0, 26.0, 0.0, 0.0, -14.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=291,300 ) /
			 -60.0, 0.0, 0.0, 2.0, 0.0, 0.0, 35.0, 0.0, 0.0, -18.0, 0.0, 0.0, -27.0, 0.0, 0.0, 11.0, 0.0, 0.0, 47.0, 0.0, 0.0, -1.0, 0.0, 0.0, 36.0, 0.0, 0.0, -15.0, 0.0, 0.0, -36.0, 0.0, 0.0, 20.0, 0.0, 0.0, -35.0, 0.0, 0.0, 19.0, 0.0, 0.0, -37.0, 0.0, 0.0, 19.0, 0.0, 0.0, 32.0, 0.0, 0.0, -16.0, 0.0, 0.0, 35.0, 0.0, 0.0, -14.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=301,310 ) /
			 32.0, 0.0, 0.0, -13.0, 0.0, 0.0, 65.0, 0.0, 0.0, -2.0, 0.0, 0.0, 47.0, 0.0, 0.0, -1.0, 0.0, 0.0, 32.0, 0.0, 0.0, -16.0, 0.0, 0.0, 37.0, 0.0, 0.0, -16.0, 0.0, 0.0, -30.0, 0.0, 0.0, 15.0, 0.0, 0.0, -32.0, 0.0, 0.0, 16.0, 0.0, 0.0, -31.0, 0.0, 0.0, 13.0, 0.0, 0.0, 37.0, 0.0, 0.0, -16.0, 0.0, 0.0, 31.0, 0.0, 0.0, -13.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=311,320 ) /
			 49.0, 0.0, 0.0, -2.0, 0.0, 0.0, 32.0, 0.0, 0.0, -13.0, 0.0, 0.0, 23.0, 0.0, 0.0, -12.0, 0.0, 0.0, -43.0, 0.0, 0.0, 18.0, 0.0, 0.0, 26.0, 0.0, 0.0, -11.0, 0.0, 0.0, -32.0, 0.0, 0.0, 14.0, 0.0, 0.0, -29.0, 0.0, 0.0, 14.0, 0.0, 0.0, -27.0, 0.0, 0.0, 12.0, 0.0, 0.0, 30.0, 0.0, 0.0, 0.0, 0.0, 0.0, -11.0, 0.0, 0.0, 5.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=321,330 ) /
			 -21.0, 0.0, 0.0, 10.0, 0.0, 0.0, -34.0, 0.0, 0.0, 15.0, 0.0, 0.0, -10.0, 0.0, 0.0, 6.0, 0.0, 0.0, -36.0, 0.0, 0.0, 0.0, 0.0, 0.0, -9.0, 0.0, 0.0, 4.0, 0.0, 0.0, -12.0, 0.0, 0.0, 5.0, 0.0, 0.0, -21.0, 0.0, 0.0, 5.0, 0.0, 0.0, -29.0, 0.0, 0.0, -1.0, 0.0, 0.0, -15.0, 0.0, 0.0, 3.0, 0.0, 0.0, -20.0, 0.0, 0.0, 0.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=331,340 ) /
			 28.0, 0.0, 0.0, 0.0, 0.0, -2.0, 17.0, 0.0, 0.0, 0.0, 0.0, 0.0, -22.0, 0.0, 0.0, 12.0, 0.0, 0.0, -14.0, 0.0, 0.0, 7.0, 0.0, 0.0, 24.0, 0.0, 0.0, -11.0, 0.0, 0.0, 11.0, 0.0, 0.0, -6.0, 0.0, 0.0, 14.0, 0.0, 0.0, -6.0, 0.0, 0.0, 24.0, 0.0, 0.0, 0.0, 0.0, 0.0, 18.0, 0.0, 0.0, -8.0, 0.0, 0.0, -38.0, 0.0, 0.0, 0.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=341,350 ) /
			 -31.0, 0.0, 0.0, 0.0, 0.0, 0.0, -16.0, 0.0, 0.0, 8.0, 0.0, 0.0, 29.0, 0.0, 0.0, 0.0, 0.0, 0.0, -18.0, 0.0, 0.0, 10.0, 0.0, 0.0, -10.0, 0.0, 0.0, 5.0, 0.0, 0.0, -17.0, 0.0, 0.0, 10.0, 0.0, 0.0, 9.0, 0.0, 0.0, -4.0, 0.0, 0.0, 16.0, 0.0, 0.0, -6.0, 0.0, 0.0, 22.0, 0.0, 0.0, -12.0, 0.0, 0.0, 20.0, 0.0, 0.0, 0.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=351,360 ) /
			 -13.0, 0.0, 0.0, 6.0, 0.0, 0.0, -17.0, 0.0, 0.0, 9.0, 0.0, 0.0, -14.0, 0.0, 0.0, 8.0, 0.0, 0.0, 0.0, 0.0, 0.0, -7.0, 0.0, 0.0, 14.0, 0.0, 0.0, 0.0, 0.0, 0.0, 19.0, 0.0, 0.0, -10.0, 0.0, 0.0, -34.0, 0.0, 0.0, 0.0, 0.0, 0.0, -20.0, 0.0, 0.0, 8.0, 0.0, 0.0, 9.0, 0.0, 0.0, -5.0, 0.0, 0.0, -18.0, 0.0, 0.0, 7.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=361,370 ) /
			 13.0, 0.0, 0.0, -6.0, 0.0, 0.0, 17.0, 0.0, 0.0, 0.0, 0.0, 0.0, -12.0, 0.0, 0.0, 5.0, 0.0, 0.0, 15.0, 0.0, 0.0, -8.0, 0.0, 0.0, -11.0, 0.0, 0.0, 3.0, 0.0, 0.0, 13.0, 0.0, 0.0, -5.0, 0.0, 0.0, -18.0, 0.0, 0.0, 0.0, 0.0, 0.0, -35.0, 0.0, 0.0, 0.0, 0.0, 0.0, 9.0, 0.0, 0.0, -4.0, 0.0, 0.0, -19.0, 0.0, 0.0, 10.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=371,380 ) /
			 -26.0, 0.0, 0.0, 11.0, 0.0, 0.0, 8.0, 0.0, 0.0, -4.0, 0.0, 0.0, -10.0, 0.0, 0.0, 4.0, 0.0, 0.0, 10.0, 0.0, 0.0, -6.0, 0.0, 0.0, -21.0, 0.0, 0.0, 9.0, 0.0, 0.0, -15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 9.0, 0.0, 0.0, -5.0, 0.0, 0.0, -29.0, 0.0, 0.0, 0.0, 0.0, 0.0, -19.0, 0.0, 0.0, 10.0, 0.0, 0.0, 12.0, 0.0, 0.0, -5.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=381,390 ) /
			 22.0, 0.0, 0.0, -9.0, 0.0, 0.0, -10.0, 0.0, 0.0, 5.0, 0.0, 0.0, -20.0, 0.0, 0.0, 11.0, 0.0, 0.0, -20.0, 0.0, 0.0, 0.0, 0.0, 0.0, -17.0, 0.0, 0.0, 7.0, 0.0, 0.0, 15.0, 0.0, 0.0, -3.0, 0.0, 0.0, 8.0, 0.0, 0.0, -4.0, 0.0, 0.0, 14.0, 0.0, 0.0, 0.0, 0.0, 0.0, -12.0, 0.0, 0.0, 6.0, 0.0, 0.0, 25.0, 0.0, 0.0, 0.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=391,400 ) /
			 -13.0, 0.0, 0.0, 6.0, 0.0, 0.0, -14.0, 0.0, 0.0, 8.0, 0.0, 0.0, 13.0, 0.0, 0.0, -5.0, 0.0, 0.0, -17.0, 0.0, 0.0, 9.0, 0.0, 0.0, -12.0, 0.0, 0.0, 6.0, 0.0, 0.0, -10.0, 0.0, 0.0, 5.0, 0.0, 0.0, 10.0, 0.0, 0.0, -6.0, 0.0, 0.0, -15.0, 0.0, 0.0, 0.0, 0.0, 0.0, -22.0, 0.0, 0.0, 0.0, 0.0, 0.0, 28.0, 0.0, 0.0, -1.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=401,410 ) /
			 15.0, 0.0, 0.0, -7.0, 0.0, 0.0, 23.0, 0.0, 0.0, -10.0, 0.0, 0.0, 12.0, 0.0, 0.0, -5.0, 0.0, 0.0, 29.0, 0.0, 0.0, -1.0, 0.0, 0.0, -25.0, 0.0, 0.0, 1.0, 0.0, 0.0, 22.0, 0.0, 0.0, 0.0, 0.0, 0.0, -18.0, 0.0, 0.0, 0.0, 0.0, 0.0, 15.0, 0.0, 0.0, 3.0, 0.0, 0.0, -23.0, 0.0, 0.0, 0.0, 0.0, 0.0, 12.0, 0.0, 0.0, -5.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=411,420 ) /
			 -8.0, 0.0, 0.0, 4.0, 0.0, 0.0, -19.0, 0.0, 0.0, 0.0, 0.0, 0.0, -10.0, 0.0, 0.0, 4.0, 0.0, 0.0, 21.0, 0.0, 0.0, -9.0, 0.0, 0.0, 23.0, 0.0, 0.0, -1.0, 0.0, 0.0, -16.0, 0.0, 0.0, 8.0, 0.0, 0.0, -19.0, 0.0, 0.0, 9.0, 0.0, 0.0, -22.0, 0.0, 0.0, 10.0, 0.0, 0.0, 27.0, 0.0, 0.0, -1.0, 0.0, 0.0, 16.0, 0.0, 0.0, -8.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=421,430 ) /
			 19.0, 0.0, 0.0, -8.0, 0.0, 0.0, 9.0, 0.0, 0.0, -4.0, 0.0, 0.0, -9.0, 0.0, 0.0, 4.0, 0.0, 0.0, -9.0, 0.0, 0.0, 4.0, 0.0, 0.0, -8.0, 0.0, 0.0, 4.0, 0.0, 0.0, 18.0, 0.0, 0.0, -9.0, 0.0, 0.0, 16.0, 0.0, 0.0, -1.0, 0.0, 0.0, -10.0, 0.0, 0.0, 4.0, 0.0, 0.0, -23.0, 0.0, 0.0, 9.0, 0.0, 0.0, 16.0, 0.0, 0.0, -1.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=431,440 ) /
			 -12.0, 0.0, 0.0, 6.0, 0.0, 0.0, -8.0, 0.0, 0.0, 4.0, 0.0, 0.0, 30.0, 0.0, 0.0, -2.0, 0.0, 0.0, 24.0, 0.0, 0.0, -10.0, 0.0, 0.0, 10.0, 0.0, 0.0, -4.0, 0.0, 0.0, -16.0, 0.0, 0.0, 7.0, 0.0, 0.0, -16.0, 0.0, 0.0, 7.0, 0.0, 0.0, 17.0, 0.0, 0.0, -7.0, 0.0, 0.0, -24.0, 0.0, 0.0, 10.0, 0.0, 0.0, -12.0, 0.0, 0.0, 5.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=441,450 ) /
			 -24.0, 0.0, 0.0, 11.0, 0.0, 0.0, -23.0, 0.0, 0.0, 9.0, 0.0, 0.0, -13.0, 0.0, 0.0, 5.0, 0.0, 0.0, -15.0, 0.0, 0.0, 7.0, 0.0, 0.0, 0.0, 0.0, -1988.0, 0.0, 0.0, -1679.0, 0.0, 0.0, -63.0, 0.0, 0.0, -27.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, 4.0, 5.0, 0.0, 0.0, -3.0, 0.0, 0.0, 0.0, 0.0, 364.0, 0.0, 0.0, 176.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=451,460 ) /
			 0.0, 0.0, -1044.0, 0.0, 0.0, -891.0, -3.0, 0.0, 0.0, 1.0, 0.0, 0.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, 330.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, -2.0, 0.0, 0.0, 3.0, 0.0, 0.0, -2.0, 0.0, 0.0, -3.0, 0.0, 0.0, 1.0, 0.0, 0.0, -5.0, 0.0, 0.0, 2.0, 0.0, 0.0, 3.0, 0.0, 0.0, -1.0, 0.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=461,470 ) /
			 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, -2.0, 0.0, 0.0, -7.0, 0.0, 0.0, 0.0, 0.0, 0.0, -12.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, -3.0, 0.0, 0.0, 3.0, 0.0, 0.0, -1.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=471,480 ) /
			 -5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, -7.0, 0.0, 0.0, 3.0, 0.0, 0.0, 7.0, 0.0, 0.0, -4.0, 0.0, 0.0, 0.0, 0.0, -12.0, 0.0, 0.0, -10.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, 3.0, 0.0, 0.0, -2.0, 0.0, 0.0, -3.0, 0.0, 0.0, 2.0, 0.0, 0.0, -7.0, 0.0, 0.0, 3.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=481,490 ) /
			 -3.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, 1.0, 0.0, 0.0, 7.0, 0.0, 0.0, -3.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, -5.0, 0.0, 0.0, 3.0, 0.0, 0.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, -5.0, 0.0, 0.0, 2.0, 0.0, 0.0, 5.0, 0.0, 0.0, -2.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=491,500 ) /
			 -8.0, 0.0, 0.0, 3.0, 0.0, 0.0, 9.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0, 0.0, 0.0, -3.0, 0.0, 0.0, -5.0, 0.0, 0.0, 2.0, 0.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, -7.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, 1.0, 0.0, 0.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, 2.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=501,510 ) /
			 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, 3.0, 0.0, 0.0, -1.0, 0.0, 0.0, -5.0, 0.0, 0.0, 2.0, 0.0, 0.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, 9.0, 0.0, 0.0, -3.0, 0.0, 0.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, -3.0, 0.0, 0.0, 2.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, 9.0, 0.0, 0.0, -3.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=511,520 ) /
			 -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, -2.0, 0.0, 0.0, 8.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, 2.0, 0.0, 0.0, 3.0, 0.0, 0.0, -1.0, 0.0, 0.0, 3.0, 0.0, 0.0, -1.0, 0.0, 0.0, -3.0, 0.0, 0.0, 1.0, 0.0, 0.0, 6.0, 0.0, 0.0, -3.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=521,530 ) /
			 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, 1.0, 0.0, 0.0, -7.0, 0.0, 0.0, 0.0, 0.0, 0.0, 9.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, 2.0, 0.0, 0.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, -5.0, 0.0, 0.0, 3.0, 0.0, 0.0, -13.0, 0.0, 0.0, 0.0, 0.0, 0.0, -7.0, 0.0, 0.0, 0.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=531,540 ) /
			 10.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, -1.0, 0.0, 0.0, 10.0, 0.0, 13.0, 6.0, 0.0, -5.0, 0.0, 0.0, 30.0, 0.0, 0.0, 14.0, 0.0, 0.0, -162.0, 0.0, 0.0, -138.0, 0.0, 0.0, 75.0, 0.0, 0.0, 0.0, -7.0, 0.0, 0.0, 4.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, 5.0, 0.0, 0.0, -2.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=541,550 ) /
			 5.0, 0.0, 0.0, -3.0, 0.0, 0.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, 2.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, -5.0, 0.0, 0.0, 2.0, 0.0, 0.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 9.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, -7.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, 1.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=551,560 ) /
			 -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, 7.0, 0.0, 0.0, 0.0, 0.0, 0.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, -6.0, 0.0, -3.0, 3.0, 0.0, 1.0, 0.0, 0.0, -3.0, 0.0, 0.0, -2.0, 11.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, -1.0, 0.0, 0.0, 11.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, 2.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=561,570 ) /
			 -1.0, 0.0, 3.0, 3.0, 0.0, -1.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, -13.0, 0.0, 0.0, -11.0, 3.0, 0.0, 6.0, 0.0, 0.0, 0.0, -7.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, -3.0, 0.0, 0.0, -3.0, 0.0, 0.0, 1.0, 0.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, -3.0, 0.0, 0.0, -7.0, 0.0, 0.0, 3.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=571,580 ) /
			 8.0, 0.0, 0.0, -3.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, 11.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, 1.0, 0.0, 0.0, 3.0, 0.0, 0.0, -1.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, 8.0, 0.0, 0.0, -4.0, 0.0, 0.0, 3.0, 0.0, 0.0, -1.0, 0.0, 0.0, 11.0, 0.0, 0.0, 0.0, 0.0, 0.0, -6.0, 0.0, 0.0, 3.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=581,590 ) /
			 -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, -8.0, 0.0, 0.0, 4.0, 0.0, 0.0, -7.0, 0.0, 0.0, 3.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, 3.0, 0.0, 0.0, -1.0, 0.0, 0.0, 6.0, 0.0, 0.0, -3.0, 0.0, 0.0, -6.0, 0.0, 0.0, 3.0, 0.0, 0.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0, 0.0, 0.0, -1.0, 0.0, 0.0, 5.0, 0.0, 0.0, -2.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=591,600 ) /
			 -5.0, 0.0, 0.0, 2.0, 0.0, 0.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0, 0.0, 0.0, -3.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, -26.0, 0.0, 0.0, -11.0, 0.0, 0.0, -10.0, 0.0, 0.0, -5.0, 5.0, 0.0, 0.0, -3.0, 0.0, 0.0, -13.0, 0.0, 0.0, 0.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=601,610 ) /
			 3.0, 0.0, 0.0, -2.0, 0.0, 0.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, 7.0, 0.0, 0.0, -3.0, 0.0, 0.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, 2.0, 0.0, 0.0, -6.0, 0.0, 0.0, 2.0, 0.0, 0.0, -5.0, 0.0, 0.0, 2.0, 0.0, 0.0, -7.0, 0.0, 0.0, 3.0, 0.0, 0.0, 5.0, 0.0, 0.0, -2.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=611,620 ) /
			 13.0, 0.0, 0.0, 0.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, -2.0, 0.0, 0.0, -11.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, -2.0, 0.0, 0.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, 6.0, 0.0, 0.0, -3.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=621,630 ) /
			 3.0, 0.0, 0.0, -2.0, 0.0, 0.0, -12.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, -1.0, 0.0, 0.0, -3.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, -5.0, 0.0, 0.0, -2.0, -7.0, 0.0, 0.0, 4.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=631,640 ) /
			 6.0, 0.0, 0.0, -3.0, 0.0, 0.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, -3.0, 0.0, 0.0, 3.0, 0.0, 0.0, -1.0, 0.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, 1.0, 0.0, 0.0, -5.0, 0.0, 0.0, 3.0, 0.0, 0.0, -3.0, 0.0, 0.0, 2.0, 0.0, 0.0, -3.0, 0.0, 0.0, 2.0, 0.0, 0.0, 12.0, 0.0, 0.0, 0.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=641,650 ) /
			 3.0, 0.0, 0.0, -1.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, -3.0, 0.0, 0.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, -6.0, 0.0, 0.0, 3.0, 0.0, 0.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, 6.0, 0.0, 0.0, -3.0, 0.0, 0.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=651,660 ) /
			 -6.0, 0.0, 0.0, 3.0, 0.0, 0.0, 3.0, 0.0, 0.0, -2.0, 0.0, 0.0, 7.0, 0.0, 0.0, -4.0, 0.0, 0.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, -5.0, 0.0, 0.0, 2.0, 0.0, 0.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, -6.0, 0.0, 0.0, 3.0, 0.0, 0.0, -6.0, 0.0, 0.0, 3.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, 10.0, 0.0, 0.0, 0.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=661,670 ) /
			 -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, 7.0, 0.0, 0.0, 0.0, 0.0, 0.0, 7.0, 0.0, 0.0, -3.0, 0.0, 0.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 11.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, -2.0, 0.0, 0.0, -6.0, 0.0, 0.0, 2.0, 0.0, 0.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, 3.0, 0.0, 0.0, -2.0, 0.0, 0.0, 5.0, 0.0, 0.0, -2.0, 0.0, 0.0,
			// DATA ( ( CLS(I,J) / I=1,6 ) / J=671,678 ) /
			 -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, -3.0, 0.0, 0.0, 2.0, 0.0, 0.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, 3.0, 0.0, 0.0, -1.0, 0.0, 0.0, -3.0, 0.0, 0.0, 1.0, 0.0, 0.0, -3.0, 0.0, 0.0, 1.0, 0.0, 0.0, -3.0, 0.0, 0.0, 2.0, 0.0, 0.0, };
}

final class IAU2000_NAPL
{
	/*
	 * Planetary argument multipliers L L' F D Om Me Ve E Ma Ju Sa Ur Ne pre
	 */
	static double[] NAPL =
	{ // DATA ( ( NAPL(I,J) / I=1,14 ) / J= 1, 10 ) /
			 0, 0, 0, 0, 0, 0, 0, 8, -16, 4, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -8, 16, -4, -5, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 8, -16, 4, 5, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 2, 2, 0, 0, 0, 0, 0, 0, 0, -4, 8, -1, -5, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 4, -8, 3, 0, 0, 0, 1, 0, 0, 1, -1, 1, 0, 0, 3, -8, 3, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 10, -3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, 6, -3, 0, 2, 0, 0, 0, 0, 0, 0, 0, 4, -8, 3, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J= 11, 20 ) /
			 0, 0, 1, -1, 1, 0, 0, -5, 8, -3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -4, 8, -3, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 4, -8, 1, 5, 0, 0, 2, 0, 0, 0, 0, 0, 0, -5, 6, 4, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, -5, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, -5, 0, 0, 1, 0, 0, 1, -1, 1, 0, 0, -1, 0, 2, -5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, -5, 0, 0, 0, 0, 0, 1, -1, 1, 0, 0, -1, 0, -2, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, 5, 0, 0, 1,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J= 21, 30 ) /
			 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, 5, 0, 0, 2, 2, 0, -1, -1, 0, 0, 0, 3, -7, 0, 0, 0, 0, 0, 1, 0, 0, -2, 0, 0, 19, -21, 3, 0, 0, 0, 0, 0, 0, 0, 1, -1, 1, 0, 2, -4, 0, -3, 0, 0, 0, 0, 1, 0, 0, -1, 1, 0, 0, -1, 0, 2, 0, 0, 0, 0, 0, 0, 1, -1, 1, 0, 0, -1, 0, -4, 10, 0, 0, 0, -2, 0, 0, 2, 1, 0, 0, 2, 0, 0, -5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, -7, 4, 0, 0, 0, 0, 0, 0, 0, -1, 1, 0, 0, 0, 1, 0, 1, -1, 0, 0, 0, -2, 0, 0, 2, 1, 0, 0, 2, 0, -2, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J= 31, 40 ) /
			 -1, 0, 0, 0, 0, 0, 18, -16, 0, 0, 0, 0, 0, 0, -2, 0, 1, 1, 2, 0, 0, 1, 0, -2, 0, 0, 0, 0, -1, 0, 1, -1, 1, 0, 18, -17, 0, 0, 0, 0, 0, 0, -1, 0, 0, 1, 1, 0, 0, 2, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -8, 13, 0, 0, 0, 0, 0, 2, 0, 0, 2, -2, 2, 0, -8, 11, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -8, 13, 0, 0, 0, 0, 0, 1, 0, 0, 1, -1, 1, 0, -8, 12, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, -13, 0, 0, 0, 0, 0, 0, 0, 0, 1, -1, 1, 0, 8, -14, 0, 0, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J= 41, 50 ) /
			 0, 0, 0, 0, 0, 0, 8, -13, 0, 0, 0, 0, 0, 1, -2, 0, 0, 2, 1, 0, 0, 2, 0, -4, 5, 0, 0, 0, -2, 0, 0, 2, 2, 0, 3, -3, 0, 0, 0, 0, 0, 0, -2, 0, 0, 2, 0, 0, 0, 2, 0, -3, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 3, -5, 0, 2, 0, 0, 0, 0, -2, 0, 0, 2, 0, 0, 0, 2, 0, -4, 3, 0, 0, 0, 0, 0, -1, 1, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, -1, 2, 0, 0, 0, 0, 0, 0, 0, 1, -1, 2, 0, 0, -2, 2, 0, 0, 0, 0, 0, -1, 0, 1, 0, 1, 0, 3, -5, 0, 0, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J= 51, 60 ) /
			 -1, 0, 0, 1, 0, 0, 3, -4, 0, 0, 0, 0, 0, 0, -2, 0, 0, 2, 0, 0, 0, 2, 0, -2, -2, 0, 0, 0, -2, 0, 2, 0, 2, 0, 0, -5, 9, 0, 0, 0, 0, 0, 0, 0, 1, -1, 1, 0, 0, -1, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, -1, 1, 0, 0, -1, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, -1, 0, 0, 1, 0, 0, 0, 3, -4, 0, 0, 0, 0, 0, 0, 0, -1, 1, 0, 0, 0, 1, 0, 0, 2, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J= 61, 70 ) /
			 0, 0, 1, -1, 2, 0, 0, -1, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, -9, 17, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, -3, 5, 0, 0, 0, 0, 0, 0, 0, 0, 1, -1, 1, 0, 0, -1, 0, -1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, -2, 0, 0, 0, 1, 0, 0, -2, 0, 0, 17, -16, 0, -2, 0, 0, 0, 0, 0, 0, 1, -1, 1, 0, 0, -1, 0, 1, -3, 0, 0, 0, -2, 0, 0, 2, 1, 0, 0, 5, -6, 0, 0, 0, 0, 0, 0, 0, -2, 2, 0, 0, 0, 9, -13, 0, 0, 0, 0, 0, 0, 0, 1, -1, 2, 0, 0, -1, 0, 0, 1, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J= 71, 80 ) /
			 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, -1, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, -2, 2, 0, 0, 5, -6, 0, 0, 0, 0, 0, 0, 0, 0, -1, 1, 1, 0, 5, -7, 0, 0, 0, 0, 0, 0, -2, 0, 0, 2, 0, 0, 6, -8, 0, 0, 0, 0, 0, 0, 2, 0, 1, -3, 1, 0, -6, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, -1, 1, 1, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1, -1, 1, 0, 0, -1, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 1,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J= 81, 90 ) /
			 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 2, 0, 0, 0, 0, 0, 0, 0, -8, 15, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, -8, 15, 0, 0, 0, 0, 1, 0, 0, 1, -1, 1, 0, 0, -9, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, -15, 0, 0, 0, 0, 0, 1, 0, -1, -1, 0, 0, 0, 8, -15, 0, 0, 0, 0, 0, 2, 0, 0, -2, 0, 0, 2, -5, 0, 0, 0, 0, 0, 0, -2, 0, 0, 2, 0, 0, 0, 2, 0, -5, 5, 0, 0, 0, 2, 0, 0, -2, 1, 0, 0, -6, 8, 0, 0, 0, 0, 0, 2, 0, 0, -2, 1, 0, 0, -2, 0, 3, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J= 91,100 ) /
			 -2, 0, 1, 1, 0, 0, 0, 1, 0, -3, 0, 0, 0, 0, -2, 0, 1, 1, 1, 0, 0, 1, 0, -3, 0, 0, 0, 0, -2, 0, 0, 2, 0, 0, 0, 2, 0, -3, 0, 0, 0, 0, -2, 0, 0, 2, 0, 0, 0, 6, -8, 0, 0, 0, 0, 0, -2, 0, 0, 2, 0, 0, 0, 2, 0, -1, -5, 0, 0, 0, -1, 0, 0, 1, 0, 0, 0, 1, 0, -1, 0, 0, 0, 0, -1, 0, 1, 1, 1, 0, -20, 20, 0, 0, 0, 0, 0, 0, 1, 0, 0, -2, 0, 0, 20, -21, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 8, -15, 0, 0, 0, 0, 0, 0, 0, 2, -2, 1, 0, 0, -10, 15, 0, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=101,110 ) /
			 0, 0, -1, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, -1, 2, 0, 0, -1, 0, 1, 0, 0, 0, 0, 0, 0, 1, -1, 1, 0, 0, -1, 0, -2, 4, 0, 0, 0, 2, 0, 0, -2, 1, 0, -6, 8, 0, 0, 0, 0, 0, 0, 0, 0, -2, 2, 1, 0, 5, -6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 1, 0, 0, 1, -1, 1, 0, 0, -1, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, -1, 1, 0, 0, -1, 0, 0, 1, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=111,120 ) /
			 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 2, 0, 0, 2, -2, 1, 0, 0, -9, 13, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 7, -13, 0, 0, 0, 0, 0, -2, 0, 0, 2, 0, 0, 0, 5, -6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, -17, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -9, 17, 0, 0, 0, 0, 2, 1, 0, 0, -1, 1, 0, 0, -3, 4, 0, 0, 0, 0, 0, 1, 0, 0, -1, 1, 0, -3, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, -1, 2, 0, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=121,130 ) /
			 0, 0, -1, 1, 1, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, -2, 2, 0, 1, 0, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, -5, 0, 2, 0, 0, 0, 0, -2, 0, 0, 2, 1, 0, 0, 2, 0, -3, 1, 0, 0, 0, -2, 0, 0, 2, 1, 0, 3, -3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 8, -13, 0, 0, 0, 0, 0, 0, 0, 0, -1, 1, 0, 0, 8, -12, 0, 0, 0, 0, 0, 0, 0, 0, 2, -2, 1, 0, -8, 11, 0, 0, 0, 0, 0, 0, -1, 0, 0, 1, 0, 0, 0, 2, -2, 0, 0, 0, 0, 0, -1, 0, 0, 0, 1, 0, 18, -16, 0, 0, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=131,140 ) /
			 0, 0, 1, -1, 1, 0, 0, -1, 0, -1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 3, -7, 4, 0, 0, 0, 0, 0, -2, 0, 1, 1, 1, 0, 0, -3, 7, 0, 0, 0, 0, 0, 0, 0, 1, -1, 2, 0, 0, -1, 0, -2, 5, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, -2, 5, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, -4, 8, -3, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, -10, 3, 0, 0, 0, 0, 0, 0, 0, 0, 2, -2, 1, 0, 0, -2, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 1, 0, 10, -3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 4, -8, 3, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=141,150 ) /
			 0, 0, 0, 0, 1, 0, 0, 0, 0, 2, -5, 0, 0, 0, 0, 0, -1, 1, 0, 0, 0, 1, 0, 2, -5, 0, 0, 0, 2, 0, -1, -1, 1, 0, 0, 3, -7, 0, 0, 0, 0, 0, -2, 0, 0, 2, 0, 0, 0, 2, 0, 0, -5, 0, 0, 0, 0, 0, 0, 0, 1, 0, -3, 7, -4, 0, 0, 0, 0, 0, -2, 0, 0, 2, 0, 0, 0, 2, 0, -2, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, -18, 16, 0, 0, 0, 0, 0, 0, -2, 0, 1, 1, 1, 0, 0, 1, 0, -2, 0, 0, 0, 0, 0, 0, 1, -1, 2, 0, -8, 12, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, -8, 13, 0, 0, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=151,160 ) /
			 0, 0, 0, 0, 0, 0, 0, 1, -2, 0, 0, 0, 0, 1, 0, 0, 1, -1, 1, 0, 0, 0, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, -2, 0, 0, 0, 0, 0, 0, 0, 1, -1, 1, 0, 0, -2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 2, 0, 0, 0, 0, 1, -1, 0, 0, 1, 1, 0, 3, -4, 0, 0, 0, 0, 0, 0, -1, 0, 0, 1, 1, 0, 0, 3, -4, 0, 0, 0, 0, 0, 0, 0, 1, -1, 1, 0, 0, -1, 0, 0, -2, 0, 0, 0, 0, 0, 1, -1, 1, 0, 0, -1, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 1,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=161,170 ) /
			 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 2, 0, 0, 1, -1, 0, 0, 3, -6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, -3, 5, 0, 0, 0, 0, 0, 0, 0, 0, 1, -1, 2, 0, -3, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, -2, 4, 0, 0, 0, 0, 0, 0, 0, 2, -2, 1, 0, -5, 6, 0, 0, 0, 0, 0, 0, 0, 0, -1, 1, 0, 0, 5, -7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 5, -8, 0, 0, 0, 0, 0, 0, -2, 0, 0, 2, 1, 0, 6, -8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, -8, 15, 0, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=171,180 ) /
			 -2, 0, 0, 2, 1, 0, 0, 2, 0, -3, 0, 0, 0, 0, -2, 0, 0, 2, 1, 0, 0, 6, -8, 0, 0, 0, 0, 0, 1, 0, 0, -1, 1, 0, 0, -1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, -5, 0, 0, 0, 0, 0, 1, -1, 1, 0, 0, -1, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, -1, 1, 0, 0, -1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=181,190 ) /
			 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 2, 0, 0, 1, -1, 2, 0, 0, -1, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, -1, 1, 0, 0, 0, 1, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -7, 13, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 7, -13, 0, 0, 0, 0, 0, 2, 0, 0, -2, 1, 0, 0, -5, 6, 0, 0, 0, 0, 0, 0, 0, 2, -2, 1, 0, 0, -8, 11, 0, 0, 0, 0, 0, 0, 0, 2, -2, 1, -1, 0, 2, 0, 0, 0, 0, 0, 0, -2, 0, 0, 2, 0, 0, 0, 4, -4, 0, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=191,200 ) /
			 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, -2, 0, 0, 0, 0, 0, 1, -1, 1, 0, 0, -1, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 2, -2, 0, 0, 2, 0, 0, 3, -3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, -4, 8, -3, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 4, -8, 3, 0, 0, 0, 0, 2, 0, 0, -2, 1, 0, 0, -2, 0, 2, 0, 0, 0, 0, 0, 0, 1, -1, 2, 0, 0, -1, 0, 2, 0, 0, 0, 0, 0, 0, 1, -1, 2, 0, 0, 0, -2, 0, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=201,210 ) /
			 0, 0, 0, 0, 1, 0, 0, 1, -2, 0, 0, 0, 0, 0, 0, 0, -1, 1, 0, 0, 0, 2, -2, 0, 0, 0, 0, 0, 0, 0, -1, 1, 0, 0, 0, 1, 0, 0, -2, 0, 0, 0, 0, 0, 2, -2, 1, 0, 0, -2, 0, 0, 2, 0, 0, 0, 0, 0, 1, -1, 1, 0, 3, -6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, -5, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 3, -5, 0, 0, 0, 0, 0, 0, 0, 0, 1, -1, 1, 0, -3, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -3, 5, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, -3, 5, 0, 0, 0, 0, 0, 2,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=211,220 ) /
			 0, 0, 2, -2, 2, 0, -3, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -3, 5, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 2, -4, 0, 0, 0, 0, 1, 0, 0, 1, -1, 1, 0, 0, 1, -4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, -4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, 4, 0, 0, 0, 0, 1, 0, 0, 1, -1, 1, 0, 0, -3, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, 4, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, -2, 4, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -5, 8, 0, 0, 0, 0, 0, 2,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=221,230 ) /
			 0, 0, 2, -2, 2, 0, -5, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -5, 8, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -5, 8, 0, 0, 0, 0, 0, 1, 0, 0, 1, -1, 1, 0, -5, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -5, 8, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 5, -8, 0, 0, 0, 0, 0, 0, 0, 0, 1, -1, 2, 0, 0, -1, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, -1, 1, 0, 0, 0, 1, 0, -1, 0, 0, 0, 0, 0, 0, 2, -2, 1, 0, 0, -2, 0, 1, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=231,240 ) /
			 0, 0, 0, 0, 0, 0, 0, -6, 11, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 6, -11, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 4, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 1, 0, -4, 0, 0, 0, 0, 0, 0, 2, 0, 0, -2, 1, 0, -3, 3, 0, 0, 0, 0, 0, 0, -2, 0, 0, 2, 0, 0, 0, 2, 0, 0, -2, 0, 0, 0, 0, 0, 2, -2, 1, 0, 0, -7, 9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, -5, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 1,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=241,250 ) /
			 0, 0, 1, -1, 1, 0, 0, -1, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 2, 0, 0, 2, -2, 2, 0, 0, -2, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 2, 0, 0, 0, 0, 1, 0, 3, -5, 0, 0, 0, 0, 0, 0, 0, 0, -1, 1, 0, 0, 3, -4, 0, 0, 0, 0, 0, 0, 0, 0, 2, -2, 1, 0, -3, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 2, -4, 0, 0, 0, 0, 0, 0, 0, 2, -2, 1, 0, 0, -4, 4, 0, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=251,260 ) /
			 0, 0, 1, -1, 2, 0, -5, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, -6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -3, 6, 0, 0, 0, 0, 1, 0, 0, 1, -1, 1, 0, 0, -4, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -3, 6, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, -3, 6, 0, 0, 0, 0, 2, 0, 0, -1, 1, 0, 0, 2, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 2, -3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -5, 9, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, -5, 9, 0, 0, 0, 0, 1,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=261,270 ) /
			 0, 0, 0, 0, 0, 0, 0, 5, -9, 0, 0, 0, 0, 0, 0, 0, -1, 1, 0, 0, 0, 1, 0, -2, 0, 0, 0, 0, 0, 0, 2, -2, 1, 0, 0, -2, 0, 2, 0, 0, 0, 0, -2, 0, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, -2, 2, 0, 0, 3, -3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -6, 10, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, -6, 10, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -2, 3, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -2, 3, 0, 0, 0, 0, 0, 1, 0, 0, 1, -1, 1, 0, -2, 2, 0, 0, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=271,280 ) /
			 0, 0, 0, 0, 0, 0, 2, -3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, -3, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 1, 0, 0, 1, -1, 1, 0, 0, -1, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 4, -8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -4, 8, 0, 0, 0, 0, 2, 0, 0, -2, 2, 0, 0, 0, 2, 0, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -4, 7, 0, 0, 0, 0, 2,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=281,290 ) /
			 0, 0, 0, 0, 0, 0, 0, -4, 7, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 4, -7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, -2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 2, -2, 1, 0, 0, -2, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -5, 10, 0, 0, 0, 0, 2, 0, 0, 0, 0, 1, 0, -1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, -3, 5, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, -3, 5, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 3, -5, 0, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=291,300 ) /
			 0, 0, 0, 0, 0, 0, 1, -2, 0, 0, 0, 0, 0, 1, 0, 0, 1, -1, 1, 0, 1, -3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 2, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, -1, 2, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -7, 11, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -7, 11, 0, 0, 0, 0, 0, 1, 0, 0, -2, 2, 0, 0, 4, -4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, -3, 0, 0, 0, 0, 0, 0, 0, 2, -2, 1, 0, -4, 4, 0, 0, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=301,310 ) /
			 0, 0, -1, 1, 0, 0, 4, -5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -4, 7, 0, 0, 0, 0, 0, 1, 0, 0, 1, -1, 1, 0, -4, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -4, 7, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -4, 6, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -4, 6, 0, 0, 0, 0, 0, 1, 0, 0, 1, -1, 1, 0, -4, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -4, 6, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 4, -6, 0, 0, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=311,320 ) /
			 -2, 0, 0, 2, 0, 0, 2, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, -1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 5, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 1, -3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 3, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, -7, 12, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -1, 1, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -1, 1, 0, 0, 0, 0, 0, 1,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=321,330 ) /
			 0, 0, 1, -1, 1, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, -1, 0, 0, 0, 0, 0, 1, 0, 0, 1, -1, 1, 0, 1, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, 5, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, -1, 0, 4, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 1, 0, -4, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, -1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -6, 10, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, -6, 10, 0, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=331,340 ) /
			 0, 0, 2, -2, 1, 0, 0, -3, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -3, 7, 0, 0, 0, 0, 2, -2, 0, 0, 2, 0, 0, 4, -4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -5, 8, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 5, -8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 3, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, -1, 0, 3, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, -3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, -4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, 4, 0, 0, 0, 0, 0, 1,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=341,350 ) /
			 0, 0, 1, -1, 1, 0, -2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, 4, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -6, 9, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -6, 9, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 6, -9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, -2, 0, 0, 0, 0, 0, 0, 2, -2, 1, 0, -2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -4, 6, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 4, -6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 3, -4, 0, 0, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=351,360 ) /
			 0, 0, 0, 0, 0, 0, 0, -1, 0, 2, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 1, 0, -2, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -5, 9, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 3, -4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -3, 4, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -3, 4, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 3, -4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, -4, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 2, -2, 0, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=361,370 ) /
			 0, 0, 0, 0, 1, 0, 0, -1, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, -3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, -5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, -1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, -3, 5, 0, 0, 0, 0, 0, 0, 0, 1, 0, -3, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, -2, 0, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=371,380 ) /
			 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, -1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, -2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -8, 14, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 1, 0, 2, -5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, -8, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, -8, 3, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, -8, 3, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=381,390 ) /
			 0, 0, 0, 0, 0, 0, 0, -3, 8, -3, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 1, 0, -2, 5, 0, 0, 2, 0, 0, 0, 0, 0, 0, -8, 12, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -8, 12, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 2, 0, 0, 2, 0, 0, 2, -2, 1, 0, -5, 5, 0, 0, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=391,400 ) /
			 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 3, -6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -3, 6, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, -3, 6, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, -1, 4, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -5, 7, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -5, 7, 0, 0, 0, 0, 0, 1, 0, 0, 1, -1, 1, 0, -5, 6, 0, 0, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=401,410 ) /
			 0, 0, 0, 0, 0, 0, 5, -7, 0, 0, 0, 0, 0, 0, 0, 0, 2, -2, 1, 0, 0, -1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 3, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 1, 0, 2, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, -2, 6, 0, 0, 0, 0, 2, 0, 0, 0, 0, 1, 0, 2, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -6, 9, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 6, -9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, 2, 0, 0, 0, 0, 0, 1,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=411,420 ) /
			 0, 0, 1, -1, 1, 0, -2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, -2, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 3, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, -5, 7, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 5, -7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, -2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, -5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, -3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 3, 0, 0, 0, 0, 0, 1,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=421,430 ) /
			 0, 0, 1, -1, 1, 0, -1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 3, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -7, 10, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -7, 10, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 3, -3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -4, 8, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -4, 5, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -4, 5, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 4, -5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 2,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=431,440 ) /
			 0, 0, 0, 0, 0, 0, 0, -2, 0, 5, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -9, 13, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, -1, 5, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, -2, 0, 4, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 2, 0, -4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, 7, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 2, 0, -3, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=441,450 ) /
			 0, 0, 0, 0, 0, 0, -2, 5, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, -2, 5, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -6, 8, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -6, 8, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 6, -8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 2, 0, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -3, 9, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 5, -6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, -6, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 2, 0, -2, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=451,460 ) /
			 0, 0, 0, 0, 0, 0, 0, 2, 0, -2, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 2, 0, -2, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -5, 10, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 4, -4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, -4, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -3, 3, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 3, -3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, -3, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 3, -3, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, -3, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=461,470 ) /
			 0, 0, 0, 0, 0, 0, 0, -5, 13, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 2, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, -1, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, -2, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 3, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, -2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, -1, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, -6, 15, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -8, 15, 0, 0, 0, 0, 0, 2,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=471,480 ) /
			 0, 0, 0, 0, 0, 0, -3, 9, -4, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 2, 0, 2, -5, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, -2, 8, -1, -5, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 6, -8, 3, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 1, 0, 0, 1, -1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 2,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=481,490 ) /
			 0, 0, 0, 0, 0, 0, 0, -6, 16, -4, -5, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, -2, 8, -3, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, -2, 8, -3, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 6, -8, 1, 5, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 2, 0, -2, 5, 0, 0, 2, 0, 0, 0, 0, 0, 0, 3, -5, 4, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -8, 11, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -8, 11, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, -8, 11, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 11, 0, 0, 0, 0, 0, 2,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=491,500 ) /
			 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 1, 0, 0, 2, 0, 0, 0, 0, 0, 0, 3, -3, 0, 2, 0, 0, 0, 2, 0, 0, 2, -2, 1, 0, 0, 4, -8, 3, 0, 0, 0, 0, 0, 0, 1, -1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 2, -2, 1, 0, 0, -4, 8, -3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 2, 0, 1, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -3, 7, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -5, 6, 0, 0, 0, 0, 0, 2,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=501,510 ) /
			 0, 0, 0, 0, 0, 0, -5, 6, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 5, -6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, -6, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 2, 0, 2, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, -1, 6, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 7, -9, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 2, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, -1, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 6, -7, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 5, -5, 0, 0, 0, 0, 2,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=511,520 ) /
			 0, 0, 0, 0, 0, 0, -1, 4, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, -1, 4, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -7, 9, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -7, 9, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 4, -3, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 3, -1, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -4, 4, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 4, -4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, -4, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 4, -4, 0, 0, 0, 0, 0, 2,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=521,530 ) /
			 0, 0, 0, 0, 0, 0, 0, 2, 1, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, -3, 0, 5, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -9, 12, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 3, 0, -4, 0, 0, 0, 0, 0, 0, 2, -2, 1, 0, 1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7, -8, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 3, 0, -3, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=531,540 ) /
			 0, 0, 0, 0, 0, 0, 0, 3, 0, -3, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -2, 6, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -6, 7, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 6, -7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, -6, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 3, 0, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, -2, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 5, -4, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 3, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, -2, 0, 0, 0, 0, 0, 2,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=541,550 ) /
			 0, 0, 0, 0, 0, 0, 0, 3, 0, -1, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 3, 0, -1, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, -2, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 4, -2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, -1, 0, 0, 2, 0, 0, 2, -2, 1, 0, 0, 1, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -8, 16, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 3, 0, 2, -5, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 7, -8, 3, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, -5, 16, -4, -5, 0, 0, 2,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=551,560 ) /
			 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, -1, 8, -3, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -8, 10, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -8, 10, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, -8, 10, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 2, 2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 3, 0, 1, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -3, 8, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -5, 5, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 5, -5, 0, 0, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=561,570 ) /
			 0, 0, 0, 0, 0, 0, 5, -5, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 5, -5, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 7, -7, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 7, -7, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 6, -5, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 7, -8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, -3, 0, 0, 0, 0, 2,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=571,580 ) /
			 0, 0, 0, 0, 0, 0, 4, -3, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 1, 2, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -9, 11, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -9, 11, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 4, 0, -4, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 4, 0, -3, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -6, 6, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 6, -6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, -6, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 4, 0, -2, 0, 0, 0, 2,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=581,590 ) /
			 0, 0, 0, 0, 0, 0, 0, 6, -4, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 3, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, -1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 3, -1, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 4, 0, -1, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 4, 0, 0, -2, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 5, -2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, -9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, -4, 0, 0, 0, 0, 0, 2,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=591,600 ) /
			 0, 0, 0, 0, 0, 0, 2, 1, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 2, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 2, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, -7, 7, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 7, -7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, -2, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 4, -2, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 4, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, 0, -4, 0, 0, 0, 2,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=601,610 ) /
			 0, 0, 0, 0, 0, 0, 0, 5, 0, -3, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 5, 0, -2, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -8, 8, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 8, -8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, -3, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 5, -3, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, -9, 9, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, -9, 9, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, -9, 9, 0, 0, 0, 0, 0, 1,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=611,620 ) /
			 0, 0, 0, 0, 0, 0, 9, -9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, -4, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0, 2,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=621,630 ) /
			 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 1, 0, 0, -2, 0, 0, 0, 2, 0, -2, 0, 0, 0, 0, 1, 0, 0, -2, 0, 0, 2, -2, 0, 0, 0, 0, 0, 0, 1, 0, 0, -2, 0, 0, 0, 1, 0, -1, 0, 0, 0, 0, 1, 0, 0, -2, 0, 0, 1, -1, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 3, -3, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 2, 0, -2, 0, 0, 0, 0, -1, 0, 0, 2, 0, 0, 0, 4, -8, 3, 0, 0, 0, 0, 1, 0, 0, -2, 0, 0, 0, 4, -8, 3, 0, 0, 0, 0, -2, 0, 0, 2, 0, 0, 0, 4, -8, 3, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=631,640 ) /
			 -1, 0, 0, 0, 0, 0, 0, 2, 0, -3, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 1, 0, -1, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 1, -1, 0, 0, 0, 0, 0, 0, -1, 0, 0, 2, 0, 0, 2, -2, 0, 0, 0, 0, 0, 0, 1, 0, -1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, -1, 0, 0, 2, 0, 0, 0, 2, 0, -3, 0, 0, 0, 0, -2, 0, 0, 0, 0, 0, 0, 2, 0, -3, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 4, -8, 3, 0, 0, 0, 0, -1, 0, 1, -1, 1, 0, 0, -1, 0, 0, 0, 0, 0, 0, 1, 0, 1, -1, 1, 0, 0, -1, 0, 0, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=641,650 ) /
			 -1, 0, 0, 0, 0, 0, 0, 4, -8, 3, 0, 0, 0, 0, -1, 0, 0, 2, 1, 0, 0, 2, 0, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, -2, 0, 0, 0, 0, -1, 0, 0, 2, 0, 0, 0, 2, 0, -2, 0, 0, 0, 0, -1, 0, 0, 2, 0, 0, 3, -3, 0, 0, 0, 0, 0, 0, 1, 0, 0, -2, 1, 0, 0, -2, 0, 2, 0, 0, 0, 0, 1, 0, 2, -2, 2, 0, -3, 3, 0, 0, 0, 0, 0, 0, 1, 0, 2, -2, 2, 0, 0, -2, 0, 2, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, -1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, -1, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=651,660 ) /
			 0, 0, 0, -2, 0, 0, 2, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, 0, 0, 0, 1, 0, -1, 0, 0, 0, 0, 0, 0, 2, 0, 2, 0, -2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 2, 0, 0, -1, 0, 1, 0, 0, 0, 0, 0, 0, 2, 0, 2, 0, -1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 2, 0, -2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 2, 0, -2, 0, 0, 0, 0, 0, 0, 1, 1, 2, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 2, 0, 2, 0, 0, 1, 0, 0, 0, 0, 0, 0, -1, 0, 2, 0, 2, 0, 10, -3, 0, 0, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=661,670 ) /
			 0, 0, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 2, 0, 2, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 2, 0, 0, 4, -8, 3, 0, 0, 0, 0, 0, 0, 2, 0, 2, 0, 0, -4, 8, -3, 0, 0, 0, 0, -1, 0, 2, 0, 2, 0, 0, -4, 8, -3, 0, 0, 0, 0, 2, 0, 2, -2, 2, 0, 0, -2, 0, 3, 0, 0, 0, 0, 1, 0, 2, 0, 1, 0, 0, -2, 0, 3, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, -1, 0, 2, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, -2, 0, 2, 2, 2, 0, 0, 2, 0, -2, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=671,680 ) /
			 0, 0, 2, 0, 2, 0, 2, -3, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 2, 0, 1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 2, 0, 0, 1, 0, -1, 0, 0, 0, 0, 0, 0, 2, 0, 2, 0, 2, -2, 0, 0, 0, 0, 0, 0, -1, 0, 2, 2, 2, 0, 0, -1, 0, 1, 0, 0, 0, 0, 1, 0, 2, 0, 2, 0, -1, 1, 0, 0, 0, 0, 0, 0, -1, 0, 2, 2, 2, 0, 0, 2, 0, -3, 0, 0, 0, 0, 2, 0, 2, 0, 2, 0, 0, 2, 0, -3, 0, 0, 0, 0, 1, 0, 2, 0, 2, 0, 0, -4, 8, -3, 0, 0, 0, 0, 1, 0, 2, 0, 2, 0, 0, 4, -8, 3, 0, 0, 0, 0,
			// DATA ( ( NAPL(I,J) / I=1,14 ) / J=681,687 ) /
			 1, 0, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 2, 0, 0, 1, 0, 0, 0, 0, 0, 0, 2, 0, 2, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, -1, 0, 2, 2, 2, 0, 0, 2, 0, -2, 0, 0, 0, 0, -1, 0, 2, 2, 2, 0, 3, -3, 0, 0, 0, 0, 0, 0, 1, 0, 2, 0, 2, 0, 1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 0, 0, 2, 0, -2, 0, 0, 0, 0, };
}

final class IAU2000_ICPL
{
	/*
	 * Planetary nutation coefficients, unit 1e-7 arcsec longitude (sin, cos) /
	 * obliquity (sin, cos)
	 */
	static double[] ICPL =
	{ // DATA ( ( ICPL(I,J) / I=1,4 ) / J= 1, 10 ) /
			 1440, 0, 0, 0, 56, -117, -42, -40, 125, -43, 0, -54, 0, 5, 0, 0, 3, -7, -3, 0, 3, 0, 0, -2, -114, 0, 0, 61, -219, 89, 0, 0, -3, 0, 0, 0, -462, 1604, 0, 0,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J= 11, 20 ) /
			 99, 0, 0, -53, -3, 0, 0, 2, 0, 6, 2, 0, 3, 0, 0, 0, -12, 0, 0, 0, 14, -218, 117, 8, 31, -481, -257, -17, -491, 128, 0, 0, -3084, 5123, 2735, 1647, -1444, 2409, -1286, -771,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J= 21, 30 ) /
			 11, -24, -11, -9, 26, -9, 0, 0, 103, -60, 0, 0, 0, -13, -7, 0, -26, -29, -16, 14, 9, -27, -14, -5, 12, 0, 0, -6, -7, 0, 0, 0, 0, 24, 0, 0, 284, 0, 0, -151,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J= 31, 40 ) /
			 226, 101, 0, 0, 0, -8, -2, 0, 0, -6, -3, 0, 5, 0, 0, -3, -41, 175, 76, 17, 0, 15, 6, 0, 425, 212, -133, 269, 1200, 598, 319, -641, 235, 334, 0, 0, 11, -12, -7, -6,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J= 41, 50 ) /
			 5, -6, 3, 3, -5, 0, 0, 3, 6, 0, 0, -3, 15, 0, 0, 0, 13, 0, 0, -7, -6, -9, 0, 0, 266, -78, 0, 0, -460, -435, -232, 246, 0, 15, 7, 0, -3, 0, 0, 2,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J= 51, 60 ) /
			 0, 131, 0, 0, 4, 0, 0, 0, 0, 3, 0, 0, 0, 4, 2, 0, 0, 3, 0, 0, -17, -19, -10, 9, -9, -11, 6, -5, -6, 0, 0, 3, -16, 8, 0, 0, 0, 3, 0, 0,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J= 61, 70 ) /
			 11, 24, 11, -5, -3, -4, -2, 1, 3, 0, 0, -1, 0, -8, -4, 0, 0, 3, 0, 0, 0, 5, 0, 0, 0, 3, 2, 0, -6, 4, 2, 3, -3, -5, 0, 0, -5, 0, 0, 2,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J= 71, 80 ) /
			 4, 24, 13, -2, -42, 20, 0, 0, -10, 233, 0, 0, -3, 0, 0, 1, 78, -18, 0, 0, 0, 3, 1, 0, 0, -3, -1, 0, 0, -4, -2, 1, 0, -8, -4, -1, 0, -5, 3, 0,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J= 81, 90 ) /
			 -7, 0, 0, 3, -14, 8, 3, 6, 0, 8, -4, 0, 0, 19, 10, 0, 45, -22, 0, 0, -3, 0, 0, 0, 0, -3, 0, 0, 0, 3, 0, 0, 3, 5, 3, -2, 89, -16, -9, -48,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J= 91,100 ) /
			 0, 3, 0, 0, -3, 7, 4, 2, -349, -62, 0, 0, -15, 22, 0, 0, -3, 0, 0, 0, -53, 0, 0, 0, 5, 0, 0, -3, 0, -8, 0, 0, 15, -7, -4, -8, -3, 0, 0, 1,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=101,110 ) /
			 -21, -78, 0, 0, 20, -70, -37, -11, 0, 6, 3, 0, 5, 3, 2, -2, -17, -4, -2, 9, 0, 6, 3, 0, 32, 15, -8, 17, 174, 84, 45, -93, 11, 56, 0, 0, -66, -12, -6, 35,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=111,120 ) /
			 47, 8, 4, -25, 0, 8, 4, 0, 10, -22, -12, -5, -3, 0, 0, 2, -24, 12, 0, 0, 5, -6, 0, 0, 3, 0, 0, -2, 4, 3, 1, -2, 0, 29, 15, 0, -5, -4, -2, 2,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=121,130 ) /
			 8, -3, -1, -5, 0, -3, 0, 0, 10, 0, 0, 0, 3, 0, 0, -2, -5, 0, 0, 3, 46, 66, 35, -25, -14, 7, 0, 0, 0, 3, 2, 0, -5, 0, 0, 0, -68, -34, -18, 36,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=131,140 ) /
			 0, 14, 7, 0, 10, -6, -3, -5, -5, -4, -2, 3, -3, 5, 2, 1, 76, 17, 9, -41, 84, 298, 159, -45, 3, 0, 0, -1, -3, 0, 0, 2, -3, 0, 0, 1, -82, 292, 156, 44,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=141,150 ) /
			 -73, 17, 9, 39, -9, -16, 0, 0, 3, 0, -1, -2, -3, 0, 0, 0, -9, -5, -3, 5, -439, 0, 0, 0, 57, -28, -15, -30, 0, -6, -3, 0, -4, 0, 0, 2, -40, 57, 30, 21,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=151,160 ) /
			 23, 7, 3, -13, 273, 80, 43, -146, -449, 430, 0, 0, -8, -47, -25, 4, 6, 47, 25, -3, 0, 23, 13, 0, -3, 0, 0, 2, 3, -4, -2, -2, -48, -110, -59, 26, 51, 114, 61, -27,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=161,170 ) /
			 -133, 0, 0, 57, 0, 4, 0, 0, -21, -6, -3, 11, 0, -3, -1, 0, -11, -21, -11, 6, -18, -436, -233, 9, 35, -7, 0, 0, 0, 5, 3, 0, 11, -3, -1, -6, -5, -3, -1, 3,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=171,180 ) /
			 -53, -9, -5, 28, 0, 3, 2, 1, 4, 0, 0, -2, 0, -4, 0, 0, -50, 194, 103, 27, -13, 52, 28, 7, -91, 248, 0, 0, 6, 49, 26, -3, -6, -47, -25, 3, 0, 5, 3, 0,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=181,190 ) /
			 52, 23, 10, -23, -3, 0, 0, 1, 0, 5, 3, 0, -4, 0, 0, 0, -4, 8, 3, 2, 10, 0, 0, 0, 3, 0, 0, -2, 0, 8, 4, 0, 0, 8, 4, 1, -4, 0, 0, 0,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=191,200 ) /
			 -4, 0, 0, 0, -8, 4, 2, 4, 8, -4, -2, -4, 0, 15, 7, 0, -138, 0, 0, 0, 0, -7, -3, 0, 0, -7, -3, 0, 54, 0, 0, -29, 0, 10, 4, 0, -7, 0, 0, 3,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=201,210 ) /
			 -37, 35, 19, 20, 0, 4, 0, 0, -4, 9, 0, 0, 8, 0, 0, -4, -9, -14, -8, 5, -3, -9, -5, 3, -145, 47, 0, 0, -10, 40, 21, 5, 11, -49, -26, -7, -2150, 0, 0, 932,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=211,220 ) /
			 -12, 0, 0, 5, 85, 0, 0, -37, 4, 0, 0, -2, 3, 0, 0, -2, -86, 153, 0, 0, -6, 9, 5, 3, 9, -13, -7, -5, -8, 12, 6, 4, -51, 0, 0, 22, -11, -268, -116, 5,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=221,230 ) /
			 0, 12, 5, 0, 0, 7, 3, 0, 31, 6, 3, -17, 140, 27, 14, -75, 57, 11, 6, -30, -14, -39, 0, 0, 0, -6, -2, 0, 4, 15, 8, -2, 0, 4, 0, 0, -3, 0, 0, 1,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=231,240 ) /
			 0, 11, 5, 0, 9, 6, 0, 0, -4, 10, 4, 2, 5, 3, 0, 0, 16, 0, 0, -9, -3, 0, 0, 0, 0, 3, 2, -1, 7, 0, 0, -3, -25, 22, 0, 0, 42, 223, 119, -22,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=241,250 ) /
			 -27, -143, -77, 14, 9, 49, 26, -5, -1166, 0, 0, 505, -5, 0, 0, 2, -6, 0, 0, 3, -8, 0, 1, 4, 0, -4, 0, 0, 117, 0, 0, -63, -4, 8, 4, 2, 3, 0, 0, -2,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=251,260 ) /
			 -5, 0, 0, 2, 0, 31, 0, 0, -5, 0, 1, 3, 4, 0, 0, -2, -4, 0, 0, 2, -24, -13, -6, 10, 3, 0, 0, 0, 0, -32, -17, 0, 8, 12, 5, -3, 3, 0, 0, -1,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=261,270 ) /
			 7, 13, 0, 0, -3, 16, 0, 0, 50, 0, 0, -27, 0, -5, -3, 0, 13, 0, 0, 0, 0, 5, 3, 1, 24, 5, 2, -11, 5, -11, -5, -2, 30, -3, -2, -16, 18, 0, 0, -9,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=271,280 ) /
			 8, 614, 0, 0, 3, -3, -1, -2, 6, 17, 9, -3, -3, -9, -5, 2, 0, 6, 3, -1, -127, 21, 9, 55, 3, 5, 0, 0, -6, -10, -4, 3, 5, 0, 0, 0, 16, 9, 4, -7,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=281,290 ) /
			 3, 0, 0, -2, 0, 22, 0, 0, 0, 19, 10, 0, 7, 0, 0, -4, 0, -5, -2, 0, 0, 3, 1, 0, -9, 3, 1, 4, 17, 0, 0, -7, 0, -3, -2, -1, -20, 34, 0, 0,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=291,300 ) /
			 -10, 0, 1, 5, -4, 0, 0, 2, 22, -87, 0, 0, -4, 0, 0, 2, -3, -6, -2, 1, -16, -3, -1, 7, 0, -3, -2, 0, 4, 0, 0, 0, -68, 39, 0, 0, 27, 0, 0, -14,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=301,310 ) /
			 0, -4, 0, 0, -25, 0, 0, 0, -12, -3, -2, 6, 3, 0, 0, -1, 3, 66, 29, -1, 490, 0, 0, -213, -22, 93, 49, 12, -7, 28, 15, 4, -3, 13, 7, 2, -46, 14, 0, 0,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=311,320 ) /
			 -5, 0, 0, 0, 2, 1, 0, 0, 0, -3, 0, 0, -28, 0, 0, 15, 5, 0, 0, -2, 0, 3, 0, 0, -11, 0, 0, 5, 0, 3, 1, 0, -3, 0, 0, 1, 25, 106, 57, -13,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=321,330 ) /
			 5, 21, 11, -3, 1485, 0, 0, 0, -7, -32, -17, 4, 0, 5, 3, 0, -6, -3, -2, 3, 30, -6, -2, -13, -4, 4, 0, 0, -19, 0, 0, 10, 0, 4, 2, -1, 0, 3, 0, 0,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=331,340 ) /
			 4, 0, 0, -2, 0, -3, -1, 0, -3, 0, 0, 0, 5, 3, 1, -2, 0, 11, 0, 0, 118, 0, 0, -52, 0, -5, -3, 0, -28, 36, 0, 0, 5, -5, 0, 0, 14, -59, -31, -8,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=341,350 ) /
			 0, 9, 5, 1, -458, 0, 0, 198, 0, -45, -20, 0, 9, 0, 0, -5, 0, -3, 0, 0, 0, -4, -2, -1, 11, 0, 0, -6, 6, 0, 0, -2, -16, 23, 0, 0, 0, -4, -2, 0,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=351,360 ) /
			 -5, 0, 0, 2, -166, 269, 0, 0, 15, 0, 0, -8, 10, 0, 0, -4, -78, 45, 0, 0, 0, -5, -2, 0, 7, 0, 0, -4, -5, 328, 0, 0, 3, 0, 0, -2, 5, 0, 0, -2,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=361,370 ) /
			 0, 3, 1, 0, -3, 0, 0, 0, -3, 0, 0, 0, 0, -4, -2, 0, -1223, -26, 0, 0, 0, 7, 3, 0, 3, 0, 0, 0, 0, 3, 2, 0, -6, 20, 0, 0, -368, 0, 0, 0,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=371,380 ) /
			 -75, 0, 0, 0, 11, 0, 0, -6, 3, 0, 0, -2, -3, 0, 0, 1, -13, -30, 0, 0, 21, 3, 0, 0, -3, 0, 0, 1, -4, 0, 0, 2, 8, -27, 0, 0, -19, -11, 0, 0,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=381,390 ) /
			 -4, 0, 0, 2, 0, 5, 2, 0, -6, 0, 0, 2, -8, 0, 0, 0, -1, 0, 0, 0, -14, 0, 0, 6, 6, 0, 0, 0, -74, 0, 0, 32, 0, -3, -1, 0, 4, 0, 0, -2,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=391,400 ) /
			 8, 11, 0, 0, 0, 3, 2, 0, -262, 0, 0, 114, 0, -4, 0, 0, -7, 0, 0, 4, 0, -27, -12, 0, -19, -8, -4, 8, 202, 0, 0, -87, -8, 35, 19, 5, 0, 4, 2, 0,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=401,410 ) /
			 16, -5, 0, 0, 5, 0, 0, -3, 0, -3, 0, 0, 1, 0, 0, 0, -35, -48, -21, 15, -3, -5, -2, 1, 6, 0, 0, -3, 3, 0, 0, -1, 0, -5, 0, 0, 12, 55, 29, -6,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=411,420 ) /
			 0, 5, 3, 0, -598, 0, 0, 0, -3, -13, -7, 1, -5, -7, -3, 2, 3, 0, 0, -1, 5, -7, 0, 0, 4, 0, 0, -2, 16, -6, 0, 0, 8, -3, 0, 0, 8, -31, -16, -4,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=421,430 ) /
			 0, 3, 1, 0, 113, 0, 0, -49, 0, -24, -10, 0, 4, 0, 0, -2, 27, 0, 0, 0, -3, 0, 0, 1, 0, -4, -2, 0, 5, 0, 0, -2, 0, -3, 0, 0, -13, 0, 0, 6,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=431,440 ) /
			 5, 0, 0, -2, -18, -10, -4, 8, -4, -28, 0, 0, -5, 6, 3, 2, -3, 0, 0, 1, -5, -9, -4, 2, 17, 0, 0, -7, 11, 4, 0, 0, 0, -6, -2, 0, 83, 15, 0, 0,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=441,450 ) /
			 -4, 0, 0, 2, 0, -114, -49, 0, 117, 0, 0, -51, -5, 19, 10, 2, -3, 0, 0, 0, -3, 0, 0, 2, 0, -3, -1, 0, 3, 0, 0, 0, 0, -6, -2, 0, 393, 3, 0, 0,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=451,460 ) /
			 -4, 21, 11, 2, -6, 0, -1, 3, -3, 8, 4, 1, 8, 0, 0, 0, 18, -29, -13, -8, 8, 34, 18, -4, 89, 0, 0, 0, 3, 12, 6, -1, 54, -15, -7, -24, 0, 3, 0, 0,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=461,470 ) /
			 3, 0, 0, -1, 0, 35, 0, 0, -154, -30, -13, 67, 15, 0, 0, 0, 0, 4, 2, 0, 0, 9, 0, 0, 80, -71, -31, -35, 0, -20, -9, 0, 11, 5, 2, -5, 61, -96, -42, -27,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=471,480 ) /
			 14, 9, 4, -6, -11, -6, -3, 5, 0, -3, -1, 0, 123, -415, -180, -53, 0, 0, 0, -35, -5, 0, 0, 0, 7, -32, -17, -4, 0, -9, -5, 0, 0, -4, 2, 0, -89, 0, 0, 38,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=481,490 ) /
			 0, -86, -19, -6, 0, 0, -19, 6, -123, -416, -180, 53, 0, -3, -1, 0, 12, -6, -3, -5, -13, 9, 4, 6, 0, -15, -7, 0, 3, 0, 0, -1, -62, -97, -42, 27, -11, 5, 2, 5,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=491,500 ) /
			 0, -19, -8, 0, -3, 0, 0, 1, 0, 4, 2, 0, 0, 3, 0, 0, 0, 4, 2, 0, -85, -70, -31, 37, 163, -12, -5, -72, -63, -16, -7, 28, -21, -32, -14, 9, 0, -3, -1, 0,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=501,510 ) /
			 3, 0, 0, -2, 0, 8, 0, 0, 3, 10, 4, -1, 3, 0, 0, -1, 0, -7, -3, 0, 0, -4, -2, 0, 6, 19, 0, 0, 5, -173, -75, -2, 0, -7, -3, 0, 7, -12, -5, -3,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=511,520 ) /
			 -3, 0, 0, 2, 3, -4, -2, -1, 74, 0, 0, -32, -3, 12, 6, 2, 26, -14, -6, -11, 19, 0, 0, -8, 6, 24, 13, -3, 83, 0, 0, 0, 0, -10, -5, 0, 11, -3, -1, -5,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=521,530 ) /
			 3, 0, 1, -1, 3, 0, 0, -1, -4, 0, 0, 0, 5, -23, -12, -3, -339, 0, 0, 147, 0, -10, -5, 0, 5, 0, 0, 0, 3, 0, 0, -1, 0, -4, -2, 0, 18, -3, 0, 0,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=531,540 ) /
			 9, -11, -5, -4, -8, 0, 0, 4, 3, 0, 0, -1, 0, 9, 0, 0, 6, -9, -4, -2, -4, -12, 0, 0, 67, -91, -39, -29, 30, -18, -8, -13, 0, 0, 0, 0, 0, -114, -50, 0,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=541,550 ) /
			 0, 0, 0, 23, 517, 16, 7, -224, 0, -7, -3, 0, 143, -3, -1, -62, 29, 0, 0, -13, -4, 0, 0, 2, -6, 0, 0, 3, 5, 12, 5, -2, -25, 0, 0, 11, -3, 0, 0, 1,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=551,560 ) /
			 0, 4, 2, 0, -22, 12, 5, 10, 50, 0, 0, -22, 0, 7, 4, 0, 0, 3, 1, 0, -4, 4, 2, 2, -5, -11, -5, 2, 0, 4, 2, 0, 4, 17, 9, -2, 59, 0, 0, 0,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=561,570 ) /
			 0, -4, -2, 0, -8, 0, 0, 4, -3, 0, 0, 0, 4, -15, -8, -2, 370, -8, 0, -160, 0, 0, -3, 0, 0, 3, 1, 0, -6, 3, 1, 3, 0, 6, 0, 0, -10, 0, 0, 4,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=571,580 ) /
			 0, 9, 4, 0, 4, 17, 7, -2, 34, 0, 0, -15, 0, 5, 3, 0, -5, 0, 0, 2, -37, -7, -3, 16, 3, 13, 7, -2, 40, 0, 0, 0, 0, -3, -2, 0, -184, -3, -1, 80,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=581,590 ) /
			 -3, 0, 0, 1, -3, 0, 0, 0, 0, -10, -6, -1, 31, -6, 0, -13, -3, -32, -14, 1, -7, 0, 0, 3, 0, -8, -4, 0, 3, -4, 0, 0, 0, 4, 0, 0, 0, 3, 1, 0,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=591,600 ) /
			 19, -23, -10, 2, 0, 0, 0, -10, 0, 3, 2, 0, 0, 9, 5, -1, 28, 0, 0, 0, 0, -7, -4, 0, 8, -4, 0, -4, 0, 0, -2, 0, 0, 3, 0, 0, -3, 0, 0, 1,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=601,610 ) /
			 -9, 0, 1, 4, 3, 12, 5, -1, 17, -3, -1, 0, 0, 7, 4, 0, 19, 0, 0, 0, 0, -5, -3, 0, 14, -3, 0, -1, 0, 0, -1, 0, 0, 0, 0, -5, 0, 5, 3, 0,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=611,620 ) /
			 13, 0, 0, 0, 0, -3, -2, 0, 2, 9, 4, 3, 0, 0, 0, -4, 8, 0, 0, 0, 0, 4, 2, 0, 6, 0, 0, -3, 6, 0, 0, 0, 0, 3, 1, 0, 5, 0, 0, -2,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=621,630 ) /
			 3, 0, 0, -1, -3, 0, 0, 0, 6, 0, 0, 0, 7, 0, 0, 0, -4, 0, 0, 0, 4, 0, 0, 0, 6, 0, 0, 0, 0, -4, 0, 0, 0, -4, 0, 0, 5, 0, 0, 0,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=631,640 ) /
			 -3, 0, 0, 0, 4, 0, 0, 0, -5, 0, 0, 0, 4, 0, 0, 0, 0, 3, 0, 0, 13, 0, 0, 0, 21, 11, 0, 0, 0, -5, 0, 0, 0, -5, -2, 0, 0, 5, 3, 0,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=641,650 ) /
			 0, -5, 0, 0, -3, 0, 0, 2, 20, 10, 0, 0, -34, 0, 0, 0, -19, 0, 0, 0, 3, 0, 0, -2, -3, 0, 0, 1, -6, 0, 0, 3, -4, 0, 0, 0, 3, 0, 0, 0,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=651,660 ) /
			 3, 0, 0, 0, 4, 0, 0, 0, 3, 0, 0, -1, 6, 0, 0, -3, -8, 0, 0, 3, 0, 3, 1, 0, -3, 0, 0, 0, 0, -3, -2, 0, 126, -63, -27, -55, -5, 0, 1, 2,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=661,670 ) /
			 -3, 28, 15, 2, 5, 0, 1, -2, 0, 9, 4, 1, 0, 9, 4, -1, -126, -63, -27, 55, 3, 0, 0, -1, 21, -11, -6, -11, 0, -4, 0, 0, -21, -11, -6, 11, -3, 0, 0, 1,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=671,680 ) /
			 0, 3, 1, 0, 8, 0, 0, -4, -6, 0, 0, 3, -3, 0, 0, 1, 3, 0, 0, -1, -3, 0, 0, 1, -5, 0, 0, 2, 24, -12, -5, -11, 0, 3, 1, 0, 0, 3, 1, 0,
			// DATA ( ( ICPL(I,J) / I=1,4 ) / J=681,687 ) /
			 0, 3, 2, 0, -24, -12, -5, 10, 4, 0, -1, -2, 13, 0, 0, -6, 7, 0, 0, -3, 3, 0, 0, -1, 3, 0, 0, -1, };
}
