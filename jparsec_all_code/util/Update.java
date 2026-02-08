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
package jparsec.util;

import jparsec.ephem.Target.TARGET;
import jparsec.ephem.moons.MoonEphem;
import jparsec.ephem.planets.OrbitEphem;
import jparsec.ephem.probes.SatelliteEphem;
import jparsec.graph.DataSet;
import jparsec.io.CatalogRead;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.io.WriteFile;
import jparsec.io.Zip;
import jparsec.observer.EarthOrientationParameters;
import jparsec.observer.Observatory;
import jparsec.time.AstroDate;
import jparsec.util.Logger.LEVEL;
import jparsec.vo.GeneralQuery;

/**
 * A class to automatically update JPARSEC package through Internet.<BR><BR>
 *
 * Updates depends on the availability of data, that could change from time
 * to time. The format could also change, so both the url constants and the
 * methods are subject to change.<BR><BR>
 *
 * All the information that is subject to change can be automatically updated
 * through Internet, with some minor exceptions where data is not available in
 * a standard form. The exceptions are as follows:<BR><BR>
 *
 * 1. Leap seconds and TT-UT1 values in file time.jar. Can be updated by hand.<BR>
 * 2. Orbital elements of space probes in orbital_elements.jar, taken from Horizons.
 * Last full update in October, 2011.<BR>
 * 3. Values of the Great Red Spot longitudes, in orbital_elements.jar, to be updated
 * each three months from the ALPO website or Sky &amp; Telescope.
 * This file is the same as that used by Kerry Shetline in Sky View Cafe.<BR>
 *
 * Some processing is needed to update the list of observatories and
 * the COLOGNE database, due to the usage of HTML format instead of a more
 * simple and desirable raw text. Anyway, this is done automatically.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Update
{
	// private constructor so that this class cannot be instantiated.
	private Update() {}

	/**
	 * The url used to update the Padova-Asiago SN catalogue.
	 */
	public static final String UPDATE_URL_PADOVA_ASIAGO_SN_CAT = "http://graspa.oapd.inaf.it/asnc/cat.txt";
	/**
	 * The url used to update the galactic novae SN catalogue.
	 */
	public static final String UPDATE_URL_NOVAE = "https://projectpluto.com/galnovae/galnovae.txt";
	/**
	 * The url used to update sizes and magnitudes or artificial satellites.
	 */
	public static final String UPDATE_URL_ARTIFICIAL_SATELLITES_SIZE_AND_MAGNITUDE = "https://www.prismnet.com/~mmccants/programs/qsmag.zip";
	/**
	 * The url used to update orbital elements of natural satellites.
	 */
	public static final String UPDATE_URL_NATURAL_SATELLITES = "https://ssd.jpl.nasa.gov/?sat_elem";
	/**
	 * The url used to update orbital elements of comets in MPC format.
	 * The url astro.vanbuitenen.nl is a source for elements with a much better estimate of comet magnitudes.
	 */
	public static final String UPDATE_URL_COMETS = "http://astro.vanbuitenen.nl/cometelements?format=mpc&mag=obs"; //"https://minorplanetcenter.net/iau/Ephemerides/Comets/Soft00Cmt.txt";
	/**
	 * The url used to update orbital elements of trans-Neptunian objects.
	 */
	public static final String UPDATE_URL_DISTANT_BODIES = "https://minorplanetcenter.net/iau/Ephemerides/Distant/Soft00Distant.txt";
	/**
	 * The url used to update orbital elements of bright asteroids.
	 */
	public static final String UPDATE_URL_BRIGHT_ASTEROIDS = "https://minorplanetcenter.net/iau/Ephemerides/Bright/2007/Soft00Bright.txt";
	/**
	 * The url used to update orbital elements of artificial satellites.
	 */
	public static final String UPDATE_URL_VISUAL_ARTIFICIAL_SATELLITES = "http://www.celestrak.com/NORAD/elements/visual.txt";
	/**
	 * The url used to update orbital elements of iridium artificial satellites.
	 */
	public static final String UPDATE_URL_VISUAL_ARTIFICIAL_SATELLITES_IRIDIUM = "http://www.celestrak.com/NORAD/elements/iridium.txt";
	/**
	 * The url used to update orbital elements of visual binary stars.
	 */
	public static final String UPDATE_URL_ORBITS_VISUAL_BINARY_STARS = "http://ad.usno.navy.mil/wds/orb6/orb6orbits.txt";
	/**
	 * The url used to update the official list of observatories.
	 */
	public static final String UPDATE_URL_OBSERVATORIES = "https://www.minorplanetcenter.net/iau/lists/ObsCodes.html";

	/**
	 * The url used to update IAU1980 Earth Orientation Parameters.
	 */
	public static final String UPDATE_URL_EOP_IAU1980 = "http://hpiers.obspm.fr/iers/eop/eopc04/eopc04.62-now";
	/**
	 * The url used to update IAU2000 Earth Orientation Parameters.
	 */
	public static final String UPDATE_URL_EOP_IAU2000 = "http://hpiers.obspm.fr/iers/eop/eopc04/eopc04_IAU2000.62-now";

	/**
	 * The url used to update Sun spots database.
	 */
	public static final String UPDATE_URL_SUN_SPOTS = "https://solarscience.msfc.nasa.gov/greenwch/";

	/**
	 * The url used to update JPL catalogue of molecular spectroscopy.
	 */
	public static final String UPDATE_URL_JPL_CATALOGUE = "http://spec.jpl.nasa.gov/ftp/pub/catalog/";

	/**
	 * The url used to update COLOGNE catalogue of molecular spectroscopy.
	 */
	public static final String UPDATE_URL_COLOGNE_CATALOGUE = "http://www.astro.uni-koeln.de/site/vorhersagen/catalog/";

	/**
	 * Updates the orbital elements .jar file by querying Minor Planet
	 * Center for updated files for comets, and bright and distant
	 * asteroids. Only MPC-style formatted files are updated. This method
	 * also updates the list of observatories and the orbital elements
	 * of artificial satellites.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void updateOrbitalElementsFromMPC()
	throws JPARSECException {
		Logger.log(LEVEL.TRACE_LEVEL2, "Updating orbital elements from MPC");

		// Query the files
		String query1 = Update.UPDATE_URL_COMETS;
		String file1 = GeneralQuery.query(query1);
		String query2 = Update.UPDATE_URL_DISTANT_BODIES;
		String file2 = GeneralQuery.query(query2);

		String query3 = Update.UPDATE_URL_BRIGHT_ASTEROIDS;
		//AstroDate astro = new AstroDate();
		//query3 = DataSet.replaceAll(query3, "2007", ""+astro.getYear(), false);
		
		// FIXME: MPC does not update asteroids since 2018, and Pallas and others are only listed in 2017
		// It is recommended to filter the complete database at https://minorplanetcenter.net/iau/MPCORB.html
		query3 = DataSet.replaceAll(query3, "2007", "2018", false);
		String file3 = null;
		try {
			file3 = GeneralQuery.query(query3);

			try {
				query3 = Update.UPDATE_URL_BRIGHT_ASTEROIDS;
				query3 = DataSet.replaceAll(query3, "2007", "2017", false);
				String file3b = GeneralQuery.query(query3);

				file3 = addAsteroids(file3, file3b);
			} catch (Exception exc) {}
		} catch (Exception exc) {}


		String query4a = Update.UPDATE_URL_VISUAL_ARTIFICIAL_SATELLITES;
		//String query4b = Update.UPDATE_URL_VISUAL_ARTIFICIAL_SATELLITES_2;
		String file4 = GeneralQuery.query(query4a);
		//file4 += GeneralQuery.query(query4b);
		String file4Iridium = GeneralQuery.query(Update.UPDATE_URL_VISUAL_ARTIFICIAL_SATELLITES_IRIDIUM);
		String query5 = Update.UPDATE_URL_OBSERVATORIES;
		String file5 = GeneralQuery.query(query5);
//		String query6 = "http://www.tle.info/data/science.txt";
//		String file6 = GeneralQuery.query(query6);
//		file4 += file6; // Not a good idea since some satellites will be twice
		String query7 = Update.UPDATE_URL_COMETS;
		query7 = DataSet.replaceAll(query7, "Soft00", "Soft01", false);
		//String file7 = GeneralQuery.query(query1);
		String query8 = Update.UPDATE_URL_DISTANT_BODIES;
		query8 = DataSet.replaceAll(query8, "Soft00", "Soft01", false);
		//String file8 = GeneralQuery.query(query2);

		// Continue only if the files seems to be correctly retrieved
		if (file1 == null) throw new JPARSECException("no response from url "+query1+".");
		if (file2 == null) throw new JPARSECException("no response from url "+query2+".");
		if (file3 == null) throw new JPARSECException("no response from url "+query3+".");
		if (file4.equals("") || file4 == null) throw new JPARSECException("no response from url "+query4a+"."); ///"+query4b+".");
		if (file4Iridium == null) throw new JPARSECException("no response from url "+Update.UPDATE_URL_VISUAL_ARTIFICIAL_SATELLITES_IRIDIUM+".");
		if (file5 == null) throw new JPARSECException("no response from url "+query5+".");

		if (file1.length() < 200) throw new JPARSECException("the response from url "+query1+" ("+file1+") seems to be invalid.");
		if (file2.length() < 200) throw new JPARSECException("the response from url "+query2+" ("+file2+") seems to be invalid.");
		if (file3.length() < 200) throw new JPARSECException("the response from url "+query3+" ("+file3+") seems to be invalid.");
		if (file4.length() < 200) throw new JPARSECException("the response from url "+query4a+" ("+file4+") seems to be invalid.");
		if (file4Iridium.length() < 200) throw new JPARSECException("the response from url "+Update.UPDATE_URL_VISUAL_ARTIFICIAL_SATELLITES_IRIDIUM+" ("+file4Iridium+") seems to be invalid.");
		if (file5.length() < 200) throw new JPARSECException("the response from url "+query5+" ("+file5+") seems to be invalid.");

		// Create temporal directory
		String path = Update.JPARSEC_TEMP_DIRECTORY+Zip.ZIP_SEPARATOR+FileIO.DATA_ORBITAL_ELEMENTS_DIRECTORY;
		path = DataSet.replaceAll(path, Zip.ZIP_SEPARATOR, FileIO.getFileSeparator(), true);
		Logger.log(LEVEL.TRACE_LEVEL1, "   Creating temp dir "+path);
		boolean success = (new java.io.File(path)).mkdirs();
		if (!success) throw new JPARSECException("cannot create directory "+path);

		// Unzip old files from orbital_elements.jar to a temp folder
		String pathToJar = FileIO.getPath(true) + FileIO.DATA_ORBITAL_ELEMENTS_JARFILE+".jar";
		Logger.log(LEVEL.TRACE_LEVEL1, "   Unzipping file "+pathToJar);
		Zip.unZipFile(pathToJar, Update.JPARSEC_TEMP_DIRECTORY, false);

		// Process file 5
		Logger.log(LEVEL.TRACE_LEVEL1, "   Processing observatories file "+file5);
		String array[] = jparsec.graph.DataSet.toStringArray(file5, FileIO.getLineSeparator());
		file5 = "";
		int init = 0;
		for (int i=0; i<array.length; i++)
		{
			if (array[i].indexOf("</pre>") >= 0) {
				for (int j=init; j<i; j++)
				{
					file5 += array[j] + FileIO.getLineSeparator();
				}
			}
			if (array[i].indexOf("<pre>") >= 0) {
				init = i + 2;
			}
		}

		// Replace old files with downloaded ones
		String fileName = OrbitEphem.PATH_TO_MPC_COMETS_FILE.substring(OrbitEphem.PATH_TO_MPC_COMETS_FILE.lastIndexOf(Zip.ZIP_SEPARATOR) + 1);
		WriteFile.writeAnyExternalFile(path+FileIO.getFileSeparator()+fileName, file1);
		Logger.log(LEVEL.TRACE_LEVEL1, "   New MPC comets file: "+FileIO.getLineSeparator() + file1);
		fileName = OrbitEphem.PATH_TO_MPC_DISTANT_BODIES_FILE.substring(OrbitEphem.PATH_TO_MPC_DISTANT_BODIES_FILE.lastIndexOf(Zip.ZIP_SEPARATOR) + 1);
		WriteFile.writeAnyExternalFile(path+FileIO.getFileSeparator()+fileName, file2);
		Logger.log(LEVEL.TRACE_LEVEL1, "   New MPC distant bodies file: "+FileIO.getLineSeparator() + file2);
		fileName = OrbitEphem.PATH_TO_MPC_BRIGHT_ASTEROIDS_FILE.substring(OrbitEphem.PATH_TO_MPC_BRIGHT_ASTEROIDS_FILE.lastIndexOf(Zip.ZIP_SEPARATOR) + 1);
		WriteFile.writeAnyExternalFile(path+FileIO.getFileSeparator()+fileName, file3);
		Logger.log(LEVEL.TRACE_LEVEL1, "   New MPC bright asteroids file: "+FileIO.getLineSeparator() + file3);
		fileName = SatelliteEphem.PATH_TO_SATELLITES_FILE.substring(SatelliteEphem.PATH_TO_SATELLITES_FILE.lastIndexOf(Zip.ZIP_SEPARATOR) + 1);
		WriteFile.writeAnyExternalFile(path+FileIO.getFileSeparator()+fileName, file4);
		Logger.log(LEVEL.TRACE_LEVEL1, "   New satellites file: "+FileIO.getLineSeparator() + file4);
		fileName = SatelliteEphem.PATH_TO_SATELLITES_IRIDIUM_FILE.substring(SatelliteEphem.PATH_TO_SATELLITES_IRIDIUM_FILE.lastIndexOf(Zip.ZIP_SEPARATOR) + 1);
		WriteFile.writeAnyExternalFile(path+FileIO.getFileSeparator()+fileName, file4Iridium);
		Logger.log(LEVEL.TRACE_LEVEL1, "   New iridium satellites file: "+FileIO.getLineSeparator() + file4Iridium);
		fileName = Observatory.PATH_TO_OFFICIAL_LIST_OF_OBSERVATORIES_FROM_MPC.substring(Observatory.PATH_TO_OFFICIAL_LIST_OF_OBSERVATORIES_FROM_MPC.lastIndexOf(Zip.ZIP_SEPARATOR) + 1);
		WriteFile.writeAnyExternalFile(path+FileIO.getFileSeparator()+fileName, file5);
		Logger.log(LEVEL.TRACE_LEVEL1, "   New MPC observatories file: "+FileIO.getLineSeparator() + file5);

/*		fileName = OrbitEphem.PATH_TO_SKYMAP_COMETS_FILE.substring(OrbitEphem.PATH_TO_SKYMAP_COMETS_FILE.lastIndexOf(Zip.ZIP_SEPARATOR) + 1);
		WriteFile.writeAnyExternalFile(path+FileIO.getFileSeparator()+fileName, file7);
		Logger.log(LEVEL.TRACE_LEVEL1, "   New SKYMAP comets file: "+FileIO.getLineSeparator() + file7);
		fileName = OrbitEphem.PATH_TO_SKYMAP_DISTANT_BODIES_FILE.substring(OrbitEphem.PATH_TO_SKYMAP_DISTANT_BODIES_FILE.lastIndexOf(Zip.ZIP_SEPARATOR) + 1);
		WriteFile.writeAnyExternalFile(path+FileIO.getFileSeparator()+fileName, file8);
		Logger.log(LEVEL.TRACE_LEVEL1, "   New SKYMAP distant bodies file: "+FileIO.getLineSeparator() + file8);
*/
		// Zip new folder
		pathToJar = FileIO.getPath(true) + FileIO.DATA_ORBITAL_ELEMENTS_JARFILE+".jar";
		Logger.log(LEVEL.TRACE_LEVEL1, "   Zipping new file: "+pathToJar);
		Zip.zipDirectory(pathToJar, Update.JPARSEC_TEMP_DIRECTORY+FileIO.getFileSeparator()+Version.PACKAGE_NAME.toLowerCase());

		// Delete temp folder. Note that compressed files/directories are automatically
		// deleted during the compression process.
		Logger.log(LEVEL.TRACE_LEVEL1, "   Deleting temp dir");
		FileIO.deleteFile(Update.JPARSEC_TEMP_DIRECTORY);

	}

	private static String addAsteroids(String f1, String f2) throws JPARSECException {
		String d1[] = DataSet.toStringArray(f1, FileIO.getLineSeparator());
		String d2[] = DataSet.toStringArray(f2, FileIO.getLineSeparator());
		String n1[] = new String[d1.length], n2[] = new String[d2.length];
		for (int i=0; i<n1.length; i++) {
			n1[i] = d1[i].substring(179).trim();
		}
		for (int i=0; i<n2.length; i++) {
			n2[i] = d2[i].substring(179).trim();
		}
		for (int i=n2.length-1; i>=0; i--) {
			int index = DataSet.getIndex(n1, n2[i]);
			if (index >= 0) {
				n2 = DataSet.eliminateRowFromTable(n2, i + 1);
				d2 = DataSet.eliminateRowFromTable(d2, i + 1);
			}
		}

		if (d2.length > 0) return f1.trim() + FileIO.getLineSeparator() + DataSet.toString(d2, FileIO.getLineSeparator());
		return f1.trim();
	}

	/**
	 * Updates the EOP parameters from Internet.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void updateEOPparameters()
	throws JPARSECException {
		Logger.log(LEVEL.TRACE_LEVEL2, "Updating EOP");

		// Query the files
		String query1 = Update.UPDATE_URL_EOP_IAU1980;
		String query2 = Update.UPDATE_URL_EOP_IAU2000;

		// Create temporal directory
		String path = Update.JPARSEC_TEMP_DIRECTORY+Zip.ZIP_SEPARATOR+FileIO.DATA_EOP_DIRECTORY;
		path = DataSet.replaceAll(path, Zip.ZIP_SEPARATOR, FileIO.getFileSeparator(), true);
		Logger.log(LEVEL.TRACE_LEVEL1, "   Creating temp dir "+path);
		boolean success = (new java.io.File(path)).mkdirs();
		if (!success) throw new JPARSECException("cannot create directory "+path);

		// Unzip old files from eop.jar to a temp folder
		String pathToJar = FileIO.getPath(true) + FileIO.DATA_EOP_JARFILE+".jar";
		Logger.log(LEVEL.TRACE_LEVEL1, "   Unzipping file "+pathToJar);
		Zip.unZipFile(pathToJar, Update.JPARSEC_TEMP_DIRECTORY, false);

		// Replace old files with downloaded ones
		String fileName = EarthOrientationParameters.PATH_TO_FILE_IAU1980.substring(EarthOrientationParameters.PATH_TO_FILE_IAU1980.lastIndexOf(Zip.ZIP_SEPARATOR) + 1);
		GeneralQuery.queryFile(query1, path+FileIO.getFileSeparator()+fileName);
		Logger.log(LEVEL.TRACE_LEVEL1, "   New IAU1980 file: "+FileIO.getLineSeparator() + DataSet.arrayListToString(ReadFile.readAnyExternalFile(path+FileIO.getFileSeparator()+fileName)));
		fileName = EarthOrientationParameters.PATH_TO_FILE_IAU2000.substring(EarthOrientationParameters.PATH_TO_FILE_IAU2000.lastIndexOf(Zip.ZIP_SEPARATOR) + 1);
		GeneralQuery.queryFile(query2, path+FileIO.getFileSeparator()+fileName);
		Logger.log(LEVEL.TRACE_LEVEL1, "   New IAU2000 file: "+FileIO.getLineSeparator() + DataSet.arrayListToString(ReadFile.readAnyExternalFile(path+FileIO.getFileSeparator()+fileName)));

		// Zip new folder
		pathToJar = FileIO.getPath(true) + FileIO.DATA_EOP_JARFILE+".jar";
		Logger.log(LEVEL.TRACE_LEVEL1, "   Zipping new file: "+pathToJar);
		Zip.zipDirectory(pathToJar, Update.JPARSEC_TEMP_DIRECTORY+FileIO.getFileSeparator()+Version.PACKAGE_NAME.toLowerCase());

		// Delete temp folder. Note that compressed files/directories are automatically
		// deleted during the compression process.
		Logger.log(LEVEL.TRACE_LEVEL1, "   Deleting temp dir");
		FileIO.deleteFile(Update.JPARSEC_TEMP_DIRECTORY);
	}

	/**
	 * Updates Sun spots database from Internet.
	 * @param year Year of the file to be updated.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void updateSunSpotsDatabase(int year)
	throws JPARSECException {
		Logger.log(LEVEL.TRACE_LEVEL2, "Updating Sun spots for year "+year);
		String fileName = "g"+year+".txt";

		// Query the files
		String query1 = Update.UPDATE_URL_SUN_SPOTS+fileName, file1 = "";
		try {
			file1 = GeneralQuery.query(query1);
		} catch (Exception exc) {
			fileName = "g"+year+".TXT";
			query1 = Update.UPDATE_URL_SUN_SPOTS+fileName;
			file1 = GeneralQuery.query(query1);
		}
		fileName = fileName.toLowerCase();

		// Continue only if the files seems to be correctly retrieved
		if (file1 == null) throw new JPARSECException("no response from url "+query1+".");
		if (file1.length() < 100) throw new JPARSECException("the response from url "+query1+" ("+file1+") seems to be invalid.");

		// Create temporal directory
		String path = Update.JPARSEC_TEMP_DIRECTORY+Zip.ZIP_SEPARATOR+FileIO.DATA_SUNSPOT_DIRECTORY;
		path = DataSet.replaceAll(path, Zip.ZIP_SEPARATOR, FileIO.getFileSeparator(), true);
		Logger.log(LEVEL.TRACE_LEVEL1, "   Creating temp dir "+path);
		boolean success = (new java.io.File(path)).mkdirs();
		if (!success) throw new JPARSECException("cannot create directory "+path);

		// Unzip old files from sunspot.jar to a temp folder
		String pathToJar = FileIO.getPath(true) + FileIO.DATA_SUNSPOT_JARFILE+".jar";
		Logger.log(LEVEL.TRACE_LEVEL1, "   Unzipping file "+pathToJar);
		Zip.unZipFile(pathToJar, Update.JPARSEC_TEMP_DIRECTORY, false);

		// Replace old file / add file  with downloaded one
		WriteFile.writeAnyExternalFile(path+fileName, file1);
		Logger.log(LEVEL.TRACE_LEVEL1, "   New file: "+path+fileName+FileIO.getLineSeparator() + file1);

		// Zip new folder
		pathToJar = FileIO.getPath(true) + FileIO.DATA_SUNSPOT_JARFILE+".jar";
		Logger.log(LEVEL.TRACE_LEVEL1, "   Zipping new file: "+pathToJar);
		Zip.zipDirectory(pathToJar, Update.JPARSEC_TEMP_DIRECTORY+FileIO.getFileSeparator()+Version.PACKAGE_NAME.toLowerCase());

		// Delete temp folder. Note that compressed files/directories are automatically
		// deleted during the compression process.
		Logger.log(LEVEL.TRACE_LEVEL1, "   Deleting temp dir");
		FileIO.deleteFile(Update.JPARSEC_TEMP_DIRECTORY);
	}

	/**
	 * Updates JPL database of molecular spectroscopy from Internet.
	 * The whole catalog is downloaded, so this method needs a lot of time.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void updateJPLdatabase()
	throws JPARSECException {
		Logger.log(LEVEL.TRACE_LEVEL2, "Updating JPL catalog");

		// Create temporal directory
		String path = Update.JPARSEC_TEMP_DIRECTORY+Zip.ZIP_SEPARATOR+"JPL/";
		path = DataSet.replaceAll(path, Zip.ZIP_SEPARATOR, FileIO.getFileSeparator(), true);
		Logger.log(LEVEL.TRACE_LEVEL1, "   Creating temp dir "+path);
		boolean success = (new java.io.File(path)).mkdirs();
		if (!success) throw new JPARSECException("cannot create directory "+path);

		// Query the files
		String query = Update.UPDATE_URL_JPL_CATALOGUE + "catdir.cat";
		String cat = GeneralQuery.query(query);
		String array[] = jparsec.graph.DataSet.toStringArray(cat, FileIO.getLineSeparator());
		for (int i=0; i<array.length; i++)
		{
			String file = CatalogRead.getMoleculeFileName(array[i]);
			query = Update.UPDATE_URL_JPL_CATALOGUE + file;
			GeneralQuery.queryFile(query, path+file);

			Logger.log(LEVEL.TRACE_LEVEL1, "   New file: "+path+file+FileIO.getLineSeparator() + DataSet.arrayListToString(ReadFile.readAnyExternalFile(path+file)));
			if (i == 0) WriteFile.writeAnyExternalFile(path+"catdir.cat", cat);
		}

		// Zip new folder
		String pathToJar = FileIO.getPath(true) + "jpl.jar";
		Logger.log(LEVEL.TRACE_LEVEL1, "   Zipping new file: "+pathToJar);
		Zip.zipDirectory(pathToJar, Update.JPARSEC_TEMP_DIRECTORY+FileIO.getFileSeparator()+"JPL");

		// Delete temp folder. Note that compressed files/directories are automatically
		// deleted during the compression process.
		Logger.log(LEVEL.TRACE_LEVEL1, "   Deleting temp dir");
		FileIO.deleteFile(Update.JPARSEC_TEMP_DIRECTORY);
	}

	/**
	 * Updates COLOGNE database of molecular spectroscopy from Internet.
	 * The whole catalogue is downloaded, so this method needs a lot of time.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void updateCOLOGNEdatabase()
	throws JPARSECException {
		Logger.log(LEVEL.TRACE_LEVEL2, "Updating COLOGNE catalog");

		// Create temporal directory
		String path = Update.JPARSEC_TEMP_DIRECTORY+Zip.ZIP_SEPARATOR+"COLOGNE/";
		path = DataSet.replaceAll(path, Zip.ZIP_SEPARATOR, FileIO.getFileSeparator(), true);
		Logger.log(LEVEL.TRACE_LEVEL1, "   Creating temp dir "+path);
		boolean success = (new java.io.File(path)).mkdirs();
		if (!success) throw new JPARSECException("cannot create directory "+path);

		// Query the files
		String query = Update.UPDATE_URL_COLOGNE_CATALOGUE + "partition_function.html";
		String cat = GeneralQuery.query(query);
		String array[] = jparsec.graph.DataSet.toStringArray(cat, FileIO.getLineSeparator());
		boolean start = false, end = false;
		int init = 0;
		for (int i=0; i<array.length; i++)
		{
			if (array[i].indexOf("</pre>") >= 0) {
				String catalog = "";
				for (int j=init; j<i; j++)
				{
					catalog += array[j] + FileIO.getLineSeparator();
				}
				WriteFile.writeAnyExternalFile(path+"catdir.cat", catalog);
				end = true;
			}
			if (start && ! end && i >= (init+2)) {
				String file = CatalogRead.getMoleculeFileName(array[i]);
				query = Update.UPDATE_URL_COLOGNE_CATALOGUE + file;
				GeneralQuery.queryFile(query, path+file);
				Logger.log(LEVEL.TRACE_LEVEL1, "   New file: "+path+file+FileIO.getLineSeparator() + DataSet.arrayListToString(ReadFile.readAnyExternalFile(path+file)));
			}
			if (array[i].indexOf("<pre>") >= 0) {
				start = true;
				init = i + 1;
			}
		}

		// Zip new folder
		String pathToJar = FileIO.getPath(true) + "cologne.jar";
		Logger.log(LEVEL.TRACE_LEVEL1, "   Zipping new file: "+pathToJar);
		Zip.zipDirectory(pathToJar, Update.JPARSEC_TEMP_DIRECTORY+FileIO.getFileSeparator()+"COLOGNE");

		// Delete temp folder. Note that compressed files/directories are automatically
		// deleted during the compression process.
		Logger.log(LEVEL.TRACE_LEVEL1, "   Deleting temp dir");
		FileIO.deleteFile(Update.JPARSEC_TEMP_DIRECTORY);
	}

	/**
	 * Updates the orbital elements of natural satellites from JPL website.
	 * This is a little risky operation, since it is known that JPL updates
	 * this site quite often, and sometimes the new format of the html file
	 * could be modified. Some effort has been made to try to avoid problems.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void updateOrbitalElementsOfNaturalSatellites()
	throws JPARSECException {
		Logger.log(LEVEL.TRACE_LEVEL2, "Updating natural satellites");

		// Query the files
		String query = Update.UPDATE_URL_NATURAL_SATELLITES;
		String file = GeneralQuery.query(query);

		String satElems = "";

		String refLaplace = "Mean orbital elements referred to the local Laplace planes";
		String refEcliptic = "Mean ecliptic orbital elements";
		String refPlanetEquator = "Mean orbital elements referred to the planet equator";
		String refEquatorial = "Mean equatorial orbital elements";
		String sep = FileIO.getLineSeparator();
		TARGET lastPlanet = TARGET.NOT_A_PLANET;
		boolean newTable;

		String subFile = file;
		int beginTable = subFile.indexOf("<TABLE");
		do {
			// Get the next table
			int lastH3 = subFile.substring(0, beginTable).lastIndexOf("<H3");
			String pre = "";
			if (lastH3 >= 0)
			{
				String preTable = subFile.substring(lastH3, beginTable).toLowerCase();
				if (preTable.indexOf("laplace") > 0) {
					pre += refLaplace + sep;
				} else {
					if (preTable.indexOf("ecliptic") > 0) {
						pre += refEcliptic + sep;
					} else {
						if (preTable.indexOf("equatorial") > 0) {
							pre += refEquatorial + sep;
						} else {
							if (preTable.indexOf("equator") > 0) pre += refPlanetEquator + sep;
						}
					}
				}
				String epoch = preTable.substring(preTable.indexOf("epoch") + 5).trim();
				epoch = epoch.substring(0, epoch.indexOf("<")).trim();
				pre += "Epoch " + epoch.toUpperCase() + sep;
				String sol = preTable.substring(preTable.indexOf("</b>") + 4).trim();
				sol = sol.substring(0, sol.indexOf("<hr>")).trim();
				if (preTable.indexOf("</b>") < 0) sol = "";
				pre += "Solution "+sol.toUpperCase() + sep;
			}

			int endTable = subFile.indexOf("</TABLE>");
			int endBeginTable = beginTable + subFile.substring(beginTable).indexOf(">") + 1;
			String table = subFile.substring(endBeginTable, endTable);
			newTable = true;

			int beginTR = table.indexOf("<TR");
			do {
				int endBeginTR = beginTR + table.substring(beginTR).indexOf(">") + 1;
				int endTR = table.indexOf("</TR>");
				String tr = table.substring(endBeginTR, endTR);

				String line = "";
				int beginTD = tr.indexOf("<TD");
				do {
					int endBeginTD = beginTD + tr.substring(beginTD).indexOf(">") + 1;
					int endTD = tr.indexOf("</TD>");
					String td = tr.substring(endBeginTD, endTD);

					int beginTag = td.indexOf("<");
					do {
						int endTag = td.indexOf(">");
						String newtd = "";
						if (beginTag > 0) newtd = td.substring(0, beginTag);
						newtd += td.substring(endTag+1);
						td = newtd;
						beginTag = td.indexOf("<");
					} while (beginTag >= 0);

					line += td + "   ";

					tr = tr.substring(endTD+5);
					beginTD = tr.indexOf("<TD");
				} while (beginTD >= 0);

				if (!line.startsWith(" ") && !line.startsWith("&") && !line.startsWith("a ")
						&& !line.startsWith("(")) {
					if (newTable) {
						newTable = false;
						try {
							TARGET satellite = jparsec.ephem.Target.getID(FileIO.getField(1, line, " ", true));
							TARGET planet = satellite.getCentralBody();
							if (planet != TARGET.SUN) lastPlanet = planet;
						} catch (JPARSECException e) {}
						pre = "*"+sep+lastPlanet.getName()+sep+pre;
						satElems += pre;
					}
					satElems += line + sep;
				}

				table = table.substring(endTR+5);
				beginTR = table.indexOf("<TR");
			} while (beginTR >= 0);

			// Update the position in the html file to go to the next table
			// if there is any
			subFile = subFile.substring(endTable+8);
			beginTable = subFile.indexOf("<TABLE");
		} while (beginTable >= 0);


		// Create temporal directory
		String path = Update.JPARSEC_TEMP_DIRECTORY+Zip.ZIP_SEPARATOR+FileIO.DATA_ORBITAL_ELEMENTS_DIRECTORY;
		path = DataSet.replaceAll(path, Zip.ZIP_SEPARATOR, FileIO.getFileSeparator(), true);
		Logger.log(LEVEL.TRACE_LEVEL1, "   Creating temp dir "+path);
		boolean success = (new java.io.File(path)).mkdirs();
		if (!success) throw new JPARSECException("cannot create directory "+path);

		// Unzip old files from orbital_elements.jar to a temp folder
		String pathToJar = FileIO.getPath(true) + FileIO.DATA_ORBITAL_ELEMENTS_JARFILE+".jar";
		Logger.log(LEVEL.TRACE_LEVEL1, "   Unzipping file "+pathToJar);
		Zip.unZipFile(pathToJar, Update.JPARSEC_TEMP_DIRECTORY, false);

		// Replace old files with downloaded and processed data
		String fileName = MoonEphem.PATH_TO_JPL_SATELLITES_FILE.substring(MoonEphem.PATH_TO_JPL_SATELLITES_FILE.lastIndexOf(Zip.ZIP_SEPARATOR) + 1);
		String filePath = path+FileIO.getFileSeparator()+fileName;
		String old[] = DataSet.arrayListToStringArray(ReadFile.readAnyExternalFile(filePath));
		for (int i=old.length-1; i>=0; i--)
		{
			if (old[i].startsWith("!")) satElems = old[i] + sep + satElems;
		}
		WriteFile.writeAnyExternalFile(filePath, satElems);
		Logger.log(LEVEL.TRACE_LEVEL1, "   New file: "+filePath+FileIO.getLineSeparator() + satElems);

		// Zip new folder
		pathToJar = FileIO.getPath(true) + FileIO.DATA_ORBITAL_ELEMENTS_JARFILE+".jar";
		Logger.log(LEVEL.TRACE_LEVEL1, "   Zipping new file: "+pathToJar);
		Zip.zipDirectory(pathToJar, Update.JPARSEC_TEMP_DIRECTORY+FileIO.getFileSeparator()+Version.PACKAGE_NAME.toLowerCase());

		// Delete temp folder. Note that compressed files/directories are automatically
		// deleted during the compression process.
		Logger.log(LEVEL.TRACE_LEVEL1, "   Deleting temp dir");
		FileIO.deleteFile(Update.JPARSEC_TEMP_DIRECTORY);
	}

	/**
	 * Updates the information about sizes and magnitudes of artificial
	 * satellites.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void updateSizeAndMagnitudeOfArtificialSatellites()
	throws JPARSECException {
		Logger.log(LEVEL.TRACE_LEVEL2, "Updating size ang magnitudes of artificial satellites");

		// Create temporal directory
		String path = Update.JPARSEC_TEMP_DIRECTORY+Zip.ZIP_SEPARATOR;
		path = DataSet.replaceAll(path, Zip.ZIP_SEPARATOR, FileIO.getFileSeparator(), true);
		Logger.log(LEVEL.TRACE_LEVEL1, "   Creating temp dir "+path);
		boolean success = (new java.io.File(path)).mkdirs();
		if (!success) throw new JPARSECException("cannot create directory "+path);

		// Query the file
		String query = Update.UPDATE_URL_ARTIFICIAL_SATELLITES_SIZE_AND_MAGNITUDE;
		String fileName = path+"sat.zip";
		GeneralQuery.queryFile(query, fileName);

		// Unzip old files from orbital_elements.jar to a temp folder
		Logger.log(LEVEL.TRACE_LEVEL1, "   Unzipping file "+fileName);
		Zip.unZipFile(fileName, Update.JPARSEC_TEMP_DIRECTORY, false);

		// Delete old files, but reading the new data previously
		FileIO.deleteFile(fileName);
		String files[] = FileIO.getFiles(path);
		if (files.length > 1) throw new JPARSECException("query returned more than one file. Please check what has been retrieved.");
		String text[] = DataSet.arrayListToStringArray(ReadFile.readAnyExternalFile(files[0]));
		FileIO.deleteFile(files[0]);
		FileIO.deleteFile(Update.JPARSEC_TEMP_DIRECTORY);

		// Create temporal directory
		path = Update.JPARSEC_TEMP_DIRECTORY+Zip.ZIP_SEPARATOR+FileIO.DATA_ORBITAL_ELEMENTS_DIRECTORY;
		path = DataSet.replaceAll(path, Zip.ZIP_SEPARATOR, FileIO.getFileSeparator(), true);
		Logger.log(LEVEL.TRACE_LEVEL1, "   Creating temp dir "+path);
		success = (new java.io.File(path)).mkdirs();
		if (!success) throw new JPARSECException("cannot create directory "+path);

		// Unzip old files from orbital_elements.jar to a temp folder
		String pathToJar = FileIO.getPath(true) + FileIO.DATA_ORBITAL_ELEMENTS_JARFILE+".jar";
		Logger.log(LEVEL.TRACE_LEVEL1, "   Unzipping file "+fileName);
		Zip.unZipFile(pathToJar, Update.JPARSEC_TEMP_DIRECTORY, false);

		// Replace old files with downloaded and processed data
		fileName = "sat_mag.txt";
		String filePath = path+FileIO.getFileSeparator()+fileName;
		WriteFile.writeAnyExternalFile(filePath, text);
		Logger.log(LEVEL.TRACE_LEVEL1, "   New file: "+filePath+FileIO.getLineSeparator() + DataSet.toString(text, FileIO.getLineSeparator()));

		// Zip new folder
		pathToJar = FileIO.getPath(true) + FileIO.DATA_ORBITAL_ELEMENTS_JARFILE+".jar";
		Logger.log(LEVEL.TRACE_LEVEL1, "   Zipping new file: "+pathToJar);
		Zip.zipDirectory(pathToJar, Update.JPARSEC_TEMP_DIRECTORY+FileIO.getFileSeparator()+Version.PACKAGE_NAME.toLowerCase());

		// Delete temp folder. Note that compressed files/directories are automatically
		// deleted during the compression process.
		Logger.log(LEVEL.TRACE_LEVEL1, "   Deleting temp dir");
		FileIO.deleteFile(Update.JPARSEC_TEMP_DIRECTORY);
	}

	/**
	 * Updates the Padova-Asiago SN catalogue.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void updatePadovaAsiagoSNcat()
	throws JPARSECException {
		Logger.log(LEVEL.TRACE_LEVEL2, "Updating SN catalog");

		// Query the file
		String query = Update.UPDATE_URL_PADOVA_ASIAGO_SN_CAT;
		String text = GeneralQuery.query(query);

		// Create temporal directory
		String path = Update.JPARSEC_TEMP_DIRECTORY+Zip.ZIP_SEPARATOR+FileIO.DATA_SKY_DIRECTORY;
		path = DataSet.replaceAll(path, Zip.ZIP_SEPARATOR, FileIO.getFileSeparator(), true);
		Logger.log(LEVEL.TRACE_LEVEL1, "   Creating temp dir "+path);
		boolean success = (new java.io.File(path)).mkdirs();
		if (!success) throw new JPARSECException("cannot create directory "+path);

		// Unzip old files from sky.jar to a temp folder
		String pathToJar = FileIO.getPath(true) + FileIO.DATA_SKY_JARFILE+".jar";
		Logger.log(LEVEL.TRACE_LEVEL1, "   Unzipping file "+pathToJar);
		Zip.unZipFile(pathToJar, Update.JPARSEC_TEMP_DIRECTORY, true);

		// Replace old files with downloaded data
		String fileName = "Padova-Asiago sn cat.txt";
		String filePath = path+FileIO.getFileSeparator()+fileName;
		String old[] = DataSet.arrayListToStringArray(ReadFile.readAnyExternalFile(filePath));
		text = text.substring(text.indexOf(FileIO.getLineSeparator()));
		String text2 = "";
		for (int i=0; i<7; i++)
		{
			text2 += old[i];
			if (i<6) text2 += FileIO.getLineSeparator();
		}
		text = text2+text;
		WriteFile.writeAnyExternalFile(filePath, text);
		Logger.log(LEVEL.TRACE_LEVEL1, "   New file: "+filePath+FileIO.getLineSeparator() + text);

		// Zip new folder
		pathToJar = FileIO.getPath(true) + FileIO.DATA_SKY_JARFILE+".jar";
		Logger.log(LEVEL.TRACE_LEVEL1, "   Zipping new file: "+pathToJar);
		Zip.zipDirectory(pathToJar, Update.JPARSEC_TEMP_DIRECTORY+FileIO.getFileSeparator()+Version.PACKAGE_NAME.toLowerCase());

		// Delete temp folder. Note that compressed files/directories are automatically
		// deleted during the compression process.
		Logger.log(LEVEL.TRACE_LEVEL1, "   Deleting temp dir");
		FileIO.deleteFile(Update.JPARSEC_TEMP_DIRECTORY);

	}

	/**
	 * Updates the galactic novae catalogue.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void updateNovae()
	throws JPARSECException {
		Logger.log(LEVEL.TRACE_LEVEL2, "Updating novae catalog");

		// Query the file
		String query = Update.UPDATE_URL_NOVAE;
		String text = GeneralQuery.query(query);

		// Create temporal directory
		String path = Update.JPARSEC_TEMP_DIRECTORY+Zip.ZIP_SEPARATOR+FileIO.DATA_SKY_DIRECTORY;
		path = DataSet.replaceAll(path, Zip.ZIP_SEPARATOR, FileIO.getFileSeparator(), true);
		Logger.log(LEVEL.TRACE_LEVEL1, "   Creating temp dir "+path);
		boolean success = (new java.io.File(path)).mkdirs();
		if (!success) throw new JPARSECException("cannot create directory "+path);

		// Unzip old files from sky.jar to a temp folder
		String pathToJar = FileIO.getPath(true) + FileIO.DATA_SKY_JARFILE+".jar";
		Logger.log(LEVEL.TRACE_LEVEL1, "   Unzipping file "+pathToJar);
		Zip.unZipFile(pathToJar, Update.JPARSEC_TEMP_DIRECTORY, true);

		// Replace old files with downloaded data
		String fileName = "galnovae.txt";
		String filePath = path+FileIO.getFileSeparator()+fileName;
		WriteFile.writeAnyExternalFile(filePath, text);
		Logger.log(LEVEL.TRACE_LEVEL1, "   New file: "+filePath+FileIO.getLineSeparator() + text);

		// Zip new folder
		pathToJar = FileIO.getPath(true) + FileIO.DATA_SKY_JARFILE+".jar";
		Logger.log(LEVEL.TRACE_LEVEL1, "   Zipping new file: "+pathToJar);
		Zip.zipDirectory(pathToJar, Update.JPARSEC_TEMP_DIRECTORY+FileIO.getFileSeparator()+Version.PACKAGE_NAME.toLowerCase());

		// Delete temp folder. Note that compressed files/directories are automatically
		// deleted during the compression process.
		Logger.log(LEVEL.TRACE_LEVEL1, "   Deleting temp dir");
		FileIO.deleteFile(Update.JPARSEC_TEMP_DIRECTORY);

	}

	/**
	 * Updates the orbital elements of visual binary stars.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void updateOrbitsOfVisualBinaryStars()
	throws JPARSECException {
		Logger.log(LEVEL.TRACE_LEVEL2, "Updating visual binary stars");

		// Query the file
		String query = Update.UPDATE_URL_ORBITS_VISUAL_BINARY_STARS;
		String text[] = DataSet.toStringArray(GeneralQuery.query(query), FileIO.getLineSeparator());

		// Create temporal directory
		String path = Update.JPARSEC_TEMP_DIRECTORY+Zip.ZIP_SEPARATOR+FileIO.DATA_ORBITAL_ELEMENTS_DIRECTORY;
		path = DataSet.replaceAll(path, Zip.ZIP_SEPARATOR, FileIO.getFileSeparator(), true);
		Logger.log(LEVEL.TRACE_LEVEL1, "   Creating temp dir "+path);
		boolean success = (new java.io.File(path)).mkdirs();
		if (!success) throw new JPARSECException("cannot create directory "+path);

		// Unzip old files from orbital_elements.jar to a temp folder
		String pathToJar = FileIO.getPath(true) + FileIO.DATA_ORBITAL_ELEMENTS_JARFILE+".jar";
		Logger.log(LEVEL.TRACE_LEVEL1, "   Unzipping file "+pathToJar);
		Zip.unZipFile(pathToJar, Update.JPARSEC_TEMP_DIRECTORY, false);

		// Replace old files with downloaded and processed data
		String fileName = "orb6orbits.txt";
		String filePath = path+FileIO.getFileSeparator()+fileName;
		WriteFile.writeAnyExternalFile(filePath, text);
		Logger.log(LEVEL.TRACE_LEVEL1, "   New file: "+filePath+FileIO.getLineSeparator() + DataSet.toString(text, FileIO.getLineSeparator()));

		// Zip new folder
		pathToJar = FileIO.getPath(true) + FileIO.DATA_ORBITAL_ELEMENTS_JARFILE+".jar";
		Logger.log(LEVEL.TRACE_LEVEL1, "   Zipping new file: "+pathToJar);
		Zip.zipDirectory(pathToJar, Update.JPARSEC_TEMP_DIRECTORY+FileIO.getFileSeparator()+Version.PACKAGE_NAME.toLowerCase());

		// Delete temp folder. Note that compressed files/directories are automatically
		// deleted during the compression process.
		Logger.log(LEVEL.TRACE_LEVEL1, "   Deleting temp dir");
		FileIO.deleteFile(Update.JPARSEC_TEMP_DIRECTORY);
	}
	/**
	 * Name for temporal file.
	 */
	public static final String JPARSEC_TEMP_FILE = "jparsec_tempFile";
	/**
	 * Name for temporal directory.
	 */
	public static final String JPARSEC_TEMP_DIRECTORY = "jparsec_tempDir";
}
