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

import jparsec.ephem.EphemerisElement.COORDINATES_TYPE;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.planets.EphemElement;
import jparsec.graph.DataSet;
import jparsec.math.Constant;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.AstroDate;
import jparsec.time.TimeScale;
import jparsec.time.SiderealTime;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.util.JPARSECException;

/**
 * Provides methods for calculation of the rise, set and transit instants. These
 * events can be obtained referred to any of the predefined twilight events.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class RiseSetTransit
{
	// private constructor so that this class cannot be instantiated.
	private RiseSetTransit() {}

	/**
	 * Constant ID for a circumpolar object, which has no rise or set times.
	 * Please note that for astronomical twilights (for instance) calculations,
	 * this would mean that the object never reaches a geometric elevation
	 * of -18 degrees, but could have other rise/set events like the usual
	 * astronomical one.
	 */
	public static final int CIRCUMPOLAR = -1000000000;

	/**
	 * Constant ID for a never visible object, which has no rise or set times.
	 */
	public static final int ALWAYS_BELOW_HORIZON = -1000000001;

	/**
	 * This value is used for objects that have no rise, set, or transit
	 * in a given date in local time, when the events calculated are those
	 * referred to current date.
	 */
	public static final int NO_RISE_SET_TRANSIT =  -1000000002;

	/**
	 * The set of twilights to calculate (types of rise/set events).
	 */
	public enum TWILIGHT {
		/**
		 * Event ID for calculation of rising and setting times for astronomical
		 * twilight. In this case, the calculated time will be the time when the
		 * center of the object is at -18 degrees of geometrical elevation below the
		 * astronomical horizon. At this time astronomical observations are possible
		 * because the sky is dark enough.
		 */
		TWILIGHT_ASTRONOMICAL,
		/**
		 * Event ID for calculation of rising and setting times for nautical
		 * twilight. In this case, the calculated time will be the time when the
		 * center of the object is at -12 degrees of geometric elevation below the
		 * astronomical horizon.
		 */
		TWILIGHT_NAUTICAL,
		/**
		 * Event ID for calculation of rising and setting times for civil twilight.
		 * In this case, the calculated time will be the time when the center of the
		 * object is at -6 degrees of geometric elevation below the astronomical
		 * horizon.
		 */
		TWILIGHT_CIVIL,
		/**
		 * Event ID for calculation of rising and setting times for the local
		 * horizon. In this case, the calculated time will be the time when the
		 * upper edge of the object is at -32.67 arcminutes of geometric elevation
		 * (0.0 of apparent elevation) below the horizon, including the effects of
		 * the depression of the horizon.
		 * <P>
		 * Note 32.67' is the refraction in the horizon following the Astronomical
		 * Almanac 1986, where an algorithm to correct from geometric to apparent
		 * elevation is given. Since we use that algorithm, this value is provided here for
		 * consistency, instead of different values used by other authors, which are
		 * near 34'. In any case, this value depends on the atmospheric conditions,
		 * and the difference is below 10s in the time of the events.
		 */
		HORIZON_ASTRONOMICAL,
		/**
		 * The standard value of 34' for the refraction at the horizon. This is the value
		 * used for calculations in {@linkplain Ephem} class. The effects of
		 * the depression of the horizon are not considered when using this constant.
		 */
		HORIZON_ASTRONOMICAL_34arcmin,
		/**
		 * ID value for a custom 'rise' and 'set', setting the value for the horizon
		 * elevation.
		 */
		CUSTOM;

		/** Set the minimum elevation above the horizon in radians to consider the rise and
		 * set of a given object. */
		public double horizonElevation = 0.0;
		/** Set to false to neglect the angular size of the object when using a custom rise and set
		 * calculation. Default value is true. */
		public boolean considerObjectAngularRadius = true;
	}

	/**
	 * The set of possible events.
	 */
	public enum EVENT {
		/** Constant ID for obtaining the transit time only. */
		TRANSIT,
		/** Constant ID for obtaining the rise time only. */
		RISE,
		/** Constant ID for obtaining the set time only. */
		SET,
		/** Constant ID for obtaining rise, set, and transit times. */
		ALL
	}

	/**
	 * Compute next instants of rise or set assuming that the body is static.
	 * Results of rise and set fields could be equal to {@linkplain RiseSetTransit#CIRCUMPOLAR} or
	 * {@linkplain RiseSetTransit#ALWAYS_BELOW_HORIZON} in the corresponding case. Calculations are made
	 * taking into account the refraction in the horizon (about 33'), the size
	 * of the body, and the depression of the horizon, but only
	 * for events in the horizon.
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param ephem Ephem object full of data.
	 * @param twilight_event Twilight/horizon event ID constant. It is the value
	 *        in radians for the altitude of the center of the object where the
	 *        event is to be considered.
	 * @param event Event ID constant for rise, set, transit, or all events.
	 * @return Ephem object.
	 * @throws JPARSECException If the date is invalid.
	 */
	private static EphemElement nextRiseSetTransit(TimeElement time, ObserverElement obs, EphemerisElement eph,
			EphemElement ephem, TWILIGHT twilight_event, EVENT event) throws JPARSECException
	{
		int indexR = ephem.rise.length - 1;
		int indexS = ephem.set.length - 1;
		int indexT = ephem.transit.length - 1;

		// Take into account the angular radius and the depression of the horizon
		// for astronomical horizon events
		double tmp = 0.0;
		switch (twilight_event) {
		case TWILIGHT_ASTRONOMICAL:
			tmp = -18.0 * Constant.DEG_TO_RAD;
			break;
		case TWILIGHT_NAUTICAL:
			tmp = -12.0 * Constant.DEG_TO_RAD;
			break;
		case TWILIGHT_CIVIL:
			tmp = -6.0 * Constant.DEG_TO_RAD;
			break;
		case HORIZON_ASTRONOMICAL:
			tmp = -(32.67 / 60.0) * Constant.DEG_TO_RAD - ephem.angularRadius - horizonDepression(obs, eph);
			if (obs.getMotherBody() != TARGET.EARTH) tmp = - ephem.angularRadius - horizonDepression(obs, eph);
			break;
		case HORIZON_ASTRONOMICAL_34arcmin:
			tmp = -(34.0 / 60.0) * Constant.DEG_TO_RAD - ephem.angularRadius;
			if (obs.getMotherBody() != TARGET.EARTH) tmp = - ephem.angularRadius;
			break;
		case CUSTOM:
			tmp = twilight_event.horizonElevation;
			if (twilight_event.considerObjectAngularRadius) tmp -= ephem.angularRadius;
			break;
		}

		// Compute cosine of hour angle
		tmp = (Math.sin(tmp) - Math.sin(obs.getLatitudeRad()) * Math.sin(ephem.declination)) / (Math.cos(obs.getLatitudeRad()) * Math
				.cos(ephem.declination));

		// Compute local apparent sidereal time and julian day
		double sidereal_time = SiderealTime.apparentSiderealTime(time, obs, eph);
		//double jd = TimeScale.getJD(time, obs, eph, SCALE.LOCAL_TIME);
		double jd_TDB = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		double celestialHoursToEarthTime = Constant.RAD_TO_DAY / Constant.SIDEREAL_DAY_LENGTH;
		if (obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != TARGET.NOT_A_PLANET) {
			EphemerisElement ephCopy = eph.clone();
			ephCopy.targetBody = obs.getMotherBody();
			celestialHoursToEarthTime /= (obs.getMotherBodyMeanRotationRate(ephCopy) / Constant.EARTH_MEAN_ROTATION_RATE);
		}

		// Make calculations for the meridian
		if (event == EVENT.TRANSIT || event == EVENT.ALL)
		{
			double transit_time = celestialHoursToEarthTime * Functions.normalizeRadians(ephem.rightAscension - sidereal_time);

			double transit_alt = Math.asin(Math.sin(ephem.declination) * Math.sin(obs.getLatitudeRad()) + Math
					.cos(ephem.declination) * Math.cos(obs.getLatitudeRad()));
			ephem.transit[indexT] = jd_TDB + transit_time;
			ephem.transitElevation[indexT] = (float) transit_alt;
			if (eph.ephemType == COORDINATES_TYPE.APPARENT) ephem.transitElevation[indexT] = (float) Ephem.getApparentElevation(eph, obs, transit_alt, 30);

			if (event == EVENT.TRANSIT) return ephem;
		}

		// Make calculations for rise and set
		if (tmp > 1.0)
		{
			if (event == EVENT.RISE || event == EVENT.ALL)
			{
				ephem.rise[indexR] = RiseSetTransit.ALWAYS_BELOW_HORIZON;
			}
			if (event == EVENT.SET || event == EVENT.ALL)
			{
				ephem.set[indexS] = RiseSetTransit.ALWAYS_BELOW_HORIZON;
			}
		}
		if (tmp < -1.0)
		{
			if (event == EVENT.RISE || event == EVENT.ALL)
			{
				ephem.rise[indexR] = RiseSetTransit.CIRCUMPOLAR;
			}
			if (event == EVENT.SET || event == EVENT.ALL)
			{
				ephem.set[indexS] = RiseSetTransit.CIRCUMPOLAR;
			}
		}
		if (Math.abs(tmp) <= 1.0)
		{
			double ang_hor = Math.acos(tmp);
			double rise_time = celestialHoursToEarthTime * Functions.normalizeRadians(ephem.rightAscension - ang_hor - sidereal_time);
			double set_time = celestialHoursToEarthTime * Functions.normalizeRadians(ephem.rightAscension + ang_hor - sidereal_time);

			if (event == EVENT.RISE || event == EVENT.ALL)
			{
				ephem.rise[indexR] = jd_TDB + rise_time;
			}
			if (event == EVENT.SET || event == EVENT.ALL)
			{
				ephem.set[indexS] = jd_TDB + set_time;
			}
		}

		return ephem;
	}

	/**
	 * Compute previous instants of rise or set assuming that the body is static.
	 * Results of rise and set fields could be equal to {@linkplain RiseSetTransit#CIRCUMPOLAR} or
	 * {@linkplain RiseSetTransit#ALWAYS_BELOW_HORIZON} in the corresponding case. Calculations are made
	 * taking into account the refraction in the horizon (about 33'), the size
	 * of the body, and the depression of the horizon, but only
	 * for events in the horizon.
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param ephem Ephem object full of data.
	 * @param twilight_event Twilight/horizon event ID constant. It is the value
	 *        in radians for the altitude of the center of the object where the
	 *        event is to be considered.
	 * @param event Event ID constant for rise, set, transit, or all events.
	 * @return Ephem object.
	 * @throws JPARSECException If the date is invalid.
	 */
	private static EphemElement previousRiseSetTransit(TimeElement time, ObserverElement obs, EphemerisElement eph,
			EphemElement ephem, TWILIGHT twilight_event, EVENT event) throws JPARSECException
	{
		int index = ephem.rise.length - 1;

		// Take into account the angular radius and the depression of the horizon
		// for astronomical horizon events
		double tmp = 0.0;
		switch (twilight_event) {
		case TWILIGHT_ASTRONOMICAL:
			tmp = -18.0 * Constant.DEG_TO_RAD;
			break;
		case TWILIGHT_NAUTICAL:
			tmp = -12.0 * Constant.DEG_TO_RAD;
			break;
		case TWILIGHT_CIVIL:
			tmp = -6.0 * Constant.DEG_TO_RAD;
			break;
		case HORIZON_ASTRONOMICAL:
			tmp = -(32.67 / 60.0) * Constant.DEG_TO_RAD - ephem.angularRadius - horizonDepression(obs, eph);
			if (obs.getMotherBody() != TARGET.EARTH) tmp = - ephem.angularRadius - horizonDepression(obs, eph);
			break;
		case HORIZON_ASTRONOMICAL_34arcmin:
			tmp = -(34.0 / 60.0) * Constant.DEG_TO_RAD - ephem.angularRadius;
			if (obs.getMotherBody() != TARGET.EARTH) tmp = - ephem.angularRadius;
			break;
		case CUSTOM:
			tmp = twilight_event.horizonElevation;
			if (twilight_event.considerObjectAngularRadius) tmp -= ephem.angularRadius;
			break;
		}

		// Compute cosine of hour angle
		tmp = (Math.sin(tmp) - Math.sin(obs.getLatitudeRad()) * Math.sin(ephem.declination)) / (Math.cos(obs.getLatitudeRad()) * Math
				.cos(ephem.declination));

		// Compute local apparent sidereal time and julian day
		double sidereal_time = SiderealTime.apparentSiderealTime(time, obs, eph);
		//double jd = TimeScale.getJD(time, obs, eph, SCALE.LOCAL_TIME);
		double jd_TDB = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		double celestialHoursToEarthTime = Constant.RAD_TO_DAY / Constant.SIDEREAL_DAY_LENGTH;
		if (obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != TARGET.NOT_A_PLANET) {
			EphemerisElement ephCopy = eph.clone();
			ephCopy.targetBody = obs.getMotherBody();
			celestialHoursToEarthTime /= (obs.getMotherBodyMeanRotationRate(ephCopy) / Constant.EARTH_MEAN_ROTATION_RATE);
		}

		// Make calculations for the meridian
		if (event == EVENT.TRANSIT || event == EVENT.ALL)
		{
			double transit_time = celestialHoursToEarthTime * (Functions.normalizeRadians(ephem.rightAscension - sidereal_time) - Constant.TWO_PI);

			double transit_alt = Math.asin(Math.sin(ephem.declination) * Math.sin(obs.getLatitudeRad()) + Math
					.cos(ephem.declination) * Math.cos(obs.getLatitudeRad()));
			ephem.transit[index] = jd_TDB + transit_time;
			ephem.transitElevation[index] = (float) transit_alt;
			if (eph.ephemType == COORDINATES_TYPE.APPARENT) ephem.transitElevation[index] = (float) Ephem.getApparentElevation(eph, obs, transit_alt, 30);

			if (event == EVENT.TRANSIT) return ephem;
		}

		// Make calculations for rise and set
		if (tmp > 1.0)
		{
			if (event == EVENT.RISE || event == EVENT.ALL)
			{
				ephem.rise[index] = RiseSetTransit.ALWAYS_BELOW_HORIZON;
			}
			if (event == EVENT.SET || event == EVENT.ALL)
			{
				ephem.set[index] = RiseSetTransit.ALWAYS_BELOW_HORIZON;
			}
		}
		if (tmp < -1.0)
		{
			if (event == EVENT.RISE || event == EVENT.ALL)
			{
				ephem.rise[index] = RiseSetTransit.CIRCUMPOLAR;
			}
			if (event == EVENT.SET || event == EVENT.ALL)
			{
				ephem.set[index] = RiseSetTransit.CIRCUMPOLAR;
			}
		}
		if (Math.abs(tmp) <= 1.0)
		{
			double ang_hor = Math.acos(tmp);
			double rise_time = celestialHoursToEarthTime * (Functions.normalizeRadians(ephem.rightAscension - ang_hor - sidereal_time) - Constant.TWO_PI);
			double set_time = celestialHoursToEarthTime * (Functions.normalizeRadians(ephem.rightAscension + ang_hor - sidereal_time) - Constant.TWO_PI);

			if (event == EVENT.RISE || event == EVENT.ALL)
			{
				ephem.rise[index] = jd_TDB + rise_time;
			}
			if (event == EVENT.SET || event == EVENT.ALL)
			{
				ephem.set[index] = jd_TDB + set_time;
			}
		}

		return ephem;
	}

	/**
	 * Compute instants of rise or set assuming that the body is static. Results
	 * of rise and set fields could be equal to {@linkplain RiseSetTransit#CIRCUMPOLAR} or
	 * {@linkplain RiseSetTransit#ALWAYS_BELOW_HORIZON} in the corresponding case. Calculations for
	 * physical twilight are made taking into account the refraction in the horizon
	 * (about 33'), the size of the body, and the depression of
	 * the horizon, but only for events in the horizon.
	 * <P>
	 * This method provides the nearest events in time. Adequate for objects at
	 * high elevation.
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param ephem Ephem object full of data.
	 * @param twilight_event Twilight/horizon event ID constant. It is the value
	 *        in radians for the altitude of the center of the object where the
	 *        event is to be considered.
	 * @param event Event ID constant for rise, set, transit, or all events.
	 * @return Ephem object.
	 * @throws JPARSECException If the date is invalid.
	 */
	private static EphemElement nearestRiseSetTransit(TimeElement time, ObserverElement obs, EphemerisElement eph,
			EphemElement ephem, TWILIGHT twilight_event, EVENT event) throws JPARSECException
	{
		int index = ephem.rise.length - 1;

		// Take into account the angular radius and the depression of the horizon
		// for astronomical horizon events
		double tmp = 0.0;
		switch (twilight_event) {
		case TWILIGHT_ASTRONOMICAL:
			tmp = -18.0 * Constant.DEG_TO_RAD;
			break;
		case TWILIGHT_NAUTICAL:
			tmp = -12.0 * Constant.DEG_TO_RAD;
			break;
		case TWILIGHT_CIVIL:
			tmp = -6.0 * Constant.DEG_TO_RAD;
			break;
		case HORIZON_ASTRONOMICAL:
			tmp = -(32.67 / 60.0) * Constant.DEG_TO_RAD - ephem.angularRadius - horizonDepression(obs, eph);
			if (obs.getMotherBody() != TARGET.EARTH) tmp = - ephem.angularRadius - horizonDepression(obs, eph);
			break;
		case HORIZON_ASTRONOMICAL_34arcmin:
			tmp = -(34.0 / 60.0) * Constant.DEG_TO_RAD - ephem.angularRadius;
			if (obs.getMotherBody() != TARGET.EARTH) tmp = - ephem.angularRadius;
			break;
		case CUSTOM:
			tmp = twilight_event.horizonElevation;
			if (twilight_event.considerObjectAngularRadius) tmp -= ephem.angularRadius;
			break;
		}

		// Compute cosine of hour angle
		tmp = (Math.sin(tmp) - Math.sin(obs.getLatitudeRad()) * Math.sin(ephem.declination)) / (Math.cos(obs.getLatitudeRad()) * Math
				.cos(ephem.declination));

		// Compute local apparent sidereal time and julian day
		double sidereal_time = SiderealTime.apparentSiderealTime(time, obs, eph);
		//double jd = TimeScale.getJD(time, obs, eph, SCALE.LOCAL_TIME);
		double jd_TDB = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		double celestialHoursToEarthTime = Constant.RAD_TO_DAY / Constant.SIDEREAL_DAY_LENGTH;
		if (obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != TARGET.NOT_A_PLANET) {
			EphemerisElement ephCopy = eph.clone();
			ephCopy.targetBody = obs.getMotherBody();
			celestialHoursToEarthTime /= (obs.getMotherBodyMeanRotationRate(ephCopy) / Constant.EARTH_MEAN_ROTATION_RATE);
		}

		// Make calculations for the meridian
		if (event == EVENT.TRANSIT || event == EVENT.ALL)
		{
			double transit_time1 = celestialHoursToEarthTime * Functions.normalizeRadians(ephem.rightAscension - sidereal_time);
			double transit_time2 = celestialHoursToEarthTime * (Functions.normalizeRadians(ephem.rightAscension - sidereal_time) - Constant.TWO_PI);
			double transit_alt = Math.asin(Math.sin(ephem.declination) * Math.sin(obs.getLatitudeRad()) + Math
					.cos(ephem.declination) * Math.cos(obs.getLatitudeRad()));

			// Obtain the nearest event in time
			double transit_time = transit_time1;
			if (Math.abs(transit_time2) < Math.abs(transit_time1)) transit_time = transit_time2;

			ephem.transit[index] = jd_TDB + transit_time;
			ephem.transitElevation[index] = (float) transit_alt;
			if (eph.ephemType == COORDINATES_TYPE.APPARENT) ephem.transitElevation[index] = (float) Ephem.getApparentElevation(eph, obs, transit_alt, 30);

			if (event == EVENT.TRANSIT) return ephem;
		}

		// Make calculations for rise and set
		if (tmp > 1.0)
		{
			if (event == EVENT.RISE || event == EVENT.ALL)
			{
				ephem.rise[index] = RiseSetTransit.ALWAYS_BELOW_HORIZON;
			}
			if (event == EVENT.SET || event == EVENT.ALL)
			{
				ephem.set[index] = RiseSetTransit.ALWAYS_BELOW_HORIZON;
			}
		}
		if (tmp < -1.0)
		{
			if (event == EVENT.RISE || event == EVENT.ALL)
			{
				ephem.rise[index] = RiseSetTransit.CIRCUMPOLAR;
			}
			if (event == EVENT.SET || event == EVENT.ALL)
			{
				ephem.set[index] = RiseSetTransit.CIRCUMPOLAR;
			}
		}
		if (Math.abs(tmp) <= 1.0)
		{
			double ang_hor = Math.abs(Math.acos(tmp));
			double rise_time1 = celestialHoursToEarthTime * Functions.normalizeRadians(ephem.rightAscension - ang_hor - sidereal_time);
			double set_time1 = celestialHoursToEarthTime * Functions.normalizeRadians(ephem.rightAscension + ang_hor - sidereal_time);
			double rise_time2 = celestialHoursToEarthTime * (Functions.normalizeRadians(ephem.rightAscension - ang_hor - sidereal_time) - Constant.TWO_PI);
			double set_time2 = celestialHoursToEarthTime * (Functions.normalizeRadians(ephem.rightAscension + ang_hor - sidereal_time) - Constant.TWO_PI);

			// Obtain the nearest event in time
			double rise_time = rise_time1;
			if (Math.abs(rise_time2) < Math.abs(rise_time1)) rise_time = rise_time2;

			// Obtain the nearest event in time
			double set_time = set_time1;
			if (Math.abs(set_time2) < Math.abs(set_time1)) set_time = set_time2;

			if (event == EVENT.RISE || event == EVENT.ALL)
			{
				ephem.rise[index] = jd_TDB + rise_time;
			}
			if (event == EVENT.SET || event == EVENT.ALL)
			{
				ephem.set[index] = jd_TDB + set_time;
			}
		}

		return ephem;
	}

	/**
	 * Compute instants of rise or set assuming that the body is static. Results
	 * of rise and set fields could be equal to {@linkplain RiseSetTransit#CIRCUMPOLAR} or
	 * {@linkplain RiseSetTransit#ALWAYS_BELOW_HORIZON} in the corresponding case. Calculations for
	 * physical twilight are made taking into account the refraction in the horizon
	 * (about 33'), the size of the body, and the depression of
	 * the horizon, but only for events in the horizon.
	 * <P>
	 * This method provides the last rise and nearest transit, and the next set
	 * time. Adequate for objects above the horizon.
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param ephem Ephem object full of data.
	 * @param twilight_event Twilight/horizon event ID constant. It is the value
	 *        in radians for the altitude of the center of the object where the
	 *        event is to be considered.
	 * @param event Event ID constant for rise, set, transit, or all events.
	 * @return Ephem object.
	 * @throws JPARSECException If the date is invalid.
	 */
	private static EphemElement currentRiseSetTransit(TimeElement time, ObserverElement obs, EphemerisElement eph,
			EphemElement ephem, TWILIGHT twilight_event, EVENT event) throws JPARSECException
	{
		int index = ephem.rise.length - 1;

		// Take into account the angular radius and the depression of the horizon
		// for astronomical horizon events
		double tmp = 0.0;
		switch (twilight_event) {
		case TWILIGHT_ASTRONOMICAL:
			tmp = -18.0 * Constant.DEG_TO_RAD;
			break;
		case TWILIGHT_NAUTICAL:
			tmp = -12.0 * Constant.DEG_TO_RAD;
			break;
		case TWILIGHT_CIVIL:
			tmp = -6.0 * Constant.DEG_TO_RAD;
			break;
		case HORIZON_ASTRONOMICAL:
			tmp = -(32.67 / 60.0) * Constant.DEG_TO_RAD - ephem.angularRadius - horizonDepression(obs, eph);
			if (obs.getMotherBody() != TARGET.EARTH) tmp = - ephem.angularRadius - horizonDepression(obs, eph);
			break;
		case HORIZON_ASTRONOMICAL_34arcmin:
			tmp = -(34.0 / 60.0) * Constant.DEG_TO_RAD - ephem.angularRadius;
			if (obs.getMotherBody() != TARGET.EARTH) tmp = - ephem.angularRadius;
			break;
		case CUSTOM:
			tmp = twilight_event.horizonElevation;
			if (twilight_event.considerObjectAngularRadius) tmp -= ephem.angularRadius;
			break;
		}

		// Compute cosine of hour angle
		tmp = (Math.sin(tmp) - Math.sin(obs.getLatitudeRad()) * Math.sin(ephem.declination)) / (Math.cos(obs.getLatitudeRad()) * Math
				.cos(ephem.declination));

		// Compute local apparent sidereal time and Julian day
		double sidereal_time = SiderealTime.apparentSiderealTime(time, obs, eph);
		double jd = TimeScale.getJD(time, obs, eph, SCALE.LOCAL_TIME);
		double jd_TDB = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		double celestialHoursToEarthTime = Constant.RAD_TO_DAY / Constant.SIDEREAL_DAY_LENGTH;
		if (obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != TARGET.NOT_A_PLANET) {
			EphemerisElement ephCopy = eph.clone();
			ephCopy.targetBody = obs.getMotherBody();
			celestialHoursToEarthTime /= (obs.getMotherBodyMeanRotationRate(ephCopy) / Constant.EARTH_MEAN_ROTATION_RATE);
		}

		// Make calculations for the meridian
		if (event == EVENT.TRANSIT || event == EVENT.ALL)
		{
			double transit_time1 = celestialHoursToEarthTime * Functions.normalizeRadians(ephem.rightAscension - sidereal_time);
			double transit_time2 = celestialHoursToEarthTime * (Functions.normalizeRadians(ephem.rightAscension - sidereal_time) - Constant.TWO_PI);
			double transit_alt = Math.asin(Math.sin(ephem.declination) * Math.sin(obs.getLatitudeRad()) + Math
					.cos(ephem.declination) * Math.cos(obs.getLatitudeRad()));

			// Obtain the current event in time
			double transit_time = transit_time1;
			double jdToday = Math.floor(jd - 0.5) + 0.5;
//			double transitToday1 = Math.floor(jd + transit_time1 - 0.5) + 0.5;
			double transitToday2 = Math.floor(jd + transit_time2 - 0.5) + 0.5;
//			if (jdToday == transitToday2 && jdToday != transitToday1) transit_time = transit_time2;
			if (jdToday == transitToday2 && Math.abs(jdToday-(jd+transit_time2)) < Math.abs(jdToday-(jd+transit_time1))) transit_time = transit_time2;
			ephem.transit[index] = jd_TDB + transit_time;

			ephem.transitElevation[index] = (float) transit_alt;
			if (eph.ephemType == COORDINATES_TYPE.APPARENT) ephem.transitElevation[index] = (float) Ephem.getApparentElevation(eph, obs, transit_alt, 30);

			if (event == EVENT.TRANSIT) return ephem;
		}

		// Make calculations for rise and set
		if (tmp > 1.0)
		{
			if (event == EVENT.RISE || event == EVENT.ALL)
			{
				ephem.rise[index] = RiseSetTransit.ALWAYS_BELOW_HORIZON;
			}
			if (event == EVENT.SET || event == EVENT.ALL)
			{
				ephem.set[index] = RiseSetTransit.ALWAYS_BELOW_HORIZON;
			}
		}
		if (tmp < -1.0)
		{
			if (event == EVENT.RISE || event == EVENT.ALL)
			{
				ephem.rise[index] = RiseSetTransit.CIRCUMPOLAR;
			}
			if (event == EVENT.SET || event == EVENT.ALL)
			{
				ephem.set[index] = RiseSetTransit.CIRCUMPOLAR;
			}
		}
		if (Math.abs(tmp) <= 1.0)
		{
			double ang_hor = Math.abs(Math.acos(tmp));
			double rise_time1 = celestialHoursToEarthTime * Functions.normalizeRadians(ephem.rightAscension - ang_hor - sidereal_time);
			double set_time1 = celestialHoursToEarthTime * Functions.normalizeRadians(ephem.rightAscension + ang_hor - sidereal_time);
			double rise_time2 = celestialHoursToEarthTime * (Functions.normalizeRadians(ephem.rightAscension - ang_hor - sidereal_time) - Constant.TWO_PI);
			double set_time2 = celestialHoursToEarthTime * (Functions.normalizeRadians(ephem.rightAscension + ang_hor - sidereal_time) - Constant.TWO_PI);

			// Obtain the current events in time
			double rise_time = rise_time1;
			double jdToday = Math.floor(jd - 0.5) + 0.5;
//			double riseToday1 = Math.floor(jd + rise_time1 - 0.5) + 0.5;
			double riseToday2 = Math.floor(jd + rise_time2 - 0.5) + 0.5;
//			if (jdToday == riseToday2 && jdToday != riseToday1) rise_time = rise_time2;
			if (jdToday == riseToday2 && Math.abs(jdToday-(jd+rise_time2)) < Math.abs(jdToday-(jd+rise_time1))) rise_time = rise_time2;

			double set_time = set_time1;
//			double setToday1 = Math.floor(jd + set_time1 - 0.5) + 0.5;
			double setToday2 = Math.floor(jd + set_time2 - 0.5) + 0.5;
//			if (jdToday == setToday2 && jdToday != setToday1) set_time = set_time2;
			if (jdToday == setToday2 && Math.abs(jdToday-(jd+set_time2)) < Math.abs(jdToday-(jd+set_time1))) set_time = set_time2;

			if (event == EVENT.RISE || event == EVENT.ALL)
			{
				ephem.rise[index] = jd_TDB + rise_time;
			}
			if (event == EVENT.SET || event == EVENT.ALL)
			{
				ephem.set[index] = jd_TDB + set_time;
			}
		}

		return ephem;
	}


	/**
	 * Gets the angle of depression of the horizon. An object will be just in
	 * the geometric horizon when it's elevation is equal to minus this value.
	 * This correction can modify the time of the events by some minutes.
	 *
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @return The angle in radians.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public static double horizonDepression(ObserverElement obs, EphemerisElement eph) throws JPARSECException
	{
		if (obs.getMotherBody() == null || obs.getMotherBody() == TARGET.NOT_A_PLANET) return 0; // FIXME
			//throw new JPARSECException("The observer must be on some Solar System body.");
		double ratio = (obs.getEllipsoid().getPolarRadius() / obs.getEllipsoid().getEquatorialRadius())*0.5;
		double rho = obs.getEllipsoid().getEquatorialRadius() * (1.0-ratio + ratio * Math.cos(2.0 * obs.getLatitudeRad()));
		double depresion = Math.acos(Math.sqrt(rho / (rho + (double) obs.getHeight() / 1000.0)));

		return depresion;
	}

	/**
	 * Provides rise, set, and transit times correcting for the movement of the
	 * object. For objects above the horizon, the result is refered to the
	 * current day, otherwise they will be the next rise, set, and transit
	 * events.
	 *
	 * @param time Time object containing the date.
	 * @param obs Observer object containing the observer position.
	 * @param eph Ephemeris object defining the ephemeris properties.
	 * @param ephem_elem Ephem object with the position of the source.
	 * @param twilight_event Twilight/horizon event ID to calculate.
	 * @return Ephem object containing full ephemeris data. The rise, set, transit
	 * times will be given in the corresponding fields as Julian days, which could
	 * have special values given as constants in this class if the object is
	 * circumpolar or is never above the horizon.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public static EphemElement obtainCurrentOrNextRiseSetTransit(TimeElement time, // Time Element
			ObserverElement obs, // Observer Element
			EphemerisElement eph, // Ephemeris Element
			EphemElement ephem_elem, TWILIGHT twilight_event) throws JPARSECException
	{
		// Establish the adequate type
		int when = OBTAIN_CURRENT_EVENTS;
		if (ephem_elem.elevation < 0.0)
			when = OBTAIN_NEXT_EVENTS;

		return RiseSetTransit.obtainCertainRiseSetTransit(time, obs, eph, ephem_elem, twilight_event, when);
	}

	/**
	 * Provides next rise, set, and transit times correcting for the movement of
	 * the object.
	 *
	 * @param time Time object containing the date.
	 * @param obs Observer object containing the observer position.
	 * @param eph Ephemeris object defining the ephemeris properties.
	 * @param ephem_elem Ephem object with the position of the source.
	 * @param twilight_event Twilight/horizon event ID to calculate.
	 * @return Ephem object containing full ephemeris data. The rise, set, transit
	 * times will be given in the corresponding fields as Julian days, which could
	 * have special values given as constants in this class if the object is
	 * circumpolar or is never above the horizon.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public static EphemElement obtainNextRiseSetTransit(TimeElement time, // Time
																				// Element
			ObserverElement obs, // Observer Element
			EphemerisElement eph, // Ephemeris Element
			EphemElement ephem_elem, TWILIGHT twilight_event) throws JPARSECException
	{
		return RiseSetTransit.obtainCertainRiseSetTransit(time, obs, eph, ephem_elem, twilight_event, OBTAIN_NEXT_EVENTS);
	}

	/**
	 * Provides current (refered to the actual day) rise, set, and transit times
	 * correcting for the movement of the object.
	 *
	 * @param time Time object containing the date.
	 * @param obs Observer object containing the observer position.
	 * @param eph Ephemeris object defining the ephemeris properties.
	 * @param ephem_elem Ephem object with the position of the source.
	 * @param twilight_event Twilight/horizon event ID to calculate.
	 * @return Ephem object containing full ephemeris data. The rise, set, transit
	 * times will be given in the corresponding fields as Julian days, which could
	 * have special values given as constants in this class if the object is
	 * circumpolar or is never above the horizon.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public static EphemElement obtainCurrentRiseSetTransit(TimeElement time, // Time
																					// Element
			ObserverElement obs, // Observer Element
			EphemerisElement eph, // Ephemeris Element
			EphemElement ephem_elem, TWILIGHT twilight_event) throws JPARSECException
	{
		return RiseSetTransit.obtainCertainRiseSetTransit(time, obs, eph, ephem_elem, twilight_event, OBTAIN_CURRENT_EVENTS);
	}

	/**
	 * Provides previous rise, set, and transit times correcting for the
	 * movement of the object.
	 *
	 * @param time Time object containing the date.
	 * @param obs Observer object containing the observer position.
	 * @param eph Ephemeris object defining the ephemeris properties.
	 * @param ephem_elem Ephem object with the position of the source.
	 * @param twilight_event Twilight/horizon event ID to calculate.
	 * @return Ephem object containing full ephemeris data. The rise, set, transit
	 * times will be given in the corresponding fields as Julian days, which could
	 * have special values given as constants in this class if the object is
	 * circumpolar or is never above the horizon.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public static EphemElement obtainPreviousRiseSetTransit(TimeElement time, // Time
																					// Element
			ObserverElement obs, // Observer Element
			EphemerisElement eph, // Ephemeris Element
			EphemElement ephem_elem, TWILIGHT twilight_event) throws JPARSECException
	{
		return RiseSetTransit.obtainCertainRiseSetTransit(time, obs, eph, ephem_elem, twilight_event, OBTAIN_PREVIOUS_EVENTS);
	}

	/**
	 * Provides nearest rise, set, and transit times correcting for the movement
	 * of the object.
	 *
	 * @param time Time object containing the date.
	 * @param obs Observer object containing the observer position.
	 * @param eph Ephemeris object defining the ephemeris properties.
	 * @param ephem_elem Ephem object with the position of the source.
	 * @param twilight_event Twilight/horizon event ID to calculate.
	 * @return Ephem object containing full ephemeris data. The rise, set, transit
	 * times will be given in the corresponding fields as Julian days, which could
	 * have special values given as constants in this class if the object is
	 * circumpolar or is never above the horizon.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public static EphemElement obtainNearestRiseSetTransit(TimeElement time, // Time
																					// Element
			ObserverElement obs, // Observer Element
			EphemerisElement eph, // Ephemeris Element
			EphemElement ephem_elem, TWILIGHT twilight_event) throws JPARSECException
	{
		return RiseSetTransit.obtainCertainRiseSetTransit(time, obs, eph, ephem_elem, twilight_event, OBTAIN_NEAREST_EVENTS);
	}

	/**
	 * ID constant for next events in {@linkplain RiseSetTransit#obtainCertainRiseSetTransit(TimeElement, ObserverElement, EphemerisElement, EphemElement, TWILIGHT, int)}
	 */
	private static final int OBTAIN_NEXT_EVENTS = 0;
	/**
	 * ID constant for current events in {@linkplain RiseSetTransit#obtainCertainRiseSetTransit(TimeElement, ObserverElement, EphemerisElement, EphemElement, TWILIGHT, int)}
	 */
	private static final int OBTAIN_CURRENT_EVENTS = 1;
	/**
	 * ID constant for previous events in {@linkplain RiseSetTransit#obtainCertainRiseSetTransit(TimeElement, ObserverElement, EphemerisElement, EphemElement, TWILIGHT, int)}.
	 */
	private static final int OBTAIN_PREVIOUS_EVENTS = 2;
	/**
	 * ID constant for nearest events in {@linkplain RiseSetTransit#obtainCertainRiseSetTransit(TimeElement, ObserverElement, EphemerisElement, EphemElement, TWILIGHT, int)}.
	 */
	private static final int OBTAIN_NEAREST_EVENTS = 3;

	/**
	 * Provides any rise, set, and transit times correcting for the movement
	 * of the object.
	 *
	 * @param time Time object containing the date.
	 * @param obs Observer object containing the observer position.
	 * @param eph Ephemeris object defining the ephemeris properties.
	 * @param ephem_obj Ephem object with the position of the source.
	 * @param twilight_event Twilight/horizon event ID to calculate.
	 * @param how Set what kind of calculations will be made.
	 * @return Ephem object containing full ephemeris data.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	private static EphemElement obtainCertainRiseSetTransit(TimeElement time, // Time
																			// Element
			ObserverElement obs, // Observer Element
			EphemerisElement eph, // Ephemeris Element
			EphemElement ephem_obj, TWILIGHT twilight_event, int how) throws JPARSECException
	{
		// Return no data for geocentric position
		if (!eph.isTopocentric || obs.getMotherBody() == TARGET.NOT_A_PLANET)
			return ephem_obj;

		// Declare new TimeElement
		TimeElement new_time = time.clone();

		// Create new EphemerisElement with adequate input values. Force
		// apparent, equinox of date, and topocentric values
		EphemerisElement eph_new = eph.clone();
		eph_new.ephemType = EphemerisElement.COORDINATES_TYPE.APPARENT;
		eph_new.equinox = EphemerisElement.EQUINOX_OF_DATE;
		eph_new.isTopocentric = EphemerisElement.TOPOCENTRIC;

		// Obtain event to better than 0.5 seconds of precision
		double precision_in_seconds = 0.5;

		EphemElement ephem_elem = ephem_obj.clone();

		// Update input ephem object in case, for instance, it was calculated for J2000 equinox,
		// which would produce incorrect rise/set times
		if (!eph_new.equals(eph))  {
			if (eph.algorithm == null) {
				// Correct at least for precession for unknown ephem type
				double jd = TimeScale.getJD(new_time, obs, eph_new, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
				double refJD = eph.getEpoch(jd);
				if (jd != refJD) {
					LocationElement newLoc = ephem_elem.getEquatorialLocation();
					newLoc = LocationElement.parseRectangularCoordinates(Precession.precess(refJD, jd, LocationElement.parseLocationElement(newLoc), eph));
					ephem_elem.rightAscension = newLoc.getLongitude();
					ephem_elem.declination = newLoc.getLatitude();
				}
			} else {
				ephem_elem = Ephem.getEphemerisResult(new_time, obs, eph_new, false, false);
			}
		}

		int indexR = 0, indexT = 0, indexS = 0;
		if (ephem_elem.rise == null) {
			ephem_elem.rise = new double[] {0.0};
		} else {
			indexR = ephem_elem.rise.length;
			ephem_elem.rise = DataSet.addDoubleArray(ephem_elem.rise, new double[] {0.0});
		}
		if (ephem_elem.set == null) {
			ephem_elem.set = new double[] {0.0};
		} else {
			indexS = ephem_elem.set.length;
			ephem_elem.set = DataSet.addDoubleArray(ephem_elem.set, new double[] {0.0});
		}
		if (ephem_elem.transit == null) {
			ephem_elem.transit = new double[] {0.0};
			ephem_elem.transitElevation = new float[] {0.0f};
		} else {
			indexT = ephem_elem.transit.length;
			ephem_elem.transit = DataSet.addDoubleArray(ephem_elem.transit, new double[] {0.0});
			ephem_elem.transitElevation = DataSet.addFloatArray(ephem_elem.transitElevation, new float[] {0.0f});
		}

		// Initial set up
		int not_yet_calculated = -1;
		ephem_elem.rise[indexR] = not_yet_calculated;
		ephem_elem.set[indexS] = not_yet_calculated;
		ephem_elem.transit[indexT] = not_yet_calculated;
		double last_time_event;
		double time_event;
		double dt;

		// Calculate time of events
		for (int i = EVENT.TRANSIT.ordinal(); i <= EVENT.SET.ordinal(); i++)
		{
			// Declare new Ephem object to work with it in the
			// calculation process
			EphemElement new_ephem_elem = ephem_elem.clone();

			boolean triedBefore = false;

			// Set maximum iterations to 20. Enough for general use.
			int n_iter = 0;
			int n_iter_max = 20;
			last_time_event = not_yet_calculated;
			dt = not_yet_calculated;
			do
			{
				n_iter++;
				switch (how)
				{
				case OBTAIN_NEXT_EVENTS: // If the object is initially below the horizon
					new_ephem_elem = RiseSetTransit.nextRiseSetTransit(time, obs, eph_new, new_ephem_elem,
							twilight_event, EVENT.values()[i]);
					break;
				case OBTAIN_CURRENT_EVENTS: // If the object is initially above the horizon
					new_ephem_elem = RiseSetTransit.currentRiseSetTransit(time, obs, eph_new, new_ephem_elem,
							twilight_event, EVENT.values()[i]);
					break;
				case OBTAIN_PREVIOUS_EVENTS:
					new_ephem_elem = RiseSetTransit.previousRiseSetTransit(time, obs, eph_new, new_ephem_elem,
							twilight_event, EVENT.values()[i]);
					break;
				case OBTAIN_NEAREST_EVENTS:
					new_ephem_elem = RiseSetTransit.nearestRiseSetTransit(time, obs, eph_new, new_ephem_elem,
							twilight_event, EVENT.values()[i]);
					break;
				}

				// Get time of the event
				time_event = new_ephem_elem.rise[indexR];
				if (i == EVENT.TRANSIT.ordinal())
					time_event = new_ephem_elem.transit[indexT];
				if (i == EVENT.SET.ordinal())
					time_event = new_ephem_elem.set[indexS];

				// Get elapsed time since last calculation
				dt = time_event - last_time_event;

				// Set elapsed time to zero if the object cannot be observed
				if (time_event == RiseSetTransit.ALWAYS_BELOW_HORIZON || time_event == RiseSetTransit.CIRCUMPOLAR)
					dt = 0.0;

				// Set elapsed time to zero if the object is not moving
				if (eph.algorithm == null || eph.algorithm == EphemerisElement.ALGORITHM.STAR) {
					dt = 0.0;
				}

				// If elapsed time is greater than the desired precision,
				// update time and calculate ephemeris for the new time
				if (Math.abs(dt) > (precision_in_seconds / Constant.SECONDS_PER_DAY) && n_iter < n_iter_max)
				{
					last_time_event = time_event;
					AstroDate new_astro = new AstroDate(time_event);
					new_time = new TimeElement(new_astro, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
					EphemElement new_ephem_elem2 = Ephem.getEphemerisResult(new_time, obs, eph_new, false, false);
					new_ephem_elem2.rise = new_ephem_elem.rise.clone();
					new_ephem_elem2.set = new_ephem_elem.set.clone();
					new_ephem_elem2.transit = new_ephem_elem.transit.clone();
					new_ephem_elem2.transitElevation = new_ephem_elem.transitElevation.clone();
					new_ephem_elem = new_ephem_elem2;
				}

				// If the objects 'gets' circumpolar or never rises then try
				// to continue. Mainly for comets/NEOs (and the Moon in
				// extreme cases).
				if ((time_event == RiseSetTransit.ALWAYS_BELOW_HORIZON || time_event == RiseSetTransit.CIRCUMPOLAR) &&
						eph.algorithm != null && eph.algorithm != EphemerisElement.ALGORITHM.STAR && (i == EVENT.RISE.ordinal() || i == EVENT.SET.ordinal())) {
					dt = 2.0 * precision_in_seconds / Constant.SECONDS_PER_DAY;
					if (triedBefore) {
						dt = not_yet_calculated;
						break;
					} else {
						triedBefore = true;

						// Just go to transit time (close to maximum elevation)
						AstroDate new_astro = new AstroDate(ephem_elem.transit[indexT]);
						new_time = new TimeElement(new_astro, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
						EphemElement new_ephem_elem2 = Ephem.getEphemerisResult(new_time, obs, eph_new, false, false);
						new_ephem_elem2.rise = new_ephem_elem.rise.clone();
						new_ephem_elem2.set = new_ephem_elem.set.clone();
						new_ephem_elem2.transit = new_ephem_elem.transit.clone();
						new_ephem_elem2.transitElevation = new_ephem_elem.transitElevation.clone();
						new_ephem_elem = new_ephem_elem2;
					}
				}

			} while (Math.abs(dt) > (precision_in_seconds / Constant.SECONDS_PER_DAY) && n_iter < n_iter_max);

			// Set time of event in our output Ephem object
			double timeTDB = new_ephem_elem.rise[indexR];
			if (i == EVENT.TRANSIT.ordinal()) timeTDB = new_ephem_elem.transit[indexT];
			if (i == EVENT.SET.ordinal()) timeTDB = new_ephem_elem.set[indexS];
			TimeElement etime = new TimeElement(timeTDB, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
			double timeLT = TimeScale.getJD(etime, obs, eph, SCALE.LOCAL_TIME);

			if (i == EVENT.RISE.ordinal())
				ephem_elem.rise[indexR] = timeLT;
			if (i == EVENT.TRANSIT.ordinal()) {
				ephem_elem.transit[indexT] = timeLT;
				ephem_elem.transitElevation[indexT] = new_ephem_elem.transitElevation[indexT];
			}
			if (i == EVENT.SET.ordinal())
				ephem_elem.set[indexS] = timeLT;

			if (n_iter == n_iter_max || dt == not_yet_calculated) {
				if (i == EVENT.RISE.ordinal()) ephem_elem.rise[indexR] = NO_RISE_SET_TRANSIT;
				if (i == EVENT.SET.ordinal()) ephem_elem.set[indexS] = NO_RISE_SET_TRANSIT;
				if (i == EVENT.TRANSIT.ordinal()) {
					ephem_elem.transit[indexT] = NO_RISE_SET_TRANSIT;
					ephem_elem.transitElevation[indexT] = 0.0f;
				}
			}
			if (how == OBTAIN_CURRENT_EVENTS && (n_iter != n_iter_max && dt != not_yet_calculated)) {
				double jd = TimeScale.getJD(time, obs, eph, SCALE.LOCAL_TIME);
				double jdToday = Math.floor(jd - 0.5) + 0.5;
				if (i == EVENT.RISE.ordinal()) {
					double jdEventToday = Math.floor(ephem_elem.rise[indexR] - 0.5) + 0.5;
					if (jdToday != jdEventToday) ephem_elem.rise[indexR] = NO_RISE_SET_TRANSIT;
				}
				if (i == EVENT.SET.ordinal()) {
					double jdEventToday = Math.floor(ephem_elem.set[indexS] - 0.5) + 0.5;
					if (jdToday != jdEventToday) ephem_elem.set[indexS] = NO_RISE_SET_TRANSIT;
				}
				if (i == EVENT.TRANSIT.ordinal()) {
					double jdEventToday = Math.floor(ephem_elem.transit[indexT] - 0.5) + 0.5;
					if (jdToday != jdEventToday) {
						ephem_elem.transit[indexT] = NO_RISE_SET_TRANSIT;
						ephem_elem.transitElevation[indexT] = 0.0f;
					}
				}
			}
		}
		ephem_obj.rise = ephem_elem.rise;
		ephem_obj.set = ephem_elem.set;
		ephem_obj.transit = ephem_elem.transit;
		ephem_obj.transitElevation = ephem_elem.transitElevation;
		return ephem_obj;
	}
	
    /**
     * Transforms an input time object to the closest time when the object passes at maximum or 
     * minimum elevation.
     * @param time Input time object. It will be modified when the method returns.
     * @param observer The observer.
     * @param eph The ephemeris properties containing the input body.
     * @param inferior True for inferior transit (minimum elevation), false for superior transit 
     * (maximum elevation).
     * @return The ephemerides of the object at maximum or minimum elevation.
     * @throws JPARSECException For an invalid date.
     */
    public static EphemElement toTransitTime(TimeElement time, ObserverElement observer, EphemerisElement eph,
    		boolean inferior) throws JPARSECException {
    	double precision = 1.0 / Constant.SECONDS_PER_DAY;
		double celestialHoursToEarthTime = Constant.RAD_TO_DAY / Constant.SIDEREAL_DAY_LENGTH;
		EphemElement ephem = null;
		int iter = 0;
    	while (iter < 10) {
	        ephem = Ephem.getEphemeris(time, observer, eph, false);
			double offFromTransit = Functions.normalizeRadians(ephem.rightAscension - SiderealTime.apparentSiderealTime(time, observer, eph) + 
					(inferior ? Math.PI : 0));
			if (offFromTransit > Math.PI) offFromTransit -= Constant.TWO_PI;
	    	double transitTime = celestialHoursToEarthTime * offFromTransit;
	    	time.add(transitTime);
	    	if (Math.abs(transitTime) < precision) break;
	    	iter ++;
    	}
    	return ephem;
    }
}
