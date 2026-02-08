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
import jparsec.math.Constant;
import jparsec.math.FastMath;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.time.TimeScale;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;

/**
 * Obtain circumstances of lunar eclipses. This class implements a
 * 'brute-force' method that does not require Bessel elements and is
 * independent from the ephemerides theory. It is very fast if used
 * correctly. The effect of the mountains on lunar limb is ignored.
 * <BR>
 * One advantage of this pure geometric approach is the possibility
 * of calculating eclipses by other satellites (not the Moon).
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class LunarEclipse
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
	private boolean[] checkEclipse(TimeElement time, ObserverElement obs, EphemerisElement eph)
			throws JPARSECException
	{
		EphemerisElement new_eph = eph.clone();
		new_eph.targetBody = targetBody;
		EphemerisElement.ALGORITHM alg = new_eph.algorithm;
		if (alg == ALGORITHM.NATURAL_SATELLITE) alg = ALGORITHM.MOSHIER;
		if (targetBody != TARGET.Moon) new_eph.algorithm = ALGORITHM.NATURAL_SATELLITE;
		new_eph.isTopocentric = false;
		EphemElement ephem_moon = Ephem.getEphemeris(time, obs, new_eph, false);
		new_eph.targetBody = TARGET.SUN;
		new_eph.algorithm = alg;
		EphemElement ephem = Ephem.getEphemeris(time, obs, new_eph, false);


		TARGET motherBody = targetBody.getCentralBody();

		boolean inside_shadow = false, totality = false;
		boolean inside_penumbra = false, totality_penumbra = false;
		if (targetBody != TARGET.Moon) {
			// Locate the position of the planet shadow cone (opposite direction of the geocentric position of the Sun)
			LocationElement loc = new LocationElement(ephem.rightAscension + Math.PI, -ephem.declination, 1);
			// Distance to the geocentric position of the moon
			double d = LocationElement.getAngularDistance(loc, ephem_moon.getEquatorialLocation());
			// Get better estimate of angle contributions of mother planet, satellite, and Sun. 
			// Note ss is not fully accurate, should be size as visible from the satellite, not the geocenter
			double s = Math.asin(motherBody.equatorialRadius / (ephem_moon.distance * Constant.AU));
			double sm = Math.asin(targetBody.equatorialRadius / (ephem_moon.distance * Constant.AU));
			double ss = Math.asin(TARGET.SUN.equatorialRadius / (ephem.distance * Constant.AU));
			
			// Apply eclipse conditions (more or less)
			if (d < s + sm + ss * 2) inside_penumbra = true;
			if (d < s - sm + ss * 2) totality_penumbra = true;
			if (d < s + ephem_moon.angularRadius) inside_shadow = true;
			if (d < s - ephem_moon.angularRadius) totality = true;

			boolean out[] = new boolean[] { inside_penumbra, totality_penumbra, inside_shadow, totality };
			return out;
		}

		// Get shadow cone direction
		double sun_size = ephem.angularRadius;
		ephem.rightAscension += Math.PI;
		ephem.declination = -ephem.declination;
		ephem.distance = ephem_moon.distance;

		// The main calculation is to position the center of the Earth shadow
		// cone. We consider this cone to be indeed an oval with a size
		// slightly larger than the Earth's equatorial and polar radius.
		// This excess can be understood taking into account Earth surface
		// elevation and opacity of the atmosphere. Values fitted to lunar
		// eclipses in 2007. Note AA supplement uses 1.02 in page 429, but more
		// precision is required (and a separation between polar and equatorial
		// axis) to get an accuracy of 1 second or better.
		// See http://eclipse.gsfc.nasa.gov/LEcat5/shadow.html for a discussion
		double val_eq = 1.0131;
		double val_pol = 1.015;
		double EarthShadowConeSize = motherBody.equatorialRadius / (Constant.AU * Math
				.tan(ephem.angularRadius));
		LocationElement shadow_loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
		double ang_radius_max = Math.atan2(motherBody.equatorialRadius / Constant.AU, ephem_moon.distance) * (val_eq - (ephem_moon.distance / EarthShadowConeSize));
		double ang_radius_min = Math.atan2(motherBody.polarRadius / Constant.AU, ephem_moon.distance) * (val_pol - (ephem_moon.distance / EarthShadowConeSize));
		double penumbra_ang_radius = 2.0 * sun_size;
		double penumbra_scale_max = ang_radius_max + penumbra_ang_radius;
		double penumbra_scale_min = ang_radius_min + penumbra_ang_radius;

		LocationElement moon_loc = new LocationElement(ephem_moon.rightAscension, ephem_moon.declination, 1.0);
		double dist = LocationElement.getAngularDistance(moon_loc, shadow_loc);
		double pa = 3.0 * Constant.PI_OVER_TWO - LocationElement.getPositionAngle(moon_loc, shadow_loc);

		double s_x = Math.sin(pa) / ang_radius_max;
		double s_y = Math.cos(pa) / ang_radius_min;
		double s_r = 1.0 / FastMath.hypot(s_x, s_y);

		if (dist <= (s_r + ephem_moon.angularRadius))
			inside_shadow = true;
		if (dist <= (s_r - ephem_moon.angularRadius))
			totality = true;

		s_x = Math.sin(pa) / penumbra_scale_max;
		s_y = Math.cos(pa) / penumbra_scale_min;
		s_r = 1.0 / FastMath.hypot(s_x, s_y);

		if (dist <= (s_r + ephem_moon.angularRadius))
			inside_penumbra = true;
		if (dist <= (s_r - ephem_moon.angularRadius))
			totality_penumbra = true;

		boolean out[] = new boolean[] { inside_penumbra, totality_penumbra, inside_shadow, totality };
		return out;
	}

	/**
	 * Sets the desired accuracy of the iterative method in seconds.
	 * @param s Accuracy in seconds, must be > 0. Default initial value is 1s.
	 */
	private void setAccuracy(double s)  {
		if (s > 0) step = s / Constant.SECONDS_PER_DAY;
	}
	private double step = 1.0 / Constant.SECONDS_PER_DAY;

	private double[] events;

	/**
	 * Obtain events for the next lunar eclipse in TDB. Input time should be
	 * Immediately before, or the day before a certain lunar eclipse starts
	 * (penumbra ingress). Precision in the results is up to 1 second.
	 * Comparisons with NASA published values (calculations by Fred Spenak) show
	 * a mean difference of 2 seconds (sometimes a little more due to possibly
	 * different dynamical time corrections).
	 * <BR>
	 * The origin of this minimum discrepancy is probably due to the fact that here
	 * the difference between the Moon center of mass and its geometric center
	 * is not corrected.
	 * Otherwise, the irregular limb profile of the Moon (not taken into account
	 * here nor in Spenak's calculations) produces more effects than this
	 * negligible discrepancy.
	 * <BR>
	 * Events are:
	 * <BR> - Penumbra ingress start.
	 * <BR> - Penumbra total ingress.
	 * <BR> - Shadow ingress.
	 * <BR> - Shadow total ingress.
	 * <BR> - Shadow total egress.
	 * <BR> - Shadow egress.
	 * <BR> - Penumbra total egress.
	 * <BR> - Penumbra egress.
	 * <BR>
	 * If you are calculation eclipses for current dates the Moshier algorithm will give a
	 * good performance. For better precision in ancient times use ELP2000.
	 *
	 * Moon/Sun elevation above local horizon is not considered in this method, so output
	 * events can be not visible by the input observer.
	 * @param time Time object with the date of the eclipse before it starts, but as close
	 * as possible (within 3 days).
	 * @param obs Observer object.
	 * @param eph Ephemeris properties.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public LunarEclipse(TimeElement time, ObserverElement obs, EphemerisElement eph)
			throws JPARSECException
	{
		init(time, obs, eph);
	}

	/**
	 * Obtain events for the next lunar eclipse in TDB. Input time should be
	 * Immediately before, or the day before a certain lunar eclipse starts
	 * (penumbra ingress). This constructor is similar to the other one, but
	 * it is intended to be used to calculate eclipses produced by other
	 * satellites, not the Moon. So it adds an accuracy parameter to control
	 * the sensitivity of the search. Eclipses with a duration below 60s could 
	 * be missed.
	 * <BR>
	 * Events are:
	 * <BR> - Penumbra ingress start.
	 * <BR> - Penumbra total ingress.
	 * <BR> - Shadow ingress.
	 * <BR> - Shadow total ingress.
	 * <BR> - Shadow total egress.
	 * <BR> - Shadow egress.
	 * <BR> - Penumbra total egress.
	 * <BR> - Penumbra egress.
	 * <BR>
	 * Moon/Sun elevation above local horizon is not considered in this method, so output
	 * events can be not visible by the input observer.
	 * @param time Time object with the date of the eclipse before it starts, but as close
	 * as possible.
	 * @param obs Observer object.
	 * @param eph Ephemeris properties.
	 * @param accuracy Accuracy of the iterative search in seconds. 1s will be used if 0 or 
	 * lower is entered.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public LunarEclipse(TimeElement time, ObserverElement obs, EphemerisElement eph, double accuracy)
			throws JPARSECException
	{
		this.setAccuracy(accuracy);
		init(time, obs, eph);
	}

	private void init(TimeElement time, ObserverElement obs, EphemerisElement eph) throws JPARSECException {
		if (eph.targetBody != TARGET.Moon && !eph.targetBody.isNaturalSatellite()) throw new JPARSECException("Target body must be the Moon or any other natural satellite.");
		if (eph.targetBody.getCentralBody() != obs.getMotherBody()) throw new JPARSECException("Target body must orbit around the observer.");
		targetBody = eph.targetBody;

		double jd = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME), jd0 = jd;
		double out[] = new double[]
		{ 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };

		double jdPrev = -1, statusPrev = -1, outPrev[] = null;
		boolean detailedMode = false;
		do {
			jd += step;
			TimeElement new_time = new TimeElement(jd, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
			boolean event[] = checkEclipse(new_time, obs, eph);

			if (event[0] && out[0] == 0.0)
				out[0] = jd;
			if (event[1] && out[1] == 0.0)
				out[1] = jd;
			if (event[2] && out[2] == 0.0)
				out[2] = jd;
			if (event[3] && out[3] == 0.0)
				out[3] = jd;

			if (!event[3] && out[4] == 0.0 && out[3] != 0.0)
				out[4] = jd;
			if (!event[2] && out[5] == 0.0 && out[2] != 0.0)
				out[5] = jd;
			if (!event[1] && out[6] == 0.0 && out[1] != 0.0)
				out[6] = jd;
			if (!event[0] && out[7] == 0.0 && out[0] != 0.0)
				out[7] = jd;

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
			jd += 60 / Constant.SECONDS_PER_DAY;
			if (jd - jd0 > 3) throw new JPARSECException("No eclipse found in the next 3 days.");
		} while (out[7] == 0.0);

		this.events = out;
	}

	/**
	 * Return eclipse maximum as Julian day in TDB. Output is the intermediate time
	 * between the deepest events in the current eclipse. Accurate up to some
	 * seconds in total eclipses.
	 *
	 * @return Eclipse maximum.
	 */
	public double getEclipseMaximum()
	{
		double jd_max = (events[0] + events[7]) * 0.5;
		if (events[1] != 0.0)
			jd_max = (events[1] + events[6]) * 0.5;
		if (events[2] != 0.0)
			jd_max = (events[2] + events[5]) * 0.5;
		if (events[3] != 0.0)
			jd_max = (events[3] + events[4]) * 0.5;

		return jd_max;
	}

	/**
	 * Return eclipse type as a string.
	 *
	 * @return Eclipse type, such as penumbral, partial, or total.
	 */
	public String getEclipseType()
	{
		if (events[3] != 0.0) return Translate.translate(168); // total
		if (events[2] != 0.0) return Translate.translate(169); // partial
		return Translate.translate(1271).toLowerCase(); // penumbral
	}

	/**
	 * Returns the set of events. Times are given in TDB.
	 * @return Events as an object array. Four events (or less): penumbra starts,
	 * penumbra total ingress, partial phase, total phase.
	 */
	public MoonEventElement[] getEvents()
	{
		String e[] = new String[] {Translate.translate(1271), Translate.translate(1272),
			Translate.translate(169), Translate.translate(400)};
		int count = 0;
		MoonEventElement event[] = new MoonEventElement[4];
		for (int i=0; i<4; i++)
		{
			event[i] = new MoonEventElement(events[i], events[7-i], targetBody, targetBody.getCentralBody(), MoonEventElement.EVENT.ECLIPSED, e[i]);
			if (events[i] > 0.0) count ++;
		}

		MoonEventElement out[] = new MoonEventElement[count];
		int index = -1;
		for (int i=0; i<4; i++)
		{
			if (event[i].startTime > 0.0) {
				index ++;
				out[index] = event[i].clone();
			}
		}
		return out;
	}
}
