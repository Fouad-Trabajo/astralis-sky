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
package jparsec.vo;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import jparsec.ephem.Functions;
import jparsec.ephem.stars.StarElement;
import jparsec.graph.DataSet;
import jparsec.graph.chartRendering.RenderSky;
import jparsec.io.FileIO;
import jparsec.math.Constant;
import jparsec.observer.LocationElement;
import jparsec.util.DataBase;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;

/**
 * A class to hold data of objects retrieved from Simbad.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class SimbadElement implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Name.
	 */
	public String name;
	/**
	 * Other designations.
	 */
	public String[] otherNames;
	/**
	 * Right ascension in radians, ICRF.
	 */
	public double rightAscension;
	/**
	 * Declination in radians, ICRF.
	 */
	public double declination;
	/**
	 * Spectral type, when applicable.
	 */
	public String spectralType;
	/**
	 * Object type, when applicable.
	 */
	public String type;
	/**
	 * B-V color index, equal to {@linkplain SimbadElement#B_MINUS_V_UNAVAILABLE}
	 * if not available.
	 */
	public float bMinusV;
	/**
	 * Proper motion in RA (radians/year), when applicable. Note Simbad uses
	 * the other criteria for RA proper motion compared to {@linkplain StarElement}
	 * class. Here the motion is as a true angle on the celestial sphere.
	 */
	public float properMotionRA;
	/**
	 * Proper motion in DEC (radians/year), when applicable.
	 */
	public float properMotionDEC;
	/**
	 * Proper motion in radial velocity (km/s), when applicable.
	 */
	public float properMotionRadialV;
	/**
	 * Parallax in mas.
	 */
	public float parallax;
	/**
	 * Default value of the B-V color if it is not available.
	 */
	public static final float B_MINUS_V_UNAVAILABLE = 100;

	/**
	 * Default empty constructor.
	 */
	public SimbadElement()
	{
		this.declination = this.rightAscension = this.properMotionDEC =
			this.properMotionRA = this.properMotionRadialV = 0.0f;
		this.bMinusV = B_MINUS_V_UNAVAILABLE;
	}
	/**
	 * Constructor with some information.
	 * @param name Name.
	 * @param ra Right ascension in radians.
	 * @param dec Declination in radians.
	 */
	public SimbadElement (String name, double ra, double dec)
	{
		this.name = name;
		this.rightAscension = ra;
		this.declination = dec;
	}
	/**
	 * Constructor with a location.
	 * @param loc Location object.
	 */
	public SimbadElement(LocationElement loc)
	{
		this.rightAscension = loc.getLongitude();
		this.declination = loc.getLatitude();
	}

	/**
	 * Returns a star object as a Simbad object.
	 * @param star The star object.
	 */
	public SimbadElement(StarElement star) {
		name = star.name;
		rightAscension = star.rightAscension;
		declination = star.declination;
		spectralType = star.spectrum;
		parallax = (float) star.parallax;
		properMotionRA = (float) (star.properMotionRA * Math.cos(star.declination)); // Simbad uses the other criteria for RA motion
		properMotionDEC = star.properMotionDEC;
		properMotionRadialV = star.properMotionRadialV;
	}

	/**
	 * Returns the location of this object (ICRF). Distance is set in pc
	 * if field parallax is not 0, otherwise distance is set to 1.
	 * @return The location.
	 */
	public LocationElement getLocation() {
		double dist = 1.0;
		if (this.parallax != 0) dist = 1000.0 / this.parallax;
		return new LocationElement(this.rightAscension, this.declination, dist);
	}

	/**
	 * Clones this instance.
	 */
	@Override
	public SimbadElement clone()
	{
		SimbadElement s = new SimbadElement();
		s.declination = this.declination;
		s.name = this.name;
		s.otherNames = this.otherNames;
		s.properMotionDEC = this.properMotionDEC;
		s.properMotionRA = this.properMotionRA;
		s.properMotionRadialV = this.properMotionRadialV;
		s.rightAscension = this.rightAscension;
		s.spectralType = this.spectralType;
		s.parallax = this.parallax;
		s.type = this.type;
		s.bMinusV = this.bMinusV;
		return s;
	}
	/**
	 * Returns true if the input object is equals to this instance.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof SimbadElement)) return false;

		SimbadElement that = (SimbadElement) o;

		if (Double.compare(that.rightAscension, rightAscension) != 0) return false;
		if (Double.compare(that.declination, declination) != 0) return false;
		if (Float.compare(that.bMinusV, bMinusV) != 0) return false;
		if (Float.compare(that.properMotionRA, properMotionRA) != 0) return false;
		if (Float.compare(that.properMotionDEC, properMotionDEC) != 0) return false;
		if (Float.compare(that.properMotionRadialV, properMotionRadialV) != 0) return false;
		if (Float.compare(that.parallax, parallax) != 0) return false;
		if (name != null ? !name.equals(that.name) : that.name != null) return false;
		if (!Arrays.equals(otherNames, that.otherNames)) return false;
		if (spectralType != null ? !spectralType.equals(that.spectralType) : that.spectralType != null) return false;

		return !(type != null ? !type.equals(that.type) : that.type != null);
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		result = name != null ? name.hashCode() : 0;
		result = 31 * result + (otherNames != null ? Arrays.hashCode(otherNames) : 0);
		temp = Double.doubleToLongBits(rightAscension);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(declination);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (spectralType != null ? spectralType.hashCode() : 0);
		result = 31 * result + (type != null ? type.hashCode() : 0);
		result = 31 * result + (bMinusV != +0.0f ? Float.floatToIntBits(bMinusV) : 0);
		result = 31 * result + (properMotionRA != +0.0f ? Float.floatToIntBits(properMotionRA) : 0);
		result = 31 * result + (properMotionDEC != +0.0f ? Float.floatToIntBits(properMotionDEC) : 0);
		result = 31 * result + (properMotionRadialV != +0.0f ? Float.floatToIntBits(properMotionRadialV) : 0);
		result = 31 * result + (parallax != +0.0f ? Float.floatToIntBits(parallax) : 0);
		return result;
	}

	/**
	 * Returns a string representation of this Simbad object.
	 */
	@Override
	public String toString() {
		StringBuffer out = new StringBuffer("");
		String sep = FileIO.getLineSeparator(), plus = "";
		if (otherNames != null && otherNames.length>0) {
			for (int i=0; i<otherNames.length; i++) {
				plus += ", " + otherNames[i];
			}
			plus = plus.substring(1).trim();
		}
		out.append(Translate.translate(506)+": "+name+plus+sep);
		out.append(Translate.translate(21)+": "+Functions.formatRA(rightAscension)+sep);
		out.append(Translate.translate(22)+": "+Functions.formatDEC(declination)+sep);
		out.append(Translate.translate(486)+": "+type+sep);
		out.append(Translate.translate(675)+": "+spectralType+sep);
		if (bMinusV == B_MINUS_V_UNAVAILABLE) {
			out.append("B-V: "+Translate.translate(819).toLowerCase()+sep);
		} else {
			out.append("B-V: "+bMinusV+sep);
		}
		out.append("dRA: "+properMotionRA+" (rad/yr)"+sep);
		out.append("dDEC: "+properMotionDEC+" (rad/yr)"+sep);
		out.append("dVr: "+properMotionRadialV+" (km/s)"+sep);
		out.append(Translate.translate(850)+": "+parallax+" (mas)"+sep);
		return out.toString();
	}

	/**
	 * Searches for a deep sky object given it's name.
	 *
	 * @param obj_name Name of the object as given in the catalog of deep
	 * sky objects.
	 * @return The Simbad object with the data for J2000, or null if it is
	 * not found.
	 * @throws JPARSECException If the method fails.
	 */
	public static SimbadElement searchDeepSkyObject(String obj_name)
			throws JPARSECException
	{
		Object[] objs = populate();

		SimbadElement s = null;
		String obj_name2 = obj_name.toLowerCase();
		for (int i = 0; i < objs.length; i++)
		{
			Object[] obj = (Object[]) objs[i];
			String messier = (String) obj[1];
			String name = (String) obj[0];
			String com = "";
			String comments = (String) obj[7];
			int pp = comments.indexOf("Popular name:");
			if (pp>=0) com = comments.substring(pp+14).trim();

			if (name.startsWith("I.")) name = DataSet.replaceAll(name, "I.", "IC ", true);
			if (DataSet.isDoubleFastCheck(name)) {
				try {
					int ngc = Integer.parseInt(FileIO.getField(1, name, " ", true));
					if (ngc > 0) name = "NGC " + name;
				} catch (Exception exc) {}
			}

			LocationElement loc = (LocationElement)obj[3];
			if (obj_name2.equals(name.toLowerCase()) || obj_name.equals(messier.trim()) || obj_name.equals(name+messier) ||
					obj_name.equals(name+messier+" - "+com) ||
					(name.indexOf(" ") > 0 && (name.substring(name.indexOf(" ")).trim()+messier+" - "+com).indexOf(obj_name) == 0)) {
//			if (name.indexOf(obj_name) >= 0 || messier.indexOf(obj_name) >= 0 || com.indexOf(obj_name) >= 0 ||
//					(s == null && obj_name.indexOf(name) >= 0)) {
				s = new SimbadElement(name, loc.getLongitude(), loc.getLatitude());
				s.otherNames = new String[] {messier, com};
				s.type = types[Math.abs((Byte) obj[2])];
				break;
			}
//			if (name.equals(obj_name) || messier.equals(obj_name) || com.equals(obj_name))
//				break;
		}

		if (s == null) {
			for (int i = 0; i < objs.length; i++)
			{
				Object[] obj = (Object[]) objs[i];

				String name = (String) obj[0];
				String messier = (String) obj[1];
				String com = "";
				String comments = (String) obj[7];
				int pp = comments.indexOf("Popular name:");
				if (pp>=0) com = comments.substring(pp+14).trim();

				if (com.toLowerCase().indexOf(obj_name2) >= 0) {
					LocationElement loc = (LocationElement) obj[3];
					s = new SimbadElement(name, loc.getLongitude(), loc.getLatitude());
					s.otherNames = new String[] {messier, com};
					s.type = types[Math.abs((Byte) obj[2])];
					break;
				}
			}
		}

		if (s == null) {
			try { s = SimbadQuery.query(obj_name); } catch (Exception exc) {}
		}
		return s;
	}

	private static String types[] = new String[] {"unk", "gal", "neb", "pneb", "ocl", "gcl", "galpart", "qua", "duplicate", "duplicateInNGC", "star/s", "notFound"};
	private static Object[] populate() throws JPARSECException {
		Object out = DataBase.getData("objectsJ2000", null, true);
		if (out == null) {
			ArrayList<Object[]> outObj = new ArrayList<Object[]>();
			// Connect to the file
			try
			{
				InputStream is = RenderSky.class.getClassLoader().getResourceAsStream(FileIO.DATA_SKY_DIRECTORY + "objects.txt");
				BufferedReader dis = new BufferedReader(new InputStreamReader(is));
				String line;
				while ((line = dis.readLine()) != null)
				{
					String name = FileIO.getField(1, line, " ", true);
					String type = FileIO.getField(2, line, " ", true);
					int tt = DataSet.getIndex(types, type);
					// Don't want to see duplicate objects, stars, inexistents, ...
					if (tt > 7) continue;
					String ra = FileIO.getField(3, line, " ", true);
					String dec = FileIO.getField(4, line, " ", true);
					String mag = FileIO.getField(5, line, " ", true);
					String max = FileIO.getField(6, line, " ", true);
					String min = FileIO.getField(7, line, " ", true);
					String pa = FileIO.getField(8, line, " ", true);
					String com = FileIO.getRestAfterField(8, line, " ", true);

					LocationElement loc = new LocationElement(Double.parseDouble(ra)/Constant.RAD_TO_HOUR, Double.parseDouble(dec)*Constant.DEG_TO_RAD, 1.0);
	//				if (jd != Constant.J2000)
	//					loc = LocationElement.parseRectangularCoordinates(Precession.precess(Constant.J2000, jd,
	//							LocationElement.parseLocationElement(loc), EphemerisElement.REDUCTION_METHOD.IAU_2006));
					if (loc != null) {
						int mes1 = com.indexOf(" M ");
						int mes2 = com.indexOf(" part of M ");
						int mes3 = com.indexOf(" in M ");
						int mes4 = com.indexOf(" near M ");
						int mes5 = com.indexOf(" not M ");
						int mes6 = com.indexOf(" on M ");
						int mes7 = com.indexOf("in M ");
						String messier = "";
						if (mes1 >= 0 && mes2 < 0 && mes3 < 0 && mes4 < 0 && mes5<0 && mes6<0 && mes7<0) {
							messier = com.substring(mes1);
							int c = messier.indexOf(",");
							if (c < 0) c = messier.indexOf(";");
							messier = DataSet.replaceAll(messier.substring(0, c), " ", "", false);
						}
						if (messier.equals("")) {
							int cal = com.indexOf("CALDWELL");
							if (cal >= 0) {
								messier = com.substring(cal);
								int end = messier.indexOf(";");
								int end2 = messier.indexOf(",");
								if (end > 0) messier = messier.substring(0, end);
								end = messier.length();
								if (end2 > 0 && end2 < end) messier = messier.substring(0, end2);
							}
						}

						float maxSize = Float.parseFloat(max), minSize = Float.parseFloat(min);
						if (tt == 6 && maxSize == 0.0) maxSize = minSize = 0.5f/60.0f;
						float paf = -1;
						try {
							if (!pa.equals("-") && !pa.equals(""))
								paf = (float) (Float.parseFloat(pa) * Constant.DEG_TO_RAD);
						} catch (Exception exc) {}
						loc.set(DataSet.toFloatArray(loc.get())); // Reduce memory use
						outObj.add(new Object[] {name, messier, (byte) tt, loc, (float) Double.parseDouble(mag),
								new float[] {maxSize, minSize}, paf, com});
					}
				}
				// Close file
				dis.close();

			} catch (Exception e2)
			{
				throw new JPARSECException(
						"error while reading objects file", e2);
			}
			Object outO[] = new Object[outObj.size()];
			for (int i=0; i<outO.length; i++) {
				outO[i] = outObj.get(i);
			}
			DataBase.addData("objectsJ2000", null, outO, true);
			return outO;
		}
		return (Object[]) out;
	}
}
