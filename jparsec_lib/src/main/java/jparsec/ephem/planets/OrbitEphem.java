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

import java.util.ArrayList;

import jparsec.astronomy.Star;
import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.EphemerisElement.ALGORITHM;
import jparsec.ephem.EphemerisElement.FRAME;
import jparsec.ephem.EphemerisElement.REDUCTION_METHOD;
import jparsec.ephem.Functions;
import jparsec.ephem.IAU2006;
import jparsec.ephem.Nutation;
import jparsec.ephem.PhysicalParameters;
import jparsec.ephem.Precession;
import jparsec.ephem.Target;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.moons.MoonEphem;
import jparsec.ephem.planets.OrbitalElement.MAGNITUDE_MODEL;
import jparsec.ephem.planets.imcce.Series96;
import jparsec.ephem.planets.imcce.Vsop;
import jparsec.ephem.probes.Spacecraft;
import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.math.Constant;
import jparsec.math.FastMath;
import jparsec.math.matrix.Matrix;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.SiderealTime;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.time.TimeScale;
import jparsec.util.DataBase;
import jparsec.util.JPARSECException;

/**
 * Provides method for calculating ephemeris of planets, comets, and asteroids,
 * using orbital elements, as well as for obtaining data of the orbital
 * elements.
 * <BR>
 * Orbital elements for comets, asteroids, and other related objects
 * (trans-Neptunian, Centaurs, Trojans, ...) can be read in two formats: the
 * official format of the Minor Planet Center, or the format of the commercial
 * program SkyMap.
 * <BR>
 * Orbital elements of planets were taken from the IMCCE's VSOP87 theory. They
 * cover the time span 1000 B.C. to 5000 A.D., in intervals of 1 year for
 * current epochs and 5 years in the remote past or future. Elements are refered
 * to mean equinox and ecliptic of J2000 epoch. All planets except Earth are
 * available.
 * <BR>
 * These orbital elements are suitable for accurate ephemeris, with errors well
 * below the arcsecond level when comparing to the full VSOP87 theory. An
 * additional advantage is the calculation speed.<BR>
 *
 * Example of use applying the main Ephem class:<BR>
 * <pre>
 * try
 * {
 *		AstroDate astro = new AstroDate(1, AstroDate.JANUARY, 2001, 0, 0, 0);
 *		TimeElement time = new TimeElement(astro, SCALE.TERRESTRIAL_TIME);
 *		ObserverElement observer = ObserverElement.parseCity(City.findCity("Madrid"));
 *		EphemerisElement eph = new EphemerisElement(TARGET.NOT_A_PLANET, EphemerisElement.COORDINATES_TYPE.APPARENT,
 *				EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.TOPOCENTRIC, EphemerisElement.REDUCTION_METHOD.WILLIAMS_1994,
 *				EphemerisElement.FRAME.ICRF, EphemerisElement.ALGORITHM.ORBIT);
 *		eph.orbit = OrbitEphem.getOrbitalElementsOfMinorBody(&quot;Ceres&quot;);
 *		if (eph.orbit != null) {
 *			String name = &quot;Ceres&quot;;
 *			EphemElement ephem = Ephem.getEphemeris(time, observer, eph, true);
 *			ConsoleReport.basicReportToConsole(ephem, name);
 *		}
 * } catch (JPARSECException ve)
 * {
 *		ve.showException();
 * }
 * </pre><BR>
 * It is recommended to use the particular method for this class instead of the main Ephem
 * class. The process is the same as in the {@linkplain Spacecraft} class, changing Probe by Comet or Asteroid.
 *
 * @see OrbitalElement
 * @see ReadFile
 * @see Ephem
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class OrbitEphem
{
	// private constructor so that this class cannot be instantiated.
	private OrbitEphem() {}

	private static ReadFile readFile_asteroids = null;
	private static ReadFile readFile_comets = null;
	private static ReadFile readFile_transNeptunians = null;
	private static ReadFile readFile_NEOs = null;

	/**
	 * Sets an external file for asteroids.
	 * @param file The read file, or null to set the internal file.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void setAsteroidsFromExternalFile(String file[]) throws JPARSECException {
		if (readFile_asteroids != null) readFile_asteroids.setReadElements(null);
		if (file == null) {
			readFile_asteroids = null;
/*			ReadFile re = new ReadFile();
			re.setFormat(ReadFile.FORMAT.MPC);
			re.setPath(OrbitEphem.PATH_TO_MPC_BRIGHT_ASTEROIDS_FILE);
			re.readFileOfAsteroids();
			readFile_asteroids = re;
*/
		} else {
			ReadFile re = new ReadFile();
			re.setFormat(ReadFile.FORMAT.MPC);
			re.setPath("asteroidsExternalFile");
			re.readFileOfAsteroidsFromExternalFile(file);
			readFile_asteroids = re;
		}
	}
	/**
	 * Sets an external file for comets.
	 * @param file The read file, or null to set the internal file.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void setCometsFromExternalFile(String file[]) throws JPARSECException {
		if (readFile_comets != null) readFile_comets.setReadElements(null);
		if (file == null) {
			readFile_comets = null;
/*			ReadFile re = new ReadFile();
			re.setFormat(ReadFile.FORMAT.MPC);
			re.setPath(OrbitEphem.PATH_TO_MPC_COMETS_FILE);
			re.readFileOfComets();
			readFile_comets = re;
*/
		} else {
			ReadFile re = new ReadFile();
			re.setFormat(ReadFile.FORMAT.MPC);
			re.setPath("cometsExternalFile");
			re.readFileOfCometsFromExternalFile(file);
			readFile_comets = re;
		}
	}
	/**
	 * Sets the elements for comets.
	 * @param list The set of comets, or null to set the internal file.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void setCometsFromElements(ArrayList<OrbitalElement> list) throws JPARSECException {
		if (readFile_comets != null) readFile_comets.setReadElements(null);
		if (list == null) {
			readFile_comets = null;
/*			ReadFile re = new ReadFile();
			re.setFormat(ReadFile.FORMAT.MPC);
			re.setPath(OrbitEphem.PATH_TO_MPC_COMETS_FILE);
			re.readFileOfComets();
			readFile_comets = re;
*/
		} else {
			ReadFile re = new ReadFile();
			re.setFormat(ReadFile.FORMAT.MPC);
			re.setPath("cometsExternalFile");
			re.setReadElements(list);
			readFile_comets = re;
		}
	}
	/**
	 * Sets an external file for transNeptunians.
	 * @param file The read file, or null to set the internal file.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void setTransNeptuniansFromExternalFile(String file[]) throws JPARSECException {
		if (readFile_transNeptunians != null) readFile_transNeptunians.setReadElements(null);
		if (file == null) {
			readFile_transNeptunians = null;
/*			ReadFile re = new ReadFile();
			re.setFormat(ReadFile.FORMAT.MPC);
			re.setPath(OrbitEphem.PATH_TO_MPC_DISTANT_BODIES_FILE);
			re.readFileOfAsteroids();
			readFile_transNeptunians = re;
*/
		} else {
			ReadFile re = new ReadFile();
			re.setFormat(ReadFile.FORMAT.MPC);
			re.setPath("transNepsExternalFile");
			re.readFileOfAsteroidsFromExternalFile(file);
			readFile_transNeptunians = re;
		}
	}

	/**
	 * Gets the closest available orbital elements of a planet (except Earth) to
	 * certain julian day. Orbital elements come from VSOP87 Theory, and are
	 * available from Julian Day 1356173.5 to 3545468.3 (years 1000 B.C. to 5000
	 * A.D.), in intervals of about 5 years except years 1700-2100, where the
	 * interval is 1 year. They are refered to dynamical equinox and ecliptic of
	 * J2000 epoch.
	 * <BR>
	 * These orbital elements are suitable for fast ephemeris calculations, with
	 * accuracy better than 1 arcsecond when compared to the full VSOP87
	 * Theory.
	 * <BR>
	 * For Pluto an approximate set of elements (fit to DE102 ephemerides done
	 * by S. Moshier) will be returned. Accuracy of some minutes of arc between
	 * 1400 B.C. to 3000 A.D.
	 *
	 * @param planet ID constant of the planet.
	 * @param JD Julian Day.
	 * @return The {@linkplain OrbitalElement} object.
	 * @throws JPARSECException Thrown if the date is invalid.
	 */
	public static OrbitalElement getOrbitalElements(TARGET planet, double JD) throws JPARSECException
	{
		// Improved search method (valid for the current values of
		// REFERENCE_TIME)
		OrbitalElement out = new OrbitalElement();
		if (planet == TARGET.Pluto)
			return OrbitEphem.getPlutoElements(JD);
		if ((JD < 1356173.5 || JD > 3545468.3) && planet != TARGET.Pluto)
			throw new JPARSECException(
					"invalid date " + JD + ", outside time spand 1356173.5 - 3545468.3.");

		int index = (int) (0.5 + (JD - 1356173.5) / (5.0 * 365.25));
		if (index < 0)
		{
			index = 0;
		} else
		{
			if (index > 539)
			{
				index = 540 + (int) (0.5 + (JD - 2341972.5) / 365.25);
				if (index > 940)
				{
					index = 941 + (int) (0.5 + (JD - 2489895.75) / (5.0 * 365.25));
					if (index > 1520)
						index = 1520;
				}
			}
		}

		double motion = 0.0;
		switch (planet)
		{
		case MERCURY:
			out = Mercury_orbit.Mercury[index];
			motion = 26087.9031415742;
			break;
		case VENUS:
			out = Venus_orbit.Venus[index];
			motion = 10213.2855462110;
			break;
		case MARS:
			out = Mars_orbit.Mars[index];
			motion = 3340.6124266998;
			break;
		case JUPITER:
			out = Jupiter_orbit.Jupiter[index];
			motion = 529.6909650946;
			break;
		case SATURN:
			out = Saturn_orbit.Saturn[index];
			motion = 213.2990954380;
			break;
		case URANUS:
			out = Uranus_orbit.Uranus[index];
			motion = 74.7815985673;
			break;
		case NEPTUNE:
			out = Neptune_orbit.Neptune[index];
			motion = 38.1330356378;
			break;
		default:
			throw new JPARSECException(
					"invalid object. It must be a planet or Pluto, but not the Earth.");
		}

		// Other elements
		out.referenceEquinox = Constant.J2000;
		out.referenceFrame = FRAME.DYNAMICAL_EQUINOX_J2000;
		out.meanMotion = motion / Constant.JULIAN_DAYS_PER_MILLENIA;
		out.name = planet.getName();

		return out;
	}

	/**
	 * Obtain orbital elements for Pluto, mean equinox and ecliptic J2000. This
	 * method provides full elements. By default the CalcOrbit method uses mean
	 * motion, mean anomaly, and argument of perihelion, but the user can derive
	 * also these values using mean longitude, longitude of perihelion, and
	 * applying the formula Mean motion = Constant.EARTH_MEAN_ORBIT_RATE / (sma *
	 * Math.sqrt(sma), assuming a massless object in heliocentric orbit. No full
	 * check has been made about which process is more accurate, but it seems to
	 * give almost identical results.
	 * <BR>
	 * Orbital elements comes from a fit to JPL DE102 ephemeris made by S. L.
	 * Moshier. Elements are valid from 1400 B.C. (JD 1206200.5) to 3000 A.D.
	 * Errors should be below 5 arcminutes respect to DE102 in this time spand.
	 * A set of perturbations, here not considered, can reduce errors to about 5
	 * arcseconds.
	 *
	 * @param JD Julian day for the elements set.
	 * @return {@linkplain OrbitalElement} object.
	 * @throws JPARSECException Thrown if the date is invalid.
	 */
	private static OrbitalElement getPlutoElements(double JD) throws JPARSECException
	{
		double t = (JD - Constant.J2000) / Constant.JULIAN_DAYS_PER_MILLENIA;

		double elpluto[] =
		{
		/* mean distance */
		0.000289, -0.001204, -0.011164, -0.006889, 0.043852, 39.544625,
		/* inclination */
		0.000430, 0.002862, 0.005871, 0.002760, -0.002756, 17.139804,
		/* node */
		-0.001108, -0.007646, -0.017331, -0.015068, -0.079643, 110.308843,
		/* arg perihelion */
		-0.000018, 0.001940, 0.004109, -0.021246, -0.044277, 113.794573,
		/* eccentricity */
		0.000010, 0.000011, -0.000115, -0.000111, 0.000651, 0.249084,
		/* mean anomaly */
		-0.007562, -0.061952, -0.106116, -1.149059, 1452.063327, 374.813282,
		/* perihelion */
		-0.001126, -0.005707, -0.013222, -0.036313, -0.123921, 224.103416,
		/* daily motion */
		-0.00000004, 0.00000018, 0.00000166, 0.00000101, -0.00000659, 0.00396357,
		/* Mean longitude of Pluto to be used for periodic perturbations: */
		-0.008689, -0.067659, -0.119338, -1.185373, 1451.939406, 238.916698, };

		double time[] =
		{ Math.pow(t, 5.0), Math.pow(t, 4.0), Math.pow(t, 3.0), Math.pow(t, 2.0), t, 1.0 };
		OrbitalElement orbit = new OrbitalElement();
		if (JD < 1206200.5 || JD > 2816796.3)
			throw new JPARSECException(
					"invalid date " + JD + ", outside time spand 1206200.5 - 2816796.3.");

		for (int i = 0; i < 6; i++)
		{
			orbit.semimajorAxis += elpluto[i] * time[i];
			orbit.inclination += elpluto[i + 6] * time[i];
			orbit.ascendingNodeLongitude += elpluto[i + 12] * time[i];
			orbit.argumentOfPerihelion += elpluto[i + 18] * time[i];
			orbit.eccentricity += elpluto[i + 24] * time[i];
			orbit.meanAnomaly += elpluto[i + 30] * time[i];
			orbit.perihelionLongitude += elpluto[i + 36] * time[i];
			orbit.meanMotion += elpluto[i + 42] * time[i];
			orbit.meanLongitude += elpluto[i + 48] * time[i];
		}
		orbit.inclination *= Constant.DEG_TO_RAD;
		orbit.ascendingNodeLongitude *= Constant.DEG_TO_RAD;
		orbit.argumentOfPerihelion *= Constant.DEG_TO_RAD;
		orbit.meanAnomaly *= Constant.DEG_TO_RAD;
		orbit.perihelionLongitude *= Constant.DEG_TO_RAD;
		orbit.meanMotion *= Constant.DEG_TO_RAD;
		orbit.meanLongitude *= Constant.DEG_TO_RAD;
		orbit.referenceTime = JD;
		orbit.referenceEquinox = Constant.J2000;

		orbit.name = TARGET.Pluto.getName();

		return orbit;
	}

	/**
	 * Gets the osculating orbital elements of certain major body (any planet or
	 * Pluto), using certain algorithm. Considerations about time span validity
	 * and object disponibility in each algorithm should be taken into account
	 * before calling this method.
	 * <BR>
	 * The orbital element set is adequate for fast and accurate ephemeris
	 * calculations in dates around the calculation time (few days), with initial errors in
	 * the milliarcsecond level comparing with the corresponding full theories,
	 * except with Moshier and VSOP algorithms. For orbital elements adequate for
	 * a longer time span use the method that returns the orbital elements from VSOP theory.
	 *
	 * @param planet ID constant of the planet. For Pluto the Pluto-Charon barycenter is used
	 * with the latest reduction method available.
	 * @param jd Julian Day.
	 * @param algorithm Algorithm to apply. Constants defined in
	 *        EphemerisElement. Can be Moshier, VSOP87, Series96, or JPL DExxx.
	 * @return The {@linkplain OrbitalElement} object.
	 * @throws JPARSECException Thrown if the method, date, or the object is
	 *         invalid.
	 */
	public static OrbitalElement getOrbitalElements(TARGET planet, double jd, EphemerisElement.ALGORITHM algorithm) throws JPARSECException
	{
		OrbitalElement orbit = new OrbitalElement();

		double posP[], velP[];
		switch (algorithm)
		{
		case MOSHIER:
			posP = PlanetEphem.getHeliocentricEclipticPositionJ2000(jd, planet);
			velP = PlanetEphem.getHeliocentricEclipticPositionJ2000(jd + 1.0, planet);
			velP = Functions.substract(velP, posP);

			orbit = OrbitEphem.obtainOrbitalElementsFromPositionAndVelocity(posP, velP, jd, Constant.SUN_MASS / planet.relativeMass);
			orbit.referenceEquinox = Constant.J2000;
			orbit.referenceFrame = FRAME.ICRF;
			break;

		case VSOP87_ELP2000ForMoon:
			posP = Vsop.getHeliocentricEclipticPositionJ2000(jd, planet);
			velP = new double[]	{ posP[3], posP[4], posP[5] };

			orbit = OrbitEphem.obtainOrbitalElementsFromPositionAndVelocity(posP, velP, jd, Constant.SUN_MASS / planet.relativeMass);
			orbit.referenceEquinox = Constant.J2000;
			orbit.referenceFrame = FRAME.DYNAMICAL_EQUINOX_J2000;
			break;
		case SERIES96_MOSHIERForMoon:
			posP = Series96.getHeliocentricEclipticPositionJ2000(jd, planet);
			velP = new double[]	{ posP[3], posP[4], posP[5] };

			orbit = OrbitEphem.obtainOrbitalElementsFromPositionAndVelocity(posP, velP, jd, Constant.SUN_MASS / planet.relativeMass);
			orbit.referenceEquinox = Constant.J2000;
			orbit.referenceFrame = FRAME.ICRF;
			break;
		default:
			if (!algorithm.isJPL()) throw new JPARSECException("invalid algorithm.");
			
			JPLEphemeris jplEphemeris = new JPLEphemeris(algorithm);
			posP = jplEphemeris.getPositionAndVelocity(jd, planet);
			if (planet == TARGET.Pluto) posP = MoonEphem.fromPlutoCenterToPlutoBarycenter(posP, jd, REDUCTION_METHOD.getLatest(), true);

			// Correct posP to center it on the Sun instead of the Solar System barycenter
			velP = jplEphemeris.getPositionAndVelocity(jd, TARGET.SUN);
			posP = Functions.substract(posP, velP);

			// rotate velP and posP to ecliptic
			EphemerisElement eph = new EphemerisElement();
			eph.ephemMethod = REDUCTION_METHOD.JPL_DE4xx;
			posP = Ephem.equatorialToEcliptic(posP, Constant.J2000, eph);
			orbit = OrbitEphem.obtainOrbitalElementsFromPositionAndVelocity(posP, new double[] {posP[3], posP[4], posP[5]}, jd, Constant.SUN_MASS / planet.relativeMass);
			orbit.referenceEquinox = Constant.J2000;
			orbit.referenceFrame = FRAME.ICRF;
			if (algorithm == ALGORITHM.JPL_DE200) orbit.referenceFrame = FRAME.DYNAMICAL_EQUINOX_J2000;
		}

		orbit.name = planet.getName();

		return orbit;
	}

	/**
	 * Returns the number of asteroids.
	 * @return The number of them.
	 * @throws JPARSECException If an error occurs.
	 */
	public static int getAsteroidsCount() throws JPARSECException
	{
		if (readFile_asteroids == null) {
			ReadFile re = new ReadFile();
			re.setFormat(ReadFile.FORMAT.MPC);
			re.setPath(OrbitEphem.PATH_TO_MPC_BRIGHT_ASTEROIDS_FILE);
			try { re.readFileOfAsteroids(); } catch (Exception exc) {}
			readFile_asteroids = re;
		}
		return readFile_asteroids.getNumberOfObjects();
	}
	/**
	 * Returns the number of comets.
	 * @return The number of them.
	 * @throws JPARSECException If an error occurs.
	 */
	public static int getCometsCount() throws JPARSECException
	{
		if (readFile_comets == null) {
			ReadFile re = new ReadFile();
			re.setFormat(ReadFile.FORMAT.MPC);
			re.setPath(OrbitEphem.PATH_TO_MPC_COMETS_FILE);
			try { re.readFileOfComets(); } catch (Exception exc) {}
			readFile_comets = re;
		}
		return readFile_comets.getNumberOfObjects();
	}
	/**
	 * Returns the number of TransNeptunians.
	 * @return The number of them.
	 * @throws JPARSECException If an error occurs.
	 */
	public static int getTransNeptuniansCount() throws JPARSECException
	{
		if (readFile_transNeptunians == null) {
			ReadFile re = new ReadFile();
			re.setFormat(ReadFile.FORMAT.MPC);
			re.setPath(OrbitEphem.PATH_TO_MPC_DISTANT_BODIES_FILE);
			try { re.readFileOfAsteroids(); } catch (Exception exc) {}
			readFile_transNeptunians = re;
		}
		return readFile_transNeptunians.getNumberOfObjects();
	}
	/**
	 * Returns the number of NEOs.
	 * @return The number of them.
	 * @throws JPARSECException If an error occurs.
	 */
	public static int getNEOsCount() throws JPARSECException
	{
		if (readFile_NEOs == null) {
			ReadFile re = new ReadFile();
			re.setFormat(ReadFile.FORMAT.MPC);
			re.setPath(OrbitEphem.PATH_TO_MPC_NEOs_FILE);
			try { re.readFileOfNEOs(-1, -1); } catch (Exception exc) {}
			readFile_NEOs = re;
		}
		return readFile_NEOs.getNumberOfObjects();
	}

	/**
	 * Returns the index of certain asteroid by its name.
	 * @param name Name.
	 * @return The index, or -1 if it is not found.
	 * @throws JPARSECException If an error occurs.
	 */
	public static int getIndexOfAsteroid(String name) throws JPARSECException
	{
		if (readFile_asteroids == null) {
			ReadFile re = new ReadFile();
			re.setFormat(ReadFile.FORMAT.MPC);
			re.setPath(OrbitEphem.PATH_TO_MPC_BRIGHT_ASTEROIDS_FILE);
			try { re.readFileOfAsteroids(); } catch (Exception exc) {}
			readFile_asteroids = re;
		}
		return readFile_asteroids.searchByName(name);
	}
	/**
	 * Returns the index of certain comet by its name.
	 * @param name Name.
	 * @return The index, or -1 if it is not found.
	 * @throws JPARSECException If an error occurs.
	 */
	public static int getIndexOfComet(String name) throws JPARSECException
	{
		if (readFile_comets == null) {
			ReadFile re = new ReadFile();
			re.setFormat(ReadFile.FORMAT.MPC);
			re.setPath(OrbitEphem.PATH_TO_MPC_COMETS_FILE);
			try { re.readFileOfComets(); } catch (Exception exc) {}
			readFile_comets = re;
		}
		return readFile_comets.searchByName(name);
	}
	/**
	 * Returns the index of certain transNeptunian by its name.
	 * @param name Name.
	 * @return The index, or -1 if it is not found.
	 * @throws JPARSECException If an error occurs.
	 */
	public static int getIndexOfTransNeptunian(String name) throws JPARSECException
	{
		if (readFile_transNeptunians == null) {
			ReadFile re = new ReadFile();
			re.setFormat(ReadFile.FORMAT.MPC);
			re.setPath(OrbitEphem.PATH_TO_MPC_DISTANT_BODIES_FILE);
			try { re.readFileOfAsteroids(); } catch (Exception exc) {}
			readFile_transNeptunians = re;
		}
		return readFile_transNeptunians.searchByName(name);
	}
	/**
	 * Returns the index of certain comet by its name.
	 * @param name Name.
	 * @return The index, or -1 if it is not found.
	 * @throws JPARSECException If an error occurs.
	 */
	public static int getIndexOfNEO(String name) throws JPARSECException
	{
		if (readFile_NEOs == null) {
			ReadFile re = new ReadFile();
			re.setFormat(ReadFile.FORMAT.MPC);
			re.setPath(OrbitEphem.PATH_TO_MPC_NEOs_FILE);
			try { re.readFileOfNEOs(-1, -1); } catch (Exception exc) {}
			readFile_NEOs = re;
		}
		return readFile_NEOs.searchByName(name);
	}

	/**
	 * Returns the orbital element of certain asteroid by its index.
	 * @param index Index.
	 * @return Orbital element set. Null is returned if the object is not found.
	 * @throws JPARSECException If an error occurs.
	 */
	public static OrbitalElement getOrbitalElementsOfAsteroid(int index) throws JPARSECException
	{
		if (readFile_asteroids == null) {
			ReadFile re = new ReadFile();
			re.setFormat(ReadFile.FORMAT.MPC);
			re.setPath(OrbitEphem.PATH_TO_MPC_BRIGHT_ASTEROIDS_FILE);
			try { re.readFileOfAsteroids(); } catch (Exception exc) {}
			readFile_asteroids = re;
		}
		if (index >=0 && index < readFile_asteroids.getNumberOfObjects()) {
			OrbitalElement new_orbit = readFile_asteroids.getOrbitalElement(index);
			return new_orbit;
		}
		return null;
	}
	/**
	 * Returns the orbital element of certain comet by its index.
	 * @param index Index.
	 * @return Orbital element set. Null is returned if the object is not found.
	 * @throws JPARSECException If an error occurs.
	 */
	public static OrbitalElement getOrbitalElementsOfComet(int index) throws JPARSECException
	{
		if (readFile_comets == null) {
			ReadFile re = new ReadFile();
			re.setFormat(ReadFile.FORMAT.MPC);
			re.setPath(OrbitEphem.PATH_TO_MPC_COMETS_FILE);
			try { re.readFileOfComets(); } catch (Exception exc) {}
			readFile_comets = re;
		}
		if (index >=0 && index < readFile_comets.getNumberOfObjects()) {
			OrbitalElement new_orbit = readFile_comets.getOrbitalElement(index);
			return new_orbit;
		}
		return null;
	}
	/**
	 * Returns the orbital element of certain transNeptunian by its index.
	 * @param index Index.
	 * @return Orbital element set. Null is returned if the object is not found.
	 * @throws JPARSECException If an error occurs.
	 */
	public static OrbitalElement getOrbitalElementsOfTransNeptunian(int index) throws JPARSECException
	{
		if (readFile_transNeptunians == null) {
			ReadFile re = new ReadFile();
			re.setFormat(ReadFile.FORMAT.MPC);
			re.setPath(OrbitEphem.PATH_TO_MPC_DISTANT_BODIES_FILE);
			try { re.readFileOfAsteroids(); } catch (Exception exc) {}
			readFile_transNeptunians = re;
		}
		if (index >=0 && index < readFile_transNeptunians.getNumberOfObjects()) {
			OrbitalElement new_orbit = readFile_transNeptunians.getOrbitalElement(index);
			return new_orbit;
		}
		return null;
	}
	/**
	 * Returns the orbital element of certain NEO by its index.
	 * @param index Index.
	 * @return Orbital element set. Null is returned if the object is not found.
	 * @throws JPARSECException If an error occurs.
	 */
	public static OrbitalElement getOrbitalElementsOfNEO(int index) throws JPARSECException
	{
		if (readFile_NEOs == null) {
			ReadFile re = new ReadFile();
			re.setFormat(ReadFile.FORMAT.MPC);
			re.setPath(OrbitEphem.PATH_TO_MPC_NEOs_FILE);
			try { re.readFileOfNEOs(-1, -1); } catch (Exception exc) {}
			readFile_NEOs = re;
		}
		if (index >=0 && index < readFile_NEOs.getNumberOfObjects()) {
			OrbitalElement new_orbit = readFile_NEOs.getOrbitalElement(index);
			return new_orbit;
		}
		return null;
	}

	/**
	 * Returns the orbital elements of all asteroids.
	 * @return Orbital elements set.
	 * @throws JPARSECException If an error occurs.
	 */
	public static OrbitalElement[] getOrbitalElementsOfAsteroids() throws JPARSECException
	{
		if (readFile_asteroids == null) {
			ReadFile re = new ReadFile();
			re.setFormat(ReadFile.FORMAT.MPC);
			re.setPath(OrbitEphem.PATH_TO_MPC_BRIGHT_ASTEROIDS_FILE);
			try { re.readFileOfAsteroids(); } catch (Exception exc) {}
			readFile_asteroids = re;
		}
		return (OrbitalElement[]) readFile_asteroids.getReadElements();
	}
	/**
	 * Returns the orbital elements of all comets.
	 * @return Orbital elements set.
	 * @throws JPARSECException If an error occurs.
	 */
	public static OrbitalElement[] getOrbitalElementsOfComets() throws JPARSECException
	{
		if (readFile_comets == null) {
			ReadFile re = new ReadFile();
			re.setFormat(ReadFile.FORMAT.MPC);
			re.setPath(OrbitEphem.PATH_TO_MPC_COMETS_FILE);
			try { re.readFileOfComets(); } catch (Exception exc) {}
			readFile_comets = re;
		}
		return (OrbitalElement[]) readFile_comets.getReadElements();
	}
	/**
	 * Returns the orbital elements of all transNeptunians.
	 * @return Orbital elements set.
	 * @throws JPARSECException If an error occurs.
	 */
	public static OrbitalElement[] getOrbitalElementsOfTransNeptunians() throws JPARSECException
	{
		if (readFile_transNeptunians == null) {
			ReadFile re = new ReadFile();
			re.setFormat(ReadFile.FORMAT.MPC);
			re.setPath(OrbitEphem.PATH_TO_MPC_DISTANT_BODIES_FILE);
			try { re.readFileOfAsteroids(); } catch (Exception exc) {}
			readFile_transNeptunians = re;
		}
		return (OrbitalElement[]) readFile_transNeptunians.getReadElements();
	}
	/**
	 * Returns the orbital elements of all NEOs.
	 * @return Orbital elements set.
	 * @throws JPARSECException If an error occurs.
	 */
	public static OrbitalElement[] getOrbitalElementsOfNEOs() throws JPARSECException
	{
		if (readFile_NEOs == null) {
			ReadFile re = new ReadFile();
			re.setFormat(ReadFile.FORMAT.MPC);
			re.setPath(OrbitEphem.PATH_TO_MPC_NEOs_FILE);
			try { re.readFileOfNEOs(-1, -1); } catch (Exception exc) {}
			readFile_NEOs = re;
		}
		return (OrbitalElement[]) readFile_NEOs.getReadElements();
	}

	private static double lastTDB = -1;
	private static double lastSun0[] = null;
	private static ObserverElement lastObserver = null;

	/**
	 * Calculate object position, providing full data. This method uses orbital
	 * elements. For planets, typical error is well below the arcsecond level
	 * between 1000 B.C. to 5000 A.D, comparing to VSOP87 solution or JPL DE200
	 * Ephemeris. Results for dwarf planet Pluto should be accurate to within 5
	 * arcminutes.
	 * <BR>
	 * To correct for aberration and transform to geocentric the Series96 theory
	 * is used for the position and velocity of the Earth in the 20th and 21st
	 * centuries, otherwise the full VSOP solution.
	 * <BR>
	 * For comets and asteroids, this method returns the angular size of the
	 * body by assuming an albedo of 0.5 and 0.25, respectively.
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
		{
			throw new JPARSECException("invalid ephemeris object.");
		}

		// Obtain dynamical time in julian centuries from J2000
		double JD = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);

		if (lastObserver == null || !lastObserver.equals(obs) || JD != lastTDB) {
			lastSun0 = null;
			lastTDB = -1;
			lastObserver = null;
		}

		OrbitalElement orbit = eph.orbit;

		// Obtain object position
		double plane_orbit_coords[] = orbitPlane(orbit, JD);
		double coords[] = toEclipticPlane(orbit, plane_orbit_coords);
		double[] planetocentricPositionOfTargetSatellite = null;
		try {
			Object o = DataBase.getData("offsetPosition", true);
			if (o != null) {
				planetocentricPositionOfTargetSatellite = (double[]) o;
				coords = Functions.sumVectors(coords, planetocentricPositionOfTargetSatellite);
			}
		} catch (Exception exc) {}
		double sun[] = null;
		if (lastTDB == JD && lastSun0 != null) {
			sun = lastSun0;
		} else {
			if (JD > 2341972.5 && JD < 2488092.5)
			{
				// Priority to Moshier since performance is far better
				try {
					sun = PlanetEphem.getGeocentricPosition(JD, TARGET.SUN, 0.0, false, obs);
					sun = Ephem.eclipticToEquatorial(sun, Constant.J2000, eph);
				} catch (Exception exc) {
					sun = Series96.getGeocentricPosition(JD, TARGET.SUN, 0.0, false, obs);
				}
			} else
			{
				try {
					sun = PlanetEphem.getGeocentricPosition(JD, TARGET.SUN, 0.0, false, obs);
				} catch (Exception exc) {
					sun = Vsop.getGeocentricPosition(JD, TARGET.SUN, 0.0, false, obs);
				}
				sun = Ephem.eclipticToEquatorial(sun, Constant.J2000, eph);
			}
		}

		// Pass to equatorial
		coords = Ephem.eclipticToEquatorial(coords, orbit.referenceEquinox, eph);

		// Precession to J2000, usually not necessary since elements will be
		// referred to J2000
		coords = Precession.precessPosAndVelInEquatorial(orbit.referenceEquinox, Constant.J2000, coords, eph);

		// Obtain light time to correct the position of the object
		double geo[] = Functions.sumVectors(coords, sun);
		double light_time = 0.0;
		double corrected_light_time = 0.0;

		// Correct light time
		if (eph.ephemType != EphemerisElement.COORDINATES_TYPE.GEOMETRIC)
		{
			double topo[] = obs.topocentricObserverICRF(time, eph);
			int niter = 0;
			do
			{
				light_time = Ephem.getTopocentricLightTime(geo, topo, eph);
				plane_orbit_coords = orbitPlane(orbit, JD - light_time);
				coords = toEclipticPlane(orbit, plane_orbit_coords);
				if (planetocentricPositionOfTargetSatellite != null)
					coords = Functions.sumVectors(coords, planetocentricPositionOfTargetSatellite);
				coords = Ephem.eclipticToEquatorial(coords, orbit.referenceEquinox, eph);
				if (orbit.referenceEquinox != Constant.J2000) coords = Precession.precessPosAndVelInEquatorial(orbit.referenceEquinox, Constant.J2000, coords,
						eph);
				geo = Functions.sumVectors(coords, sun);
				corrected_light_time = Ephem.getTopocentricLightTime(geo, topo, eph);
				niter ++;
			} while (Math.abs(light_time - corrected_light_time) > (1.0E-6 / Constant.SECONDS_PER_DAY) && niter < 5);
			light_time = corrected_light_time;
		}

		// Correct for solar deflection and aberration
		if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT)
		{
			if (eph.preferPrecisionInEphemerides)
				//geo = Ephem.solarDeflection(geo, earth, coords);
				geo = Ephem.solarAndPlanetaryDeflection(geo, sun, coords,
					new TARGET[] {TARGET.JUPITER}, JD, false, obs);
			geo = Ephem.aberration(geo, sun, light_time);

			DataBase.addData("GCRS", geo, true); // Geocentric celestial position J2000
		} else {
			DataBase.addData("GCRS", null, true);
		}

		// obtain equatorial position
		double eq[] = geo;

		/* Correction to output frame. */
		if (eph.preferPrecisionInEphemerides ||
				(eph.frame == FRAME.FK4 && orbit.referenceFrame != FRAME.FK4) ||
				(eph.frame != FRAME.FK4 && orbit.referenceFrame == FRAME.FK4)
				)
		eq = Ephem.toOutputFrame(eq, orbit.referenceFrame, eph.frame);
		double[] helio_object = Ephem.toOutputFrame(coords, FRAME.FK5, eph.frame);

		if (eph.frame == FRAME.FK4) {
			// Transform from B1950 to mean equinox of date
			eq = Precession.precess(Constant.B1950, JD, eq, eph);
			 helio_object = Precession.precess(Constant.B1950, JD, helio_object, eph);
		} else {
			// Transform from J2000 to mean equinox of date
			eq = Precession.precessFromJ2000(JD, eq, eph);
			helio_object = Precession.precessFromJ2000(JD, helio_object, eph);
		}

		// Get heliocentric ecliptic position
		LocationElement loc_elem = LocationElement.parseRectangularCoordinates(Ephem.equatorialToEcliptic(helio_object, JD, eph));

		// Mean equatorial to true equatorial
		double true_eq[] = eq;
		if (obs.getMotherBody() == TARGET.EARTH) {
			if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT)
			{
				/* Correct nutation */
				true_eq = Nutation.nutateInEquatorialCoordinates(JD, eph, eq, true);
			}

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

		// Set some variables to improve performance when using loops with the
		// same calculation time
		if (lastObserver == null) {
			lastTDB = JD;
			lastSun0 = sun;
			lastObserver = obs.clone();
		}

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

		/* Topocentric correction */
		if (eph.isTopocentric)
			ephem_elem = Ephem.topocentricCorrection(time, obs, eph, ephem_elem);

		/*
		 * Physical ephemeris. Note we search for the asteroid name to set
		 * appropriate rotational parameters. This is currently available for
		 * Ida, Gaspra, Vesta, and Eros. Ephem type should be apparent to
		 * get correct results.
		 */
		EphemerisElement sun_eph = eph.clone();
		sun_eph.targetBody = TARGET.SUN;
		EphemerisElement eph_aster = eph.clone();
		TARGET aster_id = Target.getID(orbit.name);
		if (aster_id != TARGET.NOT_A_PLANET) eph_aster.targetBody = aster_id;
		Object gcrs = DataBase.getData("GCRS", true);
		// Priority to Moshier since performance is far better
		try {
			ephem_elem = PhysicalParameters.physicalParameters(TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME), PlanetEphem.MoshierEphemeris(time, obs, sun_eph), ephem_elem, obs, eph_aster);
		} catch (Exception exc) {
			ephem_elem = PhysicalParameters.physicalParameters(TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME), Vsop.vsopEphemeris(time, obs, sun_eph), ephem_elem, obs, eph_aster);
		}
		if (obs.getMotherBody() != TARGET.NOT_A_PLANET && obs.getMotherBody() != TARGET.EARTH) {
			LocationElement locE = ephem_elem.getEquatorialLocation();
			locE = Ephem.getPositionFromEarth(locE, time, obs, eph);
			ephem_elem.constellation = jparsec.astronomy.Constellation.getConstellationName(locE.getLongitude(),
					locE.getLatitude(), JD, eph);
		}
		DataBase.addData("GCRS", gcrs, true);

		/* Horizontal coordinates */
		if (eph.isTopocentric)
			ephem_elem = Ephem.horizontalCoordinates(time, obs, eph, ephem_elem);

		ephem_elem.magnitude = ephem_elem.surfaceBrightness = MAGNITUDE_MODEL.MAGNITUDE_UNKNOWN;
		if (orbit.magnitudeModel != null && orbit.magnitudeModel != MAGNITUDE_MODEL.NONE) {
			ephem_elem.magnitude = (float) orbit.getApparentMagnitude(ephem_elem.distance, ephem_elem.distanceFromSun, ephem_elem.phaseAngle);
			switch (eph.targetBody)
			{
			case Asteroid:
			case Vesta:
			case Davida:
			case Steins:
			case Itokawa:
			case Gaspra:
			case Eros:
			case Ida:
			case Lutetia:
			case Ceres:
			case Pallas:
				ephem_elem.angularRadius = (float) Math
						.atan(0.5 * OrbitEphem.getProbableDiameter(orbit.absoluteMagnitude, 0.15) / (ephem_elem.distance * Constant.AU));
				break;
			case Comet:
			case NEO:
			case P19_Borrelly:
			case P9_Tempel_1:
				ephem_elem.angularRadius = (float) Math
						.atan(0.5 * OrbitEphem.getProbableDiameter(orbit.absoluteMagnitude, 0.5) / (ephem_elem.distance * Constant.AU));
				break;
			default:
				switch (orbit.magnitudeModel) {
				case COMET_gk:
					ephem_elem.angularRadius = (float) Math
							.atan(0.5 * OrbitEphem.getProbableDiameter(orbit.absoluteMagnitude, 0.5) / (ephem_elem.distance * Constant.AU));
					break;
				case ASTEROID_HG:
					ephem_elem.angularRadius = (float) Math
							.atan(0.5 * OrbitEphem.getProbableDiameter(orbit.absoluteMagnitude, 0.15) / (ephem_elem.distance * Constant.AU));
					break;
				case NONE:
					ephem_elem.angularRadius = 0;
					break;
				}
				break;
			}

			// Correct apparent magnitude for extinction
			if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT && eph.correctForExtinction &&
					obs.getMotherBody() == TARGET.EARTH)
				ephem_elem.magnitude += Star.getExtinction(Constant.PI_OVER_TWO-ephem_elem.elevation, obs.getHeight() / 1000.0, 5);

			// Compute surface brightness
			ephem_elem.surfaceBrightness = (float) Star.getSurfaceBrightness(ephem_elem.magnitude,
					ephem_elem.angularRadius * Constant.RAD_TO_ARCSEC);
		}

		/* Set coordinates to the output equinox */
		if (EphemerisElement.EQUINOX_OF_DATE != eph.equinox)
			ephem_elem = Ephem.toOutputEquinox(ephem_elem, eph, JD);

		ephem_elem.name = eph.orbit.name;
		return ephem_elem;
	}

	/**
	 * Obtain heliocentric mean ecliptic coordinates of a planet.
	 *
	 * @param time Time object containing the date.
	 * @param obs Observer object containing the observer position.
	 * @param eph Ephemeris object with the target and ephemeris
	 *        properties.
	 * @return Array with positions and velocities (x, y, z, vx, vy, vz).
	 * @throws JPARSECException If the date is invalid.
	 */
	public static double[] obtainPosition(TimeElement time, // Time Element
			ObserverElement obs, // Observer Element
			EphemerisElement eph) // Ephemeris Element
			throws JPARSECException
	{

		// Check Ephemeris object
		if (!EphemerisElement.checkEphemeris(eph))
		{
			throw new JPARSECException("invalid ephemeris object.");
		}

		OrbitalElement orbit = eph.orbit;

		// Obtain dynamical time in julian days
		double JD = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);

		// Obtain object position
		double plane_orbit_coords[] = OrbitEphem.orbitPlane(orbit, JD);
		double coords[] = OrbitEphem.toEclipticPlane(orbit, plane_orbit_coords);

		double finalJD = JD;
		if (eph.equinox != EphemerisElement.EQUINOX_OF_DATE) finalJD = eph.equinox;

		// Precession
		coords = Precession.precessPosAndVelInEcliptic(orbit.referenceEquinox, finalJD, coords, eph);

		return coords;
	}

	/**
	 * Obtain rectangular coordinates of the object in the orbit plane,
	 * according to the object's orbit: elliptic, parabolic, or hyperbolic.
	 *
	 * @param orbit {@linkplain OrbitalElement} object.
	 * @param JD Julian day of calculations.
	 * @return Array with x, y, vx, vy in the orbit plane.
	 */
	public static double[] orbitPlane(OrbitalElement orbit, double JD)
	{
		double exc = orbit.eccentricity;

		if (exc < 1.0)
			return elliptic(orbit, JD);
		if (exc > 0.98 && exc < 1.1) // See Meeus, chapter 34
			return parabolic(orbit, JD);
		if (exc > 1.0)
			return hyperbolic(orbit, JD);

		return new double[] { 0.0, 0.0, 0.0, 0.0 };
	}

	/**
	 * Calculate rectangular coordinates for an elliptic orbit.
	 *
	 * @param orbit {@linkplain OrbitalElement} object. Eccentricity is supposed to be lower
	 *        than unity.
	 * @param JD Julian Day of calculations.
	 * @return Array with x, y, vx, vy values in the orbit plane at time JD,
	 *         refered to the epoch of the orbital elements.
	 */
	public static double[] elliptic(OrbitalElement orbit, double JD)
	{
		double RHO, CE, SE, tmp;
		double mean_anom_at_epoch, m, de, E;
		double x, y, vx, vy;

		/*
		 * mean_anom_at_epoch is proportional to the area swept out by the
		 * radius vector of a circular orbit during the time between perihelion
		 * passage and Julian date JD. It is the mean anomaly at time JD.
		 */
		mean_anom_at_epoch = orbit.meanMotion * (JD - orbit.referenceTime) + orbit.meanAnomaly;
		m = Functions.normalizeRadians(mean_anom_at_epoch);

		/*
		 * By Kepler's second law, m must be equal to the area swept out in the
		 * same time by an elliptical orbit of same total area. Integrate the
		 * ellipse expressed in polar coordinates r = a(1-e^2)/(1 + e cosW) with
		 * respect to the angle W to get an expression for the area swept out by
		 * the radius vector. The area is given by the mean anomaly; the angle
		 * is solved numerically. The answer is obtained in two steps. We first
		 * solve Kepler's equation M = E - eccent*sin(E) for the eccentric
		 * anomaly E. Then there is a closed form solution for W in terms of E.
		 */

		/*
		 * Initial guess improves convergency. Note we are following Danby's
		 * "The Solution of Kepler's Equation", Celestial Mechanics 31, (1983)
		 * 95-107, and Celestial Mechanics 40 (1987), 303-312.
		 */
		E = 0.0;
		if (m != 0.0) {
			E = m + Math.sin(m) * .85 * orbit.eccentricity / Math.abs(Math.sin(m));
			int iteration = 0;
			de = 1.0;
			do
			{
				/*
				 * The approximate area swept out in the ellipse, temp = E - eccent *
				 * sin(E) ...minus the area swept out in the circle, -m ...should be
				 * zero. Use the derivative of the error to converge to solution by
				 * Newton's method.
				 */

				de = (m + orbit.eccentricity * Math.sin(E) - E) / (1.0 - orbit.eccentricity * Math.cos(E));
				E = E + de;
				iteration++;
			} while (iteration < 25 && Math.abs(de) > 1E-15);
		}

		/*
		 * The exact formula for the area in the ellipse is
		 * 2.0*atan(c2*tan(0.5*W)) - c1*eccent*sin(W)/(1+e*cos(W)) where c1 =
		 * sqrt( 1.0 - eccent*eccent ) c2 = sqrt( (1.0-eccent)/(1.0+eccent) ).
		 * Substituting the following value of W yields the exact solution of
		 * true anomaly, but we don't need it.
		 */
		 //tmp = Math.sqrt( (1.0+orbit.eccentricity)/(1.0-orbit.eccentricity) );
		 //double true_anomaly = 2.0 * Math.atan( tmp * Math.tan(0.5*E) );

		/*
		 * Obtain rectangular (cartesian) coordinates and velocities in the
		 * orbit plane.
		 */
		tmp = Math.sqrt(1.0 - orbit.eccentricity * orbit.eccentricity);
		CE = Math.cos(E);
		SE = Math.sin(E);

		// Obtain mean motion
		RHO = 1.0 - orbit.eccentricity * CE;

		// Obtain rectangular position and velocity
		x = orbit.semimajorAxis * (CE - orbit.eccentricity);
		y = orbit.semimajorAxis * tmp * SE;
		vx = -orbit.semimajorAxis * orbit.meanMotion * SE / RHO;
		vy = orbit.semimajorAxis * orbit.meanMotion * tmp * CE / RHO;

		return new double[] { x, y, vx, vy };
	}

	/**
	 * Calculate rectangular coordinates for a parabolic orbit. This method
	 * can be used for eccentricities between 0.98 and 1.1.
	 *
	 * @param orbit {@linkplain OrbitalElement} object.
	 * @param JD Julian Day of calculations.
	 * @return Array with x, y, vx, vy values in the orbit plane at time JD,
	 *         refered to the epoch of the orbital elements.
	 */
	public static double[] parabolic(OrbitalElement orbit, double JD)
	{
		double s = 0.0, ds = 1.0, Q = orbit.perihelionDistance;

		// See Meeus, chapter 33. It's better to use the iterative method
		double w = 3.0 * Constant.EARTH_MEAN_ORBIT_RATE * (JD - orbit.referenceTime) / (Q * Math.sqrt(2.0 * Q));
		int iterN = 0;
		do {
			double newS = (2.0 * s * s * s + w) / (3.0 * (s * s + 1.0));
			ds = Math.abs(s - newS);
			s = newS;
			iterN ++;
		} while (iterN < 25 && ds > 1.0E-15);

		if (ds >= 1.0E-15) {
			// Solve Baker's equation directly
			double B = Math.pow((w * 0.5 + Math.sqrt(w * w / 4.0 + 1.0)), 1.0 / 3.0);
			s = B - 1.0 / B;
		}

		if (orbit.eccentricity != 1.0) {
			double QQ = Constant.EARTH_MEAN_ORBIT_RATE * 0.5 * Math.sqrt((1.0 + orbit.eccentricity)/orbit.perihelionDistance) / orbit.perihelionDistance;
			double GG = (1.0 - orbit.eccentricity) / (1.0 + orbit.eccentricity);
			double tt = (JD - orbit.referenceTime);
			double s2 = QQ * tt - (1.0 - 2.0 * GG) * s * s * s / 3.0 + GG * (2.0 - 3.0 * GG) * s * s * s * s * s / 5.0 - GG * GG * (3.0 - 4.0 * GG) * s * s * s * s * s * s * s / 7.0;
			// Note this is dangerous when tt is high, and some control should be done, although Meeus says nothing
			if (!Double.isInfinite(s2) && !Double.isNaN(s2) && s2 != 0.0 && Math.abs((s-s2)/s2) < 0.25) s = s2;
		}

		// Obtain mean motion
		double k = Constant.EARTH_MEAN_ORBIT_RATE / Math.sqrt(2.0 * Q);
		double r = Q * (1.0 + s * s);

		// Obtain rectangular position and velocity
		double x = Q * (1.0 - s * s);
		double y = 2.0 * Q * s;
		double vx = -k * y / r;
		double vy = k * (x / r + 1.0);

		return new double[] { x, y, vx, vy };
	}

	/**
	 * Calculate rectangular coordinates for a hyperbolic orbit.
	 *
	 * @param orbit {@linkplain OrbitalElement} object. Eccentricity is supposed to be
	 *        greater than unity.
	 * @param JD Julian Day of calculations.
	 * @return Array with x, y, vx, vy values in the orbit plane at time JD,
	 *         refered to the epoch of the orbital elements.
	 */
	public static double[] hyperbolic(OrbitalElement orbit, double JD)
	{
		double mean_anom_at_epoch, m, E, DE, x, y, vx, vy;

		/*
		 * The equation of the hyperbola in polar coordinates r, theta is
		 * r = a(e^2 - 1)/(1 + e cos(theta)) so the perihelion distance q = a(e-1),
		 * the "mean distance" a = q/(e-1).
		 */
		double semimajor_axis = Math.abs(orbit.semimajorAxis);
		mean_anom_at_epoch = orbit.meanAnomaly + orbit.meanMotion * (JD - orbit.referenceTime);
		m = Functions.normalizeRadians(mean_anom_at_epoch);

		/* solve M = -E + e sinh E */
		int iteration = 0;
		/*
		 * Initial guess improves convergency. Note we are following Danby's
		 * "The Solution of Kepler's Equation", Celestial Mechanics 31, (1983)
		 * 95-107, and Celestial Mechanics 40 (1987), 303-312.
		 */
		E = 0.0;
		DE = 0.0;
		if (m != 0.0) {
			E = Math.log(m * 2.0 / orbit.eccentricity + 1.8);
			do
			{
				DE = (-m - E + orbit.eccentricity * Math.sinh(E)) / (1.0 - orbit.eccentricity * Math.cosh(E));
				E = E + DE;
			} while (Math.abs(E) < 100.0 && iteration < 20 && Math.abs(DE) > 1e-10);

			// If no convergency is reached, then retry with a more adequate initial
			// value. Sorry I can't give reference about this, I cannot remember now...
			if (Math.abs(E) > 100.0 || Math.abs(DE) > 1e-5)
			{
				E = (m / Math.abs(m)) * Math.pow(6.0 * Math.abs(m), 1.0 / 3.0);
				do
				{
					DE = (-m - E + orbit.eccentricity * Math.sinh(E)) / (1.0 - orbit.eccentricity * Math.cosh(E));
					E = E + DE;
				} while (Math.abs(E) < 100.0 && iteration < 25 && DE > 1e-10);
			}
		}

		// Exception if no convergency is reached
		if (Math.abs(E) > 100.0 || Math.abs(DE) > 1e-5)
			throw new RuntimeException("no convergency was reached when computing hyperbolic position in orbit plane.");

		// Obtain rectangular position and velocity
		x = semimajor_axis * (orbit.eccentricity - Math.cosh(E));
		y = semimajor_axis * Math.sqrt(orbit.eccentricity * orbit.eccentricity - 1.0) * Math.sinh(E);
		vx = orbit.meanMotion * semimajor_axis * Math.sinh(E) / (1.0 - orbit.eccentricity * Math.cosh(E));
		vy = -orbit.meanMotion * semimajor_axis * Math.sqrt(orbit.eccentricity * orbit.eccentricity - 1.0) * Math
				.cosh(E) / (1.0 - orbit.eccentricity * Math.cosh(E));

		return new double[] { x, y, vx, vy };
	}

	/**
	 * Transform coordinates from the orbit plane to the ecliptic plane.
	 *
	 * @param orbit {@linkplain OrbitalElement} object.
	 * @param position Array with x, y, vx, vy values.
	 * @return Array with x, y, z, vx, vy, vz values.
	 */
	public static double[] toEclipticPlane(OrbitalElement orbit, double[] position)
	{
		double C1, C2, C3, S1, S2, S3;
		double MAT[][] = new double[4][4];
		double out[] = new double[6];

		// Obtain arguments
		C1 = Math.cos(orbit.argumentOfPerihelion);
		C2 = Math.cos(orbit.inclination);
		C3 = Math.cos(orbit.ascendingNodeLongitude);
		S1 = Math.sin(orbit.argumentOfPerihelion);
		S2 = Math.sin(orbit.inclination);
		S3 = Math.sin(orbit.ascendingNodeLongitude);

		// Calculate matrix
		MAT[1][1] = C1 * C3 - S1 * C2 * S3;
		MAT[1][2] = -S1 * C3 - C1 * C2 * S3;
		MAT[1][3] = S2 * S3;
		MAT[2][1] = C1 * S3 + S1 * C2 * C3;
		MAT[2][2] = -S1 * S3 + C1 * C2 * C3;
		MAT[2][3] = -S2 * C3;
		MAT[3][1] = S1 * S2;
		MAT[3][2] = C1 * S2;
		MAT[3][3] = C2;

		// Apply rotation
		out[0] = MAT[1][1] * position[0] + MAT[1][2] * position[1]; // x
		out[1] = MAT[2][1] * position[0] + MAT[2][2] * position[1]; // y
		out[2] = MAT[3][1] * position[0] + MAT[3][2] * position[1]; // z
		out[3] = MAT[1][1] * position[2] + MAT[1][2] * position[3]; // vx
		out[4] = MAT[2][1] * position[2] + MAT[2][2] * position[3]; // vy
		out[5] = MAT[3][1] * position[2] + MAT[3][2] * position[3]; // vz

		return out;
	}

	/**
	 * Geocentric rectangular coordinates of the Sun. Mean equinox and ecliptic
	 * of date. Adequate for fast and low precission ephemeris. Ecliptic latitude
	 * is supossed to be 0.0.
	 * <BR>
	 * Expansion is from "Planetary Programs and Tables" by Pierre Bretagnon and
	 * Jean-Louis Simon, Willman-Bell, 1986.
	 * <BR>
	 * The expansion is valid from 4000 B.C. to 8000 A.D. Stated peak error of
	 * longitude is as follows (tested by Steve L. Moshier against JPL DE200):
	 * <BR>
	 *
	 * <pre>
	 *      years       degrees
	 * <BR>
	 * -4000 to -2000   .0009
	 * <BR>
	 * -2000 to 0       .0007
	 * <BR>
	 *     0 to 1600    .0006
	 * <BR>
	 *  1600 to 2800    .0006
	 * <BR>
	 *  2800 to 8000    .0009
	 * <BR>
	 *</pre>
	 *
	 * Accuracy said by 1986 Astronomical Almanac is 0.1s R.A., 0.8" Dec.
	 * <BR>
	 * This implementation uses the {@linkplain FastMath} class for fast sin/cos
	 * operations, introducing an additional error of the same level as above.
	 * To avoid that you can set in that class operations to exact mode.
	 *
	 * @param JD Julian Day of calculations in dynamical time.
	 * @return Array with x, y, z, vx, vy, vz values. z and vz are supposed to be 0.0.
	 */
	public static double[] sun(double JD)
	{
		double sun_elements[][] =
		{ new double[]
		{ 403406.0, 0.0, 4.721964, 1.621043 }, new double[]
		{ 195207.0, -97597.0, 5.937458, 62830.348067 }, new double[]
		{ 119433.0, -59715.0, 1.115589, 62830.821524 }, new double[]
		{ 112392.0, -56188.0, 5.781616, 62829.634302 }, new double[]
		{ 3891.0, -1556.0, 5.5474, 125660.5691 }, new double[]
		{ 2819.0, -1126.0, 1.512, 125660.9845 }, new double[]
		{ 1721.0, -861.0, 4.1897, 62832.4766 }, new double[]
		{ 0.0, 941.0, 1.163, .813 }, new double[]
		{ 660.0, -264.0, 5.415, 125659.31 }, new double[]
		{ 350.0, -163.0, 4.315, 57533.85 }, new double[]
		{ 334.0, 0.0, 4.553, -33.931 }, new double[]
		{ 314.0, 309.0, 5.198, 777137.715 }, new double[]
		{ 268.0, -158.0, 5.989, 78604.191 }, new double[]
		{ 242.0, 0.0, 2.911, 5.412 }, new double[]
		{ 234.0, -54.0, 1.423, 39302.098 }, new double[]
		{ 158.0, 0.0, .061, -34.861 }, new double[]
		{ 132.0, -93.0, 2.317, 115067.698 }, new double[]
		{ 129.0, -20.0, 3.193, 15774.337 }, new double[]
		{ 114.0, 0.0, 2.828, 5296.67 }, new double[]
		{ 99.0, -47.0, .52, 58849.27 }, new double[]
		{ 93.0, 0.0, 4.65, 5296.11 }, new double[]
		{ 86.0, 0.0, 4.35, -3980.7 }, new double[]
		{ 78.0, -33.0, 2.75, 52237.69 }, new double[]
		{ 72.0, -32.0, 4.5, 55076.47 }, new double[]
		{ 68.0, 0.0, 3.23, 261.08 }, new double[]
		{ 64.0, -10.0, 1.22, 15773.85 }, new double[]
		{ 46.0, -16.0, .14, 188491.03 }, new double[]
		{ 38.0, 0.0, 3.44, -7756.55 }, new double[]
		{ 37.0, 0.0, 4.37, 264.89 }, new double[]
		{ 32.0, -24.0, 1.14, 117906.27 }, new double[]
		{ 29.0, -13.0, 2.84, 55075.75 }, new double[]
		{ 28.0, 0.0, 5.96, -7961.39 }, new double[]
		{ 27.0, -9.0, 5.09, 188489.81 }, new double[]
		{ 27.0, 0.0, 1.72, 2132.19 }, new double[]
		{ 25.0, -17.0, 2.56, 109771.03 }, new double[]
		{ 24.0, -11.0, 1.92, 54868.56 }, new double[]
		{ 21.0, 0.0, .09, 25443.93 }, new double[]
		{ 21.0, 31.0, 5.98, -55731.43 }, new double[]
		{ 20.0, -10.0, 4.03, 60697.74 }, new double[]
		{ 18.0, 0.0, 4.27, 2132.79 }, new double[]
		{ 17.0, -12.0, .79, 109771.63 }, new double[]
		{ 14.0, 0.0, 4.24, -7752.82 }, new double[]
		{ 13.0, -5.0, 2.01, 188491.91 }, new double[]
		{ 13.0, 0.0, 2.65, 207.81 }, new double[]
		{ 13.0, 0.0, 4.98, 29424.63 }, new double[]
		{ 12.0, 0.0, .93, -7.99 }, new double[]
		{ 10.0, 0.0, 2.21, 46941.14 }, new double[]
		{ 10.0, 0.0, 3.59, -68.29 }, new double[]
		{ 10.0, 0.0, 1.5, 21463.25 }, new double[]
		{ 10.0, -9.0, 2.55, 157208.4 } };

		double t, variable;

		t = Functions.toCenturies(JD) / 100.0;
		double L = 0.0, R = 0.0, DL = 0.0, DR = 0.0, U = 0.0;

		for (int i = 0; i < 50; i++)
		{
			variable = sun_elements[i][2] + sun_elements[i][3] * t;
			U = Functions.normalizeRadians(variable);
			L = L + sun_elements[i][0] * FastMath.sin(U);
			R = R + sun_elements[i][1] * FastMath.cos(U);
			DL = DL + sun_elements[i][0] * sun_elements[i][3] * FastMath.cos(U);
			DR = DR - sun_elements[i][1] * sun_elements[i][3] * FastMath.sin(U);
		}

		variable = 62833.196168 * t;
		double tmp = Functions.normalizeRadians(variable);
		variable = 4.9353929 + tmp + L / 10000000.0;
		L = Functions.normalizeRadians(variable);
		R = 1.0001026 + R / 10000000.0;
		DL = (62833.196168 + DL / 10000000.0) / 3652500.0;
		DR = (DR / 10000000.0) / 3652500.0;

		double x = R * FastMath.cos(L);
		double y = R * FastMath.sin(L);
		double z = 0.0;
		double vx = DR * FastMath.cos(L) - DL * y;
		double vy = DR * FastMath.sin(L) + DL * x;
		double vz = 0.0;

		return new double[] { x, y, z, vx, vy, vz };
	}

	/**
	 * Obtain probable diameter of a dwarf object knowing the absolute magnitude
	 * and the albedo.
	 * <BR>
	 * The value is given by evaluating a function that was obtained after a fit
	 * from a tabulated table from the Minor Planet Center, called <I>Conversion
	 * of Absolute Magnitude to Diameter</I>. The fit found is excelent in the
	 * albedo interval of the table, 0.05 to 0.5.
	 * <BR>
	 * Typical icy objects in the outer Solar System will have an albedo of 0.5.
	 * Rocky objects lies between 0.25 and 0.05.
	 *
	 * @param H Absolute magnitude.
	 * @param albedo Albedo of the object.
	 * @return Probable diameter in km.
	 */
	public static double getProbableDiameter(double H, double albedo)
	{
		double diameter = (4172.0 * Math.sqrt(albedo)) * Math.exp(-0.23 * H);

		return diameter;
	}

	/**
	 * Obtain probable albedo of a dwarf object knowing the absolute magnitude
	 * and the radius.
	 * <BR>
	 * The value is given by evaluating a function that was obtained after a fit
	 * from a tabulated table from the Minor Planet Center, called <I>Conversion
	 * of Absolute Magnitude to Diameter</I>. The fit found is excelent in the
	 * albedo interval of the table, 0.05 to 0.5.
	 *
	 * @param H Absolute magnitude.
	 * @param diameter Diameter of the object in km.
	 * @return Probable albedo.
	 */
	public static double getProbableAlbedo(double H, double diameter)
	{
		double albedo = Math.pow((diameter / 4172.0) / Math.exp(-0.23 * H), 2.0);

		return albedo;
	}

	/**
	 * Obtains the mean motion of an object in a elliptic orbit given it's
	 * semimajor axis. This method takes use of the mass of the body, so it
	 * slightly improves accuracy respect to default value for a massless
	 * object.
	 *
	 * @param planet Planet ID constant, can be any planet or Pluto, or
	 * any other to assume a massless body.
	 * @param a Semimajor axis.
	 * @return The mean motion in radians per day.
	 */
	public static double obtainMeanMotion(TARGET planet, double a)
	{
		double mass = 0.0;
		if (planet.isPlanet()) mass = 1.0 / planet.relativeMass;

		return obtainMeanMotion(mass, a);
	}

	/**
	 * Obtains the mean motion of an object in a elliptic orbit given it's
	 * semimajor axis and mass. The mass ob the body is considered, so it
	 * slightly improves accuracy respect to default value for a massless
	 * object.
	 *
	 * @param mass Mass of the body in kg.
	 * @param a Semimajor axis.
	 * @return The mean motion in radians per day.
	 */
	public static double obtainMeanMotion(double mass, double a)
	{
		double G = Constant.GRAVITATIONAL_CONSTANT * Constant.SUN_MASS * Math.pow(Constant.SECONDS_PER_DAY, 2.0) / Math
				.pow(Constant.AU * 1000.0, 3.0);
		double mean_motion = Math.sqrt(G * (1.0 + mass / Constant.SUN_MASS) / a) / a;
		return mean_motion;
	}

	/**
	 * Solves an orbit from 3 observations, returning the approximate orbital elements of the body.
	 * This method is adequate for short time spands in the observations, so that perturbations
	 * are not very important, but not very short, since the orbit should be sampled enough.
	 * Observations must be sorted by date.<BR>
	 *
	 * This implementations follows <i>Astronomy on the Personal Computer</i>,
	 * by Oliver Montenbruck.<BR>
	 *
	 * To use this method adequately with topocentric observations the flag topocentric should
	 * be enabled in each of the ephemeris input objects, otherwise the observations will be
	 * considered as geocentric. In case of topocentric observations the input positions
	 * should contain a correct value for the distance to the object (in AU). Obviously this is
	 * not possible when estimating the orbital elements for the first time. In that case,
	 * it is recommended to use a high value for this distance (100 AU) for instance, so that
	 * the conversion to geocentric position done inside this method will maintain the input
	 * positions. After the first orbital fit, it is possible to calculate the true 
	 * distance for each of the input time values, and to repeat the call to this method with the
	 * correct distances. The new returned orbital elements will be more accurate, which means
	 * they should return a lower value of the rms (set as the object name field in the returned
	 * elements).
	 *
	 * @param loc Object's apparent position (RA, DEC, and distance) for the three different
	 * observations.
	 * @param time Time objects for the observations.
	 * @param obs Observer positions in each observation.
	 * @param eph Ephemeris objects for each observation.
	 * @return Orbital element set. The name of the body in the orbit object is set to the rms
	 * of the fit in radians.
	 * @throws JPARSECException If no convergence is found.
	 */
	public static OrbitalElement solveOrbit(LocationElement loc[], TimeElement time[],
			ObserverElement obs[], EphemerisElement eph[]) throws JPARSECException
	{
		if (loc.length != time.length || loc.length != obs.length || time.length != obs.length || loc.length != 3)
			throw new JPARSECException("Please enter 3 observations.");

		// Pass to mean ecliptic J2000
		LocationElement nloc[] = new LocationElement[loc.length];
		double earth[][] = new double[loc.length][3];
		double jdPrev = -1;
		EphemerisElement neph[] = new EphemerisElement[loc.length];
		for (int i=0; i<loc.length; i++) {
			nloc[i] = Ephem.toMeanEquatorialJ2000(loc[i], time[i], obs[i], eph[i]);

			double JD0 = TimeScale.getJD(time[i], obs[i], eph[i], SCALE.BARYCENTRIC_DYNAMICAL_TIME), JD = Constant.J2000;
			if (jdPrev != -1 && jdPrev >= JD0) throw new JPARSECException("Input observations must be sorted by date.");
			jdPrev = JD0;
			nloc[i] = LocationElement.parseRectangularCoordinates(Ephem.equatorialToEcliptic(nloc[i].getRectangularCoordinates(), JD, eph[i]));

			earth[i] = PlanetEphem.getHeliocentricEclipticPositionJ2000(JD0, TARGET.EARTH);

			neph[i] = eph[i].clone();
			neph[i].algorithm = ALGORITHM.ORBIT;
			neph[i].targetBody = TARGET.Asteroid;
			neph[i].correctForEOP = false;
			neph[i].correctForPolarMotion = false;
		}

		// Initial approximations of n1 and n3
		double jd0[] = new double[] {
				TimeScale.getJD(time[0], obs[0], eph[0], SCALE.BARYCENTRIC_DYNAMICAL_TIME),
				TimeScale.getJD(time[1], obs[1], eph[1], SCALE.BARYCENTRIC_DYNAMICAL_TIME),
				TimeScale.getJD(time[2], obs[2], eph[2], SCALE.BARYCENTRIC_DYNAMICAL_TIME)
		};
		if (jd0[0] > jd0[1] || jd0[0] > jd0[2] || jd0[1] > jd0[2])
			throw new JPARSECException("Observations not sorted by date.");

		// Uncomment to use the test by Montenbruck, page 251
		// jd0 = new double[] {2380570.51, 2380704.42, 2380830.35};

		double n[] = new double[] {
				(jd0[2]-jd0[1]) / (jd0[2]-jd0[0]),
				-1.0,
				(jd0[1]-jd0[0]) / (jd0[2]-jd0[0])
		};

		// Matrix d and its determinant
		double rsun[][] = earth.clone(), e[][] = new double[3][3], rpl[][] = new double[3][3];
		double dd[][] = new double[3][3];
		for (int i=0; i<3; i++) {
			rsun[i][0] = -rsun[i][0];
			rsun[i][1] = -rsun[i][1];
			rsun[i][2] = -rsun[i][2];
			e[i] = nloc[i].getRectangularCoordinates();

			// Uncomment to use the test by Montenbruck, page 251
			/*
			rsun[i] = DataSet.getSubArray(OrbitEphem.sun(jd0[i]), 0, 2);
			e[0] = new LocationElement(95.54 * Constant.DEG_TO_RAD, -0.99 * Constant.DEG_TO_RAD, 1.0).getRectangularCoordinates();
			e[1] = new LocationElement(99.82 * Constant.DEG_TO_RAD, 7.28 * Constant.DEG_TO_RAD, 1.0).getRectangularCoordinates();
			e[2] = new LocationElement(118.10 * Constant.DEG_TO_RAD, 7.65 * Constant.DEG_TO_RAD, 1.0).getRectangularCoordinates();
			//double lon = LocationElement.parseRectangularCoordinates(rsun[i]).getLongitude() * Constant.RAD_TO_DEG;
			//System.out.println("sun lon "+lon);
			*/
		}

		double di[] = Functions.crossProduct(e[1], e[2]);
		for (int i=0; i<3; i++) {
			dd[0][i] = Functions.scalarProduct(di, rsun[i]);
		}
		di = Functions.crossProduct(e[2], e[0]);
		for (int i=0; i<3; i++) {
			dd[1][i] = Functions.scalarProduct(di, rsun[i]);
		}
		di = Functions.crossProduct(e[0], e[1]);
		for (int i=0; i<3; i++) {
			dd[2][i] = Functions.scalarProduct(di, rsun[i]);
		}
		double det = Functions.scalarProduct(e[2], di);

		// Iterate until distance rho[1] does not change any more
		double rho[] = new double[3], eta[] = new double[3], jd[] = new double[3];
		rho[1] = 0;
		double eps_rho = 1E-8, rhoold = 0;

		do {
			rhoold = rho[1];

			// Geocentric distance rho from n1 and n3
			double tau[] = new double[3];
			for (int i=0; i<3; i++) {
				rho[i] = (n[0] * dd[i][0] - dd[i][1] + n[2] * dd[i][2]) / (n[i] * det);
				jd[i] = jd0[i] - Constant.LIGHT_TIME_DAYS_PER_AU * rho[i];
			}
			// Uncomment to use the test by Montenbruck, page 251
			// System.out.println(rho[0]+"/"+rho[1]+"/"+rho[2]);

			// Light time correction and calculate time differences
			tau[0] = Constant.EARTH_MEAN_ORBIT_RATE * (jd[2] - jd[1]);
			tau[1] = Constant.EARTH_MEAN_ORBIT_RATE * (jd[2] - jd[0]);
			tau[2] = Constant.EARTH_MEAN_ORBIT_RATE * (jd[1] - jd[0]);

			// Heliocentric coordinate vectors
			for (int i=0; i<3; i++) {
				for (int s=0; s<3; s++) {
					rpl[i][s] = rho[i] * e[i][s] - rsun[i][s];
				}
			}

			// Sector/triangle ratios
			eta[0] = eta(rpl[1], rpl[2], tau[0]);
			eta[1] = eta(rpl[0], rpl[2], tau[1]);
			eta[2] = eta(rpl[0], rpl[1], tau[2]);

			// Improvement of the sector/triangle ratios
			n[0] = (tau[0] / eta[0]) / (tau[1] / eta[1]);
			n[2] = (tau[2] / eta[2]) / (tau[1] / eta[1]);
		} while(Math.abs(rho[1] - rhoold) >= eps_rho);

		OrbitalElement orbit = obtainOrbitalElementsFromTwoPosition(jd[0], jd[2], rpl[0], rpl[2]);
		EphemerisElement ephCopy[] = new EphemerisElement[] {eph[0].clone(), eph[1].clone(), eph[2].clone()};
		orbit.name = "dummy";
		orbit.referenceEquinox = Constant.J2000;
		ephCopy[0].orbit = orbit;
		ephCopy[1].orbit = orbit;
		ephCopy[2].orbit = orbit;
		orbit.name = ""+getPositionError(loc, time, obs, ephCopy);
		return orbit;
	}

	/**
	 * Returns the orbital elements from two observations of a body.
	 * This implementations follows <i>Astronomy on the Personal Computer</i>,
	 * by Oliver Montenbruck.
	 * @param jda Observation at time 1.
	 * @param jdb Observation at time 2.
	 * @param ra Ecliptic coordinates of the body at time 1.
	 * @param rb Ecliptic coordinates of the body at time 2.
	 * @return The set of elements.
	 * @throws JPARSECException In case there is no convergency or the input
	 * arrays are of incorrect sizes.
	 */
	public static OrbitalElement obtainOrbitalElementsFromTwoPosition(double jda, double jdb, double ra[], double rb[]) throws JPARSECException {
		// Compute r0 (fraction of rb perpendicular to ra)
		double sa = Functions.getNorm(ra), sb = Functions.getNorm(rb);
		double ea[] = Functions.scalarProduct(ra, 1.0 / sa);
		if (ea.length == 6) ea = DataSet.getSubArray(ea, 0, 2);
		double fac = Functions.scalarProduct(DataSet.getSubArray(rb, 0, 2), ea);
		double r0[] = new double[] {
				rb[0] - fac * ea[0],
				rb[1] - fac * ea[1],
				rb[2] - fac * ea[2]
		};
		double s0 = Functions.getNorm(r0);
		double e0[] = Functions.scalarProduct(r0, 1.0 / s0);

		// Inclination and ascending node
		double r[] = Functions.crossProduct(ea, e0);
		LocationElement loc = LocationElement.parseRectangularCoordinates(-r[1], r[0], r[2]);
		double lan = loc.getLongitude(), inc = Constant.PI_OVER_TWO - loc.getLatitude();
		double u;
		if (inc == 0) {
			u = Math.atan2(ra[1], ra[0]);
		} else {
			u = Math.atan2(e0[0] * r[1] - e0[1] * r[0], -ea[0] * r[1] + ea[1] * r[0]);
		}

		// Semilatus rectum p
		double tau = Constant.EARTH_MEAN_ORBIT_RATE * Math.abs(jdb - jda);
		double eta = eta(ra, rb, tau);
		double p = sa * s0 * eta / tau;
		p *= p;

		// Eccentricity, true anomaly, and perihelion longitude
		double cos_dny = fac / sb, sin_dny = s0 / sb;
		double ecos_ny = p / sa - 1.0, esin_ny = (ecos_ny * cos_dny - (p / sb - 1.0)) / sin_dny;
		loc = LocationElement.parseRectangularCoordinates(ecos_ny, esin_ny, 0.0);
		double ecc = loc.getRadius(), ny = loc.getLongitude();
		double aop = Functions.normalizeRadians(u - ny);

		// Perihelion distance, semimajor axis, and mean daily motion
		double q = p / (1.0 + ecc), ax = q / (1.0 - ecc);
		double n = Constant.EARTH_MEAN_ORBIT_RATE / Math.sqrt(Math.abs(ax * ax * ax));

		// Mean anomaly and perihelion time
		double e, m;
		if (ecc < 1.0) {
			e = Math.atan2(Math.sqrt((1.0 - ecc) * (1.0 + ecc)) * esin_ny, ecos_ny + ecc * ecc);
			m = e - ecc * Math.sin(e);
		} else {
			double shh = Math.sqrt((ecc - 1.0) * (ecc + 1.0)) * esin_ny / (ecc + ecc * ecos_ny);
			m = ecc * shh - Math.log(shh + Math.sqrt(1.0 + shh * shh));
		}
		//double tp = jda - m / n;

		OrbitalElement orbit = new OrbitalElement(ax, m+aop+lan, ecc, aop+lan, lan, inc, jda);
		orbit.meanMotion = n;
		return orbit;
	}

	// sector/triangle ratio from two positions and time difference
	// See Montenbruck's Astronomy on the Personal Computer, page 232
	private static Double eta(double ra[], double rb[], double tau) throws JPARSECException {
		double sa = Functions.getNorm(ra), sb = Functions.getNorm(rb);
		double kappa = Math.sqrt(2.0 * (sa * sb + Functions.scalarProduct(ra, rb)));
		double m = tau * tau / (FastMath.pow(kappa, 3));
		double l = (sa + sb) / (2.0 * kappa) - 0.5;
		double eta_min = Math.sqrt(m / (l + 1.0));

		// Hansen approximation
		double eta2 = (12.0 + 10.0 * Math.sqrt(1.0 + (44.0 / 9.0) * m / (l + 5.0 / 6.0))) / 22.0;
		double eta1 = eta2 + 0.1, f1 = F(eta1, m, l), f2 = F(eta2, m, l);
		int i = 0, maxit = 30;

		// Secant method
		double delta = 1.0E-9;
		while(Math.abs(f2 - f1) > delta && i < maxit) {
			double d_eta = -f2 * (eta2 - eta1) / (f2 - f1);
			eta1 = eta2;
			f1 = f2;
			while(eta2 + d_eta <= eta_min) {
				d_eta *= 0.5;
			};
			eta2 += d_eta;
			f2 = F(eta2, m, l);
			i ++;
		};
		if (i == maxit) throw new JPARSECException("No convergence"); // No convergence
		return eta2;
	}

	// 1 - eta + (m / eta^2) * w ( m / eta^2 - 1)
	private static double F(double eta, double m, double l) {
		double eps = 1.0E-10;
		double w = m / (eta * eta) - l, ww = 0;;
		if (Math.abs(w) < 0.1) { // series expansion
			double a = 4.0 / 3.0, n = 0;
			ww = a;
			do {
				n ++;
				a *= w * (n + 2.0) / (n + 1.5);
				ww += a;
			} while(Math.abs(a) >= eps);
		} else {
			if (w > 0) {
				double g = 2.0 * Math.atan(Math.sqrt(w / (1.0 - w)));
				double s = Math.sin(g);
				ww = (2.0 * g - Math.sin(2.0 * g)) / (s * s * s);
			} else {
				double g = 2.0 * Math.log(Math.sqrt(-w) + Math.sqrt(1.0 - w));
				double e = Math.exp(g), s = 0.5 * (e - 1.0 / e);
				e *= e;
				ww = (0.5 * (e - 1.0 / e) - 2.0 * g) / (s * s * s);
			}
		}
		return 1.0 - eta + (w + l) * ww;
	}

	private static double getPositionError(LocationElement loc[], TimeElement time[], ObserverElement obs[], EphemerisElement eph[])
			throws JPARSECException {
		double rms = 0;
		for (int i = 0; i< loc.length; i++) {
			EphemElement ephem = OrbitEphem.orbitEphemeris(time[i], obs[i], eph[i]);
			double d = LocationElement.getAngularDistance(loc[i], ephem.getEquatorialLocation());
			rms += d * d;
		}
		rms = Math.sqrt(rms / loc.length);
		return rms;
	}

	/**
	 * Obtain a set of orbital elements knowing the position and velocity
	 * vectors in certain instant, according to the classical theory of the two
	 * body motion.
	 * <BR>
	 * For reference see Practical Ephemeris Calculations, by Oliver
	 * Montenbruck, chapter 3.
	 *
	 * @param pos Geometric heliocentric position vector in ecliptic coordinates, in AU.
	 * @param v Heliocentric velocity vector in ecliptic coordinates, in AU /
	 *        day.
	 * @param jd Time of vectors as Julian day.
	 * @param mass Mass of the orbiting body in kg. Can be set to zero if it is
	 *        very low or unknown.
	 * @return A set of orbital elements describing the shape and orientation of
	 *         the orbit, and ready for subsequent ephemeris calculations (all
	 *         data except magnitude, mag slope, name, and applicable times).
	 *         Mean equinox and ecliptic of date.
	 * @throws JPARSECException If the orbit is a straight line through the sun.
	 */
	public static OrbitalElement obtainOrbitalElementsFromPositionAndVelocity(double pos[], double v[], double jd,
			double mass) throws JPARSECException
	{
		double c[] = new double[]
		{ pos[1] * v[2] - pos[2] * v[1], pos[2] * v[0] - pos[0] * v[2], pos[0] * v[1] - pos[1] * v[0] };

		LocationElement cloc = LocationElement.parseRectangularCoordinates(c);
		double modc = cloc.getRadius();
		if (modc == 0.0)
		{
			throw new JPARSECException(
					"movement is in straight line through the sun. No solution.");
		}

		double inc = Math.acos(c[2] / modc);
		double omega = Math.atan2(c[0], -c[1]);

		double r = Math.sqrt(pos[0] * pos[0] + pos[1] * pos[1] + pos[2] * pos[2]);
		double sinu = pos[2] / (r * Math.sin(inc));
		double cosu = (pos[0] * Math.cos(omega) + pos[1] * Math.sin(omega)) / r;
		double u = Math.atan2(sinu, cosu);

		double v2 = v[0] * v[0] + v[1] * v[1] + v[2] * v[2];
		double G = Constant.GRAVITATIONAL_CONSTANT * Constant.SUN_MASS * FastMath.pow(Constant.SECONDS_PER_DAY, 2.0) / FastMath
				.pow(Constant.AU * 1000.0, 3.0);
		double a_inverse = 2.0 / r - v2 / (G * (1.0 + mass / Constant.SUN_MASS));

		double p = modc * modc / (G * (1.0 + mass / Constant.SUN_MASS));
		double exc = Math.sqrt(1.0 - p * a_inverse);
		double q = p / (1.0 + exc);

		double a = 0.0;
		if (a_inverse != 0.0)
			a = 1.0 / a_inverse;

		double true_anomaly = 0.0, mean_motion = 0.0, mean_anomaly = 0.0, perihelion_time = 0.0;
		// Elliptic motion
		if (a_inverse > 0.0)
		{
			double cosE = (1.0 - r * a_inverse) / exc;
			double sinE = (pos[0] * v[0] + pos[1] * v[1] + pos[2] * v[2]) / (exc * Math
					.sqrt(G * (1.0 + mass / Constant.SUN_MASS) / a_inverse));
			double E = Math.atan2(sinE, cosE);

			mean_anomaly = E - exc * Math.sin(E);
			true_anomaly = 2.0 * Math.atan(Math.tan(E * 0.5) * Math.sqrt((1.0 + exc) / (1.0 - exc)));
			mean_motion = Math.sqrt(G * (1.0 + mass / Constant.SUN_MASS) / a) / a;
			perihelion_time = jd - mean_anomaly / mean_motion;
		}
		// Parabolic motion
		if (a_inverse == 0.0)
		{
			true_anomaly = 2.0 * Math.atan((pos[0] * v[0] + pos[1] * v[1] + pos[2] * v[2]) / Math
					.sqrt(2.0 * q * G * (1.0 + mass / Constant.SUN_MASS)));
			double vt = (1.0 / 3.0) * FastMath.pow(Math.tan(true_anomaly * 0.5), 3.0) + Math.tan(true_anomaly * 0.5);
			perihelion_time = jd - Math.sqrt(2.0 * FastMath.pow(q, 3.0) / (G * (1.0 + mass / Constant.SUN_MASS))) * vt;
		}
		// Hyperbolic motion
		if (a_inverse < 0.0)
		{
			double cosH = (1.0 - r * a_inverse) / exc;
			double sinH = (pos[0] * v[0] + pos[1] * v[1] + pos[2] * v[2]) / (exc * Math
					.sqrt(Math.abs(a) * G * (1.0 + mass / Constant.SUN_MASS)));
			double H = Math.atan2(sinH, cosH);

			mean_anomaly = -(H - exc * Math.sinh(H));
			true_anomaly = 2.0 * Math.atan(Math.tanh(H * 0.5) * Math.sqrt((1.0 + exc) / (1.0 - exc)));
			mean_motion = Math.sqrt(G * (1.0 + mass / Constant.SUN_MASS) / Math.abs(a)) / Math.abs(a);
			perihelion_time = jd - (exc * Math.sinh(H) - H) * Math
					.sqrt(FastMath.pow(Math.abs(a), 3.0) / (G * (1.0 + mass / Constant.SUN_MASS)));
		}

		double perih_arg = u - true_anomaly;
		double perih_lon = perih_arg + omega;

		OrbitalElement orbit = new OrbitalElement();
		orbit.argumentOfPerihelion = perih_arg;
		orbit.ascendingNodeLongitude = omega;
		orbit.eccentricity = exc;
		orbit.inclination = inc;
		orbit.meanAnomaly = mean_anomaly;
		orbit.meanLongitude = mean_anomaly + perih_lon;
		orbit.meanMotion = mean_motion;
		orbit.perihelionDistance = q;
		orbit.perihelionLongitude = perih_lon;
		orbit.referenceEquinox = jd;
		orbit.referenceTime = jd;
		if (a_inverse == 0.0)
			orbit.referenceTime = perihelion_time;
		orbit.semimajorAxis = a;

		return orbit;
	}

	/**
	 * Default path to MPC distant bodies file.
	 */
	public static final String PATH_TO_MPC_DISTANT_BODIES_FILE = FileIO.DATA_ORBITAL_ELEMENTS_DIRECTORY + "MPC_distant_bodies.txt";

	/**
	 * Default path to MPC bright bodies file.
	 */
	public static final String PATH_TO_MPC_BRIGHT_ASTEROIDS_FILE = FileIO.DATA_ORBITAL_ELEMENTS_DIRECTORY + "MPC_asteroids_bright.txt";

	/**
	 * Default path to MPC Near Earth Objects file.
	 */
	public static final String PATH_TO_MPC_NEOs_FILE = FileIO.DATA_ORBITAL_ELEMENTS_DIRECTORY + "NEOs.txt";

	/**
	 * Default path to SKYMAP distant bodies file.
	 */
	public static final String PATH_TO_SKYMAP_DISTANT_BODIES_FILE = FileIO.DATA_ORBITAL_ELEMENTS_DIRECTORY + "SKYMAP_distant_bodies.txt";

	/**
	 * Default path to MPC comets file.
	 */
	public static final String PATH_TO_MPC_COMETS_FILE = FileIO.DATA_ORBITAL_ELEMENTS_DIRECTORY + "MPC_comets.txt";

	/**
	 * Default path to SKYMAP comets file.
	 */
	public static final String PATH_TO_SKYMAP_COMETS_FILE = FileIO.DATA_ORBITAL_ELEMENTS_DIRECTORY + "SKYMAP_comets.txt";

	/**
	 * Default path to old (historical) comets file.
	 */
	public static final String PATH_TO_OLD_COMETS_FILE = FileIO.DATA_ORBITAL_ELEMENTS_DIRECTORY + "oldComets.txt";
}
