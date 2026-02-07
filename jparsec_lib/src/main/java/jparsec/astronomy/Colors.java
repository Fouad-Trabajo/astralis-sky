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
package jparsec.astronomy;

import jparsec.util.JPARSECException;

/**
 * Class to transform color indexes between several systems.
 * <P>
 * High-order polynomials for transforming amongst colors are given by Caldwell
 * et al.:
 * <P>
 * 1993SAAOC..15....1C<BR>
 * CALDWELL J.A.R., COUSINS A.W.J., AHLERS C.C., VAN WAMELEN P., MARITZ E.J.,
 * South African Astron. Obs. Circ., 15, 1-29 (1993), "Statistical relations
 * between photometric colours of common types of stars in the UBV (RI)c, JHK
 * and uvby systems".
 * <P>
 * Code has been taken from Guide software. It has not been tested, use with
 * caution.<P>
 * Color index conversion is something complex and instrument dependent. For
 * a list of formulae you can check http://www.aerith.net/astro/color_conversion.html.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Colors
{
	// private constructor so that this class cannot be instantiated.
	private Colors() {}

	/*
	 * Code to do assorted rough transformations between color systems, such as
	 * getting a B-V value given a V-I color. I started out with FORTRAN code
	 * supplied by Brian Skiff, converted it to C, and added some inverse
	 * transformations not previously available. Note that these are _extremely_
	 * rocky, because they happen when one color doesn't vary much with respect
	 * to another color. They should not be taken too seriously. Brian comments:
	 * "High-order polynomials for transforming amongst colors are given by
	 * Caldwell et al.: 1993SAAOC..15....1C CALDWELL J.A.R., COUSINS A.W.J.,
	 * AHLERS C.C., VAN WAMELEN P., MARITZ E.J. South African Astron. Obs.
	 * Circ., 15, 1-29 (1993) Statistical relations between photometric colours
	 * of common types of stars in the UBV (RI)c, JHK and uvby systems." You'll
	 * see three separate test main( ) functions at the bottom, originally used
	 * in testing these functions and now possibly helpful in showing how they
	 * are used. If you define LONEOS_PHOT (the last test main( )), you'll get a
	 * program to read loneos.phot and add colors where no colors were
	 * previously available, flagged so you can know where they came from.
	 */

	static int COLOR_ORDER = 13;

	private static double compute_color_polynomial(double ival, double coeffs[], double low_limit, double high_limit)
	throws JPARSECException {
		double rval = 0., power = 1.;
		int order = COLOR_ORDER;
		int coeffs_index = -1;

		if (ival < low_limit || ival > high_limit)
			throw new JPARSECException("calculations did not converge.");
		else
			while (order != 0)
			{
				coeffs_index++;
				rval += (coeffs[coeffs_index]) * power;
				power *= ival - 1.;
				order--;
			}
		return (rval + 1.);
	}

	private static double compute_inverse_color_polynomial(double ival, double coeffs[], double ilow_limit,
			double ihigh_limit)
	throws JPARSECException {
		double color_low, color_high, rval = 0.0, color_rval;
		double high_limit = ihigh_limit;
		double low_limit = ilow_limit;
		int max_iterations = 100;

		color_low = compute_color_polynomial(ilow_limit, coeffs, low_limit, high_limit);
		color_high = compute_color_polynomial(ihigh_limit, coeffs, low_limit, high_limit);
		if (ival < color_low || ival > color_high)
			throw new JPARSECException("calculations did not converge.");
		else
			while ((color_high - color_low) > 1.e-6 && max_iterations != 0)
			{
				rval = (ival - color_low) / (color_high - color_low);
				rval += rval * (rval - .5) * (rval - 1.);
				rval = low_limit + (high_limit - low_limit) * rval;
				color_rval = compute_color_polynomial(rval, coeffs, ilow_limit, ihigh_limit);
				if (color_rval < ival)
				{
					low_limit = rval;
					color_low = color_rval;
				} else
				{
					high_limit = rval;
					color_high = color_rval;
				}
				max_iterations--;
			}

		if (max_iterations == 0) /* didn't converge on an answer */
			throw new JPARSECException("calculations did not converge.");

		return (rval);
	}

	static final double coeffs_vi_to_bv[] =
	{ -0.6865072E-01, 0.8837997E+00, -0.3889774E+00, -0.4998126E-02, 0.3867544E+00, -0.5422331E+00, -0.8926476E-01,
			0.5194797E+00, -0.2044681E+00, -0.1009025E+00, 0.9543256E-01, -0.2567529E-01, 0.2393742E-02 };

	/**
	 * Obtains approximate B-V from V-I color for common stars.
	 * @param v_minus_i V-I color index.
	 * @return B-V color index.
	 * @throws JPARSECException If no convergence is found.
	 */
	public static double viTobv(double v_minus_i)
	throws JPARSECException {
		return (compute_color_polynomial(v_minus_i, coeffs_vi_to_bv, -.23, 3.70));
	}

	/**
	 * Computes approximate V-I from B-V color index for common stars.
	 * @param b_minus_v B-V.
	 * @return V-I color index.
	 * @throws JPARSECException If no convergence is found.
	 */
	public static double bvTovi(double b_minus_v)
	throws JPARSECException {
		return (compute_inverse_color_polynomial(b_minus_v, coeffs_vi_to_bv, -.23, 3.70));
	}

	static final double coeffs_vi_to_vr[] =
	{ -0.4708373E+00, 0.5920728E+00, -0.1095294E-01, -0.2281118E+00, -0.9372892E-01, 0.1931393E+00, 0.5077253E-01,
			-0.9927284E-01, 0.8560631E-02, 0.1922702E-01, -0.7201880E-02, 0.7743020E-03, 0. };

	/**
	 * Obtains approximate V-R from V-I color for common stars.
	 * @param v_minus_i V-I color index.
	 * @return V-R color index.
	 * @throws JPARSECException If no convergence is found.
	 */
	public static double viTovr(double v_minus_i)
	throws JPARSECException {
		return (compute_color_polynomial(v_minus_i, coeffs_vi_to_vr, -.30, 4.00));
	}

	/**
	 * Obtains approximate V-I from V-R color for common stars.
	 * @param v_minus_r V-R color index.
	 * @return V-I color index.
	 * @throws JPARSECException If no convergence is found.
	 */
	public static double vrTovi(double v_minus_r)
	throws JPARSECException {
		return (compute_inverse_color_polynomial(v_minus_r, coeffs_vi_to_vr, -.30, 4.00));
	}

	/**
	 * Obtains approximate B-V from V-R color for common stars.
	 * @param v_minus_r V-R color index.
	 * @return B-V color index.
	 * @throws JPARSECException If no convergence is found.
	 */
	public static double vrTobv(double v_minus_r)
	throws JPARSECException {
		double coeffs[] =
		{ 0.4860429E+00, 0.6904008E+00, -0.1229411E+01, 0.2990030E+01, 0.7104513E+01, -0.1637799E+02, -0.2977123E+02,
				0.4390751E+02, 0.6145810E+02, -0.5265358E+02, -0.6135921E+02, 0.2297835E+02, 0.2385013E+02 };

		return (compute_color_polynomial(v_minus_r, coeffs, -.10, 1.75));
	}

	/**
	 * Obtains approximate V-R from B-V color for common stars.
	 * @param b_minus_v B-V color index.
	 * @return V-R color index.
	 * @throws JPARSECException If no convergence is found.
	 */
	public static double bvTovr(double b_minus_v)
	throws JPARSECException {
		double coeffs[] =
		{ -0.4140951E+00, 0.7357165E+00, -0.5242979E-01, -0.6293304E+00, 0.2332871E+01, 0.3812365E+01, -0.5082941E+01,
				-0.6520325E+01, 0.4817797E+01, 0.5065505E+01, -0.1706011E+01, -0.1568243E+01, 0. };

		return (compute_color_polynomial(b_minus_v, coeffs, -.23, 1.95));
	}

	/**
	 * Obtains approximate B-V from Bt-Vt in the Tycho system.
	 * @param b_v_t Bt-Vt color index.
	 * @return B-V color index.
	 * @throws JPARSECException If input is out of range (&lt;-0.2 or &gt;1.8).
	 */
	public static double bvTychoTobvJohnson(double b_v_t)
	throws JPARSECException {
		double delta = 0.;

		if (b_v_t < -.2 || b_v_t > 1.8)
			throw new JPARSECException("input is out of range.");
		if (b_v_t < .1)
			delta = -.006 + .006 * (b_v_t + .2) / .3;
		else if (b_v_t < .5)
			delta = .046 * (b_v_t - .1) / .4;
		else if (b_v_t < 1.4)
			delta = .046 - .054 * (b_v_t - .5) / .9;
		else if (b_v_t < 1.8)
			delta = -.008 - .024 * (b_v_t - 1.4) / .4;
		return (.85 * b_v_t + delta);
	}

	/**
	 * Obtains approximate V magnitude from Bt-Vt and Vt in the Tycho system.
	 * @param b_v_t Bt-Vt color index.
	 * @param vt Vt magnitude.
	 * @return V magnitude.
	 * @throws JPARSECException If input is out of range (&lt;-0.2 or &gt;1.8).
	 */
	public static double vTychoTovJohnson(double b_v_t, double vt)
	throws JPARSECException {
		double delta = 0.;

		if (b_v_t < -.2 || b_v_t > 1.8)
			throw new JPARSECException("input is out of range.");
		if (b_v_t < .1)
			delta = .014 - .014 * (b_v_t + .2) / .3;
		else if (b_v_t < .5)
			delta = -.005 * (b_v_t - .1) / .4;
		else if (b_v_t < 1.4)
			delta = -.005;
		else if (b_v_t < 1.8)
			delta = -.005 - .010 * (b_v_t - 1.4) / .4;
		return (vt - .09 * b_v_t + delta);
	}

	/*
	 * The following function takes a (B-V)T value, i.e., a B-V color in the
	 * Tycho magnitude system, and computes from it V-VT, the difference between
	 * Johnson V and Tycho V; the corresponding B-V in the Johnson scheme; and
	 * V-Hp, the difference between Johnson V and Hp (Hipparcos "magnitude") for
	 * the input color. I got the raw data for this from Brian Skiff, who posted
	 * them on the Minor Planet Mailing List (MPML) with the following comment:
	 * "...To get standard V and B from Tycho-2, it is probably best to use the
	 * relation shown by Mike Bessell in the July 2000 PASP... Bessell does not
	 * give an algebraic relation, but instead shows a cubic spline fit with a
	 * look-up table... I have copied out Bessell's table below as a flat ASCII
	 * list." In what follows, values for (B-V)T away from the lookup table
	 * points is computed using (again) a cubic spline. The functions vary with
	 * sufficient slowness that this ought to be accurate down to the .001 mag
	 * level.
	 */
	private static int LOOKUP_SIZE = 46;

	/**
	 * Obtains from Bt-Vt (B-V in the Tycho magnitude system) the following data:<BR>
	 * V-Vt: difference between Johnson V and Tycho V.<BR>
	 * B-V: in the Johnson squeme.<BR>
	 * V-Hp: difference between Johnson V and Hipparcos magnitude.
	 * @param bt_minus_vt B-V in the Tycho magnitude system.
	 * @return Array of three values.
	 * @throws JPARSECException If input is out of range: Bt-Vt &lt; -0.25 or &gt; 2.
	 */
	public static double[]  bvTychoToJohnson(double bt_minus_vt)
	throws JPARSECException {
		double results[] = new double[3];
		int i, table_loc = (int) ((bt_minus_vt + .25) / .05) - 1;
		double dx, coeff[] = new double[4];
		int lookup_tbl[] =
		{
		/* BT-VT V-VT del(B-V) V-Hp */
		/* -0.250 */38, 31, 66,
		/* -0.200 */30, 21, 51,
		/* -0.150 */22, 11, 36,
		/* -0.100 */15, 5, 21,
		/* -0.050 */8, 2, 6,
		/* -0.000 */1, -5, -11,
		/* 0.050 */-5, -10, -25,
		/* 0.100 */-12, -17, -38,
		/* 0.150 */-18, -20, -48,
		/* 0.200 */-24, -21, -58,
		/* 0.250 */-29, -23, -69,
		/* 0.300 */-35, -25, -79,
		/* 0.350 */-40, -25, -87,
		/* 0.400 */-45, -26, -94,
		/* 0.450 */-50, -30, -101,
		/* 0.500 */-54, -35, -108,
		/* 0.550 */-59, -45, -114,
		/* 0.600 */-64, -51, -120,
		/* 0.650 */-68, -60, -127,
		/* 0.700 */-72, -68, -131,
		/* 0.750 */-77, -76, -134,
		/* 0.800 */-81, -85, -137,
		/* 0.850 */-85, -94, -142,
		/* 0.900 */-89, -104, -147,
		/* 0.950 */-93, -113, -151,
		/* 1.000 */-98, -122, -155,
		/* 1.050 */-102, -131, -158,
		/* 1.100 */-106, -142, -157,
		/* 1.150 */-110, -154, -160,
		/* 1.200 */-115, -166, -162,
		/* 1.250 */-119, -178, -164,
		/* 1.300 */-124, -189, -166,
		/* 1.350 */-128, -199, -166,
		/* 1.400 */-133, -210, -165,
		/* 1.450 */-138, -222, -164,
		/* 1.500 */-143, -234, -161,
		/* 1.550 */-148, -245, -157,
		/* 1.600 */-154, -256, -153,
		/* 1.650 */-160, -266, -148,
		/* 1.700 */-165, -277, -143,
		/* 1.750 */-172, -288, -137,
		/* 1.800 */-178, -299, -131,
		/* 1.850 */-185, -309, -125,
		/* 1.900 */-191, -320, -119,
		/* 1.950 */-199, -331, -112,
		/* 2.000 */-206, -342, -106 };

		if (bt_minus_vt < -.25 || bt_minus_vt > 2.)
			throw new JPARSECException("input is out of range.");
		if (table_loc < 0)
			table_loc = 0;
		if (table_loc >= LOOKUP_SIZE - 4)
			table_loc = LOOKUP_SIZE - 4;
		dx = ((bt_minus_vt + .25) / .05) - (double) table_loc;
		coeff[0] = (dx - 1.) * (dx - 2.) * (dx - 3.) / -6.;
		coeff[1] = dx * (dx - 2.) * (dx - 3.) / 2.;
		coeff[2] = dx * (dx - 1.) * (dx - 3.) / -2.;
		coeff[3] = dx * (dx - 1.) * (dx - 2.) / 6.;
		for (i = 0; i < 3; i++)
		{
			int tptr_index = i + table_loc * 3;

			results[i] = .001 * ((double) lookup_tbl[0 + tptr_index] * coeff[0] + (double) lookup_tbl[3 + tptr_index] * coeff[1] + (double) lookup_tbl[6 + tptr_index] * coeff[2] + (double) lookup_tbl[9 + tptr_index] * coeff[3]);
		}
		results[1] += bt_minus_vt; /* change a 'delta' into an 'absolute' */
		return results;
	}
}
