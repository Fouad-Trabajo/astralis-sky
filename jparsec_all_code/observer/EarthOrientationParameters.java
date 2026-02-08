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
package jparsec.observer;

import java.util.ArrayList;

import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.EphemerisElement.FRAME;
import jparsec.ephem.EphemerisElement.REDUCTION_METHOD;
import jparsec.ephem.Functions;
import jparsec.ephem.Nutation;
import jparsec.ephem.Obliquity;
import jparsec.ephem.Precession;
import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.math.Constant;
import jparsec.math.Interpolation;
import jparsec.time.AstroDate;
import jparsec.time.TimeScale;
import jparsec.util.Configuration;
import jparsec.util.DataBase;
import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Logger.LEVEL;
import jparsec.util.Translate;
import jparsec.util.Update;
import jparsec.vo.GeneralQuery;

/**
 * A class to obtain the current Earth Orientation Parameters as defined by the
 * IERS.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 * @see Nutation
 */
public class EarthOrientationParameters
{
	// private constructor so that this class cannot be instantiated.
	private EarthOrientationParameters() {}

	/**
	 * Path to the file of Earth Rotation Parameters, iau1980 version.
	 */
	public static final String PATH_TO_FILE_IAU1980 = FileIO.DATA_EOP_DIRECTORY + "IERS_EOP_iau1980.txt";

	/**
	 * Path to the file of Earth Rotation Parameters, iau2000 version.
	 */
	public static final String PATH_TO_FILE_IAU2000 = FileIO.DATA_EOP_DIRECTORY + "IERS_EOP_iau2000.txt";

	/**
	 * Downloads the EOP record directly from Internet, used automatically when the EOP data in JPARSEC 
	 * is not extended to this date.
	 * @param JD Julian day in UTC.
	 * @param pathToFile Path to the IAU1980 or IAU2000 files defined in this class.
	 * @return String with the values corresponding to the previous midnight,
	 * 			in IERS format, or empty string if input time is not applicable.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public static String downloadEOP(double JD, String pathToFile) throws JPARSECException
	{
		boolean iau2000 = (pathToFile == PATH_TO_FILE_IAU2000);
		if (!iau2000 && !pathToFile.equals(PATH_TO_FILE_IAU1980)) return "";
		
		// Obtain previous midnight
		AstroDate astro = new AstroDate(0.5 + (int) (JD - 0.5));

		// Get URL of the EOP file for the year
		String year = ""+astro.getYear();
		if (year.length() != 4) return "";
		year = year.substring(2);
		String path = iau2000 ? Update.UPDATE_URL_EOP_IAU2000 : Update.UPDATE_URL_EOP_IAU1980;
		path = DataSet.replaceAll(path, "62-now", year, true);

		// Check if the file was previously downloaded, and recently
		String data[] = null;
		String id = "EOP_download_1980."+year;
		if (iau2000) id = "EOP_download_2000."+year;
		Object o = DataBase.getData(id, true);
		if (o != null) {
			Object eop[] = (Object[]) o;
			long when = (Long) eop[0];
			double elapsed = Math.abs(System.currentTimeMillis() - when) * 0.001 / Constant.SECONDS_PER_DAY;
			if (elapsed < 1) data = (String[]) eop[1];
		}

		// Download and save in database if necessary
		if (data == null) {
			data = DataSet.toStringArray(GeneralQuery.query(path, ReadFile.ENCODING_UTF_8, 30000), 
					FileIO.getLineSeparator(), false);
			if (data == null) return "";
			DataBase.addData(id, new Object[] {System.currentTimeMillis(), data}, true);
		}
		
		// Calculate record ID and position
		int recordPos = 15 + (int) (JD - new AstroDate(astro.getYear(), 1, 1).jd());
		int month = astro.getMonth();
		int day = astro.getDay();
		String monthV = "  "+month;
		if (month < 10) monthV = " " + monthV;
		String dayV = "  "+day;
		if (day < 10) dayV = " " + dayV;
		String record_to_find = astro.getYear() + monthV + dayV+"  ";

		// Return the record or empty string if not found
		for (int i=recordPos-5;i<=recordPos+5;i++) {
			if (data.length <= i) return "";
			if (data[i].startsWith(record_to_find)) return data[i];
		}
		return "";
	}
	
	/**
	 * Reads the example file with the Earth Orientation Parameters, formatted
	 * in the standard way, established by IERS.
	 * <P>
	 * An example of IERS format is:
	 * <P>
	 *
	 * <pre>
     * Date      MJD      x          y        UT1-UTC       LOD         dX        dY        x Err     y Err   UT1-UTC Err  Lod Err     dY Err       dY Err
     *                    &quot;          &quot;           s           s          &quot;         &quot;           &quot;          &quot;          s         s            &quot;           &quot;
     * (0h UTC)
	 * 1962   1   1  37665  -0.012700   0.213000   0.0326338   0.0017230   0.000000   0.000000   0.030000   0.030000  0.0020000  0.0014000    0.012000    0.002000
	 * 1962  JAN   1  37665-0.012700 0.213000 0.0326338   0.0017230   0.065037 0.000436
	 * </pre>
	 *
	 * @param JD Julian day in UTC.
	 * @param pathToFile Path to the IAU1980 or IAU2000 files.
	 * @return String with the values corresponding to the previous midnight,
	 * 			in IERS format, or empty string if input time is not applicable.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	private static String obtainEOPRecord(double JD, String pathToFile) throws JPARSECException
	{
		// Obtain previous midnight
		AstroDate astro = new AstroDate(0.5 + (int) (JD - 0.5));

		// Calculate record ID
		int month = astro.getMonth();
		int day = astro.getDay();
		String monthV = "  "+month;
		if (month < 10) monthV = " " + monthV;
		String dayV = "  "+day;
		if (day < 10) dayV = " " + dayV;
		String record_to_find = astro.getYear() + monthV + dayV+"  ";

		// Define necessary variables
		String file_line = "";

		// Connect to the file
		String out = "";
		AstroDate astro1962 = new AstroDate(1962, 1, 1);
		int recordPosition = (int) (JD - astro1962.jd()) + 15;
		try {
			ArrayList<String> v = ReadFile.readResourceSomeLines(pathToFile, ReadFile.ENCODING_ISO_8859, recordPosition - 5, recordPosition + 5);
			int index = 0;
			while (index < v.size() && out.equals(""))
			{
				file_line = v.get(index);
				index ++;
	
				// Obtain object
				int a = file_line.indexOf(record_to_find);
				if (0 <= a)	out = file_line;
				file_line = file_line.trim();
				if (file_line.length() > record_to_find.length()) {
					if (file_line.substring(0, record_to_find.length()).equals(record_to_find))
						break;
				}
			}
		} catch (Exception exc) {}

		if (astro.getYear() >= 1962 && out.equals("") && 
				Configuration.ALLOW_DOWNLOAD_EOP) return downloadEOP(JD, pathToFile);
		return out;
	}

	/**
	 * Returns the date of the last record in the EOP file available in JPARSEC.
	 * @param eph The ephemeris properties to select IAU 1980 or 2000 file.
	 * @return The date.
	 * @throws JPARSECException If an error occurs.
	 */
	public static AstroDate lastEOPRecordDate(EphemerisElement eph) throws JPARSECException
	{
		EphemerisElement.REDUCTION_METHOD method = eph.ephemMethod;

		boolean iau2000 = false;
		String pathToFile = PATH_TO_FILE_IAU1980;
		if (method.isIAU20xx()) iau2000 = true;
		if (iau2000) pathToFile = PATH_TO_FILE_IAU2000;

		// Connect to the file
		ArrayList<String> v = ReadFile.readResourceLastNlines(pathToFile, ReadFile.ENCODING_ISO_8859, 100);
		String file_line = v.get(v.size()-1);

		int y = Integer.parseInt(FileIO.getField(1, file_line, " ", true));
		int m = Integer.parseInt(FileIO.getField(2, file_line, " ", true));
		int d = Integer.parseInt(FileIO.getField(3, file_line, " ", true));

		AstroDate astro = new AstroDate(y, m, d);

		return astro;
	}

	/**
	 * Returns the date of the first record in the EOP file available in JPARSEC.
	 * @param eph The ephemeris properties to select IAU 1980 or 2000 file.
	 * @return The date.
	 * @throws JPARSECException If an error occurs.
	 */
	public static AstroDate firstEOPRecordDate(EphemerisElement eph) throws JPARSECException
	{
		EphemerisElement.REDUCTION_METHOD method = eph.ephemMethod;

		boolean iau2000 = false;
		String pathToFile = PATH_TO_FILE_IAU1980;
		if (method.isIAU20xx()) iau2000 = true;
		if (iau2000) pathToFile = PATH_TO_FILE_IAU2000;

		// Connect to the file
		ArrayList<String> v = ReadFile.readResourceFirstNlines(pathToFile, ReadFile.ENCODING_ISO_8859, 100);
		String file_line = "";
		int i = 0;
		do {
			file_line = v.get(i);
			i++;
			if (file_line.trim().equals(file_line) && FileIO.getNumberOfFields(file_line, " ", true) > 10) break;
		} while (true);

		int y = Integer.parseInt(FileIO.getField(1, file_line, " ", true));
		int m = Integer.parseInt(FileIO.getField(2, file_line, " ", true));
		int d = Integer.parseInt(FileIO.getField(3, file_line, " ", true));

		AstroDate astro = new AstroDate(y, m, d);

		return astro;
	}

	/**
	 * Returns the LOD ('Length of Day') excess, which may be positive or
	 * negative depending on the Earth rotation rate (slower or faster
	 * respectively). This method is not used in JPARSEC, but can be
	 * optionally used to correct magnitudes that depends on Earth rotation,
	 * for instance sidereal time or horizontal coordinates.
	 * <P>There is a relationship between LOD and the angular velocity
	 * of the rotation of the Earth. OMEGA = OMEGA0 (1 - LOD / T) (radians/s),
	 * where T = 86400 s and OMEGA0 = 72 921 151.467064 E-12 radians/s,
	 * corresponding to the mean angular rotation speed on year 1820.
	 * @param jd_UTC Julian day in UTC.
	 * @param eph Ephemeris object.
	 * @return LOD in s for the closest midnight to the input date. 0 is
	 * returned in case of being unavailable.
	 */
	public static double getLOD(double jd_UTC, EphemerisElement eph) {
		EphemerisElement.REDUCTION_METHOD method = eph.ephemMethod;

		boolean iau2000 = false;
		String pathToFile = PATH_TO_FILE_IAU1980;
		if (method.isIAU20xx()) iau2000 = true;
		if (iau2000) pathToFile = PATH_TO_FILE_IAU2000;

		String record = "";
		try {
			record = EarthOrientationParameters.obtainEOPRecord(0.5 + (int) jd_UTC, pathToFile); // closest midnight
		} catch (Exception exc) {
			Logger.log(LEVEL.ERROR, "Error when reading the LOD record. Returning 0 as LOD.");
			return 0;
		}

		if (record == null || record.equals("")) {
			Logger.log(LEVEL.ERROR, "Could not read the LOD record. Returning 0 as LOD.");
			return 0;
		}

		return DataSet.parseDouble(FileIO.getField(8, record, " ", true));
	}

	/**
	 * Obtains Earth Orientation Parameters.
	 * The results are set to 0 in case of unacceptable input date (prior to
	 * January, 1, 1962, or beyond the last date in the IERS EOP input file). If
	 * this correction is not desired, then UT1-UTC is calculated, but not the
	 * pole offsets, which will remain to 0.0.
	 * <P>
	 * Values for IAU2000 model are automatically transformed from dx, dy
	 * celestial pole offsets to dpsi, deps. For external files, this
	 * transformation must be manually done by calling {@link EarthOrientationParameters#dxdyTOdpsideps(double, 
	 * double, double, EphemerisElement.FRAME)}
	 * method.
	 * <P>
	 * There are two methods: IAU1980 and IAU2000. The first one can be applied
	 * when using Laskar 1986 or IAU 1976 precession methods. The second is for IAU2000 or
	 * Capitaine et al. 2003 precession methods (IAU2006/2009 reduction methods). This distinction is
	 * automatically performed. For other precession methods this correction is
	 * not applied (UT1-UTC correction is still applied). The difference between Laskar precession plus this
	 * correction and IAU2000 model is about 10 milliarcseconds for current
	 * dates. Without this correction, errors amount to about 50 milliarcseconds
	 * for Laskar method.
	 *
	 * @param jd_UTC Julian day in UTC.
	 * @param eph Ephemeris object.
	 * @throws JPARSECException Thrown if the method fails.
	 * @return The values of dPsi, dEpsilon (rad), pole x, y ("), and UT1-UTC. They will be 0 if the EOP correction should not
	 * be applied or the input date is outside the range covered by the EOP parameters available.
	 */
	public static double[] obtainEOP(double jd_UTC, EphemerisElement eph) throws JPARSECException
	{
		EphemerisElement.REDUCTION_METHOD method = eph.ephemMethod;

		Object o = DataBase.getData("EOP", true);
		double eop[] = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0}; // dPsi, dEpsilon, x, y, UT1-UTC, lastJD, lastMethod
		if (o != null) eop = (double[]) o;
		double lastJD = eop[5];
		int lastMethod = (int) eop[6];

		// Don't repeat calculations for similar dates
		if (Math.abs(jd_UTC - lastJD) < 0.25 && method.ordinal() == lastMethod)
			return new double[] {eop[0], eop[1], eop[2], eop[3], eop[4]};

		clearEOP();

		if (!eph.correctForEOP) return new double[] {0.0, 0.0, 0.0, 0.0, 0.0};

		boolean iau2000 = false;
		String pathToFile = PATH_TO_FILE_IAU1980;
		if (method.isIAU20xx()) iau2000 = true;
		if (iau2000) pathToFile = PATH_TO_FILE_IAU2000;

		String record = "", nextRecord1 = "", nextRecord2 = "", previousRecord1 = "", previousRecord2;
		try {
			record = EarthOrientationParameters.obtainEOPRecord(jd_UTC, pathToFile);
			nextRecord1 = EarthOrientationParameters.obtainEOPRecord(jd_UTC + 1.0, pathToFile);
			nextRecord2 = EarthOrientationParameters.obtainEOPRecord(jd_UTC + 2.0, pathToFile);
			previousRecord1 = EarthOrientationParameters.obtainEOPRecord(jd_UTC - 1.0, pathToFile);
			previousRecord2 = EarthOrientationParameters.obtainEOPRecord(jd_UTC - 2.0, pathToFile);
		} catch (Exception exc) {
			JPARSECException.addWarning("Earth Orientation Parameters (EOP) file not available.");
			return new double[] {0.0, 0.0, 0.0, 0.0, 0.0}; // If EOP are not available just don't apply correction
		}


		double EOP[] = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0 };
		boolean calcPM = true;
		if (method == EphemerisElement.REDUCTION_METHOD.JPL_DE4xx || method == EphemerisElement.REDUCTION_METHOD.WILLIAMS_1994 || method == EphemerisElement.REDUCTION_METHOD.SIMON_1994)
			calcPM = false;
		if (record != null && !record.equals(""))
		{
			double jd0 = (int) (jd_UTC - 0.5) + 0.5;
			try {
				EOP = interpolateEOP(new String[] {previousRecord2, previousRecord1, record, nextRecord1, nextRecord2},
					new double[] {jd0 - 2.0, jd0 - 1.0, jd0, jd0 + 1.0, jd0 + 2.0}, new int[] {7, 9, 10, 5, 6}, jd_UTC);
			} catch (JPARSECException exc) {
				// Thrown when interpolating in the future (interpolation out of range)
				JPARSECException.addWarning(Translate.translate(275));
				calcPM = false;
			}
		} else {
			JPARSECException.addWarning(Translate.translate(275));
			calcPM = false;
		}

		double UT1minusUTC = EOP[0];
		lastJD = jd_UTC;
		lastMethod = method.ordinal();
		double dPsi = 0, dEpsilon = 0, x = 0, y = 0;

		if (!calcPM) {
			DataBase.addData("EOP", new double[] {
					dPsi, dEpsilon, x, y, UT1minusUTC, lastJD, lastMethod
			}, true);
			return new double[] {dPsi, dEpsilon, x, y, UT1minusUTC};
		}

		x = EOP[3];
		y = EOP[4];
		double TTminusUT1 = TimeScale.getTTminusUT1(new AstroDate(jd_UTC));
		double jd_TT = jd_UTC + UT1minusUTC + TTminusUT1;

		if (eph.correctEOPForDiurnalSubdiurnalTides) {
			double c[] = RAYmodelForDiurnalSubdiurnalTides(jd_TT);
			x += c[0];
			y += c[1];
			UT1minusUTC += c[2];
		}

		if (iau2000) {
			double EOP_2000[] = dxdyTOdpsideps(EOP[1], EOP[2], jd_TT, eph.frame);
			dPsi = EOP_2000[0];
			dEpsilon = EOP_2000[1];
		} else {
			dPsi = EOP[1];
			dEpsilon = EOP[2];
		}
		DataBase.addData("EOP", new double[] {
				dPsi, dEpsilon, x, y, UT1minusUTC, lastJD, lastMethod
			}, true);
		return new double[] {dPsi, dEpsilon, x, y, UT1minusUTC};
	}

	private static  double[] interpolateEOP(String records[], double dates[], int fields[], double jd) throws JPARSECException {
		// fields: 7, 9, 10, 5, 6 for UT1-UTC, dx/dPsi, dy/dEps, x, y
		double out[] = new double[fields.length];
		for (int f=0; f<fields.length; f++) {
			ArrayList<Double> x = new ArrayList<Double>();
			ArrayList<Double> y = new ArrayList<Double>();
			for (int r = 0; r<records.length; r++) {
				if (records[r] != null && !records[r].equals("")) {
					x.add(dates[r]-dates[0]);
					double v = DataSet.parseDouble(FileIO.getField(fields[f], records[r], " ", true));
					y.add(v);
				}
			}
			Interpolation interp = new Interpolation(DataSet.arrayListToDoubleArray(x), DataSet.arrayListToDoubleArray(y), false);
			out[f] = interp.LagrangeInterpolation(jd-dates[0]);
		}
		return out;
	}

	/**
	 * Forces the EOP parameters for a given date. Note in case EOPs are
	 * requested for another date, the values forced here for the old date will be lost.
	 * @param jd_UTC The UTC date.
	 * @param eph Ephemeris object.
	 * @param UT1minusUTC UT1-UTC (s).
	 * @param x X pole position (").
	 * @param y Y pole position (").
	 * @param dx dX pole offset (").
	 * @param dy dY pole offset (").
	 * @param dxdyAredPsidEpsilon True in case the input values of dx and dy
	 * are dPsi and dEpsilon (nutation components). Should be true for IAU1980
	 * reduction method.
	 * @throws JPARSECException  If an error occurs.
	 */
	public static void forceEOP(double jd_UTC, EphemerisElement eph,
			double UT1minusUTC, double x, double y, double dx, double dy,
			boolean dxdyAredPsidEpsilon) throws JPARSECException {
		double dPsi = dx, dEpsilon = dy;
		if (!dxdyAredPsidEpsilon) {
			double TTminusUT1 = TimeScale.getTTminusUT1(new AstroDate(jd_UTC));
			double jd_TT = jd_UTC + UT1minusUTC + TTminusUT1;
			double EOP_2000[] = dxdyTOdpsideps(dx, dy, jd_TT, eph.frame);
			dPsi = EOP_2000[0];
			dEpsilon = EOP_2000[1];
		}

		DataBase.addData("EOP", new double[] {
				dPsi, dEpsilon, x, y, UT1minusUTC, jd_UTC, eph.ephemMethod.ordinal()
		}, true);
	}

	/**
	 * Forces the EOP parameters for a given date. Note in case EOPs are
	 * requested for another date, the values forced here for the old date will be lost.
	 * @param jd_UTC The UTC date.
	 * @param eph Ephemeris object.
	 * @param eop The EOP parameters: dPsi, dEpsilon, x, y, UT1-UTC.
	 * @throws JPARSECException  If an error occurs.
	 */
	public static void forceEOP(double jd_UTC, EphemerisElement eph,
			double eop[]) throws JPARSECException {
		DataBase.addData("EOP", new double[] {
				eop[0], eop[1], eop[2], eop[3], eop[4], jd_UTC, eph.ephemMethod.ordinal()
		}, true);
	}

	   /** HS parameter. */
    private static final double[] HS = {
        -001.94, -001.25, -006.64, -001.51, -008.02,
        -009.47, -050.20, -001.80, -009.54, +001.52,
        -049.45, -262.21, +001.70, +003.43, +001.94,
        +001.37, +007.41, +020.62, +004.14, +003.94,
        -007.14, +001.37, -122.03, +001.02, +002.89,
        -007.30, +368.78, +050.01, -001.08, +002.93,
        +005.25, +003.95, +020.62, +004.09, +003.42,
        +001.69, +011.29, +007.23, +001.51, +002.16,
        +001.38, +001.80, +004.67, +016.01, +019.32,
        +001.30, -001.02, -004.51, +120.99, +001.13,
        +022.98, +001.06, -001.90, -002.18, -023.58,
        +631.92, +001.92, -004.66, -017.86, +004.47,
        +001.97, +017.20, +294.00, -002.46, -001.02,
        +079.96, +023.83, +002.59, +004.47, +001.95,
        +001.17
    };

    /** PHASE parameter. */
    private static final double[] PHASE = {
        +09.0899831 - Constant.PI_OVER_TWO, +08.8234208 - Constant.PI_OVER_TWO, +12.1189598 - Constant.PI_OVER_TWO,
        +01.4425700 - Constant.PI_OVER_TWO, +04.7381090 - Constant.PI_OVER_TWO, +04.4715466 - Constant.PI_OVER_TWO,
        +07.7670857 - Constant.PI_OVER_TWO, -02.9093042 - Constant.PI_OVER_TWO, +00.3862349 - Constant.PI_OVER_TWO,
        -03.1758666 - Constant.PI_OVER_TWO, +00.1196725 - Constant.PI_OVER_TWO, +03.4152116 - Constant.PI_OVER_TWO,
        +12.8946194 - Constant.PI_OVER_TWO, +05.5137686 - Constant.PI_OVER_TWO, +06.4441883 - Constant.PI_OVER_TWO,
        -04.2322016 - Constant.PI_OVER_TWO, -00.9366625 - Constant.PI_OVER_TWO, +08.5427453 - Constant.PI_OVER_TWO,
        +11.8382843 - Constant.PI_OVER_TWO, +01.1618945 - Constant.PI_OVER_TWO, +05.9693878 - Constant.PI_OVER_TWO,
        -01.2032249 - Constant.PI_OVER_TWO, +02.0923141 - Constant.PI_OVER_TWO, -01.7847596 - Constant.PI_OVER_TWO,
        +08.0679449 - Constant.PI_OVER_TWO, +00.8953321 - Constant.PI_OVER_TWO, +04.1908712 - Constant.PI_OVER_TWO,
        +07.4864102 - Constant.PI_OVER_TWO, +10.7819493 - Constant.PI_OVER_TWO, +00.3137975 - Constant.PI_OVER_TWO,
        +06.2894282 - Constant.PI_OVER_TWO, +07.2198478 - Constant.PI_OVER_TWO, -00.1610030 - Constant.PI_OVER_TWO,
        +03.1345361 - Constant.PI_OVER_TWO, +02.8679737 - Constant.PI_OVER_TWO, -04.5128771 - Constant.PI_OVER_TWO,
        +04.9665307 - Constant.PI_OVER_TWO, +08.2620698 - Constant.PI_OVER_TWO, +11.5576089 - Constant.PI_OVER_TWO,
        +00.6146566 - Constant.PI_OVER_TWO, +03.9101957 - Constant.PI_OVER_TWO,
        +20.6617051, +13.2808543, +16.3098310, +08.9289802, +05.0519065,
        +15.8350306, +08.6624178, +11.9579569, +08.0808832, +04.5771061,
        +00.7000324, +14.9869335, +11.4831564, +04.3105437, +07.6060827,
        +03.7290090, +10.6350594, +03.2542086, +12.7336164, +16.0291555,
        +10.1602590, +06.2831853, +02.4061116, +05.0862033, +08.3817423,
        +11.6772814, +14.9728205, +04.0298682, +07.3254073, +09.1574019
    };

    /** FREQUENCY parameter. */
    private static final double[] FREQUENCY = {
        05.18688050, 05.38346657, 05.38439079, 05.41398343, 05.41490765,
        05.61149372, 05.61241794, 05.64201057, 05.64293479, 05.83859664,
        05.83952086, 05.84044508, 05.84433381, 05.87485066, 06.03795537,
        06.06754801, 06.06847223, 06.07236095, 06.07328517, 06.10287781,
        06.24878055, 06.26505830, 06.26598252, 06.28318449, 06.28318613,
        06.29946388, 06.30038810, 06.30131232, 06.30223654, 06.31759007,
        06.33479368, 06.49789839, 06.52841524, 06.52933946, 06.72592553,
        06.75644239, 06.76033111, 06.76125533, 06.76217955, 06.98835826,
        06.98928248, 11.45675174, 11.48726860, 11.68477889, 11.71529575,
        11.73249771, 11.89560406, 11.91188181, 11.91280603, 11.93000800,
        11.94332289, 11.96052486, 12.11031632, 12.12363121, 12.13990896,
        12.14083318, 12.15803515, 12.33834347, 12.36886033, 12.37274905,
        12.37367327, 12.54916865, 12.56637061, 12.58357258, 12.59985198,
        12.60077620, 12.60170041, 12.60262463, 12.82880334, 12.82972756,
        13.06071921
    };

    /** Orthotide weight factors. */
    private static final double[] SP = {
        0.0298, 0.1408, 0.0805, 0.6002, 0.3025, 0.1517, 0.0200, 0.0905, 0.0638, 0.3476, 0.1645, 0.0923
    };

    /** Orthoweights for X polar motion. */
    private static final double[] ORTHOWX = {
        -06.77832 * 1.0E-6 * Constant.ARCSEC_TO_RAD,
        -14.86323 * 1.0E-6 * Constant.ARCSEC_TO_RAD,
        +00.47884 * 1.0E-6 * Constant.ARCSEC_TO_RAD,
        -01.45303 * 1.0E-6 * Constant.ARCSEC_TO_RAD,
        +00.16406 * 1.0E-6 * Constant.ARCSEC_TO_RAD,
        +00.42030 * 1.0E-6 * Constant.ARCSEC_TO_RAD,
        +00.09398 * 1.0E-6 * Constant.ARCSEC_TO_RAD,
        +25.73054 * 1.0E-6 * Constant.ARCSEC_TO_RAD,
        -04.77974 * 1.0E-6 * Constant.ARCSEC_TO_RAD,
        +00.28080 * 1.0E-6 * Constant.ARCSEC_TO_RAD,
        +01.94539 * 1.0E-6 * Constant.ARCSEC_TO_RAD,
        -00.73089 * 1.0E-6 * Constant.ARCSEC_TO_RAD
    };

    /** Orthoweights for Y polar motion. */
    private static final double[] ORTHOWY = {
        +14.86283 * 1.0E-6 * Constant.ARCSEC_TO_RAD,
        -06.77846 * 1.0E-6 * Constant.ARCSEC_TO_RAD,
        +01.45234 * 1.0E-6 * Constant.ARCSEC_TO_RAD,
        +00.47888 * 1.0E-6 * Constant.ARCSEC_TO_RAD,
        -00.42056 * 1.0E-6 * Constant.ARCSEC_TO_RAD,
        +00.16469 * 1.0E-6 * Constant.ARCSEC_TO_RAD,
        +15.30276 * 1.0E-6 * Constant.ARCSEC_TO_RAD,
        -04.30615 * 1.0E-6 * Constant.ARCSEC_TO_RAD,
        +00.07564 * 1.0E-6 * Constant.ARCSEC_TO_RAD,
        +02.28321 * 1.0E-6 * Constant.ARCSEC_TO_RAD,
        -00.45717 * 1.0E-6 * Constant.ARCSEC_TO_RAD,
        -01.62010 * 1.0E-6 * Constant.ARCSEC_TO_RAD
    };

    /** Orthoweights for UT1. */
    private static double[] ORTHOWT = {
        -1.76335 *  1.0E-6,
        +1.03364 *  1.0E-6,
        -0.27553 *  1.0E-6,
        +0.34569 *  1.0E-6,
        -0.12343 *  1.0E-6,
        -0.10146 *  1.0E-6,
        -0.47119 *  1.0E-6,
        +1.28997 *  1.0E-6,
        -0.19336 *  1.0E-6,
        +0.02724 *  1.0E-6,
        +0.08955 *  1.0E-6,
        +0.04726 *  1.0E-6
    };

	/**
	 * Implementation of the RAY model for diurnal/subdiurnal tides.
	 * <P>References:
	 * <P>Ray, R. D., Steinberg, D. J., Chao, B. F., and Cartwright, D. E.,
	 * "Diurnal and Semidiurnal Variations in the Earth's Rotation
	 * Rate Induced by Ocean Tides", 1994, Science, 264, pp. 830-832
	 * <P>Petit, G. and Luzum, B. (eds.), IERS Conventions (2010),
	 * IERS Technical Note No. 36, BKG (2010)
	 * @param jd Input date in Julian days in TT.
	 * @return The corrections in arcseconds to the interpolated
	 * values of the x and y pole positions, and the correction to the
	 * UT1-UTC in seconds of time (3rd value). These corrections
	 * provides the estimates of the instantaneous values.
	 */
	public static double[] RAYmodelForDiurnalSubdiurnalTides(double jd) {
          // Compute the time dependent potential matrix
          double t = (jd - Constant.JD_MINUS_MJD) - 37076.5;

          double d60A = t + 2;
          double d60B = t;
          double d60C = t - 2;

          double anm00 = 0;
          double anm01 = 0;
          double anm02 = 0;
          double bnm00 = 0;
          double bnm01 = 0;
          double bnm02 = 0;
          for (int j = 0; j < 41; j++) {
              double hsj = HS[j];
              double pj  = PHASE[j];
              double fj  = FREQUENCY[j];

              double alphaA = pj + fj * d60A;
              anm00 += hsj * Math.cos(alphaA);
              bnm00 -= hsj * Math.sin(alphaA);

              double alphaB = pj + fj * d60B;
              anm01 += hsj * Math.cos(alphaB);
              bnm01 -= hsj * Math.sin(alphaB);

              double alphaC = pj + fj * d60C;
              anm02 += hsj * Math.cos(alphaC);
              bnm02 -= hsj * Math.sin(alphaC);
          }

          double anm10 = 0;
          double anm11 = 0;
          double anm12 = 0;
          double bnm10 = 0;
          double bnm11 = 0;
          double bnm12 = 0;
          for (int j = 41; j < HS.length; j++) {
              double hsj = HS[j];
              double pj  = PHASE[j];
              double fj  = FREQUENCY[j];

              double alphaA = pj + fj * d60A;
              anm10 += hsj * Math.cos(alphaA);
              bnm10 -= hsj * Math.sin(alphaA);

              double alphaB = pj + fj * d60B;
              anm11 += hsj * Math.cos(alphaB);
              bnm11 -= hsj * Math.sin(alphaB);

              double alphaC = pj + fj * d60C;
              anm12 += hsj * Math.cos(alphaC);
              bnm12 -= hsj * Math.sin(alphaC);
          }

          // Orthogonalize the response terms
          double ap0 = anm02 + anm00;
          double am0 = anm02 - anm00;
          double bp0 = bnm02 + bnm00;
          double bm0 = bnm02 - bnm00;
          double ap1 = anm12 + anm10;
          double am1 = anm12 - anm10;
          double bp1 = bnm12 + bnm10;
          double bm1 = bnm12 - bnm10;

          // Fill partials vector
          double partials[] = new double[] {SP[0] * anm01, SP[0] * bnm01, SP[1] * anm01 - SP[2] * ap0,
        		  SP[1] * bnm01 - SP[2] * bp0, SP[3] * anm01 - SP[4] * ap0 + SP[5] * bm0,
        		  SP[3] * bnm01 - SP[4] * bp0 - SP[5] * am0, SP[6] * anm11, SP[6] * bnm11,
        		  SP[7] * anm11 - SP[8] * ap1, SP[7] * bnm11 - SP[8] * bp1, SP[9] * anm11 - SP[10] * ap1 + SP[11] * bm1,
        		  SP[9] * bnm11 - SP[10] * bp1 - SP[11] * am1};

          // Set up corrections
          double dx = 0, dy = 0, dt = 0;
          for (int i=0; i<12; i++) {
        	  dx += partials[i] * ORTHOWX[i];
        	  dy += partials[i] * ORTHOWY[i];
        	  dt += partials[i] * ORTHOWT[i];
          }
          return new double[] {dx*Constant.RAD_TO_ARCSEC, dy*Constant.RAD_TO_ARCSEC, dt};
	}


	/**
	 * Transforms dx and dy values into dpsi and deps. Vondr&aacute;k et al. formulae
	 * for obliquity and precession is used.
	 *
	 * @param dX Celestial pole x offset in arcseconds.
	 * @param dY Celestial pole y offset in arcseconds.
	 * @param TJD Julian day in TT.
	 * @param frame Output frame, to determine if pole offset should be computed respect to
	 * ICRS or dynamical frame.
	 * @return dpsi and deps celestial pole offsets in arcseconds.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double[] dxdyTOdpsideps(double dX, double dY, double TJD,
			FRAME frame) throws JPARSECException
	{
		double T = Functions.toCenturies(TJD);

		// COMPUTE SINE OF MEAN OBLIQUITY OF DATE
		EphemerisElement eph = new EphemerisElement();
		eph.ephemMethod = REDUCTION_METHOD.IAU_2006;
		double SINE = Math.sin(Obliquity.meanObliquity(T, eph));

		/**
		 * THE FOLLOWING ALGORITHM, TO TRANSFORM DX AND DY TO DELTA-DELTA-PSI
		 * AND DELTA-DELTA-EPSILON, IS FROM G. KAPLAN (2003), USNO/AA TECHNICAL
		 * NOTE 2003-03, EQS. (7)-(9). TRIVIAL MODEL OF POLE TRAJECTORY IN GCRS
		 * ALLOWS COMPUTATION OF DZ
		 */
		double X = (2004.19 * T) / Constant.RAD_TO_ARCSEC;
		double DZ = -(X + 0.5 * Math.pow(X, 3.0)) * dX;

		// FORM POLE OFFSET VECTOR (OBSERVED - MODELED) IN GCRS
		double DP1[] = new double[] { dX / Constant.RAD_TO_ARCSEC, dY / Constant.RAD_TO_ARCSEC, DZ / Constant.RAD_TO_ARCSEC };

		// PRECESS POLE OFFSET VECTOR TO MEAN EQUATOR AND EQUINOX OF DATE
		double DP2[] = Ephem.toOutputFrame(DP1, FRAME.ICRF, frame);
		double DP3[] = Precession.precessFromJ2000(TJD, DP2, eph);

		// COMPUTE DELTA-DELTA-PSI AND DELTA-DELTA-EPSILON IN ARCSECONDS
		double PSICOR = (DP3[0] / SINE) * Constant.RAD_TO_ARCSEC;
		double EPSCOR = (DP3[1]) * Constant.RAD_TO_ARCSEC;

		return new double[] { PSICOR, EPSCOR };


		// Newer algorithm by Ch. Bizouard - December 2006, see SUBROUTINE DPSIDEPS2000_DXDY2000(dmjd,dX,dY,dpsi,deps)
		// at ftp://hpiers.obspm.fr/iers/models/uai2000.package
		// Difference with Kaplan is 0.02 muas. I still prefer the old one
/*		double dt = Functions.toCenturies(TJD);

		// Luni-solar precession
		double Psi_A = (5038.47875*dt - 1.07259*dt*dt -0.001147*dt*dt*dt)*Constant.ARCSEC_TO_RAD;

		// Planetary precession
		double Chi_A = (10.5526*dt - 2.38064 * dt*dt -0.001125*dt*dt*dt)*Constant.ARCSEC_TO_RAD;

		double sineps0 = 0.3977771559319137, coseps0 = 0.9174820620691818;

		double dpsi = (-dX  + (Psi_A * coseps0 - Chi_A) *dY)/(-FastMath.pow((Psi_A * coseps0 - Chi_A), 2.0) * sineps0 - sineps0);
		double deps = (-(Psi_A * coseps0 - Chi_A)*sineps0*dX - sineps0*dY)/(-FastMath.pow((Psi_A * coseps0 - Chi_A), 2.0) * sineps0 - sineps0);

		return new double[] { dpsi, deps};
*/
	}

	/**
	 * Resets all EOP parameters to 0. It is not necessary to call this method
	 * even when changing between reduction methods, is automatically done in
	 * ephemerides computation when needed.
	 */
	public static void clearEOP()
	{
		DataBase.addData("EOP", new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0}, true);
	}

	/**
	 * Returns the prediction of the Earth Orientation Parameters for a given date
	 * that should be between current date and the next three months, approximately.
	 * Predictions are obtained by accessing the file ftp://hpiers.obspm.fr/prediction/eop_pred_ncep.final
	 * for IAU1980 algorithms or http://maia.usno.navy.mil/ser7/finals2000A.daily for IAU2000 algorithms.
	 * This method is not used internally in JPARSEC (EOP are never predicted), you should use it
	 * manually if you wish. Note this method can require a few seconds to complete the file transfer.
	 * @param JD_UTC The Julian date in UTC.
	 * @param set True to set the values predicted as the EOP parameters. Note that values will be overwritten
	 * when computing ephemerides unless the input UTC date is the exact output from
	 * {@linkplain TimeScale#getJD(jparsec.time.TimeElement, ObserverElement, EphemerisElement, jparsec.time.TimeElement.SCALE)}.
	 * @param IAU2000 True for IAU2000 predictions, false for IAU1980 predictions.
	 * @param frame The frame to be used when calling {@linkplain #dxdyTOdpsideps(double, double, double, 
	 * EphemerisElement.FRAME)} in case of IAU2000 algorithms.
	 * @return The EOP parameters dPsi, dEps, x, y ("), UT1-UTC and LOD (s), or null in case the prediction
	 * is not available. LOD cannot be always given for IAU2000 algorithms, in case it is unknown 0 is returned.
	 * @throws JPARSECException If an error occurs trying to access the obspm server.
	 */
	public static double[] getEOPPrediction(double JD_UTC, boolean set, boolean IAU2000, FRAME frame) throws JPARSECException {
		double eop[] = null;
		if (!IAU2000) {
			String id = "eop_pred_ncep";
			String d[] = null;
			Object o = DataBase.getData(id, null, false);
			if (o != null) {
				d = (String[]) o;
			} else {
				String data = GeneralQuery.query("ftp://hpiers.obspm.fr/prediction/eop_pred_ncep.final");
				d = DataSet.toStringArray(data, FileIO.getLineSeparator(), true);
				DataBase.addData(id, null, d, false, 6 * 3600); // 6 hrs of life time
			}
			double mjd = JD_UTC - Constant.JD_MINUS_MJD;
			String start = Functions.formatValue((int)mjd, 0)+".0000 ";
			String nextRecord = null;
			for (int i=0; i<d.length; i++) {
				if (d[i].startsWith(start)) {
					d[i] = d[i].trim();
					eop = new double[] {
							DataSet.parseDouble(FileIO.getField(6, d[i], " ", true)),
							DataSet.parseDouble(FileIO.getField(7, d[i], " ", true)),
							DataSet.parseDouble(FileIO.getField(2, d[i], " ", true)),
							DataSet.parseDouble(FileIO.getField(3, d[i], " ", true)),
							DataSet.parseDouble(FileIO.getField(4, d[i], " ", true)),
							DataSet.parseDouble(FileIO.getField(5, d[i], " ", true))
					};
					if (i < d.length - 1) nextRecord = d[i+1];
					break;
				}
			}

			// Simple linear interpolation between current and next record
			if (nextRecord != null && !nextRecord.equals(""))
			{
				double eop0 = DataSet.parseDouble(FileIO.getField(6, nextRecord, " ", true));
				double eop1 = DataSet.parseDouble(FileIO.getField(7, nextRecord, " ", true));
				double eop2 = DataSet.parseDouble(FileIO.getField(2, nextRecord, " ", true));
				double eop3 = DataSet.parseDouble(FileIO.getField(3, nextRecord, " ", true));
				double eop4 = DataSet.parseDouble(FileIO.getField(4, nextRecord, " ", true));
				double eop5 = DataSet.parseDouble(FileIO.getField(5, nextRecord, " ", true));
				double jd0 = (int) (JD_UTC - 0.5) + 0.5, jd1 = jd0 + 1.0;
				double factor = (JD_UTC - jd0) / (jd1 - jd0);
				eop[0] = eop[0] + (eop0 - eop[0]) * factor;
				eop[1] = eop[1] + (eop1 - eop[1]) * factor;
				eop[2] = eop[2] + (eop2 - eop[2]) * factor;
				eop[3] = eop[3] + (eop3 - eop[3]) * factor;
				eop[4] = eop[4] + (eop4 - eop[4]) * factor;
				eop[5] = eop[5] + (eop5 - eop[5]) * factor;
			}

			if (set && eop != null) {
				DataBase.addData("EOP", new double[] {
						// dPsi, dEpsilon, x, y, UT1minusUTC, lastJD, lastMethod
						eop[0], eop[1], eop[2], eop[3], eop[4], JD_UTC, EphemerisElement.REDUCTION_METHOD.IAU_1976.ordinal()
				}, true);
			}
		} else {
			String id = "finals2000A";
			String d[] = null;
			Object o = DataBase.getData(id, null, false);
			if (o != null) {
				d = (String[]) o;
			} else {
				String data = GeneralQuery.query("http://maia.usno.navy.mil/ser7/finals2000A.daily");
				d = DataSet.toStringArray(data, FileIO.getLineSeparator(), true);
				DataBase.addData(id, null, d, false, 6 * 3600); // 6 hrs of life time
			}
			double mjd = JD_UTC - Constant.JD_MINUS_MJD;
			String start = Functions.formatValue((int)mjd, 0)+".00 ";
			String nextRecord = null;
			for (int i=0; i<d.length; i++) {
				if (d[i].substring(7).startsWith(start)) {
					eop = new double[] {
							DataSet.parseDouble(d[i].substring(100, 106).trim()) * 0.001,
							DataSet.parseDouble(d[i].substring(119, 125).trim()) * 0.001,
							DataSet.parseDouble(d[i].substring(18, 27).trim()),
							DataSet.parseDouble(d[i].substring(37, 46).trim()),
							DataSet.parseDouble(d[i].substring(58, 68).trim()),
							-1E10
					};
					String lod = d[i].substring(78, 86).trim();
					if (!lod.equals("")) eop[5] = DataSet.parseDouble(lod) * 0.001;

					if (i < d.length - 1) nextRecord = d[i+1];
					break;
				}
			}

			// Simple linear interpolation between current and next record
			if (nextRecord != null && !nextRecord.equals(""))
			{
				double eop0 = DataSet.parseDouble(nextRecord.substring(100, 106).trim()) * 0.001;
				double eop1 = DataSet.parseDouble(nextRecord.substring(119, 125).trim()) * 0.001;
				double eop2 = DataSet.parseDouble(nextRecord.substring(18, 27).trim());
				double eop3 = DataSet.parseDouble(nextRecord.substring(37, 46).trim());
				double eop4 = DataSet.parseDouble(nextRecord.substring(58, 68).trim());
				double eop5 = -1E10;
				String lod = nextRecord.substring(78, 86).trim();
				if (!lod.equals("")) eop5 = DataSet.parseDouble(lod) * 0.001;
				double jd0 = (int) (JD_UTC - 0.5) + 0.5, jd1 = jd0 + 1.0;
				double factor = (JD_UTC - jd0) / (jd1 - jd0);
				eop[0] = eop[0] + (eop0 - eop[0]) * factor;
				eop[1] = eop[1] + (eop1 - eop[1]) * factor;
				eop[2] = eop[2] + (eop2 - eop[2]) * factor;
				eop[3] = eop[3] + (eop3 - eop[3]) * factor;
				eop[4] = eop[4] + (eop4 - eop[4]) * factor;
				if (eop[5] != -1E10 && eop5 != -1E10) {
					eop[5] = eop[5] + (eop5 - eop[5]) * factor;
				} else {
					eop[5] = 0;
				}
			}

			double TTminusUT1 = TimeScale.getTTminusUT1(new AstroDate(JD_UTC));
			double jd_TT = JD_UTC + eop[4] + TTminusUT1;
			double EOP_2000[] = dxdyTOdpsideps(eop[0], eop[1], jd_TT, frame);
			eop[0] = EOP_2000[0];
			eop[1] = EOP_2000[1];

			if (set && eop != null) {
				DataBase.addData("EOP", new double[] {
						// dPsi, dEpsilon, x, y, UT1minusUTC, lastJD, lastMethod
						eop[0], eop[1], eop[2], eop[3], eop[4], JD_UTC, EphemerisElement.REDUCTION_METHOD.IAU_1976.ordinal()
				}, true);
			}
		}

		return eop;
	}
}
