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
package jparsec.graph.chartRendering;

import jparsec.astronomy.CoordinateSystem;
import jparsec.astronomy.CoordinateSystem.COORDINATE_SYSTEM;
import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.Functions;
import jparsec.ephem.Obliquity;
import jparsec.ephem.PhysicalParameters;
import jparsec.ephem.Precession;
import jparsec.ephem.RiseSetTransit;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.planets.EphemElement;
import jparsec.math.Constant;
import jparsec.math.FastMath;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.SiderealTime;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.time.TimeScale;
import jparsec.util.JPARSECException;

/**
 * Provides support for drawing the sky using different kind of projections.
 * The projections are implemented here without using other (more complete)
 * libraries, since these other libraries are generally too slow.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Projection
{
	/**
	 * The set of available projections.
	 */
	public enum PROJECTION {
		/** Symbolic constant for stereographic sky projection. */
		STEREOGRAPHICAL,
		/** Symbolic constant for spheric sky projection. */
		SPHERICAL,
		/** Symbolic constant for mercator sky projection. */
		CYLINDRICAL,
		/** Symbolic constant for cylindrical equidistant sky projection. */
		CYLINDRICAL_EQUIDISTANT,
		/** Symbolic constant for polar sky projection. */
		POLAR
	};

	/**
	 * The coordinate system list array.
	 */
	public static final String PROJECTIONS[] = new String[] {
		"Stereographical", "Spherical", "Cylindrical", "Cylindrical equidistant", "Polar"
	};

	/**
	 * Ephemeris object to transform coordinates in sky renderings.
	 */
	public EphemerisElement eph;

	/**
	 * Time object to transform coordinates in sky renderings.
	 */
	public TimeElement time;

	/**
	 * Observer object to transform coordinates in sky renderings.
	 */
	public ObserverElement obs;

	/**
	 * Sky render object
	 */
	SkyRenderElement render;

	/**
	 * Holds field of view in radians, automatically set from the current
	 * selected telescope.
	 */
	private float field;
	/**
	 * Center position in pixels.
	 */
	int centerX;

	int centerY;

	/**
	 * Corrected obliquity.
	 */
	public double obliquity;

	/**
	 * Apparent Sidereal Time.
	 */
	double ast;
	double jd;
	private float sy = 1.0f, sx = 1.0f, stx = 1.0f, rwf = 0.0f; //scale_x = 1.0f, scale_y = 0.9f, sy = 1.0f;
	private double horizon_elevation = 0.0;
	private float sin_lat0, cos_lat0, cos_lat0_times_sy, sin_lat0_times_sy;
	private boolean fastCalc = true;
	private LocationElement np = null;

	/**
	 * Constructor.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param render Render object.
	 * @param field Field of view in radians.
	 * @param x0 X center position in pixels.
	 * @param y0 Y center position in pixels.
	 * @throws JPARSECException If an error occurs.
	 */
	public Projection(TimeElement time, ObserverElement obs, EphemerisElement eph, SkyRenderElement render,
			double field, int x0, int y0)
	throws JPARSECException {
		this.time = time.clone();
		this.obs = obs.clone();
		this.render = render.clone();
		this.eph = eph.clone();
		this.eph.correctEquatorialCoordinatesForRefraction = false;
		this.centerX = x0;
		this.centerY = y0;

		jd = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		double equinox = eph.equinox;
		if (equinox == EphemerisElement.EQUINOX_OF_DATE)
			equinox = jd;

		if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT) {
			obliquity = Obliquity.trueObliquity(Functions.toCenturies(equinox), eph);
		} else {
			obliquity = Obliquity.meanObliquity(Functions.toCenturies(equinox), eph);
		}
		try {
			ast = SiderealTime.apparentSiderealTime(time, obs, eph);
			if (eph.equinox != EphemerisElement.EQUINOX_OF_DATE && render.coordinateSystem == COORDINATE_SYSTEM.HORIZONTAL) {
				LocationElement loc = new LocationElement(ast, obs.getLatitudeRad(), 1.0);
				if (eph.ephemType != EphemerisElement.COORDINATES_TYPE.APPARENT) loc = Ephem.toMeanEquatorial(loc, time, obs, eph);
				EphemElement ephem = new EphemElement();
				ephem.setEquatorialLocation(loc);
				loc = Ephem.toOutputEquinox(ephem, eph, jd).getEquatorialLocation();
				ast = loc.getLongitude();
				this.obs.setLatitudeRad(loc.getLatitude());
			}
		} catch (Exception exc) {}
		horizon_elevation = RiseSetTransit.horizonDepression(obs, eph);

		EphemerisElement ephIn = eph.clone();
		ephIn.targetBody = obs.getMotherBody();
		np = PhysicalParameters.getBodyNorthPole(jd, ephIn);

		this.configure(render);
		setField(field);
	}

	/**
	 * Updates the projection instance for a new sky rendering.
	 * @param render Render object.
	 * @param field Field of view in radians.
	 * @param x0 X center position in pixels.
	 * @param y0 Y center position in pixels.
	 * @throws JPARSECException If an error occurs.
	 */
	public void updateProjection(SkyRenderElement render, double field, int x0, int y0)
	throws JPARSECException {
		this.render = render.clone();
		this.centerX = x0;
		this.centerY = y0;
		lh = null;

		configure(render);
		setField(field);
	}

	/**
	 * Invalid ID position for both x and y, if the position is outside
	 */
	public static final float INVALID_POSITION[] = null;

	/**
	 * Checks if certain position in the screen is invalid or not.
	 *
	 * @param position Input position.
	 * @return True if it is invalid.
	 */
	public boolean isInvalid(float[] position)
	{
		return (position == INVALID_POSITION);
	}

	/**
	 * Configures the selected projection with a new sky render object.
	 *
	 * @param render Sky render object.
	 * @throws JPARSECException If the method fails.
	 */
	public void configure(SkyRenderElement render) throws JPARSECException
	{
		this.render = render; //.clone();
		sx = 1.0f;
		if (render.coordinateSystem == CoordinateSystem.COORDINATE_SYSTEM.HORIZONTAL) sx = -1.0f;
		stx = sx * (float) Math.PI;
		sin_lat0 = (float) Math.sin(render.centralLatitude);
		cos_lat0 = (float) Math.cos(render.centralLatitude);
		loc0 = new LocationElement(render.centralLongitude, render.centralLatitude, 1.0);
		setField(render.telescope.getField());
 	}

	/**
	 * Returns the distance to the current central position of rendering.
	 * better than 0.2 deg of precision.
	 * @param loc The position in the current coordinate system.
	 * @return Approximate angular distance.
	 */
	public double getApproximateAngularDistance(LocationElement loc)
	{
		return FastMath.acos(FastMath.sin(loc.getLatitude()) * sin_lat0 + FastMath.cos(loc.getLatitude()) * cos_lat0 * FastMath.cos(loc.getLongitude()-loc0.getLongitude()));
	}

	/**
	 * Returns the distance to the current central position of rendering.
	 * better than 0.2 deg of precision.
	 * @param ra RA or longitude;
	 * @param dec DEC or latitude.
	 * @return Approximate angular distance.
	 */
	public double getApproximateAngularDistance(double ra, double dec)
	{
		return FastMath.acos(FastMath.sin(dec) * sin_lat0 + FastMath.cos(dec) * cos_lat0 * FastMath.cos(ra-loc0.getLongitude()));
	}

	/**
	 * Changes the field of view.
	 * @param field New field of view in radians.
	 */
	public void setField(double field)
	{
		this.field = (float) field;
		sy = (float) (render.width * Constant.PI_OVER_TWO / field);
		fastCalc = true;
		if (sy > 5400) fastCalc = false; // accurate projection when resolution is 1' or better
		stxx = stx / this.field;
		stxxTimesCenterX = stxx * centerX;
		cos_lat0_times_sy = cos_lat0 * Math.abs(stxxTimesCenterX);
		sin_lat0_times_sy = sin_lat0 * Math.abs(stxxTimesCenterX);

		cylindrical = false;
		if (render.drawClever) {
			if (field < Constant.DEG_TO_RAD && Math.abs(render.centralLatitude) < Constant.PI_OVER_TWO-Constant.DEG_TO_RAD) cylindrical = true;
			if (field < Constant.PI_OVER_SIX && Math.abs(render.centralLatitude) < Constant.PI_OVER_TWO-field) cylindrical = true;
		}
		rwf = (float) (render.width / field);
		sxx = rwf * sx;
		sxs = sx * (float) Math.PI / this.field;
	}

	/**
	 * Returns the field of view.
	 * @return Field of view in radians.
	 */
	public double getField()
	{
		return this.field;
	}

	/**
	 * Returns the elevation of the horizon.
	 * @return Elevation of the horizon in radians.
	 */
	public double getHorizonElevation()
	{
		return this.horizon_elevation;
	}

	/**
	 * Sets the elevation of the horizon.
	 * @param val Elevation of the horizon in radians. Positive value.
	 * In case of a negative input (or 5 degrees in radians, which
	 * is a special value used internally) it will be recomputed for the currently
	 * selected location.
	 */
	public void setHorizonElevation(double val)
	{
/*		if (val >= 0 && val != 5*Constant.DEG_TO_RAD) {
			this.horizon_elevation = val;
		} else {
*/			try {
				horizon_elevation = RiseSetTransit.horizonDepression(obs, eph);
			} catch (Exception e) {		}
//		}
	}

	/**
	 * Obtains position of an object using the current sky projection and
	 * coordinate system.
	 *
	 * @param loc Location object with the position in equatorial
	 *        coordinates.
	 * @param size Angular radius of rendered object in pixels.
	 * @param check_limits True to check for object limits in the rendered
	 *        window (in a soft way).
	 * @return Array with (x, y) position in the screen.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public float[] project(LocationElement loc, int size, boolean check_limits)
			throws JPARSECException
	{
		loc = getApparentLocationInSelectedCoordinateSystem(loc, true, fastCalc, size/rwf);
		if (loc == null) return INVALID_POSITION;

		if (render.projection == PROJECTION.SPHERICAL &&
				getApproximateAngularDistance(loc) > Constant.PI_OVER_TWO)
			return INVALID_POSITION;

		float[] pos = project_position(loc);

		if (!check_limits || this.isInvalid(pos))
			return pos;

		if (size == 0 && render.projection == PROJECTION.STEREOGRAPHICAL) size = 100;
		if (pos[0] > (render.width + size) || pos[1] > (render.height + size))
			return INVALID_POSITION;
		if ((-size) > pos[0] || (-size) > pos[1])
			return INVALID_POSITION;

		return pos;
	}

	/**
	 * Obtains position of an object using the current sky projection and
	 * coordinate system. Special for Earth's shadow cone.
	 *
	 * @param loc Location object with the position in equatorial
	 *        coordinates.
	 * @param size Angular radius of rendered object in pixels.
	 * @param check_limits True to check for object limits in the rendered
	 *        window (in a soft way).
	 * @return Array with (x, y) position in the screen.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public float[] projectEarthShadow(LocationElement loc, int size, boolean check_limits)
			throws JPARSECException
	{
		loc = getApparentLocationInSelectedCoordinateSystem(loc, false, fastCalc, size/rwf);
		if (loc == null) return INVALID_POSITION;

		if (render.projection == PROJECTION.SPHERICAL &&
				getApproximateAngularDistance(loc) > Constant.PI_OVER_TWO)
			return INVALID_POSITION;

		float[] pos = project_position(loc);

		if (!check_limits)
			return pos;

		if (size == 0 && render.projection == PROJECTION.STEREOGRAPHICAL) size = 100;
		if ((-size) > pos[0] || (-size) > pos[1])
			return INVALID_POSITION;
		if (pos[0] > (render.width + size) || pos[1] > (render.height + size))
			return INVALID_POSITION;

		return pos;
	}

	private Boolean lh = null;
	/**
	 * Disables correction of local horizon.
	 */
	public void disableCorrectionOfLocalHorizon()
	{
		lh = render.drawSkyCorrectingLocalHorizon;
		render.drawSkyCorrectingLocalHorizon = false;
	}
	/**
	 * Resets the value of the correction of local horizon.
	 */
	public void enableCorrectionOfLocalHorizon()
	{
		if (lh != null) render.drawSkyCorrectingLocalHorizon = lh;
	}

	/**
	 * Returns apparent position of the input equatorial position correcting for local horizon and
	 * coordinate system.
	 * @param locIn Input coordinates in equatorial system.
	 * @param checkHorizon True to return null if the object is below the horizon and only the sky
	 * above horizon should be drawn.
	 * @return Output position, or null if the object will not be visible and to check
	 * horizon is enabled.
	 * @param fast True for an approximate but faster calculation.
	 * @param angRadius Angular radius of the object in radians.
	 * @throws JPARSECException If an error occurs.
	 */
	public LocationElement getApparentLocationInSelectedCoordinateSystem(LocationElement locIn, boolean checkHorizon,
			boolean fast, float angRadius)
	throws JPARSECException {

		switch (render.coordinateSystem)
		{
		case EQUATORIAL:
			if (obs.getMotherBody() != TARGET.NOT_A_PLANET && eph.isTopocentric && (render.drawSkyCorrectingLocalHorizon || (!render.drawSkyBelowHorizon && checkHorizon)))
			{
				LocationElement loc = CoordinateSystem.equatorialToHorizontal(locIn, ast, obs, eph, render.drawSkyCorrectingLocalHorizon, fast);
				if (!render.drawSkyBelowHorizon && loc.getLatitude() < -horizon_elevation-angRadius && checkHorizon) return null;
				loc = CoordinateSystem.horizontalToEquatorial(loc, ast, obs.getLatitudeRad(), fast);
				return loc;
			}
			break;
		case ECLIPTIC:
			if (obs.getMotherBody() != TARGET.NOT_A_PLANET && eph.isTopocentric && (render.drawSkyCorrectingLocalHorizon || (!render.drawSkyBelowHorizon && checkHorizon)))
			{
				LocationElement loc = CoordinateSystem.equatorialToHorizontal(locIn, ast, obs, eph, render.drawSkyCorrectingLocalHorizon, fast);
				if (!render.drawSkyBelowHorizon && loc.getLatitude() < -horizon_elevation-angRadius && checkHorizon) return null;
				loc = CoordinateSystem.horizontalToEquatorial(loc, ast, obs.getLatitudeRad(), fast);
				if (obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != TARGET.NOT_A_PLANET) loc = getPositionFromEarth(loc, true);
				return CoordinateSystem.equatorialToEcliptic(loc, obliquity, fast);
			} else {
				LocationElement loc = locIn;
				if (obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != TARGET.NOT_A_PLANET) loc = getPositionFromEarth(locIn, true);
				return CoordinateSystem.equatorialToEcliptic(loc, obliquity, fast);
			}
		case GALACTIC:
		case SUPERGALACTIC:
			LocationElement loc = locIn.clone();
			if (obs.getMotherBody() != TARGET.NOT_A_PLANET && eph.isTopocentric && (render.drawSkyCorrectingLocalHorizon || (!render.drawSkyBelowHorizon && checkHorizon)))
			{
				loc = CoordinateSystem.equatorialToHorizontal(loc, ast, obs, eph, render.drawSkyCorrectingLocalHorizon, fast);
				if (!render.drawSkyBelowHorizon && loc.getLatitude() < -horizon_elevation-angRadius && checkHorizon) return null;
				loc = CoordinateSystem.horizontalToEquatorial(loc, ast, obs.getLatitudeRad(), fast);
			}
			if (obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != TARGET.NOT_A_PLANET) loc = getPositionFromEarth(loc, true);
			double d = eph.getEpoch(jd);
			if (d != Constant.J2000) {
				if (fast) {
					loc = LocationElement.parseRectangularCoordinatesFast(Precession.precessToJ2000(jd, loc.getRectangularCoordinates(), eph));
				} else {
					loc = LocationElement.parseRectangularCoordinates(Precession.precessToJ2000(jd, loc.getRectangularCoordinates(), eph));
				}
			}
			loc = CoordinateSystem.equatorialToGalactic(loc, fast);
			if (render.coordinateSystem == COORDINATE_SYSTEM.SUPERGALACTIC)
				loc = CoordinateSystem.galacticToSupergalactic(loc);
			return loc;
		case HORIZONTAL:
			loc = CoordinateSystem.equatorialToHorizontal(locIn, ast, obs, eph, render.drawSkyCorrectingLocalHorizon, fast);
			if (!render.drawSkyBelowHorizon && loc.getLatitude() < -horizon_elevation-angRadius && checkHorizon) return null;
			return loc;
		}

		return locIn;
	}

	/**
	 * Returns apparent position of the input equatorial position correcting for local horizon and
	 * coordinate system.
	 * @param locIn Input coordinates in equatorial system.
	 * @param checkHorizon True to return null if the object is below the horizon and only the sky
	 * above horizon should be drawn.
	 * @return Output position, or null if the object will not be visible and to check
	 * horizon is enabled.
	 * @param fast True for an approximate but faster calculation.
	 * @param coordinateSystem The coordinate system for the output position.
	 * @param angRadius Angular radius of the object in radians.
	 * @throws JPARSECException If an error occurs.
	 */
	public LocationElement getApparentLocationInSelectedCoordinateSystem(LocationElement locIn, boolean checkHorizon,
			boolean fast, COORDINATE_SYSTEM coordinateSystem, float angRadius)
	throws JPARSECException {

		switch (coordinateSystem)
		{
		case EQUATORIAL:
			if (obs.getMotherBody() != TARGET.NOT_A_PLANET && eph.isTopocentric && (render.drawSkyCorrectingLocalHorizon || (!render.drawSkyBelowHorizon && checkHorizon)))
			{
				LocationElement loc = CoordinateSystem.equatorialToHorizontal(locIn, ast, obs, eph, render.drawSkyCorrectingLocalHorizon, fast);
				if (!render.drawSkyBelowHorizon && loc.getLatitude() < -horizon_elevation-angRadius && checkHorizon) return null;
				loc = CoordinateSystem.horizontalToEquatorial(loc, ast, obs.getLatitudeRad(), fast);
				return loc;
			}
			break;
		case ECLIPTIC:
			if (obs.getMotherBody() != TARGET.NOT_A_PLANET && eph.isTopocentric && (render.drawSkyCorrectingLocalHorizon || (!render.drawSkyBelowHorizon && checkHorizon)))
			{
				LocationElement loc = CoordinateSystem.equatorialToHorizontal(locIn, ast, obs, eph, render.drawSkyCorrectingLocalHorizon, fast);
				if (!render.drawSkyBelowHorizon && loc.getLatitude() < -horizon_elevation-angRadius && checkHorizon) return null;
				loc = CoordinateSystem.horizontalToEquatorial(loc, ast, obs.getLatitudeRad(), fast);
				if (obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != TARGET.NOT_A_PLANET) loc = getPositionFromEarth(loc, true);
				return CoordinateSystem.equatorialToEcliptic(loc, obliquity, fast);
			} else {
				LocationElement loc = locIn;
				if (obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != TARGET.NOT_A_PLANET) loc = getPositionFromEarth(loc, true);
				return CoordinateSystem.equatorialToEcliptic(loc, obliquity, fast);
			}
		case GALACTIC:
		case SUPERGALACTIC:
			LocationElement loc = locIn.clone();
			if (obs.getMotherBody() != TARGET.NOT_A_PLANET && eph.isTopocentric && (render.drawSkyCorrectingLocalHorizon || (!render.drawSkyBelowHorizon && checkHorizon)))
			{
				loc = CoordinateSystem.equatorialToHorizontal(loc, ast, obs, eph, render.drawSkyCorrectingLocalHorizon, fast);
				if (!render.drawSkyBelowHorizon && loc.getLatitude() < -horizon_elevation-angRadius && checkHorizon) return null;
				loc = CoordinateSystem.horizontalToEquatorial(loc, ast, obs.getLatitudeRad(), fast);
			}
			if (obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != TARGET.NOT_A_PLANET) loc = getPositionFromEarth(loc, true);
			double d = eph.getEpoch(jd);
			if (d != Constant.J2000) loc = LocationElement.parseRectangularCoordinates(Precession.precess(d, Constant.J2000, loc.getRectangularCoordinates(), eph));
			loc = CoordinateSystem.equatorialToGalactic(loc, fast);
			if (render.coordinateSystem == COORDINATE_SYSTEM.SUPERGALACTIC)
				loc = CoordinateSystem.galacticToSupergalactic(loc);
			return loc;
		case HORIZONTAL:
			boolean toApparent = false;
			if (obs.getMotherBody() == TARGET.EARTH && eph.isTopocentric && render.drawSkyCorrectingLocalHorizon) toApparent = true;
			loc = CoordinateSystem.equatorialToHorizontal(locIn, ast, obs, eph, toApparent, fast);
			if (!render.drawSkyBelowHorizon && loc.getLatitude() < -horizon_elevation-angRadius && checkHorizon) return null;
			return loc;
		}

		return locIn;
	}

	private LocationElement loc0;
	/**
	 * Obtains position of an object using the current sky projection and
	 * coordinate system, without transforming them to equatorial.
	 *
	 * @param loc Location object with the position in a given coordinate system.
	 * @param size Angular radius of rendered object in pixels.
	 * @param check_limits True to check for object limits in the rendered
	 *        window (in a soft way).
	 * @return Array with (x, y) position in the screen.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public float[] projectPosition(LocationElement loc, float size, boolean check_limits) throws JPARSECException
	{
		if (loc == null || (render.coordinateSystem == CoordinateSystem.COORDINATE_SYSTEM.HORIZONTAL && loc.getLatitude() < -horizon_elevation && render.drawSkyCorrectingLocalHorizon && !render.drawSkyBelowHorizon))
			return INVALID_POSITION;

		if (render.projection == PROJECTION.SPHERICAL &&
				getApproximateAngularDistance(loc) > Constant.PI_OVER_TWO)
			return INVALID_POSITION;

		float[] pos = project_position(loc);

		if (!check_limits || pos == null)
			return pos;

		if (size == 0 && render.projection == PROJECTION.STEREOGRAPHICAL) size = 100;
		if (-size > pos[0] || -size > pos[1])
			return INVALID_POSITION;
		if (pos[0] > (render.width + size) || pos[1] > (render.height + size))
			return INVALID_POSITION;

		return pos;
	}

	/**
	 * Obtains position of an object using the current sky projection and
	 * coordinate system, without transforming them to equatorial.
	 *
	 * @param loc Location object with the position in a given coordinate system.
	 * @param size Angular radius of rendered object in pixels.
	 * @param check_limits True to check for object limits in the rendered
	 *        window (in a soft way).
	 * @return Array with (x, y) position in the screen.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public float[] projectPosition(float loc[], float size, boolean check_limits) throws JPARSECException
	{
		if (loc == null || (render.coordinateSystem == CoordinateSystem.COORDINATE_SYSTEM.HORIZONTAL && loc[1] < -horizon_elevation && render.drawSkyCorrectingLocalHorizon && !render.drawSkyBelowHorizon))
			return INVALID_POSITION;

		if (render.projection == PROJECTION.SPHERICAL &&
				getApproximateAngularDistance(loc[0], loc[1]) > Constant.PI_OVER_TWO)
			return INVALID_POSITION;

		float[] pos = project_position(loc);

		if (!check_limits || pos == null)
			return pos;

		if (size == 0 && render.projection == PROJECTION.STEREOGRAPHICAL) size = 100;
		if (-size > pos[0] || -size > pos[1])
			return INVALID_POSITION;
		if (pos[0] > (render.width + size) || pos[1] > (render.height + size))
			return INVALID_POSITION;

		return pos;
	}
	/**
	 * Obtains position of an object using the current sky projection and
	 * coordinate system, without transforming them to equatorial.
	 *
	 * @param loc Location object with the position in a given coordinate system.
	 * @param size Angular radius of rendered object in pixels.
	 * @param check_limits True to check for object limits in the rendered
	 *        window (in a soft way).
	 * @param factor Number of screen widths to consider a position as invalid.
	 * @return Array with (x, y) position in the screen.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public float[] projectPosition(LocationElement loc, int size, boolean check_limits, int factor)
			throws JPARSECException
	{
		if (loc == null || (render.coordinateSystem == CoordinateSystem.COORDINATE_SYSTEM.HORIZONTAL && loc.getLatitude() < -horizon_elevation && render.drawSkyCorrectingLocalHorizon && !render.drawSkyBelowHorizon))
			return INVALID_POSITION;

		if (render.projection == PROJECTION.SPHERICAL &&
				getApproximateAngularDistance(loc) > Constant.PI_OVER_TWO)
			return INVALID_POSITION;

		float[] pos = project_position(loc);

		if (!check_limits)
			return pos;

		if (pos[0] > factor*(render.width + size) || pos[1] > factor*(render.height + size))
			return INVALID_POSITION;
		if ((factor-1)*(-size-render.width) > pos[0] || (factor-1)*(-size-render.height) > pos[1])
			return INVALID_POSITION;

		return pos;
	}

	/**
	 * Returns true if cylindrical equidistant projection is forced to ensure
	 * that sky is correctly drawn at the scales of planets.
	 * @return True or false.
	 */
	public boolean isCylindricalForced() {
		return cylindrical;
	}
	private boolean cylindrical = false;
	private float[] project_position(LocationElement loc) throws JPARSECException
	{
		if (cylindrical) return cylindricalEquidistant(loc);

		// Without waiting for switch is slightly faster ...
		if (render.projection == PROJECTION.STEREOGRAPHICAL) return stereographic(loc);

		switch (render.projection)
		{
		case SPHERICAL:
			return spheric(loc);
		case CYLINDRICAL_EQUIDISTANT:
			return cylindricalEquidistant(loc);
		case CYLINDRICAL:
			return cylindric(loc);
		case POLAR:
			return polar(loc);
		default:
			throw new JPARSECException("invalid projection.");
		}
	}

	private float[] project_position(float[] loc) throws JPARSECException
	{
		if (cylindrical) return cylindricalEquidistant(loc);

		// Without waiting for switch is slightly faster ...
		if (render.projection == PROJECTION.STEREOGRAPHICAL) return stereographic(loc);

		switch (render.projection)
		{
		case SPHERICAL:
			return spheric(loc);
		case CYLINDRICAL_EQUIDISTANT:
			return cylindricalEquidistant(loc);
		case CYLINDRICAL:
			return cylindric(loc);
		case POLAR:
			return polar(loc);
		default:
			throw new JPARSECException("invalid projection.");
		}
	}

	private float stxx, sxx, sxs, stxxTimesCenterX;
	private float out[] = new float[2];
	/** Set to true (default value) to create a new array with the location
	 * calculated for the projected object, and false to use a preallocated
	 * array, which should not be modified later. Setting to false is only
	 * possible when the array will not be modified later, and will perform
	 * faster. */
	public boolean createNewArrayWhenProjecting = true;
	/**
	 * Obtains position of an object using cylindrical equidistant projection.
	 * Used if field of view is lower than 30 deg.
	 *
	 * @param loc Location object with the position, in equatorial,
	 *        horizontal, galactic, or ecliptic coordinates.
	 * @return Array with (x, y) position in the screen.
	 */
	public float[] cylindricalEquidistant(LocationElement loc) {
		float x = (float) (loc.getLongitude() - render.centralLongitude);
		float y = (float) (loc.getLatitude() - render.centralLatitude);
		if (Math.abs(x) > Math.PI) x = x - (float) (Constant.TWO_PI * FastMath.sign(x));
		x *= FastMath.cosf(loc.getLatitude());

		float pox = (centerX - sxx * x);
		float poy = (centerY - rwf * y);

		if (render.poleAngle != 0.0)
		{
			float dx = pox - centerX;
			float dy = poy - centerY;

			float r = (float) FastMath.hypot(dx, dy); //Math.sqrt(dx * dx + dy * dy);
			float ang = (float) FastMath.atan2_accurate(dy, dx);

			pox = centerX + (r * FastMath.cosf(ang - render.poleAngle));
			poy = centerY + (r * FastMath.sinf(ang - render.poleAngle));
		}

		if (createNewArrayWhenProjecting) return new float[]{ pox, poy };
		out[0] = pox;
		out[1] = poy;
		return out;
	}
	private float[] cylindricalEquidistant(float[] loc) {
		float x = (float) (loc[0] - render.centralLongitude);
		float y = (float) (loc[1] - render.centralLatitude);
		if (Math.abs(x) > Math.PI) x = x - (float) (Constant.TWO_PI * FastMath.sign(x));
		x *= FastMath.cosf(loc[1]);

		float pox = (centerX - sxx * x);
		float poy = (centerY - rwf * y);

		if (render.poleAngle != 0.0)
		{
			float dx = pox - centerX;
			float dy = poy - centerY;

			float r = (float) FastMath.hypot(dx, dy); //Math.sqrt(dx * dx + dy * dy);
			float ang = (float) FastMath.atan2_accurate(dy, dx);

			pox = centerX + (r * FastMath.cosf(ang - render.poleAngle));
			poy = centerY + (r * FastMath.sinf(ang - render.poleAngle));
		}

		if (createNewArrayWhenProjecting) return new float[]{ pox, poy };
		out[0] = pox;
		out[1] = poy;
		return out;
	}

	/**
	 * Obtains position of an object by inverting the cylindric equidistant sky projection.
	 *
	 * @param pox X position.
	 * @param poy Y position.
	 * @return Coordinates in the sky, or null if the position cannot be calculated.
	 */
	public LocationElement invertCylindricEquidistant(float pox, float poy)
	{
		if (render.telescope.invertHorizontal) pox = render.width-1-pox;
		if (render.telescope.invertVertical) poy = render.height-1-poy;

		float pos0[] = new float[] {pox, poy};
		if (this.isInvalid(pos0)) return null;

		if (render.poleAngle != 0.0)
		{
			double dx = pox - centerX;
			double dy = poy - centerY;

			double r = FastMath.hypot(dx, dy); //Math.sqrt(dx * dx + dy * dy);
			double ang = FastMath.atan2_accurate(dy, dx);

			pox = (float) (centerX + (r * FastMath.cos(ang + render.poleAngle)));
			poy = (float) (centerY + (r * FastMath.sin(ang + render.poleAngle)));
		}

		float x = (centerX - pox) / sxx;
		float y = (centerY - poy) / rwf;

		double lat = y + render.centralLatitude;
		return new LocationElement(x / FastMath.cos(lat) + render.centralLongitude, lat, 1.0);
	}

	private double sincosLat[] = new double[2], sincosLon[] = new double[2];
	/**
	 * Obtains position of an object using stereographic sky projection.
	 *
	 * @param loc Location object with the position, in equatorial,
	 *        horizontal, galactic, or ecliptic coordinates.
	 * @return Array with (x, y) position in the screen.
	 */
	public float[] stereographic(LocationElement loc)
	{
		FastMath.sincos(loc.getLatitude(), false, sincosLat);
		FastMath.sincos(loc.getLongitude() - render.centralLongitude, false, sincosLon);

		double h = sincosLat[1] * sincosLon[1];
		double div = (1.0f + sin_lat0 * sincosLat[0] + cos_lat0 * h);
		if (div == 0) return Projection.INVALID_POSITION;

		float pox = (float) (centerX - stxxTimesCenterX * (sincosLat[1] * sincosLon[0] / div));
		float poy = (float) (centerY - (cos_lat0_times_sy * sincosLat[0] - sin_lat0_times_sy * h) / div);

		if (render.poleAngle != 0.0)
		{
			float dx = pox - centerX;
			float dy = poy - centerY;

			float r = (float) FastMath.hypot(dx, dy);
			float ang = (float) FastMath.atan2_accurate(dy, dx);

			pox = centerX + (r * FastMath.cosf(ang - render.poleAngle));
			poy = centerY + (r * FastMath.sinf(ang - render.poleAngle));
		}

		if (createNewArrayWhenProjecting) return new float[]{ pox, poy };
		out[0] = pox;
		out[1] = poy;
		return out;
	}
	private float[] stereographic(float[] loc)
	{
		FastMath.sincos(loc[1], false, sincosLat);
		FastMath.sincos(loc[0] - render.centralLongitude, false, sincosLon);

		double h = sincosLat[1] * sincosLon[1];
		double div = (1.0f + sin_lat0 * sincosLat[0] + cos_lat0 * h);
		if (div == 0) return Projection.INVALID_POSITION;

		float pox = (float) (centerX - stxxTimesCenterX * (sincosLat[1] * sincosLon[0] / div));
		float poy = (float) (centerY - (cos_lat0_times_sy * sincosLat[0] - sin_lat0_times_sy * h) / div);

		if (render.poleAngle != 0.0)
		{
			float dx = pox - centerX;
			float dy = poy - centerY;

			float r = (float) FastMath.hypot(dx, dy);
			float ang = (float) FastMath.atan2_accurate(dy, dx);

			pox = centerX + (r * FastMath.cosf(ang - render.poleAngle));
			poy = centerY + (r * FastMath.sinf(ang - render.poleAngle));
		}

		if (createNewArrayWhenProjecting) return new float[]{ pox, poy };
		out[0] = pox;
		out[1] = poy;
		return out;
	}

	/**
	 * Obtains position of an object using mercator sky projection.
	 *
	 * @param loc Location object with the position, in equatorial,
	 *        horizontal, galactic, or ecliptic coordinates.
	 * @return Array with (x, y) position in the screen.
	 */
	public float[] cylindric(LocationElement loc)
	{
		float x = (float) (loc.getLongitude() - render.centralLongitude);
		float y = (float) (loc.getLatitude() - render.centralLatitude);
		if (Math.abs(x) > Math.PI) x = x - (float) (Constant.TWO_PI) * FastMath.sign(x);

		float pox = (centerX - sxx * x);
		float poy = (centerY - rwf * y);

		if (render.poleAngle != 0.0)
		{
			float dx = pox - centerX;
			float dy = poy - centerY;
			float r = (float) FastMath.hypot(dx, dy); //Math.sqrt(dx * dx + dy * dy);
			float ang = (float) FastMath.atan2_accurate(dy, dx);
			pox = centerX + (r * FastMath.cosf(ang - render.poleAngle));
			poy = centerY + (r * FastMath.sinf(ang - render.poleAngle));
		}

		if (createNewArrayWhenProjecting) return new float[]{ pox, poy };
		out[0] = pox;
		out[1] = poy;
		return out;
	}
	private float[] cylindric(float[] loc)
	{
		float x = (float) (loc[0] - render.centralLongitude);
		float y = (float) (loc[1] - render.centralLatitude);
		if (Math.abs(x) > Math.PI) x = x - (float) (Constant.TWO_PI) * FastMath.sign(x);

		float pox = (centerX - sxx * x);
		float poy = (centerY - rwf * y);

		if (render.poleAngle != 0.0)
		{
			float dx = pox - centerX;
			float dy = poy - centerY;
			float r = (float) FastMath.hypot(dx, dy); //Math.sqrt(dx * dx + dy * dy);
			float ang = (float) FastMath.atan2_accurate(dy, dx);
			pox = centerX + (r * FastMath.cosf(ang - render.poleAngle));
			poy = centerY + (r * FastMath.sinf(ang - render.poleAngle));
		}

		if (createNewArrayWhenProjecting) return new float[]{ pox, poy };
		out[0] = pox;
		out[1] = poy;
		return out;
	}

	/**
	 * Obtains position of an object by inverting the cylindric sky projection.
	 *
	 * @param pox X position.
	 * @param poy Y position.
	 * @return Coordinates in the sky, or null if the position cannot be calculated.
	 */
	public LocationElement invertCylindric(float pox, float poy)
	{
		if (render.telescope.invertHorizontal) pox = render.width-1-pox;
		if (render.telescope.invertVertical) poy = render.height-1-poy;

		float pos0[] = new float[] {pox, poy};
		if (this.isInvalid(pos0)) return null;

		if (render.poleAngle != 0.0)
		{
			double dx = pox - centerX;
			double dy = poy - centerY;

			double r = FastMath.hypot(dx, dy); //Math.sqrt(dx * dx + dy * dy);
			double ang = FastMath.atan2_accurate(dy, dx);

			pox = (float) (centerX + (r * FastMath.cos(ang + render.poleAngle)));
			poy = (float) (centerY + (r * FastMath.sin(ang + render.poleAngle)));
		}

		float x = (centerX - pox) / sxx;
		float y = (centerY - poy) / rwf;

		return new LocationElement(x + render.centralLongitude, y + render.centralLatitude, 1.0);
	}

	/**
	 * Obtains position of an object using spheric sky projection.
	 *
	 * @param loc Location object with the position, in equatorial,
	 *        horizontal, galactic, or ecliptic coordinates.
	 * @return Array with (x, y) position in the screen.
	 */
	public float[] spheric(LocationElement loc)
	{
		float cos_lat = FastMath.cosf(loc.getLatitude());

		double dlon = loc.getLongitude() - render.centralLongitude;
		if (dlon < 0 || dlon > Constant.TWO_PI) dlon = Functions.normalizeRadians(dlon);

		float pox = (centerX * (1.0f - sxs * FastMath.sinf(dlon) * cos_lat));
		float poy = (centerY - sy * (FastMath.sinf(loc.getLatitude()) * cos_lat0 - cos_lat * sin_lat0 * FastMath.cosf(dlon)));

		if (render.poleAngle != 0.0)
		{
			float dx = pox - centerX;
			float dy = poy - centerY;
			float r = (float) FastMath.hypot(dx, dy); //Math.sqrt(dx * dx + dy * dy);
			float ang = (float) FastMath.atan2_accurate(dy, dx);
			pox = centerX + (r * FastMath.cosf(ang - render.poleAngle));
			poy = centerY + (r * FastMath.sinf(ang - render.poleAngle));
		}

		if (createNewArrayWhenProjecting) return new float[]{ pox, poy };
		out[0] = pox;
		out[1] = poy;
		return out;
	}
	private float[] spheric(float[] loc)
	{
		float cos_lat = FastMath.cosf(loc[1]);

		double dlon = loc[0] - render.centralLongitude;
		if (dlon < 0 || dlon > Constant.TWO_PI) dlon = Functions.normalizeRadians(dlon);

		float pox = (centerX * (1.0f - sxs * FastMath.sinf(dlon) * cos_lat));
		float poy = (centerY - sy * (FastMath.sinf(loc[1]) * cos_lat0 - cos_lat * sin_lat0 * FastMath.cosf(dlon)));

		if (render.poleAngle != 0.0)
		{
			float dx = pox - centerX;
			float dy = poy - centerY;
			float r = (float) FastMath.hypot(dx, dy); //Math.sqrt(dx * dx + dy * dy);
			float ang = (float) FastMath.atan2_accurate(dy, dx);
			pox = centerX + (r * FastMath.cosf(ang - render.poleAngle));
			poy = centerY + (r * FastMath.sinf(ang - render.poleAngle));
		}

		if (createNewArrayWhenProjecting) return new float[]{ pox, poy };
		out[0] = pox;
		out[1] = poy;
		return out;
	}

	/**
	 * Obtains position of an object by inverting the spherical sky projection.
	 *
	 * @param pox X position.
	 * @param poy Y position.
	 * @return Coordinates in the sky, or null if the position cannot be calculated.
	 */
	public LocationElement invertSpheric(float pox, float poy)
	{
		if (render.telescope.invertHorizontal) pox = render.width-1-pox;
		if (render.telescope.invertVertical) poy = render.height-1-poy;

		float pos0[] = new float[] {pox, poy};
		if (this.isInvalid(pos0)) return null;

		if (render.poleAngle != 0.0)
		{
			double dx = pox - centerX;
			double dy = poy - centerY;

			double r = FastMath.hypot(dx, dy); //Math.sqrt(dx * dx + dy * dy);
			double ang = FastMath.atan2_accurate(dy, dx);

			pox = (float) (centerX + (r * FastMath.cos(ang + render.poleAngle)));
			poy = (float) (centerY + (r * FastMath.sin(ang + render.poleAngle)));
		}

		double dx = pox - centerX, dy = centerY - poy;
		dx = -dx / (centerX * sxs);
		dy = -dy / sy;
		double tmp = (dx * dx + dy * dy);
		if (tmp > 1.0) return null;

		double dz = Math.sqrt(1.0 - tmp);
		double sinlat = FastMath.sin(render.centralLatitude), coslat = FastMath.cos(render.centralLatitude);
		tmp = dy * sinlat + dz * coslat;
		dy = dy * coslat - dz * sinlat;
		dz = tmp;
		double lat = FastMath.atan2_accurate(dx, dz);
		double lon = Functions.normalizeRadians(render.centralLongitude + lat);
		lat = Math.asin(-dy);

		return new LocationElement(lon, lat, 1.0);
	}

	/**
	 * Obtains position of an object using polar projection.
	 *
	 * @param loc Location object with the position, in equatorial,
	 *        horizontal, galactic, or ecliptic coordinates.
	 * @return Array with (x, y) position in the screen.
	 */
	public float[] polar(LocationElement loc) {
		float pox, poy;

		double offx = 0, offy = 1.0;
		if (render.telescope.invertHorizontal) offx += Math.PI;
		if (render.telescope.invertVertical) offy = -1.0;
		if (render.centralLatitude >= 0.0) {
			if (loc.getLatitude() < -this.horizon_elevation) return INVALID_POSITION;
			double r0 = -offy*sy * FastMath.sin(Constant.PI_OVER_TWO - render.centralLatitude);
			double r = -sy * FastMath.sin(loc.getLatitude() - Constant.PI_OVER_TWO);

			double dlon = loc.getLongitude() - render.centralLongitude + Math.PI + offx;
			if (dlon < 0 || dlon > Constant.TWO_PI) dlon = Functions.normalizeRadians(dlon);

			pox = (float) (-r * FastMath.sin(dlon) + centerX);
			poy = (float) (-r * FastMath.cos(dlon) + centerY + r0);
		} else {
			if (loc.getLatitude() > this.horizon_elevation) return INVALID_POSITION;
			double r0 = -offy*sy * FastMath.sin(-Constant.PI_OVER_TWO - render.centralLatitude);
			double	r = -sy * FastMath.sin(loc.getLatitude() + Constant.PI_OVER_TWO);

			double dlon = loc.getLongitude() - render.centralLongitude + offx + Math.PI;
			if (dlon < 0 || dlon > Constant.TWO_PI) dlon = Functions.normalizeRadians(dlon);

			pox = (float) (r * FastMath.sin(dlon) + centerX);
			poy = (float) (-r * FastMath.cos(dlon) + centerY + r0);
		}

		if (render.poleAngle != 0.0)
		{
			float dx = pox - centerX;
			float dy = poy - centerY;

			float r = (float) FastMath.hypot(dx, dy); //Math.sqrt(dx * dx + dy * dy);
			float ang = (float) FastMath.atan2_accurate(dy, dx);

			pox = centerX + (r * FastMath.cosf(ang - render.poleAngle));
			poy = centerY + (r * FastMath.sinf(ang - render.poleAngle));
		}

		if (createNewArrayWhenProjecting) return new float[]{ pox, poy };
		out[0] = pox;
		out[1] = poy;
		return out;
	}
	private float[] polar(float[] loc) {
		float pox, poy;

		double offx = 0, offy = 1.0;
		if (render.telescope.invertHorizontal) offx += Math.PI;
		if (render.telescope.invertVertical) offy = -1.0;
		if (render.centralLatitude >= 0.0) {
			if (loc[1] < -this.horizon_elevation) return INVALID_POSITION;
			double r0 = -offy*sy * FastMath.sin(Constant.PI_OVER_TWO - render.centralLatitude);
			double r = -sy * FastMath.sin(loc[1] - Constant.PI_OVER_TWO);

			double dlon = loc[0] - render.centralLongitude + Math.PI + offx;
			if (dlon < 0 || dlon > Constant.TWO_PI) dlon = Functions.normalizeRadians(dlon);

			pox = (float) (-r * FastMath.sin(dlon) + centerX);
			poy = (float) (-r * FastMath.cos(dlon) + centerY + r0);
		} else {
			if (loc[1] > this.horizon_elevation) return INVALID_POSITION;
			double r0 = -offy*sy * FastMath.sin(-Constant.PI_OVER_TWO - render.centralLatitude);
			double	r = -sy * FastMath.sin(loc[1] + Constant.PI_OVER_TWO);

			double dlon = loc[0] - render.centralLongitude + offx + Math.PI;
			if (dlon < 0 || dlon > Constant.TWO_PI) dlon = Functions.normalizeRadians(dlon);

			pox = (float) (r * FastMath.sin(dlon) + centerX);
			poy = (float) (-r * FastMath.cos(dlon) + centerY + r0);
		}

		if (render.poleAngle != 0.0)
		{
			float dx = pox - centerX;
			float dy = poy - centerY;

			float r = (float) FastMath.hypot(dx, dy); //Math.sqrt(dx * dx + dy * dy);
			float ang = (float) FastMath.atan2_accurate(dy, dx);

			pox = centerX + (r * FastMath.cosf(ang - render.poleAngle));
			poy = centerY + (r * FastMath.sinf(ang - render.poleAngle));
		}

		if (createNewArrayWhenProjecting) return new float[]{ pox, poy };
		out[0] = pox;
		out[1] = poy;
		return out;
	}

	/**
	 * Obtains position of an object by inverting the polar sky projection.
	 *
	 * @param pox X position.
	 * @param poy Y position.
	 * @return Coordinates in the sky, or null if the position cannot be calculated.
	 */
	public LocationElement invertPolar(float pox, float poy)
	{
		if (render.telescope.invertHorizontal) pox = render.width-1-pox;
		if (render.telescope.invertVertical) poy = render.height-1-poy;

		float pos0[] = new float[] {pox, poy};
		if (this.isInvalid(pos0)) return null;

		if (render.poleAngle != 0.0)
		{
			double dx = pox - centerX;
			double dy = poy - centerY;

			double r = FastMath.hypot(dx, dy); //Math.sqrt(dx * dx + dy * dy);
			double ang = FastMath.atan2_accurate(dy, dx);

			pox = (float) (centerX + (r * FastMath.cos(ang + render.poleAngle)));
			poy = (float) (centerY + (r * FastMath.sin(ang + render.poleAngle)));
		}

		double offx = 0;
		float offy = 1.0f;
		if (render.telescope.invertHorizontal) offx += Math.PI;
		if (render.telescope.invertVertical) offy = -1.0f;
		if (render.centralLatitude >= 0.0) {
			float r0 = -offy*sy * FastMath.sinf(Constant.PI_OVER_TWO - render.centralLatitude);
			poy = -(poy - r0 - centerY);
			pox = -(pox - centerX);
			double lon = FastMath.atan2_accurate(pox, poy);
			double r = pox / FastMath.sin(lon);
			double lat = Math.asin(-r / sy) + Constant.PI_OVER_TWO;
			if (Double.isNaN(lat) || lat < 0.0) return null;
			return new LocationElement(lon + render.centralLongitude - offx + Math.PI, lat, 1.0);
		} else {
			float r0 = -offy*sy * FastMath.sinf(-Constant.PI_OVER_TWO - render.centralLatitude);
			poy = -(poy - r0 - centerY);
			pox = pox - centerX;
			double lon = Math.PI+FastMath.atan2_accurate(pox, poy);
			double r = pox / FastMath.sin(lon);
			double lat = Math.asin(-r / sy) - Constant.PI_OVER_TWO;
			if (Double.isNaN(lat) || lat > 0.0) return null;
			return new LocationElement(lon + render.centralLongitude - Math.PI - offx, lat, 1.0);
		}
	}

	/**
	 * Returns ecliptic position of the central position.
	 * @return Ecliptic position of central longitude and latitude.
	 * @throws JPARSECException If an error occurs.
	 */
	public LocationElement getEclipticPositionOfRendering() throws JPARSECException
	{
		LocationElement loc;
		boolean fast = false;
		switch (this.render.coordinateSystem)
		{
		case GALACTIC:
		case SUPERGALACTIC:
			loc = loc0;
			if (render.coordinateSystem == COORDINATE_SYSTEM.SUPERGALACTIC)
				loc = CoordinateSystem.supergalacticToGalactic(loc0);
			loc = CoordinateSystem.galacticToEquatorial(loc, eph.getEpoch(jd), fast);
			if (obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != TARGET.NOT_A_PLANET) loc = getPositionFromEarth(loc, true);
			loc = CoordinateSystem.equatorialToEcliptic(loc, this.obliquity, fast);
			break;
		case HORIZONTAL:
			loc = CoordinateSystem.horizontalToEquatorial(loc0, this.ast, this.obs.getLatitudeRad(), fast);
			if (obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != TARGET.NOT_A_PLANET) loc = getPositionFromEarth(loc, true);
			loc = CoordinateSystem.equatorialToEcliptic(loc, this.obliquity, fast);
			break;
		case EQUATORIAL:
			loc = loc0;
			if (obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != TARGET.NOT_A_PLANET) loc = getPositionFromEarth(loc, true);
			loc = CoordinateSystem.equatorialToEcliptic(loc, this.obliquity, fast);
			break;
		default:
			return loc0;
		}

		return loc;
	}

	/**
	 * Returns equatorial position of the central position.
	 * @return Equatorial position of central longitude and latitude.
	 * @throws JPARSECException If an error occurs.
	 */
	public LocationElement getEquatorialPositionOfRendering() throws JPARSECException
	{
		if (render.coordinateSystem == CoordinateSystem.COORDINATE_SYSTEM.EQUATORIAL) return loc0;

		boolean fast = false;
		switch (this.render.coordinateSystem)
		{
		case GALACTIC:
		case SUPERGALACTIC:
			LocationElement loc = loc0;
			if (render.coordinateSystem == COORDINATE_SYSTEM.SUPERGALACTIC)
				loc = CoordinateSystem.supergalacticToGalactic(loc0);
			loc = CoordinateSystem.galacticToEquatorial(loc, eph.getEpoch(jd), fast);
			if (obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != TARGET.NOT_A_PLANET) loc = getPositionFromBody(loc, true);
			return loc;
		case HORIZONTAL:
			return CoordinateSystem.horizontalToEquatorial(loc0, this.ast, this.obs.getLatitudeRad(), fast);
		case ECLIPTIC:
			loc = CoordinateSystem.eclipticToEquatorial(loc0, this.obliquity, fast);
			if (obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != TARGET.NOT_A_PLANET) loc = getPositionFromBody(loc, true);
			return loc;
		default:
			return loc0;
		}
	}

	/**
	 * Returns equatorial position of the zenith.
	 * @return Equatorial position of zenith.
	 * @throws JPARSECException If an error occurs.
	 */
	public LocationElement getEquatorialPositionOfZenith() throws JPARSECException
	{
		if (obs.getMotherBody() == TARGET.NOT_A_PLANET) return null;
		return CoordinateSystem.horizontalToEquatorial(new LocationElement(0.0, Constant.PI_OVER_TWO, 1.0), this.ast, this.obs.getLatitudeRad(), true);
	}

	/**
	 * Returns galactic position of the central position.
	 * @return Galactic position of central longitude and latitude.
	 * @throws JPARSECException If an error occurs.
	 */
	public LocationElement getGalacticPositionOfRendering() throws JPARSECException
	{
		boolean fast = false; // <5" of difference
		LocationElement loc0 = this.loc0.clone();
		switch (this.render.coordinateSystem)
		{
		case EQUATORIAL:
			if (obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != TARGET.NOT_A_PLANET) {
				loc0 = getPositionFromEarth(loc0, fast);
			}

			LocationElement locJ2000 = LocationElement.parseRectangularCoordinates(Precession.precessToJ2000(jd, LocationElement.parseLocationElement(loc0), eph));
			return CoordinateSystem.equatorialToGalactic(locJ2000, fast);
		case HORIZONTAL:
			LocationElement loc = CoordinateSystem.horizontalToEquatorial(loc0, this.ast, this.obs.getLatitudeRad(), fast);
			if (obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != TARGET.NOT_A_PLANET)
				loc = getPositionFromEarth(loc, fast);

			locJ2000 = LocationElement.parseRectangularCoordinates(Precession.precessToJ2000(jd, LocationElement.parseLocationElement(loc), eph));
			return CoordinateSystem.equatorialToGalactic(locJ2000, fast);
		case ECLIPTIC:
			loc = CoordinateSystem.eclipticToEquatorial(loc0, this.obliquity, fast);
			locJ2000 = LocationElement.parseRectangularCoordinates(Precession.precessToJ2000(jd, LocationElement.parseLocationElement(loc), eph));
			return CoordinateSystem.equatorialToGalactic(locJ2000, fast);
		default:
			return loc0;
		}
	}

	/**
	 * Returns supergalactic position of the central position.
	 * @return Supergalactic position of central longitude and latitude.
	 * @throws JPARSECException If an error occurs.
	 */
	public LocationElement getSuperGalacticPositionOfRendering() throws JPARSECException
	{
		LocationElement loc = getGalacticPositionOfRendering();
		return CoordinateSystem.galacticToSupergalactic(loc);
	}
	
	/**
	 * Returns horizontal position of the central position.
	 * @return Geometric azimuth and elevation of central position of rendering.
	 * @throws JPARSECException If an error occurs.
	 */
	public LocationElement getHorizontalPositionOfRendering() throws JPARSECException
	{
		boolean fast = false; // <5" of difference
		LocationElement loc0 = this.loc0.clone();
		switch (this.render.coordinateSystem)
		{
		case EQUATORIAL:
			if (obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != TARGET.NOT_A_PLANET)
				loc0 = getPositionFromEarth(loc0, fast);

			return CoordinateSystem.equatorialToHorizontal(loc0, ast, obs, eph, false, fast);
		case GALACTIC:
		case SUPERGALACTIC:
			LocationElement loc = loc0;
			if (render.coordinateSystem == COORDINATE_SYSTEM.SUPERGALACTIC)
				loc = CoordinateSystem.supergalacticToGalactic(loc0);
			loc = CoordinateSystem.galacticToEquatorial(loc, jd, fast);
			if (obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != TARGET.NOT_A_PLANET)
				loc = getPositionFromEarth(loc, fast);

			return CoordinateSystem.equatorialToHorizontal(loc0, ast, obs, eph, false, fast);
		case ECLIPTIC:
			loc = CoordinateSystem.eclipticToEquatorial(loc0, this.obliquity, fast);
			if (obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != TARGET.NOT_A_PLANET)
				loc0 = getPositionFromEarth(loc0, fast);
			return CoordinateSystem.equatorialToHorizontal(loc0, ast, obs, eph, false, fast);
		default:
			return loc0;
		}
	}

	/**
	 * Returns the location of the central position in the current coordinate system.
	 * @return Central longitude and latitude.
	 * @throws JPARSECException If an error occurs.
	 */
	public LocationElement getPositionOfRendering() throws JPARSECException
	{
		return loc0;
	}

	/**
	 * Returns equatorial position.
	 * @param loc Input position in current coordinate system.
	 * @param fast True for an approximate but faster calculation.
	 * @return Equatorial position.
	 * @throws JPARSECException If an error occurs.
	 */
	public LocationElement toEquatorialPosition(LocationElement loc, boolean fast) throws JPARSECException
	{
		switch (this.render.coordinateSystem)
		{
		case GALACTIC:
		case SUPERGALACTIC:
			LocationElement out = loc;
			if (render.coordinateSystem == COORDINATE_SYSTEM.SUPERGALACTIC)
				out = CoordinateSystem.supergalacticToGalactic(loc);
			out = CoordinateSystem.galacticToEquatorial(out, eph.getEpoch(jd), false);
			if (obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != TARGET.NOT_A_PLANET) out = getPositionFromBody(out, true);
			return out;
		case HORIZONTAL:
			if (render.drawSkyCorrectingLocalHorizon && obs.getMotherBody() == TARGET.EARTH && eph.isTopocentric ) {
				LocationElement locIn = new LocationElement(
						loc.getLongitude(), Ephem.getGeometricElevation(eph, obs, loc.getLatitude()), 1.0);
				return CoordinateSystem.horizontalToEquatorial(locIn, this.ast, this.obs.getLatitudeRad(), fast);
			} else {
				return CoordinateSystem.horizontalToEquatorial(loc, this.ast, this.obs.getLatitudeRad(), fast);
			}
		case ECLIPTIC:
			out = CoordinateSystem.eclipticToEquatorial(loc, this.obliquity, fast);
			if (obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != TARGET.NOT_A_PLANET) out = getPositionFromBody(out, true);
			return out;
		case EQUATORIAL:
			break;
		}

		return loc.clone();
	}

	/**
	 * Gets the north angle at a specific equatorial position.
	 * @param loc00 Position. If null the angle will be calculated for the current central
	 * position of the rendering.
	 * @param isEquatorial True if the input position is in equatorial coordinates, false
	 * if it is (possibly) in other coordinate system.
	 * @param passToEarth True to get north angle considering that the input position should
	 * be converted to that from Earth, in case the observer is located in another planet.
	 * @return The angle towards north in radians.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getNorthAngleAt(LocationElement loc00, boolean isEquatorial, boolean passToEarth) throws JPARSECException
	{
		this.disableCorrectionOfLocalHorizon();

		float pos0[] = new float[2];
		LocationElement loc = null;
		LocationElement locEq = null;
		if (loc00 == null) {
			locEq = this.getEquatorialPositionOfRendering();
			loc = this.getPositionOfRendering();
		} else {
			if (!isEquatorial) {
				loc = loc00.clone();
				locEq = this.toEquatorialPosition(loc00, fastCalc);
			} else {
				locEq = loc00.clone();
				loc = getApparentLocationInSelectedCoordinateSystem(locEq, false, fastCalc, 0);
			}
		}

		boolean newArray = createNewArrayWhenProjecting;
		createNewArrayWhenProjecting = true;
		pos0 = project_position(loc);

		if (obs.getMotherBody() == TARGET.EARTH && field < Constant.DEG_TO_RAD && render.coordinateSystem == COORDINATE_SYSTEM.HORIZONTAL) {
			double angh = ast - locEq.getLongitude();
			double sinlat = Math.sin(obs.getLatitudeRad());
			double coslat = Math.cos(obs.getLatitudeRad());
			double sindec = Math.sin(locEq.getLatitude()), cosdec = Math.cos(locEq.getLatitude());
			double y = Math.sin(angh);
			double x = (sinlat / coslat) * cosdec - sindec * Math.cos(angh);
			double p = 0.0;
			if (x != 0.0)
			{
				p = Math.atan2(y, x);
			} else
			{
				p = (y / Math.abs(y)) * Constant.PI_OVER_TWO;
			}

			float center[] = project_position(new LocationElement(loc.getLongitude(), Math.min(loc.getLatitude()+10*Constant.DEG_TO_RAD, Constant.PI_OVER_TWO), 1.0));
			float dx = pos0[0] - center[0];
			float dy = pos0[1] - center[1];
			double ang = FastMath.atan2_accurate(-dy, dx);

			this.enableCorrectionOfLocalHorizon();
			createNewArrayWhenProjecting = newArray;
			return p-ang;
		}

		if (pos0 == null) {
			this.enableCorrectionOfLocalHorizon();
			JPARSECException.addWarning("Could not calculate where is north. Trace: "+JPARSECException.getCurrentTrace());
			createNewArrayWhenProjecting = newArray;
			return 0;
		}

		if (passToEarth && obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != TARGET.NOT_A_PLANET) locEq = getPositionFromEarth(locEq, true);
		double lat = locEq.getLatitude();

		double DEC = Math.max(this.getField() * 0.1, 0.1 * Constant.DEG_TO_RAD);
		DEC += lat;
		if (DEC > Constant.PI_OVER_TWO) DEC = Constant.PI_OVER_TWO;
		locEq.setLatitude(DEC);
		if (passToEarth && obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != TARGET.NOT_A_PLANET) locEq = getPositionFromBody(locEq, true);
		loc = getApparentLocationInSelectedCoordinateSystem(locEq, false, fastCalc, 0);
		float[] pos1 = project_position(loc);

		double off = Math.PI;
		if (pos1 == null) {
			DEC = -Math.max(this.getField() * 0.1, 0.1 * Constant.DEG_TO_RAD);
			DEC += lat;
			if (DEC < -Constant.PI_OVER_TWO) DEC = -Constant.PI_OVER_TWO;
			locEq.setLatitude(DEC);
			if (passToEarth && obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != TARGET.NOT_A_PLANET) locEq = getPositionFromBody(locEq, true);
			loc = getApparentLocationInSelectedCoordinateSystem(locEq, false, fastCalc, 0);
			pos1 = project_position(loc);
			off = 0;

			if (pos1 == null) {
				this.enableCorrectionOfLocalHorizon();
				JPARSECException.addWarning("Could not calculate where is north. Trace: "+JPARSECException.getCurrentTrace());
				createNewArrayWhenProjecting = newArray;
				return 0;
			}
		}
		this.enableCorrectionOfLocalHorizon();

		double ang = FastMath.atan2_accurate(pos1[1] - pos0[1], pos1[0] - pos0[0]) + off;
		createNewArrayWhenProjecting = newArray;
		return ang;
	}

	/**
	 * Gets the north angle at a specific equatorial position.
	 * @param loc00 Position. If null the angle will be calculated for the current central
	 * position of the rendering.
	 * @param isEquatorial True if the input position is in equatorial coordinates, false
	 * if it is (possibly) in other coordinate system.
	 * @return The angle towards zenith in radians.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getCenitAngleAt(LocationElement loc00, boolean isEquatorial) throws JPARSECException
	{
		this.disableCorrectionOfLocalHorizon();

		float pos0[] = new float[2];
		LocationElement loc = null;
		LocationElement locH = null;
		if (loc00 == null) {
			locH = this.getEquatorialPositionOfRendering();
			loc = this.getPositionOfRendering();
		} else {
			if (!isEquatorial) {
				loc = loc00.clone();
				locH = this.toEquatorialPosition(loc00, true);
			} else {
				locH = loc00.clone();
				loc = getApparentLocationInSelectedCoordinateSystem(locH, false, fastCalc, 0);
			}
		}
		locH = this.getApparentLocationInSelectedCoordinateSystem(locH, false, fastCalc, COORDINATE_SYSTEM.HORIZONTAL, 0);

		boolean newArray = createNewArrayWhenProjecting;
		createNewArrayWhenProjecting = true;
		pos0 = project_position(loc);

		double dH = Math.max(this.getField() * 0.1, 0.1 * Constant.DEG_TO_RAD);
		dH += locH.getLatitude();
		if (dH > Constant.PI_OVER_TWO) dH = Constant.PI_OVER_TWO;
		locH.setLatitude(dH);
		LocationElement locEq = CoordinateSystem.horizontalToEquatorial(locH, ast, obs.getLatitudeRad(), fastCalc);
		loc = getApparentLocationInSelectedCoordinateSystem(locEq, false, fastCalc, 0);
		float[] pos1 = project_position(loc);

		this.enableCorrectionOfLocalHorizon();
		if (pos0 == null || pos1 == null) {
			JPARSECException.addWarning("Could not calculate where is north. Trace: "+JPARSECException.getCurrentTrace());
			createNewArrayWhenProjecting = newArray;
			return 0;
		}

		double dh = 1.0, dv = 1.0;
		if (render.telescope.invertHorizontal) dh = -1;
		if (render.telescope.invertVertical) dv = -1;
		double ang = FastMath.atan2_accurate(-dh*(pos1[1] - pos0[1]), dv*(pos1[0] - pos0[0])) + Math.PI;
		createNewArrayWhenProjecting = newArray;
		return ang;
	}

	/**
	 * Sets a new direction for the pole in the current projection.
	 * This overrides the initial value in the {@linkplain SkyRenderElement} object.
	 * @param ang The new angle, in radians.
	 */
	public void setPoleAngle(double ang) {
		render.poleAngle = (float) ang;
	}

	/**
	 * Obtains position of an object by inverting the stereographic sky projection.
	 *
	 * @param pox X position.
	 * @param poy Y position.
	 * @return Coordinates in the sky, or null if the position cannot be calculated.
	 * @throws JPARSECException If an error occurs.
	 */
	public LocationElement invertStereographic(float pox, float poy)
	throws JPARSECException {
		if (render.telescope.invertHorizontal) pox = render.width-1-pox;
		if (render.telescope.invertVertical) poy = render.height-1-poy;

		float pos0[] = new float[] {pox, poy};
		if (this.isInvalid(pos0)) return null;

		if (render.poleAngle != 0.0)
		{
			double dx = pox - centerX;
			double dy = poy - centerY;

			double r = FastMath.hypot(dx, dy); //Math.sqrt(dx * dx + dy * dy);
			double ang = FastMath.atan2_accurate(dy, dx);

			pox = (float) (centerX + (r * FastMath.cos(ang + render.poleAngle)));
			poy = (float) (centerY + (r * FastMath.sin(ang + render.poleAngle)));
 		}

		pox = -(pox - centerX);
		poy = -(poy - centerY);
		if (render.coordinateSystem == COORDINATE_SYSTEM.HORIZONTAL) poy = -poy;
		double rho = FastMath.hypot(pox, poy), R = stxxTimesCenterX * 0.5;

		if (fastCalc) {
			double c = 2 * FastMath.atan2_accurate(rho * 0.5, R);
			double sinc = FastMath.sin(c), cosc = FastMath.cos(c);
			double phi = render.centralLatitude;
			if (rho > 0) phi = FastMath.asin(cosc * sin_lat0 + poy * sinc * cos_lat0 / rho);
			double lam = render.centralLongitude + FastMath.atan2_accurate(pox * sinc, rho * cos_lat0 * cosc - poy * sin_lat0 * sinc);

			LocationElement out = new LocationElement(lam, phi, 1.0);
			return out;
		}

		double c = 2 * Math.atan2(rho * 0.5, R);
		double sinc = FastMath.sin(c), cosc = FastMath.cos(c);
		double phi = render.centralLatitude;
		if (rho > 0) phi = FastMath.asin(cosc * sin_lat0 + poy * sinc * cos_lat0 / rho);
		double lam = render.centralLongitude + Math.atan2(pox * sinc, rho * cos_lat0 * cosc - poy * sin_lat0 * sinc);

		LocationElement out = new LocationElement(lam, phi, 1.0);
		return out;
	}

	/**
	 * Transforms the equatorial position of a body as seen from Earth to the location
	 * as seen from a given body, rotating to account for the direction of its north
	 * pole of rotation.
	 * @param loc Equatorial position from Earth.
	 * @param fast True for approximate but fast computation.
	 * @return The new location.
	 * @throws JPARSECException If an error occurs.
	 */
	public LocationElement getPositionFromBody(LocationElement loc, boolean fast) throws JPARSECException {
		if (obs.getMotherBody() != TARGET.NOT_A_PLANET && obs.getMotherBody() != TARGET.EARTH) {

			if (!fast)
				return new LocationElement(LocationElement.getPositionAngle(np, loc), Constant.PI_OVER_TWO - LocationElement.getAngularDistance(np, loc), loc.getRadius());
			return new LocationElement(LocationElement.getApproximatePositionAngle(np, loc), Constant.PI_OVER_TWO - LocationElement.getApproximateAngularDistance(np, loc), loc.getRadius());
		}

		return loc;
	}

	/**
	 * Transforms the equatorial position of a body as seen from another body to the location
	 * as seen from Earth, de-rotating to account for the direction of the north
	 * pole of rotation of the mother (original) body. This method uses IAU resolutions, which
	 * provides an acceptable accuracy.
	 * @param loc Equatorial position from a given body.
	 * @param fast True for approximate but fast computation.
	 * @return The new location from Earth.
	 * @throws JPARSECException If an error occurs.
	 */
	public LocationElement getPositionFromEarth(LocationElement loc, boolean fast) throws JPARSECException {
		if (obs.getMotherBody() != TARGET.NOT_A_PLANET && obs.getMotherBody() != TARGET.EARTH) {
			LocationElement np = new LocationElement(0.0, this.np.getLatitude(), 1.0);
			//LocationElement np = Ephem.getPositionFromBody(this.np, time, obs, eph);
			//np.setLatitude(this.np.getLatitude());


			if (!fast)
				return new LocationElement(LocationElement.getPositionAngle(np, loc) - (np.getLongitude() - this.np.getLongitude()), Constant.PI_OVER_TWO - LocationElement.getAngularDistance(np, loc), loc.getRadius());
			return new LocationElement(LocationElement.getApproximatePositionAngle(np, loc) - (np.getLongitude() - this.np.getLongitude()), Constant.PI_OVER_TWO - LocationElement.getApproximateAngularDistance(np, loc), loc.getRadius());
		}

		return loc;
	}
}
