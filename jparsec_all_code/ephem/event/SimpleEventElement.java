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

import jparsec.time.*;
import jparsec.time.TimeElement.SCALE;
import jparsec.util.*;
import jparsec.util.Translate.LANGUAGE;
import jparsec.math.Constant;
import jparsec.observer.*;
import jparsec.astronomy.CoordinateSystem;
import jparsec.ephem.*;
import jparsec.ephem.EphemerisElement.ALGORITHM;
import jparsec.ephem.EphemerisElement.COORDINATES_TYPE;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.planets.EphemElement;
import jparsec.graph.DataSet;
import jparsec.io.FileIO;

import java.io.*;

/**
 * Manages data related to simple events.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class SimpleEventElement implements Serializable {
	private static final long serialVersionUID = 1L;

	/** ID value for the end time of an event when that event has no end time defined. */
	public static final double END_TIME_NOT_APPLICABLE = -1E100;

	/**
	 * Holds the time of the event as a Julian day in TT. In case the event
	 * has a start and end times, set here the start time.
	 */
	public double time;
	/**
	 * Holds the time of the end of an event as a Julian day in TT. Only set
	 * for certain events, like those for natural satellites.
	 */
	public double endTime = END_TIME_NOT_APPLICABLE;
	/**
	 * Event id code. Constants defined in this class.
	 */
	public EVENT eventType;
	/**
	 * Holds details on the event.
	 */
	public String details;
	/**
	 * The equatorial position where this event occurs. Null by default, but
	 * can be used manually to set this information. For certain events it is
	 * set, like variable stars (position of the star), occultations and conjunctions.
	 */
	public LocationElement eventLocation = null;
	/**
	 * The name of the body that produces the event.
	 */
	public String body;
	/**
	 * The name of the secondary body that produces the event in case there are two.
	 * This occurs with eclipses of natural satellites, where the body is the satellite and the
	 * secondary body is the mother planet. For mutual events the secondary body is the body
	 * behind the primary one.
	 */
	public String secondaryBody;

	/**
	 * The set of events.
	 */
	public enum EVENT {
		/** ID constant for the full Moon event. */
		MOON_FULL,
		/** ID constant for the new Moon event. */
		MOON_NEW,
		/** ID constant for the Moon first quarter event. */
		MOON_FIRST_QUARTER,
		/** ID constant for the Moon last quarter event. */
		MOON_LAST_QUARTER,
		/** ID constant for a lunar eclipse event. */
		MOON_LUNAR_ECLIPSE,
		/** ID constant for a solar eclipse event. */
		MOON_SOLAR_ECLIPSE,
		/** ID constant for a spring equinox event. */
		SUN_SPRING_EQUINOX,
		/** ID constant for a autumn equinox event. */
		SUN_AUTUMN_EQUINOX,
		/** ID constant for a summer solstice event. */
		SUN_SUMMER_SOLSTICE,
		/** ID constant for a winter solstice event. */
		SUN_WINTER_SOLSTICE,
		/** ID constant for the minimum distance event. */
		PLANET_MINIMUM_DISTANCE,
		/** ID constant for the minimum distance from Sun event. */
		PLANET_MINIMUM_DISTANCE_FROM_SUN,
		/** ID constant for the minimum elongation event. */
		PLANET_MINIMUM_ELONGATION,
		/** ID constant for the maximum distance event. */
		PLANET_MAXIMUM_DISTANCE,
		/** ID constant for the maximum distance from Sun event. */
		PLANET_MAXIMUM_DISTANCE_FROM_SUN,
		/** ID constant for the maximum elongation event. */
		PLANET_MAXIMUM_ELONGATION,
		/** ID constant for the transit of the Great Red Spot on Jupiter. */
		JUPITER_GRS_TRANSIT,
		/** ID constant for the Lunar perigee. */
		MOON_PERIGEE,
		/** ID constant for the Lunar apogee. */
		MOON_APOGEE,
		/** ID constant for Mercury transit. */
		MERCURY_TRANSIT,
		/** ID constant for Mercury transit. */
		VENUS_TRANSIT,
		/** ID constant for a meteor shower event. */
		METEOR_SHOWER,
		/** ID constant for a pass of the Moon through its ascending node. */
		MOON_ASCENDING_NODE,
		/** ID constant for a pass of the Moon through its descending node. */
		MOON_DESCENDING_NODE,
		/** ID constant for a maximum declination of the Moon. */
		MOON_MAXIMUM_DECLINATION,
		/** ID constant for a minimum declination of the Moon. */
		MOON_MINIMUM_DECLINATION,
		/** ID constant for an event related to a mira-type variable star. Field details
		 * will contain magnitude range, jd of next maxima, and jd of next minima (local time). */
		VARIABLE_STAR_MIRA,
		/** ID constant for an event related to a eclipsing variable star. Field details
		 * will contain mag range, phase, and jd of next minima (local time). */
		VARIABLE_STAR_ECLIPSING,
		/** ID constant for a generic calendar event. */
		CALENDAR,
		/** ID constant for an event related to lunar craters. */
		CRATER,
		/** ID constant for a generic conjunction event. */
		CONJUNCTION,
		/** ID constant for a generic occultation event. */
		OCCULTATION,
		/** ID constant for a generic transit event. */
		TRANSIT,
		/** ID constant for a generic eclipse event (natural satellites). */
		ECLIPSE,
		/** ID constant for the shadow of a natural satellite transiting on the planet. */
		SHADOW_TRANSIT,
		/** ID constant for a generic planet oposition event. */
		PLANET_OPPOSITION,
		/** ID constant for a generic planet conjunction event. */
		PLANET_CONJUNCTION,
		/** ID constant for the edge-on rings of Saturn. */
		SATURN_RINGS_EDGE_ON,
		/** ID constant for the maximum aperture of the rings of Saturn. */
		SATURN_RINGS_MAXIMUM_APERTURE,
		/** ID constant to compute transits of artificial satellites above the observer. */
		ARTIFICIAL_SATELLITES_TRANSITS,
		/** ID constant to compute transits of artificial satellites on the Sun/Moon disk. */
		ARTIFICIAL_SATELLITES_TRANSITS_SUN_MOON,
		/** ID constant to compute Iridium flares of artificial satellites. */
		ARTIFICIAL_SATELLITES_IRIDIUM_FLARES,
		/** ID constant for other kind of events. */
		OTHER,
		/** ID constant for an inexistent event, only used internally. */
		NO_EVENT,
		/** ID constant to compute dichotomy of Mercury/Venus or the 
		 * quadratures for external planets. */
		PLANET_DICHOTOMY_OR_QUADRATURE;
	};

	/**
	 * The events as strings.
	 */
	public static final String EVENTS[] = new String[] {
		"Full Moon", "New Moon", "Moon's first quarter", "Moon's last quarter",
		"Lunar eclipse", "Solar eclipse", "Spring equinox", "Autumn equinox",
		"Summer solstice", "Winter solstice", "Minimum distance", "Minimum distance from Sun",
		"Minimum elongation", "Maximum distance", "Maximum distance from Sun", "Maximum elongation",
		"GRS transit", "Moon's perigee", "Moon's apogee", "Mercury transit", "Venus transit",
		"Meteor shower", "Moon's ascending node", "Moon's descending node",
		"Moon's maximum declination", "Moon's minimum declination", "Mira variable star", "Eclipsing variable star",
		"Calendar", "Crater",
		"Conjunction", "Occultation", "Transit", "Eclipse", "Shadow transit", "Opposition", "Conjunction",
		"Saturn rings edge-on", "Saturn rings in maximum aperture",
		"Transit", "Transit", "Flare of Iridium",
		"Other", "Inexistent", 
		"Dichotomy/Quadrature"
	};

	/**
	 * Empty constructor.
	 */
	public SimpleEventElement() {}

	/**
	 * Constructor for a simple event.
	 * @param jd Event time.
	 * @param event Event id constant.
	 * @param details Details.
	 */
	public SimpleEventElement(double jd, EVENT event, String details)
	{
		this.time = jd;
		this.eventType = event;
		this.details = details;
	}

	/**
	 * Transforms the event time into another time scale.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param timeScale Ouput time scale id constant.
	 * @return The output time.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getEventTime(ObserverElement obs, EphemerisElement eph, SCALE timeScale)
	throws JPARSECException {
		TimeElement time = new TimeElement(this.time, SCALE.BARYCENTRIC_DYNAMICAL_TIME);

		double out = TimeScale.getJD(time, obs, eph, timeScale);
		return out;
	}

	/**
	 * Transforms the end time of the event into another time scale.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param timeScale Ouput time scale id constant.
	 * @return The output time.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getEventEndTime(ObserverElement obs, EphemerisElement eph, SCALE timeScale)
	throws JPARSECException {
		TimeElement time = new TimeElement(this.endTime, SCALE.BARYCENTRIC_DYNAMICAL_TIME);

		double out = TimeScale.getJD(time, obs, eph, timeScale);
		return out;
	}

	/**
	 * Returns the elevation of the event body above horizon for a given observer. Elevation
	 * is calculated respect the time of the maximum of the event (mean time between start and
	 * end) or respect to the start time in case end time is not known or not applicable.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @return The elevation in radians, or -1 in case cannot be calculated.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getElevation(ObserverElement obs, EphemerisElement eph) throws JPARSECException {
		String obj = null;
		try {
			TARGET t = Target.getID(body);
			if (t != TARGET.Moon && !t.isPlanet()) {
				t = Target.getID(secondaryBody);
				if (t.isPlanet()) {
					obj = secondaryBody;
				} else {
					if (t.isNaturalSatellite()) obj = t.getCentralBody().getName();
				}
			} else {
				obj = body;
			}
		} catch (Exception exc) {}

		if (obj == null && body != null && eventLocation == null) {
			try {
				LocationElement loc = new LocationElement(body, eph.ephemType == COORDINATES_TYPE.APPARENT);
				eventLocation = loc;
			} catch (Exception exc) {}
		}
		if (obj == null && eventLocation == null) return -1;
		double et = this.time;
		if (endTime > this.time) et = (et + this.endTime) * 0.5;
		TimeElement time = new TimeElement(et, SCALE.TERRESTRIAL_TIME);
		if (eventLocation != null) {
			LocationElement loc = CoordinateSystem.equatorialToHorizontal(eventLocation, time, obs, eph);
			return loc.getLatitude();
		}

		EphemerisElement ephCopy = eph.clone();
		ephCopy.targetBody = Target.getID(obj);
		if (ephCopy.targetBody == TARGET.EARTH) return -1;
		ephCopy.algorithm = ALGORITHM.MOSHIER;
		if (ephCopy.targetBody.isNaturalSatellite()) ephCopy.algorithm = ALGORITHM.NATURAL_SATELLITE;
		EphemElement ephem = Ephem.getEphemeris(time, obs, ephCopy, false, false);
		return ephem.elevation;
	}

	/**
	 * Clones this instance.
	 */
	@Override
	public SimpleEventElement clone()
	{
		SimpleEventElement e = new SimpleEventElement(this.time, this.eventType, this.details);
		e.body = this.body;
		e.secondaryBody = this.secondaryBody;
		e.eventLocation = null;
		if (this.eventLocation != null) e.eventLocation = this.eventLocation.clone();
		e.endTime = this.endTime;
		return e;
	}
	/**
	 * Returns whether the input Object contains the same information
	 * as this instance.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof SimpleEventElement)) return false;

		SimpleEventElement that = (SimpleEventElement) o;

		if (Double.compare(that.time, time) != 0) return false;
		if (Double.compare(that.endTime, endTime) != 0) return false;
		if (eventType != that.eventType) return false;
		if (details != null ? !details.equals(that.details) : that.details != null) return false;
		if (eventLocation != null ? !eventLocation.equals(that.eventLocation) : that.eventLocation != null)
			return false;
		if (body != null ? !body.equals(that.body) : that.body != null) return false;

		return !(secondaryBody != null ? !secondaryBody.equals(that.secondaryBody) : that.secondaryBody != null);
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		temp = Double.doubleToLongBits(time);
		result = (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(endTime);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (eventType != null ? eventType.hashCode() : 0);
		result = 31 * result + (details != null ? details.hashCode() : 0);
		result = 31 * result + (eventLocation != null ? eventLocation.hashCode() : 0);
		result = 31 * result + (body != null ? body.hashCode() : 0);
		result = 31 * result + (secondaryBody != null ? secondaryBody.hashCode() : 0);
		return result;
	}

	/**
	 * Returns a string representation of this event.
	 */
	@Override
	public String toString() {
		EphemerisElement eph = new EphemerisElement();
		eph.correctForEOP = false;
		eph.correctForPolarMotion = false;
		return this.toString(new ObserverElement(), eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
	}

	private String fix(String s, boolean bb) {
		if (s == null) return s;
		if (bb) {
			int a = s.indexOf("(");

			if (a >= 0) {
				s = DataSet.replaceAll(s, "(", " ", true);
				s = DataSet.replaceAll(s, ")", " ", true);
				s = s.trim();
			}
		}
		int b = s.indexOf("  ");
		if (b >= 0) {
			s = s.trim();
			b = s.indexOf("  ");
			while (b >= 0) {
				s = DataSet.replaceAll(s, "  ", " ", true);
				b = s.indexOf("  ");
			}
		}
		return s;
	}

	/**
	 * Returns a string representation of this event.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param timeScale The time scale of the output times.
	 * @return The event formatted.
	 */
	public String toString(ObserverElement obs, EphemerisElement eph, SCALE timeScale) {
		String body = fix(this.body, true);
		String secondaryBody = fix(this.secondaryBody, true);
		boolean secondaryIsNaturalSatellite = false;
		TARGET mainBody = null, secBody = null;
		try {
			secBody = Target.getIDFromEnglishName(secondaryBody);
			if (secBody == TARGET.NOT_A_PLANET)
				secBody = Target.getID(secondaryBody);
			secondaryIsNaturalSatellite = secBody.isNaturalSatellite();
		} catch (Exception exc) { }
		try {
			mainBody = Target.getIDFromEnglishName(body);
			if (mainBody == TARGET.NOT_A_PLANET)
				mainBody = Target.getID(body);
		} catch (Exception exc) { }
		String out = "";
		if (eventType.ordinal() < 21) {
			out = Translate.translate(829 + eventType.ordinal());
		} else {
			out = Translate.translate(EVENTS[eventType.ordinal()]);
			if (eventType == EVENT.PLANET_DICHOTOMY_OR_QUADRATURE)
				out = Translate.translate(1335);
		}
		TimeElement timeE = null;
		String tbody = null;
		if (mainBody != null && mainBody != TARGET.NOT_A_PLANET) {
			tbody = mainBody.getName();
		} else {
			tbody = Translate.translate(body);
		}
		String tof = Translate.translate(160); // of
		String tsecondaryBody = null;
		if (secondaryBody != null) {
			if (secBody != null && secBody != TARGET.NOT_A_PLANET) {
				tsecondaryBody = secBody.getName();
			} else {
				tsecondaryBody = Translate.translate(secondaryBody);
			}
		}
		String tevent = out;
		try {
			if (secondaryBody == null && ((eventType.compareTo(EVENT.PLANET_MINIMUM_DISTANCE) >= 0 &&
				eventType.compareTo(EVENT.PLANET_MAXIMUM_ELONGATION) <= 0) || eventType == EVENT.PLANET_CONJUNCTION ||
				eventType == EVENT.PLANET_OPPOSITION)) {
				if (eventType == EVENT.PLANET_CONJUNCTION || eventType == EVENT.PLANET_OPPOSITION) {
					out = tevent + " " + tof + " " + tbody;
				} else {
					out = tevent + " " + tbody;
				}
			} else {
				if (secondaryBody != null) {
					if (eventType == EVENT.OCCULTATION || eventType == EVENT.ECLIPSE) {
						out = tevent + " "+ tof + " " + tbody+" " + Translate.translate(161) + " " + tsecondaryBody;
					} else {
						if (eventType == EVENT.ARTIFICIAL_SATELLITES_TRANSITS_SUN_MOON 
								|| eventType == EVENT.TRANSIT || eventType == EVENT.SHADOW_TRANSIT) tevent += " " + tof;
						out = tevent + " "+ tbody+" " + Translate.translate(1020) + " " + tsecondaryBody;
					}
				} else {
					if (eventType == EVENT.ARTIFICIAL_SATELLITES_TRANSITS) tevent += " " + tof;
					if (eventType == EVENT.ARTIFICIAL_SATELLITES_TRANSITS || 
							eventType == EVENT.ARTIFICIAL_SATELLITES_IRIDIUM_FLARES) {
						int iri = tbody.toLowerCase().indexOf("iridium");
						if (iri >= 0) {
							out = tevent + " " + tbody.substring(iri+7).trim();							
						} else {
							out = tevent + " " + tbody;
						}
					}
				}
			}
			timeE = new TimeElement(this.getEventTime(obs, eph, timeScale), timeScale);
			if (Math.abs(endTime-time) < 3.0 / 1440.0) {
				out += ": "+timeE.toString();
			} else {
				out += ": "+timeE.toMinString();
			}
			if (this.endTime > -1E100) {
				TimeElement timeEE = new TimeElement(this.getEventEndTime(obs, eph, timeScale), timeScale);
				if (Math.abs(endTime-time) < 3.0 / 1440.0) {
					out += " -> "+timeEE.toString();
				} else {
					out += " -> "+timeEE.toMinString();
				}
			}
		} catch (Exception e) { }
		boolean translateIt = false;

		if (details != null && !details.equals("")) {
			if (((eventType.compareTo(EVENT.PLANET_MINIMUM_DISTANCE) >= 0 &&
					eventType.compareTo(EVENT.PLANET_MAXIMUM_ELONGATION) <= 0) || eventType == EVENT.PLANET_CONJUNCTION ||
					eventType == EVENT.PLANET_OPPOSITION || eventType == EVENT.MOON_MAXIMUM_DECLINATION || eventType == EVENT.MOON_MINIMUM_DECLINATION)) {
				String details = fix(this.details, false);
				String d = details;
				if (d.indexOf(",") > 0) {
					d = d.substring(0, d.indexOf(",")).trim();
				} else {
					try {
						double value = Double.parseDouble(d);
						int ndec = 3;
						d = Functions.formatValue(value, ndec);
						double val = Double.parseDouble(d);

						if (val == 0.0) {
							do {
								ndec ++;
								d = Functions.formatValue(value, ndec);
								val = Double.parseDouble(d);
							} while (val == 0.0 && ndec < 10);
						}
					} catch (Exception exc) {
						d = "-";
					}
				}
				if (eventType == EVENT.MOON_MAXIMUM_DECLINATION || eventType == EVENT.MOON_MINIMUM_DECLINATION ||
						eventType == EVENT.PLANET_MAXIMUM_ELONGATION || eventType == EVENT.PLANET_MINIMUM_ELONGATION
						|| eventType == EVENT.PLANET_CONJUNCTION || eventType == EVENT.PLANET_OPPOSITION) {
					d += "\u00b0";
					if (eventType == EVENT.PLANET_CONJUNCTION || eventType == EVENT.PLANET_OPPOSITION)
						d = Translate.translate(301).toLowerCase() + " " + d;
					if (eventType == EVENT.PLANET_MAXIMUM_ELONGATION && (mainBody == TARGET.MERCURY ||
									mainBody == TARGET.VENUS)) {
						int c = details.indexOf(",");
						if (c > 0) d += details.substring(1+c);
					}
				} else {
					if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) {
						d += " UA";
					} else {
						d += " AU";
					}
				}
				if (d.length() > 2) out += " ("+d+")";
				translateIt = true;
			} else {
				if (eventType == EVENT.MOON_APOGEE || eventType == EVENT.MOON_PERIGEE) {
					double value = Double.parseDouble(details);
					String p = Functions.formatAngle(value, 3);
					double dist = Constant.EARTH_RADIUS / Math.sin(value);
					String d = Functions.formatValue(dist, 0);
					out += " ("+Translate.translate(850).toLowerCase()+" = "+p+", "+Translate.translate(299).toLowerCase()+" = "+d+" km)";
				} else {
					String details = fix(this.details, false);
					if (this.eventType == EVENT.SATURN_RINGS_MAXIMUM_APERTURE) {
						out += " ("+Functions.formatValue(Double.parseDouble(details), 2)+"\u00b0)";
					} else {
						if (eventType == EVENT.ARTIFICIAL_SATELLITES_TRANSITS_SUN_MOON || eventType == EVENT.ARTIFICIAL_SATELLITES_TRANSITS) {
							out += " ("+Translate.translate(29).toLowerCase()+" "+tof+" " +details+")";
						} else {
							out += " ("+details+")";
						}
					}

					if (this.eventType == EVENT.CALENDAR || eventType == EVENT.CONJUNCTION || eventType == EVENT.CRATER
							|| eventType == EVENT.METEOR_SHOWER || (eventType == EVENT.OCCULTATION && !secondaryIsNaturalSatellite) ||
							eventType == EVENT.VARIABLE_STAR_ECLIPSING
							|| eventType == EVENT.VARIABLE_STAR_MIRA || eventType == EVENT.OTHER) {
						try {
							if (obs.getLatitudeDeg() < 0.0) {
								if (details.equals("DST1")) details = Translate.translate(1274);
								if (details.equals("DST2")) details = Translate.translate(1273);
							} else {
								if (details.equals("DST1")) details = Translate.translate(1273);
								if (details.equals("DST2")) details = Translate.translate(1274);
							}
						} catch (Exception exc) {
							if (details.equals("DST1")) details = Translate.translate(1273);
							if (details.equals("DST2")) details = Translate.translate(1274);
						}
						if (this.eventType == EVENT.CALENDAR && Translate.getDefaultLanguage() != LANGUAGE.ENGLISH) {
							for (int k=732; k<=746; k++) {
								details = DataSet.replaceAll(details, Translate.getEntry(k, LANGUAGE.ENGLISH), Translate.translate(k), true);
							}
							for (int k=1299; k<=1300; k++) {
								details = DataSet.replaceAll(details, Translate.getEntry(k, LANGUAGE.ENGLISH), Translate.translate(k), true);
							}
						}
						if (Math.abs(endTime-time) < 3.0 / 1440.0) {
							out = details + ": " + timeE.toString();
						} else {
							out = details + ": " + timeE.toMinString();
						}
						if (this.endTime > -1E100) {
							try {
								timeE = new TimeElement(this.getEventEndTime(obs, eph, timeScale), timeScale);
								if (Math.abs(endTime-time) < 3.0 / 1440.0) {
									out += " -> "+timeE.toString();
								} else {
									out += " -> "+timeE.toMinString();
								}
							} catch (Exception exc) {}
						}
					}
					translateIt = true;
				}
			}
		}

		// Try to translate every string found usually in events
		if (eventType == EVENT.ARTIFICIAL_SATELLITES_IRIDIUM_FLARES) {
			if (Translate.getDefaultLanguage() != Translate.LANGUAGE.ENGLISH) {
				out = DataSet.replaceAll(out, "start", Translate.translate(1319), true);
				out = DataSet.replaceAll(out, "end", Translate.translate(1320), true);
				out = DataSet.replaceAll(out, "max", Translate.translate(1321), true);
				out = DataSet.replaceAll(out, "azimuth", Translate.translate(28).toLowerCase(), true);
				out = DataSet.replaceAll(out, "elevation", Translate.translate(29).toLowerCase(), true);
				out = DataSet.replaceAll(out, "magnitude", Translate.translate(157).toLowerCase(), true);
			}
		} else {
			if (translateIt) {
				LANGUAGE lang = LANGUAGE.SPANISH;
				if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) lang = LANGUAGE.ENGLISH;
				int translate[] = new int[] {
						829, 830, 831, 832, 839, 840, 841, 842, 843, 844, 1085, 1086, 1087, 301, 73, 74, 963, 964, 161, 164, 167, 168, 
						169, 962, 965, 966, 1007, 1008, 
						1009, 1010, 1021, 1022, 1023, 1077, 1078, 1080, 163, 1323};
				if (eventType == EVENT.MOON_LUNAR_ECLIPSE || eventType == EVENT.MOON_SOLAR_ECLIPSE)
					translate = new int[] {963, 964, 161, 164, 167, 168, 169, 829, 830, 831, 832, 839, 840, 
						841, 842, 843, 844, 962, 966, 1007, 1008, 1009, 1010, 1021, 1022, 1023};
				int from = 1025, to = 1067;
				int translateLC[] = new int[] {1022, 1024};
				if (eventType != EVENT.OTHER && Translate.getDefaultLanguage() == Translate.LANGUAGE.SPANISH) {
					String translateS[] = new String[] {"ends", "new", "year"};
					int translateI[] = new int[] {1005, 535, 454};
					for (int i=0; i<translateS.length; i++) {
						try {
							out = DataSet.replaceAll(out, translateS[i], Translate.translate(translateI[i]).toLowerCase(), true);
							if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH && (out.startsWith("new") || out.startsWith("nuevo")) && eventType == EVENT.CALENDAR) {
								out = "nuevo a\u00f1o "+DataSet.replaceAll(FileIO.getRestAfterField(1, out, " ", true), "a\u00f1o", "", true).trim();
							}
						} catch (Exception exc) {}
					}
				}
				for (int i=0; i<translate.length; i++) {
					try {
						String in1 = Translate.getEntry(translate[i], lang);
						String in2 = Translate.translate(translate[i]);
						if (translate[i] == 1087) {
							in1 = in1.substring(0, in1.indexOf("(")).trim();
							in2 = in2.substring(0, in2.indexOf("(")).trim();
						}
						if (translate[i] == 301 || translate[i] == 163 || translate[i] == 841) {
							in1 = in1.toLowerCase();
							in2 = in2.toLowerCase();
						}
						out = DataSet.replaceAll(out, in1, in2, true);
					} catch (Exception exc) {}
				}
				for (int i=from; i<=to; i++) {
					try {
						out = DataSet.replaceAll(out, Translate.getEntry(i, lang), Translate.translate(i), true);
					} catch (Exception exc) {}
				}
				for (int i=0; i<translateLC.length; i++) {
					try {
						out = DataSet.replaceAll(out, Translate.getEntry(translateLC[i], lang).toLowerCase(), Translate.translate(translateLC[i]).toLowerCase(), true);
					} catch (Exception exc) {}
				}
				if (eventType != EVENT.OTHER) {
					for (int i=0; i<11; i++) {
						if (i == 3) continue;
						if (i == 4 && eventType == EVENT.CRATER) continue;
						if ((i == 0 || i == 4) && (eventType == EVENT.MOON_LUNAR_ECLIPSE || eventType == EVENT.MOON_SOLAR_ECLIPSE)) continue;
						try {
							String s = Translate.getEntry(i, lang);
							out = DataSet.replaceAll(out, s, Translate.translate(i), true);
						} catch (Exception exc) {}
					}
				}

				if (Translate.getDefaultLanguage() != Translate.LANGUAGE.ENGLISH) {
					out = DataSet.replaceAll(out, "Moon age", "edad Luna", true);
					out = DataSet.replaceAll(out, "Saturnoo", "Saturno", true);
					out = DataSet.replaceAll(out, "Asteroidee", "Asteroide", true);
					out = DataSet.replaceAll(out, "Cometaa", "Cometa", true);
					out = DataSet.replaceAll(out, "star", Translate.translate(79).toLowerCase(), true);
					out = DataSet.replaceAll(out, "Chinese", Translate.translate(1006), true);
					out = DataSet.replaceAll(out, " of ", " "+tof+" ", true);
				}
			}
		}

		if (eventType == EVENT.ARTIFICIAL_SATELLITES_TRANSITS)
			out = DataSet.replaceAll(out, "(E)", "("+Translate.translate(163).toLowerCase()+")", true);

		if (this.eventLocation != null) {
			out +=". " + Translate.translate(790)+" "+tof+" "+tbody+": " + Functions.formatRAOnlyMinutes(this.eventLocation.getLongitude(), 2)+", "+Functions.formatDECOnlyMinutes(this.eventLocation.getLatitude(), 1);
		}
		return out;
	}
}
