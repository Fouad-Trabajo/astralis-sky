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
import jparsec.observer.*;
import jparsec.ephem.*;
import jparsec.ephem.Target.TARGET;

import java.io.*;

/**
 * Manages data related to satellite events.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class MoonEventElement implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Holds the start time of the events as a Julian day in TDB.
	 */
	public double startTime;
	/**
	 * Holds the end time of the events as a Julian day in TDB.
	 */
	public double endTime;
	/**
	 * Main body id constant.
	 */
	public TARGET mainBody;
	/**
	 * Secondary body id constant, if anyone exists.
	 */
	public TARGET secondaryBody;
	/**
	 * Event id code. Constants defined in this class.
	 */
	public MoonEventElement.EVENT eventType;
	/**
	 * Holds details on the event. For mutual events it contains a number
	 * with the percentage of eclipse/occultation, the date of that maximum,
	 * and the distance between both bodies in degrees.
	 */
	public String details;
	/**
	 * Subevent id constant. Only takes sense on mutual events with the
	 * mother planet, while a satellite is partially eclipsed or occulted. In this case
	 * the event marks the beggining or the ending of the period when the
	 * satellite is fully eclipsed or occulted. Constants defined in this class.
	 */
	public MoonEventElement.SUBEVENT subType;
	/**
	 * True for a visible event, false otherwise. Note a given event
	 * must be visible from Earth and with positive elevation to be
	 * observable.
	 */
	public boolean visibleFromEarth;
	/**
	 * Elevation of the source in radians above the observer, calculated
	 * even if requested ephemerides are geocentric.
	 */
	public double elevation;
	/**
	 * This flag is false always except when calculating natural satellites events
	 * (mutual and non-mutual). In this case this flag will be true for those
	 * events that are simultaneous (several events going on at the same time).
	 * In practice, this flag is true always for non-mutual events involving two or
	 * more satellites, and also true always for mutual events (since they involve
	 * two satellites).
	 */
	public boolean severalSimultaneousEvents = false;

	/**
	 * The set of moon events.
	 */
	public enum EVENT {
		/** ID code for an eclipse event. */
		ECLIPSED,
		/** Id code for an occultation event. */
		OCCULTED,
		/** Id code for a transit event. */
		TRANSIT,
		/** Id code for a shadow transit event. */
		SHADOW_TRANSIT,
	};

	/**
	 * The set of moon subevents (start and end times).
	 */
	public enum SUBEVENT {
		/** ID code for an event that begins. */
		START,
		/** Id code for an event that ends. */
		END,
		/** Id code for an event that begins and ends. */
		TIME
	};

	/**
	 * Holds a description of the events.
	 */
	public static final String[] EVENTS = new String[] {"Eclipsed", "Occulted", "Transiting",
		"Shadow transiting"};

	/**
	 * Empty constructor.
	 */
	public MoonEventElement() {}

	/**
	 * Constructor for a simple event.
	 * @param jd Event time.
	 * @param main Main object involved.
	 * @param event Event id constant.
	 * @param details Details.
	 */
	public MoonEventElement(double jd, TARGET main, MoonEventElement.EVENT event, String details)
	{
		this.startTime = jd;
		this.endTime = jd;
		this.mainBody = main;
		this.secondaryBody = main;
		this.eventType = event;
		this.details = details;
		this.visibleFromEarth = true;
		this.subType = MoonEventElement.SUBEVENT.TIME;
	}
	/**
	 * Full constructor.
	 * @param jdi initial time.
	 * @param jdf Final time.
	 * @param main Main object involved.
	 * @param secondary Secondary object involved.
	 * @param event Event id constant.
	 * @param details Details.
	 */
	public MoonEventElement(double jdi, double jdf, TARGET main, TARGET secondary, MoonEventElement.EVENT event, String details)
	{
		this.startTime = jdi;
		this.endTime = jdf;
		this.mainBody = main;
		this.secondaryBody = secondary;
		this.eventType = event;
		this.details = details;
		this.visibleFromEarth = true;
		this.subType = MoonEventElement.SUBEVENT.TIME;
	}

	/**
	 * Transforms the event time into another time scale.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param start True to transform start time, false for end time.
	 * @param timeScale Ouput time scale id constant.
	 * @return The output time.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getEventTime(ObserverElement obs, EphemerisElement eph, boolean start, SCALE timeScale)
	throws JPARSECException {
		TimeElement time = new TimeElement(this.startTime, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		if (!start) time = new TimeElement(this.endTime, SCALE.BARYCENTRIC_DYNAMICAL_TIME);

		double out = TimeScale.getJD(time, obs, eph, timeScale);
		return out;
	}

	/**
	 * Reports the current event to the console.
	 * @throws JPARSECException If an error occurs.
	 */
	public void report()
	throws JPARSECException {
		System.out.println(this.mainBody.getName()+" "+MoonEventElement.EVENTS[this.eventType.ordinal()]+" by "+this.secondaryBody.getName()+" between "+this.startTime+" and "+this.endTime+" (TDB). Details: "+this.details+".");
	}
	/**
	 * Clones this instance.
	 */
	@Override
	public MoonEventElement clone()
	{
		MoonEventElement e = new MoonEventElement(this.startTime, this.endTime, this.mainBody, this.secondaryBody,
				this.eventType, this.details);
		e.elevation = this.elevation;
		e.visibleFromEarth = this.visibleFromEarth;
		e.subType = this.subType;
		e.severalSimultaneousEvents = this.severalSimultaneousEvents;
		return e;
	}
	/**
	 * Returns wether the input Object contains the same information
	 * as this instance.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof MoonEventElement)) return false;

		MoonEventElement that = (MoonEventElement) o;

		if (Double.compare(that.startTime, startTime) != 0) return false;
		if (Double.compare(that.endTime, endTime) != 0) return false;
		if (visibleFromEarth != that.visibleFromEarth) return false;
		if (Double.compare(that.elevation, elevation) != 0) return false;
		if (severalSimultaneousEvents != that.severalSimultaneousEvents) return false;
		if (mainBody != that.mainBody) return false;
		if (secondaryBody != that.secondaryBody) return false;
		if (eventType != that.eventType) return false;
		if (details != null ? !details.equals(that.details) : that.details != null) return false;

		return subType == that.subType;
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		temp = Double.doubleToLongBits(startTime);
		result = (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(endTime);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (mainBody != null ? mainBody.hashCode() : 0);
		result = 31 * result + (secondaryBody != null ? secondaryBody.hashCode() : 0);
		result = 31 * result + (eventType != null ? eventType.hashCode() : 0);
		result = 31 * result + (details != null ? details.hashCode() : 0);
		result = 31 * result + (subType != null ? subType.hashCode() : 0);
		result = 31 * result + (visibleFromEarth ? 1 : 0);
		temp = Double.doubleToLongBits(elevation);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (severalSimultaneousEvents ? 1 : 0);
		return result;
	}
}
