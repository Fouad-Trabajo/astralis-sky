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
package jparsec.ephem.stars;

import java.io.Serializable;

import jparsec.observer.LocationElement;

/**
 * Convenient class for star ephem data access.
 * <P>
 * This class provides access to the data resulting from the
 * calculation of ephemerides of stars.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class StarEphemElement implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs an star ephem object providing the values of the fields.
	 * This sets the values of all the fields except light rise, set, transit,
	 * transit elevation, and constellation.
	 *
	 * @param ra Right Ascension in radians.
	 * @param dec Declination in radians.
	 * @param r Distance to the observer in AU.
	 * @param mag Apparent magnitude.
	 * @param paralactic Paralactic angle in radians.
	 * @param azi Azimuth in radians.
	 * @param altitude Elevation in radians.
	 */
	public StarEphemElement(double ra, double dec, double r, float mag, float paralactic, double azi, double altitude)
	{
		rightAscension = ra;
		declination = dec;
		distance = r;
		magnitude = mag;
		paralacticAngle = paralactic;
		elevation = altitude;
		azimuth = azi;
	}

	/**
	 * Constructs an empty star ephem object.
	 */
	public StarEphemElement()
	{
		rightAscension = 0.0;
		declination = 0.0;
		distance = 0.0;
		magnitude = 0.0f;
		paralacticAngle = 0.0f;
		elevation = 0.0;
		azimuth = 0.0;
	}

	/**
	 * Right Ascension in radians.
	 */
	public double rightAscension;

	/**
	 * Declination in radians.
	 */
	public double declination;

	/**
	 * Distance to the observer in pc.
	 */
	public double distance;

	/**
	 * Apparent magnitude.
	 */
	public float magnitude;

	/**
	 * Paralactic angle in radians.
	 */
	public float paralacticAngle;

	/**
	 * Azimuth in radians.
	 */
	public double azimuth;

	/**
	 * Geometric/apparent elevation in radians.
	 */
	public double elevation;

	/**
	 * Rise time as a julian day in local time. If the object is above the
	 * horizon, then the value will be refered to the current day, otherwise it
	 * will be the next rise event in time.
	 */
	public double rise;

	/**
	 * Set time as a julian day in local time. If the object is above the
	 * horizon, then the value will be refered to the current day, otherwise it
	 * will be the next set event in time.
	 */
	public double set;

	/**
	 * Transit time as a julian day in local time. If the object is above the
	 * horizon, then the value will be refered to the current day, otherwise it
	 * will be the next transit event in time.
	 */
	public double transit;

	/**
	 * Transit geometrical elevation from horizon in radians.
	 */
	public float transitElevation;

	/**
	 * Constellation where the object is located.
	 */
	public String constellation;

	/**
	 * Name of the star.
	 */
	public String name;

	/**
	 * Returns the equatorial location of this object.
	 * Radius vector is in pc, but it is set to unity in case distance is 0.
	 * @return The location in equatorial system.
	 */
	public LocationElement getEquatorialLocation() {
		double d = this.distance;
		if (d == 0.0) d = 1;
		return new LocationElement(rightAscension, declination, d);
	}

	/**
	 * To clone the object.
	 */
	@Override
	public StarEphemElement clone()
	{
		StarEphemElement s = new StarEphemElement();
		s.azimuth = this.azimuth;
		s.constellation = this.constellation;
		s.declination = this.declination;
		s.distance = this.distance;
		s.elevation = this.elevation;
		s.magnitude = this.magnitude;
		s.paralacticAngle = this.paralacticAngle;
		s.rightAscension = this.rightAscension;
		s.rise = this.rise;
		s.set = this.set;
		s.transit = this.transit;
		s.transitElevation = this.transitElevation;
		s.name = this.name;

		return s;
	}
	/**
	 * Returns true if the input object is equals to this
	 * instance.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof StarEphemElement)) return false;

		StarEphemElement that = (StarEphemElement) o;

		if (Double.compare(that.rightAscension, rightAscension) != 0) return false;
		if (Double.compare(that.declination, declination) != 0) return false;
		if (Double.compare(that.distance, distance) != 0) return false;
		if (Float.compare(that.magnitude, magnitude) != 0) return false;
		if (Float.compare(that.paralacticAngle, paralacticAngle) != 0) return false;
		if (Double.compare(that.azimuth, azimuth) != 0) return false;
		if (Double.compare(that.elevation, elevation) != 0) return false;
		if (Double.compare(that.rise, rise) != 0) return false;
		if (Double.compare(that.set, set) != 0) return false;
		if (Double.compare(that.transit, transit) != 0) return false;
		if (Float.compare(that.transitElevation, transitElevation) != 0) return false;
		if (constellation != null ? !constellation.equals(that.constellation) : that.constellation != null)
			return false;

		return !(name != null ? !name.equals(that.name) : that.name != null);
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
		result = 31 * result + (magnitude != +0.0f ? Float.floatToIntBits(magnitude) : 0);
		result = 31 * result + (paralacticAngle != +0.0f ? Float.floatToIntBits(paralacticAngle) : 0);
		temp = Double.doubleToLongBits(azimuth);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(elevation);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(rise);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(set);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(transit);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (transitElevation != +0.0f ? Float.floatToIntBits(transitElevation) : 0);
		result = 31 * result + (constellation != null ? constellation.hashCode() : 0);
		result = 31 * result + (name != null ? name.hashCode() : 0);
		return result;
	}
};
