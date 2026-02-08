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

import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.EphemerisElement.ALGORITHM;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.planets.EphemElement;
import jparsec.graph.chartRendering.RenderEclipse;
import jparsec.math.Constant;
import jparsec.math.FastMath;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.AstroDate;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.time.TimeScale;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;

/**
 * Obtain circumstances of solar eclipses. This class implements a
 * 'brute-force' method that does not require Bessel elements and is
 * independent from the ephemerides theory. It is quite fast if used
 * correctly. The effect of the mountains on lunar limb is ignored.
 * <BR>
 * One advantage of this pure geometric approach is the possibility
 * of calculating solar eclipses from other satellites (not the Moon).
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class SolarEclipse
{
	private TARGET targetBody = null;

	/**
	 * Checks for events.
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris properties.
	 * @return A set of true or false values for the events.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	private boolean[] checkEclipse(TimeElement time, ObserverElement obs, EphemerisElement new_eph)
			throws JPARSECException
	{
		//new_eph.isTopocentric = true;
		new_eph.targetBody = targetBody;
		EphemerisElement.ALGORITHM alg = new_eph.algorithm;
		if (alg == ALGORITHM.NATURAL_SATELLITE) alg = ALGORITHM.MOSHIER;
		if (targetBody != TARGET.Moon) new_eph.algorithm = ALGORITHM.NATURAL_SATELLITE;
		EphemElement ephem_moon = Ephem.getEphemeris(time, obs, new_eph, false);

		new_eph.targetBody = TARGET.SUN;
		new_eph.algorithm = alg;
		EphemElement ephem_sun = Ephem.getEphemeris(time, obs, new_eph, false);
		LocationElement sun_loc = ephem_sun.getEquatorialLocation();
		LocationElement moon_loc = ephem_moon.getEquatorialLocation();
		double dist = LocationElement.getAngularDistance(moon_loc, sun_loc);
		double pa = 3.0 * Constant.PI_OVER_TWO - LocationElement.getPositionAngle(sun_loc, moon_loc) - ephem_moon.positionAngleOfAxis;

		double m_x = Math.sin(pa) / ephem_moon.angularRadius;
		double m_y = Math.cos(pa) / ephem_moon.angularRadius;
		double m_r = 1.0 / FastMath.hypot(m_x, m_y);
		double s_r = ephem_sun.angularRadius;

		boolean eclipsed = false, totality = false;
		if (dist <= (s_r + m_r)) eclipsed = true;

		if (((dist + m_r) <= s_r && (m_r <= s_r)) || ((dist + s_r) <= m_r && (m_r >= s_r)))
		{
			totality = true;
			if (m_r > s_r)	{
				type = ECLIPSE_TYPE.TOTAL;
			} else {
				type = ECLIPSE_TYPE.ANNULAR;
			}
		} else {
			if (eclipsed && type == ECLIPSE_TYPE.NO_ECLIPSE) type = ECLIPSE_TYPE.PARTIAL;
		}

		boolean out[] = new boolean[] { eclipsed, totality };
		return out;
	}

	/**
	 * Sets the desired accuracy of the iterative method in seconds for
	 * eclipses produced by any moon.
	 * @param s Accuracy in seconds, must be > 0.
	 */
	private void setAccuracy(double s)  {
		if (s > 0) step = s / Constant.SECONDS_PER_DAY;
	}
	private double step = 0.5 / Constant.SECONDS_PER_DAY;

	private double jdMax;
	private double[] events;

	/**
	 * The set of eclipse types for both solar and lunar eclipses.
	 */
	public enum ECLIPSE_TYPE {
		/** Constant ID for a total solar/lunar eclipse. */
		TOTAL,
		/** Constant ID for a partial solar/lunar eclipse. */
		PARTIAL,
		/** Constant ID for an annular solar eclipse. */
		ANNULAR,
		/**
		 * Constant ID for an inexistent solar eclipse. In practice, the algorithm
		 * finds the next eclipse, so this type will never exist as output. This value
		 * is used only internally.
		 */
		NO_ECLIPSE
	};

	/**
	 * Eclipse type.
	 */
	public ECLIPSE_TYPE type = ECLIPSE_TYPE.NO_ECLIPSE;

	/**
	 * Obtain events for the next solar eclipse in TDB. Input time should be
	 * Immediately before, or the day before a certain solar eclipse starts
	 * (shadow ingress). Precision in the results is up to 0.5 seconds.
	 * Comparisons with NASA published values (calculations by Fred Spenak) show
	 * a mean difference of 1 second. The origin of this minimum discrepancy is
	 * probably due to the fact that here the difference between the Moon center
	 * of mass and its geometric center is not corrected. Otherwise, the irregular
	 * limb profile of the Moon (not taken into account here nor in Spenak's
	 * calculations) produces more effects than this negligible discrepancy.
	 *
	 * Events are:<BR>
	 * - Shadow ingress.<BR>
	 * - Shadow total/annular ingress.<BR>
	 * - Shadow total/annular egress.<BR>
	 * - Shadow egress.
	 *
	 * Eclipse type (total, partial, annular) is set in static variable type.
	 *
	 * If you are calculation eclipses for current dates the Moshier algorithm will give a
	 * good performance. For better precision in ancient times use ELP2000, although internal
	 * precision of the algorithm will be reduced to 1 second instead of 0.5s to improve
	 * performance.
	 *
	 * Moon/Sun elevation above local horizon is not considered in this method, so output
	 * events can be not visible by the input observer.
	 *
	 * @param time Time object with the date of the eclipse before it starts, but as close
	 * as possible.
	 * @param obs Observer object.
	 * @param eph Ephemeris properties.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public SolarEclipse (TimeElement time, ObserverElement obs, EphemerisElement eph)
			throws JPARSECException
	{
		init(time, obs, eph);
	}

	/**
	 * Obtain events for the next solar eclipse in TDB. Input time should be
	 * Immediately before, or the day before a certain solar eclipse starts
	 * (shadow ingress). This constructor is similar to the other one, but
	 * it is intended to be used to calculate eclipses produced by other
	 * satellites, not the Moon. So it adds an accuracy parameter to control
	 * the sensitivity of the search. Eclipses with a duration below 10s could 
	 * be missed.
	 *
	 * Events are:<BR>
	 * - Shadow ingress.<BR>
	 * - Shadow total/annular ingress.<BR>
	 * - Shadow total/annular egress.<BR>
	 * - Shadow egress.
	 *
	 * Eclipse type (total, partial, annular) is set in static variable type.
	 *
	 * Moon/Sun elevation above local horizon is not considered in this method, so output
	 * events can be not visible by the input observer.
	 *
	 * @param time Time object with the date of the eclipse before it starts, but as close
	 * as possible (within 3 days).
	 * @param obs Observer object.
	 * @param eph Ephemeris properties.
	 * @param accuracy Accuracy of the iterative search in seconds for eclipses produced by
	 * any moon. 0.5s is used if 0 or lower is entered.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public SolarEclipse (TimeElement time, ObserverElement obs, EphemerisElement eph,
			double accuracy) throws JPARSECException {
		this.setAccuracy(accuracy);
		init(time, obs, eph);
	}

	private void init(TimeElement time, ObserverElement obs, EphemerisElement eph) throws JPARSECException {
		if (eph.targetBody != TARGET.Moon && !eph.targetBody.isNaturalSatellite()) throw new JPARSECException("Target body must be the Moon or any other natural satellite.");
		if (eph.targetBody.getCentralBody() != obs.getMotherBody()) throw new JPARSECException("Target body must orbit around the observer.");
		targetBody = eph.targetBody;

		double jd = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME), jd0 = jd;
		double out[] = new double[] { 0.0, 0.0, 0.0, 0.0 };

		EphemerisElement newEph = eph.clone();
		type = ECLIPSE_TYPE.NO_ECLIPSE;
		double jdPrev = -1, statusPrev = -1, outPrev[] = null;
		boolean detailedMode = false;
		boolean fromEarth = obs.getMotherBody() == TARGET.EARTH;
		do {
			jd += step;
			TimeElement new_time = new TimeElement(jd, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
			boolean event[] = checkEclipse(new_time, obs, newEph);

			if (event[0] && out[0] == 0.0)
				out[0] = jd;
			if (event[1] && out[1] == 0.0)
				out[1] = jd;

			if (!event[1] && out[2] == 0.0 && out[1] != 0.0)
				out[2] = jd;
			if (!event[0] && out[3] == 0.0 && out[0] != 0.0)
				out[3] = jd;

			double status = 0;
			for (int i=0; i<out.length; i++) {
				if (out[i] != 0) status += Math.pow(10, i);
			}
			if (statusPrev >= 0 && status != statusPrev && !detailedMode) {
				out = outPrev;
				jd = jdPrev;
				detailedMode = true;
				continue;
			}
			if (detailedMode && status == statusPrev) continue;
			
			detailedMode = false;
			jdPrev = jd;
			statusPrev = status;
			outPrev = out.clone();
			jd += 10 / Constant.SECONDS_PER_DAY;
			if (jd - jd0 > 3) throw new JPARSECException("No eclipse found in the next 3 days.");
		} while (out[3] == 0.0);

		events = out;

		RenderEclipse re = null;
		jdMax = (events[0] + events[3]) * 0.5;
		if (events[1] != 0.0) jdMax = (events[1] + events[2]) * 0.5;
		if (fromEarth) {
			try {
				re = new RenderEclipse(new AstroDate(events[0] - 0.25));
			} catch (Exception exc) {
				re = new RenderEclipse(new AstroDate(events[0] + 0.25));
			}
			jdMax = TimeScale.getJD(re.solarEclipseMaximum(obs, eph), obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		}
	}

	/**
	 * Returns type of the current calculated eclipse.
	 *
	 * @return Eclipse type, such as annular, partial, or total.
	 */
	public String getEclipseType()
	{
		return Saros.getEclipseTypeAsString(type);
	}

	/**
	 * Returns the set of events. Times are given in TDB.
	 * @return Events as an object array. Two events defining
	 * the start/end of the partial phase, and start/end of the
	 * total/annular phase. In case of a partial eclipse only
	 * one event is returned.
	 */
	public MoonEventElement[] getEvents()
	{
		String e[] = new String[] {Translate.translate(169), Translate.translate(400)};
		if (this.type == ECLIPSE_TYPE.ANNULAR) e[1] = Translate.translate(167);
		int count = 0;
		MoonEventElement event[] = new MoonEventElement[2];
		for (int i=0; i<2; i++)
		{
			event[i] = new MoonEventElement(events[i], events[3-i], TARGET.SUN, targetBody, MoonEventElement.EVENT.ECLIPSED, e[i]);
			if (events[i] > 0.0) count ++;
		}

		MoonEventElement out[] = new MoonEventElement[count];
		int index = -1;
		for (int i=0; i<2; i++)
		{
			if (event[i].startTime > 0.0) {
				index ++;
				out[index] = event[i].clone();
			}
		}

		return out;
	}

	/**
	 * Return eclipse maximum as Julian day.
	 *
	 * @return Eclipse maximum in TDB.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getEclipseMaximum() throws JPARSECException
	{
		return jdMax;
/*		double jd_max = (events[0] + events[3]) * 0.5;
		if (events[1] != 0.0)
			jd_max = (events[1] + events[2]) * 0.5;

		return jd_max;
*/	}
}
