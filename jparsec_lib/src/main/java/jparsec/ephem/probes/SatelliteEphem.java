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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import jparsec.astronomy.CoordinateSystem;
import jparsec.astronomy.Star;
import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.Functions;
import jparsec.ephem.Nutation;
import jparsec.ephem.Obliquity;
import jparsec.ephem.Precession;
import jparsec.ephem.EphemerisElement.ALGORITHM;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.event.Saros;
import jparsec.ephem.event.SimpleEventElement;
import jparsec.ephem.event.SimpleEventElement.EVENT;
import jparsec.ephem.planets.EphemElement;
import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.math.Constant;
import jparsec.math.FastMath;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.observer.ReferenceEllipsoid.ELLIPSOID;
import jparsec.time.AstroDate;
import jparsec.time.SiderealTime;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.time.TimeScale;
import jparsec.util.DataBase;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;

/**
 * This class implements a very simple theory of the motion of artificial
 * satellites. They are modeled mainly as precessing ellipses. <BR>
 * To obtain ephemeris follow these simple steps: <BR>
 * <pre>
 *
 * // Search an object.
 * String name = &quot;ISS&quot;
 * int index = SatelliteEphem.getArtificialSatelliteTargetIndex(name);
 *
 * // Declare an adequate Ephemeris object with the algorithm field set properly.
 * EphemerisElement eph = new EphemerisElement(TARGET.NOT_A_PLANET, EphemerisElement.COORDINATES_TYPE.APPARENT, EphemerisElement.EQUINOX_OF_DATE,
 * 		EphemerisElement.GEOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU2006, EphemerisElement.FRAME.ICRF);
 * eph.algorithm = EphemerisElement.ALGORITHM.ARTIFICIAL_SATELLITE;
 * eph.targetBody.setIndex(index);
 *
 * // Call ephemeris using also a valid Time and Observer objects.
 * SatelliteEphemElement ephem = SatelliteEphem.satEphemeris(time, observer, eph, true);
 * </pre>
 *
 * Better precision can be achieved using the {@linkplain SDP8_SGP8} class.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 * @see SDP4_SGP4
 * @see SDP8_SGP8
 */
public class SatelliteEphem
{
	// private constructor so that this class cannot be instantiated.
	private SatelliteEphem() {}

	private static ReadFile readFile = null;

	/**
	 * Set this flag to true to use the database of iridium artificial satellites.
	 * Default is false to use 'normal' satellites. In case this flag is modified
	 * after calculating ephemerides for satellites (and they were not loaded
	 * by means of {@linkplain SatelliteEphem#setSatellitesFromExternalFile(String[])}) the method
	 * {@linkplain SatelliteEphem#setSatellitesFromExternalFile(String[])}
	 * should be also called, giving null as argument.
	 */
	public static boolean USE_IRIDIUM_SATELLITES = false;

	/**
	 * This constant holds the maximum value allowed for the panel angle of an
	 * Iridium satellite to consider it can throw a flare. Default value is 5 (degrees),
	 * which corresponds to an apparent magnitude around +2.
	 */
	public static double MAXIMUM_IRIDIUM_ANGLE_FOR_FLARES = 5;

	/**
	 * This constant holds the maximum value allowed for the panel angle of an
	 * Iridium satellite to consider it can throw a lunar flare. Default value is
	 * 0.5 (degrees), which corresponds to an apparent magnitude close to +8
	 * for the full Moon (!).
	 */
	public static double MAXIMUM_IRIDIUM_ANGLE_FOR_LUNAR_FLARES = 0.25;

	/**
	 * Value for an Iridium angle not valid, for instance when computing ephemerides
	 * for non Iridium satellites.
	 */
	public static double IRIDIUM_ANGLE_NOT_APPLICABLE = 100;

	private static String defaultData[] = null, defaultDataIridium[] = null;

	/**
	 * Returns if a set of external elements (from an updated file of elements of 
	 * artificial satellites) is currently being used as the default data for 
	 * the main artificial satellites or the iridium satellites. If true, you can 
	 * reset and use again the internal catalog (possibly outdated) by calling 
	 * {@linkplain #setSatellitesFromExternalFile(String[])} with a null object. If 
	 * false, you can call {@linkplain #setSatellitesFromExternalFile(String[], boolean)}
	 * with the new elements and true as boolean, to set as default new updated elements.
	 * @return True or false.
	 */
	public static boolean usingExternalElementsAsDefaultData() {
		if (USE_IRIDIUM_SATELLITES) {
			return defaultDataIridium != null;
		} else {
			return defaultData != null;
		}
	}

	/**
	 * Sets the list of orbital elements of satellite to that of an
	 * external file.
	 * @param file The read file, in the three-line format
	 * (name and two-line elements) used by NORAD, or null to use the
	 * internal file in orbital_elements.jar.
	 * @param asDefaultData True to set the input array as the default set, 
	 * overriding the file in orbital_elements.jar.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void setSatellitesFromExternalFile(String file[], boolean asDefaultData) throws JPARSECException {
		if (asDefaultData) {
			if (USE_IRIDIUM_SATELLITES) {
				defaultDataIridium = file;
			} else {
				defaultData = file;
			}
		}

		if (file == null) {
			if (readFile != null) readFile.setReadElements(null);
			readFile = null;
			return;
		}

		ReadFile re = new ReadFile();
		re.readFileOfArtificialSatellitesFromExternalFile(file);
		if (re.getReadElements() != null) {
			readFile = re;
		} else {
			throw new JPARSECException("Could not read the provided array of elements.");
		}
	}
	private static void readData() throws JPARSECException {
		if (readFile == null) {
			ReadFile re = new ReadFile();
			if (USE_IRIDIUM_SATELLITES && defaultDataIridium != null) {
				re.readFileOfArtificialSatellitesFromExternalFile(defaultDataIridium);
			} else {
				if (!USE_IRIDIUM_SATELLITES && defaultData != null) {
					re.readFileOfArtificialSatellitesFromExternalFile(defaultData);
				} else {
					re.setPath(PATH_TO_SATELLITES_FILE);
					if (USE_IRIDIUM_SATELLITES) re.setPath(PATH_TO_SATELLITES_IRIDIUM_FILE);
					re.readFileOfArtificialSatellites();
				}
			}
			readFile = re;
		}
	}

	/**
	 * Sets the list of orbital elements of satellite to that of an
	 * external file.
	 * @param file The read file, in the three-line format
	 * (name and two-line elements) used by NORAD, or null to use the
	 * internal file in orbital_elements.jar.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void setSatellitesFromExternalFile(String file[]) throws JPARSECException {
		if (file == null) {
			if (readFile != null) readFile.setReadElements(null);
			readFile = null;
			return;
		}

		readFile = new ReadFile();
		readFile.readFileOfArtificialSatellitesFromExternalFile(file);
	}

	/**
	 * Searchs for a given artificial satellite and returns the index.
	 * @param name Object name.
	 * @return The index, or -1 if no match is found.
	 * @throws JPARSECException If an error occurs.
	 */
	public static int getArtificialSatelliteTargetIndex(String name)
	throws JPARSECException {
		readData();
		int index = readFile.searchByName(name);
		return index;
	}
	/**
	 * Returns the name of a satellite.
	 * @param index Index for the satellite as sorted in the file.
	 * @return The name.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String getArtificialSatelliteName(int index)
	throws JPARSECException {
		readData();
		String name = readFile.getObjectName(index);
		return name;
	}
	/**
	 * Returns the orbital element set for a given artificial satellite.
	 * @param index Index for the satellite.
	 * @return The orbital element set.
	 * @throws JPARSECException if an error occurs.
	 */
	public static SatelliteOrbitalElement getArtificialSatelliteOrbitalElement(int index)
	throws JPARSECException {
		readData();
		SatelliteOrbitalElement sat = readFile.getSatelliteOrbitalElement(index);
		return sat;
	}
	/**
	 * Returns the number of satellites in the file.
	 * @return The number of objects.
	 * @throws JPARSECException If an error occurs.
	 */
	public static int getArtificialSatelliteCount()
	throws JPARSECException {
		readData();
		int n = readFile.getNumberOfObjects();
		return n;
	}

	private static boolean FAST_MODE = false;

	/**
	 * Calculate the ephemeris of a satellite.
	 * <BR>
	 * This method is not designed for precise ephemeris, although is gives
	 * correct positions.
	 * The ephemerisElement object is used when transforming to apparent
	 * coordinates. In any other case output position is the same
	 * (geometric = astrometric).
	 * <BR>
	 * Based on the work by James Miller, PLAN-13 program,
	 * http://www.amsat.org/amsat/articles/g3ruh/111.html.
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param sat Satellite orbital element set.
	 * @param magAndSize True to calculate magnitude and size, which is a
	 * relatively slow operation.
	 * @return Satellite ephem.
	 * @throws JPARSECException If the method fails, for example because of an
	 *         invalid date.
	 */
	private static SatelliteEphemElement calcSatellite(TimeElement time, ObserverElement obs, EphemerisElement eph,
			SatelliteOrbitalElement sat, boolean magAndSize) throws JPARSECException
	{
		double AZI, Hz, Hx, Hy, Ux, Uy, Uz, DNOM, ILL, SEL, SAZ, ELO;
		double SUNx, SUNy, SUNz, C, S, GHAA, CUA, UMD, SATx, SATy, SATz, SSA;
		double SNS, CNS, ANTx, ANTy, ANTz, TAS, earthTraslationRate, sunMeanRA, EQC1, EQC2, EQC3, T, sunMeanAnomaly, MASD;
		double MAS, Rx, Ry, Rz, Ox, Oy, Oz, Vx, Vy, equatorialRadius, HGT;
		double Sx, Sy, Sz, SLON, Ax, Ay, Az, sinCorrectedNodeRA, EL, E, N, Nx, Ny, Nz, R, Ex, Ey;
		double VELx, VELy, CZx, CZy, CZz, CYx, CYy, CYz, CXx, CXy, CXz;
		double cosIncl, cosCorrectedArgPerigee, sinCorrectedArgPerigee, cosCorrectedNodeRA, correctedNodeRA, nodePrecessionRate, KDP, correctedArgPerigee, nodeRA, PC, dragCoeff, meanMotion, firstDerivative, G0, LAT;
		double LON, HT_km, cosLAT, sinLAT, flatenning, D, cosLON, sinLON, incl, argPerigee, meanAnomaly, YT, earthTraslationRate2, DT;
		double DE, year, days, GM, J2, n, a, b, sinIncl, ecc, perigeePrecessionRate, INS, MAS0, TEG, ALON, ALAT;
		double DN, TN, KD, M, DR, RN, revolutionNumber, EA, A, B, U, SLAT, polarRadius, RS;
		String ECL;
		double RR, VOx, VOy, Vz, VELz, W0;
		//double XX, ZZ, Ez, GHAe;
		SLON = SLAT = HGT = ELO = RR = ILL = 0;

		double JD = TimeScale.getJD(time, obs, eph, SCALE.UNIVERSAL_TIME_UTC);
		boolean exactMode = FastMath.EXACT_MODE;
		FastMath.EXACT_MODE = false;
		if (!FAST_MODE) FastMath.EXACT_MODE = true;

		// Fixed (not real) values for the orientation of the satellite antenna
		ALON = 180.0;
		ALAT = 0;
		TN = 0.0;
		DN = JD;

		String NOM = sat.getName();
		year = sat.year;
		days = sat.day;
		incl = sat.inclination;
		nodeRA = sat.ascendingNodeRA;
		ecc = sat.eccentricity;
		meanMotion = sat.meanMotion;
		meanAnomaly = sat.meanAnomaly;
		revolutionNumber = sat.revolutionNumber;
		argPerigee = sat.argumentOfPerigee;
		firstDerivative = sat.firstDerivative;

		LAT = obs.getLatitudeRad();
		LON = obs.getLongitudeRad();
		HT_km = obs.getHeight() / 1000.0;

		cosLAT = FastMath.cos(LAT);
		sinLAT = FastMath.sin(LAT);
		cosLON = FastMath.cos(LON);
		sinLON = FastMath.sin(LON);

		ELLIPSOID ellipsoid = obs.getEllipsoid();
		equatorialRadius = ellipsoid.getEquatorialRadius();
		flatenning = 1.0 / ellipsoid.getInverseOfFlatteningFactor();
		polarRadius = equatorialRadius * (1.0 - flatenning);

		D = FastMath.hypot(equatorialRadius * cosLAT, polarRadius * sinLAT); //Math.sqrt(equatorialRadius * equatorialRadius * cosLAT * cosLAT + polarRadius * polarRadius * sinLAT * sinLAT);
		Rx = equatorialRadius * equatorialRadius / D + HT_km;
		Rz = polarRadius * polarRadius / D + HT_km;

		// Observer's unit vectors UP EAST and NORTH in GEOCENTRIC coords.
		Ux = cosLAT * cosLON;
		Ex = -sinLON;
		Nx = -sinLAT * cosLON;

		Uy = cosLAT * sinLON;
		Ey = cosLON;
		Ny = -sinLAT * sinLON;

		Uz = sinLAT;
		//Ez = 0.0;
		Nz = cosLAT;

		// Observer's XYZ coords at Earth's surface
		Ox = Rx * Ux;
		Oy = Rx * Uy;
		Oz = Rz * Uz;

		YT = Constant.TROPICAL_YEAR; // Tropical year, days
		earthTraslationRate = Constant.TWO_PI / YT; // Earth's traslation rate, rads/whole day
		earthTraslationRate2 = Constant.TWO_PI + earthTraslationRate; // ditto radians/day
		W0 = earthTraslationRate2 / Constant.SECONDS_PER_DAY; // ditto radians/sec

		VOx = -Oy * W0;
		VOy = Ox * W0; // Observer's velocity, GEOCENTRIC coords. (VOz=0)

		// Convert satellite Epoch to Day No. and Fraction of day
		AstroDate astro = new AstroDate((int) year, 1, (int) Math.floor(days));
		DE = astro.jd();
		days = days - Math.floor(days);

		// Average Precession rates
		GM = 3.98600433e14 * 1.0e-9; // Earth's Gravitational constant g' * R' * R' km^3/s^2, DE405
		J2 = 0.00108263; // 2nd Zonal coeff, Earth's Gravity Field
		n = meanMotion / Constant.SECONDS_PER_DAY; // Mean motion rad/s
		a = Math.pow((GM / (n * n)), 1.0 / 3.0); // Semi major axis km
		b = a * Math.sqrt(1.0 - ecc * ecc); // Semi minor axis km. Note 'in astronomy' (comets/asteroids) b=a*(1-e)
		sinIncl = FastMath.sin(incl);
		cosIncl = FastMath.cos(incl);
		PC = equatorialRadius * a / (b * b);
		PC = 1.5 * J2 * PC * PC * meanMotion; // Precession const, rad/Day
		nodePrecessionRate = -PC * cosIncl; // Node precession rate, rad/day
		perigeePrecessionRate = PC * (5.0 * cosIncl * cosIncl - 1.0) / 2.0; // Perigee precession rate, rad/day
		dragCoeff = -2.0 * firstDerivative / (meanMotion * 3.0); // Drag coeff. (Angular momentum rate)/(Ang mom) s^-1

		// Sidereal and Solar data. NEVER needs changing. Valid to year 2000+
		G0 = 98.9821; // GHAA, Year YG, Jan 0.0
		MAS0 = 356.0507; // MA Sun and rate, deg, deg/day
		MASD = 0.98560028; // MA Sun and rate, deg, deg/day
		INS = Constant.DEG_TO_RAD * 23.4393;
		CNS = FastMath.cos(INS);
		SNS = FastMath.sin(INS); // Sun's inclination
		EQC1 = 0.03342;
		EQC2 = 0.00035; // Sun's Equation of centre terms
		EQC3 = 5.0E-6;

		// Bring Sun data to Satellite Epoch
		TEG = (DE - 2451543.5) + days; // Elapsed Time: Epoch - YG

		sunMeanRA = Constant.DEG_TO_RAD * G0 + TEG * earthTraslationRate + Math.PI; // Mean RA Sun at Sat epoch
		sunMeanAnomaly = Constant.DEG_TO_RAD * (MAS0 + MASD * TEG); // Mean MA Sun ..

		// Antenna unit vector in orbit plane coordinates.
		cosLON = FastMath.cos(Constant.DEG_TO_RAD * ALON);
		sinLON = FastMath.sin(Constant.DEG_TO_RAD * ALON);
		cosLAT = FastMath.cos(Constant.DEG_TO_RAD * ALAT);
		sinLAT = FastMath.sin(Constant.DEG_TO_RAD * ALAT);
		Ax = -cosLAT * cosLON;
		Ay = -cosLAT * sinLON;
		Az = -sinLAT;

		// Calculate Satellite Position at DN,TN
		T = (DN - DE) + (TN - days); // Elapsed T since epoch, days

		DT = dragCoeff * T / 2.0;
		KD = 1.0 + 4.0 * DT;
		KDP = 1.0 - 7.0 * DT; // Linear drag terms
		M = meanAnomaly + meanMotion * T * (1.0 - 3.0 * DT); // Mean anomaly at YR,TN
		DR = Math.floor(M / Constant.TWO_PI); // Strip out whole no of revs
		M = M - DR * Constant.TWO_PI; // M now in range 0 - 2pi
		RN = revolutionNumber + DR; // Current Orbit number

		// Solve M = EA - EC*SIN(EA) for EA given M, by Newton's Method
		EA = M; // Initial solution
		D = 1.0;
		do
		{
			C = FastMath.cos(EA);
			S = FastMath.sin(EA);
			DNOM = 1.0 - ecc * C;
			D = (EA - ecc * S - M) / DNOM; // Change to EA for better solution
			EA = EA - D; // by this amount

		} while (Math.abs(D) > 1.0e-10); // Until convergence

		A = a * KD;
		B = b * KD;
		RS = A * DNOM; // Distances

		// Calc satellite position & velocity in plane of ellipse
		Sx = A * (C - ecc);
		Vx = -A * S / DNOM * n;
		Sy = B * S;
		Vy = B * C / DNOM * n;

		correctedArgPerigee = argPerigee + perigeePrecessionRate * T * KDP;
		cosCorrectedArgPerigee = FastMath.cos(correctedArgPerigee);
		sinCorrectedArgPerigee = FastMath.sin(correctedArgPerigee);
		correctedNodeRA = nodeRA + nodePrecessionRate * T * KDP;
		cosCorrectedNodeRA = FastMath.cos(correctedNodeRA);
		sinCorrectedNodeRA = FastMath.sin(correctedNodeRA);

		// Plane -> celestial coordinate transformation, [C] = [RAAN]*[IN]*[AP]
		CXx = cosCorrectedArgPerigee * cosCorrectedNodeRA - sinCorrectedArgPerigee * cosIncl * sinCorrectedNodeRA;
		CXy = -sinCorrectedArgPerigee * cosCorrectedNodeRA - cosCorrectedArgPerigee * cosIncl * sinCorrectedNodeRA;
		CXz = sinIncl * sinCorrectedNodeRA;
		CYx = cosCorrectedArgPerigee * sinCorrectedNodeRA + sinCorrectedArgPerigee * cosIncl * cosCorrectedNodeRA;
		CYy = -sinCorrectedArgPerigee * sinCorrectedNodeRA + cosCorrectedArgPerigee * cosIncl * cosCorrectedNodeRA;
		CYz = -sinIncl * cosCorrectedNodeRA;
		CZx = sinCorrectedArgPerigee * sinIncl;
		CZy = cosCorrectedArgPerigee * sinIncl;
		CZz = cosIncl;

		// Compute SATellite's position vector, ANTenna axis unit vector
		// and VELocity in CELESTIAL coordinates. (Note: Sz=0, Vz=0)
		SATx = Sx * CXx + Sy * CXy;
		ANTx = Ax * CXx + Ay * CXy + Az * CXz;
		VELx = Vx * CXx + Vy * CXy;
		SATy = Sx * CYx + Sy * CYy;
		ANTy = Ax * CYx + Ay * CYy + Az * CYz;
		VELy = Vx * CYx + Vy * CYy;
		SATz = Sx * CZx + Sy * CZy;
		ANTz = Ax * CZx + Ay * CZy + Az * CZz;
		VELz = Vx * CZx + Vy * CZy;

		// Also express SAT,ANT and VEL in GEOCENTRIC coordinates:
		GHAA = jparsec.time.SiderealTime.greenwichMeanSiderealTime(time, obs, eph);
		if (!FAST_MODE) GHAA += jparsec.time.SiderealTime.equationOfEquinoxes(time, obs, eph);

		C = FastMath.cos(GHAA);
		S = -FastMath.sin(GHAA);
		Sx = (SATx * C - SATy * S);
		Ax = ANTx * C - ANTy * S;
		Vx = VELx * C - VELy * S;
		Sy = (SATx * S + SATy * C);
		Ay = ANTx * S + ANTy * C;
		Vy = VELx * S + VELy * C;
		Sz = SATz;
		Az = ANTz;
		Vz = VELz;

		double JD_TDB = 0;
		if (!FAST_MODE) {
			JD_TDB = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
			DataBase.addData("GCRS", Precession.precessToJ2000(JD_TDB, new double[] {Sx / Constant.AU, Sy / Constant.AU, Sz / Constant.AU}, eph), true);
		}

		// Compute and manipulate range/velocity/antenna vectors
		Rx = Sx - Ox;
		Ry = Sy - Oy;
		Rz = Sz - Oz; // Rangevec = Satvec - Obsvec
		R = Math.sqrt(Rx * Rx + Ry * Ry + Rz * Rz); // Range magnitude
		Rx = Rx / R;
		Ry = Ry / R;
		Rz = Rz / R; // Normalise Range vector
		U = Rx * Ux + Ry * Uy + Rz * Uz; // UP Component of unit range
		E = Rx * Ex + Ry * Ey; // EAST do (Ez=0)
		N = Rx * Nx + Ry * Ny + Rz * Nz; // NORTH do
		AZI = FastMath.atan2_accurate(E, N); // Azimuth
		EL = FAST_MODE ? FastMath.asin(U) : Math.asin(U); // Elevation

		// Resolve antenna vector along unit range vector, -r.a = Cos(SQ)
		//sinCorrectedNodeRA = -(Ax * Rx + Ay * Ry + Az * Rz); // Hi-gain ant SQuint
		//sinCorrectedNodeRA = Math.acos(sinCorrectedNodeRA);

		// Calculate sub-satellite Lat/Lon
		SLON = FastMath.atan2_accurate(Sy, Sx); // Lon, + East
		SLAT = FAST_MODE ? FastMath.asin(Sz / RS) : Math.asin(Sz / RS); // Lat, + North
		HGT = RS - equatorialRadius;

		// Resolve Sat-Obs velocity vector along unit range vector. (VOz=0)
		RR = (Vx-VOx)*Rx + (Vy-VOy)*Ry + Vz*Rz; // Range rate, km/s

		MAS = sunMeanAnomaly + Constant.DEG_TO_RAD * MASD * T; // MA of Sun round its orbit
		TAS = sunMeanRA + earthTraslationRate * T + EQC1 * FastMath.sin(MAS) + EQC2 * FastMath.sin(2 * MAS) + EQC3 * FastMath.sin(3 * MAS);

		// Note other programs (XEphem among them) uses the following lines, which seems to be wrong
		// by 0.004 deg around year 2011. Algorithm at Saros class from Calendrical Calculations agree
		// with previous code up to 0.00001 deg.
		//double Tp = (itsEpochJD - 2415020.0) / 36525.0;
	    //double sunMeanAnomaly2 = (358.475845 + 35999.04975 * Tp - 0.00015 * Tp * Tp - 0.00000333333 * Tp * Tp * Tp) * Constant.DEG_TO_RAD;

		C = FastMath.cos(TAS);
		S = FastMath.sin(TAS); // Sin/Cos Sun's true anomaly
		SUNx = C;
		SUNy = S * CNS;
		SUNz = S * SNS; // Sun unit vector - CELESTIAL coords

		// Find Solar angle, elongation, illumination, and eclipse status.
		SSA = -(ANTx * SUNx + ANTy * SUNy + ANTz * SUNz); // Sin of Sun angle -a.h
		// ELO = Math.asin(Rx * SUNx + Ry * SUNy + Rz * SUNz); // Calculated later from az/el
		ILL = Math.sqrt(1.0 - SSA * SSA); // Illumination
		CUA = -(SATx * SUNx + SATy * SUNy + SATz * SUNz) / RS; // Cos of umbral angle -h.s
		UMD = RS * Math.sqrt(1.0 - CUA * CUA) / equatorialRadius; // Umbral dist, Earth radii

		ECL = "Visible at sunset/sunrise";
		if (CUA <= 0.0)
			ECL = "Visible with the sun"; // + for shadow side
		if (UMD <= 1.0 && CUA >= 0.0)
			ECL = "Eclipsed"; // - for sunny side

		// Obtain SUN unit vector in GEOCENTRIC coordinates
		C = FastMath.cos(-GHAA);
		S = FastMath.sin(-GHAA);
		Hx = SUNx * C - SUNy * S;
		Hy = SUNx * S + SUNy * C; // If Sun more than 10 deg below horizon
		Hz = SUNz; // satellite possibly visible

		U = Hx * Ux + Hy * Uy + Hz * Uz;
		E = Hx * Ex + Hy * Ey;
		N = Hx * Nx + Hy * Ny + Hz * Nz;
		SAZ = FastMath.atan2_accurate(E, N); // Azimuth
		SEL = FAST_MODE ? FastMath.asin(U) : Math.asin(U); // Elevation

		if ((SEL * Constant.RAD_TO_DEG < -10.0) && !(ECL.equals("Eclipsed")))
			ECL = "Possibly visible";

		double iridiumAngle = SatelliteEphem.IRIDIUM_ANGLE_NOT_APPLICABLE, iridiumAngleMoon = iridiumAngle;
		if (sat.isIridium()) {
			iridiumAngle = SatelliteEphem.iridiumAngle(new double[] {Sx, Sy, Sz}, new double[] {Vx, Vy, Vz},
					new double[] {Sx - Ox, Sy - Oy, Sz - Oz}, new double[] {Hx, Hy, Hz});

			// Obtain Moon iridium angle
			double jdTT = TimeScale.getJD(time, obs, eph, SCALE.TERRESTRIAL_TIME);
			double pos[] = Saros.getMoonPosition(jdTT);
			if (pos[3] < 29.5306-3.0 && pos[3] > 3.0) {
				double toIlluminatedDiskCenter = 0.25 * Constant.DEG_TO_RAD * (pos[3] - 29.53 * 0.5) / 15.0;
				LocationElement locMoon = CoordinateSystem.eclipticToEquatorial(
						new LocationElement(pos[0] + toIlluminatedDiskCenter, pos[1], 1.0), INS, true);
				double locM[] = locMoon.getRectangularCoordinates();
				double MOONx = locM[0], MOONy = locM[1], MOONz = locM[2];

				double Mx = MOONx * C - MOONy * S;
				double My = MOONx * S + MOONy * C;
				double Mz = MOONz;
				iridiumAngleMoon = SatelliteEphem.iridiumAngle(new double[] {Sx, Sy, Sz}, new double[] {Vx, Vy, Vz},
						new double[] {Sx - Ox, Sy - Oy, Sz - Oz}, new double[] {Mx, My, Mz});
			}
		}

		// Obtain Sun unit vector in EQ coordinates
//		Hx =  SUNx*CXx + SUNy*CYx + SUNz*CZx;
//		Hy =  SUNx*CXy + SUNy*CYy + SUNz*CZy;
//		Hz =  SUNx*CXz + SUNy*CYz + SUNz*CZz;

		boolean isEclipsed = false;
		if (ECL.equals("Eclipsed")) isEclipsed = true;
		if (isEclipsed) ILL = 0;

		FastMath.EXACT_MODE = exactMode;

		ELO = 0;
		if (FAST_MODE) {
			ELO = LocationElement.getApproximateAngularDistance(new LocationElement(SAZ, SEL, 1.0), new LocationElement(AZI, EL, 1.0));
		} else {
			ELO = LocationElement.getAngularDistance(new LocationElement(SAZ, SEL, 1.0), new LocationElement(AZI, EL, 1.0));
		}

		LocationElement loc_horiz = new LocationElement(AZI, EL, R);
		double ast = FAST_MODE ? GHAA + obs.getLongitudeRad() : SiderealTime.apparentSiderealTime(time, obs, eph);
		LocationElement loc_eq = CoordinateSystem.horizontalToEquatorial(loc_horiz, ast, obs.getLatitudeRad(), true);

		if (FAST_MODE) {
			SatelliteEphemElement ephem = new SatelliteEphemElement(sat.getName(), loc_eq.getLongitude(), loc_eq.getLatitude(), R, AZI, EL,
					(float) SLON, (float) SLAT, (float) HGT, (float) RR, (float) ELO, (float) ILL,
					isEclipsed, (int) RN);

			ephem.iridiumAngle = (float) iridiumAngle;
			ephem.iridiumAngleForMoon = (float) iridiumAngleMoon;
			ephem.sunElevation = (float) SEL;
			return ephem;
		}

		// Mean equatorial to true equatorial
		if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT)
		{
			/* Correct nutation */
			double true_eq[] = loc_eq.getRectangularCoordinates();
			true_eq = Nutation.nutateInEquatorialCoordinates(JD_TDB, eph, true_eq, true);
			loc_eq = LocationElement.parseRectangularCoordinates(true_eq);

			/* To apparent elevation */
			loc_horiz = CoordinateSystem.equatorialToHorizontal(loc_eq, ast, obs, eph, true, true);
			//loc_horiz.setLatitude(Ephem.getApparentElevation(eph, obs, loc_horiz.getLatitude(), 5));
		}

		SatelliteEphemElement ephem = new SatelliteEphemElement(NOM, loc_eq.getLongitude(), loc_eq.getLatitude(),
				loc_eq.getRadius(), loc_horiz.getLongitude(), loc_horiz.getLatitude(), (float) SLON, (float) SLAT, (float) HGT, (float) RR, (float) ELO, (float) ILL,
				isEclipsed, (int) RN);

		if (magAndSize) ephem = getMagnitudeAndAngularSize(ephem, sat);
		ephem.iridiumAngle = (float) iridiumAngle;
		ephem.iridiumAngleForMoon = (float) iridiumAngleMoon;
		ephem.sunElevation = (float) SEL;

		// Correct apparent magnitude for extinction
		if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT && eph.correctForExtinction &&
				obs.getMotherBody() == TARGET.EARTH && ephem.magnitude != SatelliteEphemElement.UNKNOWN_MAGNITUDE)
			ephem.magnitude += Star.getExtinction(Constant.PI_OVER_TWO-ephem.elevation, obs.getHeight() / 1000.0, 5);

		return ephem;
	}

	private static String lastSatN = null, lastSatMag = null, lastSatSize = null;
	/**
	 * Gets the apparent magnitude and angular size of a satellite if this
	 * information is known. Information is taken from Mike McCants,
	 * http://www.io.com/~mmccants/index.html, to obtain this data. Apparent
	 * magnitude is not corrected for atmosferic extinction.
	 *
	 * @param sat_ephem Satellite ephem object.
	 * @param sat_orb Satellite orbit object.
	 * @return The same object with missing data completed if this data is
	 *         available.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public static synchronized SatelliteEphemElement getMagnitudeAndAngularSize(SatelliteEphemElement sat_ephem,
			SatelliteOrbitalElement sat_orb) throws JPARSECException
	{
		sat_ephem.magnitude = SatelliteEphemElement.UNKNOWN_MAGNITUDE;
		sat_ephem.angularRadius = SatelliteEphemElement.UNKNOWN_ANGULAR_SIZE;
		
		String satN = ""+sat_orb.satelliteNumber;
		satN = DataSet.repeatString("0", 5-satN.length()) + satN + " ";
		String satMag = null, satSize = null;
		
		if (lastSatN == null || !satN.equals(lastSatN)) {
			String pathToFile = FileIO.DATA_ORBITAL_ELEMENTS_DIRECTORY + "sat_mag.txt";
			try
			{
				InputStream is = SatelliteEphem.class.getClassLoader().getResourceAsStream(pathToFile);
				BufferedReader dis = new BufferedReader(new InputStreamReader(is, ReadFile.ENCODING_ISO_8859));
	
				String line = "";
				while ((line = dis.readLine()) != null)
				{
					if (line.startsWith(satN)) {
						try {
							String mag = line.substring(33, 38).trim();
							satMag = mag;
						} catch (Exception exc) {}
	
						try {
							String end_line = "";
							if (line.length() > 53) {
								end_line = line.substring(38, 53).trim();
							} else if (line.length() > 51) {
								end_line = line.substring(38, 51).trim();
							}
	
							if (!end_line.equals(""))
								satSize = FileIO.getField(1, end_line, " ", true);
						} catch (Exception exc) {}
						break;
					}
				}
	
				// Close file
				dis.close();
				is.close();
	
				lastSatN = satN;
				lastSatMag = satMag;
				lastSatSize = satSize;
			} catch (FileNotFoundException e1) {
				throw new JPARSECException("file not found in path " + pathToFile+".", e1);
			} catch (IOException e2) {
				throw new JPARSECException(
						"error while reading file " + pathToFile + ".", e2);
			}
		} else {
			satN = lastSatN;
			satMag = lastSatMag;
			satSize = lastSatSize;
		}

		if (satMag != null && !satMag.equals("") && !sat_ephem.isEclipsed) {
			sat_ephem.magnitude = (float) DataSet.parseDouble(satMag);
			// Apply McCants's algorithm
			sat_ephem.magnitude = (float) (sat_ephem.magnitude - 15.75 + 2.5 * Math.log10(sat_ephem.distance * sat_ephem.distance / sat_ephem.illumination));
		}
		if (satSize != null) {
			double sat_size = DataSet.parseDouble(satSize);
			if (sat_size > 0.0)
				sat_ephem.angularRadius = (float) (0.5 * Math.atan2(sat_size, sat_ephem.distance * 1000.0));
		}
		
		return sat_ephem;
	}

	/**
	 * Path to the file of satellites, including extension.
	 */
	public static final String PATH_TO_SATELLITES_FILE = FileIO.DATA_ORBITAL_ELEMENTS_DIRECTORY + "ArtificialSatellites.txt";
	/**
	 * Path to the file of iridium satellites, including extension.
	 */
	public static final String PATH_TO_SATELLITES_IRIDIUM_FILE = FileIO.DATA_ORBITAL_ELEMENTS_DIRECTORY + "iridium.txt";

	/**
	 * Calculate the ephemeris of a satellite using the method by James Miller,
	 * PLAN-13 program, http://www.amsat.org/amsat/articles/g3ruh/111.html.
	 * <BR>
	 * The ephemerisElement object is used when transforming to apparent
	 * coordinates. In any other case output position is the same
	 * (geometric = astrometric). Results are referred to mean equinox
	 * of date.
	 * <BR>
	 * A pass is defined as the instant when the satellite is more then 15
	 * degrees above the horizon of the observer. A search for the next pass up
	 * to 7 days after calculation time will be done.
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object. The index of the satellite must be added to the index property.
	 * @param fullEphemeris True for full ephemeris, including next pass time and rise, set, transit.
	 * @return Satellite ephem.
	 * @throws JPARSECException If the method fails, for example because of an
	 *         invalid date.
	 * @deprecated The {@linkplain SDP8_SGP8} class is recommended instead.
	 */
	public static SatelliteEphemElement satEphemeris(TimeElement time, ObserverElement obs, EphemerisElement eph,
			boolean fullEphemeris) throws JPARSECException
	{
		readData();

		// Check Ephemeris object
		if (!EphemerisElement.checkEphemeris(eph))
			throw new JPARSECException("invalid ephemeris object.");

		if (eph.targetBody.getIndex() < 0)
			throw new JPARSECException("invalid target body in ephemeris object.");

		// Obtain object
		SatelliteOrbitalElement sat = (SatelliteOrbitalElement) readFile.getReadElements()[eph.targetBody.getIndex()];

		// Obtain ephemeris
		SatelliteEphemElement ephem = SatelliteEphem.calcSatellite(time, obs, eph, sat, true);

		// Obtain next pass time, when the satellite is at least 15 degrees
		// above horizon
		double min_elevation = 15.0 * Constant.DEG_TO_RAD;
		if (fullEphemeris) {
			ephem.nextPass = getNextPass(time, obs, eph, sat, min_elevation, 7, true);
			if (ephem.nextPass != 0.0)
				ephem = SatelliteEphem.getCurrentOrNextRiseSetTransit(time, obs, eph, ephem, 34.0 * Constant.DEG_TO_RAD / 60.0);
		}

		return ephem;
	}

	// Returns the approximate time in days required for a given satellite to move from
	// one side to the other side of the sky from a given observer.
	private static double getBestQuickSearch(SatelliteOrbitalElement sat, double minElev) {
		double GM = 3.98600433e14 * 1.0e-9; // Earth's Gravitational constant g' * R' * R'
		double n = sat.meanMotion / Constant.SECONDS_PER_DAY; // Mean motion rad/s
		double a = Math.pow((GM / (n * n)), 1.0 / 3.0); // Semi major axis km
		double ecc = sat.eccentricity;
		double b = a * Math.sqrt(1.0 - ecc * ecc); // Semi minor axis km

		double r = (a + b) / 2.0 - Constant.EARTH_RADIUS;
		double ang = Constant.PI_OVER_TWO - 2.0 * minElev;
		double dr = ang * r;
		double drDay = Constant.TWO_PI * (r + Constant.EARTH_RADIUS);
		double dt = dr * Constant.SECONDS_PER_DAY / drDay;
		return dt / Constant.SECONDS_PER_DAY; // days
	}

	/**
	 * Obtain the time of the next pass of the satellite above observer. It can be used
	 * as an starting point prior to obtain rise, set, transit times.
	 * <BR>
	 * A pass is defined as an instant when the elevation of the satellite is
	 * greater than certain minimum value. If the observer has a perfect sight
	 * of the horizon, it is possible to set a value equal to zero, but the
	 * satellite will be probably too faint.
	 * <BR>
	 * The pass is a search iteration with a precision of 1 minute of time. If
	 * the satellite appears too quickly or just above minimum elevation only
	 * for a few seconds, then the search could fail. Another possible cause
	 * of fail is for geostationary satellites.
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param sat Satellite orbital elements.
	 * @param min_elevation Minimum elevation in radians.
	 * @param maxDays Maximum number of days to search for a next pass.
	 * @param current True to return the input time if the satellite is above the minimum
	 * elevation, false to return next pass without considering the actual position of the satellite.
	 * @return Julian day of the next pass in local time, or 0.0 if the satellite
	 * has no next transit. If the day is negative that means that the satellite is
	 * eclipsed during the next pass.
	 * @throws JPARSECException If the method fails, for example because of an
	 *         invalid date.
	 * @deprecated The {@linkplain SDP8_SGP8} class is recommended instead.
	 */
	public static double getNextPass(TimeElement time, ObserverElement obs, EphemerisElement eph,
			SatelliteOrbitalElement sat, double min_elevation, double maxDays, boolean current) throws JPARSECException
	{
		// Check Ephemeris object
		if (!EphemerisElement.checkEphemeris(eph))
			throw new JPARSECException("invalid ephemeris object.");

		if (min_elevation < 0.0 || min_elevation >= Math.PI*0.5) throw new JPARSECException("invalid minimum elevation.");

		// Check is satellite can really be observed
		/*
		double GM = 3.98600433e14 * 1.0e-9; // Earth's Gravitational constant g' * R' * R' km^3/s^2, DE405
		double n = sat.meanMotion / Constant.SECONDS_PER_DAY; // Mean motion rad/s
		double a = Math.pow((GM / (n * n)), 1.0 / 3.0); // Semi major axis km
		double radius = Math.acos(Constant.EARTH_RADIUS / a);
		double minLatitude = sat.inclination + radius + min_elevation;
		if (Math.abs(obs.getLatitudeRad()) > minLatitude) return 0;
		*/

		boolean oldf = FAST_MODE;
		FAST_MODE = true;

		// Obtain ephemeris: elevation, isEclipsed
		SatelliteEphemElement ephem = SatelliteEphem.calcSatellite(time, obs, eph, sat, false);

		// Obtain Julian day in reference scale
		SCALE refScale = SCALE.UNIVERSAL_TIME_UTC;
		double JD = TimeScale.getJD(time, obs, eph, refScale);
		double JD_LT = TimeScale.getJD(time, obs, eph, SCALE.LOCAL_TIME);

		// Set time step to 0.5 minutes
		double time_step = 0.5 / (Constant.MINUTES_PER_HOUR * Constant.HOURS_PER_DAY);
		int nstep = 0;
		int max_step = (int) Math.floor(maxDays / time_step);
		double qs = getBestQuickSearch(sat, min_elevation) / time_step;
		int quickSearch = (int) (0.5 + qs / 2.0);
		if (quickSearch < 1) quickSearch = 1;
		if (quickSearch > 8) quickSearch = 8;

		// Obtain next pass. First we obtain the time when the satellite is
		// below the minimum elevation (necessary if it is currently above). Then, we
		// obtain the next pass
		while (ephem.elevation > min_elevation && nstep < max_step && !current)
		{
			nstep++;
			double new_JD = JD + (double) nstep * time_step;

			TimeElement new_time = new TimeElement(new_JD, refScale);

			ephem = SatelliteEphem.calcSatellite(new_time, obs, eph, sat, false);
		}

		if (nstep >= max_step) {
//			JPARSECException.addWarning("this satellite is permanently above the horizon and the minimum elevation.");
			FAST_MODE = oldf;
			return 0.0;
		}

		while (ephem.elevation < min_elevation && nstep < max_step)
		{
			if (ephem.elevation < -25.0 * Constant.DEG_TO_RAD) {
				nstep = nstep + quickSearch;
			} else {
				if (ephem.elevation < -15.0 * Constant.DEG_TO_RAD) {
					int bqs = quickSearch / 2;
					if (bqs < 1) bqs = 1;
					nstep = nstep + bqs;
				} else {
					int bqs = quickSearch / 4;
					if (bqs < 1 || ephem.elevation > 0) bqs = 1;
					nstep = nstep + bqs;
				}
			}
			double new_JD = JD + (double) nstep * time_step;

			TimeElement new_time = new TimeElement(new_JD, refScale);

			ephem = SatelliteEphem.calcSatellite(new_time, obs, eph, sat, false);
		}

		while (ephem.elevation > min_elevation && nstep < max_step)
		{
			nstep--;
			double new_JD = JD + (double) nstep * time_step;

			TimeElement new_time = new TimeElement(new_JD, refScale);

			ephem = SatelliteEphem.calcSatellite(new_time, obs, eph, sat, false);
		}

		double next_pass = JD_LT + nstep * time_step;

		FAST_MODE = oldf;
		if (next_pass >= JD_LT + maxDays) {
//			JPARSECException.addWarning("could not find next pass time during next "+maxDays+" days.");
			next_pass = 0.0;
		}

		if (ephem.isEclipsed) next_pass = -next_pass;

		return next_pass;
	}

	/**
	 * Obtain all transits of a given satellite on top of the Sun or the Moon.
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param sat Satellite orbital elements.
	 * @param maxDays Maximum number of days to search for a next pass.
	 * @param minDist Minimum distance in degrees to consider that there is a transit
	 * on top of the Sun or Moon. Typical value is 0.25 degrees, but you can set it to a
	 * higher value to return more transits (although you will have to move from the 
	 * observing site set to have a change to see it).
	 * @return An array of event objects with the type of transit (on the Sun or the
	 * Moon) using the secondary object field, and with the initial and ending transit times.
	 * The details field will contain the elevation above the horizon, always &gt;= 0.
	 * Precision is 0.5s.
	 * @throws JPARSECException If the method fails, for example because of an
	 *         invalid date.
	 */
	public static ArrayList<SimpleEventElement> getNextSunOrMoonTransits(TimeElement time, ObserverElement obs, EphemerisElement eph,
			SatelliteOrbitalElement sat, double maxDays, double minDist) throws JPARSECException
	{
		// Check Ephemeris object
		if (!EphemerisElement.checkEphemeris(eph))
			throw new JPARSECException("invalid ephemeris object.");

		double min_elevation = 0;
		boolean current = true;
		ArrayList<SimpleEventElement> out = new ArrayList<SimpleEventElement>();
		double JD = TimeScale.getJD(time, obs, eph, SCALE.UNIVERSAL_TIME_UTC);
		TimeElement new_time = new TimeElement(JD, SCALE.UNIVERSAL_TIME_UTC);
		double jdF = maxDays + JD;
		double time_step = 0.5 / Constant.SECONDS_PER_DAY;

		String eclipsed = Translate.translate(163).toLowerCase();
		while (true) {
			maxDays = jdF - new_time.astroDate.jd();
			double next_pass = getNextPass(new_time, obs, eph, sat, min_elevation, maxDays, current);
			if (next_pass == 0) break;
			current = false;

			// Obtain ephemeris
			FAST_MODE = true;
			SatelliteEphemElement ephem = calcSatellite(time, obs, eph, sat, false);

			// Obtain Julian day in reference scale
			new_time = new TimeElement(Math.abs(next_pass), SCALE.LOCAL_TIME);
			JD = TimeScale.getJD(new_time, obs, eph, SCALE.UNIVERSAL_TIME_UTC);
			double JD_TT = TimeScale.getJD(new_time, obs, eph, SCALE.TERRESTRIAL_TIME);
			new_time = new TimeElement(JD, SCALE.UNIVERSAL_TIME_UTC);

			int nstep = 0;
			boolean insideSun = false, insideMoon = false;
			double obl = Obliquity.meanObliquity(Functions.toCenturies(JD_TT), eph) +
					Nutation.getFastNutation(JD_TT)[1];

			while (ephem.elevation > min_elevation || nstep == 0)
			{
				nstep ++;
				double new_JD = JD + (double) nstep * time_step;

				new_time = new TimeElement(new_JD, SCALE.UNIVERSAL_TIME_UTC);

				ephem = calcSatellite(new_time, obs, eph, sat, false);

				LocationElement loc = CoordinateSystem.equatorialToEcliptic(ephem.getEquatorialLocation(), obl, true);
				double sun[] = Saros.getSunPosition(JD_TT + (double) nstep * time_step);
				double moon[] = Saros.getMoonPosition(JD_TT + (double) nstep * time_step);

				double dSun = LocationElement.getAngularDistance(loc, new LocationElement(sun[0], sun[1], 1.0)) * Constant.RAD_TO_DEG;
				double dMoon = LocationElement.getAngularDistance(loc, new LocationElement(moon[0], moon[1], 1.0)) * Constant.RAD_TO_DEG;

				if (dSun < minDist && !insideSun) {
					String det = Functions.formatAngleAsDegrees(ephem.elevation, 1)+"\u00b0";
					if (ephem.isEclipsed) det += ", "+eclipsed;
					SimpleEventElement see = new SimpleEventElement(JD_TT + (double) nstep * time_step, EVENT.ARTIFICIAL_SATELLITES_TRANSITS_SUN_MOON, det);
					see.body = sat.name;
					see.secondaryBody = TARGET.SUN.getName();
					see.eventLocation = ephem.getEquatorialLocation();
					out.add(see);
					insideSun = true;
				} else {
					if (insideSun && dSun >= minDist) {
						insideSun = false;
						SimpleEventElement see = out.get(out.size()-1);
						if (see.secondaryBody.equals(TARGET.SUN.getName()))
							see.endTime = JD_TT + (nstep - 1.0) * time_step;
					}
				}
				if (dMoon < minDist && !insideMoon) {
					String det = Functions.formatAngleAsDegrees(ephem.elevation, 1)+"\u00b0";
					if (ephem.isEclipsed) det += ", "+eclipsed;
					SimpleEventElement see = new SimpleEventElement(JD_TT + (double) nstep * time_step, EVENT.ARTIFICIAL_SATELLITES_TRANSITS_SUN_MOON, det);
					see.body = sat.name;
					see.secondaryBody = TARGET.Moon.getName();
					see.eventLocation = ephem.getEquatorialLocation();
					out.add(see);
					insideMoon = true;
				} else {
					if (insideMoon && dMoon >= minDist) {
						insideMoon = false;
						SimpleEventElement see = out.get(out.size()-1);
						if (see.secondaryBody.equals(TARGET.Moon.getName()))
							see.endTime = JD_TT + (nstep - 1.0) * time_step;
					}
				}
				double min = Math.min(dMoon, dSun);
				if (min > 5 && !insideMoon && !insideSun) nstep += (int) (min / (time_step * Constant.SECONDS_PER_DAY));
				if ((insideSun || insideMoon) && ephem.elevation <= min_elevation && out.size() > 0) out.remove(out.size()-1);
			}
		}

		return out;
	}
	
	/**
	 * Obtain all transits of a given satellite on top of any planet (Sun or Moon excluded).
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param sat Satellite orbital elements.
	 * @param maxDays Maximum number of days to search for a next pass.
	 * @param minDist Minimum distance in degrees, in addition to the radius of the planet, 
	 * to consider that there is a transit on top of the planet. Strict value should be 0.
	 * @return An array of event objects with the type of transit (target body)
	 * set as the secondary object field, and with the initial and ending transit times.
	 * The details field will contain the elevation above the horizon, always &gt;= 0.
	 * Precision is 0.5s.
	 * @throws JPARSECException If the method fails, for example because of an
	 *         invalid date.
	 */
	public static ArrayList<SimpleEventElement> getNextPlanetTransits(TimeElement time, ObserverElement obs, EphemerisElement eph,
			SatelliteOrbitalElement sat, double maxDays, double minDist) throws JPARSECException
	{
		// Check Ephemeris object
		if (!EphemerisElement.checkEphemeris(eph))
			throw new JPARSECException("invalid ephemeris object.");

		double min_elevation = 0;
		boolean current = true;
		ArrayList<SimpleEventElement> out = new ArrayList<SimpleEventElement>();
		double JD = TimeScale.getJD(time, obs, eph, SCALE.UNIVERSAL_TIME_UTC);
		TimeElement new_time = new TimeElement(JD, SCALE.UNIVERSAL_TIME_UTC);
		double jdF = maxDays + JD;
		double time_step = 0.5 / Constant.SECONDS_PER_DAY;

		String eclipsed = Translate.translate(163).toLowerCase();
		EphemElement pephem[] = new EphemElement[7];
		double pephemDate[] = new double[7];
		TARGET ptar[] = new TARGET[] {TARGET.MERCURY, TARGET.VENUS, TARGET.MARS, 
				TARGET.JUPITER, TARGET.SATURN, TARGET.URANUS, TARGET.NEPTUNE};
		double ptol[] = new double[] {20/Constant.SECONDS_PER_DAY, 60/Constant.SECONDS_PER_DAY, 
				160/Constant.SECONDS_PER_DAY, 900/Constant.SECONDS_PER_DAY, 2500/Constant.SECONDS_PER_DAY, 
				4000/Constant.SECONDS_PER_DAY, 7000/Constant.SECONDS_PER_DAY};
		double dplan[] = new double[7];
		boolean pinside[] = new boolean[7];
		EphemerisElement peph = eph.clone();
		if (peph.algorithm == ALGORITHM.ARTIFICIAL_SATELLITE) peph.algorithm = ALGORITHM.MOSHIER;
		peph.optimizeForSpeed();
		
		while (true) {
			maxDays = jdF - new_time.astroDate.jd();
			double next_pass = getNextPass(new_time, obs, eph, sat, min_elevation, maxDays, current);
			if (next_pass == 0) break;
			current = false;

			// Obtain ephemeris
			FAST_MODE = true;
			SatelliteEphemElement ephem = calcSatellite(time, obs, eph, sat, false);

			// Obtain Julian day in reference scale
			new_time = new TimeElement(Math.abs(next_pass), SCALE.LOCAL_TIME);
			JD = TimeScale.getJD(new_time, obs, eph, SCALE.UNIVERSAL_TIME_UTC);
			double JD_TT = TimeScale.getJD(new_time, obs, eph, SCALE.TERRESTRIAL_TIME);
			new_time = new TimeElement(JD, SCALE.UNIVERSAL_TIME_UTC);

			int nstep = 0;
			boolean insidePlan = false;
			for (int t=0; t<ptar.length; t++) {	pinside[t] = false; }

			while (ephem.elevation > min_elevation || nstep == 0)
			{
				nstep ++;
				double new_JD = JD + (double) nstep * time_step;

				new_time = new TimeElement(new_JD, SCALE.UNIVERSAL_TIME_UTC);

				ephem = calcSatellite(new_time, obs, eph, sat, false);

				LocationElement loc = ephem.getEquatorialLocation();
				for (int t=0; t<ptar.length; t++) {
					if (pephem[t] == null || Math.abs(pephemDate[t]-new_JD) > ptol[t]) {
						peph.targetBody = ptar[t];
						pephem[t] = Ephem.getEphemeris(new_time, obs, peph, false);
						pephemDate[t] = new_JD;
					}
					dplan[t] = LocationElement.getAngularDistance(loc, pephem[t].getEquatorialLocation()) * Constant.RAD_TO_DEG;
					
					double md = pephem[t].angularRadius * Constant.RAD_TO_DEG + minDist;
					if (dplan[t] < md && !pinside[t]) {
						String det = Functions.formatAngleAsDegrees(ephem.elevation, 1)+"\u00b0";
						if (ephem.isEclipsed) det += ", "+eclipsed;
						SimpleEventElement see = new SimpleEventElement(JD_TT + (double) nstep * time_step, 
								EVENT.ARTIFICIAL_SATELLITES_TRANSITS, det);
						see.body = sat.name;
						see.secondaryBody = ptar[t].getName();
						see.eventLocation = ephem.getEquatorialLocation();
						out.add(see);
						pinside[t] = true;
					} else {
						if (pinside[t] && dplan[t] >= md) {
							pinside[t] = false;
							SimpleEventElement see = out.get(out.size()-1);
							if (see.secondaryBody.equals(ptar[t].getName()))
								see.endTime = JD_TT + (nstep - 1.0) * time_step;
						}
					}
				}
				insidePlan = false;
				for (int t=0; t<ptar.length; t++) {	if (pinside[t]) insidePlan = true; }
				double min = DataSet.getMinimumValue(dplan);
				if (min > 5 && !insidePlan) nstep += (int) (min / (time_step * Constant.SECONDS_PER_DAY));
				if (insidePlan && ephem.elevation <= min_elevation && out.size() > 0) out.remove(out.size()-1);
			}
		}

		return out;
	}
	
	/**
	 * Returns the magnitude of an iridium flare based on the panel angle.
	 * The function 2.1013871 * Math.log(angle) - 1.6738664 is used here,
	 * based on a fit to observational data by Randy John, see http://home.comcast.net/~skysat/.
	 * @param ephem The satellite ephemeris.
	 * @param obs The observer. Set to null to avoid correction for atmosferic extinction.
	 * @return The expected magnitude for the flare.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double getIridiumFlareMagnitude(SatelliteEphemElement ephem, ObserverElement obs) throws JPARSECException {
		double mag = 2.1013871 * Math.log(ephem.iridiumAngle) - 1.6738664;
		if (obs != null) mag += Star.getExtinction(Constant.PI_OVER_TWO-ephem.elevation, obs.getHeight() / 1000.0, 5);
		return mag;
	}

	/**
	 * Returns the magnitude of an iridium flare when the reflected light comes from the Moon.
	 * The function 2.1013871 * Math.log(angle) - 1.6738664 is used here, but corrected for
	 * the difference between the apparent solar and lunar magnitudes. Previous formula is
	 * based on a fit to observational data by Randy John, see http://home.comcast.net/~skysat/.
	 * @param time Time object.
	 * @param obs The observer.
	 * @param eph Ephemeris object.
	 * @param ephem The satellite ephemeris.
	 * @return The expected magnitude for the flare. It will be corrected for extinction in case
	 * of choosing apparent coordinates from an Earth site in the ephemeris object (with the
	 * extinction correction flag also enabled).
	 * @throws JPARSECException If an error occurs.
	 */
	public static double getIridiumLunarFlareMagnitude(TimeElement time, ObserverElement obs, EphemerisElement eph,
			SatelliteEphemElement ephem) throws JPARSECException {
		double jd = TimeScale.getJD(time, obs, eph, SCALE.TERRESTRIAL_TIME);
		double moon[] = Saros.getMoonPosition(jd);
		double sun[] = Saros.getSunPosition(jd);
		double rr = moon[2] * Constant.EARTH_RADIUS / Constant.AU;
		double PH = Functions.normalizeRadians(sun[0] - moon[0] + Math.PI) * Constant.RAD_TO_DEG;
		if (PH > 180) PH = 360.0 - PH;
		rr *= sun[2];

		double moonMag = 0.23 + 5.0 * Math.log10(rr) + 0.026 * PH + 4.0E-9 * Math.pow(PH, 4.0);

		double mag = 2.1013871 * Math.log(ephem.iridiumAngleForMoon) - 1.6738664 + (26.74 + moonMag);
		if (obs.getMotherBody() == TARGET.EARTH && eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT && eph.correctForExtinction
				&& ephem.elevation > 0)
			mag += Star.getExtinction(Constant.PI_OVER_TWO-ephem.elevation, obs.getHeight() / 1000.0, 5);
		return mag;
	}

	/**
	 * Obtain the time of the next flares of the satellite above observer. This
	 * method calls {@linkplain #getNextPass(TimeElement, ObserverElement, EphemerisElement, SatelliteOrbitalElement, double, double, boolean)}
	 * and then checks for the flaring conditions. The returned array contains as the
	 * three latest objects the ephemerides for the satellite when the flare starts, ends, and
	 * reaches its maximum. In these objects the apparent magnitude expected for the flare is
	 * set to the magnitude field, and it is corrected for extinction.
	 *
	 * The field {@linkplain #MAXIMUM_IRIDIUM_ANGLE_FOR_FLARES} sets the sensitivty when
	 * searching for more or less bright flares. Note also the method {@linkplain SatelliteOrbitalElement#getStatus}
	 * that returns a flag with the flaring status of the satellite.
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param ephIn Ephemeris object.
	 * @param sat Satellite orbital elements. Should be obviosly an IRIDIUM satellite.
	 * @param min_elevation Minimum elevation of the satellite in radians.
	 * @param maxDays Maximum number of days to search for a next flare.
	 * @param current True to return the input time if the satellite is above the minimum
	 * elevation and flaring, false to return next flare without considering the actual
	 * position of the satellite.
	 * @param precision Precision in the search for events in seconds. The more the value you enter here,
	 * the faster the calculations will be, but some of the events could be skipped. A good value is
	 * 5, which means that flares that last for less than 5s could be missed. This value must be between
	 * 1 and 10. The output precision of the found flares will be always 1s.
	 * @return An array list with all the events for this satellite. The list will be null
	 * if the satellite has no next flare during the number of days given. Otherwise, it will
	 * contains arrays of double values with the Julian day of the beggining of the next flare
	 * in local time, the Julian day of the ending time of the flare, the Julian day of the
	 * maximum of the flare, and the minimum iridium angle as fourth value. The fifth, sixth,
	 * and seventh values will be respectivelly the {@linkplain SatelliteEphemElement} object
	 * for the start, end, and maximum times. No check is done
	 * for flares during day or night, although it is easy to provide a time object for sunset
	 * and a maximum number of days of 0.5 or the required value for sunrise. Precision in
	 * returned times is 1 second, and they consider the minimum elevation so that the start/end
	 * times could reflect the instants when the satellite reaches the minimum elevation (or
	 * is eclipsed) and not the start/end times of the flare.
	 *
	 * @throws JPARSECException If the method fails, for example because of an
	 *         invalid date.
	 * @deprecated The {@linkplain SDP8_SGP8} class is recommended instead.
	 */
	public static ArrayList<Object[]> getNextIridiumFlares(TimeElement time, ObserverElement obs, EphemerisElement ephIn,
			SatelliteOrbitalElement sat, double min_elevation, double maxDays, boolean current, int precision) throws JPARSECException
	{
		if (sat == null || sat.name.toLowerCase().indexOf("dummy")>=0) return null;
		if (precision < 1 || precision > 10) throw new JPARSECException("Precision parameters is "+precision+", which is outside range 1-10.");

		FAST_MODE = true;
		ArrayList<Object[]> events = null;
		SCALE refScale = SCALE.UNIVERSAL_TIME_UTC;
		EphemerisElement eph = ephIn.clone();
		eph.optimizeForSpeed();
		double inputJD = TimeScale.getJD(time, obs, eph, refScale);
		double inputJD_LT = TimeScale.getJD(time, obs, eph, SCALE.LOCAL_TIME);
		double limitJD = inputJD + maxDays;
		double limitJD_LT = inputJD_LT + maxDays;
		double jd = inputJD, jdOut = 0.0;
		while (jd < limitJD && jd != 0.0) {
			TimeElement newTime = new TimeElement(jd, refScale);
			maxDays = limitJD - jd;
			jd = SatelliteEphem.getNextPass(newTime, obs, eph, sat, min_elevation, maxDays, current);
			jd = Math.abs(jd); // <0 => eclipsed, but this limitation should be set at the end only if the sat is eclipsed
			if (jd > 0.0 && jd < limitJD_LT) {
	 			jd = TimeScale.getJD(new TimeElement(Math.abs(jd), SCALE.LOCAL_TIME), obs, eph, refScale);
				current = false;

				jdOut = jd;
				newTime = new TimeElement(jdOut, refScale);
				SatelliteEphemElement ephem = SatelliteEphem.calcSatellite(newTime, obs, eph, sat, false);
				//if (!ephem.isEclipsed) { // this limitation should be set at the end only if the sat is eclipsed
					// Check iridium angle second by second until flare ends, minimum elevation, or eclipse
					double startTime = 0.0, endTime = 0.0, maxTime = 0.0, minimumIA = 0.0;
					SatelliteEphemElement start = null, end = null, max = null;
					boolean above = false;
					if (ephem.iridiumAngle <= MAXIMUM_IRIDIUM_ANGLE_FOR_FLARES)	{
						ephem.magnitude = (float) SatelliteEphem.getIridiumFlareMagnitude(ephem, obs);
						startTime = jdOut;
						minimumIA = ephem.iridiumAngle;
						maxTime = jdOut;
						start = ephem;
						max = ephem;
						end = ephem;
						endTime = jdOut;
					}

					boolean found = false;
					do {
						jdOut += precision / Constant.SECONDS_PER_DAY;
						newTime = new TimeElement(jdOut, refScale);
						ephem = SatelliteEphem.calcSatellite(newTime, obs, eph, sat, false);
						if (ephem.elevation > min_elevation) above = true;
						if (above && ephem.iridiumAngle <= MAXIMUM_IRIDIUM_ANGLE_FOR_FLARES) {
							found = true;
							break;
						}
						if (ephem.elevation < min_elevation && above) break;
					} while (true);

					if (found) {
						jdOut -= precision / Constant.SECONDS_PER_DAY;
						above = false;
						do {
							jdOut += 1.0 / Constant.SECONDS_PER_DAY;
							newTime = new TimeElement(jdOut, refScale);
							// isEclipsed, elevation, iridiumAngle
							ephem = SatelliteEphem.calcSatellite(newTime, obs, eph, sat, false);
							ephem.magnitude = (float) SatelliteEphem.getIridiumFlareMagnitude(ephem, obs);
							if (ephem.elevation > min_elevation) above = true;

							if (ephem.iridiumAngle <= MAXIMUM_IRIDIUM_ANGLE_FOR_FLARES && startTime == 0.0) {
								if (ephem.elevation < min_elevation || ephem.isEclipsed) {
									startTime = 0.0;
									break;
								}
					 			startTime = TimeScale.getJD(newTime, obs, eph, SCALE.LOCAL_TIME);
								start = ephem;
								max = ephem;
								end = ephem;
								minimumIA = ephem.iridiumAngle;
								maxTime = startTime;
								endTime = startTime;
							}
							if ((ephem.iridiumAngle > MAXIMUM_IRIDIUM_ANGLE_FOR_FLARES || ephem.elevation < min_elevation || ephem.isEclipsed) && startTime != 0.0) {
					 			endTime = TimeScale.getJD(newTime, obs, eph, SCALE.LOCAL_TIME);
								end = ephem;
								break;
							}
							if (startTime != 0.0 && ephem.iridiumAngle < minimumIA) {
								minimumIA = ephem.iridiumAngle;
								maxTime = TimeScale.getJD(newTime, obs, eph, SCALE.LOCAL_TIME);
								max = ephem;
							}
							if (ephem.elevation < min_elevation && above) {
								startTime = 0.0;
								break;
							}
						} while (true);
					}

					jd = jdOut;
					if (startTime != 0.0) {
						if (!max.isEclipsed) {
							if (events == null) events = new ArrayList<Object[]>();
							events.add(new Object[] {startTime, endTime, maxTime, minimumIA, start, end, max});
						}
					}
				//}
			}
			jd = Math.abs(jd);
			if (jd != 0.0) jd += 10.0 / 1440.0;
		}
		FAST_MODE = false;
		return events;
	}

	/**
	 * Obtain the time of the next lunar flares of the satellite above observer. This
	 * method calls {@linkplain #getNextPass(TimeElement, ObserverElement, EphemerisElement, SatelliteOrbitalElement, double, double, boolean)}
	 * and then checks for the flaring conditions. The returned array contains as the
	 * three latest objects the ephemerides for the satellite when the lunar flare starts, ends, and
	 * reaches its maximum. In these objects the apparent magnitude expected for the flare is
	 * set to the magnitude field, and it is corrected for extinction in case the ephemeris object
	 * is set to return apparent coordinates from a site on Earth (and the extinction correction
	 * flag is enabled).
	 *
	 * The field {@linkplain #MAXIMUM_IRIDIUM_ANGLE_FOR_LUNAR_FLARES} sets the sensitivty when
	 * searching for more or less bright flares. Note also the method {@linkplain SatelliteOrbitalElement#getStatus}
	 * that returns a flag with the flaring status of the satellite.
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param ephIn Ephemeris object.
	 * @param sat Satellite orbital elements. Should be obviosly an IRIDIUM satellite.
	 * @param min_elevation Minimum elevation of the satellite in radians.
	 * @param maxDays Maximum number of days to search for a next lunar flare.
	 * @param current True to return the input time if the satellite is above the minimum
	 * elevation and flaring, false to return next lunar flare without considering the actual
	 * position of the satellite.
	 * @param precision Precision in the search for events in seconds. The more the value you enter here,
	 * the faster the calculations will be, but some of the events could be skipped. A good value is
	 * 5, which means that flares that last for less than 5s could be missed. This value must be between
	 * 1 and 10. The output precision of the found flares will be always 1s.
	 * @return An array list with all the events for this satellite. The list will be null
	 * if the satellite has no next lunar flare during the number of days given. Otherwise, it will
	 * contains arrays of double values with the Julian day of the beggining of the next flare
	 * in local time, the Julian day of the ending time of the flare, the Julian day of the
	 * maximum of the flare, and the minimum iridium angle as fourth value. The fifth, sixth,
	 * and seventh values will be respectivelly the {@linkplain SatelliteEphemElement} object
	 * for the start, end, and maximum times. No check is done
	 * for flares during day or night, although it is easy to provide a time object for sunset
	 * and a maximum number of days of 0.5 or the required value for sunrise. Precision in
	 * returned times is 1 second, and they consider the minimum elevation so that the start/end
	 * times could reflect the instants when the satellite reaches the minimum elevation (or
	 * is eclipsed) and not the start/end times of the flare.
	 *
	 * @throws JPARSECException If the method fails, for example because of an
	 *         invalid date.
	 * @deprecated The {@linkplain SDP8_SGP8} class is recommended instead.
	 */
	public static ArrayList<Object[]> getNextIridiumLunarFlares(TimeElement time, ObserverElement obs, EphemerisElement ephIn,
			SatelliteOrbitalElement sat, double min_elevation, double maxDays, boolean current, int precision) throws JPARSECException
	{
		if (sat == null || sat.name.toLowerCase().indexOf("dummy")>=0) return null;
		if (precision < 1 || precision > 10) throw new JPARSECException("Precision parameters is "+precision+", which is outside range 1-10.");

		FAST_MODE = true;
		ArrayList<Object[]> events = null;
		SCALE refScale = SCALE.UNIVERSAL_TIME_UTC;
		EphemerisElement eph = ephIn.clone();
		eph.optimizeForSpeed();
		double inputJD = TimeScale.getJD(time, obs, eph, refScale);
		double inputJD_LT = TimeScale.getJD(time, obs, eph, SCALE.LOCAL_TIME);
		double limitJD = inputJD + maxDays;
		double limitJD_LT = inputJD_LT + maxDays;
		double jd = inputJD, jdOut = 0.0;
		while (jd < limitJD && jd != 0.0) {
			TimeElement newTime = new TimeElement(jd, refScale);
			maxDays = limitJD - jd;
			jd = SatelliteEphem.getNextPass(newTime, obs, eph, sat, min_elevation, maxDays, current);
			jd = Math.abs(jd); // <0 => eclipsed, but this limitation should be set at the end only if the sat is eclipsed
			if (jd > 0.0 && jd < limitJD_LT) {
	 			jd = TimeScale.getJD(new TimeElement(Math.abs(jd), SCALE.LOCAL_TIME), obs, eph, refScale);
				current = false;

				jdOut = jd;
				newTime = new TimeElement(jdOut, refScale);
				SatelliteEphemElement ephem = SatelliteEphem.calcSatellite(newTime, obs, eph, sat, false);
				//if (!ephem.isEclipsed) { // this limitation should be set at the end only if the sat is eclipsed
					// Check iridium angle second by second until flare ends, minimum elevation, or eclipse
					double startTime = 0.0, endTime = 0.0, maxTime = 0.0, minimumIA = 0.0;
					SatelliteEphemElement start = null, end = null, max = null;
					boolean above = false;
					if (ephem.iridiumAngleForMoon <= MAXIMUM_IRIDIUM_ANGLE_FOR_LUNAR_FLARES)	{
						ephem.magnitude = (float) SatelliteEphem.getIridiumLunarFlareMagnitude(newTime, obs, eph, ephem);
						startTime = jdOut;
						minimumIA = ephem.iridiumAngleForMoon;
						maxTime = jdOut;
						start = ephem;
						max = ephem;
						end = ephem;
						endTime = jdOut;
					}

					boolean found = false;
					do {
						jdOut += precision / Constant.SECONDS_PER_DAY;
						newTime = new TimeElement(jdOut, refScale);
						ephem = SatelliteEphem.calcSatellite(newTime, obs, eph, sat, false);
						if (ephem.elevation > min_elevation) above = true;
						if (above && ephem.iridiumAngleForMoon <= MAXIMUM_IRIDIUM_ANGLE_FOR_LUNAR_FLARES) {
							found = true;
							break;
						}
						if (ephem.elevation < min_elevation && above) break;
					} while (true);

					if (found) {
						jdOut -= precision / Constant.SECONDS_PER_DAY;
						above = false;
						do {
							jdOut += 1.0 / Constant.SECONDS_PER_DAY;
							newTime = new TimeElement(jdOut, refScale);
							// isEclipsed, elevation, iridiumAngle
							ephem = SatelliteEphem.calcSatellite(newTime, obs, eph, sat, false);
							ephem.magnitude = (float) SatelliteEphem.getIridiumLunarFlareMagnitude(newTime, obs, eph, ephem);
							if (ephem.elevation > min_elevation) above = true;

							if (ephem.iridiumAngleForMoon <= MAXIMUM_IRIDIUM_ANGLE_FOR_LUNAR_FLARES && startTime == 0.0) {
								if (ephem.elevation < min_elevation || ephem.isEclipsed) {
									startTime = 0.0;
									break;
								}
					 			startTime = TimeScale.getJD(newTime, obs, eph, SCALE.LOCAL_TIME);
								start = ephem;
								max = ephem;
								end = ephem;
								minimumIA = ephem.iridiumAngleForMoon;
								maxTime = startTime;
								endTime = startTime;
							}
							if ((ephem.iridiumAngleForMoon > MAXIMUM_IRIDIUM_ANGLE_FOR_LUNAR_FLARES || ephem.elevation < min_elevation || ephem.isEclipsed) && startTime != 0.0) {
					 			endTime = TimeScale.getJD(newTime, obs, eph, SCALE.LOCAL_TIME);
								end = ephem;
								break;
							}
							if (startTime != 0.0 && ephem.iridiumAngleForMoon < minimumIA) {
								minimumIA = ephem.iridiumAngleForMoon;
								maxTime = TimeScale.getJD(newTime, obs, eph, SCALE.LOCAL_TIME);
								max = ephem;
							}
							if (ephem.elevation < min_elevation && above) {
								startTime = 0.0;
								break;
							}
						} while (true);
					}

					jd = jdOut;
					if (startTime != 0.0) {
						if (!max.isEclipsed) {
							if (events == null) events = new ArrayList<Object[]>();
							events.add(new Object[] {startTime, endTime, maxTime, minimumIA, start, end, max});
						}
					}
				//}
			}
			jd = Math.abs(jd);
			if (jd != 0.0) jd += 10.0 / 1440.0;
		}
		FAST_MODE = false;
		return events;
	}

	/**
	 * Calculates current or next rise, set, transit for a satellite. It is recommended that the
	 * input ephem objects corresponds to a time when the satellite is above the horizon,
	 * obtained for the next pass, but it is not required.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object. The index of the satellite must be added to the index property.
	 * @param satEphem Satellite ephem object where the data will be inserted.
	 * @param horizon Refraction at horizon. Standard value is 34 arcminutes.
	 * @return The ephem object with the data included. If the rise, set, transit cannot be
	 * obtained the output will be set to zero, thats means that the results are unknown.
	 * @throws JPARSECException If an error occurs.
	 * @deprecated The {@linkplain SDP8_SGP8} class is recommended instead.
	 */
	public static SatelliteEphemElement getCurrentOrNextRiseSetTransit(TimeElement time, ObserverElement obs, EphemerisElement eph,
			SatelliteEphemElement satEphem, double horizon)
	throws JPARSECException {
		SatelliteEphemElement sat = satEphem.clone();
		TimeElement timeEphem = time.clone();
		int index = 0;
		if (sat.rise == null) {
			sat.rise = new double[] {0.0};
			sat.set = new double[] {0.0};
			sat.transit = new double[] {0.0};
			sat.transitElevation = new float[] {0.0f};
		} else {
			index = sat.rise.length;
			sat.rise = DataSet.addDoubleArray(sat.rise, new double[] {0.0});
			sat.set = DataSet.addDoubleArray(sat.set, new double[] {0.0});
			sat.transit = DataSet.addDoubleArray(sat.transit, new double[] {0.0});
			sat.transitElevation = DataSet.addFloatArray(sat.transitElevation, new float[] {0.0f});
		}
		SatelliteEphemElement satOut = sat.clone();

		// Obtain next pass time, when the satellite is at least 15 degrees
		// above horizon
		if (sat.nextPass == 0.0) {
			readData();

			// Check Ephemeris object
			if (!EphemerisElement.checkEphemeris(eph))
				throw new JPARSECException("invalid ephemeris object.");

			if (eph.targetBody.getIndex() < 0)
				throw new JPARSECException("invalid target body in ephemeris object.");

			// Obtain object
			SatelliteOrbitalElement satOrb = (SatelliteOrbitalElement) readFile.getReadElements()[eph.targetBody.getIndex()];
			double min_elevation = 15.0 * Constant.DEG_TO_RAD;
			sat.nextPass = getNextPass(timeEphem, obs, eph, satOrb, min_elevation, 7, true);
		}

		if (sat.elevation < 0.0) {
			if (sat.nextPass == 0.0) {
				//throw new JPARSECException("satellite below horizon and no next pass could be obtained.");
				return sat;
			}
			timeEphem = new TimeElement(Math.abs(sat.nextPass), SCALE.LOCAL_TIME);
			sat = SatelliteEphem.satEphemeris(timeEphem, obs, eph, false);
		}
		double jdref = TimeScale.getJD(timeEphem, obs, eph, SCALE.LOCAL_TIME);
		double jdMax = jdref, maxElev = sat.elevation;
		double precission = 1.0;
		double jd = jdref;
		int maxIter = 5000;
		int iter = 0;
		do {
			iter++;
			jd = jd - precission / Constant.SECONDS_PER_DAY;
			timeEphem = new TimeElement(jd, SCALE.LOCAL_TIME);
			sat = SatelliteEphem.satEphemeris(timeEphem, obs, eph, false);
			if (sat.elevation > maxElev) {
				maxElev = sat.elevation;
				jdMax = jd;
			}
		} while (sat.elevation > -horizon && iter < maxIter);
		double rise = jd;
		if (iter == maxIter) rise = 0.0;
		iter = 0;

		jd = jdref;
		do {
			iter ++;
			jd = jd + precission / Constant.SECONDS_PER_DAY;
			timeEphem = new TimeElement(jd, SCALE.LOCAL_TIME);
			sat = SatelliteEphem.satEphemeris(timeEphem, obs, eph, false);
			if (sat.elevation > maxElev) {
				maxElev = sat.elevation;
				jdMax = jd;
			}
		} while (sat.elevation > -horizon && iter < maxIter);
		double set = jd;
		double tra = jdMax;
		float traE = (float) maxElev;
		if (iter == maxIter) {
			set = 0.0;
			tra = 0.0;
			traE = 0.0f;
		}
		satOut.rise[index] = rise;
		satOut.set[index] = set;
		satOut.transit[index] = tra;
		satOut.transitElevation[index] = traE;
		return satOut;
	}

	/**
	 * Returns the smallest iridium angle. A given Iridium satellite will
	 * be flaring if it is not eclipsed, above the horizon, and this
	 * value is lower enough.  An empirical relationship between this angle
	 * and the brightness of the reflection has been determined (Randy John,
	 * 2002, SKYSAT v0.64, see http://home.comcast.net/~skysat). 2 deg
	 * corresponds to about 0 mag, 0.5&deg; to -3 mag. The brightest flares are
	 * -8 or -9 mag (visible during day), and can last from 10 to 30s. This
	 * code comes from Horst Meyerdierks (Sputnik library).
	 * @param itsR The geocentric position (x, y, z) of the satellite in arbitrary units.
	 * @param itsV The geocentric velocity vector of the satellite in arbitrary units.
	 * @param t2 The topocentric position of the satellite in arbitrary units.
	 * @param t1 The vector of the object to be reflected.  Usually the Sun,
	 *   but if you want to use the Moon be sure to use the position as seen by
	 *   the satellite.
	 * @return The smallest iridium angle in degrees.
	 */
	public static double iridiumAngle(double itsR[], double itsV[],
			double t2[], double t1[]) {

		/* Forward reflector. */
		double t = SAT_REFLECTION(itsR[0], itsR[1], itsR[2],
	          itsV[0], itsV[1], itsV[2],
		  t2[0], t2[1], t2[2], t1[0], t1[1], t1[2],
		  -40.0*Constant.DEG_TO_RAD, 0.) * Constant.RAD_TO_DEG;
		double tleft = t + 3, tright = tleft;

		/* Left rear reflector. */
		if (t > 2) {
			tleft = SAT_REFLECTION(itsR[0], itsR[1], itsR[2],
	            itsV[0], itsV[1], itsV[2],
	            t2[0], t2[1], t2[2], t1[0], t1[1], t1[2],
	            -40.0*Constant.DEG_TO_RAD, 120.0*Constant.DEG_TO_RAD) * Constant.RAD_TO_DEG;
		    t = Math.min(t, tleft);
		}
	    /* Right rear reflector. */
		if (t > 2 && tleft > 2) {
			tright = SAT_REFLECTION(itsR[0], itsR[1], itsR[2],
              itsV[0], itsV[1], itsV[2],
              t2[0], t2[1], t2[2], t1[0], t1[1], t1[2],
              -40.0*Constant.DEG_TO_RAD, 240.0*Constant.DEG_TO_RAD) * Constant.RAD_TO_DEG;
		    t = Math.min(t, tright);
		}

		/* Return minimum angle */
	    return t;
	}

	/**
	 * Returns the smallest iridium angle. A given Iridium satellite will
	 * be flaring if it is not eclipsed, above the horizon, and this
	 * value is lower enough.  An empirical relationship between this angle
	 * and the brightness of the reflection has been determined (Randy John,
	 * 2002, SKYSAT v0.64, see http://home.comcast.net/~skysat). 2 deg
	 * corresponds to about 0 mag, 0.5&deg; to -3 mag. The brightest flares are
	 * -8 or -9 mag (visible during day), and can last from 10 to 30s. This
	 * code comes from Horst Meyerdierks (Sputnik library).
	 * <BR>
	 * This method also returns the reflector that produces the minimum angle.
	 * It is not used inside JPARSEC.
	 * @param itsR The geocentric position (x, y, z) of the satellite in arbitrary units.
	 * @param itsV The geocentric velocity vector of the satellite in arbitrary units.
	 * @param t2 The topocentric position of the satellite in arbitrary units.
	 * @param t1 The vector of the object to be reflected.  Usually the Sun,
	 *   but if you want to use the Moon be sure to use the position as seen by
	 *   the satellite.
	 * @return An array containing in the first index the smallest iridium angle in degrees,
	 * and in the second the reflector that produced the minimum angle (0 = forward
	 * reflector, -1 = left reflector, 1 = right reflector).
	 */
	public static double[] iridiumAngleAndReflector(double itsR[], double itsV[],
			double t2[], double t1[]) {
		/* Forward reflector. */
		double t = SAT_REFLECTION(itsR[0], itsR[1], itsR[2],
	          itsV[0], itsV[1], itsV[2],
		  t2[0], t2[1], t2[2], t1[0], t1[1], t1[2],
		  -40.0*Constant.DEG_TO_RAD, 0.) * Constant.RAD_TO_DEG;
		double tleft = t + 3, tright = tleft, out = 0;

		/* Left rear reflector. */
		if (t > 2) {
			tleft = SAT_REFLECTION(itsR[0], itsR[1], itsR[2],
	            itsV[0], itsV[1], itsV[2],
	            t2[0], t2[1], t2[2], t1[0], t1[1], t1[2],
	            -40.0*Constant.DEG_TO_RAD, 120.0*Constant.DEG_TO_RAD) * Constant.RAD_TO_DEG;
			if (tleft < t) {
				t = tleft;
				out = -1;
			}
		}
	    /* Right rear reflector. */
		if (t > 2 && tleft > 2) {
			tright = SAT_REFLECTION(itsR[0], itsR[1], itsR[2],
              itsV[0], itsV[1], itsV[2],
              t2[0], t2[1], t2[2], t1[0], t1[1], t1[2],
              -40.0*Constant.DEG_TO_RAD, 240.0*Constant.DEG_TO_RAD) * Constant.RAD_TO_DEG;
			if (tright < t) {
				t = tright;
				out = 1;
			}
		}

		/* Return minimum angle */
	    return new double[] {t, out};
	}

	  /**
	   * Calculate where the reflection of the observer's gaze would point to.
	   * Code from Horst Meyerdierks (Sputnik library).
	   *
	   * <BR>Assume that a mirror is attached to the satellite and is pointing
	   * into the direction of motion.  It is now swiveled in the
	   * orbit plane (like an altitude axis, towards earth is negative) and
	   * then again about the (almost) earth-sat direction (like an azimuth
	   * axis, a positive value would point the mirror West if the sat was
	   * moving North).  By "almost" I [Randy John] really mean about the in-plane
	   * vector normal to motion).
	   *
	   * <BR>This routine is ported from Pascal code from Randy John's SKYSAT
	   * programme
	   * (Randy John, 2002, SKYSAT v0.64, <a href="http://home.comcast.net/~skysat/">http://home.comcast.net/~skysat/</a>).
	   * The interface is different here in that the angles must be given in radian
	   * rather than degrees and that the Sun has to be given as vector rather than
	   * right ascension and declination.
	   *
	   * <BR>The routine is used for Iridium satellites.  A flash occurs when
	   * the returned angle is small.  From John's plot such a satellite is
	   * brighter than -3&nbsp;mag (0&nbsp;mag) when the angle is smaller than
	   * 0.5&deg; (2&deg;).  The brightest flashes are -8 or -9&nbsp;mag.
	   *
	   * <BR>The Iridium satellite main body is a long triangular prism that is kept
	   * vertical (in fact perpendicular to the orbital velocity).  Each of the
	   * three sides has a large flat'ish antenna pointing 40&deg; down from the
	   * "horizontal".  One of these is pointing forward, the other two
	   * are looking between sideways and backward.  While the satellite is sunlit
	   * any two of the three antennae reflect sunlight, often to places on the
	   * Earth; these places can observe an Iridium flash.  This routine calculates
	   * how far from the centre of the flash a given observer is in terms of the
	   * angle seen from the satellite.
	   *
	   * @param EQ_X
	   * @param EQ_Y
	   * @param EQ_Z
	   *   The geocentric position of the satellite in arbitrary units.
	   * @param V_X
	   * @param V_Y
	   * @param V_Z
	   *   The velocity vector of the satellite in arbitrary units.
	   * @param SAT_X_TOPO_EQ
	   * @param SAT_Y_TOPO_EQ
	   * @param SAT_Z_TOPO_EQ
	   *   The topocentric position of the satellite in arbitrary units.
	   * @param SUN_X
	   * @param SUN_Y
	   * @param SUN_Z
	   *   The vector of the object to be reflected.  Usually the Sun,
	   *   but if you want to use the Moon be sure to use the position as seen by
	   *   the satellite.
	   * @param ROT_1
	   *   The first mirror rotation angle in rad.  The first angle says how much
	   *   the mirror is tilted upwards (away from the Earth).  Use -40&deg; for
	   *   Iridium satellites.
	   * @param ROT_2
	   *   The second mirror rotation angle in rad.  The second angle says how much
	   *   the mirror is rotated away from the forward direction.  Use 0&deg;,
	   *   120&deg; and -120&deg; for Iridium satellites.  The three mirrors of
	   *   these satellites require three calls to this routine.
	   */
	  private static double SAT_REFLECTION(
	    double EQ_X, double EQ_Y, double EQ_Z,
	    double V_X, double V_Y, double V_Z,
	    double SAT_X_TOPO_EQ, double SAT_Y_TOPO_EQ, double SAT_Z_TOPO_EQ,
	    double SUN_X, double SUN_Y, double SUN_Z,
	    double ROT_1, double ROT_2)
	  {
	    double SIN_ROT_1, COS_ROT_1, SIN_ROT_2, COS_ROT_2;
	    double RR[] = new double[9];
	    double XX[] = new double[3];
	    double YY[] = new double[3];
	    double ZZ[] = new double[3];
	    double TT[] = new double[3];
	    double NTT[] = new double[3];
	    double SUN_REF_X, SUN_REF_Y, SUN_REF_Z;
	    double TEMP;
	    double t1[] = new double[3];
	    double t2[] = new double[3];

	    /*
	     * Step 1 - Create a new coordinate system oriented to the satellite's
	     *          local situation.  The vectors for x, y and z in the
	     *          equatorial system are :
	     *             x is the velocity vector
	     *             y is the position-vector cross x
	     *             z is x cross y
	     *          Normalize vectors.
	     *          Create the transform matrix RR using direction cosines.
	     *          (see Fund. of Astrod., p. 82).
	     */

	    XX[0] = V_X; XX[1] = V_Y; XX[2] = V_Z;
	    TEMP = Math.sqrt(XX[0]*XX[0] + XX[1]*XX[1] + XX[2]*XX[2]);
	    XX[0] = V_X / TEMP; XX[1] = V_Y / TEMP; XX[2] = V_Z / TEMP;

	    YY[0] = EQ_Y * V_Z - EQ_Z * V_Y;
	    YY[1] = EQ_Z * V_X - EQ_X * V_Z;
	    YY[2] = EQ_X * V_Y - EQ_Y * V_X;
	    TEMP = Math.sqrt(YY[0]*YY[0] + YY[1]*YY[1] + YY[2]*YY[2]);
	    YY[0] = YY[0] / TEMP; YY[1] = YY[1] / TEMP; YY[2] = YY[2] / TEMP;

	    ZZ[0] = XX[1] * YY[2] - XX[2] * YY[1];
	    ZZ[1] = XX[2] * YY[0] - XX[0] * YY[2];
	    ZZ[2] = XX[0] * YY[1] - XX[1] * YY[0];
	    TEMP = Math.sqrt(ZZ[0]*ZZ[0] + ZZ[1]*ZZ[1] + ZZ[2]*ZZ[2]);
	    ZZ[0] = ZZ[0] / TEMP; ZZ[1] = ZZ[1] / TEMP; ZZ[2] = ZZ[2] / TEMP;

	    /*
	     * The xx, yy, zz vectors are mutually perpendicular and define the
	     * coordinate system of the sat.
	     *   xx is forward (velocity vector)
	     *   yy is left (normal to the orbit plane)
	     *   zz is up (well, not up, but perpendicular to the other two;
	     *     almost 'up')
	     * Everything that we know about the MMA's is given with respect to this
	     * coordinate system. Since the vectors are equatorial, we now know the
	     * orientation of the sat in equatorial coordinates. The rr matrix (below)
	     * can transform a vector in sat coordinates into equatorial coordinates.
	     * It should be able to transform (1, 0, 0) back into your original
	     * velocity vector (but normalized). I hope I got all that right.
	     */

	    RR[0] = XX[0]; RR[1] = YY[0]; RR[2] = ZZ[0];
	    RR[3] = XX[1]; RR[4] = YY[1]; RR[5] = ZZ[1];
	    RR[6] = XX[2]; RR[7] = YY[2]; RR[8] = ZZ[2];

	    /*
	     * Step 2 - Initialize an x unit vector to (1, 0, 0).
	     *          Rotate it about the y-axis the desired amount.
	     *          Rotate it about the z-axis the desired amount.
	     *          Transform it to equatorial using RR and call it XX.
	     *          Repeat for y (0, 1, 0) and z (0, 0, 1).
	     *          These vectors define the coordinate system for the mirror.
	     *          The XX vector is normal to the surface of the mirror.
	     *          Recreate the transform matrix RR.
	     */

	    SIN_ROT_1 = Math.sin(ROT_1); /* Rotate the y-axis. */
	    COS_ROT_1 = Math.cos(ROT_1);
	    SIN_ROT_2 = Math.sin(ROT_2); /* Rotate the z-axis. */
	    COS_ROT_2 = Math.cos(ROT_2);

	    SIN_ROT_1 = Math.sin(ROT_1); /* Rotate the y-axis. */
	    COS_ROT_1 = Math.cos(ROT_1);
	    SIN_ROT_2 = Math.sin(ROT_2); /* Rotate the z-axis. */
	    COS_ROT_2 = Math.cos(ROT_2);

	    TT[0] = 1.; TT[1] = 0.; TT[2] = 0.;
	    NTT[0] = TT[0] * COS_ROT_1 -  TT[2]  * SIN_ROT_1;
	    NTT[1] = TT[1];
	    NTT[2] = TT[0] * SIN_ROT_1 +  TT[2]  * COS_ROT_1;
	    TT[0] =  NTT[0] * COS_ROT_2 +  NTT[1] * SIN_ROT_2;
	    TT[1] = -NTT[0] * SIN_ROT_2 +  NTT[1] * COS_ROT_2;
	    TT[2] =  NTT[2];
	    XX[0] = TT[0] * RR[0] + TT[1] * RR[1] + TT[2] * RR[2];
	    XX[1] = TT[0] * RR[3] + TT[1] * RR[4] + TT[2] * RR[5];
	    XX[2] = TT[0] * RR[6] + TT[1] * RR[7] + TT[2] * RR[8];

	    TT[0] = 0.; TT[1] = 1.; TT[2] = 0.;
	    NTT[0] = TT[0] * COS_ROT_1 -  TT[2]  * SIN_ROT_1;
	    NTT[1] = TT[1];
	    NTT[2] = TT[0] * SIN_ROT_1 +  TT[2]  * COS_ROT_1;
	    TT[0] =  NTT[0] * COS_ROT_2 +  NTT[1] * SIN_ROT_2;
	    TT[1] = -NTT[0] * SIN_ROT_2 +  NTT[1] * COS_ROT_2;
	    TT[2] =  NTT[2];
	    YY[0] = TT[0] * RR[0] + TT[1] * RR[1] + TT[2] * RR[2];
	    YY[1] = TT[0] * RR[3] + TT[1] * RR[4] + TT[2] * RR[5];
	    YY[2] = TT[0] * RR[6] + TT[1] * RR[7] + TT[2] * RR[8];

	    TT[0] = 0.; TT[1] = 0.; TT[2] = 1.;
	    NTT[0] = TT[0] * COS_ROT_1 -  TT[2]  * SIN_ROT_1;
	    NTT[1] = TT[1];
	    NTT[2] = TT[0] * SIN_ROT_1 +  TT[2]  * COS_ROT_1;
	    TT[0] =  NTT[0] * COS_ROT_2 +  NTT[1] * SIN_ROT_2;
	    TT[1] = -NTT[0] * SIN_ROT_2 +  NTT[1] * COS_ROT_2;
	    TT[2] =  NTT[2];
	    ZZ[0] = TT[0] * RR[0] + TT[1] * RR[1] + TT[2] * RR[2];
	    ZZ[1] = TT[0] * RR[3] + TT[1] * RR[4] + TT[2] * RR[5];
	    ZZ[2] = TT[0] * RR[6] + TT[1] * RR[7] + TT[2] * RR[8];

	    RR[0] = XX[0]; RR[1] = YY[0]; RR[2] = ZZ[0];
	    RR[3] = XX[1]; RR[4] = YY[1]; RR[5] = ZZ[1];
	    RR[6] = XX[2]; RR[7] = YY[2]; RR[8] = ZZ[2];

	    /*
	     * Step 3 - Convert the observer-sat vector to the mirror coord system.
	     *          Negate the x coordinate (the reflection).
	     *          Convert it back to the equatorial system.
	     *          Convert to RA and Dec and find the angle between that
	     *          position and the sun's.
	     *          Make sure that the sun is in front of the mirror.
	     */

	    TT[0] = SAT_X_TOPO_EQ * RR[0] + SAT_Y_TOPO_EQ * RR[3]
		  + SAT_Z_TOPO_EQ * RR[6];
	    TT[1] = SAT_X_TOPO_EQ * RR[1] + SAT_Y_TOPO_EQ * RR[4]
		  + SAT_Z_TOPO_EQ * RR[7];
	    TT[2] = SAT_X_TOPO_EQ * RR[2] + SAT_Y_TOPO_EQ * RR[5]
		  + SAT_Z_TOPO_EQ * RR[8];

	    TT[0] = -TT[0];

	    if (TT[0] >= 0.) {
	      SUN_REF_X = TT[0] * RR[0] + TT[1] * RR[1] + TT[2] * RR[2];
	      SUN_REF_Y = TT[0] * RR[3] + TT[1] * RR[4] + TT[2] * RR[5];
	      SUN_REF_Z = TT[0] * RR[6] + TT[1] * RR[7] + TT[2] * RR[8];

	      t1[0] = SUN_X;     t1[1] = SUN_Y;     t1[2] = SUN_Z;
	      t2[0] = SUN_REF_X; t2[1] = SUN_REF_Y; t2[2] = SUN_REF_Z;
	      LocationElement locS = LocationElement.parseRectangularCoordinatesFast(t1);
	      LocationElement locR = LocationElement.parseRectangularCoordinatesFast(t2);
	      return LocationElement.getAngularDistance(locS, locR);
	    }
	    return Math.PI;
	  }
}
