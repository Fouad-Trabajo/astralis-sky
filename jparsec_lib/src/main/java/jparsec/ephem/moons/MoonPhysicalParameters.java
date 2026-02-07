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

import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.Functions;
import jparsec.ephem.PhysicalParameters;
import jparsec.ephem.Precession;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.planets.EphemElement;
import jparsec.math.Constant;
import jparsec.observer.ExtraterrestrialObserverElement;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.util.JPARSECException;

/**
 * A class to obtain the orientation and physical ephemeris of a planet, based
 * on IAU 2000/2006/2009/2015 recomendations. The different implementations are used
 * for the different values of the ephem method to apply in the ephemeris object.
 * The default method (for values different from IAU 2006/2009/2015) is the
 * IAU 2000 recommendations for the orientations of the planetary axes.
 *
 * @see MoonEphem
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class MoonPhysicalParameters
{
	// private constructor so that this class cannot be instantiated.
	private MoonPhysicalParameters() {}

	/**
	 * Obtain physical parameters of the satellite: angular radius, visual
	 * magnitude, and axis orientation. Any satellite with known information
	 * (see IAU2009 report by B. A. Archinal et al.) is supported. No supported
	 * objects will have zero in the corresponding fields.
	 *
	 * @param JD Julian day in dynamical time.
	 * @param ephem_sun Ephem object with ephemeris of sun.
	 * @param ephem_obj Moon ephem object to take and store data.
	 * @param obs The observer object.
	 * @param eph Ephemeris object defining the ephemeris properties.
	 * @return Moon ephem with the completed data.
	 * @throws JPARSECException If the object data cannot be found.
	 */
	public static MoonEphemElement physicalParameters(double JD, // Julian
																	// day
			EphemElement ephem_sun, // Ephem Element
			MoonEphemElement ephem_obj, // Ephem Element
			ObserverElement obs,
			EphemerisElement eph) // Ephemeris Element
			throws JPARSECException
	{
		MoonEphemElement ephem = ephem_obj.clone();

		// Put distances and angles in a more confortable way
		double RE = ephem_sun.distance;
		double RP = ephem.distanceFromSun;
		double RO = ephem.distance;
		double LP = ephem.heliocentricEclipticLongitude;
		double eq_sun[] = LocationElement.parseLocationElement(new LocationElement(ephem_sun.rightAscension,
				ephem_sun.declination, ephem_sun.distance));
		double LE = Math.PI + LocationElement.parseRectangularCoordinates(
				Ephem.equatorialToEcliptic(eq_sun, JD, eph)).getLongitude();

		if (eph.targetBody != TARGET.SUN)
		{
			// Elongation
			//double DELO = (RE * RE + RO * RO - RP * RP) / (2.0 * RE * RO);
			//ephem.elongation = (float) Math.acos(DELO);
			// Returns true aparent elongation, not geometric (aberration ... everything corrected).
			ephem.elongation = (float) LocationElement.getAngularDistance(
					new LocationElement(ephem.rightAscension, ephem.declination, ephem.distance), ephem_sun.getEquatorialLocation());
			// Correct RP to obtain true phase angle
			RP = Math.sqrt(-(Math.cos(ephem.elongation) * 2.0 * RE * RO - RE * RE - RO * RO));

			// Phase and phase angle. Note phase angle can be
			// negative to represent the case LE < LP (before opposition)
			double DPH = ((RP * RP + RO * RO - RE * RE) / (2.0 * RP * RO)); // Cos(phase angle)
			double DPHA = RE * Math.sin(LE - LP) / RO; // Sin(phase angle)
			ephem.phaseAngle = (float) (Math.acos(DPH) * DPHA / Math.abs(DPHA));
			ephem.phase = (float) ((1.0 + DPH) * 0.5);
			if (DPHA < 0) ephem.phaseAngle = -ephem.phaseAngle;
		}

		// Now compute disk orientation as seen by the observer
		ephem.positionAngleOfAxis = 0.0f;
		ephem.positionAngleOfPole = 0.0f;
		ephem.brightLimbAngle = 0.0f;
		ephem.subsolarLatitude = 0.0f;
		ephem.subsolarLongitude = 0.0f;
		ephem.longitudeOfCentralMeridian = 0.0f;
		ephem.magnitude = EphemElement.INVALID_MAGNITUDE;
		ephem.angularRadius = 0f;

		if (eph.targetBody == TARGET.NOT_A_PLANET) return ephem;

		// Visual magnitude and axis orientation
		double rr = ephem.distance * ephem.distanceFromSun;
		double mag = EphemElement.INVALID_MAGNITUDE;
		ephem.northPoleRA = 0.0f;
		ephem.northPoleDEC = 0.0f;
		double lon0 = 0.0; // Initial longitude at JD = 2451545.0
		double rot_per_day = 0.0; // Degrees per day of rotation of satellite

		double rotationModel[] = null;
		switch (eph.ephemMethod) {
		case IAU_2006:
			rotationModel = getIAU2006Model(eph.targetBody, JD - ephem.distance * Constant.LIGHT_TIME_DAYS_PER_AU, rr);
			break;
		case IAU_2009:
			rotationModel = getIAU2009Model(eph.targetBody, JD - ephem.distance * Constant.LIGHT_TIME_DAYS_PER_AU, rr);
			break;
		case IAU_2015:
			rotationModel = getIAU2015Model(eph.targetBody, JD - ephem.distance * Constant.LIGHT_TIME_DAYS_PER_AU, rr);
			break;
		default:
			rotationModel = getIAU2000Model(eph.targetBody, JD - ephem.distance * Constant.LIGHT_TIME_DAYS_PER_AU, rr);
			break;
		}

		if (rotationModel != null) {
			mag = rotationModel[0];
			ephem.northPoleRA = rotationModel[1];
			ephem.northPoleDEC = rotationModel[2];
			lon0 = rotationModel[3];
			rot_per_day = rotationModel[4];
			ephem = calcAxis(JD, ephem_sun, ephem, lon0, rot_per_day, eph, obs.getMotherBody());
		}

		// Apparent visual magnitude
		ephem.magnitude = (float) mag;

		// Correct magnitude for phase
		if (ephem.phase < 1.0 && mag != EphemElement.INVALID_MAGNITUDE) {
			if (ephem.phase <= 0) {
				ephem.magnitude = EphemElement.INVALID_MAGNITUDE;
			} else {
				double L = Math.pow(10.0, -mag / 2.5) * ephem.phase;
				ephem.magnitude = (float) (-2.5 * Math.log10(L));
			}
		}

		// Angular radius in radians
		try {
			ephem.angularRadius = (float) Math.atan(eph.targetBody.equatorialRadius / (ephem.distance * Constant.AU));
		} catch (Exception exc) {}

		return ephem;
	}

	/**
	 * Calculate orientation of a satellite providing the position and the
	 * direction of the north pole of rotation. The IAU 2000/2006/2009 models are used
	 * depending on the properties of the ephemeris element object. See the
	 * <I> REPORT OF THE IAU/IAG WORKING GROUP ON CARTOGRAPHIC COORDINATES
	 * AND ROTATIONAL ELEMENTS: 2009</I>, B. A. Archinal et al, in Celestial
	 * Mechanics and Dynamical Astronomy, 2011.
	 * <P>
	 *
	 * @param JD Julian day in TDB.
	 * @param ephem_sun Ephem object with all data for the sun.
	 * @param ephem_obj Moon ephem object with all data completed except those
	 *        fields related to the disk orientation (equinox of date).
	 * @param lon0 Initial longitude of satellite at JD = 2451545.0.
	 * @param rot_per_day Rotation speed in degrees/day.
	 * @param eph Ephemeris object defining the ephemeris properties.
	 * @param motherBody The mother body for the calculations. Can be set to null
	 * for the Earth.
	 * @return Moon ephem object with all the fields.
	 * @throws JPARSECException If an error occurs.
	 */
	public static MoonEphemElement calcAxis(double JD, EphemElement ephem_sun, MoonEphemElement ephem_obj, double lon0,
			double rot_per_day, EphemerisElement eph, TARGET motherBody)
	throws JPARSECException {
		EphemElement ephem = EphemElement.parseMoonEphemElement(ephem_obj, JD);

		ephem = PhysicalParameters.calcAxis(JD, ephem_sun, ephem, lon0, rot_per_day, eph, motherBody);

		MoonEphemElement moon_ephem = ephem_obj.clone();

		moon_ephem.positionAngleOfAxis = ephem.positionAngleOfAxis;
		moon_ephem.positionAngleOfPole = ephem.positionAngleOfPole;
		moon_ephem.brightLimbAngle = ephem.brightLimbAngle;
		moon_ephem.subsolarLatitude = ephem.subsolarLatitude;
		moon_ephem.subsolarLongitude = ephem.subsolarLongitude;
		moon_ephem.longitudeOfCentralMeridian = ephem.longitudeOfCentralMeridian;
		moon_ephem.northPoleDEC = ephem.northPoleDEC;
		moon_ephem.northPoleRA = ephem.northPoleRA;

		return moon_ephem;
	}

	/**
	 * Returns the magnitude and rotation model parameters of the body following IAU 2000
	 * recommendations.
	 * @param target Target body.
	 * @param JD Julian day in dynamical time.
	 * @param rr Distance parameter of the body: distance (to Earth) * distance from Sun.
	 * @return The magnitude and rotation model for IAU 2000: magnitude, north pole RA,
	 * north pole DEC (rad), longitude of planet at J2000 (deg), and speed rotation in degrees/day.
	 * Null is returned for a target without these data.
	 */
	public static double[] getIAU2000Model(TARGET target, double JD, double rr) {
		double mag = 0, northPoleRA = 0, northPoleDEC = 0, rot_per_day = 0, lon0 = 0;

		double calc_time = Functions.toCenturies(JD);

		double J1 = (073.32 + 91472.9 * calc_time) * Constant.DEG_TO_RAD;
		double J2 = (024.62 + 45137.2 * calc_time) * Constant.DEG_TO_RAD;
		double J3 = (283.90 + 4850.7 * calc_time) * Constant.DEG_TO_RAD;
		double J4 = (355.80 + 1191.3 * calc_time) * Constant.DEG_TO_RAD;
		double J5 = (119.90 + 262.1 * calc_time) * Constant.DEG_TO_RAD;
		double J6 = (229.80 + 64.3 * calc_time) * Constant.DEG_TO_RAD;
		double J7 = (352.25 + 2382.6 * calc_time) * Constant.DEG_TO_RAD;
		double J8 = (113.35 + 6070.0 * calc_time) * Constant.DEG_TO_RAD;
		double S1 = (353.32 + 75706.7 * calc_time) * Constant.DEG_TO_RAD;
		double S2 = (028.72 + 75706.7 * calc_time) * Constant.DEG_TO_RAD;
		double S3 = (177.40 - 36505.5 * calc_time) * Constant.DEG_TO_RAD;
		double S4 = (300.00 - 7225.9 * calc_time) * Constant.DEG_TO_RAD;
		double S5 = (316.45 + 506.2 * calc_time) * Constant.DEG_TO_RAD;
		double S6 = (345.20 - 1016.3 * calc_time) * Constant.DEG_TO_RAD;
		double S7 = (29.80 - 52.1 * calc_time) * Constant.DEG_TO_RAD;
		double U1 = (115.75 + 54991.87 * calc_time) * Constant.DEG_TO_RAD;
		double U2 = (141.69 + 41887.66 * calc_time) * Constant.DEG_TO_RAD;
		double U3 = (135.03 + 29927.35 * calc_time) * Constant.DEG_TO_RAD;
		double U4 = (061.77 + 25733.59 * calc_time) * Constant.DEG_TO_RAD;
		double U5 = (249.32 + 24471.46 * calc_time) * Constant.DEG_TO_RAD;
		double U6 = (043.86 + 22278.41 * calc_time) * Constant.DEG_TO_RAD;
		double U7 = (077.66 + 20289.42 * calc_time) * Constant.DEG_TO_RAD;
		double U8 = (157.36 + 16652.76 * calc_time) * Constant.DEG_TO_RAD;
		double U9 = (101.81 + 12872.63 * calc_time) * Constant.DEG_TO_RAD;
		double U10 = (138.64 + 8061.81 * calc_time) * Constant.DEG_TO_RAD;
		double U11 = (102.23 - 2024.22 * calc_time) * Constant.DEG_TO_RAD;
		double U12 = (316.41 + 2863.96 * calc_time) * Constant.DEG_TO_RAD;
		double U13 = (304.01 - 51.94 * calc_time) * Constant.DEG_TO_RAD;
		double U14 = (308.71 - 93.17 * calc_time) * Constant.DEG_TO_RAD;
		double U15 = (340.82 - 75.32 * calc_time) * Constant.DEG_TO_RAD;
		double U16 = (259.14 - 504.81 * calc_time) * Constant.DEG_TO_RAD;
		double N0 = (357.85 + 52.316 * calc_time) * Constant.DEG_TO_RAD;
		double N1 = (323.92 + 62606.6 * calc_time) * Constant.DEG_TO_RAD;
		double N2 = (220.51 + 55064.2 * calc_time) * Constant.DEG_TO_RAD;
		double N3 = (354.27 + 46564.5 * calc_time) * Constant.DEG_TO_RAD;
		double N4 = (075.31 + 26109.4 * calc_time) * Constant.DEG_TO_RAD;
		double N5 = (035.36 + 14325.4 * calc_time) * Constant.DEG_TO_RAD;
		double N6 = (142.61 + 2824.6 * calc_time) * Constant.DEG_TO_RAD;
		double N7 = (177.85 + 52.316 * calc_time) * Constant.DEG_TO_RAD;

		switch (target)
		{
		case Phobos:
			mag = 13.25 + 5.0 * Math.log10(rr);
			double M1 = (169.51 - 0.4357640 * (JD - Constant.J2000)) * Constant.DEG_TO_RAD;
			double M2 = (192.93 + 1128.4096700 * (JD - Constant.J2000) + 8.864 * calc_time * calc_time) * Constant.DEG_TO_RAD;
			northPoleRA = (float) ((317.68 - 0.108 * calc_time + 1.79 * Math.sin(M1)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((52.90 - 0.061 * calc_time - 1.08 * Math.cos(M1)) * Constant.DEG_TO_RAD);
			rot_per_day = 1128.8445850;
			lon0 = 35.06 + 8.864 * calc_time * calc_time - 1.42 * Math.sin(M1) - 0.78 * Math.sin(M2);
			break;
		case Deimos:
			mag = 13.88 + 5.0 * Math.log10(rr);
			double M3 = (53.57 - 0.0181510 * (JD - Constant.J2000)) * Constant.DEG_TO_RAD;
			northPoleRA = (float) ((316.65 - 0.108 * calc_time + 2.98 * Math.sin(M3)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((53.52 - 0.061 * calc_time - 1.78 * Math.cos(M3)) * Constant.DEG_TO_RAD);
			rot_per_day = 285.1618970;
			lon0 = 79.41 - 0.520 * calc_time * calc_time - 2.58 * Math.sin(M3) - 0.19 * Math.sin(M3);
			break;
		case Io:
			mag = -1.64 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((268.05 - 0.009 * calc_time + 0.094 * Math.sin(J3) + 0.024 * Math.sin(J4)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((64.50 + 0.003 * calc_time + 0.040 * Math.cos(J3) + 0.011 * Math.cos(J4)) * Constant.DEG_TO_RAD);
			lon0 = 200.39 - 0.085 * Math.sin(J3) - 0.022 * Math.sin(J4);
			rot_per_day = 203.4889538;
			break;
		case Europa:
			mag = -1.37 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((268.08 - 0.009 * calc_time + 1.086 * Math.sin(J4) + 0.060 * Math.sin(J5) + 0.015 * Math
					.sin(J6) + 0.009 * Math.sin(J7)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((64.51 + 0.003 * calc_time + 0.468 * Math.cos(J4) + 0.026 * Math.cos(J5) + 0.007 * Math
					.cos(J6) + 0.002 * Math.cos(J7)) * Constant.DEG_TO_RAD);
			lon0 = 36.022 - 0.980 * Math.sin(J4) - 0.054 * Math.sin(J5) - 0.014 * Math.sin(J6) - 0.008 * Math.sin(J7);
			rot_per_day = 101.3747235;
			break;
		case Ganymede:
			mag = -2.04 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((268.20 - 0.009 * calc_time - 0.037 * Math.sin(J4) + 0.431 * Math.sin(J5) + 0.091 * Math
					.sin(J6)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((64.57 + 0.003 * calc_time - 0.016 * Math.cos(J4) + 0.186 * Math.cos(J5) + 0.039 * Math
					.cos(J6)) * Constant.DEG_TO_RAD);
			lon0 = 44.064 + 0.033 * Math.sin(J4) - 0.389 * Math.sin(J5) - 0.082 * Math.sin(J6);
			rot_per_day = 50.3176081;
			break;
		case Callisto:
			mag = -1.0 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((268.72 - 0.009 * calc_time - 0.068 * Math.sin(J5) + 0.590 * Math.sin(J6) + 0.010 * Math
					.sin(J8)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((64.83 + 0.003 * calc_time - 0.029 * Math.cos(J5) + 0.254 * Math.cos(J6) - 0.004 * Math
					.cos(J8)) * Constant.DEG_TO_RAD);
			lon0 = 259.51 + 0.061 * Math.sin(J5) - 0.533 * Math.sin(J6) - 0.009 * Math.sin(J8);
			rot_per_day = 21.5710715;
			break;
		case Mimas:
			mag = 3.3 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.66 - 0.036 * calc_time + 13.56 * Math.sin(S3)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.52 - 0.004 * calc_time - 1.53 * Math.cos(S3)) * Constant.DEG_TO_RAD);
			lon0 = 337.46 - 13.48 * Math.sin(S3) - 44.85 * Math.sin(S5);
			rot_per_day = 381.9945550;
			break;
		case Enceladus:
			mag = 2.1 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.66 - 0.036 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.52 - 0.004 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 2.82;
			rot_per_day = 262.7318996;
			break;
		case Tethys:
			mag = 0.6 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.66 - 0.036 * calc_time + 9.66 * Math.sin(S4)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.52 - 0.004 * calc_time - 1.09 * Math.cos(S4)) * Constant.DEG_TO_RAD);
			lon0 = 10.45 - 9.60 * Math.sin(S4) + 2.23 * Math.sin(S5);
			rot_per_day = 190.6979085;
			break;
		case Dione:
			mag = 0.88 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.66 - 0.036 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.52 - 0.004 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 357.00;
			rot_per_day = 131.5349316;
			break;
		case Rhea:
			mag = 0.16 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.38 - 0.036 * calc_time + 3.10 * Math.sin(S6)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.55 - 0.004 * calc_time - 0.35 * Math.cos(S6)) * Constant.DEG_TO_RAD);
			lon0 = 235.16 - 3.08 * Math.sin(S6);
			rot_per_day = 79.6900478;
			break;
		case Titan:
			mag = -1.29 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((36.41 - 0.036 * calc_time + 2.66 * Math.sin(S7)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.94 - 0.004 * calc_time - 0.30 * Math.cos(S7)) * Constant.DEG_TO_RAD);
			lon0 = 189.64 - 2.64 * Math.sin(S7);
			rot_per_day = 22.5769768;
			break;
		case Hyperion: // No data available
			mag = 4.63 + 5.0 * Math.log10(rr);
			northPoleRA = 0.0f;
			northPoleDEC = 0.0f;
			lon0 = 0.0;
			rot_per_day = 0.0;
			break;
		case Iapetus:
			mag = 1.5 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((318.16 - 3.949 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((75.03 - 1.143 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 350.20;
			rot_per_day = 4.5379572;
			break;
		case Miranda:
			mag = 3.6 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.43 + 4.41 * Math.sin(U11) - 0.04 * Math.sin(2.0 * U11)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.08 + 4.25 * Math.cos(U11) - 0.02 * Math.cos(2.0 * U11)) * Constant.DEG_TO_RAD);
			lon0 = 30.70 - 1.27 * Math.sin(U12) + 0.15 * Math.sin(2.0 * U12) + 1.15 * Math.sin(U11) - 0.09 * Math
					.sin(2.0 * U11);
			rot_per_day = -254.6906892;
			break;
		case Ariel:
			mag = 1.45 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.43 + 0.29 * Math.sin(U13)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.10 + 0.28 * Math.cos(U13)) * Constant.DEG_TO_RAD);
			lon0 = 156.22 + 0.05 * Math.sin(U12) + 0.08 * Math.sin(U13);
			rot_per_day = -142.8356681;
			break;
		case Umbriel:
			mag = 2.1 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.43 + 0.21 * Math.sin(U14)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.10 + 0.20 * Math.cos(U14)) * Constant.DEG_TO_RAD);
			lon0 = 108.95 - 0.09 * Math.sin(U12) + 0.086 * Math.sin(U14);
			rot_per_day = -86.8688923;
			break;
		case Titania:
			mag = 1.02 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.43 + 0.29 * Math.sin(U15)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.10 + 0.28 * Math.cos(U15)) * Constant.DEG_TO_RAD);
			lon0 = 77.74 + 0.08 * Math.sin(U15);
			rot_per_day = -41.3514316;
			break;
		case Oberon:
			mag = 1.23 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.43 + 0.16 * Math.sin(U16)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.10 + 0.16 * Math.cos(U16)) * Constant.DEG_TO_RAD);
			lon0 = 6.77 + 0.04 * Math.sin(U16);
			rot_per_day = -26.7394932;
			break;
		case Triton:
			mag = -1.24 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((299.36 - 32.35 * Math.sin(N7) - 6.28 * Math.sin(2.0 * N7) - 2.08 * Math
					.sin(3.0 * N7) - 0.74 * Math.sin(4.0 * N7) - 0.28 * Math.sin(5.0 * N7) - 0.11 * Math.sin(6.0 * N7) - 0.07 * Math
					.sin(7.0 * N7) - 0.02 * Math.sin(8.0 * N7) - 0.01 * Math.sin(9.0 * N7)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((41.17 + 22.55 * Math.cos(N7) + 2.10 * Math.cos(2.0 * N7) + 0.55 * Math
					.cos(3.0 * N7) + 0.16 * Math.cos(4.0 * N7) + 0.05 * Math.cos(5.0 * N7) + 0.02 * Math.cos(6.0 * N7) + 0.01 * Math
					.cos(7.0 * N7)) * Constant.DEG_TO_RAD);
			lon0 = 296.53 + 22.25 * Math.sin(N7) + 6.73 * Math.sin(2.0 * N7) + 2.05 * Math.sin(3.0 * N7) + 0.74 * Math
					.sin(4.0 * N7) + 0.28 * Math.sin(5.0 * N7) + 0.11 * Math.sin(6.0 * N7) + 0.07 * Math.sin(7.0 * N7) + 0.02 * Math
					.sin(8.0 * N7) + 0.01 * Math.sin(9.0 * N7);
			rot_per_day = -61.2572637;
			break;
		case Charon:
			mag = -0.99 + 5.0 * Math.log10(rr);
			northPoleRA = (float) (313.02 * Constant.DEG_TO_RAD);
			northPoleDEC = (float) (9.09 * Constant.DEG_TO_RAD);
			lon0 = 56.77;
			rot_per_day = -56.3623195;
			break;
		case Metis:
			mag = 10.9 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((268.05 - 0.009 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((64.49 + 0.003 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 346.09;
			rot_per_day = 1221.2547301;
			break;
		case Adrastea:
			mag = 12.5 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((268.05 - 0.009 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((64.49 + 0.003 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 33.29;
			rot_per_day = 1206.9986602;
			break;
		case Amalthea:
			mag = 7.5 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((268.05 - 0.009 * calc_time - 0.84 * Math.sin(J1) + 0.01 * Math.sin(2.0 * J1)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((64.49 + 0.003 * calc_time - 0.36 * Math.cos(J1)) * Constant.DEG_TO_RAD);
			lon0 = 231.67 + 0.76 * Math.sin(J1) - 0.01 * Math.sin(2.0 * J1);
			rot_per_day = 722.6314560;
			break;
		case Thebe:
			mag = 9.1 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((268.05 - 0.009 * calc_time - 2.11 * Math.sin(J2) + 0.04 * Math.sin(2.0 * J2)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((64.49 + 0.003 * calc_time - 0.91 * Math.cos(J2) + 0.01 * Math.cos(2.0 * J2)) * Constant.DEG_TO_RAD);
			lon0 = 8.56 + 1.91 * Math.sin(J2) - 0.04 * Math.sin(2.0 * J2);
			rot_per_day = 533.7004100;
			break;
		case Pan:
			mag = 9.4 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.6 - 0.036 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.5 - 0.004 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 48.8;
			rot_per_day = 626.0440000;
			break;
		case Atlas:
			mag = 8.4 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.58 - 0.036 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.53 - 0.004 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 137.88;
			rot_per_day = 598.3060000;
			break;
		case Prometheus:
			mag = 6.2 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.58 - 0.036 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.53 - 0.004 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 296.14;
			rot_per_day = 587.2890000;
			break;
		case Pandora:
			mag = 6.9 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.58 - 0.036 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.53 - 0.004 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 162.92;
			rot_per_day = 572.7891000;
			break;
		case Epimetheus:
			mag = 6.1 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.58 - 0.036 * calc_time - 3.153 * Math.sin(S1) + 0.086 * Math.sin(2.0 * S1)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.52 - 0.004 * calc_time - 0.356 * Math.cos(S1) + 0.005 * Math.cos(2.0 * S1)) * Constant.DEG_TO_RAD);
			lon0 = 293.87 + 3.133 * Math.sin(S1) - 0.086 * Math.sin(2.0 * S1);
			rot_per_day = 518.4907239;
			break;
		case Janus:
			mag = 4.9 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.58 - 0.036 * calc_time - 1.623 * Math.sin(S2) + 0.023 * Math.sin(2.0 * S2)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.52 - 0.004 * calc_time - 0.183 * Math.cos(S2) + 0.001 * Math.cos(2.0 * S2)) * Constant.DEG_TO_RAD);
			lon0 = 58.83 + 1.613 * Math.sin(S2) - 0.023 * Math.sin(2.0 * S2);
			rot_per_day = 518.2359876;
			break;
		case Telesto:
			mag = 8.9 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((50.51 - 0.036 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((84.06 - 0.004 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 56.88;
			rot_per_day = 190.6979332;
			break;
		case Calypso:
			mag = 9.1 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((36.41 - 0.036 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((85.04 - 0.004 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 153.51;
			rot_per_day = 190.6742373;
			break;
		case Helene:
			mag = 8.8 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.85 - 0.036 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.34 - 0.004 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 245.12;
			rot_per_day = 131.6174056;
			break;
		case Phoebe:
			mag = 6.9 + 5.0 * Math.log10(rr);
			northPoleRA = (float) (355.00 * Constant.DEG_TO_RAD);
			northPoleDEC = (float) (68.70 * Constant.DEG_TO_RAD);
			lon0 = 304.70;
			rot_per_day = 930.8338720;
			break;
		case Cordelia:
			mag = 11.4 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.31 - 0.15 * Math.sin(U1)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.18 + 0.14 * Math.cos(U1)) * Constant.DEG_TO_RAD);
			lon0 = 127.69 - 0.04 * Math.sin(U1);
			rot_per_day = -1074.520573;
			break;
		case Ophelia:
			mag = 11.1 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.31 - 0.09 * Math.sin(U2)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.18 + 0.09 * Math.cos(U2)) * Constant.DEG_TO_RAD);
			lon0 = 130.35 - 0.03 * Math.sin(U2);
			rot_per_day = -956.406815;
			break;
		case Bianca:
			mag = 10.3 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.31 - 0.16 * Math.sin(U3)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.18 + 0.16 * Math.cos(U3)) * Constant.DEG_TO_RAD);
			lon0 = 105.46 - 0.04 * Math.sin(U3);
			rot_per_day = -828.3914760;
			break;
		case Cressida:
			mag = 9.4 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.31 - 0.04 * Math.sin(U4)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.18 + 0.04 * Math.cos(U4)) * Constant.DEG_TO_RAD);
			lon0 = 59.16 - 0.01 * Math.sin(U4);
			rot_per_day = -776.581632;
			break;
		case Desdemona:
			mag = 9.8 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.31 - 0.17 * Math.sin(U5)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.18 + 0.16 * Math.cos(U5)) * Constant.DEG_TO_RAD);
			lon0 = 95.08 - 0.04 * Math.sin(U5);
			rot_per_day = -760.0531690;
			break;
		case Juliet:
			mag = 8.9 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.31 - 0.06 * Math.sin(U6)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.18 + 0.06 * Math.cos(U6)) * Constant.DEG_TO_RAD);
			lon0 = 302.56 - 0.02 * Math.sin(U6);
			rot_per_day = -730.1253660;
			break;
		case Portia:
			mag = 8.2 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.31 - 0.09 * Math.sin(U7)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.18 + 0.09 * Math.cos(U7)) * Constant.DEG_TO_RAD);
			lon0 = 25.03 - 0.02 * Math.sin(U7);
			rot_per_day = -701.4865870;
			break;
		case Rosalind:
			mag = 9.6 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.31 - 0.29 * Math.sin(U8)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.18 + 0.28 * Math.cos(U8)) * Constant.DEG_TO_RAD);
			lon0 = 314.90 - 0.08 * Math.sin(U8);
			rot_per_day = -644.6311260;
			break;
		case Belinda:
			mag = 9.3 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.31 - 0.03 * Math.sin(U9)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.18 + 0.03 * Math.cos(U9)) * Constant.DEG_TO_RAD);
			lon0 = 297.46 - 0.01 * Math.sin(U9);
			rot_per_day = -577.3628170;
			break;
		case Puck:
			mag = 7.5 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.31 - 0.33 * Math.sin(U10)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.18 + 0.31 * Math.cos(U10)) * Constant.DEG_TO_RAD);
			lon0 = 91.24 - 0.09 * Math.sin(U10);
			rot_per_day = -472.5450690;
			break;
		case Naiad:
			mag = 9.86 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((299.36 + 0.70 * Math.sin(N0) - 6.49 * Math.sin(N1) + 0.25 * Math.sin(2.0 * N1)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((43.36 - 0.51 * Math.cos(N0) - 4.75 * Math.cos(N1) + 0.09 * Math.cos(2.0 * N1)) * Constant.DEG_TO_RAD);
			lon0 = 254.06 - 0.48 * Math.sin(N0) + 4.40 * Math.sin(N1) - 0.27 * Math.sin(2.0 * N1);
			rot_per_day = 1222.8441209;
			break;
		case Thalassa:
			mag = 9.16 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((299.36 + 0.70 * Math.sin(N0) - 0.28 * Math.sin(N2)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((43.45 - 0.51 * Math.cos(N0) - 0.21 * Math.cos(N2)) * Constant.DEG_TO_RAD);
			lon0 = 102.06 - 0.48 * Math.sin(N0) + 0.19 * Math.sin(N2);
			rot_per_day = 1155.7555612;
			break;
		case Despina:
			mag = 7.76 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((299.36 + 0.70 * Math.sin(N0) - 0.09 * Math.sin(N3)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((43.45 - 0.51 * Math.cos(N0) - 0.07 * Math.cos(N3)) * Constant.DEG_TO_RAD);
			lon0 = 306.51 - 0.49 * Math.sin(N0) + 0.06 * Math.sin(N3);
			rot_per_day = 1075.7341562;
			break;
		case Galatea:
			mag = 7.76 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((299.36 + 0.70 * Math.sin(N0) - 0.07 * Math.sin(N4)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((43.43 - 0.51 * Math.cos(N0) - 0.05 * Math.cos(N4)) * Constant.DEG_TO_RAD);
			lon0 = 258.09 - 0.48 * Math.sin(N0) + 0.05 * Math.sin(N4);
			rot_per_day = 839.6597686;
			break;
		case Larissa:
			mag = 7.26 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((299.36 + 0.70 * Math.sin(N0) - 0.27 * Math.sin(N5)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((43.41 - 0.51 * Math.cos(N0) - 0.20 * Math.cos(N5)) * Constant.DEG_TO_RAD);
			lon0 = 179.41 - 0.48 * Math.sin(N0) + 0.19 * Math.sin(N5);
			rot_per_day = 649.0534470;
			break;
		case Proteus:
			mag = 5.76 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((299.27 + 0.70 * Math.sin(N0) - 0.05 * Math.sin(N6)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((42.91 - 0.51 * Math.cos(N0) - 0.04 * Math.cos(N6)) * Constant.DEG_TO_RAD);
			lon0 = 93.38 - 0.48 * Math.sin(N0) + 0.04 * Math.sin(N6);
			rot_per_day = 320.7654228;
			break;
		default:
			return null;
		}

		return new double[] {mag, northPoleRA, northPoleDEC, lon0, rot_per_day};
	}

	/**
	 * Returns the magnitude and rotation model parameters of the body following IAU 2006
	 * recommendations.
	 * @param target Target body.
	 * @param JD Julian day in dynamical time.
	 * @param rr Distance parameter of the body: distance (to Earth) * distance from Sun.
	 * @return The magnitude and rotation model for IAU 2006: magnitude, north pole RA,
	 * north pole DEC (rad), longitude of planet at J2000 (deg), and speed rotation in degrees/day.
	 * Null is returned for a target without these data.
	 */
	public static double[] getIAU2006Model(TARGET target, double JD, double rr) {
		double mag = 0, northPoleRA = 0, northPoleDEC = 0, rot_per_day = 0, lon0 = 0;

		double calc_time = Functions.toCenturies(JD);

		double J1 = (073.32 + 91472.9 * calc_time) * Constant.DEG_TO_RAD;
		double J2 = (024.62 + 45137.2 * calc_time) * Constant.DEG_TO_RAD;
		double J3 = (283.90 + 4850.7 * calc_time) * Constant.DEG_TO_RAD;
		double J4 = (355.80 + 1191.3 * calc_time) * Constant.DEG_TO_RAD;
		double J5 = (119.90 + 262.1 * calc_time) * Constant.DEG_TO_RAD;
		double J6 = (229.80 + 64.3 * calc_time) * Constant.DEG_TO_RAD;
		double J7 = (352.25 + 2382.6 * calc_time) * Constant.DEG_TO_RAD;
		double J8 = (113.35 + 6070.0 * calc_time) * Constant.DEG_TO_RAD;
		double S1 = (353.32 + 75706.7 * calc_time) * Constant.DEG_TO_RAD;
		double S2 = (028.72 + 75706.7 * calc_time) * Constant.DEG_TO_RAD;
		double S3 = (177.40 - 36505.5 * calc_time) * Constant.DEG_TO_RAD;
		double S4 = (300.00 - 7225.9 * calc_time) * Constant.DEG_TO_RAD;
		double S5 = (316.45 + 506.2 * calc_time) * Constant.DEG_TO_RAD;
		double S6 = (345.20 - 1016.3 * calc_time) * Constant.DEG_TO_RAD;
		double S7 = (29.80 - 52.1 * calc_time) * Constant.DEG_TO_RAD;
		double U1 = (115.75 + 54991.87 * calc_time) * Constant.DEG_TO_RAD;
		double U2 = (141.69 + 41887.66 * calc_time) * Constant.DEG_TO_RAD;
		double U3 = (135.03 + 29927.35 * calc_time) * Constant.DEG_TO_RAD;
		double U4 = (061.77 + 25733.59 * calc_time) * Constant.DEG_TO_RAD;
		double U5 = (249.32 + 24471.46 * calc_time) * Constant.DEG_TO_RAD;
		double U6 = (043.86 + 22278.41 * calc_time) * Constant.DEG_TO_RAD;
		double U7 = (077.66 + 20289.42 * calc_time) * Constant.DEG_TO_RAD;
		double U8 = (157.36 + 16652.76 * calc_time) * Constant.DEG_TO_RAD;
		double U9 = (101.81 + 12872.63 * calc_time) * Constant.DEG_TO_RAD;
		double U10 = (138.64 + 8061.81 * calc_time) * Constant.DEG_TO_RAD;
		double U11 = (102.23 - 2024.22 * calc_time) * Constant.DEG_TO_RAD;
		double U12 = (316.41 + 2863.96 * calc_time) * Constant.DEG_TO_RAD;
		double U13 = (304.01 - 51.94 * calc_time) * Constant.DEG_TO_RAD;
		double U14 = (308.71 - 93.17 * calc_time) * Constant.DEG_TO_RAD;
		double U15 = (340.82 - 75.32 * calc_time) * Constant.DEG_TO_RAD;
		double U16 = (259.14 - 504.81 * calc_time) * Constant.DEG_TO_RAD;
		double N0 = (357.85 + 52.316 * calc_time) * Constant.DEG_TO_RAD;
		double N1 = (323.92 + 62606.6 * calc_time) * Constant.DEG_TO_RAD;
		double N2 = (220.51 + 55064.2 * calc_time) * Constant.DEG_TO_RAD;
		double N3 = (354.27 + 46564.5 * calc_time) * Constant.DEG_TO_RAD;
		double N4 = (075.31 + 26109.4 * calc_time) * Constant.DEG_TO_RAD;
		double N5 = (035.36 + 14325.4 * calc_time) * Constant.DEG_TO_RAD;
		double N6 = (142.61 + 2824.6 * calc_time) * Constant.DEG_TO_RAD;
		double N7 = (177.85 + 52.316 * calc_time) * Constant.DEG_TO_RAD;

		switch (target)
		{
		case Phobos:
			mag = 13.25 + 5.0 * Math.log10(rr);
			double M1 = (169.51 - 0.4357640 * (JD - Constant.J2000)) * Constant.DEG_TO_RAD;
			double M2 = (192.93 + 1128.4096700 * (JD - Constant.J2000) + 8.864 * calc_time * calc_time) * Constant.DEG_TO_RAD;
			northPoleRA = (float) ((317.68 - 0.108 * calc_time + 1.79 * Math.sin(M1)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((52.90 - 0.061 * calc_time - 1.08 * Math.cos(M1)) * Constant.DEG_TO_RAD);
			rot_per_day = 1128.8445850;
			lon0 = 35.06 + 8.864 * calc_time * calc_time - 1.42 * Math.sin(M1) - 0.78 * Math.sin(M2);
			break;
		case Deimos:
			mag = 13.88 + 5.0 * Math.log10(rr);
			double M3 = (53.57 - 0.0181510 * (JD - Constant.J2000)) * Constant.DEG_TO_RAD;
			northPoleRA = (float) ((316.65 - 0.108 * calc_time + 2.98 * Math.sin(M3)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((53.52 - 0.061 * calc_time - 1.78 * Math.cos(M3)) * Constant.DEG_TO_RAD);
			rot_per_day = 285.1618970;
			lon0 = 79.41 - 0.520 * calc_time * calc_time - 2.58 * Math.sin(M3) + 0.19 * Math.cos(M3);
			break;
		case Io:
			mag = -1.64 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((268.05 - 0.009 * calc_time + 0.094 * Math.sin(J3) + 0.024 * Math.sin(J4)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((64.50 + 0.003 * calc_time + 0.040 * Math.cos(J3) + 0.011 * Math.cos(J4)) * Constant.DEG_TO_RAD);
			lon0 = 200.39 - 0.085 * Math.sin(J3) - 0.022 * Math.sin(J4);
			rot_per_day = 203.4889538;
			break;
		case Europa:
			mag = -1.37 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((268.08 - 0.009 * calc_time + 1.086 * Math.sin(J4) + 0.060 * Math.sin(J5) + 0.015 * Math.sin(J6) + 0.009 * Math.sin(J7)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((64.51 + 0.003 * calc_time + 0.468 * Math.cos(J4) + 0.026 * Math.cos(J5) + 0.007 * Math.cos(J6) + 0.002 * Math.cos(J7)) * Constant.DEG_TO_RAD);
			lon0 = 36.022 - 0.980 * Math.sin(J4) - 0.054 * Math.sin(J5) - 0.014 * Math.sin(J6) - 0.008 * Math.sin(J7);
			rot_per_day = 101.3747235;
			break;
		case Ganymede:
			mag = -2.04 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((268.20 - 0.009 * calc_time - 0.037 * Math.sin(J4) + 0.431 * Math.sin(J5) + 0.091 * Math.sin(J6)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((64.57 + 0.003 * calc_time - 0.016 * Math.cos(J4) + 0.186 * Math.cos(J5) + 0.039 * Math.cos(J6)) * Constant.DEG_TO_RAD);
			lon0 = 44.064 + 0.033 * Math.sin(J4) - 0.389 * Math.sin(J5) - 0.082 * Math.sin(J6);
			rot_per_day = 50.3176081;
			break;
		case Callisto:
			mag = -1.0 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((268.72 - 0.009 * calc_time - 0.068 * Math.sin(J5) + 0.590 * Math.sin(J6) + 0.010 * Math.sin(J8)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((64.83 + 0.003 * calc_time - 0.029 * Math.cos(J5) + 0.254 * Math.cos(J6) - 0.004 * Math.cos(J8)) * Constant.DEG_TO_RAD);
			lon0 = 259.51 + 0.061 * Math.sin(J5) - 0.533 * Math.sin(J6) - 0.009 * Math.sin(J8);
			rot_per_day = 21.5710715;
			break;
		case Mimas:
			mag = 3.3 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.66 - 0.036 * calc_time + 13.56 * Math.sin(S3)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.52 - 0.004 * calc_time - 1.53 * Math.cos(S3)) * Constant.DEG_TO_RAD);
			lon0 = 337.46 - 13.48 * Math.sin(S3) - 44.85 * Math.sin(S5);
			rot_per_day = 381.9945550;
			break;
		case Enceladus:
			mag = 2.1 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.66 - 0.036 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.52 - 0.004 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 2.82;
			rot_per_day = 262.7318996;
			break;
		case Tethys:
			mag = 0.6 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.66 - 0.036 * calc_time + 9.66 * Math.sin(S4)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.52 - 0.004 * calc_time - 1.09 * Math.cos(S4)) * Constant.DEG_TO_RAD);
			lon0 = 10.45 - 9.60 * Math.sin(S4) + 2.23 * Math.sin(S5);
			rot_per_day = 190.6979085;
			break;
		case Dione:
			mag = 0.88 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.66 - 0.036 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.52 - 0.004 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 357.00;
			rot_per_day = 131.5349316;
			break;
		case Rhea:
			mag = 0.16 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.38 - 0.036 * calc_time + 3.10 * Math.sin(S6)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.55 - 0.004 * calc_time - 0.35 * Math.cos(S6)) * Constant.DEG_TO_RAD);
			lon0 = 235.16 - 3.08 * Math.sin(S6);
			rot_per_day = 79.6900478;
			break;
		case Titan:
			mag = -1.29 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((36.41 - 0.036 * calc_time + 2.66 * Math.sin(S7)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.94 - 0.004 * calc_time - 0.30 * Math.cos(S7)) * Constant.DEG_TO_RAD);
			lon0 = 189.64 - 2.64 * Math.sin(S7);
			rot_per_day = 22.5769768;
			break;
		case Hyperion: // No data available
			mag = 4.63 + 5.0 * Math.log10(rr);
			northPoleRA = 0.0f;
			northPoleDEC = 0.0f;
			lon0 = 0.0;
			rot_per_day = 0.0;
			break;
		case Iapetus:
			mag = 1.5 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((318.16 - 3.949 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((75.03 - 1.143 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 350.20;
			rot_per_day = 4.5379572;
			break;
		case Miranda:
			mag = 3.6 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.43 + 4.41 * Math.sin(U11) - 0.04 * Math.sin(2.0 * U11)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.08 + 4.25 * Math.cos(U11) - 0.02 * Math.cos(2.0 * U11)) * Constant.DEG_TO_RAD);
			lon0 = 30.70 - 1.27 * Math.sin(U12) + 0.15 * Math.sin(2.0 * U12) + 1.15 * Math.sin(U11) - 0.09 * Math.sin(2.0 * U11);
			rot_per_day = -254.6906892;
			break;
		case Ariel:
			mag = 1.45 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.43 + 0.29 * Math.sin(U13)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.10 + 0.28 * Math.cos(U13)) * Constant.DEG_TO_RAD);
			lon0 = 156.22 + 0.05 * Math.sin(U12) + 0.08 * Math.sin(U13);
			rot_per_day = -142.8356681;
			break;
		case Umbriel:
			mag = 2.1 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.43 + 0.21 * Math.sin(U14)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.10 + 0.20 * Math.cos(U14)) * Constant.DEG_TO_RAD);
			lon0 = 108.05 - 0.09 * Math.sin(U12) + 0.06 * Math.sin(U14);
			rot_per_day = -86.8688923;
			break;
		case Titania:
			mag = 1.02 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.43 + 0.29 * Math.sin(U15)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.10 + 0.28 * Math.cos(U15)) * Constant.DEG_TO_RAD);
			lon0 = 77.74 + 0.08 * Math.sin(U15);
			rot_per_day = -41.3514316;
			break;
		case Oberon:
			mag = 1.23 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.43 + 0.16 * Math.sin(U16)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.10 + 0.16 * Math.cos(U16)) * Constant.DEG_TO_RAD);
			lon0 = 6.77 + 0.04 * Math.sin(U16);
			rot_per_day = -26.7394932;
			break;
		case Triton:
			mag = -1.24 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((299.36 - 32.35 * Math.sin(N7) - 6.28 * Math.sin(2.0 * N7) - 2.08 * Math
					.sin(3.0 * N7) - 0.74 * Math.sin(4.0 * N7) - 0.28 * Math.sin(5.0 * N7) - 0.11 * Math.sin(6.0 * N7) - 0.07 * Math
					.sin(7.0 * N7) - 0.02 * Math.sin(8.0 * N7) - 0.01 * Math.sin(9.0 * N7)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((41.17 + 22.55 * Math.cos(N7) + 2.10 * Math.cos(2.0 * N7) + 0.55 * Math
					.cos(3.0 * N7) + 0.16 * Math.cos(4.0 * N7) + 0.05 * Math.cos(5.0 * N7) + 0.02 * Math.cos(6.0 * N7) + 0.01 * Math
					.cos(7.0 * N7)) * Constant.DEG_TO_RAD);
			lon0 = 296.53 + 22.25 * Math.sin(N7) + 6.73 * Math.sin(2.0 * N7) + 2.05 * Math.sin(3.0 * N7) + 0.74 * Math
					.sin(4.0 * N7) + 0.28 * Math.sin(5.0 * N7) + 0.11 * Math.sin(6.0 * N7) + 0.05 * Math.sin(7.0 * N7) + 0.02 * Math
					.sin(8.0 * N7) + 0.01 * Math.sin(9.0 * N7);
			rot_per_day = -61.2572637;
			break;
		case Charon:
			mag = -0.99 + 5.0 * Math.log10(rr);
			northPoleRA = (float) (312.993 * Constant.DEG_TO_RAD);
			northPoleDEC = (float) (6.163 * Constant.DEG_TO_RAD);
			lon0 = 122.695; // See the 'erratum' by Archinal et al. 2011 (Celestial Mechanics & Dynamical Astronomy 110-4, 401-403)
			rot_per_day = 56.3625225; // See the 'erratum' by Archinal et al. 2011 (Celestial Mechanics & Dynamical Astronomy 110-4, 401-403)
			break;
		case Metis:
			mag = 10.9 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((268.05 - 0.009 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((64.49 + 0.003 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 346.09;
			rot_per_day = 1221.2547301;
			break;
		case Adrastea:
			mag = 12.5 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((268.05 - 0.009 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((64.49 + 0.003 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 33.29;
			rot_per_day = 1206.9986602;
			break;
		case Amalthea:
			mag = 7.5 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((268.05 - 0.009 * calc_time - 0.84 * Math.sin(J1) + 0.01 * Math.sin(2.0 * J1)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((64.49 + 0.003 * calc_time - 0.36 * Math.cos(J1)) * Constant.DEG_TO_RAD);
			lon0 = 231.67 + 0.76 * Math.sin(J1) - 0.01 * Math.sin(2.0 * J1);
			rot_per_day = 722.6314560;
			break;
		case Thebe:
			mag = 9.1 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((268.05 - 0.009 * calc_time - 2.11 * Math.sin(J2) + 0.04 * Math.sin(2.0 * J2)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((64.49 + 0.003 * calc_time - 0.91 * Math.cos(J2) + 0.01 * Math.cos(2.0 * J2)) * Constant.DEG_TO_RAD);
			lon0 = 8.56 + 1.91 * Math.sin(J2) - 0.04 * Math.sin(2.0 * J2);
			rot_per_day = 533.7004100;
			break;
		case Pan:
			mag = 9.4 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.6 - 0.036 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.5 - 0.004 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 48.8;
			rot_per_day = 626.0440000;
			break;
		case Atlas:
			mag = 8.4 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.58 - 0.036 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.53 - 0.004 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 137.88;
			rot_per_day = 598.3060000;
			break;
		case Prometheus:
			mag = 6.2 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.58 - 0.036 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.53 - 0.004 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 296.14;
			rot_per_day = 587.2890000;
			break;
		case Pandora:
			mag = 6.9 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.58 - 0.036 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.53 - 0.004 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 162.92;
			rot_per_day = 572.7891000;
			break;
		case Epimetheus:
			mag = 6.1 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.58 - 0.036 * calc_time - 3.153 * Math.sin(S1) + 0.086 * Math.sin(2.0 * S1)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.52 - 0.004 * calc_time - 0.356 * Math.cos(S1) + 0.005 * Math.cos(2.0 * S1)) * Constant.DEG_TO_RAD);
			lon0 = 293.87 + 3.133 * Math.sin(S1) - 0.086 * Math.sin(2.0 * S1);
			rot_per_day = 518.4907239;
			break;
		case Janus:
			mag = 4.9 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.58 - 0.036 * calc_time - 1.623 * Math.sin(S2) + 0.023 * Math.sin(2.0 * S2)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.52 - 0.004 * calc_time - 0.183 * Math.cos(S2) + 0.001 * Math.cos(2.0 * S2)) * Constant.DEG_TO_RAD);
			lon0 = 58.83 + 1.613 * Math.sin(S2) - 0.023 * Math.sin(2.0 * S2);
			rot_per_day = 518.2359876;
			break;
		case Telesto:
			mag = 8.9 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((50.51 - 0.036 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((84.06 - 0.004 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 56.88;
			rot_per_day = 190.6979332;
			break;
		case Calypso:
			mag = 9.1 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((36.41 - 0.036 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((85.04 - 0.004 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 153.51;
			rot_per_day = 190.6742373;
			break;
		case Helene:
			mag = 8.8 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.85 - 0.036 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.34 - 0.004 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 245.12;
			rot_per_day = 131.6174056;
			break;
		case Phoebe:
			mag = 6.9 + 5.0 * Math.log10(rr);
			northPoleRA = (float) (356.90 * Constant.DEG_TO_RAD);
			northPoleDEC = (float) (77.80 * Constant.DEG_TO_RAD);
			lon0 = 178.58;
			rot_per_day = 931.639;
			break;
		case Cordelia:
			mag = 11.4 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.31 - 0.15 * Math.sin(U1)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.18 + 0.14 * Math.cos(U1)) * Constant.DEG_TO_RAD);
			lon0 = 127.69 - 0.04 * Math.sin(U1);
			rot_per_day = -1074.520573;
			break;
		case Ophelia:
			mag = 11.1 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.31 - 0.09 * Math.sin(U2)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.18 + 0.09 * Math.cos(U2)) * Constant.DEG_TO_RAD);
			lon0 = 130.35 - 0.03 * Math.sin(U2);
			rot_per_day = -956.406815;
			break;
		case Bianca:
			mag = 10.3 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.31 - 0.16 * Math.sin(U3)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.18 + 0.16 * Math.cos(U3)) * Constant.DEG_TO_RAD);
			lon0 = 105.46 - 0.04 * Math.sin(U3);
			rot_per_day = -828.3914760;
			break;
 		case Cressida:
			mag = 9.4 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.31 - 0.04 * Math.sin(U4)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.18 + 0.04 * Math.cos(U4)) * Constant.DEG_TO_RAD);
			lon0 = 59.16 - 0.01 * Math.sin(U4);
			rot_per_day = -776.581632;
			break;
		case Desdemona:
			mag = 9.8 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.31 - 0.17 * Math.sin(U5)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.18 + 0.16 * Math.cos(U5)) * Constant.DEG_TO_RAD);
			lon0 = 95.08 - 0.04 * Math.sin(U5);
			rot_per_day = -760.0531690;
			break;
		case Juliet:
			mag = 8.9 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.31 - 0.06 * Math.sin(U6)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.18 + 0.06 * Math.cos(U6)) * Constant.DEG_TO_RAD);
			lon0 = 302.56 - 0.02 * Math.sin(U6);
			rot_per_day = -730.1253660;
			break;
		case Portia:
			mag = 8.2 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.31 - 0.09 * Math.sin(U7)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.18 + 0.09 * Math.cos(U7)) * Constant.DEG_TO_RAD);
			lon0 = 25.03 - 0.02 * Math.sin(U7);
			rot_per_day = -701.4865870;
			break;
		case Rosalind:
			mag = 9.6 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.31 - 0.29 * Math.sin(U8)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.18 + 0.28 * Math.cos(U8)) * Constant.DEG_TO_RAD);
			lon0 = 314.90 - 0.08 * Math.sin(U8);
			rot_per_day = -644.6311260;
			break;
		case Belinda:
			mag = 9.3 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.31 - 0.03 * Math.sin(U9)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.18 + 0.03 * Math.cos(U9)) * Constant.DEG_TO_RAD);
			lon0 = 297.46 - 0.01 * Math.sin(U9);
			rot_per_day = -577.3628170;
			break;
		case Puck:
			mag = 7.5 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.31 - 0.33 * Math.sin(U10)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.18 + 0.31 * Math.cos(U10)) * Constant.DEG_TO_RAD);
			lon0 = 91.24 - 0.09 * Math.sin(U10);
			rot_per_day = -472.5450690;
			break;
		case Naiad:
			mag = 9.86 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((299.36 + 0.70 * Math.sin(N0) - 6.49 * Math.sin(N1) + 0.25 * Math.sin(2.0 * N1)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((43.36 - 0.51 * Math.cos(N0) - 4.75 * Math.cos(N1) + 0.09 * Math.cos(2.0 * N1)) * Constant.DEG_TO_RAD);
			lon0 = 254.06 - 0.48 * Math.sin(N0) + 4.40 * Math.sin(N1) - 0.27 * Math.sin(2.0 * N1);
			rot_per_day = 1222.8441209;
			break;
		case Thalassa:
			mag = 9.16 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((299.36 + 0.70 * Math.sin(N0) - 0.28 * Math.sin(N2)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((43.45 - 0.51 * Math.cos(N0) - 0.21 * Math.cos(N2)) * Constant.DEG_TO_RAD);
			lon0 = 102.06 - 0.48 * Math.sin(N0) + 0.19 * Math.sin(N2);
			rot_per_day = 1155.7555612;
			break;
		case Despina:
			mag = 7.76 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((299.36 + 0.70 * Math.sin(N0) - 0.09 * Math.sin(N3)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((43.45 - 0.51 * Math.cos(N0) - 0.07 * Math.cos(N3)) * Constant.DEG_TO_RAD);
			lon0 = 306.51 - 0.49 * Math.sin(N0) + 0.06 * Math.sin(N3);
			rot_per_day = 1075.7341562;
			break;
		case Galatea:
			mag = 7.76 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((299.36 + 0.70 * Math.sin(N0) - 0.07 * Math.sin(N4)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((43.43 - 0.51 * Math.cos(N0) - 0.05 * Math.cos(N4)) * Constant.DEG_TO_RAD);
			lon0 = 258.09 - 0.48 * Math.sin(N0) + 0.05 * Math.sin(N4);
			rot_per_day = 839.6597686;
			break;
		case Larissa:
			mag = 7.26 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((299.36 + 0.70 * Math.sin(N0) - 0.27 * Math.sin(N5)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((43.41 - 0.51 * Math.cos(N0) - 0.20 * Math.cos(N5)) * Constant.DEG_TO_RAD);
			lon0 = 179.41 - 0.48 * Math.sin(N0) + 0.19 * Math.sin(N5);
			rot_per_day = 649.0534470;
			break;
		case Proteus:
			mag = 5.76 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((299.27 + 0.70 * Math.sin(N0) - 0.05 * Math.sin(N6)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((42.91 - 0.51 * Math.cos(N0) - 0.04 * Math.cos(N6)) * Constant.DEG_TO_RAD);
			lon0 = 93.38 - 0.48 * Math.sin(N0) + 0.04 * Math.sin(N6);
			rot_per_day = 320.7654228;
			break;
		default:
			return null;
		}
		return new double[] {mag, northPoleRA, northPoleDEC, lon0, rot_per_day};
	}

	/**
	 * Returns the magnitude and rotation model parameters of the body following IAU 2009
	 * recommendations.
	 * @param target Target body.
	 * @param JD Julian day in dynamical time.
	 * @param rr Distance parameter of the body: distance (to Earth) * distance from Sun.
	 * @return The magnitude and rotation model for IAU 2009: magnitude, north pole RA,
	 * north pole DEC (rad), longitude of planet at J2000 (deg), and speed rotation in degrees/day.
	 * Null is returned for a target without these data.
	 */
	public static double[] getIAU2009Model(TARGET target, double JD, double rr) {
		double mag = 0, northPoleRA = 0, northPoleDEC = 0, rot_per_day = 0, lon0 = 0;

		double calc_time = Functions.toCenturies(JD);

		double J1 = (073.32 + 91472.9 * calc_time) * Constant.DEG_TO_RAD;
		double J2 = (024.62 + 45137.2 * calc_time) * Constant.DEG_TO_RAD;
		double J3 = (283.90 + 4850.7 * calc_time) * Constant.DEG_TO_RAD;
		double J4 = (355.80 + 1191.3 * calc_time) * Constant.DEG_TO_RAD;
		double J5 = (119.90 + 262.1 * calc_time) * Constant.DEG_TO_RAD;
		double J6 = (229.80 + 64.3 * calc_time) * Constant.DEG_TO_RAD;
		double J7 = (352.25 + 2382.6 * calc_time) * Constant.DEG_TO_RAD;
		double J8 = (113.35 + 6070.0 * calc_time) * Constant.DEG_TO_RAD;
		double S1 = (353.32 + 75706.7 * calc_time) * Constant.DEG_TO_RAD;
		double S2 = (028.72 + 75706.7 * calc_time) * Constant.DEG_TO_RAD;
		double S3 = (177.40 - 36505.5 * calc_time) * Constant.DEG_TO_RAD;
		double S4 = (300.00 - 7225.9 * calc_time) * Constant.DEG_TO_RAD;
		double S5 = (316.45 + 506.2 * calc_time) * Constant.DEG_TO_RAD;
		double S6 = (345.20 - 1016.3 * calc_time) * Constant.DEG_TO_RAD;
		double U1 = (115.75 + 54991.87 * calc_time) * Constant.DEG_TO_RAD;
		double U2 = (141.69 + 41887.66 * calc_time) * Constant.DEG_TO_RAD;
		double U3 = (135.03 + 29927.35 * calc_time) * Constant.DEG_TO_RAD;
		double U4 = (061.77 + 25733.59 * calc_time) * Constant.DEG_TO_RAD;
		double U5 = (249.32 + 24471.46 * calc_time) * Constant.DEG_TO_RAD;
		double U6 = (043.86 + 22278.41 * calc_time) * Constant.DEG_TO_RAD;
		double U7 = (077.66 + 20289.42 * calc_time) * Constant.DEG_TO_RAD;
		double U8 = (157.36 + 16652.76 * calc_time) * Constant.DEG_TO_RAD;
		double U9 = (101.81 + 12872.63 * calc_time) * Constant.DEG_TO_RAD;
		double U10 = (138.64 + 8061.81 * calc_time) * Constant.DEG_TO_RAD;
		double U11 = (102.23 - 2024.22 * calc_time) * Constant.DEG_TO_RAD;
		double U12 = (316.41 + 2863.96 * calc_time) * Constant.DEG_TO_RAD;
		double U13 = (304.01 - 51.94 * calc_time) * Constant.DEG_TO_RAD;
		double U14 = (308.71 - 93.17 * calc_time) * Constant.DEG_TO_RAD;
		double U15 = (340.82 - 75.32 * calc_time) * Constant.DEG_TO_RAD;
		double U16 = (259.14 - 504.81 * calc_time) * Constant.DEG_TO_RAD;
		double N0 = (357.85 + 52.316 * calc_time) * Constant.DEG_TO_RAD;
		double N1 = (323.92 + 62606.6 * calc_time) * Constant.DEG_TO_RAD;
		double N2 = (220.51 + 55064.2 * calc_time) * Constant.DEG_TO_RAD;
		double N3 = (354.27 + 46564.5 * calc_time) * Constant.DEG_TO_RAD;
		double N4 = (075.31 + 26109.4 * calc_time) * Constant.DEG_TO_RAD;
		double N5 = (035.36 + 14325.4 * calc_time) * Constant.DEG_TO_RAD;
		double N6 = (142.61 + 2824.6 * calc_time) * Constant.DEG_TO_RAD;
		double N7 = (177.85 + 52.316 * calc_time) * Constant.DEG_TO_RAD;

		switch (target)
		{
		case Phobos:
			mag = 13.25 + 5.0 * Math.log10(rr);
			double M1 = (169.51 - 0.4357640 * (JD - Constant.J2000)) * Constant.DEG_TO_RAD;
			double M2 = (192.93 + 1128.4096700 * (JD - Constant.J2000) + 8.864 * calc_time * calc_time) * Constant.DEG_TO_RAD;
			northPoleRA = (float) ((317.68 - 0.108 * calc_time + 1.79 * Math.sin(M1)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((52.90 - 0.061 * calc_time - 1.08 * Math.cos(M1)) * Constant.DEG_TO_RAD);
			rot_per_day = 1128.8445850;
			lon0 = 35.06 + 8.864 * calc_time * calc_time - 1.42 * Math.sin(M1) - 0.78 * Math.sin(M2);
			break;
		case Deimos:
			mag = 13.88 + 5.0 * Math.log10(rr);
			double M3 = (53.57 - 0.0181510 * (JD - Constant.J2000)) * Constant.DEG_TO_RAD;
			northPoleRA = (float) ((316.65 - 0.108 * calc_time + 2.98 * Math.sin(M3)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((53.52 - 0.061 * calc_time - 1.78 * Math.cos(M3)) * Constant.DEG_TO_RAD);
			rot_per_day = 285.1618970;
			lon0 = 79.41 - 0.520 * calc_time * calc_time - 2.58 * Math.sin(M3) + 0.19 * Math.cos(M3);
			break;
		case Io:
			mag = -1.64 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((268.05 - 0.009 * calc_time + 0.094 * Math.sin(J3) + 0.024 * Math.sin(J4)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((64.50 + 0.003 * calc_time + 0.040 * Math.cos(J3) + 0.011 * Math.cos(J4)) * Constant.DEG_TO_RAD);
			lon0 = 200.39 - 0.085 * Math.sin(J3) - 0.022 * Math.sin(J4);
			rot_per_day = 203.4889538;
			break;
		case Europa:
			mag = -1.37 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((268.08 - 0.009 * calc_time + 1.086 * Math.sin(J4) + 0.060 * Math.sin(J5) + 0.015 * Math.sin(J6) + 0.009 * Math.sin(J7)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((64.51 + 0.003 * calc_time + 0.468 * Math.cos(J4) + 0.026 * Math.cos(J5) + 0.007 * Math.cos(J6) + 0.002 * Math.cos(J7)) * Constant.DEG_TO_RAD);
			lon0 = 36.022 - 0.980 * Math.sin(J4) - 0.054 * Math.sin(J5) - 0.014 * Math.sin(J6) - 0.008 * Math.sin(J7);
			rot_per_day = 101.3747235;
			break;
		case Ganymede:
			mag = -2.04 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((268.20 - 0.009 * calc_time - 0.037 * Math.sin(J4) + 0.431 * Math.sin(J5) + 0.091 * Math.sin(J6)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((64.57 + 0.003 * calc_time - 0.016 * Math.cos(J4) + 0.186 * Math.cos(J5) + 0.039 * Math.cos(J6)) * Constant.DEG_TO_RAD);
			lon0 = 44.064 + 0.033 * Math.sin(J4) - 0.389 * Math.sin(J5) - 0.082 * Math.sin(J6);
			rot_per_day = 50.3176081;
			break;
		case Callisto:
			mag = -1.0 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((268.72 - 0.009 * calc_time - 0.068 * Math.sin(J5) + 0.590 * Math.sin(J6) + 0.010 * Math.sin(J8)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((64.83 + 0.003 * calc_time - 0.029 * Math.cos(J5) + 0.254 * Math.cos(J6) - 0.004 * Math.cos(J8)) * Constant.DEG_TO_RAD);
			lon0 = 259.51 + 0.061 * Math.sin(J5) - 0.533 * Math.sin(J6) - 0.009 * Math.sin(J8);
			rot_per_day = 21.5710715;
			break;
		case Mimas:
			mag = 3.3 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.66 - 0.036 * calc_time + 13.56 * Math.sin(S3)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.52 - 0.004 * calc_time - 1.53 * Math.cos(S3)) * Constant.DEG_TO_RAD);
			lon0 = 333.46 - 13.48 * Math.sin(S3) - 44.85 * Math.sin(S5);
			rot_per_day = 381.9945550;
			break;
		case Enceladus:
			mag = 2.1 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.66 - 0.036 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.52 - 0.004 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 6.32;
			rot_per_day = 262.7318996;
			break;
		case Tethys:
			mag = 0.6 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.66 - 0.036 * calc_time + 9.66 * Math.sin(S4)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.52 - 0.004 * calc_time - 1.09 * Math.cos(S4)) * Constant.DEG_TO_RAD);
			lon0 = 8.95 - 9.60 * Math.sin(S4) + 2.23 * Math.sin(S5);
			rot_per_day = 190.6979085;
			break;
		case Dione:
			mag = 0.88 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.66 - 0.036 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.52 - 0.004 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 357.60;
			rot_per_day = 131.5349316;
			break;
		case Rhea:
			mag = 0.16 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.38 - 0.036 * calc_time + 3.10 * Math.sin(S6)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.55 - 0.004 * calc_time - 0.35 * Math.cos(S6)) * Constant.DEG_TO_RAD);
			lon0 = 235.16 - 3.08 * Math.sin(S6);
			rot_per_day = 79.6900478;
			break;
		case Titan:
			mag = -1.29 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((39.4827) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.4279) * Constant.DEG_TO_RAD);
			lon0 = 186.5855;
			rot_per_day = 22.5769768;
			break;
		case Hyperion: // No data available (chaotic)
			mag = 4.63 + 5.0 * Math.log10(rr);
			northPoleRA = 0.0f;
			northPoleDEC = 0.0f;
			lon0 = 0.0;
			rot_per_day = 0.0;
			break;
		case Iapetus:
			mag = 1.5 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((318.16 - 3.949 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((75.03 - 1.143 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 355.20;
			rot_per_day = 4.5379572;
			break;
		case Miranda:
			mag = 3.6 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.43 + 4.41 * Math.sin(U11) - 0.04 * Math.sin(2.0 * U11)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.08 + 4.25 * Math.cos(U11) - 0.02 * Math.cos(2.0 * U11)) * Constant.DEG_TO_RAD);
			lon0 = 30.70 - 1.27 * Math.sin(U12) + 0.15 * Math.sin(2.0 * U12) + 1.15 * Math.sin(U11) - 0.09 * Math.sin(2.0 * U11);
			rot_per_day = -254.6906892;
			break;
		case Ariel:
			mag = 1.45 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.43 + 0.29 * Math.sin(U13)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.10 + 0.28 * Math.cos(U13)) * Constant.DEG_TO_RAD);
			lon0 = 156.22 + 0.05 * Math.sin(U12) + 0.08 * Math.sin(U13);
			rot_per_day = -142.8356681;
			break;
		case Umbriel:
			mag = 2.1 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.43 + 0.21 * Math.sin(U14)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.10 + 0.20 * Math.cos(U14)) * Constant.DEG_TO_RAD);
			lon0 = 108.05 - 0.09 * Math.sin(U12) + 0.06 * Math.sin(U14);
			rot_per_day = -86.8688923;
			break;
		case Titania:
			mag = 1.02 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.43 + 0.29 * Math.sin(U15)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.10 + 0.28 * Math.cos(U15)) * Constant.DEG_TO_RAD);
			lon0 = 77.74 + 0.08 * Math.sin(U15);
			rot_per_day = -41.3514316;
			break;
		case Oberon:
			mag = 1.23 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.43 + 0.16 * Math.sin(U16)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.10 + 0.16 * Math.cos(U16)) * Constant.DEG_TO_RAD);
			lon0 = 6.77 + 0.04 * Math.sin(U16);
			rot_per_day = -26.7394932;
			break;
		case Triton:
			mag = -1.24 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((299.36 - 32.35 * Math.sin(N7) - 6.28 * Math.sin(2.0 * N7) - 2.08 * Math
					.sin(3.0 * N7) - 0.74 * Math.sin(4.0 * N7) - 0.28 * Math.sin(5.0 * N7) - 0.11 * Math.sin(6.0 * N7) - 0.07 * Math
					.sin(7.0 * N7) - 0.02 * Math.sin(8.0 * N7) - 0.01 * Math.sin(9.0 * N7)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((41.17 + 22.55 * Math.cos(N7) + 2.10 * Math.cos(2.0 * N7) + 0.55 * Math
					.cos(3.0 * N7) + 0.16 * Math.cos(4.0 * N7) + 0.05 * Math.cos(5.0 * N7) + 0.02 * Math.cos(6.0 * N7) + 0.01 * Math
					.cos(7.0 * N7)) * Constant.DEG_TO_RAD);
			lon0 = 296.53 + 22.25 * Math.sin(N7) + 6.73 * Math.sin(2.0 * N7) + 2.05 * Math.sin(3.0 * N7) + 0.74 * Math
					.sin(4.0 * N7) + 0.28 * Math.sin(5.0 * N7) + 0.11 * Math.sin(6.0 * N7) + 0.05 * Math.sin(7.0 * N7) + 0.02 * Math
					.sin(8.0 * N7) + 0.01 * Math.sin(9.0 * N7);
			rot_per_day = -61.2572637;
			break;
		case Charon:
			mag = -0.99 + 5.0 * Math.log10(rr);
			northPoleRA = (float) (132.993 * Constant.DEG_TO_RAD);
			northPoleDEC = (float) (-6.163 * Constant.DEG_TO_RAD);
			lon0 = 122.695;  // See the 'erratum' by Archinal et al. 2011 (Celestial Mechanics & Dynamical Astronomy 110-4, 401-403)
			rot_per_day = 56.3625225;
			break;
		case Metis:
			mag = 10.9 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((268.05 - 0.009 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((64.49 + 0.003 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 346.09;
			rot_per_day = 1221.2547301;
			break;
		case Adrastea:
			mag = 12.5 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((268.05 - 0.009 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((64.49 + 0.003 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 33.29;
			rot_per_day = 1206.9986602;
			break;
		case Amalthea:
			mag = 7.5 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((268.05 - 0.009 * calc_time - 0.84 * Math.sin(J1) + 0.01 * Math.sin(2.0 * J1)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((64.49 + 0.003 * calc_time - 0.36 * Math.cos(J1)) * Constant.DEG_TO_RAD);
			lon0 = 231.67 + 0.76 * Math.sin(J1) - 0.01 * Math.sin(2.0 * J1);
			rot_per_day = 722.6314560;
			break;
		case Thebe:
			mag = 9.1 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((268.05 - 0.009 * calc_time - 2.11 * Math.sin(J2) + 0.04 * Math.sin(2.0 * J2)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((64.49 + 0.003 * calc_time - 0.91 * Math.cos(J2) + 0.01 * Math.cos(2.0 * J2)) * Constant.DEG_TO_RAD);
			lon0 = 8.56 + 1.91 * Math.sin(J2) - 0.04 * Math.sin(2.0 * J2);
			rot_per_day = 533.7004100;
			break;
		case Pan:
			mag = 9.4 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.6 - 0.036 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.5 - 0.004 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 48.8;
			rot_per_day = 626.0440000;
			break;
		case Atlas:
			mag = 8.4 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.58 - 0.036 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.53 - 0.004 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 137.88;
			rot_per_day = 598.3060000;
			break;
		case Prometheus:
			mag = 6.2 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.58 - 0.036 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.53 - 0.004 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 296.14;
			rot_per_day = 587.2890000;
			break;
		case Pandora:
			mag = 6.9 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.58 - 0.036 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.53 - 0.004 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 162.92;
			rot_per_day = 572.7891000;
			break;
		case Epimetheus:
			mag = 6.1 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.58 - 0.036 * calc_time - 3.153 * Math.sin(S1) + 0.086 * Math.sin(2.0 * S1)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.52 - 0.004 * calc_time - 0.356 * Math.cos(S1) + 0.005 * Math.cos(2.0 * S1)) * Constant.DEG_TO_RAD);
			lon0 = 293.87 + 3.133 * Math.sin(S1) - 0.086 * Math.sin(2.0 * S1);
			rot_per_day = 518.4907239;
			break;
		case Janus:
			mag = 4.9 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.58 - 0.036 * calc_time - 1.623 * Math.sin(S2) + 0.023 * Math.sin(2.0 * S2)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.52 - 0.004 * calc_time - 0.183 * Math.cos(S2) + 0.001 * Math.cos(2.0 * S2)) * Constant.DEG_TO_RAD);
			lon0 = 58.83 + 1.613 * Math.sin(S2) - 0.023 * Math.sin(2.0 * S2);
			rot_per_day = 518.2359876;
			break;
		case Telesto:
			mag = 8.9 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((50.51 - 0.036 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((84.06 - 0.004 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 56.88;
			rot_per_day = 190.6979332;
			break;
		case Calypso:
			mag = 9.1 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((36.41 - 0.036 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((85.04 - 0.004 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 153.51;
			rot_per_day = 190.6742373;
			break;
		case Helene:
			mag = 8.8 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((40.85 - 0.036 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((83.34 - 0.004 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 245.12;
			rot_per_day = 131.6174056;
			break;
		case Phoebe:
			mag = 6.9 + 5.0 * Math.log10(rr);
			northPoleRA = (float) (356.90 * Constant.DEG_TO_RAD);
			northPoleDEC = (float) (77.80 * Constant.DEG_TO_RAD);
			lon0 = 178.58;
			rot_per_day = 931.639;
			break;
		// Absolute magnitudes relative to Miranda derived from data at http://www.dtm.ciw.edu/users/sheppard/satellites/urasatdata.html
		case Cordelia:
			mag = 11.4 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.31 - 0.15 * Math.sin(U1)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.18 + 0.14 * Math.cos(U1)) * Constant.DEG_TO_RAD);
			lon0 = 127.69 - 0.04 * Math.sin(U1);
			rot_per_day = -1074.520573;
			break;
		case Ophelia:
			mag = 11.1 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.31 - 0.09 * Math.sin(U2)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.18 + 0.09 * Math.cos(U2)) * Constant.DEG_TO_RAD);
			lon0 = 130.35 - 0.03 * Math.sin(U2);
			rot_per_day = -956.406815;
			break;
		case Bianca:
			mag = 10.3 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.31 - 0.16 * Math.sin(U3)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.18 + 0.16 * Math.cos(U3)) * Constant.DEG_TO_RAD);
			lon0 = 105.46 - 0.04 * Math.sin(U3);
			rot_per_day = -828.3914760;
			break;
 		case Cressida:
			mag = 9.4 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.31 - 0.04 * Math.sin(U4)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.18 + 0.04 * Math.cos(U4)) * Constant.DEG_TO_RAD);
			lon0 = 59.16 - 0.01 * Math.sin(U4);
			rot_per_day = -776.581632;
			break;
		case Desdemona:
			mag = 9.8 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.31 - 0.17 * Math.sin(U5)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.18 + 0.16 * Math.cos(U5)) * Constant.DEG_TO_RAD);
			lon0 = 95.08 - 0.04 * Math.sin(U5);
			rot_per_day = -760.0531690;
			break;
		case Juliet:
			mag = 8.9 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.31 - 0.06 * Math.sin(U6)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.18 + 0.06 * Math.cos(U6)) * Constant.DEG_TO_RAD);
			lon0 = 302.56 - 0.02 * Math.sin(U6);
			rot_per_day = -730.1253660;
			break;
		case Portia:
			mag = 8.2 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.31 - 0.09 * Math.sin(U7)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.18 + 0.09 * Math.cos(U7)) * Constant.DEG_TO_RAD);
			lon0 = 25.03 - 0.02 * Math.sin(U7);
			rot_per_day = -701.4865870;
			break;
		case Rosalind:
			mag = 9.6 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.31 - 0.29 * Math.sin(U8)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.18 + 0.28 * Math.cos(U8)) * Constant.DEG_TO_RAD);
			lon0 = 314.90 - 0.08 * Math.sin(U8);
			rot_per_day = -644.6311260;
			break;
		case Belinda:
			mag = 9.3 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.31 - 0.03 * Math.sin(U9)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.18 + 0.03 * Math.cos(U9)) * Constant.DEG_TO_RAD);
			lon0 = 297.46 - 0.01 * Math.sin(U9);
			rot_per_day = -577.3628170;
			break;
		case Puck:
			mag = 7.5 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((257.31 - 0.33 * Math.sin(U10)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((-15.18 + 0.31 * Math.cos(U10)) * Constant.DEG_TO_RAD);
			lon0 = 91.24 - 0.09 * Math.sin(U10);
			rot_per_day = -472.5450690;
			break;
		// Absolute magnitudes relative to Triton derived from data at http://www.dtm.ciw.edu/users/sheppard/satellites/nepsatdata.html
		case Naiad:
			mag = 9.86 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((299.36 + 0.70 * Math.sin(N0) - 6.49 * Math.sin(N1) + 0.25 * Math.sin(2.0 * N1)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((43.36 - 0.51 * Math.cos(N0) - 4.75 * Math.cos(N1) + 0.09 * Math.cos(2.0 * N1)) * Constant.DEG_TO_RAD);
			lon0 = 254.06 - 0.48 * Math.sin(N0) + 4.40 * Math.sin(N1) - 0.27 * Math.sin(2.0 * N1);
			rot_per_day = 1222.8441209;
			break;
		case Thalassa:
			mag = 9.16 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((299.36 + 0.70 * Math.sin(N0) - 0.28 * Math.sin(N2)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((43.45 - 0.51 * Math.cos(N0) - 0.21 * Math.cos(N2)) * Constant.DEG_TO_RAD);
			lon0 = 102.06 - 0.48 * Math.sin(N0) + 0.19 * Math.sin(N2);
			rot_per_day = 1155.7555612;
			break;
		case Despina:
			mag = 7.76 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((299.36 + 0.70 * Math.sin(N0) - 0.09 * Math.sin(N3)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((43.45 - 0.51 * Math.cos(N0) - 0.07 * Math.cos(N3)) * Constant.DEG_TO_RAD);
			lon0 = 306.51 - 0.49 * Math.sin(N0) + 0.06 * Math.sin(N3);
			rot_per_day = 1075.7341562;
			break;
		case Galatea:
			mag = 7.76 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((299.36 + 0.70 * Math.sin(N0) - 0.07 * Math.sin(N4)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((43.43 - 0.51 * Math.cos(N0) - 0.05 * Math.cos(N4)) * Constant.DEG_TO_RAD);
			lon0 = 258.09 - 0.48 * Math.sin(N0) + 0.05 * Math.sin(N4);
			rot_per_day = 839.6597686;
			break;
		case Larissa:
			mag = 7.26 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((299.36 + 0.70 * Math.sin(N0) - 0.27 * Math.sin(N5)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((43.41 - 0.51 * Math.cos(N0) - 0.20 * Math.cos(N5)) * Constant.DEG_TO_RAD);
			lon0 = 179.41 - 0.48 * Math.sin(N0) + 0.19 * Math.sin(N5);
			rot_per_day = 649.0534470;
			break;
		case Proteus:
			mag = 5.76 + 5.0 * Math.log10(rr);
			northPoleRA = (float) ((299.27 + 0.70 * Math.sin(N0) - 0.05 * Math.sin(N6)) * Constant.DEG_TO_RAD);
			northPoleDEC = (float) ((42.91 - 0.51 * Math.cos(N0) - 0.04 * Math.cos(N6)) * Constant.DEG_TO_RAD);
			lon0 = 93.38 - 0.48 * Math.sin(N0) + 0.04 * Math.sin(N6);
			rot_per_day = 320.7654228;
			break;
		default:
			return null;
		}
		return new double[] {mag, northPoleRA, northPoleDEC, lon0, rot_per_day};
	}
	
	/**
	 * Returns the magnitude and rotation model parameters of the body following IAU 2015
	 * recommendations.
	 * @param target Target body.
	 * @param JD Julian day in dynamical time.
	 * @param rr Distance parameter of the body: distance (to Earth) * distance from Sun.
	 * @return The magnitude and rotation model for IAU 2015: magnitude, north pole RA,
	 * north pole DEC (rad), longitude of planet at J2000 (deg), and speed rotation in degrees/day.
	 * Null is returned for a target without these data.
	 */
	public static double[] getIAU2015Model(TARGET target, double JD, double rr) {
		double mag = 0, northPoleRA = 0, northPoleDEC = 0, rot_per_day = 0, lon0 = 0;

		double T = Functions.toCenturies(JD);

		switch (target)
		{
		case Phobos: // See the erratum by Archinal 2019
			mag = 13.25 + 5.0 * Math.log10(rr);
			double M1 = (190.72646643 + 15917.10818695 * T) * Constant.DEG_TO_RAD;
			double M2 = (21.46892470 + 31834.27934054 * T) * Constant.DEG_TO_RAD;
			double M3 = (332.86082793 + 19139.89694742 * T) * Constant.DEG_TO_RAD;
			double M4 = (394.93256437 + 38280.79631835 * T) * Constant.DEG_TO_RAD;
			double M5 = (189.63271560 + 41215158.18420050 * T + 12.71192322 * T * T) * Constant.DEG_TO_RAD;

			northPoleRA = 317.67071657 - 0.10844326 * T
					- 1.78428399 * Math.sin(M1) + 0.02212824 * Math.sin(M2)
					- 0.01028251 * Math.sin(M3) - 0.00475595 * Math.sin(M4);
			northPoleDEC = 52.88627266 - 0.06134706 * T
					- 1.07516537 * Math.cos(M1) + 0.00668626 * Math.cos(M2)
					- 0.00648740 * Math.cos(M3) + 0.00281576 * Math.cos(M4);
			northPoleRA *= Constant.DEG_TO_RAD;
			northPoleDEC *= Constant.DEG_TO_RAD;
			lon0 = 35.18774440 + 12.72192797 * T * T
					+ 1.42421769 * Math.sin(M1) - 0.02273783 * Math.sin(M2)
					+ 0.00410711 * Math.sin(M3) + 0.00631964 * Math.sin(M4)
					- 1.143 * Math.sin(M5);
			rot_per_day = 1128.84475928;
			break;
		case Deimos:
			mag = 13.88 + 5.0 * Math.log10(rr);
			double M6 = (121.46893664 + 660.22803474 * T) * Constant.DEG_TO_RAD;
			double M7 = (231.05028581 + 660.99123540 * T) * Constant.DEG_TO_RAD;
			double M8 = (251.37314025 + 1320.50145245 * T) * Constant.DEG_TO_RAD;
			double M9 = (217.98635955 + 38279.96125550 * T) * Constant.DEG_TO_RAD;
			double M10 = (196.19729402 + 19139.83628608 * T) * Constant.DEG_TO_RAD;
			
			northPoleRA = 316.65705808 - 0.10518014 * T + 3.09217726 * Math.sin(M6) + 
					0.22980637 * Math.sin(M7) + 0.06418655 * Math.sin(M8) + 
					0.02533537 * Math.sin(M9) + 0.00778695 * Math.sin(M10);
			northPoleDEC = 53.50992033 - 0.05979094 * T + 1.83936004 * Math.cos(M6) + 
					0.14325320 * Math.cos(M7) + 0.01911409 * Math.cos(M8) - 
					0.01482590 * Math.cos(M9) + 0.00192430 * Math.cos(M10);
			northPoleRA *= Constant.DEG_TO_RAD;
			northPoleDEC *= Constant.DEG_TO_RAD;
			lon0 = 79.39932954 - 2.73954829 * Math.sin(M6) - 
					0.39968606 * Math.sin(M7) - 0.06563259 * Math.sin(M8) - 
					0.02912940 * Math.sin(M9) + 0.01699160 * Math.sin(M10);
			rot_per_day = 285.16188899;
			break;
		default:
			return getIAU2009Model(target, JD, rr);
		}
		return new double[] {mag, northPoleRA, northPoleDEC, lon0, rot_per_day};
	}

	/**
	 * Returns the mean rotation rate of a given body using the corresponding IAU model.
	 * @param eph Ephemeris object containing target body and ephemeris method.
	 * @return The rotation rate in radians/second.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double getBodyMeanRotationRate(EphemerisElement eph) throws JPARSECException {
		double JD = 2451545, rr = 1, rotationModel[] = null;
		switch (eph.ephemMethod) {
		case IAU_2006:
			rotationModel = getIAU2006Model(eph.targetBody, JD, rr);
			break;
		case IAU_2009:
			rotationModel = getIAU2009Model(eph.targetBody, JD, rr);
			break;
		case IAU_2015:
			rotationModel = getIAU2015Model(eph.targetBody, JD, rr);
			break;
		default:
			rotationModel = getIAU2000Model(eph.targetBody, JD, rr);
			break;
		}
		return rotationModel[4] * Math.PI / (24.0 * 3600.0 * 180.0);
	}

	/**
	 * Returns the mean rotation rate of a given body using the corresponding IAU model.
	 * @param JD_TDB Julian day in TDB.
	 * @param eph Ephemeris object containing target body and ephemeris method.
	 * @return Apparent sidereal time in radians for an observer at longitude 0.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double getBodySiderealTimeAt0Lon(double JD_TDB, EphemerisElement eph) throws JPARSECException {
		double JD = JD_TDB, rr = 1, rotationModel[] = null;
		switch (eph.ephemMethod) {
		case IAU_2006:
			rotationModel = getIAU2006Model(eph.targetBody, JD, rr);
			break;
		case IAU_2009:
			rotationModel = getIAU2009Model(eph.targetBody, JD, rr);
			break;
		case IAU_2015:
			rotationModel = getIAU2015Model(eph.targetBody, JD, rr);
			break;
		default:
			rotationModel = getIAU2000Model(eph.targetBody, JD, rr);
			break;
		}
		double lon = Functions.normalizeDegrees(90.0 + rotationModel[3] + rotationModel[4] * (JD - 2451545.0)) * Constant.DEG_TO_RAD;
		lon -= Ephem.getZeroRightAscension(new TimeElement(JD_TDB, SCALE.BARYCENTRIC_DYNAMICAL_TIME), 
				ObserverElement.parseExtraterrestrialObserver(new ExtraterrestrialObserverElement("", eph.targetBody, new LocationElement(0, 0, 0))), eph, 0.5);
		return lon;
	}

	/**
	 * Returns the orientation of the north pole of rotation of a given body using the corresponding IAU model.
	 * @param JD_TDB Julian day in TDB.
	 * @param eph Ephemeris object containing target body, ephemeris method, and output equinox.
	 * @return North pole direction.
	 * @throws JPARSECException If an error occurs.
	 */
	public static LocationElement getBodyNorthPole(double JD_TDB, EphemerisElement eph) throws JPARSECException {
		double JD = JD_TDB, rr = 1, rotationModel[] = null;
		switch (eph.ephemMethod) {
		case IAU_2006:
			rotationModel = getIAU2006Model(eph.targetBody, JD, rr);
			break;
		case IAU_2009:
			rotationModel = getIAU2009Model(eph.targetBody, JD, rr);
			break;
		case IAU_2015:
			rotationModel = getIAU2015Model(eph.targetBody, JD, rr);
			break;
		default:
			rotationModel = getIAU2000Model(eph.targetBody, JD, rr);
			break;
		}
		if (rotationModel == null) return null;
		LocationElement loc = new LocationElement(rotationModel[1], rotationModel[2], 1.0);
		if (eph.equinox != Constant.J2000) loc = LocationElement.parseRectangularCoordinates(Precession.precess(Constant.J2000, eph.getEpoch(JD), loc.getRectangularCoordinates(), eph));
		return loc;
	}

	/**
	 * Returns the absolute magnitude of a given natural satellite.
	 * @param eph Ephemeris object containing target body.
	 * @return Absolute magnitude, or {@linkplain EphemElement#INVALID_MAGNITUDE}
	 * in case it cannot be computed for the given body.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double getBodyAbsoluteMagnitude(EphemerisElement eph) throws JPARSECException {
		double JD = Constant.J2000, rr = 1, rotationModel[] = null;
		switch (eph.ephemMethod) {
		case IAU_2006:
			rotationModel = getIAU2006Model(eph.targetBody, JD, rr);
			break;
		case IAU_2009:
			rotationModel = getIAU2009Model(eph.targetBody, JD, rr);
			break;
		case IAU_2015:
			rotationModel = getIAU2015Model(eph.targetBody, JD, rr);
			break;
		default:
			rotationModel = getIAU2000Model(eph.targetBody, JD, rr);
			break;
		}
		if (rotationModel == null) return EphemElement.INVALID_MAGNITUDE;
		return rotationModel[0];
	}
}
