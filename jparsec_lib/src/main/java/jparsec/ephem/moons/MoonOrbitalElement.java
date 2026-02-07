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

import jparsec.ephem.Target.TARGET;
import jparsec.io.FileIO;
import jparsec.math.Constant;

/**
 * An adequate class for storing orbital elements of natural
 * satellites.
 *
 * @see MoonEphem
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class MoonOrbitalElement implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs an empty moon orbital object.
	 */
	public MoonOrbitalElement()
	{
		semimajorAxis = 0.0;
		meanLongitude = 0.0;
		eccentricity = 0.0;
		periapsisLongitude = 0.0;
		ascendingNodeLongitude = 0.0;
		inclination = 0.0;
		referenceTime = 0.0;
		meanAnomaly = 0.0;
		argumentOfPeriapsis = 0.0;
		meanMotion = 0.0;
		referenceEquinox = 0.0;
		beginOfApplicableTime = 0.0;
		endOfApplicableTime = 0.0;
		referencePlane = REFERENCE_PLANE.LAPLACE;
		LaplacePoleRA = 0.0;
		LaplacePoleDEC = 0.0;
		argumentOfPeriapsisPrecessionRate = 0.0;
		ascendingNodePrecessionRate = 0.0;
		referenceEphemeris = "";
	}

	/**
	 * Constructs a moon orbit object giving the values of the main
	 * fields. Argument of periapsis is set to periapsis longitude minus
	 * ascending node longitude. Mean anomaly is set to mean longitude minus
	 * periapsis longitude. Mean motion is set to Constant.GAUSS / (sma *
	 * Math.sqrt(sma), assuming a massless object in planetocentric orbit.
	 * sma = semimajor axis.
	 * <P>
	 * Is is necessary to set also the reference equinox to get correct
	 * ephemeris.
	 *
	 * @param sma Semimajor axis in AU.
	 * @param mean_lon Mean Longitude in radians.
	 * @param ecc Eccentricity.
	 * @param peri_lon Periapsis longitude in radians.
	 * @param asc_node_lon Longitude of ascensing node in radians.
	 * @param incl Inclination in radians.
	 * @param ref_time Reference time (usually perihelion time) as a Julian day.
	 */
	public MoonOrbitalElement(double sma, double mean_lon, double ecc, double peri_lon, double asc_node_lon,
			double incl, double ref_time)
	{
		semimajorAxis = sma;
		meanLongitude = mean_lon;
		eccentricity = ecc;
		periapsisLongitude = peri_lon;
		ascendingNodeLongitude = asc_node_lon;
		inclination = incl;
		referenceTime = ref_time;
		argumentOfPeriapsis = peri_lon - asc_node_lon;
		meanAnomaly = mean_lon - peri_lon;
		meanMotion = Constant.EARTH_MEAN_ORBIT_RATE / (sma * Math.sqrt(sma));
		referencePlane = REFERENCE_PLANE.LAPLACE;
		LaplacePoleRA = 0.0;
		LaplacePoleDEC = 0.0;
		argumentOfPeriapsisPrecessionRate = 0.0;
		ascendingNodePrecessionRate = 0.0;
		referenceEphemeris = "";
	}

	/**
	 * Constructs a moon orbit object giving the values of all the
	 * fields.
	 *
	 * @param nom Name of the object
	 * @param sma Semimajor axis in AU.
	 * @param mean_anom Mean anomaly in radians.
	 * @param ecc Eccentricity.
	 * @param arg_peri Argument of periapsis in radians.
	 * @param asc_node_lon Longitude of ascensing node in radians.
	 * @param incl Inclination in radians.
	 * @param ref_time Reference time (usually perihelion time) as a Julian day.
	 * @param motion Mean motion in radians/day.
	 * @param equinox Reference equinox as a Julian day.
	 * @param init_time Begin of applicable time as a Julian day.
	 * @param final_time End of applicable time as a Julian day.
	 * @param ref_plane Reference plane for the elements.
	 * @param Laplace_RA Right ascension of the Laplace local plane.
	 * @param Laplace_DEC Declination of the Laplace local plane.
	 * @param prec_peri Precession of the periapsis, rad/day.
	 * @param prec_node Precession of the ascending node, rad/day.
	 * @param ref_ephem Ephemeris reference.
	 */
	public MoonOrbitalElement(String nom, double sma, double mean_anom, double ecc, double arg_peri,
			double asc_node_lon, double incl, double ref_time, double motion, double equinox, double init_time,
			double final_time, REFERENCE_PLANE ref_plane, double Laplace_RA, double Laplace_DEC,
			double prec_peri, double prec_node, String ref_ephem)
	{
		semimajorAxis = sma;
		meanLongitude = mean_anom + arg_peri + asc_node_lon;
		eccentricity = ecc;
		periapsisLongitude = arg_peri + asc_node_lon;
		ascendingNodeLongitude = asc_node_lon;
		inclination = incl;
		referenceTime = ref_time;
		meanMotion = motion;
		referenceEquinox = equinox;
		beginOfApplicableTime = init_time;
		endOfApplicableTime = final_time;
		argumentOfPeriapsis = arg_peri;
		meanAnomaly = mean_anom;
		name = nom;
		referencePlane = ref_plane;
		LaplacePoleRA = Laplace_RA;
		LaplacePoleDEC = Laplace_DEC;
		argumentOfPeriapsisPrecessionRate = prec_peri;
		ascendingNodePrecessionRate = prec_node;
		referenceEphemeris = ref_ephem;
	}

	/**
	 * Constructor for a set of elements as returned by Horizons in case of elements
	 * referred to 'Body Mean Equator and Node of Date'. These elements should not
	 * be used more than 2 years after or before the reference time of the elements.
	 * @param elementsFromHorizons Array of 5 strings with the elements.
	 * @param name The name of the satellite.
	 * @param motherPlanet The planet this satellite is orbiting around.
	 */
	public MoonOrbitalElement(String elementsFromHorizons[], String name, TARGET motherPlanet) {
		this();
		eccentricity = Double.parseDouble(FileIO.getField(2, elementsFromHorizons[1], " ", true));
		inclination = Double.parseDouble(FileIO.getField(6, elementsFromHorizons[1], " ", true)) * Constant.DEG_TO_RAD;
		periapsisDistance = Double.parseDouble(FileIO.getField(4, elementsFromHorizons[1], " ", true));
		referenceEquinox = Constant.J2000;
		referenceTime = Double.parseDouble(FileIO.getField(1, elementsFromHorizons[0], " ", true));
		argumentOfPeriapsis = Double.parseDouble(FileIO.getField(5, elementsFromHorizons[2], " ", true)) * Constant.DEG_TO_RAD;
		ascendingNodeLongitude = Double.parseDouble(FileIO.getField(2, elementsFromHorizons[2], " ", true)) * Constant.DEG_TO_RAD;
		meanMotion = Double.parseDouble(FileIO.getField(3, elementsFromHorizons[3], " ", true)) * Constant.DEG_TO_RAD;
		meanAnomaly = Double.parseDouble(FileIO.getField(5, elementsFromHorizons[3], " ", true)) * Constant.DEG_TO_RAD;
		centralBody = motherPlanet;
		beginOfApplicableTime = referenceTime - 2 * 365.25;
		endOfApplicableTime = referenceTime + 2 * 365.25;
		referencePlane = REFERENCE_PLANE.PLANET_EQUATOR;
		this.name = name;
		periapsisLongitude = ascendingNodeLongitude + argumentOfPeriapsis;
		meanLongitude = meanAnomaly + periapsisLongitude;
		semimajorAxis = Double.parseDouble(FileIO.getField(3, elementsFromHorizons[4], " ", true));
	}

	/**
	 * Semimajor axis of the orbit in AU.
	 */
	public double semimajorAxis;

	/**
	 * Mean longitude at reference time in radians.
	 */
	public double meanLongitude;

	/**
	 * Eccentricity.
	 */
	public double eccentricity;

	/**
	 * Perihelion longitude in radians.
	 */
	public double periapsisLongitude;

	/**
	 * Ascending node longitude in radians.
	 */
	public double ascendingNodeLongitude;

	/**
	 * Inclination of orbit in radians.
	 */
	public double inclination;

	/**
	 * Reference time in Julian Day. Usually perihelion time in comets.
	 */
	public double referenceTime;

	/**
	 * Mean anomaly = mean longitude - longitude of periapsis. Sometimes it is
	 * needed as a replacement to mean longitude. Radians.
	 */
	public double meanAnomaly;

	/**
	 * Argument of periapsis = longitude of periapsis - long. ascending node.
	 * Sometimes it is needed as a replacement to long. of perihelion. Radians.
	 */
	public double argumentOfPeriapsis;

	/**
	 * Mean motion in rad/day.
	 */
	public double meanMotion;

	/**
	 * Equinox of the orbital elements as a Julian day.
	 */
	public double referenceEquinox;

	/**
	 * Julian day of the beginning of the interval where these orbital elements
	 * are applicable. Currently used only for space probes.
	 */
	public double beginOfApplicableTime;

	/**
	 * Name of the object.
	 */
	public String name;

	/**
	 * Julian day of the ending of the interval where these orbital elements are
	 * applicable. Currently used only for space probes.
	 */
	public double endOfApplicableTime;

	/**
	 * Perihelion distance in AU for comets.
	 */
	public double periapsisDistance;

	/**
	 * Central body ID.
	 */
	public TARGET centralBody;

	/**
	 * Reference plane for the elements.
	 */
	public REFERENCE_PLANE referencePlane;

	/**
	 * Right ascension of the local Laplace plane.
	 */
	public double LaplacePoleRA;

	/**
	 * Declination of the local Laplace plane.
	 */
	public double LaplacePoleDEC;

	/**
	 * Precession rate of the periapsis, rad/day.
	 */
	public double argumentOfPeriapsisPrecessionRate;

	/**
	 * Precession rate of the ascending node, rad/day.
	 */
	public double ascendingNodePrecessionRate;

	/**
	 * Reference ephemeris.
	 */
	public String referenceEphemeris;

	/**
	 * The different reference planes for the elements.
	 */
	public enum REFERENCE_PLANE {
		/** Laplace reference plane (defined by the angular momentum vector of the Solar System). */
		LAPLACE,
		/** Ecliptic reference plane for the elements. */
		ECLIPTIC,
		/** Earth's equator reference plane for the elements. */
		EQUATOR,
		/** Planet equator reference plane for the elements. Horizons can provide elements
		 * for this plane, as 'Body Mean Equator and Node of Date'. */
		PLANET_EQUATOR};

	/**
	 * To clone the object.
	 */
	@Override
	public MoonOrbitalElement clone()
	{
		MoonOrbitalElement orbit = new MoonOrbitalElement(this.name, this.semimajorAxis, this.meanAnomaly,
				this.eccentricity, this.argumentOfPeriapsis, this.ascendingNodeLongitude, this.inclination,
				this.referenceTime, this.meanMotion, this.referenceEquinox, this.beginOfApplicableTime,
				this.endOfApplicableTime, this.referencePlane,
				this.LaplacePoleRA, this.LaplacePoleDEC, this.argumentOfPeriapsisPrecessionRate,
				this.ascendingNodePrecessionRate, this.referenceEphemeris);

		orbit.centralBody = this.centralBody;
		orbit.periapsisDistance = this.periapsisDistance;
		orbit.periapsisLongitude = this.periapsisLongitude;
		return orbit;
	}
	/**
	 * Returns true if the input object is equals to this moon orbital
	 * element object..
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof MoonOrbitalElement)) return false;

		MoonOrbitalElement that = (MoonOrbitalElement) o;

		if (Double.compare(that.semimajorAxis, semimajorAxis) != 0) return false;
		if (Double.compare(that.meanLongitude, meanLongitude) != 0) return false;
		if (Double.compare(that.eccentricity, eccentricity) != 0) return false;
		if (Double.compare(that.periapsisLongitude, periapsisLongitude) != 0) return false;
		if (Double.compare(that.ascendingNodeLongitude, ascendingNodeLongitude) != 0) return false;
		if (Double.compare(that.inclination, inclination) != 0) return false;
		if (Double.compare(that.referenceTime, referenceTime) != 0) return false;
		if (Double.compare(that.meanAnomaly, meanAnomaly) != 0) return false;
		if (Double.compare(that.argumentOfPeriapsis, argumentOfPeriapsis) != 0) return false;
		if (Double.compare(that.meanMotion, meanMotion) != 0) return false;
		if (Double.compare(that.referenceEquinox, referenceEquinox) != 0) return false;
		if (Double.compare(that.beginOfApplicableTime, beginOfApplicableTime) != 0) return false;
		if (Double.compare(that.endOfApplicableTime, endOfApplicableTime) != 0) return false;
		if (Double.compare(that.periapsisDistance, periapsisDistance) != 0) return false;
		if (Double.compare(that.LaplacePoleRA, LaplacePoleRA) != 0) return false;
		if (Double.compare(that.LaplacePoleDEC, LaplacePoleDEC) != 0) return false;
		if (Double.compare(that.argumentOfPeriapsisPrecessionRate, argumentOfPeriapsisPrecessionRate) != 0)
			return false;
		if (Double.compare(that.ascendingNodePrecessionRate, ascendingNodePrecessionRate) != 0) return false;
		if (name != null ? !name.equals(that.name) : that.name != null) return false;
		if (centralBody != that.centralBody) return false;
		if (referencePlane != that.referencePlane) return false;

		return !(referenceEphemeris != null ? !referenceEphemeris.equals(that.referenceEphemeris) : that.referenceEphemeris != null);
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		temp = Double.doubleToLongBits(semimajorAxis);
		result = (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(meanLongitude);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(eccentricity);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(periapsisLongitude);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(ascendingNodeLongitude);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(inclination);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(referenceTime);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(meanAnomaly);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(argumentOfPeriapsis);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(meanMotion);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(referenceEquinox);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(beginOfApplicableTime);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (name != null ? name.hashCode() : 0);
		temp = Double.doubleToLongBits(endOfApplicableTime);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(periapsisDistance);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (centralBody != null ? centralBody.hashCode() : 0);
		result = 31 * result + (referencePlane != null ? referencePlane.hashCode() : 0);
		temp = Double.doubleToLongBits(LaplacePoleRA);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(LaplacePoleDEC);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(argumentOfPeriapsisPrecessionRate);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(ascendingNodePrecessionRate);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (referenceEphemeris != null ? referenceEphemeris.hashCode() : 0);
		return result;
	}
}
