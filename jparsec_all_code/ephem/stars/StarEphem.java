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

import jparsec.astronomy.CoordinateSystem;
import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.EphemerisElement.FRAME;
import jparsec.ephem.EphemerisElement.REDUCTION_METHOD;
import jparsec.ephem.Functions;
import jparsec.ephem.IAU2006;
import jparsec.ephem.Nutation;
import jparsec.ephem.Precession;
import jparsec.ephem.RiseSetTransit;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.planets.EphemElement;
import jparsec.ephem.planets.JPLEphemeris;
import jparsec.ephem.planets.PlanetEphem;
import jparsec.ephem.planets.imcce.Series96;
import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.math.Constant;
import jparsec.math.matrix.Matrix;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.observer.ReferenceEllipsoid.ELLIPSOID;
import jparsec.time.SiderealTime;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.time.TimeScale;
import jparsec.util.Configuration;
import jparsec.util.DataBase;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;
import jparsec.util.Translate.LANGUAGE;

/**
 * A class to obtain accurate ephemerides of stars.
 * <BR>
 * To obtain star ephemeris follow these simple steps:
 * <BR>
 *
 * <pre>
 * // Read BSC5 or SKYMASTER 2000 catalog
 * ReadFile rf = new ReadFile();
 * rf.setPath(PATH_TO_BSC5_FILE);
 * rf.setFormat(ReadFile.FORMAT.BSC5);
 * rf.readFileOfStars();
 *
 * // Choose a star.
 * int my_star = rf.searchByName(&quot;Alp UMi&quot;);
 * StarElement star = (StarElement) rf.READ_ELEMENTS.elementAt(my_star);
 *
 * // Calc ephemeris.
 * StarEphemElement star_ephem = StarEphem.StarEphemeris(time, observer, eph, star, true);
 * </pre>
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class StarEphem
{
	// private constructor so that this class cannot be instantiated.
	private StarEphem() {}

	private static ReadFile readFile = null;

	/** Set to true to include stars fainter than magnitude 6.5 when reading the
	 * catalog in this class. Default value is false. */
	public static boolean READ_STARS_BEYOND_MAG_6_5 = false;

	/**
	 * An array holding the indexes of around 100 famous double stars. No guarantee is 
	 * given about including all interesting stars in this list. It comes from the catalog
	 * <i>110 Best Double Stars</i> by the Saguaro Astronomy Club. Some indexes will be 
	 * equal or greater to 8884, requiring to set the flag {@linkplain #READ_STARS_BEYOND_MAG_6_5}
	 * to true to read them.
	 */
	public static final int[] MAIN_DOUBLE_STARS = new int[] {
		0, 3, 6, 14, 21, 23, 31, 48, 62, 67, 70, 73, 83, 93, 103, 122, 125, 150, 164, 185, 195, 211, 214, 
		247, 251, 271, 274, 279, 292, 318, 324, 344, 347, 350, 384, 399, 408, 415, 417, 418, 457, 530, 537, 
		564, 600, 616, 650, 682, 746, 799, 806, 812, 824, 852, 902, 1023, 1040, 1042, 1051, 1098, 1149, 
		1194, 1234, 1306, 1375, 1388, 1412, 1424, 1549, 1600, 1646, 1717, 1727, 1780, 1920, 1963, 1991, 
		2040, 2059, 2326, 2614, 2643, 2994, 3029, 3307, 3355, 3383, 3468, 3597, 3654, 4152, 4164, 4779, 
		5097, 5279, 5588, 6019, 6098, 6582, 6761, 7652, 8399, 8770, 8859, 9333, 11035, 14116, 17881, 18305, 
		89553
	};
	
	/**
	 * An array holding the indexes of 109 famous variable stars. No guarantee is 
	 * given about including all interesting stars in this list. It comes mainly from the book 
	 * <i>Observing Variable Stars - a guide for the beginner</i>, by David H. Levy. The 
	 * original list was filtered to reduce the number of stars to 109. Some indexes will be 
	 * equal or greater to 8884, requiring to set the flag {@linkplain #READ_STARS_BEYOND_MAG_6_5}
	 * to true to read them. The first 12 values in the array are the top 12 naked eye variables 
	 * stars given by Sky &amp; Telescope (http://www.skyandtelescope.com/observing/celestial-objects-to-watch/the-top-12-naked-eye-variable-stars/),
	 * then the list is completed up to 25 with the ones visible to naked eye in H. Levy's book.
	 */
	public static final int[] MAIN_VARIABLE_STARS = new int[] {
		9, 59, 63, 178, 229, 247, 259, 292, 454, 523, 534, 564, 628, 916, 1262, 1516, 1525, 2228, 2399, 2622, 
		3532, 3671, 3799, 4266, 5061, 5610, 5849, 6048, 6167, 6237, 6682, 7632, 7737, 8158, 8667, 8961, 11311, 
		11823, 11973, 12124, 12489, 12635, 12752, 12823, 13336, 14100, 14234, 14273, 14961, 15012, 15104, 
		15861, 17247, 17802, 18286, 18300, 18849, 20206, 20844, 21396, 21599, 22800, 22918, 23016, 23059, 
		24059, 24335, 25848, 27385, 27477, 29790, 29895, 31908, 32513, 33125, 33753, 35452, 36899, 38198, 
		38306, 40432, 40713, 42373, 42644, 43814, 47317, 47406, 47627, 51480, 51545, 52118, 52760, 55292, 
		55338, 55574, 55993, 57600, 58503, 60483, 66828, 68218, 69267, 73336, 75151, 75406, 76444, 76672, 
		77202, 77877
	};
	
	/**
	 * Searchs for a given star in SkyMaster 2000 catalog and returns the index.
	 * @param name Star name.
	 * @return The index, or -1 if no match is found.
	 * @throws JPARSECException If an error occurs.
	 */
	public static int getStarTargetIndex(String name)
	throws JPARSECException {
		if (readFile == null) {
			ReadFile re = new ReadFile();
			re.setPath(StarEphem.PATH_TO_SkyMaster2000_JPARSEC_FILE);
			re.setFormat(ReadFile.FORMAT.JPARSEC_SKY2000);
			re.readFileOfStars();
			readFile = re;

			if (READ_STARS_BEYOND_MAG_6_5) {
				ReadFile re2 = new ReadFile();
				re2.setPath(StarEphem.PATH_TO_SkyMaster2000_JPARSEC_FILE_BEYOND6_5mag);
				re2.setFormat(ReadFile.FORMAT.JPARSEC_SKY2000);
				re2.readFileOfStars();
				readFile.setReadElementsFromArray(DataSet.addObjectArray(re.getReadElements(), re2.getReadElements()));
			}
		}
		int index = readFile.searchByName(StarEphem.getCatalogNameFromProperName(name));
		return index;
	}
	
	/**
	 * Search for a given star in SkyMaster 2000 catalog and returns the index.
	 * @param loc Location of the object to search for. Note this location should be usually
	 * mean J2000 equatorial coordinates.
	 * @param radius Radius in radians around the given position to search for an object.
	 * @return index Indexes of the objects close to the given location, if any, at a distance
	 * lower than radius, ordered by radius in crescent order. null is returned if no match is found.
	 * @throws JPARSECException If an error occurs.
	 */
	public static int[] getStarTargetIndex(LocationElement loc, double radius)
	throws JPARSECException {
		if (readFile == null) {
			ReadFile re = new ReadFile();
			re.setPath(StarEphem.PATH_TO_SkyMaster2000_JPARSEC_FILE);
			re.setFormat(ReadFile.FORMAT.JPARSEC_SKY2000);
			re.readFileOfStars();
			readFile = re;

			if (READ_STARS_BEYOND_MAG_6_5) {
				ReadFile re2 = new ReadFile();
				re2.setPath(StarEphem.PATH_TO_SkyMaster2000_JPARSEC_FILE_BEYOND6_5mag);
				re2.setFormat(ReadFile.FORMAT.JPARSEC_SKY2000);
				re2.readFileOfStars();
				readFile.setReadElementsFromArray(DataSet.addObjectArray(re.getReadElements(), re2.getReadElements()));
			}
		}
		return readFile.searchByPositionGetAll(loc, radius);
	}
	
	/**
	 * Returns the name of a star from the index in SkyMaster 2000 catalog.
	 * @param index Index for the star as sorted in the file.
	 * @return The name.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String getStarName(int index)
	throws JPARSECException {
		if (readFile == null) {
			ReadFile re = new ReadFile();
			re.setPath(StarEphem.PATH_TO_SkyMaster2000_JPARSEC_FILE);
			re.setFormat(ReadFile.FORMAT.JPARSEC_SKY2000);
			re.readFileOfStars();
			readFile = re;

			if (READ_STARS_BEYOND_MAG_6_5) {
				ReadFile re2 = new ReadFile();
				re2.setPath(StarEphem.PATH_TO_SkyMaster2000_JPARSEC_FILE_BEYOND6_5mag);
				re2.setFormat(ReadFile.FORMAT.JPARSEC_SKY2000);
				re2.readFileOfStars();
				readFile.setReadElementsFromArray(DataSet.addObjectArray(re.getReadElements(), re2.getReadElements()));
			}
		}
		String name = readFile.getObjectName(index);
		return name;
	}
	/**
	 * Returns the star element set for a given star using SkyMaster 2000 catalog.
	 * @param index Index for the star.
	 * @return The star element set.
	 * @throws JPARSECException if an error occurs.
	 */
	public static StarElement getStarElement(int index)
	throws JPARSECException {
		if (readFile == null) {
			ReadFile re = new ReadFile();
			re.setPath(StarEphem.PATH_TO_SkyMaster2000_JPARSEC_FILE);
			re.setFormat(ReadFile.FORMAT.JPARSEC_SKY2000);
			re.readFileOfStars();
			readFile = re;

			if (READ_STARS_BEYOND_MAG_6_5) {
				ReadFile re2 = new ReadFile();
				re2.setPath(StarEphem.PATH_TO_SkyMaster2000_JPARSEC_FILE_BEYOND6_5mag);
				re2.setFormat(ReadFile.FORMAT.JPARSEC_SKY2000);
				re2.readFileOfStars();
				readFile.setReadElementsFromArray(DataSet.addObjectArray(re.getReadElements(), re2.getReadElements()));
			}
		}
		StarElement sat = readFile.getStarElement(index);
		return sat;
	}
	/**
	 * Returns the number of stars in SkyMaster 2000 catalog.
	 * @return The number of objects.
	 * @throws JPARSECException If an error occurs.
	 */
	public static int getStarCount()
	throws JPARSECException {
		if (readFile == null) {
			ReadFile re = new ReadFile();
			re.setPath(StarEphem.PATH_TO_SkyMaster2000_JPARSEC_FILE);
			re.setFormat(ReadFile.FORMAT.JPARSEC_SKY2000);
			re.readFileOfStars();
			readFile = re;

			if (READ_STARS_BEYOND_MAG_6_5) {
				ReadFile re2 = new ReadFile();
				re2.setPath(StarEphem.PATH_TO_SkyMaster2000_JPARSEC_FILE_BEYOND6_5mag);
				re2.setFormat(ReadFile.FORMAT.JPARSEC_SKY2000);
				re2.readFileOfStars();
				readFile.setReadElementsFromArray(DataSet.addObjectArray(re.getReadElements(), re2.getReadElements()));
			}
		}
		int n = readFile.getNumberOfObjects();
		return n;
	}
	/**
	 * Resets  the stars read internally in this class, so they will
	 * be read again when requesting ephemerides.
	 */
	public static void resetStars() {
		readFile = null;
	}


	/* Factors to eliminate E terms of aberration */
	private static final double A[] = new double[]
	{ -1.62557e-6, -3.1919e-7, -1.3843e-7 };

	private static final double AD[] = new double[]
//	{ 1.244e-3, -1.579e-3, -6.60e-4 }; // This is found in the original code by Moshier, from AA 1992
  { 1.245e-3, -1.580e-3, -6.59e-4 }; // This is found in AA 2004, based on articles from 1982-3

	/*
	 * Transformation matrix for unit direction vector, and motion vector in arc
	 * seconds per century
	 */
	private static final double MAT[] = new double[]
	{ 0.9999256782, -0.0111820611, -4.8579477e-3, 2.42395018e-6, -2.710663e-8, -1.177656e-8, 0.0111820610,
			0.9999374784, -2.71765e-5, 2.710663e-8, 2.42397878e-6, -6.587e-11, 4.8579479e-3, -2.71474e-5, 0.9999881997,
			1.177656e-8, -6.582e-11, 2.42410173e-6, -5.51e-4, -0.238565, 0.435739, 0.99994704, -0.01118251,
			-4.85767e-3, 0.238514, -2.667e-3, -8.541e-3, 0.01118251, 0.99995883, -2.718e-5, -0.435623, 0.012254,
			2.117e-3, 4.85767e-3, -2.714e-5, 1.00000956 };

	private static final double MAT_INVERSE[] = new double[] {
		  0.999925679464461,  0.011181482851459,  0.004859003846486, -0.000002423898397, -0.000000027105446, -0.000000011777421,
		  -0.011181482771564,  0.999937484931120, -0.000027177091951,  0.000000027105446, -0.000002423927023,  0.000000000065853,
		  -0.004859004003576, -0.000027155783797,  0.999988194635765,  0.000000011777421,  0.000000000065848, -0.000002424049950,
		  -0.000550383713599,  0.238509389858923, -0.435613424180734,  0.999904317129668,  0.011181454040714,  0.004858518649158,
		  -0.238559418959058, -0.002667814477651,  0.012253699727072, -0.011181454113760,  0.999916129088180, -0.000027170347867,
		   0.435729962168090, -0.008540856009088,  0.002116430447234, -0.004858518484394, -0.000027159935551,  0.999966838499726
	};

	/**
	 * Converts FK5 J2000.0 catalog coordinates to FK4 B1950.0 coordinates,
	 * supposing that the object is static.
	 *
	 * @param loc Right Ascension and declination.
	 * @return Output coordinates.
	 * @throws JPARSECException Should not be thrown.
	 */
	public static LocationElement transform_FK5_J2000_to_FK4_B1950(LocationElement loc) throws JPARSECException
	{
		StarElement star = new StarElement(); // proper motions to zero
		star.rightAscension = loc.getLongitude();
		star.declination = loc.getLatitude();
		star.parallax = 0.0;
		star.equinox = Constant.J2000;
		star.frame = EphemerisElement.FRAME.FK5;
		star = StarEphem.transform_FK5_J2000_to_FK4_B1950(star, null);
		return new LocationElement(star.rightAscension, star.declination, loc.getRadius());
	}

	/**
	 * Converts FK4 B1950.0 catalog coordinates to FK5 J2000.0 coordinates,
	 * supposing that the object is static.
	 *
	 * @param loc Right Ascension and declination.
	 * @return Output coordinates.
	 * @throws JPARSECException Should not be thrown.
	 */
	public static LocationElement transform_FK4_B1950_to_FK5_J2000(LocationElement loc) throws JPARSECException
	{
		StarElement star = new StarElement(); // proper motions to zero
		star.rightAscension = loc.getLongitude();
		star.declination = loc.getLatitude();
		star.parallax = 0.0;
		star.equinox = Constant.B1950;
		star.frame = EphemerisElement.FRAME.FK4;
		star = StarEphem.transform_FK4_B1950_to_FK5_J2000(star);
		return new LocationElement(star.rightAscension, star.declination, loc.getRadius());
	}

	/**
	 * Converts FK5 J2000.0 catalog coordinates to FK4 B1950.0 coordinates. AA
	 * page B58. Radial movement is considered only when distance is not zero.
	 * <BR>
	 * Note systematic corrections FK5-FK4 are not considered (see Fricke 1988
	 * to correct for this), and the input J2000 position is precessed to J2000
	 * if the input equinox is different (i.e. equinox is considered to be
	 * the epoch of the observation, which for example corresponds to 1983.5 for
	 * IRAS data).
	 * <BR>
	 * Conversion between B1950 and J2000 coordinates is not a standard procedure
	 * done exactly in the same way in any library. However, this method gives
	 * almost exactly the same conversion as that provided by the CDS web service
	 * or the one described in the Astronomical Almanac Supplement, to a discrepancy
	 * below the milliarcsecond level. Note that for a moving star with non-zero
	 * radial velocity the conversion towards J2000 and back to B1950 could show
	 * inconsistencies close to the milliarcsecond, since radial velocity is supposed
	 * constant between 1950 and 2000 in the conversion methods usually described in
	 * the literature.
	 * <BR>
	 * Main part of the method taken from C code by S. L. Moshier.
	 *
	 * @param s Star input object. Must be FK5, but not necessarily J2000 (automatically corrected).
	 * @param eph Ephemeris properties with the method to apply for precession, if necessary. Classic conversion
	 * methods use IAU 1976 resolutions.
	 * @return Output Star object, FK4 B1950.
	 * @throws JPARSECException If the input frame is not FK5.
	 */
	public static StarElement transform_FK5_J2000_to_FK4_B1950(StarElement s, EphemerisElement eph) throws JPARSECException
	{
		if (s.frame != EphemerisElement.FRAME.FK5) throw new JPARSECException("The frame of the input star is not FK5.");

		StarElement star = s.clone();

		if (star.equinox != Constant.J2000) {
			// Precess to J2000
			LocationElement loc = new LocationElement(star.rightAscension, star.declination, 1.0);
			loc = LocationElement.parseRectangularCoordinates(Precession.precessToJ2000(star.equinox, LocationElement.parseLocationElement(loc), eph));
			star.rightAscension = loc.getLongitude();
			star.declination = loc.getLatitude();
			star.equinox = Constant.J2000;
		}

		LocationElement loc_FK5 = new LocationElement(star.rightAscension, star.declination, 1.0);
		double geo_eq_FK5[] = LocationElement.parseLocationElement(loc_FK5);

		/* space motion */
		double relativisticFactor = 1.0 / (1.0 - star.properMotionRadialV / Constant.SPEED_OF_LIGHT);
		double cte = Constant.SECONDS_PER_DAY * Constant.JULIAN_DAYS_PER_CENTURY * 0.01 / Constant.AU; // 0.2109495...
		double sindec = Math.sin(star.declination);
		double cosdec = Math.cos(star.declination);
		double cosra = Math.cos(star.rightAscension);
		double sinra = Math.sin(star.rightAscension);
		double vpi = cte * star.properMotionRadialV * star.parallax * 0.001 / Constant.RAD_TO_ARCSEC;
		double m[] = new double[3];
		m[0] = -star.properMotionRA * cosdec * sinra - star.properMotionDEC * sindec * cosra + vpi * geo_eq_FK5[0];
		m[1] = star.properMotionRA * cosdec * cosra - star.properMotionDEC * sindec * sinra + vpi * geo_eq_FK5[1];
		m[2] = star.properMotionDEC * cosdec + vpi * geo_eq_FK5[2];

		double R[] = new double[6];
		int i, j;

		for (i = 0; i < 3; i++)
		{
			m[i] *= 100.0 * Constant.RAD_TO_ARCSEC * relativisticFactor;
			R[i] = geo_eq_FK5[i];
			R[i + 3] = m[i];
		}

		/*
		 * Perform matrix multiplication
		 */
		double geo_eq_FK4[] = new double[3];
		double geo_eq_vel_FK4[] = new double[3];
		int v_mat = -1;
		for (i = 0; i < 6; i++)
		{
			double a = 0.0;
			int u_R = -1;
			for (j = 0; j < 6; j++)
			{
				v_mat++;
				u_R++;
				a += R[u_R] * MAT_INVERSE[v_mat];
			}
			if (i < 3)
				geo_eq_FK4[i] = a;
			else
				geo_eq_vel_FK4[i - 3] = a;
		}

		double a = 0.0, b = 0.0, c = 0.0;
		for (i = 0; i < 3; i++)
		{
			a -= A[i] * geo_eq_FK4[i];
			b -= AD[i] * geo_eq_FK4[i];
		}
		/*
		 * Add E terms of aberration from FK4
		 */
		for (i = 0; i < 3; i++)
		{
			geo_eq_FK4[i] = geo_eq_FK4[i] + A[i] + a * geo_eq_FK4[i];
			geo_eq_vel_FK4[i] = geo_eq_vel_FK4[i] + AD[i] + b * geo_eq_FK4[i];
		}

		/*
		 * Transform the answers into B1950 catalog entries in radian measure.
		 */
		b = geo_eq_FK4[0] * geo_eq_FK4[0] + geo_eq_FK4[1] * geo_eq_FK4[1];
		a = b + geo_eq_FK4[2] * geo_eq_FK4[2];
		c = a;
		a = Math.sqrt(a);

		StarElement out = star.clone();
		out.rightAscension = Math.atan2(geo_eq_FK4[1], geo_eq_FK4[0]);
		out.declination = Math.asin(geo_eq_FK4[2] / a);

		/* Note motion converted back to radians per (Julian) year */
		out.properMotionRA = (float) (0.01 * (geo_eq_FK4[0] * geo_eq_vel_FK4[1] - geo_eq_FK4[1] * geo_eq_vel_FK4[0]) / (Constant.RAD_TO_ARCSEC * b * relativisticFactor));
		out.properMotionDEC = (float) (0.01 * (geo_eq_vel_FK4[2] * b - geo_eq_FK4[2] * (geo_eq_FK4[0] * geo_eq_vel_FK4[0] + geo_eq_FK4[1] * geo_eq_vel_FK4[1])) / (Constant.RAD_TO_ARCSEC * c * Math
				.sqrt(b) * relativisticFactor));

		// Pass proper motions from "/Julian year to "/tropical year. This correction
		// depends on the particular case for a given input coordinates, and is usually
		// ignored or already done in input proper motions (javadoc of StarElement.propermotion...
		// already defines that the proper motion should be given in "/Julian year, so this
		// transformation is not done here).
		//out.properMotionRA /= 1.00002136;
		//out.properMotionDEC /= 1.00002136;

		out.properMotionRadialV = star.properMotionRadialV;
		if (!star.isDistanceUnknown())
		{
			c = 0.0;
			for (i = 0; i < 3; i++)
				c += geo_eq_FK4[i] * geo_eq_vel_FK4[i];

			/*
			 * divide by RTS to deconvert m (and therefore c) from arc seconds
			 * back to radians
			 */
			out.properMotionRadialV = (float) (c / (relativisticFactor * cte * 100.0 * a * star.parallax * 0.001));
		}
		out.parallax = star.parallax * a;
		out.equinox = Constant.B1950;
		out.frame = EphemerisElement.FRAME.FK4;

		// Correct for fictitious velocities
		if (star.isDistanceUnknown()) {
			out.properMotionRA = 0.0f;
			out.properMotionDEC = 0.0f;
		}

		return out;
	}

	/**
	 * Converts FK4 B1950.0 catalog coordinates to FK5 J2000.0 coordinates. AA
	 * page B58. Radial movement is considered only when distance is not zero.
	 * <BR>
	 * Note systematic corrections FK5-FK4 are not considered (see Fricke 1988
	 * to correct for this), and the input B1950 position is precessed to B1950
	 * if the input equinox is different (i.e. equinox is considered to be
	 * the epoch of the observation, which for example corresponds to 1983.5 for
	 * IRAS data).
	 * <BR>
	 * Conversion between B1950 and J2000 coordinates is not a standard procedure
	 * done exactly in the same way in any library. However, this method gives
	 * almost exactly the same conversion as that provided by the CDS web service
	 * or the one described in the Astronomical Almanac Supplement, to a discrepancy
	 * below the milliarcsecond level. Note that for a moving star with non-zero
	 * radial velocity the conversion towards J2000 and back to B1950 could show
	 * inconsistencies close to the milliarcsecond, since radial velocity is supposed
	 * constant between 1950 and 2000 in the conversion methods usually described in
	 * the literature.
	 * <BR>
	 * Main part of the method taken from C code by S. L. Moshier.
	 *
	 * @param s Star input object. Must be FK4, but not necessarily B1950 (automatically corrected).
	 * @return Output Star object, FK5 J2000.
	 * @throws JPARSECException If the input frame is not FK4.
	 */
	public static StarElement transform_FK4_B1950_to_FK5_J2000(StarElement s) throws JPARSECException
	{
		if (s.frame != EphemerisElement.FRAME.FK4) throw new JPARSECException("The frame of the input star is not FK4.");

		StarElement star = s.clone();

		if (star.equinox != Constant.B1950) {
			EphemerisElement eph = new EphemerisElement();
			eph.ephemMethod = REDUCTION_METHOD.IAU_1976;

			// Apply correction for the precession constant between Newcomb and IAU 1976 precession methods: 1.13"/century.
			LocationElement loc = new LocationElement(star.rightAscension, star.declination, 1.0);
			double eqc = (star.equinox - Constant.B1950) * 0.07555 * 15.0 * Constant.ARCSEC_TO_RAD / Constant.JULIAN_DAYS_PER_CENTURY;
			loc = LocationElement.parseRectangularCoordinates(Ephem.equatorialToEcliptic(loc.getRectangularCoordinates(), Constant.B1950, eph));
			loc.setLongitude(loc.getLongitude()+eqc);
			loc = LocationElement.parseRectangularCoordinates(Ephem.eclipticToEquatorial(loc.getRectangularCoordinates(), Constant.B1950, eph));

			// Now precess to B1950 using Lieske formula
			loc = LocationElement.parseRectangularCoordinates(Precession.precess(star.equinox, Constant.B1950, loc.getRectangularCoordinates(), eph));
			star.rightAscension = loc.getLongitude();
			star.declination = loc.getLatitude();
			star.equinox = Constant.B1950;
		}
		// Pass proper motions from "/tropical year to "/Julian year. This correction
		// depends on the particular case for a given input coordinates, and is usually
		// ignored or already done in input proper motions (javadoc of StarElement.propermotion...
		// already defines that the proper motion should be given in "/Julian year).
		//star.properMotionRA *= 1.00002136;
		//star.properMotionDEC *= 1.00002136;

		LocationElement loc_FK4 = new LocationElement(star.rightAscension, star.declination, 1.0);
		double geo_eq_FK4[] = LocationElement.parseLocationElement(loc_FK4);

		/* space motion */
		double relativisticFactor = 1.0 / (1.0 - star.properMotionRadialV / Constant.SPEED_OF_LIGHT);
		double cte = Constant.SECONDS_PER_DAY * Constant.JULIAN_DAYS_PER_CENTURY * 0.01 / Constant.AU; // 0.2109495...
		double sindec = Math.sin(star.declination);
		double cosdec = Math.cos(star.declination);
		double cosra = Math.cos(star.rightAscension);
		double sinra = Math.sin(star.rightAscension);
		double vpi = cte * star.properMotionRadialV * star.parallax * 0.001 / Constant.RAD_TO_ARCSEC;
		double m[] = new double[3];
		m[0] = -star.properMotionRA * cosdec * sinra - star.properMotionDEC * sindec * cosra + vpi * geo_eq_FK4[0];
		m[1] = star.properMotionRA * cosdec * cosra - star.properMotionDEC * sindec * sinra + vpi * geo_eq_FK4[1];
		m[2] = star.properMotionDEC * cosdec + vpi * geo_eq_FK4[2];

		double a, b, c;
		double R[] = new double[6];
		int i, j;

		a = 0.0;
		b = 0.0;
		for (i = 0; i < 3; i++)
		{
			m[i] *= 100.0 * Constant.RAD_TO_ARCSEC * relativisticFactor;
			a += A[i] * geo_eq_FK4[i];
			b += AD[i] * geo_eq_FK4[i];
		}
		/*
		 * Remove E terms of aberration from FK4
		 */
		for (i = 0; i < 3; i++)
		{
			R[i] = geo_eq_FK4[i] - A[i] + a * geo_eq_FK4[i];
			R[i + 3] = m[i] - AD[i] + b * geo_eq_FK4[i];
		}

		/*
		 * Perform matrix multiplication
		 */
		double geo_eq_FK5[] = new double[3];
		double geo_eq_vel_FK5[] = new double[3];
		int v_mat = -1;
		for (i = 0; i < 6; i++)
		{
			a = 0.0;
			int u_R = -1;
			for (j = 0; j < 6; j++)
			{
				v_mat++;
				u_R++;
				a += R[u_R] * MAT[v_mat];
			}
			if (i < 3)
				geo_eq_FK5[i] = a;
			else
				geo_eq_vel_FK5[i - 3] = a;
		}

		/*
		 * Transform the answers into J2000 catalog entries in radian measure.
		 */
		b = geo_eq_FK5[0] * geo_eq_FK5[0] + geo_eq_FK5[1] * geo_eq_FK5[1];
		a = b + geo_eq_FK5[2] * geo_eq_FK5[2];
		c = a;
		a = Math.sqrt(a);

		StarElement out = star.clone();
		out.rightAscension = Math.atan2(geo_eq_FK5[1], geo_eq_FK5[0]);
		out.declination = Math.asin(geo_eq_FK5[2] / a);

		/* Note motion converted back to radians per (Julian) year */
		out.properMotionRA = (float) (0.01 * (geo_eq_FK5[0] * geo_eq_vel_FK5[1] - geo_eq_FK5[1] * geo_eq_vel_FK5[0]) / (Constant.RAD_TO_ARCSEC * b * relativisticFactor));
		out.properMotionDEC = (float) (0.01 * (geo_eq_vel_FK5[2] * b - geo_eq_FK5[2] * (geo_eq_FK5[0] * geo_eq_vel_FK5[0] + geo_eq_FK5[1] * geo_eq_vel_FK5[1])) / (Constant.RAD_TO_ARCSEC * c * Math
				.sqrt(b) * relativisticFactor));

		out.properMotionRadialV = star.properMotionRadialV;
		if (!star.isDistanceUnknown())
		{
			c = 0.0;
			for (i = 0; i < 3; i++)
				c += geo_eq_FK5[i] * geo_eq_vel_FK5[i];

			/*
			 * divide by RTS to deconvert m (and therefore c) from arc seconds
			 * back to radians
			 */
			out.properMotionRadialV = (float) (c / (relativisticFactor * cte * 100.0 * a * star.parallax * 0.001));
		}
		out.parallax = star.parallax * a;
		out.equinox = Constant.J2000;
		out.frame = EphemerisElement.FRAME.FK5;

		// Correct for fictitious velocities when input object is static
		if (star.isDistanceUnknown()) {
			out.rightAscension -= out.properMotionRA * 100.0 * (Constant.J2000 - Constant.B1950) / Constant.JULIAN_DAYS_PER_CENTURY;
			out.declination -= out.properMotionDEC * 100.0 * (Constant.J2000 - Constant.B1950) / Constant.JULIAN_DAYS_PER_CENTURY;
			out.properMotionRA = 0.0f;
			out.properMotionDEC = 0.0f;
		}

		return out;
	}

	/**
	 * Transform the elements of a star to another frame and/or epoch/equinox. Transformations of Hipparcos
	 * data to old FK4 frame is not directly supported, but you can use this method to go to FK5 J2000 and then
	 * another method provided to go to FK4 B1950. IAU2006 algorithms are used for precession when changing
	 * the equinox.
	 * <BR>
	 * In case you want to transform Hipparcos data to FK5/ICRF J2000, select as input values for this method
	 * FK5/ICRF frame, J2000 epoch, and J1991.25 equinox. In case you want to obtain the data for FK4 B1950,
	 * use this method with previous values (and FK5 frame) to go to FK5, then force equinox to be J2000 in the
	 * {@linkplain StarElement} object, and then call {@linkplain StarEphem#transform_FK5_J2000_to_FK4_B1950(StarElement, EphemerisElement)}
	 * using IAU1976 algorithms.
	 * <BR>
	 * Main part of the method taken from C code by S. L. Moshier.
	 *
	 * @param s Star input object.
	 * @param outFrame the output reference frame.
	 * @param outputEpoch The output epoch. Proper motion will be applied from input to output epoch. Input epoch
	 * is assumed to be equal to the input equinox.
	 * @param outputEquinox The output equinox. Precession will be applied from input to output equinox, using IAU2006
	 * algorithms.
	 * @return Output Star object.
	 * @throws JPARSECException If the output frame is not supported.
	 */
	public static StarElement transformStarElementsToOutputEquinoxAndFrame(StarElement s, FRAME outFrame,
			double outputEpoch, double outputEquinox) throws JPARSECException
	{
		StarElement star = s.clone();

		if (s.frame == EphemerisElement.FRAME.FK4) star = StarEphem.transform_FK4_B1950_to_FK5_J2000(star);

		if (s.frame == outFrame && s.equinox == outputEquinox && outputEpoch == s.equinox) return star;
		boolean toFK4 = false;
		if (outFrame == FRAME.FK4  && outputEquinox == Constant.B1950) {
			toFK4 = true;
			outFrame = FRAME.FK5;
			outputEquinox = Constant.J2000;
		}

		// Pass proper motions from "/tropical year to "/Julian year. This correction
		// depends on the particular case for a given input coordinates, and is usually
		// ignored or already done in input proper motions (javadoc of StarElement.propermotion...
		// already defines that the proper motion should be given in "/Julian year).
		//star.properMotionRA *= 1.00002136;
		//star.properMotionDEC *= 1.00002136;

		LocationElement loc_inputFrame = new LocationElement(star.rightAscension, star.declination, 1.0);
		double geo_eq_inputFrame[] = LocationElement.parseLocationElement(loc_inputFrame);

		/* space motion */
		double relativisticFactor = 1.0 / (1.0 - star.properMotionRadialV / Constant.SPEED_OF_LIGHT);
		double cte = Constant.SECONDS_PER_DAY * Constant.JULIAN_DAYS_PER_CENTURY * 0.01 / Constant.AU; // 0.2109495...
		double sindec = Math.sin(star.declination);
		double cosdec = Math.cos(star.declination);
		double cosra = Math.cos(star.rightAscension);
		double sinra = Math.sin(star.rightAscension);
		double vpi = cte * star.properMotionRadialV * star.parallax * 0.001 / Constant.RAD_TO_ARCSEC;
		double m[] = new double[3];
		m[0] = -star.properMotionRA * cosdec * sinra - star.properMotionDEC * sindec * cosra + vpi * geo_eq_inputFrame[0];
		m[1] = star.properMotionRA * cosdec * cosra - star.properMotionDEC * sindec * sinra + vpi * geo_eq_inputFrame[1];
		m[2] = star.properMotionDEC * cosdec + vpi * geo_eq_inputFrame[2];

		double R[] = new double[6];
		for (int i = 0; i < 3; i++)
		{
			m[i] *= 100.0 * Constant.RAD_TO_ARCSEC * relativisticFactor;
			R[i] = geo_eq_inputFrame[i]  + m[i] * (outputEpoch - star.equinox) / (Constant.JULIAN_DAYS_PER_CENTURY * Constant.RAD_TO_ARCSEC);
			R[i + 3] = m[i];
		}

		// Precess
		if (star.equinox != outputEquinox) {
			EphemerisElement eph = new EphemerisElement();
			eph.ephemMethod = REDUCTION_METHOD.IAU_2006;
			R = Precession.precessPosAndVelInEquatorial(star.equinox, outputEquinox, R, eph);
		}

		// Change frame
		R = Ephem.toOutputFrame(R, star.frame, outFrame);
		if (R.length < 6) throw new JPARSECException("cannot transform velocities to output frame.");

		double geo_eq_outFrameEq[] = new double[] {R[0], R[1], R[2]};
		double geo_eq_vel_outFrameEq[] = new double[] {R[3], R[4], R[5]};

		/*
		 * Transform the answers into catalog entries in radian measure.
		 */
		double b = geo_eq_outFrameEq[0] * geo_eq_outFrameEq[0] + geo_eq_outFrameEq[1] * geo_eq_outFrameEq[1];
		double a = b + geo_eq_outFrameEq[2] * geo_eq_outFrameEq[2];
		double c = a;
		a = Math.sqrt(a);

		StarElement out = star.clone();
		out.rightAscension = Math.atan2(geo_eq_outFrameEq[1], geo_eq_outFrameEq[0]);
		out.declination = Math.asin(geo_eq_outFrameEq[2] / a);

		/* Note motion converted back to radians per (Julian) year */
		out.properMotionRA = (float) (0.01 * (geo_eq_outFrameEq[0] * geo_eq_vel_outFrameEq[1] - geo_eq_outFrameEq[1] * geo_eq_vel_outFrameEq[0]) / (Constant.RAD_TO_ARCSEC * b * relativisticFactor));
		out.properMotionDEC = (float) (0.01 * (geo_eq_vel_outFrameEq[2] * b - geo_eq_outFrameEq[2] * (geo_eq_outFrameEq[0] * geo_eq_vel_outFrameEq[0] + geo_eq_outFrameEq[1] * geo_eq_vel_outFrameEq[1])) / (Constant.RAD_TO_ARCSEC * c * Math
				.sqrt(b) * relativisticFactor));

		out.properMotionRadialV = star.properMotionRadialV;
		if (!star.isDistanceUnknown())
		{
			c = 0.0;
			for (int i = 0; i < 3; i++)
				c += geo_eq_outFrameEq[i] * geo_eq_vel_outFrameEq[i];

			/*
			 * divide by RTS to deconvert m (and therefore c) from arc seconds
			 * back to radians
			 */
			out.properMotionRadialV = (float) (c / (relativisticFactor * cte * 100.0 * a * star.parallax * 0.001));
		}
		out.parallax = star.parallax * a;
		out.equinox = outputEquinox;
		out.frame = outFrame;

		// Correct for fictitious velocities when input object is static
		if (star.isDistanceUnknown()) {
			out.rightAscension -= out.properMotionRA * 100.0 * (outputEpoch - s.equinox) / Constant.JULIAN_DAYS_PER_CENTURY;
			out.declination -= out.properMotionDEC * 100.0 * (outputEpoch - s.equinox) / Constant.JULIAN_DAYS_PER_CENTURY;
			out.properMotionRA = 0.0f;
			out.properMotionDEC = 0.0f;
		}

		if (toFK4) {
			EphemerisElement eph = new EphemerisElement();
			eph.ephemMethod = REDUCTION_METHOD.IAU_1976;
			out = StarEphem.transform_FK5_J2000_to_FK4_B1950(out, eph);
		}

		return out;
	}

	/**
	 * Calculates ephemerides of stars. This method assumes that the star
	 * velocity is much lower than the speed of the light. This is a valid
	 * approximation for any star in our Galaxy, since velocities are below 1000
	 * km/s.
	 * <BR>
	 * It is not recommended to use this method if the speed of the star is
	 * above 25% of the speed of light.
	 * <BR>
	 * This method takes the star object from the integer number defined
	 * in {@linkplain EphemerisElement#targetBody}.
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object. The index of the star must be added to the index
	 *  property.
	 * @param fullEphemeris True for calculating full ephemeris, including rise,
	 *        set, transit times, constellation, and topocentric corrections.
	 * @return Output ephem object.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public static StarEphemElement starEphemeris(TimeElement time, ObserverElement obs, EphemerisElement eph,
			boolean fullEphemeris) throws JPARSECException
	{
		StarElement star = StarEphem.getStarElement(eph.targetBody.getIndex());
		return starEphemeris(time, obs, eph, star, fullEphemeris);
	}

	/**
	 * Calculates ephemerides of stars. This method assumes that the star
	 * velocity is much lower than the speed of the light. This is a valid
	 * approximation for any star in our Galaxy, since velocities are below 1000
	 * km/s.
	 * <BR>
	 * It is not recommended to use this method if the speed of the star is
	 * above 25% of the speed of light.
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param star Star object.
	 * @param fullEphemeris True for calculating full ephemeris, including rise,
	 *        set, transit times.
	 * @return Output ephem object.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public static StarEphemElement starEphemeris(TimeElement time, ObserverElement obs, EphemerisElement eph,
			StarElement star, boolean fullEphemeris) throws JPARSECException
	{
		// Check Ephemeris object
		if (!EphemerisElement.checkEphemeris(eph))
			throw new JPARSECException("invalid ephemeris object.");

		StarEphemElement out = new StarEphemElement();

		/* Convert from RA and Dec to equatorial rectangular direction	 */
		StarElement in = star.clone();
		// Convert FK4 to FK5
		if (star.frame == EphemerisElement.FRAME.FK4)
			in = StarEphem.transform_FK4_B1950_to_FK5_J2000(in);
		LocationElement loc = new LocationElement(in.rightAscension, in.declination, 1.0);
		double q[] = LocationElement.parseLocationElement(loc);

		// Obtain julian day in Barycentric Dynamical Time
		double JD_TDB = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		EphemerisElement ephClone = eph.clone();
		ephClone.targetBody = TARGET.SUN;
		if (ephClone.algorithm == EphemerisElement.ALGORITHM.SERIES96_MOSHIERForMoon && (JD_TDB < 2415020.5 || JD_TDB > 2488092.5))
			ephClone.algorithm = EphemerisElement.ALGORITHM.MOSHIER;
		if (ephClone.algorithm != EphemerisElement.ALGORITHM.SERIES96_MOSHIERForMoon && ephClone.algorithm != EphemerisElement.ALGORITHM.MOSHIER)
			ephClone.algorithm = EphemerisElement.ALGORITHM.MOSHIER;
		double e[] = new double[6];
		if (ephClone.algorithm == EphemerisElement.ALGORITHM.SERIES96_MOSHIERForMoon)
		{
			e = Series96.getGeocentricPosition(JD_TDB, ephClone.targetBody, 0.0, false, obs);
		} else
		{
			if (eph.algorithm.name().indexOf("JPL") >= 0 ||
					(eph.algorithm == EphemerisElement.ALGORITHM.STAR && eph.preferPrecisionInEphemerides)) {
				try {
					// Use DE406 or the version selected
					JPLEphemeris jplEph = new JPLEphemeris(EphemerisElement.ALGORITHM.JPL_DE406);
					if (eph.algorithm.name().indexOf("JPL") >= 0) jplEph = new JPLEphemeris(eph.algorithm);
					if (!jplEph.isAvailable(JD_TDB)) throw new JPARSECException("JPL integration "+jplEph.getJPLVersion()+" not available for JD = "+JD_TDB+"!");
					ephClone.targetBody = TARGET.Solar_System_Barycenter; // Improves precision
					e = jplEph.getGeocentricPosition(JD_TDB, ephClone.targetBody, 0.0, false, obs);
				} catch (JPARSECException exc) {
					if (eph.algorithm.name().indexOf("JPL") >= 0) throw exc;
						//JPARSECException.addWarning("JPL integration version "+eph.algorithm.name()+" not available. Using Moshier instead.");
					e = PlanetEphem.getGeocentricPosition(JD_TDB, ephClone.targetBody, 0.0, false, obs);
					e = Ephem.eclipticToEquatorial(e, Constant.J2000, ephClone);
				}
			} else {
				e = PlanetEphem.getGeocentricPosition(JD_TDB, ephClone.targetBody, 0.0, false, obs);
				e = Ephem.eclipticToEquatorial(e, Constant.J2000, ephClone);
			}
		}

		/* space motion */
		double relativisticFactor = 1.0 / (1.0 - star.properMotionRadialV / Constant.SPEED_OF_LIGHT);
		double sindec = Math.sin(in.declination);
		double cosdec = Math.cos(in.declination);
		double cosra = Math.cos(in.rightAscension);
		double sinra = Math.sin(in.rightAscension);
		double cte = Constant.SECONDS_PER_DAY * Constant.JULIAN_DAYS_PER_CENTURY * 0.01 / Constant.AU; // 0.2109495...
		double vpi = cte * in.properMotionRadialV * in.parallax * 0.001 / Constant.RAD_TO_ARCSEC;
		double m[] = new double[3];
		m[0] = (-in.properMotionRA * cosdec * sinra - in.properMotionDEC * sindec * cosra + vpi * q[0]) * relativisticFactor;
		m[1] = (in.properMotionRA * cosdec * cosra - in.properMotionDEC * sindec * sinra + vpi * q[1]) * relativisticFactor;
		m[2] = (in.properMotionDEC * cosdec + vpi * q[2]) * relativisticFactor;

		// Add warning for possible incorrect result when the speed is too high
		// (25% c)
		if (vpi != 0) {
			double speed_check = in.properMotionRadialV * LocationElement.parseRectangularCoordinates(m).getRadius() / vpi;
			if (speed_check > (0.00025 * Constant.SPEED_OF_LIGHT))
				JPARSECException
						.addWarning("the speed of the star " + star.name + " is " + speed_check + " km/s, which seems to be very high.");
		}
		
		/* Correct for proper motion and parallax */
		double T = (JD_TDB - in.equinox) * 100.0 / Constant.JULIAN_DAYS_PER_CENTURY;
		double p[] = new double[3];

		// Correction for differential light time
		boolean correction = true;
		if (ephClone.ephemType == EphemerisElement.COORDINATES_TYPE.GEOMETRIC)
			correction = false;
		double light_time_before = in.getDistance() * Constant.RAD_TO_ARCSEC * Constant.LIGHT_TIME_DAYS_PER_AU;
		double dT = 0.0;
		double ddT = 0.0;
		int iter = 0;
		do
		{
			iter ++;
			for (int i = 0; i < 3; i++)
			{
				p[i] = q[i] + (T + dT) * m[i] + e[i] * in.parallax * 0.001 / Constant.RAD_TO_ARCSEC;
			}
			double norm = Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]);
			double light_time_now = in.getDistance() * norm * Constant.RAD_TO_ARCSEC * Constant.LIGHT_TIME_DAYS_PER_AU;
			ddT = dT;
			dT = (light_time_now - light_time_before) * 100.0 / Constant.JULIAN_DAYS_PER_CENTURY;
			ddT -= dT;
		} while (iter < 5 && Math.abs(ddT) > (100.0 * 1.0E-6 / (Constant.SECONDS_PER_DAY * Constant.JULIAN_DAYS_PER_CENTURY)) && correction);

		/* precess the star to J2000 equinox */
		//p = Precession.precessToJ2000(in.equinox, p, ephClone.ephemMethod);

		/* Find vector from earth in direction of object, in AU units */
		double EO = in.getDistance() * Constant.RAD_TO_ARCSEC;
		for (int i = 0; i < 3; i++)
		{
			p[i] = p[i] * EO;
		}

		// Correct for solar deflection and aberration
		if (ephClone.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT)
		{
			LocationElement loc_p = LocationElement.parseRectangularCoordinates(p);
			double light_time = loc_p.getRadius() * Constant.LIGHT_TIME_DAYS_PER_AU;
			if (eph.preferPrecisionInEphemerides)
				//p = Ephem.solarDeflection(p, e, Functions.substract(p, e));
				p = Ephem.solarAndPlanetaryDeflection(p, e, Functions.substract(p, e),
						new TARGET[] {TARGET.JUPITER}, JD_TDB, false, obs);
			p = Ephem.aberration(p, e, light_time);

			DataBase.addData("GCRS", p, true);
		} else {
			DataBase.addData("GCRS", null, true);
		}

		/* Correction to output frame. */
		p = Ephem.toOutputFrame(p, in.frame, eph.frame);

		double geo_date[];
		if (eph.frame == FRAME.FK4) {
			// Transform from B1950 to mean equinox of date
			 geo_date = Precession.precess(Constant.B1950, JD_TDB, p, eph);
		} else {
			// Transform from J2000 to mean equinox of date
			geo_date = Precession.precessFromJ2000(JD_TDB, p, ephClone);
		}

		// Mean equatorial to true equatorial
		double true_eq[] = geo_date;
		if (obs.getMotherBody() == TARGET.EARTH) {
			if (ephClone.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT)
				/* Correct nutation */
				true_eq = Nutation.nutateInEquatorialCoordinates(JD_TDB, ephClone, geo_date, true);

			// Correct for polar motion
			if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT &&
					eph.correctForPolarMotion)
			{
				double gast = SiderealTime.greenwichApparentSiderealTime(time, obs, eph);
				true_eq = Functions.rotateZ(true_eq, -gast);
				Matrix mat = IAU2006.getPolarMotionCorrectionMatrix(time, obs, eph);
				true_eq = mat.times(new Matrix(true_eq)).getColumn(0);
				true_eq = Functions.rotateZ(true_eq, gast);
			}
		}

		// Pass to coordinates as seen from another body, if necessary
		if (obs.getMotherBody() != TARGET.NOT_A_PLANET && obs.getMotherBody() != TARGET.EARTH)
			true_eq = Ephem.getPositionFromBody(LocationElement.parseRectangularCoordinates(true_eq), time, obs, eph).getRectangularCoordinates();

		// Get equatorial coordinates
		LocationElement ephem_loc = LocationElement.parseRectangularCoordinates(true_eq);
		out.rightAscension = ephem_loc.getLongitude();
		out.declination = ephem_loc.getLatitude();
		out.distance = ephem_loc.getRadius() * Constant.ARCSEC_TO_RAD;
		out.magnitude = in.magnitude + (float) (5.0 * Math.log(out.distance / in.getDistance()) / Math.log(10.0));
		if (in.isDistanceUnknown()) out.distance = StarElement.DISTANCE_UNKNOWN;
		out.name = star.name;

		/* Topocentric correction */
		EphemElement ephem = EphemElement.parseStarEphemElement(out);
		if (ephClone.isTopocentric)
			ephem = Ephem.topocentricCorrection(time, obs, eph, ephem);

		/* Horizontal coordinates */
		if (ephClone.isTopocentric)
			ephem = Ephem.horizontalCoordinates(time, obs, eph, ephem);

		ephem.constellation = "";
		try {
			LocationElement locE = ephem.getEquatorialLocation();
			if (obs.getMotherBody() != TARGET.NOT_A_PLANET && obs.getMotherBody() != TARGET.EARTH)
				locE = Ephem.getPositionFromEarth(locE, time, obs, eph);
			String constel = jparsec.astronomy.Constellation.getConstellationName(locE.getLongitude(),
					locE.getLatitude(), JD_TDB, ephClone);
			ephem.constellation = constel;
		} catch (Exception exc) {}

		/* Set coordinates to the output equinox */
		if (EphemerisElement.EQUINOX_OF_DATE != ephClone.equinox)
			ephem = Ephem.toOutputEquinox(ephem, eph, JD_TDB);

		// Get results
		out.rightAscension = ephem.rightAscension;
		out.declination = ephem.declination;
		out.distance = ephem.distance / Constant.RAD_TO_ARCSEC;
		out.magnitude = ephem.magnitude;
		out.azimuth = ephem.azimuth;
		out.elevation = ephem.elevation;
		out.paralacticAngle = ephem.paralacticAngle;
		out.constellation = ephem.constellation;

		if (fullEphemeris)
		{
			Object gcrs = DataBase.getData("GCRS", true);

			ephClone.algorithm = EphemerisElement.ALGORITHM.STAR;
			ephClone.targetBody = TARGET.NOT_A_PLANET;
			ephClone.targetBody.setIndex(eph.targetBody.getIndex());
			if (ephClone.isTopocentric)
				ephem = RiseSetTransit.obtainCurrentRiseSetTransit(time, obs, ephClone, ephem, RiseSetTransit.TWILIGHT.HORIZON_ASTRONOMICAL_34arcmin);

			// Get results
			if (ephem.rise != null) out.rise = ephem.rise[0];
			if (ephem.set != null) out.set = ephem.set[0];
			if (ephem.transit != null) out.transit = ephem.transit[0];
			if (ephem.transitElevation != null) out.transitElevation = ephem.transitElevation[0];

			DataBase.addData("GCRS", gcrs, true);
		}

		return out;
	}

	/**
	 * Default path to the file of BSC5, including extension.
	 */
	public static final String PATH_TO_BSC5_FILE = FileIO.DATA_STARS_BSC5_DIRECTORY + "BrightStarCatalogue5.txt";
	/**
	 * Default path to the file of SkyMaster 2000 in JPARSEC format, including
	 * extension.
	 */
	public static final String PATH_TO_SkyMaster2000_JPARSEC_FILE = FileIO.DATA_STARS_SKY2000_DIRECTORY + "JPARSEC_Sky2000.txt";
	/**
	 * Default path to the file of SkyMaster 2000 in JPARSEC format, including
	 * extension, for the stars beyond magnitude 6.5.
	 */
	public static final String PATH_TO_SkyMaster2000_JPARSEC_FILE_BEYOND6_5mag = FileIO.DATA_STARS_SKY2000_DIRECTORY + "JPARSEC_Sky2000_plus.txt";

	/**
	 * Transforms the popular name of a star into the catalog name to allow to
	 * search for it . This should always be done since certain popular stars like
	 * 'Alp Cen' are identified in the Sky Master 2000 as 'Alp1 Cen', the primary
	 * component.
	 *
	 * @param name Proper star name, for example "Vega" or "Polaris".
	 * @return Name of the closets match found, for example "Alp Lyr" or "Alp
	 *         UMi", or the same input name if no match is found.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String getCatalogNameFromProperName(String name) throws JPARSECException
	{
		String star_name = name;

		Object o = DataBase.getData("starNames_all", null, true);
		String[] starNames = null;
		if (o != null) starNames = (String[]) o;
		if (starNames == null)
		{
			starNames = DataSet.arrayListToStringArray(ReadFile.readResource(FileIO.DATA_SKY_DIRECTORY + "star_names.txt"));
			DataBase.addData("starNames_all", null, starNames, true);
		}

		String name2 = name.toLowerCase();
		for (int n = 0; n < starNames.length; n++)
		{
			String line[] = DataSet.toStringArray(starNames[n].toLowerCase(), ";");
			int aa = DataSet.getIndex(line, name2);
			if (aa >= 0)
			{
				star_name = FileIO.getField(1, starNames[n], ";", true);
				break;
			}
		}

		// Correct for double stars
		String wrongNames[] = new String [] {
				"Alp Cen","The Eri","Alp Cru","Alp Cru","Gam And","Gam Leo","Bet Sco","Alp Lib","Alp CVn","Bet Cyg","Bet Cap","Alp Her","Alp Cap","Psi Dra","Gam Ari"
		};
		String okNames[] = new String[] {
				"Alp1 Cen","The1 Eri","Alp1 Cru","Alp1 Cru","Gam1 And","Gam1 Leo","Bet1 Sco","Alp1 Lib","Alp1 CVn","Bet1 Cyg","Bet1 Cap","Alp1 Her","Alp1 Cap","Psi1 Dra","Gam1 Ari"
		};
		int index = DataSet.getIndex(wrongNames, star_name);
		if (index >= 0) star_name = okNames[index];

		return star_name;
	}

	/**
	 * Returns the proper name of a star given its catalog name.
	 * @param catalogName Catalog name.
	 * @return Proper name, or null if the star has no proper name.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String getStarProperNameFromCatalogName(String catalogName)
	throws JPARSECException {
		Object o2 = null, o = null;
		o2 = DataBase.getDataForAnyThread("starNames2", true);
		o = DataBase.getDataForAnyThread("starNames", true);
		String[] names2 = null, names = null;
		if (o2 == null) {
			String data[] = DataSet.arrayListToStringArray(ReadFile.readResource(FileIO.DATA_SKY_DIRECTORY + "star_names.txt"));
			names2 = DataSet.extractColumnFromTable(data, ";", 0);
			if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) {
				names = DataSet.extractColumnFromTable(data, ";", 2);
			} else {
				names = DataSet.extractColumnFromTable(data, ";", 1);
			}
		} else {
			names2 = (String[]) o2;
			names = (String[]) o;
		}

		String properName = catalogName;

		catalogName = DataSet.replaceOne(catalogName, "Alp1 ", "Alp ", 1);
		catalogName = DataSet.replaceOne(catalogName, "Bet1 ", "Bet ", 1);
		catalogName = DataSet.replaceOne(catalogName, "Gam1 ", "Gam ", 1);
		catalogName = DataSet.replaceOne(catalogName, "The1 ", "The ", 1);
		catalogName = DataSet.replaceOne(catalogName, "Psi1 ", "Psi ", 1);
		
		int bracket1 = catalogName.indexOf("(");
		int bracket2 = catalogName.indexOf(")");
		if (bracket1 >= 0 && bracket2 >= 0)
		{
			properName = catalogName.substring(bracket1 + 1, bracket2);
		} else {
			try {
				int i = Integer.parseInt(catalogName); // Ensure is an int
				properName = Integer.toString(i);
			} catch (Exception exc) {
				//return null;
			}
		}

		for (int n = 0; n < names2.length; n++)
		{
			String line = names2[n];
			int aa = line.toLowerCase().indexOf(properName.toLowerCase());
			if (aa >= 0)
			{
				return names[n];
			}
		}
		return null;
	}
	
	/**
	 * Location of the LSR in J2000, 18h 03m 50.2s, 30&deg; 00' 16.8", and
	 * 19.5 km/s of speed set as radius.
	 */
	public static final LocationElement LSR_J2000_direction = new LocationElement(
			Functions.parseRightAscension("18h 03m 50.2s"),
			Functions.parseDeclination("30\u00b0 00' 16.8\""), 19.5
			);

	/**
	 * Transforms radial velocity from heliocentric to LSR.
	 * <BR>
	 * The sun has a systematic motion relative to nearby stars, the mean
	 * depending on the spectral type of the stars used for comparison. The
	 * standard solar motion is defined to be the average velocity of spectral
	 * types A through G as found in general catalogs of radial velocity,
	 * regardless of luminosity class. This motion is 19.5 km/s toward 18 hrs
	 * right ascension and 30&deg; declination for epoch 1900.0 (galactic
	 * co-ordinates l=56&deg;, b=23&deg;). Basic solar motion is the most probable
	 * velocity of stars in the solar neighborhood, so it is weighted more
	 * heavily by the radial velocities of stars of the most common spectral
	 * types (A, gK, dM) in the solar vicinity. In this system, the sun moves at
	 * 15.4 km/s toward l=51&deg;, b=23&deg;.
	 * <BR>
	 * The conventional local standard of rest used for galactic studies is
	 * essentially based on the standard solar motion. It assumes the sun to
	 * move at the rounded velocity of 20.0 km/s toward 18 hrs right ascension
	 * and 30&deg; declination for epoch 1900.0. This choice presumes that the
	 * earlier spectral types involved in determining the standard solar motion,
	 * being younger, more closely represent the velocity of the interstellar
	 * gas.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param star Input star with position and proper motions.
	 * @return Radial velocity measured in conventional LSR.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double getLSRradialVelocity(TimeElement time, ObserverElement obs,
			EphemerisElement eph, StarElement star) throws JPARSECException
	{
		StarEphemElement sephem = StarEphem.starEphemeris(time, obs, eph, star, false);
		double alpha = sephem.rightAscension;
		double delta = sephem.declination;

		double JD = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		double qLSR[] = LocationElement.parseLocationElement(LSR_J2000_direction);
		if (eph.getEpoch(JD) != Constant.J2000)
			qLSR = Precession.precess(Constant.J2000, JD, LocationElement.parseLocationElement(LSR_J2000_direction),
					eph);
		LocationElement locLSR = LocationElement.parseRectangularCoordinates(qLSR);
		double lsrRA = locLSR.getLongitude(), lsrDEC = locLSR.getLatitude();
		double speed_LSR = Math.cos(lsrRA) * Math.cos(lsrDEC) * Math.cos(alpha) * Math.cos(delta);
		speed_LSR = speed_LSR + Math.sin(lsrRA) * Math.cos(lsrDEC) * Math.sin(alpha) * Math.cos(delta) + Math.sin(lsrDEC) * Math.sin(delta);
		speed_LSR = locLSR.getRadius() * speed_LSR + star.properMotionRadialV;

		return speed_LSR;
	}

	/**
	 * Computes the galactic motion of an object. Method taken from program
	 * http://idlastro.gsfc.nasa.gov/ftp/pro/astro/gal_uvw.pro.<BR>
	 * Follows the general outline of Johnson &amp; Soderblom (1987, AJ, 93,864)
	 * except that the J2000 transformation matrix to Galactic coordinates is
	 * taken from the introduction to the Hipparcos catalog.<BR>
	 * Authors: W. Landsman and Sergey Koposov.
	 * @param star The star object containing J2000 object position of proper motions.
	 * @param toLSR True to return the motion respect the LSR instead of respect the Sun.
	 * True will add to the result (U,V,W)_Sun = (-8.5, 13.38, 6.49), taken from
	 * Coskunoglu et al. 2011 (MNRAS 412, 1237). Despite that UVW errors are (0.29, 0.43, 0.26), these
	 * and previous values considered as correct are in clear disagreement, so the solar
	 * motion through the LSR remains poorly determined.
	 * @return The U, V, W components of the galactic motion in km/s. U is positive
	 * towards galactic center, V positive towards galactic rotation direction, and
	 * W positive towards galactic north pole.
	 */
	public static double[] getGalacticMotionUVW(StarElement star, boolean toLSR) {
		double ra = star.rightAscension, dec = star.declination;
		double vrad = star.properMotionRadialV, plx = 1000.0 * star.parallax;
		double pmra = star.properMotionRA * Constant.RAD_TO_ARCSEC;
		double pmdec = star.properMotionDEC * Constant.RAD_TO_ARCSEC;

		double cosd = Math.cos(dec);
		double sind = Math.sin(dec);
		double cosa = Math.cos(ra);
		double sina = Math.sin(ra);

		double k = Constant.AU / (Constant.SECONDS_PER_DAY * 365.25); // Equivalent of 1 A.U/yr in km/s

		// J2000 to galactic coordinates, following Hipparcos document
		double A_G[][] = new double[][] {
				new double[] {0.0548755604, 0.8734370902, 0.4838350155},
				new double[] {0.4941094279, -0.4448296300, 0.7469822445},
				new double[] {-0.8676661490, -0.1980763734, 0.4559837762}
		};

  		double vec1 = vrad;
		double vec2 = k * pmra / plx;
		double vec3 = k * pmdec / plx;

		double u = -(( A_G[0][0]*cosa*cosd+A_G[0][1]*sina*cosd+A_G[0][2]*sind)*vec1+
				     (-A_G[0][0]*sina     +A_G[0][1]*cosa                   )*vec2+
				     (-A_G[0][0]*cosa*sind-A_G[0][1]*sina*sind+A_G[0][2]*cosd)*vec3);
		double v = ( A_G[1][0]*cosa*cosd+A_G[1][1]*sina*cosd+A_G[1][2]*sind)*vec1+
				     (-A_G[1][0]*sina     +A_G[1][1]*cosa                   )*vec2+
				     (-A_G[1][0]*cosa*sind-A_G[1][1]*sina*sind+A_G[1][2]*cosd)*vec3;
		double w = ( A_G[2][0]*cosa*cosd+A_G[2][1]*sina*cosd+A_G[2][2]*sind)*vec1+
				     (-A_G[2][0]*sina     +A_G[2][1]*cosa                   )*vec2+
				     (-A_G[2][0]*cosa*sind-A_G[2][1]*sina*sind+A_G[2][2]*cosd)*vec3;

		double lsr_vel[] = new double[] {8.5, 13.38, 6.49};
		if (toLSR) {
			u = u + lsr_vel[0];
			v = v + lsr_vel[1];
			w = w + lsr_vel[2];
		}
		return new double[] {u, v, w};
	}

	/**
	 * Transforms radial velocity from heliocentric to topocentric, or to
	 * geocentric if the ephemeris object is set to geocentric calculations.
	 * No relativistic corrections are applied in this method, and Moshier
	 * algorithms are used for the position of the Earth.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param star Input star with position and proper motions.
	 * @return Radial velocity measured respect observer in km/s.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double getRadialVelocity(TimeElement time, ObserverElement obs,
			EphemerisElement eph, StarElement star) throws JPARSECException
	{
		EphemerisElement newEph = eph.clone();
		newEph.equinox = EphemerisElement.EQUINOX_J2000;
		StarEphemElement sephem = StarEphem.starEphemeris(time, obs, newEph, star, false);
		double delta = sephem.declination;

		LocationElement ecl = CoordinateSystem.equatorialToEcliptic(sephem.getEquatorialLocation(), time, obs, newEph);
		double beta = ecl.getLatitude();
		double lam = ecl.getLongitude();
		double tsl = SiderealTime.apparentSiderealTime(time, obs, newEph);
		double ha = tsl - sephem.rightAscension;
		newEph.targetBody = TARGET.SUN;
		newEph.ephemType = EphemerisElement.COORDINATES_TYPE.GEOMETRIC;
		EphemElement ephemSun = PlanetEphem.MoshierEphemeris(time, obs, newEph);
		ecl = CoordinateSystem.equatorialToEcliptic(ephemSun.getEquatorialLocation(), time, obs, newEph);
		double lsun = ecl.getLongitude();
		double JD = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		double v[] = PlanetEphem.getGeocentricPosition(JD, TARGET.SUN, 0.0, false, obs);

		v = DataSet.getSubArray(v, 3, 5);
		v[0] *= Constant.AU / Constant.SECONDS_PER_DAY;
		v[1] *= Constant.AU / Constant.SECONDS_PER_DAY;
		v[2] *= Constant.AU / Constant.SECONDS_PER_DAY;
		double v0 = Math.sqrt(v[0]*v[0]+v[1]*v[1]+v[2]*v[2]);

		// Can also be used the simple code below, but result is slightly different
		//DoubleVector dv = new DoubleVector(DataSet.applyFunction("x*"+Constant.AU+"/"+Constant.SECONDS_PER_DAY, DataSet.getSubArray(v, 3, 5)));
		//double v0 = dv.norm2();

		double vhel = -v0 * Math.cos(beta) * Math.sin(lsun - lam); // Note this should be geocentric beta
		ELLIPSOID ref = obs.getEllipsoid();
		double ver = obs.getMotherBodyMeanRotationRate(eph) * (ref.getRadiusAtLatitude(obs.getLatitudeRad()) + obs.getHeight() * 0.001) * Math.sin(ha) * Math.cos(delta) * Math.cos(obs.getLatitudeRad());
		if (!eph.isTopocentric) ver = 0.0;
		return vhel + ver + star.properMotionRadialV;
	}

	/**
	 * Transforms radial velocity from heliocentric to topocentric, or to
	 * geocentric if the ephemeris object is set to geocentric calculations.
	 * No relativistic corrections are applied in this method, and Moshier
	 * algorithms are used for the position of the Earth.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param locEq Equatorial position of the object.
	 * @return Radial velocity measured respect observer in km/s. Note the proper
	 * radial velocity of the input source is not considered, add it to the
	 * result if required.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double getRadialVelocity(TimeElement time, ObserverElement obs,
			EphemerisElement eph, LocationElement locEq) throws JPARSECException
	{
		EphemerisElement newEph = eph.clone();
		newEph.equinox = EphemerisElement.EQUINOX_J2000;

		LocationElement ecl = CoordinateSystem.equatorialToEcliptic(locEq, time, obs, newEph);
		double beta = ecl.getLatitude();
		double lam = ecl.getLongitude();
		double tsl = SiderealTime.apparentSiderealTime(time, obs, newEph);
		double ha = tsl - locEq.getLongitude();
		newEph.targetBody = TARGET.SUN;
		newEph.ephemType = EphemerisElement.COORDINATES_TYPE.GEOMETRIC;
		EphemElement ephemSun = PlanetEphem.MoshierEphemeris(time, obs, newEph);
		ecl = CoordinateSystem.equatorialToEcliptic(ephemSun.getEquatorialLocation(), time, obs, newEph);
		double lsun = ecl.getLongitude();
		double JD = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		double v[] = PlanetEphem.getGeocentricPosition(JD, TARGET.SUN, 0.0, false, obs);

		v = DataSet.getSubArray(v, 3, 5);
		v[0] *= Constant.AU / Constant.SECONDS_PER_DAY;
		v[1] *= Constant.AU / Constant.SECONDS_PER_DAY;
		v[2] *= Constant.AU / Constant.SECONDS_PER_DAY;
		double v0 = Math.sqrt(v[0]*v[0]+v[1]*v[1]+v[2]*v[2]);

		// Can also be used the simple code below, but result is slightly different
		//DoubleVector dv = new DoubleVector(DataSet.applyFunction("x*"+Constant.AU+"/"+Constant.SECONDS_PER_DAY, DataSet.getSubArray(v, 3, 5)));
		//double v0 = dv.norm2();

		double vhel = -v0 * Math.cos(beta) * Math.sin(lsun - lam); // Note this should be geocentric beta
		ELLIPSOID ref = obs.getEllipsoid();
		double ver = obs.getMotherBodyMeanRotationRate(eph) * (ref.getRadiusAtLatitude(obs.getLatitudeRad()) + obs.getHeight() * 0.001) * Math.sin(ha) * Math.cos(locEq.getLatitude()) * Math.cos(obs.getLatitudeRad());
		if (!eph.isTopocentric) ver = 0.0;
		return vhel + ver;
	}

	private static double[] getSSBPosition(TimeElement time, ObserverElement observer, EphemerisElement eph,
			double JD_TDB, double lightTime) throws JPARSECException {
		double pos_SSB[] = null;
		String a = "JPL DE406";
		if (eph.preferPrecisionInEphemerides) {
			try {
				JPLEphemeris jpl = new JPLEphemeris(EphemerisElement.ALGORITHM.JPL_DE406);
				if (eph.algorithm.name().indexOf("JPL") >= 0) {
					a = eph.algorithm.name();
					jpl = new JPLEphemeris(eph.algorithm, Configuration.JPL_EPHEMERIDES_FILES_EXTERNAL_PATH);
				}
				pos_SSB = jpl.getGeocentricPosition(JD_TDB, TARGET.Solar_System_Barycenter, lightTime, false, observer);
			} catch (Exception exc) {
				JPARSECException.addWarning(a+" could not be found in the classpath, using Sun position from Moshier algorithms instead.");
				pos_SSB = Ephem.eclipticToEquatorial(PlanetEphem.getGeocentricPosition(JD_TDB, TARGET.SUN, lightTime, false, observer),
						Constant.J2000, eph);
			}
		} else {
			pos_SSB = Ephem.eclipticToEquatorial(PlanetEphem.getGeocentricPosition(JD_TDB, TARGET.SUN, lightTime, false, observer),
					Constant.J2000, eph);
		}
		// Topocentric observer should not be corrected for nutation, although the effect is
		// well below the microsecond level. A better way is to avoid aberration in stars,
		// so that deflection is also considered.
		return Functions.substract(pos_SSB, observer.topocentricObserverICRF(time, eph));
	}

	/**
	 * Returns the light time to a star from the Solar System Barycenter (SSB) and the Earth.
	 * Precision of this method should be at the level of +/- 0.05 ms if JPL DE406 (or the selected JPL
	 * integration) is available. In case the files are not found (or the ephemerides are not configured as
	 * high precision at {@linkplain EphemerisElement#preferPrecisionInEphemerides}), Sun position (using Moshier algorihtms)
	 * is used instead of the Solar System barycenter. In that case precision
	 * will be +/- 4s or better (obviously these values are relative between computations,
	 * not absolute since stellar distances are not known to such level of precision).<BR>
	 * The difference between the light-time from SSB and from Earth (calculated from the
	 * distance of the star to the observer) allows to correct time events observed from Earth
	 * and refer them to an uniform time scale defined by the movement of the barycenter
	 * of the Solar System in the space.<BR>
	 * Note that the difference between JPL DE406 and DE422 is of a few us, so precision can never be
	 * better than that.
	 * @param time The time.
	 * @param observer The observer at Earth (possibly) used to obtain the star position.
	 * @param ephIn The ephemeris properties. Only geocentric/topocentric flag and ephemeris
	 * reduction method are considered, to rest is set to J2000 equinox, ICRF frame, and astrometric
	 * coordinates. The algorithm is also taken into account if it corresponds to a JPL ephemeris
	 * version supported and available, otherwise DE406 is used (if available). As a last chance,
	 * Moshier is used.
	 * @param star The properties of the star.
	 * @return The light time to the star from the Solar System Barycenter and the Earth (second
	 * component of the output array), in days.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double[] getLightTimeFromSSBandEarth(TimeElement time, ObserverElement observer, EphemerisElement ephIn,
			StarElement star) throws JPARSECException {

		EphemerisElement eph = ephIn.clone();
		eph.equinox = EphemerisElement.EQUINOX_J2000;
		eph.frame = EphemerisElement.FRAME.ICRF;
		eph.targetBody = TARGET.NOT_A_PLANET;
		eph.algorithm = EphemerisElement.ALGORITHM.STAR;
		eph.ephemType = EphemerisElement.COORDINATES_TYPE.GEOMETRIC; // Neglect light deflection

		// Get star properties to J2000 equinox and FK5/ICRF frame
		StarElement newStar = star.clone();
		if (newStar.frame == EphemerisElement.FRAME.FK4) newStar = StarEphem.transform_FK4_B1950_to_FK5_J2000(newStar);
		if (newStar.equinox != Constant.J2000) {
			// Precess to J2000
			LocationElement loc = new LocationElement(newStar.rightAscension, newStar.declination, 1.0);
			loc = LocationElement.parseRectangularCoordinates(Precession.precessToJ2000(newStar.equinox, LocationElement.parseLocationElement(loc), eph));
			newStar.rightAscension = loc.getLongitude();
			newStar.declination = loc.getLatitude();
			newStar.equinox = Constant.J2000;
		}

		double pc2au = Constant.PARSEC / (1000.0 * Constant.AU);
		StarEphemElement ephem_star = StarEphem.starEphemeris(time, observer, eph, newStar, false);
		double lightTimeInDaysFromEarth = ephem_star.distance * pc2au * Constant.LIGHT_TIME_DAYS_PER_AU;

		double JD_TDB = TimeScale.getJD(time, observer, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		double pos_star[] = ephem_star.getEquatorialLocation().getRectangularCoordinates();
		pos_star = Functions.scalarProduct(pos_star, Constant.PARSEC / (1000.0 * Constant.AU)); // to AU, same as planetary ephemerides

		// Get topocentric/geocentric position of the Solar System barycenter
		double lightTime = 0.0;
		double pos_SSB[] = getSSBPosition(time, observer, ephIn, JD_TDB, lightTime);

		// Get position from SSB
		double pos_star_SSB[] = Functions.substract(pos_star, pos_SSB);
		double lightTimeInDaysFromSSB = LocationElement.parseRectangularCoordinates(pos_star_SSB).getRadius() * Constant.LIGHT_TIME_DAYS_PER_AU;

		// Correct from different light-time SSB-Earth (The barycenter and the star have moved in those +/- 8 minutes)
		// This effect is generally below 1s, and should be done with a StarElement referred to an
		// equinox close to J2000. This is corrected at the beginning of this method.
		// This should be an iteration, but here I simply use a first order approximation.
		lightTime = -(lightTimeInDaysFromSSB - lightTimeInDaysFromEarth);
		pos_SSB = getSSBPosition(time, observer, ephIn, JD_TDB, lightTime);
		pos_star_SSB = Functions.substract(pos_star, pos_SSB);
		lightTimeInDaysFromSSB = LocationElement.parseRectangularCoordinates(pos_star_SSB).getRadius() * Constant.LIGHT_TIME_DAYS_PER_AU;

		double dif = (lightTimeInDaysFromSSB - lightTimeInDaysFromEarth) * newStar.properMotionRadialV * 1000.0 / Constant.SPEED_OF_LIGHT;
		lightTimeInDaysFromSSB += dif;

		return new double[] {lightTimeInDaysFromSSB, lightTimeInDaysFromEarth};
	}

	/**
	 * Returns the light time to a star from the Solar System Barycenter (SSB).
	 * Precision of this method should be at the level of +/- 0.05 ms if JPL DE406 (or the selected
	 * JPL integration) is available. In case the files are not found (or the ephemerides are not configured as
	 * high precision at {@linkplain EphemerisElement#preferPrecisionInEphemerides}), Sun position (using Moshier algorihtms)
	 * is used instead of the Solar System barycenter. In that case precision
	 * will be +/- 4s or better (obviously these values are relative between computations,
	 * not absolute since stellar distances are not known to such level of precision).<BR>
	 * The difference between the light-time from SSB and from Earth (calculated from the
	 * distance of the star to the observer) allows to correct time events observed from Earth
	 * and refer them to an uniform time scale defined by the movement of the barycenter
	 * of the Solar System in the space.<BR>
	 * Note that the difference between JPL DE406 and DE422 is of a few us, so precision can never be
	 * better than that.
	 * @param time The time.
	 * @param observer The observer at Earth (possibly) used to obtain the star position.
	 * @param eph The ephemeris properties. Only geocentric/topocentric flag and ephemeris
	 * reduction method are considered, to rest is set to J2000 equinox, ICRF frame, and astrometric
	 * coordinates. The algorithm is also taken into account if it corresponds to a JPL ephemeris
	 * version supported and available, otherwise DE406 is used (if available). As a last chance,
	 * Moshier is used.
	 * @param star The properties of the star.
	 * @return The light time to the star from the Solar System Barycenter, in days.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double getLightTimeFromSSB(TimeElement time, ObserverElement observer, EphemerisElement eph,
			StarElement star) throws JPARSECException {

		return StarEphem.getLightTimeFromSSBandEarth(time, observer, eph, star)[0];
	}
	/**
	   RESULTS CHECKED WITH AA EPHEMERIDES PROGRAM BY S. L. MOSHIER, EVERYTHING OK

	                                  per century
eq   RAh RAm RAs  DECg DECm DECs  RApm(s) DECpm('') VR_pm  Parallax  Mag  Nombre    ID
2000 00 08 23.265  29 05 25.58   1.039  -16.33 -12.0 0.0240   2.06 alAnd(Alpheratz)       4
1950 17 55 23.000   4 33 18.00  -5.0   1031.0 -107.8 0.548   9.54  Barnard

Terrestrial east longitude -71.1300 deg
geocentric latitude 42.0785 deg

               alpha Andromedae (Alpheratz)

JD 2446431.76,  1986 January 1 Wednesday  6h 09m 05.130s  UT
1986 January 1 Wednesday  6h 10m 00.000s  TDT
approx. visual magnitude 2.1
Astrometric J2000.0:  R.A.   0h 08m 23.118s  Dec.    29d 05' 27.86"
Astrometric B1950.0:  R.A.   0h 05m 48.257s  Dec.    28d 48' 46.14"
Astrometric of date:  R.A.   0h 07m 39.711s  Dec.    29d 00' 47.45"
elongation from sun 93.22 degrees, light defl. dRA 0.000s dDec 0.00"
annual aberration dRA -0.215s dDec 8.66"
nutation dRA -0.829s dDec -3.45"
    Apparent:  R.A.   0h 07m 38.668s  Dec.    29d 00' 52.66"
Local apparent sidereal time   8h 06m 58.673s
diurnal aberration dRA -0.009s dDec 0.10"
atmospheric refraction 0.444 deg  dRA 78.482s dDec 1224.21"
Topocentric:  Altitude 0.692 deg, Azimuth 310.658 deg
Topocentric: R.A.  0h 08m 57.141s   Dec.   29d 21' 16.96"
local meridian transit 1985 December 31 Tuesday 22h 11m 03.676s  UT
rises 1985 December 31 Tuesday 14h 07m 12.434s  UT
sets 1986 January 1 Wednesday  6h 14m 54.875s  UT
Visible hours 16.1285


              Barnard

JD 2446431.50,  1986 January 1 Wednesday  0h 00m 00.000s  TDT
Converting to FK5 system
approx. visual magnitude 9.5
Astrometric J2000.0:  R.A.  17h 57m 49.551s  Dec.     4d 39' 14.85"
Astrometric B1950.0:  R.A.  17h 55m 21.275s  Dec.     4d 39' 29.75"
Astrometric of date:  R.A.  17h 57m 08.029s  Dec.     4d 39' 17.93"
elongation from sun 30.03 degrees, light defl. dRA -0.000s dDec 0.01"
annual aberration dRA -1.368s dDec -1.80"
nutation dRA -0.546s dDec -6.91"

- NOTE MOSHIER DOES NOT CORRECT FOR DIFFERENTIAL LIGHT TIME => 0.05" in DEC FOR BARNARD

 Apparent:  R.A.  17h 57m 06.114s  Dec.     4d 39' 09.26"
Local apparent sidereal time   1h 56m 52.913s
diurnal aberration dRA -0.008s dDec 0.02"
atmospheric refraction 0.000 deg  dRA -0.000s dDec 0.00"
Topocentric:  Altitude -18.318 deg, Azimuth 294.529 deg
Topocentric: R.A. 17h 57m 06.106s   Dec.    4d 39' 09.28"
local meridian transit 1985 December 31 Tuesday 16h 01m 31.813s  UT
rises 1985 December 31 Tuesday  9h 42m 34.767s  UT
sets 1985 December 31 Tuesday 22h 20m 28.821s  UT
Visible hours 12.6317

	 */
}
