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
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.Functions;
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
 * Implementation of E2x3 and E5 jovian satellites theories by Jay Lieske.
 *
 * @see MoonEphemElement
 * @author T. Alonso Albi - OAN (Spain)
 * @author Kerry Shetline
 * @version 1.0
 */
public class E
{
	// private constructor so that this class cannot be instantiated.
	private E() {}

	/**
	 * This is an implementation of the E2x3 Jovian satellite theory by Jay
	 * Lieske, as presented by Jean Meeus, Astronomical Algorithms, 2nd Ed., pp.
	 * 285-299. Based on Lieske, Astronomy &amp; Astrophysics 176, 146-158 (1987).
	 * Objects are Io, Europa, Ganymede, and Callisto.
	 * <P>
	 * Satellites are positioned respect to the mother planet. For this purpose
	 * the Moshier fit is used by default. You can select Series96 theory to
	 * obtain the position of the satellites in the Ephemeris object.
	 * This is recommended when possible (20th and 21st centuries).
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @return An array of {@linkplain MoonEphem} objects with the ephemeris.
	 * @deprecated L1 ephemerides theory is recommended instead.
	 * @throws JPARSECException If the calculation fails.
	 */
	public static MoonEphemElement[] galileanSatellitesEphemerides_E2x3(TimeElement time, // Time
																							// Element
			ObserverElement obs, // Observer Element
			EphemerisElement eph) // Ephemeris Element
			throws JPARSECException
	{
		int nmoons = 4;
		double L0 = 0.0, B0 = 0.0, DELTA = 0.0;
		double L0_sun = 0.0, B0_sun = 0.0, DELTA_sun = 0.0;

		double time_JDE = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);

		// Obtain ephemeris of Jupiter and Sun
		EphemerisElement new_eph =  eph.clone();
		new_eph.targetBody = TARGET.JUPITER;
		EphemerisElement new_eph_sun =  eph.clone();
		new_eph_sun.targetBody = TARGET.SUN;

		// Obtain position of planet
		EphemElement ephem = new EphemElement();
		ephem = MoonEphem.getBodyEphem(time, obs, new_eph, new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, time_JDE);

		// Obtain position of sun
		EphemElement ephem_sun = new EphemElement();
		ephem_sun = MoonEphem.getBodyEphem(time, obs, new_eph_sun, new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, time_JDE);

		// Set light delay
		double lightDelay = ephem.lightTime;
		if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.GEOMETRIC)
			lightDelay = 0.0;

		// Obtain heliocentric ecliptic coordinates of date for Jupiter, as seen from the observer and
		// from the Sun. Later heliocentric position will be used for calculating if the satellite is
		// eclipsed or not.
		L0_sun = ephem.heliocentricEclipticLongitude * Constant.RAD_TO_DEG;
		B0_sun = ephem.heliocentricEclipticLatitude * Constant.RAD_TO_DEG;
		DELTA_sun = ephem.distanceFromSun;

		LocationElement loc = new LocationElement(ephem.rightAscension, ephem.declination, ephem.distance);
		double pos[] = LocationElement.parseLocationElement(CoordinateSystem.equatorialToEcliptic(loc, time, obs, eph));
		LocationElement new_loc = LocationElement.parseRectangularCoordinates(pos);

		L0 = new_loc.getLongitude() * Constant.RAD_TO_DEG;
		B0 = new_loc.getLatitude() * Constant.RAD_TO_DEG;
		DELTA = new_loc.getRadius();

		// Start calculations for satellites
		double t = time_JDE - 2443000.5 - lightDelay;
		double flattening = 1.071374;

		double l1 = 106.07947 + 203.488955432 * t;
		double l2 = 175.72938 + 101.374724550 * t;
		double l3 = 120.55434 + 50.317609110 * t;
		double l4 = 84.44868 + 21.571071314 * t;

		double p1 = 58.3329 + 0.16103936 * t;
		double p2 = 132.8959 + 0.04647985 * t;
		double p3 = 187.2887 + 0.00712740 * t;
		double p4 = 335.3418 + 0.00183998 * t;

		double w1 = 311.0793 - 0.13279430 * t;
		double w2 = 100.5099 - 0.03263047 * t;
		double w3 = 119.1688 - 0.00717704 * t;
		double w4 = 322.5729 - 0.00175934 * t;

		double GAMMA = 0.33033 * sin_deg(163.679 + 0.0010512 * t) + 0.03439 * sin_deg(34.486 - 0.0161731 * t);
		double PHI_l = 191.8132 + 0.17390023 * t;
		double psi = 316.5182 - 0.00000208 * t;
		double G = 30.23756 + 0.0830925701 * t + GAMMA;
		double G1 = 31.97853 + 0.0334597339 * t;
		double PIj = 13.469942;

		double S = 0.0, L = 0.0, B = 0.0, R = 0.0, K = 0.0;
		double[] X = new double[nmoons + 1];
		double[] Y = new double[nmoons + 1];
		double[] Z = new double[nmoons + 1];
		double[] coef_K = new double[nmoons + 1];
		double[] coef_R = new double[nmoons + 1];

		for (int j = 0; j < nmoons; ++j)
		{
			switch (j)
			{
			case 0: // I, Io
				S = +0.47259 * sin_deg(2.0 * (l1 - l2)) - 0.03480 * sin_deg(p3 - p4) - 0.01756 * sin_deg(p1 + p3 - 2.0 * PIj - 2.0 * G) + 0.01080 * sin_deg(l2 - 2.0 * l3 + p3) + 0.00757 * sin_deg(PHI_l) + 0.00663 * sin_deg(l2 - 2.0 * l3 + p4) + 0.00453 * sin_deg(l1 - p3) + 0.00453 * sin_deg(l2 - 2.0 * l3 + p2) - 0.00354 * sin_deg(l1 - l2) - 0.00317 * sin_deg(2.0 * psi - 2.0 * PIj) - 0.00269 * sin_deg(l2 - 2.0 * l3 + p1) + 0.00263 * sin_deg(l1 - p4) + 0.00186 * sin_deg(l1 - p1) - 0.00186 * sin_deg(G) + 0.00167 * sin_deg(p2 - p3) + 0.00158 * sin_deg(4.0 * (l1 - l2)) - 0.00155 * sin_deg(l1 - l3) - 0.00142 * sin_deg(psi + w3 - 2.0 * PIj - 2.0 * G) - 0.00115 * sin_deg(2.0 * (l1 - 2.0 * l2 + w2)) + 0.00089 * sin_deg(p2 - p4) + 0.00084 * sin_deg(l1 + p3 - 2.0 * PIj - 2.0 * G) + 0.00084 * sin_deg(w2 - w3) + 0.00053 * sin_deg(psi - w2);
				L = l1 + S;
				B = atan_deg(+0.0006502 * sin_deg(L - w1) + 0.0001835 * sin_deg(L - w2) + 0.0000329 * sin_deg(L - w3) - 0.0000311 * sin_deg(L - psi) + 0.0000093 * sin_deg(L - w4) + 0.0000075 * sin_deg(3.0 * L - 4.0 * l2 - 1.9927 * S + w2) + 0.0000046 * sin_deg(L + psi - 2.0 * PIj - 2.0 * G));
				R = 5.90730 * (1.0 - 0.0041339 * cos_deg(2.0 * (l1 - l2)) - 0.0000395 * cos_deg(l1 - p3) - 0.0000214 * cos_deg(l1 - p4) + 0.0000170 * cos_deg(l1 - l2) - 0.0000162 * cos_deg(l1 - p1) - 0.0000130 * cos_deg(4.0 * (l1 - l2)) + 0.0000106 * cos_deg(l1 - l3) - 0.0000063 * cos_deg(l1 + p3 - 2.0 * PIj - 2.0 * G));
				K = 17295.0;
				break;

			case 1: // II, Europa
				S = +1.06476 * sin_deg(2.0 * (l2 - l3)) + 0.04253 * sin_deg(l1 - 2.0 * l2 + p3) + 0.03579 * sin_deg(l2 - p3) + 0.02383 * sin_deg(l1 - 2.0 * l2 + p4) + 0.01977 * sin_deg(l2 - p4) - 0.01843 * sin_deg(PHI_l) + 0.01299 * sin_deg(p3 - p4) - 0.01142 * sin_deg(l2 - l3) + 0.01078 * sin_deg(l2 - p2) - 0.01058 * sin_deg(G) + 0.00870 * sin_deg(l2 - 2.0 * l3 + p2) - 0.00775 * sin_deg(2.0 * (psi - PIj)) + 0.00524 * sin_deg(2.0 * (l1 - l2)) - 0.00460 * sin_deg(l1 - l3) + 0.00450 * sin_deg(l2 - 2.0 * l3 + p1) + 0.00327 * sin_deg(psi - 2.0 * G + w3 - 2.0 * PIj) - 0.00296 * sin_deg(p1 + p3 - 2.0 * PIj - 2.0 * G) - 0.00151 * sin_deg(2.0 * G) + 0.00146 * sin_deg(psi - w3) + 0.00125 * sin_deg(psi - w4) - 0.00117 * sin_deg(l1 - 2.0 * l3 + p3) - 0.00095 * sin_deg(2.0 * (l2 - w2)) + 0.00086 * sin_deg(2.0 * (l1 - 2.0 * l2 + w2)) - 0.00086 * sin_deg(5.0 * G1 - 2.0 * G + 52.225) - 0.00078 * sin_deg(l2 - l4) - 0.00064 * sin_deg(l1 - 2.0 * l3 + p4) - 0.00063 * sin_deg(3.0 * l3 - 7.0 * l4 + 4.0 * p4) + 0.00061 * sin_deg(p1 - p4) + 0.00058 * sin_deg(w3 - w4) + 0.00058 * sin_deg(2.0 * (psi - PIj - G)) + 0.00056 * sin_deg(2.0 * (l2 - l4)) + 0.00055 * sin_deg(2.0 * (l1 - l3)) + 0.00052 * sin_deg(3.0 * l3 - 7.0 * l4 + p3 + 3.0 * p4) - 0.00043 * sin_deg(l1 - p3) + 0.00042 * sin_deg(p3 - p2) + 0.00041 * sin_deg(5.0 * (l2 - l3)) + 0.00041 * sin_deg(p4 - PIj) + 0.00038 * sin_deg(l2 - p1) + 0.00032 * sin_deg(w2 - w3) + 0.00032 * sin_deg(2.0 * (l3 - G - PIj)) + 0.00029 * sin_deg(p1 - p3);
				L = l2 + S;
				B = atan_deg(+0.0081275 * sin_deg(L - w2) + 0.0004512 * sin_deg(L - w3) - 0.0003286 * sin_deg(L - psi) + 0.0001164 * sin_deg(L - w4) + 0.0000273 * sin_deg(l1 - 2.0 * l3 + 1.0146 * S + w2) - 0.0000143 * sin_deg(L - w1) + 0.0000143 * sin_deg(L + psi - 2.0 * PIj - 2.0 * G) + 0.0000035 * sin_deg(L - psi + G) - 0.0000028 * sin_deg(l1 - 2.0 * l3 + 1.0146 * S + w3));
				R = 9.39912 * (1.0 + 0.0093847 * cos_deg(l1 - l2) - 0.0003114 * cos_deg(l2 - p3) - 0.0001738 * cos_deg(l2 - p4) - 0.0000941 * cos_deg(l2 - p2) + 0.0000553 * cos_deg(l2 - l3) + 0.0000523 * cos_deg(l1 - l3) - 0.0000290 * cos_deg(2.0 * (l1 - l2)) + 0.0000166 * cos_deg(2.0 * (l2 - w2)) + 0.0000107 * cos_deg(l1 - 2.0 * l3 + p3) - 0.0000102 * cos_deg(l2 - p1) - 0.0000091 * cos_deg(2.0 * (l1 - l3)));
				K = 21819.0;
				break;

			case 2: // III, Ganymede
				S = +0.16477 * sin_deg(l3 - p3) + 0.09062 * sin_deg(l3 - p4) - 0.06907 * sin_deg(l2 - l3) + 0.03786 * sin_deg(p3 - p4) + 0.01844 * sin_deg(2.0 * (l3 - l4)) - 0.01340 * sin_deg(G) + 0.00703 * sin_deg(l2 - 2.0 * l3 + p3) - 0.00670 * sin_deg(2.0 * (psi - PIj)) - 0.00540 * sin_deg(l3 - l4) + 0.00481 * sin_deg(p1 + p3 - 2.0 * PIj - 2.0 * G) - 0.00409 * sin_deg(l2 - 2.0 * l3 + p2) + 0.00379 * sin_deg(l2 - 2.0 * l3 + p4) + 0.00235 * sin_deg(psi - w3) + 0.00198 * sin_deg(psi - w4) + 0.00180 * sin_deg(PHI_l) + 0.00129 * sin_deg(3.0 * (l3 - l4)) + 0.00124 * sin_deg(l1 - l3) - 0.00119 * sin_deg(5.0 * G1 - 2.0 * G + 52.225) + 0.00109 * sin_deg(l1 - l2) - 0.00099 * sin_deg(3.0 * l3 - 7.0 * l4 + 4.0 * p4) + 0.00091 * sin_deg(w3 - w4) + 0.00081 * sin_deg(3.0 * l3 - 7.0 * l4 + p3 + 3.0 * p4) - 0.00076 * sin_deg(2.0 * l2 - 3.0 * l3 + p3) + 0.00069 * sin_deg(p4 - PIj) - 0.00058 * sin_deg(2.0 * l3 - 3.0 * l4 + p4) + 0.00057 * sin_deg(l3 + p3 - 2.0 * PIj - 2.0 * G) - 0.00057 * sin_deg(l3 - 2.0 * l4 + p4) - 0.00052 * sin_deg(l2 - 2.0 * l3 + p1) - 0.00052 * sin_deg(p2 - p3) + 0.00048 * sin_deg(l3 - 2.0 * l4 + p3) - 0.00045 * sin_deg(2.0 * l2 - 3.0 * l3 + p4) - 0.00041 * sin_deg(p2 - p4) - 0.00038 * sin_deg(2.0 * G) - 0.00033 * sin_deg(p3 - p4 + w3 - w4) - 0.00032 * sin_deg(3.0 * l3 - 7.0 * l4 + 2.0 * p3 + 2.0 * p4) + 0.00030 * sin_deg(4.0 * (l3 - l4)) - 0.00029 * sin_deg(w3 + psi - 2.0 * PIj - 2.0 * G) + 0.00029 * sin_deg(l3 + p4 - 2.0 * PIj - 2.0 * G) + 0.00026 * sin_deg(l3 - PIj - G) + 0.00024 * sin_deg(l2 - 3.0 * l3 + 2.0 * l4) + 0.00021 * sin_deg(2.0 * (l3 - PIj - G)) - 0.00021 * sin_deg(l3 - p2) + 0.00017 * sin_deg(2.0 * (l3 - p3));
				L = l3 + S;
				B = atan_deg(+0.0032364 * sin_deg(L - w3) - 0.0016911 * sin_deg(L - psi) + 0.0006849 * sin_deg(L - w4) - 0.0002806 * sin_deg(L - w2) + 0.0000321 * sin_deg(L + psi - 2.0 * PIj - 2.0 * G) + 0.0000051 * sin_deg(L - psi + G) - 0.0000045 * sin_deg(L - psi - G) - 0.0000045 * sin_deg(L + psi - 2.0 * PIj) + 0.0000037 * sin_deg(L + psi - 2.0 * PIj - 3.0 * G) + 0.0000030 * sin_deg(2.0 * l2 - 3.0 * L + 4.03 * S + w2) - 0.0000021 * sin_deg(2.0 * l2 - 3.0 * L + 4.03 * S + w3));
				R = 14.99240 * (1.0 - 0.0014377 * cos_deg(l3 - p3) - 0.0007904 * cos_deg(l3 - p4) + 0.0006342 * cos_deg(l2 - l3) - 0.0001758 * cos_deg(2.0 * (l3 - l4)) + 0.0000294 * cos_deg(l3 - l4) - 0.0000156 * cos_deg(3.0 * (l3 - l4)) + 0.0000155 * cos_deg(l1 - l3) - 0.0000153 * cos_deg(l1 - l2) + 0.0000070 * cos_deg(2.0 * l2 - 3.0 * l3 + p3) - 0.0000051 * cos_deg(l3 + p3 - 2.0 * PIj - 2.0 * G));
				K = 27558.0;
				break;

			case 3: // IV, Callisto
				S = +0.84109 * sin_deg(l4 - p4) + 0.03429 * sin_deg(p4 - p3) - 0.03305 * sin_deg(2.0 * (psi - PIj)) - 0.03211 * sin_deg(G) - 0.01860 * sin_deg(l4 - p3) + 0.01182 * sin_deg(psi - w4) + 0.00622 * sin_deg(l4 + p4 - 2.0 * G - 2.0 * PIj) + 0.00385 * sin_deg(2.0 * (l4 - p4)) - 0.00284 * sin_deg(5.0 * G1 - 2.0 * G + 52.225) - 0.00233 * sin_deg(2.0 * (psi - p4)) - 0.00223 * sin_deg(l3 - l4) - 0.00208 * sin_deg(l4 - PIj) + 0.00177 * sin_deg(psi + w4 - 2.0 * p4) + 0.00134 * sin_deg(p4 - PIj) + 0.00125 * sin_deg(2.0 * (l4 - G - PIj)) - 0.00117 * sin_deg(2.0 * G) - 0.00112 * sin_deg(2.0 * (l3 - l4)) + 0.00106 * sin_deg(3.0 * l3 - 7.0 * l4 + 4.0 * p4) + 0.00102 * sin_deg(l4 - G - PIj) + 0.00096 * sin_deg(2.0 * l4 - psi - w4) + 0.00087 * sin_deg(2.0 * (psi - w4)) - 0.00087 * sin_deg(3.0 * l3 - 7.0 * l4 + p3 + 3.0 * p4) + 0.00085 * sin_deg(l3 - 2.0 * l4 + p4) - 0.00081 * sin_deg(2.0 * (l4 - psi)) + 0.00071 * sin_deg(l4 + p4 - 2.0 * PIj - 3.0 * G) + 0.00060 * sin_deg(l1 - l4) - 0.00056 * sin_deg(psi - w3) - 0.00055 * sin_deg(l3 - 2.0 * l4 + p3) + 0.00051 * sin_deg(l2 - l4) + 0.00042 * sin_deg(2.0 * (psi - G - PIj)) + 0.00039 * sin_deg(2.0 * (p4 - w4)) + 0.00036 * sin_deg(psi + PIj - p4 - w4) + 0.00035 * sin_deg(2.0 * G1 - G + 188.37) - 0.00035 * sin_deg(l4 - p4 + 2.0 * PIj - 2.0 * psi) - 0.00032 * sin_deg(l4 + p4 - 2.0 * PIj - G) + 0.00030 * sin_deg(2.0 * G1 - 2.0 * G + 149.15) + 0.00030 * sin_deg(3.0 * l3 - 7.0 * l4 + 2.0 * p3 + 2.0 * p4) + 0.00028 * sin_deg(l4 - p4 + 2.0 * psi - 2.0 * PIj) - 0.00028 * sin_deg(2.0 * (l4 - w4)) - 0.00027 * sin_deg(p3 - p4 + w3 - w4) - 0.00026 * sin_deg(5.0 * G1 - 3.0 * G + 188.37) + 0.00025 * sin_deg(w4 - w3) - 0.00025 * sin_deg(l2 - 3.0 * l3 + 2.0 * l4) - 0.00023 * sin_deg(3.0 * (l3 - l4)) + 0.00021 * sin_deg(2.0 * l4 - 2.0 * PIj - 3.0 * G) - 0.00021 * sin_deg(2.0 * l3 - 3.0 * l4 + p4) + 0.00019 * sin_deg(l4 - p4 - G) - 0.00019 * sin_deg(2.0 * l4 - p3 - p4) - 0.00018 * sin_deg(l4 - p4 + G) - 0.00016 * sin_deg(l4 + p3 - 2.0 * PIj - 2.0 * G);
				L = l4 + S;
				B = atan_deg(-0.0076579 * sin_deg(L - psi) + 0.0044148 * sin_deg(L - w4) - 0.0005106 * sin_deg(L - w3) + 0.0000773 * sin_deg(L + psi - 2.0 * PIj - 2.0 * G) + 0.0000104 * sin_deg(L - psi + G) - 0.0000102 * sin_deg(L - psi - G) + 0.0000088 * sin_deg(L + psi - 2.0 * PIj - 3.0 * G) - 0.0000038 * sin_deg(L + psi - 2.0 * PIj - G));
				R = 26.36990 * (1.0 - 0.0073391 * cos_deg(l4 - p4) + 0.0001620 * cos_deg(l4 - p3) + 0.0000974 * cos_deg(l3 - l4) - 0.0000541 * cos_deg(l4 + p4 - 2.0 * PIj - 2.0 * G) - 0.0000269 * cos_deg(2.0 * (l4 - p4)) + 0.0000182 * cos_deg(l4 - PIj) + 0.0000177 * cos_deg(2.0 * (l3 - l4)) - 0.0000167 * cos_deg(2.0 * l4 - psi - w4) + 0.0000167 * cos_deg(psi - w4) - 0.0000155 * cos_deg(2.0 * (l4 - PIj - G)) + 0.0000142 * cos_deg(2.0 * (l4 - psi)) + 0.0000104 * cos_deg(l1 - l4) + 0.0000092 * cos_deg(l2 - l4) - 0.0000089 * cos_deg(l4 - PIj - G) - 0.0000062 * cos_deg(l4 + p4 - 2.0 * PIj - 3.0 * G) + 0.0000048 * cos_deg(2.0 * (l4 - w4)));
				K = 36548.0;
				break;
			}

			// The precessional adjustment, P, made to both L and psi by Meeus, cancels out
			// inside this loop. Since I'm not saving L, and psi should remain unadjusted for
			// the series calculations, I only use P to produce PHI (derived from psi) later.

			X[j] = R * cos_deg(L - psi) * cos_deg(B);
			Y[j] = R * sin_deg(L - psi) * cos_deg(B);
			Z[j] = R * sin_deg(B);
			coef_K[j] = K;
			coef_R[j] = R;
		}

		double T0 = (time_JDE - Constant.B1950) / Constant.JULIAN_DAYS_PER_CENTURY;
		double P = 1.3966626 * T0 + 0.0003088 * T0 * T0;
		double T = 1.0 + Functions.toCenturies(time_JDE);
		double I = 3.120262 + 0.0006 * T;
		T = T - 1.0; // From J2000
		double OMEGA = 100.464441 + T * (1.020955 + T * (.00040117 + T * 0.000000569));
		double i = 1.303270 + T * (-.0054966 + T * (4.65e-6 - T * 0.000000004));
		double PHI = psi + P - OMEGA;

		// Now we set up the ol' ficticious moon.
		X[nmoons] = 0.0;
		Y[nmoons] = 0.0;
		Z[nmoons] = 1.0;

		double D = 0, D_sun = 0, D_ecl = 0, D_ecl_sun = 0;
		double Y1;

		MoonEphemElement moon[] = new MoonEphemElement[nmoons];
		loc = new LocationElement(ephem.rightAscension, ephem.declination, ephem.distance);
		loc = CoordinateSystem.equatorialToEcliptic(loc, time, obs, eph);

		// We'll loop backwards so we can compute D from the ficticious moon first.
		for (int j = nmoons; j >= 0; j--)
		{
			double sat_pos[] = MoonEphem.getSatellitePosition(X[j], Y[j], Z[j], I, PHI, i, OMEGA, L0, B0, L0_sun,
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
				double apparent_pos[] = MoonEphem.getApparentPosition(TARGET.JUPITER, sat_pos, D, DELTA, D_sun,
						DELTA_sun, coef_K[j], coef_R[j]);
				double apparent_pos_from_ecliptic[] = MoonEphem.getApparentPosition(TARGET.JUPITER, sat_pos, D_ecl,
						DELTA, D_ecl_sun, DELTA_sun, coef_K[j], coef_R[j]);

				// Obtain equatorial and horizontal coordinates
				double ecl_lon = loc.getLongitude() - apparent_pos_from_ecliptic[0] * ephem.angularRadius;
				double ecl_lat = loc.getLatitude() + apparent_pos_from_ecliptic[1] * ephem.angularRadius;
				double ecl_r = DELTA + apparent_pos_from_ecliptic[2] * TARGET.JUPITER.equatorialRadius / Constant.AU;
				LocationElement sat_loc = new LocationElement(ecl_lon, ecl_lat, ecl_r);
				pos = LocationElement.parseLocationElement(CoordinateSystem
						.eclipticToEquatorial(sat_loc, time, obs, eph));
				new_loc = LocationElement.parseRectangularCoordinates(pos);
				LocationElement hor_loc = CoordinateSystem.equatorialToHorizontal(new_loc, time, obs, eph);

				// Set results of ephemeris
				String nom = TARGET.values()[TARGET.Io.ordinal() + j].getName();
				double ra = new_loc.getLongitude();
				double dec = new_loc.getLatitude();
				double dist = new_loc.getRadius();
				double azi = hor_loc.getLongitude();
				double ele = hor_loc.getLatitude();
				double RE = ephem_sun.distance;
				double RP = DELTA_sun + apparent_pos[5] * TARGET.JUPITER.equatorialRadius / Constant.AU;
				double RO = dist;
				double DELO = (RE * RE + RO * RO - RP * RP) / (2.0 * RE * RO);
				double DPH = ((RP * RP + RO * RO - RE * RE) / (2.0 * RP * RO));
				double ill = (1.0 + DPH) * 0.5;
				double elo = Math.acos(DELO);

				// From the observer
				boolean inferior = (apparent_pos[2] <= 0.0);
				Y1 = apparent_pos[1] * flattening;

				TARGET target = TARGET.values()[TARGET.Io.ordinal() + j];
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
				// to the Earth, and to the Sun. If my calculations are correct, this means a maximum
				// error of 30 km in the position of the shadow of Callisto projected on Jupiter. This
				// error is well below the position error of the theory.
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
				new_eph.targetBody = target;
				moon[j] = MoonPhysicalParameters.physicalParameters(time_JDE, ephem_sun, moon[j], obs, new_eph);

				// Obtain relative phenomena
				if (j == 0)
					moon = MoonEphem.satellitesPhenomena(moon, ephem.angularRadius);
			}
		}

		return moon;
	}

	/**
	 * This is an implementation of the E5 Jovian satellite theory by Jay
	 * Lieske, as presented by Jean Meeus. Code adapted from Kerry Shetline. It
	 * has not been extensively checked, although it seems to be ok, with
	 * improvements in the 0.01 arcsecond level compared to E2x3. For reference
	 * see J. Lieske, Galilean Satellite Ephemerides E5, A&amp;A 129, 205-217
	 * (1998). Objects are Io, Europa, Ganymede, and Callisto.
	 * <P>
	 * Satellites are positioned respect to the mother planet. For this purpose
	 * the Moshier fit is used by default. You can select Series96 theory to
	 * obtain the position of the satellites in the Ephemeris object.
	 * This is recommended when possible (20th and 21st centuries).
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @return An array of {@linkplain MoonEphem} objects with the ephemeris.
	 * @deprecated L1 ephemerides theory is recommended instead.
	 * @throws JPARSECException If the calculation fails.
	 */
	public static MoonEphemElement[] galileanSatellitesEphemerides_E5(TimeElement time, // Time
																						// Element
			ObserverElement obs, // Observer Element
			EphemerisElement eph) // Ephemeris Element
			throws JPARSECException
	{
		int nmoons = 4;
		double L0 = 0.0, B0 = 0.0, DELTA = 0.0;
		double L0_sun = 0.0, B0_sun = 0.0, DELTA_sun = 0.0;

		double time_JDE = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);

		// Obtain ephemeris of Jupiter and Sun
		EphemerisElement new_eph = eph.clone();
		new_eph.targetBody = TARGET.JUPITER;
		EphemerisElement new_eph_sun = eph.clone();
		new_eph_sun.targetBody = TARGET.SUN;

		// Obtain position of planet
		EphemElement ephem = new EphemElement();
		ephem = MoonEphem.getBodyEphem(time, obs, new_eph, new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, time_JDE);

		// Obtain position of sun
		EphemElement ephem_sun = new EphemElement();
		ephem_sun = MoonEphem.getBodyEphem(time, obs, new_eph_sun, new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, time_JDE);

		// Set light delay
		double lightDelay = ephem.lightTime;
		if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.GEOMETRIC)
			lightDelay = 0.0;

		// Obtain heliocentric ecliptic coordinates of the date for Jupiter, as seen from the observer and
		// from the Sun. Later heliocentric position will be used for calculating if the satellite is
		// eclipsed or not.
		L0_sun = ephem.heliocentricEclipticLongitude * Constant.RAD_TO_DEG;
		B0_sun = ephem.heliocentricEclipticLatitude * Constant.RAD_TO_DEG;
		DELTA_sun = ephem.distanceFromSun;

		LocationElement loc = new LocationElement(ephem.rightAscension, ephem.declination, ephem.distance);
		double pos[] = LocationElement.parseLocationElement(CoordinateSystem.equatorialToEcliptic(loc, time, obs, eph));
		LocationElement new_loc = LocationElement.parseRectangularCoordinates(pos);

		L0 = new_loc.getLongitude() * Constant.RAD_TO_DEG;
		B0 = new_loc.getLatitude() * Constant.RAD_TO_DEG;
		DELTA = new_loc.getRadius();

		// Start calculations for satellites
		double t = time_JDE - 2443000.5 - lightDelay;
		double flattening = 1.069303;

		double l1 = 106.07719 + 203.488955790 * t;
		double l2 = 175.73161 + 101.374724735 * t;
		double l3 = 120.55883 + 50.317609207 * t;
		double l4 = 84.44459 + 21.571071177 * t;

		double p1 = 97.0881 + 0.16138586 * t;
		double p2 = 154.8663 + 0.04726307 * t;
		double p3 = 188.1840 + 0.00712734 * t;
		double p4 = 335.2868 + 0.00184000 * t;

		double w1 = 312.3346 - 0.13279386 * t;
		double w2 = 100.4411 - 0.03263064 * t;
		double w3 = 119.1942 - 0.00717703 * t;
		double w4 = 322.6186 - 0.00175934 * t;

		double GAMMA = 0.33033 * sin_deg(163.679 + 0.0010512 * t) + 0.03439 * sin_deg(34.486 - 0.0161713 * t);
		double PHI_l = 199.6766 + 0.17379190 * t;
		double psi = 316.5182 - 0.00000208 * t;
		double G = 30.23756 + 0.0830925701 * t + GAMMA;
		double G1 = 31.97853 + 0.0334597339 * t;
		double PIj = 13.469942;

		double S, L = 0.0, B = 0.0, R = 0.0, K = 0.0;
		double[] X = new double[nmoons + 1];
		double[] Y = new double[nmoons + 1];
		double[] Z = new double[nmoons + 1];
		double[] coef_K = new double[nmoons + 1];
		double[] coef_R = new double[nmoons + 1];

		for (int j = 0; j < nmoons; ++j)
		{
			switch (j)
			{
			case 0: // I, Io
				S = +0.47259 * sin_deg(2.0 * (l1 - l2)) - 0.03478 * sin_deg(p3 - p4) + 0.01081 * sin_deg(l2 - 2.0 * l3 + p3) + 0.00738 * sin_deg(PHI_l) + 0.00713 * sin_deg(l2 - 2.0 * l3 + p2) - 0.00674 * sin_deg(p1 + p3 - 2.0 * PIj - 2.0 * G) + 0.00666 * sin_deg(l2 - 2.0 * l3 + p4) + 0.00445 * sin_deg(l1 - p3) - 0.00354 * sin_deg(l1 - l2) - 0.00317 * sin_deg(2.0 * psi - 2.0 * PIj) + 0.00265 * sin_deg(l1 - p4) - 0.00186 * sin_deg(G) + 0.00162 * sin_deg(p2 - p3) + 0.00158 * sin_deg(4.0 * (l1 - l2)) - 0.00155 * sin_deg(l1 - l3) - 0.00138 * sin_deg(psi + w3 - 2.0 * PIj - 2.0 * G) - 0.00115 * sin_deg(2.0 * (l1 - 2.0 * l2 + w2)) + 0.00089 * sin_deg(p2 - p4) + 0.00085 * sin_deg(l1 + p3 - 2.0 * PIj - 2.0 * G) + 0.00083 * sin_deg(w2 - w3) + 0.00053 * sin_deg(psi - w2);
				L = l1 + S;
				B = atan_deg(+0.0006393 * sin_deg(L - w1) + 0.0001825 * sin_deg(L - w2) + 0.0000329 * sin_deg(L - w3) - 0.0000311 * sin_deg(L - psi) + 0.0000093 * sin_deg(L - w4) + 0.0000075 * sin_deg(3.0 * L - 4.0 * l2 - 1.9927 * S + w2) + 0.0000046 * sin_deg(L + psi - 2.0 * PIj - 2.0 * G));
				R = 5.90569 * (1.0 - 0.0041339 * cos_deg(2.0 * (l1 - l2)) - 0.0000387 * cos_deg(l1 - p3) - 0.0000214 * cos_deg(l1 - p4) + 0.0000170 * cos_deg(l1 - l2) - 0.0000131 * cos_deg(4.0 * (l1 - l2)) + 0.0000106 * cos_deg(l1 - l3) - 0.0000066 * cos_deg(l1 + p3 - 2.0 * PIj - 2.0 * G));
				K = 17295.0;
				break;
			case 1: // II, Europa
				S = +1.06476 * sin_deg(2.0 * (l2 - l3)) + 0.04256 * sin_deg(l1 - 2.0 * l2 + p3) + 0.03581 * sin_deg(l2 - p3) + 0.02395 * sin_deg(l1 - 2.0 * l2 + p4) + 0.01984 * sin_deg(l2 - p4) - 0.01778 * sin_deg(PHI_l) + 0.01654 * sin_deg(l2 - p2) + 0.01334 * sin_deg(l2 - 2.0 * l3 + p2) + 0.01294 * sin_deg(p3 - p4) - 0.01142 * sin_deg(l2 - l3) - 0.01057 * sin_deg(G) - 0.00775 * sin_deg(2.0 * (psi - PIj)) + 0.00524 * sin_deg(2.0 * (l1 - l2)) - 0.00460 * sin_deg(l1 - l3) + 0.00316 * sin_deg(psi - 2.0 * G + w3 - 2.0 * PIj) - 0.00203 * sin_deg(p1 + p3 - 2.0 * PIj - 2.0 * G) + 0.00146 * sin_deg(psi - w3) - 0.00145 * sin_deg(2.0 * G) + 0.00125 * sin_deg(psi - w4) - 0.00115 * sin_deg(l1 - 2.0 * l3 + p3) - 0.00094 * sin_deg(2.0 * (l2 - w2)) + 0.00086 * sin_deg(2.0 * (l1 - 2.0 * l2 + w2)) - 0.00086 * sin_deg(5.0 * G1 - 2.0 * G + 52.225) - 0.00078 * sin_deg(l2 - l4) - 0.00064 * sin_deg(3.0 * l3 - 7.0 * l4 + 4.0 * p4) + 0.00064 * sin_deg(p1 - p4) - 0.00063 * sin_deg(l1 - 2.0 * l3 + p4) + 0.00058 * sin_deg(w3 - w4) + 0.00056 * sin_deg(2.0 * (psi - PIj - G)) + 0.00056 * sin_deg(2.0 * (l2 - l4)) + 0.00055 * sin_deg(2.0 * (l1 - l3)) + 0.00052 * sin_deg(3.0 * l3 - 7.0 * l4 + p3 + 3.0 * p4) - 0.00043 * sin_deg(l1 - p3) + 0.00041 * sin_deg(5.0 * (l2 - l3)) + 0.00041 * sin_deg(p4 - PIj) + 0.00032 * sin_deg(w2 - w3) + 0.00032 * sin_deg(2.0 * (l3 - G - PIj));
				L = l2 + S;
				B = atan_deg(+0.0081004 * sin_deg(L - w2) + 0.0004512 * sin_deg(L - w3) - 0.0003284 * sin_deg(L - psi) + 0.0001160 * sin_deg(L - w4) + 0.0000272 * sin_deg(l1 - 2.0 * l3 + 1.0146 * S + w2) - 0.0000144 * sin_deg(L - w1) + 0.0000143 * sin_deg(L + psi - 2.0 * PIj - 2.0 * G) + 0.0000035 * sin_deg(L - psi + G) - 0.0000028 * sin_deg(l1 - 2.0 * l3 + 1.0146 * S + w3));
				R = 9.39657 * (1.0 + 0.0093848 * cos_deg(l1 - l2) - 0.0003116 * cos_deg(l2 - p3) - 0.0001744 * cos_deg(l2 - p4) - 0.0001442 * cos_deg(l2 - p2) + 0.0000553 * cos_deg(l2 - l3) + 0.0000523 * cos_deg(l1 - l3) - 0.0000290 * cos_deg(2.0 * (l1 - l2)) + 0.0000164 * cos_deg(2.0 * (l2 - w2)) + 0.0000107 * cos_deg(l1 - 2.0 * l3 + p3) - 0.0000102 * cos_deg(l2 - p1) - 0.0000091 * cos_deg(2.0 * (l1 - l3)));
				K = 21819.0;
				break;
			case 2: // III, Ganymede
				S = +0.16490 * sin_deg(l3 - p3) + 0.09081 * sin_deg(l3 - p4) - 0.06907 * sin_deg(l2 - l3) + 0.03784 * sin_deg(p3 - p4) + 0.01846 * sin_deg(2.0 * (l3 - l4)) - 0.01340 * sin_deg(G) - 0.01014 * sin_deg(2.0 * (psi - PIj)) + 0.00704 * sin_deg(l2 - 2.0 * l3 + p3) - 0.00620 * sin_deg(l2 - 2.0 * l3 + p2) - 0.00541 * sin_deg(l3 - l4) + 0.00381 * sin_deg(l2 - 2.0 * l3 + p4) + 0.00235 * sin_deg(psi - w3) + 0.00198 * sin_deg(psi - w4) + 0.00176 * sin_deg(PHI_l) + 0.00130 * sin_deg(3.0 * (l3 - l4)) + 0.00125 * sin_deg(l1 - l3) - 0.00119 * sin_deg(5.0 * G1 - 2.0 * G + 52.225) + 0.00109 * sin_deg(l1 - l2) - 0.00100 * sin_deg(3.0 * l3 - 7.0 * l4 + 4.0 * p4) + 0.00091 * sin_deg(w3 - w4) + 0.00080 * sin_deg(3.0 * l3 - 7.0 * l4 + p3 + 3.0 * p4) - 0.00075 * sin_deg(2.0 * l2 - 3.0 * l3 + p3) + 0.00072 * sin_deg(p1 + p3 - 2.0 * PIj - 2.0 * G) + 0.00069 * sin_deg(p4 - PIj) - 0.00058 * sin_deg(2.0 * l3 - 3.0 * l4 + p4) - 0.00057 * sin_deg(l3 - 2.0 * l4 + p4) + 0.00056 * sin_deg(l3 + p3 - 2.0 * PIj - 2.0 * G) - 0.00052 * sin_deg(l2 - 2.0 * l3 + p1) - 0.00050 * sin_deg(p2 - p3) + 0.00048 * sin_deg(l3 - 2.0 * l4 + p3) - 0.00045 * sin_deg(2.0 * l2 - 3.0 * l3 + p4) - 0.00041 * sin_deg(p2 - p4) - 0.00038 * sin_deg(2.0 * G) - 0.00037 * sin_deg(p3 - p4 + w3 - w4) - 0.00032 * sin_deg(3.0 * l3 - 7.0 * l4 + 2.0 * p3 + 2.0 * p4) + 0.00030 * sin_deg(4.0 * (l3 - l4)) + 0.00029 * sin_deg(l3 + p4 - 2.0 * PIj - 2.0 * G) - 0.00028 * sin_deg(w3 + psi - 2.0 * PIj - 2.0 * G) + 0.00026 * sin_deg(l3 - PIj - G) + 0.00024 * sin_deg(l2 - 3.0 * l3 + 2.0 * l4) + 0.00021 * sin_deg(2.0 * (l3 - PIj - G)) - 0.00021 * sin_deg(l3 - p2) + 0.00017 * sin_deg(2.0 * (l3 - p3));
				L = l3 + S;
				B = atan_deg(+0.0032402 * sin_deg(L - w3) - 0.0016911 * sin_deg(L - psi) + 0.0006847 * sin_deg(L - w4) - 0.0002797 * sin_deg(L - w2) + 0.0000321 * sin_deg(L + psi - 2.0 * PIj - 2.0 * G) + 0.0000051 * sin_deg(L - psi + G) - 0.0000045 * sin_deg(L - psi - G) - 0.0000045 * sin_deg(L + psi - 2.0 * PIj) + 0.0000037 * sin_deg(L + psi - 2.0 * PIj - 3.0 * G) + 0.0000030 * sin_deg(2.0 * l2 - 3.0 * L + 4.03 * S + w2) - 0.0000021 * sin_deg(2.0 * l2 - 3.0 * L + 4.03 * S + w3));
				R = 14.98832 * (1.0 - 0.0014388 * cos_deg(l3 - p3) - 0.0007919 * cos_deg(l3 - p4) + 0.0006342 * cos_deg(l2 - l3) - 0.0001761 * cos_deg(2.0 * (l3 - l4)) + 0.0000294 * cos_deg(l3 - l4) - 0.0000156 * cos_deg(3.0 * (l3 - l4)) + 0.0000156 * cos_deg(l1 - l3) - 0.0000153 * cos_deg(l1 - l2) + 0.0000070 * cos_deg(2.0 * l2 - 3.0 * l3 + p3) - 0.0000051 * cos_deg(l3 + p3 - 2.0 * PIj - 2.0 * G));
				K = 27558.0;
				break;
			case 3: // IV, Callisto
				S = +0.84287 * sin_deg(l4 - p4) + 0.03431 * sin_deg(p4 - p3) - 0.03305 * sin_deg(2.0 * (psi - PIj)) - 0.03211 * sin_deg(G) - 0.01862 * sin_deg(l4 - p3) + 0.01186 * sin_deg(psi - w4) + 0.00623 * sin_deg(l4 + p4 - 2.0 * G - 2.0 * PIj) + 0.00387 * sin_deg(2.0 * (l4 - p4)) - 0.00284 * sin_deg(5.0 * G1 - 2.0 * G + 52.225) - 0.00234 * sin_deg(2.0 * (psi - p4)) - 0.00223 * sin_deg(l3 - l4) - 0.00208 * sin_deg(l4 - PIj) + 0.00178 * sin_deg(psi + w4 - 2.0 * p4) + 0.00134 * sin_deg(p4 - PIj) + 0.00125 * sin_deg(2.0 * (l4 - G - PIj)) - 0.00117 * sin_deg(2.0 * G) - 0.00112 * sin_deg(2.0 * (l3 - l4)) + 0.00107 * sin_deg(3.0 * l3 - 7.0 * l4 + 4.0 * p4) + 0.00102 * sin_deg(l4 - G - PIj) + 0.00096 * sin_deg(2.0 * l4 - psi - w4) + 0.00087 * sin_deg(2.0 * (psi - w4)) - 0.00085 * sin_deg(3.0 * l3 - 7.0 * l4 + p3 + 3.0 * p4) + 0.00085 * sin_deg(l3 - 2.0 * l4 + p4) - 0.00081 * sin_deg(2.0 * (l4 - psi)) + 0.00071 * sin_deg(l4 + p4 - 2.0 * PIj - 3.0 * G) + 0.00061 * sin_deg(l1 - l4) - 0.00056 * sin_deg(psi - w3) - 0.00054 * sin_deg(l3 - 2.0 * l4 + p3) + 0.00051 * sin_deg(l2 - l4) + 0.00042 * sin_deg(2.0 * (psi - G - PIj)) + 0.00039 * sin_deg(2.0 * (p4 - w4)) + 0.00036 * sin_deg(psi + PIj - p4 - w4) + 0.00035 * sin_deg(2.0 * G1 - G + 188.37) - 0.00035 * sin_deg(l4 - p4 + 2.0 * PIj - 2.0 * psi) - 0.00032 * sin_deg(l4 + p4 - 2.0 * PIj - G) + 0.00030 * sin_deg(2.0 * G1 - 2.0 * G + 149.15) + 0.00029 * sin_deg(3.0 * l3 - 7.0 * l4 + 2.0 * p3 + 2.0 * p4) + 0.00028 * sin_deg(l4 - p4 + 2.0 * psi - 2.0 * PIj) - 0.00028 * sin_deg(2.0 * (l4 - w4)) - 0.00027 * sin_deg(p3 - p4 + w3 - w4) - 0.00026 * sin_deg(5.0 * G1 - 3.0 * G + 188.37) + 0.00025 * sin_deg(w4 - w3) - 0.00025 * sin_deg(l2 - 3.0 * l3 + 2.0 * l4) - 0.00023 * sin_deg(3.0 * (l3 - l4)) + 0.00021 * sin_deg(2.0 * l4 - 2.0 * PIj - 3.0 * G) - 0.00021 * sin_deg(2.0 * l3 - 3.0 * l4 + p4) + 0.00019 * sin_deg(l4 - p4 - G) - 0.00019 * sin_deg(2.0 * l4 - p3 - p4) - 0.00018 * sin_deg(l4 - p4 + G) - 0.00016 * sin_deg(l4 + p3 - 2.0 * PIj - 2.0 * G);
				L = l4 + S;
				B = atan_deg(-0.0076579 * sin_deg(L - psi) + 0.0044134 * sin_deg(L - w4) - 0.0005112 * sin_deg(L - w3) + 0.0000773 * sin_deg(L + psi - 2.0 * PIj - 2.0 * G) + 0.0000104 * sin_deg(L - psi + G) - 0.0000102 * sin_deg(L - psi - G) + 0.0000088 * sin_deg(L + psi - 2.0 * PIj - 3.0 * G) - 0.0000038 * sin_deg(L + psi - 2.0 * PIj - G));
				R = 26.36273 * (1.0 - 0.0073546 * cos_deg(l4 - p4) + 0.0001621 * cos_deg(l4 - p3) + 0.0000974 * cos_deg(l3 - l4) - 0.0000543 * cos_deg(l4 + p4 - 2.0 * PIj - 2.0 * G) - 0.0000271 * cos_deg(2.0 * (l4 - p4)) + 0.0000182 * cos_deg(l4 - PIj) + 0.0000177 * cos_deg(2.0 * (l3 - l4)) - 0.0000167 * cos_deg(2.0 * l4 - psi - w4) + 0.0000167 * cos_deg(psi - w4) - 0.0000155 * cos_deg(2.0 * (l4 - PIj - G)) + 0.0000142 * cos_deg(2.0 * (l4 - psi)) + 0.0000105 * cos_deg(l1 - l4) + 0.0000092 * cos_deg(l2 - l4) - 0.0000089 * cos_deg(l4 - PIj - G) - 0.0000062 * cos_deg(l4 + p4 - 2.0 * PIj - 3.0 * G) + 0.0000048 * cos_deg(2.0 * (l4 - w4)));
				K = 36548.0;
				break;
			}

			// The precessional adjustment, P, made to both L and psi by Meeus,
			// cancels out inside this loop. Since I'm not saving L, and psi should remain
			// unadjusted for the series calculations, I only use P to produce PHI (derived
			// from psi) later.
			X[j] = R * cos_deg(L - psi) * cos_deg(B);
			Y[j] = R * sin_deg(L - psi) * cos_deg(B);
			Z[j] = R * sin_deg(B);
			coef_K[j] = K;
			coef_R[j] = R;
		}

		double T0 = (time_JDE - Constant.B1950) / Constant.JULIAN_DAYS_PER_CENTURY;
		double P = 1.3966626 * T0 + 0.0003088 * T0 * T0;
		double T = 1.0 + Functions.toCenturies(time_JDE);
		double I = 3.120262 + 0.0006 * T;
		T = T - 1.0; // From J2000
		double OMEGA = 100.464441 + T * (1.020955 + T * (.00040117 + T * 0.000000569));
		double i = 1.303270 + T * (-.0054966 + T * (4.65e-6 - T * 0.000000004));
		double PHI = psi + P - OMEGA;

		// Now we set up the ol' ficticious moon.
		X[nmoons] = 0.0;
		Y[nmoons] = 0.0;
		Z[nmoons] = 1.0;

		double D = 0, D_sun = 0, D_ecl = 0, D_ecl_sun = 0;
		double Y1;

		MoonEphemElement moon[] = new MoonEphemElement[nmoons];
		loc = new LocationElement(ephem.rightAscension, ephem.declination, ephem.distance);
		loc = CoordinateSystem.equatorialToEcliptic(loc, time, obs, eph);

		// We'll loop backwards so we can compute D from the ficticious moon first.
		for (int j = nmoons; j >= 0; --j)
		{

			double sat_pos[] = MoonEphem.getSatellitePosition(X[j], Y[j], Z[j], I, PHI, i, OMEGA, L0, B0, L0_sun,
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
				double apparent_pos[] = MoonEphem.getApparentPosition(TARGET.JUPITER, sat_pos, D, DELTA, D_sun,
						DELTA_sun, coef_K[j], coef_R[j]);
				double apparent_pos_from_ecliptic[] = MoonEphem.getApparentPosition(TARGET.JUPITER, sat_pos, D_ecl,
						DELTA, D_ecl_sun, DELTA_sun, coef_K[j], coef_R[j]);

				// Obtain equatorial and horizontal coordinates
				double ecl_lon = loc.getLongitude() - apparent_pos_from_ecliptic[0] * ephem.angularRadius;
				double ecl_lat = loc.getLatitude() + apparent_pos_from_ecliptic[1] * ephem.angularRadius;
				double ecl_r = DELTA + apparent_pos_from_ecliptic[2] * TARGET.JUPITER.ordinal() / Constant.AU;
				LocationElement sat_loc = new LocationElement(ecl_lon, ecl_lat, ecl_r);
				pos = LocationElement.parseLocationElement(CoordinateSystem
						.eclipticToEquatorial(sat_loc, time, obs, eph));
				new_loc = LocationElement.parseRectangularCoordinates(pos);
				LocationElement hor_loc = CoordinateSystem.equatorialToHorizontal(new_loc, time, obs, eph);

				// Set results of ephemeris
				String nom = TARGET.values()[TARGET.Io.ordinal() + j].getName();
				double ra = new_loc.getLongitude();
				double dec = new_loc.getLatitude();
				double dist = new_loc.getRadius();
				double azi = hor_loc.getLongitude();
				double ele = hor_loc.getLatitude();
				double RE = ephem_sun.distance;
				double RP = DELTA_sun + apparent_pos[5] * TARGET.JUPITER.equatorialRadius / Constant.AU;

				double RO = dist;
				double DELO = (RE * RE + RO * RO - RP * RP) / (2.0 * RE * RO);
				double DPH = ((RP * RP + RO * RO - RE * RE) / (2.0 * RP * RO));
				double ill = (1.0 + DPH) * 0.5;
				double elo = Math.acos(DELO);

				// From the observer
				boolean inferior = (apparent_pos[2] <= 0.0);
				Y1 = apparent_pos[1] * flattening;

				TARGET target = TARGET.values()[TARGET.Io.ordinal() + j];
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

				// From Sun. Note we are neglecting the difference between light
				// time from the planet to the Earth, and to the Sun. This introduces
				// no significant errors in the calculations.
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

//				boolean eclipsed = false, occulted = false, transiting = false, shadow_transiting = false, inferior = false;

				// Create ephemeris object
				moon[j] = new MoonEphemElement(nom, ra, dec, dist, RP, azi, ele, (float) ill, (float) elo, eclipsed, occulted,
						transiting, shadow_transiting, inferior, apparent_pos[0], apparent_pos[1], apparent_pos[2],
						apparent_pos[3], apparent_pos[4], apparent_pos[5]);

				// Obtain physical ephemerides
				new_eph.targetBody = TARGET.values()[TARGET.Io.ordinal() + j];
				moon[j] = MoonPhysicalParameters.physicalParameters(time_JDE, ephem_sun, moon[j], obs, new_eph);
//				moon[j] = MoonEphem.satellitePhenomena(moon[j], ephem, TARGET.JUPITER);

				// Obtain relative phenomena
				if (j == 0)
					moon = MoonEphem.satellitesPhenomena(moon, ephem.angularRadius);
			}
		}

		return moon;
	}

	private static double sin_deg(double deg)
	{
		return Math.sin(deg * Constant.DEG_TO_RAD);
	}

	private static double cos_deg(double deg)
	{
		return Math.cos(deg * Constant.DEG_TO_RAD);
	}

	private static double atan_deg(double tan)
	{
		return Math.atan(tan) * Constant.RAD_TO_DEG;
	}
}
