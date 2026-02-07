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

import jparsec.ephem.EphemerisElement;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.planets.OrbitEphem;
import jparsec.ephem.planets.OrbitalElement;
import jparsec.io.FileIO;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.TimeElement;
import jparsec.util.JPARSECException;

/**
 * Convenient class for double stars ephemerides. Example:<BR>
 * <pre>
 * try {
 * 		ReadFile re = new ReadFile();
 * 		re.setPath(DoubleStarElement.PATH_OLD_VISUAL_DOUBLE_STAR_CATALOG);
 * 		re.readOldFileOfDoubleStars();
 * 		System.out.println(re.getNumberOfObjects());
 * 		int index = re.searchByName("alpha centauri");
 * 		DoubleStarElement dstar = re.getDoubleStarElement(index);
 *
 * 		AstroDate astro = new AstroDate(2010, 1, 1);
 * 		TimeElement time = new TimeElement(astro.jd(), SCALE.UNIVERSAL_TIME_UTC);
 * 		CityElement city = City.findCity("Madrid");
 * 		ObserverElement observer = ObserverElement.parseCity(city);
 *
 * 		dstar.calcEphemeris(time, observer);
 * } catch (Exception e)
 * {
 *		e.printStackTrace();
 * }
 * </pre>
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class DoubleStarElement implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructs an star object providing the values of the fields.
	 *
	 * @param nom Name.
	 * @param ra Right Ascension in radians.
	 * @param dec Declination in radians.
	 * @param wds WDS designation.
	 * @param ads ADS designation.
	 * @param hd HP designation.
	 * @param hipp HipparcosS designation.
	 * @param magP Magnitude of primary.
	 * @param magS Magnitude of secondary.
	 * @param orbit The orbit object.
	 * @param orbitG Orbit grade.
	 * @param notes Contain a link to a notes file.
	 * @param ref Contain a link to a file of references.
	 * @param png Contain a link to a png file showing the orbit.
	 */
	public DoubleStarElement(String nom, double ra, double dec, String wds, String ads, String hd, String hipp,
			String magP, String magS, OrbitalElement orbit, int orbitG, String notes, String ref, String png)
	{
		rightAscension = ra;
		declination = dec;
		name = nom;
		this.wds = wds;
		this.ads = ads;
		this.hd = hd;
		hipparcos = hipp;
		magPrimary = magP;
		magSecondary = magS;
		this.orbit = orbit;
		orbitGrade = orbitG;
		this.notes = notes;
		reference = ref;
		orbitPNG = png;
	}

	/**
	 * Constructs an empty star object.
	 */
	public DoubleStarElement()
	{
		rightAscension = 0.0;
		declination = 0.0;
		name = "";
		wds = "";
		ads = "";
		hd = "";
		hipparcos = "";
		magPrimary = "";
		magSecondary = "";
		orbit = null;
		orbitGrade = 0;
		notes = "";
		reference = "";
		orbitPNG = "";
	}

	/**
	 * Name of the star.
	 */
	public String name;
	/**
	 * WDS designation.
	 */
	public String wds;
	/**
	 * ADS designation.
	 */
	public String ads;
	/**
	 * HD designation.
	 */
	public String hd;
	/**
	 * Hipparcos designation.
	 */
	public String hipparcos;
	/**
	 * Magnitude of primary.
	 */
	public String magPrimary;
	/**
	 * Magnitude of secondary.
	 */
	public String magSecondary;
	/**
	 * Orbital elements for the star.
	 */
	public OrbitalElement orbit;
	/**
	 * Orbit grade, from 1 (reliable) to 5 (uncertain)
	 */
	public int orbitGrade;
	/**
	 * Link to notes.
	 */
	public String notes;
	/**
	 * Link to reference.
	 */
	public String reference;
	/**
	 * Link to online png file showing the orbit.
	 */
	public String orbitPNG;

	/**
	 * Right Ascension in radians from the catalogue.
	 */
	public double rightAscension;

	/**
	 * Declination in radians from the catalogue.
	 */
	public double declination;

	/**
	 * To clone the object.
	 */
	@Override
	public DoubleStarElement clone()
	{
		DoubleStarElement out = new DoubleStarElement(this.name, this.rightAscension, this.declination, wds, ads,
				hd, hipparcos, magPrimary, magSecondary, orbit.clone(), orbitGrade, notes, reference, orbitPNG);
		out.pa = this.pa;
		out.rho = this.rho;
		return out;
	}
	/**
	 * Returns true if the input object is equals to this
	 * instance.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof DoubleStarElement)) return false;

		DoubleStarElement that = (DoubleStarElement) o;

		if (orbitGrade != that.orbitGrade) return false;
		if (Double.compare(that.rightAscension, rightAscension) != 0) return false;
		if (Double.compare(that.declination, declination) != 0) return false;
		if (Double.compare(that.pa, pa) != 0) return false;
		if (Double.compare(that.rho, rho) != 0) return false;
		if (name != null ? !name.equals(that.name) : that.name != null) return false;
		if (wds != null ? !wds.equals(that.wds) : that.wds != null) return false;
		if (ads != null ? !ads.equals(that.ads) : that.ads != null) return false;
		if (hd != null ? !hd.equals(that.hd) : that.hd != null) return false;
		if (hipparcos != null ? !hipparcos.equals(that.hipparcos) : that.hipparcos != null) return false;
		if (magPrimary != null ? !magPrimary.equals(that.magPrimary) : that.magPrimary != null) return false;
		if (magSecondary != null ? !magSecondary.equals(that.magSecondary) : that.magSecondary != null) return false;
		if (orbit != null ? !orbit.equals(that.orbit) : that.orbit != null) return false;
		if (notes != null ? !notes.equals(that.notes) : that.notes != null) return false;
		if (reference != null ? !reference.equals(that.reference) : that.reference != null) return false;

		return !(orbitPNG != null ? !orbitPNG.equals(that.orbitPNG) : that.orbitPNG != null);
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		result = name != null ? name.hashCode() : 0;
		result = 31 * result + (wds != null ? wds.hashCode() : 0);
		result = 31 * result + (ads != null ? ads.hashCode() : 0);
		result = 31 * result + (hd != null ? hd.hashCode() : 0);
		result = 31 * result + (hipparcos != null ? hipparcos.hashCode() : 0);
		result = 31 * result + (magPrimary != null ? magPrimary.hashCode() : 0);
		result = 31 * result + (magSecondary != null ? magSecondary.hashCode() : 0);
		result = 31 * result + (orbit != null ? orbit.hashCode() : 0);
		result = 31 * result + orbitGrade;
		result = 31 * result + (notes != null ? notes.hashCode() : 0);
		result = 31 * result + (reference != null ? reference.hashCode() : 0);
		result = 31 * result + (orbitPNG != null ? orbitPNG.hashCode() : 0);
		temp = Double.doubleToLongBits(rightAscension);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(declination);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(pa);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(rho);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	private double pa, rho;
	/**
	 * Return position angle.
	 * @return Position angle in radians, measured from North to East (North = 0 degrees, East = 90 degrees).
	 */
	public double getPositionAngle() {
		return pa;
	}

	/**
	 * Return distance.
	 * @return Distance in arcseconds.
	 */
	public double getDistance() {
		return rho;
	}

	/**
	 * The path to the sixth catalog of orbits of visual binary stars,
	 * Hartkopf 2006.
	 */
	public static final String PATH_VISUAL_DOUBLE_STAR_CATALOG =  FileIO.DATA_ORBITAL_ELEMENTS_DIRECTORY + "orb6orbits.txt";
	/**
	 * The path to the old fourth catalog of orbits of visual binary stars,
	 * Worley 1983.
	 */
	public static final String PATH_OLD_VISUAL_DOUBLE_STAR_CATALOG =  FileIO.DATA_ORBITAL_ELEMENTS_DIRECTORY + "worley.txt";

	/**
	 * Calculates ephemeris for this double star.
	 * @param time Time object.
	 * @param observer Observer object.
	 * @throws JPARSECException If an error occurs.
	 */
	public void calcEphemeris(TimeElement time, ObserverElement observer) throws JPARSECException {
		if (this.orbit == null) throw new JPARSECException("this double star has no set of orbital elements!");
		EphemerisElement eph = new EphemerisElement(TARGET.NOT_A_PLANET, EphemerisElement.COORDINATES_TYPE.APPARENT,
				EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.GEOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
				EphemerisElement.FRAME.DYNAMICAL_EQUINOX_J2000, EphemerisElement.ALGORITHM.ORBIT, this.orbit);
		double pos[] = OrbitEphem.obtainPosition(time, observer, eph);

		rho = Math.sqrt(pos[0]*pos[0]+pos[1]*pos[1]);
		pa = Math.atan2(pos[1], pos[0]);
	}

	/**
	 * Returns the equatorial position of this star.
	 * @return Equatorial position.
	 */
	public LocationElement getEquatorialPosition() {
		return new LocationElement(rightAscension, declination, 1);
	}
}
