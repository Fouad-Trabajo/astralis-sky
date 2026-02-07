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

import jparsec.astronomy.Constellation;
import jparsec.astronomy.Star;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.event.LunarEvent;
import jparsec.ephem.moons.MoonPhysicalParameters;
import jparsec.ephem.planets.EphemElement;
import jparsec.math.Constant;
import jparsec.observer.ExtraterrestrialObserverElement;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.TimeElement;
import jparsec.time.TimeScale;
import jparsec.time.TimeElement.SCALE;
import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Logger.LEVEL;

/**
 * A class to obtain the orientation and physical ephemeris of a planet, based
 * on IAU 2000/2006/2009/2015 recomendations. The different implementations are used
 * for the different values of the ephem method to apply in the ephemeris object.
 * The default method (for values different from IAU 2006/2009/2015) is the
 * IAU 2000 recommendations for the orientations of the planetary axes.
 *
 * @see Ephem
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class PhysicalParameters
{
	// private constructor so that this class cannot be instantiated.
	private PhysicalParameters() {}

	/**
	 * Obtain physical parameters of the planet: elongation, phase, phase angle,
	 * angular radius, visual magnitude (AA supplement), and axis orientation.
	 * This method is applicable for the Sun, the Moon, any planet, Pluto, and
	 * the asteroids Ida, Vesta, Gaspra, and Eros. Previous calculation of basic
	 * ephemeris is required.<P>
	 * The axis orientation for the Moon is computed using Eckhardt's theory,
	 * instead of the IAU rotation model.
	 *
	 * @param JD Julian day in dynamical time.
	 * @param ephem_sun Ephem object with ephemeris of sun (should be apparent, equinox of date).
	 * @param ephem_obj Ephem object to take and store data (should be apparent, equinox of date).
	 * @param obs The Observer object.
	 * @param eph Ephemeris object defining the ephemeris properties.
	 * @return EphemElement with the completed data.
	 * @throws JPARSECException If the object data cannot be found.
	 */
	public static EphemElement physicalParameters(double JD, // Julian day
			EphemElement ephem_sun, // Ephem Element
			EphemElement ephem_obj, // Ephem Element
			ObserverElement obs,
			EphemerisElement eph) // Ephemeris Element
			throws JPARSECException
	{

		EphemElement ephem = ephem_obj.clone();

		// Put distances and angles in a more comfortable way
		double RE = ephem_sun.distance;
		double RP = ephem.distanceFromSun;
		double RO = ephem.distance;
		double LP = ephem.heliocentricEclipticLongitude;
		double eq_sun[] = LocationElement.parseLocationElement(new LocationElement(ephem_sun.rightAscension,
				ephem_sun.declination, ephem_sun.distance));
		double LE = Math.PI + LocationElement.parseRectangularCoordinates(
				Ephem.equatorialToEcliptic(eq_sun, JD, eph)).getLongitude();

		// Angular radius in radians
		ephem.angularRadius = (float) Math.atan(eph.targetBody.equatorialRadius / (ephem.distance * Constant.AU));

		ephem.phase = 1;
		if (eph.targetBody != TARGET.SUN)
		{
			// Elongation
			//double DELO = (RE * RE + RO * RO - RP * RP) / (2.0 * RE * RO);
			//ephem.elongation = (float) Math.acos(DELO);
			// Returns true apparent elongation, not geometric (aberration ... everything corrected).
			ephem.elongation = (float) LocationElement.getAngularDistance(ephem.getEquatorialLocation(), ephem_sun.getEquatorialLocation());
			RP = Math.sqrt(-(Math.cos(ephem.elongation) * 2.0 * RE * RO - RE * RE - RO * RO));

			// Phase and phase angle. Note phase angle can be
			// negative to represent the case LE < LP (before opposition)
			double DPH = ((RP * RP + RO * RO - RE * RE) / (2.0 * RP * RO));
			double DPHA = RE * Math.sin(LE - LP) / RO; // Sin(phase angle)
			ephem.phaseAngle = (float) Math.acos(DPH);
			if (DPHA < 0) ephem.phaseAngle = -ephem.phaseAngle;
			ephem.phase = (float) ((1.0 + DPH) * 0.5);

			// Defect of illumination
			ephem.defectOfIllumination = (float) (1.0 - ephem.phase) * ephem.angularRadius;
		}

		// Constellation where the object is in
		ephem.constellation = "";
		try {
			// I use here JD instead of eph.getEpoch(JD) since this method is called before passing
			// to output equinox. Input ephemerides are always apparent and respect equinox of date.
			String cons = Constellation.getConstellationName(ephem.rightAscension, ephem.declination, JD,
				eph);
			ephem.constellation = cons;
		} catch (Exception exc) {	}

		// Continue only for supported objects
		if (!eph.targetBody.isPlanet() && eph.targetBody != TARGET.Pluto && eph.targetBody != TARGET.SUN && eph.targetBody != TARGET.Moon
				&& !eph.targetBody.isAsteroid())
			return ephem;

		// Now compute disk orientation as seen by the observer
		ephem.positionAngleOfAxis = 0.0f;
		ephem.positionAngleOfPole = 0.0f;
		ephem.brightLimbAngle = 0.0f;
		ephem.subsolarLatitude = 0.0f;
		ephem.subsolarLongitude = 0.0f;
		ephem.longitudeOfCentralMeridian = 0.0f;
		ephem.magnitude = 0.0f;
		ephem.surfaceBrightness = 0.0f;

		if (eph.targetBody == TARGET.NOT_A_PLANET) return ephem;

		// Visual magnitude and axis orientation
		double rr = ephem.distance * ephem.distanceFromSun;
		if (eph.targetBody == TARGET.SUN) rr = ephem.distance;
		double PH = Math.abs(ephem.phaseAngle) * Constant.RAD_TO_DEG;
		double mag = 0.0;
		double lon0 = 0.0; // Initial longitude at JD = 2451545.0
		double rot_per_day = 0.0; // Degrees per day of rotation of planet

		double rotationModel[] = null;
		switch (eph.ephemMethod) {
		case IAU_2006:
			rotationModel = getIAU2006Model(eph.targetBody, JD - ephem.distance * Constant.LIGHT_TIME_DAYS_PER_AU, rr, PH);
			break;
		case IAU_2009:
			rotationModel = getIAU2009Model(eph.targetBody, JD - ephem.distance * Constant.LIGHT_TIME_DAYS_PER_AU, rr, PH);
			break;
		case IAU_2015:
			rotationModel = getIAU2015Model(eph.targetBody, JD - ephem.distance * Constant.LIGHT_TIME_DAYS_PER_AU, rr, PH);
			break;
		default:
			rotationModel = getIAU2000Model(eph.targetBody, JD - ephem.distance * Constant.LIGHT_TIME_DAYS_PER_AU, rr, PH);
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

		ephem.magnitude = (float) mag;
		
		if (eph.preferPrecisionInEphemerides && eph.targetBody.isPlanet() && eph.targetBody != TARGET.SUN) { 
			// Re-compute more accurate planetary magnitudes (Mallama & Hilton 2018)
			TimeElement time = new TimeElement(JD, SCALE.TERRESTRIAL_TIME);
			ephem.magnitude = (float) RevisedPlanetaryMagnitudes2018.getRevisedPlanetMagnitude(time, obs, eph, ephem, false);
		}
		
		// Compute surface brightness
		if (ephem.magnitude !=  EphemElement.INVALID_MAGNITUDE &&
				(ephem.angularRadius * Constant.RAD_TO_ARCSEC > 0.5) && ephem.phase >= 0)
			ephem.surfaceBrightness = (float) Star.getSurfaceBrightness(mag, 
					Math.sqrt(ephem.angularRadius * ephem.angularRadius * ephem.phase) * Constant.RAD_TO_ARCSEC);

		// Correct visual magnitude of Saturn because of the rings brightness
		// http://ntrs.nasa.gov/archive/nasa/casi.ntrs.nasa.gov/19710009758.pdf
		if (eph.targetBody == TARGET.SATURN && ephem.magnitude != EphemElement.INVALID_MAGNITUDE)
		{
			if (eph.preferPrecisionInEphemerides) { 
				// Re-compute more accurate planetary magnitudes (Mallama & Hilton 2018)
				TimeElement time = new TimeElement(JD, SCALE.TERRESTRIAL_TIME);
				ephem.magnitude = (float) RevisedPlanetaryMagnitudes2018.getRevisedPlanetMagnitude(time, obs, eph, ephem, true);
			} else {
				double sinB = Math.abs(Math.sin(ephem.positionAngleOfPole));
				double ring_magn = -2.6 * sinB + 1.25 * sinB * sinB;
				ephem.magnitude += ring_magn;
			}
		}
		
		if (eph.targetBody == TARGET.Moon && obs.getMotherBody() == TARGET.EARTH) {
			// Substitute librations by the results of Eckhardt's theory. IAU rotation model is extremely inaccurate (in
			// longitude of central meridian) far from year 2000.
			try {
				double v[] = LunarEvent.getEckhardtMoonLibrations(JD, eph.ephemMethod, ephem_obj.getEquatorialLocation());
				ephem.longitudeOfCentralMeridian = v[0];
				ephem.positionAngleOfPole = v[1];
				ephem.positionAngleOfAxis = v[2];
			} catch (Exception e) { }
		}

		return ephem;
	}

	/**
	 * Returns the direction of the north pole of rotation of a planet for a given date.
	 * @param JD The Julian day.
	 * @param eph Ephemeris properties with target body, ephemeris reduction method, and
	 * output equinox.
	 * @return The direction of the north pole of rotation, or null if it is not available.
	 * @throws JPARSECException If an error occurs.
	 */
	public static LocationElement getPlanetaryAxisDirection(double JD, EphemerisElement eph) throws JPARSECException {
		if (eph.targetBody.isNaturalSatellite()) return MoonPhysicalParameters.getBodyNorthPole(JD, eph);

		double rr = 1.0, PH = 0;
		double rotationModel[] = null;

		if (eph.targetBody != null && eph.targetBody != TARGET.NOT_A_PLANET) {
			switch (eph.ephemMethod) {
			case IAU_2006:
				rotationModel = getIAU2006Model(eph.targetBody, JD, rr, PH);
				break;
			case IAU_2009:
				rotationModel = getIAU2009Model(eph.targetBody, JD, rr, PH);
				break;
			case IAU_2015:
				rotationModel = getIAU2015Model(eph.targetBody, JD, rr, PH);
				break;
			default:
				rotationModel = getIAU2000Model(eph.targetBody, JD, rr, PH);
				break;
			}
		}

		if (rotationModel == null) return null;
		LocationElement loc = new LocationElement(rotationModel[1], rotationModel[2], 1.0);
		if (eph.equinox != Constant.J2000) loc = LocationElement.parseRectangularCoordinates(Precession.precess(Constant.J2000, eph.getEpoch(JD), loc.getRectangularCoordinates(), eph));
		return loc;
	}

	/**
	 * Calculate moon axis orientation as established by the IAU. These formulae
	 * is an approximation of the real movement of the lunar axis. Previous
	 * calculation of lunar ephemeris is required. Better results for the longitude
	 * of the central meridian (libration in longitude) can be obtained with
	 * {@linkplain LunarEvent#getEckhardtMoonLibrations(double, int, EphemElement)}.
	 *
	 * @param JD Julian day in TT.
	 * @return orientation data.
	 */
	private static double[] moonAxis(double JD)
	{
		double d, d2, E1, E2, E3, E4, E5, E6, E7, E8, E9, E10, E11, E12, E13;
		double ra, dec, lon;

		d = JD - Constant.J2000;
		d2 = d * d;
		double tmp = Functions.toCenturies(JD);

		E1 = (125.045 - 0.0529921 * d) * Constant.DEG_TO_RAD;
		E2 = (250.089 - 0.1059842 * d) * Constant.DEG_TO_RAD;
		E3 = (260.008 + 13.0120009 * d) * Constant.DEG_TO_RAD;
		E4 = (176.625 + 13.3407154 * d) * Constant.DEG_TO_RAD;
		E5 = (357.529 + 0.9856003 * d) * Constant.DEG_TO_RAD;
		E6 = (311.589 + 26.4057084 * d) * Constant.DEG_TO_RAD;
		E7 = (134.963 + 13.0649930 * d) * Constant.DEG_TO_RAD;
		E8 = (276.617 + 0.3287146 * d) * Constant.DEG_TO_RAD;
		E9 = (34.226 + 1.7484877 * d) * Constant.DEG_TO_RAD;
		E10 = (15.134 - 0.1589763 * d) * Constant.DEG_TO_RAD;
		E11 = (119.743 + 0.0036096 * d) * Constant.DEG_TO_RAD;
		E12 = (239.961 + 0.1643573 * d) * Constant.DEG_TO_RAD;
		E13 = (25.053 + 12.9590088 * d) * Constant.DEG_TO_RAD;

		ra = 269.9949 + 0.0031 * tmp - 3.8787 * Math.sin(E1) - 0.1204 * Math.sin(E2) + 0.0700 * Math.sin(E3) - 0.0172 * Math.sin(E4);
		ra = ra + 0.0072 * Math.sin(E6) - 0.0052 * Math.sin(E10) + 0.0043 * Math.sin(E13);

		dec = 66.5392 + 0.0130 * tmp + 1.5419 * Math.cos(E1) + 0.0239 * Math.cos(E2) - 0.0278 * Math.cos(E3) + 0.0068 * Math.cos(E4);
		dec = dec - 0.0029 * Math.cos(E6) + 0.0009 * Math.cos(E7) + 0.0008 * Math.cos(E10) - 0.0009 * Math.cos(E13);

		lon = 38.3213 - 1.4E-12 * d2 + 3.5610 * Math.sin(E1) + 0.1208 * Math.sin(E2) - 0.0642 * Math.sin(E3);
		lon = lon + 0.0158 * Math.sin(E4) + 0.0252 * Math.sin(E5) - 0.0066 * Math.sin(E6) - 0.0047 * Math.sin(E7) - 0.0044 * Math.sin(E13);
		lon = lon - 0.0046 * Math.sin(E8) + 0.0028 * Math.sin(E9) + 0.0052 * Math.sin(E10) + 0.0040 * Math.sin(E11) + 0.0019 * Math.sin(E12);

		// Set parameters according to IAU
		double northPoleRA = (ra * Constant.DEG_TO_RAD);
		double northPoleDEC = (dec * Constant.DEG_TO_RAD);
		double rot_per_day = 13.17635815;
		double lon0 = lon;
		return new double[] {northPoleRA, northPoleDEC, lon0, rot_per_day};
	}

	/**
	 * Calculate orientation of a planet providing the position and the
	 * direction of the north pole of rotation. Previous calculation of basic
	 * ephemeris is required, as well as knowledge of axis orientation. The
	 * IAU 2000/2006/2009 models are used, see <I> REPORT OF THE IAU/IAG WORKING GROUP ON
	 * CARTOGRAPHIC COORDINATES AND ROTATIONAL ELEMENTS: 2009</I>, B. A. Archinal
	 * et al, in Celestial Mechanics and Dynamical Astronomy, 2011.
	 * <P>
	 * Note 1: The program use the dynamical north pole of rotation. In giant
	 * planets, an offset between dynamical and magnetic north pole will exist.
	 * So, for these planets, longitude of central meridian in System III and
	 * subsolar longitude values should be taken with caution.
	 * <P>
	 * Note 2: No correction is performed for object radius. In giant planets
	 * like Jupiter (or Saturn), with radius of 70 000 km = 0.25 light-seconds, a
	 * discrepancy up to +0.003 degrees can be expected with other sources. Since
	 * this is a difference in light time that affects the central meridian
	 * longitude on the disk in certain instant, this correction depends on the
	 * position in the disk surface.
	 *
	 * @param JD Julian day in TDB.
	 * @param ephem_sun EphemElement with at least RA, DEC, and distance for the Sun.
	 * @param ephem_obj EphemElement with at least RA, DEC, distance, and north pole
	 * direction.
	 * @param lon0 Initial longitude of planet at JD = 2451545.0 in degrees.
	 * @param rot_per_day Rotation speed in degrees/day.
	 * @param eph Ephemeris object defining the ephemeris properties.
	 * @param motherBody The mother body for the calculations. Can be set to null
	 * for the Earth.
	 * @return Ephem object with all the fields (PA pole, PA axis, bright limb angle, central
	 * meridian, and subsolar longitude and latitude).
	 * @throws JPARSECException If an error occurs.
	 */
	public static EphemElement calcAxis(double JD, EphemElement ephem_sun, EphemElement ephem_obj, double lon0,
			double rot_per_day, EphemerisElement eph, TARGET motherBody) throws JPARSECException
	{
		EphemElement ephem = ephem_obj.clone();
		EphemElement ephemSunCopy = ephem_sun.clone();

		double N, D, delta_lon = 0.0;

		// Obtain direction of the north pole of rotation referred to the equinox
		// of date
		ephem = Precession.precessPoleFromJ2000(JD, ephem, eph);

		// Correct also for Nutation ? Disabled. Effect is only 0.5" in Jupiter.
		// Note aberration is not needed in this kind of calculations
		//LocationElement loc = LocationElement.parseRectangularCoordinates(Nutation.nutateInEquatorialCoordinates(JD, eph, (new LocationElement(ephem.northPoleRA, ephem.northPoleDEC, 1.0)).getRectangularCoordinates(), true));
		//ephem.northPoleRA = loc.getLongitude();
		//ephem.northPoleDEC = loc.getLatitude();

		LocationElement locEq = ephem.getEquatorialLocation();
		LocationElement locNP = new LocationElement(ephem.northPoleRA, ephem.northPoleDEC, 1.0);
		LocationElement locEqSun = ephemSunCopy.getEquatorialLocation();
		if (motherBody != null && motherBody != TARGET.EARTH) {
			ObserverElement obs = ObserverElement.parseExtraterrestrialObserver(new ExtraterrestrialObserverElement("", motherBody));
			TimeElement time = new TimeElement(JD, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
			LocationElement np = Ephem.getPositionFromBody(locNP, time, obs, eph);
			ephem.northPoleRA = np.getLongitude();
			ephem.northPoleDEC = np.getLatitude();
			
			locEqSun = Ephem.getPositionFromEarth(locEqSun, time, obs, eph);
			locEq = Ephem.getPositionFromEarth(locEq, time, obs, eph);
			ephemSunCopy.setEquatorialLocation(locEqSun);
			ephem.setEquatorialLocation(locEq);
		}

		/* Correct Julian day and obtain it in TCB (Barycentric Coordinate
		 * Time), referred to the barycenter of the solar system. Note that
		 * following G. H. Kaplan et al. 2006 (astro-ph/0602086) = USNO
		 * Circular 179, this correction is not needed, since the time
		 * argument in Seidelmann et al. 2002 is TDB, not TCB. So there was
		 * an error in Seidelmann et al. 2002.
		 */
		// JD = JD + TimeScale.getTCBminusTDB(JD) / Constant.SECONDS_PER_DAY;

		// Obtain position angle of pole as seen from Earth
		ephem.positionAngleOfPole = dotProduct(ephem.northPoleRA, ephem.northPoleDEC, ephem.rightAscension,
				ephem.declination);

		// Correct value (planetocentric to planeto-geodetic latitude)
		ephem.positionAngleOfPole = Target.planetocentricToPlanetogeodeticLatitude(ephem.positionAngleOfPole, eph.targetBody);

		double factor = 1.0;
		if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.GEOMETRIC) factor = 0.0;
		double lightTime = Constant.LIGHT_TIME_DAYS_PER_AU * ephem.distance;
		// Correct light time for object radius ?
//		try {
//			lightTime -= TARGET.getEquatorialRadius(eph.targetBody) * Constant.LIGHT_TIME_DAYS_PER_AU / Constant.AU;
//		} catch (Exception e) { }

		// To obtain the illumination angle of equator as seen from Earth, first
		// we need the position of the planet as seen from the sun
		if (eph.targetBody != TARGET.SUN)
		{
			double fromSun[] = Ephem.getGeocentricPosition(new LocationElement(ephem.rightAscension,
					ephem.declination, ephem.distance), new LocationElement(ephemSunCopy.rightAscension,
					ephemSunCopy.declination, ephemSunCopy.distance));

			LocationElement ephem_loc = LocationElement.parseRectangularCoordinates(fromSun);
			double ra = ephem_loc.getLongitude();
			double dec = ephem_loc.getLatitude();

			// Obtain subsolar latitude and longitude
			ephem.subsolarLatitude = dotProduct(ephem.northPoleRA, ephem.northPoleDEC, ra, dec);

			// Correct value (planetocentric to planeto-geodetic latitude)
			ephem.subsolarLatitude = Target.planetocentricToPlanetogeodeticLatitude(ephem.subsolarLatitude, eph.targetBody);

			// Get subsolar position
			//D = Math.cos(dec) * Math.sin(ephem.northPoleRA - ra);
			//N = Math.sin(ephem.northPoleDEC) * Math.cos(dec) * Math.cos(ephem.northPoleRA - ra);
			//N = N - Math.cos(ephem.northPoleDEC) * Math.sin(dec);

			fromSun = Ephem.getGeocentricPosition(locEq, locEqSun);
			ephem_loc = LocationElement.parseRectangularCoordinates(fromSun);
			ra = ephem_loc.getLongitude();
			dec = ephem_loc.getLatitude();
			D = Math.cos(dec) * Math.sin(locNP.getLongitude() - ra);
			N = Math.sin(locNP.getLatitude()) * Math.cos(dec) * Math.cos(locNP.getLongitude() - ra);
			N = N - Math.cos(locNP.getLatitude()) * Math.sin(dec);
			if (D != 0.0) delta_lon = Math.atan(N / D) * Constant.RAD_TO_DEG;
			if (D < 0.0) delta_lon = delta_lon + 180.0;
			ephem.subsolarLongitude = (rot_per_day * ((JD - Constant.J2000) - factor * lightTime) - delta_lon + lon0);
			if (rot_per_day < 0.0) ephem.subsolarLongitude = 360.0f - ephem.subsolarLongitude;
			ephem.subsolarLongitude = Functions.normalizeRadians(Math.toRadians(ephem.subsolarLongitude));

			// Compute bright limb angle
			ephem.brightLimbAngle = (float) (Math.PI + Math.atan2(Math.cos(ephemSunCopy.declination) * Math
					.sin(ephem.rightAscension - ephemSunCopy.rightAscension), Math.cos(ephemSunCopy.declination) * Math
					.sin(ephem.declination) * Math.cos(ephem.rightAscension - ephemSunCopy.rightAscension) - Math
					.sin(ephemSunCopy.declination) * Math.cos(ephem.declination)));
		}

		// Compute position angle of axis
		ephem.positionAngleOfAxis = (Math.PI + Math.atan2(Math.cos(ephem.northPoleDEC) * Math
				.sin(ephem.rightAscension - ephem.northPoleRA), Math.cos(ephem.northPoleDEC) * Math
				.sin(ephem.declination) * Math.cos(ephem.rightAscension - ephem.northPoleRA) - Math
				.sin(ephem.northPoleDEC) * Math.cos(ephem.declination)));

		// Compute longitude of central meridian
		// This should be calculated with coordinates from Earth (ecliptic)
/*		D = Math.cos(ephem.declination) * Math.sin(ephem.northPoleRA - ephem.rightAscension);
		N = Math.sin(ephem.northPoleDEC) * Math.cos(ephem.declination) * Math
				.cos(ephem.northPoleRA - ephem.rightAscension);
		N = N - Math.cos(ephem.northPoleDEC) * Math.sin(ephem.declination);
*/
		D = Math.cos(locEq.getLatitude()) * Math.sin(locNP.getLongitude() - locEq.getLongitude());
		N = Math.sin(locNP.getLatitude()) * Math.cos(locEq.getLatitude()) * Math
				.cos(locNP.getLongitude() - locEq.getLongitude());
		N = N - Math.cos(locNP.getLatitude()) * Math.sin(locEq.getLatitude());

		delta_lon = Math.atan2(N, D) * Constant.RAD_TO_DEG;

		double meridian0 = (rot_per_day * ((JD - Constant.J2000) - factor * lightTime) - delta_lon);
		double meridian = meridian0 + lon0;
		if (rot_per_day < 0.0) meridian = 360.0 - meridian;
		meridian = Functions.normalizeDegrees(meridian);
		ephem.longitudeOfCentralMeridian = Math.toRadians(meridian);

		// Establish adequate values for Jupiter, Saturn, and other bodies
		switch (eph.targetBody)
		{
		case JUPITER:
			double tmp1 = -(-67.1 + delta_lon - (meridian0 + delta_lon) * 877.9 / rot_per_day); // Equatorial belt
			double tmp2 = -(-43.3 + delta_lon - (meridian0 + delta_lon) * 870.27 / rot_per_day); // Tropical belt
			tmp1 = Functions.normalizeDegrees(tmp1);
			tmp2 = Functions.normalizeDegrees(tmp2);
			ephem.longitudeOfCentralMeridianSystemI = Math.toRadians(tmp1);
			ephem.longitudeOfCentralMeridianSystemII = Math.toRadians(tmp2);
			ephem.longitudeOfCentralMeridianSystemIII = ephem.longitudeOfCentralMeridian;
			break;
		case SATURN:
			double tmp = -(-227.2037 + delta_lon - (meridian0 + delta_lon) * 844.3 / rot_per_day); // Equatorial belt
			tmp = Functions.normalizeDegrees(tmp);
			ephem.longitudeOfCentralMeridianSystemI = Math.toRadians(tmp);
			ephem.longitudeOfCentralMeridianSystemIII = ephem.longitudeOfCentralMeridian;
			break;
		case SUN: // This inversion is due to historical reasons
			ephem.longitudeOfCentralMeridian = Functions
					.normalizeRadians(Constant.TWO_PI - ephem.longitudeOfCentralMeridian);
			break;
		case Moon: // This inversion is due to historical reasons
		case EARTH: // This inversion is due to historical reasons
			ephem.longitudeOfCentralMeridian = Functions
					.normalizeRadians(Constant.TWO_PI - ephem.longitudeOfCentralMeridian);
			ephem.subsolarLongitude = -ephem.subsolarLongitude;
			break;
		case URANUS: // Set value also for System III
			ephem.longitudeOfCentralMeridianSystemIII = ephem.longitudeOfCentralMeridian;
			break;
		case NEPTUNE: // Set value also for System III
			ephem.longitudeOfCentralMeridianSystemIII = ephem.longitudeOfCentralMeridian;
			break;
		default:
			break;
		}

		return ephem;
	}

	/**
	 * Performs adequate dot product for axis orientation calculations. The
	 * result is the planetocentric latitude of the object supposed that the
	 * object's axis is pointing to pole_ra, pole_dec, and the object is
	 * observed is a position p_ra, p_dec. The value should later be corrected
	 * to planetogeodetic by applying the formula: geo_lat =
	 * atan(tan(planeto_lat) / (1.0 - shape)^2), where shape = (equatorial -
	 * polar radius) / (equatorial radius).
	 *
	 * @param pole_ra Right ascension of the north pole.
	 * @param pole_dec Declination of the north pole.
	 * @param p_ra Right ascension of some planet as seen by the observer.
	 * @param p_dec Declination of some planet as seen by the observer.
	 * @return Result of the dot product as a double precission value.
	 */
	static double dotProduct(double pole_ra, double pole_dec, double p_ra, double p_dec)
	{
		double incc = 0.0, DOT;

		pole_dec = Constant.PI_OVER_TWO - pole_dec;
		p_dec = Constant.PI_OVER_TWO - p_dec;

		DOT = Math.sin(pole_dec) * Math.cos(pole_ra) * Math.sin(p_dec) * Math.cos(p_ra);
		DOT = DOT + Math.sin(pole_dec) * Math.sin(pole_ra) * Math.sin(p_dec) * Math.sin(p_ra);
		DOT = DOT + Math.cos(pole_dec) * Math.cos(p_dec);

		incc = -Math.asin(DOT);

		return incc;
	}

	/**
	 * Returns the magnitude and rotation model parameters of the body following IAU 2015
	 * recommendations.
	 * @param target Target body.
	 * @param JD Julian day in dynamical time.
	 * @param rr Distance parameter of the body: distance (to Earth) * distance from Sun.
	 * For the Sun this value is its distance.
	 * @param PH Phase angle in degrees, as a positive value. Both this value and the previous
	 * one affects only the returned magnitude (first value).
	 * @return The magnitude and rotation model for IAU 2015: magnitude, north pole RA,
	 * north pole DEC (rad), longitude of planet at J2000 (deg), and speed rotation in degrees/day.
	 * Null is returned for a target without these data.
	 */
	public static double[] getIAU2015Model(TARGET target, double JD, double rr, double PH) {
		double mag = 0, northPoleRA = 0, northPoleDEC = 0, rot_per_day = 0, lon0 = 0;

		double T = Functions.toCenturies(JD);

		// Magnitudes for planets from http://stjarnhimlen.se/comp/ppcomp.html, no reference there.
		switch (target)
		{
		case SUN:
		case VENUS:
		case EARTH:
		case JUPITER:
		case SATURN:
		case URANUS:
		case Moon: // Previous ME system still recommended but not explicitly mentioned. Accurate to 150m
		case Pluto:
		case Pallas:
		case Lutetia:
		case Eros:
		case Davida:
		case Gaspra:
		case Itokawa:
			return getIAU2009Model(target, JD, rr, PH);
		case MERCURY:
			mag = -0.36 + 5.0 * Math.log10(rr) + 0.038 * PH - 0.000273 * PH * PH + 2.0E-6 * Math.pow(PH, 3.0);
			northPoleRA = ((281.0103 - 0.0328 * T) * Constant.DEG_TO_RAD);
			northPoleDEC = ((61.4155 - 0.0049 * T) * Constant.DEG_TO_RAD);
			double M1 = (174.7910857 +  4.092335 * (JD - Constant.J2000)) * Constant.DEG_TO_RAD;
			double M2 = (349.5821714 +  8.184670 * (JD - Constant.J2000)) * Constant.DEG_TO_RAD;
			double M3 = (164.3732571 + 12.277005 * (JD - Constant.J2000)) * Constant.DEG_TO_RAD;
			double M4 = (339.1643429 + 16.369340 * (JD - Constant.J2000)) * Constant.DEG_TO_RAD;
			double M5 = (153.9554286 + 20.461675 * (JD - Constant.J2000)) * Constant.DEG_TO_RAD;
			lon0 = 329.5988 + 0.01067257 * Math.sin(M1) - 0.00112309 * Math.sin(M2) - 0.00011040 * Math.sin(M3)
				- 0.00002539 * Math.sin(M4) - 0.00000571 * Math.sin(M5);
			rot_per_day = 6.1385108;
			break;
		case MARS:
			mag = -1.52 + 5.0 * Math.log10(rr) + 0.016 * PH;
			double dr = Constant.DEG_TO_RAD;
			northPoleRA = 317.269202 - 0.10927547 * T + 0.000068 * Math.sin((198.991226 + 19139.4819985 * T) * dr) + 
					0.000238 * Math.sin((226.292679 + 38280.8511281 * T) * dr) + 0.000052 * Math.sin((249.663391 + 57420.7251593 * T) * dr) + 
					0.000009 * Math.sin((266.183510 + 76560.6367950 * T) * dr) + 0.419057 * Math.sin((79.398797 + 0.5042615 * T) * dr);
			northPoleDEC = 54.432516 - 0.05827105 * T + 0.000051 * Math.cos((122.433576 + 19139.9407476 * T) * dr) + 
					0.000141 * Math.cos((43.058401 + 38280.8753272 * T) * dr) + 0.000031 * Math.cos((57.663379 + 57420.7517205 * T) * dr) + 
					0.000005 * Math.cos((79.476401 + 76560.6495004 * T) * dr) + 1.591274 * Math.cos((166.325722 + 0.5042615 * T) * dr);
			lon0 = 176.049863 + 0.000145 * Math.sin((129.071773 + 19140.0328244 * T) * dr) + 
					0.000157 * Math.sin((36.352167 + 38281.0473591 * T) * dr) + 0.000040 * Math.sin((56.668646 + 57420.9295360 * T) * dr) + 
					0.000001 * Math.sin((67.364003 + 76560.2552215 * T) * dr) + 0.000001 * Math.sin((104.792680 + 95700.4387578 * T) * dr) + 
					0.584542 * Math.sin((95.391654 + 0.5042615 * T) * dr);
			northPoleRA *= Constant.DEG_TO_RAD;
			northPoleDEC *= Constant.DEG_TO_RAD;
			rot_per_day = 350.891982443297;					
			break;
		case NEPTUNE:
			mag = -6.87 + 5.0 * Math.log10(rr);
			double tmp = (357.85 + 52.316 * T) * Constant.DEG_TO_RAD;
			northPoleRA = ((299.36 + 0.70 * Math.sin(tmp)) * Constant.DEG_TO_RAD);
			northPoleDEC = ((43.46 - 0.51 * Math.cos(tmp)) * Constant.DEG_TO_RAD);
			lon0 = 249.978 - 0.48 * Math.sin(tmp);
			rot_per_day = 541.1397757;
			break;
		case Ceres:
			mag = 0.0 + 5.0 * Math.log10(rr);
			northPoleRA = (291.418 * Constant.DEG_TO_RAD);
			northPoleDEC = (66.764 * Constant.DEG_TO_RAD);
			lon0 = 170.650;
			rot_per_day = 952.1532;
			break;
		case Vesta:
			mag = 0.0 + 5.0 * Math.log10(rr);
			northPoleRA = (309.031 * Constant.DEG_TO_RAD);
			northPoleDEC = (42.235 * Constant.DEG_TO_RAD);
			lon0 = 285.39;
			rot_per_day = 1617.3329428;
			break;
		case Ida:
			mag = 0.0 + 5.0 * Math.log10(rr);
			northPoleRA = (168.76 * Constant.DEG_TO_RAD);
			northPoleDEC = (-87.12 * Constant.DEG_TO_RAD);
			lon0 = 274.05; 
			rot_per_day = 1864.6280070;
			break;
		case Steins:
			mag = 0.0 + 5.0 * Math.log10(rr);
			northPoleRA = (91 * Constant.DEG_TO_RAD);
			northPoleDEC = (-62 * Constant.DEG_TO_RAD);
			lon0 = 321.76;
			rot_per_day = 1428.09917;
			break;
		case P9_Tempel_1: // Unknown values for lon0 and rot_per_day
			mag = 0.0 + 5.0 * Math.log10(rr);
			northPoleRA = (255.0 * Constant.DEG_TO_RAD);
			northPoleDEC = (64.5 * Constant.DEG_TO_RAD);
			lon0 = 252.63;
			rot_per_day = 212.064;
			break;
		case P19_Borrelly: // Unknown values for lon0 and rot_per_day
			mag = 0.0 + 5.0 * Math.log10(rr);
			northPoleRA = (218.5 * Constant.DEG_TO_RAD);
			northPoleDEC = (-12.5 * Constant.DEG_TO_RAD);
			lon0 = 0;
			rot_per_day = 324.3;
			break;
		default:
			return null;
		}
		return new double[] {mag, northPoleRA, northPoleDEC, lon0, rot_per_day};
	}
	
	/**
	 * Returns the magnitude and rotation model parameters of the body following IAU 2009
	 * resolutions.
	 * @param target Target body.
	 * @param JD Julian day in dynamical time.
	 * @param rr Distance parameter of the body: distance (to Earth) * distance from Sun.
	 * For the Sun this value is its distance.
	 * @param PH Phase angle in degrees, as a positive value. Both this value and the previous
	 * one affects only the returned magnitude (first value).
	 * @return The magnitude and rotation model for IAU 2009: magnitude, north pole RA,
	 * north pole DEC (rad), longitude of planet at J2000 (deg), and speed rotation in degrees/day.
	 * Null is returned for a target without these data.
	 */
	public static double[] getIAU2009Model(TARGET target, double JD, double rr, double PH) {
		double mag = 0, northPoleRA = 0, northPoleDEC = 0, rot_per_day = 0, lon0 = 0;

		double calc_time = Functions.toCenturies(JD);

		// Magnitudes for planets from http://stjarnhimlen.se/comp/ppcomp.html, no reference there.
		switch (target)
		{
		case SUN:
			mag = -26.742 + 5.0 * Math.log10(rr);
			northPoleRA = (286.13 * Constant.DEG_TO_RAD);
			northPoleDEC = (63.87 * Constant.DEG_TO_RAD);
			rot_per_day = 14.1844;
			lon0 = 84.176;
			break;
		case MERCURY:
			mag = -0.36 + 5.0 * Math.log10(rr) + 0.038 * PH - 0.000273 * PH * PH + 2.0E-6 * Math.pow(PH, 3.0);
			northPoleRA = ((281.0097 - 0.0328 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = ((61.4143 - 0.0049 * calc_time) * Constant.DEG_TO_RAD);
			double M1 = (174.791086 +  4.092335 * (JD - Constant.J2000)) * Constant.DEG_TO_RAD;
			double M2 = (349.582171 +  8.184670 * (JD - Constant.J2000)) * Constant.DEG_TO_RAD;
			double M3 = (164.373257 + 12.277005 * (JD - Constant.J2000)) * Constant.DEG_TO_RAD;
			double M4 = (339.164343 + 16.369340 * (JD - Constant.J2000)) * Constant.DEG_TO_RAD;
			double M5 = (153.955429 + 20.461675 * (JD - Constant.J2000)) * Constant.DEG_TO_RAD;
			lon0 = 329.5469 + 0.00993822 * Math.sin(M1) - 0.00104581 * Math.sin(M2) - 0.0001028 * Math.sin(M3)
				- 0.00002364 * Math.sin(M4) - 0.00000532 * Math.sin(M5);
			rot_per_day = 6.1385025;
			break;
		case VENUS:
			mag = -4.29 + 5.0 * Math.log10(rr) + 0.0009 * PH + 0.000239 * PH * PH - 6.5E-7 * Math.pow(PH, 3.0);
			northPoleRA = (272.76 * Constant.DEG_TO_RAD);
			northPoleDEC = (67.16 * Constant.DEG_TO_RAD);
			lon0 = 160.20;
			rot_per_day = -1.4813688;
			break;
		case EARTH: // Here the observer is supposed to be outside the Earth
			mag = -3.86 + 5.0 * Math.log10(rr);
			northPoleRA = ((0.0 - 0.641 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = ((90.0 - 0.557 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 190.147;
			rot_per_day = 360.9856235;
			break;
		case MARS:
			mag = -1.52 + 5.0 * Math.log10(rr) + 0.016 * PH;
			northPoleRA =  ((317.68143 - 0.1061 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = ((52.8865 - 0.0609 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 176.630;
			rot_per_day = 350.89198226;
			break;
		case JUPITER:
			mag = -9.25 + 5.0 * Math.log10(rr) + 0.005 * PH;
			northPoleRA = ((268.056595 - 0.006499 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = ((64.495303 + 0.002413 * calc_time) * Constant.DEG_TO_RAD);
			double Ja = (99.360714 + 4850.4046 * calc_time) * Constant.DEG_TO_RAD;
			double Jb = (175.895369 + 1191.9605 * calc_time) * Constant.DEG_TO_RAD;
			double Jc = (300.323162 + 262.5475 * calc_time) * Constant.DEG_TO_RAD;
			double Jd = (114.012305 + 6070.2476 * calc_time) * Constant.DEG_TO_RAD;
			double Je = (49.511251 + 64.3 * calc_time) * Constant.DEG_TO_RAD;
			northPoleRA += (0.000117 * Math.sin(Ja) + 0.000938 * Math.sin(Jb) +
					0.001432 * Math.sin(Jc) + 0.00003 * Math.sin(Jd) + 0.00215 * Math.sin(Je)) * Constant.DEG_TO_RAD;
			northPoleDEC += (0.00005 * Math.cos(Ja) + 0.000404 * Math.cos(Jb) +
					0.000617 * Math.cos(Jc) - 0.000013 * Math.cos(Jd) + 0.000926 * Math.cos(Je)) * Constant.DEG_TO_RAD;
			lon0 = 284.95; // for System III
			rot_per_day = 870.536;
			break;
		case SATURN:
			mag = -8.88 + 5.0 * Math.log10(rr) + 0.044 * PH;
			northPoleRA = ((40.589 - 0.036 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = ((83.537 - 0.004 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 38.9; // for System III
			rot_per_day = 810.7939024;
			break;
		case URANUS:
			mag = -7.19 + 5.0 * Math.log10(rr);
			northPoleRA = (257.311 * Constant.DEG_TO_RAD);
			northPoleDEC = (-15.175 * Constant.DEG_TO_RAD);
			lon0 = 203.81;
			rot_per_day = -501.1600928;
			break;
		case NEPTUNE:
			mag = -6.87 + 5.0 * Math.log10(rr);
			double tmp = (357.85 + 52.316 * calc_time) * Constant.DEG_TO_RAD;
			northPoleRA = ((299.36 + 0.70 * Math.sin(tmp)) * Constant.DEG_TO_RAD);
			northPoleDEC = ((43.46 - 0.51 * Math.cos(tmp)) * Constant.DEG_TO_RAD);
			lon0 = 253.18 - 0.48 * Math.sin(tmp);
			rot_per_day = 536.3128492;
			break;
		case Pluto:
			mag = -1.01 + 5.0 * Math.log10(rr);
			northPoleRA = (132.993 * Constant.DEG_TO_RAD);
			northPoleDEC = (-6.163 * Constant.DEG_TO_RAD);
			lon0 = 302.695; // See the 'erratum' by Archinal et al. 2011 (Celestial Mechanics & Dynamical Astronomy 110-4, 401-403)
			rot_per_day = 56.3625225;
			break;
		case Moon:
			mag = 0.23 + 5.0 * Math.log10(rr) + 0.026 * PH + 4.0E-9 * Math.pow(PH, 4.0);
			double data[] = moonAxis(JD);
			northPoleRA = data[0];
			northPoleDEC = data[1];
			lon0 = data[2];
			rot_per_day = data[3];
			break;
		case Ceres:
			mag = 0.0 + 5.0 * Math.log10(rr);
			northPoleRA = (291.0 * Constant.DEG_TO_RAD);
			northPoleDEC = (59.0 * Constant.DEG_TO_RAD);
			lon0 = 170.9;
			rot_per_day = 952.1532;
			break;
		case Pallas:
			mag = 0.0 + 5.0 * Math.log10(rr);
			northPoleRA = (33.0 * Constant.DEG_TO_RAD);
			northPoleDEC = (-3.0 * Constant.DEG_TO_RAD);
			lon0 = 38.0;
			rot_per_day = 1105.8036;
			break;
		case Vesta:
			mag = 0.0 + 5.0 * Math.log10(rr);
			northPoleRA = (305.8 * Constant.DEG_TO_RAD);
			northPoleDEC = (41.4 * Constant.DEG_TO_RAD);
			lon0 = 292.0;
			rot_per_day = 1617.332776;
			break;
		case Lutetia:
			mag = 0.0 + 5.0 * Math.log10(rr);
			northPoleRA = (52.0 * Constant.DEG_TO_RAD);
			northPoleDEC = (12.0 * Constant.DEG_TO_RAD);
			lon0 = 94.0;
			rot_per_day = 1057.7515;
			break;
		case Ida:
			mag = 0.0 + 5.0 * Math.log10(rr);
			northPoleRA = (168.76 * Constant.DEG_TO_RAD);
			northPoleDEC = (-2.88 * Constant.DEG_TO_RAD);
			lon0 = 274.05;  // See the 'erratum' by Archinal et al. 2011 (Celestial Mechanics & Dynamical Astronomy 110-4, 401-403)
			rot_per_day = 1864.6280070;
			break;
		case Eros:
			mag = 0.0 + 5.0 * Math.log10(rr);
			northPoleRA = (11.35 * Constant.DEG_TO_RAD);
			northPoleDEC = (17.22 * Constant.DEG_TO_RAD);
			lon0 = 326.07;
			rot_per_day = 1639.38864745;
			break;
		case Davida:
			mag = 0.0 + 5.0 * Math.log10(rr);
			northPoleRA = (297.0 * Constant.DEG_TO_RAD);
			northPoleDEC = (5.0 * Constant.DEG_TO_RAD);
			lon0 = 268.1;
			rot_per_day = 1684.4193549;
			break;
		case Gaspra:
			mag = 0.0 + 5.0 * Math.log10(rr);
			northPoleRA = (9.47 * Constant.DEG_TO_RAD);
			northPoleDEC = (26.70 * Constant.DEG_TO_RAD);
			lon0 = 83.67;
			rot_per_day = 1226.9114850;
			break;
		case Steins: // 2867
			mag = 0.0 + 5.0 * Math.log10(rr);
			northPoleRA = (90 * Constant.DEG_TO_RAD);
			northPoleDEC = (-62 * Constant.DEG_TO_RAD);
			lon0 = 93.94;
			rot_per_day = 1428.852332;
			break;
		case Itokawa: // 25143
			mag = 0.0 + 5.0 * Math.log10(rr);
			northPoleRA = (90.53 * Constant.DEG_TO_RAD);
			northPoleDEC = (-66.3 * Constant.DEG_TO_RAD);
			lon0 = 0;
			rot_per_day = 712.143;
			break;
		case P9_Tempel_1:
			mag = 0.0 + 5.0 * Math.log10(rr);
			northPoleRA = (294.0 * Constant.DEG_TO_RAD);
			northPoleDEC = (73 * Constant.DEG_TO_RAD);
			lon0 = 252.63;
			rot_per_day = 212.064;
			break;
		case P19_Borrelly:
			mag = 0.0 + 5.0 * Math.log10(rr);
			northPoleRA = (218.5 * Constant.DEG_TO_RAD);
			northPoleDEC = (-12.5 * Constant.DEG_TO_RAD);
			lon0 = 0;
			rot_per_day = 390.0;
			break;
		default:
			return null;
		}
		return new double[] {mag, northPoleRA, northPoleDEC, lon0, rot_per_day};
	}

	/**
	 * Returns the magnitude and rotation model parameters of the body following IAU 2006
	 * resolutions.
	 * @param target Target body.
	 * @param JD Julian day in dynamical time.
	 * @param rr Distance parameter of the body: distance (to Earth) * distance from Sun.
	 * For the Sun this value is its distance.
	 * @param PH Phase angle in degrees, as a positive value. Both this value and the previous
	 * one affects only the returned magnitude (first value).
	 * @return The magnitude and rotation model for IAU 2006: magnitude, north pole RA,
	 * north pole DEC (rad), longitude of planet at J2000 (deg), and speed rotation in degrees/day.
	 * Null is returned for a target without these data.
	 */
	public static double[] getIAU2006Model(TARGET target, double JD, double rr, double PH) {
		double mag = 0, northPoleRA = 0, northPoleDEC = 0, rot_per_day = 0, lon0 = 0;

		double calc_time = Functions.toCenturies(JD);

		switch (target)
		{
		case SUN:
			mag = -26.742 + 5.0 * Math.log10(rr);
			northPoleRA = (286.13 * Constant.DEG_TO_RAD);
			northPoleDEC = (63.87 * Constant.DEG_TO_RAD);
			rot_per_day = 14.1844;
			lon0 = 84.176;
			break;
		case MERCURY:
			mag = -0.36 + 5.0 * Math.log10(rr) + 0.038 * PH - 0.000273 * PH * PH + 2.0E-6 * Math.pow(PH, 3.0);
			northPoleRA = ((281.01 - 0.033 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = ((61.45 - 0.005 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 329.548;
			rot_per_day = 6.1385025;
			break;
		case VENUS:
			mag = -4.29 + 5.0 * Math.log10(rr) + 0.0009 * PH + 0.000239 * PH * PH - 6.5E-7 * Math.pow(PH, 3.0);
			northPoleRA = (272.76 * Constant.DEG_TO_RAD);
			northPoleDEC = (67.16 * Constant.DEG_TO_RAD);
			lon0 = 160.20;
			rot_per_day = -1.4813688;
			break;
		case EARTH: // Here the observer is supposed to be outside the Earth
			mag = -3.86 + 5.0 * Math.log10(rr);
			northPoleRA = ((0.0 - 0.641 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = ((90.0 - 0.557 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 190.147;
			rot_per_day = 360.9856235;
			break;
		case MARS:
			mag = -1.52 + 5.0 * Math.log10(rr) + 0.016 * PH;
			northPoleRA =  ((317.68143 - 0.1061 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = ((52.8865 - 0.0609 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 176.630;
			rot_per_day = 350.89198226;
			break;
		case JUPITER:
			mag = -9.25 + 5.0 * Math.log10(rr) + 0.005 * PH;
			northPoleRA = ((268.056595 - 0.006499 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = ((64.495303 + 0.002413 * calc_time) * Constant.DEG_TO_RAD);
			double Ja = (99.360714 + 4850.4046 * calc_time) * Constant.DEG_TO_RAD;
			double Jb = (175.895369 + 1191.9605 * calc_time) * Constant.DEG_TO_RAD;
			double Jc = (300.323162 + 262.5475 * calc_time) * Constant.DEG_TO_RAD;
			double Jd = (114.012305 + 6070.2476 * calc_time) * Constant.DEG_TO_RAD;
			double Je = (49.511251 + 64.3 * calc_time) * Constant.DEG_TO_RAD;
			northPoleRA += (0.000117 * Math.sin(Ja) + 0.000938 * Math.sin(Jb) +
					0.001432 * Math.sin(Jc) + 0.00003 * Math.sin(Jd) + 0.00215 * Math.sin(Je)) * Constant.DEG_TO_RAD;
			northPoleDEC += (0.00005 * Math.cos(Ja) + 0.000404 * Math.cos(Jb) +
					0.000617 * Math.cos(Jc) - 0.000013 * Math.cos(Jd) + 0.000926 * Math.cos(Je)) * Constant.DEG_TO_RAD;
			lon0 = 284.95; // for System III
			rot_per_day = 870.536642;
			break;
		case SATURN:
			mag = -8.88 + 5.0 * Math.log10(rr) + 0.044 * PH;
			northPoleRA = ((40.589 - 0.036 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = ((83.537 - 0.004 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 38.9; // for System III
			rot_per_day = 810.7939024;
			break;
		case URANUS:
			mag = -7.19 + 5.0 * Math.log10(rr);
			northPoleRA = (257.311 * Constant.DEG_TO_RAD);
			northPoleDEC = (-15.175 * Constant.DEG_TO_RAD);
			lon0 = 203.81;
			rot_per_day = -501.1600928;
			break;
		case NEPTUNE:
			mag = -6.87 + 5.0 * Math.log10(rr);
			double tmp = (357.85 + 52.316 * calc_time) * Constant.DEG_TO_RAD;
			northPoleRA = ((299.36 + 0.70 * Math.sin(tmp)) * Constant.DEG_TO_RAD);
			northPoleDEC = ((43.46 - 0.51 * Math.cos(tmp)) * Constant.DEG_TO_RAD);
			lon0 = 253.18 - 0.48 * Math.sin(tmp);
			rot_per_day = 536.3128492;
			break;
		case Pluto:
			mag = -1.01 + 5.0 * Math.log10(rr);
			northPoleRA = (312.993 * Constant.DEG_TO_RAD);
			northPoleDEC = (6.163 * Constant.DEG_TO_RAD);
			lon0 = 302.695; // See the 'erratum' by Archinal et al. 2011 (Celestial Mechanics & Dynamical Astronomy 110-4, 401-403)
			rot_per_day = 56.3625225; // See the 'erratum' by Archinal et al. 2011 (Celestial Mechanics & Dynamical Astronomy 110-4, 401-403)
			break;
		case Moon:
			mag = 0.23 + 5.0 * Math.log10(rr) + 0.026 * PH + 4.0E-9 * Math.pow(PH, 4.0);
			double data[] = moonAxis(JD);
			northPoleRA = data[0];
			northPoleDEC = data[1];
			lon0 = data[2];
			rot_per_day = data[3];
			break;
		case Vesta:
			mag = 0.0 + 5.0 * Math.log10(rr);
			northPoleRA = (301 * Constant.DEG_TO_RAD);
			northPoleDEC = (41 * Constant.DEG_TO_RAD);
			lon0 = 292.0;
			rot_per_day = 1617.332776;
			break;
		case Ida:
			mag = 0.0 + 5.0 * Math.log10(rr);
			northPoleRA = (168.76 * Constant.DEG_TO_RAD);
			northPoleDEC = (-2.88 * Constant.DEG_TO_RAD);
			lon0 = 274.05; // See the 'erratum' by Archinal et al. 2011 (Celestial Mechanics & Dynamical Astronomy 110-4, 401-403)
			rot_per_day = 1864.6280070;
			break;
		case Eros:
			mag = 0.0 + 5.0 * Math.log10(rr);
			northPoleRA = (11.35 * Constant.DEG_TO_RAD);
			northPoleDEC = (17.22 * Constant.DEG_TO_RAD);
			lon0 = 326.07;
			rot_per_day = 1639.38864745;
			break;
		case Gaspra:
			mag = 0.0 + 5.0 * Math.log10(rr);
			northPoleRA = (9.47 * Constant.DEG_TO_RAD);
			northPoleDEC = (26.70 * Constant.DEG_TO_RAD);
			lon0 = 83.67;
			rot_per_day = 1226.9114850;
			break;
		case Itokawa: // 25143
			mag = 0.0 + 5.0 * Math.log10(rr);
			northPoleRA = (90.53 * Constant.DEG_TO_RAD);
			northPoleDEC = (-66.3 * Constant.DEG_TO_RAD);
			lon0 = 0;
			rot_per_day = 712.143;
			break;
		case P9_Tempel_1:
			mag = 0.0 + 5.0 * Math.log10(rr);
			northPoleRA = (294.0 * Constant.DEG_TO_RAD);
			northPoleDEC = (73 * Constant.DEG_TO_RAD);
			lon0 = 252.63;
			rot_per_day = 212.064;
			break;
		case P19_Borrelly:
			mag = 0.0 + 5.0 * Math.log10(rr);
			northPoleRA = (218.5 * Constant.DEG_TO_RAD);
			northPoleDEC = (-12.5 * Constant.DEG_TO_RAD);
			lon0 = 0;
			rot_per_day = 390.0;
			break;
		default:
			return null;
		}
		return new double[] {mag, northPoleRA, northPoleDEC, lon0, rot_per_day};
	}

	/**
	 * Returns the magnitude and rotation model parameters of the body following IAU 2000
	 * resolutions.
	 * @param target Target body.
	 * @param JD Julian day in dynamical time.
	 * @param rr Distance parameter of the body: distance (to Earth) * distance from Sun.
	 * For the Sun this value is its distance.
	 * @param PH Phase angle in degrees, as a positive value. Both this value and the previous
	 * one affects only the returned magnitude (first value).
	 * @return The magnitude and rotation model for IAU 2000: magnitude, north pole RA,
	 * north pole DEC (rad), longitude of planet at J2000 (deg), and speed rotation in degrees/day.
	 * Null is returned for a target without these data.
	 */
	public static double[] getIAU2000Model(TARGET target, double JD, double rr, double PH) {
		double mag = 0, northPoleRA = 0, northPoleDEC = 0, rot_per_day = 0, lon0 = 0;

		double calc_time = Functions.toCenturies(JD);

		switch (target)
		{
		case SUN:
			mag = -26.742 + 5.0 * Math.log10(rr);
			northPoleRA = (286.13 * Constant.DEG_TO_RAD);
			northPoleDEC = (63.87 * Constant.DEG_TO_RAD);
			rot_per_day = 14.1844;
			lon0 = 84.1;
			break;
		case MERCURY:
			mag = -0.36 + 5.0 * Math.log10(rr) + 0.038 * PH - 0.000273 * PH * PH + 2.0E-6 * Math.pow(PH, 3.0);
			northPoleRA = ((281.01 - 0.033 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = ((61.45 - 0.005 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 329.548;
			rot_per_day = 6.1385025;
			break;
		case VENUS:
			mag = -4.29 + 5.0 * Math.log10(rr) + 0.0009 * PH + 0.000239 * PH * PH - 6.5E-7 * Math.pow(PH, 3.0);
			northPoleRA = (272.76 * Constant.DEG_TO_RAD);
			northPoleDEC = (67.16 * Constant.DEG_TO_RAD);
			lon0 = 160.20;
			rot_per_day = -1.4813688;
			break;
		case EARTH: // Here the observer is supposed to be outside the Earth
			mag = -3.86 + 5.0 * Math.log10(rr);
			northPoleRA = ((0.0 - 0.641 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = ((90.0 - 0.557 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 190.147;
			rot_per_day = 360.9856235;
			break;
		case MARS:
			mag = -1.52 + 5.0 * Math.log10(rr) + 0.016 * PH;
			northPoleRA =  ((317.68143 - 0.1061 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = ((52.8865 - 0.0609 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 176.630; // Value published in Preprint 2002 IAU and used by JPL
			// Previous value -176.753 in IAU travaux 2001 seems to be wrong
			rot_per_day = 350.89198226;
			break;
		case JUPITER:
			mag = -9.25 + 5.0 * Math.log10(rr) + 0.005 * PH;
			northPoleRA = ((268.05 - 0.009 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = ((64.49 + 0.003 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 284.95; // for System III
			rot_per_day = 870.536642;
			break;
		case SATURN:
			mag = -8.88 + 5.0 * Math.log10(rr) + 0.044 * PH;
			northPoleRA = ((40.589 - 0.036 * calc_time) * Constant.DEG_TO_RAD);
			northPoleDEC = ((83.537 - 0.004 * calc_time) * Constant.DEG_TO_RAD);
			lon0 = 38.9; // for System III
			rot_per_day = 810.7939024;
			break;
		case URANUS:
			mag = -7.19 + 5.0 * Math.log10(rr);
			northPoleRA = (257.311 * Constant.DEG_TO_RAD);
			northPoleDEC = (-15.175 * Constant.DEG_TO_RAD);
			lon0 = 203.81;
			rot_per_day = -501.1600928;
			break;
		case NEPTUNE:
			mag = -6.87 + 5.0 * Math.log10(rr);
			double tmp = (357.85 + 52.316 * calc_time) * Constant.DEG_TO_RAD;
			northPoleRA = ((299.36 + 0.70 * Math.sin(tmp)) * Constant.DEG_TO_RAD);
			northPoleDEC = ((43.46 - 0.51 * Math.cos(tmp)) * Constant.DEG_TO_RAD);
			lon0 = 253.18 - 0.48 * Math.sin(tmp);
			rot_per_day = 536.3128492;
			break;
		case Pluto:
			mag = -1.01 + 5.0 * Math.log10(rr);
			northPoleRA = (313.02 * Constant.DEG_TO_RAD);
			northPoleDEC = (9.09 * Constant.DEG_TO_RAD);
			lon0 = 236.77;
			rot_per_day = -56.3623195;
			break;
		case Moon:
			mag = 0.23 + 5.0 * Math.log10(rr) + 0.026 * PH + 4.0E-9 * Math.pow(PH, 4.0);
			double data[] = moonAxis(JD);
			northPoleRA = data[0];
			northPoleDEC = data[1];
			lon0 = data[2];
			rot_per_day = data[3];
			break;
		case Ida:
			mag = 0.0 + 5.0 * Math.log10(rr);
			northPoleRA = (348.76 * Constant.DEG_TO_RAD);
			northPoleDEC = (87.12 * Constant.DEG_TO_RAD);
			lon0 = 265.95;
			rot_per_day = -1864.6280070;
			break;
		case Gaspra:
			mag = 0.0 + 5.0 * Math.log10(rr);
			northPoleRA = (9.47 * Constant.DEG_TO_RAD);
			northPoleDEC = (26.70 * Constant.DEG_TO_RAD);
			lon0 = 83.67;
			rot_per_day = 1226.9114850;
			break;
		case Vesta:
			mag = 0.0 + 5.0 * Math.log10(rr);
			northPoleRA = (301.0 * Constant.DEG_TO_RAD);
			northPoleDEC = (41.0 * Constant.DEG_TO_RAD);
			lon0 = 292.0;
			rot_per_day = 1617.332776;
			break;
		case Eros:
			mag = 0.0 + 5.0 * Math.log10(rr);
			northPoleRA = (11.35 * Constant.DEG_TO_RAD);
			northPoleDEC = (17.22 * Constant.DEG_TO_RAD);
			lon0 = 326.07;
			rot_per_day = 1639.38864745;
			break;
		default:
			return null;
		}
		return new double[] {mag, northPoleRA, northPoleDEC, lon0, rot_per_day};
	}

	/**
	 * Sets the physical parameters of one Ephem object into another. Fields are
	 * angular radius, defect of illumination, elongation, phase, phase angle, constellation,
	 * axis PA, pole PA, bright limb angle, subsolar and sub-Earth positions, magnitude,
	 * and surface brightness. In case ephemerides are calculated respect a body different
	 * from Earth, the correct constellation name is calculated by obtaining internally
	 * the equivalent Earth-based position.
	 * @param ephem_output The output object, their fields will be modified.
	 * @param ephem_input The input object, their fields will be taken to modify
	 * the output object.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 */
	public static void setPhysicalParameters(EphemElement ephem_output, EphemElement ephem_input,
			TimeElement time, ObserverElement obs, EphemerisElement eph) {
		ephem_output.angularRadius = ephem_input.angularRadius;
		ephem_output.defectOfIllumination = ephem_input.defectOfIllumination;
		ephem_output.elongation = ephem_input.elongation;
		ephem_output.phase = ephem_input.phase;
		ephem_output.phaseAngle = ephem_input.phaseAngle;
		ephem_output.constellation = ephem_input.constellation;
		ephem_output.positionAngleOfAxis = ephem_input.positionAngleOfAxis;
		ephem_output.positionAngleOfPole = ephem_input.positionAngleOfPole;
		ephem_output.brightLimbAngle = ephem_input.brightLimbAngle;
		ephem_output.subsolarLatitude = ephem_input.subsolarLatitude;
		ephem_output.subsolarLongitude = ephem_input.subsolarLongitude;
		ephem_output.longitudeOfCentralMeridian = ephem_input.longitudeOfCentralMeridian;
		ephem_output.longitudeOfCentralMeridianSystemI = ephem_input.longitudeOfCentralMeridianSystemI;
		ephem_output.longitudeOfCentralMeridianSystemII = ephem_input.longitudeOfCentralMeridianSystemII;
		ephem_output.longitudeOfCentralMeridianSystemIII = ephem_input.longitudeOfCentralMeridianSystemIII;
		ephem_output.northPoleRA = ephem_input.northPoleRA;
		ephem_output.northPoleDEC = ephem_input.northPoleDEC;
		ephem_output.magnitude = ephem_input.magnitude;
		ephem_output.surfaceBrightness = ephem_input.surfaceBrightness;

		if (obs.getMotherBody() != TARGET.NOT_A_PLANET && obs.getMotherBody() != TARGET.EARTH) {
			try {
				LocationElement locE = ephem_output.getEquatorialLocation();
					locE = Ephem.getPositionFromEarth(locE, time, obs, eph);
				double JD_TDB = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
				ephem_output.constellation = jparsec.astronomy.Constellation.getConstellationName(locE.getLongitude(),
						locE.getLatitude(), JD_TDB, eph);
			} catch (Exception exc) {
				Logger.log(LEVEL.ERROR, "Could not correct constellation for object "+ephem_output.name+" for ephemerides from "+obs.getMotherBody()+".");
			}
		}
	}

	/**
	 * Returns the mean rotation rate of a given body using the corresponding IAU model.
	 * @param eph Ephemeris object containing target body and ephemeris method.
	 * @return The rotation rate in radians/second.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double getBodyMeanRotationRate(EphemerisElement eph) throws JPARSECException {
		if (eph.targetBody.isNaturalSatellite()) return MoonPhysicalParameters.getBodyMeanRotationRate(eph);
		double JD = 2451545, rr = 1, PH = 0, rotationModel[] = null;
		switch (eph.ephemMethod) {
		case IAU_2006:
			rotationModel = getIAU2006Model(eph.targetBody, JD, rr, PH);
			break;
		case IAU_2009:
			rotationModel = getIAU2009Model(eph.targetBody, JD, rr, PH);
			break;
		case IAU_2015:
			rotationModel = getIAU2015Model(eph.targetBody, JD, rr, PH);
			break;
		default:
			rotationModel = getIAU2000Model(eph.targetBody, JD, rr, PH);
			break;
		}
		return rotationModel[4] * Math.PI / (24.0 * 3600.0 * 180.0);
	}

	/**
	 * Returns the sidereal time at 0&deg; of longitude of a given body using the corresponding IAU model.
	 * @param JD_TDB Julian day in TDB.
	 * @param eph Ephemeris object containing target body and ephemeris method.
	 * @return Apparent sidereal time in radians for an observer at longitude 0.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double getBodySiderealTimeAt0Lon(double JD_TDB, EphemerisElement eph) throws JPARSECException {
		if (eph.targetBody.isNaturalSatellite()) return MoonPhysicalParameters.getBodySiderealTimeAt0Lon(JD_TDB, eph);
		double JD = JD_TDB, rr = 1, PH = 0, rotationModel[] = null;
		switch (eph.ephemMethod) {
		case IAU_2006:
			rotationModel = getIAU2006Model(eph.targetBody, JD, rr, PH);
			break;
		case IAU_2009:
			rotationModel = getIAU2009Model(eph.targetBody, JD, rr, PH);
			break;
		case IAU_2015:
			rotationModel = getIAU2015Model(eph.targetBody, JD, rr, PH);
			break;
		default:
			rotationModel = getIAU2000Model(eph.targetBody, JD, rr, PH);
			break;
		}
		double lon = Functions.normalizeDegrees(90.0 + rotationModel[3] + rotationModel[4] * (JD - 2451545.0)) * Constant.DEG_TO_RAD;
		lon -= Ephem.getZeroRightAscension(new TimeElement(JD_TDB, SCALE.BARYCENTRIC_DYNAMICAL_TIME), 
				ObserverElement.parseExtraterrestrialObserver(new ExtraterrestrialObserverElement("", eph.targetBody, new LocationElement(0, 0, 0))), eph, 0.5);
		return lon;
	}

	/**
	 * Returns the orientation of the north pole of rotation of a given body using the corresponding IAU model.
	 * This method gives the same value as {@linkplain #getPlanetaryAxisDirection(double, EphemerisElement)}.
	 * @param JD_TDB Julian day in TDB.
	 * @param eph Ephemeris object containing target body, ephemeris method, and output equinox.
	 * @return North pole direction.
	 * @throws JPARSECException If an error occurs.
	 */
	public static LocationElement getBodyNorthPole(double JD_TDB, EphemerisElement eph) throws JPARSECException {
		return getPlanetaryAxisDirection(JD_TDB, eph);
	}
}
