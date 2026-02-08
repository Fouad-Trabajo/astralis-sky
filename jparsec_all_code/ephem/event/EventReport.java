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
package jparsec.ephem.event;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import jparsec.astronomy.CoordinateSystem;
import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.Functions;
import jparsec.ephem.Obliquity;
import jparsec.ephem.PhysicalParameters;
import jparsec.ephem.Precession;
import jparsec.ephem.EphemerisElement.ALGORITHM;
import jparsec.ephem.EphemerisElement.COORDINATES_TYPE;
import jparsec.ephem.Nutation;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.event.MainEvents.EVENT_TIME;
import jparsec.ephem.event.SimpleEventElement.EVENT;
import jparsec.ephem.planets.EphemElement;
import jparsec.ephem.planets.OrbitEphem;
import jparsec.ephem.planets.OrbitalElement;
import jparsec.ephem.planets.PlanetEphem;
import jparsec.ephem.probes.SDP4_SGP4;
import jparsec.ephem.probes.SatelliteEphem;
import jparsec.ephem.probes.SatelliteEphemElement;
import jparsec.ephem.probes.SatelliteOrbitalElement;
import jparsec.ephem.stars.StarElement;
import jparsec.ephem.stars.VariableStarElement;
import jparsec.graph.DataSet;
import jparsec.graph.chartRendering.RenderEclipse;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.io.ReadFile.FORMAT;
import jparsec.math.Constant;
import jparsec.math.FastMath;
import jparsec.math.Interpolation;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.AstroDate;
import jparsec.time.DateTimeOps;
import jparsec.time.SiderealTime;
import jparsec.time.TimeElement;
import jparsec.time.TimeScale;
import jparsec.time.TimeElement.SCALE;
import jparsec.time.calendar.Calendar;
import jparsec.time.calendar.CalendarGenericConversion;
import jparsec.time.calendar.Chinese;
import jparsec.time.calendar.Ecclesiastical;
import jparsec.time.calendar.Gregorian;
import jparsec.time.calendar.MayanLongCount;
import jparsec.time.calendar.CalendarGenericConversion.CALENDAR;
import jparsec.util.Configuration;
import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Translate;
import jparsec.util.Version;
import jparsec.util.Translate.LANGUAGE;
import jparsec.vo.Feed;

/**
 * Methods to report different kind of astronomical events using an RSS feed.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class EventReport {
	// private constructor so that this class cannot be instantiated.
	private EventReport() {}

	/**
	 * Variables that controls what is calculated and what is not. Everything is set to true by default except the comets
	 * and asteroids flag in occultations and conjunctions.
	 */
	public static boolean eclipses = true, lunarPhases = true, equinoxesAndSolstices = true, MercuryVenusTransits = true,
		meteorShowers = true, calendar = true, variableStars = true, craters = true, planetaryEvents = true,
		lunarPerigeeApogee = true, EarthPerihelionAphelion = true, occultationAndConjunctions = true,
		occultationsConjunctionsAddCometsAsteroids = false, moonEvents = true, moonEventsOnlySeveralNonMutualAtSameTime = true, moonEventsAlsoMutualEvents = true,
		includePlutoAsPlanet = true, cometAsteroidVisibleNakedEye = true, cratersOnlyLunarX = false, NEOs = true,
		lunarMaxMinDeclination = true, calendarDST = true, artSatTransits = true, artSatTransitsSunMoon = true, artSatIridium = true,
		lunarNeomenia = true, dichotomyAndQuadrature = true;
	/**
	 * Maximum accuracy or not for planetary events. True means a few minutes of error at most, false a few hours.
	 */
	public static boolean maximumAccuracy = true;

	/**
	 * Limiting magnitude for the conjunction between different objects. Default values are 5 for stars,
	 * 10 for objects, and 9 for planets.
	 */
	public static float occultationsConjunctionsStarMaglim = 5f, occultationsConjunctionsObjectMaglim = 10f,
		occultationsConjunctionsPlanetMaglim = 9f;

	/**
	 * A set of artificial satellites names, separated by ',', to compute transit events.
	 * Default value includes ISS, HST, and TIANGONG.
	 */
	public static String artificialSatellites = "ISS,HST,TIANGONG";

	/**
	 * Set all static flags in this class to a given value, except the 
	 * flag {@linkplain #maximumAccuracy}.
	 * @param a The value.
	 */
	public static void setEverythingTo(boolean a) {
		eclipses = a;
		MercuryVenusTransits = a;
		planetaryEvents = a;
		lunarPhases = a;
		equinoxesAndSolstices = a;
		meteorShowers = a;
		calendar = a;
		variableStars = a;
		craters = a;
		lunarPerigeeApogee = a;
		lunarMaxMinDeclination = a;
		lunarNeomenia = a;
		EarthPerihelionAphelion = a;
		occultationAndConjunctions = a;
		occultationsConjunctionsAddCometsAsteroids = a;
		moonEvents = a;
		moonEventsOnlySeveralNonMutualAtSameTime = a;
		moonEventsAlsoMutualEvents = a;
		includePlutoAsPlanet = a;
		//maximumAccuracy = a;
		cometAsteroidVisibleNakedEye = a;
		cratersOnlyLunarX = a;
		NEOs = a;
		calendarDST = a;
		artSatTransits = a;
		artSatTransitsSunMoon = a;
		artSatIridium = a;
		dichotomyAndQuadrature = a;
	}

	/**
	 * Returns the set of astronomical events between two given dates. Events are
	 * sorted in crescent order of date. Note this method requires a few hours in a
	 * reasonably fast computer to complete if everything is activated. The events calculated are:
	 * <BR>Solar and lunar eclipses.
	 * <BR>Moon phases, perigee and apogee, maximum/minimum declination.
	 * <BR>Equinoxes and solstices, Earth's perihelion and aphelion.
	 * <BR>Mercury and Venus transits on the Sun.
	 * <BR>Meteor showers.
	 * <BR>Planetary perihelions, aphelions, oppositions (or maximum elongations), and conjunctions. Mercury and Venus perigee.
	 * <BR>Planetary quadratures (outer planets) and dichotomies (Mercury/Venus).
	 * <BR>Minima/maxima of variable stars.
	 * <BR>New year in different calendars.
	 * <BR>(*) Sunrise/sunset in lunar craters Ptolemaeus, Gassendi, Plato, and Alphonsus (times of frequent Lunar Transient Phenomena) + Lunar-X phenomena.
	 * <BR>(*) Occultation of planets, stars, deep sky objects, comets and asteroids by the Moon.
	 * <BR>(*) Transits of comets or asteroids in front of the Sun.
	 * <BR>(*) Comets/asteroids visible to the naked eye.
	 * <BR>(*) Close-approach of NEOs.
	 * <BR>(*) Time change due to DST.
	 * <BR>(*) Conjunctions between outer planets, comets, and asteroids, between Moon and planets, between planets/Moon and stars, and between deep sky objects and outer planets, comets, or asteroids.
	 * <BR>(*) Eclipses, transits, shadow transits, and occultations of natural satellites in Jupiter, Saturn, and Uranus.
	 * <BR>(*) Mutual events for the satellites of Jupiter, Saturn, and Uranus.
	 * <BR>
	 * <BR>(*) Events calculated considering the location of the observer, the other events are 'geocentric'.
	 * <BR>
	 * <BR>In the last two cases calculations are performed with the 'optimize' flag enabled in the constructor of the
	 * {@linkplain MoonEvent} object. Values for precision and accuracy are 300 and 30 for normal events and
	 * 100 and 30 for mutual events, respectively.
	 *
	 * @param init Initial date.
	 * @param end Final date.
	 * @param obs The observer.
	 * @param ephIn The ephemeris properties.
	 * @return The set of events.
	 * @throws JPARSECException If an error occurs.
	 */
	public static ArrayList<SimpleEventElement> getEvents(TimeElement init, TimeElement end, ObserverElement obs, EphemerisElement ephIn) throws JPARSECException {
		EphemerisElement eph = ephIn.clone();
		eph.correctForEOP = false;
		eph.correctForPolarMotion = false;
		eph.preferPrecisionInEphemerides = false;

		double jd0 = TimeScale.getJD(init, obs, eph, SCALE.TERRESTRIAL_TIME);
		double jdf = TimeScale.getJD(end, obs, eph, SCALE.TERRESTRIAL_TIME);
		ArrayList<SimpleEventElement> list = new ArrayList<SimpleEventElement>();
		double jdl0 = TimeScale.getJD(init, obs, eph, SCALE.LOCAL_TIME);
		double jdlf = TimeScale.getJD(end, obs, eph, SCALE.LOCAL_TIME);
		AstroDate al0 = new AstroDate(jdl0);
		AstroDate alf = new AstroDate(jdlf);
		int year0 = al0.getYear(), yearf = alf.getYear();

		// Calculate events
		for (int event = 0; event <= 66; event ++) {
			double jd = jd0;
			SimpleEventElement s = null, sold = null;
			do {
				try {
					EVENT_TIME eventTime = EVENT_TIME.NEXT;
					if (jd == jd0) eventTime = EVENT_TIME.CLOSEST;
	
	//				System.out.println(jd0+"/"+jdf+"/"+jd+"/"+event);
					switch (event) {
					case 0:
						if (eclipses) s = MainEvents.MoonPhaseOrEclipse(jd, EVENT.MOON_LUNAR_ECLIPSE, eventTime);
						if (s != null) jd = s.time + 15;
						break;
					case 1:
						if (eclipses) s = MainEvents.MoonPhaseOrEclipse(jd, EVENT.MOON_SOLAR_ECLIPSE, eventTime);
						if (s != null) jd = s.time + 15;
						break;
					case 2:
						if (lunarPhases) s = MainEvents.MoonPhaseOrEclipse(jd, EVENT.MOON_FIRST_QUARTER, eventTime);
						if (s != null) jd = s.time + 15;
						break;
					case 3:
						if (lunarPhases) s = MainEvents.MoonPhaseOrEclipse(jd, EVENT.MOON_FULL, eventTime);
						if (s != null) jd = s.time + 15;
						break;
					case 4:
						if (lunarPhases) s = MainEvents.MoonPhaseOrEclipse(jd, EVENT.MOON_LAST_QUARTER, eventTime);
						if (s != null) jd = s.time + 15;
						break;
					case 5:
						if (lunarPhases) s = MainEvents.MoonPhaseOrEclipse(jd, EVENT.MOON_NEW, eventTime);
						if (s != null) jd = s.time + 15;
						break;
					case 6:
						if (equinoxesAndSolstices) {
							for (int year = year0; year <= yearf; year ++) {
								s = MainEvents.EquinoxesAndSolstices(year, EVENT.SUN_SPRING_EQUINOX);
								if (s.time > jd0 && s.time < jdf) list.add(s);
							}
						}
						s = null;
						break;
					case 7:
						if (equinoxesAndSolstices) {
							for (int year = year0; year <= yearf; year ++) {
								s = MainEvents.EquinoxesAndSolstices(year, EVENT.SUN_AUTUMN_EQUINOX);
								if (s.time > jd0 && s.time < jdf) list.add(s);
							}
						}
						s = null;
						break;
					case 8:
						if (equinoxesAndSolstices) {
							for (int year = year0; year <= yearf; year ++) {
								s = MainEvents.EquinoxesAndSolstices(year, EVENT.SUN_SUMMER_SOLSTICE);
								if (s.time > jd0 && s.time < jdf) list.add(s);
							}
						}
						s = null;
						break;
					case 9:
						if (equinoxesAndSolstices) {
							for (int year = year0; year <= yearf; year ++) {
								s = MainEvents.EquinoxesAndSolstices(year, EVENT.SUN_WINTER_SOLSTICE);
								if (s.time > jd0 && s.time < jdf) list.add(s);
							}
						}
						s = null;
						break;
					case 10:
						if (MercuryVenusTransits) s = MainEvents.getMercuryOrVenusTransit(TARGET.MERCURY, jd, jdf, maximumAccuracy);
						if (s != null) {
							s.details = Translate.getEntry(841, LANGUAGE.ENGLISH).toLowerCase()+" "+DataSet.replaceAll(s.details.toLowerCase(), ",", "\u00b0,", true);
							jd = s.time + 30;
						}
						break;
					case 11:
						if (MercuryVenusTransits) s = MainEvents.getMercuryOrVenusTransit(TARGET.VENUS, jd, jdf, maximumAccuracy);
						if (s != null) {
							s.details = Translate.getEntry(841, LANGUAGE.ENGLISH).toLowerCase()+" "+DataSet.replaceAll(s.details.toLowerCase(), ",", "\u00b0,", true);
							jd = s.time + 30;
						}
						break;
					case 12:
						if (meteorShowers) {
							for (int year = year0; year <= yearf; year ++) {
								SimpleEventElement ss[] = MainEvents.meteorShowers(year);
								for (int si = 0; si < ss.length; si ++) {
									if (ss[si].time > jd0 && ss[si].time < jdf) {
										String d = ss[si].details;
										String f[] = DataSet.toStringArray(d, "|", true);
										ss[si].eventLocation = new LocationElement(Functions.parseRightAscension(f[2].substring(0, f[2].indexOf(",")).trim()),
												Functions.parseDeclination(f[2].substring(f[2].indexOf(",") + 1)), 1.0);
										ss[si].body = Translate.getEntry(1027, LANGUAGE.ENGLISH);
										String shower = f[1];
										//if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) shower = f[0];
										d = Translate.getEntry(1022, LANGUAGE.ENGLISH)+" "+Translate.getEntry(160, LANGUAGE.ENGLISH)+" "+shower+" ("+Translate.getEntry(1024, LANGUAGE.ENGLISH).toLowerCase()+", "+Translate.getEntry(1023, LANGUAGE.ENGLISH)+" "+f[3]+")";
										ss[si].details = d;
										list.add(ss[si]);
									}
								}
							}
						}
						s = null;
						break;
					case 13:
						if (EarthPerihelionAphelion) //s = MainEvents.PerihelionAndAphelion(TARGET.EARTH, jd, EVENT.PLANET_MAXIMUM_DISTANCE_FROM_SUN, EVENT_TIME.NEXT);
							s = MainEvents.getPlanetaryEvent(TARGET.EARTH, jd, EVENT.PLANET_MAXIMUM_DISTANCE_FROM_SUN, EVENT_TIME.NEXT, maximumAccuracy);
						if (s != null) jd = s.time + 30;
						break;
					case 14:
						if (EarthPerihelionAphelion) //s = MainEvents.PerihelionAndAphelion(TARGET.EARTH, jd, EVENT.PLANET_MINIMUM_DISTANCE_FROM_SUN, EVENT_TIME.NEXT);
							s = MainEvents.getPlanetaryEvent(TARGET.EARTH, jd, EVENT.PLANET_MINIMUM_DISTANCE_FROM_SUN, EVENT_TIME.NEXT, maximumAccuracy);
						if (s != null) jd = s.time + 30;
						break;
					case 15:
						if (planetaryEvents) s = MainEvents.getPlanetaryEvent(TARGET.VENUS, jd, EVENT.PLANET_MINIMUM_DISTANCE, EVENT_TIME.NEXT, maximumAccuracy);
						if (s != null) jd = s.time + 30;
						break;
					case 16:
						if (planetaryEvents) s = MainEvents.getPlanetaryEvent(TARGET.MARS, jd, EVENT.PLANET_MINIMUM_DISTANCE, EVENT_TIME.NEXT, maximumAccuracy);
						if (s != null) jd = s.time + 30;
						break;
					case 17:
						if (planetaryEvents)
						s = MainEvents.getPlanetaryEvent(TARGET.MERCURY, jd, EVENT.PLANET_MAXIMUM_ELONGATION, EVENT_TIME.CLOSEST, maximumAccuracy);
						if (s != null) jd = s.time + 40;
						break;
					case 18:
						if (planetaryEvents) {
							s = MainEvents.getPlanetaryEvent(TARGET.MERCURY, jd, EVENT.PLANET_MINIMUM_ELONGATION, eventTime, maximumAccuracy);
							try {
								double elo = Double.parseDouble(s.details.substring(0, s.details.indexOf(",")));
								if (elo < 0.25) s = null; // Transit on Sun
							} catch (Exception exc) {}
						}
						if (s != null) jd = s.time + 40;
						break;
					case 19:
						if (planetaryEvents) s = MainEvents.getPlanetaryEvent(TARGET.VENUS, jd, EVENT.PLANET_MAXIMUM_ELONGATION, eventTime, maximumAccuracy);
						if (s != null) jd = s.time + 100;
						break;
					case 20:
						if (planetaryEvents) {
							s = MainEvents.getPlanetaryEvent(TARGET.VENUS, jd, EVENT.PLANET_MINIMUM_ELONGATION, eventTime, maximumAccuracy);
							try {
								double elo = Double.parseDouble(s.details.substring(0, s.details.indexOf(",")));
								if (elo < 0.25) s = null; // Transit on Sun
							} catch (Exception exc) {}
						}
						if (s != null) jd = s.time + 100;
						break;
					case 21:
						if (planetaryEvents) s = MainEvents.getPlanetaryEvent(TARGET.MARS, jd, EVENT.PLANET_MAXIMUM_ELONGATION, EVENT_TIME.NEXT, maximumAccuracy);
						if (s != null) {
							s.eventType = EVENT.PLANET_OPPOSITION;
							jd = s.time + 200;
						}
						break;
					case 22:
						if (planetaryEvents) s = MainEvents.getPlanetaryEvent(TARGET.MARS, jd, EVENT.PLANET_MINIMUM_ELONGATION, EVENT_TIME.NEXT, maximumAccuracy);
						if (s != null) {
							s.eventType = EVENT.PLANET_CONJUNCTION;
							jd = s.time + 200;
						}
						break;
					case 23:
						if (planetaryEvents) s = MainEvents.getPlanetaryEvent(TARGET.JUPITER, jd, EVENT.PLANET_MAXIMUM_ELONGATION, EVENT_TIME.NEXT, maximumAccuracy);
						if (s != null) {
							s.eventType = EVENT.PLANET_OPPOSITION;
							jd = s.time + 300;
						}
						break;
					case 24:
						if (planetaryEvents) s = MainEvents.getPlanetaryEvent(TARGET.JUPITER, jd, EVENT.PLANET_MINIMUM_ELONGATION, EVENT_TIME.NEXT, maximumAccuracy);
						if (s != null) {
							s.eventType = EVENT.PLANET_CONJUNCTION;
							jd = s.time + 300;
						}
						break;
					case 25:
						if (planetaryEvents) s = MainEvents.getPlanetaryEvent(TARGET.SATURN, jd, EVENT.PLANET_MAXIMUM_ELONGATION, EVENT_TIME.NEXT, maximumAccuracy);
						if (s != null) {
							s.eventType = EVENT.PLANET_OPPOSITION;
							jd = s.time + 300;
						}
						break;
					case 26:
						if (planetaryEvents) s = MainEvents.getPlanetaryEvent(TARGET.SATURN, jd, EVENT.PLANET_MINIMUM_ELONGATION, EVENT_TIME.NEXT, maximumAccuracy);
						if (s != null) {
							s.eventType = EVENT.PLANET_CONJUNCTION;
							jd = s.time + 300;
						}
						break;
					case 27:
						if (planetaryEvents) s = MainEvents.getPlanetaryEvent(TARGET.URANUS, jd, EVENT.PLANET_MAXIMUM_ELONGATION, EVENT_TIME.NEXT, maximumAccuracy);
						if (s != null) {
							s.eventType = EVENT.PLANET_OPPOSITION;
							jd = s.time + 300;
						}
						break;
					case 28:
						if (planetaryEvents) s = MainEvents.getPlanetaryEvent(TARGET.URANUS, jd, EVENT.PLANET_MINIMUM_ELONGATION, EVENT_TIME.NEXT, maximumAccuracy);
						if (s != null) {
							s.eventType = EVENT.PLANET_CONJUNCTION;
							jd = s.time + 300;
						}
						break;
					case 29:
						if (planetaryEvents) s = MainEvents.getPlanetaryEvent(TARGET.NEPTUNE, jd, EVENT.PLANET_MAXIMUM_ELONGATION, EVENT_TIME.NEXT, maximumAccuracy);
						if (s != null) {
							s.eventType = EVENT.PLANET_OPPOSITION;
							jd = s.time + 300;
						}
						break;
					case 30:
						if (planetaryEvents) s = MainEvents.getPlanetaryEvent(TARGET.NEPTUNE, jd, EVENT.PLANET_MINIMUM_ELONGATION, EVENT_TIME.NEXT, maximumAccuracy);
						if (s != null) {
							s.eventType = EVENT.PLANET_CONJUNCTION;
							jd = s.time + 300;
						}
						break;
					case 31:
						if (planetaryEvents) s = MainEvents.getPlanetaryEvent(TARGET.MARS, jd, EVENT.PLANET_MAXIMUM_DISTANCE_FROM_SUN, EVENT_TIME.NEXT, maximumAccuracy);
						if (s != null) jd = s.time + 300;
						break;
					case 32:
						if (planetaryEvents) s = MainEvents.getPlanetaryEvent(TARGET.MARS, jd, EVENT.PLANET_MINIMUM_DISTANCE_FROM_SUN, EVENT_TIME.NEXT, maximumAccuracy);
						if (s != null) jd = s.time + 300;
						break;
					case 33:
						if (planetaryEvents) s = MainEvents.getPlanetaryEvent(TARGET.JUPITER, jd, EVENT.PLANET_MAXIMUM_DISTANCE_FROM_SUN, EVENT_TIME.NEXT, maximumAccuracy);
						if (s != null) jd = s.time + 300;
						break;
					case 34:
						if (planetaryEvents) s = MainEvents.getPlanetaryEvent(TARGET.JUPITER, jd, EVENT.PLANET_MINIMUM_DISTANCE_FROM_SUN, EVENT_TIME.NEXT, maximumAccuracy);
						if (s != null) jd = s.time + 300;
						break;
					case 35:
						if (planetaryEvents) s = MainEvents.getPlanetaryEvent(TARGET.SATURN, jd, EVENT.PLANET_MAXIMUM_DISTANCE_FROM_SUN, EVENT_TIME.NEXT, maximumAccuracy);
						if (s != null) jd = s.time + 300;
						break;
					case 36:
						if (planetaryEvents) s = MainEvents.getPlanetaryEvent(TARGET.SATURN, jd, EVENT.PLANET_MINIMUM_DISTANCE_FROM_SUN, EVENT_TIME.NEXT, maximumAccuracy);
						if (s != null) jd = s.time + 300;
						break;
					case 37:
						if (planetaryEvents) s = MainEvents.getPlanetaryEvent(TARGET.URANUS, jd, EVENT.PLANET_MAXIMUM_DISTANCE_FROM_SUN, EVENT_TIME.NEXT, maximumAccuracy);
						if (s != null) jd = s.time + 300;
						break;
					case 38:
						if (planetaryEvents) s = MainEvents.getPlanetaryEvent(TARGET.URANUS, jd, EVENT.PLANET_MINIMUM_DISTANCE_FROM_SUN, EVENT_TIME.NEXT, maximumAccuracy);
						if (s != null) jd = s.time + 300;
						break;
					case 39:
						if (planetaryEvents) s = MainEvents.getPlanetaryEvent(TARGET.NEPTUNE, jd, EVENT.PLANET_MAXIMUM_DISTANCE_FROM_SUN, EVENT_TIME.NEXT, maximumAccuracy);
						if (s != null) jd = s.time + 100;
						break;
					case 40:
						if (planetaryEvents) s = MainEvents.getPlanetaryEvent(TARGET.NEPTUNE, jd, EVENT.PLANET_MINIMUM_DISTANCE_FROM_SUN, EVENT_TIME.NEXT, maximumAccuracy);
						if (s != null) jd = s.time + 300;
						break;
					case 41:
						if (planetaryEvents) {
							s = MainEvents.SaturnRingsEdgeOn(jd, eventTime);
							if (s.time < jd && eventTime == EVENT_TIME.CLOSEST) 
								s = MainEvents.SaturnRingsEdgeOn(jd, EVENT_TIME.NEXT);
						}
						if (s != null) jd = s.time + 300;
						break;
					case 42:
						if (planetaryEvents) {
							s = MainEvents.SaturnRingsMaximumAperture(jd, eventTime);
							if (s.time < jd && eventTime == EVENT_TIME.CLOSEST) 
								s = MainEvents.SaturnRingsMaximumAperture(jd, EVENT_TIME.NEXT);
						}
						if (s != null) jd = s.time + 300;
						break;
					case 43:
						if (variableStars) {
							for (int year = year0; year <= yearf; year ++) {
								ArrayList<String[]> a = getVariableStarsEphemerides(init, end, obs, VARIABLE_STARS_ALL,
										5);
								if (a != null) {
									for (int i=0; i<a.size(); i++) {
										String d[] = a.get(i);
										double jdl = 0;
										int in = d[d.length-1].indexOf(";");
										if (in > 0) {
											jdl = Double.parseDouble(d[d.length-1].substring(0, in).trim());
											if (jdl < jd0 - 1 || jdl > jdf + 1) continue;
											jdl = Double.parseDouble(d[d.length-1].substring(in+1).trim());
											if (jdl < jd0 - 1 || jdl > jdf + 1) continue;
										} else {
											jdl = Double.parseDouble(d[d.length-1]);
											if (jdl < jd0 - 1 || jdl > jdf + 1) continue;
										}
										if (jdl > jd0 - 1 && jdl < jdf + 1) {
											double jde = TimeScale.getJD(new TimeElement(jdl, SCALE.LOCAL_TIME), obs, eph, SCALE.TERRESTRIAL_TIME);
											if (jde > jd0 && jde < jdf) {
												if (d[0].equals("MIRA")) {
													// mag range, jd of next maxima, jd of next minima
													String details = Translate.getEntry(1025, LANGUAGE.ENGLISH)+" of "+d[1]+" (mag "+d[4]+")";
													//d[4]+", "+d[6].substring(0, d[6].indexOf(";")).trim()+", "+d[6].substring(d[6].indexOf(";")+1).trim();
													s = new SimpleEventElement(jde, EVENT.VARIABLE_STAR_MIRA, details);
												} else {
													// mag range, phase, jd of next minima
													String details = Translate.getEntry(1025, LANGUAGE.ENGLISH)+" of "+d[1]+" (mag "+d[4]+")";
													//d[4]+", "+d[5].substring(0, d[5].indexOf(";")).trim()+", "+d[6];
													s = new SimpleEventElement(jde, EVENT.VARIABLE_STAR_ECLIPSING, details);
												}
												s.body = d[1];
												s.eventLocation = new LocationElement(Double.parseDouble(d[2]), Double.parseDouble(d[3]), 1.0);
												list.add(s);
												if (d[0].equals("MIRA")) {
													SimpleEventElement s2 = s.clone();
													s2.details = Translate.getEntry(1026, LANGUAGE.ENGLISH)+" of "+d[1]+" (mag "+d[4]+")";
													s2.time = Double.parseDouble(d[6].substring(0, d[6].indexOf(";")).trim());
													list.add(s2);
												}
											}
										}
									}
								}
							}
						}
						s = null;
						break;
					case 44:
						if (calendar) {
							ArrayList<SimpleEventElement> a = getCalendarEvents(jd0, jdf, year0, yearf, obs, eph);
							for (int si = 0; si < a.size(); si ++) {
								s = a.get(si);
								if (s.time > jd0 && s.time < jdf) list.add(s);
							}
						}
						s = null;
						break;
					case 45:
						if (lunarPerigeeApogee) s = MainEvents.getPlanetaryEvent(TARGET.Moon, jd, EVENT.PLANET_MAXIMUM_DISTANCE, EVENT_TIME.NEXT, maximumAccuracy);
						if (s != null) jd = s.time + 10;
						break;
					case 46:
						if (lunarPerigeeApogee) s = MainEvents.getPlanetaryEvent(TARGET.Moon, jd, EVENT.PLANET_MINIMUM_DISTANCE, EVENT_TIME.NEXT, maximumAccuracy);
						if (s != null) jd = s.time + 10;
						break;
					case 47:
						if (craters) {
							ArrayList<SimpleEventElement> a = getLTP(jd0, jdf, obs, eph);
							for (int si = 0; si < a.size(); si ++) {
								s = a.get(si);
								if (s.time > jd0 && s.time < jdf) list.add(s);
							}
						}
						s = null;
						break;
					case 48:
						if (occultationAndConjunctions) {
							ArrayList<SimpleEventElement> a = getOccCon(jd0, jdf, obs, eph);
							for (int si = 0; si < a.size(); si ++) {
								s = a.get(si);
								if (s.time > jd0 && s.time < jdf) list.add(s);
							}
						}
						s = null;
						break;
					case 49:
						if (cometAsteroidVisibleNakedEye) {
							ArrayList<SimpleEventElement> a = getCometAsteroidVisibleNakedEye(jd0, jdf, obs, eph);
							for (int si = 0; si < a.size(); si ++) {
								s = a.get(si);
								if (s.time > jd0 && s.time < jdf) list.add(s);
							}
						}
						s = null;
						break;
					case 50:
						if (NEOs) {
							ArrayList<SimpleEventElement> a = getCloseApproachOfNEOs(jd0, jdf, obs, eph);
							for (int si = 0; si < a.size(); si ++) {
								s = a.get(si);
								if (s.time > jd0 && s.time < jdf) list.add(s);
							}
						}
						s = null;
						break;
					case 51:
						if (moonEvents) {
							// Return events for Jupiter, Saturn, and Uranus
							if (Runtime.getRuntime().availableProcessors() > 1) {
								thread0 th0 = new thread0(jd0, jdf, obs, eph);
								thread1 th1 = new thread1(jd0, jdf, obs, eph);
								Thread t0 = new Thread(th0);
								Thread t1 = new Thread(th1);
								t0.start();
								t1.start();
								try {
									t0.join();
									t1.join();
									if (th0.a != null) {
										for (int si = 0; si < th0.a.size(); si ++) {
											s = th0.a.get(si);
											if (s.time > jd0 && s.time < jdf) list.add(s);
										}
									}
									if (th1.a != null) {
										for (int si = 0; si < th1.a.size(); si ++) {
											s = th1.a.get(si);
											if (s.time > jd0 && s.time < jdf) list.add(s);
										}
									}
								} catch (Exception exc) {
									exc.printStackTrace();
								}
							} else {
								eph.targetBody = TARGET.JUPITER;
								ArrayList<SimpleEventElement> a = getMoonEvents(jd0, jdf, obs, eph);
								for (int si = 0; si < a.size(); si ++) {
									s = a.get(si);
									if (s.time > jd0 && s.time < jdf) list.add(s);
								}
								eph.targetBody = TARGET.SATURN;
								a = getMoonEvents(jd0, jdf, obs, eph);
								for (int si = 0; si < a.size(); si ++) {
									s = a.get(si);
									if (s.time > jd0 && s.time < jdf) list.add(s);
								}
								eph.targetBody = TARGET.URANUS;
								a = getMoonEvents(jd0, jdf, obs, eph);
								for (int si = 0; si < a.size(); si ++) {
									s = a.get(si);
									if (s.time > jd0 && s.time < jdf) list.add(s);
								}
							}
						}
						s = null;
						break;
					case 52:
						if (lunarMaxMinDeclination) s = LunarEvent.MoonMaximumDeclination(jd, EVENT_TIME.NEXT);
						if (s != null) jd = s.time + 10;
						break;
					case 53:
						if (lunarMaxMinDeclination) s = LunarEvent.MoonMinimumDeclination(jd, EVENT_TIME.NEXT);
						if (s != null) jd = s.time + 10;
						break;
					case 54:
						if (calendarDST) {
							double times[] = TimeScale.getDSTStartEnd((new AstroDate((new AstroDate(jd)).getYear(), 6, 1)).jd(), obs);
							double jd1 = TimeScale.getJD(new TimeElement(times[0], SCALE.UNIVERSAL_TIME_UTC), obs, eph, SCALE.TERRESTRIAL_TIME);
							double jd2 = TimeScale.getJD(new TimeElement(times[1], SCALE.UNIVERSAL_TIME_UTC), obs, eph, SCALE.TERRESTRIAL_TIME);
							if (jd1 > jd0 && jd1 < jdf) {
								s = new SimpleEventElement(jd1, EVENT.CALENDAR, "DST1");
								list.add(s);
							}
							if (jd2 > jd0 && jd2 < jdf) {
								s = new SimpleEventElement(jd2 + 1.0/24.0, EVENT.CALENDAR, "DST2");
								list.add(s);
							}
						}
						s = null;
						break;
					case 55:
						if (artSatTransits) {
							SatelliteEphem.USE_IRIDIUM_SATELLITES = false;
							SatelliteEphem.setSatellitesFromExternalFile(null);
		    				boolean withSats = false;
		    				AstroDate astroUT = new AstroDate(TimeScale.getJD(init, obs, eph, SCALE.UNIVERSAL_TIME_UTC));
		    				try {
			    				if (Configuration.isAcceptableDateForArtificialSatellites(astroUT)) {
			    					withSats = true;
			    				} else {
			    					String pt = Configuration.updateArtificialSatellitesInTempDir(astroUT);
			    					if (pt != null) withSats = true;
			    				}
		    				} catch (Exception exc) {}
		    				if (withSats) {
								String name[] = DataSet.toStringArray(artificialSatellites, ",");
			    				SatelliteOrbitalElement sat[] = new SatelliteOrbitalElement[name.length];
			    				for (int i=0; i<name.length; i++) {
			    					name[i] = name[i].trim();
				    				sat[i] = SatelliteEphem.getArtificialSatelliteOrbitalElement(SatelliteEphem.getArtificialSatelliteTargetIndex(name[i]));
			    				}
			    				double min_elevation = 15 * Constant.DEG_TO_RAD, maxDays = jdf - jd0;
	
			    				ArrayList<SimpleEventElement> newEvents = new ArrayList<SimpleEventElement>();
			    				for (int i=0; i<name.length; i++) {
			    					TimeElement initTime = init.clone();
	
			    					while (true) {
				    					double initJD = initTime.astroDate.jd();
				    					if (initJD > jdf) break;
				    					maxDays = jdf - initJD;
				    					double jdNext = SDP4_SGP4.getNextPass(initTime, obs, eph, sat[i], min_elevation, maxDays, true);
				    					if (Math.abs(jdNext) > jdf || jdNext == 0) break;
			    						initTime.astroDate = new AstroDate(Math.abs(jdNext) + 30.0 / 1440.0);
				    					eph.targetBody = TARGET.NOT_A_PLANET;
				    					eph.targetBody.setIndex(SatelliteEphem.getArtificialSatelliteTargetIndex(name[i]));
				    					double jdNextTT = TimeScale.getJD(new TimeElement(Math.abs(jdNext), SCALE.LOCAL_TIME), obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
				    					SatelliteEphemElement sephem = SDP4_SGP4.satEphemeris(new TimeElement(jdNextTT, SCALE.TERRESTRIAL_TIME), obs, eph, false);
				    					String ecl = "E";
				    					if (jdNext > 0) ecl = Functions.formatValue(sephem.magnitude, 1)+"m";
				    					SimpleEventElement newEvent = new SimpleEventElement(jdNextTT, SimpleEventElement.EVENT.ARTIFICIAL_SATELLITES_TRANSITS, ecl);
				    					newEvent.body = sat[i].name;
				    					newEvent.eventLocation = sephem.getEquatorialLocation();
				    					newEvents.add(newEvent);
			    					}
			    				}
			    				list.addAll(newEvents);
		    				}
						}
						break;
					case 56:
						if (artSatTransitsSunMoon) {
							SatelliteEphem.USE_IRIDIUM_SATELLITES = false;
							SatelliteEphem.setSatellitesFromExternalFile(null);
		    				boolean withSats = false;
		    				AstroDate astroUT = new AstroDate(TimeScale.getJD(init, obs, eph, SCALE.UNIVERSAL_TIME_UTC));
		    				try {
			    				if (Configuration.isAcceptableDateForArtificialSatellites(astroUT)) {
			    					withSats = true;
			    				} else {
			    					String pt = Configuration.updateArtificialSatellitesInTempDir(astroUT);
			    					if (pt != null) withSats = true;
			    				}
		    				} catch (Exception exc) {}
		    				if (withSats) {
								String satNames[] = DataSet.toStringArray(artificialSatellites, ",");
								for (int i=0; i<satNames.length; i++) {
									int index = SatelliteEphem.getArtificialSatelliteTargetIndex(satNames[i]);
									if (index >= 0) {
										SatelliteOrbitalElement sat = SatelliteEphem.getArtificialSatelliteOrbitalElement(index);
										ArrayList<SimpleEventElement> more = SDP4_SGP4.getNextSunOrMoonTransits(new TimeElement(jd0, SCALE.TERRESTRIAL_TIME),
											obs, eph, sat, jdf-jd0, 0.25);
										if (more != null || more.size() > 0)
											list.addAll(more);
									}
								}
		    				}
						}
						break;
					case 57:
						if (artSatIridium) {
							SatelliteEphem.USE_IRIDIUM_SATELLITES = true;
							SatelliteEphem.setSatellitesFromExternalFile(null);
		    				boolean withSats = false;
		    				AstroDate astroUT = new AstroDate(TimeScale.getJD(init, obs, eph, SCALE.UNIVERSAL_TIME_UTC));
		    				try {
			    				if (Configuration.isAcceptableDateForArtificialSatellites(astroUT)) {
			    					withSats = true;
			    				} else {
			    					String pt = Configuration.updateArtificialSatellitesInTempDir(astroUT);
			    					if (pt != null) withSats = true;
			    				}
		    				} catch (Exception exc) {}
		    				if (withSats) {
			    				double min_elevation = 15 * Constant.DEG_TO_RAD, maxDays = jdf - jd0;
	
			    				ArrayList<SimpleEventElement> newEvents = new ArrayList<SimpleEventElement>();
		        				String sstart = "start", send = "end", smax = "max";
		        				/*if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) {
		        					sstart = "inicio";
		        					send = "final";
		        					smax = Translate.translate(1321);
		        				}*/
		        				String mag = Translate.getEntry(157, LANGUAGE.ENGLISH).toLowerCase();
		        				String az = Translate.getEntry(28, LANGUAGE.ENGLISH).toLowerCase();
		        				String el = Translate.getEntry(29, LANGUAGE.ENGLISH).toLowerCase();
		        				int n = SatelliteEphem.getArtificialSatelliteCount();
		        				int precision = 5;
			    				for (int i=0; i<n; i++) {
			    	                eph.targetBody.setIndex(i);
			    	                SatelliteOrbitalElement soe = SatelliteEphem.getArtificialSatelliteOrbitalElement(i);
			    	                if (soe.getStatus() == SatelliteOrbitalElement.STATUS.FAILED || soe.getStatus() == SatelliteOrbitalElement.STATUS.UNKNOWN)
			    	                    continue;
			    	                soe.name = DataSet.replaceAll(soe.name, " [+]", "", true);
			    					ArrayList<Object[]> flares = SatelliteEphem.getNextIridiumFlares(init, obs, eph, soe, min_elevation, maxDays, true, precision);
			    					if (flares == null) continue;
			    					for (int j=0;j<flares.size(); j++) {
			    						Object data[] = flares.get(j);
				    					double jdNext = (Double) data[0];
				    					double jdEnd = (Double) data[1];
				    					double jdMax = (Double) data[2];
				    					SatelliteEphemElement satInit = (SatelliteEphemElement) data[4];
				    					SatelliteEphemElement satEnd = (SatelliteEphemElement) data[5];
				    					SatelliteEphemElement satMax = (SatelliteEphemElement) data[6];
	
				    					AstroDate ainit = new AstroDate(jdNext);
				    					AstroDate aend = new AstroDate(jdEnd);
				    					AstroDate amax = new AstroDate(jdMax);
				    					String det = sstart + " ("+DateTimeOps.twoDigits(ainit.getHour())+":"+DateTimeOps.twoDigits(ainit.getMinute()) + ":" + DateTimeOps.twoDigits((int) ainit.getSeconds()) + "): ";
				    					det += az + " " +Functions.formatAngleAsDegrees(satInit.azimuth, 1)+", "+el+" "+Functions.formatAngleAsDegrees(satInit.elevation, 1) + ", "+mag+" "+Functions.formatValue(satInit.magnitude, 1)+ "; ";
				    					det += send + " ("+DateTimeOps.twoDigits(aend.getHour())+":"+DateTimeOps.twoDigits(aend.getMinute()) + ":" + DateTimeOps.twoDigits((int) aend.getSeconds()) + "): ";
				    					det += Functions.formatAngleAsDegrees(satEnd.azimuth, 1)+", "+Functions.formatAngleAsDegrees(satEnd.elevation, 1) + ", "+Functions.formatValue(satEnd.magnitude, 1)+"; ";
				    					det += smax + " ("+DateTimeOps.twoDigits(amax.getHour())+":"+DateTimeOps.twoDigits(amax.getMinute()) + ":" + DateTimeOps.twoDigits((int) amax.getSeconds()) + "): ";
				    					det += Functions.formatAngleAsDegrees(satMax.azimuth, 1)+", "+Functions.formatAngleAsDegrees(satMax.elevation, 1) + ", "+Functions.formatValue(satMax.magnitude, 1);
				    					double jdNextTT = TimeScale.getJD(new TimeElement(jdNext, SCALE.LOCAL_TIME), obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
				    					double jdEndTT = TimeScale.getJD(new TimeElement(jdEnd, SCALE.LOCAL_TIME), obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
				    					SimpleEventElement newEvent = new SimpleEventElement(jdNextTT, SimpleEventElement.EVENT.ARTIFICIAL_SATELLITES_IRIDIUM_FLARES,
				    							det);
				    					newEvent.endTime = jdEndTT;
				    					newEvent.body = soe.name;
				    					newEvent.eventLocation = satInit.getEquatorialLocation();
				    					newEvents.add(newEvent);
			    					}
			    				}
			    				list.addAll(newEvents);
		    				}
							SatelliteEphem.USE_IRIDIUM_SATELLITES = false;
							SatelliteEphem.setSatellitesFromExternalFile(null);
						}
						break;
					case 58:
						if (lunarNeomenia) {
							s = null;
							while (s == null && jd < jdf) {
								s = LunarEvent.Neomenia(new TimeElement(jd, SCALE.TERRESTRIAL_TIME), obs, eph);
								if (s != null) {
									jd = s.time + 15;
								} else {
									jd = jd + 15;
								}
							}
						}
						break;
					case 59:
					case 60:
					case 61:
					case 62:
					case 63:
					case 64:
					case 65:
					case 66:
						TARGET target = TARGET.values()[event - 58];
						if (target == TARGET.EARTH) break;
						if (dichotomyAndQuadrature) s = MainEvents.getPlanetaryEvent(target, jd, EVENT.PLANET_DICHOTOMY_OR_QUADRATURE, EVENT_TIME.NEXT, maximumAccuracy);
						if (s != null) jd = s.time + 20;
						break;
					default:
						s = null;
						break;
					};
				} catch (JPARSECException exc) {
					s = null;
				}

				if (s == null) break;
				if (s.equals(sold)) {
					jd += (jd - s.time);
				} else {
					if (s.time > jd0 && s.time < jdf) {
						if (event == 0 || event == 1 || event == 10 || event == 11) {
							TimeElement time = new TimeElement(s.time, SCALE.TERRESTRIAL_TIME);
							EphemerisElement ephCopy = eph.clone();
							ephCopy.targetBody = TARGET.SUN;
							if (event == 0) ephCopy.targetBody = TARGET.Moon;
							if (event == 10) ephCopy.targetBody = TARGET.MERCURY;
							if (event == 11) ephCopy.targetBody = TARGET.VENUS;
							EphemElement ephemSun = Ephem.getEphemeris(time, obs, ephCopy, false);
							boolean solarEclipseNotVisible = false;
							if (event == 1 && ephemSun.elevation >= 0) {
								RenderEclipse re = null;
								try {
									re = new RenderEclipse(new AstroDate(s.time-0.25));
								} catch (Exception exc) {
									try {
										re = new RenderEclipse(new AstroDate(s.time+0.25));
									} catch (Exception exc2) {	}
								}
								if (re != null && Math.abs(re.getEclipseDate().jd()-s.time) < 3)
									if (!re.isVisible(obs)) solarEclipseNotVisible = true;
							}
							if (ephemSun.elevation < 0 || solarEclipseNotVisible)
								s.details += ". "+Translate.getEntry(1021, LANGUAGE.ENGLISH)+" "+obs.getName();
						}
						list.add(s);
					} else {
						if (s.time > jdf || jd > jdf) break;
					}
				}
				sold = s;
			} while (true);
		}

		// Sort in crescent order of time
		double val[] = new double[list.size()];
		for (int i=0; i<list.size(); i++) {
			val[i] = list.get(i).time;
		}
		Object a[] = DataSet.sortInCrescent(list.toArray(), val);
		for (int i=0; i<list.size(); i++) {
			list.set(i, (SimpleEventElement) a[i]);
		}

		// Eliminate duplicate entries, if any
		for (int i=1; i<list.size(); i++) {
			do {
				String entryI = list.get(i).toString();
				String entryJ = list.get(i-1).toString();
				if (entryI.equals(entryJ)) {
					list.remove(i);
					if (i == list.size()) break;
				} else {
					break;
				}
			} while(true);
		}

		return list;
	}

	/**
	 * Creates an RSS feed with astronomical events. The description of the
	 * events is set according to the current language.
	 *
	 * @param list The list of events.
	 * @param obs The observer.
	 * @return The feed populate with events.
	 * @throws JPARSECException If an error occurs.
	 */
	public static Feed getFeed(ArrayList<SimpleEventElement> list, ObserverElement obs) throws JPARSECException {
		// Create the feed
		String title = Translate.translate("Astronomical events");
		String link = "";
		String description = "";
		String language = "en";
		if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) language = "es";
		String copyright = "";
		String pubDate = "";
		String url = "";

		Feed feed = new Feed(title, link, description, language, copyright, pubDate);
		feed.setFeedImage(url);

		EphemerisElement eph = new EphemerisElement(TARGET.NOT_A_PLANET, EphemerisElement.COORDINATES_TYPE.APPARENT,
				EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.TOPOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
				EphemerisElement.FRAME.DYNAMICAL_EQUINOX_J2000);
		eph.correctForEOP = false;
		eph.correctForPolarMotion = false;

		// Populate the feed
		for (int i=0; i<list.size(); i++) {
			SimpleEventElement s = list.get(i);
			title = Translate.translate(SimpleEventElement.EVENTS[s.eventType.ordinal()]);
			description = s.toString(obs, eph, SCALE.LOCAL_TIME);
			int lastCP = description.lastIndexOf(")");
			int twoP = description.indexOf(": ");
			if (lastCP > 0 && twoP > 0 && twoP < lastCP &&
					s.eventType != EVENT.PLANET_MINIMUM_DISTANCE &&
					s.eventType != EVENT.PLANET_MAXIMUM_DISTANCE &&
					s.eventType != EVENT.PLANET_MAXIMUM_DISTANCE_FROM_SUN &&
					s.eventType != EVENT.PLANET_MINIMUM_DISTANCE_FROM_SUN &&
					s.eventType != EVENT.PLANET_MAXIMUM_ELONGATION &&
					s.eventType != EVENT.PLANET_MINIMUM_ELONGATION &&
					s.eventType != EVENT.VENUS_TRANSIT &&
					s.eventType != EVENT.MERCURY_TRANSIT &&
					s.eventType != EVENT.MOON_LUNAR_ECLIPSE &&
					s.eventType != EVENT.MOON_SOLAR_ECLIPSE &&
					s.eventType != EVENT.VARIABLE_STAR_ECLIPSING &&
					s.eventType != EVENT.VARIABLE_STAR_MIRA &&
					s.eventType != EVENT.OTHER &&
					s.eventType != EVENT.MOON_APOGEE &&
					s.eventType != EVENT.MOON_PERIGEE
					) {
				if (twoP >= 0 && (s.eventType == EVENT.MOON_FULL || s.eventType == EVENT.ECLIPSE || s.eventType == EVENT.OCCULTATION ||
						s.eventType == EVENT.MOON_MAXIMUM_DECLINATION || s.eventType == EVENT.MOON_MINIMUM_DECLINATION ||
						s.eventType == EVENT.PLANET_CONJUNCTION || s.eventType == EVENT.PLANET_OPPOSITION)) {
					title = description.substring(0, twoP).trim();
					description = description.substring(twoP+1).trim();
				} else {
					title = description.substring(0, lastCP + 1);
					description = description.substring(lastCP + 1);
					twoP = description.indexOf(": ");
					if (twoP >= 0) {
						if (twoP > 0) title += description.substring(0, twoP).trim();
						description = description.substring(twoP+1).trim();
					}
				}
			} else {
				if (twoP > 0) {
					title = description.substring(0, twoP).trim();
					description = description.substring(twoP+1).trim();
				}
			}
			link = "";
			String author = Version.PACKAGE_NAME + " " + Version.VERSION_ID;
			String guid = link;
			String imgs[] = null;
			feed.addFeedMessage(Feed.createMessage(title, description, link, author, guid, imgs, getPDate(obs)));
		}

		return feed;
	}

	private static class thread0 implements Runnable {
		private double jd0, jdf;
		private ObserverElement obs;
		private EphemerisElement eph;
		public ArrayList<SimpleEventElement> a = null;
		public thread0(double jd0, double jdf, ObserverElement obs, EphemerisElement eph) {
			this.jd0 = jd0;
			this.jdf = jdf;
			this.obs = obs.clone();
			this.eph = eph.clone();
		}
		public void run() {
			try {
				eph.targetBody = TARGET.JUPITER;
				a = getMoonEvents(jd0, jdf, obs, eph);
				eph.targetBody = TARGET.URANUS;
				ArrayList<SimpleEventElement> aa = getMoonEvents(jd0, jdf, obs, eph);
				if (aa != null) {
					for (int si = 0; si < aa.size(); si ++) {
						a.add(aa.get(si));
					}
				}
			} catch (Exception exc) { exc.printStackTrace(); }
		}
	}

	private static class thread1 implements Runnable {
		private double jd0, jdf;
		private ObserverElement obs;
		private EphemerisElement eph;
		public ArrayList<SimpleEventElement> a = null;
		public thread1(double jd0, double jdf, ObserverElement obs, EphemerisElement eph) {
			this.jd0 = jd0;
			this.jdf = jdf;
			this.obs = obs.clone();
			this.eph = eph.clone();
		}
		public void run() {
			try {
				eph.targetBody = TARGET.SATURN;
				a = getMoonEvents(jd0, jdf, obs, eph);
			} catch (Exception exc) { exc.printStackTrace(); }
		}
	}

	private static String getPDate(ObserverElement obs) throws JPARSECException {
		// Assume the execution is by the observer defined here
		return (new AstroDate()).toStandarizedString(obs);
	}

	private static String replaceVars(String raw, String[] vars, String[] values) {
		if (vars == null) return raw;
		String out = raw;
		for (int i=0; i<vars.length; i++) {
			if (!vars[i].startsWith("%")) vars[i] = "%"+vars[i];
			if (vars[i].indexOf("hour") >= 0 || vars[i].indexOf("minute") >= 0) {
				int v = Integer.parseInt(values[i]);
				if (v < 10 && values[i].length() == 1) values[i] = "0"+values[i];
			}
			out = DataSet.replaceAll(out, vars[i], values[i], true);
		}
		return out;
	}

	private static void readFileOfStars(double maglim, ReadFile re) throws JPARSECException
	{
		// Define necesary variables
		String file_line = "";

		ArrayList list = new ArrayList();
		Object o = re.getReadElements();

		// Connect to the file
		try
		{
			InputStream is = EventReport.class.getClassLoader().getResourceAsStream(re.pathToFile);
			BufferedReader dis = new BufferedReader(new InputStreamReader(is));

			while ((file_line = dis.readLine()) != null)
			{
				// Obtain fields
				StarElement star = ReadFile.parseJPARSECfile(file_line);
				if (star != null) {
					if (star.magnitude < maglim)
					{
						list.add(star);
					} else {
						break;
					}
				}
			}

			// Close file
			dis.close();

			if (o == null) {
				re.setReadElements(list);
			} else {
				re.addReadElements(list);
			}
		} catch (Exception e2)
		{
			throw new JPARSECException(
					"error while reading file " + re.pathToFile, e2);
		}
	}

	private static String types[] = new String[] {"unk", "gal", "neb", "pneb", "ocl", "gcl", "galpart", "qua", "duplicate", "duplicateInNGC", "star/s", "notFound"};
	private static ArrayList<Object> readObjects(double maglim) throws JPARSECException {
		ArrayList<Object> objects = new ArrayList<Object>();
		try
		{
			InputStream is = EventReport.class.getClassLoader().getResourceAsStream(FileIO.DATA_SKY_DIRECTORY + "objects.txt");
			BufferedReader dis = new BufferedReader(new InputStreamReader(is, ReadFile.ENCODING_ISO_8859));
			String line = "";
			while ((line = dis.readLine()) != null)
			{
				String name = FileIO.getField(1, line, " ", true);
				String type = FileIO.getField(2, line, " ", true);
				String ra = FileIO.getField(3, line, " ", true);
				String dec = FileIO.getField(4, line, " ", true);
				String mag = FileIO.getField(5, line, " ", true);
				String max = FileIO.getField(6, line, " ", true);
				String min = FileIO.getField(7, line, " ", true);
				String pa = FileIO.getField(8, line, " ", true);
				String com = FileIO.getRestAfterField(8, line, " ", true);

				double m = Double.parseDouble(mag);
				if (m > maglim) break;
				LocationElement loc = new LocationElement(Double.parseDouble(ra)/Constant.RAD_TO_HOUR, Double.parseDouble(dec)*Constant.DEG_TO_RAD, 1.0);
				if (loc != null && m < maglim) {
					int tt = DataSet.getIndex(types, type);
					int mes1 = com.indexOf(" M ");
					int mes2 = com.indexOf(" part of M ");
					int mes3 = com.indexOf(" in M ");
					int mes4 = com.indexOf(" near M ");
					int mes5 = com.indexOf(" not M ");
					int mes6 = com.indexOf(" on M ");
					int mes7 = com.indexOf("in M ");
					String messier = "";
					if (mes1 >= 0 && mes2 < 0 && mes3 < 0 && mes4 < 0 && mes5<0 && mes6<0 && mes7<0) {
						messier = com.substring(mes1);
						int c = messier.indexOf(",");
						if (c < 0) c = messier.indexOf(";");
						messier = DataSet.replaceAll(messier.substring(0, c), " ", "", false);
					}
					double maxSize = Double.parseDouble(max), minSize = Double.parseDouble(min);
					if (tt == 6 && maxSize == 0.0) maxSize = minSize = 0.5/60.0;

					try {
						int ngc = Integer.parseInt(name);
						if (ngc > 0) name = "NGC" + name;
					} catch (Exception exc) {
						try {
							if (name.startsWith("I.")) name = "IC"+name.substring(2);
						} catch (Exception exc2) {	}
					}
					if (!messier.equals("")) name += " ("+messier+")";

					objects.add(new Object[] {name, messier, tt, loc, m,
							new float[] {(float) maxSize, (float) minSize}, pa, com});
				}
			}

			// Close file
			dis.close();
		} catch (FileNotFoundException e1)
		{
			throw new JPARSECException("objects file not found.", e1);
		} catch (IOException e2)
		{
			throw new JPARSECException(
					"error while reading objects file.", e2);
		}
		return objects;
	}

	private static ArrayList<SimpleEventElement> getCloseApproachOfNEOs(double jd0, double jdf, ObserverElement obs, EphemerisElement ephIn) throws JPARSECException {
		ArrayList<SimpleEventElement> list = new ArrayList<SimpleEventElement>();

		ReadFile re = new ReadFile(FORMAT.MPC, OrbitEphem.PATH_TO_MPC_NEOs_FILE);
		try { re.readFileOfNEOs((jd0+jdf)*0.5, 365); } catch (Exception exc) {}
		int n = re.getNumberOfObjects();
		if (n == 0) return list;

		EphemerisElement eph = ephIn.clone();
		eph.ephemType = COORDINATES_TYPE.APPARENT;
		eph.equinox = EphemerisElement.EQUINOX_OF_DATE;
		eph.preferPrecisionInEphemerides = false;
		eph.correctEOPForDiurnalSubdiurnalTides = false;
		eph.correctForEOP = false;
		eph.correctForPolarMotion = false;

		eph.targetBody = TARGET.NEO;
		eph.algorithm = EphemerisElement.ALGORITHM.ORBIT;
		double step = 1.0;

		double times[] = new double[n];
		double minD[] = new double[n];
		for (int i=0; i<n; i++) {
			times[i] = -1;
			minD[i] = -1;
		}

		for (double jd = jd0; jd <=jdf; jd = jd + step) {
			TimeElement time = new TimeElement(jd, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
			for (int index = 0; index < n; index++)
			{
				OrbitalElement orbit = re.getOrbitalElement(index);
				eph.orbit = orbit;
				EphemElement ephem = OrbitEphem.orbitEphemeris(time, obs, eph);

				if (ephem.distance < minD[index] || minD[index] == -1) {
					minD[index] = ephem.distance;
					times[index] = jd;
				}
			}
		}

		for (int i=0; i<n; i++) {
			if (times[i] != -1 && minD[i] < 400000 / Constant.AU) {
				OrbitalElement orbit = re.getOrbitalElement(i);
				eph.orbit = orbit;

				double t[] = new double[5], d[] = new double[5];
				double back = 0.75;
				double tstep = 2.0 * back / 5.0;
				for (int nn=0; nn<5;nn++) {
					double jd = times[i] - back + nn * tstep;
					TimeElement time = new TimeElement(jd, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
					EphemElement ephem = OrbitEphem.orbitEphemeris(time, obs, eph);
					t[nn] = jd;
					d[nn] = ephem.distance;
				}

				Interpolation interp = new Interpolation(t, d, false);
				Point2D p = interp.MeeusExtremum();
				double jd = p.getX();
				TimeElement time = new TimeElement(jd, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
				EphemElement ephem = OrbitEphem.orbitEphemeris(time, obs, eph);

				String details = ""+p.getY();
				SimpleEventElement s = new SimpleEventElement(jd, EVENT.PLANET_MINIMUM_DISTANCE, details);
				s.body = orbit.name;
				s.eventLocation = ephem.getEquatorialLocation();
				list.add(s);
			}
		}

		return list;
	}

	private static ArrayList<SimpleEventElement> getCometAsteroidVisibleNakedEye(double jd0, double jdf, ObserverElement obs, EphemerisElement ephIn) throws JPARSECException {
		ArrayList<SimpleEventElement> list = new ArrayList<SimpleEventElement>();

		EphemerisElement eph = ephIn.clone();
		eph.ephemType = COORDINATES_TYPE.ASTROMETRIC;
		eph.equinox = Constant.J2000;
		eph.isTopocentric = true;
		eph.preferPrecisionInEphemerides = false;
		eph.correctEOPForDiurnalSubdiurnalTides = false;
		eph.correctForEOP = false;
		eph.correctForPolarMotion = false;

		int ncomet = OrbitEphem.getCometsCount();
		int naster = OrbitEphem.getAsteroidsCount();
		OrbitalElement comets[] = new OrbitalElement[ncomet];
		for (int i=0; i<ncomet; i++) {
			comets[i] = OrbitEphem.getOrbitalElementsOfComet(i);
		}
		OrbitalElement asteroids[] = new OrbitalElement[naster];
		for (int i=0; i<naster; i++) {
			asteroids[i] = OrbitEphem.getOrbitalElementsOfAsteroid(i);
		}

		eph.targetBody = TARGET.Comet;
		eph.algorithm = EphemerisElement.ALGORITHM.ORBIT;
		int year0 = (new AstroDate(jd0)).getYear();
		double step = 0.25, maglim = 6.5;
		String events[] = new String[] {};
		double times[] = new double[] {};
		Object obj[] = new Object[] {};
		String cometVisible = "Cvisible ", asteroidVisible = "Avisible ";
		String cometUnvisible = "Cunvisible ", asteroidUnvisible = "Aunvisible ";
		String cometMaxMag = "Cmax ", asteroidMaxMag = "Amax ";
		String cometPerih = "Cperih ", cometPerig = "Cperig ";
		boolean alreadyVisibleA[] = new boolean[naster];
		boolean alreadyVisibleC[] = new boolean[ncomet];
		for (double jd = jd0; jd <=jdf; jd = jd + step) {
			TimeElement time = new TimeElement(jd, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
			for (int index = 0; index < ncomet; index++)
			{
				OrbitalElement orbit = comets[index];
				if (orbit == null) continue;
				if (orbit.name.startsWith("C/")) {
					try {
						if (orbit.referenceTime < jd0 - 365*2) continue;

						int year = Integer.parseInt(orbit.name.substring(2, orbit.name.indexOf(" ")));
						if (year > year0+2) continue;
					} catch (Exception exc) {
						exc.printStackTrace();
						continue;
					}
				}

				eph.orbit = orbit;
				if (Math.abs(orbit.referenceTime - jd) < Constant.JULIAN_DAYS_PER_CENTURY &&
						orbit.getCometMaximumMagnitude() < maglim)
				{
					eph.targetBody = TARGET.Comet;
					EphemElement ephem = OrbitEphem.orbitEphemeris(time, obs, eph);
					if (jd == jd0) alreadyVisibleC[index] = ephem.magnitude < maglim;
					int ivis0 = DataSet.getIndex(events, cometVisible+ephem.name);
					if (ivis0 < 0) ivis0 = DataSet.getIndex(events, "*"+cometVisible+ephem.name);
					if (ephem.magnitude < maglim) {
						if (ivis0 < 0) {
							if (!alreadyVisibleC[index]) {
								events = DataSet.addStringArray(events, new String[] {cometVisible+ephem.name});								
							} else {
								events = DataSet.addStringArray(events, new String[] {"*"+cometVisible+ephem.name});
							}
							times = DataSet.addDoubleArray(times, new double[] {jd});
							obj = DataSet.addObjectArray(obj, new Object[] {ephem.getEquatorialLocation()});
							events = DataSet.addStringArray(events, new String[] {ephem.distance+" "+cometPerig+ephem.name});
							times = DataSet.addDoubleArray(times, new double[] {jd});
							obj = DataSet.addObjectArray(obj, new Object[] {ephem.getEquatorialLocation()});
							if (orbit.meanAnomaly == 0.0) {
								events = DataSet.addStringArray(events, new String[] {orbit.perihelionDistance+" "+cometPerih+ephem.name});
								times = DataSet.addDoubleArray(times, new double[] {orbit.referenceTime});
								obj = DataSet.addObjectArray(obj, new Object[] {new LocationElement(0.0, 0.0, 0.0)});
							}
						}
						int imax = DataSet.getIndexEndingWith(events, " "+cometMaxMag+ephem.name);
						if (imax >= 0) {
							double mag = Double.parseDouble(FileIO.getField(1, events[imax], " ", true));
							if (ephem.magnitude < mag) {
								events[imax] = ephem.magnitude+" "+ephem.elongation+" "+cometMaxMag+ephem.name;
								times[imax] = jd;
								obj[imax] = ephem.getEquatorialLocation();
							}
						} else {
							events = DataSet.addStringArray(events, new String[] {ephem.magnitude+" "+ephem.elongation+" "+cometMaxMag+ephem.name});
							times = DataSet.addDoubleArray(times, new double[] {jd});
							obj = DataSet.addObjectArray(obj, new Object[] {ephem.getEquatorialLocation()});
						}
					} else {
						if (ephem.magnitude > maglim && ivis0 >= 0) {
							String eventsLater[] = DataSet.getSubArray(events, ivis0, events.length-1);
							int iuvis = DataSet.getIndex(eventsLater, cometUnvisible+ephem.name);
							if (iuvis < 0) {
								events = DataSet.addStringArray(events, new String[] {cometUnvisible+ephem.name});
								times = DataSet.addDoubleArray(times, new double[] {jd});
								obj = DataSet.addObjectArray(obj, new Object[] {ephem.getEquatorialLocation()});
							}
						}
					}
					int imax = DataSet.getIndexEndingWith(events, " "+cometPerig+ephem.name);
					if (imax >= 0) {
						double d = Double.parseDouble(FileIO.getField(1, events[imax], " ", true));
						if (ephem.distance < d) {
							events[imax] = ephem.distance+" "+cometPerig+ephem.name;
							times[imax] = jd;
							obj[imax] = ephem.getEquatorialLocation();
						}
					}
				}
			}


			eph.targetBody = TARGET.Asteroid;
			eph.algorithm = EphemerisElement.ALGORITHM.ORBIT;
			for (int index = 0; index < naster; index++)
			{
				OrbitalElement orbit = asteroids[index];
				eph.orbit = orbit;
				if (Math.abs(orbit.referenceTime - jd) < Constant.JULIAN_DAYS_PER_CENTURY &&
						orbit.getAsteroidMaximumMagnitude() < maglim)
				{
					eph.targetBody = TARGET.Asteroid;
					EphemElement ephem = OrbitEphem.orbitEphemeris(time, obs, eph);
					if (jd == jd0) alreadyVisibleA[index] = ephem.magnitude < maglim;
					int ivis0 = DataSet.getIndex(events, asteroidVisible+ephem.name);
					if (ivis0 < 0) ivis0 = DataSet.getIndex(events, "*"+asteroidVisible+ephem.name);
					if (ephem.magnitude < maglim) {
						if (ivis0 < 0) {
							if (!alreadyVisibleA[index]) {
								events = DataSet.addStringArray(events, new String[] {asteroidVisible+ephem.name});								
							} else {
								events = DataSet.addStringArray(events, new String[] {"*"+asteroidVisible+ephem.name});
							}
							times = DataSet.addDoubleArray(times, new double[] {jd});
							obj = DataSet.addObjectArray(obj, new Object[] {ephem.getEquatorialLocation()});
						}
						int imax = DataSet.getIndexEndingWith(events, " "+asteroidMaxMag+ephem.name);
						if (imax >= 0) {
							double mag = Double.parseDouble(FileIO.getField(1, events[imax], " ", true));
							if (ephem.magnitude < mag) {
								events[imax] = ephem.magnitude+" "+ephem.elongation+" "+asteroidMaxMag+ephem.name;
								times[imax] = jd;
								obj[imax] = ephem.getEquatorialLocation();
							}
						} else {
							events = DataSet.addStringArray(events, new String[] {ephem.magnitude+" "+ephem.elongation+" "+asteroidMaxMag+ephem.name});
							times = DataSet.addDoubleArray(times, new double[] {jd});
							obj = DataSet.addObjectArray(obj, new Object[] {ephem.getEquatorialLocation()});
						}
					} else {
						if (ephem.magnitude > maglim && ivis0 >= 0) {
							String eventsLater[] = DataSet.getSubArray(events, ivis0, events.length-1);
							int iuvis = DataSet.getIndex(eventsLater, asteroidUnvisible+ephem.name);
							if (iuvis < 0) {
								events = DataSet.addStringArray(events, new String[] {asteroidUnvisible+ephem.name});
								times = DataSet.addDoubleArray(times, new double[] {jd});
								obj = DataSet.addObjectArray(obj, new Object[] {ephem.getEquatorialLocation()});
							}
						}
					}
				}
			}
		}


		String type = "";
		String comet = Translate.getEntry(74, LANGUAGE.ENGLISH), asteroid = Translate.getEntry(73, LANGUAGE.ENGLISH);
		for (int i=0; i<events.length; i++) {
			SimpleEventElement s = null;
			type = "";
			if (events[i].startsWith(cometVisible)) type = comet;
			if (events[i].startsWith(asteroidVisible)) type = asteroid;
			if (!type.equals("")) {
				String name = FileIO.getRestAfterField(1, events[i], " ", true);
				if (times[i] > jd0) {
					s = new SimpleEventElement(times[i], EVENT.OTHER, type+" "+name+" "+Translate.getEntry(1085, LANGUAGE.ENGLISH));
					s.body = name;
					if (obj[i] != null) {
						LocationElement loc = (LocationElement) obj[i];
						if (loc.getLatitude() != 0.0 || loc.getLongitude() != 0.0 || loc.getRadius() != 0.0)
							s.eventLocation = loc;
					}
				}
			} else {
				if (events[i].startsWith(cometUnvisible)) type = comet;
				if (events[i].startsWith(asteroidUnvisible)) type = asteroid;
				if (!type.equals("")) {
					String name = FileIO.getRestAfterField(1, events[i], " ", true);
					if (times[i] > jd0) {
						s = new SimpleEventElement(times[i], EVENT.OTHER, type+" "+name+" "+Translate.getEntry(1086, LANGUAGE.ENGLISH));
						s.body = name;
						if (obj[i] != null) {
							LocationElement loc = (LocationElement) obj[i];
							if (loc.getLatitude() != 0.0 || loc.getLongitude() != 0.0 || loc.getRadius() != 0.0)
								s.eventLocation = loc;
						}
					}
				} else {
					if (events[i].indexOf(" "+cometMaxMag) > 0) type = comet;
					if (events[i].indexOf(" "+asteroidMaxMag) > 0) type = asteroid;
					if (!type.equals("")) {
						String name = FileIO.getRestAfterField(3, events[i], " ", true);
						if (times[i] > jd0) {
							s = new SimpleEventElement(times[i], EVENT.OTHER, type+" "+name+" "+Translate.getEntry(1087, LANGUAGE.ENGLISH));
							s.body = name;
							s.details = DataSet.replaceAll(s.details, "%mag", Functions.formatValue(Double.parseDouble(FileIO.getField(1, events[i], " ", true)), 2), true);
							s.details = DataSet.replaceAll(s.details, "%elong", Functions.formatValue(Constant.RAD_TO_DEG*Double.parseDouble(FileIO.getField(2, events[i], " ", true)), 2), true);
							if (obj[i] != null) {
								LocationElement loc = (LocationElement) obj[i];
								if (loc.getLatitude() != 0.0 || loc.getLongitude() != 0.0 || loc.getRadius() != 0.0)
									s.eventLocation = loc;
							}
						}
					} else {
						if (events[i].indexOf(" "+cometPerih) > 0) type = comet;
						if (!type.equals("")) {
							String name = FileIO.getRestAfterField(2, events[i], " ", true);
							if (times[i] > jd0) {
								s = new SimpleEventElement(times[i], EVENT.PLANET_MINIMUM_DISTANCE_FROM_SUN, "");
								s.body = name;
								s.details = Functions.formatValue(Double.parseDouble(FileIO.getField(1, events[i], " ", true)), 3);
								if (obj[i] != null) {
									LocationElement loc = (LocationElement) obj[i];
									if (loc.getLatitude() != 0.0 || loc.getLongitude() != 0.0 || loc.getRadius() != 0.0)
										s.eventLocation = loc;
								}
							}
						} else {
							if (events[i].indexOf(" "+cometPerig) > 0) type = comet;
							if (!type.equals("")) {
								String name = FileIO.getRestAfterField(2, events[i], " ", true);
								if (times[i] > jd0) {
									s = new SimpleEventElement(times[i], EVENT.PLANET_MINIMUM_DISTANCE, "");
									s.body = name;
									s.details = Functions.formatValue(Double.parseDouble(FileIO.getField(1, events[i], " ", true)), 3);
									if (obj[i] != null) {
										LocationElement loc = (LocationElement) obj[i];
										if (loc.getLatitude() != 0.0 || loc.getLongitude() != 0.0 || loc.getRadius() != 0.0)
											s.eventLocation = loc;
									}
								}
							}
						}
					}
				}
			}

			if (s != null) list.add(s);
		}

		return list;
	}

	private static ArrayList<SimpleEventElement> getOccCon(double jd0, double jdf, ObserverElement obs, EphemerisElement ephIn) throws JPARSECException {
		int stepTest = 0;
		ArrayList<SimpleEventElement> list = new ArrayList<SimpleEventElement>();

		ReadFile re_star = new ReadFile();
		re_star.setPath(FileIO.DATA_STARS_SKY2000_DIRECTORY + "JPARSEC_Sky2000.txt");
		re_star.setFormat(ReadFile.FORMAT.JPARSEC_SKY2000);

		readFileOfStars(occultationsConjunctionsStarMaglim, re_star);
		ArrayList<Object> objects = readObjects(occultationsConjunctionsObjectMaglim);

		double step = 0.0125; // 0.0125 d = 18 min => errors around 1 minute in star/planet occultations by Moon
		EphemerisElement eph = ephIn.clone();
		eph.ephemType = COORDINATES_TYPE.ASTROMETRIC;
		eph.equinox = Constant.J2000;
		eph.isTopocentric = true;
		eph.preferPrecisionInEphemerides = false;
		eph.correctEOPForDiurnalSubdiurnalTides = false;
		eph.correctForEOP = false;
		eph.correctForPolarMotion = false;

		int ncomet = OrbitEphem.getCometsCount();
		int naster = OrbitEphem.getAsteroidsCount();
		double maglim = occultationsConjunctionsPlanetMaglim;
		ArrayList listEphem = new ArrayList();
		ArrayList<String> source = new ArrayList<String>();
		ArrayList<EphemElement> sourceEphem = new ArrayList<EphemElement>();
		int year0 = (new AstroDate(jd0)).getYear();
		for (double jd = jd0; jd <=jdf; jd = jd + step) {
			TimeElement time = new TimeElement(jd, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
			if (jd > jd0) {
				int nsi = sourceEphem.size();
				sourceEphem = new ArrayList<EphemElement>();
				for (int si = 0; si < nsi; si++) {
					sourceEphem.add(null);
				}
			}

			eph.algorithm = ALGORITHM.MOSHIER;
			for (int target = 0; target<=TARGET.Moon.ordinal(); target ++) {
				if ((target != TARGET.EARTH.ordinal() || obs.getMotherBody() != TARGET.EARTH) && (target != TARGET.Pluto.ordinal() || includePlutoAsPlanet)) {
					if (jd == jd0) {
						eph.targetBody = TARGET.values()[target];
						EphemElement ephem = Ephem.getEphemeris(time, obs, eph, false);
						source.add(ephem.name);
						sourceEphem.add(ephem);
					} else {
						eph.targetBody = TARGET.values()[target];
						int si = source.indexOf(eph.targetBody.getName());
						EphemElement ephem = Ephem.getEphemeris(time, obs, eph, false);
						sourceEphem.set(si, ephem);
					}
				}
			}

			if (occultationsConjunctionsAddCometsAsteroids) {
				eph.targetBody = TARGET.Comet;
				eph.algorithm = EphemerisElement.ALGORITHM.ORBIT;
				for (int index = 0; index < ncomet; index++)
				{
					OrbitalElement orbit = OrbitEphem.getOrbitalElementsOfComet(index);
					if (orbit.name.startsWith("C/")) {
						try {
							if (orbit.referenceTime < jd0 - 365*2) continue;

							int year = Integer.parseInt(orbit.name.substring(2, orbit.name.indexOf(" ")));
							if (year > year0+1) continue;
						} catch (Exception exc) {
							continue;
						}
					}

					eph.orbit = orbit;
					if (Math.abs(orbit.referenceTime - jd) < Constant.JULIAN_DAYS_PER_CENTURY &&
							orbit.getMagnitude(jd) < maglim)
					{
						EphemElement ephem = OrbitEphem.orbitEphemeris(time, obs, eph);
						//if (ephem.magnitude < maglim) {
							int si = source.indexOf(ephem.name);
							if (jd == jd0 || si < 0) {
								source.add(ephem.name);
								sourceEphem.add(ephem);
							} else {
								sourceEphem.set(si, ephem);
							}
						//}
					}
				}


				eph.targetBody = TARGET.Asteroid;
				eph.algorithm = EphemerisElement.ALGORITHM.ORBIT;
				for (int index = 0; index < naster; index++)
				{
					OrbitalElement orbit = OrbitEphem.getOrbitalElementsOfAsteroid(index);
					eph.orbit = orbit;
					if (Math.abs(orbit.referenceTime - jd) < Constant.JULIAN_DAYS_PER_CENTURY &&
							orbit.getAsteroidMaximumMagnitude() < maglim)
					{
						EphemElement ephem = OrbitEphem.orbitEphemeris(time, obs, eph);
						//if (ephem.magnitude < maglim) {
							int si = source.indexOf(ephem.name);
							if (jd == jd0 || si < 0) {
								source.add(ephem.name);
								sourceEphem.add(ephem);
							} else {
								sourceEphem.set(si, ephem);
							}
						//}
					}
				}
			}

			listEphem.add(sourceEphem);
		}

		int moon = 8;
		if (includePlutoAsPlanet) moon ++;
		if (obs.getMotherBody() != TARGET.EARTH) moon ++;

		int nbody = source.size();
		TARGET mother = obs.getMotherBody();
		if (mother.isNaturalSatellite()) mother = mother.getCentralBody();
		int initBody = moon+1;
		if (mother != TARGET.EARTH && mother != TARGET.Moon) initBody = 1;

		// Comet/asteroid in front of Sun
		for (int i=initBody; i<nbody; i++) {
			if (i > moon || i < mother.ordinal())
				addEvent(jd0, jdf, step, list, listEphem, 0, i, -1, obs, eph); // 0 = Sun, -1 = Sun radius and d<d0 => In front of Sun
		}
		// Planet/comet/asteroid behind Moon
		Object o[] = re_star.getReadElements();
		if (mother == TARGET.EARTH) {
			for (int i=1; i<nbody; i++) {
				if (i != moon) addEvent(jd0, jdf, step, list, listEphem, moon, i, -2, obs, eph); // 8 = Moon, -2 = Moon radius and d<d0 => behind Moon
			}

			// Star behind Moon
			for (int i=0; i<o.length; i++) {
				StarElement s = (StarElement) o[i];
				String name = s.name;
				int ind = name.indexOf("(");
				if (ind > 0) {
					name = name.substring(ind+1);
					name = name.substring(0,name.indexOf(")"));
				} else {
					name = "star #"+name;
				}
				addEvent(jd0, jdf, step, list, listEphem, moon,
						new LocationElement(s.rightAscension, s.declination, s.getDistance() * (Constant.PARSEC / (Constant.AU * 1000.0))),
						name, -2, s.magnitude, obs, eph); // 8 = Moon, -2 = Moon radius and d<d0 => star behind Moon
			}
			// Deep sky object behind Moon
			for (int i=0; i<objects.size(); i++) {
				Object[] s = (Object[]) objects.get(i);
				addEvent(jd0, jdf, step, list, listEphem, moon,
						(LocationElement) s[3],
						(String) s[0], -2, -100, obs, eph); // 8 = Moon, -2 = Moon radius and d<d0 => star behind Moon
			}
		}

		// Conjunctions between planets and comets/asteroids, not Moon, and not occultations
		for (int i=1; i<nbody-1; i++) {
			for (int j=i+1; j<nbody; j++) {
				if (i != moon && j != moon) addEvent(jd0, jdf, step, list, listEphem, i, j, 5, obs, eph);
			}
		}
		// Conjunctions between Moon and planets, but without occultations
		if (mother == TARGET.EARTH) {
			for (int i=1; i<moon; i++) {
				addEvent(jd0, jdf, step, list, listEphem, moon, i, 5, obs, eph);
			}
		}

		// Conjunctions between planets/Moon and stars
		for (int i=1; i<nbody; i++) {
			if (mother == TARGET.EARTH || (i != moon && i != mother.ordinal())) {
				for (int j=0; j<o.length; j++) {
					StarElement s = (StarElement) o[j];
					String name = s.name;
					int ind = name.indexOf("(");
					if (ind > 0) {
						name = name.substring(ind+1);
						name = name.substring(0,name.indexOf(")"));
					} else {
						name = "star #"+name;
					}
					double lim = 5;
					if (i > moon) lim = 1;
					if (i <= moon && s.magnitude > 1.5) lim = 1;
					addEvent(jd0, jdf, step, list, listEphem, i,
							new LocationElement(s.rightAscension, s.declination, s.getDistance() * (Constant.PARSEC / (Constant.AU * 1000.0))),
							name, lim, s.magnitude, obs, eph);
				}
			}
		}
		// Conjunctions between outer planets/comets/asteroids and deep sky objects
		for (int i=mother.ordinal()+1; i<nbody; i++) {
			if (i != moon) {
				double lim = 1;
				for (int j=0; j<objects.size(); j++) {
					Object[] s = (Object[]) objects.get(j);
					addEvent(jd0, jdf, step, list, listEphem, i, (LocationElement) s[3], (String) s[0], lim, -100, obs, eph);
				}
			}
		}

		return list;
	}

	private static void addEvent(double jd0, double jdf, double step, ArrayList<SimpleEventElement> list,
			ArrayList listEphem, int body0, int body1, double limitD, ObserverElement obs, EphemerisElement eph) throws JPARSECException {
		int index = -1;
		ArrayList<Double> x = new ArrayList<Double>();
		ArrayList<Double> y = new ArrayList<Double>();
		ArrayList<Double> yra = new ArrayList<Double>();
		ArrayList<Double> ydec = new ArrayList<Double>();
		ArrayList<Double> z = new ArrayList<Double>();
		String b0 = null, b1 = null;
		for (double jd = jd0; jd <=jdf; jd = jd + step) {
			index ++;
			ArrayList<EphemElement> ephem = (ArrayList<EphemElement>) listEphem.get(index);
			if (ephem.size() <= Math.max(body1, body0)) break;

		    EphemElement ephem0 = ephem.get(body0);
		    EphemElement ephem1 = ephem.get(body1);
		    if (ephem0 == null || ephem1 == null) break;

		    double d = LocationElement.getApproximateAngularDistance(ephem0.getEquatorialLocation(), ephem1.getEquatorialLocation()) * Constant.RAD_TO_DEG;
		    if (d < 1.0) d = LocationElement.getAngularDistance(ephem0.getEquatorialLocation(), ephem1.getEquatorialLocation()) * Constant.RAD_TO_DEG;
		    x.add(jd);
		    y.add(d);
		    yra.add(ephem0.getEquatorialLocation().getLongitude());
		    ydec.add(ephem0.getEquatorialLocation().getLatitude());
		    double size = (ephem0.angularRadius + ephem1.angularRadius) * Constant.RAD_TO_DEG;
		    if (ephem0.distance < ephem1.distance && limitD != -2 && limitD != 5 && limitD != 1) size = 0;
		    z.add(size);

		    if (jd == jd0) {
		    	b0 = ephem0.name;
		    	b1 = ephem1.name;
		    }
		}

		double xv[] = DataSet.arrayListToDoubleArray(x);
		double yv[] = DataSet.arrayListToDoubleArray(y);
		boolean down = false;
		for (int i=0; i<xv.length-1; i++) {
			if (yv[i+1] < yv[i]) {
				down = true;
			} else {
				if (down) {
					int mini = i-2, maxi = i + 2;
					if (mini < 0) {
						mini = 0;
						maxi = 4;
					}
					if (maxi >= xv.length) {
						maxi = xv.length-1;
						mini = maxi - 4;
					}

					int meani = (mini + maxi) / 2;
					double vx[] = DataSet.getSubArray(xv, mini, maxi);
					double vy[] = DataSet.getSubArray(yv, mini, maxi);
					Interpolation in = new Interpolation(vx, vy, false);
					Point2D p = in.MeeusExtremum();
					if (p != null) {
						double limit = limitD;
						if (limit < 0) limit = z.get(meani);
						if (p.getY() < limit) {
							double yvra[] = DataSet.arrayListToDoubleArray(yra);
							double yvdec[] = DataSet.arrayListToDoubleArray(ydec);
							double vyra[] = DataSet.getSubArray(yvra, mini, maxi);
							double vydec[] = DataSet.getSubArray(yvdec, mini, maxi);
							Interpolation inra = new Interpolation(vx, vyra, false);
							Interpolation indec = new Interpolation(vx, vydec, false);
							LocationElement loc = new LocationElement(inra.MeeusInterpolation(p.getX()),
									indec.MeeusInterpolation(p.getX()), 1.0);

							EVENT event = EVENT.CONJUNCTION;
							String details = "";
							if (limitD == -1) {
								details = b1+" "+Translate.getEntry(1009, LANGUAGE.ENGLISH)+" "+b0;
								event = EVENT.TRANSIT;
							}
							if (limitD == -2) {
								details = b1+" "+Translate.getEntry(1010, LANGUAGE.ENGLISH)+" "+b0;
								event = EVENT.OCCULTATION;
							}
							if (limitD > 0) details = Translate.getEntry(1008, LANGUAGE.ENGLISH)+" "+b0+"-"+b1;
							String add0 = Functions.formatValue(p.getY(), 1);
							String add = " (d = "+add0+"\u00b0)";
							if (add0.equals("0.0")) {
								add0 = Functions.formatValue(p.getY()*60, 1);
								add = " (d = "+add0+"')";								
							}
							double init = -1, end = -1;
							if (event != EVENT.CONJUNCTION) {
								double find = -1, previous = -1;
								int np = 200;
								in = new Interpolation(xv, yv, false);
								for (int ii=0; ii<=np; ii++) {
									double xx = xv[meani] - 0.1 * ii / (double) np;
									double yy = 0;
									try {
										yy = in.splineInterpolation(xx) - limit;
									} catch (JPARSECException e) { continue; }
									if (Math.abs(yy) < find || find == -1) {
										find = Math.abs(yy);
										init = xx;
									}
									if (yy < 0 && previous > 0 || yy > 0 && previous < 0) break;
									previous = yy;
								}
								find = -1;
								previous = -1;
								for (int ii=0; ii<=np; ii++) {
									double xx = xv[meani] + 0.1 * ii / (double) np;
									double yy = 0;
									try {
										yy = in.splineInterpolation(xx) - limit;
									} catch (JPARSECException e) { continue; }
									if (Math.abs(yy) < find || find == -1) {
										find = Math.abs(yy);
										end = xx;
									}
									if (yy < 0 && previous > 0 || yy > 0 && previous < 0) break;
									previous = yy;
								}
								if (init != -1 && end != -1 && init != end) {
									TimeElement timeE = new TimeElement(p.getX(), SCALE.TERRESTRIAL_TIME);
									add0 = Functions.formatValue(p.getY(), 1);
									add = " (d = "+add0+"\u00b0";
									if (add0.equals("0.0")) {
										add0 = Functions.formatValue(p.getY()*60, 1);
										add = " (d = "+add0+"'";								
									}

									add += ", "+
										Translate.getEntry(1022, LANGUAGE.ENGLISH).toLowerCase()+" "+timeE.toMinString()+")";
								}
							}
							details += add;

							if ((limitD != 5 && limitD != 1) || p.getY() >= z.get(meani)) {
								SimpleEventElement s = new SimpleEventElement(p.getX(), event, details);
								s.eventLocation = toApproximateApparent(loc, s.time);
								s.body = b0;
								TimeElement time = new TimeElement(s.time, SCALE.TERRESTRIAL_TIME);
								LocationElement locH = CoordinateSystem.equatorialToHorizontal(s.eventLocation, time, obs, eph);
								if (locH.getLatitude() < 0.0) s.details += ". "+Translate.getEntry(1021, LANGUAGE.ENGLISH)+" "+obs.getName();
								if (init != -1 && end != -1 && init != end) {
									s.time = init;
									s.endTime = end;
								}
								list.add(s);
							}
						}
					}
					i = maxi;
				}
				down = false;
			}
		}
	}

	private static void addEvent(double jd0, double jdf, double step, ArrayList<SimpleEventElement> list,
			ArrayList listEphem, int body0, LocationElement loc1, String b1, double limitD,
			float magStar, ObserverElement obs, EphemerisElement eph) throws JPARSECException {
		int index = -1;
		ArrayList<Double> x = new ArrayList<Double>();
		ArrayList<Double> y = new ArrayList<Double>();
		ArrayList<Double> yra = new ArrayList<Double>();
		ArrayList<Double> ydec = new ArrayList<Double>();
		ArrayList<Double> z = new ArrayList<Double>();
		String b0 = null;
		for (double jd = jd0; jd <=jdf; jd = jd + step) {
			index ++;
			ArrayList<EphemElement> ephem = (ArrayList<EphemElement>) listEphem.get(index);
			if (ephem.size() <= body0) break;
		    EphemElement ephem0 = ephem.get(body0);
		    if (ephem0 == null) break;

		    double d = LocationElement.getApproximateAngularDistance(ephem0.getEquatorialLocation(), loc1) * Constant.RAD_TO_DEG;
		    if (d < 1.0) d = LocationElement.getAngularDistance(ephem0.getEquatorialLocation(), loc1) * Constant.RAD_TO_DEG;
		    x.add(jd);
		    y.add(d);
		    yra.add(ephem0.getEquatorialLocation().getLongitude());
		    ydec.add(ephem0.getEquatorialLocation().getLatitude());
		    double size = ephem0.angularRadius * Constant.RAD_TO_DEG;
		    if (ephem0.distance < loc1.getRadius() && limitD != -2 && limitD != 5 && limitD != 1) size = 0;
		    z.add(size);

		    if (jd == jd0) b0 = ephem0.name;
		}

		double xv[] = DataSet.arrayListToDoubleArray(x);
		double yv[] = DataSet.arrayListToDoubleArray(y);
		boolean down = false;
		for (int i=0; i<xv.length-1; i++) {
			if (yv[i+1] < yv[i]) {
				down = true;
			} else {
				if (down) {
					int mini = i-2, maxi = i + 2;
					if (mini < 0) {
						mini = 0;
						maxi = 4;
					}
					if (maxi >= xv.length) {
						maxi = xv.length-1;
						mini = maxi - 4;
					}

					int meani = (mini + maxi) / 2;
					double vx[] = DataSet.getSubArray(xv, mini, maxi);
					double vy[] = DataSet.getSubArray(yv, mini, maxi);
					Interpolation in = new Interpolation(vx, vy, false);
					Point2D p = in.MeeusExtremum();
					if (p != null) {
						double limit = limitD;
						if (limit < 0) limit = z.get(meani);
						if (p.getY() < limit) {
							double yvra[] = DataSet.arrayListToDoubleArray(yra);
							double yvdec[] = DataSet.arrayListToDoubleArray(ydec);
							double vyra[] = DataSet.getSubArray(yvra, mini, maxi);
							double vydec[] = DataSet.getSubArray(yvdec, mini, maxi);
							Interpolation inra = new Interpolation(vx, vyra, false);
							Interpolation indec = new Interpolation(vx, vydec, false);
							LocationElement loc = new LocationElement(inra.MeeusInterpolation(p.getX()),
									indec.MeeusInterpolation(p.getX()), 1.0);

							EVENT event = EVENT.CONJUNCTION;
							String details = "";
							if (limitD == -1) {
								event = EVENT.TRANSIT;
								details = b1+" "+Translate.getEntry(1009, LANGUAGE.ENGLISH)+" "+b0;
							}
							if (limitD == -2) {
								event = EVENT.OCCULTATION;
								details = b1+" "+Translate.getEntry(1010, LANGUAGE.ENGLISH)+" "+b0;
							}
							if (limitD > 0) {
								details = Translate.getEntry(1008, LANGUAGE.ENGLISH)+" "+b0+"-"+b1;
								if (p.getY() < z.get(meani))
									details = b1+" "+Translate.getEntry(164, LANGUAGE.ENGLISH).toLowerCase()+" "+Translate.getEntry(161, LANGUAGE.ENGLISH)+" "+b0;
							}
							String add0 = Functions.formatValue(p.getY(), 1);
							String add = " (d = "+add0+"\u00b0";
							if (add0.equals("0.0")) {
								add0 = Functions.formatValue(p.getY()*60, 1);
								add = " (d = "+add0+"'";								
							}
							if (magStar > -100) add += ", mag = "+Functions.formatValue(magStar, 2);
							add += ")";
							double init = -1, end = -1;
							if (event != EVENT.CONJUNCTION) {
								double find = -1, previous = -1;
								int np = 200;
								in = new Interpolation(xv, yv, false);
								for (int ii=0; ii<=np; ii++) {
									double xx = xv[meani] - 0.1 * ii / (double) np;
									double yy = 0;
									try {
										yy = in.splineInterpolation(xx) - limit;
									} catch (JPARSECException e) { continue; }
									if (Math.abs(yy) < find || find == -1) {
										find = Math.abs(yy);
										init = xx;
									}
									if (yy < 0 && previous > 0 || yy > 0 && previous < 0) break;
									previous = yy;
								}
								find = -1;
								previous = -1;
								for (int ii=0; ii<=np; ii++) {
									double xx = xv[meani] + 0.1 * ii / (double) np;
									double yy = 0;
									try {
										yy = in.splineInterpolation(xx) - limit;
									} catch (JPARSECException e) { continue; }
									if (Math.abs(yy) < find || find == -1) {
										find = Math.abs(yy);
										end = xx;
									}
									if (yy < 0 && previous > 0 || yy > 0 && previous < 0) break;
									previous = yy;
								}
								if (init != -1 && end != -1 && init != end) {
									TimeElement timeE = new TimeElement(p.getX(), SCALE.TERRESTRIAL_TIME);
									add0 = Functions.formatValue(p.getY(), 1);
									add = " (d = "+add0+"\u00b0";
									if (add0.equals("0.0")) {
										add0 = Functions.formatValue(p.getY()*60, 1);
										add = " (d = "+add0+"'";								
									}
									add += ", "+
										Translate.getEntry(1022, LANGUAGE.ENGLISH).toLowerCase()+" "+timeE.toMinString();
									if (magStar > -100) add += ", mag = "+Functions.formatValue(magStar, 2);
									add += ")";
								}
							}
							details += add;
							if ((limitD != 5 && limitD != 1) || p.getY() >= z.get(meani)) {
								SimpleEventElement s = new SimpleEventElement(p.getX(), event, details);
								s.eventLocation = toApproximateApparent(loc, s.time);
								s.body = b0;
								TimeElement time = new TimeElement(s.time, SCALE.TERRESTRIAL_TIME);
								LocationElement locH = CoordinateSystem.equatorialToHorizontal(s.eventLocation, time, obs, eph);
								if (locH.getLatitude() < 0.0) s.details += ". "+Translate.getEntry(1021, LANGUAGE.ENGLISH)+" "+obs.getName();
								if (init != -1 && end != -1 && init != end) {
									s.time = init;
									s.endTime = end;
								}
								list.add(s);
							}
						}
					}

					i = maxi;
				}
				down = false;
			}
		}
	}

	private static LocationElement toApproximateApparent(LocationElement locIn, double equinox) throws JPARSECException {
		LocationElement loc = locIn.clone();
		loc.setRadius(1000 * Constant.RAD_TO_ARCSEC); // 1000 pctoApproximateApparent
		EphemerisElement eph = new EphemerisElement();

		double baryc[] = Ephem.eclipticToEquatorial(PlanetEphem.getGeocentricPosition(equinox, TARGET.Solar_System_Barycenter, 0.0, false, null), Constant.J2000, eph);
		double light_time = loc.getRadius() * Constant.LIGHT_TIME_DAYS_PER_AU;
		double r[] = Ephem.aberration(loc.getRectangularCoordinates(), baryc, light_time);

		r = Precession.precessFromJ2000(equinox, r, eph);
		loc = LocationElement.parseRectangularCoordinates(Nutation.nutateInEquatorialCoordinates(equinox, eph, r, true));
		return loc;
	}

	// Get events when Lunar Transient Phenomena (LTP) are more common. This method is based on historical records by
	// NASA showing frequent/periodical LTP in certain craters at sunrise/sunset, so they can be 'predicted'. Although probably
	// nothing strange would be visible within a few hours around the event, it is also nice to see the sun rising on them!
	private static ArrayList<SimpleEventElement> getLTP(double jd0, double jdf, ObserverElement obs, EphemerisElement eph) throws JPARSECException {
		ArrayList<SimpleEventElement> list = new ArrayList<SimpleEventElement>();

		double step = 2.0 / (60.0 * 24.0);
		eph.targetBody = TARGET.Moon;
		eph.algorithm = ALGORITHM.MOSHIER;
		String[] crater = new String[] {
				"Ptolemaeus -1.9 -9.3 0.7",
				"Alphonsus -3.2 -13.7 0.7",
				"Gassendi -40.1 -17.6 0.7",
				"Gassendi -40.1 -17.6 -3",
				"Plato -9.4 51.6 0.7",
				"Lunar-X 0.9 -25.3 0.5"
		};
		if (cratersOnlyLunarX) crater = new String[] {"Lunar-X 0.9 -25.3 0.5"};
		double maxDif = 0.05;
		boolean event[] = new boolean[crater.length];
		for (int i=0; i<crater.length; i++) {
			event[i] = false;
		}

		for (double jd = jd0; jd <=jdf; jd = jd + step) {
			EphemElement ephem = null;
			boolean fast = !eph.preferPrecisionInEphemerides && Math.abs(jd-Constant.J2000) < 36525;
			TimeElement time = new TimeElement(jd, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
			if (fast) {
				double sunPos[] = Saros.getSunPosition(jd);
				double moonPos[] = Saros.getMoonPosition(jd);
				double obliquity = Obliquity.meanObliquity(Functions.toCenturies(jd), eph);
				obliquity += Nutation.getFastNutation(jd)[1];
				double lst = SiderealTime.apparentSiderealTime(time, obs, eph);
				moonPos[2] *= Constant.EARTH_RADIUS / Constant.AU;
				double PH = 0.0, rr = sunPos[2] * moonPos[2];
				double[] rotationModel = PhysicalParameters.getIAU2009Model(TARGET.Moon, jd, rr, PH);
				if (rotationModel != null) {
					ephem = new EphemElement();
					ephem.northPoleRA = rotationModel[1];
					ephem.northPoleDEC = rotationModel[2];
					EphemElement ephem_sun = new EphemElement();
					LocationElement loc = CoordinateSystem.eclipticToEquatorial(new LocationElement(sunPos[0], sunPos[1], sunPos[2]), obliquity, true);
					ephem_sun.setEquatorialLocation(loc);
					ephem_sun.setEquatorialLocation(Ephem.fastTopocentricCorrection(time, obs, eph, ephem_sun, lst));
					loc = CoordinateSystem.eclipticToEquatorial(new LocationElement(moonPos[0], moonPos[1], moonPos[2]), obliquity, true);
					ephem.setEquatorialLocation(loc);
					ephem = PhysicalParameters.calcAxis(jd, ephem_sun, ephem, rotationModel[3], rotationModel[4], eph, obs.getMotherBody());
					ephem.setEquatorialLocation(Ephem.fastTopocentricCorrection(time, obs, eph, ephem, lst));
				}
			} else {
				ephem = Ephem.getEphemeris(time, obs, eph, false);
			}

			double lonlsolar = ephem.subsolarLongitude;
			double latlsolar = ephem.subsolarLatitude;
			double sinLatSolar = FastMath.sin(Constant.PI_OVER_TWO-latlsolar);
			double cosLatSolar = FastMath.cos(Constant.PI_OVER_TWO-latlsolar);
			double cosLonSolar = FastMath.cos(lonlsolar);
			double sinLonSolar = FastMath.sin(lonlsolar);
			double minValue = -1;
			for (int i=0; i<crater.length; i++) {
				double craterLon = Double.parseDouble(FileIO.getField(2, crater[i], " ", true)) * Constant.DEG_TO_RAD;
				double craterLat = Double.parseDouble(FileIO.getField(3, crater[i], " ", true)) * Constant.DEG_TO_RAD;
				double offset = Double.parseDouble(FileIO.getField(4, crater[i], " ", true));

				// Get elevation of sun from crater
				double sinCraterLat = FastMath.sin(Constant.PI_OVER_TWO-craterLat);
				double zx = (cosLonSolar * sinLatSolar - FastMath.cos(craterLon) * sinCraterLat);
				double zy = (sinLonSolar * sinLatSolar - FastMath.sin(craterLon) * sinCraterLat);
				double zz = (cosLatSolar - FastMath.cos(Constant.PI_OVER_TWO-craterLat));
				double tmp2 = zx * zx + zy * zy + zz * zz;
				double sunElev = (Constant.PI_OVER_TWO - Math.acos(1.0 - tmp2 * 0.5)) * Constant.RAD_TO_DEG;
				double oldMV = minValue;
				if (Math.abs(sunElev) < minValue || minValue == -1) minValue = Math.abs(sunElev);

				String name = FileIO.getField(1, crater[i], " ", true);
				if (Math.abs(sunElev+offset) < maxDif) {
					if (!event[i]) {
						if (fast) ephem = Ephem.getEphemeris(time, obs, eph, false);
						String details = Translate.getEntry(1007, LANGUAGE.ENGLISH)+" "+name;
						if (!name.equals("Lunar-X")) details = Translate.getEntry(1078, LANGUAGE.ENGLISH)+" "+name;
						SimpleEventElement s = new SimpleEventElement(jd, EVENT.CRATER, details);
						s.body = TARGET.Moon.getEnglishName();
						s.eventLocation = ephem.getEquatorialLocation();
						list.add(s);
						event[i] = true;
					}
					minValue = oldMV;
				} else {
					if (event[i]) minValue = oldMV;
					event[i] = false;
				}
			}

			if (minValue > 0.2) {
				double increment = minValue / 64.0; //16.0;
				jd += increment;
			}
		}
		return list;
	}

	private static ArrayList<SimpleEventElement> getCalendarEvents(double jd0, double jdf, int year0, int yearf,
			ObserverElement observer, EphemerisElement eph) throws JPARSECException {
		ArrayList<SimpleEventElement> list = new ArrayList<SimpleEventElement>();

		MayanLongCount h = new MayanLongCount((int) (jd0 + 0.5));
		MayanLongCount hNew = new MayanLongCount(h.baktun+1, 0, 0, 0, 0);
		int lastDay = (int) (hNew.toJulianDay() + 0.5) - 1;
		Gregorian g = new Gregorian(lastDay);
		double jd = g.getJulianDate() + 0.5;
		if (jd > jd0 && jd < jdf) {
			String details = "baktun "+h.baktun+" ends (Mayan calendar)";
			SimpleEventElement s = new SimpleEventElement(jd, EVENT.CALENDAR, details);
			list.add(s);
		}

		for (int year = year0; year <= yearf; year ++) {
			int yearAstronomical = year;
			if (year < 0) yearAstronomical ++;
			CALENDAR input_calendar = CALENDAR.GREGORIAN;
			for (int i=8;i<CalendarGenericConversion.CALENDAR_NAMES.length;i++) {
				CALENDAR output_calendar = CALENDAR.values()[i];
					int date[] = CalendarGenericConversion.GenericConversion(input_calendar, output_calendar, yearAstronomical, 1, 1);

					int monthForNewYear = 1;
					if (i == CalendarGenericConversion.CALENDAR.HEBREW.ordinal()) monthForNewYear = 7;

					int date_back[] = CalendarGenericConversion.GenericConversion(output_calendar, input_calendar, date[0] + 1,	monthForNewYear, 1);
					AstroDate astro_back = new AstroDate(date_back[0], date_back[1] , date_back[2]);

					jd = astro_back.jd();
					if (jd > jd0 && jd < jdf) {
						String details = "new "+CalendarGenericConversion.CALENDAR_NAMES[i]+" year";
						SimpleEventElement s = new SimpleEventElement(jd, EVENT.CALENDAR, details);
						list.add(s);
					}
			}
			Chinese h0 = new Chinese(jd);
			int yearTotal = h0.getYearNumber();
			Chinese h2 = new Chinese(Chinese.newYear(year));
			if (h2.getYearNumber() == yearTotal) h2 = new Chinese(Chinese.newYear(year+1));
			jd = h2.getJulianDate();
			if (jd > jd0 && jd < jdf) {
				String details = "new Chinese year";
				SimpleEventElement s = new SimpleEventElement(jd, EVENT.CALENDAR, details);
				list.add(s);
			}
		}

		for (int year1 = year0; year1 <= yearf; year1 ++) {
			int year = year1;
			if (year < 0) year ++; // to astronomical year
			AstroDate astro_p1 = new AstroDate(Calendar.jdFromFixed(Ecclesiastical.easter(year)));
			AstroDate astro_p2 = new AstroDate(Calendar.jdFromFixed(Ecclesiastical.pentecost(year)));

			SimpleEventElement s1 = new SimpleEventElement(astro_p1.jd(), EVENT.CALENDAR, Translate.getEntry(1299, LANGUAGE.ENGLISH));
			list.add(s1);
			SimpleEventElement s2 = new SimpleEventElement(astro_p2.jd(), EVENT.CALENDAR, Translate.getEntry(1300, LANGUAGE.ENGLISH));
			list.add(s2);

			double times[] = TimeScale.getDSTStartEnd((new AstroDate(year1, 6, 1)).jd(), observer);
			double dst1 = (new AstroDate(TimeScale.getJD(new TimeElement(times[0], SCALE.UNIVERSAL_TIME_UTC), observer, eph, SCALE.TERRESTRIAL_TIME))).jd();
			double dst2 = (new AstroDate(1.0/24.0 + TimeScale.getJD(new TimeElement(times[1], SCALE.UNIVERSAL_TIME_UTC), observer, eph, SCALE.TERRESTRIAL_TIME))).jd();

			SimpleEventElement s3 = new SimpleEventElement(dst1, EVENT.CALENDAR, "DST1");
			list.add(s3);
			SimpleEventElement s4 = new SimpleEventElement(dst2, EVENT.CALENDAR, "DST2");
			list.add(s4);
		}

		return list;
	}

	/**
	 * ID constant for selecting eclipsing stars.
	 */
	private static final int VARIABLE_STARS_ECLIPSING = 0;
	/**
	 * ID constant for selecting Mira-type stars.
	 */
	private static final int VARIABLE_STARS_MIRA = 1;
	/**
	 * ID constant for selecting both eclipsing and Mira-type stars.
	 */
	private static final int VARIABLE_STARS_ALL = 2;

	/**
	 * Returns ephemerides for variable stars according to AAVSO bulletins and the
	 * <i>Up-to-date Linear Elements of Close Binaries</i>, (J.M. Kreiner, 2004),
	 * Acta Astronomica, vol. 54, pp 207-210. See AAVSO website and
	 * http://www.as.up.krakow.pl/o-c/cont.html for for more information on the
	 * variable stars.
	 * @param init Time object for start time.
	 * @param end Time object for end time.
	 * @param observer Observer object.
	 * @param which Selects which objects to retrieve, constants defined in this class.
	 * @param minMag Minimum magnitude to filter results, or -1 to consider all.
	 * @return A set of fields containing variable star type (ECLIPSING or MIRA), and, for
	 * eclipsing stars, name, RA, DEC (radians), magnitude range, phase; next minima date, jd of
	 * next minima. For long-period variables fields are: name, RA, DEC, magnitude range,
	 * next maxima; next minima, jd of next maxima; jd of next minima (local time). In
	 * case of error or date outside 1900-2100 null will be returned.
	 */
	private static ArrayList<String[]> getVariableStarsEphemerides(TimeElement init, TimeElement end, ObserverElement observer,
			int which, double minMag) {
		try {
			EphemerisElement eph = new EphemerisElement(TARGET.NOT_A_PLANET, EphemerisElement.COORDINATES_TYPE.APPARENT,
					EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.TOPOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
					EphemerisElement.FRAME.ICRF, EphemerisElement.ALGORITHM.ORBIT);

			AstroDate astro = new AstroDate(TimeScale.getJD(init, observer, eph, TimeElement.SCALE.UNIVERSAL_TIME_UTC));
			int year = astro.getYear();
			double jdEnd = TimeScale.getJD(end, observer, eph, SCALE.LOCAL_TIME);

			if (year < 1900 || year > 2100) return null;

			ArrayList<String[]> a = new ArrayList<String[]>();
			if (which == VARIABLE_STARS_ECLIPSING || which == VARIABLE_STARS_ALL) {
				ReadFile re = new ReadFile();
				re.setPath(VariableStarElement.PATH_VARIABLE_STAR_CATALOG);
				re.readFileOfVariableStars();
				int imax = re.getNumberOfObjects();

				for (int i=0; i<imax;i++) {
					VariableStarElement vstar = re.getVariableStarElement(i);

					try {
						double primMag = Double.parseDouble(FileIO.getField(1, vstar.magRange, "-", true));

						boolean ok = true;
						if (primMag > minMag && minMag != -1) ok = false;

						if (ok) {
							TimeElement time0 = init.clone();
							while (true) {
								vstar.calcEphemeris(time0, observer, false);
	
								LocationElement loc = new LocationElement(vstar.rightAscension, vstar.declination, 1.0);
								//String secondaryFlag = "";
								//if (vstar.onlySecondaryMinima()) secondaryFlag = "*";
	
								double jd = vstar.getNextMinima();
								if (jd == 0) break;
								String date1 = "-";
								astro = new AstroDate(jd);
								String l = "%day of %month, %hour:%minute LT";
								date1 = replaceVars(l, new String[] {"year", "month", "day", "hour", "minute"}, new String[] {""+astro.getYear(), CalendarGenericConversion.getMonthName(astro.getMonth(), CalendarGenericConversion.CALENDAR.GREGORIAN), ""+astro.getDay(), ""+astro.getHour(), ""+astro.getMinute()});
	
								String c[] = new String[] {
										"ECLIPSING",
										vstar.name.trim(),
										""+loc.getLongitude(),
										""+loc.getLatitude(),
										vstar.magRange,
										Functions.formatValue(vstar.getPhase(), 2)+"; "+date1, // phase, next minima
										""+jd
										//, vstar.spectralType+"|"+vstar.eclipsingType+"|"+vstar.minimaDuration
								};
	
								a.add(c);
								if (jd + vstar.period > jdEnd) break;
								time0 = new TimeElement(jd + vstar.period / 2.0, SCALE.LOCAL_TIME);
							}
						}
					} catch (Exception exc) {}
				}
			}

			if (which == VARIABLE_STARS_MIRA || which == VARIABLE_STARS_ALL) {
				ReadFile re = new ReadFile();
				String path = VariableStarElement.PATH_VARIABLE_STAR_AAVSO_BULLETIN_2011;
				if (year != 2011) path = DataSet.replaceAll(path, "2011", ""+year, false);
				re.setPath(path);
				try {
					re.readFileOfVariableStars();
					int imax = re.getNumberOfObjects();

					for (int i=0; i<imax;i++) {
						VariableStarElement vstar = re.getVariableStarElement(i);

						String m = vstar.magRange;
						if (m.lastIndexOf("-") > 0) {
							m = m.substring(0, m.indexOf("-"));
						}
						double primMag = Double.parseDouble(m);

						boolean ok = true;
						if (primMag > minMag && minMag != -1) ok = false;

						if (ok) {
							LocationElement loc = new LocationElement(vstar.rightAscension, vstar.declination, 1.0);

							double jd = vstar.getNextMaxima(init, observer);
							String date1 = "-";
							String l = "%month, %day, %year";
							double jdMax = 0;
							if (jd != 0.0) {
								astro = new AstroDate(jd);
								date1 = replaceVars(l, new String[] {"year", "month", "day"}, new String[] {""+astro.getYear(), CalendarGenericConversion.getMonthName(astro.getMonth(), CalendarGenericConversion.CALENDAR.GREGORIAN), ""+astro.getDay()});
								jdMax = jd;
							}
							jd = vstar.getNextMinima(init, observer);
							String date2 = "-";
							if (jd != 0.0) {
								astro = new AstroDate(jd);
								date2 = replaceVars(l, new String[] {"year", "month", "day"}, new String[] {""+astro.getYear(), CalendarGenericConversion.getMonthName(astro.getMonth(), CalendarGenericConversion.CALENDAR.GREGORIAN), ""+astro.getDay()});
							}

							String c[] = new String[] {
									"MIRA",
									vstar.name.trim(),
									""+loc.getLongitude(),
									""+loc.getLatitude(),
									vstar.magRange,
									date1+"; "+date2,
									""+jdMax+";"+jd
							};

							a.add(c);
						}
					}
				} catch (Exception exc) {
					exc.printStackTrace();
				}
			}

			return a;
		} catch (Exception exc) {
			Logger.log(Logger.LEVEL.ERROR, exc.getMessage()+", at "+JPARSECException.getTrace(exc.getStackTrace())+".");
		}
		return null;
	}

	private static ArrayList<SimpleEventElement> getMoonEvents(double jd0, double jdf, ObserverElement obs, EphemerisElement ephIn)
			throws JPARSECException {
		ArrayList<SimpleEventElement> list = new ArrayList<SimpleEventElement>();
		TimeElement timei = new TimeElement(jd0, SCALE.TERRESTRIAL_TIME);
		TimeElement timef = new TimeElement(jdf, SCALE.TERRESTRIAL_TIME);
		if (ephIn.algorithm == ALGORITHM.ARTIFICIAL_SATELLITE || 
				ephIn.algorithm == ALGORITHM.NEWCOMB || ephIn.algorithm == ALGORITHM.PROBE ||
				ephIn.algorithm == ALGORITHM.STAR) ephIn.algorithm = ALGORITHM.MOSHIER;

		// Return only several events at the same time for non-mutual phenomena
		boolean onlySimultaneousEvents = moonEventsOnlySeveralNonMutualAtSameTime;
		if (ephIn.targetBody == TARGET.JUPITER || ephIn.targetBody == TARGET.SATURN ||
				ephIn.targetBody == TARGET.URANUS) {
			MoonEvent me = new MoonEvent(timei, obs, ephIn, timef, 300, 10, true);
			MoonEventElement me1[] = me.getPhenomena();
			for (int i=0; i<me1.length; i++) {
				if (!onlySimultaneousEvents || me1[i].severalSimultaneousEvents) {
					String details = me1[i].details;
					SimpleEventElement se = new SimpleEventElement(me1[i].startTime, EVENT.OCCULTATION, details);
					if (me1[i].eventType == MoonEventElement.EVENT.ECLIPSED) se.eventType = SimpleEventElement.EVENT.ECLIPSE;
					if (me1[i].eventType == MoonEventElement.EVENT.OCCULTED) se.eventType = SimpleEventElement.EVENT.OCCULTATION;
					if (me1[i].eventType == MoonEventElement.EVENT.TRANSIT) se.eventType = SimpleEventElement.EVENT.TRANSIT;
					if (me1[i].eventType == MoonEventElement.EVENT.SHADOW_TRANSIT) se.eventType = SimpleEventElement.EVENT.SHADOW_TRANSIT;
					se.endTime = me1[i].endTime;
					se.body = me1[i].mainBody.getEnglishName();
					se.secondaryBody = me1[i].secondaryBody.getEnglishName();
					list.add(se);
				}
			}
		}

		// Return all events for mutual phenomena
		if (!moonEventsAlsoMutualEvents) return list;
		onlySimultaneousEvents = false;
		if (ephIn.targetBody == TARGET.JUPITER || ephIn.targetBody == TARGET.SATURN ||
				ephIn.targetBody == TARGET.URANUS) {
			MoonEvent me = new MoonEvent(timei, obs, ephIn, timef, 100, 30, true);
			MoonEventElement me2[] = me.getMutualPhenomena(false);
			for (int i=0; i<me2.length; i++) {
				if (!onlySimultaneousEvents || me2[i].severalSimultaneousEvents) {
					String details = me2[i].details;
					SimpleEventElement se = new SimpleEventElement(me2[i].startTime, EVENT.OCCULTATION, details);
					if (me2[i].eventType == MoonEventElement.EVENT.ECLIPSED) se.eventType = SimpleEventElement.EVENT.ECLIPSE;
					if (me2[i].eventType == MoonEventElement.EVENT.OCCULTED) se.eventType = SimpleEventElement.EVENT.OCCULTATION;
					if (me2[i].eventType == MoonEventElement.EVENT.TRANSIT) se.eventType = SimpleEventElement.EVENT.TRANSIT;
					if (me2[i].eventType == MoonEventElement.EVENT.SHADOW_TRANSIT) se.eventType = SimpleEventElement.EVENT.SHADOW_TRANSIT;
					se.endTime = me2[i].endTime;
					se.body = me2[i].mainBody.getEnglishName();
					se.secondaryBody = me2[i].secondaryBody.getEnglishName();
					list.add(se);
				}
			}
		}

		return list;
	}
}
