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
package jparsec.ephem.moons;

import jparsec.astronomy.CoordinateSystem;
import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.Precession;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.event.MoonEvent;
import jparsec.ephem.event.MoonEvent.EVENT_DEFINITION;
import jparsec.ephem.planets.EphemElement;
import jparsec.math.Constant;
import jparsec.math.FastMath;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.TimeScale;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.util.JPARSECException;

/**
 * Implementation of Dourneau's Saturn satellites theory. Objects are Mimas,
 * Enceladus, Tethys, Dione, Rhea, Titan, Hyperion, Iapetus.
 *
 * @see MoonEphemElement
 * @author T. Alonso Albi - OAN (Spain)
 * @author Kerry Shetline
 * @version 1.0
 */
public class Dourneau
{
	// private constructor so that this class cannot be instantiated.
	private Dourneau() {}

	/**
	 * This is an implementation of the method of computing Saturn's moons
	 * created by G&eacute;rard Dourneau, as presented by Jean Meeus. Adapted from
	 * original code by Kerry Shetline. It has not been extensively checked,
	 * although it seems to be ok, within the arcsecond compared with TASS 1.7.
	 * <P>
	 * Objects are Mimas, Enceladus, Tethys, Dione, Rhea, Titan, Hyperion,
	 * Iapetus.
	 * <P>
	 * Satellites are positioned respect to the mother planet. For this purpose
	 * the Moshier fit is used by default. You can select Series96 theory to
	 * obtain the position of the satellites in the Ephemeris object.
	 * This is recommended when possible (20th and 21st centuries).
	 * <P>
	 * For reference see G. Dourneau, <I>Orbital elements of the eight major
	 * satellites of Saturn determined from a fit of their theories of motion to
	 * observations from 1886 to 1985</I>, A&amp;A 267, 292 (1993).
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @return An array of {@linkplain MoonEphem} objects with the ephemeris of the
	 *         satellites.
	 * @deprecated TASS 1.7 theory is recommended instead.
	 * @throws JPARSECException If the calculation fails.
	 */
	public static MoonEphemElement[] saturnianSatellitesEphemerides_Dourneau(TimeElement time, // Time
																							// Element
			ObserverElement obs, // Observer Element
			EphemerisElement eph) // Ephemeris Element
			throws JPARSECException
	{
		int nmoons = 8;

		double L0 = 0.0, B0 = 0.0, DELTA = 0.0;
		double L0_sun = 0.0, B0_sun = 0.0, DELTA_sun = 0.0;

		double time_JDE = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);

		// Obtain ephemeris of Saturn and Sun for B1950
		EphemerisElement new_eph = eph.clone();
		new_eph.targetBody = TARGET.SATURN;
		new_eph.equinox = Constant.B1950;
		EphemerisElement new_eph_sun = eph.clone();
		new_eph_sun.targetBody = TARGET.SUN;
		new_eph_sun.equinox = Constant.B1950;

		// Obtain position of planet
		EphemElement ephem = new EphemElement();
		ephem = MoonEphem.getBodyEphem(time, obs, new_eph, new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, time_JDE);

		// Obtain position of sun
		EphemElement ephem_sun = new EphemElement();
		ephem_sun = MoonEphem.getBodyEphem(time, obs, new_eph_sun, new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, time_JDE);
		new_eph_sun.equinox = eph.equinox;
		EphemElement ephem_sun_now = MoonEphem.getBodyEphem(time, obs, new_eph_sun, new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, time_JDE);

		// Set light delay
		double lightDelay = ephem.lightTime;
		if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.GEOMETRIC)
			lightDelay = 0.0;

		// Obtain heliocentric ecliptic coordinates in B1950 for Saturn, as seen from the observer and
		// from the Sun. Later heliocentric position will be used for calculating if the satellite is
		// eclipsed or not.
		LocationElement loc = new LocationElement(ephem.heliocentricEclipticLongitude, ephem.heliocentricEclipticLatitude,
				ephem.distanceFromSun);

		L0_sun = loc.getLongitude() * Constant.RAD_TO_DEG;
		B0_sun = loc.getLatitude() * Constant.RAD_TO_DEG;
		DELTA_sun = loc.getRadius();

		double pos[] = LocationElement.parseLocationElement(new LocationElement(ephem.rightAscension,
				ephem.declination, ephem.distance));
		LocationElement new_loc = LocationElement.parseRectangularCoordinates(Ephem.equatorialToEcliptic(pos,
				Constant.B1950, eph));

		L0 = new_loc.getLongitude() * Constant.RAD_TO_DEG;
		B0 = new_loc.getLatitude() * Constant.RAD_TO_DEG;
		DELTA = new_loc.getRadius();

		// Start calculations for satellites
		double flattening = 1.120699;

		double t = time_JDE - lightDelay;
		double t1 = t - 2411093.0;
		double t2 = t1 / 365.25;
		double t3 = (t - 2433282.423) / 365.25 + 1950.0;
		double t4 = t - 2411368.0;
		double t5 = t4 / 365.25;
		double t6 = t - 2415020.0;
		double t7 = t6 / Constant.JULIAN_DAYS_PER_CENTURY;
		double t8 = t6 / 365.25;
		double t9 = (t - 2442000.5) / 365.25;
		double t10 = t - 2409786.0;
		double t11 = t10 / Constant.JULIAN_DAYS_PER_CENTURY;

		double W0 = 5.095 * (t3 - 1866.39);
		double W1 = 74.4 + 32.39 * t2;
		double W2 = 134.3 + 92.62 * t2;
		double W3 = 42.0 - 0.5118 * t5;
		double W4 = 276.59 + 0.5118 * t5;
		double W5 = 267.2635 + 1222.1136 * t7;
		double W6 = 175.4762 + 1221.5515 * t7;
		double W7 = 2.4891 + 0.002435 * t7;
		double W8 = 113.35 - 0.2597 * t7;

		double lambda = 0.0, r = 0.0, gamma = 0.0, OMEGA = 0.0, K = 0.0;
		double L, p = 0.0, M, C, u, w;
		double[] X = new double[nmoons + 1];
		double[] Y = new double[nmoons + 1];
		double[] Z = new double[nmoons + 1];
		double[] cteK = new double[nmoons + 1];
		double[] cteR = new double[nmoons + 1];

		for (int j = 0; j < nmoons; ++j)
		{
			switch (j)
			{
			case 0: // I, Mimas
				L = 127.64 + 381.994497 * t1 - 43.57 * sin_deg(W0) - 0.720 * sin_deg(3.0 * W0) - 0.02144 * Math
						.sin(5.0 * W0);
				p = 106.1 + 365.549 * t2;
				M = L - p;
				C = 2.18287 * sin_deg(M) + 0.025988 * sin_deg(2.0 * M) + 0.00043 * sin_deg(3.0 * M);
				lambda = L + C;
				r = 3.06879 / (1.0 + 0.01905 * cos_deg(M + C));
				gamma = 1.563;
				OMEGA = 54.5 - 365.072 * t2;
				K = 20947.0;
				break;

			case 1: // II, Enceladus
				L = 200.317 + 262.7319002 * t1 + 0.25667 * sin_deg(W1) + 0.20883 * sin_deg(W2);
				p = 309.107 + 123.44121 * t2;
				M = L - p;
				C = 0.55577 * sin_deg(M) + 0.00168 * sin_deg(2.0 * M);
				lambda = L + C;
				r = 3.94118 / (1.0 + 0.00485 * cos_deg(M + C));
				gamma = 0.0262;
				OMEGA = 348.0 - 151.95 * t2;
				K = 23715.0;
				break;

			case 2: // III, Tethys
				lambda = 285.306 + 190.69791226 * t1 + 2.063 * sin_deg(W0) + 0.03409 * sin_deg(3.0 * W0) + 0.001015 * sin_deg(5.0 * W0);
				r = 4.880998;
				gamma = 1.0976;
				OMEGA = 111.33 - 72.2441 * t2;
				K = 26382.0;
				break;

			case 3: // IV, Dione
				L = 254.712 + 131.53493193 * t1 - 0.0215 * sin_deg(W1) - 0.01733 * sin_deg(W2);
				p = 174.8 + 30.820 * t2;
				M = L - p;
				C = 0.24717 * sin_deg(M) + 0.00033 * sin_deg(2.0 * M);
				lambda = L + C;
				r = 6.24871 / (1.0 + 0.002157 * cos_deg(M + C));
				gamma = 0.0139;
				OMEGA = 232.0 - 30.27 * t2;
				K = 29876.0;
				break;

			case 4: // Outer moons
			case 5:
			case 6:
			case 7:
				double p1, a1, a2, N, i1, OMEGA1, g0, psi, s, g, ww = 0.0, e1, q;
				double b1, b2, theta, h;
				double eta, zeta, theta1, as, bs, cs, phi, chi;
				double ww1, ww0, mu, l, g1, ls, gs, lT, gT, u1,	u2, u3,	u4,	u5,	w1,	PHI;
				double e = 0.0,	a = 0.0, i = 0.0, lambda1 = 0.0;
				OuterMoonInfo omi;

				switch (j)
				{
				case 4: // V, Rhea
					p1 = 342.7 + 10.057 * t2;
					a1 = 0.000265 * sin_deg(p1) + 0.01 * sin_deg(W4);
					a2 = 0.000265 * cos_deg(p1) + 0.01 * cos_deg(W4);
					e = Math.sqrt(a1 * a1 + a2 * a2);
					p = atan2_deg(a1, a2);
					N = 345.0 - 10.057 * t2;
					lambda1 = 359.244 + 79.69004720 * t1 + 0.086754 * sin_deg(N);
					i = 28.0362 + 0.346890 * cos_deg(N) + 0.01930 * cos_deg(W3);
					OMEGA = 168.8034 + 0.73693 * sin_deg(N) + 0.041 * sin_deg(W3);
					a = 8.725924;
					M = lambda1 - p;
					K = 35313.0;
					break;

				case 5: // VI, Titan
					L = 261.1582 + 22.57697855 * t4 + 0.074025 * sin_deg(W3);
					i1 = 27.45141 + 0.295999 * cos_deg(W3);
					OMEGA1 = 168.66925 + 0.628808 * sin_deg(W3);
					a1 = sin_deg(W7) * sin_deg(OMEGA1 - W8);
					a2 = cos_deg(W7) * sin_deg(i1) - sin_deg(W7) * cos_deg(i1) * cos_deg(OMEGA1 - W8);
					g0 = 102.8623;
					psi = atan2_deg(a1, a2);
					s = Math.sqrt(a1 * a1 + a2 * a2);
					g = W4 - OMEGA1 - psi;
					for (int k = 0; k < 3; ++k)
					{
						ww = W4 + 0.37515 * (sin_deg(2.0 * g) - sin_deg(2.0 * g0));
						g = ww - OMEGA1 - psi;
					}
					e1 = 0.029092 + 0.00019048 * (cos_deg(2.0 * g) - cos_deg(2.0 * g0));
					q = 2.0 * (W5 - ww);
					b1 = sin_deg(i1) * sin_deg(OMEGA1 - W8);
					b2 = cos_deg(W7) * sin_deg(i1) * cos_deg(OMEGA1 - W8) - sin_deg(W7) * cos_deg(i1);
					theta = atan2_deg(b1, b2) + W8;
					e = e1 + 0.002778797 * e1 * cos_deg(q);
					p = ww + 0.159215 * sin_deg(q);
					u = 2.0 * W5 - 2.0 * theta + psi;
					h = 0.9375 * e1 * e1 * sin_deg(q) + 0.1875 * s * s * sin_deg(2.0 * (W5 - theta));
					lambda1 = L - 0.254744 * (e1 * sin_deg(W6) + 0.75 * e1 * e1 * sin_deg(2.0 * W6) + h);
					i = i1 + 0.031843 * s * cos_deg(u);
					OMEGA = OMEGA1 + 0.031843 * s * sin_deg(u) / sin_deg(i1);
					a = 20.216193;
					K = 53800.0;
					break;

				case 6: // VII, Hyperion
					eta = 92.39 + 0.5621071 * t6;
					zeta = 148.19 - 19.18 * t8;
					theta = 184.8 - 35.41 * t9;
					theta1 = theta - 7.5;
					as = 176.0 + 12.22 * t8;
					bs = 8.0 + 24.44 * t8;
					cs = bs + 5.0;
					ww = 69.898 - 18.67088 * t8;
					phi = 2.0 * (ww - W5);
					chi = 94.9 - 2.292 * t8;
					a = 24.50601 - 0.08686 * cos_deg(eta) - 0.00166 * cos_deg(zeta + eta) + 0.00175 * cos_deg(zeta - eta);
					e = 0.103458 - 0.004099 * cos_deg(eta) - 0.000167 * cos_deg(zeta + eta) + 0.000235 * cos_deg(zeta - eta) + 0.02303 * cos_deg(zeta) - 0.00212 * cos_deg(2.0 * zeta) + 0.000151 * cos_deg(3.0 * zeta) + 0.00013 * cos_deg(phi);
					p = ww + 0.15648 * sin_deg(chi) - 0.4457 * sin_deg(eta) - 0.2657 * sin_deg(zeta + eta) - 0.3573 * sin_deg(zeta - eta) - 12.872 * sin_deg(zeta) + 1.668 * sin_deg(2.0 * zeta) - 0.2419 * sin_deg(3.0 * zeta) - 0.07 * sin_deg(phi);
					lambda1 = 177.047 + 16.91993829 * t6 + 0.15648 * sin_deg(chi) + 9.142 * sin_deg(eta) + 0.007 * sin_deg(2.0 * eta) - 0.014 * sin_deg(3.0 * eta) + 0.2275 * sin_deg(zeta + eta) + 0.2112 * sin_deg(zeta - eta) - 0.26 * sin_deg(zeta) - 0.0098 * sin_deg(2.0 * zeta) - 0.013 * sin_deg(as) + 0.017 * sin_deg(bs) - 0.0303 * sin_deg(phi);
					i = 27.3347 + 0.643486 * cos_deg(chi) + 0.315 * cos_deg(W3) + 0.018 * cos_deg(theta) - 0.018 * cos_deg(cs);
					OMEGA = 168.6812 + 1.40136 * cos_deg(chi) + 0.68599 * sin_deg(W3) - 0.0392 * sin_deg(cs) + 0.0366 * sin_deg(theta1);
					K = 59222.0;
					break;

				case 7: // VII, Iapetus
					L = 261.1582 + 22.57697855 * t4;
					ww1 = 91.769 + 0.562 * t7;
					psi = 4.367 - 0.195 * t7;
					theta = 146.819 - 3.198 * t7;
					phi = 60.470 + 1.521 * t7;
					PHI = 205.055 - 2.091 * t7;
					e1 = 0.028298 + 0.001156 * t11;
					ww0 = 352.91 + 11.71 * t11;
					mu = 76.3852 + 4.53795125 * t10;
					i1 = 18.4602 - 0.9518 * t11 - 0.072 * t11 * t11 + 0.0054 * t11 * t11 * t11;
					OMEGA1 = 143.198 - 3.919 * t11 + 0.116 * t11 * t11 + 0.008 * t11 * t11 * t11;
					l = mu - ww0;
					g = ww0 - OMEGA1 - psi;
					g1 = ww0 - OMEGA1 - phi;
					ls = W5 - ww1;
					gs = ww1 - theta;
					lT = L - W4;
					gT = W4 - PHI;
					u1 = 2.0 * (l + g - ls - gs);
					u2 = l + g1 - lT - gT;
					u3 = l + 2.0 * (g - ls - gs);
					u4 = lT + gT - g1;
					u5 = 2.0 * (ls + gs);
					a = 58.935028 + 0.004638 * cos_deg(u1) + 0.058222 * cos_deg(u2);
					e = e1 - 0.0014097 * cos_deg(g1 - gT) + 0.0003733 * cos_deg(u5 - 2.0 * g) + 0.0001180 * cos_deg(u3) + 0.0002408 * cos_deg(l) + 0.0002849 * cos_deg(l + u2) + 0.0006190 * cos_deg(u4);
					w = 0.08077 * sin_deg(g1 - gT) + 0.02139 * sin_deg(u5 - 2.0 * g) - 0.00676 * sin_deg(u3) + 0.01380 * sin_deg(l) + 0.01632 * sin_deg(l + u2) + 0.03547 * sin_deg(u4);
					p = ww0 + w / e1;
					lambda1 = mu - 0.04299 * sin_deg(u2) - 0.00789 * sin_deg(u1) - 0.06312 * sin_deg(ls) - 0.00295 * sin_deg(2.0 * ls) - 0.02231 * sin_deg(u5) + 0.00650 * sin_deg(u5 + psi);
					i = i1 + 0.04204 * cos_deg(u5 + psi) + 0.00235 * cos_deg(l + g1 + lT + gT + phi) + 0.00360 * cos_deg(u2 + phi);
					w1 = 0.04204 * sin_deg(u5 + psi) + 0.00235 * sin_deg(l + g1 + lT + gT + phi) + 0.00358 * sin_deg(u2 + phi);
					OMEGA = OMEGA1 + w1 / sin_deg(i1);
					K = 91820.0;
					break;
				}

				M = lambda1 - p;
				omi = solveOuterMoon(e, M, a, OMEGA, i, lambda1);

				lambda = omi.lambda;
				gamma = omi.gamma;
				OMEGA = omi.w;
				r = omi.r;
				break;
			}

			u = lambda - OMEGA;
			w = OMEGA - 168.8112;

			X[j] = r * (cos_deg(u) * cos_deg(w) - sin_deg(u) * cos_deg(gamma) * sin_deg(w));
			Y[j] = r * (sin_deg(u) * cos_deg(w) * cos_deg(gamma) + cos_deg(u) * sin_deg(w));
			Z[j] = r * sin_deg(u) * sin_deg(gamma);
			cteK[j] = K;
			cteR[j] = r;
		}

		// Now we set up the ol' ficticious moon.
		X[nmoons] = 0.0;
		Y[nmoons] = 0.0;
		Z[nmoons] = 1.0;

		double D = 0, D_sun = 0, D_ecl = 0, D_ecl_sun = 0;
		double Y1;

		MoonEphemElement moon[] = new MoonEphemElement[nmoons];
		loc = new_loc;

		double inc = 28.0817;
		double ome = 168.8112;

		// We'll loop backwards so we can compute D from the ficticious moon first.
		for (int j = nmoons; j >= 0; --j)
		{

			double sat_pos[] = MoonEphem.getSatellitePosition(X[j], Y[j], Z[j], 0.0, 0.0, inc, ome, L0, B0, L0_sun,
					B0_sun);

			if (j == nmoons)
			{
				D = Math.atan2(sat_pos[0], sat_pos[2]);
				D_sun = Math.atan2(sat_pos[3], sat_pos[5]);

				double sat_pos_from_ecliptic[] = MoonEphem.getSatellitePosition(X[j], Y[j], Z[j], 0.0, 0.0, 0.0, 0.0,
						L0, B0, L0_sun, B0_sun);
				D_ecl = Math.atan2(sat_pos_from_ecliptic[0], sat_pos_from_ecliptic[2]);
				D_ecl_sun = Math.atan2(sat_pos_from_ecliptic[3], sat_pos_from_ecliptic[5]);
			} else
			{
				double apparent_pos[] = MoonEphem.getApparentPosition(TARGET.SATURN, sat_pos, D, DELTA, D_sun,
						DELTA_sun, cteK[j], cteR[j]);
				double apparent_pos_from_ecliptic[] = MoonEphem.getApparentPosition(TARGET.SATURN, sat_pos, D_ecl,
						DELTA, D_ecl_sun, DELTA_sun, cteK[j], cteR[j]);

				// Obtain equatorial and horizontal coordinates
				double ecl_lon = loc.getLongitude() - apparent_pos_from_ecliptic[0] * ephem.angularRadius;
				double ecl_lat = loc.getLatitude() + apparent_pos_from_ecliptic[1] * ephem.angularRadius;
				double ecl_r = DELTA + apparent_pos_from_ecliptic[2] * TARGET.SATURN.equatorialRadius / Constant.AU;
				LocationElement sat_loc = new LocationElement(ecl_lon, ecl_lat, ecl_r);
				pos = Ephem.eclipticToEquatorial(LocationElement.parseLocationElement(sat_loc), Constant.B1950,
						eph);
				double equinox = eph.equinox;
				if (equinox == EphemerisElement.EQUINOX_OF_DATE)
					equinox = time_JDE;
				double new_pos[] = Precession.precess(Constant.B1950, equinox, pos, eph);
				new_loc = LocationElement.parseRectangularCoordinates(new_pos);
				LocationElement hor_loc = CoordinateSystem.equatorialToHorizontal(new_loc, time, obs, eph);

				// Set results of ephemeris
				String nom = TARGET.values()[TARGET.Mimas.ordinal() + j].getName();
				double ra = new_loc.getLongitude();
				double dec = new_loc.getLatitude();
				double dist = new_loc.getRadius();
				double azi = hor_loc.getLongitude();
				double ele = hor_loc.getLatitude();
				double RE = ephem_sun.distance;
				double RP = DELTA_sun + apparent_pos[5] * TARGET.SATURN.equatorialRadius / Constant.AU;
				double RO = dist;
				double DELO = (RE * RE + RO * RO - RP * RP) / (2.0 * RE * RO);
				double DPH = ((RP * RP + RO * RO - RE * RE) / (2.0 * RP * RO));
				double ill = (1.0 + DPH) * 0.5;
				double elo = Math.acos(DELO);

				// From the observer
				boolean inferior = (apparent_pos[2] <= 0.0);
				Y1 = apparent_pos[1] * flattening;
				TARGET target = TARGET.values()[TARGET.Mimas.ordinal() + j];
				double angularRadius = Math.atan(target.equatorialRadius / dist);
				double satSize = angularRadius / ephem.angularRadius;
				double satSizeOccultation = -satSize;
				EVENT_DEFINITION ed = MoonEvent.getEventDefinition();
				if (ed == EVENT_DEFINITION.ENTIRE_SATELLITE) satSize = -satSize;
				if (ed == EVENT_DEFINITION.SATELLITE_CENTER || ed == EVENT_DEFINITION.AUTOMATIC) satSize = 0;
				boolean withinDisc = (Math.sqrt(apparent_pos[0] * apparent_pos[0] + Y1 * Y1) <= (1.0 + satSize));
				boolean transiting = withinDisc && inferior;
				boolean withinDiscOcc = withinDisc;
				if (ed == EVENT_DEFINITION.AUTOMATIC_FOR_DRAWING) withinDiscOcc = (Math.sqrt(apparent_pos[0] * apparent_pos[0] + Y1 * Y1) <= (1.0 + satSizeOccultation));
				boolean occulted = withinDiscOcc && !inferior;

				// From Sun. Note we are neglecting the difference between light time from the planet
				// to the Earth, and to the Sun. This introduces no significant errors in the
				// calculations.
				boolean inferior_sun = (apparent_pos[5] <= 0.0);
				Y1 = apparent_pos[4] * flattening;
				boolean withinDisc_sun = (Math.sqrt(apparent_pos[3] * apparent_pos[3] + Y1 * Y1) < (1.0 + satSize));
				boolean withinDisc_sunOcc = withinDisc_sun;
				boolean eclipsed = withinDisc_sunOcc && !inferior_sun;
				boolean shadow_transiting = withinDisc_sunOcc && inferior_sun;
				if (ed == EVENT_DEFINITION.AUTOMATIC_FOR_DRAWING) {
					withinDisc_sunOcc = (Math.sqrt(apparent_pos[3] * apparent_pos[3] + Y1 * Y1) < (1.0 + satSizeOccultation));
					eclipsed = withinDisc_sunOcc && !inferior_sun;

					double satRadius = satSize * TARGET.JUPITER.equatorialRadius;
					double satPlanDistance = (Math.sqrt(apparent_pos[3] * apparent_pos[3] + apparent_pos[4] * apparent_pos[4] + apparent_pos[5] * apparent_pos[5]) - 1.0) * TARGET.JUPITER.equatorialRadius;
					double sun_size = FastMath.atan2_accurate(TARGET.SUN.equatorialRadius, ephem.distanceFromSun * Constant.AU);
					double shadow_cone_dist = satRadius / FastMath.tan(sun_size);
					double shadow_size = (satSize * (1.0 - 0.5 * satPlanDistance / shadow_cone_dist));
					double penumbra_size = 2 * (satSize-shadow_size);
					satSizeOccultation = -(shadow_size + penumbra_size);

					withinDisc_sunOcc = (Math.sqrt(apparent_pos[3] * apparent_pos[3] + Y1 * Y1) < (1.0 - satSizeOccultation));
					shadow_transiting = withinDisc_sunOcc && inferior_sun;
				}

				// Create ephemeris object
				moon[j] = new MoonEphemElement(nom, ra, dec, dist, RP, azi, ele, (float) ill, (float) elo, eclipsed, occulted,
						transiting, shadow_transiting, inferior, apparent_pos[0], apparent_pos[1], apparent_pos[2],
						apparent_pos[3], apparent_pos[4], apparent_pos[5]);

				// Obtain physical ephemerides
				EphemerisElement sat_eph =  eph.clone();

				sat_eph.targetBody = target;
				moon[j] = MoonPhysicalParameters.physicalParameters(time_JDE, ephem_sun_now, moon[j], obs, sat_eph);

				// Obtain relative phenomena
				if (j == 0)
					moon = MoonEphem.satellitesPhenomena(moon, ephem.angularRadius);

			}
		}

		return moon;
	}

	protected static OuterMoonInfo solveOuterMoon(double e, double M, double a, double OMEGA, double i, double lambda1)
	{
		OuterMoonInfo omi = new OuterMoonInfo();
		double e2 = e * e;
		double e3 = e2 * e;
		double e4 = e3 * e;
		double e5 = e4 * e;

		double s1 = sin_deg(28.0817);
		double c1 = cos_deg(28.0817);

		double C = Constant.RAD_TO_DEG * ((2.0 * e - 0.25 * e3 + 0.0520833333 * e5) * sin_deg(M) + (1.25 * e2 - 0.458333333 * e4) * sin_deg(2.0 * M) + (1.083333333 * e3 - 0.671875 * e5) * sin_deg(3.0 * M) + 1.072917 * e4 * sin_deg(4.0 * M) + 1.142708 * e5 * sin_deg(5.0 * M));

		omi.r = a * (1.0 - e2) / (1.0 + e * cos_deg(M + C));

		double g = OMEGA - 168.8112;
		double a1 = sin_deg(i) * sin_deg(g);
		double a2 = c1 * sin_deg(i) * cos_deg(g) - s1 * cos_deg(i);

		omi.gamma = asin_deg(Math.sqrt(a1 * a1 + a2 * a2));

		double u = atan2_deg(a1, a2);

		omi.w = 168.8112 + u;

		double h = c1 * sin_deg(i) - s1 * cos_deg(i) * cos_deg(g);
		double psi = atan2_deg(s1 * sin_deg(g), h);

		omi.lambda = lambda1 + C + u - g - psi;

		return omi;
	}

	protected static class OuterMoonInfo
	{
		public double lambda;

		public double gamma;

		public double w;

		public double r;
	}

	private static double sin_deg(double deg)
	{
		return Math.sin(deg * Constant.DEG_TO_RAD);
	}

	private static double cos_deg(double deg)
	{
		return Math.cos(deg * Constant.DEG_TO_RAD);
	}

	private static double atan2_deg(double a1, double a2)
	{
		return Math.atan2(a1, a2) * Constant.RAD_TO_DEG;
	}

	private static double asin_deg(double sin)
	{
		return Math.asin(sin) * Constant.RAD_TO_DEG;
	}
}
