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

import java.io.Serializable;

import jparsec.ephem.planets.EphemElement;
import jparsec.observer.LocationElement;

/**
 * Convenient class to store results of ephemeris of natural satellites.
 *
 * @see MoonEphem
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class MoonEphemElement implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a MoonEphem object giving all the data except angular
	 * radius, magnitude, ecliptic position, phase angle, and axis orientation.
	 *
	 * @param nom Name of the satellite.
	 * @param ra Right ascension.
	 * @param dec Declination.
	 * @param dist Distance.
	 * @param dist_sun Distance from the Sun.
	 * @param azi Azimuth.
	 * @param ele Elevation.
	 * @param ill Illumination fraction.
	 * @param elo Elongation in radians.
	 * @param ecl True if the satellite is not illuminated.
	 * @param ocl True if the satellite is oculted.
	 * @param tran True is the satellite is transiting as seen by the
	 *        observer.
	 * @param shadow_transiting True if the shadow is transiting.
	 * @param inf True if z is lower than 0.0.
	 * @param x X position in units of equatorial radii respect to planet and
	 *        the sky plane.
	 * @param y Y position in units of equatorial radii respect to planet and
	 *        the sky plane.
	 * @param z Z position in units of equatorial radii respect to planet and
	 *        the sky plane.
	 * @param x_sun X position in units of equatorial radii respect to Sun and
	 *        the sky plane.
	 * @param y_sun Y position in units of equatorial radii respect to Sun and
	 *        the sky plane.
	 * @param z_sun Z position in units of equatorial radii respect to Sun and
	 *        the sky plane.
	 */
	public MoonEphemElement(String nom, double ra, double dec, double dist, double dist_sun, double azi, double ele,
			float ill, float elo, boolean ecl, boolean ocl, boolean tran, boolean shadow_transiting,
			boolean inf, double x, double y, double z, double x_sun, double y_sun, double z_sun)
	{
		name = nom;
		rightAscension = ra;
		declination = dec;
		distance = dist;
		azimuth = azi;
		elevation = ele;
		distanceFromSun = dist_sun;
		eclipsed = ecl;
		occulted = ocl;
		shadowTransiting = shadow_transiting;
		transiting = tran;
		inferior = inf;
		xPosition = x;
		yPosition = y;
		zPosition = z;
		xPositionFromSun = x_sun;
		yPositionFromSun = y_sun;
		zPositionFromSun = z_sun;
		phase = ill;
		elongation = elo;
		mutualPhenomena = "";
	}

	/**
	 * Constructor of an empty object.
	 */
	public MoonEphemElement()
	{
		name = "";
		rightAscension = 0.0;
		declination = 0.0;
		distance = 0.0;
		azimuth = 0.0;
		elevation = 0.0;
		eclipsed = false;
		occulted = false;
		transiting = false;
		inferior = false;
		xPosition = 0.0;
		yPosition = 0.0;
		zPosition = 0.0;
		xPositionFromSun = 0.0;
		yPositionFromSun = 0.0;
		zPositionFromSun = 0.0;
		phase = 0.0f;
		elongation = 0.0f;
		magnitude = 0.0f;
		angularRadius = 0.0f;
		heliocentricEclipticLongitude = 0.0;
		heliocentricEclipticLatitude = 0.0;
		phaseAngle = 0.0f;
		paralacticAngle = 0.0f;
		mutualPhenomena = "";
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
	 * Distance in AU.
	 */
	public double distance;

	/**
	 * Distance from the Sun in Astronomical Units.
	 */
	public double distanceFromSun;

	/**
	 * Angular radius in radians.
	 */
	public float angularRadius;

	/**
	 * Azimuth in radians.
	 */
	public double azimuth;

	/**
	 * Elevation in radians.
	 */
	public double elevation;

	/**
	 * Elongation in radians.
	 */
	public float elongation;

	/**
	 * Apparent magnitude.
	 */
	public float magnitude;

	/**
	 * Visible phase percentage.
	 */
	public float phase;

	/**
	 * True if the center of the satellite is not illuminated by the Sun.
	 */
	public boolean eclipsed;

	/**
	 * True if the center of the satellite is in front of the planet as seen
	 * from the Sun.
	 */
	public boolean shadowTransiting;

	/**
	 * True if the center of the satellite is behind the planet as seen by the
	 * observer.
	 */
	public boolean occulted;

	/**
	 * True if the center of the satellite is in front of the planet as seen by
	 * the observer.
	 */
	public boolean transiting;

	/**
	 * True if the satellite is in front of the planet as seen from Earth, with
	 * z position lower than 0.0.
	 */
	public boolean inferior;

	/**
	 * Holds apparent x position of the satellite respect to the axis of the
	 * mother planet and the sky plane, in equatorial radii units, as seen from
	 * the observer.
	 */
	public double xPosition;

	/**
	 * Holds apparent y position of the satellite respect to the axis of the
	 * mother planet and the sky plane, in equatorial radii units, as seen from
	 * the observer.
	 */
	public double yPosition;

	/**
	 * Holds apparent z position of the satellite respect to the axis of the
	 * mother planet and the sky plane, in equatorial radii units, as seen from
	 * the observer.
	 */
	public double zPosition;

	/**
	 * Holds apparent x position of the satellite respect to the axis of the
	 * mother planet and the sky plane, in equatorial radii units, as seen from
	 * the Sun.
	 */
	public double xPositionFromSun;

	/**
	 * Holds apparent y position of the satellite respect to the axis of the
	 * mother planet and the sky plane, in equatorial radii units, as seen from
	 * the Sun.
	 */
	public double yPositionFromSun;

	/**
	 * Holds apparent z position of the satellite respect to the axis of the
	 * mother planet and the sky plane, in equatorial radii units, as seen from
	 * the Sun.
	 */
	public double zPositionFromSun;

	/**
	 * Holds a description of the mutual phenomena in a given instant.
	 * Format is ECLIPSED/OCCULTED BY and the name of the object producing
	 * the event. The object can be another satellite or the mother planet,
	 * the last case only when the eclipse/occultation is currently partial.
	 */
	public String mutualPhenomena;

	/**
	 * Position angle of axis in radians.
	 */
	public double positionAngleOfAxis;

	/**
	 * Position angle of pole in radians.
	 */
	public double positionAngleOfPole;

	/**
	 * Longitude of central meridian in radians. For giant planets (Jupiter,
	 * Saturn, Uranus, and Neptune) the value will be refered to system III of
	 * coordinates.
	 */
	public double longitudeOfCentralMeridian;

	/**
	 * Bright limb angle in radians.
	 */
	public float brightLimbAngle;

	/**
	 * Subsolar latitude. In case of Saturn, Uranus, and Neptune, can be
	 * considered the ring plane ilumination angle. It depends on the relative
	 * position of the Sun.
	 */
	public double subsolarLatitude;

	/**
	 * Subsolar longitude. It depends on the relative position of the Sun. In
	 * giant planets the value is refered to System III of coordinates.
	 */
	public double subsolarLongitude;

	/**
	 * Right ascension of the north pole of rotation.
	 */
	public double northPoleRA;

	/**
	 * Declination of the north pole of rotation.
	 */
	public double northPoleDEC;

	/**
	 * Heliocentric ecliptic longitude in radians.
	 */
	public double heliocentricEclipticLongitude;

	/**
	 * Heliocentric ecliptic latitude in radians.
	 */
	public double heliocentricEclipticLatitude;

	/**
	 * Phase angle in radians.
	 */
	public float phaseAngle;

	/**
	 * Paralactic angle in radians.
	 */
	public float paralacticAngle;

	/**
	 * Transform the corresponding information in an Ephem object into a
	 * moon ephem object.
	 *
	 * @param ephem Ephem object.
	 * @return Moon ephem object.
	 */
	public static MoonEphemElement parseEphemElement(EphemElement ephem)
	{
		MoonEphemElement moon_ephem = new MoonEphemElement("", ephem.rightAscension, ephem.declination,
				ephem.distance, ephem.distanceFromSun, ephem.azimuth, ephem.elevation, ephem.phase, ephem.elongation,
				false, false, false, false, false, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);

		moon_ephem.magnitude = ephem.magnitude;
		moon_ephem.angularRadius = ephem.angularRadius;
		moon_ephem.positionAngleOfAxis = ephem.positionAngleOfAxis;
		moon_ephem.positionAngleOfPole = ephem.positionAngleOfPole;
		moon_ephem.brightLimbAngle = ephem.brightLimbAngle;
		moon_ephem.subsolarLatitude = ephem.subsolarLatitude;
		moon_ephem.subsolarLongitude = ephem.subsolarLongitude;
		moon_ephem.longitudeOfCentralMeridian = ephem.longitudeOfCentralMeridian;
		moon_ephem.northPoleDEC = ephem.northPoleDEC;
		moon_ephem.northPoleRA = ephem.northPoleRA;
		moon_ephem.heliocentricEclipticLatitude = ephem.heliocentricEclipticLatitude;
		moon_ephem.heliocentricEclipticLongitude = ephem.heliocentricEclipticLongitude;
		moon_ephem.phaseAngle = ephem.phaseAngle;
		moon_ephem.paralacticAngle = ephem.paralacticAngle;
		moon_ephem.mutualPhenomena = "";

		return moon_ephem;
	}

	/**
	 * To clone the object.
	 */
	@Override
	public MoonEphemElement clone()
	{
		MoonEphemElement moon = new MoonEphemElement();
		moon.rightAscension = this.rightAscension;
		moon.name = this.name;
		moon.declination = this.declination;
		moon.distance = this.distance;
		moon.azimuth = this.azimuth;
		moon.angularRadius = this.angularRadius;
		moon.brightLimbAngle = this.brightLimbAngle;
		moon.distanceFromSun = this.distanceFromSun;
		moon.eclipsed = this.eclipsed;
		moon.elevation = this.elevation;
		moon.elongation = this.elongation;
		moon.heliocentricEclipticLatitude = this.heliocentricEclipticLatitude;
		moon.heliocentricEclipticLongitude = this.heliocentricEclipticLongitude;
		moon.inferior = this.inferior;
		moon.longitudeOfCentralMeridian = this.longitudeOfCentralMeridian;
		moon.magnitude = this.magnitude;
		moon.northPoleDEC = this.northPoleDEC;
		moon.northPoleRA = this.northPoleRA;
		moon.occulted = this.occulted;
		moon.paralacticAngle = this.paralacticAngle;
		moon.phase = this.phase;
		moon.phaseAngle = this.phaseAngle;
		moon.mutualPhenomena = this.mutualPhenomena;
		moon.positionAngleOfAxis = this.positionAngleOfAxis;
		moon.positionAngleOfPole = this.positionAngleOfPole;
		moon.shadowTransiting = this.shadowTransiting;
		moon.subsolarLatitude = this.subsolarLatitude;
		moon.subsolarLongitude = this.subsolarLongitude;
		moon.transiting = this.transiting;
		moon.xPosition = this.xPosition;
		moon.xPositionFromSun = this.xPositionFromSun;
		moon.yPosition = this.yPosition;
		moon.yPositionFromSun = this.yPositionFromSun;
		moon.zPosition = this.zPosition;
		moon.zPositionFromSun = this.zPositionFromSun;
		return moon;
	}
	/**
	 * Return true if the object is equals to another moon
	 * ephem object.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof MoonEphemElement)) return false;

		MoonEphemElement that = (MoonEphemElement) o;

		if (Double.compare(that.rightAscension, rightAscension) != 0) return false;
		if (Double.compare(that.declination, declination) != 0) return false;
		if (Double.compare(that.distance, distance) != 0) return false;
		if (Double.compare(that.distanceFromSun, distanceFromSun) != 0) return false;
		if (Float.compare(that.angularRadius, angularRadius) != 0) return false;
		if (Double.compare(that.azimuth, azimuth) != 0) return false;
		if (Double.compare(that.elevation, elevation) != 0) return false;
		if (Float.compare(that.elongation, elongation) != 0) return false;
		if (Float.compare(that.magnitude, magnitude) != 0) return false;
		if (Float.compare(that.phase, phase) != 0) return false;
		if (eclipsed != that.eclipsed) return false;
		if (shadowTransiting != that.shadowTransiting) return false;
		if (occulted != that.occulted) return false;
		if (transiting != that.transiting) return false;
		if (inferior != that.inferior) return false;
		if (Double.compare(that.xPosition, xPosition) != 0) return false;
		if (Double.compare(that.yPosition, yPosition) != 0) return false;
		if (Double.compare(that.zPosition, zPosition) != 0) return false;
		if (Double.compare(that.xPositionFromSun, xPositionFromSun) != 0) return false;
		if (Double.compare(that.yPositionFromSun, yPositionFromSun) != 0) return false;
		if (Double.compare(that.zPositionFromSun, zPositionFromSun) != 0) return false;
		if (Double.compare(that.positionAngleOfAxis, positionAngleOfAxis) != 0) return false;
		if (Double.compare(that.positionAngleOfPole, positionAngleOfPole) != 0) return false;
		if (Double.compare(that.longitudeOfCentralMeridian, longitudeOfCentralMeridian) != 0) return false;
		if (Float.compare(that.brightLimbAngle, brightLimbAngle) != 0) return false;
		if (Double.compare(that.subsolarLatitude, subsolarLatitude) != 0) return false;
		if (Double.compare(that.subsolarLongitude, subsolarLongitude) != 0) return false;
		if (Double.compare(that.northPoleRA, northPoleRA) != 0) return false;
		if (Double.compare(that.northPoleDEC, northPoleDEC) != 0) return false;
		if (Double.compare(that.heliocentricEclipticLongitude, heliocentricEclipticLongitude) != 0) return false;
		if (Double.compare(that.heliocentricEclipticLatitude, heliocentricEclipticLatitude) != 0) return false;
		if (Float.compare(that.phaseAngle, phaseAngle) != 0) return false;
		if (Float.compare(that.paralacticAngle, paralacticAngle) != 0) return false;
		if (name != null ? !name.equals(that.name) : that.name != null) return false;

		return !(mutualPhenomena != null ? !mutualPhenomena.equals(that.mutualPhenomena) : that.mutualPhenomena != null);
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
		temp = Double.doubleToLongBits(distanceFromSun);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (angularRadius != +0.0f ? Float.floatToIntBits(angularRadius) : 0);
		temp = Double.doubleToLongBits(azimuth);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(elevation);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (elongation != +0.0f ? Float.floatToIntBits(elongation) : 0);
		result = 31 * result + (magnitude != +0.0f ? Float.floatToIntBits(magnitude) : 0);
		result = 31 * result + (phase != +0.0f ? Float.floatToIntBits(phase) : 0);
		result = 31 * result + (eclipsed ? 1 : 0);
		result = 31 * result + (shadowTransiting ? 1 : 0);
		result = 31 * result + (occulted ? 1 : 0);
		result = 31 * result + (transiting ? 1 : 0);
		result = 31 * result + (inferior ? 1 : 0);
		temp = Double.doubleToLongBits(xPosition);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(yPosition);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(zPosition);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(xPositionFromSun);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(yPositionFromSun);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(zPositionFromSun);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (mutualPhenomena != null ? mutualPhenomena.hashCode() : 0);
		temp = Double.doubleToLongBits(positionAngleOfAxis);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(positionAngleOfPole);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(longitudeOfCentralMeridian);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (brightLimbAngle != +0.0f ? Float.floatToIntBits(brightLimbAngle) : 0);
		temp = Double.doubleToLongBits(subsolarLatitude);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(subsolarLongitude);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(northPoleRA);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(northPoleDEC);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(heliocentricEclipticLongitude);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(heliocentricEclipticLatitude);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (phaseAngle != +0.0f ? Float.floatToIntBits(phaseAngle) : 0);
		result = 31 * result + (paralacticAngle != +0.0f ? Float.floatToIntBits(paralacticAngle) : 0);
		return result;
	}

	/**
	 * Retrieves the location of this object in the equatorial
	 * system. Radius vector is set to unity in case distance is 0.
	 * @return Location object.
	 */
	public LocationElement getEquatorialLocation() {
		double d = this.distance;
		if (d == 0.0) d = 1;
		return new LocationElement(this.rightAscension, this.declination, d);
	}
}
