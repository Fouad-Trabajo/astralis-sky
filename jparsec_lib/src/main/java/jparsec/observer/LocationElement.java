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

import java.io.Serializable;

import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.EphemerisElement.ALGORITHM;
import jparsec.ephem.EphemerisElement.COORDINATES_TYPE;
import jparsec.ephem.Functions;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.moons.MoonEphem;
import jparsec.ephem.planets.EphemElement;
import jparsec.ephem.stars.StarElement;
import jparsec.ephem.stars.StarEphem;
import jparsec.ephem.stars.StarEphemElement;
import jparsec.graph.DataSet;
import jparsec.graph.chartRendering.RenderSky;
import jparsec.math.Constant;
import jparsec.math.FastMath;
import jparsec.time.TimeElement;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;
import jparsec.util.Translate.LANGUAGE;

/**
 * This is a convenience class used for passing around polar coordinates. Units
 * are radians for longitude and latitude, AU for distance.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class LocationElement implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor.
	 */
	public LocationElement()
	{
		set(new double[] {0.0, 0.0, 0.0});
	}

	/**
	 * Explicit constructor.
	 *
	 * @param lon longitude.
	 * @param lat latitude.
	 * @param rad radius.
	 */
	public LocationElement(double lon, double lat, double rad)
	{
		set(lon, lat, rad);
	}

	/**
	 * Constructs a location object with the equatorial location of a given source.
	 * This method does not call any Internet service, it resolves objects using only JPARSEC.
	 * Source can be a star, deep sky object, or solar system body. Location will
	 * contain heliocentric coordinates of the body, except for solar system
	 * bodies, that will provide geocentric positions. Distance will be
	 * 1.0 for deep sky objects, in pc for stars, and in AU for solar system objects.
	 * Reference frame is J2000 Dynamical Equinox for solar system objects and ICRF for
	 * stars and deep sky objects. Output positions can be mean (astrometric) J2000 or
	 * apparent of date.
	 * @param body The name of the body.
	 * @param apparentOfDate True to return the apparent position of the body for the
	 * current time (corrected for aberration, precesion, and nutation), false for mean
	 * J2000 position. Apparent position will be geocentric.
	 * @throws JPARSECException If the body is not found.
	 */
	public LocationElement(String body, boolean apparentOfDate) throws JPARSECException {
		TARGET t = jparsec.ephem.Target.getID(body);
		if (t == TARGET.NOT_A_PLANET && Translate.getDefaultLanguage() != LANGUAGE.ENGLISH)
			t = jparsec.ephem.Target.getIDFromEnglishName(body);

		TimeElement time = new TimeElement();
		CityElement city = City.findCity("Madrid");
		ObserverElement observer = ObserverElement.parseCity(city);
		EphemerisElement eph = new EphemerisElement(t, EphemerisElement.COORDINATES_TYPE.APPARENT,
				EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.GEOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
				EphemerisElement.FRAME.DYNAMICAL_EQUINOX_J2000, EphemerisElement.ALGORITHM.MOSHIER);
		if (!apparentOfDate) {
			eph.ephemType = COORDINATES_TYPE.ASTROMETRIC;
			eph.equinox = Constant.J2000;
		}

		if (t == TARGET.NOT_A_PLANET) {
			int index = StarEphem.getStarTargetIndex(body);
		    double parsecToAU = apparentOfDate ? (Constant.PARSEC / (Constant.AU * 1E3)) : 1;
			if (index == -1) {
				LocationElement se = RenderSky.searchDeepSkyObjectJ2000(body);
				if (se == null) throw new JPARSECException("Object not found");
				se.setRadius(1E8);
				set(se.get());
			} else {
				StarElement star = StarEphem.getStarElement(index);
				if (!apparentOfDate) {
					set(star.rightAscension, star.declination, star.getDistance());				
				} else {
				    StarEphemElement se = StarEphem.starEphemeris(time, observer, eph, star, false);
					set(se.rightAscension, se.declination, se.distance);
				}
				return;
			}

			if (apparentOfDate) {
				LocationElement loc = Ephem.fromJ2000ToApparentGeocentricEquatorial(this, time, observer, eph);
			    loc.setRadius(loc.getRadius() / parsecToAU);
				set(loc.get());
			}
		} else {
			eph.targetBody = t;
			if (t.isAsteroid()) eph.algorithm = ALGORITHM.ORBIT;
			if (t.isNaturalSatellite()) eph.algorithm = ALGORITHM.NATURAL_SATELLITE;
			EphemElement ephem = Ephem.getEphemeris(time, observer, eph, false);
			set(ephem.rightAscension, ephem.declination, ephem.distance);
		}
	}

	/**
	 * Constructs a location object with the equatorial location of a given source.
	 * This method does not call any Internet service, it resolves objects using only JPARSEC.
	 * Source can be a star, deep sky object, or solar system body. Location will
	 * contain heliocentric coordinates of the body, except for solar system
	 * bodies, that will provide geocentric positions. Distance will be
	 * 1.0 for deep sky objects, in pc for stars, and in AU for solar system objects.
	 * Reference frame is J2000 Dynamical Equinox for solar system objects and ICRF for
	 * stars and deep sky objects. Output positions can be mean (astrometric) J2000 or
	 * apparent of date.
	 * @param body The name of the body.
	 * @param time Time object for calculations.
	 * @param observer Observer object for calculations.
	 * @param eph0 Ephemeris object for calculations.
	 * @throws JPARSECException If the body is not found.
	 */
	public LocationElement(String body, TimeElement time, ObserverElement observer,
			EphemerisElement eph0) throws JPARSECException {
		EphemElement ephem = Ephem.getEphemeris(body, time, observer, eph0, false);
		set(new double[] {ephem.rightAscension, ephem.declination, ephem.distance});
	}

	/**
	 * Vector constructor.
	 *
	 * @param vector { lon, lat, rad }
	 */
	public LocationElement(double vector[])
	{
		set(vector);
	}

	/**
	 * Explicit constructor for float values.
	 * The instance will use float internally to reduce memory use.
	 *
	 * @param lon longitude.
	 * @param lat latitude.
	 * @param rad radius.
	 */
	public LocationElement(float lon, float lat, float rad)
	{
		set(lon, lat, rad);
	}

	/**
	 * Vector constructor using floats.
	 * The instance will use float internally to reduce memory use.
	 *
	 * @param vector { lon, lat, rad }
	 */
	public LocationElement(float vector[])
	{
		set(vector);
	}

	/**
	 * Gets the latitude.
	 *
	 * @return The latitude value of this instance.
	 */
	public double getLatitude()
	{
		if (lonLatRad != null)
			return lonLatRad[1];
		return lonLatRadF[1];
	}

	/**
	 * Gets the longitude.
	 *
	 * @return The longitude value of this instance.
	 */
	public double getLongitude()
	{
		if (lonLatRad != null)
			return lonLatRad[0];
		return lonLatRadF[0];
	}

	/**
	 * Gets the radius.
	 *
	 * @return The radius value of this instance.
	 */
	public double getRadius()
	{
		if (lonLatRad != null)
			return lonLatRad[2];
		return lonLatRadF[2];
	}

	/**
	 * Get all values in this instance as a vector. <P>
	 * The vector is an array of three doubles, longitude, latitude, radius.
	 *
	 * @return v[0] = longitude, v[1] = latitude, v[2] = radius.
	 */
	public double[] get()
	{
		if (lonLatRad != null)
			return lonLatRad;
		return DataSet.toDoubleArray(lonLatRadF);
	}

	/**
	 * Set the latitude.
	 *
	 * @param d The new latitude value.
	 */
	public void setLatitude(double d)
	{
		if (lonLatRad != null) {
			lonLatRad[1] = d;
		} else {
			lonLatRadF[1] = (float) d;
		}
	}

	/**
	 * Set the longitude.
	 *
	 * @param d The new longitude value.
	 */
	public void setLongitude(double d)
	{
		if (lonLatRad != null) {
			lonLatRad[0] = Functions.normalizeRadians(d);
		} else {
			lonLatRadF[0] = (float) Functions.normalizeRadians(d);
		}
	}

	/**
	 * Set the radius.
	 *
	 * @param d The new radius value.
	 */
	public void setRadius(double d)
	{
		if (lonLatRad != null) {
			lonLatRad[2] = d;
		} else {
			lonLatRadF[2] = (float) d;
		}
	}

	/**
	 * Set all members of this instance from a vector. <P>
	 * The vector is an array of three doubles, longitude, latitude, radius, in
	 * that order.
	 *
	 * @param vector v[0] = longitude, v[1] = latitude, v[2] = radius.
	 */
	public void set(double vector[])
	{
		lonLatRad = vector;
		lonLatRadF = null;
	}

	/**
	 * Set all members of this instance individually.
	 *
	 * @param lon The new longitude.
	 * @param lat The new latitude.
	 * @param rad The new radius.
	 */
	public void set(double lon, double lat, double rad)
	{
		lonLatRad = new double[] {lon, lat, rad};
		lonLatRadF = null;
	}

	/**
	 * Set all members of this instance from a vector. <P>
	 * The vector is an array of three doubles, longitude, latitude, radius, in
	 * that order.
	 *
	 * @param vector v[0] = longitude, v[1] = latitude, v[2] = radius.
	 */
	public void set(float vector[])
	{
		lonLatRadF = vector;
		lonLatRad = null;
	}

	/**
	 * Set all members of this instance individually.
	 *
	 * @param lon The new longitude.
	 * @param lat The new latitude.
	 * @param rad The new radius.
	 */
	public void set(float lon, float lat, float rad)
	{
		lonLatRadF = new float[] {lon, lat, rad};
		lonLatRad = null;
	}

	/**
	 * Longitude, latitude, and radius.
	 */
	private double[] lonLatRad;
	/**
	 * Longitude, latitude, and radius when using floats.
	 */
	private float[] lonLatRadF;

	/**
	 * Transforms rectangular coordinates x, y, z contained in an array to a
	 * {@linkplain LocationElement}. Coordinate system is independent: equatorial, ecliptic,
	 * or any other.
	 *
	 * @param v (x, y, z) vector.
	 * @return Location object.
	 */
	public static LocationElement parseRectangularCoordinates(double v[])
	{
		double lon = 0.0;
		double lat = Constant.PI_OVER_TWO;
		if (v[2] < 0.0)
			lat = -lat;
		if (v[1] != 0.0 || v[0] != 0.0)
		{
			lon = Math.atan2(v[1], v[0]);
			lat = Math.atan(v[2] / Math.sqrt(v[0] * v[0] + v[1] * v[1]));
			if (lon < 0.0) lon += Constant.TWO_PI;
		}
		double rad = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);

		LocationElement loc = new LocationElement(lon, lat, rad);
		return loc;
	}

	/**
	 * Transforms rectangular coordinates x, y, z contained in an array to a
	 * {@linkplain LocationElement}. Coordinate system is independent: equatorial, ecliptic,
	 * or any other. This method uses {@linkplain FastMath#atan2_accurate(double, double)} method.
	 *
	 * @param v (x, y, z) vector.
	 * @return Location object.
	 */
	public static LocationElement parseRectangularCoordinatesFast(double v[])
	{
		return parseRectangularCoordinatesFast(v[0], v[1], v[2]);
	}

	/**
	 * Transforms rectangular coordinates x, y, z contained in an array to a
	 * {@linkplain LocationElement}. Coordinate system is independent: equatorial, ecliptic,
	 * or any other. This method uses {@linkplain FastMath#atan2_accurate(double, double)} method.
	 *
	 * @param x X coordinate.
	 * @param y Y coordinate.
	 * @param z Z coordinate.
	 * @return Location object.
	 */
	public static LocationElement parseRectangularCoordinatesFast(double x, double y, double z)
	{
		double lon = 0.0, h = 0.0;
		double lat = Constant.PI_OVER_TWO;
		if (z < 0.0)
			lat = -lat;

		if (y != 0.0 || x!= 0.0)
		{
			h = FastMath.hypot(x, y);
			lon = FastMath.atan2_accurate(y, x);
			lat = FastMath.atan2_accurate(z / h, 1.0);
		}
		double rad = Math.sqrt(h * h + z * z);

		LocationElement loc = new LocationElement(lon, lat, rad);
		return loc;
	}

	/**
	 * Transforms rectangular coordinates x, y, z to a {@linkplain LocationElement}.
	 * Coordinate system is independent: equatorial, ecliptic, or any other.
	 *
	 * @param x X coordinate.
	 * @param y Y coordinate.
	 * @param z Z coordinate.
	 * @return Location object.
	 */
	public static LocationElement parseRectangularCoordinates(double x, double y, double z)
	{
		double v[] = new double[] { x, y, z };
		LocationElement loc = LocationElement.parseRectangularCoordinates(v);
		return loc;
	}

	/**
	 * Transforms a {@linkplain LocationElement} into a set of rectangular coordinates x, y, z.
	 *
	 * @param loc Location object.
	 * @return Array with (x, y, z) vector.
	 */
	public static double[] parseLocationElement(LocationElement loc)
	{
		double lon = loc.getLongitude(), lat = loc.getLatitude(), rad = loc.getRadius();
		double cl = Math.cos(lat);
		double x = rad * Math.cos(lon) * cl;
		double y = rad * Math.sin(lon) * cl;
		double z = rad * Math.sin(lat);

		return new double[] { x, y, z };
	}

	/**
	 * Transforms a {@linkplain LocationElement} into a set of rectangular coordinates x, y, z,
	 * using approximate trigonometric functions.
	 *
	 * @param loc Location object.
	 * @return Array with (x, y, z) vector.
	 */
	public static double[] parseLocationElementFast(LocationElement loc)
	{
		double lon = loc.getLongitude(), lat = loc.getLatitude(), rad = loc.getRadius();
		double cl = FastMath.cos(lat);
		double x = rad * FastMath.cos(lon) * cl;
		double y = rad * FastMath.sin(lon) * cl;
		double z = rad * FastMath.sin(lat);

		return new double[] { x, y, z };
	}

	/**
	 * Transforms a {@linkplain LocationElement} into a set of rectangular coordinates x, y, z.
	 *
	 * @return Array with (x, y, z) vector.
	 */
	public double[] getRectangularCoordinates()
	{
		return LocationElement.parseLocationElement(this);
	}

	/**
	 * Obtain linear distance between two spherical positions.
	 *
	 * @param loc1 Location object.
	 * @param loc2 Location object.
	 * @return Linear distance.
	 */
	public static double getLinearDistance(LocationElement loc1, LocationElement loc2)
	{
		double[] xyz1 = parseLocationElement(loc1);
		double[] xyz2 = parseLocationElement(loc2);

		double dx = xyz1[0] - xyz2[0];
		double dy = xyz1[1] - xyz2[1];
		double dz = xyz1[2] - xyz2[2];

		double r = Math.sqrt(dx * dx + dy * dy + dz * dz);

		return r;
	}

	/**
	 * Obtain the direction between two spherical positions.
	 *
	 * @param loc1 Location object.
	 * @param loc2 Location object.
	 * @return The midpoint.
	 */
	public static LocationElement getMidPoint(LocationElement loc1, LocationElement loc2)
	{
		double[] xyz1 = parseLocationElement(loc1);
		double[] xyz2 = parseLocationElement(loc2);

		double x = xyz1[0] + (xyz2[0] - xyz1[0]) * 0.5;
		double y = xyz1[1] + (xyz2[1] - xyz1[1]) * 0.5;
		double z = xyz1[2] + (xyz2[2] - xyz1[2]) * 0.5;

		return LocationElement.parseRectangularCoordinates(x, y, z);
	}

	/**
	 * Obtain the relative position of the second loc object respect the first one.
	 *
	 * @param loc1 Location object.
	 * @param loc2 Location object.
	 * @return The relative position of the second body respect the first one, as an
	 * offset in longitude and latitude (radians), and distance.
	 */
	public static double[] getRelativeOffsets(LocationElement loc1, LocationElement loc2)
	{
		double[] xyz1 = parseLocationElement(loc1);
		double[] xyz2 = parseLocationElement(loc2);

		return MoonEphem.relativePosition(xyz1, xyz2);
	}

	/**
	 * Solves an spherical triangle return the angle between the positions
	 * locP and locP1 as seen from locA.
	 * @param locA A position.
	 * @param locP P position.
	 * @param locP1 P1 position.
	 * @param approx True for an approximate and faster computation.
	 * @return The spherical angle.
	 */
	public static double solveSphericalTriangle(LocationElement locA,
			LocationElement locP, LocationElement locP1, boolean approx) {
		if (approx) {
			double p1p = LocationElement.getApproximateAngularDistance(locP1, locP);
			double p1o = LocationElement.getApproximateAngularDistance(locP1, locA);
			double po = LocationElement.getApproximateAngularDistance(locP, locA);
			double sinO = FastMath.sin(p1p) * FastMath.sin(locA.getLongitude()-locP1.getLongitude()) / FastMath.sin(p1o);
			double cosO = (FastMath.cos(p1p) - FastMath.cos(p1o) * FastMath.cos(po)) / FastMath.sin(p1o) * FastMath.sin(po);
			return FastMath.atan2(sinO, cosO);
		}

		double p1p = LocationElement.getAngularDistance(locP1, locP);
		double p1o = LocationElement.getAngularDistance(locP1, locA);
		double po = LocationElement.getAngularDistance(locP, locA);
		double sinO = Math.sin(p1p) * Math.sin(locA.getLongitude()-locP1.getLongitude()) / Math.sin(p1o);
		double cosO = (Math.cos(p1p) - Math.cos(p1o) * Math.cos(po)) / Math.sin(p1o) * Math.sin(po);
		return Math.atan2(sinO, cosO);
	}

	/**
	 * Obtain angular distance between two spherical coordinates.
	 *
	 * @param loc1 Location object.
	 * @param loc2 Location object.
	 * @return The distance in radians, from 0 to PI.
	 */
	public static double getAngularDistance(LocationElement loc1, LocationElement loc2)
	{
		LocationElement cl1 = new LocationElement(loc1.getLongitude(), loc1.getLatitude(), 1.0);
		LocationElement cl2 = new LocationElement(loc2.getLongitude(), loc2.getLatitude(), 1.0);

		double[] xyz1 = parseLocationElement(cl1);
		double[] xyz2 = parseLocationElement(cl2);

		double dx = xyz1[0] - xyz2[0];
		double dy = xyz1[1] - xyz2[1];
		double dz = xyz1[2] - xyz2[2];

		double r2 = dx * dx + dy * dy + dz * dz;

		return Math.acos(1.0 - r2 * 0.5);
/*
 		// Haversine formula
 		double dLat = loc1.lat - loc2.lat;
		double dLon = loc1.lon - loc2.lon;
		double a = FastMath.sin(dLat/2) * FastMath.sin(dLat/2) + FastMath.cos(loc1.lat) * FastMath.cos(loc2.lat) * FastMath.sin(dLon/2) * FastMath.sin(dLon/2);
		return 2.0 * FastMath.atan2(Math.sqrt(a), Math.sqrt(1.0-a));
*/
	}

	/**
	 * Obtain approximate angular distance between two spherical coordinates. Good performance,
	 * accuracy around 0.2 deg or better.
	 *
	 * @param loc1 Location object.
	 * @param loc2 Location object.
	 * @return The distance in radians, from 0 to PI.
	 */
	public static double getApproximateAngularDistance(LocationElement loc1, LocationElement loc2)
	{
		double lon1 = loc1.getLongitude(), lat1 = loc1.getLatitude();
		double lon2 = loc2.getLongitude(), lat2 = loc2.getLatitude();

		return FastMath.acos(FastMath.sin(lat2) * FastMath.sin(lat1) + FastMath.cos(lat2) * FastMath.cos(lat1) * FastMath.cos(lon2-lon1));
	}

	/**
	 * Obtain position angle between two spherical coordinates. Good performance.
	 *
	 * @param loc1 Location object.
	 * @param loc2 Location object.
	 * @return The position angle in radians.
	 */
	public static double getApproximatePositionAngle(LocationElement loc1, LocationElement loc2)
	{
		double al = loc1.getLongitude(), ap = loc1.getLatitude();
		double bl = loc2.getLongitude(), bp = loc2.getLatitude();
		double dl = bl - al;
		double cbp = FastMath.cos(bp);
	    double y = FastMath.sin(dl) * cbp;
	    double x = FastMath.sin(bp) * FastMath.cos(ap) - cbp * FastMath.sin(ap) * FastMath.cos(dl);
	    double pa = 0.0;
	    if (x != 0.0 || y != 0.0) pa = -FastMath.atan2_accurate(y, x);
		return pa;
	}

	/**
	 * Obtain exact position angle between two spherical coordinates. Performance will be poor.
	 *
	 * @param loc1 Location object.
	 * @param loc2 Location object.
	 * @return The position angle in radians.
	 */
	public static double getPositionAngle(LocationElement loc1, LocationElement loc2)
	{
		double al = loc1.getLongitude(), ap = loc1.getLatitude();
		double bl = loc2.getLongitude(), bp = loc2.getLatitude();
		double dl = bl - al;
		double cbp = Math.cos(bp);
	    double y = Math.sin(dl) * cbp;
	    double x = Math.sin(bp) * Math.cos(ap) - cbp * Math.sin(ap) * Math.cos(dl);
	    double pa = 0.0;
	    if (x != 0.0 || y != 0.0) pa = -Math.atan2(y, x);
		return pa;
	}

	/**
	 * Gets the geodetic {@linkplain LocationElement} of certain {@linkplain CityElement}.
	 * Radius is set to unity.
	 * <P>
	 *
	 * @param city The {@linkplain CityElement} to parse.
	 * @return The corresponding {@linkplain LocationElement}.
	 */
	public static LocationElement parseCity(CityElement city)
	{
		LocationElement loc = new LocationElement(city.longitude * Constant.DEG_TO_RAD, city.latitude * Constant.DEG_TO_RAD, 1.0);

		return loc;
	}

	/**
	 * Gets the geodetic {@linkplain LocationElement} of certain {@linkplain ObservatoryElement}.
	 * Radius is set to unity.
	 *
	 * @param observatory The {@linkplain ObservatoryElement} to parse.
	 * @return The corresponding {@linkplain LocationElement}.
	 */
	public static final LocationElement parseObservatory(ObservatoryElement observatory)
	{
		LocationElement loc = new LocationElement(observatory.longitude * Constant.DEG_TO_RAD,
				observatory.latitude * Constant.DEG_TO_RAD, 1.0);

		return loc;
	}

	/**
	 * To clone the object.
	 */
	@Override
	public LocationElement clone()
	{
		LocationElement loc = new LocationElement(this.getLongitude(), this.getLatitude(),
				this.getRadius());
		return loc;
	}
	/**
	 * Returns true if the input object is equals to this instance.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof LocationElement)) return false;

		LocationElement that = (LocationElement) o;

		double lon = getLongitude(), lat = getLatitude(), rad = getRadius();
		double lont = that.getLongitude(), latt = that.getLatitude(), radt = that.getRadius();
		if (Double.compare(latt, lat) != 0) return false;
		if (Double.compare(lont, lon) != 0) return false;

		return Double.compare(radt, rad) == 0;
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		double lon = getLongitude(), lat = getLatitude(), rad = getRadius();
		temp = Double.doubleToLongBits(lat);
		result = (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(lon);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(rad);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	/**
	 * Returns a String representation of this object.
	 */
	@Override
	public String toString() {
		double lon = getLongitude(), lat = getLatitude(), rad = getRadius();
		String out = "lon: "+Functions.formatDEC(lon)+", lat: "+Functions.formatDEC(lat)+", rad: "+rad;
		return out;
	}

	/**
	 * Returns a String representation of this object.
	 * @return The string.
	 */
	public String toStringAsEquatorialLocation() {
		double lon = getLongitude(), lat = getLatitude(), rad = getRadius();
		String out = "RA: "+Functions.formatRA(lon)+", DEC: "+Functions.formatDEC(lat)+", DIST: "+rad;
		return out;
	}

	/**
	 * Moves this position a given amount in each axis.
	 * @param dlon Displacement in longitude, radians. It is a
	 * pure incremental modification.
	 * @param dlat Displacement in latitude, radians.
	 * @param dr Displacemente in distance, in units of the distance.
	 */
	public void move(double dlon, double dlat, double dr) {
		if (dlon != 0.0) setLongitude(getLongitude() + dlon);
		if (dlat != 0.0) setLatitude(getLatitude() + dlat);
		if (dr != 0.0) setRadius(getRadius() + dr);
	}

	/**
	 * Moves this position a given amount in each axis, setting the location
	 * to a given point with some offset.
	 * @param dlon Displacement in longitude, radians. This increment is
	 * respect the sky plain, not a coordinate incremental offset.
	 * @param dlat Displacement in latitude, radians.
	 */
	public void toOffset(double dlon, double dlat) {
		double lonp = dlon / Math.cos(getLatitude());
		if (lonp != 0.0) setLongitude(getLongitude() + lonp);
		if (dlat != 0.0) setLatitude(getLatitude() + dlat);
	}
}
