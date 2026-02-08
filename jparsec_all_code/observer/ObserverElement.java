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
package jparsec.observer;

import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.Functions;
import jparsec.ephem.Nutation;
import jparsec.ephem.PhysicalParameters;
import jparsec.ephem.Precession;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.moons.GUST86;
import jparsec.ephem.moons.L1;
import jparsec.ephem.moons.Mars07;
import jparsec.ephem.moons.MoonPhysicalParameters;
import jparsec.ephem.moons.TASS17;
import jparsec.ephem.planets.JPLEphemeris;
import jparsec.ephem.planets.PlanetEphem;
import jparsec.ephem.planets.imcce.Elp2000;
import jparsec.ephem.planets.imcce.Series96;
import jparsec.ephem.planets.imcce.Vsop;
import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.math.Constant;
import jparsec.observer.ReferenceEllipsoid.ELLIPSOID;
import jparsec.time.SiderealTime;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.time.TimeScale;
import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Logger.LEVEL;
import jparsec.util.Translate;
import jparsec.vo.GeneralQuery;

/**
 * An adequate class for storing the position of an observer as a previous step
 * to calculate ephemerides. <BR>
 * This class is suitable for storing data from the city and observatory
 * classes. It contains some methods to parse a {@linkplain CityElement} or an
 * {@linkplain ObservatoryElement} object. It is possible to store atmospheric
 * conditions like pressure and temperature, values that will be used when
 * obtaining the apparent elevation of an object.<BR>
 * A special feature is to set the location of the observer to a given point
 * located in another planet or within the Solar System, using the class
 * {@linkplain ExtraterrestrialObserverElement}.
 *
 * @see CityElement
 * @see ObservatoryElement
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class ObserverElement implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs an empty observer. All values to 0.0 or empty strings.
	 */
	public ObserverElement() {
		name = "";
		longitude = 0.0;
		latitude = 0.0;
		height = 0;
		timeZone = 0.0;
		pressure = DEFAULT_PRESSURE;
		temperature = DEFAULT_TEMPERATURE;
		humidity = DEFAULT_HUMIDITY;
		isAnObservatory = false;
		dstCode = DST_RULE.NONE;
		observerPosition = null;
		ellipsoid = ELLIPSOID.LATEST;
	}

	/**
	 * Constructs a observer by giving the values of the fields. Pressure and
	 * temperature are set to default values.
	 *
	 * @param loc_name Name of the city/observatory, or otherwise the user location.
	 * @param loc_lon Longitude in radians. West negative.
	 * @param loc_lat Latitude in radians.
	 * @param loc_alt Altitude in meters.
	 * @param tz Time zone in hours.
	 */
	public ObserverElement(String loc_name, double loc_lon, double loc_lat,
			int loc_alt, double tz) {
		name = loc_name;
		longitude = loc_lon;
		latitude = loc_lat;
		height = loc_alt;
		timeZone = tz;
		pressure = (int) (DEFAULT_PRESSURE * Math.exp(-loc_alt / 9100.0));
		temperature = DEFAULT_TEMPERATURE;
		humidity = DEFAULT_HUMIDITY;
		isAnObservatory = false;
		observerPosition = null;
		ellipsoid = ELLIPSOID.LATEST;

		try {
			// Set DST information
			CityElement city = City.findNearestCity(new LocationElement(longitude, latitude, 1.0));
			dstCode = Country.getID(city.country).getDSTCode();
			if (name == null || name.equals("")) name = city.name;
		} catch (JPARSECException e) {
			Logger.log(LEVEL.WARNING, "Could not set the DST field for location "+loc_name+".");
			dstCode = DST_RULE.NONE;
		}
	}

	/**
	 * Constructs a observer by giving the values of all the main fields. Pressure and
	 * temperature are set to default values.
	 *
	 * @param loc_name Name of the city/observatory, or otherwise the user location.
	 * @param loc_lon Longitude in radians. West negative.
	 * @param loc_lat Latitude in radians.
	 * @param loc_alt Altitude in meters.
	 * @param tz Time zone in hours.
	 * @param dst DST rule.
	 */
	public ObserverElement(String loc_name, double loc_lon, double loc_lat,
			int loc_alt, double tz, DST_RULE dst) {
		name = loc_name;
		longitude = loc_lon;
		latitude = loc_lat;
		height = loc_alt;
		timeZone = tz;
		pressure = (int) (DEFAULT_PRESSURE * Math.exp(-loc_alt / 9100.0));
		temperature = DEFAULT_TEMPERATURE;
		humidity = DEFAULT_HUMIDITY;
		isAnObservatory = false;
		dstCode = dst;
		observerPosition = null;
		ellipsoid = ELLIPSOID.LATEST;
	}

	/**
	 * Constructs a observer by giving the values of the fields.
	 *
	 * @param loc_name Name of the city/observatory, or otherwise the user location.
	 * @param loc_lon Longitude in radians. West negative.
	 * @param loc_lat Latitude in radians.
	 * @param loc_alt Altitude in meters.
	 * @param tz Time zone in hours.
	 * @param dst The DST rule.
	 * @param pres Pressure in milibars.
	 * @param temp Temperature in Celsius.
	 * @param humi Humidity as a percentage, from 0 to 100.
	 * @param ellipsoid The ellipsoid for the mother body.
	 * @param target The barycentric equatorial vector of the observer (position and
	 * velocity) in J2000 equinox (mean coordinates) for non-Earth centered ephemerides.
	 */
	public ObserverElement(String loc_name, double loc_lon, double loc_lat,
			int loc_alt, double tz, DST_RULE dst, int pres, int temp, int humi,
			ELLIPSOID ellipsoid, double[] target) {
		name = loc_name;
		longitude = loc_lon;
		latitude = loc_lat;
		height = loc_alt;
		timeZone = tz;
		isAnObservatory = false;
		pressure = pres;
		temperature = temp;
		humidity = humi;
		observerPosition = target;
		this.ellipsoid = ellipsoid;
		this.dstCode = dst;
		if (name == null || name.equals("")) {
			try {
				// Set DST information
				CityElement city = City.findNearestCity(new LocationElement(longitude, latitude, 1.0));
				dstCode = Country.getID(city.country).getDSTCode();
				name = city.name;
			} catch (JPARSECException e) {
				Logger.log(LEVEL.WARNING, "Could not set the DST field for location "+loc_name+".");
				dstCode = DST_RULE.NONE;
			}
		}
	}

	/**
	 * Creates an Observer object from a City object.
	 *
	 * @param city City object.
	 * @throws JPARSECException If an error occurs.
	 */
	public ObserverElement(CityElement city) throws JPARSECException {
		ObserverElement obs = ObserverElement.parseCity(city);
		this.dstCode = obs.dstCode;
		this.name = obs.name;
		this.geoLat = obs.geoLat;
		this.geoLon = obs.geoLon;
		this.geoRad = obs.geoRad;
		this.height = obs.height;
		this.longitude = obs.longitude;
		this.latitude = obs.latitude;
		this.timeZone = obs.timeZone;
		this.isAnObservatory = obs.isAnObservatory;
		this.humidity = obs.humidity;
		this.pressure = obs.pressure;
		this.temperature = obs.temperature;
		observerPosition = null;
		ellipsoid = ELLIPSOID.LATEST;
	}

	/**
	 * Creates an Observer object from an Observatory object.
	 *
	 * @param observatory Observatory object.
	 * @throws JPARSECException If an error occurs.
	 */
	public ObserverElement(ObservatoryElement observatory)
			throws JPARSECException {
		ObserverElement obs = ObserverElement.parseObservatory(observatory);
		this.dstCode = obs.dstCode;
		this.name = obs.name;
		this.geoLat = obs.geoLat;
		this.geoLon = obs.geoLon;
		this.geoRad = obs.geoRad;
		this.height = obs.height;
		this.longitude = obs.longitude;
		this.latitude = obs.latitude;
		this.timeZone = obs.timeZone;
		this.isAnObservatory = obs.isAnObservatory;
		this.humidity = obs.humidity;
		this.pressure = obs.pressure;
		this.temperature = obs.temperature;
		observerPosition = null;
		ellipsoid = ELLIPSOID.LATEST;
	}

	/**
	 * Creates an Observer object from an Extraterrestrial object.
	 *
	 * @param et The extraterrestrial observer object.
	 * @throws JPARSECException If an error occurs.
	 */
	public ObserverElement(ExtraterrestrialObserverElement et)
			throws JPARSECException {
		ObserverElement obs = ObserverElement.parseExtraterrestrialObserver(et);
		this.dstCode = obs.dstCode;
		this.name = obs.name;
		this.geoLat = obs.geoLat;
		this.geoLon = obs.geoLon;
		this.geoRad = obs.geoRad;
		this.height = obs.height;
		this.longitude = obs.longitude;
		this.latitude = obs.latitude;
		this.timeZone = obs.timeZone;
		this.isAnObservatory = obs.isAnObservatory;
		this.humidity = obs.humidity;
		this.pressure = obs.pressure;
		this.temperature = obs.temperature;
		observerPosition = obs.observerPosition;
		motherPlanet = obs.motherPlanet;
		ellipsoid = ELLIPSOID.LATEST;
	}

	/**
	 * Name of the location.
	 */
	private String name;

	/**
	 * Longitude in radians measured to the east of the observer.
	 */
	private double longitude;

	/**
	 * Geodetic latitude in radians of the observer.
	 */
	private double latitude;

	/**
	 * Height above sea level in meters of the observer.
	 */
	private int height;

	/**
	 * Time zone, without considering daylight saving time,
	 * which is calculated and applied automatically. Time
	 * zone is the number of hours to be added to UTC to get
	 * local time (without DST), and is always the same value
	 * for a given place.
	 */
	private double timeZone;

	/**
	 * Pressure in milibars.
	 */
	private int pressure;

	/**
	 * Temperature in Celsius.
	 */
	private int temperature;

	/**
	 * Humidity as a percentage, from 0 to 100.
	 */
	private int humidity;

	/**
	 * Geocentric longitude in radians, only used when converting from geodetic.
	 */
	private double geoLon;

	/**
	 * Geocentric latitude in radians, only used when converting from geodetic.
	 */
	private double geoLat;

	/**
	 * Geocentric distance in Earth radii, only used when converting from
	 * geodetic.
	 */
	private double geoRad;

	/**
	 * Daylight Saving Time rule.
	 */
	private DST_RULE dstCode;

	/**
	 * Sets whether the observer is at some observatory or in a city.
	 */
	private boolean isAnObservatory;

	/**
	 * The heliocentric equatorial vector of the observer, mean equinox
	 * and equator J2000. Null for Planet-based computations.
	 */
	private double[] observerPosition;

	/**
	 * The mother planet for the observer. Earth by default.
	 */
	private TARGET motherPlanet = TARGET.EARTH;

	/**
	 * The ellipsoid used as reference for the geodetic location of the observer,
	 * by default the latest one available.
	 */
	private ELLIPSOID ellipsoid;

	/**
	 * Default temperature of 10 Celsius.
	 */
	public static final int DEFAULT_TEMPERATURE = 10;

	/**
	 * Default humidity of 20%.
	 */
	public static final int DEFAULT_HUMIDITY = 20;

	/**
	 * Default pressure of 1010 mb.
	 */
	public static final int DEFAULT_PRESSURE = 1010;

	/**
	 * Returns the DST code.
	 * @return DST code.
	 */
	public DST_RULE getDSTCode() {
		return dstCode;
	}

	/**
	 * Sets the DST code.
	 * @param dstCode DST code.
	 */
	public void setDSTCode(DST_RULE dstCode) {
		this.dstCode = dstCode;
	}

	/**
	 * Returns if this observer comes from an observatory object.
	 * @return True or false.
	 */
	public boolean isAnObservatory() {
		return isAnObservatory;
	}

	/**
	 * Returns the name of the place.
	 * @return Observer's location name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of the location.
	 * @param name Location name.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the pressure.
	 * @return Pressure in mb.
	 * @throws JPARSECException If the observer is not on a Solar System body.
	 */
	public int getPressure() throws JPARSECException {
		if (getMotherBody() == TARGET.NOT_A_PLANET) throw new JPARSECException("Observer must be on some Solar System body.");
		return pressure;
	}

	/**
	 * Sets the pressure.
	 * @param pressure Pressure in mb.
	 * @throws JPARSECException If the observer is not on a Solar System body.
	 */
	public void setPressure(int pressure) throws JPARSECException {
		if (getMotherBody() == TARGET.NOT_A_PLANET) throw new JPARSECException("Observer must be on some Solar System body.");
		this.pressure = pressure;
	}

	/**
	 * Gets the temperature.
	 * @return Temperature in C.
	 * @throws JPARSECException If the observer is not on a Solar System body.
	 */
	public int getTemperature() throws JPARSECException {
		if (getMotherBody() == TARGET.NOT_A_PLANET) throw new JPARSECException("Observer must be on some Solar System body.");
		return temperature;
	}

	/**
	 * Sets the temperature.
	 * @param temperature Temperature in C.
	 * @throws JPARSECException If the observer is not on a Solar System body.
	 */
	public void setTemperature(int temperature) throws JPARSECException {
		if (getMotherBody() == TARGET.NOT_A_PLANET) throw new JPARSECException("Observer must be on some Solar System body.");
		this.temperature = temperature;
	}

	/**
	 * Gets the humidity percentage.
	 * @return Humidity, 0 to 100.
	 * @throws JPARSECException If the observer is not on a Solar System body.
	 */
	public int getHumidity() throws JPARSECException {
		if (getMotherBody() == TARGET.NOT_A_PLANET) throw new JPARSECException("Observer must be on some Solar System body.");
		return humidity;
	}

	/**
	 * Sets the humidity.
	 * @param humidity Humidity, 0 to 100.
	 * @throws JPARSECException If the observer is not on a Solar System body.
	 */
	public void setHumidity(int humidity) throws JPARSECException {
		if (getMotherBody() == TARGET.NOT_A_PLANET) throw new JPARSECException("Observer must be on some Solar System body.");
		this.humidity = humidity;
	}

	/**
	 * The set of DST rules. Daylight saving time is usually an hour added between
	 * spring and autumn in the north hemisphere. The rule applied in each case when
	 * the time used for calculations reaches the 'limit' is as follows:
	 * <BR>1. In case of changing 2h LT to 3h LT in spring (DST changes from 0 to 1), 2h LT to 3h LT does not exists, but you can access this range in JPARSEC and resulting UT is correct (DST still 0).
	 * <BR>2. In case of changing 2h LT to 1h LT in autumn (DST changes from 1 to 0), 1h LT to 2h LT for DST 0 cannot be accessed. &gt;=2h LT will be available for DST 0.
	 * <BR>DST is obtained using UT in {@linkplain TimeScale#getDST(double, ObserverElement)}.
	 */
	public enum DST_RULE {
		/** No daylight saving time rule or unknown. */
		NONE,
		/** Daylight saving time starts on last Sunday of march and ends on last Sunday of October.  */
		N1,
		/** Daylight saving time starts on last Sunday of April and ends on last Sunday of November. */
		N2,
		/** Daylight saving time starts on last Sunday of October and ends on last Sunday of March of the next year. */
		S1,
		/** Daylight saving time starts on last Sunday of November and ends on last Sunday of April of the next year. */
		S2,
		/** Symbolic constant for USA/CANADA old rule for DST. */
		USA_OLD,
		/** Symbolic constant for USA/CANADA new rule for DST. */
		USA_NEW,
		/**
		 * Symbolic constant for USA/CANADA automatic rule selection for DST. Before
		 * 2007 old rule will be applied, in 2007 and after the new one. This is the
		 * default behavior and it is not necessary to choose this setting for USA
		 * or Canada, although it could be useful if it is necessary to apply the
		 * same method in other countries.
		 */
		USA_AUTO,
		/** Symbolic constant for a custom DST rule. You will have to set also the
		 * start and end fields accordingly (and, optionally, the time offset),
		 * using any of the constants provided in this class. The UNKNOWN field
		 * should not be used. */
		CUSTOM;

		/** Sets the start of DST. Will have effect only for CUSTOM DST rule. */
		public int start = UNKNOWN;
		/** Sets the end of DST. Will have effect only for CUSTOM DST rule. */
		public int end = UNKNOWN;
		/** Sets the time offset to apply the DST respect 0h UT. Set to 1 to
		 * apply the DST at 1h UT, and so on. */
		public double timeOffsetHours = 0;

		/** Constant for an UNKNOWN DST. */
		public static final int UNKNOWN = 0;
		/** Constant for last Sunday of March. */
		public static final int LAST_SUNDAY_MARCH = 1;
		/** Constant for last Sunday of October. */
		public static final int LAST_SUNDAY_OCTOBER = 2;
		/** Constant for last Sunday of April. */
		public static final int LAST_SUNDAY_APRIL = 3;
		/** Constant for last Sunday of November. */
		public static final int LAST_SUNDAY_NOVEMBER = 4;
		/** Constant for last Sunday of March in the next year. */
		public static final int LAST_SUNDAY_MARCH_NEXT_YEAR = 5;
		/** Constant for last Sunday of April in the next year. */
		public static final int LAST_SUNDAY_APRIL_NEXT_YEAR = 6;
		/** Constant for first Sunday of April. */
		public static final int FIRST_SUNDAY_APRIL = 7;
		/** Constant for first Sunday of November. */
		public static final int FIRST_SUNDAY_NOVEMBER = 8;
		/** Constant for second Sunday of March. */
		public static final int SECOND_SUNDAY_MARCH = 9;

		/**
		 * Resets the start and end of DST to their default values (UNKNOWN),
		 * and time offset to 0.
		 */
		public void clear() {
			if (this == CUSTOM) {
				start = UNKNOWN;
				end = UNKNOWN;
				timeOffsetHours = 0;
			}
		}
	};

	/**
	 * Get latitude in degrees.
	 *
	 * @return Latitude in <B>degrees</B>
	 * @throws JPARSECException If the observer is not on a Solar System body.
	 */
	public double getLatitudeDeg() throws JPARSECException {
		if (getMotherBody() == TARGET.NOT_A_PLANET) throw new JPARSECException("Observer must be on some Solar System body.");
		return Math.toDegrees(latitude);
	}

	/**
	 * Get latitude in radians.
	 *
	 * @return Latitude in <B>radians</B>
	 * @throws JPARSECException If the observer is not on a Solar System body.
	 */
	public double getLatitudeRad() throws JPARSECException {
		if (getMotherBody() == TARGET.NOT_A_PLANET) throw new JPARSECException("Observer must be on some Solar System body.");
		return latitude;
	}

	/**
	 * Set latitude in <B>degrees</B>.
	 *
	 * @param lat Latitude in <B>degrees</B>
	 * @throws JPARSECException If the observer is not on a Solar System body.
	 */
	public void setLatitudeDeg(double lat) throws JPARSECException {
		if (getMotherBody() == TARGET.NOT_A_PLANET) throw new JPARSECException("Observer must be on some Solar System body.");
		latitude = Math.toRadians(lat);
		geoLon = geoLat = geoRad = 0.0;
	}

	/**
	 * Set latitude in <B>radians</B>.
	 *
	 * @param lat Latitude in <B>radians</B>
	 * @throws JPARSECException If the observer is not on a Solar System body.
	 */
	public void setLatitudeRad(double lat) throws JPARSECException {
		if (getMotherBody() == TARGET.NOT_A_PLANET) throw new JPARSECException("Observer must be on some Solar System body.");
		latitude = lat;
		geoLon = geoLat = geoRad = 0.0;
	}

	/**
	 * Get longitude in degrees.
	 *
	 * @return Longitude in <B>degrees</B>
	 * @throws JPARSECException If the observer is not on a Solar System body.
	 */
	public double getLongitudeDeg() throws JPARSECException {
		if (getMotherBody() == TARGET.NOT_A_PLANET) throw new JPARSECException("Observer must be on some Solar System body.");
		return Math.toDegrees(longitude);
	}

	/**
	 * Get longitude in radians.
	 *
	 * @return Longitude in <B>radians</B>
	 * @throws JPARSECException If the observer is not on a Solar System body.
	 */
	public double getLongitudeRad() throws JPARSECException {
		if (getMotherBody() == TARGET.NOT_A_PLANET) throw new JPARSECException("Observer must be on some Solar System body.");
		return longitude;
	}

	/**
	 * Set longitude in degrees.
	 *
	 * @param lon Longitude in <B>degrees</B>
	 * @throws JPARSECException If the observer is not on a Solar System body.
	 */
	public void setLongitudeDeg(double lon) throws JPARSECException {
		if (getMotherBody() == TARGET.NOT_A_PLANET) throw new JPARSECException("Observer must be on some Solar System body.");
		//if (geoLon == longitude) geoLon = Math.toRadians(lon);
		longitude = Math.toRadians(lon);
		geoLon = geoLat = geoRad = 0.0;
	}

	/**
	 * Set longitude in radians.
	 *
	 * @param lon Longitude in <B>radians</B>
	 * @throws JPARSECException If the observer is not on a Solar System body.
	 */
	public void setLongitudeRad(double lon) throws JPARSECException {
		if (getMotherBody() == TARGET.NOT_A_PLANET) throw new JPARSECException("Observer must be on some Solar System body.");
		//if (geoLon == longitude) geoLon = lon;
		longitude = lon;
		geoLon = geoLat = geoRad = 0.0;
	}

	/**
	 * Get time zone offset, without considering DST.
	 *
	 * @return Time zone offset from UTC (-12 to 12 inclusive)
	 */
	public double getTimeZone() {
		return timeZone;
	}

	/**
	 * Set time zone offset.
	 *
	 * @param tz Time zone offset from UTC (-12 to 12 inclusive)
	 */
	public void setTimeZone(double tz) {
		timeZone = tz;
	}

	/**
	 * Returns the height.
	 *
	 * @return Height above sea level in m.
	 * @throws JPARSECException If the observer is not on a Solar System body.
	 */
	public int getHeight() throws JPARSECException {
		if (getMotherBody() == TARGET.NOT_A_PLANET) throw new JPARSECException("Observer must be on some Solar System body.");
		return height;
	}

	/**
	 * Sets the height.
	 *
	 * @param h Height above sea level in m.
	 * @param setDefaultPressure True to set the pressure to its
	 * default value for the given height (for an observer on Earth).
	 * @throws JPARSECException If the observer is not on a Solar System body.
	 */
	public void setHeight(int h, boolean setDefaultPressure) throws JPARSECException {
		if (getMotherBody() == TARGET.NOT_A_PLANET) throw new JPARSECException("Observer must be on some Solar System body.");
		height = h;
		if (setDefaultPressure) pressure = (int) (DEFAULT_PRESSURE * Math.exp(-h / 9100.0));
		geoLon = geoLat = geoRad = 0.0;
	}

	/**
	 * Returns the geocentric longitude.
	 * @return Geocentric longitude in radians.
	 * @throws JPARSECException If the observer is not on a Solar System body.
	 */
	public double getGeoLon() throws JPARSECException {
		if (getMotherBody() == TARGET.NOT_A_PLANET) throw new JPARSECException("Observer must be on some Solar System body.");
		if (geoLon == 0.0 && geoLat == 0.0 && geoRad == 0.0) {
			LocationElement loc = geodeticToGeocentric(ellipsoid, longitude, latitude, height);
			geoLon = loc.getLongitude();
			geoLat = loc.getLatitude();
			geoRad = loc.getRadius();
		}
		return geoLon;
	}
	/**
	 * Returns the geocentric latitude.
	 * @return Geocentric latitude in radians.
	 * @throws JPARSECException If the observer is not on a Solar System body.
	 */
	public double getGeoLat() throws JPARSECException {
		if (getMotherBody() == TARGET.NOT_A_PLANET) throw new JPARSECException("Observer must be on some Solar System body.");
		if (geoLon == 0.0 && geoLat == 0.0 && geoRad == 0.0) {
			LocationElement loc = geodeticToGeocentric(ellipsoid, longitude, latitude, height);
			geoLon = loc.getLongitude();
			geoLat = loc.getLatitude();
			geoRad = loc.getRadius();
		}
		return geoLat;
	}
	/**
	 * Returns the geocentric distance in units of
	 * Earth's equatorial radius.
	 * @return Geocentric distance, close to unity.
	 * @throws JPARSECException If the observer is not on a Solar System body.
	 */
	public double getGeoRad() throws JPARSECException {
		if (getMotherBody() == TARGET.NOT_A_PLANET) throw new JPARSECException("Observer must be on some Solar System body.");
		if (geoLon == 0.0 && geoLat == 0.0 && geoRad == 0.0) {
			LocationElement loc = geodeticToGeocentric(ellipsoid, longitude, latitude, height);
			geoLon = loc.getLongitude();
			geoLat = loc.getLatitude();
			geoRad = loc.getRadius();
		}
		return geoRad;
	}

	/**
	 * Returns the body where this observer is located.
	 * @return The body.
	 */
	public TARGET getMotherBody() {
		return motherPlanet;
	}

	/**
	 * Returns the ellipsoid.
	 * @return The ellipsoid.
	 * @throws JPARSECException If the observer is not on a Solar System body.
	 */
	public ELLIPSOID getEllipsoid() throws JPARSECException {
		if (getMotherBody() == TARGET.NOT_A_PLANET) throw new JPARSECException("Observer must be on some Solar System body.");
		return ellipsoid;
	}

	/**
	 * Transform from geodetic to geocentric coordinates. The geocentric
	 * distance is set to Earth equatorial radii. For reference see Astronomical
	 * Almanac, page K5.
	 * @param ellipsoid The reference ellipsoid for input geodetic location.
	 * @param longitude The longitude.
	 * @param latitude The latitude.
	 * @param height Height above sea level.
	 * @return The location from the geocenter, distance in units of Earth's equatorial radius.
	 */
	public static LocationElement geodeticToGeocentric(ELLIPSOID ellipsoid, double longitude,
			double latitude, int height)
	{
		// Get ellipsoid
		double Earth_radius = ellipsoid.getEquatorialRadius();
		double Earth_flatenning = ellipsoid.getInverseOfFlatteningFactor();

		// Apply calculations
		double flat = Earth_flatenning;
		double co = Math.cos(latitude);
		double si = Math.sin(latitude);
		double fl = 1.0 - 1.0 / flat;
		fl = fl * fl;
		si = si * si;
		double u = 1.0 / Math.sqrt(co * co + fl * si);
		double a = Earth_radius * u * 1000.0 + height;
		double b = Earth_radius * fl * u * 1000.0 + height;
		double rho = Math.sqrt(a * a * co * co + b * b * si);
		double geo_lat = Math.acos(a * co / rho);
		if (latitude < 0.0)
			geo_lat = -geo_lat;
		rho = rho / (1000.0 * Earth_radius);

		double geoLat = geo_lat;
		double geoLon = longitude;
		double geoRad = rho;
		return new LocationElement(geoLon, geoLat, geoRad);
	}

	/**
	 * Transform from geocentric/planetocentric coordinates to geodetic. The
	 * geodetic distance is set as a height above mean elipsoid height. The
	 * method is an analitical inversion of the geodetic to geocentric
	 * transformation, that produces an slightly error in latitude in the
	 * milliarcsecond level.
	 * @param ellipsoid The reference ellipsoid.
	 * @param geoLon Geocentric longitude.
	 * @param geoLat Geocentric latitude.
	 * @param geoRad Geocentric distance in units of Earth's equatorial radius.
	 * @return Geodetic location as longitude, latitude, and elevation above sea level in m.
	 */
	public static LocationElement geocentricToGeodetic(ELLIPSOID ellipsoid, double geoLon,
			double geoLat, double geoRad)
	{
		// Get ellipsoid
		double Earth_radius = ellipsoid.getEquatorialRadius();
		double Earth_flatenning = ellipsoid.getInverseOfFlatteningFactor();

		// Apply calculations
		double flat = Earth_flatenning;
		double fl = 1.0 - 1.0 / flat;
		fl = fl * fl;
		double rho = geoRad;
		double lat = Math.atan(Math.tan(geoLat) / fl);
		double co = Math.cos(lat);
		double si = Math.sin(lat);
		double u = 1.0 / Math.sqrt(co * co + fl * si * si);
		double a = Earth_radius * u * 1000.0;
		double b = Earth_radius * fl * u * 1000.0;

		rho = rho * (1000.0 * Earth_radius);

		double coef_A = co * co + si * si;
		double coef_B = 2.0 * a * co * co + 2.0 * b * si * si;
		double coef_C = a * a * co * co + b * b * si * si - rho * rho;

		double alt = (-coef_B + Math.sqrt(coef_B * coef_B - 4.0 * coef_A * coef_C)) / (2.0 * coef_A);
		lat = Math.acos(rho * Math.cos(geoLat) / (a + alt));
		if (geoLat < 0.0)
			lat = -lat;

		double latitude = lat;
		double longitude = geoLon;
		double height = (int) Math.floor(alt + 0.5);
		return new LocationElement(longitude, latitude, height);
	}

	/**
	 * Returns the distance between two observers, supposing they are
	 * on Earth surface and at sea level. From Astronomical Algorithms,
	 * chapter 10. Accuracy of about 50 m.
	 * @param obs1 First observer.
	 * @param obs2 Second observer. The coordinates are supposed to be
	 * referred to the same ellipsoid as the first observer.
	 * @return Distance in km.
	 */
	public static double getDistance(ObserverElement obs1, ObserverElement obs2) {
		double F = (obs1.latitude + obs2.latitude) * 0.5;
		double G = (obs1.latitude - obs2.latitude) * 0.5;
		double L = (obs1.longitude - obs2.longitude) * 0.5;
		double sg = Math.sin(G), cg = Math.cos(G), sl = Math.sin(L), cl = Math.cos(L), cf = Math.cos(F), sf = Math.sin(F);
		double S = sg * sg * cl * cl + cf * cf * sl * sl;
		double C = cg * cg * cl * cl + sf * sf * sl * sl;
		double w = Math.atan(Math.sqrt(S / C));
		double R = Math.sqrt(S * C) / w;
		double D = 2.0 * w * obs1.ellipsoid.getEquatorialRadius();
		double ifl = obs1.ellipsoid.getInverseOfFlatteningFactor();
		double H1 = (3.0 * R - 1.0) / (2.0 * C);
		double H2 = (3.0 * R + 1.0) / (2.0 * S);
		double s = D * (1.0 + H1 * sf * sf * cg * cg / ifl - H2 * cf * cf * sg * sg / ifl);
		return s;
	}

	/**
	 * Forces the observer to be on Earth by setting the body
	 * to Earth and the ellipsoid to the latest Earth's ellipsoid.
	 */
	public void forceObserverOnEarth() {
		name = "";
		longitude = 0.0;
		latitude = 0.0;
		height = 0;
		timeZone = 0.0;
		pressure = DEFAULT_PRESSURE;
		temperature = DEFAULT_TEMPERATURE;
		humidity = DEFAULT_HUMIDITY;
		isAnObservatory = false;
		dstCode = DST_RULE.NONE;
		this.observerPosition = null;
		this.ellipsoid = ELLIPSOID.LATEST;
		motherPlanet = TARGET.EARTH;
	}

	/**
	 * Clones this instance.
	 */
	public ObserverElement clone() {
		ObserverElement o = new ObserverElement("NO", longitude, latitude, height, timeZone, dstCode, pressure, temperature, humidity, ellipsoid, observerPosition);
		o.name = this.name;
		o.geoLat = this.geoLat;
		o.geoLon = this.geoLon;
		o.geoRad = this.geoRad;
		o.isAnObservatory = this.isAnObservatory;
		o.observerPosition = null;
		if (this.observerPosition != null) o.observerPosition = this.observerPosition.clone();
		o.motherPlanet = this.motherPlanet;
		return o;
	}

	/**
	 * Checks if the actual {@linkplain ObserverElement} is similar to another
	 * or not.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ObserverElement)) return false;

		ObserverElement that = (ObserverElement) o;

		if (Double.compare(that.longitude, longitude) != 0) return false;
		if (Double.compare(that.latitude, latitude) != 0) return false;
		if (height != that.height) return false;
		if (Double.compare(that.timeZone, timeZone) != 0) return false;
		if (pressure != that.pressure) return false;
		if (temperature != that.temperature) return false;
		if (humidity != that.humidity) return false;
		if (Double.compare(that.geoLon, geoLon) != 0) return false;
		if (Double.compare(that.geoLat, geoLat) != 0) return false;
		if (Double.compare(that.geoRad, geoRad) != 0) return false;
		if (isAnObservatory != that.isAnObservatory) return false;
		if (name != null ? !name.equals(that.name) : that.name != null) return false;
		if (dstCode != that.dstCode) return false;
		if (!Arrays.equals(observerPosition, that.observerPosition)) return false;
		if (motherPlanet != that.motherPlanet) return false;

		return ellipsoid == that.ellipsoid;
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		result = name != null ? name.hashCode() : 0;
		temp = Double.doubleToLongBits(longitude);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(latitude);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + height;
		temp = Double.doubleToLongBits(timeZone);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + pressure;
		result = 31 * result + temperature;
		result = 31 * result + humidity;
		temp = Double.doubleToLongBits(geoLon);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(geoLat);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(geoRad);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (dstCode != null ? dstCode.hashCode() : 0);
		result = 31 * result + (isAnObservatory ? 1 : 0);
		result = 31 * result + (observerPosition != null ? Arrays.hashCode(observerPosition) : 0);
		result = 31 * result + (motherPlanet != null ? motherPlanet.hashCode() : 0);
		result = 31 * result + (ellipsoid != null ? ellipsoid.hashCode() : 0);
		return result;
	}

	/**
	 * Gets the {@linkplain ObserverElement} of certain {@linkplain CityElement}.
	 *
	 * @param city The city to parse.
	 * @return The corresponding observer.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public static ObserverElement parseCity(CityElement city)
			throws JPARSECException {
		ObserverElement obs = new ObserverElement(city.name, city.longitude
				* Constant.DEG_TO_RAD, city.latitude * Constant.DEG_TO_RAD,
				city.height, city.timeZone, null);

		try {
			// Set DST information
			obs.dstCode = Country.getID(city.country).getDSTCode();
		} catch (JPARSECException e) {
			obs.dstCode = DST_RULE.NONE;
			JPARSECException.addWarning(Translate.translate(273));
		}

		obs.isAnObservatory = false;

		return (obs);
	}

	/**
	 * Gets the observer of certain observatory. Note 0.0 is the output for the
	 * time zone offset due to a lack of information. DST rule is set to the country
	 * where the observatory lies, if it is found.
	 *
	 * @param observatory The observatory to parse.
	 * @return The corresponding observer.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public static ObserverElement parseObservatory(
			ObservatoryElement observatory) throws JPARSECException {
		String n = observatory.name;
		if (!observatory.location.equals("")) n += " ("+observatory.location+")";
		ObserverElement obs = new ObserverElement(n,
				observatory.longitude * Constant.DEG_TO_RAD,
				observatory.latitude * Constant.DEG_TO_RAD, observatory.height,
				0.0, null);

		try {
			// Set DST information
			obs.dstCode = Country.getID(observatory.country).getDSTCode();
		} catch (Exception e) {
			obs.dstCode = DST_RULE.NONE;
			JPARSECException.addWarning(Translate.translate(273));
		}

		obs.isAnObservatory = true;

		return (obs);
	}

	/**
	 * Gets the observer for certain extraterrestrial object. Time zone and
	 * elevation above reference level is set to 0, and dst rule to NONE.
	 * Any calculation with this kind of observer should never be done in
	 * local time, since local time is for an observer on Earth. Anyway, just
	 * take into account that for an observer in other body local time = UTC.
	 *
	 * @param et The extraterrestrial object.
	 * @return The corresponding observer.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public static ObserverElement parseExtraterrestrialObserver(
			ExtraterrestrialObserverElement et) throws JPARSECException {
		if (et.barycentricVector != null) {
			ObserverElement obs = new ObserverElement(et.name, 0, 0, 0, 0.0, null);
			obs.observerPosition = et.barycentricVector;
			obs.dstCode = DST_RULE.NONE;
			obs.isAnObservatory = false;
			obs.motherPlanet = TARGET.NOT_A_PLANET;
			return obs;
		} else {
			double lon = 0, lat = 0;
			int elev = 0;
			if (et.obsLoc != null) {
				lon = et.obsLoc.getLongitude();
				lat = et.obsLoc.getLatitude();
				elev = (int) et.obsLoc.getRadius();
			}
			ObserverElement obs = new ObserverElement(et.name, lon, lat, elev, 0.0, null);
			obs.observerPosition = et.barycentricVector;
			obs.dstCode = DST_RULE.NONE;
			obs.isAnObservatory = false;
			obs.motherPlanet = et.motherPlanet;
			obs.ellipsoid = ELLIPSOID.CUSTOM;
			obs.ellipsoid.setEllipsoid(et.motherPlanet);
			return obs;
		}
	}

	/**
	 * Creates an Observer object by using geolocalization to automatically find
	 * the position of the user, using freegeoip.net.
	 *
	 * @param ip The IP to be located, or null (or empty string) to use the
	 * current computer IP.
	 * @throws JPARSECException If an error occurs during the query.
	 */
	public ObserverElement(String ip) throws JPARSECException {
		// Create and execute query
		String query = "http://api.ipstack.com/check?access_key=8aa6cb5c55d68fcc823bb2e62155acd1";
		if (ip != null && !ip.equals("")) query = "http://api.ipstack.com/" + ip + "?access_key=8aa6cb5c55d68fcc823bb2e62155acd1";
		String out = GeneralQuery.query(query, "UTF-8", 30000);

		// Parse query results
		String country = FileIO.getField(6, out, ",", false);
		String region = FileIO.getField(8, out, ",", false);
		String city = FileIO.getField(9, out, ",", false);
		String lat = FileIO.getField(11, out, ",", false);
		String lon = FileIO.getField(12, out, ",", false);
		// String time = FileIO.getField(11, out, ",", false); // time zone in hours

		country = FileIO.getField(2, country, ":", false);
		region = FileIO.getField(2, region, ":", false);
		city = FileIO.getField(2, city, ":", false);
		lat = FileIO.getField(2, lat, ":", false);
		lon = FileIO.getField(2, lon, ":", false);
		country = DataSet.replaceAll(country, "\"", "", true);
		region = DataSet.replaceAll(region, "\"", "", true);
		country = DataSet.replaceAll(country, "null", "", true);
		region = DataSet.replaceAll(region, "null", "", true);
		city = DataSet.replaceAll(city, "null", "", true);
		city = DataSet.replaceAll(city, "\"", "", true);
		lat = DataSet.replaceAll(lat, "\"", "", true);
		lon = DataSet.replaceAll(lon, "\"", "", true);
		
		country = DataSet.capitalize(country.toLowerCase(), true);
		country = DataSet.replaceAll(country, " And ", " and ", true);
		country = DataSet.replaceAll(country, " Of ", " of ", true);
		country = DataSet.replaceAll(country, " The ", " the ", true);

		double obslon = DataSet.parseDouble(lon);
		double obslat = DataSet.parseDouble(lat);
		double tz = 0.0; //DateTimeOps.tzOffset();
		// Don't trust time zone since its value can be affected by DST
/*		if (!time.equals("") && time.indexOf(":") > 0) {
			String h = time.substring(0, time.indexOf(":"));
			String m = time.substring(time.indexOf(":")+1);
			tz = DataSet.parseDouble(h) + DataSet.parseDouble(m) / 60.0;
		} else {
*/			CityElement closestCity = City.findNearestCity(new LocationElement(obslon * Constant.DEG_TO_RAD, obslat * Constant.DEG_TO_RAD, 1.0));
			if (city == null || city.equals("")) city = closestCity.name;
			try {
				Country.getID(country);
			} catch (Exception exc) {
				country = null;
			}
			if (country == null || country.equals("")) country = closestCity.country;
			tz = closestCity.timeZone;
//		}

		name = city;
		if (!region.equals("")) {
			if (!city.equals("")) {
				name += ", "+region;
			} else {
				name = region;
			}
		}
		if (!country.equals("")) name += " (" + country + ")";
		longitude = obslon * Constant.DEG_TO_RAD;
		latitude = obslat * Constant.DEG_TO_RAD;
		timeZone = tz;
		pressure = DEFAULT_PRESSURE;
		temperature = DEFAULT_TEMPERATURE;
		humidity = DEFAULT_HUMIDITY;
		isAnObservatory = false;
		height = 0;
		observerPosition = null;
		ellipsoid = ELLIPSOID.LATEST;

		// Pass name to ISO charset
		//Charset utf8charset = Charset.forName("UTF-8");
		//Charset iso88591charset = Charset.forName("ISO-8859-1");
		//ByteBuffer inputBuffer = ByteBuffer.wrap(name.getBytes());
		// decode UTF-8
		//CharBuffer data = utf8charset.decode(inputBuffer);
		// encode ISO-8559-1
		//ByteBuffer outputBuffer = iso88591charset.encode(data);
		//byte[] outputData = inputBuffer.array();
		//name = new String(outputData, utf8charset);

		try {
			// Set DST information
			dstCode = Country.getID(country).getDSTCode();
		} catch (JPARSECException e) {
			dstCode = DST_RULE.NONE;
			JPARSECException.addWarning(Translate.translate(273));
		}

		obtainElevation(false);
	}

	/**
	 * Sets the elevation approximately by using an Earth bump map or the closest 
	 * city in the database. The method will
	 * adjust the {@link #height} field or will do nothing
	 * if there is a problem accessing the map or an error during calculations.
	 * @param useBumpMap True to use a bump map, false to find closest city. In the
	 * latter case the location name will be also updated in case it is empty or null,
	 * and daylight saving time rule and time zone will also be updated.
	 */
	public void obtainElevation(boolean useBumpMap) {
		if (this.getMotherBody() != TARGET.EARTH) return;

		if (useBumpMap) {
			double obslat = this.latitude * Constant.RAD_TO_DEG;
			double obslon = this.longitude * Constant.RAD_TO_DEG;

			// Obtain location height in m using Earth bump map
			String s = "4kEarthBump";
			try {
				ObjectInputStream is = new ObjectInputStream(getClass().getClassLoader().getResourceAsStream(FileIO.DATA_TEXTURES_DIRECTORY + s));

				int texturax = (Integer) is.readObject();
				int texturay = (Integer) is.readObject();
				int[] pixels = (int[]) is.readObject();

				int pos_y = (int) (texturay * 0.5 * (1.0 - obslat / 90.0) + 0.5);
				int pos_x = (int) (texturax * 0.5 * (1.0 + obslon / 180.0) + 0.5);

				int img_index = pos_y * texturax + pos_x;

				int k = pixels[img_index];
				int red = 0xff & (k >> 16);
				int green = 0xff & (k >> 8);
				int blue = 0xff & k;

				int valor0 = 0;
				int alt = (int) (8848 * (red + green + blue - valor0 * 3) / (3.0 * (255 - valor0))); // 8420 could be better
				if (alt < 0) alt = 0;

				height = alt;
			} catch (Exception e3) {
				// throw new JPARSECException("error while reading texture file " + FileIO.DATA_TEXTURES_DIRECTORY + s + "." , e3);
			}
		} else {
			try {
				LocationElement loc0 = new LocationElement(this.longitude, this.latitude, 1.0);
				CityElement cc = City.findNearestCity(loc0);

				double dist0 = LocationElement.getAngularDistance(loc0, new LocationElement(cc.longitude*Constant.DEG_TO_RAD,cc.latitude*Constant.DEG_TO_RAD,1.0));
				double dr = -1;
				String ciudad = "", altura = "";
				if (cc.country.toLowerCase().equals("spain")) {
					 ArrayList<String> file = ReadFile.readResource("jparsec/observer/SpainCities.txt");
					 for (int i=0;i<file.size();i=i+2) {
						String li = file.get(i), lip = file.get(i + 1);
						String n = li.substring(0, 48).trim();
						String lats = lip.substring(0, 21).trim();
						String lons = (lip.substring(21,22)+lip.substring(23, 31)).trim();
						String alt = lip.substring(42).trim();
						double lo = Functions.parseDeclination(lons);
						double la = Functions.parseDeclination(lats);
						LocationElement loc = new LocationElement(lo, la, 1.0);
						double r = LocationElement.getAngularDistance(loc, loc0);
						if (r < dr || dr == -1) {
							dr = r;
							ciudad = n;
							altura = alt;
						}
					 }
				}

				if ((dr >= 0 && dr < dist0) || ciudad.equals(cc.name)) {
					if (name == null || name.equals("")) this.name = ciudad;
					cc.height = Integer.parseInt(altura);
				}

				if (name == null || name.equals("")) this.name = cc.name;
				this.height = cc.height;
				this.timeZone = cc.timeZone;
				try {
					// Set DST information
					dstCode = Country.getID(cc.country).getDSTCode();
				} catch (JPARSECException e) {
					dstCode = DST_RULE.NONE;
					JPARSECException.addWarning(Translate.translate(273));
				}
			} catch (Exception e3) {
				// throw new JPARSECException("error while reading texture file " + FileIO.DATA_TEXTURES_DIRECTORY + s + "." , e3);
			}
		}
	}

	/**
	 * Returns a String representation of this object.
	 */
	@Override
	public String toString() {
		String out = "lon: "+Functions.formatDEC(this.longitude)+", lat: "+Functions.formatDEC(this.latitude)+", elev: "+this.height+" m";
		if (motherPlanet != TARGET.EARTH) out += " ("+motherPlanet.getName()+")";
		return out;
	}

	/**
	 * Returns the observer's position as a {@linkplain LocationElement} object.
	 * @return The position. Radius is set as height above sea level in m.
	 */
	public LocationElement getGeodeticLocation() {
		return new LocationElement(longitude, latitude, height);
	}
	/**
	 * Returns the observer's geocentric position as a {@linkplain LocationElement} object.
	 * @return The position. Radius is set as geocentric distance in units of Earth's equatorial radii.
	 * @throws JPARSECException If the observer is not on a Solar System body.
	 */
	public LocationElement getGeocentricLocation() throws JPARSECException {
		return new LocationElement(this.getGeoLon(), this.getGeoLat(), this.getGeoRad());
	}

	/**
	 * Corrects geodetic coordinates by polar motion. This correction amounts to about 10m, and it is
	 * not required most of the times (only for precise apparent positions). This correction, however,
	 * can improve ephemerides for Moon by about 2 mas, and those for artificial satellites by several
	 * arcseconds. JPARSEC never calls this method.
	 * @param time Time object.
	 * @param eph Ephemeris object.
	 * @throws JPARSECException If an error occurs.
	 */
	public void correctObserverForPolarMotion(TimeElement time, EphemerisElement eph) throws JPARSECException {
		if (getMotherBody() == TARGET.NOT_A_PLANET) throw new JPARSECException("Observer must be on some Solar System body.");
 		if (this.getMotherBody() != TARGET.EARTH) return;

		double jd_UTC = TimeScale.getJD(time, this, eph, SCALE.UNIVERSAL_TIME_UTC);
		double eop[] = new double[] {0.0, 0.0, 0.0, 0.0, 0.0};
		if (eph.correctForEOP) eop = EarthOrientationParameters.obtainEOP(jd_UTC, eph);
		double x = eop[2] * Constant.ARCSEC_TO_RAD, y = eop[3] * Constant.ARCSEC_TO_RAD;

		// Site mean longitude and mean geodetic latitude as a Cartesian vector
		double SEL = Math.sin(this.longitude);
		double CEL = Math.cos(this.longitude);
		double SPH = Math.sin(this.latitude);
		double CPH = Math.cos(this.latitude);

		double XM = CEL * CPH;
		double YM = SEL * CPH;
		double ZM = SPH;

		// Rotate site vector by polar motion, Y-component then X-component
		double SXP = Math.sin(x);
		double CXP = Math.cos(x);
		double SYP = Math.sin(y);
		double CYP = Math.cos(y);

		double ZW = (-YM * SYP + ZM * CYP);

		double XT = XM * CXP - ZW * SXP;
		double YT = YM * CYP + ZM * SYP;
		double ZT = XM * SXP + ZW * CXP;

		CPH = Math.sqrt(XT * XT + YT * YT);
		if (CPH == 0.0) XT = 1.0;

		// Return true longitude and true geodetic latitude of site
		if (XT != 0.0 || YT != 0.0) {
			this.longitude = Math.atan2(YT, XT);
		} else {
			this.longitude = 0.0;
		}
		this.latitude = Math.atan2(ZT, CPH);
	}

	/**
	 * Return geocentric rectangular coordinates of the observer in the ICRF
	 * system, for J2000 mean equinox and equator (GCRS coordinates). In case
	 * the ephemerides properties are set to geocentric, this method returns
	 * 0 for all components.
	 *
	 * @param time Time object.
	 * @param eph Ephemeris object.
	 * @return (x, y, z, vx, vy, vz) of observer in AU and AU/d. Zero values for
	 *         geocentric ephemerides.
	 * @throws JPARSECException Thrown if the method fails, for example because
	 *         of an invalid date or reference ellipsoid.
	 */
	public double[] topocentricObserverICRF(TimeElement time, EphemerisElement eph)
			throws JPARSECException
	{
		if (eph.isTopocentric && this.getMotherBody() != TARGET.NOT_A_PLANET)
		{
			double JD = TimeScale.getJD(time, this, eph, SCALE.TERRESTRIAL_TIME);
			double last = SiderealTime.apparentSiderealTime(time, this, eph);

			double geoLat = this.getGeoLat(), geoRad = this.getGeoRad();
			double true_pos[] = LocationElement.parseLocationElement(new LocationElement(last,
					geoLat, geoRad * this.ellipsoid.getEquatorialRadius() / Constant.AU));
			double vel_mod = (this.geoRad * this.ellipsoid.getEquatorialRadius() / Constant.AU) * getMotherBodyMeanRotationRate(eph) * Constant.SECONDS_PER_DAY;
			double true_vel[] = { -vel_mod * Math.sin(last), vel_mod * Math.cos(last), 0.0 };

			// Remove the nutation correction to go to mean position
			if (this.getMotherBody() == TARGET.EARTH) {
				true_pos = Nutation.nutateInEquatorialCoordinates(JD, eph, true_pos, false);
				true_vel = Nutation.nutateInEquatorialCoordinates(JD, eph, true_vel, false);
			}

			true_pos = Precession.precessToJ2000(JD, true_pos, eph);
			true_vel = Precession.precessToJ2000(JD, true_vel, eph);

			true_pos = Ephem.DynamicaltoICRSFrame(true_pos);
			true_vel = Ephem.DynamicaltoICRSFrame(true_vel);

			return new double[] { true_pos[0], true_pos[1], true_pos[2], true_vel[0], true_vel[1], true_vel[2] };
		}

		return new double[]	{ 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
	}

	/**
	 * Return heliocentric equatorial rectangular coordinates of the observer's mother body in case the
	 * observer is not located on Earth.
	 *
	 * @param JD_TDB Julian day in TDB.
	 * @param eph Ephemeris object.
	 * @return (x, y, z, vx, vy, vz) of observer in AU and AU/d, for J2000 equinox and equator.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[] heliocentricPositionOfObserver(double JD_TDB, EphemerisElement eph) throws JPARSECException {
		if (observerPosition != null) {
			return this.observerPosition;
		} else {
			double out[] = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
			TARGET body = this.getMotherBody();
			if (body.isNaturalSatellite()) {
				body = this.getMotherBody().getCentralBody();

				switch (body)
				{
				case MARS:
					int sat = 1;
					if (this.getMotherBody() == TARGET.Deimos) sat = 2;
					out = Mars07.getMoonPosition(JD_TDB, sat, true);
					break;
				case JUPITER:
					int ks = 1 + this.getMotherBody().ordinal() - TARGET.Io.ordinal();
					if (ks < 1 || ks > 4)
						throw new JPARSECException("unsupported body. Use one of the main satellites of Mars, Jupiter, Saturn, or Uranus.");
					out = L1.L1_theory(JD_TDB, ks);
					break;
				case SATURN:
					int nsat = 1 + this.getMotherBody().ordinal() - TARGET.Mimas.ordinal();
					if (nsat < 1 || nsat > 8)
						throw new JPARSECException("unsupported body. Use one of the main satellites of Mars, Jupiter, Saturn, or Uranus.");
					out = TASS17.TASS17_theory(JD_TDB, nsat, false);
					out = Ephem.eclipticToEquatorial(out, Constant.J2000, eph);
					break;
				case URANUS:
					int IS = 1 + this.getMotherBody().ordinal() - TARGET.Miranda.ordinal();
					if (IS < 1 || IS > 5)
						throw new JPARSECException("unsupported body. Use one of the main satellites of Mars, Jupiter, Saturn, or Uranus.");
					out = GUST86.GUST86_theory(JD_TDB, IS, 3);
					out = Precession.precessPosAndVelInEquatorial(Constant.J1950, Constant.J2000, out, eph);
					break;
				default:
					throw new JPARSECException("unsupported body. Use one of the main satellites of Mars, Jupiter, Saturn, or Uranus.");
				}
			}
			if (body == TARGET.Moon) {
				switch (eph.algorithm) {
				case MOSHIER:
				case SERIES96_MOSHIERForMoon:
					out = Ephem.eclipticToEquatorial(PlanetEphem.getHeliocentricEclipticPositionJ2000(JD_TDB, body), Constant.J2000, eph);
					break;
				case VSOP87_ELP2000ForMoon:
					out = Elp2000.meanJ2000InertialToEquatorialFK5(Elp2000.calc(JD_TDB));
					break;
				default:
					if (!eph.algorithm.isJPL()) throw new JPARSECException("Invalid/unsupported algorithm.");
					JPLEphemeris jpl = new JPLEphemeris(eph.algorithm);
					out = jpl.getPositionAndVelocity(JD_TDB, TARGET.Moon);
					break;
				}
				body = TARGET.EARTH;
				if (out.length == 3) out = new double[] {out[0], out[1], out[2], 0.0, 0.0, 0.0};
			}

			switch (eph.algorithm) {
			case SERIES96_MOSHIERForMoon:
				// Compute position of Earth (or home planet)
				double helio_earth[] = Series96.getHeliocentricEclipticPositionJ2000(JD_TDB, body);
				return Functions.sumVectors(out, Ephem.eclipticToEquatorial(helio_earth, Constant.J2000, eph));
			case VSOP87_ELP2000ForMoon:
				// Compute position of Earth (or home planet)
				helio_earth = Vsop.getHeliocentricEclipticPositionJ2000(JD_TDB, body);
				return Functions.sumVectors(out, Ephem.eclipticToEquatorial(helio_earth, Constant.J2000, eph));
			case MOSHIER:
				// Compute position of Earth (or home planet)
				helio_earth = PlanetEphem.getHeliocentricEclipticPositionJ2000(JD_TDB, body);
				double time_step = 0.001;
				double[] helio_earth_plus = PlanetEphem.getHeliocentricEclipticPositionJ2000(JD_TDB + time_step, body);
				double[] helio_earth_vel = new double[]
				{ (helio_earth_plus[0] - helio_earth[0]) / time_step, (helio_earth_plus[1] - helio_earth[1]) / time_step,
						(helio_earth_plus[2] - helio_earth[2]) / time_step };
				return Functions.sumVectors(out, Ephem.eclipticToEquatorial(new double[] {helio_earth[0], helio_earth[1], helio_earth[2], helio_earth_vel[0], helio_earth_vel[1], helio_earth_vel[2]}, Constant.J2000, eph));
			default:
				if (!eph.algorithm.isJPL())	throw new JPARSECException("Invalid/unsupported algorithm.");
				JPLEphemeris jpl = new JPLEphemeris(eph.algorithm);
				if (body == TARGET.EARTH) {
					// Compute apparent position of barycenter from Earth
					double helio_barycenter[] = jpl.getPositionAndVelocity(JD_TDB, TARGET.Earth_Moon_Barycenter);
					if (Functions.equalVectors(helio_barycenter, JPLEphemeris.INVALID_VECTOR))
						return JPLEphemeris.INVALID_VECTOR;
					double moon[] = jpl.getPositionAndVelocity(JD_TDB, TARGET.Moon);
					double emrat = Double.parseDouble(jpl.getConstant("EMRAT"));
					return Functions.sumVectors(out, Functions.substract(helio_barycenter, Functions.scalarProduct(moon, 1.0 / (1.0 + emrat))));
				} else {
					return Functions.sumVectors(out, jpl.getPositionAndVelocity(JD_TDB, body));
				}
			}
		}
	}

	/**
	 * Returns the mean rotation rate of a given body.
	 * @param eph Ephemeris properties containing body and ephemeris method.
	 * @return The rotation rate in radians/second. 0 is returned for geocentric
	 * ephemerides.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getMotherBodyMeanRotationRate(EphemerisElement eph) throws JPARSECException {
		if (this.getMotherBody() == null || !eph.isTopocentric) return 0;
		if (this.getMotherBody() == TARGET.EARTH) return Constant.EARTH_MEAN_ROTATION_RATE;
		EphemerisElement ephIn = eph.clone();
		ephIn.targetBody = this.getMotherBody();
		if (ephIn.targetBody.isNaturalSatellite()) {
			return MoonPhysicalParameters.getBodyMeanRotationRate(ephIn);
		} else {
			return PhysicalParameters.getBodyMeanRotationRate(ephIn);
		}
	}
}
