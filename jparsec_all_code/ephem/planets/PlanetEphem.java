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
package jparsec.ephem.planets;

import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.EphemerisElement.ALGORITHM;
import jparsec.ephem.EphemerisElement.COORDINATES_TYPE;
import jparsec.ephem.EphemerisElement.FRAME;
import jparsec.ephem.EphemerisElement.REDUCTION_METHOD;
import jparsec.ephem.Functions;
import jparsec.ephem.IAU2006;
import jparsec.ephem.Nutation;
import jparsec.ephem.PhysicalParameters;
import jparsec.ephem.Precession;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.event.LunarEvent;
import jparsec.ephem.moons.MoonEphem;
import jparsec.ephem.planets.imcce.Vsop;
import jparsec.graph.DataSet;
import jparsec.math.Constant;
import jparsec.math.matrix.Matrix;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.SiderealTime;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.time.TimeScale;
import jparsec.util.DataBase;
import jparsec.util.JPARSECException;

/**
 * A class for planetary and lunar ephemeris calculations, based on a fit to JPL
 * DE404 ephemeris over the interval 3000 B.D to 3000 A.D for giant planets, and
 * 1500 B.D. to 3000 A.D. for inner planets (including Mars). Errors are in the
 * arcsecond level when comparing to the long time span ephemeris JPL DE406.
 * <P>
 * This code is based on the work by Steve L. Moshier.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @author S. L. Moshier
 * @version 1.0
 */
public class PlanetEphem
{
	// private constructor so that this class cannot be instantiated.
	private PlanetEphem() {}

	/* Values of mean elements at Julian date J. */
	private static double LP_equinox;
	private static double NF_arcsec;
	private static double Ea_arcsec;
	private static double pA_precession;
	private static double lastJ[], lastArg[][], lastLP[], lastNF[], lastEa[], lastpA[];

	/**
	 * Obtain mean elements of the planets.
	 *
	 * @param J Julian day.
	 * @return An array with the mean longitudes.
	 */
	private synchronized static double[] meanElements(double J)
	{
		if (lastArg != null && lastJ != null && lastJ[0] == J && lastArg[0] != null) {
			LP_equinox = lastLP[0];
			NF_arcsec = lastNF[0];
			Ea_arcsec = lastEa[0];
			pA_precession = lastpA[0];
			return lastArg[0].clone();
		}
		if (lastArg != null && lastJ != null && lastJ[1] == J && lastArg[1] != null) {
			LP_equinox = lastLP[1];
			NF_arcsec = lastNF[1];
			Ea_arcsec = lastEa[1];
			pA_precession = lastpA[1];
			return lastArg[1].clone();
		}
		if (lastArg != null && lastJ != null && lastJ[2] == J && lastArg[2] != null) {
			LP_equinox = lastLP[2];
			NF_arcsec = lastNF[2];
			Ea_arcsec = lastEa[2];
			pA_precession = lastpA[2];
			return lastArg[2].clone();
		}

		int index = 0;
		if (lastJ == null) {
			lastJ = new double[3];
			lastArg = new double[3][];
			lastLP = new double[3];
			lastNF = new double[3];
			lastEa = new double[3];
			lastpA = new double[3];
		} else {
			double dif0 = Math.abs(lastJ[0]-J);
			double dif1 = Math.abs(lastJ[1]-J);
			double dif2 = Math.abs(lastJ[2]-J);
			if (lastJ[1] == 0 || dif1 > dif0 && dif1 > dif2) index = 1;
			if (index == 0 && (lastJ[2] == 0 || dif2 > dif0 && dif2 > dif1)) index = 2;
		}
		
		double x, T, T2;

		double Args[] = new double[20];

		/* Time variables. T is in Julian centuries. */
		T = Functions.toCenturies(J);
		T2 = T * T;

		/*
		 * Mean longitudes of planets (Simon et al, 1994) .047" subtracted from
		 * constant term for offset to DE403 origin.
		 */

		/* Mercury */
		x = (538101628.6889819 * T + 908103.213);
		x += (6.39e-6 * T - 0.0192789) * T2;
		Args[0] = Constant.ARCSEC_TO_RAD * x;

		/* Venus */
		x = (210664136.4335482 * T + 655127.236);
		x += (-6.27e-6 * T + 0.0059381) * T2;
		Args[1] = Constant.ARCSEC_TO_RAD * x;

		/* Earth */
		x = (129597742.283429 * T + 361679.198);
		x += (-5.23e-6 * T - 2.04411e-2) * T2;
		Ea_arcsec = Constant.ARCSEC_TO_RAD * x;
		Args[2] = Constant.ARCSEC_TO_RAD * x;

		/* Mars */
		x = (68905077.493988 * T + 1279558.751);
		x += (-1.043e-5 * T + 0.0094264) * T2;
		Args[3] = Constant.ARCSEC_TO_RAD * x;

		/* Jupiter */
		x = (10925660.377991 * T + 123665.420);
		x += ((((-3.4e-10 * T + 5.91e-8) * T + 4.667e-6) * T + 5.706e-5) * T - 3.060378e-1) * T2;
		Args[4] = Constant.ARCSEC_TO_RAD * x;

		/* Saturn */
		x = (4399609.855372 * T + 180278.752);
		x += ((((8.3e-10 * T - 1.452e-7) * T - 1.1484e-5) * T - 1.6618e-4) * T + 7.561614E-1) * T2;
		Args[5] = Constant.ARCSEC_TO_RAD * x;

		/* Uranus */
		x = (1542481.193933 * T + 1130597.971) + (0.00002156 * T - 0.0175083) * T2;
		Args[6] = Constant.ARCSEC_TO_RAD * x;

		/* Neptune */
		x = (786550.320744 * T + 1095655.149) + (-0.00000895 * T + 0.0021103) * T2;
		Args[7] = Constant.ARCSEC_TO_RAD * x;

		/* Copied from cmoon.c, DE404 version. */
		/* Mean elongation of moon = D */
		x = (1.6029616009939659e+09 * T + 1.0722612202445078e+06);
		x += (((((-3.207663637426e-013 * T + 2.555243317839e-011) * T + 2.560078201452e-009) * T - 3.702060118571e-005) * T + 6.9492746836058421e-03) * T /* D, t^3 */
				- 6.7352202374457519e+00) * T2; /* D, t^2 */
		Args[9] = Constant.ARCSEC_TO_RAD * x;

		/* Mean distance of moon from its ascending node = F */
		x = (1.7395272628437717e+09 * T + 3.3577951412884740e+05);
		x += (((((4.474984866301e-013 * T + 4.189032191814e-011) * T - 2.790392351314e-009) * T - 2.165750777942e-006) * T - 7.5311878482337989e-04) * T /* F, t^3 */
				- 1.3117809789650071e+01) * T2; /* F, t^2 */
		NF_arcsec = Constant.ARCSEC_TO_RAD * x;
		Args[10] = Constant.ARCSEC_TO_RAD * x;

		/* Mean anomaly of sun = l' (J. Laskar) */
		x = (1.2959658102304320e+08 * T + 1.2871027407441526e+06);
		x += ((((((((1.62e-20 * T - 1.0390e-17) * T - 3.83508e-15) * T + 4.237343e-13) * T + 8.8555011e-11) * T - 4.77258489e-8) * T - 1.1297037031e-5) * T + 8.7473717367324703e-05) * T - 5.5281306421783094e-01) * T2;
		Args[11] = Constant.ARCSEC_TO_RAD * x;

		/* Mean anomaly of moon = l */
		x = (1.7179159228846793e+09 * T + 4.8586817465825332e+05);
		x += (((((-1.755312760154e-012 * T + 3.452144225877e-011) * T - 2.506365935364e-008) * T - 2.536291235258e-004) * T + 5.2099641302735818e-02) * T /* l, t^3 */
				+ 3.1501359071894147e+01) * T2; /* l, t^2 */
		Args[12] = Constant.ARCSEC_TO_RAD * x;

		/* Mean longitude of moon, re mean ecliptic and equinox of date = L */
		x = (1.7325643720442266e+09 * T + 7.8593980921052420e+05);
		x += (((((7.200592540556e-014 * T + 2.235210987108e-010) * T - 1.024222633731e-008) * T - 6.073960534117e-005) * T + 6.9017248528380490e-03) * T /* L, t^3 */
				- 5.6550460027471399e+00) * T2; /* L, t^2 */
		LP_equinox = Constant.ARCSEC_TO_RAD * x;
		Args[13] = Constant.ARCSEC_TO_RAD * x;

		/* Precession of the equinox */
		x = (((((((((-8.66e-20 * T - 4.759e-17) * T + 2.424e-15) * T + 1.3095e-12) * T + 1.7451e-10) * T - 1.8055e-8) * T - 0.0000235316) * T + 0.000076) * T + 1.105414) * T + 5028.791959) * T;
		/* Moon's longitude re fixed J2000 equinox. */
		pA_precession = Constant.ARCSEC_TO_RAD * x;

		/* Lunar free librations. */
		/* 74.7 years. Denoted W or LA. */
		x = (-0.112 * T + 1.73655499e6) * T - 389552.81;
		Args[14] = Constant.ARCSEC_TO_RAD * (x);

		/* 2.891725 years. Denoted LB. */
		Args[15] = Constant.ARCSEC_TO_RAD * (4.48175409e7 * T + 806045.7);

		/* 24.2 years. Denoted P or LC. */
		Args[16] = Constant.ARCSEC_TO_RAD * (5.36486787e6 * T - 391702.8);

		/* Usual node term re equinox of date, denoted NA. */
		Args[17] = LP_equinox - NF_arcsec;

		/* Fancy node term, denoted NB. */
		/* Capital Pi of ecliptic motion (Williams 1994). */
		x = (((-0.000004 * T + 0.000026) * T + 0.153382) * T - 867.919986) * T + 629543.967373;
		Args[18] = Args[17] + Constant.ARCSEC_TO_RAD * (3.24e5 - x) - pA_precession;

		lastLP[index] = LP_equinox;
		lastNF[index] = NF_arcsec;
		lastEa[index] = Ea_arcsec;
		lastpA[index] = pA_precession;
		lastJ[index] = J;
		lastArg[index] = Args.clone();
		return Args;
	}

	/**
	 * Generic program to accumulate sum of trigonometric series in three
	 * variables (e.g., longitude, latitude, radius) of the same list of
	 * arguments.
	 *
	 * @param J Julian day.
	 * @param arg_tbl
	 * @param distance
	 * @param lat_tbl
	 * @param lon_tbl
	 * @param rad_tbl
	 * @param max_harmonic
	 * @param max_power_of_t
	 * @param maxargs
	 * @param timescale
	 * @param trunclvl
	 * @return An array with x, y, z (AU).
	 */
	private static double[] gplan(double J, int arg_tbl[], double distance, double lat_tbl[], double lon_tbl[],
			double rad_tbl[], int[] max_harmonic, int max_power_of_t, int maxargs, double timescale, double trunclvl)
	{
		int i, j, k, m, k1, ip, np, nt;
		int p[];
		double pl[];
		double pb[];
		double pr[];
		double su, cu, sv, cv;
		double T, t, sl, sb, sr;

		T = (J - Constant.J2000) / timescale;

		/* From Simon et al (1994) */
		double freqs[] = {
			/* Arc sec per 10000 Julian years. */
			53810162868.8982, 21066413643.3548, 12959774228.3429, 6890507749.3988, 1092566037.7991, 439960985.5372,
				154248119.3933, 78655032.0744, 52272245.1795 };
		double phases[] = {
			/* Arc sec. */
			252.25090552 * 3600., 181.97980085 * 3600., 100.46645683 * 3600., 355.43299958 * 3600., 34.35151874 * 3600.,
				50.07744430 * 3600., 314.05500511 * 3600., 304.34866548 * 3600., 860492.1546, };

		/* Calculate sin( i*MM ), etc. for needed multiple angles. */
		double ss[][] = new double[20][41];
		double cc[][] = new double[20][41];
		for (i = 0; i < 9; i++)
		{
			if ((max_harmonic[i]) > 0)
			{
				sr = (Functions.mod3600(freqs[i] * T) + phases[i]) * Constant.ARCSEC_TO_RAD;
				sscc(i, sr, max_harmonic[i], ss, cc);
			}
		}

		/* Point to start of table of arguments. */
		p = arg_tbl;

		/* Point to tabulated cosine and sine amplitudes. */
		pl = lon_tbl;
		pb = lat_tbl;
		pr = rad_tbl;

		sl = 0.0;
		sb = 0.0;
		sr = 0.0;

		np = 0;
		nt = 0;
		cu = 0;

		int p_index = -1;
		int pl_index = -1;
		int pb_index = -1;
		int pr_index = -1;

		for (;;)
		{
			/* argument of sine and cosine */
			/* Number of periodic arguments. */
			p_index++;
			np = p[p_index];
			if (np < 0)
				break;
			if (np == 0)
			{ /* It is a polynomial term. */
				p_index++;
				nt = p[p_index];
				/* "Longitude" polynomial (phi). */
				pl_index++;
				cu = pl[pl_index];
				for (ip = 0; ip < nt; ip++)
				{
					pl_index++;
					cu = cu * T + pl[pl_index];
				}
				sl += Functions.mod3600(cu);
				/* "Latitude" polynomial (theta). */
				pb_index++;
				cu = pb[pb_index];
				for (ip = 0; ip < nt; ip++)
				{
					pb_index++;
					cu = cu * T + pb[pb_index];
				}
				sb += cu;
				/* Radius polynomial (psi). */
				pr_index++;
				cu = pr[pr_index];
				for (ip = 0; ip < nt; ip++)
				{
					pr_index++;
					cu = cu * T + pr[pr_index];
				}
				sr += cu;
				continue;
			}

			k1 = 0;
			cv = 0.0;
			sv = 0.0;
			for (ip = 0; ip < np; ip++)
			{
				/* What harmonic. */
				p_index++;
				j = p[p_index];
				/* Which planet. */
				p_index++;
				m = p[p_index] - 1;
				if (j != 0)
				{
					k = Math.abs(j) - 1;

					su = ss[m][k]; /* sin(k*angle) */
					if (j < 0)
						su = -su;

					cu = cc[m][k];
					if (k1 == 0)
					{ /* set first angle */
						sv = su;
						cv = cu;
						k1 = 1;
					} else
					{ /* combine angles */
						t = su * cv + cu * sv;
						cv = cu * cv - su * sv;
						sv = t;
					}
				}
			}

			/* Highest power of T. */
			p_index++;
			nt = p[p_index];
			/* Longitude. */
			pl_index++;
			cu = pl[pl_index];
			pl_index++;
			su = pl[pl_index];
			for (ip = 0; ip < nt; ip++)
			{
				pl_index++;
				cu = cu * T + pl[pl_index];
				pl_index++;
				su = su * T + pl[pl_index];
			}
			sl += cu * cv + su * sv;
			/* Latitude. */
			pb_index++;
			cu = pb[pb_index];
			pb_index++;
			su = pb[pb_index];
			for (ip = 0; ip < nt; ip++)
			{
				pb_index++;
				cu = cu * T + pb[pb_index];
				pb_index++;
				su = su * T + pb[pb_index];
			}
			sb += cu * cv + su * sv;
			/* Radius. */
			pr_index++;
			cu = pr[pr_index];
			pr_index++;
			su = pr[pr_index];
			for (ip = 0; ip < nt; ip++)
			{
				pr_index++;
				cu = cu * T + pr[pr_index];
				pr_index++;
				su = su * T + pr[pr_index];
			}
			sr += cu * cv + su * sv;
		}

		if (distance == 0.0) return new double[] {
				Functions.normalizeRadians(Constant.ARCSEC_TO_RAD * sl),
				Functions.normalizeRadians(Constant.ARCSEC_TO_RAD * sb),
				Functions.normalizeRadians(Constant.ARCSEC_TO_RAD * sr)};

		double pobj[] = new double[3];
		pobj[0] = Constant.ARCSEC_TO_RAD * sl;
		pobj[1] = Constant.ARCSEC_TO_RAD * sb;
		pobj[2] = distance * (1.0 + Constant.ARCSEC_TO_RAD * sr);

		double x = pobj[2] * Math.cos(pobj[0]) * Math.cos(pobj[1]);
		double y = pobj[2] * Math.sin(pobj[0]) * Math.cos(pobj[1]);
		double z = pobj[2] * Math.sin(pobj[1]);

		return new double[] { x, y, z };
	}

	/**
	 * Generic program to accumulate sum of trigonometric series in three
	 * variables (e.g., longitude, latitude, radius) of the same list of
	 * arguments.
	 *
	 * @param J Julian day.
	 * @param arg_tbl
	 * @param distance
	 * @param lat_tbl
	 * @param lon_tbl
	 * @param rad_tbl
	 * @param max_harmonic
	 * @param max_power_of_t
	 * @param maxargs
	 * @param timescale
	 * @param trunclvl
	 * @return An array with x, y, z (AU).
	 */
	private static double[] g3plan(double J, int arg_tbl[], double distance, int lat_tbl[], int lon_tbl[],
			int rad_tbl[], int[] max_harmonic, int max_power_of_t, int maxargs, double timescale, double trunclvl,
			boolean libration)
	{
		int i, j, k, m, k1, ip, np, nt;
		double su, cu, sv, cv;
		double T, t, sl, sb, sr;

		double args[] = PlanetEphem.meanElements(J);
		if (libration) args[13] -= PlanetEphem.pA_precession; // Only librations
		T = (J - Constant.J2000) / timescale;

		/* Calculate sin( i*MM ), etc. for needed multiple angles. */
		double ss[][] = new double[20][41];
		double cc[][] = new double[20][41];
		for (i = 0; i < maxargs; i++)
		{
			if ((max_harmonic[i]) > 0)
			{
				sscc(i, args[i], max_harmonic[i], ss, cc);
			}
		}

		sl = 0.0;
		sb = 0.0;
		sr = 0.0;

		np = 0;
		nt = 0;
		cu = 0;

		int p_index = -1;
		int pl_index = -1;
		int pb_index = -1;
		int pr_index = -1;

		for (;;)
		{
			/* argument of sine and cosine */
			/* Number of periodic arguments. */
			p_index++;
			np = arg_tbl[p_index];
			if (np < 0)
				break;
			if (np == 0)
			{ /* It is a polynomial term. */
				p_index++;
				nt = arg_tbl[p_index];
				/* "Longitude" polynomial (phi). */
				pl_index++;
				cu = lon_tbl[pl_index];
				for (ip = 0; ip < nt; ip++)
				{
					pl_index++;
					cu = cu * T + lon_tbl[pl_index];
				}
				sl += cu;
				/* "Latitude" polynomial (theta). */
				pb_index++;
				cu = lat_tbl[pb_index];
				for (ip = 0; ip < nt; ip++)
				{
					pb_index++;
					cu = cu * T + lat_tbl[pb_index];
				}
				sb += cu;
				/* Radius polynomial (psi). */
				pr_index++;
				cu = rad_tbl[pr_index];
				for (ip = 0; ip < nt; ip++)
				{
					pr_index++;
					cu = cu * T + rad_tbl[pr_index];
				}
				sr += cu;
				continue;
			}

			k1 = 0;
			cv = 0.0;
			sv = 0.0;
			for (ip = 0; ip < np; ip++)
			{
				/* What harmonic. */
				p_index++;
				j = arg_tbl[p_index];
				/* Which planet. */
				p_index++;
				m = arg_tbl[p_index] - 1;
				if (j != 0)
				{
					k = Math.abs(j) - 1;

					su = ss[m][k]; /* sin(k*angle) */
					if (j < 0)
						su = -su;

					cu = cc[m][k];
					if (k1 == 0)
					{ /* set first angle */
						sv = su;
						cv = cu;
						k1 = 1;
					} else
					{ /* combine angles */
						t = su * cv + cu * sv;
						cv = cu * cv - su * sv;
						sv = t;
					}
				}
			}

			/* Highest power of T. */
			p_index++;
			nt = arg_tbl[p_index];
			/* Longitude. */
			pl_index++;
			cu = lon_tbl[pl_index];
			pl_index++;
			su = lon_tbl[pl_index];
			for (ip = 0; ip < nt; ip++)
			{
				pl_index++;
				cu = cu * T + lon_tbl[pl_index];
				pl_index++;
				su = su * T + lon_tbl[pl_index];
			}
			sl += cu * cv + su * sv;
			/* Latitude. */
			pb_index++;
			cu = lat_tbl[pb_index];
			pb_index++;
			su = lat_tbl[pb_index];
			for (ip = 0; ip < nt; ip++)
			{
				pb_index++;
				cu = cu * T + lat_tbl[pb_index];
				pb_index++;
				su = su * T + lat_tbl[pb_index];
			}
			sb += cu * cv + su * sv;
			/* Radius. */
			pr_index++;
			cu = rad_tbl[pr_index];
			pr_index++;
			su = rad_tbl[pr_index];
			for (ip = 0; ip < nt; ip++)
			{
				pr_index++;
				cu = cu * T + rad_tbl[pr_index];
				pr_index++;
				su = su * T + rad_tbl[pr_index];
			}
			sr += cu * cv + su * sv;
		}

		sl = sl * 0.0001;
		sb = sb * 0.0001;
		sr = sr * 0.0001;

		if (distance == 0.0) return new double[] {Constant.ARCSEC_TO_RAD * sl + PlanetEphem.Ea_arcsec, Constant.ARCSEC_TO_RAD * sb,
				Constant.ARCSEC_TO_RAD * sr};

		double pobj[] = new double[3];
		pobj[0] = Constant.ARCSEC_TO_RAD * sl + PlanetEphem.Ea_arcsec;
		pobj[1] = Constant.ARCSEC_TO_RAD * sb;
		pobj[2] = distance * (1.0 + Constant.ARCSEC_TO_RAD * sr);

		double x = pobj[2] * Math.cos(pobj[0]) * Math.cos(pobj[1]);
		double y = pobj[2] * Math.sin(pobj[0]) * Math.cos(pobj[1]);
		double z = pobj[2] * Math.sin(pobj[1]);

		return new double[] { x, y, z };
	}

	/**
	 * Generic program to accumulate sum of trigonometric series in two
	 * variables (e.g., longitude, radius) of the same list of arguments.
	 *
	 * @param J Julian day.
	 * @param arg_tbl
	 * @param distance
	 * @param lat_tbl
	 * @param lon_tbl
	 * @param rad_tbl
	 * @param max_harmonic
	 * @param max_power_of_t
	 * @param maxargs
	 * @param timescale
	 * @param trunclvl
	 * @return An array with x, y, z (AU).
	 */
	private static double[] g2plan(double J, int arg_tbl[], double distance, int lat_tbl[], int lon_tbl[],
			int rad_tbl[], int[] max_harmonic, int max_power_of_t, int maxargs, double timescale, double trunclvl,
			double lat)
	{
		int i, j, k, m, k1, ip, np, nt;
		double su, cu, sv, cv;
		double T, t, sl, sr;

		double args[] = PlanetEphem.meanElements(J);
		T = (J - Constant.J2000) / timescale;

		/* Calculate sin( i*MM ), etc. for needed multiple angles. */
		double ss[][] = new double[20][41];
		double cc[][] = new double[20][41];
		for (i = 0; i < maxargs; i++)
		{
			if ((max_harmonic[i]) > 0)
			{
				sscc(i, args[i], max_harmonic[i], ss, cc);
			}
		}

		sl = 0.0;
		sr = 0.0;

		np = 0;
		nt = 0;
		cu = 0;

		int p_index = -1;
		int pl_index = -1;
		int pr_index = -1;

		for (;;)
		{
			/* argument of sine and cosine */
			/* Number of periodic arguments. */
			p_index++;
			np = arg_tbl[p_index];
			if (np < 0)
				break;
			if (np == 0)
			{ /* It is a polynomial term. */
				p_index++;
				nt = arg_tbl[p_index];
				/* "Longitude" polynomial (phi). */
				pl_index++;
				cu = lon_tbl[pl_index];
				for (ip = 0; ip < nt; ip++)
				{
					pl_index++;
					cu = cu * T + lon_tbl[pl_index];
				}
				sl += cu;
				/* Radius polynomial (psi). */
				pr_index++;
				cu = rad_tbl[pr_index];
				for (ip = 0; ip < nt; ip++)
				{
					pr_index++;
					cu = cu * T + rad_tbl[pr_index];
				}
				sr += cu;
				continue;
			}

			k1 = 0;
			cv = 0.0;
			sv = 0.0;
			for (ip = 0; ip < np; ip++)
			{
				/* What harmonic. */
				p_index++;
				j = arg_tbl[p_index];
				/* Which planet. */
				p_index++;
				m = arg_tbl[p_index] - 1;
				if (j != 0)
				{
					k = Math.abs(j) - 1;

					su = ss[m][k]; /* sin(k*angle) */
					if (j < 0)
						su = -su;

					cu = cc[m][k];
					if (k1 == 0)
					{ /* set first angle */
						sv = su;
						cv = cu;
						k1 = 1;
					} else
					{ /* combine angles */
						t = su * cv + cu * sv;
						cv = cu * cv - su * sv;
						sv = t;
					}
				}
			}

			/* Highest power of T. */
			p_index++;
			nt = arg_tbl[p_index];
			/* Longitude. */
			pl_index++;
			cu = lon_tbl[pl_index];
			pl_index++;
			su = lon_tbl[pl_index];
			for (ip = 0; ip < nt; ip++)
			{
				pl_index++;
				cu = cu * T + lon_tbl[pl_index];
				pl_index++;
				su = su * T + lon_tbl[pl_index];
			}
			sl += cu * cv + su * sv;
			/* Radius. */
			pr_index++;
			cu = rad_tbl[pr_index];
			pr_index++;
			su = rad_tbl[pr_index];
			for (ip = 0; ip < nt; ip++)
			{
				pr_index++;
				cu = cu * T + rad_tbl[pr_index];
				pr_index++;
				su = su * T + rad_tbl[pr_index];
			}
			sr += cu * cv + su * sv;
		}

		double pobj[] = new double[3];
		sl = sl * 0.0001;
		sr = sr * 0.0001;

		if (distance == 0.0) return new double[] {Constant.ARCSEC_TO_RAD * sl + PlanetEphem.LP_equinox,
				lat, Constant.ARCSEC_TO_RAD * sr};

		pobj[0] = Constant.ARCSEC_TO_RAD * sl + PlanetEphem.LP_equinox;
		pobj[1] = lat;
		pobj[2] = distance * (1.0 + Constant.ARCSEC_TO_RAD * sr);

		double x = pobj[2] * Math.cos(pobj[0]) * Math.cos(pobj[1]);
		double y = pobj[2] * Math.sin(pobj[0]) * Math.cos(pobj[1]);
		double z = pobj[2] * Math.sin(pobj[1]);

		return new double[] { x, y, z };
	}

	/**
	 * Generic program to accumulate sum of trigonometric series in one
	 * variables (e.g., latitude) of the same list of arguments.
	 *
	 * @param J Julian day.
	 * @param arg_tbl
	 * @param distance
	 * @param lat_tbl
	 * @param lon_tbl
	 * @param rad_tbl
	 * @param max_harmonic
	 * @param max_power_of_t
	 * @param maxargs
	 * @param timescale
	 * @param trunclvl
	 * @return Latitude (rad).
	 */
	private static double g1plan(double J, int arg_tbl[], double distance, int lat_tbl[], int lon_tbl[],
			int rad_tbl[], int[] max_harmonic, int max_power_of_t, int maxargs, double timescale, double trunclvl)
	{
		int i, j, k, m, k1, ip, np, nt;
		double su, cu, sv, cv;
		double T, t, sb;

		double args[] = PlanetEphem.meanElements(J);
		T = (J - Constant.J2000) / timescale;

		/* Calculate sin( i*MM ), etc. for needed multiple angles. */
		double ss[][] = new double[20][41];
		double cc[][] = new double[20][41];
		for (i = 0; i < maxargs; i++)
		{
			if ((max_harmonic[i]) > 0)
			{
				sscc(i, args[i], max_harmonic[i], ss, cc);
			}
		}

		sb = 0.0;
		np = 0;
		nt = 0;
		cu = 0;

		int p_index = -1;
		int pb_index = -1;

		for (;;)
		{
			/* argument of sine and cosine */
			/* Number of periodic arguments. */
			p_index++;
			np = arg_tbl[p_index];
			if (np < 0)
				break;
			if (np == 0)
			{ /* It is a polynomial term. */
				p_index++;
				nt = arg_tbl[p_index];
				/* "Latitude" polynomial (theta). */
				pb_index++;
				cu = lat_tbl[pb_index];
				for (ip = 0; ip < nt; ip++)
				{
					pb_index++;
					cu = cu * T + lat_tbl[pb_index];
				}
				sb += cu;
				continue;
			}

			k1 = 0;
			cv = 0.0;
			sv = 0.0;
			for (ip = 0; ip < np; ip++)
			{
				/* What harmonic. */
				p_index++;
				j = arg_tbl[p_index];
				/* Which planet. */
				p_index++;
				m = arg_tbl[p_index] - 1;
				if (j != 0)
				{
					k = Math.abs(j) - 1;

					su = ss[m][k]; /* sin(k*angle) */
					if (j < 0)
						su = -su;

					cu = cc[m][k];
					if (k1 == 0)
					{ /* set first angle */
						sv = su;
						cv = cu;
						k1 = 1;
					} else
					{ /* combine angles */
						t = su * cv + cu * sv;
						cv = cu * cv - su * sv;
						sv = t;
					}
				}
			}

			/* Highest power of T. */
			p_index++;
			nt = arg_tbl[p_index];
			/* Latitude. */
			pb_index++;
			cu = lat_tbl[pb_index];
			pb_index++;
			su = lat_tbl[pb_index];
			for (ip = 0; ip < nt; ip++)
			{
				pb_index++;
				cu = cu * T + lat_tbl[pb_index];
				pb_index++;
				su = su * T + lat_tbl[pb_index];
			}
			sb += cu * cv + su * sv;
		}

		return (Constant.ARCSEC_TO_RAD * sb * 0.0001);
	}

	/**
	 * Prepare lookup table of sin and cos ( i*Lj ) for required multiple
	 * angles.
	 *
	 * @param k
	 * @param arg
	 * @param n
	 */
	private static void sscc(int k, double arg, int n, double ss[][], double cc[][])
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
	 * Obtain position of a planet. Rectangular heliocentric coordinates mean
	 * equinox and ecliptic J2000. For the Moon the geocentric position is returned.
	 * For Pluto the body center is returned instead of the barycenter, using the 
	 * latest reduction method available.
	 *
	 * @param JD Time in Julian day.
	 * @param planet Planet ID constant (From Sun to Pluto, Moon, barycenters, and libration).
	 * @return Array with the x, y, z results.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public static synchronized double[] getHeliocentricEclipticPositionJ2000(double JD, TARGET planet) throws JPARSECException
	{
		// Planet.SUN: assumed to be at the SSB, which is not fully accurate
		if (planet == TARGET.Solar_System_Barycenter || planet == TARGET.SUN) return new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0};

		double p[] = new double[] { 0.0, 0.0, 0.0 };
		EphemerisElement eph = new EphemerisElement();
		eph.ephemMethod = REDUCTION_METHOD.WILLIAMS_1994;

		switch (planet)
		{
		case Libration:
			p = g3plan(JD, Moshier_libration.args, Moshier_libration.distance, Moshier_libration.tabb, Moshier_libration.tabl,
					Moshier_libration.tabr, Moshier_libration.max_harmonic, Moshier_libration.max_power_of_t,
					Moshier_libration.maxargs, Moshier_libration.timescale, Moshier_libration.trunclvl, true);

			// For ecliptic mean equinox of date
			double args[] = PlanetEphem.meanElements(JD);
			p[0] -= args[2];

			  // phi+psi
			  p[2] += LP_equinox + 6.48e5 * Constant.ARCSEC_TO_RAD;
			  if (p[2] < -6.45e5 * Constant.ARCSEC_TO_RAD)
			    p[2] += 1.296e6 * Constant.ARCSEC_TO_RAD;
			  if (p[2] > 6.45e5 * Constant.ARCSEC_TO_RAD)
			    p[2] -= 1.296e6 * Constant.ARCSEC_TO_RAD;
			  // phi
			  p[0] += LP_equinox - NF_arcsec + 6.48e5 * Constant.ARCSEC_TO_RAD;
			  if (p[0] < -6.45e5 * Constant.ARCSEC_TO_RAD)
			    p[0] += 1.296e6 * Constant.ARCSEC_TO_RAD;
			  if (p[0] > 6.45e5 * Constant.ARCSEC_TO_RAD)
			    p[0] -= 1.296e6 * Constant.ARCSEC_TO_RAD;
			p[2] -= p[0];

			// From Euler angles to matrix M
			double sinpsi = Math.sin(p[2]);
			double cospsi = Math.cos(p[2]);
			double sinth = Math.sin(p[1]);
			double costh = Math.cos(p[1]);
			double sinphi = Math.sin(p[0]);
			double cosphi = Math.cos(p[0]);
			double a = costh*sinphi;
			double b = costh*cosphi;
			double M[][] = new double[3][3];
			M[0][0] = cospsi*cosphi - a*sinpsi;
			M[0][1] = cospsi*sinphi + b*sinpsi;
			M[0][2] = sinpsi*sinth;
			M[1][0] = -sinpsi*cosphi - a*cospsi;
			M[1][1] = -sinpsi*sinphi + b*cospsi;
			M[1][2] = cospsi*sinth;
			M[2][0] = sinth*sinphi;
			M[2][1] = -sinth*cosphi;
			M[2][2] = costh;
			Matrix mM = new Matrix(M);

			// Get rotation matrix around x axis with eps
			Matrix mQ = Matrix.getR1(84381.406173 * Constant.ARCSEC_TO_RAD);

			// Get precession matrix
			eph.ephemMethod = REDUCTION_METHOD.JPL_DE4xx;
			p = Precession.getAngles(false, JD, eph);
			double p0 = p[1], p1 = p[2], p2 = -(p[1]+p[0]);
			p[0] = p0;
			p[1] = p1;
			p[2] = p2;
				// From Euler angles to matrix M
				sinpsi = Math.sin(p[2]);
				cospsi = Math.cos(p[2]);
				sinth = Math.sin(p[1]);
				costh = Math.cos(p[1]);
				sinphi = Math.sin(p[0]);
				cosphi = Math.cos(p[0]);
				a = costh*sinphi;
				b = costh*cosphi;
				double P[][] = new double[3][3];
				P[0][0] = cospsi*cosphi - a*sinpsi;
				P[0][1] = cospsi*sinphi + b*sinpsi;
				P[0][2] = sinpsi*sinth;
				P[1][0] = -sinpsi*cosphi - a*cospsi;
				P[1][1] = -sinpsi*sinphi + b*cospsi;
				P[1][2] = cospsi*sinth;
				P[2][0] = sinth*sinphi;
				P[2][1] = -sinth*cosphi;
				P[2][2] = costh;
			Matrix mP = new Matrix(P);

			// Precess Q
			mP = mP.times(mQ);

			// Space to body
			Matrix mM2000 = mM.times(mP);

			// Get back the Euler angles, now equatorial (as JPL ones)
			  M = mM2000.getArray();
			  double phi = Math.atan2(  M[2][0], -M[2][1]);
			  a = M[0][2];
			  b = M[1][2];
			  /* psi = zatan2( b, a ); */
			  double psi = Math.atan2( a, b );

			  if( Math.abs(a) > Math.abs(b) )
			  	a = a/Math.sin(psi);
			  else
			  	a = b/Math.cos(psi);

			  /* theta = zatan2( M[2][2], a ); */
			  double theta = Math.atan2( a, M[2][2]);

			  p[0] = phi;
			  p[1] = theta;
			  p[2] = psi;

			return p;
		case MERCURY:
			p = gplan(JD, Moshier_Mercury.args, Moshier_Mercury.distance, Moshier_Mercury.tabb, Moshier_Mercury.tabl,
					Moshier_Mercury.tabr, Moshier_Mercury.max_harmonic, Moshier_Mercury.max_power_of_t,
					Moshier_Mercury.maxargs, Moshier_Mercury.timescale, Moshier_Mercury.trunclvl);
			break;
		case VENUS:
			p = gplan(JD, Moshier_Venus.args, Moshier_Venus.distance, Moshier_Venus.tabb, Moshier_Venus.tabl,
					Moshier_Venus.tabr, Moshier_Venus.max_harmonic, Moshier_Venus.max_power_of_t,
					Moshier_Venus.maxargs, Moshier_Venus.timescale, Moshier_Venus.trunclvl);
			break;
		case Earth_Moon_Barycenter:
			p = g3plan(JD, Moshier_Earth_Moon_Barycenter.args, Moshier_Earth_Moon_Barycenter.distance,
					Moshier_Earth_Moon_Barycenter.tabb, Moshier_Earth_Moon_Barycenter.tabl,
					Moshier_Earth_Moon_Barycenter.tabr, Moshier_Earth_Moon_Barycenter.max_harmonic,
					Moshier_Earth_Moon_Barycenter.max_power_of_t, Moshier_Earth_Moon_Barycenter.maxargs,
					Moshier_Earth_Moon_Barycenter.timescale, Moshier_Earth_Moon_Barycenter.trunclvl, false);
			break;
		case MARS:
			p = gplan(JD, Moshier_Mars.args, Moshier_Mars.distance, Moshier_Mars.tabb, Moshier_Mars.tabl,
					Moshier_Mars.tabr, Moshier_Mars.max_harmonic, Moshier_Mars.max_power_of_t, Moshier_Mars.maxargs,
					Moshier_Mars.timescale, Moshier_Mars.trunclvl);
			break;
		case JUPITER:
			p = gplan(JD, Moshier_Jupiter.args, Moshier_Jupiter.distance, Moshier_Jupiter.tabb, Moshier_Jupiter.tabl,
					Moshier_Jupiter.tabr, Moshier_Jupiter.max_harmonic, Moshier_Jupiter.max_power_of_t,
					Moshier_Jupiter.maxargs, Moshier_Jupiter.timescale, Moshier_Jupiter.trunclvl);
			break;
		case SATURN:
			p = gplan(JD, Moshier_Saturn.args, Moshier_Saturn.distance, Moshier_Saturn.tabb, Moshier_Saturn.tabl,
					Moshier_Saturn.tabr, Moshier_Saturn.max_harmonic, Moshier_Saturn.max_power_of_t,
					Moshier_Saturn.maxargs, Moshier_Saturn.timescale, Moshier_Saturn.trunclvl);
			break;
		case URANUS:
			p = gplan(JD, Moshier_Uranus.args, Moshier_Uranus.distance, Moshier_Uranus.tabb, Moshier_Uranus.tabl,
					Moshier_Uranus.tabr, Moshier_Uranus.max_harmonic, Moshier_Uranus.max_power_of_t,
					Moshier_Uranus.maxargs, Moshier_Uranus.timescale, Moshier_Uranus.trunclvl);
			break;
		case NEPTUNE:
			p = gplan(JD, Moshier_Neptune.args, Moshier_Neptune.distance, Moshier_Neptune.tabb, Moshier_Neptune.tabl,
					Moshier_Neptune.tabr, Moshier_Neptune.max_harmonic, Moshier_Neptune.max_power_of_t,
					Moshier_Neptune.maxargs, Moshier_Neptune.timescale, Moshier_Neptune.trunclvl);
			break;
		case Pluto:
			p = gplan(JD, Moshier_Pluto.args, Moshier_Pluto.distance, Moshier_Pluto.tabb, Moshier_Pluto.tabl,
					Moshier_Pluto.tabr, Moshier_Pluto.max_harmonic, Moshier_Pluto.max_power_of_t,
					Moshier_Pluto.maxargs, Moshier_Pluto.timescale, Moshier_Pluto.trunclvl);

			// Return position of Pluto's body center
			if (planet == TARGET.Pluto) {
				p = Ephem.eclipticToEquatorial(p, Constant.J2000, eph);
				p = MoonEphem.fromPlutoBarycenterToPlutoCenter(p, JD, EphemerisElement.REDUCTION_METHOD.getLatest(), true);
				p = Ephem.equatorialToEcliptic(p, Constant.J2000, eph);
			}
			break;
		case Moon:
			double moon_lat = g1plan(JD, Moshier_Moon_lat.args, Moshier_Moon_lat.distance, Moshier_Moon_lat.tabl,
					Moshier_Moon_lat.tabb, Moshier_Moon_lat.tabr, Moshier_Moon_lat.max_harmonic,
					Moshier_Moon_lat.max_power_of_t, Moshier_Moon_lat.maxargs, Moshier_Moon_lat.timescale,
					Moshier_Moon_lat.trunclvl);
			p = g2plan(JD, Moshier_Moon_lon_rad.args, Moshier_Moon_lon_rad.distance, Moshier_Moon_lon_rad.tabb,
					Moshier_Moon_lon_rad.tabl, Moshier_Moon_lon_rad.tabr, Moshier_Moon_lon_rad.max_harmonic,
					Moshier_Moon_lon_rad.max_power_of_t, Moshier_Moon_lon_rad.maxargs, Moshier_Moon_lon_rad.timescale,
					Moshier_Moon_lon_rad.trunclvl, moon_lat);
			// Here we apply Williams formula to pass to J2000, since this is
			// the one chosen by Moshier
			p = Precession.precessPosAndVelInEcliptic(JD, Constant.J2000, p, eph);
			break;
		case EARTH:
			p = g3plan(JD, Moshier_Earth_Moon_Barycenter.args, Moshier_Earth_Moon_Barycenter.distance,
					Moshier_Earth_Moon_Barycenter.tabb, Moshier_Earth_Moon_Barycenter.tabl,
					Moshier_Earth_Moon_Barycenter.tabr, Moshier_Earth_Moon_Barycenter.max_harmonic,
					Moshier_Earth_Moon_Barycenter.max_power_of_t, Moshier_Earth_Moon_Barycenter.maxargs,
					Moshier_Earth_Moon_Barycenter.timescale, Moshier_Earth_Moon_Barycenter.trunclvl, false);
			moon_lat = g1plan(JD, Moshier_Moon_lat.args, Moshier_Moon_lat.distance, Moshier_Moon_lat.tabl,
					Moshier_Moon_lat.tabb, Moshier_Moon_lat.tabr, Moshier_Moon_lat.max_harmonic,
					Moshier_Moon_lat.max_power_of_t, Moshier_Moon_lat.maxargs, Moshier_Moon_lat.timescale,
					Moshier_Moon_lat.trunclvl);
			double p_moon[] = g2plan(JD, Moshier_Moon_lon_rad.args, Moshier_Moon_lon_rad.distance,
					Moshier_Moon_lon_rad.tabb, Moshier_Moon_lon_rad.tabl, Moshier_Moon_lon_rad.tabr,
					Moshier_Moon_lon_rad.max_harmonic, Moshier_Moon_lon_rad.max_power_of_t,
					Moshier_Moon_lon_rad.maxargs, Moshier_Moon_lon_rad.timescale, Moshier_Moon_lon_rad.trunclvl,
					moon_lat);
			// Here we apply Williams formula to pass to J2000, since this is
			// the one chosen by Moshier
			p_moon = Precession.precessPosAndVelInEcliptic(JD, Constant.J2000, p_moon, eph);

			// Earth position is found indirectly, knowing the position of the
			// Earth-Moon barycenter and the geocentric Moon
			double Earth_Moon_mass_ratio = TARGET.Moon.relativeMass / TARGET.EARTH.relativeMass;
			double c = 1.0 / (Earth_Moon_mass_ratio + 1.0);
			p[0] = p[0] - c * p_moon[0];
			p[1] = p[1] - c * p_moon[1];
			p[2] = p[2] - c * p_moon[2];

			break;
		default:
			throw new JPARSECException("Target "+planet+" is invalid.");
		}

		return p;
	}

	/**
	 * Get rectangular ecliptic geocentric position of a planet in equinox
	 * J2000, and ICRF frame.
	 *
	 * @param JD Julian day in TDB.
	 * @param planet Planet ID.
	 * @param light_time Light time to planet in days.
	 * @param addSat True to add the planetocentric position of the satellite to the position
	 * of the planet.
	 * @param obs The observer object. Can be null for the Earth's center.
	 * @return Array with x, y, z, vx, vy, vz coordinates. Note velocity components are those
	 * for the Earth (used for aberration correction) not those for the planet relative to the
	 * geocenter.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public static double[] getGeocentricPosition(double JD, TARGET planet, double light_time, boolean addSat, ObserverElement obs) throws JPARSECException
	{
		// Heliocentric position corrected for light time
		double helio_object[] = getHeliocentricEclipticPositionJ2000(JD - light_time, planet);
		if (addSat) {
			Object o = DataBase.getData("offsetPosition", true);
			if (o != null) {
				double[] planetocentricPositionOfTargetSatellite = (double[]) o;
				helio_object = Functions.sumVectors(helio_object, planetocentricPositionOfTargetSatellite);
			}
		}

		if (planet == TARGET.Moon && (obs == null || obs.getMotherBody() == TARGET.EARTH))
			return new double[] { helio_object[0], helio_object[1], helio_object[2], 0.0, 0.0, 0.0 };

		// Compute position of Earth
		double[] helio_earth = null;
		if (obs == null || obs.getMotherBody() == TARGET.EARTH || planet == TARGET.Moon) {
			helio_earth = PlanetEphem.getHeliocentricEclipticPositionJ2000(JD, TARGET.EARTH);
			double time_step = 0.1;
			double helio_earth_plus[] = PlanetEphem.getHeliocentricEclipticPositionJ2000(JD + time_step, TARGET.EARTH);
			double helio_earth_vel[] =
			{ (helio_earth_plus[0] - helio_earth[0]) / time_step, (helio_earth_plus[1] - helio_earth[1]) / time_step,
					(helio_earth_plus[2] - helio_earth[2]) / time_step };
			helio_earth = new double[] {helio_earth[0], helio_earth[1], helio_earth[2], helio_earth_vel[0], helio_earth_vel[1], helio_earth_vel[2]};
		}

		if (obs != null && obs.getMotherBody() != TARGET.EARTH) {
			if (planet == TARGET.Moon) helio_object = Functions.sumVectors(helio_earth, helio_object);
			EphemerisElement eph = new EphemerisElement();
			eph.ephemMethod = REDUCTION_METHOD.IAU_2006;
			eph.algorithm = ALGORITHM.MOSHIER;
			helio_earth = Ephem.equatorialToEcliptic(obs.heliocentricPositionOfObserver(JD, eph), Constant.J2000, eph);
		}

		// Compute geocentric position of the object, and
		// also velocity vector of the geocenter
		double geo_pos[] = new double[]
		{ -helio_earth[0] + helio_object[0], -helio_earth[1] + helio_object[1], -helio_earth[2] + helio_object[2],
				helio_earth[3], helio_earth[4], helio_earth[5], };

		return geo_pos;

	}

	/**
	 * Calculate planetary ephemeris, providing full data. This method uses
	 * Moshier's fit to JPL DE404 Ephemeris. Peak errors are below 0.5 arcsecond
	 * for planets, and below 0.1 arcseconds for the Moon, when comparing with
	 * JPL results. Typical errors are much lower. Fit is valid from 3000 B.C.
	 * to 3000 A.D. for giant planets, and from 1350 B.C. to 3000 A.D. for inner
	 * planets. To calculate approximate ephemerides of inner planets up to 3000
	 * B.C. use VSOP87, or ELP2000 for more acurrate ephemeris for the Moon before
	 * year 1600. The Moon position will contains lunar librations computed also
	 * from Moshier's fit.
	 *
	 * @param time Time object containing the date.
	 * @param obs Observer object containing the observer position.
	 * @param eph Ephemeris object with the target and ephemeris
	 *        properties.
	 * @return Ephem object containing ephemeris data. Rise, set, transit
	 * times and maximum elevation fields are not computed in this method, use
	 * {@linkplain Ephem} class for that.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public static synchronized EphemElement MoshierEphemeris(TimeElement time, // Time
																	// Element
			ObserverElement obs, // Observer Element
			EphemerisElement eph) // Ephemeris Element
			throws JPARSECException
	{
		// Check Ephemeris object
		if (!EphemerisElement.checkEphemeris(eph))
		{
			throw new JPARSECException("invalid ephemeris object.");
		}

		// Obtain julian day in Barycentric Dynamical Time
		double JD_TDB = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);

		if (lastObserver == null || !lastObserver.equals(obs) || JD_TDB != lastTDB) {
			lastSun = null;
			lastEphem2 = null;
			lastSun0 = null;
			lastBaryc = null;
			lastTDB = -1;
			lastObserver = null;
		}

		EphemElement ephem_elem = PlanetEphem.MoshierCalc(time, obs, eph, true, true);

		/* Physical ephemeris */
		EphemElement ephemSun = null;
		if (JD_TDB == lastTDB && lastSun != null) ephemSun = lastSun;
		EphemerisElement new_eph = null;
		EphemElement ephem_elem2 = ephem_elem;
		if (eph.ephemType != EphemerisElement.COORDINATES_TYPE.APPARENT || eph.equinox != EphemerisElement.EQUINOX_OF_DATE) {
			if (JD_TDB == lastTDB && lastEphem2 != null) {
				ephem_elem2 = lastEphem2;
			} else {
				new_eph = eph.clone();
				new_eph.ephemType = COORDINATES_TYPE.APPARENT;
				new_eph.equinox = EphemerisElement.EQUINOX_OF_DATE;
				ephem_elem2 = MoshierCalc(time, obs, new_eph, true, true);
				//lastEphem2 = ephem_elem2;
			}
		}
		// Priority to Moshier since performance is far better
		if (ephemSun == null) {
			if (new_eph == null) new_eph = eph.clone();
			new_eph.targetBody = TARGET.SUN;
			new_eph.ephemType = COORDINATES_TYPE.APPARENT;
			new_eph.equinox = EphemerisElement.EQUINOX_OF_DATE;
			try {
				ephemSun = MoshierCalc(time, obs, new_eph, false, false);
				lastSun = ephemSun;
			} catch (Exception exc) { // To obtain Earth position in ephemerides of giant planets before 1350 B.C.
		 		Object gcrs = DataBase.getData("GCRS", true);
				ephemSun = Vsop.vsopEphemeris(time, obs, new_eph);
				DataBase.addData("GCRS", gcrs, true);
				lastSun = ephemSun;
			}
		}
		if (lastObserver == null) lastObserver = obs.clone();

		ephem_elem2 = PhysicalParameters.physicalParameters(JD_TDB, ephemSun, ephem_elem2, obs, eph);
		PhysicalParameters.setPhysicalParameters(ephem_elem, ephem_elem2, time, obs, eph);

		/* Horizontal coordinates */
		if (eph.isTopocentric)
			ephem_elem = Ephem.horizontalCoordinates(time, obs, eph, ephem_elem);

		/* Set coordinates to the output equinox */
		if (EphemerisElement.EQUINOX_OF_DATE != eph.equinox)
			ephem_elem = Ephem.toOutputEquinox(ephem_elem, eph, JD_TDB);

		/* Obtain accurate lunar orientation. Default theory is Eckhardt's, which is already accurate. */
		if (eph.preferPrecisionInEphemerides && eph.targetBody == TARGET.Moon && obs.getMotherBody() == TARGET.EARTH) {
			double lib[] = LunarEvent.getJPLMoonLibrations(time, obs, eph, ephem_elem.getEquatorialLocation());
			ephem_elem.longitudeOfCentralMeridian = lib[0];
			ephem_elem.positionAngleOfPole = lib[1];
			ephem_elem.positionAngleOfAxis = lib[2];
		}
		return ephem_elem;
	}

	private static double lastTDB = -1;
	private static double lastSun0[] = null, lastBaryc[] = null;
	private static ObserverElement lastObserver = null;
	private static EphemElement lastSun = null, lastEphem2 = null;
	private static EphemElement MoshierCalc(TimeElement time, // Time Element
			ObserverElement obs, // Observer Element
			EphemerisElement eph, boolean addGCRS, boolean addOffset) // Ephemeris Element
			throws JPARSECException
	{
		if ((!eph.targetBody.isPlanet() && eph.targetBody != TARGET.SUN && eph.targetBody != TARGET.Pluto && eph.targetBody != TARGET.Moon)
				|| ((eph.targetBody == TARGET.EARTH || eph.targetBody == TARGET.Earth_Moon_Barycenter) && obs.getMotherBody() == TARGET.EARTH)
				)
			throw new JPARSECException("target object '"+eph.targetBody+"' is invalid.");

		// Obtain julian day in Barycentric Dynamical Time
		double JD_TDB = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);

		// Check Julian day for time span validity
		// Rigorous criteria
		if (eph.preferPrecisionInEphemerides) {
			if (JD_TDB < 1228000.5 && (eph.targetBody == TARGET.MERCURY || eph.targetBody == TARGET.VENUS || eph.targetBody == TARGET.Earth_Moon_Barycenter || eph.targetBody == TARGET.EARTH || eph.targetBody == TARGET.SUN || eph.targetBody == TARGET.Moon || eph.targetBody == TARGET.MARS))
				throw new JPARSECException("invalid date.");
			if (JD_TDB < 625296.5 && (eph.targetBody == TARGET.JUPITER || eph.targetBody == TARGET.SATURN || eph.targetBody == TARGET.URANUS || eph.targetBody == TARGET.NEPTUNE || eph.targetBody == TARGET.Pluto))
				throw new JPARSECException("invalid date.");
			if (JD_TDB > 2817057.5) throw new JPARSECException("invalid date.");
		} else {
			// Non-rigorous criteria: Moshier ephem can be used up to year -3000, with some lost of precision
			if (JD_TDB > 2817057.5 || JD_TDB < 625296.5) throw new JPARSECException("invalid date.");
		}

		// Obtain geocentric position
		double geo_eq[] = Ephem.eclipticToEquatorial(PlanetEphem.getGeocentricPosition(JD_TDB, eph.targetBody, 0.0, addOffset, obs),
				Constant.J2000, eph);

		// Obtain topocentric light_time
		LocationElement loc = LocationElement.parseRectangularCoordinates(geo_eq);
		double light_time = loc.getRadius() * Constant.LIGHT_TIME_DAYS_PER_AU;
		if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.GEOMETRIC)
			light_time = 0.0;
		if (eph.ephemType != EphemerisElement.COORDINATES_TYPE.GEOMETRIC && eph.targetBody != TARGET.SUN)
		{
			double topo[] = obs.topocentricObserverICRF(time, eph);
			geo_eq = Ephem.eclipticToEquatorial(PlanetEphem
					.getGeocentricPosition(JD_TDB, eph.targetBody, light_time, addOffset, obs), Constant.J2000, eph);
			double light_time_corrected = Ephem.getTopocentricLightTime(geo_eq, topo, eph);
			// Iterate to obtain correct light time and geocentric position.
			// Iterate to a precission up to 1E-6 seconds (below the
			// milliarsecond).
			do
			{
				light_time = light_time_corrected;
				geo_eq = Ephem.eclipticToEquatorial(PlanetEphem.getGeocentricPosition(JD_TDB, eph.targetBody,
						light_time_corrected, addOffset, obs), Constant.J2000, eph);
				light_time_corrected = Ephem.getTopocentricLightTime(geo_eq, topo, eph);
			} while (Math.abs(light_time - light_time_corrected) > (1.0E-6 / Constant.SECONDS_PER_DAY));
			light_time = light_time_corrected;
		}

		// Obtain light time to Sun (first order approx)
		double geo_sun_0[] = null;
		if (lastTDB == JD_TDB && lastSun0 != null) {
			geo_sun_0 = lastSun0;
		} else {
			geo_sun_0 = PlanetEphem.getGeocentricPosition(JD_TDB, TARGET.SUN, 0.0, false, obs);
		}
		double lightTimeS = 0.0;
		if (eph.ephemType != EphemerisElement.COORDINATES_TYPE.GEOMETRIC) {
			lightTimeS = light_time;
			if (eph.targetBody != TARGET.SUN && geo_sun_0 != null) {
				lightTimeS = Functions.getNorm(geo_sun_0) * Constant.LIGHT_TIME_DAYS_PER_AU;
			}
		}
		// Note that Moshier considers the Sun is at Solar System barycenter, so the position of barycenter
		// and the Sun is the same
		double baryc[] = null;
		if (lastTDB == JD_TDB && lastBaryc != null) {
			baryc = lastBaryc;
		} else {
			baryc = Ephem.eclipticToEquatorial(geo_sun_0, Constant.J2000, eph); //PlanetEphem.getGeocentricPosition(JD_TDB, TARGET.Solar_System_Barycenter, 0.0, false, obs), Constant.J2000, eph);
		}

		// Obtain heliocentric ecliptic coordinates
		double helio_object[] = PlanetEphem.getHeliocentricEclipticPositionJ2000(JD_TDB - light_time, eph.targetBody);
		if (addOffset) {
			Object o = DataBase.getData("offsetPosition", true);
			if (o != null) {
				double[] planetocentricPositionOfTargetSatellite = (double[]) o;
				helio_object = Functions.sumVectors(helio_object, planetocentricPositionOfTargetSatellite);
			}
		}

		if (eph.targetBody != TARGET.Moon)
		{
			LocationElement locP = LocationElement.parseRectangularCoordinates(helio_object);
			double lightTimeP = locP.getRadius() * Constant.LIGHT_TIME_DAYS_PER_AU;
			double helio_object_sun[] = PlanetEphem.getHeliocentricEclipticPositionJ2000(JD_TDB - light_time - lightTimeP, TARGET.SUN);
			helio_object = Functions.substract(helio_object, helio_object_sun);
		} else {
			double[] geo_sun_ltS = PlanetEphem.getGeocentricPosition(JD_TDB, TARGET.SUN, lightTimeS, false, obs);
			helio_object = Functions.substract(helio_object, geo_sun_ltS);
			if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.ASTROMETRIC) {
				geo_eq = Ephem.aberration(new double[] {-geo_eq[0], -geo_eq[1], -geo_eq[2], 0, 0, 0}, baryc, light_time);
				geo_eq = Functions.scalarProduct(geo_eq, -1.0);
			}
		}

		if (eph.preferPrecisionInEphemerides) {
			// Moshier is DE404, with the same reference frame (IERS). So in case of
			// high precision we can rotate it into ICRF following Folkner 1994 and Chernetenko 2007
			double ang1 = -0.1 * 0.001 * Constant.ARCSEC_TO_RAD;
			double ang2 = 3 * 0.001 * Constant.ARCSEC_TO_RAD;
			double ang3 = -5.2 * 0.001 * Constant.ARCSEC_TO_RAD;
			Matrix m = Matrix.getR1(ang1).times(Matrix.getR2(ang2).times(Matrix.getR3(ang3)));
			geo_eq = m.times(new Matrix(DataSet.getSubArray(geo_eq, 0, 2))).getColumn(0);
			helio_object = m.times(new Matrix(DataSet.getSubArray(helio_object, 0, 2))).getColumn(0);
		}

		// Correct for solar deflection and aberration
		if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT)
		{
			if (eph.preferPrecisionInEphemerides) {
				double sun[] = PlanetEphem.getHeliocentricEclipticPositionJ2000(JD_TDB - lightTimeS, TARGET.SUN);
				//geo_eq = Ephem.solarDeflection(geo_eq, Ephem.eclipticToEquatorial(geo_sun_0, Constant.J2000, eph),
				//		Ephem.eclipticToEquatorial(Functions.substract(helio_object, sun), Constant.J2000, eph));
				geo_eq = Ephem.solarAndPlanetaryDeflection(geo_eq, baryc, //Ephem.eclipticToEquatorial(geo_sun_0, Constant.J2000, eph),
						Ephem.eclipticToEquatorial(Functions.substract(helio_object, sun), Constant.J2000, eph),
						new TARGET[] {TARGET.JUPITER}, JD_TDB, false, obs);
			}
			if (obs.getMotherBody() != TARGET.EARTH || eph.targetBody != TARGET.Moon)
				geo_eq = Ephem.aberration(geo_eq, baryc, light_time);

			if (addGCRS) DataBase.addData("GCRS", geo_eq, true);
		} else {
			if (addGCRS) DataBase.addData("GCRS", null, true);
		}

		/* Correction to output frame. */
		if (eph.preferPrecisionInEphemerides || eph.frame == FRAME.FK4)
			geo_eq = Ephem.toOutputFrame(geo_eq, FRAME.ICRF, eph.frame);
		helio_object = Ephem.eclipticToEquatorial(helio_object, Constant.J2000, eph);
		helio_object = Ephem.toOutputFrame(helio_object, FRAME.ICRF, eph.frame);

		double geo_date[];
		if (eph.frame == FRAME.FK4) {
			// Transform from B1950 to mean equinox of date
			 geo_date = Precession.precess(Constant.B1950, JD_TDB, geo_eq, eph);
			 helio_object = Precession.precess(Constant.B1950, JD_TDB, helio_object, eph);
		} else {
			// Transform from J2000 to mean equinox of date
			geo_date = Precession.precessFromJ2000(JD_TDB, geo_eq, eph);
			helio_object = Precession.precessFromJ2000(JD_TDB, helio_object, eph);
		}

		// Get heliocentric ecliptic position
		LocationElement loc_elem = LocationElement.parseRectangularCoordinates(Ephem.equatorialToEcliptic(helio_object, JD_TDB, eph));

		// Mean equatorial to true equatorial
		double true_eq[] = geo_date;
		if (obs.getMotherBody() == TARGET.EARTH) {
			if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT)
			{
				/* Correct nutation */
				true_eq = Nutation.nutateInEquatorialCoordinates(JD_TDB, eph, geo_date, true);
			}

			// Correct for polar motion
			if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT &&
					eph.correctForPolarMotion)
			{
				double gast = SiderealTime.greenwichApparentSiderealTime(time, obs, eph);
				true_eq = Functions.rotateZ(true_eq, -gast);
				Matrix mat = IAU2006.getPolarMotionCorrectionMatrix(time, obs, eph);
				true_eq = mat.times(new Matrix(true_eq)).getColumn(0);
				true_eq = Functions.rotateZ(true_eq, gast);
			}
		}

		// Set some variables to improve performance when using loops with the
		// same calculation time
		if (lastSun0 == null) {
			lastTDB = JD_TDB;
			lastSun0 = geo_sun_0;
			lastBaryc = baryc;
		}

		// Pass to coordinates as seen from another body, if necessary
		if (obs.getMotherBody() != TARGET.NOT_A_PLANET && obs.getMotherBody() != TARGET.EARTH)
			true_eq = Ephem.getPositionFromBody(LocationElement.parseRectangularCoordinates(true_eq), time, obs, eph).getRectangularCoordinates();

		// Get equatorial coordinates
		LocationElement ephem_loc = LocationElement.parseRectangularCoordinates(true_eq);

		// Set preliminary results
		EphemElement ephem_elem = new EphemElement();
		ephem_elem.rightAscension = ephem_loc.getLongitude();
		ephem_elem.declination = ephem_loc.getLatitude();
		ephem_elem.distance = ephem_loc.getRadius();
		ephem_elem.heliocentricEclipticLongitude = loc_elem.getLongitude();
		ephem_elem.heliocentricEclipticLatitude = loc_elem.getLatitude();
		ephem_elem.lightTime = (float) light_time;
		// Note distances are apparent, not true
		ephem_elem.distanceFromSun = loc_elem.getRadius();

		if (eph.targetBody == TARGET.SUN) ephem_elem.heliocentricEclipticLatitude = ephem_elem.heliocentricEclipticLongitude =
			ephem_elem.distanceFromSun = 0;

		/* Topocentric correction */
		if (eph.isTopocentric)
			ephem_elem = Ephem.topocentricCorrection(time, obs, eph, ephem_elem);

		ephem_elem.name = eph.targetBody.getName();
		return ephem_elem;
	}
}
