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
package jparsec.io;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.imageio.ImageIO;

import jparsec.astronomy.Constellation;
import jparsec.astronomy.Constellation.CONSTELLATION_NAME;
import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.EphemerisElement.REDUCTION_METHOD;
import jparsec.ephem.Functions;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.moons.MoonOrbitalElement;
import jparsec.ephem.planets.EphemElement;
import jparsec.ephem.planets.OrbitalElement;
import jparsec.ephem.planets.OrbitalElement.MAGNITUDE_MODEL;
import jparsec.ephem.stars.DoubleStarElement;
import jparsec.ephem.stars.StarElement;
import jparsec.ephem.stars.VariableStarElement;
import jparsec.math.Constant;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.AstroDate;
import jparsec.time.DateTimeOps;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.util.Configuration;
import jparsec.util.DataBase;
import jparsec.util.JPARSECException;
import jparsec.vo.GeneralQuery;
import jparsec.ephem.probes.Spacecraft;
import jparsec.ephem.probes.SatelliteOrbitalElement;
import jparsec.ephem.planets.OrbitEphem;
import jparsec.ephem.stars.StarEphem;
import jparsec.graph.DataSet;

/**
 * A suitable class for reading files in the hard disk or located inside
 * any of the .jar files of the dependencies, for instance orbital elements
 * of comets and asteroids. Available formats are SkyMap and Minor Planet Center.
 * When reading files with an instance of this class be aware that there's an
 * internal restriction about the number of times a given file can be read and
 * its data hold in different instances of this class. The maximum allowed times
 * is internally set to 3, which means, for instance, that 3 is the maximum allowed
 * number of different sky renderings that can be hold in memory per thread. This
 * is obviously done to save memory. When changing the time in sky rendering many times,
 * only the set of objects (orbital elements for comets and asteroids, list of stars,
 * ...) corrsponding to the 3 most recent renderings are in memory using the
 * {@linkplain DataBase} class. This value (3) is the value of {@linkplain Configuration#MAX_CACHE_SIZE},
 * and can be changed, but it is not recommended to increase it.
 * <BR>
 * NOTE: The availability of the SkyMap format is only for our practical
 * purposes, since it is a popular commercial program used by astronomers. This
 * does not mean any particular interest at all.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 * @see FileFormatElement
 * @see OrbitalElement
 * @see StarElement
 * @see SatelliteOrbitalElement
 * @see DataBase
 */
public class ReadFile implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Empty constructor. Note the path must be provided
	 * later, otherwise an error will be thrown when trying
	 * to read data.
	 */
	public ReadFile() {
		//setID();
	}

	/**
	 * Constructor for a given format and path.
	 * @param format Format to read.
	 * @param path Path of the .jar file to read. Constants defined
	 * in other classes depending of what is going to be read: for example,
	 * {@linkplain Spacecraft}, {@linkplain OrbitEphem}, {@linkplain StarEphem},
	 * and so on.
	 * @throws JPARSECException In case the path is null.
	 */
	public ReadFile(FORMAT format, String path) throws JPARSECException {
		pathToFile = path;
		setFormat(format);
		setID();
	}

	/**
	 * Constructor for a given format and path.
	 * @param path Path of the .jar file to read. Constants defined
	 * in other classes depending of what is going to be read: for example,
	 * {@linkplain Spacecraft}, {@linkplain OrbitEphem}, {@linkplain StarEphem},
	 * and so on.
	 * @param encoding The encoding. Constants defined in this class.
	 * @throws JPARSECException In case the path is null.
	 */
	public ReadFile(String path, String encoding) throws JPARSECException {
		pathToFile = path;
		setEncoding(encoding);
		setID();
	}
	private ReadFile(String path, String encoding, boolean needID) throws JPARSECException {
		pathToFile = path;
		if (pathToFile == null || pathToFile.equals("null")) throw new JPARSECException("Path cannot be null!");
		setEncoding(encoding);
		if (needID) setID();
	}

	/**
	 * Set some read constraints. This method will limit the possible objects
	 * read to those satisfying the name of the source and/or its location.
	 * Constraints are applied to all objects read except sun spots, and location
	 * constraint is only applied to stars (normal stars, doubles, and variables).
	 * @param name Source name or part of its name. Set to null to set no constrain.
	 * @param loc Source position. Set to null to set no constrain.
	 * @param radius Radius of the source around the previous position in radians.
	 */
	public void setReadConstraints(String name, LocationElement loc, double radius) {
		consName = name;
		consLoc = loc;
		consRadius = radius;
	}
	private String consName = null;
	private LocationElement consLoc = null;
	private double consRadius = 0;

	private static String ids[] = new String[0];
	private String id = null;
	private void setID() throws JPARSECException {
		if (pathToFile == null || pathToFile.equals("null")) throw new JPARSECException("Path cannot be null!");

		if (id != null) {
			DataBase.addData(this.getDataBaseID(), threadName, null, inMemory);
			try {
				int idindex = DataSet.getIndex(ids, id);
				if (idindex >= 0) ids = DataSet.eliminateRowFromTable(ids, 1+idindex);
			} catch (Exception e) { e.printStackTrace(); }
			// System.out.println("*** Re-eliminated "+id);
		}

		int index = 0;
		while(true) {
			String id = pathToFile+"_"+encoding+"_"+index;
			int i = DataSet.getIndex(ids, id);
			if (i < 0) break;
			index ++;
		};
		if (index >= Configuration.MAX_CACHE_SIZE) {
			index = 0;
			if (threadName == null) threadName = Thread.currentThread().getName();

			id = pathToFile+"_"+encoding+"_0";
			DataBase.addData(this.getDataBaseID(), threadName, null, inMemory);
			try {
				int idindex = DataSet.getIndex(ids, id);
				if (idindex >= 0) ids = DataSet.eliminateRowFromTable(ids, 1+idindex);
			} catch (Exception e) { e.printStackTrace(); }
			// System.out.println("*** Eliminated "+id);
		}

		if (index < Configuration.MAX_CACHE_SIZE-1) {
			id = pathToFile+"_"+encoding+"_"+(index+1);
			int idindex = DataSet.getIndex(ids, id);
			if (idindex >= 0) {
				DataBase.addData(this.getDataBaseID(), threadName, null, inMemory);
				try {
					ids = DataSet.eliminateRowFromTable(ids, 1+idindex);
				} catch (Exception e) { e.printStackTrace(); }
				// System.out.println("*** Eliminated next = "+id);
			}
		}

		id = pathToFile+"_"+encoding+"_"+index;
		ids = DataSet.addStringArray(ids, new String[] {id});
		// System.out.println("*** Loaded "+id);

/*
		do {
			double n = Math.random();
			id = ""+n;
			index = DataSet.getIndex(ids, id);
		} while (index >= 0);
		this.id = id;
		ids = DataSet.addStringArray(ids, new String[] {id});
*/
	}

	/**
	 * Obtain an {@linkplain OrbitalElement} object from a line of a file of orbital elements
	 * of asteroids.
	 *
	 * @param asteroid A full line of the file.
	 * @return {@linkplain OrbitalElement} object full of data.
	 * @throws JPARSECException If the reference time is invalid.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public OrbitalElement parseAsteroid(String asteroid) throws JPARSECException
	{
		OrbitalElement orbit = new OrbitalElement();

		ReadFormat rf = new ReadFormat();

		switch (formatOfFile)
		{
		case SKYMAP:

			try {
				rf.setFormatToRead(FileFormatElement.SKYMAP_ASTEROIDS_FORMAT);

				orbit.name = rf.readString(asteroid, "NAME");
				orbit.absoluteMagnitude = (float) rf.readDouble(asteroid, "ABSOLUTE_MAGNITUDE");
				orbit.meanAnomaly = rf.readDoubleToRadians(asteroid, "MEAN_ANOMALY");
				orbit.argumentOfPerihelion = rf.readDoubleToRadians(asteroid, "ARGUMENT_OF_PERIHELION");
				orbit.ascendingNodeLongitude = rf.readDoubleToRadians(asteroid, "ASCENDING_NODE_LONGITUDE");
				orbit.inclination = rf.readDoubleToRadians(asteroid, "INCLINATION");
				orbit.eccentricity = rf.readDouble(asteroid, "ECCENTRICITY");
				orbit.semimajorAxis = rf.readDouble(asteroid, "SEMIMAJOR_AXIS");
				orbit.magnitudeSlope = (float) rf.readDouble(asteroid, "MAGNITUDE_SLOPE");
				orbit.referenceEquinox = Constant.J2000;
				orbit.meanMotion = Constant.EARTH_MEAN_ORBIT_RATE / (Math.abs(orbit.semimajorAxis) * Math.sqrt(Math
						.abs(orbit.semimajorAxis)));
				orbit.magnitudeModel = MAGNITUDE_MODEL.ASTEROID_HG;

				// Read perihelion time
				AstroDate astro = new AstroDate(rf.readInteger(asteroid, "PERIHELION_YEAR"), rf.readInteger(
						asteroid, "PERIHELION_MONTH"), rf.readDouble(asteroid, "PERIHELION_DAY"));
				orbit.referenceTime = astro.jd();
			} catch (Exception exc)
			{
				JPARSECException.addWarning("Could not parse this asteroid in SkyMap format, returning null. Details: "+exc.getLocalizedMessage()+". Line to parse was: "+asteroid);
				return null;
			}
			break;

		case MPC:

			try {
				rf.setFormatToRead(FileFormatElement.MPC_ASTEROIDS_FORMAT);

				orbit.name = rf.readString(asteroid, "NAME");
				orbit.meanAnomaly = rf.readDoubleToRadians(asteroid, "MEAN_ANOMALY");
				orbit.argumentOfPerihelion = rf.readDoubleToRadians(asteroid, "ARGUMENT_OF_PERIHELION");
				orbit.ascendingNodeLongitude = rf.readDoubleToRadians(asteroid, "ASCENDING_NODE_LONGITUDE");
				orbit.inclination = rf.readDoubleToRadians(asteroid, "INCLINATION");
				orbit.eccentricity = rf.readDouble(asteroid, "ECCENTRICITY");
				orbit.semimajorAxis = rf.readDouble(asteroid, "SEMIMAJOR_AXIS");
				orbit.referenceEquinox = Constant.J2000;
				orbit.meanMotion = rf.readDoubleToRadians(asteroid, "MEAN_MOTION");
				orbit.absoluteMagnitude = (float) rf.readDouble(asteroid, "ABSOLUTE_MAGNITUDE");
				orbit.magnitudeSlope = (float) rf.readDouble(asteroid, "MAGNITUDE_SLOPE");
				orbit.magnitudeModel = MAGNITUDE_MODEL.ASTEROID_HG;

				// Read MPC packed date
				orbit.referenceTime = readMPCPackedDate(rf.readString(asteroid, "EPOCH"));
			} catch (Exception exc)
			{
				JPARSECException.addWarning("Could not parse this asteroid in MPC format, returning null. Details: "+exc.getLocalizedMessage()+". Line to parse was: "+asteroid);
				return null;
			}
			break;
		default:
			throw new JPARSECException("invalid format.");
		}

		return orbit;
	}

	/**
	 * Obtain an {@linkplain OrbitalElement} object from a line of a file of orbital elements
	 * of asteroids, in a format defined by the user.
	 * <BR>
	 * This should be used with caution, since the fields not present in the
	 * {@linkplain FileFormatElement} array object will be set to zero. Also note that the library uses
	 * argument of perihelion and mean anomaly as input, instead of longitude of
	 * perihelion and mean longitude.
	 *
	 * @param asteroid A full line of the file.
	 * @param fmt Format of file.
	 * @return {@linkplain OrbitalElement} object full of data.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public static OrbitalElement parseCustomAsteroidFile(String asteroid, FileFormatElement[] fmt) throws JPARSECException
	{
		OrbitalElement orbit = new OrbitalElement();

		ReadFormat rf = new ReadFormat();
		rf.setFormatToRead(fmt);

		orbit.name = rf.readString(asteroid, "NAME");
		orbit.absoluteMagnitude = (float) rf.readDouble(asteroid, "ABSOLUTE_MAGNITUDE");
		orbit.meanAnomaly = rf.readDoubleToRadians(asteroid, "MEAN_ANOMALY");
		orbit.argumentOfPerihelion = rf.readDoubleToRadians(asteroid, "ARGUMENT_OF_PERIHELION");
		orbit.ascendingNodeLongitude = rf.readDoubleToRadians(asteroid, "ASCENDING_NODE_LONGITUDE");
		orbit.inclination = rf.readDoubleToRadians(asteroid, "INCLINATION");
		orbit.meanMotion = rf.readDoubleToRadians(asteroid, "MEAN_MOTION");
		orbit.eccentricity = rf.readDouble(asteroid, "ECCENTRICITY");
		orbit.semimajorAxis = rf.readDouble(asteroid, "SEMIMAJOR_AXIS");
		orbit.perihelionDistance = rf.readDouble(asteroid, "PERIHELION_DISTANCE");
		orbit.perihelionLongitude = rf.readDoubleToRadians(asteroid, "PERIHELION_LONGITUDE");
		orbit.meanLongitude = rf.readDoubleToRadians(asteroid, "MEAN_LONGITUDE");
		orbit.magnitudeSlope = (float) rf.readDouble(asteroid, "MAGNITUDE_SLOPE");
		orbit.referenceEquinox = rf.readDouble(asteroid, "REFERENCE_EQUINOX");
		orbit.referenceTime = rf.readDouble(asteroid, "REFERENCE_TIME");
		orbit.magnitudeModel = MAGNITUDE_MODEL.ASTEROID_HG;

		return orbit;
	}

	/**
	 * Obtains orbital elements for certain probe.
	 *
	 * @param data The probe data as given by the JPARSEC database.
	 * @return The {@linkplain OrbitalElement} object.
	 * @throws JPARSECException If the reference or applicable times are
	 *         invalid.
	 */
	public static OrbitalElement parseProbe(String data) throws JPARSECException
	{
		OrbitalElement orbit = new OrbitalElement();
		ReadFormat rf = new ReadFormat();

		try
		{
			rf.setFormatToRead(FileFormatElement.JPARSEC_PROBES_FORMAT);

			orbit.name = rf.readString(data, "NAME");
			orbit.inclination = rf.readDoubleToRadians(data, "INCLINATION");
			orbit.ascendingNodeLongitude = rf.readDoubleToRadians(data, "ASCENDING_NODE_LONGITUDE");
			orbit.argumentOfPerihelion = rf.readDoubleToRadians(data, "ARGUMENT_OF_PERIHELION");
			orbit.semimajorAxis = rf.readDouble(data, "SEMIMAJOR_AXIS");
			orbit.eccentricity = rf.readDouble(data, "ECCENTRICITY");
			orbit.meanAnomaly = rf.readDoubleToRadians(data, "MEAN_ANOMALY");
			double year = rf.readDouble(data, "REFERENCE_EQUINOX");
			orbit.referenceEquinox = Constant.B1950 + (1950.0 - year) * Constant.TROPICAL_YEAR;
			if (year == 2000.0)
				orbit.referenceEquinox = Constant.J2000;
			orbit.meanMotion = Constant.EARTH_MEAN_ORBIT_RATE / (Math.abs(orbit.semimajorAxis) * Math.sqrt(Math
					.abs(orbit.semimajorAxis)));

			// Read reference time
			AstroDate astro = new AstroDate(rf.readInteger(data, "REF_YEAR"), rf.readInteger(data,
					"REF_MONTH"), rf.readDouble(data, "REF_DAY"));
			orbit.referenceTime = astro.jd();

			// Read initial time
			astro = new AstroDate(rf.readInteger(data, "BEGIN_YEAR"),
					rf.readInteger(data, "BEGIN_MONTH"), rf.readDouble(data, "BEGIN_DAY"));
			orbit.beginOfApplicableTime = astro.jd();

			// Read final time
			astro = new AstroDate(rf.readInteger(data, "END_YEAR"), rf.readInteger(data, "END_MONTH"),
					rf.readDouble(data, "END_DAY"));
			orbit.endOfApplicableTime = astro.jd();

			orbit.centralBody = TARGET.SUN;
			orbit.magnitudeModel = MAGNITUDE_MODEL.NONE;
		} catch (NullPointerException e1)
		{
			throw new JPARSECException("invalid format.", e1);
		} catch (NumberFormatException e2)
		{
			throw new JPARSECException("invalid format.", e2);
		} catch (Exception exc) {
			throw new JPARSECException(exc.getCause());
		}

		return orbit;
	}

	/**
	 * Reads a star in the original BSC5 format
	 *
	 * @param line A full line of the file.
	 * @return {@linkplain StarElement} object full of data.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public static StarElement parseBSC5file(String line) throws JPARSECException
	{
		StarElement star = new StarElement();

		ReadFormat rf = new ReadFormat();
		rf.setFormatToRead(FileFormatElement.BSC5_STARS_FORMAT);

		star.name = rf.readString(line, "NAME")+", HD"+rf.readString(line, "HD");
		star.rightAscension = rf.readDouble(line, "RA_HOUR_J2000");
		star.rightAscension += rf.readDouble(line, "RA_MIN_J2000") / Constant.MINUTES_PER_HOUR;
		star.rightAscension += rf.readDouble(line, "RA_SEC_J2000") / Constant.SECONDS_PER_HOUR;
		star.rightAscension = star.rightAscension / Constant.RAD_TO_HOUR;
		double dec_deg = rf.readDouble(line, "DEC_DEG_J2000");
		star.declination = Math.abs(dec_deg);
		star.declination += rf.readDouble(line, "DEC_MIN_J2000") / Constant.MINUTES_PER_DEGREE;
		star.declination += rf.readDouble(line, "DEC_SEG_J2000") / Constant.SECONDS_PER_DEGREE;
		star.declination = star.declination * Constant.DEG_TO_RAD;
		if (dec_deg < 0.0)
			star.declination = -star.declination;
		star.magnitude = (float) rf.readDouble(line, "MAG");
		star.properMotionRA = (float) (rf.readDouble(line, "RA_PM") * Constant.ARCSEC_TO_RAD / Math
				.cos(star.declination));
		star.properMotionDEC = (float) (rf.readDouble(line, "DEC_PM") * Constant.ARCSEC_TO_RAD);
		star.properMotionRadialV = (float) rf.readDouble(line, "RADIAL_VELOCITY");
		double parallax = 0;
		String pa = rf.readString(line, "PARALLAX");
		if (!pa.equals("")) parallax = DataSet.parseDouble(pa);
		star.parallax = parallax * 1000.0;
		star.equinox = Constant.J2000;
		star.frame = EphemerisElement.FRAME.ICRF;
		star.spectrum = rf.readString(line, "SPECTRUM");
		star.type = "N";
		int var = 0, doub = 0;
		if (!rf.readString(line, "ADS").equals(""))
			doub = 1;
		if (rf.readString(line, "VAR").startsWith("V"))
			var = 1;
		if (var == 1)
			star.type = "V";
		if (doub == 1)
		{
			star.type = "D";
			if (var == 1)
				star.type = "B";
		}

		return star;
	}

	/**
	 * Reads star information in the JPARSEC format, derived from Sky 2000
	 * Master Catalog, version 5. See
	 * https://wakata.nascom.nasa.gov/dist/generalProducts/attitude/ATT_SKYMAP.html.
	 *
	 * @param line A full line of the file.
	 * @return {@linkplain StarElement} object full of data.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public static StarElement parseJPARSECfile(String line) throws JPARSECException
	{
		// FIXME: THIS METHOD IS CLONED IN RENDERSKY CLASS TO IMPROVE PERFORMANCE. ANY CHANGE REQUIRED
		// HERE MUST BE IMPLEMENTED ALSO THERE
		StarElement star = new StarElement();

		ReadFormat rf = new ReadFormat();
		rf.setFormatToRead(FileFormatElement.JPARSEC_SKY2000_FORMAT);

		try {
			star.name = rf.readString(line, "NAME");
			star.spectrum = rf.readString(line, "SPECTRUM");
			star.type = rf.readString(line, "TYPE")+";"+rf.readString(line, "DATA");
			star.rightAscension = rf.readDouble(line, "RA");
			star.rightAscension = star.rightAscension / Constant.RAD_TO_HOUR;
			star.declination = rf.readDouble(line, "DEC");
			star.declination = star.declination * Constant.DEG_TO_RAD;
			star.magnitude = (float) rf.readDouble(line, "MAG");
			star.properMotionRadialV = 0.0f;
			star.properMotionRA = (float) (rf.readDouble(line, "RA_PM") * 15.0 * Constant.ARCSEC_TO_RAD);
			star.properMotionDEC = (float) (rf.readDouble(line, "DEC_PM") * Constant.ARCSEC_TO_RAD);
			if (!rf.readString(line, "RADIAL_VELOCITY").equals(""))
				star.properMotionRadialV = (float) rf.readDouble(line, "RADIAL_VELOCITY");
			double parallax = rf.readDouble(line, "PARALLAX");
			star.parallax = parallax;
			star.equinox = Constant.J2000;
			star.frame = EphemerisElement.FRAME.ICRF;

			// Add classical name
			String greek = "AlpBetGamDelEpsZetEtaTheIotKapLamMu Nu Xi OmiPi RhoSigTauUpsPhiChiPsiOme";
			String id = rf.readString(line, "ID");
			String constel = "";
			if (!id.equals(""))
			{
				int index = id.indexOf("-");
				String idd = "";
				if (index >= 0) {
					idd = id.substring(index + 1);
					index = Integer.parseInt(id.substring(0, index));
				} else {
					index = Integer.parseInt(id);
				}
				String code = greek.substring((index - 1) * 3, index * 3).trim()+idd;
				String constell = "";
				try
				{
					EphemerisElement eph = new EphemerisElement();
					eph.ephemMethod = REDUCTION_METHOD.IAU_2006;
					constell = Constellation.getConstellation(Constellation.getConstellationName(star.rightAscension, star.declination, Constant.J2000,
							eph), CONSTELLATION_NAME.ABREVIATED);
				} catch (JPARSECException ve)
				{
					constell = "";
				}
				constel = code;
				if (!constell.equals(""))
					constel += " " + constell.substring(0, 3);
				if (!constel.equals(""))
					star.name += " (" + constel + ") (" + id + ")";
			}
		} catch (Exception exc) {
			JPARSECException.addWarning("Could not parse this star in JPARSEC format, returning null. Details: "+exc.getLocalizedMessage()+". Line to parse was: "+line);
			return null;
		}
		return star;
	}

	/**
	 * Reads a star in the original FK6 format
	 *
	 * @param line A full line of the file.
	 * @return {@linkplain StarElement} object full of data.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public static StarElement parseFK6file(String line) throws JPARSECException
	{
		StarElement star = new StarElement();

		ReadFormat rf = new ReadFormat();
		rf.setFormatToRead(FileFormatElement.FK6_STARS_FORMAT);

		star.name = "(" + rf.readString(line, "FK6") + ")" + rf.readString(line, "NAME");
		star.rightAscension = rf.readDouble(line, "RA_HOUR_J2000");
		star.rightAscension += rf.readDouble(line, "RA_MIN_J2000") / Constant.MINUTES_PER_HOUR;
		star.rightAscension += rf.readDouble(line, "RA_SEC_J2000") / Constant.SECONDS_PER_HOUR;
		star.rightAscension = star.rightAscension / Constant.RAD_TO_HOUR;
		double dec_deg = rf.readDouble(line, "DEC_DEG_J2000");
		star.declination = Math.abs(dec_deg);
		star.declination += rf.readDouble(line, "DEC_MIN_J2000") / Constant.MINUTES_PER_DEGREE;
		star.declination += rf.readDouble(line, "DEC_SEG_J2000") / Constant.SECONDS_PER_DEGREE;
		star.declination = star.declination * Constant.DEG_TO_RAD;
		if (dec_deg < 0.0)
			star.declination = -star.declination;
		star.magnitude = (float) rf.readDouble(line, "MAG");
		star.properMotionRA = (float) (rf.readDouble(line, "RA_PM") * 0.001 * Constant.ARCSEC_TO_RAD / Math
				.cos(star.declination));
		star.properMotionDEC = (float) (rf.readDouble(line, "DEC_PM") * 0.001 * Constant.ARCSEC_TO_RAD);
		star.properMotionRadialV = (float) rf.readDouble(line, "RADIAL_VELOCITY");
		double parallax = rf.readDouble(line, "PARALLAX");
		star.parallax = parallax;
		star.equinox = Constant.J2000;
		star.frame = EphemerisElement.FRAME.ICRF;

		return star;
	}

	/**
	 * Obtain an {@linkplain OrbitalElement} object from a line of a file of orbital elements
	 * of comets.
	 *
	 * @param comet A full line of the file.
	 * @return {@linkplain OrbitalElement} object full of data.
	 * @throws JPARSECException If the reference time is invalid.
	 */
	public OrbitalElement parseComet(String comet) throws JPARSECException
	{
		ReadFormat rf = new ReadFormat();
		switch (formatOfFile)
		{
		case SKYMAP:

			// Set file format
			rf.setFormatToRead(FileFormatElement.SKYMAP_COMETS_FORMAT);

			break;

		case MPC:

			// Set file format
			rf.setFormatToRead(FileFormatElement.MPC_COMETS_FORMAT);

			break;
		default:
			throw new JPARSECException("invalid format.");
		}

		OrbitalElement orbit = new OrbitalElement();

		try {
			// Read record
			orbit.name = rf.readString(comet, "NAME");
			orbit.absoluteMagnitude = (float) rf.readDouble(comet, "ABSOLUTE_MAGNITUDE");
			orbit.meanAnomaly = 0.0;
			orbit.argumentOfPerihelion = rf.readDoubleToRadians(comet, "ARGUMENT_OF_PERIHELION");
			orbit.ascendingNodeLongitude = rf.readDoubleToRadians(comet, "ASCENDING_NODE_LONGITUDE");
			orbit.inclination = rf.readDoubleToRadians(comet, "INCLINATION");
			orbit.eccentricity = rf.readDouble(comet, "ECCENTRICITY");
			orbit.perihelionDistance = rf.readDouble(comet, "PERIHELION_DISTANCE");
			orbit.magnitudeSlope = (float) rf.readDouble(comet, "MAGNITUDE_SLOPE");
			orbit.referenceEquinox = Constant.J2000;
			orbit.magnitudeModel = MAGNITUDE_MODEL.COMET_gk;

			// Read perihelion time
			AstroDate astro = new AstroDate(rf.readInteger(comet, "PERIHELION_YEAR"), rf.readInteger(comet,
					"PERIHELION_MONTH"), rf.readDouble(comet, "PERIHELION_DAY"));
			orbit.referenceTime = astro.jd();

			// Set data for elliptic motion
			if (orbit.eccentricity != 1.0)
			{
				orbit.semimajorAxis = orbit.perihelionDistance / (1.0 - orbit.eccentricity);
				orbit.meanMotion = Constant.EARTH_MEAN_ORBIT_RATE / (Math.abs(orbit.semimajorAxis) * Math.sqrt(Math
						.abs(orbit.semimajorAxis)));
			}
		} catch (Exception exc) {
			// When this happens for a NEO it is then read as asteroid. Warning not necessary.
			if (!pathToFile.equals(OrbitEphem.PATH_TO_MPC_NEOs_FILE))
				JPARSECException.addWarning("Could not parse this comet, returning null. Details: "+exc.getLocalizedMessage()+". Line to parse was: "+comet);
			return null;
		}
		return orbit;
	}

	/**
	 * Obtain an {@linkplain DoubleStarElement} object from a line of a file of orbital elements
	 * of double stars (Hartkopf 2010).
	 *
	 * @param dstar A full line of the file.
	 * @return {@linkplain DoubleStarElement} object full of data.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public static DoubleStarElement parseDoubleStar(String dstar) throws JPARSECException
	{
		DoubleStarElement orbit = new DoubleStarElement();

		ReadFormat rf = new ReadFormat();
		rf.setFormatToRead(FileFormatElement.DOUBLE_STARS_HARTKPF_2010);

		try {
			orbit.name = rf.readString(dstar, "NAME");
			orbit.wds = rf.readString(dstar, "WDS");
			orbit.ads = rf.readString(dstar, "ADS");
			orbit.hd = rf.readString(dstar, "HD");
			orbit.hipparcos = rf.readString(dstar, "HIP");
			orbit.magPrimary = rf.readString(dstar, "MAGP");
			orbit.magSecondary = rf.readString(dstar, "MAGS");
			orbit.orbitGrade = Integer.parseInt(rf.readString(dstar, "GRADE"));
			orbit.notes = rf.readString(dstar, "NOTES");
			if (orbit.notes.trim().equals("n")) orbit.notes = "http://ad.usno.navy.mil/wds/orb6/orb6notes.html#"+orbit.wds.trim();
			orbit.reference = rf.readString(dstar, "REF");
			if (!orbit.reference.trim().equals("")) orbit.reference = "http://ad.usno.navy.mil/wds/orb6/wdsref.html#"+orbit.reference;
			orbit.orbitPNG = rf.readString(dstar, "PNG");
			if (!orbit.orbitPNG.trim().equals("")) orbit.orbitPNG = "http://ad.usno.navy.mil/wds/orb6/PNG/"+orbit.orbitPNG;

			String ra = rf.readString(dstar, "RA");
			String dec = rf.readString(dstar, "DEC");
			int rah = Integer.parseInt(ra.substring(0, 2));
			int ram = Integer.parseInt(ra.substring(2, 4));
			int deg = Integer.parseInt(dec.substring(1, 3));
			int dem = Integer.parseInt(dec.substring(3, 5));
			double ras = Float.parseFloat(ra.substring(4));
			double decs = Float.parseFloat(dec.substring(5));
			double rap = (rah + (ram / 60.0 + ras / 3600.0)) / Constant.RAD_TO_HOUR;
			double decp = (deg + (dem / 60.0 + decs / 3600.0)) * Constant.DEG_TO_RAD;
			if (dec.startsWith("-")) decp = -decp;

			orbit.rightAscension = rap;
			orbit.declination = decp;

			String aunit = rf.readString(dstar, "A_UNIT");
			double sma = Float.parseFloat(rf.readString(dstar, "A"));
			if (aunit.equals("m")) sma = sma / 1000.0;

			double ecc = Float.parseFloat(rf.readString(dstar, "EXC"));
			double perih_lon = Float.parseFloat(rf.readString(dstar, "LP")) * Constant.DEG_TO_RAD;
			double asc_node_lon = Float.parseFloat(rf.readString(dstar, "NODE")) * Constant.DEG_TO_RAD;
			double incl = Float.parseFloat(rf.readString(dstar, "INCL")) * Constant.DEG_TO_RAD;
			double ref_time = Float.parseFloat(rf.readString(dstar, "TP"));
			String tunit = rf.readString(dstar, "TP_UNIT");
			if (tunit.equals("d")) {
				ref_time += 2400000.0;
			} else {
				ref_time = (ref_time - 2000.0) * 365.242189 + 2451544.53;
			}
			String punit = rf.readString(dstar, "P_UNIT");
			double period = Float.parseFloat(rf.readString(dstar, "P"));
			if (punit.equals("y")) period *= 365.25;
			if (punit.equals("c")) period *= Constant.JULIAN_DAYS_PER_CENTURY;
			double motion = Constant.TWO_PI / period;
			double anomaly = 0.0;
			String nom = orbit.name;
			double arg_perih = perih_lon;
			double equinox = Constant.J2000;
			String eq = rf.readString(dstar, "EQ");
			if (!eq.equals("") && !eq.equals("2000")) {
				equinox = Constant.J2000 + (Integer.parseInt(eq) - 2000) * 365.25;
			}
			double init_time = 0, final_time = Constant.J2000 + 10000 * 365.25;
			orbit.orbit = new OrbitalElement(nom, sma, arg_perih, ecc, anomaly, asc_node_lon, incl, ref_time, motion, equinox, init_time, final_time);

		} catch (Exception exc) {
			JPARSECException.addWarning("Could not parse this double star, returning null. Details: "+exc.getLocalizedMessage()+". Line to parse was: \n"+dstar);
			return null;
		}
		return orbit;
	}

	/**
	 * Obtain an {@linkplain DoubleStarElement} object from a line of a file of orbital elements
	 * of double stars (Worley 1983).
	 *
	 * @param dstar A full line of the file.
	 * @return {@linkplain DoubleStarElement} object full of data.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public static DoubleStarElement parseOldDoubleStar(String dstar) throws JPARSECException
	{
		DoubleStarElement orbit = new DoubleStarElement();

		ReadFormat rf = new ReadFormat();
		rf.setFormatToRead(FileFormatElement.DOUBLE_STARS_WORLEY_1983);

		try {
			orbit.name = rf.readString(dstar, "NAME");
			orbit.ads = rf.readString(dstar, "ADS");
			orbit.magPrimary = rf.readString(dstar, "MAGP");
			orbit.magSecondary = rf.readString(dstar, "MAGS");
			String grade = rf.readString(dstar, "GRADE");
			if (grade.equals("")) grade = "0";
			orbit.orbitGrade = Integer.parseInt(grade);
			orbit.notes = rf.readString(dstar, "NOTES");
			orbit.reference = rf.readString(dstar, "REF");

			String srah = rf.readString(dstar, "RAH");
			String sdecd = rf.readString(dstar, "DECD");
			String sram = rf.readString(dstar, "RAM");
			String sdecm = rf.readString(dstar, "DECM");
			String sdecs = rf.readString(dstar, "DECS");
			int rah = Integer.parseInt(srah);
			double ram = DataSet.parseDouble(sram);
			int deg = Integer.parseInt(sdecd);
			double dem = DataSet.parseDouble(sdecm);
			double rap = (rah + (ram / 60.0)) / Constant.RAD_TO_HOUR;
			double decp = (deg + (dem / 60.0)) * Constant.DEG_TO_RAD;
			if (sdecs.startsWith("-")) decp = -decp;

			orbit.rightAscension = rap;
			orbit.declination = decp;

			double sma = DataSet.parseDouble(rf.readString(dstar, "A"));
			double ecc = DataSet.parseDouble(rf.readString(dstar, "EXC"));
			double perih_lon = DataSet.parseDouble(rf.readString(dstar, "LP")) * Constant.DEG_TO_RAD;
			double asc_node_lon = DataSet.parseDouble(rf.readString(dstar, "NODE")) * Constant.DEG_TO_RAD;
			double incl = DataSet.parseDouble(rf.readString(dstar, "INCL")) * Constant.DEG_TO_RAD;
			double ref_time = DataSet.parseDouble(rf.readString(dstar, "TP"));
			ref_time = (ref_time - 2000.0) * 365.242189 + 2451544.53;
			double period = DataSet.parseDouble(rf.readString(dstar, "PERIOD")) * 365.25;
			double motion = Constant.TWO_PI / period;
			double anomaly = 0.0;
			String nom = orbit.name;
			double arg_perih = perih_lon; // - asc_node_lon;
			double equinox = Constant.J2000;
			String eq = rf.readString(dstar, "EQ");
			if (!eq.equals("") && !eq.equals("2000")) {
				equinox = Constant.J2000 + (Integer.parseInt(eq) - 2000) * 365.25;
			}
			double init_time = 0, final_time = Constant.J2000 + 10000 * 365.25;
			orbit.orbit = new OrbitalElement(nom, sma, arg_perih, ecc, anomaly, asc_node_lon, incl, ref_time, motion, equinox, init_time, final_time);

		} catch (Exception exc) {
			JPARSECException.addWarning("Could not parse this double star, returning null. Details: "+exc.getLocalizedMessage()+". Line to parse was: \n"+dstar);
			return null;
		}
		return orbit;
	}

	/**
	 * Obtain a {@linkplain VariableStarElement} object from a line of one of the
	 * two catalogs supported.
	 *
	 * @param line A full line of the file.
	 * @param line2 The next line to complete the record for the AAVSO bulletin, or null
	 * for the catalog by Kreiner.
	 * @param year The year in case of the AAVSO catalog.
	 * @return {@linkplain VariableStarElement} object full of data, or null if cannot
	 * be parsed.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public static VariableStarElement parseVariableStar(String line, String line2, int year) throws JPARSECException
	{
		boolean aavso = true;
		if (line2 != null) aavso = false;

		VariableStarElement var = new VariableStarElement();

		try {
			if (aavso) {
				String values[] = DataSet.toStringArray(line, ",");
				var.name = values[0];
				int rah = Integer.parseInt(values[1]);
				int ram = Integer.parseInt(values[2]);
				double ras = DataSet.parseDouble(values[3]);
				String decg = values[4];
				int decm = Integer.parseInt(values[5]);
				double decs = DataSet.parseDouble(values[6]);
				var.rightAscension = Functions.parseRightAscension(rah, ram, ras);
				var.declination = Functions.parseDeclination(decg, decm, decs);
				var.period = DataSet.parseDouble(values[7]);
				String mag = values[8];
				if (mag.startsWith("<")) mag = mag.substring(1);
				if (mag.indexOf(">") > 0) mag = mag.substring(0, mag.indexOf(">"));
				var.magRange = mag;
				var.maximaDates = "";
				var.minimaDates = "";
				var.isEclipsing = false;
				for (int i=10; i<values.length; i++) {
					String v = values[i].toLowerCase();
					if (!v.equals("") && !v.equals("fading") && !v.equals("rising")) {
						int day = Integer.parseInt(v.substring(v.indexOf("(") + 1, v.indexOf(")")));
						int month = i - 9;
						AstroDate astro = new AstroDate(year, month, day);
						double jd = astro.jd();
						if (v.startsWith("max")) {
							if (!var.maximaDates.equals("")) var.maximaDates += ",";
							var.maximaDates += ""+jd;
						} else {
							if (!var.minimaDates.equals("")) var.minimaDates += ",";
							var.minimaDates += ""+jd;
						}
					}
				}
			} else {
/*				FileFormatElement fmt_Kreiner_1[] = new FileFormatElement[] {
						new FileFormatElement(1, 4, "NAME"), new FileFormatElement(5, 8, "CONSTEL"),
						new FileFormatElement(14, 18, "MAG1"), new FileFormatElement(27, 31, "MAG2"),
						new FileFormatElement(34, 41, "SPECTRAL_TYPE"), new FileFormatElement(43, 48, "ECL_TYPE"),
						new FileFormatElement(50, 57, "TYPE")
				};
				FileFormatElement fmt_Kreiner_2[] = new FileFormatElement[] {
						new FileFormatElement(1, 8, "RA"), new FileFormatElement(11, 18, "DEC"),
						new FileFormatElement(28, 31, "DURATION"), new FileFormatElement(39, 47, "PERIOD"),
						new FileFormatElement(49, 60, "T0")
				};

				ReadFormat rf1 = new ReadFormat();
				rf1.setFormatToRead(fmt_Kreiner_1);
				ReadFormat rf2 = new ReadFormat();
				rf2.setFormatToRead(fmt_Kreiner_2);

				var.name = rf1.readString(line, "NAME").trim() + " " + rf1.readString(line, "CONSTEL").trim();
				var.magRange = rf1.readString(line, "MAG1").trim() + "-" + rf1.readString(line, "MAG2").trim();
				var.spectralType = rf1.readString(line, "SPECTRAL_TYPE").trim();
				var.eclipsingType = rf1.readString(line, "ECL_TYPE").trim();
				var.type = rf1.readString(line, "TYPE").trim();

				// Only main minima supported, to also avoid repeated objects
				if (var.type.toLowerCase().indexOf("sec") >= 0) return null;

				var.period = DataSet.parseDouble(rf2.readString(line2, "PERIOD").trim());
				var.minimaTime = DataSet.parseDouble(rf2.readString(line2, "T0").trim());
				var.rightAscension = Functions.parseRightAscension(rf2.readString(line2, "RA").trim());
				var.declination = Functions.parseDeclination(rf2.readString(line2, "DEC").trim());
				var.minimaDuration = DataSet.parseDouble(rf2.readString(line2, "DURATION").trim());
				var.isEclipsing = true;
*/
				// Crazy format Kreiner ...
				int index = line.lastIndexOf("for");
				if (index < 1) return null;
				int index2 = line.lastIndexOf("sec");
				if (index2 > index) return null;
				var.name = FileIO.getField(1, line, " ", true) + " " + FileIO.getField(2, line, " ", true);
				var.magRange = FileIO.getField(4, line, " ", true) + "-" + "/";
				String l0 = line.substring(0, index).trim();
				var.eclipsingType = FileIO.getLastField(l0, " ", true);
				var.type = FileIO.getLastField(line, " ", true);
				var.spectralType = FileIO.getField(FileIO.getNumberOfFields(l0, " ", true) - 1, l0, " ", true).trim();
				if (var.spectralType.equals("|")) var.spectralType = "";
				if (var.spectralType.startsWith("|")) var.spectralType = var.spectralType.substring(1);

				var.rightAscension = Functions.parseRightAscension(line2.substring(0, 8));
				var.declination = Functions.parseDeclination(line2.substring(10, 18));
				var.minimaDuration = DataSet.parseDouble(FileIO.getField(8, line2, " ", true));
				var.period = DataSet.parseDouble(FileIO.getField(10, line2, " ", true));
				var.minimaTime = DataSet.parseDouble(FileIO.getField(11, line2, " ", true));
				var.isEclipsing = true;
			}
		} catch (Exception exc) {
			JPARSECException.addWarning("Could not parse this variable star, returning null. Details: "+exc.getLocalizedMessage()+". Line to parse was: \n"+line+"\n"+line2);
			return null;
		}
		return var;
	}
	
	/**
	 * Sets the path to the file of objects elements. Full path required in case
	 * of external files.
	 *
	 * @param path Full path including extension.
	 * @throws JPARSECException In case the path is null.
	 */
	public void setPath(String path) throws JPARSECException
	{
		pathToFile = path;
		setID();
	}

	/**
	 * Sets the encoding, for example UTF-8 or ISO-8859-1. Default is
	 * ISO-8859-1.
	 * @param charset The charset.
	 */
	public void setEncoding(String charset)
	{
		encoding = charset;
	}
	/**
	 * Returns the current encoding of the file to be read.
	 * @return The encoding.
	 */
	public String getEncoding()
	{
		return encoding;
	}

	/**
	 * Sets the format of the file of objects elements.
	 *
	 * @param format ID value for the format.
	 */
	public void setFormat(FORMAT format)
	{
		formatOfFile = format;
	}

	/**
	 * The different format supported when reading stars, orbital
	 * elements and other files.
	 */
	public enum FORMAT {
		/** ID constant for reading the file as an Sky Map (comets, asteroids) input file. */
		SKYMAP,
		/** ID constant for reading the file as a MPC (comets, asteroids) formatted file. */
		MPC,
		/** ID constant for reading the file as a JPARSEC Sky 2000 stars input file. */
		JPARSEC_SKY2000,
		/** ID constant for reading the file as an FK6 stars input file. */
		FK6,
		/** ID constant for reading the file as a BSC5 stars input file. */
		BSC5
	};

	/**
	 * Constant for ISO-8859-1 encoding.
	 */
	public static final String ENCODING_ISO_8859 = "ISO-8859-1";
	/**
	 * Constant for UTF-8 encoding.
	 */
	public static final String ENCODING_UTF_8 = "UTF-8";
	/**
	 * Constant for IBM 850 encoding.
	 */
	public static final String ENCODING_IBM850 = "IBM850";

	/**
	 * Path to the file of objects, including extension.
	 */
	public String pathToFile;

	/**
	 * The encoding of the file to be read. ISO-8859-1 set by default.
	 */
	public String encoding = ENCODING_ISO_8859;

	/**
	 * Format of the file of objects.
	 */
	public FORMAT formatOfFile;

	private boolean inMemory = true;
	private String threadName = null;

	/**
	 * Overwrites the name of the thread identifier for the
	 * elements read in this object and stored in Database.
	 * Don't use this if you don't know what you are doing.
	 * @param threadID The new thread identifier.
	 */
	public void setThreadName(String threadID) {
		threadName = threadID;
	}

	/**
	 * Sets the array of objects read by this instance to
	 * a given list of objects.
	 * @param a The list of objects.
	 */
	public void setReadElements(ArrayList a) {
		if (threadName == null) threadName = Thread.currentThread().getName();

		if (a == null) {
			DataBase.addData(getDataBaseID(), threadName, null, inMemory);
			return;
		}

		DataBase.addData(getDataBaseID(), threadName, DataSet.toObjectArray(a), inMemory);
	}

	/**
	 * Sets the array of objects read by this instance to
	 * a given array of objects.
	 * @param o The array of objects.
	 */
	public void setReadElementsFromArray(Object o[]) {
		if (threadName == null) threadName = Thread.currentThread().getName();

		if (o == null) {
			DataBase.addData(getDataBaseID(), threadName, null, inMemory);
			return;
		}

		DataBase.addData(getDataBaseID(), threadName, o, inMemory);
	}

	/**
	 * Sets the array of objects read by this instance to
	 * a given list of objects.
	 * @param a The list of objects.
	 * @throws JPARSECException In case the class of the objects
	 * in the input array and the class of the ones currently read
	 * differ.
	 */
	public void addReadElements(ArrayList a) throws JPARSECException {
		DataBase.addData(getDataBaseID(), threadName, DataSet.addObjectArray(getReadElements(), DataSet.toObjectArray(a)), inMemory);
	}

	/**
	 * Returns the array of objects read by this instance.
	 * @return The list of objects.
	 */
	public Object[] getReadElements() {
		if (threadName == null) threadName = Thread.currentThread().getName();
		Object o = DataBase.getData(getDataBaseID(), threadName, inMemory);
		if (o == null) return null;
		return (Object[]) o;
	}

	/**
	 * Returns the String identifier for the elements stored
	 * in this instance using {@linkplain DataBase}.
	 * @return The identifier.
	 */
	public String getDataBaseID() {
		return "readElements_"+id;
	}

	/**
	 * Reads an example file with orbital elements of asteroids formatted in the
	 * standard way, established by the Minor Planet Center or either the
	 * commercial program SkyMap. The format is the same for trans-Neptunian or
	 * Centaur objects.
	 * <BR>
	 * An example of MPC format is:
	 * <BR>
	 *
	 * <pre>
	 * 00001    3.34  0.12 K0636 129.98342   73.23162   80.40970   10.58687  0.0800102  0.21432279   2.7653949    MPC 24219  4676  62 1839-1994 0.54 M-v 30  Bowell     0000             (1) Ceres
	 * </pre>
	 *
	 * <BR>
	 * An example of SkyMap format is:
	 * <BR>
	 *
	 * <pre>
	 *       1 Ceres                                  2005 08 18.0  86.9545  2.765979 0.080014  73.3924  80.4097  10.5860   3.34  0.12
	 * </pre>
	 *
	 * <BR>
	 * After the file is successfully read, the objects are stored using
	 * {@linkplain DataBase} with the identifier returned by
	 * {@linkplain #getDataBaseID()}.
	 *
	 * @throws JPARSECException If the reference time is invalid.
	 */
	public void readFileOfAsteroids() throws JPARSECException
	{
		// Define necesary variables
		ArrayList<Object> vec = new ArrayList<Object>();
		String file_line = "";

		// Connect to the file
		InputStream is = null;
		BufferedReader dis = null;
		try
		{
			is = getClass().getClassLoader().getResourceAsStream(pathToFile);
			dis = new BufferedReader(new InputStreamReader(is, encoding));

			while ((file_line = dis.readLine()) != null)
			{
				// Obtain fields
				OrbitalElement orbit = parseAsteroid(file_line);

				// Store object in ArrayList
				if (orbit != null && satisfyConstraints(orbit.name, null)) vec.add(orbit);
			}

			// Close file
			dis.close();
			is.close();
			is = null;

			this.setReadElements(vec);
		} catch (Exception e2)
		{
			try {
				if (dis != null) dis.close();
				if (is != null) is.close();
				is = null;
			} catch (Exception exc) {}
			throw new JPARSECException(
					"error while reading file " + pathToFile + ".", e2);
		}
		if (vec.size() == 0) throw new JPARSECException("No asteroid was read");
	}

	/**
	 * Reads an example file with orbital elements of comets formatted in the
	 * standard way, established by the Minor Planet Center or either the
	 * commercial program SkyMap.
	 * <BR>
	 * An example of MPC format is:
	 * <BR>
	 *
	 * <pre>
	 * 0002P         2007 04 19.3070  0.339201  0.847059  186.5211  334.5780   11.7556  20060922  11.5  6.0  2P/Encke
	 * </pre>
	 *
	 * <BR>
	 * An example of SkyMap format is:
	 * <BR>
	 *
	 * <pre>
	 * 2P Encke                                       2003 12 29.8945  0.338844       0.847212 186.5134 334.5891  11.7634  11.5   6.0
	 * </pre>
	 *
	 * <BR>
	 * After the file is successfully read, the objects are stored using
	 * {@linkplain DataBase} with the identifier returned by
	 * {@linkplain #getDataBaseID()}.
	 *
	 * @throws JPARSECException If the reference time is invalid.
	 */
	public void readFileOfComets() throws JPARSECException
	{
		// Define necesary variables
		ArrayList<Object> vec = new ArrayList<Object>();
		String file_line = "";

		// Connect to the file
		InputStream is = null;
		BufferedReader dis = null;
		try
		{
			is = getClass().getClassLoader().getResourceAsStream(pathToFile);
			dis = new BufferedReader(new InputStreamReader(is, encoding));

			while ((file_line = dis.readLine()) != null)
			{

				// Obtain fields
				OrbitalElement orbit = parseComet(file_line);

				// Store object in ArrayList
				if (orbit != null && satisfyConstraints(orbit.name, null)) vec.add(orbit);
			}

			// Close file
			dis.close();
			is.close();
			is = null;

			this.setReadElements(vec);
		} catch (Exception e2)
		{
			try {
				if (dis != null) dis.close();
				if (is != null) is.close();
				is = null;
			} catch (Exception exc) {}
			throw new JPARSECException(
					"error while reading file " + pathToFile + ".", e2);
		}
		if (vec.size() == 0) throw new JPARSECException("No comet was read");
	}

	/**
	 * Reads the file of Near Earth Objects, with orbital elements formatted in the
	 * standard way, established by the Minor Planet Center,
	 * <BR>
	 * After the file is successfully read, the objects are stored using
	 * {@linkplain DataBase} with the identifier returned by
	 * {@linkplain #getDataBaseID()}.
	 *
	 * @param jd The date for the ephemerides calculations. Set to -1
	 * to get all NEOS without limiting the date.
	 * @param maxDays The maximum number of days for the elements validity.
	 * In case perihelion time is outside limit jd +/- maxDays, the NEO
	 * will not be returned. Recommended value is 365 days or lower.
	 * @throws JPARSECException If the reference time is invalid.
	 */
	public void readFileOfNEOs(double jd, double maxDays) throws JPARSECException
	{
		// Define necesary variables
		ArrayList<Object> vec = new ArrayList<Object>();
		String file_line = "";

		this.formatOfFile = FORMAT.MPC;

		// Connect to the file
		InputStream is = null;
		BufferedReader dis = null;
		try
		{
			is = getClass().getClassLoader().getResourceAsStream(pathToFile);
			dis = new BufferedReader(new InputStreamReader(is, encoding));

			while ((file_line = dis.readLine()) != null)
			{
				// Obtain fields
				OrbitalElement orbit = null;
				try {
					orbit = parseComet(file_line);
					if (orbit == null)
						orbit = parseAsteroid(file_line);
				} catch (Exception exc) {
					orbit = parseAsteroid(file_line);
				}

				if (orbit != null && jd > 0 && (orbit.referenceTime < jd - maxDays || orbit.referenceTime > jd + maxDays))
					orbit = null;

				// Store object in ArrayList
				if (orbit != null && satisfyConstraints(orbit.name, null)) vec.add(orbit);
			}

			// Close file
			dis.close();
			is.close();
			is = null;

			this.setReadElements(vec);
		} catch (Exception e2)
		{
			try {
				if (dis != null) dis.close();
				if (is != null) is.close();
				is = null;
			} catch (Exception exc) {}
			throw new JPARSECException(
					"error while reading file " + pathToFile + ".", e2);
		}
		if (vec.size() == 0) throw new JPARSECException("No NEO was read");
	}

	/**
	 * Reads a file with orbital elements of old comets.
	 *
	 * @param astro Optional date to retrieve only some of the comets around it. Can be null.
	 * @param years Maximum number of years around the date to retrieve a given comet.
	 * Set to 10 to retrieve all comets with elements referred to a date within +/- 10 years
	 * from the input date.
	 * @return The list of elements, or null if none is found for the interval of years.
	 * @throws JPARSECException If the reference time is invalid.
	 */
	public static ArrayList<OrbitalElement> readFileOfOldComets(AstroDate astro, int years) throws JPARSECException
	{
		// Define necesary variables
		String file_line = "";
		double jd = 0;
		if (astro != null) jd = astro.jd();

		// Connect to the file
		InputStream is = null;
		BufferedReader dis = null;
		try
		{
			is = ReadFile.class.getClassLoader().getResourceAsStream(OrbitEphem.PATH_TO_OLD_COMETS_FILE);
			dis = new BufferedReader(new InputStreamReader(is));

			file_line = dis.readLine();
			file_line = dis.readLine();
			ArrayList<OrbitalElement> out = new ArrayList<OrbitalElement>();
			while ((file_line = dis.readLine()) != null)
			{
				String name = file_line.substring(0, 42).trim();
				//int p = name.lastIndexOf("(");
				//if (p > 0) name = name.substring(0, p).trim();
				file_line = file_line.substring(42).trim();
				double day = DataSet.parseDouble(FileIO.getField(1, file_line, " ", true));
				int month = Integer.parseInt(FileIO.getField(2, file_line, " ", true));
				int year = Integer.parseInt(FileIO.getField(3, file_line, " ", true));

				// Read perihelion time
				AstroDate refTime = new AstroDate(year, month, day);
				double rt = refTime.jd();

				if (astro == null || Math.abs(jd-rt) < years * 365.25) {
					//double m0 = DataSet.parseDouble(FileIO.getField(4, file_line, " ", true));
					double perihD = DataSet.parseDouble(FileIO.getField(5, file_line, " ", true));
					double ecc = DataSet.parseDouble(FileIO.getField(6, file_line, " ", true));
					double i = DataSet.parseDouble(FileIO.getField(7, file_line, " ", true));
					double argP = DataSet.parseDouble(FileIO.getField(8, file_line, " ", true));
					double node = DataSet.parseDouble(FileIO.getField(9, file_line, " ", true));
					//double epochY = DataSet.parseDouble(FileIO.getField(10, file_line, " ", true));
					double mabs = DataSet.parseDouble(FileIO.getField(11, file_line, " ", true));
					double g = 0.0;
					boolean magOK = true;
					try {
						g = DataSet.parseDouble(FileIO.getField(12, file_line, " ", true));
					} catch (Exception exc) {
						// Sometimes mabs is missing ...
						g = mabs;
						mabs = 10; // Unknown value.
						magOK = false;
					}
					if (g == 0.0 && mabs == 0.0) magOK = false; // Sometimes both are 0, unrealistic

					// Obtain fields
					OrbitalElement orbit = new OrbitalElement();
					orbit.name = name;
					orbit.absoluteMagnitude = (float) mabs;
					orbit.meanAnomaly = 0.0;
					orbit.argumentOfPerihelion = argP * Constant.DEG_TO_RAD;
					orbit.ascendingNodeLongitude = node * Constant.DEG_TO_RAD;
					orbit.inclination = i * Constant.DEG_TO_RAD;
					orbit.eccentricity = ecc;
					orbit.perihelionDistance = perihD;
					orbit.magnitudeSlope = (float) g;
					orbit.referenceEquinox = Constant.J2000;
					orbit.referenceTime = rt;
					orbit.magnitudeModel = MAGNITUDE_MODEL.COMET_gk;
					if (!magOK) orbit.magnitudeModel = MAGNITUDE_MODEL.NONE;

					// Set data for elliptic motion
					if (orbit.eccentricity != 1.0)
					{
						orbit.semimajorAxis = orbit.perihelionDistance / (1.0 - orbit.eccentricity);
						orbit.meanMotion = Constant.EARTH_MEAN_ORBIT_RATE / (Math.abs(orbit.semimajorAxis) * Math.sqrt(Math
								.abs(orbit.semimajorAxis)));
					}

					// Store object in ArrayList
					out.add(orbit);
				}
			}

			// Close file
			dis.close();
			is.close();
			is = null;

			if (out.size() == 0) return null;

			return out;
		} catch (Exception e2)
		{
			try {
				if (dis != null) dis.close();
				if (is != null) is.close();
				is = null;
			} catch (Exception exc) {}
			throw new JPARSECException(
					"error while reading old comets file.", e2);
		}
	}

	/**
	 * Reads a file with orbital elements of probes in the JPARSEC format.
	 * <BR>
	 * An example of such format is:
	 * <BR>
	 *
	 * <pre>
	 * Galileo-1       e 4.3224100 24.682800 -175.312000 0.8323240 1.2979700 0.19785000 -158.241000 11/09.0/1989   1950    0 0 1989 11 9.690 1990 02 10.26
	 * </pre>
	 *
	 * <BR>
	 * After the file is successfully read, the objects are stored using
	 * {@linkplain DataBase} with the identifier returned by
	 * {@linkplain #getDataBaseID()}.
	 *
	 * @throws JPARSECException If the reference time is invalid.
	 */
	public void readFileOfProbes() throws JPARSECException
	{
		// Define necesary variables
		ArrayList<Object> vec = new ArrayList<Object>();
		String file_line = "";

		// Connect to the file
		InputStream is = null;
		BufferedReader dis = null;
		try
		{
			is = getClass().getClassLoader().getResourceAsStream(pathToFile);
			dis = new BufferedReader(new InputStreamReader(is, encoding));

			while ((file_line = dis.readLine()) != null)
			{
				// Obtain fields
				OrbitalElement orbit = parseProbe(file_line);

				// Store object in ArrayList
				if (orbit != null && satisfyConstraints(orbit.name, null)) vec.add(orbit);
			}

			// Close file
			dis.close();
			is.close();
			is = null;

			this.setReadElements(vec);
		} catch (Exception e2)
		{
			try {
				if (dis != null) dis.close();
				if (is != null) is.close();
				is = null;
			} catch (Exception exc) {}
			throw new JPARSECException(
					"error while reading file " + pathToFile + ".", e2);
		}
	}

	/**
	 * Reads a file of stars, formatted in any of the available formats.
	 * <BR>
	 * After the file is successfully read, the objects are stored using
	 * {@linkplain DataBase} with the identifier returned by
	 * {@linkplain #getDataBaseID()}.
	 *
	 * @throws JPARSECException Thrown if the format is invalid.
	 */
	public void readFileOfStars() throws JPARSECException
	{
		// Define necesary variables
		ArrayList<Object> vec = new ArrayList<Object>();
		String file_line = "";

		// Connect to the file
		InputStream is = null;
		BufferedReader dis = null;
		try
		{
			is = getClass().getClassLoader().getResourceAsStream(pathToFile);
			dis = new BufferedReader(new InputStreamReader(is, encoding));

			while ((file_line = dis.readLine()) != null)
			{
				// Obtain fields
				StarElement star = new StarElement();
				try {
					switch (formatOfFile)
					{
					case BSC5:
						star = parseBSC5file(file_line);
						break;
					case JPARSEC_SKY2000:
						star = parseJPARSECfile(file_line);
						break;
					default:
						throw new JPARSECException("invalid format.");
					}
				} catch (Exception exc) {
					star = null;
				}

				// Store object in ArrayList
				if (star != null && satisfyConstraints(star.name, (consLoc == null) ? null : star.getEquatorialPosition())) vec.add(star);
			}

			// Close file
			dis.close();
			is.close();
			is = null;

			this.setReadElements(vec);
		} catch (Exception e2)
		{
			try {
				if (dis != null) dis.close();
				if (is != null) is.close();
				is = null;
			} catch (Exception exc) {}
			throw new JPARSECException(
					"error while reading file " + pathToFile + ".", e2);
		}
	}

	/**
	 * Reads an external file with orbital elements of asteroids formatted in
	 * the standard way, established by the Minor Planet Center or either the
	 * commercial program SkyMap. The format is the same for trans-Neptunian or
	 * Centaur objects.
	 * <BR>
	 * An example of MPC format is:
	 * <BR>
	 *
	 * <pre>
	 * 00001    3.34  0.12 K0636 129.98342   73.23162   80.40970   10.58687  0.0800102  0.21432279   2.7653949    MPC 24219  4676  62 1839-1994 0.54 M-v 30  Bowell     0000             (1) Ceres
	 * </pre>
	 *
	 * <BR>
	 * An example of SkyMap format is:
	 * <BR>
	 *
	 * <pre>
	 *       1 Ceres                                  2005 08 18.0  86.9545  2.765979 0.080014  73.3924  80.4097  10.5860   3.34  0.12
	 * </pre>
	 *
	 * <BR>
	 * After the file is successfully read, the objects are stored using
	 * {@linkplain DataBase} with the identifier returned by
	 * {@linkplain #getDataBaseID()}.
	 *
	 * @param file The read file. If null the file will be read from {@linkplain ReadFile#pathToFile}.
	 * @throws JPARSECException If the reference time is invalid.
	 */
	public void readFileOfAsteroidsFromExternalFile(String file[]) throws JPARSECException
	{
		// Define necesary variables
		ArrayList<Object> vec = new ArrayList<Object>();

		// Connect to the file
		try
		{
			if (file == null) file = DataSet.arrayListToStringArray(ReadFile.readAnyExternalFile(pathToFile));

			for (int i=0; i<file.length; i++) {
				// Obtain fields
				OrbitalElement orbit = parseAsteroid(file[i]);

				// Store object in ArrayList
				if (orbit != null && satisfyConstraints(orbit.name, null)) vec.add(orbit);
			}

			this.setReadElements(vec);
		} catch (Exception e2)
		{
			throw new JPARSECException(
					"error while reading file " + pathToFile + ".", e2);
		}
		
		if (vec.size() == 0) throw new JPARSECException("No asteroid was read");
	}

	/**
	 * Reads an external file with orbital elements of comets formatted in the
	 * standard way, established by the Minor Planet Center or either the
	 * commercial program SkyMap.
	 * <BR>
	 * An example of MPC format is:
	 * <BR>
	 *
	 * <pre>
	 * 0002P         2007 04 19.3070  0.339201  0.847059  186.5211  334.5780   11.7556  20060922  11.5  6.0  2P/Encke
	 * </pre>
	 *
	 * <BR>
	 * An example of SkyMap format is:
	 * <BR>
	 *
	 * <pre>
	 * 2P Encke                                       2003 12 29.8945  0.338844       0.847212 186.5134 334.5891  11.7634  11.5   6.0
	 * </pre>
	 *
	 * <BR>
	 * After the file is successfully read, the objects are stored using
	 * {@linkplain DataBase} with the identifier returned by
	 * {@linkplain #getDataBaseID()}.
	 *
	 * @param file The read file. If null the file will be read from {@linkplain ReadFile#pathToFile}.
	 * @throws JPARSECException If the reference time is invalid.
	 */
	public void readFileOfCometsFromExternalFile(String file[]) throws JPARSECException
	{
		// Define necesary variables
		ArrayList<Object> vec = new ArrayList<Object>();

		// Connect to the file
		try
		{
			if (file == null) file = DataSet.arrayListToStringArray(ReadFile.readAnyExternalFile(pathToFile));

			for (int i=0; i<file.length; i++) {
				// Obtain fields
				OrbitalElement orbit = parseComet(file[i]);

				// Store object in ArrayList
				if (orbit != null && satisfyConstraints(orbit.name, null)) vec.add(orbit);
			}

			this.setReadElements(vec);
		} catch (Exception e2)
		{
			throw new JPARSECException(
					"error while reading file " + pathToFile + ".", e2);
		}
	}

	/**
	 * Reads an external file with satellites elements formatted in the standard
	 * way, used by the comunity. Results are copied to {@linkplain DataBase}.
	 * <BR>
	 * An example of such format is:
	 * <BR>
	 *
	 * <pre>
	 * HST
	 * <BR>
	 * 1 20580U 90037B   05218.56668550  .00000739  00000-0  45252-4 0  3985
	 * <BR>
	 * 2 20580  28.4690 300.5069 0003775  90.9393 269.1626 14.99794238637817
	 * <BR>
	 * </pre>
	 *
	 * @param file The read file. If null the file will be read from {@linkplain ReadFile#pathToFile}.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public void readFileOfArtificialSatellitesFromExternalFile(String file[]) throws JPARSECException
	{
		// Define necesary variables
		ArrayList<Object> vec = new ArrayList<Object>();
		String nom;
		int ye, rv, sat_n;
		double te, inc, ra, ec, mm, ma, wp, m2, m3, drag;

		String line0 = "";
		String line1 = "";
		String line2 = "";

		ReadFormat rf1 = new ReadFormat();
		ReadFormat rf2 = new ReadFormat();
		rf1.setFormatToRead(FileFormatElement.TLE_LINE1_FORMAT);
		rf2.setFormatToRead(FileFormatElement.TLE_LINE2_FORMAT);

		ArrayList<String> names = new ArrayList<String>();

		// Connect to the file
		try
		{
			if (file == null) file = DataSet.arrayListToStringArray(ReadFile.readAnyExternalFile(pathToFile));

			for (int i=0; i<file.length; i = i + 3) {
				line0 = file[i];
				line1 = file[i+1];
				line2 = file[i+2];

				// Obtain fields
				nom = line0.trim();

				ye = 2000 + rf1.readInteger(line1, "EPOCH_YEAR_LAST2DIGITS");
				te = rf1.readDouble(line1, "EPOCH_DAY");
				m2 = rf1.readDouble(line1, "FIRST_DERIVATIVE");
				String sd = rf1.readString(line1, "SECOND_DERIVATIVE");
				m3 = DataSet.parseDouble(sd.substring(0, sd.length()-2).trim()) / 1.0E5;
				m3 *= Math.pow(10.0, DataSet.parseDouble(sd.substring(sd.length()-2).trim()));
				sat_n = rf1.readInteger(line1, "SAT_NUMBER");
				sd = rf1.readString(line1, "DRAG");
				drag = DataSet.parseDouble(sd.substring(0, sd.length()-2).trim()) / 1.0E5;
				drag *= Math.pow(10.0, DataSet.parseDouble(sd.substring(sd.length()-2).trim()));

				inc = rf2.readDoubleToRadians(line2, "INCLINATION");
				ra = rf2.readDoubleToRadians(line2, "ASCENDING_NODE_RA");
				ec = DataSet.parseDouble("." + rf2.readString(line2, "ECCENTRICITY"));
				mm = rf2.readDouble(line2, "MEAN_MOTION");
				ma = rf2.readDoubleToRadians(line2, "MEAN_ANOMALY");
				rv = rf2.readInteger(line2, "REVOLUTION_NUMBER");
				wp = rf2.readDoubleToRadians(line2, "ARGUMENT_OF_PERIGEE");

				// Convert angles to radians.
				mm = mm * Constant.TWO_PI;
				m2 = m2 * Constant.TWO_PI;

				// Create Satellite object
				SatelliteOrbitalElement sat = new SatelliteOrbitalElement(nom, sat_n, ye, te, m2, inc, ra, ec, ma, wp,
						mm, rv, m3, drag);

				// Store object in ArrayList
				if (satisfyConstraints(sat.getName(), null)) {
					int index = names.indexOf(nom);
					if (index >= 0) {
						index = 1;
						while (true) {
							String nom2 = nom + " ("+index+")";
							if (names.indexOf(nom2) < 0) {
								nom = nom2;
								sat.name = nom;
								break;
							}
							index ++;
						}
					}
					names.add(nom);
					vec.add(sat);
				}
			}

			this.setReadElements(vec);
		} catch (Exception exc) {
			throw new JPARSECException(exc.getCause());
		}
	}

	/**
	 * Reads the example file with satellites elements formatted in the standard
	 * way, used by the community. Results are copied to {@linkplain DataBase}.
	 * <BR>
	 * An example of such format is:
	 * <BR>
	 *
	 * <pre>
	 * HST
	 * <BR>
	 * 1 20580U 90037B   05218.56668550  .00000739  00000-0  45252-4 0  3985
	 * <BR>
	 * 2 20580  28.4690 300.5069 0003775  90.9393 269.1626 14.99794238637817
	 * <BR>
	 * </pre>
	 *
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public void readFileOfArtificialSatellites() throws JPARSECException
	{
		// Define necesary variables
		ArrayList<Object> vec = new ArrayList<Object>();
		String nom;
		int ye, rv, sat_n;
		double te, inc, ra, ec, mm, ma, wp, m2, m3, drag;

		String line0 = "";
		String line1 = "";
		String line2 = "";

		ReadFormat rf1 = new ReadFormat();
		ReadFormat rf2 = new ReadFormat();
		rf1.setFormatToRead(FileFormatElement.TLE_LINE1_FORMAT);
		rf2.setFormatToRead(FileFormatElement.TLE_LINE2_FORMAT);

		ArrayList<String> names = new ArrayList<String>();

		// Connect to the file
		InputStream is = null;
		BufferedReader dis = null;
		try
		{
			is = getClass().getClassLoader().getResourceAsStream(pathToFile);
			dis = new BufferedReader(new InputStreamReader(is, encoding));

			while ((line0 = dis.readLine()) != null)
			{
				line1 = dis.readLine();
				line2 = dis.readLine();

				// Obtain fields
				nom = line0.trim();

				ye = 2000 + rf1.readInteger(line1, "EPOCH_YEAR_LAST2DIGITS");
				te = rf1.readDouble(line1, "EPOCH_DAY");
				m2 = rf1.readDouble(line1, "FIRST_DERIVATIVE");
				String sd = rf1.readString(line1, "SECOND_DERIVATIVE");
				m3 = DataSet.parseDouble(sd.substring(0, sd.length()-2).trim()) / 1.0E5;
				m3 *= Math.pow(10.0, DataSet.parseDouble(sd.substring(sd.length()-2).trim()));
				sat_n = rf1.readInteger(line1, "SAT_NUMBER");
				sd = rf1.readString(line1, "DRAG");
				drag = DataSet.parseDouble(sd.substring(0, sd.length()-2).trim()) / 1.0E5;
				drag *= Math.pow(10.0, DataSet.parseDouble(sd.substring(sd.length()-2).trim()));

				inc = rf2.readDoubleToRadians(line2, "INCLINATION");
				ra = rf2.readDoubleToRadians(line2, "ASCENDING_NODE_RA");
				ec = DataSet.parseDouble("." + rf2.readString(line2, "ECCENTRICITY"));
				mm = rf2.readDouble(line2, "MEAN_MOTION");
				ma = rf2.readDoubleToRadians(line2, "MEAN_ANOMALY");
				rv = rf2.readInteger(line2, "REVOLUTION_NUMBER");
				wp = rf2.readDoubleToRadians(line2, "ARGUMENT_OF_PERIGEE");

				// Convert angles to radians.
				mm = mm * Constant.TWO_PI;
				m2 = m2 * Constant.TWO_PI;

				// Create Satellite object
				SatelliteOrbitalElement sat = new SatelliteOrbitalElement(nom, sat_n, ye, te, m2, inc, ra, ec, ma, wp,
						mm, rv, m3, drag);

				// Store object in ArrayList
				if (satisfyConstraints(sat.getName(), null)) {
					int index = names.indexOf(nom);
					if (index >= 0) {
						index = 1;
						while (true) {
							String nom2 = nom + " ("+index+")";
							if (names.indexOf(nom2) < 0) {
								nom = nom2;
								sat.name = nom;
								break;
							}
							index ++;
						}
					}
					names.add(nom);
					vec.add(sat);
				}
			}

			// Close file
			dis.close();
			is.close();
			is = null;

			this.setReadElements(vec);
		} catch (Exception e2)
		{
			try {
				if (dis != null) dis.close();
				if (is != null) is.close();
				is = null;
			} catch (Exception exc) {}
			throw new JPARSECException("Error reading file "+pathToFile+", line: "+FileIO.getLineSeparator()+line1+FileIO.getLineSeparator()+line2, e2);
		}
	}

	/**
	 * Obtain number of objects read.
	 *
	 * @return Number of objects.
	 */
	public int getNumberOfObjects()
	{
		Object o[] = this.getReadElements();
		if (o == null) return 0;
		return o.length;
	}

	/**
	 * Obtains one set of orbital elements. Don't use this method in a loop,
	 * since it is very slow, use it only to retrieve one record.
	 *
	 * @param index ID value for the object. From 0 to {@linkplain ReadFile#getNumberOfObjects()}-1.
	 * @return The {@linkplain OrbitalElement} object.
	 * @throws JPARSECException Thrown if the index is not valid.
	 */
	public OrbitalElement getOrbitalElement(int index) throws JPARSECException
	{
		Object o[] = this.getReadElements();
		if (o == null) return null;

		if (index < 0 || index >= o.length)
			throw new JPARSECException("invalid object " + index + ".");

		OrbitalElement orbit = (OrbitalElement) o[index];

		return orbit;
	}

	/**
	 * Obtains one set of orbital elements of artificial satellites. Don't use this method in a loop,
	 * since it is very slow, use it only to retrieve one record.
	 *
	 * @param index ID value for the object. From 0 to {@linkplain ReadFile#getNumberOfObjects()}-1.
	 * @return The {@linkplain SatelliteOrbitalElement} object.
	 * @throws JPARSECException Thrown if the index is not valid.
	 */
	public SatelliteOrbitalElement getSatelliteOrbitalElement(int index) throws JPARSECException
	{
		Object o[] = this.getReadElements();
		if (o == null) return null;

		if (index < 0 || index >= o.length)
			throw new JPARSECException("invalid object " + index + ".");

		SatelliteOrbitalElement orbit = (SatelliteOrbitalElement) o[index];

		return orbit;
	}

	/**
	 * Obtains one set of orbital elements of natural satellites. Don't use this method in a loop,
	 * since it is very slow, use it only to retrieve one record.
	 *
	 * @param index ID value for the object. From 0 to {@linkplain ReadFile#getNumberOfObjects()}-1.
	 * @return The Moon orbit object.
	 * @throws JPARSECException Thrown if the index is not valid.
	 */
	public MoonOrbitalElement getMoonOrbitalElement(int index) throws JPARSECException
	{
		Object o[] = this.getReadElements();
		if (o == null) return null;

		if (index < 0 || index >= o.length)
			throw new JPARSECException("invalid object " + index + ".");

		MoonOrbitalElement orbit = (MoonOrbitalElement) o[index];

		return orbit;
	}

	/**
	 * Obtains one set of elements for stars. Don't use this method in a loop,
	 * since it is very slow, use it only to retrieve one record.
	 *
	 * @param index ID value for the object. From 0 to {@linkplain ReadFile#getNumberOfObjects()}-1.
	 * @return The {@linkplain StarElement} object.
	 * @throws JPARSECException Thrown if the index is not valid.
	 */
	public StarElement getStarElement(int index) throws JPARSECException
	{
		Object o[] = this.getReadElements();
		if (o == null) return null;

		if (index < 0 || index >= o.length)
			throw new JPARSECException("invalid object " + index + ".");

		StarElement star = (StarElement) o[index];

		return star;
	}

	/**
	 * Obtains one set of elements for double stars. Don't use this method in a loop,
	 * since it is very slow, use it only to retrieve one record.
	 *
	 * @param index ID value for the object. From 0 to {@linkplain ReadFile#getNumberOfObjects()}-1.
	 * @return The {@linkplain DoubleStarElement} object.
	 * @throws JPARSECException Thrown if the index is not valid.
	 */
	public DoubleStarElement getDoubleStarElement(int index) throws JPARSECException
	{
		Object o[] = this.getReadElements();
		if (o == null) return null;

		if (index < 0 || index >= o.length)
			throw new JPARSECException("invalid object " + index + ".");

		DoubleStarElement star = (DoubleStarElement) o[index];

		return star;
	}

	/**
	 * Obtains one set of data for variable stars. Don't use this method in a loop,
	 * since it is very slow, use it only to retrieve one record.
	 *
	 * @param index ID value for the object. From 0 to {@linkplain ReadFile#getNumberOfObjects()}-1.
	 * @return The {@linkplain VariableStarElement} object.
	 * @throws JPARSECException Thrown if the index is not valid.
	 */
	public VariableStarElement getVariableStarElement(int index) throws JPARSECException
	{
		Object o[] = this.getReadElements();
		if (o == null) return null;

		if (index < 0 || index >= o.length)
			throw new JPARSECException("invalid object " + index + ".");

		VariableStarElement star = (VariableStarElement) o[index];

		return star;
	}
	
	/**
	 * Search for an object by it's name.
	 *
	 * @param object Name of the object to seach for.
	 * @return Index value of the object found with the closest name. 
	 * The index should be checked to confirm it belongs to the desired object. 
	 * -1 is returned if no exact/approximate match is found.
	 * @throws JPARSECException Thrown if the index is not valid.
	 */
	public int searchByName(String object) throws JPARSECException
	{
		// Search object
		int index = -1;
		Object o[] = this.getReadElements();
		if (o == null) return index;
		for (int i = 0; i < o.length; i++)
		{
			String name = obtainName(i, o);
			if (name.equals(object) || (o[i] instanceof StarElement && name.indexOf("("+object+")") >= 0))
			{
				index = i;
				break;
			} else {
				if (name.toLowerCase().indexOf(object.toLowerCase()) >= 0)
				{
					index = i;
				}
			}
		}

		return index;
	}

	/**
	 * Search for an object by it's position. This method is not supported for orbital elements
	 * (comets, asteroids, transneptunians, artificial satellites). It is only supported for stars.
	 *
	 * @param loc Location of the object to search for. Note this location should be usually
	 * mean J2000 equatorial coordinates.
	 * @param radius Radius in radians around the given position to search for an object.
	 * @return index Index of the closest object to the given location, if any, at a distance
	 * lower than radius. -1 is returned if no match is found.
	 * @throws JPARSECException Thrown if the object is not supported.
	 */
	public int searchByPosition(LocationElement loc, double radius) throws JPARSECException
	{
		// Search object
		int index = -1;
		Object o[] = this.getReadElements();
		if (o == null) return index;
		double minDist = -1;
		for (int i = 0; i < o.length; i++)
		{
			LocationElement l = obtainPos(i, o);
			double d = LocationElement.getAngularDistance(loc, l);
			if (d < radius) {
				if (d < minDist || minDist == -1) {
					index = i;
					minDist = d;
				}
			}
		}

		return index;
	}

	/**
	 * Search for an object by it's position. This method is not supported for orbital elements
	 * (comets, asteroids, transneptunians, artificial satellites). It is only supported for stars.
	 *
	 * @param loc Location of the object to search for. Note this location should be usually
	 * mean J2000 equatorial coordinates.
	 * @param radius Radius in radians around the given position to search for an object.
	 * @return index Indexes of the objects close to the given location, if any, at a distance
	 * lower than radius, ordered by radius in crescent order. null is returned if no match is found.
	 * @throws JPARSECException Thrown if the object is not supported.
	 */
	public int[] searchByPositionGetAll(LocationElement loc, double radius) throws JPARSECException
	{
		// Search object
		Integer index[] = null;
		Object o[] = this.getReadElements();
		if (o == null) return null;
		index = new Integer[0];
		double dist[] = new double[0];
		for (int i = 0; i < o.length; i++)
		{
			LocationElement l = obtainPos(i, o);
			double d = LocationElement.getAngularDistance(loc, l);
			if (d < radius) {
				index = (Integer[]) DataSet.addObjectArray(index, new Integer[] {new Integer(i)});
				dist = DataSet.addDoubleArray(dist, new double[] {d});
			}
		}
		index = (Integer[]) DataSet.sortInCrescent(index, dist);
		if (index.length == 0) return null;
		return DataSet.toPrimitiveArrayInteger(index);
	}
	
	private String obtainName(int index, Object obj[]) throws JPARSECException
	{
		if (index < 0 || index >= obj.length)
			throw new JPARSECException("invalid object " + index + ".");
		Object o = obj[index];
		if (o instanceof StarElement) return ((StarElement) o).name;
		if (o instanceof OrbitalElement) return ((OrbitalElement) o).name;
		if (o instanceof MoonOrbitalElement) return ((MoonOrbitalElement) o).name;
		if (o instanceof SatelliteOrbitalElement) return ((SatelliteOrbitalElement) o).getName();
		if (o instanceof DoubleStarElement) {
			DoubleStarElement dstar = (DoubleStarElement) o;
			return dstar.name+ " (ADS "+dstar.ads+", WDS "+dstar.wds+", HP "+dstar.hipparcos+", HD "+dstar.hd+")";
		}
		if (o instanceof VariableStarElement) return ((VariableStarElement) o).name;
		throw new JPARSECException("invalid object " + index + ".");
	}

	private LocationElement obtainPos(int index, Object obj[]) throws JPARSECException
	{
		if (index < 0 || index >= obj.length)
			throw new JPARSECException("invalid object " + index + ".");
		Object o = obj[index];
		if (o instanceof StarElement) return ((StarElement) o).getEquatorialPosition();
		if (o instanceof OrbitalElement) throw new JPARSECException("unsupported object type OrbitalElement.");
		if (o instanceof MoonOrbitalElement) throw new JPARSECException("unsupported object type MoonOrbitalElement.");
		if (o instanceof SatelliteOrbitalElement) throw new JPARSECException("unsupported object type SatelliteOrbitalElement.");
		if (o instanceof DoubleStarElement) return ((DoubleStarElement) o).getEquatorialPosition();
		if (o instanceof VariableStarElement) return ((VariableStarElement) o).getEquatorialPosition();
		throw new JPARSECException("invalid object " + index + ".");
	}

	/**
	 * Gets the name of an object.
	 *
	 * @param index Index value for the object.
	 * @return Name of the object.
	 * @throws JPARSECException Thrown if the index is not valid.
	 */
	public String getObjectName(int index) throws JPARSECException
	{
		Object o[] = this.getReadElements();
		if (o == null) return null;
		if (index < 0 || index >= o.length)
			throw new JPARSECException("invalid object " + index + ".");

		return obtainName(index, o);
	}

	/**
	 * Read MPC packed date. This method should be valid until year 3500 or so
	 * (Character.MAX_RADIX must be greater than 21 in the system to support the
	 * days in a month, and this is ensured due to the fact that exist 26
	 * characters in the alphabet).
	 *
	 * @param epoch String representing a packed date.
	 * @return Julian day.
	 * @throws JPARSECException If the packed date is invalid.
	 */
	public static double readMPCPackedDate(String epoch) throws JPARSECException
	{
		int century = Integer.parseInt(epoch.substring(0, 1), Character.MAX_RADIX);
		int year = century * 100 + Integer.parseInt(epoch.substring(1, 3));
		int month = Integer.parseInt(epoch.substring(3, 4), Character.MAX_RADIX);
		int day = Integer.parseInt(epoch.substring(4, 5), Character.MAX_RADIX);
		double fraction = 0.0;
		if (epoch.length() > 5)
			fraction = DataSet.parseDouble("." + epoch.substring(5));
		double days = day + fraction;

		AstroDate astro = new AstroDate(year, month, days);

		return astro.jd();
	}

	/**
	 * Reads a file with orbital elements of natural satellites formatted in a
	 * way defined by the JPL. See <A target="_blank" href="http://ssd.jpl.nasa.gov/?sat_elem">http://ssd.jpl.nasa.gov/?sat_elem</A>
	 * for more information. Results are copied to {@linkplain DataBase}.
	 * <BR>
	 * An example of such format is given next. Note the column positions are
	 * not taken into account, only the order where the elements are given and
	 * the overall format. See example files, named like JPL_natural_satellites.txt.
	 * <BR>
	 *
	 * <pre>
	 * Mars
	 * Mean orbital elements referred to the local Laplace planes
	 * Epoch 1950 Jan. 1.00 TT
	 * Solution: MAR033
	 * Phobos 9380.  0.0151 150.247  92.474 1.075 164.931 1128.8444155  0.319  1.131  2.262 317.724 52.924 0.046 5
	 * Deimos 23460. 0.0002 290.496 296.230 1.793 339.600  285.1618919  1.262 26.892 54.536 316.700 53.564 0.897 5
	 * </pre>
	 *
	 * @param planet Planet whose satellites should be obtained.
	 * @throws JPARSECException If the read fails.
	 */
	public void readFileOfNaturalSatellites(TARGET planet) throws JPARSECException
	{
		// Define necesary variables
		String file_line = "";
		ArrayList<Object> v = new ArrayList<Object>();
		String path = FileIO.DATA_ORBITAL_ELEMENTS_DIRECTORY + "JPL_natural_satellites.txt";

		// Connect to the file
		InputStream is = null;
		BufferedReader dis = null;
		try
		{
			is = getClass().getClassLoader().getResourceAsStream(path);
			dis = new BufferedReader(new InputStreamReader(is, encoding));

			while ((file_line = dis.readLine()) != null)
			{

				if (file_line.equals(planet.getEnglishName()))
				{
					String type = "Mean equatorial orbital elements";
					if (planet != TARGET.Pluto) type = dis.readLine();
					String epoch = dis.readLine();
					String solution = dis.readLine();
					int laplace = type.toLowerCase().indexOf("laplace");
					int equatorial = type.toLowerCase().indexOf("equatorial");
					int ecliptic = type.toLowerCase().indexOf("ecliptic");
					int equator = type.toLowerCase().indexOf("equator");
					MoonOrbitalElement.REFERENCE_PLANE typesat = null;
					if (laplace >= 0)
						typesat = MoonOrbitalElement.REFERENCE_PLANE.LAPLACE; // Laplace
					if (equatorial >= 0)
						typesat = MoonOrbitalElement.REFERENCE_PLANE.EQUATOR; // Equatorial
					if (ecliptic >= 0)
						typesat = MoonOrbitalElement.REFERENCE_PLANE.ECLIPTIC; // Ecliptic
					if (equator >= 0 && typesat == null)
						typesat = MoonOrbitalElement.REFERENCE_PLANE.PLANET_EQUATOR; // Equator
					String solutionsat = FileIO.getRestAfterField(1, solution, " ", true);
					int year = Integer.parseInt(FileIO.getField(2, epoch, " ", true));
					double day = DataSet.parseDouble(FileIO.getField(4, epoch, " ", true)); // TT
					String months = "JanFebMarAprMayJunJulAugSepOctNovDec";
					String monthsat = FileIO.getField(3, epoch, " ", true);
					int month = months.toLowerCase()
							.indexOf(monthsat.toLowerCase().substring(0, monthsat.length() - 1));
					month = 1 + month / 3;
					AstroDate astro = new AstroDate(year, month, day);
					double epochJD = astro.jd();
					solution = FileIO.getRestAfterField(1, solution, " ", true);

					do
					{
						file_line = dis.readLine();
						if (file_line == null) break;

						if (!file_line.equals("*"))
						{
							double prec_peri = (365.25 * Double
									.parseDouble(FileIO.getField(10, file_line, " ", true))) / (2.0 * Math.PI);
							double prec_node = (365.25 * Double
									.parseDouble(FileIO.getField(11, file_line, " ", true))) / (2.0 * Math.PI);
							if (prec_peri != 0.0) prec_peri = 1.0 / prec_peri;
							if (prec_node != 0.0) prec_node = 1.0 / prec_node;

							int nf = FileIO.getNumberOfFields(file_line, " ", true);
							double LaplaceRA = 0.0, LaplaceDEC = 0.0;
							if (nf > 12) {
								LaplaceRA = DataSet.parseDouble("0" + FileIO.getField(12, file_line, " ", true)) * Constant.DEG_TO_RAD;
								LaplaceDEC = DataSet.parseDouble("0" + FileIO.getField(13, file_line, " ", true)) * Constant.DEG_TO_RAD;
							}
							MoonOrbitalElement orbit = new MoonOrbitalElement(
									FileIO.getField(1, file_line, " ", true),
									DataSet.parseDouble(FileIO.getField(2, file_line, " ", true)) / Constant.AU,
									DataSet.parseDouble(FileIO.getField(5, file_line, " ", true)) * Constant.DEG_TO_RAD,
									DataSet.parseDouble(FileIO.getField(3, file_line, " ", true)),
									DataSet.parseDouble(FileIO.getField(4, file_line, " ", true)) * Constant.DEG_TO_RAD,
									DataSet.parseDouble(FileIO.getField(7, file_line, " ", true)) * Constant.DEG_TO_RAD,
									DataSet.parseDouble(FileIO.getField(6, file_line, " ", true)) * Constant.DEG_TO_RAD,
									epochJD,
									DataSet.parseDouble(FileIO.getField(8, file_line, " ", true)) * Constant.DEG_TO_RAD,
									EphemerisElement.EQUINOX_J2000,
									Constant.J1900,
									Constant.J2000 + Constant.JULIAN_DAYS_PER_CENTURY,
									typesat,
									LaplaceRA, LaplaceDEC,
									prec_peri,
									prec_node,
									solutionsat);
							orbit.centralBody = planet;
							if (satisfyConstraints(orbit.name, null)) v.add(orbit);

						}
					} while (!file_line.equals("*") && typesat != null);
				}
			}

			// Close file
			dis.close();
			is.close();
			is = null;
		} catch (Exception e2)
		{
			try {
				if (dis != null) dis.close();
				if (is != null) is.close();
				is = null;
			} catch (Exception exc) {}
			throw new JPARSECException(
					"error while reading path " + path + "." , e2);
		}

		this.setReadElements(v);
	}

	/**
	 * Returns all the resources contained in a given path inside the classpath.
	 * @param path An existing path inside the classpath.
	 * @return The set of files inside that path.
	 * @throws IOException In case of problems opening or reading the provided path.
	 */
	public static ArrayList<String> getResourceFiles(String path) throws IOException {
		  
		ArrayList<String> filenames = new ArrayList<String>();
		Enumeration<URL> en = ReadFile.class.getClassLoader().getResources(path);
		if (en.hasMoreElements()) {
			URL url = en.nextElement();
			JarURLConnection urlcon = (JarURLConnection) (url.openConnection());
			JarFile jar = urlcon.getJarFile(); 
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				String entry = entries.nextElement().getName();
				filenames.add(entry);
			}
		}
		    		    
		return filenames;
	}
	
	/**
	 * Reads a file in the classpath and returns it contents as an array of strings.
	 * Default encoding is ISO-8859-1.
	 * @param jarpath Path inside .jar file of the file to read.
	 * @return Array of strings.
	 * @throws JPARSECException If an error occurs accessing the resource.
	 */
	public static ArrayList<String> readResource(String jarpath) throws JPARSECException
	{
		String charset = ReadFile.ENCODING_ISO_8859;
		return ReadFile.readResource(jarpath, charset);
	}

	/**
	 * Returns if a resource file is available in the classpath.
	 * @param jarpath Path inside .jar file of the file to read.
	 * @return True or false.
	 * @throws JPARSECException In case the path is null.
	 */
	public static boolean resourceAvailable(String jarpath) throws JPARSECException
	{
		String charset = ReadFile.ENCODING_ISO_8859;
		ReadFile rfile = new ReadFile(jarpath, charset);
		URL url = rfile.getResourceAsURL();
		if (url == null) return false;
		return true;
	}

	/**
	 * Reads an image in a given path.
	 * @param path Path of an image.
	 * @return The image.
	 * @throws JPARSECException If the image cannot be read.
	 */
	public static BufferedImage readImage(String path) throws JPARSECException {
		try {
			return ImageIO.read(new File(path));
		} catch (Exception e) {
			throw new JPARSECException("could not read the image "+path, e);
		}
	}

	/**
	 * Reads an image in the classpath.
	 * @param jarpath Path of an image in the classpath.
	 * @return The image.
	 * @throws JPARSECException If the image cannot be read.
	 */
	public static BufferedImage readImageResource(String jarpath) throws JPARSECException {
		ReadFile rfile = new ReadFile(jarpath, ReadFile.ENCODING_ISO_8859);
		try {
			return ImageIO.read(rfile.getClass().getClassLoader().getResourceAsStream(jarpath));
		} catch (Exception e) {
			throw new JPARSECException("could not read the image "+jarpath, e);
		}
	}

	/**
	 * Reads a file inside a .jar located in the classpath and returns it contents as an array of strings.
	 * @param jarpath Path inside .jar file of the file to read.
	 * @param charset The name of the charset used to encode the file. Can be NONE, UTF-8,
	 * ISO-8859-1, and so on.
	 * @return Array of strings.
	 * @throws JPARSECException If an error occurs accessing the resource.
	 */
	public static ArrayList<String> readResource(String jarpath, String charset) throws JPARSECException
	{
		ReadFile rfile = new ReadFile(jarpath, charset, false);
		return rfile.readResource();
	}

	/**
	 * Reads a file inside a .jar located in the classpath and returns it contents as an array of strings.
	 * Only lines containing a given record are returned.
	 * @param jarpath Path inside .jar file of the file to read.
	 * @param charset The name of the charset used to encode the file. Can be NONE, UTF-8,
	 * ISO-8859-1, and so on.
	 * @param record Something to search in each line of the file.
	 * @return Array of strings.
	 * @throws JPARSECException If an error occurs accessing the resource.
	 */
	public static ArrayList<String> readResourceContaining(String jarpath, String charset, String record) throws JPARSECException
	{
		ReadFile rfile = new ReadFile(jarpath, charset, false);
		return rfile.readResourceContaining(record);
	}

	/**
	 * Reads a file inside a .jar located in the classpath and returns it contents as an array of strings.
	 * @param jarpath Path inside .jar file of the file to read.
	 * @param charset The name of the charset used to encode the file. Can be NONE, UTF-8,
	 * ISO-8859-1, and so on.
	 * @param n Number of lines to read from the beginning of the file.
	 * @return Array of strings.
	 * @throws JPARSECException If an error occurs accessing the resource.
	 */
	public static ArrayList<String> readResourceFirstNlines(String jarpath, String charset, int n) throws JPARSECException
	{
		ReadFile rfile = new ReadFile(jarpath, charset, false);
		return rfile.readResourceFirstNlines(n);
	}

	/**
	 * Reads a file inside a .jar located in the classpath and returns it contents as an array of strings.
	 * @param jarpath Path inside .jar file of the file to read.
	 * @param charset The name of the charset used to encode the file. Can be NONE, UTF-8,
	 * ISO-8859-1, and so on.
	 * @param n Number of lines to read from the end of the file.
	 * @return Array of strings.
	 * @throws JPARSECException If an error occurs accessing the resource.
	 */
	public static ArrayList<String> readResourceLastNlines(String jarpath, String charset, int n) throws JPARSECException
	{
		ReadFile rfile = new ReadFile(jarpath, charset, false);
		return rfile.readResourceLastNlines(n);
	}

	/**
	 * Reads a file inside a .jar located in the classpath and returns it contents as an array of strings.
	 * @param jarpath Path inside .jar file of the file to read.
	 * @param charset The name of the charset used to encode the file. Can be NONE, UTF-8,
	 * ISO-8859-1, and so on.
	 * @param i0 First line to read (index, starting from 0).
	 * @param i1 Last line to read (index, starting from 0).
	 * @return Array of strings.
	 * @throws JPARSECException If an error occurs accessing the resource.
	 */
	public static ArrayList<String> readResourceSomeLines(String jarpath, String charset, int i0, int i1) throws JPARSECException
	{
		ReadFile rfile = new ReadFile(jarpath, charset, false);
		return rfile.readResourceSomeLines(i0, i1);
	}

	/**
	 * Reads a file inside a .jar located in the classpath and returns the number of lines it contains.
	 * @param jarpath Path inside .jar file of the file to read.
	 * @param charset The name of the charset used to encode the file. Can be NONE, UTF-8,
	 * ISO-8859-1, and so on.
	 * @return Number of lines.
	 * @throws JPARSECException If an error occurs accessing the resource.
	 */
	public static int readResourceGetNumberOfLines(String jarpath, String charset) throws JPARSECException
	{
		ReadFile rfile = new ReadFile(jarpath, charset, false);
		return rfile.readResourceAndReturnNumberOfLines();
	}

	/**
	 * Reads an external file and returns it contents as an array of strings,
	 * assuming ISO-8859-1 encoding.
	 * @param pathToFile Path to the file.
	 * @return Array of strings.
	 * @throws JPARSECException If an error occurs accesing the resource.
	 */
	public static ArrayList<String> readAnyExternalFile(String pathToFile) throws JPARSECException
	{
		return readAnyExternalFile(pathToFile, ReadFile.ENCODING_ISO_8859);
	}

	/**
	 * Reads an external file and returns it contents as an array of strings.
	 * @param pathToFile Path to the file.
	 * @param charset The name of the charset used to encode the file. Can be NONE, UTF-8,
	 * ISO-8859-1, and so on.
	 * @return Array of strings.
	 * @throws JPARSECException If an error occurs accessing the resource.
	 */
	public static ArrayList<String> readAnyExternalFile(String pathToFile, String charset) throws JPARSECException
	{
		// Define necesary variables
		ArrayList<String> vec = new ArrayList<String>();
		String file_line = "";

		String path = pathToFile;

		// Connect to the file
		InputStream is = null;
		BufferedReader dis = null;
		try
		{
			URLConnection Connection = ((new File(path)).toURI().toURL()).openConnection();
			is = Connection.getInputStream();
			dis = new BufferedReader(new InputStreamReader(is, charset));

			while ((file_line = dis.readLine()) != null)
			{
				// Store object in ArrayList
				vec.add(file_line);
			}

			// Close file
			dis.close();
			is.close();
			is = null;
			return vec;
		} catch (Exception e2)
		{
			try {
				if (dis != null) dis.close();
				if (is != null) is.close();
				is = null;
			} catch (Exception exc) {}
			throw new JPARSECException(
					"error while reading file " + pathToFile + ".", e2);
		}
	}

	/**
	 * Returns a {@linkplain BufferedReader} object to read a file line by line.
	 * @param pathToFile The path to the file.
	 * @param charset The charset of that file.
	 * @return The {@linkplain BufferedReader} object.
	 * @throws JPARSECException If an error occurs.
	 */
	public static BufferedReader getBufferedReaderToAnyExternalFile(String pathToFile, String charset) throws JPARSECException
	{
		// Connect to the file
		try
		{
			URLConnection Connection = ((new File(pathToFile)).toURI().toURL()).openConnection();
			InputStream is = Connection.getInputStream();
			BufferedReader dis = new BufferedReader(new InputStreamReader(is, charset));
			return dis;
		} catch (Exception e2) {
			throw new JPARSECException("error while reading file " + pathToFile + ".", e2);
		}
	}

	/**
	 * Counts the number of lines in an external file.
	 *
	 * @param path The path to the file.
	 * @return number of lines.
	 * @throws JPARSECException If an error occurs accessing the file.
	 */
	public static int readAnyExternalFileAndReturnNumberOfLines(String path) throws JPARSECException
	{
		// Define necessary variables
		int n = 0;

		// Connect to the file
		InputStream is = null;
		BufferedReader dis = null;
		try
		{
			URLConnection Connection = ((new File(path)).toURI().toURL()).openConnection();
			is = Connection.getInputStream();
		    byte[] buffer = new byte[8 * 1024]; // BUFFER_SIZE = 8 * 1024
		    int read;
		    char sep = '\n';
		    while ((read = is.read(buffer)) != -1) {
		        for (int i = 0; i < read; i++) {
		            if (buffer[i] == sep) n++;
		        }
		    }

		    is.close();
			is = null;

			return n;
		} catch (Exception e2)
		{
			try {
				if (dis != null) dis.close();
				if (is != null) is.close();
				is = null;
			} catch (Exception exc) {}
			throw new JPARSECException(
					"error while reading file " + path + ".", e2);
		}
	}

	/**
	 * Reads an external file and returns it contents as an array of strings, including
	 * only a given number of lines from the begining of the file.
	 *
	 * @param path The path to the file.
	 * @param n Numer of lines to read from the beginning of the file.
	 * @param encoding The encoding.
	 * @return Array of strings.
	 * @throws JPARSECException If an error occurs accessing the file.
	 */
	public static ArrayList<String> readAnyExternalFileFirstNlines(String path, int n, String encoding) throws JPARSECException
	{
		// Define necessary variables
		ArrayList<String> vec = new ArrayList<String>();
		String file_line = "";

		// Connect to the file
		InputStream is = null;
		BufferedReader dis = null;
		try
		{
			URLConnection Connection = ((new File(path)).toURI().toURL()).openConnection();
			is = Connection.getInputStream();
			dis = new BufferedReader(new InputStreamReader(is, encoding));

			while ((file_line = dis.readLine()) != null)
			{
				// Store object in ArrayList
				vec.add(file_line);
				if (vec.size() == n) break;
			}

			// Close file
			dis.close();
			is.close();
			is = null;

			return vec;
		} catch (Exception e2)
		{
			try {
				if (dis != null) dis.close();
				if (is != null) is.close();
				is = null;
			} catch (Exception exc) {}
			throw new JPARSECException(
					"error while reading file " + path + ".", e2);
		}
	}

	/**
	 * Reads a file and returns it contents as an array of strings, but only the last n
	 * lines of the file
	 *
	 * @param path The path to the file.
	 * @param n Numer of lines to read from the end of the file.
	 * @param encoding The encoding.
	 * @return Array of strings.
	 * @throws JPARSECException If an error occurs accessing the file.
	 */
	public static ArrayList<String> readAnyExternalFileLastNlines(String path, int n, String encoding) throws JPARSECException
	{
		// Define necessary variables
		ArrayList<String> vec = new ArrayList<String>();
		String file_line = "";

		// Connect to the file
		InputStream is = null;
		BufferedReader dis = null;
		try
		{
			URLConnection Connection = ((new File(path)).toURI().toURL()).openConnection();
			is = Connection.getInputStream();
			dis = new BufferedReader(new InputStreamReader(is, encoding));

			while ((file_line = dis.readLine()) != null)
			{
				// Store object in ArrayList
				vec.add(file_line);
				if (vec.size() > n) vec.remove(0);
			}

			// Close file
			dis.close();
			is.close();
			is = null;

			return vec;
		} catch (Exception e2)
		{
			try {
				if (dis != null) dis.close();
				if (is != null) is.close();
				is = null;
			} catch (Exception exc) {}
			throw new JPARSECException(
					"error while reading file " + path + ".", e2);
		}
	}

	/**
	 * Reads a file and returns it contents as an array of strings, returning only
	 * the lines in a given range within the file.
	 *
	 * @param path The path to the file.
	 * @param i0 First line to read (index, starting from 0).
	 * @param i1 Last line to read (index, starting from 0).
	 * @param encoding The encoding.
	 * @return Array of strings.
	 * @throws JPARSECException If an error occurs accessing the file or end of file is found before
	 * reading the lines.
	 */
	public static ArrayList<String> readAnyExternalFileSomeLines(String path, int i0, int i1, String encoding) throws JPARSECException
	{
		// Define necessary variables
		ArrayList<String> vec = new ArrayList<String>();
		String file_line = "";

		// Connect to the file
		InputStream is = null;
		BufferedReader dis = null;
		try
		{
			URLConnection Connection = ((new File(path)).toURI().toURL()).openConnection();
			is = Connection.getInputStream();
			dis = new BufferedReader(new InputStreamReader(is, encoding));

			for (int i=0; i<i0; i++) {
				file_line = dis.readLine();
				if (file_line == null) throw new JPARSECException("End of file found before reading the lines.");
			}
			for (int i=0; i<(i1-i0+1); i++) {
				file_line = dis.readLine();
				if (file_line == null) break; //throw new JPARSECException("End of file found before reading the lines.");
				vec.add(file_line);
			}

			// Close file
			dis.close();
			is.close();
			is = null;

			return vec;
		} catch (Exception e2)
		{
			try {
				if (dis != null) dis.close();
				if (is != null) is.close();
				is = null;
			} catch (Exception exc) {}
			throw new JPARSECException(
					"error while reading file " + path + ".", e2);
		}
	}

	/**
	 * Reads a file and returns it contents as an array of strings. Only lines
	 * containing a given record are returned.
	 *
	 * @param path The path to the file.
	 * @param record Something to search for in each line of the file.
	 * @param encoding The encoding.
	 * @return Array of strings.
	 * @throws JPARSECException If an error occurs accessing the file.
	 */
	public static ArrayList<String> readAnyExternalFileContaining(String path, String record, String encoding) throws JPARSECException
	{
		// Define necessary variables
		ArrayList<String> vec = new ArrayList<String>();
		String file_line = "";

		// Connect to the file
		InputStream is = null;
		BufferedReader dis = null;
		try
		{
			URLConnection Connection = ((new File(path)).toURI().toURL()).openConnection();
			is = Connection.getInputStream();
			dis = new BufferedReader(new InputStreamReader(is, encoding));

			while ((file_line = dis.readLine()) != null)
			{
				// Store object in ArrayList
				if (file_line.indexOf(record) >= 0) vec.add(file_line);
			}

			// Close file
			dis.close();
			is.close();
			is = null;

			return vec;
		} catch (Exception e2)
		{
			try {
				if (dis != null) dis.close();
				if (is != null) is.close();
				is = null;
			} catch (Exception exc) {}
			throw new JPARSECException(
					"error while reading file " + path + ".", e2);
		}
	}

	/**
	 * Reads an external file and returns it contents as an array of strings.
	 * @param pathToFile URL path to the file.
	 * @param charset The name of the charset used to encode the file. Can be NONE, UTF-8,
	 * ISO-8859-1, and so on.
	 * @return Array of strings.
	 * @throws JPARSECException If an error occurs accessing the resource.
	 */
	public static ArrayList<String> readAnyExternalFile(URL pathToFile, String charset) throws JPARSECException
	{
		// Define necesary variables
		ArrayList<String> vec = new ArrayList<String>();
		String file_line;

		// Connect to the file
		InputStream is = null;
		BufferedReader dis = null;
		try
		{
			URLConnection Connection = pathToFile.openConnection();
			is = Connection.getInputStream();
			dis = new BufferedReader(new InputStreamReader(is, charset));

			while ((file_line = dis.readLine()) != null)
			{
				// Store object in ArrayList
				vec.add(file_line);
			}

			// Close file
			dis.close();
			is.close();
			is = null;

			return vec;
		} catch (Exception e2)
		{
			try {
				if (dis != null) dis.close();
				if (is != null) is.close();
				is = null;
			} catch (Exception exc) {}
			throw new JPARSECException(
					"error while reading file " + pathToFile + ".", e2);
		}
	}

	/**
	 * Returns the size in bytes of the input stream.
	 * @param is Input stream.
	 * @return Size in bytes.
	 * @throws JPARSECException If an error occurs.
	 */
	public static long getSize(InputStream is) throws JPARSECException {
		try {
			byte[] buffer = new byte[8096];
			long size = 0;
			int chunk = 0;
			while((chunk = is.read(buffer)) != -1){
				size += chunk;
			}
		    is.close();
			is = null;

			return size;
		} catch (Exception e2) {
			try {
				if (is != null) is.close();
				is = null;
			} catch (Exception exc) {}
			throw new JPARSECException(e2);
		}
	}
	
	/**
	 * Returns the input stream of a resource.
	 *
	 * @return The input stream of the resource, or null if it is unavailable.
	 */
	public InputStream getResourceAsStream()
	{
		InputStream is = getClass().getClassLoader().getResourceAsStream(pathToFile);
		return is;
	}

	/**
	 * Returns the URL of a resource.
	 *
	 * @return The URL of the resource, or null if it is unavalilable.
	 */
	public URL getResourceAsURL()
	{
		URL url = getClass().getClassLoader().getResource(pathToFile);
		return url;
	}

	/**
	 * Reads a resource and returns it contents as an array of strings. Path is taken from
	 * the current established path in this instance, and this path should be a local resource
	 * available.
	 *
	 * @return Array of strings.
	 * @throws JPARSECException If an error occurs accessing the resource.
	 */
	private ArrayList<String> readResource() throws JPARSECException
	{
		// Define necessary variables
		ArrayList<String> vec = new ArrayList<String>();
		String file_line;

		// Connect to the file
		InputStream is = null;
		BufferedReader dis = null;
		try
		{
			is = getClass().getClassLoader().getResourceAsStream(pathToFile);
			dis = new BufferedReader(new InputStreamReader(is, encoding));

			while ((file_line = dis.readLine()) != null)
			{
				// Store object in ArrayList
				vec.add(file_line);
			}

			// Close file
			dis.close();
			is.close();
			is = null;

			return vec;
		} catch (Exception e2)
		{
			try {
				if (dis != null) dis.close();
				if (is != null) is.close();
				is = null;
			} catch (Exception exc) {}
			throw new JPARSECException(
					"error while reading file " + pathToFile + ".", e2);
		}
	}

	/**
	 * Counts the number of lines in a resource.
	 *
	 * @return number of lines.
	 * @throws JPARSECException If an error occurs accessing the resource.
	 */
	private int readResourceAndReturnNumberOfLines() throws JPARSECException
	{
		// Define necessary variables
		int n = 0;

	    InputStream is = null;
	    try {
			is = getClass().getClassLoader().getResourceAsStream(pathToFile);
			n = 8 * 1024;
		    char sep = '\n';
		    byte[] c = new byte[n]; // BUFFER_SIZE = 8 * 1024

	        int readChars = is.read(c);
	        if (readChars == -1) return 0; // bail out if nothing to read

	        // make it easy for the optimizer to tune this loop
	        int count = 0;
	        while (readChars == n) {
	            for (int i=0; i<n;) {
	                if (c[i++] == sep) ++count;
	            }
	            readChars = is.read(c);
	        }

	        // count remaining characters
	        while (readChars != -1) {
	            for (int i=0; i<readChars; ++i) {
	                if (c[i] == sep) ++count;
	            }
	            readChars = is.read(c);
	        }

		    is.close();
			is = null;

	        return count == 0 ? 1 : count;
		} catch (Exception e2) {
			try {
				if (is != null) is.close();
				is = null;
			} catch (Exception exc) {}
			throw new JPARSECException(
					"error while reading file " + pathToFile + ".", e2);
		}
	}

	/**
	 * Reads a resource and returns it contents as an array of strings. Path is taken from
	 * the current established path in this instance, and this path should be a local resource
	 * available.
	 *
	 * @param n Numer of lines to read from the beginning of the resource.
	 * @return Array of strings.
	 * @throws JPARSECException If an error occurs accessing the resource.
	 */
	private ArrayList<String> readResourceFirstNlines(int n) throws JPARSECException
	{
		// Define necessary variables
		ArrayList<String> vec = new ArrayList<String>();
		String file_line = "";

		// Connect to the file
		InputStream is = null;
		BufferedReader dis = null;
		try
		{
			is = getClass().getClassLoader().getResourceAsStream(pathToFile);
			dis = new BufferedReader(new InputStreamReader(is, encoding));

			while ((file_line = dis.readLine()) != null)
			{
				// Store object in ArrayList
				vec.add(file_line);
				if (vec.size() == n) break;
			}

			// Close file
			dis.close();
			is.close();
			is = null;

			return vec;
		} catch (Exception e2)
		{
			try {
				if (dis != null) dis.close();
				if (is != null) is.close();
				is = null;
			} catch (Exception exc) {}
			throw new JPARSECException(
					"error while reading file " + pathToFile + ".", e2);
		}
	}

	/**
	 * Reads a resource and returns it contents as an array of strings. Path is taken from
	 * the current established path in this instance, and this path should be a local resource
	 * available.
	 *
	 * @param n Numer of lines to read from the end of the resource.
	 * @return Array of strings.
	 * @throws JPARSECException If an error occurs accessing the resource.
	 */
	private ArrayList<String> readResourceLastNlines(int n) throws JPARSECException
	{
		int nlines = readResourceAndReturnNumberOfLines();
		return readResourceSomeLines(nlines-n, nlines-1);
	}

	/**
	 * Reads a resource and returns it contents as an array of strings. Path is taken from
	 * the current established path in this instance, and this path should be a local resource
	 * available.
	 *
	 * @param i0 First line to read (index, starting from 0).
	 * @param i1 Last line to read (index, starting from 0).
	 * @return Array of strings. Empty in case the range is out from the file contents.
	 * @throws JPARSECException If an error occurs accessing the resource.
	 */
	private ArrayList<String> readResourceSomeLines(int i0, int i1) throws JPARSECException
	{
		// Define necessary variables
		ArrayList<String> vec = new ArrayList<String>();
		String file_line = "";

		// Connect to the file
		InputStream is = null;
		BufferedReader dis = null;
		try
		{
			is = getClass().getClassLoader().getResourceAsStream(pathToFile);
			dis = new BufferedReader(new InputStreamReader(is, encoding));

			for (int i=0; i<i0; i++) {
				file_line = dis.readLine();
				if (file_line == null) {
					dis.close();
					is.close();
					return vec;
				}
			}
			for (int i=0; i<(i1-i0+1); i++) {
				file_line = dis.readLine();
				if (file_line == null) break;
				vec.add(file_line);
			}

			// Close file
			dis.close();
			is.close();
			is = null;

			return vec;
		} catch (Exception e2)
		{
			try {
				if (dis != null) dis.close();
				if (is != null) is.close();
				is = null;
			} catch (Exception exc) {}
			throw new JPARSECException(
					"error while reading file " + pathToFile + ".", e2);
		}
	}

	/**
	 * Reads a resource and returns it contents as an array of strings. Path is taken from
	 * the current established path in this instance, and this path should be a local resource
	 * available. Only lines containing a given record are returned.
	 *
	 * @param record Something to search for in each line of the file.
	 * @return Array of strings.
	 * @throws JPARSECException If an error occurs accessing the resource.
	 */
	private ArrayList<String> readResourceContaining(String record) throws JPARSECException
	{
		// Define necessary variables
		ArrayList<String> vec = new ArrayList<String>();
		String file_line = "";

		// Connect to the file
		InputStream is = null;
		BufferedReader dis = null;
		try
		{
			is = getClass().getClassLoader().getResourceAsStream(pathToFile);
			dis = new BufferedReader(new InputStreamReader(is, encoding));

			while ((file_line = dis.readLine()) != null)
			{
				// Store object in ArrayList
				if (file_line.indexOf(record) >= 0) vec.add(file_line);
			}

			// Close file
			dis.close();
			is.close();
			is = null;

			return vec;
		} catch (Exception e2)
		{
			try {
				if (dis != null) dis.close();
				if (is != null) is.close();
				is = null;
			} catch (Exception exc) {}
			throw new JPARSECException(
					"error while reading file " + pathToFile + ".", e2);
		}
	}

	private static String lastNOAAdata = null;
	private static double lastNOAAdate = -1;
	private static EphemElement lastSun = null;
	/**
	 * Resets the data already read for current solar spots from NOAA. This allows
	 * them to be updated again anytime.
	 */
	public static void resetNOAAcurrentSolarSpots() {
		lastNOAAdata = null;
		lastNOAAdate = -1;
		lastSun = null;
	}

	/**
	 * Reads GREENWICH - NOAA solar spots database. A ArrayList with string arrays
	 * is returned, with the following fields: group number, group type,
	 * heliographic longitude, latitude, and area in solar disk units. Position
	 * is in radians.
	 * In case no data is found in the database the method will attempt to download
	 * updated data for the spots currently visible from the URL
	 * http://services.swpc.noaa.gov/text/solar-regions.txt. In case this fails it will
	 * attempt again only three days later. If success they will be updated again three
	 * days after. Use the method {@linkplain #resetNOAAcurrentSolarSpots} to updated
	 * them again anytime.
	 *
	 * @param jd Julian day of interest.
	 * @return Array of string with the fields.
	 * @throws JPARSECException If an error occurs.
	 */
	public static ArrayList<String[]> readFileOfSunSpots(double jd) throws JPARSECException
	{
		ArrayList<String[]> out = new ArrayList<String[]>();

		AstroDate astro = new AstroDate(jd);
		int year = astro.getYear();
		String fileName = "g" + year + ".txt";

		ReadFormat rf = new ReadFormat();
		boolean error = false;

		try
		{
			if (year >= 2016) {
				// DPD Catalog at http://fenyi.solarobs.csfk.mta.hu/DPD/
				// Baranyi+ 2016, https://arxiv.org/abs/1606.00669
				// Gyori+ 2017, https://arxiv.org/pdf/1612.03274v1.pdf
				FileFormatElement DEBRECEN_PHOTOH_DATA[] = {
						new FileFormatElement(22, 28, "NOAA_GROUP_NUMBER"),
						new FileFormatElement(30, 34, "SPOT_NUMBER_IN_GROUP"),
						new FileFormatElement(42, 46, "SPOT_AREA_NOT_CORRECTED"),
						new FileFormatElement(54, 58, "SPOT_AREA"),
						new FileFormatElement(36, 40, "UMBRAL_AREA_NOT_CORRECTED"),
						new FileFormatElement(48, 52, "UMBRAL_AREA"),
						new FileFormatElement(60, 65, "LATITUDE"),
						new FileFormatElement(67, 72, "LONGITUDE"),
						new FileFormatElement(41, 44, "SPOT_AREA"), 
						new FileFormatElement(88, 93, "DISTANCE_TO_CENTRE"),
						new FileFormatElement(81, 86, "POSITION_ANGLE"), 
						new FileFormatElement(74, 79, "CENTRAL_MERIDIAN_DISTANCE"),
				};

				fileName = "DPD"+year+".txt";
				ReadFile rfile = new ReadFile(FileIO.DATA_SUNSPOT_DIRECTORY + fileName, ReadFile.ENCODING_ISO_8859);
				rf.setFormatToRead(DEBRECEN_PHOTOH_DATA);
				String mo = DateTimeOps.twoDigits(astro.getMonth());
				String da = DateTimeOps.twoDigits(astro.getMonth());
				ArrayList<String> v = rfile.readResourceContaining("s "+year+" "+mo+" "+da+" ");
				for (int i = 0; i < v.size(); i++) {
					String line = v.get(i);
					
					double area = rf.readDouble(line.trim(), "SPOT_AREA") / 1000000.0;
					if (area < 0) area = -area;
					String group = rf.readString(line, "NOAA_GROUP_NUMBER").trim();
					String type = rf.readString(line, "SPOT_NUMBER_IN_GROUP").trim();
					double lon = rf.readDouble(line, "LONGITUDE") * Constant.DEG_TO_RAD;
					double lat = rf.readDouble(line, "LATITUDE") * Constant.DEG_TO_RAD;
					String record[] = new String[] {group, type, "" + lon, "" + lat, "" + area};
					out.add(record);					
				}				
			} else {
				ReadFile rfile = new ReadFile(FileIO.DATA_SUNSPOT_DIRECTORY + fileName, ReadFile.ENCODING_ISO_8859);
				ArrayList<String> v = rfile.readResource();
	
				int nlines = v.size();
				rf.setFormatToRead(FileFormatElement.NOAA_GREENWICH_SOLAR_SPOTS);
				String groupLabel = "NOAA";
				if (astro.getYear() < 1976)
					groupLabel = "GREENWICH";
				for (int i = 0; i < nlines; i++)
				{
					String line = v.get(i);
	
					int month = rf.readInteger(line, "MONTH");
					int day = rf.readInteger(line, "DAY");
					if (month == astro.getMonth() && day == astro.getDay())
					{
						String gn = rf.readString(line, "GROUP_NUMBER");
						if (gn.length() > 1) gn = gn.substring(1);
						String group = groupLabel + gn;
						String type = rf.readString(line, "GREENWICH_GROUP_TYPE");
						if (astro.getYear() > 1981)
							type = rf.readString(line, "NOAA_GROUP_TYPE");
						double area = rf.readDouble(line, "SPOT_AREA") / 1000000.0;
						double lon = rf.readDouble(line, "LONGITUDE") * Constant.DEG_TO_RAD;
						double lat = rf.readDouble(line, "LATITUDE") * Constant.DEG_TO_RAD;
	
						String record[] = new String[]
						{ group, type, "" + lon, "" + lat, "" + area };
						out.add(record);
					}
				}
			}
		} catch (Exception e)
		{
			error = true;
		}

		if (out.size() == 0 && year > 2010 && Configuration.QUERY_TIMEOUT > 0) {
			// Try to retrieve real time solar spots
			try {
				String data = null;
				if (lastNOAAdate > 0 && Math.abs(lastNOAAdate-jd) < 3 && lastNOAAdata != null) {
					data = lastNOAAdata;
				} else {
					String link = "https://services.swpc.noaa.gov/text/solar-regions.txt";
					data = GeneralQuery.query(link, Configuration.QUERY_TIMEOUT);
					lastSun = null;
				}
				if (!data.equals("")) {
					String lines[] = DataSet.toStringArray(data, FileIO.getLineSeparator());
					int idate = DataSet.getIndexStartingWith(lines, ":Solar_Region_Summary");
					if (idate >= 0) {
						int idate1 = lines[idate].lastIndexOf(":");
						String date = lines[idate].substring(idate1+1).trim();
						int myyear = Integer.parseInt(FileIO.getField(1, date, " ", true));
						int myday = Integer.parseInt(FileIO.getField(3, date, " ", true));
						int mymonth = 1 + DataSet.getIndexStartingWith(AstroDate.MONTH_NAMES, FileIO.getField(2, date, " ", true));
	
						AstroDate astrodate = new AstroDate(myyear, mymonth, myday);
	
						double dif = Math.abs(astrodate.jd()-jd);
						if (dif < 3) {
							lastNOAAdate = astrodate.jd();
							lastNOAAdata = data;
	
							int inum = DataSet.getIndexStartingWith(lines, "# Num");
							if (lastSun == null) {
								EphemerisElement eph = new EphemerisElement(TARGET.SUN, EphemerisElement.COORDINATES_TYPE.APPARENT,
										EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.GEOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
										EphemerisElement.FRAME.DYNAMICAL_EQUINOX_J2000);
								TimeElement time = new TimeElement(astrodate.jd()+1, SCALE.UNIVERSAL_TIME_UTC);
								ObserverElement obs = new ObserverElement();
								lastSun = Ephem.getEphemeris(time, obs, eph, false);
							}
	
							for (int i=inum+1; i<lines.length; i++) {
								String fields[] = DataSet.toStringArray(lines[i], " ", true);
								if (fields.length < 8) continue;
								
								String group = "NOAA" + fields[0];
								String type = fields[7];
								double area = Double.parseDouble(fields[3]) / 1000000.0;
	
								fields[1] = DataSet.replaceAll(fields[1], "*", "", true);
								double lon = -Double.parseDouble(fields[1].substring(4));
								double lat = Double.parseDouble(fields[1].substring(1, 3));
								if (fields[1].substring(3).startsWith("W")) lon = -lon;
								if (fields[1].startsWith("S")) lat = -lat;
								lon *= Constant.DEG_TO_RAD;
								lat *= Constant.DEG_TO_RAD;
								lon += lastSun.longitudeOfCentralMeridian;
	
								String record[] = new String[] { group, type, "" + lon, "" + lat, "" + area };
								out.add(record);
							}
						}
					}
				}
			} catch (Exception exc) {
				exc.printStackTrace();
				lastNOAAdate = jd;
				lastNOAAdata = "";
			}

			if (error && out.size() == 0)
				throw new JPARSECException("could not read sun spots file.");
		}

		return out;
	}

	/**
	 * Reads the catalog of orbits of visual binary stars (Hartkopf 2010).
	 * After the file is successfully read, the objects are stored using
	 * {@linkplain DataBase} with the identifier returned by
	 * {@linkplain #getDataBaseID()}.
	 *
	 * @throws JPARSECException If an error occurs..
	 */
	public void readFileOfDoubleStars() throws JPARSECException
	{
		// Define necesary variables
		ArrayList<Object> vec = new ArrayList<Object>();
		String file_line = "";

		// Connect to the file
		InputStream is = null;
		BufferedReader dis = null;
		try
		{
			is = getClass().getClassLoader().getResourceAsStream(pathToFile);
			dis = new BufferedReader(new InputStreamReader(is, encoding));

			for (int i = 0; i < 7; i++) {
				file_line = dis.readLine();
			}
			while ((file_line = dis.readLine()) != null)
			{
				// Obtain fields
				DoubleStarElement orbit = parseDoubleStar(file_line);

				// Store object in ArrayList
				if (orbit != null && satisfyConstraints(orbit.name, (consLoc == null) ? null : orbit.getEquatorialPosition())) vec.add(orbit);
			}

			// Close file
			dis.close();
			is.close();
			is = null;

			this.setReadElements(vec);
		} catch (Exception e2)
		{
			try {
				if (dis != null) dis.close();
				if (is != null) is.close();
				is = null;
			} catch (Exception exc) {}
			throw new JPARSECException(
					"error while reading file " + pathToFile + ".", e2);
		}
	}

	/**
	 * Reads the old catalog of orbits of visual binary stars (Worley 1983).
	 * After the file is successfully read, the elements are stored using
	 * {@linkplain DataBase} with the identifier returned by
	 * {@linkplain #getDataBaseID()}.
	 *
	 * @throws JPARSECException If an error occurs..
	 */
	public void readOldFileOfDoubleStars() throws JPARSECException
	{
		// Define necesary variables
		ArrayList<Object> vec = new ArrayList<Object>();
		String file_line = "";

		// Connect to the file
		InputStream is = null;
		BufferedReader dis = null;
		try
		{
			is = getClass().getClassLoader().getResourceAsStream(pathToFile);
			dis = new BufferedReader(new InputStreamReader(is, encoding));

			while ((file_line = dis.readLine()) != null)
			{
				// Obtain fields
				DoubleStarElement orbit = parseOldDoubleStar(file_line);

				// Store object in ArrayList
				if (orbit != null && satisfyConstraints(orbit.name, (consLoc == null) ? null : orbit.getEquatorialPosition())) vec.add(orbit);
			}

			// Close file
			dis.close();
			is.close();
			is = null;

			this.setReadElements(vec);
		} catch (Exception e2)
		{
			try {
				if (dis != null) dis.close();
				if (is != null) is.close();
				is = null;
			} catch (Exception exc) {}
			throw new JPARSECException(
					"error while reading file " + pathToFile + ".", e2);
		}
	}

	/**
	 * Reads the catalog of variable stars (from AAVSO or from http://www.as.up.krakow.pl/).
	 * After the file is successfully read, the elements are stored using
	 * {@linkplain DataBase} with the identifier returned by
	 * {@linkplain #getDataBaseID()}.
	 *
	 * @throws JPARSECException If an error occurs..
	 */
	public void readFileOfVariableStars() throws JPARSECException
	{
		// Define necesary variables
		ArrayList<Object> vec = new ArrayList<Object>();
		String file_line = "";

		boolean aavso = true;
		if (pathToFile.equals(VariableStarElement.PATH_VARIABLE_STAR_CATALOG)) aavso = false;

		int year = 0;
		if (aavso) {
			int i0 = pathToFile.lastIndexOf("bulletin");
			int i1 = pathToFile.lastIndexOf(".csv");
			year = Integer.parseInt(pathToFile.substring(i0 + 8, i1));
		}

		InputStream is = null;
		BufferedReader dis = null;
		try
		{
			is = getClass().getClassLoader().getResourceAsStream(pathToFile);
			dis = new BufferedReader(new InputStreamReader(is, encoding));

			int imax = 1;
			if (!aavso) imax = 15;
			for (int i=0; i<imax; i++) {
				file_line = dis.readLine();
			}
			String file_line2 = null;
			while ((file_line = dis.readLine()) != null)
			{
				if (!aavso) file_line2 = dis.readLine();
				// Obtain fields
				VariableStarElement var = parseVariableStar(file_line, file_line2, year);

				// Store object in ArrayList
				if (var != null && satisfyConstraints(var.name, (consLoc == null) ? null : var.getEquatorialPosition())) vec.add(var);
			}

			// Close file
			dis.close();
			is.close();
			is = null;

			this.setReadElements(vec);
		} catch (Exception e2)
		{
			try {
				if (dis != null) dis.close();
				if (is != null) is.close();
				is = null;
			} catch (Exception exc) {}
			throw new JPARSECException(
					"error while reading file " + pathToFile + ".", e2);
		}
	}
	
	private boolean satisfyConstraints(String name, LocationElement loc) {
		if (consName == null && consLoc == null) return true;
		if (name != null && consName != null) {
			if (name.indexOf(consName) < 0) return false;
		}
		if (loc != null && consLoc != null && consRadius > 0) {
			double d = LocationElement.getAngularDistance(loc, consLoc);
			if (d > consRadius) return false;
		}
		return true;
	}
}
