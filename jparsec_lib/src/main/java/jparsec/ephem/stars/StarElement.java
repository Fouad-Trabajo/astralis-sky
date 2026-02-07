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
import jparsec.io.FileIO;
import jparsec.math.Constant;
import jparsec.observer.LocationElement;
import jparsec.util.Translate;
import jparsec.vo.SimbadElement;

/**
 * Convenient class for stars data access.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class StarElement implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Value returned for the distance to the star when parallax is 0.
	 * Current value is 1000 pc.
	 */
	public static final int DISTANCE_UNKNOWN = 1000;

	/**
	 * Constructs an star object providing the values of the fields.
	 *
	 * @param nom Name.
	 * @param ra Right Ascension in radians.
	 * @param dec Declination in radians.
	 * @param p Parallax in mas.
	 * @param mag Apparent magnitude.
	 * @param pm_ra Proper motion in RA.
	 * @param pm_dec Proper motion in DEC.
	 * @param pm_rv Radial velocity.
	 * @param eq Reference equinox.
	 * @param fr Reference frame.
	 */
	public StarElement(String nom, double ra, double dec, double p, float mag, float pm_ra, float pm_dec,
			float pm_rv, double eq, EphemerisElement.FRAME fr)
	{
		rightAscension = ra;
		declination = dec;
		parallax = p;
		magnitude = mag;
		properMotionRA = pm_ra;
		properMotionDEC = pm_dec;
		properMotionRadialV = pm_rv;
		equinox = eq;
		frame = fr;
		name = nom;
		spectrum = "";
		type = "";
	}

	/**
	 * Constructs an empty star object.
	 */
	public StarElement()
	{
		rightAscension = 0.0;
		declination = 0.0;
		parallax = 0.0;
		properMotionRA = 0.0f;
		properMotionDEC = 0.0f;
		properMotionRadialV = 0.0f;
		equinox = 0.0;
		frame = EphemerisElement.FRAME.ICRF;
		name = "";
		spectrum = "";
		type = "";
		magnitude = 0f;
	}

	/**
	 * Name of the star.
	 */
	public String name;

	/**
	 * Right Ascension in radians from the catalogue.
	 */
	public double rightAscension;

	/**
	 * Declination in radians from the catalogue.
	 */
	public double declination;

	/**
	 * Parallax in milliarcseconds.
	 */
	public double parallax;

	/**
	 * Apparent visual magnitude.
	 */
	public float magnitude;

	/**
	 * Displacement in Right Ascension in radians per Julian year, referred to
	 * the Solar System Barycenter. This movement is in terms of coordinate
	 * angle on the celestial sphere. In the catalogs (specially the old
	 * ones), it is sometimes measured in arcseconds per Julian year as a true
	 * angle. If it is the case, as in the Bright Star Catalogue, you will have
	 * to divide it by COS(DECLINATION). If you are in doubts, check Alp UMi
	 * (Polaris) in the catalog. It's annual proper motion in RA is 3
	 * [coordinate "/year] = 0.04 [apparent "/year] / COS(89.2).
	 */
	public float properMotionRA;

	/**
	 * Displacement in Declination in radians per Julian year, referred to the
	 * Solar System Barycenter.
	 */
	public float properMotionDEC;

	/**
	 * Displacement in Radial velocity in km/s, referred to the Solar System
	 * Barycenter.
	 */
	public float properMotionRadialV;

	/**
	 * Reference equinox (and epoch) for the catalogue coordinates as a Julian day.
	 */
	public double equinox;

	/**
	 * Reference frame for the catalogue coordinates.
	 */
	public EphemerisElement.FRAME frame;

	/**
	 * Spectral type. Currently available only for JPARSEC file format.
	 */
	public String spectrum;

	/**
	 * Type of star: N for Normal, D for double or multiple, V for variable, and
	 * B for both double and variable. Only available for BSC5 and JPARSEC file
	 * formats. For JPARSEC file format additional information is available as three
	 * fields separated by ;. First field is one of the previous values N, D, V, B.
	 * Second is double star data (only if it is double or multiple). Third is variability
	 * data (if it is variable). Double data includes four fields separated by a comma
	 * (separation of main components in arcseconds, magnitude difference in components A-B,
	 * orbit period in years, position angle in degrees), while variability data includes
	 * another four fields separated by a comma (maximum magnitude, minimum magnitude,
	 * period of variability in days, variable type).
	 */
	public String type;

	/**
	 * To clone the object.
	 */
	@Override
	public StarElement clone()
	{
		StarElement out = new StarElement(this.name, this.rightAscension, this.declination, this.parallax,
				this.magnitude, this.properMotionRA, this.properMotionDEC, this.properMotionRadialV,
				this.equinox, this.frame);
		out.spectrum = this.spectrum;
		out.type = this.type;
		return out;
	}
	/**
	 * Returns true if the input object is equals to this
	 * instance.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof StarElement)) return false;

		StarElement that = (StarElement) o;

		if (Double.compare(that.rightAscension, rightAscension) != 0) return false;
		if (Double.compare(that.declination, declination) != 0) return false;
		if (Double.compare(that.parallax, parallax) != 0) return false;
		if (Float.compare(that.magnitude, magnitude) != 0) return false;
		if (Float.compare(that.properMotionRA, properMotionRA) != 0) return false;
		if (Float.compare(that.properMotionDEC, properMotionDEC) != 0) return false;
		if (Float.compare(that.properMotionRadialV, properMotionRadialV) != 0) return false;
		if (Double.compare(that.equinox, equinox) != 0) return false;
		if (name != null ? !name.equals(that.name) : that.name != null) return false;
		if (frame != that.frame) return false;
		if (spectrum != null ? !spectrum.equals(that.spectrum) : that.spectrum != null) return false;

		return !(type != null ? !type.equals(that.type) : that.type != null);
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
		temp = Double.doubleToLongBits(parallax);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (magnitude != +0.0f ? Float.floatToIntBits(magnitude) : 0);
		result = 31 * result + (properMotionRA != +0.0f ? Float.floatToIntBits(properMotionRA) : 0);
		result = 31 * result + (properMotionDEC != +0.0f ? Float.floatToIntBits(properMotionDEC) : 0);
		result = 31 * result + (properMotionRadialV != +0.0f ? Float.floatToIntBits(properMotionRadialV) : 0);
		temp = Double.doubleToLongBits(equinox);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (frame != null ? frame.hashCode() : 0);
		result = 31 * result + (spectrum != null ? spectrum.hashCode() : 0);
		result = 31 * result + (type != null ? type.hashCode() : 0);
		return result;
	}

	/**
	 * Creates a star object from a Simbad one.
	 *
	 * @param s Simbad object.
	 * @return Star object, with J2000 equinox and FK5 frame. In case
	 * parallax is 0 distance is arbitrarily set to 1000 pc.
	 */
	public static StarElement parseSimbadElement(SimbadElement s)
	{
		StarElement star = new StarElement(s.name, s.rightAscension, s.declination, s.parallax,
			0, s.properMotionRA, s.properMotionDEC, s.properMotionRadialV,
			Constant.J2000, EphemerisElement.FRAME.FK5);
		return star;
	}

	/**
	 * Returns the distance to the star in pc.
	 * @return Distance in pc, or 0 if parallax is 0.
	 */
	public double getDistance() {
		if (parallax == 0) return DISTANCE_UNKNOWN;
		return (1000.0 / parallax);
	}

	/**
	 * Returns if the distance to the star is unknown.
	 * @return True if parallax is 0, false otherwise.
	 */
	public boolean isDistanceUnknown() {
		return (parallax == 0);
	}

	/**
	 * Returns the equatorial position of this star.
	 * @return Equatorial position.
	 */
	public LocationElement getEquatorialPosition() {
		return new LocationElement(rightAscension, declination, getDistance());
	}
	
	/**
	 * Returns a string representation of the type of variable star this star is,
	 * or null if it is not a variable or of an unknown type.
	 * @return String representation of the variable star type.
	 */
	public String getVariableType() {
		if (type == null) return null;
		int nf = FileIO.getNumberOfFields(type, ";", false);
		if (nf < 3) return null;
		String vdata = FileIO.getField(3, type, ";", false);
		nf = FileIO.getNumberOfFields(vdata, ",", false);
		if (nf < 4) return null;
		String vt = FileIO.getField(4, vdata, ",", false);
		if (!vt.equals("")) return getVariableType(Integer.parseInt(vt));
		return null;
	}
	
	/**
	 * Returns a string representation of the type of variable star this star is,
	 * or null if it is not a variable or of an unknown type.
	 * @param var Integer identifier of the variable star as given in the Sky 2000 
	 * Master Catalog.
	 * @return String representation of the variable star type.
	 */
	public static String getVariableType(int var) {
		String v = ""+var;
		//if (v.length() < 2) return null;
		int n1 = Integer.parseInt(v.substring(0, 1));
		if (n1 < 1 || n1 > 4) return null;
		int n2 = -1;
		if (v.length() > 1) n2 = Integer.parseInt(v.substring(1, 2));
		String TYPES[] = new String[] {
				Translate.translate(1324),
				Translate.translate(1325),
				Translate.translate(1326),
				Translate.translate(1327),
				};
		String mainType = TYPES[n1-1];
		if (n1 == 3 && n2 == 5) mainType = TYPES[1];
		String out = null;
		switch (n1) {
		case 1: // Pulsing
			String PULSATING[] = new String[] {
					"Del Cep", "Del Cep", "RR Lyr", "Bet Cep (Bet CMa)", "Del Scu", "Alf CVn", "Irr", "Irr Superg", "Mira", "Semi-irr" 
			};
			if (n2 >= 0 && n2 < PULSATING.length) out = PULSATING[n2];
			break;
		case 2:
			String ERUPTIVE[] = new String[] {
					"Irr", "T Tau", "UV Cet", "R CrB", "U Gem", null, "Nova", "Irr" 
			};
			if (n2 >= 0 && n2 < ERUPTIVE.length) out = ERUPTIVE[n2];
			break;
		case 3:
			String ECLIPSING[] = new String[] {
					null, "Algol", "Bet Lyr", "W UMa", "Ellip", "S Dor" 
			};
			if (n2 >= 0 && n2 < ECLIPSING.length) out = ECLIPSING[n2];
			break;
		case 4:
			String ROTATING[] = new String[] {
					null, "FK Com", "SX Ari", "Ellip", "BY Dra" 
			};
			if (n2 >= 0 && n2 < ROTATING.length) out = ROTATING[n2];
			break;
		}
		if (out != null) {
			out = " ("+out+")";
		} else {
			out = "";
		}
		return mainType + out;
	}
}
