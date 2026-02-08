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

import java.io.Serializable;
import java.util.Arrays;

import jparsec.astronomy.Star;
import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.EphemerisElement.REDUCTION_METHOD;
import jparsec.ephem.RiseSetTransit;
import jparsec.ephem.Target;
import jparsec.ephem.moons.MoonEphemElement;
import jparsec.ephem.probes.SatelliteEphemElement;
import jparsec.ephem.stars.StarEphemElement;
import jparsec.math.Constant;
import jparsec.observer.LocationElement;
import jparsec.util.JPARSECException;

/**
 * Convenient class for ephem data access.
 *
 * @see Ephem
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class EphemElement implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Constant that holds the magnitude of a planet or satellite when it is fully eclipsed/occulted.
	 */
	public static final int INVALID_MAGNITUDE = 100;

	/**
	 * Constructs an ephem object providing the values of the fields.
	 * This sets the values of all the fields except light time, rise, set,
	 * transit, transit elevation, and constellation.
	 *
	 * @param ra Right Ascension in radians.
	 * @param dec Declination in radians.
	 * @param r Distance to the observer in AU.
	 * @param tam Angular radius in radians.
	 * @param mag Apparent magnitude.
	 * @param sun_r Distance to the sun.
	 * @param elong Elongation in radians, only for objects different to the
	 *        sun.
	 * @param fase Phase percentage, only for objects different to the sun.
	 * @param fase_ang Phase angle in radians, only for objects different to the
	 *        sun.
	 * @param p0 Position angle of axis in radians.
	 * @param b0 Position angle of pole in radians.
	 * @param l0 Longitude of central meridian in radians.
	 * @param subs_lon Subsolar longitude in radians.
	 * @param subs_lat Subsolar latitude in radians.
	 * @param l0_I Longitude of central meridian, system I.
	 * @param l0_II Longitude of central meridian, system II.
	 * @param l0_III Longitude of central meridian, system III.
	 * @param limb Bright limb angle in radians.
	 * @param paralactic Paralactic angle in radians.
	 * @param azi Azimuth in radians.
	 * @param altitude Altitude in radians.
	 * @param ecl_lon Heliocentric ecliptic longitude in radians.
	 * @param ecl_lat Heliocentric ecliptic latitude in radians.
	 * @param np_ra North pole right ascension in radians.
	 * @param np_dec North pole declination in radians.
	 * @param defect Defect of illumination in radians.
	 * @param bright Surface brightness in mag/arcsecond^2.
	 */
	public EphemElement(double ra, double dec, double r, float tam, float mag, double sun_r, float elong,
			float fase, float fase_ang, double p0, double b0, double l0, double subs_lon, double subs_lat,
			double l0_I, double l0_II, double l0_III, float limb, float paralactic, double azi, double altitude,
			double ecl_lon, double ecl_lat, double np_ra, double np_dec, float defect, float bright)
	{
		rightAscension = ra;
		declination = dec;
		distance = r;
		angularRadius = tam;
		magnitude = mag;
		distanceFromSun = sun_r;
		elongation = elong;
		phase = fase;
		phaseAngle = fase_ang;
		positionAngleOfAxis = p0;
		positionAngleOfPole = b0;
		longitudeOfCentralMeridian = l0;
		subsolarLongitude = subs_lon;
		subsolarLatitude = subs_lat;
		longitudeOfCentralMeridianSystemI = l0_I;
		longitudeOfCentralMeridianSystemII = l0_II;
		longitudeOfCentralMeridianSystemIII = l0_III;
		brightLimbAngle = limb;
		paralacticAngle = paralactic;
		azimuth = azi;
		elevation = altitude;
		heliocentricEclipticLongitude = ecl_lon;
		heliocentricEclipticLatitude = ecl_lat;
		northPoleRA = np_ra;
		northPoleDEC = np_dec;
		defectOfIllumination = defect;
		surfaceBrightness = bright;
		setLocation(new LocationElement(ra, dec, r));
	}

	/**
	 * Constructs an ephem object providing the values of all fields.
	 *
	 * @param ra Right Ascension in radians.
	 * @param dec Declination in radians.
	 * @param r Distance to the observer in AU.
	 * @param tam Angular radius in radians.
	 * @param mag Apparent magnitude.
	 * @param sun_r Distance to the sun.
	 * @param elong Elongation in radians, only for objects different to the
	 *        sun.
	 * @param fase Phase percentage, only for objects different to the sun.
	 * @param fase_ang Phase angle in radians, only for objects different to the
	 *        sun.
	 * @param p0 Position angle of axis in radians.
	 * @param b0 Position angle of pole in radians.
	 * @param l0 Longitude of central meridian in radians.
	 * @param subs_lon Subsolar longitude in radians.
	 * @param subs_lat Subsolar latitude in radians.
	 * @param l0_I Longitude of central meridian, system I.
	 * @param l0_II Longitude of central meridian, system II.
	 * @param l0_III Longitude of central meridian, system III.
	 * @param limb Bright limb angle in radians.
	 * @param paralactic Paralactic angle in radians.
	 * @param azi Azimuth in radians.
	 * @param altitude Altitude in radians.
	 * @param ecl_lon Heliocentric ecliptic longitude in radians.
	 * @param ecl_lat Heliocentric ecliptic latitude in radians.
	 * @param np_ra North pole right ascension in radians.
	 * @param np_dec North pole declination in radians.
	 * @param defect Defect of illumination in radians.
	 * @param bright Surface brightness in mag/arcsecond^2.
	 * @param lt Light-time.
	 * @param trise Rise date.
	 * @param tset Set date.
	 * @param ttransit Transit date.
	 * @param tr_elev Transit elevation.
	 * @param constel Constellation.
	 */
	public EphemElement(double ra, double dec, double r, float tam, float mag, double sun_r, float elong,
			float fase, float fase_ang, double p0, double b0, double l0, double subs_lon, double subs_lat,
			double l0_I, double l0_II, double l0_III, float limb, float paralactic, double azi, double altitude,
			double ecl_lon, double ecl_lat, double np_ra, double np_dec, float defect, float bright, float lt,
			double trise, double tset, double ttransit, float tr_elev, String constel)
	{
		rightAscension = ra;
		declination = dec;
		distance = r;
		angularRadius = tam;
		magnitude = mag;
		distanceFromSun = sun_r;
		elongation = elong;
		phase = fase;
		phaseAngle = fase_ang;
		positionAngleOfAxis = p0;
		positionAngleOfPole = b0;
		longitudeOfCentralMeridian = l0;
		subsolarLongitude = subs_lon;
		subsolarLatitude = subs_lat;
		longitudeOfCentralMeridianSystemI = l0_I;
		longitudeOfCentralMeridianSystemII = l0_II;
		longitudeOfCentralMeridianSystemIII = l0_III;
		brightLimbAngle = limb;
		paralacticAngle = paralactic;
		azimuth = azi;
		elevation = altitude;
		heliocentricEclipticLongitude = ecl_lon;
		heliocentricEclipticLatitude = ecl_lat;
		northPoleRA = np_ra;
		northPoleDEC = np_dec;
		defectOfIllumination = defect;
		surfaceBrightness = bright;
		lightTime = lt;
		rise = new double[] {trise};
		set = new double[] {tset};
		transit = new double[] {ttransit};
		transitElevation = new float[] {tr_elev};
		constellation = constel;
		setLocation(new LocationElement(ra, dec, r));
	}

	/**
	 * Constructs an empty ephem object.
	 */
	public EphemElement()
	{
		rightAscension = 0.0;
		declination = 0.0;
		distance = 0.0;
		angularRadius = 0.0f;
		magnitude = 0.0f;
		distanceFromSun = 0.0;
		elongation = 0.0f;
		phaseAngle = 0.0f;
		phase = 0.0f;
		positionAngleOfAxis = 0.0f;
		positionAngleOfPole = 0.0f;
		longitudeOfCentralMeridian = 0.0f;
		longitudeOfCentralMeridianSystemI = 0.0f;
		longitudeOfCentralMeridianSystemII = 0.0f;
		longitudeOfCentralMeridianSystemIII = 0.0f;
		brightLimbAngle = 0.0f;
		subsolarLatitude = 0.0f;
		subsolarLongitude = 0.0f;
		paralacticAngle = 0.0f;
		elevation = 0.0;
		azimuth = 0.0;
		heliocentricEclipticLongitude = 0.0;
		heliocentricEclipticLatitude = 0.0;
		northPoleRA = 0.0f;
		northPoleDEC = 0.0f;
		defectOfIllumination = 0.0f;
		surfaceBrightness = 0.0f;
		lightTime = 0.0f;
	}

	/**
	 * Right Ascension in radians. It is measured respect the true or mean equinox of the
	 * epoch selected in the ephemeris object. For positions calculated for an observer
	 * located in the surface of a planet different from the Earth, the reference RA = 0
	 * is taken as the meridian containing the Earth's north pole at J2000. This criteria
	 * is not the same as the one used by JPL.
	 */
	public double rightAscension;

	/**
	 * Declination in radians. It is measured respect the equator of the reference body,
	 * usually the Earth.
	 */
	public double declination;

	/**
	 * Distance to the observer in Astronomical Units. As a general rule, distance
	 * is calculated as 'apparent', which means that is referred to the instant light
	 * seen by the observer for a given calculation time, and not the true distance
	 * the body has at that time (that cannot be 'seen' due to its distance).
	 */
	public double distance;

	/**
	 * Angular radius in radians.
	 */
	public float angularRadius;

	/**
	 * Apparent magnitude, corrected for extinction if the corresponding flag is
	 * enabled in the configuration class. This magnitude maybe not accurate for
	 * calculations done from outside the Earth.
	 */
	public float magnitude;

	/**
	 * Distance from the Sun in Astronomical Units. As a general rule, it is the
	 * distance that the planet had to the Sun when it emitted the light
	 * that reaches the observer at a given calculation time ('apparent', not the
	 * true geometric distance), since ephemerides are usually apparent, not geometric.
	 * Note that in certain methods (in fact, all of them except JPL ephemerides)
	 * the Sun is incorrectly assumed to be at the Solar System Barycenter (SSB),
	 * and this value is the distance to SSB, not to the Sun itsef.
	 */
	public double distanceFromSun;

	/**
	 * Elongation in radians.
	 */
	public float elongation;

	/**
	 * Phase angle in radians. It can be negative if the ecliptic longitude
	 * of the Earth is below that of the planet (before opposition in
	 * outer planets).
	 */
	public float phaseAngle;

	/**
	 * Visible phase percentage, from 0 to 1.
	 */
	public float phase;

	/**
	 * Position angle of axis in radians. 0 is towards north, 90 degrees towards
	 * East.
	 */
	public double positionAngleOfAxis;

	/**
	 * Position angle of pole in radians. Positive values means that the north
	 * hemisphere of the planet is towards the observer. For the Moon it
	 * will be the libration in latitude. As a general rule, this value is
	 * corrected from planetocentric to planetogeodetic, which means that in practice it
	 * is a sub-observer latitude that can be compared with topographic maps.
	 * {@linkplain Target} class contains methods for transforming between both values.
	 */
	public double positionAngleOfPole;

	/**
	 * Longitude of central meridian in radians. For giant planets (Jupiter,
	 * Saturn, Uranus, and Neptune) the value will be refered to system III of
	 * coordinates. For the Moon it will be the libration in longitude.
	 * The longitude is measured towards east for the Moon (positive values
	 * represents positions towards east or to the right on the Moon), the Sun,
	 * and the inner planets Mercury and Venus. For Mars and the rest of the
	 * outer planets this value is measured towards west (west longitudes are
	 * positive).
	 */
	public double longitudeOfCentralMeridian;

	/**
	 * Longitude of central meridian in radians for system I of coordinates
	 * (mean rotation of equatorial belt). Only available in Jupiter and Saturn.
	 */
	public double longitudeOfCentralMeridianSystemI;

	/**
	 * Longitude of central meridian in radians for system II of coordinates
	 * (mean rotation of the tropical belt). Only available in Jupiter,
	 * otherwise NULL.
	 */
	public double longitudeOfCentralMeridianSystemII;

	/**
	 * Longitude of central meridian in radians for system III of coordinates
	 * (rotation of magnetic field). Only available in giant planets (Jupiter,
	 * Saturn, Uranus, and Neptune).
	 */
	public double longitudeOfCentralMeridianSystemIII;

	/**
	 * Bright limb angle in radians.
	 */
	public float brightLimbAngle;

	/**
	 * Subsolar latitude. In case of Saturn, Uranus, and Neptune, can be
	 * considered the ring plane ilumination angle. It depends on the relative
	 * position of the Sun. As a general rule, this value is
	 * corrected from planetocentric to planetogeodetic, which means that in practice it
	 * is a sub-observer latitude that can be compared with topographic maps.
	 * {@linkplain Target} class contains methods for transforming between both values.
	 */
	public double subsolarLatitude;

	/**
	 * Subsolar longitude. It depends on the relative position of the Sun. In
	 * giant planets the value is refered to System III of coordinates.
	 */
	public double subsolarLongitude;

	/**
	 * Paralactic angle in radians.
	 */
	public float paralacticAngle;

	/**
	 * Azimuth in radians. It is 0 for an object towards South, and -90 degrees
	 * if it is towards East.
	 */
	public double azimuth;

	/**
	 * Geometric/apparent elevation in radians. For apparent ephemerides the
	 * apparent elevation will be provided, unless the elevation itself is
	 * below -2 degrees. In this case the elevation will be geometric.
	 */
	public double elevation;

	/**
	 * Mean heliocentric ecliptic longitude in radians for the output equinox and frame defined in the
	 * ephemeris object. For JPL ephemerides the value returned is heliocentric, for the others it is
	 * in fact barycentric (referred to solar system barycenter). As with other ephemerides values,
	 * this value is referred to the time the source emitted the light that reaches the observer (it is
	 * corrected for light-time), not to the input time of the ephemerides.
	 *
	 */
	public double heliocentricEclipticLongitude;

	/**
	 * Mean heliocentric ecliptic latitude in radians for the output equinox and frame defined in the
	 * ephemeris object. For JPL ephemerides the value returned is heliocentric, for the others it is
	 * in fact barycentric (referred to solar system barycenter). As with other ephemerides values,
	 * this value is referred to the time the source emitted the light that reaches the observer (it is
	 * corrected for light-time), not to the input time of the ephemerides.
	 */
	public double heliocentricEclipticLatitude;

	/**
	 * Right ascension of the north pole of rotation.
	 */
	public double northPoleRA;

	/**
	 * Declination of the north pole of rotation.
	 */
	public double northPoleDEC;

	/**
	 * Defect of ilumination in radians.
	 */
	public float defectOfIllumination;

	/**
	 * Rise time/s as a Julian day in local time. By default if the object is above the
	 * horizon, then the value will be refered to the current day, otherwise it
	 * will be the next rise event in time. In case the object will not rise for the
	 * calculate date (from 0 to 24h local time of the observer), set time will be
	 * equal to {@linkplain RiseSetTransit#ALWAYS_BELOW_HORIZON}.
	 */
	public double rise[];

	/**
	 * Set time/s as a Julian day in local time. By default if the object is above the
	 * horizon, then the value will be refered to the current day, otherwise it
	 * will be the next set event in time. In case the object will not set for the
	 * calculate date (from 0 to 24h local time of the observer), set time will be
	 * equal to {@linkplain RiseSetTransit#ALWAYS_BELOW_HORIZON}.
	 */
	public double set[];

	/**
	 * Transit (usually maximum elevation) time/s as a Julian day in local time. By default if the
	 * object is above the horizon, then the value will be refered to the
	 * current day, otherwise it will be the next transit event in time.
	 */
	public double transit[];

	/**
	 * Transit geometric/apparent elevation/s from horizon in radians. Usually maximum elevation, but
	 * not necessarily.
	 */
	public float transitElevation[];

	/**
	 * Constellation where the object is located.
	 */
	public String constellation;

	/**
	 * Surface brightness in mag/arcsecond^2, or 0 if the angular radius 
	 * of the object is lower than 0.5 arcseconds.
	 */
	public float surfaceBrightness;

	/**
	 * Holds light time from observer to object in days.
	 */
	public float lightTime;
	/**
	 * Name of the body.
	 */
	public String name;

	/**
	 * Holds additional information for this object, for example if it is
	 * eclipsed in case it represents the ephemerides of a natural or
	 * artificial satellites, or if is occulted.
	 */
	public String status;

	/**
	 * Transform the corresponding information in an {@linkplain MoonEphemElement} object
	 * into a ephem object.
	 *
	 * @param moon_ephem {@linkplain MoonEphemElement} object.
	 * @param jdEpoch Epoch as Julian day when the ephemeris were obtained.
	 * @return Ephem object.
	 * @throws JPARSECException If an error occurs.
	 */
	public static EphemElement parseMoonEphemElement(MoonEphemElement moon_ephem, double jdEpoch)
	throws JPARSECException {
		// Fields missing: central meridian in systems I II III, defect of
		// illumination, and surface brightness
		EphemElement ephem = new EphemElement(moon_ephem.rightAscension, moon_ephem.declination, moon_ephem.distance,
				moon_ephem.angularRadius, moon_ephem.magnitude, moon_ephem.distanceFromSun, moon_ephem.elongation,
				moon_ephem.phase, moon_ephem.phaseAngle, moon_ephem.positionAngleOfAxis,
				moon_ephem.positionAngleOfPole, moon_ephem.longitudeOfCentralMeridian,
				moon_ephem.subsolarLongitude, moon_ephem.subsolarLatitude, 0.0f, 0.0f, 0.0f,
				moon_ephem.brightLimbAngle, moon_ephem.paralacticAngle, moon_ephem.azimuth, moon_ephem.elevation,
				moon_ephem.heliocentricEclipticLongitude, moon_ephem.heliocentricEclipticLatitude, moon_ephem.northPoleRA,
				moon_ephem.northPoleDEC, 0.0f, 0.0f);

		ephem.constellation = "";
		try {
			EphemerisElement eph = new EphemerisElement();
			eph.ephemMethod = REDUCTION_METHOD.IAU_2006;
			String constel = jparsec.astronomy.Constellation.getConstellationName(ephem.rightAscension,
					ephem.declination, jdEpoch, eph);
			ephem.constellation = constel;
		} catch (Exception exc) {}
		ephem.lightTime = (float) (ephem.distance * Constant.LIGHT_TIME_DAYS_PER_AU);
		ephem.surfaceBrightness = 0;
		if (ephem.magnitude != EphemElement.INVALID_MAGNITUDE &&
				(ephem.angularRadius * Constant.RAD_TO_ARCSEC > 0.5))
			ephem.surfaceBrightness = (float) Star.getSurfaceBrightness(ephem.magnitude, ephem.angularRadius * Constant.RAD_TO_ARCSEC);
		ephem.defectOfIllumination = (float) ((1.0 - ephem.phase) * ephem.angularRadius);
		ephem.name = moon_ephem.name;
		ephem.status = moon_ephem.mutualPhenomena;
		return ephem;
	}

	/**
	 * Transform the corresponding information in an {@linkplain StarEphemElement} object
	 * into a ephem object.
	 *
	 * @param star_ephem {@linkplain StarEphemElement} object.
	 * @return Ephem object.
	 */
	public static EphemElement parseStarEphemElement(StarEphemElement star_ephem)
	{
		EphemElement ephem = new EphemElement(star_ephem.rightAscension, star_ephem.declination,
				star_ephem.distance * Constant.PARSEC / (Constant.AU * 1000.0),
				0.0f, star_ephem.magnitude, star_ephem.distance * Constant.PARSEC / (Constant.AU * 1000.0),
				0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
				0.0f, star_ephem.paralacticAngle, star_ephem.azimuth, star_ephem.elevation, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
				0.0f);

		ephem.constellation = star_ephem.constellation;
		ephem.lightTime = (float) (ephem.distance * Constant.LIGHT_TIME_DAYS_PER_AU);
		if (star_ephem.rise != 0.0) ephem.rise = new double[] {star_ephem.rise};
		if (star_ephem.set != 0.0) ephem.set = new double[] {star_ephem.set};
		if (star_ephem.transit != 0.0) ephem.transit = new double[] {star_ephem.transit};
		if (star_ephem.transitElevation != 0.0f) ephem.transitElevation = new float[] {star_ephem.transitElevation};
		ephem.name = star_ephem.name;

		return ephem;
	}

	/**
	 * Transform the corresponding information in an {@linkplain SatelliteEphemElement}
	 * object into a ephem object. The other fields are set to 0.0.
	 *
	 * @param sat_ephem {@linkplain SatelliteEphemElement} object.
	 * @param jdEpoch Epoch as Julian day when the ephemeris were obtained.
	 * @return Ephem object.
	 * @throws JPARSECException If an error occurs.
	 */
	public static EphemElement parseSatelliteEphemElement(SatelliteEphemElement sat_ephem, double jdEpoch)
	throws JPARSECException {
		EphemElement ephem = new EphemElement(sat_ephem.rightAscension, sat_ephem.declination,
				sat_ephem.distance / Constant.AU, sat_ephem.angularRadius, sat_ephem.magnitude,
				0.0f, sat_ephem.elongation, sat_ephem.illumination, 0.0f,
				0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, sat_ephem.azimuth, sat_ephem.elevation, 0.0f, 0.0f,
				0.0f, 0.0f, 0.0f, 0.0f);

		ephem.constellation = "";
		try {
			EphemerisElement eph = new EphemerisElement();
			eph.ephemMethod = REDUCTION_METHOD.IAU_2006;
			String constel = jparsec.astronomy.Constellation.getConstellationName(ephem.rightAscension,
					ephem.declination, jdEpoch, eph);
			ephem.constellation = constel;
		} catch (Exception exc) {}
		ephem.lightTime = (float) (ephem.distance * Constant.LIGHT_TIME_DAYS_PER_AU);
		ephem.surfaceBrightness = 0;
		if (ephem.angularRadius * Constant.RAD_TO_ARCSEC > 0.5)
			ephem.surfaceBrightness = (float) Star.getSurfaceBrightness(ephem.magnitude, ephem.angularRadius * Constant.RAD_TO_ARCSEC);
		ephem.name = sat_ephem.name;
		ephem.rise = sat_ephem.rise;
		ephem.set = sat_ephem.set;
		ephem.transit = sat_ephem.transit;
		ephem.transitElevation = sat_ephem.transitElevation;
		ephem.status = "";
		if (sat_ephem.isEclipsed) ephem.status = "eclipsed";
		return ephem;
	}

	/**
	 * To clone the object.
	 */
	@Override
	public EphemElement clone()
	{
		EphemElement ephem = new EphemElement(this.rightAscension, this.declination, this.distance,
				this.angularRadius, this.magnitude, this.distanceFromSun, this.elongation, this.phase,
				this.phaseAngle, this.positionAngleOfAxis, this.positionAngleOfPole,
				this.longitudeOfCentralMeridian, this.subsolarLongitude, this.subsolarLatitude,
				this.longitudeOfCentralMeridianSystemI, this.longitudeOfCentralMeridianSystemII,
				this.longitudeOfCentralMeridianSystemIII, this.brightLimbAngle, this.paralacticAngle,
				this.azimuth, this.elevation, this.heliocentricEclipticLongitude, this.heliocentricEclipticLatitude,
				this.northPoleRA, this.northPoleDEC, this.defectOfIllumination, this.surfaceBrightness);
		ephem.constellation = this.constellation;
		if (rise != null) ephem.rise = this.rise.clone();
		if (set != null) ephem.set = this.set.clone();
		if (transit != null) ephem.transit = this.transit.clone();
		if (transitElevation != null) ephem.transitElevation = this.transitElevation.clone();
		ephem.lightTime = this.lightTime;
		ephem.name = this.name;
		ephem.location = null;
		if (this.location != null) ephem.location = this.location.clone();
		return ephem;
	}
	/**
	 * Returns if the given object is equal to this ephemeris object.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof EphemElement)) return false;

		EphemElement that = (EphemElement) o;

		if (Double.compare(that.rightAscension, rightAscension) != 0) return false;
		if (Double.compare(that.declination, declination) != 0) return false;
		if (Double.compare(that.distance, distance) != 0) return false;
		if (Float.compare(that.angularRadius, angularRadius) != 0) return false;
		if (Float.compare(that.magnitude, magnitude) != 0) return false;
		if (Double.compare(that.distanceFromSun, distanceFromSun) != 0) return false;
		if (Float.compare(that.elongation, elongation) != 0) return false;
		if (Float.compare(that.phaseAngle, phaseAngle) != 0) return false;
		if (Float.compare(that.phase, phase) != 0) return false;
		if (Double.compare(that.positionAngleOfAxis, positionAngleOfAxis) != 0) return false;
		if (Double.compare(that.positionAngleOfPole, positionAngleOfPole) != 0) return false;
		if (Double.compare(that.longitudeOfCentralMeridian, longitudeOfCentralMeridian) != 0) return false;
		if (Double.compare(that.longitudeOfCentralMeridianSystemI, longitudeOfCentralMeridianSystemI) != 0)
			return false;
		if (Double.compare(that.longitudeOfCentralMeridianSystemII, longitudeOfCentralMeridianSystemII) != 0)
			return false;
		if (Double.compare(that.longitudeOfCentralMeridianSystemIII, longitudeOfCentralMeridianSystemIII) != 0)
			return false;
		if (Float.compare(that.brightLimbAngle, brightLimbAngle) != 0) return false;
		if (Double.compare(that.subsolarLatitude, subsolarLatitude) != 0) return false;
		if (Double.compare(that.subsolarLongitude, subsolarLongitude) != 0) return false;
		if (Float.compare(that.paralacticAngle, paralacticAngle) != 0) return false;
		if (Double.compare(that.azimuth, azimuth) != 0) return false;
		if (Double.compare(that.elevation, elevation) != 0) return false;
		if (Double.compare(that.heliocentricEclipticLongitude, heliocentricEclipticLongitude) != 0) return false;
		if (Double.compare(that.heliocentricEclipticLatitude, heliocentricEclipticLatitude) != 0) return false;
		if (Double.compare(that.northPoleRA, northPoleRA) != 0) return false;
		if (Double.compare(that.northPoleDEC, northPoleDEC) != 0) return false;
		if (Float.compare(that.defectOfIllumination, defectOfIllumination) != 0) return false;
		if (Float.compare(that.surfaceBrightness, surfaceBrightness) != 0) return false;
		if (Float.compare(that.lightTime, lightTime) != 0) return false;
		if (!Arrays.equals(rise, that.rise)) return false;
		if (!Arrays.equals(set, that.set)) return false;
		if (!Arrays.equals(transit, that.transit)) return false;
		if (!Arrays.equals(transitElevation, that.transitElevation)) return false;
		if (constellation != null ? !constellation.equals(that.constellation) : that.constellation != null)
			return false;
		if (name != null ? !name.equals(that.name) : that.name != null) return false;
		if (status != null ? !status.equals(that.status) : that.status != null) return false;

		return !(location != null ? !location.equals(that.location) : that.location != null);
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		temp = Double.doubleToLongBits(rightAscension);
		result = (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(declination);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(distance);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (angularRadius != +0.0f ? Float.floatToIntBits(angularRadius) : 0);
		result = 31 * result + (magnitude != +0.0f ? Float.floatToIntBits(magnitude) : 0);
		temp = Double.doubleToLongBits(distanceFromSun);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (elongation != +0.0f ? Float.floatToIntBits(elongation) : 0);
		result = 31 * result + (phaseAngle != +0.0f ? Float.floatToIntBits(phaseAngle) : 0);
		result = 31 * result + (phase != +0.0f ? Float.floatToIntBits(phase) : 0);
		temp = Double.doubleToLongBits(positionAngleOfAxis);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(positionAngleOfPole);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(longitudeOfCentralMeridian);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(longitudeOfCentralMeridianSystemI);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(longitudeOfCentralMeridianSystemII);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(longitudeOfCentralMeridianSystemIII);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (brightLimbAngle != +0.0f ? Float.floatToIntBits(brightLimbAngle) : 0);
		temp = Double.doubleToLongBits(subsolarLatitude);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(subsolarLongitude);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (paralacticAngle != +0.0f ? Float.floatToIntBits(paralacticAngle) : 0);
		temp = Double.doubleToLongBits(azimuth);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(elevation);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(heliocentricEclipticLongitude);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(heliocentricEclipticLatitude);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(northPoleRA);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(northPoleDEC);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (defectOfIllumination != +0.0f ? Float.floatToIntBits(defectOfIllumination) : 0);
		result = 31 * result + (rise != null ? Arrays.hashCode(rise) : 0);
		result = 31 * result + (set != null ? Arrays.hashCode(set) : 0);
		result = 31 * result + (transit != null ? Arrays.hashCode(transit) : 0);
		result = 31 * result + (transitElevation != null ? Arrays.hashCode(transitElevation) : 0);
		result = 31 * result + (constellation != null ? constellation.hashCode() : 0);
		result = 31 * result + (surfaceBrightness != +0.0f ? Float.floatToIntBits(surfaceBrightness) : 0);
		result = 31 * result + (lightTime != +0.0f ? Float.floatToIntBits(lightTime) : 0);
		result = 31 * result + (name != null ? name.hashCode() : 0);
		result = 31 * result + (status != null ? status.hashCode() : 0);
		result = 31 * result + (location != null ? location.hashCode() : 0);
		return result;
	}

	private LocationElement location;

	/**
	 * Sets the location of this body in a custom coordinate system.
	 * Only used in JPARSEC when rendering the sky.
	 * @param loc Object location.
	 */
	public void setLocation(LocationElement loc) {
		if (loc == null) {
			location = null;
		} else {
			this.location = loc.clone();
		}
	}
	/**
	 * Retrieves the location of this object in a custom coordinate
	 * system, or null if the set method has not been called. Used
	 * internally by JPARSEC to render the sky.
	 * @return Location object, null if still not set.
	 */
	public LocationElement getLocation() {
		return this.location;
	}
	
	/**
	 * Retrieves the location of this object in the equatorial
	 * system.
	 * @return Location object.
	 */
	public LocationElement getEquatorialLocation() {
		double d = this.distance;
		//if (d == 0.0) d = 1; // This line produce problems when calculating the position of bodies for observers outside Earth
		return new LocationElement(this.rightAscension, this.declination, d);
	}
	
	/**
	 * Retrieves the location of this object in the horizontal
	 * system.
	 * @return Location object.
	 */
	public LocationElement getHorizontalLocation() {
		double d = this.distance;
		//if (d == 0.0) d = 1; // This line produce problems when calculating the position of bodies for observers outside Earth
		return new LocationElement(this.azimuth, this.elevation, d);
	}

	/**
	 * Sets the right ascension, declination, and distance for this instance.
	 * @param loc The equatorial position.
	 */
	public void setEquatorialLocation(LocationElement loc) {
		this.rightAscension = loc.getLongitude();
		this.declination = loc.getLatitude();
		this.distance = loc.getRadius();
	}

};
