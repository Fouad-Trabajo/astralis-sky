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
package jparsec.ephem.probes;

import java.text.DecimalFormat;

import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.Functions;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.planets.EphemElement;
import jparsec.ephem.planets.OrbitEphem;
import jparsec.ephem.planets.OrbitalElement;
import jparsec.graph.DataSet;
import jparsec.io.FileFormatElement;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.io.ReadFormat;
import jparsec.math.Constant;
import jparsec.observer.ObserverElement;
import jparsec.time.AstroDate;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.time.TimeScale;
import jparsec.util.JPARSECException;

/**
 * This class provides data for most of the space probes launched so far, as
 * well as methods for ephemeris calculations. It is possible to calculate
 * ephemeris and to obtain ecliptic coordinates of each one in the correct time
 * span where the probe was active, being able to graphically recreate the
 * whole history of space exploration. Of course, this is intended for
 * educational purposes. These calculations require a set of orbital elements for
 * each spacecraft at different times, which is available as a dependency for
 * the JPARSEC library, usually named orbital_elements.jar.
 * <BR>
 * The orbital elements come from the JPL and are suitable for low precision
 * ephemeris. For high precision ephemeris please use the JPL Horizons
 * System.
 * <BR>
 * To use this class follow these simple steps: <BR>
 *
 * <pre>
 * // Search one probe.
 * int probe = Probes.searchProbe(&quot;Galileo-10&quot;);
 *
 * // Modify the previously defined EphemerisElement to account for the algorithm
 * // to apply, and for the selected probe.
 * eph.TARGET_BODY = TARGET.NOT_A_PLANET;
 * eph.ALGORITHM = EphemerisElement.ALGORITHM.PROBE;
 * eph.ORBIT = (OrbitalElement) Probes.getProbeElement(probe);
 *
 * // Calc ephemeris refered to certain Time and Observer objects.
 * EphemElement ephem = orbitEphemeris(time, observer, eph);
 *
 * // A simple alternative (but less efficient for subsequent calculations) using
 * // the main Ephem class is, in its full form:
 *
 * try
 * {
 *		AstroDate astro = new AstroDate(1, AstroDate.JANUARY, 2001, 0, 0, 0);
 *		TimeElement time = new TimeElement(astro(), SCALE.TERRESTRIAL_TIME);
 *		ObserverElement observer = new ObserverElement.parseCity(City.findCity("Madrid"));
 *		EphemerisElement eph = new EphemerisElement(Target.NOT_A_PLANET, EphemerisElement.COORDINATES_TYPE.APPARENT,
 *				EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.TOPOCENTRIC, EphemerisElement.REDUCTION_METHOD.WILLIAMS_1994,
 *				EphemerisElement.FRAME.ICRF, EphemerisElement.ALGORITHM.PROBE);
 *		eph.orbit = Probes.getOrbitalElementsOfMinorBody(&quot;Galileo-10&quot;);
 *		if (eph.orbit != null) {
 *			String name = &quot;Galileo&quot;;
 *			EphemElement ephem = Ephem.getEphemeris(time, observer, eph, true);
 *			ConsoleReport.basicReportToConsole(ephem, name);
 *		}
 * } catch (JPARSECException ve)
 * {
 *		ve.showException();
 * }
 *
 * </pre>
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Spacecraft
{
	// private constructor so that this class cannot be instantiated.
	private Spacecraft() {}

	private static ReadFile readFile;

	/**
	 * Gets the name of the probe without it's current phase.
	 *
	 * @param index Probe index, from 0 to getNumberOfProbes()-1.
	 * @return Name of the probe. Empty string if the index is incorrect.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String getName(int index)
	throws JPARSECException {
		if (readFile == null) {
			ReadFile re = new ReadFile();
			re.setPath(Spacecraft.PATH_TO_PROBES_FILE);
			re.readFileOfProbes();
			readFile = re;
		}

		String data = "";

		if (index < 0 || index >= readFile.getNumberOfObjects())
			return data;

		data = ((OrbitalElement) readFile.getReadElements()[index]).name;

		int phase = data.indexOf("-");
		if (phase < 0) phase = data.length();

		return data.substring(0, phase).trim();
	}

	/**
	 * Gets the index of a probe.
	 *
	 * @param name Probe name.
	 * @return Probe index of the first match, or -1 if no match in found.
	 * @throws JPARSECException If an error occurs.
	 */
	public static int getIndex(String name)
	throws JPARSECException {
		if (readFile == null) {
			ReadFile re = new ReadFile();
			re.setPath(Spacecraft.PATH_TO_PROBES_FILE);
			re.readFileOfProbes();
			readFile = re;
		}

		int index = -1;
		for (int i=0; i<readFile.getNumberOfObjects(); i++)
		{
			if (Spacecraft.getFullName(i).toLowerCase().indexOf(name.toLowerCase()) >= 0) {
				index = i;
				break;
			}
		}

		return index;
	}

	/**
	 * Gets the index of a probe when the phase is not known.
	 *
	 * @param name Probe name, with or without the phase.
	 * @param jd Julian day to search for an active phase.
	 * @return Probe index of the first match, or -1 if no match in found.
	 * @throws JPARSECException If an error occurs.
	 */
	public static int getIndex(String name, double jd)
	throws JPARSECException {
		if (readFile == null) {
			ReadFile re = new ReadFile();
			re.setPath(Spacecraft.PATH_TO_PROBES_FILE);
			re.readFileOfProbes();
			readFile = re;
		}

		int index = -1;
		for (int i=0; i<readFile.getNumberOfObjects(); i++)
		{
			if (Spacecraft.getFullName(i).toLowerCase().indexOf(name.toLowerCase()) >= 0) {
				if (isTimeApplicable(getOrbitalElementsOfProbe(Spacecraft.getFullName(i)), jd)) {
					index = i;
					break;
				}
			}
		}

		return index;
	}

	/**
	 * Gets the full name of the probe. Format is "NAME-PHASE". Phase does not
	 * exist always.
	 *
	 * @param index Probe index, from 0 to getNumberOfProbes()-1.
	 * @return Name of the probe. Empty string if the index is incorrect.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String getFullName(int index)
	throws JPARSECException {
		if (readFile == null) {
			ReadFile re = new ReadFile();
			re.setPath(Spacecraft.PATH_TO_PROBES_FILE);
			re.readFileOfProbes();
			readFile = re;
		}

		String data = "";

		if (index < 0 || index >= readFile.getNumberOfObjects())
			return data;

		data = ((OrbitalElement) readFile.getReadElements()[index]).name;

		return data;
	}

	/**
	 * Gets the phase of the probe. The phase changes when the reactor is on and
	 * the trajectory of the probe is modified. Sometimes could exist several
	 * phases active simultaneously. In that case, the last of them should be
	 * considered as the correct position of the probe.
	 *
	 * @param index Probe index, from 0 to getNumberOfProbes()-1.
	 * @return Phase of the probe. Empty string if no phase value exists, for
	 *         example when the trajectory is Earth to target directly.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String getPhase(int index)
	throws JPARSECException {
		if (readFile == null) {
			ReadFile re = new ReadFile();
			re.setPath(Spacecraft.PATH_TO_PROBES_FILE);
			re.readFileOfProbes();
			readFile = re;
		}

		String name = ((OrbitalElement) readFile.getReadElements()[index]).name;
		String phase = "";

		int symbol = name.indexOf("-");
		if (symbol >= 0)
			phase = name.substring(symbol + 1).trim();

		return phase;
	}

	/**
	 * Searches for a probe name. Search for certain phase is possible by adding
	 * a "-" symbol and the phase numeric value. For example, "Cassini" will
	 * return the first match for Cassini space probe (typically phase 1), and
	 * "Cassini-6" will return the sixth (last) phase for this spacecraft.
	 *
	 * @param name The name of the probe to search for.
	 * @return The ID index value for this probe. -1 if no match is found.
	 * @throws JPARSECException If an error occurs.
	 */
	public static int searchProbe(String name)
	throws JPARSECException {
		if (readFile == null) {
			ReadFile re = new ReadFile();
			re.setPath(Spacecraft.PATH_TO_PROBES_FILE);
			re.readFileOfProbes();
			readFile = re;
		}

		int index = -1;

		for (int i = 0; i < readFile.getNumberOfObjects(); i++)
		{
			String full_name = getFullName(i);
			if (full_name.toLowerCase().indexOf(name.toLowerCase()) >= 0)
			{
				index = i;
				break;
			}
		}

		return index;
	}

	/**
	 * Searches for a probe name active in certain Julian day. Search for
	 * certain phase is possible by adding a "-" symbol and the phase numeric
	 * value, although it is not recommended nor necessary in this method.
	 *
	 * @param name The name of the probe to search for.
	 * @param jd Julian day.
	 * @return The ID value for this probe. -1 if no match is found.
	 * @throws JPARSECException If the reference or applicable times are
	 *         invalid.
	 */
	public static int searchProbe(String name, double jd) throws JPARSECException
	{
		if (readFile == null) {
			ReadFile re = new ReadFile();
			re.setPath(Spacecraft.PATH_TO_PROBES_FILE);
			re.readFileOfProbes();
			readFile = re;
		}

		int index = -1;

		for (int i = 0; i < readFile.getNumberOfObjects(); i++)
		{
			String full_name = getFullName(i);
			if (full_name.toLowerCase().indexOf(name.toLowerCase()) >= 0 && isTimeApplicable(getProbeElement(i), jd))
			{
				index = i;
				break;
			}
		}

		return index;
	}

	/**
	 * Gets the maximum value of the phase for certain probe.
	 *
	 * @param name The name of the probe.
	 * @return The maximum phase. An empty string if no value exists.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String getMaxPhase(String name)
	throws JPARSECException {
		if (readFile == null) {
			ReadFile re = new ReadFile();
			re.setPath(Spacecraft.PATH_TO_PROBES_FILE);
			re.readFileOfProbes();
			readFile = re;
		}

		int max_phase = -1;

		for (int i = 0; i < readFile.getNumberOfObjects(); i++)
		{
			String partial_name = getName(i);
			int phase = Integer.parseInt("0" + getPhase(i));

			if (partial_name.indexOf(name) >= 0 && max_phase < phase)
			{
				max_phase = phase;
			}
		}

		return Integer.toString(max_phase);
	}

	/**
	 * Returns if the current time is valid for certain probe.
	 *
	 * @param orbit Orbit object for the probe.
	 * @param JD Julian day.
	 * @return True is date is valid, false otherwise.
	 */
	public static boolean isTimeApplicable(OrbitalElement orbit, double JD)
	{
		boolean isApp = false;
		if (orbit.beginOfApplicableTime <= JD && orbit.endOfApplicableTime >= JD)
			isApp = true;

		return isApp;
	}

	/**
	 * Returns if the current time is valid for certain probe.
	 *
	 * @param time Time object containing the date.
	 * @param obs Observer object containing the observer position.
	 * @param eph Ephemeris object with the orbital element set included,
	 *        and ephemeris properties.
	 * @return True is date is valid, false otherwise.
	 * @throws JPARSECException If the time or the index is invalid.
	 */
	public static boolean isTimeApplicable(TimeElement time, ObserverElement obs, EphemerisElement eph)
			throws JPARSECException
	{
		// Obtain dynamical time in julian centuries from J2000
		double JD = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);

		return isTimeApplicable(eph.orbit, JD);
	}

	/**
	 * Path to the file of probes, including extension.
	 */
	public static final String PATH_TO_PROBES_FILE = FileIO.DATA_ORBITAL_ELEMENTS_DIRECTORY + "Probes.txt";

	/**
	 * Obtains orbital elements for a certain probe.
	 *
	 * @param index ID value for the probe. From 0 to getNumberOfProbes-1.
	 * @return The Orbit object.
	 * @throws JPARSECException If the reference or applicable times are
	 *         invalid.
	 */
	public static OrbitalElement getProbeElement(int index) throws JPARSECException
	{
		if (readFile == null) {
			ReadFile re = new ReadFile();
			re.setPath(Spacecraft.PATH_TO_PROBES_FILE);
			re.readFileOfProbes();
			readFile = re;
		}

		if (index < 0 || index >= readFile.getNumberOfObjects())
			throw new JPARSECException("probe " + index + " does not exist.");

		return (OrbitalElement) readFile.getReadElements()[index];
	}

	/**
	 * Obtains the number of probes in the database.
	 *
	 * @return The number of objects.
	 * @throws JPARSECException If an error occurs.
	 */
	public static int getNumberOfProbes() throws JPARSECException
	{
		if (readFile == null) {
			ReadFile re = new ReadFile();
			re.setPath(Spacecraft.PATH_TO_PROBES_FILE);
			re.readFileOfProbes();
			readFile = re;
		}

		return readFile.getNumberOfObjects();
	}

	/**
	 * Returns the orbital element of certain probe by its name.
	 * @param name Name, including phase.
	 * @return Orbital element set, or null if no match is found.
	 * @throws JPARSECException If an error occurs.
	 */
	public static OrbitalElement getOrbitalElementsOfProbe(String name) throws JPARSECException
	{
		if (readFile == null) {
			ReadFile re = new ReadFile();
			re.setPath(Spacecraft.PATH_TO_PROBES_FILE);
			re.readFileOfProbes();
			readFile = re;
		}
		int probe = Spacecraft.searchProbe(name);
		if (probe < 0) return null;
		return getProbeElement(probe);
	}

	/**
	 * Obtains orbital elements for all probes.
	 * @return The Orbit objects.
	 * @throws JPARSECException If an error occurs.
	 */
	public static OrbitalElement[] getAllProbeElements() throws JPARSECException
	{
		if (readFile == null) {
			ReadFile re = new ReadFile();
			re.setPath(Spacecraft.PATH_TO_PROBES_FILE);
			re.readFileOfProbes();
			readFile = re;
		}

		OrbitalElement orbits[] = new OrbitalElement[readFile.getNumberOfObjects()];
		Object o[] = readFile.getReadElements();
		for (int i=0; i<orbits.length; i++) {
			orbits[i] = (OrbitalElement) o[i];
		} return orbits;
	}

	/**
	 * Transforms JPL Horizons format of orbital elements to JPARSEC format.
	 * Elements are supposed to be refered to J2000 epoch and calculated in
	 * certain date at 00:00 TT.
	 * <BR>
	 * Example of input format (taken from JPL Horizons system):
	 * <BR>
	 *
	 * <pre>
	 * 2453795.500000000 = A.D. 2006-Mar-01 00:00:00.0000 (CT)
	 * EC= 2.477738107218447E-01 QR= 1.012878320719386E+00 IN= 3.063985846769699E+00
	 * OM= 3.193402961435428E+02 W = 3.584021022460865E+02 Tp=  2453593.124935312662
	 * N = 6.307987654522806E-01 MA= 1.276579409631244E+02 TA= 1.462236568469655E+02
	 * A = 1.346507653092158E+00 AD= 1.680136985464930E+00 PR= 5.707049850388993E+02
	 * </pre>
	 *
	 * <BR>
	 * Output will be, if name set to MRO:
	 * <BR>
	 * MRO e 3.063986 319.340296 358.4021022 1.3465077 0.6307988 0.2477738 127.65794096 03/01.0/2006 2000 0 0
	 *
	 * @param horizons String array of length 5, in the previous format.
	 * @param probe_name Name of the probe.
	 * @param init_jd Initial jd (launch date or after orbit correction).
	 * @param end_jd Final jd (orbit insertion or before orbit correction).
	 * @return JPARSEC format.
	 * @throws JPARSECException If something goes wrong.
	 */
	public static String horizons2JPARSEC(String probe_name, String[] horizons, double init_jd, double end_jd)
			throws JPARSECException
	{
		return orbitalElement2JPARSEC(Spacecraft.horizons2OrbitalElement(probe_name, horizons, init_jd, end_jd));
	}

	/**
	 * Transforms JPL Horizons format of orbital elements to an OrbitalElement object.
	 * <BR>
	 * Example of input format (taken from JPL Horizons system):
	 * <BR>
	 *
	 * <pre>
	 * 2453795.500000000 = A.D. 2006-Mar-01 00:00:00.0000 (CT)
	 * EC= 2.477738107218447E-01 QR= 1.012878320719386E+00 IN= 3.063985846769699E+00
	 * OM= 3.193402961435428E+02 W = 3.584021022460865E+02 Tp=  2453593.124935312662
	 * N = 6.307987654522806E-01 MA= 1.276579409631244E+02 TA= 1.462236568469655E+02
	 * A = 1.346507653092158E+00 AD= 1.680136985464930E+00 PR= 5.707049850388993E+02
	 * </pre>
	 *
	 * @param horizons String array of length 5, in the previous format.
	 * @param probe_name Name of the probe.
	 * @param init_jd Initial jd (launch date or after orbit correction).
	 * @param end_jd Final jd (orbit insertion or before orbit correction).
	 * @return OrbitalElement object.
	 * @throws JPARSECException If something goes wrong.
	 */
	public static OrbitalElement horizons2OrbitalElement(String probe_name, String[] horizons, double init_jd, double end_jd)
			throws JPARSECException
	{
		double time = DataSet.parseDouble(readField("TIME= " + horizons[0], "TIME="));
		double ecc = DataSet.parseDouble(readField(horizons[1], "EC="));
		double inc = DataSet.parseDouble(readField(horizons[1], "IN=")) * Constant.DEG_TO_RAD;
		double node = Functions.normalizeDegrees(DataSet.parseDouble(readField(horizons[2], "OM="))) * Constant.DEG_TO_RAD;
		double arg = Functions.normalizeDegrees(DataSet.parseDouble(readField(horizons[2], "W ="))) * Constant.DEG_TO_RAD;
		double n = DataSet.parseDouble(readField(horizons[3], "N =")) * Constant.DEG_TO_RAD;
		double m0 = Functions.normalizeDegrees(DataSet.parseDouble(readField(horizons[3], "MA="))) * Constant.DEG_TO_RAD;
		double a = DataSet.parseDouble(readField(horizons[4], "A ="));

		OrbitalElement orbit = new OrbitalElement(probe_name, a, arg, ecc, m0, node, inc, time, n, Constant.J2000,
				init_jd, end_jd);

		return orbit;
	}

	/**
	 * Transforms an orbital element set to JPARSEC format of probes.
	 * The invert transformation is provided by {@linkplain ReadFile#parseProbe(String)}.
	 *
	 * @param orbit OrbitalElement set.
	 * @return JPARSEC format.
	 * @throws JPARSECException If something goes wrong.
	 */
	public static String orbitalElement2JPARSEC(OrbitalElement orbit) throws JPARSECException
	{
		String orbit_type = "e";
		double time = orbit.referenceTime;
		double ecc = orbit.eccentricity;
		double inc = orbit.inclination * Constant.RAD_TO_DEG;
		double node = Functions.normalizeRadians(orbit.ascendingNodeLongitude) * Constant.RAD_TO_DEG;
		double arg = Functions.normalizeRadians(orbit.argumentOfPerihelion) * Constant.RAD_TO_DEG;
		double n = orbit.meanMotion * Constant.RAD_TO_DEG;
		double m0 = Functions.normalizeRadians(orbit.meanAnomaly) * Constant.RAD_TO_DEG;
		double a = orbit.semimajorAxis;
		String probe_name = orbit.name;

		ReadFormat rf = new ReadFormat();

		rf.setFormatToRead(FileFormatElement.JPARSEC_PROBES_FORMAT);
		if (ecc == 1.0)
			orbit_type = "p";
		if (ecc > 1.0)
			orbit_type = "h";
		int l = probe_name.length() - ReadFormat.getLength(rf.getField("NAME"));
		if (l > 0)
		{
			probe_name = probe_name.substring(0, rf.getField("NAME").endingPosition);
		}
		if (l < 0)
		{
			l = -l;
			for (int i = 1; i <= l; i++)
			{
				probe_name += " ";
			}
		}

		DecimalFormat formatter1 = new DecimalFormat("00.0000000");
		DecimalFormat formatter1a = new DecimalFormat("0.000000");
		DecimalFormat formatter1b = new DecimalFormat("00.00000");
		DecimalFormat formatter2 = new DecimalFormat("000.00000");
		DecimalFormat formatter3 = new DecimalFormat("000.0000000");
		DecimalFormat formatter4 = new DecimalFormat("00.00000000");
		DecimalFormat formatter5 = new DecimalFormat("00.000000");
		DecimalFormat formatter6 = new DecimalFormat("00");
		DecimalFormat formatter7 = new DecimalFormat("0000");
		DecimalFormat formatter8 = new DecimalFormat("00.0");
		DecimalFormat formatter9 = new DecimalFormat("00.00");

		String output = probe_name + orbit_type;
		output += eliminateLeadingZero(formatter1.format(inc), 2) + " ";
		if (node < 100.0 && node >= 0.0)
		{
			output += eliminateLeadingZero(formatter5.format(node), 2) + " ";
		} else
		{
			output += eliminateLeadingZero(formatter2.format(node), 3) + " ";
		}
		if (arg < 100.0 && arg >= 0.0)
		{
			output += eliminateLeadingZero(formatter4.format(arg), 2);
		} else
		{
			output += eliminateLeadingZero(formatter3.format(arg), 3);
		}
		if (a < 0.0)
		{
			if (a <= -10.0)
			{
				output += " " + eliminateLeadingZero(formatter1b.format(a), 2);
			} else
			{
				output += " " + eliminateLeadingZero(formatter1a.format(a), 1);
			}
		} else
		{
			output += eliminateLeadingZero(formatter1.format(a), 2);
		}
		output += eliminateLeadingZero(formatter1.format(n), 2);
		output += eliminateLeadingZero(formatter4.format(ecc), 2) + " ";
		if (m0 < 100.0 && m0 >= 0.0)
		{
			output += eliminateLeadingZero(formatter4.format(m0), 2) + " ";
		} else
		{
			output += eliminateLeadingZero(formatter3.format(m0), 3) + " ";
		}

		AstroDate astro = new AstroDate(time);
		String date = formatter6.format(astro.getMonth()) + "/" + formatter8.format(astro.getDay()) + "/" + formatter7
				.format(astro.getYear());
		output += date;
		astro = new AstroDate(orbit.referenceEquinox);
		date = formatter7.format((int) (astro.getYear() + astro.getMonth() / 12.0));
		output += "   " + date + "    0 0 ";
		astro = new AstroDate(orbit.beginOfApplicableTime);

		String sep = " ";
		if (formatter7.format(astro.getYear()).length() == 5) sep = "";
		date = formatter7.format(astro.getYear()) + sep + formatter6.format(astro.getMonth()) + " " + formatter9
				.format(astro.getDayPlusFraction()) + " ";
		output += date;
		astro = new AstroDate(orbit.endOfApplicableTime);
		sep = " ";
		if (formatter7.format(astro.getYear()).length() == 5) sep = "";
		date = formatter7.format(astro.getYear()) + sep + formatter6.format(astro.getMonth()) + " " + formatter9
				.format(astro.getDayPlusFraction()) + " ";
		output += date;
		output = output.replaceAll(",", ".");

		return output;
	}

	private static String readField(String line, String field)
	{
		String value = "";
		int a = line.indexOf(field);

		if (a >= 0)
		{
			String rest = line.substring(a + field.length()).trim();
			a = rest.indexOf(" ");
			if (a < 0)
				a = rest.length();
			value = rest.substring(0, a);
		}

		return value;
	}

	private static String eliminateLeadingZero(String val, int beforeP)
	{
		String out = val;

		if (val.startsWith("0"))
			out = " " + val.substring(1);


		int p = out.indexOf(",")-beforeP;
		if (out.startsWith("-")) p --;
		if (p > 0) out = out.substring(0, out.length()-p);
		return out;
	}

	/**
	 * Calculates probes positions, providing full data. This method uses
	 * orbital elements.
	 * <BR>
	 * The orbital element is automatically obtained by this method if the input
	 * orbital element set in the ephemeris object is null, and its target body
	 * number is the desired probe. In this case the target can be set to any
	 * phase of that probe, and if the probe has an active phase in the given
	 * calculation time, then the adequate phase will be automatically taken
	 * instead of the input one.
	 *
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
	public static EphemElement orbitEphemeris(TimeElement time, // Time Element
			ObserverElement obs, // Observer Element
			EphemerisElement eph) // Ephemeris Element
			throws JPARSECException
	{

		// Check Ephemeris object
		if (!EphemerisElement.checkEphemeris(eph))
			throw new JPARSECException(
					"invalid ephemeris object. Check if algorithm is properly set to probe.");

		// Obtain dynamical time in julian centuries from J2000
		double JD = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);

		// Get orbital elements
		OrbitalElement orbit = eph.orbit;
		if (orbit == null && eph.targetBody.getIndex() >= 0) {
			orbit = Spacecraft.getProbeElement(eph.targetBody.getIndex());
			if (!Spacecraft.isTimeApplicable(orbit, JD)) {
				String name = Spacecraft.getName(eph.targetBody.getIndex());
				String maxPhase = Spacecraft.getMaxPhase(name);
				if (!maxPhase.equals("")) {
					int max = Integer.parseInt(maxPhase);
					for (int i=1; i<=max; i++)
					{
						String newName = name + "-" + i;
						int index = Spacecraft.getIndex(newName);
						eph.targetBody.setIndex(index);
						orbit = Spacecraft.getProbeElement(index);
						if (Spacecraft.isTimeApplicable(orbit, JD)) break;
					}
				}
			}
			eph.orbit = orbit;
		}

		// Check date
		if (!isTimeApplicable(orbit, JD))
			throw new JPARSECException(
					"calculation time " + JD + " is outside acceptable interval " + orbit.beginOfApplicableTime + "-" + orbit.endOfApplicableTime + ".");

		// Create new EphemerisElement identical to the input, but with NOT A PLANET id
		EphemerisElement new_eph = eph.clone();
		new_eph.targetBody = TARGET.NOT_A_PLANET;
		new_eph.algorithm = EphemerisElement.ALGORITHM.ORBIT;
		new_eph.orbit = orbit.clone();

		// Return the ephemeris
		try
		{
			EphemElement ephem = OrbitEphem.orbitEphemeris(time, obs, new_eph);
			return ephem;
		} catch (JPARSECException ve)
		{
			throw ve;
		}
	}

	/**
	 * Obtain heliocentric mean rectangular coordinates of a probe.
	 *
	 * @param time Time object containing the date.
	 * @param obs Observer object containing the observer position.
	 * @param eph Ephemeris object with the orbital element set included,
	 *        and ephemeris properties.
	 * @return Array with positions and velocities.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public static double[] obtainPosition(TimeElement time, // Time Element
			ObserverElement obs, // Observer Element
			EphemerisElement eph) // Ephemeris Element
			throws JPARSECException
	{

		// Check Ephemeris object
		if (!EphemerisElement.checkEphemeris(eph))
		{
			throw new JPARSECException(
					"invalid ephemeris object. Check if algorithm is properly set to probe.");
		}

		// Obtain dynamical time in julian centuries from J2000
		double JD = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);

		// Check date
		OrbitalElement orbit = eph.orbit;
		if (!isTimeApplicable(orbit, JD))
			throw new JPARSECException(
					"calculation time " + JD + " is outside acceptable interval " + orbit.beginOfApplicableTime + "-" + orbit.endOfApplicableTime + ".");

		// Create new EphemerisElement identical to the input, but with NOT A PLANET id
		EphemerisElement new_eph = eph.clone();
		new_eph.algorithm = EphemerisElement.ALGORITHM.ORBIT;
		new_eph.targetBody = TARGET.NOT_A_PLANET;
		new_eph.orbit = orbit.clone();

		// Obtain object position
		return OrbitEphem.obtainPosition(time, obs, new_eph);
	}
}
