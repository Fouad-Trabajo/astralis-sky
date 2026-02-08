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
package jparsec.ephem.moons;

import java.util.ArrayList;

import jparsec.astronomy.CoordinateSystem;
import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.EphemerisElement.COORDINATES_TYPE;
import jparsec.ephem.Functions;
import jparsec.ephem.PhysicalParameters;
import jparsec.ephem.Precession;
import jparsec.ephem.Target;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.event.MoonEvent;
import jparsec.ephem.event.MoonEvent.EVENT_DEFINITION;
import jparsec.ephem.event.Saros;
import jparsec.ephem.moons.MoonOrbitalElement.REFERENCE_PLANE;
import jparsec.ephem.planets.EphemElement;
import jparsec.ephem.planets.JPLEphemeris;
import jparsec.ephem.planets.PlanetEphem;
import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.math.Constant;
import jparsec.math.FastMath;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.AstroDate;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.time.TimeScale;
import jparsec.util.Configuration;
import jparsec.util.DataBase;
import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Translate;
import jparsec.util.Logger.LEVEL;

/**
 * A class to obtain accurate ephemerides of natural satellites. There are two methods
 * to obtain ephemerides: using a given theory for the main satellites of a given planet
 * (L1, TASS, GUST86, ..) or using elliptic elements from JPL for dwarf satellites (or
 * sometimes also the main ones). There are methods to obtain the ephemerides for all
 * satellites orbiting a planet by combining the two methods. JPL elliptic elements
 * usually provide ephemerides within the arcsecond level compared to the main theories.
 *
 * @see TimeElement
 * @see ObserverElement
 * @see EphemerisElement
 * @see MoonEphemElement
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class MoonEphem
{
	// private constructor so that this class cannot be instantiated.
	private MoonEphem() {}
	
	/**
	 * Corrects the equatorial coordinates of Pluto from the barycenter of the Pluto system to the
	 * center of Pluto's body. Internally this method calculates the position of Charon respect the
	 * barycenter to derive the actual approximate location of Pluto. Error in around a few hundred
	 * km or lower. <P>
	 * This method is internally used by JPARSEC and the user has no need to call it.
	 * @param pos Equatorial rectangular position (x, y, z and optionally velocities) of Pluto-Charon 
	 * barycenter, mean equinox and equator J2000.
	 * @param jd Julian day, TDB.
	 * @param method Ephemeris reduction method in case precession is required.
	 * @param checkOffset JPARSEC uses a variable in the database to know if the ephemerides should be
	 * calculated for a given offset respect the barycenter of the body. In case the current ephemerides
	 * uses an offset, then the ephemerides will be surely for Charon, and not for Pluto itself. Setting
	 * this to true will check this and will return the input position (barycenter) in case of there's
	 * an offset set, and will correct for Pluto's body center if an offset is not found. True is used
	 * always internally in JPARSEC.
	 * @return The input position corrected for the true position of Pluto.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double[] fromPlutoBarycenterToPlutoCenter(double pos[], double jd, EphemerisElement.REDUCTION_METHOD method,
			boolean checkOffset) throws JPARSECException {
		if (checkOffset) {
			Object o = DataBase.getData("offsetPosition", true);
			if (o != null) {
				double p[] = (double[]) o;
				// Return the barycenter position in case the ephemerides are for Charon
				if (p[0] != 0.0 || p[1] != 0.0 || p[2] != 0.0) return pos;
			}
		}
		MoonOrbitalElement orbit[] = MoonEphem.getMoonElements("Charon", jd);
		if (orbit == null || orbit.length == 0) return pos;
		double dp[] = MoonEphem.rocks(jd, orbit[0], method);
		
		// Try to derive approximate velocity
		double dt = 0.1;
		double dpp[] = MoonEphem.rocks(jd+dt, orbit[0], method);
		dp = new double[] {dp[0], dp[1], dp[2], (dpp[0]-dp[0])/dt, (dpp[1]-dp[1])/dt, (dpp[2]-dp[2])/dt};
		
		dp = Functions.scalarProduct(dp, -0.1218);
		return Functions.sumVectors(pos, dp);
	}
	
	/**
	 * Corrects the equatorial coordinates of Pluto from the body center to the Pluto-Charon barycenter.
	 * Internally this method calculates the position of Charon respect the barycenter to derive the 
	 * actual approximate location of Pluto. Error in around a few hundred km or lower. <P>
	 * This method is internally used by JPARSEC and the user has no need to call it.
	 * @param pos Equatorial rectangular position (x, y, z and optionally velocities) of Pluto, mean 
	 * equinox and equator J2000.
	 * @param jd Julian day, TDB.
	 * @param method Ephemeris reduction method in case precession is required.
	 * @param checkOffset JPARSEC uses a variable in the database to know if the ephemerides should be
	 * calculated for a given offset respect the barycenter of the body. In case the current ephemerides
	 * uses an offset, then the ephemerides will be surely for Charon, and not for Pluto itself. Setting
	 * this to true will check this and will return the input position (barycenter) in case of there's
	 * an offset set, and will correct for Pluto's body center if an offset is not found. True is used
	 * always internally in JPARSEC.
	 * @return The input position corrected for the position of Pluto-Charon system.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double[] fromPlutoCenterToPlutoBarycenter(double pos[], double jd, EphemerisElement.REDUCTION_METHOD method,
			boolean checkOffset) throws JPARSECException {
		if (checkOffset) {
			Object o = DataBase.getData("offsetPosition", true);
			if (o != null) {
				double p[] = (double[]) o;
				// Return the barycenter position in case the ephemerides are for Charon
				if (p[0] != 0.0 || p[1] != 0.0 || p[2] != 0.0) return pos;
			}
		}
		MoonOrbitalElement orbit[] = MoonEphem.getMoonElements("Charon", jd);
		if (orbit == null || orbit.length == 0) return pos;
		double dp[] = MoonEphem.rocks(jd, orbit[0], method);
		
		// Try to derive approximate velocity
		double dt = 0.1;
		double dpp[] = MoonEphem.rocks(jd+dt, orbit[0], method);
		dp = new double[] {dp[0], dp[1], dp[2], (dpp[0]-dp[0])/dt, (dpp[1]-dp[1])/dt, (dpp[2]-dp[2])/dt};
		
		dp = Functions.scalarProduct(dp, 0.1218);
		return Functions.sumVectors(pos, dp);
	}

	private static JPLEphemeris jpl = null; // Used to avoid creating a new object all the time in Ephem class
	static synchronized EphemElement getBodyEphem(TimeElement time, ObserverElement obs,
			EphemerisElement eph, double offset[], double JD)
	throws JPARSECException {
		if (!eph.targetBody.isNaturalSatellite() && eph.algorithm == EphemerisElement.ALGORITHM.NATURAL_SATELLITE)
			eph.algorithm = EphemerisElement.ALGORITHM.MOSHIER;

		MoonEphem.setEphemerisOffset(eph, offset, JD);

		EphemElement ephem_plan = null;
		if (eph.targetBody == TARGET.SUN && !eph.preferPrecisionInEphemerides) {
			ephem_plan = new EphemElement();
			double sun[] = Saros.getSunPosition(JD);
			LocationElement eqloc = CoordinateSystem.eclipticToEquatorial(new LocationElement(sun[0], sun[1], sun[2]), time, obs, eph);
			ephem_plan.setEquatorialLocation(eqloc);
		} else {
			if (eph.algorithm.isJPL()) {
				try {
					if (jpl == null || eph.algorithm != jpl.getJPLVersionID()) 
						jpl = new JPLEphemeris(eph.algorithm, Configuration.JPL_EPHEMERIDES_FILES_EXTERNAL_PATH);
					ephem_plan = jpl.getJPLEphemeris(time, obs, eph);
				} catch (JPARSECException exc) {
					Logger.log(LEVEL.WARNING, "Could not use JPL ephemerides ("+eph.algorithm+"). Using Moshier instead.");
					ephem_plan = PlanetEphem.MoshierEphemeris(time, obs, eph);
				}
			} else {
				ephem_plan = Ephem.getEphemeris(time, obs, eph, false);
			}
		}

		if (!offset.equals(OFFSET0)) MoonEphem.setEphemerisOffset(eph, OFFSET0, JD);
		return ephem_plan;
	}

	private static void setEphemerisOffset(EphemerisElement eph, double[] eq, double JD)
	throws JPARSECException {
		if (!eq.equals(OFFSET0)) {
			switch (eph.algorithm)
			{
			case JPL_DE200:
				break;
			case SERIES96_MOSHIERForMoon:
				eq = Ephem.DynamicaltoICRSFrame(eq);
				break;
			case MOSHIER:
			case NATURAL_SATELLITE:
				eq = Ephem.DynamicaltoICRSFrame(eq);
				eq = Ephem.equatorialToEcliptic(eq, Constant.J2000, eph);
				break;
			case ORBIT:
			case VSOP87_ELP2000ForMoon:
				eq = Ephem.equatorialToEcliptic(eq, Constant.J2000, eph);
				break;
			default:
				if (!eph.algorithm.isJPL())
					throw new JPARSECException("unsupported algorithm "+eph.algorithm+" in natural satellites ephemeris calculations.");
				eq = Ephem.DynamicaltoICRSFrame(eq);
			}
		}
		DataBase.addData("offsetPosition", eq, true);
	}

	private static MoonOrbitalElement[] getMoonElements(String target, double jd) throws JPARSECException {
		double limit0 = new AstroDate(OLD_YEAR, 1, 1).jd();
		//double limitf = new AstroDate(YEAR, 1, 1).jd();

		String orb[] = MoonEphem.ORBITS;
		if (jd <= limit0) orb = OLD_ORBITS;

		if (target != null) {
			int index = DataSet.getIndexStartingWith(orb, target+" ");
			if (index >= 0) {
				orb = new String[] {orb[index]};
				target = TARGET.values()[Integer.parseInt(FileIO.getField(2, orb[0], " ", true))].getName();
			}
 		}

		TARGET centralBody = Target.getID(target);
		if (!centralBody.isPlanet() && centralBody != TARGET.Pluto)
			throw new JPARSECException("satellite "+target+" not found in the set of JPL elements.");

		ArrayList<MoonOrbitalElement> list = new ArrayList<MoonOrbitalElement>();
		ArrayList<String> added = new ArrayList<String>();
		// Name, central body ID, epoch JD, a (AU), e, periapsis long (rad), mean lon, incl, ascending node long, mean motion (rad/day), apsis rate, node rate (rad/day), Laplace pole RA, DEC
		for (int i=0; i<orb.length; i++) {
			int cb = Integer.parseInt(FileIO.getField(2, orb[i], " ", true));
			if (cb == centralBody.ordinal()) {
				MoonOrbitalElement out = getMoonOrbit(orb[i]);

				if (out.referenceTime < jd + 75 * 365.25 && out.referenceTime > jd - 75 * 365.25) {
					list.add(out);
					added.add(out.name);
				}
			}
		}

		// Add more satellites from dependency
		// This feature is disabled due to the high level of inconsistencies I find in the
		// orbital elements of natural satellites as given in http://ssd.jpl.nasa.gov/?sat_elem.
		// It is impossible to keep a correct set of orbital elements. For instance, positions
		// of Miranda ... Oberon are fine, but Cupid/Puck have an offset of 180 degrees despite
		// of having the same reference plane for the elements.
/*		ReadFile me = new ReadFile();
		me.readFileOfNaturalSatellites(centralBody);
		Object v[] = me.getReadElements();
		for (int i = 0; i < v.length; i++)
		{
			MoonOrbitalElement moon = (MoonOrbitalElement) v[i];

			if (!added.contains(moon.name)) {
				//if (moon.referencePlane == REFERENCE_PLANE.EQUATOR || moon.referencePlane == REFERENCE_PLANE.LAPLACE
				//	 || moon.referencePlane == REFERENCE_PLANE.PLANET_EQUATOR)
					list.add(moon);
			}
		}
*/
		MoonOrbitalElement out[] = new MoonOrbitalElement[list.size()];
		for (int i=0; i<list.size(); i++) {
			out[i] = list.get(i);
		}
		return out;
	}

	protected static MoonOrbitalElement[] getMoonOrbitalElements(String target, double jd) throws JPARSECException {
		return getMoonElements(target, jd);
	}

	private static MoonOrbitalElement getMoonOrbit(String record) throws JPARSECException {
		String nom = FileIO.getField(1, record, " ", true);
		int cb = Integer.parseInt(FileIO.getField(2, record, " ", true));
		double epoch = Double.parseDouble(FileIO.getField(3, record, " ", true));
		double sma = Double.parseDouble(FileIO.getField(4, record, " ", true));
		double e = Double.parseDouble(FileIO.getField(5, record, " ", true));
		double pl = Double.parseDouble(FileIO.getField(6, record, " ", true));
		double ml = Double.parseDouble(FileIO.getField(7, record, " ", true));
		double inc = Double.parseDouble(FileIO.getField(8, record, " ", true));
		double anl = Double.parseDouble(FileIO.getField(9, record, " ", true));
		double mm = Double.parseDouble(FileIO.getField(10, record, " ", true));
		double ar = Double.parseDouble(FileIO.getField(11, record, " ", true));
		double nr = Double.parseDouble(FileIO.getField(12, record, " ", true));
		double ra = 0.0, dec = 0.0;
		boolean laplace = false;
		if (FileIO.getNumberOfFields(record, " ", true) == 14) {
			laplace = true;
			ra = Double.parseDouble(FileIO.getField(13, record, " ", true));
			dec = Double.parseDouble(FileIO.getField(14, record, " ", true));
		}

		MoonOrbitalElement out = new MoonOrbitalElement(sma, ml, e, pl, anl, inc, epoch);
		out.name = nom;
		out.meanMotion = mm;
		out.argumentOfPeriapsisPrecessionRate = ar;
		out.ascendingNodePrecessionRate = nr;
		out.LaplacePoleRA = ra;
		out.LaplacePoleDEC = dec;
		out.referenceEquinox = Constant.J2000;
		out.referencePlane = MoonOrbitalElement.REFERENCE_PLANE.LAPLACE;
		out.centralBody = TARGET.values()[cb];
		//if (!laplace) throw new JPARSECException("Unsupported elements");
		if (!laplace) out.referencePlane = REFERENCE_PLANE.EQUATOR;
		return out;
	}

	/**
	 * Calculate position of minor satellites, providing full data. This method
	 * uses orbital elements from the JPL. See <A
	 * target="_blank" href="http://ssd.jpl.nasa.gov/?sat_elem">http://ssd.jpl.nasa.gov/?sat_elem</A>
	 * for reference. Elements used in this method are not exactly the same since
	 * they have been completed/corrected using other sources.
	 * <P>
	 * All officially approved satellites with calculated orbital elements and
	 * known rotational model are available. Note that {@linkplain TARGET} class does not
	 * support every discovered satellite, since a lot of dwarf satellites has
	 * no known rotational models. For this reason, no full ephemeris can be
	 * calculated for satellites that are not listed in {@linkplain TARGET} class (That's, by
	 * the way, the reason to skip them). Otherwise, partial ephemerides can be
	 * obtained for those satellites (position) using
	 * {@linkplain MoonEphem#calcJPLSatellite(TimeElement, ObserverElement, EphemerisElement, String)}
	 * method.
	 * <P>
	 * To obtain the ephemeris the position of the mother planet is calculated
	 * using the algorithm selected in the ephemeris object. Valid algorithms
	 * includes the Moshier fit, Series96, VSOP, and JPL ephemerides. Deflection due
	 * to planet is ignored.
	 * <P>
	 * Note that this method calculates only the position of the selected object, which
	 * means that mutual phenomena events between this and other satellites cannot be
	 * calculated, only mutual phenomena between this satellite and the mother planet can
	 * be calculated.
	 *
	 * @param time Time object containing the date.
	 * @param obs Observer object containing the observer position.
	 * @param eph Ephemeris object with the target and ephemeris
	 *        properties.
	 * @return Moon ephem object containing full ephemeris data. In case the input time
	 * is more than 50 years away from the reference time of the elements, an error will
	 * be thrown.
	 * @throws JPARSECException If the calculation fails.
	 */
	public static MoonEphemElement calcSatellite(TimeElement time,
			ObserverElement obs, EphemerisElement eph) throws JPARSECException
	{
		try
		{
			// Determine central body
			TARGET central_body = eph.targetBody.getCentralBody();
			if (central_body.isNaturalSatellite()) central_body = central_body.getCentralBody();
			if (central_body == TARGET.SUN)
				throw new JPARSECException("invalid target/central body.");

			double jd = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);

			// Get elements for target
			MoonOrbitalElement orbit = getMoonElements(eph.targetBody.getEnglishName(), jd)[0];

			MoonEphemElement moon = MoonEphem.solveSatellite(time, obs, eph, orbit, central_body);

			return moon;
		} catch (JPARSECException ve)
		{
			throw ve;
		}
	}

	/**
	 * Calculate position of a natural satellite using orbital elements.
	 * <P>
	 * To obtain the ephemeris the position of the mother planet is calculated
	 * using the algorithm selected in the ephemeris object. Valid algorithms
	 * includes the Moshier fit, Series96, VSOP, and JPL ephemerides. The target
	 * body should be the mother planet.
	 * <P>
	 * Note that this method calculates only the position of the selected object, which
	 * means that mutual phenomena events between this and other satellites cannot be
	 * calculated, only mutual phenomena between this satellite and the mother planet can
	 * be calculated.
	 *
	 * @param time Time object containing the date.
	 * @param obs Observer object containing the observer position.
	 * @param eph Ephemeris object with the mother planet as target and ephemeris properties.
	 * @param orbit The orbital elements.
	 * @return Moon ephem object containing full ephemeris data.
	 * @throws JPARSECException If the calculation fails.
	 */
	public synchronized static MoonEphemElement calcSatellite(TimeElement time,
			ObserverElement obs, EphemerisElement eph, MoonOrbitalElement orbit) throws JPARSECException
	{
		try
		{
			// Determine central body
			TARGET central_body = eph.targetBody.getCentralBody();
			if (central_body.isNaturalSatellite()) central_body = central_body.getCentralBody();
			if (central_body == TARGET.SUN)
				throw new JPARSECException("invalid target/central body.");

			MoonEphemElement moon = MoonEphem.solveSatellite(time, obs, eph, orbit, central_body);

			return moon;
		} catch (JPARSECException ve)
		{
			throw ve;
		}
	}

	/**
	 * Calculate position of a minor JPL satellite, providing full data. This method
	 * uses orbital elements from the JPL. See <A
	 * target="_blank" href="http://ssd.jpl.nasa.gov/?sat_elem">http://ssd.jpl.nasa.gov/?sat_elem</A>
	 * for reference. Elements used in this method are not exactly the same since
	 * they have been completed/corrected using other sources.
	 * <P>
	 * All officially approved satellites with calculated orbital elements and
	 * known rotational model are available. Since the {@linkplain TARGET} class does not
	 * support every discovered satellite with
	 * no known rotational models, partial ephemerides can be
	 * obtained for those satellites (position) using this method.
	 * <P>
	 * To obtain the ephemeris the position of the mother planet is calculated
	 * using the algorithm selected in the ephemeris object. The target body field
	 * should be set to the Id constant of the mother planet, since the constant for
	 * the satellite itself is supposed to be unavailable (existent satellites can be
	 * obtained as well, but it is mandatory to use the mother planet Id constant). Valid algorithms
	 * includes the Moshier fit, Series96, VSOP, and JPL ephemerides. Deflection due
	 * to planet is ignored.
	 * <P>
	 * Note that this method calculates only the position of the selected object, which
	 * means that mutual phenomena events between this and other satellites cannot be
	 * calculated, only mutual phenomena between this satellite and the mother planet can
	 * be calculated.
	 *
	 * @param time Time object containing the date.
	 * @param obs Observer object containing the observer position.
	 * @param eph Ephemeris object with the mother planet as target and ephemeris
	 *        properties.
	 * @param satName The name of the satellite.
	 * @return Moon ephem object containing full ephemeris data. In case the input time
	 * is more than 50 years away from the reference time of the elements, an error will
	 * be thrown.
	 * @throws JPARSECException If the calculation fails.
	 */
	public static MoonEphemElement calcJPLSatellite(TimeElement time,
			ObserverElement obs, EphemerisElement eph, String satName)
			throws JPARSECException
	{
		try
		{
			// Determine central body
			TARGET central_body = eph.targetBody;
			if (central_body.isNaturalSatellite()) central_body = central_body.getCentralBody();
			if (central_body == TARGET.SUN)
				throw new JPARSECException("invalid central body.");

			double jd = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);

			// Get elements for target
			MoonOrbitalElement orbit = getMoonElements(satName, jd)[0];

			MoonEphemElement moon = MoonEphem.solveSatellite(time, obs, eph, orbit, central_body);

			return moon;
		} catch (JPARSECException ve)
		{
			throw ve;
		}
	}

	private static final double[] OFFSET0 = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };

	private static MoonEphemElement solveSatellite(TimeElement time, ObserverElement obs, EphemerisElement eph,
			MoonOrbitalElement orbit, TARGET central_body)
	throws JPARSECException {
		// Obtain dynamical time in julian days
		double JD = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);

		// Construct new ephemeris objects
		EphemerisElement new_eph = eph.clone();
		new_eph.targetBody = central_body;
		EphemerisElement new_eph_sun = eph.clone();
		new_eph_sun.targetBody = TARGET.SUN;
		EphemerisElement new_eph_moon = eph.clone();

		// Obtain position of planet
		EphemElement ephem_plan = MoonEphem.getBodyEphem(time, obs, new_eph, OFFSET0, JD);

		// Obtain position of sun
		EphemElement ephem_sun = MoonEphem.getBodyEphem(time, obs, new_eph_sun, OFFSET0, JD);

		// Obtain new light time value
		double light_time_new = ephem_plan.lightTime;

		EphemElement ephem = new EphemElement();
		MoonEphemElement moon = new MoonEphemElement();
		double light_time = ephem_plan.lightTime;

		do
		{
			light_time = light_time_new;
			if (eph.ephemType == COORDINATES_TYPE.GEOMETRIC) light_time = 0.0;

			double ELEM[] = MoonEphem.rocks(JD - light_time, orbit, eph.ephemMethod);

			if (ELEM == null) throw new JPARSECException("cannot solve the orbit of this satellite.");

			// Precession to J2000 if necessary
			if (orbit.referenceEquinox != EphemerisElement.EQUINOX_J2000) {
				ELEM = Precession.precess(orbit.referenceEquinox, Constant.J2000, ELEM, eph);
			}

			if (new Double(ELEM[0]).equals(Double.NaN)) throw new JPARSECException("cannot solve the orbit of this satellite.");

			// Obtain position of satellite
			ephem = MoonEphem.getBodyEphem(time, obs, new_eph, new double[] {ELEM[0], ELEM[1], ELEM[2], 0.0, 0.0, 0.0}, JD);

			light_time_new = ephem.lightTime;
			if (!eph.preferPrecisionInEphemerides || eph.ephemType == COORDINATES_TYPE.GEOMETRIC) break;
		} while (Math.abs(light_time_new - light_time) > (1.0E-3 / Constant.SECONDS_PER_DAY));

		moon = MoonEphemElement.parseEphemElement(ephem);
		try {
			new_eph_moon.targetBody = Target.getIDFromEnglishName(orbit.name);
			moon.name = orbit.name;
			if (new_eph_moon.targetBody != TARGET.NOT_A_PLANET) moon.name = new_eph_moon.targetBody.getName();
			moon = MoonPhysicalParameters.physicalParameters(JD, ephem_sun, moon, obs, new_eph_moon);
		} catch (Exception exc) {
			moon.name = Translate.translate(orbit.name);
		}
		moon = MoonEphem.satellitePhenomena(moon, ephem_plan, central_body);

		return moon;
	}
	/**
	 * Calculate position of all JPL minor satellites, providing full data. This
	 * method uses orbital elements from the JPL ephemerides. See <A
	 * target="_blank" href="http://ssd.jpl.nasa.gov/?sat_ephem">http://ssd.jpl.nasa.gov/?sat_ephem</A>
	 * for reference, time span validity, error expectance, and so on. Elements used in this method are not exactly the same since
	 * they have been completed/corrected using other sources.
	 * <P>
	 * All officially approved satellites with calculated orbital elements are
	 * available, with no exception. Note that some of them are not supported in
	 * {@linkplain TARGET} class (no ID value), since a lot of dwarf satellites has no known
	 * rotational models. Otherwise, ephemerides can be obtained for those
	 * satellites using this method.
	 * <P>
	 * To obtain the ephemeris the position of the mother planet is calculated
	 * using the algorithm selected in the ephemeris object. Valid algorithms
	 * includes the Moshier fit, Series96, VSOP, and JPL ephemerides. Deflection due
	 * to planet is ignored.
	 *
	 * @param time Time object containing the date.
	 * @param obs Observer object containing the observer position.
	 * @param eph Ephemeris object with the central body as target, and
	 *        ephemeris properties.
	 * @return Moon ephem set of objects containing full ephemeris data. In case the input time
	 * is more than 50 years away from the reference time of the elements for some satellite,
	 * that satellite will be ignored.
	 * @throws JPARSECException If the calculation fails or in case there's no satellite with
	 * valid orbital elements.
	 */
	public static MoonEphemElement[] calcAllJPLSatellites(TimeElement time,
			ObserverElement obs, EphemerisElement eph) throws JPARSECException
	{
		try
		{
			// Return ephemerides for satellites using JPL elements between 1950 and 2050 (or last update
			// time + 20 years in case this date is after year 2050)
			double JD = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
/*			double minDate = 2433282.5, maxDate = 2469807.5; // 1950, 1, 1 to 2050, 1, 1
			AstroDate lastModified = Configuration.getResourceLastModifiedTime(
					FileIO.DATA_ORBITAL_ELEMENTS_JARFILE,
					FileIO.DATA_ORBITAL_ELEMENTS_DIRECTORY, FileIO.getFileNameFromPath(PATH_TO_JPL_SATELLITES_FILE));
			if (lastModified != null) {
				AstroDate maxDate2 = new AstroDate(lastModified.getYear() + 20, 1, 1);
				if (JD < minDate) return null;
				if (JD > Math.max(maxDate, maxDate2.jd())) return null;
			} else {
				if (JD < minDate) return null;
				if (JD > maxDate) return null;
			}
*/
			// Determine central body
			TARGET central_body = eph.targetBody;
			if (central_body.isNaturalSatellite()) central_body = central_body.getCentralBody();
			if (central_body == TARGET.SUN)
				return null;

			// Get elements for targets
			MoonOrbitalElement orbits[] = getMoonElements(eph.targetBody.getName(), JD);

			MoonEphemElement moons[] = new MoonEphemElement[orbits.length];
			boolean found = false;
/*			for (int index = 0; index < orbits.length; index++)
			{
				MoonOrbitalElement orbit = orbits[index];
				if (obs.getMotherBody() == Target.getIDFromEnglishName(orbit.name)) {
					found = true;
					break;
				}
			}
*/			if (found) moons = new MoonEphemElement[orbits.length-1];

			int delete = 0, mindex = -1;
			for (int index = 0; index < orbits.length; index++)
			{
				MoonOrbitalElement orbit = orbits[index];
				//if (obs.getMotherBody() == Target.getIDFromEnglishName(orbit.name)) continue;
				mindex ++;

				// Construct new ephemeris objects
				EphemerisElement new_eph = eph.clone();
				new_eph.targetBody = central_body;
				EphemerisElement new_eph_sun = eph.clone();
				new_eph_sun.targetBody = TARGET.SUN;
				EphemerisElement new_eph_moon = eph.clone();

				// Obtain position of planet
				EphemElement ephem_plan = MoonEphem.getBodyEphem(time, obs, new_eph, OFFSET0, JD);

				// Obtain position of sun
				EphemElement ephem_sun = MoonEphem.getBodyEphem(time, obs, new_eph_sun, OFFSET0, JD);

				// Obtain new light time value
				double light_time_new = ephem_plan.lightTime;

				EphemElement ephem = new EphemElement();
				MoonEphemElement moon = new MoonEphemElement();
				double light_time = ephem_plan.lightTime;

				do
				{
					light_time = light_time_new;
					if (eph.ephemType == COORDINATES_TYPE.GEOMETRIC) light_time = 0.0;

					double ELEM[] = MoonEphem.rocks(JD - light_time, orbit, eph.ephemMethod);

					if (ELEM == null) {
						ephem = null;
						break;
					}

					// Precession to J2000 if necessary
					if (orbit.referenceEquinox != EphemerisElement.EQUINOX_J2000)
						ELEM = Precession.precess(orbit.referenceEquinox, Constant.J2000, ELEM, eph);

					// Obtain position of satellite
					ephem = MoonEphem.getBodyEphem(time, obs, new_eph, new double[] {ELEM[0], ELEM[1], ELEM[2], 0.0, 0.0, 0.0}, JD);

					light_time_new = ephem.lightTime;
					if (!eph.preferPrecisionInEphemerides || eph.ephemType == COORDINATES_TYPE.GEOMETRIC) break;
				} while (Math.abs(light_time_new - light_time) > (1.0E-3 / Constant.SECONDS_PER_DAY));

				moon = null;
				if (ephem != null) {
					moon = MoonEphemElement.parseEphemElement(ephem);
					try {
						new_eph_moon.targetBody = Target.getIDFromEnglishName(orbit.name);
						moon.name = orbit.name;
						if (new_eph_moon.targetBody != TARGET.NOT_A_PLANET) moon.name = new_eph_moon.targetBody.getName();
						moon = MoonPhysicalParameters.physicalParameters(JD, ephem_sun, moon, obs, new_eph_moon);
					} catch (Exception exc) {
						moon.name = Translate.translate(orbit.name);						
					}
					moon = MoonEphem.satellitePhenomena(moon, ephem_plan, central_body);
				}

				if (moon == null || new Double(moon.rightAscension).equals(Double.NaN)) {
					moon = null;
					delete ++;
				}

				moons[mindex] = moon;

				// Obtain relative phenomena
				if (index == orbits.length-1) {
					if (delete > 0) {
						MoonEphemElement newMoon[] = new MoonEphemElement[moons.length-delete];
						int j = -1;
						for (int i=0; i<moons.length; i++) {
							if (moons[i] != null) {
								j++;
								newMoon[j] = moons[i];
							}
						}
						moons = newMoon;
					}
					moons = MoonEphem.satellitesPhenomena(moons, ephem_plan.angularRadius);
					// Little trick with poor performance to obtain eclipse phenomena independently of
					// the mother object. Basically we go back to Earth to compute them from there.
					if (obs.getMotherBody() != TARGET.EARTH && moons.length > 0) {
						ObserverElement obs2 = obs.clone();
						obs2.forceObserverOnEarth();
						MoonEphemElement moon2[] = calcAllJPLSatellites(time, obs2, eph);

						if (moon2.length > 0 && !Double.isInfinite(moon2[0].distance) && !Double.isNaN(moon2[0].distance)) {
							TimeElement time2 = time.clone();
							time2.add((moon2[0].distance-moons[0].distance)*Constant.LIGHT_TIME_DAYS_PER_AU);
							moon2 = calcAllJPLSatellites(time2, obs2, eph);
						}
						String nom2[] = new String[moon2.length];
						for (int i = 0; i < moon2.length; i++) {
							nom2[i] = moon2[i].name;
						}
						String nom[] = new String[moons.length];
						for (int i = 0; i < moons.length; i++) {
							nom[i] = moons[i].name;
						}
						for (int i = 0; i < moons.length; i++) {
							int ii = DataSet.getIndex(nom2, nom[i]);
							if (ii >= 0) {
								moons[i].eclipsed = moon2[ii].eclipsed;
								moons[i].shadowTransiting = moon2[ii].shadowTransiting;
								//moons[i].mutualPhenomena = moon2[ii].mutualPhenomena;
								moons[i].xPositionFromSun = moon2[ii].xPositionFromSun;
								moons[i].yPositionFromSun = moon2[ii].yPositionFromSun;
								moons[i].zPositionFromSun = moon2[ii].zPositionFromSun;
							}
						}
					}

				}
			}

			return moons;
		} catch (JPARSECException ve)
		{
			throw ve;
		} catch (Exception e)
		{
			e.printStackTrace();
			throw new JPARSECException(e);
		}
	}

	/**
	 * Calculate position of all minor satellites, providing full data. This
	 * method uses the main theories for natural satellites, and adds those
	 * from JPL elements to complete the set of ephemerides, optionally.
	 * <P>
	 * To obtain the ephemeris the position of the mother planet is calculated
	 * using the algorithm selected in the ephemeris object. Valid algorithms
	 * includes the Moshier fit, Series96, VSOP, and JPL ephemerides. Deflection due
	 * to planet is ignored.
	 *
	 * @param time Time object containing the date.
	 * @param obs Observer object containing the observer position.
	 * @param eph Ephemeris object with the central body as target, and
	 *        ephemeris properties.
	 * @param addJPL True to add satellites from JPL elements.
	 * @return Moon ephem set of objects containing full ephemeris data, or null
	 * for an unsupported central body.
	 * @throws JPARSECException If the calculation fails.
	 */
	public static MoonEphemElement[] calcAllSatellites(TimeElement time,
			ObserverElement obs, EphemerisElement eph, boolean addJPL) throws JPARSECException
	{
		try
		{
			// Determine central body
			TARGET central_body = eph.targetBody;
			if (central_body.isNaturalSatellite()) central_body = central_body.getCentralBody();

			MoonEphemElement m[] = null;

			switch (central_body) {
			case MARS:
				m = MoonEphem.martianSatellitesEphemerides_2007(time, obs, eph);
				addJPL = false; // No more
				break;
			case JUPITER:
				m = MoonEphem.galileanSatellitesEphemerides_L1(time, obs, eph);
				break;
			case SATURN:
				m = MoonEphem.saturnianSatellitesEphemerides_TASS17(time, obs, eph, false);
				break;
			case URANUS:
				m = MoonEphem.uranianSatellitesEphemerides_GUST86(time, obs, eph);
				break;
			case NEPTUNE:
				// Triton method is deprecated
				m = new MoonEphemElement[] {MoonEphem.calcJPLSatellite(time, obs, eph, TARGET.Triton.getEnglishName())};
				break;
			case Pluto:
				// Triton method is deprecated
				m = new MoonEphemElement[] {MoonEphem.calcJPLSatellite(time, obs, eph, TARGET.Charon.getEnglishName())};
				break;
			default:
				return null;
			}

			if (m == null) m = new MoonEphemElement[0];
			if (m.length > 0) {
				boolean found = false;
/*				for (int i=0; i<m.length; i++) {
					if (obs.getMotherBody() == Target.getID(m[i].name)) {
						found = true;
						m[i] = null;
						break;
					}
				}
*/				if (found) {
					MoonEphemElement mm[] = new MoonEphemElement[m.length-1];
					int index = -1;
					for (int i=0; i<m.length; i++) {
						if (m[i] != null) {
							index ++;
							mm[index] = m[i];
						}
					}
					m = mm;
				}
			}
			if (!addJPL) return m;
			MoonEphemElement moon[] = MoonEphem.calcAllJPLSatellites(time, obs, eph);
			if (moon == null || moon.length == 0) return m;

			ArrayList<MoonEphemElement> al = new ArrayList<MoonEphemElement>();
			for (int i=0; i<moon.length; i++) {
				if (moon[i] != null) {
					for (int j = 0; j < m.length; j ++) {
						if (m[j].name.trim().toLowerCase().equals(moon[i].name.trim().toLowerCase())) {
							moon[i] = null;
							break;
						}
					}
					if (moon[i] != null) al.add(moon[i]);
				}
			}

			if (al.size() == 0) return m;

			MoonEphemElement om[] = new MoonEphemElement[m.length + al.size()];
			for (int i=0; i<m.length; i++) {
				om[i] = m[i];
			}
			for (int i=0; i<al.size(); i++) {
				om[i+m.length] = al.get(i);
			}
			return om;
		} catch (JPARSECException ve)
		{
			throw ve;
		}
	}
	/**
	 * This is an implementation of the 2007 Martian satellites ephemerides by
	 * V. Lainey et al. For reference see
	 * Lainey, V., Dehant, V. and Paetzold, M. "First numerical ephemerides of the Martian moons",
	 * Astron. Astrophys., vol 465 pp.1075-1084	(2007).
	 * <P>
	 * This is the best available theory for the motion of the Martian satellites
	 * Phobos and Deimos.
	 * <P>
	 * Satellites are positioned respect to Mars. For Mars coordinates,
	 * the algorithm selected in the ephemeris object is used. Valid algorithms
	 * includes the Moshier fit, Series96, VSOP, and JPL ephemerides. Deflection due
	 * to planet is ignored, since the effect is negligible.
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @return An array of Moon ephem objects with the ephemeris.
	 * @throws JPARSECException If the calculation fails.
	 */
	public static MoonEphemElement[] martianSatellitesEphemerides_2007(TimeElement time,
			ObserverElement obs, EphemerisElement eph) throws JPARSECException
	{
		try
		{
			// Obtain dynamical time in julian days
			double JD = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);

			// Obtain position of sun
			EphemerisElement new_eph = eph.clone();
			new_eph.targetBody = TARGET.SUN;
			EphemElement ephem_sun = MoonEphem.getBodyEphem(time, obs, new_eph, OFFSET0, JD);

			// Obtain position of planet
			new_eph.targetBody = TARGET.MARS;
			EphemElement ephem_mars = MoonEphem.getBodyEphem(time, obs, new_eph, OFFSET0, JD);

			EphemElement ephem[] = new EphemElement[2];
			MoonEphemElement moon[] = new MoonEphemElement[2];
			for (int nsat = 1; nsat <= 2; nsat++)
			{
				new_eph.targetBody = TARGET.MARS;
				double light_time = ephem_mars.lightTime, light_time_new = light_time;
				do
				{
					light_time = light_time_new;
					if (eph.ephemType == COORDINATES_TYPE.GEOMETRIC) light_time = 0.0;

					double ELEM[] = Mars07.getMoonPosition(JD - light_time, nsat, true);

					// Obtain position of satellite
					ephem[nsat-1] = MoonEphem.getBodyEphem(time, obs, new_eph, new double[] {ELEM[0], ELEM[1], ELEM[2], 0.0, 0.0, 0.0}, JD);

					light_time_new = ephem[nsat - 1].lightTime;
					if (!eph.preferPrecisionInEphemerides || eph.ephemType == COORDINATES_TYPE.GEOMETRIC) break;
				} while (Math.abs(light_time_new - light_time) > (1.0E-3 / Constant.SECONDS_PER_DAY));

				moon[nsat - 1] = MoonEphemElement.parseEphemElement(ephem[nsat - 1]);
				new_eph.targetBody = TARGET.values()[TARGET.Phobos.ordinal() + nsat - 1];
				moon[nsat - 1] = MoonPhysicalParameters.physicalParameters(JD, ephem_sun, moon[nsat - 1], obs, new_eph);
				moon[nsat - 1] = MoonEphem.satellitePhenomena(moon[nsat - 1], ephem_mars, TARGET.MARS);
				moon[nsat - 1].name = TARGET.values()[TARGET.Phobos.ordinal() + nsat - 1].getName();

				// Obtain relative phenomena
				if (nsat == 2) {
					moon = MoonEphem.satellitesPhenomena(moon, ephem_mars.angularRadius);
					// Little trick with poor performance to obtain eclipse phenomena independently of
					// the mother object. Basically we go back to Earth to compute them from there.
					if (obs.getMotherBody() != TARGET.EARTH && moon.length > 0) {
						ObserverElement obs2 = obs.clone();
						obs2.forceObserverOnEarth();
						MoonEphemElement moon2[] = martianSatellitesEphemerides_2007(time, obs2, eph);
						if (moon2.length > 0 && !Double.isInfinite(moon2[0].distance) && !Double.isNaN(moon2[0].distance)) {
							TimeElement time2 = time.clone();
							time2.add((moon2[0].distance-moon[0].distance)*Constant.LIGHT_TIME_DAYS_PER_AU);
							moon2 = martianSatellitesEphemerides_2007(time2, obs2, eph);
						}
						String nom2[] = new String[moon2.length];
						for (int i = 0; i < moon2.length; i++) {
							nom2[i] = moon2[i].name;
						}
						String nom[] = new String[moon.length];
						for (int i = 0; i < moon.length; i++) {
							nom[i] = moon[i].name;
						}
						for (int i = 0; i < moon.length; i++) {
							int ii = DataSet.getIndex(nom2, nom[i]);
							if (ii >= 0) {
								moon[i].eclipsed = moon2[ii].eclipsed;
								moon[i].shadowTransiting = moon2[ii].shadowTransiting;
								//moon[i].mutualPhenomena = moon2[ii].mutualPhenomena;
								moon[i].xPositionFromSun = moon2[ii].xPositionFromSun;
								moon[i].yPositionFromSun = moon2[ii].yPositionFromSun;
								moon[i].zPositionFromSun = moon2[ii].zPositionFromSun;
							}
						}
					}
				}
			}

			return moon;
		} catch (JPARSECException ve)
		{
			throw ve;
		}
	}

	/**
	 * This is an implementation of the L1 ephemerides by V. Lainey et al. For
	 * reference see Astronomy and Astrophysics 427, 371 (2004).
	 * <P>
	 * This is the best available theory for the motion of the main satellites
	 * of Jupiter: Io, Europa, Ganymede, and Callisto.
	 * <P>
	 * Satellites are positioned respect to Jupiter. For Jupiter coordinates,
	 * the algorithm selected in the ephemeris object is used. Valid algorithms
	 * includes the Moshier fit, Series96, VSOP, and JPL ephemerides. Deflection due
	 * to planet is ignored, since the effect is negligible.
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @return An array of Moon ephem objects with the ephemeris.
	 * @throws JPARSECException If the calculation fails.
	 */
	public static MoonEphemElement[] galileanSatellitesEphemerides_L1(TimeElement time,
			ObserverElement obs, EphemerisElement eph) throws JPARSECException
	{
		try
		{
			// Obtain dynamical time in julian days
			double JD = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);

			// Obtain position of sun
			EphemerisElement new_eph = eph.clone();
			new_eph.targetBody = TARGET.SUN;
			EphemElement ephem_sun = MoonEphem.getBodyEphem(time, obs, new_eph, OFFSET0, JD);

			// Obtain position of planet
			new_eph.targetBody = TARGET.JUPITER;
			EphemElement ephem_jup = MoonEphem.getBodyEphem(time, obs, new_eph, OFFSET0, JD);

			EphemElement ephem[] = new EphemElement[4];
			MoonEphemElement moon[] = new MoonEphemElement[4];
			for (int nsat = 1; nsat <= 4; nsat++)
			{
				new_eph.targetBody = TARGET.JUPITER;
				double light_time_new= ephem_jup.lightTime, light_time = light_time_new;
				do
				{
					light_time = light_time_new;
					if (eph.ephemType == COORDINATES_TYPE.GEOMETRIC) light_time = 0.0;

					double ELEM[] = L1.L1_theory(JD - light_time, nsat);

					// Obtain position of satellite
					ephem[nsat-1] = MoonEphem.getBodyEphem(time, obs, new_eph, new double[] {ELEM[0], ELEM[1], ELEM[2], 0.0, 0.0, 0.0}, JD);

					light_time_new = ephem[nsat - 1].lightTime;
					if (!eph.preferPrecisionInEphemerides || eph.ephemType == COORDINATES_TYPE.GEOMETRIC) break;
				} while (Math.abs(light_time_new - light_time) > (1.0E-3 / Constant.SECONDS_PER_DAY));

				moon[nsat - 1] = MoonEphemElement.parseEphemElement(ephem[nsat - 1]);
				new_eph.targetBody = TARGET.values()[TARGET.Io.ordinal() + nsat - 1];
				moon[nsat - 1] = MoonPhysicalParameters.physicalParameters(JD, ephem_sun, moon[nsat - 1], obs, new_eph);
				moon[nsat - 1] = MoonEphem.satellitePhenomena(moon[nsat - 1], ephem_jup, TARGET.JUPITER);
				moon[nsat - 1].name = TARGET.values()[TARGET.Io.ordinal() + nsat - 1].getName();

				// Obtain relative phenomena
				if (nsat == 4) {
					moon = MoonEphem.satellitesPhenomena(moon, ephem_jup.angularRadius);
					// Little trick with poor performance to obtain eclipse phenomena independently of
					// the mother object. Basically we go back to Earth to compute them from there.
					if (obs.getMotherBody() != TARGET.EARTH && moon.length > 0) {
						ObserverElement obs2 = obs.clone();
						obs2.forceObserverOnEarth();
						MoonEphemElement moon2[] = galileanSatellitesEphemerides_L1(time, obs2, eph);
						if (moon2.length > 0 && !Double.isInfinite(moon2[0].distance) && !Double.isNaN(moon2[0].distance)) {
							TimeElement time2 = time.clone();
							time2.add((moon2[0].distance-moon[0].distance)*Constant.LIGHT_TIME_DAYS_PER_AU);
							moon2 = galileanSatellitesEphemerides_L1(time2, obs2, eph);
						}
						String nom2[] = new String[moon2.length];
						for (int i = 0; i < moon2.length; i++) {
							nom2[i] = moon2[i].name;
						}
						String nom[] = new String[moon.length];
						for (int i = 0; i < moon.length; i++) {
							nom[i] = moon[i].name;
						}
						for (int i = 0; i < moon.length; i++) {
							int ii = DataSet.getIndex(nom2, nom[i]);
							if (ii >= 0) {
								moon[i].eclipsed = moon2[ii].eclipsed;
								moon[i].shadowTransiting = moon2[ii].shadowTransiting;
								//moon[i].mutualPhenomena = moon2[ii].mutualPhenomena;
								moon[i].xPositionFromSun = moon2[ii].xPositionFromSun;
								moon[i].yPositionFromSun = moon2[ii].yPositionFromSun;
								moon[i].zPositionFromSun = moon2[ii].zPositionFromSun;
							}
						}
					}
				}
			}

			return moon;
		} catch (JPARSECException ve)
		{
			throw ve;
		}

	}

	/**
	 * Calculate position of Saturn satellites, providing full data. This method
	 * uses TASS1.7 theory from the IMCCE. For reference see A&amp;A 297, 588-605
	 * (1995), and A&amp;A 324, 366 (1997). Objects are Mimas, Enceladus, Tethys,
	 * Dione, Rhea, Titan, Hyperion, Iapetus.
	 * <P>
	 * Satellites are positioned respect to Saturn. For Saturn coordinates, the
	 * the algorithm selected in the ephemeris object is used. Valid algorithms
	 * includes the Moshier fit, Series96, VSOP, and JPL ephemerides. Deflection due
	 * to planet is ignored, since the effect is negligible.
	 *
	 * @param time Time object containing the date.
	 * @param obs Observer object containing the observer position.
	 * @param eph Ephemeris object with the target and ephemeris
	 *        properties.
	 * @param truncate False for using the whole TASS theory, true for using
	 *        only critical terms. Only used in the first call to this method,
	 *        since the terms are stored in memory. A true value improves
	 *        performance while keeping precission in the 0.2 arcsecond level.
	 * @return MoonEphemElement array of objects containing full ephemeris data
	 *         for each satellite.
	 * @throws JPARSECException If the calculation fails.
	 */
	public static MoonEphemElement[] saturnianSatellitesEphemerides_TASS17(TimeElement time,
			ObserverElement obs, EphemerisElement eph, boolean truncate) throws JPARSECException
	{
		try
		{
			// Obtain dynamical time in julian days
			double JD = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);

			// Obtain position of sun
			EphemerisElement new_eph = eph.clone();
			new_eph.targetBody = TARGET.SUN;
			EphemElement ephem_sun = MoonEphem.getBodyEphem(time, obs, new_eph, OFFSET0, JD);

			// Obtain position of planet
			new_eph.targetBody = TARGET.SATURN;
			EphemElement ephem_sat = MoonEphem.getBodyEphem(time, obs, new_eph, OFFSET0, JD);

			EphemElement ephem[] = new EphemElement[8];
			MoonEphemElement moon[] = new MoonEphemElement[8];
			for (int i = 0; i < 8; i++)
			{
				new_eph.targetBody = TARGET.SATURN;
				double light_time = ephem_sat.lightTime, light_time_new = light_time;
				do
				{
					light_time = light_time_new;
					if (eph.ephemType == COORDINATES_TYPE.GEOMETRIC) light_time = 0.0;

					double ecl[] = TASS17.TASS17_theory(JD - light_time, i + 1, truncate);

					// Pass to equatorial
					// Note that ecliptic -> equatorial should be done for time JD instead of J2000,
					// to obtain J2000 equatorial position. TASS is respect J2000 ecliptic but coordinates
					// are calculated for time JD, with a different ecliptic (Earth's tilt). This is
					// independent from precession
					double ELEM[] = Ephem.eclipticToEquatorial(ecl, JD, eph);

					// Obtain position of satellite
					ephem[i] = MoonEphem.getBodyEphem(time, obs, new_eph, new double[] {ELEM[0], ELEM[1], ELEM[2], 0.0, 0.0, 0.0}, JD);

					light_time_new = ephem[i].lightTime;
					if (!eph.preferPrecisionInEphemerides || eph.ephemType == COORDINATES_TYPE.GEOMETRIC) break;
				} while (Math.abs(light_time_new - light_time) > (1.0E-3 / Constant.SECONDS_PER_DAY)); // && !APPROX);

				moon[i] = MoonEphemElement.parseEphemElement(ephem[i]);
				new_eph.targetBody = TARGET.values()[TARGET.Mimas.ordinal() + i];
				moon[i] = MoonPhysicalParameters.physicalParameters(JD, ephem_sun, moon[i], obs, new_eph);
				moon[i] = MoonEphem.satellitePhenomena(moon[i], ephem_sat, TARGET.SATURN);
				moon[i].name = TARGET.values()[TARGET.Mimas.ordinal() + i].getName();

				// Obtain relative phenomena
				if (i == 7) {
					moon = MoonEphem.satellitesPhenomena(moon, ephem_sat.angularRadius);
					// Little trick with poor performance to obtain eclipse phenomena independently of
					// the mother object. Basically we go back to Earth to compute them from there.
					if (obs.getMotherBody() != TARGET.EARTH && moon.length > 0) {
						ObserverElement obs2 = obs.clone();
						obs2.forceObserverOnEarth();
						MoonEphemElement moon2[] = saturnianSatellitesEphemerides_TASS17(time, obs2, eph, false);
						if (moon2.length > 0 && !Double.isInfinite(moon2[0].distance) && !Double.isNaN(moon2[0].distance)) {
							TimeElement time2 = time.clone();
							time2.add((moon2[0].distance-moon[0].distance)*Constant.LIGHT_TIME_DAYS_PER_AU);
							moon2 = saturnianSatellitesEphemerides_TASS17(time2, obs2, eph, false);
						}
						String nom2[] = new String[moon2.length];
						for (int ii = 0; ii < moon2.length; ii++) {
							nom2[ii] = moon2[ii].name;
						}
						String nom[] = new String[moon.length];
						for (int ii = 0; ii < moon.length; ii++) {
							nom[ii] = moon[ii].name;
						}
						for (int i0 = 0; i0 < moon.length; i0++) {
							int ii = DataSet.getIndex(nom2, nom[i0]);
							if (ii >= 0) {
								moon[i0].eclipsed = moon2[ii].eclipsed;
								moon[i0].shadowTransiting = moon2[ii].shadowTransiting;
								//moon[i0].mutualPhenomena = moon2[ii].mutualPhenomena;
								moon[i0].xPositionFromSun = moon2[ii].xPositionFromSun;
								moon[i0].yPositionFromSun = moon2[ii].yPositionFromSun;
								moon[i0].zPositionFromSun = moon2[ii].zPositionFromSun;
							}
						}
					}
				}
			}

			return moon;
		} catch (JPARSECException ve)
		{
			throw ve;
		}
	}

	/**
	 * Obtains the position of the Uranian satellites using GUST86 theory. For
	 * reference see: GUST86 - An analytical ephemeris of the Uranian
	 * satellites, Laskar J., Jacobson, R. Astron. Astrophys. 188, 212-224
	 * (1987). Objects are Miranda, Ariel, Umbriel, Titania, and Oberon.
	 * <P>
	 * Satellites are positioned respect to Uranus. For Uranus coordinates, the
	 * the algorithm selected in the ephemeris object is used. Valid algorithms
	 * includes the Moshier fit, Series96, VSOP, and JPL ephemerides. Deflection due
	 * to planet is ignored, since the effect is negligible.
	 *
	 * @param time Time object containing the date.
	 * @param obs Observer object containing the observer position.
	 * @param eph Ephemeris object with the target and ephemeris
	 *        properties.
	 * @return Moon ephem object full of data.
	 * @throws JPARSECException If the calculation fails.
	 */
	public static MoonEphemElement[] uranianSatellitesEphemerides_GUST86(TimeElement time,
			ObserverElement obs, EphemerisElement eph) throws JPARSECException
	{
		try
		{
			// Obtain dynamical time in julian days
			double JD = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);

			// Obtain position of sun
			EphemerisElement new_eph = eph.clone();
			new_eph.targetBody = TARGET.SUN;
			EphemElement ephem_sun = MoonEphem.getBodyEphem(time, obs, new_eph, OFFSET0, JD);

			// Obtain position of planet
			new_eph.targetBody = TARGET.URANUS;
			EphemElement ephem_ura = MoonEphem.getBodyEphem(time, obs, new_eph, OFFSET0, JD);

			EphemElement ephem[] = new EphemElement[5];
			MoonEphemElement moon[] = new MoonEphemElement[5];
			for (int i = 0; i < 5; i++)
			{
				new_eph.targetBody = TARGET.URANUS;
				double light_time = ephem_ura.lightTime, light_time_new = light_time;
				do
				{
					light_time = light_time_new;
					if (eph.ephemType == COORDINATES_TYPE.GEOMETRIC) light_time = 0.0;

					double eq_1950[] = GUST86.GUST86_theory(JD - light_time, i + 1, 3);
					double eq[] = Precession.precess(Constant.J1950, Constant.J2000, eq_1950, eph);

					// Obtain position of satellite
					ephem[i] = MoonEphem.getBodyEphem(time, obs, new_eph, new double[] {eq[0], eq[1], eq[2], 0.0, 0.0, 0.0}, JD);

					light_time_new = ephem[i].lightTime;
					if (!eph.preferPrecisionInEphemerides || eph.ephemType == COORDINATES_TYPE.GEOMETRIC) break;
				} while (Math.abs(light_time_new - light_time) > (1.0E-3 / Constant.SECONDS_PER_DAY));

				moon[i] = MoonEphemElement.parseEphemElement(ephem[i]);
				new_eph.targetBody = TARGET.values()[TARGET.Miranda.ordinal() + i];
				moon[i] = MoonPhysicalParameters.physicalParameters(JD, ephem_sun, moon[i], obs, new_eph);
				moon[i] = MoonEphem.satellitePhenomena(moon[i], ephem_ura, TARGET.URANUS);
				moon[i].name = TARGET.values()[TARGET.Miranda.ordinal() + i].getName();

				// Obtain relative phenomena
				if (i == 4) {
					moon = MoonEphem.satellitesPhenomena(moon, ephem_ura.angularRadius);
					// Little trick with poor performance to obtain eclipse phenomena independently of
					// the mother object. Basically we go back to Earth to compute them from there.
					if (obs.getMotherBody() != TARGET.EARTH && moon.length > 0) {
						ObserverElement obs2 = obs.clone();
						obs2.forceObserverOnEarth();
						MoonEphemElement moon2[] = uranianSatellitesEphemerides_GUST86(time, obs2, eph);
						if (moon2.length > 0 && !Double.isInfinite(moon2[0].distance) && !Double.isNaN(moon2[0].distance)) {
							TimeElement time2 = time.clone();
							time2.add((moon2[0].distance-moon[0].distance)*Constant.LIGHT_TIME_DAYS_PER_AU);
							moon2 = uranianSatellitesEphemerides_GUST86(time2, obs2, eph);
						}
						String nom2[] = new String[moon2.length];
						for (int ii = 0; ii < moon2.length; ii++) {
							nom2[ii] = moon2[ii].name;
						}
						String nom[] = new String[moon.length];
						for (int ii = 0; ii < moon.length; ii++) {
							nom[ii] = moon[ii].name;
						}
						for (int i0 = 0; i0 < moon.length; i0++) {
							int ii = DataSet.getIndex(nom2, nom[i0]);
							if (ii >= 0) {
								moon[i0].eclipsed = moon2[ii].eclipsed;
								moon[i0].shadowTransiting = moon2[ii].shadowTransiting;
								//moon[i0].mutualPhenomena = moon2[ii].mutualPhenomena;
								moon[i0].xPositionFromSun = moon2[ii].xPositionFromSun;
								moon[i0].yPositionFromSun = moon2[ii].yPositionFromSun;
								moon[i0].zPositionFromSun = moon2[ii].zPositionFromSun;
							}
						}
					}
				}
			}

			return moon;
		} catch (JPARSECException ve)
		{
			throw ve;
		}

	}

	/**
	 * Obtain relative position of a satellite respect to it's mother planet.
	 * Based on code from the IMCCE.
	 *
	 * @param VP Geocentric equatorial coordinates (x, y, z) of the planet.
	 * @param VS Planetocentric equatorial coordinates (x, y, z) of the
	 *        satellite.
	 * @return Array with the offset in right ascension, declination, and distance.
	 */
	public static double[] relativePosition(double[] VP, double[] VS)
	{
		double D = Math.sqrt(VP[0] * VP[0] + VP[1] * VP[1] + VP[2] * VP[2]);
		double ALPHA = Math.atan2(VP[1], VP[0]);
		double DELTA = Math.asin(VP[2] / D);
		double C = Math.cos(ALPHA);
		double S = Math.sin(ALPHA);
		double X = VS[0] * C + VS[1] * S;
		double Y = VS[1] * C - VS[0] * S;
		C = Math.cos(DELTA);
		S = Math.sin(DELTA);
		double Z = VS[2] * C - X * S;
		double CD1 = Math.atan(Y / D);
		double CD2 = Math.atan(Z / D);

		return new double[] { CD1, CD2, Z};
	}

	// Perform adequate rotations for Jovian satellites theories E2x3, E5, and
	// Saturnian satellites by Dourneau.
	static double[] getSatellitePosition(double X, double Y, double Z, double I, double PHI, double ii, double OMEGA,
			double L0, double B0, double L0_sun, double B0_sun)
	{
		// Rotation towards Jupiter's orbital plane
		double A1 = X;
		double B1 = Y * cos_deg(I) - Z * sin_deg(I);
		double C1 = Y * sin_deg(I) + Z * cos_deg(I);

		// Rotation towards ascending node of Jupiter's orbit
		double A2 = A1 * cos_deg(PHI) - B1 * sin_deg(PHI);
		double B2 = A1 * sin_deg(PHI) + B1 * cos_deg(PHI);
		double C2 = C1;

		// Rotation towards plane of ecliptic
		double A3 = A2;
		double B3 = B2 * cos_deg(ii) - C2 * sin_deg(ii);
		double C3 = B2 * sin_deg(ii) + C2 * cos_deg(ii);

		// Rotation towards the vernal equinox
		double A4 = A3 * cos_deg(OMEGA) - B3 * sin_deg(OMEGA);
		double B4 = A3 * sin_deg(OMEGA) + B3 * cos_deg(OMEGA);
		double C4 = C3;

		// Rotate to the ecliptic location of the planet
		double A5 = A4 * sin_deg(L0) - B4 * cos_deg(L0);
		double B5 = A4 * cos_deg(L0) + B4 * sin_deg(L0);
		double C5 = C4;

		double A6 = A5;
		double B6 = C5 * sin_deg(B0) + B5 * cos_deg(B0);
		double C6 = C5 * cos_deg(B0) - B5 * sin_deg(B0);

		// Same from the Sun
		double A5_sun = A4 * sin_deg(L0_sun) - B4 * cos_deg(L0_sun);
		double B5_sun = A4 * cos_deg(L0_sun) + B4 * sin_deg(L0_sun);
		double C5_sun = C4;

		double A6_sun = A5_sun;
		double B6_sun = C5_sun * sin_deg(B0_sun) + B5_sun * cos_deg(B0_sun);
		double C6_sun = C5_sun * cos_deg(B0_sun) - B5_sun * sin_deg(B0_sun);

		return new double[]
		{ A6, B6, C6, A6_sun, B6_sun, C6_sun };
	}

	// Obtains apparent positions for Jovian satellites theories E2x3, E5, and
	// Saturnian satellites by Dourneau.
	static double[] getApparentPosition(TARGET planet, double[] sat_pos, double D, double DELTA, double D_sun,
			double DELTA_sun, double K, double R) throws JPARSECException
	{
		double x = sat_pos[0] * Math.cos(D) - sat_pos[2] * Math.sin(D);
		double y = sat_pos[0] * Math.sin(D) + sat_pos[2] * Math.cos(D);
		double z = sat_pos[1];
		double x_sun = sat_pos[3] * Math.cos(D_sun) - sat_pos[5] * Math.sin(D_sun);
		double y_sun = sat_pos[3] * Math.sin(D_sun) + sat_pos[5] * Math.cos(D_sun);
		double z_sun = sat_pos[4];

		double W = DELTA / (DELTA + z / (Constant.AU / planet.equatorialRadius));
		double W_sun = DELTA_sun / (DELTA_sun + z_sun / (Constant.AU / planet.equatorialRadius));

		// Project to the sky correcting differential light time
		x += (Math.abs(z) / K) * Math.sqrt(1.0 - squared(x / R));
		y *= W;
		x *= W;
		x_sun += (Math.abs(z_sun) / K) * Math.sqrt(1.0 - squared(x_sun / R));
		y_sun *= W_sun;
		x_sun *= W_sun;

		return new double[]	{ x, y, z, x_sun, y_sun, z_sun };
	}

	// Calculates satellites phenomena like transits, eclipses, etc.,
	// Calculations are respect to the mother planet
 	static MoonEphemElement satellitePhenomena(MoonEphemElement moon_obj, EphemElement ephem, TARGET planet)
			throws JPARSECException
	{
		MoonEphemElement moon = moon_obj.clone();

		// Obtain relative position
		LocationElement locEphem = new LocationElement(ephem.rightAscension, ephem.declination, ephem.distance);
		LocationElement locMoon = new LocationElement(moon.rightAscension, moon.declination, moon.distance);

		double planetVector[] = LocationElement.parseLocationElement(locEphem);
		double satelliteVector[] = LocationElement.parseLocationElement(locMoon);
		double dif[] = Functions.substract(planetVector, satelliteVector);
		double dp[] = MoonEphem.relativePosition(planetVector, dif);

		// Obtain relative position
		double pr = LocationElement.getAngularDistance(locMoon, locEphem) / ephem.angularRadius;
		double pz = (moon.distance - ephem.distance) * Math.cos(pr * ephem.angularRadius);
		double pang = Math.atan2(dp[0], -dp[1]);

		// From the Earth: rotate along the position angle of axis
		double ang = ephem.positionAngleOfAxis;
		double ppx = pr * Math.sin(pang + ang);
		double ppy = pr * Math.cos(pang + ang);
		double ppz = pz * Constant.AU / planet.equatorialRadius;

		moon.xPosition = ppx;
		moon.yPosition = ppy;
		moon.zPosition = ppz;

		// Obtain position from Sun. NEW ALGORITHM, NOT CLEAR, BUT SEEMS BETTER TO KEEP THE OLD ONE
/*		double subslat = ephem.subsolarLatitude;
		double diff = subslat - (float) Math.atan(Math.tan(subslat) / Math.pow(planet.polarRadius / planet.equatorialRadius, 2.0));
		subslat += diff;
		double dlon = -(ephem.subsolarLongitude - ephem.longitudeOfCentralMeridian);
		double incl_pole = ephem.positionAngleOfPole;
		diff = incl_pole - (float) Math.atan(Math.tan(incl_pole) / Math.pow(planet.polarRadius / planet.equatorialRadius, 2.0));
		incl_pole += diff;
		double dlat = -(subslat - incl_pole);
		if (planet.isPlanet() && planet.ordinal() >= TARGET.JUPITER.ordinal() && planet.ordinal() <= TARGET.NEPTUNE.ordinal()) {
			dlon = ephem.subsolarLongitude - ephem.longitudeOfCentralMeridianSystemIII;
		}
		double pos[] = MoonEphem.fromPlanetEquatorToFromOtherDirection(new double[] {ppx, ppy, ppz}, dlon, -dlat);
		moon.xPositionFromSun = pos[0];
		moon.yPositionFromSun = pos[1];
		moon.zPositionFromSun = pos[2];
*/

		// From the Sun: rotate to the bright limb angle, rotate along the
		// phase angle, and back
		ang = ephem.brightLimbAngle;
		ppx = pr * Math.sin(pang + ang);
		ppy = pr * Math.cos(pang + ang);

		double r = Math.sqrt(ppy * ppy + ppz * ppz);
		ang = Math.atan2(ppz, ppy) - Math.abs(ephem.phaseAngle);
		double y = r * Math.cos(ang);
		double z = r * Math.sin(ang);
		double x = -ppx;

		ang = ephem.positionAngleOfAxis - ephem.brightLimbAngle;
		pang = Math.atan2(-x, y);
		pr = Math.sqrt(x * x + y * y);
		ppx = pr * Math.sin(pang + ang);
		ppy = pr * Math.cos(pang + ang);
		ppz = z;

		moon.xPositionFromSun = ppx;
		moon.yPositionFromSun = ppy;
		moon.zPositionFromSun = ppz;



		// Check for events
		double flattening = planet.equatorialRadius / planet.polarRadius;
		double satSize = moon.angularRadius / ephem.angularRadius;
		double satSizeOccultation = -satSize;
		EVENT_DEFINITION ed = MoonEvent.getEventDefinition();
		if (ed == EVENT_DEFINITION.ENTIRE_SATELLITE) satSize = -satSize;
		if (ed == EVENT_DEFINITION.SATELLITE_CENTER || ed == EVENT_DEFINITION.AUTOMATIC) satSize = 0;

		// From the observer
		boolean inferior = (moon.zPosition <= 0.0);
		double Y1 = moon.yPosition * flattening;
		boolean withinDisc = (Math.sqrt(moon.xPosition * moon.xPosition + Y1 * Y1) <= (1.0 + satSize));
		boolean transiting = withinDisc && inferior;
		boolean withinDiscOcc = withinDisc;
		if (ed == EVENT_DEFINITION.AUTOMATIC_FOR_DRAWING) withinDiscOcc = (Math.sqrt(moon.xPosition * moon.xPosition + Y1 * Y1) <= (1.0 + satSizeOccultation));
		boolean occulted = withinDiscOcc && !inferior;

		moon.transiting = transiting;
		moon.occulted = occulted;
		moon.inferior = inferior;

		if (moon.occulted && Math.sqrt(moon.xPosition * moon.xPosition + Y1 * Y1) > (1.0 - satSize))
		{
			double occultedArea = MoonEphem.getOccultedArea(ephem.angularRadius, moon.angularRadius, Math.sqrt(moon.xPosition * moon.xPosition + Y1 * Y1) * ephem.angularRadius);
			occultedArea /= (Math.PI * Math.pow(moon.angularRadius, 2.0));
			occultedArea *= 100.0;
			if (occultedArea > 99.999) occultedArea = 100.0;
			if (occultedArea == 100.0) moon.magnitude = EphemElement.INVALID_MAGNITUDE;
			if (occultedArea > 0.0 && occultedArea <= 100.0)
			{
				if (!moon.mutualPhenomena.equals("")) moon.mutualPhenomena += ", ";
				moon.mutualPhenomena += Translate.translate(164)+" "+Translate.translate(161)+" " + planet.getName() + " ("+Functions.formatValue(occultedArea, 1)+"%)";
				double fractionVisible = 1.0 - occultedArea / 100.0;
				if (fractionVisible > 0.0) moon.magnitude -= Math.log10(fractionVisible) * 2.5;
			}
		} else {
			if (moon.occulted) moon.magnitude = EphemElement.INVALID_MAGNITUDE;
		}

		// From Sun
		boolean inferior_sun = (moon.zPositionFromSun <= 0.0);
		Y1 = moon.yPositionFromSun * flattening;
		boolean withinDisc_sun = (Math.sqrt(moon.xPositionFromSun * moon.xPositionFromSun + Y1 * Y1) < (1.0 + satSize));
		boolean withinDisc_sunOcc = withinDisc_sun;
		boolean eclipsed = withinDisc_sunOcc && !inferior_sun;
		boolean shadow_transiting = withinDisc_sunOcc && inferior_sun;
		if (ed == EVENT_DEFINITION.AUTOMATIC_FOR_DRAWING) {
			withinDisc_sunOcc = (Math.sqrt(moon.xPositionFromSun * moon.xPositionFromSun + Y1 * Y1) < (1.0 + satSizeOccultation));
			eclipsed = withinDisc_sunOcc && !inferior_sun;

			double satRadius = satSize * planet.equatorialRadius;
			double satPlanDistance = (Math.sqrt(moon.xPositionFromSun * moon.xPositionFromSun + moon.yPositionFromSun * moon.yPositionFromSun + moon.zPositionFromSun * moon.zPositionFromSun) - 1.0) * planet.equatorialRadius;
			double sun_size = FastMath.atan2_accurate(TARGET.SUN.equatorialRadius, ephem.distanceFromSun * Constant.AU);
			double shadow_cone_dist = satRadius / FastMath.tan(sun_size);
			double shadow_size = (satSize * (1.0 - 0.5 * satPlanDistance / shadow_cone_dist));
			double penumbra_size = 2 * (satSize-shadow_size);
			satSizeOccultation = -(shadow_size + penumbra_size);

			withinDisc_sunOcc = (Math.sqrt(moon.xPositionFromSun * moon.xPositionFromSun + Y1 * Y1) < (1.0 - satSizeOccultation));
			shadow_transiting = withinDisc_sunOcc && inferior_sun;
		}

		moon.eclipsed = eclipsed;
		moon.shadowTransiting = shadow_transiting;

		if (moon.eclipsed && Math.sqrt(moon.xPositionFromSun * moon.xPositionFromSun + Y1 * Y1) > (1.0 - satSize))
		{
			double occultedArea = MoonEphem.getOccultedArea(ephem.angularRadius, moon.angularRadius, Math.sqrt(moon.xPositionFromSun * moon.xPositionFromSun + Y1 * Y1) * ephem.angularRadius);
			occultedArea /= (Math.PI * Math.pow(moon.angularRadius, 2.0));
			occultedArea *= 100.0;
			if (occultedArea > 99.999) occultedArea = 100.0;
			if (occultedArea == 100.0) moon.magnitude = EphemElement.INVALID_MAGNITUDE;
			if (occultedArea > 0.0 && occultedArea <= 100.0)
			{
				if (!moon.mutualPhenomena.equals("")) moon.mutualPhenomena += ", ";
				moon.mutualPhenomena += Translate.translate(163)+" "+Translate.translate(161) + " " + planet.getName() + " ("+Functions.formatValue(occultedArea, 1)+"%)";
				double fractionVisible = 1.0 - occultedArea / 100.0;
				if (fractionVisible > 0.0) moon.magnitude -= Math.log10(fractionVisible) * 2.5;
			}
		} else {
			if (moon.eclipsed) moon.magnitude = EphemElement.INVALID_MAGNITUDE;
		}

		return moon;
	}

 	/**
 	 * Returns the occulted area of a disk of size r inside a disk of size R.
 	 * @param R Disk radius.
 	 * @param r Disk radius of the secondary body.
 	 * @param d Distance between both centers.
 	 * @return Occulted area. If r is greater than R, both values are flipped.
 	 */
	public static double getOccultedArea(double R, double r, double d)
	{
		if (R >= (r+d)) return Math.PI * r * r;
		double tmp = R;
		if (r > R) {
			R = r;
			r = tmp;
		}
		if (R >= (r+d)) return Math.PI * r * r;

		double a = r * r * Math.acos((d * d + r * r - R * R) / (2.0 * d * r));
		a += R * R * Math.acos((d * d + R * R - r * r) / (2.0 * d * R));
		a -= 0.5 * Math.sqrt((-d+r+R) * (d+r-R) * (d-r+R) * (d+r+R));
		return a;
	}

	// Calculates satellites phenomena like transits, eclipses, etc.
	// Calculations are respect to the other satellites (mutual)
	static MoonEphemElement[] satellitesPhenomena(MoonEphemElement[] moons_obj, double planet_angular_radius)
			throws JPARSECException
	{
		//double fl = 1.0 - TARGET.getFlatenningFactor(Target.getCentralBody(Target.getID(moons_obj[0].name)));
		MoonEphemElement moons[] = new MoonEphemElement[moons_obj.length];
		for (int i = 0; i < moons.length; i++)
		{
			moons[i] = moons_obj[i].clone();
		}

		for (int i = 0; i < (moons.length - 1); i++)
		{
			for (int j = i + 1; j < moons.length; j++)
			{
				double size = (moons[i].angularRadius + moons[j].angularRadius) / planet_angular_radius;
				double r = Math.sqrt(Math.pow(moons[i].xPosition - moons[j].xPosition, 2.0) + Math.pow(moons[i].yPosition - moons[j].yPosition, 2.0));
				double ri = Math.sqrt(Math.pow(moons[i].xPosition, 2.0) + Math.pow(moons[i].yPosition, 2.0));
				double rj = Math.sqrt(Math.pow(moons[j].xPosition, 2.0) + Math.pow(moons[j].yPosition, 2.0));

				int biggerSize = i, smallerSize = j;
				if (moons[biggerSize].angularRadius < moons[smallerSize].angularRadius) {
					int tmp = biggerSize;
					biggerSize = smallerSize;
					smallerSize = tmp;
				}
				EVENT_DEFINITION ed = MoonEvent.getEventDefinition();
				if (ed == EVENT_DEFINITION.ENTIRE_SATELLITE) size = (moons[biggerSize].angularRadius - moons[smallerSize].angularRadius) / planet_angular_radius;
				if (ed == EVENT_DEFINITION.SATELLITE_CENTER) size = moons[biggerSize].angularRadius / planet_angular_radius;

				if (r <= size && (moons[i].inferior || ri > 1.0) && (moons[j].inferior || rj > 1.0))
				{
					int body_behind = i;
					int body_infront = j;
					if (moons[i].zPosition < moons[j].zPosition)
					{
						body_behind = j;
						body_infront = i;
					}

					double occultedArea = MoonEphem.getOccultedArea(moons[body_infront].angularRadius, moons[body_behind].angularRadius, r * planet_angular_radius);
					occultedArea /= (Math.PI * Math.pow(moons[body_behind].angularRadius, 2.0));
					occultedArea *= 100.0;
					if (occultedArea > 99.999) occultedArea = 100.0;

						if (!moons[body_behind].mutualPhenomena.equals("")) moons[body_behind].mutualPhenomena += ", ";
						moons[body_behind].mutualPhenomena += Translate.translate(164)+" "+Translate.translate(161) + " " + moons[body_infront].name + " ("+Functions.formatValue(occultedArea, 1)+"%) (d="+Functions.formatAngleAsDegrees(r * planet_angular_radius, 8)+"\u00b0)";
						double fractionVisible = 1.0 - occultedArea / 100.0;
						if (fractionVisible == 0.0)
						{
							moons[body_behind].magnitude = EphemElement.INVALID_MAGNITUDE;
						} else {
							moons[body_behind].magnitude -= Math.log10(fractionVisible) * 2.5;
						}
				}

				// Now the events as seen from the Sun. Note we are following a formulation which we have a
				// ficticious Sun at the distance of the Earth. Angular radii are those from Earth, not from Sun.
				r = Math.sqrt(Math.pow(moons[i].xPositionFromSun - moons[j].xPositionFromSun, 2.0) + Math.pow(moons[i].yPositionFromSun - moons[j].yPositionFromSun, 2.0));
				ri = Math.sqrt(Math.pow(moons[i].xPositionFromSun, 2.0) + Math.pow(moons[i].yPositionFromSun, 2.0));
				rj = Math.sqrt(Math.pow(moons[j].xPositionFromSun, 2.0) + Math.pow(moons[j].yPositionFromSun, 2.0));

				int body_behind = i;
				int body_infront = j;
				double planet_angular_radius_fromSat = ri;
				if (moons[i].zPositionFromSun < moons[j].zPositionFromSun)
				{
					body_behind = j;
					body_infront = i;
					planet_angular_radius_fromSat = rj;
				}

				//double solar_angular_radius_fromSat = Math.atan(TARGET.SUN.equatorialRadius / (moons[body_behind].distanceFromSun * Constant.AU));
				planet_angular_radius_fromSat = Math.atan(1.0 / planet_angular_radius_fromSat);
//				size -= solar_angular_radius_fromSat / planet_angular_radius_fromSat; // An event that happens 'from somewhere in Sun's disk'
				if (r <= size && (moons[i].inferior || ri > 1.0) && (moons[j].inferior || rj > 1.0))
				{
					double occultedArea = MoonEphem.getOccultedArea(moons[body_infront].angularRadius, moons[body_behind].angularRadius, r * planet_angular_radius);
					occultedArea /= (Math.PI * Math.pow(moons[body_behind].angularRadius, 2.0));
					occultedArea *= 100.0;
					if (occultedArea > 99.999) occultedArea = 100.0;

					if (!moons[body_behind].mutualPhenomena.equals("")) moons[body_behind].mutualPhenomena += ", ";
						moons[body_behind].mutualPhenomena += Translate.translate(163)+" "+Translate.translate(161) + " " + moons[body_infront].name + " ("+Functions.formatValue(occultedArea, 1)+"%) (d="+Functions.formatAngleAsDegrees(r * planet_angular_radius, 8)+"\u00b0)";

						double fractionVisible = 1.0 - occultedArea / 100.0;
						if (fractionVisible == 0.0)
						{
							moons[body_behind].magnitude = EphemElement.INVALID_MAGNITUDE;
						} else {
							if (moons[body_behind].magnitude != EphemElement.INVALID_MAGNITUDE) moons[body_behind].magnitude -= Math.log10(fractionVisible) * 2.5;
						}
				}
			}
		}
		return moons;
	}

	/**
	 * Obtain position of Triton, the moon of Neptune.
	 * <P>
	 * Page 373 of the Explanatory Supplement of the Astronomical Almanac.
	 * <P>
	 * Code taken from Guide software.
	 *
	 * @param time Time object containing the date.
	 * @param obs Observer object containing the observer position.
	 * @param eph Ephemeris object with the target and ephemeris
	 *        properties.
	 * @return Object with the ephemeris.
	 * @throws JPARSECException If the calculation fails.
	 * @deprecated Better ephemerides for Triton can be obtained
	 * using elliptic elements from JPL.
	 */
	public static MoonEphemElement triton(TimeElement time, ObserverElement obs,
			EphemerisElement eph) throws JPARSECException
	{
		try
		{
			// Obtain ephemeris of Neptune
			EphemerisElement new_eph = eph.clone();
			new_eph.targetBody = TARGET.NEPTUNE;
			EphemElement ephem = PlanetEphem.MoshierEphemeris(time, obs, new_eph);

			double jd = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
			if (eph.ephemType != EphemerisElement.COORDINATES_TYPE.GEOMETRIC)
				jd = jd - ephem.lightTime;

			double t_cent = Functions.toCenturies(jd);
			double n = (359.28 + 54.308 * t_cent) * Constant.DEG_TO_RAD;
			double t0 = 2433282.5;
			double theta = (151.401 + .57806 * (jd - t0) / 365.25) * Constant.DEG_TO_RAD;

			/* Semimajor axis is 488.49 arcseconds at one AU, so semimajor is in AU */
			double semimajor = 488.49 * Constant.DEG_TO_RAD / 3600.;
			double longitude = (200.913 + 61.2588532 * (jd - t0)) * Constant.DEG_TO_RAD;
			double gamma = 158.996 * Constant.DEG_TO_RAD;

			/* Calculate longitude and latitude on invariable plane: */
			double lon_on_ip = theta + Math.atan2(Math.sin(longitude) * Math.cos(gamma), Math.cos(longitude));
			double lat_on_ip = Math.asin(Math.sin(longitude) * Math.sin(gamma));

			/* Vector defining Triton position in invariable plane space: */
			double triton[] = LocationElement.parseLocationElement(new LocationElement(lon_on_ip, lat_on_ip, 1.0));

			/* This position of north pole is prior to IAU2000 recomendations.
			 * It seems better to maintain as it was defined in AA */
			double ra = 298.72 * Constant.DEG_TO_RAD + 2.58 * Constant.DEG_TO_RAD * Math.sin(n) - 0.04 * Constant.DEG_TO_RAD * Math
					.sin(n + n);
			double dec = 42.63 * Constant.DEG_TO_RAD - 1.90 * Constant.DEG_TO_RAD * Math.cos(n) + 0.01 * Constant.DEG_TO_RAD * Math
					.cos(n + n);

			double pole[] = Precession.precessFromJ2000(Constant.B1950, LocationElement
					.parseLocationElement(new LocationElement(ra, dec, 1.0)), eph);

			LocationElement loc = LocationElement.parseRectangularCoordinates(pole);
			/* Vectors defining invariable plane, expressed in B1950: */
			double x_axis[] = LocationElement.parseLocationElement(new LocationElement(loc.getLongitude() + Math.PI,
					loc.getLatitude(), 1.0));
			double y_axis[] = LocationElement.parseLocationElement(new LocationElement(loc.getLongitude() + Math.PI,
					loc.getLatitude(), 1.0));
			double z_axis[] = LocationElement.parseLocationElement(new LocationElement(loc.getLongitude(), loc
					.getLatitude(), 1.0));

			/* Obtain position of Triton refered to B1950 */
			double vect_1950[] = new double[3];
			for (int i = 0; i < 3; i++)
			{
				vect_1950[i] = semimajor * (x_axis[i] * triton[0] + y_axis[i] * triton[1] + z_axis[i] * triton[2]);
			}

			/* Precess to date */
			double results[] = Precession.FK4_B1950ToFK5_J2000(vect_1950);
			results = Precession.precessFromJ2000(jd, vect_1950, eph);

			// Obtain equatorial and horizontal coordinates
			LocationElement sat_loc = new LocationElement(ephem.rightAscension, ephem.declination, ephem.distance);
			double pos[] = LocationElement.parseLocationElement(sat_loc);
			double sat_pos[] = Functions.sumVectors(pos, results);
			sat_loc = LocationElement.parseRectangularCoordinates(sat_pos);
			LocationElement hor_loc = CoordinateSystem.equatorialToHorizontal(sat_loc, time, obs, eph);

			// Set results of ephemeris
			String nom = TARGET.Triton.getName();
			ra = sat_loc.getLongitude();
			dec = sat_loc.getLatitude();
			double dist = sat_loc.getRadius();
			double azi = hor_loc.getLongitude();
			double ele = hor_loc.getLatitude();
			double ill = ephem.phase;
			double elo = ephem.elongation;

			// From the observer
			double X = results[0];
			double Y = results[1];
			double Z = results[2];
			double flattening = TARGET.NEPTUNE.equatorialRadius / TARGET.NEPTUNE.polarRadius;
			double x_sun = results[0];
			double y_sun = results[1];
			double z_sun = results[2];

			boolean inferior = (Z <= 0.0);
			double Y1 = Y * flattening;
			boolean withinDisc = (Math.sqrt(X * X + Y1 * Y1) < 1.0);
			boolean transiting = withinDisc && inferior;
			boolean occulted = withinDisc && !inferior;

			// From Sun
			boolean inferior_sun = (z_sun <= 0.0);
			Y1 = y_sun * flattening;
			boolean withinDisc_sun = (Math.sqrt(x_sun * x_sun + Y1 * Y1) < 1.0);
			boolean eclipsed = withinDisc_sun && !inferior_sun;
			boolean shadow_transiting = withinDisc_sun && inferior_sun;

			// Create ephemeris object
			MoonEphemElement moon = new MoonEphemElement(nom, ra, dec, dist,
					ephem.distanceFromSun + (dist - ephem.distance), azi, ele, (float) ill, (float) elo, eclipsed, occulted,
					transiting, shadow_transiting, inferior, X, Y, Z, x_sun, y_sun, z_sun);

			return moon;

		} catch (JPARSECException ve)
		{
			throw ve;
		}
	}

	/**
	 * Compute positions of minor satellites. From Guide software.
	 *
	 * @param jd Julian day in dynamical time.
	 * @param orbit Orbital elements of the satellite.
	 * @param ephem_method Ephemeris method to apply.
	 * @return Array with x, y, z coordinates, planetocentric mean equinox and
	 *         equator J2000, in AU.
	 * @throws JPARSECException If an error occurs.
	 */
	private static double[] rocks(double jd, MoonOrbitalElement orbit, EphemerisElement.REDUCTION_METHOD ephem_method)
	throws JPARSECException {
		double output_vect[] = new double[] { 0.0, 0.0, 0.0 };

		double dt = jd - orbit.referenceTime;
		double mean_lon = orbit.meanLongitude + dt * orbit.meanMotion;
		double avect[] = new double[3], bvect[] = new double[3], cvect[] = new double[3];
		double h, k, p, q, tsin, tcos, r, e, omega, true_lon;
		double a_fraction, b_fraction, c_fraction, dot_prod;

		if (orbit.referencePlane == MoonOrbitalElement.REFERENCE_PLANE.LAPLACE)
		{
			/* avect is at right angles to Laplacian pole, */
			/* but in plane of the J2000 equator: */
			avect[0] = -Math.sin(orbit.LaplacePoleRA);
			avect[1] = Math.cos(orbit.LaplacePoleRA);
			avect[2] = 0.;

			/* bvect is at right angles to Laplacian pole */
			/* _and_ to avect: */
			tsin = Math.sin(orbit.LaplacePoleDEC);
			tcos = Math.cos(orbit.LaplacePoleDEC);
			bvect[0] = -avect[1] * tsin;
			bvect[1] = avect[0] * tsin;
			bvect[2] = tcos;

			/* cvect is the Laplacian pole vector: */
			cvect[0] = avect[1] * tcos;
			cvect[1] = -avect[0] * tcos;
			cvect[2] = tsin;
		}

		/* Rotate the (h, k) vector to account for */
		/* a constant apsidal motion: */
		tsin = Math.sin(dt * orbit.argumentOfPeriapsisPrecessionRate);
		tcos = Math.cos(dt * orbit.argumentOfPeriapsisPrecessionRate);
		double sat_h = orbit.eccentricity * Math.sin(orbit.periapsisLongitude);
		double sat_k = orbit.eccentricity * Math.cos(orbit.periapsisLongitude);
		h = sat_k * tsin + sat_h * tcos;
		k = sat_k * tcos - sat_h * tsin;

		/* I'm sure there's a better way to do this... */
		/* all I do here is to compute the eccentricity */
		/* and omega, a.k.a. longitude of perihelion, */
		/* and do a first-order correction to get the */
		/* 'actual' r and true longitude values. */
		e = Math.sqrt(h * h + k * k);
		omega = 0.0;
		if (h != 0.0 || k != 0.0) omega = Math.atan2(h, k);
		true_lon = mean_lon + 2. * e * Math.sin(mean_lon - omega) + 1.25 * e * e * Math.sin(2. * (mean_lon - omega));
		r = orbit.semimajorAxis * (1. - e * e) / (1 + e * Math.cos(true_lon - omega));

		/* Just as we rotated (h,k), we gotta rotate */
		/* the (p,q) vector to account for precession */
		/* in the Laplacian plane: */
		tsin = Math.sin(dt * orbit.ascendingNodePrecessionRate);
		tcos = Math.cos(dt * orbit.ascendingNodePrecessionRate);
		double sat_p = Math.tan(orbit.inclination * 0.5) * Math.sin(orbit.ascendingNodeLongitude);
		double sat_q = Math.tan(orbit.inclination * 0.5) * Math.cos(orbit.ascendingNodeLongitude);
		p = sat_q * tsin + sat_p * tcos;
		q = sat_q * tcos - sat_p * tsin;

		/* Now we evaluate the position in components */
		/* along avect, bvect, cvect. I derived the */
		/* formulae from scratch... sorry I can't */
		/* give references: */
		tsin = Math.sin(true_lon);
		tcos = Math.cos(true_lon);
		dot_prod = 2. * (q * tsin - p * tcos) / (1. + p * p + q * q);
		a_fraction = tcos + p * dot_prod;
		b_fraction = tsin - q * dot_prod;
		c_fraction = dot_prod;

		double pos[] = new double[] {a_fraction * r, b_fraction * r, c_fraction * r};

		// This method from IMCCE should not be used with incl > 90 deg.
		// It gives the same output to the mas.
//		TARGET t = Target.getID(orbit.name);
//		pos = ELEM2PV(t.relativeMass > 0.0 ? (1.0 / t.relativeMass) : 0.0,
//				new double[] {0.0, orbit.semimajorAxis, mean_lon, k, h, q, p});

		if (orbit.referencePlane != MoonOrbitalElement.REFERENCE_PLANE.LAPLACE) {
			if (orbit.referencePlane == REFERENCE_PLANE.EQUATOR ||
					orbit.referencePlane == REFERENCE_PLANE.PLANET_EQUATOR) {
				EphemerisElement eph = new EphemerisElement();
				eph.equinox = orbit.referenceTime;
				eph.ephemMethod = ephem_method;
				eph.targetBody = orbit.centralBody;
				LocationElement locAxis = PhysicalParameters.getPlanetaryAxisDirection(jd, eph);
				if (locAxis == null) return null;
				double SA = Math.sin(locAxis.getLongitude());
				double CA = Math.cos(locAxis.getLongitude());
				double SD = Math.sin(locAxis.getLatitude());
				double CD = Math.cos(locAxis.getLatitude());
				if (orbit.referencePlane == REFERENCE_PLANE.EQUATOR) {
					SA = -SA;
					CA = -CA;
					SD = -SD;
				}

				double TRANS[][] = new double[3][3];
				TRANS[0][0] = SA;
				TRANS[1][0] = -CA;
				TRANS[2][0] = 0.0;
				TRANS[0][1] = CA * SD;
				TRANS[1][1] = SA * SD;
				TRANS[2][1] = -CD;
				TRANS[0][2] = CA * CD;
				TRANS[1][2] = SA * CD;
				TRANS[2][2] = SD;
				double out[] = new double[3];
				for (int IV = 0; IV < 3; IV++)
				{
					out[IV] = 0.0;
					for (int J = 0; J < 3; J++)
					{
						out[IV] += TRANS[IV][J] * pos[J];
					}
				}
				if (orbit.referencePlane == REFERENCE_PLANE.PLANET_EQUATOR) {
					out[0] = -out[0];
					out[1] = -out[1];
					out[2] = -out[2];
				}
				out = Precession.precessToJ2000(orbit.referenceTime, out, eph);
				return out;
			}
			EphemerisElement eph = new EphemerisElement();
			eph.equinox = orbit.referenceTime;
			eph.ephemMethod = ephem_method;
			eph.targetBody = orbit.centralBody;
			pos = Ephem.eclipticToEquatorial(pos, jd, eph);
			pos = Precession.precessToJ2000(orbit.referenceTime, pos, eph);
			return pos;
		}

		/* Now that we've got components on each axis, */
		/* the remainder is trivial: */
		for (int i = 0; i < 3; i++)
		{
			output_vect[i] = (pos[0] * avect[i] + pos[1] * bvect[i] + pos[2] * cvect[i]);
		}

		return output_vect;
	}

	protected static double[] getRocks(double jd, MoonOrbitalElement orbit, EphemerisElement.REDUCTION_METHOD ephem_method)
			throws JPARSECException {
		return rocks(jd, orbit, ephem_method);
	}

	private static double squared(double a) {
		return a * a;
	}

	private static double sin_deg(double deg) {
		return Math.sin(deg * Constant.DEG_TO_RAD);
	}

	private static double cos_deg(double deg) {
		return Math.cos(deg * Constant.DEG_TO_RAD);
	}

	/**
	 * Default path to JPL natural satellites file.
	 */
	public static final String PATH_TO_JPL_SATELLITES_FILE = FileIO.DATA_ORBITAL_ELEMENTS_DIRECTORY + "JPL_natural_satellites.txt";

	// Orbits of natural satellites respect to local Laplace plane. Format is:
	// Name, central body ID, epoch JD, a (AU), e, periapsis long (rad), mean lon, incl, ascending node long, mean motion (rad/day), apsis rate, node rate (rad/day), Laplace pole RA, DEC
	// These orbits corresponds to values from Guide later updated using JPL elements
	// DATA FOR YEAR 2012
	//private static final int YEAR = 2012;
	private static String[] ORBITS = new String[] {
		"Phobos 4 2433282.5 6.267468886215952E-5 0.0151 6.245503648629029 7.8347830122025455 0.01876228945893904 3.6265149329638975 19.70205775177998 0.007600929585966103 0.007605970658778125 5.544404888102907 0.9231570012573608",
		"Deimos 4 2433282.5 1.568070447236069E-4 2.0E-4 4.978621504483904 10.656683706704538 0.031206487025658612 0.4280419990516093 4.977013689723675 3.142534761942412E-4 3.15428396638566E-4 5.526707249487684 0.9342572953000448",
		"Io 5 2450464.5 0.002819558848342458 0.0041 2.235871491559856 8.205264052523383 6.283185307179586E-4 0.7675434451495462 3.5515523137884433 0.005293053488910303 0.002318385962123785 4.67847723301844 1.125650101073743",
		"Europa 5 2450464.5 0.004486026418024238 0.0094 5.37694054637405 8.361732819964672 0.008133234314293577 3.8241211108746955 1.7693227155911746 0.006170166369784249 5.699186270526929E-4 4.6789484719164784 1.1258420872914623",
		"Ganymede 5 2450464.5 0.0071551820561066085 0.0013 4.467501833037366 10.009620339820161 0.0030892327760299633 1.1091916462274363 0.8782079173652049 1.3534771466866893E-4 1.296788927507537E-4 4.680414548488153 1.1264878591147003",
		"Callisto 5 2450464.5 0.012585072175851935 0.0074 6.134675241127389 9.300842130585263 0.003351032163829113 5.215881563000014 0.3764862435473923 4.180418915907286E-5 5.077157145079536E-5 4.688635049265047 1.1300832373738083",
		"Amalthea 5 2450464.5 0.0012125841040524464 0.0032 4.621963471838864 7.854208526777242 0.0066322511575784525 1.9014664068777423 12.61230269386599 0.043883734283057356 0.04377207083704449 4.67847723301844 1.125650101073743",
		"Thebe 5 2450464.5 0.0014833098825206058 0.0176 8.20240171255011 10.575281550391521 0.01884955592153876 4.113636327195515 9.314826699899257 0.021611085224822215 0.021583969685016916 4.67847723301844 1.125650101073743",
		"Adrastea 5 2450464.5 8.623117388245072E-4 0.0018 9.711448290409448 12.079388846467715 9.424777960769379E-4 3.98594803911961 21.066103239321606 0.1482967572324007 0.1482967572324007 4.67847723301844 1.1256675543662626",
		"Metis 5 2450464.5 8.556271517018366E-4 0.0012 7.750815221889098 12.568744262141886 3.316125578789226E-4 2.5640981106899097 21.314913743710413 0.150898454727706 0.14958629425181288 4.67847723301844 1.1256675543662626",
		"Himalia 5 2451544.5 0.07661205301292773 0.1623 6.793519580462728 7.992927295725753 0.4798957311283608 0.999113730304154 0.025076398509805465 6.270933157975534E-5 6.0807436687728833E-5 4.811296789095208 1.1768580613272566",
		"Elara 5 2451544.5 0.07848373740727549 0.2174 4.415054689014936 10.226337873040295 0.4647288199285301 1.9089189627837582 0.02419984703556411 6.803679733807343E-5 6.813110950516251E-5 4.768239516448508 1.1911174013160502",
		"Pasiphae 5 2451544.5 0.1579166861859702 0.409 8.437619735841388 13.327910126881857 2.6429695395875332 5.462706025817052 0.008449325659134513 1.0641113348359819E-4 2.1234938697640393E-4 4.772428306653294 1.1806279725115643",
		"Sinope 5 2451544.5 0.16002233112961145 0.2495 11.335477159390173 14.274559259871062 2.7595226270357145 5.289761350236934 0.008279334080648768 9.635053119165724E-5 1.9650929676671787E-4 4.769391433754825 1.1672762037338076",
		"Lysithea 5 2451544.5 0.0783233073163314 0.1124 0.9601754346921604 6.704420522148417 0.49396308489943513 0.09648180105024654 0.024240527169769592 6.539850912012805E-5 5.736435853994426E-5 4.736055645041732 1.1713777274759942",
		"Carme 5 2451544.5 0.1564460770189827 0.2533 2.4772679804031914 6.56180966896796 2.8781701095862893 1.9851025846333106 0.008558237695117462 1.061092020661145E-4 1.9431180208921814E-4 4.758116606786941 1.1639426248624982",
		"Ananke 5 2451544.5 0.14222127562193965 0.2435 1.8890396626035426 6.231296668517795 2.5986032700018376 0.1329068225393682 0.00997694428556432 6.106646730194704E-5 1.5787833919748975E-4 4.870987049513414 1.154570206779289",
		"Leda 5 2451544.5 0.07463341522461722 0.1636 8.543142342416964 12.52381948719555 0.4792150527200831 3.7897555779029273 0.026079578857266764 6.0329746226269495E-5 5.798700141225135E-5 4.741501072307956 1.1667176983731693",
		"Mimas 6 2451545.0 0.00124025161015318 0.0196 8.823093154436854 9.082239641772974 0.027471482426390748 3.0198908448482285 6.667061658752358 0.01744667732145891 0.01744667732145891 0.7084116900919784 1.4579782439459832",
		"Enceladus 6 2451545.0 0.0015912124878547545 0.0 5.979201311359734 9.464379481497131 5.235987755982989E-5 5.977874861128218 4.585536666623247 0.007264537094154766 0.007289162643626476 0.7083593302144185 1.4579782439459832",
		"Tethys 6 2451545.0 0.0019697606566115907 1.0E-4 5.3240221634535825 9.571577604154623 0.019041542139258134 4.5350984349671055 3.328306421879783 0.0034543019756944743 0.003452915262737552 0.7082197038742591 1.457995697238503",
		"Dione 6 2451545.0 0.0025228634489027238 0.0022 10.03093080998701 15.654940165273379 4.886921905584122E-4 5.0686979471793325 2.2957176220975697 0.0014697901434516816 0.0014691625108001099 0.707626291928581 1.458048057116063",
		"Rhea 6 2451545.0 0.003523231965571747 2.0E-4 10.343885798162113 13.481656180690038 0.005811946409141118 6.126838712785934 1.3908536820204078 4.79469977115739E-4 4.8008550566416846E-4 0.7038563807442733 1.4583796696739417",
		"Titan 6 2451545.0 0.00816766304464191 0.0288 3.640617193320012 6.490914394751951 0.005340707511102648 0.4897393881096089 0.39404255936242233 2.442693377109861E-5 2.4414453362132392E-5 0.6320535353172265 1.4651864537567196",
		"Hyperion 6 2451545.0 0.010033117403791349 0.0232 9.896453191120846 11.40340537387779 0.010733774899765127 4.604998871509478 0.2953088420088023 6.720221829423582E-5 1.1122736220715428E-4 0.6348111555353776 1.4636680173074845",
		"Iapetus 6 2451545.0 0.02380283879411009 0.0293 6.15596825800172 9.677850702308557 0.14482742133048945 1.415549289830001 0.0792020221832195 5.129876076960704E-6 5.002551476550495E-6 4.969214179815655 1.3744293326530146",
		"Phoebe 6 2451545.0 0.08655148592819485 0.1634 10.185497168543629 11.111184897216381 3.058567341072423 4.20774448046305 0.011465266823885479 1.8660964851773067E-5 2.378752414917445E-5 4.8163058840484325 1.177311846932775",
		// I use the old data for Janus and Epimetheus since testing against Horizons shows old elements are better than new ones from JPL
		// In the rest of cases new elements are much better.
		//"Janus 6 2451545.0 0.0010123807197284622 0.0098 2.9703234940915895 3.2729984929724463 0.0028797932657906437 2.6908613742622576 9.046794645348127 0.035838382997830175 -0.03568967601443669 0.7083418769218986 1.458013150531023",
		//"Epimetheus 6 2451545.0 0.0010123807197284622 0.0161 4.917238274691265 6.3200815675667465 0.006161012259539983 3.3643315727293097 9.042515382510903 0.035838382997830175 0.03568967601443669 0.7083418769218986 1.458013150531023",
		"Janus 6 2444786.5 0.0010123617668799697 0.006583150064048162 -0.5349562582333303 3.479557783325707 0.0029413527386594573 -0.9669975098091169 9.045986333897599 0.03586418617334444 -0.035699436362576734 0.7082545868055182 1.4580480492940784",
		"Epimetheus 6 2444786.5 0.0010120410795321257 0.01256369659694896 1.4358949827638827 2.511338189508377 0.005685814895946847 -0.44056820350578785 9.050298786940205 0.035875089189336795 -0.03570222586418812 0.7082545868055182 1.4580480492940784",
		"Helene 6 2451545.0 0.0025230573019292815 0.0 3.425138843868792 4.178876734635063 0.0037175513067479217 2.846841449512991 2.2957176203522405 0.00335722557356723 0.0014695390260514678 0.707626291928581 1.458048057116063",
		"Telesto 6 2451545.0 0.0019700815167934785 2.0E-4 6.079278490669089 10.619874712779977 0.020594885173533087 3.999980486305644 3.328306386973198 0.003455689802924565 0.003452915262737552 0.7082197038742591 1.457995697238503",
		"Calypso 6 2451545.0 0.0019700882013806015 5.0E-4 5.789187315695111 8.523420121869428 0.026179938779914945 5.484278295371702 3.328306386973198 0.003458468805580716 0.0034522223236922505 0.7082197038742591 1.457995697238503",
		"Atlas 6 2451545.0 9.20962306238819E-4 0.0011 7.806386505272598 12.750590116907174 5.235987755982989E-5 4.126342324150034 10.442562359473389 0.05029948490923533 0.050007046043483966 0.7083418769218986 1.458013150531023",
		"Prometheus 6 2451545.0 9.320252979268389E-4 0.0022 6.225414908938574 7.916394608025801 1.2217304763960306E-4 5.570672093345421 10.25007459870417 0.048051463237314206 0.047917615150302184 0.7083418769218986 1.458013150531023",
		"Pandora 6 2451545.0 9.479412998659176E-4 0.0042 3.7266270188582924 5.910243352613438 8.726646259971648E-4 2.570381295997089 9.997034341233032 0.04526953641831181 0.045150718737423844 0.7083418769218986 1.458013150531023",
		"Pan 6 2451545.0 8.929605707819519E-4 0.0 2.7123638306468276 8.841733270848154 0.0 0.9088976612685671 10.92631498201466 0.05585202545116391 0.055671274559736195 0.7083069703368587 1.458013150531023",
		"Methone 6 2451545.0 0.0012994971058214097 0.0 7.822530800853546 9.065013242055787 2.2689280275926284E-4 5.47268930913846 6.223755540642898 0.03440484767791697 0.01485528828925603 0.7084116900919784 1.4579782439459832",
		"Pallene 6 2451545.0 0.00141901752357476 0.0040 2.4304407965721837 8.647809737659063 1.7453292519943296E-5 2.149896572606615 5.445899791428006 0.009978204082922553 0.00905867500735044 0.708237157166779 1.457995697238503",
		"Polydeuces 6 2451545.0 0.0025215733235880483 0.0191 6.628463793101624 8.1871649881802 0.0030543261909900766 1.1857068806348678 2.2957176203522405 0.0014801603716192123 0.001470921234626634 0.707608838636061 1.458048057116063",
		"Daphnis 6 2451545.0 9.124728805930273E-4 0.0 6.9777914428882895 8.963801598732637 5.235987755982989E-5 2.318966617247306 10.576331386880549 0.05181452963541711 0.05150426299089366 0.7083069703368587 1.458013150531023",
		"Anthe 6 2451545.0 0.0013161149894083688 0.0011 7.44826239605588 10.77264338220704 2.617993877991494E-4 5.023965158450717 6.0625532919172125 0.013940375882462306 0.013963006362790978 0.7083069703368587 1.457995697238503",
		"Aegaeon 6 2451545.0 0.0011191669990131248 2.0E-4 8.204914986672982 13.8383316666276 1.7453292519943296E-5 5.536219293911053 7.775326934827384 0.025076419590318488 0.025003523021741983 0.7083244236293788 1.457995697238503",
		"Cordelia 7 2446450.0 3.3257681763970595E-4 2.5380901545878906E-4 3.0073479654800193 1.2217681069211517 0.0013854216102467487 0.5642706142248847 18.75389341941029 0.026220481699683867 -0.02618395571564411 1.3493767223712083 0.2648442511170915",
		"Ophelia 7 2446450.0 3.5944604322500206E-4 0.010002052272752707 -3.120092394198527 5.202435493851493 7.21697435525152E-4 -3.0855667844375843 16.69265413861426 0.019976186951861562 -0.019952384739476872 1.3493767223712083 0.2648442511170915",
		"Bianca 7 2446450.0 3.955004050503375E-4 8.700029119494172E-4 1.7975955380483486 4.188780121505055 0.0031811581530430334 1.5306766348915604 14.45809677715721 0.014276058836267192 -0.014262253414483915 1.3493767223712083 0.2648442511170915",
		"Cressida 7 2446450.0 4.128869779759953E-4 3.719330094143247E-4 2.79016818005877 0.3044258508071657 2.0827936918116582E-4 -2.148984563066767 13.553919675051443 0.01227838643593449 -0.012267302945715125 1.3493767223712083 0.2648442511170915",
		"Desdemona 7 2446450.0 4.188430257589893E-4 9.676820674809028E-5 2.4565334159171077 5.480490475609905 0.0017377780535808311 -1.23556197716936 13.265470803841449 0.01167712380711475 -0.011666921819178307 1.3493767223712083 0.2648442511170915",
		"Juliet 7 2446450.0 4.3020654147831247E-4 6.361693005836747E-4 1.1203467190763194 5.387152173217216 7.039470740043897E-4 3.1359675607711486 12.743104340594764 0.010632340161244874 -0.01062350127259412 1.3493767223712083 0.2648442511170915",
		"Portia 7 2446450.0 4.4183108930733784E-4 1.8741661373991525E-5 -2.1571419254190074 5.948425776694432 0.001235971676796694 -1.6242096786018336 12.243248261521641 0.009684851213620145 -0.009677242856785595 1.3493767223712083 0.2648442511170915",
		"Rosalind 7 2446450.0 4.674331702841975E-4 2.64900010614682E-4 -3.1362952470024466 5.053105558222829 0.002776103510217616 0.10287101816861277 11.250922858909426 0.007952883752589877 -0.007947371850883332 1.3493767223712083 0.2648442511170915",
		"Belinda 7 2446450.0 5.030552416158751E-4 1.4529753483760174E-4 -0.8943985849793581 5.566969062962514 5.917798698454599E-4 -0.9010358690609218 10.07683803858385 0.00615338793681825 -0.006149658143857638 1.3493767223712083 0.2648442511170915",
		"Puck 7 2446450.0 5.749079535998969E-4 5.018154316867449E-5 0.5005164248215471 5.788091388725543 0.005848846388301901 -1.6236874977154485 8.247458413410449 0.0038663118736249422 -0.0038647605506632794 1.3493767223712083 0.2648442511170915",
		"Perdita 7 2453243.0 5.163691897340136E-4 0.01440701109067305 1.3064519971794253 0.627472126785817 0.009476346121648589 -0.9973006123124718 9.84794693337463 0.01093973248703778 -0.0019069267669350296 1.3493767223712083 0.2648442511170915",
		"Mab 7 2453243.0 6.534562757019648E-4 0.004591779670058372 -1.5339279720017234 2.689852932363044 0.002517753596429204 -0.566800832526093 6.80764047151097 0.003083926606693281 -0.002896630735547899 1.3493767223712083 0.2648442511170915",
		"Cupid 7 2453243.0 4.980228040953151E-4 0.0021124843056036196 1.0550534644365261 4.087556992923546 0.00223970105736972 -2.894149863987594 10.252817074693283 0.0060462334920823105 -0.005199975424700547 1.3493767223712083 0.2648442511170915",
		"Triton 8 2451545.0 0.002371417443051499 0.0 4.254240051736178 10.402284514933845 2.7378107311409052 3.0998443778820888 1.069140944072733 2.2261535983495765E-5 2.502367289788359E-5 5.2264931648521396 0.7577172414608182",
		"Nereid 8 2451545.0 0.03685759679954935 0.7507 10.763218604246271 14.545207466977825 0.12374384396639797 5.856801370917371 0.017446792913808868 1.0630000703803697E-6 1.8192592046260295E-6 4.7002065822057695 1.2063192191009209",
		"Naiad 8 2451545.0 3.223775831650349E-4 3.0E-4 0.7735997376539667 1.2978093784904636 0.081873395211054 0.7379077544506827 21.342646789905157 0.029761978960135785 0.029917258850362585 5.2260568325391406 0.7494443808063651",
		"Thalassa 8 2451545.0 3.3472401558060755E-4 2.0E-4 6.68539643330168 11.27426846252273 0.002356194490192345 2.5478316420613223 20.171747111601942 0.026303400365379946 0.026303400365379946 5.2260568325391406 0.7494269275138452",
		"Despina 8 2451545.0 3.5111462320539584E-4 2.0E-4 4.431687676786442 8.460117029899594 0.001186823891356144 1.3449507215868304 18.775083898708075 0.022225353797103983 0.022225353797103983 5.2260568325391406 0.7493920209288052",
		"Galatea 8 2451545.0 4.141302260108116E-4 1.0E-4 6.636754107048598 7.7886539600723355 5.93411945678072E-4 0.6500827864903279 14.65485433893574 0.01246552452098441 0.01246552452098441 5.226039379246621 0.7491825814185659",
		"Larissa 8 2451545.0 4.916380136981771E-4 0.0014 9.739251385393718 12.64079145366421 0.0035779249665883754 5.377830664292568 11.3281306671428 0.006842650691709819 0.006837211382733896 5.2260219259541 0.7486938892280075",
		"Proteus 8 2451545.0 7.864149366337052E-4 5.0E-4 6.6863389110977565 11.066033229467287 0.001308996938995747 5.50007352510225 5.5984162747407895 0.00134815233847637 0.001351647979803448 5.225620500226142 0.7405781082062339",
		"Charon 9 2451545.0 -1.1722091978315161E-4 0.0022 2.730427988404969 5.310862380893545 1.7453292519943296E-5 1.4867936298964095 0.9837102327428984 0.0 0.0"
	};

	// Orbits of natural satellites respect to local Laplace plane. Format is:
	// Name, central body ID, epoch JD, a (AU), e, periapsis long (rad), mean lon, incl, ascending node long, mean motion (rad/day), apsis rate, node rate (rad/day), Laplace pole RA, DEC
	// This is a backup of the old original data from Guide, which is periodically updated in JPARSEC to obtain the data above using JPL website.
	// DATA FOR YEAR 2005
	private static final int OLD_YEAR = 2005;
	private static final String[] OLD_ORBITS = new String[] {
		"Phobos 4 2433282.5 6.267578615950085E-5 0.015100488455132744 -0.0376892087333764 1.5515824447774913 0.01877010836393613 -2.6566718446265365 19.702057751786885 0.007595634829850728 -0.007605933889043184 5.544400363803764 0.9231568907786973",
		"Deimos 4 2433282.5 1.5680477451638896E-4 2.477565772864281E-4 -1.3045745684804448 4.373488589949038 0.031209570404047256 0.4280370356743034 4.977013690150073 3.130779447851181E-4 -3.154285617914763E-4 5.526706884732783 0.9342638352298094",
		"Amalthea 5 2450000.5 0.0012123538283589947 0.003080613787296755 3.026940936538228 5.391287144985582 0.006773231839121259 -2.887141342594295 12.612302690058815 0.04386143088067623 -0.04371246647432638 4.678482586078966 1.125638497231957",
		"Thebe 5 2450000.5 0.0014832313884227276 0.01769444275756342 -1.8356553613511088 5.043872141774269 0.01867311770312725 1.5627600353174749 9.314826604632305 0.021615150460260225 -0.021576438331435854 4.678483458358264 1.1256389954543",
		"Adrastea 5 2450000.5 8.622098786029097E-4 7.161791599482022E-4 3.0333998221209804 1.4781327513909397 0.001553459038692019 1.2189508650382748 21.066104308223718 0.1466001519734568 -0.14876964047908334 4.678470189094617 1.1256553885787377",
		"Metis 5 2450000.5 8.556245333986389E-4 7.664523109378132E-4 2.55654970834453 5.900056619617797 0.0012228151870376331 0.3914577079828685 21.314914819458853 0.14936978687541982 -0.1480506750514827 4.678470189094617 1.1256553885787377",
		"Janus 6 2444786.5 0.0010123617668799697 0.006583150064048162 -0.5349562582333303 3.479557783325707 0.0029413527386594573 -0.9669975098091169 9.045986333897599 0.03586418617334444 -0.035699436362576734 0.7082545868055182 1.4580480492940784",
		"Epimetheus 6 2444786.5 0.0010120410795321257 0.01256369659694896 1.4358949827638827 2.511338189508377 0.005685814895946847 -0.44056820350578785 9.050298786940205 0.035875089189336795 -0.03570222586418812 0.7082545868055182 1.4580480492940784",
		"Helene 6 2444786.5 0.0025228743995129926 0.0015857958188670114 -0.059017359237988856 1.756172471705913 0.0034821948314660857 0.18661539873831198 2.2957175591933883 -0.0010220818684428803 -0.0014597533308260725 0.7081874175946671 1.4581412878189801",
		"Telesto 6 2444786.5 0.00196977123623561 8.620812196064149E-4 -2.4898881334074567 3.7525041001731174 0.019946366586123098 2.189594496320703 3.328303103659601 0.0034638873628358934 -0.003453505413416172 0.7082545868055182 1.4580480492940784",
		"Calypso 6 2444786.5 0.0019697702950459434 1.828793919005018E-4 2.9823057411173837 1.6556796990616767 0.02567944448518036 -2.622211599790809 3.3283031154589207 0.0034638278190857407 -0.0034527951487533606 0.7082545868055182 1.4580480492940784",
		"Atlas 6 2444786.5 9.202434591088212E-4 0.0 0.0 3.255756328332502 0.0 0.0 10.442421108702158 0.050284356928364846 -0.05004453070715518 0.7084050382744774 1.4577920828491755",
		"Prometheus 6 2444940.0 9.316818705788246E-4 0.0019199994835770139 -1.7976891786402003 5.919371424601368 0.0 0.0 10.250100007877629 0.048125150286896984 -0.048125150286896984 0.708525136493358 1.4580152449261254",
		"Pandora 6 2444940.0 9.472936135081343E-4 0.004499999999796484 -0.017453292523449616 1.675917507642515 0.0 0.0 9.996994628067084 0.04536712863910845 -0.04536712863910845 0.708525136493358 1.4580152449261254",
		"Pan 6 2451545.0 8.929567380662009E-4 7.017326454628715E-6 -3.129275636287046 2.558596071225332 6.1185991115665795E-6 0.8879192733475367 10.926314962846979 0.055907564896971844 -0.055620012105206534 0.7082914262285315 1.4580069614291769",
		"Daphnis 6 2453491.91412037 9.124832283165889E-4 3.709137672444563E-5 1.1225509336100452 3.8911816495924327 6.0921726850505926E-5 2.456956648095457 10.57633157194576 0.05263285438180516 -0.05153536746175301 0.7082914262285315 1.4580069614291769",
		"Cordelia 7 2446450.0 3.3257681763970595E-4 2.5380901545878906E-4 3.0073479654800193 1.2217681069211517 0.0013854216102467487 0.5642706142248847 18.75389341941029 0.026220481699683867 -0.02618395571564411 1.3493767223712083 0.2648442511170915",
		"Ophelia 7 2446450.0 3.5944604322500206E-4 0.010002052272752707 -3.120092394198527 5.202435493851493 7.21697435525152E-4 -3.0855667844375843 16.69265413861426 0.019976186951861562 -0.019952384739476872 1.3493767223712083 0.2648442511170915",
		"Bianca 7 2446450.0 3.955004050503375E-4 8.700029119494172E-4 1.7975955380483486 4.188780121505055 0.0031811581530430334 1.5306766348915604 14.45809677715721 0.014276058836267192 -0.014262253414483915 1.3493767223712083 0.2648442511170915",
		"Cressida 7 2446450.0 4.128869779759953E-4 3.719330094143247E-4 2.79016818005877 0.3044258508071657 2.0827936918116582E-4 -2.148984563066767 13.553919675051443 0.01227838643593449 -0.012267302945715125 1.3493767223712083 0.2648442511170915",
		"Desdemona 7 2446450.0 4.188430257589893E-4 9.676820674809028E-5 2.4565334159171077 5.480490475609905 0.0017377780535808311 -1.23556197716936 13.265470803841449 0.01167712380711475 -0.011666921819178307 1.3493767223712083 0.2648442511170915",
		"Juliet 7 2446450.0 4.3020654147831247E-4 6.361693005836747E-4 1.1203467190763194 5.387152173217216 7.039470740043897E-4 3.1359675607711486 12.743104340594764 0.010632340161244874 -0.01062350127259412 1.3493767223712083 0.2648442511170915",
		"Portia 7 2446450.0 4.4183108930733784E-4 1.8741661373991525E-5 -2.1571419254190074 5.948425776694432 0.001235971676796694 -1.6242096786018336 12.243248261521641 0.009684851213620145 -0.009677242856785595 1.3493767223712083 0.2648442511170915",
		"Rosalind 7 2446450.0 4.674331702841975E-4 2.64900010614682E-4 -3.1362952470024466 5.053105558222829 0.002776103510217616 0.10287101816861277 11.250922858909426 0.007952883752589877 -0.007947371850883332 1.3493767223712083 0.2648442511170915",
		"Belinda 7 2446450.0 5.030552416158751E-4 1.4529753483760174E-4 -0.8943985849793581 5.566969062962514 5.917798698454599E-4 -0.9010358690609218 10.07683803858385 0.00615338793681825 -0.006149658143857638 1.3493767223712083 0.2648442511170915",
		"Puck 7 2446450.0 5.749079535998969E-4 5.018154316867449E-5 0.5005164248215471 5.788091388725543 0.005848846388301901 -1.6236874977154485 8.247458413410449 0.0038663118736249422 -0.0038647605506632794 1.3493767223712083 0.2648442511170915",
		"Perdita 7 2453243.0 5.163691897340136E-4 0.01440701109067305 1.3064519971794253 0.627472126785817 0.009476346121648589 -0.9973006123124718 9.84794693337463 0.01093973248703778 -0.0019069267669350296 1.3493767223712083 0.2648442511170915",
		"Mab 7 2453243.0 6.534562757019648E-4 0.004591779670058372 -1.5339279720017234 2.689852932363044 0.002517753596429204 -0.566800832526093 6.80764047151097 0.003083926606693281 -0.002896630735547899 1.3493767223712083 0.2648442511170915",
		"Cupid 7 2453243.0 4.980228040953151E-4 0.0021124843056036196 1.0550534644365261 4.087556992923546 0.00223970105736972 -2.894149863987594 10.252817074693283 0.0060462334920823105 -0.005199975424700547 1.3493767223712083 0.2648442511170915",
		"Triton 8 2447763.5 0.002371418419001219 1.5590146914698226E-5 3.001075212620709 -1.33909285381974 0.4043147385075364 3.0036566941612373 -1.0691409422687608 1.8275833103848593E-5 2.4998933318098167E-5 5.217614570227489 0.7560575380619267",
		"Naiad 8 2447757.0 3.223776791493794E-4 3.6220705454169963E-4 1.5388703407046889 1.1886285212830825 0.08282507102554905 0.9850248845550155 21.34262168313067 0.029618850830498856 -0.029935816250927463 5.2248785433078275 0.7583297822150981",
		"Thalassa 8 2447757.0 3.347303746711761E-4 2.1466146433816774E-4 1.0684718945512728 4.321104246702821 0.003654373022531852 1.7120568434267607 20.17172214424821 0.026322183480591083 -0.026334748999562157 5.2248785433078275 0.7583297822150981",
		"Despina 8 2447757.0 3.511151182496916E-4 2.23571652247945E-4 1.6207238554026175 1.625135982896519 0.0011104873541881947 2.7837312659891174 18.77505879069193 0.022251008486039333 -0.02226940382355045 5.2248785433078275 0.7583297822150981",
		"Galatea 8 2447757.0 4.141300858777136E-4 3.710347945060224E-5 2.673412477794181 0.9509973053774584 0.0010735786747469863 2.1940079935830155 14.654829076299283 0.012463311040593797 -0.012492373589136765 5.2248785433078275 0.7583297822150981",
		"Larissa 8 2447757.0 4.91637459499346E-4 0.0013933848662935535 2.716798624702286 3.362645973494407 0.003571181569065572 0.30697334949096383 11.328105652689134 0.006826463459756517 -0.00686964366755779 5.2248785433078275 0.7583297822150981",
		"Proteus 8 2447757.0 7.864203837714743E-4 5.305344375742068E-4 1.8220561541871432 3.8649613098519593 4.498008970132817E-4 2.839476203598833 5.598391305843201 0.0013215968130346589 -0.001370259394032259 5.2248785433078275 0.7583297822150981",
		"Io 5 2450464.5 0.002819558848342458 0.0041 2.235871491559856 8.205264052523383 6.283185307179586E-4 0.7675434451495462 3.5515523137884433 0.005293053488910303 0.002318385962123785 4.67847723301844 1.125650101073743",
		"Europa 5 2450464.5 0.004486026418024238 0.0094 5.37694054637405 8.361732819964672 0.008133234314293577 3.8241211108746955 1.7693227155911746 0.006170166369784249 5.699186270526929E-4 4.6789484719164784 1.1258420872914623",
		"Ganymede 5 2450464.5 0.0071551820561066085 0.0013 4.467501833037366 10.009620339820161 0.0030892327760299633 1.1091916462274363 0.8782079173652049 1.3534771466866893E-4 1.296788927507537E-4 4.680414548488153 1.1264878591147003",
		"Callisto 5 2450464.5 0.012585072175851935 0.0074 6.134675241127389 9.300842130585263 0.003351032163829113 5.215881563000014 0.3764862435473923 4.180418915907286E-5 5.077157145079536E-5 4.688635049265047 1.1300832373738083",
		"Himalia 5 2451544.5 0.07661205301292773 0.1623 6.793519580462728 7.992927295725753 0.4798957311283608 0.999113730304154 0.025076398509805465 6.270933157975534E-5 6.0807436687728833E-5 4.811296789095208 1.1768580613272566",
		"Elara 5 2451544.5 0.07848373740727549 0.2174 4.415054689014936 10.226337873040295 0.4647288199285301 1.9089189627837582 0.02419984703556411 6.803679733807343E-5 6.813110950516251E-5 4.768239516448508 1.1911174013160502",
		"Pasiphae 5 2451544.5 0.1579166861859702 0.409 8.437619735841388 13.327910126881857 2.6429695395875332 5.462706025817052 0.008449325659134513 1.0641113348359819E-4 2.1234938697640393E-4 4.772428306653294 1.1806279725115643",
		"Sinope 5 2451544.5 0.16002233112961145 0.2495 11.335477159390173 14.274559259871062 2.7595226270357145 5.289761350236934 0.008279334080648768 9.635053119165724E-5 1.9650929676671787E-4 4.769391433754825 1.1672762037338076",
		"Lysithea 5 2451544.5 0.0783233073163314 0.1124 0.9601754346921604 6.704420522148417 0.49396308489943513 0.09648180105024654 0.024240527169769592 6.539850912012805E-5 5.736435853994426E-5 4.736055645041732 1.1713777274759942",
		"Carme 5 2451544.5 0.1564460770189827 0.2533 2.4772679804031914 6.56180966896796 2.8781701095862893 1.9851025846333106 0.008558237695117462 1.061092020661145E-4 1.9431180208921814E-4 4.758116606786941 1.1639426248624982",
		"Ananke 5 2451544.5 0.14222127562193965 0.2435 1.8890396626035426 6.231296668517795 2.5986032700018376 0.1329068225393682 0.00997694428556432 6.106646730194704E-5 1.5787833919748975E-4 4.870987049513414 1.154570206779289",
		"Leda 5 2451544.5 0.07463341522461722 0.1636 8.543142342416964 12.52381948719555 0.4792150527200831 3.7897555779029273 0.026079578857266764 6.0329746226269495E-5 5.798700141225135E-5 4.741501072307956 1.1667176983731693",
		"Mimas 6 2451545.0 0.00124025161015318 0.0196 8.822709182001416 9.082344361528092 0.027471482426390748 3.0198733915557088 6.667061709366907 0.01744667732145891 0.01744667732145891 0.7083244236293788 1.458013150531023",
		"Enceladus 6 2451545.0 0.001591179064919141 0.0047 9.277908504214057 9.464484201252251 1.5707963267948965E-4 5.991121910150856 4.585536671859234 0.006585920305879971 0.007276829035092422 0.708272063751819 1.458013150531023",
		"Tethys 6 2451545.0 0.0019697606566115907 1.0E-4 5.317808791316484 9.571647417324703 0.019041542139258134 4.535150794844666 3.3283064288611 0.0034543019756944743 0.003452915262737552 0.7081149841191394 1.4580306038235429",
		"Dione 6 2451545.0 0.0025228634489027238 0.0022 10.03086099681693 15.655044885028497 4.886921905584122E-4 5.072188605683321 2.2957176238428993 0.0014697901434516816 0.0014691625108001099 0.7075390254659812 1.4580829637011028",
		"Rhea 6 2451545.0 0.003523231965571747 0.0010 9.8555426734541 13.481760900445158 0.005811946409141118 6.126419833765455 1.3908536872563957 2.523015435006084E-4 4.8008550566416846E-4 0.7037167544041136 1.4583971229664618",
		"Titan 6 2451545.0 0.00816766304464191 0.0288 3.6421181764767265 6.492380471323626 0.005445427266222308 0.5019217862885292 0.39404255936242233 2.4131559968238482E-5 2.382409195767455E-5 0.6305525521605114 1.4653086268043594",
		"Hyperion 6 2451545.0 0.010033124088378472 0.0232 9.896784803678726 11.403667173265589 0.010733774899765127 4.6056097367476765 0.2953088437541316 6.718122252190301E-5 1.1119860270819965E-4 0.6345319028550584 1.4636331107224447",
		"Iapetus 6 2451545.0 0.023802818740348723 0.0293 6.157312161525756 9.679561124975512 0.14508922071828861 1.417015366401676 0.07920202916453652 5.125413055733541E-6 4.992997390353403E-6 4.967870276291619 1.3744467859455345",
		"Phoebe 6 2451545.0 0.08655145250525924 0.1634 10.185532075128668 11.1112023505089 3.058567341072423 4.207761933755569 0.011417692639134617 1.8660964851773067E-5 2.378752414917445E-5 4.8163058840484325 1.177311846932775",
		"Nereid 8 2451545.0 0.03685759679954935 0.7507 10.763218604246271 14.545207466977825 0.12374384396639797 5.856801370917371 0.017446792913808868 1.0630000703803697E-6 1.8192592046260295E-6 4.7002065822057695 1.2063192191009209",
		"Charon 9 2451545.0 -1.1722091978315161E-4 0.0022 2.730427988404969 5.310862380893545 1.7453292519943296E-5 1.4867936298964095 0.9837102327428984 0.0 0.0"
	};
}
