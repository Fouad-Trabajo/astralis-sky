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

import java.util.ArrayList;

import jparsec.ephem.EphemerisElement.REDUCTION_METHOD;
import jparsec.ephem.planets.EphemElement;
import jparsec.ephem.stars.StarElement;
import jparsec.ephem.stars.StarEphem;
import jparsec.math.Constant;
import jparsec.observer.LocationElement;
import jparsec.util.JPARSECException;

/**
 * Precession of the equinox and ecliptic.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Precession
{
	// private constructor so that this class cannot be instantiated.
	private Precession() {}

	/**
	 * Get the precession matrices.
	 *
	 * @param type Precession method.
	 * @return Vector with position angle, node, and inclination of Earth's
	 *         orbit.
	 * @throws JPARSECException If the reduction method is not supported.
	 */
	private static ArrayList<double[]> getMatrices(EphemerisElement.REDUCTION_METHOD type) throws JPARSECException
	{
		ArrayList<double[]> v = new ArrayList<double[]>();

		switch (type)
		{
		case WILLIAMS_1994:
			/*
			 * In WILLIAMS and SIMON, Laskar's terms of order higher than t^4
			 * have been retained, because Simon et al mention that the solution
			 * is the same except for the lower order terms.
			 */
			v.add(new double[]
					{ -8.66e-10, -4.759e-8, 2.424e-7, 1.3095e-5, 1.7451e-4, -1.8055e-3, -0.235316, 0.076, 110.5407,
							50287.70000 });
			/* Pi from Williams' 1994 paper, in radians. */
			v.add(new double[]
			{ 6.6402e-16, -2.69151e-15, -1.547021e-12, 7.521313e-12, 1.9e-10, -3.54e-9, -1.8103e-7, 1.26e-7,
					7.436169e-5, -0.04207794833, 3.052115282424 });
			v.add(new double[]
			{ 1.2147e-16, 7.3759e-17, -8.26287e-14, 2.503410e-13, 2.4650839e-11, -5.4000441e-11, 1.32115526e-9,
					-6.012e-7, -1.62442e-5, 0.00227850649, 0.0 });

			break;
		case JPL_DE4xx:
			/* Corrections to Williams (1994) introduced in DE403. */
			v.add(new double[]
					{ -8.66e-10, -4.759e-8, 2.424e-7, 1.3095e-5, 1.7451e-4, -1.8055e-3, -0.235316, 0.076, 110.5414, 50287.91959 });
			/* Pi from Williams' 1994 paper, in radians. No change in DE403. */
			v.add(new double[]
			{ 6.6402e-16, -2.69151e-15, -1.547021e-12, 7.521313e-12, 1.9e-10, -3.54e-9, -1.8103e-7, 1.26e-7,
					7.436169e-5, -0.04207794833, 3.052115282424 });
			v.add(new double[]
			{ 1.2147e-16, 7.3759e-17, -8.26287e-14, 2.503410e-13, 2.4650839e-11, -5.4000441e-11, 1.32115526e-9,
					-6.012e-7, -1.62442e-5, 0.00227850649, 0.0 });
			break;
		case SIMON_1994:
			/* Precession coefficients from Simon et al: */
			v.add(new double[]
					{ -8.66e-10, -4.759e-8, 2.424e-7, 1.3095e-5, 1.7451e-4, -1.8055e-3, -0.235316, 0.07732, 111.2022, 	50288.200 });
			v.add(new double[]
			{ 6.6402e-16, -2.69151e-15, -1.547021e-12, 7.521313e-12, 1.9e-10, -3.54e-9, -1.8103e-7, 2.579e-8,
					7.4379679e-5, -0.0420782900, 3.0521126906 });
			v.add(new double[]
			{ 1.2147e-16, 7.3759e-17, -8.26287e-14, 2.503410e-13, 2.4650839e-11, -5.4000441e-11, 1.32115526e-9,
					-5.99908e-7, -1.624383e-5, 0.002278492868, 0.0 });

			break;
		case LASKAR_1986:
			/* Precession coefficients taken from Laskar's paper: */
			v.add(new double[]
					{ -8.66e-10, -4.759e-8, 2.424e-7, 1.3095e-5, 1.7451e-4, -1.8055e-3, -0.235316, 0.07732, 111.1971, 50290.966 });
			// Bretagnon and Chapront 1981
//			        { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.076, 111.37, 50290.966 });

			/*
			 * Node and inclination of the earth's orbit computed from Laskar's
			 * data as done in Bretagnon and Francou's paper. Units are radians.
			 */
			v.add(new double[]
			{ 6.6402e-16, -2.69151e-15, -1.547021e-12, 7.521313e-12, 6.3190131e-10, -3.48388152e-9, -1.813065896e-7,
					2.75036225e-8, 7.4394531426e-5, -0.042078604317, 3.052112654975 });
			v.add(new double[]
			{ 1.2147e-16, 7.3759e-17, -8.26287e-14, 2.503410e-13, 2.4650839e-11, -5.4000441e-11, 1.32115526e-9,
					-5.998737027e-7, -1.6242797091e-5, 0.002278495537, 0.0 });
			break;
		case IAU_1976:
			v.add(new double[]
					{ 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.006, 111.113, 50290.966 });

			/*
			 * Node and inclination of the earth's orbit computed from Laskar's
			 * data as done in Bretagnon and Francou's paper. Units are radians.
			 */
			v.add(new double[]
			{ 6.6402e-16, -2.69151e-15, -1.547021e-12, 7.521313e-12, 6.3190131e-10, -3.48388152e-9, -1.813065896e-7,
					2.75036225e-8, 7.4394531426e-5, -0.042078604317, 3.052112654975 });
			v.add(new double[]
			{ 1.2147e-16, 7.3759e-17, -8.26287e-14, 2.503410e-13, 2.4650839e-11, -5.4000441e-11, 1.32115526e-9,
					-5.998737027e-7, -1.6242797091e-5, 0.002278495537, 0.0 });
			break;
			default:
				throw new JPARSECException("Reduction method not supported.");
		}

		return v;
	}

	/**
	 * Get precession angles.
	 *
	 * @param toJ2000 True to precess to J2000, false for from J2000.
	 * @param JD Julian day of the output angles.
	 * @param eph Ephemeris properties.
	 * @return Array with the three precession angles (pA, omega, i) in the ecliptic,
	 * or with the precession angles (psiA, omegaA, chiA, EPS0) in the equator for IAU2000 and
	 * later resolutions.
	 * @throws JPARSECException If the reduction method is not supported.
	 */
	public static double[] getAngles(boolean toJ2000, double JD, EphemerisElement eph) throws JPARSECException
	{
		double JD0 = Constant.J2000;

		/*
		 * Each precession angle is specified by a polynomial in T = Julian
		 * centuries from JD0. See AA page B18.
		 */
		double T = (JD - JD0) / Constant.JULIAN_DAYS_PER_CENTURY;

		EphemerisElement.REDUCTION_METHOD type = eph.ephemMethod;
		if (type.isIAU20xx()) {
			if (toJ2000) T = -T;

			if (type == REDUCTION_METHOD.IAU_2000) {
				double T0 = 0.0;
				if (!toJ2000) T0 = Functions.toCenturies(JD);

				double EPS0 = 84381.448;
				double PSIA = ((((-0.0 * T + 0.0) * T - 0.001147) * T - 1.07259) * T + 5038.7784) * T - 0.29965 * T0;
				double OMEGAA = ((((+0.0 * T - 0.0) * T - 0.007726) * T + 0.05127) * T - 0.0) * T + EPS0 - 0.02524 * T0;
				double CHIA = ((((-0.0 * T + 0.0) * T - 0.001125) * T - 2.38064) * T + 10.5526) * T;

				PSIA *= Constant.ARCSEC_TO_RAD;
				OMEGAA *= Constant.ARCSEC_TO_RAD;
				CHIA *= Constant.ARCSEC_TO_RAD;
				EPS0 *= Constant.ARCSEC_TO_RAD;
				return new double[] {PSIA, OMEGAA, CHIA, EPS0};
			} else {
				if (eph.useVondrak2011PrecessionFormulaInsteadOfIAU2006) {
					double x = 0, y = 0, z = 0;
					double w = Constant.TWO_PI * T;
					for (int i=0; i<14; i++) {
						double a = w / xyper[i][0];
						double s = Math.sin(a);
						double c = Math.cos(a);
						x += c * xyper[i][1] + s * xyper[i][3];
						y += c * xyper[i][2] + s * xyper[i][4];

						a = w / zper[i][0];
						s = Math.sin(a);
						c = Math.cos(a);
						z += c * zper[i][1] + s * zper[i][2];
					}

					w = 1.0;
					for (int j=0; j<4; j++) {
						x += xypol[0][j] * w;
						y += xypol[1][j] * w;
						z += xypol[2][j] * w;
						w *= T;
					}
					x *= Constant.ARCSEC_TO_RAD;
					y *= Constant.ARCSEC_TO_RAD;
					z *= Constant.ARCSEC_TO_RAD;

					double PSIA = Functions.normalizeRadians(x), OMEGAA = Functions.normalizeRadians(y),
							CHIA = Functions.normalizeRadians(z), EPS0 = 84381.406 * Constant.ARCSEC_TO_RAD;
					return new double[] {PSIA, OMEGAA, CHIA, EPS0};
				} else {
					double EPS0 = 84381.406;
					double PSIA = ((((-0.0000000951 * T + 0.000132851) * T - 0.00114045) * T - 1.0790069) * T + 5038.481507) * T;
					double OMEGAA = ((((+0.0000003337 * T - 0.000000467) * T - 0.00772503) * T + 0.0512623) * T - 0.025754) * T + EPS0;
					double CHIA = ((((-0.0000000560 * T + 0.000170663) * T - 0.00121197) * T - 2.3814292) * T + 10.556403) * T;

					PSIA *= Constant.ARCSEC_TO_RAD;
					OMEGAA *= Constant.ARCSEC_TO_RAD;
					CHIA *= Constant.ARCSEC_TO_RAD;
					EPS0 *= Constant.ARCSEC_TO_RAD;
					return new double[] {PSIA, OMEGAA, CHIA, EPS0};
				}
			}
		}

		double pA, W, z;
		double element1[], element2[], element3[];
		int p1 = -1, p2 = -1, p3 = -1;
		int i;


		ArrayList<double[]> v = getMatrices(type);

		/*
		 * Precession in longitude
		 */
		T /= 10.0; /* thousands of years */
		p1++;
		element1 = v.get(0);
		pA = element1[p1];
		for (i = 0; i < 9; i++)
		{
			p1++;
			pA = pA * T + element1[p1];
		}
		pA *= Constant.ARCSEC_TO_RAD * T;

		/*
		 * Node of the moving ecliptic on the JD0 ecliptic.
		 */
		p2++;
		element2 = v.get(1);
		W = element2[p2];
		for (i = 0; i < 10; i++)
		{
			p2++;
			W = W * T + element2[p2];
		}

		/*
		 * Rotate about new x axis by the inclination of the moving ecliptic on
		 * the JD0 ecliptic.
		 */
		p3++;
		element3 = v.get(2);
		z = element3[p3];
		for (i = 0; i < 10; i++)
		{
			p3++;
			z = z * T + element3[p3];
		}

		if (toJ2000) z = -z;

		return new double[] { pA, W, z };
	}

	private static final double[][] xypol = new double[][] {
			new double[] {8473.343527, 5042.7980307, -0.00740913, 289E-9},
			new double[] {84283.175915, -0.4436568, 0.00000146, 151E-9},
			new double[] {-19.657270, 0.0790159, 0.00001472, -61E-9}
	};

	private static final double[][] xyper = new double[][] {
			new double[] {402.90, -22206.325946, 1267.727824, -3243.236469, -8571.476251},
			new double[] {256.75, 12236.649447, 1702.324248, -3969.723769, 5309.796459},
			new double[] {292.00, -1589.008343, -2970.553839, 7099.207893, -610.393953},
			new double[] {537.22, 2482.103195, 693.790312, -1903.696711, 923.201931},
			new double[] {241.45, 150.322920, -14.724451, 146.435014, 3.759055},
			new double[] {375.22, -13.632066, -516.649401, 1300.630106, -40.691114},
			new double[] {157.87, 389.437420, -356.794454, 1727.498039, 80.437484},
			new double[] {274.20, 2031.433792, -129.552058, 299.854055, 807.300668},
			new double[] {203.00, 363.748303, 256.129314, -1217.125982, 83.712326},
			new double[] {440.00, -896.747562, 190.266114, -471.367487, -368.654854},
			new double[] {170.72, -926.995700, 95.103991, -441.682145, -191.881064},
			new double[] {713.37, 37.070667, -332.907067, -86.169171, -4.263770},
			new double[] {313.00, -597.682468, 131.337633, -308.320429, -270.353691},
			new double[] {128.38, 66.282812, 82.731919, -422.815629, 11.602861}
	};

	private static final double[][] zper = new double[][] {
			new double[] {402.90, -13765.924050, -2206.967126},
			new double[] {256.75, 13511.858383, -4186.752711},
			new double[] {292.00, -1455.229106, 6737.949677},
			new double[] {537.22, 1054.394467, -856.922846},
			new double[] {375.22, -112.300144, 957.149088},
			new double[] {157.87, 202.769908, 1709.440735},
			new double[] {274.20, 1936.050095, 154.425505},
			new double[] {202.00, 327.517465, -1049.071786},
			new double[] {440.00, -655.484214, -243.520976},
			new double[] {170.72, -891.898637, -406.539008},
			new double[] {315.00, -494.780332, -301.504189},
			new double[] {136.32, 585.492621, 41.348740},
			new double[] {128.38, -333.322021, -446.656435},
			new double[] {490.00, 110.512834, 142.525186}
	};
	/**
	 * Precession following Vondrak et al. 2011. See A&amp;A 534, A22.
	 *
	 * @param JD0 Julian day of input vector (equatorial rectangular).
	 * @param JD Julian day of output. Either JD or JD0 must be equal to
	 *        Constant.J2000.
	 * @param R Input vector.
	 * @return Vector referred to mean equinox and equator of JD.
	 * @throws JPARSECException If JD and JD0 are non equal to J2000.
	 */
	public static double[] precessionVondrak2011(double JD0, double JD, double[] R) throws JPARSECException
	{
		if (JD != Constant.J2000 && JD0 != Constant.J2000)
			throw new JPARSECException("Precession must be from or to J2000 epoch.");

		double T = (JD - JD0) / Constant.JULIAN_DAYS_PER_CENTURY;
		if (JD == Constant.J2000)
			T = -T;

		double x = 0, y = 0, z = 0;
		double w = Constant.TWO_PI * T;
		for (int i=0; i<14; i++) {
			double a = w / xyper[i][0];
			double s = Math.sin(a);
			double c = Math.cos(a);
			x += c * xyper[i][1] + s * xyper[i][3];
			y += c * xyper[i][2] + s * xyper[i][4];

			a = w / zper[i][0];
			s = Math.sin(a);
			c = Math.cos(a);
			z += c * zper[i][1] + s * zper[i][2];
		}

		w = 1.0;
		for (int j=0; j<4; j++) {
			x += xypol[0][j] * w;
			y += xypol[1][j] * w;
			z += xypol[2][j] * w;
			w *= T;
		}
		x *= Constant.ARCSEC_TO_RAD;
		y *= Constant.ARCSEC_TO_RAD;
		z *= Constant.ARCSEC_TO_RAD;
		//w = x * x + y * y;
		//double z = 0.0;
		//if (w < 1.0) z = -Math.sqrt(1.0 - w);

		double PSIA = x, OMEGAA = y, CHIA = z, EPS0 = 84381.406 * Constant.ARCSEC_TO_RAD;
		double SA = Math.sin(EPS0);
		double CA = Math.cos(EPS0);
		double SB = Math.sin(-PSIA);
		double CB = Math.cos(-PSIA);
		double SC = Math.sin(-OMEGAA);
		double CC = Math.cos(-OMEGAA);
		double SD = Math.sin(CHIA);
		double CD = Math.cos(CHIA);

		// COMPUTE ELEMENTS OF PRECESSION ROTATION MATRIX
		// EQUIVALENT TO R3(CHI_A)R1(-OMEGA_A)R3(-PSI_A)R1(EPSILON_0)
		double XX = CD * CB - SB * SD * CC;
		double YX = CD * SB * CA + SD * CC * CB * CA - SA * SD * SC;
		double ZX = CD * SB * SA + SD * CC * CB * SA + CA * SD * SC;
		double XY = -SD * CB - SB * CD * CC;
		double YY = -SD * SB * CA + CD * CC * CB * CA - SA * CD * SC;
		double ZY = -SD * SB * SA + CD * CC * CB * SA + CA * CD * SC;
		double XZ = SB * SC;
		double YZ = -SC * CB * CA - SA * CC;
		double ZZ = -SC * CB * SA + CC * CA;

		double px = 0.0, py = 0.0, pz = 0.0;

		if (JD0 == Constant.J2000)
		{
			// PERFORM ROTATION FROM J2000.0 TO EPOCH
			px = XX * R[0] + YX * R[1] + ZX * R[2];
			py = XY * R[0] + YY * R[1] + ZY * R[2];
			pz = XZ * R[0] + YZ * R[1] + ZZ * R[2];
		} else
		{
			// PERFORM ROTATION FROM EPOCH TO J2000.0
			px = XX * R[0] + XY * R[1] + XZ * R[2];
			py = YX * R[0] + YY * R[1] + YZ * R[2];
			pz = ZX * R[0] + ZY * R[1] + ZZ * R[2];
		}

		return new double[] { px, py, pz };
	}

	/**
	 * Precession following Capitaine et al. 2003.
	 * <P>
	 * Capitaine formula of precession is to be officially adopted by the IAU,
	 * see recommendation in the report of the IAU Division I Working Group on
	 * Precession and the Ecliptic (Hilton et al. 2006, Celest. Mech., 94,
	 * 351-367).
	 * <P>
	 * Reference: Capitaine et al., Astronomy & Astrophysics 412, 567-586,
	 * 2003.
	 *
	 * @param JD0 Julian day of input vector (equatorial rectangular).
	 * @param JD Julian day of output. Either JD or JD0 must be equal to
	 *        Constant.J2000.
	 * @param R Input vector.
	 * @return Vector referred to mean equinox and equator of JD.
	 * @throws JPARSECException If JD and JD0 are non equal to J2000.
	 */
	protected static double[] precessionIAU2006(double JD0, double JD, double[] R) throws JPARSECException
	{
		if (JD != Constant.J2000 && JD0 != Constant.J2000)
			throw new JPARSECException("Precession must be from or to J2000 epoch.");

		double T = (JD - JD0) / Constant.JULIAN_DAYS_PER_CENTURY;
		if (JD == Constant.J2000)
			T = -T;

		double EPS0 = 84381.406;
		double PSIA = ((((-0.0000000951 * T + 0.000132851) * T - 0.00114045) * T - 1.0790069) * T + 5038.481507) * T;
		double OMEGAA = ((((+0.0000003337 * T - 0.000000467) * T - 0.00772503) * T + 0.0512623) * T - 0.025754) * T + EPS0;
		double CHIA = ((((-0.0000000560 * T + 0.000170663) * T - 0.00121197) * T - 2.3814292) * T + 10.556403) * T;

		EPS0 *= Constant.ARCSEC_TO_RAD;
		PSIA *= Constant.ARCSEC_TO_RAD;
		OMEGAA *= Constant.ARCSEC_TO_RAD;
		CHIA *= Constant.ARCSEC_TO_RAD;

		double SA = Math.sin(EPS0);
		double CA = Math.cos(EPS0);
		double SB = Math.sin(-PSIA);
		double CB = Math.cos(-PSIA);
		double SC = Math.sin(-OMEGAA);
		double CC = Math.cos(-OMEGAA);
		double SD = Math.sin(CHIA);
		double CD = Math.cos(CHIA);

		// COMPUTE ELEMENTS OF PRECESSION ROTATION MATRIX
		// EQUIVALENT TO R3(CHI_A)R1(-OMEGA_A)R3(-PSI_A)R1(EPSILON_0)
		double XX = CD * CB - SB * SD * CC;
		double YX = CD * SB * CA + SD * CC * CB * CA - SA * SD * SC;
		double ZX = CD * SB * SA + SD * CC * CB * SA + CA * SD * SC;
		double XY = -SD * CB - SB * CD * CC;
		double YY = -SD * SB * CA + CD * CC * CB * CA - SA * CD * SC;
		double ZY = -SD * SB * SA + CD * CC * CB * SA + CA * CD * SC;
		double XZ = SB * SC;
		double YZ = -SC * CB * CA - SA * CC;
		double ZZ = -SC * CB * SA + CC * CA;

		double px = 0.0, py = 0.0, pz = 0.0;

		if (JD0 == Constant.J2000)
		{
			// PERFORM ROTATION FROM J2000.0 TO EPOCH
			px = XX * R[0] + YX * R[1] + ZX * R[2];
			py = XY * R[0] + YY * R[1] + ZY * R[2];
			pz = XZ * R[0] + YZ * R[1] + ZZ * R[2];
		} else
		{
			// PERFORM ROTATION FROM EPOCH TO J2000.0
			px = XX * R[0] + XY * R[1] + XZ * R[2];
			py = YX * R[0] + YY * R[1] + YZ * R[2];
			pz = ZX * R[0] + ZY * R[1] + ZZ * R[2];
		}

		return new double[] { px, py, pz };
	}

	/**
	 * Precession following IAU2000 definitions. From SOFA software library.
	 * <P>
	 * Reference: Capitaine et al., Astronomy & Astrophysics 400, 1145-1154,
	 * 2003. See also Lieske et al. 1977.
	 *
	 * @param JD0 Julian day of input vector (equatorial rectangular).
	 * @param JD Julian day of output. Either JD or JD0 must be equal to
	 *        Constant.J2000.
	 * @param R Input vector.
	 * @return Vector refered to mean equinox and equator of JD.
	 * @throws JPARSECException If both JD and JD0 are not equal to J2000 epoch.
	 */
	private static double[] precessionIAU2000(double JD0, double JD, double[] R) throws JPARSECException
	{
		if (JD != Constant.J2000 && JD0 != Constant.J2000) throw new JPARSECException("Precession must be from or to J2000 epoch.");

		double T = (JD - JD0) / Constant.JULIAN_DAYS_PER_CENTURY;
		double T0 = Functions.toCenturies(JD);
		if (JD == Constant.J2000) T = -T;

		double EPS0 = 84381.448;
		double PSIA = ((((-0.0 * T + 0.0) * T - 0.001147) * T - 1.07259) * T + 5038.7784) * T - 0.29965 * T0;
		double OMEGAA = ((((+0.0 * T - 0.0) * T - 0.007726) * T + 0.05127) * T - 0.0) * T + EPS0 - 0.02524 * T0;
		double CHIA = ((((-0.0 * T + 0.0) * T - 0.001125) * T - 2.38064) * T + 10.5526) * T;

		EPS0 *= Constant.ARCSEC_TO_RAD;
		PSIA *= Constant.ARCSEC_TO_RAD;
		OMEGAA *= Constant.ARCSEC_TO_RAD;
		CHIA *= Constant.ARCSEC_TO_RAD;

		double SA = Math.sin(EPS0);
		double CA = Math.cos(EPS0);
		double SB = Math.sin(-PSIA);
		double CB = Math.cos(-PSIA);
		double SC = Math.sin(-OMEGAA);
		double CC = Math.cos(-OMEGAA);
		double SD = Math.sin(CHIA);
		double CD = Math.cos(CHIA);

		// COMPUTE ELEMENTS OF PRECESSION ROTATION MATRIX
		// EQUIVALENT TO R3(CHI_A)R1(-OMEGA_A)R3(-PSI_A)R1(EPSILON_0)
		double XX = CD * CB - SB * SD * CC;
		double YX = CD * SB * CA + SD * CC * CB * CA - SA * SD * SC;
		double ZX = CD * SB * SA + SD * CC * CB * SA + CA * SD * SC;
		double XY = -SD * CB - SB * CD * CC;
		double YY = -SD * SB * CA + CD * CC * CB * CA - SA * CD * SC;
		double ZY = -SD * SB * SA + CD * CC * CB * SA + CA * CD * SC;
		double XZ = SB * SC;
		double YZ = -SC * CB * CA - SA * CC;
		double ZZ = -SC * CB * SA + CC * CA;

		double px = 0.0, py = 0.0, pz = 0.0;

		if (JD0 == Constant.J2000)
		{
			// PERFORM ROTATION FROM J2000.0 TO EPOCH
			px = XX * R[0] + YX * R[1] + ZX * R[2];
			py = XY * R[0] + YY * R[1] + ZY * R[2];
			pz = XZ * R[0] + YZ * R[1] + ZZ * R[2];
		} else {
			// PERFORM ROTATION FROM EPOCH TO J2000.0
			px = XX * R[0] + XY * R[1] + XZ * R[2];
			py = YX * R[0] + YY * R[1] + YZ * R[2];
			pz = ZX * R[0] + ZY * R[1] + ZZ * R[2];
		}

		return new double[] { px, py, pz };
	}

	/**
	 * Precess rectangular equatorial coordinates from J2000 epoch.
	 *
	 * @param JD Equinox of the output in Julian day (TT).
	 * @param R Array with x, y, z.
	 * @param eph Ephemeris properties.
	 * @return Array with corrected x, y, z.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double[] precessFromJ2000(double JD, double[] R, EphemerisElement eph) throws JPARSECException
	{
		double A, B, pA, W, z;
		double x[] = new double[3];
		double JD0 = Constant.J2000;

		if (JD == JD0)
			return R;

		EphemerisElement.REDUCTION_METHOD type = eph.ephemMethod;
		if (type == EphemerisElement.REDUCTION_METHOD.IAU_2000)
			return Precession.precessionIAU2000(Constant.J2000, JD, R);
		if (type.isIAU20xx()) {
			if (eph.useVondrak2011PrecessionFormulaInsteadOfIAU2006) return Precession.precessionVondrak2011(Constant.J2000, JD, R);
			return Precession.precessionIAU2006(Constant.J2000, JD, R);
		}
		if (type == EphemerisElement.REDUCTION_METHOD.IAU_1976)
			return Precession.precessionIAU1976(Constant.J2000, JD, R);

		double angles[] = getAngles(false, JD, eph);

		/*
		 * Implementation by elementary rotations using expansions. First rotate
		 * about the x axis from the initial equator to the ecliptic. (The input
		 * is equatorial.)
		 */
		double eps = Obliquity.meanObliquity(Functions.toCenturies(JD0), eph);

		x[0] = R[0];
		z = Math.cos(eps) * R[1] + Math.sin(eps) * R[2];
		x[2] = -Math.sin(eps) * R[1] + Math.cos(eps) * R[2];
		x[1] = z;

		/*
		 * Precession in longitude
		 */
		pA = angles[0];

		/*
		 * Node of the moving ecliptic on the JD0 ecliptic.
		 */
		W = angles[1];

		/*
		 * Rotate about z axis to the node.
		 */
		z = W;
		B = Math.cos(z);
		A = Math.sin(z);
		z = B * x[0] + A * x[1];
		x[1] = -A * x[0] + B * x[1];
		x[0] = z;

		/*
		 * Rotate about new x axis by the inclination of the moving ecliptic on
		 * the JD0 ecliptic.
		 */
		z = angles[2];

		B = Math.cos(z);
		A = Math.sin(z);
		z = B * x[1] + A * x[2];
		x[2] = -A * x[1] + B * x[2];
		x[1] = z;

		/*
		 * Rotate about new z axis back from the node.
		 */
		z = -W - pA;
		B = Math.cos(z);
		A = Math.sin(z);
		z = B * x[0] + A * x[1];
		x[1] = -A * x[0] + B * x[1];
		x[0] = z;

		/*
		 * Rotate about x axis to final equator.
		 */
		eps = Obliquity.meanObliquity(Functions.toCenturies(JD), eph);

		z = Math.cos(eps) * x[1] - Math.sin(eps) * x[2];
		x[2] = Math.sin(eps) * x[1] + Math.cos(eps) * x[2];
		x[1] = z;

		return x;
	}

	/**
	 * Precess rectangular equatorial coordinates to J2000 epoch.
	 *
	 * @param JD Equinox of the input in Julian day (TT).
	 * @param R Array with x, y, z.
	 * @param eph The ephemeris properties.
	 * @return Array with corrected x, y, z.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double[] precessToJ2000(double JD, double[] R, EphemerisElement eph) throws JPARSECException
	{
		double A, B, pA, W, z;
		double x[] = new double[3];
		double JD0 = Constant.J2000;

		if (JD == JD0)
			return R;

		EphemerisElement.REDUCTION_METHOD type = eph.ephemMethod;
		if (type == EphemerisElement.REDUCTION_METHOD.IAU_2000)
			return Precession.precessionIAU2000(JD, Constant.J2000, R);
		if (type.isIAU20xx()) {
			if (eph.useVondrak2011PrecessionFormulaInsteadOfIAU2006) return Precession.precessionVondrak2011(JD, Constant.J2000, R);
			return Precession.precessionIAU2006(JD, Constant.J2000, R);
		}
		if (type == EphemerisElement.REDUCTION_METHOD.IAU_1976)
			return Precession.precessionIAU1976(JD, Constant.J2000, R);

		double angles[] = getAngles(true, JD, eph);

		/*
		 * Implementation by elementary rotations using expansions. First rotate
		 * about the x axis from the initial equator to the ecliptic. (The input
		 * is equatorial.)
		 */
		double eps = Obliquity.meanObliquity(Functions.toCenturies(JD), eph);

		x[0] = R[0];
		z = Math.cos(eps) * R[1] + Math.sin(eps) * R[2];
		x[2] = -Math.sin(eps) * R[1] + Math.cos(eps) * R[2];
		x[1] = z;

		/*
		 * Precession in longitude
		 */
		pA = angles[0];

		/*
		 * Node of the moving ecliptic on the JD0 ecliptic.
		 */
		W = angles[1];

		/*
		 * Rotate about z axis to the node.
		 */
		z = W + pA;
		B = Math.cos(z);
		A = Math.sin(z);
		z = B * x[0] + A * x[1];
		x[1] = -A * x[0] + B * x[1];
		x[0] = z;

		/*
		 * Rotate about new x axis by the inclination of the moving ecliptic on
		 * the JD0 ecliptic.
		 */
		z = angles[2];

		B = Math.cos(z);
		A = Math.sin(z);
		z = B * x[1] + A * x[2];
		x[2] = -A * x[1] + B * x[2];
		x[1] = z;

		/*
		 * Rotate about new z axis back from the node.
		 */
		z = -W;
		B = Math.cos(z);
		A = Math.sin(z);
		z = B * x[0] + A * x[1];
		x[1] = -A * x[0] + B * x[1];
		x[0] = z;

		/*
		 * Rotate about x axis to final equator.
		 */
		eps = Obliquity.meanObliquity(Functions.toCenturies(JD0), eph);

		z = Math.cos(eps) * x[1] - Math.sin(eps) * x[2];
		x[2] = Math.sin(eps) * x[1] + Math.cos(eps) * x[2];
		x[1] = z;

		return x;
	}

	/**
	 * Precess rectangular equatorial coordinates between two epochs.
	 *
	 * @param JD0 Equinox of the input in Julian day (TT).
	 * @param JD Equinox of the output in Julian day (TT).
	 * @param R Array with x, y, z.
	 * @param eph Ephemeris properties.
	 * @return Array with corrected x, y, z.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double[] precess(double JD0, double JD, double[] R, EphemerisElement eph) throws JPARSECException
	{
		// Transform to J2000
		double to2000[] = Precession.precessToJ2000(JD0, R, eph);

		// Transform to output date
		double toJD[] = Precession.precessFromJ2000(JD, to2000, eph);

		return toJD;
	}

	/**
	 * Precess equatorial coordinates between two epochs. Input is taken
	 * from the ephem object, and results set in it.
	 *
	 * @param JD0 Equinox of the input in Julian day.
	 * @param JD Equinox of the output in Julian day.
	 * @param ephem Ephem object.
	 * @param eph Ephemeris properties.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void precessEphemObject(double JD0, double JD, EphemElement ephem, EphemerisElement eph) throws JPARSECException
	{
		double R[] = ephem.getEquatorialLocation().getRectangularCoordinates();

		// Transform to J2000
		double to2000[] = Precession.precessToJ2000(JD0, R, eph);

		// Transform to output date
		double toJD[] = Precession.precessFromJ2000(JD, to2000, eph);

		LocationElement loc = LocationElement.parseRectangularCoordinates(toJD);
		ephem.rightAscension = loc.getLongitude();
		ephem.declination = loc.getLatitude();
	}

	/**
	 * Performs precession correction to the direction of the north pole of
	 * rotation, refered to J2000 epoch.
	 *
	 * @param JD Desired date of the results.
	 * @param ephem {@linkplain EphemElement} with the input values of RA and DEC of north
	 *        pole.
	 * @param eph Ephemeris properties.
	 * @return {@linkplain EphemElement} object with the corrected RA and DEC.
	 * @throws JPARSECException If an error occurs.
	 */
	public static EphemElement precessPoleFromJ2000(double JD, EphemElement ephem, EphemerisElement eph) throws JPARSECException
	{
		// Transform spherical pole variables to rectangular
		double coord[] = LocationElement.parseLocationElement(new LocationElement(ephem.northPoleRA,
				ephem.northPoleDEC, ephem.distance));

		// Apply precession
		double eq[] = Precession.precessFromJ2000(eph.getEpoch(JD), coord, eph);

		// Transform to spherical variables
		LocationElement loc = LocationElement.parseRectangularCoordinates(eq);

		// Set values
		EphemElement ephemOut = ephem.clone();
		ephemOut.northPoleRA = loc.getLongitude();
		ephemOut.northPoleDEC = loc.getLatitude();

		return ephemOut;
	}

	/**
	 * Performs precession in ecliptic coordinates both in positions and
	 * velocities.
	 *
	 * @param JD0 Julian day of reference (TT).
	 * @param JD Julian day of the results (TT).
	 * @param coords Array with x, y, z, vx, vy, vz refered to mean ecliptic of
	 *        JD0.
	 * @param eph Ephemeris properties.
	 * @return Array with x, y, z, vx, vy, vz refered to mean ecliptic of JD.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double[] precessPosAndVelInEcliptic(double JD0, double JD, double coords[], EphemerisElement eph) throws JPARSECException
	{
		/* Ecliptic to equatorial */
		double epsilon = Obliquity.meanObliquity(Functions.toCenturies(JD0), eph);
		coords = Functions.rotateX(coords, epsilon);

		/* Precession */
		coords = Precession.precessPosAndVelInEquatorial(JD0, JD, coords, eph);

		/* Equatorial to ecliptic */
		epsilon = Obliquity.meanObliquity(Functions.toCenturies(JD), eph);
		coords = Functions.rotateX(coords, -epsilon);

		return coords;
	}

	/**
	 * Performs precession in equatorial coordinates both in positions and
	 * velocities.
	 *
	 * @param JD0 Julian day of reference.
	 * @param JD Julian day of the results.
	 * @param eq Array with x, y, z, vx, vy, vz refered to mean equator of JD0.
	 * @param eph Ephemeris properties.
	 * @return Array with x, y, z, vx, vy, vz refered to mean equator of JD.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double[] precessPosAndVelInEquatorial(double JD0, double JD, double eq[], EphemerisElement eph) throws JPARSECException
	{
		double pos[] = Precession.precess(JD0, JD, new double[] { eq[0], eq[1], eq[2] }, eph);
		double vel[] = { 0.0, 0.0, 0.0 };

		if (eq.length >= 6) vel = Precession.precess(JD0, JD, new double[] { eq[3], eq[4], eq[5] }, eph);
		return new double[] { pos[0], pos[1], pos[2], vel[0], vel[1], vel[2] };
	}

	/**
	 * Transforms B1950 coordinates (FK4 system) to J2000, supposing that the
	 * object has no proper motion or is far away.
	 *
	 * @param eq Equatorial coordinates FK4 B1950.
	 * @return FK5 J2000 coordinates.
	 * @throws JPARSECException Should not be thrown.
	 */
	public static double[] FK4_B1950ToFK5_J2000(double eq[]) throws JPARSECException
	{
		// Pass to spherical
		LocationElement loc = LocationElement.parseRectangularCoordinates(new double[] {eq[0], eq[1], eq[2]});

		// Create an static star at this position in FK4 B1950
		StarElement star = new StarElement("", loc.getLongitude(), loc.getLatitude(), 0,
				0.0f, 0.0f, 0.0f, 0.0f, Constant.B1950, EphemerisElement.FRAME.FK4);

		// Transform coordinates.
		star = StarEphem.transform_FK4_B1950_to_FK5_J2000(star);

		// Pass to LocationElement
		LocationElement loc_out = new LocationElement(star.rightAscension, star.declination, loc.getRadius());

		// Pass to rectangular coordinates
		double eq_out[] = LocationElement.parseLocationElement(loc_out);

		return eq_out;
	}

	/**
	 * Transforms J2000 FK5 coordinates to FK4 B1950, supposing that the
	 * object has no proper motion or is far away.
	 *
	 * @param eq Equatorial coordinates FK5 J2000.
	 * @return FK4 B1950 coordinates.
	 * @throws JPARSECException Should not be thrown.
	 */
	public static double[] FK5_J2000ToFK4_B1950(double eq[]) throws JPARSECException
	{
		// Pass to spherical
		LocationElement loc = LocationElement.parseRectangularCoordinates(new double[] {eq[0], eq[1], eq[2]});

		// Create an static star at this position in FK5 J2000
		StarElement star = new StarElement("", loc.getLongitude(), loc.getLatitude(), 0,
				0.0f, 0.0f, 0.0f, 0.0f, Constant.J2000, EphemerisElement.FRAME.FK5);

		// Transform coordinates.
		star = StarEphem.transform_FK5_J2000_to_FK4_B1950(star, null);

		// Pass to LocationElement
		LocationElement loc_out = new LocationElement(star.rightAscension, star.declination, loc.getRadius());

		// Pass to rectangular coordinates
		double eq_out[] = LocationElement.parseLocationElement(loc_out);

		return eq_out;
	}

	/**
	 * Transforms FK5 coordinates to FK4, supposing that the
	 * object has no proper motion or is far away. The equinoxes for the
	 * input/output positions can be selected.
	 *
	 * @param eq Equatorial coordinates FK5 Jxxxx.
	 * @param jdFK5 The equinox for the input equatorial coordinates.
	 * @param jdFK4 The equinox desired for the output FK4 coordinates.
	 * @param eph The ephemeris object selecting the precession algorithms.
	 * @return FK4 Bxxxx coordinates.
	 * @throws JPARSECException Should not be thrown.
	 */
	public static double[] FK5_JxxxxToFK4_Bxxxx(double eq[], double jdFK5, double jdFK4, EphemerisElement eph) throws JPARSECException
	{
		// To J2000
		eq = Precession.precess(jdFK5, Constant.J2000, eq, eph);

		// To B1950 FK4
		eq = FK5_J2000ToFK4_B1950(eq);
		LocationElement out = LocationElement.parseRectangularCoordinates(eq);

		// Apply correction for the precession constant between Newcomb and IAU 1976 precession methods: 1.13"/century.
		if (jdFK4 != Constant.B1950) {
			double eqc = (jdFK4 - Constant.B1950) * 0.07555 * 15.0 * Constant.ARCSEC_TO_RAD / Constant.JULIAN_DAYS_PER_CENTURY;
			out = LocationElement.parseRectangularCoordinates(Ephem.equatorialToEcliptic(eq, Constant.B1950, eph));
			out.setLongitude(out.getLongitude()-eqc);
			out = LocationElement.parseRectangularCoordinates(Ephem.eclipticToEquatorial(out.getRectangularCoordinates(), Constant.B1950, eph));
		}

		// Transform to output (input!) epoch using IAU 1976 precession
		out = LocationElement.parseRectangularCoordinates(Precession.precess(Constant.B1950, jdFK4, out.getRectangularCoordinates(), eph));

		return out.getRectangularCoordinates();
	}

	/**
	 * Transforms B1950 coordinates (FK4 system) to J2000 or another equinox, supposing that the
	 * object has no proper motion or is far away.
	 *
	 * @param eq Equatorial coordinates FK4 B1950.
	 * @param jdFK5 The epoch of the output coordinates as a Julian day.
	 * @param jdFK4 The epoch of observation as a Julian day, in case it is not B1950.
	 * @param eph The ephemeris object selecting the precession algorithms.
	 * @return FK5 Jxxxx (jdFK5) coordinates.
	 * @throws JPARSECException Should not be thrown.
	 */
	public static double[] FK4_BxxxxToFK5_Jxxxx(double eq[], double jdFK5, double jdFK4, EphemerisElement eph) throws JPARSECException
	{
		// Pass to spherical
		LocationElement loc = LocationElement.parseRectangularCoordinates(new double[] {eq[0], eq[1], eq[2]});

		// Create an static star at this position in FK4 B1950
		StarElement star = new StarElement("", loc.getLongitude(), loc.getLatitude(), 0,
				0.0f, 0.0f, 0.0f, 0.0f, jdFK4, EphemerisElement.FRAME.FK4);

		// Transform coordinates
		star = StarEphem.transform_FK4_B1950_to_FK5_J2000(star);

		// Pass to LocationElement
		LocationElement loc_out = new LocationElement(star.rightAscension, star.declination, loc.getRadius());

		// Pass to rectangular coordinates
		double eq_out[] = LocationElement.parseLocationElement(loc_out);

		eq_out = Precession.precess(Constant.J2000, jdFK5, eq_out, eph);

		return eq_out;
	}

	/**
	 * Transforms coordinates for precession between two dates, using the method
	 * by Newcomb. This is an old method implemented for the transformation of
	 * coordinates between Bessel epochs, and it is not used in JPARSEC actively.
	 * Algorithm based on Woolard and Clemence 1966 (see AA suplement, page 107).
	 * @param from Julian date of reference.
	 * @param to Julian date for the mean equinox of the output.
	 * @param p Input equatorial coordinates.
	 * @return The output coordinates.
	 */
	public static double[] precessionNewcomb(double from, double to, double p[]) {
		double t1 = (from - Constant.B1950) / (1000.0 * Constant.TROPICAL_YEAR);
		double t2 = (to - Constant.B1950) / (1000.0 * Constant.TROPICAL_YEAR);
		double tau = t2 - t1;

		double zeta1 = 23035.545 + 139.720 * t1 + 0.060 * t1 * t1, zeta2 = 30.240 - 0.270 * t1;
		double z1 = zeta1, z2 = 109.480 + 0.390 * t1;
		double theta1 = 20051.12 - 85.29 * t1 - 0.37 * t1 * t1, theta2 = -42.65 - 0.37 * t1;

		double zeta = zeta1 * tau + zeta2 * tau * tau + 17.995 * tau * tau * tau;
		double z = z1 * tau + z2 * tau * tau + 18.325 * tau * tau * tau;
		double theta = theta1 * tau + theta2 * tau * tau - 41.80 * tau * tau * tau;

		zeta *= Constant.ARCSEC_TO_RAD;
		z *= Constant.ARCSEC_TO_RAD;
		theta *= Constant.ARCSEC_TO_RAD;

		LocationElement loc = LocationElement.parseRectangularCoordinates(p);

		double term1 = Math.sin(loc.getLongitude() + zeta) * Math.cos(loc.getLatitude());
		double term2 = Math.cos(loc.getLongitude() + zeta) * Math.cos(theta) * Math.cos(loc.getLatitude()) - Math.sin(theta) * Math.sin(loc.getLatitude());
		double term3 = Math.cos(loc.getLongitude() + zeta) * Math.sin(theta) * Math.cos(loc.getLatitude()) + Math.cos(theta) * Math.sin(loc.getLatitude());

		double alfa = Math.atan2(term1, term2) + z;
		double delta = Math.asin(term3);

		loc = new LocationElement(alfa, delta, loc.getRadius());
		return LocationElement.parseLocationElement(loc);
	}

	/**
	 * Transforms coordinates for precession between J2000 and another date,
	 * using the method by Lieske. This is an old method implemented here to
	 * support old reduction models.
	 * @param from Julian day of input vector (equatorial rectangular) in TT.
	 * @param to Julian day of output in TT. Either from or to must be equal to J2000.
	 * @param p Input equatorial coordinates.
	 * @return The output coordinates.
	 * @throws JPARSECException If from and to are not equal to J2000 epoch.
	 */
	public static double[] precessionIAU1976(double from, double to, double p[]) throws JPARSECException {
		if (from != Constant.J2000 && to != Constant.J2000) throw new JPARSECException("Precession must be from or to J2000 epoch.");
		double J = from;
		if (J == Constant.J2000) J = to;
		double t = Functions.toCenturies(J);

		double zeta = 2306.2181 * t + 0.30188 * t * t + 0.017998 * t * t * t;
		double z = 2306.2181 * t + 1.09468 * t * t + 0.018203 * t * t * t;
		double theta = 2004.3109 * t - 0.42665 * t * t - 0.041833 * t * t * t;

		zeta *= Constant.ARCSEC_TO_RAD;
		z *= Constant.ARCSEC_TO_RAD;
		theta *= Constant.ARCSEC_TO_RAD;

		double sinth = Math.sin(theta), costh = Math.cos(theta),
			sinZ = Math.sin(zeta), cosZ = Math.cos(zeta), sinz = Math.sin(z), cosz = Math.cos(z);
		double A = cosZ*costh;
		double B = sinZ*costh;

		double x[] = new double[3];
		if( from == Constant.J2000) {
			/* From J2000.0 to J */
			x[0] =    (A*cosz - sinZ*sinz)*p[0] - (B*cosz + cosZ*sinz)*p[1] - sinth*cosz*p[2];
			x[1] =    (A*sinz + sinZ*cosz)*p[0] - (B*sinz - cosZ*cosz)*p[1] - sinth*sinz*p[2];
			x[2] =    cosZ*sinth*p[0] - sinZ*sinth*p[1] + costh*p[2];
		} else {
				/* From J to J2000.0 */
			x[0] =    (A*cosz - sinZ*sinz)*p[0] + (A*sinz + sinZ*cosz)*p[1] + cosZ*sinth*p[2];
			x[1] =   -(B*cosz + cosZ*sinz)*p[0] - (B*sinz - cosZ*cosz)*p[1] - sinZ*sinth*p[2];
			x[2] =   -sinth*cosz*p[0] - sinth*sinz*p[1] + costh*p[2];
		}
		return x;
	}
}
