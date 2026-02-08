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

import java.io.File;
import java.math.RoundingMode;

import jparsec.ephem.EphemerisElement;
import jparsec.ephem.EphemerisElement.ALGORITHM;
import jparsec.ephem.planets.OrbitEphem;
import jparsec.ephem.probes.SatelliteEphem;
import jparsec.ephem.probes.SatelliteOrbitalElement;
import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.io.WriteFile;
import jparsec.math.Constant;
import jparsec.time.AstroDate;
import jparsec.util.Logger.LEVEL;
import jparsec.vo.GeneralQuery;

/**
 * A class to configure the behavior of JPARSEC in the current JVM.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Configuration
{
	// private constructor so that this class cannot be instantiated.
	private Configuration() {}

	/**
	 * True to use disk to hold data. Default is false.
	 */
	//public static boolean USE_DISK_FOR_DATABASE = false;

	/** Holds the number of similar instances that can be cached simultaneously in the database
	 * when using the class {@linkplain ReadFile}. This means, for instance, the number of
	 * different sky renderings that can be hold on memory per thread (saving orbital elements,
	 * stars, ...). */
	public static int MAX_CACHE_SIZE = 3;

	/**
	 * Holds how many instances of {@linkplain jparsec.graph.chartRendering.RenderSky} can hold data in the class 
	 * {@linkplain DataBase} for all threads. When rendering the sky while creating new 
	 * instances of {@linkplain jparsec.graph.chartRendering.RenderSky} (independently of the thread creating it), all 
	 * the data used in previous renderings will be saved to allow later use. Once the 
	 * cache is filled the old data will be removed. Use &lt;= 0 for no limitation (not recommended).
	 */
	public static int MAX_RENDERSKY_DATABASE_CACHE_SIZE = 10;

	/**
	 * Holds the maximum acceptable difference of days between the date of the
	 * latest data for supernovae. After this period new data will be downloaded if possible.
	 */
	public static int MAXIMUM_DAYS_FOR_SUPERNOVAE = 30;

	/**
	 * Holds the maximum acceptable difference of days between the date of the
	 * latest data for galactic novae. After this period new data will be downloaded if possible.
	 */
	public static int MAXIMUM_DAYS_FOR_NOVAE = 30;

	/**
	 * Holds the maximum acceptable difference of days between the date of the
	 * orbital elements of artificial satellites and the date when new elements
	 * should be downloaded.
	 */
	public static int MAXIMUM_DAYS_FROM_ELEMENTS_ARTIFICIAL_SATELLITES = 3;

	/**
	 * Holds the maximum acceptable difference of days between the date of the
	 * orbital elements of artificial satellites and the date they can be shown.
	 */
	public static int MAXIMUM_DAYS_FROM_ELEMENTS_ARTIFICIAL_SATELLITES_SHOW = 7;

	/**
	 * Holds the maximum acceptable difference of days between the date of the
	 * orbital elements of comets/asteroids and the date they can be shown.
	 */
	public static int MAXIMUM_DAYS_FROM_ELEMENTS_COMETS_ASTEROIDS = 1 + 5 * 365;

	/**
	 * Holds the maximum acceptable difference of days between the date of the
	 * orbital elements of comets/asteroids and the date of the rendering to get
	 * accurate positions.
	 */
	public static int MAXIMUM_DAYS_FROM_ELEMENTS_COMETS_ASTEROIDS_QUERY = 30;

	/**
	 * Holds the maximum number of days the cache file at /tmp will be used before
	 * updating it to a new one.
	 */
	public static int MAXIMUM_DAYS_FROM_ELEMENTS_COMETS_ASTEROIDS_BETWEEN_TWO_QUERIES = 15;

	/**
	 * Time out for the queries in milliseconds. Use &lt;= 0 to disable downloads.
	 */
	public static int QUERY_TIMEOUT = 5000;

	/**
	 * Forces the /lib subdirectory of JPARSEC (with all dependencies) to be
	 * at certain path. Default is null to obtain it automatically.
	 */
	public static String FORCE_JPARSEC_LIB_DIRECTORY = null;

	/**
	 * Forces the temporal directory of the system to be
	 * at certain path. Default is null to obtain it automatically.
	 */
	public static String FORCE_TEMP_DIRECTORY = null;

	/**
	 * True to set applet mode. In this case the impossibility of writing to
	 * temporal directory is overtaken by reading some files in memory.
	 */
	public static boolean APPLET_MODE = false;

	/**
	 * Path to Gildas (GAG) root directory (for example gildas-exe-dec08b).
	 */
	public static String PATH_GILDAS_GAG_ROOT = null;
	/**
	 * Path to Gildas (GAG) exec directory name (for example pc-debianlenny-g95).
	 */
	public static String PATH_GILDAS_GAG_EXEC = null;

	/**
	 * Location of the files of JPL ephemerides, DE405 or any other integration. Set to
	 * null to use JPARSEC file if it is available.
	 */
	public static String JPL_EPHEMERIDES_FILES_EXTERNAL_PATH = null;

	/**
	 * Holds the number of decimal places for the precision of the
	 * operations in big decimal mode. Default is 18.
	 */
	public static int BIG_DECIMAL_PRECISION_DECIMAL_PLACES = 18;
	/**
	 * Holds the rounding mode for division operation in bid decimal mode. Default is 'half up'.
	 */
	public static RoundingMode BIG_DECIMAL_PRECISION_ROUNDING_MODE = RoundingMode.HALF_UP;
	
	/** Default JPL version to use for high accuracy ephemerides. */
	public static EphemerisElement.ALGORITHM JPL_DEFAULT_VERSION = ALGORITHM.JPL_DE405;
	
	/** Apply time correction for Moon secular acceleration in ELP2000 and MainEvents/LunarEvent classes. */
	public static boolean ENABLE_TIME_CORRECTION_FOR_MOON_SECULAR_ACCELERATION = true;

	/** Select true to allow downloading EOP data on the fly for current times if required and the 
	 * EOP data in the dependencies is missing or not updated for the calculation time. */
	public static boolean ALLOW_DOWNLOAD_EOP = false;
	
	/**
	 * Returns the last time a given resource was modified.
	 * @param j The jar file name. See {@linkplain FileIO} class.
	 * @param d The path to the directory where the file is located. See {@linkplain FileIO} class.
	 * @param f The name of the file.
	 * @return The date, or null if it is unknown.
	 * @throws JPARSECException If an error occurs.
	 */
	public static AstroDate getResourceLastModifiedTime(String j, String d, String f) throws JPARSECException {
		long time = getLastModifiedTimeOfResource(j, d, f);
		if (time <= 0) return null;
		return new AstroDate(time);
	}

	private static long getLastModifiedTimeOfResource(String j, String d, String f) {
		try {
			long date = FileIO.getLastModifiedTimeOfResource(j, d, f);
			if (date <= 0) throw new Exception("not allowed");
			return date;
		} catch (Exception exc) {
			// Security manager (applet mode) or exception
			try {
				// The trick is to use the reference date of the elements of the first artificial satellite,
				// since when they are updated the reflect very approximately the last update time.
				// This works if everything is updated the same date, which is usual.
				ReadFile re = new ReadFile();
				re.setPath(SatelliteEphem.PATH_TO_SATELLITES_FILE);
				if (SatelliteEphem.USE_IRIDIUM_SATELLITES) re.setPath(SatelliteEphem.PATH_TO_SATELLITES_IRIDIUM_FILE);
				re.readFileOfArtificialSatellites();

				SatelliteOrbitalElement sat = re.getSatelliteOrbitalElement(0);
				AstroDate astro = new AstroDate(sat.year, 1, sat.day);
				Logger.log(LEVEL.INFO, "Cannot obtain last update time of orbital elements. The approximate date is "+astro.toString()+", based on artificial satellites elements.");
				return astro.msFrom1970();
			} catch (Exception exc2) {
				return -1; // Unknown
			}
		}
	}

	/**
	 * Returns if a date is acceptable to show artificial satellites.
	 * @param astro The date in UTC.
	 * @return True or false.
	 * @throws JPARSECException If an error occurs.
	 */
	public static boolean isAcceptableDateForArtificialSatellites(AstroDate astro) throws JPARSECException {
		long date;
		if (SatelliteEphem.usingExternalElementsAsDefaultData()) {
			SatelliteOrbitalElement sat = SatelliteEphem.getArtificialSatelliteOrbitalElement(0);
			AstroDate sastro = new AstroDate(sat.year, 1, sat.day);
			date = sastro.msFrom1970();
		} else {
			String fileName = FileIO.getFileNameFromPath(SatelliteEphem.PATH_TO_SATELLITES_FILE);
			if (SatelliteEphem.USE_IRIDIUM_SATELLITES)
				fileName = FileIO.getFileNameFromPath(SatelliteEphem.PATH_TO_SATELLITES_IRIDIUM_FILE);
			date = getLastModifiedTimeOfResource(FileIO.DATA_ORBITAL_ELEMENTS_JARFILE, FileIO.DATA_ORBITAL_ELEMENTS_DIRECTORY, fileName);
		}
		if (date <= 0) {
			if (APPLET_MODE) return true;
			return false;
		}
		double dt = (astro.msFrom1970() - date) / (1000.0 * Constant.SECONDS_PER_DAY);
		if (dt < -MAXIMUM_DAYS_FROM_ELEMENTS_ARTIFICIAL_SATELLITES || dt > MAXIMUM_DAYS_FROM_ELEMENTS_ARTIFICIAL_SATELLITES) return false;
		return true;
	}

	/**
	 * Returns if a date is acceptable to show supernovae.
	 * @param astro The date in UTC.
	 * @return True, or false if new data should be downloaded according to the
	 * maximum number of days allowed for old data to be used.
	 * @throws JPARSECException If an error occurs.
	 */
	public static boolean isAcceptableDateForSupernovae(AstroDate astro) throws JPARSECException {
		long date = getLastModifiedTimeOfResource(FileIO.DATA_SKY_JARFILE, FileIO.DATA_SKY_DIRECTORY, "Padova-Asiago sn cat.txt");
		if (date <= 0) {
			return false;
		}
		double dt = (astro.msFrom1970() - date) / (1000.0 * Constant.SECONDS_PER_DAY);
		int max = MAXIMUM_DAYS_FOR_SUPERNOVAE;
		if (dt > max) return false;
		return true;
	}

	/**
	 * Returns if a date is acceptable to show galactic novae.
	 * @param astro The date in UTC.
	 * @return True, or false if new data should be downloaded according to the
	 * maximum number of days allowed for old data to be used.
	 * @throws JPARSECException If an error occurs.
	 */
	public static boolean isAcceptableDateForNovae(AstroDate astro) throws JPARSECException {
		long date = getLastModifiedTimeOfResource(FileIO.DATA_SKY_JARFILE, FileIO.DATA_SKY_DIRECTORY, "galnovae.txt");
		if (date <= 0) {
			return false;
		}
		double dt = (astro.msFrom1970() - date) / (1000.0 * Constant.SECONDS_PER_DAY);
		int max = MAXIMUM_DAYS_FOR_NOVAE;
		if (dt > max) return false;
		return true;
	}

	/**
	 * Returns if a date is acceptable to show comets.
	 * @param astro The date in UTC.
	 * @param criteriaForAccuracy True to consider the lower time span for accuracy.
	 * @return True or false.
	 * @throws JPARSECException If an error occurs.
	 */
	public static boolean isAcceptableDateForComets(AstroDate astro, boolean criteriaForAccuracy) throws JPARSECException {
		long date = getLastModifiedTimeOfResource(FileIO.DATA_ORBITAL_ELEMENTS_JARFILE, FileIO.DATA_ORBITAL_ELEMENTS_DIRECTORY, "MPC_comets.txt");
		if (date <= 0) {
			if (APPLET_MODE && !criteriaForAccuracy) return true;
			return false;
		}
		double dt = (astro.msFrom1970() - date) / (1000.0 * Constant.SECONDS_PER_DAY);
		int max = MAXIMUM_DAYS_FROM_ELEMENTS_COMETS_ASTEROIDS;
		if (criteriaForAccuracy) max = MAXIMUM_DAYS_FROM_ELEMENTS_COMETS_ASTEROIDS_QUERY;
		if (dt < -max || dt > max) return false;
		return true;
	}

	/**
	 * Returns if a date is acceptable to show asteroids.
	 * @param astro The date in UTC.
	 * @param criteriaForAccuracy True to consider the lower time span for accuracy.
	 * @return True or false.
	 * @throws JPARSECException If an error occurs.
	 */
	public static boolean isAcceptableDateForAsteroids(AstroDate astro, boolean criteriaForAccuracy) throws JPARSECException {
		long date = getLastModifiedTimeOfResource(FileIO.DATA_ORBITAL_ELEMENTS_JARFILE, FileIO.DATA_ORBITAL_ELEMENTS_DIRECTORY, "MPC_asteroids_bright.txt");
		if (date <= 0) {
			if (APPLET_MODE && !criteriaForAccuracy) return true;
			return false;
		}
		double dt = (astro.msFrom1970() - date) / (1000.0 * Constant.SECONDS_PER_DAY);
		int max = MAXIMUM_DAYS_FROM_ELEMENTS_COMETS_ASTEROIDS;
		if (criteriaForAccuracy) max = MAXIMUM_DAYS_FROM_ELEMENTS_COMETS_ASTEROIDS_QUERY;
		if (dt < -max || dt > max) return false;
		return true;
	}

	/**
	 * Returns if a date is acceptable to show transNeptunian objects.
	 * @param astro The date in UTC.
	 * @param criteriaForAccuracy True to consider the lower time span for accuracy.
	 * @return True or false.
	 * @throws JPARSECException If an error occurs.
	 */
	public static boolean isAcceptableDateForTransNeptunians(AstroDate astro, boolean criteriaForAccuracy) throws JPARSECException {
		long date = getLastModifiedTimeOfResource(FileIO.DATA_ORBITAL_ELEMENTS_JARFILE, FileIO.DATA_ORBITAL_ELEMENTS_DIRECTORY, "MPC_distant_bodies.txt");
		if (date <= 0) {
			if (APPLET_MODE && !criteriaForAccuracy) return true;
			return false;
		}
		double dt = (astro.msFrom1970() - date) / (1000.0 * Constant.SECONDS_PER_DAY);
		int max = MAXIMUM_DAYS_FROM_ELEMENTS_COMETS_ASTEROIDS;
		if (criteriaForAccuracy) max = MAXIMUM_DAYS_FROM_ELEMENTS_COMETS_ASTEROIDS_QUERY;
		if (dt < -max || dt > max) return false;
		return true;
	}

	private static double getdt(long t0, long t1) {
		double dt = (t0 - t1) / (1000.0 * Constant.SECONDS_PER_DAY);
		return dt;
	}

	private static boolean appletOnLineModeArtSat = false, appletOnLineModeAst = false, appletOnLineModeCom = false, appletOnLineModeTran = false;

	/**
	 * Updates the orbital elements of artificial satellites writing
	 * the new file to the temporal directory.
	 * @param astro The date to show artificial satellites.
	 * @return Null if the orbital elements in orbital_elements.jar are
	 * already acceptable, null if the simulation date is too far from
	 * current date (so updating the file would not be acceptable too), and
	 * the path of the temporal file if the file already exists and it is
	 * acceptable or if it has been successfully downloaded. If not null
	 * the process of reading the temp file can continue to draw the objects.
	 * @throws JPARSECException If an error occurs.
	 */
	public static synchronized String updateArtificialSatellitesInTempDir(AstroDate astro) throws JPARSECException {
		//if (isAcceptableDateForArtificialSatellites(astro)) return null;

		double dt = getdt(astro.msFrom1970(), System.currentTimeMillis());

		if (dt < -MAXIMUM_DAYS_FROM_ELEMENTS_ARTIFICIAL_SATELLITES || dt > MAXIMUM_DAYS_FROM_ELEMENTS_ARTIFICIAL_SATELLITES) {
			if (dt < -MAXIMUM_DAYS_FROM_ELEMENTS_ARTIFICIAL_SATELLITES_SHOW || dt > MAXIMUM_DAYS_FROM_ELEMENTS_ARTIFICIAL_SATELLITES_SHOW)
				return null;
		}

		String fileName = FileIO.getFileNameFromPath(SatelliteEphem.PATH_TO_SATELLITES_FILE);
		if (SatelliteEphem.USE_IRIDIUM_SATELLITES)
			fileName = FileIO.getFileNameFromPath(SatelliteEphem.PATH_TO_SATELLITES_IRIDIUM_FILE);

		if (APPLET_MODE) {
			if (appletOnLineModeArtSat) return null;
			appletOnLineModeArtSat = true;

			String[] file = null;
			if (SatelliteEphem.USE_IRIDIUM_SATELLITES) {
				String query1 = Update.UPDATE_URL_VISUAL_ARTIFICIAL_SATELLITES_IRIDIUM;
				file = DataSet.toStringArray(GeneralQuery.query(query1, QUERY_TIMEOUT), FileIO.getLineSeparator(), false);
			} else {
				String query1 = Update.UPDATE_URL_VISUAL_ARTIFICIAL_SATELLITES;
				String file1[] = DataSet.toStringArray(GeneralQuery.query(query1, QUERY_TIMEOUT), FileIO.getLineSeparator(), false);
				//String query2 = Update.UPDATE_URL_VISUAL_ARTIFICIAL_SATELLITES_2;
				//String file2[] = DataSet.toStringArray(GeneralQuery.query(query2, QUERY_TIMEOUT), FileIO.getLineSeparator(), false);
				file = file1; //DataSet.addStringArray(file2, file1);
			}
			try {
				SatelliteEphem.setSatellitesFromExternalFile(file, true);
				appletOnLineModeArtSat = false;
				return "APPLET_MODE";
			} catch (Exception exc) {
				return null;
			}
		}


		String p = FileIO.getTemporalDirectory() + fileName;
		File f = new File(p);
		if (f.exists()) {
			double newdt = getdt(astro.msFrom1970(), f.lastModified());
			double dtElapsedFromLastDownload = getdt(System.currentTimeMillis(), f.lastModified());
			if (Math.abs(newdt) <= Math.abs(dt) || Math.abs(dtElapsedFromLastDownload) < MAXIMUM_DAYS_FROM_ELEMENTS_ARTIFICIAL_SATELLITES) {
				String file[] = DataSet.arrayListToStringArray(ReadFile.readAnyExternalFile(p));
				SatelliteEphem.setSatellitesFromExternalFile(file, true);
				return p;
			}
		}

		if (SatelliteEphem.USE_IRIDIUM_SATELLITES) {
			String query = Update.UPDATE_URL_VISUAL_ARTIFICIAL_SATELLITES_IRIDIUM;
			String q = GeneralQuery.query(query, QUERY_TIMEOUT);
			WriteFile.writeAnyExternalFile(p, q);
		} else {
			String query = Update.UPDATE_URL_VISUAL_ARTIFICIAL_SATELLITES;
			String q1 = GeneralQuery.query(query, QUERY_TIMEOUT);
			//query = Update.UPDATE_URL_VISUAL_ARTIFICIAL_SATELLITES_2;
			//String q2 = GeneralQuery.query(query, QUERY_TIMEOUT);
			WriteFile.writeAnyExternalFile(p, q1);
		}

		// Check the format
		try {
			String file[] = DataSet.arrayListToStringArray(ReadFile.readAnyExternalFile(p));
			SatelliteEphem.setSatellitesFromExternalFile(file, true);
		} catch (Exception exc) {
			//SatelliteEphem.setSatellitesFromExternalFile(null, true);
			FileIO.deleteFile(p);
			return null;
		}

		return p;
	}

	/**
	 * Updates the data for supernovae writing the new file to the temporal directory.
	 * @param astro The date to show supernovae.
	 * @return Null if the data is already acceptable, null if the simulation date is too
	 * far from current date (so updating the file would not be acceptable too), and
	 * the path of the temporal file if the file already exists and it is
	 * acceptable or if it has been successfully downloaded. If not null
	 * the process of reading the temporal file can continue to draw the objects.
	 * @throws JPARSECException If an error occurs.
	 */
	public static synchronized String updateSupernovaeInTempDir(AstroDate astro) throws JPARSECException {
		return null; // Catalog is not longer updated
		/*
		if (isAcceptableDateForSupernovae(astro)) return null;
		if (APPLET_MODE) return "APPLET_MODE";

		double dt = getdt(astro.msFrom1970(), System.currentTimeMillis());

		String p = FileIO.getTemporalDirectory() + "Padova-Asiago sn cat.txt";
		File f = new File(p);
		if (f.exists()) {
			double newdt = getdt(astro.msFrom1970(), f.lastModified());
			double dtElapsedFromLastDownload = getdt(System.currentTimeMillis(), f.lastModified());
			if (Math.abs(newdt) <= Math.abs(dt) || Math.abs(dtElapsedFromLastDownload) < MAXIMUM_DAYS_FOR_SUPERNOVAE)
				return p;
		}

		String query = Update.UPDATE_URL_PADOVA_ASIAGO_SN_CAT;
		GeneralQuery.queryFile(query, p, QUERY_TIMEOUT);

		return p;
		*/
	}

	/**
	 * Updates the data for novae writing the new file to the temporal directory.
	 * @param astro The date to show novae.
	 * @return Null if the data is already acceptable, null if the simulation date is too
	 * far from current date (so updating the file would not be acceptable too), and
	 * the path of the temporal file if the file already exists and it is
	 * acceptable or if it has been successfully downloaded. If not null
	 * the process of reading the temporal file can continue to draw the objects.
	 * @throws JPARSECException If an error occurs.
	 */
	public static synchronized String updateNovaeInTempDir(AstroDate astro) throws JPARSECException {
		if (isAcceptableDateForNovae(astro)) return null;
		if (APPLET_MODE) return "APPLET_MODE";

		double dt = getdt(astro.msFrom1970(), System.currentTimeMillis());

		String p = FileIO.getTemporalDirectory() + "galnovae.txt";
		File f = new File(p);
		if (f.exists()) {
			double newdt = getdt(astro.msFrom1970(), f.lastModified());
			double dtElapsedFromLastDownload = getdt(System.currentTimeMillis(), f.lastModified());
			if (Math.abs(newdt) <= Math.abs(dt) || Math.abs(dtElapsedFromLastDownload) < MAXIMUM_DAYS_FOR_NOVAE)
				return p;
		}

		String query = Update.UPDATE_URL_NOVAE;
		GeneralQuery.queryFile(query, p, QUERY_TIMEOUT);

		return p;
	}

	/**
	 * Updates the orbital elements of comets writing
	 * the new file to the temporal directory.
	 * @param astro The date to show comets.
	 * @return Null if the orbital elements in orbital_elements.jar is
	 * already acceptable, null if the simulation date is too far from
	 * current date (so updating the file would not be acceptable too), and
	 * the path of the temporal file if the file already exists and it is
	 * acceptable or if it has been successfully downloaded. If not null
	 * the process of reading the temp file can continue to draw the objects.
	 * @throws JPARSECException If an error occurs.
	 */
	public static synchronized String updateCometsInTempDir(AstroDate astro) throws JPARSECException {
		OrbitEphem.setCometsFromExternalFile(null);

		if (isAcceptableDateForComets(astro, true)) return null;

		double dt = getdt(astro.msFrom1970(), System.currentTimeMillis());
		if (dt < -MAXIMUM_DAYS_FROM_ELEMENTS_COMETS_ASTEROIDS || dt > MAXIMUM_DAYS_FROM_ELEMENTS_COMETS_ASTEROIDS) {
			return null;
		}

		if (APPLET_MODE) {
			if (appletOnLineModeCom) return null;
			String query = Update.UPDATE_URL_COMETS;
			try {
				String file[] = DataSet.toStringArray(GeneralQuery.query(query, QUERY_TIMEOUT), FileIO.getLineSeparator(), false);
				OrbitEphem.setCometsFromExternalFile(file);
				return "APPLET_MODE";
			} catch (Exception exc) {
				appletOnLineModeCom = true;
				return null;
			}
		}

		String p = FileIO.getTemporalDirectory() + "MPC_comets.txt";
		File f = new File(p);
		if (f.exists()) {
			double newdt = getdt(astro.msFrom1970(), f.lastModified());
			double dtElapsedFromLastDownload = getdt(System.currentTimeMillis(), f.lastModified());
			if (Math.abs(newdt) <= Math.abs(dt) || Math.abs(dtElapsedFromLastDownload) < MAXIMUM_DAYS_FROM_ELEMENTS_COMETS_ASTEROIDS_BETWEEN_TWO_QUERIES) {
				String file[] = DataSet.arrayListToStringArray(ReadFile.readAnyExternalFile(p));
				OrbitEphem.setCometsFromExternalFile(file);
				return p;
			}
		}

		String query = Update.UPDATE_URL_COMETS;
		GeneralQuery.queryFile(query, p, QUERY_TIMEOUT);

		// Check the format
		try {
			String file[] = DataSet.arrayListToStringArray(ReadFile.readAnyExternalFile(p));
			OrbitEphem.setCometsFromExternalFile(file);
		} catch (Exception exc) {
			FileIO.deleteFile(p);
			return null;
		}
		return p;
	}

	/**
	 * Updates the orbital elements of asteroids writing
	 * the new file to the temporal directory.
	 * @param astro The date to asteroids.
	 * @return Null if the orbital elements in orbital_elements.jar is
	 * already acceptable, null if the simulation date is too far from
	 * current date (so updating the file would not be acceptable too), and
	 * the path of the temporal file if the file already exists and it is
	 * acceptable or if it has been successfully downloaded. If not null
	 * the process of reading the temp file can continue to draw the objects.
	 * @throws JPARSECException If an error occurs.
	 */
	public static synchronized String updateAsteroidsInTempDir(AstroDate astro) throws JPARSECException {
		OrbitEphem.setAsteroidsFromExternalFile(null);

		if (isAcceptableDateForAsteroids(astro, true)) return null;

		double dt = getdt(astro.msFrom1970(), System.currentTimeMillis());
		if (dt < -MAXIMUM_DAYS_FROM_ELEMENTS_COMETS_ASTEROIDS || dt > MAXIMUM_DAYS_FROM_ELEMENTS_COMETS_ASTEROIDS) {
			return null;
		}

		String query = Update.UPDATE_URL_BRIGHT_ASTEROIDS;
		int year = astro.getYear();
		// MPC does not update asteroids since 2018, and Pallas and others are only listed in 2017
		if (year > 2018) year = 2018;
		query = DataSet.replaceAll(query, "2007", ""+year, false);

		if (APPLET_MODE) {
			if (appletOnLineModeAst) return null;
			try {
				String filea = GeneralQuery.query(query, QUERY_TIMEOUT);

				try {
					query = Update.UPDATE_URL_BRIGHT_ASTEROIDS;
					query = DataSet.replaceAll(query, "2007", ""+(year-1), false);
					String fileb = GeneralQuery.query(query, QUERY_TIMEOUT);

					filea = addAsteroids(filea, fileb);
				} catch (Exception exc) { }

				String file[] = DataSet.toStringArray(filea, FileIO.getLineSeparator(), false);
				OrbitEphem.setAsteroidsFromExternalFile(file);
				return "APPLET_MODE";
			} catch (Exception exc) {
				appletOnLineModeAst = true;
				return null;
			}
		}

		String p = FileIO.getTemporalDirectory() + "MPC_asteroids_bright.txt";
		File f = new File(p);
		// In case the file exists always assume it is more updated than the one inside the jar file,
		// so it can be updated by hand
		if (f.exists()) {
			//double newdt = getdt(astro.msFrom1970(), f.lastModified());
			//double dtElapsedFromLastDownload = getdt(System.currentTimeMillis(), f.lastModified());
			//if (Math.abs(newdt) <= Math.abs(dt) || Math.abs(dtElapsedFromLastDownload) < MAXIMUM_DAYS_FROM_ELEMENTS_COMETS_ASTEROIDS_BETWEEN_TWO_QUERIES) {
				String file[] = DataSet.arrayListToStringArray(ReadFile.readAnyExternalFile(p));
				OrbitEphem.setAsteroidsFromExternalFile(file);
				return p;
			//}
		}

		try {
			String filea = GeneralQuery.query(query, QUERY_TIMEOUT);

			try {
				query = Update.UPDATE_URL_BRIGHT_ASTEROIDS;
				query = DataSet.replaceAll(query, "2007", ""+(year-1), false);
				String fileb = GeneralQuery.query(query, QUERY_TIMEOUT);

				filea = addAsteroids(filea, fileb);
			} catch (Exception exc) {}

			WriteFile.writeAnyExternalFile(p, filea);

			//GeneralQuery.queryFile(query, p, QUERY_TIMEOUT);
		} catch (Exception exc) {
			try { FileIO.deleteFile(p); } catch (Exception exc1) {}
			return null;
		}

		// Check the format
		try {
			String file[] = DataSet.arrayListToStringArray(ReadFile.readAnyExternalFile(p));
			OrbitEphem.setAsteroidsFromExternalFile(file);
		} catch (Exception exc) {
			FileIO.deleteFile(p);
			return null;
		}
		return p;
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

		if (d2.length > 0) return f1 + DataSet.toString(d2, FileIO.getLineSeparator());
		return f1;
	}

	/**
	 * Updates the orbital elements of transneptunians writing
	 * the new file to the temporal directory.
	 * @param astro The date to show transneptunians.
	 * @return Null if the orbital elements in orbital_elements.jar is
	 * already acceptable, null if the simulation date is too far from
	 * current date (so updating the file would not be acceptable too), and
	 * the path of the temporal file if the file already exists and it is
	 * acceptable or if it has been successfully downloaded. If not null
	 * the process of reading the temp file can continue to draw the objects.
	 * @throws JPARSECException If an error occurs.
	 */
	public static synchronized String updateTransNeptuniansInTempDir(AstroDate astro) throws JPARSECException {
		OrbitEphem.setTransNeptuniansFromExternalFile(null);

		if (isAcceptableDateForTransNeptunians(astro, true)) return null;

		double dt = getdt(astro.msFrom1970(), System.currentTimeMillis());
		if (dt < -MAXIMUM_DAYS_FROM_ELEMENTS_COMETS_ASTEROIDS || dt > MAXIMUM_DAYS_FROM_ELEMENTS_COMETS_ASTEROIDS) {
			return null;
		}

		if (APPLET_MODE) {
			if (appletOnLineModeTran) return null;
			String query = Update.UPDATE_URL_DISTANT_BODIES;
			try {
				String file[] = DataSet.toStringArray(GeneralQuery.query(query, QUERY_TIMEOUT), FileIO.getLineSeparator(), false);
				OrbitEphem.setTransNeptuniansFromExternalFile(file);
				return "APPLET_MODE";
			} catch (Exception exc) {
				appletOnLineModeTran = true;
				return null;
			}
		}

		String p = FileIO.getTemporalDirectory() + "MPC_distant_bodies.txt";
		File f = new File(p);
		if (f.exists()) {
			double newdt = getdt(astro.msFrom1970(), f.lastModified());
			double dtElapsedFromLastDownload = getdt(System.currentTimeMillis(), f.lastModified());
			if (Math.abs(newdt) <= Math.abs(dt) || Math.abs(dtElapsedFromLastDownload) < MAXIMUM_DAYS_FROM_ELEMENTS_COMETS_ASTEROIDS_BETWEEN_TWO_QUERIES) {
				String file[] = DataSet.arrayListToStringArray(ReadFile.readAnyExternalFile(p));
				OrbitEphem.setTransNeptuniansFromExternalFile(file);
				return p;
			}
		}

		String query = Update.UPDATE_URL_DISTANT_BODIES;
		GeneralQuery.queryFile(query, p, QUERY_TIMEOUT);

		// Check the format
		try {
			String file[] = DataSet.arrayListToStringArray(ReadFile.readAnyExternalFile(p));
			OrbitEphem.setTransNeptuniansFromExternalFile(file);
		} catch (Exception exc) {
			FileIO.deleteFile(p);
			return null;
		}
		return p;
	}
}
