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

import java.awt.Color;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.Arrays;

import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.EphemerisElement.FRAME;
import jparsec.ephem.Functions;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.moons.MoonOrbitalElement;
import jparsec.ephem.moons.MoonPhysicalParameters;
import jparsec.ephem.planets.imcce.Vsop;
import jparsec.graph.ChartElement;
import jparsec.graph.ChartSeriesElement;
import jparsec.graph.CreateChart;
import jparsec.graph.DataSet;
import jparsec.graph.JPARSECStroke;
import jparsec.graph.chartRendering.AWTGraphics;
import jparsec.graph.chartRendering.Graphics;
import jparsec.graph.chartRendering.Graphics.FONT;
import jparsec.io.FileIO;
import jparsec.math.Constant;
import jparsec.math.FastMath;
import jparsec.math.Interpolation;
import jparsec.observer.City;
import jparsec.observer.CityElement;
import jparsec.observer.ObserverElement;
import jparsec.time.AstroDate;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.time.TimeScale;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;
import jparsec.util.Translate.LANGUAGE;

/**
 * An adequate class for storing orbital elements in the FK5 system.<P>
 *
 * This class is suitable for storing orbital elements of planets, comets,
 * asteroids, and space probes. Elements should be referred to mean equinox
 * and ecliptic of Earth, for a given equinox.
 *
 * @see Ephem
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class OrbitalElement implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * The different magnitude models for comets and asteroids.
	 */
	public enum MAGNITUDE_MODEL {
		/** The simple g, k model. In this model the absolute magnitude, g, is the visual magnitude of the object
		 * if it were one AU from both the Sun and the Earth. The other, the luminosity index, k, characterizes
		 * the brightness change of the object as a function of its distance from the Sun. This is generally zero,
		 * or very small, for inactive objects like asteroids. This model is used for comets. */
		COMET_gk,
		/** The H, G model involving the calculation of some exp. The first parameter, H, is the magnitude of the
		 * object when one AU from the Sun and the Earth. The other, G, attempts to model the reflection
		 * characteristics of a passive surface, such as an asteroid. This model is used for asteroid and natural
		 * satellites. */
		ASTEROID_HG,
		/** ID constant to calculate no magnitude for the minor object
		 * (will be 0). */
		NONE;

		/** Default value the apparent magnitude will not be computed. */
		public static final int MAGNITUDE_UNKNOWN = EphemElement.INVALID_MAGNITUDE; // 100
	};

	/**
	 * Constructs an empty {@linkplain OrbitalElement} object.
	 */
	public OrbitalElement() {
			semimajorAxis=0.0; meanLongitude=0.0; eccentricity=0.0;
			perihelionLongitude=0.0; ascendingNodeLongitude=0.0; inclination=0.0;
		  referenceTime=0.0; meanAnomaly=0.0; argumentOfPerihelion=0.0; meanMotion=0.0;
		  referenceEquinox=0.0; beginOfApplicableTime=0.0; endOfApplicableTime=0.0;
		  absoluteMagnitude=0.0f;centralBody=TARGET.SUN;magnitudeSlope=0.0f;perihelionDistance=0.0;
		  referenceFrame = FRAME.FK5;magnitudeModel = MAGNITUDE_MODEL.NONE;}

	/**
	 * Constructs an {@linkplain OrbitalElement} object giving the values of the main fields.
	 * Argument of perihelion is set to perihelion longitude minus ascending node
	 * longitude. Mean anomaly is set to mean longitude minus perihelion longitude.
	 * Mean motion is set to Constant.GAUSS / (sma * Math.sqrt(sma), assuming a
	 * massless object in heliocentric orbit.<P>
	 *
	 * Is is necessary to set also the reference equinox and frame (J2000 and FK5
	 * by default) to get correct ephemeris.
	 *
	 * @param sma Semimajor axis in AU.
	 * @param mean_lon Mean Longitude in radians.
	 * @param ecc Eccentricity.
	 * @param perih_lon Perihelion longitude in radians.
	 * @param asc_node_lon Longitude of ascensing node in radians.
	 * @param incl Inclination in radians.
	 * @param ref_time Reference time (usually perihelion time) as a Julian day.
	 */
	public OrbitalElement( double sma, double mean_lon,
		  double ecc,  double perih_lon, double asc_node_lon,
		  double incl, double ref_time) {
		semimajorAxis=sma; meanLongitude=mean_lon; eccentricity=ecc;
		perihelionLongitude=perih_lon;ascendingNodeLongitude=asc_node_lon; inclination=incl;
	  referenceTime=ref_time;argumentOfPerihelion=perih_lon-asc_node_lon;
	  meanAnomaly=mean_lon-perih_lon;
	  meanMotion = Constant.EARTH_MEAN_ORBIT_RATE / (sma * Math.sqrt(sma));
	  perihelionDistance = 0.0;
	  if (ecc < 1.0) perihelionDistance = sma * (1.0 - ecc);
	  referenceEquinox = Constant.J2000;
	  referenceFrame = FRAME.FK5;
	  magnitudeModel = MAGNITUDE_MODEL.NONE;
	}

	/**
	 * Constructs an {@linkplain OrbitalElement} object giving the values of most of the fields.
	 * Argument of perihelion is set to perihelion longitude minus ascending node
	 * longitude. Mean anomaly is set to mean longitude minus perihelion longitude. Frame is FK5.
	 *
	 * @param nom Name of the object
	 * @param sma Semimajor axis in AU.
	 * @param mean_lon Mean Longitude in radians.
	 * @param ecc Eccentricity.
	 * @param perih_lon Perihelion longitude in radians.
	 * @param asc_node_lon Longitude of ascensing node in radians.
	 * @param incl Inclination in radians.
	 * @param ref_time Reference time (usually perihelion time) as a Julian day.
	 * @param motion Mean motion in radians/day.
	 * @param equinox Reference equinox as a Julian day.
	 * @param init_time Begin of applicable time as a Julian day.
	 * @param final_time End of applicable time as a Julian day.
	 * @param mabs Absolute magnitude.
	 * @param slope Magnitude slope.
	 * @param magModel The model to apply for the magnitude.
	 */
	public OrbitalElement( String nom, double sma, double mean_lon,
		  double ecc,  double perih_lon, double asc_node_lon,
		  double incl, double ref_time, double motion, double equinox,
		  double init_time, double final_time, float mabs, float slope,
		  MAGNITUDE_MODEL magModel) {
		semimajorAxis=sma; meanLongitude=mean_lon; eccentricity=ecc;
		perihelionLongitude=perih_lon;ascendingNodeLongitude=asc_node_lon; inclination=incl;
	  referenceTime=ref_time;meanMotion=motion;referenceEquinox=equinox;
	  beginOfApplicableTime=init_time;endOfApplicableTime=final_time;
	  argumentOfPerihelion=perih_lon-asc_node_lon;meanAnomaly=mean_lon-perih_lon;
	  name=nom;absoluteMagnitude=mabs;magnitudeSlope=slope;
	  perihelionDistance = 0.0;
	  if (ecc < 1.0) perihelionDistance = sma * (1.0 - ecc);
	  referenceFrame = FRAME.FK5;
	  magnitudeModel = magModel;
	  }

	/**
	 * Constructs an {@linkplain OrbitalElement} object giving the values of most of the fields, but
	 * in a different way sometimes used. Frame is FK5.
	 *
	 * @param nom Name of the object
	 * @param sma Semimajor axis in AU.
	 * @param arg_perih Argument of perihelion.
	 * @param ecc Eccentricity.
	 * @param mean_anomaly Mean anomaly in radians.
	 * @param asc_node_lon Longitude of ascensing node in radians.
	 * @param incl Inclination in radians.
	 * @param ref_time Reference time (usually perihelion time) as a Julian day.
	 * @param motion Mean motion in radians/day.
	 * @param equinox Reference equinox as a Julian day.
	 * @param init_time Begin of applicable time as a Julian day.
	 * @param final_time End of applicable time as a Julian day.
	 */
	public OrbitalElement( String nom, double sma, double arg_perih,
		  double ecc,  double mean_anomaly, double asc_node_lon,
		  double incl, double ref_time, double motion, double equinox,
		  double init_time, double final_time) {
		semimajorAxis=sma; meanLongitude=mean_anomaly+arg_perih+asc_node_lon; eccentricity=ecc;
		perihelionLongitude=arg_perih + asc_node_lon;ascendingNodeLongitude=asc_node_lon; inclination=incl;
	  referenceTime=ref_time;meanMotion=motion;referenceEquinox=equinox;
	  beginOfApplicableTime=init_time;endOfApplicableTime=final_time;
	  argumentOfPerihelion=arg_perih;meanAnomaly=mean_anomaly;
	  name=nom;absoluteMagnitude=0.0f;magnitudeSlope=0.0f;
	  perihelionDistance = 0.0;
	  if (ecc < 1.0) perihelionDistance = sma * (1.0 - ecc);
	  referenceFrame = FRAME.FK5;
	  magnitudeModel = MAGNITUDE_MODEL.NONE;
	  }

	/**
	 * Constructs an {@linkplain OrbitalElement} object giving the values of all the fields
	 * except the frame, which is FK5.
	 *
	 * @param nom Name of the object
	 * @param sma Semimajor axis in AU.
	 * @param arg_perih Argument of perihelion.
	 * @param ecc Eccentricity.
	 * @param mean_anomaly Mean anomaly in radians.
	 * @param asc_node_lon Longitude of ascensing node in radians.
	 * @param incl Inclination in radians.
	 * @param ref_time Reference time (usually perihelion time) as a Julian day.
	 * @param motion Mean motion in radians/day.
	 * @param equinox Reference equinox as a Julian day.
	 * @param init_time Begin of applicable time as a Julian day.
	 * @param final_time End of applicable time as a Julian day.
	 * @param mean_lon Mean longitude.
	 * @param perih_lon Perihelion longitude.
	 * @param perih_dist Perihelion distance.
	 * @param abs_mag Absolute magnitude.
	 * @param mag_slope Magnitude slope.
	 * @param magModel The model to apply for the magnitude.
	 */
	public OrbitalElement( String nom, double sma, double arg_perih,
		  double ecc,  double mean_anomaly, double asc_node_lon,
		  double incl, double ref_time, double motion, double equinox,
		  double init_time, double final_time, double mean_lon, double perih_lon,
		  double perih_dist, float abs_mag, float mag_slope, MAGNITUDE_MODEL magModel) {
		name=nom; semimajorAxis=sma; argumentOfPerihelion=arg_perih; eccentricity=ecc;
		meanAnomaly=mean_anomaly; ascendingNodeLongitude=asc_node_lon; inclination=incl;
		referenceTime=ref_time;meanMotion=motion;referenceEquinox=equinox;
		beginOfApplicableTime=init_time;endOfApplicableTime=final_time;
		meanLongitude=mean_lon; perihelionLongitude=perih_lon;
		perihelionDistance = perih_dist;
		absoluteMagnitude=abs_mag;magnitudeSlope=mag_slope;
		referenceFrame = FRAME.FK5;
		magnitudeModel = magModel;
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
  public double perihelionLongitude;

  /**
   * Ascending node longitude in radians.
   */
  public double ascendingNodeLongitude;

  /**
   * Inclination of orbit in radians.
   */
  public double inclination;

  /**
   * Reference time as a Julian Day in TT . Usually perihelion time in
   * comets.
   */
  public double referenceTime;

  /**
   * Mean anomaly = mean longitude - longitude of perihelion.
   * Sometimes it is needed as a replacement to mean longitude. Radians.
   */
  public double meanAnomaly;

  /**
   * Argument of perihelion = longitude of perihelion - long. ascending node.
   * Sometimes it is needed as a replacement to long. of perihelion. Radians.
   */
  public double argumentOfPerihelion;

  /**
   * Mean motion in rad/day.
   */
  public double meanMotion;

  /**
   * Equinox of the orbital elements as a Julian day.
   */
  public double referenceEquinox;

  /**
   * Holds the reference frame for this set of orbital elements.
   */
  public FRAME referenceFrame;

  /**
   * Julian day of the beginning of the interval where these orbital
   * elements are applicable. Currently used only for space probes.
   */
  public double beginOfApplicableTime;

  /**
   * Name of the object.
   */
  public String name;
  /**
   * Julian day of the ending of the interval where these orbital
   * elements are applicable. Currently used only for space probes.
   */
  public double endOfApplicableTime;

  /**
   * Absolute magnitude.
   */
  public float absoluteMagnitude;
  /**
   * Magnitude slope for comets.
   */
  public float magnitudeSlope;
  /**
   * Perihelion distance in AU for comets.
   */
  public double perihelionDistance;

  /**
   * Central body, only for orbital elements of minor satellites.
   */
  public TARGET centralBody;

  /** The magnitude model for the object. */
  public MAGNITUDE_MODEL magnitudeModel;

	/**
	 * Transforms a {@linkplain MoonOrbitalElement} object into an {@linkplain OrbitalElement} object, correcting for
	 * longitude and periapsis precession rates to certain Julian day for subsequent ephemeris
	 * calculation.
	 *
	 * @param moon_orbit Input object.
	 * @param jd Julian day when the elements should be calculated, i.e. subsequent calculation time.
	 * @return Output object.
	 */
	public static OrbitalElement parseMoonOrbitalElement(MoonOrbitalElement moon_orbit, double jd)
	{
        double dt = jd - moon_orbit.referenceTime;

		OrbitalElement orbit = new OrbitalElement(moon_orbit.name, moon_orbit.semimajorAxis,
				moon_orbit.meanLongitude + dt * moon_orbit.meanMotion, moon_orbit.eccentricity,
				moon_orbit.periapsisLongitude + dt * moon_orbit.argumentOfPeriapsisPrecessionRate,
				moon_orbit.ascendingNodeLongitude + dt * moon_orbit.ascendingNodePrecessionRate,
				moon_orbit.inclination, jd,
				moon_orbit.meanMotion, moon_orbit.referenceEquinox, moon_orbit.beginOfApplicableTime,
				moon_orbit.endOfApplicableTime, 0.0f, 0.0f, MAGNITUDE_MODEL.NONE);

		// Try to obtain the magnitude model
		try {
			EphemerisElement eph = new EphemerisElement();
			double mag = MoonPhysicalParameters.getBodyAbsoluteMagnitude(eph);
			if (mag != EphemElement.INVALID_MAGNITUDE) {
				orbit.absoluteMagnitude = (float) mag;
				orbit.magnitudeSlope = 0.0f;
				orbit.magnitudeModel = MAGNITUDE_MODEL.ASTEROID_HG;
			}
		} catch (Exception e) {	}

		orbit.centralBody = moon_orbit.centralBody;

		return orbit;
	}

	/**
	 * To clone the object.
	 */
	@Override
	public OrbitalElement clone()
	{
		OrbitalElement orbit = new OrbitalElement(this.name, this.semimajorAxis, this.argumentOfPerihelion,
				this.eccentricity, this.meanAnomaly, this.ascendingNodeLongitude, this.inclination,
				this.referenceTime, this.meanMotion, this.referenceEquinox, this.beginOfApplicableTime,
				this.endOfApplicableTime, this.meanLongitude, this.perihelionLongitude,
				this.perihelionDistance, this.absoluteMagnitude, this.magnitudeSlope, this.magnitudeModel);
		orbit.centralBody = this.centralBody;
		orbit.referenceFrame = this.referenceFrame;
		return orbit;
	}
	/**
	 * Returns if a given object is equal to this orbital
	 * elements object.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof OrbitalElement)) return false;

		OrbitalElement that = (OrbitalElement) o;

		if (Double.compare(that.semimajorAxis, semimajorAxis) != 0) return false;
		if (Double.compare(that.meanLongitude, meanLongitude) != 0) return false;
		if (Double.compare(that.eccentricity, eccentricity) != 0) return false;
		if (Double.compare(that.perihelionLongitude, perihelionLongitude) != 0) return false;
		if (Double.compare(that.ascendingNodeLongitude, ascendingNodeLongitude) != 0) return false;
		if (Double.compare(that.inclination, inclination) != 0) return false;
		if (Double.compare(that.referenceTime, referenceTime) != 0) return false;
		if (Double.compare(that.meanAnomaly, meanAnomaly) != 0) return false;
		if (Double.compare(that.argumentOfPerihelion, argumentOfPerihelion) != 0) return false;
		if (Double.compare(that.meanMotion, meanMotion) != 0) return false;
		if (Double.compare(that.referenceEquinox, referenceEquinox) != 0) return false;
		if (Double.compare(that.beginOfApplicableTime, beginOfApplicableTime) != 0) return false;
		if (Double.compare(that.endOfApplicableTime, endOfApplicableTime) != 0) return false;
		if (Float.compare(that.absoluteMagnitude, absoluteMagnitude) != 0) return false;
		if (Float.compare(that.magnitudeSlope, magnitudeSlope) != 0) return false;
		if (Double.compare(that.perihelionDistance, perihelionDistance) != 0) return false;
		if (Double.compare(that.lastJD, lastJD) != 0) return false;
		if (referenceFrame != that.referenceFrame) return false;
		if (name != null ? !name.equals(that.name) : that.name != null) return false;
		if (centralBody != that.centralBody) return false;
		if (magnitudeModel != that.magnitudeModel) return false;

		return Arrays.equals(lastSun, that.lastSun);
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
		temp = Double.doubleToLongBits(perihelionLongitude);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(ascendingNodeLongitude);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(inclination);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(referenceTime);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(meanAnomaly);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(argumentOfPerihelion);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(meanMotion);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(referenceEquinox);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (referenceFrame != null ? referenceFrame.hashCode() : 0);
		temp = Double.doubleToLongBits(beginOfApplicableTime);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (name != null ? name.hashCode() : 0);
		temp = Double.doubleToLongBits(endOfApplicableTime);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (absoluteMagnitude != +0.0f ? Float.floatToIntBits(absoluteMagnitude) : 0);
		result = 31 * result + (magnitudeSlope != +0.0f ? Float.floatToIntBits(magnitudeSlope) : 0);
		temp = Double.doubleToLongBits(perihelionDistance);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (centralBody != null ? centralBody.hashCode() : 0);
		result = 31 * result + (magnitudeModel != null ? magnitudeModel.hashCode() : 0);
		result = 31 * result + (lastSun != null ? Arrays.hashCode(lastSun) : 0);
		temp = Double.doubleToLongBits(lastJD);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	/**
	 * Returns a string representation of this object.
	 */
	@Override
	public String toString() {
		StringBuffer out = new StringBuffer("");
		String sep = FileIO.getLineSeparator();
		out.append(this.name+sep);
		out.append("a (AU)   = " + semimajorAxis + sep);
		out.append("e        = " + eccentricity + sep);
		out.append("M (\u00b0)    = " + (meanAnomaly * Constant.RAD_TO_DEG) + sep);
		out.append("n (\u00b0/d)  = " + (meanMotion * Constant.RAD_TO_DEG) + sep);
		out.append("i (\u00b0)    = " + (inclination * Constant.RAD_TO_DEG) + sep);
		out.append("o (\u00b0)    = " + (ascendingNodeLongitude * Constant.RAD_TO_DEG) + sep);
		out.append("w (\u00b0)    = " + (argumentOfPerihelion * Constant.RAD_TO_DEG) + sep);
		out.append("time     = " + (referenceTime) + sep);
		return out.toString();
	}
	/**
	 * To obtain the brightest apparent magnitude an asteroid could reach.
	 * Note the magnitude returned depends on the magnitude model selected for the orbital
	 * elements.
	 * @return Brightest magnitude.
	 */
	public double getAsteroidMaximumMagnitude()
	{
		// Conditions for maximum magnitude
		double phase_angle = 0.0;
		double distance = Math.abs(this.semimajorAxis - 1.0);
		double distanceFromSun = this.semimajorAxis;
		if (this.eccentricity < 1.0) {
			distanceFromSun = distanceFromSun * (1.0 - this.eccentricity);
			double minDistance = Math.abs(distanceFromSun - 1.0);
			if (minDistance < distance) distance = minDistance;
		}

		if (distance == 0.0) distance = 0.1;
		if (distanceFromSun == 0.0) distanceFromSun = 0.1;
		return getApparentMagnitude(distance, distanceFromSun, phase_angle);
	}

	private double lastSun[] = null, lastJD = -1;
	/**
	 * To obtain the brightest apparent magnitude a given asteroid could reach in a given date.
	 * Note the magnitude returned depends on the magnitude model selected for the orbital
	 * elements.
	 * @param JD The Julian date.
	 * @return Brightest magnitude.
	 */
	public double getMagnitude(double JD)
	{
		double pos[] = OrbitEphem.orbitPlane(this, JD);
		double sunBody = FastMath.hypot(pos[0], pos[1]);

		// To ecliptic plane
		double C1 = FastMath.cos(argumentOfPerihelion);
		double C2 = FastMath.cos(inclination);
		double C3 = FastMath.cos(ascendingNodeLongitude);
		double S1 = FastMath.sin(argumentOfPerihelion);
		double S2 = FastMath.sin(inclination);
		double S3 = FastMath.sin(ascendingNodeLongitude);
		double MAT11 = C1 * C3 - S1 * C2 * S3;
		double MAT12 = -S1 * C3 - C1 * C2 * S3;
		double MAT21 = C1 * S3 + S1 * C2 * C3;
		double MAT22 = -S1 * S3 + C1 * C2 * C3;
		double MAT31 = S1 * S2;
		double MAT32 = C1 * S2;
		double p0 = MAT11 * pos[0] + MAT12 * pos[1]; // x
		double p1 = MAT21 * pos[0] + MAT22 * pos[1]; // y
		double p2 = MAT31 * pos[0] + MAT32 * pos[1]; // z
		pos[0] = p0;
		pos[1] = p1;
		pos[2] = p2;

		double phase_angle = 0.0;
		double sun[] = lastSun;
		if (lastSun == null || lastJD != JD) {
			lastSun = OrbitEphem.sun(JD);
			lastJD = JD;
			sun = lastSun;
		}
		double be0 = (pos[0]+sun[0]), be1 = (pos[1]+sun[1]), be2 = (pos[2]+sun[2]);
		double bodyEarth = Math.sqrt(be0*be0+be1*be1+be2*be2);
		if (this.magnitudeModel == MAGNITUDE_MODEL.ASTEROID_HG) {
			double sunEarth = FastMath.hypot(sun[0], sun[1]);
			double DPH = ((sunBody * sunBody + bodyEarth * bodyEarth - sunEarth * sunEarth) / (2.0 * sunBody * bodyEarth));
			phase_angle = Math.acos(DPH);
		}
		return getApparentMagnitude(bodyEarth, sunBody, phase_angle);
	}

	/**
	 * To obtain the brightest apparent magnitude a given comet could reach,
	 * when it is at perihelion.
	 * @return Brightest magnitude.
	 */
	public double getCometMaximumMagnitude()
	{
		double distanceFromSun = Math.abs(perihelionDistance);
		double distance = Math.abs(Math.abs(distanceFromSun) - 1.0);
		if (this.eccentricity != 1.0) distance = Math.min(distance, Math.abs(Math.abs(this.semimajorAxis) - 1.0));
		if (distance == 0.0) distance = 0.1;
		if (distanceFromSun == 0.0) distanceFromSun = 0.1;
		return getApparentMagnitude(distance, distanceFromSun, 0.0);
	}

	/**
	 * Returns the apparent magnitude of this minor object.
	 * Note the magnitude returned depends on the magnitude model selected for the orbital
	 * elements, and it is not corrected for the phase angle of sun light. In case the object
	 * has phase close to 0 the value returned here will be the magnitude for the side
	 * illuminated by the sun, not the one being observed.
	 * @param distance Distance from Earth.
	 * @param distanceFromSun Distance from Sun.
	 * @param phaseAngle The phase angle in radians.
	 * @return The apparent magnitude using the corresponding magnitude model.
	 */
	public double getApparentMagnitude(double distance, double distanceFromSun, double phaseAngle) {
		double magnitude = MAGNITUDE_MODEL.MAGNITUDE_UNKNOWN;

		/* The following methods of getting a magnitude are */
		/* discussed in Meeus' Astronomical Algorithms, */
		/* pages 216 and 217. See also http://www.mmto.org/obscats/edb.html */
		switch (magnitudeModel) {
		case ASTEROID_HG:
			double beta = Math.abs(phaseAngle);
			if (beta > Math.PI) beta = Constant.TWO_PI - beta;
			beta = Math.abs(beta);
			double tmp = Math.tan(beta * 0.5);
			double phi1 = Math.exp(-3.33 * Math.pow(tmp, 0.63));
			double phi2 = Math.exp(-1.87 * Math.pow(tmp, 1.22));
			magnitude = absoluteMagnitude + (float) (5.0 * Math.log10(distance * distanceFromSun))
					- 2.5 * Math.log10(phi1 * (1.0 - magnitudeSlope) + phi2 * magnitudeSlope);
			break;
		case COMET_gk:
			magnitude = absoluteMagnitude + 5.0 * Math.log10(distance) +
					2.5 * magnitudeSlope * Math.log10(distanceFromSun);
			break;
		default:
			magnitude = MAGNITUDE_MODEL.MAGNITUDE_UNKNOWN;
			break;
		}
		return magnitude;
	}

	/**
	 * Transforms the set of orbital elements to another equinox following Astronomical Algorithms,
	 * based on IAU 1977 precession.
	 * @param jd The Julian day of the new equinox in TT. Reference time (epoch)
	 * is unchanged, only inclination, argument of perihelion (and its longitude),
	 * and ascending node longitude are changed.
	 */
	public void changeToEquinox(double jd) {
		if (jd == this.referenceEquinox) return;

		double t = (jd - this.referenceEquinox) / Constant.JULIAN_DAYS_PER_CENTURY;
		double T = Functions.toCenturies(this.referenceEquinox);

		double eta = ((47.0029 - 0.06603 * T + 0.000598 * T * T) * t +
			(-0.03302 + 0.000598 * T) * t * t + 0.000060 * t * t * t) * Constant.ARCSEC_TO_RAD;
		double pi = 174.876384 * Constant.DEG_TO_RAD + (3289.4789 * T + 0.60622 * T * T -
			(869.8089 + 0.50491 * T) * t + 0.03536 * t * t) * Constant.ARCSEC_TO_RAD;
		double rho = ((5029.0966 + 2.22226 * T - 0.000042 * T * T) * t +
			(1.11113 - 0.000042 * T) * t * t + 0.0000060 * t * t * t) * Constant.ARCSEC_TO_RAD;
		double psi = pi + rho;

		double sinisdp = Math.sin(this.inclination) * Math.sin(this.ascendingNodeLongitude - pi);
		double sinicdp = -Math.sin(eta) * Math.cos(this.inclination) + Math.cos(eta) * Math.sin(this.inclination) * Math.cos(this.ascendingNodeLongitude - pi);

		double omegaMinusPsi = Math.atan2(sinisdp, sinicdp);
		double omega = omegaMinusPsi + psi;
		double i = Math.asin(Math.sqrt(sinisdp * sinisdp + sinicdp * sinicdp));

		double sinisdw = - Math.sin(eta) * Math.sin(this.ascendingNodeLongitude - pi);
		double sinicdw = Math.sin(this.inclination) * Math.cos(eta) - Math.cos(this.inclination) * Math.sin(eta) * Math.cos(this.ascendingNodeLongitude - pi);
		double dw = Math.atan2(sinisdw, sinicdw);
		double w = this.argumentOfPerihelion + dw;

		this.inclination = i;
		this.ascendingNodeLongitude = omega;
		this.argumentOfPerihelion = w;
		this.perihelionLongitude = this.argumentOfPerihelion + this.ascendingNodeLongitude;
		this.referenceEquinox = jd;
	}

	/**
	 * Transforms a set of elements from FK4 B1950 to FK5 J2000, following Meeus,
	 * chapter 23. Reference time (epoch) is unchanged, only inclination, argument
	 * of perihelion (and its longitude), and ascending node longitude are changed.<P>
	 * To transform elements between Bessel equinoxes
	 * @throws JPARSECException If the reference frame is not FK4 or the reference time
	 * is not B1950.
	 */
	public void FK4_to_FK5() throws JPARSECException {
		if (this.referenceFrame != FRAME.FK4 || this.referenceEquinox != Constant.B1950)
			throw new JPARSECException("Please check that the reference frame is FK4 and reference equinox is B1950.");

		double psi =- 4.50001688 * Constant.DEG_TO_RAD;
		double pi = -5.19856209 * Constant.DEG_TO_RAD;
		double eta = -0.00651966 * Constant.DEG_TO_RAD;

		double sinisdp = Math.sin(this.inclination) * Math.sin(this.ascendingNodeLongitude - pi);
		double sinicdp = -Math.sin(eta) * Math.cos(this.inclination) + Math.cos(eta) * Math.sin(this.inclination) * Math.cos(this.ascendingNodeLongitude - pi);

		double omegaMinusPsi = Math.atan2(sinisdp, sinicdp);
		double omega = omegaMinusPsi + psi;
		double i = Math.asin(Math.sqrt(sinisdp * sinisdp + sinicdp * sinicdp));

		double sinisdw = - Math.sin(eta) * Math.sin(this.ascendingNodeLongitude - pi);
		double sinicdw = Math.sin(this.inclination) * Math.cos(eta) - Math.cos(this.inclination) * Math.sin(eta) * Math.cos(this.ascendingNodeLongitude - pi);
		double dw = Math.atan2(sinisdw, sinicdw);
		double w = this.argumentOfPerihelion + dw;

		this.inclination = i;
		this.ascendingNodeLongitude = omega;
		this.argumentOfPerihelion = w;
		this.perihelionLongitude = this.argumentOfPerihelion + this.ascendingNodeLongitude;
		this.referenceEquinox = Constant.J2000;
		this.referenceFrame = FRAME.FK5;
	}

	/**
	 * Returns the time the body pass through the mean longitude
	 * of the ascending node (for the equinox of the elements). It
	 * will be the approximate real pass if the orbit is unperturbed
	 * and the mean motion or inclination are not too small.
	 * @return The time as a Julian day in TT.
	 * @throws JPARSECException If the orbit is hyperbolic, which is
	 * unsupported in this method.
	 */
	public double getNextPassThroughMeanAscendingNode() throws JPARSECException {
		double v = -this.argumentOfPerihelion;
		if (this.eccentricity < 1.0) {
			double tane2 = Math.sqrt((1.0 - this.eccentricity) / (1.0 + this.eccentricity)) * Math.tan(v * 0.5);
			double E = Math.atan(tane2) * 2.0;
			double M = E - this.eccentricity * Math.sin(E);
			return this.referenceTime + (M - this.meanAnomaly) / this.meanMotion;
		}
		if (this.eccentricity == 1.0) {
			double s = Math.tan(v * 0.5);
			return this.referenceTime + 27.403895 * s * (s * s + 3.0) * this.perihelionDistance * Math.sqrt(this.perihelionDistance);
		}
		throw new JPARSECException("the orbit is hyperbolic.");
	}

	/**
	 * Returns the time the body pass through the mean longitude
	 * of the descending node (for the equinox of the elements). It
	 * will be the approximate real pass if the orbit is unperturbed
	 * and the mean motion or inclination are not too small.
	 * @return The time as a Julian day.
	 * @throws JPARSECException If the orbit is hyperbolic.
	 */
	public double getNextPassThroughMeanDescendingNode() throws JPARSECException {
		double v = Math.PI - this.argumentOfPerihelion;
		if (this.eccentricity < 1.0) {
			double tane2 = Math.sqrt((1.0 - this.eccentricity) / (1.0 + this.eccentricity)) * Math.tan(v * 0.5);
			double E = Math.atan(tane2) * 2.0;
			double M = E - this.eccentricity * Math.sin(E);
			return this.referenceTime + (M - this.meanAnomaly) / this.meanMotion;
		}
		if (this.eccentricity == 1.0) {
			double s = Math.tan(v * 0.5);
			return this.referenceTime + 27.403895 * s * (s * s + 3.0) * this.perihelionDistance * Math.sqrt(this.perihelionDistance);
		}
		throw new JPARSECException("the orbit is hyperbolic.");
	}

	/**
	 * Returns the elements for a given planet using Vsop ephemerides.
	 * @param target The target body, from Mercury to Neptune.
	 * @param jd The Julian day for the reference time of the elements.
	 * @return The orbital element set.
	 * @throws JPARSECException If an error occurs.
	 */
	public static OrbitalElement getOrbitalElementsOfPlanet(TARGET target, double jd) throws JPARSECException {
		if (!target.isPlanet() || target == TARGET.SUN) throw new JPARSECException("Body must be a planet");
		double posV[] = Vsop.getHeliocentricEclipticPositionJ2000(jd, target);
		OrbitalElement orbit = OrbitEphem.obtainOrbitalElementsFromPositionAndVelocity(
				DataSet.getSubArray(posV, 0, 2), DataSet.getSubArray(posV, 3, 5), jd, Constant.SUN_MASS/target.relativeMass);
		return orbit;
	}

	/**
	 * Returns a simple sketch of the orbit as an image.
	 * For double stars (disable to show planets in this case) north (PA = 0) is up
	 * and east towards left (PA = 90).
	 * @param title The title for the chart.
	 * @param w The image width.
	 * @param h Image height.
	 * @param scaling Additional scaling factor if desired, otherwise set to 1.
	 * @param jd The Julian day to mark to position of the orbiting body in red.
	 * Set to 0 or negative for no mark.
	 * @param showPlanets True to show Solar System planets.
	 * @param markOtherYears True to mark the position of the object in different years.
	 * @return The image.
	 * @throws JPARSECException If an error occurs.
	 */
	public BufferedImage getOrbitImage(String title, int w, int h, double scaling, double jd,
			boolean showPlanets, boolean markOtherYears) throws JPARSECException {
		return getOrbitImage(title, w, h, scaling, jd, showPlanets, markOtherYears, true, 0);
	}

	/**
	 * Returns a simple sketch of the orbits of the planets.
	 * @param title The title for the chart.
	 * @param w The image width.
	 * @param h Image height.
	 * @param scaling Additional scaling factor if desired, otherwise set to 1.
	 * @param jd The Julian day to mark to position of the planets.
	 * Set to 0 or negative for no mark.
	 * @return The image.
	 * @throws JPARSECException If an error occurs.
	 */
	public static BufferedImage getPlanetsOrbitImage(String title, int w, int h, double scaling, double jd)
			throws JPARSECException {
		OrbitalElement orbit = new OrbitalElement();
		return orbit.getOrbitImage(title, w, h, scaling, jd, true, false, false, 0);
	}

	/**
	 * Returns a simple sketch of the orbits of the planets.
	 * @param title The title for the chart.
	 * @param w The image width.
	 * @param h Image height.
	 * @param scaling Additional scaling factor if desired, otherwise set to 1.
	 * @param jd The Julian day to mark to position of the planets.
	 * Set to 0 or negative for no mark.
	 * @param highlightDays Days to highlight the next orbit of each body. Set to 0 to 
	 * ignore this feature.
	 * @return The image.
	 * @throws JPARSECException If an error occurs.
	 */
	public static BufferedImage getPlanetsOrbitImage(String title, int w, int h, double scaling, double jd, 
			int highlightDays)
			throws JPARSECException {
		OrbitalElement orbit = new OrbitalElement();
		return orbit.getOrbitImage(title, w, h, scaling, jd, true, false, false, highlightDays);
	}

	private BufferedImage getOrbitImage(String title, int w, int h, double scaling, double jd,
			boolean showPlanets, boolean markOtherYears, boolean showOrbit, int highlightDays) throws JPARSECException {
		int x0 = w / 2 - 1, y0 = h / 2 - 1;
		OrbitalElement orbit = null;
		if (showOrbit) orbit = this.clone();
		double sma = 1, refEq = Constant.J2000;
		if (orbit != null) {
			sma = Math.abs(orbit.semimajorAxis);
			refEq = orbit.referenceEquinox;
		}
		if (sma > 4 && orbit.eccentricity > 0.95) sma = 4;
		double scale = scaling * Math.min(x0 * 0.8, y0 * 0.8) / sma;
		boolean mark = true;
		if (jd <= 0) {
			jd = Constant.J2000;
			mark = false;
		}
		if (orbit != null && orbit.referenceFrame == FRAME.FK4) orbit.FK4_to_FK5();
		double jd0 = jd;
		TimeElement time = new TimeElement(jd, TimeElement.SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		CityElement city = City.findCity("Madrid");
		ObserverElement observer = ObserverElement.parseCity(city);
		EphemerisElement eph = new EphemerisElement(TARGET.NOT_A_PLANET, EphemerisElement.COORDINATES_TYPE.APPARENT,
				refEq, EphemerisElement.GEOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
				EphemerisElement.FRAME.ICRF, EphemerisElement.ALGORITHM.ORBIT, orbit);
		eph.correctForEOP = eph.correctForPolarMotion = eph.preferPrecisionInEphemerides = false;
		if (orbit != null && orbit.meanMotion == 0.0) orbit.meanMotion = OrbitEphem.obtainMeanMotion(0.0, orbit.semimajorAxis);
		double now[] = null, pos[] = null;
		if (orbit != null) {
			pos = OrbitEphem.obtainPosition(time, observer, eph);
			now = new double[] {(x0 - pos[1] * scale), (y0 - pos[0]*scale)};
		}

		int year = time.astroDate.getYear();

		int n = (250 * w) / 500;
		double step = 5.0;
		boolean close = true;
		if (orbit != null) {
			if (orbit.eccentricity < 0.95) {
				double period = Constant.TWO_PI / orbit.meanMotion;
				step = period / (double) n;
			} else {
				jd = orbit.referenceTime - n * step / 2.0;
				time = new TimeElement(jd, TimeElement.SCALE.BARYCENTRIC_DYNAMICAL_TIME);
				pos = OrbitEphem.obtainPosition(time, observer, eph);
				close = false;
			}
		}

		Graphics g = new AWTGraphics(w, h, false, false); //img.createGraphics();
		g.setFont(FONT.getDerivedFont(FONT.DIALOG_PLAIN_15, (16*w)/500));


		Object path = null, path2 = null;
		float lw = 3.5f;
		if (orbit != null) {
			path = g.generalPathInitialize();
			g.generalPathMoveTo(path, (float) (x0 - pos[1] * scale), (float) (y0 - pos[0]*scale));
			if (highlightDays > 0) {
				path2 = g.generalPathInitialize();
				g.generalPathMoveTo(path2, (float) (x0 - pos[1] * scale), (float) (y0 - pos[0]*scale));
			}
			for (int i=0; i<n; i++) {
				time.add(step);
				jd += step;
				pos = OrbitEphem.obtainPosition(time, observer, eph);
				g.generalPathLineTo(path, (float) (x0 - pos[1]*scale), (float) (y0 - pos[0] * scale));
				if (jd-jd0 < highlightDays && path2 != null) g.generalPathLineTo(path2, (float) (x0 - pos[1]*scale), (float) (y0 - pos[0] * scale));
			}
			if (close) g.generalPathClosePath(path);
		}

		Object planets[] = new Object[8];
		Object planets2[] = new Object[8];
		String labels[] = new String[8];
		double labelPos[] = new double[8];
		if (showPlanets) {
			for (int planet=1;planet<=8;planet++) {
				TARGET t = TARGET.values()[planet];
				OrbitalElement planetOrbit = getOrbitalElementsOfPlanet(t, jd0);
				double p = 365.25 * Math.sqrt(FastMath.pow(planetOrbit.semimajorAxis, 3));
				step = p / (n - 1.0);
				time = new TimeElement(jd0, TimeElement.SCALE.BARYCENTRIC_DYNAMICAL_TIME);
				eph.orbit = planetOrbit;
				pos = OrbitEphem.obtainPosition(time, observer, eph);
				int index = planet - 1;
				planets[index] = g.generalPathInitialize();
				g.generalPathMoveTo(planets[index], (float) (x0 - pos[1] * scale), (float) (y0 - pos[0]*scale));
				if (highlightDays > 0) {
					planets2[index] = g.generalPathInitialize();
					g.generalPathMoveTo(planets2[index], (float) (x0 - pos[1] * scale), (float) (y0 - pos[0]*scale));
				}
				double minY = -1;
				for (int i=0; i<n; i++) {
					time.add(step);
					pos = OrbitEphem.obtainPosition(time, observer, eph);
					double py = - pos[0] * scale;
					g.generalPathLineTo(planets[index], (float) (x0 - pos[1]*scale), (float) (y0 + py));
					if (time.astroDate.jd()-jd0 < highlightDays && planets2[index] != null) g.generalPathLineTo(planets2[index], (float) (x0 - pos[1]*scale), (float) (y0 + py));
					if (py > minY || minY == -1) minY = py;
				}
				g.generalPathClosePath(planets[index]);
				labelPos[index] = minY;
				labels[index] = "";
				if (planetOrbit.semimajorAxis*scale > 20) labels[index] = t.getName();
			}
		}

		g.setColor(Functions.getColor(255, 255, 255, 255), true);
		g.fillRect(0, 0, w, h);
		n = 6;
		if (markOtherYears && orbit != null) {
			int years = (int) ((jd - jd0) / 365.25);
			if (years > 0) {
				int ybef = years / 2, yaft = years - ybef, ys = 1;
				g.setColor(Functions.getColor(255, 0, 0, 255), true);
				eph.orbit = orbit;
				if (ybef >= 0) {
					ys = ybef > 7 ? ybef / 4 : 1;
					if (ybef > 30) ys = 10;
					if (ybef > 100) ys = 20;
					if (ybef > 200) ys = 40;
					for (int i=0; i<=ybef; i+=ys) {
						int y = year - i;
						AstroDate astro = new AstroDate(y, 1, 1);
						time = new TimeElement(astro, TimeElement.SCALE.BARYCENTRIC_DYNAMICAL_TIME);
						pos = OrbitEphem.obtainPosition(time, observer, eph);
						float px = (float) (x0 - pos[1]*scale), py = (float) (y0 - pos[0] * scale);
						g.drawLine(px - n, py, px + n, py, false);
						g.drawLine(px, py - n, px, py + n, false);
						String label = ""+y;
						int width = (int) g.getStringWidth(label);
						int offx = (int)((FastMath.sign(px-x0)*width)/1.25), offy = (int)(((0.5+FastMath.sign(py-y0))*g.getFont().getSize())/1.25);
						g.drawString(label, px-width/2+offx, py+offy);
					}
				}
				if (yaft > 0) {
					ys = yaft > 7 ? yaft / 4 : 1;
					if (yaft > 30) ys = 10;
					if (yaft > 100) ys = 20;
					if (yaft > 200) ys = 40;
					for (int i=ys; i<=yaft; i+=ys) {
						int y = year + i;
						AstroDate astro = new AstroDate(y, 1, 1);
						time = new TimeElement(astro, TimeElement.SCALE.BARYCENTRIC_DYNAMICAL_TIME);
						pos = OrbitEphem.obtainPosition(time, observer, eph);
						float px = (float) (x0 - pos[1]*scale), py = (float) (y0 - pos[0] * scale);
						g.drawLine(px - n, py, px + n, py, false);
						g.drawLine(px, py - n, px, py + n, false);
						String label = ""+y;
						int width = (int) g.getStringWidth(label);
						int offx = (int)((FastMath.sign(px-x0)*width)/1.25), offy = (int)(((0.5+FastMath.sign(py-y0))*g.getFont().getSize())/1.25);
						g.drawString(label, px-width/2+offx, py+offy);
					}
				}
			}
		}
		n = 4;
		if (showPlanets) {
			g.setColor(Functions.getColor(0, 0, 255, 255), true);
			int offy = g.getFont().getSize();
			for (int i=0; i<planets.length; i++) {
				if (planets2[i] != null) {
					g.setStroke(new JPARSECStroke(JPARSECStroke.STROKE_DEFAULT_LINE_THICK, lw));
					g.draw(planets2[i]);
				}
				g.setStroke(JPARSECStroke.STROKE_DEFAULT_LINE);
				g.draw(planets[i]);
				int width = (int) g.getStringWidth(labels[i]), s = 1;
				if (labelPos[i]+g.getFont().getSize()*2 >= h) s = -1;
				g.drawString(labels[i], x0-width/2, y0 + (float) labelPos[i] + offy*s);
				if (mark) {
					((GeneralPath)planets[i]).getPathIterator(null).currentSegment(pos);
					g.fillOval((int) pos[0]- n, (int) pos[1] - n, 2*n, 2*n, false);
				}
			}
		}
		g.setColor(Color.ORANGE.getRGB(), true);
		g.fillOval(x0 - n, y0 - n, 2*n, 2*n, false);
		g.setColor(Functions.getColor(0, 0, 0, 92), true);
		int border = 100;
		g.drawLine(x0, y0-h/2+border, x0, y0+h/2-border, false);
		g.drawLine(x0-w/2+border, y0, x0+w/2-border, y0, false);
		g.setColor(Functions.getColor(0, 0, 0, 255), true);
		String s = "0\u00b0";
		if (!showPlanets) s += " (N)";
		int size = g.getFont().getSize();
		g.drawString(s, x0-g.getStringWidth(s)/2, y0-h/2+border-size/2);
		s = "90\u00b0";
		if (!showPlanets) s += " (E)";
		g.drawString(s, x0-w/2+border-g.getStringWidth(s)-size/2, y0+size/2);
		s = "180\u00b0";
		if (!showPlanets) s += " (S)";
		g.drawString(s, x0-g.getStringWidth(s)/2, y0+h/2-border+size);
		s = "270\u00b0";
		if (!showPlanets) {
			if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) {
				s += " (O)";
			} else {
				s += " (W)";
			}
		}
		g.drawString(s, x0+w/2-border+size/2, y0+size/2);

		double pix = scale, factor = 1;
		while (true) {
			if (pix*factor < w/8) {
				factor *= 2;
				continue;
			}
			if (pix*factor > w/2 && pix*factor > w/(8*0.5-1)) {
				factor *= 0.5;
				continue;
			}
			break;
		};
		s = Functions.formatValue(factor, 1);
		if (showPlanets) {
			if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) {
				s += " UA";
			} else {
				s += " AU";
			}
		} else {
			s += " \"";
		}
		int l = (int) (pix * factor);
		g.drawLine(x0-l/2, y0+h/2-border/2+size, x0+l/2, y0+h/2-border/2+size, false);
		g.drawString(s, x0-g.getStringWidth(s)/2, y0+h/2-border/2+size/2);

		g.setColor(Functions.getColor(255, 0, 0, 255), true);
		g.setStroke(new JPARSECStroke(JPARSECStroke.STROKE_DEFAULT_LINE_THICK, lw));
		if (path2 != null) g.draw(path2);
		g.setStroke(JPARSECStroke.STROKE_DEFAULT_LINE);
		if (path != null) g.draw(path);
		if (mark && now != null) {
			n++;
			g.fillOval((int) now[0] - n, (int) now[1] - n, 2*n, 2*n, false);
		}

		g.setColor(Functions.getColor(0, 0, 0, 255), true);
		g.drawString(title, x0 - g.getStringWidth(title)/2, Math.max(y0*0.05f, g.getFont().getSize()));
		return (BufferedImage) g.getRendering();
	}

	/**
	 * Returns the light curve of this minor object in a given time interval.
	 * @param init Time object for the beginning of the interval. Its time scale
	 * value will be used as reference for the x axis of the output chart.
	 * @param end Time object for the end of the interval.
	 * @param obs The observer.
	 * @param eph0 The ephemeris properties.
	 * @param npoints The number of points in the light curve.
	 * @return The chart object representing the light curve.
	 * @throws JPARSECException If an error occurs.
	 */
	public CreateChart getLightCurveChart(TimeElement init, TimeElement end, ObserverElement obs, EphemerisElement eph0,
			int npoints) throws JPARSECException {
		EphemerisElement eph = eph0.clone();
		eph.optimizeForSpeed();
		double jd0 = TimeScale.getJD(init, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		double jdf = TimeScale.getJD(end, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		double x[] = DataSet.getSetOfValues(jd0, jdf, npoints, false);
		double y[] = new double[x.length];
		double step = x[1] - x[0];

		OrbitalElement orbit = this.clone();
		int sp = orbit.name.indexOf("   ");
		if (sp > 0) orbit.name = orbit.name.substring(0, sp).trim();
		orbit.changeToEquinox(Constant.J2000);
		double jd = jd0;
		double jdMax = -1, magMax = -1;
		int indexMax = -1;
		for (int i=0; i<npoints; i++) {
			double plane_orbit_coords[] = OrbitEphem.orbitPlane(orbit, jd);
			double coords[] = OrbitEphem.toEclipticPlane(orbit, plane_orbit_coords);
			double sun[] = PlanetEphem.getGeocentricPosition(jd, TARGET.SUN, 0.0, false, obs);

			double RP = Functions.getNorm(coords);
			double RO = Functions.getNorm(new double[] {sun[0] + coords[0], sun[1] + coords[1], sun[2] + coords[2]});
			double RE = Functions.getNorm(sun);
			if (eph.preferPrecisionInEphemerides) {
				double ltSun = RE * Constant.LIGHT_TIME_DAYS_PER_AU;
				double ltObj = RO * Constant.LIGHT_TIME_DAYS_PER_AU;
				plane_orbit_coords = OrbitEphem.orbitPlane(orbit, jd - ltObj);
				coords = OrbitEphem.toEclipticPlane(orbit, plane_orbit_coords);
				sun = PlanetEphem.getGeocentricPosition(jd - ltSun, TARGET.SUN, 0.0, false, obs);
				RP = Functions.getNorm(coords);
				RO = Functions.getNorm(new double[] {sun[0] + coords[0], sun[1] + coords[1], sun[2] + coords[2]});
				RE = Functions.getNorm(sun);
			}
			double DPH = ((RP * RP + RO * RO - RE * RE) / (2.0 * RP * RO));

			x[i] = TimeScale.getJD(new TimeElement(jd, SCALE.BARYCENTRIC_DYNAMICAL_TIME), obs, eph, init.timeScale);
			y[i] = orbit.getApparentMagnitude(RO, RP, Math.acos(DPH));
			if (magMax == -1 || y[i] < magMax) {
				magMax = y[i];
				jdMax = x[i];
				indexMax = i;
			}
			jd += step;
		}

		if (indexMax > 0 && indexMax < y.length-1) {
			Interpolation interp = new Interpolation(DataSet.getSubArray(x, indexMax-1, indexMax+1), DataSet.getSubArray(y, indexMax-1, indexMax+1), false);
			jdMax = interp.MeeusExtremum().getX();
			magMax = interp.MeeusInterpolation(jdMax);
		}
		
		// FIXME: '^{m}-' should be replaced by 'm -' in the Android version
		String title = Translate.translate(159) + " (" + Functions.formatValue(magMax, 1)+"^{m}- "+(new AstroDate(jdMax).toStringDate(false)+")");
		// title = Translate.translate(159)+" "+Translate.translate(160)+" "+orbit.name;

		ChartSeriesElement series = new ChartSeriesElement(x, y, null, null, Translate.translate(157)+" "+Translate.translate(160)+" "+orbit.name, true, Color.BLACK, ChartSeriesElement.SHAPE_POINT, null);
		series.showLines = true;
		ChartElement chart = new ChartElement(new ChartSeriesElement[] {series}, ChartElement.TYPE.XY_CHART, ChartElement.SUBTYPE.XY_TIME,
				title, Translate.translate(1202), Translate.translate(157), false);
		CreateChart ch = new CreateChart(chart);
		return ch;
	}

	/**
	 * Returns the distance curve of this minor object in a given time interval,
	 * respect both the Earth and the Sun.
	 * @param init Time object for the beginning of the interval. Its time scale
	 * value will be used as reference for the x axis of the output chart.
	 * @param end Time object for the end of the interval.
	 * @param obs The observer.
	 * @param eph0 The ephemeris properties.
	 * @param npoints The number of points in the chart.
	 * @return The chart object representing the distance chart.
	 * @throws JPARSECException If an error occurs.
	 */
	public CreateChart getDistanceChart(TimeElement init, TimeElement end, ObserverElement obs, EphemerisElement eph0,
			int npoints) throws JPARSECException {
		EphemerisElement eph = eph0.clone();
		eph.optimizeForSpeed();
		double jd0 = TimeScale.getJD(init, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		double jdf = TimeScale.getJD(end, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		double x[] = DataSet.getSetOfValues(jd0, jdf, npoints, false);
		double y1[] = new double[x.length];
		double y2[] = new double[x.length];
		double step = x[1] - x[0];

		OrbitalElement orbit = this.clone();
		int sp = orbit.name.indexOf("   ");
		if (sp > 0) orbit.name = orbit.name.substring(0, sp).trim();
		orbit.changeToEquinox(Constant.J2000);
		double jd = jd0;
		double minDist = -1, jdMin = -1;
		int indexMin = -1;
		for (int i=0; i<npoints; i++) {
			double plane_orbit_coords[] = OrbitEphem.orbitPlane(orbit, jd);
			double coords[] = OrbitEphem.toEclipticPlane(orbit, plane_orbit_coords);
			double sun[] = PlanetEphem.getGeocentricPosition(jd, TARGET.SUN, 0.0, false, obs);

			double RP = Functions.getNorm(coords);
			double RO = Functions.getNorm(new double[] {sun[0] + coords[0], sun[1] + coords[1], sun[2] + coords[2]});
			if (eph.preferPrecisionInEphemerides) {
				double ltSun = Functions.getNorm(sun) * Constant.LIGHT_TIME_DAYS_PER_AU;
				double ltObj = RO * Constant.LIGHT_TIME_DAYS_PER_AU;
				plane_orbit_coords = OrbitEphem.orbitPlane(orbit, jd - ltObj);
				coords = OrbitEphem.toEclipticPlane(orbit, plane_orbit_coords);
				sun = PlanetEphem.getGeocentricPosition(jd - ltSun, TARGET.SUN, 0.0, false, obs);
				RP = Functions.getNorm(coords);
				RO = Functions.getNorm(new double[] {sun[0] + coords[0], sun[1] + coords[1], sun[2] + coords[2]});
			}
			
			x[i] = TimeScale.getJD(new TimeElement(jd, SCALE.BARYCENTRIC_DYNAMICAL_TIME), obs, eph, init.timeScale);
			y1[i] = RO;
			y2[i] = RP;
			if (minDist == -1 || y1[i] < minDist) {
				minDist = y1[i];
				jdMin = jd;
				indexMin = i;
			}
			jd += step;
		}

		if (indexMin > 0 && indexMin < y1.length-1) {
			Interpolation interp = new Interpolation(DataSet.getSubArray(x, indexMin-1, indexMin+1), DataSet.getSubArray(y1, indexMin-1, indexMin+1), false);
			jdMin = interp.MeeusExtremum().getX();
			minDist = interp.MeeusInterpolation(jdMin);
		}
		
		ChartSeriesElement series1 = new ChartSeriesElement(x, y1, null, null, Translate.translate(299)+" "+Translate.translate(3)+"-"+orbit.name, true, Color.BLUE, ChartSeriesElement.SHAPE_POINT, null);
		ChartSeriesElement series2 = new ChartSeriesElement(x, y2, null, null, Translate.translate(299)+" "+Translate.translate(0)+"-"+orbit.name, true, Color.RED, ChartSeriesElement.SHAPE_POINT, null);
		series1.showLines = true;
		series2.showLines = true;
		String au = "AU";
		if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) au = "UA";

		String title = Translate.translate(1303);
		title = title.substring(0, title.lastIndexOf(" "));
		// FIXME: @BLUE and @BLACK should be removed in the Android version
		title += " (@BLUE"+Functions.formatValue(minDist, 3)+au+"@BLACK - "+(new AstroDate(jdMin).toStringDate(false))+")";
		//title = Translate.translate(1303)+" "+orbit.name;
		ChartElement chart = new ChartElement(new ChartSeriesElement[] {series1, series2}, ChartElement.TYPE.XY_CHART, ChartElement.SUBTYPE.XY_TIME,
				title, Translate.translate(1202), Translate.translate(299)+" ("+au+")", false);
		CreateChart ch = new CreateChart(chart);
		return ch;
	}
}
