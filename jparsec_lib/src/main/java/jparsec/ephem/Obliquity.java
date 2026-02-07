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

import jparsec.ephem.EphemerisElement;
import jparsec.ephem.EphemerisElement.REDUCTION_METHOD;
import jparsec.math.Constant;
import jparsec.util.JPARSECException;

/**
 * Calculation of mean and true obliquity. Different methods can be applied, in
 * an unique system of constants compatible with {@linkplain EphemerisElement}
 * class.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Obliquity
{
	// private constructor so that this class cannot be instantiated.
	private Obliquity() {}

	private static final double[] xypol = new double[] {84028.206305, 0.3624445, -0.00004039, -110E-9};

	private static final double[][] xyper = new double[][] {
		new double[] {409.90, 753.872780, -1704.720302},
		new double[] {396.15, -247.805823, -862.308358},
		new double[] {537.22, 379.471484, 447.832178},
		new double[] {402.90, -53.880558, -889.571909},
		new double[] {417.15, -90.109153, 190.402846},
		new double[] {288.92, -353.600190, -56.564991},
		new double[] {4043.00, -63.115353, -296.222622},
		new double[] {306.00, -28.248187, -75.859952},
		new double[] {277.00, 17.703387, 67.473503},
		new double[] {203.00, 38.911307, 3.014055}
	};

	/**
	 * Calculates the mean obliquity at a given time. The code for this method
	 * comes from S. L. Moshier. In case IAU2006 or IAU2009 algorithms are used
	 * and the Vondrak precession flag is enabled in the configuration, the obliquity
	 * will be calculated using the formulae by Vondrak. Time span in Julian centuries
	 * from J2000 can be expanded in this case from +/- 100 to +/- 2000.<P>
	 * References:<P>
	 * Vondrak et al. 2011, A&amp;A 534, A22.
	 * <P>
	 * Capitaine et al., Astronomy and Astrophysics 412, 567-586, (2003).
	 * <P>
	 * James G. Williams, "Contributions to the Earth's obliquity rate,
	 * precession, and nutation," Astron. J. 108, 711-724 (1994).
	 * <P>
	 * J. L. Simon, P. Bretagnon, J. Chapront, M. Chapront-Touze', G. Francou,
	 * and J. Laskar, "Numerical Expressions for precession formulae and mean
	 * elements for the Moon and the planets," Astronomy and Astrophysics 282,
	 * 663-683 (1994).
	 * <P>
	 * J. H. Lieske, T. Lederle, W. Fricke, and B. Morando, "Expressions for the
	 * Precession Quantities Based upon the IAU (1976) System of Astronomical
	 * Constants," Astronomy and Astrophysics 58, 1-16 (1977).
	 * <P>
	 * J. Laskar's expansion comes from "Secular terms of classical planetary
	 * theories using the results of general theory," Astronomy and Astrophysics
	 * 157, 59070 (1986).
	 *
	 * @param t Time in Julian centuries from J2000 in TT.<BR>
	 *        Valid range is the years -8000 to +12000 (t = -100 to 100). In case of using
	 *        the Vondrak formulae validity is t between +/- 2000.
	 * @param eph Ephemeris properties.
	 * @return The mean obliquity (epsilon sub 0) in radians.
	 * @throws JPARSECException In case t is outside range a warning is thrown, and in case
	 * warnings should be treated as errors an exception will be thrown.
	 */
	public static double meanObliquity(double t, EphemerisElement eph) throws JPARSECException
	{
		// The obliquity formula come from Meeus, Astro Algorithms, 2ed.
		double rval = 0.;
		double u, u0;

		// Capitaine et al. 2003, Hilton et al. 2006
		final double rvalStart_CAP = 23. * Constant.SECONDS_PER_DEGREE + 26. * Constant.MINUTES_PER_DEGREE + 21.406;
		final double coeffs_CAP[] =
		{ -468367.69, -183.1, 200340., -5760., -43400., 0.0, 0.0, 0.0, 0.0, 0.0 };

		// Simon et al., 1994
		final int OBLIQ_COEFFS = 10;
		final double rvalStart_SIM = 23. * Constant.SECONDS_PER_DEGREE + 26. * Constant.MINUTES_PER_DEGREE + 21.412;
		final double coeffs_SIM[] =
		{ -468092.7, -152., 199890., -5138., -24967., -3905., 712., 2787., 579., 245. };

		// Williams et al., DE403 Ephemeris
		final double rvalStart_WIL = 23. * Constant.SECONDS_PER_DEGREE + 26. * Constant.MINUTES_PER_DEGREE + 21.406173;
		final double coeffs_WIL[] =
		{ -468339.6, -175., 199890., -5138., -24967., -3905., 712., 2787., 579., 245. };

		// Laskar et al.
		/*
		 * This expansion is from Laskar, cited above. Bretagnon and Simon say,
		 * in Planetary Programs and Tables, that it is accurate to 0.1" over a
		 * span of 6000 years. Laskar estimates the precision to be 0.01" after
		 * 1000 years and a few seconds of arc after 10000 years.
		 */
		final double rvalStart_LAS = 23. * Constant.SECONDS_PER_DEGREE + 26. * Constant.MINUTES_PER_DEGREE + 21.448;
		final double coeffs_LAS[] =
		{ -468093., -155., 199925., -5138., -24967., -3905., 712., 2787., 579., 245. };

		// IAU 1976
		final double rvalStart_IAU = 23. * Constant.SECONDS_PER_DEGREE + 26. * Constant.MINUTES_PER_DEGREE + 21.448;
		final double coeffs_IAU[] =
		{ -468150., -590., 181300., 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};

		double rvalStart = rvalStart_SIM;
		double coeffs[] = coeffs_SIM;

		// Select the desired formula
		EphemerisElement.REDUCTION_METHOD type = eph.ephemMethod;
		switch (type)
		{
		case WILLIAMS_1994:
		case JPL_DE4xx:
			rvalStart = rvalStart_WIL;
			coeffs = coeffs_WIL;
			break;
		case SIMON_1994:
			rvalStart = rvalStart_SIM;
			coeffs = coeffs_SIM;
			break;
		case LASKAR_1986:
			rvalStart = rvalStart_LAS;
			coeffs = coeffs_LAS;
			break;
		case IAU_1976:
			rvalStart = rvalStart_IAU;
			coeffs = coeffs_IAU;
			break;
		default:
			rvalStart = rvalStart_CAP;
			coeffs = coeffs_CAP;
			break;
		}

		if (Math.abs(t) > 100 || (
				(type == REDUCTION_METHOD.IAU_2006 || 
				type == REDUCTION_METHOD.IAU_2009 || type == REDUCTION_METHOD.IAU_2015) && 
				eph.useVondrak2011PrecessionFormulaInsteadOfIAU2006)) {
			double y = 0;
			double w = Constant.TWO_PI * t;
			for (int i=0; i<10; i++) {
				double a = w / xyper[i][0];
				double s = Math.sin(a);
				double c = Math.cos(a);
				y += c * xyper[i][1] + s * xyper[i][2];
			}

			w = 1.0;
			for (int j=0; j<4; j++) {
				y += xypol[j] * w;
				w *= t;
			}
			rval = y * Constant.ARCSEC_TO_RAD;

			if (Math.abs(t) > 100)
				JPARSECException.addWarning("Date is too far from J2000, obliquity forced to Vondr\u00e1k et al. 2011 model.");
		} else {
			u = u0 = t / 100.; // u is in julian 10000's of years
			rval = rvalStart;

			for (int i = 0; i < OBLIQ_COEFFS; i++)
			{
				rval += u * coeffs[i] / 100.;
				u *= u0;
			}

			// convert from seconds to radians
			rval = rval * Constant.ARCSEC_TO_RAD;

			if (Math.abs(t) > 100.0) JPARSECException.addWarning("This date is too far from J2000 epoch. Obliquity is probably incorrect.");
		}

		return rval;
	}

	/**
	 * Calculate true obliquity applying the corresponding nutation theory.
	 *
	 * @param t Julian centuries from J2000 in TT.
	 * @param eph Ephemeris properties.
	 * @return true obliquity.
	 * @throws JPARSECException In case t is outside range a warning is thrown, and in case
	 * warnings should be treated as errors an exception will be thrown.
	 */
	public static double trueObliquity(double t, EphemerisElement eph) throws JPARSECException
	{
		double meanObliquity = meanObliquity(t, eph);

		// This corrects for polar movement, but it is not noticeable and usually done when correcting nutation.
//		Nutation.clearPreviousCalculation();
//		try {
//			EarthRotationParameters.obtainEOP(t, nutTheory);
//		} catch (Exception e) { }

		Nutation.calcNutation(t, eph);
		double trueObliquity = meanObliquity + Nutation.getNutationInObliquity();

		return trueObliquity;
	}
}
