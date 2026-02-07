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
package jparsec.ephem;


import java.util.ArrayList;

import jparsec.astronomy.CoordinateSystem;
import jparsec.astronomy.Star;
import jparsec.ephem.EphemerisElement.ALGORITHM;
import jparsec.ephem.EphemerisElement.COORDINATES_TYPE;
import jparsec.ephem.EphemerisElement.FRAME;
import jparsec.ephem.EphemerisElement.OBSERVING_WAVELENGTH;
import jparsec.ephem.EphemerisElement.REDUCTION_METHOD;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.moons.MoonEphem;
import jparsec.ephem.moons.MoonEphemElement;
import jparsec.ephem.planets.EphemElement;
import jparsec.ephem.planets.JPLEphemeris;
import jparsec.ephem.planets.Newcomb;
import jparsec.ephem.planets.OrbitEphem;
import jparsec.ephem.planets.OrbitalElement;
import jparsec.ephem.planets.PlanetEphem;
import jparsec.ephem.planets.imcce.Elp2000;
import jparsec.ephem.planets.imcce.Series96;
import jparsec.ephem.planets.imcce.Vsop;
import jparsec.ephem.probes.SDP4_SGP4;
import jparsec.ephem.probes.SatelliteEphem;
import jparsec.ephem.probes.Spacecraft;
import jparsec.ephem.stars.StarElement;
import jparsec.ephem.stars.StarEphem;
import jparsec.graph.DataSet;
import jparsec.graph.chartRendering.RenderSky;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.io.Reflection;
import jparsec.math.Constant;
import jparsec.math.FastMath;
import jparsec.observer.City;
import jparsec.observer.LocationElement;
import jparsec.observer.Observatory;
import jparsec.observer.ObserverElement;
import jparsec.observer.ReferenceEllipsoid.ELLIPSOID;
import jparsec.time.AstroDate;
import jparsec.time.SiderealTime;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.time.TimeScale;
import jparsec.util.Configuration;
import jparsec.util.DataBase;
import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Logger.LEVEL;
import jparsec.util.Translate;
import jparsec.util.Translate.LANGUAGE;

/**
 * This class provides accurate ephemeris of any major body in the Solar System.
 * The method {@linkplain Ephem#getEphemeris(TimeElement, ObserverElement, EphemerisElement, boolean)}
 * is the main part of this class, and needs some other methods for different corrections.
 *
 * @see Functions
 * @see TimeElement
 * @see ObserverElement
 * @see EphemerisElement
 * @see EphemElement
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Ephem
{
	// private constructor so that this class cannot be instantiated.
	private Ephem() {}

	/**
	 * A fast way to perform ephemerides calculations for a given object.
	 * This method does not call any Internet service, it resolves objects using only JPARSEC.
	 * Source can be a star, deep sky object, or solar system body. For deep sky objects only
	 * the equatorial position is returned. Ephemeris properties are modified to perform
	 * the ephemerides computation, although the input object is not modified.
	 * @param body The name of the body.
	 * @param time Time object for calculations.
	 * @param observer Observer object for calculations.
	 * @param eph0 Ephemeris object for calculations. It is not modified.
	 * @param fullEphem True to obtain also instants of rise, set, transit. Refraction
	 * at horizon is supposed to be 34', and the angular radius of the object in taken
	 * into account.
	 * @return The results of the ephemerides.
	 * @throws JPARSECException If the body is not found.
	 */
	public static EphemElement getEphemeris(String body, TimeElement time, ObserverElement observer,
			EphemerisElement eph0, boolean fullEphem) throws JPARSECException {
		TARGET t = jparsec.ephem.Target.getID(body);
		if (t == TARGET.NOT_A_PLANET && Translate.getDefaultLanguage() != LANGUAGE.ENGLISH)
			t = jparsec.ephem.Target.getIDFromEnglishName(body);

		EphemerisElement eph = eph0.clone();
		boolean apparentOfDate = true;
		if (eph.ephemType != COORDINATES_TYPE.APPARENT || eph.equinox != EphemerisElement.EQUINOX_OF_DATE) apparentOfDate = false;

		if (t == TARGET.NOT_A_PLANET) {
			LocationElement se = null;
			int index = StarEphem.getStarTargetIndex(body);
			if (index == -1) {
				se = RenderSky.searchDeepSkyObjectJ2000(body);
				if (se == null) {
					eph.targetBody = null;
					int ai = OrbitEphem.getIndexOfAsteroid(body);
					if (ai >= 0) {
						eph.targetBody = TARGET.Asteroid;
						eph.targetBody.setIndex(ai);
						eph.orbit = OrbitEphem.getOrbitalElementsOfAsteroid(ai);
						eph.algorithm = ALGORITHM.ORBIT;
					} else {
						int ci = OrbitEphem.getIndexOfComet(body);
						if (ci >= 0) {
							eph.targetBody = TARGET.Comet;
							eph.targetBody.setIndex(ci);
							eph.orbit = OrbitEphem.getOrbitalElementsOfComet(ci);
							eph.algorithm = ALGORITHM.ORBIT;
						} else {
							int ti = OrbitEphem.getIndexOfTransNeptunian(body);
							if (ti >= 0) {
								eph.targetBody = TARGET.Asteroid;
								eph.targetBody.setIndex(ti);
								eph.algorithm = ALGORITHM.ORBIT;
								eph.orbit = OrbitEphem.getOrbitalElementsOfTransNeptunian(ti);
							} else {
								try {
									ReadFile re = new ReadFile();
									re.setFormat(ReadFile.FORMAT.MPC);
									re.setPath(OrbitEphem.PATH_TO_MPC_NEOs_FILE);
									try { re.readFileOfNEOs(time.astroDate.jd(), 365); } catch (Exception exc) {}
									int n = re.searchByName(body);
									if (n >= 0) {
										eph.targetBody = TARGET.NEO;
										eph.targetBody.setIndex(n);
										eph.algorithm = ALGORITHM.ORBIT;
										eph.orbit = re.getOrbitalElement(n);
									}
								} catch (JPARSECException exc) {}
							}
						}
					}
					if (eph.targetBody == null) throw new JPARSECException("Object "+body+" not found");
					EphemElement ephem = Ephem.getEphemeris(time, observer, eph, fullEphem);
					return ephem;
				}
			} else {
				StarElement star = StarEphem.getStarElement(index);
				return EphemElement.parseStarEphemElement(StarEphem.starEphemeris(time, observer, eph, star, fullEphem));
			}

			if (apparentOfDate) se =  Ephem.fromJ2000ToApparentGeocentricEquatorial(se, time, observer, eph);
			EphemElement ephem = new EphemElement();
			ephem.rightAscension = se.getLongitude();
			ephem.declination = se.getLatitude();
			ephem.distance = se.getRadius() * Constant.PARSEC * 0.001 / Constant.AU;
			return ephem;
		} else {
			eph.targetBody = t;
			int ai = OrbitEphem.getIndexOfAsteroid(body);
			if (ai >= 0) {
				eph.targetBody = TARGET.Asteroid;
				eph.targetBody.setIndex(ai);
				eph.algorithm = ALGORITHM.ORBIT;
			} else {
				int ci = OrbitEphem.getIndexOfComet(body);
				if (ci >= 0) {
					eph.targetBody = TARGET.Comet;
					eph.targetBody.setIndex(ci);
					eph.algorithm = ALGORITHM.ORBIT;
				} else {
					int ti = OrbitEphem.getIndexOfTransNeptunian(body);
					if (ti >= 0) {
						eph.targetBody = TARGET.Asteroid;
						eph.targetBody.setIndex(ti);
						eph.algorithm = ALGORITHM.ORBIT;
					}
				}
			}
			if (t.isAsteroid()) eph.algorithm = ALGORITHM.ORBIT;
			if (t.isNaturalSatellite()) eph.algorithm = ALGORITHM.NATURAL_SATELLITE;
			EphemElement ephem = Ephem.getEphemeris(time, observer, eph, fullEphem);
			return ephem;
		}
	}

	/**
	 * Calculate planetary (or natural satellite) positions, providing full data. This method applies
	 * the adequate planetary algorithm depending if one prefers precision or
	 * performance. The ephemeris algorithm and reduction method is changed if
	 * required depending on the calculation date and the preference for precision or performance. Full
	 * data includes rise, set, and transit times, referred to the current day or
	 * the next events in time (if the object is actually below the horizon).
	 * <P>
	 * Natural satellite ephemeris are calculated using the best possible method for the mother planet,
	 * with maximum possible precision. The JPL version defined in class {@linkplain Configuration} is 
	 * used when available (external files supported), and Series96
	 * or Moshier when JPL is not available. The theory used for the satellite will be a specific one
	 * for the satellites around the planet (L1, TASS, GUST86, ...) or JPL elliptic elements in case
	 * precise ephemeris is not requested or the satellite position can only be calculated in this way.
	 * <P>
	 * Algorithm, method, and frame selected are not changed if the ephemeris is for a star,
	 * comet or asteroid, space probe, or artificial satellite.
	 *
	 * @param time Time object containing the date.
	 * @param obs Observer object containing the observer position.
	 * @param eph_in Ephemeris object defining the ephemeris properties.
	 * @param full_ephem True to obtain also instants of rise, set, transit. Refraction
	 * at horizon is supposed to be 34', and the angular radius of the object in taken
	 * into account.
	 * @param preferPrecision True to prefer precision (only if
	 * {@linkplain EphemerisElement#preferPrecisionInEphemerides} is also true).
	 * In this case the fields algorithm, method, and frame of the ephemeris object will
	 * be modified to get the best possible accuracy depending on the available resources.
	 * In case this is set to false OR {@linkplain EphemerisElement#preferPrecisionInEphemerides}
	 * is false the method to be used will be always the Moshier fit to JPL DE404 ephemerides.
	 * Preference order for accuracy is {@linkplain Configuration#JPL_DEFAULT_VERSION} 
	 * (either JPARSEC's file or an external one, if
	 * the one selected in {@linkplain Configuration#JPL_EPHEMERIDES_FILES_EXTERNAL_PATH} is available),
	 * Series96 between 1900 and 2100 for planets, ELP2000 always for the Moon, VSOP87
	 * for Mercury, Mars, and Venus before 1350 B.C., and Moshier in the rest of cases. For
	 * both values of this parameter Moshier method will use JPL reduction methods, and the
	 * others IAU 2006 algorithms. Frame is ICRF always.
	 *
	 * @return Ephem object containing full ephemeris data.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public static EphemElement getEphemeris(TimeElement time, // Time Element
			ObserverElement obs, // Observer Element
			EphemerisElement eph_in, // Ephemeris Element
			boolean full_ephem, boolean preferPrecision) throws JPARSECException
	{
		EphemerisElement eph = (EphemerisElement) eph_in.clone();
		boolean isPlanet = eph.targetBody.isPlanet() || eph.targetBody == TARGET.Pluto || eph.targetBody == TARGET.Moon;
		if (eph.algorithm == EphemerisElement.ALGORITHM.NATURAL_SATELLITE || eph.algorithm == EphemerisElement.ALGORITHM.ORBIT ||
				eph.algorithm == EphemerisElement.ALGORITHM.PROBE || eph.algorithm == EphemerisElement.ALGORITHM.ARTIFICIAL_SATELLITE ||
				eph.algorithm == EphemerisElement.ALGORITHM.STAR) isPlanet = false;
		if (!eph.preferPrecisionInEphemerides) preferPrecision = false;
		if (isPlanet) {
			if (preferPrecision) {
				double jd = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
				JPLEphemeris jpl = null;
				try {
					jpl = new JPLEphemeris(Configuration.JPL_DEFAULT_VERSION, Configuration.JPL_EPHEMERIDES_FILES_EXTERNAL_PATH);
					if (!jpl.isAvailable(jd)) throw new Exception("Not available.");
				} catch (Exception exc) {}
				//if (jd >= 2414992.5+0.5 && jd <= 2488080.5-0.5 && jpl.isAvailable(jd))	{ // offset 0.5 because light-time corrections could give jd out of range
				if (jpl != null && jpl.isAvailable(jd))	{ // offset 0.5 because light-time corrections could give jd out of range
					eph.algorithm = jpl.getJPLVersionID();
					eph.ephemMethod = EphemerisElement.REDUCTION_METHOD.IAU_2006;
					eph.frame = EphemerisElement.FRAME.DYNAMICAL_EQUINOX_J2000;
				} else {
					eph.algorithm = EphemerisElement.ALGORITHM.SERIES96_MOSHIERForMoon;
					eph.ephemMethod = EphemerisElement.REDUCTION_METHOD.IAU_2006;
					eph.frame = EphemerisElement.FRAME.DYNAMICAL_EQUINOX_J2000;
					if (eph.targetBody == TARGET.Moon || jd < 2415020.5 + 0.5 || jd > 2488092.5 - 0.5 || !Series96.isSeries96Available()) {
						eph.algorithm = EphemerisElement.ALGORITHM.MOSHIER;
						eph.ephemMethod = EphemerisElement.REDUCTION_METHOD.JPL_DE4xx; // More consistent with JPL integration
						eph.frame = EphemerisElement.FRAME.DYNAMICAL_EQUINOX_J2000;

						// The position of the Moon is more accurate with ELP2000 (fixed), and the same
						// for the inner planets before 1350 B.C.
						if (eph.targetBody == TARGET.Moon || (jd < 1228335.5 && (eph.targetBody == TARGET.MERCURY ||
								eph.targetBody == TARGET.MARS || eph.targetBody == TARGET.VENUS))) {
							eph.algorithm = EphemerisElement.ALGORITHM.VSOP87_ELP2000ForMoon;
							eph.ephemMethod = EphemerisElement.REDUCTION_METHOD.IAU_2006;
						}
					}
				}
			} else {
				eph.algorithm = EphemerisElement.ALGORITHM.MOSHIER;
				eph.ephemMethod = EphemerisElement.REDUCTION_METHOD.JPL_DE4xx;
				eph.frame = EphemerisElement.FRAME.DYNAMICAL_EQUINOX_J2000;
				if (eph.targetBody == TARGET.Moon) eph.ephemMethod = EphemerisElement.REDUCTION_METHOD.WILLIAMS_1994;
			}
		}

		// Check Ephemeris object
		if (!EphemerisElement.checkEphemeris(eph))
		{
			throw new JPARSECException("invalid ephemeris object.");
		}

		// Obtain ephemeris
		EphemElement ephem_elem = Ephem.getEphemerisResult(time, obs, eph, full_ephem, preferPrecision);

		// Obtain rise/set/transit times. Note we use here the default 34
		// arcminute value with no correction for depresion of the horizon.
		if (eph.isTopocentric && full_ephem && eph.algorithm != EphemerisElement.ALGORITHM.ARTIFICIAL_SATELLITE &&
				eph.algorithm != EphemerisElement.ALGORITHM.STAR) {
			Object gcrs = DataBase.getData("GCRS", true);
			ephem_elem = RiseSetTransit.obtainCurrentOrNextRiseSetTransit(time, obs, eph, ephem_elem,
					RiseSetTransit.TWILIGHT.HORIZON_ASTRONOMICAL_34arcmin);
			DataBase.addData("GCRS", gcrs, true);
		}

		return ephem_elem;
	}

	/**
	 * Calculate planetary positions, providing full data. This method applies
	 * the planetary algorithm selected in the ephemeris object, without
	 * considering the value of {@linkplain EphemerisElement#preferPrecisionInEphemerides}. Full
	 * data includes rise, set, and transit times, referred to the current day or
	 * the next events in time (if the object is actually below the horizon).
	 *
	 * Natural satellite ephemeris are calculated using the best possible method for the mother planet,
	 * with maximum possible precision. The JPL integration defined at {@linkplain Configuration#JPL_DEFAULT_VERSION}
	 * is used when available (external files supported), and Series96
	 * or Moshier when JPL is not available. The theory used for the satellite will be a specific one
	 * for the satellites around the planet (L1, TASS, GUST86, ...) or JPL elliptic elements in case
	 * precise ephemeris is not requested or the satellite position can only be calculated in this way.
	 *
	 * Using this method for planets could launch exceptions if the required algorithm is not available
	 * in the classpath or it requires external files that cannot be found. For natural satellites no problem
	 * will appear, but the algorithm used for the position of the mother planet will give preference to
	 * the JPL defined in class {@linkplain Configuration} or Series96, using Moshier only if they are unavailable.
	 *
	 * @param time Time object containing the date.
	 * @param obs Observer object containing the observer position.
	 * @param eph Ephemeris object defining the ephemeris properties.
	 * @param full_ephem True to obtain also instants of rise, set, transit. Refraction
	 * at horizon is supposed to be 34', and the angular radius of the object in taken
	 * into account.
	 * @return Ephem object containing full ephemeris data.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public static EphemElement getEphemeris(TimeElement time, // Time Element
			ObserverElement obs, // Observer Element
			EphemerisElement eph, // Ephemeris Element
			boolean full_ephem) throws JPARSECException

	{
		// Check Ephemeris object
		if (!EphemerisElement.checkEphemeris(eph))
			throw new JPARSECException("invalid ephemeris object.");

		// Obtain ephemeris
		EphemElement ephem_elem = Ephem.getEphemerisResult(time, obs, eph, full_ephem, true);

		// Obtain rise/set/transit times. Note we use here the default 34
		// arcminute value with no correction for depresion of the horizon.
		if (eph.isTopocentric && full_ephem && eph.algorithm != EphemerisElement.ALGORITHM.ARTIFICIAL_SATELLITE &&
				eph.algorithm != EphemerisElement.ALGORITHM.STAR) {
			Object gcrs = DataBase.getData("GCRS", true);
			ephem_elem = RiseSetTransit.obtainCurrentRiseSetTransit(time, obs, eph, ephem_elem,
					RiseSetTransit.TWILIGHT.HORIZON_ASTRONOMICAL_34arcmin);
			DataBase.addData("GCRS", gcrs, true);
		}

		return ephem_elem;
	}

	static EphemElement getEphemerisResult(TimeElement time, // Time Element
			ObserverElement obs, // Observer Element
			EphemerisElement eph, // Ephemeris Element
			boolean full_ephem, boolean preferPrecision)
			throws JPARSECException
	{
		try
		{
			EphemElement ephem_elem = new EphemElement();

			switch (eph.algorithm)
			{
			case MOSHIER:
				ephem_elem = PlanetEphem.MoshierEphemeris(time, obs, eph);
				break;
			case SERIES96_MOSHIERForMoon:
				if (eph.targetBody == TARGET.Moon)
				{
					ephem_elem = PlanetEphem.MoshierEphemeris(time, obs, eph);
				} else {
					ephem_elem = Series96.series96Ephemeris(time, obs, eph);
				}
				break;
			case VSOP87_ELP2000ForMoon:
				if (eph.targetBody == TARGET.Moon)
				{
					ephem_elem = Elp2000.elp2000Ephemeris(time, obs, eph);
				} else
				{
					ephem_elem = Vsop.vsopEphemeris(time, obs, eph);
				}
				break;
			case NATURAL_SATELLITE:
				// Automatically select between JPL, Moshier and Series96 method for
				// satellite positions depending on the date.
				EphemerisElement new_eph = (EphemerisElement) eph.clone();

				double JD = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
				JPLEphemeris jplEph = null;
				if (preferPrecision && eph.preferPrecisionInEphemerides) {
					try {
						jplEph = new JPLEphemeris(Configuration.JPL_DEFAULT_VERSION, Configuration.JPL_EPHEMERIDES_FILES_EXTERNAL_PATH);
						if (!jplEph.isAvailable(JD)) throw new Exception("Not available.");
					} catch (Exception exc) {}
				}
				if (jplEph != null && jplEph.isAvailable(JD)) {
					new_eph.algorithm = jplEph.getJPLVersionID();
// Here it is better to allow the use of other reducton methods (the one selected in eph)
//					new_eph.ephemMethod = EphemerisElement.APPLY_IAU2006;
				} else {
					new_eph.algorithm = EphemerisElement.ALGORITHM.SERIES96_MOSHIERForMoon;
//					new_eph.ephemMethod = EphemerisElement.APPLY_JPLDE403;
					// Here I add 0.5 for possible problems when using JPL for natural satellites with certain light time corrections
					if ((JD < 2415020.5 + 0.5 || JD > 2488092.5 - 0.5 || !preferPrecision) || !Series96.isSeries96Available()) {
						new_eph.algorithm = EphemerisElement.ALGORITHM.MOSHIER;
//						new_eph.ephemMethod = EphemerisElement.APPLY_WILLIAMS;
					}
				}

				int mainSat = 0, mainSatN = 0;
				if (eph.targetBody == TARGET.Phobos || eph.targetBody == TARGET.Deimos) {
					mainSat = 1;
					mainSatN = eph.targetBody.ordinal() - TARGET.Phobos.ordinal();
				}
				if (eph.targetBody == TARGET.Io || eph.targetBody == TARGET.Europa || eph.targetBody == TARGET.Ganymede
						|| eph.targetBody == TARGET.Callisto) {
					mainSat = 2;
					mainSatN = eph.targetBody.ordinal() - TARGET.Io.ordinal();
				}
				if (eph.targetBody == TARGET.Mimas || eph.targetBody == TARGET.Enceladus || eph.targetBody == TARGET.Tethys
						|| eph.targetBody == TARGET.Dione || eph.targetBody == TARGET.Rhea || eph.targetBody == TARGET.Titan
						|| eph.targetBody == TARGET.Hyperion || eph.targetBody == TARGET.Iapetus) {
					mainSat = 3;
					mainSatN = eph.targetBody.ordinal() - TARGET.Mimas.ordinal();
				}
				if (eph.targetBody == TARGET.Miranda || eph.targetBody == TARGET.Ariel || eph.targetBody == TARGET.Umbriel
						|| eph.targetBody == TARGET.Titania || eph.targetBody == TARGET.Oberon) {
					mainSat = 4;
					mainSatN = eph.targetBody.ordinal() - TARGET.Miranda.ordinal();
				}
				if (eph.targetBody == TARGET.Triton) {
					mainSat = 5;
					mainSatN = 0;
				}
				if (preferPrecision && mainSat > 0) {
					if (mainSat == 1) ephem_elem = EphemElement.parseMoonEphemElement(MoonEphem.martianSatellitesEphemerides_2007(time, obs, new_eph)[mainSatN], new_eph.getEpoch(JD));
					if (mainSat == 2) ephem_elem = EphemElement.parseMoonEphemElement(MoonEphem.galileanSatellitesEphemerides_L1(time, obs, new_eph)[mainSatN], new_eph.getEpoch(JD));
					if (mainSat == 3) ephem_elem = EphemElement.parseMoonEphemElement(MoonEphem.saturnianSatellitesEphemerides_TASS17(time, obs, new_eph, false)[mainSatN], new_eph.getEpoch(JD));
					if (mainSat == 4) ephem_elem = EphemElement.parseMoonEphemElement(MoonEphem.uranianSatellitesEphemerides_GUST86(time, obs, new_eph)[mainSatN], new_eph.getEpoch(JD));
					// Triton method is deprecated, JPL elements give more accuracy
					if (mainSat == 5) {
						new_eph.targetBody = TARGET.NEPTUNE;
						ephem_elem = EphemElement.parseMoonEphemElement(MoonEphem.calcJPLSatellite(time, obs, new_eph, TARGET.Triton.getEnglishName()), JD);
					}
				} else {
					ephem_elem = null;

					TARGET sat = new_eph.targetBody;
					new_eph.targetBody = sat.getCentralBody();

					if (full_ephem) {
						// Calculate all satellites around the planet to allow information about mutual phenomena
						MoonEphemElement moons[] = MoonEphem.calcAllJPLSatellites(time, obs, new_eph);
						for (int i=0; i<moons.length; i++) {
							if (Target.getID(moons[i].name) == sat) {
								ephem_elem = EphemElement.parseMoonEphemElement(moons[i], new_eph.getEpoch(JD));
								break;
							}
						}
					} else {
						ephem_elem = EphemElement.parseMoonEphemElement(MoonEphem.calcJPLSatellite(time, obs, new_eph, sat.getEnglishName()), new_eph.getEpoch(JD));

						if (ephem_elem == null) {
							new_eph.targetBody = sat;
							ephem_elem = EphemElement.parseMoonEphemElement(MoonEphem.calcSatellite(time, obs, new_eph), new_eph.getEpoch(JD));
						}
					}
				}
				break;
			case NEWCOMB:
				ephem_elem = Newcomb.newcombSunEphemeris(time, obs, eph);
				break;
			case ORBIT:
				ephem_elem = OrbitEphem.orbitEphemeris(time, obs, eph);
				break;
			case PROBE:
				ephem_elem = Spacecraft.orbitEphemeris(time, obs, eph);
				break;
			case ARTIFICIAL_SATELLITE:
				JD = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
				if (preferPrecision) {
					ephem_elem = EphemElement.parseSatelliteEphemElement(SDP4_SGP4.satEphemeris(time, obs, eph, full_ephem),
						eph.getEpoch(JD));
				} else {
					ephem_elem = EphemElement.parseSatelliteEphemElement(SatelliteEphem.satEphemeris(time, obs, eph, full_ephem),
							eph.getEpoch(JD));
				}
				break;
			case STAR:
				ephem_elem = EphemElement.parseStarEphemElement(StarEphem.starEphemeris(time, obs, eph, full_ephem));
				break;
			default:
				if (!eph.algorithm.isJPL()) throw new JPARSECException("invalid algorithm "+eph.algorithm+".");
				JPLEphemeris jpl = null;
				try {
					jpl = new JPLEphemeris(eph.algorithm, Configuration.JPL_EPHEMERIDES_FILES_EXTERNAL_PATH);
					ephem_elem = jpl.getJPLEphemeris(time, obs, eph);
				} catch (JPARSECException exc) {
					Logger.log(LEVEL.WARNING, "Could not use JPL ephemerides ("+eph.algorithm+"). Using Moshier instead.");
					ephem_elem = PlanetEphem.MoshierEphemeris(time, obs, eph);
				}
			}

			return ephem_elem;
		} catch (JPARSECException ve)
		{
			throw ve;
		}
	}

	/**
	 * A quick way to set some properties of an ephemeris element (algorithm,
	 * orbital elements set, and target body) according to certain body.
	 * This methods searches for a given planet, natural satellite, asteroid,
	 * distant body, comet, probe, artificial satellite, or star.<BR><BR>
	 * Please note the availability of certain objects like asteroids and comets
	 * depends on certain configuration values in class {@linkplain Configuration}.
	 * This means a given comet will be available for some years around the
	 * reference date of the orbital elements (date given by the time object),
	 * but for a different date (time object) the same object could be unavailable
	 * in case the ephemerides are expected to be inaccurate.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param targetName Target's name.
	 * @throws JPARSECException If the object is not found.
	 */
	public static void setEphemerisElementAccordingToBodyType(TimeElement time, ObserverElement obs, EphemerisElement eph, String targetName) throws JPARSECException {
		eph.algorithm = EphemerisElement.ALGORITHM.MOSHIER;
		LANGUAGE lang = Translate.getDefaultLanguage();
		Translate.setDefaultLanguage(Translate.LANGUAGE.SPANISH);
		TARGET target = Target.getID(targetName);
		if (target == TARGET.NOT_A_PLANET) target = Target.getIDFromEnglishName(targetName);
		Translate.setDefaultLanguage(lang);
		boolean isPlanet = target.isPlanet();
		eph.targetBody = target;
		if (isPlanet || target == TARGET.Moon) return;
		boolean isNaturalSatellite = target.isNaturalSatellite();
		if (isNaturalSatellite) {
			eph.algorithm = EphemerisElement.ALGORITHM.NATURAL_SATELLITE;
			return;
		}

		double jd = TimeScale.getJD(time, obs, eph, SCALE.UNIVERSAL_TIME_UTC);
		AstroDate astro = new AstroDate(jd);
		boolean valid = true;
		if (!Configuration.isAcceptableDateForAsteroids(astro, false)) {
			String p = Configuration.updateAsteroidsInTempDir(astro);
			if (p == null) valid = false;
		} else {
			if (!Configuration.isAcceptableDateForAsteroids(astro, true)) {
				String p = Configuration.updateAsteroidsInTempDir(astro);
				if (p == null) {
					OrbitEphem.setAsteroidsFromExternalFile(null);
				}
			}
		}
		int index = -1;
		if (valid) index = OrbitEphem.getIndexOfAsteroid(targetName);
		if (index >= 0) {
			OrbitalElement new_orbit = OrbitEphem.getOrbitalElementsOfAsteroid(index);
			eph.targetBody = TARGET.Asteroid;
			eph.orbit = new_orbit;
			eph.algorithm = EphemerisElement.ALGORITHM.ORBIT;
		} else {
			valid = true;
			if (!Configuration.isAcceptableDateForTransNeptunians(astro, false)) {
				String p = Configuration.updateTransNeptuniansInTempDir(astro);
				if (p == null) valid = false;
			} else {
				if (!Configuration.isAcceptableDateForTransNeptunians(astro, true)) {
					String p = Configuration.updateTransNeptuniansInTempDir(astro);
					if (p == null) {
						OrbitEphem.setTransNeptuniansFromExternalFile(null);
					}
				}
			}
			if (valid) index = OrbitEphem.getIndexOfTransNeptunian(targetName);
			if (index >= 0) {
				OrbitalElement new_orbit = OrbitEphem.getOrbitalElementsOfTransNeptunian(index);
				eph.targetBody = TARGET.Asteroid;
				eph.orbit = new_orbit;
				eph.algorithm = EphemerisElement.ALGORITHM.ORBIT;
			} else {
				valid = true;
				if (!Configuration.isAcceptableDateForComets(astro, false)) {
					String p = Configuration.updateCometsInTempDir(astro);
					if (p == null) valid = false;
				} else {
					if (!Configuration.isAcceptableDateForComets(astro, true)) {
						String p = Configuration.updateCometsInTempDir(astro);
						if (p == null) {
							OrbitEphem.setCometsFromExternalFile(null);
						}
					}
				}
				if (valid) index = OrbitEphem.getIndexOfComet(targetName);
				if (index >= 0) {
					OrbitalElement new_orbit = OrbitEphem.getOrbitalElementsOfComet(index);
					eph.targetBody = TARGET.Comet;
					eph.orbit = new_orbit;
					eph.algorithm = EphemerisElement.ALGORITHM.ORBIT;
				} else {
					ReadFile re = new ReadFile();
					re.setFormat(ReadFile.FORMAT.JPARSEC_SKY2000);
					re.setPath(StarEphem.PATH_TO_SkyMaster2000_JPARSEC_FILE);
					re.readFileOfStars();
					index = re.searchByName(targetName);
					if (index < 0) {
						// Support for proper star names
						ArrayList<String> names = ReadFile.readResource(FileIO.DATA_SKY_DIRECTORY + "star_names.txt");
						for (int n = 0; n < names.size(); n++)
						{
							String line = names.get(n);
							int aa = line.toLowerCase().indexOf(targetName.toLowerCase());
							if (aa >= 0)
							{
								String proper_name = FileIO.getField(1, line, ";", true);
								index = re.searchByName(proper_name);
							}
						}
					}
					if (index >= 0) {
						eph.targetBody = TARGET.NOT_A_PLANET;
						eph.targetBody.setIndex(index);
						eph.algorithm = EphemerisElement.ALGORITHM.STAR;
					} else {
						double jd_tt = TimeScale.getJD(time, obs, eph, SCALE.TERRESTRIAL_TIME);
						int probe = Spacecraft.searchProbe(targetName, jd_tt);
						if (probe >= 0) {
							OrbitalElement new_orbit = (OrbitalElement) Spacecraft.getProbeElement(probe);
							eph.targetBody = TARGET.NOT_A_PLANET;
							eph.algorithm = EphemerisElement.ALGORITHM.PROBE;
							eph.orbit = new_orbit;
						} else {
							index = SatelliteEphem.getArtificialSatelliteTargetIndex(targetName);
							if (index >= 0) {
								eph.targetBody = TARGET.NOT_A_PLANET;
								eph.targetBody.setIndex(index);
								eph.algorithm = EphemerisElement.ALGORITHM.ARTIFICIAL_SATELLITE;
							} else {
	//							try {
	//								specialSimbad = SimbadQuery.query(args[9]);
	//							} catch (Exception exc) {
									throw new JPARSECException("Could not find object "+targetName+" (inexistent or unavailable for this date)");
	//							}
							}
						}
					}
				}
			}
		}
	}
	/**
	 * Obtain light time distance in days from the position of the object and
	 * the observer, taking into account the ephemeris type.
	 *
	 * @param geo_eq Goecentric equatorial coordinates of object.
	 * @param topo Topocentric equatorial coordinates of observer.
	 * @param eph Ephemeris object defining the ephemeris properties.
	 * @return Light time in days. Will be null if ephemeris are geometric.
	 */
	public static double getTopocentricLightTime(double geo_eq[], double topo[], EphemerisElement eph)
	{
		// Obtain geocentric positions
		double p[] = Ephem.getGeocentricPosition(geo_eq, topo);
		double r = Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]);

		// Obtain light time to correct the time of ephemeris
		if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.GEOMETRIC)
			r = 0.0;
		double light_time = Constant.LIGHT_TIME_DAYS_PER_AU * r;

		return light_time;
	}

	/**
	 * Obtain geocentric equatorial coordinates.
	 *
	 * @param p_obj Equatorial vector from origin to object.
	 * @param p_earth Equatorial vector from origin to observer.
	 * @return Array with x, y, z coordinates.
	 */
	public static double[] getGeocentricPosition(double p_obj[], double p_earth[])
	{
		return Functions.substract(p_obj, p_earth);
	}

	/**
	 * Obtain geocentric ecliptic coordinates.
	 *
	 * @param loc_elem LocationElement for the outer object.
	 * @param loc_earth LocationElement for the Earth or the mother object.
	 * @return Array with x, y, z coordinates.
	 */
	public static double[] getGeocentricPosition(LocationElement loc_elem, LocationElement loc_earth)
	{
		// Obtain ecliptic coordinates
		double p1[] = LocationElement.parseLocationElement(loc_elem);
		double p2[] = LocationElement.parseLocationElement(loc_earth);

		// Obtain geocentric positions
		return Ephem.getGeocentricPosition(p1, p2);
	}

	/**
	 * Transform coordinates from J2000 mean pole to ICRS frame. From NOVAS
	 * package, based on Hilton and Honenkerk 2004, <I>Astronomy and
	 * Astrophysics 413, 765-770, EQ. (6) AND (8)</I>.
	 *
	 * @param geo_eq Geocentric rectangular equatorial coordinates of the
	 *        object, mean equinox J2000.
	 * @return vector (x, y, z) with the corrected frame.
	 */
	public static double[] DynamicaltoICRSFrame(double[] geo_eq)
	{
		// XI0, ETA0, AND DA0 ARE ICRS FRAME BIASES IN ARCSECONDS TAKEN
		// FROM IERS CONVENTIONS (2003), CHAPTER 5
		double XI0 = -0.0166170 * Constant.ARCSEC_TO_RAD; // -0.016617140689 ?
		double ETA0 = -0.0068192 * Constant.ARCSEC_TO_RAD;
		double DA0 = -0.01460 * Constant.ARCSEC_TO_RAD;

		// COMPUTE ELEMENTS OF ROTATION MATRIX (TO FIRST ORDER)
		double XX = 1.0;
		double YX = -DA0 * Constant.ARCSEC_TO_RAD;
		double ZX = XI0 * Constant.ARCSEC_TO_RAD;
		double XY = DA0 * Constant.ARCSEC_TO_RAD;
		double YY = 1.0;
		double ZY = ETA0 * Constant.ARCSEC_TO_RAD;
		double XZ = -XI0 * Constant.ARCSEC_TO_RAD;
		double YZ = -ETA0 * Constant.ARCSEC_TO_RAD;
		double ZZ = 1.0;

		// INCLUDE SECOND-ORDER CORRECTIONS TO DIAGONAL ELEMENTS
		XX = 1.0 - 0.5 * (YX * YX + ZX * ZX);
		YY = 1.0 - 0.5 * (YX * YX + ZY * ZY);
		ZZ = 1.0 - 0.5 * (ZY * ZY + ZX * ZX);

		// PERFORM ROTATION FROM DYNAMICAL SYSTEM TO ICRS
		double x = XX * geo_eq[0] + YX * geo_eq[1] + ZX * geo_eq[2];
		double y = XY * geo_eq[0] + YY * geo_eq[1] + ZY * geo_eq[2];
		double z = XZ * geo_eq[0] + YZ * geo_eq[1] + ZZ * geo_eq[2];

		if (geo_eq.length > 3) {
			double vx = XX * geo_eq[3] + YX * geo_eq[4] + ZX * geo_eq[5];
			double vy = XY * geo_eq[3] + YY * geo_eq[4] + ZY * geo_eq[5];
			double vz = XZ * geo_eq[3] + YZ * geo_eq[4] + ZZ * geo_eq[5];
			return new double[] { x, y, z, vx, vy, vz};
		}
		return new double[] { x, y, z };
	}

	/**
	 * Transform coordinates from ICRS frame to J2000 mean dynamical frame. From
	 * NOVAS package, based on Hilton and Honenkerk 2004, <I>Astronomy and
	 * Astrophysics, 413, 765-770, EQ. (6) AND (8)</I>.
	 *
	 * @param geo_eq Geocentric rectangular equatorial coordinates of the
	 *        object (ICRS).
	 * @return vector (x, y, z) with the corrected frame.
	 */
	public static double[] ICRStoDynamicalFrame(double[] geo_eq)
	{
		// XI0, ETA0, AND DA0 ARE ICRS FRAME BIASES IN ARCSECONDS TAKEN
		// FROM IERS CONVENTIONS (2003), CHAPTER 5
		double XI0 = -0.0166170;
		double ETA0 = -0.0068192;
		double DA0 = -0.01460;

		// COMPUTE ELEMENTS OF ROTATION MATRIX (TO FIRST ORDER)
		double XX = 1.0;
		double YX = -DA0 * Constant.ARCSEC_TO_RAD;
		double ZX = XI0 * Constant.ARCSEC_TO_RAD;
		double XY = DA0 * Constant.ARCSEC_TO_RAD;
		double YY = 1.0;
		double ZY = ETA0 * Constant.ARCSEC_TO_RAD;
		double XZ = -XI0 * Constant.ARCSEC_TO_RAD;
		double YZ = -ETA0 * Constant.ARCSEC_TO_RAD;
		double ZZ = 1.0;

		// INCLUDE SECOND-ORDER CORRECTIONS TO DIAGONAL ELEMENTS
		XX = 1.0 - 0.5 * (YX * YX + ZX * ZX);
		YY = 1.0 - 0.5 * (YX * YX + ZY * ZY);
		ZZ = 1.0 - 0.5 * (ZY * ZY + ZX * ZX);

		// PERFORM ROTATION FROM ICRS TO DYNAMICAL SYSTEM
		double x = XX * geo_eq[0] + XY * geo_eq[1] + XZ * geo_eq[2];
		double y = YX * geo_eq[0] + YY * geo_eq[1] + YZ * geo_eq[2];
		double z = ZX * geo_eq[0] + ZY * geo_eq[1] + ZZ * geo_eq[2];

		if (geo_eq.length > 3) {
			double vx = XX * geo_eq[3] + XY * geo_eq[4] + XZ * geo_eq[5];
			double vy = YX * geo_eq[3] + YY * geo_eq[4] + YZ * geo_eq[5];
			double vz = ZX * geo_eq[3] + ZY * geo_eq[4] + ZZ * geo_eq[5];
			return new double[] { x, y, z, vx, vy, vz};
		}

		return new double[] { x, y, z };
	}

	/**
	 * Transform coordinates from FK5 to ICRS frame. Based on Hilton
	 * and Honenkerk 2004, <I>Astronomy and Astrophysics 413, 765-770.</I>.
	 *
	 * @param geo_eq Geocentric rectangular equatorial coordinates of the
	 *        object, FK5.
	 * @return vector (x, y, z) with the corrected frame.
	 */
	public static double[] FK5toICRSFrame(double[] geo_eq)
	{
		double XI0 = 9.1 / 1000.0;
		double ETA0 = -19.9 / 1000.0;
		double DA0 = -22.9 / 1000.0;

		// COMPUTE ELEMENTS OF ROTATION MATRIX (TO FIRST ORDER)
		double XX = 1.0;
		double YX = -DA0 * Constant.ARCSEC_TO_RAD;
		double ZX = XI0 * Constant.ARCSEC_TO_RAD;
		double XY = DA0 * Constant.ARCSEC_TO_RAD;
		double YY = 1.0;
		double ZY = ETA0 * Constant.ARCSEC_TO_RAD;
		double XZ = -XI0 * Constant.ARCSEC_TO_RAD;
		double YZ = -ETA0 * Constant.ARCSEC_TO_RAD;
		double ZZ = 1.0;

		// INCLUDE SECOND-ORDER CORRECTIONS TO DIAGONAL ELEMENTS
		XX = 1.0 - 0.5 * (YX * YX + ZX * ZX);
		YY = 1.0 - 0.5 * (YX * YX + ZY * ZY);
		ZZ = 1.0 - 0.5 * (ZY * ZY + ZX * ZX);

		// PERFORM ROTATION FROM FK5 TO ICRS
		double x = XX * geo_eq[0] + YX * geo_eq[1] + ZX * geo_eq[2];
		double y = XY * geo_eq[0] + YY * geo_eq[1] + ZY * geo_eq[2];
		double z = XZ * geo_eq[0] + YZ * geo_eq[1] + ZZ * geo_eq[2];

		if (geo_eq.length > 3) {
			double vx = XX * geo_eq[3] + YX * geo_eq[4] + ZX * geo_eq[5];
			double vy = XY * geo_eq[3] + YY * geo_eq[4] + ZY * geo_eq[5];
			double vz = XZ * geo_eq[3] + YZ * geo_eq[4] + ZZ * geo_eq[5];
			return new double[] { x, y, z, vx, vy, vz};
		}

		return new double[] { x, y, z };
	}

	/**
	 * Transform coordinates from ICRS to FK5 frame. Based on Hilton
	 * and Honenkerk 2004, <I>Astronomy &amp; Astrophysics 413, 765-770.</I>.
	 *
	 * @param geo_eq Geocentric rectangular equatorial coordinates of the
	 *        object, ICRS.
	 * @return vector (x, y, z) with the corrected frame.
	 */
	public static double[] ICRStoFK5Frame(double[] geo_eq)
	{
		double XI0 = 9.1 / 1000.0;
		double ETA0 = -19.9 / 1000.0;
		double DA0 = -22.9 / 1000.0;

		// COMPUTE ELEMENTS OF ROTATION MATRIX (TO FIRST ORDER)
		double XX = 1.0;
		double YX = -DA0 * Constant.ARCSEC_TO_RAD;
		double ZX = XI0 * Constant.ARCSEC_TO_RAD;
		double XY = DA0 * Constant.ARCSEC_TO_RAD;
		double YY = 1.0;
		double ZY = ETA0 * Constant.ARCSEC_TO_RAD;
		double XZ = -XI0 * Constant.ARCSEC_TO_RAD;
		double YZ = -ETA0 * Constant.ARCSEC_TO_RAD;
		double ZZ = 1.0;

		// INCLUDE SECOND-ORDER CORRECTIONS TO DIAGONAL ELEMENTS
		XX = 1.0 - 0.5 * (YX * YX + ZX * ZX);
		YY = 1.0 - 0.5 * (YX * YX + ZY * ZY);
		ZZ = 1.0 - 0.5 * (ZY * ZY + ZX * ZX);

		// PERFORM ROTATION FROM ICRS TO FK5
		double x = XX * geo_eq[0] + XY * geo_eq[1] + XZ * geo_eq[2];
		double y = YX * geo_eq[0] + YY * geo_eq[1] + YZ * geo_eq[2];
		double z = ZX * geo_eq[0] + ZY * geo_eq[1] + ZZ * geo_eq[2];

		if (geo_eq.length > 3) {
			double vx = XX * geo_eq[3] + XY * geo_eq[4] + XZ * geo_eq[5];
			double vy = YX * geo_eq[3] + YY * geo_eq[4] + YZ * geo_eq[5];
			double vz = ZX * geo_eq[3] + ZY * geo_eq[4] + ZZ * geo_eq[5];
			return new double[] { x, y, z, vx, vy, vz};
		}

		return new double[] { x, y, z };
	}

	/**
	 * Transforms a set of rectangular equatorial coordinates (x, y, z) from one
	 * frame into another.
	 * @param eq The equatorial coordinates, in J2000 equinox for FK5, ICRF, and
	 * J2000 frames, and in B1950 for FK4 frame.
	 * @param input The input frame.
	 * @param output The output frame.
	 * @return The output coordinates.
	 * @throws JPARSECException If the conversion fails, should never happen.
	 */
	public static double[] toOutputFrame(double eq[], FRAME input, FRAME output) throws JPARSECException {
		if (input == output) return eq;
		double out[] = null;
		switch (input) {
		case FK4:
			out = Precession.FK4_B1950ToFK5_J2000(eq);
			if (output == FRAME.FK5) return out;
			out = Ephem.FK5toICRSFrame(out);
			if (output == FRAME.ICRF) return out;
			out = Ephem.ICRStoDynamicalFrame(out);
			if (output == FRAME.DYNAMICAL_EQUINOX_J2000) return out;
			break;
		case FK5:
			if (output == FRAME.FK4) return Precession.FK5_J2000ToFK4_B1950(eq);
			out = Ephem.FK5toICRSFrame(eq);
			if (output == FRAME.ICRF) return out;
			out = Ephem.ICRStoDynamicalFrame(out);
			if (output == FRAME.DYNAMICAL_EQUINOX_J2000) return out;
			break;
		case DYNAMICAL_EQUINOX_J2000:
			out = Ephem.DynamicaltoICRSFrame(eq);
			if (output == FRAME.ICRF) return out;
			out = Ephem.ICRStoFK5Frame(out);
			if (output == FRAME.FK5) return out;
			out = Precession.FK5_J2000ToFK4_B1950(out);
			if (output == FRAME.FK4) return out;
			break;
		case ICRF:
			if (output == FRAME.DYNAMICAL_EQUINOX_J2000) return Ephem.ICRStoDynamicalFrame(eq);
			out = Ephem.ICRStoFK5Frame(eq);
			if (output == FRAME.FK5) return out;
			out = Precession.FK5_J2000ToFK4_B1950(out);
			if (output == FRAME.FK4) return out;
			break;
		}
		throw new JPARSECException("Unsupported frame conversion. This should never happen.");
	}

	/**
	 * Transform mean ecliptic geocentric rectangular coordinates to mean
	 * equatorial.
	 *
	 * @param pos ecliptic coordinates as an (x, y, z) or (x, y, z, vx, vy, vz)
	 *        vector.
	 * @param JD Julian day in Terrestrial Time.
	 * @param eph Ephemeris properties.
	 * @return An array containing the new coordinates.
	 * @throws JPARSECException In case JD is outside range a warning is thrown, and in case
	 * warnings should be treated as errors an exception will be thrown.
	 */
	public static double[] eclipticToEquatorial(double pos[], double JD, EphemerisElement eph) throws JPARSECException
	{
		double epsilon = Obliquity.meanObliquity(Functions.toCenturies(JD), eph);
		pos = Functions.rotateX(pos, epsilon);

		return pos;
	}

	/**
	 * Transform mean equatorial geocentric rectangular coordinates to mean
	 * ecliptic.
	 *
	 * @param pos equatorial coordinates as an (x, y, z) or (x, y, z, vx, vy,
	 *        vz) vector.
	 * @param JD Julian day in Terrestrial Time.
	 * @param eph Ephemeris properties.
	 * @return An array containing the new coordinates.
	 * @throws JPARSECException In case JD is outside range a warning is thrown, and in case
	 * warnings should be treated as errors an exception will be thrown.
	 */
	public static double[] equatorialToEcliptic(double pos[], double JD, EphemerisElement eph) throws JPARSECException
	{
		double epsilon = Obliquity.meanObliquity(Functions.toCenturies(JD), eph);
		pos = Functions.rotateX(pos, -epsilon);

		return pos;
	}

	/**
	 * Performs topocentric correction of right ascension, declination, and
	 * distance, and also diurnal aberration, and adds the results to the
	 * {@linkplain EphemElement} object.
	 * @param time Time object.
	 * @param obs The {@linkplain ObserverElement} that defines observer's position. In
	 * case the observer is not on a given body, a clone of the the input ephemerides
	 * object is returned.
	 * @param eph Ephemeris object defining the ephemeris properties.
	 * @param ephem The {@linkplain EphemElement} to take from and set the results to.
	 *
	 * @return The {@linkplain EphemElement} with the corrected values.
	 * @throws JPARSECException Thrown if the method fails, for example because
	 *         of an invalid date or reference ellipsoid.
	 */
	public static EphemElement topocentricCorrection(TimeElement time, ObserverElement obs, EphemerisElement eph,
			EphemElement ephem) throws JPARSECException
	{
		if (obs.getMotherBody() == TARGET.NOT_A_PLANET) return ephem.clone();

		// Object coordinates
		double eq_geo[] = LocationElement.parseLocationElement(ephem.getEquatorialLocation());

		// Obtain local apparent sidereal time
		double lst = SiderealTime.apparentSiderealTime(time, obs, eph);

		// Obtain topocentric rectangular coordinates (diurnal parallax). See AA
		// 1986, page D3.
		double radiusAU = obs.getGeoRad() * (obs.getEllipsoid().getEquatorialRadius() / Constant.AU);
		double correction[] = new double[] {
				radiusAU * Math.cos(obs.getGeoLat()) * Math.cos(lst),
				radiusAU * Math.cos(obs.getGeoLat()) * Math.sin(lst),
				radiusAU * Math.sin(obs.getGeoLat())};
		if (eph.frame != FRAME.DYNAMICAL_EQUINOX_J2000)
			correction = Ephem.toOutputFrame(correction, FRAME.DYNAMICAL_EQUINOX_J2000, eph.frame);

		double xtopo = eq_geo[0] - correction[0];
		double ytopo = eq_geo[1] - correction[1];
		double ztopo = eq_geo[2] - correction[2];

		// Obtain topocentric equatorial coordinates
		LocationElement loc = LocationElement.parseRectangularCoordinates(xtopo, ytopo, ztopo);

		/* Diurnal aberration */
		double dra = 0.0;
		double ddec = 0.0;
		if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT)
		{
			double factor = obs.getMotherBodyMeanRotationRate(eph) * (obs.getEllipsoid().getEquatorialRadius() * obs.getGeoRad() * 1000.0) / Constant.SPEED_OF_LIGHT;
			if (Math.cos(ephem.declination) != 0.0)
				dra = factor * Math.cos(obs.getGeoLat()) * Math.cos(lst - ephem.rightAscension) / Math.cos(ephem.declination);
			ddec = factor * Math.cos(obs.getGeoLat()) * Math.sin(ephem.declination) * Math.sin(lst - ephem.rightAscension);
		}

		/* Set values */
		EphemElement ephemOut = ephem.clone();
		ephemOut.rightAscension = loc.getLongitude() + dra;
		ephemOut.declination = loc.getLatitude() + ddec;
		ephemOut.distance = loc.getRadius();

		return ephemOut;
	}

	/**
	 * Performs topocentric correction of right ascension, declination, and
	 * distance, without diurnal aberration and using approximate trigonometric
	 * computations.
	 * @param time Time object.
	 * @param obs The {@linkplain ObserverElement} that defines observer's position. In
	 * case the observer is not on a given body, a clone of the the input ephemerides
	 * object is returned.
	 * @param eph Ephemeris object defining the ephemeris properties.
	 * @param ephem The {@linkplain EphemElement} object with geocentric position.
	 * @param lst The apparent sidereal time.
	 * @return The {@linkplain LocationElement} with the topocentric values.
	 * @throws JPARSECException Thrown if the method fails, for example because
	 *         of an invalid date or reference ellipsoid.
	 */
	public static LocationElement fastTopocentricCorrection(TimeElement time, ObserverElement obs, EphemerisElement eph,
			EphemElement ephem, double lst) throws JPARSECException
	{
		if (obs.getMotherBody() == TARGET.NOT_A_PLANET) return ephem.getEquatorialLocation();

		// Object coordinates
		double eq_geo[] = LocationElement.parseLocationElementFast(ephem.getEquatorialLocation());

		// Obtain topocentric rectangular coordinates (diurnal parallax). See AA
		// 1986, page D3.
		double radiusAU = obs.getGeoRad() * (obs.getEllipsoid().getEquatorialRadius() / Constant.AU);
		double correction[] = new double[] {
				radiusAU * FastMath.cos(obs.getGeoLat()) * FastMath.cos(lst),
				radiusAU * FastMath.cos(obs.getGeoLat()) * FastMath.sin(lst),
				radiusAU * FastMath.sin(obs.getGeoLat())};

		double xtopo = eq_geo[0] - correction[0];
		double ytopo = eq_geo[1] - correction[1];
		double ztopo = eq_geo[2] - correction[2];

		// Obtain topocentric equatorial coordinates
		return LocationElement.parseRectangularCoordinatesFast(xtopo, ytopo, ztopo);
	}

	/**
	 * Correct apparent coordinates for deflection, using an algorithm from
	 * NOVAS package, based on Murray (1981) <I>Monthly Notices Royal
	 * Astronomical Society 195, 639-648</I>. This correction is usually
	 * lower than 1 arcsecond, and can be neglected most of the times. Only
	 * for apparent coordinates.
	 *
	 * @param vep Vector from Earth (observer) to the planet (deflected body).
	 * @param ves Vector from Earth (observer) to Sun.
	 * @param vsp Vector from Sun to planet (deflected body).
	 * @param deflector Vector from sun to deflector body. (0, 0, 0) if it is
	 *        the sun.
	 * @param relative_mass Reciprocal mass of the deflector body in solar units,
	 * equals to deflector body mass / Sun mass = 1 for Sun.
	 * @return Array containing (x, y, z) corrected for deflection.
	 */
	public static double[] deflectionCorrection(double vep[], // Earth-Planet vector
			double ves[], // Earth-Sun vector
			double vsp[], // Sun-Planet vector
			double deflector[], // Sun-deflector vector
			double relative_mass) // Sun-Planet mass ratio
	{
		if (relative_mass == 0.0) return new double[] {vep[0], vep[1], vep[2]};

		// Sun-Earth vector
		double vse[] =	{ -ves[0], -ves[1], -ves[2] };

		// Deflector to Earth vector
		double deflector_to_earth[] = { vse[0] - deflector[0], vse[1] - deflector[1], vse[2] - deflector[2] };

		// Deflector to planet vector
		double deflector_to_planet[] = { vsp[0] - deflector[0], vsp[1] - deflector[1], vsp[2] - deflector[2] };

		// Pass to spherical
		LocationElement loc_sun = LocationElement.parseRectangularCoordinates(deflector_to_earth);
		LocationElement loc_plan = LocationElement.parseRectangularCoordinates(deflector_to_planet);
		LocationElement loc_geoc = LocationElement.parseRectangularCoordinates(vep);

		if (loc_sun.getRadius() == 0 || loc_plan.getRadius() == 0 || loc_geoc.getRadius() == 0) return new double[] {vep[0], vep[1], vep[2]};

		// COMPUTE NORMALIZED DOT PRODUCTS OF VECTORS
		double DOT_PLANET = vep[0] * deflector_to_planet[0] + vep[1] * deflector_to_planet[1] + vep[2] * deflector_to_planet[2];
		double DOT_EARTH = deflector_to_earth[0] * vep[0] + deflector_to_earth[1] * vep[1] + deflector_to_earth[2] * vep[2];
		double DOT_DEFLECTOR = deflector_to_planet[0] * deflector_to_earth[0] + deflector_to_planet[1] * deflector_to_earth[1] + deflector_to_planet[2] * deflector_to_earth[2];

		DOT_PLANET = DOT_PLANET / (loc_geoc.getRadius() * loc_plan.getRadius());
		DOT_EARTH = DOT_EARTH / (loc_geoc.getRadius() * loc_sun.getRadius());
		DOT_DEFLECTOR = DOT_DEFLECTOR / (loc_sun.getRadius() * loc_plan.getRadius());

		// IF GRAVITATING BODY IS OBSERVED OBJECT, OR IS ON A STRAIGHT LINE
		// TOWARD OR AWAY FROM OBSERVED OBJECT TO WITHIN 1 ARCSEC,
		// DEFLECTION IS SET TO ZERO
		if (Math.abs(DOT_DEFLECTOR) > 0.99999999999)
			return new double[] {vep[0], vep[1], vep[2]};

		// COMPUTE SCALAR FACTORS
		double FAC1 = Constant.SUN_GRAVITATIONAL_CONSTANT * 2.0 / (Math.pow(Constant.SPEED_OF_LIGHT, 2.0) * Constant.AU * 1000.0 * loc_sun
				.getRadius() * relative_mass);
		double FAC2 = 1.0 + DOT_DEFLECTOR;

		// CONSTRUCT CORRECTED POSITION VECTOR
		for (int i = 0; i < 3; i++)
		{
			double v = vep[i] / loc_geoc.getRadius() + FAC1 * (DOT_PLANET * deflector_to_earth[i] / loc_sun.getRadius() - DOT_EARTH * deflector_to_planet[i] / loc_plan
					.getRadius()) / FAC2;

			vep[i] = v * loc_geoc.getRadius();
		}

		return new double[] {vep[0], vep[1], vep[2]};
	}

	/**
	 * Correct apparent coordinates for solar and planetary deflection, using an algorithm
	 * from NOVAS package, based on Murray (1981), <I>Monthly Notices Royal
	 * Astronomical Society 195, 639-648</I>.
	 *
	 * @param vep Vector from Earth (observer) to the planet (deflected body).
	 * @param ves Vector from Earth (observer) to Sun.
	 * @param vsp Vector from Sun to planet (deflected body).
	 * @param additionalBodies Additional bodies for the deflection correction. In order
	 * of relevance, Jupiter, Saturn, Moon, Venus, Uranus, Neptune. Earth can also be added.
	 * The positions of these bodies are calculated using Moshier algorithms. Set to null to use
	 * no additional body.
	 * @param jdTDB The Julian day in TDB.
	 * @param ecliptic True if input coordinates are ecliptic, false if they are equatorial.
	 * @param obs The observer object. Can be null for the Earth's center.
	 * @return Array containing (x, y, z) corrected for deflection.
	 * @throws JPARSECException For invalid calculation dates.
	 */
	public static double[] solarAndPlanetaryDeflection(double vep[], // Earth-Planet vector
			double ves[], // Earth-Sun vector
			double vsp[], // Sun-Planet vector
			TARGET additionalBodies[],
			double jdTDB, boolean ecliptic, ObserverElement obs) throws JPARSECException
	{
		double deflector[] =	{ 0.0, 0.0, 0.0 };
		double relative_mass = 1.0;

		double out[] = Ephem.deflectionCorrection(vep, ves, vsp, deflector, relative_mass);

		if (additionalBodies != null && additionalBodies.length > 0) {
			double tlt = Functions.getNorm(vep) * Constant.LIGHT_TIME_DAYS_PER_AU;

			EphemerisElement eph = new EphemerisElement();
			eph.ephemMethod = REDUCTION_METHOD.IAU_2009;
			for (int i=0; i<additionalBodies.length; i++) {
				if (additionalBodies[i] == obs.getMotherBody()) continue; // Earth unsupported as deflector, since ephemerides are initially geocentric

				deflector = PlanetEphem.getHeliocentricEclipticPositionJ2000(jdTDB, additionalBodies[i]);
				if (!ecliptic) deflector = Ephem.eclipticToEquatorial(deflector, Constant.J2000, eph);

				// Project light-time to the gravitating body onto the incoming rays
				double nout = Functions.getNorm(out);
				double u1[] = Functions.scalarProduct(out, 1.0 / nout);
				double p[] = PlanetEphem.getGeocentricPosition(jdTDB, additionalBodies[i], 0.0, false, obs);
				if (!ecliptic) p = Ephem.eclipticToEquatorial(p, Constant.J2000, eph);
				double dlt = Functions.scalarProduct(new double[] {p[0], p[1], p[2]}, u1) * Constant.LIGHT_TIME_DAYS_PER_AU;

				double tclose = jdTDB;
				if (dlt > 0.0) tclose = jdTDB - dlt;
				if (tlt < dlt) tclose = jdTDB - tlt;

				deflector = PlanetEphem.getHeliocentricEclipticPositionJ2000(tclose, additionalBodies[i]);
				if (!ecliptic) deflector = Ephem.eclipticToEquatorial(deflector, Constant.J2000, eph);

				out = Ephem.deflectionCorrection(vep, ves, vsp, deflector, additionalBodies[i].relativeMass);
			}
		}

		return out;
	}

	/**
	 * Correct apparent coordinates for solar deflection, using an algorithm
	 * from NOVAS package, based on Murray (1981), <I>Monthly Notices Royal
	 * Astronomical Society 195, 639-648</I>.
	 *
	 * @param vep Vector from Earth (observer) to the planet (deflected body).
	 * @param ves Vector from Earth (observer) to Sun.
	 * @param vsp Vector from Sun to planet (deflected body).
	 * @return Array containing (x, y, z) corrected for deflection.
	 */
	public static double[] solarDeflection(double vep[], // Earth-Planet vector
			double ves[], // Earth-Sun vector
			double vsp[]) // Sun-Planet vector
	{
		double deflector[] =	{ 0.0, 0.0, 0.0 };
		double relative_mass = 1.0;

		return Ephem.deflectionCorrection(vep, ves, vsp, deflector, relative_mass);
	}

	/**
	 * Obtain horizontal coordinates (azimuth, altitude) as seen by the
	 * observer. Resulting altitude will be apparent, not geometic, if the value
	 * of ephemeris type is {@linkplain EphemerisElement.COORDINATES_TYPE#APPARENT}. This
	 * method requires previous ephemeris calculations, since it only adds
	 * azimuth, elevation, and paralactic angle to the Ephem object.
	 *
	 * The azimuth is considered equal to zero when an object is towards north,
	 * and 90 degrees when is towards East.
	 *
	 * This method also calculates paralactic angle and corrects the magnitud for
	 * extinction if this correction is enabled in the configuration.
	 *
	 * @param time Time object.
	 * @param obs Observer object. In case the observer is not on a given body,
	 * a clone of the the input ephemerides object is returned.
	 * @param eph Ephemeris object defining the ephemeris properties.
	 * @param ephem Input coordinates.
	 * @return EphemElement with horizontal coordinates.
	 * @throws JPARSECException Thrown if the method fails, for example because
	 *         of an invalid date.
	 */
	public static EphemElement horizontalCoordinates(TimeElement time, ObserverElement obs, EphemerisElement eph,
			EphemElement ephem) throws JPARSECException
	{
		if (obs.getMotherBody() == TARGET.NOT_A_PLANET) {
			EphemElement out = ephem.clone();
			out.rise = null;
			out.set = null;
			out.transit = null;
			out.transitElevation = null;
			return out;
		}

		// Obtain local apparent sidereal time
		double lst = SiderealTime.apparentSiderealTime(time, obs, eph);

		// Hour angle
		double angh = lst - ephem.rightAscension;

		// Obtain azimuth and geometric alt
		double sinlat = Math.sin(obs.getLatitudeRad());
		double coslat = Math.cos(obs.getLatitudeRad());
		double sindec = Math.sin(ephem.declination), cosdec = Math.cos(ephem.declination);
		double cosangh = Math.cos(angh);

		// Obtain azimuth and geometric alt
		double h = sinlat * sindec + coslat * cosdec * cosangh;
		double alt = Math.asin(h);
		double y = Math.sin(angh);
		double x = cosangh * sinlat - sindec * coslat / cosdec;
		double azi = Math.PI+Math.atan2(y, x);

		// Paralactic angle
		x = (sinlat / coslat) * cosdec - sindec * cosangh;
		double p = 0.0;
		if (x != 0.0)
		{
			p = Math.atan2(y, x);
		} else
		{
			p = (y / Math.abs(y)) * Constant.PI_OVER_TWO;
		}

		// Correct altitude to obtain the apparent value if necessary
		if (obs.getMotherBody() == TARGET.EARTH && eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT) {
			double alt0 = alt;
			alt = getApparentElevation(eph, obs, alt, 50);
			double zd = Constant.PI_OVER_TWO - alt;
			double zd0 = Constant.PI_OVER_TWO - alt0;
			double refr = zd0 - zd;
			if (eph.correctEquatorialCoordinatesForRefraction
					&& refr > 0.0 && zd > 3E-4*Constant.DEG_TO_RAD) {
				LocationElement out = Ephem.correctEquatorialCoordinatesForRefraction(time, obs, eph, ephem.getEquatorialLocation());
				ephem.rightAscension = out.getLongitude();
				ephem.declination = out.getLatitude();
			}
		}

		// Set results
		EphemElement ephem_elem = ephem.clone();
		ephem_elem.elevation = alt;
		ephem_elem.azimuth = azi;
		ephem_elem.paralacticAngle = (float) Functions.normalizeRadians(p);

		// Correct apparent magnitude for extinction
		if (obs.getMotherBody() == TARGET.EARTH && eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT && eph.correctForExtinction) {
			ephem_elem.magnitude += Star.getExtinction(Constant.PI_OVER_TWO-alt, obs.getHeight() / 1000.0, 5);
			ephem_elem.surfaceBrightness = (float) Star.getSurfaceBrightness(ephem_elem.magnitude, ephem_elem.angularRadius * Constant.RAD_TO_ARCSEC);
		}


		return ephem_elem;
	}

	/**
	 * Removes the refraction correction in equatorial coordinates.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param loc Right ascension and declination referred to the equinox.
	 * @return The equatorial position without refraction correction.
	 * @throws JPARSECException If an error occurs.
	 */
	public static LocationElement removeRefractionCorrectionFromEquatorialCoordinates(TimeElement time, ObserverElement obs,
			EphemerisElement eph, LocationElement loc) throws JPARSECException {
		// Obtain local apparent sidereal time
		double lst = SiderealTime.apparentSiderealTime(time, obs, eph);

		LocationElement out = CoordinateSystem.equatorialToHorizontal(loc, lst, obs, eph, false, false);
		out.setLatitude(Ephem.getGeometricElevation(eph, obs, out.getLatitude()));
		return CoordinateSystem.horizontalToEquatorial(out, lst, obs.getLatitudeRad(), false);
	}

	/**
	 * Transforms equatorial coordinates for refraction.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param loc Right ascension and declination referred to the equinox.
	 * @return The equatorial position corrected for refraction.
	 * @throws JPARSECException If an error occurs.
	 */
	public static LocationElement correctEquatorialCoordinatesForRefraction(TimeElement time, ObserverElement obs,
			EphemerisElement eph, LocationElement loc) throws JPARSECException {
		// Obtain local apparent sidereal time
		double lst = SiderealTime.apparentSiderealTime(time, obs, eph);

		// Can be done in two ways: correcting equatorial position as explained by NOVAS (below),
		// or doing direct/inverse transformations. Unless there's an error in the algorithm below
		// that reduces accuracy, this option seems more consistent and accurate.
		LocationElement out = CoordinateSystem.equatorialToHorizontal(loc, lst, obs, eph, true, false);
		return CoordinateSystem.horizontalToEquatorial(out, lst, obs.getLatitudeRad(), false);

		/*
		LocationElement out = loc.clone();

		// Hour angle
		double angh = lst - loc.getLongitude();

		// Obtain azimuth and geometric alt
		double sinlon = Math.sin(obs.longitude);
		double coslon = Math.cos(obs.longitude);
		double sinlat = Math.sin(obs.latitude);
		double coslat = Math.cos(obs.latitude);

		double h = sinlat * Math.sin(loc.getLatitude()) + coslat * Math.cos(loc.getLatitude()) * Math.cos(angh);
		double alt0 = Math.asin(h);
		double alt = getApparentElevation(eph, obs, alt0);

		// Zenith distances
		double zd = Constant.PI_OVER_TWO - alt;
		double zd0 = Constant.PI_OVER_TWO - alt0;

		double sinzd  = Math.sin(zd);
		double coszd  = Math.cos(zd);
		double sinzd0  = Math.sin(zd0);
		double coszd0  = Math.cos(zd0);

		// Shift position vector in celestial system to account for refraction. See
		// USNO/AA technical note 1998-09
		// FORM VECTOR TOWARD LOCAL ZENITH (ORTHOGONAL TO ELLIPSOID) IN ITRS
		double uz[] = new double[] {coslat * coslon, coslat * sinlon, sinlat };
		// TRANSFORM VECTOR TO GCRS
		Matrix mat = IAU2006.getGCRS_to_ITRS(time, obs, eph).inverse();
		uz = mat.times(new Matrix(uz)).getColumn(0);

		double pr[] = loc.getRectangularCoordinates();
		pr = Functions.scalarProduct(pr, 1.0 / loc.getRadius());
		for (int i=0; i< 3; i++) {
			pr[i] = ((pr[i] - coszd0 * uz[i]) / sinzd0 ) * sinzd + uz[i] * coszd;
		}

		double proj = Math.sqrt(pr[0]*pr[0] + pr[1]*pr[1]);
		if (proj > 0.0) out.setLongitude(Math.atan2(pr[1], pr[0]));
		out.setLatitude(Math.atan2(pr[2], proj));

		return out;
		*/
	}

	/**
	 * Transforms equatorial coordinates for refraction, with an approximate but fast method.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param loc Right ascension and declination referred to the equinox.
	 * @return The equatorial position corrected for refraction.
	 * @throws JPARSECException If an error occurs.
	 */
	public static LocationElement correctEquatorialCoordinatesForRefractionFast(TimeElement time, ObserverElement obs,
			EphemerisElement eph, LocationElement loc) throws JPARSECException {
		// Obtain local apparent sidereal time
		double lst = SiderealTime.greenwichMeanSiderealTime(time, obs, eph) + obs.getLongitudeRad(); // neglect EcEquinox

		// Can be done in two ways: correcting equatorial position as explained by NOVAS (below),
		// or doing direct/inverse transformations. Unless there's an error in the algorithm below
		// that reduces accuracy, this option seems more consistent and accurate.
		LocationElement out = CoordinateSystem.equatorialToHorizontal(loc, lst, obs, eph, true, true);
		return CoordinateSystem.horizontalToEquatorial(out, lst, obs.getLatitudeRad(), true);
	}
	
	/**
	 * Converts to apparent elevation, by inverting method
	 * {@linkplain Ephem#getGeometricElevation(EphemerisElement, ObserverElement, double)}.
	 * The range of elevations that returns a correct value of the apparent elevation
	 * depends on the method. For Bennet formulae output is correct up to a geometric
	 * elevation of -4 deg. For Yan formulae up to -1 deg, and for numerical integration
	 * up to -3 deg.
	 * <P>
	 * This correction is automatically done for apparent coordinates.
	 *
	 * @param eph Ephemeris properties.
	 * @param obs Observer object containing values of pressure and
	 *        temperature.
	 * @param alt Geometric elevation in radians.
	 * @param maxIter maximum number of iterations. The value is calculated with
	 * an accuracy in the milliarcsecond level unles this value is low enough. A good
	 * value is 50.
	 * @return Apparent elevation in radians.
	 * @throws JPARSECException For an invalid (null) value of the observing wavelength.
	 */
	public static double getApparentElevation(EphemerisElement eph, ObserverElement obs, double alt, int maxIter) throws JPARSECException
	{
		if (obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != null) {
			if (obs.getMotherBody() == TARGET.NOT_A_PLANET) throw new JPARSECException("Observer must be on some Solar System body.");
			return alt;
		}

		double altIn = alt; // geometric
		double altLim = -1 * Constant.DEG_TO_RAD;
		if (alt < altLim && (eph.wavelength != EphemerisElement.OBSERVING_WAVELENGTH.OPTICAL_BENNET &&
				eph.wavelength != EphemerisElement.OBSERVING_WAVELENGTH.RADIO_BENNET &&
				eph.wavelength != EphemerisElement.OBSERVING_WAVELENGTH.NUMERICAL_INTEGRATION)) alt = altLim;
		altLim = -4 * Constant.DEG_TO_RAD;
		if (alt < altLim && eph.wavelength == EphemerisElement.OBSERVING_WAVELENGTH.OPTICAL_BENNET) alt = altLim;
		altLim = -3 * Constant.DEG_TO_RAD;
		if (alt < altLim && eph.wavelength == EphemerisElement.OBSERVING_WAVELENGTH.NUMERICAL_INTEGRATION) alt = altLim;
		int niter = 0;
		if (maxIter < 1) maxIter = 1;
		do {
			double altOut = Ephem.getGeometricElevation(eph, obs, alt);
			alt = altIn - (altOut-alt);
			niter ++;
		} while(Math.abs(altIn-alt) > 5.0E-9 && niter < maxIter); // to the millisecond

		return alt;
	}

	/** The refraction integrand */
	private static double REFI(double R, double DN, double DNDR) {
		return R*DNDR/(DN+R*DNDR);
	}

	/**
	*  Refractive index and derivative wrt height for the stratosphere
	*
	*  Given:
	*    RT      d    height of tropopause from centre of the Earth (metre)
	*    TT      d    temperature at the tropopause (deg K)
	*    DNT     d    refractive index at the tropopause
	*    GAMAL   d    constant of the atmospheric model = G*MD/R
	*    R       d    current distance from the centre of the Earth (metre)
	*
	*  Returned:
	*    DN      d    refractive index at R
	*    DNDR    d    rate the refractive index is changing at R
	*
	*  This routine is a version of the ATMOSSTR routine (C.Hohenkerk,
	*  HMNAO), with trivial modifications.
	*
	*  Original version by C.Hohenkerk, HMNAO, August 1984
	*  This Starlink version by P.T.Wallace, 4 July 1993
	 */
	private static double[] ATMS(double RT, double TT, double DNT, double GAMAL, double R) {
	     double B = GAMAL/TT;
	     double W = (DNT-1.0)*Math.exp(-B*(R-RT));
	     double DN = 1.0+W;
	     double DNDR = -B*W;
	     return new double[] {DN, DNDR};
	}

	/**
	*  Refractive index and derivative wrt height for the troposphere
	*
	*  Given:
	*    R0      d    height of observer from centre of the Earth (metre)
	*    T0      d    temperature at the observer (deg K)
	*    ALPHA   d    alpha          )
	*    GAMM2   d    gamma minus 2  ) see HMNAO paper
	*    DELM2   d    delta minus 2  )
	*    C1      d    useful term  )
	*    C2      d    useful term  )
	*    C3      d    useful term  ) see source
	*    C4      d    useful term  ) of sla_REFRO
	*    C5      d    useful term  )
	*    C6      d    useful term  )
	*    R       d    current distance from the centre of the Earth (metre)
	*
	*  Returned:
	*    T       d    temperature at R (deg K)
	*    DN      d    refractive index at R
	*    DNDR    d    rate the refractive index is changing at R
	*
	*  This routine is a version of the ATMOSTRO routine (C.Hohenkerk,
	*  HMNAO), with enhancements, specified by A.T.Sinclair (RGO) to
	*  handle the radio case.
	*
	*  Note that in the optical case C5 and C6 are zero.
	*
	*  Original version by C.Hohenkerk, HMNAO, August 1984
	*  This Starlink version by P.T.Wallace, 26 July 1993
	 */
	private static double[] ATMT(double R0, double T0, double ALPHA, double GAMM2, double DELM2,
			double C1, double C2, double C3, double C4, double C5, double C6, double R) {
		double T = T0-ALPHA*(R-R0);
		double TT0 = Math.max(T/T0, 0.00);
		double TT0GM2 = Math.pow(TT0, GAMM2);
		double TT0DM2 = Math.pow(TT0, DELM2);
		double DN = 1.0+(C1*TT0GM2-(C2-C5/T)*TT0DM2)*TT0;
		double DNDR = -C3*TT0GM2+(C4-C6/TT0)*TT0DM2;
		return new double[] {T, DN, DNDR};
	}
	/**
	 * Converts to geometric elevation using the adequate model depending on
	 * the observing wavelength.
	 *
	 * @param eph Ephemeris properties.
	 * @param obs Observer object.
	 * @param alt Apparent elevation in radians.
	 * @return The geometric elevation.
	 * @throws JPARSECException For an invalid (null) value of the observing wavelength.
	 */
	public static double getGeometricElevation(EphemerisElement eph, ObserverElement obs, double alt) throws JPARSECException
	{
		if (obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != null) {
			if (obs.getMotherBody() == TARGET.NOT_A_PLANET) throw new JPARSECException("Observer must be on some Solar System body.");
			return alt;
		}

		double alt_deg = alt * Constant.RAD_TO_DEG;
		if (alt_deg < -4 || alt_deg >= 90) return alt;

		if (eph.wavelength == OBSERVING_WAVELENGTH.NUMERICAL_INTEGRATION) {
			if (alt_deg < -3.1) return alt;

			/**
			*   Atmospheric refraction for radio and optical wavelengths
			*
			* 	Given:
			*    ZOBS    d  observed zenith distance of the source (radian)
			*    HM      d  height of the observer above sea level (metre)
			*    TDK     d  ambient temperature at the observer (deg K)
			*    PMB     d  pressure at the observer (millibar)
			*    RH      d  relative humidity at the observer (range 0-1)
			*    WL      d  effective wavelength of the source (micrometre)
			*    PHI     d  latitude of the observer (radian, astronomical)
			*    TLR     d  temperature lapse rate in the troposphere (degK/metre)
			*    EPS     d  precision required to terminate iteration (radian)
			*
			*  Returned:
			*    REF     d  refraction: in vacuo ZD minus observed ZD (radian)
			*
			*  Typical values for the TLR and EPS arguments are 0.0065D0
			*  and 1D-9 respectively.
			*
			*  This routine computes the refraction for zenith distances up to
			*  and a little beyond 90 deg using the method of Hohenkerk and
			*  Sinclair (NAO Technical Notes 59 and 63).  The code is a
			*  slightly modified form of the optical refraction subroutine AREF
			*  of C.Hohenkerk (HMNAO, September 1984), with extensions to support
			*  radio wavelengths.  Most of the modifications to the HMNAO optical
			*  algorithm are cosmetic;  in addition the angle arguments have been
			*  changed to radians, any value of ZOBS is allowed, and other values
			*  have been limited to safe values.  The radio expressions were
			*  devised by A.T.Sinclair (RGO - private communication), based on
			*  the Essen & Froome refractivity formula adopted in Resolution 1
			*  of the 13th International Geodesy Association General Assembly
			*  (Bulletin Geodesique 1963 p390).
			*
			*  The radio refraction is chosen by specifying WL > 100 micrometres.
			*
			*  Before use, the value of ZOBS is expressed in the range +/- Pi.
			*  If this ranged ZOBS is -ve, the result REF is computed from its
			*  absolute value before being made -ve to match.  In addition, if
			*  it has an absolute value greater than 93 deg, a fixed REF value
			*  equal to the result for ZOBS = 93 deg is returned, appropriately
			*  signed.
			*
			*  Fixed values of the water vapour exponent, height of tropopause, and
			*  height at which refraction is negligible are used.
			*
			*  Original version by C.Hohenkerk, HMNAO, September 1986.
			*  This Starlink version by P.T.Wallace, 4 July 1993.
			*/
		     // 93 degrees in radians
		     double D93 = 93 * Constant.DEG_TO_RAD;
		     // Universal gas constant
		     double GCR = Constant.GAS_CONSTANT * 1000.0;
		     // Molecular weight of dry air
		     double DMD = 28.966;
		     // Molecular weight of water vapour
		     double DMW = 18.016;
		     // Mean Earth radius (metre)
		     double S = 6378120.0;
		     // Exponent of temperature dependence of water vapour pressure
		     double DELTA = 18.36;
		     // Height of tropopause (metre)
		     double HT = 11000.0;
		     // Upper limit for refractive effects (metre)
		     double HS = 80000.0;

		     int IN,IS,K,ISTART,I,J;
		     boolean OPTIC,LOOP;
		     double ZOBS1,ZOBS2,HMOK,TDKOK,PMBOK,RHOK,WLOK,ALPHA,TOL,WLSQ,GB,A,GAMAL,GAMMA,
		     	GAMM2,DELM2,PW0,W,C1,C2,C3,C4,C5,C6,R0,DN0,DNDR0,SK0,F0,RT,TT,DNT,DNDRT,
		     	ZT,FT,DNTS,DNDRTS,SINE,ZTS,FTS,RS,DNS,DNDRS,ZS,FS,REFO,FE,FO,H,FB,FF,STEP,Z=0,R=0,
		     	RG,DN,DNDR,F,REFP=0,REFT=0; //,TG,T,TEMPO;

		     double ZOBS = Constant.PI_OVER_TWO - alt;
		     double HM = obs.getHeight();
		     double TDK = obs.getTemperature() + 273.15;
		     double PMB = obs.getPressure();
		     double RH = obs.getHumidity() * 0.01;
		     double WL = eph.wavelength.observingWavelength;
		     double PHI = obs.getLatitudeRad();
		     double TLR = 0.0065;
		     double EPS = 1.0E-9;

		     // Transform ZOBS into the normal range
		     ZOBS1 = ZOBS;
		     ZOBS2 = Math.min(Math.abs(ZOBS1),D93);

		     // Keep other arguments within safe bounds
		     HMOK = Math.min(Math.max(HM,-1000.0),10000.0);
		     TDKOK = Math.min(Math.max(TDK,100.0),500.0);
		     PMBOK = Math.min(Math.max(PMB,0.0),10000.0);
		     RHOK = Math.min(Math.max(RH,0.0),1.0);
		     WLOK = Math.max(WL,0.1);
		     ALPHA = Math.min(Math.max(Math.abs(TLR),0.001),0.01);

		     // Tolerance for iteration
		     TOL = Math.min(Math.abs(EPS),0.1)/2.0;

		     // Decide whether optical or radio case - switch at 100 micron
		     OPTIC = true;
		     if (WLOK > 100.0) OPTIC = false;

		     // Set up model atmosphere parameters defined at the observer
		     WLSQ = WLOK*WLOK;
		     GB = 9.784*(1.0-0.0026*Math.cos(2.0*PHI)-0.00000028*HMOK);
		     if (OPTIC) {
		    	 A = ((287.604+1.6288/WLSQ+0.0136/(WLSQ*WLSQ))*273.15/1013.25)*1.0E-6;
		     } else {
		    	 A = 77.624E-6;
		     }
		     GAMAL = (GB*DMD)/GCR;
		     GAMMA = GAMAL/ALPHA;
		     GAMM2 = GAMMA-2.0;
		     DELM2 = DELTA-2.0;
		     PW0 = RHOK*Math.pow(TDKOK/247.1, DELTA);
		     W = PW0*(1.0-DMW/DMD)*GAMMA/(DELTA-GAMMA);
		     C1 = A*(PMBOK+W)/TDKOK;
		     if (OPTIC) {
		    	 C2 = (A*W+11.2684E-6*PW0)/TDKOK;
		     } else {
		    	 C2 = (A*W+12.92E-6*PW0)/TDKOK;
		     }
		     C3 = (GAMMA-1.0)*ALPHA*C1/TDKOK;
		     C4 = (DELTA-1.0)*ALPHA*C2/TDKOK;
		     if (OPTIC) {
		    	 C5 = 0.0;
		    	 C6 = 0.0;
		     } else {
		    	 C5 = 371897E-6*PW0/TDKOK;
		    	 C6 = C5*DELM2*ALPHA/(TDKOK*TDKOK);
		     }

		     // At the observer
		     R0 = S+HMOK;
		     double out[] = ATMT(R0,TDKOK,ALPHA,GAMM2,DELM2,C1,C2,C3,C4,C5,C6,R0);
		     //TEMPO = out[0];
		     DN0 = out[1];
		     DNDR0 = out[2];
		     SK0 = DN0*R0*Math.sin(ZOBS2);
		     F0 = REFI(R0,DN0,DNDR0);

		     // At the tropopause in the troposphere
		     RT = S+HT;
		     out = ATMT(R0,TDKOK,ALPHA,GAMM2,DELM2,C1,C2,C3,C4,C5,C6,RT);
		     TT = out[0];
		     DNT = out[1];
		     DNDRT = out[2];
		     SINE = SK0/(RT*DNT);
		     ZT = Math.atan2(SINE,Math.sqrt(Math.max(1.0-SINE*SINE,0.0)));
		     FT = REFI(RT,DNT,DNDRT);

		     // At the tropopause in the stratosphere
		     out = ATMS(RT,TT,DNT,GAMAL,RT);
		     DNTS = out[0];
		     DNDRTS = out[1];
		     SINE = SK0/(RT*DNTS);
		     ZTS = Math.atan2(SINE,Math.sqrt(Math.max(1.0-SINE*SINE,0.0)));
		     FTS = REFI(RT,DNTS,DNDRTS);

		     // At the stratosphere limit
		     RS = S+HS;
		     out = ATMS(RT,TT,DNT,GAMAL,RS);
		     DNS = out[0];
		     DNDRS = out[1];
		     SINE = SK0/(RS*DNS);
		     ZS = Math.atan2(SINE,Math.sqrt(Math.max(1.0-SINE*SINE,0.0)));
		     FS = REFI(RS,DNS,DNDRS);

		     // Integrate the refraction integral in two parts;  first in the
		     // troposphere (K=1), then in the stratosphere (K=2)
		     REFO = -999.999;
		     IS = 16;
		     for (K=1; K<=2; K++) {
		    	 ISTART = 0;
		    	 FE = 0.0;
		    	 FO = 0.0;
		    	 if (K == 1) {
		    		 H = (ZT-ZOBS2)/ (double) IS;
		    		 FB = F0;
		    		 FF = FT;
		    	 } else {
		    		 H = (ZS-ZTS)/(double)IS;
		    		 FB = FTS;
		    		 FF = FS;
		    	 }
		    	 IN = IS-1;
		    	 IS = IS/2;
		    	 STEP = H;

		    	 // Start of iteration loop (terminates at specified precision)
		    	 LOOP = true;
		    	 while (LOOP) {
		    		 for (I=1;I<=IN;I++) {
		    			 if (I == 1 && K == 1) {
		                       Z = ZOBS2 + H;
		                       R = R0;
		    			 } else {
		                    if (I == 1 && K == 2) {
		                       Z = ZTS + H;
		                       R = RT;
		                    } else {
		                       Z = Z + STEP;
		                    }
		    			 }

		    			 // Given the zenith distance (Z) find R
		    			 RG = R;
		    			 for (J=1;J<=4;J++) {
		                       if (K == 1) {
		                          out = ATMT(R0,TDKOK,ALPHA,GAMM2,DELM2,C1,C2,C3,C4,C5,C6,RG);
		                          //TG = out[0];
		                          DN = out[1];
		                          DNDR = out[2];
		                       } else {
		                          out = ATMS(RT,TT,DNT,GAMAL,RG);
		                          DN = out[0];
		                          DNDR = out[1];
		                       }
		                       if (Z > 1E-20) RG = RG-((RG*DN-SK0/Math.sin(Z))/(DN+RG*DNDR));
		    			 }
		    			 R = RG;

		    			 // Find refractive index and integrand at R
		    			 if (K == 1) {
		    				 out = ATMT(R0,TDKOK,ALPHA,GAMM2,DELM2,C1,C2,C3,C4,C5,C6,R);
		    				 //T = out[0];
		    				 DN = out[1];
		    				 DNDR = out[2];
		    			 } else {
		    				 out = ATMS(RT,TT,DNT,GAMAL,R);
		    				 DN = out[0];
		    				 DNDR = out[1];
		    			 }

		    			 F = REFI(R,DN,DNDR);
		    			 if (ISTART == 0 && I % 2 == 0) {
		    				 FE = FE+F;
		    			 } else {
		    				 FO = FO+F;
		    			 }
		    		 }

			    	 // Evaluate the integrand using Simpson's Rule
			    	 REFP = H*(FB+4.0*FO+2.0*FE+FF)/3.0;

			    	 // Has required precision been reached?
			    	 if (Math.abs(REFP-REFO) > TOL) {
			    		 // No: prepare for next iteration
			    		 IS = 2*IS;
			    		 IN = IS;
			    		 STEP = H;
			    		 H = H/2.0;
			    		 FE = FE+FO;
			    		 FO = 0.0;
			    		 REFO = REFP;
			    		 if (ISTART == 0) ISTART=1;
			    	 } else {
			    		 // Yes: save troposphere component and terminate loop
			    		 if (K == 1) REFT=REFP;
			    		 LOOP = false;
			    	 }
			     }
			}

		     // Result
		     double REF = REFT+REFP;
		     if (ZOBS1 < 0.0) REF=-REF;
		     return Math.min(alt - REF, Constant.PI_OVER_TWO);
		}

		// I put this case here for better performance
		if (eph.wavelength == OBSERVING_WAVELENGTH.OPTICAL_BENNET) {
			// if (alt_deg > 89.9 || alt_deg < -1) return alt;
			//double r = 0.016667 * Constant.DEG_TO_RAD / Math.tan((alt_deg +  7.31 / (alt_deg + 4.4)) * Constant.DEG_TO_RAD);
			// Note here the original Bennet formulae R = R0 / tan(E + B1 / (E + B2))
			// is replaced by R = R0 * abs(tan(90 - E - B1 / (E + B2))) to allow extension to E = 90 deg
			double r = 0.016667 * Constant.DEG_TO_RAD * Math.abs(Math.tan(Constant.PI_OVER_TWO - (alt_deg +  7.31 / (alt_deg + 4.4)) * Constant.DEG_TO_RAD));
			double refr = r * ( 0.28 * obs.getPressure() / (obs.getTemperature() + 273.0));
			return Math.min(alt - refr, Constant.PI_OVER_TWO);
		}

		double A1 = 0.0, A2 = 0.0, R0 = 0.0;

		double Ts = obs.getTemperature() + Constant.TEMPERATURE_OF_0_CELSIUS_IN_K;
		double Ps = obs.getPressure();
		double P0 = Constant.ATMOSPHERE * 0.01;
		double T0 = 258.15;
		// Water vapor saturation pressure following Crane (1976), as in the ALMA memorandum
		double esat = 6.105 * FastMath.exp(25.22 * (1.0 - 273.0 / Ts) - 5.31 * Math.log(Ts / 273.0));
		double Pw = obs.getHumidity() * esat / 100.0;
		double lambda = eph.wavelength.observingWavelength;

		switch (eph.wavelength) {
		case RADIO_BENNET:
			double N0 = 77.6 * Ps / Ts - 5.6 * Pw / Ts + 3.75E5 * Pw / (Ts * Ts);
			R0 = N0 * 1.0E-6;
			double r = R0 * Math.abs(FastMath.tan(Constant.PI_OVER_TWO - (alt_deg + 5.9 / (alt_deg + 2.5)) * Constant.DEG_TO_RAD));
			double refr = r;
			return Math.min(alt - refr, Constant.PI_OVER_TWO);
		case OPTICAL_YAN:
			double lambdaMinusPoint532 = lambda - 0.532;
			double lambdaTominus2 = 1.0 / (lambda * lambda);
			double Nstp = 83.4305 + 24062.94 / (130.0 - lambdaTominus2) + 159.99 / (38.9 - lambdaTominus2);
			// Note the bug in eq. 34 of Magnum 2001, it is 1.0E-6, not 1.0E-3. Thanks P. Wallace for pointing
			// this at https://safe.nrao.edu/wiki/pub/Main/RefBendDelayCalc/RefBendDelayCalc.pdf
			double Ntp = Ps * 288.15 * (1.0 + (3.25602 - 0.00972*Ts) * Ps * 1.0E-6) / (P0 * Ts * 1.00047);
			double Nrh = Pw * (37.345 - 0.401 * lambdaTominus2) * 1.0E-3;
			N0 = Nstp * Ntp - Nrh;
			R0 = N0 * Constant.RAD_TO_ARCSEC * 1.0E-6;
			// These two regimes produces sometimes a discontinuity of 0.4" when inverting with the other function
			if (alt_deg > 15) {
				A1 = 0.6345042 + 0.6430E-4 * (Ps - P0) - 0.542E-3 * (Ts - T0) + 0.3011E-5 * (Ts - T0) * (Ts - T0);
				A1 += 0.5314E-2 * Pw + 0.97E-6 * Pw * Pw - 0.1891E-1 * lambdaMinusPoint532 + 0.565E-1 * lambdaMinusPoint532 * lambdaMinusPoint532;
				A2 = 1.302642;
				// 0.5314E-2 appears as 0.5177E-3 for alt < 15 deg. Seems to be correct anyway.
			} else {
				A1 = 0.5787089 + 0.5609E-4 * (Ps - P0) - 0.6229E-3 * (Ts - T0) + 0.2824E-5 * (Ts - T0) * (Ts - T0);
				A1 += 0.5177E-3 * Pw + 0.29E-6 * Pw * Pw - 0.1644E-1 * lambdaMinusPoint532 + 0.491E-1 * lambdaMinusPoint532 * lambdaMinusPoint532;
				A2 = 1.302474 + 0.2142E-4 * (Ps - P0) + 0.1287E-2 * Pw + 0.65E-6 * Pw * Pw - 0.6298E-2 * lambdaMinusPoint532;
				A2 += 0.189E-1 * lambdaMinusPoint532 * lambdaMinusPoint532 - 0.2623E-2 * (Ts - T0) + 0.8776E-5 * (Ts - T0) * (Ts - T0);
			}
			break;
		case RADIO_YAN:
			N0 = 77.6 * Ps / Ts - 5.6 * Pw / Ts + 3.75E5 * Pw / (Ts * Ts); // Brusaard & Watson 1995
			// N0 = 77.6 * Ps / Ts - 12.8 * Pw / Ts + 3.776E5 * Pw / (Ts * Ts); // Smith & Weintraub 1953
			R0 = N0 * Constant.RAD_TO_ARCSEC * 1.0E-6;
			if (alt_deg > 15) {
				A1 = 0.6306849 + 0.6069E-4 * (Ps - P0) - 0.2532E-4 * Pw - 0.9881E-6 * Pw * Pw - 0.5154E-3 * (Ts - T0) + 0.2880E-5 * (Ts - T0) * (Ts - T0);
				A2 = 1.302642;
			} else {
				A1 = 0.5753868 + 0.5291E-4 * (Ps - P0) - 0.2819E-4 * Pw - 0.9381E-6 * Pw * Pw - 0.5958E-3 * (Ts - T0) + 0.2657E-5 * (Ts - T0) * (Ts - T0);
				A2 = 1.301211 + 0.2003E-4 * (Ps - P0) - 0.7285E-4 * Pw + 0.2579E-5 * Pw * Pw - 0.2595E-2 * (Ts - T0) + 0.8509E-5 * (Ts - T0) * (Ts - T0);
			}
			break;
		default:
			throw new JPARSECException("Cannot calculate refraction for wavelength "+eph.wavelength);
		}

		if (alt_deg < -1) return alt;
		if (alt == 0.0) alt = 0.0001 * Constant.DEG_TO_RAD;
		double sinE = Math.sin(alt);
		double M0 = 28.9644 * 1.0E-3, Mw = 18.0152 * 1.0E-3; // Kg/mol
		double M = ((Ps - Pw) * M0 + Pw * Mw) / Ps;
		// g from AA 1992 & Saastamoinen 1972
		double g = 9.784 * (1.0 - 0.00266 * Math.cos(2.0 * obs.getLatitudeRad()) - 0.00000028 * obs.getHeight());
		double H = Constant.GAS_CONSTANT * Ts / (M * g); // in meters. Assuming T = Ts
		double r0 = obs.getEllipsoid().getRadiusAtLatitude(obs.getLatitudeRad()) * 1000.0; // also in meters
		double I = Math.tan(alt) * Math.sqrt(r0 * 0.5 / H);
		double m = A1 / (I * I / sinE + (A2 / (sinE + 13.24969 / (I * I / sinE + 173.4233))));
		double mp = 1.0 / (sinE + m);
		double refr = R0 * Math.cos(alt) * mp;
		return Math.min(alt - refr * Constant.ARCSEC_TO_RAD, Constant.PI_OVER_TWO);
	}

	/**
	 * Correct position for aberration, including relativistic effects.
	 * <P>
	 * Adapted from NOVAS package. Algorithm based on Murray (1981), <I>Monthly
	 * Notices Royal Astronomical Society 195, 639-648</I>.
	 *
	 * @param geo_pos Geocentric position of the body, corrected for light time (body
	 * is supposed static).
	 * @param earth Heliocentric (barycentric when possible) position and velocity of Earth.
	 * @param light_time Topocentric light time in days to the source.
	 * @return Geocentric coordinates.
	 * @throws JPARSECException Should never be thrown.
	 */
	public static double[] aberration(double geo_pos[], double earth[], double light_time) throws JPARSECException
	{
		if (light_time <= 0 || earth == null) return geo_pos;

		double vearth[] = new double[] { earth[3], earth[4], earth[5] };
		double p[] = geo_pos.clone();

		double TL = light_time;
		double P1MAG = TL / Constant.LIGHT_TIME_DAYS_PER_AU;
		double VEMAG = Functions.getNorm(vearth);
		if (VEMAG == 0) return geo_pos;
		double BETA = VEMAG * Constant.LIGHT_TIME_DAYS_PER_AU;
		double DOT = geo_pos[0] * vearth[0] + geo_pos[1] * vearth[1] + geo_pos[2] * vearth[2];
		double COSD = DOT / (P1MAG * VEMAG);
		double GAMMAI = Math.sqrt(1.0 - BETA * BETA);
		double P = BETA * COSD;
		double Q = (1.0 + P / (1.0 + GAMMAI)) * TL;
		double R = 1.0 + P;

		for (int i = 0; i < 3; i++)
		{
			p[i] = (GAMMAI * geo_pos[i] + Q * vearth[i]) / R;
		}

		return new double[] { p[0], p[1], p[2] };
	}

	/**
	 * Removes the correction for aberration, including relativistic effects.
	 * <P>
	 * Adapted from NOVAS package. Algorithm based on Murray (1981), <I>Monthly
	 * Notices Royal Astronomical Society 195, 639-648</I>.
	 *
	 * @param geo_pos Geocentric position of the body, corrected for light time (body
	 * is supposed static) and aberration.
	 * @param earth Heliocentric (barycentric when possible) position and velocity of Earth.
	 * @param light_time Topocentric light time in days to the source.
	 * @return Geocentric coordinates without aberration correction.
	 * @throws JPARSECException Should never be thrown.
	 */
	public static double[] removeAberrationCorrection(double geo_pos[], double earth[], double light_time) throws JPARSECException
	{
		if (light_time <= 0) return geo_pos;

		double vearth[] = new double[] { earth[3], earth[4], earth[5] };
		double p[] = geo_pos.clone();

		double TL = light_time;
		double P1MAG = TL / Constant.LIGHT_TIME_DAYS_PER_AU;
		double VEMAG = Functions.getNorm(vearth);
		if (VEMAG == 0) return geo_pos;
		double BETA = VEMAG * Constant.LIGHT_TIME_DAYS_PER_AU;
		double DOT = geo_pos[0] * vearth[0] + geo_pos[1] * vearth[1] + geo_pos[2] * vearth[2];
		double COSD = DOT / (P1MAG * VEMAG);
		double GAMMAI = Math.sqrt(1.0 - BETA * BETA);
		double P = BETA * COSD;
		double Q = (1.0 + P / (1.0 + GAMMAI)) * TL;
		double R = 1.0 + P;

		for (int i = 0; i < 3; i++)
		{
			p[i] = (geo_pos[i] * R - Q * vearth[i]) / GAMMAI;
		}

		return new double[] { p[0], p[1], p[2] };
	}

	/**
	 * Transform previous calculated position in an Ephem object from
	 * true equinox of date to true output equinox.
	 *
	 * @param ephem Input Ephem object.
	 * @param eph Ephemeris object with output equinox.
	 * @param JD_TDB Julian date of input results.
	 * @return Ephem object.
	 * @throws JPARSECException If an error occurs.
	 */
	public static EphemElement toOutputEquinox(EphemElement ephem, EphemerisElement eph, double JD_TDB) throws JPARSECException
	{
		if (eph.equinox == EphemerisElement.EQUINOX_OF_DATE) return ephem;

		EphemElement ephem_elem = ephem.clone();

		double true_eq[] = LocationElement.parseLocationElement(new LocationElement(ephem_elem.rightAscension,
				ephem_elem.declination, ephem_elem.distance));
		true_eq = Precession.precess(JD_TDB, eph.equinox, true_eq, eph);
		LocationElement final_loc = LocationElement.parseRectangularCoordinates(true_eq);
		ephem_elem.rightAscension = final_loc.getLongitude();
		ephem_elem.declination = final_loc.getLatitude();

		if (eph.targetBody != TARGET.SUN) {
			true_eq = Ephem.eclipticToEquatorial(LocationElement.parseLocationElement(new LocationElement(
					ephem_elem.heliocentricEclipticLongitude, ephem_elem.heliocentricEclipticLatitude,
					ephem_elem.distanceFromSun)), JD_TDB, eph);
			true_eq = Precession.precess(JD_TDB, eph.equinox, true_eq, eph);

			LocationElement heliocentric_loc = LocationElement.parseRectangularCoordinates(Ephem.equatorialToEcliptic(
					true_eq, eph.equinox, eph));
			ephem_elem.heliocentricEclipticLongitude = heliocentric_loc.getLongitude();
			ephem_elem.heliocentricEclipticLatitude = heliocentric_loc.getLatitude();
		}

		return ephem_elem;
	}

	/**
	 * Converts apparent equatorial coordinates to mean equatorial referred to the equinox of the
	 * input coordinates (reverting the correction for topocentric location and nutation when necessary).
	 * This method is accurate except for the Moon, whose mean position as returned by this method can
	 * be wrong by about 0.2". Topocentric correction is done in case ephemeris properties are set to
	 * topocentric, and aberration/nutation is corrected in case ephemeris are apparent, and diurnal aberration
	 * is corrected in case both previous flags are true.
	 * @param trueEq True position resulting for ephemeris calculation. Distance must be in AU.
	 * @param time Time object used for the computation of the true equatorial position.
	 * @param obs Observer object used for the computation of the true equatorial position.
	 * @param eph Ephemeris object used for the computation of the true equatorial position.
	 * @return The mean equatorial location.
	 * @throws JPARSECException If an error occurs.
	 */
	public static LocationElement toMeanEquatorial(LocationElement trueEq, TimeElement time, ObserverElement obs, EphemerisElement eph) throws JPARSECException {
		LocationElement out = trueEq.clone();

		// Obtain JD and local apparent sidereal time
		double JD = TimeScale.getJD(time, obs, eph, SCALE.TERRESTRIAL_TIME);
		double lst = SiderealTime.apparentSiderealTime(time, obs, eph);

		if (eph.isTopocentric) {
			/* Substract diurnal aberration */
			double dra = 0.0;
			double ddec = 0.0;
			if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT)
			{
				// Object coordinates
				double eq_top[] = LocationElement.parseLocationElement(out);

				// Substract topocentric rectangular coordinates (diurnal parallax). See AA
				// 1986, page D3.
				double radiusAU = obs.getGeoRad() * (obs.getEllipsoid().getEquatorialRadius() / Constant.AU);
				double correction[] = new double[] {
						radiusAU * Math.cos(obs.getGeoLat()) * Math.cos(lst),
						radiusAU * Math.cos(obs.getGeoLat()) * Math.sin(lst),
						radiusAU * Math.sin(obs.getGeoLat())};
				if (eph.frame != FRAME.DYNAMICAL_EQUINOX_J2000)
					correction = Ephem.toOutputFrame(correction, FRAME.DYNAMICAL_EQUINOX_J2000, eph.frame);

				double xgeo = eq_top[0] + correction[0];
				double ygeo = eq_top[1] + correction[1];
				double zgeo = eq_top[2] + correction[2];

				// Obtain topocentric equatorial coordinates
				out = LocationElement.parseRectangularCoordinates(xgeo, ygeo, zgeo);

				double factor = obs.getMotherBodyMeanRotationRate(eph) * (obs.getEllipsoid().getEquatorialRadius() * obs.getGeoRad() * 1000.0) / Constant.SPEED_OF_LIGHT;
				if (Math.cos(out.getLatitude()) != 0.0)
					dra = factor * Math.cos(obs.getGeoLat()) * Math.cos(lst - out.getLongitude()) / Math.cos(out.getLatitude());
				ddec = factor * Math.cos(obs.getGeoLat()) * Math.sin(out.getLatitude()) * Math.sin(lst - out.getLongitude());

				/* Set values */
				out.setLongitude(out.getLongitude() - dra);
				out.setLatitude(out.getLatitude() - ddec);

				if (Math.cos(out.getLatitude()) != 0.0)
					dra = factor * Math.cos(obs.getGeoLat()) * Math.cos(lst - out.getLongitude()) / Math.cos(out.getLatitude());
				ddec = factor * Math.cos(obs.getGeoLat()) * Math.sin(out.getLatitude()) * Math.sin(lst - out.getLongitude());

				/* Set values */
				out.setLongitude(trueEq.getLongitude() - dra);
				out.setLatitude(trueEq.getLatitude() - ddec);
			}

			// Object coordinates
			out.setRadius(trueEq.getRadius());
			double eq_top[] = LocationElement.parseLocationElement(out);

			// Substract topocentric rectangular coordinates (diurnal parallax). See AA
			// 1986, page D3.
			double radiusAU = obs.getGeoRad() * (obs.getEllipsoid().getEquatorialRadius() / Constant.AU);
			double correction[] = new double[] {
					radiusAU * Math.cos(obs.getGeoLat()) * Math.cos(lst),
					radiusAU * Math.cos(obs.getGeoLat()) * Math.sin(lst),
					radiusAU * Math.sin(obs.getGeoLat())};
			if (eph.frame != FRAME.DYNAMICAL_EQUINOX_J2000)
				correction = Ephem.toOutputFrame(correction, FRAME.DYNAMICAL_EQUINOX_J2000, eph.frame);

			double xgeo = eq_top[0] + correction[0];
			double ygeo = eq_top[1] + correction[1];
			double zgeo = eq_top[2] + correction[2];

			// Obtain topocentric equatorial coordinates
			out = LocationElement.parseRectangularCoordinates(xgeo, ygeo, zgeo);
		}

		// Substract nutation
		if (obs.getMotherBody() == TARGET.EARTH && eph.ephemType == COORDINATES_TYPE.APPARENT) {

/*			// Correct for polar motion
			if (eph.correctForPolarMotion)
			{
				double true_eq[] = out.getRectangularCoordinates();
				true_eq = Functions.rotateZ(true_eq, -lst);
				Matrix mat = IAU2006.getPolarMotionCorrectionMatrix(time, obs, eph);
				mat = mat.inverse();
				true_eq = mat.times(new Matrix(true_eq)).getColumn(0);
				true_eq = Functions.rotateZ(true_eq, lst);
				out = LocationElement.parseRectangularCoordinates(true_eq);
			}
*/
			out = LocationElement.parseRectangularCoordinates(Nutation.nutateInEquatorialCoordinates(JD, eph, out.getRectangularCoordinates(), false));
		}

		if (eph.ephemType == COORDINATES_TYPE.APPARENT && (eph.targetBody != TARGET.Moon || obs.getMotherBody() != TARGET.EARTH)) {
			double d = out.getRadius();
			double baryc[] = Ephem.eclipticToEquatorial(PlanetEphem.getGeocentricPosition(JD, TARGET.Solar_System_Barycenter, 0.0, false, obs), Constant.J2000, eph);
			double light_time = d * Constant.LIGHT_TIME_DAYS_PER_AU;
			out = LocationElement.parseRectangularCoordinates(Ephem.removeAberrationCorrection(out.getRectangularCoordinates(), baryc, light_time));
			out.setRadius(d);
		}

		return out;
	}

	/**
	 * Converts apparent equatorial coordinates to mean equatorial referred to the equinox J2000
	 * (reverting the correction for precession, topocentric location and nutation when necessary).
	 * This method is accurate except for the Moon, whose mean position as returned by this method can
	 * be wrong by about 0.2". Topocentric correction is done in case ephemeris properties are set to
	 * topocentric, and nutation is corrected in case ephemeris are apparent, and diurnal aberration
	 * is corrected in case both previous flags are true.
	 * @param trueEq True position resulting for ephemeris calculation. Distance must be in AU.
	 * @param time Time object used for the computation of the true equatorial position.
	 * @param obs Observer object used for the computation of the true equatorial position.
	 * @param eph Ephemeris object used for the computation of the true equatorial position.
	 * @return The mean equatorial location for J2000 equinox.
	 * @throws JPARSECException If an error occurs.
	 */
	public static LocationElement toMeanEquatorialJ2000(LocationElement trueEq, TimeElement time, ObserverElement obs, EphemerisElement eph) throws JPARSECException {
		LocationElement loc = Ephem.toMeanEquatorial(trueEq, time, obs, eph);
		double JD = TimeScale.getJD(time, obs, eph, SCALE.TERRESTRIAL_TIME);
		JD = eph.getEpoch(JD);
		loc = LocationElement.parseRectangularCoordinates(Precession.precessToJ2000(JD, loc.getRectangularCoordinates(), eph));
		return loc;
	}

	/**
	 * Converts mean (astrometric) equatorial coordinates to geocentric, apparent equatorial referred
	 * to the true equinox of date. This method is not designed to be extremely accurate, although it
	 * provides good accuracy for far away objects.<P>
	 * To obtain the topocentric the {@linkplain #topocentricCorrection(TimeElement, ObserverElement, EphemerisElement, EphemElement)}
	 * method can be called after by creating an ephem object and setting its equatorial position, but
	 * check that the distance in that object is set in AU. For true apparent position you can also consider calling
	 * {@linkplain #correctEquatorialCoordinatesForRefraction(TimeElement, ObserverElement, EphemerisElement, LocationElement)}.
	 * Note this method is not suitable for stars since proper motions are not considered.
	 * @param loc0 Mean (catalog) J2000 position.
	 * @param time Time object.
	 * @param observer Observer object.
	 * @param eph Ephemeris object.
	 * @return The apparent equatorial location.
	 * @throws JPARSECException If an error occurs.
	 */
	public static LocationElement fromJ2000ToApparentGeocentricEquatorial(LocationElement loc0, TimeElement time, ObserverElement observer, EphemerisElement eph) throws JPARSECException {
		LocationElement loc = loc0.clone();
		loc.setRadius(100000 * Constant.RAD_TO_ARCSEC); // 100000 pc
		double light_time = loc.getRadius() * Constant.LIGHT_TIME_DAYS_PER_AU;
		
		double equinox = TimeScale.getJD(time, observer, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		equinox = eph.getEpoch(equinox);
		double r[] = loc.getRectangularCoordinates();
		if (eph.preferPrecisionInEphemerides) {
			if (eph.ephemType == COORDINATES_TYPE.APPARENT && (eph.targetBody != TARGET.Moon || observer.getMotherBody() != TARGET.EARTH)) {
				double baryc[] = Ephem.eclipticToEquatorial(PlanetEphem.getGeocentricPosition(equinox, TARGET.Solar_System_Barycenter, 0.0, false, observer), Constant.J2000, eph);
				r = Ephem.aberration(loc.getRectangularCoordinates(), baryc, light_time);
			}
	
			r = Precession.precessFromJ2000(equinox, r, eph);
		} else {
			r = Precession.precessFromJ2000(equinox, r, eph);			
			if (eph.ephemType == COORDINATES_TYPE.APPARENT && (eph.targetBody != TARGET.Moon || observer.getMotherBody() != TARGET.EARTH)) {
				double sun[] = OrbitEphem.sun(equinox);
				sun[3] *= -1;
				sun[4] *= -1;
				sun[5] *= -1;
				double baryc[] = Ephem.eclipticToEquatorial(sun, equinox, eph);
				r = Ephem.aberration(r, baryc, light_time);
			}
		}
		
		if (observer.getMotherBody() == TARGET.EARTH && eph.ephemType == COORDINATES_TYPE.APPARENT) {
			loc = LocationElement.parseRectangularCoordinates(Nutation.nutateInEquatorialCoordinates(equinox, eph, r, true));

			// Correct for polar motion
/*			if (eph.correctForPolarMotion)
			{
				double true_eq[] = loc.getRectangularCoordinates();
				double gast = SiderealTime.greenwichApparentSiderealTime(time, observer, eph);
				true_eq = Functions.rotateZ(true_eq, -gast);
				Matrix mat = IAU2006.getPolarMotionCorrectionMatrix(time, observer, eph);
				true_eq = mat.times(new Matrix(true_eq)).getColumn(0);
				true_eq = Functions.rotateZ(true_eq, gast);
				loc = LocationElement.parseRectangularCoordinates(true_eq);
			}
*/
		}

		loc.setRadius(loc0.getRadius());
		return loc;
	}

	/**
	 * Returns the radial velocity of a Solar System object respect the observer, or the geocenter
	 * in case the ephemeris object is set to geocentric position. No relativistic corrections
	 * are applied in this method, and Moshier algorithms are used for the position of the Earth.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param planetEquatorialPosAndVel Geocentric equatorial position and velocity of the object, AU and AU/day,
	 * corrected for light-time. Note methods called getGeocentricPosition in classes for ephemerides
	 * (JPL, Moshier, etc) don't provide the adequate velocities for this method, to obtain radial velocity for
	 * planets use {@linkplain Ephem#getRadialVelocity(TimeElement, ObserverElement, EphemerisElement)}.
	 * @return Radial velocity measured respect observer in km/s.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double getRadialVelocity(TimeElement time, ObserverElement obs,
			EphemerisElement eph, double planetEquatorialPosAndVel[]) throws JPARSECException
	{
		EphemerisElement newEph = eph.clone();
		newEph.equinox = EphemerisElement.EQUINOX_J2000;
		LocationElement locEq = LocationElement.parseRectangularCoordinates(planetEquatorialPosAndVel);
		double delta = locEq.getLatitude();

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

		ELLIPSOID ref = obs.getEllipsoid();
		double ver = obs.getMotherBodyMeanRotationRate(eph) * (ref.getRadiusAtLatitude(obs.getLatitudeRad()) + obs.getHeight() * 0.001) * Math.sin(ha) * Math.cos(delta) * Math.cos(obs.getLatitudeRad());
		if (!eph.isTopocentric) ver = 0.0;

		double geoVel = -Functions.getNorm(new double[] {planetEquatorialPosAndVel[3], planetEquatorialPosAndVel[4], planetEquatorialPosAndVel[5]});
		geoVel *= Math.cos(beta) * Math.sin(lsun - lam);
		return ver + geoVel * Constant.AU / Constant.SECONDS_PER_DAY;
	}

	/**
	 * Returns the radial velocity of a Solar System object respect the observer, or the geocenter
	 * in case the ephemeris object is set to geocentric position. No relativistic corrections
	 * are applied in this method, and Moshier algorithms are used for the position of the Earth
	 * and the target body.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object with a planet as target body.
	 * @return Radial velocity measured respect observer in km/s.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double getRadialVelocity(TimeElement time, ObserverElement obs,
			EphemerisElement eph) throws JPARSECException
	{
		EphemerisElement newEph = eph.clone();
		newEph.equinox = EphemerisElement.EQUINOX_J2000;
		double JD = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		double planetEquatorialPosAndVel[] = PlanetEphem.getGeocentricPosition(JD, eph.targetBody, 0.0, false, obs);
		double lightTime = Functions.getNorm(DataSet.getSubArray(planetEquatorialPosAndVel, 0, 2)) * Constant.LIGHT_TIME_DAYS_PER_AU;
		double v1p[] = PlanetEphem.getHeliocentricEclipticPositionJ2000(JD - lightTime, eph.targetBody);
		double time_step = 0.001;
		double v2p[] = PlanetEphem.getHeliocentricEclipticPositionJ2000(JD + time_step - lightTime, eph.targetBody);
		double vp[] = { (v2p[0] - v1p[0]) / time_step, (v2p[1] - v1p[1]) / time_step, (v2p[2] - v1p[2]) / time_step};
		double v1e[] = PlanetEphem.getHeliocentricEclipticPositionJ2000(JD, obs.getMotherBody());
		double v2e[] = PlanetEphem.getHeliocentricEclipticPositionJ2000(JD + time_step, obs.getMotherBody());
		double ve[] = { (v2e[0] - v1e[0]) / time_step, (v2e[1] - v1e[1]) / time_step, (v2e[2] - v1e[2]) / time_step};

		planetEquatorialPosAndVel = Ephem.eclipticToEquatorial(new double[] {v1p[0]-v1e[0], v1p[1]-v1e[1], v1p[2]-v1e[2],
				vp[0]-ve[0], vp[1]-ve[1], vp[2]-ve[2]}, Constant.J2000, eph);

		LocationElement locEq = LocationElement.parseRectangularCoordinates(planetEquatorialPosAndVel);
		double delta = locEq.getLatitude();

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

		ELLIPSOID ref = obs.getEllipsoid();
		double ver = obs.getMotherBodyMeanRotationRate(eph) * (ref.getRadiusAtLatitude(obs.getLatitudeRad()) + obs.getHeight() * 0.001) * Math.sin(ha) * Math.cos(delta) * Math.cos(obs.getLatitudeRad());
		if (!eph.isTopocentric) ver = 0.0;

		double geoVel = -Functions.getNorm(new double[] {planetEquatorialPosAndVel[3], planetEquatorialPosAndVel[4], planetEquatorialPosAndVel[5]});
		geoVel *= Math.cos(beta) * Math.sin(lsun - lam);
		return ver + geoVel * Constant.AU / Constant.SECONDS_PER_DAY;
	}

	/**
	 * Transforms the equatorial position of a body as seen from Earth to the location
	 * as seen from a given body, rotating to account for the direction of its north
	 * pole of rotation.
	 * @param loc Mean equatorial position from Earth.
	 * @param time Time object.
	 * @param obs Observer object, containing the new body to refer the position as its
	 * 'mother body'.
	 * @param eph Ephemeris object containing the equinox of the input location.
	 * @return The new location.
	 * @throws JPARSECException If an error occurs.
	 */
	public static LocationElement getPositionFromBody(LocationElement loc, TimeElement time, ObserverElement obs, EphemerisElement eph) throws JPARSECException {
		if (obs.getMotherBody() != TARGET.NOT_A_PLANET && obs.getMotherBody() != TARGET.EARTH) {
			double JD_TDB = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);

			EphemerisElement ephIn = eph.clone();
			ephIn.targetBody = obs.getMotherBody();
			ephIn.equinox = EphemerisElement.EQUINOX_J2000;
			LocationElement np = PhysicalParameters.getBodyNorthPole(JD_TDB, ephIn);

			LocationElement loc2 = LocationElement.parseRectangularCoordinates(Precession.precessToJ2000(eph.getEpoch(JD_TDB), loc.getRectangularCoordinates(), eph));
			double dec = Constant.PI_OVER_TWO - LocationElement.getAngularDistance(np, loc2);
			double ra = LocationElement.getPositionAngle(np, loc2);
			
			ra -= getZeroRightAscension(time, obs, eph, 0.5);
			
			return new LocationElement(ra, dec, loc.getRadius());
		}

		return loc;
	}

	/**
	 * Transforms the equatorial position of a body as seen from another body to the location
	 * as seen from Earth, de-rotating to account for the direction of the north
	 * pole of rotation of the mother (original) body. This method uses IAU resolutions, which
	 * provides an acceptable accuracy.
	 * @param loc Equatorial position from a given body.
	 * @param time Time object.
	 * @param obs Observer object, containing the old body as its
	 * 'mother body'.
	 * @param eph Ephemeris object containing the equinox of the output location from Earth.
	 * @return The new location from Earth.
	 * @throws JPARSECException If an error occurs.
	 */
	public static LocationElement getPositionFromEarth(LocationElement loc, TimeElement time, ObserverElement obs, EphemerisElement eph) throws JPARSECException {
		if (obs.getMotherBody() != TARGET.NOT_A_PLANET && obs.getMotherBody() != TARGET.EARTH) {
			double JD_TDB = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
			EphemerisElement ephIn = eph.clone();
			ephIn.targetBody = obs.getMotherBody();
			ephIn.equinox = Constant.J2000;

			LocationElement np0 = PhysicalParameters.getBodyNorthPole(JD_TDB, ephIn);
			LocationElement np = new LocationElement(0.0, np0.getLatitude(), 1.0);

			LocationElement locIn = loc.clone();
			locIn.move(getZeroRightAscension(time, obs, eph, 0.5), 0, 0);
			
			double dec = Constant.PI_OVER_TWO - LocationElement.getAngularDistance(np, locIn);
			double ra = LocationElement.getPositionAngle(np, locIn) - (np.getLongitude() - np0.getLongitude());
			LocationElement out = new LocationElement(ra, dec, locIn.getRadius());
			out = LocationElement.parseRectangularCoordinates(Precession.precessFromJ2000(eph.getEpoch(JD_TDB), out.getRectangularCoordinates(), eph));
			return out;
		}

		return loc;
	}

	/**
	 * When computing the equatorial position of a body as visible from another body different from Earth 
	 * JPARSEC computes all the required calculations and rotations, but a final rotation is required to 
	 * align the right ascension with the vernal equinox of the planet. This method is used inside JPARSEC 
	 * automatically for this purpose, you do not need to call it from your code at all.
	 * <P>
	 * This will return the 'zero' right ascension for a given planet. This value can be used to get the correct 
	 * right ascension of an object as seen from a given point on the surface of a planet different from 
	 * Earth. To compute this correction to the right ascension calculated in this library this method 
	 * computes the instant of the closest equinox in time of the mother planet contained in the observer
	 * object. When the equinox computed is not the vernal the returned value is properly shifted by PI. 
	 * The vernal equinox is defined as the instant when the declination of the Sun is 0 and increasing with 
	 * time as visible from the planet center. The returned value is the planetocentric right ascension of 
	 * the Sun at that moment as computed in JPARSEC, that should be zero but it is not until that last 
	 * rotation is done.
	 * <P>
	 * So the correct right ascension is the one returned in the computation of the ephemerides minus the 
	 * value returned by this method. This correction is done automatically, is the same for all objects, 
	 * and changes slowly with time. The correction is also automatically applied to the apparent sidereal 
	 * time.
	 * <P>
	 * The resulting corrected right ascension will match the values provided in some other sources. 
	 * For instance, the Ls value for the Sun at https://www.giss.nasa.gov/tools/mars24/help/algorithm.html, 
	 * or the right ascension for the different bodies listed in Table II of the book 
	 * "Relative Geometries of the Earth, Sun, and Mars From the Year 1973 to the Year 2000", by Sue W. 
	 * Souders (NASA). For some reason the values provided by the Horizons ephemerides service at JPL 
	 * are different, at least for Mars.
	 * @param time Time object.
	 * @param obs Observer, generally for a given point on the surface of a planet different from Earth.
	 * For an observer on Earth the output correction will be zero.
	 * @param eph Ephemeris object. The precise result will slightly depend in the IAU resolution selected 
	 * here to define the planet equator.
	 * @param arcsecAccuracy Accuracy in arcseconds for the instant of declination = 0. 0.5 can be a good 
	 * value. Minimum value accepted is 0.05.
	 * @return The zero right ascension. When JPARSEC computes the position of a body from another planet, 
	 * a final rotation is done to use as right ascension the one calculated minus this value. Moshier method 
	 * is used internally.
	 * @throws JPARSECException If the body is unsupported. Currently supported bodies are any planet, 
	 * Pluto, and natural satellite.
	 */
	public static double getZeroRightAscension(TimeElement time, ObserverElement obs, EphemerisElement eph, 
			double arcsecAccuracy) throws JPARSECException {
		TARGET target = obs.getMotherBody();
		if (target == TARGET.EARTH) return 0;
		if (target == TARGET.SUN || (target.ordinal() > TARGET.Moon.ordinal() && !target.isNaturalSatellite()))
			throw new JPARSECException("Unsupported body.");

		String id = "ZeroRA_"+target.getEnglishName();
		Object o = DataBase.getDataForAnyThread(id, true);
		if (o != null) {
			double data[] = (double[]) o;
			boolean eop = eph.correctForEOP, prec = eph.preferPrecisionInEphemerides; // Avoid cloning eph
			eph.correctForEOP = eph.preferPrecisionInEphemerides = false;
			double jd0 = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
			eph.correctForEOP = eop;
			eph.preferPrecisionInEphemerides = prec;
			double dif = Math.abs(jd0 - data[0]);
			if (dif < 1) return data[1];
		}
		
		//long t0 = System.currentTimeMillis();
		if (arcsecAccuracy < 0.05) arcsecAccuracy = 0.05;
		EphemerisElement eph0 = eph.clone();
		eph0.optimizeForSpeed();
		eph0.targetBody = obs.getMotherBody();
		eph0.equinox = EphemerisElement.EQUINOX_J2000;
		double jd0 = TimeScale.getJD(time, obs, eph0, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		LocationElement sunPos = getSunPosition(jd0, obs.getMotherBody(), eph0);
		double minDec = Math.abs(sunPos.getLatitude());
		double step = 400;
		if (target == TARGET.MERCURY) step = 1;
		if (target == TARGET.VENUS) step = 2;
		if (target == TARGET.EARTH || target == TARGET.Moon) step = 5;
		if (target == TARGET.MARS || target.getCentralBody() == TARGET.MARS) step = 10;
		if (target == TARGET.JUPITER|| target.getCentralBody() == TARGET.JUPITER) step = 50;
		if (target == TARGET.SATURN|| target.getCentralBody() == TARGET.SATURN) step = 100;
		if (target == TARGET.URANUS|| target.getCentralBody() == TARGET.URANUS) step = 200;
		double step0 = step;
		
		double jd = jd0, s = minDec < 0 ? 1 : -1;
		//int niter = 0;
		while (true) {
			//niter ++;
			jd += s * step;
			sunPos = getSunPosition(jd, obs.getMotherBody(), eph0);
			double dec = Math.abs(sunPos.getLatitude());
			if (dec <= arcsecAccuracy * Constant.ARCSEC_TO_RAD) break;
			if (dec > minDec) {
				step /= 4;
				s *= -1;
			}
			minDec = dec;
		}
		//double sec = (System.currentTimeMillis() - t0) * 0.001;
		//System.out.println(niter+"/"+sec);

		// Check which equinox and correct
		if (getSunPosition(jd + step0, obs.getMotherBody(), eph0).getLatitude() < sunPos.getLatitude()) sunPos.move(Math.PI, 0, 0);
		
		DataBase.addData(id, new double[] {jd0, sunPos.getLongitude()}, true);
		return sunPos.getLongitude();
	}
	
	private static LocationElement getSunPosition(double jd_tdb, TARGET mother, EphemerisElement eph) throws JPARSECException {
		int nd = 1;
		double posH[] = PlanetEphem.getHeliocentricEclipticPositionJ2000(jd_tdb, mother);
		double posHp[] = PlanetEphem.getHeliocentricEclipticPositionJ2000(jd_tdb + nd, mother);
		double pos[] = Functions.scalarProduct(posH, -1); // Invert for Sun from planet
		double light_time = Functions.getNorm(pos) * Constant.LIGHT_TIME_DAYS_PER_AU;
		pos = Ephem.aberration(pos, new double[] {posH[0], posH[1], posH[2], 
				(posHp[0] - posH[0])/nd, (posHp[1] - posH[1])/nd, (posHp[2] - posH[2])/nd}, light_time);
		double eq[] = Ephem.eclipticToEquatorial(pos, Constant.J2000, eph); // Use J2000 ecliptic since position is J2000
		LocationElement loc = LocationElement.parseRectangularCoordinates(eq); // Mean apparent J2000 planetocentric equatorial position
		
		// Rotate to target planet maybe applying its nutation, like Ephem.getPositionFromBody. Keep J2000
		LocationElement np = PhysicalParameters.getBodyNorthPole(jd_tdb, eph);
		double dec = Constant.PI_OVER_TWO - LocationElement.getAngularDistance(np, loc);
		double ra = LocationElement.getPositionAngle(np, loc);
		return new LocationElement(ra, dec, 1); // RA here is not aligned with vernal equinox
	}

	/**
	 * Initializes the main ephemerides objects by setting them to their most common values.
	 * @param time Time object, will be set to 'now' using computer clock.
	 * @param obs Observer object, will be set to the city or observatory entered as location name.
	 * @param eph The ephemeris object, will be set to apparent, topocentric ephemerides using IAU 2006
	 * algorithms and Moshier planetary theory. The object will be optimized for speed.
	 * @param loc The name of the location for the observer. Can be a city or an observatory.
	 * @param target The target body for the ephemerides object.
	 * @throws JPARSECException If the location is not found to be a city nor an observatory.
	 */
	public static void initializeEphemObjects(TimeElement time, ObserverElement obs, EphemerisElement eph, String loc, TARGET target)
			throws JPARSECException {
		TimeElement time0 = new TimeElement();
		ObserverElement obs0;
		try {
			obs0 = new ObserverElement(City.findCity(loc));
		} catch (Exception exc) {
			obs0 = new ObserverElement(Observatory.findObservatorybyName(loc));
		}
		EphemerisElement eph0 = new EphemerisElement(target, EphemerisElement.COORDINATES_TYPE.APPARENT,
				EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.TOPOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
				EphemerisElement.FRAME.DYNAMICAL_EQUINOX_J2000, EphemerisElement.ALGORITHM.MOSHIER);
		eph0.optimizeForSpeed();

		Reflection.copyFields(time0, time);
		Reflection.copyFields(obs0, obs);
		Reflection.copyFields(eph0, eph);
	}
}
