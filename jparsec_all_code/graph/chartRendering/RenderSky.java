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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import jparsec.astronomy.Constellation;
import jparsec.astronomy.CoordinateSystem;
import jparsec.astronomy.CoordinateSystem.COORDINATE_SYSTEM;
import jparsec.astronomy.Star;
import jparsec.astronomy.TelescopeElement;
import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.EphemerisElement.ALGORITHM;
import jparsec.ephem.EphemerisElement.COORDINATES_TYPE;
import jparsec.ephem.EphemerisElement.FRAME;
import jparsec.ephem.EphemerisElement.REDUCTION_METHOD;
import jparsec.ephem.Functions;
import jparsec.ephem.IAU2006;
import jparsec.ephem.Nutation;
import jparsec.ephem.Obliquity;
import jparsec.ephem.PhysicalParameters;
import jparsec.ephem.Precession;
import jparsec.ephem.RiseSetTransit;
import jparsec.ephem.Target;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.event.MainEvents;
import jparsec.ephem.event.MoonEvent;
import jparsec.ephem.event.MoonEvent.EVENT_DEFINITION;
import jparsec.ephem.moons.MoonEphem;
import jparsec.ephem.moons.MoonEphemElement;
import jparsec.ephem.planets.EphemElement;
import jparsec.ephem.planets.OrbitEphem;
import jparsec.ephem.planets.OrbitalElement;
import jparsec.ephem.planets.PlanetEphem;
import jparsec.ephem.probes.SDP4_SGP4;
import jparsec.ephem.probes.SatelliteEphem;
import jparsec.ephem.probes.SatelliteEphemElement;
import jparsec.ephem.probes.SatelliteOrbitalElement;
import jparsec.ephem.probes.Spacecraft;
import jparsec.ephem.stars.StarElement;
import jparsec.ephem.stars.StarEphem;
import jparsec.ephem.stars.StarEphemElement;
import jparsec.graph.DataSet;
import jparsec.graph.JPARSECStroke;
import jparsec.graph.chartRendering.Graphics.ANAGLYPH_COLOR_MODE;
import jparsec.graph.chartRendering.Graphics.FONT;
import jparsec.graph.chartRendering.Projection.PROJECTION;
import jparsec.graph.chartRendering.SkyRenderElement.COLOR_MODE;
import jparsec.graph.chartRendering.SkyRenderElement.CONSTELLATION_CONTOUR;
import jparsec.graph.chartRendering.SkyRenderElement.FAST_LINES;
import jparsec.graph.chartRendering.SkyRenderElement.HORIZON_TEXTURE;
import jparsec.graph.chartRendering.SkyRenderElement.LEYEND_POSITION;
import jparsec.graph.chartRendering.SkyRenderElement.MILKY_WAY_TEXTURE;
import jparsec.graph.chartRendering.SkyRenderElement.REALISTIC_STARS;
import jparsec.graph.chartRendering.SkyRenderElement.SUPERIMPOSED_LABELS;
import jparsec.io.FileFormatElement;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.io.ReadFile.FORMAT;
import jparsec.io.ReadFormat;
import jparsec.io.UnixSpecialCharacter;
import jparsec.io.WriteFile;
import jparsec.math.Constant;
import jparsec.math.FastMath;
import jparsec.math.Interpolation;
import jparsec.math.matrix.Matrix;
import jparsec.observer.ExtraterrestrialObserverElement;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.AstroDate;
import jparsec.time.DateTimeOps;
import jparsec.time.SiderealTime;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.time.TimeScale;
import jparsec.time.calendar.Calendar;
import jparsec.util.Configuration;
import jparsec.util.DataBase;
import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Logger.LEVEL;
import jparsec.util.Translate;
import jparsec.util.Translate.LANGUAGE;
import jparsec.vo.GeneralQuery;
import jparsec.vo.GeneralQuery.SKYVIEW_COORDINATE;
import jparsec.vo.GeneralQuery.SKYVIEW_INTENSITY_SCALE;
import jparsec.vo.GeneralQuery.SKYVIEW_LUT_TABLE;
import jparsec.vo.GeneralQuery.SKYVIEW_PROJECTION;
import jparsec.vo.GeneralQuery.SKYVIEW_SURVEY;

/**
 * A class for sky rendering
 * <P>
 * This class performs sky rendering operations. The first step is to configure
 * the render process with the necessary input objects. Rendering is performed
 * using a sequence of high precision ephemeris with very optimized algorithms,
 * allowing to read, calculate, and accurately draw thousands of different kind
 * of objects in a fraction of second for any given date and place.
 * <P>
 * Stars are rendered after calculating their apparent positions. Limiting
 * magnitude is 10.0, but only stars up to
 * 6.5 will be drawn, unless the scale is above 10 px/degree. In this
 * case the limiting magnitude will be 7.5. If the scale is above 20 px/degree,
 * all the stars up to 10.0 (limiting magnitude of the catalog used, adapted from
 * Sky2000 Master Catalog) will be drawn. If the limiting magnitude is fainter, then
 * those stars will be drawn using UCAC4 catalog if Internet connection is
 * available. It is not recommended to set a limiting magnitude fainter than 10, unless you really need
 * it. The limiting magnitude for stars is
 * customizable in one of the parameters of the {@linkplain SkyRenderElement} object,
 * if you prefer to permanently reduce the number of stars shown. The same happens
 * with other objects like comets, asteroids, and deep sky objects, but if the
 * scale is above 20 px/degree, then all the objects will be drawn, until the
 * selected limiting magnitude for these objects, which is by default much
 * bigger than the limiting magnitude for stars. So until 20 px/degree the
 * same limiting magnitude (6.5 or 7.5) exist for every usual object, unless you
 * select a lower value in the corresponding cases. Labels for deep sky objects
 * are shown only for scales above 10 px/degree. The user may expect the
 * possibility of showing deep sky objects with apparent magnitudes below 7.5
 * for scales below 20 px/degree. This can be achieved setting a
 * given limiting magnitude for objects (&gt;7.5), 10 for instance.
 * In this case, every object up to 10.0 magnitude (or the selected value) will
 * be visible, but not any other fainter than this. This allow showing more
 * objects without making a mess in the image with too many ones. Of course,
 * this is an important question that an hypothetical user should take into
 * account depending on different considerations.
 * <P>
 * In addition to comets and asteroids, trans-Neptunian objects (treated as
 * asteroids), space probes, and artificial satellites are also supported. They
 * will be drawn if the scale is above 10 px/degree, with no limiting
 * magnitude considerations in space probes and artificial satellites. If an
 * object is occulted, or any satellite is eclipsed, it will not be drawn. Comets
 * and asteroids will be drawn if the reference time of orbital elements is less
 * than a century away from the current time. Comet tails are shown as if they
 * should be visible if they were extended up to 20 000 000 km. For space probes
 * and artificial satellites, considerations about active times of the object
 * are taken into account.
 * <P>
 * This sky rendering class also works as a planet renderer. Planets are drawn
 * as symbols when the disk is not
 * visible, and in the same situation, the Sun, the Moon, space probes and
 * artificial satellites are drawn as icons if textures are enabled.
 * <P>
 * Labels for natural satellites are drawn only for fields of view below one degree. Apparent
 * sizes of stars depends on magnitude, and are correlated with sizes of natural
 * satellites when they are not resolved as discs.
 * <P>
 * The circle representing the angular field of view of the instrument
 * (telescope, objective) will be shown only if the field is below 50 degrees.
 * <P>
 * Apparent sky can be rendered using horizontal coordinates and any projection
 * type. If you want to occult the sky below the horizon you can set the proper
 * field to false. Only in this case, the method will
 * automatically correct elevation to apparent (refraction) and for depression
 * of the horizon.
 * <P>
 * For dates outside interval -3000 to +3000 in years, no Solar System object
 * will be shown. There are no explicit limiting dates for sky rendering when the
 * precession algorithm by Vondrak 2011 is used, but it is not recommended to go more
 * than 100 000 years into the past or future.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class RenderSky
{
	private static final double deg_0_5 = (0.5 * Constant.DEG_TO_RAD);
	private static final double deg_0_05 = (0.05 * Constant.DEG_TO_RAD);
	private static final double deg_20 = (20 * Constant.DEG_TO_RAD);
	private static final double deg_0_6 = (0.6 * Constant.DEG_TO_RAD);
	private static final double deg_10 = (10 * Constant.DEG_TO_RAD);
	private static final double deg_0_017 = (Constant.DEG_TO_RAD / 60.0);

	/** This flag is true when the field of view is forced to a given value. */
	private boolean forcedFOV = false;

	/**
	 * True to report to console the results of the ephemerides of minor objects.
	 * True by default.
	 */
	public boolean ReportEphemToConsole = false;
	/**
	 * True (default value) to save visible objects to an array to later allow
	 * search and identification based on screen position. False will save memory.
	 */
	public boolean SaveObjectsToAllowSearch = true;
	/**
	 * True (default value) to render stars in fake (AWT) mode instead of using image
	 * textures when rendering is performed to external graphics. True allows to render
	 * stars in vector graphics (PDF output) and to reduce file size a lot.
	 */
	public boolean FakeStarsForExternalGraphics = true;

	/**
	 * Sky render object that holds render parameters. It is hold here as public to
	 * simplyfy the access to underlying information, but be careful not to change it
	 * unless you later call to methods like {@linkplain #forceUpdateFast()} or {@linkplain #resetLeyend(boolean)}.
	 */
	public SkyRenderElement render;

	private String[] labelsName = new String[0];
	private String[] labelsAxesNameX = new String[0];
	private String[] labelsAxesNameY = new String[0];
	private ArrayList<Object[]> labels = new ArrayList<Object[]>();
	private ArrayList<Object[]> labelsAxesX = new ArrayList<Object[]>();
	private ArrayList<Object[]> labelsAxesY = new ArrayList<Object[]>();
	private Rectangle rec = new Rectangle();
	private String threadID = null;

	/**
	 * A list to hold rendered planets. Planetographic coordinates can be
	 * obtained after any render using this list.
	 */
	public ArrayList<Object> planets;

	private ReadFile re_star = null;
	private double lst = 0.0, baryc[] = null;
	int hugeFactor = 0;
	boolean firstTime = true;

	// FIXME: Note the use of these constants require that the indexes of the data in 
	// the DataBase must be constant. This is not the case when using several threads, so
	// the use of these values is both a problem for database integrity and a limitation 
	// for multithreading. Multiple threads are not supported anyway in sky rendering. 
	// These constants are used to improve performance and avoid searching the DataBase 
	// for the corresponding identifiers all the time
	private int db_conlin = -1, db_starNames = -1, db_starNames2 = -1, db_milkyWay = -1;
	private int db_nebula = -1, db_conlim = -1, db_connom = -1, db_objects = -1, db_raline = -1;
	private int db_decline = -1, db_horizonLine = -1, db_eclipticLine = -1, db_minorObjects = -1;
	private int db_deepSkyTextures = -1, db_sncat = -1, db_sunSpot = -1, db_faintStars = -1;
	private int db_transEphem = -1, db_asterEphem = -1, db_cometEphem = -1, db_probeEphem = -1;
	private int db_satEphem = -1, db_neoEphem = -1, db_meteor = -1, db_novae = -1;

	/**
	 * The set of different object types when rendering the sky.
	 */
	public enum OBJECT {
		/** ID constant for a star in a search. */
		STAR,
		/** ID constant for a planet in a search.Sun, Moon, and Pluto are included. */
		PLANET,
		/** ID constant for a comet in a search. */
		COMET,
		/** ID constant for a NEO in a search. */
		NEO,
		/** ID constant for an asteroid in a search. */
		ASTEROID,
		/** ID constant for a transneptunian object in a search. */
		TRANSNEPTUNIAN,
		/** ID constant for a deep sky object in a search. */
		DEEPSKY,
		/** ID constant for a probe in a search. */
		PROBE,
		/** ID constant for an artificial satellite in a search. */
		ARTIFICIAL_SATELLITE,
		/** ID constant for a supernova in a search. */
		SUPERNOVA,
		/** ID constant for a nova in a search. */
		NOVA,
		/** ID constant for a meteor shower in a search. */
		METEOR_SHOWER;

		/** Set to true to show comet's tail in trajectories. Default value is false. */
		public boolean showCometTail = false;
		
		/**
		 * Indexes for translating the different enum values to string.
		 */
		private static final int[] TRANS = new int[] {
	    	79, 878, 74, 1275, 73, 1003, 972, 76, 78, 877, 1304, 1024
	    };

		/**
		 * A string representation of the object type for the current language.
		 * @return String representation.
		 */
		public String toStringTranslated() {
			return Translate.translate(TRANS[this.ordinal()]);
		}
	}

	private EphemElement[] majorObjects = null;
	private Object[] majorObjectsSats = null;

	private static String imgs[] = new String[0];
	private static ArrayList<Object> images = new ArrayList<Object>();


	private Projection projection;
	private float pixels_per_degree, pixels_per_radian, field, field0, fieldDeg, fieldDegBefore = -1, maglim, pixels_per_degree_50;
	private double jd, equinox, trajectoryField = 0.0, obsLat = 0.0;
	private boolean repaintLeyend = true, trajectoryCalculated = false, fast = false, externalGraphics = false;
	private Graphics g;
	private float refz;

	private float foregroundDist = 0, conlimDist = 0, axesDist = 0, milkywayDist = 0, nebulaDist = 0;

	private boolean drawAll = false;
	private float drawObjectsLimitingMagnitude = -1;
	private int nstars = -1, adds;

	/**
	 * Returns the projection object.
	 * @return Projection object.
	 */
	public Projection getProjection() {
		return projection;
	}
	
	/**
	 * Returns the internal rectagble of the rendering grid.
	 * @return An array with minimum x, minimum y, width, and height
	 * of the external rectangle.
	 */
	public float[] getRectangle() {
		if (rec == null) return null;
		return new float[] {rec.getMinX(), rec.getMinY(), rec.getWidth(), rec.getHeight()};
	}

	/**
	 * Returns if the drawing of all possible objects in a limited sky region is active.
	 * @return True or false.
	 */
	public boolean isAllPosibleObjectsInNextFrame() { return drawAll; }

	/**
	 * Toggles on/off the drawing of all possible objects in the next rendering up
	 * to a reasonable limiting magnitude based on field, loading into memory only
	 * the objects that will be visible on screen.<P>
	 * This method has effect only in Android. The limiting magnitude for stars
	 * should be set to 6.5 exactly.
	 * @return True if the toggle was successful. In case of switching it on, next
	 * frame will contain more objects. false is returned when the field of view is
	 * too high (greater than 50 deg).
	 */
	public boolean toggleAllPosibleObjectsInNextFrame() {
		if (!g.renderingToAndroid()) return false;
		if (drawAll) {
    		render.drawObjectsLimitingMagnitude = drawObjectsLimitingMagnitude;
    		drawAll = false;

    		DataBase.addData("objects", threadID, null, true);
    		DataBase.addData("objectsJ2000", null, null, true);
    		DataBase.addData("satEphem", threadID, null, true);
    		DataBase.addData("probeEphem", threadID, null, true);
    		DataBase.addData("asterEphem", threadID, null, true);
    		DataBase.addData("cometEphem", threadID, null, true);
    		DataBase.addData("neoEphem", threadID, null, true);
    		DataBase.addData("transEphem", threadID, null, true);
    		DataBase.addData("sncat", threadID, null, true);
    		DataBase.addData("novae", threadID, null, true);

    		db_objects = -1;
    		db_satEphem = -1;
    		db_probeEphem = -1;
    		db_asterEphem = -1;
    		db_cometEphem = -1;
    		db_neoEphem = -1;
    		db_transEphem = -1;
    		db_sncat = -1;
    		db_objects = -1;
    		db_novae = -1;

    		re_star = null;
    		nstars = -1;
    		return true;
		}

		if (fieldDeg > 50) return false;

		drawAll = true;
		setStarsLimitingMagnitude();

		drawObjectsLimitingMagnitude = render.drawObjectsLimitingMagnitude;
		render.drawObjectsLimitingMagnitude = 16.5f; //maglim + 3;
		//if (render.drawObjectsLimitingMagnitude > 16) render.drawObjectsLimitingMagnitude = 16;
		//if (render.drawObjectsLimitingMagnitude <= drawObjectsLimitingMagnitude) {
		//	render.drawObjectsLimitingMagnitude = drawObjectsLimitingMagnitude;
		//	return true;
		//}

		DataBase.addData("objects", threadID, null, true);
		DataBase.addData("objectsJ2000", null, null, true);
		DataBase.addData("satEphem", threadID, null, true);
		DataBase.addData("probeEphem", threadID, null, true);
		DataBase.addData("asterEphem", threadID, null, true);
		DataBase.addData("cometEphem", threadID, null, true);
		DataBase.addData("neoEphem", threadID, null, true);
		DataBase.addData("transEphem", threadID, null, true);
		DataBase.addData("sncat", threadID, null, true);
		DataBase.addData("novae", threadID, null, true);

		db_objects = -1;
		db_satEphem = -1;
		db_probeEphem = -1;
		db_asterEphem = -1;
		db_cometEphem = -1;
		db_neoEphem = -1;
		db_transEphem = -1;
		db_sncat = -1;
		db_novae = -1;
		return true;
	}

	/**
	 * Renderize the sky to another device.
	 *
	 * @param gg Graphics device.
	 * @throws Exception Thrown if the calculation fails.
	 */
	public synchronized void renderize(Graphics gg) throws Exception
	{
		this.g = gg;

//		g.enableAntialiasing();
		if (!render.drawWithAntialiasing) g.disableAntialiasing();
//		g.disableInversion();
//		g.setClip(0, 0, render.width, render.height);
//		g.enableInversion();
		if (!render.enableTransparentColors) g.disableTransparency();

		// Force to read stars again in case rendering mode has changed,
		// since greek labels for stars are read in a special way for
		// external graphics
		if (externalGraphics != g.renderingToExternalGraphics() && re_star != null) re_star = null;
		externalGraphics = g.renderingToExternalGraphics();

		render();

//		g.disableInversion();
//        g.setClip((int) rec.getMinX()+1, (int) rec.getMinY()+1, (int) rec.getWidth()-2, (int) rec.getHeight()-2);
//        g.enableInversion();
		drawLabels();
		drawHorizonTexture();
		drawLabelsCoord();
		drawAxesLabels();
/*		if (!g.renderingToAndroid() && !firstTime && render.drawLeyend != LEYEND_POSITION.NO_LEYEND) { // Set clipping region to later use drawImage only in required rectangle
			g.disableInversion();
			int py = 0;
			if (repaintLeyend) py = this.leyendMargin;
			if (render.drawLeyend == LEYEND_POSITION.TOP)
				g.setClip((int) rec.getMinX(), (int) rec.getMinY()-py, (int) rec.getWidth(), (int) rec.getHeight()+py+1);
			if (render.drawLeyend == LEYEND_POSITION.BOTTOM)
				g.setClip((int) rec.getMinX(), (int) rec.getMinY(), (int) rec.getWidth(), (int) rec.getHeight()+py+1);
			if (render.drawLeyend == LEYEND_POSITION.LEFT)
				g.setClip((int) rec.getMinX()-py, (int) rec.getMinY(), (int) rec.getWidth()+py, (int) rec.getHeight()+py+1);
			if (render.drawLeyend == LEYEND_POSITION.RIGHT)
				g.setClip((int) rec.getMinX(), (int) rec.getMinY(), (int) rec.getWidth()+py, (int) rec.getHeight()+py+1);
	        g.enableInversion();
		}
*/    	firstTime = false;
	}

	/**
	 * Returns the image of the rendering.
	 * @return The rendering.
	 */
	public Object getImage() {
		return g.getRendering();
	}
	private static final String[] EMPTY_ARRAY = new String[0];
	//private static final int numberOfProcessors = Runtime.getRuntime().availableProcessors();
	//private Thread t0;

	/**
	 * Renderize the sky.
	 *
	 * @throws Exception Thrown if the calculation fails.
	 */
	private void render() throws Exception
	{
		labelsName = EMPTY_ARRAY;
		labelsAxesNameX = EMPTY_ARRAY;
		labelsAxesNameY = EMPTY_ARRAY;
		labels = new ArrayList<Object[]>();
		labelsAxesX = new ArrayList<Object[]>();
		labelsAxesY = new ArrayList<Object[]>();
		DataBase.addData("minorObjects", threadID, null, true);
		db_minorObjects = -1;
		adds = g.renderingToAndroid()? 2:0;

        if (firstTime) {
			equinox = projection.eph.equinox;
			if (equinox == EphemerisElement.EQUINOX_OF_DATE)
				equinox = jd;
/*		} else {
			// drawImage in AWTGraphics can also support drawing using an int[] array, which is faster than intrinsic for images
			// without alpha, and this is the case for stars without textures. This will optimize the starImg array for such
			// fast rendering (25% faster). DISABLED SINCE IT IS FINALLY NOT FASTER
			if ((render.drawStarsRealistic == REALISTIC_STARS.NONE || render.drawStarsRealistic == REALISTIC_STARS.NONE_CUTE) &&
					!g.renderingToAndroid() && !g.renderingToExternalGraphics() && starImg != null && starImg[1][0] != null
					&& !starImg[1][0].getClass().isArray()) {
				for (int i=0; i<starImg.length; i++) {
					for (int j=0; j<starImg[0].length; j++) {
						Object img = starImg[i][j];
						if (img != null) {
							int size[] = g.getSize(img);
							starImg[i][j] = new Object[] {size, g.getImageAsPixels(img)};
						}
					}
				}
			}
*/		}

        if (render.trajectory != null && firstTime && !trajectoryCalculated) {
        	double ra_mean = 0.0, dec_mean = 0.0, ra_min = -1;
        	int n = 0;
        	double ast = projection.ast, jd = projection.jd;
    		trajectoryField = -1;
        	for (int i=0; i<render.trajectory.length; i++)
        	{
        		render.trajectory[i].loc_path = null;
        		try {
        			render.trajectory[i].populateTrajectoryPath(projection.obs, projection.eph);
        		} catch (Exception exc) {
        			continue;
        		}

        		for (int j=0; j<render.trajectory[i].loc_path.length; j++) {
        			if (render.coordinateSystem == COORDINATE_SYSTEM.HORIZONTAL) {
						double jd_TDB = render.trajectory[i].startTimeJD + render.trajectory[i].stepTimeJD * j;
						projection.ast = SiderealTime.apparentSiderealTime(new TimeElement(jd_TDB, SCALE.BARYCENTRIC_DYNAMICAL_TIME), projection.obs, projection.eph);
						projection.jd = jd_TDB;
        			}

        			if (render.trajectory[i].loc_path[j] != null)
        				render.trajectory[i].loc_path[j] = projection.getApparentLocationInSelectedCoordinateSystem(render.trajectory[i].loc_path[j], true, true, 0);
        			if (i == 0 && render.trajectory[i].loc_path[j] != null) {
        				double lon = render.trajectory[i].loc_path[j].getLongitude();
	        			if (j == 0 && lon < ra_min) ra_min = lon;
	        			if (ra_min < Constant.PI_OVER_TWO && lon > 3*Constant.PI_OVER_TWO) lon -= Constant.TWO_PI;
	        			if (ra_min > 3*Constant.PI_OVER_TWO && lon > Constant.PI_OVER_TWO) lon += Constant.TWO_PI;
	        			ra_mean += lon;
	        			dec_mean += render.trajectory[i].loc_path[j].getLatitude();
	        			n++;
	        			double dif = LocationElement.getApproximateAngularDistance(new LocationElement(ra_mean / n, dec_mean / n, 1.0), render.trajectory[i].loc_path[j]);
	        			if (dif > trajectoryField || trajectoryField == -1) trajectoryField = dif;
        			}
        		}
        	}
        	projection.ast = ast;
        	projection.jd = jd;
			if (render.trajectory[0].autoCentering && render.trajectory.length == 1)
			{
				render.centralLongitude = ra_mean / n;;
				render.centralLatitude = dec_mean / n;
				if (trajectoryField > Constant.PI_OVER_TWO && render.trajectory[0].central_loc != null)
				{
					render.centralLongitude = render.trajectory[0].central_loc.getLongitude();
					render.centralLatitude = render.trajectory[0].central_loc.getLatitude();
				}

				trajectoryField *= 2.5;
				if (render.trajectory[0].autoScale)
				{
					double ratio = ((double) render.width) / render.height;
					if (ratio < 1) ratio = 1.0 / ratio;
					trajectoryField *= (ratio + 1.0) * 0.5;
					float fl = TelescopeElement.getOcularFocalLengthForCertainField(trajectoryField,
							render.telescope);
					if (fl > 0)
					{
						render.telescope.ocular.focalLength = fl;
					} else
					{
						if (trajectoryField > Constant.PI_OVER_TWO)
						{
							render.telescope = TelescopeElement.HUMAN_EYE;
						} else
						{
							if (trajectoryField < deg_0_5)
							{
								render.telescope = TelescopeElement.SCHMIDT_CASSEGRAIN_20cm;
								fl = TelescopeElement.getOcularFocalLengthForCertainField(trajectoryField,
										render.telescope);
								if (fl > 0)
								{
									render.telescope.ocular.focalLength = fl;
								}
							} else
							{
								render.telescope = TelescopeElement.BINOCULARS_7x50;
								fl = TelescopeElement.getOcularFocalLengthForCertainField(trajectoryField,
										render.telescope);
								if (fl > 0)
								{
									render.telescope.ocular.focalLength = fl;
								} else
								{
									JPARSECException.addWarning("cannot perform auto scale operation. Please select an adequate telescope first.");
								}
							}
						}
					}
				}
			}
			trajectoryCalculated = true;
		}

		// Set values for field and north angle
		if (!forcedFOV) projection.configure(render);
		this.field = (float) projection.getField();
		fieldDeg = (float) (field * Constant.RAD_TO_DEG);
		fast = true;
		if (g.renderingToExternalGraphics() || render.anaglyphMode != ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) fast = false;

		/*
		 * Now we set limiting magnitude for reading stars. The value depends on
		 * the actual resolution, which strongly depends on the field to draw.
		 * It has no sense to draw stars beyond 6.5 magnitude when the field of
		 * view is very wide
		 */
        if (!g.renderingToAndroid() && render.width >= 3000) {
        	int newHugeFactor = 0; //Math.max((render.width/1000) - 2, 4);
        	//if (hugeFactor != newHugeFactor) starImg = null;
        	hugeFactor = newHugeFactor;
        } else {
        	//if (hugeFactor != 0) starImg = null;
            hugeFactor = 0;
        }
		if (fieldDeg != fieldDegBefore || render.drawStarsLimitingMagnitude != maglim) {
			pixels_per_radian = (render.width / this.field);
			pixels_per_degree = (float) (pixels_per_radian * Constant.DEG_TO_RAD);
			pixels_per_degree_50 = pixels_per_degree * 50;

			setStarsLimitingMagnitude();
			fieldDegBefore = fieldDeg;
		}

		// Draw everything
        repaintLeyend = true;
        g.setColor(this.render.background, true);
		g.disableInversion();
		g.setClip(0, 0, render.width, render.height);
		int marginX = 0; //render.drawCoordinateGridFont.getSize()+1;
		//if (!render.drawCoordinateGrid && !render.drawExternalGrid) marginX = 0;
		if (g.renderingToAndroid() && (render.drawLeyend == LEYEND_POSITION.LEFT || render.drawLeyend == LEYEND_POSITION.RIGHT))
			render.drawLeyend = LEYEND_POSITION.TOP;
		if (g.renderingToAndroid() || firstTime || g.renderingToExternalGraphics()) {
			marginX = 0;
			if (!g.renderingToAndroid() || render.background != 0) g.fillRect(0, 0, render.width, render.height);
		}
        if (render.drawLeyend != LEYEND_POSITION.NO_LEYEND) {
    		leyendMargin = 1+(51*14)/15;
        	if (render.width >= 850)
        		leyendMargin = 1+(51*render.drawCoordinateGridFont.getSize())/15;
    		if (maglim > 9)
    			leyendMargin += maglim-9;
    		if (hugeFactor > 1) leyendMargin *= (hugeFactor-1);

    		int recy = 0, recx = graphMarginX, recw = render.width-graphMarginX, rech = render.height-leyendMargin-graphMarginY;
    		if (render.drawLeyend == LEYEND_POSITION.TOP) recy = leyendMargin;
    		if (render.drawLeyend == LEYEND_POSITION.LEFT) recx += leyendMargin;
    		if (render.drawLeyend == LEYEND_POSITION.RIGHT || render.drawLeyend == LEYEND_POSITION.LEFT) {
    			recw -= leyendMargin;
    			rech += leyendMargin;
    			if (!g.renderingToAndroid() && !firstTime && !g.renderingToExternalGraphics()) {
    	    		if (render.drawLeyend == LEYEND_POSITION.LEFT) {
    	    			g.fillRect(marginX + recx - graphMarginX, 0, recw + leyendMargin, rech + graphMarginY - marginX);
    	    		} else {
    	    			g.fillRect(marginX + recx - graphMarginX, 0, recw + graphMarginX - marginX, rech + graphMarginY - marginX);
    	    			g.fillRect(recx + recw, recy + rech, render.width - (recx + recw), render.height - (recy + rech));
    	    		}
    			}
    		} else {
    			if (!g.renderingToAndroid() && !firstTime && !g.renderingToExternalGraphics())
    				g.fillRect(marginX, recy, render.width-marginX, render.height-leyendMargin-graphMarginY+(render.drawCoordinateGridFont.getSize()*3)/2+3);
    		}
    		rec = new Rectangle(recx, recy, recw, rech);
        } else {
			if (!g.renderingToAndroid() && !firstTime && !g.renderingToExternalGraphics())
				g.fillRect(marginX, 0, render.width-marginX, render.height-(marginX+ (render.drawCoordinateGrid ? 6 : 0)));
        }
        if (//!g.renderingToAndroid() &&
        		maglim == this.mmax && lastLeyend != -1 && !g.renderingToExternalGraphics()) {
        	repaintLeyend = false;
        }

		g.setColor(render.drawCoordinateGridColor, 255);
		if (fast && render.drawCoordinateGridStroke.getLineWidth() == JPARSECStroke.STROKE_DEFAULT_LINE_THIN.getLineWidth()) {
			g.setStroke(JPARSECStroke.STROKE_DEFAULT_LINE_THIN);
			if (render.drawExternalGrid) {
				g.drawStraightLine(rec.getMinX(), rec.getMinY(), rec.getMinX(), rec.getMaxY());
				g.drawStraightLine(rec.getMaxX(), rec.getMinY(), rec.getMaxX(), rec.getMaxY());
				g.drawStraightLine(rec.getMinX(), rec.getMaxY(), rec.getMaxX(), rec.getMaxY());
			}
			if (repaintLeyend && render.drawLeyend != LEYEND_POSITION.NO_LEYEND) {
				drawLeyend();
				g.setColor(render.drawCoordinateGridColor, 255);
				g.setStroke(JPARSECStroke.STROKE_DEFAULT_LINE_THIN);
			}
			g.disableInversion();
			if (render.drawExternalGrid || render.drawLeyend != LEYEND_POSITION.NO_LEYEND) g.drawStraightLine(rec.getMinX(), rec.getMinY(), rec.getMaxX(), rec.getMinY());
		} else {
			g.setStroke(new JPARSECStroke(JPARSECStroke.STROKE_DEFAULT_LINE_THIN, render.drawCoordinateGridStroke.getLineWidth()));
			float dist = getDist(foregroundDist);
			if (render.drawExternalGrid) {
				g.drawLine(rec.getMinX(), rec.getMinY(), rec.getMinX(), rec.getMaxY(), dist, dist);
				g.drawLine(rec.getMaxX(), rec.getMinY(), rec.getMaxX(), rec.getMaxY(), dist, dist);
				g.drawLine(rec.getMinX(), rec.getMaxY(), rec.getMaxX(), rec.getMaxY(), dist, dist);
			}
			if (repaintLeyend && render.drawLeyend != LEYEND_POSITION.NO_LEYEND) {
				drawLeyend();
				g.setColor(render.drawCoordinateGridColor, 255);
				g.setStroke(new JPARSECStroke(JPARSECStroke.STROKE_DEFAULT_LINE_THIN, render.drawCoordinateGridStroke.getLineWidth()));
			}
			g.disableInversion();
			if (render.drawExternalGrid || render.drawLeyend != LEYEND_POSITION.NO_LEYEND) g.drawLine(rec.getMinX(), rec.getMinY(), rec.getMaxX(), rec.getMinY(), dist, dist);
		}
		g.disableInversion();
		int addVclip = (int) (render.drawCoordinateGridStroke.getLineWidth() - 1);
        g.setClip((int) rec.getMinX()+1, (int) rec.getMinY()+1+addVclip, (int) rec.getWidth()-2, (int) rec.getHeight()-2-addVclip);
        g.enableInversion();


		LocationElement loc0Date = this.projection.getEquatorialPositionOfRendering();
		if (loc0J2000 == null || !loc0Date.equals(this.loc0Date)) {
			this.loc0Date = loc0Date;
			if (equinox != EphemerisElement.EQUINOX_J2000)
			{
				loc0J2000 = LocationElement.parseRectangularCoordinates(this.precessToJ2000(equinox, loc0Date.getRectangularCoordinates(), projection.eph));
			} else {
				loc0J2000 = loc0Date;
			}
		}

		if (faintStars != null && faintStarsLoc != null) {
			double fdist = LocationElement.getApproximateAngularDistance(loc0J2000, faintStarsLoc) * Constant.RAD_TO_DEG;
			if (fdist > 1.75) {
				faintStars = null;
				if (render.drawFaintStars) {
		    		DataBase.addData("objects", threadID, null, true);
		    		DataBase.addData("satEphem", threadID, null, true);
		    		DataBase.addData("probeEphem", threadID, null, true);
		    		DataBase.addData("asterEphem", threadID, null, true);
		    		DataBase.addData("cometEphem", threadID, null, true);
		    		DataBase.addData("neoEphem", threadID, null, true);
		    		DataBase.addData("transEphem", threadID, null, true);
		    		DataBase.addData("sncat", threadID, null, true);
		    		DataBase.addData("novae", threadID, null, true);

		    		db_objects = -1;
		    		db_satEphem = -1;
		    		db_probeEphem = -1;
		    		db_asterEphem = -1;
		    		db_cometEphem = -1;
		    		db_neoEphem = -1;
		    		db_transEphem = -1;
		    		db_sncat = -1;
		    		db_objects = -1;
		    		db_novae = -1;
				}
			}
		}
		
		projection.createNewArrayWhenProjecting = false;
		drawHorizon();
		drawMilkyWay();
		drawNebulae();
		projection.createNewArrayWhenProjecting = true;

		if (fieldDeg < 380) {
			if (g.renderingToAndroid()) { // && render.drawFastLinesMode.fastGrid()) {
				drawAxes();
			} else {
				if (g.renderingToExternalGraphics() || (!render.drawCoordinateGridStroke.equals(JPARSECStroke.STROKE_DEFAULT_LINE_THIN)
						&& !render.drawCoordinateGridStroke.equals(JPARSECStroke.STROKE_DEFAULT_LINE))) {
					drawAxesOld();
				} else {
					drawAxes(); // drawAxes();
				}
			}
		}

		projection.createNewArrayWhenProjecting = false;
		drawDeepSkyObjects();
		g.setStroke(JPARSECStroke.STROKE_DEFAULT_LINE_THIN);
		drawMeteorShowers();
 		drawTransNeptunianObjects();
		drawSuperNova();
		drawNovae();

		if (render.trajectory != null) {
			if (render.trajectory[0] != null) {
				if (render.trajectory[0].objectType != null) {
					drawTrajectory(false);
				}
			}
		}

		magLabelCount = 0;
		drawConstelAndStars();
		if (render.drawFaintStars && fieldDeg < 8) {
			if (g.renderingToAndroid()) {
				if (render.drawStarsLimitingMagnitude > 8.5)
					drawFaintStars();
			} else {
				if (render.drawStarsLimitingMagnitude > 10)
					drawFaintStars();
			}
		}
        drawConstelLimits();
		projection.createNewArrayWhenProjecting = true;
		drawEclipticAxis();
		projection.createNewArrayWhenProjecting = false;
		g.setStroke(JPARSECStroke.STROKE_DEFAULT_LINE_THIN);
        drawConstelNames();

		if ((jd > 1228000.5 || (jd > 625296.5 && !projection.eph.preferPrecisionInEphemerides)) 
				&& jd < 2817057.5)
		{
			if (labels.size() > 0) {
				drawLabels();
				labels = new ArrayList<Object[]>();
			}

			boolean onEarth = projection.obs.getMotherBody() == TARGET.EARTH;
			if (onEarth) {
				planets = null;
				drawComets(true);
			}
			planets = drawPlanets();
			drawComets(!onEarth);
			drawAsteroids();
			g.setStroke(JPARSECStroke.STROKE_DEFAULT_LINE_THIN);
			drawNEOs();
			drawProbes();
			drawArtificialSatellites();
		} else
		{
			planets = null;
			JPARSECException
					.addWarning("cannot obtain ephemeris for Solar System objects in this date.");
		}
		if (render.trajectory != null) {
			if (render.trajectory[0] != null) {
				if (render.trajectory[0].objectType != null) {
					drawTrajectory(true);
				}
			}
		}
		projection.createNewArrayWhenProjecting = true;

 		// Draw ocular field of view
		if (render.drawOcularFieldOfView && (render.projection == PROJECTION.CYLINDRICAL_EQUIDISTANT || projection.isCylindricalForced()))
		{
			float radius = (float) (pixels_per_radian * 0.5 * field0);
			float x0 = this.getXCenter(), y0 = this.getYCenter();
			if (radius < x0*1.5 && radius > 2) {
				g.setColor(render.drawOcularFieldOfViewColor, true);
				float dist = getDist(foregroundDist);
				String s = render.telescope.name;
				double sf1 = field0, sf2 = field0;
				if (//render.telescope.ocular == null &&
						render.telescope.ccd != null) {
					if (render.telescope.ccd.name != null && !render.telescope.ccd.name.trim().equals("")) 
						s += " + "+render.telescope.ccd.name.trim();
					TelescopeElement tel = render.telescope.clone();
					tel.ocular = null;
					radius = (float) (pixels_per_radian * 0.5 * render.telescope.ccd.getFieldX(tel));
					float radiusy = (float) (pixels_per_radian * 0.5 * render.telescope.ccd.getFieldY(tel));
					sf1 = render.telescope.ccd.getFieldX(tel);
					sf2 = render.telescope.ccd.getFieldY(tel);
					float drs = pixels_per_degree;
					if (radius / drs < 2) drs /= 6;
					if (radius / drs < 2) drs /= 10;
					if (radius / drs < 2) drs /= 6;
					if (radius / drs < 2) drs /= 10;
					int nmarksX = (int) (radius / drs);
					int nmarksY = (int) (radiusy / drs);
					int off = (int) (24f * (2f * radius / (float) render.width));
					if (render.telescope.ccd.cameraPA == 0) {
						g.drawRect(x0-radius, y0-radiusy, 2*radius, 2*radiusy, dist);
						for (int i=0; i<=nmarksX; i++) {
							float dri = drs * i;
							g.drawLine(x0-dri, y0+radiusy, x0-dri, y0+radiusy+off, dist, dist);
							g.drawLine(x0-dri, y0-radiusy, x0-dri, y0-radiusy-off, dist, dist);
							if (i > 0) {
								g.drawLine(x0+dri, y0+radiusy, x0+dri, y0+radiusy+off, dist, dist);
								g.drawLine(x0+dri, y0-radiusy, x0+dri, y0-radiusy-off, dist, dist);
							}
						}
						for (int i=0; i<=nmarksY; i++) {
							float dri = drs * i;
							g.drawLine(x0+radius, y0-dri, x0+radius+off, y0-dri, dist, dist);
							g.drawLine(x0-radius, y0-dri, x0-radius-off, y0-dri, dist, dist);
							if (i > 0) {
								g.drawLine(x0+radius, y0+dri, x0+radius+off, y0+dri, dist, dist);
								g.drawLine(x0-radius, y0+dri, x0-radius-off, y0+dri, dist, dist);
							}
						}
					} else {
						double dr = FastMath.hypot(radius, radiusy), ang0 = FastMath.atan2(radiusy, radius);
						float ang = -render.telescope.ccd.cameraPA;
						float dx1 = (float) (dr * FastMath.cos(ang0+ang));
						float dy1 = (float) (-dr * FastMath.sin(ang0+ang));
						float dx2 = (float) (dr * FastMath.cos(Math.PI-ang0+ang));
						float dy2 = (float) (-dr * FastMath.sin(Math.PI-ang0+ang));
						float dx3 = -dx1, dy3 = -dy1, dx4 = -dx2, dy4 = -dy2;
						g.drawLine(x0+dx1, y0+dy1, x0+dx2, y0+dy2, dist, dist);
						g.drawLine(x0+dx2, y0+dy2, x0+dx3, y0+dy3, dist, dist);
						g.drawLine(x0+dx3, y0+dy3, x0+dx4, y0+dy4, dist, dist);
						g.drawLine(x0+dx4, y0+dy4, x0+dx1, y0+dy1, dist, dist);
						dx1 = (float) (off * FastMath.cos(Constant.PI_OVER_TWO+ang));
						dy1 = (float) (-off * FastMath.sin(Constant.PI_OVER_TWO+ang));
						dx2 = dy1;
						dy2 = -dx1;
						g.drawLine(x0+dx1, y0+dy1, x0-dx1, y0-dy1, dist, dist);
						g.drawLine(x0+dx2, y0+dy2, x0-dx2, y0-dy2, dist, dist);
						dx1 = dx1 * radiusy / (float) off;
						dy1 = dy1 * radiusy / (float) off;
						dx2 = dx2 * radius / (float) off;
						dy2 = dy2 * radius / (float) off;
						dx3 = dx1 * (radiusy+off) / radiusy;
						dy3 = dy1 * (radiusy+off) / radiusy;
						dx4 = dx2 * (radius+off) / radius;
						dy4 = dy2 * (radius+off) / radius;
						g.drawLine(x0+dx1, y0+dy1, x0+dx3, y0+dy3, dist, dist);
						g.drawLine(x0-dx1, y0-dy1, x0-dx3, y0-dy3, dist, dist);
						g.drawLine(x0+dx2, y0+dy2, x0+dx4, y0+dy4, dist, dist);
						g.drawLine(x0-dx2, y0-dy2, x0-dx4, y0-dy4, dist, dist);
						for (int i=-nmarksX; i<=nmarksX; i++) {
							if (i == 0) continue;
							float dri = drs * i;
							ang0 = FastMath.atan2(radiusy, dri);
							dri = (float) FastMath.hypot(dri, radiusy);
							float drix = (float) (dri * FastMath.cos(ang0+ang));
							float driy = (float) (-dri * FastMath.sin(ang0+ang));
							ang0 = FastMath.atan2(radiusy + off, drs * i);
							dri = (float) FastMath.hypot(drs * i, radiusy+off);
							float drix2 = (float) (dri * FastMath.cos(ang0+ang));
							float driy2 = (float) (-dri * FastMath.sin(ang0+ang));
							g.drawLine(x0-drix, y0-driy, x0-drix2, y0-driy2, dist, dist);
							g.drawLine(x0+drix, y0+driy, x0+drix2, y0+driy2, dist, dist);
						}
						for (int i=-nmarksY; i<=nmarksY; i++) {
							if (i == 0) continue;
							float dri = drs * i;
							ang0 = FastMath.atan2(dri, radius);
							dri = (float) FastMath.hypot(dri, radius);
							float drix = (float) (dri * FastMath.cos(ang0+ang));
							float driy = (float) (-dri * FastMath.sin(ang0+ang));
							ang0 = FastMath.atan2(drs * i, radius + off);
							dri = (float) FastMath.hypot(drs * i, radius+off);
							float drix2 = (float) (dri * FastMath.cos(ang0+ang));
							float driy2 = (float) (-dri * FastMath.sin(ang0+ang));
//							g.drawLine(x0-drix, y0+driy, x0-drix2, y0+driy2, dist, dist);
							g.drawLine(x0-drix, y0-driy, x0-drix2, y0-driy2, dist, dist);
							g.drawLine(x0+drix, y0+driy, x0+drix2, y0+driy2, dist, dist);
//							g.drawLine(x0+drix, y0-driy, x0+drix2, y0-driy2, dist, dist);
						}
					}
					if (render.telescope.ccd.cameraPA == 0) radius = radiusy;
				} else {
					if (render.telescope.ocular != null && 
							render.telescope.ocular.name != null && 
							!render.telescope.ocular.name.trim().equals("")) s += " + "+render.telescope.ocular.name.trim();
					g.drawOval(x0-radius, y0 - radius, 2*radius+1, 2*radius+1, dist);
					int off = (int) (24f * (2f * radius / (float) render.width));
					g.drawLine(x0-off, y0, x0+off, y0, dist, dist);
					g.drawLine(x0, y0-off, x0, y0+off, dist, dist);
					g.drawLine(x0, y0-off-radius, x0, y0-radius, dist, dist);
					g.drawLine(x0, y0+off+radius, x0, y0+radius, dist, dist);
					g.drawLine(x0-off-radius, y0, x0-radius, y0, dist, dist);
					g.drawLine(x0+off+radius, y0, x0+radius, y0, dist, dist);
					off = (int) (16f * (2f * radius / (float) render.width));
					float cos15 = FastMath.cosf(15 * Constant.DEG_TO_RAD);
					float cos30 = FastMath.cosf(30 * Constant.DEG_TO_RAD);
					float cos45 = FastMath.cosf(45 * Constant.DEG_TO_RAD);
					float cos60 = FastMath.cosf(60 * Constant.DEG_TO_RAD);
					float cos75 = FastMath.cosf(75 * Constant.DEG_TO_RAD);
					float dx0 = radius * cos15, dx1 = (radius + off) * cos15, dy0 = radius * cos75, dy1 = (radius + off) * cos75;
					g.drawLine(x0+dx0, y0+dy0, x0+dx1, y0+dy1, dist, dist);
					g.drawLine(x0-dx0, y0-dy0, x0-dx1, y0-dy1, dist, dist);
					g.drawLine(x0+dx0, y0-dy0, x0+dx1, y0-dy1, dist, dist);
					g.drawLine(x0-dx0, y0+dy0, x0-dx1, y0+dy1, dist, dist);

					g.drawLine(x0+dy0, y0+dx0, x0+dy1, y0+dx1, dist, dist);
					g.drawLine(x0-dy0, y0-dx0, x0-dy1, y0-dx1, dist, dist);
					g.drawLine(x0+dy0, y0-dx0, x0+dy1, y0-dx1, dist, dist);
					g.drawLine(x0-dy0, y0+dx0, x0-dy1, y0+dx1, dist, dist);

					dx0 = radius * cos30;
					dx1 = (radius + off) * cos30;
					dy0 = radius * cos60;
					dy1 = (radius + off) * cos60;
					g.drawLine(x0+dx0, y0+dy0, x0+dx1, y0+dy1, dist, dist);
					g.drawLine(x0-dx0, y0-dy0, x0-dx1, y0-dy1, dist, dist);
					g.drawLine(x0+dx0, y0-dy0, x0+dx1, y0-dy1, dist, dist);
					g.drawLine(x0-dx0, y0+dy0, x0-dx1, y0+dy1, dist, dist);

					g.drawLine(x0+dy0, y0+dx0, x0+dy1, y0+dx1, dist, dist);
					g.drawLine(x0-dy0, y0-dx0, x0-dy1, y0-dx1, dist, dist);
					g.drawLine(x0+dy0, y0-dx0, x0+dy1, y0-dx1, dist, dist);
					g.drawLine(x0-dy0, y0+dx0, x0-dy1, y0+dx1, dist, dist);

					dx0 = dy0 = radius * cos45;
					dx1 = dy1 = (radius + off) * cos45;
					g.drawLine(x0+dy0, y0+dx0, x0+dy1, y0+dx1, dist, dist);
					g.drawLine(x0-dy0, y0-dx0, x0-dy1, y0-dx1, dist, dist);
					g.drawLine(x0+dy0, y0-dx0, x0+dy1, y0-dx1, dist, dist);
					g.drawLine(x0-dy0, y0+dx0, x0-dy1, y0+dx1, dist, dist);
				}
				String f = Functions.formatAngle(sf1, 0);
				if (sf2 != sf1) f += " x "+Functions.formatAngle(sf2, 0);
				s += " ("+f+")";
				float sw = g.getStringWidth(s);
				g.drawString(s, x0 - sw / 2, y0 + radius + g.getFont().getSize()*2);
			}
		}

		if (render.drawCentralCrux) {
			float radius = (float) (render.width / 5.0);
			float x0 = this.getXCenter(), y0 = this.getYCenter();
			g.setColor(render.drawOcularFieldOfViewColor, true);
			float dist = getDist(foregroundDist);
			int off = (int) (24f * (2f * radius / (float) render.width));
			if (g.renderingToAndroid()) g.setStroke(JPARSECStroke.STROKE_DEFAULT_LINE);
			g.drawLine(x0-off, y0, x0+off, y0, dist, dist);
			g.drawLine(x0, y0-off, x0, y0+off, dist, dist);
		}
//        if (firstTime) System.gc();
	}

	private static final double dists0[] = new double[] {1.0, 0.61, 0.28, 0, 0.52, 4.2, 8.5, 18.2, 29, 30};
	private boolean satsPossible(TARGET targetBody, double dist) {
		if (targetBody.ordinal() < 4 || !render.drawClever || pixels_per_degree < 12) return false;

		double pixels_per_degree = this.pixels_per_degree;
		if (targetBody.ordinal() < dists0.length) pixels_per_degree *= dists0[targetBody.ordinal()] / dist;
		if ((targetBody == TARGET.MARS && pixels_per_degree > 120) ||
		(targetBody == TARGET.JUPITER && pixels_per_degree > 12) ||
		(targetBody == TARGET.SATURN && pixels_per_degree > 25) ||
		(targetBody == TARGET.URANUS && pixels_per_degree > 120) ||
		(targetBody == TARGET.NEPTUNE && pixels_per_degree > 400) ||
		(targetBody == TARGET.Pluto && pixels_per_degree > 2000)
			)
			return true;
		return false;
	}
	private TARGET[] planetsOrdered = null;
	private RenderPlanet rp = null;
	/**
	 * Returns the instance of {@linkplain RenderPlanet}.
	 * @return Instance of {@linkplain RenderPlanet}.
	 */
	public RenderPlanet getRenderPlanet() {
		return rp;
	}
	private ArrayList<Object> drawPlanets() throws Exception
	{
		/*
		 * We will use this ArrayList to store (x, y) position in screen, radius in
		 * pixels, and distance to observer in AU of any visible planet. This
		 * will be later use when drawing asteroids and comets. A minor object
		 * will not be drawn if it is behind a major body
		 */
		ArrayList<Object> planet_v = new ArrayList<Object>();
		planet_v.ensureCapacity(50);

		// Renderize planets if visible and enabled
		if (firstTime || majorObjects == null) {
			EphemerisElement eph = projection.eph.clone();
			eph.targetBody = TARGET.Moon;
			ephem_moon_topo = Ephem.getEphemeris(projection.time, projection.obs, eph, false);
			EphemerisElement sun_eph = new EphemerisElement(TARGET.SUN, projection.eph.ephemType,
					EphemerisElement.EQUINOX_OF_DATE, projection.eph.isTopocentric, projection.eph.ephemMethod,
					projection.eph.frame, projection.eph.algorithm);
			sun_eph.optimizeForSpeed();
			render.planetRender.ephemSun = Ephem.getEphemeris(projection.time, projection.obs, sun_eph, false);
			render.planetRender.telescope = render.telescope;
	        if (forcedFOV) {
		        render.planetRender.telescope = TelescopeElement.SCHMIDT_CASSEGRAIN_20cm;
		        render.planetRender.telescope.invertHorizontal = render.telescope.invertHorizontal;
		        render.planetRender.telescope.invertVertical = render.telescope.invertVertical;
		        render.planetRender.telescope.ocular.focalLength = TelescopeElement.getOcularFocalLengthForCertainField(field, render.planetRender.telescope);
	        }
			render.planetRender.width = render.width;
			render.planetRender.height = render.height;
			render.planetRender.northUp = false;
			render.planetRender.background = this.render.background;
			render.planetRender.foreground = g.invertColor(this.render.background);
			planetsOrdered = new TARGET[] {
					TARGET.Pluto, TARGET.NEPTUNE, TARGET.URANUS, TARGET.SATURN, TARGET.JUPITER, TARGET.MARS, TARGET.SUN,
					TARGET.MERCURY, TARGET.VENUS, TARGET.Moon};
			if (projection.obs.getMotherBody() != TARGET.EARTH) {
				// Reordering as seen by the observer
				planetsOrdered = new TARGET[] {
						TARGET.Pluto, TARGET.NEPTUNE, TARGET.URANUS, TARGET.SATURN, TARGET.JUPITER, TARGET.MARS, TARGET.SUN,
						TARGET.MERCURY, TARGET.VENUS, TARGET.Moon, TARGET.EARTH};
				double dist[] = new double[planetsOrdered.length];
				//eph = projection.eph.clone();
				for (int i=0; i<dist.length; i++) {
					if (planetsOrdered[i] == TARGET.Moon || planetsOrdered[i] == TARGET.SUN) {
						if (planetsOrdered[i] == TARGET.Moon) dist[i] = ephem_moon_topo.distance;
						if (planetsOrdered[i] == TARGET.SUN) dist[i] = render.planetRender.ephemSun.distance;
					} else {
						eph.targetBody = planetsOrdered[i];
						EphemElement ephem = Ephem.getEphemeris(projection.time, projection.obs, eph, false);
						dist[i] = ephem.distance;
					}
				}
				Object o[] = DataSet.sortInDescent(planetsOrdered, dist);
				for (int i=0; i<dist.length; i++) {
					planetsOrdered[i] = (TARGET) o[i];
				}
			}
			if (majorObjects == null) majorObjects = new EphemElement[planetsOrdered.length];
		}
		if (majorObjectsSats == null) majorObjectsSats = new Object[50];

/*		if (difraction && pixels_per_degree < 200 && render.drawClever)
			render.planetRender.difraction = false;
		if (pixels_per_degree < 1000 && render.drawClever && render.planetRender.target != TARGET.SUN && render.planetRender.target != TARGET.Moon) {
			render.planetRender.axes = false;
			render.planetRender.axesNOSE = false;
		}
		if (pixels_per_degree < 30 && render.drawClever && (render.planetRender.target == TARGET.SUN || render.planetRender.target == TARGET.Moon)) {
			render.planetRender.axes = false;
			render.planetRender.axesNOSE = false;
		} 
*/
		if (rp == null) rp = new RenderPlanet();
		rp.offsetInLongitudeOfJupiterGRS = this.offsetInLongitudeOfJupiterGRS;
		rp.offsetInLongitudeOfJupiterGRS_system = this.offsetInLongitudeOfJupiterGRS_system;
		rp.renderingSky = true;
		rp.renderingSkyMagLim = maglim;

		boolean moonVisible = false;

		if (render.drawPlanetsMoonSun)
		{
			double showAllBelowField = deg_0_05;
			boolean draw_sats = render.planetRender.satellitesMain;
			float pos0[];
			double p2 = 2 * pixels_per_radian;
			EphemerisElement eph = null;
			MoonEphemElement lastMoons[] = null;

			if (render.planetRender.ephemSun == null) {
				EphemerisElement sun_eph = new EphemerisElement(TARGET.SUN, projection.eph.ephemType,
						EphemerisElement.EQUINOX_OF_DATE, projection.eph.isTopocentric, projection.eph.ephemMethod,
						projection.eph.frame, projection.eph.algorithm);
				sun_eph.correctForEOP = false;
				sun_eph.correctForPolarMotion = false;
				render.planetRender.ephemSun = Ephem.getEphemeris(projection.time, projection.obs, sun_eph, false);
			}

			for (int index = 0; index < planetsOrdered.length; index++)
			{
				if (majorObjects == null) break;
				TARGET target = planetsOrdered[index];
				if (projection.obs.getMotherBody() == target && target == TARGET.SUN) continue;

				projection.eph.targetBody = target;
				EphemElement obj = majorObjects[index];
				LocationElement loc0 = null;
				if (obj == null) {
					if (eph == null) eph = projection.eph.clone();
					eph.targetBody = target;

					render.planetRender.ephem = Ephem.getEphemeris(projection.time, projection.obs, eph, false);
					loc0 = projection.getApparentLocationInSelectedCoordinateSystem(render.planetRender.ephem.getEquatorialLocation(), true, false, render.planetRender.ephem.angularRadius);
					majorObjects[index] = render.planetRender.ephem.clone();
					majorObjects[index].setLocation(loc0);
				} else {
					render.planetRender.ephem = obj;
					loc0 = obj.getLocation();
				}
				
				if (render.drawHorizonTexture != HORIZON_TEXTURE.NONE && 
						render.planetRender.ephem.magnitude > maglim) continue;

				/*
				 * Since we want to draw satellites even if the planet is
				 * outside the visible screen, we accept a planet position
				 * outside the screen up to 2 degrees. No satellite goes
				 * away more than 2 degrees as seen from Earth
				 */
				if (target == TARGET.MERCURY || target == TARGET.VENUS || target == TARGET.SUN) {
					pos0 = projection.projectPosition(loc0, (int) (pixels_per_radian * render.planetRender.ephem.angularRadius), true);
				} else {
					pos0 = projection.projectPosition(loc0, (int) (pixels_per_degree * 2.0), projection.obs.getMotherBody() == TARGET.EARTH);
				}

				// Don't draw planets below limiting magnitude ?
				//if (render.planetRender.ephem.magnitude > maglim) pos0 = Projection.INVALID_POSITION;

				boolean sats_are_possible = satsPossible(projection.eph.targetBody, render.planetRender.ephem.distance);
				if (!projection.isInvalid(pos0) || (projection.obs.getMotherBody() == target && sats_are_possible))
				{
					// Don't draw object if the observer is inside it
					if (projection.obs.getMotherBody() != target && render.planetRender.ephem.angularRadius > Constant.PI_OVER_FOUR) continue;

					// Don't draw Mercury and Venus if they are behind the Sun
					if (projection.obs.getMotherBody() != TARGET.SUN &&
							render.planetRender.ephem.elongation < render.planetRender.ephemSun.angularRadius &&
							(target == TARGET.MERCURY || target == TARGET.VENUS) &&
							render.planetRender.ephem.distance > render.planetRender.ephemSun.distance)
						continue;

					render.planetRender.target = projection.eph.targetBody;

					if (target == TARGET.Moon) moonVisible = true;
					if (projection.isInvalid(pos0)) pos0 = new float[] {-1000000, -1000000};
					planet_v.add(new float[]
					{ pos0[0], pos0[1], pixels_per_radian * render.planetRender.ephem.angularRadius,
							(float) render.planetRender.ephem.distance, target.ordinal() });
					planet_v.add(render.planetRender.clone());
					rp.xPosition = pos0[0];
					rp.yPosition = pos0[1];

					if (draw_sats && sats_are_possible)
					{
						Object objS[] = null;
						if (majorObjectsSats[index] != null) objS = (Object[]) majorObjectsSats[index];
						if (objS == null) {
							boolean all = render.planetRender.satellitesAll;
							// Change event definition to limb to draw correctly the satellites when
							// they start to being occulted/eclipsed by the mother planet.
							EVENT_DEFINITION ed = MoonEvent.getEventDefinition();
							MoonEvent.setEventDefinition(EVENT_DEFINITION.AUTOMATIC_FOR_DRAWING);
							eph = projection.eph.clone();
							eph.targetBody = target;
							render.planetRender.moonephem = MoonEphem.calcAllSatellites(projection.time, projection.obs, eph, all);
							if (render.planetRender.moonephem == null) continue; // XXX
							MoonEvent.setEventDefinition(ed);
							MoonEphemElement me[] = render.planetRender.moonephem.clone();
							LocationElement loci[] = new LocationElement[me.length];
							for (int mei=0; mei<me.length;mei++)
							{
								loci[mei] = projection.getApparentLocationInSelectedCoordinateSystem(new LocationElement(me[mei].rightAscension, me[mei].declination, 1.0), true, false, me[mei].angularRadius);
								render.planetRender.moonephem[mei].name = me[mei].name;
							}
							majorObjectsSats[index] = new Object[] {me, loci};
							objS = new Object[] {me, loci};
						} else {
							render.planetRender.moonephem = (MoonEphemElement[]) objS[0];
						}
						if (index == planetsOrdered.length-1) lastMoons = render.planetRender.moonephem;

						ArrayList<float[]> v = new ArrayList<float[]>();
						if (render.planetRender.moonephem != null) {
							for (int i = 0; i < render.planetRender.moonephem.length; i++)
							{
								loc0 = ((LocationElement[]) objS[1])[i];
								float poss[] = projection.projectPosition(loc0, 0, false);
								if (!projection.isInvalid(poss)) poss = poss.clone();
								
								// Uncommenting this will not allow satellite shadows to be rendered when satellite is not visible
								//if (poss != null && !this.isInTheScreen((int)poss[0], (int)poss[1])) poss = null;

								if (!projection.isInvalid(poss) && !render.planetRender.moonephem[i].occulted && !render.planetRender.moonephem[i].eclipsed)
								{
									planet_v.add(new float[]
									{ poss[0], poss[1],
											-pixels_per_radian * render.planetRender.moonephem[i].angularRadius,
											(float) render.planetRender.moonephem[i].distance, i });
									planet_v.add(render.planetRender.clone());
								}

								v.add(poss);
							}
						}
						// In RenderPlanet satellite positions are drawn as RA vs DEC in cilyndrical projection, here
						// we consider the projection selected to allow comparing satellite positions with stellar ones
						rp.useSkySatPos = true;
						rp.skySatPos = v;
					} else {
						render.planetRender.moonephem = null;
					}

					double size = render.planetRender.ephem.angularRadius * this.pixels_per_radian;
					if (target == TARGET.Moon && size < 0.1) continue;
					if (sats_are_possible || size > RenderPlanet.minScale || target == TARGET.SUN || target == TARGET.Moon)
					{
						if (((target == TARGET.SUN && size < 1000 && render.planetRender.textures) || (target == TARGET.Moon && !render.planetRender.textures && size < 35 && render.drawIcons)) ||
								(size <= 15 && render.drawIcons && (target == TARGET.EARTH || target == TARGET.SUN || target == TARGET.Moon)))
						{
							String icon = "Earth.png";
							if (target == TARGET.SUN)
							{
								icon = "Sun2.png";
								if (size > 15) icon = "Sun3.png";
								if (render.drawStarsRealistic == REALISTIC_STARS.NONE ||
										render.drawStarsRealistic == REALISTIC_STARS.NONE_CUTE) {
									//icon = "sun.png";
									if (!render.planetRender.textures) icon = "Sun.png";
								}
								float scale = (float) Math.sqrt(render.width * 0.001f) / 3f;
								if (size > 15 && icon.equals("Sun2.png")) scale = (float) size * 2f / 89f;
								if (size > 15 && icon.equals("Sun3.png")) scale = (float) size * 2f / 548f;
								if (icon.equals("Sun.png")) {
									drawPlanetaryIcon(target, rp.xPosition, rp.yPosition, 0, scale);
								} else {
									if (icon.equals("Sun3.png")) {
										LocationElement loc = render.planetRender.ephemSun.getEquatorialLocation();
										String source = ""+(loc.getLongitude()*Constant.RAD_TO_DEG)+" "+(loc.getLatitude()*Constant.RAD_TO_DEG);
										String l = "DSS "+source+" 0 "+(render.planetRender.ephem.angularRadius*Constant.RAD_TO_DEG*2.0*60.0)+" 0 DSS DSS DSS";
										l = DataSet.replaceAll(l, " ", UnixSpecialCharacter.UNIX_SPECIAL_CHARACTER.TAB.value, true);
										overlay(new String[] {l}, 0, false, "SUN", readImage(icon), baryc,
												new float[] {rp.xPosition, rp.yPosition},
												null, 1, 0);
										boolean axesNOSE = render.planetRender.axesNOSE;
										if (axesNOSE && render.coordinateSystem == COORDINATE_SYSTEM.EQUATORIAL)
											render.planetRender.axesNOSE = false;
										if (render.planetRender.axes || render.planetRender.axesNOSE)
											renderDisk(size);
										render.planetRender.axesNOSE = axesNOSE;
									} else {
										drawIcon(icon, rp.xPosition, rp.yPosition, 0, scale);
									}
								}
								planets = planet_v;
								drawSunSpots((float[]) planet_v.get(planet_v.size() - 2));
							} else {
								if (target == TARGET.Moon)
								{
									// Obtain correct icon for the Moon
									// depending on it's phase
									double MoonElong = Math.PI * render.planetRender.ephem.phase; //render.planetRender.ephem.elongation;
									if (render.planetRender.ephem.phaseAngle < 0.0)
										MoonElong = Constant.TWO_PI - MoonElong;
									int satn = (int) (0.5+ MoonElong * 12.0 * Constant.TWO_PI_INVERSE);
									if (satn == 12) satn = 0;

									icon = "moon"+satn+".png";
									double add1 = projection.getNorthAngleAt(render.planetRender.ephem.getEquatorialLocation(), true, false);
									double add2 = -Constant.PI_OVER_TWO-render.planetRender.ephem.positionAngleOfAxis;
									//add2 = Math.PI-render.planetRender.ephem.brightLimbAngle;
									//if (render.planetRender.ephem.phaseAngle < 0.0) add2 = Math.PI+add2;
									double angle = add2 + add1;
									if (render.telescope.invertHorizontal || render.telescope.invertVertical) {
										if (render.telescope.invertHorizontal && !render.telescope.invertVertical) {
											angle = -angle;
										}
										if (render.telescope.invertVertical && !render.telescope.invertHorizontal) {
											angle = -angle;
										}
									}
									float scale = (float) Math.sqrt(render.width * 0.001f) / 3f;
									if (size > 15) scale = (float) size * 2f / 89f;
									drawIcon(icon, rp.xPosition, rp.yPosition, (float)angle, scale);
									moonVisible = false;
								} else {
									drawPlanetaryIcon(target, rp.xPosition, rp.yPosition, 0, 1.0f);
								}
							}
						} else
						{
							boolean axesNOSE = render.planetRender.axesNOSE;
							if (axesNOSE && render.coordinateSystem == COORDINATE_SYSTEM.EQUATORIAL)
								render.planetRender.axesNOSE = false;
							renderDisk(size);
							render.planetRender.axesNOSE = axesNOSE;

							if (target == TARGET.SUN)
							{
								planets = planet_v;
								drawSunSpots((float[]) planet_v.get(planet_v.size() - 2));
							}
						}
						if (render.drawPlanetsLabels)
						{
							int displacement = (int) (0.5 + p2 * render.planetRender.ephem.angularRadius);
							displacement = Math.max(32, displacement);
							FONT font = render.drawPlanetsNamesFont;
							if (render.planetRender.showLabels && (target == TARGET.SUN || target == TARGET.Moon || target == TARGET.EARTH || isVisible(render.planetRender.ephem, render.planetRender.ephemSun, this.ephem_moon_topo)))
								drawString(render.drawStarsColor, font, target.getName(), rp.xPosition,
									rp.yPosition, -displacement, false);

							if (size > RenderPlanet.minScale && render.planetRender.satellitesMain && render.planetRender.moonephem != null && rp.skySatPos != null)
							{
								font = null;
								for (int i = 0; i < render.planetRender.moonephem.length; i++)
								{
									pos0 = rp.skySatPos.get(i);
									if ((!render.planetRender.moonephem[i].eclipsed && !render.planetRender.moonephem[i].occulted || !render.planetRender.textures) && !projection
											.isInvalid(pos0) && isVisible(EphemElement.parseMoonEphemElement(render.planetRender.moonephem[i], jd), render.planetRender.ephemSun, this.ephem_moon_topo))
									{
										displacement = Math.max(20, (int) (0.5 + p2 * render.planetRender.moonephem[i].angularRadius));
										if (field < showAllBelowField && render.drawClever) displacement = -displacement;
										String label = render.planetRender.moonephem[i].name;
										if (render.planetRender.moonephem[i].eclipsed) label += " ("+t163+")";
										if (render.planetRender.moonephem[i].occulted) label += " ("+t164+")";
										if (render.planetRender.showLabels) {
											if (font == null) font = FONT.getDerivedFont(render.drawPlanetsNamesFont, render.drawPlanetsNamesFont.getSize()-4, 1);
											drawString(render.drawStarsColor, font, label, pos0[0], pos0[1], displacement, false);
										}
									}
								}
							}
						}
					} else
					{
						if (target != TARGET.Pluto || fieldDeg < 50) {
							if (render.drawPlanetsLabels)
							{
								int displacement = (int) (0.5 + p2 * render.planetRender.ephem.angularRadius);
								displacement = Math.max(32, displacement);
								FONT font = render.drawPlanetsNamesFont;
								if (fieldDeg < 1) font = FONT.getDerivedFont(render.drawPlanetsNamesFont, render.drawPlanetsNamesFont.getSize(), 1);
								if (displacement < font.getSize()) displacement = font.getSize();
								if (render.planetRender.showLabels && (target == TARGET.SUN || target == TARGET.Moon || target == TARGET.EARTH || isVisible(render.planetRender.ephem, render.planetRender.ephemSun, this.ephem_moon_topo))) drawString(render.drawStarsColor, font, target.getName(), rp.xPosition,
										rp.yPosition, -displacement, false);
							}

							if (render.drawIcons) {
								float scale = (float) Math.sqrt(render.width * 0.001f) / 1.5f;
								drawPlanetaryIcon(target, rp.xPosition, rp.yPosition, 0f, scale);
							} else {
								g.setColor(render.drawStarsColor, true);
								if (render.planetRender.ephem.magnitude < maglim) {
									this.drawStar((int)(adds+0.5+1.5f*getSizeForAGivenMagnitude(render.planetRender.ephem.magnitude)), new float[] {rp.xPosition, rp.yPosition}, 0, -1, g);
								//} else {
								//	if (size < 1) size = 1;
								//	this.drawStar((int)(adds+0.5+1.5*size), new float[] {rp.xPosition, rp.yPosition}, 0, -1, g);
								//	//g.fillOval((int)(rp.xPosition-size), (int)(rp.yPosition-size), (int)(2*size+1), (int)(2*size+1));
								}
							}
						}
					}
				}
			}

			/*
			 * Now draw Earth's shadow cone to simulate possible lunar eclipse.
			 * We apply a simple method: Earth's cone shadow is positioned in
			 * the opposite direction respect to the geocentric Sun. We establish
			 * a distance equal to the Moon's distance, and then, we apply a
			 * topocentric correction and a correction for the size of the
			 * Earth's shadow cone based on the distance to the Moon. Result can
			 * be considered as accurate (graphically speaking) to within a few
			 * seconds, since the Earth is not a perfect sphere and the effects
			 * of the atmosphere, although they are considered here, are not
			 * always the same.
			 */
			if (firstTime || ephem == null) {
				if (eph == null) eph = projection.eph.clone();
				eph.targetBody = TARGET.Moon;
				render.planetRender.ephem = Ephem.getEphemeris(projection.time, projection.obs, eph, false);
				eph.isTopocentric = false;
				ephem_moon = Ephem.getEphemeris(projection.time, projection.obs, eph, false);
				eph.targetBody = TARGET.SUN;
				eph.isTopocentric = false;
				ephem = Ephem.getEphemeris(projection.time, projection.obs, eph, false);
				sun_size = ephem.angularRadius;
				ephem.rightAscension += Math.PI;
				ephem.declination = -ephem.declination;
				ephem.distance = ephem_moon.distance;
				shadow_loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
				EarthShadowConeSize = TARGET.EARTH.equatorialRadius / (Constant.AU * Math
						.tan(ephem.angularRadius));
				if (projection.eph.isTopocentric)
					ephem = Ephem.topocentricCorrection(projection.time, projection.obs, eph, ephem);

				/*
				 * The main calculation is to position the center of the
				 * Earth shadow cone. We consider this cone to be indeed an
				 * oval with a size slightly larger than the Earth's
				 * equatorial and polar radius. This excess can be
				 * understood taking into account Earth surface elevation
				 * and opacity of the atmosphere. Values fitted to lunar
				 * eclipses in 2007. It seems to work pretty well for any other
				 * eclipse.
				 */
				double val_eq = 1.0131;
				double val_pol = 1.015;
				double ang_radius_max = FastMath.atan2_accurate(TARGET.EARTH.equatorialRadius / Constant.AU,
						ephem_moon.distance) * (val_eq - (ephem_moon.distance / EarthShadowConeSize));
				double ang_radius_min = FastMath.atan2_accurate(TARGET.EARTH.equatorialRadius / Constant.AU,
						ephem_moon.distance) * (val_pol - (ephem_moon.distance / EarthShadowConeSize));
				double penumbra_ang_radius = 2.0 * sun_size;
				penumbra_scale_max0 = ang_radius_max + penumbra_ang_radius;
				penumbra_scale_min0 = ang_radius_min + penumbra_ang_radius;

				scale_max0 = ang_radius_max;
				scale_min0 = ang_radius_min;

				LocationElement moon_loc = new LocationElement(ephem_moon.rightAscension, ephem_moon.declination,
						1.0);
				cone_angle_PA = 3.0 * Constant.PI_OVER_TWO - LocationElement.getPositionAngle(moon_loc, shadow_loc);

				moon_size0 = render.planetRender.ephem.angularRadius;

				if (projection.obs.getMotherBody() != TARGET.EARTH) {
					eph.targetBody = TARGET.EARTH;
					ObserverElement obs = ObserverElement.parseExtraterrestrialObserver(new ExtraterrestrialObserverElement("geocentric moon", TARGET.Moon));
					eph.isTopocentric = false;
					ephemEarth = Ephem.getEphemeris(projection.time, obs, eph, false);
					eph.targetBody = TARGET.SUN;
					ephemEarth2 = Ephem.getEphemeris(projection.time, obs, eph, false);
				}
			}
			if (((pixels_per_degree >= 10 && moonVisible) || projection.obs.getMotherBody() != TARGET.EARTH) && !RenderPlanet.repaint)
			{
				if (ephem_moon_topo.elongation < Constant.PI_OVER_TWO && projection.obs.getMotherBody() == TARGET.EARTH) {
					rp.renderingSky = false;
					return planet_v;
				}

				double moon_size = (pixels_per_radian * moon_size0) + 5;
				if (moon_size < 15 && projection.obs.getMotherBody() == TARGET.EARTH) {
					rp.renderingSky = false;
					return planet_v;
				}
				double moon_size2 = (moon_size - 3) * (moon_size - 3);
				double scale_max = pixels_per_radian * scale_max0;
				double penumbra_scale_max = pixels_per_radian * penumbra_scale_max0;
				double scale_min = pixels_per_radian * scale_min0;
				double penumbra_scale_min = pixels_per_radian * penumbra_scale_min0;

				float dist = getDist(refz-ephem_moon.distance);

				/*
				 * Now obtain umbra and penumbra size in the
				 * current direction taking into account that
				 * the Earth is not a perfect sphere
				 */
				double s_x = FastMath.sin(cone_angle_PA) / scale_max;
				double s_y = FastMath.cos(cone_angle_PA) / scale_min;
				double s_r = 1.0 / (s_x * s_x + s_y * s_y);
				int alpha0 = 255;
				if (render.planetRender.textures && RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR >= 1f) alpha0 = 64;
				int c = 0, delta = 20; // less delta for a red eclipse, more (around 50) for a dark one
				g.setColor(render.background, false);
				int back = g.getColor();
				s_x = FastMath.sin(cone_angle_PA) / penumbra_scale_max;
				s_y = FastMath.cos(cone_angle_PA) / penumbra_scale_min;
				double s_r2 = 1.0 / (s_x * s_x + s_y * s_y);
				planets = planet_v;
				if (projection.obs.getMotherBody() != TARGET.EARTH) {
					if (eph == null) eph = projection.eph.clone();
					eph.isTopocentric = false;
					MoonEphemElement moon[] = null;
					if (lastMoons != null && (projection.obs.getMotherBody() == TARGET.Pluto || projection.obs.getMotherBody().isPlanet() || projection.obs.getMotherBody().isNaturalSatellite()))
						moon = lastMoons; //MoonEphem.calcAllSatellites(projection.time, projection.obs, eph, false);
					eph.isTopocentric = projection.eph.isTopocentric;
					g.setColor(255, 0, 0, 128);

					if (moon != null && moon.length > 0) {
						EphemElement ephem = this.ephem.clone();
						for (int i=0; i<moon.length; i++) {
							TARGET sat = Target.getID(moon[i].name);
							if (sat == projection.obs.getMotherBody()) continue;

							ephem.setEquatorialLocation(shadow_loc);
							ephem.distance = moon[i].distance;
							if (projection.eph.isTopocentric)
								ephem = Ephem.topocentricCorrection(projection.time, projection.obs, eph, ephem);
							LocationElement loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);

							ephem.setEquatorialLocation(moon[i].getEquatorialLocation());
							ephem.distance = moon[i].distance;
							if (projection.eph.isTopocentric)
								ephem = Ephem.topocentricCorrection(projection.time, projection.obs, eph, ephem);
							ephem.angularRadius = (float) FastMath.atan2_accurate(sat.equatorialRadius, ephem.distance*Constant.AU);

							double d = LocationElement.getApproximateAngularDistance(shadow_loc, moon[i].getEquatorialLocation());
							double shadowConeSize = projection.obs.getMotherBody().equatorialRadius / (Constant.AU * Math.tan(sun_size));
							double distM = moon[i].distance - projection.obs.getMotherBody().equatorialRadius / Constant.AU;
							double ang_radius_max = FastMath.atan2_accurate(projection.obs.getMotherBody().equatorialRadius * (1.005 - (distM / shadowConeSize)) / Constant.AU, moon[i].distance*Math.abs(FastMath.cos(moon[i].elongation)));
							if (d-ephem.angularRadius < ang_radius_max) {
								float pos[] = projection.projectEarthShadow(ephem.getEquatorialLocation(), 0, false);
								if (!projection.isInvalid(pos)) {
									if (d+ephem.angularRadius < ang_radius_max) {
										int size = 1 + (int) (ephem.angularRadius * pixels_per_radian);
										g.fillOval(pos[0]-size, pos[1]-size, 2*size+1, 2*size+1, false);
									} else {
										float pos1[] = projection.projectEarthShadow(loc, 0, false);
										if (!projection.isInvalid(pos)) {
											double r = FastMath.hypot(pos1[0]-pos[0], pos1[1]-pos[1])-(d-ang_radius_max)*this.pixels_per_radian;
											int size = (int) (r+0.5);
											g.fillOval(pos1[0]-size, pos1[1]-size, 2*size+1, 2*size+1, false);
										}
									}
								}
							}
						}
					}

					if (moon_size < 15) {
						rp.renderingSky = false;
						return planet_v;
					}

					if (ephemEarth.elongation < 2 * Constant.DEG_TO_RAD && moon_size > 5) {
						Object shadow = g.getImage(FileIO.DATA_TEXTURES_DIRECTORY + "earth-shadow.png");
						int factorQ = 4; // Resample texture, since it has low resolution
						int sizei[] = g.getSize(shadow);
						shadow = g.getScaledImage(shadow, sizei[0]*factorQ, 4, false, true);
						int refC = 157*factorQ;

						float[] pos = projection.projectEarthShadow(ephem_moon_topo.getEquatorialLocation(), 0, false);
						float xPosition = pos[0], yPosition = pos[1];
						ObserverElement obs = ObserverElement.parseExtraterrestrialObserver(new ExtraterrestrialObserverElement("geocentric moon", TARGET.Moon, new LocationElement(0.0, 0.0, 0.0)));
						TimeElement time = projection.time.clone();
						time.add(-ephem_moon_topo.distance*Constant.LIGHT_TIME_DAYS_PER_AU);
						double lst = SiderealTime.apparentSiderealTime(time, obs, projection.eph);
						double radiusAU = obs.getGeoRad() * (obs.getEllipsoid().getEquatorialRadius() / Constant.AU);
						double eq_geo[] = LocationElement.parseLocationElement(ephemEarth.getEquatorialLocation());
						for (int iy = (int)(yPosition - moon_size); iy < (int)(yPosition + moon_size); iy++)
						{
							for (int ix = (int)(xPosition - moon_size); ix < (int)(xPosition + moon_size); ix++)
							{
								if (ix >= 0 && ix < render.width && iy >= 0 && iy < render.height)
								{
									double r2 = FastMath.pow(ix - (int)xPosition, 2.0) + FastMath.pow(iy - (int)yPosition, 2.0);
									if (r2 <= moon_size2 && g.getRGB(ix, iy) != back) {
										LocationElement moonLoc = this.getPlanetographicPosition(ix, iy, 3, true);
										if (moonLoc == null || ((int)moonLoc.getRadius()) != TARGET.Moon.ordinal()) continue;

										obs.setLongitudeRad(moonLoc.getLongitude());
										obs.setLatitudeRad(moonLoc.getLatitude());

										// Fast topocentric correction
										double cosGeoLat = FastMath.cos(obs.getGeoLat());
										double topox = radiusAU * FastMath.cos(lst + obs.getLongitudeRad());
										double topoy = radiusAU * FastMath.sin(lst + obs.getLongitudeRad());
										double correction[] = new double[] {topox * cosGeoLat, topoy * cosGeoLat, radiusAU * FastMath.sin(obs.getGeoLat())};
										double xtopo = eq_geo[0] - correction[0];
										double ytopo = eq_geo[1] - correction[1];
										double ztopo = eq_geo[2] - correction[2];
										LocationElement loc = LocationElement.parseRectangularCoordinatesFast(new double[] {xtopo, ytopo, ztopo});
										ephem.setEquatorialLocation(loc);

										if (LocationElement.getApproximateAngularDistance(loc, new LocationElement(lst + obs.getLongitudeRad(), loc.getLatitude(), 1.0)) > Constant.PI_OVER_TWO) continue;

										ephem.elongation = (float) LocationElement.getApproximateAngularDistance(loc, ephemEarth2.getEquatorialLocation());
										ephem.angularRadius = (float) FastMath.atan2_accurate(TARGET.EARTH.equatorialRadius, ephem.distance * Constant.AU);

										double r1 = s_r + (ephem.elongation-ephem.angularRadius)*s_r/ephem.angularRadius;
										//System.out.println(ephem.elongation*Constant.RAD_TO_DEG+"/"+ephem.angularRadius*Constant.RAD_TO_DEG+"/"+r1+"/"+s_r2+"/"+s_r);

										if (render.planetRender.textures && RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR >= 1f) {
											if (r1 <= s_r2)
											{
												if (moon_size < 15) {
													rp.renderingSky = false;
													return planet_v;
												}
												g.disableInversion();
												if (r1 <= s_r) {
													c = (int) (refC * Math.sqrt(r1 / s_r));
													c = g.getRGB(shadow, c, 2);
													g.setColor(
															Math.max(0, g.getRed(c)-delta),
															Math.max(0, g.getGreen(c)-delta),
															Math.max(0, g.getBlue(c)-delta),
															255
															);
												} else {
													double ss_r = Math.sqrt(s_r);
													double frac = (Math.sqrt(r1) - ss_r) / (Math.sqrt(s_r2) - ss_r);
													c = refC + (int) ((256 * factorQ - 1 - refC) * frac);
													int alpha = 16;
													if (c < refC+10*factorQ) alpha = 16 + (refC+10*factorQ-c) * 230 / (factorQ * 10);
													if (alpha > 255) alpha = 255;
													c = g.getRGB(shadow, c, 2);
													g.setColor(
															Math.max(0, g.getRed(c)-delta),
															Math.max(0, g.getGreen(c)-delta),
															Math.max(0, g.getBlue(c)-delta),
															alpha);
												}
												g.enableInversion();
												if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
													g.fillRect(ix, iy, 1, 1);
												} else {
													g.fillRect(ix, iy, 1, 1, dist);
												}
											}
										} else {
											if (s_r >= r1) {
												g.setColor(0, alpha0);
												if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
													g.fillRect(ix, iy, 1, 1);
												} else {
													g.fillRect(ix, iy, 1, 1, dist);
												}
											}
										}
									}
								}
							}
						}
						RenderPlanet.updateRendering(g, (float)moon_size);
					}
				} else {
					LocationElement loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
					float pos[] = projection.projectEarthShadow(loc, (int) (render.planetRender.ephem.angularRadius * pixels_per_radian * 6.0), true); // 6 since Earth's shadow cone is about 4 times the size of the Moon
					if (!projection.isInvalid(pos))
					{
						if (!render.planetRender.textures) {
							if (projection.isCylindricalForced()) {
								if (render.drawSkyCorrectingLocalHorizon && projection.obs.getMotherBody() == TARGET.EARTH && projection.eph.isTopocentric) {
									LocationElement locEq = shadow_loc;
									LocationElement locH = projection.getApparentLocationInSelectedCoordinateSystem(locEq, false, true, COORDINATE_SYSTEM.HORIZONTAL, 0);
									double elev = locH.getLatitude(), angrad = scale_max0;
									double geoElev0 = Ephem.getGeometricElevation(projection.eph, projection.obs, elev);
									double geoElevp = geoElev0 + angrad;
									double appElevp = Ephem.getApparentElevation(projection.eph, projection.obs, geoElevp, 10);
									double dp = (appElevp - elev);
									float upperLimbFactor = (float) (dp / angrad);
									int de = (int) (scale_max * (1.0 - upperLimbFactor) + 0.5);
									if (de > 1) {
										Graphics gg = g.getGraphics((int)(2*scale_max), (int)(2*scale_max));
										gg.setColor(0, 0, 0, 128);
										gg.fillOval(0, 0, 2*(float)scale_max, 2*(float)scale_max, false);
										Object img = gg.getRendering();

										float angr = (float) (projection.getCenitAngleAt(locEq, true) - Constant.PI_OVER_TWO);
										float radius_x = (float) scale_max, radius_y = (float) scale_max;
										img = g.getRotatedAndScaledImage(img, radius_x, radius_y, angr, 1.0f, 1.0f);
										int size[] = g.getSize(img);
										int newH = (int)(size[1]*upperLimbFactor);
										int displ = (size[1] - newH) / 2;
										img = g.getScaledImage(img, size[0], newH, false, false, displ);
										img = g.getRotatedAndScaledImage(img, radius_x, radius_y, -angr, 1.0f, 1.0f);
										g.drawImage(img, pos[0]-radius_x, pos[1]-newH/2-displ);
									} else {
										g.setColor(0, 0, 0, 128);
										g.fillOval(pos[0]-(float)scale_max, pos[1]-(float)scale_max, 2*(float)scale_max, 2*(float)scale_max, false);
									}
								} else {
									g.setColor(0, 0, 0, 128);
									g.fillOval(pos[0]-(float)scale_max, pos[1]-(float)scale_max, 2*(float)scale_max, 2*(float)scale_max, false);
								}
							}
						} else {
							Object shadow = g.getImage(FileIO.DATA_TEXTURES_DIRECTORY + "earth-shadow.png");
							int step = g.renderingToAndroid() ? 2 : 1;
							int factorQ = 4 / step; // Resample texture, since it has low resolution
							int sizei[] = g.getSize(shadow);
							shadow = g.getScaledImage(shadow, sizei[0]*factorQ, 4, false, true);
							int refC = 157*factorQ;

							float upperLimbFactor = 1, angr = 0;
							if (render.drawSkyCorrectingLocalHorizon && projection.obs.getMotherBody() == TARGET.EARTH && projection.eph.isTopocentric) {
								LocationElement locEq = shadow_loc;
								LocationElement locH = projection.getApparentLocationInSelectedCoordinateSystem(locEq, false, true, COORDINATE_SYSTEM.HORIZONTAL, 0);
								double elev = locH.getLatitude(), angrad = scale_max0;
								double geoElev0 = Ephem.getGeometricElevation(projection.eph, projection.obs, elev);
								double geoElevp = geoElev0 + angrad;
								double appElevp = Ephem.getApparentElevation(projection.eph, projection.obs, geoElevp, 10);
								double dp = (appElevp - elev);
								upperLimbFactor = (float) (dp / angrad);
								angr = (float) (projection.getCenitAngleAt(locEq, true) - Constant.PI_OVER_TWO);
							}
							for (int iy = (int)(rp.yPosition - moon_size); iy < (int)(rp.yPosition + moon_size); iy = iy + step)
							{
								for (int ix = (int)(rp.xPosition - moon_size); ix < (int)(rp.xPosition + moon_size); ix = ix + step)
								{
									if (ix >= 0 && ix < render.width && iy >= 0 && iy < render.height)
									{
										double r2 = FastMath.pow(ix - (int)rp.xPosition, 2.0) + FastMath.pow(iy - (int)rp.yPosition, 2.0)/upperLimbFactor;
										if (r2 <= moon_size2 && g.getRGB(ix, iy) != back) {
											//double dang = Math.PI + FastMath.atan2_accurate(-(iy - pos[1]), ix - pos[0]) - angr;
											double dang = -Constant.PI_OVER_TWO - FastMath.atan2_accurate(-(iy - pos[1]), ix - pos[0]) - angr;
											double corr = 1.0 + (upperLimbFactor - 1.0) * Math.abs(FastMath.cos(dang));
											double r1 = (FastMath.pow(ix - pos[0], 2.0) + FastMath.pow(iy - pos[1], 2.0))/corr;
											if (render.planetRender.textures && RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR >= 1f) {
												if (r1 <= s_r2)
												{
													if (moon_size < 15) {
														rp.renderingSky = false;
														return planet_v;
													}
													int alpha = 255;
													g.disableInversion();
													if (r1 <= s_r) {
														c = (int) (refC * Math.sqrt(r1 / s_r));
														c = g.getRGB(shadow, c, 2);
													} else {
														double ss_r = Math.sqrt(s_r);
														double frac = (Math.sqrt(r1) - ss_r) / (Math.sqrt(s_r2) - ss_r);
														c = refC + (int) ((256 * factorQ - 1 - refC) * frac);
														alpha = 16;
														if (c < refC+10*factorQ) alpha = 16 + (refC+10*factorQ-c) * 230 / (factorQ * 10);
														if (alpha > 255) alpha = 255;
														c = g.getRGB(shadow, c, 2);
													}
													g.enableInversion();
													if (alpha > 232) alpha = 232;
													g.setColor(
															Math.max(0, g.getRed(c)-delta),
															Math.max(0, g.getGreen(c)-delta),
															Math.max(0, g.getBlue(c)-delta),
															alpha);
													if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
														g.fillRect(ix, iy, step, step);
													} else {
														g.fillRect(ix, iy, step, step, dist);
													}
												}
											} else {
												if (s_r >= r1) {
													g.setColor(0, alpha0);
													if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
														g.fillRect(ix, iy, step, step);
													} else {
														g.fillRect(ix, iy, step, step, dist);
													}
												}
											}
										}
									}
								}
							}
							RenderPlanet.updateRendering(g, (float)moon_size);
						}
					}
				}
			}
		}
		rp.renderingSky = false;

		return planet_v;
	}

	private void renderDisk(double size) throws Exception {
		rp.render = render.planetRender;
		render.planetRender.foreground = render.getColorMode() == COLOR_MODE.NIGHT_MODE ? 255<<24 | 255<<16 | 0<<8 | 0 : g.invertColor(this.render.background);
		render.planetRender.width = render.width;
		render.planetRender.height = render.height;
		render.planetRender.northUp = false;
		render.planetRender.telescope = render.telescope;
        if (forcedFOV) {
	        render.planetRender.telescope = TelescopeElement.SCHMIDT_CASSEGRAIN_20cm;
	        render.planetRender.telescope.invertHorizontal = render.telescope.invertHorizontal;
	        render.planetRender.telescope.invertVertical = render.telescope.invertVertical;
	        render.planetRender.telescope.ocular.focalLength = TelescopeElement.getOcularFocalLengthForCertainField(field, render.planetRender.telescope);
        }
		rp.northAngle = 0;
		if (render.poleAngle != 0.0 || render.coordinateSystem != COORDINATE_SYSTEM.EQUATORIAL
				|| projection.obs.getMotherBody() != TARGET.EARTH)
			rp.northAngle = (-Constant.PI_OVER_TWO+projection.getNorthAngleAt(render.planetRender.ephem.getEquatorialLocation(), true, false));
		rp.hugeFactor = this.hugeFactor;
		if (rp.render.background != render.background) {
			rp.render.background = render.background;
			rp.render.foreground = g.invertColor(this.render.background);
		}
		if (projection.obs.getMotherBody() == TARGET.EARTH && projection.eph.isTopocentric && render.drawSkyCorrectingLocalHorizon) {
			float angr = (float) (projection.getCenitAngleAt(render.planetRender.ephem.getEquatorialLocation(), true) - Constant.PI_OVER_TWO);
			rp.refractionCorrection(projection.eph, projection.obs, angr);
		}

		rp.motherBody = projection.obs.getMotherBody().getName();
		if (((size <= 5 || (!projection.isCylindricalForced() && projection.render.projection != PROJECTION.CYLINDRICAL_EQUIDISTANT))) && render.planetRender.textures) {
			if (projection.obs.getMotherBody() == TARGET.EARTH && size > 5) rp.render.textures = false;
			if (projection.obs.getMotherBody() != TARGET.EARTH) RenderPlanet.dateChanged();
			rp.renderize(g);
			rp.render.textures = true;
			if (projection.obs.getMotherBody() != TARGET.EARTH) RenderPlanet.dateChanged();
		} else {
			rp.illuminateMoonByEarth = true;
			if (projection.obs.getMotherBody() != TARGET.EARTH || rp.render.target != TARGET.Moon) rp.illuminateMoonByEarth = false;
			if ((rp.render.target.isPlanet() && rp.render.target != TARGET.SUN) && rp.render.target == projection.obs.getMotherBody())
				rp.illuminateMoonByEarth = true;
			rp.renderize(g);
		}
	}

	private double scale_max0, scale_min0, penumbra_scale_max0, penumbra_scale_min0, cone_angle_PA,
		EarthShadowConeSize, sun_size, moon_size0;
	private LocationElement shadow_loc;
	private EphemElement ephem, ephem_moon, ephem_moon_topo, ephemEarth, ephemEarth2;

	private boolean isVisible(EphemElement ephem, EphemElement sun, EphemElement moon) {
		boolean visible = true;

		try {
			if (ephem.elongation < sun.angularRadius + ephem.angularRadius && ephem.distance > sun.distance) visible = false;
			if (visible) {
				double elong = LocationElement.getApproximateAngularDistance(ephem.getEquatorialLocation(), moon.getEquatorialLocation());
				if (elong < moon.angularRadius + ephem.angularRadius && ephem.distance > moon.distance) visible = false;
			}
		} catch (Exception exc) {}
		return visible;
	}

	private LocationElement loc0Date = null, loc0J2000 = null;
	private static int maxStars = -1;
	// To be called whenever the field increases or the time changes
	private void drawConstelAndStars() throws JPARSECException
	{
		if (render.drawStars || fieldDeg > 1 && render.drawConstellationContours != CONSTELLATION_CONTOUR.NONE)
		{
			Object[] readStars = null;
			int indexf = 0;
			if (re_star != null) {
				readStars = re_star.getReadElements();
				if (readStars != null) indexf = readStars.length;
			}

			if (re_star == null || indexf == 0 || (drawAll && nstars == -1)) {
				re_star = readStars(projection, false);
				readStars = re_star.getReadElements();
				indexf = readStars.length;
			}

			int deg30 = 0;
			if (fieldDeg > 1 && render.drawConstellationContours != CONSTELLATION_CONTOUR.NONE)
				deg30 = (int)(30 * pixels_per_degree);

			maxStars = -1;
			float minX = rec.getMinX(), minY = rec.getMinY(), maxX = rec.getMaxX(), maxY = rec.getMaxY();
			for (int iii = 0; iii < indexf; iii++)
			{
				if (readStars[iii] != null) {
					StarData sd = (StarData)readStars[iii];
					if (sd.mag[0] > maglim) {
						if (maxStars == -1) maxStars = iii;
						if (iii >= 4255 || db_conlin >= 0) break;
					}

					float pos[] = projection.projectPosition(sd.loc, 0, false);
					if (pos != null && !this.isInTheScreen(pos[0], pos[1], deg30, minX, minY, maxX, maxY)) pos = null;
					if (pos != null) {
						if (sd.pos == null) {
							sd.pos = new float[] {pos[0], pos[1]};
						} else {
							sd.pos[0] = pos[0];
							sd.pos[1] = pos[1];
						}
					} else {
						sd.pos = pos;
					}
				}
			}
			if (maxStars == -1) maxStars = indexf;

			if (fieldDeg > 1 && render.drawConstellationContours != CONSTELLATION_CONTOUR.NONE) {
				ArrayList<Object> conlin = null;
				Object o = null;
				if (db_conlin >= 0) {
					o = DataBase.getData(db_conlin);
				} else {
					o = DataBase.getData("conlin", threadID, true);
				}
				if (o != null) conlin = new ArrayList<Object>(Arrays.asList((Object[]) o));
				if (conlin == null) {
					String culture = "";
					if (render.drawConstellationContours != CONSTELLATION_CONTOUR.DEFAULT) culture = "_"+render.drawConstellationContours.name();
					ArrayList<String> conlinFile = ReadFile.readResource(FileIO.DATA_SKY_DIRECTORY + "conlin"+culture+".txt");
					conlin = new ArrayList<Object>();
					int rs = conlinFile.size();
					int maxStars2 = Math.min(readStars.length, Math.max(maxStars, 4255));
					for (int i = 0; i < rs; i++)
					{
						String line = conlinFile.get(i);
						int origin_star = Integer.parseInt(FileIO.getField(1, line, " ", true));
						int destination_star = Integer.parseInt(FileIO.getField(2, line, " ", true));

						if (origin_star >= 0 && destination_star >= 0 && (origin_star < maxStars2 && destination_star < maxStars2))
						{
							StarData sd0 = (StarData)readStars[origin_star];
							if (sd0 == null) continue;
							StarData sd1 = (StarData)readStars[destination_star];
							if (sd1 == null) continue;

							conlin.add(new int[] {origin_star, destination_star});
						}
					}

					DataBase.addData("conlin", threadID, conlin.toArray(), true);
					db_conlin = DataBase.getIndex("conlin", threadID);
				}
				g.setColor(render.drawConstellationContoursColor, true);

				float pos0[] = new float[2], pos1[];
				int origin_star, destination_star, line[];
				float rr, margin = render.drawConstellationContoursMarginBetweenLineAndStar;
				if (render.drawClever && this.pixels_per_degree < 10) margin = 0;
				float marginHalf = margin/2;
				if (hugeFactor > 1) {
					margin *= 2;
					marginHalf *= 2;
				}
				g.setStroke(render.drawConstellationStroke);
				boolean fastMode = render.drawFastLinesMode.fastConstellations();
				if (fastMode) {
					if (!render.drawConstellationStroke.isContinuousLine()) fastMode = false;
					if (render.drawConstellationStroke.getLineWidth() >= 2) fastMode = false;
					if (render.anaglyphMode != ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) fastMode = false;
				}

				//Object p = null;
				//if (!fastMode &&render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) p = g.generalPathInitialize();
				int m = conlin.size();
				float dx = 0, dy = 0;
				for (int s = 0; s<m; s++)
				{
					line = (int[]) conlin.get(s);
					origin_star = line[0];
					destination_star = line[1];

					if (origin_star >= 0 && destination_star >= 0 && (origin_star < maxStars && destination_star < maxStars))
					{
						StarData sd0 = (StarData)readStars[origin_star];
						if (sd0 == null || sd0.pos == null) continue;
						StarData sd1 = (StarData)readStars[destination_star];
						if (sd1 == null || sd1.pos == null) continue;
						pos0 = sd0.pos;
						pos1 = sd1.pos;

						if (!render.telescope.invertHorizontal && !render.telescope.invertVertical &&
								!rec.isLineIntersectingRectangle(pos0[0], pos0[1], pos1[0], pos1[1])) continue;

						rr = (float) FastMath.hypot(pos0[0]-pos1[0], pos0[1]-pos1[1]);
						if (rr <= margin || rr >= pixels_per_degree_50) continue;
						if (margin > 0) {
							rr = (float) (margin * 0.5 / rr);
							dx = (pos1[0]-pos0[0]) * rr;
							dy = (pos1[1]-pos0[1]) * rr;
							if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
								g.drawLine(pos0[0] + dx, pos0[1] + dy, pos1[0] - dx, pos1[1] - dy, fastMode);
							} else {
								g.drawLine(pos0[0] + dx, pos0[1] + dy, pos1[0] - dx, pos1[1] - dy, getDistStar(sd0.loc.getRadius()), getDistStar(sd1.loc.getRadius()));
							}
						} else {
							if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
								g.drawLine(pos0[0], pos0[1], pos1[0], pos1[1], fastMode);
							} else {
								g.drawLine(pos0[0], pos0[1], pos1[0], pos1[1], getDistStar(sd0.loc.getRadius()), getDistStar(sd1.loc.getRadius()));
							}
						}
/*						if (fastMode) {
							rr = (float) (margin * 0.5 / rr);
							float dx = (pos1[0]-pos0[0]) * rr;
							float dy = (pos1[1]-pos0[1]) * rr;
							g.drawLine(pos0[0] + dx, pos0[1] + dy, pos1[0] - dx, pos1[1] - dy, true);
						} else {
							g.setStroke(new JPARSECStroke(render.drawConstellationStroke, new float[] {rr-margin, margin}, rr-marginHalf));

							if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
								//g.generalPathMoveTo(p, pos0[0], pos0[1]);
								//g.generalPathLineTo(p, pos1[0], pos1[1]);
								g.drawLine(pos0[0], pos0[1], pos1[0], pos1[1], false);
							} else {
								g.drawLine(pos0[0], pos0[1], pos1[0], pos1[1], getDistStar(sd0.loc.getRadius()), getDistStar(sd1.loc.getRadius()));
							}
						}
*/
					}
				}
				//if (!fastMode && render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) g.draw(p);
			}

			if (!render.drawStars) return;

			float size = 0;

	 		boolean labels = false;
			if (render.drawStarsLabels != SkyRenderElement.STAR_LABELS.NONE) labels = true;
			if (pixels_per_degree < 6.0 && render.drawClever) labels = false;

			Object o = null;
			if (db_starNames >= 0) {
				o = DataBase.getData(db_starNames);
			} else {
				o = DataBase.getData("starNames", threadID, true);
			}
			String[] names = null;
			ArrayList<String> names2 = null;
			if (render.drawStarsLabels != SkyRenderElement.STAR_LABELS.NONE && labels) {
				if (o == null) {
					names = DataSet.arrayListToStringArray(ReadFile.readResource(FileIO.DATA_SKY_DIRECTORY + "star_names.txt"));
					String n2[] = DataSet.extractColumnFromTable(names, ";", 0);
					names2 = new ArrayList<String>(Arrays.asList(n2));

					if (render.drawStarsLabels == SkyRenderElement.STAR_LABELS.ONLY_PROPER_NAME_SPANISH) {
						names = DataSet.extractColumnFromTable(names, ";", 2);
					} else {
						names = DataSet.extractColumnFromTable(names, ";", 1);
					}
					DataBase.addData("starNames", threadID, names, true);
					DataBase.addData("starNames2", threadID, n2, true);
					db_starNames = DataBase.getIndex("starNames", threadID);
					db_starNames2 = DataBase.getIndex("starNames2", threadID);
				} else {
					names = (String[]) o;
					Object o2 = null;
					if (db_starNames2 >= 0) {
						o2 = DataBase.getData(db_starNames2);
					} else {
						o2 = DataBase.getData("starNames2", threadID, true);
					}
					names2 = new ArrayList<String>(Arrays.asList((String[])o2));
				}
			}

			float newSize = 0;
			ArrayList<String> sn = new ArrayList<String>();
			int fontSize = render.drawStarsNamesFont.getSize();
			float pos[], mag, sep, maxMag, pa;
			int iii, n;
			float dist = 0, position;
			String name, label;
			g.setColor(render.drawStarsColor, true);

			boolean joinGreekAndName = true;
			if (!render.drawStarsGreekSymbolsOnlyIfHasProperName && render.drawFastLabels != SUPERIMPOSED_LABELS.FAST &&
					(pixels_per_degree >= 10 || !render.drawClever || !render.drawFastLabelsInWideFields)) joinGreekAndName = false;
			int halo = 2, minSizeVariable = 2, halo2 = 4; //2*halo;
			if (render.getColorMode() == COLOR_MODE.PRINT_MODE || g.renderingToExternalGraphics() || g.renderingToAndroid()) minSizeVariable = 3;
			FONT greekFont = render.drawStarsNamesFont;
			if (g.renderingToExternalGraphics()) greekFont = FONT.getDerivedFont(render.drawStarsNamesFont, "Symbol");
			if (render.drawClever && render.drawStarsLabelsLimitingMagnitude > maglim-2) render.drawStarsLabelsLimitingMagnitude = maglim-2f;
			g.setStroke(JPARSECStroke.STROKE_DEFAULT_LINE_THIN);
			int colIndex = -1, colS = Functions.getColor(255, 255, 255, 156);
			float dadd = g.renderingToExternalGraphics() ? 0: 0.5f;

	    		int col1 = g.getRGB(3, 3);
	    		int red1 = g.getRed(col1);
	    		int green1 = g.getGreen(col1);
	    		int blue1 = g.getBlue(col1);
	    		float masking_factor = 1.2f-(156)/255.0f;
	    		if (green1 > 150) masking_factor += 0.15f;
	    		int red = (int) (255 * (1 - masking_factor) + red1 * masking_factor);
	    		int green = (int) (255 * (1 - masking_factor) + green1 * masking_factor);
	    		int blue = (int) (255 * (1 - masking_factor) + blue1 * masking_factor);
	    		colS = 255<<24 | red<<16 | green<<8 | blue;

 			for (iii = 0; iii < maxStars; iii++)
			{
				StarData sd = (StarData) readStars[iii];
				if (sd == null) continue;
				pos = sd.pos;

				if (!projection.isInvalid(pos))
				{
					if (!this.isInTheScreen((int)pos[0], (int)pos[1])) continue;
					mag = sd.mag[sd.mag.length-1];

			    	if (render.anaglyphMode != ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) dist = getDistStar(sd.loc.getRadius());
					size = getSizeForAGivenMagnitude(mag);
					if (!g.renderingToExternalGraphics()) {
						size = (int) (size);
						pos[0] = (int) pos[0];
						pos[1] = (int) pos[1];
					}

					if (render.drawStarsRealistic != REALISTIC_STARS.NONE &&
							render.drawStarsRealistic != REALISTIC_STARS.NONE_CUTE) {
						int tsize = (int) (0.5 + (1.5f * size+adds));
/*						if (tsize > 1 && render.drawStarsColors && !sd.sp.isEmpty())
						{
							if (sd.spi >= 0) {
								g.setColor(col[sd.spi], true);
							} else {
				    			g.setColor(render.drawStarsColor, false);
							}
						}
*/
			    		if (tsize <= 1) {
			    			if (render.getColorMode() == COLOR_MODE.NIGHT_MODE || render.getColorMode() == COLOR_MODE.BLACK_BACKGROUND) {
						    	if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH && !g.renderingToExternalGraphics()) {
						    		g.drawPoint((int)pos[0], (int)pos[1], colS);
						    	} else {
				    				if (g.renderingToExternalGraphics()) {
						    			g.fillOval(pos[0]-1, pos[1]-1, 2, 2, false);
				    				} else {
					    				g.setColor(colS, true);
				    					g.fillOval(pos[0], pos[1], 1, 1, dist);
				    				}
						    	}
			    			} else {
						    	if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH && !g.renderingToExternalGraphics()) {
						    		g.drawPoint((int)pos[0], (int)pos[1], g.getColor());
						    	} else {
						    		if (g.renderingToExternalGraphics()) {
						    			g.fillOval(pos[0]-1, pos[1]-1, 2, 2, false);
						    		} else {
						    			g.fillOval(pos[0], pos[1], 1, 1, dist);
						    		}
						    	}
			    			}
			    		} else {
			    			drawStar(tsize, pos, dist, sd.spi/10, g);
			    		}
					} else {
						int tsize = (int) (2 * size+adds);
						size = tsize/2f-0.5f; //(tsize-adds)/2f;
						colIndex = 2;
						if (render.getColorMode() == COLOR_MODE.NIGHT_MODE) colIndex = 6;

						if (render.drawStarsColors && sd.spi >= 0) {
							colIndex = sd.spi / 10;
							g.setColor(col[colIndex], false);
						}

						float tx = (pos[0]) - size;
						float ty = (pos[1]) - size;

						if (render.drawStarsSymbols && size > 1 && (maglim-mag) > 2 && sd.doub != null) //(type.equals("D") || type.equals("B") || type.equals("V")))
						{
							sep = pa = 0;
							if (sd.doub != null) {
								sep = sd.doub[0];
								pa = sd.doub[1];
							}

							maxMag = mag;
							newSize = size+minSizeVariable;
							if (sd.var != null) {
								if (sd.var[0] - sd.var[1] > render.limitOfDifferenceOfMagnitudesForVariableStars)
									mag = sd.var[0];
								maxMag = sd.var[1];
								newSize = getSizeForAGivenMagnitude(maxMag);
								if (!g.renderingToExternalGraphics()) newSize = (int) newSize;
								if (Math.abs(newSize-size) < minSizeVariable) newSize = size + minSizeVariable;

								if (render.drawStarsRealistic == REALISTIC_STARS.NONE_CUTE &&
										mag-maxMag > render.limitOfDifferenceOfMagnitudesForVariableStars) {
									int c = g.getColor();
									g.setColor(render.background, false);
									int tsize2 = (int)(2 * newSize + halo) + 1;
									if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
										if (!g.renderingToExternalGraphics()) {
											this.fillOval(g, (pos[0]) - newSize-halo, (pos[1]) - newSize-halo, tsize2, tsize2, 7);
										} else {
											g.fillOval((pos[0]) - newSize-halo, (pos[1]) - newSize-halo, tsize2, tsize2, false);
										}
									} else {
										g.fillOval((pos[0]) - newSize-halo, (pos[1]) - newSize-halo, tsize2, tsize2, dist);
									}
									g.setColor(c, true);
								}

							}

							if (sd.var != null && mag-maxMag > render.limitOfDifferenceOfMagnitudesForVariableStars)
							{
								if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
									fillOval(g, tx, ty, tsize, tsize, colIndex);

									tsize = (int)(2 * newSize + 1);
									tx = (pos[0]) - newSize;
									ty = (pos[1]) - newSize;

									g.drawOval(tx, ty, tsize, tsize, render.drawFastLinesMode.fastOvals());

									if (sd.doub != null && sep > render.limitOfSeparationForDoubleStars)
									{
										if (!render.drawStarsPositionAngleInDoubles || pa == 0) {
											g.drawStraightLine(tx, (pos[1]), (pos[0]) - tsize,  pos[1]);
											g.drawStraightLine((pos[0]) + newSize, (pos[1]), (pos[0]) + tsize,  pos[1]);
										} else {
											float ang = pa + (float) projection.getNorthAngleAt(sd.loc, false, true);
											float px0 = FastMath.cosf(ang), py0 = FastMath.sinf(ang);
											g.drawLine((pos[0])-(px0*(newSize+ dadd)), (pos[1])-(py0*(newSize+ dadd)), (pos[0])-(px0*tsize),  (pos[1])-(py0*tsize), true);
										}
									}
								} else {
									g.fillOval(tx, ty, tsize, tsize, dist);

									tsize = (int)(2 * newSize+1);
									tx = (pos[0]) - newSize;
									ty = (pos[1]) - newSize;

									g.drawOval(tx, ty, tsize, tsize, dist);

									if (sd.doub != null && sep > render.limitOfSeparationForDoubleStars)
									{
										if (!render.drawStarsPositionAngleInDoubles || pa == 0) {
											g.drawStraightLine(tx, (pos[1]), (pos[0]) - tsize,  pos[1], dist, dist);
											g.drawStraightLine((pos[0]) + newSize, (pos[1]), (pos[0]) + tsize,  pos[1], dist, dist);
										} else {
											float ang = pa + (float) projection.getNorthAngleAt(sd.loc, false, true);
											float px0 = FastMath.cosf(ang), py0 = FastMath.sinf(ang);
											g.drawLine((pos[0])-(px0*(newSize+ dadd)), (pos[1])-(py0*(newSize+ dadd)), (pos[0])-(px0*tsize),  (pos[1])-(py0*tsize), dist, dist);
										}
									}
								}
							} else {
								if (render.drawStarsRealistic == REALISTIC_STARS.NONE_CUTE) {
									int c = g.getColor();
									g.setColor(render.background, false);
									if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
										if (!g.renderingToExternalGraphics()) {
											this.fillOval(g, tx-halo, ty-halo, tsize+halo2, tsize+halo2, 7);
										} else {
											g.fillOval(tx-halo, ty-halo, tsize+halo2, tsize+halo2, false);
										}
									} else {
										g.fillOval(tx-halo, ty-halo, tsize+halo2, tsize+halo2, dist);
									}
									g.setColor(c, true);
								}

								if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
									fillOval(g, tx, ty, tsize, tsize, colIndex);
									if (sd.doub != null && sep > render.limitOfSeparationForDoubleStars)
									{
										if (!render.drawStarsPositionAngleInDoubles || pa == 0) {
											g.drawStraightLine(tx, pos[1], (pos[0]) - tsize, pos[1]);
											g.drawStraightLine((pos[0]) + size, pos[1], (pos[0]) + tsize, pos[1]);
										} else {
											float ang = pa + (float) projection.getNorthAngleAt(sd.loc, false, true);
											float px0 = FastMath.cosf(ang), py0 = FastMath.sinf(ang);
											g.drawLine((pos[0])-(px0*(size+ dadd)), (pos[1])-(py0*(size+ dadd)), (pos[0])-(px0*tsize),  (pos[1])-(py0*tsize), true);
										}
									}
								} else {
									g.fillOval(tx, ty, tsize, tsize, dist);
									if (sd.doub != null && sep > render.limitOfSeparationForDoubleStars)
									{
										if (!render.drawStarsPositionAngleInDoubles || pa == 0) {
											g.drawStraightLine(tx, pos[1], (pos[0]) - tsize, pos[1], dist, dist);
											g.drawStraightLine((pos[0]) + size, pos[1], (pos[0]) + tsize, pos[1], dist, dist);
										} else {
											float ang = pa + (float) projection.getNorthAngleAt(sd.loc, false, true);
											float px0 = FastMath.cosf(ang), py0 = FastMath.sinf(ang);
											g.drawLine((pos[0])-(px0*(size+ dadd)), (pos[1])-(py0*(size+ dadd)), (pos[0])-(px0*tsize),  (pos[1])-(py0*tsize), dist, dist);
										}
									}
								}
							}
						} else {
							if (render.drawStarsRealistic == REALISTIC_STARS.NONE_CUTE) {
								int c = g.getColor();
								g.setColor(render.background, false);
								if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
									if (g.renderingToExternalGraphics()) {
										g.fillOval(tx-halo, ty-halo, tsize+halo2, tsize+halo2, false);
									} else {
										fillOval(g, tx-halo, ty-halo, tsize+halo2, tsize+halo2, 7);
									}
								} else {
									g.fillOval(tx-halo, ty-halo, tsize+halo2, tsize+halo2, dist);
								}
								g.setColor(c, true);
							}

							if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
								fillOval(g, tx, ty, tsize, tsize, colIndex);
							} else {
								g.fillOval(tx, ty, tsize, tsize, dist);
							}
						}
					}

					position = 0;
	 				if (sd.nom2 != null && labels && sd.mag[0] < render.drawStarsLabelsLimitingMagnitude && render.drawStarsLabels != SkyRenderElement.STAR_LABELS.NONE)
					{
						name = sd.nom2;
						if (sn.contains(name)) continue;

						position = Math.max(size * 3, size+fontSize);
						if (render.drawStarsLabels != SkyRenderElement.STAR_LABELS.NORMAL)
						{
							n = names2.indexOf(name);
							if (n >= 0) {
								sn.add(name);
								name = names[n];

								if (render.drawStarsGreekSymbols) {
									if (sd.mag[0] > render.drawStarsLabelsLimitingMagnitude) name = "";
									if (joinGreekAndName) {
										//label = "@SIZE+4"+greek[Integer.parseInt(name2) - 1]+name3+"@SIZE-4 "+name;
										label = sd.greek + sd.properName+" "+name;
										drawString(render.drawStarsColor, render.drawStarsNamesFont, label.trim(), pos[0], pos[1], -position, false);
									} else {
										//label = "@SIZE+4"+greek[Integer.parseInt(name2) - 1]+name3+"@SIZE-4";
										label = sd.greek + sd.properName;
										drawString(render.drawStarsColor, render.drawStarsNamesFont, name.trim(), pos[0], pos[1], -position, false);
										drawString(render.drawStarsColor, greekFont, label.trim(), pos[0], pos[1], -position, false);
									}
								} else {
									drawString(render.drawStarsColor, render.drawStarsNamesFont, name, pos[0], pos[1], -position, false);
								}
							} else {
								if (render.drawStarsGreekSymbols && !render.drawStarsGreekSymbolsOnlyIfHasProperName) {
									sn.add(name);
									label = sd.greek + sd.properName;
									drawString(render.drawStarsColor, greekFont, label, pos[0], pos[1], -position, false);
								}
							}
						} else
						{
							sn.add(name);
							drawString(render.drawStarsColor, render.drawStarsNamesFont, name, pos[0], pos[1], -position, false);
						}
					}

	 				if (magLabelCount < 20 && maglim >= 8.5 && render.drawMagnitudeLabels && sd.mag[sd.mag.length-1] < maglim - 2) {
	 					magLabelCount ++;
	 					if (position == 0) position = Math.max(size * 3, size+fontSize);
						drawString(render.drawStarsColor, render.drawStarsNamesFont, Functions.formatValue(sd.mag[sd.mag.length-1], 1), pos[0], pos[1], -position, false);
	 				}
				}
			}
		}
	}
	private static int magLabelCount = 0;

	private void fillOval(Graphics g, float x, float y, float ww, float hh, int colIndex) {
		if (g.renderingToAndroid() || ww >= 20 || g.renderingToExternalGraphics() || ww <= 1 || colIndex < 0) {
			g.fillOval(x, y, ww, hh, false);
			return;
		}

		if (ww < 20 && ww == hh && ww == (int) ww) {
		   	if (starImg == null || starImg[(int)ww][0] == null) {
	    		if (starImg == null) {
	    			int max = 45;
	    			// Reduce memory use when colors are not required in stars
	    			if (!render.drawStarsColors) {
	    				starImg = new Object[max][1];
	    			} else {
	    				starImg = new Object[max][7];
	    			}
	    		}
 	    		Object img = g.getImage("jparsec/data/icons/starSimple.png");
				if (render.getColorMode() == COLOR_MODE.WHITE_BACKGROUND
						|| render.getColorMode() == COLOR_MODE.PRINT_MODE
						|| render.getColorMode() == COLOR_MODE.WHITE_BACKGROUND_SIMPLE_GREEN_RED_OR_RED_CYAN_ANAGLYPH) img = g.getColorInvertedImage(img);
				if (render.getColorMode() == COLOR_MODE.PRINT_MODE || render.drawStarsRealistic == REALISTIC_STARS.NONE_CUTE) starImg = new Object[20][8];
	        	int c = g.getColor();
	        	int size[] = g.getSize(img);
				for (int tsizeIndex=2; tsizeIndex < starImg.length; tsizeIndex ++) {
		        	int ss = tsizeIndex;
		        	int w = size[0], h = size[1], colors[] = g.getImageAsPixels(img);
		        	int cr, cg, cb;
		        	int lev1 = 2*48, lev2 = 2*32, off = 90;
		        	if (render.getColorMode() != COLOR_MODE.BLACK_BACKGROUND
		        			&& render.getColorMode() != COLOR_MODE.NIGHT_MODE) {
		        		off = 0;
		        		lev1 = 0;
		        		lev2 = 0;
		        	}
		        	for (int i=0; i<starImg[0].length; i++) {
		        		int iindex = starImg[0].length > 1 ? i : 2;
		            	int[] colors0 = colors.clone();
//		        		if (render.getColorMode() == COLOR_MODE.BLACK_BACKGROUND || render.getColorMode() == COLOR_MODE.NIGHT_MODE) {
			            	for (int j=0; j<colors0.length; j ++) {
			            		int ca = g.getAlpha(colors0[j]);
			            		cr = cg = cb = 164;
					        	if (g.getRed(colors0[j]) < 100 && render.getColorMode() != COLOR_MODE.BLACK_BACKGROUND
					        			&& render.getColorMode() != COLOR_MODE.NIGHT_MODE) {
					        		cr = g.getRed(render.drawStarsColor);
					        		cg = g.getGreen(render.drawStarsColor);
					        		cb = g.getBlue(render.drawStarsColor);
					        		ca = g.getAlpha(render.drawStarsColor);
					        	}
					        	if (g.getRed(colors0[j]) > 100 && render.getColorMode() == COLOR_MODE.NIGHT_MODE) {
					        		ca = 180; //g.getAlpha(render.drawStarsColor);
					        		cr = 180+off;
					        		cg = off;
					        		cb = off;
					        	} else {
				            		if (iindex == 0) cb += lev1;
				            		if (iindex == 1) cb += lev2;
				            		if (iindex == 2) {
				            			cr += lev1;
				            			cg += lev1;
				            			cb += lev1;
				            		}
				            		if (iindex == 3 || iindex == 4) cg += lev1;
				            		if (iindex >= 4) cr += lev1;
				            		if (iindex == 5) cg += lev2;

				            		if (cr > 255) cr = 255;
				            		if (cg > 255) cg = 255;
				            		if (cb > 255) cb = 255;
				            		if (ca > 255) ca = 255;
					        	}

			            		g.setColor(cr-off, cg-off, cb-off, ca);
			            		colors0[j] = g.getColor();
			            	}
//		        		}
		        		if (i == 7) {
		        			starImg[tsizeIndex][i] = g.getScaledImage(g.getImage("jparsec/data/icons/starSimple.png"), ss+1, ss+1, true, false);
		        		} else {
		        			starImg[tsizeIndex][i] = g.getScaledImage(g.getImage(w, h, colors0), ss+1, ss+1, true, false);
		        		}
		        	}
				}
	        	g.setColor(c, true);
	    	}

/*	    	if (colIndex < 0) colIndex = 2;
	    	if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
	    		if (ww == 1) {
	        		g.fillOval(x, y, 1, 1);
	    		} else {
*/	    			
		   	if (colIndex >= starImg[(int) ww].length) colIndex = 0;
		   	g.drawImage(starImg[(int)ww][colIndex], x, y);
/*	    		}
	    	} else {
	    		if (ww == 1) {
	        		g.fillOval(x, y, 1, 1, dist);
	    		} else {
	    			if (starImg[tsize][colIndex] != null) g.drawImage(starImg[(int)ww][colIndex], x, y, dist);
	    		}
	    	}
*/		}
	}

	private Interpolation interp = null;
	/**
	 * This method should be called to update the limiting magnitude for stars
	 * when the field of view changes, or when you want to update the number
	 * of stars shown. Used internally, normally you shouldn't call this.
	 */
	public void setStarsLimitingMagnitude()
	{
		maglim = render.drawStarsLimitingMagnitude;
		if (maglim <= 5.0 && !drawAll) {
//			if (render.drawConstellationContours && maglim < 5.3) maglim = 5.3f;
			return;
		}
		if (render.drawClever || drawAll) {
			try {
				if (interp == null)
					interp = new Interpolation(new double[] {1000000, 5000, 180, 150, 100, 45, 25, 15, 10, 3, 0}, new double[] {5, 5, 5, 6, 6.5, 7.5, 8.0, 8.5, 9.5, 16.5, 16.5}, true);
				float maglim = (float) interp.linearInterpolation(1500.0 / pixels_per_degree)
						- (g.renderingToAndroid() ? 0.5f : 0f);
				if (maglim < 5) maglim = 5;
				if (maglim < this.maglim || drawAll) {
					this.maglim = maglim;
					if (this.maglim > 8.5 && g.renderingToAndroid() && this.maglim > render.drawStarsLimitingMagnitude) this.maglim = render.drawStarsLimitingMagnitude;
				}
			} catch (JPARSECException e) {}
		}
//		if (render.drawConstellationContours && maglim < 5.3) maglim = 5.3f;

		if (sizes == null || maglim != lastmllim) setSizes();
	}

	/**
	 * Returns the limiting magnitude for stars in the rendering.
	 * @param defaultMagLim Initial limiting magnitude in the rendering object.
	 * @param width Render width in pixels.
	 * @param field Render field in radians.
	 * @param android True if rendering to Android.
	 * @return Limiting magnitude to be used in the rendering.
	 */
	public static float getMagLim(float defaultMagLim, int width, float field, boolean android) {

		float maglim = defaultMagLim;
		if (maglim <= 5.0) return maglim;
		float pixels_per_radian = width / field;
		float pixels_per_degree = (float) (pixels_per_radian * Constant.DEG_TO_RAD);
		try {
			Interpolation interp = new Interpolation(new double[] {5000, 180, 150, 100, 45, 25, 15, 10, 3, 0}, new double[] {5, 5, 6, 6.5, 7.5, 8.0, 8.5, 9.5, 16.5, 16.5}, true);
			maglim = (float) interp.linearInterpolation(1500.0 / pixels_per_degree)
					- (android ? 0.5f : 0);
		} catch (JPARSECException e) {}
		return maglim;
	}
	/**
	 * Returns current limiting magnitude for stars.
	 * @return Limiting magnitude.
	 */
	public double getCurrentStarLimitingMagnitude() {
		return maglim;
	}

	/**
	 * Returns the radius of a star given its magnitude.
	 * @param mag Magnitude.
	 * @param maglim Limiting magnitude.
	 * @param hugeFactor A factor depending on the width
	 * in pixels of the rendering. 0 unles width &gt;= 3000,
	 * incrementing by 1 in each 1000 px.
	 * @return Size in pixels.
	 */
	public static float getSizeForAGivenMagnitude(double mag, double maglim, int hugeFactor)
	{
		if (mag > maglim) return 0;
		double maglim2 = ((int) maglim) + 0.5;
		float dif = (float) (maglim2-mag);
		if (hugeFactor > 2) dif *= (hugeFactor-2);
//		if (maglim2 >= 9.51) {
//			return (float) (Math.pow(dif, 0.9)+0.5);
//		}
		return dif;
	}

	private float[] sizes = null;
	private float lastmllim = -10;
	private void setSizes() {
		sizes = new float[220];
		for (int i=0; i<220; i++) {
			sizes[i] = giveMeSizeForAGivenMagnitude(-2 + i*0.1);
		}
		lastmllim = maglim;
		//starImg = null;
	}

	private float giveMeSizeForAGivenMagnitude(double mag)
	{
		float size0 = 0;
    	if (this.hugeFactor > 0 || render.getColorMode() == COLOR_MODE.PRINT_MODE) size0 = 1f;
		return size0 + getSizeForAGivenMagnitude(mag, maglim, hugeFactor);
	}

	private float getSizeForAGivenMagnitude(double mag)
	{
		if (sizes == null) setSizes();
		if (mag < -2) return sizes[0];
		int min = Math.min(sizes.length-1, (int) (mag*10+0.5)+20);
		return sizes[min];
	}

	private static int col[] = new int[]
			{
			128<<24 | 0<<16 | 0<<8 | 200, // Color.blue,
			128<<24 | 0<<16 | 0<<8 | 255, // Color.BLUE,
			128<<24 | 255<<16 | 255<<8 | 255, // Color.WHITE,
			128<<24 | 0<<16 | 255<<8 | 0, // Color.GREEN,
			128<<24 | 255<<16 | 255<<8 | 0, //Color.YELLOW,
			128<<24 | 255<<16 | 200<<8 | 0, //Color.ORANGE,
			128<<24 | 255<<16 | 0<<8 | 0 //Color.RED }, 128);
			};

	// The Greek alphabet as Unicode symbols, for fonts that support them
    private static final char[] greek = new char[] {'\u03B1', '\u03B2', '\u03B3', '\u03B4', '\u03B5',
 		'\u03B6', '\u03B7', '\u03B8', '\u03B9', '\u03BA', '\u03BB', '\u03BC', '\u03BD', '\u03BE', '\u03BF',
 		'\u03C0', '\u03C1', //'\u03C2',
 		'\u03C3', '\u03C4', '\u03C5', '\u03C6', '\u03C7', '\u03C8', '\u03C9'};
    // The Greek alphabet as latin characters for font Symbol. Used when exporting to PDF
    // in iText with iText's own set of fonts.
    private static final char[] greekPDF = new char[] {'a', 'b', 'g', 'd', 'e',
 		'z', 'h', 'q', 'i', 'k', 'l', 'm', 'n', 'x', 'o',
 		'p', 'r', 's', 't', 'u', 'f', 'c', 'y', 'w'};
    // The Greek alphabet using TextLabel (possibly encoding Greek characters as images)
/*    private static final String[] greek = new String[] {"@alpha", "@beta", "@gamma",
		 "@delta", "@epsilon", "@zeta", "@eta", "@theta", "@iota", "@kappa", "@lambda", "@mu", "@nu",
		 "@xi", "@omicron", "@pi", "@rho", "@sigma", "@tau", "@upsilon", "@phi", "@chi", "@psi", "@omega"};
*/

    private void drawFakeStar(int tsize, int colIndex, float pos[]) {
		if (tsize <= 1) {
    		g.fillOval(pos[0], pos[1], 2, 2, false);
    		return;
		}
		g.setColor(col[colIndex], 64);
		if (render.getColorMode() == COLOR_MODE.WHITE_BACKGROUND
				|| render.getColorMode() == COLOR_MODE.PRINT_MODE
				|| render.getColorMode() == COLOR_MODE.WHITE_BACKGROUND_SIMPLE_GREEN_RED_OR_RED_CYAN_ANAGLYPH) g.setColor(g.invertColor(g.getColor()), 64);
		int tsize2 = tsize;
		float tsizef = (tsize2-1f)/2f;
		int d = 1;
		g.fillOval(pos[0]-tsizef-d, pos[1]-tsizef-d, tsize2+2*d, tsize2+2*d, false);

		g.setColor(col[colIndex], 24);
		if (render.getColorMode() == COLOR_MODE.WHITE_BACKGROUND
				|| render.getColorMode() == COLOR_MODE.PRINT_MODE
				|| render.getColorMode() == COLOR_MODE.WHITE_BACKGROUND_SIMPLE_GREEN_RED_OR_RED_CYAN_ANAGLYPH) g.setColor(g.invertColor(g.getColor()), 24);
		d = 2;
		g.fillOval(pos[0]-tsizef-d, pos[1]-tsizef-d, tsize2+2*d, tsize2+2*d, false);

		g.setColor(col[2], false);
		if (render.getColorMode() == COLOR_MODE.WHITE_BACKGROUND
				|| render.getColorMode() == COLOR_MODE.PRINT_MODE
				|| render.getColorMode() == COLOR_MODE.WHITE_BACKGROUND_SIMPLE_GREEN_RED_OR_RED_CYAN_ANAGLYPH) g.setColor(g.invertColor(g.getColor()), false);
		d = 0; //Math.min(1.5f, tsize-1);
		float ts = tsizef - d, ts2 = 2 * ts + 1;
		g.fillOval(pos[0]-ts, pos[1]-ts, ts2, ts2, false);
		if (render.drawStarsRealistic == REALISTIC_STARS.STARRED || render.drawStarsRealistic == REALISTIC_STARS.SPIKED) {
    		g.setColor(col[2], true);
    		if (render.getColorMode() == COLOR_MODE.WHITE_BACKGROUND
					|| render.getColorMode() == COLOR_MODE.PRINT_MODE
					|| render.getColorMode() == COLOR_MODE.WHITE_BACKGROUND_SIMPLE_GREEN_RED_OR_RED_CYAN_ANAGLYPH) g.setColor(g.invertColor(g.getColor()), g.getAlpha(col[2]));
			tsize = tsize2-1;
			if (render.drawStarsRealistic == REALISTIC_STARS.SPIKED) tsize += 1;
			g.setStroke(JPARSECStroke.STROKE_DEFAULT_LINE_THIN);
			g.drawLine(pos[0]-tsize, pos[1], pos[0]-tsize/2, pos[1], false);
			g.drawLine(pos[0]+tsize, pos[1], pos[0]+tsize/2, pos[1], false);
			g.drawLine(pos[0], pos[1]-tsize, pos[0], pos[1]-tsize/2, false);
			g.drawLine(pos[0], pos[1]+tsize, pos[0], pos[1]+tsize/2, false);

			float fac = 0.6f;
			if (render.drawStarsRealistic == REALISTIC_STARS.STARRED) {
    			g.drawLine(pos[0]-tsize*fac, pos[1]-tsize*fac, pos[0]-tsize*fac/2, pos[1]-tsize*fac/2, false);
    			g.drawLine(pos[0]+tsize*fac, pos[1]+tsize*fac, pos[0]+tsize*fac/2, pos[1]+tsize*fac/2, false);
    			g.drawLine(pos[0]+tsize*fac, pos[1]-tsize*fac, pos[0]+tsize*fac/2, pos[1]-tsize*fac/2, false);
    			g.drawLine(pos[0]-tsize*fac, pos[1]+tsize*fac, pos[0]-tsize*fac/2, pos[1]+tsize*fac/2, false);
			}

			tsize -= 2;
			g.setStroke(JPARSECStroke.STROKE_DEFAULT_LINE);
			if (render.drawStarsRealistic == REALISTIC_STARS.SPIKED) g.setStroke(new JPARSECStroke(JPARSECStroke.STROKE_DEFAULT_LINE, 2.5f));
			g.drawLine(pos[0]-tsize, pos[1], pos[0]-tsize/2, pos[1], false);
			g.drawLine(pos[0]+tsize, pos[1], pos[0]+tsize/2, pos[1], false);
			g.drawLine(pos[0], pos[1]-tsize, pos[0], pos[1]-tsize/2, false);
			g.drawLine(pos[0], pos[1]+tsize, pos[0], pos[1]+tsize/2, false);

			if (render.drawStarsRealistic == REALISTIC_STARS.STARRED) {
    			g.drawLine(pos[0]-tsize*fac, pos[1]-tsize*fac, pos[0]-tsize*fac/2, pos[1]-tsize*fac/2, false);
    			g.drawLine(pos[0]+tsize*fac, pos[1]+tsize*fac, pos[0]+tsize*fac/2, pos[1]+tsize*fac/2, false);
    			g.drawLine(pos[0]+tsize*fac, pos[1]-tsize*fac, pos[0]+tsize*fac/2, pos[1]-tsize*fac/2, false);
    			g.drawLine(pos[0]-tsize*fac, pos[1]+tsize*fac, pos[0]-tsize*fac/2, pos[1]+tsize*fac/2, false);
			}
		}
    }
    
	// Renderize stars
    Object starImg[][] = null;
    private static float starSize[] = null;
    private void drawStar(int tsize, float[] pos, float dist, int colIndex, Graphics g) {
    	if (tsize >= 45) tsize = 44;
    	if (render.getColorMode() == COLOR_MODE.NIGHT_MODE) colIndex = 6;
    	int index = render.drawStarsRealistic.ordinal();
    	if (colIndex < 0) colIndex = 2;

    	if (FakeStarsForExternalGraphics && g.renderingToExternalGraphics()) {
    		drawFakeStar(tsize, colIndex, pos);
    		return;
    	}

    	if (starImg != null && tsize >= starImg.length) starImg = null;
     	if (starImg == null || (starImg[tsize][0] == null && tsize > 0)) {
    		if (starImg == null) {
    			int max = 45;
    			// Reduce memory use when colors are not required in stars
    			if (!render.drawStarsColors) {
    				starImg = new Object[max][1];
    			} else {
    				starImg = new Object[max][7];
    			}
    			starSize = new float[max];
    		}

        	int c = g.getColor();
    		String type = "PC";
    		if (g.renderingToAndroid()) type = "Android";
    		String name = "jparsec/data/icons/stars"+type+"/";
    		if (render.getColorMode() == COLOR_MODE.NIGHT_MODE || render.getColorMode() == COLOR_MODE.BLACK_BACKGROUND) {
    			name+= "black";
    		} else {
    			name+= "white";
    		}
			for (int tsizeIndex=1; tsizeIndex < starImg.length; tsizeIndex ++) {
	        	for (int i=0; i<starImg[tsizeIndex].length; i++) {
	        		int iindex = starImg[tsizeIndex].length > 1 ? i : 2;
	        		String sname = name + "_"+index+"_"+tsizeIndex+"_"+iindex+".png";
		    		starImg[tsizeIndex][i] = g.getImage(sname);
		    		if (starImg == null || starImg[tsizeIndex][i] == null) break;
		    		starSize[tsizeIndex] = g.getSize(starImg[tsizeIndex][i])[1]/2f;
	        	}
			}
        	g.setColor(c, true);
    	}

    	float ss = starSize[tsize]; //(int) (tsize*starFactor[index]);
/*    	if (hugeFactor == 0) {
	    	if (index == 0) ss = (ss * 4) / 5;
	    	if (index == 1) ss = (ss * 2) / 3;
    	}
*/
    	if (starImg[0].length == 1) colIndex = 0;
    	if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
    		if (tsize <= 1) {
        		g.fillOval(pos[0], pos[1], 1, 1, false);
        		return;
    		} else {
    			if (g.renderingToExternalGraphics()) {
    				double scale = (double) tsize / (starImg.length-1.0);
    				if (starImg[starImg.length-1][colIndex] != null) {
    					g.drawImage(starImg[starImg.length-1][colIndex], pos[0]-ss, pos[1]-ss, scale, scale);
    					return;
    				}
    			} else {
    				if (starImg[tsize][colIndex] != null) {
    					g.drawImage(starImg[tsize][colIndex], pos[0]-ss, pos[1]-ss);
    					return;
    				}
    			}
    		}
    	} else {
    		if (tsize <= 1) {
        		g.fillOval(pos[0], pos[1], 1, 1, dist);
        		return;
    		} else {
    			if (g.renderingToExternalGraphics()) {
    				double scale = (double) tsize / (starImg.length-1.0);
    				if (starImg[starImg.length-1][colIndex] != null) {
    					g.drawImage(starImg[starImg.length-1][colIndex], pos[0]-ss, pos[1]-ss, dist, scale, scale);
    					return;
    				}
    			} else {
    				if (starImg[tsize][colIndex] != null) {
    					g.drawImage(starImg[tsize][colIndex], pos[0]-ss, pos[1]-ss, dist);
    					return;
    				}
    			}
    		}
    	}
    	if (render.getColorMode() != COLOR_MODE.NIGHT_MODE) g.setColor(col[colIndex], true);
		g.fillOval(pos[0]-ss, pos[1]-ss, tsize, tsize, false);
    }

	// Draw Horizon as a texture
	private void drawHorizonTexture() throws JPARSECException
	{
		if (render.drawHorizonTexture == HORIZON_TEXTURE.NONE) return;
		if (projection.obs.getMotherBody() != TARGET.EARTH && projection.obs.getMotherBody() != TARGET.NOT_A_PLANET) return;
		if (fieldDeg < 1) return;

		String t = "Veleta_30m";
		if (render.drawHorizonTexture == HORIZON_TEXTURE.VILLAGE) t = "village";
		if (render.drawHorizonTexture == HORIZON_TEXTURE.GUEREINS) t = "guereins";
		if (render.drawHorizonTexture == HORIZON_TEXTURE.LICHTENSTEIN) t = "Lichtenstein";
		Object horizonTexture = g.getImage(FileIO.DATA_LANDSCAPES_DIRECTORY + t + ".jpg");
		if (horizonTexture == null) horizonTexture = g.getImage(FileIO.DATA_LANDSCAPES_DIRECTORY + t + ".png");
		if (horizonTexture == null) return;

		String data[] = DataSet.arrayListToStringArray(ReadFile.readResource(FileIO.DATA_LANDSCAPES_DIRECTORY + t + ".txt"));
		int sizei[] = g.getSize(horizonTexture);
		int imax = sizei[0], jmax = sizei[1];
		double step0 = (imax / 4000.0) * (imax / 360.0) / pixels_per_degree;
		int step = (int) (step0+0.5);
		if (step < 1) step = 1;
		if (RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR < 1f) step += (int) (1.0 / RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR);
		float size = 0.8f + (float)(pixels_per_degree * step / (imax / 180.0));

		double im2 = Double.parseDouble(data[1]), jm2 = Double.parseDouble(data[2]);
		int brightness = Integer.parseInt(data[3]), bmax = 133;
		int minr = Integer.parseInt(FileIO.getField(1, data[0], " ", true));
		int ming = Integer.parseInt(FileIO.getField(2, data[0], " ", true));
		int minb = Integer.parseInt(FileIO.getField(3, data[0], " ", true));
		if (render.planetRender.ephemSun.elevation*Constant.RAD_TO_DEG > 5) {
			brightness = 0;
		} else {
			if (render.planetRender.ephemSun.elevation*Constant.RAD_TO_DEG > -15) {
				double exp = 1 - (render.planetRender.ephemSun.elevation*Constant.RAD_TO_DEG + 15)/20.0;
				brightness = (int) (brightness * exp * exp * exp);
			}
		}
		if (brightness > bmax) brightness = bmax;
		LocationElement loc0 = projection.getHorizontalPositionOfRendering();
		double scaley = sizei[1] * Constant.TWO_PI / imax;
		double i0 = imax*(-1+loc0.getLongitude()/Constant.TWO_PI);
		double j0 = -scaley*(-1.0+loc0.getLatitude()/Constant.PI_OVER_TWO);
		double s = field * imax / Constant.TWO_PI;
		if (s > imax/2) s = imax/2;
		int init = (int) (i0 - s), iend = (int) (i0 + s);
		int jnit = (int) (j0 - s), jend = (int) (j0 + s);
		if (jnit < 0) jnit = 0;
		if (jend >= sizei[1]) jend = sizei[1]-1;
		if (fieldDeg > 160) {
			init = 0;
			iend = imax-1;
		}
		float d = getDist(milkywayDist);

		float scale = 1;
		if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH && ((render.planetRender.highQuality
				 && RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR > 1) || g.renderingToExternalGraphics())) {
			if (render.planetRender.highQuality && RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR > 1) {
				scale = RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR;
				//if (RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR > 2) scale = RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR * 1.5f / 2f;
				step /= scale;
			}
			if (step < 1) step = 1;

			Graphics g2 = g.getGraphics((int) (scale*g.getWidth()), (int)(scale*g.getHeight()));
			g2.setAnaglyph(g2.getScaledImage(g.getImage(0, 0, g.getWidth(), g.getHeight()), g2.getWidth(), g2.getHeight(), true, false), null);
			g2.disableInversion();
			g2.setColor(0, true);
			g2.fillRect(0, 0, g2.getWidth(), g2.getHeight());
	        g2.setClip((int) (rec.getMinX()*scale+1), (int) (rec.getMinY()*scale+1), (int) (rec.getWidth()*scale-2), (int) (rec.getHeight()*scale-2));
			g2.enableInversion();

			this.drawHorizonTexture(init, iend, jnit, jend, step, size, imax, im2, jm2, d, horizonTexture, g2, scale, brightness, minr, ming, minb);
			Object pathMilkyWay = g2.getRendering();
			if (scale > 1) pathMilkyWay = g2.getScaledImage(pathMilkyWay, g.getWidth(), g.getHeight(), true, false); //RenderPlanet.ALLOW_SPLINE_RESIZING); // Frequent out of memory errors here in old PCs

			if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
				g.drawImage(g.makeColorTransparent(pathMilkyWay, render.background, false, false, 0), 0, 0);
			} else {
				g.drawImage(g.makeColorTransparent(pathMilkyWay, render.background, false, false, 0), 0, 0, getDist(milkywayDist));
			}

		} else {
			this.drawHorizonTexture(init, iend, jnit, jend, step, size, imax, im2, jm2, d, horizonTexture, g, scale, brightness, minr, ming, minb);
		}
		return;
	}

	private void drawHorizonTexture(int init, int iend, int jnit, int jend, int step, float size, int imax, double im2, double jm2, float d,
			Object horizonTexture, Graphics g, float scale, int brightness, int minr, int ming, int minb) throws JPARSECException {
		//if (Math.abs(Functions.toCenturies(jd)) < 10) return;

		int size2 = (int) (2*size+scale);
		int[] sizei = g.getSize(horizonTexture);
		double scaley = sizei[1] * Constant.TWO_PI / imax;

		init = (int) Functions.module(init, imax);
		iend = (int) Functions.module(iend, imax);
		if (iend < init) iend += imax;
		boolean showGround = true;
		if (showGround) projection.disableCorrectionOfLocalHorizon();
		for (int j=jnit; j<=jend; j = j + step) {
			for (int ii=0; ii<imax; ii = ii + step) {
//				for (int ii=init; ii<=iend; ii = ii + step) {
				int i = ii;
				if (i >= imax) i-= imax;
					LocationElement loc = null;

						loc = new LocationElement((i-im2)*Constant.TWO_PI/imax, (1.0-j/jm2)*scaley, 1.0);
						loc = CoordinateSystem.horizontalToEquatorial(loc, projection.ast, projection.obs.getLatitudeRad(), true);
						loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, !showGround, true, 0);

						if (loc == null) continue;

						g.disableInversion();
						int rgb = g.getRGB(horizonTexture, i, j);
						g.enableInversion();
						if (((rgb>>24)&255) < 20) continue;
						int rr = ((rgb>>16)&255), gg = ((rgb>>8)&255), bb = (rgb&255);
						if (rr >= minr && gg >= ming && bb >= minb) continue;

						if (brightness > 0) {
							double lum = ((0.299*(rr-brightness) + 0.587*(gg-brightness) + 0.114*(bb-brightness)))/255.0;
							if (lum <= 0) {
								rr = gg = bb = 0;
							} else {
								rr = (int) (rr * lum);
								gg = (int) (gg * lum);
								bb = (int) (bb * lum);
								if (rr < 0) rr = 0;
								if (gg < 0) gg = 0;
								if (bb < 0) bb = 0;
								if (rr > 255) rr = 255;
								if (gg > 255) gg = 255;
								if (bb > 255) bb = 255;
							}
						}
						g.setColor(rr, gg, bb, 255);
						loc.setRadius(g.getColor()+0.5);


					float pos[] = projection.projectPosition(loc, 0, false);
					if (pos != null && !this.isInTheScreen((int)pos[0], (int)pos[1], (int)size)) continue;
					if (pos != Projection.INVALID_POSITION) {
						g.setColor((int)loc.getRadius(), true);
						if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
							if (scale > 1) {
								//if (fieldDeg > 50)
								//	g.setColor((int)loc.getRadius(), g.getAlpha((int)loc.getRadius())/2);
								pos[0] = scale*pos[0];
								pos[1] = scale*pos[1];
								g.fillRect((int)(0.5+pos[0])-size*scale, (int)(0.5+pos[1])-size*scale, size2*scale, size2*scale);
							} else {
								g.fillRect((int)(0.5+pos[0])-size, (int)(0.5+pos[1])-size, size2, size2);
							}
						} else {
							g.fillRect((int)(0.5+pos[0])-size, (int)(0.5+pos[1])-size, size2, size2, d);
						}
					}
			}
		}
		if (showGround) projection.enableCorrectionOfLocalHorizon();
		g.enableInversion();
//		if (save) DataBase.addData("milkyWayTexture", threadID, mw.toArray(), true);
	}

	// Draw Milky Way shapes
	//private Object milkyWayTexture = null;
	private void drawMilkyWay() throws JPARSECException
	{
		if (!render.drawMilkyWayContours) {
			return;
		}

		if (render.drawMilkyWayContoursWithTextures != MILKY_WAY_TEXTURE.NO_TEXTURE && 
				(render.getColorMode() == COLOR_MODE.NIGHT_MODE || render.getColorMode() == COLOR_MODE.BLACK_BACKGROUND)) {
			if (fieldDeg < 1) return;
// 			if (milkyWayTexture == null) {
			String mw_url = FileIO.DATA_TEXTURES_DIRECTORY + "deepsky/"+SkyRenderElement.MILKY_WAY_TEXTURE_FILENAME[render.drawMilkyWayContoursWithTextures.ordinal()];
			Object milkyWayTexture = g.getImage(mw_url);
//			}
 			if (milkyWayTexture == null) {
 				render.drawMilkyWayContoursWithTextures = MILKY_WAY_TEXTURE.OPTICAL;
 				mw_url = FileIO.DATA_TEXTURES_DIRECTORY + "deepsky/"+SkyRenderElement.MILKY_WAY_TEXTURE_FILENAME[render.drawMilkyWayContoursWithTextures.ordinal()];
 				milkyWayTexture = g.getImage(mw_url);
 	 			if (milkyWayTexture == null) {
 	 				render.drawMilkyWayContoursWithTextures = MILKY_WAY_TEXTURE.NO_TEXTURE;
 	 				return;
 	 			}
 			}
 			if (!g.renderingToAndroid() && !DataBase.dataExists(mw_url, true))
 				g.addToDataBase(milkyWayTexture, mw_url, 0);
 			
 			int sizei[] = g.getSize(milkyWayTexture);
			int imax = sizei[0], jmax = sizei[1];
			double step0 = (imax / 360.0) / pixels_per_degree;
			//if (g.renderingToAndroid()) step0 *= 2;
			int step = (int) (step0+0.5);
			if (step < 1) step = 1;
			if (RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR < 1f) step += (int) (1.0 / RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR);
			float size = 0.8f + (float)(pixels_per_degree * step / (imax / 180.0));
			double im2 = (imax-1) / 2.0, jm2 = (jmax-1)/2.0;
			LocationElement loc0 = null;
			if (projection.obs.getMotherBody() == TARGET.EARTH) {
				loc0 = CoordinateSystem.equatorialToGalactic(loc0J2000, fast);
			} else {
				loc0 = projection.getGalacticPositionOfRendering();
			}
			double i0 = -im2*(-1.0+loc0.getLongitude()/Math.PI);
			double j0 = -jm2*(-1.0+loc0.getLatitude()/Constant.PI_OVER_TWO);
			double s = field * 0.75 * imax / Constant.TWO_PI;
			float vs = ((float) render.height / render.width) * 1.125f;
			if (vs > 1) s = s * vs;
			if (s > imax/2) s = imax/2;
			int init = (int) (i0 - s), iend = (int) (i0 + s);
			int jnit = (int) (j0 - s), jend = (int) (j0 + s);
			if (fieldDeg > 160) {
				init = 0;
				iend = imax-1;
			}

			float d = getDist(milkywayDist);

			float scale = g.renderingToAndroid() ? 0.25f : 1.0f;
			boolean isWMAP = false;
			if (render.drawMilkyWayContoursWithTextures == MILKY_WAY_TEXTURE.WMAP) isWMAP = true;
			if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH && ((render.planetRender.highQuality
					 && RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR > 1) || g.renderingToExternalGraphics())) {
				if (!g.renderingToAndroid() && render.planetRender.highQuality && RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR > 1) {
					scale = 1.5f;
					if (RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR > 2) scale = RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR * 1.5f / 2f;
					step /= scale;
				}
				if (step < 1) step = 1;

				Graphics g2 = g.getGraphics((int) (scale*g.getWidth()), (int)(scale*g.getHeight()));
				g2.disableInversion();
				g2.setColor(0, true);
				g2.fillRect(0, 0, g2.getWidth(), g2.getHeight());
		        g2.setClip((int) (rec.getMinX()*scale+1), (int) (rec.getMinY()*scale+1), (int) (rec.getWidth()*scale-2), (int) (rec.getHeight()*scale-2));
				g2.enableInversion();

				this.drawMilkyWayTexture(init, iend, jnit, jend, step, size, imax, im2, jm2, d, milkyWayTexture, g2, scale, isWMAP);
				if (render.drawMilkyWayContoursWithTextures != MILKY_WAY_TEXTURE.WMAP) {
					// 2130, 990 -> 2215, 1060 (LMC)
					this.drawMilkyWayTexture2((int) (0.5 + imax * 2130.0 / 3000.0), (int) (0.5 + imax * 2215 / 3000.0), (int) (0.5 + imax * 990.0 / 3000.0), (int) (0.5 + imax * 1060.0 / 3000.0), step, size, imax, im2, jm2, d, milkyWayTexture, g2, scale);
					// 1955, 1100 -> 2000, 1135 (SMC)
					this.drawMilkyWayTexture2((int) (0.5 + imax * 1955.0 / 3000.0), (int) (0.5 + imax * 2000.0 / 3000.0), (int) (0.5 + imax * 1100.0 / 3000.0), (int) (0.5 + imax * 1135.0 / 3000.0), step, size, imax, im2, jm2, d, milkyWayTexture, g2, scale);
				}
				if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
					g.drawImage(g2.getRendering(), 0, 0, 1.0f/scale, 1.0f/scale);
				} else {
					g.drawImage(g2.getRendering(), 0, 0, getDist(milkywayDist), 1.0f/scale, 1.0f/scale);
				}
				drawHorizon();

			} else {
				this.drawMilkyWayTexture(init, iend, jnit, jend, step, size, imax, im2, jm2, d, milkyWayTexture, g, scale, isWMAP);
				if (render.drawMilkyWayContoursWithTextures != MILKY_WAY_TEXTURE.WMAP) {
					// 2130, 990 -> 2215, 1060 (LMC)
					this.drawMilkyWayTexture2((int) (0.5 + imax * 2130.0 / 3000.0), (int) (0.5 + imax * 2215 / 3000.0), (int) (0.5 + imax * 990.0 / 3000.0), (int) (0.5 + imax * 1060.0 / 3000.0), step, size, imax, im2, jm2, d, milkyWayTexture, g, scale);
					// 1955, 1100 -> 2000, 1135 (SMC)
					this.drawMilkyWayTexture2((int) (0.5 + imax * 1955.0 / 3000.0), (int) (0.5 + imax * 2000.0 / 3000.0), (int) (0.5 + imax * 1100.0 / 3000.0), (int) (0.5 + imax * 1135.0 / 3000.0), step, size, imax, im2, jm2, d, milkyWayTexture, g, scale);
				}
			}
			return;
		}

		if (fieldDeg < 5.0) {
			return;
		}

		// There's no milky way above 20 degrees of galactic latitude
		if (fieldDeg < 100) {
			LocationElement loc = null;
			if (projection.obs.getMotherBody() == TARGET.EARTH) {
				loc = CoordinateSystem.equatorialToGalactic(loc0J2000, fast);
			} else {
				loc = projection.getGalacticPositionOfRendering();
			}
			double lat = Math.abs(loc.getLatitude()) - field * 0.75;
			if (lat > deg_20) {
				return;
			}
		}

		float pos0[] = new float[2];
		float pos1[] = new float[2];

		// Draw Milky Way
		ArrayList<Object> milkyWay = null;
		Object o = null;
		if (db_milkyWay >= 0) {
			o = DataBase.getData(db_milkyWay);
		} else {
			o = DataBase.getData("milkyWay", threadID, true);
		}
		if (o != null) milkyWay = new ArrayList<Object>(Arrays.asList((Object[]) o));
		if (milkyWay == null) {
			milkyWay = new ArrayList<Object>();
			LocationElement old_loc = null, old_loc2 = null;
			if (baryc == null) baryc = Ephem.eclipticToEquatorial(PlanetEphem.getGeocentricPosition(equinox, TARGET.Solar_System_Barycenter, 0.0, false, projection.obs), Constant.J2000, projection.eph);

			InputStream is = null;
			BufferedReader dis = null;
			try
			{
				is = getClass().getClassLoader().getResourceAsStream(FileIO.DATA_SKY_DIRECTORY + "milkyway.txt");
				dis = new BufferedReader(new InputStreamReader(is, ReadFile.ENCODING_ISO_8859));
				String line = "";
				boolean starting = true, fillPoints = false;
				while ((line = dis.readLine()) != null)
				{
					if (!line.startsWith("!")) {
						float ra = Float.parseFloat(FileIO.getField(1, line, " ", true));
						float dec = Float.parseFloat(FileIO.getField(2, line, " ", true));

						if ((ra == 0.0f && dec == 0.0f) || (ra == 1.0f && dec == 1.0f) || (ra == 2.0f && dec == 2.0f))
						{
							if (ra == 1.0f && dec == 1.0f) fillPoints = true;
							milkyWay.add(new float[] { ra, dec }); //new LocationElement(ra, dec, 1.0));
						} else
						{
							LocationElement loc = new LocationElement(ra / Constant.RAD_TO_HOUR, dec * Constant.DEG_TO_RAD, 1.0);
							if (equinox != Constant.J2000) {
								// Correct for aberration, precession, and nutation
								if (projection.eph.ephemType == COORDINATES_TYPE.APPARENT) {
									loc.setRadius(1000 * Constant.RAD_TO_ARCSEC); // 1000 pc
									double light_time = loc.getRadius() * Constant.LIGHT_TIME_DAYS_PER_AU;
									double r[] = Ephem.aberration(loc.getRectangularCoordinates(), baryc, light_time);

									r = precessFromJ2000(equinox, r, projection.eph);
									loc = LocationElement.parseRectangularCoordinates(nutateInEquatorialCoordinates(equinox, projection.eph, r, true));
								} else {
									loc = LocationElement.parseRectangularCoordinates(precessFromJ2000(equinox,
											LocationElement.parseLocationElement(loc), projection.eph));
								}
							}
							if (projection.obs.getMotherBody() != TARGET.EARTH && projection.obs.getMotherBody() != TARGET.NOT_A_PLANET) {
								loc = projection.getPositionFromBody(loc, false);
							}
							LocationElement loc2 = projection.getApparentLocationInSelectedCoordinateSystem(loc, true, false, (float)(Constant.DEG_TO_RAD*0.15));
							if (fillPoints) {
								if (loc2 != null) milkyWay.add(new float[] {(float)loc2.getLongitude(), (float)loc2.getLatitude()}); //loc2);
								continue;
							}

							if (loc2 != null && !render.drawSkyBelowHorizon && projection.obs.getMotherBody() != TARGET.NOT_A_PLANET
									&& render.coordinateSystem != COORDINATE_SYSTEM.HORIZONTAL) {
								LocationElement ll = projection.getApparentLocationInSelectedCoordinateSystem(loc, false, false, COORDINATE_SYSTEM.HORIZONTAL, 0);
								if (ll.getLatitude() < -projection.getHorizonElevation()-Constant.DEG_TO_RAD*0.15) loc2 = null;
							}

							if (loc2 == null && old_loc != null) {
								LocationElement loc3 = projection.getApparentLocationInSelectedCoordinateSystem(loc, false, true, 0);
								if (loc3 != null) {
									if (starting) {
										milkyWay.add(new float[] {(float)old_loc.getLongitude(), (float)old_loc.getLatitude()}); //old_loc);
										starting = false;
									}
									milkyWay.add(new float[] {(float)loc3.getLongitude(), (float)loc3.getLatitude()}); //loc2);
									milkyWay.add(null);
									starting = true;
									old_loc = null;
									old_loc2 = loc3;
									continue;
								}
							} else {
								if (loc2 != null && old_loc != null) {
									if (starting) {
										milkyWay.add(new float[] {(float)old_loc.getLongitude(), (float)old_loc.getLatitude()}); //old_loc);
										starting = false;
									}
									milkyWay.add(new float[] {(float)loc2.getLongitude(), (float)loc2.getLatitude()}); //loc2);
								} else {
									if (loc2 == null) {
										milkyWay.add(null);
										starting = true;
									} else {
										if (old_loc == null ) {
											if (old_loc2 != null) {
												milkyWay.add(new float[] {(float)old_loc2.getLongitude(), (float)old_loc2.getLatitude()}); //old_loc2);
												milkyWay.add(new float[] {(float)loc2.getLongitude(), (float)loc2.getLatitude()}); //loc2);
											}
										}
									}
								}
							}
							old_loc = loc2;
							old_loc2 = projection.getApparentLocationInSelectedCoordinateSystem(loc, false, true, 0);
						}
					}
				}
				// Close file
				dis.close();
				is.close();
				is = null;
				DataBase.addData("milkyWay", threadID, milkyWay.toArray(), true);
				db_milkyWay = DataBase.getIndex("milkyWay", threadID);
			} catch (Exception e2)
			{
				try {
					if (dis != null) dis.close();
					if (is != null) is.close();
					is = null;
				} catch (Exception exc) {}
				throw new JPARSECException(
						"error while reading milky way file.", e2);
			}
		}

		boolean fastMode = false;
		if (!g.renderingToAndroid()) {
			fastMode = render.drawFastLinesMode.fastMilkyWay();
			if (render.drawFastLinesMode == FAST_LINES.NONE && render.planetRender.highQuality) fastMode = true;
			if (fastMode) {
				if (!render.drawMilkyWayStroke.isContinuousLine()) fastMode = false;
				if (render.drawMilkyWayStroke.getLineWidth() >= 2) fastMode = false;
				if (g.renderingToExternalGraphics()) fastMode = false;
	//			if (render.anaglyphMode != ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) fastMode = false;
			}
		}
		Object pathMilkyWay = null, pathMilkyWay1 = null;
		boolean starting = true, startup = true;
		float[] curvePos1 = null, curvePos2 = null, curvePos3 = null;
		boolean soft = true; //!render.fillMilkyWay;
		int step = 1;
		boolean fill = render.fillMilkyWay; // && render.drawCoordinateGrid;
		if (g.renderingToAndroid()) {
			soft = false;
			fastMode = false;
		}
		/*
		if (!fill && pixels_per_degree < 11 && render.drawClever && !g.renderingToExternalGraphics()) {
			step = 2;
			soft = false;
			if (render.coordinateSystem == CoordinateSystem.COORDINATE_SYSTEM.HORIZONTAL && !render.drawSkyBelowHorizon && projection.obs.getMotherBody() != TARGET.NOT_A_PLANET) step = 1;
		}
		*/

		//int cte = (int) (pixels_per_degree * 10);
		float loc[]; //[];

		int width2 = (int) (pixels_per_degree_50);
		width2 *= width2;
		int lastIndex = -1;

		int rs = milkyWay.size();
		int x[] = null, y[] = null, index = -1;
		//double he = projection.getHorizonElevation();
		//projection.setHorizonElevation(5*Constant.DEG_TO_RAD);

		projection.disableCorrectionOfLocalHorizon();
		if (fastMode) {
			x = new int[4000];
			y = new int[4000];
			for (int i = 0; i < rs; i = i + step)
			{
				loc = (float[]) milkyWay.get(i);

				if (loc == null) {
					starting = true;
				} else {
					if (loc[0] == 1.0 && loc[1] == 1.0) {
						lastIndex = i;
						break;
					}

					if ((loc[0] == 0.0 && loc[1] == 0.0))
					{
						starting = true;
						startup = true;
					} else
					{
						pos0 = projection.projectPosition(loc, 200, true);
						//pos0 = projection.projectPosition(loc, cte, !fill);
						if (!projection.isInvalid(pos1) && !projection.isInvalid(pos0) && (render.projection == Projection.PROJECTION.CYLINDRICAL_EQUIDISTANT || render.projection == Projection.PROJECTION.CYLINDRICAL || projection.isCylindricalForced()) && !starting) {
							if ((FastMath.pow(pos0[0] - pos1[0], 2.0) + FastMath.pow(pos0[1] - pos1[1], 2.0)) > width2)
								pos0 = Projection.INVALID_POSITION;
						}

						if (!projection.isInvalid(pos0))
						{
							if (starting) {
								index ++;
								x[index] = -1;
								y[index] = -1;
								index ++;
								x[index] = (int)(pos0[0]+0.5);
								y[index] = (int)(pos0[1]+0.5);
								if (startup) {
									loc = (float[]) milkyWay.get(i+1);
									pos0 = projection.projectPosition(loc, 200, true);
									//pos0 = projection.projectPosition(loc, cte, !fill);
								}
								starting = false;
								startup = false;
							} else {
								index ++;
								x[index] = (int)(pos0[0]+0.5);
								y[index] = (int)(pos0[1]+0.5);
							}
						} else {
							starting = true;
						}
						pos1 = pos0;
					}
				}
			}
		} else {
			pathMilkyWay = g.generalPathInitialize();
			pathMilkyWay1 = g.generalPathInitialize();
			g.generalPathMoveTo(pathMilkyWay, 0, 0);
			g.generalPathMoveTo(pathMilkyWay1, 0, 0);
			for (int i = 0; i < rs; i = i + step)
			{
				loc = (float[]) milkyWay.get(i);

				if (loc == null) {
					starting = true;
					if (soft) {
						if (curvePos3 != null) {
							g.generalPathCurveTo(pathMilkyWay, curvePos1[0],curvePos1[1], curvePos2[0], curvePos2[1], curvePos3[0], curvePos3[1]);
						} else {
							if (curvePos2 != null) {
								g.generalPathQuadTo(pathMilkyWay, curvePos1[0],curvePos1[1], curvePos2[0], curvePos2[1]);
							} else {
								if (curvePos1 != null)
									g.generalPathLineTo(pathMilkyWay, curvePos1[0],curvePos1[1]);
							}
						}
						curvePos1 = null;
						curvePos2 = null;
						curvePos3 = null;
					}
				} else {
					if (loc[0] == 1.0 && loc[1] == 1.0) {
						lastIndex = i;
						if (soft) {
							if (curvePos3 != null) {
								g.generalPathCurveTo(pathMilkyWay, curvePos1[0],curvePos1[1], curvePos2[0], curvePos2[1], curvePos3[0], curvePos3[1]);
							} else {
								if (curvePos2 != null) {
									g.generalPathQuadTo(pathMilkyWay, curvePos1[0],curvePos1[1], curvePos2[0], curvePos2[1]);
								} else {
									if (curvePos1 != null)
										g.generalPathLineTo(pathMilkyWay, curvePos1[0],curvePos1[1]);
								}
							}
							curvePos1 = null;
							curvePos2 = null;
							curvePos3 = null;

						}
						break;
					}

					if ((loc[0] == 0.0 && loc[1] == 0.0))
					{
						starting = true;
						startup = true;
						if (soft) {
							if (curvePos3 != null) {
								g.generalPathCurveTo(pathMilkyWay, curvePos1[0],curvePos1[1], curvePos2[0], curvePos2[1], curvePos3[0], curvePos3[1]);
							} else {
								if (curvePos2 != null) {
									g.generalPathQuadTo(pathMilkyWay, curvePos1[0],curvePos1[1], curvePos2[0], curvePos2[1]);
								} else {
									if (curvePos1 != null)
										g.generalPathLineTo(pathMilkyWay, curvePos1[0],curvePos1[1]);
								}
							}
							curvePos1 = null;
							curvePos2 = null;
							curvePos3 = null;

							pathMilkyWay1 = pathMilkyWay;
							pathMilkyWay = g.generalPathInitialize();
						}
					} else
					{
						pos0 = projection.projectPosition(loc, 200, true);
						//pos0 = projection.projectPosition(loc, cte, !fill);
						if (!projection.isInvalid(pos1) && !projection.isInvalid(pos0) && (render.projection == Projection.PROJECTION.CYLINDRICAL_EQUIDISTANT || render.projection == Projection.PROJECTION.CYLINDRICAL || projection.isCylindricalForced()) && !starting) {
							if ((FastMath.pow(pos0[0] - pos1[0], 2.0) + FastMath.pow(pos0[1] - pos1[1], 2.0)) > width2)
								pos0 = Projection.INVALID_POSITION;
						}

						if (!projection.isInvalid(pos0))
						{
							if (starting) {
								g.generalPathMoveTo(pathMilkyWay, pos0[0],pos0[1]);
								if (startup) {
									loc = (float[]) milkyWay.get(i+1);
									pos0 = projection.projectPosition(loc, 200, true);
									//pos0 = projection.projectPosition(loc, cte, !fill);
								}
								starting = false;
								startup = false;
								if (soft) {
									if (curvePos3 != null) {
										g.generalPathCurveTo(pathMilkyWay, curvePos1[0],curvePos1[1], curvePos2[0], curvePos2[1], curvePos3[0], curvePos3[1]);
									} else {
										if (curvePos2 != null) {
											g.generalPathQuadTo(pathMilkyWay, curvePos1[0],curvePos1[1], curvePos2[0], curvePos2[1]);
										} else {
											if (curvePos1 != null)
												g.generalPathLineTo(pathMilkyWay, curvePos1[0],curvePos1[1]);
										}
									}
									curvePos1 = pos0;
									curvePos2 = null;
									curvePos3 = null;
								}
							} else {
								if (soft) {
									if (curvePos1 == null) {
										curvePos1 = pos0;
									} else {
										if (curvePos2 == null) {
											curvePos2 = pos0;
										} else {
											if (curvePos3 == null) {
												curvePos3 = pos0;
											} else {
												int rest = i % 4;
												if (rest == 0) {
													g.generalPathCurveTo(pathMilkyWay, curvePos1[0],curvePos1[1], curvePos2[0], curvePos2[1], curvePos3[0], curvePos3[1]);
													curvePos1 = pos0;
													curvePos2 = null;
													curvePos3 = null;
												} else {
													if (rest == 3) {
														g.generalPathLineTo(pathMilkyWay, curvePos1[0],curvePos1[1]);
														curvePos1 = curvePos2.clone();
														curvePos2 = curvePos3.clone();
														curvePos3 = pos0;
													} else {
														if (rest == 2) {
															g.generalPathQuadTo(pathMilkyWay, curvePos1[0],curvePos1[1], curvePos2[0], curvePos2[1]);
															curvePos1 = curvePos3.clone();
															curvePos2 = pos0;
															curvePos3 = null;
														} else {
															g.generalPathLineTo(pathMilkyWay, curvePos1[0],curvePos1[1]);
															g.generalPathLineTo(pathMilkyWay, curvePos2[0],curvePos2[1]);
															g.generalPathLineTo(pathMilkyWay, curvePos3[0],curvePos3[1]);
															curvePos1 = pos0;
															curvePos2 = null;
															curvePos3 = null;
														}
													}
												}
											}
										}
									}
								} else {
//									if (rec.isLineIntersectingRectangle(oldpos[0], oldpos[1], pos0[0], pos0[1])) {
										g.generalPathLineTo(pathMilkyWay, pos0[0],pos0[1]);
//									} else {
//										starting = true;
//									}
								}
							}
						} else {
							starting = true;
							if (soft) {
								if (curvePos3 != null) {
									g.generalPathCurveTo(pathMilkyWay, curvePos1[0],curvePos1[1], curvePos2[0], curvePos2[1], curvePos3[0], curvePos3[1]);
								} else {
									if (curvePos2 != null) {
										g.generalPathQuadTo(pathMilkyWay, curvePos1[0],curvePos1[1], curvePos2[0], curvePos2[1]);
									} else {
										if (curvePos1 != null)
											g.generalPathLineTo(pathMilkyWay, curvePos1[0],curvePos1[1]);
									}
								}
								curvePos1 = null;
								curvePos2 = null;
								curvePos3 = null;
							}
						}
						pos1 = pos0;
					}
				}
			}
		}
		projection.enableCorrectionOfLocalHorizon();
		//projection.setHorizonElevation(he);

		// JAVA BASED METHOD (BUGGY)
/*		if (render.fillMilkyWay && render.drawSkyBelowHorizon && render.projection != PROJECTION.SPHERICAL) {
			g2.setStroke(render.drawMilkyWayStroke);
			g2.setColor(render.drawMilkyWayContoursColor, true);
			//if (render.fillMilkyWay) g2.setColor(render.drawMilkyWayContoursColor, false);
			//int col = g2.getColor();
			if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
				if (render.fillMilkyWay) {
					g2.fill(pathMilkyWay);
				} else {
					g2.draw(pathMilkyWay);
				}
			} else {
				if (render.fillMilkyWay) {
					g2.fill(pathMilkyWay, getDist(milkywayDist));
				} else {
					g2.draw(pathMilkyWay, getDist(milkywayDist));
				}
			}
			pathMilkyWay = g2.getRendering();

			// Check when Java determines the interior of the Milky Way wrongly
			// Maybe poor performance but necessary ...
			if (buffer && this.rec != null && (render.projection == Projection.PROJECTION.STEREOGRAPHICAL ||
					render.projection == Projection.PROJECTION.POLAR)) {
				int wrong = 0;
				boolean wrongFill = false;
				for (int i = lastIndex+1; i < milkyWay.size(); i++)
				{
					loc = (LocationElement) milkyWay.get(i);

					if (loc != null) {
						if (loc.getLongitude() == 2.0 && loc.getLatitude() == 2.0) {
							break;
						}
						if (loc.getLongitude() == 0.0 && loc.getLatitude() == 0.0) continue;
						pos0 = projection.projectPosition(loc, -1, true);
						if (!projection.isInvalid(pos0)) {
							if (render.telescope.invertHorizontal) pos0[0] = render.width-1-pos0[0];
							if (render.telescope.invertVertical) pos0[1] = render.height-1-pos0[1];
							if (this.rec.contains((int) (pos0[0]), (int) (pos0[1])))
							{
				        		if (g2.getRGB(pathMilkyWay, (int) (pos0[0]), (int) (pos0[1])) == render.background) {
				        			wrong ++;
				        			if (wrong > 90) {
				        				//System.out.println(wrong);
					        			wrongFill = true;
					        			break;
				        			}
				        		}
							}
						}
					}
				}

				if (wrongFill) {
//					pathMilkyWay = g2.changeColor(pathMilkyWay, render.background, render.drawMilkyWayContoursColor, true);
					int x0 = (int) rec.getMinX(), y0 = (int) rec.getMinY();
					int xf = (int) rec.getMaxX(), yf = (int) rec.getMaxY();
					for (int i=x0; i<xf; i++) {
						for (int j=y0; j<yf; j++) {
							int rgb = g2.getRGB(pathMilkyWay, i, j);
							if (rgb == render.background) {
								g2.setRGB(pathMilkyWay, i, j, render.drawMilkyWayContoursColor);
							} else {
								g2.setRGB(pathMilkyWay, i, j, render.background);
							}
						}
					}

					return;
				}
			}
//			pathMilkyWay = g2.changeColor(pathMilkyWay, col, render.drawMilkyWayContoursColor, false);
			int x0 = (int) rec.getMinX(), y0 = (int) rec.getMinY();
			int xf = (int) rec.getMaxX(), yf = (int) rec.getMaxY();
			for (int i=x0; i<xf; i++) {
				for (int j=y0; j<yf; j++) {
					int rgb = g2.getRGB(pathMilkyWay, i, j);
					if (rgb != render.background) {
						g2.setRGB(pathMilkyWay, i, j, render.drawMilkyWayContoursColor);
					}
				}
			}

			return;
		}
*/
		float d = 0;
		if (render.anaglyphMode != ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) d = getDist(milkywayDist);
		if (fill) {
			g.enableInversion();

			g.setStroke(JPARSECStroke.STROKE_DEFAULT_LINE);
			g.setColor(render.fillMilkyWayColor, true);
			int col = g.getColor();
			if (x != null) {
				g.drawLines(x, y, index+1, fastMode);
			} else {
				g.draw(pathMilkyWay);
				if (soft) g.draw(pathMilkyWay1);
			}
			if (!render.drawExternalGrid) {
				g.disableInversion();
				int[] clip = g.getClip();
				g.setClip(0, 0, render.width, render.height);
				g.setColor(render.drawCoordinateGridColor, true);
				g.drawStraightLine(rec.getMinX(), rec.getMinY(), rec.getMinX(), rec.getMaxY());
				g.drawStraightLine(rec.getMaxX(), rec.getMinY(), rec.getMaxX(), rec.getMaxY());
				g.drawStraightLine(rec.getMinX(), rec.getMaxY(), rec.getMaxX(), rec.getMaxY());
				g.setClip(clip[0], clip[1], clip[2], clip[3]);
				g.enableInversion();
			}
// 	        if (render.projection == PROJECTION.STEREOGRAPHICAL) drawHorizon(g);
 	        if (render.projection == PROJECTION.SPHERICAL) {
	        	int radius = (int)(Constant.PI_OVER_TWO * pixels_per_radian*1.0);
	        	g.drawOval(this.getXCenter()-radius, this.getYCenter()-radius, 2*radius, 2*radius, render.drawFastLinesMode.fastOvals());
	        }
	        if (render.projection == PROJECTION.POLAR) {
	        	float c[] = projection.projectPosition(new LocationElement(0.0, FastMath.sign(render.centralLatitude)*Constant.PI_OVER_TWO, 1.0), 0, false);
	        	if (c != null) {
		        	int radius = (int)(Constant.PI_OVER_TWO * pixels_per_radian);
		        	g.drawOval(c[0]-radius, c[1]-radius, 2*radius, 2*radius, render.drawFastLinesMode.fastOvals());
	        	}
	        }

			if (g.renderingToExternalGraphics()) g.setStroke(new JPARSECStroke(JPARSECStroke.STROKE_DEFAULT_LINE_THIN, 1));
			for (int i = lastIndex+1; i < rs; i++)
			{
				//int delta = i - lastIndex;
				loc = (float[]) milkyWay.get(i);

				if (loc != null) {
					if (loc[0] == 2.0 && loc[1] == 2.0) {
						break;
					}
					if (loc[0] == 1.0 && loc[1] == 1.0) continue;

					if (projection.obs.getMotherBody() != TARGET.NOT_A_PLANET && projection.eph.isTopocentric)
					{
						if (!render.drawSkyBelowHorizon) {
							LocationElement loc2 = new LocationElement(loc[0], loc[1], 1.0);
							if (render.coordinateSystem != COORDINATE_SYSTEM.EQUATORIAL)
								loc2 = projection.toEquatorialPosition(loc2, false);

							//boolean correct = false;
							//if (render.drawSkyCorrectingLocalHorizon && projection.obs.getMotherBody() == TARGET.EARTH && projection.eph.isTopocentric) correct = true;
							loc2 = CoordinateSystem.equatorialToHorizontal(loc2, projection.ast, projection.obs, projection.eph, false, true);
							if (loc2.getLatitude() < Constant.DEG_TO_RAD) continue;
						}
						if (render.projection == PROJECTION.POLAR && (loc[1] < deg_10 && FastMath.sign(render.centralLatitude) == 1 ||
								loc[1] > -deg_10 && FastMath.sign(render.centralLatitude) == -1)) continue;
						if (render.projection == PROJECTION.SPHERICAL && projection.getApproximateAngularDistance(loc[0], loc[1]) > Constant.PI_OVER_TWO-deg_20) continue;
					}

					pos0 = projection.projectPosition(loc, -1, true);
					if (!projection.isInvalid(pos0)) {
						if (render.telescope.invertHorizontal) pos0[0] = render.width-1-pos0[0];
						if (render.telescope.invertVertical) pos0[1] = render.height-1-pos0[1];
						int rx = (int) (pos0[0]), ry = (int) (pos0[1]); //, rz = -5;
						if (rx < 0 || ry < 0 || rx >= g.getWidth() || ry >= g.getHeight()) continue;
						//if (rx<=graphMarginX-rz || rx >= render.width+rz) continue;
						//if (ry<=leyendMargin-rz || ry >= render.height+rz || ry >= rec.getMaxY()+rz) continue;
						if (rx > rec.getMinX()+1 && rx < rec.getMaxX()-1 && ry > rec.getMinY()+1 && ry < rec.getMaxY()-1)
						{
			        		if (g.getRGB(rx, ry) != col) {
//			        			boolean filled =
			        					floodFill(rx, ry, col, render.background, g, d);
/*			        			if (filled) {
				        			g.setColor(255, 0, 0, 255);
				        			g.fillOval(rx-4, ry-4, 8, 8, false);
				        			g.drawString(""+i, rx, ry+20);
				        			//System.out.println(delta);
			        			}
			        			//break;
*/			        		}
						}
					}
				}
			}
			this.g.enableInversion();
			if ((!render.drawMilkyWayStroke.equals(JPARSECStroke.STROKE_DEFAULT_LINE) && 
					!render.drawMilkyWayStroke.equals(JPARSECStroke.STROKE_DEFAULT_LINE_THIN)) ||
					render.drawMilkyWayContoursColor != render.fillMilkyWayColor) {
				this.g.setStroke(render.drawMilkyWayStroke);
				this.g.setColor(render.drawMilkyWayContoursColor, true);
				if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
					if (x != null) {
						g.drawLines(x, y, index+1, fastMode);
					} else {
						this.g.draw(pathMilkyWay);
						if (soft) this.g.draw(pathMilkyWay1);
					}
				} else {
//					this.g.draw(pathMilkyWay, d);
//					if (soft) this.g.draw(pathMilkyWay1, d);
				}
			}
			if (!render.drawExternalGrid) {
				g.disableInversion();
				int[] clip = g.getClip();
				g.setClip(0, 0, render.width, render.height);
				g.setColor(render.background, true);
				g.drawStraightLine(rec.getMinX(), rec.getMinY(), rec.getMinX(), rec.getMaxY());
				g.drawStraightLine(rec.getMaxX(), rec.getMinY(), rec.getMaxX(), rec.getMaxY());
				g.drawStraightLine(rec.getMinX(), rec.getMaxY(), rec.getMaxX(), rec.getMaxY());
				g.setClip(clip[0], clip[1], clip[2], clip[3]);
				g.enableInversion();
			}
		} else {
			g.setStroke(render.drawMilkyWayStroke);
			g.setColor(render.drawMilkyWayContoursColor, true);
			if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
				if (x != null) {
					g.drawLines(x, y, index+1, fastMode);
				} else {
					g.draw(pathMilkyWay);
					if (soft) g.draw(pathMilkyWay1);
				}
			} else {
				g.draw(pathMilkyWay, d);
				if (soft) g.draw(pathMilkyWay1, d);
			}
		}
	}

	private boolean floodFill(int x, int y, int fillColour, int background, Graphics g, float dist)
	{
		int minX = (int)rec.getMinX(), maxX = (int)rec.getMaxX();
		int minY = (int)rec.getMinY(), maxY = (int)rec.getMaxY();
		if (render.telescope.invertHorizontal) {
			x = render.width-1-x;
			int tmp = render.width-1-minX;
			minX = render.width-1-maxX;
			maxX = tmp;
		}
		if (render.telescope.invertVertical) {
			y = render.height-1-y;
			int tmp = render.height-1-minY;
			minY = render.height-1-maxY;
			maxY = tmp;
		}

		//***Get starting color.
		int startPixel = g.getRGB(x, y);

		if (startPixel == fillColour || startPixel != background) return false;
		g.setColor(fillColour, true);

		int width = g.getWidth(), height = g.getHeight();
		BitSet pixelsChecked = new BitSet(width*height);
		Queue<FloodFillRange> ranges = new LinkedList<FloodFillRange>();

		//***Do first call to floodfill.
		LinearFill(x,  y, width, fillColour, minX, maxX, startPixel, pixelsChecked, ranges, g, dist); //, col0, y);

		//***Call floodfill routine while floodfill ranges still exist on the queue
		FloodFillRange range;
		while (ranges.size() > 0)
		{
			//**Get Next Range Off the Queue
			range = ranges.remove();

			//**Check Above and Below Each Pixel in the Floodfill Range
			int upY = range.Y - 1;//so we can pass the y coord by ref
			int downY = range.Y + 1;
			int offU = width * upY;
			int offD = width * downY;
			for (int i = range.startX; i <= range.endX; i++)
			{
				//*Start Fill Upwards
				if (i > minX && i < maxX) {
					//if we're not above the top of the bitmap and the pixel above this one is within the color tolerance
					if (range.Y > minY+2 && !pixelsChecked.get(offU+i) && CheckPixel(i, upY, startPixel, g))
						LinearFill( i,  upY, width, fillColour, minX, maxX, startPixel, pixelsChecked, ranges, g, dist);

					//*Start Fill Downwards
					//if we're not below the bottom of the bitmap and the pixel below this one is within the color tolerance
					if (range.Y < maxY-2 && !pixelsChecked.get(offD+i) && CheckPixel(i, downY, startPixel, g))
						LinearFill( i,  downY, width, fillColour, minX, maxX, startPixel,  pixelsChecked, ranges, g, dist);
				}
			}
		}
		return true;
	}
	// Finds the furthermost left and right boundaries of the fill area
	// on a given y coordinate, starting from a given x coordinate, filling as it goes.
	// Adds the resulting horizontal range to the queue of floodfill ranges,
	// to be processed in the main loop.
	//
	// int x, int y: The starting coords
	protected void LinearFill(int x, int y, int width, int fillColour,
			int minX, int maxX,
			int startPixel, BitSet pixelsChecked, Queue<FloodFillRange> ranges, Graphics g, float dist)
	{
		//***Find Left Edge of Color Area
		int lFillLoc = x; //the location to check/fill on the left
		int off = width*y;
		while (true)
		{
			//**fill with the color
			//if (lFillLoc != x) g.fillOval(lFillLoc, y, 1, 1);
			//**indicate that this pixel has already been checked and filled
			pixelsChecked.set(off+lFillLoc, true);
			//**de-increment
			lFillLoc--;     //de-increment counter
			//**exit loop if we're at edge of bitmap or color area
			if (lFillLoc <= minX || pixelsChecked.get(off+lFillLoc) || !CheckPixel(lFillLoc, y, startPixel, g))
				break;
		}
		// fillOval of size 1 uses setRGB, and also drawStraightLine. But note that the drawLine
		// method of Graphics is very slow even compared to setRGB pixel by pixel ... Using setRGB
		// means also we cannot use real transparency.
		lFillLoc++;

		//***Find Right Edge of Color Area
		int rFillLoc = x; //the location to check/fill on the left
		while (true)
		{
			//**fill with the color
			//g.fillOval(rFillLoc, y, 1, 1);
			//**indicate that this pixel has already been checked and filled
			pixelsChecked.set(off+rFillLoc, true);
			//**increment
			rFillLoc++;     //increment counter
			//**exit loop if we're at edge of bitmap or color area
			if (rFillLoc >= maxX || pixelsChecked.get(off+rFillLoc) || !CheckPixel(rFillLoc, y, startPixel, g))
				break;
		}
		rFillLoc--;
		if (lFillLoc < rFillLoc) {
			g.drawStraightLine(lFillLoc, y, rFillLoc, y, dist, dist);

			//add range to queue
			FloodFillRange r = new FloodFillRange(lFillLoc, rFillLoc, y);
			ranges.offer(r);
		}
	}
	//Sees if a pixel is within the color tolerance range.
	protected boolean CheckPixel(int x, int y, int startPixel, Graphics g)
	{
		if (x >= g.getWidth() || x < 0 || y < 0 || y >= g.getHeight()) return false;
		return g.getRGB(x, y) == startPixel;
	}
	// Represents a linear range to be filled and branched from.
	protected class FloodFillRange
	{
		public int startX;
		public int endX;
		public int Y;

		public FloodFillRange(int startX, int endX, int y)
		{
		    this.startX = startX;
		    this.endX = endX;
		    this.Y = y;
		}
	}

	private void drawMilkyWayTexture(int init, int iend, int jnit, int jend, int step, float size, int imax, double im2, double jm2, float d,
			Object milkyWayTexture, Graphics g, float scale, boolean isWMAP) throws JPARSECException {
		//if (Math.abs(Functions.toCenturies(jd)) < 10) return;

		int jnit0 = (int) (0.5 + imax * 500.0 / 3000.0), jend0 = (int) (imax * 950.0 / 3000.0);
		if (isWMAP) {
			jnit0 = 0;
			jend0 = imax/2;
		}

		// Hold Milky Way in memory disabled since it needs 26 MB and it just only improves
		// performance a factor 2
/*		ArrayList<Object> mw = null;
		Object o = DataBase.getData("milkyWayTexture", threadID, true);
		boolean read = false, save = false;
		if (o != null) {
			mw = new ArrayList<Object>(Arrays.asList((Object[]) o));
			read = true;
		} else {
			mw = new ArrayList<Object>();
			int rs = (jend0-jnit0)*imax;
			for (int ii=0; ii<rs;ii++) {
				mw.add(null);
			}
		}
*/
		int andromedai0 = (int) (0.5 + imax * 460.0 / 3000.0);
		int andromedai1 = (int) (0.5 + imax * 515.0 / 3000.0);
		int andromedaj0 = (int) (0.5 + imax * 915.0 / 3000.0);
		init = (int) Functions.module(init, imax);
		iend = (int) Functions.module(iend, imax);
		if (iend < init) iend += imax;
		boolean useRGB = (render.getColorMode() != COLOR_MODE.NIGHT_MODE);
		boolean useGray = useRGB && render.drawHorizonTexture != HORIZON_TEXTURE.NONE;
		if (useGray) {
			step *= 2;
			size *= 2;
		}
		int size2 = (int) (2*size)+1;
		float size2s = size2*scale;
		float sizes = size*scale;
		for (int j=jnit0; j<jend0; j = j + step) {
			for (int ii=init; ii<iend; ii = ii + step) {
				int i = ii; //(int) Functions.module(ii, imax);
				if (i >= imax) i -= imax;
				if (!isWMAP && i > andromedai0 && i < andromedai1 && j > andromedaj0) continue;
				if (j >= jnit && j <= jend) {
					// Note here aberration + nutation is not corrected, only precession, since texture resolution is 7' and both
					// effects amount to +/- 0.5'
					LocationElement loc = null;

/*					int mwi = i+(j-jnit0)*imax;
					if (read) {
						MilkyWayData f = (MilkyWayData)mw.get(mwi);
						if (f != null) loc = new LocationElement(f.x, f.y, f.z);
					}
					if (loc == null) {
*/						loc = new LocationElement(-(i/im2-1.0)*Math.PI, (1.0-j/jm2)*Constant.PI_OVER_TWO, 1.0);
						loc = LocationElement.parseRectangularCoordinatesFast(precessFromJ2000(jd, LocationElement.parseLocationElementFast(CoordinateSystem.galacticToEquatorial(loc, Constant.J2000, true)), projection.eph));
						if (projection.obs.getMotherBody() != TARGET.EARTH && projection.obs.getMotherBody() != TARGET.NOT_A_PLANET) {
							loc = projection.getPositionFromBody(loc, true);
						}
						loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, true, true, 0);

						if (loc == null) continue;

						g.disableInversion();
						int rgb = g.getRGB(milkyWayTexture, i, j);
						int alpha = 16 + (((rgb>>16)&255) + ((rgb>>8)&255) + (rgb&255)) / 3;
						if (alpha > 254) alpha = 254;
						if (useGray) {
							int a0 = alpha - 16, a02 = (a0 * 2) / 3;;
							rgb = Functions.getColor(a02, a02, a0, a0);
							alpha = Math.min(a0/4 - 5, 40);
							if (alpha < 0) alpha = 0;
						}
						if (!useRGB) rgb = ((rgb>>16)&255) << 16;
						g.setColor(rgb, alpha);
						loc.setRadius(g.getColor());
//						mw.set(mwi, new MilkyWayData((float)loc.getLongitude(), (float)loc.getLatitude(), g.getColor()));
//						save = true;
						g.enableInversion();
//					}
					float pos[] = projection.projectPosition(loc, 0, false);
					if (pos == null || !this.isInTheScreen((int)pos[0], (int)pos[1], (int)size)) continue;
						g.setColor((int)loc.getRadius(), true);
						if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
							if (scale > 1) {
								if (fieldDeg > 50)
									g.setColor((int)loc.getRadius(), g.getAlpha((int)loc.getRadius())/2);
								pos[0] = scale*pos[0];
								pos[1] = scale*pos[1];
								g.fillRect((int)(0.5+pos[0])-sizes, (int)(0.5+pos[1])-sizes, size2s, size2s);
							} else {
								g.fillRect((int)(0.5+pos[0])-size, (int)(0.5+pos[1])-size, size2, size2);
							}
						} else {
							g.fillRect((int)(0.5+pos[0])-size, (int)(0.5+pos[1])-size, size2, size2, d);
						}
				}
			}
		}
		g.enableInversion();
//		if (save) DataBase.addData("milkyWayTexture", threadID, mw.toArray(), true);
	}

	class MilkyWayData {
		public float x, y;
		public int z;
		public MilkyWayData(float x, float y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}

	private void drawMilkyWayTexture2(int init, int iend, final int jnit, final int jend, int step, float size, int imax, double im2, double jm2, float d,
			Object milkyWayTexture, Graphics g, float scale) throws JPARSECException {
		//if (Math.abs(Functions.toCenturies(jd)) < 10) return;

		int i = (int) Functions.module((init + iend)/2, imax);
		LocationElement loc = new LocationElement(-(i/im2-1.0)*Math.PI, (1.0-(jnit + jend)/(2*jm2))*Constant.PI_OVER_TWO, 1.0);
		loc = LocationElement.parseRectangularCoordinatesFast(precessFromJ2000(jd, CoordinateSystem.galacticToEquatorial(loc, Constant.J2000, true).getRectangularCoordinates(), projection.eph));
		LocationElement loc2 = loc0Date; //projection.getEquatorialPositionOfRendering();
		double dist = LocationElement.getApproximateAngularDistance(loc, loc2);
		if (dist > field*0.75) return;

		init = (int) Functions.module(init, imax);
		iend = (int) Functions.module(iend, imax);
		if (iend < init) iend += imax;
		boolean useRGB = (render.getColorMode() != COLOR_MODE.NIGHT_MODE);
		boolean useGray = useRGB && render.drawHorizonTexture != HORIZON_TEXTURE.NONE;
		if (useGray) {
			step *= 2;
			size *= 2;
		}
		int size2 = (int)(2*size)+1;
		float size2s = size2*scale;
		float sizes = size*scale;
		for (int j=jnit; j<jend; j = j + step) {
			for (int ii=init; ii<iend; ii = ii + step) {
				i = ii;
				if (i >= imax) i-=imax; //(int) Functions.module(ii, imax);
				loc = new LocationElement(-(i/im2-1.0)*Math.PI, (1.0-j/jm2)*Constant.PI_OVER_TWO, 1.0);
				// Note here aberration + nutation is not corrected, only precession, since texture resolution is 7' and both
				// effects amount to +/- 0.5'
				loc = LocationElement.parseRectangularCoordinatesFast(precessFromJ2000(jd, LocationElement.parseLocationElementFast(CoordinateSystem.galacticToEquatorial(loc, Constant.J2000, true)), projection.eph));
				if (projection.obs.getMotherBody() != TARGET.EARTH && projection.obs.getMotherBody() != TARGET.NOT_A_PLANET) {
					loc = projection.getPositionFromBody(loc, true);
				}
				loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, true, true, 0);
				float pos[] = projection.projectPosition(loc, 0, false);
				if (pos == null || !this.isInTheScreen((int)pos[0], (int)pos[1], (int)size)) continue;
					g.disableInversion();
					int rgb = g.getRGB(milkyWayTexture, i, j);
					g.enableInversion();
					int alpha = 16 + (((rgb>>16)&255) + ((rgb>>8)&255) + (rgb&255)) / 3;
					if (alpha > 255) alpha = 255;
					if (useGray) {
						int a0 = alpha - 16, a02 = (a0 * 2) / 3;;
						rgb = Functions.getColor(a02, a02, a0, a0);
						alpha = Math.min(a0/4 - 5, 40);
						if (alpha < 0) alpha = 0;
					}
					if (!useRGB) rgb = ((rgb>>16)&255) << 16;

					g.setColor(rgb, alpha);
					if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
						if (scale > 1) {
							pos[0] = scale*pos[0];
							pos[1] = scale*pos[1];
							g.fillRect((int)(0.5+pos[0])-sizes, (int)(0.5+pos[1])-sizes, size2s, size2s);
						} else {
							g.fillRect((int)(0.5+pos[0])-size, (int)(0.5+pos[1])-size, size2, size2);
						}
					} else {
						g.fillRect((int)(0.5+pos[0])-size, (int)(0.5+pos[1])-size, size2, size2, d);
					}
			}
		}
	}

	// Draw radiants of meteor showers
	private void drawMeteorShowers() throws JPARSECException {
		if (!render.drawMeteorShowers || projection.obs.getMotherBody() != TARGET.EARTH)
			return;

		ArrayList<Object> meteors = null;
		Object o = null;
		if (db_meteor >= 0) {
			o = DataBase.getData(db_meteor);
		} else {
			o = DataBase.getData("meteor", threadID, true);
		}
		if (o != null) meteors = new ArrayList<Object>(Arrays.asList((Object[]) o));
		if (meteors == null) {
			meteors = new ArrayList<Object>();
			ArrayList<String> meteor = ReadFile.readResource(FileIO.DATA_SKY_DIRECTORY + "meteorShowers.txt", ReadFile.ENCODING_UTF_8);
			AstroDate astro = new AstroDate(jd);
			int year = astro.getYear();
			AstroDate astro0 = new AstroDate(year, 1, 1);
			double jd0 = astro0.jd();
			String months[] = new String[] {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
			for (int i=1;i<meteor.size(); i++) {
				String li = meteor.get(i);
				String name = FileIO.getField(1, li, ";", false);
				if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) {
					int p = name.indexOf("(");
					if (p > 0) name = name.substring(0, p).trim();
				} else {
					name = FileIO.getField(10, li, ";", false);
				}
				double sunLon = Double.parseDouble(FileIO.getField(4, li, ";", false));
				double radRA = Double.parseDouble(FileIO.getField(5, li, ";", false)) * Constant.DEG_TO_RAD;
				double radDEC = Double.parseDouble(FileIO.getField(6, li, ";", false)) * Constant.DEG_TO_RAD;
				String thz = FileIO.getField(9, li, ";", false);

				String lim = FileIO.getField(2, li, ";", false);
				String lmin = FileIO.getField(1, lim, "-", false);
				String lmax = FileIO.getField(2, lim, "-", false);
				String lminm = FileIO.getField(1, lmin, " ", false).trim();
				String lmind = FileIO.getField(2, lmin, " ", false).trim();
				String lmaxm = FileIO.getField(1, lmax, " ", false).trim();
				String lmaxd = FileIO.getField(2, lmax, " ", false).trim();
				String max = FileIO.getField(3, li, ";", false);
				String maxm = FileIO.getField(1, max, " ", false).trim();
				String maxd = FileIO.getField(2, max, " ", false).trim();
				AstroDate lmina = new AstroDate(year, 1 + DataSet.getIndex(months, lminm), Integer.parseInt(lmind));
				AstroDate lmaxa = new AstroDate(year, 1 + DataSet.getIndex(months, lmaxm), Integer.parseInt(lmaxd));
				AstroDate maxa = new AstroDate(year, 1 + DataSet.getIndex(months, maxm), Integer.parseInt(maxd));

				LocationElement lon = LocationElement.parseRectangularCoordinates(Precession.precessPosAndVelInEcliptic(Constant.J2000, jd, LocationElement.parseLocationElement(new LocationElement(sunLon * Constant.DEG_TO_RAD, 0, 1)), projection.eph));
				double time = Calendar.solarLongitudeAfter(Calendar.fixedFromJD(jd0), lon.getLongitude() * Constant.RAD_TO_DEG);
				time = 1721424.5 + time;
				TimeElement t = new TimeElement(time, SCALE.UNIVERSAL_TIME_UT1);
				double tdb = TimeScale.getJD(t, projection.obs, projection.eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);

				double mint = tdb - Math.abs(maxa.jd() - lmina.jd());
				double maxt = tdb + Math.abs(maxa.jd() - lmaxa.jd());
				int rate = -1;
				if (DataSet.isDoubleStrictCheck(thz)) rate = Integer.parseInt(thz);
				boolean active = false;
				if (jd > mint && jd < maxt) active = true;
				LocationElement loc = Ephem.fromJ2000ToApparentGeocentricEquatorial(new LocationElement(radRA, radDEC, 1.0), projection.time, projection.obs, projection.eph);
				if (projection.obs.getMotherBody() != TARGET.EARTH && projection.obs.getMotherBody() != TARGET.NOT_A_PLANET)
					loc = projection.getPositionFromBody(loc, true);
				loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, true, true, 0);
				if (loc == null || (render.drawMeteorShowersOnlyActive && !active)) continue;
				String month = t.astroDate.getMonthName().substring(0, 3).toLowerCase();
				if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) {
					name += ", "+t.astroDate.getDay()+" "+month;
				} else {
					name += ", "+month+" "+t.astroDate.getDay();
				}
				meteors.add(new Object[] {name, rate, loc, active});
			}
			DataBase.addData("meteor", threadID, meteors.toArray(), true);
			db_meteor = DataBase.getIndex("meteor", threadID);
		}

		Object obj[] = null;
		float pos0[] = null;
		int radius2 = 8;
		FONT f = render.drawMinorObjectsNamesFont;
		f = FONT.getDerivedFont(f, f.getSize(), 0);
		ArrayList<Object> minorObjects = null;
		if (SaveObjectsToAllowSearch) {
			minorObjects = new ArrayList<Object>();
			Object oo = null;
			if (db_minorObjects >= 0) {
				oo = DataBase.getData(db_minorObjects);
			} else {
				oo = DataBase.getData("minorObjects", threadID, true);
			}
			if (oo != null) minorObjects = new ArrayList<Object>(Arrays.asList(((Object[]) oo)));
		}
		for (Iterator<Object> itr = meteors.iterator();itr.hasNext();)
		{
			obj = (Object[]) itr.next();

			boolean active = (Boolean) obj[3];
			if (!active && render.drawMeteorShowersOnlyActive) continue;

			LocationElement loc = (LocationElement) obj[2];
			pos0 = projection.projectPosition(loc, 0, false);
			if (pos0 == null || !this.isInTheScreen((int)pos0[0], (int)pos0[1], 0)) continue;

			String name = (String) obj[0];
			int thz = (Integer) obj[1];
			FONT ff = f;
			if (active && !render.drawMeteorShowersOnlyActive) ff = FONT.getDerivedFont(ff, ff.getSize(), 1);

			drawMS(g, pos0, active, thz);
			if (render.drawMinorObjectsLabels) drawString(g.getColor(), ff, name, pos0[0], pos0[1], -(radius2+f.getSize()), false);
			if (SaveObjectsToAllowSearch) {
				minorObjects.add(new Object[] {
						RenderSky.OBJECT.METEOR_SHOWER, pos0.clone(), loc, (float) thz, name
				});
			}
		}
		if (SaveObjectsToAllowSearch) {
			DataBase.addData("minorObjects", threadID, minorObjects.toArray(), true);
			if (db_minorObjects < 0) db_minorObjects = DataBase.getIndex("minorObjects", threadID);
		}
	}

	private void drawMS(Graphics g, float pos0[], boolean active, int thz) {
		int radius1 = 2, radius2 = 8;
		int d1 = (int) (radius1 * FastMath.cos(45 * Constant.DEG_TO_RAD)), d2 = (int) (radius2 * FastMath.cos(45 * Constant.DEG_TO_RAD));
		if (active && !render.drawMeteorShowersOnlyActive) {
			g.setColor(render.drawMeteorShowersColor, false);
		} else {
			g.setColor(render.drawMeteorShowersColor, true);
		}
		if (thz < 30) {
			g.setStroke(JPARSECStroke.STROKE_DEFAULT_LINE_THIN);
		} else {
			g.setStroke(new JPARSECStroke(JPARSECStroke.STROKE_DEFAULT_LINE_THIN, render.drawDeepSkyObjectsStroke.getLineWidth()*(1f+thz/30f)));
		}

		g.drawLine(pos0[0]+radius1, pos0[1], pos0[0]+radius2, pos0[1], fast);
		g.drawLine(pos0[0]-radius1, pos0[1], pos0[0]-radius2, pos0[1], fast);
		g.drawLine(pos0[0], pos0[1]+radius1, pos0[0], pos0[1]+radius2, fast);
		g.drawLine(pos0[0], pos0[1]-radius1, pos0[0], pos0[1]-radius2, fast);

		g.drawLine(pos0[0]+d1, pos0[1]+d1, pos0[0]+d2, pos0[1]+d2, fast);
		g.drawLine(pos0[0]-d1, pos0[1]-d1, pos0[0]-d2, pos0[1]-d2, fast);
		g.drawLine(pos0[0]+d1, pos0[1]-d1, pos0[0]+d2, pos0[1]-d2, fast);
		g.drawLine(pos0[0]-d1, pos0[1]+d1, pos0[0]-d2, pos0[1]+d2, fast);
	}

	// Draw nebula shapes
	private void drawNebulae() throws JPARSECException
	{
		if (!render.drawNebulaeContours)
			return;

		float pos0[] = new float[2];
		float pos1[] = new float[2];

		g.setStroke(render.drawNebulaeStroke);

		float[] curvePos1 = null, curvePos2 = null;
		boolean soft = true;
		int step = 1;
		projection.createNewArrayWhenProjecting = true;
		if (g.renderingToAndroid() ||
				(pixels_per_degree < 17 && render.drawClever && !g.renderingToExternalGraphics())) {
			soft = false;
			step = 2;
			if (pixels_per_degree < 11) step = 3;
			if (pixels_per_degree < 7) step = 4;
			if (pixels_per_degree < 4) step = 6;
			projection.createNewArrayWhenProjecting = false;
			if (g.renderingToAndroid()) step += 2;
		}

		projection.enableCorrectionOfLocalHorizon();

		// Nebula
		Object path = g.generalPathInitialize();
		ArrayList<Object> nebula = null;
		Object o = null;
		if (db_nebula >= 0) {
			o = DataBase.getData(db_nebula);
		} else {
			o = DataBase.getData("nebula", threadID, true);
		}
		if (o != null) nebula = new ArrayList<Object>(Arrays.asList((Object[]) o));
		if (nebula == null) {
			nebula = new ArrayList<Object>();
			boolean skip = false;
			if (baryc == null) baryc = Ephem.eclipticToEquatorial(PlanetEphem.getGeocentricPosition(equinox, TARGET.Solar_System_Barycenter, 0.0, false, projection.obs), Constant.J2000, projection.eph);

			InputStream is = null;
			BufferedReader dis = null;
			try
			{
				is = getClass().getClassLoader().getResourceAsStream(FileIO.DATA_SKY_DIRECTORY + "shapes.txt");
				dis = new BufferedReader(new InputStreamReader(is, ReadFile.ENCODING_ISO_8859));
				String line = "";
				while ((line = dis.readLine()) != null)
				{
					if (line.equals("0 0.737 40.916 ")) {
						nebula.add(new Object[] {new float[] {0, 0}, 0});
						break; // don't read M31 and M33 outlines since there are real photos
					}

					int flag = Integer.parseInt(FileIO.getField(1, line, " ", true));
					double ra = Float.parseFloat(FileIO.getField(2, line, " ", true)) / Constant.RAD_TO_HOUR;
					double dec = Float.parseFloat(FileIO.getField(3, line, " ", true)) * Constant.DEG_TO_RAD;

					LocationElement loc = new LocationElement(ra, dec, 1.0);
					if (equinox != Constant.J2000) {
						// Correct for aberration, precession, and nutation
						if (projection.eph.ephemType == COORDINATES_TYPE.APPARENT) {
							loc.setRadius(1000 * Constant.RAD_TO_ARCSEC); // 1000 pc
							double light_time = loc.getRadius() * Constant.LIGHT_TIME_DAYS_PER_AU;
							double r[] = Ephem.aberration(loc.getRectangularCoordinates(), baryc, light_time);

							r = precessFromJ2000(equinox, r, projection.eph);
							loc = LocationElement.parseRectangularCoordinates(nutateInEquatorialCoordinates(equinox, projection.eph, r, true));
						} else {
							loc = LocationElement.parseRectangularCoordinates(precessFromJ2000(equinox,
									LocationElement.parseLocationElement(loc), projection.eph));
						}
					}

					if (projection.obs.getMotherBody() != TARGET.EARTH && projection.obs.getMotherBody() != TARGET.NOT_A_PLANET) {
						loc = projection.getPositionFromBody(loc, true);
					}
					loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, true, true, 0);
					if (flag < 0) {
						skip = false;
						if (loc == null) skip = true;
					}
					if (!skip) {
						if (loc == null) {
							nebula.add(new Object[] {null, flag});
						} else {
							nebula.add(new Object[] {new float[] {(float)loc.getLongitude(), (float)loc.getLatitude()}, flag});
						}
					}
				}
				// Close file
				dis.close();
				is.close();
				is = null;
				DataBase.addData("nebula", threadID, nebula.toArray(), true);
				db_nebula = DataBase.getIndex("nebula", threadID);
			} catch (Exception e2)
			{
				try {
					if (dis != null) dis.close();
					if (is != null) is.close();
					is = null;
				} catch (Exception exc) {}
				throw new JPARSECException(
						"error while reading objects file.", e2);
			}
		}
		g.setColor(render.drawBrightNebulaeContoursColor, true);
		boolean bright = true, valid = true;
		pos1 = Projection.INVALID_POSITION;
		boolean check = true;
		curvePos1 = null;
		curvePos2 = null;
		int cte = (int) (pixels_per_degree*10);
		float[] loc;
		int flag;
		Object[] obj;
		float dist = getDist(nebulaDist);
		double width2 = render.width * render.width;
		boolean first = true;
		double cr2 = FastMath.pow(50 * this.pixels_per_degree, 2);
		int s = nebula.size();
		for (int n=0; n<s; n++)
		{
			obj = (Object[]) nebula.get(n);
			flag = (Integer) obj[1];

			if (flag != 0 && step > 1) {
				n++;
				obj = (Object[]) nebula.get(n);
				flag = (Integer) obj[1];
				if (flag != 0 && step > 2) {
					for (int k=2; k<step; k++) {
						if (n < s-1 && flag != 0) {
							n++;
							obj = (Object[]) nebula.get(n);
							flag = (Integer) obj[1];
						}
					}
				}
			}

			if (valid) {
				pos0 = projection.projectPosition((float[]) obj[0], cte, check);
				if ((render.projection == Projection.PROJECTION.CYLINDRICAL_EQUIDISTANT || render.projection == Projection.PROJECTION.CYLINDRICAL || projection.isCylindricalForced()) && !projection.isInvalid(pos0) && flag > 0) {
					double r2 = FastMath.pow(pos0[0] - pos1[0], 2.0) + FastMath.pow(pos0[1] - pos1[1], 2.0);
					if (r2 > width2 || (r2 > cr2 && render.projection == Projection.PROJECTION.CYLINDRICAL_EQUIDISTANT)) pos0 = Projection.INVALID_POSITION;
				}

				if (flag > 0) {
					if (projection.isInvalid(pos0)) valid = false;
					if (soft) {
						if (curvePos1 == null) {
							curvePos1 = pos0;
						} else {
							if (curvePos2 == null) {
								curvePos2 = pos0;
							} else {
								if (valid) {
									g.generalPathCurveTo(path, curvePos1[0], curvePos1[1], curvePos2[0], curvePos2[1], pos0[0],pos0[1]);
								} else {
									g.generalPathQuadTo(path, curvePos1[0], curvePos1[1], curvePos2[0], curvePos2[1]);
								}
								curvePos1 = null;
								curvePos2 = null;
							}
						}
					} else {
						if (valid) g.generalPathLineTo(path, pos0[0],pos0[1]);
					}
					if (flag == 1) {
						bright = true; // bright nebula
					} else {
						bright = false; // dark nebula
					}
				} else {
					if (valid && !first) {
						if (soft) {
							if (curvePos2 != null) {
								g.generalPathQuadTo(path, curvePos1[0],curvePos1[1], curvePos2[0], curvePos2[1]);
							} else {
								if (curvePos1 != null)
									g.generalPathLineTo(path, curvePos1[0],curvePos1[1]);
							}
						}
						g.generalPathClosePath(path);
						if (bright) {
							if (render.fillNebulae) {
								g.setColor(render.drawBrightNebulaeContoursColor, true);
								if (render.fillBrightNebulaeColor != -1) g.setColor(render.fillBrightNebulaeColor, true);
								g.fill(path, dist);
							}
							if (render.fillBrightNebulaeColor != -1 || !render.fillNebulae) {
								g.setColor(render.drawBrightNebulaeContoursColor, true);
								g.draw(path, dist);
							}
						} else {
							g.setColor(render.drawDarkNebulaeContoursColor, true);
							if (render.fillNebulae) {
								g.fill(path, dist);
							} else {
								g.draw(path, dist);
							}
						}
					}
					valid = !projection.isInvalid(pos0);
					if (valid) {
						path = g.generalPathInitialize();
						g.generalPathMoveTo(path, pos0[0],pos0[1]);
					}
					curvePos1 = null;
					curvePos2 = null;
				}
			} else {
				if (flag == 0) {
					loc = (float[]) obj[0];
					pos0 = projection.projectPosition(loc, cte, check);
					valid = !projection.isInvalid(pos0);
					if (valid) {
						path = g.generalPathInitialize();
						g.generalPathMoveTo(path, pos0[0],pos0[1]);
					}
					curvePos1 = null;
					curvePos2 = null;
				}
			}
			pos1 = pos0;
			first = false;
		}
		projection.createNewArrayWhenProjecting = true;
	}

	// Draw constellation limits
	private static double refJD = Constant.B1900 - 25.0 * Constant.TROPICAL_YEAR;
	private static double refVal = 5023 / (Constant.JULIAN_DAYS_PER_CENTURY * 3600.0);
	private static double lastConLimStep = 10;
	private void drawConstelLimits() throws JPARSECException
	{
		if (!render.drawConstellationLimits || ((fieldDeg > 125 || fieldDeg < 3) && render.drawClever)) {
			return;
		}

		Object pathConLim = g.generalPathInitialize();

		if (render.drawClever) {
			double conlimStep = 600.0 / pixels_per_degree;
			if (conlimStep < 10) conlimStep = 10;
			if (lastConLimStep != conlimStep || lastConLimStep == -1) {
				DataBase.addData("conlim", threadID, null, true);
				db_conlim = -1;
			}
			lastConLimStep = conlimStep;
		}
		ArrayList<Object> conlim = null;
		Object o = null;
		if (db_conlim >= 0) {
			o = DataBase.getData(db_conlim);
		} else {
			o = DataBase.getData("conlim", threadID, true);
		}
		if (o != null) conlim = new ArrayList<Object>(Arrays.asList((Object[]) o));
		if (conlim == null) {
			double step = lastConLimStep / 3000.0;
			String[] avoidRepeatedLines = new String[0];
			conlim = new ArrayList<Object>();
			double ra1 = 0.0, dec1 = 0.0;

			InputStream is = null;
			BufferedReader dis = null;
			try
			{
				is = getClass().getClassLoader().getResourceAsStream(FileIO.DATA_SKY_DIRECTORY + "conlim.txt");
				dis = new BufferedReader(new InputStreamReader(is, ReadFile.ENCODING_ISO_8859));
				String line = "";
				while ((line = dis.readLine()) != null)
				{
					line = line.trim();
					int n = FileIO.getNumberOfFields(line, " ", true);
					double ra = Double.parseDouble(FileIO.getField(1, line, " ", true)) / Constant.RAD_TO_HOUR;
					double dec = Double.parseDouble(FileIO.getField(2, line, " ", true)) * Constant.DEG_TO_RAD;
					LocationElement loc = new LocationElement(ra, dec, 1.0);
					if (n == 4)
					{
						double iniDec = dec1, finDec = dec;
						double iniRa = ra1, finRa = ra;
						String lineC = ""+ra+"/"+dec+"/"+ra1+"/"+dec1;
						String lineI = ""+ra1+"/"+dec1+"/"+ra+"/"+dec;
						int index = DataSet.getIndex(avoidRepeatedLines, lineC);
						if (index == -1)  index = DataSet.getIndex(avoidRepeatedLines, lineI);
						if (index == -1)
						{
							avoidRepeatedLines = DataSet.addStringArray(avoidRepeatedLines, lineC);

							if (iniRa > 22.0/Constant.RAD_TO_HOUR && finRa < 10.0/Constant.RAD_TO_HOUR) {
								finRa += Constant.TWO_PI;
							} else {
								if (iniRa < 10.0/Constant.RAD_TO_HOUR && finRa > 22.0/Constant.RAD_TO_HOUR)
									iniRa += Constant.TWO_PI;
							}
							double dist = LocationElement.getApproximateAngularDistance(loc, new LocationElement(ra1, dec1, 1));
							double fac = Math.abs(FastMath.sin((iniDec+finDec)*0.5));
							if (fac < 0.1) fac = 0.1;
							int maxIter = (int) ((dist/step) * fac);
							if (maxIter == 0) maxIter = 1;
							double stepRA = (finRa-iniRa)/(double) maxIter;
							double stepDEC = (finDec-iniDec)/(double) maxIter;
							double r = iniRa-stepRA, d = iniDec-stepDEC;
							int nit=0;
							boolean started = false;
							do
							{
								nit ++;
								r += stepRA;
								d += stepDEC;
								loc = new LocationElement(r, d, 1.0);
								if (jd != refJD)
									loc = LocationElement.parseRectangularCoordinates(Precession.precess(refJD, projection.eph.getEpoch(jd),
											LocationElement.parseLocationElement(loc), projection.eph));

								if (projection.obs.getMotherBody() != TARGET.EARTH && projection.obs.getMotherBody() != TARGET.NOT_A_PLANET) {
									loc = projection.getPositionFromBody(loc, true);
								}
								loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, true, true, 0);
								if (loc != null) {
									if (!started) {
										started = true;
										conlim.add(new float[] {(float) loc.getLongitude(), (float) loc.getLatitude(), 0, 0});
									} else {
										conlim.add(new float[] {(float) loc.getLongitude(), (float) loc.getLatitude()});
									}

									ra1 = loc.getLongitude();
									dec1 = loc.getLatitude();
								}
							} while (nit<=maxIter);
						}
					}
					ra1 = ra;
					dec1 = dec;
				}
				// Close file
				dis.close();
				is.close();
				is = null;
				DataBase.addData("conlim", threadID, conlim.toArray(), true);
				db_conlim = DataBase.getIndex("conlim", threadID);
			} catch (Exception e2)
			{
				try {
					if (dis != null) dis.close();
					if (is != null) is.close();
					is = null;
				} catch (Exception exc) {}
				throw new JPARSECException(
						"error while reading constellation limits file.", e2);
			}
		}
		float pos0[] = new float[2];
		float pos1[] = new float[2];
		boolean start = false;
		int size = (int) (pixels_per_degree*20);
		//LocationElement loc0, loc1;
		float val[];
		float oldpos0[] = null;
		for (Iterator<Object> itr = conlim.iterator();itr.hasNext();)
		{
			val = (float[]) itr.next();
			if (val.length == 2) {
				//loc0 = new LocationElement(val[0], val[1], 1.0);
				pos0 = projection.projectPosition(val, size, false);

				if (!projection.isInvalid(pos0))
				{
					if (!start) {
						g.generalPathMoveTo(pathConLim, pos0[0], pos0[1]);
						start = true;
					} else {
						if (oldpos0 != null && (this.isInTheScreen((int)pos0[0], (int)pos0[1], size) ||
								render.telescope.invertHorizontal || render.telescope.invertVertical ||
								rec.isLineIntersectingRectangle(oldpos0[0], oldpos0[1], pos0[0], pos0[1]))) {
							g.generalPathLineTo(pathConLim, pos0[0], pos0[1]);
						}
					}
					oldpos0 = pos0;
				}
			} else {
				//loc1 = new LocationElement(val[2], val[3], 1.0);
				pos1 = projection.projectPosition(val, 0, false);
				start = false;
				oldpos0 = null;
				if (!projection.isInvalid(pos1))
				{
					if (!this.isInTheScreen((int)pos1[0], (int)pos1[1], size)) continue;
					g.generalPathMoveTo(pathConLim, pos1[0], pos1[1]);
					oldpos0 = pos1;
					start = true;
				}
			}
		}

		g.setStroke(render.drawConstellationLimitsStroke);
		g.setColor(render.drawConstellationLimitsColor, true);
		if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
			g.draw(pathConLim);
		} else {
			g.draw(pathConLim, getDist(conlimDist));
		}
	}

	// Draw constellation names
	private void drawConstelNames() throws JPARSECException
	{
		if (!render.drawConstellationNames)
			return;

		ArrayList<Object> connom = null;
		Object o = null;
		if (db_connom >= 0) {
			o = DataBase.getData(db_connom);
		} else {
			o = DataBase.getData("connom", threadID, true);
		}
		if (o != null) connom = new ArrayList<Object>(Arrays.asList((Object[]) o));
		if (connom == null)  {
			String culture = "";
			if (render.drawConstellationContours != CONSTELLATION_CONTOUR.DEFAULT &&
					render.drawConstellationContours != CONSTELLATION_CONTOUR.NONE) culture = "_"+render.drawConstellationContours.name();
			ArrayList<String> connomFile = ReadFile.readResource(FileIO.DATA_SKY_DIRECTORY + "connom"+culture+".txt");
			connom = new ArrayList<Object>();
			for (int i = 0; i < connomFile.size(); i++)
			{
				String line = connomFile.get(i);
				double ra = Double.parseDouble(FileIO.getField(2, line, " ", true)) / Constant.RAD_TO_HOUR;
				double dec = Double.parseDouble(FileIO.getField(3, line, " ", true)) * Constant.DEG_TO_RAD;

				// Precession
				LocationElement loc = new LocationElement(ra, dec, 1.0);
				if (equinox != Constant.J2000)
					loc = LocationElement.parseRectangularCoordinates(precessFromJ2000(equinox,
							LocationElement.parseLocationElement(loc), projection.eph));
				if (projection.obs.getMotherBody() != TARGET.EARTH && projection.obs.getMotherBody() != TARGET.NOT_A_PLANET) {
					loc = projection.getPositionFromBody(loc, true);
				}
				loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, true, true, 0);
				if (loc != null) {
					String cons = "";
					if (culture.equals("")) {
						cons = line.substring(16, 20).trim();
						if (render.drawConstellationNamesType == Constellation.CONSTELLATION_NAME.SPANISH_WITH_ALTERNATE_ZODIAC)
							cons = line.substring(62, 84).trim();
						if (render.drawConstellationNamesType == Constellation.CONSTELLATION_NAME.SPANISH)
							cons = line.substring(40, 62).trim();
						if (render.drawConstellationNamesType == Constellation.CONSTELLATION_NAME.LATIN)
							cons = line.substring(20, 40).trim();
						if (render.drawConstellationNamesType == Constellation.CONSTELLATION_NAME.ENGLISH)
							cons = line.substring(84).trim();
					} else {
						cons = FileIO.getRestAfterField(4, line, " ", true);
						int sep = cons.indexOf("   ");
						if (render.drawConstellationNamesType == Constellation.CONSTELLATION_NAME.SPANISH_WITH_ALTERNATE_ZODIAC ||
								render.drawConstellationNamesType == Constellation.CONSTELLATION_NAME.SPANISH) {
							cons = cons.substring(sep).trim();
						} else {
							cons = cons.substring(0, sep).trim();
						}
					}
					if (render.drawConstellationNamesUppercase) {
						cons = cons.toUpperCase();
					} else {
						String nc = "";
						if (render.drawConstellationNamesType == Constellation.CONSTELLATION_NAME.ABREVIATED) {
							nc = cons;
						} else {
							int n = FileIO.getNumberOfFields(cons, " ", true);
							for (int j=0; j<n; j++) {
								String nf = FileIO.getField(j+1, cons, " ", true);
								if (render.drawConstellationNamesType == Constellation.CONSTELLATION_NAME.SPANISH ||
										render.drawConstellationNamesType == Constellation.CONSTELLATION_NAME.SPANISH_WITH_ALTERNATE_ZODIAC) {
									if (!nf.toLowerCase().equals("de") && !nf.toLowerCase().equals("del"))
										nf = nf.substring(0, 1).toUpperCase() + nf.substring(1).toLowerCase();
								} else {
									nf = nf.substring(0, 1).toUpperCase() + nf.substring(1).toLowerCase();
								}
								nc += nf + " ";
							}
						}
						cons = nc.trim();
					}
					connom.add(new Object[] {loc, cons});
				}
			}
			DataBase.addData("connom", threadID, connom.toArray(), true);
			db_connom = DataBase.getIndex("connom", threadID);
		}
		float pos0[] = new float[2];

		Graphics.FONT font = render.drawConstellationNamesFont;
		if (pixels_per_degree < 12 && render.drawClever) {
			if (pixels_per_degree < 8) {
				font = FONT.getDerivedFont(font, font.getSize()-4);
				if (pixels_per_degree < 6) font = FONT.getDerivedFont(font, font.getSize()-1);
			} else {
				font = FONT.getDerivedFont(font, font.getSize()-2);
			}
		}

		LocationElement loc;
		Object obj[];
		float pos = -font.getSize()*2;
		if (pixels_per_degree < 10) pos = pos / 2;
		if (render.drawFastLabels == SUPERIMPOSED_LABELS.FAST || pixels_per_degree < 10 && render.drawClever && render.drawFastLabelsInWideFields)
			pos = -1;
		for (Iterator<Object> itr = connom.iterator();itr.hasNext();)
		{
			obj = (Object[]) itr.next();
			loc = (LocationElement) obj[0];
			pos0 = projection.projectPosition(loc, 0, false);
			if (pos0 != null && !this.isInTheScreen((int)pos0[0], (int)pos0[1])) continue;

			if (!projection.isInvalid(pos0))
				drawString(render.drawConstellationNamesColor, font, (String) obj[1], pos0[0], pos0[1], pos, false);
		}
	}

	private Integer overlay(String list[], int index, boolean recovered, String file, Object img,
			double[] baryc, float pos00[], Object[] obj, int type, double sph) throws JPARSECException {
		if (img == null) return null;

		int imgMaxWidth = (int) (render.width);
		int imgMaxHeight = (int) (render.height);
		if (!g.renderingToAndroid()) {
			imgMaxWidth *= 3;
			imgMaxHeight *= 3;
		}

		/// sc = size in arcmin
		double sc = Double.parseDouble(FileIO.getField(5, list[index].trim(), UnixSpecialCharacter.UNIX_SPECIAL_CHARACTER.TAB.value, true));
		float size0 = (float) (sc * pixels_per_degree / 60.0);
		double rot = Double.parseDouble(FileIO.getField(6, list[index].trim(), UnixSpecialCharacter.UNIX_SPECIAL_CHARACTER.TAB.value, true));
		int sizei[] = g.getSize(img);
		float scale = (float) ((sc * pixels_per_degree / 60.0) / sizei[0]), ang = (float)(rot * Constant.DEG_TO_RAD);

		float radius_x = sizei[0] * scale * 0.5f;
		float radius_y = sizei[1] * scale * 0.5f;

		if (recovered) {
			double ds = Math.abs(radius_x*2-sizei[0]);
			if (file != null && ds > 3) { // size different by > 3 px => reload again
				img = g.getImage(file);
				g.waitUntilImagesAreRead(new Object[] {img});
				recovered = false;
				sizei = g.getSize(img);
				scale = (float) ((sc * pixels_per_degree / 60.0) / sizei[0]);
				radius_x = sizei[0] * scale * 0.5f;
				radius_y = sizei[1] * scale * 0.5f;
			} else {
				if (render.telescope.invertHorizontal || render.telescope.invertVertical) {
					if (render.telescope.invertHorizontal)
						ang = (float) (Math.PI - ang);
					if (render.telescope.invertVertical)
						ang = -ang;
				}
			}
		}

		if (sc <= 180 && (radius_x > imgMaxWidth || radius_y > imgMaxHeight) && 
				radius_x < render.width * 5 && radius_y < render.height * 5) {
			int w0 = imgMaxWidth, h0 = imgMaxHeight;
			int x0 = sizei[0]/2-w0/2, y0 = sizei[1]/2-h0/2;
			if (x0 < 0) {
				x0 = 0;
				w0 = sizei[0];
			}
			if (y0 < 0) {
				y0 = 0;
				h0 = sizei[1];
			}
			img = g.getImage(img, x0, y0, w0, h0);
			sizei = g.getSize(img);
			radius_x = sizei[0] * scale * 0.5f;
			radius_y = sizei[1] * scale * 0.5f;
		}
		
		double factor = 1;
		if (sc > 180) factor = 2; // Images covering large fields (>3\u00b0) should disappear faster
		int w = 1, h = 1;
		if (radius_x*factor <= imgMaxWidth || radius_y*factor <= imgMaxHeight) {
			if (!recovered) {
				int col = g.getColor();
				int nn = 20;
				if (render.planetRender.highQuality) {
					if (RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR < 1) nn = 10;
					if (RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR > 1) nn = 50;
					if (RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR > 1.5) nn = 80;
				}
				if (nn<80 && (render.getColorMode() == COLOR_MODE.WHITE_BACKGROUND ||
						render.getColorMode() == COLOR_MODE.PRINT_MODE ||
						render.getColorMode() == COLOR_MODE.WHITE_BACKGROUND_SIMPLE_GREEN_RED_OR_RED_CYAN_ANAGLYPH))
					nn = 80;
				g.setColor(nn, nn, nn, 255);
				if (file != null && !file.equals("SUN")) img = g.makeColorTransparent(img, g.getColor(), true, false, 0);
				g.setColor(col, true);

				if (render.telescope.invertHorizontal || render.telescope.invertVertical) {
					if (render.telescope.invertHorizontal) {
						w = -1;
						ang = (float) (Math.PI - ang);
					}
					if (render.telescope.invertVertical) {
						h = -1;
						ang = -ang;
					}
					//img = g.getScaledImage(img, sizei[0]*w, sizei[1]*h, false, false);
					//img = g.getRotatedAndScaledImage(img, 0, 0, 0, w, h);
				}
			}


			double ra = Double.parseDouble(FileIO.getField(2, list[index].trim(), UnixSpecialCharacter.UNIX_SPECIAL_CHARACTER.TAB.value, true)) * Constant.DEG_TO_RAD;
			double dec = Double.parseDouble(FileIO.getField(3, list[index].trim(), UnixSpecialCharacter.UNIX_SPECIAL_CHARACTER.TAB.value, true)) * Constant.DEG_TO_RAD;
			LocationElement loc = new LocationElement(ra, dec, 1.0);
			if (pos00 == null) {
					if (equinox != Constant.J2000) {
						// Correct for aberration, precession, and nutation
						if (projection.eph.ephemType == COORDINATES_TYPE.APPARENT) {
							loc.setRadius(1000 * Constant.RAD_TO_ARCSEC); // 1000 pc
							double light_time = loc.getRadius() * Constant.LIGHT_TIME_DAYS_PER_AU;
							if (baryc == null)
								baryc = Ephem.eclipticToEquatorial(PlanetEphem.getGeocentricPosition(equinox, TARGET.Solar_System_Barycenter, 0.0, false, projection.obs), Constant.J2000, projection.eph);

							double r[] = Ephem.aberration(loc.getRectangularCoordinates(), baryc, light_time);

							r = precessFromJ2000(equinox, r, projection.eph);
							loc = LocationElement.parseRectangularCoordinates(nutateInEquatorialCoordinates(equinox, projection.eph, r, true));
						} else {
							loc = LocationElement.parseRectangularCoordinates(precessFromJ2000(equinox,
									LocationElement.parseLocationElement(loc), projection.eph));
						}
					}

					if (loc == null) return null;
					if (projection.obs.getMotherBody() != TARGET.EARTH && projection.obs.getMotherBody() != TARGET.NOT_A_PLANET) {
						loc = projection.getPositionFromBody(loc, this.fast);
					}

				loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, true, false, (float) (sc * Constant.DEG_TO_RAD / 120.0));
				pos00 = projection.projectPosition(loc, 0, false);
			} else {
				// loc = apparent eq in this case
				loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, true, false, (float) (sc * Constant.DEG_TO_RAD / 120.0));
			}

			if (pos00 != null && !this.isInTheScreen((int)pos00[0], (int)pos00[1], (int)size0)) pos00 = null;
			if (projection.isInvalid(pos00)) return null;
			double ang2 = projection.getNorthAngleAt(loc, false, true) - Constant.PI_OVER_TWO;
			if (render.telescope.invertHorizontal || render.telescope.invertVertical) {
				if (render.telescope.invertHorizontal) {
					ang2 = (Math.PI - ang2);
				}
				if (render.telescope.invertVertical) {
					ang2 = -ang2;
				}
			}
			ang += ang2+sph;
			if (file != null && file.equals("SUN")) ang = 0;

			//ang -= Constant.PI_OVER_FOUR; //Constant.TWO_PI * ((jd - Constant.J2000) / 365.25) / 25800.0;
			if (!recovered || w == -1 || h == -1) {
				img = g.getScaledImage(img, w*(int)(2*radius_x+0.5), h*(int)(2*radius_y+0.5), true, false);
			}				
			if (!recovered) {
				img = g.getRotatedAndScaledImage(img, radius_x-0.5f, radius_y-0.5f, ang, 1, 1);
				if (render.getColorMode() == COLOR_MODE.NIGHT_MODE) {
					int arr[] = g.getImageAsPixels(img), s[] = g.getSize(img);
					for (int i=0; i<arr.length; i++) {
						int r = (arr[i]>>16)&255;
						arr[i] = 255<<24 | r<<16;
					}
					img = g.getImage(s[0], s[1], arr);
				}
			}
			if (file != null && !file.equals("SUN") && !recovered) {
				g.addToDataBase(img, file+render.getColorMode().name(), 5);
			}
			
			if (render.drawSkyCorrectingLocalHorizon && projection.obs.getMotherBody() == TARGET.EARTH && projection.eph.isTopocentric
					&& (projection.eph.targetBody != TARGET.SUN)) {
				LocationElement locEq = projection.toEquatorialPosition(loc, true);
				LocationElement locH = projection.getApparentLocationInSelectedCoordinateSystem(locEq, false, true, COORDINATE_SYSTEM.HORIZONTAL, (float) (sc * Constant.DEG_TO_RAD / 120.0));
				double elev = locH.getLatitude(), angrad = Constant.DEG_TO_RAD * sc / 120.0;
				double geoElev0 = Ephem.getGeometricElevation(projection.eph, projection.obs, elev);
				double geoElevp = geoElev0 + angrad;
				double appElevp = Ephem.getApparentElevation(projection.eph, projection.obs, geoElevp, 10);
				double dp = (appElevp - elev);
				float upperLimbFactor = (float) (dp / angrad);
				int de = (int) (radius_x * (1.0 - upperLimbFactor) + 0.5);
				if (de > 1) {
					float angr = (float) (projection.getCenitAngleAt(locEq, true) - Constant.PI_OVER_TWO);
					img = g.getRotatedAndScaledImage(img, radius_x-0.5f, radius_y-0.5f, angr, 1.0f, 1.0f);
					int newH = (int)(2*radius_y*upperLimbFactor+0.5);
					int displ = ((int) (2*radius_y) - newH) / 2;
					img = g.getScaledImage(img, (int)(2*radius_x+0.5), newH, false, false, displ);
					img = g.getRotatedAndScaledImage(img, radius_x-0.5f, radius_y-0.5f, -angr, 1.0f, 1.0f);

					g.drawImage(img, pos00[0]-radius_x, pos00[1]-newH/2-displ);
				} else {
					g.drawImage(img, pos00[0]-radius_x, pos00[1]-radius_y);
				}
			} else {
				g.drawImage(img, pos00[0]-radius_x, pos00[1]-radius_y);
			}
			type = -Math.abs(type);
			//if (obj != null) loc = ((LocationElement) obj[3]);
		}
		return type;
	}

	private Object[] getSDSSoldImage(LocationElement loc) throws Exception {
		String f[] = FileIO.getFiles(FileIO.getTemporalDirectory());
		ArrayList<Object[]> out = new ArrayList<Object[]>();
		for (int i=0; i<f.length; i++) {
			String s = FileIO.getFileNameFromPath(f[i]);
			if (s.startsWith("sdss_") && s.endsWith(".jpg")) {
				String s2 = s.substring(5);
				s2 = s2.substring(0, s2.lastIndexOf("."));
				double lon = Double.parseDouble(FileIO.getField(1, s2, "_", true));
				double lat = Double.parseDouble(FileIO.getField(2, s2, "_", true));
				LocationElement loc2 = new LocationElement(lon, lat, 1.0);
				double dist = LocationElement.getApproximateAngularDistance(loc, loc2) * Constant.RAD_TO_DEG;
				if (dist < 1.75) {
					out.add(new Object[] {g.getImage(new File(f[i]).toURI().toURL().toString()), 
							loc2,
							Double.parseDouble(FileIO.getField(3, s2, "_", true))
						});
				}
			}
		}
		if (out.size() == 0) return new Object[] {new Object[] {null, loc, 2.0}};
		return out.toArray();
	}

	private static final String types[] = new String[] {"unk", "gal", "neb", "pneb", "ocl", "gcl", "galpart", "qua", "duplicate", "duplicateInNGC", "star/s", "notFound"};
	//private static final String types2[] = new String[] {"unknown", "galaxy", "nebula", "planetary nebula", "open cluster", "globular cluster", "region of galaxy", "quasar", "duplicate", "duplicate in NGC", "star/s", "not found"};
	private static final int types2Int[] = new int[] {819, 40, 959, 960, 1297, 961, 953, 954, 955, 956, 957, 958};
//	private static final String types2Spa[] = new String[] {"desconocido", "galaxia", "nebulosa", "nebulosa planetaria", "c\u00famulo abierto", "c\u00famulo globular", "regi\u00f3n de una galaxia", "quasar", "duplicado", "duplicado en el NGC", "estrella/s", "no encontrado"};
	private static final String nodraw2[] = new String[] {"LMC", "292", "Mel22", "7000", "I.5067", "6533", "6523"};
	private float lastObjMaglim = -1, maglimNotDrag = -1, maglimStarsNotDrag = -1;
	private static ArrayList<String> imagesNotFound = new ArrayList<String>();
	private void drawDeepSkyObjects() throws JPARSECException
	{
		if (!render.drawDeepSkyObjects || render.drawClever && pixels_per_degree < 5.0)
			return;

		float pos0[] = null;
		g.setColor(render.drawDeepSkyObjectsColor, true);

		boolean labels = render.drawDeepSkyObjectsLabels;
		if (pixels_per_degree < 10.0 && render.drawClever) labels = false;

		boolean SN1054 = false;
		if (jd < 2106216) SN1054 = true;

		double objMagLim = Math.abs(render.drawObjectsLimitingMagnitude);
		if (render.drawFastLinesMode.fastGrid()) {
			if (maglimNotDrag == -1) {
				if (maglim < render.drawStarsLimitingMagnitude && objMagLim > maglim && render.drawClever) objMagLim = maglim;
			} else {
				if (maglimStarsNotDrag < maglimNotDrag && objMagLim > maglimStarsNotDrag && render.drawClever) objMagLim = maglimStarsNotDrag;
			}
		} else {
			if (maglim < render.drawStarsLimitingMagnitude && objMagLim > maglim && render.drawClever) objMagLim = maglim;
			maglimNotDrag = render.drawStarsLimitingMagnitude;
			maglimStarsNotDrag = maglim;
		}
		if (fieldDeg > 15 && objMagLim-maglim > 2 && render.drawClever && g.renderingToAndroid()) objMagLim = maglim + 2;
		LocationElement loc;
		LocationElement locF;
		Object obj[];
		double mag;
		float size_xy[];
		String messier, name, comments, name2print;
		float size, size0, size2, displacement, pa;
		int pp;
		float distGal = getDist(-refz/(render.anaglyphMode.isDubois() ? 8.0:1.0)), distCl = getDist(0);

		ArrayList<Object> minorObjects = null;
		if (SaveObjectsToAllowSearch) {
			minorObjects = new ArrayList<Object>();
			Object o = null;
			if (db_minorObjects >= 0) {
				o = DataBase.getData(db_minorObjects);
			} else {
				o = DataBase.getData("minorObjects", threadID, true);
			}
			if (o != null) minorObjects = new ArrayList<Object>(Arrays.asList(((Object[]) o)));
		}
		boolean external = false;
		FONT mainFont = render.drawDeepSkyObjectsNamesFont;
		FONT secondaryFont = FONT.getDerivedFont(mainFont, mainFont.getSize()-2);
		FONT font = mainFont;
		g.setStroke(render.drawDeepSkyObjectsStroke);
		boolean fast = this.fast;
		if (render.drawDeepSkyObjectsStroke.getLineWidth() != 1f) fast = false;

		// See if overlaided images should be rotated due to precession
		double sph = 0;
		boolean rotateOverlay = false;
		LocationElement locPolar0 = null, locPolar = null;
		if (Math.abs(projection.eph.getEpoch(jd)-Constant.J2000) > 36525) {
			rotateOverlay = true;
			locPolar0 = new LocationElement(0, Constant.PI_OVER_TWO, 1.0);
			locPolar = LocationElement.parseRectangularCoordinates(precessFromJ2000(projection.eph.getEpoch(jd),
					locPolar0.getRectangularCoordinates(), projection.eph));
		}

		if (render.overlayDSSimageInNextRendering && fieldDeg < 5) {
	 		// General properties for the queries
			SKYVIEW_COORDINATE scoord = SKYVIEW_COORDINATE.EQUATORIAL_J2000;
			SKYVIEW_PROJECTION sproj = SKYVIEW_PROJECTION.ZENITHAL_EQUAL_AREA;
			SKYVIEW_LUT_TABLE slut = SKYVIEW_LUT_TABLE.GRAY;
			SKYVIEW_INTENSITY_SCALE sscale = SKYVIEW_INTENSITY_SCALE.LINEAR;
			boolean invertLevels = false, grid = false;
			String catalog = null, contours = null;

			int width = 800;
			double fieldDSS = 2; // deg
			loc = getEquatorialPositionOfRendering();

			if (rotateOverlay)
				sph = LocationElement.solveSphericalTriangle(loc, locPolar0, locPolar, true);
			loc = LocationElement.parseRectangularCoordinates(precessToJ2000(jd, loc.getRectangularCoordinates(), projection.eph));

			 if (g.renderingToAndroid()) {

				try {
					Object oo[] = getSDSSoldImage(loc);
					if (oo != null) {
						for (int i=0; i<oo.length; i++) {
							Object o[] = (Object[]) oo[i];
							Object img = o[0];
							loc = (LocationElement) o[1];
							fieldDSS = (Double) o[2];
		
							String source = ""+(loc.getLongitude()*Constant.RAD_TO_DEG)+" "+(loc.getLatitude()*Constant.RAD_TO_DEG);
							String l = "DSS "+source+" 0 "+(fieldDSS*60.0)+" 0 DSS DSS DSS";
							l = DataSet.replaceAll(l, " ", "\t", true);
							if (img == null && fieldDeg < 3) {
							 	width = 300;
								SKYVIEW_SURVEY survey = SKYVIEW_SURVEY.DSSOLD;
								String query = GeneralQuery.getQueryToSkyView(source, survey, fieldDSS, width, invertLevels, grid, scoord, sproj, slut, sscale, catalog, contours);
								img = g.getImage(query);
								if (img != null) {
									String p = FileIO.getTemporalDirectory() + "sdss_"+(float)loc.getLongitude()+"_"+(float)loc.getLatitude()+"_"+(float)fieldDSS+".jpg";
									WriteFile.writeImage(p, img);
								}
							}
							overlay(new String[] {l}, 0, false, null, img, baryc,
									null, null, 1, sph);
						}
					}
				} catch (Exception exc) {}
			 } else {
				try {
					Object oo[] = getSDSSoldImage(loc);
					if (oo != null) {
						for (int i=0; i<oo.length; i++) {
							Object o[] = (Object[]) oo[i];
							Object imgR = o[0];
							loc = (LocationElement) o[1];
							fieldDSS = (Double) o[2];
			/*				if (lastDSSSource != null) {
								LocationElement lastDSS = new LocationElement(
										Double.parseDouble(FileIO.getField(1, lastDSSSource, " ", true)) * Constant.DEG_TO_RAD,
										Double.parseDouble(FileIO.getField(2, lastDSSSource, " ", true)) * Constant.DEG_TO_RAD, 1.0
										);
								double dist = LocationElement.getAngularDistance(lastDSS, locEQ);
								if (dist < 5 * Constant.DEG_TO_RAD) {
									imgR = g.getFromDataBase(lastDSSSource);
									fieldDSS = Double.parseDouble(FileIO.getField(3, lastDSSSource, " ", true));
									source = ""+(lastDSS.getLongitude()*Constant.RAD_TO_DEG)+" "+(lastDSS.getLatitude()*Constant.RAD_TO_DEG);
									l = "DSS "+source+" 0 "+(fieldDSS*60.0)+" 0 DSS DSS DSS";
									l = DataSet.replaceAll(l, " ", "\t", true);
								} else {
									DataBase.addData(lastDSSSource, null, true);
								}
							}
			*/
							String source = ""+(loc.getLongitude()*Constant.RAD_TO_DEG)+" "+(loc.getLatitude()*Constant.RAD_TO_DEG);
							String l = "DSS "+source+" 0 "+(fieldDSS*60.0)+" 0 DSS DSS DSS";
							l = DataSet.replaceAll(l, " ", "\t", true);
			
							if (imgR == null && fieldDeg < 3) {
								// Create the string queries
								SKYVIEW_SURVEY surveyR = SKYVIEW_SURVEY.DSS2R;
								SKYVIEW_SURVEY surveyG = SKYVIEW_SURVEY.DSSOLD;
								SKYVIEW_SURVEY surveyB = SKYVIEW_SURVEY.DSS2B;
								String queryR = GeneralQuery.getQueryToSkyView(source, surveyR, fieldDSS, width, invertLevels, grid, scoord, sproj, slut, sscale, catalog, contours);
								String queryG = GeneralQuery.getQueryToSkyView(source, surveyG, fieldDSS, width, invertLevels, grid, scoord, sproj, slut, sscale, catalog, contours);
								String queryB = GeneralQuery.getQueryToSkyView(source, surveyB, fieldDSS, width, invertLevels, grid, scoord, sproj, slut, sscale, catalog, contours);
			
								// getImage must support web queries
								imgR = g.getImage(queryR);
								Object imgG = g.getImage(queryG);
								Object imgB = g.getImage(queryB);
								int imgSize[] = g.getSize(imgR);
			
								// COMBINE RGB
								g.disableInversion();
								for (int x=0; x<imgSize[0]; x++) {
									for (int y=0; y<imgSize[1]; y++) {
										g.setRGB(imgR, x, y, Functions.getColor(g.getRed(g.getRGB(imgR, x, y)), g.getGreen(g.getRGB(imgG, x, y)), g.getBlue(g.getRGB(imgB, x, y)), 255));
									}
								}
								g.enableInversion();
			
								String p = FileIO.getTemporalDirectory() + "sdss_"+(float)loc.getLongitude()+"_"+(float)loc.getLatitude()+"_"+(float)fieldDSS+".jpg";
								WriteFile.writeImage(p, imgR);
								//lastDSSSource = source+" "+fieldDSS+" "+render.getColorMode().name();
								//g.addToDataBase(imgR, lastDSSSource, 0);
							}
			
							overlay(new String[] {l}, 0, false, null, imgR, baryc,
									null, null, 1, sph);
						}
					}
				} catch (Exception exc) {}
			}
		}

		ArrayList<Object> objects = readObjects(projection, false);
		ArrayList<Object> objectsJ2000 = null;
		Object o = DataBase.getData("objectsJ2000", null, true);
		if (o == null) o = populate(false);
		objectsJ2000 = new ArrayList<Object>(Arrays.asList((Object[]) o));

		float fieldLimit = g.renderingToAndroid() ? 40:60;
		float minX = rec.getMinX(), minY = rec.getMinY(), maxX = rec.getMaxX(), maxY = rec.getMaxY();
		int m = objects.size();
		for (int s=0; s<m; s++)
		{
			font = mainFont;
			obj = (Object[]) objects.get(s);
			if (obj.length <= 3) {
				locF = ((LocationElement) obj[1]);
				if (obj.length == 3) {
					mag = (Float) obj[2];
					obj = (Object[]) objectsJ2000.get((Integer) obj[0]);
				} else {
					obj = (Object[]) objectsJ2000.get((Integer) obj[0]);
					mag = (Float) obj[4];
				}
			} else {
				mag = (Float) obj[4];
				locF = ((LocationElement) obj[3]);
			}

			messier = (String) obj[1];
			if (mag > objMagLim && (render.drawFastLinesMode.fastGrid() || maglim != render.drawStarsLimitingMagnitude)) {
				if (render.drawDeepSkyObjectsAllMessierAndCaldwell && !external && fieldDeg < fieldLimit) {
					if (messier.isEmpty()) continue;
				} else {
					if (render.getNumberOfEnabledExternalCatalogs() == 0) break;
					continue;
				}
			}

			if (obj.length == 9) {
				if (render.getNumberOfEnabledExternalCatalogs() == 0) break;
				external = true;
				if (render.getColorMode() == COLOR_MODE.NIGHT_MODE) {
					g.setColor(render.drawDeepSkyObjectsColor, true);
				} else {
					g.setColor((Integer)obj[8], true);
				}
			} else {
				if (mag == 100 && render.drawClever && (fieldDeg > fieldLimit*0.5 || maglim <= 8)) continue;
			}

			int type = (Byte) obj[2];
			if (render.drawFaintStars && render.drawStarsLimitingMagnitude > 12 && (type == 4 || type == 5)
					&& faintStars != null && fieldDeg < 3.0 && mag < render.drawStarsLimitingMagnitude - 3) { // mag components around cluster mag + 3
				// Draw labels for open/globular clusters but not the object itself, when the cluster is partially
				// resolved in stars
				size_xy = (float[]) obj[5];
				if (size_xy[0] > 0.1) type = -type; // size greater than 0.1 deg = 6'
			} else {
				if (!render.drawNebulaeContours) {
					type = Math.abs(type);
					if (render.drawMilkyWayContours && render.drawMilkyWayContoursWithTextures == MILKY_WAY_TEXTURE.OPTICAL &&
							render.getColorMode() == COLOR_MODE.BLACK_BACKGROUND) {
						int nodrawIndex = DataSet.getIndex(nodraw2, (String) obj[0]);
						if (nodrawIndex >= 0) type = -type;
					}
				}
			}

			if (((String) obj[0]).equals("I.434") || Math.abs(type) == 100 || (Math.abs(type) <= 7 && (mag <= objMagLim || (!messier.isEmpty() && render.drawDeepSkyObjectsAllMessierAndCaldwell) || (render.drawClever && (mag == 100.0 || Math.abs(type) == 7) && ((render.drawObjectsLimitingMagnitude < 0 || maglim == render.drawStarsLimitingMagnitude) && fieldDeg < 30) && this.fieldDeg < 60))))
			{
				if ((!render.drawDeepSkyObjectsOnlyMessier || (render.drawDeepSkyObjectsOnlyMessier && !messier
						.isEmpty())) && (!SN1054 || (SN1054 && !messier.equals("M1"))))
				{
					//loc = ((LocationElement) obj[3]);

					if (rotateOverlay) {
						LocationElement locEq = projection.toEquatorialPosition(locF, true);
						sph = LocationElement.solveSphericalTriangle(locEq, locPolar0, locPolar, true);
					}

					// Correct apparent magnitude for extinction
					if (projection.eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT && projection.eph.correctForExtinction &&
							projection.obs.getMotherBody() == TARGET.EARTH && projection.eph.isTopocentric) {
						LocationElement locEq = projection.toEquatorialPosition(locF, true);
						double angh = lst - locEq.getLongitude();
						double h = FastMath.sin(projection.obs.getLatitudeRad()) * FastMath.sin(locEq.getLatitude()) + FastMath.cos(projection.obs.getLatitudeRad()) * FastMath.cos(locEq.getLatitude()) * FastMath
								.cos(angh);
						double alt = FastMath.asin(h);
						mag += Star.getExtinction(Constant.PI_OVER_TWO-alt, projection.obs.getHeight() / 1000.0, 5);
					}
					if (mag > objMagLim && maglim != render.drawStarsLimitingMagnitude && (messier.isEmpty() || !render.drawDeepSkyObjectsAllMessierAndCaldwell)) continue;

					size_xy = (float[]) obj[5];
					size0 = (size_xy[0] * pixels_per_degree) + 1;
					pos0 = projection.projectPosition(locF, 0, false);
					if (pos0 != null && !this.isInTheScreen(pos0[0], pos0[1], (int) (size0 * (Math.abs(type) == 3? 2:1)), minX, minY, maxX, maxY)) pos0 = null;

					if (!projection.isInvalid(pos0))
					{
						name = (String) obj[0];
						comments = (String) obj[7];
						size = Math.max(size0, g.renderingToAndroid() ? 6 : 3);
						size = Math.max(size, render.drawDeepSkyObjectSymbolMinimumSize);
						if (projection.isCylindricalForced() && (fieldDeg < 60 || hugeFactor >= 1) && !external && size > 15 && render.drawDeepSkyObjectsTextures && !imagesNotFound.contains(name)
								&& locF.getLatitude() < 80*Constant.DEG_TO_RAD) {
							String file = name.toLowerCase() + ".jpg";
							if (file.indexOf("caldwell")>=0) {
								file = searchDeepSkyObjectReturnMainName(file);
								if (file != null) {
									file = file.toLowerCase() + ".jpg";
								} else {
									file = "";
								}
							}
							if (DataSet.isDoubleStrictCheck(name) || (name.length() > 4 && DataSet.isDoubleStrictCheck(name.substring(0, 4))))
								file = "ngc "+name+".jpg";
							if (name.startsWith("I.")) file = "ic "+name.substring(2).toLowerCase()+".jpg";
							if (name.startsWith("QSO")) file = name.toLowerCase()+".png";
							if (!messier.isEmpty() && !messier.startsWith("C")) file = messier.toLowerCase()+".jpg";
							//if (file.equals("ic 434.jpg")) file = "ic434.png";
							//if (file.equals("ngc 1499.jpg")) file = "ngc 1499.png";
							if (file.equals("m27.jpg")) file = "m27.png";
							if (file.equals("m57.jpg")) file = "m57.png";
							if (file.equals("m42.jpg")) file = "m42.png";
							//if (file.equals("m37.jpg")) file = "m37.png";
							String file0 = file;
							if (	(file0.equals("m43.jpg") && fieldDeg > 1)
									//|| (file0.equals("ngc 2244.jpg") && fieldDeg > 0.44)
									|| ((file0.equals("m110.jpg") || file0.equals("m32.jpg")) && fieldDeg > 3)
									|| (file0.equals("ngc 5195.jpg") && fieldDeg > 0.0)
//									|| (Math.abs(jd-Constant.J2000) > 365250 && (Math.abs(type) == 2 || Math.abs(type) == 4))
								    // In epochs far from J2000 stars in clusters/nebula will appear moved due to proper motions
									) {
								type = -Math.abs(type); // draw only the label
							} else {
								file = FileIO.DATA_TEXTURES_DIRECTORY + "deepsky/"+file;
								Object img = null;
								boolean recovered = false;
								if (!g.renderingToAndroid()) { // Fast recover of texture without processing it again
									img = g.getFromDataBase(file+render.getColorMode().name());
									if (img != null) {
										recovered = true;
									} else {
										img = g.getImage(file);
										g.waitUntilImagesAreRead(new Object[] {img});
									}
								} else {
									img = g.getImage(file);
									g.waitUntilImagesAreRead(new Object[] {img});
								}
								if (img != null) {
									o = null;
									if (db_deepSkyTextures >= 0) {
										o = DataBase.getData(db_deepSkyTextures);
									} else {
										o = DataBase.getData("deepSkyTextures", threadID, true);
									}
									String list[] = null;
									if (o == null) {
										String file2 = FileIO.DATA_TEXTURES_DIRECTORY + "deepsky/nebula_textures.fab";
										list = DataSet.arrayListToStringArray(ReadFile.readResource(file2));
										DataBase.addData("deepSkyTextures", threadID, list, true);
										db_deepSkyTextures = DataBase.getIndex("deepSkyTextures", threadID);
									} else {
										list = (String[])o;
									}
									int index = DataSet.getIndexContaining(list, file0);
									if (index >= 0 && !list[index].startsWith("#")) {
										Integer result = overlay(list, index, recovered, file, img, baryc, null, obj, type, sph);
										if (result == null) continue;
										type = result;
									}
								} else {
									if (!imagesNotFound.contains(name)) imagesNotFound.add(name);
								}
							}
						}

						switch (type) {
						case 1: // gal
							// Draw galaxies taking into account PA and sizes
							pa = (Float) obj[6];
							size2 = Math.max((int) (size_xy[1] * pixels_per_degree) + 1, 1);
							if (size > 3 && size2 != size && pa != -1f)
							{
								double add = projection.getNorthAngleAt(locF, false, true);
								double ang0 = -pa + add + sph;
								int jmax = 20;
								if (size > 27.5) jmax = (int)(4*Math.sqrt(size));
								Object path = g.generalPathInitialize();
								double ang, px0, py0, r, px, py;
								int j;
								for (j = 0; j < jmax; j++)
								{
									ang = j * (Constant.TWO_PI / jmax);
									px0 = (size * FastMath.cos(ang));
									py0 = (size2 * FastMath.sin(ang));

									r = FastMath.hypot(px0, py0);
									ang = FastMath.atan2(py0, px0);
									px = (r * FastMath.cos(ang + ang0));
									py = (r * FastMath.sin(ang + ang0));

									if (j > 0) {
										g.generalPathLineTo(path, (float)(pos0[0] + px), (float)(pos0[1] + py));
									} else {
										g.generalPathMoveTo(path, (float)(pos0[0] + px), (float)(pos0[1] + py));
										g.generalPathLineTo(path, (float)(pos0[0] + px), (float)(pos0[1] + py + 0.1));
									}
								}
								g.generalPathClosePath(path);
								if (render.fillGalaxyColor != -1) {
									if (render.drawStarsRealistic == REALISTIC_STARS.NONE_CUTE) {
										g.setColor(render.background, true);
										g.fill(path, distGal);
										g.setColor(render.fillGalaxyColor, true);
									} else {
										if (!external) g.setColor(render.fillGalaxyColor, true);
									}
									g.fill(path, distGal);
									g.setColor(render.drawDeepSkyObjectsColor, true);
								}
								g.setStroke(new JPARSECStroke(render.drawDeepSkyObjectsStroke, render.drawDeepSkyObjectsStroke.getLineWidth()+0.25f));
								if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
									g.draw(path);
								} else {
									g.draw(path, distGal);
								}
								g.setStroke(render.drawDeepSkyObjectsStroke);
							} else {
								if (render.fillGalaxyColor != -1) {
									if (pa == -1 || render.drawStarsRealistic == REALISTIC_STARS.NONE_CUTE) {
										g.setColor(render.background, true);
										g.fillOval(pos0[0] - size+1, pos0[1] - size+1, 2*size-1, 2*size-1, this.fast);
									}
								}
								drawGalaxy(pos0, size, distGal, external, g, render.drawDeepSkyObjectsColor, render.fillGalaxyColor);
								g.setColor(render.drawDeepSkyObjectsColor, true);
							}
							break;
						case 4: // ocl
						case 10: // star
							drawOpenCluster(pos0, size, distCl, g);
							break;
						case 3: // pneb
							drawpneb(pos0, size, distCl, g, fast);
							break;
						case 2: // neb
							if (render.drawNebulaeContours && (size_xy[0] > 0.17 || size_xy[1] > 0.17)) break; // draw labels, but not the spot, since most of them have contours
							g.setColor(render.drawBrightNebulaeContoursColor, true);
						case 6: // galpart
						case 7: // quasar
						//case 8: // duplicate, we don't want them
						//case 9: // duplicate NGC
						case 100:
							if (type == 100 || size0 == 1)
								size0 = getSizeForAGivenMagnitude(mag);
							float size02 = (2 * size0+1);
							if (!g.renderingToExternalGraphics()) {
								size0 = (int) size0;
								size02 = (int) size02;
								pos0[0] = (int) pos0[0];
								pos0[1] = (int) pos0[1];
							}
							if (!external) {
								if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
									g.fillOval((pos0[0] - size0), (pos0[1] - size0), size02, size02, this.fast);
								} else {
									g.fillOval((pos0[0] - size0), (pos0[1] - size0), size02, size02, distGal);
								}
							} else {
								pa = (Float) obj[6];
								size2 = Math.max((int) (size_xy[1] * pixels_per_degree) + 1, 1);
								if (size > 3 && size2 != size && pa != -1f)
								{
									double add = projection.getNorthAngleAt(locF, false, true);
									double ang0 = -pa + add + sph;
									int jmax = 20;
									if (size > 27.5) jmax = (int)(4*Math.sqrt(size));
									Object path = g.generalPathInitialize();
									double ang, px0, py0, r, px, py;
									int j;
									for (j = 0; j < jmax; j++)
									{
										ang = j * (Constant.TWO_PI / jmax);
										px0 = (size * FastMath.cos(ang));
										py0 = (size2 * FastMath.sin(ang));

										r = FastMath.hypot(px0, py0);
										ang = FastMath.atan2(py0, px0);
										px = (r * FastMath.cos(ang + ang0));
										py = (r * FastMath.sin(ang + ang0));

										if (j > 0) {
											g.generalPathLineTo(path, (float)(pos0[0] + px), (float)(pos0[1] + py));
										} else {
											g.generalPathMoveTo(path, (float)(pos0[0] + px), (float)(pos0[1] + py));
										}
									}
									g.generalPathClosePath(path);
									if (render.drawStarsRealistic == REALISTIC_STARS.NONE_CUTE)
										g.setColor(render.drawDeepSkyObjectsColor, true);
									g.fill(path, distGal);
									g.setColor(render.drawDeepSkyObjectsColor, true);
									g.setStroke(new JPARSECStroke(render.drawDeepSkyObjectsStroke, render.drawDeepSkyObjectsStroke.getLineWidth()+0.25f));
									if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
										g.draw(path);
									} else {
										g.draw(path, distGal);
									}
									g.setStroke(render.drawDeepSkyObjectsStroke);
								} else {
									drawGalaxy(pos0, size, distGal, false, g, render.drawDeepSkyObjectsColor, g.getColor());
								}
							}
							g.setColor(render.drawDeepSkyObjectsColor, true);
								break;
						case 5: // gcl
							drawGlobularCluster(pos0, size, distCl, g, fast);
							break;
						}

						name2print = name;
						type = Math.abs(type);
						if (labels || SaveObjectsToAllowSearch)
						{
							if (render.drawDeepSkyObjectsOnlyMessier && !external)
							{
								name2print = messier;
							} else
							{
								if (!external && !messier.isEmpty()) {
									name2print = name2print+" "+messier;
									if (pixels_per_degree <= 30 || name2print.startsWith("CALDWELL")) { // && render.drawClever) {
										name2print = messier;
										if (external) name2print = name;
									}
								}
							}

							if (labels) {
								if (Math.abs(type) == 3) size *= 2f;
								if (size/pixels_per_degree > 3) size = pixels_per_degree;
								if (type == 6 || mag < objMagLim-1) {
									if (type == 6) font = secondaryFont;
									displacement = -size - font.getSize();
								} else {
									displacement = size + font.getSize();
								}

								drawString(render.drawDeepSkyObjectsColor, font, DataSet.replaceAll(name2print, "CALDWELL ", "C", true), pos0[0], pos0[1],  displacement, false);
								if (pixels_per_degree > 30) { // && render.drawClever) {
									pp = comments.indexOf("Popular name:");
									if (pp>=0) {
										String pn = comments.substring(pp+14).trim();
										if (render.drawFastLabels == SUPERIMPOSED_LABELS.FAST || pixels_per_degree < 5 && render.drawClever && render.drawFastLabelsInWideFields) {
											drawString(render.drawDeepSkyObjectsColor, font, pn, pos0[0], pos0[1],  displacement+FastMath.sign(displacement)*font.getSize(), false);											
										} else {
											drawString(render.drawDeepSkyObjectsColor, font, pn, pos0[0], pos0[1],  displacement, false);
										}
										name2print += " - "+pn;
									}
								}
							}
						}

						if (SaveObjectsToAllowSearch) {
							String objType = messier;
							if (type <= 7) objType = ""+type; //objt[type];
							if (external && comments != null && !comments.equals("")) comments = "Type: " + comments;
							minorObjects.add(new Object[] {
									RenderSky.OBJECT.DEEPSKY, pos0.clone(), locF, (float) Math.abs(mag), name2print, 
									new Object[] {objType, size_xy, comments}
							});
						}
					}
				}
			}
		}
		if (SaveObjectsToAllowSearch) {
			DataBase.addData("minorObjects", threadID, minorObjects.toArray(), true);
			if (db_minorObjects < 0) db_minorObjects = DataBase.getIndex("minorObjects", threadID);
		}
	}

	/**
	 * Returns the identifier (thread name) for the sky data contained in this instance.
	 * @return Thread identifier.
	 */
	public String getDataBaseThreadID() {
		return threadID;
	}

//    Object ovals[] = null; // Also used more below
	private void drawOval(Graphics g, float px, float py, float w, float h) {
		// (Maybe) Slightly faster using images, but need transparency and produces less quality output
/*		if (render.drawFastLinesMode.fastOvals()) {
			float index = (w - 7)/2;
			if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH && (index == 0 || index == 1 || index == 2 || index == 3 || index == 4)) {
				if (ovals == null) {
					ovals = new Object[5];
					for (int i=0; i<ovals.length; i++) {
						int size = 2*i+7;
						Graphics gg = g.getGraphics(size, size);
						gg.setColor(render.background, true);
						gg.fillRect(0, 0, size, size);
						gg.setColor(render.drawDeepSkyObjectsColor, true);
						gg.drawOval(0, 0, size, size);
						ovals[i] = gg.getRendering();
						ovals[i] = gg.getRotatedAndScaledImage(ovals[i], 0, 0, 0, 1.0f, 1.0f); // Trick to pass to transparent
						ovals[i] = gg.makeColorTransparent(ovals[i], render.background, false, false, 0);
					}
				}
				g.drawImage(ovals[(int)index], px, py);
				return;
			}
		}
*/
		if (render.drawFastLinesMode.fastOvals()) g.setColor(g.getColor(), 255);
		g.drawOval(px, py, w, h, render.drawFastLinesMode.fastOvals());
	}
	private void drawpneb(float pos0[], float size, float distCl, Graphics g, boolean fast) {
		if (!g.renderingToExternalGraphics()) {
			pos0[0] = (int)pos0[0];
			pos0[1] = (int)pos0[1];
		}
		size = (int) (size);
		//fast = false;
		float size2 = 2*size+1;
		if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
			drawOval(g, pos0[0] - size, pos0[1] - size, size2, size2);
			if (fast) {
				g.drawStraightLine(pos0[0] - size, pos0[1], pos0[0] - size2, pos0[1]);
				g.drawStraightLine(pos0[0] + size, pos0[1], pos0[0] + size2, pos0[1]);
				g.drawStraightLine(pos0[0], pos0[1] - size, pos0[0], pos0[1] - size2);
				g.drawStraightLine(pos0[0], pos0[1] + size, pos0[0], pos0[1] + size2);
			} else {
				g.drawLine(pos0[0] - size, pos0[1], pos0[0] - size2, pos0[1], false);
				g.drawLine(pos0[0] + size, pos0[1], pos0[0] + size2, pos0[1], false);
				g.drawLine(pos0[0], pos0[1] - size, pos0[0], pos0[1] - size2, false);
				g.drawLine(pos0[0], pos0[1] + size, pos0[0], pos0[1] + size2, false);
			}
		} else {
			g.drawOval(pos0[0] - size, pos0[1] - size, size2, size2, distCl);
			g.drawLine(pos0[0] - size, pos0[1], pos0[0] - size2, pos0[1], distCl, distCl);
			g.drawLine(pos0[0] + size, pos0[1], pos0[0] + size2, pos0[1], distCl, distCl);
			g.drawLine(pos0[0], pos0[1] - size, pos0[0], pos0[1] - size2, distCl, distCl);
			g.drawLine(pos0[0], pos0[1] + size, pos0[0], pos0[1] + size2, distCl, distCl);
		}
/*		Object path = g.generalPathInitialize();
		g.generalPathMoveTo(path, pos0[0] - size, pos0[1]);
		g.generalPathLineTo(path, pos0[0], pos0[1] - size);
		g.generalPathLineTo(path, pos0[0]+size, pos0[1]);
		g.generalPathLineTo(path, pos0[0], pos0[1]+size);
		g.generalPathLineTo(path, pos0[0]-size, pos0[1]);
		g.fill(path, distCl);
*/
	}
	private void drawGalaxy(float pos0[], float size, float dist, boolean external, Graphics g,
			int color, int fillColor) {
		if (!g.renderingToExternalGraphics()) {
			pos0[0] = (int)pos0[0];
			pos0[1] = (int)pos0[1];
		}
		size = (int)size;
		float size2 = 2*size+1;
		if (!external) g.setColor(color, true);
		if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
			if (fillColor != -1) {
				if (!external) g.setColor(fillColor, true);
				g.fillOval(pos0[0] - size, pos0[1] - size, size2, size2, render.drawFastLinesMode.fastOvals() && external);
				if (!external) g.setColor(color, true);
			}
			if (fillColor != color && !external) {
				if (render.drawFastLinesMode.fastOvals()) {
					this.drawOval(g, pos0[0] - size, pos0[1] - size, size2, size2);
				} else {
				//g.setStroke(new JPARSECStroke(render.drawDeepSkyObjectsStroke, render.drawDeepSkyObjectsStroke.getLineWidth()+0.25f));
					g.drawOval(pos0[0] - size, pos0[1] - size, size2, size2, false);
				//g.setStroke(render.drawDeepSkyObjectsStroke);
				}
			}
		} else {
			if (fillColor != -1) {
				if (!external) g.setColor(fillColor, true);
				g.fillOval(pos0[0] - size, pos0[1] - size, size2, size2, dist);
				if (!external) g.setColor(color, true);
			}
			if (fillColor != color) {
				//g.setStroke(new JPARSECStroke(render.drawDeepSkyObjectsStroke, render.drawDeepSkyObjectsStroke.getLineWidth()+0.25f));
				g.drawOval(pos0[0] - size, pos0[1] - size, size2, size2, dist);
				//g.setStroke(render.drawDeepSkyObjectsStroke);
			}
		}
	}
	private void drawGlobularCluster(float pos0[], float size, float dist, Graphics g, boolean fast) {
		if (!g.renderingToExternalGraphics()) {
			pos0[0] = (int)pos0[0];
			pos0[1] = (int)pos0[1];
		}
		size = (int)size;
		float size2 = (2*size+1);
		if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
			if (render.fillGlobularColor != -1) {
				g.setColor(render.fillGlobularColor, true);
				g.fillOval(pos0[0] - size, pos0[1] - size, size2, size2, fast);
				g.setColor(render.drawDeepSkyObjectsColor, true);
			}
			drawOval(g, pos0[0] - size, pos0[1] - size, size2, size2);
			if (fast) {
				g.drawStraightLine(pos0[0] - size + 1, pos0[1], pos0[0] + size, pos0[1]);
				g.drawStraightLine(pos0[0], pos0[1] - size + 1, pos0[0], pos0[1] + size);
			} else {
				g.drawLine(pos0[0] - size, pos0[1], pos0[0] + size, pos0[1], false);
				g.drawLine(pos0[0], pos0[1] - size, pos0[0], pos0[1] + size, false);
			}
		} else {
			if (render.fillGlobularColor != -1) {
				g.setColor(render.fillGlobularColor, true);
				g.fillOval(pos0[0] - size, pos0[1] - size, size2, size2, dist);
				g.setColor(render.drawDeepSkyObjectsColor, true);
			}
			g.drawOval(pos0[0] - size, pos0[1] - size, size2, size2, dist);
			g.drawLine(pos0[0] - size, pos0[1], pos0[0] + size, pos0[1], dist, dist);
			g.drawLine(pos0[0], pos0[1] - size, pos0[0], pos0[1] + size, dist, dist);
		}
	}
	private static final JPARSECStroke STROKE2 = new JPARSECStroke(JPARSECStroke.STROKE_POINTS_LOW_SPACE, 2);
	private static final JPARSECStroke STROKE13 = new JPARSECStroke(JPARSECStroke.STROKE_POINTS_LOW_SPACE, new float[] {1, 5}, 0);
	private static final JPARSECStroke STROKE12 = new JPARSECStroke(JPARSECStroke.STROKE_POINTS_LOW_SPACE, new float[] {1, 3}, 0);
	private void drawOpenCluster(float[] pos0, float size, float dist, Graphics g)
	{
		size = (int)size;
		float size2 = (2*size+1);
		if (pixels_per_degree > 100 && size > 15) {
			g.setStroke(STROKE2);
		} else {
			if (size > 10) {
				g.setStroke(STROKE13);
			} else {
				g.setStroke(STROKE12);
			}
		}
		if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
			if (render.fillOpenColor != -1) {
				g.setColor(render.fillOpenColor, true);
				g.fillOval((int)pos0[0] - size, (int)pos0[1] - size, size2, size2, this.fast);
				g.setColor(render.drawDeepSkyObjectsColor, true);
				if (!g.renderingToAndroid() && render.drawFastLinesMode.fastOvals()) {
					this.drawOval(g, (int)pos0[0] - size, (int)pos0[1] - size, size2, size2);
				} else {
					g.drawOval((int)pos0[0] - size, (int)pos0[1] - size, size2, size2, false);
				}
			} else {
/*				int jmax = 9;
				if (size > 5) {
					jmax = (int)(6*Math.sqrt(size));
					int m = jmax % 4;
					if (m != 1) jmax += 1-m;
				}
				for (int j = 0; j <= jmax / 4; j++)
				{
					double ang = j * (Constant.TWO_PI / jmax);
					float dx = size * FastMath.cosf(ang), dy = size * FastMath.sinf(ang);
					this.drawStar(2, new float[] {pos0[0] + dx, pos0[1] + dy}, 0, -1, g);
					if (dy != 0) this.drawStar(2, new float[] {pos0[0] + dx, pos0[1] - dy}, 0, -1, g);
					if (dx != 0) this.drawStar(2, new float[] {pos0[0] - dx, pos0[1] + dy}, 0, -1, g);
					if (dx != 0 && dy != 0) this.drawStar(2, new float[] {pos0[0] - dx, pos0[1] - dy}, 0, -1, g);
				}
*/
				if (render.drawFastLinesMode.fastOvals()) {
					this.drawOval(g, (int)pos0[0] - size, (int)pos0[1] - size, size2, size2);
				} else {
					g.drawOval((int)pos0[0] - size, (int)pos0[1] - size, size2, size2, false);
				}
			}
		} else {
			if (render.fillOpenColor != -1) {
				g.setColor(render.fillOpenColor, true);
				g.fillOval((int)pos0[0] - size, (int)pos0[1] - size, size2, size2, dist);
				g.setColor(render.drawDeepSkyObjectsColor, true);
				g.drawOval((int)pos0[0] - size, (int)pos0[1] - size, size2, size2, dist);
			} else {
				g.drawOval((int)pos0[0] - size, (int)pos0[1] - size, size2, size2, dist);
			}
		}
		g.setStroke(render.drawDeepSkyObjectsStroke);
	}

	private double lastIncrement = -1.0;
	private String lati0 = null, ecl0 = null;
	private void drawEclipticAxis() throws JPARSECException {
		if (//!render.drawCoordinateGrid ||
				fieldDeg < 5.0)
			return;

		if (fieldDeg < 100) {
			LocationElement loc = projection.getEclipticPositionOfRendering();
			if (loc.getLatitude() > field * 0.75)
				return;
		}

		Object path = null;
		boolean fast = false;
		if (render.drawFastLinesMode.fastGrid()) {
			fast = true;
		} else {
			path = g.generalPathInitialize();
		}

		CoordinateSystem.COORDINATE_SYSTEM coordinate_system = render.coordinateSystem;
		g.setStroke(render.drawCoordinateGridStroke);
		g.setColor(render.drawCoordinateGridEclipticColor, true);
		g.setFont(render.drawCoordinateGridFont);
		int radius = -16;
		int increment = 1;
		int eclStep = 2;
		int pxSize = 3;
		if (g.renderingToExternalGraphics()) pxSize = 4;
		if (pixels_per_degree < 10 && !g.renderingToExternalGraphics()) { // Reduce sampling
			increment = 3;
			eclStep = 5;
		}
		if (lastIncrement > 0 && lastIncrement != increment) {
			DataBase.addData("eclipticLine", threadID, null, true);
			db_eclipticLine = -1;
		}
		lastIncrement = increment;

		// Draw the ecliptic
		ArrayList<Object> eclipticLine = null;
		Object o = null;
		if (db_eclipticLine >= 0) {
			o = DataBase.getData(db_eclipticLine);
		} else {
			o = DataBase.getData("eclipticLine", threadID, true);
		}
		if (o != null) eclipticLine = new ArrayList<Object>(Arrays.asList((Object[]) o));
		if (eclipticLine == null) {
			eclipticLine = new ArrayList<Object>();
			LocationElement old_loc = null;
			int n = eclStep == 5 ? 6 : 5, index = -1;
			for (int i = 360; i >= 0; i = i - eclStep)
			{
				index ++;
				LocationElement loc = new LocationElement(i * Constant.DEG_TO_RAD, 0.0, 1.0);
				loc = CoordinateSystem.eclipticToEquatorial(loc, projection.obliquity, true);
				if (projection.obs.getMotherBody() != TARGET.EARTH && projection.obs.getMotherBody() != TARGET.NOT_A_PLANET) {
					loc = projection.getPositionFromBody(loc, false);
				}
				loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, true, false, (float)(Constant.DEG_TO_RAD*1));
				if (loc != null || old_loc != null) {
					if (loc != null) {
						if (index % n == 0) {
							eclipticLine.add(new float[] {(float)loc.getLongitude(), (float)loc.getLatitude(), (float) (i == 360 ? 0 : i)});							
						} else {
							eclipticLine.add(new float[] {(float)loc.getLongitude(), (float)loc.getLatitude()});
						}
					} else {
						eclipticLine.add(null);
					}
				}
				old_loc = loc;
			}
			DataBase.addData("eclipticLine", threadID, eclipticLine.toArray(), true);
			db_eclipticLine = DataBase.getIndex("eclipticLine", threadID);
		}
		if (ecl0 == null) ecl0 = t19;
		boolean eclipticDrawn = false;
		float dist = getDist(this.axesDist);
		if (render.drawCoordinateGridEcliptic && coordinate_system != CoordinateSystem.COORDINATE_SYSTEM.ECLIPTIC)
		{
			float old_pos[] = null; //Projection.INVALID_POSITION;
			float prev_pos1[] = null, prev_pos2[] = null, pos[];
			float[] loc;
			int steppix = (int) (20 + eclStep * pixels_per_degree);
			int rs = eclipticLine.size();
			projection.disableCorrectionOfLocalHorizon();
			for (int i=0; i<rs; i++)
			{
				loc = (float[]) eclipticLine.get(i);
				pos = projection.projectPosition(loc, 0, false);
				if (pos != null && !this.isInTheScreen((int)pos[0], (int)pos[1], steppix)) pos = null;

				if (!projection.isInvalid(pos)) {
					if (old_pos == null) {
						if (!fast) g.generalPathMoveTo(path, pos[0], pos[1]);
						old_pos = pos;
					} else {
						if (!fast) {
							if (i == rs-1 && prev_pos2 == null) {
								if (prev_pos1 != null) {
									prev_pos2 = pos;
									g.generalPathQuadTo(path, prev_pos1[0], prev_pos1[1], prev_pos2[0], prev_pos2[1]);
								} else {
									prev_pos1 = pos;
									g.generalPathLineTo(path, prev_pos1[0], prev_pos1[1]);
								}
							} else {
								if (prev_pos1 == null) {
									prev_pos1 = pos;
								} else {
									if (prev_pos2 == null) {
										prev_pos2 = pos;
									} else {
										g.generalPathCurveTo(path, prev_pos1[0], prev_pos1[1], prev_pos2[0], prev_pos2[1], pos[0], pos[1]);
										prev_pos1 = null;
										prev_pos2 = null;
										old_pos = pos;
									}
								}
							}
						} else {
							if (render.telescope.invertHorizontal || render.telescope.invertVertical ||
									rec.isLineIntersectingRectangle(old_pos[0], old_pos[1], pos[0], pos[1]))
								g.drawStraightLine(old_pos[0], old_pos[1], pos[0], pos[1]);
							old_pos = pos;
						}
					}
					if (render.drawCoordinateGridEclipticLabels && i > 0 && loc.length > 2)
					{
						if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
							g.fillOval(pos[0] - 1.5f, pos[1] - 1.5f, pxSize, pxSize, false);
						} else {
							g.fillOval(pos[0] - 1.5f, pos[1] - 1.5f, pxSize, pxSize, dist);
						}
						if (render.drawCoordinateGridLabels)
						{
							int val = (int) (loc[2]+0.5);
							if (fieldDeg < 30 || eclStep == 5 || (eclStep == 2 && val % 30 == 0))
								drawString(render.drawCoordinateGridEclipticColor, render.drawCoordinateGridFont, Integer.toString(val)+"\u00b0", pos[0], pos[1], -radius, false);
						}
					} else {
						if (!eclipticDrawn && render.drawCoordinateGridLabels && isInTheScreen((int)pos[0], (int)pos[1], -(int)g.getStringWidth(ecl0))) {
							eclipticDrawn = true;
							drawString(render.drawCoordinateGridEclipticColor, render.drawCoordinateGridFont, ecl0, pos[0], pos[1], -radius, false);
						}
					}
				} else {
					if (!fast) {
						if (prev_pos2 != null) {
							g.generalPathQuadTo(path, prev_pos1[0], prev_pos1[1], prev_pos2[0], prev_pos2[1]);
						} else {
							if (prev_pos1 != null) {
								g.generalPathLineTo(path, prev_pos1[0], prev_pos1[1]);
							}
						}
						prev_pos1 = null;
						prev_pos2 = null;
					}
					old_pos = null;
				}
			}
			projection.enableCorrectionOfLocalHorizon();
		}
		if (!fast) {
			if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
				g.draw(path);
			} else {
				g.draw(path, dist);
			}
		}
	}

	private double lastIncrementAxis = -1.0;
	private void drawAxes() throws JPARSECException
	{
		if (!render.drawCoordinateGrid || fieldDeg < 1) {
			return;
		}

		g.setStroke(render.drawCoordinateGridStroke);
		g.setColor(render.drawCoordinateGridColor, true);

		CoordinateSystem.COORDINATE_SYSTEM coordinate_system = render.coordinateSystem;
		int fontSize = render.drawCoordinateGridFont.getSize();
		int radius = -24;
		double step = 15.0 / pixels_per_degree;
		if (fieldDeg < 200) step *= 2;
		if (step > 5) step = 5;
		if (step < 0.21) step = 0.21;
		step = ((int) (step * 10.0)) * 0.1;
		if (step != lastIncrementAxis) {
			DataBase.addData("raline", threadID, null, true);
			DataBase.addData("decline", threadID, null, true);
			db_raline = -1;
			db_decline = -1;
		}
		float dist = 0.0f;
		if (render.anaglyphMode != ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) dist = getDist(axesDist);
		int npix = 50 + (int) (step * pixels_per_degree);

		// Draw celestial points
		if (render.drawCoordinateGridCelestialPoints && fieldDeg < 100) {
			LocationElement loc[] = new LocationElement[] {
				new LocationElement(0, Constant.PI_OVER_TWO, 1.0),
				new LocationElement(0, -Constant.PI_OVER_TWO, 1.0),
			};
			String label[] = new String[] {
					"NCP", "SCP"
			};
			if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) {
				label = new String[] {"PNC", "PSC"};
			}
			int r = 10;
			for (int i=0; i<loc.length; i++) {
				if (render.coordinateSystem != COORDINATE_SYSTEM.EQUATORIAL || !render.drawSkyBelowHorizon)
					loc[i] = projection.getApparentLocationInSelectedCoordinateSystem(loc[i], true, fast, 0);
				float[] pos = projection.projectPosition(loc[i], 0, false);
				if (pos != null && !this.isInTheScreen((int)pos[0], (int)pos[1], npix)) pos = null;

				if (!projection.isInvalid(pos))
				{
					g.drawLine(pos[0]-r, pos[1], pos[0]+r, pos[1], true);
					g.drawLine(pos[0], pos[1]-r, pos[0], pos[1]+r, true);
					if (render.drawCoordinateGridLabels) drawString(render.drawCoordinateGridColor, render.drawCoordinateGridFont, label[i], pos[0], pos[1], -radius, false);
				}
			}
		}

		// Draw longitude lines
		ArrayList<Object> raline = null;
		Object o = null;
		if (db_raline >= 0) {
			o = DataBase.getData(db_raline);
		} else {
			o = DataBase.getData("raline", threadID, true);
		}
		if (o != null) raline = new ArrayList<Object>(Arrays.asList((Object[]) o));
		if (raline == null) {
			boolean fast = false;
			//if (pixels_per_degree < 60) fast = true;
			raline = new ArrayList<Object>();
			lastIncrementAxis = step;
			projection.disableCorrectionOfLocalHorizon();
			if (g.renderingToAndroid() && fieldDeg < 180) step *= 2;
			for (int i = 0; i < 24; i++)
			{
				LocationElement old_loc = null;
				double min_value = -85;
				if (coordinate_system == CoordinateSystem.COORDINATE_SYSTEM.HORIZONTAL && !render.drawSkyBelowHorizon && projection.obs.getMotherBody() != TARGET.NOT_A_PLANET)
					min_value = 0;
				boolean first = false;
				for (double j = min_value; j <= 88; j = j + step)
				{
					if (j > 85) j = 85;
					LocationElement loc = new LocationElement(i / Constant.RAD_TO_HOUR, j * Constant.DEG_TO_RAD, 1.0);
					loc = projection.toEquatorialPosition(loc, fast);
//					loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, true, fast, 0);

					if (render.coordinateSystem != COORDINATE_SYSTEM.EQUATORIAL || render.drawSkyBelowHorizon) {
						loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, true, fast, (float)(Constant.DEG_TO_RAD*0.25));
					} else {
						LocationElement loc1 = projection.getApparentLocationInSelectedCoordinateSystem(loc, true, fast, (float)(step*Constant.DEG_TO_RAD));
						if (loc1 == null) {
							loc = null;
						} else {
							LocationElement loc2 = null;
							if (projection.obs.getMotherBody() != TARGET.NOT_A_PLANET) loc2 = projection.getApparentLocationInSelectedCoordinateSystem(loc, false, fast, COORDINATE_SYSTEM.HORIZONTAL, 0);
							if (loc2 != null && loc2.getLatitude() < 0.0) {
//								LocationElement loc3 = projection.getEquatorialPositionOfZenith();
//								loc3 = projection.getApparentLocationInSelectedCoordinateSystem(loc3, true, fast, 0);
//								double dec = Math.atan(-(Math.cos(loc3.getLatitude()) * Math.cos(loc3.getLongitude()-i / Constant.RAD_TO_HOUR))/Math.sin(loc3.getLatitude()));
								double sincos[] = new double[2];
								FastMath.sincos(projection.obs.getLatitudeRad(), false, sincos);
								double dec = Math.atan(-(sincos[1] * FastMath.cos(projection.ast-loc.getLongitude()))/sincos[0]);
								loc.setLatitude(dec);
								loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, true, fast, 0);
							} else {
								loc = loc1;
							}
						}
					}


					if (old_loc != null && loc != null) {
						if (!first) {
							first = true;
							raline.add(new float[] {(float)loc.getLongitude(), (float)loc.getLatitude(), (float)old_loc.getLongitude(), (float)old_loc.getLatitude()});
						} else {
							raline.add(new float[] {(float)loc.getLongitude(), (float)loc.getLatitude()});
						}
					}
					old_loc = loc;

					if (j >= 85) break;
				}
			}
			projection.enableCorrectionOfLocalHorizon();

			DataBase.addData("raline", threadID, raline.toArray(), true);
			db_raline = DataBase.getIndex("raline", threadID);
		}
		boolean labelDrawn = false;
		double oldTol = 0.0;
		int ra = -1, index, value;
		float old_pos[] = null, prev_pos[] = null;;
		float pos[], tol, px;
		String label;
		float[] loc;
		double labelTolerance = 0.05 * render.width;
		int graphMarginY = this.graphMarginY+(leyendMargin-(int)rec.getMinY());
		int nmax = 100, nin = -1;
		int x[] = new int[nmax], y[] = new int[nmax];
		float maxYminus50 = rec.getMaxY()-50;
		float heightPlus50MinusMaxY = g.getHeight()-rec.getMaxY()+50;
		float widthMinus1MinusGraphMarginX = render.width-1-rec.getMinX();
		float maxYPlusFontSize3Over2 = rec.getMaxY()+ (fontSize*3)/2;
		boolean showRALabelsOnTop = render.telescope.invertVertical || render.centralLatitude < (-60*Constant.DEG_TO_RAD);
		// TODO
		boolean fastMode = render.drawFastLinesMode.fastGrid();
		if (fastMode) {
			if (!render.drawCoordinateGridStroke.isContinuousLine()) fastMode = false;
			if (render.drawCoordinateGridStroke.getLineWidth() > 2) fastMode = false;
		}
		projection.disableCorrectionOfLocalHorizon();
		for (Iterator<Object> itr = raline.iterator();itr.hasNext();)
		{
			float obj[] = (float[]) itr.next();
			if (obj.length == 4) {
				labelDrawn = false;
				oldTol = 0.0;
				float[] old_loc = new float[] {obj[2], obj[3]};
				old_pos = projection.projectPosition(old_loc, 0, false);
				ra ++;
			}
			loc = obj;
			if (fieldDeg < 15 && Math.abs(render.centralLatitude-loc[1]) > field) {
				pos = null;
			} else {
				pos = projection.projectPosition(loc, 0, false);
				if (pos != null && !this.isInTheScreen((int)pos[0], (int)pos[1], npix)) pos = null;
			}

			if (!projection.isInvalid(pos))
			{
				if (!projection.isInvalid(old_pos)) {
					if (prev_pos == old_pos && nin < nmax-1) {
						//if (render.telescope.invertHorizontal || render.telescope.invertVertical ||
						//		rec.isLineIntersectingRectangle(old_pos[0], old_pos[1], pos[0], pos[1])) {
							nin ++;
							if (nin == 0) {
								x[nin] = (int)old_pos[0];
								y[nin] = (int)old_pos[1];
								nin ++;
							}
							x[nin] = (int)pos[0];
							y[nin] = (int)pos[1];
						//}
					} else {
						if (nin > 0) {
							if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
								g.drawLines(x, y, nin+1, fastMode);
							} else {
								g.drawLines(x, y, nin+1, DataSet.toFloatArray(DataSet.getSetOfValues(dist, dist, nin+1, false)), fastMode);
							}
						}
						x[0] = (int)old_pos[0];
						y[0] = (int)old_pos[1];
						x[1] = (int)pos[0];
						y[1] = (int)pos[1];
						nin = 1;
					}
					prev_pos = pos;

//					g.drawLine(old_pos[0], old_pos[1], pos[0], pos[1]);
				}

				if (render.drawCoordinateGridLabels &&
						(pos[1] > maxYminus50 && !render.telescope.invertVertical || pos[1] < heightPlus50MinusMaxY && render.telescope.invertVertical)
						&&
						(pos[0] > rec.getMinX() && !render.telescope.invertHorizontal || pos[0] < widthMinus1MinusGraphMarginX && render.telescope.invertHorizontal) &&
						(pos[0] < rec.getMaxX() && !render.telescope.invertHorizontal || pos[0] > render.width-1-rec.getMaxX() && render.telescope.invertHorizontal)) {

					if (render.telescope.invertVertical) {
						tol = Math.abs(rec.getMaxY() - (g.getHeight() - pos[1] - 1));
					} else {
						tol = Math.abs(rec.getMaxY() - pos[1]);
					}

					if ((!labelDrawn || tol<oldTol)) {
						if (coordinate_system != CoordinateSystem.COORDINATE_SYSTEM.EQUATORIAL) {
							int v = (int) (ra * 15.0 + 0.5);
							label = Integer.toString(v)+"\u00b0";
							if (coordinate_system == CoordinateSystem.COORDINATE_SYSTEM.HORIZONTAL) {
								if (v == 0) label += " (N)";
								if (v == 180) label += " (S)";
								if (v == 90) label += " (E)";
								if (v == 270) {
									if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) {
										label += " (O)";
									} else {
										label += " (W)";
									}
								}
							}
						} else {
							if (!render.useSuperScriptForRA) {
								label = ra + "h";
							} else {
								label = ra + "^{h}";
							}
						}
						index = DataSet.getIndex(labelsAxesNameX, label);
						if (index < 0 || tol<oldTol) {
							if (index >= 0) {
								labelsAxesNameX = DataSet.eliminateRowFromTable(labelsAxesNameX, 1+index);
								labelsAxesX.remove(index);
							}
							drawStringAxes(render.drawCoordinateGridColor, render.drawCoordinateGridFont, label, pos[0], maxYPlusFontSize3Over2, 0, 0);
							labelDrawn = true;
						}
						oldTol = tol;
					}
				}
			}
			old_pos = pos;
		}
		projection.enableCorrectionOfLocalHorizon();
		if (nin > 0) {
			if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
				g.drawLines(x, y, nin+1, fastMode);
			} else {
				g.drawLines(x, y, nin+1, DataSet.toFloatArray(DataSet.getSetOfValues(dist, dist, nin+1, false)), fastMode);
			}
		}

		// Draw latitude lines
		if (lati0 == null) {
			lati0 = t18;
			if (render.coordinateSystem == CoordinateSystem.COORDINATE_SYSTEM.ECLIPTIC) lati0 = t19;
			if (render.coordinateSystem == CoordinateSystem.COORDINATE_SYSTEM.GALACTIC) lati0 = t20;
			if (render.coordinateSystem == CoordinateSystem.COORDINATE_SYSTEM.HORIZONTAL) lati0 = t23;
		}
		boolean equatorDrawn = false;
		double min_value = -75;
		double cs = 15.0;
		if (coordinate_system == CoordinateSystem.COORDINATE_SYSTEM.HORIZONTAL && !render.drawSkyBelowHorizon && projection.obs.getMotherBody() != TARGET.NOT_A_PLANET)
			min_value = 0;

		ArrayList<Object> decline = null;
		if (db_decline >= 0) {
			o = DataBase.getData(db_decline);
		} else {
			o = DataBase.getData("decline", threadID, true);
		}
		if (o != null) decline = new ArrayList<Object>(Arrays.asList((Object[]) o));
		if (decline == null) {
			boolean fast = false;
			//if (pixels_per_degree < 60) fast = true;
			decline = new ArrayList<Object>();
			projection.disableCorrectionOfLocalHorizon();
			double minStep = (2.0 / pixels_per_degree);
			LocationElement loc0;
			for (double j = min_value; j <= 80; j = j + cs)
			{
				float[] old_loc = null;
				boolean first = false, firstt = false, last = false;
				step = minStep;
				if (Math.abs(j) > 30) step *= 0.5*0.866/Math.cos(j*Constant.DEG_TO_RAD);
				if (g.renderingToAndroid() && fieldDeg < 180) step *= 1.5;
				for (double i = 24; i >= -2*step; i = i - step)
				{
					if (i <= 0.0) {
						i = 0.0;
						last = true;
					}
					loc0 = new LocationElement(i / Constant.RAD_TO_HOUR, j * Constant.DEG_TO_RAD, 1.0);
					if (render.coordinateSystem != COORDINATE_SYSTEM.HORIZONTAL) {
						loc0 = projection.toEquatorialPosition(loc0, fast);
						if (render.coordinateSystem != COORDINATE_SYSTEM.EQUATORIAL || render.drawSkyBelowHorizon) {
							loc0 = projection.getApparentLocationInSelectedCoordinateSystem(loc0, true, fast, (float)(Constant.DEG_TO_RAD*0.25));
						} else {
							LocationElement loc1 = projection.getApparentLocationInSelectedCoordinateSystem(loc0, true, fast, (float)(step*0.25/Constant.RAD_TO_HOUR));
							if (loc1 == null) {
								loc0 = null;
							} else {
								LocationElement loc2 = null;
								if (projection.obs.getMotherBody() != TARGET.NOT_A_PLANET) loc2 = projection.getApparentLocationInSelectedCoordinateSystem(loc0, false, fast, COORDINATE_SYSTEM.HORIZONTAL, 0);
								if (loc2 != null && loc2.getLatitude() < 0.0) {
									double tmp = 0;
									tmp = (Math.sin(tmp) - Math.sin(projection.obs.getLatitudeRad()) * Math.sin(loc0.getLatitude())) / (Math.cos(projection.obs.getLatitudeRad()) * Math.cos(loc0.getLatitude()));
									if (Math.abs(tmp) < 1.0) {
										double nra = projection.ast - Math.acos(tmp) * FastMath.sign(FastMath.sin(projection.ast-loc0.getLongitude()));
										loc0.setLongitude(nra);
										loc0 = projection.getApparentLocationInSelectedCoordinateSystem(loc0, false, fast, 0);
									} else {
										loc0 = loc1;
									}
								} else {
									loc0 = loc1;
								}
							}
						}
					}
					if (old_loc != null && loc0 != null) {
						if (!first) {
							first = true;
							if (!firstt) {
								firstt = true;
								decline.add(new float[] {(float)loc0.getLongitude(), (float)loc0.getLatitude(), old_loc[0], old_loc[1], (float)j});
							} else {
								decline.add(new float[] {(float)loc0.getLongitude(), (float)loc0.getLatitude(), old_loc[0], old_loc[1]});
							}
						} else {
							decline.add(new float[] {(float)loc0.getLongitude(), (float)loc0.getLatitude()});
						}
					} else {
						if (old_loc == null && loc0 == null) first = false;
					}
					old_loc = null;
					if (loc0 != null) old_loc = new float[] {(float)loc0.getLongitude(), (float)loc0.getLatitude()};
					if (last) break;
				}
			}
			projection.enableCorrectionOfLocalHorizon();

			DataBase.addData("decline", threadID, decline.toArray(), true);
			db_decline = DataBase.getIndex("decline", threadID);
		}
		double dec = min_value-cs;
		labelDrawn = false;
		oldTol = 0.1;
		old_pos = null;
		nin = -1;
		float minYplusFontSize = rec.getMinY()+fontSize;
		float minXplus100 = rec.getMinX()+100;
		npix = 400;
		for (Iterator<Object> itr = decline.iterator();itr.hasNext();)
		{
			float obj[] = (float[]) itr.next();
			if (obj.length > 2) {
				float[] old_loc = new float[] {obj[2], obj[3]};
				old_pos = projection.projectPosition(old_loc, 0, false);
				if (obj.length > 4) {
					labelDrawn = false;
					dec = obj[4];
					oldTol = 0.1;
				}
			}
			loc = obj;
			if (fieldDeg < 15 && Math.abs(render.centralLatitude-loc[1]) > field) {
				pos = null;
			} else {
				pos = projection.projectPosition(loc, 0, false);
				if (pos != null && !this.isInTheScreen((int)pos[0], (int)pos[1], npix)) pos = null;
			}

			if (!projection.isInvalid(pos))
			{
				if (!projection.isInvalid(old_pos)) {
					if (prev_pos == old_pos && nin < nmax-1) {
//						if (rec.isLineIntersectingRectangle(old_pos[0], old_pos[1], pos[0], pos[1])) {
							nin ++;
							if (nin == 0) {
								x[nin] = (int)old_pos[0];
								y[nin] = (int)old_pos[1];
								nin ++;
							}
							x[nin] = (int)pos[0];
							y[nin] = (int)pos[1];
//						}
					} else {
						if (nin > 0) {
							if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
								g.drawLines(x, y, nin+1, fastMode);
							} else {
								g.drawLines(x, y, nin+1, DataSet.toFloatArray(DataSet.getSetOfValues(dist, dist, nin+1, false)), fastMode);
							}
						}
						x[0] = (int)old_pos[0];
						y[0] = (int)old_pos[1];
						x[1] = (int)pos[0];
						y[1] = (int)pos[1];
						nin = 1;
					}
					prev_pos = pos;

//					g.drawLine(old_pos[0], old_pos[1], pos[0], pos[1]);
				}
				old_pos = pos;

				if (render.coordinateSystem != CoordinateSystem.COORDINATE_SYSTEM.HORIZONTAL || (fieldDeg < 200 && fieldDeg > 60)) {
					if (dec == 0 && !equatorDrawn && render.drawCoordinateGridLabels && isInTheScreen((int)pos[0], (int)pos[1], -(int)g.getStringWidth(lati0))) {
						equatorDrawn = true;
						drawString(render.drawCoordinateGridColor, render.drawCoordinateGridFont, lati0, pos[0], pos[1], -radius, false);
					}
				}
					if (render.drawCoordinateGridLabels &&
							((!render.telescope.invertHorizontal && pos[0] < minXplus100) || (render.telescope.invertHorizontal && pos[0] > render.width-1-minXplus100))
							&&
							((!showRALabelsOnTop && pos[1] > minYplusFontSize && pos[1] < rec.getMaxY()) ||
									(showRALabelsOnTop && pos[1] < render.height-1-minYplusFontSize && pos[1] > render.height-1-(rec.getMaxY())))
							) {
						if (render.telescope.invertHorizontal) {
							tol = (float) Math.abs((double) (rec.getMinX())-(render.width-1-pos[0]));
						} else {
							tol = (float) Math.abs((double) (rec.getMinX())-(double)pos[0]);
						}
						if ((!labelDrawn || tol < oldTol) && tol < labelTolerance) {
							value = (int) (Math.abs(dec) + 0.5);
							if (dec < 0) value = -value;
							label = Integer.toString(value) + "\u00b0";

							index = DataSet.getIndex(labelsAxesNameY, label);
							if (index < 0 || tol<oldTol) {
								if (index >= 0) {
									labelsAxesY.remove(index);
									labelsAxesNameY = DataSet.eliminateRowFromTable(labelsAxesNameY, 1+index);
								}
								px = fontSize-4;
								//if (dec<0) px *= 1.25;
								drawStringAxes(render.drawCoordinateGridColor, render.drawCoordinateGridFont, label, rec.getMinX()-px, pos[1], 0, -1);
								labelDrawn = true;
							}
							oldTol = tol;
						}
					}
			} else {
				old_pos = null;
			}
		}
		if (nin > 0) {
			if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
				g.drawLines(x, y, nin+1, fastMode);
			} else {
				g.drawLines(x, y, nin+1, DataSet.toFloatArray(DataSet.getSetOfValues(dist, dist, nin+1, false)), fastMode);
			}
		}
	}

	private void drawAxesOld() throws JPARSECException
	{
		if (!render.drawCoordinateGrid || fieldDeg < 1) {
			return;
		}

		g.setStroke(render.drawCoordinateGridStroke);
		g.setColor(render.drawCoordinateGridColor, true);

		Object pathAxes = g.generalPathInitialize();

		CoordinateSystem.COORDINATE_SYSTEM coordinate_system = render.coordinateSystem;
		int fontSize = render.drawCoordinateGridFont.getSize();
		double labelTolerance = 0.05 * render.width;
		int radius = -24;
		double step = 15.0 / pixels_per_degree;
		if (fieldDeg < 200) step *= 2;
		if (step > 5) step = 5;
		if (step < 0.21) step = 0.21;
		step = ((int) (step * 10.0)) * 0.1;
		step *= 0.25;
		if (step != lastIncrementAxis) {
			DataBase.addData("raline", threadID, null, true);
			DataBase.addData("decline", threadID, null, true);
			db_raline = -1;
			db_decline = -1;
		}
		int npix = 20 + (int) (step * pixels_per_degree);

		// Draw celestial points
		if (render.drawCoordinateGridCelestialPoints && fieldDeg < 100) {
			LocationElement loc[] = new LocationElement[] {
				new LocationElement(0, Constant.PI_OVER_TWO, 1.0),
				new LocationElement(0, -Constant.PI_OVER_TWO, 1.0),
			};
			String label[] = new String[] {
					"NCP", "SCP"
			};
			if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) {
				label = new String[] {"PNC", "PSC"};
			}
			int r = 10;
			for (int i=0; i<loc.length; i++) {
				if (render.coordinateSystem != COORDINATE_SYSTEM.EQUATORIAL || render.drawSkyBelowHorizon)
					loc[i] = projection.getApparentLocationInSelectedCoordinateSystem(loc[i], true, fast, 0);
				float[] pos = projection.projectPosition(loc[i], 0, false);
				if (pos != null && !this.isInTheScreen((int)pos[0], (int)pos[1], npix)) pos = null;

				if (!projection.isInvalid(pos))
				{
					g.drawLine(pos[0]-r, pos[1], pos[0]+r, pos[1], true);
					g.drawLine(pos[0], pos[1]-r, pos[0], pos[1]+r, true);
					drawString(render.drawCoordinateGridColor, render.drawCoordinateGridFont, label[i], pos[0], pos[1], -radius, false);
				}
			}
		}

		// Draw longitude lines
		ArrayList<Object> raline = null;
		Object o = null;
		if (db_raline >= 0) {
			o = DataBase.getData(db_raline);
		} else {
			o = DataBase.getData("raline", threadID, true);
		}
		if (o != null) raline = new ArrayList<Object>(Arrays.asList((Object[]) o));
		if (raline == null) {
			boolean fast = false;
			//if (pixels_per_degree < 60) fast = true;
			raline = new ArrayList<Object>();
			lastIncrementAxis = step;
			//raline.ensureCapacity((int) (170 * 24 / step) + 2);
			projection.disableCorrectionOfLocalHorizon();
			for (int i = 0; i < 24; i++)
			{
				LocationElement old_loc = null;
				double min_value = -85;
				if (coordinate_system == CoordinateSystem.COORDINATE_SYSTEM.HORIZONTAL && !render.drawSkyBelowHorizon && projection.obs.getMotherBody() != TARGET.NOT_A_PLANET)
					min_value = 0;
				boolean first = false;
				for (double j = min_value; j <= 85; j = j + step)
				{
					LocationElement loc = new LocationElement(i / Constant.RAD_TO_HOUR, j * Constant.DEG_TO_RAD, 1.0);
					loc = projection.toEquatorialPosition(loc, fast);
//					loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, true, fast, 0);

					if (render.coordinateSystem != COORDINATE_SYSTEM.EQUATORIAL || render.drawSkyBelowHorizon) {
						loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, true, fast, (float)(Constant.DEG_TO_RAD*0.25));
					} else {
						LocationElement loc1 = projection.getApparentLocationInSelectedCoordinateSystem(loc, true, fast, (float)(step*0.5*Constant.DEG_TO_RAD));
						if (loc1 == null) {
							loc = null;
						} else {
							LocationElement loc2 = null;
							if (projection.obs.getMotherBody() != TARGET.NOT_A_PLANET) loc2 = projection.getApparentLocationInSelectedCoordinateSystem(loc, false, fast, COORDINATE_SYSTEM.HORIZONTAL, 0);
							if (loc2 != null && loc2.getLatitude() < 0.0) {
								double sincos[] = new double[2];
								FastMath.sincos(projection.obs.getLatitudeRad(), false, sincos);
								double dec = Math.atan(-(sincos[1] * FastMath.cos(projection.ast-loc.getLongitude()))/sincos[0]);
								loc.setLatitude(dec);
								loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, true, fast, 0);
							} else {
								loc = loc1;
							}
						}
					}

					if (old_loc != null && loc != null) {
						if (!first) {
							first = true;
							raline.add(new float[] {(float)loc.getLongitude(), (float)loc.getLatitude(), (float)old_loc.getLongitude(), (float)old_loc.getLatitude()});
						} else {
							raline.add(new float[] {(float)loc.getLongitude(), (float)loc.getLatitude()});
						}
					}
					old_loc = loc;
				}
			}
			projection.enableCorrectionOfLocalHorizon();

			DataBase.addData("raline", threadID, raline.toArray(), true);
			db_raline = DataBase.getIndex("raline", threadID);
		}
		boolean labelDrawn = false;
		double oldTol = 0.0;
		int ra = -1, index, value;
		float old_pos[] = null;
		float prev_pos1[] = null, prev_pos2[] = null, pos[], tol, px;
		String label;
		float[] loc;
		boolean showRALabelsOnTop = render.telescope.invertVertical || render.centralLatitude < (-60*Constant.DEG_TO_RAD);
		//int rs = raline.size();
		//for (int i = 0; i<rs; i++)
		projection.disableCorrectionOfLocalHorizon();
		for (Iterator<Object> itr = raline.iterator();itr.hasNext();)
		{
			float obj[] = (float[]) itr.next(); //.poll();
			if (obj.length > 2) {
				if (prev_pos2 != null) {
					g.generalPathQuadTo(pathAxes, prev_pos1[0], prev_pos1[1], prev_pos2[0], prev_pos2[1]);
				} else {
					if (prev_pos1 != null)
						g.generalPathLineTo(pathAxes, prev_pos1[0], prev_pos1[1]);
				}
				labelDrawn = false;
				oldTol = 0.0;
				float[] old_loc = new float[] {obj[2], obj[3]};
				old_pos = projection.projectPosition(old_loc, 0, false);
				if (old_pos != null) g.generalPathMoveTo(pathAxes, old_pos[0], old_pos[1]);
				ra ++;
				prev_pos1 = null;
				prev_pos2 = null;
			}
			loc = obj;
			pos = projection.projectPosition(loc, 0, false);
			if (pos != null && !this.isInTheScreen((int)pos[0], (int)pos[1], npix)) pos = null;

			if (!projection.isInvalid(pos))
			{
				if (old_pos == null || projection.isInvalid(old_pos)) {
					g.generalPathMoveTo(pathAxes, pos[0], pos[1]);
					old_pos = pos;
				} else {
					if (prev_pos1 == null) {
						prev_pos1 = pos;
					} else {
						if (prev_pos2 == null) {
							prev_pos2 = pos;
						} else {
							g.generalPathCurveTo(pathAxes, prev_pos1[0], prev_pos1[1], prev_pos2[0], prev_pos2[1], pos[0], pos[1]);
							prev_pos1 = null;
							prev_pos2 = null;
							old_pos = pos;
						}
					}
				}

				if (render.drawCoordinateGridLabels && pos[1] > rec.getMaxY()-50 &&
						(pos[0] > graphMarginX && !render.telescope.invertHorizontal || pos[0] < render.width-1-graphMarginX && render.telescope.invertHorizontal) && pos[0] < render.width) {
					if (render.telescope.invertVertical) {
						tol = Math.abs(rec.getMaxY() - (g.getHeight() - pos[1] - 1));
					} else {
						tol = Math.abs(rec.getMaxY() - pos[1]);
					}

					if ((!labelDrawn || tol<oldTol) && tol < labelTolerance) {
						if (!render.useSuperScriptForRA) {
							label = ra + "h";
						} else {
							label = ra + "^{h}";
						}
						if (coordinate_system != CoordinateSystem.COORDINATE_SYSTEM.EQUATORIAL) {
							int v = (int) (ra * 15.0 + 0.5);
							label = Integer.toString(v)+"\u00b0";
							if (coordinate_system == CoordinateSystem.COORDINATE_SYSTEM.HORIZONTAL) {
								if (v == 0) label += " (N)";
								if (v == 180) label += " (S)";
								if (v == 90) label += " (E)";
								if (v == 270) {
									if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) {
										label += " (O)";
									} else {
										label += " (W)";
									}
								}
							}
						}
						index = DataSet.getIndex(labelsAxesNameX, label);
						if (index < 0 || tol<oldTol) {
							if (index >= 0) {
								labelsAxesNameX = DataSet.eliminateRowFromTable(labelsAxesNameX, 1+index);
								labelsAxesX.remove(index);
							}
							drawStringAxes(render.drawCoordinateGridColor, render.drawCoordinateGridFont, label, pos[0], rec.getMaxY()+ fontSize*3/2, 0, 0);
							labelDrawn = true;
						}
					}
					oldTol = tol;
				}
			} else {
				if (prev_pos2 != null) {
					g.generalPathQuadTo(pathAxes, prev_pos1[0], prev_pos1[1], prev_pos2[0], prev_pos2[1]);
				} else {
					if (prev_pos1 != null) {
						g.generalPathLineTo(pathAxes, prev_pos1[0], prev_pos1[1]);
					}
				}
				prev_pos1 = null;
				prev_pos2 = null;
				old_pos = null;
			}
		}
		projection.enableCorrectionOfLocalHorizon();

		// Draw latitude lines
		if (lati0 == null) {
			lati0 = t18;
			if (render.coordinateSystem == CoordinateSystem.COORDINATE_SYSTEM.ECLIPTIC) lati0 = t19;
			if (render.coordinateSystem == CoordinateSystem.COORDINATE_SYSTEM.GALACTIC) lati0 = t20;
			if (render.coordinateSystem == CoordinateSystem.COORDINATE_SYSTEM.HORIZONTAL) lati0 = t23;
		}
		boolean equatorDrawn = false;
		double min_value = -75;
		double cs = 15.0;
		if (coordinate_system == CoordinateSystem.COORDINATE_SYSTEM.HORIZONTAL && !render.drawSkyBelowHorizon && projection.obs.getMotherBody() != TARGET.NOT_A_PLANET)
			min_value = 0;

		ArrayList<Object> decline = null;
		if (db_decline >= 0) {
			o = DataBase.getData(db_decline);
		} else {
			o = DataBase.getData("decline", threadID, true);
		}
		if (o != null) decline = new ArrayList<Object>(Arrays.asList((Object[]) o));
		if (decline == null) {
			boolean fast = false;
			decline = new ArrayList<Object>();
			projection.disableCorrectionOfLocalHorizon();
			double minStep = (2.0 / pixels_per_degree) / 4.0;
			LocationElement loc0;
			for (double j = min_value; j <= 80; j = j + cs)
			{
				float[] old_loc = null;
				boolean first = false, firstt = false, last = false;
				step = minStep;
				if (Math.abs(j) > 30) step *= 0.5*0.866/Math.cos(j*Constant.DEG_TO_RAD);
				for (double i = 24; i >= -2*step; i = i - step)
				{
					if (i <= 0.0) {
						i = 0.0;
						last = true;
					}
					loc0 = new LocationElement(i / Constant.RAD_TO_HOUR, j * Constant.DEG_TO_RAD, 1.0);
					if (render.coordinateSystem != COORDINATE_SYSTEM.HORIZONTAL) {
						loc0 = projection.toEquatorialPosition(loc0, fast);
						if (render.coordinateSystem != COORDINATE_SYSTEM.EQUATORIAL || render.drawSkyBelowHorizon) {
							loc0 = projection.getApparentLocationInSelectedCoordinateSystem(loc0, true, fast, 0);
						} else {
							LocationElement loc1 = projection.getApparentLocationInSelectedCoordinateSystem(loc0, true, fast, (float)(step*0.25/Constant.RAD_TO_HOUR));
							if (loc1 == null) {
								loc0 = null;
							} else {
								LocationElement loc2 = null;
								if (projection.obs.getMotherBody() != TARGET.NOT_A_PLANET) loc2 = projection.getApparentLocationInSelectedCoordinateSystem(loc0, false, fast, COORDINATE_SYSTEM.HORIZONTAL, 0);
								if (loc2 != null && loc2.getLatitude() < 0.0) {
									double tmp = 0;
									tmp = (Math.sin(tmp) - Math.sin(projection.obs.getLatitudeRad()) * Math.sin(loc0.getLatitude())) / (Math.cos(projection.obs.getLatitudeRad()) * Math.cos(loc0.getLatitude()));
									if (Math.abs(tmp) < 1.0) {
										double nra = projection.ast - Math.acos(tmp) * FastMath.sign(FastMath.sin(projection.ast-loc0.getLongitude()));
										loc0.setLongitude(nra);
										loc0 = projection.getApparentLocationInSelectedCoordinateSystem(loc0, false, fast, 0);
									} else {
										loc0 = loc1;
									}
								} else {
									loc0 = loc1;
								}
							}
						}
					}

					if (old_loc != null && loc0 != null) {
						if (!first) {
							first = true;
							if (!firstt) {
								firstt = true;
								decline.add(new float[] {(float)loc0.getLongitude(), (float)loc0.getLatitude(), old_loc[0], old_loc[1], (float)j});
							} else {
								decline.add(new float[] {(float)loc0.getLongitude(), (float)loc0.getLatitude(), old_loc[0], old_loc[1]});
							}
						} else {
							decline.add(new float[] {(float)loc0.getLongitude(), (float)loc0.getLatitude()});
						}
					} else {
						if (old_loc == null && loc0 == null) first = false;
					}
					old_loc = null;
					if (loc0 != null) old_loc = new float[] {(float)loc0.getLongitude(), (float)loc0.getLatitude()};
					if (last) break;
				}
			}
			projection.enableCorrectionOfLocalHorizon();

			DataBase.addData("decline", threadID, decline.toArray(), true);
			db_decline = DataBase.getIndex("decline", threadID);
		}
		double dec = min_value-cs;
		labelDrawn = false;
		oldTol = 0.1;
		old_pos = null;
		prev_pos1 = null;
		prev_pos2 = null;
		npix = 50;
		for (Iterator<Object> itr = decline.iterator();itr.hasNext();)
		{
			float obj[] = (float[]) itr.next(); //decline.poll();
			if (obj.length > 2) {
				if (prev_pos2 != null) {
					g.generalPathQuadTo(pathAxes, prev_pos1[0], prev_pos1[1], prev_pos2[0], prev_pos2[1]);
				} else {
					if (prev_pos1 != null)
						g.generalPathLineTo(pathAxes, prev_pos1[0], prev_pos1[1]);
				}

				float[] old_loc = new float[] {obj[2], obj[3]};
				old_pos = projection.projectPosition(old_loc, 0, false);
				if (old_pos != null) g.generalPathMoveTo(pathAxes, old_pos[0], old_pos[1]);
				if (obj.length > 4) {
					labelDrawn = false;
					dec = obj[4];
					oldTol = 0.1;
				}
				prev_pos1 = null;
				prev_pos2 = null;
			}

			loc = obj;
			pos = projection.projectPosition(loc, 0, false);
			if (pos != null && !this.isInTheScreen((int)pos[0], (int)pos[1], npix)) pos = null;

			if (!projection.isInvalid(pos))
			{
				if (old_pos == null || projection.isInvalid(old_pos)) {
					g.generalPathMoveTo(pathAxes, pos[0], pos[1]);
					old_pos = pos;
				} else {
					if (!itr.hasNext()) { //i == decline.size()-1) {
						if (prev_pos2 != null) {
							g.generalPathCurveTo(pathAxes, prev_pos1[0], prev_pos1[1], prev_pos2[0], prev_pos2[1], pos[0], pos[1]);
						} else {
							if (prev_pos1 != null) {
								g.generalPathQuadTo(pathAxes, prev_pos1[0], prev_pos1[1], pos[0], pos[1]);
							} else {
								g.generalPathLineTo(pathAxes, pos[0], pos[1]);
							}
						}
					} else {
						if (prev_pos1 == null) {
							prev_pos1 = pos;
						} else {
							if (prev_pos2 == null) {
								prev_pos2 = pos;
							} else {
								g.generalPathCurveTo(pathAxes, prev_pos1[0], prev_pos1[1], prev_pos2[0], prev_pos2[1], pos[0], pos[1]);
								prev_pos1 = null;
								prev_pos2 = null;
								old_pos = pos;
							}
						}
					}
				}

				if (render.coordinateSystem != CoordinateSystem.COORDINATE_SYSTEM.HORIZONTAL || (fieldDeg < 200 && fieldDeg > 60)) {
					if (dec == 0 && !equatorDrawn && render.drawCoordinateGridLabels && isInTheScreen((int)pos[0], (int)pos[1], -(int)g.getStringWidth(lati0))) {
						equatorDrawn = true;
						drawString(render.drawCoordinateGridColor, render.drawCoordinateGridFont, lati0, pos[0], pos[1], -radius, false);
					}
				}
					if (render.drawCoordinateGridLabels && pos[0] < rec.getMinX()+100 &&
							pos[1] > rec.getMinY()+fontSize && pos[1] < render.height-graphMarginX) {
						if (render.telescope.invertHorizontal) {
							tol = (float) Math.abs((double) (rec.getMinX())-(render.width-1-pos[0]));
						} else {
							tol = (float) Math.abs((double) (rec.getMinX())-(double)pos[0]);
						}
						if ((!labelDrawn || tol < oldTol) && tol < labelTolerance) {
							value = (int) (Math.abs(dec) + 0.5);
							if (dec < 0) value = -value;
							label = Integer.toString(value) + "\u00b0";
							index = DataSet.getIndex(labelsAxesNameY, label);
							if (index < 0 || tol<oldTol) {
								if (index >= 0) {
									labelsAxesY.remove(index);
									labelsAxesNameY = DataSet.eliminateRowFromTable(labelsAxesNameY, 1+index);
								}
								px = fontSize-4;
								//if (dec<0) px *= 1.25;
								drawStringAxes(render.drawCoordinateGridColor, render.drawCoordinateGridFont, label, graphMarginX-px, pos[1], 0, -1);
								labelDrawn = true;
								oldTol = tol;
							}
						}
					}
			} else {
				if (prev_pos2 != null) {
					g.generalPathQuadTo(pathAxes, prev_pos1[0], prev_pos1[1], prev_pos2[0], prev_pos2[1]);
				} else {
					if (prev_pos1 != null) {
						g.generalPathLineTo(pathAxes, prev_pos1[0], prev_pos1[1]);
					}
				}
				prev_pos1 = null;
				prev_pos2 = null;
				old_pos = null;
			}
		}

		g.draw(pathAxes);
	}

	private double lastIncrementHoriz = -1;
	private void drawHorizon() throws JPARSECException {
		if (//!render.drawCoordinateGrid ||
				projection.obs.getMotherBody() == TARGET.NOT_A_PLANET || !projection.eph.isTopocentric) return;

		// Draw horizon
		if (!render.drawSkyBelowHorizon) {
			if (fieldDeg < 150) {
				LocationElement locH = projection.getApparentLocationInSelectedCoordinateSystem(loc0Date, //projection.getEquatorialPositionOfRendering(),
						false, true, COORDINATE_SYSTEM.HORIZONTAL, 0);
				if (Math.abs(locH.getLatitude()) > field) return;
			}

			if (render.drawCoordinateGridCardinalPoints && projection.obs.getMotherBody() != TARGET.NOT_A_PLANET) {
				COORDINATE_SYSTEM coordinate_system = render.coordinateSystem;
				double step = 15.0 / pixels_per_degree;
				if (fieldDeg < 200) step *= 2;
				if (step > 5 || fieldDeg < 100) step = 5;
				if (step < 0.21) step = 0.21;
				step = ((int) (step * 10.0)) * 0.1;
				int npix = 50 + (int) (step * pixels_per_degree);
				g.setFont(render.drawCoordinateGridFont);
				g.setFont(FONT.getDerivedFont(g.getFont(), g.getFont().getSize(), 1));

				projection.disableCorrectionOfLocalHorizon();
				double pos = (render.drawCoordinateGridFont.getSize() * 1.5) / pixels_per_radian;
				LocationElement loc = new LocationElement(0, -pos, 1);
				if (coordinate_system != COORDINATE_SYSTEM.HORIZONTAL) {
					loc = CoordinateSystem.horizontalToEquatorial(loc, projection.ast, projection.obs.getLatitudeRad(), true);
					loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, false, fast, 0);
				}
				float p[] = projection.projectPosition(loc, 0, false);
				if (p != null && !this.isInTheScreen((int)p[0], (int)p[1], npix)) p = null;
				if (p != null) g.drawString("N", p[0]-g.getStringWidth("N")/2, p[1]);

				loc = new LocationElement(Math.PI, -pos, 1);
				if (coordinate_system != COORDINATE_SYSTEM.HORIZONTAL) {
					loc = CoordinateSystem.horizontalToEquatorial(loc, projection.ast, projection.obs.getLatitudeRad(), true);
					loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, false, fast, 0);
				}
				p = projection.projectPosition(loc, 0, false);
				if (p != null && !this.isInTheScreen((int)p[0], (int)p[1], npix)) p = null;
				if (p != null) g.drawString("S", p[0]-g.getStringWidth("S")/2, p[1]);

				loc = new LocationElement(Constant.PI_OVER_TWO, -pos, 1);
				if (coordinate_system != COORDINATE_SYSTEM.HORIZONTAL) {
					loc = CoordinateSystem.horizontalToEquatorial(loc, projection.ast, projection.obs.getLatitudeRad(), true);
					loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, false, fast, 0);
				}
				p = projection.projectPosition(loc, 0, false);
				if (p != null && !this.isInTheScreen((int)p[0], (int)p[1], npix)) p = null;
				if (p != null) g.drawString("E", p[0]-g.getStringWidth("E")/2, p[1]);

				loc = new LocationElement(-Constant.PI_OVER_TWO, -pos, 1);
				if (coordinate_system != COORDINATE_SYSTEM.HORIZONTAL) {
					loc = CoordinateSystem.horizontalToEquatorial(loc, projection.ast, projection.obs.getLatitudeRad(), true);
					loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, false, fast, 0);
				}
				p = projection.projectPosition(loc, 0, false);
				if (p != null && !this.isInTheScreen((int)p[0], (int)p[1], npix)) p = null;
				if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) {
					if (p != null) g.drawString("O", p[0]-g.getStringWidth("O")/2, p[1]);
				} else {
					if (p != null) g.drawString("W", p[0]-g.getStringWidth("W")/2, p[1]);
				}

				loc = new LocationElement(Constant.PI_OVER_FOUR, -pos, 1);
				if (coordinate_system != COORDINATE_SYSTEM.HORIZONTAL) {
					loc = CoordinateSystem.horizontalToEquatorial(loc, projection.ast, projection.obs.getLatitudeRad(), true);
					loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, false, fast, 0);
				}
				p = projection.projectPosition(loc, 0, false);
				if (p != null && !this.isInTheScreen((int)p[0], (int)p[1], npix)) p = null;
				if (p != null) g.drawString("NE", p[0]-g.getStringWidth("NE")/2, p[1]);

				loc = new LocationElement(Math.PI-Constant.PI_OVER_FOUR, -pos, 1);
				if (coordinate_system != COORDINATE_SYSTEM.HORIZONTAL) {
					loc = CoordinateSystem.horizontalToEquatorial(loc, projection.ast, projection.obs.getLatitudeRad(), true);
					loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, false, fast, 0);
				}
				p = projection.projectPosition(loc, 0, false);
				if (p != null && !this.isInTheScreen((int)p[0], (int)p[1], npix)) p = null;
				if (p != null) g.drawString("SE", p[0]-g.getStringWidth("SE")/2, p[1]);

				loc = new LocationElement(Math.PI+Constant.PI_OVER_FOUR, -pos, 1);
				if (coordinate_system != COORDINATE_SYSTEM.HORIZONTAL) {
					loc = CoordinateSystem.horizontalToEquatorial(loc, projection.ast, projection.obs.getLatitudeRad(), true);
					loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, false, fast, 0);
				}
				p = projection.projectPosition(loc, 0, false);
				if (p != null && !this.isInTheScreen((int)p[0], (int)p[1], npix)) p = null;
				if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) {
					if (p != null) g.drawString("SO", p[0]-g.getStringWidth("SO")/2, p[1]);
				} else {
					if (p != null) g.drawString("SW", p[0]-g.getStringWidth("SW")/2, p[1]);
				}

				loc = new LocationElement(-Constant.PI_OVER_FOUR, -pos, 1);
				if (coordinate_system != COORDINATE_SYSTEM.HORIZONTAL) {
					loc = CoordinateSystem.horizontalToEquatorial(loc, projection.ast, projection.obs.getLatitudeRad(), true);
					loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, false, fast, 0);
				}
				p = projection.projectPosition(loc, 0, false);
				if (p != null && !this.isInTheScreen((int)p[0], (int)p[1], npix)) p = null;
				if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) {
					if (p != null) g.drawString("NO", p[0]-g.getStringWidth("NO")/2, p[1]);
				} else {
					if (p != null) g.drawString("NW", p[0]-g.getStringWidth("NW")/2, p[1]);
				}

				projection.enableCorrectionOfLocalHorizon();
			}

			if ((!render.drawMilkyWayContours || !render.fillMilkyWay) && render.coordinateSystem == COORDINATE_SYSTEM.HORIZONTAL && render.drawCoordinateGrid)
				return;

			//Object pathAxes = g.generalPathInitialize();
			float[] old_pos = Projection.INVALID_POSITION;
			double j = 0;
			float[] pos;
			//if (projection.obs.getMotherBody() == null || projection.obs.getMotherBody() == TARGET.EARTH) {
				//if (render.drawSkyCorrectingLocalHorizon) // && g == this.g)
				//	j = -projection.getHorizonElevation(); // Commented => draw always astronomical horizon
			//}
			projection.disableCorrectionOfLocalHorizon(); // to draw geometric horizon
			ArrayList<Object> horizonLine = null;
			double step = (5.0 / pixels_per_degree);
			if (step > 0.33) step = 0.33;
			step = ((int) (step * 10.0)) / 10.0;
			if (step < 0.014) step = 0.014;
			if (g.renderingToExternalGraphics()) step /= 4;
			if (lastIncrementHoriz != -1 && lastIncrementHoriz <= step) {
				Object o = null;
				if (db_horizonLine >= 0) {
					o = DataBase.getData(db_horizonLine);
				} else {
					o = DataBase.getData("horizonLine", threadID, true);
				}
				if (o != null) horizonLine = new ArrayList<Object>(Arrays.asList((Object[]) o));
			}
			if (horizonLine == null) {
				horizonLine = new ArrayList<Object>();
				lastIncrementHoriz = step;
				LocationElement loc;
				for (double i = 0; i <= 24+step*2; i = i + step)
				{
					if (i > 24) i = 24;
					loc = new LocationElement(i / Constant.RAD_TO_HOUR, j, 1.0);
					loc = CoordinateSystem.horizontalToEquatorial(loc, projection.ast, obsLat, false); //.horizontalToEquatorial(loc, projection.time, obs, projection.eph);
					if (loc != null && projection.eph.equinox != EphemerisElement.EQUINOX_OF_DATE) {
						EphemElement ephem = new EphemElement();
						ephem.setEquatorialLocation(loc);
						loc = Ephem.toOutputEquinox(ephem, projection.eph, jd).getEquatorialLocation();
					}

					loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, false, false, 0);
					if (loc != null)
						horizonLine.add(new float[] {(float)loc.getLongitude(), (float)loc.getLatitude()});
					if (i == 24) break;
				}

				DataBase.addData("horizonLine", threadID, horizonLine.toArray(), true);
				db_horizonLine = DataBase.getIndex("horizonLine", threadID);
			}
			g.setStroke(render.drawCoordinateGridStroke);
			g.setColor(render.drawCoordinateGridColor, true);

			old_pos = null;
			int npix = 1 + 2 * (int)(step * 15.0 * pixels_per_degree);
			if (npix < 20) npix = 20;
			boolean fast = false;
			if (render.drawFastLinesMode.fastGrid()) fast = true;
			float[] loc;
			for (Iterator<Object> itr = horizonLine.iterator();itr.hasNext();)
			{
				loc = (float[]) itr.next();
				pos = projection.projectPosition(loc, 0, false);
				if (pos != null && !this.isInTheScreen((int)pos[0], (int)pos[1], npix)) pos = null;
				if (!projection.isInvalid(pos))
				{
					if (old_pos != null)  {
						if (render.telescope.invertHorizontal || render.telescope.invertVertical ||
								rec.isLineIntersectingRectangle(old_pos[0], old_pos[1], pos[0], pos[1])) {
							if (fast) {
								g.drawStraightLine(old_pos[0], old_pos[1], pos[0], pos[1]);
							} else {
								g.drawLine(old_pos[0], old_pos[1], pos[0], pos[1], true);
							}
						}
					}
					old_pos = new float[] {pos[0], pos[1]};
				} else {
					old_pos = null;
				}
			}
			projection.enableCorrectionOfLocalHorizon();
		}
	}

	private boolean neverWait = false; //, forceRecalculate = false;
	private boolean neverWaitS = false, neverWaitP = false, neverWaitC = false, neverWaitN = false, neverWaitA = false, neverWaitT = false;
	/**
	 * Returns if some delay is expected to occur in the next rendering.
	 * @return True or false.
	 */
	public boolean willCalculateForAWhile() {

//		if (forceRecalculate) {
//			forceRecalculate = false;
//			return true;
//		}
		if (neverWait || jd < 2451545.0) return false;

		double pixels_per_degree = 0;
		try {
			pixels_per_degree = (float) (render.width / (Constant.RAD_TO_DEG * render.telescope.getField()));
		} catch (JPARSECException e) {}

		boolean will = false;
		Object o = DataBase.getData("asterEphem", threadID, true);
		if (render.drawAsteroids && o == null && !neverWaitA) will = returnWillCalculate(pixels_per_degree);
		if (will) return will;
		o = DataBase.getData("transEphem", threadID, true);
		if (render.drawTransNeptunianObjects && o == null && !neverWaitT && render.drawMinorObjectsLimitingMagnitude >= 14) will = returnWillCalculate(pixels_per_degree);
		if (will) return will;
		o = DataBase.getData("cometEphem", threadID, true);
		if (render.drawComets && o == null && !neverWaitC) will = returnWillCalculate(pixels_per_degree);
		if (will) return will;
		o = DataBase.getData("neoEphem", threadID, true);
		if (render.drawAsteroids && o == null && !neverWaitN) will = returnWillCalculate(pixels_per_degree);
		if (will) return will;
		o = DataBase.getData("probeEphem", threadID, true);
		if (render.drawSpaceProbes && o == null && !neverWaitP) will = returnWillCalculate(pixels_per_degree);
		if (will) return will;
		o = DataBase.getData("satEphem", threadID, true);
		if (render.drawArtificialSatellites && o == null && !neverWaitS) will = returnWillCalculate(pixels_per_degree);
		if (will) return will;

		if (pixels_per_degree >= 10.0) neverWait = true;
		return false;
	}
	private boolean returnWillCalculate(double pixels_per_degree) {
		if (!render.drawClever) return true;
		if (pixels_per_degree >= 10.0) return true;
		return false;
	}
	private float transEphemLastMaglim = -1, cometEphemLastMaglim = -1, asterEphemLastMaglim = -1, neoEphemLastMaglim = -1;
	private void drawTransNeptunianObjects() throws JPARSECException
	{
		if (render.drawTransNeptunianObjects)
		{
			if (neverWaitT || render.drawMinorObjectsLimitingMagnitude < 14 || pixels_per_degree < 10.0 && render.drawClever)
				return;

			g.setColor(render.drawStarsColor, true);
			if (Math.abs(transEphemLastMaglim) < render.drawMinorObjectsLimitingMagnitude) {
				DataBase.addData("transEphem", threadID, null, true);
				db_transEphem = -1;
			}

			boolean calc = true;
			int n = 0;
			EphemElement transEphem[] = null;
			Object o = null;
			if (db_transEphem >= 0) {
				o = DataBase.getData(db_transEphem);
			} else {
				o = DataBase.getData("transEphem", threadID, true);
			}
			if (o != null) transEphem = (EphemElement[]) o;
			if (transEphem == null) {
				transEphem = new EphemElement[0];
				try {
					double jd = TimeScale.getJD(projection.time, projection.obs, projection.eph, SCALE.UNIVERSAL_TIME_UTC);
					AstroDate astro = new AstroDate(jd);
					String p = Configuration.updateTransNeptuniansInTempDir(astro);
					if (p == null) {
						if (!Configuration.isAcceptableDateForTransNeptunians(astro, false)) {
							neverWaitT = true;
							JPARSECException.addWarning("Cannot show accurate positions for transneptunian objects in this date.");
							return;
						} else {
							if (!Configuration.isAcceptableDateForTransNeptunians(astro, true))
								OrbitEphem.setTransNeptuniansFromExternalFile(null);
						}
					}
				} catch (Exception exc) {
					return;
				}

				n = OrbitEphem.getTransNeptuniansCount();
			} else {
				calc = false;
				n = transEphem.length;
			}
			if (n == 0) return;
			float size;
			int b1, b2;
			LocationElement loc;
			String name;
			int index2 = 0;
			if (calc) {
				transEphemLastMaglim = render.drawMinorObjectsLimitingMagnitude;
				EphemerisElement eph = projection.eph.clone();
				eph.targetBody = TARGET.Asteroid;
				eph.algorithm = EphemerisElement.ALGORITHM.ORBIT;

				OrbitalElement orbit[] = OrbitEphem.getOrbitalElementsOfAsteroids();
				if (orbit == null) return;
				TimeElement time = new TimeElement(TimeScale.getJD(projection.time, projection.obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME), SCALE.BARYCENTRIC_DYNAMICAL_TIME);
				n = orbit.length;
				transEphem = new EphemElement[n];
				for (int index = 0; index < n; index++)
				{
					if (orbit[index] == null) continue;
					eph.orbit = orbit[index];

					// Pluto is drawn using the main planetary ephemerides since it gives a more accurate position
					boolean isPluto = false;
					if (orbit[index].name.indexOf("134340") >= 0 && orbit[index].name.toLowerCase().indexOf("pluto") >= 0) isPluto = true;

					if (!isPluto && Math.abs(orbit[index].referenceTime - jd) < Constant.JULIAN_DAYS_PER_CENTURY &&
							orbit[index].getMagnitude(jd) < render.drawMinorObjectsLimitingMagnitude)
					{
						transEphem[index2] = OrbitEphem.orbitEphemeris(time, projection.obs, eph);
						if (transEphem[index2].magnitude > render.drawMinorObjectsLimitingMagnitude) {
							transEphem[index2] = null;
							continue;
						}
						loc = new LocationElement(transEphem[index2].rightAscension, transEphem[index2].declination, 1.0);
						transEphem[index2].setLocation(projection.getApparentLocationInSelectedCoordinateSystem(loc, true, false, transEphem[index2].angularRadius));
						if (transEphem[index2].getLocation() != null) index2 ++;
					}
				}

				// Sort by magnitude
				n = index2;
				transEphem = (EphemElement[]) DataSet.getSubArray(transEphem, 0, index2-1);
				double mag[] = new double[index2];
				for (int i=0; i<index2; i++) {
					mag[i] = transEphem[i].magnitude;
				}
				transEphem = (EphemElement[]) DataSet.sortInCrescent(transEphem, mag);
				if (transEphem == null) transEphem = new EphemElement[0];
				if (index2 == 0) neverWaitT = true;
				report("trans-neptunian objects", transEphem);
				DataBase.addData("transEphem", threadID, transEphem, true);
				db_transEphem = DataBase.getIndex("transEphem", threadID);
			}
			EphemElement ephem;
			ArrayList<Object> minorObjects = null;
			if (SaveObjectsToAllowSearch) {
				minorObjects = new ArrayList<Object>();
				if (db_minorObjects >= 0) {
					o = DataBase.getData(db_minorObjects);
				} else {
					o = DataBase.getData("minorObjects", threadID, true);
				}
				if (o != null) minorObjects = new ArrayList<Object>(Arrays.asList((Object[]) o));
			}
			boolean save = false;
			float position;
			for (int index = 0; index < n; index++)
			{
				ephem = transEphem[index];
				if (ephem != null && ephem.getLocation() != null) {
					// Don't draw object if the observer is inside it
					if (ephem.angularRadius > Constant.PI_OVER_FOUR) continue;

					if (ephem.magnitude <= maglim || ((maglim == render.drawStarsLimitingMagnitude || fieldDeg < 30) && ephem.magnitude <= render.drawMinorObjectsLimitingMagnitude))
					{
						float pos[] = projection.projectPosition(ephem.getLocation(), 0, false);
						if (pos != null && !this.isInTheScreen((int)pos[0], (int)pos[1])) pos = null;

						if (!projection.isInvalid(pos))
						{
							size = getSizeForAGivenMagnitude(ephem.magnitude);
							int tsize = (int) (0.5 + (1.5f * size+adds));
							if (tsize >= 1 && render.drawStarsRealistic != REALISTIC_STARS.NONE &&
									render.drawStarsRealistic != REALISTIC_STARS.NONE_CUTE) {
								this.drawStar(tsize, pos, getDist(refz - ephem.distance), -1, g);
							} else {
								tsize = (int) (2* size+adds);
								if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
									g.fillOval((pos[0] - size), (pos[1] - size), tsize, tsize, this.fast);
								} else {
									g.fillOval((pos[0] - size), (pos[1] - size), tsize, tsize, getDist(refz-ephem.distance));
								}
							}

							if (SaveObjectsToAllowSearch) {
								minorObjects.add(new Object[] {
										RenderSky.OBJECT.TRANSNEPTUNIAN, pos.clone(),
										ephem
								});
								save = true;
							}

							position = 0;
							if (render.drawMinorObjectsLabels)
							{
								name = ephem.name;
								b1 = name.indexOf("(");
								if (b1>=0) {
									b2 = name.indexOf(")");
									name = name.substring(b1+1, b2) + name.substring(b2+1);
								}
								position = Math.max(render.drawMinorObjectsNamesFont.getSize(), 3 * size);
								drawString(render.drawStarsColor, render.drawMinorObjectsNamesFont, name, pos[0], pos[1], -position, false);
							}
			 				if (render.drawMagnitudeLabels && ephem.magnitude < maglim - 2) {
			 					if (position == 0) position = Math.max(size * 3, render.drawMinorObjectsNamesFont.getSize());
								drawString(render.drawStarsColor, render.drawMinorObjectsNamesFont, Functions.formatValue(ephem.magnitude, 1), pos[0], pos[1], -position, false);
			 				}
						}
					} else {
						break;
					}
				} else {
					break;
				}
			}
			if (save && SaveObjectsToAllowSearch) {
				DataBase.addData("minorObjects", threadID, minorObjects.toArray(), true);
				if (db_minorObjects < 0) db_minorObjects = DataBase.getIndex("minorObjects", threadID);
			}
		}
	}

	private void drawAsteroids() throws JPARSECException
	{
		if (render.drawAsteroids)
		{
			if (neverWaitA || pixels_per_degree < 10.0 && render.drawClever && projection.obs.getMotherBody() != TARGET.NOT_A_PLANET)
				return;

			g.setColor(render.drawStarsColor, true);

			if (Math.abs(asterEphemLastMaglim) < render.drawMinorObjectsLimitingMagnitude)
				DataBase.addData("asterEphem", threadID, null, true);

			boolean calc = true;
			int n = 0;
			EphemElement asterEphem[] = null;
			Object o = null;
			if (db_asterEphem >= 0) {
				o = DataBase.getData(db_asterEphem);
			} else {
				o = DataBase.getData("asterEphem", threadID, true);
			}
			if (o != null) asterEphem = (EphemElement[]) o;
			if (asterEphem == null) {
				asterEphem = new EphemElement[0];
				try {
					double jd = TimeScale.getJD(projection.time, projection.obs, projection.eph, SCALE.UNIVERSAL_TIME_UTC);
					AstroDate astro = new AstroDate(jd);
					String p = Configuration.updateAsteroidsInTempDir(astro);
					if (p == null) {
						if (!Configuration.isAcceptableDateForAsteroids(astro, false)) {
							neverWaitA = true;
							JPARSECException.addWarning("Cannot show accurate positions for asteroids in this date.");
							return;
						} else {
							OrbitEphem.setAsteroidsFromExternalFile(null);
						}
					}
				} catch (Exception exc) {
					return;
				}

				n = OrbitEphem.getAsteroidsCount();
			} else {
				calc = false;
				n = asterEphem.length;
			}
			if (n == 0) return;
			float size;
			int b1, b2;
			LocationElement loc;
			String name;
			int index2 = 0;
			if (calc) {
				asterEphemLastMaglim = render.drawMinorObjectsLimitingMagnitude;
				EphemerisElement eph = projection.eph.clone();
				eph.targetBody = TARGET.Asteroid;
				eph.algorithm = EphemerisElement.ALGORITHM.ORBIT;
				OrbitalElement orbit[] = OrbitEphem.getOrbitalElementsOfAsteroids();
				if (orbit == null) return;
				TimeElement time = new TimeElement(TimeScale.getJD(projection.time, projection.obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME), SCALE.BARYCENTRIC_DYNAMICAL_TIME);
				n = orbit.length;
				asterEphem = new EphemElement[n];
				for (int index = 0; index < n; index++)
				{
					if (orbit[index] == null) continue;
					eph.orbit = orbit[index];

					if (Math.abs(orbit[index].referenceTime - jd) < Constant.JULIAN_DAYS_PER_CENTURY &&
							(render.drawMinorObjectsLimitingMagnitude >= 16 || orbit[index].getMagnitude(jd) < render.drawMinorObjectsLimitingMagnitude))
					{
						asterEphem[index2] = OrbitEphem.orbitEphemeris(time, projection.obs, eph);
						if (asterEphem[index2].magnitude > render.drawMinorObjectsLimitingMagnitude) {
							asterEphem[index2] = null;
							continue;
						}
						loc = new LocationElement(asterEphem[index2].rightAscension, asterEphem[index2].declination, 1.0);
						asterEphem[index2].setLocation(projection.getApparentLocationInSelectedCoordinateSystem(loc, true, false, asterEphem[index2].angularRadius));
						if (asterEphem[index2].getLocation() != null) index2 ++;
					}
				}

				// Sort by magnitude
				n = index2;
				asterEphem = (EphemElement[]) DataSet.getSubArray(asterEphem, 0, index2-1);
				double mag[] = new double[index2];
				for (int i=0; i<index2; i++) {
					mag[i] = asterEphem[i].magnitude;
				}
				asterEphem = (EphemElement[]) DataSet.sortInCrescent(asterEphem, mag);
				if (asterEphem == null) asterEphem = new EphemElement[0];
				if (index2 == 0) neverWaitA = true;
				report("asteroids", asterEphem);
				DataBase.addData("asterEphem", threadID, asterEphem, true);
				db_asterEphem = DataBase.getIndex("asterEphem", threadID);
			}
			EphemElement ephem;
			ArrayList<Object> minorObjects = null;
			if (SaveObjectsToAllowSearch) {
				minorObjects = new ArrayList<Object>();
				if (db_minorObjects >= 0) {
					o = DataBase.getData(db_minorObjects);
				} else {
					o = DataBase.getData("minorObjects", threadID, true);
				}
				if (o != null) minorObjects = new ArrayList<Object>(Arrays.asList((Object[]) o));
			}
			boolean save = false;
			float position;
			for (int index = 0; index < n; index++)
			{
				ephem = asterEphem[index];
				if (ephem != null && ephem.getLocation() != null) {
					// Don't draw object if the observer is inside it
					if (ephem.angularRadius > Constant.PI_OVER_FOUR) continue;

					if (ephem.magnitude <= maglim || ((maglim == render.drawStarsLimitingMagnitude || fieldDeg < 30 || projection.obs.getMotherBody() == TARGET.NOT_A_PLANET) && ephem.magnitude <= render.drawMinorObjectsLimitingMagnitude))
					{
						float pos[] = projection.projectPosition(ephem.getLocation(), 0, false);
						if (pos != null && !this.isInTheScreen((int)pos[0], (int)pos[1])) pos = null;

						if (!projection.isInvalid(pos) && !isEclipsedByPlanets(planets, pos, ephem.distance))
						{
							size = getSizeForAGivenMagnitude(ephem.magnitude);
							int tsize = (int) (0.5 + (1.5f * size+adds));
							if (tsize >= 1 && render.drawStarsRealistic != REALISTIC_STARS.NONE &&
									render.drawStarsRealistic != REALISTIC_STARS.NONE_CUTE) {
								this.drawStar(tsize, pos, getDist(refz-ephem.distance), -1, g);
							} else {
								tsize = (int) (2* size+adds);
								if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
									g.fillOval((pos[0] - size), (pos[1] - size), tsize, tsize, this.fast);
								} else {
									g.fillOval((pos[0] - size), (pos[1] - size), tsize, tsize, getDist(refz-ephem.distance));
								}
							}

							if (SaveObjectsToAllowSearch) {
								minorObjects.add(new Object[] {
										RenderSky.OBJECT.ASTEROID, pos.clone(),
										ephem
								});
								save = true;
							}

							position = 0;
							if (render.drawMinorObjectsLabels)
							{
								name = ephem.name;
								b1 = name.indexOf("(");
								if (b1>=0) {
									b2 = name.indexOf(")");
									name = name.substring(b1+1, b2) + name.substring(b2+1);
								}
								position = Math.max(render.drawMinorObjectsNamesFont.getSize(), 3 * size);
								drawString(render.drawStarsColor, render.drawMinorObjectsNamesFont, name, pos[0], pos[1], -position, false);
							}
			 				if (render.drawMagnitudeLabels && ephem.magnitude < maglim - 2) {
			 					if (position == 0) position = Math.max(size * 3, render.drawMinorObjectsNamesFont.getSize());
								drawString(render.drawStarsColor, render.drawMinorObjectsNamesFont, Functions.formatValue(ephem.magnitude, 1), pos[0], pos[1], -position, false);
			 				}
						}
					} else {
						break;
					}
				} else {
					break;
				}
			}
			if (save && SaveObjectsToAllowSearch) {
				DataBase.addData("minorObjects", threadID, minorObjects.toArray(), true);
				if (db_minorObjects < 0) db_minorObjects = DataBase.getIndex("minorObjects", threadID);
			}
		}
	}

	private float brightestComet = -100;
	private void drawComets(boolean drawTail) throws JPARSECException
	{
		if (render.drawComets)
		{
			if (neverWaitC || (pixels_per_degree < 10.0 && render.drawClever && brightestComet > 6.5 && projection.obs.getMotherBody() != TARGET.NOT_A_PLANET))
				return;

			if (render.planetRender.ephemSun == null) {
				EphemerisElement sun_eph = new EphemerisElement(TARGET.SUN, projection.eph.ephemType,
						EphemerisElement.EQUINOX_OF_DATE, projection.eph.isTopocentric, projection.eph.ephemMethod,
						projection.eph.frame, projection.eph.algorithm);
				sun_eph.correctForEOP = false;
				sun_eph.correctForPolarMotion = false;
				render.planetRender.ephemSun = Ephem.getEphemeris(projection.time, projection.obs, sun_eph, false);
			}

			LocationElement loc_sun = new LocationElement(render.planetRender.ephemSun.rightAscension,
					render.planetRender.ephemSun.declination, 1.0);
			int n = 0;
			if (Math.abs(cometEphemLastMaglim) < render.drawMinorObjectsLimitingMagnitude) {
				DataBase.addData("cometEphem", threadID, null, true);
				brightestComet = -100;
			}
			boolean calc = true;
			EphemElement cometEphem[] = null;
			Object o = null;
			if (db_cometEphem >= 0) {
				o = DataBase.getData(db_cometEphem);
			} else {
				o = DataBase.getData("cometEphem", threadID, true);
			}
			if (o != null) cometEphem = (EphemElement[]) o;
			if (cometEphem == null) {
				cometEphem = new EphemElement[0];
				double jd = TimeScale.getJD(projection.time, projection.obs, projection.eph, SCALE.UNIVERSAL_TIME_UTC);
				AstroDate astro = new AstroDate(jd);
				try {
					OrbitEphem.setCometsFromExternalFile(null);
					String p = Configuration.updateCometsInTempDir(astro);
					if (p == null) {
						if (!Configuration.isAcceptableDateForComets(astro, false)) {
							ArrayList<OrbitalElement> oldComets = ReadFile.readFileOfOldComets(astro, 10);
							if (oldComets == null) {
								neverWaitC = true;
								JPARSECException.addWarning("Cannot show accurate positions for recent or historical comets in this date.");
								return;
							} else {
								OrbitEphem.setCometsFromElements(oldComets);
								JPARSECException.addWarning("Using file of old historical comets.");
							}
						} else {
							OrbitEphem.setCometsFromExternalFile(null);
						}
					}
				} catch (Exception exc) {
					if (Configuration.isAcceptableDateForComets(astro, false)) {
						OrbitEphem.setCometsFromExternalFile(null);
					} else {
						if (!g.renderingToAndroid()) exc.printStackTrace();
						return;
					}
				}

				n = OrbitEphem.getCometsCount();
			} else {
				n = cometEphem.length;
				calc = false;
			}
			if (n == 0) return;
			double cte = 2.0E7 / Constant.AU;
			LocationElement loc;
			float size;
			double tail;
			String name;
			int index2 = 0;
			if (calc) {
				cometEphemLastMaglim = render.drawMinorObjectsLimitingMagnitude;
				EphemerisElement eph = projection.eph.clone();
				eph.targetBody = TARGET.Comet;
				eph.algorithm = EphemerisElement.ALGORITHM.ORBIT;

				OrbitalElement orbit[] = OrbitEphem.getOrbitalElementsOfComets();
				if (orbit == null) return;
				TimeElement time0 = new TimeElement(TimeScale.getJD(projection.time, projection.obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME), SCALE.BARYCENTRIC_DYNAMICAL_TIME);
				brightestComet = -100;
				n = orbit.length;
				cometEphem = new EphemElement[n];
				for (int index = 0; index < n; index++)
				{
					if (orbit[index] == null) continue;
					eph.orbit = orbit[index];
					if (Math.abs(orbit[index].referenceTime - jd) < Constant.JULIAN_DAYS_PER_CENTURY &&
							orbit[index].getMagnitude(jd) < render.drawMinorObjectsLimitingMagnitude)
					{
						cometEphem[index2] = OrbitEphem.orbitEphemeris(time0, projection.obs, eph);
						if (cometEphem[index2].magnitude > render.drawMinorObjectsLimitingMagnitude) {
							cometEphem[index2] = null;
							continue;
						}
						loc = new LocationElement(cometEphem[index2].rightAscension, cometEphem[index2].declination, 1.0);
						cometEphem[index2].setLocation(projection.getApparentLocationInSelectedCoordinateSystem(loc, true, false, cometEphem[index2].angularRadius));
						if (cometEphem[index2].magnitude < brightestComet || brightestComet == -100) brightestComet = cometEphem[index2].magnitude;

						if (cometEphem[index2].getLocation() != null) {
							// Calculate position after 3 days to render the dust tail
							TimeElement time = time0.clone();
							time.add(3);
							EphemElement ephemPlus = OrbitEphem.orbitEphemeris(time, projection.obs, eph);
							ephemPlus.setLocation(projection.getApparentLocationInSelectedCoordinateSystem(ephemPlus.getEquatorialLocation(), false, false, ephemPlus.angularRadius));
							cometEphem[index2].brightLimbAngle = (float) (LocationElement.getPositionAngle(cometEphem[index2].getEquatorialLocation(), ephemPlus.getEquatorialLocation()));

							index2 ++;
						}
					}
				}

				// Sort by magnitude
				n = index2;
				cometEphem = (EphemElement[]) DataSet.getSubArray(cometEphem, 0, index2-1);
				if (index2 > 1) {
					double mag[] = new double[index2];
					for (int i=0; i<index2; i++) {
						mag[i] = cometEphem[i].magnitude;
					}
					cometEphem = (EphemElement[]) DataSet.sortInCrescent(cometEphem, mag);
				}
				if (cometEphem == null) cometEphem = new EphemElement[0];
				if (index2 == 0) neverWaitC = true;
				report("comets", cometEphem);
				DataBase.addData("cometEphem", threadID, cometEphem, true);
				db_cometEphem = DataBase.getIndex("cometEphem", threadID);
			}
			EphemElement ephem;
			ArrayList<Object> minorObjects = null;
			if (SaveObjectsToAllowSearch && drawTail) {
				minorObjects = new ArrayList<Object>();
				if (db_minorObjects >= 0) {
					o = DataBase.getData(db_minorObjects);
				} else {
					o = DataBase.getData("minorObjects", threadID, true);
				}
				if (o != null) minorObjects = new ArrayList<Object>(Arrays.asList((Object[]) o));
			}
			boolean save = false;
			float position;
			for (int index = 0; index < n; index++)
			{
				ephem = cometEphem[index];
				if (ephem != null && ephem.getLocation() != null) {
					// Don't draw object if the observer is inside it
					if (ephem.angularRadius > Constant.PI_OVER_FOUR) continue;

					if (ephem.magnitude <= maglim || ((maglim == render.drawStarsLimitingMagnitude || fieldDeg < 30 || projection.obs.getMotherBody() == TARGET.NOT_A_PLANET) && ephem.magnitude <= render.drawMinorObjectsLimitingMagnitude))
					{
						float pos[] = projection.projectPosition(ephem.getLocation(), 0, false);
						tail = (pixels_per_radian * FastMath.atan(cte / ephem.distance) * FastMath.sin(ephem.elongation));
						if (pos != null && !this.isInTheScreen((int)pos[0], (int)pos[1], (int)tail)) pos = null;

						if (!projection.isInvalid(pos) && !isEclipsedByPlanets(planets, pos, ephem.distance))
						{
							//size = (int) (0.5 + pixels_per_radian * ephem.angularRadius);
							float dist = getDist(refz-ephem.distance);

							if (drawTail && render.projection != PROJECTION.POLAR) 
								drawCometTail(g, pos, loc_sun, ephem);

							// Draw comet position
							size = getSizeForAGivenMagnitude(ephem.magnitude);
							g.setColor(render.drawStarsColor, true);
							int tsize = (int) (0.5 + (1.5f * size+adds));
							if (tsize > 1 && render.drawStarsRealistic != REALISTIC_STARS.NONE &&
									render.drawStarsRealistic != REALISTIC_STARS.NONE_CUTE) {
								this.drawStar(tsize, pos, dist, -1, g);
							} else {
								tsize = (int) (2* size+adds);
								if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
									g.fillOval((pos[0] - size), (pos[1] - size), tsize, tsize, this.fast);
								} else {
									g.fillOval((pos[0] - size), (pos[1] - size), tsize, tsize, dist);
								}
							}

							if (SaveObjectsToAllowSearch && drawTail) {
								minorObjects.add(new Object[] {
										RenderSky.OBJECT.COMET, pos.clone(),
										ephem
								});
								save = true;
							}

							position = 0;
							if (render.drawMinorObjectsLabels && drawTail)
							{
								name = ephem.name;
								int b1 = name.indexOf("  ");
								if (b1 > 0) name = name.substring(0, b1).trim();
								position = Math.max(3 * size, tsize+render.drawMinorObjectsNamesFont.getSize());
								drawString(render.drawStarsColor, render.drawMinorObjectsNamesFont, name, pos[0], pos[1], -position, false);
							}
			 				if (render.drawMagnitudeLabels && drawTail && ephem.magnitude < maglim - 2) {
			 					if (position == 0) position = Math.max(3 * size, tsize+render.drawMinorObjectsNamesFont.getSize());
								drawString(render.drawStarsColor, render.drawMinorObjectsNamesFont, Functions.formatValue(ephem.magnitude, 1), pos[0], pos[1], -position, false);
			 				}
						}
					} else {
						break;
					}
				} else {
					break;
				}
			}
			if (save && SaveObjectsToAllowSearch && drawTail) {
				DataBase.addData("minorObjects", threadID, minorObjects.toArray(), true);
				if (db_minorObjects < 0) db_minorObjects = DataBase.getIndex("minorObjects", threadID);
			}
		}
	}

	private void drawCometTail(Graphics g, float pos[], LocationElement loc_sun, EphemElement ephem) throws JPARSECException {
		double cte = 2.0E7 / Constant.AU;
		double cte2 = Math.PI / 12.0;

		double tail = (pixels_per_radian * FastMath.atan(cte / ephem.distance) * FastMath.sin(ephem.elongation));
		float size = (int) (0.5 + pixels_per_radian * ephem.angularRadius);
		float dist = getDist(refz-ephem.distance);

		// Draw comet tail supposing 20 000 000 km in
		// size, and x degrees aperture
		if ((tail - size) > 1)
		{
			// Ionic tail
			g.setColor(0, 32, 96, 100);
			double angle = LocationElement.getPositionAngle(ephem.getEquatorialLocation(), loc_sun);
			//double plus = projection.getNorthAngleAt(ephem.getEquatorialLocation(), true, false);
			double plus = projection.getNorthAngleAt(ephem.getLocation(), false, true);
			angle += plus;
			int step = (int) (Math.sqrt(tail * cte2));
			if (step < 1) step = 1;
			if (tail/step > 30) step = (int) (tail/30);
			g.setStroke(JPARSECStroke.STROKE_DEFAULT_LINE); //.STROKE_POINTS_LOW_SPACE);
			if (projection.obs.getMotherBody() != TARGET.SUN) {
				for (int i = 0; i < tail; i = i + step*3)
				{
					double ang = angle + (i * 2.0 / tail - 1.0) * cte2 * 0.33;
					float pxf = pos[0] + (float) (tail * FastMath.cos(ang));
					float pyf = pos[1] + (float) (tail * FastMath.sin(ang));
					if (pixels_per_degree < 100 || tail < 1500) {
						if (g.renderingToExternalGraphics()) {
							g.drawLine(pos[0], pos[1], pxf, pyf, dist, dist);
						} else {
							if (g.getAnaglyphMode().is3D() || !render.drawFastLinesMode.fastGrid()) {
								g.drawLine((int)pos[0], (int)pos[1], (int)pxf, (int)pyf, dist, dist);
							} else {
								g.drawLine((int)pos[0], (int)pos[1], (int)pxf, (int)pyf, true);
							}
						}
					} else {
						if (g.renderingToExternalGraphics()) {
							g.drawStraightLine(pos[0], pos[1], pxf, pyf, dist, dist);
						} else {
							if (g.getAnaglyphMode().is3D() || !render.drawFastLinesMode.fastGrid()) {
								g.drawStraightLine((int)pos[0], (int)pos[1], (int)pxf, (int)pyf, dist, dist);
							} else {
								g.drawLine((int)pos[0], (int)pos[1], (int)pxf, (int)pyf, true);
							}
						}
					}
				}
			}

			// Dust tail
			g.setColor(128, 128, 128, 100);
			if (render.getColorMode() == SkyRenderElement.COLOR_MODE.NIGHT_MODE) g.setColor(255, 128, 128, 60);

			// Tails is dragged by Sun's wind, so apparent
			// tail will be something intermediate between
			// the trajectory of the comet and the ionic tail
			angle -= plus;
			double delta = Functions.normalizeRadians(ephem.brightLimbAngle-angle);
			if (delta > Math.PI) delta -= Constant.TWO_PI;
			angle -= delta * 0.25;

			angle += plus;
			for (int i = 0; i < tail; i = i + step)
			{
				double ang = angle + (i * 2.0 / tail - 1.0) * cte2;
				float pxf = pos[0] + (float) (tail * FastMath.cos(ang));
				float pyf = pos[1] + (float) (tail * FastMath.sin(ang));
				if (pixels_per_degree < 100 || tail < 1500) {
					if (g.renderingToExternalGraphics()) {
						g.drawLine(pos[0], pos[1], pxf, pyf, dist, dist);
					} else {
						if (g.getAnaglyphMode().is3D() || !render.drawFastLinesMode.fastGrid()) {
							g.drawLine((int)pos[0], (int)pos[1], (int)pxf, (int)pyf, dist, dist);
						} else {
							g.drawLine((int)pos[0], (int)pos[1], (int)pxf, (int)pyf, true);
						}
					}
				} else {
					if (g.renderingToExternalGraphics()) {
						g.drawStraightLine(pos[0], pos[1], pxf, pyf, dist, dist);
					} else {
						if (g.getAnaglyphMode().is3D() || !render.drawFastLinesMode.fastGrid()) {
							g.drawStraightLine((int)pos[0], (int)pos[1], (int)pxf, (int)pyf, dist, dist);
						} else {
							g.drawStraightLine((int)pos[0], (int)pos[1], (int)pxf, (int)pyf);
						}
					}
				}
			}

			g.setStroke(JPARSECStroke.STROKE_DEFAULT_LINE);
		}
	}

	private void drawNEOs() throws JPARSECException
	{
		if (render.drawAsteroids)
		{
			if (neverWaitN || (pixels_per_degree < 10.0 && render.drawClever))
				return;

			int n = 0;
			if (Math.abs(neoEphemLastMaglim) < render.drawMinorObjectsLimitingMagnitude) {
				DataBase.addData("neoEphem", threadID, null, true);
			}
			boolean calc = true;
			EphemElement cometEphem[] = null;
			Object o = null;
			if (db_neoEphem >= 0) {
				o = DataBase.getData(db_neoEphem);
			} else {
				o = DataBase.getData("neoEphem", threadID, true);
			}
			if (o != null) cometEphem = (EphemElement[]) o;
			if (cometEphem != null) {
				n = cometEphem.length;
				calc = false;
			}
			LocationElement loc;
			float size;
			String name;
			int index2 = 0;
			if (calc) {
				neoEphemLastMaglim = render.drawMinorObjectsLimitingMagnitude;
				EphemerisElement eph = projection.eph.clone();
				eph.targetBody = TARGET.Comet;
				eph.algorithm = EphemerisElement.ALGORITHM.ORBIT;

				ReadFile re = new ReadFile(FORMAT.MPC, OrbitEphem.PATH_TO_MPC_NEOs_FILE);
				try {
					re.readFileOfNEOs(jd, 365);
				} catch (Exception exc) {
					neverWaitN = true;
					return;
				}
				n = re.getNumberOfObjects();
				if (n == 0) {
					neverWaitN = true;
					return;
				}

				OrbitalElement orbit[] = (OrbitalElement[]) re.getReadElements();
				if (orbit == null) return;
				n = orbit.length;
				cometEphem = new EphemElement[n];
				TimeElement time = new TimeElement(TimeScale.getJD(projection.time, projection.obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME), SCALE.BARYCENTRIC_DYNAMICAL_TIME);
				for (int index = 0; index < n; index++)
				{
					if (orbit[index] == null) continue;
					eph.orbit = orbit[index];
					if (Math.abs(eph.orbit.referenceTime - jd) < Constant.JULIAN_DAYS_PER_CENTURY &&
							eph.orbit.getMagnitude(jd) < render.drawMinorObjectsLimitingMagnitude)
					{
						cometEphem[index2] = OrbitEphem.orbitEphemeris(time, projection.obs, eph);
						if (cometEphem[index2].magnitude > render.drawMinorObjectsLimitingMagnitude) {
							cometEphem[index2] = null;
							continue;
						}
						loc = new LocationElement(cometEphem[index2].rightAscension, cometEphem[index2].declination, 1.0);
						cometEphem[index2].setLocation(projection.getApparentLocationInSelectedCoordinateSystem(loc, true, false, cometEphem[index2].angularRadius));
						if (cometEphem[index2].magnitude < brightestComet || brightestComet == -100) brightestComet = cometEphem[index2].magnitude;
						index2 ++;
					}
				}

				// Sort by magnitude
				n = index2;
				cometEphem = (EphemElement[]) DataSet.getSubArray(cometEphem, 0, index2-1);
				double mag[] = new double[index2];
				for (int i=0; i<index2; i++) {
					mag[i] = cometEphem[i].magnitude;
				}
				cometEphem = (EphemElement[]) DataSet.sortInCrescent(cometEphem, mag);
				if (cometEphem == null) cometEphem = new EphemElement[0];
				if (index2 == 0) neverWaitN = true;
				report("NEOs", cometEphem);
				DataBase.addData("neoEphem", threadID, cometEphem, true);
				db_neoEphem = DataBase.getIndex("neoEphem", threadID);
			}
			EphemElement ephem;
			ArrayList<Object> minorObjects = null;
			if (SaveObjectsToAllowSearch) {
				minorObjects = new ArrayList<Object>();
				if (db_minorObjects >= 0) {
					o = DataBase.getData(db_minorObjects);
				} else {
					o = DataBase.getData("minorObjects", threadID, true);
				}
				if (o != null) minorObjects = new ArrayList<Object>(Arrays.asList((Object[]) o));
			}
			boolean save = false;
			g.setColor(render.drawStarsColor, true);
			float position;
			for (int index = 0; index < n; index++)
			{
				ephem = cometEphem[index];
				if (ephem != null && ephem.getLocation() != null) {
					// Don't draw object if the observer is inside it
					if (ephem.angularRadius > Constant.PI_OVER_FOUR) continue;

					if (ephem.magnitude <= maglim || ((maglim == render.drawStarsLimitingMagnitude || fieldDeg < 30) && ephem.magnitude <= render.drawMinorObjectsLimitingMagnitude))
					{
						float pos[] = projection.projectPosition(ephem.getLocation(), 0, false);
						if (pos != null && !this.isInTheScreen((int)pos[0], (int)pos[1])) pos = null;

						if (!projection.isInvalid(pos) && !isEclipsedByPlanets(planets, pos, ephem.distance))
						{
							//size = (int) (0.5 + pixels_per_radian * ephem.angularRadius);
							float dist = getDist(refz-ephem.distance);

							size = getSizeForAGivenMagnitude(ephem.magnitude);
							int tsize = (int) (0.5 + (1.5f * size+adds));
							if (tsize >= 1 && render.drawStarsRealistic != REALISTIC_STARS.NONE &&
									render.drawStarsRealistic != REALISTIC_STARS.NONE_CUTE) {
								this.drawStar(tsize, pos, dist, -1, g);
							} else {
								tsize = (int) (2* size+adds);
								if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
									g.fillOval((pos[0] - size), (pos[1] - size), tsize, tsize, this.fast);
								} else {
									g.fillOval((pos[0] - size), (pos[1] - size), tsize, tsize, dist);
								}
							}

							if (SaveObjectsToAllowSearch) {
								minorObjects.add(new Object[] {
										RenderSky.OBJECT.NEO, pos.clone(),
										ephem
								});
								save = true;
							}

							position = 0;
							if (render.drawMinorObjectsLabels)
							{
								name = ephem.name;
								int b1 = name.indexOf("  ");
								if (b1 > 0) name = name.substring(0, b1).trim();
								position = Math.max(3 * size, render.drawMinorObjectsNamesFont.getSize());
								drawString(render.drawStarsColor, render.drawMinorObjectsNamesFont, name, pos[0], pos[1], -position, false);
							}
			 				if (render.drawMagnitudeLabels && ephem.magnitude < maglim - 2) {
			 					if (position == 0) position = Math.max(3 * size, render.drawMinorObjectsNamesFont.getSize());
								drawString(render.drawStarsColor, render.drawMinorObjectsNamesFont, Functions.formatValue(ephem.magnitude, 1), pos[0], pos[1], -position, false);
			 				}
						}
					} else {
						break;
					}
				} else {
					break;
				}
			}
			if (save && SaveObjectsToAllowSearch) {
				DataBase.addData("minorObjects", threadID, minorObjects.toArray(), true);
				if (db_minorObjects < 0) db_minorObjects = DataBase.getIndex("minorObjects", threadID);
			}
		}
	}

	private void drawProbes() throws Exception
	{
		if (render.drawSpaceProbes)
		{
			if (neverWaitP || pixels_per_degree < 10.0 && render.drawClever && projection.obs.getMotherBody() != TARGET.NOT_A_PLANET)
				return;

			g.setColor(render.drawStarsColor, true);

			EphemElement probeEphem[] = null;
			Object o = null;
			if (db_probeEphem >= 0) {
				o = DataBase.getData(db_probeEphem);
			} else {
				o = DataBase.getData("probeEphem", threadID, true);
			}
			if (o != null) probeEphem = (EphemElement[]) o;
			int index2 = 0;
			if (probeEphem == null) {
				EphemerisElement eph = projection.eph.clone();
				eph.targetBody = TARGET.NOT_A_PLANET;
				eph.algorithm = EphemerisElement.ALGORITHM.PROBE;
				int rs = Spacecraft.getNumberOfProbes();
				probeEphem = new EphemElement[rs];
				for (int index = 0; index < rs; index++)
				{
					eph.orbit = Spacecraft.getProbeElement(index);
					if (eph.orbit == null) continue;
					boolean isApplicable = Spacecraft.isTimeApplicable(projection.time, projection.obs, eph);
					if (isApplicable)
					{
						probeEphem[index2] = Spacecraft.orbitEphemeris(projection.time, projection.obs, eph);
						LocationElement loc = new LocationElement(probeEphem[index2].rightAscension, probeEphem[index2].declination, 1.0);
						loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, true, false, probeEphem[index2].angularRadius);
						if (loc != null) {
							loc.setRadius(index);
							probeEphem[index2].setLocation(loc);
							index2 ++;
						}
					}
				}
				if (index2 == 0) neverWaitP = true;
				report("space probes", probeEphem);
				DataBase.addData("probeEphem", threadID, probeEphem, true);
				db_probeEphem = DataBase.getIndex("probeEphem", threadID);
			}
			EphemElement ephem;
			ArrayList<Object> minorObjects = null;
			if (SaveObjectsToAllowSearch) {
				minorObjects = new ArrayList<Object>();
				if (db_minorObjects >= 0) {
					o = DataBase.getData(db_minorObjects);
				} else {
					o = DataBase.getData("minorObjects", threadID, true);
				}
				if (o != null) minorObjects = new ArrayList<Object>(Arrays.asList((Object[]) o));
			}
			int rs = Spacecraft.getNumberOfProbes();
			boolean save = false;
			for (int index = 0; index < rs; index++)
			{
				ephem = probeEphem[index];
				if (ephem != null && ephem.getLocation() != null) {
					// Don't draw object if the observer is inside it
					if (ephem.angularRadius > Constant.PI_OVER_FOUR) continue;

					float pos[] = projection.projectPosition(ephem.getLocation(), 0, false);
					if (pos != null && !this.isInTheScreen((int)pos[0], (int)pos[1])) pos = null;

					if (!projection.isInvalid(pos) && !isEclipsedByPlanets(planets, pos, ephem.distance))
					{
						String icon = "sat.png";
						if (Spacecraft.getName(index).toLowerCase().indexOf("pioneer") >= 0)
							icon = "pioneer.png";
						if (Spacecraft.getName(index).toLowerCase().indexOf("voyager") >= 0)
							icon = "voyager.png";
						if (Spacecraft.getName(index).toLowerCase().indexOf("galileo") >= 0)
							icon = "galileo.png";
						drawIcon(icon, pos[0], pos[1], 0.0f, 1.0f);

						if (SaveObjectsToAllowSearch) {
							minorObjects.add(new Object[] {
									RenderSky.OBJECT.PROBE, pos.clone(),
									ephem
							});
							save = true;
						}

						if (render.drawMinorObjectsLabels)
						{
							drawString(render.drawStarsColor, render.drawMinorObjectsNamesFont, Spacecraft.getName((int) ephem.getLocation().getRadius()), pos[0], pos[1], -Math.max(15, render.drawMinorObjectsNamesFont.getSize()), false);
						}
					}
				} else {
					break;
				}
			}
			if (save && SaveObjectsToAllowSearch) {
				DataBase.addData("minorObjects", threadID, minorObjects.toArray(), true);
				if (db_minorObjects < 0) db_minorObjects = DataBase.getIndex("minorObjects", threadID);
			}
		}
	}

	private void report(String id, EphemElement[] ephem) {
		if (ReportEphemToConsole && projection.obs.getMotherBody() != TARGET.NOT_A_PLANET) {
			System.out.println();
			if (ephem.length == 0) {
				System.out.println("No "+id+" found for JD = "+jd+" (TDB)");
			} else {
				System.out.println("Calculated new ephemerides for "+id+" on JD = "+jd+" (TDB). RA, DEC, mag:");
				String sep = "   ";
				for (int i=0; i<ephem.length; i++) {
					if (ephem[i] != null && ephem[i].getLocation() != null)
						System.out.println(FileIO.addSpacesAfterAString(ephem[i].name, 20)+sep+Functions.formatRA(ephem[i].rightAscension, 1)+sep+Functions.formatDEC(ephem[i].declination, 0)+sep+Functions.formatValue(ephem[i].magnitude, 1));
				}
			}
		}
	}

	private void drawArtificialSatellites() throws Exception
	{
		if (render.drawArtificialSatellites)
		{
			if (projection.obs.getMotherBody() != TARGET.EARTH || neverWaitS || pixels_per_degree < 5.0 && render.drawClever && render.drawArtificialSatellitesOnlyThese == null)
				return;

			g.setColor(render.drawStarsColor, true);

			boolean calc = false;
			SatelliteEphemElement satEphem[] = null;
			ArrayList<Object[]> iridiumFlares = null;
			Object o = null;
			if (db_satEphem >= 0) {
				o = DataBase.getData(db_satEphem);
			} else {
				o = DataBase.getData("satEphem", threadID, true);
			}
			if (DataBase.dataExists("satFlares", threadID, true)) iridiumFlares = (ArrayList<Object[]>) DataBase.getData("satFlares", threadID, true);
			if (o != null) satEphem = (SatelliteEphemElement[]) o;
			if (satEphem == null) {
				SatelliteEphem.USE_IRIDIUM_SATELLITES = false;
				satEphem = new SatelliteEphemElement[0];
				try {
					double jd = TimeScale.getJD(projection.time, projection.obs, projection.eph, SCALE.UNIVERSAL_TIME_UTC);
					AstroDate astro = new AstroDate(jd);
					if (!Configuration.isAcceptableDateForArtificialSatellites(astro)) {
						String p = Configuration.updateArtificialSatellitesInTempDir(astro);
						if (p == null) {
							neverWaitS = true;
							JPARSECException.addWarning("Cannot show accurate positions for artificial satellites in this date.");
							return;
						}
					}
	 			} catch (Exception exc) {
	 				return;
	 			}
				calc = true;
			}
			int yearNow = projection.time.astroDate.getYear();
			boolean enoughBright = true;
			float size;
			int index2 = 0;
			if (calc) {
				EphemerisElement eph = projection.eph.clone();
				eph.algorithm = EphemerisElement.ALGORITHM.ARTIFICIAL_SATELLITE;
				if (g.renderingToAndroid()) eph.optimizeForSpeed();
				String listSat[] = null;
				if (render.drawArtificialSatellitesOnlyThese != null) listSat = DataSet.toStringArray(render.drawArtificialSatellitesOnlyThese, ",");
				int rs = SatelliteEphem.getArtificialSatelliteCount();
				satEphem = new SatelliteEphemElement[rs];
				for (int index = 0; index < rs; index++)
				{
					SatelliteOrbitalElement satorb = null;
					try { satorb = SatelliteEphem.getArtificialSatelliteOrbitalElement(index);
					} catch (Exception exc) { continue; }
					if (satorb == null) continue;
					boolean ok = true;
					if (Math.abs(satorb.year-yearNow) > 1) ok = false;
					if (ok) {
						String sname = satorb.name;
						if (sname.indexOf("(") > 0) sname = sname.substring(0, sname.indexOf("(")).trim();
						if (listSat != null && DataSet.getIndexStartingWith(listSat, sname) < 0) ok = false;
					}
					if (ok) {
						eph.targetBody = TARGET.NOT_A_PLANET;
						eph.targetBody.setIndex(index);
						satEphem[index2] = SDP4_SGP4.satEphemeris(projection.time, projection.obs, eph, false); //SatelliteEphem.satEphemeris(projection.time, projection.obs, eph, false);
						if (satEphem[index2].magnitude != SatelliteEphemElement.UNKNOWN_MAGNITUDE &&
								satEphem[index2].magnitude > render.drawMinorObjectsLimitingMagnitude) {
							satEphem[index2] = null;
							continue;
						}
						LocationElement loc = new LocationElement(satEphem[index2].rightAscension, satEphem[index2].declination, 1.0);
						loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, true, false, satEphem[index2].angularRadius);
						if (loc != null) {
							loc.setRadius(index);
							satEphem[index2].setLocation(loc);
							satEphem[index2].name = satorb.name;
							index2 ++;
						}
					}
				}

				// Sort by magnitude
				satEphem = (SatelliteEphemElement[]) DataSet.getSubArray(satEphem, 0, index2-1);
				double mag[] = new double[index2];
				for (int i=0; i<index2; i++) {
					mag[i] = satEphem[i].magnitude;
					if (mag[i] == SatelliteEphemElement.UNKNOWN_MAGNITUDE) mag[i] = 0;
				}
				satEphem = (SatelliteEphemElement[]) DataSet.sortInCrescent(satEphem, mag);

				DataBase.addData("satEphem", threadID, satEphem, true);
				db_satEphem = DataBase.getIndex("satEphem", threadID);

				// Add trajectories for Iridium flares
				DataBase.addData("satFlares", threadID, null, true);
				if (render.drawArtificialSatellitesIridiumFlares) {
					SatelliteEphem.USE_IRIDIUM_SATELLITES = true;
					SatelliteEphem.setSatellitesFromExternalFile(null);
					try {
						double jd = TimeScale.getJD(projection.time, projection.obs, projection.eph, SCALE.UNIVERSAL_TIME_UTC);
						AstroDate astro = new AstroDate(jd);
						boolean iridium = true;
						if (!Configuration.isAcceptableDateForArtificialSatellites(astro)) {
							String p = Configuration.updateArtificialSatellitesInTempDir(astro);
							if (p == null) iridium = false;
						}
						if (iridium) {
							rs = SatelliteEphem.getArtificialSatelliteCount();
							double dt = 10 / 1440.0;
							TimeElement time = projection.time.clone();
							time.add(-dt);
							iridiumFlares = new ArrayList<Object[]>();
							for (int index = 0; index < rs; index++)
							{
								SatelliteOrbitalElement sat = SatelliteEphem.getArtificialSatelliteOrbitalElement(index);
								ArrayList<Object[]> events = SDP4_SGP4.getNextIridiumFlares(time, projection.obs, eph, sat, Constant.DEG_TO_RAD, dt * 2, true, 5);
								if (events != null && events.size() > 0) {
									for (int i=0; i<events.size(); i++) {
										Object data[] = events.get(i);

										SatelliteEphemElement satEphemObj4 = (SatelliteEphemElement) data[4];
										LocationElement loc4 = projection.getApparentLocationInSelectedCoordinateSystem(satEphemObj4.getEquatorialLocation(), true, false, 0);
										if (loc4 != null) {
											loc4.setRadius(index);
											satEphemObj4.setLocation(loc4);
											satEphemObj4.name = sat.name;
										}
										SatelliteEphemElement satEphemObj5 = (SatelliteEphemElement) data[5];
										LocationElement loc5 = projection.getApparentLocationInSelectedCoordinateSystem(satEphemObj5.getEquatorialLocation(), true, false, 0);
										if (loc5 != null) {
											loc5.setRadius(index);
											satEphemObj5.setLocation(loc5);
											satEphemObj5.name = sat.name;
										}
										SatelliteEphemElement satEphemObj6 = (SatelliteEphemElement) data[6];
										LocationElement loc6 = projection.getApparentLocationInSelectedCoordinateSystem(satEphemObj6.getEquatorialLocation(), true, false, 0);
										if (loc6 != null) {
											loc6.setRadius(index);
											satEphemObj6.setLocation(loc6);
											satEphemObj6.name = sat.name;
										}

										iridiumFlares.add(data);
									}
								}
							}
							if (iridiumFlares.size() > 0)
								DataBase.addData("satFlares", threadID, iridiumFlares, true);
						}
		 			} catch (Exception exc) {
		 			}
					SatelliteEphem.USE_IRIDIUM_SATELLITES = false;
					SatelliteEphem.setSatellitesFromExternalFile(null);
				}
			}
			SatelliteEphemElement ephem;
			ArrayList<Object> minorObjects = null;
			if (SaveObjectsToAllowSearch) {
				minorObjects = new ArrayList<Object>();
				if (db_minorObjects >= 0) {
					o = DataBase.getData(db_minorObjects);
				} else {
					o = DataBase.getData("minorObjects", threadID, true);
				}
				if (o != null) minorObjects = new ArrayList<Object>(Arrays.asList((Object[]) o));
			}
			boolean save = false;
			for (int index = 0; index < satEphem.length; index++)
			{
				ephem = satEphem[index];
				enoughBright = true;
				if (ephem != null && ephem.magnitude != SatelliteEphemElement.UNKNOWN_MAGNITUDE &&
						ephem.magnitude > render.drawMinorObjectsLimitingMagnitude) break; //enoughBright = false;
				if (ephem != null && !ephem.isEclipsed && enoughBright && ephem.getLocation() != null) {
					// Don't draw object if the observer is inside it
					if (ephem.angularRadius != SatelliteEphemElement.UNKNOWN_ANGULAR_SIZE && 
							ephem.angularRadius > Constant.PI_OVER_FOUR) continue;

					float pos[] = projection.projectPosition(ephem.getLocation(), 0, false);
					if (pos != null && !this.isInTheScreen((int)pos[0], (int)pos[1])) pos = null;

					if (!projection.isInvalid(pos) && !ephem.isEclipsed && ephem.revolutionsCompleted > 0)
					{
						size = getSizeForAGivenMagnitude(1.0);
						String icon = "sat.png";
						if (ephem.name.toLowerCase().indexOf("hst") >= 0)
							icon = "hst.png";
						if (ephem.name.toLowerCase().indexOf("iss") >= 0)
							icon = "iss.png";
						if (ephem.name.toLowerCase().indexOf("tiangong") >= 0)
							icon = "tiangong1.png";
						drawIcon(icon, pos[0], pos[1], 0.0f, 1.0f);

						if (SaveObjectsToAllowSearch) {
							minorObjects.add(new Object[] {
									RenderSky.OBJECT.ARTIFICIAL_SATELLITE, pos.clone(),
									ephem
							});
							save = true;
						}

						if (render.drawMinorObjectsLabels)
						{
							drawString(render.drawStarsColor, render.drawMinorObjectsNamesFont, ephem.name, pos[0], pos[1], -(18+render.drawMinorObjectsNamesFont.getSize()), false);
						}
					}
				} else {
					if (ephem == null || ephem.getLocation() == null) break;
				}
			}

			if (iridiumFlares != null && iridiumFlares.size() > 0) {
				/*
				Julian day of the beggining of the next flare in local time,
				the Julian day of the ending time of the flare,
				the Julian day of the maximum of the flare,
				and the minimum iridium angle as fourth value.
				The fifth, sixth, and seventh values will be respectivelly the SatelliteEphemElement object for the start, end, and maximum times.
				 */
				for (int index = 0; index < iridiumFlares.size(); index++)
				{
					Object data[] = iridiumFlares.get(index);
					ephem = (SatelliteEphemElement) data[6];
					float pos[] = projection.projectPosition(ephem.getLocation(), 0, false);
					if (pos != null && !this.isInTheScreen((int)pos[0], (int)pos[1])) pos = null;

					if (!projection.isInvalid(pos))
					{
						//size = getSizeForAGivenMagnitude(1.0);
						String icon = "sat.png";
						drawIcon(icon, pos[0], pos[1], 0.0f, 1.0f);

						if (SaveObjectsToAllowSearch) {
							minorObjects.add(new Object[] {
									RenderSky.OBJECT.ARTIFICIAL_SATELLITE, pos.clone(),
									ephem
							});
							save = true;
						}

						if (render.drawMinorObjectsLabels)
						{
							String label = ephem.name+", "+Functions.formatValue(ephem.magnitude, 1);
							label += (g.renderingToAndroid() ? "m" : "^{m}");
							AstroDate astroS = new AstroDate((Double)data[0]);
							String start = DateTimeOps.twoDigits(astroS.getHour())+":"+DateTimeOps.twoDigits(astroS.getMinute())+":"+DateTimeOps.twoDigits(astroS.getRoundedSecond());
							AstroDate astroE = new AstroDate((Double)data[1]);
							String end = DateTimeOps.twoDigits(astroE.getHour())+":"+DateTimeOps.twoDigits(astroE.getMinute())+":"+DateTimeOps.twoDigits(astroE.getRoundedSecond());
							label += "("+start+"-"+end+")";
							drawString(render.drawStarsColor, render.drawMinorObjectsNamesFont,
									label, pos[0], pos[1], -(18+render.drawMinorObjectsNamesFont.getSize()), false);
						}
					}

				}
			}

			if (save && SaveObjectsToAllowSearch) {
				DataBase.addData("minorObjects", threadID, minorObjects.toArray(), true);
				if (db_minorObjects < 0) db_minorObjects = DataBase.getIndex("minorObjects", threadID);
			}
		}
	}

	private boolean isEclipsedByPlanets(ArrayList<Object> planets, float[] pos, double distance)
	{
		boolean eclipsed = false;
		if (planets == null) return eclipsed;

		for (int i = 0; i < planets.size(); i = i + 2)
		{
			float element[] = (float[]) planets.get(i);
			double dist = (FastMath.pow(pos[0] - element[0], 2.0) + FastMath.pow(pos[1] - element[1], 2.0));
			if (dist <= (element[2]*element[2]) && distance > element[3])
			{
				return true;
			}
		}
		return eclipsed;
	}

	private float lastFieldDegSN = -1, lastFieldDegNovae = -1;
	// Draw SNs
	private void drawSuperNova() throws JPARSECException
	{
		if (!render.drawSuperNovaAndNovaEvents || fieldDeg > 60)
			return;

		ArrayList<Object> sncat = null;
		Object oo = null;
		if (db_sncat >= 0) {
			oo = DataBase.getData(db_sncat);
		} else {
			oo = DataBase.getData("sncat", threadID, true);
		}
		if (oo != null) sncat = new ArrayList<Object>(Arrays.asList((Object[]) oo));
		int limFieldDeg = 30;
		if (sncat == null || (sncat.size() == 0 && fieldDeg < limFieldDeg && fieldDeg != lastFieldDegSN))  {
			lastFieldDegSN = fieldDeg;
			sncat = new ArrayList<Object>();

			InputStream is = null;
			BufferedReader dis = null;
			try
			{
				double jd = TimeScale.getJD(projection.time, projection.obs, projection.eph, SCALE.UNIVERSAL_TIME_UTC);
				AstroDate astro = new AstroDate(jd);
				if (astro.getYear() >= 2015) {
					if (is == null) is = getClass().getClassLoader().getResourceAsStream(FileIO.DATA_SKY_DIRECTORY + "SNafter2015.txt");
					dis = new BufferedReader(new InputStreamReader(is, ReadFile.ENCODING_ISO_8859));
					String line = dis.readLine();
					if (baryc == null) baryc = Ephem.eclipticToEquatorial(PlanetEphem.getGeocentricPosition(equinox, TARGET.Solar_System_Barycenter, 0.0, false, projection.obs), Constant.J2000, projection.eph);
					while ((line = dis.readLine()) != null)
					{
						String SN_id = FileIO.getField(1, line, ",", fast);
						String SN_date = FileIO.getField(2, line, ",", fast);
						String SN_mag = FileIO.getField(3, line, ",", fast);
						String ra = FileIO.getField(4, line, ",", fast);
						String dec = FileIO.getField(5, line, ",", fast);
						
						double mag = Double.parseDouble(SN_mag);
						LocationElement loc = new LocationElement(Functions.parseRightAscension(ra), 
								Functions.parseDeclination(dec), 1.0);
						if (equinox != Constant.J2000) {
							// Correct for aberration, precession, and nutation
							if (projection.eph.ephemType == COORDINATES_TYPE.APPARENT) {
								loc.setRadius(1000 * Constant.RAD_TO_ARCSEC); // 1000 pc
								double light_time = loc.getRadius() * Constant.LIGHT_TIME_DAYS_PER_AU;
								double r[] = Ephem.aberration(loc.getRectangularCoordinates(), baryc, light_time);

								r = precessFromJ2000(equinox, r, projection.eph);
								loc = LocationElement.parseRectangularCoordinates(nutateInEquatorialCoordinates(equinox, projection.eph, r, true));
							} else {
								loc = LocationElement.parseRectangularCoordinates(precessFromJ2000(equinox,
										LocationElement.parseLocationElement(loc), projection.eph));
							}
						}
						if (loc != null) {
							// Correct apparent magnitude for extinction
							if (projection.eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT && projection.eph.correctForExtinction &&
									projection.obs.getMotherBody() == TARGET.EARTH && projection.eph.isTopocentric) {
								double angh = lst - loc.getLongitude();
								double h = FastMath.sin(projection.obs.getLatitudeRad()) * FastMath.sin(loc.getLatitude()) + FastMath.cos(projection.obs.getLatitudeRad()) * FastMath.cos(loc.getLatitude()) * FastMath
										.cos(angh);
								double alt = FastMath.asin(h);
								mag += Star.getExtinction(Constant.PI_OVER_TWO-alt, projection.obs.getHeight() / 1000.0, 5);
							}

							if (projection.obs.getMotherBody() != TARGET.EARTH && projection.obs.getMotherBody() != TARGET.NOT_A_PLANET) {
								loc = projection.getPositionFromBody(loc, this.fast);
							}

							loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, true, false, 0);
							sncat.add(new Object[] {loc, SN_id, mag, SN_date, SN_mag});
						}

					}
				} else {
					FileFormatElement format_Padova_Asiago_SN_cat[] = new FileFormatElement[] {
							new FileFormatElement(3, 9, "SN_ID"), new FileFormatElement(13, 31, "OBJ_HOST"), new FileFormatElement(33, 42, "OBJ_RA"),
							new FileFormatElement(43, 51, "OBJ_DEC"), new FileFormatElement(53, 61, "SN_RA"), new FileFormatElement(63, 71, "SN_DEC"),
							new FileFormatElement(148, 156, "SN_OFFSET_RA"), new FileFormatElement(158, 166, "SN_OFFSET_DEC"),
							new FileFormatElement(169, 172, "SN_MAG"), new FileFormatElement(188, 192, "SN_DATE") };
					ReadFormat rf = new ReadFormat();
					rf.setFormatToRead(format_Padova_Asiago_SN_cat);
					String months = "JanFebMarAprMayJunJulAugSepOctNovDec";

				String p = null;
				try {
					p = Configuration.updateSupernovaeInTempDir(astro);
					if (p != null) is = new FileInputStream(p);
				} catch (Exception exc) {}
				if (is == null) is = getClass().getClassLoader().getResourceAsStream(FileIO.DATA_SKY_DIRECTORY + "Padova-Asiago sn cat.txt");

				dis = new BufferedReader(new InputStreamReader(is, ReadFile.ENCODING_ISO_8859));
				String line = dis.readLine();
				if (baryc == null) baryc = Ephem.eclipticToEquatorial(PlanetEphem.getGeocentricPosition(equinox, TARGET.Solar_System_Barycenter, 0.0, false, projection.obs), Constant.J2000, projection.eph);
				while ((line = dis.readLine()) != null)
				{
					if (!line.isEmpty()) {
						String SN_id = rf.readField(line, "SN_ID").trim();
						String SN_mag = rf.readField(line, "SN_MAG").trim();
						String SN_date = rf.readField(line, "SN_DATE").trim();

						int year = Integer.parseInt(SN_id.substring(0, 4));
						int month = 6;
						int day = 15;
						if (!SN_date.isEmpty())
						{
							month = AstroDate.JANUARY + months.indexOf(SN_date.substring(0, 3)) / 3;
							String SN_day = "";
							if (SN_date.length() > 3)
								SN_day = SN_date.substring(3).trim();
							int k = SN_day.indexOf(":");
							if (k >= 0)
								SN_day = SN_day.substring(0, k);
							if (!SN_day.isEmpty() && !SN_day.equals("t"))
							{
								day = Integer.parseInt(SN_day);
							}
						}
						astro = new AstroDate(year, month, day);
						double SN_jd = astro.jd();

						double mag = 14.0;
						if (SN_mag.endsWith("*")) SN_mag = SN_mag.substring(0, SN_mag.length()-1);
						if (!SN_mag.isEmpty() && !SN_mag.equals("adio") && !SN_mag.startsWith("."))
							mag = Double.parseDouble(SN_mag);

						if (jd > SN_jd && jd < (SN_jd + render.drawSuperNovaEventsNumberOfYears*365.25) && (mag < Math.abs(render.drawObjectsLimitingMagnitude) || fieldDeg < limFieldDeg))
						{
							String SN_RA = rf.readField(line, "SN_RA").trim();
							String SN_DEC = rf.readField(line, "SN_DEC").trim();

							double dec = 0.0;
							if (!SN_DEC.isEmpty()) {
								if (SN_DEC.length() <= 5)
								{
									dec = Functions.parseDeclination(SN_DEC.substring(0, 3) + "d " + SN_DEC.substring(3) + "'");
								} else
								{
									dec = Functions
											.parseDeclination(SN_DEC.substring(0, 3) + "d " + SN_DEC.substring(3, 5) + "' " + SN_DEC
													.substring(5) + "''");
								}
							} else {
								String OBJ_DEC = rf.readField(line, "OBJ_DEC").trim();
								String SN_OFF_DEC = rf.readField(line, "SN_OFFSET_DEC").trim();
								double off_dec = readOffset(SN_OFF_DEC, false);

								if (OBJ_DEC.length() <= 5)
								{
									dec = Functions.parseDeclination(OBJ_DEC.substring(0, 3) + "d " + OBJ_DEC.substring(3) + "'");
								} else
								{
									dec = Functions
											.parseDeclination(OBJ_DEC.substring(0, 3) + "d " + OBJ_DEC.substring(3, 5) + "' " + OBJ_DEC
													.substring(5) + "''");
								}
								dec += off_dec * Constant.ARCSEC_TO_RAD;
							}

							double ra = 0.0;
							if (!SN_RA.isEmpty()) {
								if (SN_RA.indexOf(".") == 4)
								{
									ra = Functions.parseRightAscension(SN_RA.substring(0, 2) + "h " + SN_RA.substring(2) + "m");
								} else
								{
									ra = Functions
											.parseRightAscension(SN_RA.substring(0, 2) + "h " + SN_RA.substring(2, 4) + "m " + SN_RA
													.substring(4) + "s");
								}
							} else {
								String OBJ_RA = rf.readField(line, "OBJ_RA").trim();
								String SN_OFF_RA = rf.readField(line, "SN_OFFSET_RA").trim();
								if (OBJ_RA.indexOf(".") == 4)
								{
									ra = Functions.parseRightAscension(OBJ_RA.substring(0, 2) + "h " + OBJ_RA.substring(2) + "m");
								} else
								{
									ra = Functions
											.parseRightAscension(OBJ_RA.substring(0, 2) + "h " + OBJ_RA.substring(2, 4) + "m " + OBJ_RA
													.substring(4) + "s");
								}
								double off_ra = readOffset(SN_OFF_RA, true);
								ra += off_ra * Constant.ARCSEC_TO_RAD / FastMath.cos(dec);
							}


							LocationElement loc = new LocationElement(ra, dec, 1.0);
							if (equinox != Constant.J2000) {
								// Correct for aberration, precession, and nutation
								if (projection.eph.ephemType == COORDINATES_TYPE.APPARENT) {
									loc.setRadius(1000 * Constant.RAD_TO_ARCSEC); // 1000 pc
									double light_time = loc.getRadius() * Constant.LIGHT_TIME_DAYS_PER_AU;
									double r[] = Ephem.aberration(loc.getRectangularCoordinates(), baryc, light_time);

									r = precessFromJ2000(equinox, r, projection.eph);
									loc = LocationElement.parseRectangularCoordinates(nutateInEquatorialCoordinates(equinox, projection.eph, r, true));
								} else {
									loc = LocationElement.parseRectangularCoordinates(precessFromJ2000(equinox,
											LocationElement.parseLocationElement(loc), projection.eph));
								}
							}
							if (loc != null) {
								// Correct apparent magnitude for extinction
								if (projection.eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT && projection.eph.correctForExtinction &&
										projection.obs.getMotherBody() == TARGET.EARTH && projection.eph.isTopocentric) {
									double angh = lst - loc.getLongitude();
									double h = FastMath.sin(projection.obs.getLatitudeRad()) * FastMath.sin(loc.getLatitude()) + FastMath.cos(projection.obs.getLatitudeRad()) * FastMath.cos(loc.getLatitude()) * FastMath
											.cos(angh);
									double alt = FastMath.asin(h);
									mag += Star.getExtinction(Constant.PI_OVER_TWO-alt, projection.obs.getHeight() / 1000.0, 5);
								}

								if (projection.obs.getMotherBody() != TARGET.EARTH && projection.obs.getMotherBody() != TARGET.NOT_A_PLANET) {
									loc = projection.getPositionFromBody(loc, this.fast);
								}

								loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, true, false, 0);
								sncat.add(new Object[] {loc, "SN"+SN_id, mag, SN_date, SN_mag});
							}
						}
					}
				}
				}
				// Close file
				dis.close();
				is.close();
				is = null;
				DataBase.addData("sncat", threadID, sncat.toArray(), true);
				db_sncat = DataBase.getIndex("sncat", threadID);
			} catch (Exception e2)
			{
				try {
					if (dis != null) dis.close();
					if (is != null) is.close();
					is = null;
				} catch (Exception exc) {}
				throw new JPARSECException(
						"error while reading SN file.", e2);
			}
		}

		g.setColor(render.drawSuperNovaEventsColor, true);
		LocationElement loc;
		float dist = getDist(-refz/4);
		ArrayList<Object> minorObjects = null;
		if (SaveObjectsToAllowSearch) {
			minorObjects = new ArrayList<Object>();
			Object o = null;
			if (db_minorObjects >= 0) {
				o = DataBase.getData(db_minorObjects);
			} else {
				o = DataBase.getData("minorObjects", threadID, true);
			}
			if (o != null) minorObjects = new ArrayList<Object>(Arrays.asList((Object[]) o));
		}
		boolean save = false;
		for (Iterator<Object> itr = sncat.iterator();itr.hasNext();)
		{
			Object obj[] = (Object[]) itr.next();
			double mag = (Double) obj[2];
			if (mag < Math.abs(render.drawObjectsLimitingMagnitude) || (fieldDeg < limFieldDeg && render.drawClever)) {
				loc = (LocationElement) obj[0];
				float[] pos = projection.projectPosition(loc, 0, false);
				if (pos != null && !this.isInTheScreen((int)pos[0], (int)pos[1])) pos = null;

				if (!projection.isInvalid(pos))
				{
					float size = getSizeForAGivenMagnitude(mag);
					int tsize = (int) (0.5 + (1.5f * size+adds));
					if (tsize > 1 && render.drawStarsRealistic != REALISTIC_STARS.NONE &&
							render.drawStarsRealistic != REALISTIC_STARS.NONE_CUTE) {
						if (g.renderingToAndroid()) tsize ++;
						this.drawStar(tsize, pos, dist, 6, g);
					} else {
						tsize = (int) (2* size+adds);
						if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
							g.fillOval((pos[0] - size), (pos[1] - size), tsize, tsize, this.fast);
						} else {
							g.fillOval((pos[0] - size), (pos[1] - size), tsize, tsize, dist);
						}
					}

					if (SaveObjectsToAllowSearch) {
						minorObjects.add(new Object[] {
								RenderSky.OBJECT.SUPERNOVA, pos.clone(), loc, (float) mag, (String) obj[1],  
								(String) obj[3]
						});
						save = true;
					}

					if (render.drawDeepSkyObjectsLabels)
						drawString(render.drawSuperNovaEventsColor, render.drawMinorObjectsNamesFont, (String) obj[1], pos[0], pos[1], -Math.max(render.drawMinorObjectsNamesFont.getSize()+2, 3 * size), false);
				}
			}
		}
		if (save && SaveObjectsToAllowSearch) {
			DataBase.addData("minorObjects", threadID, minorObjects.toArray(), true);
			if (db_minorObjects < 0) db_minorObjects = DataBase.getIndex("minorObjects", threadID);
		}
	}

	// Draw Novae
	private void drawNovae() throws JPARSECException
	{
		if (!render.drawSuperNovaAndNovaEvents || fieldDeg > 60)
			return;

		ArrayList<Object> novae = null;
		Object oo = null;
		if (db_novae >= 0) {
			oo = DataBase.getData(db_novae);
		} else {
			oo = DataBase.getData("novae", threadID, true);
		}
		if (oo != null) novae = new ArrayList<Object>(Arrays.asList((Object[]) oo));
		int limFieldDeg = 50;
		if (novae == null || (novae.size() == 0 && fieldDeg < limFieldDeg && fieldDeg != lastFieldDegNovae))  {
			lastFieldDegNovae = fieldDeg;
			novae = new ArrayList<Object>();
			FileFormatElement novaeFormat[] = new FileFormatElement[] {
					new FileFormatElement(1, 14, "ID"),
					new FileFormatElement(15, 30, "DATE"),
					new FileFormatElement(32, 43, "VAR"),
					new FileFormatElement(44, 55, "RA"),
					new FileFormatElement(60, 70, "DEC"),
					new FileFormatElement(75, 78, "DISCO_MAG"),
					new FileFormatElement(84, 87, "MAX_MAG"),
					new FileFormatElement(92, 103, "MIN_MAG"),
					new FileFormatElement(104, 110, "T3"),
					new FileFormatElement(111, 118, "CLASS"),
					new FileFormatElement(119, 140, "X"),
					new FileFormatElement(141, 181, "DISCOVERER"),
					new FileFormatElement(182, 500, "REFERENCES"),
					};
			ReadFormat rf = new ReadFormat();
			rf.setFormatToRead(novaeFormat);

			InputStream is = null;
			BufferedReader dis = null;
			try
			{
				double jd = TimeScale.getJD(projection.time, projection.obs, projection.eph, SCALE.UNIVERSAL_TIME_UTC);
				AstroDate astro = new AstroDate(jd);
				String p = null;
				try {
					p = Configuration.updateNovaeInTempDir(astro);
					if (p != null) is = new FileInputStream(p);
				} catch (Exception exc) {}
				if (is == null) is = getClass().getClassLoader().getResourceAsStream(FileIO.DATA_SKY_DIRECTORY + "galnovae.txt");

				dis = new BufferedReader(new InputStreamReader(is, ReadFile.ENCODING_ISO_8859));
				String line = dis.readLine();
				if (baryc == null) baryc = Ephem.eclipticToEquatorial(PlanetEphem.getGeocentricPosition(equinox, TARGET.Solar_System_Barycenter, 0.0, false, projection.obs), Constant.J2000, projection.eph);
				while ((line = dis.readLine()) != null)
				{
					if (!line.isEmpty() && !line.startsWith("NOVA")) {
						String id = rf.readField(line, "ID").trim();
						String magnitude = rf.readField(line, "MAX_MAG").trim();
						String discoDate = rf.readField(line, "DATE").trim();
						if (magnitude.equals("")) magnitude = rf.readField(line, "DISCO_MAG").trim();;
						if (magnitude.equals("")) continue;

						String val[] = DataSet.toStringArray(discoDate, " ", true);
						if (val.length < 1) continue;
						int year = Integer.parseInt(val[0]);
						int month = 6;
						double day = 1;
						if (val.length > 1 && !val[1].startsWith("M") && DataSet.isDoubleFastCheck(val[1])) month = Integer.parseInt(val[1]);
						if (val.length > 2 && !val[2].startsWith("M") && DataSet.isDoubleFastCheck(val[2])) day = Double.parseDouble(val[2]);
						astro = new AstroDate(year, month, day);
						double discoJD = astro.jd();

						double mag = Double.parseDouble(magnitude);

						if (jd > discoJD && jd < (discoJD + render.drawSuperNovaEventsNumberOfYears*365.25) && (mag < Math.abs(render.drawObjectsLimitingMagnitude) || fieldDeg < limFieldDeg))
						{
							String sRA = rf.readField(line, "RA").trim();
							String sDEC = rf.readField(line, "DEC").trim();

							double ra = Functions.parseRightAscension(sRA);
							double dec = Functions.parseDeclination(sDEC);

							LocationElement loc = new LocationElement(ra, dec, 1.0);
							if (equinox != Constant.J2000) {
								// Correct for aberration, precession, and nutation
								if (projection.eph.ephemType == COORDINATES_TYPE.APPARENT) {
									loc.setRadius(1000 * Constant.RAD_TO_ARCSEC); // 1000 pc
									double light_time = loc.getRadius() * Constant.LIGHT_TIME_DAYS_PER_AU;
									double r[] = Ephem.aberration(loc.getRectangularCoordinates(), baryc, light_time);

									r = precessFromJ2000(equinox, r, projection.eph);
									loc = LocationElement.parseRectangularCoordinates(nutateInEquatorialCoordinates(equinox, projection.eph, r, true));
								} else {
									loc = LocationElement.parseRectangularCoordinates(precessFromJ2000(equinox,
											LocationElement.parseLocationElement(loc), projection.eph));
								}
							}
							if (loc != null) {
								// Correct apparent magnitude for extinction
								if (projection.eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT && projection.eph.correctForExtinction &&
										projection.obs.getMotherBody() == TARGET.EARTH && projection.eph.isTopocentric) {
									double angh = lst - loc.getLongitude();
									double h = FastMath.sin(projection.obs.getLatitudeRad()) * FastMath.sin(loc.getLatitude()) + FastMath.cos(projection.obs.getLatitudeRad()) * FastMath.cos(loc.getLatitude()) * FastMath
											.cos(angh);
									double alt = FastMath.asin(h);
									mag += Star.getExtinction(Constant.PI_OVER_TWO-alt, projection.obs.getHeight() / 1000.0, 5);
								}

								if (projection.obs.getMotherBody() != TARGET.EARTH && projection.obs.getMotherBody() != TARGET.NOT_A_PLANET) {
									loc = projection.getPositionFromBody(loc, this.fast);
								}

								loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, true, false, 0);
								novae.add(new Object[] {loc, id, mag, discoDate, magnitude});
							}
						}
					}
				}
				// Close file
				dis.close();
				is.close();
				is = null;
				DataBase.addData("novae", threadID, novae.toArray(), true);
				db_novae = DataBase.getIndex("novae", threadID);
			} catch (Exception e2)
			{
				try {
					if (dis != null) dis.close();
					if (is != null) is.close();
					is = null;
				} catch (Exception exc) {}
				throw new JPARSECException(
						"error while reading novae file.", e2);
			}
		}

		g.setColor(render.drawSuperNovaEventsColor, true);
		LocationElement loc;
		float dist = getDist(-refz/4);
		ArrayList<Object> minorObjects = null;
		if (SaveObjectsToAllowSearch) {
			minorObjects = new ArrayList<Object>();
			Object o = null;
			if (db_minorObjects >= 0) {
				o = DataBase.getData(db_minorObjects);
			} else {
				o = DataBase.getData("minorObjects", threadID, true);
			}
			if (o != null) minorObjects = new ArrayList<Object>(Arrays.asList((Object[]) o));
		}
		boolean save = false;
		for (Iterator<Object> itr = novae.iterator();itr.hasNext();)
		{
			Object obj[] = (Object[]) itr.next();
			double mag = (Double) obj[2];
			if (mag < Math.abs(render.drawObjectsLimitingMagnitude) || (fieldDeg < limFieldDeg && render.drawClever)) {
				loc = (LocationElement) obj[0];
				float[] pos = projection.projectPosition(loc, 0, false);
				if (pos != null && !this.isInTheScreen((int)pos[0], (int)pos[1])) pos = null;

				if (!projection.isInvalid(pos))
				{
					float size = getSizeForAGivenMagnitude(mag);
					int tsize = (int) (0.5 + (1.5f * size+adds));
					if (tsize > 1 && render.drawStarsRealistic != REALISTIC_STARS.NONE &&
							render.drawStarsRealistic != REALISTIC_STARS.NONE_CUTE) {
						if (g.renderingToAndroid()) tsize ++;
						this.drawStar(tsize, pos, dist, 6, g);
					} else {
						tsize = (int) (2* size+adds);
						if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
							g.fillOval((pos[0] - size), (pos[1] - size), tsize, tsize, this.fast);
						} else {
							g.fillOval((pos[0] - size), (pos[1] - size), tsize, tsize, dist);
						}
					}

					if (SaveObjectsToAllowSearch) {
						minorObjects.add(new Object[] {
								RenderSky.OBJECT.NOVA, pos.clone(), loc, (float) mag, (String) obj[1], 
								(String) obj[3]
						});
						save = true;
					}

					if (render.drawDeepSkyObjectsLabels)
						drawString(render.drawSuperNovaEventsColor, render.drawMinorObjectsNamesFont, (String) obj[1], pos[0], pos[1], -Math.max(render.drawMinorObjectsNamesFont.getSize()+2, 3 * size), false);
				}
			}
		}
		if (save && SaveObjectsToAllowSearch) {
			DataBase.addData("minorObjects", threadID, minorObjects.toArray(), true);
			if (db_minorObjects < 0) db_minorObjects = DataBase.getIndex("minorObjects", threadID);
		}
	}

	private static double readOffset(String field, boolean ra)
	{
		int b = -1;
		int a = 0;
		if (ra) {
			a = field.indexOf("W");
			if (a < 0)
			{
				a = field.indexOf("E");
				if (a >= 0) b = 1;
			}
			if (field.indexOf("S") < a && field.indexOf("S") >= 0 || a < 0)
				a = field.indexOf("S");
			if (field.indexOf("N") < a && field.indexOf("N") >= 0 || a < 0)
				a = field.indexOf("N");
		} else {
			a = field.indexOf("S");
			if (a < 0)
			{
				a = field.indexOf("N");
				if (a >= 0) b = 1;
			}
			if (field.indexOf("E") < a && field.indexOf("E") >= 0 || a < 0)
				a = field.indexOf("E");
			if (field.indexOf("W") < a && field.indexOf("W") >= 0 || a < 0)
				a = field.indexOf("W");
		}
		double c = 0.0;
		if (a > 0)
			c = Double.parseDouble(field.substring(0, a));

		return c * (double) b;
	}

	/**
	 * Reads stars closer to certain equatorial position from a file of stars,
	 * formatted in JPARSEC file format.
	 * <P>
	 * After the file is succesfully read, the objects are added to and stored
	 * in READ_ELEMENTS ArrayList.
	 *
	 * @param loc Object defining the position.
	 * @param maglim Limiting magnitude.
	 * @param re Read object.
	 * @param path Path to the file to read.
	 * @param externalGraphics True in case of rendering to external Graphics.
	 * @param android True when rendering to Android.
	 * @throws JPARSECException Thrown if the format is invalid.
	 * @return Maximum proper motion in arcseconds/century for the stars in this
	 *         field.
	 */
	private void readFileOfStars(double jd, double equinox, Projection projection, LocationElement loc, double maglim, ReadFile re, String path, boolean externalGraphics,
			boolean android) throws JPARSECException
	{
		// Define necesary variables
		String file_line = "";

		ArrayList<StarData> list = new ArrayList<StarData>();
		Object o[] = re.getReadElements();

		ReadFormat rf = new ReadFormat();
		rf.setFormatToRead(FileFormatElement.JPARSEC_SKY2000_FORMAT);
		String greek = "AlpBetGamDelEpsZetEtaTheIotKapLamMu Nu Xi OmiPi RhoSigTauUpsPhiChiPsiOme";
		EphemerisElement eph = projection.eph.clone();
		eph.ephemMethod = REDUCTION_METHOD.IAU_2006;
		double lim = Constant.PI_OVER_TWO + deg_0_6; // aberration + nutation + refraction
		double cte = 100.0 * Math.abs(Functions.toCenturies(jd));
		int CRITICAL_NSTARS = 4255;
		int index = -1;
		if (o != null) index = 8883;
		if (baryc == null) baryc = Ephem.eclipticToEquatorial(PlanetEphem.getGeocentricPosition(equinox, TARGET.Solar_System_Barycenter, 0.0, false, projection.obs), Constant.J2000, projection.eph);
		double cte0 = 100.0 * Constant.RAD_TO_DEG * pixels_per_degree * Math.abs(Functions.toCenturies(jd));
		double cte1 = 30 * Constant.ARCSEC_TO_DEG * pixels_per_degree; // aberration + nutation
		double cte11 = cte1 / pixels_per_radian;
		double fieldLimit = field * 0.5;
		fieldLimit = 1.05*fieldLimit*Math.sqrt(1.0+(render.height/(double)render.width));
		if (fieldLimit < Constant.DEG_TO_RAD) fieldLimit = Constant.DEG_TO_RAD;
		double jYearsFromJ2000 = Math.abs(jd - Constant.J2000) / 365.25;

		// Connect to the file
		InputStream is = null;
		BufferedReader dis = null;
		try
		{
			is = getClass().getClassLoader().getResourceAsStream(path);
			dis = new BufferedReader(new InputStreamReader(is));

			while ((file_line = dis.readLine()) != null)
			{
				// Obtain fields
				StarData star = parseJPARSECfile(jd, equinox, projection, file_line, rf, greek, eph, loc, lim, cte, baryc, externalGraphics,
						android, cte0, cte1, cte11, jYearsFromJ2000, fieldLimit);
				index ++;
				if (star != null) {
					if (star.mag[0] > maglim) break;
					//star.index = index;
					list.add(star);
				} else {
					if (index < CRITICAL_NSTARS) list.add(null);
				}
			}

			// Close file
			dis.close();
			is.close();
			is = null;

			if (o == null) {
				re.setReadElements(list);
			} else {
				re.addReadElements(list);
			}

/*
			if (maglim > 9) {
				ArrayList<String> names = new ArrayList<String>();
				Object oo[] = re.getReadElements();
				for (int i=0; i<oo.length; i++) {
					String n = ((StarElement)oo[i]).name;
					String type = ((StarElement)oo[i]).type;
					if (type.startsWith("D") || type.startsWith("B")) {
						int nData = FileIO.getNumberOfFields(type, ";", true);
						if (nData >= 2) {
							String dData = FileIO.getField(2, type, ";", true);
							String s = FileIO.getField(4, dData, ",", true);
							if (s != null && !s.isEmpty()) {
								int bracket1 = n.indexOf("(");
								int bracket2 = n.indexOf(")");
								if (bracket1 >= 0 && bracket2 >= 0) {
									n = n.substring(bracket1 + 1, bracket2);
									names.add(n);
								}
							}
						}
					}
				}
				System.out.println("searching ..."); // Double stars with PA and repeated names (possibly wrong PA in the secondary in SkyMaster2000 catalog)
				do {
					String n = names.remove(0);
					if (names.contains(n)) {
						do {
							names.remove(n);
						} while (names.contains(n));
						System.out.println(n);
					}
				} while (names.size() > 0);
			}
*/
		} catch (Exception e2)
		{
			try {
				if (dis != null) dis.close();
				if (is != null) is.close();
				is = null;
			} catch (Exception exc) {}
			throw new JPARSECException(
					"error while reading file " + re.pathToFile, e2);
		}
	}

	private static final int[] doubleStarsWithSecondaryDrawnWithOppositePA = new int[] {
		7340159, 5400235, 13230151, 14440151, 9470168, 5350252, 2020164, 15050148, 12540162, 15340198, 16110142,
		16040191, 19060248, 4420028, 17230211, 16250111, 2590131, 18030012, 21440198, 14400113, 18440069, 15560193,
		17180197, 18440267, 16140187, 17050176, 4500154, 8120239, 8260269
	};

	class StarData {
		public LocationElement loc;
		public double ra, dec;
		public float pos[], var[], doub[], mag[];
		public String nom2, properName;
		public char greek;
		public int sky2000; // index
		public byte spi;

		public StarData(LocationElement loc, float mag) {
			if (loc != null) loc.set(DataSet.toFloatArray(loc.get())); // Reduce memory use
			this.loc = loc;
			this.mag = new float[] {mag};
			spi = -1;
		}
	}

	/** Nasty clone of method in ReadFile class to improve performance. */
	private StarElement starElem = null;
	private static int[] doubles = new int[] {
			2030133, 2030177, 22280123, 22280181, 1530079, 1530157, 12560152, 12560007, 
			20210000, 20200211, 14400004, 14390136, 8260173, 8260268, 8120049, 8120239, 
			18330084, 18330083, 15390172, 15390051, 12260079, 12260152, 20460241, 20460138, 
			2580135, 2580038, 17140177, 17140107, 10200011, 10190145, 18440069, 18440263, 
			18440080, 18440267, 3170086, 3180005, 16050080, 16050083, 4390027, 4390047, 
			310184, 310089, 8090116, 8090128, 8190189, 8200002, 7080200, 7080327
	};
	private StarData parseJPARSECfile(double jd, double equinox, Projection projection, String line, ReadFormat rf, String greek,
			EphemerisElement eph, LocationElement eqCenit, double lim, double cte, double baryc[],
			boolean externalGraphics, boolean android,
			double cte0, double cte1, double cte11, double jYearsFromJ2000, double fieldLimit) throws JPARSECException
	{
		try {
			double sra = rf.readFloat(line, "RA") / Constant.RAD_TO_HOUR;
			double sdec = rf.readFloat(line, "DEC") * Constant.DEG_TO_RAD;

			LocationElement locStar0 = new LocationElement(sra, sdec, 1.0);
			double approxAngDist2 = -1;
			if (jYearsFromJ2000 < 100 && !render.drawSkyBelowHorizon && eqCenit != null && projection.obs.getMotherBody() == TARGET.EARTH) {
				approxAngDist2 = LocationElement.getApproximateAngularDistance(eqCenit, locStar0);
				if (approxAngDist2 > lim) return null;
			}

			float pmra = (float) (rf.readFloat(line, "RA_PM") * 15.0 * Constant.ARCSEC_TO_RAD);
			float pmdec = (float) (rf.readFloat(line, "DEC_PM") * Constant.ARCSEC_TO_RAD);

			double cosdec = FastMath.cos(sdec);
			if (eqCenit != null) {
				approxAngDist2 = LocationElement.getApproximateAngularDistance(eqCenit, locStar0);
				double properM = Math.max(Math.abs(pmra), Math.abs(pmdec / cosdec));
				if (approxAngDist2 > lim + (properM * cte)) return null;
			}

			starElem = new StarElement();
			starElem.rightAscension = sra;
			starElem.declination = sdec;
			starElem.properMotionRA = pmra;
			starElem.properMotionDEC = pmdec;

			starElem.name = rf.readString(line, "NAME");
			starElem.spectrum = rf.readString(line, "SPECTRUM");
			starElem.type = rf.readString(line, "TYPE")+";"+rf.readString(line, "DATA");
			starElem.magnitude = rf.readFloat(line, "MAG");
			//starElem.properMotionRadialV = 0.0f;
			String rv = rf.readString(line, "RADIAL_VELOCITY");
			if (!rv.isEmpty()) starElem.properMotionRadialV = Float.parseFloat(rv);
			starElem.parallax = rf.readFloat(line, "PARALLAX");
			starElem.equinox = Constant.J2000;
			//star.frame = EphemerisElement.FRAME.ICRF;

			// Add classical name
			String id = rf.readString(line, "ID");
			String constel = "";
			if (!id.isEmpty())
			{
				int index = id.indexOf("-");
				String idd = "";
				if (index >= 0) {
					idd = id.substring(index + 1);
					index = Integer.parseInt(id.substring(0, index));
				} else {
					index = Integer.parseInt(id);
				}
				String code = greek.substring((index - 1) * 3, index * 3).trim()+idd;
				String constell = "";
				//if (!g.renderingToAndroid()) {
					try
					{
						constell = Constellation.getConstellationAbbreviation(locStar0, Constant.J2000,
								eph);
					} catch (JPARSECException ve)
					{
						constell = "";
					}
				//}
				if (!constell.isEmpty()) {
					constel = code + " " + constell.substring(0, 3);
				} else {
					constel = code;
				}
				if (!constel.isEmpty())
					starElem.name += " (" + constel + ") (" + id + ")";
			}


			StarData sd = null;
			double properM = Math.max(Math.abs(starElem.properMotionDEC), Math.abs(starElem.properMotionRA / cosdec));

			// Reduce (very slightly) the accuracy of star ephemerides for better startup time in Android.
			// Accuracy is sacrified by less than 1", and startup time is 2-3 times faster. Note star proper
			// motions are taken into account even on Android
			boolean highPrecision = !android;
			if (highPrecision && !projection.eph.preferPrecisionInEphemerides) highPrecision = false;

			/* space motion */
			if (baryc == null) baryc = Ephem.eclipticToEquatorial(PlanetEphem.getGeocentricPosition(equinox, TARGET.Solar_System_Barycenter, 0.0, false, projection.obs), Constant.J2000, projection.eph);
			if (projection.eph.ephemType != COORDINATES_TYPE.GEOMETRIC && properM > 0)
			{
				double p[] = new double[3];
				double relativisticFactor = 1.0 / (1.0 - starElem.properMotionRadialV / Constant.SPEED_OF_LIGHT);
				double sindec = FastMath.sin(starElem.declination);
				double cosra = FastMath.cos(starElem.rightAscension);
				double sinra = FastMath.sin(starElem.rightAscension);
				double q[] = new double[] {cosra * cosdec, sinra * cosdec, sindec};
				double cte2 = 0.21094952658238966; // Constant.SECONDS_PER_DAY * Constant.JULIAN_DAYS_PER_CENTURY * 0.01 / Constant.AU;
				double cte3 = starElem.parallax * 0.001 / Constant.RAD_TO_ARCSEC;
				double vpi = cte2 * starElem.properMotionRadialV * cte3;
				double m[] = new double[3];
				m[0] = (-starElem.properMotionRA * cosdec * sinra - starElem.properMotionDEC * sindec * cosra + vpi * q[0]) * relativisticFactor;
				m[1] = (starElem.properMotionRA * cosdec * cosra - starElem.properMotionDEC * sindec * sinra + vpi * q[1]) * relativisticFactor;
				m[2] = (starElem.properMotionDEC * cosdec + vpi * q[2]) * relativisticFactor;
				double T = (jd - starElem.equinox) * 100.0 / Constant.JULIAN_DAYS_PER_CENTURY;
				for (int i = 0; i < 3; i++)
				{
					p[i] = q[i] + T * m[i] + baryc[i] * cte3;
				}
				locStar0 = LocationElement.parseRectangularCoordinates(p);
			}

			locStar0.setRadius(starElem.getDistance() * Constant.RAD_TO_ARCSEC);
			//if (equinox != Constant.J2000) {
				// Correct for aberration, precession, and nutation
				if (projection.eph.ephemType == COORDINATES_TYPE.APPARENT) {
					double light_time = locStar0.getRadius() * Constant.LIGHT_TIME_DAYS_PER_AU;
					double[] r = Ephem.aberration(locStar0.getRectangularCoordinates(), baryc, light_time);

					if (highPrecision) r = Ephem.toOutputFrame(r, starElem.frame, eph.frame);
					r = precessFromJ2000(equinox, r, projection.eph);
					r = nutateInEquatorialCoordinates(equinox, projection.eph, r, true);

					// Correct for polar motion
					if (highPrecision && eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT &&
							eph.correctForPolarMotion)
					{
						double gast = SiderealTime.greenwichApparentSiderealTime(projection.time, projection.obs, projection.eph);
						r = Functions.rotateZ(r, -gast);
						Matrix mat = IAU2006.getPolarMotionCorrectionMatrix(projection.time, projection.obs, projection.eph);
						r = mat.times(new Matrix(r)).getColumn(0);
						r = Functions.rotateZ(r, gast);
					}

					locStar0 = LocationElement.parseRectangularCoordinates(r);
				} else {
					if (equinox != Constant.J2000)
						locStar0 = LocationElement.parseRectangularCoordinates(precessFromJ2000(equinox,
							LocationElement.parseLocationElement(locStar0), projection.eph));
				}
			//}
			if (highPrecision && projection.eph.isTopocentric) {
				EphemElement ephem = new EphemElement();
				ephem.setEquatorialLocation(locStar0);
				ephem = Ephem.topocentricCorrection(projection.time, projection.obs, projection.eph, ephem);
				locStar0 = ephem.getEquatorialLocation();
			}
			if (projection.obs.getMotherBody() != TARGET.EARTH && projection.obs.getMotherBody() != TARGET.NOT_A_PLANET) {
				locStar0 = projection.getPositionFromBody(locStar0, false);
 			}
			LocationElement ll = projection.getApparentLocationInSelectedCoordinateSystem(locStar0, true, false, 0);

			if (drawAll) {
				float[] pos0 = projection.projectPosition(ll, 0, false);
				if (pos0 != null && !this.isInTheScreen((int)pos0[0], (int)pos0[1], 0)) pos0 = null;
				if (pos0 == null) ll = null;
			}
			if (ll != null) ll.setRadius(ll.getRadius() / Constant.RAD_TO_ARCSEC);
			sd = new StarData(ll, starElem.magnitude);
			sd.ra = locStar0.getLongitude();
			sd.dec = locStar0.getLatitude();
			if (eph.correctForExtinction) {
				sd.mag = new float[] {starElem.magnitude, (float) correctForExtinction(locStar0, starElem.magnitude)};
			} else {
				sd.mag = new float[] {starElem.magnitude};
			}

			String spectrum = "OBAFGKM";
			sd.spi = -1;
			if (!starElem.spectrum.equals("")) {
				sd.spi = (byte) (spectrum.indexOf(starElem.spectrum.substring(0, 1)) * 10);
				if (starElem.spectrum.length() > 1) {
					String ss = starElem.spectrum.substring(1, 2);
					if (DataSet.isDoubleFastCheck(ss)) try { sd.spi += Integer.parseInt(ss); } catch (Exception exc) {}
				}
			}
			sd.sky2000 = Integer.parseInt(FileIO.getField(1, starElem.name, " ", true));
			int bracket1 = starElem.name.indexOf("(");
			int bracket2 = starElem.name.indexOf(")");
			if (bracket1 >= 0 && bracket2 >= 0) {
				sd.nom2 = starElem.name.substring(bracket1 + 1, bracket2);

				bracket1 = starElem.name.lastIndexOf("(");
				bracket2 = starElem.name.lastIndexOf(")");
				String name2 = starElem.name.substring(bracket1 + 1, bracket2);
				String name3 = "";
				int n3 = name2.indexOf("-");
				if (n3 >= 0) {
					name3 = "^{"+name2.substring(n3+1)+"}";
					name2 = name2.substring(0, n3);
				}
				//label = "@SIZE+4"+greek[Integer.parseInt(name2) - 1]+name3+"@SIZE-4";
				sd.properName = name3;
				if (externalGraphics && !android) {
					sd.greek = greekPDF[Integer.parseInt(name2) - 1];
					if (!name3.equals("")) {
						// Remove repeated symbols like multiple eps-1 eps-2 lyrae etc when 
						// rendering to pdf, usually with wide angle views
						int index = DataSet.getIndex(doubles, sd.sky2000);
						if (index >= 0) {
							sd.properName = "";
							if ((name3.equals("^{2}") && sd.sky2000 != 8260173) || 
									sd.sky2000 == 8120239 || sd.sky2000 == 18440263) 
								sd.greek = '\u0000';
						}
					}
				} else {
					sd.greek = RenderSky.greek[Integer.parseInt(name2) - 1];
				}
			}

			int nData = 0;
			if (starElem.type != null) nData = FileIO.getNumberOfFields(starElem.type, ";", true);
			if (nData >= 2) {
				String dData = FileIO.getField(2, starElem.type, ";", true);
				String s = FileIO.getField(1, dData, ",", true);
				float pa = 0, sep = -1, difM = -1, per = -1;
				if (s != null && !s.isEmpty())
					sep = Float.parseFloat(s); 
				s = FileIO.getField(2, dData, ",", true);
				if (s != null && !s.isEmpty()) difM = Float.parseFloat(s); 
				s = FileIO.getField(3, dData, ",", true);
				if (s != null && !s.isEmpty()) per = Float.parseFloat(s); 
				if (render.drawStarsPositionAngleInDoubles) {
					s = FileIO.getField(4, dData, ",", true);
					if (s != null && !s.isEmpty()) {
						pa = (float) (-Constant.DEG_TO_RAD * Float.parseFloat(s));

						int index = DataSet.getIndex(doubleStarsWithSecondaryDrawnWithOppositePA, sd.sky2000);
						if (index >= 0) pa += Math.PI;
					}
					if (pa == 0) pa = (float)(0.001 * Constant.DEG_TO_RAD);
				}
				if (sep > 0 || pa != 0) sd.doub = new float[] {sep, pa, difM, per};
			}

			if (nData == 3) {
				String varData = FileIO.getField(3, starElem.type, ";", true);
				int varnData = FileIO.getNumberOfFields(varData, ",", false);
				if (varnData == 4) { // Only consider variable those with variability in band V in Sky2000 catalog
					String max = FileIO.getField(1, varData, ",", true);
					String min = FileIO.getField(2, varData, ",", true);
					if (!max.isEmpty() && !min.isEmpty()) {
						float maxMag = Float.parseFloat(max);
						float minm = Float.parseFloat(min);
						String s = FileIO.getField(3, varData, ",", true);
						float per = -1, type = -1;
						if (s != null && !s.isEmpty()) per = Float.parseFloat(s); 
						s = FileIO.getField(4, varData, ",", true);
						if (s != null && !s.isEmpty()) type = Float.parseFloat(s); 
						sd.var = new float[] {minm, maxMag, per, type};
					}
				}
			}

			return sd;
		} catch (Exception exc) {
			if (!g.renderingToAndroid()) exc.printStackTrace();
			JPARSECException.addWarning("Could not parse this star in JPARSEC format, returning null. Details: "+exc.getLocalizedMessage()+". Line to parse was: "+line);
		}
		return null;
	}

	private double correctForExtinction(LocationElement eq, double mag) throws JPARSECException {
		// Correct apparent magnitude for extinction
		if (projection.obs.getMotherBody() == TARGET.EARTH && projection.eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT && projection.eph.correctForExtinction) {
			LocationElement horLoc = CoordinateSystem.equatorialToHorizontal(eq, projection.ast, projection.obs, projection.eph, true, false);
			mag += Star.getExtinction(Constant.PI_OVER_TWO-horLoc.getLatitude(), projection.obs.getHeight() / 1000.0, 5);
		}
		return mag;
	}

	/**
	 * Returns the coordinates of certain equatorial position in the current
	 * selected coordinate system for sky projection.
	 *
	 * @param loc Equatorial (non-refracted) coordinates.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param sky Render object.
	 * @param fast True for an approximate but faster calculation.
	 * @return Output coordinates. Same as input in the equatorial system.
	 * @throws JPARSECException If the method fails.
	 */
	public static LocationElement getPositionInSelectedCoordinateSystem(LocationElement loc, TimeElement time,
			ObserverElement obs, EphemerisElement eph, SkyRenderElement sky, boolean fast) throws JPARSECException
	{
		double jd = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		double equinox = eph.equinox;
		if (equinox == EphemerisElement.EQUINOX_OF_DATE)
			equinox = jd;

		double obliquity = 0.0;
		if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT) {
			obliquity = Obliquity.trueObliquity(Functions.toCenturies(equinox), eph);
		} else {
			obliquity = Obliquity.meanObliquity(Functions.toCenturies(equinox), eph);
		}

		LocationElement loc0 = loc.clone();

		boolean toApparent = false;
		if (sky.drawSkyCorrectingLocalHorizon && obs.getMotherBody() == TARGET.EARTH && eph.isTopocentric) toApparent = true;

		double ast = 0.0;
		if (sky.coordinateSystem == COORDINATE_SYSTEM.HORIZONTAL || toApparent)
			ast = SiderealTime.apparentSiderealTime(time, obs, eph);

		switch (sky.coordinateSystem)
		{
		case EQUATORIAL:
			if (toApparent)
			{
				loc0 = CoordinateSystem.equatorialToHorizontal(loc0, ast, obs, eph, toApparent, fast);
				loc0 = CoordinateSystem.horizontalToEquatorial(loc0, ast, obs.getLatitudeRad(), fast);
			}
			break;
		case ECLIPTIC:
			if (toApparent)
			{
				loc0 = CoordinateSystem.equatorialToHorizontal(loc0, ast, obs, eph, toApparent, fast);
				loc0 = CoordinateSystem.horizontalToEquatorial(loc0, ast, obs.getLatitudeRad(), fast);
			}
			if (obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != TARGET.NOT_A_PLANET) loc0 = Ephem.getPositionFromEarth(loc0, time, obs, eph);
			loc0 = CoordinateSystem.equatorialToEcliptic(loc0, obliquity, true);
			break;
		case GALACTIC:
			if (toApparent)
			{
				loc0 = CoordinateSystem.equatorialToHorizontal(loc0, ast, obs, eph, toApparent, fast);
				loc0 = CoordinateSystem.horizontalToEquatorial(loc0, ast, obs.getLatitudeRad(), fast);
			}
			if (obs.getMotherBody() != TARGET.EARTH && obs.getMotherBody() != TARGET.NOT_A_PLANET) loc0 = Ephem.getPositionFromEarth(loc0, time, obs, eph);
			double d = eph.getEpoch(jd);
			if (d != Constant.J2000) {
				if (fast) {
					loc0 = LocationElement.parseRectangularCoordinatesFast(Precession.precessToJ2000(jd, loc0.getRectangularCoordinates(), eph));
				} else {
					loc0 = LocationElement.parseRectangularCoordinates(Precession.precessToJ2000(jd, loc0.getRectangularCoordinates(), eph));
				}
			}
			loc0 = CoordinateSystem.equatorialToGalactic(loc0, true);
			break;
		case HORIZONTAL:
			loc0 = CoordinateSystem.equatorialToHorizontal(loc0, ast, obs, eph, toApparent, fast);
			break;
		}

		return loc0;
	}

	private Object readImage(String icon) {
		int index = DataSet.getIndex(imgs, icon);
		Object img;
		if (index < 0 || g.renderingToExternalGraphics()) {
			// Wait until the images are loaded
			img = g.getImage(FileIO.DATA_ICONS_DIRECTORY + icon);
			g.waitUntilImagesAreRead(new Object[] {img});

			String iconok = "sat.png, hst.png, iss.png, tiangong1.png, pioneer.png, voyager.png, galileo.png, sun.png, Sun2.png, Sun3.png";
			if ((render.getColorMode() == SkyRenderElement.COLOR_MODE.BLACK_BACKGROUND || render.getColorMode() == SkyRenderElement.COLOR_MODE.NIGHT_MODE)
					&& !icon.startsWith("moon") && iconok.indexOf(icon) < 0) {
				try {
					if (render.getColorMode() == COLOR_MODE.NIGHT_MODE) {
						img = g.getColorInvertedImage(img, true, false, false);
					} else {
						img = g.getColorInvertedImage(img);
					}
				} catch (Exception exc) {
					Logger.log(LEVEL.ERROR, "Error processing icon "+icon+". Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
				}
			}

			if (render.getColorMode() == COLOR_MODE.NIGHT_MODE && (icon.startsWith("moon") || icon.toLowerCase().startsWith("sun"))) {
				int arr[] = g.getImageAsPixels(img), s[] = g.getSize(img);
				for (int i=0; i<arr.length; i++) {
					int r = (arr[i]>>16)&255;
					arr[i] = 255<<24 | r<<16;
				}
				img = g.getImage(s[0], s[1], arr);
			}

			if (render.telescope.invertHorizontal || render.telescope.invertVertical) { // || scale != 1) {
				int size[] = g.getSize(img);
				int w = size[0];
				int h = size[1];
				if (icon.startsWith("moon")) {
					if (render.telescope.invertHorizontal) w = -w;
					if (render.telescope.invertVertical) h = -h;
				}
				img = g.getScaledImage(img, w, h, true, true);
			}
			if (!g.renderingToExternalGraphics()) {
				images.add(img);
				imgs = DataSet.addStringArray(imgs, icon);
			}
		} else {
			img = images.get(index);
		}
		return img;
	}
	private void drawIcon(String icon, float px, float py, float ang, float scale) throws Exception
	{
		Object img = readImage(icon);

		int size[] = g.getSize(img);
		float radius_x = size[0] * scale / 2f;
		float radius_y = size[1] * scale / 2f;
		if (g.renderingToExternalGraphics()) {
			if (ang != 0f) {
				g.drawImage(g.getRotatedAndScaledImage(img, radius_x/scale, radius_y/scale, ang, 1.0f, 1.0f), px-radius_x, py-radius_y, scale, scale);
			} else {
				g.drawImage(img, px-radius_x+1, py-radius_y+1, scale, scale);
			}
		} else {
			if (ang != 0f) {
				// getScaledImage gives better quality compared to getRotatedAndScaledImage when scaling, so it is better
				// to first rotate using getRotatedAndScaledImage and then scale down using getScaledImage. For vector
				// graphics output (above) both things are done simultaneously to use the icon at full resolution.
				//g.drawImage(g.getRotatedAndScaledImage(img, radius_x/scale, radius_y/scale, ang, 1.0f, 1.0f), px-radius_x, py-radius_y, scale, scale);
				g.drawImage(g.getScaledImage(g.getRotatedAndScaledImage(img, radius_x/scale, radius_y/scale, ang, 1.0f, 1.0f), (int)(radius_x*2), (int)(radius_y*2), false, false), px-radius_x, py-radius_y);
			} else {
				g.drawImage(g.getScaledImage(img, (int)(radius_x*2), (int)(radius_y*2), false, false), px-radius_x+1, py-radius_y+1);
			}
		}
	}

	private void drawPlanetaryIcon(TARGET target, float px, float py, float ang, float scale) throws Exception
	{
		//String icon = target.getEnglishName()+".png";
		//drawIcon(icon, px, py, ang, scale); // For Android

		// Vector graphics mode (AWT)
		g.setColor(0, 0, 0, 255);
		if (render.getColorMode() == SkyRenderElement.COLOR_MODE.BLACK_BACKGROUND) g.setColor(255, 255, 255, 255);
		if (render.getColorMode() == SkyRenderElement.COLOR_MODE.NIGHT_MODE) g.setColor(255, 128, 128, 255);
		if (render.telescope.invertHorizontal) px = render.width-1-px;
		if (render.telescope.invertVertical) py = render.height-1-py;
		if (target == TARGET.SUN) scale *= 2;
		SVGPlanets.drawIcon(target, g.getDirectGraphics(), px, py, scale);
		if (g.getDirectGraphics2() != null) {
			SVGPlanets.drawIcon(target, g.getDirectGraphics2(), px, py, scale);
			if (g.renderingToExternalGraphics()) { // Draw two more times to reduce transparency problem in iText
				SVGPlanets.drawIcon(target, g.getDirectGraphics2(), px, py, scale);
				SVGPlanets.drawIcon(target, g.getDirectGraphics2(), px, py, scale);
			}
		}

		// For an image-based method, not vector graphics
/*		int index = DataSet.getIndex(imgs, icon);
		Object img;
		if (index < 0) {
			// Wait until the images are loaded
			img = SVGPlanets.drawIcon(target, scale);
			if (render.getColorMode() == SkyRenderElement.COLOR_MODE.BLACK_BACKGROUND) {
				try {
					img = g.getColorInvertedImage(img);
				} catch (Exception exc) {
					Logger.log(LEVEL.ERROR, "Error processing icon "+icon+". Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
				}
			}

			images.add(img);
			imgs = DataSet.addStringArray(imgs, icon);
		} else {
			img = images.get(index);
		}

		float radius_x = (float) (g.getWidth(img) / 2);
		float radius_y = (float) (g.getHeight(img) / 2);
		g.drawImage(img, px-radius_x, py-radius_y);
*/
	}

	/**
	 * Searches for a deep sky object given it's name.
	 *
	 * @param obj_name Name of the object as given in the catalog.
	 * @return Right ascension and declination, or null object if not found.
	 * @throws JPARSECException If the method fails.
	 */
	public LocationElement searchDeepSkyObject(String obj_name)
			throws JPARSECException
	{
		boolean isMessier = false;
		if (!obj_name.equals("") && obj_name.length() > 1)
			isMessier = obj_name.startsWith("M") && DataSet.isDoubleStrictCheck(obj_name.substring(1).trim());

		if (obj_name.toUpperCase().equals("M1") || obj_name.toUpperCase().equals("M 1")) obj_name = "NGC 1952";
		int cal = obj_name.toUpperCase().indexOf("CALDWELL");
		if (cal >= 0) {
			obj_name = obj_name.substring(cal+8).trim();
			cal = obj_name.indexOf(" ");
			if (cal > 0) obj_name = obj_name.substring(0, cal);
			obj_name = "CALDWELL"+obj_name;
			isMessier = true;
		} else {
			if (obj_name.toUpperCase().startsWith("C") && DataSet.isDoubleStrictCheck(obj_name.substring(1).trim())) {
				obj_name = "CALDWELL"+obj_name.substring(1).trim();
				isMessier = true;
			}
		}

		ArrayList<Object> objects = null;
		Object o = null; //DataBase.getData("objectsJ2000", null, true);
		if (db_objects >= 0) {
			o = DataBase.getData(db_objects);
		} else {
			o = DataBase.getData("objects", threadID, true);
		}
//		if (o == null) o = populate();
		if (o != null) objects = new ArrayList<Object>(Arrays.asList((Object[]) o));
		if (objects == null) return null;

		ArrayList<Object> objectsJ2000 = null;
		o = DataBase.getData("objectsJ2000", null, true);
		if (o == null) o = populate(false);
		objectsJ2000 = new ArrayList<Object>(Arrays.asList((Object[]) o));

		LocationElement ephem = null;
		String obj_name2 = obj_name.toLowerCase();
		LocationElement locF;
		for (int i = 0; i < objects.size(); i++)
		{
			Object[] obj = (Object[]) objects.get(i);
			if (obj.length <= 3) {
				locF = (LocationElement) obj[1];
				obj = (Object[]) objectsJ2000.get((Integer) obj[0]);
			} else {
				locF = (LocationElement) obj[3];
			}

			String messier = (String) obj[1];
			messier = DataSet.replaceAll(messier, " ", "", true);
			if (isMessier) {
				if (obj_name.equals(messier.trim())) {
					LocationElement loc = projection.toEquatorialPosition(locF, false);
					ephem = loc.clone();
					break;
				}
			} else {
				String name = (String) obj[0];
				if (DataSet.isDoubleStrictCheck(name) || (name.length() > 4 && DataSet.isDoubleStrictCheck(name.substring(0, 4)))) {
					if (name.length() < 8 || name.indexOf(" ") > 0) name = "NGC " + name;
				} else {
					try {
						if (name.startsWith("I.")) name = "IC "+name.substring(2);
					} catch (Exception exc2) {	}
				}
	
				String com = "";
				String comments = (String) obj[7];
				int pp = comments.indexOf("Popular name:");
				if (pp>=0) com = comments.substring(pp+14).trim();
				if (!messier.equals("")) messier = " "+messier;
	
				if (obj_name2.equals(name.toLowerCase()) || obj_name.equals(messier.trim()) || obj_name.equals(name+messier) ||
						obj_name.equals(name+messier+" - "+com) ||
						(name.indexOf(" ") > 0 && (name.substring(name.indexOf(" ")).trim()+messier+" - "+com).indexOf(obj_name) == 0)) {
					LocationElement loc = projection.toEquatorialPosition(locF, false);
					ephem = loc.clone();
					break;
				}
			}
		}
		if (ephem == null) {
			for (int i = 0; i < objects.size(); i++)
			{
				Object[] obj = (Object[]) objects.get(i);
				if (obj.length <= 3) {
					locF = (LocationElement) obj[1];
					obj = (Object[]) objectsJ2000.get((Integer) obj[0]);
				} else {
					locF = (LocationElement) obj[3];
				}

				String com = "";
				String comments = (String) obj[7];
				int pp = comments.indexOf("Popular name:");
				if (pp>=0) com = comments.substring(pp+14).trim();

				if (com.toLowerCase().indexOf(obj_name2) >= 0) {
					LocationElement loc = projection.toEquatorialPosition(locF, false);
					ephem = loc.clone();
					break;
				}
			}
		}

		if (ephem == null) return null;
		if (projection.obs.getMotherBody() != TARGET.NOT_A_PLANET && projection.eph.isTopocentric && render.drawSkyCorrectingLocalHorizon
				&& render.coordinateSystem != COORDINATE_SYSTEM.HORIZONTAL)
		{
			ephem = CoordinateSystem.equatorialToHorizontal(ephem, projection.time, projection.obs, projection.eph);
			ephem.setLatitude(Ephem.getGeometricElevation(projection.eph, projection.obs, ephem.getLatitude()));
			ephem = CoordinateSystem.horizontalToEquatorial(ephem, projection.time, projection.obs, projection.eph);
		}

		return ephem;
	}

	/**
	 * Searches for a deep sky object given it's name.
	 *
	 * @param obj_name Name of the object as given in the catalog.
	 * @return Two objects with the array of names and the array of objects locations.
	 * @throws JPARSECException If the method fails.
	 */
	public Object[] searchDeepSkyObjects(String obj_name)
			throws JPARSECException
	{
		boolean isMessier = false;
		if (!obj_name.equals("") && obj_name.length() > 1)
			isMessier = obj_name.startsWith("M") && DataSet.isDoubleStrictCheck(obj_name.substring(1).trim());

		if (obj_name.toUpperCase().equals("M1") || obj_name.toUpperCase().equals("M 1")) obj_name = "NGC 1952";
		int cal = obj_name.toUpperCase().indexOf("CALDWELL");
		if (cal >= 0) {
			obj_name = obj_name.substring(cal+8).trim();
			cal = obj_name.indexOf(" ");
			if (cal > 0) obj_name = obj_name.substring(0, cal);
			obj_name = "CALDWELL"+obj_name;
			isMessier = true;
		} else {
			if (obj_name.toUpperCase().startsWith("C") && DataSet.isDoubleStrictCheck(obj_name.substring(1).trim())) {
				obj_name = "CALDWELL"+obj_name.substring(1).trim();
				isMessier = true;
			}
		}

		ArrayList<Object> objects = null;
		Object o = null; //DataBase.getData("objectsJ2000", null, true);
		if (db_objects >= 0) {
			o = DataBase.getData(db_objects);
		} else {
			o = DataBase.getData("objects", threadID, true);
		}
//		if (o == null) o = populate();
		if (o != null) objects = new ArrayList<Object>(Arrays.asList((Object[]) o));
		if (objects == null) return null;

		ArrayList<Object> objectsJ2000 = null;
		o = DataBase.getData("objectsJ2000", null, true);
		if (o == null) o = populate(false);
		objectsJ2000 = new ArrayList<Object>(Arrays.asList((Object[]) o));

		LocationElement ephem[] = new LocationElement[0];
		String names[] = new String[0];
		String obj_name2 = obj_name.toLowerCase();
		LocationElement locF;
		for (int i = 0; i < objects.size(); i++)
		{
			Object[] obj = (Object[]) objects.get(i);
			if (obj.length <= 3) {
				locF = (LocationElement) obj[1];
				obj = (Object[]) objectsJ2000.get((Integer) obj[0]);
			} else {
				locF = (LocationElement) obj[3];
			}

			String messier = (String) obj[1];
			messier = DataSet.replaceAll(messier, " ", "", true);
			if (isMessier) {
				if (obj_name.equals(messier.trim())) {
					LocationElement loc = projection.toEquatorialPosition(locF, false);
					ephem = (LocationElement[]) DataSet.addObjectArray(ephem, new LocationElement[] {loc.clone()});
					names = DataSet.addStringArray(names, messier);
					break;
				}
			} else {
				String name = (String) obj[0];
				if (DataSet.isDoubleStrictCheck(name) || (name.length() > 4 && DataSet.isDoubleStrictCheck(name.substring(0, 4)))) {
					if (name.length() < 8 || name.indexOf(" ") > 0) name = "NGC " + name;
				} else {
					try {
						if (name.startsWith("I.")) name = "IC "+name.substring(2);
					} catch (Exception exc2) {	}
				}
	
				String com = "";
				String comments = (String) obj[7];
				int pp = comments.indexOf("Popular name:");
				if (pp>=0) com = comments.substring(pp+14).trim();
				if (!messier.equals("")) messier = " "+messier;
	
				if (obj_name2.equals(name.toLowerCase()) || obj_name.equals(messier.trim()) || obj_name.equals(name+messier) ||
						obj_name.equals(name+messier+" - "+com) ||
						(name.indexOf(" ") > 0 && (name.substring(name.indexOf(" ")).trim()+messier+" - "+com).indexOf(obj_name) == 0)) {
					LocationElement loc = projection.toEquatorialPosition(locF, false);
					ephem = (LocationElement[]) DataSet.addObjectArray(ephem, new LocationElement[] {loc.clone()});
					names = DataSet.addStringArray(names, name);
					break;
				}
			}
		}
		for (int i = 0; i < objects.size(); i++)
		{
			Object[] obj = (Object[]) objects.get(i);
			if (obj.length <= 3) {
				locF = (LocationElement) obj[1];
				obj = (Object[]) objectsJ2000.get((Integer) obj[0]);
			} else {
				locF = (LocationElement) obj[3];
			}

			String com = "";
			String comments = (String) obj[7];
			int pp = comments.indexOf("Popular name:");
			if (pp>=0) com = comments.substring(pp+14).trim();

			if (com.toLowerCase().indexOf(obj_name2) >= 0) {				
				String name = (String) obj[0];
				if (DataSet.isDoubleStrictCheck(name) || (name.length() > 4 && DataSet.isDoubleStrictCheck(name.substring(0, 4)))) {
					if (name.length() < 8 || name.indexOf(" ") > 0) name = "NGC " + name;
				} else {
					try {
						if (name.startsWith("I.")) name = "IC "+name.substring(2);
					} catch (Exception exc2) {	}
				}
				
				if (DataSet.getIndex(names, name) < 0) {
					LocationElement loc = projection.toEquatorialPosition(locF, false);
					ephem = (LocationElement[]) DataSet.addObjectArray(ephem, new LocationElement[] {loc.clone()});
					names = DataSet.addStringArray(names, name);
				}
				//break;
			}
		}

		if (ephem == null || ephem.length == 0) return null;
		if (projection.obs.getMotherBody() != TARGET.NOT_A_PLANET && projection.eph.isTopocentric && render.drawSkyCorrectingLocalHorizon
				&& render.coordinateSystem != COORDINATE_SYSTEM.HORIZONTAL)
		{
			for (int i=0; i<ephem.length; i++) {
				ephem[i] = CoordinateSystem.equatorialToHorizontal(ephem[i], projection.time, projection.obs, projection.eph);
				ephem[i].setLatitude(Ephem.getGeometricElevation(projection.eph, projection.obs, ephem[i].getLatitude()));
				ephem[i] = CoordinateSystem.horizontalToEquatorial(ephem[i], projection.time, projection.obs, projection.eph);
			}
		}

		return new Object[] {names, ephem};
	}
	
	/**
	 * Searches for a deep sky object given it's name.
	 *
	 * @param obj_name Name of the object as given in the catalog.
	 * @return The main name in the catalog, NGC or IC name almost always.
	 * @throws JPARSECException If the method fails.
	 */
	public static String searchDeepSkyObjectReturnMainName(String obj_name)
			throws JPARSECException
	{
		if (DataSet.isDoubleStrictCheck(obj_name)) obj_name = "NGC " + obj_name;
		if (obj_name.toUpperCase().equals("M1") || obj_name.toUpperCase().equals("M 1")) obj_name = "NGC 1952";
		int cal = obj_name.toUpperCase().indexOf("CALDWELL");
		if (cal >= 0) {
			obj_name = obj_name.substring(cal+8).trim();
			cal = obj_name.indexOf(" ");
			if (cal > 0) obj_name = obj_name.substring(0, cal);
			obj_name = "CALDWELL"+obj_name;
		} else {
			if (obj_name.toUpperCase().startsWith("C") && DataSet.isDoubleStrictCheck(obj_name.substring(1).trim()))
				obj_name = "CALDWELL"+obj_name.substring(1).trim();
		}

		ArrayList<Object> objects = null;
		Object o = DataBase.getData("objectsJ2000", null, true);
		if (o == null) o = populate(false);
		if (o != null) objects = new ArrayList<Object>(Arrays.asList((Object[]) o));
		if (objects == null) return null;

		String out = null;
		String obj_name2 = obj_name.toLowerCase();
		for (int i = 0; i < objects.size(); i++)
		{
			Object[] obj = (Object[]) objects.get(i);
			String messier = (String) obj[1];
			String name = (String) obj[0];
			messier = DataSet.replaceAll(messier, " ", "", true);
			if (DataSet.isDoubleStrictCheck(name) || (name.length() > 4 && DataSet.isDoubleStrictCheck(name.substring(0, 4)))) {
				if (name.length() < 8 || name.indexOf(" ") > 0) name = "NGC " + name;
			} else {
				try {
					if (name.startsWith("I.")) name = "IC "+name.substring(2);
				} catch (Exception exc2) {	}
			}

			String com = "";
			String comments = (String) obj[7];
			int pp = comments.indexOf("Popular name:");
			if (pp>=0) com = comments.substring(pp+14).trim();
			if (!messier.equals("")) messier = " "+messier;

			if (obj_name2.equals(name.toLowerCase()) || obj_name.equals(messier.trim()) || obj_name.equals(name+messier) ||
					obj_name.equals(name+messier+" - "+com) ||
					(name.indexOf(" ") > 0 && (name.substring(name.indexOf(" ")).trim()+messier+" - "+com).indexOf(obj_name) == 0)) {
				out = name;
				break;
			}
		}
		if (out == null) {
			for (int i = 0; i < objects.size(); i++)
			{
				Object[] obj = (Object[]) objects.get(i);

				String com = "";
				String comments = (String) obj[7];
				int pp = comments.indexOf("Popular name:");
				if (pp>=0) com = comments.substring(pp+14).trim();

				if (com.toLowerCase().indexOf(obj_name2) >= 0) {
					String name = (String) obj[0];
					if (DataSet.isDoubleStrictCheck(name) || (name.length() > 4 && DataSet.isDoubleStrictCheck(name.substring(0, 4)))) {
						if (name.length() < 8 || name.indexOf(" ") > 0) name = "NGC " + name;
					} else {
						try {
							if (name.startsWith("I.")) name = "IC "+name.substring(2);
						} catch (Exception exc2) {	}
					}
					out = name;
					break;
				}
			}
		}

		return out;
	}

	private static int searchStar(String object, ReadFile re) {
		int index = -1;
		Object o[] = re.getReadElements();
		if (o == null) return index;
		for (int i = 0; i < o.length; i++)
		{
			StarData sd = null;
			if (o[i] instanceof StarElement) {
				StarElement se = (StarElement)o[i];
				if (se == null) continue;
				String name = ""+se.name;
				if (name.equals(object) || (name.indexOf("("+object+")") >= 0))
				{
					index = i;
					break;
				} else {
					if (name.toLowerCase().indexOf(object.toLowerCase()) >= 0)
					{
						index = i;
					}
				}
			} else {
				sd = (StarData)o[i];
				if (sd == null) continue;
				String name = ""+sd.sky2000;
				if (sd.nom2 != null) name += " ("+sd.nom2+")";
				if (sd.greek != '\u0000') name += " ("+sd.greek+")";
				if (name.equals(object) || (name.indexOf("("+object+")") >= 0))
				{
					index = i;
					break;
				} else {
					if (name.toLowerCase().indexOf(object.toLowerCase()) >= 0)
					{
						index = i;
					}
				}
			}
		}
		return index;
	}

	static int getStar(String object, ReadFile re) throws JPARSECException
	{
		// Search object
		int my_star = searchStar(object, re);

		if (my_star < 0)
		{
			ArrayList<String> names = null;
			Object o = DataBase.getData("starNames0", true);
			if (o == null) {
				names = ReadFile.readResource(FileIO.DATA_SKY_DIRECTORY + "star_names.txt");
				DataBase.addData("starNames0", Thread.currentThread().getName(), DataSet.arrayListToStringArray(names), true);
			} else {
				names = new ArrayList<String>(Arrays.asList((String[])o));
			}

			// Correct for double stars
			String wrongNames[] = new String [] {
					"Alp Cen","The Eri","Alp Cru","Alp Cru","Gam And","Gam Leo","Bet Sco","Alp Lib","Alp CVn","Bet Cyg","Bet Cap","Alp Her","Alp Cap","Psi Dra","Gam Ari"
			};
			String okNames[] = new String[] {
					"Alp1 Cen","The1 Eri","Alp1 Cru","Alp1 Cru","Gam1 And","Gam1 Leo","Bet1 Sco","Alp1 Lib","Alp1 CVn","Bet1 Cyg","Bet1 Cap","Alp1 Her","Alp1 Cap","Psi1 Dra","Gam1 Ari"
			};

			// Support for proper star names
			for (int n = 0; n < names.size(); n++)
			{
				String line = names.get(n);
				int aa = line.toLowerCase().indexOf(object.toLowerCase());
				if (aa >= 0)
				{
					String proper_name = FileIO.getField(1, line, ";", true);
					
					// Correct for double stars
					int index = DataSet.getIndex(wrongNames, proper_name);
					if (index >= 0) proper_name = okNames[index];
					
					my_star = searchStar(proper_name, re);
					if (FileIO.getField(2, line, ";", true).toLowerCase().equals(object.toLowerCase())) break;
					if (FileIO.getField(3, line, ";", true).toLowerCase().equals(object.toLowerCase())) break;
				}
			}
		}

		return my_star;
	}


	/**
	 * Searches for certain star by it's name.
	 *
	 * @param name Name of the star. For instance 'Alp UMi', 'Polaris' or Sky2000
	 *        catalog number.
	 * @return Ephemeris for the star, or null object if not found.
	 * @throws JPARSECException If an error occurs.
	 */
	public StarEphemElement searchStar(String name) throws JPARSECException
	{
		if (re_star == null || name == null) return null;

		int my_star = getStar(name, re_star);
		if (my_star < 0) return null;
		return calcStar(my_star, false);
	}

	private static int[] searchStars(String object, ReadFile re) {
		int index[] = new int[0];
		Object o[] = re.getReadElements();
		if (o == null) return null;
		for (int i = 0; i < o.length; i++)
		{
			StarData sd = null;
			if (o[i] instanceof StarElement) {
				StarElement se = (StarElement)o[i];
				if (se == null) continue;
				String name = ""+se.name;
				if (name.equals(object) || (name.indexOf("("+object+")") >= 0))
				{
					index = DataSet.addIntegerArray(index, new int[] {i});
					break;
				} else {
					if (name.toLowerCase().indexOf(object.toLowerCase()) >= 0)
					{
						index = DataSet.addIntegerArray(index, new int[] {i});
					}
				}
			} else {
				sd = (StarData)o[i];
				if (sd == null) continue;
				String name = ""+sd.sky2000;
				if (sd.nom2 != null) name += " ("+sd.nom2+")";
				if (sd.greek != '\u0000') name += " ("+sd.greek+")";
				if (name.equals(object) || (name.indexOf("("+object+")") >= 0))
				{
					index = DataSet.addIntegerArray(index, new int[] {i});
					break;
				} else {
					if (name.toLowerCase().indexOf(object.toLowerCase()) >= 0)
					{
						index = DataSet.addIntegerArray(index, new int[] {i});
					}
				}
			}
		}
		return index;
	}
	
	static int[] getStars(String object, ReadFile re) throws JPARSECException
	{
		// Search object
		int my_star[] = searchStars(object, re);

		if (my_star == null) my_star = new int[0];

			ArrayList<String> names = null;
			Object o = DataBase.getData("starNames0", true);
			if (o == null) {
				names = ReadFile.readResource(FileIO.DATA_SKY_DIRECTORY + "star_names.txt");
				DataBase.addData("starNames0", Thread.currentThread().getName(), DataSet.arrayListToStringArray(names), true);
			} else {
				names = new ArrayList<String>(Arrays.asList((String[])o));
			}

			// Correct for double stars
			String wrongNames[] = new String [] {
					"Alp Cen","The Eri","Alp Cru","Alp Cru","Gam And","Gam Leo","Bet Sco","Alp Lib","Alp CVn","Bet Cyg","Bet Cap","Alp Her","Alp Cap","Psi Dra","Gam Ari"
			};
			String okNames[] = new String[] {
					"Alp1 Cen","The1 Eri","Alp1 Cru","Alp1 Cru","Gam1 And","Gam1 Leo","Bet1 Sco","Alp1 Lib","Alp1 CVn","Bet1 Cyg","Bet1 Cap","Alp1 Her","Alp1 Cap","Psi1 Dra","Gam1 Ari"
			};

			// Support for proper star names
			for (int n = 0; n < names.size(); n++)
			{
				String line = names.get(n);
				int aa = line.toLowerCase().indexOf(object.toLowerCase());
				if (aa >= 0)
				{
					String proper_name = FileIO.getField(1, line, ";", true);
					
					// Correct for double stars
					int index = DataSet.getIndex(wrongNames, proper_name);
					if (index >= 0) proper_name = okNames[index];
					
					boolean first = false;
					if (FileIO.getField(2, line, ";", true).toLowerCase().equals(object.toLowerCase())) first = true;
					if (FileIO.getField(3, line, ";", true).toLowerCase().equals(object.toLowerCase())) first = true;
					int ss = searchStar(proper_name, re);
					if (ss >= 0) {
						if (first) {
							my_star = DataSet.addIntegerArray(new int[] {ss}, my_star);						
						} else {
							my_star = DataSet.addIntegerArray(my_star, new int[] {ss});
						}
					}
				}
			}

		return my_star;
	}
	/**
	 * Searches for certain stars by it's name.
	 *
	 * @param name Name of the star. For instance 'Alp UMi', 'Polaris' or Sky2000
	 *        catalog number.
	 * @return Ephemeris for the stars, or null object if not found.
	 * @throws JPARSECException If an error occurs.
	 */
	public StarEphemElement[] searchStars(String name) throws JPARSECException
	{
		if (re_star == null || name == null) return null;

		int my_star[] = getStars(name, re_star);
		if (my_star == null) return null;
		StarEphemElement out[] = new StarEphemElement[my_star.length];
		for (int i=0; i<out.length; i++) {
			out[i] = calcStar(my_star[i], false);
			if (out[i] == null) {
				out = (StarEphemElement[]) DataSet.deleteIndex(out, i);
				continue;
			}
			String pn = getStarProperName(out[i].name);
			if (pn != null) out[i].name = pn;
		}
		return out;
	}
	
	/**
	 * Return ephemeris for a given star.
	 * @param my_star ID value for the star, retrieved with {@linkplain RenderSky#getClosestStarInScreenCoordinates(int, int, boolean)}.
	 * @param fullDataAndPrecision True to return also rise, set, transit times.
	 * @return Ephemeris, or null if the star is not found.
	 * @throws JPARSECException If an error occurs.
	 */
	public StarEphemElement calcStar(int my_star, boolean fullDataAndPrecision)
			throws JPARSECException
	{
		Object readStars[] = re_star.getReadElements();
		if (readStars == null || my_star >= readStars.length) return null;
		StarData star = (StarData) readStars[my_star];
		if (star == null || star.loc == null) return null;
		if (maxStars > -1 && my_star >= maxStars) return null;
		float p = 0;
		double az = 0, el = 0;
		StarEphemElement se = new StarEphemElement(star.ra, star.dec, star.loc.getRadius(), star.mag[star.mag.length-1], p, az, el);
		String name = ""+star.sky2000;
		if (star.nom2 != null) name += " ("+star.nom2+")";
		if (star.greek != '\u0000') name += " ("+star.greek+")";
		se.name = name;
		LocationElement locE = new LocationElement(star.ra, star.dec, 1.0);
		if (projection.obs.getMotherBody() != TARGET.NOT_A_PLANET && projection.obs.getMotherBody() != TARGET.EARTH)
			locE = projection.getPositionFromEarth(locE, true);

		se.constellation = Constellation.getConstellationName(locE.getLongitude(), locE.getLatitude(), jd, projection.eph);
		EphemElement ephem = new EphemElement();
  		ephem.angularRadius = 0;
  		ephem.rightAscension = se.rightAscension;
  		ephem.declination = se.declination;
  		ephem = Ephem.horizontalCoordinates(projection.time, projection.obs, projection.eph, ephem);
  		se.azimuth = ephem.azimuth;
  		se.elevation = ephem.elevation;
  		se.paralacticAngle = ephem.paralacticAngle;
		if (fullDataAndPrecision && projection.obs.getMotherBody() != TARGET.NOT_A_PLANET) {
	  		EphemerisElement eph = projection.eph.clone();
	  		eph.optimizeForSpeed();
	  		eph.algorithm = null;
	  		ephem = RiseSetTransit.obtainCurrentOrNextRiseSetTransit(projection.time, projection.obs, eph, ephem, RiseSetTransit.TWILIGHT.HORIZON_ASTRONOMICAL_34arcmin);
	  		se.rise = ephem.rise[0];
	  		se.set = ephem.set[0];
	  		se.transit = ephem.transit[0];
	  		se.transitElevation = ephem.transitElevation[0];
		}

		return se;
	}

	/**
	 * Return star parameters for a given star. Proper motions are set to 0.
	 * @param my_star ID value for the star, retrieved with {@linkplain RenderSky#getClosestStarInScreenCoordinates(int, int, boolean)}.
	 * @return Star data.
	 * @throws JPARSECException If an error occurs.
	 */
	public StarElement getStar(int my_star)
			throws JPARSECException
	{
		Object readStars[] = re_star.getReadElements();
		if (readStars == null || my_star >= readStars.length) return null;
		StarData sd = (StarData) readStars[my_star];
		if (sd == null || sd.loc == null) return null;
		if (maxStars > -1 && my_star >= maxStars) return null;
		String name = ""+sd.sky2000;
		if (sd.nom2 != null) name += " ("+sd.nom2+")";
		if (sd.greek != '\u0000') name += " ("+sd.greek+")";
		double p = sd.loc.getRadius();
		if (p != 0) p = 1000.0 / p;
		StarElement se = new StarElement(name, sd.ra, sd.dec, p, sd.mag[sd.mag.length-1], 0.0f, 0.0f, 0.0f, Constant.J2000, FRAME.ICRF);
		se.type = "N";
		String type = "";
		if (sd.doub != null) {
			if (sd.doub[0] > 0) type += ""+sd.doub[0];
			type += ",";
			if (sd.doub[2] > 0) type += ""+sd.doub[2];
			type += ",";
			if (sd.doub[3] > 0) type += ""+sd.doub[3];
			type += ",";
			if (sd.doub[1] > 0) type += ""+(float) (sd.doub[1]*Constant.RAD_TO_DEG);
			se.type = "D";
		}
		if (sd.var != null) {
			if (se.type.equals("D")) {
				se.type = "B";
			} else {
				se.type = "V";
			}
			type += ";"+sd.var[0]+","+sd.var[1]+",";
			if (sd.var[2] > 0) type += ""+sd.var[2];
			type += ",";
			if (sd.var[3] > 0) type += ""+(int)(sd.var[3]+0.5);
		}
		if (!se.type.equals("N")) se.type += ";" + type;
		
		se.spectrum = null;
		if (sd.spi >= 0) {
			String sp = "OBAFGKM";
			int i = sd.spi / 10;
			int ii = sd.spi - i * 10;
			se.spectrum = sp.substring(i, i + 1) + Integer.toString(ii);
		}
		return se;
	}

	/**
	 * Return ephemeris for a given planet or natural satellite.
	 * @param planet ID value for the planet or natural satellite.
	 * @param alsoNonVisible True to return ephemerides of the object even if
	 * it is not visible in the screen.
	 * @param fullDataAndPrecision True to obtain results with maximum accuracy
	 * and including rise, set, transit times.
	 * @return Ephemeris, or null object if the target cannot be found in the screen.
	 * Null is never returned for natural satellites.
	 * @throws JPARSECException If an error occurs.
	 */
	public EphemElement calcPlanet(TARGET planet, boolean alsoNonVisible, boolean fullDataAndPrecision)
			throws JPARSECException
	{
		if (planet == null || planet == TARGET.NOT_A_PLANET || planet == TARGET.Comet || planet == TARGET.Asteroid || planet.isAsteroid()) return null;
		boolean isPlanet = planet.isPlanet();
		if (planet == TARGET.SUN || planet == TARGET.Moon || planet == TARGET.Pluto) isPlanet = true;
		if (!isPlanet && planet.isNaturalSatellite()) {
			EphemElement ephem = null;
			if (render.planetRender != null) {
				if (render.planetRender.moonephem != null) {
					int n = render.planetRender.moonephem.length;
					String search = planet.getName();
					for (int i=0; i<n;i++) {
						MoonEphemElement meph = render.planetRender.moonephem[i];
						if (meph != null) {
							if (meph.name.equals(search)) {
								ephem = EphemElement.parseMoonEphemElement(meph, projection.eph.getEpoch(jd));
							}
						}
					}
				}
			}
			if (ephem == null) {
				EphemerisElement eph = projection.eph.clone();
		  		eph.optimizeForSpeed();
				eph.targetBody = planet;
				eph.algorithm = ALGORITHM.NATURAL_SATELLITE;
				ephem = Ephem.getEphemeris(projection.time, projection.obs, eph, fullDataAndPrecision);
			}
			return ephem;
		} else {
			EphemerisElement eph = projection.eph.clone();
	  		eph.optimizeForSpeed();
			eph.targetBody = planet;
			EphemElement ephem = Ephem.getEphemeris(projection.time, projection.obs, eph, fullDataAndPrecision);
			if (alsoNonVisible) return ephem;

			float pos0[] = projection.project(ephem.getEquatorialLocation(), (int)(ephem.angularRadius * this.pixels_per_radian), true);

			// Don't return Mercury and Venus if they are behind the Sun
			if (ephem.elongation < render.planetRender.ephemSun.angularRadius &&
					planet != TARGET.SUN &&
					ephem.distance > render.planetRender.ephemSun.distance)
				pos0 = Projection.INVALID_POSITION;

			if (!projection.isInvalid(pos0) && this.isInTheScreen((int)pos0[0], (int)pos0[1]))
				return ephem;
			return null;
		}
	}

	/**
	 * Obtain ID constant of the closest star to certain screen position. For a valid
	 * output it is previously necessary to render the sky.
	 *
	 * @param x Horizontal position in the rendering in pixels.
	 * @param y Horizontal position in the rendering in pixels.
	 * @param considerMagLim True to consider the limiting magnitude to identify the star.
	 * @return A array with two values (null if no object is found):<BR>
	 * ID value (integer) of the star.<BR>
	 * Minimum distance to the input position in pixels.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[] getClosestStarInScreenCoordinates(int x, int y, boolean considerMagLim) throws JPARSECException
	{
		return getClosestStarInScreenCoordinates(x, y, considerMagLim ? maglim : 100);
	}

	/**
	 * Obtain ID constant of the closest star to certain screen position. For a valid
	 * output it is previously necessary to render the sky.
	 *
	 * @param x Horizontal position in the rendering in pixels.
	 * @param y Horizontal position in the rendering in pixels.
	 * @param maglim Limiting magnitude up-to identify a star.
	 * @return A array with two values (null if no object is found):<BR>
	 * ID value (integer) of the star.<BR>
	 * Minimum distance to the input position in pixels.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[] getClosestStarInScreenCoordinates(int x, int y, double maglim) throws JPARSECException
	{
		if (re_star == null) return null;
		Object[] stars = re_star.getReadElements();
		if (stars == null) return null;

		int n = stars.length;
		if (maxStars != -1) n = maxStars;
		double minDist = -1;
		int closest = -1;
		if (render.telescope.invertHorizontal) x = render.width-1-x;
		if (render.telescope.invertVertical) y = render.height-1-y;
		for (int i=0; i<n;i++)
		{
			if (stars[i] == null) continue;
			StarData sd = (StarData) stars[i];
			float[] pos = sd.pos;
			if (projection.isInvalid(pos)) continue;
			//if (!projection.eph.correctForExtinction && projection.isInvalid(pos)) break;

			double dx = pos[0] - x, dy = pos[1] - y;
			double dist = (dx*dx+dy*dy);
			if (rec.contains(pos[0], pos[1]) && (dist < minDist || minDist == -1.0)) {
				int element = i;
				if (maglim < 100) {
					double mag = sd.mag[sd.mag.length-1];

 					if (mag <= maglim) {
						minDist = dist;
						closest = element;
					}
				} else {
					minDist = dist;
					closest = element;
				}
			}
		}

 		if (closest == -1) return null;
		return new double[] {closest, Math.sqrt(minDist)};
	}
	
	private String t163, t164, t19, t18, t20, t23, t21, t24, t26, t28, t22, t25, t27, t29, t38, t1072, t1073;
	private String objt[] = new String[8], mont[] = new String[12];
	private void setDefaultStrings() {
		t163 = Translate.translate(163).toLowerCase();
		t164 = Translate.translate(164).toLowerCase();
		for (int type = 0; type <= 7; type ++) {
			objt[type] = Translate.translate(types2Int[type]).toLowerCase();
		}
		t19 = Translate.translate(19);
		t18 = Translate.translate(18);
		t20 = Translate.translate(20);
		t23 = Translate.translate(23);
		for (int type = 0; type <= 11; type ++) {
			mont[type] = Translate.translate(41 + type);
		}
		t21 = Translate.translate(21);
		t24 = Translate.translate(24);
		t26 = Translate.translate(26);
		t28 = Translate.translate(28);
		t22 = Translate.translate(22);
		t25 = Translate.translate(25);
		t27 = Translate.translate(27);
		t29 = Translate.translate(29);
		t38 = Translate.translate(38);
		t1072 = Translate.translate(1072);
		t1073 = Translate.translate(1073);
	}

	/**
	 * Constructor to configure sky render process for planetary rendering purposes.
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @throws JPARSECException If an error occurs.
	 */
	RenderSky (TimeElement time, ObserverElement obs, EphemerisElement eph,
			PlanetRenderElement r)
	throws JPARSECException {
		jd = jparsec.time.TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		try { lst = SiderealTime.apparentSiderealTime(time, obs, eph); } catch (Exception exc) {}
		this.setJupiterGRSLongitude(MainEvents.getJupiterGRSLongitude(jd), 2);
		render = new SkyRenderElement();
		render.telescope = r.telescope;
		field = (float) render.telescope.getField();
		field0 = field;
		pixels_per_radian = render.width / this.field;
		pixels_per_degree = pixels_per_radian / (float) Constant.RAD_TO_DEG;
		obsLat = 0;
		try { obsLat = obs.getLatitudeRad(); } catch (Exception exc) {}
		rec = null;
		projection = new Projection(time, obs, eph, render, field, this.getXCenter(), this.getYCenter());
		setDefaultStrings();
	}

	/**
	 * Constructor to configure sky render process. A necessary previous step to renderize.
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param r Sky render object.
	 * @throws JPARSECException If an error occurs.
	 */
	public RenderSky (TimeElement time, ObserverElement obs, EphemerisElement eph,
			SkyRenderElement r)
	throws JPARSECException {
		this.render = r;
		field = (float) render.telescope.getField();
		field0 = field;
		pixels_per_radian = render.width / this.field;
		pixels_per_degree = pixels_per_radian / (float) Constant.RAD_TO_DEG;
		obsLat = 0;
		try { obsLat = obs.getLatitudeRad(); } catch (Exception exc) {}
		jd = jparsec.time.TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		try { lst = SiderealTime.apparentSiderealTime(time, obs, eph); } catch (Exception exc) {}
		this.setJupiterGRSLongitude(jparsec.ephem.event.MainEvents.getJupiterGRSLongitude(jd), 2);
		if (render.drawCoordinateGridFont.getSize() != 15) {
			graphMarginX = 1+(28*render.drawCoordinateGridFont.getSize())/15;
			leyendMargin = 1+(51*render.drawCoordinateGridFont.getSize())/15;
			graphMarginY = 1+(38*render.drawCoordinateGridFont.getSize())/15;
		}
		if (!render.drawExternalGrid && !render.drawCoordinateGrid) graphMarginX = graphMarginY = 0;
		int recy = 0, recx = graphMarginX, recw = render.width-graphMarginX, rech = render.height-leyendMargin-graphMarginY;
		if (render.drawLeyend == LEYEND_POSITION.TOP) recy = leyendMargin;
		if (render.drawLeyend == LEYEND_POSITION.LEFT) recx += leyendMargin;
		if (render.drawLeyend == LEYEND_POSITION.RIGHT || render.drawLeyend == LEYEND_POSITION.LEFT) {
			recw -= leyendMargin;
			rech += leyendMargin;
		}
		rec = new Rectangle(recx, recy, recw, rech);

        if (render.drawLeyend == LEYEND_POSITION.NO_LEYEND) {
        	leyendMargin = 0;
    		rec = new Rectangle(graphMarginX, leyendMargin, render.width-graphMarginX, render.height-leyendMargin-graphMarginY);
        }
		projection = new Projection(time, obs, eph, render, field, this.getXCenter(), this.getYCenter());
		refz = render.anaglyphMode.getReferenceZ();
		foregroundDist = 0;
		conlimDist = 0;
		axesDist = refz;
		milkywayDist = 0;
		nebulaDist = refz/4;
		if (render.anaglyphMode.isReal3D()) {
			conlimDist /= 2.0;
			axesDist /= 2.0;
			nebulaDist /= 2.0;
		}
		threadID = "RenderSky";
		String t[] = DataBase.getThreads();
		int n = DataSet.getIndex(t, threadID);
		if (n >= 0) {
			int index = 0;
			while(true) {
				threadID = "RenderSky"+index;
				n = DataSet.getIndex(t, threadID);
				if (n < 0) break;
				index ++;
			}
			if (Configuration.MAX_RENDERSKY_DATABASE_CACHE_SIZE > 0 && 
					index >= Configuration.MAX_RENDERSKY_DATABASE_CACHE_SIZE) {
				int i1 = index - Configuration.MAX_RENDERSKY_DATABASE_CACHE_SIZE;
				DataBase.deleteThreadData("RenderSky");
				for (int i=0; i<=i1; i++) {
					DataBase.deleteThreadData("RenderSky"+i);
				}
			} else {
				DataBase.deleteThreadData("RenderSky"+(index+1));				
			}
		}
		setDefaultStrings();
	}

	/**
	 * Constructor to configure sky render process. A necessary previous step to renderize.
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param r Sky render object.
	 * @param setStrings True to read default string values, something slow in Android.
	 * @throws JPARSECException If an error occurs.
	 */
	public RenderSky (TimeElement time, ObserverElement obs, EphemerisElement eph,
			SkyRenderElement r, boolean setStrings)
	throws JPARSECException {
		this.render = r;
		field = (float) render.telescope.getField();
		field0 = field;
		pixels_per_radian = render.width / this.field;
		pixels_per_degree = pixels_per_radian / (float) Constant.RAD_TO_DEG;
		obsLat = 0;
		try { obsLat = obs.getLatitudeRad(); } catch (Exception exc) {}
		jd = jparsec.time.TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		try { lst = SiderealTime.apparentSiderealTime(time, obs, eph); } catch (Exception exc) {}
		this.setJupiterGRSLongitude(jparsec.ephem.event.MainEvents.getJupiterGRSLongitude(jd), 2);
		if (render.drawCoordinateGridFont.getSize() != 15) {
			graphMarginX = 1+(28*render.drawCoordinateGridFont.getSize())/15;
			leyendMargin = 1+(51*render.drawCoordinateGridFont.getSize())/15;
			graphMarginY = 1+(38*render.drawCoordinateGridFont.getSize())/15;
		}
		if (!render.drawExternalGrid && !render.drawCoordinateGrid) graphMarginX = graphMarginY = 0;
		int recy = 0, recx = graphMarginX, recw = render.width-graphMarginX, rech = render.height-leyendMargin-graphMarginY;
		if (render.drawLeyend == LEYEND_POSITION.TOP) recy = leyendMargin;
		if (render.drawLeyend == LEYEND_POSITION.LEFT) recx += leyendMargin;
		if (render.drawLeyend == LEYEND_POSITION.RIGHT || render.drawLeyend == LEYEND_POSITION.LEFT) {
			recw -= leyendMargin;
			rech += leyendMargin;
		}
		rec = new Rectangle(recx, recy, recw, rech);

        if (render.drawLeyend == LEYEND_POSITION.NO_LEYEND) {
        	leyendMargin = 0;
    		rec = new Rectangle(graphMarginX, leyendMargin, render.width-graphMarginX, render.height-leyendMargin-graphMarginY);
        }
		projection = new Projection(time, obs, eph, render, field, this.getXCenter(), this.getYCenter());
		refz = render.anaglyphMode.getReferenceZ();
		foregroundDist = 0;
		conlimDist = 0;
		axesDist = refz;
		milkywayDist = 0;
		nebulaDist = refz/4;
		if (render.anaglyphMode.isReal3D()) {
			conlimDist /= 2.0;
			axesDist /= 2.0;
			nebulaDist /= 2.0;
		}
		threadID = "RenderSky";
		String t[] = DataBase.getThreads();
		int n = DataSet.getIndex(t, threadID);
		if (n >= 0) {
			int index = 0;
			while(true) {
				threadID = "RenderSky"+index;
				n = DataSet.getIndex(t, threadID);
				if (n < 0) break;
				index ++;
			}
			if (Configuration.MAX_RENDERSKY_DATABASE_CACHE_SIZE > 0 && 
					index >= Configuration.MAX_RENDERSKY_DATABASE_CACHE_SIZE) {
				int i1 = index - Configuration.MAX_RENDERSKY_DATABASE_CACHE_SIZE;
				DataBase.deleteThreadData("RenderSky");
				for (int i=0; i<=i1; i++) {
					DataBase.deleteThreadData("RenderSky"+i);
				}
			} else {
				DataBase.deleteThreadData("RenderSky"+(index+1));				
			}
		}
		if (setStrings) setDefaultStrings();
	}

	/**
	 * Sets the sky render object.
	 * @param r The new render object.
	 * @throws JPARSECException If an error occurs.
	 */
	public void setSkyRenderElement(SkyRenderElement r)
	throws JPARSECException {
		if (r.drawExternalCatalogs != render.drawExternalCatalogs) {
			DataBase.addData("objects", threadID, null, true);
			db_objects = -1;
		}

		if (r.coordinateSystem != render.coordinateSystem ||
				r.drawSkyBelowHorizon != render.drawSkyBelowHorizon ||
				r.drawSkyCorrectingLocalHorizon != render.drawSkyCorrectingLocalHorizon) {
			dateChanged(true);
			this.trajectoryChanged();
		}
		if (r.getColorMode() != render.getColorMode()) {
			this.colorSquemeChanged();
		}
		if (r.planetRender.satellitesAll != render.planetRender.satellitesAll)
			majorObjectsSats = null;

		this.render = r.clone();
		DataBase.addData("eclipticLine", threadID, null, true);
		db_eclipticLine = -1;
		repaintLeyend = true;
		field = (float) render.telescope.getField();
		fieldDeg = (float) (field * Constant.RAD_TO_DEG);
		pixels_per_radian = render.width / this.field;
		pixels_per_degree = pixels_per_radian / (float) Constant.RAD_TO_DEG;
		if (render.drawCoordinateGridFont.getSize() != 15) {
			graphMarginX = 1+(28*render.drawCoordinateGridFont.getSize())/15;
			leyendMargin = 1+(51*render.drawCoordinateGridFont.getSize())/15;
			graphMarginY = 1+(38*render.drawCoordinateGridFont.getSize())/15;
		}
		if (!render.drawExternalGrid && !render.drawCoordinateGrid) graphMarginX = graphMarginY = 0;
		int recy = 0, recx = graphMarginX, recw = render.width-graphMarginX, rech = render.height-leyendMargin-graphMarginY;
		if (render.drawLeyend == LEYEND_POSITION.TOP) recy = leyendMargin;
		if (render.drawLeyend == LEYEND_POSITION.LEFT) recx += leyendMargin;
		if (render.drawLeyend == LEYEND_POSITION.RIGHT || render.drawLeyend == LEYEND_POSITION.LEFT) {
			recw -= leyendMargin;
			rech += leyendMargin;
		}
		rec = new Rectangle(recx, recy, recw, rech);
        if (render.drawLeyend == LEYEND_POSITION.NO_LEYEND) {
        	leyendMargin = 0;
    		rec = new Rectangle(graphMarginX, leyendMargin, render.width-graphMarginX, render.height-leyendMargin-graphMarginY);
        }
        lastLeyend = -1;
		render.planetRender.width = render.width;
		render.planetRender.height = render.height;
		render.planetRender.telescope = render.telescope;
		projection.updateProjection(render, field, this.getXCenter(), this.getYCenter());
		refz = render.anaglyphMode.getReferenceZ();
		foregroundDist = 0;
		conlimDist = 0;
		axesDist = refz;
		milkywayDist = 0;
		nebulaDist = refz/4;
		if (render.anaglyphMode.isReal3D()) {
			conlimDist /= 2.0;
			axesDist /= 2.0;
			nebulaDist /= 2.0;
		}
		//setDefaultStrings();
	}

	private double originalJD = -1, originalJDMinorObj = -1;
	/**
	 * Sets the sky render object.
	 * @param r Sky render object.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param setOriginalField True to set the original field of view to the one defined in the
	 * telescope object inside the sky render object, false to maintain the previous original
	 * field set in the first rendering created.
	 * @throws JPARSECException If an error occurs.
	 */
	public void setSkyRenderElement(SkyRenderElement r, TimeElement time, ObserverElement obs, EphemerisElement eph,
			boolean setOriginalField)
	throws JPARSECException {
		if (setOriginalField) field0 = (float) r.telescope.getField();
		setSkyRenderElement(r, time, obs, eph);
	}

	/**
	 * Sets the sky render object.
	 * @param r Sky render object.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @throws JPARSECException If an error occurs.
	 */
	public void setSkyRenderElement(SkyRenderElement r, TimeElement time, ObserverElement obs, EphemerisElement eph)
	throws JPARSECException {
		if (r.drawExternalCatalogs != render.drawExternalCatalogs) {
			DataBase.addData("objects", threadID, null, true);
			db_objects = -1;
		}
		if (originalJD == -1.0) originalJD = jd;
		if (originalJDMinorObj == -1.0) originalJDMinorObj = jd;
		jd = jparsec.time.TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		if (r.coordinateSystem != render.coordinateSystem ||
				r.drawSkyBelowHorizon != render.drawSkyBelowHorizon ||
				r.drawSkyCorrectingLocalHorizon != render.drawSkyCorrectingLocalHorizon) {
			dateChanged(true);
			this.trajectoryChanged();
		}
		if (r.getColorMode() != render.getColorMode()) {
			this.colorSquemeChanged();
		}
		if (r.planetRender.satellitesAll != render.planetRender.satellitesAll)
			majorObjectsSats = null;

		this.render = r.clone();
		DataBase.addData("eclipticLine", threadID, null, true);
		db_eclipticLine = -1;
		repaintLeyend = true;
		field = 0;
		if (render != null) field = (float) render.telescope.getField();
		fieldDeg = (float) (field * Constant.RAD_TO_DEG);
		pixels_per_radian = render.width / this.field;
		pixels_per_degree = pixels_per_radian / (float) Constant.RAD_TO_DEG;
		try { obs.getGeoLat(); } catch (Exception exc) {
		}
		eph.targetBody = projection.eph.targetBody;
		if (!obs.equals(projection.obs) || !eph.equals(projection.eph)) {
			dateChanged(true);
			//forceRecalculate = true;
		} else {
			if (jd != originalJD) {
				double elapsedTime = Math.abs(jd - originalJD);
				if (elapsedTime > render.updateTime / Constant.SECONDS_PER_DAY) {
					if (render.coordinateSystem == COORDINATE_SYSTEM.HORIZONTAL ||
							elapsedTime > render.updateTimeFullUpdate / Constant.SECONDS_PER_DAY) {
						dateChanged(true);
						//forceRecalculate = true;
						originalJD = jd;
					} else {
						double elapsedTimeMinorObj = Math.abs(jd - originalJDMinorObj);
						if (elapsedTimeMinorObj > render.updateTime / Constant.SECONDS_PER_DAY) {
							if (//projection.obs.getMotherBody() != TARGET.NOT_A_PLANET || elapsedTime > 365 ||
									render.coordinateSystem != COORDINATE_SYSTEM.EQUATORIAL) { // fieldDeg < 20) {
								if (re_star != null) this.re_star.setReadElements(null);
								DataBase.addData("objects", threadID, null, true);
								DataBase.addData("milkyWay", threadID, null, true);
								DataBase.addData("nebula", threadID, null, true);
								db_objects = -1;
								db_milkyWay = -1;
								db_nebula = -1;
								DataBase.addData("sncat", threadID, null, true);
					    		DataBase.addData("novae", threadID, null, true);
								DataBase.addData("meteor", threadID, null, true);
								db_meteor = -1;
								db_sncat = -1;
								db_novae = -1;
							}

							DataBase.addData("sunSpot", threadID, null, true);
							DataBase.addData("satEphem", threadID, null, true);
							DataBase.addData("probeEphem", threadID, null, true);
							DataBase.addData("asterEphem", threadID, null, true);
							DataBase.addData("cometEphem", threadID, null, true);
							DataBase.addData("neoEphem", threadID, null, true);
							DataBase.addData("transEphem", threadID, null, true);

							RenderPlanet.lastRenderElement = null;
							DataBase.addData("horizonLine", threadID, null, true);
							DataBase.addData("raline", threadID, null, true);
							DataBase.addData("decline", threadID, null, true);
							db_sunSpot = -1;
							db_satEphem = -1;
							db_probeEphem = -1;
							db_asterEphem = -1;
							db_cometEphem = -1;
							db_neoEphem = -1;
							db_transEphem = -1;
							db_horizonLine = -1;
							db_raline = -1;
							db_decline = -1;
							brightestComet = -100;
							firstTime = true;
							jd_ut = -1;
							neverWait = false;
							originalJDMinorObj = jd;
						}

						render.planetRender.moonephem = null;
						this.planets = null;
						majorObjects = null;
						majorObjectsSats = null;
						ephem = null;
					}
				} else {
					render.planetRender.moonephem = null;
					this.planets = null;
					majorObjects = null;
					majorObjectsSats = null;
					ephem = null;
				}
			}
		}
		try { obsLat = obs.getLatitudeRad(); } catch (Exception exc) {}
		try { lst = SiderealTime.apparentSiderealTime(time, obs, eph); } catch (Exception exc) {}
		this.setJupiterGRSLongitude(jparsec.ephem.event.MainEvents.getJupiterGRSLongitude(jd), 2);
		if (render.drawCoordinateGridFont.getSize() != 15) {
			graphMarginX = 1+(28*render.drawCoordinateGridFont.getSize())/15;
			leyendMargin = 1+(51*render.drawCoordinateGridFont.getSize())/15;
			graphMarginY = 1+(38*render.drawCoordinateGridFont.getSize())/15;
		}
		if (!render.drawExternalGrid && !render.drawCoordinateGrid) graphMarginX = graphMarginY = 0;
		int recy = 0, recx = graphMarginX, recw = render.width-graphMarginX, rech = render.height-leyendMargin-graphMarginY;
		if (render.drawLeyend == LEYEND_POSITION.TOP) recy = leyendMargin;
		if (render.drawLeyend == LEYEND_POSITION.LEFT) recx += leyendMargin;
		if (render.drawLeyend == LEYEND_POSITION.RIGHT || render.drawLeyend == LEYEND_POSITION.LEFT) {
			recw -= leyendMargin;
			rech += leyendMargin;
		}
		rec = new Rectangle(recx, recy, recw, rech);
        if (render.drawLeyend == LEYEND_POSITION.NO_LEYEND) {
        	leyendMargin = 0;
    		rec = new Rectangle(graphMarginX, leyendMargin, render.width-graphMarginX, render.height-leyendMargin-graphMarginY);
        }
        projection = new Projection(time, obs, eph, render, field, this.getXCenter(), this.getYCenter());
        lastLeyend = -1;
		render.planetRender.width = render.width;
		render.planetRender.height = render.height;
		render.planetRender.telescope = render.telescope;
		render.planetRender.ephemSun = null;
		baryc = Ephem.eclipticToEquatorial(PlanetEphem.getGeocentricPosition(equinox, TARGET.Solar_System_Barycenter, 0.0, false, projection.obs), Constant.J2000, projection.eph);
		equinox = projection.eph.equinox;
		if (equinox == EphemerisElement.EQUINOX_OF_DATE)
			equinox = jd;

		//setDefaultStrings();
	}

	/**
	 * Changes the field of view. This option can be used to show a different FOV
	 * from the one automatically calculated based on the current selected telescope.
	 * @param fov Field of view in radians. Set to 0 to allow calculating
	 * it again using the current telescope.
	 * @throws JPARSECException If an error occurs.
	 */
	public void setFieldOfView(double fov) throws JPARSECException {
		forcedFOV = true;
		if (fov <= 0) forcedFOV = false;
		field = (float) fov;
		fieldDeg = (float) (field * Constant.RAD_TO_DEG);
		pixels_per_radian = render.width / this.field;
		pixels_per_degree = pixels_per_radian / (float) Constant.RAD_TO_DEG;
        projection = new Projection(projection.time, projection.obs, projection.eph, render, field, this.getXCenter(), this.getYCenter());
        render.planetRender.telescope = TelescopeElement.SCHMIDT_CASSEGRAIN_20cm;
        render.planetRender.telescope.invertHorizontal = render.telescope.invertHorizontal;
        render.planetRender.telescope.invertVertical = render.telescope.invertVertical;
        render.planetRender.telescope.ocular.focalLength = TelescopeElement.getOcularFocalLengthForCertainField(field, render.planetRender.telescope);
	}

	/**
	 * Returns the field of view.
	 * @return Field of view in radians.
	 */
	public double getFieldOfView() { return field; }

	/**
	 * Returns the field of view the first time it was rendered,
	 * corresponding to the telescope instance at startup.
	 * @return Field of view in radians.
	 */
	public double getOriginalFieldOfView() { return field0; }

	private void drawTrajectory(boolean onlyClose) throws JPARSECException
	{
		if (render.drawClever && this.fieldDeg >= 100 && field > 5 * this.trajectoryField) return;

		int col;

		double cte = pixels_per_degree_50*0.5;
		float dist = getDist(0);
		double steps = 1.0;
		if (render.drawClever && field > 5 * this.trajectoryField) steps = 2;
		if (render.drawClever && field > 10 * this.trajectoryField) steps = 3;
		double cte2 = cte * cte;

		boolean spa = Translate.getDefaultLanguage() == LANGUAGE.SPANISH;
		for (int index = 0; index < render.trajectory.length; index++)
		{
			if (!onlyClose && 
					(render.trajectory[index].objectType == OBJECT.NEO || 
					render.trajectory[index].objectType == OBJECT.ARTIFICIAL_SATELLITE)) continue;
			if (onlyClose && 
					render.trajectory[index].objectType != OBJECT.NEO && 
					render.trajectory[index].objectType != OBJECT.ARTIFICIAL_SATELLITE) continue;
			if (render.trajectory[index].loc_path == null) continue;
			float pos[] = Projection.INVALID_POSITION;
			float old_pos[] = Projection.INVALID_POSITION;
			col = render.trajectory[index].drawPathColor1;
			int point_size = 1;
			if (render.trajectory[index].loc_path != null) {
				LocationElement[] loc_path = render.trajectory[index].loc_path.clone();
/*
				// THIS ALGORITHM USING GENERALPATH IS NOT WORKING SINCE THE PATH IN THIS OBJECT
				// IS NOT GUARRANTEED TO PASS EXACTLY ACROSS ALL POINTS UNLESS ONLY LINETO IS USED
				Object path = g.generalPathInitialize();
				float[] prev_pos1 = null, prev_pos2 = null;
				for (int i = 0; i < loc_path.length; i++)
				{
					col = render.trajectory[index].drawPathColor1;
					pos = projection.projectPosition(loc_path[i], 0, false);
					if (!projection.isInvalid(pos)) {
						pos[0] = (int) pos[0];
						pos[1] = (int) pos[1];
						if (render.projection != Projection.PROJECTION.CYLINDRICAL || old_pos == null ||
								(FastMath.pow(old_pos[0]-pos[0], 2.0)+FastMath.pow(old_pos[1]-pos[1], 2.0)) < cte2) {
							if (old_pos == null) {
								g.generalPathMoveTo(path, pos[0], pos[1]);
								old_pos = pos;
							} else {
								if (i == loc_path.length-1 && prev_pos2 == null) {
									if (prev_pos1 != null) {
										prev_pos2 = pos;
										g.generalPathQuadTo(path, prev_pos1[0], prev_pos1[1], prev_pos2[0], prev_pos2[1]);
									} else {
										prev_pos1 = pos;
										g.generalPathLineTo(path, prev_pos1[0], prev_pos1[1]);
									}
								} else {
									if (prev_pos1 == null) {
										prev_pos1 = pos;
									} else {
										if (prev_pos2 == null) {
											prev_pos2 = pos;
										} else {
											g.generalPathQuadTo(path, prev_pos1[0], prev_pos1[1], prev_pos2[0], prev_pos2[1]);
											g.generalPathLineTo(path, pos[0], pos[1]);
											//g.generalPathCurveTo(path, prev_pos1[0], prev_pos1[1], prev_pos2[0], prev_pos2[1], pos[0], pos[1]);
											prev_pos1 = null;
											prev_pos2 = null;
											old_pos = pos;
										}
									}
								}
							}

						} else {
							if (prev_pos2 != null) {
								g.generalPathQuadTo(path, prev_pos1[0], prev_pos1[1], prev_pos2[0], prev_pos2[1]);
							} else {
								if (prev_pos1 != null) {
									g.generalPathLineTo(path, prev_pos1[0], prev_pos1[1]);
								}
							}
							prev_pos1 = null;
							prev_pos2 = null;
							g.generalPathMoveTo(path, pos[0], pos[1]);
							old_pos = pos;
						}

						double step = (double) i % (steps*render.trajectory[index].labelsSteps);
						if (render.trajectory[index].drawLabels && step == 0.0)
						{
								double jd = render.trajectory[index].startTimeJD + render.trajectory[index].stepTimeJD * i;
								AstroDate astro = new AstroDate(jd);
								String label = astro.getMonth() + " " + astro.getDay();
								if (render.trajectory[index].drawLabelsFormat == TrajectoryElement.LABELS.MONTH_DAY_YEAR)
									label = label + " " + astro.getYear();
								if (render.trajectory[index].drawLabelsFormat == TrajectoryElement.LABELS.YEAR_MONTH_DAY)
									label = astro.getYear() + " " + label;
								if (render.trajectory[index].drawLabelsFormat == TrajectoryElement.LABELS.DAY_MONTH_ABBREVIATION)
									label = astro.getDay() + " " + Translate.translate(monthNames[astro.getMonth()-1]);
								int position = 1; // down
								if (i>0) {
									if (loc_path[i].getLatitude() > loc_path[i-1].getLatitude())
										position = -1; // up
								} else {
									if (loc_path[i].getLatitude() < loc_path[i+1].getLatitude())
										position = -1; // up
								}
								drawString(col, render.trajectory[index].drawPathFont, label, pos[0], pos[1] + 8 * position, -8, false);
								col = render.trajectory[index].drawPathColor2;
								point_size = 3;
						}
						int point2 = 2*point_size+1;
						g.setColor(col, true);
						if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
							g.fillOval((int)pos[0] - point_size, (int)pos[1] - point_size, point2, point2);
						} else {
							g.fillOval((int)pos[0] - point_size, (int)pos[1] - point_size, point2, point2, dist);
						}
						point_size = 1;

						if (render.trajectory[index].apparentObjectName != null) {
							if (i==loc_path.length/2 && !render.trajectory[index].apparentObjectName.isEmpty()) {
								Graphics.FONT mf = FONT.getDerivedFont(render.trajectory[index].drawPathFont, render.trajectory[index].drawPathFont.getSize()*2);
								drawString(col, mf, render.trajectory[index].apparentObjectName, pos[0], pos[1], -60, false);
							}
						}
					} else {
						if (prev_pos2 != null) {
							g.generalPathQuadTo(path, prev_pos1[0], prev_pos1[1], prev_pos2[0], prev_pos2[1]);
						} else {
							if (prev_pos1 != null) {
								g.generalPathLineTo(path, prev_pos1[0], prev_pos1[1]);
							}
						}
						prev_pos1 = null;
						prev_pos2 = null;
						old_pos = null;
					}
				}

				col = render.trajectory[index].drawPathColor1;
				g.setColor(col, true);
				if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
					g.draw(path);
				} else {
					g.draw(path, dist);
				}
*/
				double jd_TDB = -1;
				int rz = (int) (pixels_per_degree*10);
				for (int i = 0; i < loc_path.length; i++)
				{
					col = render.trajectory[index].drawPathColor1;
					pos = projection.projectPosition(loc_path[i], 0, false);
					if (pos != null && !this.isInTheScreen((int)pos[0], (int)pos[1], rz)) {
						if (old_pos == null || !this.isInTheScreen((int)old_pos[0], (int)old_pos[1], rz)) {
							old_pos = pos;
							continue;
						}
					}

					if (pos != null) {
						pos[0] = (int) pos[0];
						pos[1] = (int) pos[1];
					}
					if (!projection.isInvalid(pos) && !projection.isInvalid(old_pos))
					{
						if (render.projection != Projection.PROJECTION.CYLINDRICAL ||
								(FastMath.pow(old_pos[0]-pos[0], 2.0)+FastMath.pow(old_pos[1]-pos[1], 2.0)) < cte2) {
							g.setColor(col, true);
							g.setStroke(render.trajectory[index].stroke);
							if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
								g.drawLine(old_pos[0], old_pos[1], pos[0], pos[1], true);
							} else {
								g.drawLine(old_pos[0], old_pos[1], pos[0], pos[1], dist, dist);
							}
						}
					}

					if (!projection.isInvalid(pos))
					{
						int stepi = (int)steps*render.trajectory[index].labelsSteps;
						double step = (double) (i % stepi);
						jd_TDB = render.trajectory[index].startTimeJD + render.trajectory[index].stepTimeJD * i;
						if (render.trajectory[index].drawLabels && step == 0.0)
						{
							double jd = TimeScale.getJD(new TimeElement(jd_TDB, SCALE.BARYCENTRIC_DYNAMICAL_TIME), projection.obs, projection.eph, render.trajectory[index].timeScaleForLabels); //SCALE.LOCAL_TIME);

							AstroDate astro = new AstroDate(jd);
							String label = astro.getMonth() + " " + astro.getDay();
							if (render.trajectory[index].drawLabelsFormat == TrajectoryElement.LABELS.MONTH_DAY_YEAR)
								label = label + " " + astro.getYear();
							if (render.trajectory[index].drawLabelsFormat == TrajectoryElement.LABELS.YEAR_MONTH_DAY)
								label = astro.getYear() + " " + label;
							if (render.trajectory[index].drawLabelsFormat == TrajectoryElement.LABELS.DAY_MONTH_ABBREVIATION) {
								if (spa) {
									label = astro.getDay() + " " + mont[-1 + astro.getMonth()];
								} else {
									label = mont[-1 + astro.getMonth()] + " " + astro.getDay();									
								}
								if (Math.abs(this.jd-jd) > 365) {
									label += " " + astro.getYear();
								}
							}
							double stepTime = render.trajectory[index].stepTimeJD * render.trajectory[index].labelsSteps;
							if (render.trajectory[index].showTime && (stepTime < 1.0 || (//stepTime < 15 &&
									(jd-0.5) != (int)(jd-0.5)))) {
								AstroDate astro2 = new AstroDate(jd);
								if (stepTime * 1440.0 < 2.0) {
									if (stepTime * 1440.0 <= 0.1) {
										String sec = Functions.formatValue(astro.getSeconds(), 1);
										if (sec.indexOf(".") == 1) sec = "0" + sec;
										label = Functions.fmt(astro2.getHour(), 2, ':')+Functions.fmt(astro2.getMinute(), 2, ' ').trim()+":"+sec;
									} else {
										label = Functions.fmt(astro2.getHour(), 2, ':')+Functions.fmt(astro2.getMinute(), 2, ' ').trim();
										String sec = Functions.fmt(astro.getRoundedSecond(), 2, ' ').trim();
										if (!sec.equals("00")) label += ":" + sec;
									}
								} else {
									astro2.add(0.5 / 1440.0);
									String label2 = Functions.fmt(astro2.getHour(), 2, ':')+Functions.fmt(astro2.getMinute(), 2, ' ');
									if (label2.trim().endsWith("00:00")) label2 = DataSet.replaceAll(label2, "00:00", "0h", true);
									label += " "+label2;
								}
								label = label.trim();
								if (render.trajectory[index].showTimeScale && render.trajectory[index].timeScaleForLabels != SCALE.LOCAL_TIME)
									label += " "+TimeElement.getTimeScaleAbbreviation(render.trajectory[index].timeScaleForLabels);
							}

							if (render.trajectory[index].addLabel != null && render.trajectory[index].addLabel.length > i) 
								label += render.trajectory[index].addLabel[i];

							/*
							int position = 1; // down
							if (i>0 && loc_path[i-1] != null) {
								if (loc_path[i].getLatitude() > loc_path[i-1].getLatitude())
									position = -1; // up
							} else {
								if (i < loc_path.length-1 && loc_path[i+1] != null && loc_path[i].getLatitude() < loc_path[i+1].getLatitude())
									position = -1; // up
							}
							*/
							point_size = 3;
							if (g.renderingToAndroid()) point_size ++;
							
							boolean showLabel = true;
							if (i >= stepi && loc_path[i-stepi] != null) {
								double ad = LocationElement.getApproximateAngularDistance(loc_path[i], loc_path[i-stepi]);
								if (ad < field/15 && !g.renderingToExternalGraphics()) showLabel = false;
							}
							
							if (showLabel) drawString(col, render.trajectory[index].drawPathFont, label, pos[0], pos[1], -render.trajectory[index].drawPathFont.getSize()-point_size, true);
							col = render.trajectory[index].drawPathColor2;
							
							int point2 = 2*point_size;
							g.setColor(col, true);
							if (render.trajectory[index].objectType == OBJECT.ARTIFICIAL_SATELLITE && loc_path[i].getRadius() == EphemElement.INVALID_MAGNITUDE)
								g.setColor(col, 164);
							if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
								g.fillOval((int)pos[0] - point_size, (int)pos[1] - point_size, point2, point2, this.fast);
							} else {
								g.fillOval((int)pos[0] - point_size, (int)pos[1] - point_size, point2, point2, dist);
							}
						} else {
							if (g.renderingToAndroid()) point_size ++;
							int point2 = 2*point_size;
							g.setColor(col, true);
							if (render.trajectory[index].objectType == OBJECT.ARTIFICIAL_SATELLITE && loc_path[i].getRadius() == EphemElement.INVALID_MAGNITUDE)
								g.setColor(col, 164);
							if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
								g.fillOval(pos[0] - point_size, pos[1] - point_size, point2+1, point2+1, this.fast);
								//g.drawLine(pos[0] - point2*2, pos[1], pos[0] + point2*2, pos[1], this.fast);
								//g.drawLine(pos[0], pos[1] - point2*2, pos[0], pos[1] + point2*2, this.fast);
							} else {
								g.fillOval((int)pos[0] - point_size, (int)pos[1] - point_size, point2+1, point2+1, dist);
								//g.drawLine(pos[0] - point2*2, pos[1], (int)pos[0] + point2*2, (int)pos[1], dist, dist);
								//g.drawLine(pos[0], pos[1] - point2*2, pos[0], (int)pos[1] + point2*2, dist, dist);
							}
						}

						if (render.trajectory[index].objectType == OBJECT.COMET && render.trajectory[index].objectType.showCometTail && i < loc_path.length-1 && step == 0.0) {
							double sun[] = OrbitEphem.sun(jd_TDB);
							LocationElement loc_sun = CoordinateSystem.eclipticToEquatorial(LocationElement.parseRectangularCoordinates(sun), new TimeElement(jd_TDB, SCALE.BARYCENTRIC_DYNAMICAL_TIME), projection.obs, projection.eph);
							LocationElement loc = projection.toEquatorialPosition(loc_path[i], true);
							EphemElement ephem = new EphemElement();
							ephem.rightAscension = loc.getLongitude();
							ephem.declination = loc.getLatitude();
							ephem.distance = loc.getRadius();
							ephem.elongation = (float) LocationElement.getApproximateAngularDistance(loc_sun, loc);
							ephem.setLocation(loc_path[i]);
							if (loc_path[i+1] != null) {
								LocationElement locPlus = projection.toEquatorialPosition(loc_path[i+1], true);
								ephem.brightLimbAngle = (float) (LocationElement.getPositionAngle(ephem.getEquatorialLocation(), locPlus));

								this.drawCometTail(g, pos, loc_sun, ephem);
							}
						}

						if (render.trajectory[index].apparentObjectName != null) {
							if (i==loc_path.length/2 && !render.trajectory[index].apparentObjectName.isEmpty()) {
								Graphics.FONT mf = FONT.getDerivedFont(render.trajectory[index].drawPathFont, render.trajectory[index].drawPathFont.getSize()*2);
								drawString(render.trajectory[index].drawPathColor1, mf, render.trajectory[index].apparentObjectName, pos[0], pos[1], -mf.getSize()-point_size, false);
							}
						}

						point_size = 1;
					}

					old_pos = null;
					if (pos != null) old_pos = pos.clone();
				}
				
				// Mark current position for NEOs
				if (render.trajectory[index].objectType == OBJECT.NEO && render.trajectory[index].loc_path.length > 2) {
					if (render.trajectory[index].startTimeJD < projection.jd && render.trajectory[index].endTimeJD > projection.jd) {
						double x[] = new double [3], y[] = new double[3];
						int imin = -1;
						double dmin = -1;
						for (int i=0; i<render.trajectory[index].loc_path.length; i++) {
							jd_TDB = render.trajectory[index].startTimeJD + render.trajectory[index].stepTimeJD * i;
							double d = Math.abs(projection.jd - jd_TDB);
							if (d < dmin || dmin == -1) {
								dmin = d;
								imin = i;
							}
						}
						double lat = -1, lon = -1;
						for (int comp = 0; comp < 2; comp ++) {
							x[0] = render.trajectory[index].startTimeJD + render.trajectory[index].stepTimeJD * imin;
							y[0] = render.trajectory[index].loc_path[imin].getLatitude();
							if (comp == 0) y[0] = render.trajectory[index].loc_path[imin].getLongitude();
							if (imin > 0) {
								x[1] = render.trajectory[index].startTimeJD + render.trajectory[index].stepTimeJD * (imin-1);
								y[1] = render.trajectory[index].loc_path[imin-1].getLatitude();
								if (comp == 0) y[1] = render.trajectory[index].loc_path[imin-1].getLongitude();
								if (imin < render.trajectory[index].loc_path.length-1) {
									x[2] = render.trajectory[index].startTimeJD + render.trajectory[index].stepTimeJD * (imin+1);
									y[2] = render.trajectory[index].loc_path[imin+1].getLatitude();
									if (comp == 0) y[2] = render.trajectory[index].loc_path[imin+1].getLongitude();
								} else {
									x[2] = render.trajectory[index].startTimeJD + render.trajectory[index].stepTimeJD * (imin-2);
									y[2] = render.trajectory[index].loc_path[imin-2].getLatitude();								
									if (comp == 0) y[2] = render.trajectory[index].loc_path[imin-2].getLongitude();								
								}
							} else {
								x[1] = render.trajectory[index].startTimeJD + render.trajectory[index].stepTimeJD * (imin+1);
								y[1] = render.trajectory[index].loc_path[imin+1].getLatitude();
								if (comp == 0) y[1] = render.trajectory[index].loc_path[imin+1].getLongitude();
								x[2] = render.trajectory[index].startTimeJD + render.trajectory[index].stepTimeJD * (imin+2);
								y[2] = render.trajectory[index].loc_path[imin+2].getLatitude();
								if (comp == 0) y[2] = render.trajectory[index].loc_path[imin+2].getLongitude();								
							}
							Interpolation interp = new Interpolation(x, y, false);
							double out = interp.MeeusInterpolation(projection.jd);
							if (comp == 0) lon = out;
							if (comp == 1) lat = out;
						}
						LocationElement currentPos = new LocationElement(lon, lat, 1);
						pos = projection.projectPosition(currentPos, 0, false);
						if (pos != null && this.isInTheScreen((int)pos[0], (int)pos[1])) {
							pos[0] = (int) pos[0];
							pos[1] = (int) pos[1];
							g.setColor(render.trajectory[index].drawPathColor1, true);
							g.setStroke(render.trajectory[index].stroke);
							int r = 5;
							if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
								g.drawLine(pos[0]-r, pos[1]-r, pos[0]+r, pos[1]+r, true);
								g.drawLine(pos[0]+r, pos[1]-r, pos[0]-r, pos[1]+r, true);
								g.drawOval(pos[0]-r, pos[1]-r, 2*r+1, 2*r+1, true);
							} else {
								g.drawLine(pos[0]-r, pos[1]-r, pos[0]+r, pos[1]+r, dist, dist);
								g.drawLine(pos[0]+r, pos[1]-r, pos[0]-r, pos[1]+r, dist, dist);
								g.drawOval(pos[0]-r, pos[1]-r, 2*r+1, 2*r+1, dist);
							}
						}
					}
				}
			}
		}
	}

	// Draw Sun spots
	private double jd_ut = -1;
	private void drawSunSpots(float position[]) throws JPARSECException
	{
		if (!render.drawSunSpots)
			return;
		double Sun_radius = pixels_per_radian * render.planetRender.ephemSun.angularRadius;
		if (Sun_radius < 10) return;

/*		g.setColor(255, 255, 0, 164); // Yellow
		int position22 = (int)(2 * position[2])+1;
		if (render.planetRender.textures)
			g.setColor(164, 164, 0, 255); // Yellow opa
		if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
			g.fillOval((int)(position[0] - position[2]), (int)(position[1] - position[2]), position22, position22, false);
		} else {
			g.fillOval((int)(position[0] - position[2]), (int)(position[1] - position[2]), position22, position22, dist);
		}
*/

		float dist = getDist(refz-render.planetRender.ephemSun.distance);
		if (jd_ut == -1.0) jd_ut = TimeScale.getJD(projection.time, projection.obs, projection.eph, SCALE.UNIVERSAL_TIME_UTC);

		ArrayList<Object> sunSpot = null;
		Object o = null;
		if (db_sunSpot >= 0) {
			o = DataBase.getData(db_sunSpot);
		} else {
			o = DataBase.getData("sunSpot", threadID, true);
		}
		if (o != null) sunSpot = new ArrayList<Object>(Arrays.asList((Object[]) o));
		if (sunSpot == null) {
			try
			{
				ArrayList<String[]> sunSpots = ReadFile.readFileOfSunSpots(jd_ut);
				sunSpot = new ArrayList<Object>();
				for (int i=0; i<sunSpots.size(); i++) {
					sunSpot.add(sunSpots.get(i));
				}
				DataBase.addData("sunSpot", threadID, sunSpot.toArray(), true);
				db_sunSpot = DataBase.getIndex("sunSpot", threadID);
			} catch (Exception e)
			{
				// File not available
				AstroDate astro = new AstroDate();
				if (jd_ut - astro.jd() > 3) {
					DataBase.addData("sunSpot", threadID, new Object[] {}, true);
					db_sunSpot = DataBase.getIndex("sunSpot", threadID);
				}
			}
		}
		if (sunSpot != null) {
			g.setColor(render.drawSunSpotsColor, true);
			int c = render.drawSunSpotsColor;
			if (c == render.background) {
				g.setColor(g.invertColor(render.background), g.getAlpha(render.background));
				c = g.getColor();
				g.setColor(render.drawSunSpotsColor, true);
			}

			int nlines = sunSpot.size();
			for (int i = 0; i < nlines; i++)
			{
				String record[] = (String[]) sunSpot.get(i);

				double lon = Double.parseDouble(record[2]);
				double lat = Double.parseDouble(record[3]);

				float pos[] = getScreenCoordinatesOfPlanetographicPosition(new LocationElement(lon, lat, 1.0),
						TARGET.SUN, 0, true);
				if (pos == null) continue;

				// Undo inversion since this will be done automatically in Android/AWT Graphics
				if (render.telescope.invertHorizontal) pos[0] = render.width-1-pos[0];
				if (render.telescope.invertVertical) pos[1] = render.height-1-pos[1];

				if (!projection.isInvalid(pos) && this.isInTheScreen((int)pos[0], (int)pos[1]))
				{
					double area = Double.parseDouble(record[4]);
					int radius = (int) (Math.sqrt(area) * Sun_radius + 0.5);
					radius = Math.max(radius, 0);
					int width = 2 * radius+1;
					if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
						g.fillOval(pos[0] - radius, pos[1] - radius, width, width, render.drawFastLinesMode.fastOvals() && this.fast);
					} else {
						g.fillOval(pos[0] - radius, pos[1] - radius, width, width, dist);
					}
					if (render.drawSunSpotsLabels && Sun_radius > 30)
					{
						String group = record[0];
						String type = record[1];
						String label = group;
						if (!type.equals("")) label += "-" + type;
						drawString(c, render.drawStarsNamesFont, label, pos[0], pos[1], Math.max(10+radius, render.drawStarsNamesFont.getSize()), false);
					}
				}
			}
		}
	}

	/**
	 * Returns the sky coordinates of a given pixel position.
	 * @param px X position.
	 * @param py Y position.
	 * @return The sky location, or null if it cannot be obtained.
	 * @throws JPARSECException If an error occurs.
	 */
	public LocationElement getSkyLocation(float px, float py) throws JPARSECException {
		LocationElement out = null;
		if (projection.isCylindricalForced()) {
			out = projection.invertCylindricEquidistant(px, py);
		} else {
			switch (render.projection) {
			case STEREOGRAPHICAL:
				out = projection.invertStereographic(px, py);
				break;
			case CYLINDRICAL:
				out = projection.invertCylindric(px, py);
				break;
			case  CYLINDRICAL_EQUIDISTANT:
				out = projection.invertCylindricEquidistant(px, py);
				break;
			case SPHERICAL:
				out = projection.invertSpheric(px, py);
				break;
			case POLAR:
				out = projection.invertPolar(px, py);
				break;
			}
		}
/*		if (render.drawSkyCorrectingLocalHorizon) {
			if (projection.obs.getMotherBody() == null || projection.obs.getMotherBody() == TARGET.EARTH) {
			boolean fast = false;
			if (fieldDeg > 10) fast = true;
			out = projection.toEquatorialPosition(out, fast);
			out = CoordinateSystem.equatorialToHorizontal(out, projection.ast, projection.obs, projection.eph, false, fast);
			out.setLatitude(Ephem.getGeometricElevation(projection.eph, projection.obs, out.getLatitude()));
			out = CoordinateSystem.horizontalToEquatorial(out, projection.ast, projection.obs.getLatitudeRad(), fast);
			boolean lh = render.drawSkyCorrectingLocalHorizon;
			projection.disableCorrectionOfLocalHorizon();
			out = projection.getApparentLocationInSelectedCoordinateSystem(out, false, fast);
			projection.enableCorrectionOfLocalHorizon();
			}
		}
*/
		return out;
	}

	/**
	 * Returns the pixel position of a given location.
	 * @param loc Sky location.
	 * @param isEquatorial True if input location is equatorial, false if it is given
	 * in the current selected coordinate system for rendering.
	 * @param checkScreen True to check if the position is inside the visible screen or not.
	 * In case of true and the position is outside, null is returned.
	 * @param correctForRefraction True to correct for refraction the input position. This only
	 * works in case of providing a location in the equatorial system, otherwise is ignored.
	 * @return The pixel position, or null if it cannot be obtained.
	 * @throws JPARSECException If an error occurs.
	 */
	public float[] getSkyPosition(LocationElement loc, boolean isEquatorial, boolean checkScreen, boolean correctForRefraction) throws JPARSECException {
		float pos[] = null;
		if (isEquatorial) {
			if (!correctForRefraction) projection.disableCorrectionOfLocalHorizon();
			pos = projection.projectPosition(projection.getApparentLocationInSelectedCoordinateSystem(loc, false, false, 0), 0, false);
			projection.enableCorrectionOfLocalHorizon();
		} else {
			pos = projection.projectPosition(loc, 0, false, 0);
		}
		if (checkScreen && pos != null && !this.isInTheScreen((int)pos[0], (int)pos[1])) pos = null;
		return pos;
	}

	/**
	 * Obtain planetographic, heliographic, selenographic, or satellitographic
	 * position for certain screen point. The default result will be always the
	 * position of the nearest body to the observer, if, for instance, one object
	 * is occulting/transiting any other. To avoid considering satellites in
	 * this proccess set the corresponding boolean to false.
	 *
	 * @param x Horizontal position in the rendering in pixels.
	 * @param y Horizontal position in the rendering in pixels.
	 * @param coordinate_system Reference coordinate system, only for giant
	 *        planets: 1 (system I), 2 (system II), 3 (system III).
	 * @param consider_satellites True to consider possible transiting
	 *        satellites, false to ignore them.
	 * @return Corresponding position in the object, or null object if the
	 * position is outside any body rendered. The radius field of the location
	 * object will contain the index of the {@linkplain TARGET} body where
	 * this location was found.
	 * @throws JPARSECException If an error occurs.
	 */
	public LocationElement getPlanetographicPosition(double x, double y, int coordinate_system,
			boolean consider_satellites) throws JPARSECException
	{
		if (render.telescope.invertHorizontal) x = render.width-1-x;
		if (render.telescope.invertVertical) y = render.height-1-y;

		LocationElement loc_out = null;
		double distMin = -1.0;
		if (planets == null || (rec != null && pixels_per_degree < 10.0)) return loc_out;

		for (int i = 1; i < planets.size(); i = i + 2)
		{
			float position[] = (float[]) planets.get(i - 1);

			// Prepare some variables
			double r = Math.abs(position[2]);
			double dx = x - position[0];
			double dy = position[1] - y;
			double rr = FastMath.hypot(dx, dy);

			// Obtain planetographic position
			if (rr <= r && (position[3] < distMin || distMin == -1.0))
			{
				PlanetRenderElement planet_render = (PlanetRenderElement) planets.get(i);
				EphemElement ephem = planet_render.ephem;

				if (position[2] < 0.0 && consider_satellites)
				{
					ephem = EphemElement.parseMoonEphemElement(planet_render.moonephem[(int) position[4]],	jd);
					planet_render.target = Target.getID(planet_render.moonephem[(int) position[4]].name);
				}

				if (planet_render.target == projection.obs.getMotherBody()) continue;

				if (projection.obs.getMotherBody() == TARGET.EARTH && render.drawSkyCorrectingLocalHorizon && projection.eph.isTopocentric) {
					double geoElev0 = Ephem.getGeometricElevation(projection.eph, projection.obs, ephem.elevation);
					double geoElevp = geoElev0 + ephem.angularRadius;
					double appElevp = Ephem.getApparentElevation(projection.eph, projection.obs, geoElevp, 10);
					double dp = (appElevp - ephem.elevation);
					float upperLimbFactor = (float) (dp / ephem.angularRadius);
					int de = (int) (r * (1.0 - upperLimbFactor) + 0.5);
					if (de > 1) {
						double angle = Constant.PI_OVER_TWO+projection.getCenitAngleAt(this.getSkyLocation((float)x, (float)y), false);
						double p[] = Functions.rotateZ(new double[] {dx, dy, 1}, -angle);
						p[1] /= upperLimbFactor;
						p = Functions.rotateZ(p, angle);
						dx = p[0];
						dy = p[1];
						rr = FastMath.hypot(dx, dy);
						if (rr > r) continue;
					}
				}

				if (position[2] > 0.0 || consider_satellites)
				{
					// Recover information in a more confortable way
					double incl_north = -Constant.PI_OVER_TWO+projection.getNorthAngleAt(ephem.getEquatorialLocation(), true, false); //this.getSkyLocation((float)x, (float)y), false); //ephem.paralacticAngle;
					//System.out.println(planet_render.telescope.getField()*Constant.RAD_TO_DEG+"/"+ephem.paralacticAngle*Constant.RAD_TO_DEG);
					if (planet_render.northUp) incl_north = 0.0;

					double incl_axis = ephem.positionAngleOfAxis;
					double incl_rotation = ephem.longitudeOfCentralMeridian;
					if (position[2] > 0.0 && (planet_render.target == TARGET.JUPITER || planet_render.target == TARGET.SATURN || planet_render.target == TARGET.URANUS || planet_render.target == TARGET.NEPTUNE))
					{
						switch (coordinate_system)
						{
						case 1:
							incl_rotation = ephem.longitudeOfCentralMeridianSystemI;
							break;
						case 2:
							incl_rotation = ephem.longitudeOfCentralMeridianSystemII;
							break;
						case 3:
							incl_rotation = ephem.longitudeOfCentralMeridianSystemIII;
							break;
						}
					}

					// Set initial shape and size parameters
					double oblateness = 1.0 + Math.abs(FastMath.pow(FastMath.cos(ephem.positionAngleOfPole), 2)) * (planet_render.target.polarRadius / planet_render.target.equatorialRadius - 1.0);
					double incl_up = incl_axis - incl_north;

					// Obtain planetographic position
					double tmp = rr / r;
					double dz = Math.sqrt(r * r - rr);
					double ang = FastMath.atan2_accurate(dy, dx);
					dx = -rr * FastMath.cos(ang - incl_up) / r;
					dy = -rr * FastMath.sin(ang - incl_up) / (r * oblateness);
					if (oblateness != 1.0) tmp = (dx * dx + dy * dy);
					if (tmp <= 1.0)
					{
						double incl_pole = ephem.positionAngleOfPole;
						incl_pole = RenderPlanet.geodeticToGeocentric(planet_render.target.equatorialRadius, planet_render.target.polarRadius, incl_pole);

						dz = Math.sqrt(1.0 - FastMath.pow(dx, 2.0) - FastMath.pow(dy, 2.0));
						tmp = dy * FastMath.sin(incl_pole) + dz * FastMath.cos(incl_pole);
						dy = dy * FastMath.cos(incl_pole) - dz * FastMath.sin(incl_pole);
						dz = tmp;
						rr = 1.0;
						if (planet_render.target == TARGET.Moon || planet_render.target == TARGET.SUN || planet_render.target == TARGET.MERCURY || planet_render.target == TARGET.VENUS || planet_render.target == TARGET.EARTH)
							rr = -1.0;
						double lat = FastMath.atan2_accurate(dx, dz);
						double lon = Functions.normalizeRadians(incl_rotation + rr * lat);
						lat = Math.asin(-dy / Math.sqrt(dx * dx + dy * dy + dz * dz));

						lat = RenderPlanet.geocentricToGeodetic(planet_render.target.equatorialRadius, planet_render.target.polarRadius, lat);

						loc_out = new LocationElement(lon, lat, planet_render.target.ordinal());
						distMin = position[3];
					}
				}
			}
		}
		return loc_out;
	}

	/**
	 * Obtain ID constant of an object in certain screen position. For a valid
	 * output it is previously necessary to render the sky, and the object must
	 * be visible.
	 *
	 * @param x Horizontal position in the rendering in pixels.
	 * @param y Horizontal position in the rendering in pixels.
	 * @param consider_satellites True to consider possible transiting
	 *        satellites, false to ignore them.
	 * @param minimumSize Minimum size in pixels of the planet to consider it
	 * as identified for the input coordinates. Set to 0 to allow identifying
	 * planets event when their disks are almost invisible because of their
	 * minimum apparent sizes.
	 * @return ID value of the object, or {@linkplain TARGET#NOT_A_PLANET} if no object is
	 *         found.
	 * @throws JPARSECException If an error occurs.
	 */
	public TARGET getPlanetInScreenCoordinates(double x, double y, boolean consider_satellites, int minimumSize) throws JPARSECException
	{
		if (render.telescope.invertHorizontal) x = render.width-1-x;
		if (render.telescope.invertVertical) y = render.height-1-y;

		TARGET object = TARGET.NOT_A_PLANET;
		if (rec != null && !rec.contains((float)x, (float)y)) return object;
		if (planets == null) return object;
		double distMin = -1.0;
		for (int i = 1; i < planets.size(); i = i + 2)
		{
			float position[] = (float[]) planets.get(i - 1);
			PlanetRenderElement planet_render = (PlanetRenderElement) planets.get(i);
			EphemElement ephem = planet_render.ephem;
			TARGET target = planet_render.target;

			if (position[2] < 0.0 && consider_satellites)
			{
				target = Target.getID(planet_render.moonephem[(int) position[4]].name);
			}

			if (target == projection.obs.getMotherBody()) continue;

			// Recover information in a more confortable way
			float posx = position[0];
			float posy = position[1];

			// Prepare some variables
			double r = Math.abs(position[2]);
			if (r < minimumSize) continue;
			double dx = x - posx;
			double dy = posy - y;
			double rr = FastMath.hypot(dx, dy);

			if (projection.obs.getMotherBody() == TARGET.EARTH && render.drawSkyCorrectingLocalHorizon && projection.eph.isTopocentric) {
				double geoElev0 = Ephem.getGeometricElevation(projection.eph, projection.obs, ephem.elevation);
				double geoElevp = geoElev0 + ephem.angularRadius;
				double appElevp = Ephem.getApparentElevation(projection.eph, projection.obs, geoElevp, 10);
				double dp = (appElevp - ephem.elevation);
				float upperLimbFactor = (float) (dp / ephem.angularRadius);
				int de = (int) (r * (1.0 - upperLimbFactor) + 0.5);
				if (de > 1) {
					double angle = Constant.PI_OVER_TWO+projection.getCenitAngleAt(this.getSkyLocation((float)x, (float)y), false);
					double p[] = Functions.rotateZ(new double[] {dx, dy, 1}, -angle);
					p[1] /= upperLimbFactor;
					p = Functions.rotateZ(p, angle);
					dx = p[0];
					dy = p[1];
					rr = FastMath.hypot(dx, dy);
				}
			}

			// Set initial shape and size parameters
			double incl_axis = ephem.positionAngleOfAxis;
//			double incl_pole = ephem.positionAngleOfPole;
//			incl_pole = RenderPlanet.geodeticToGeocentric(planet_render.target.equatorialRadius, planet_render.target.polarRadius, incl_pole);
			double incl_north = -Constant.PI_OVER_TWO+projection.getNorthAngleAt(this.getSkyLocation((float)x, (float)y), false, false); //ephem.paralacticAngle;
			//if (planet_render.northUp) incl_north = 0.0;
			double oblateness = 1.0 + Math.abs(FastMath.pow(FastMath.cos(ephem.positionAngleOfPole), 2)) * (planet_render.target.polarRadius / planet_render.target.equatorialRadius - 1.0);
			double incl_up = incl_axis - incl_north;

			double tmp = rr / r;
			double ang = FastMath.atan2_accurate(dy, dx);
			dx = -rr * FastMath.cos(ang - incl_up) / r;
			dy = -rr * FastMath.sin(ang - incl_up) / (r * oblateness);
			if (oblateness != 1.0) tmp = (dx * dx + dy * dy);

			// Obtain planet
			if ((tmp <= 1.0) &&
					(position[3] < distMin || distMin == -1.0))
			{
				distMin = position[3];
				object = target;
			}
		}
		return object;
	}

	/**
	 * Obtain ID constant of the solar system object closest to certain screen position. For a valid
	 * output it is previously necessary to render the sky, and the object must
	 * be visible.
	 *
	 * @param x Horizontal position in the rendering in pixels.
	 * @param y Horizontal position in the rendering in pixels.
	 * @param consider_satellites True to consider possible transiting
	 *        satellites, false to ignore them.
	 * @return A array with three values (null if no object is found):<BR>
	 * ID value (TARGET) of the object.<BR>
	 * Minimum distance (Double) to the input position in pixels.<BR>
	 * Radius in pixels (Double) of the planet at input coordinates.
	 * @throws JPARSECException If an error occurs.
	 */
	public Object[] getClosestPlanetInScreenCoordinates(double x, double y, boolean consider_satellites) throws JPARSECException
	{
		if (render.telescope.invertHorizontal) x = render.width-1-x;
		if (render.telescope.invertVertical) y = render.height-1-y;

		double minDist = -1, minDist2 = -1, psize = -1;
		TARGET object = TARGET.NOT_A_PLANET;
		if (planets == null) return null;
		boolean mouseInsidePlanet = false;
		for (int i = 1; i < planets.size(); i = i + 2)
		{
			float position[] = (float[]) planets.get(i - 1);
			PlanetRenderElement planet_render = (PlanetRenderElement) planets.get(i);
			EphemElement ephem = planet_render.ephem;

			TARGET target = planet_render.target;

			if (position[2] < 0.0 && consider_satellites)
			{
				target = Target.getID(planet_render.moonephem[(int) position[4]].name);
				ephem = EphemElement.parseMoonEphemElement(planet_render.moonephem[(int) position[4]],	jd);
			}

			if (target == projection.obs.getMotherBody()) continue;

			// Recover information in a more confortable way
			float posx = position[0];
			float posy = position[1];

			// Prepare some variables
			double r = Math.abs(position[2]);
			double dx = x - posx;
			double dy = posy - y;
			double rr = FastMath.hypot(dx, dy);

			if (projection.obs.getMotherBody() == TARGET.EARTH && render.drawSkyCorrectingLocalHorizon && projection.eph.isTopocentric) {
				double geoElev0 = Ephem.getGeometricElevation(projection.eph, projection.obs, ephem.elevation);
				double geoElevp = geoElev0 + ephem.angularRadius;
				double appElevp = Ephem.getApparentElevation(projection.eph, projection.obs, geoElevp, 10);
				double dp = (appElevp - ephem.elevation);
				float upperLimbFactor = (float) (dp / ephem.angularRadius);
				int de = (int) (r * (1.0 - upperLimbFactor) + 0.5);
				if (de > 1) {
					double angle = Constant.PI_OVER_TWO+projection.getCenitAngleAt(this.getSkyLocation((float)x, (float)y), false);
					double p[] = Functions.rotateZ(new double[] {dx, dy, 1}, -angle);
					p[1] /= upperLimbFactor;
					p = Functions.rotateZ(p, angle);
					dx = p[0];
					dy = p[1];
					rr = FastMath.hypot(dx, dy);
				}
			}

			// Set initial shape and size parameters
			double incl_axis = ephem.positionAngleOfAxis;
//			double incl_pole = ephem.positionAngleOfPole;
//			incl_pole = RenderPlanet.geodeticToGeocentric(planet_render.target.equatorialRadius, planet_render.target.polarRadius, incl_pole);
			double incl_north = -Constant.PI_OVER_TWO+projection.getNorthAngleAt(this.getSkyLocation((float)x, (float)y), false, false); //ephem.paralacticAngle;
			//if (planet_render.northUp) incl_north = 0.0;
			double oblateness = 1.0 + Math.abs(FastMath.pow(FastMath.cos(ephem.positionAngleOfPole), 2)) * (planet_render.target.polarRadius / planet_render.target.equatorialRadius - 1.0);
			double incl_up = incl_axis - incl_north;

			double tmp = rr / r;
			double ang = FastMath.atan2_accurate(dy, dx);
			dx = -rr * FastMath.cos(ang - incl_up) / r;
			dy = -rr * FastMath.sin(ang - incl_up) / (r * oblateness);
			if (oblateness != 1.0) tmp = (dx * dx + dy * dy);

			// Obtain planet
			if ((rec == null || rec.contains(posx, posy) || tmp <= 1.0) &&
					(minDist2 > rr || minDist2 == -1 || tmp <= 1.0)
					)
			{
				if (!mouseInsidePlanet || (tmp <= 1.0 && (position[3] < minDist || minDist == -1.0))) {
					object = target;
					minDist = position[3];
					minDist2 = rr;
					psize = r;
					if (tmp <= 1.0) mouseInsidePlanet = true;
				}
			}
		}
		if (object == TARGET.NOT_A_PLANET) return null;
		return new Object[] {object, minDist2, psize};
	}

	/**
	 * Obtain screen coordinates of a given planetographic, heliographic,
	 * selenographic, or satellitographic position in certain object. The scene
	 * must be rendered previously.
	 *
	 * @param locIn Planetographic/heliographic/selenographic coordinates on the
	 *        object.
	 * @param body ID value for the object, as defined in class {@linkplain TARGET}.
	 * @param coordinate_system Reference coordinate system of input
	 *        coordinates, only for giant planets: 1 (system I), 2 (system II),
	 *        3 (system III).
	 * @param only_if_visible True for returning coordinates only if the input
	 *        position is visible.
	 * @return Corresponding position in the screen, or
	 *         {@linkplain Projection#INVALID_POSITION} (null) if the object is not visible, of the
	 *         position is not visible and only_if_visible is set to true.
	 * @throws JPARSECException If an error occurs.
	 */
	public float[] getScreenCoordinatesOfPlanetographicPosition(LocationElement locIn, TARGET body,
			int coordinate_system, boolean only_if_visible) throws JPARSECException
	{
		float pos[] = Projection.INVALID_POSITION;
		if (planets == null) return pos;

		for (int i = 1; i < planets.size(); i = i + 2)
		{
			float position[] = (float[]) planets.get(i - 1);
			PlanetRenderElement planet_render = (PlanetRenderElement) planets.get(i);
			EphemElement ephem = planet_render.ephem;

			if (position[2] < 0.0)
			{
				ephem = EphemElement.parseMoonEphemElement(planet_render.moonephem[(int) position[4]],	jd);
				planet_render.target = Target.getID(planet_render.moonephem[(int) position[4]].name);
			}

			if (planet_render.target == projection.obs.getMotherBody()) continue;

			if (planet_render.target == body)
			{
				LocationElement loc = locIn.clone();

				// Recover information in a more confortable way
				double incl_pole = ephem.positionAngleOfPole;
				// FIXME: This is fine, simply neglect this correction for RenderPlanet, since it is used
				// also in RenderEclipse in a big loop.
				if (!this.firstTime) {
					incl_pole = RenderPlanet.geodeticToGeocentric(planet_render.target.equatorialRadius, planet_render.target.polarRadius, incl_pole);
					loc.setLatitude(RenderPlanet.geodeticToGeocentric(planet_render.target.equatorialRadius, planet_render.target.polarRadius, loc.getLatitude()));
				}

				double incl_north = -Constant.PI_OVER_TWO+projection.getNorthAngleAt(ephem.getEquatorialLocation(), true, false); //ephem.paralacticAngle;
				if (planet_render.northUp) incl_north = 0.0;
				double incl_axis = ephem.positionAngleOfAxis;
				double incl_rotation = ephem.longitudeOfCentralMeridian;
				if (position[2] > 0.0 && (planet_render.target == TARGET.JUPITER || planet_render.target == TARGET.SATURN || planet_render.target == TARGET.URANUS || planet_render.target == TARGET.NEPTUNE))
				{
					switch (coordinate_system)
					{
					case 1:
						incl_rotation = ephem.longitudeOfCentralMeridianSystemI;
						break;
					case 2:
						incl_rotation = ephem.longitudeOfCentralMeridianSystemII;
						break;
					case 3:
						incl_rotation = ephem.longitudeOfCentralMeridianSystemIII;
						break;
					}
				}

				// Set initial shape and size parameters
				double oblateness = 1.0 + Math.abs(FastMath.pow(FastMath.cos(ephem.positionAngleOfPole), 2)) * (planet_render.target.polarRadius / planet_render.target.equatorialRadius - 1.0);
				double incl_up = (incl_axis - incl_north);

				// Obtain position
				double dxdz = FastMath.tan(loc.getLongitude() - incl_rotation);
				double dy = -FastMath.sin(loc.getLatitude());
				double dx2dz2 = 1.0 - dy * dy;
				double dz = Math.sqrt(dx2dz2 / (1.0 + dxdz * dxdz));
				double sign = 1.0;
				if (planet_render.target == TARGET.Moon || planet_render.target == TARGET.SUN || planet_render.target == TARGET.MERCURY || planet_render.target == TARGET.VENUS || planet_render.target == TARGET.EARTH)
					sign = -1.0;
				if (FastMath.sin(loc.getLongitude() - incl_rotation) < 0.0)
					sign = -sign;
				if (FastMath.cos(loc.getLongitude() - incl_rotation) < 0.0) dz = -dz;
				double dx = -Math.sqrt(Math.abs(1.0 - dy * dy - dz * dz)) * sign;
				double tmp = -dy * FastMath.sin(incl_pole) + dz * FastMath.cos(incl_pole);
				dy = dy * FastMath.cos(incl_pole) + dz * FastMath.sin(incl_pole);
				dz = tmp;
				dy = dy * oblateness;

				double rr = FastMath.hypot(dx, dy);
				double ang = FastMath.atan2_accurate(dy, dx);

				pos = new float[] {
						(float) (position[0] + rr * FastMath.cos(ang - incl_up) * Math.abs(position[2])),
						(float) (position[1] + rr * FastMath.sin(ang - incl_up) * Math.abs(position[2]))
				};

				if (only_if_visible && LocationElement.getApproximateAngularDistance(loc, new LocationElement(incl_rotation, incl_pole, 1.0)) > Constant.PI_OVER_TWO) {
					pos = Projection.INVALID_POSITION;
				} else {

					if (projection.obs.getMotherBody() == TARGET.EARTH && render.drawSkyCorrectingLocalHorizon && projection.eph.isTopocentric) {
						double geoElev0 = Ephem.getGeometricElevation(projection.eph, projection.obs, ephem.elevation);
						double geoElevp = geoElev0 + ephem.angularRadius;
						double appElevp = Ephem.getApparentElevation(projection.eph, projection.obs, geoElevp, 10);
						double dp = (appElevp - ephem.elevation);
						float upperLimbFactor = (float) (dp / ephem.angularRadius);
						int de = (int) (Math.abs(position[2]) * (1.0 - upperLimbFactor) + 0.5);
						if (de > 1) {
							double angle = Constant.PI_OVER_TWO+projection.getCenitAngleAt(ephem.getEquatorialLocation(), true);
							float posx = position[0];
							float posy = position[1];
							dx = pos[0] - posx;
							dy = posy - pos[1];
							double p[] = Functions.rotateZ(new double[] {dx, dy, 1}, -angle);
							p[1] *= upperLimbFactor;
							p = Functions.rotateZ(p, angle);
							pos[0] = (float) (p[0] + posx);
							pos[1] = (float) (-p[1] + posy);
						}
					}

					if (render.telescope.invertHorizontal) pos[0] = render.width-1-pos[0];
					if (render.telescope.invertVertical) pos[1] = render.height-1-pos[1];
				}
			}
		}

		return pos;
	}

	private ArrayList<float[]> faintStars = null;
	private LocationElement faintStarsLoc = null;
	private void drawFaintStars()
	throws Exception {
		g.setColor(render.drawStarsColor, true);
		float dist = getDist(-refz);
		int halo = 2, halo2 = 2*halo;

		ArrayList<Object> minorObjects = null;
		if (SaveObjectsToAllowSearch) {
			minorObjects = new ArrayList<Object>();
			Object o = null;
			if (db_minorObjects >= 0) {
				o = DataBase.getData(db_minorObjects);
			} else {
				o = DataBase.getData("minorObjects", threadID, true);
			}
			if (o != null) minorObjects = new ArrayList<Object>(Arrays.asList(((Object[]) o)));
		}

		int fontSize = render.drawStarsNamesFont.getSize();
		if (faintStars != null) {
			LocationElement loc;
			float[] d, pos;
			float size;

			ArrayList<Object> faintStarsList = new ArrayList<Object>();
			Object o = null;
			if (db_faintStars >= 0) {
				o = DataBase.getData(db_faintStars);
			} else {
				o = DataBase.getData("faintStars", threadID, true);
			}
			if (o != null) faintStarsList = new ArrayList<Object>(Arrays.asList(((Object[]) o)));
			Object o2 = DataBase.getData("faintStarsNames", threadID, true);
			ArrayList<Object> faintStarsNames = new ArrayList<Object>();
			if (o2 != null) faintStarsNames = new ArrayList<Object>(Arrays.asList(((Object[]) o2)));

			for (int index = 0; index < faintStarsList.size(); index ++)
			{
				d = (float[]) faintStarsList.get(index);
				loc = new LocationElement(d[0], d[1], d[2]);

		    	if (d[3] < maglim) {
					pos = projection.projectPosition(loc, 0, false);
					if (pos != null && !this.isInTheScreen((int)pos[0], (int)pos[1])) pos = null;
					if (!projection.isInvalid(pos))
					{
						size = getSizeForAGivenMagnitude(d[3]);
						int tsize = (int) (0.5 + (1.5f * size+adds));
						if (tsize >= 1 && render.drawStarsRealistic != REALISTIC_STARS.NONE &&
								render.drawStarsRealistic != REALISTIC_STARS.NONE_CUTE) {
							this.drawStar(tsize, pos, dist, -1, g);
						} else {
							tsize = (int) (2* size+adds);
							if (render.drawStarsRealistic == REALISTIC_STARS.NONE_CUTE) {
								int c = g.getColor();
								g.setColor(render.background, true);
								int tx = (int)pos[0] - (int)size;
								int ty = (int)pos[1] - (int)size;
								if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
									g.fillOval(tx-halo, ty-halo, tsize+halo2, tsize+halo2, this.fast);
								} else {
									g.fillOval(tx-halo, ty-halo, tsize+halo2, tsize+halo2, dist);
								}
								g.setColor(c, true);
							}

							if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
								g.fillOval((pos[0] - size), (pos[1] - size), tsize, tsize, this.fast);
							} else {
								g.fillOval((pos[0] - size), (pos[1] - size), tsize, tsize, dist);
							}
						}
		 				if (magLabelCount < 20 && render.drawMagnitudeLabels && d[3] < maglim - 2) {
		 					magLabelCount ++;
		 					float position = Math.max(size * 3, size+fontSize);
							drawString(render.drawStarsColor, render.drawStarsNamesFont, Functions.formatValue(d[3], 1), pos[0], pos[1], -position, false);
		 				}
		 				
		 				if (SaveObjectsToAllowSearch) {
/*					    	String name2print = "";
					    	if (data.length() > 64) {
					    		name2print = data.substring(63).trim();
					    		name2print = DataSet.replaceAll(name2print, "  ", ",", true);
					    		name2print = DataSet.replaceAll(name2print, ",,", ",", true);
					    	}
*/							minorObjects.add(new Object[] {
									RenderSky.OBJECT.DEEPSKY, pos.clone(), loc, d[3], 
									(String) faintStarsNames.get(index)
							});
		 				}		 				
					}
				}
			}

			if (SaveObjectsToAllowSearch) {
				DataBase.addData("minorObjects", threadID, minorObjects.toArray(), true);
				if (db_minorObjects < 0) db_minorObjects = DataBase.getIndex("minorObjects", threadID);
			}			
		} else {
			LocationElement locEq;
	 		String ra0 = Functions.formatRA(loc0J2000.getLongitude());
			String dec0 = Functions.formatDEC(loc0J2000.getLatitude());
			String name = Functions.getHoursFromFormattedRA(ra0)+":"+Functions.getMinutesFromFormattedRA(ra0)+":"+
				Functions.getSecondsFromFormattedRA(ra0) + Functions.getDegreesFromFormattedDEC(dec0) + ":"+
				Functions.getMinutesFromFormattedDEC(dec0)+ ":"+Functions.getSecondsFromFormattedDEC(dec0);
			name = name.replaceAll(",", ".");

			String limmag0 = "9.5", limmag = Functions.formatValue(render.drawStarsLimitingMagnitude,1);
			if (g.renderingToAndroid()) limmag0 = "8.5";
			String field = "1.6"; //Functions.formatValue(2, 1);
			String datas = getOldUCAC4(loc0J2000, field, limmag0, limmag);
			if (datas == null) {
				String query = "http://vizier.u-strasbg.fr/viz-bin/VizieR?-source=UCAC4&-c="+name+"&-c.rd="+field+"&-mime=ascii&-out.form=csv&-oc.form=dec&-out.max=20000&-out=RAJ2000,DEJ2000,f.mag,a.mag,pmRA,pmDE,UCAC4,Tycho-2&f.mag="+limmag0+".."+limmag;
				try {
					datas = GeneralQuery.query(query, render.drawFaintStarsTimeOut*1000);
					String out = FileIO.getTemporalDirectory() + "ucac4_"+field+"_"+limmag0+"_"+limmag+"_"+(float)loc0J2000.getLongitude()+"_"+(float)loc0J2000.getLatitude()+".txt";
					WriteFile.writeAnyExternalFile(out, datas);
				} catch (Exception exc) {
			    	faintStars = new ArrayList<float[]>();
					faintStars.add(new float[] {(float) render.centralLongitude, (float) render.centralLatitude, 0, 0});
					JPARSECException.addWarning("cannot query UCAC4 catalog, timeout expired ("+render.drawFaintStarsTimeOut+"s). Please disable UCAC4 queries if you don't have Internet connection available.");
				}
				faintStarsLoc = loc0J2000.clone();
			}
			if (datas != null && !datas.isEmpty()) {
				int init = datas.indexOf(FileIO.getLineSeparator()+"<HR>"+FileIO.getLineSeparator())+6;
				int end = datas.indexOf(FileIO.getLineSeparator()+"<HR></PRE>"+FileIO.getLineSeparator());
				if (init >= 0 && end >= 0 && init < end) {
					datas = datas.substring(init, end);
					String file[] = DataSet.toStringArray(datas, FileIO.getLineSeparator());
					int nstars = file.length;

			    	String ra = "", dec = "", pmRA = "", pmDEC = "", mag1 = "", mag2 = "";
			    	double magV = -100.0;
			    	faintStars = new ArrayList<float[]>();
			    	ArrayList<String> faintStarsNames = new ArrayList<String>();
			    	double cte = Constant.RAD_TO_ARCSEC * 1000.0;
			    	float[] pos;
			    	float size;
					EphemerisElement eph = projection.eph.clone();
					eph.algorithm = EphemerisElement.ALGORITHM.STAR;
					if (baryc == null) baryc = Ephem.eclipticToEquatorial(PlanetEphem.getGeocentricPosition(equinox, TARGET.Solar_System_Barycenter, 0.0, false, projection.obs), Constant.J2000, projection.eph);

					boolean calc = false;
					// FIXME ? Seems unnecesary since very dim UCAC4 stars have no information about proper motions
/*					calc = true;
					if (nstars > 100) {
						int selection = JOptionPane.showConfirmDialog(null,
								DataSet.replaceAll(Translate.translate(951), "%nstars", ""+nstars, true),
								Translate.translate(952),
								JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
						if (selection == JOptionPane.NO_OPTION) calc = false;
					}
*/
					ArrayList<float[]> faintStarsList = new ArrayList<float[]>();
				    for (int i=0; i<nstars; i++)
				    {
				    	magV = -100.0;
				    	String data = file[i];
				    	init = data.indexOf(">");
				    	if (init > 0) {
				    		pmRA = pmDEC = mag2 = "";
					    	data = data.substring(init+1);

					    	init = data.indexOf("<");
					    	end = data.indexOf(">");
					    	if (init >=0 && end > 0) {
					    		do {
					    			String d = data.substring(0, init) + data.substring(end+1);
					    			data = d;
							    	init = data.indexOf("<");
							    	end = data.indexOf(">");
					    		} while (init >=0 && end > 0);
					    	}
					    	ra = data.substring(2, 12);
					    	dec = data.substring(15, 25);
					    	mag1 = data.substring(28, 33).trim();
					    	if (data.length()>40) mag2 = data.substring(36, 41).trim();
					    	if (data.length()>50) {
					    		pmRA = data.substring(47, 51).trim();
						    	pmDEC = data.substring(58, 62).trim();
					    	}
					    	
					    	String name2print = "UCAC ?";
					    	if (data.length() > 64) {
					    		name2print = data.substring(63).trim();
					    		String ucac = FileIO.getField(1, name2print, "  ", true);
					    		String tycho = FileIO.getField(2, name2print, "  ", true);
					    		name2print = "UCAC4 "+ucac;
					    		if (!tycho.equals("")) name2print += ", Tycho-2 "+tycho;
					    	}
					    	faintStarsNames.add(name2print);

					    	if (mag2.isEmpty()) {
					    		if (!mag1.isEmpty()) magV = Double.parseDouble(mag1);
					    	} else {
					    		magV = Double.parseDouble(mag2);
					    	}
					    	if (magV != -100 && !ra.isEmpty() && !dec.isEmpty())
					    	{
					    		StarEphemElement ephem = new StarEphemElement();
								StarElement star = new StarElement();
					    		LocationElement loc = new LocationElement(
					    				Double.parseDouble(ra) * Constant.DEG_TO_RAD,
					    				Double.parseDouble(dec) * Constant.DEG_TO_RAD,
					    				1.0);
					    		star.rightAscension = loc.getLongitude();
					    		star.declination = loc.getLatitude();
					    		star.parallax = 1000.0 / 500.0;
					    		star.equinox = Constant.J2000;
					    		star.frame = EphemerisElement.FRAME.FK5;
					    		star.magnitude = (float) magV;
					    		star.properMotionRadialV = 0.0f;
					    		if (pmRA.isEmpty()) pmRA = "0";
					    		if (pmDEC.isEmpty()) pmDEC = "0";
					    		star.properMotionDEC = (float) (Double.parseDouble(pmDEC) / cte);
					    		star.properMotionRA = (float) (Double.parseDouble(pmRA) / (cte * FastMath.cos(star.declination)));
								if (calc && (star.properMotionDEC != 0.0 || star.properMotionRA != 0.0))
								{
						    		ephem = StarEphem.starEphemeris(projection.time, projection.obs, eph, star, false);
								} else
								{
									// Correct for aberration, precession, and nutation
									if (projection.eph.ephemType == COORDINATES_TYPE.APPARENT) {
										loc.setRadius(1000 * Constant.RAD_TO_ARCSEC); // 1000 pc
										double light_time = loc.getRadius() * Constant.LIGHT_TIME_DAYS_PER_AU;
										double r[] = Ephem.aberration(loc.getRectangularCoordinates(), baryc, light_time);

										r = precessFromJ2000(equinox, r, projection.eph);
										locEq = LocationElement.parseRectangularCoordinates(nutateInEquatorialCoordinates(equinox, projection.eph, r, true));
									} else {
										locEq = LocationElement.parseRectangularCoordinates(precessFromJ2000(equinox,
												LocationElement.parseLocationElement(loc), projection.eph));
									}

									ephem.rightAscension = locEq.getLongitude();
									ephem.declination = locEq.getLatitude();
									ephem.distance = star.getDistance();
									ephem.magnitude = star.magnitude;
								}

								loc = projection.getApparentLocationInSelectedCoordinateSystem(new LocationElement(ephem.rightAscension, ephem.declination, 1.0), true, false, 0);
								if (loc != null) {
									if (faintStars.size() == 0) faintStars.add(new float[] {(float) loc.getLongitude(), (float) loc.getLatitude(), (float) ephem.distance, ephem.magnitude});
									faintStarsList.add(new float[] {(float) loc.getLongitude(), (float) loc.getLatitude(), (float) ephem.distance, ephem.magnitude});

									if (ephem.magnitude < maglim) {
										pos = projection.projectPosition(loc, 0, false);
										if (pos != null && !this.isInTheScreen((int)pos[0], (int)pos[1])) pos = null;
										if (!projection.isInvalid(pos))
										{
											size = getSizeForAGivenMagnitude(ephem.magnitude);
											int tsize = (int) (0.5 + (1.5f * size+adds));
											if (tsize >= 1 && render.drawStarsRealistic != REALISTIC_STARS.NONE &&
													render.drawStarsRealistic != REALISTIC_STARS.NONE_CUTE) {
												this.drawStar(tsize, pos, dist, -1, g);
											} else {
												tsize = (int) (2* size+adds);
												if (render.drawStarsRealistic == REALISTIC_STARS.NONE_CUTE) {
													int c = g.getColor();
													g.setColor(render.background, true);
													int tx = (int)pos[0] - (int)size;
													int ty = (int)pos[1] - (int)size;
													if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
														g.fillOval(tx-halo, ty-halo, tsize+halo2, tsize+halo2, this.fast);
													} else {
														g.fillOval(tx-halo, ty-halo, tsize+halo2, tsize+halo2, dist);
													}
													g.setColor(c, true);
												}

												if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
													g.fillOval((pos[0] - size), (pos[1] - size), tsize, tsize, this.fast);
												} else {
													g.fillOval((pos[0] - size), (pos[1] - size), tsize, tsize, dist);
												}
											}
							 				if (magLabelCount < 20 && render.drawMagnitudeLabels && ephem.magnitude < maglim - 2) {
							 					magLabelCount ++;
							 					float position = Math.max(size * 3, size+fontSize);
												drawString(render.drawStarsColor, render.drawStarsNamesFont, Functions.formatValue(ephem.magnitude, 1), pos[0], pos[1], -position, false);
							 				}
							 				
							 				if (SaveObjectsToAllowSearch) {
												minorObjects.add(new Object[] {
														RenderSky.OBJECT.DEEPSKY, pos.clone(), loc, ephem.magnitude, name2print
												});
							 				}
										}
									}
						    	}
					    	}
				    	}
				    }

				    DataBase.addData("faintStars", threadID, faintStarsList.toArray(), true);
				    DataBase.addData("faintStarsNames", threadID, faintStarsNames.toArray(), true);
				    db_faintStars = DataBase.getIndex("faintStars", threadID);
				}
			}
		}
	}

	private String getOldUCAC4(LocationElement loc, String field, String lim0, String lim) throws Exception {
		String out = null, fout = "ucac4_"+field+"_"+lim0+"_"+lim;
		String f[] = FileIO.getFiles(FileIO.getTemporalDirectory());
		double minDist = -1;
		for (int i=0; i<f.length; i++) {
			String s = FileIO.getFileNameFromPath(f[i]);
			if (s.startsWith(fout)) {
				s = s.substring(0, s.lastIndexOf("."));
				double lon = Double.parseDouble(FileIO.getField(5, s, "_", true));
				double lat = Double.parseDouble(FileIO.getField(6, s, "_", true));
				LocationElement l = new LocationElement(lon, lat, 1);
				double dist = LocationElement.getApproximateAngularDistance(l, loc) * Constant.RAD_TO_DEG;
				if (dist < 1.5) {
					if (dist < minDist || minDist == -1) {
						minDist = dist;
						out = DataSet.arrayListToString(ReadFile.readAnyExternalFile(f[i]));
						faintStarsLoc = l;
					}
				}
			}
		}
		return out;
	}

	/**
	 * Sets the longitude of Jupiter's Great Red Spot.
	 * <P>
	 * Please note that the rendering will use the default value of the
	 * longitude of central meridian for rendering. This value is refered to
	 * System III for giant planets (rotation of the magnetic field), so the
	 * apparent rotation will not match that of the observed equatorial nor
	 * tropical belts in these planets. This function is intended to adjust
	 * specifically the apparent sight of Jupiter's disk.
	 *
	 * @param GRS_lon Observed longitude in radians. Typically obtained
	 * for a given date calling {@linkplain MainEvents#getJupiterGRSLongitude(double)}.
	 * @param system System of coordinates of GRS_lon, 1, 2, or 3. Will be
	 *        ussually 2, since the Great Red Spot is in the tropical belt (1
	 *        refers to equatorial belt, and 3 to the rotation of the magnetic
	 *        field).
	 */
	public void setJupiterGRSLongitude(double GRS_lon, int system)
	{
		double lon0 = (270.0 / 2000.0) * Constant.TWO_PI;
		double offset = lon0 - GRS_lon;

		offsetInLongitudeOfJupiterGRS = offset;
		offsetInLongitudeOfJupiterGRS_system = system;
	}

	/**
	 * Returns the longitude of the GRS supposing that the longitude
	 * was updated using system 2 with {@linkplain RenderSky#setJupiterGRSLongitude(double, int)}.
	 * @param system The system on Jupiter, 1, 2, or 3.
	 * @return GRS longitude in radians.
	 */
	public double getJupiterGRSLongitude(int system) {
		double lon0 = (270.0 / 2000.0) * Constant.TWO_PI;
		double lon = lon0 - offsetInLongitudeOfJupiterGRS;
		EphemElement ephem = majorObjects[4];
		if (system == 1) lon += ephem.longitudeOfCentralMeridianSystemI - ephem.longitudeOfCentralMeridianSystemII;
		if (system == 3) lon += ephem.longitudeOfCentralMeridianSystemIII - ephem.longitudeOfCentralMeridianSystemII;
		return lon;
	}

	private double offsetInLongitudeOfJupiterGRS = 0.0;
	private int offsetInLongitudeOfJupiterGRS_system = 1;

	public int leyendMargin = 51;
    public int graphMarginX = 28;
    public int graphMarginY = 38;
    private String labelRA = null, labelDEC = null;

    private static String fixLabel(String label, boolean ra) {
    	int f = 1;
    	if (label.startsWith("-")) f = -1;
		double add = 0.15*f*Constant.ARCSEC_TO_RAD;
    	if (label.endsWith("60'")) {
    		if (ra) {
        		double val = Functions.parseRightAscension(label) + add;
    			label = Functions.formatRAOnlyMinutes(val, 0);
    		} else {
        		double val = Functions.parseDeclination(label) + add;
    			label = Functions.formatDECOnlyMinutes(val, 0);    			
    		}
    		if (label.startsWith("360\u00b0")) label = "00\u00b0";
    		return label;
    	}
    	if (label.endsWith("60\"")) {
    		if (ra) {
        		double val = Functions.parseRightAscension(label) + add;
    			label = Functions.formatRA(val, 0);
    		} else {
        		double val = Functions.parseDeclination(label) + add;
    			label = Functions.formatDEC(val, 0);    			
    		}    		
    		if (label.startsWith("360\u00b0")) label = "00\u00b0";
    	}
    	if (label.endsWith("60s") && ra) {
    		double val = Functions.parseRightAscension(label) + add;
			label = Functions.formatRA(val, 0);    		
    	}
		if (label.startsWith("360\u00b0")) label = "00\u00b0";
    	return label;
    }
    private void drawAxesLabels() throws JPARSECException
    {
    	float dist = refz; //getDist(this.axesDist);
		g.setColor(render.drawCoordinateGridColor, 255);
		g.setFont(render.drawCoordinateGridFont);
		g.disableInversion();
		int fs = g.getFont().getSize();

/*    	if ((firstTime || g.renderingToAndroid()) && render.drawCoordinateGrid) {
    		if (labelRA == null) {
    			labelRA = t21;
    			if (render.coordinateSystem == CoordinateSystem.COORDINATE_SYSTEM.ECLIPTIC) labelRA = t24;
    			if (render.coordinateSystem == CoordinateSystem.COORDINATE_SYSTEM.GALACTIC) labelRA = t26;
    			if (render.coordinateSystem == CoordinateSystem.COORDINATE_SYSTEM.HORIZONTAL) labelRA = t28;
    		}

    		float w = g.getStringWidth(labelRA);
    		if (labelRA.equals(t21)) {
    			g.drawString(labelRA, graphMarginX+(render.width-graphMarginX-w)/2f, (int) (rec.getMaxY()+fs*1.7), dist);
    		} else {
    			g.drawString(labelRA, graphMarginX+(render.width-graphMarginX-w)/2f, (int) (rec.getMaxY()+fs*1.5), dist);
    		}

    		if (labelDEC == null) {
    			labelDEC = t22;
    			if (render.coordinateSystem == CoordinateSystem.COORDINATE_SYSTEM.ECLIPTIC) labelDEC = t25;
    			if (render.coordinateSystem == CoordinateSystem.COORDINATE_SYSTEM.GALACTIC) labelDEC = t27;
    			if (render.coordinateSystem == CoordinateSystem.COORDINATE_SYSTEM.HORIZONTAL) labelDEC = t29;
    		}
    		Rectangle bounds = g.getStringBounds(labelDEC);
    		w = (float) bounds.getWidth();
    		float h = (float) bounds.getHeight();
    		g.drawRotatedString(labelDEC, rec.getMinX()-graphMarginX+(h/2.0f+ (g.renderingToAndroid() ? 11f : 5.5f)), (render.height+w)/2f, (float)Constant.PI_OVER_TWO, dist);
    	}
*/
		if (render.drawCoordinateGrid && !g.renderingToExternalGraphics()) {
	   		if (labelRA == null) {
    			labelRA = Translate.translate(CoordinateSystem.COORDINATE_SYSTEMS[render.coordinateSystem.ordinal()]).substring(0, 3);
    		}
   			g.drawString(labelRA, 0*graphMarginX+0*(render.width-graphMarginX)/2f, (int) (rec.getMaxY()+fs*1.5), dist);
		}


    	if (!render.drawExternalGrid) {
			g.enableInversion();
    		return;
    	}

		g.setStroke(render.drawCoordinateGridStroke);
		boolean thin = false;
		if (render.drawCoordinateGridStroke.getLineWidth() == JPARSECStroke.STROKE_DEFAULT_LINE_THIN.getLineWidth()) thin = true;

		int ndec = 0;
		if (fieldDeg < 0.5) ndec = 1;
		if (fieldDeg < 0.05) ndec = 2;
		int ls = g.renderingToAndroid() ? 8 : 4;
		if (labelsAxesX.size() == 0) {
			int cx = this.getXCenter();
			if (render.telescope.invertHorizontal) cx = render.width-1-cx;
			int py = render.height-this.graphMarginY;
			LocationElement loc = this.getSkyLocation(cx, py);
			if (loc != null) {
				String labelRA = null;
				if (render.coordinateSystem != COORDINATE_SYSTEM.EQUATORIAL) {
					if (pixels_per_degree < 1000) {		
						if (pixels_per_degree < 100) {
							labelRA = Functions.formatAngleAsDegrees(loc.getLongitude(), 1) + "\u00b0"; //render.centralLongitude, ndec+1);												
						} else {
							labelRA = Functions.formatDECOnlyMinutes(loc.getLongitude(), 1); //render.centralLongitude, ndec+1);						
						}
						if (render.coordinateSystem == COORDINATE_SYSTEM.HORIZONTAL) {
							double cl = Functions.normalizeRadians(loc.getLongitude());
							if (cl < deg_10 || cl > Constant.TWO_PI - deg_10) labelRA += " (N)";
							if (cl < Math.PI + deg_10 && cl > Math.PI - deg_10) labelRA += " (S)";
							if (cl < Constant.PI_OVER_TWO + deg_10 && cl > Constant.PI_OVER_TWO - deg_10) labelRA += " (E)";
							if (cl < 3*Constant.PI_OVER_TWO + deg_10 && cl > 3*Constant.PI_OVER_TWO - deg_10) {
								if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) {
									labelRA += " (O)";
								} else {
									labelRA += " (W)";
								}
							}
							if (cl < Constant.PI_OVER_FOUR + deg_10 && cl > Constant.PI_OVER_FOUR - deg_10) labelRA += " (NE)";
							if (cl < Math.PI - Constant.PI_OVER_FOUR + deg_10 && cl > Math.PI - Constant.PI_OVER_FOUR - deg_10) labelRA += " (SE)";
							if (cl < Constant.TWO_PI - Constant.PI_OVER_FOUR + deg_10 && cl > Constant.TWO_PI - Constant.PI_OVER_FOUR - deg_10) {
								if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) {
									labelRA += " (NO)";
								} else {
									labelRA += " (NW)";
								}
							}
							if (cl < Math.PI + Constant.PI_OVER_FOUR + deg_10 && cl > Math.PI + Constant.PI_OVER_FOUR - deg_10) {
								if (Translate.getDefaultLanguage() == LANGUAGE.SPANISH) {
									labelRA += " (SO)";
								} else {
									labelRA += " (SW)";
								}
							}
						}
					} else {
						labelRA = Functions.formatAngle(loc.getLongitude(), ndec+1); //render.centralLongitude, ndec+1);
					}
				} else {
					if (pixels_per_degree < 1000) {						
						if (pixels_per_degree < 100) {
							labelRA = Functions.formatRAOnlyMinutes(loc.getLongitude(), 1);
						} else {
							labelRA = Functions.formatRAOnlyMinutes(loc.getLongitude(), 2);
						}
					} else {
						labelRA = Functions.formatRA(loc.getLongitude(), ndec+1); //render.centralLongitude, ndec+1);
					}
				}
				float w = g.getStringWidth(labelRA);
				g.drawString(labelRA, graphMarginX+(render.width-graphMarginX-w)/2f, (int) (rec.getMaxY()+fs*1.7), dist);
			}
			if (render.drawLeyend == LEYEND_POSITION.BOTTOM) py -= this.leyendMargin;

			if (fast && thin) {
				g.drawStraightLine(cx, py, cx, py+ls);
			} else {
				g.drawLine(cx, py, cx, py+ls, dist, dist);
			}
		}

		if (labelsAxesY.size() == 0) {
			String labelDEC = null;
			if (pixels_per_degree < 1000) {						
				if (pixels_per_degree < 100) {
					labelDEC = Functions.formatAngleAsDegrees(render.centralLatitude, 1) + "\u00b0"; //render.centralLongitude, ndec+1);												
				} else {
					labelDEC = Functions.formatDECOnlyMinutes(render.centralLatitude, 1); //render.centralLongitude, ndec+1);						
				}
			} else {
				labelDEC = Functions.formatAngle(render.centralLatitude, ndec); //render.centralLongitude, ndec+1);
			}
			float w = g.getStringWidth(labelDEC);
			float dy = rec.getMinX()-render.drawCoordinateGridFont.getSize();
			g.drawRotatedString(labelDEC, dy, (render.height+w)/2f, (float)Constant.PI_OVER_TWO, dist);
			int cy = this.getYCenter();
			if (fast && thin) {
				g.drawStraightLine(this.graphMarginX, cy, this.graphMarginX-ls, cy, dist, dist);
			} else {
				g.drawLine(this.graphMarginX, cy, this.graphMarginX-ls, cy, dist, dist);
			}
		}

		if (fieldDeg >= 20) {
			g.enableInversion();
			return;
		}

		boolean invertH = render.telescope.invertHorizontal;
		boolean invertV = render.telescope.invertVertical;
		if (projection.isCylindricalForced()) {
			g.enableInversion(invertH, false);
			double step = 180; // arcmin
			if (render.coordinateSystem == COORDINATE_SYSTEM.EQUATORIAL) step = 150;
			if (fieldDeg < 20*FastMath.cos(render.centralLatitude)) {
				step = 60;
				if (render.coordinateSystem == COORDINATE_SYSTEM.EQUATORIAL) step = 75;
			}
			int l = g.renderingToAndroid() ? 10 : 6;
			if (fieldDeg < 5) {
				step = 30;
				l = g.renderingToAndroid() ? 8 : 4;
			}
			if (fieldDeg < 1*FastMath.cos(render.centralLatitude)) {
				step = 5;
				l = g.renderingToAndroid() ? 6 : 3;
			}
			if (fieldDeg < 0.2*FastMath.cos(render.centralLatitude)) {
				step = 1;
				l = g.renderingToAndroid() ? 6 : 3;
			}

			double step2 = step * deg_0_017;
			double closest = render.centralLongitude / step2;
			closest = Math.round(closest) * step2;
			LocationElement loc1 = this.getSkyLocation(0, render.height-this.graphMarginY);
			LocationElement loc2 = this.getSkyLocation(1000, render.height-this.graphMarginY);
			double dlon = Math.abs(loc1.getLongitude()-loc2.getLongitude());
			if (dlon > Math.PI) dlon = Constant.TWO_PI - dlon;
			double ppr = 1000.0 / dlon;
			double dec = loc1.getLatitude();
			step = step2 * ppr;
			boolean somethingWritten = false;
			double arcsec900 = 900.0 * Constant.ARCSEC_TO_RAD;

			if (step > 5 && render.centralLatitude < 80 * Constant.DEG_TO_RAD) {
				LocationElement loc = new LocationElement(closest, dec, 1.0);
				projection.disableCorrectionOfLocalHorizon();
				loc = projection.toEquatorialPosition(loc, false);
				loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, true, false, 0);
				projection.enableCorrectionOfLocalHorizon();
				float pos[] = projection.projectPosition(loc, 0, false, 1);
				int py = render.height-this.graphMarginY;
				if (render.drawLeyend == LEYEND_POSITION.BOTTOM) py -= this.leyendMargin;
				String s = null;
				if (pos != null) {
					int min = render.width; //(int) Math.min(render.width, rec.getMaxX());
					int max = 0; //(int) Math.max(0, rec.getMinX());
					double x = pos[0];
					if (render.drawCoordinateGridHighZoom) {
						if (render.coordinateSystem == COORDINATE_SYSTEM.EQUATORIAL) {
							if (step2 < arcsec900) {
								s = fixLabel(Functions.formatRA(loc.getLongitude(), 0), true);
							} else {
								s = fixLabel(Functions.formatRAOnlyMinutes(loc.getLongitude(), 0), true);
							}
						} else {
							loc.setLongitude(Functions.normalizeRadians(loc.getLongitude()));
							if (step2 < arcsec900) {
								s = fixLabel(Functions.formatDEC(loc.getLongitude(), 0), false);
							} else {
								s = fixLabel(Functions.formatDECOnlyMinutes(loc.getLongitude(), 0), false);
							}
						}
						//s = s.substring(s.indexOf(" ")).trim();
						if (s.endsWith("00\"")) s = s.substring(0, s.length()-3).trim();
						if (s.endsWith("00'")) s = s.substring(0, s.length()-3).trim();
//						if (s.startsWith("00") && s.length() > 3) s = s.substring(3).trim();
						if (s.equals("")) s = null;
					}
					if (!somethingWritten) {
						int c = g.getColor();
						g.setColor(render.background, true);
						int x0 = (int)(graphMarginX+(render.width-graphMarginX)/2f);
						if (labelsAxesX.size() == 0) {
							g.fillRect(0, (int) (rec.getMaxY()+fs*1.7)-fs-3, x0-100, fs+4);
							g.fillRect(x0+100, (int) (rec.getMaxY()+fs*1.7)-fs-3, render.width-(x0+100), fs+4);
						} else {
							g.fillRect(0, (int) (rec.getMaxY()+fs*1.7)-fs-3, render.width, fs+4);
						}
						g.setColor(c, true);
						somethingWritten = true;
					}
					drawLine1((int)(x), py, (int)(x), py+l, dist, dist, g, thin, s);
					do {
						x += step;
						if (render.drawCoordinateGridHighZoom) {
							loc = this.getSkyLocation(render.telescope.invertHorizontal ? (float) (render.width-1-x) : (float) x, py);
							if (render.coordinateSystem == COORDINATE_SYSTEM.EQUATORIAL) {
								if (step2 < arcsec900) {
									s = fixLabel(Functions.formatRA(loc.getLongitude(), 0), true);
								} else {
									s = fixLabel(Functions.formatRAOnlyMinutes(loc.getLongitude(), 0), true);
								}
							} else {
								loc.setLongitude(Functions.normalizeRadians(loc.getLongitude()));
								if (step2 < arcsec900) {
									s = fixLabel(Functions.formatDEC(loc.getLongitude(), 0), false);
								} else {
									s = fixLabel(Functions.formatDECOnlyMinutes(loc.getLongitude(), 0), false);
								}
							}
							//s = s.substring(s.indexOf(" ")).trim();
							if (s.endsWith("00\"")) s = s.substring(0, s.length()-3).trim();
							if (s.endsWith("00'")) s = s.substring(0, s.length()-3).trim();
//							if (s.startsWith("00") && s.length() > 3) s = s.substring(3).trim();
							if (s.equals("")) s = null;
						}
						if (x < min) {
							if (!somethingWritten) {
								int c = g.getColor();
								g.setColor(render.background, true);
								int x0 = (int)(graphMarginX+(render.width-graphMarginX)/2f);
								if (labelsAxesX.size() == 0) {
									g.fillRect(0, (int) (rec.getMaxY()+fs*1.7)-fs-3, x0-100, fs+4);
									g.fillRect(x0+100, (int) (rec.getMaxY()+fs*1.7)-fs-3, render.width-(x0+100), fs+4);
								} else {
									g.fillRect(0, (int) (rec.getMaxY()+fs*1.7)-fs-3, render.width, fs+4);
								}
								g.setColor(c, true);
								somethingWritten = true;
							}
							drawLine1((int)(x), py, (int)(x), py+l, dist, dist, g, thin, s);
						}
					} while (x < (min));

					x = pos[0];
					do {
						x -= step;
						if (render.drawCoordinateGridHighZoom) {
							loc = this.getSkyLocation(render.telescope.invertHorizontal ? (float) (render.width-1-x) : (float) x, py);
							if (render.coordinateSystem == COORDINATE_SYSTEM.EQUATORIAL) {
								if (step2 < arcsec900) {
									s = fixLabel(Functions.formatRA(loc.getLongitude(), 0), true);
								} else {
									s = fixLabel(Functions.formatRAOnlyMinutes(loc.getLongitude(), 0), true);
								}
							} else {
								if (step2 < arcsec900) {
									s = fixLabel(Functions.formatDEC(loc.getLongitude(), 0), false);
								} else {
									s = fixLabel(Functions.formatDECOnlyMinutes(loc.getLongitude(), 0), false);
								}
							}
							//s = s.substring(s.indexOf(" ")).trim();
							if (s.endsWith("00\"")) s = s.substring(0, s.length()-3).trim();
							if (s.endsWith("00'")) s = s.substring(0, s.length()-3).trim();
//							if (s.startsWith("00") && s.length() > 3) s = s.substring(3).trim();
							if (s.equals("")) s = null;
						}
						if (x > max) {
							if (!somethingWritten) {
								int c = g.getColor();
								g.setColor(render.background, true);
								int x0 = (int)(graphMarginX+(render.width-graphMarginX)/2f);
								if (labelsAxesX.size() == 0) {
									g.fillRect(0, (int) (rec.getMaxY()+fs*1.7)-fs-3, x0-100, fs+4);
									g.fillRect(x0+100, (int) (rec.getMaxY()+fs*1.7)-fs-3, render.width-(x0+100), fs+4);
								} else {
									g.fillRect(0, (int) (rec.getMaxY()+fs*1.7)-fs-3, render.width, fs+4);
								}
								g.setColor(c, true);
								somethingWritten = true;
							}
							drawLine1((int)(x), py, (int)(x), py+l, dist, dist, g, thin, s);
						}
					} while (x > max);
				}
			}

			g.enableInversion(false, invertV);
			step = 60; // arcmin
			l = g.renderingToAndroid() ? 10 : 6;
			if (fieldDeg < 5) {
				step = 30;
				l = g.renderingToAndroid() ? 8 : 4;
			}
			if (fieldDeg < 1) {
				step = 5;
				l = g.renderingToAndroid() ? 6 : 3;
			}
			if (fieldDeg < 0.2) {
				step = 1;
				l = g.renderingToAndroid() ? 6 : 3;
			}

			step2 = step * deg_0_017;
			closest = render.centralLatitude / step2;
			closest = Math.round(closest) * step2;
			step = step2 * pixels_per_radian;
			LocationElement loc = new LocationElement(render.centralLongitude, closest, 1.0);
			projection.disableCorrectionOfLocalHorizon();
			loc = projection.toEquatorialPosition(loc, false);
			loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, true, false, 0);
			projection.enableCorrectionOfLocalHorizon();
			float pos[] = projection.projectPosition(loc, 0, false, 1);
			String s = null;
			if (pos != null) {
				int min = render.height; //(int) Math.min(render.height, rec.getMaxY());
				int max = 0; //(int) Math.max(0, rec.getMinY());
				double y = pos[1];
				int x = this.graphMarginX;
				if (render.drawLeyend == LEYEND_POSITION.LEFT) x += this.leyendMargin;
				if (render.drawCoordinateGridHighZoom) {
					s = fixLabel(Functions.formatDECOnlyMinutes(loc.getLatitude(), 0), false);
					//s = s.substring(s.indexOf(" ")).trim();
					if (s.startsWith("00")) s = s.substring(3).trim();
					if (s.endsWith("00'")) s = s.substring(0, s.length()-3).trim();
					if (s.equals("") || s.startsWith("00")) s = "00\u00b0";
				}
				drawLine2(x, (int) (y+0.5), x-l, (int) (y+0.5), dist, dist, g, thin, s);
				do {
					y += step;
					if (render.drawCoordinateGridHighZoom) {
						double lat = this.getSkyLocation(x, render.telescope
								.invertVertical ? (float) (render.height-1-y): (float) y).getLatitude();
						s = fixLabel(Functions.formatDECOnlyMinutes(lat, 0), false);
						//s = s.substring(s.indexOf(" ")).trim();
						if (s.startsWith("00")) s = s.substring(3).trim();
						if (s.endsWith("00'")) s = s.substring(0, s.length()-3).trim();
						if (s.equals("") || s.startsWith("00")) s = "00\u00b0";
					}
					if (y < min) drawLine2(x, (int) (y+0.5), x-l, (int) (y+0.5), dist, dist, g, thin, s);
				} while (y < min);

				y = pos[1];
				do {
					y -= step;
					if (render.drawCoordinateGridHighZoom) {
						double lat = this.getSkyLocation(x, render.telescope
								.invertVertical ? (float) (render.height-1-y): (float) y).getLatitude();
						s = fixLabel(Functions.formatDECOnlyMinutes(lat, 0), false);
						//s = s.substring(s.indexOf(" ")).trim();
						if (s.startsWith("00")) s = s.substring(3).trim();
						if (s.endsWith("00'")) s = s.substring(0, s.length()-3).trim();
						if (s.equals("") || s.startsWith("00")) s = "00\u00b0";
					}
					if (y > max) drawLine2(x, (int) (y+0.5), x-l, (int) (y+0.5), dist, dist, g, thin, s);
				} while (y > max);
			}
		}
		g.enableInversion(invertH, invertV);
    }

    private void drawLine1(float x0, float y0, float xf, float yf, float d0, float d1, Graphics g, boolean thin,
    		String s) {
    	if (render.telescope.invertHorizontal) {
    		if (render.drawLeyend != LEYEND_POSITION.RIGHT) {
        		if ((x0 >= render.width-1-rec.getMinX())) return;
    		} else {
	    		if (x0 < render.width-1-rec.getMaxX() || x0 >= render.width-graphMarginX) return;
    		}
    	} else {
    		if (x0 < rec.getMinX() || x0 >= rec.getMaxX()) return;
    	}

		if (fast && thin) {
			g.drawStraightLine(x0, y0, xf, yf);
		} else {
			g.drawLine(x0, y0, xf, yf, d0, d1);
		}

		if (x0 == xf && s != null) {
			float w = g.getStringWidth(s);
			if (render.useSuperScriptForRA) {
				s = DataSet.replaceAll(s, "m", "^{m}", true);
				s = DataSet.replaceAll(s, "h", "^{h}", true);
			}
	    	if (render.telescope.invertHorizontal) {
	    		if (render.drawLeyend != LEYEND_POSITION.RIGHT) {
	        		if ((x0+w/2 >= render.width-1-rec.getMinX())) return;
	    		} else {
		    		if (x0-w/2 < render.width-1-rec.getMaxX() || x0+w/2 >= render.width-graphMarginX) return;
	    		}
	    	} else {
	    		if (x0-w/2 < rec.getMinX() || x0+w/2 >= rec.getMaxX()) return;
	    	}

			int dp = (int) Math.abs(graphMarginX+(render.width-graphMarginX)/2f - x0);
			if (labelsAxesX.size() > 0 || dp > 150) {
				int fs = g.getFont().getSize();
				//int c = g.getColor();
				//g.setColor(render.background, false);
				//g.fillRect(x0-w/2f, (int) (rec.getMaxY()+fs*1.7)-fs, w+2, fs+2);
				//g.setColor(c, true);
				g.drawString(s, x0-w/2f, (int) (rec.getMaxY()+fs*1.7));

			}
		}
    }

    private void drawLine2(float x0, float y0, float xf, float yf, float d0, float d1, Graphics g, boolean thin,
    		String s) {
    	if (render.telescope.invertVertical) {
    		if (render.drawLeyend == LEYEND_POSITION.TOP) {
        		if (y0 >= render.height-1-rec.getMinY() || y0 < this.graphMarginY) return;
    		} else {
        		if (render.drawLeyend == LEYEND_POSITION.BOTTOM || render.drawLeyend == LEYEND_POSITION.NO_LEYEND) {
            		if (y0 >= render.height || y0 < render.height-rec.getMaxY())  return;
        		} else {
            		if (render.drawLeyend == LEYEND_POSITION.LEFT || render.drawLeyend == LEYEND_POSITION.RIGHT) {
                		if (y0 >= render.height || y0 < render.height-rec.getMaxY())  return;
            		}
        		}
    		}
    	} else {
    		if (y0 < rec.getMinY() || y0 > rec.getMaxY()) return;
    	}

		if (fast && thin) {
			g.drawStraightLine(x0, y0, xf, yf);
		} else {
			g.drawLine(x0, y0, xf, yf, d0, d1);
		}

		if (y0 == yf && s != null) {
			float w = g.getStringWidth(s);
			if (render.useSuperScriptForRA) {
				s = DataSet.replaceAll(s, "m", "^{m}", true);
				s = DataSet.replaceAll(s, "h", "^{h}", true);
			}
			if (render.telescope.invertVertical) {
	    		if (render.drawLeyend == LEYEND_POSITION.TOP) {
	        		if (y0+w/2 >= render.height-1-rec.getMinY() || y0-w/2 < this.graphMarginY) return;
	    		} else {
	        		if (render.drawLeyend == LEYEND_POSITION.BOTTOM || render.drawLeyend == LEYEND_POSITION.NO_LEYEND) {
	            		if (y0+w/2 >= render.height || y0-w/2 < render.height-rec.getMaxY())  return;
	        		} else {
	            		if (render.drawLeyend == LEYEND_POSITION.LEFT || render.drawLeyend == LEYEND_POSITION.RIGHT) {
	                		if (y0+w/2 >= render.height || y0-w/2 < render.height-rec.getMaxY())  return;
	            		}
	        		}
	    		}
	    	} else {
	    		if (y0-w/2 < rec.getMinY() || y0+w/2 > rec.getMaxY()) return;
	    	}

			int dp = (int) Math.abs(render.height/2f - y0);
			if (labelsAxesY.size() > 0 || dp > 100) {
				float dy = rec.getMinX()-render.drawCoordinateGridFont.getSize();
				int c = g.getColor();
				g.setColor(render.background, true);
				g.fillRect(dy-g.getFont().getSize()+4, y0-w/2f, g.getFont().getSize(), w+2);
				g.setColor(c, true);
				if (render.telescope.invertVertical) w = -w;
				g.drawRotatedString(s, dy, y0+w/2f, (float)Constant.PI_OVER_TWO);
			}
		}
    }

    private String labelsm=null, labeld=null, labelv=null, labelts=null,labeloc=null,labelcon=null,labellim=null,labelgc=null,labelm=null,labeln=null,labelg=null,labelpn=null,labelmet=null;
    private Rectangle boundssm,boundsd,boundsv,boundsts,boundsoc,boundcon,boundlim,boundsgc,boundsm,boundsn,boundsg,boundspn,boundmet;
    private int lastLeyend = -1;
    private double mmax = -1;
	private void drawLeyend() {
		float[] pos;
		String label;
		Rectangle bounds;
		float w, h;
        int oldGraphM = graphMarginX;
        float dist = getDist(this.foregroundDist);
        int minWidth = 699;

		int recy = g.getWidth()/2, recx = graphMarginX;
		if (render.drawLeyend == LEYEND_POSITION.RIGHT || render.drawLeyend == LEYEND_POSITION.LEFT) recx = graphMarginY;
        Graphics g = null;
        if (this.g.renderingToAndroid() || this.g.renderingToExternalGraphics() && (render.drawLeyend == LEYEND_POSITION.TOP || render.drawLeyend == LEYEND_POSITION.BOTTOM)) {
        	g = this.g;
        	recy = 0;
        	if (render.drawLeyend == LEYEND_POSITION.BOTTOM) recy = g.getHeight()-leyendMargin-1;
        	g.setColor(render.background, true);
        	g.fillRect(0, recy, g.getWidth(), leyendMargin);
        } else {
			if (render.drawLeyend == LEYEND_POSITION.RIGHT || render.drawLeyend == LEYEND_POSITION.LEFT) {
				g = this.g.getGraphics(this.g.getHeight(), this.g.getHeight());
				recy = this.g.getHeight()/2;
			} else {
				g = this.g.getGraphics(this.g.getWidth(), leyendMargin+1);
				recy = 0;
			}
			g.setClip(0, 0, g.getWidth(), g.getHeight());
			g.setColor(render.background, true);
			g.fillRect(0, 0, this.g.getWidth(), this.g.getWidth());
        }
		g.disableInversion();
		g.setColor(render.drawCoordinateGridColor, false);

		if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH && render.drawCoordinateGridStroke.getLineWidth() == JPARSECStroke.STROKE_DEFAULT_LINE_THIN.getLineWidth()) {
			g.setStroke(JPARSECStroke.STROKE_DEFAULT_LINE_THIN);
			if (render.drawLeyend == LEYEND_POSITION.BOTTOM)
				g.drawStraightLine(recx, recy+leyendMargin-1, render.width-1, recy+leyendMargin-1);
			g.drawStraightLine(recx, recy, g.getWidth()-1, recy);
			g.drawStraightLine(recx, recy, recx, recy+leyendMargin);
			g.drawStraightLine(g.getWidth()-1, recy, g.getWidth()-1, recy+leyendMargin);
			if (render.drawLeyend == LEYEND_POSITION.RIGHT || render.drawLeyend == LEYEND_POSITION.LEFT) {
				g.drawStraightLine(recx, recy+leyendMargin, g.getWidth()-1, recy+leyendMargin);
			}
		} else {
			g.setStroke(new JPARSECStroke(JPARSECStroke.STROKE_DEFAULT_LINE_THIN, render.drawCoordinateGridStroke.getLineWidth()));
			if (render.drawLeyend == LEYEND_POSITION.BOTTOM)
				g.drawLine(recx, recy+leyendMargin-1, render.width-1, recy+leyendMargin-1, dist, dist);
			g.drawLine(recx, recy, g.getWidth()-1, recy, dist, dist);
			g.drawLine(recx, recy, recx, recy+leyendMargin, dist, dist);
			g.drawLine(g.getWidth()-1, recy, g.getWidth()-1, recy+leyendMargin, dist, dist);
			if (render.drawLeyend == LEYEND_POSITION.RIGHT || render.drawLeyend == LEYEND_POSITION.LEFT) {
				g.drawLine(recx, recy+leyendMargin, g.getWidth()-1, recy+leyendMargin, dist, dist);
			}
		}

		int fs = render.drawCoordinateGridFont.getSize();
		if (render.width < 850 && fs >= 15) fs = 14;
		if (render.width < 1200 && fs > 18) fs = 18;
		if (render.width < 1600 && fs > 21) fs = 21;
		if (render.width < 2000 && fs > 24) fs = 24;
		g.setFont(FONT.getDerivedFont(render.drawCoordinateGridFont, fs));

		if (sizes == null) setSizes();
		int baseY = recy+1+leyendMargin/2;
		int baseY1 = baseY - (leyendMargin/10 - 1);
		int baseY2 = recy+leyendMargin + 5;
		if (baseY < 0) return;
		int add = (fs-15)/4;
		if (add >= 0) {
			baseY += add;
			if (g.renderingToAndroid()) add -= 2;
			baseY1 += add;
			baseY2 += add;
		}
		add = (15-fs);
		if (add > 0) {
			baseY --;
			baseY1 += add;
			baseY2 += add-1;
		}
		mmax = maglim;
		double m = -10.0 + 0.5*(int)((maglim+10.0)/0.5);

		int nobjItems = 0;
		boolean constel = false, limits = false;
		if (render.drawDeepSkyObjects) nobjItems += 4;
		if (render.drawMilkyWayContours) nobjItems ++;
		if (render.drawNebulaeContours) nobjItems ++;
		if (render.drawMeteorShowers) nobjItems ++;
		if (fieldDeg > 1 && render.drawConstellationContours != CONSTELLATION_CONTOUR.NONE) {
			nobjItems ++;
			constel = true;
		}
		if (render.drawConstellationLimits && ((fieldDeg <= 125 && fieldDeg >= 3) || !render.drawClever)) {
			nobjItems ++;
			limits = true;
		}
		if (render.getNumberOfExternalCatalogs() > 0) {
			for (int i=0; i<render.getNumberOfExternalCatalogs(); i++) {
				if (render.drawExternalCatalogs == null || render.drawExternalCatalogs[i]) nobjItems ++;
			}
		}

		float itemSize = 107.67f;
		int objSectionSize = (int)(((itemSize*nobjItems)*fs)/15);
		if (objSectionSize < 400) objSectionSize = 400;
		int limit0 = (160*fs)/15;
		if (render.width-objSectionSize < limit0) {
			if (render.width >= minWidth && constel) {
				nobjItems --;
				constel = false;
				objSectionSize = (int)(((itemSize*nobjItems)*fs)/15);
			}
			if (render.width >= minWidth && limits && render.width-objSectionSize < limit0) {
				nobjItems --;
				limits = false;
				objSectionSize = (int)(((itemSize*nobjItems)*fs)/15);
			}
		}

		int starSymbolsSectionSize = 0;
		int restAfterStars = objSectionSize;
		if ((render.drawStarsRealistic == REALISTIC_STARS.NONE || render.drawStarsRealistic == REALISTIC_STARS.NONE_CUTE) && render.drawStarsSymbols) {
			starSymbolsSectionSize = (156*fs)/15;
			restAfterStars += starSymbolsSectionSize;
		}
		double limit = ((minWidth + itemSize * (nobjItems - 6))*fs)/15;
		if (render.width < limit && render.width-objSectionSize >= limit0) {
			if (render.width >= minWidth && constel) {
				nobjItems --;
				constel = false;
				limit = ((minWidth + itemSize * (nobjItems - 6))*fs)/15;
			}
			if (render.width >= minWidth && limits && render.width < limit) {
				nobjItems --;
				limits = false;
				limit = ((minWidth + itemSize * (nobjItems - 6))*fs)/15;
			}
			if (render.width < limit) return;
		}

		double s = (g.getWidth()-graphMarginX-1-restAfterStars)/((int)m+3.0);

		if (hugeFactor >= 1) {
			s = (g.getWidth()/2-graphMarginX-1)/((int)m+3.0);
			fs = 22 + (hugeFactor-1)*3;
			if (fs > 35) fs = 35;
			g.setFont(FONT.getDerivedFont(g.getFont(), fs));
			baseY1 = recy+50;
			baseY2 = recy+leyendMargin;
		} else {
			int fontS = g.getFont().getSize();
			if (fontS < 15) {
				baseY1 = baseY1 - (15-fontS);
				baseY2 = baseY2 - (15-fontS);
			} else {
//				g.setFont(FONT.getDerivedFont(g.getFont(), 15));
			}
		}

		if (render.drawStarsRealistic != REALISTIC_STARS.NONE && render.drawStarsRealistic != REALISTIC_STARS.NONE_CUTE && hugeFactor < 1) s += 56.0 / (m+3.0);
		int index = 0;
		int offset = 0;
		if (g.renderingToAndroid()) {
			offset --;
			baseY1 --;
		}
		if (mmax>7 && !g.renderingToAndroid()) offset++;
		float step = 1, sPerStar = (float) (s / ((int)m+2f));
		if (starSymbolsSectionSize > 0 && sPerStar < (1.55*fs)/15 && hugeFactor < 1) {
			restAfterStars -= starSymbolsSectionSize;
			starSymbolsSectionSize = 0;
			s = (g.getWidth()-graphMarginX-1-restAfterStars)/((int)m+3.0);
			sPerStar = (float) (s / ((int)m+2f));
		}
		if (sPerStar > 14 && (hugeFactor > 1)) { // || maglim >= 9.51 || (render.drawStarsRealistic != REALISTIC_STARS.NONE && render.drawStarsRealistic != REALISTIC_STARS.NONE_CUTE))) {
			step = 0.5f;
			s = (g.getWidth()-graphMarginX-1-restAfterStars)/(m*2+4.0);
			if (hugeFactor >= 1) s = (g.getWidth()/2-graphMarginX-1)/(m*2+4.0);
		}

		for (float m0 = -1; m0<=m;m0=m0+step)
		{
			if (step == 0.5f && m0 != (int) m0 && m0 >= maglim) continue;

			index ++;
			float size = getSizeForAGivenMagnitude(m0+step*0.5);
			pos = new float[] {graphMarginX + (float) (index*s), baseY-1};
			int tsize = (int) (0.5 + (1.5f * size+adds));
			if (render.drawStarsRealistic != REALISTIC_STARS.NONE && render.drawStarsRealistic != REALISTIC_STARS.NONE_CUTE) {
				if (tsize <= 1) {
	    			if (render.getColorMode() == COLOR_MODE.NIGHT_MODE || render.getColorMode() == COLOR_MODE.BLACK_BACKGROUND)
	    				g.setColor(255, 255, 255, 156);
			    	if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH && !g.renderingToExternalGraphics()) {
			    		g.drawPoint((int)pos[0], (int)pos[1], g.getColor());
			    	} else {
			    		if (g.renderingToExternalGraphics()) {
			        		g.fillOval(pos[0]-1, pos[1]-1, 2, 2, false);
			    		} else {
			    			g.fillOval(pos[0], pos[1], 1, 1, dist);
			    		}
			    	}
					g.setColor(render.drawCoordinateGridColor, false);
				} else {
					this.drawStar(tsize, pos, dist, -1, g);
				}
			} else {
				tsize = (int) (2* size+adds);
				g.fillOval((pos[0] - size), (pos[1] - size), tsize, tsize, dist);
			}
			if (step == 1) {
				label = Integer.toString((int)m0);
			} else {
				label = Float.toString(m0);
			}
			bounds = g.getStringBounds(label);
			w = bounds.getWidth();
			h = bounds.getHeight();
			g.setColor(render.drawCoordinateGridColor, false);
			g.drawString(label, pos[0]-(w/2.0f), baseY2-(h/2.0f)+offset, dist);
		}
		if (g.renderingToAndroid()) baseY2 -= 3;
		Graphics.FONT f = g.getFont();
		if (labelsm == null) labelsm = Translate.translate(30);
		int little = 0;
		if (g.getStringWidth(labelsm) > s*(index+1)) {
			Graphics.FONT ff = FONT.getDerivedFont(f, f.getSize()*2/3);
			g.setFont(ff);
			little = 4;
		}
		boundssm = g.getStringBounds(labelsm);
		w = boundssm.getWidth();
		h = boundssm.getHeight();
		index ++;
		pos = new float[] {graphMarginX + (float) (index*s*0.5f), baseY1};
		g.drawString(labelsm, pos[0]-(w/2.0f), baseY1-(h/2.0f)-little, dist);
		g.setFont(f);

		if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH && render.drawCoordinateGridStroke.getLineWidth() == JPARSECStroke.STROKE_DEFAULT_LINE_THIN.getLineWidth()) {
			g.drawStraightLine(graphMarginX + (int) (index*s), recy, graphMarginX + (int) (index*s), recy+leyendMargin);
		} else {
			g.drawLine(graphMarginX + (int) (index*s), recy, graphMarginX + (int) (index*s), recy+leyendMargin, dist, dist);
		}

		graphMarginX = graphMarginX + (int) (index*s);
		s = starSymbolsSectionSize/3.0;
		if (hugeFactor > 0) s = (g.getWidth()/2) * (starSymbolsSectionSize / (double)(objSectionSize+starSymbolsSectionSize)) / 3.0;
		index = 0;

		float starMaxSize = Math.min(6, getSizeForAGivenMagnitude(1 + (render.drawStarsRealistic == REALISTIC_STARS.NONE_CUTE? 0:-1)));
		starMaxSize = (starMaxSize*fs)/15;
		if (hugeFactor > 2) starMaxSize = (starMaxSize * (hugeFactor-1));
		starMaxSize = Math.min(starMaxSize, leyendMargin/6);
		if (starSymbolsSectionSize > 0 && (render.drawStarsRealistic == REALISTIC_STARS.NONE || render.drawStarsRealistic == REALISTIC_STARS.NONE_CUTE) && render.drawStarsSymbols) {
			index ++;
			float size = (int) starMaxSize;
			pos = new float[] {graphMarginX + (int) ((index-0.15)*s), baseY};
			g.fillOval(pos[0] - size, pos[1] - size, 2 * size+1, 2 * size+1, dist);
			if (!render.drawStarsPositionAngleInDoubles)
				g.drawStraightLine(pos[0] - size, pos[1], pos[0] - size - size - 1, pos[1], dist, dist);
			g.drawStraightLine(pos[0] + size, pos[1], pos[0] + size + size + 1, pos[1], dist, dist);

			if (labeld == null) {
				labeld = Translate.translate(32);
				boundsd = g.getStringBounds(labeld);
			}
			w = boundsd.getWidth();
			h = boundsd.getHeight();
			g.drawString(labeld, pos[0]-(w/2.0f), baseY2-(h/2.0f), dist);
			index ++;

			pos = new float[] {graphMarginX + (int) ((index+0.15)*s), baseY};
			g.fillOval(pos[0] - size, pos[1] - size, 2 * size, 2 * size, dist);
			size = size + 3;
			if (hugeFactor > 1) size = size - 3 + 3 * hugeFactor;
			g.drawOval(pos[0] - size, pos[1] - size, 2 * size, 2 * size, dist);

			if (labelv == null) {
				labelv = Translate.translate(33);
				boundsv = g.getStringBounds(labelv);
			}
			w = boundsv.getWidth();
			h = boundsv.getHeight();
			g.drawString(labelv, pos[0]-(w/2.0f), baseY2-(h/2.0f), dist);

			if (labelts==null) {
				labelts = Translate.translate(31);
				boundsts = g.getStringBounds(labelts);
			}
			w = (float) (boundsts.getWidth() + s);
			h = boundsts.getHeight();
			g.drawString(labelts, pos[0]-(w/2.0f), baseY1-(h/2.0f), dist);

			index ++;
			if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH && render.drawCoordinateGridStroke.getLineWidth() == JPARSECStroke.STROKE_DEFAULT_LINE_THIN.getLineWidth()) {
				g.drawStraightLine(graphMarginX + (int) (index*s), recy, graphMarginX + (int) (index*s), recy+leyendMargin);
			} else {
				g.drawLine(graphMarginX + (int) (index*s), recy, graphMarginX + (int) (index*s), recy+leyendMargin, dist, dist);
			}

			graphMarginX = graphMarginX + (int) (index*s-15);
		}

		s = (g.getWidth() - graphMarginX + 15) / (nobjItems+1);

		index = 1;
		if (render.drawDeepSkyObjects) {
			g.setStroke(render.drawDeepSkyObjectsStroke);
			g.setColor(render.drawDeepSkyObjectsColor, true);
			boolean fast = this.fast;
			if (render.drawDeepSkyObjectsStroke.getLineWidth() != 1f) fast = false;

			pos = new float[] {graphMarginX + (int) (index*s), baseY};
			drawOpenCluster(pos, starMaxSize, dist, g);
			g.setColor(render.drawCoordinateGridColor, 255);
			if (labeloc == null) {
				labeloc = Translate.translate(34);
				boundsoc = g.getStringBounds(labeloc);
			}
			w = boundsoc.getWidth();
			h = boundsoc.getHeight();
			g.drawString(labeloc, pos[0]-(w/2.0f), baseY2-(h/2.0f), dist);
			index ++;

			pos = new float[] {graphMarginX + (float) (index*s), baseY};
			float poss[] = new float[] {(float) (graphMarginX + (index*s)), baseY};
			g.setColor(render.drawDeepSkyObjectsColor, true);
			drawGlobularCluster(poss, starMaxSize, dist, g, fast);
			g.setColor(render.drawCoordinateGridColor, 255);
			if (labelgc == null) {
				labelgc = Translate.translate(35);
				boundsgc = g.getStringBounds(labelgc);
			}
			w = boundsgc.getWidth();
			h = boundsgc.getHeight();
			g.drawString(labelgc, pos[0]-(w/2.0f), baseY2-(h/2.0f), dist);
			index ++;
		}

		if (render.drawNebulaeContours) {
			pos = new float[] {graphMarginX + (int) (index*s), baseY};

			g.setStroke(render.drawDeepSkyObjectsStroke);
			float nebOff = (float)s/8f;
			pos[0] -= nebOff;
			if (render.drawNebulaeStroke.isContinuousLine()) {
				int fill = render.background;
				if (render.fillNebulae) {
					fill = render.drawBrightNebulaeContoursColor;
					if (render.fillBrightNebulaeColor != -1) fill = render.fillBrightNebulaeColor;
				}
				drawGalaxy(pos, starMaxSize, dist, false, g, render.drawBrightNebulaeContoursColor, fill);
				pos[0] += 2*nebOff;
				fill = render.background;
				if (render.fillNebulae) fill = render.drawDarkNebulaeContoursColor;
				drawGalaxy(pos, starMaxSize, dist, false, g, render.drawDarkNebulaeContoursColor, fill);
				pos[0] -= nebOff;
			} else {
				Object path = g.generalPathInitialize();
				step = (float) (Constant.TWO_PI/20.0);
				if (hugeFactor > 1) step /= hugeFactor;
				for (double ang=0; ang<Constant.TWO_PI; ang=ang+step) {
					float px = pos[0] + (float) (starMaxSize * FastMath.cos(ang));
					float py = pos[1] + (float) (starMaxSize * FastMath.sin(ang));
					if (ang == 0.0) {
						g.generalPathMoveTo(path, px, py);
					} else {
						g.generalPathLineTo(path, px, py);
					}
				}
				g.generalPathClosePath(path);
				if (render.fillNebulae) {
					g.setColor(render.drawBrightNebulaeContoursColor, true);
					if (render.fillBrightNebulaeColor != -1) g.setColor(render.fillBrightNebulaeColor, true);
					g.fill(path, dist);
				}
				if (render.fillBrightNebulaeColor != -1 || !render.fillNebulae) {
					g.setColor(render.drawBrightNebulaeContoursColor, true);
					g.draw(path, dist);
				}

				pos[0] += nebOff*2;
				path = g.generalPathInitialize();
				for (double ang=0; ang<Constant.TWO_PI; ang=ang+step) {
					float px = pos[0] + (float) (starMaxSize * FastMath.cos(ang));
					float py = pos[1] + (float) (starMaxSize * FastMath.sin(ang));
					if (ang == 0.0) {
						g.generalPathMoveTo(path, px, py);
					} else {
						g.generalPathLineTo(path, px, py);
					}
				}
				g.generalPathClosePath(path);
				g.setColor(render.drawDarkNebulaeContoursColor, true);
				g.setStroke(render.drawNebulaeStroke);
				if (render.fillNebulae) {
					g.fill(path, dist);
				} else {
					g.draw(path, dist);
				}

				pos[0] -= nebOff;
				g.setStroke(render.drawDeepSkyObjectsStroke);
				g.setStroke(JPARSECStroke.STROKE_DEFAULT_LINE_THIN);
			}

			g.setColor(render.drawCoordinateGridColor, 255);
			if (labeln==null) {
				labeln = Translate.translate(36);
				boundsn = g.getStringBounds(labeln);
			}
			w = boundsn.getWidth();
			h = boundsn.getHeight();
			g.drawString(labeln, pos[0]-(w/2.0f), baseY2-(h/2.0f), dist);
			index ++;
		}

		if (render.drawMilkyWayContours) {
			pos = new float[] {graphMarginX + (float) (index*s), baseY};
			if (render.getColorMode() == COLOR_MODE.BLACK_BACKGROUND && render.drawMilkyWayContoursWithTextures != MILKY_WAY_TEXTURE.NO_TEXTURE) {
//				if (milkyWayTexture == null) {
				Object	milkyWayTexture = g.getImage(FileIO.DATA_TEXTURES_DIRECTORY + "deepsky/"+SkyRenderElement.MILKY_WAY_TEXTURE_FILENAME[render.drawMilkyWayContoursWithTextures.ordinal()]);
//				}
				int imax = g.getSize(milkyWayTexture)[0];
				int init = (int) (0.5 + imax * 1680.0 / 3000.0), jnit = (int) (imax * 750.0 / 3000.0);
				Object img = g.getImage(milkyWayTexture, init, jnit, (int)(starMaxSize*4+1), (int)(starMaxSize*4+1));
				g.drawImage(img, pos[0]-starMaxSize, pos[1]-starMaxSize, 0.5, 0.5);
			} else {
				g.setStroke(render.drawDeepSkyObjectsStroke);
				if (render.drawMilkyWayStroke.isContinuousLine()) {
					g.setColor(render.fillMilkyWayColor, true);
					int fillc = g.getColor();
					if (!render.fillMilkyWay) fillc = render.background;
					drawGalaxy(pos, starMaxSize, dist, false, g, render.drawMilkyWayContoursColor, fillc);
				} else {
					g.setColor(render.drawMilkyWayContoursColor, true);
					Object path = g.generalPathInitialize();
					pos = new float[] {graphMarginX + (float) (index*s), baseY};
					step = (float) (Constant.TWO_PI/20.0);
					for (double ang=0; ang<Constant.TWO_PI; ang=ang+step) {
						float px = pos[0] + (float) (starMaxSize * FastMath.cos(ang));
						float py = pos[1] + (float) (starMaxSize * FastMath.sin(ang));
						if (ang == 0.0) {
							g.generalPathMoveTo(path, px, py);
						} else {
							g.generalPathLineTo(path, px, py);
						}
					}
					g.generalPathClosePath(path);
					g.setStroke(render.drawMilkyWayStroke);
					if (render.fillMilkyWay) { // && render.drawCoordinateGrid) {
						g.setColor(render.fillMilkyWayColor, true);
						g.fill(path, dist);
						g.setColor(render.drawMilkyWayContoursColor, true);
						g.draw(path, dist);
					} else {
						g.draw(path, dist);
					}
					g.setStroke(JPARSECStroke.STROKE_DEFAULT_LINE_THIN);
				}
			}

			g.setColor(render.drawCoordinateGridColor, 255);
			if (labelm==null) {
				labelm = Translate.translate(37);
				boundsm = g.getStringBounds(labelm);
			}
			w = boundsm.getWidth();
			h = boundsm.getHeight();
			g.drawString(labelm, pos[0]-(w/2.0f), baseY2-(h/2.0f), dist);
			index ++;
		}

			pos = new float[] {graphMarginX + (float) ((0.5+nobjItems/2f)*s), baseY};
			g.setColor(render.drawCoordinateGridColor, 255);
			String labeldso = "";
			if (!constel && !limits) {
				labeldso = t38;
			} else {
				if (nobjItems > 6) {
					labeldso = t1073;
					if (g.getStringWidth(labeldso) > nobjItems*s) labeldso = t1072;
				} else {
					labeldso = t1072;
				}
			}
			Rectangle boundsdso = g.getStringBounds(labeldso);
			w = (boundsdso.getWidth());
			h = boundsdso.getHeight();
			g.drawString(labeldso, pos[0]-(w/2.0f), baseY1-(h/2.0f), dist);

		if (render.drawDeepSkyObjects) {
			g.setStroke(render.drawDeepSkyObjectsStroke);
			boolean fast = this.fast;
			if (render.drawDeepSkyObjectsStroke.getLineWidth() != 1f) fast = false;
			pos = new float[] {graphMarginX + (float) (index*s), baseY};
			g.setColor(render.drawDeepSkyObjectsColor, true);
			drawpneb(pos, starMaxSize*2/3, dist, g, fast);
			pos = new float[] {graphMarginX + (float) (index*s), baseY};
			g.setColor(render.drawCoordinateGridColor, 255);
			if (labelpn==null) {
				labelpn = Translate.translate(39);
				boundspn = g.getStringBounds(labelpn);
			}
			w = boundspn.getWidth();
			h = boundspn.getHeight();
			g.drawString(labelpn, pos[0]-(w/2.0f), baseY2-(h/2.0f), dist);
			index ++;

			pos = new float[] {graphMarginX + (float) (index*s), baseY};
			float[] poss = new float[] {(float) (graphMarginX + (index*s)), baseY};
			drawGalaxy(poss, starMaxSize, dist, false, g, render.drawDeepSkyObjectsColor, render.fillGalaxyColor);
			g.setColor(render.drawCoordinateGridColor, 255);
			if (labelg==null) {
				labelg = Translate.translate(40);
				boundsg = g.getStringBounds(labelg);
			}
			w = boundsg.getWidth();
			h = boundsg.getHeight();
			g.drawString(labelg, pos[0]-(w/2.0f), baseY2-(h/2.0f), dist);
			index ++;
		}

		if (constel) {
			pos = new float[] {graphMarginX + (int) (index*s), baseY};
			g.setColor(render.drawConstellationContoursColor, true);
			g.setStroke(render.drawConstellationStroke);
			g.drawLine(pos[0]-(w/4.0f), pos[1], pos[0]+(w/4.0f), pos[1], true);
			g.setColor(render.drawCoordinateGridColor, 255);
			if (labelcon == null) {
				labelcon = Translate.translate(321);
				boundcon = g.getStringBounds(labelcon);
			}
			w = boundcon.getWidth();
			h = boundcon.getHeight();
			g.drawString(labelcon, pos[0]-(w/2.0f), baseY2-(h/2.0f), dist);
			index ++;
		}

		if (limits) {
			pos = new float[] {graphMarginX + (int) (index*s), baseY};
			g.setColor(render.drawConstellationLimitsColor, true);
			g.setStroke(render.drawConstellationLimitsStroke);
			g.drawLine(pos[0]-(w/4.0f), pos[1], pos[0]+(w/4.0f), pos[1], false);
			g.setColor(render.drawCoordinateGridColor, 255);
			if (labellim == null) {
				labellim = Translate.translate(1071);
				boundlim = g.getStringBounds(labellim);
			}
			w = boundlim.getWidth();
			h = boundlim.getHeight();
			g.drawString(labellim, pos[0]-(w/2.0f), baseY2-(h/2.0f), dist);
			index ++;
		}

		if (render.drawMeteorShowers) {
			pos = new float[] {graphMarginX + (int) (index*s), baseY};
			g.setColor(render.drawMeteorShowersColor, true);
			drawMS(g, pos, true, 30);
			g.setColor(render.drawCoordinateGridColor, 255);
			if (labelmet == null) {
				labelmet = Translate.translate(1276);
				boundmet = g.getStringBounds(labelmet);
			}
			w = boundmet.getWidth();
			h = boundmet.getHeight();
			g.drawString(labelmet, pos[0]-(w/2.0f), baseY2-(h/2.0f), dist);
			index ++;
		}
		g.setStroke(JPARSECStroke.STROKE_DEFAULT_LINE_THIN);

		if (render.getNumberOfExternalCatalogs() > 0) {
			index --;
			int rs = render.getNumberOfExternalCatalogs();
			for (int e = 0; e<rs; e++) {
				if (render.drawExternalCatalogs == null || render.drawExternalCatalogs[e]) {
					String id = "RenderSkyExternalCatalog"+e;
					if (render.externalCatalogAvailable(id)) {
						Object o = DataBase.getDataForAnyThread(id, true);
						if (o == null) o = DataBase.getDataForAnyThread(id, false);
						if (o != null) {
							ArrayList<Object> externalObj = new ArrayList<Object>(Arrays.asList(((Object[]) o)));
								Object data[] = (Object[]) externalObj.get(0);

								index ++;
								pos = new float[] {graphMarginX + (float) (index*s), baseY};
								float[] poss = new float[] {(float) (graphMarginX + (index*s)), baseY-1};
								g.setColor((Integer)data[8], true);
								if (render.getColorMode() == COLOR_MODE.NIGHT_MODE) g.setColor(render.drawDeepSkyObjectsColor, true);
								drawGalaxy(poss, starMaxSize, dist, false, g, render.drawDeepSkyObjectsColor, g.getColor());
								label = (String) data[1];
								Rectangle boundsg = g.getStringBounds(label);
								g.setColor(render.drawCoordinateGridColor, 255);
								w = boundsg.getWidth();
								h = boundsg.getHeight();
								g.drawString(label, pos[0]-(w/2.0f), baseY2-(h/2.0f), dist);
						}
					}
				}
			}
		}

		graphMarginX = oldGraphM;
		lastLeyend = 1;
		if (!this.g.renderingToAndroid()) {
			if (!this.g.renderingToExternalGraphics() || render.drawLeyend == LEYEND_POSITION.LEFT || render.drawLeyend == LEYEND_POSITION.RIGHT) {
				try {
					Object imgLeyend = null;
					try { imgLeyend = g.getImage(0, 0, g.getWidth(), g.getHeight()); } catch (Exception exc) {}
					this.g.setClip(0, 0, this.g.getWidth(), this.g.getHeight());
					if (render.drawLeyend == LEYEND_POSITION.RIGHT || render.drawLeyend == LEYEND_POSITION.LEFT) {
						float ang = -(float)Constant.PI_OVER_TWO;
						if (render.drawLeyend == LEYEND_POSITION.RIGHT) ang = -ang;
						imgLeyend = g.getRotatedAndScaledImage(imgLeyend, g.getWidth()/2, g.getWidth()/2, ang, 1.0f, 1.0f);
						int px = -g.getWidth()/2, py = 0;
						if (render.drawLeyend == LEYEND_POSITION.RIGHT) {
							py = -graphMarginY;
							px += leyendMargin;
						}
						int size[] = g.getSize(imgLeyend);
						imgLeyend = g.getImage(imgLeyend, -px, -py, leyendMargin+1, size[1]+py);
						px = 0;
						py = 0;
						if (render.drawLeyend == LEYEND_POSITION.RIGHT) px += render.width-leyendMargin;
						this.g.drawImage(imgLeyend, px, py);
					} else {
						int py = 0;
						imgLeyend = g.getImage(imgLeyend, 0, 0, g.getWidth(), leyendMargin+1);
						if (render.drawLeyend == LEYEND_POSITION.BOTTOM) py = render.height-leyendMargin;
						this.g.drawImage(imgLeyend, 0, py);
					}
				} catch (Exception exc) {}
			}
		}

		if (g == this.g) this.g.enableInversion();
	}
	private void drawString(int col, Graphics.FONT font, String label, float posx, float posy, float radius, boolean check)
	{
		if (this.rec == null || !this.isInTheScreen((int)posx, (int)posy)) return;
		//if (radius == 0) radius = -15;
		int c = DataSet.getIndex(labelsName, label);
		if (!check || c < 0) {
			if (c < 0) labelsName = DataSet.addStringArray(labelsName, label);
			if (render.telescope.invertVertical) posy -= font.getSize()/2;
			Object obj[] = new Object[] {label, posx, posy, radius, font, col};
			labels.add(obj);
		}
	}
	private void drawStringAxes(int col, Graphics.FONT font, String label, float posx, float posy, float radius, int axis)
	{
		//if (posy > rec.getMaxY() && axis != 0) return;
		Object obj[] = new Object[] {label, posx, posy, radius, font, col};
		if (axis == 0) {
				labelsAxesX.add(obj);
				labelsAxesNameX = DataSet.addStringArray(labelsAxesNameX, label);
		} else {
				labelsAxesY.add(obj);
				labelsAxesNameY = DataSet.addStringArray(labelsAxesNameY, label);
		}
	}

	private boolean isInTheScreen(int rx, int ry) {
		if (render.telescope.invertHorizontal) rx = render.width-1-rx;
		if (rx<=rec.getMinX() || rx >= rec.getMaxX()) return false;
		if (render.telescope.invertVertical) ry = render.height-1-ry;
		if (ry<=rec.getMinY() || ry >= rec.getMaxY()) return false;
		return true;
	}
	private boolean isInTheScreen(int rx, int ry, int rz) {
		if (rz == 0) return isInTheScreen(rx, ry);
		if (render.telescope.invertHorizontal) rx = render.width-1-rx;
		if (rx<=rec.getMinX()-rz || rx >= rec.getMaxX()+rz) return false;
		if (render.telescope.invertVertical) ry = render.height-1-ry;
		if (ry<=rec.getMinY()-rz || ry >= rec.getMaxY()+rz) return false;
		return true;
	}
	private boolean isInTheScreen(float rx, float ry, float minX, float minY, float maxX, float maxY) {
		if (render.telescope.invertHorizontal) rx = render.width-1-rx;
		if (rx<=minX || rx >= maxX) return false;
		if (render.telescope.invertVertical) ry = render.height-1-ry;
		if (ry<=minY || ry >= maxY) return false;
		return true;
	}
	private boolean isInTheScreen(float rx, float ry, int rz, float minX, float minY, float maxX, float maxY) {
		if (rz == 0) return isInTheScreen(rx, ry, minX, minY, maxX, maxY);
		if (render.telescope.invertHorizontal) rx = render.width-1-rx;
		if (rx<=minX-rz || rx >= maxX+rz) return false;
		if (render.telescope.invertVertical) ry = render.height-1-ry;
		if (ry<=minY-rz || ry >= maxY+rz) return false;
		return true;
	}


	private void drawLabels()
	{
		if (labels.size() == 0) return;

		if (render.drawFastLabels == SUPERIMPOSED_LABELS.FAST || pixels_per_degree < 5 && render.drawClever && render.drawFastLabelsInWideFields) {
			drawFastLabels(labels);
			return;
		}

		int red,green,blue;
		String l = "",label;
		Rectangle bounds;
		float w, h, bestPX, bestPY, bestScore,px,py,px0,py0,ang;
		float oversampling = 0.15f;
		double overs = 1.0; //+oversampling*2.0;
		float dangle = (float) (Constant.PI_OVER_TWO);
		if (maglim == render.drawStarsLimitingMagnitude || !render.drawFastLabelsInWideFields)
			dangle /= 3.0f;
		if (render.drawFastLabels == SUPERIMPOSED_LABELS.AVOID_SUPERIMPOSING_VERY_ACCURATE)
			dangle /= 3.0f;
		int np,i,imax=labels.size();
		float posx,posy,radius;
		Object obj[];
 		int step = 1;
 		if (imax > 50 && render.drawFastLabels != SUPERIMPOSED_LABELS.AVOID_SUPERIMPOSING_VERY_ACCURATE && render.drawFastLabels != SUPERIMPOSED_LABELS.AVOID_SUPERIMPOSING_ACCURATE) step = 2;
 		if (g.renderingToExternalGraphics() && render.drawFastLabels == SUPERIMPOSED_LABELS.AVOID_SUPERIMPOSING_VERY_ACCURATE) {
 			dangle /= 16;
 			overs = 1.2;
 			oversampling = 0.0f;
 		}
		int r1 = g.getRed(render.background), g1 = g.getGreen(render.background), b1 = g.getBlue(render.background);
		boolean useBackground = false;
		int off = 1;
		if (render.telescope.invertVertical) off = -1;
		for (i=0; i<imax; i++)
		{
			obj = labels.get(i);
			if (obj != null) {
				label = (String) obj[0];
				posx = (Float) obj[1];
				posy = (Float) obj[2];
				radius = (Float) obj[3];
				g.setFont((Graphics.FONT) obj[4]);
				g.setColor(((Integer) obj[5]), true);

				l = label;
				bounds = g.getStringBounds(l);
				w = bounds.getWidth();
				h = bounds.getHeight();
				bestPX = -1;
				bestPY = -1;
				bestScore = Float.MAX_VALUE;
				np = (int) (1+w) * (int) (1+h);
				float r = Math.abs(radius);
				r -= h/3.5;
				for (ang=0.0f;ang<Constant.TWO_PI;ang=ang+dangle)
				{
					float cosa = FastMath.cosf(ang), sina = FastMath.sinf(ang);
					px = posx + ((r + w * (0.5f+oversampling)) * cosa);
					py = posy + ((r + h * (0.5f+oversampling)) * sina);
					if (this.isInTheScreen((int)px, (int)py))  //!projection.isOutside(new float[] {px, py}, graphMarginX, graphMarginY, (int)rec.getMinY()))
					{
						int scoreR1 = 0, scoreG1 = 0, scoreB1 = 0;
						boolean out = false;
						int xmax = (int) (w*overs), ymax = (int) (h*overs);
						int coln = 0;
						float rx0 = px - w* (0.5f);
						float ry0 = py - h* (0.5f);
						int col0[] = null;
						int i0 = (int) (rx0+0.5f);
						int j0 = (int) (ry0+0.5f);
						if (!useBackground) {
							// Obtain mean background
							r1 = 0;
							g1 = 0;
							b1 = 0;
							if (i0 < 0) {
								xmax += i0;
								i0 = 0;
							}
							if (j0 < 0) {
								ymax += j0;
								j0 = 0;
							}
							if (i0+xmax > g.getWidth()) xmax = g.getWidth()-i0;
							if (j0+ymax > g.getHeight()) ymax = g.getHeight()-j0;
							if (isInTheScreen(i0 + xmax - 1, j0 + ymax - 1) && isInTheScreen(i0, j0 + ymax - 1) &&
									isInTheScreen(i0 + xmax - 1, j0) && isInTheScreen(i0, j0)) {
								col0 = g.getRGBs(i0, j0, xmax, ymax);
								out = true;
								if (col0 != null) {
									out = false;
									for (int x=0;x<xmax; x=x+step)
									{
										for (int y=0;y<ymax; y=y+step)
										{
											if (isInTheScreen(i0 + x, j0 + y)) {
												int col = x + y * xmax;
												if (col < 0 || col >= col0.length) {
													out = true;
													break;
												} else {
													col = col0[col]; //g.getRGB((int) rx, (int) ry);
													red = 0xff & (col >> 16);
													green = 0xff & (col >> 8);
													blue = 0xff & col;
													r1 += red;
													g1 += green;
													b1 += blue;
													coln++;
												}
											} else {
												out = true;
												break;
											}
										}
										if (out) break;
									}
								}
							} else {
								out = true;
							}
						}
						if (!out) {
							if (!useBackground) {
								if (coln == 0) continue;
								r1 = r1 / coln;
								g1 = g1 / coln;
								b1 = b1 / coln;
							}
							if (col0 == null) {
								if (i0 < 0) {
									xmax += i0;
									i0 = 0;
								}
								if (j0 < 0) {
									ymax += j0;
									j0 = 0;
								}
								if (i0+xmax > g.getWidth()) xmax = g.getWidth()-i0;
								if (j0+ymax > g.getHeight()) ymax = g.getHeight()-j0;
								col0 = g.getRGBs(i0, j0, xmax, ymax);
							}
							out = true;
							if (col0 != null) {
								out = false;
								for (int x=0;x<xmax; x=x+step)
								{
									for (int y=0;y<ymax; y=y+step)
									{
										if (isInTheScreen(i0 + x, j0 + y)) {
											int col = col0[x + y * xmax]; //g.getRGB((int) rx, (int) ry);
											red = 0xff & (col >> 16);
											green = 0xff & (col >> 8);
											blue = 0xff & col;
											scoreR1 += Math.abs(red-r1);
											scoreG1 += Math.abs(green-g1);
											scoreB1 += Math.abs(blue-b1);
										} else {
											out = true;
											break;
										}
									}
									if (out) break;
								}
							}
							if (out) continue;
							float score = (float)(scoreR1 + scoreG1 + scoreB1) / np; // Averaged by pixel
							if (score < bestScore) {
								bestScore = score;
								px0 = posx + ((r + w * 0.5f) * cosa);
							    py0 = posy + ((r + h * 0.5f) * sina);
								bestPX = px0;
								bestPY = py0;
							}
						}
					}
				}
				// FIXME: commented part sometimes useful to avoid certain labels, but only sometimes
				if (bestPX != -1 && bestPY != -1 && (bestScore < 10 || (radius < 0  ))) { // && (projection.obs.getMotherBody() == TARGET.EARTH || projection.obs.getMotherBody() == TARGET.NOT_A_PLANET)))) {
					g.drawString(label, bestPX - (w/2.0f), bestPY + off*(h/3.0f));
				}
			}
		}
	}

	private void drawFastLabels(ArrayList<Object[]> labels)
	{
		if (labels.size() == 0) return;
		String label;
		int i,imax=labels.size();
		float posx,posy,radius;
		Object obj[];
		for (i=0; i<imax; i++)
		{
			obj = labels.get(i);
			if (obj != null) {
				label = (String) obj[0];
				posx = (Float) obj[1];
				posy = (Float) obj[2];
				g.setFont((Graphics.FONT) obj[4]);
				g.setColor(((Integer) obj[5]), true);
				radius = Math.abs((Float) obj[3]);

				g.drawString(label, posx - g.getStringWidth(label)/2, posy + radius);
			}
		}
	}

	private void drawLabelsCoord()
	{
		g.disableInversion();
		g.setClip(0, 0, render.width, render.height);
		boolean invertH = render.telescope.invertHorizontal;
		boolean invertV = render.telescope.invertVertical;
		g.enableInversion(invertH, false);
		this.drawLabelsCoord(this.labelsAxesX, true);
		g.enableInversion(false, invertV);
		this.drawLabelsCoord(this.labelsAxesY, false);
		g.enableInversion(invertH, invertV);
	}

	private void drawLabelsCoord(ArrayList<Object[]> labels, boolean check)
	{
		if (labels.size() == 0) return;
		String label;
		int i,imax=labels.size();
		float posx,posy;
		Object obj[];
		Rectangle bounds;
		float w, h;
		int red,green,blue;
		int r1 = g.getRed(render.background), g1 = g.getGreen(render.background), b1 = g.getBlue(render.background);
		float minx = rec.getMinX()+0.5f;
		for (i=0; i<imax; i++)
		{
			obj = labels.get(i);
			label = (String) obj[0];
			if (label == null) continue;

			posx = (Float) obj[1];
			posy = (Float) obj[2];
			Graphics.FONT font = (Graphics.FONT) obj[4];
			int color = (Integer) obj[5];

			if (check) {
				bounds = g.getStringBounds(label);
				w = bounds.getWidth();
				posx -= w/2;
//				if (posx+w/2 > rec.getMaxX() || posx < rec.getMinX()) continue;
				h = bounds.getHeight();
				int scoreR1 = 0, scoreG1 = 0, scoreB1 = 0, np = 1;
				float rx0 = posx - w* (0.5f);
				float ry0 = posy - h* (0.5f);
				for (int y=0;y<h; y=y+2)
				{
					for (int x=0;x<w; x=x+2)
					{
						float rx = rx0 + x;
						float ry = ry0 + y;
						if (rec.contains(rx, ry)) { //rx>=rec.getMinX() && ry >= rec.getMinY() && rx < rec.getMaxX() && ry<rec.getMaxY()) {
							int col = g.getRGB((int) rx, (int) ry);
							red = 0xff & (col >> 16);
							green = 0xff & (col >> 8);
							blue = 0xff & col;
							scoreR1 += Math.abs(red-r1);
							scoreG1 += Math.abs(green-g1);
							scoreB1 += Math.abs(blue-b1);
							np ++;
						}
					}
				}
				if (np>1) np --;
				if ((scoreR1+scoreG1+scoreB1)/np < 5) {
					posx = Math.max(posx, minx);
					g.setFont(font);
					g.setColor(color, false);
					g.drawString(label, posx, posy);
				}
			} else {
				g.setFont(font);
				g.setColor(color, false);
				int dy = (int) (g.getStringWidth(label)/2);
				if (render.telescope.invertVertical) dy = -dy;
				g.drawRotatedString(label, posx, posy + dy, (float) Constant.PI_OVER_TWO); //.drawString(label, posx, posy);
			}
		}
	}

	/**
	 * Returns x center.
	 * @return X center in pixels.
	 */
	public int getXCenter()
	{
		if (rec == null) {
			int xc = (int) (graphMarginX+(render.width-graphMarginX)/2f);
			if (render.telescope.invertHorizontal) xc = render.width-1-xc;
			return xc;
			//return render.width/2;
		}

		int xc = (int)(rec.getMinX() + (rec.getMaxX()-rec.getMinX())/2);
		if (render.telescope.invertHorizontal) xc = render.width-1-xc;
		return xc;
	}
	/**
	 * Returns y center.
	 * @return Y center in pixels.
	 */
	public int getYCenter()
	{
		//if (rec == null)
		if (render.drawLeyend == LEYEND_POSITION.NO_LEYEND ||
				render.drawLeyend == LEYEND_POSITION.BOTTOM) return (int)(rec.getMaxY()/2)+yoff;
			return render.height/2+yoff;
		//int yc = (int)(yoff+rec.getMinY() + (rec.getMaxY()-rec.getMinY())/2);
		//if (render.telescope.invertVertical) yc = render.height-1-yc;
		//return yc;
	}
	private int yoff = 0;
	/**
	 * Sets an y offset.
	 * @param dy Y offset.
	 * @throws JPARSECException If an error occurs.
	 */
	public void setYCenterOffset(int dy) throws JPARSECException {
		if (dy != yoff) {
			yoff = dy;
			projection.updateProjection(render, field, this.getXCenter(), this.getYCenter());
		}
	}

	/**
	 * Sets the margin in the y axis.
	 * @param y Value in pixels.
	 */
	public void setYMargin(int y)
	{
		this.graphMarginY = y;
	}
	/**
	 * Returns the graph margin in y axis.
	 * @return Y margin.
	 */
	public int getYMargin() {
		return this.graphMarginY;
	}

	/**
	 * Forces the update of planetary positions.
	 */
	public void forceUpdatePlanets() {
		render.planetRender.moonephem = null;
		this.planets = null;
		majorObjects = null;
		majorObjectsSats = null;
	}

	/**
	 * To be called when the color squeme changes.
	 */
	public void colorSquemeChanged() {
		imgs = new String[0];
		images = new ArrayList<Object>();
		lastLeyend = -1;
		starImg = null;
//		ovals = null;
		firstTime = true;
		sizes = null;
		fieldDegBefore = -1;
		if (g == null) return;
		if (render.trajectory != null) {
			int c = g.getColor();
			for (int i=0; i<render.trajectory.length; i++) {
				g.setColor(g.invertColor(render.background), g.getAlpha(render.trajectory[i].drawPathColor1));
				if (render.getColorMode() == COLOR_MODE.NIGHT_MODE) g.setColor(g.getRed(g.getColor()), 0, 0, g.getAlpha(g.getColor()));
				render.trajectory[i].drawPathColor1 = g.getColor();
			}
			g.setColor(c, true);
		} 
		/*
		if (!g.renderingToAndroid() && render.width >= 3000) {
        	int hugeFactor = 0; //Math.max((render.width/1000) - 2, 4);
        	render.drawCoordinateGridFont = FONT.getDerivedFont(render.drawCoordinateGridFont, 14);
        	int s = 14 + hugeFactor * 2; 
        	if (s > 35) s = 35;
        	render.drawCoordinateGridFont = FONT.getDerivedFont(render.drawCoordinateGridFont, s);        	
		}
		 */
	}

	/**
	 * Forces the complete update of the frame for the next rendering
	 * in a light or fast way.
	 */
	public void forceUpdateFast() {
		firstTime = true;
		lastLeyend = -1;
	}

	/**
	 * To be called when the trajectory changes.
	 */
	public void trajectoryChanged() {
		firstTime = true;
		trajectoryCalculated = false;
		if (render.trajectory != null) {
			int c = g.getColor();
			for (int i=0; i<render.trajectory.length; i++) {
				g.setColor(g.invertColor(render.background), g.getAlpha(render.trajectory[i].drawPathColor1));
				if (render.getColorMode() == COLOR_MODE.NIGHT_MODE) g.setColor(g.getRed(g.getColor()), 0, 0, g.getAlpha(g.getColor()));
				render.trajectory[i].drawPathColor1 = g.getColor();
			}
			g.setColor(c, true);
		}
	}

	/**
	 * Forces the update of all strings specific the sky rendering.
	 */
	public void languageChanged() {
	    labelsm=labeld=labelv=labelts=labeloc=labelcon=labellim=labelgc=labelm=labeln=labelg=labelpn=labelmet=null;
	    labelRA=labelDEC=lati0=ecl0=null;
	    setDefaultStrings();
		DataBase.addData("connom", threadID, null, true);
		db_connom = -1;
		DataBase.addData("starNames", threadID, null, true);
		db_starNames = -1;
	}

	/**
	 * Forces the leyend to be rendered again. Leyend is by
	 * default only drawn when something affecting it changes,
	 * like the limiting magnitude for stars when the sky
	 * is zoomed in/out, or the properties of nebula.
	 * @param alsoStars True to render again stars according
	 * to their sizes. False unless limiting magnitude or zoom
	 * changes.
	 */
	public void resetLeyend(boolean alsoStars) {
		lastLeyend = -1;
		if (alsoStars) {
			starImg = null;
			forceUpdateFast();
		}
	}

	/**
	 * Resets the original telescope to reflect any change in the
	 * telescope parameters.
	 * @throws JPARSECException If an error occurs.
	 */
	public void resetOriginalTelescope() throws JPARSECException {
		if (render.telescope.ccd != null) {
			TelescopeElement tel = render.telescope.clone();
			tel.ocular = null;
			field0 = (float) render.telescope.ccd.getFieldX(tel);
		} else {
			field0 = (float) render.telescope.getField();
		}
	}

	/**
	 * Clears ephemeris of minor bodies. This is
	 * necessary when the date changes moderately
	 * to calculate new ephemeris for these objects
	 * (comets, asteroids, transneptunian objects,
	 * probes, artificial satellites) when the next
	 * rendering is launch.
	 * @param forceAll True to force update of
	 * stars, constellations, milky way and nebula,
	 * and objects, false to do that only when required.
	 */
	public void dateChanged(boolean forceAll)
	{
		if (drawAll) toggleAllPosibleObjectsInNextFrame();

		DataBase.addData("satEphem", threadID, null, true);
		DataBase.addData("probeEphem", threadID, null, true);
		DataBase.addData("asterEphem", threadID, null, true);
		DataBase.addData("cometEphem", threadID, null, true);
		DataBase.addData("neoEphem", threadID, null, true);
		DataBase.addData("transEphem", threadID, null, true);

		db_satEphem = -1;
		db_probeEphem = -1;
		db_asterEphem = -1;
		db_cometEphem = -1;
		db_neoEphem = -1;
		db_transEphem = -1;
		//DataBase.addData("milkyWayTexture", threadID, null, true);
		//db_milkyWayTexture = -1;

		loc0J2000 = null;
		render.planetRender.moonephem = null;
		this.planets = null;
		majorObjects = null;
		majorObjectsSats = null;
		ephem = null;
		baryc = null;

		labelRA = null;
		labelDEC = null;
		lati0 = null;

		neverWaitS = false;
		neverWaitP = false;
		neverWaitC = false;
		neverWaitN = false;
		neverWaitA = false;
		neverWaitT = false;
		brightestComet = -100;

		if (forceAll || (projection.obs.getMotherBody() != TARGET.NOT_A_PLANET ||
				Math.abs(originalJD - jd) > 365) || fieldDeg < 20) {
			if (re_star != null) this.re_star.setReadElements(null);
			DataBase.addData("conlim", threadID, null, true);
			DataBase.addData("conlin", threadID, null, true);
			DataBase.addData("objects", threadID, null, true);
			DataBase.addData("milkyWay", threadID, null, true);
			DataBase.addData("nebula", threadID, null, true);
			DataBase.addData("connom", threadID, null, true);
			db_objects = -1;
			db_conlim = -1;
			db_conlin = -1;
			db_milkyWay = -1;
			db_nebula = -1;
			db_connom = -1;
		}
		// ovals = null;

		DataBase.addData("sncat", threadID, null, true);
		DataBase.addData("novae", threadID, null, true);
		DataBase.addData("horizonLine", threadID, null, true);
		DataBase.addData("eclipticLine", threadID, null, true);
		DataBase.addData("raline", threadID, null, true);
		DataBase.addData("decline", threadID, null, true);
		DataBase.addData("meteor", threadID, null, true);
		db_meteor = -1;
		db_sncat = -1;
		db_novae = -1;
		db_horizonLine = -1;
		db_eclipticLine = -1;
		db_raline = -1;
		db_decline = -1;

		firstTime = true;
		faintStars = null;
		DataBase.addData("sunSpot", threadID, null, true);
		db_sunSpot = -1;
		jd_ut = -1;
		lastLeyend = -1;
		neverWait = false;
		RenderPlanet.lastRenderElement = null;
		RenderPlanet.dateChanged();
		imgs = new String[0];
		images = new ArrayList<Object>();
	}

	/**
	 * Returns data for the object closest to certain screen coordinates. The data returned
	 * is an array of objects with the following information:<P>
	 * - Integer ID constant for the object type. Constants defined in this class (enum OBJECT).<BR>
	 * - Integer array with the position in the screen x and y.<BR>
	 * - Information for the object. For stars, planets, natural or artificial satellites, comets, asteroids and probes
	 * an object of type {@linkplain EphemElement} is returned, for deep sky objects a String array
	 * (with name, RA, DEC, magnitude, subtype, size in degrees, and comments), and for supernovae a
	 * String array (with name, RA, DEC, magnitude, and date of explosion).<BR>
	 * - Star additional data from catalogue (only for stars).
	 * @param x X position in pixels.
	 * @param y Y position in pixels.
	 * @param considerMagLim True to consider the limiting magnitude (recommended) to identify the
	 * object closest to the given position. This is done only for planets and stars.
	 * @param fullDataAndPrecision True to return also rise, set, transit times for the closets object. Note
	 * this will be much slower in some cases.
	 * @return Object data, or null if no object is found or the position is outside the visible map.
	 * @throws JPARSECException If an error occurs.
	 */
	public Object[] getClosestObjectData(int x, int y, boolean considerMagLim, boolean fullDataAndPrecision)
	throws JPARSECException {
		return getClosestObjectData(x, y, true, true, true, 100, considerMagLim, fullDataAndPrecision);
	}
	
	/**
	 * Returns data for the object closest to certain screen coordinates. The data returned
	 * is an array of objects with the following information:<P>
	 * - Integer ID constant for the object type. Constants defined in this class (enum OBJECT).<BR>
	 * - Integer array with the position in the screen x and y.<BR>
	 * - Information for the object. For stars, planets, natural or artificial satellites, comets, asteroids and probes
	 * an object of type {@linkplain EphemElement} is returned, for deep sky objects a String array
	 * (with name, RA, DEC, magnitude, subtype, size in degrees, and comments), and for supernovae a
	 * String array (with name, RA, DEC, magnitude, and date of explosion).<BR>
	 * - Star additional data from catalogue (only for stars).
	 * @param x X position in pixels.
	 * @param y Y position in pixels.
	 * @param stars True to search for stars.
	 * @param plan True to search for planets.
	 * @param dso True to search for deep sky objects.
	 * @param starMaglim Limiting magnitude for the stars to search, set to a high value to avoid this 
	 * optional limitation, and set the next parameter to false.
	 * @param considerMagLim True to consider the limiting magnitude (recommended) to identify the
	 * object closest to the given position. This is done only for planets and stars. If false the previous 
	 * customized limiting magnitude will be used for stars.
	 * @param fullDataAndPrecision True to return also rise, set, transit times for the closets object. Note
	 * this will be much slower in some cases.
	 * @return Object data, or null if no object is found or the position is outside the visible map.
	 * @throws JPARSECException If an error occurs.
	 */
	public Object[] getClosestObjectData(int x, int y, boolean stars, 
			boolean plan, boolean dso, float starMaglim, boolean considerMagLim, boolean fullDataAndPrecision)
	throws JPARSECException {
		double minDist = -1;
		Object data[] = null;
		if (!rec.contains(x, y)) return data;

		boolean noFaintStar = false;
		if (stars) {
			double star[] = this.getClosestStarInScreenCoordinates(x, y, considerMagLim ? maglim : starMaglim);
			if (star != null) {
				if (star[1] < minDist || minDist == -1.0) {
					StarEphemElement ephem = this.calcStar((int) star[0], fullDataAndPrecision);
					if (ephem.magnitude < starMaglim) {
						float[] pos = projection.project(new LocationElement(ephem.rightAscension, ephem.declination, 1.0), 0, true);
						data = new Object[] {
								RenderSky.OBJECT.STAR, pos, EphemElement.parseStarEphemElement(ephem), this.getStar((int) star[0])
						};
						minDist = star[1];
		//				if (ephem.magnitude > maglim - 2) noFaintStar = true;
					}
				}
			}
		}

		if (plan) {
			boolean consider_satellites = true;
			Object planet[] = this.getClosestPlanetInScreenCoordinates(x, y, consider_satellites);
			if (planet != null) {
				double d = (Double) planet[1];
				if (d < minDist || minDist == -1 || (noFaintStar && d < 60)) {
					TARGET p = (TARGET) planet[0];
					EphemElement ephem = this.calcPlanet(p, true, fullDataAndPrecision);
					boolean isPlanet = p.isPlanet();
					if (p == TARGET.SUN || p == TARGET.Moon || p == TARGET.Pluto) isPlanet = true;
					if (!isPlanet && considerMagLim && ephem.magnitude > maglim) {
						planet = this.getClosestPlanetInScreenCoordinates(x, y, false);
						ephem = this.calcPlanet((TARGET) planet[0], true, fullDataAndPrecision);
						if (considerMagLim && ephem.magnitude > maglim) ephem = null;
					}
					if (ephem != null) {
						float[] pos = projection.project(new LocationElement(ephem.rightAscension, ephem.declination, 1.0), 0, false);
						data = new Object[] {
								RenderSky.OBJECT.PLANET, pos, ephem
						};
						minDist = (Double) planet[1];
						noFaintStar = false;
					}
				}
			}
		}
		
		if (dso) {
			ArrayList<Object> minorObjects = new ArrayList<Object>();
			Object o = null;
			if (db_minorObjects >= 0) {
				o = DataBase.getData(db_minorObjects);
			} else {
				o = DataBase.getData("minorObjects", threadID, true);
			}
			if (o != null) minorObjects = new ArrayList<Object>(Arrays.asList((Object[]) o));
			if (minorObjects != null && minorObjects.size() > 0) {
				if (render.telescope.invertHorizontal) x = render.width-1-x;
				if (render.telescope.invertVertical) y = render.height-1-y;
				double minDist2 = minDist * minDist;
				for (int i=0; i<minorObjects.size(); i++)
				{
		 			Object d[] = ((Object[]) minorObjects.get(i)).clone();
		 			float pos[] = (float[]) d[1];
					double dx = pos[0] - x, dy = pos[1] - y;
					double dist = (dx*dx+dy*dy);
					if (rec.contains(pos[0], pos[1]) && dist < minDist2 || minDist == -1.0|| (noFaintStar && dist < 3600)) {
						noFaintStar = false;
						minDist = Math.sqrt(dist);
						minDist2 = dist;
						data = d;
						OBJECT type = (OBJECT) d[0];
						if (type != RenderSky.OBJECT.DEEPSKY &&
								type != RenderSky.OBJECT.SUPERNOVA && type != OBJECT.NOVA && type != OBJECT.METEOR_SHOWER) {
							EphemElement ephem = null;
							if (type == RenderSky.OBJECT.ARTIFICIAL_SATELLITE) {
								SatelliteEphemElement satEp = (SatelliteEphemElement) data[2];
								ephem = EphemElement.parseSatelliteEphemElement(satEp, projection.eph.getEpoch(jd));
								data[2] = ephem;
							} else {
								ephem = ((EphemElement) data[2]).clone();
							}
							if (fullDataAndPrecision) {
								EphemElement fullEphem = this.getEphemerisOfMinorObject(ephem.name, type, fullDataAndPrecision);
								if (fullEphem != null) {
									ephem.rise = fullEphem.rise;
									ephem.set = fullEphem.set;
									ephem.transit = fullEphem.transit;
									ephem.transitElevation = fullEphem.transitElevation;
									ephem.name = fullEphem.name;
								}
							}
							data[2] = ephem;
						} else {
							String objData[] = new String[4]; //((String[]) data[2]).clone();
							LocationElement loc = (LocationElement) data[2];
							loc = projection.toEquatorialPosition(loc, false);
							objData[1] = Functions.formatRA(loc.getLongitude(), 2);
							objData[2] = Functions.formatDEC(loc.getLatitude(), 1);
							objData[0] = (String) data[4];
							if (type == RenderSky.OBJECT.METEOR_SHOWER) {
								objData[3] = "" + (int) (0.5 + (Float) data[3]);							
							} else {
								objData[3] = "" + (Float) data[3];
							}
							if (data.length > 5) {
								if (type == OBJECT.SUPERNOVA || type == OBJECT.NOVA) {
									objData = DataSet.addStringArray(objData, ((String) data[5])); 									
								} else {
									Object oo[] = ((Object[]) data[5]);
									String objType = (String) oo[0];
									if (DataSet.isDoubleFastCheck(objType)) objType = objt[Integer.parseInt(objType)];
									float size[] = (float[]) oo[1];
									objData = DataSet.addStringArray(objData, 
											new String[] {objType, ""+size[0]+"x"+size[1], (String) oo[2]}); 
								}
							}
							if (type == RenderSky.OBJECT.DEEPSKY) {
								String name = (String) data[4];
								if (name.startsWith("I."))
									name = DataSet.replaceAll(name, "I.", "IC ", true);
								try {
									String name2 = FileIO.getField(1, name, " ", true);
									if (DataSet.isDoubleStrictCheck(name2) || (name2.length() > 4 && DataSet.isDoubleStrictCheck(name2.substring(0, 4)))) {
										boolean ok = true;
										String f2 = FileIO.getField(2, name, " ", true);
										if (f2 != null && f2.length() > 0 && !f2.equals("-")) {
											ok = false;
											if (f2.startsWith("M")) {
												try {
													int mn = Integer.parseInt(f2.substring(1));
													ok = true;
												} catch (Exception exc2) {}
											}
											if (f2.startsWith("CALDWELL")) ok = true;
										}
										if (ok && (name.length() < 8 || name.indexOf(" ") > 0)) name = "NGC " + name;
									}
								} catch (Exception exc) {}
								objData[0] = name;
							}
							data[2] = objData;
						}
					}
				}
			}
		}

		return data;
	}
	
	/**
	 * Obtains full ephemeris (including rise, set, transit times) of a 'minor' body, which is any
	 * asteroid, comet, probe, transneptunian object, artificial satellite, supernova, or deep sky
	 * object shown in the rendering.
	 * @param objName Name of the object.
	 * @param objType Type of object. Constants defined in this class.
	 * @param fullEphem True to return full ephemerides include rise, set, transit.
	 * @return Object with the ephemeris. For some situations only certain fields of the object
	 * take sense, like name, RA, DEC, rise, set, transit, azimuth, elevation, and maybe some other.
	 * If the object name is not found null is returned.
	 * @throws JPARSECException If the object type is set to an invalid value, like stars, planets,
	 * or any other unrecognized value.
	 */
	public EphemElement getEphemerisOfMinorObject(String objName, OBJECT objType,
			boolean fullEphem)
	throws JPARSECException {
		EphemerisElement eph = projection.eph.clone();
		EphemElement ephem = null;
		switch (objType)
		{
		case TRANSNEPTUNIAN:
			eph.targetBody = TARGET.Asteroid;
			eph.algorithm = EphemerisElement.ALGORITHM.ORBIT;
			int index = OrbitEphem.getIndexOfTransNeptunian(objName);
			if (index >= 0) {
				OrbitalElement orbit = OrbitEphem.getOrbitalElementsOfTransNeptunian(index);
				eph.orbit = orbit;
				ephem = Ephem.getEphemeris(projection.time, projection.obs, eph, fullEphem); // To get rise, set, transit
			}
			break;
		case ASTEROID:
			eph.targetBody = TARGET.Asteroid;
			eph.algorithm = EphemerisElement.ALGORITHM.ORBIT;
			index = OrbitEphem.getIndexOfAsteroid(objName);
			if (index >= 0) {
				OrbitalElement orbit = OrbitEphem.getOrbitalElementsOfAsteroid(index);
				eph.orbit = orbit;
				ephem = Ephem.getEphemeris(projection.time, projection.obs, eph, fullEphem); // To get rise, set, transit
			}
			break;
		case COMET:
			eph.targetBody = TARGET.Comet;
			eph.algorithm = EphemerisElement.ALGORITHM.ORBIT;
			index = OrbitEphem.getIndexOfComet(objName);
			if (index >= 0) {
				OrbitalElement orbit = OrbitEphem.getOrbitalElementsOfComet(index);
				eph.orbit = orbit;
				ephem = Ephem.getEphemeris(projection.time, projection.obs, eph, fullEphem); // To get rise, set, transit
				int b1 = ephem.name.indexOf("  ");
				if (b1 > 0) ephem.name = ephem.name.substring(0, b1).trim();
			}
			break;
		case NEO:
			eph.targetBody = TARGET.NEO;
			eph.algorithm = EphemerisElement.ALGORITHM.ORBIT;
			ReadFile re = new ReadFile(FORMAT.MPC, OrbitEphem.PATH_TO_MPC_NEOs_FILE);
			try { re.readFileOfNEOs(jd, 365); } catch (Exception exc) {}
			int n = re.getNumberOfObjects();
			index = -1;
			if (n > 0) {
				for (int i=0; i<n;i++) {
					OrbitalElement orbit = re.getOrbitalElement(i);
					if (orbit.name.equals(objName)) {
						index = i;
						eph.orbit = orbit;
						break;
					}
				}
				if (index >= 0) {
					ephem = Ephem.getEphemeris(projection.time, projection.obs, eph, fullEphem); // To get rise, set, transit
					int b1 = ephem.name.indexOf("  ");
					if (b1 > 0) ephem.name = ephem.name.substring(0, b1).trim();
				}
			}
			break;
		case PROBE:
			eph.algorithm = EphemerisElement.ALGORITHM.PROBE;
			index = Spacecraft.getIndex(objName, projection.jd);
			if (index >= 0) {
				eph.orbit = Spacecraft.getProbeElement(index);
				try { ephem = Ephem.getEphemeris(projection.time, projection.obs, eph, fullEphem); // To get rise, set, transit
				} catch (Exception exc) {}
			}
			break;
		case ARTIFICIAL_SATELLITE:
			eph.algorithm = EphemerisElement.ALGORITHM.ARTIFICIAL_SATELLITE;
			index = SatelliteEphem.getArtificialSatelliteTargetIndex(objName);
			if (index >= 0) {
				eph.targetBody = TARGET.NOT_A_PLANET;
				eph.targetBody.setIndex(index);
				ephem = Ephem.getEphemeris(projection.time, projection.obs, eph, fullEphem);
			}
			break;
		case SUPERNOVA:
		case NOVA:
			break;
		case DEEPSKY:
			break;
		default:
			throw new JPARSECException("unsupported minor object type.");
		}
		return ephem;
	}

	/**
	 * Returns the proper name of a star given its catalog name.
	 * @param catalogName Catalog name.
	 * @return Proper name, or null if the star has no proper name.
	 * @throws JPARSECException If an error occurs.
	 */
	public String getStarProperName(String catalogName)
	throws JPARSECException {
		Object o2 = null, o = null;
		catalogName = DataSet.replaceOne(catalogName, "Alp1 ", "Alp ", 1);
		catalogName = DataSet.replaceOne(catalogName, "Bet1 ", "Bet ", 1);
		catalogName = DataSet.replaceOne(catalogName, "Gam1 ", "Gam ", 1);
		catalogName = DataSet.replaceOne(catalogName, "The1 ", "The ", 1);
		catalogName = DataSet.replaceOne(catalogName, "Psi1 ", "Psi ", 1);

		if (db_starNames >= 0) {
			o2 = DataBase.getData(db_starNames2);
			o = DataBase.getData(db_starNames);
		} else {
			o2 = DataBase.getData("starNames2", threadID, true);
			o = DataBase.getData("starNames", threadID, true);
		}
		String[] names2 = null, names = null;
		if (o2 == null) {
			String data[] = DataSet.arrayListToStringArray(ReadFile.readResource(FileIO.DATA_SKY_DIRECTORY + "star_names.txt"));
			names2 = DataSet.extractColumnFromTable(data, ";", 0);
			if (render.drawStarsLabels == SkyRenderElement.STAR_LABELS.ONLY_PROPER_NAME_SPANISH) {
				names = DataSet.extractColumnFromTable(data, ";", 2);
			} else {
				names = DataSet.extractColumnFromTable(data, ";", 1);
			}
			DataBase.addData("starNames", threadID, names, true);
			DataBase.addData("starNames2", threadID, names2, true);
			if (db_starNames < 0) db_starNames = DataBase.getIndex("starNames", threadID);
			if (db_starNames2 < 0) db_starNames2 = DataBase.getIndex("starNames2", threadID);
		} else {
			names2 = (String[]) o2;
			names = (String[]) o;
		}

		String properName = catalogName;

		int bracket1 = catalogName.indexOf("(");
		int bracket2 = catalogName.indexOf(")");
		if (bracket1 >= 0 && bracket2 >= 0)
		{
			properName = catalogName.substring(bracket1 + 1, bracket2);
		} else {
			try {
				int i = Integer.parseInt(catalogName); // Ensure is an int
				properName = Integer.toString(i);
			} catch (Exception exc) {
				//return null;
			}
		}

		if (names != null && names2 != null) {
			for (int n = 0; n < names2.length; n++)
			{
				String line = names2[n];
				int aa = line.toLowerCase().indexOf(properName.toLowerCase());
				if (aa >= 0)
				{
					return names[n];
				}
			}
		}
		return null;
	}

	/**
	 * Searches for an object and return its coordinates. Planets, deep sky objects,
	 * stars, asteroids, comets, probes, and artificial satellites are considered.
	 * @param s Object name.
	 * @return Equatorial coordinates, or null if not found.
	 */
	public LocationElement searchObject(String s)
	{
		try {
			if (s.toUpperCase().equals("M1") || s.toUpperCase().equals("M 1")) s = "NGC 1952";
				LocationElement loc = null;
				TARGET planet = Target.getID(s);
				if (planet == TARGET.NOT_A_PLANET) planet = Target.getIDFromEnglishName(s);
				if (planet != TARGET.NOT_A_PLANET) {
					EphemElement ephem = calcPlanet(planet, true, false);
					if (ephem != null) loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
				}
				if (loc == null) loc = searchDeepSkyObject(s);
				if (loc == null) {
					StarEphemElement star = searchStar(s);
					if (star != null) loc = new LocationElement(star.rightAscension, star.declination, 1.0);
				}
				if (loc == null) {
					EphemElement ephem = getEphemerisOfMinorObject(s, RenderSky.OBJECT.ASTEROID, false);
					if (ephem != null) loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
				}
				if (loc == null) {
					EphemElement ephem = getEphemerisOfMinorObject(s, RenderSky.OBJECT.COMET, false);
					if (ephem != null) loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
				}
				if (loc == null) {
					EphemElement ephem = getEphemerisOfMinorObject(s, RenderSky.OBJECT.NEO, false);
					if (ephem != null) loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
				}
				if (loc == null) {
					EphemElement ephem = getEphemerisOfMinorObject(s, RenderSky.OBJECT.ARTIFICIAL_SATELLITE, false);
					if (ephem != null) loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
				}
				if (loc == null) {
					EphemElement ephem = getEphemerisOfMinorObject(s, RenderSky.OBJECT.PROBE, false);
					if (ephem != null) loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
				}
				if (loc == null) {
					EphemElement ephem = getEphemerisOfMinorObject(s, RenderSky.OBJECT.TRANSNEPTUNIAN, false);
					if (ephem != null) loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
				}
				if (loc == null) {
					ArrayList<Object> sncat = null;
					Object oo = null;
					if (db_sncat >= 0) {
						oo = DataBase.getData(db_sncat);
					} else {
						oo = DataBase.getData("sncat", threadID, true);
					}
					if (oo != null) {
						sncat = new ArrayList<Object>(Arrays.asList((Object[]) oo));
						for (Iterator<Object> itr = sncat.iterator();itr.hasNext();)
						{
							Object obj[] = (Object[]) itr.next();
							String n = (String) obj[1];
							if (n.toLowerCase().equals(s.toLowerCase())) {
								loc = (LocationElement) obj[0];
								loc = projection.toEquatorialPosition(loc, false);
								//loc = Ephem.removeRefractionCorrectionFromEquatorialCoordinates(projection.time, projection.obs, projection.eph, loc);
								break;
							}
						}
					}
				}
				if (loc == null) {
					ArrayList<Object> sncat = null;
					Object oo = null;
					if (db_novae >= 0) {
						oo = DataBase.getData(db_novae);
					} else {
						oo = DataBase.getData("novae", threadID, true);
					}
					if (oo != null) {
						sncat = new ArrayList<Object>(Arrays.asList((Object[]) oo));
						for (Iterator<Object> itr = sncat.iterator();itr.hasNext();)
						{
							Object obj[] = (Object[]) itr.next();
							String n = (String) obj[1];
							if (n.toLowerCase().equals(s.toLowerCase())) {
								loc = (LocationElement) obj[0];
								loc = projection.toEquatorialPosition(loc, false);
								//loc = Ephem.removeRefractionCorrectionFromEquatorialCoordinates(projection.time, projection.obs, projection.eph, loc);
								break;
							}
						}
					}
				}
				if (loc == null) {
					ArrayList<Object> met = null;
					Object oo = null;
					if (db_meteor >= 0) {
						oo = DataBase.getData(db_meteor);
					} else {
						oo = DataBase.getData("meteor", threadID, true);
					}
					if (oo != null && s.length() > 4) {
						met = new ArrayList<Object>(Arrays.asList((Object[]) oo));
						for (Iterator<Object> itr = met.iterator();itr.hasNext();)
						{
							Object obj[] = (Object[]) itr.next();
							String n = (String) obj[0];
							if (n.toLowerCase().startsWith(s.toLowerCase())) {
								loc = (LocationElement) obj[2];
								loc = projection.toEquatorialPosition(loc, false);
								//loc = Ephem.removeRefractionCorrectionFromEquatorialCoordinates(projection.time, projection.obs, projection.eph, loc);
								break;
							}
						}
					}
				}
				return loc;
			} catch (Exception exc) {
				Logger.log(LEVEL.ERROR, "Error searching for object "+s+". Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
				return null;
			}
	}

	/**
	 * Searches for objects and return its coordinates. Planets, deep sky objects,
	 * stars, asteroids, comets, probes, and artificial satellites are considered.
	 * @param s Object name.
	 * @return Arrays with the names of hte objects and the equatorial coordinates of them.
	 * null if none is found. The radius of the location object will be an integer 
	 * provided the index of the @linkplain{OBJECT} type.
	 */
	public Object[] searchObjects(String s)
	{
		LocationElement out[] = new LocationElement[0];
		String name = null, names[] = new String[0];
		try {
			if (s.toUpperCase().equals("M1") || s.toUpperCase().equals("M 1")) s = "NGC 1952";
				LocationElement loc = null;
				TARGET planet = Target.getID(s);
				if (planet == TARGET.NOT_A_PLANET) planet = Target.getIDFromEnglishName(s);
				if (planet != TARGET.NOT_A_PLANET) {
					EphemElement ephem = calcPlanet(planet, true, false);
					if (ephem != null) {
						loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
						name = ephem.name;
					}
				}
				if (loc != null) {
					loc.setRadius(OBJECT.PLANET.ordinal());
					out = (LocationElement[]) DataSet.addObjectArray(out, new LocationElement[] {loc});
					names = DataSet.addStringArray(names, name);
				}
				Object outs[] = searchDeepSkyObjects(s);
				if (outs != null) {
					String n[] = (String[]) outs[0];
					LocationElement[] locs = (LocationElement[]) outs[1];
					for (int i=0; i<locs.length; i++) {
						locs[i].setRadius(OBJECT.DEEPSKY.ordinal());
						names = DataSet.addStringArray(names, n[i]);
					}
					out = (LocationElement[]) DataSet.addObjectArray(out, locs);
				}
				StarEphemElement star[] = searchStars(s);
				if (star != null) {
					for (int i=0; i<star.length; i++) {
						loc = new LocationElement(star[i].rightAscension, star[i].declination, OBJECT.STAR.ordinal());
						out = (LocationElement[]) DataSet.addObjectArray(out, new LocationElement[] {loc});
						names = DataSet.addStringArray(names, star[i].name);
					}
				}
				EphemElement ephem = getEphemerisOfMinorObject(s, RenderSky.OBJECT.ASTEROID, false);
				if (ephem != null) {
					loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
					if (loc != null) {
						loc.setRadius(OBJECT.ASTEROID.ordinal());
						out = (LocationElement[]) DataSet.addObjectArray(out, new LocationElement[] {loc});
						names = DataSet.addStringArray(names, ephem.name);
					}
				}
				ephem = getEphemerisOfMinorObject(s, RenderSky.OBJECT.COMET, false);
				if (ephem != null) {
					loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
					if (loc != null) {
						loc.setRadius(OBJECT.COMET.ordinal());
						out = (LocationElement[]) DataSet.addObjectArray(out, new LocationElement[] {loc});
						names = DataSet.addStringArray(names, ephem.name);
					}
				}
				ephem = getEphemerisOfMinorObject(s, RenderSky.OBJECT.NEO, false);
				if (ephem != null) {
					loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
					if (loc != null) {
						loc.setRadius(OBJECT.NEO.ordinal());
						out = (LocationElement[]) DataSet.addObjectArray(out, new LocationElement[] {loc});
						names = DataSet.addStringArray(names, ephem.name);
					}
				}
				ephem = getEphemerisOfMinorObject(s, RenderSky.OBJECT.ARTIFICIAL_SATELLITE, false);
				if (ephem != null) {
					loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
					if (loc != null) {
						loc.setRadius(OBJECT.ARTIFICIAL_SATELLITE.ordinal());
						out = (LocationElement[]) DataSet.addObjectArray(out, new LocationElement[] {loc});
						names = DataSet.addStringArray(names, ephem.name);
					}
				}
				ephem = getEphemerisOfMinorObject(s, RenderSky.OBJECT.PROBE, false);
				if (ephem != null) {
					loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
					if (loc != null) {
						loc.setRadius(OBJECT.PROBE.ordinal());
						out = (LocationElement[]) DataSet.addObjectArray(out, new LocationElement[] {loc});
						names = DataSet.addStringArray(names, ephem.name);
					}
				}
				ephem = getEphemerisOfMinorObject(s, RenderSky.OBJECT.TRANSNEPTUNIAN, false);
				if (ephem != null) {
					loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
					if (loc != null) {
						loc.setRadius(OBJECT.TRANSNEPTUNIAN.ordinal());
						out = (LocationElement[]) DataSet.addObjectArray(out, new LocationElement[] {loc});
						names = DataSet.addStringArray(names, ephem.name);
					}
				}
				ArrayList<Object> sncat = null;
				Object oo = null;
				if (db_sncat >= 0) {
					oo = DataBase.getData(db_sncat);
				} else {
					oo = DataBase.getData("sncat", threadID, true);
				}
				if (oo != null) {
					sncat = new ArrayList<Object>(Arrays.asList((Object[]) oo));
					for (Iterator<Object> itr = sncat.iterator();itr.hasNext();)
					{
						Object obj[] = (Object[]) itr.next();
						String n = (String) obj[1];
						if (n.toLowerCase().indexOf(s.toLowerCase()) >= 0) {
							loc = (LocationElement) obj[0];
							if (loc == null) continue;
							loc = projection.toEquatorialPosition(loc, false);
							if (loc != null) {
								loc.setRadius(OBJECT.SUPERNOVA.ordinal());
								out = (LocationElement[]) DataSet.addObjectArray(out, new LocationElement[] {loc});
								names = DataSet.addStringArray(names, n);
							}
							//loc = Ephem.removeRefractionCorrectionFromEquatorialCoordinates(projection.time, projection.obs, projection.eph, loc);
							//break;
						}
					}
				}

				sncat = null;
				oo = null;
				if (db_novae >= 0) {
					oo = DataBase.getData(db_novae);
				} else {
					oo = DataBase.getData("novae", threadID, true);
				}
				if (oo != null) {
					sncat = new ArrayList<Object>(Arrays.asList((Object[]) oo));
					for (Iterator<Object> itr = sncat.iterator();itr.hasNext();)
					{
						Object obj[] = (Object[]) itr.next();
						String n = (String) obj[1];
						if (n.toLowerCase().indexOf(s.toLowerCase()) >= 0) {
							loc = (LocationElement) obj[0];
							if (loc == null) continue;
							loc = projection.toEquatorialPosition(loc, false);
							if (loc != null) {
								loc.setRadius(OBJECT.NOVA.ordinal());
								out = (LocationElement[]) DataSet.addObjectArray(out, new LocationElement[] {loc});
								names = DataSet.addStringArray(names, n);
							}
							//loc = Ephem.removeRefractionCorrectionFromEquatorialCoordinates(projection.time, projection.obs, projection.eph, loc);
							//break;
						}
					}
				}

				ArrayList<Object> met = null;
				oo = null;
				if (db_meteor >= 0) {
					oo = DataBase.getData(db_meteor);
				} else {
					oo = DataBase.getData("meteor", threadID, true);
				}
				if (oo != null && s.length() > 3) {
					met = new ArrayList<Object>(Arrays.asList((Object[]) oo));
					for (Iterator<Object> itr = met.iterator();itr.hasNext();)
					{
						Object obj[] = (Object[]) itr.next();
						String n = (String) obj[0];
						if (n.toLowerCase().indexOf(s.toLowerCase()) >= 0) {
							loc = (LocationElement) obj[2];
							if (loc == null) continue;
							loc = projection.toEquatorialPosition(loc, false);
							if (loc != null) {
								loc.setRadius(OBJECT.METEOR_SHOWER.ordinal());
								out = (LocationElement[]) DataSet.addObjectArray(out, new LocationElement[] {loc});
								names = DataSet.addStringArray(names, n);
							}
							//loc = Ephem.removeRefractionCorrectionFromEquatorialCoordinates(projection.time, projection.obs, projection.eph, loc);
							//break;
						}
					}
				}
				
		    	 double cra[] = null, cdec[] = null;
		    	 String cons[] = null;
		    	 try {
		    		 double jd = projection.jd;
		        	 ArrayList<Object> constel = Constellation.getConstellations(jd, render.drawConstellationNamesType);
		        	 cons = (String[]) constel.get(0);
			    	 cra = (double[]) constel.get(1);
			    	 cdec = (double[]) constel.get(2);
		    	 } catch (Exception exc) {}
		    	 if (projection.obs.getMotherBody() != TARGET.NOT_A_PLANET && 
		    			 projection.obs.getMotherBody() != TARGET.EARTH) {
		 			try {
		 				double jd = projection.jd;
			 			EphemerisElement ephIn = projection.eph.clone();
			 			ephIn.targetBody = projection.obs.getMotherBody();
			 			LocationElement np = PhysicalParameters.getBodyNorthPole(jd, ephIn);
			 			for (int i=0; i<cra.length; i++) {
			 				loc = new LocationElement(cra[i], cdec[i], 1);
			 				loc = new LocationElement(LocationElement.getApproximatePositionAngle(np, loc), Constant.PI_OVER_TWO - LocationElement.getApproximateAngularDistance(np, loc), loc.getRadius());
			 				cra[i] = loc.getLongitude();
			 				cdec[i] = loc.getLatitude();
			 			}
		 			} catch (Exception exc) {}
		    	 }
		    	 if (cons != null && cra != null && cdec != null) {
		    		 for (int i=0; i<cons.length; i++) {
		    			 if (s.toLowerCase().equals(cons[i].toLowerCase())) {
								out = (LocationElement[]) DataSet.addObjectArray(new LocationElement[] {new LocationElement(cra[i], cdec[i], -1)}, out);		    				 
								names = DataSet.addStringArray(names, cons[i]);
		    			 } else {
			    			 if (cons[i].toLowerCase().indexOf(s.toLowerCase()) >= 0) {
								out = (LocationElement[]) DataSet.addObjectArray(out, new LocationElement[] {new LocationElement(cra[i], cdec[i], -1)});
								names = DataSet.addStringArray(names, cons[i]);
			    			 }
		    			 }
		    		 }
		    	 }
		    	 
			if (out.length == 0) return null;
			return new Object[] {names, out};
		} catch (Exception exc) {
			Logger.log(LEVEL.ERROR, "Error searching for objects "+s+". Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
			return null;
		}
	}
	
	/**
	 * Searches for an object and return its coordinates. Planets,
	 * asteroids, comets, and probes are considered.
	 * @param s Object name.
	 * @return Equatorial coordinates, or null if not found.
	 */
	public LocationElement searchSolarSystemObject(String s)
	{
		try {
				LocationElement loc = null;
				TARGET planet = Target.getID(s);
				if (planet == TARGET.NOT_A_PLANET) planet = Target.getIDFromEnglishName(s);
				if (planet != TARGET.NOT_A_PLANET) {
					EphemElement ephem = calcPlanet(planet, true, false);
					if (ephem != null) loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
				}
				if (loc == null) {
					EphemElement ephem = getEphemerisOfMinorObject(s, RenderSky.OBJECT.ASTEROID, false);
					if (ephem != null) loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
				}
				if (loc == null) {
					EphemElement ephem = getEphemerisOfMinorObject(s, RenderSky.OBJECT.COMET, false);
					if (ephem != null) loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
				}
				if (loc == null) {
					EphemElement ephem = getEphemerisOfMinorObject(s, RenderSky.OBJECT.NEO, false);
					if (ephem != null) loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
				}
				if (loc == null) {
					EphemElement ephem = getEphemerisOfMinorObject(s, RenderSky.OBJECT.PROBE, false);
					if (ephem != null) loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
				}
				if (loc == null) {
					EphemElement ephem = getEphemerisOfMinorObject(s, RenderSky.OBJECT.TRANSNEPTUNIAN, false);
					if (ephem != null) loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
				}
				return loc;
			} catch (Exception exc) {
				Logger.log(LEVEL.ERROR, "Error searching for object "+s+". Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
				return null;
			}
	}

	/**
	 * Searches for an object and return its coordinates. Planets, deep sky objects,
	 * stars, asteroids, comets, probes, and artificial satellites are considered.
	 * @param s Object name.
	 * @param type The type of object.
	 * @return Equatorial coordinates, or null if not found.
	 */
	public LocationElement searchObject(String s, RenderSky.OBJECT type)
	{
		try {
				LocationElement loc = null;
				if (type == OBJECT.PLANET) {
					TARGET planet = Target.getID(s);
					if (planet == TARGET.NOT_A_PLANET) planet = Target.getIDFromEnglishName(s);
					if (planet != TARGET.NOT_A_PLANET) {
						EphemElement ephem = calcPlanet(planet, true, false);
						if (ephem != null) loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
					}
				}
				if (loc == null && type == OBJECT.STAR) {
					StarEphemElement star = searchStar(s);
					if (star != null) loc = new LocationElement(star.rightAscension, star.declination, 1.0);
				}
				if (loc == null && type == OBJECT.DEEPSKY) loc = searchDeepSkyObject(s);
				if (loc == null && type == OBJECT.ASTEROID) {
					EphemElement ephem = getEphemerisOfMinorObject(s, RenderSky.OBJECT.ASTEROID, false);
					if (ephem != null) loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
				}
				if (loc == null && type == OBJECT.COMET) {
					EphemElement ephem = getEphemerisOfMinorObject(s, RenderSky.OBJECT.COMET, false);
					if (ephem != null) loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
				}
				if (loc == null && type == OBJECT.NEO) {
					EphemElement ephem = getEphemerisOfMinorObject(s, RenderSky.OBJECT.NEO, false);
					if (ephem != null) loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
				}
				if (loc == null && type == OBJECT.ARTIFICIAL_SATELLITE) {
					EphemElement ephem = getEphemerisOfMinorObject(s, RenderSky.OBJECT.ARTIFICIAL_SATELLITE, false);
					if (ephem != null) loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
				}
				if (loc == null && type == OBJECT.PROBE) {
					EphemElement ephem = getEphemerisOfMinorObject(s, RenderSky.OBJECT.PROBE, false);
					if (ephem != null) loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
				}
				if (loc == null && type == OBJECT.TRANSNEPTUNIAN) {
					EphemElement ephem = getEphemerisOfMinorObject(s, RenderSky.OBJECT.TRANSNEPTUNIAN, false);
					if (ephem != null) loc = new LocationElement(ephem.rightAscension, ephem.declination, 1.0);
				}

				if (loc == null && type == OBJECT.SUPERNOVA) {
					ArrayList<Object> sncat = null;
					Object oo = null;
					if (db_sncat >= 0) {
						oo = DataBase.getData(db_sncat);
					} else {
						oo = DataBase.getData("sncat", threadID, true);
					}
					if (oo != null) {
						sncat = new ArrayList<Object>(Arrays.asList((Object[]) oo));
						for (Iterator<Object> itr = sncat.iterator();itr.hasNext();)
						{
							Object obj[] = (Object[]) itr.next();
							String n = (String) obj[1];
							if (n.toLowerCase().equals(s.toLowerCase())) {
								loc = (LocationElement) obj[0];
								loc = projection.toEquatorialPosition(loc, false);
								//loc = Ephem.removeRefractionCorrectionFromEquatorialCoordinates(projection.time, projection.obs, projection.eph, loc);
								break;
							}
						}
					}
				}
				if (loc == null && type == OBJECT.NOVA) {
					ArrayList<Object> sncat = null;
					Object oo = null;
					if (db_novae >= 0) {
						oo = DataBase.getData(db_novae);
					} else {
						oo = DataBase.getData("novae", threadID, true);
					}
					if (oo != null) {
						sncat = new ArrayList<Object>(Arrays.asList((Object[]) oo));
						for (Iterator<Object> itr = sncat.iterator();itr.hasNext();)
						{
							Object obj[] = (Object[]) itr.next();
							String n = (String) obj[1];
							if (n.toLowerCase().equals(s.toLowerCase())) {
								loc = (LocationElement) obj[0];
								loc = projection.toEquatorialPosition(loc, false);
								//loc = Ephem.removeRefractionCorrectionFromEquatorialCoordinates(projection.time, projection.obs, projection.eph, loc);
								break;
							}
						}
					}
				}
				if (loc == null && type == OBJECT.METEOR_SHOWER) {
					ArrayList<Object> met = null;
					Object oo = null;
					if (db_meteor >= 0) {
						oo = DataBase.getData(db_meteor);
					} else {
						oo = DataBase.getData("meteor", threadID, true);
					}
					if (oo != null && s.length() > 4) {
						met = new ArrayList<Object>(Arrays.asList((Object[]) oo));
						for (Iterator<Object> itr = met.iterator();itr.hasNext();)
						{
							Object obj[] = (Object[]) itr.next();
							String n = (String) obj[0];
							if (n.toLowerCase().startsWith(s.toLowerCase())) {
								loc = (LocationElement) obj[2];
								loc = projection.toEquatorialPosition(loc, false);
								//loc = Ephem.removeRefractionCorrectionFromEquatorialCoordinates(projection.time, projection.obs, projection.eph, loc);
								break;
							}
						}
					}
				}
				return loc;
			} catch (Exception exc) {
				Logger.log(LEVEL.ERROR, "Error searching for object "+s+". Message was: "+exc.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(exc.getStackTrace()));
				return null;
			}
	}

	/**
	 * Returns the scale of the rendering.
	 * @return Scale in pixels per degree.
	 */
	public float getScale() {
		return pixels_per_degree;
	}

	private float getDist(double z) {
		if (pixels_per_degree > 100) {
			if (z == 0) return refz;
			return refz*1.2f;
		}

		if (new Double(z).equals(Double.NaN)) z = 0;
		return (float) (refz - z);
	}

	private float getDistStar(double z) {
		if (pixels_per_degree > 100) {
			if (z == 0) return refz;
			return refz*1.2f;
		}

		if (render.anaglyphMode.isReal3D()) {
			return (float) (z>30? refz:refz+(z-30)*refz/200f);
		} else {
			return (float) (z>100? refz:z+refz-101);
		}
	}

	private double lastEq = -1E100, XX, XY, XZ, YX, YY, YZ, ZX, ZY, ZZ;
	private EphemerisElement.REDUCTION_METHOD lastM = null;
	private double[] precessFromJ2000(double equinox, double r[], EphemerisElement eph) throws JPARSECException {
		if (eph.ephemMethod.isIAU20xx()) {
			if (lastEq != equinox || lastM == null || eph.ephemMethod != lastM) {
				double ang[] = Precession.getAngles(false, equinox, eph);

				double PSIA = ang[0], OMEGAA = ang[1], CHIA = ang[2], EPS0 = ang[3];
				double SA = Math.sin(EPS0);
				double CA = Math.cos(EPS0);
				double SB = Math.sin(-PSIA);
				double CB = Math.cos(-PSIA);
				double SC = Math.sin(-OMEGAA);
				double CC = Math.cos(-OMEGAA);
				double SD = Math.sin(CHIA);
				double CD = Math.cos(CHIA);

				// COMPUTE ELEMENTS OF PRECESSION ROTATION MATRIX
				// EQUIVALENT TO R3(CHI_A)R1(-OMEGA_A)R3(-PSI_A)R1(EPSILON_0)
				XX = CD * CB - SB * SD * CC;
				YX = CD * SB * CA + SD * CC * CB * CA - SA * SD * SC;
				ZX = CD * SB * SA + SD * CC * CB * SA + CA * SD * SC;
				XY = -SD * CB - SB * CD * CC;
				YY = -SD * SB * CA + CD * CC * CB * CA - SA * CD * SC;
				ZY = -SD * SB * SA + CD * CC * CB * SA + CA * CD * SC;
				XZ = SB * SC;
				YZ = -SC * CB * CA - SA * CC;
				ZZ = -SC * CB * SA + CC * CA;

				lastEq = equinox;
				lastM = eph.ephemMethod;
			}
			double px = XX * r[0] + YX * r[1] + ZX * r[2];
			double py = XY * r[0] + YY * r[1] + ZY * r[2];
			double pz = XZ * r[0] + YZ * r[1] + ZZ * r[2];
			return new double[] {px, py, pz};
		}

		return Precession.precessFromJ2000(equinox, r, eph);
	}
	private double[] precessToJ2000(double equinox, double r[], EphemerisElement eph) throws JPARSECException {
		if (eph.ephemMethod.isIAU20xx()) {
			if (lastEq != equinox || lastM == null || eph.ephemMethod != lastM) {
				double ang[] = Precession.getAngles(false, equinox, eph);

				double PSIA = ang[0], OMEGAA = ang[1], CHIA = ang[2], EPS0 = ang[3];
				double SA = Math.sin(EPS0);
				double CA = Math.cos(EPS0);
				double SB = Math.sin(-PSIA);
				double CB = Math.cos(-PSIA);
				double SC = Math.sin(-OMEGAA);
				double CC = Math.cos(-OMEGAA);
				double SD = Math.sin(CHIA);
				double CD = Math.cos(CHIA);

				// COMPUTE ELEMENTS OF PRECESSION ROTATION MATRIX
				// EQUIVALENT TO R3(CHI_A)R1(-OMEGA_A)R3(-PSI_A)R1(EPSILON_0)
				XX = CD * CB - SB * SD * CC;
				YX = CD * SB * CA + SD * CC * CB * CA - SA * SD * SC;
				ZX = CD * SB * SA + SD * CC * CB * SA + CA * SD * SC;
				XY = -SD * CB - SB * CD * CC;
				YY = -SD * SB * CA + CD * CC * CB * CA - SA * CD * SC;
				ZY = -SD * SB * SA + CD * CC * CB * SA + CA * CD * SC;
				XZ = SB * SC;
				YZ = -SC * CB * CA - SA * CC;
				ZZ = -SC * CB * SA + CC * CA;

				lastEq = equinox;
				lastM = eph.ephemMethod;
			}
			double px = XX * r[0] + XY * r[1] + XZ * r[2];
			double py = YX * r[0] + YY * r[1] + YZ * r[2];
			double pz = ZX * r[0] + ZY * r[1] + ZZ * r[2];
			return new double[] {px, py, pz};
		}

		return Precession.precessToJ2000(equinox, r, eph);
	}

	private double lastT = -1E100, nxx, nxy, nxz, nyx, nyy, nyz, nzx, nzy, nzz;
	private double[] nutateInEquatorialCoordinates(double jd_tt, EphemerisElement eph,
			double[] in, boolean meanToTrue) throws JPARSECException {
		if (lastT != jd_tt || lastM == null || eph.ephemMethod != lastM) {
			double t = Functions.toCenturies(jd_tt);
			double oblm = Obliquity.meanObliquity(t, eph);
			double oblt = Obliquity.trueObliquity(t, eph);
			double nut[] = Nutation.calcNutation(t, eph);
			double dpsi = nut[0];

			double cobm = Math.cos(oblm), sobm = Math.sin(oblm);
			double cobt = Math.cos(oblt), sobt = Math.sin(oblt);
			double cpsi = Math.cos(dpsi), spsi = Math.sin(dpsi);

			// Compute elements of nutation matrix
			nxx = cpsi;
			nyx = -spsi * cobm;
			nzx = -spsi * sobm;
			nxy = spsi * cobt;
			nyy = cpsi * cobm * cobt + sobm * sobt;
			nzy = cpsi * sobm * cobt - cobm * sobt;
			nxz = spsi * sobt;
			nyz = cpsi * cobm * sobt - sobm * cobt;
			nzz = cpsi * sobm * sobt + cobm * cobt;

			lastT = jd_tt;
			lastM = eph.ephemMethod;
		}

		double out[] = new double[in.length];
		if (meanToTrue) {
			out[0] = nxx * in[0] + nyx * in[1] + nzx * in[2];
			out[1] = nxy * in[0] + nyy * in[1] + nzy * in[2];
			out[2] = nxz * in[0] + nyz * in[1] + nzz * in[2];
			if (out.length == 6) {
				out[3] = nxx * in[3] + nyx * in[4] + nzx * in[5];
				out[4] = nxy * in[3] + nyy * in[4] + nzy * in[5];
				out[5] = nxz * in[3] + nyz * in[4] + nzz * in[5];
			}
		} else {
			out[0] = nxx * in[0] + nxy * in[1] + nxz * in[2];
			out[1] = nyx * in[0] + nyy * in[1] + nyz * in[2];
			out[2] = nzx * in[0] + nzy * in[1] + nzz * in[2];
			if (out.length == 6) {
				out[3] = nxx * in[3] + nyx * in[4] + nzx * in[5];
				out[4] = nxy * in[3] + nyy * in[4] + nzy * in[5];
				out[5] = nxz * in[3] + nyz * in[4] + nzz * in[5];
			}
		}

		return out;
	}

	/**
	 * Searches for a deep sky object given it's name.
	 *
	 * @param obj_name Name of the object as given in the catalog of deep
	 * sky objects.
	 * @return The location object with the J2000 equatorial position, or null if it is
	 * not found.
	 * @throws JPARSECException If the method fails.
	 */
	public static LocationElement searchDeepSkyObjectJ2000(String obj_name)
			throws JPARSECException
	{
		Object[] objs = populate(false);

		LocationElement s = null;
		for (int i = 0; i < objs.length; i++)
		{
			Object[] obj = (Object[]) objs[i];
			String messier = (String) obj[1];
			String name = (String) obj[0];
			String com = "";
			String comments = (String) obj[7];
			int pp = comments.indexOf("Popular name:");
			if (pp>=0) com = comments.substring(pp+14).trim();

			if (name.startsWith("I."))
				name = DataSet.replaceAll(name, "I.", "IC ", true);
			try {
				String name2 = FileIO.getField(1, name, " ", true);
				if (DataSet.isDoubleFastCheck(name2) || 
						(name2.length() > 4 && DataSet.isDoubleFastCheck(name2.substring(0, 4)))) {
					if (DataSet.isDoubleStrictCheck(name2) || (name2.length() > 4 && DataSet.isDoubleStrictCheck(name2.substring(0, 4)))) {
						boolean ok = true;
						String f2 = FileIO.getField(2, name, " ", true);
						if (f2 != null && f2.length() > 0 && !f2.equals("-")) {
							ok = false;
							if (f2.startsWith("M")) {
								try {
									int mn = Integer.parseInt(f2.substring(1));
									ok = true;
								} catch (Exception exc2) {}
							}
						}
						if (ok && (name.length() < 8 || name.indexOf(" ") > 0)) name = "NGC " + name;
					}
				}
			} catch (Exception exc) {}

			LocationElement loc = (LocationElement) obj[3];
			if (name.indexOf(obj_name) >= 0 || messier.indexOf(obj_name) >= 0 || com.indexOf(obj_name) >= 0 ||
					(s == null && obj_name.indexOf(name) >= 0)) {
				s = new LocationElement(loc.getLongitude(), loc.getLatitude(), 1.0);
			}
			if (name.equals(obj_name) || messier.equals(obj_name) || com.equals(obj_name))
				break;
		}

		return s;
	}

	/**
	 * Returns all deep sky objects brighter than certain magnitude.
	 *
	 * @param mag Limiting magnitude, or -1 to return all.
	 * @return An array containing the following fields separated by a comma:
	 * Name, second name, RA, DEC (rad), mag, max size, min size (deg), type, PA, comments.
	 * @throws JPARSECException If the method fails.
	 */
	public static String[] searchDeepSkyObjectJ2000(double mag)
			throws JPARSECException
	{
		Object[] objs = populate(false);

		ArrayList<String> list = new ArrayList<String>();
		String sep = ",";
		for (int i = 0; i < objs.length; i++)
		{
			Object[] obj = (Object[]) objs[i];
			String messier = (String) obj[1];
			String name = (String) obj[0];
			String comments = (String) obj[7];
			int pp = comments.indexOf("Popular name:");
			if (pp>=0) comments = comments.substring(pp+14).trim();
			float mg = (Float) obj[4];
			if (mag != -1 && mg > mag) break;
			float s[] = (float[]) obj[5];
			int type = Math.abs((Byte) obj[2]);
			float pa = (Float) obj[6];

			if (name.startsWith("I.")) name = DataSet.replaceAll(name, "I.", "IC ", true);
			LocationElement loc = (LocationElement) obj[3];

			String line = name + sep + messier + sep + loc.getLongitude() + sep + loc.getLatitude() + sep + mg +
					sep + s[0] + sep + s[1] + sep + type + sep + pa + sep + comments;
			list.add(line);
		}

		return DataSet.arrayListToStringArray(list);
	}

	private static Object[] populate(boolean android) throws JPARSECException {
		Object out = DataBase.getData("objectsJ2000", null, true);

		if (out == null) {
			ArrayList<Object[]> outObj = new ArrayList<Object[]>();
			String nodraw[] = new String[] {"LMC", "292", "1976", "1982", "6995", "6979", "I.1287", "I.4601", "6514", "6526", "3324", "896"};

			// Connect to the file
			InputStream is = null;
			BufferedReader dis = null;
			try
			{
				is = RenderSky.class.getClassLoader().getResourceAsStream(FileIO.DATA_SKY_DIRECTORY + "objects.txt");
				dis = new BufferedReader(new InputStreamReader(is));
				String line;
				while ((line = dis.readLine()) != null)
				{
					String mag = FileIO.getField(5, line, " ", true);
					float magnitude = Float.parseFloat(mag);
					//if (magnitude >= 14) break;

					String type = FileIO.getField(2, line, " ", true);
					int tt = DataSet.getIndex(types, type);
					// Don't want to see duplicate objects, stars, inexistents, ...
					if (tt > 7) continue;

					String name = FileIO.getField(1, line, " ", true);
					String ra = FileIO.getField(3, line, " ", true);
					String dec = FileIO.getField(4, line, " ", true);
					String com = FileIO.getRestAfterField(8, line, " ", true);

					LocationElement loc = new LocationElement((float)(Double.parseDouble(ra)/Constant.RAD_TO_HOUR), (float)(Double.parseDouble(dec)*Constant.DEG_TO_RAD), 1.0f);
//					if (jd != Constant.J2000)
//						loc = LocationElement.parseRectangularCoordinates(Precession.precess(Constant.J2000, jd,
//								LocationElement.parseLocationElement(loc), EphemerisElement.REDUCTION_METHOD.IAU_2006));

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
						if (messier.equals("")) {
							int cal = com.indexOf("CALDWELL");
							if (cal >= 0) {
								messier = com.substring(cal);
								int end = messier.indexOf(";");
								int end2 = messier.indexOf(",");
								if (end > 0) messier = messier.substring(0, end);
								end = messier.length();
								if (end2 > 0 && end2 < end) messier = messier.substring(0, end2);
							}
						}

						if (android && messier.equals("")) {
							if (magnitude < 100 && magnitude > 12.5) continue;
						}

						String max = FileIO.getField(6, line, " ", true);
						String min = FileIO.getField(7, line, " ", true);
						String pa = FileIO.getField(8, line, " ", true);

						float maxSize = Float.parseFloat(max), minSize = Float.parseFloat(min);
						if (tt == 6 && maxSize == 0.0) maxSize = minSize = 0.5f/60.0f;

						if (nodraw.length > 0) {
							int nodrawIndex = DataSet.getIndex(nodraw, name);
							if (nodrawIndex >=0) {
								tt = -tt;
								nodraw = DataSet.eliminateRowFromTable(nodraw, nodrawIndex+1);
							}
						}
						float paf = -1;
						try {
							if (!pa.equals("-") && !pa.equals(""))
								paf = (float) (Float.parseFloat(pa) * Constant.DEG_TO_RAD);
						} catch (Exception exc) {}
						loc.set(DataSet.toFloatArray(loc.get())); // Reduce memory use
						outObj.add(new Object[] {name, messier, (byte) tt, loc, magnitude,
								new float[] {maxSize, minSize}, paf, com});
				}

				// Close file
				dis.close();
				is.close();
				is = null;
			} catch (Exception e2)
			{
				try {
					if (dis != null) dis.close();
					if (is != null) is.close();
					is = null;
				} catch (Exception exc) {}
				throw new JPARSECException(
						"error while reading objects file", e2);
			}
			Object outO[] = outObj.toArray();
			DataBase.addData("objectsJ2000", null, outO, true);
			return outO;
		}
		return (Object[]) out;
	}


	/**
	 * Returns the equatorial position of the current rendering direction.
	 * @return Equatorial direction.
	 */
	public LocationElement getEquatorialPositionOfRendering() {
		return loc0Date.clone();
	}

	/**
	 * Returns all stars around a given sky location between magnitudes 9.5 and a given limiting
	 * magnitude, using UCAC4 catalog.
	 * @param loc0J2000 The J2000 equatorial location of the position.
	 * @param fieldDeg The field of view in degrees.
	 * @param maglim The limiting magnitude, between 10 and 16.
	 * @param timeout Timeout for the query in s.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param peph Ephemeris object.
	 * @return The list of stars, including for each string the apparent RA, DEC, magnitude, and UCAC4 name. Column
	 * separator is a blank space. RA and DEC are given in radians. Stars are sorted by magnitude.
	 * @throws JPARSECException If an error occurs.
	 */
	public static ArrayList<String> queryUCAC(LocationElement loc0J2000, double fieldDeg, double maglim,
			int timeout, TimeElement time, ObserverElement obs, EphemerisElement peph) throws JPARSECException {
		return queryUCAC(loc0J2000, fieldDeg, maglim, timeout, time, obs, peph, true);
	}
	
	/**
	 * Returns all stars around a given sky location between magnitudes 9.5 and a given limiting
	 * magnitude, using UCAC4 catalog.
	 * @param loc0J2000 The J2000 equatorial location of the position.
	 * @param fieldDeg The field of view in degrees.
	 * @param maglim The limiting magnitude, between 10 and 16.
	 * @param timeout Timeout for the query in s.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param peph Ephemeris object.
	 * @param outApparent True to return output RA/DEC as apparent coordinates, false for J2000 output
	 * @return The list of stars, including for each string the RA, DEC, magnitude, and UCAC4 name. Column
	 * separator is a blank space. RA and DEC are given in radians. Stars are sorted by magnitude.
	 * @throws JPARSECException If an error occurs.
	 */
	public static ArrayList<String> queryUCAC(LocationElement loc0J2000, double fieldDeg, double maglim,
			int timeout, TimeElement time, ObserverElement obs, EphemerisElement peph, boolean outApparent) throws JPARSECException {
		ArrayList<String> list = new ArrayList<String>();
		String sep = " ";

		double equinox = Constant.J2000;
		if (time != null) {
			double jd = TimeScale.getJD(time, obs, peph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
			equinox = peph.equinox;
			if (equinox == EphemerisElement.EQUINOX_OF_DATE)
				equinox = jd;
		}

		LocationElement locEq;
 		String ra0 = Functions.formatRA(loc0J2000.getLongitude());
		String dec0 = Functions.formatDEC(loc0J2000.getLatitude());
		String name = Functions.getHoursFromFormattedRA(ra0)+":"+Functions.getMinutesFromFormattedRA(ra0)+":"+
			Functions.getSecondsFromFormattedRA(ra0) + Functions.getDegreesFromFormattedDEC(dec0) + ":"+
			Functions.getMinutesFromFormattedDEC(dec0)+ ":"+Functions.getSecondsFromFormattedDEC(dec0);
		name = name.replaceAll(",", ".");

		String query = "http://vizier.u-strasbg.fr/viz-bin/VizieR?-source=UCAC4&-c="+name+"&-c.rd="+fieldDeg+"&-mime=ascii&-out.form=csv&-oc.form=dec&-out.max=20000&-out=RAJ2000,DEJ2000,f.mag,a.mag,pmRA,pmDE,UCAC4&f.mag=9.5.."+maglim;
		String datas = "";
		try {
			datas = GeneralQuery.query(query, timeout*1000);
		} catch (Exception exc) {
			JPARSECException.addWarning("cannot query UCAC4 catalog, timeout expired ("+timeout+"s). Please disable UCAC4 queries if you don't have Internet connection available.");
			return null;
		}
		if (!datas.isEmpty()) {
			int init = datas.indexOf(FileIO.getLineSeparator()+"<HR>"+FileIO.getLineSeparator())+6;
			int end = datas.indexOf(FileIO.getLineSeparator()+"<HR></PRE>"+FileIO.getLineSeparator());
			if (init >= 0 && end >= 0 && init < end) {
				datas = datas.substring(init, end);
				String file[] = DataSet.toStringArray(datas, FileIO.getLineSeparator());
				int nstars = file.length;

		    	String ra = "", dec = "", pmRA = "", pmDEC = "", mag1 = "", mag2 = "";
		    	double magV = -100.0;
		    	double cte = Constant.RAD_TO_ARCSEC * 1000.0;
				EphemerisElement eph = null;
				double baryc[] = null;
				if (outApparent) {
					eph = peph.clone();
					eph.algorithm = EphemerisElement.ALGORITHM.STAR;
					baryc = Ephem.eclipticToEquatorial(PlanetEphem.getGeocentricPosition(equinox, TARGET.Solar_System_Barycenter, 0.0, false, obs), Constant.J2000, peph);
				}

				boolean calc = false;
			    for (int i=0; i<nstars; i++)
			    {
			    	magV = -100.0;
			    	String data = file[i];
			    	init = data.indexOf(">");
			    	if (init > 0) {
			    		pmRA = pmDEC = mag2 = "";
				    	data = data.substring(init+1);

				    	init = data.indexOf("<");
				    	end = data.indexOf(">");
				    	if (init >=0 && end > 0) {
				    		do {
				    			String d = data.substring(0, init) + data.substring(end+1);
				    			data = d;
						    	init = data.indexOf("<");
						    	end = data.indexOf(">");
				    		} while (init >=0 && end > 0);
				    	}
				    	ra = data.substring(2, 12);
				    	dec = data.substring(15, 25);
				    	mag1 = data.substring(28, 33).trim();
				    	if (data.length()>40) mag2 = data.substring(36, 41).trim();
				    	if (data.length()>50) {
				    		pmRA = data.substring(47, 51).trim();
					    	pmDEC = data.substring(58, 62).trim();
				    	}
				    	
				    	String name2print = "UCAC ?";
				    	if (data.length() > 64) {
				    		name2print = data.substring(63).trim();
				    		String ucac = FileIO.getField(1, name2print, "  ", true);
				    		name2print = "UCAC4 "+ucac;
				    	}
				    	
				    	if (mag2.isEmpty()) {
				    		if (!mag1.isEmpty()) magV = Double.parseDouble(mag1);
				    	} else {
				    		magV = Double.parseDouble(mag2); // mag2 'more robust'
				    	}

				    	if (magV != -100 && !ra.isEmpty() && !dec.isEmpty())
				    	{
				    		LocationElement loc = new LocationElement(
				    				Double.parseDouble(ra) * Constant.DEG_TO_RAD,
				    				Double.parseDouble(dec) * Constant.DEG_TO_RAD,
				    				1.0);
				    		if (outApparent) {
								StarElement star = new StarElement();
					    		star.rightAscension = loc.getLongitude();
					    		star.declination = loc.getLatitude();
					    		star.parallax = 1000.0 / 500.0;
					    		star.equinox = Constant.J2000;
					    		star.frame = EphemerisElement.FRAME.FK5;
					    		star.magnitude = (float) magV;
					    		star.properMotionRadialV = 0.0f;
					    		if (pmRA.isEmpty()) pmRA = "0";
					    		if (pmDEC.isEmpty()) pmDEC = "0";
					    		star.properMotionDEC = (float) (Double.parseDouble(pmDEC) / cte);
					    		star.properMotionRA = (float) (Double.parseDouble(pmRA) / (cte * FastMath.cos(star.declination)));
					    		StarEphemElement ephem = new StarEphemElement();
								if (calc || (star.properMotionDEC != 0.0 || star.properMotionRA != 0.0))
								{
						    		ephem = StarEphem.starEphemeris(time, obs, eph, star, false);
								} else
								{
									// Correct for aberration, precession, and nutation
									if (peph.ephemType == COORDINATES_TYPE.APPARENT) {
										loc.setRadius(1000 * Constant.RAD_TO_ARCSEC); // 1000 pc
										double light_time = loc.getRadius() * Constant.LIGHT_TIME_DAYS_PER_AU;
										double r[] = Ephem.aberration(loc.getRectangularCoordinates(), baryc, light_time);
	
										r = Precession.precessFromJ2000(equinox, r, peph);
										locEq = LocationElement.parseRectangularCoordinates(Nutation.nutateInEquatorialCoordinates(equinox, peph, r, true));
									} else {
										locEq = LocationElement.parseRectangularCoordinates(Precession.precessFromJ2000(equinox,
												LocationElement.parseLocationElement(loc), peph));
									}
	
									ephem.rightAscension = locEq.getLongitude();
									ephem.declination = locEq.getLatitude();
									ephem.distance = star.getDistance();
									ephem.magnitude = star.magnitude;
								}
	
								list.add(""+ephem.rightAscension+sep+ephem.declination+sep+ephem.magnitude+sep+name2print);
				    		} else {
								list.add(""+loc.getLongitude()+sep+loc.getLatitude()+sep+(float)magV+sep+name2print);				    			
				    		}
				    	}
			    	}
			    }
			}
		}

		// Sort by magnitude
		String data[] = DataSet.arrayListToStringArray(list);
		double val[] = DataSet.toDoubleValues(DataSet.extractColumnFromTable(data, sep, 2));
		data = DataSet.sortInCrescent(data, val);
		return DataSet.stringArraytoArrayList(data);
	}

	private ReadFile readStars(Projection projection, boolean pre) throws JPARSECException {
		if (!render.drawStars && render.drawConstellationContours == CONSTELLATION_CONTOUR.NONE)
			return null;

		/*
		 * Read stars using Sky2000 catalog. Read stars are those within the
		 * field of view, taking into account precession if necessary
		 */
			ReadFile re_star = new ReadFile();
			re_star.setThreadName(threadID);
			if (pre)
				re_star.setThreadName(threadID+"_pre");
			// Set here a fake 'path' since an identifier is used in ReadFile class to track the objects read
			// and the default name is used to hold StarElement objects, not StarData objects. This also ensures
			// using the same one identifier to hold stars in all ranges of magnitudes
			re_star.setPath("renderSkyStars");
			re_star.setFormat(ReadFile.FORMAT.JPARSEC_SKY2000);
			re_star.setReadElements(null);

			double jd = projection.jd;
			double equinox = projection.eph.equinox;
			if (equinox == EphemerisElement.EQUINOX_OF_DATE)
				equinox = jd;
			baryc = Ephem.eclipticToEquatorial(PlanetEphem.getGeocentricPosition(equinox, TARGET.Solar_System_Barycenter, 0.0, false, projection.obs), Constant.J2000, projection.eph);

			LocationElement zenith = null;
			if (!render.drawSkyBelowHorizon && projection.obs.getMotherBody() != TARGET.NOT_A_PLANET) {
				zenith = projection.getEquatorialPositionOfZenith();
				if (projection.obs.getMotherBody() != TARGET.EARTH && projection.obs.getMotherBody() != TARGET.NOT_A_PLANET) {
					zenith = projection.getPositionFromEarth(zenith, this.fast);
				}
				if (equinox != EphemerisElement.EQUINOX_J2000)
				{
					double pos[] = LocationElement.parseLocationElement(zenith);
					pos = precessToJ2000(equinox, pos, projection.eph);
					zenith = LocationElement.parseRectangularCoordinates(pos);
				}
			}

			if (render.drawStarsLimitingMagnitude <= 6.5) {
				readFileOfStars(jd, equinox, projection, zenith, render.drawStarsLimitingMagnitude, re_star, FileIO.DATA_STARS_SKY2000_DIRECTORY + "JPARSEC_Sky2000.txt", g.renderingToExternalGraphics(), g.renderingToAndroid());
			} else {
				readFileOfStars(jd, equinox, projection, zenith, 6.5, re_star, FileIO.DATA_STARS_SKY2000_DIRECTORY + "JPARSEC_Sky2000.txt", g.renderingToExternalGraphics(), g.renderingToAndroid());
				readFileOfStars(jd, equinox, projection, zenith, render.drawStarsLimitingMagnitude, re_star, FileIO.DATA_STARS_SKY2000_DIRECTORY + "JPARSEC_Sky2000_plus.txt", g.renderingToExternalGraphics(), g.renderingToAndroid());
			}
			starElem = null;
			if (drawAll) nstars = re_star.getNumberOfObjects();
			return re_star;
	}

	private ArrayList<Object> readObjects(Projection projection, boolean forceRead) throws JPARSECException {
		ArrayList<Object> objects = null;

		if (!render.drawDeepSkyObjects) return objects;

		Object o = null;
		if (!forceRead) {
			if (db_objects >= 0) {
				o = DataBase.getData(db_objects);
			} else {
				o = DataBase.getData("objects", threadID, true);
			}
			if (o != null) objects = new ArrayList<Object>(Arrays.asList((Object[]) o));
		}
		if (objects == null || (lastObjMaglim < Math.abs(render.drawObjectsLimitingMagnitude) && lastObjMaglim != -1)) {
			objects = new ArrayList<Object>();
			ArrayList<Object> objectsJ2000 = null;
			Object o2000 = DataBase.getData("objectsJ2000", null, true);
			if (o2000 == null) {
				objectsJ2000 = new ArrayList<Object>(Arrays.asList(populate(g.renderingToAndroid() && render.drawObjectsLimitingMagnitude <= 12.5)));
			} else {
				objectsJ2000 = new ArrayList<Object>(Arrays.asList((Object[]) o2000));
			}
			double jd = projection.jd;
			double equinox = projection.eph.equinox;
			if (equinox == EphemerisElement.EQUINOX_OF_DATE)
				equinox = jd;

			double[] baryc = Ephem.eclipticToEquatorial(PlanetEphem.getGeocentricPosition(equinox, TARGET.Solar_System_Barycenter, 0.0, false, projection.obs), Constant.J2000, projection.eph);
			boolean extinction = false;
			if (projection.obs.getMotherBody() == TARGET.EARTH && projection.eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT && projection.eph.correctForExtinction)
				extinction = true;
			for (int index=0; index<objectsJ2000.size(); index++)
			{
				Object obj[] = (Object[]) objectsJ2000.get(index);
				//name, messier, tt, locJ2000, (float)magnitude,
				//new float[] {(float) maxSize, (float) minSize}, paf, com});

				float magnitude = (Float) obj[4];

				boolean outsideMag = !drawAll && (magnitude > Math.abs(render.drawObjectsLimitingMagnitude));
				if (outsideMag && !render.drawDeepSkyObjectsAllMessierAndCaldwell) break;

				String messier = (String) obj[1];
				if (messier.equals("")) {
					if (magnitude < 100 && outsideMag) continue;
					//if (magnitude < 100 && outsideMag && render.drawDeepSkyObjectsAllMessierAndCaldwell) continue;
					//if (magnitude < 100 && outsideMag) continue;
				}

				LocationElement loc = (LocationElement) obj[3];
				if (equinox != Constant.J2000) {
					// Correct for aberration, precession, and nutation
					if (projection.eph.ephemType == COORDINATES_TYPE.APPARENT) {
						loc.setRadius(1000 * Constant.RAD_TO_ARCSEC); // 1000 pc
						double light_time = loc.getRadius() * Constant.LIGHT_TIME_DAYS_PER_AU;
						double r[] = Ephem.aberration(loc.getRectangularCoordinates(), baryc, light_time);

						r = precessFromJ2000(equinox, r, projection.eph);
						loc = LocationElement.parseRectangularCoordinates(nutateInEquatorialCoordinates(equinox, projection.eph, r, true));
					} else {
						loc = LocationElement.parseRectangularCoordinates(precessFromJ2000(equinox,
								LocationElement.parseLocationElement(loc), projection.eph));
					}
				}
				if (projection.obs.getMotherBody() != TARGET.EARTH && projection.obs.getMotherBody() != TARGET.NOT_A_PLANET) {
					loc = projection.getPositionFromBody(loc, this.fast);
				}

				LocationElement eq = loc;
				float ss[] = (float[]) obj[5];
				loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, true, false, ss[0]/pixels_per_radian);
				if (drawAll) {
					float size0 = (ss[0] * pixels_per_degree) + 1.0f;
					float[] pos0 = projection.projectPosition(loc, 0, false);
					if (pos0 != null && !this.isInTheScreen((int)pos0[0], (int)pos0[1], (int)(size0 * 2))) pos0 = null;
					if (pos0 == null) loc = null;
				}
				if (loc != null) {
					loc.set(DataSet.toFloatArray(loc.get())); // Reduce memory use
					if (extinction && magnitude < 100) {
						magnitude = (float) correctForExtinction(eq, magnitude);
						objects.add(new Object[] {index, loc, magnitude});
					} else {
						objects.add(new Object[] {index, loc});
					}
				}
			}

			if (render.getNumberOfExternalCatalogs() > 0) {
				int rs = SkyRenderElement.getTotalNumberOfExternalCatalogs();
				for (int i=0; i<rs; i++) {
					if (render.drawExternalCatalogs == null || render.drawExternalCatalogs[i]) {
						String id = "RenderSkyExternalCatalog"+i;
						if (render.externalCatalogAvailable(id)) {
							o = DataBase.getDataForAnyThread(id, true);
							if (o == null)
								o = DataBase.getDataForAnyThread(id, false);
							if (o != null) {
								ArrayList<Object> externalObj = new ArrayList<Object>(Arrays.asList(((Object[]) o)));
								int rs2 = externalObj.size();
								for (int j = 0; j<rs2; j++) {
									Object data[] = (Object[]) externalObj.get(j);
									LocationElement loc = ((LocationElement) data[3]);
									if (loc == null) continue;
									loc = loc.clone();
									data = data.clone();
									if (equinox != Constant.J2000) {
										// Correct for aberration, precession, and nutation
										if (projection.eph.ephemType == COORDINATES_TYPE.APPARENT) {
											loc.setRadius(1000 * Constant.RAD_TO_ARCSEC); // 1000 pc
											double light_time = loc.getRadius() * Constant.LIGHT_TIME_DAYS_PER_AU;
											double r[] = Ephem.aberration(loc.getRectangularCoordinates(), baryc, light_time);

											r = precessFromJ2000(equinox, r, projection.eph);
											loc = LocationElement.parseRectangularCoordinates(nutateInEquatorialCoordinates(equinox, projection.eph, r, true));
										} else {
											loc = LocationElement.parseRectangularCoordinates(precessFromJ2000(equinox,
													LocationElement.parseLocationElement(loc), projection.eph));
										}
									}
									if (projection.obs.getMotherBody() != TARGET.EARTH && projection.obs.getMotherBody() != TARGET.NOT_A_PLANET) {
										loc = projection.getPositionFromBody(loc, this.fast);
									}
									loc = projection.getApparentLocationInSelectedCoordinateSystem(loc, true, false, 0);
									if (drawAll) {
										float[] pos0 = projection.projectPosition(loc, 0, false);
										if (pos0 != null && !this.isInTheScreen((int)pos0[0], (int)pos0[1], 0)) pos0 = null;
										if (pos0 == null) loc = null;
									}
									data[3] = loc;
									if (loc != null) objects.add(data);
								}
							}
						}
					}
				}
			}

			lastObjMaglim = Math.abs(render.drawObjectsLimitingMagnitude);
			if (!forceRead) {
				DataBase.addData("objects", threadID, objects.toArray(), true);
				db_objects = DataBase.getIndex("objects", threadID);
			}
		}
		return objects;
	}
}
