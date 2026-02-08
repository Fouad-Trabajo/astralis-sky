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

import java.awt.Color;
import java.util.ArrayList;

import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.EphemerisElement.COORDINATES_TYPE;
import jparsec.ephem.EphemerisElement.FRAME;
import jparsec.ephem.EphemerisElement.REDUCTION_METHOD;
import jparsec.ephem.Functions;
import jparsec.ephem.Target;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.moons.Dourneau;
import jparsec.ephem.moons.E;
import jparsec.ephem.moons.MoonEphem;
import jparsec.ephem.moons.MoonEphemElement;
import jparsec.ephem.planets.EphemElement;
import jparsec.graph.ChartElement;
import jparsec.graph.ChartSeriesElement;
import jparsec.graph.CreateChart;
import jparsec.graph.DataSet;
import jparsec.graph.JPARSECStroke;
import jparsec.io.FileIO;
import jparsec.math.Constant;
import jparsec.math.FastMath;
import jparsec.observer.City;
import jparsec.observer.CityElement;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.time.TimeScale;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;;

/**
 * A class to calculate events related to natural satellites.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class MoonEvent {

	private TimeElement time, timef;
	private ObserverElement observer;
	private EphemerisElement eph;
	private MoonEventElement events[];
	private int precision, accuracy;
	private double myjd, myjd0;
	private JUPITER_THEORY jup = JUPITER_THEORY.L1;
	private SATURN_THEORY sat = SATURN_THEORY.TASS;

	/**
	 * The set of event definitions.
	 */
	public enum EVENT_DEFINITION {
		/** The event starts/ends when the limb/edge of the satellite starts/ends touching the secondary body. Equivalent
		 * to calculating the umbra in mutual phenomena. */
		SATELLITE_LIMB,
		/** The event starts/ends when the center of the satellite starts/ends touching the secondary body's limb. This option
		 * will skip some partial eclipses/occultations in mutual phenomena. */
		SATELLITE_CENTER,
		/** The event starts/ends when the entire satellite starts/ends transiting the secondary body's limb. Equivalent to totality. */
		ENTIRE_SATELLITE,
		/** Automatic value: selects the criteria of satellite center for non-mutual events (although strictly the event starts
		 * before and ends later), and limb criteria for mutual events (neglecting penumbra). Default value.*/
		AUTOMATIC,
		/** Automatic value for drawing purposes: selects the criteria of satellite limb for transits (including penumbra, so it is
		 * not exactly equal to satellite limb mode) and entire satellite for occultations, and satellite limb for mutual events.
		 * Default value when rendering. */
		AUTOMATIC_FOR_DRAWING
	};

	/**
	 * Sets the minimum number of simultaneous events to set the corresponding flag
	 * {@linkplain MoonEventElement#severalSimultaneousEvents} as true. Default value is
	 * 2, but can be set to a greater value to search for more uncommon but interesting
	 * events.
	 */
	public static int SEVERAL_SIMULTANEOUS_EVENTS_MINIMUM_NUMBER = 2;

	/**
	 * Sets the minimum number of simultaneous events to set the corresponding flag
	 * {@linkplain MoonEventElement#severalSimultaneousEvents} as true, in case of
	 * mutual events between satellites. Default value is 2, but can be set to a greater
	 * value to search for more uncommon but interesting events.
	 */
	public static int SEVERAL_MUTUAL_SIMULTANEOUS_EVENTS_MINIMUM_NUMBER = 2;

	private static EVENT_DEFINITION eventDef = EVENT_DEFINITION.AUTOMATIC;

	/**
	 * Sets how to consider the start/end of an event. Default value is when the center of
	 * the main body starts/ends crossing the limb or edge of the secondary body in non-mutual
	 * events, and when the satellite's limb starts/ends the transit on the secondary body in
	 * mutual events.
	 * @param ed An event definition.
	 */
	public static void setEventDefinition(EVENT_DEFINITION ed) {
		eventDef = ed;
		//DataBase.addData("eventDefinition", null, ed.ordinal(), true);
	}
	/**
	 * Returns how to consider the start/end of an event.
	 * @return The event definition.
	 */
	public static EVENT_DEFINITION getEventDefinition() {
		return eventDef;
		/*
		Object o = DataBase.getData("eventDefinition", null, true);
		if (o == null) return EVENT_DEFINITION.AUTOMATIC;

		int ord = (Integer) o;
		return EVENT_DEFINITION.values()[ord];
		*/
	}

	/**
	 * The set of theories to obtain events for Jupiter satellites.
	 */
	public enum JUPITER_THEORY {
		/** Lieske's 2x3 theory. */
		E2x3,
		/** Lieske's E5 theory, an improved version of E2x3. */
		E5,
		/** IMCCE's L1 Theory. This is probably the better possible theory, but
		 * it is also much slower during calculations. */
		L1
	};

	/**
	 * The set of theories to obtain events for Saturn satellites.
	 */
	public enum SATURN_THEORY {
		/** Dourneau's theory. */
		Dourneau,
		/** IMCCE's TASS Theory. */
		TASS
	};

	/**
	 * Base constructor to obtain moon events. L1 and TASS are the default theories for
	 * Jupiter and Saturn satellites.
	 * @param time Time object for the initial calculation time.
	 * @param observer Observer object.
	 * @param eph The ephemeris properties. The target should be the mother planet, for example
	 * Jupiter for galilean satellites. It is strongly recommended to set the
	 * ephemerides to geocentric to save time.
	 * @param timef Time oject for the final calculation time.
	 * @param precision Precision in seconds for the search. A good value for mutual
	 * phenomena is 30s (although in case you define the event as starting/ending with the center or
	 * the entire satellite disk, this value should be reduced to around 10s), and 100s for non-mutual phenomena. Note that
	 * the accuracy of the returned events is independent of this parameter. A
	 * value of 30s only means that some events of shorter duration could be missed.
	 * @param accuracy This will be the accuracy in seconds of the returned event times.
	 * A good value is 1-10s for mutual events and 30s for non-mutual, but can be lower
	 * for Mars.
	 * @param optimize True to optimize calculations for speed. This consists on forcing astrometric J2000
	 * geocentric coordinates, reduced using IAU1976 algorithms, which are less CPU intensive. All posible
	 * corrections allowed in the ephemeris object are also disabled. Calculations can become 2.5 times faster,
	 * but slightly less accurate.
	 * @throws JPARSECException If an error occurs.
	 */
	public MoonEvent(TimeElement time, ObserverElement observer,
			EphemerisElement eph, TimeElement timef, int precision, int accuracy, boolean optimize)
	throws JPARSECException {
		// Pass to TDB to improve performance
		double jd0 = TimeScale.getJD(time, observer, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		double jdf = TimeScale.getJD(timef, observer, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		TimeElement newTime = new TimeElement(jd0, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		TimeElement newTimef = new TimeElement(jdf, SCALE.BARYCENTRIC_DYNAMICAL_TIME);

		this.time = newTime;
		this.observer  = observer.clone();
		this.eph = eph.clone();
		this.timef = newTimef;
		this.precision = precision;
		this.accuracy = accuracy;

		if (optimize) {
			this.eph.ephemType = COORDINATES_TYPE.ASTROMETRIC;
			this.eph.isTopocentric = false;
			this.eph.equinox = Constant.J2000;
			this.eph.frame = FRAME.DYNAMICAL_EQUINOX_J2000;
			this.eph.ephemMethod = REDUCTION_METHOD.IAU_1976;
			this.eph.preferPrecisionInEphemerides = false;
			this.eph.correctEOPForDiurnalSubdiurnalTides = false;
			this.eph.correctForPolarMotion = false;
			this.eph.correctEquatorialCoordinatesForRefraction = false;
			this.eph.correctForEOP = false;
			this.eph.correctForExtinction = false;
		}
	}

	/**
	 * Base constructor to obtain moon events, using the selected theories for
	 * Jupiter and Saturn satellites.
	 * @param time Time object for the initial calculation time.
	 * @param observer Observer object.
	 * @param eph The ephemeris properties. The target should be the mother planet, for example
	 * Jupiter for galilean satellites. It is strongly recommended to set the
	 * ephemerides to geocentric to save time.
	 * @param timef Time oject for the final calculation time.
	 * @param precision Precision in seconds for the search. A good value for mutual
	 * phenomena is 30s (although in case you define the event as starting/ending with the center or
	 * the entire satellite disk, this value should be reduced to around 10s), and 100s for non-mutual phenomena. Note that
	 * the accuracy of the returned events is independent of this parameter. A
	 * value of 30s only means that some events of shorter duration could be missed.
	 * @param accuracy This will be the accuracy in seconds of the returned event times.
	 * A good value is 1-10s for mutual events and 30s for non-mutual, but can be lower
	 * for Mars.
	 * @param optimize True to optimize calculations for speed. This consists on forcing astrometric J2000
	 * geocentric coordinates, reduced using IAU1976 algorithms, which are less CPU intensive. All posible
	 * corrections allowed in the ephemeris object are also disabled. Calculations can become 2.5 times faster,
	 * but slightly less accurate.
	 * @param jup Default theory to use for Jupiter satellites. L1 is very slow.
	 * @param sat Default theory to use for Saturn satellites.
	 * @throws JPARSECException If an error occurs.
	 */
	public MoonEvent(TimeElement time, ObserverElement observer,
			EphemerisElement eph, TimeElement timef, int precision, int accuracy, boolean optimize,
			JUPITER_THEORY jup, SATURN_THEORY sat)
	throws JPARSECException {
		this(time, observer, eph, timef, precision, accuracy, optimize);
		if (jup != null) this.jup = jup;
		if (sat != null) this.sat = sat;
	}

	private MoonEphemElement[] getEphem(ObserverElement observer, EphemerisElement eph)
	throws JPARSECException {
		TimeElement myTime = new TimeElement(myjd, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		if (observer.getMotherBody() != TARGET.EARTH) {
			return MoonEphem.calcAllSatellites(myTime, observer, eph, false);
		} else {
			return getSat(myTime, observer, eph);
		}
	}

	private MoonEphemElement[] getSat(TimeElement myTime, ObserverElement observer, EphemerisElement eph) throws JPARSECException {
		switch (eph.targetBody)
		{
		case MARS:
			return MoonEphem.martianSatellitesEphemerides_2007(myTime, observer, eph);
		case JUPITER:
			if (jup == JUPITER_THEORY.L1) return MoonEphem.galileanSatellitesEphemerides_L1(myTime, observer, eph);
			if (jup == JUPITER_THEORY.E5) return E.galileanSatellitesEphemerides_E5(myTime, observer, eph);
			if (jup == JUPITER_THEORY.E2x3) return E.galileanSatellitesEphemerides_E2x3(myTime, observer, eph);
		case SATURN:
			if (sat == SATURN_THEORY.TASS) return MoonEphem.saturnianSatellitesEphemerides_TASS17(myTime, observer, eph, false);
			if (sat == SATURN_THEORY.Dourneau) return Dourneau.saturnianSatellitesEphemerides_Dourneau(myTime, observer, eph);
		case URANUS:
			return MoonEphem.uranianSatellitesEphemerides_GUST86(myTime, observer, eph);
		case NEPTUNE:
			return new MoonEphemElement[] {MoonEphem.calcJPLSatellite(myTime, observer, eph, TARGET.Triton.getEnglishName())};
		case Pluto:
			return new MoonEphemElement[] {MoonEphem.calcJPLSatellite(myTime, observer, eph, TARGET.Charon.getEnglishName())};
		default:
			throw new JPARSECException("unsupported body for mutual phenomena. Use Mars, Jupiter, Saturn, Uranus, Neptune, or Pluto.");
		}
	}

	/**
	 * Obtains all mutual phenomena. Supported objects are Mars (2007 numerical integration theory),
	 * Jupiter (L1 theory), Saturn (TASS 1.7), and Uranus (GUST86).
	 *
	 * @param all True to return all events including occultations/eclipses by the mother planet itself. Note
	 * that in the latter case only events in a partial phase will be returned, i.e., the interval while the
	 * eclipse/occultation is in progress.
	 * @return Events visible for the observer. Those events that started before the initial time and persists at
	 * the initial time will have invalid initial times (jd = 0).
	 * @throws JPARSECException If an error occurs.
	 */
	public MoonEventElement[] getMutualPhenomena(boolean all)
	throws JPARSECException {
		boolean approx = true;
		ArrayList<MoonEventElement> vector = new ArrayList<MoonEventElement>();
		double jd = TimeScale.getJD(time, observer, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		double step = (double) precision / Constant.SECONDS_PER_DAY;
		double step2 = (double) accuracy / Constant.SECONDS_PER_DAY;
		double jdf = TimeScale.getJD(timef, observer, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		String target = eph.targetBody.getName();
		myjd0 = jd;
		double lastStep = 0;
		for (myjd = jd; myjd < jdf; myjd = myjd + step)
		{
			lastStep += step;
			MoonEphemElement ephem[] = getEphem(observer, eph);
			if (!all && ephem.length == 1) return new MoonEventElement[] {};
			boolean eventExist = false;
			for (int i = 0; i<ephem.length; i++)
			{
				if (!ephem[i].mutualPhenomena.equals("") && (all || ephem[i].mutualPhenomena.indexOf(target) < 0)) { // && (!eph.isTopocentric || eph.isTopocentric && ephem[i].elevation > 0)) {
					double oldJD = myjd;
					MoonEventElement ev[] = getMutualEventDetails(observer, eph, lastStep, step2, approx, all);
					// Since myjd maybe modified by getMutualEventDetails, we need to update the ephem array
					if (myjd <= oldJD) myjd = oldJD;
					myjd += step;
					ephem = getEphem(observer, eph);

					if (ev != null) {
						for (int j=0; j<ev.length; j++)
						{
							//ev[j].details = FileIO.getField(1, ev[j].details, ",", false)+"%";
							if (all || (!all && ev[j].secondaryBody != eph.targetBody)) vector.add(ev[j]);
							if ((ev[j].endTime+step) > myjd) myjd = ev[j].endTime+step;
						}
						eventExist = true;
					};
				}
			}

			double minDist = -1.0;
			lastStep = 0;
			if (!eventExist)
			{
				for (int i = 0; i<ephem.length; i++)
				{
					for (int j = i+1; j<ephem.length; j++)
					{
						LocationElement loci = LocationElement.parseRectangularCoordinates(ephem[i].xPosition,
								ephem[i].yPosition, 0.0);
						LocationElement locj = LocationElement.parseRectangularCoordinates(ephem[j].xPosition,
								ephem[j].yPosition, 0.0);
						double r = LocationElement.getLinearDistance(loci, locj);
						if (r < minDist || minDist < 0.0) minDist = r;

						loci = LocationElement.parseRectangularCoordinates(ephem[i].xPositionFromSun,
								ephem[i].yPositionFromSun, 0.0);
						locj = LocationElement.parseRectangularCoordinates(ephem[j].xPositionFromSun,
								ephem[j].yPositionFromSun, 0.0);
						r = LocationElement.getLinearDistance(loci, locj);
						if (r < minDist || minDist < 0.0) minDist = r;
					}
				}
				if (minDist > 0) lastStep = minDist * step * Constant.SECONDS_PER_DAY / 50000.0;
				myjd += lastStep;
			}
		}

		events = new MoonEventElement[vector.size()];
		for (int i=0; i<events.length;i++)
		{
			events[i] = vector.get(i);
		}
		return events;
	}

	private MoonEventElement[] getMutualEventDetails(ObserverElement observer, EphemerisElement eph,
			double mystep, double step, boolean approx, boolean all)
	throws JPARSECException {
		boolean started = false;
		boolean eventFound = false;
		MoonEventElement ev[] = null;
		myjd = myjd - mystep;
		double jd = myjd;
		double endTime = 1.0;
		String by = Translate.translate(161)+" ";
		String target = eph.targetBody.getName();
		for (myjd = jd; myjd < jd + endTime; myjd = myjd + step)
		{
			TimeElement myTime = new TimeElement(myjd, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
			MoonEphemElement ephem[] = null;
			if (observer.getMotherBody() != TARGET.EARTH) {
				ephem = MoonEphem.calcAllSatellites(myTime, observer, eph, false);
			} else {
				ephem = getSat(myTime, observer, eph);
			}
			if (!started) {
				ev = new MoonEventElement[ephem.length];
				started = true;
			}

			boolean eventPersists = false;
			for (int i = 0; i<ephem.length; i++)
			{
				if (!ephem[i].mutualPhenomena.equals("") && (all || ephem[i].mutualPhenomena.indexOf(target) < 0)) {
					eventPersists = true;
					if (ev[i] == null) {
						eventFound = true;
						MoonEventElement.EVENT phenom = MoonEventElement.EVENT.OCCULTED;
						if (ephem[i].mutualPhenomena.toLowerCase().indexOf("eclips") >= 0) phenom = MoonEventElement.EVENT.ECLIPSED;
						String second = ephem[i].mutualPhenomena.substring(ephem[i].mutualPhenomena.toLowerCase().indexOf(by)+by.length()).trim();
						String per = second.substring(second.indexOf("(") + 1, second.indexOf("%"));
						second = second.substring(0, second.indexOf("(")).trim();
						ev[i] = new MoonEventElement(myjd - step * 0.5, -1.0, Target.getID(ephem[i].name), Target.getID(second), phenom, per+", "+myjd);
						if (!eph.isTopocentric) {
							EphemElement ephemObj = EphemElement.parseMoonEphemElement(ephem[i], eph.getEpoch(myjd));
							ephemObj = Ephem.horizontalCoordinates(myTime, observer, eph, ephemObj);
							ephem[i].elevation = ephemObj.elevation;
						}
						ev[i].elevation = ephem[i].elevation;
						if (ephem[i].eclipsed && phenom == MoonEventElement.EVENT.OCCULTED) ev[i].visibleFromEarth = false;
						if (ephem[i].occulted && phenom == MoonEventElement.EVENT.ECLIPSED) ev[i].visibleFromEarth = false;
					} else {
						String second = ephem[i].mutualPhenomena.substring(ephem[i].mutualPhenomena.toLowerCase().indexOf(by)+by.length()).trim();
						String per = second.substring(second.indexOf("(") + 1, second.indexOf("%"));
						if (second.indexOf("\u00b0") >= 0) {
							String dist = second.substring(second.lastIndexOf("(") + 3, second.lastIndexOf("\u00b0"));
							if (FileIO.getField(3, ev[i].details, ",", true).equals("") || Double.parseDouble(dist) < Double.parseDouble(FileIO.getField(3, ev[i].details, ",", true)))
								ev[i].details = per+", "+myjd+", "+dist;
						} else {
							if (FileIO.getField(2, ev[i].details, ",", true).equals("") || Double.parseDouble(per) > Double.parseDouble(FileIO.getField(1, ev[i].details, ",", true)))
								ev[i].details = per+", "+myjd;
						}
					}
					if (myjd <= myjd0) ev[i].startTime = 0.0;
				} else {
					if (ev[i] != null) {
						if (ev[i].endTime == -1.0) {
							ev[i].endTime = myjd - step * 0.5;
							if (ephem[i].occulted && ev[i].eventType == MoonEventElement.EVENT.OCCULTED) ev[i].subType = MoonEventElement.SUBEVENT.START;
							if (!ephem[i].occulted && ev[i].eventType == MoonEventElement.EVENT.OCCULTED) ev[i].subType = MoonEventElement.SUBEVENT.END;
							if (ephem[i].eclipsed && ev[i].eventType == MoonEventElement.EVENT.ECLIPSED) ev[i].subType = MoonEventElement.SUBEVENT.START;
							if (!ephem[i].eclipsed && ev[i].eventType == MoonEventElement.EVENT.ECLIPSED) ev[i].subType = MoonEventElement.SUBEVENT.END;
						}
					}
				}
			}

			if (!eventPersists && eventFound) break;
		}

		int count = 0;
		for (int i=0; i<ev.length; i++)
		{
			if (ev[i] != null) count ++;
		}
		MoonEventElement out[] = new MoonEventElement[count];
		int index = -1;
		for (int i=0; i<ev.length; i++)
		{
			if (ev[i] != null) {
				index ++;
				out[index] = ev[i];
			}
		}
		if (out.length >= MoonEvent.SEVERAL_MUTUAL_SIMULTANEOUS_EVENTS_MINIMUM_NUMBER) {
			for (int i=0; i<out.length; i++) {
				out[i].severalSimultaneousEvents = true;
			}
		}
		return out;
	}

	/**
	 * Obtains the light curve of a given mutual event.
	 * @param event Event to calculate. Must have valid initial/ending times.
	 * @param outputTimeScale Time scale for the x axis. Constants define in {@linkplain TimeElement}.
	 * @param magnitude True to represent magnitude instead of non-eclipsed/oculted disk fraction.
	 * @param combined True to represent combined magnitudes of the two satellites. If the chart represents
	 * non-eclipsed/oculted disk fraction, this parameter will have no effect.
	 * @return The chart.
	 * @throws JPARSECException If an error occurs.
	 */
	public CreateChart getLightCurve(MoonEventElement event, SCALE outputTimeScale, boolean magnitude, boolean combined)
	throws JPARSECException {
		TARGET mother = event.mainBody.getCentralBody();
		CityElement city = City.findCity("Madrid");
		ObserverElement observer = ObserverElement.parseCity(city);
		EphemerisElement eph = new EphemerisElement(mother, EphemerisElement.COORDINATES_TYPE.APPARENT,
				EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.GEOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
				EphemerisElement.FRAME.ICRF);
		eph.algorithm = EphemerisElement.ALGORITHM.MOSHIER;

		ArrayList<double[]> vector = new ArrayList<double[]>();
		double duration = event.endTime - event.startTime;
		TimeElement time = new TimeElement(event.startTime - duration / 10.0, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		TimeElement timef = new TimeElement(event.endTime + duration / 10.0, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		double jd = TimeScale.getJD(time, observer, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		double jdf = TimeScale.getJD(timef, observer, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		double step = (jdf - jd) / 100.0;

		int index = -1, index2 = -1;
		for (double myjd = jd; myjd < jdf; myjd = myjd + step)
		{
			TimeElement myTime = new TimeElement(myjd, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
			MoonEphemElement ephem[] = null;
			if (observer.getMotherBody() != TARGET.EARTH) {
				ephem = MoonEphem.calcAllSatellites(myTime, observer, eph, false);
			} else {
				ephem = getSat(myTime, observer, eph);
			}

			boolean eventExist = false;
			if (index == -1 || index2 == -1) {
				for (int i = 0; i<ephem.length; i++)
				{
					if (ephem[i].name.equals(event.mainBody.getName())) index = i;
					if (ephem[i].name.equals(event.secondaryBody.getName())) index2 = i;
				}
			}
			for (int i = 0; i<ephem.length; i++)
			{
				if (!ephem[i].mutualPhenomena.equals("") && ephem[i].name.equals(event.mainBody.getName())) {
					String by = Translate.translate(161)+" ";
					String second = ephem[i].mutualPhenomena.substring(ephem[i].mutualPhenomena.toLowerCase().indexOf(by)+by.length()).trim();
					String per = second.substring(second.indexOf("(") + 1, second.indexOf("%"));
					double fraction = 1.0 - Double.parseDouble(per) / 100.0;
					if (magnitude) {
						if (combined) vector.add(new double[] {myjd, jparsec.astronomy.Star.combinedMagnitude(ephem[i].magnitude, ephem[index2].magnitude)});
						if (!combined) vector.add(new double[] {myjd, ephem[i].magnitude});
					} else {
						vector.add(new double[] {myjd, fraction});
					}
					eventExist = true;
				}
			}
			if (!eventExist) {
				if (magnitude) {
					if (combined) vector.add(new double[] {myjd, jparsec.astronomy.Star.combinedMagnitude(ephem[index].magnitude, ephem[index2].magnitude)});
					if (!combined) vector.add(new double[] {myjd, ephem[index].magnitude});
				} else {
					if (index >=0) {
						if (ephem[index].eclipsed || ephem[index].occulted)
						{
							vector.add(new double[] {myjd, 0.0});
						} else {
							vector.add(new double[] {myjd, 1.0});
						}
					}
					vector.add(new double[] {myjd, 1.0});
				}
			}
		}

		double x[] = new double[vector.size()];
		double y[] = new double[vector.size()];
		double dy[] = new double[vector.size()];
		for (int i=0; i<vector.size(); i++)
		{
			double set[] = vector.get(i);
			TimeElement myTime = new TimeElement(set[0], SCALE.BARYCENTRIC_DYNAMICAL_TIME);
			double outjd = TimeScale.getJD(myTime, observer, eph, outputTimeScale);
			x[i] = outjd;
			y[i] = set[1];
			dy[i] = 0.005;
		}
		ChartSeriesElement chartSeries = new ChartSeriesElement(x, y, null, dy,
				Translate.translate(155), false, Color.BLACK, ChartSeriesElement.SHAPE_CIRCLE,
				ChartSeriesElement.REGRESSION.NONE);
		chartSeries.showLines = true;
		chartSeries.showErrorBars = false;
		ChartSeriesElement series[] = new ChartSeriesElement[] {chartSeries};
		ChartElement chart = new ChartElement(series, ChartElement.TYPE.XY_CHART, ChartElement.SUBTYPE.XY_TIME,
				event.mainBody.getName()+" "+Translate.translate(163 + event.eventType.ordinal())+" "+Translate.translate(161)+" "+event.secondaryBody.getName()+". "+Translate.translate(159),
				Translate.translate(180)+" ("+TimeElement.getTimeScaleAbbreviation(outputTimeScale)+")", Translate.translate(155)+" "+Translate.translate(160)+
				" "+event.mainBody.getName(), false, 400, 400);

		if (magnitude) chart.yLabel = Translate.translate(157)+" "+Translate.translate(160)+" "+event.mainBody.getName();
		if (magnitude && combined) chart.yLabel = Translate.translate(158)+" "+Translate.translate(160)+" "+event.mainBody.getName()+" and "+event.secondaryBody.getName();

		return new CreateChart(chart);
	}

	/**
	 * Obtains all non-mutual phenomena (eclipses, occultations, transits, shadow transits).
	 * Supported objects are Mars (2007 numerical integration theory),
	 * Jupiter (L1 theory), Saturn (TASS 1.7), and Uranus (GUST86).
	 * @return Events visible for the observer. Those events that started before the initial
	 * time and persists at the initial time will have invalid initial times (jd = 0).
	 * @throws JPARSECException If an error occurs.
	 */
	public MoonEventElement[] getPhenomena()
	throws JPARSECException {
		boolean approx = true;
		ArrayList<MoonEventElement> vector = new ArrayList<MoonEventElement>();
		double jd = TimeScale.getJD(time, observer, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		double step = (double) precision / Constant.SECONDS_PER_DAY;
		double step2 = (double) accuracy / Constant.SECONDS_PER_DAY;
		double jdf = TimeScale.getJD(timef, observer, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		myjd0 = jd;
		double lastStep = 0;
		for (myjd = jd; myjd < jdf; myjd = myjd + step)
		{
			lastStep += step;
			MoonEphemElement ephem[] = getEphem(observer, eph);
			boolean eventExist = false;
			for (int i = 0; i<ephem.length; i++)
			{
				if ((ephem[i].occulted || ephem[i].eclipsed || ephem[i].transiting || ephem[i].shadowTransiting)) { // && (!eph.isTopocentric || eph.isTopocentric && ephem[i].elevation > 0)) {
					eventExist = true;
					double oldJD = myjd;
					MoonEventElement ev[] = getEventDetails(observer, eph, lastStep, step2, approx);
					// Since myjd maybe modified by getEventDetails, we need to update the ephem array
					if (myjd <= oldJD) myjd = oldJD;
					myjd += step;
					ephem = getEphem(observer, eph);

					if (ev != null) {
						for (int j=0; j<ev.length; j++)
						{
							vector.add(ev[j]);
							if ((ev[j].endTime+step) > myjd) myjd = ev[j].endTime+step;
						}
					};
				}
			}

			double minDist = -1.0;
			lastStep = 0;
			if (!eventExist)
			{
				double step0 = 0.5 * 3600.0 / Constant.SECONDS_PER_DAY; // can be up to 0.85 hours instead of 0.5
				for (int i = 0; i<ephem.length; i++)
				{
					double f = 1.0 + (2.0 * i) / (ephem.length - 1.0);
					double r = Math.sqrt(ephem[i].xPosition*ephem[i].xPosition+ephem[i].yPosition*ephem[i].yPosition);
					if (r < 1) break;

					r = (r - 1.0) * f;
					if (r < minDist || minDist < 0.0) minDist = r;

					r = Math.sqrt(ephem[i].xPositionFromSun*ephem[i].xPositionFromSun+ephem[i].yPositionFromSun*ephem[i].yPositionFromSun);
					if (r < 1) break;

					r = (r - 1.0) * f;
					if (r < minDist || minDist < 0.0) minDist = r;
				}
				if (minDist > 0.0) lastStep = minDist * step0; //minDist * step * Constant.SECONDS_PER_DAY / 50000.0; // before
				myjd += lastStep;
			}
		}

		events = new MoonEventElement[vector.size()];
		for (int i=0; i<events.length;i++)
		{
			events[i] = vector.get(i);
		}
		return events;
	}

	private MoonEventElement[] getEventDetails(ObserverElement observer, EphemerisElement eph,
			double mystep, double step, boolean approx)
	throws JPARSECException {
		boolean started = false;
		boolean eventFound = false;
		MoonEventElement ev[][] = null;
		myjd = myjd - mystep;
		double jd = myjd;
		double endTime = 1.0;
		for (myjd = jd; myjd < jd + endTime; myjd = myjd + step)
		{
			TimeElement myTime = new TimeElement(myjd, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
			MoonEphemElement ephem[] = null;
			if (observer.getMotherBody() != TARGET.EARTH) {
				ephem = MoonEphem.calcAllSatellites(myTime, observer, eph, false);
			} else {
				ephem = getSat(myTime, observer, eph);
			}
			if (!started) {
				ev = new MoonEventElement[ephem.length][2];
				started = true;
			}

			boolean eventPersists = false;
			String eventList = "";
			for (int i = 0; i<ephem.length; i++)
			{
				String ph = "";
				if (ephem[i].occulted) ph += "O";
				if (ephem[i].eclipsed) ph += "E";
				if (ephem[i].transiting) ph += "T";
				if (ephem[i].shadowTransiting) ph += "S";
				eventList += ph + ",";

				if (ephem[i].occulted || ephem[i].eclipsed || ephem[i].transiting || ephem[i].shadowTransiting) {
					eventPersists = true;
					if (ev[i][0] == null && ephem[i].occulted) {
						eventFound = true;
						MoonEventElement.EVENT phenom = MoonEventElement.EVENT.OCCULTED;
						ev[i][0] = new MoonEventElement(myjd - step * 0.5, -1.0, Target.getID(ephem[i].name), eph.targetBody, phenom, "");
						if (!eph.isTopocentric) {
							EphemElement ephemObj = EphemElement.parseMoonEphemElement(ephem[i], eph.getEpoch(myjd));
							ephemObj = Ephem.horizontalCoordinates(myTime, observer, eph, ephemObj);
							ephem[i].elevation = ephemObj.elevation;
						}
						ev[i][0].elevation = ephem[i].elevation;
						if (ephem[i].eclipsed) ev[i][0].visibleFromEarth = false;
						if (myjd <= myjd0) ev[i][0].startTime = 0.0;
					}
					if (ev[i][1] == null && ephem[i].eclipsed) {
						eventFound = true;
						MoonEventElement.EVENT phenom = MoonEventElement.EVENT.ECLIPSED;
						ev[i][1] = new MoonEventElement(myjd - step * 0.5, -1.0, Target.getID(ephem[i].name), eph.targetBody, phenom, "");
						if (!eph.isTopocentric) {
							EphemElement ephemObj = EphemElement.parseMoonEphemElement(ephem[i], eph.getEpoch(myjd));
							ephemObj = Ephem.horizontalCoordinates(myTime, observer, eph, ephemObj);
							ephem[i].elevation = ephemObj.elevation;
						}
						ev[i][1].elevation = ephem[i].elevation;
						if (ephem[i].occulted) ev[i][0].visibleFromEarth = false;
						if (myjd <= myjd0) ev[i][1].startTime = 0.0;
					}

					if (ev[i][0] == null && ephem[i].transiting) {
						eventFound = true;
						MoonEventElement.EVENT phenom = MoonEventElement.EVENT.TRANSIT;
						ev[i][0] = new MoonEventElement(myjd - step * 0.5, -1.0, Target.getID(ephem[i].name), eph.targetBody, phenom, "");
						if (!eph.isTopocentric) {
							EphemElement ephemObj = EphemElement.parseMoonEphemElement(ephem[i], eph.getEpoch(myjd));
							ephemObj = Ephem.horizontalCoordinates(myTime, observer, eph, ephemObj);
							ephem[i].elevation = ephemObj.elevation;
						}
						ev[i][0].elevation = ephem[i].elevation;
						if (myjd <= myjd0) ev[i][0].startTime = 0.0;
					}
					if (ev[i][1] == null && ephem[i].shadowTransiting) {
						eventFound = true;
						MoonEventElement.EVENT phenom = MoonEventElement.EVENT.SHADOW_TRANSIT;
						ev[i][1] = new MoonEventElement(myjd - step * 0.5, -1.0, Target.getID(ephem[i].name), eph.targetBody, phenom, "");
						if (!eph.isTopocentric) {
							EphemElement ephemObj = EphemElement.parseMoonEphemElement(ephem[i], eph.getEpoch(myjd));
							ephemObj = Ephem.horizontalCoordinates(myTime, observer, eph, ephemObj);
							ephem[i].elevation = ephemObj.elevation;
						}
						ev[i][1].elevation = ephem[i].elevation;
						if (myjd <= myjd0) ev[i][1].startTime = 0.0;
					}

					if (ev[i][0] != null && !ephem[i].occulted && !ephem[i].transiting) {
						if (ev[i][0].endTime == -1.0) {
							ev[i][0].endTime = myjd - step * 0.5;
						}
					}
					if (ev[i][1] != null && !ephem[i].eclipsed && !ephem[i].shadowTransiting) {
						if (ev[i][1].endTime == -1.0) {
							ev[i][1].endTime = myjd - step * 0.5;
						}
					}
				}
				if (ev[i][0] != null && !ephem[i].occulted && !ephem[i].transiting) {
					if (ev[i][0].endTime == -1.0) {
						ev[i][0].endTime = myjd - step * 0.5;
					}
				}
				if (ev[i][1] != null && !ephem[i].eclipsed && !ephem[i].shadowTransiting) {
					if (ev[i][1].endTime == -1.0) {
						ev[i][1].endTime = myjd - step * 0.5;
					}
				}
			}

			if (!eventPersists && eventFound) break;

			// Accelerate calculations
			if (eventPersists) {
				double timeStep = 20 * step;
				double step2 = 20.0 / Constant.SECONDS_PER_DAY;
				double step3 = 300.0 / Constant.SECONDS_PER_DAY;
				if (timeStep < step2) timeStep = step2;
				if (timeStep > step3) timeStep = step3;
				do {
					myTime = new TimeElement(myjd + timeStep, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
					if (observer.getMotherBody() != TARGET.EARTH) {
						ephem = MoonEphem.calcAllSatellites(myTime, observer, eph, false);
					} else {
						ephem = getSat(myTime, observer, eph);
					}
					String newEventList = "";
					for (int i = 0; i<ephem.length; i++)
					{
						String ph = "";
						if (ephem[i].occulted) ph += "O";
						if (ephem[i].eclipsed) ph += "E";
						if (ephem[i].transiting) ph += "T";
						if (ephem[i].shadowTransiting) ph += "S";
						newEventList += ph + ",";
					}
					if (newEventList.equals(eventList)) {
						myjd += timeStep;
					} else {
						break;
					}
				} while (true);
			}
		}

		int count = 0;
		for (int i=0; i<ev.length; i++)
		{
			if (ev[i][0] != null) count ++;
			if (ev[i][1] != null) count ++;
		}
		MoonEventElement out[] = new MoonEventElement[count];
		int index = -1;
		for (int i=0; i<ev.length; i++)
		{
			if (ev[i][0] != null) {
				index ++;
				out[index] = ev[i][0];
			}
			if (ev[i][1] != null) {
				index ++;
				out[index] = ev[i][1];
			}
		}
		if (out.length > 1) {
			int nsat = 0;
			String sat = "";
			for (int i=0; i<out.length; i++) {
				String mb = out[i].mainBody.getName();
				if (sat.indexOf(mb) < 0) {
					sat += mb + " ";
					nsat ++;
				}
			}
			if (nsat >= MoonEvent.SEVERAL_SIMULTANEOUS_EVENTS_MINIMUM_NUMBER) {
				for (int i=0; i<out.length; i++) {
					out[i].severalSimultaneousEvents = true;
				}
			}
		}
		return out;
	}

	/**
	 * Creates a chart with the paths of the satellites.
	 * @param step The step in days between consecutive points. 1 hour is a good value, but the total
	 * computing time could be several minutes for one month of time span or more.
	 * @param arcseconds True to select arcseconds as output unit, false for planetary radii.
	 * @param projectToEquator True to project to planet equator (strongly recommended). If false the offsets
	 * will be the total radial distance, that will never be 0 unless the inclination of the planet
	 * equator respect to Earth is zero.
	 * @return The chart.
	 * @throws JPARSECException If an error occurs.
	 */
	public CreateChart getPathChart(double step, boolean arcseconds, boolean projectToEquator) throws JPARSECException {
		SCALE timeScale = SCALE.BARYCENTRIC_DYNAMICAL_TIME;
		double initJD = TimeScale.getJD(time, observer, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		double endJD = TimeScale.getJD(timef, observer, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);

		TARGET tbm[] = new TARGET[] {TARGET.Phobos, TARGET.Deimos};
		TARGET tbj[] = new TARGET[] {TARGET.Io, TARGET.Europa, TARGET.Ganymede, TARGET.Callisto};
		TARGET tbs[] = new TARGET[] {TARGET.Mimas, TARGET.Enceladus, TARGET.Tethys, TARGET.Dione, TARGET.Rhea, TARGET.Titan, TARGET.Hyperion, TARGET.Iapetus};
		TARGET tbu[] = new TARGET[] {TARGET.Miranda, TARGET.Ariel, TARGET.Umbriel, TARGET.Titania, TARGET.Oberon};
		TARGET targetBodies[] = tbm;
		if (eph.targetBody == TARGET.JUPITER) targetBodies = tbj;
		if (eph.targetBody == TARGET.SATURN) targetBodies = tbs;
		if (eph.targetBody == TARGET.URANUS) targetBodies = tbu;
		if (targetBodies == tbm && eph.targetBody != TARGET.MARS) throw new JPARSECException("central body ID "+eph.targetBody+" unsupported.");

		String fieldSep = "  ";
		String lineSep = FileIO.getLineSeparator();
		String dataTable[] = new String[targetBodies.length];
		ArrayList<Object[]> ephem = populateEphemCalculations(observer, eph, initJD, endJD, step, timeScale, targetBodies);

		for (int targetIndex = 0; targetIndex < targetBodies.length; targetIndex ++) {
			String data = "";
			int index = 0;
			for (double jd = initJD; jd < endJD + step; jd = jd + step) {
				Object ephemCalc[] = ephem.get(index);
				index ++;

				EphemElement ephemPlan = (EphemElement) ephemCalc[0];
				MoonEphemElement ephemSat = ((MoonEphemElement[]) ephemCalc[1])[targetIndex];
				LocationElement locSat = new LocationElement(ephemSat.rightAscension, ephemSat.declination, ephemSat.distance);

				double sep = LocationElement.getAngularDistance(locSat, ephemPlan.getEquatorialLocation()) / ephemPlan.angularRadius;
				if (arcseconds) sep *= ephemPlan.angularRadius * Constant.RAD_TO_ARCSEC;

				// Project separation towards planet's equator ?
				if (projectToEquator) {
					double angRespectNorth = LocationElement.getPositionAngle(locSat, ephemPlan.getEquatorialLocation());
					double angRespectEquator = Constant.PI_OVER_TWO + LocationElement.getPositionAngle(
							new LocationElement(ephemPlan.northPoleRA, ephemPlan.northPoleDEC, 1.0),
							new LocationElement(0.0, Constant.PI_OVER_TWO, 1.0));
					sep = -sep * Math.cos(angRespectEquator - angRespectNorth);
				} else {
					double angRespectEast = Constant.PI_OVER_TWO + LocationElement.getPositionAngle(locSat, ephemPlan.getEquatorialLocation());
					sep = sep * FastMath.sign(Math.cos(angRespectEast));
				}

				String status = "ok";
				if (ephemSat.eclipsed) status = "eclipsed";
				if (ephemSat.occulted) status = "occulted";
				String line = Functions.formatValue(sep, 3) + fieldSep + Functions.formatValue(jd - initJD, 5) + fieldSep + status + lineSep;
				data += line;
			}
			dataTable[targetIndex] = data;
		}

		// Create chart
		String motherBody = targetBodies[0].getCentralBody().getName();
		String title = Translate.translate(853) + " " + motherBody;
		ChartSeriesElement series[] = new ChartSeriesElement[targetBodies.length];
		Color[] col = new Color[] {Color.RED, Color.GREEN, Color.BLUE, Color.BLACK, Color.MAGENTA,
				Color.ORANGE};
		JPARSECStroke strokes[] = new JPARSECStroke[] {
				JPARSECStroke.STROKE_DEFAULT_LINE,
				JPARSECStroke.STROKE_LINES_LARGE,
				JPARSECStroke.STROKE_LINES_MEDIUM,
				JPARSECStroke.STROKE_LINES_SHORT,
				JPARSECStroke.STROKE_POINTS_LOW_SPACE,
				JPARSECStroke.STROKE_POINTS_MEDIUM_SPACE
		};
		for (int i = 0; i < series.length; i++) {
			String table[] = DataSet.toStringArray(dataTable[i], lineSep);
			double x[] = DataSet.toDoubleValues(DataSet.extractColumnFromTable(table, fieldSep, 1));
			double y[] = DataSet.toDoubleValues(DataSet.extractColumnFromTable(table, fieldSep, 0));
			for (int j=0; j<x.length; j++) {
				x[j] += initJD;
			}
			series[i] = new ChartSeriesElement(x, y, null, null,
					targetBodies[i].getName(), true, col[i % col.length], ChartSeriesElement.SHAPE_EMPTY,
					ChartSeriesElement.REGRESSION.NONE);
			series[i].showShapes = false;
			series[i].showLines = true;
			series[i].stroke = strokes[i % strokes.length];
		}
		ChartSeriesElement.setShapeSize(1);
		ChartElement chart = new ChartElement(series, ChartElement.TYPE.XY_CHART,
				ChartElement.SUBTYPE.XY_TIME, title, Translate.translate(851), Translate.translate(852)+" (\")", false, 900, 300);
		if (!arcseconds) chart.yLabel = Translate.translate(852)+ " (" + Translate.translate(854)+")";
		CreateChart ch = new CreateChart(chart);

		return ch;
	}

	// Used to calculate ephemerides in a more efficient way, can be reduced to
	// few lines using Ephem class, but would be slower
	private ArrayList<Object[]> populateEphemCalculations(ObserverElement observer, EphemerisElement eph,
			double initJD, double endJD, double step, SCALE timeScale, TARGET targetBodies[]) throws JPARSECException {
		ArrayList<Object[]> ephem = new ArrayList<Object[]>();

		TARGET mo = eph.targetBody;
		for (double jd = initJD; jd < endJD + step; jd = jd + step) {
			MoonEphemElement ephemJup[] = null, ephemSat[] = null, ephemUra[] = null, ephemMar[] = null;

			eph.algorithm = EphemerisElement.ALGORITHM.MOSHIER;
			eph.targetBody = mo;
			TimeElement time = new TimeElement(jd, timeScale);
			EphemElement ephemPlan = Ephem.getEphemeris(time, observer, eph, false, true);
			MoonEphemElement ephemS[] = new MoonEphemElement[targetBodies.length];
			for (int targetIndex = 0; targetIndex < targetBodies.length; targetIndex ++) {

				TARGET target = targetBodies[targetIndex];

				eph.targetBody = target;
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

				if (mainSat > 0) { // Satellite supported in one of the main theories
					if (observer.getMotherBody() != TARGET.EARTH) {
						eph.targetBody = TARGET.values()[TARGET.EARTH.ordinal()+mainSat];
						if (mainSat == 1) {
							if (ephemMar == null) ephemMar = MoonEphem.calcAllSatellites(time, observer, eph, false);
							ephemS[targetIndex] = ephemMar[mainSatN];
						}
						if (mainSat == 2) {
							if (ephemJup == null) ephemJup = MoonEphem.calcAllSatellites(time, observer, eph, false);
							ephemS[targetIndex] = ephemJup[mainSatN];
						}
						if (mainSat == 3) {
							if (ephemSat == null) ephemSat = MoonEphem.calcAllSatellites(time, observer, eph, false);
							ephemS[targetIndex] = ephemSat[mainSatN];
						}
						if (mainSat == 4) {
							if (ephemUra == null) ephemUra = MoonEphem.calcAllSatellites(time, observer, eph, false);
							ephemS[targetIndex] = ephemUra[mainSatN];
						}
					} else {
						if (mainSat == 1) {
							if (ephemMar == null) ephemMar = MoonEphem.martianSatellitesEphemerides_2007(time, observer, eph);
							ephemS[targetIndex] = ephemMar[mainSatN];
						}
						if (mainSat == 2) {
							if (ephemJup == null) {
								if (jup == JUPITER_THEORY.L1) ephemJup = MoonEphem.galileanSatellitesEphemerides_L1(time, observer, eph);
								if (jup == JUPITER_THEORY.E5) ephemJup = E.galileanSatellitesEphemerides_E5(time, observer, eph);
								if (jup == JUPITER_THEORY.E2x3) ephemJup = E.galileanSatellitesEphemerides_E2x3(time, observer, eph);
							}
							ephemS[targetIndex] = ephemJup[mainSatN];
						}
						if (mainSat == 3) {
							if (ephemSat == null) {
								if (sat == SATURN_THEORY.TASS) ephemSat = MoonEphem.saturnianSatellitesEphemerides_TASS17(time, observer, eph, false);
								if (sat == SATURN_THEORY.Dourneau) ephemSat = Dourneau.saturnianSatellitesEphemerides_Dourneau(time, observer, eph);
							}
							ephemS[targetIndex] = ephemSat[mainSatN];
						}
						if (mainSat == 4) {
							if (ephemUra == null) ephemUra = MoonEphem.uranianSatellitesEphemerides_GUST86(time, observer, eph);
							ephemS[targetIndex] = ephemUra[mainSatN];
						}
					}
				} else { // If not supported, use JPL elements
					ephemS[targetIndex] = MoonEphem.calcSatellite(time, observer, eph);
				}
			}
			ephem.add(new Object[] {ephemPlan, ephemS});
		}
		return ephem;
	}
}
