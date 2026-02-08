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

import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.EphemerisElement.FRAME;
import jparsec.ephem.EphemerisElement.REDUCTION_METHOD;
import jparsec.ephem.Functions;
import jparsec.ephem.Obliquity;
import jparsec.ephem.Precession;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.stars.StarEphem;
import jparsec.math.Constant;
import jparsec.math.FastMath;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.TimeScale;
import jparsec.time.SiderealTime;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.util.JPARSECException;

/**
 * A class to perform coordinates transformations. These methods take into
 * account the equinox specified in the {@linkplain EphemerisElement} object
 * or epoch input value to transform input/output coordinates from/to that epoch
 * accordingly (using IAU2006 algorithms). The frame is not taken into account,
 * but note that equatorial to galactic coordinates should use the FK5 system as
 * input coordinates. Methods to transform between different frames are available
 * in {@linkplain Ephem} class. The orientation of the galactic plane follows
 * Jia-Cheng Liu et al. 2010 (see http://arxiv.org/abs/1010.3773), and since
 * Sgr A is not at the center, a new definition of the galactic system is pending.
 * <P>
 * The methods in this class are complemented by those in other classes. For
 * instance, {@linkplain LocationElement} can be instantiated using a body object
 * to retrieve its coordinates, {@linkplain Ephem#toOutputFrame(double[], 
 * jparsec.ephem.EphemerisElement.FRAME, jparsec.ephem.EphemerisElement.FRAME)}
 * can be used for frame transformations. In case the object has proper motions and
 * FK4 frame is involved, {@linkplain StarEphem} class provides methods to account
 * for proper motions.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 * @see LocationElement
 * @see StarEphem
 * @see Ephem
 */
public class CoordinateSystem
{
	// private constructor so that this class cannot be instantiated.
	private CoordinateSystem() {}

	/**
	 * Transform from horizontal (geometric) to equatorial coordinates.
	 *
	 * @param loc Location object with horizontal coordinates.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @return Location object with the equatorial coordinates.
	 * @throws JPARSECException If the date is invalid.
	 */
	public static LocationElement horizontalToEquatorial(LocationElement loc, TimeElement time, ObserverElement obs,
			EphemerisElement eph) throws JPARSECException
	{
		double TSL = SiderealTime.apparentSiderealTime(time, obs, eph);
		RotateTo rot = new RotateTo(TSL, obs.getLatitudeRad(), Constant.PI_OVER_TWO, -Math.PI-loc.getLongitude(), loc.getLatitude());

		LocationElement loc_out = rotateTo(rot);
		loc_out.setRadius(loc.getRadius());

		return loc_out;
	}

	/**
	 * Transform from equatorial to horizontal (geometric) coordinates.
	 *
	 * @param loc Location object with equatorial coordinates.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @return Location object with the horizontal coordinates.
	 * @throws JPARSECException If the date is invalid.
	 */
	public static LocationElement equatorialToHorizontal(LocationElement loc, TimeElement time, ObserverElement obs,
			EphemerisElement eph) throws JPARSECException
	{
		double TSL = SiderealTime.apparentSiderealTime(time, obs, eph);
		RotateFrom rot = new RotateFrom(-TSL, obs.getLatitudeRad(), Constant.PI_OVER_TWO, -loc.getLongitude(), loc.getLatitude());

		LocationElement loc_out = rotateFrom(rot);
		loc_out.setLongitude(Functions.normalizeRadians(Math.PI+loc_out.getLongitude()));
		loc_out.setRadius(loc.getRadius());

		return loc_out;
	}

	/**
	 * Transform from horizontal to equatorial coordinates.
	 *
	 * @param loc Location object with horizontal coordinates.
	 * @param ast Apparent Sidereal Time in radians.
	 * @param lat Latitude of observer in radians.
	 * @param fast True for an approximate but faster calculation.
	 * @return Location object with the equatorial coordinates.
	 */
	public static LocationElement horizontalToEquatorial(LocationElement loc, double ast, double lat,
			boolean fast)
	{
		RotateTo rot = new RotateTo(ast, lat, Constant.PI_OVER_TWO, -Math.PI-loc.getLongitude(), loc.getLatitude());

		if (fast) {
			LocationElement loc_out = fastRotateTo(rot);
			loc_out.setRadius(loc.getRadius());
			return loc_out;
		} else {
			LocationElement loc_out = rotateTo(rot);
			loc_out.setRadius(loc.getRadius());
			return loc_out;
		}
	}

	/**
	 * Transform from equatorial to horizontal coordinates.
	 *
	 * @param loc Location object with equatorial coordinates.
	 * @param ast Apparent Sidereal Time in radians.
	 * @param obs Observer object.
	 * @param eph The ephemeris object.
	 * @param toApparentElevation True for obtaining apparent elevation.
	 * @param fast True for an approximate but faster calculation.
	 * @return Location object with the horizontal coordinates.
	 * @throws JPARSECException If the date is invalid.
	 */
	public static LocationElement equatorialToHorizontal(LocationElement loc, double ast, ObserverElement obs,
			EphemerisElement eph, boolean toApparentElevation, boolean fast) throws JPARSECException
	{
		RotateFrom rot = new RotateFrom(-ast, obs.getLatitudeRad(), Constant.PI_OVER_TWO, -loc.getLongitude(), loc.getLatitude());

		LocationElement loc_out = null;
		if (fast) {
			loc_out = fastRotateFrom(rot);
		} else {
			loc_out = rotateFrom(rot);
		}
		loc_out.setLongitude(Math.PI+loc_out.getLongitude());
		if (toApparentElevation && obs.getMotherBody() == TARGET.EARTH)
		{
			int maxIter = 10;
			if (fast) maxIter = 1;
			double apparentAlt = Ephem.getApparentElevation(eph, obs, loc_out.getLatitude(), maxIter);
			loc_out.setLatitude(apparentAlt);
		}
		loc_out.setRadius(loc.getRadius());

		return loc_out;
	}

	/**
	 * J2000 position of the galactic pole in FK5 system. See Jia-Cheng Liu et al. 2010,
	 * http://arxiv.org/abs/1010.3773. Distance to galactic center is about 25830 +/- 500 
	 * light years (VERA collaboration, 2020), although the radius field here is set to 1.
	 */
	public static final LocationElement GALACTIC_POLE = new LocationElement(
			Functions.parseRightAscension(12, 51, 26.27549),
			Functions.parseDeclination("27", 7, 41.7043),
			1.0);

	/**
	 * J2000 node of the galactic pole in FK5 system. See Jia-Cheng Liu et al. 2010,
	 * http://arxiv.org/abs/1010.3773.
	 */
	public static final double GALACTIC_POLE_NODE_J2000 = Constant.DEG_TO_RAD * 32.93191857; // 33.0 at B1950

	/**
	 * Transform from galactic to equatorial coordinates. The IAU 1958 system
	 * is used, established before the precise coordinates of Sagittarius A (the
	 * core of our galaxy) were known. The real center of our galaxy lies at
	 * -3' 21" of galactic longitude, and at -2' 46" of galactic latitude.
	 *
	 * @param loc Location object with galactic coordinates.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object. The equinox and frame fields are taken into account to
	 * transform results to output equinox and frame.
	 * @return Location object with the equatorial coordinates, respect to equinox and frame
	 * selected in the ephemeris object.
	 * @throws JPARSECException If the date is invalid.
	 */
	public static LocationElement galacticToEquatorial(LocationElement loc, TimeElement time, ObserverElement obs,
			EphemerisElement eph) throws JPARSECException
	{
		// Rotate
		RotateTo rot = new RotateTo(GALACTIC_POLE.getLongitude(), GALACTIC_POLE.getLatitude(), GALACTIC_POLE_NODE_J2000, loc
				.getLongitude(), loc.getLatitude());
		LocationElement loc_out = rotateTo(rot);
		loc_out.setRadius(loc.getRadius());

		// Transform input position to FK5 J2000
		double equinox = eph.equinox;
		if (equinox == EphemerisElement.EQUINOX_OF_DATE) equinox = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		if (eph.frame != FRAME.FK5)
			loc_out = LocationElement.parseRectangularCoordinates(Ephem.toOutputFrame(loc_out.getRectangularCoordinates(), FRAME.FK5, eph.frame));
		if (eph.frame == FRAME.FK4) {
			if (equinox != Constant.B1950) {
				loc_out = LocationElement.parseRectangularCoordinates(Precession.precessionNewcomb(Constant.B1950, equinox, loc_out.getRectangularCoordinates()));
				JPARSECException.addWarning("Transforming coordinates using Newcomb precession (FK4 system) from B1950 to JD"+equinox+". I warn this because it is unusual.");
			}
		} else {
			if (equinox != Constant.J2000)
				loc_out = LocationElement.parseRectangularCoordinates(Precession.precess(Constant.J2000, equinox, loc_out.getRectangularCoordinates(), eph));
		}

		return loc_out;
	}

	/**
	 * Transform from equatorial to galactic coordinates. The IAU 1958 system
	 * is used, established before the precise coordinates of Sagittarius A (the
	 * core of our galaxy) were known. The real center of our galaxy lies at
	 * -3' 21" of galactic longitude, and at -2' 46" of galactic latitude.
	 *
	 * @param loc Location object with equatorial coordinates, FK5 J2000 (or any other
	 * frame and equinox).
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object. The properties of this object are used to transform
	 * input coordinates to FK5 J2000 (before going to galactic) if this is required.
	 * @return Location object with the galactic coordinates.
	 * @throws JPARSECException If the date is invalid.
	 */
	public static LocationElement equatorialToGalactic(LocationElement loc, TimeElement time, ObserverElement obs,
			EphemerisElement eph) throws JPARSECException
	{
		LocationElement loc_out = loc.clone();

		// Transform input position to FK5 J2000
		double equinox = eph.equinox;
		if (equinox == EphemerisElement.EQUINOX_OF_DATE) equinox = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		if (eph.frame != FRAME.FK5) {
			if (eph.frame == FRAME.FK4) {
				if (eph.equinox != Constant.B1950) {
					loc_out = LocationElement.parseRectangularCoordinates(Precession.precessionNewcomb(equinox, Constant.B1950, loc_out.getRectangularCoordinates()));
					JPARSECException.addWarning("Transforming coordinates using Newcomb precession (FK4 system) from JD"+equinox+" to B1950. I warn this because it is unusual.");
				}
			} else {
				if (equinox != Constant.J2000) {
					double pos[] = LocationElement.parseLocationElement(loc_out);
					double new_pos[] = Precession.precess(equinox, Constant.J2000, pos, eph);
					loc_out = LocationElement.parseRectangularCoordinates(new_pos);
				}
			}
			loc_out = LocationElement.parseRectangularCoordinates(Ephem.toOutputFrame(loc_out.getRectangularCoordinates(), eph.frame, FRAME.FK5));
		} else {
			if (equinox != Constant.J2000) {
				double pos[] = LocationElement.parseLocationElement(loc_out);
				double new_pos[] = Precession.precess(equinox, Constant.J2000, pos, eph);
				loc_out = LocationElement.parseRectangularCoordinates(new_pos);
			}
		}

		// Rotate
		RotateFrom rot = new RotateFrom(GALACTIC_POLE.getLongitude(), GALACTIC_POLE.getLatitude(), GALACTIC_POLE_NODE_J2000, loc_out
				.getLongitude(), loc_out.getLatitude());
		loc_out = rotateFrom(rot);
		loc_out.setRadius(loc.getRadius());

		return loc_out;
	}

	/**
	 * Galactic position of the supergalactic pole. See Lahav et al 2000, MNRAS 312 166-176, 
	 * https://arxiv.org/abs/astro-ph/9809343. Definition by G. de Vaucouleurs 1991.
	 */
	public static final LocationElement SUPER_GALACTIC_POLE = new LocationElement(
			47.37 * Constant.DEG_TO_RAD, 6.32 * Constant.DEG_TO_RAD, 1.0);

	/**
	 * Transform from galactic to supergalactic coordinates using the definition by 
	 * G. de Vaucouleurs 1991. See also Lahav et al 2000, 
	 * The supergalactic plane revisited with the Optical Redshift Survey, MNRAS 312 166-176. 
	 *
	 * @param loc Location object with galactic coordinates.
	 * @return Location object with the supergalactic coordinates.
	 * @throws JPARSECException If the date is invalid.
	 */
	public static LocationElement galacticToSupergalactic(LocationElement loc) throws JPARSECException
	{
		// Rotate
		RotateFrom rot = new RotateFrom(SUPER_GALACTIC_POLE.getLongitude(), SUPER_GALACTIC_POLE.getLatitude(), 
				0, loc.getLongitude(), loc.getLatitude());
		LocationElement loc_out = rotateFrom(rot);
		loc_out.setRadius(loc.getRadius());

		return loc_out;
	}

	/**
	 * Transform from supergalactic to galactic coordinates using the definition by 
	 * G. de Vaucouleurs 1991. See also Lahav et al 2000, 
	 * The supergalactic plane revisited with the Optical Redshift Survey, MNRAS 312 166-176. 
	 *
	 * @param loc Location object with galactic coordinates.
	 * @return Location object with the galactic coordinates.
	 * @throws JPARSECException If the date is invalid.
	 */
	public static LocationElement supergalacticToGalactic(LocationElement loc) throws JPARSECException
	{
		// Rotate
		RotateTo rot = new RotateTo(SUPER_GALACTIC_POLE.getLongitude(), SUPER_GALACTIC_POLE.getLatitude(), 
				0, loc.getLongitude(), loc.getLatitude());
		LocationElement loc_out = rotateTo(rot);
		loc_out.setRadius(loc.getRadius());

		return loc_out;
	}
	
	/**
	 * Transform from ecliptic to equatorial coordinates.
	 *
	 * @param loc Location object with ecliptic coordinates.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object. If the ephemeris type is apparent,
	 *        then the rotation will be done respect to the true ecliptic of
	 *        date.
	 * @return Location object with the equatorial coordinates.
	 * @throws JPARSECException If the date is invalid.
	 */
	public static LocationElement eclipticToEquatorial(LocationElement loc, TimeElement time, ObserverElement obs,
			EphemerisElement eph) throws JPARSECException
	{
		double JD = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		double equinox = eph.equinox;
		if (equinox == EphemerisElement.EQUINOX_OF_DATE)
			equinox = JD;
		double obliquity = 0.0;
		if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT) {
			obliquity = Obliquity.trueObliquity(Functions.toCenturies(equinox), eph);
		} else {
			obliquity = Obliquity.meanObliquity(Functions.toCenturies(equinox), eph);
		}
		RotateTo rot = new RotateTo(-Constant.PI_OVER_TWO, Constant.PI_OVER_TWO - obliquity, 0.0, loc.getLongitude(), loc
				.getLatitude());

		LocationElement loc_out = rotateTo(rot);
		loc_out.setRadius(loc.getRadius());

		return loc_out;
	}

	/**
	 * Transform from equatorial to ecliptic coordinates.
	 *
	 * @param loc Location object with equatorial coordinates.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object. If the ephemeris type is apparent,
	 *        then the rotation will be done respect to the true ecliptic of
	 *        date.
	 * @return Location object with the ecliptic coordinates.
	 * @throws JPARSECException If the date is invalid.
	 */
	public static LocationElement equatorialToEcliptic(LocationElement loc, TimeElement time, ObserverElement obs,
			EphemerisElement eph) throws JPARSECException
	{
		double JD = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		double equinox = eph.equinox;
		if (equinox == EphemerisElement.EQUINOX_OF_DATE)
			equinox = JD;
		double obliquity = 0.0;
		if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT) {
			obliquity = Obliquity.trueObliquity(Functions.toCenturies(equinox), eph);
		} else {
			obliquity = Obliquity.meanObliquity(Functions.toCenturies(equinox), eph);
		}
		RotateFrom rot = new RotateFrom(Constant.PI_OVER_TWO, Constant.PI_OVER_TWO + obliquity, 0.0, loc.getLongitude(), loc
				.getLatitude());

		LocationElement loc_out = rotateFrom(rot);
		loc_out.setRadius(loc.getRadius());

		return loc_out;
	}

	/**
	 * Transform from galactic to equatorial coordinates.
	 *
	 * @param loc Location object with galactic coordinates.
	 * @param epoch Epoch for the output coordinates (Julian day).
	 * @param fast True for an approximate but faster calculation.
	 * @return Location object with the equatorial coordinates referred to the
	 * specified epoch (precessed using IAU 2006 algorithms), and FK5 frame.
	 */
	public static LocationElement galacticToEquatorial(LocationElement loc, double epoch, boolean fast)
	{
		// Rotate
		RotateTo galRot = new RotateTo(GALACTIC_POLE.getLongitude(), GALACTIC_POLE.getLatitude(), GALACTIC_POLE_NODE_J2000, loc.getLongitude(), loc.getLatitude());
		LocationElement loc_out = null;
		if (fast) {
			loc_out = fastRotateTo(galRot);
		} else {
			loc_out = rotateTo(galRot);
		}
		loc_out.setRadius(loc.getRadius());

		if (epoch != Constant.J2000) {
			try {
				EphemerisElement eph = new EphemerisElement();
				eph.ephemMethod = REDUCTION_METHOD.IAU_2006;
				loc_out = LocationElement.parseRectangularCoordinates(Precession.precessFromJ2000(epoch, loc_out.getRectangularCoordinates(), eph));
			} catch (Exception e) {	}
		}

		return loc_out;
	}

	/**
	 * Transform from equatorial to galactic coordinates.
	 *
	 * @param loc Location object with equatorial coordinates referred to FK5 J2000.
	 * @param fast True for an approximate but faster calculation.
	 * @return Location object with the galactic coordinates.
	 */
	public static LocationElement equatorialToGalactic(LocationElement loc, boolean fast)
	{
		// Rotate
		RotateFrom galRot2 = new RotateFrom(GALACTIC_POLE.getLongitude(), GALACTIC_POLE.getLatitude(), GALACTIC_POLE_NODE_J2000, loc.getLongitude(), loc.getLatitude());
		LocationElement loc_out = null;
		if (fast) {
			loc_out = fastRotateFrom(galRot2);
		} else {
			loc_out = rotateFrom(galRot2);
		}
		loc_out.setRadius(loc.getRadius());

		return loc_out;
	}

	/**
	 * Transform from ecliptic to equatorial coordinates.
	 *
	 * @param loc Location object with ecliptic coordinates.
	 * @param obliquity Correct obliquity in equinox and type (true or mean).
	 * @param fast True for an approximate but faster calculation.
	 * @return Location object with the equatorial coordinates.
	 */
	public static LocationElement eclipticToEquatorial(LocationElement loc, double obliquity, boolean fast)
	{
		RotateTo rot = new RotateTo(-Constant.PI_OVER_TWO, Constant.PI_OVER_TWO - obliquity, 0.0,
				loc.getLongitude(), loc.getLatitude());

		LocationElement loc_out = null;
		if (fast) {
			loc_out = fastRotateTo(rot);
		} else {
			loc_out = rotateTo(rot);
		}
		loc_out.setRadius(loc.getRadius());

		return loc_out;
	}

	/**
	 * Transform from equatorial to ecliptic coordinates.
	 *
	 * @param loc Location object with equatorial coordinates.
	 * @param obliquity Correct obliquity in equinox and type (true or mean).
	 * @param fast True for an approximate but faster calculation.
	 * @return Location object with the ecliptic coordinates.
	 */
	public static LocationElement equatorialToEcliptic(LocationElement loc, double obliquity, boolean fast)
	{
		RotateFrom rot = new RotateFrom(Constant.PI_OVER_TWO, Constant.PI_OVER_TWO + obliquity, 0.0, loc.getLongitude(), loc
				.getLatitude());

		LocationElement loc_out = null;
		if (fast) {
			loc_out = fastRotateFrom(rot);
		} else {
			loc_out = rotateFrom(rot);
		}
		loc_out.setRadius(loc.getRadius());

		return loc_out;
	}

	/**
	 * Generic 'rotation to'.
	 *
	 * @param rot RotateTo object.
	 * @return Rotated input.
	 */
	private static LocationElement rotateTo(RotateTo rot)
	{
		double sind0 = Math.sin(rot.DELTA0), cosd0 = Math.cos(rot.DELTA0), cosy = Math.cos(rot.Y),
				siny = Math.sin(rot.Y), sinxl = Math.sin(rot.X - rot.LON0);

		double sinLat = siny * sind0 + cosy * cosd0 * sinxl;
		double lat = Constant.PI_OVER_TWO * FastMath.sign(sinLat);
		if (Math.abs(sinLat) < 1) lat = Math.asin(sinLat);
		double lon = rot.ALFA0 + Math.atan2(cosy * Math.cos(rot.X - rot.LON0), siny * cosd0
				- cosy * sind0 * sinxl);

		LocationElement loc = new LocationElement(lon, lat, 1.0);
		return loc;
	}

	private static LocationElement fastRotateTo(RotateTo rot)
	{
		double sind0 = FastMath.sin(rot.DELTA0), cosd0 = FastMath.cos(rot.DELTA0), cosy = FastMath.cos(rot.Y),
				siny = FastMath.sin(rot.Y), sinxl = FastMath.sin(rot.X - rot.LON0);
		double sinLat = siny * sind0 + cosy * cosd0 * sinxl;
		double lat = Constant.PI_OVER_TWO * FastMath.sign(sinLat);
		if (Math.abs(sinLat) < 1) lat = FastMath.asin(sinLat);
		double lon = rot.ALFA0 + FastMath.atan2_accurate(cosy * FastMath.cos(rot.X - rot.LON0), siny * cosd0
				- cosy * sind0 * sinxl);

		LocationElement loc = new LocationElement(lon, lat, 1.0);
		return loc;
	}

	/**
	 * Generic 'rotation from'.
	 *
	 * @param rot RotateFrom object.
	 * @return Rotated input.
	 */
	private static LocationElement rotateFrom(RotateFrom rot)
	{
		double sind0 = Math.sin(rot.DELTA0), cosd0 = Math.cos(rot.DELTA0), cosd = Math.cos(rot.DELTA),
				sind = Math.sin(rot.DELTA);
		double y = sind * sind0 + cosd * cosd0 * Math.cos(rot.ALFA - rot.ALFA0);
		double x = rot.LON0 + Math.atan2(sind - y * sind0, cosd * cosd0 * Math.sin(rot.ALFA - rot.ALFA0));
		double lat = Constant.PI_OVER_TWO * FastMath.sign(y);
		if (Math.abs(y) < 1) lat = Math.asin(y);
		y = lat;

		LocationElement loc = new LocationElement(x, y, 1.0);
		return loc;
	}

	private static LocationElement fastRotateFrom(RotateFrom rot)
	{
		double sind0 = FastMath.sin(rot.DELTA0), cosd0 = FastMath.cos(rot.DELTA0), cosd = FastMath.cos(rot.DELTA),
				sind = FastMath.sin(rot.DELTA);
		double y = sind * sind0 + cosd * cosd0 * FastMath.cos(rot.ALFA - rot.ALFA0);
		double x = rot.LON0 + FastMath.atan2_accurate(sind - y * sind0, cosd * cosd0 * FastMath.sin(rot.ALFA - rot.ALFA0));
		double lat = Constant.PI_OVER_TWO * FastMath.sign(y);
		if (Math.abs(y) < 1) lat = FastMath.asin(y);
		y = lat;

		LocationElement loc = new LocationElement(x, y, 1.0);
		return loc;
	}

	/**
	 * The set of available coordinate systems.
	 */
	public enum COORDINATE_SYSTEM {
		/** Symbolic constant for equatorial coordinates. */
		EQUATORIAL,
		/** Symbolic constant for galactic coordinates. */
		GALACTIC,
		/** Symbolic constant for ecliptic coordinates. */
		ECLIPTIC,
		/** Symbolic constant for horizontal coordinates. */
		HORIZONTAL,
		/** Symbolic constant for supergalactic coordinates. */
		SUPERGALACTIC
	}
	// DO NOT CHANGE ORDERING (See LMVCube class)
	/**
	 * The coordinate system list array.
	 */
	public static final String COORDINATE_SYSTEMS[] = new String[] {
		"Equatorial", "Galactical", "Ecliptical", "Horizontal", "Supergalactical"
	};
	/**
	 * Transforms coordinates.
	 * @param input Input type, as define in the id constants.
	 * @param output Output type.
	 * @param loc Input coordinates.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @return Output coordinates.
	 * @throws JPARSECException If an error occurs.
	 */
	public static LocationElement transform(COORDINATE_SYSTEM input, COORDINATE_SYSTEM output, LocationElement loc,
			TimeElement time, ObserverElement obs, EphemerisElement eph)
	throws JPARSECException {
		LocationElement out = loc;
		if (input == COORDINATE_SYSTEM.EQUATORIAL) {
			if (output == COORDINATE_SYSTEM.ECLIPTIC) out = CoordinateSystem.equatorialToEcliptic(loc, time, obs, eph);
			if (output == COORDINATE_SYSTEM.HORIZONTAL) out = CoordinateSystem.equatorialToHorizontal(loc, time, obs, eph);
			if (output == COORDINATE_SYSTEM.GALACTIC) out = CoordinateSystem.equatorialToGalactic(loc, time, obs, eph);
		}
		if (input == COORDINATE_SYSTEM.ECLIPTIC) {
			out = CoordinateSystem.eclipticToEquatorial(loc, time, obs, eph);
			if (output == COORDINATE_SYSTEM.HORIZONTAL) out = CoordinateSystem.equatorialToHorizontal(loc, time, obs, eph);
			if (output == COORDINATE_SYSTEM.GALACTIC) out = CoordinateSystem.equatorialToGalactic(loc, time, obs, eph);
		}
		if (input == COORDINATE_SYSTEM.HORIZONTAL) {
			out = CoordinateSystem.horizontalToEquatorial(loc, time, obs, eph);
			if (output == COORDINATE_SYSTEM.ECLIPTIC) out = CoordinateSystem.equatorialToEcliptic(loc, time, obs, eph);
			if (output == COORDINATE_SYSTEM.GALACTIC) out = CoordinateSystem.equatorialToGalactic(loc, time, obs, eph);
		}
		if (input == COORDINATE_SYSTEM.GALACTIC) {
			out = CoordinateSystem.galacticToEquatorial(loc, time, obs, eph);
			if (output == COORDINATE_SYSTEM.ECLIPTIC) out = CoordinateSystem.equatorialToEcliptic(loc, time, obs, eph);
			if (output == COORDINATE_SYSTEM.HORIZONTAL) out = CoordinateSystem.equatorialToHorizontal(loc, time, obs, eph);
		}
		if (input == COORDINATE_SYSTEM.SUPERGALACTIC) {
			out = CoordinateSystem.supergalacticToGalactic(loc);
			out = CoordinateSystem.galacticToEquatorial(out, time, obs, eph);
			if (output == COORDINATE_SYSTEM.ECLIPTIC) out = CoordinateSystem.equatorialToEcliptic(loc, time, obs, eph);
			if (output == COORDINATE_SYSTEM.HORIZONTAL) out = CoordinateSystem.equatorialToHorizontal(loc, time, obs, eph);
		}
		return out;
	}
}

//A RotateTo term
class RotateTo
{
	RotateTo(double alfa0, double delta0, double l0, double x, double y)
	{
		ALFA0 = alfa0;
		DELTA0 = delta0;
		LON0 = l0;
		X = x;
		Y = y;
	}

	double ALFA0;
	double DELTA0;
	double LON0;
	double X;
	double Y;
};

// A RotateFrom term
class RotateFrom
{
	RotateFrom(double alfa0, double delta0, double l0, double alfa, double delta)
	{
		ALFA0 = alfa0;
		DELTA0 = delta0;
		LON0 = l0;
		ALFA = alfa;
		DELTA = delta;
	}

	double ALFA0;
	double DELTA0;
	double LON0;
	double ALFA;
	double DELTA;
};
