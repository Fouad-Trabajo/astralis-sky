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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.net.URLConnection;

import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.EphemerisElement.ALGORITHM;
import jparsec.ephem.EphemerisElement.COORDINATES_TYPE;
import jparsec.ephem.EphemerisElement.FRAME;
import jparsec.ephem.EphemerisElement.REDUCTION_METHOD;
import jparsec.ephem.Functions;
import jparsec.ephem.IAU2006;
import jparsec.ephem.Nutation;
import jparsec.ephem.PhysicalParameters;
import jparsec.ephem.Precession;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.event.LunarEvent;
import jparsec.ephem.moons.MoonEphem;
import jparsec.ephem.planets.imcce.Elp2000;
import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.io.Zip;
import jparsec.io.binaryFormat.Convertible;
import jparsec.io.binaryFormat.EEEI2EEEI;
import jparsec.io.binaryFormat.IEEE2EEEI;
import jparsec.io.binaryFormat.VAX2EEEI;
import jparsec.math.Constant;
import jparsec.math.matrix.Matrix;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.SiderealTime;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.time.TimeScale;
import jparsec.util.Configuration;
import jparsec.util.DataBase;
import jparsec.util.JPARSECException;

/**
 * A class to perform ephemeris calculations using JPL numerical integration
 * theories with ASCII files. The files for integrations DE200, DE403, DE405, 
 * DE406, DE413, DE414, DE422, DE424, and DE430 are provided in the optional 
 * dependency jpl_ephem.jar. They are provided to cover the time span from 
 * 1950 to 2050, or greater in some cases. Additional external files for these 
 * and any other integrations are supported in both ASCII and binary formats. 
 * They can be located in an external directory or, for ASCII files, also 
 * inside a compressed zip/jar file. The constructors allow to select the 
 * external path (a directory or an specific file), and for using this feature 
 * from {@linkplain Ephem} class you can set the path to 
 * {@linkplain Configuration#JPL_EPHEMERIDES_FILES_EXTERNAL_PATH}.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class JPLEphemeris {

	/** Latest JPL integration available in JPARSEC. Note there are more recent 
	 * integrations supported, but DE430 is currently the latest for which files 
	 * are provided with the dependencies available for downloading with JPARSEC. */
	public static final EphemerisElement.ALGORITHM LATEST = ALGORITHM.JPL_DE430;
	
	/**
	 * Invalid vector representing an invalid JPL ephemeris result (out of time span)
	 * for {@linkplain #getGeocentricPosition(double, Target.TARGET, double, boolean, ObserverElement)}.
	 */
	public static final double[] INVALID_VECTOR = { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };

	/**
	 * Value of the Moon secular acceleration ("/cy^2) for DE403 and DE404.
	 * The value is -25.8 as it appears in the IOM of JPL for DE403, but
	 * from the information of the IOM of JPL DE421 it seems that this
	 * value could be -26.06. See page 7 of
	 * http://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/planets/de421_lunar_ephemeris_and_orientation.pdf.
	 */
	public static final double MOON_SECULAR_ACCELERATION_DE403 = -25.8;
	/** Value of the Moon secular acceleration ("/cy^2) for DE405 and DE406. */
	public static final double MOON_SECULAR_ACCELERATION_DE405 = -25.7376;
	/** Value of the Moon secular acceleration ("/cy^2) for DE200. */
	public static final double MOON_SECULAR_ACCELERATION_DE200 = -23.8946;
	/** Value of the Moon secular acceleration ("/cy^2) for DE413 and DE414. Need check. */
	public static final double MOON_SECULAR_ACCELERATION_DE413 = -25.85;
	/**
	 * Value of the Moon secular acceleration ("/cy^2) for DE422.
	 * It requires confirmation. The value is for DE421 according to
	 * http://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/planets/de421_lunar_ephemeris_and_orientation.pdf.
	 */
	public static final double MOON_SECULAR_ACCELERATION_DE422 = -25.85;
	/**
	 * Value of the Moon secular acceleration ("/cy^2) for DE430.
	 * From http://ipnpr.jpl.nasa.gov/progress_report/42-196/196C.pdf.
	 */
	public static final double MOON_SECULAR_ACCELERATION_DE430 = -25.82;

	/** List of targets available. */
	private static final TARGET targets[] = new TARGET[] {TARGET.NOT_A_PLANET, TARGET.MERCURY, TARGET.VENUS,
			TARGET.Earth_Moon_Barycenter, TARGET.MARS, TARGET.JUPITER, TARGET.SATURN, TARGET.URANUS,
			TARGET.NEPTUNE, TARGET.Pluto, TARGET.Moon, TARGET.SUN, TARGET.Nutation, TARGET.Libration, TARGET.Solar_System_Barycenter};

	/** Maximum number of years allowed per JPL ASCII file. */
	private final static int MAX_YEARS_PER_FILE = 1000;

	/** Default format to read binary files: IEEE = little endian, EEEI = big endian. 
	 * Default is null to try to find out automatically. If a wrong format is given it may be 
	 * possible to correct it automatically. */
	public Convertible formatForBinaryFiles = null;
	/** ID for the ephemeris version. */
	private EphemerisElement.ALGORITHM jplID;
	/** Integer ID for the ephemeris version, used if the 
	 * JPL integration is not available in {@link EphemerisElement#algorithm}. */
	private int jplVersion;
	/** Interval between records. */
	private double jds;
	/** ncoeff parameter. */
	private int ncoeff;
	/** Names of constants. */
	private String cnam[];
	/** Constants values. */
	private String cval[];
	/** Number of coefficients for planets. */
	private int lpt[][];

	private String externalPath;
	private double[] ephemerisDates = new double[2];
	private double[] ephemerisCoefficients;
	private int readVersion = -1;
	private int numbers_per_interval;

	/** The Earth-Moon mass ratio. */
	private double emrat;
	/** The value assumed for the Astronomical Unit. */
	private double au;
	
	/**
	 * Default constructor for JPL version defined in class {@linkplain Configuration}.
	 * @throws JPARSECException If the integration version is not available
	 * (the header could not be read).
	 */
	public JPLEphemeris() throws JPARSECException {
		this(Configuration.JPL_DEFAULT_VERSION, Configuration.JPL_EPHEMERIDES_FILES_EXTERNAL_PATH);
	}

	/**
	 * Constructor for a given JPL version avalable in JPARSEC.
	 * @param jplID ID for the JPL ephemeris version.
	 * @throws JPARSECException If the integration version is not available
	 * (the header could not be read).
	 */
	public JPLEphemeris(EphemerisElement.ALGORITHM jplID)
	throws JPARSECException {
		this(jplID, Configuration.JPL_EPHEMERIDES_FILES_EXTERNAL_PATH);
	}

	/**
	 * Constructor for a given integration available for ephemerides in JPARSEC
	 * (defined in {@link EphemerisElement#algorithm}) and an external path for the files. 
	 * @param jplID ID for the JPL ephemeris version.
	 * @param externalPath Path for the external directory or zip/jar file with ASCII 
	 * files corresponding to the previous integration, or directly to the ASCII/binary 
	 * file to read. ASCII files must be named like ascmYEAR.xxx or
	 * ascpYEAR.xxx, where YEAR is the starting year of validity and
	 * xxx is the JPL ephemeris version. Files can be downloaded
	 * from ftp://ssd.jpl.nasa.gov/pub/eph/planets/ascii/. Set to null to
	 * use JPARSEC files if they are available (jpl_ephem.jar dependency).
	 * Note reading from compressed files is slow.
	 * @throws JPARSECException If the integration version is not available
	 * (the header could not be read).
	 */
	public JPLEphemeris(EphemerisElement.ALGORITHM jplID, String externalPath)
	throws JPARSECException {
		if (!jplID.isJPL()) throw new JPARSECException("This is not a JPL algorithm.");
		if (externalPath != null && !new File(externalPath).isFile() 
				&& !externalPath.endsWith(FileIO.getFileSeparator())) externalPath += FileIO.getFileSeparator();
		this.externalPath = externalPath;
		this.jplID = jplID;
		this.jplVersion = getJPLVersion();
		this.readHeader();
	}

	/**
	 * Constructor for a given integration not available in JPARSEC and a given 
	 * external path for the files.
	 * @param version Version number, 200 for DE200, 405 for DE405, and so on.
	 * @param externalPath Path for the external directory or zip/jar file with ASCII 
	 * files corresponding to the previous integration, or directly to the ASCII/binary 
	 * file to read. ASCII files must be named like ascmYEAR.xxx or
	 * ascpYEAR.xxx, where YEAR is the starting year of validity and
	 * xxx is the JPL ephemeris version. Files can be downloaded
	 * from ftp://ssd.jpl.nasa.gov/pub/eph/planets/ascii/. Set to null to
	 * use JPARSEC files if they are available (jpl_ephem.jar dependency).
	 * Note reading from compressed files is slow.
	 * @throws JPARSECException If the integration version is not available
	 * (the header could not be found or read).
	 */
	public JPLEphemeris(int version, String externalPath) throws JPARSECException {
		if (externalPath != null && !new File(externalPath).isFile()  
				&& !externalPath.endsWith(FileIO.getFileSeparator())) externalPath += FileIO.getFileSeparator();
		this.externalPath = externalPath;
		this.jplID = null;
		this.jplVersion = version;
		this.readHeader();		
	}
	
	/**
	 * Returns JPL ephemeris version of this instance.
	 * The returned value is not equal to {@linkplain JPLEphemeris#jplID}.
	 * For example, this method returns 200 for DE200.
	 * @return JPL version.
	 */
	public int getJPLVersion()
	{
		if (jplID == null) return jplVersion;
		String idNumber = jplID.name().substring(6);
		return Integer.parseInt(idNumber);
	}

	/**
	 * Returns JPL ephemeris id version of this instance.
	 * The returned value could be null in case the constructor 
	 * for an integer was used.
	 * @return JPL version id.
	 */
	public EphemerisElement.ALGORITHM getJPLVersionID() {
		return this.jplID;
	}

	/**
	 * Delete the coefficients read for the last ASCII file (which is always cached).
	 * This will free memory resources, but will force to read the ASCII file in the 
	 * next calculation.
	 */
	public void reset() {
		cnam = null;
		cval = null;
		lpt = null;
		ephemerisCoefficients = null;
		System.gc();
	}

	private String[] getCompatibleEphemeridesFile(double jd, boolean willRead) throws Exception {
		if (externalPath == null) return getCompatibleEphemeridesFileInternal(jd, willRead);
		String extFile[] = getCompatibleEphemeridesFileExternal(jd, willRead);
		if (extFile != null) return extFile;
		if (new File(externalPath).isFile() && FileIO.exists(externalPath)) {
			try {
				readExternalFileAsBinary(jd);
				return new String[] {externalPath};
			} catch (Exception exc) {
				JPARSECException.addWarning("Could not read external path as ascii or binary file, trying with internal dependencies");
			}
		}
		return getCompatibleEphemeridesFileInternal(jd, willRead);
	}

	private void readExternalFileAsBinary(double jd) throws Exception {
		RandomAccessFile file = new RandomAccessFile(new File(externalPath), "r");
		Convertible convert = formatForBinaryFiles;
		if (convert == null) convert = new IEEE2EEEI(); // Assume initially IEEE = little endian

		byte bint[] = new byte[4];
		byte bdouble[] = new byte[8];
		byte bchar84[] = new byte[12*7];
		byte bchar6[] = new byte[6];
		
		file.read(bchar84);
		String l1 = new String(bchar84);
		if (!l1.startsWith("JPL") && !l1.startsWith("INPOP")) {
			file.close();
			throw new JPARSECException("Not a binary JPL (or INPOP compatible with JPL) ephemeris file");
		}
		file.skipBytes(bchar84.length*2);

		if (l1.startsWith("INPOP")) { // INPOP usually constants another line of 84 bytes
			long pos = file.getFilePointer();
			file.read(bchar84);
			String findConstants = new String(bchar84).trim();
			if (!findConstants.equals("")) 
				file.seek(pos);
		}
		
		cnam = new String[400];
		for (int i=0; i<cnam.length; i++) {
			file.read(bchar6);
			cnam[i] = new String(bchar6).trim();
			if (cnam[i].equals("")) {
				file.skipBytes((399-i)*bchar6.length);
				break;
			}
		}

		file.read(bdouble);
		double jd0 = convert.readDouble(bdouble, 0);
		if (1.0 / jd0 > 1E100) {
			JPARSECException.addWarning("The file is not in IEEE (little endian) format, trying with EEEI (big endian)");
			convert = new EEEI2EEEI();
			jd0 = convert.readDouble(bdouble, 0);
			if (1.0 / jd0 > 1E100) {
				JPARSECException.addWarning("The file is not in EEEI (big endian) format, trying with VAX");
				convert = new VAX2EEEI();
				jd0 = convert.readDouble(bdouble, 0);
				if (1.0 / jd0 > 1E100) {
					file.close();
					throw new JPARSECException("Could not identify the format of the file");
				}
			}				
		}

		file.read(bdouble);
		double jdf = convert.readDouble(bdouble, 0);
		
		if (jd < jd0 || jd > jdf) {
			file.close();
			throw new JPARSECException("Invalid date");
		}

		ephemerisDates = new double[] {jd0, jdf};
		file.read(bdouble);
		jds = convert.readDouble(bdouble, 0);

		file.read(bint);
		int ncon = convert.readInt(bint, 0);

		file.read(bdouble);
		au = convert.readDouble(bdouble, 0);

		file.read(bdouble);
		emrat = convert.readDouble(bdouble, 0);

		lpt = new int[3][13];
		int numde = 0;
		for (int i=0;i<lpt[0].length;i++) {
			if (i == lpt[0].length-1) {
				file.read(bint);
				numde = convert.readInt(bint, 0);
			}
			for (int j=0;j<lpt.length;j++) {
				file.read(bint);
				lpt[j][i] = convert.readInt(bint, 0);
			}
		}
		
		if (ncon > 400) {
			String cnamAdd[] = new String[ncon-400];
			for (int i=400; i<ncon; i++) {
				file.read(bchar6);
				cnamAdd[i-400] = new String(bchar6).trim();
			}
			cnam = DataSet.addStringArray(cnam, cnamAdd);
		}
		
		//if (!l1.startsWith("INPOP")) {
			int rpt[] = new int[3];
			for (int i=0; i<rpt.length; i++) {
				file.read(bint);
				rpt[i] = convert.readInt(bint, 0);
			}
			int tpt[] = new int[3];
			for (int i=0; i<tpt.length; i++) {
				file.read(bint);
				tpt[i] = convert.readInt(bint, 0);
			}
		//}
		
		double cval0 = 0;
		while (true) {
			file.read(bdouble);
			cval0 = convert.readDouble(bdouble, 0);
			if (cval0 == numde) break;
			if (l1.startsWith("INPOP") && cval0 != 0) break;
		}

		cval = new String[ncon];
		cval[0] = new Double(cval0).toString();
		for (int i=1; i<cval.length; i++) {
			file.read(bdouble);
			cval[i] = new Double(convert.readDouble(bdouble, 0)).toString();
		}
		
		if (l1.startsWith("INPOP")) { // Check AU and EMRAT are in list of constants
			int iau = DataSet.getIndex(cnam, "AU");
			int iem = DataSet.getIndex(cnam, "EMRAT");
			if (iau < 0) {
				cnam = DataSet.addStringArray(cnam, "AU");
				cval = DataSet.addStringArray(cval, Double.toString(au));
			}
			if (iem < 0) {
				cnam = DataSet.addStringArray(cnam, "EMRAT");
				cval = DataSet.addStringArray(cval, Double.toString(emrat));
			}
		}
		
		int kernelSize = 4;
		for (int i=0; i<lpt[0].length; i++) {
			kernelSize += 2 * lpt[1][i] * lpt[2][i] * ((i == 11) ? 2 : 3);
		}
		ncoeff = kernelSize / 2;
		
		long startPos = file.getFilePointer();
		for (int i=0; i<2000; i++) {
			file.read(bdouble);
			double v = convert.readDouble(bdouble, 0);
			if (v == jd0) {
				startPos = file.getFilePointer() - bdouble.length;
				break;
			}
		}

		int blockMax = (int) ((jdf - jd0) / jds);
		int block = (int) ((jd - jd0) / jds);
		int blockSize = ncoeff * bdouble.length;
		int readBlocksBeforeAfter = 50;
		if (block < readBlocksBeforeAfter) readBlocksBeforeAfter = block;
		if (blockMax-block < readBlocksBeforeAfter) readBlocksBeforeAfter = blockMax-block;
		block -= readBlocksBeforeAfter;
		file.seek(startPos);
		file.skipBytes(blockSize * block);
		
		numbers_per_interval = ncoeff - 2;
		ephemerisCoefficients = new double[1 + numbers_per_interval*(readBlocksBeforeAfter+1)*2];
		
		int index = 1;
		for (int b=0; b<=readBlocksBeforeAfter*2; b++) {
			file.read(bdouble);
			double jdStart = convert.readDouble(bdouble, 0);
			file.read(bdouble);
			double jdEnd = convert.readDouble(bdouble, 0);
			if (b == 0) ephemerisDates[0] = jdStart;
			ephemerisDates[1] = jdEnd;
			for (int i=0; i<numbers_per_interval; i++) {
				file.read(bdouble);
				ephemerisCoefficients[index] = convert.readDouble(bdouble, 0);
				index++;
			}
		}
		file.close();
		if (jd < ephemerisDates[0] || jd >= ephemerisDates[1]) 
			throw new JPARSECException("Corrupt file ? WRONG block for input JD "+jd);
		readVersion = numde;
		if (l1.startsWith("INPOP")) {
			jplVersion = numde;
			jplID = null;
		}
	}

	private String[] getCompatibleEphemeridesFileInternal(double jd, boolean willRead) throws Exception {
		String filePath = FileIO.DATA_JPL_EPHEM_DIRECTORY+"de"+this.getJPLVersion()+Zip.ZIP_SEPARATOR;
		String f[] = DataSet.arrayListToStringArray(ReadFile.getResourceFiles(filePath));
		String end = "."+getJPLVersion();
		String enc = ReadFile.ENCODING_UTF_8;
		for (int i=0; i<f.length; i++) {
			String fn = FileIO.getFileNameFromPath(f[i]);
			
			if (!fn.endsWith(end)) continue;
			if (!fn.startsWith("asc")) continue;
			//String year = fn.substring(3);
			//year = year.substring(0, year.indexOf("."));
			//int s = 1;
			//if (year.startsWith("m")) s = -1;
			//int fYear = s * Integer.parseInt(year.substring(1));
			
			String first2[] = DataSet.arrayListToStringArray(ReadFile.readResourceFirstNlines(f[i], enc, 2));
			first2[1] = DataSet.replaceAll(first2[1], "D", "E", true);
			double jd0 = Double.parseDouble(FileIO.getField(1, first2[1].trim(), " ", true).trim());
			if (jd < jd0) continue;

			int nPerBlock = Integer.parseInt(FileIO.getField(2, first2[0].trim(), " ", true).trim());
			nPerBlock = (nPerBlock + 2) / 3 + 1;

			int n = ReadFile.readResourceGetNumberOfLines(f[i], enc);
			int blocks = n / nPerBlock;
			// Following faster but not reliable, unfortunately line separator is sometimes \n\r (2 bytes)
			//long size = ReadFile.getSize(new ReadFile(f[i], enc).getResourceAsStream());
			//int bytesPerBlock = (nPerBlock - 1) * (first2[1].length() + 1) + first2[0].length() + 1;
			//int blocks = (int) (size / bytesPerBlock + 0.5);
			//int n = blocks * nPerBlock;

			double jdi = Double.parseDouble(FileIO.getField(2, first2[1].trim(), " ", true).trim());
			double jdfApprox = jd0 + (jdi - jd0) * blocks;
			if (jd > jdfApprox) continue;

			String lastBlock[] = DataSet.arrayListToStringArray(ReadFile.readResourceSomeLines(f[i], enc, n-nPerBlock, n-nPerBlock+2));
			lastBlock[1] = DataSet.replaceAll(lastBlock[1], "D", "E", true);
			double jdf = Double.parseDouble(FileIO.getField(2, lastBlock[1].trim(), " ", true).trim());
			
			if (jd < jdf) {
				if (willRead) {
					ephemerisDates[0] = jd0;
					ephemerisDates[1] = jdf;
				}
				return new String[] {f[i]};
			}
		}
		return null;
	}
	private String[] getCompatibleEphemeridesFileExternal(double jd, boolean willRead) throws Exception {
		if (externalPath == null) return null;
		
		if (externalPath.endsWith(".jar") || externalPath.endsWith(".zip")) {
			String f[] = Zip.unZipFile(externalPath, null, false);
			String end = "."+getJPLVersion();
			//String enc = ReadFile.ENCODING_UTF_8;
			String separator = FileIO.getLineSeparator();
			for (int i=0; i<f.length; i++) {
				String fn = FileIO.getFileNameFromPath(f[i]);
				
				if (!fn.endsWith(end)) continue;
				if (!fn.startsWith("asc")) continue;
				//String year = fn.substring(3);
				//year = year.substring(0, year.indexOf("."));
				//int s = 1;
				//if (year.startsWith("m")) s = -1;
				//int fYear = s * Integer.parseInt(year.substring(1));
				
				byte file[] = Zip.unZipFileGetFileInside(externalPath, f[i]);
				String dataS = new String(file);
				String all[] = DataSet.toStringArray(dataS, separator, false);
				
				all[1] = DataSet.replaceAll(all[1], "D", "E", true);
				double jd0 = Double.parseDouble(FileIO.getField(1, all[1].trim(), " ", true).trim());
				if (jd < jd0) continue;

				int n = all.length-1;
				int nPerBlock = Integer.parseInt(FileIO.getField(2, all[0].trim(), " ", true).trim());
				nPerBlock = (nPerBlock + 2) / 3 + 1;
				int blocks = n / nPerBlock;
				double jdi = Double.parseDouble(FileIO.getField(2, all[1].trim(), " ", true).trim());
				double jdfApprox = jd0 + (jdi - jd0) * blocks;
				if (jd > jdfApprox) continue;

				String lastBlock[] = DataSet.getSubArray(all, n-nPerBlock, n-nPerBlock+2);
				lastBlock[1] = DataSet.replaceAll(lastBlock[1], "D", "E", true);
				double jdf = Double.parseDouble(FileIO.getField(2, lastBlock[1].trim(), " ", true).trim());

				if (jd >= jd0 && jd < jdf) {
					if (willRead) {
						ephemerisDates[0] = jd0;
						ephemerisDates[1] = jdf;
					}
					return all;
				}
			}
			return null;
		}
		
		String filePath = externalPath;
		String f[] = null;
		if (new File(externalPath).isFile()) {
			if (!filePath.endsWith("."+getJPLVersion())) { // Search in entire directory if input file is probably wrong
				filePath = FileIO.getDirectoryFromPath(filePath);
				f = FileIO.getFiles(filePath);
			} else {
				f = new String[] {filePath}; // Use only the path to the provided file
			}
		} else {
			f = FileIO.getFiles(filePath);
		}
		
		String end = "."+getJPLVersion();
		String enc = ReadFile.ENCODING_UTF_8;
		for (int i=0; i<f.length; i++) {
			String fn = FileIO.getFileNameFromPath(f[i]);
			
			if (!fn.endsWith(end)) continue;
			if (!fn.startsWith("asc")) continue;
			//String year = fn.substring(3);
			//year = year.substring(0, year.indexOf("."));
			//int s = 1;
			//if (year.startsWith("m")) s = -1;
			//int fYear = s * Integer.parseInt(year.substring(1));
			
			String first2[] = DataSet.arrayListToStringArray(ReadFile.readAnyExternalFileFirstNlines(f[i], 2, enc));
			first2[1] = DataSet.replaceAll(first2[1], "D", "E", true);
			String val = FileIO.getField(1, first2[1].trim(), " ", true).trim();
			if (!DataSet.isDoubleStrictCheck(val)) continue;
			double jd0 = Double.parseDouble(val);
			if (jd < jd0) continue;

			int n = ReadFile.readAnyExternalFileAndReturnNumberOfLines(f[i]);
			int nPerBlock = Integer.parseInt(FileIO.getField(2, first2[0].trim(), " ", true).trim());
			nPerBlock = (nPerBlock + 2) / 3 + 1;
			int blocks = n / nPerBlock;
			double jdi = Double.parseDouble(FileIO.getField(2, first2[1].trim(), " ", true).trim());
			double jdfApprox = jd0 + (jdi - jd0) * blocks;
			if (jd > jdfApprox) continue;

			String lastBlock[] = DataSet.arrayListToStringArray(ReadFile.readAnyExternalFileSomeLines(f[i], n-nPerBlock, n-nPerBlock+2, enc));
			lastBlock[1] = DataSet.replaceAll(lastBlock[1], "D", "E", true);
			double jdf = Double.parseDouble(FileIO.getField(2, lastBlock[1].trim(), " ", true).trim());

			if (jd >= jd0 && jd < jdf) {
				if (willRead) {
					ephemerisDates[0] = jd0;
					ephemerisDates[1] = jdf;
				}
				return new String[] {f[i]};
			}
		}
		return null;
	}
	
	/**
	 * Returns if the ephemerides file is available or not.
	 * @param jd Julian day of the calculations, file name depends on this.
	 * @return True or false.
	 */
	public boolean isAvailable(double jd) {
		try {
			if (getCompatibleEphemeridesFile(jd, false) != null) return true;
		} catch (Exception exc) {}
		return false;
	}
	
	private void readHeader() throws JPARSECException {
		int version = this.getJPLVersion();
		String filePath = FileIO.DATA_JPL_EPHEM_DIRECTORY+"de" + version + Zip.ZIP_SEPARATOR;
		// Header file provided by JPARSEC. Since the dates and years should be typed in this class
		// it is not allowed to read an external header file, so only JPL integrations listed in this class
		// are supported.

		filePath += "header." + version;

		String file[] = null;
		// Read header from dependencies or from external directory if dependencies are not available
		try {
			file = DataSet.arrayListToStringArray(ReadFile.readResource(filePath));
		} catch (Exception exc) {
			if (externalPath != null) {
				filePath = externalPath + "header." + version;
				if (new File(externalPath).isFile()) filePath = FileIO.getDirectoryFromPath(externalPath) + "header." + version;
				try {
					file = DataSet.arrayListToStringArray(ReadFile.readAnyExternalFile(filePath));
				} catch (Exception exc2) {
					if (!new File(externalPath).isFile()) throw new JPARSECException("Could not read the header. Only binary files are allowed without a header");
					return;
				}
			}
		}

		for (int i=0; i<file.length; i++)
		{
			if (file[i].startsWith("KSIZE=")) {
				//this.ksize = Integer.parseInt(FileIO.getField(2, file[i], " "));
				this.ncoeff = Integer.parseInt(FileIO.getField(4, file[i], " ", true));
			}
			int group = -1;
			if (file[i].startsWith("GROUP")) group = Integer.parseInt(FileIO.getField(2, file[i], " ", true).trim());
			
			if (group == 1010) i += 4;
			if (group == 1030) {
				String data = file[i+2];
				//this.jdi = Double.parseDouble(FileIO.getField(1, data, " "));
				//this.jdf = Double.parseDouble(FileIO.getField(2, data, " "));
				this.jds = Double.parseDouble(FileIO.getField(3, data, " ", true));
				i += 2;
			}
			if (group == 1040) {
				String data = file[i+2].trim();
				int n = Integer.parseInt(data);
				i = i + 3;
				int columns = FileIO.getNumberOfFields(file[i].trim(), " ", true);
				double frac = (double) n / (double) columns;
				int rows = (int) frac;
				if (rows != frac) rows ++;
				int index = -1;
				cnam = new String[n];
				for (int j=0; j<rows; j++)
				{
					data = file[i+j];
					for (int k=0; k<columns; k++)
					{
						index ++;
						if (index < n) cnam[index] = FileIO.getField(k+1, data, " ", true);
					}
				}
				i = i + rows - 1;
			}
			if (group == 1041) {
				String data = file[i+2].trim();
				int n = Integer.parseInt(data);
				i = i + 3;
				int columns = FileIO.getNumberOfFields(file[i].trim(), " ", true);
				double frac = (double) n / (double) columns;
				int rows = (int) frac;
				if (rows != frac) rows ++;
				int index = -1;
				cval = new String[n];
				for (int j=0; j<rows; j++)
				{
					data = file[i+j];
					for (int k=0; k<columns; k++)
					{
						index ++;
						if (index < n) cval[index] = DataSet.replaceAll(FileIO.getField(k+1, data, " ", true), "D", "E", false);
					}
				}
				i = i + rows - 1;
			}
			if (group == 1050) {
				i = i + 2;
				String data = file[i];
				int columns = FileIO.getNumberOfFields(file[i].trim(), " ", true);
				int n = columns*3;
				double frac = (double) n / (double) columns;
				int rows = (int) frac;
				if (rows != frac) rows ++;
				lpt = new int[3][columns];
				for (int j=0; j<rows; j++)
				{
					data = file[i+j];
					for (int k=0; k<columns; k++)
					{
						lpt[j][k] = Integer.parseInt(FileIO.getField(k+1, data, " ", true));
					}
				}
				i = i + rows - 1;
			}
		}

		this.emrat = Double.parseDouble(getConstant("EMRAT"));
		this.au = Double.parseDouble(getConstant("AU"));
		numbers_per_interval = this.ncoeff-2;
	}

	/**
	 * Returns a given constant from the header for the JPL integration 
	 * defined by this instance.
	 * @param id Identifier (name) of the variable.
	 * @return The value as a String.
	 * @throws JPARSECException If the constant is not found.
	 */
	public String getConstant(String id) throws JPARSECException {
		int index = DataSet.getIndex(this.cnam, id);
		if (index >= 0) return this.cval[index];
		throw new JPARSECException("Constant "+id+" not found");
	}
	
	/**
	 * Returns the list of constant that appear in the header for the 
	 * JPL integration defined by this instance.
	 * @return List of constants.
	 */
	public String[] getConstants() {
		return cnam.clone();
	}
	
	/** Returns the inverse of c in days / AU */
	private double getLightTime() {
		try {
			double c = Double.parseDouble(getConstant("CLIGHT"));
			double lt = au / (c * Constant.SECONDS_PER_DAY);
			double rel = lt / Constant.LIGHT_TIME_DAYS_PER_AU;
			if (rel < 1) rel = 1.0 / rel;
			if (rel * 100 < 101) return lt; // If it is off by > 1% respect the constant, assume it is wrong
		} catch (Exception exc) { }
		return Constant.LIGHT_TIME_DAYS_PER_AU;
	}
	
	/**
	 * Uses this instance of {@linkplain JPLEphemeris} to set the values used in this
	 * integration for the field {@linkplain TARGET#relativeMass} for the planets 
	 * (Mercury to Neptune plus Pluto).<P>
	 * This will directly call {@linkplain TARGET#setPlanetaryMassFromJPLEphemeris(JPLEphemeris)},
	 * it is cloned from there so that you will find the method also here.
	 * @throws JPARSECException If an error occurs.
	 */
	public void setPlanetaryMassFromJPLEphemeris() throws JPARSECException {
		TARGET.setPlanetaryMassFromJPLEphemeris(this);
	}
	
	/**
	 * Calculate ephemeris, providing full data. This method uses JPL
	 * ephemeris. The position of the Moon will contains the librations
	 * computed using JPL ephemerides in case the integration is
	 * one of the supported in {@linkplain LunarEvent#getJPLMoonLibrations(TimeElement, ObserverElement, EphemerisElement, LocationElement)},
	 * otherwise Eckhardt's theory will be used. In case of lunar ephemerides you may also want
	 * to correct it from center of mass to geometric center by means of
	 * {@linkplain Elp2000#fromMoonBarycenterToGeometricCenter(TimeElement, ObserverElement, EphemerisElement, EphemElement)}.
	 *
	 * @param time Time object containing the date.
	 * @param obs Observer object containing the observer position.
	 * @param eph Ephemeris object with the target and ephemeris
	 *        properties.
	 * @return Ephem object containing ephemeris data. Rise, set, transit
	 * times and maximum elevation fields are not computed in this method, use
	 * {@linkplain Ephem} class for that.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public EphemElement getJPLEphemeris(TimeElement time, // Time Element
			ObserverElement obs, // Observer Element
			EphemerisElement eph) // Ephemeris Element
			throws JPARSECException
	{
		// Obtain julian day in Barycentric Dynamical Time
		double JD_TDB = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		EphemElement ephem_elem = this.JPLEphem(time, obs, eph, JD_TDB, true, true);

		/* Physical ephemeris */
		EphemerisElement new_eph = eph.clone();
		new_eph.ephemType = COORDINATES_TYPE.APPARENT;
		new_eph.equinox = EphemerisElement.EQUINOX_OF_DATE;
		EphemElement ephem_elem2 = ephem_elem;
		if (eph.ephemType != EphemerisElement.COORDINATES_TYPE.APPARENT || eph.equinox != EphemerisElement.EQUINOX_OF_DATE)
			ephem_elem2 = this.JPLEphem(time, obs, new_eph, JD_TDB, true, true);
		new_eph.targetBody = TARGET.SUN;
		EphemElement ephemSun = this.JPLEphem(time, obs, new_eph, JD_TDB, false, false);

		ephem_elem2 = PhysicalParameters.physicalParameters(JD_TDB, ephemSun, ephem_elem2, obs, eph);
		PhysicalParameters.setPhysicalParameters(ephem_elem, ephem_elem2, time, obs, eph);

		/* Horizontal coordinates */
		if (eph.isTopocentric)
			ephem_elem = Ephem.horizontalCoordinates(time, obs, eph, ephem_elem);

		/* Set coordinates to the output equinox */
		if (EphemerisElement.EQUINOX_OF_DATE != eph.equinox)
			ephem_elem = Ephem.toOutputEquinox(ephem_elem, eph, JD_TDB);

		ephem_elem.name = eph.targetBody.getName();

		/* Obtain accurate lunar orientation, if possible */
		if (obs.getMotherBody() == TARGET.EARTH && eph.targetBody == TARGET.Moon &&
				(eph.algorithm.isJPL() && eph.algorithm != ALGORITHM.JPL_DE200)	) {
			double lib[] = LunarEvent.getJPLMoonLibrations(time, obs, eph, ephem_elem.getEquatorialLocation());
			ephem_elem.longitudeOfCentralMeridian = lib[0];
			ephem_elem.positionAngleOfPole = lib[1];
			ephem_elem.positionAngleOfAxis = lib[2];
		}

		return ephem_elem;
	}

	private EphemElement JPLEphem(TimeElement time, // Time Element
			ObserverElement obs, // Observer Element
			EphemerisElement eph, // Ephemeris Element
			double JD_TDB, boolean addGCRS, boolean addOffset)
	throws JPARSECException {
		if ((!eph.targetBody.isPlanet() && eph.targetBody != TARGET.Moon &&
				eph.targetBody != TARGET.Earth_Moon_Barycenter &&
				eph.targetBody != TARGET.SUN && eph.targetBody != TARGET.Pluto) ||
				((eph.targetBody == TARGET.EARTH) && obs.getMotherBody() == TARGET.EARTH))
			throw new JPARSECException("target object is invalid.");

		/** Use the value specific or consistent with the integration used. */
		double LIGHT_TIME_DAYS_PER_AU = getLightTime();
		
		// Check Ephemeris object
		if (!EphemerisElement.checkEphemeris(eph) || eph.algorithm.name().indexOf("JPL") < 0)
			throw new JPARSECException("invalid ephemeris object. "+eph.algorithm);

		// Obtain geocentric position
		double geo_eq[] = this.getGeocentricPosition(JD_TDB, eph.targetBody, 0.0, addOffset, obs);
		if (Functions.equalVectors(geo_eq, JPLEphemeris.INVALID_VECTOR))
			throw new JPARSECException(
					"error during calculations. Resulting vector invalid.");

		// Obtain topocentric light_time
		LocationElement loc = LocationElement.parseRectangularCoordinates(geo_eq);
		double light_time = loc.getRadius() * LIGHT_TIME_DAYS_PER_AU;

		if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.GEOMETRIC)
			light_time = 0.0;
		if (eph.ephemType != EphemerisElement.COORDINATES_TYPE.GEOMETRIC) // && eph.targetBody != TARGET.SUN)
		{
			double topo[] = obs.topocentricObserverICRF(time, eph);
			geo_eq = this.getGeocentricPosition(JD_TDB, eph.targetBody, light_time, addOffset, obs);
			double light_time_corrected = Ephem.getTopocentricLightTime(geo_eq, topo, eph);
			// Iterate to obtain correct light time and geocentric position.
			// Typical difference in light time is 0.1 seconds. Iterate to
			// a precision up to 1E-6 seconds.

			do
			{
				light_time = light_time_corrected;
				geo_eq = this.getGeocentricPosition(JD_TDB, eph.targetBody, light_time_corrected, addOffset, obs);
				light_time_corrected = Ephem.getTopocentricLightTime(geo_eq, topo, eph);
				// (A relativistic effect of about 1E-10 AU is neglected in the light time calculation)
			} while (Math.abs(light_time - light_time_corrected) > (1.0E-6 / Constant.SECONDS_PER_DAY));
			light_time = light_time_corrected;
		}

		// Obtain light time to Sun (first order approx)
		double geo_sun_0[] = this.getGeocentricPosition(JD_TDB, TARGET.SUN, 0.0, false, obs);
		double lightTimeS = 0.0;
		if (eph.ephemType != EphemerisElement.COORDINATES_TYPE.GEOMETRIC) {
			lightTimeS = light_time;
			if (eph.targetBody != TARGET.SUN) {
				LocationElement locS = LocationElement.parseRectangularCoordinates(geo_sun_0);
				lightTimeS = locS.getRadius() * LIGHT_TIME_DAYS_PER_AU;
			}
		}
		double baryc[] = this.getGeocentricPosition(JD_TDB, TARGET.Solar_System_Barycenter, 0.0, false, obs);

		// Obtain heliocentric equatorial coordinates, J2000. Note JPL ephems are respect to
		// Solar system barycenter, we will need to subtract the position of the Sun here
		double helio_object[] = this.getPositionAndVelocity(JD_TDB - light_time, eph.targetBody);
		if (addOffset) {
			Object o = DataBase.getData("offsetPosition", true);
			if (o != null) {
				double[] planetocentricPositionOfTargetSatellite = (double[]) o;
				helio_object = Functions.sumVectors(helio_object, planetocentricPositionOfTargetSatellite);
			}
		}

		if (eph.targetBody != TARGET.Moon)
		{
			LocationElement locP = LocationElement.parseRectangularCoordinates(helio_object);
			double lightTimeP = locP.getRadius() * LIGHT_TIME_DAYS_PER_AU;
			double helio_object_sun[] = this.getPositionAndVelocity(JD_TDB - light_time - lightTimeP, TARGET.SUN);
			helio_object = Functions.substract(helio_object, helio_object_sun);
		} else {
			double[] geo_sun_ltS = this.getGeocentricPosition(JD_TDB, TARGET.SUN, lightTimeS, false, obs);
			helio_object = Functions.substract(helio_object, geo_sun_ltS);
			if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.ASTROMETRIC) {
				geo_eq = Ephem.aberration(new double[] {-geo_eq[0], -geo_eq[1], -geo_eq[2], 0, 0, 0}, baryc, light_time);
				geo_eq = Functions.scalarProduct(geo_eq, -1.0);
			}
		}

		// Correct for solar deflection and aberration
		if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT)
		{
			if (eph.preferPrecisionInEphemerides) {
				double sun[] = this.getPositionAndVelocity(JD_TDB - lightTimeS, TARGET.SUN);
				//geo_eq = Ephem.solarDeflection(geo_eq, geo_sun_0, Functions.substract(helio_object, sun));
				geo_eq = Ephem.solarAndPlanetaryDeflection(geo_eq, geo_sun_0, Functions.substract(helio_object, sun),
					new TARGET[] {TARGET.JUPITER}, JD_TDB, false, obs);
			}
			if (obs.getMotherBody() != TARGET.EARTH || eph.targetBody != TARGET.Moon)
				geo_eq = Ephem.aberration(geo_eq, baryc, light_time);

			if (addGCRS) DataBase.addData("GCRS", geo_eq, true);
		} else {
			if (addGCRS) DataBase.addData("GCRS", null, true);
		}

		/* Correction to output frame. */
		if (this.getJPLVersion() >= 403)
		{
			if (this.getJPLVersion() == 403) {
				// Rotate DE403 into ICRF following Folkner 1994 and Chernetenko 2007
				double ang1 = -0.1 * 0.001 * Constant.ARCSEC_TO_RAD;
				double ang2 = 3 * 0.001 * Constant.ARCSEC_TO_RAD;
				double ang3 = -5.2 * 0.001 * Constant.ARCSEC_TO_RAD;
				Matrix m = Matrix.getR1(ang1).times(Matrix.getR2(ang2).times(Matrix.getR3(ang3)));
				geo_eq = m.times(new Matrix(DataSet.getSubArray(geo_eq, 0, 2))).getColumn(0);
				helio_object = m.times(new Matrix(DataSet.getSubArray(helio_object, 0, 2))).getColumn(0);
			}

			geo_eq = Ephem.toOutputFrame(geo_eq, FRAME.ICRF, eph.frame);
			helio_object = Ephem.toOutputFrame(helio_object, FRAME.ICRF, eph.frame);
		} else {
			// FIXME: Not sure if the following rotation to FK5 is required. It seems so, but results
			// from DE200 agree better with VSOP87 and ELP2000 if this is commented.
			geo_eq = meanEquatorialDE200ToFK5(geo_eq);
			helio_object = meanEquatorialDE200ToFK5(helio_object);

			geo_eq = Ephem.toOutputFrame(geo_eq, FRAME.FK5, eph.frame);
			helio_object = Ephem.toOutputFrame(helio_object, FRAME.FK5, eph.frame);
		}

		double geo_date[];
		if (eph.frame == FRAME.FK4) {
			// Transform from B1950 to mean equinox of date
			 geo_date = Precession.precess(Constant.B1950, JD_TDB, geo_eq, eph);
			 helio_object = Precession.precess(Constant.B1950, JD_TDB, helio_object, eph);
		} else {
			// Transform from ICRS/J2000 to mean equinox of date
			geo_date = Precession.precessFromJ2000(JD_TDB, geo_eq, eph);
			helio_object = Precession.precessFromJ2000(JD_TDB, helio_object, eph);
		}

		// Get heliocentric ecliptic position
		LocationElement loc_elem = LocationElement.parseRectangularCoordinates(Ephem.equatorialToEcliptic(helio_object, JD_TDB, eph));

		// Mean equatorial to true equatorial: correct for nutation
		double true_eq[] = geo_date;
		if (obs.getMotherBody() == TARGET.EARTH) {
			if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT)
				true_eq = Nutation.nutateInEquatorialCoordinates(JD_TDB, eph, geo_date, true);

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
		if (obs.getMotherBody() != TARGET.NOT_A_PLANET)
			true_eq = Ephem.getPositionFromBody(LocationElement.parseRectangularCoordinates(true_eq), time, obs, eph).getRectangularCoordinates();

		// Get equatorial coordinates
		LocationElement ephem_loc = LocationElement.parseRectangularCoordinates(true_eq);

		// Set preliminary results
		EphemElement ephem_elem = new EphemElement();
		ephem_elem.rightAscension = ephem_loc.getLongitude();
		ephem_elem.declination = ephem_loc.getLatitude();
		ephem_elem.distance = ephem_loc.getRadius();
		ephem_elem.heliocentricEclipticLongitude = loc_elem.getLongitude();
		ephem_elem.heliocentricEclipticLatitude = loc_elem.getLatitude();
		ephem_elem.lightTime = (float) light_time;
		// Note distances are apparent, not true
		ephem_elem.distanceFromSun = loc_elem.getRadius();

		if (eph.targetBody == TARGET.SUN) ephem_elem.heliocentricEclipticLatitude = ephem_elem.heliocentricEclipticLongitude =
			ephem_elem.distanceFromSun = 0;

		/* Topocentric correction */
		if (eph.isTopocentric)
			ephem_elem = Ephem.topocentricCorrection(time, obs, eph, ephem_elem);

		return ephem_elem;
	}

	/**
	 * Transforms J2000 mean equatorial (dynamical equinox) coordinates into FK5.
	 * Specific to this theory (class) only for DE200. Rotation is performed
	 * according to IMCCE documentation, see
	 * http://www.imcce.fr/en/ephemerides/generateur/ephepos/ephemcc_doc.ps.gz.
	 *
	 * @param position Equatorial coordinates (x, y, z) or (x, y, z, vx, vy, vz)
	 *        from DE200.
	 * @return Equatorial FK5 coordinates.
	 */
	private static double[] meanEquatorialDE200ToFK5(double position[])
	{
		double RotM[][] = new double[4][4];
		double out_pos[] = new double[3];
		double out_vel[] = new double[3];

		// http://www.imcce.fr/en/ephemerides/generateur/ephepos/ephemcc_doc.ps.gz
		RotM[1][1] = 1.000000000000;
		RotM[1][2] = -0.000000028604007;
		RotM[1][3] = 0.000000000000005;
		RotM[2][1] = 0.000000028604007;
		RotM[2][2] = 0.999999999999984;
		RotM[2][3] = -0.000000175017739;
		RotM[3][1] = 0.000000000000;
		RotM[3][2] = 0.000000175017739;
		RotM[3][3] = 0.999999999999985;

		// Apply rotation
		out_pos[0] = RotM[1][1] * position[0] + RotM[1][2] * position[1] + RotM[1][3] * position[2]; // x
		out_pos[1] = RotM[2][1] * position[0] + RotM[2][2] * position[1] + RotM[2][3] * position[2]; // y
		out_pos[2] = RotM[3][1] * position[0] + RotM[3][2] * position[1] + RotM[3][3] * position[2]; // z
		if (position.length > 3)
		{
			out_vel[0] = RotM[1][1] * position[3] + RotM[1][2] * position[4] + RotM[1][3] * position[5]; // vx
			out_vel[1] = RotM[2][1] * position[3] + RotM[2][2] * position[4] + RotM[2][3] * position[5]; // vy
			out_vel[2] = RotM[3][1] * position[3] + RotM[3][2] * position[4] + RotM[3][3] * position[5]; // vz

			return new double[]
			{ out_pos[0], out_pos[1], out_pos[2], out_vel[0], out_vel[1], out_vel[2] };
		}

		return out_pos;
	}

	/**
	 * Get rectangular equatorial geocentric position of a planet in epoch
	 * J2000.
	 *
	 * @param JD Julian day in TDB.
	 * @param planet Target ID.
	 * @param light_time Light time in days.
	 * @param addSat True to add the planetocentric position of the satellite to the position
	 * of the planet.
	 * @param obs The observer object. Can be null for the Earth's center.
	 * @return Array with x, y, z, (geocentric position) vx, vy, vz (earth barycentric
	 * velocity) coordinates. Note velocity components are those
	 * for the Earth (used for aberration correction) not those for the planet relative to the
	 * geocenter.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public double[] getGeocentricPosition(double JD, TARGET planet, double light_time, boolean addSat, ObserverElement obs) throws JPARSECException
	{
		// Heliocentric position corrected for light time
		double helio_object[] = this.getPositionAndVelocity(JD - light_time, planet);
		if (Functions.equalVectors(helio_object, JPLEphemeris.INVALID_VECTOR) && planet != TARGET.SUN
				&& planet != TARGET.Solar_System_Barycenter)
			return JPLEphemeris.INVALID_VECTOR;

		if (addSat) {
			Object o = DataBase.getData("offsetPosition", true);
			if (o != null) {
				double[] planetocentricPositionOfTargetSatellite = (double[]) o;
				helio_object = Functions.sumVectors(helio_object, planetocentricPositionOfTargetSatellite);
			}
		}

		if (planet == TARGET.Moon && (obs == null || obs.getMotherBody() == TARGET.EARTH)) return helio_object;

		// Compute apparent position of barycenter from Earth
		double earth[] = null;
		if (obs == null || obs.getMotherBody() == TARGET.EARTH || planet == TARGET.Moon) {
			double helio_barycenter[] = this.getPositionAndVelocity(JD, TARGET.Earth_Moon_Barycenter);
			if (Functions.equalVectors(helio_barycenter, JPLEphemeris.INVALID_VECTOR))
				return JPLEphemeris.INVALID_VECTOR;
			double moon[] = this.getPositionAndVelocity(JD, TARGET.Moon);
			earth = Functions.substract(helio_barycenter, Functions.scalarProduct(moon, 1.0 / (1.0 + emrat)));
		}

		/*
		// The call to the Moon can be replaced by the Series96 calculation of geocentric barycenter, which gives
		// also good results.
		double dt = 0.001;
		double[] geoEMB = Series96.getBarycenter(JD);
		double[] geoEMB2 = Series96.getBarycenter(JD+dt);
		geoEMB = new double[] {geoEMB[0], geoEMB[1], geoEMB[2], (geoEMB2[0]-geoEMB[0])/dt, (geoEMB2[1]-geoEMB[1])/dt, (geoEMB2[2]-geoEMB[2])/dt};
		earth = Functions.substract(helio_barycenter, geoEMB);
		 */

		if (obs != null && obs.getMotherBody() != TARGET.EARTH) {
			if (planet == TARGET.Moon) helio_object = Functions.sumVectors(earth, helio_object);
			EphemerisElement eph = new EphemerisElement();
			eph.ephemMethod = REDUCTION_METHOD.IAU_2006;
			eph.algorithm = this.jplID;
			if (eph.algorithm == null) {
				eph.algorithm = ALGORITHM.MOSHIER;
				JPARSECException.addWarning("The algorithm in the ephemeris object is null. Moshier has been selected.");
			}
			earth = obs.heliocentricPositionOfObserver(JD, eph);
		}

		double geo_pos[] = Functions.substract(helio_object, earth);

		geo_pos[3] = earth[3];
		geo_pos[4] = earth[4];
		geo_pos[5] = earth[5];
		return geo_pos;
	}

	/**
	 * Get rectangular equatorial geocentric position of a planet in epoch
	 * J2000, using extended precision. Not actively used in JPARSEC.
	 *
	 * @param JD Julian day in TDB.
	 * @param planet Target ID.
	 * @param light_time Light time in days.
	 * @param addSat True to add the planetocentric position of the satellite to the position
	 * of the planet.
	 * @param obs The observer object. Can be null for the Earth's center.
	 * @return Array with x, y, z, (geocentric position) vx, vy, vz (earth barycentric
	 * velocity) coordinates. Note velocity components are those
	 * for the Earth (used for aberration correction) not those for the planet relative to the
	 * geocenter.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public double[] getGeocentricPosition(BigDecimal JD, TARGET planet, double light_time, boolean addSat, ObserverElement obs) throws JPARSECException
	{
		// Heliocentric position corrected for light time
		double helio_object[] = this.getPositionAndVelocity(JD.subtract(new BigDecimal(light_time)), planet);
		if (Functions.equalVectors(helio_object, JPLEphemeris.INVALID_VECTOR) && planet != TARGET.SUN
				&& planet != TARGET.Solar_System_Barycenter)
			return JPLEphemeris.INVALID_VECTOR;

		if (addSat) {
			Object o = DataBase.getData("offsetPosition", true);
			if (o != null) {
				double[] planetocentricPositionOfTargetSatellite = (double[]) o;
				helio_object = Functions.sumVectors(helio_object, planetocentricPositionOfTargetSatellite);
			}
		}

		if (planet == TARGET.Moon) return helio_object;

		// Compute apparent position of barycenter from Earth
		double earth[] = null;
		if (obs == null || obs.getMotherBody() == TARGET.EARTH || planet == TARGET.Moon) {
			double helio_barycenter[] = this.getPositionAndVelocity(JD, TARGET.Earth_Moon_Barycenter);
			if (Functions.equalVectors(helio_barycenter, JPLEphemeris.INVALID_VECTOR))
				return JPLEphemeris.INVALID_VECTOR;
			double moon[] = this.getPositionAndVelocity(JD, TARGET.Moon);
			earth = Functions.substract(helio_barycenter, Functions.scalarProduct(moon, 1.0 / (1.0 + emrat)));
		}

		/*
		// The call to the Moon can be replaced by the Series96 calculation of geocentric barycenter, which gives
		// also good results.
		double dt = 0.001;
		double[] geoEMB = Series96.getBarycenter(JD);
		double[] geoEMB2 = Series96.getBarycenter(JD+dt);
		geoEMB = new double[] {geoEMB[0], geoEMB[1], geoEMB[2], (geoEMB2[0]-geoEMB[0])/dt, (geoEMB2[1]-geoEMB[1])/dt, (geoEMB2[2]-geoEMB[2])/dt};
		earth = Functions.substract(helio_barycenter, geoEMB);
		 */

		if (obs != null && obs.getMotherBody() != TARGET.EARTH) {
			if (planet == TARGET.Moon) helio_object = Functions.sumVectors(earth, helio_object);
			EphemerisElement eph = new EphemerisElement();
			eph.ephemMethod = REDUCTION_METHOD.IAU_2006;
			eph.algorithm = this.jplID;
			if (eph.algorithm == null) {
				eph.algorithm = ALGORITHM.MOSHIER;
				JPARSECException.addWarning("The algorithm in the ephemeris object is null. Moshier has been selected.");
			}
			earth = obs.heliocentricPositionOfObserver(JD.doubleValue(), eph);
		}

		double geo_pos[] = Functions.substract(helio_object, earth);

		geo_pos[3] = earth[3];
		geo_pos[4] = earth[4];
		geo_pos[5] = earth[5];
		return geo_pos;
	}

	private double[] getEarthPositionAndVelocity(double JD) throws JPARSECException {
		double helio_barycenter[] = this.getPositionAndVelocity(JD, TARGET.Earth_Moon_Barycenter);
		if (Functions.equalVectors(helio_barycenter, JPLEphemeris.INVALID_VECTOR))
			return JPLEphemeris.INVALID_VECTOR;
		double moon[] = this.getPositionAndVelocity(JD, TARGET.Moon);
		return Functions.substract(helio_barycenter, Functions.scalarProduct(moon, 1.0 / (1.0 + emrat)));
	}

	/**
	 * Obtains position and velocity of certain object using the selected
	 * JPL ephemeris version.
	 * @param jd Julian day, TDB.
	 * @param target Target. Can be a planet, Pluto, or the Sun, Moon, Earth-Moon
	 * barycenter, or can be also nutation and libration.
	 * @return A vector with equatorial position and velocity from Solar System
	 * Barycenter, referred to ICRS (or dynamical equinox and equator for DE200) and J2000 equinox.
	 * For the Moon the geocentric position is returned. For Pluto its body center is returned 
	 * using the latest reduction method available.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[] getPositionAndVelocity(double jd, TARGET target)
	throws JPARSECException {
		if (target == TARGET.Solar_System_Barycenter) return new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		if (target == TARGET.EARTH) return this.getEarthPositionAndVelocity(jd);
		int object = DataSet.getIndex(targets, target);
		if (object <= 0) throw new JPARSECException("invalid target "+target+".");

		/*
		 * Begin by determining whether the current ephemeris coefficients are
		 * appropriate for jultime, or if we need to load a new set.
		 */
		if (readVersion != getJPLVersion() || (jd < ephemerisDates[0]) || (jd >= ephemerisDates[1]))
			getEphemerisCoefficients(jd);
		if (readVersion != getJPLVersion() || (jd < ephemerisDates[0]) || (jd >= ephemerisDates[1]))
			throw new JPARSECException("Corrupt file or not adequate for this date");

		if (lpt[0].length <= object-1 || (lpt[0][object-1] == 0 && lpt[1][object-1] == 0 && lpt[2][object-1] == 0)) 
			throw new JPARSECException("Target "+target+" is not available for JPL integration "+getJPLVersion()+".");

		int interval = 0, numbers_to_skip = 0, pointer = 0, j = 0, k = 0, subinterval = 0;

		double interval_start_time = 0, subinterval_duration = 0, chebyshev_time = 0;

		double[] position_poly = new double[20];
		double[][] coef = new double[4][20];
		double[] velocity_poly = new double[20];

		int[] number_of_coef_sets = new int[14];
		int[] number_of_coefs = new int[14];

		/* Initialize arrays */
		for (j=1; j<=13; j++)
		{
			number_of_coefs[j] = this.lpt[1][j-1];
			number_of_coef_sets[j] = this.lpt[2][j-1];
		}

		interval = (int) (Math.floor((jd - ephemerisDates[0]) / jds) + 1);
		interval_start_time = (interval - 1.0) * jds + ephemerisDates[0];
		subinterval_duration = jds / number_of_coef_sets[object];
		subinterval = (int) (Math.floor((jd - interval_start_time) / subinterval_duration) + 1);
		numbers_to_skip = (interval - 1) * numbers_per_interval;

		/*
		 * Starting at the beginning of the coefficient array, skip the first
		 * "numbers_to_skip" coefficients. This puts the pointer on the first
		 * piece of data in the correct interval.
		 */
		pointer = numbers_to_skip + 1;

		/* Skip the coefficients for the first (i-1) planets */
		for (j = 1; j <= (object - 1); j++) {
			int n = 3;
			if (j == 12) n = 2;
			pointer = pointer + n * number_of_coef_sets[j] * number_of_coefs[j];
		}

		/* Skip the next (subinterval - 1)*3*number_of_coefs(i) coefficients */
		int n = 3;
		if (target == TARGET.Nutation) n = 2;
		pointer = pointer + (subinterval - 1) * n * number_of_coefs[object];

		for (j = 1; j <= n; j++)
		{
			for (k = 1; k <= number_of_coefs[object]; k++)
			{
				/* Read the pointer'th coefficient as the array entry coef[j][k] */
				coef[j][k] = ephemerisCoefficients[pointer];
				pointer = pointer + 1;
			}
		}

		/*
		 * Calculate the chebyshev time within the subinterval, between -1 and +1.
		 * jd is a double value. I have tested that with BigDecimal the difference
		 * is around 20 microarcseconds for the Moon (0.04 m), and also below 1 m
		 * for Mars and other planets.
		 */
		chebyshev_time = 2.0 * (jd - ((subinterval - 1.0) * subinterval_duration + interval_start_time)) / subinterval_duration - 1.0;

		/* Calculate the Chebyshev position polynomials */
		position_poly[1] = 1.0;
		position_poly[2] = chebyshev_time;
		for (j = 3; j <= number_of_coefs[object]; j++)
			position_poly[j] = 2.0 * chebyshev_time * position_poly[j - 1] - position_poly[j - 2];

		/* Calculate the position of the i'th planet at jultime */
		double[] ephemeris_r = new double[7];
		for (j = 1; j <= n; j++)
		{
			ephemeris_r[j] = 0;
			for (k = 1; k <= number_of_coefs[object]; k++)
				ephemeris_r[j] = ephemeris_r[j] + coef[j][k] * position_poly[k];

			/* Convert from km to A.U. */
			if (target != TARGET.Libration && target != TARGET.Nutation) ephemeris_r[j] = ephemeris_r[j] / au;
		}

		/* Calculate the Chebyshev velocity polynomials */
		velocity_poly[1] = 0.0;
		velocity_poly[2] = 1.0;
		velocity_poly[3] = 4.0 * chebyshev_time;
		for (j = 4; j <= number_of_coefs[object]; j++)
			velocity_poly[j] = 2.0 * chebyshev_time * velocity_poly[j - 1] + 2.0 * position_poly[j - 1] - velocity_poly[j - 2];

		/* Calculate the velocity of the i'th planet */
		for (j = n+1; j <= 2*n; j++)
		{
			ephemeris_r[j] = 0;
			for (k = 1; k <= number_of_coefs[object]; k++)
				ephemeris_r[j] = ephemeris_r[j] + coef[j-n][k] * velocity_poly[k];
			/*
			 * The next line accounts for differentiation of the iterative
			 * formula with respect to chebyshev time. Essentially, if dx/dt =
			 * (dx/dct) times (dct/dt), the next line includes the factor
			 * (dct/dt) so that the units are km/day
			 */
			ephemeris_r[j] = ephemeris_r[j] * (2.0 * number_of_coef_sets[object] / jds);

			/* Convert from km to A.U. */
			if (target != TARGET.Libration && target != TARGET.Nutation) ephemeris_r[j] = ephemeris_r[j] / au;
		}

		double array[] = new double[] {
			ephemeris_r[1], ephemeris_r[2], ephemeris_r[3],
			ephemeris_r[4], ephemeris_r[5], ephemeris_r[6]
		};

		if (target == TARGET.Nutation) array = DataSet.getSubArray(array, 0, 3);

		// Return position of Pluto's body center
		if (target == TARGET.Pluto) 
			array = MoonEphem.fromPlutoBarycenterToPlutoCenter(array.clone(), jd, EphemerisElement.REDUCTION_METHOD.getLatest(), true);

		return array;
	}

	/**
	 * Obtains position and velocity of certain object using the selected
	 * JPL ephemeris version, and an input date with arbitrary precision. The use
	 * of high precision double values is generally unnecessary and not actively used
	 * in JPARSEC.
	 * @param bigjd Julian day, TDB.
	 * @param target Target. Can be a planet, Pluto, or the Sun, Moon, Earth-Moon
	 * barycenter, or can be also nutation and libration.
	 * @return A vector with equatorial position and velocity from Solar System
	 * Barycenter, referred to ICRS (or dynamical equinox and equator for DE200) and J2000 equinox.
	 * For the Moon the geocentric position is returned. For Pluto its body center is returned 
	 * using the latest reduction method available.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[] getPositionAndVelocity(BigDecimal bigjd, TARGET target)
	throws JPARSECException {
		if (target == TARGET.Solar_System_Barycenter) return new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		if (target == TARGET.EARTH) return this.getEarthPositionAndVelocity(bigjd.doubleValue());
		int object = DataSet.getIndex(targets, target);
		if (object <= 0) throw new JPARSECException("invalid target.");

		double jd = bigjd.doubleValue();
		/*
		 * Begin by determining whether the current ephemeris coefficients are
		 * appropriate for jultime, or if we need to load a new set.
		 */
		if (readVersion != getJPLVersion() || (jd < ephemerisDates[0]) || (jd >= ephemerisDates[1]))
			getEphemerisCoefficients(jd);
		if (readVersion != getJPLVersion() || (jd < ephemerisDates[0]) || (jd >= ephemerisDates[1]))
			throw new JPARSECException("Corrupt file or not adequate for this date");
		
		if (lpt[0].length <= object-1 || (lpt[0][object-1] == 0 && lpt[1][object-1] == 0 && lpt[2][object-1] == 0)) 
			throw new JPARSECException("Target "+target+" is not available for JPL integration "+getJPLVersion()+".");

		int interval = 0, numbers_to_skip = 0, pointer = 0, j = 0, k = 0, subinterval = 0;

		double interval_start_time = 0, subinterval_duration = 0, chebyshev_time = 0;

		double[] position_poly = new double[20];
		double[][] coef = new double[4][20];
		double[] velocity_poly = new double[20];

		int[] number_of_coef_sets = new int[14];
		int[] number_of_coefs = new int[14];

		/* Initialize arrays */
		for (j=1; j<=13; j++)
		{
			number_of_coefs[j] = this.lpt[1][j-1];
			number_of_coef_sets[j] = this.lpt[2][j-1];
		}

		interval = (int) (Math.floor((jd - ephemerisDates[0]) / jds) + 1);
		interval_start_time = (interval - 1.0) * jds + ephemerisDates[0];
		subinterval_duration = jds / number_of_coef_sets[object];
		subinterval = (int) (Math.floor((jd - interval_start_time) / subinterval_duration) + 1);
		numbers_to_skip = (interval - 1) * numbers_per_interval;

		/*
		 * Starting at the beginning of the coefficient array, skip the first
		 * "numbers_to_skip" coefficients. This puts the pointer on the first
		 * piece of data in the correct interval.
		 */
		pointer = numbers_to_skip + 1;

		/* Skip the coefficients for the first (i-1) planets */
		for (j = 1; j <= (object - 1); j++) {
			int n = 3;
			if (j == 12) n = 2;
			pointer = pointer + n * number_of_coef_sets[j] * number_of_coefs[j];
		}

		/* Skip the next (subinterval - 1)*3*number_of_coefs(i) coefficients */
		int n = 3;
		if (target == TARGET.Nutation) n = 2;
		pointer = pointer + (subinterval - 1) * n * number_of_coefs[object];

		for (j = 1; j <= n; j++)
		{
			for (k = 1; k <= number_of_coefs[object]; k++)
			{
				/* Read the pointer'th coefficient as the array entry coef[j][k] */
				coef[j][k] = ephemerisCoefficients[pointer];
				pointer = pointer + 1;
			}
		}

		/*
		 * Calculate the chebyshev time within the subinterval, between -1 and +1.
		 * I have tested that with BigDecimal the difference
		 * is around 20 microarcseconds for the Moon (0.04 m), and also below 1 m
		 * for Mars and other planets.
		 */
		//chebyshev_time = 2.0 * (jd - ((subinterval - 1.0) * subinterval_duration + interval_start_time)) / subinterval_duration - 1.0;
		BigDecimal big_chebyshev_time = (bigjd.subtract(new BigDecimal((subinterval - 1.0) * subinterval_duration + interval_start_time)));
		big_chebyshev_time = big_chebyshev_time.multiply(new BigDecimal(2.0 / subinterval_duration));
		big_chebyshev_time = big_chebyshev_time.subtract(new BigDecimal(1.0));
		chebyshev_time = big_chebyshev_time.doubleValue();


		/* Calculate the Chebyshev position polynomials */
		position_poly[1] = 1.0;
		position_poly[2] = chebyshev_time;
		for (j = 3; j <= number_of_coefs[object]; j++)
			position_poly[j] = 2.0 * chebyshev_time * position_poly[j - 1] - position_poly[j - 2];

		/* Calculate the position of the i'th planet at jultime */
		double[] ephemeris_r = new double[7];
		for (j = 1; j <= n; j++)
		{
			ephemeris_r[j] = 0;
			for (k = 1; k <= number_of_coefs[object]; k++)
				ephemeris_r[j] = ephemeris_r[j] + coef[j][k] * position_poly[k];

			/* Convert from km to A.U. */
			if (target != TARGET.Libration && target != TARGET.Nutation) ephemeris_r[j] = ephemeris_r[j] / au;
		}

		/* Calculate the Chebyshev velocity polynomials */
		velocity_poly[1] = 0.0;
		velocity_poly[2] = 1.0;
		velocity_poly[3] = 4.0 * chebyshev_time;
		for (j = 4; j <= number_of_coefs[object]; j++)
			velocity_poly[j] = 2.0 * chebyshev_time * velocity_poly[j - 1] + 2.0 * position_poly[j - 1] - velocity_poly[j - 2];

		/* Calculate the velocity of the i'th planet */
		for (j = n+1; j <= 2*n; j++)
		{
			ephemeris_r[j] = 0;
			for (k = 1; k <= number_of_coefs[object]; k++)
				ephemeris_r[j] = ephemeris_r[j] + coef[j-n][k] * velocity_poly[k];
			/*
			 * The next line accounts for differentiation of the iterative
			 * formula with respect to chebyshev time. Essentially, if dx/dt =
			 * (dx/dct) times (dct/dt), the next line includes the factor
			 * (dct/dt) so that the units are km/day
			 */
			ephemeris_r[j] = ephemeris_r[j] * (2.0 * number_of_coef_sets[object] / jds);

			/* Convert from km to A.U. */
			if (target != TARGET.Libration && target != TARGET.Nutation) ephemeris_r[j] = ephemeris_r[j] / au;
		}

		double array[] = new double[] {
			ephemeris_r[1], ephemeris_r[2], ephemeris_r[3],
			ephemeris_r[4], ephemeris_r[5], ephemeris_r[6]
		};

		if (target == TARGET.Nutation) array = DataSet.getSubArray(array, 0, 3);

		// Return position of Pluto's body center
		if (target == TARGET.Pluto) 
			array = MoonEphem.fromPlutoBarycenterToPlutoCenter(array.clone(), jd, EphemerisElement.REDUCTION_METHOD.getLatest(), true);

		return array;
	}

	/**
	 * Procedure to read the DExxx ephemeris file corresponding to jultime. The
	 * start and end dates of the ephemeris file are returned, as are the
	 * Chebyshev coefficients for Mercury, Venus, Earth-Moon, Mars, Jupiter,
	 * Saturn, Uranus, Neptune, Pluto, Geocentric Moon, and Sun.
	 */
	private void getEphemerisCoefficients(double jultime)
	throws JPARSECException {
		if (lpt == null || cval == null || cnam == null) readHeader();
		
		int i = 0, j = 0;
		String filePath = " ", line = " ";

		try	{
			double oldEphCoef[] = ephemerisCoefficients;
			String f[] = getCompatibleEphemeridesFile(jultime, true);
			if (f == null) throw new JPARSECException("No JPL file was found for date " + jultime + " and JPL DE"+getJPLVersion());
			if (ephemerisCoefficients != null && ephemerisCoefficients != oldEphCoef) return; // New coefficients read from binary file

			int seriesApprox = (int) (2.0 + 367.0 * (double) MAX_YEARS_PER_FILE / this.jds);
			double[] ephCoef = new double[numbers_per_interval*seriesApprox+1];
			int imax = 1 + (this.ncoeff + 2) / 3;
			int rest = (this.ncoeff + 2) % 3;

			if (f.length > 1) { // Directly returned the contents from an external zip file
				int index = 0;
				j = 0;
				while (true) {
					if (index >= f.length) break;
					line = f[index];
					index ++;
					if (index >= f.length) break;
					
					j ++;
					for (i = 2; i <= imax; i++)	{
						line = DataSet.replaceAll(f[index], "D", "E", true);
						index ++;

						int eindex = (j - 1) * numbers_per_interval + (3 * (i - 2) - 1);
						if (i > 2) {
							ephCoef[eindex] =
									Double.parseDouble(FileIO.getField(1, line, " ", true));
							if (i < imax || rest > 0) ephCoef[eindex+1] =
									Double.parseDouble(FileIO.getField(2, line, " ", true));
						}
						if (i < imax || rest == 2) ephCoef[eindex+2] =
								Double.parseDouble(FileIO.getField(3, line, " ", true));
					}
				}
				f = null;
				ephemerisCoefficients = DataSet.getSubArray(ephCoef, 0, (j - 1) * numbers_per_interval + (3 * (imax - 2) + 1));
				readVersion = getJPLVersion();
				return;
			}
			
			filePath = f[0];
			InputStream is = null;
			if (externalPath != null && filePath.startsWith(externalPath) && new File(filePath).exists()) {
				URLConnection Connection = new File(filePath).toURI().toURL().openConnection();
				is = Connection.getInputStream();
			} else {
				is = getClass().getClassLoader().getResourceAsStream(filePath);
			}
			BufferedReader buff = new BufferedReader(new InputStreamReader(is));

			/* Read each record in the file */
			j = 0;
			while ((line = buff.readLine()) != null) {
				j ++;
				for (i = 2; i <= imax; i++)	{
					line = DataSet.replaceAll(buff.readLine(), "D", "E", true);

					int index = (j - 1) * numbers_per_interval + (3 * (i - 2) - 1);
					if (i > 2) {
						ephCoef[index] =
								Double.parseDouble(FileIO.getField(1, line, " ", true));
						if (i < imax || rest > 0) ephCoef[index+1] =
								Double.parseDouble(FileIO.getField(2, line, " ", true));
					}
					if (i < imax || rest == 2) ephCoef[index+2] =
							Double.parseDouble(FileIO.getField(3, line, " ", true));
				}
			}
			buff.close();
			is.close();
			is = null;
			ephemerisCoefficients = DataSet.getSubArray(ephCoef, 0, (j - 1) * numbers_per_interval + (3 * (imax - 2) + 1));
			readVersion = getJPLVersion();
		} catch (Exception e) {
			if (e instanceof JPARSECException) throw (JPARSECException) e;
			throw new JPARSECException("a problem was found when trying to read from the file "+filePath+".", e);
		}
	}

	/**
	 * Basic propagator of orbits based on the gravitation law. In this method double 
	 * values are used, which limits the accuracy of the results. The perturbers used are the 
	 * planets, Sun, Pluto, and Ceres, Pallas, and Vesta. Earth-Moon barycenter is used instead 
	 * of the individual bodies. Initial state vectors for the planets, Sun, and Pluto are 
	 * obtained for the JPL ephemeris defined by this instance.
	 * @param eph Ephemeris object. Only used for the method to compute the J2000 obliquity.
	 * @param startDate Initial date for the propagation of an orbit.
	 * @param endDate Final date to return the results.
	 * @param vector0 Initial State vector at startDate, position and velocities (6 values), 
	 * for the body to propagate. Mean equatorial J2000 coordinates, units of AU and AU/day, 
	 * like JPL ones.
	 * @param mass Mass of the perturber relative to the Sun and greater than 0. Sun = 1, 
	 * Jupiter = 1047, and so on. For a mass-less body use 0.
	 * @return Positions and velocities of the input body for the end date, respect the Solar 
	 * System barycenter (like JPL ones), not respect to the Sun.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[] propagatePerturbations(EphemerisElement eph, 
			double startDate, double endDate, double[] vector0, double mass) throws JPARSECException {
		// Coordinate mode and main perturbers. Last target must be for the input body
		final boolean eclipticMode = false, neglectWeakInteractions = false;
		final TARGET targets[] = new TARGET[] {TARGET.SUN, TARGET.MERCURY, TARGET.VENUS,
				TARGET.Earth_Moon_Barycenter, TARGET.MARS, TARGET.JUPITER, TARGET.SATURN, TARGET.URANUS,
				TARGET.NEPTUNE, TARGET.Pluto, TARGET.Ceres, TARGET.Pallas, TARGET.Vesta, TARGET.NOT_A_PLANET};
		
		// Initial conditions
		double pos[][] = new double[targets.length][];
		for (int i=0; i<targets.length-1; i++) {
			double p[] = null;
			if (targets[i].isAsteroid()) {
				int index = OrbitEphem.getIndexOfAsteroid(targets[i].getName());
				p = new double[] {1E10, 1E10, 1E10, 0, 0, 0};
				if (index >= 0) {
					OrbitalElement orbit = OrbitEphem.getOrbitalElementsOfAsteroid(index);
					if (orbit.referenceEquinox != Constant.J2000) orbit.changeToEquinox(Constant.J2000);
					p = OrbitEphem.toEclipticPlane(orbit, OrbitEphem.elliptic(orbit, startDate));
					if (!eclipticMode) p = Ephem.eclipticToEquatorial(p, Constant.J2000, eph);
				}
			} else {
				p = getPositionAndVelocity(startDate, targets[i]);
				if (targets[i] == TARGET.Moon) {
					if (targets[i-1] != TARGET.EARTH) throw new JPARSECException("Earth not found before the Moon");
					p = Functions.sumVectors(p, pos[i-1]);
				}
				if (eclipticMode) p = Ephem.equatorialToEcliptic(p, Constant.J2000, eph);
			}
			pos[i] = p;
			if (eclipticMode) pos[i] = Ephem.equatorialToEcliptic(p, Constant.J2000, eph);
			// Pass velocities to AU/s
			pos[i][3] /= Constant.SECONDS_PER_DAY;
			pos[i][4] /= Constant.SECONDS_PER_DAY;
			pos[i][5] /= Constant.SECONDS_PER_DAY;
		}
		double vector[] = vector0.clone();
		vector[3] /= Constant.SECONDS_PER_DAY;
		vector[4] /= Constant.SECONDS_PER_DAY;
		vector[5] /= Constant.SECONDS_PER_DAY;
		pos[pos.length-1] = vector;

		// Calculate the Gravitational constant from the header constants
        double gms = Double.parseDouble(getConstant("GMS"));
        double G = gms / Math.pow(Constant.SECONDS_PER_DAY, 2); // AU^3/s^2, assuming Msun = 1
        
        // Propagate
        double jd = startDate, speed = 10; // seconds
		if (endDate < jd) speed = -Math.abs(speed);
		while (Math.abs(endDate-jd) > Math.abs(speed/Constant.SECONDS_PER_DAY)) {
			Newton(pos, speed, G, neglectWeakInteractions, mass);
			// Make integration effective on date
			jd += speed / Constant.SECONDS_PER_DAY;
		}
		speed = (endDate - jd) * Constant.SECONDS_PER_DAY;
		if (speed != 0) Newton(pos, speed, G, neglectWeakInteractions, mass);
		return pos[pos.length-1];
	}
	
	private static void Newton(double[][] pos, double speed, double G, boolean neglectWeakInteractions, 
			double mass) {
		// 'Fit' the position of the Sun by computing the SSB and forcing it to be at 0
		// This helps to keep constant L long term but will degrade the position of the 
		// Sun respect JPL integrations
		/*
		double cx = 0, cy = 0, cz = 0;
		int sunIndex = -1;
		for (int i=0;i<targets.length;i++) {			
			if (targets[i] == TARGET.SUN) {
				sunIndex = i;
				continue;
			}
			cx += pos[i][0] / targets[i].relativeMass;
			cy += pos[i][1] / targets[i].relativeMass;
			cz += pos[i][2] / targets[i].relativeMass;
		}
		pos[sunIndex][0] = -cx * targets[sunIndex].relativeMass;
		pos[sunIndex][1] = -cy * targets[sunIndex].relativeMass;
		pos[sunIndex][2] = -cz * targets[sunIndex].relativeMass;
		*/
		
		// Integration, credits for Newton and his apple
		double speedG = -speed * G;
		for (int i=0;i<targets.length-1;i++) {
			for (int j=i+1; j<targets.length;j++) {
				double dx = pos[i][0] - pos[j][0];
				double dy = pos[i][1] - pos[j][1];
				double dz = pos[i][2] - pos[j][2];
				double d2 = dx*dx + dy*dy + dz*dz;
				double d = Math.sqrt(d2);

				if (targets[j].relativeMass != 0) {
					double g = d2 * targets[j].relativeMass;
					if (mass > 0 && j == targets.length-1) g = d2 * mass;
					if (!neglectWeakInteractions || g < 1000) { // Neglect interactions from far/little bodies
						double G2 = speedG / g;
						pos[i][3] += G2 * dx / d;
						pos[i][4] += G2 * dy / d;
						pos[i][5] += G2 * dz / d;
					}
				}
				if (targets[i].relativeMass != 0) {
					double g = d2 * targets[i].relativeMass;
					if (!neglectWeakInteractions || g < 1000) { // Neglect interactions from far/little bodies
						double G2 = speedG / g;
						pos[j][3] -= G2 * dx / d;
						pos[j][4] -= G2 * dy / d;
						pos[j][5] -= G2 * dz / d;
					}
				}
			}
		}
		
		// Move the planets
		for (int i=0;i<targets.length;i++) {	
			pos[i][0] += speed * pos[i][3];
			pos[i][1] += speed * pos[i][4];
			pos[i][2] += speed * pos[i][5];
		}
	}
	
	/**
	 * Corrects Julian day of calculations of JPL Ephemeris (and other methods) for a different value
	 * of the secular acceleration of the Moon. This method uses as reference the current value of the
	 * constant {@linkplain Constant#MOON_SECULAR_ACCELERATION}, which is the (usually) adopted
	 * value, to compute the time correction when using a theory in which a different value was adopted. 
	 * This correction is not applied automatically in JPARSEC when computing the position of the Moon
	 * using JPL ephemerides (it is only corrected when using ELP2000), and should only be applied to 
	 * lunar ephemerides (mainly for DE200). The reference value is very similar to the one adopted in DE422.
	 *
	 * @param jd Julian day in dynamical time.
	 * @param jplID The algorithm.
	 * @return Correction to add to input time in days.
	 * @throws JPARSECException If the input algorithm does not belong to a JPL ephemerides or
	 * a compatible one (Vsop/ELP = DE200, and Moshier/Series96 = DE403).
	 */
	public static double timeCorrectionForMoonSecularAcceleration(double jd, EphemerisElement.ALGORITHM jplID) throws JPARSECException
	{
		if (!Configuration.ENABLE_TIME_CORRECTION_FOR_MOON_SECULAR_ACCELERATION) return 0;
		double moonSecularAcceleration = 0.0;
		switch (jplID) {
		case VSOP87_ELP2000ForMoon:
		case JPL_DE200:
			moonSecularAcceleration = JPLEphemeris.MOON_SECULAR_ACCELERATION_DE200;
			break;
		case MOSHIER:
		case SERIES96_MOSHIERForMoon:
		case JPL_DE403:
			moonSecularAcceleration = JPLEphemeris.MOON_SECULAR_ACCELERATION_DE403;
			break;
		case JPL_DE405:
		case JPL_DE406:
			moonSecularAcceleration = JPLEphemeris.MOON_SECULAR_ACCELERATION_DE405;
			break;
		case JPL_DE413:
		case JPL_DE414:
			moonSecularAcceleration = JPLEphemeris.MOON_SECULAR_ACCELERATION_DE413;
			break;
		case JPL_DE422:
			moonSecularAcceleration = JPLEphemeris.MOON_SECULAR_ACCELERATION_DE422;
			break;
		case JPL_DE424:
			moonSecularAcceleration = JPLEphemeris.MOON_SECULAR_ACCELERATION_DE422;
			break;
		case JPL_DE430:
		case JPL_DE431:
			moonSecularAcceleration = JPLEphemeris.MOON_SECULAR_ACCELERATION_DE430;
			break;
		default:
			if (!jplID.isJPL()) throw new JPARSECException("Invalid algorithm.");
			// Use DE430 value
			moonSecularAcceleration = JPLEphemeris.MOON_SECULAR_ACCELERATION_DE430;
		}
		double cent = (jd - 2435109.0) / Constant.JULIAN_DAYS_PER_CENTURY;
		double deltaT = -0.91072 * (moonSecularAcceleration - Constant.MOON_SECULAR_ACCELERATION) * cent * cent;

		return deltaT / Constant.SECONDS_PER_DAY;
	}
}
