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
package jparsec.ephem.probes;

import java.io.Serializable;
import java.util.Arrays;

import jparsec.observer.LocationElement;

/**
 * Convenient class to store results of ephemeris of artificial satellites.
 *
 * @see SatelliteEphem
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class SatelliteEphemElement implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a satellite ephem object giving all the data.
	 *
	 * @param nom Name of the satellite.
	 * @param ra Right ascension.
	 * @param dec Declination.
	 * @param dist Distance.
	 * @param azi Azimuth.
	 * @param ele Elevation
	 * @param selon Sub-Earth longitude.
	 * @param selat Sub-Earth latitude.
	 * @param sedist Sub-Earth distance above sea level.
	 * @param speed Topocentric relative speed.
	 * @param elo Elongation.
	 * @param ill Illumination fraction.
	 * @param st True if the satellite if eclipsed, false otherwise.
	 * @param rev Number of revolutions completed.
	 */
	public SatelliteEphemElement(String nom, double ra, double dec, double dist, double azi, double ele, float selon,
			float selat, float sedist, float speed, float elo, float ill, boolean st, int rev)
	{
		name = nom;
		rightAscension = ra;
		declination = dec;
		distance = dist;
		azimuth = azi;
		elevation = ele;
		subEarthLongitude = selon;
		subEarthLatitude = selat;
		subEarthDistance = sedist;
		illumination = ill;
		isEclipsed = st;
		revolutionsCompleted = rev;
		elongation = elo;
		topocentricSpeed = speed;
	}

	/**
	 * Constructor of an empty object.
	 */
	public SatelliteEphemElement()
	{
		name = "";
		rightAscension = 0.0;
		declination = 0.0;
		distance = 0.0;
		azimuth = 0.0;
		elevation = 0.0;
		subEarthLongitude = 0.0f;
		subEarthLatitude = 0.0f;
		subEarthDistance = 0.0f;
		illumination = 0.0f;
		isEclipsed = false;
		revolutionsCompleted = 0;
		elongation = 0.0f;
		nextPass = 0.0;
		topocentricSpeed = 0.0f;
	}

	/**
	 * Name of the satellite.
	 */
	public String name;

	/**
	 * Right ascension in radians.
	 */
	public double rightAscension;

	/**
	 * Declination in radians.
	 */
	public double declination;

	/**
	 * Distance in km.
	 */
	public double distance;

	/**
	 * Azimuth in radians.
	 */
	public double azimuth;

	/**
	 * Elevation in radians.
	 */
	public double elevation;

	/**
	 * Sub-Earth longitude in radians.
	 */
	public float subEarthLongitude;

	/**
	 * Sub-Earth latitude in radians.
	 */
	public float subEarthLatitude;

	/**
	 * Sub-Earth altitude in km.
	 */
	public float subEarthDistance;

	/**
	 * Elongation in radians.
	 */
	public float elongation;

	/**
	 * Phase of ilumination.
	 */
	public float illumination;

	/**
	 * True if it is not visible for any observer at all (not illuminated).
	 */
	public boolean isEclipsed;

	/**
	 * Number of revolutions completed since launch.
	 */
	public int revolutionsCompleted;

	/**
	 * Julian day of the next pass of the satellite above observer, local time.
	 * If negative that means that the satellite is eclipsed during the next pass.
	 * If zero then the event could not be calculated.
	 */
	public double nextPass;

	/**
	 * The smallest iridium angle in degrees. A given Iridium satellite will
	 * be flaring if it is not eclipsed, above the horizon, and this
	 * value is lower enough.  An empirical relationship between this angle
	 * and the brightness of the reflection has been determined (Randy John,
	 * 2002, SKYSAT v0.64, see http://home.comcast.net/~skysat). 2 deg
	 * corresponds to about 0 mag, 0.5&deg; to -3 mag. The brightest flares are
	 * -8 or -9 mag (visible during day), and can last from 10 to 30s.
	 */
	public float iridiumAngle;

	/**
	 * The smallest iridium angle in degrees in case the reflected body is
	 * the Moon.
	 */
	public float iridiumAngleForMoon;

	/**
	 * Apparent magnitude (if it is known).
	 */
	public float magnitude;

	/**
	 * Angular radius (if size is known) in radians.
	 */
	public float angularRadius;

	/**
	 * Rise time as a Julian day in local time.
	 */
	public double rise[];

	/**
	 * Set time/s as a Julian day in local time.
	 */
	public double set[];

	/**
	 * Transit (maximum elevation) time/s as a Julian day in local time.
	 */
	public double transit[];

	/**
	 * Transit geometric elevation/s from horizon in radians.
	 */
	public float transitElevation[];

	/**
	 * Relative topocentric speed in km/s.
	 */
	public float topocentricSpeed;

	/**
	 * Solar elevation in radians.
	 */
	public float sunElevation;

	/**
	 * Constant for an unknown magnitude.
	 */
	public static final int UNKNOWN_MAGNITUDE = 100;

	/**
	 * Constant for an unknown angular size.
	 */
	public static final int UNKNOWN_ANGULAR_SIZE = 100;

	/**
	 * To clone the object.
	 */
	@Override
	public SatelliteEphemElement clone()
	{
		SatelliteEphemElement s = new SatelliteEphemElement(this.name, this.rightAscension, this.declination,
				this.distance, this.azimuth, this.elevation, this.subEarthLongitude, this.subEarthLatitude,
				this.subEarthDistance, this.topocentricSpeed, this.elongation, this.illumination, this.isEclipsed,
				this.revolutionsCompleted);
		s.magnitude = this.magnitude;
		s.nextPass = this.nextPass;
		s.angularRadius = this.angularRadius;
		s.iridiumAngle = this.iridiumAngle;
		s.iridiumAngleForMoon = this.iridiumAngleForMoon;
		s.sunElevation = this.sunElevation;
		if (rise != null) s.rise = this.rise.clone();
		if (set != null) s.set = this.set.clone();
		if (transit != null) s.transit = this.transit.clone();
		if (transitElevation != null) s.transitElevation = this.transitElevation.clone();
		return s;
	}
	/**
	 * Returns if a given object is equals to this satellite
	 * ephemeris object.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof SatelliteEphemElement)) return false;

		SatelliteEphemElement that = (SatelliteEphemElement) o;

		if (Double.compare(that.rightAscension, rightAscension) != 0) return false;
		if (Double.compare(that.declination, declination) != 0) return false;
		if (Double.compare(that.distance, distance) != 0) return false;
		if (Double.compare(that.azimuth, azimuth) != 0) return false;
		if (Double.compare(that.elevation, elevation) != 0) return false;
		if (Float.compare(that.subEarthLongitude, subEarthLongitude) != 0) return false;
		if (Float.compare(that.subEarthLatitude, subEarthLatitude) != 0) return false;
		if (Float.compare(that.subEarthDistance, subEarthDistance) != 0) return false;
		if (Float.compare(that.elongation, elongation) != 0) return false;
		if (Float.compare(that.illumination, illumination) != 0) return false;
		if (isEclipsed != that.isEclipsed) return false;
		if (revolutionsCompleted != that.revolutionsCompleted) return false;
		if (Double.compare(that.nextPass, nextPass) != 0) return false;
		if (Float.compare(that.iridiumAngle, iridiumAngle) != 0) return false;
		if (Float.compare(that.iridiumAngleForMoon, iridiumAngleForMoon) != 0) return false;
		if (Float.compare(that.magnitude, magnitude) != 0) return false;
		if (Float.compare(that.angularRadius, angularRadius) != 0) return false;
		if (Float.compare(that.topocentricSpeed, topocentricSpeed) != 0) return false;
		if (Float.compare(that.sunElevation, sunElevation) != 0) return false;
		if (name != null ? !name.equals(that.name) : that.name != null) return false;
		if (!Arrays.equals(rise, that.rise)) return false;
		if (!Arrays.equals(set, that.set)) return false;
		if (!Arrays.equals(transit, that.transit)) return false;
		if (!Arrays.equals(transitElevation, that.transitElevation)) return false;

		return !(location != null ? !location.equals(that.location) : that.location != null);
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		result = name != null ? name.hashCode() : 0;
		temp = Double.doubleToLongBits(rightAscension);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(declination);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(distance);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(azimuth);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(elevation);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (subEarthLongitude != +0.0f ? Float.floatToIntBits(subEarthLongitude) : 0);
		result = 31 * result + (subEarthLatitude != +0.0f ? Float.floatToIntBits(subEarthLatitude) : 0);
		result = 31 * result + (subEarthDistance != +0.0f ? Float.floatToIntBits(subEarthDistance) : 0);
		result = 31 * result + (elongation != +0.0f ? Float.floatToIntBits(elongation) : 0);
		result = 31 * result + (illumination != +0.0f ? Float.floatToIntBits(illumination) : 0);
		result = 31 * result + (isEclipsed ? 1 : 0);
		result = 31 * result + revolutionsCompleted;
		temp = Double.doubleToLongBits(nextPass);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (iridiumAngle != +0.0f ? Float.floatToIntBits(iridiumAngle) : 0);
		result = 31 * result + (iridiumAngleForMoon != +0.0f ? Float.floatToIntBits(iridiumAngleForMoon) : 0);
		result = 31 * result + (magnitude != +0.0f ? Float.floatToIntBits(magnitude) : 0);
		result = 31 * result + (angularRadius != +0.0f ? Float.floatToIntBits(angularRadius) : 0);
		result = 31 * result + (rise != null ? Arrays.hashCode(rise) : 0);
		result = 31 * result + (set != null ? Arrays.hashCode(set) : 0);
		result = 31 * result + (transit != null ? Arrays.hashCode(transit) : 0);
		result = 31 * result + (transitElevation != null ? Arrays.hashCode(transitElevation) : 0);
		result = 31 * result + (topocentricSpeed != +0.0f ? Float.floatToIntBits(topocentricSpeed) : 0);
		result = 31 * result + (sunElevation != +0.0f ? Float.floatToIntBits(sunElevation) : 0);
		result = 31 * result + (location != null ? location.hashCode() : 0);
		return result;
	}

	private LocationElement location;
	/**
	 * Sets the location of this body in a custom coordinate system,
	 * used by JPARSEC to render the sky.
	 * @param loc Object location.
	 */
	public void setLocation(LocationElement loc) {
		this.location = loc.clone();
	}
	/**
	 * Retrieves the location of this object in a custom coordinate
	 * system, or null if the set method has not been called. Used
	 * internally by JPARSEC to render the sky.
	 * @return Location object.
	 */
	public LocationElement getLocation() {
		return this.location;
	}

	/**
	 * Returns the equatorial location of this object.
	 * Radius vector is in km, but it is set to unity in case distance is 0.
	 * @return The location in equatorial system.
	 */
	public LocationElement getEquatorialLocation() {
		double d = this.distance;
		if (d == 0.0) d = 1;
		return new LocationElement(rightAscension, declination, d);
	}
}
