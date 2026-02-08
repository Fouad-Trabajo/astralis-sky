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

import java.util.ArrayList;

import jparsec.astronomy.Difraction;
import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.EphemerisElement.ALGORITHM;
import jparsec.ephem.Functions;
import jparsec.ephem.Target;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.event.MainEvents;
import jparsec.ephem.event.MoonEvent;
import jparsec.ephem.event.MoonEvent.EVENT_DEFINITION;
import jparsec.ephem.moons.MoonEphem;
import jparsec.ephem.moons.MoonEphemElement;
import jparsec.ephem.planets.EphemElement;
import jparsec.ephem.planets.PlanetEphem;
import jparsec.graph.DataSet;
import jparsec.graph.chartRendering.Graphics.ANAGLYPH_COLOR_MODE;
import jparsec.graph.chartRendering.Graphics.FONT;
import jparsec.graph.chartRendering.SatelliteRenderElement.PLANET_MAP;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.io.UnixSpecialCharacter;
import jparsec.math.Constant;
import jparsec.math.FastMath;
import jparsec.observer.City;
import jparsec.observer.CityElement;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.util.*;

/**
 * A class for planetary rendering.
 * <P>
 * This class performs planetary renderings using models taken from the textures
 * directory. The modelization is quite complete, taking into account the
 * position of the observer, the object, and the Sun in the space. Illumination
 * is applied to create a fotorealistic image, and the planetary rings are
 * modeled taking into account their colors, transparency, and angle of
 * illumination. The result can be improved just a little (opacity inside rings,
 * elevation effects in surfaces, ...), but it will not differ so much from the
 * results obtained with professional rendering tools.
 * <P>
 * In addition, it is possible to simulate the objects as they would be observed
 * through certain telescope, by modelizing also the difraction effects. This
 * modelization takes into account possible chromatic aberration, but it is not
 * fully accurate, so it's serves only as an approximation supposing a perfect
 * aligned telescope and a very clear atmosphere.
 * <P>
 * Models comes from Bj&ouml;rn J&oacute;nsson &amp; David Seal <A target="_blank" 
 * href="http://maps.jpl.nasa.gov">http://maps.jpl.nasa.gov</A>. The model for
 * Mercury is from James Hastings <A target="_blank" 
 * href="http://gw.marketingden.com/planets/planets.html">http://gw.marketingden.com/planets/planets.html</A>.
 * Some models for the moons comes from Celestia software.
 * <P>
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class RenderPlanet
{
	/** Minimum radius in pixels of a planet to draw it using textures. */
	public static float minScale = 1.5f;
	
	/** Selects the map for the Earth. */
	public PLANET_MAP earthMap = null;
	/** True to show day and night. */
	public boolean showDayAndNight = true;

	boolean renderingSky = false;
	String motherBody = null;
	double northAngle = 0;
	double renderingSkyMagLim;
	boolean useSkySatPos = false;
	ArrayList<float[]> skySatPos;
	double offsetInLongitudeOfJupiterGRS = 0.0;
	int offsetInLongitudeOfJupiterGRS_system = 2;
	int texture_step;
	int hugeFactor = 0;
	float upperLimbFactor = 1f, lowerLimbFactor = 1f, cenitAngle = 0;

	private float satX[] = new float[0];
	private float satY[] = new float[0];
	private float satZ[] = new float[0];
	private float satR[] = new float[0];
	private int satID[] = new int[0];
	private float satDist[] = new float[0];
	//private static String imgs[] = new String[0];
	//private static ArrayList<Object> images = new ArrayList<Object>();
	// Planetary ring distances in km
	private static int ring_radius[][] = new int[][] {
		new int [] {74510, 92000, 117500, 122200, 136800, 139400, 140390},
		new int [] {38000, 44720, 45665, 48300, 50020, 51150},
		new int [] {42900, 53000, 57200, 62930}
	};

	static PlanetRenderElement lastRenderElement;
	private static Graphics lastRender;
	private static float lastPosX, lastPosY;
	private static float lastScale;


	/**
	 * Render object that holds render parameters.
	 */
	public PlanetRenderElement render = new PlanetRenderElement();

	/**
	 * Position where the planet will be rendered in pixels.
	 */
	public float xPosition, yPosition;
	/** True to simulate Earth illuminating Moon's dark side. */
	public boolean illuminateMoonByEarth = true;

	private float planet_size = 0, scaleFactor = 1.0f;
	private RenderSky renderSky;
	private static double[] isatOrdered = null;
	static boolean repaint = false;

	/**
	 * This variable holds how many times the size of the rendering window
	 * is increased to calculate the output image (reduced by the same amount
	 * before showing) to obtain a smoothed output image. The more the number
	 * the more the quality of the output, but more computing time. This
	 * factor is only used if {@linkplain PlanetRenderElement#highQuality} is
	 * set to true. Default value is 1.5. 2 is good to improve quality, and 1
	 * to disable high quality mode. The rest of possible values are not so useful.
	 * In particular, values below 1 are possible to produce fast renderings, but
	 * quality will be highly degraded.
	 */
	public static float MAXIMUM_TEXTURE_QUALITY_FACTOR = 1.5f;
	/**
	 * True to allow using an spline technique (in case the instance of
	 * {@linkplain Graphics} supports it) to resize the image in high
	 * quality mode. Default value is false, which gives better (besides
	 * much faster) results when the background is white, for instance. It is
	 * very rare the cases when false gives better output, but it is useful
	 * to improve a lot performance in big renderings, while reducing quality
	 * just a bit. Set to true for high quality (and slow) resizing.
	 */
	public static boolean ALLOW_SPLINE_RESIZING = false;
	/**
	 * This flag allows to force the background color to white before applying
	 * some scaling operations to the rendering, allowing for better output
	 * quality in case the desired background is white. In this case, set this
	 * flag to true and the background color to black. This has no effect in
	 * case anaglyph mode is selected.
	 */
	public static boolean FORCE_WHITE_BACKGROUND = false;
	/**
	 * For fields of view larger than 1 deg the high quality mode is automatically
	 * disabled to prevent artifacts when rendering the sky. These artifacts occurs
	 * when dragging a planet with other lines (constellations for instance) on the
	 * screen, due to field rotation. In case you are rendering one frame, or one
	 * after another (without repainting fast a planet and dragging it) you can
	 * set this flag to true to allow high quality rendering on large fields of view,
	 * useful to create movies of planets as visible from their satellites. Default
	 * value for this flag is false.
	 */
	public static boolean FORCE_HIGHT_QUALITY = false;

	static double geodeticToGeocentric(double equatorial_radius, double polar_radius, double lat0) throws JPARSECException
	{
		// Get ellipsoid
		double Earth_flatenning = (equatorial_radius / (equatorial_radius - polar_radius));

		// Apply calculations
		double flat = Earth_flatenning;
		double co = Math.cos(lat0);
		double si = Math.sin(lat0);
		double fl = 1.0 - 1.0 / flat;
		fl = fl * fl;
		si = si * si;
		double u = 1.0 / Math.sqrt(co * co + fl * si);
		double a = equatorial_radius * u * 1000.0;
		double b = equatorial_radius * fl * u * 1000.0;
		double rho = Math.sqrt(a * a * co * co + b * b * si);
		double geo_lat = Math.acos(a * co / rho);
		if (lat0 < 0.0)
			geo_lat = -geo_lat;
		rho = rho / (1000.0 * equatorial_radius);

		return geo_lat;
	}

	static double geocentricToGeodetic(double equatorial_radius, double polar_radius, double lat0) throws JPARSECException
	{
		// Get ellipsoid
		double Earth_flatenning = (equatorial_radius / (equatorial_radius - polar_radius));

		// Apply calculations
		double flat = Earth_flatenning;
		double fl = 1.0 - 1.0 / flat;
		fl = fl * fl;
		double rho = 1.0;
		double lat = Math.atan(Math.tan(lat0) / fl);
		double co = Math.cos(lat);
		double si = Math.sin(lat);
		double u = 1.0 / Math.sqrt(co * co + fl * si * si);
		double a = equatorial_radius * u * 1000.0;
		double b = equatorial_radius * fl * u * 1000.0;

		rho = rho * (1000.0 * equatorial_radius);

		double coef_A = co * co + si * si;
		double coef_B = 2.0 * a * co * co + 2.0 * b * si * si;
		double coef_C = a * a * co * co + b * b * si * si - rho * rho;

		double alt = (-coef_B + Math.sqrt(coef_B * coef_B - 4.0 * coef_A * coef_C)) / (2.0 * coef_A);
		lat = Math.acos(rho * Math.cos(lat0) / (a + alt));
		if (lat0 < 0.0) lat = -lat;

		return lat;
	}

	void refractionCorrection(EphemerisElement eph, ObserverElement obs, float angr) throws JPARSECException {
		// Rude correction for refraction in certain cases
		// Planets are also enabled since satellites are positioned using their apparent elevations.
		// This method is called from RenderSky for sky rendering, but not for planetary rendering alone
//		if (eph.targetBody == TARGET.SUN || eph.targetBody == TARGET.Moon) {
			double geoElev0 = Ephem.getGeometricElevation(eph, obs, render.ephem.elevation);
			double geoElevp = geoElev0 + render.ephem.angularRadius;
			double geoElevm = geoElev0 - render.ephem.angularRadius;
			double appElevp = Ephem.getApparentElevation(eph, obs, geoElevp, 10);
			double appElevm = Ephem.getApparentElevation(eph, obs, geoElevm, 10);
			double dp = (appElevp - render.ephem.elevation);
			double dm = -(appElevm - render.ephem.elevation);
			upperLimbFactor = (float) (dp / render.ephem.angularRadius);
			lowerLimbFactor = (float) (dm / render.ephem.angularRadius);
			cenitAngle = angr;
//		} else {
//			upperLimbFactor = 1f;
//			lowerLimbFactor = 1f;
//		}
	}

	private Object[] readRingTexture(Graphics g, String s, int r) {
		boolean unlit = false;
		if (FastMath.sign(render.ephem.positionAngleOfPole) != FastMath.sign(render.ephem.subsolarLatitude)) unlit = true;

		Object img = null, rings2 = null;
		boolean newModel = true;
		int desiredW = (int)((r/100.0)+0.5)*100;
		if (desiredW < 100) desiredW = 100;
		g.disableInversion();

		if (render.target == TARGET.SATURN) {
			if (newModel) {
				Object back = null, forw = null;
				double gamma = 0.7;
				int offset = 0;
				if (unlit) {
					String Iback = FileIO.DATA_TEXTURES_DIRECTORY + s + "_unlitside.png";
					back = g.getImage(Iback);
					if (!g.renderingToAndroid() && !DataBase.dataExists(Iback, true)) g.addToDataBase(g.cloneImage(back), Iback, 0);
					gamma = 0.7;
					offset = -128;
				} else {
					String Iback = FileIO.DATA_TEXTURES_DIRECTORY + s + "_backscattered.png";
					String Iforw = FileIO.DATA_TEXTURES_DIRECTORY + s + "_forwardscattered.png";
					back = g.getImage(Iback);
					forw = g.getImage(Iforw);
					if (!g.renderingToAndroid() && !DataBase.dataExists(Iback, true)) g.addToDataBase(g.cloneImage(back), Iback, 0);
					if (!g.renderingToAndroid() && !DataBase.dataExists(Iforw, true)) g.addToDataBase(g.cloneImage(forw), Iforw, 0);
					forw = g.getScaledImage(forw, desiredW, 33, false, ALLOW_SPLINE_RESIZING);
				}
				String Icolor = FileIO.DATA_TEXTURES_DIRECTORY + s + "_color.png";
				String Irings2 = FileIO.DATA_TEXTURES_DIRECTORY + s + "_transparency.png";
				Object color = g.getImage(Icolor);
				rings2 = g.getImage(Irings2);
				if (!g.renderingToAndroid() && !DataBase.dataExists(Icolor, true)) g.addToDataBase(g.cloneImage(color), Icolor, 0);
				if (!g.renderingToAndroid() && !DataBase.dataExists(Irings2, true)) {
					rings2 = g.getColorInvertedImage(rings2);
					g.addToDataBase(g.cloneImage(rings2), Irings2, 0);
				}
				
				back = g.getScaledImage(back, desiredW, 33, false, ALLOW_SPLINE_RESIZING);
				color = g.getScaledImage(color, desiredW, 33, false, ALLOW_SPLINE_RESIZING);
				rings2 = g.getScaledImage(rings2, desiredW, 33, false, ALLOW_SPLINE_RESIZING);

				img = g.cloneImage(back);
				int size[] = g.getSize(img);
				for (int x=0;x<size[0]; x++) {
					int ca = g.getRGB(color, x, 0);
					double cr = g.getRed(ca) / 255.0;
					double cg = g.getGreen(ca) / 255.0;
					double cb = g.getBlue(ca) / 255.0;
					int ba = g.getRGB(back, x, 30);
					double cba = g.getGreen(ba) / 255.0;
					double bag = (Math.pow(cba, gamma) * 255.0);

					// Correct for phase angle
					if (forw != null) {
						int fa = g.getRGB(forw, x, 30);
						double cfa = g.getGreen(fa) / 255.0;
						double fag = (Math.pow(cfa, gamma) * 255.0);
						bag = bag + (fag - bag) * Math.min(1.0, Math.abs(render.ephem.phaseAngle/0.78)/Math.PI); // 140 deg should be maximum value according to Bjorn Johnsson
					}

					int ocr = offset + (int)(cr*bag);
					int ocg = offset + (int)(cg*bag);
					int ocb = offset + (int)(cb*bag);
					if (ocr > 255) ocr = 255;
					if (ocg > 255) ocg = 255;
					if (ocb > 255) ocb = 255;
					if (ocr < 0) ocr = 0;
					if (ocg < 0) ocg = 0;
					if (ocb < 0) ocb = 0;
					g.setColor(ocr, ocg, ocb, 255);
					int c = g.getColor();
					for (int y=0;y<size[1]; y++) {
						g.setRGB(img, x, y, c);
					}

					double tr = (0xff & (g.getRGB(rings2,x, 30) >> 8)) / 255.0;
					int transp = (int)(Math.pow(tr, gamma) * 255.0);
					transp = 192-((128-transp)*4)/3;
					if (transp > 255) transp = 255;
					if (transp < 0) transp = 0;
					g.setColor(transp, transp, transp, 255);
					c = g.getColor();
					for (int y=0;y<size[1]; y++) {
						g.setRGB(rings2, x, y, c);
					}
				}
				g.enableInversion();
				return new Object[] {img, rings2};
			} else {
				String Iimg = FileIO.DATA_TEXTURES_DIRECTORY + s + "1.jpg";
				String Irings2 = FileIO.DATA_TEXTURES_DIRECTORY + s + "2.jpg";
				img = g.getImage(Iimg);
				rings2 = g.getImage(Irings2);
				if (!g.renderingToAndroid() && !DataBase.dataExists(Iimg, true)) g.addToDataBase(g.cloneImage(img), Iimg, 0);
				if (!g.renderingToAndroid() && !DataBase.dataExists(Irings2, true)) g.addToDataBase(g.cloneImage(rings2), Irings2, 0);
				if (unlit) {
					int size[] = g.getSize(img);
					for (int x=0;x<size[0]; x++) {
						int ca = g.getRGB(img, x, 0);
						double cr = g.getRed(ca);
						double cg = g.getGreen(ca);
						double cb = g.getBlue(ca);

						double tr = 1.0 - (0xff & (g.getRGB(rings2,x, 30) >> 8)) / 255.0;
						cr *= tr;
						cg *= tr;
						cb *= tr;
						g.setColor((int)cr, (int)cg, (int)cb, 255);
						int c = g.getColor();
						for (int y=0;y<size[1]; y++) {
							g.setRGB(img, x, y, c);
						}
					}
				}
			}
		} else {
			String Iimg = FileIO.DATA_TEXTURES_DIRECTORY + s + "1.jpg";
			String Irings2 = FileIO.DATA_TEXTURES_DIRECTORY + s + "2.jpg";
			img = g.getImage(Iimg);
			rings2 = g.getImage(Irings2);
			if (!g.renderingToAndroid() && !DataBase.dataExists(Iimg, true)) g.addToDataBase(g.cloneImage(img), Iimg, 0);
			if (!g.renderingToAndroid() && !DataBase.dataExists(Irings2, true)) g.addToDataBase(g.cloneImage(rings2), Irings2, 0);

			if (unlit) {
				int size[] = g.getSize(img);
				for (int x=0;x<size[0]; x++) {
					int ca = g.getRGB(img, x, 0);
					double cr = g.getRed(ca);
					double cg = g.getGreen(ca);
					double cb = g.getBlue(ca);

					double tr = 1.0 - (0xff & (g.getRGB(rings2,x, 30) >> 8)) / 255.0;
					cr *= tr;
					cg *= tr;
					cb *= tr;
					g.setColor((int)cr, (int)cg, (int)cb, 255);
					int c = g.getColor();
					for (int y=0;y<size[1]; y++) {
						g.setRGB(img, x, y, c);
					}
				}
			}
		}
		img = g.getScaledImage(img, desiredW, 33, false, ALLOW_SPLINE_RESIZING);
		rings2 = g.getScaledImage(rings2, desiredW, 33, false, ALLOW_SPLINE_RESIZING);
		g.enableInversion();
		return new Object[] {img, rings2};
	}

	/**
	 * Eliminates from memory the latest rendering.
	 */
	public static void dateChanged() {
		lastRenderElement = null;
		isatOrdered = null;
/*		int index = DataSet.getIndex(imgs, "ringsat");
		if (index >= 0) {
			try {
				imgs = DataSet.eliminateRowFromTable(imgs, 1 + index);
				imgs = DataSet.eliminateRowFromTable(imgs, 1 + index);
				images.remove(index);
				images.remove(index);
			} catch (Exception e) {
				Logger.log(LEVEL.ERROR, "Could not remove rings textures from memory.");
			}
		}
*/
	}

	private void renderAxes(Graphics g, float posx, float posy, int r, double incl_north, double incl_up, double refz, float scaleFactor) {
		if (render.axes && r > 0)
		{
			double dx0 = (r + 2.0*scaleFactor) * FastMath.cos((Constant.PI_OVER_TWO - incl_up));
			double dy0 = (r + 2.0*scaleFactor) * FastMath.sin((Constant.PI_OVER_TWO - incl_up));
			double dx = (r + 15.0*scaleFactor) * FastMath.cos((Constant.PI_OVER_TWO - incl_up));
			double dy = (r + 15.0*scaleFactor) * FastMath.sin((Constant.PI_OVER_TWO - incl_up));
			g.setColor(0, 0, 255, 255);
			g.drawLine(posx - (int) dx, posy - (int) dy, posx - (int) dx0, posy - (int) dy0, true);
			g.drawLine(posx + (int) dx, posy + (int) dy, posx + (int) dx0, posy + (int) dy0, true);
		}
		if (render.axesNOSE && r > 0)
		{
			double dx0 = (r+2*scaleFactor) * FastMath.cos((Constant.PI_OVER_TWO + incl_north));
			double dy0 = (r+2*scaleFactor) * FastMath.sin((Constant.PI_OVER_TWO + incl_north));
			double dx = (r + 15*scaleFactor) * FastMath.cos((Constant.PI_OVER_TWO + incl_north));
			double dy = (r + 15*scaleFactor) * FastMath.sin((Constant.PI_OVER_TWO + incl_north));
			g.setColor(render.foreground, false);
			g.drawLine(posx - (int) dx, posy - (int) dy, posx - (int) dx0, posy - (int) dy0, true);
			g.drawLine(posx + (int) dx, posy + (int) dy, posx + (int) dx0, posy + (int) dy0, true);
			g.drawLine(posx - (int) dy, posy + (int) dx, posx - (int) dy0, posy + (int) dx0, true);
			g.drawLine(posx + (int) dy, posy - (int) dx, posx + (int) dy0, posy - (int) dx0, true);

			dx = (r + 25*scaleFactor) * FastMath.cos((Constant.PI_OVER_TWO + incl_north));
			dy = (r + 25*scaleFactor) * FastMath.sin((Constant.PI_OVER_TWO + incl_north));
			if (render.telescope.invertVertical) posy -= g.getFont().getSize() - 2;
			if (render.showLabels) {
				int fs = (g.getFont().getSize() * 4) / 10;
				g.drawString("N", posx - fs - (int) dx, posy + fs - (int) dy, getDist(0, refz));
				g.drawString("S", posx - fs + (int) dx, posy + fs + (int) dy, getDist(0, refz));
				g.drawString("E", posx - fs - (int) dy, posy + fs + (int) dx, getDist(0, refz));
				g.drawString("W", posx - fs + (int) dy, posy + fs - (int) dx, getDist(0, refz));
			}
		}
	}

	private void renderRings(Graphics g, int r, boolean dubois, float scale, double diameter, double oblateness,
			double refz, int backgroundCol, double posx, double posy, double subslat,
			double dlon, double incl_up, double incl_pole, double distCenter, double timesOut,
			boolean onPlanet, boolean behindPlanet, boolean shadow) {
		boolean useRGB = ((0xff & (render.foreground >> 16)) <= (0xff & (render.foreground >> 8)));
		// Draw planetary rings with textures
		if ((render.target == TARGET.SATURN || render.target == TARGET.URANUS) && render.textures && r > 0)
		{
			g.setColor(255, 255, 0, 255);

			int pz = 7;
			String s = "ringsat";
			if (render.target == TARGET.URANUS) {
				s = "ringura";
				pz = 4;
			}

//			int index = DataSet.getIndex(imgs, s);
			Object img = null, rings2 = null;
//			if (index < 0) {
				Object o[] = this.readRingTexture(g, s, r);
				img = o[0];
				rings2 = o[1];
/*				images.add(img);
				images.add(rings2);
				imgs = DataSet.addStringArray(imgs, new String[] {s, s});
			} else {
				img = images.get(index);
				rings2 = images.get(index+1);
				int desiredW = (int)((r/100.0)+0.5)*100;
				if (desiredW < 100) desiredW = 100;
				if (g.getWidth(img) != desiredW) { // The scaling improves rendering quality
					if (render.target == TARGET.SATURN) {
						img = g.getImage(FileIO.DATA_TEXTURES_DIRECTORY + s + "_backscattered.png");
					} else {
						img = g.getImage(FileIO.DATA_TEXTURES_DIRECTORY + s + "1.jpg");
					}
					if (desiredW > g.getWidth(img)) desiredW = g.getWidth(img);
					Object o[] = this.readRingTexture(g, s, r);
					img = o[0];
					rings2 = o[1];
					images.set(index, img);
					images.set(index+1,  rings2);
				}
			}
*/
			int size[] = g.getSize(img);
			int texture_width = size[0];

			double km2pix = scale * 2.0 / diameter;
			double rr1 = ring_radius[render.target.ordinal() - TARGET.SATURN.ordinal()][0] * km2pix;
			double rr2 = ring_radius[render.target.ordinal() - TARGET.SATURN.ordinal()][pz-1] * km2pix;
			//double rrDispersion = 0;
			//if (render.target == TARGET.SATURN) rrDispersion = km2pix * 136800;

			// Draw the shadow of the rings on the surface of Saturn
			Object screenCopy = g.cloneImage(g.getImage(0,0,g.getWidth(),g.getHeight())); //(int)(posx-r), (int)(posy-r), (int)(2*r+1), (int)(2*r+1)));
			if (screenCopy == null) return;
			boolean visible = true;
			double oblateness2 = oblateness * oblateness;
			double oblatenessSun = 1.0 + Math.abs(FastMath.pow(FastMath.cos(render.ephem.subsolarLatitude), 2)) * (render.target.polarRadius / render.target.equatorialRadius - 1.0);
			double oblatenessSun2 = oblatenessSun * oblatenessSun;
			double r2 = scale * scale;
			double dx, dy;
			if (distCenter < 1 || timesOut < 2.5)
			{
				double zpz;
				if (shadow && (distCenter < 1 || timesOut < 1))
				{
					double factor = 1.0;
					if (Math.abs(dlon) > 6 * Constant.DEG_TO_RAD && render.target == TARGET.SATURN)	factor = 3;
					double dr = 1.0 / factor;
					for (double rr3 = rr1; rr3 <= rr2; rr3 = rr3 + dr)
					{
						double dalfa = Math.PI / (6.0 * factor * rr3);
						int i = (int) (0.5+(double) (texture_width-1) * (rr3 - rr1) / (rr2 - rr1));
						g.disableInversion();
						int transp = (0xff & (g.getRGB(rings2, i, 30) >> 8));
						double transp2 = transp / 255.0;
						g.enableInversion();
						for (double alfa = 0; alfa < Constant.TWO_PI; alfa = alfa + dalfa)
						{
							double zpx = rr3 * FastMath.cos(alfa) / r;
							zpz = rr3 * FastMath.sin(alfa) / r;
							double zpy = 0;

							// Get position from Sun starting from coordinates respect planet equator
							double[] pos = fromPlanetEquatorToFromOtherDirection(new double[] {zpx, zpy, zpz}, 0, subslat);
							if (Math.abs(pos[0]) > 1.0 || Math.abs(pos[1]) > 1.0) continue;
							if (pos[2] < 0) continue;

							// Project into planet surface and go to vision from observer
							pos[2] = Math.sqrt(1.0 - pos[0]*pos[0]-pos[1]*pos[1]*oblateness2);
							pos = fromPlanetEquatorToFromOtherDirection(pos, 0, -subslat);
							//double projectionDist = Math.abs(pos[1]);
							pos = fromPlanetEquatorToFromOtherDirection(pos, dlon, incl_pole);
							if (pos[2] < 0) continue;

							double Zx = pos[0]*r, Zy = pos[1]*r;
							double rr = (Zx * Zx + Zy * Zy);
							if (rr > r2) continue;
							double ang = FastMath.atan2_accurate(Zy, Zx);
							rr = Math.sqrt(rr);
							Zx = rr * FastMath.cos(ang - incl_up);
							Zy = rr * FastMath.sin(ang - incl_up);
							dx = posx + Zx;
							dy = posy + Zy;
							dx = (int) (dx+0.5);
							dy = (int) (dy+0.5);

							if (isInTheScreen(dx, dy, 0)) {
								if (!dubois && (g.getRGB((int) dx, (int) dy) == backgroundCol || g.getRGB((int) dx, (int) dy) != g.getRGB(screenCopy, (int) dx, (int) dy))) continue;
								float dist = getDist(pos[2], refz);

								if (!dubois) {
									//g.setColor(0, 0, 0, transp);
									//g.fillOval((int)dx, (int)dy, 1, 1);

									int my_color_old = g.getRGB(screenCopy, (int)dx, (int)dy);
									// Get RGB compounds of the background
									// (planet)
									int red3 = 0xff & (my_color_old >> 16);
									if (useRGB) {
										int green3 = 0xff & (my_color_old >> 8);
										int blue3 = 0xff & my_color_old;
	
	/*									if (render.target == TARGET.SATURN && rr3 < rrDispersion) {
											float brightnessFactor = 0.25f + (red3 + green3 + blue3) / (255f);
											red3 += (int) (projectionDist * 36.0 * brightnessFactor);
											green3 += (int) (projectionDist * 18.0 * brightnessFactor);
											blue3 += (int) (projectionDist * 64.0 * brightnessFactor);
											if (red3 > 255) red3 = 255;
											if (green3 > 255) green3 = 255;
											if (blue3 > 255) blue3 = 255;
										}
	*/
										// Apply simple illumination model: final
										// planet color = original background *
										// transparency
										int red4 = (int) (red3 * (1.0 - transp2));
										int green4 = (int) (green3 * (1.0 - transp2));
										int blue4 = (int) (blue3 * (1.0 - transp2));
	
										// Compound RGB color
										g.setColor(red4, green4, blue4, 255);
									} else {
										g.setColor((int) (red3 * (1.0 - transp2)), 0, 0, 255);										
									}
									drawPoint(dx, dy, dist, true, g, dubois);
								} else {
									// Get color of the planet at this position
									int my_color_old = backgroundCol;
									int ii = (int) (dx); // - posx + r);
									int jj = (int) (dy); // - posy + r);
		//							if (ii > 0 && ii < 2 * r && jj > 0 && jj < 2 * r) {
										if (dubois) {
											try {
												my_color_old = g.getRGBLeft(screenCopy, ii, jj, dist);
											} catch (Exception exc) {
												continue;
											}
										} else {
											my_color_old = g.getRGB(screenCopy, ii, jj);
										}
		//							}

									// If planet exists here, then apply transparency
									if (pos[2] >= 0)
									{
										// Get RGB compounds of the background
										// (planet)
										int red3 = 0xff & (my_color_old >> 16);
										if (useRGB) {
											int green3 = 0xff & (my_color_old >> 8);
											int blue3 = 0xff & my_color_old;
	
	/*										if (render.target == TARGET.SATURN && rr3 < rrDispersion) {
												red3 += (int) (projectionDist * 36.0);
												green3 += (int) (projectionDist * 18.0);
												blue3 += (int) (projectionDist * 64.0);
												if (red3 > 255) red3 = 255;
												if (green3 > 255) green3 = 255;
												if (blue3 > 255) blue3 = 255;
											}
	*/
											// Apply simple illumination model: final
											// planet color = original background *
											// transparency
											int red4 = (int) (red3 * (1.0 - transp2));
											int green4 = (int) (green3 * (1.0 - transp2));
											int blue4 = (int) (blue3 * (1.0 - transp2));
	
											// Compound RGB color
											g.setColor(red4, green4, blue4, 255);
										} else {
											g.setColor((int) (red3 * (1.0 - transp2)), 0, 0, 255);										
										}
										drawPoint(dx, dy, dist, true, g, dubois);
									}
								}
							}
						}
					}
					screenCopy = g.cloneImage(g.getImage(0,0,g.getWidth(),g.getHeight())); //(int)(posx-r), (int)(posy-r), (int)(2*r+1), (int)(2*r+1)));
				}

				// Draw the rings with textures after the shadows
				if (!onPlanet && !behindPlanet) return;

				int red3;
				int green3;
				int blue3;

				//double planetBorder = (1.0+1.0/r);
				double dxr = 0.0, posr2 = 0.0;
				int my_color, my_color2;
				Object screenCopy2 = null;
				if (dubois)
					screenCopy2 = g.cloneImage(g.getImage2(0,0,g.getWidth(),g.getHeight())); //(int)(posx-r), (int)(posy-r), (int)(2*r+1), (int)(2*r+1)));
				//double maxDlon = 15 * Constant.DEG_TO_RAD;
				//boolean accelerate = false;
				//if (Math.abs(dlon) < maxDlon) accelerate = true;
				double rlim = render.width * scaleFactor / 4;
				//double polR2 = scale * render.target.polarRadius / render.target.equatorialRadius;
				//polR2 *= polR2;
				double shadowDist = 0.0, alpha0 = -dlon, alpha1 = Constant.TWO_PI-dlon;
				if (onPlanet && !behindPlanet) alpha1 -= Math.PI;
				if (!onPlanet && behindPlanet) alpha0 += Math.PI;
				boolean lit = true;
				if (FastMath.sign(render.ephem.positionAngleOfPole) != FastMath.sign(render.ephem.subsolarLatitude)) lit = false;
				for (double rr3 = rr1; rr3 <= rr2; rr3 = rr3 + 1)
				{
					double dalfa0 = Math.PI / (6.0 * rr3), dalfa = dalfa0;
					int i = (int) (0.5+(double) (texture_width-1) * (rr3 - rr1) / (rr2 - rr1));
					g.disableInversion();
					int transp = (0xff & (g.getRGB(rings2, i, 30) >> 8));
					double transp2 = transp / 255.0;
					int rgb = g.getRGB(img, i, 30);
					// Ring compounds
					int red = 0xff & (rgb >> 16);
					int green = 0xff & (rgb >> 8);
					int blue = 0xff & rgb;
					g.enableInversion();
					//double halfrr3 = rr3 * 0.5, frr3 = rr3 * 0.75;
					for (double alfa = alpha0; alfa < alpha1; alfa = alfa + dalfa)
					{
						double zpx = rr3 * FastMath.cos(alfa);
	/*					if (accelerate) {
							if (Math.abs(zpx) > frr3) {
								alfa += dalfa * 0.75;
							} else {
								if (Math.abs(zpx) > halfrr3) alfa += dalfa * 0.25;
							}
						}
	*/					zpz = rr3 * FastMath.sin(alfa);
						double zpy = 0;

						double[] pos = fromPlanetEquatorToFromOtherDirection(new double[] {zpx, zpy, zpz}, 0, subslat);
						if (render.target == TARGET.SATURN) shadowDist = Math.sqrt((pos[0] * pos[0] + pos[1] * pos[1] / oblateness2)) / r;

						visible = pos[2] > 0;
						if (!visible) {
							double posr = (pos[0] * pos[0] + pos[1] * pos[1] / oblatenessSun2);
							if (posr > r2) visible = true;
						}

						pos = fromPlanetEquatorToFromOtherDirection(new double[] {zpx, zpy, zpz}, dlon, incl_pole);
						double posr = (pos[0] * pos[0] + pos[1] * pos[1] / oblateness2);
						if (!dubois && pos[2] < 0 && posr < r2*0.8) continue;

						double Zx = pos[0], Zy = pos[1];
						double ang = FastMath.atan2_accurate(Zy, Zx);
						double rr = FastMath.hypot(Zx, Zy); //Math.sqrt(Zx * Zx + Zy * Zy);
						double ZZx = rr * FastMath.cos(ang - incl_up);
						double ZZy = rr * FastMath.sin(ang - incl_up);
						dx = posx + ZZx;
						dy = posy + ZZy;

						if (isInTheScreen(dx, dy, 0))
						{
							// FIXME
							//if (onPlanet && !behindPlanet && ((alfa < alpha0 + 15*dalfa0 && g.getRGB(screenCopy, (int) dx, (int) dy) != backgroundCol) || (alfa > alpha1-15*dalfa0 && g.getRGB(screenCopy, (int) dx, (int) dy) != backgroundCol))) continue;
							int rgb0 = g.getRGB((int) dx, (int) dy);
							int rgb1 = g.getRGB(screenCopy, (int) dx, (int) dy);
							g.setColor(rgb1, g.getAlpha(rgb0));
							rgb1 = g.getColor();							
							if (!dubois && rgb0 != rgb1) continue;
							float dist = getDist(pos[2]/r, refz);
							//if (dubois && g.getRGBLeft((int) dx, (int) dy, dist) != g.getRGBLeft(screenCopy, (int) dx, (int) dy, dist)) continue;
							//dx = (int) (dx+0.5);
							//dy = (int) (dy+0.5);

							if (dubois) {
								if (posr < r2) {
									dalfa = dalfa0*2;
								} else {
									dalfa = dalfa0;
								}

								double ddx = (0.5f + dist - render.anaglyphMode.getReferenceZ()) * render.anaglyphMode.getEyeSeparation();
								if (render.telescope.invertHorizontal) ddx = -ddx;
								dxr = dx + ddx - posx;
								double dyr = dy - posy;
								double rrd = FastMath.hypot(dxr, dyr); //Math.sqrt(dxr * dxr + dyr*dyr);
								ang = FastMath.atan2_accurate(dyr, dxr);
								double xd = rrd * FastMath.cos(ang + incl_up);
								double yd = rrd * FastMath.sin(ang + incl_up);
								double rrr2 = FastMath.hypot(xd, yd/oblateness); // Math.sqrt(xd*xd + yd*yd / oblateness2);
								posr2 = rrr2 / r;

								dxr = dx - ddx - posx;
								rrd = FastMath.hypot(dxr, dyr); //Math.sqrt(dxr * dxr + dyr*dyr);
								ang = FastMath.atan2_accurate(dyr, dxr);
								xd = rrd * FastMath.cos(ang + incl_up);
								yd = rrd * FastMath.sin(ang + incl_up);
								rrr2 = FastMath.hypot(xd, yd/oblateness); //Math.sqrt(xd*xd + yd*yd / oblateness2);
								posr = rrr2 / r;
								dxr += posx;
							} else {
								posr = Math.sqrt(posr) / r;
							}

							// Get pixel color
							int ii = (int) (ZZx + posx); //r);
							int jj = (int) (ZZy + posy); //r);
							my_color = backgroundCol;
							my_color2 = backgroundCol;
	//						if (ii > 0 && ii < 2 * r && jj > 0 && jj < 2 * r) {
								if (dubois) {
									try {
										my_color = g.getRGBLeft(screenCopy, ii, jj, dist);
									} catch (Exception exc) {}
									try {
										my_color2 = g.getRGBRight(screenCopy2, ii, jj, dist);
									} catch (Exception exc) {}
								} else {
									my_color = g.getRGB(screenCopy, ii, jj);
								}
	//						}


							// eliminate possible background stars in the dark region of the Saturn rings
							if (!visible) { // Region of the rings in shadow
								my_color = backgroundCol;
								my_color2 = backgroundCol;
								if (dubois) {
									if (posr > 0.99 && (posr> 1.0+0.5/r || (g.getRGBLeft((int) dx, (int) dy, dist) == backgroundCol)) || pos[2] > 0) {
										if (g.getRed(my_color) < 10) {
											my_color = 0;
											g.setColor(my_color, transp);
										} else {
											g.setColor(my_color, transp);
										}
										g.fillOvalAnaglyphLeft((float)dx, (float)dy, -1.0f, -1.0f, dist);
									}
									if (isInTheScreen(dxr, dy, 0)) {
										if (posr2 > 0.99 && (posr2> 1.0+0.5/r || (g.getRGBRight((int) dx, (int) dy, dist) == backgroundCol)) || pos[2] > 0) {
											if (g.getRed(my_color2) < 10) {
												my_color2 = 0;
												g.setColor(my_color2, transp);
											} else {
												g.setColor(my_color2, transp);
											}
											g.setColor(my_color2, transp);
											g.fillOvalAnaglyphRight((float)dx, (float)dy, -1.0f, -1.0f, dist);
										}
									}
								} else {
									if (posr > 0.99 && (posr> 1.0+0.5/r || (rgb0 == backgroundCol))) {
										if (render.target == TARGET.SATURN && shadowDist > 0.995 && shadowDist < 1.005 && lit) {
											red3 = 0xff & (my_color >> 16);
											green3 = 0xff & (my_color >> 8);
											blue3 = 0xff & my_color;
											red3 += 32 + (int) ((128.0)*(shadowDist-0.995)/0.01);
											if (red3 > 255) red3 = 255;
											g.setColor(red3, green3, blue3, transp);
											g.fillOval((int)dx, (int)dy, -1, -1, false);
										} else {
											if (g.getRed(my_color) < 10) {
												my_color = 0;
												g.setColor(my_color, transp);
											} else {
												g.setColor(my_color, transp);
											}
											g.fillOval((int)dx, (int)dy, -1, -1, false);
											//drawPoint(dx, dy, dist, true, g, dubois);
										}
									}
								}
								continue;
							}

							// Operate the color
							if (posr > 0.99 && (posr> 1.0+0.5/r || ((!dubois && rgb0 == backgroundCol) || (dubois && g.getRGBLeft((int) dx, (int) dy, dist) == backgroundCol))) || pos[2] > 0)
							{
								// Background compounds (planet with
								// transparency applied)
								red3 = 0xff & (my_color >> 16);
								if (useRGB) {
									green3 = 0xff & (my_color >> 8);
									blue3 = 0xff & my_color;
	
									// Apply illumination model for rings:
									// final color = ring color * opacity +
									// background * transparency
									// I also add a term which computes the sun
									// inclination above rings
									int red4 = (int) ((red * transp2 + red3 * (1.0 - transp2)));
									int green4 = (int) ((green * transp2 + green3 * (1.0 - transp2)));
									int blue4 = (int) ((blue * transp2 + blue3 * (1.0 - transp2)));
									if (red4 > 254) red4 = 254;
									if (green4 > 254) green4 = 254;
									if (blue4 > 254) blue4 = 254;
	
									// Draw
									g.setColor(red4, green4, blue4, 255); //190 + (int)(transp2 * 64));
								} else {
									g.setColor((int) ((red * transp2 + red3 * (1.0 - transp2))), 0, 0, 255);
								}
								if (dubois) {
									g.fillOvalAnaglyphLeft((float)dx, (float)dy, 1.1f, 1.1f, dist);
								} else {
									g.fillOval((int)dx, (int)dy, 1, 1, false);
								}
							}
							if (dubois && posr2 > 0.99 && (posr2> 1.0+0.5/r || (g.getRGBRight((int) dx, (int) dy, dist) == backgroundCol)) || pos[2] > 0)
							{
								// Background compounds (planet with
								// transparency applied)
								red3 = 0xff & (my_color2 >> 16);
								if (useRGB) {
									green3 = 0xff & (my_color2 >> 8);
									blue3 = 0xff & my_color2;
	
									// Apply illumination model for rings:
									// final color = ring color * opacity +
									// background * transparency
									// I also add a term which computes the sun
									// inclination above rings
									int red4 = (int) ((red * transp2 + red3 * (1.0 - transp2)));
									int green4 = (int) ((green * transp2 + green3 * (1.0 - transp2)));
									int blue4 = (int) ((blue * transp2 + blue3 * (1.0 - transp2)));
									if (red4 > 254) red4 = 254;
									if (green4 > 254) green4 = 254;
									if (blue4 > 254) blue4 = 254;
	
									// Draw
									g.setColor(red4, green4, blue4, 255);
								} else {
									g.setColor((int) ((red * transp2 + red3 * (1.0 - transp2))), 0, 0, 255);									
								}
								g.fillOvalAnaglyphRight((float)dx, (float)dy, 1.1f, 1.1f, dist);
							}
						} else {
							if (r > rlim) { // acelerate rings for very high zoom
								double rx = 0, ry = 0;
								if (dx > render.width*scaleFactor) rx = dx - render.width*scaleFactor;
								if (dx < 0) rx = -dx;
								if (dy > render.height*scaleFactor) ry = dy - render.height*scaleFactor;
								if (dy < 0) ry = -dy;
								double d = FastMath.hypot(rx, ry)/r; //Math.sqrt(rx*rx+ry*ry)/r;
								alfa = alfa + d * Constant.PI_OVER_SIX;
							}
						}
					}
				}
			}
		}

		// Planetary rings without textures
		// Note I draw Neptune's rings always in this way. I have no information
		// about Neptune's rings, which are not continuous and very faint
		if (r > 0 && ((render.target == TARGET.SATURN || render.target == TARGET.URANUS) && !render.textures || 
				(r > 5 && render.target == TARGET.NEPTUNE)))
		{
			g.setColor(useRGB ? 128 : 255, useRGB ? 128 : 0, 0, 255); // In yellow
			int pz = 4;
			if (render.target == TARGET.SATURN) pz = 7;
			// Approximate y/x relative factor
			double a00 = oblateness * oblateness * Math.abs(FastMath.sin(render.ephem.positionAngleOfPole));
			double r2 = r * r;
			for (int i = 1; i <= pz; i++)
			{
				double rr1 = (double) ring_radius[render.target.ordinal() - TARGET.SATURN.ordinal()][i-1] * (double) r * 2.0 / diameter;
				double tmp = 0;
				double alfaStep = Math.PI / (rr1 * 4.0);
				if (alfaStep < Math.PI / 50.0) alfaStep = Math.PI / 50.0;
				float olddx=-1, olddy=-1,olddz = -1;
				for (double alfa = tmp; alfa < (tmp / 2.0 + Constant.TWO_PI); alfa = alfa + alfaStep)
				{
					double zpx = rr1 * FastMath.cos(alfa);
					double zpy = rr1 * FastMath.sin(alfa) * a00;
					double rr = (zpx * zpx + zpy * zpy);

					if (rr > r2 || rr < r2 && FastMath.sign(incl_pole) == FastMath.sign(zpy))
					{
						rr = Math.sqrt(rr);
						double ang = FastMath.atan2_accurate(zpy, zpx);
						double dx = posx + rr * FastMath.cos(ang - incl_up);
						double dy = posy + rr * FastMath.sin(ang - incl_up);
						if (this.isInTheScreen(dx, dy, 0))
						{
							double dz = Math.sqrt(rr1*rr1-rr*rr)/r;
							if (FastMath.sign(incl_pole) != FastMath.sign(zpy)) dz = -dz;
							if (olddx >=0 && olddy >= 0)
								g.drawLine((float) dx, (float) dy, olddx, olddy, getDist(dz, refz), getDist(olddz, refz));
							olddx = (float) dx;
							olddy = (float) dy;
							olddz = (float) dz;
						} else {
							olddx = olddy = olddz = -1;
						}
					} else {
						olddx = olddy = olddz = -1;
					}
				}
			}
		}
	}

	private void drawPlanetGrid(Graphics g, double incl_rotation, double incl_pole, double incl_up,
			float scale, int r, double oblateness, float posx, float posy, boolean dubois, double refz) throws JPARSECException {
		// Draw planet without textures
		double x1 = FastMath.cos(incl_rotation) * FastMath.cos(incl_pole);
		double y1 = FastMath.sin(incl_rotation) * FastMath.cos(incl_pole);
		double z1 = FastMath.sin(incl_pole);
		double dr2 = ((x1 - 1.0) * (x1 - 1.0) + (y1 - 0.0) * (y1 - 0.0) + (z1 - 0.0) * (z1 - 0.0));
		double dr = Functions.normalizeRadians(Math.acos(1.0 - dr2 / 2.0));

		// Draw origin of coordinates if it is visible
		if (r > 30 && (dr < Constant.PI_OVER_TWO || dr > Math.PI * 1.5))
		{
			double dz = scale * FastMath.cos(dr);
			double dx = -dz * FastMath.tan(-incl_rotation) / FastMath.cos(incl_pole);
			double dy = dz * FastMath.tan(incl_pole) * oblateness;
			if (render.target == TARGET.Moon || render.target == TARGET.SUN || render.target == TARGET.MERCURY || render.target == TARGET.VENUS || render.target == TARGET.EARTH)
				dx = -dx;
			dr = (dx * dx + dy * dy);
			if (dr < scale * scale)
			{
				double ang = FastMath.atan2_accurate(dy, dx);
				dr = Math.sqrt(dr);
				dx = dr * FastMath.cos(ang - incl_up);
				dy = dr * FastMath.sin(ang - incl_up);
				g.setColor(render.foreground, false);
				g.drawOval(posx + (float) dx - 3, posy + (float) dy - 3, 6, 6, true);
			}
		}

		// White if it is above horizon, blue otherwise
		g.setColor(render.foreground, false);

		// Step within lines
		float lessLines = 1, dpi = 1;
		//if (g.renderingToAndroid()) dpi = 3;
		if (scale < 80*dpi) lessLines = 2;
		double delt = lessLines * 5.0 * Constant.DEG_TO_RAD;
		double b0 = -incl_pole;
		double a0 = -(incl_rotation / (2*delt) - (int) (incl_rotation / (2*delt))) * 2*delt;
		if (render.target == TARGET.Moon || render.target == TARGET.SUN || render.target == TARGET.MERCURY || render.target == TARGET.VENUS || render.target == TARGET.EARTH)
			a0 = -a0;

		double c0 = Constant.PI_OVER_TWO + b0;
		double d0 = Constant.PI_OVER_TWO - a0;
		double a = -2*delt;
		double r2 = r * r;
		double piOver36 = lessLines * Math.PI / 36.0;
		double piOver32 = Math.PI / 32.0;
		float previous_x[] = new float[36];
		float previous_y[] = new float[36];
		LocationElement loc0 = new LocationElement(d0, Constant.PI_OVER_TWO - c0, 1.0);
		boolean hq = true; //false;
		//if (render.highQuality && RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR > 1) hq = true;
		do {
			a = a + delt;
			previous_x[1] = 0.0f;
			previous_y[1] = (float) (scale * FastMath.cos(b0));
			double b = -piOver36;
			int nn = 0;
			boolean started = false;
			boolean pV = false;
			boolean pH = true;
			int nV = (int) Math.round(a/delt);
			if (nV/2.0 == (int) (nV/2.0)) pV = true;
			double t = 0;
			do
			{
				b = b + piOver36;

				// Rotate into the sphere
				double u = LocationElement.getApproximateAngularDistance(new LocationElement(a, Constant.PI_OVER_TWO - b, 1.0), loc0);
				double b1 = b; //RenderPlanet.geocentricToGeodetic(render.target.equatorialRadius, render.target.polarRadius, b);

				double q = scale * FastMath.cos(a + a0) * FastMath.sin(b1);
				double w = scale * (FastMath.cos(b1) * FastMath.cos(b0) + FastMath.sin(b1) * FastMath.sin(b0) * FastMath.sin(a + a0)) * oblateness;
				double e = FastMath.hypot(q, w);
				if (e > 500) {
					t = FastMath.atan2_accurate(w, q);
				} else {
					t = FastMath.atan2(w, q);
				}
				q = e * FastMath.cos(t + incl_up);
				w = e * FastMath.sin(t + incl_up);

				// Show if it is visible
				if (u <= (Constant.PI_OVER_TWO) && b >= 0.0)
				{
					double z = 0;

					// Draw properly
					if (started && nn > 0 && b > piOver32 && pV)
					{
						float yp0 = posy - (float)w, yp1 = posy - previous_y[nn];

						if (!dubois) {
							if (hq) {
								g.drawLine(posx + (float)q, yp0, posx + previous_x[nn], yp1, true);
							} else {
								g.drawStraightLine(posx + (float)q, yp0, posx + previous_x[nn], yp1);
							}
						} else {
							z = Math.sqrt(r2 - e * e);
							double prev_z = Math.sqrt(r2 - previous_x[nn] * previous_x[nn] - previous_y[nn] * previous_y[nn]);
							if (hq) {
								g.drawLine(posx + (float)q, yp0, posx + previous_x[nn],
									yp1, getDist(z/r, refz), getDist(prev_z/r, refz));
							} else {
								g.drawStraightLine(posx + (float)q, yp0, posx + previous_x[nn],
										yp1, getDist(z/r, refz), getDist(prev_z/r, refz));

							}
						}
							double longit = a + a0 + incl_rotation - Constant.PI_OVER_TWO;
							if (render.target == TARGET.Moon || render.target == TARGET.SUN || render.target == TARGET.MERCURY || render.target == TARGET.VENUS || render.target == TARGET.EARTH)
								longit = -a - a0 + incl_rotation + Constant.PI_OVER_TWO;
							int deg = (int) (Functions.normalizeDegrees(longit * Constant.RAD_TO_DEG + 1));
							deg = 10 * (deg / 10);
							if (Math.abs(b - Constant.PI_OVER_TWO) < piOver36*0.5 && deg % 20 == 0 && r > 100*dpi)
							{
								String label = "" + deg+"\u00b0";
								if (render.showLabels) g.drawString(label, posx + (int) q, posy - (int) w, getDist(z/r, refz));
							}
					}
					if (a > 0 && nn > 1 && started && pH)
					{
						double prev_z = Math.sqrt(r2 - previous_x[nn+1] * previous_x[nn+1] - previous_y[nn+1] * previous_y[nn+1]);
						float yp0 = posy - (float)w, yp1 = posy - previous_y[nn+1];

						if (!dubois) {
							if (hq) {
								g.drawLine(posx + (float)q, yp0, posx + previous_x[nn + 1], yp1, true);
							} else {
								g.drawStraightLine(posx + (float)q, yp0, posx + previous_x[nn + 1], yp1);
							}
						} else {
							z = Math.sqrt(r2 - e * e);
							if (hq) {
								g.drawLine(posx + (float)q, yp0, posx + previous_x[nn + 1],
										yp1, getDist(z/r, refz), getDist(prev_z/r, refz));
							} else {
								g.drawStraightLine(posx + (float)q, yp0, posx + previous_x[nn + 1],
										yp1, getDist(z/r, refz), getDist(prev_z/r, refz));
							}
						}
					}
					started = true;
				}
				nn ++;
				previous_x[nn] = (float) q;
				previous_y[nn] = (float) w;
				pH = !pH;
			} while (b <= (Math.PI * 0.92));
		} while (a <= (Constant.TWO_PI));
	}

	private static boolean similarRenders(PlanetRenderElement ere, PlanetRenderElement ere2)
	{
		if (ere == null) {
			return ere2 == null;
		}
		if (ere2 == null) {
			return false;
		}
		boolean equals = true;
		//if (ere.showLabels != ere2.showLabels) equals = false;
		//if (ere.axes != ere2.axes) equals = false;
		//if (ere.axesNOSE != ere2.axesNOSE) equals = false;
		if (ere.textures != ere2.textures) equals = false;
		if (ere.height != ere2.height) equals = false;
		if (ere.width != ere2.width) equals = false;
		//if (ere.satellitesMain != ere2.satellitesMain) equals = false;
		//if (ere.satellitesAll != ere2.satellitesAll) equals = false;
		if (ere.difraction != ere2.difraction) equals = false;
		if (ere.northUp != ere2.northUp) equals = false;
		if (ere.target == null || ere2.target == null) {
			if (ere.target != null || ere2.target != null) equals = false;
		} else {
			if (ere.target != ere2.target) equals = false;
		}
		//if (!ere.telescope.equals(ere2.telescope)) equals = false;
		if (ere.ephemSun == null || ere2.ephemSun == null) {
			if (ere.ephemSun != null || ere2.ephemSun != null) equals = false;
		} else {
			if (!ere.ephemSun.equals(ere2.ephemSun)) equals = false;
		}
		if (ere.ephem == null || ere2.ephem == null) {
			if (ere.ephem != null || ere2.ephem != null) equals = false;
		} else {
			if (!ere.ephem.equals(ere2.ephem)) equals = false;
		}
		if (ere.background != ere2.background) equals = false;
		if (ere.foreground != ere2.foreground) equals = false;
		if (ere.anaglyphMode != ere2.anaglyphMode) equals = false;
		//if (ere.highQuality != ere2.highQuality) equals = false;

		if (ere2.moonephem == null || ere.moonephem == null) {
			if (ere.moonephem != null || ere2.moonephem != null) equals = false;
		} else {
			if (ere2.moonephem.length == ere.moonephem.length)
			{
				for (int i=0; i<ere2.moonephem.length; i++)
				{
					if (ere2.moonephem[i] == null) {
						if (ere.moonephem[i] != null) equals = false;
					} else {
						if (!ere2.moonephem[i].equals(ere.moonephem[i])) equals = false;
					}
				}
			} else {
				equals = false;
			}
		}

		return equals;
	}

	private static Object imgMoon = null;
	
	/**
	 * Renderize a planet.
	 *
	 * @param gg Graphics object.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public synchronized void renderize(Graphics gg) throws Exception
	{
		if (gg.renderingToExternalGraphics()) dateChanged();
		Graphics g = null;
		scaleFactor = 1f;
		double field = render.telescope.getField() * Constant.RAD_TO_ARCSEC;
		float scale = (float) (render.width * render.ephem.angularRadius * Constant.RAD_TO_ARCSEC / field), scale0 = scale;
		int r = (int) scale;

		boolean hq = render.highQuality && render.textures;
		if (!RenderPlanet.FORCE_HIGHT_QUALITY && field > 3600 && hq && RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR > 1) hq = false;

/*		if (lastRenderElement != null && lastRenderElement.target == render.target) {
			lastRenderElement.ephem.paralacticAngle = render.ephem.paralacticAngle;
			if (render.moonephem != null && lastRenderElement.moonephem != null && render.moonephem.length == lastRenderElement.moonephem.length) {
				for (int i=0; i<render.moonephem.length; i++) {
					lastRenderElement.moonephem[i].paralacticAngle = render.moonephem[i].paralacticAngle;
				}
			}
		}
*/
		repaint = false;
		if ((field < 3600 || scale > minScale) && render.textures && render.target != TARGET.SUN && renderingSky && lastRenderElement != null &&
				similarRenders(lastRenderElement, render) &&
				lastScale >= scale && lastScale > minScale) {
			repaint = !(lastScale > scale && (lastScale*2 > render.width || lastScale*2 > render.height));
		}
		scaleFactor = MAXIMUM_TEXTURE_QUALITY_FACTOR;
		if (!hq) scaleFactor = 1;
		boolean fastGridAndBye = false;
		boolean refraction = (int) (scale * (1.0 - upperLimbFactor) + 0.5) > 1;
		if (!render.highQuality && (!render.textures || render.target == TARGET.SUN) && !render.difraction && render.anaglyphMode == ANAGLYPH_COLOR_MODE.NO_ANAGLYPH && !refraction)
			fastGridAndBye = true;
		if (!repaint && hq && MAXIMUM_TEXTURE_QUALITY_FACTOR > 0f && MAXIMUM_TEXTURE_QUALITY_FACTOR != 1f) {
			g = gg.getGraphics((int)(gg.getWidth()*scaleFactor+0.5), (int)(gg.getHeight()*scaleFactor+0.5));
			scale *= scaleFactor;
			r = (int) scale;
			FONT font = gg.getFont();
			font = FONT.getDerivedFont(font, (int)(font.getSize()*scaleFactor+0.5));
			g.setFont(font);
		} else {
			if (repaint || fastGridAndBye || scale <= 2) { // *
				g = gg;
			} else {
				g = gg.getGraphics();
				g.setFont(gg.getFont());
				g.setColor(render.background, true);
				g.disableInversion();
				g.fillRect(0, 0, g.getWidth(), g.getHeight());
				g.enableInversion();
			}
		}
		if (!repaint && !fastGridAndBye && scale > minScale) { // *
			int clip[] = gg.getClip();
			if (render.anaglyphMode != ANAGLYPH_COLOR_MODE.NO_ANAGLYPH) {
				Object img1 = gg.getImage(0, 0, gg.getWidth(), gg.getHeight());
				if (img1 != null) {
					Object img2 = gg.getImage2(0, 0, gg.getWidth(), gg.getHeight());
					if (scaleFactor != 1) {
						int size1[] = g.getSize(img1);
						int size2[] = g.getSize(img2);
						img1 = g.getScaledImage(img1, (int)(scaleFactor*size1[0]), (int)(scaleFactor*size1[1]), false, RenderPlanet.ALLOW_SPLINE_RESIZING);
						img2 = g.getScaledImage(img2, (int)(scaleFactor*size2[0]), (int)(scaleFactor*size2[1]), false, RenderPlanet.ALLOW_SPLINE_RESIZING);
					}
					g.setAnaglyph(img1, img2);
				}
			} else {
				Object img1 = gg.getImage(0, 0, gg.getWidth(), gg.getHeight());
				if (img1 != null) {
					if (scaleFactor != 1) {
						int size1[] = g.getSize(img1);
						img1 = g.getScaledImage(img1, (int)(scaleFactor*size1[0]), (int)(scaleFactor*size1[1]), false, RenderPlanet.ALLOW_SPLINE_RESIZING);
					}
					g.setAnaglyph(img1, img1);
				}
			}
			g.setColor(render.background, true);
			g.disableInversion();
			g.fillRect(0, 0, g.getWidth(), (clip[1])*scaleFactor+1);
			g.fillRect(0, (clip[1]+clip[3])*scaleFactor-1, g.getWidth(), g.getHeight());
			g.fillRect(0, 0, (clip[0])*scaleFactor+1, g.getHeight());
			g.fillRect((clip[0]+clip[2])*scaleFactor-1, 0, g.getWidth(), g.getHeight());
			g.enableInversion();
		}
		if (render.anaglyphMode == ANAGLYPH_COLOR_MODE.GREEN_RED || render.anaglyphMode == ANAGLYPH_COLOR_MODE.RED_CYAN)
			g.disableAnaglyph();

		// Recover information in a more confortable way
		double incl_pole = render.ephem.positionAngleOfPole;
		double incl_north = render.ephem.paralacticAngle;
		if (renderingSky) incl_north = this.northAngle;
		if (render.northUp) incl_north = 0.0;

		double incl_axis = render.ephem.positionAngleOfAxis;
		double incl_rotation = render.ephem.longitudeOfCentralMeridian;
		double diameter = render.target.equatorialRadius * 2.0;
		double phase = render.ephem.phase;
		// Add 'dif' to pass from geodetic to geocentric latitude
		incl_pole = RenderPlanet.geodeticToGeocentric(render.target.equatorialRadius, render.target.polarRadius, incl_pole);
		double subslat = render.ephem.subsolarLatitude;
		subslat = RenderPlanet.geodeticToGeocentric(render.target.equatorialRadius, render.target.polarRadius, subslat);

		double dlon = -(render.ephem.subsolarLongitude - render.ephem.longitudeOfCentralMeridian);
		double dlat = -(subslat - incl_pole);
		if (render.target.isPlanet() && render.target.ordinal() >= TARGET.JUPITER.ordinal() && render.target.ordinal() <= TARGET.NEPTUNE.ordinal()) {
			dlon = (render.ephem.subsolarLongitude - render.ephem.longitudeOfCentralMeridianSystemIII);
		}

		// Set initial shape and size parameters
		double oblateness = 1.0 + Math.abs(FastMath.pow(FastMath.cos(render.ephem.positionAngleOfPole), 2)) * (render.target.polarRadius / render.target.equatorialRadius - 1.0);
		double incl_up = incl_axis - incl_north;
		float posx = (int) (xPosition * scaleFactor);
		float posy = (int) (yPosition * scaleFactor);
		double refz = render.anaglyphMode.getReferenceZ();
		planet_size = scale;

		double screenRadiusX = render.width*scaleFactor*0.5;
		double screenRadiusY = render.height*scaleFactor*0.5;
		double screenRadiusMax = (screenRadiusX + screenRadiusY)*0.5;
		double distCenterX = Math.abs(1-posx/screenRadiusX), distCenterY = Math.abs(1-posy/screenRadiusY);
		double distCenter = Math.min(distCenterX, distCenterY);
		if (distCenter < 1.0) distCenter = Math.max(distCenterX, distCenterY);
		double timesOut = 0;
		if (distCenter > 1) {
			timesOut = ((distCenter-1)*screenRadiusMax)/r;
			double ratio = screenRadiusX/screenRadiusY;
			if (ratio < 1) ratio = 1.0 / ratio;
			timesOut /= ratio;
		}
		if (distCenter > 1 && timesOut > 200) {
			if (renderingSky) {
				scale = scale0;
				posx = (int) (xPosition);
				posy = (int) (yPosition);
				scaleFactor = 1.0f;
				try {
					this.renderizeSatellites(gg, posx, posy, incl_north, incl_up, scale, oblateness, render.ephem.distance, dlon, dlat);
				} catch (Exception e) {	}
			}
			return;
		}
		boolean planetVisible = true, ringsTexturesVisible = true;
		if (posx/scaleFactor > (render.width + r/scaleFactor) || posy/scaleFactor > (render.height + r/scaleFactor)) planetVisible = false;
		if (-r/scaleFactor > posx || -r/scaleFactor > posy) planetVisible = false;
		if (2*r/scaleFactor > render.width) ringsTexturesVisible = false;
		if (render.ephem.angularRadius > Constant.PI_OVER_FOUR) planetVisible = false;

		// Fast rendering if previous rendering was saved
		if (repaint) {
			double imgScale =  scale0 / (lastScale * scaleFactor);
			if (scaleFactor == MAXIMUM_TEXTURE_QUALITY_FACTOR) {
				posx = (int) (xPosition);
				posy = (int) (yPosition);
			}

			Object img = lastRender.getRendering();
			//if (imgScale != 1.0f) img = g.getScaledImage(img, (int)(g.getWidth(img)*imgScale), (int)(g.getHeight(img)*imgScale), true, ALLOW_SPLINE_RESIZING);

			int s = 2+(int)(scale0+1);
			if (render.target == TARGET.SATURN || render.target == TARGET.URANUS || render.target == TARGET.NEPTUNE) s*=4;
			int cl[] = gg.getClip();
			int cli[] = new int[] {cl[0], cl[1], cl[2], cl[3]};
			cl = gg.getInvertedRectangle(cl);
			int cl0[] = cl.clone();
			if (cl[0] < posx-s) cl[0] = (int)posx-s;
			if (cl[1] < posy-s) cl[1] = (int)posy-s;
			if (cl[0]+2*s+1 < cl0[0] + cl[2]) cl[2] = 2*s+1;
			if (cl[0]+cl[2] > cl0[0] + cl0[2]) cl[2] = cl0[2]+cl0[0]-cl[0];
			if (cl[1]+2*s+1 < cl0[1] + cl[3]) cl[3] = 2*s+1;
			if (cl[1]+cl[3] > cl0[1] + cl0[3]) cl[3] = cl0[3]+cl0[1]-cl[1];
			cl = gg.getInvertedRectangle(cl);
			gg.disableInversion();
			gg.setClip(cl[0], cl[1], cl[2], cl[3]);
			gg.enableInversion();
			gg.drawImage(img, (float)(0.5+posx-lastPosX*imgScale/scaleFactor), (float)(0.5+posy - lastPosY*imgScale/scaleFactor), imgScale, imgScale);
			gg.disableInversion();
			gg.setClip(cli[0], cli[1], cli[2], cli[3]);
			gg.enableInversion();

			scaleFactor = 1f;
			try {
				this.renderizeSatellites(gg, posx, posy, incl_north, incl_up, scale, oblateness, render.ephem.distance, dlon, dlat);
			} catch (Exception e) {
				e.printStackTrace();
			}

//			this.renderAxes(gg, posx, posy, r, incl_north, incl_up, refz, 1);

			return;
		}

		// Render everything
		isatOrdered = null;
		double dx, dy;

		// Draw background unless we are rendering the sky
		g.setColor(this.render.background, true);
		if (!renderingSky)
			g.fillRect(0, 0, g.getWidth(), g.getHeight());
		int backgroundCol = g.getColor();
		boolean dubois = render.anaglyphMode.isReal3D();
		boolean ih = render.telescope.invertHorizontal, iv = render.telescope.invertVertical;

		// Renderize planet
		g.setColor(render.foreground, false);
		int br = g.getRed(render.background), bg = g.getGreen(render.background), bb = g.getBlue(render.background);
		if (r < minScale && !fastGridAndBye) fastGridAndBye = true;
		if (((r > render.width && !render.highQuality && !RenderPlanet.FORCE_HIGHT_QUALITY) ||
				(renderingSky && r > 900) || // XXX: Disable 4k rendering in sky (slow)
				(r > render.width/1.5 && g.renderingToAndroid())) && render.textures && !fastGridAndBye) fastGridAndBye = true;
		if (!fastGridAndBye && render.textures && r > minScale && render.target != TARGET.SUN)
		{
//			if (ringsTexturesVisible)
//				this.renderRings(g, r, dubois, scale, diameter, oblateness, refz, backgroundCol, posx, posy, subslat, dlon, incl_up, incl_pole, distCenter, timesOut, false, true, false);

			// Get the texture
			String s = render.target.getEnglishName();
//			int index = DataSet.getIndex(imgs, s);
			Object img = null, img2 = null;
//			if (index < 0) {
			int size[] = new int[] {10, 10};
				if (planetVisible) {
					if (render.target == TARGET.EARTH && earthMap != null && earthMap.EarthMapSource != null) {
						img = g.getImage(earthMap.EarthMapSource);
						if (!g.renderingToAndroid() && !DataBase.dataExists(earthMap.EarthMapSource, true)) g.addToDataBase(g.cloneImage(img), earthMap.EarthMapSource, 0);
					} else {
						if (render.target == TARGET.Moon && imgMoon != null) {
							img = imgMoon;
						} else {
							String Iimg = FileIO.DATA_TEXTURES_DIRECTORY + s + ".jpg";
							img = g.getImage(Iimg);
							if (!g.renderingToAndroid() && !DataBase.dataExists(Iimg, true)) g.addToDataBase(g.cloneImage(img), Iimg, 0);
							if (render.target == TARGET.Moon) imgMoon = img;
						}
					}
					size = g.getSize(img);
					if (render.target == TARGET.EARTH && showDayAndNight) {
						String Iimg2 = FileIO.DATA_TEXTURES_DIRECTORY + "Earth_night.jpg";
						img2 = g.getImage(Iimg2);
						if (!g.renderingToAndroid() && !DataBase.dataExists(Iimg2, true)) g.addToDataBase(g.cloneImage(img2), Iimg2, 0);
						int size2[] = g.getSize(img2);
						if (size2[0] != size[0] || size2[1] != size[1])
							img2 = g.getScaledImage(img2, size[0], size[1], false, ALLOW_SPLINE_RESIZING);
	
					}
				}
/*				if (!Configuration.USE_DISK_FOR_DATABASE) {
					images.add(img);
					imgs = DataSet.addStringArray(imgs, new String[] {s});
				}
			} else {
				img = images.get(index);
			}
*/
			int texture_width = size[0];
			int texture_height = size[1];

			// Obtain sun illumination position
			double dr = (double) r * Math.abs(FastMath.sin(render.ephem.phaseAngle));
			double sun_x = posx + dr * FastMath.cos((-Constant.PI_OVER_TWO - render.ephem.brightLimbAngle + incl_north));
			double sun_y = posy + dr * FastMath.sin((-Constant.PI_OVER_TWO - render.ephem.brightLimbAngle + incl_north));
			double sun_z = -r * Math.abs(FastMath.cos(render.ephem.phaseAngle));
			if (phase < 0.5) sun_z = -sun_z;

			texture_step = texture_height / (4 * r);
			if (texture_step < 1) texture_step = 1;

			// Modify Jupiter rendered longitudes to account for Great Red Spot
			// movement
			if (render.target == TARGET.JUPITER)
			{
				switch (offsetInLongitudeOfJupiterGRS_system)
				{
				case 1:
					incl_rotation = render.ephem.longitudeOfCentralMeridianSystemI;
					break;
				case 2:
					incl_rotation = render.ephem.longitudeOfCentralMeridianSystemII;
					break;
				case 3:
					incl_rotation = render.ephem.longitudeOfCentralMeridianSystemIII;
					break;
				}
				incl_rotation += offsetInLongitudeOfJupiterGRS;
			}

			// Pixel position in the planet
			double a0 = incl_rotation - Constant.PI_OVER_TWO;
			// Correct for the longitude criteria in Moon: the texture is flipped
			if (render.target == TARGET.Moon || render.target == TARGET.MERCURY || render.target == TARGET.EARTH) a0 = -incl_rotation - Constant.PI_OVER_TWO;
			// This is necessary to fix a displacement in the Venus Magellan
			// texture from 0 degrees longitude
			if (render.target == TARGET.VENUS) a0 = -a0 - Constant.TWO_PI / 3.0;

			int tw = texture_width, th = texture_height;
			float lx = ((int)(posx+0.5) - r - 1);
			float ux = ((int)(posx+0.5) + r + 1);
			float ly = ((int)(posy+0.5) - r - 1);
			float uy = ((int)(posy+0.5) + r + 1);
			double tw2pi = tw / Constant.TWO_PI;
			double a0minus2pi = Functions.normalizeRadians(Constant.PI_OVER_TWO - a0);
			double r2 = r * r;
			double thpi = (th-1.0) / Math.PI;
			double sinp = Math.sin(incl_pole), cosp = Math.cos(incl_pole);

			if (planetVisible)
			{
				final int white = (0x0ff << 24) | (0x0ff << 16) | (0x0ff << 8) | 0x0ff; // 0xff03fc0ff00;
				float SaturnLimbFactor = 0.95f;
				for (float j=ly; j<=uy; j++) {
					boolean first = true, doit = true;
					int oldi = 0, oldj = 0;
					double oldz = 0, oldtmp = 0;
					for (float i=lx; i<=ux; i++) {
						if (this.isInTheScreen(i, j, 0)) {
							dx = i - posx;
							dy = posy - j;
							double dxdy2 = dx * dx + dy * dy;

							if (dxdy2 <= r2) {
								// Obtain planetographic position
								double rr = Math.sqrt(dxdy2)/r;
								double dz0 = Math.sqrt(r2 - dxdy2);
								double ang = FastMath.atan2_accurate(dy, dx);
								dx = -rr * FastMath.cos(ang - incl_up);
								dy = -rr * FastMath.sin(ang - incl_up) / oblateness;
								dxdy2 = dx * dx + dy * dy;
								if (dxdy2 <= 1.0)
								{
									double tmp = dxdy2;

									double dz = Math.sqrt(1.0 - dxdy2);
									double tmp2 = dy * sinp + dz * cosp;
									dy = dy * cosp - dz * sinp;
									dz = tmp2;
									double lat = FastMath.atan2_accurate(dx, dz);
									double lon = a0minus2pi - lat;
									if (lon < 0.0) lon += Constant.TWO_PI;
									lat = FastMath.asin(-dy / Math.sqrt(dx * dx + dy * dy + dz * dz));

									int jindex = (int)(0.5+(Constant.PI_OVER_TWO - lat) * thpi);
									int iindex = (int)(0.5+lon*tw2pi);
									if (iindex >= tw) iindex -= tw;
									if (ih) iindex = texture_width-1-iindex;
									if (iv) jindex = texture_height-1-jindex;
									int c = g.getRGB(img, iindex, jindex);
									if (dxdy2 > SaturnLimbFactor && render.target == TARGET.SATURN) {
										int red = 0xff & (c >> 16);
										int green = 0xff & (c >> 8);
										int blue = 0xff & c;
										blue = blue + (int)((255.0-blue)*(dxdy2-SaturnLimbFactor)/0.05);
										if (blue > 255) blue = 255;
							    		c = 255<<24 | red<<16 | green<<8 | blue;
									}
									setPixel(i, j, c, white, sun_x, sun_y, sun_z, dz0, r2, br, bg, bb, g, render.target, img2, iindex, jindex);

									// Save pixel for later use if we have to apply transparency
									// effects on rings
									float d = getDist(dz0/r, refz);
									drawPoint(i, j, d, true, g, dubois);

									// FIXME: Dead code in Android. MUST BE REMOVED FOR ANDROID, OTHERWISE
									// ART WILL NOT COMPILE IT AND INSTALLATION WILL BE IMPOSSIBLE !!!
									// THIS ALSO FIXES SOME ISSUES IN DALVIK ...
									if (r > 7 && doit && !g.renderingToAndroid()) {
										tmp = Math.sqrt(tmp);
										if (first) {
											first = false;
											oldi = iindex;
											oldj = jindex;
											oldz = dz0;
											i++;
										} else {
											int oldii = oldi;
											oldi = (oldi + iindex)/2;
											if (Math.abs(oldii-iindex) > tw/10) {
												oldi += tw / 2;
												if (oldi >= tw) oldi -= tw;
											}
											oldj = (oldj + jindex)/2;
											oldz = (oldz + dz0)/2.0;
											c = g.getRGB(img, oldi, oldj);
											if (dxdy2 > SaturnLimbFactor && render.target == TARGET.SATURN) {
												int red = 0xff & (c >> 16);
												int green = 0xff & (c >> 8);
												int blue = 0xff & c;
												blue = blue + (int)((255.0-blue)*(dxdy2-SaturnLimbFactor)/0.05);
												if (blue > 255) blue = 255;
									    		c = 255<<24 | red<<16 | green<<8 | blue;
											}
											setPixel(i-1, j, c, white, sun_x, sun_y, sun_z, oldz, r2, br, bg, bb, g, render.target, img2, oldi, oldj);
											float z = getDist(oldz/r, refz);
											drawPoint(i-1, j, z, true, g, dubois);

/*											if (dubois && render.anaglyphMode.getEyeSeparation() > 1) {
												if (g.getRGBLeft((int)i-2, (int)j, z) == backgroundCol)
													g.fillOvalAnaglyphLeft(i-2, j, 1, 1, z);
												if (g.getRGBRight((int)i-2, (int)j, z) == backgroundCol)
													g.fillOvalAnaglyphRight(i-2, j, 1, 1, z);
												if (g.getRGBLeft((int)i, (int)j, z) == backgroundCol)
													g.fillOvalAnaglyphLeft(i, j, 1, 1, z);
												if (g.getRGBRight((int)i, (int)j, z) == backgroundCol)
													g.fillOvalAnaglyphRight(i, j, 1, 1, z);
											}
*/
											double ntmp = tmp + (tmp-oldtmp);
											if (tmp < 1.0 && ntmp > 1.0) {
												doit = false;
											} else {
												oldi = iindex;
												oldj = jindex;
												oldz = dz0;
												i++;
											}
										}
										oldtmp = tmp;
									}
								}
							}
						}
					}
				}
				// This is to occultate possible background stars behind the planet when gg
				// comes from sky rendering
				Object o = null, o2 = null;
				int rec[] = g.getClip();
				if (dubois) {
					Object li = g.getImage(rec[0], rec[1], rec[2], rec[3]);
					Object ri = g.getImage2(rec[0], rec[1], rec[2], rec[3]);
					//li = g.getScaledImage(li, gg.getWidth(), gg.getHeight(), true, ALLOW_SPLINE_RESIZING);
					//ri = g.getScaledImage(ri, gg.getWidth(), gg.getHeight(), true, ALLOW_SPLINE_RESIZING);
					o = li;
					o2 = ri;
					//o = g.blendImagesToAnaglyphMode(li, ri);
				} else {
					o = g.getRendering(rec[0], rec[1], rec[2], rec[3]);
					//if (hq) o = g.getScaledImage(o, gg.getWidth(), gg.getHeight(), true, ALLOW_SPLINE_RESIZING);
				}
				// Simulate refraction
				int ppy = rec[1];
				if (renderingSky && upperLimbFactor < 1f) {
					int de = (int) (scale * (1.0 - upperLimbFactor) + 0.5);
					if (de > 1) {
						o = g.getRotatedAndScaledImage(o, (posx-rec[0])*scaleFactor, (posy-rec[1])*scaleFactor, cenitAngle, 1.0f, 1.0f);
						int sizeo[] = g.getSize(o);
						o = g.getScaledImage(o, sizeo[0], (int)(sizeo[1]*upperLimbFactor), false, RenderPlanet.ALLOW_SPLINE_RESIZING);
						o = g.getRotatedAndScaledImage(o, (posx-rec[0])*scaleFactor, (posy-rec[1])*upperLimbFactor*scaleFactor, -cenitAngle, 1.0f, 1.0f);
						if (dubois) {
							o2 = g.getRotatedAndScaledImage(o2, (posx-rec[0])*scaleFactor, (posy-rec[1])*scaleFactor, cenitAngle, 1.0f, 1.0f);
							int sizeo2[] = g.getSize(o2);
							o2 = g.getScaledImage(o2, sizeo2[0], (int)(sizeo2[1]*upperLimbFactor), false, RenderPlanet.ALLOW_SPLINE_RESIZING);
							o2 = g.getRotatedAndScaledImage(o2, (posx-rec[0])*scaleFactor, (posy-rec[1])*upperLimbFactor*scaleFactor, -cenitAngle, 1.0f, 1.0f);
						}
						de = (int)((posy-rec[1])*(1.0/upperLimbFactor - 1.0));
						ppy += de;
					}
				}
				gg.disableInversion();
				if (dubois) {
					int sizeo[] = g.getSize(o);
					int sizeo2[] = g.getSize(o2);
					o = g.getScaledImage(o, (int)(sizeo[0] / scaleFactor), (int)(sizeo[1] / scaleFactor), true, RenderPlanet.ALLOW_SPLINE_RESIZING);
					o2 = g.getScaledImage(o2, (int)(sizeo2[0] / scaleFactor), (int)(sizeo2[1] / scaleFactor), true, RenderPlanet.ALLOW_SPLINE_RESIZING);
					gg.setAnaglyph(o, o2, rec[0], ppy);
				} else {
					gg.drawImage(o, rec[0], ppy, 1.0f/scaleFactor, 1.0f/scaleFactor);
				}
				gg.enableInversion();

				try {
					this.renderizeSatelliteShadows(g, posx, posy, incl_north, incl_up, scale, oblateness, render.ephem.distance, dlon, dlat);
					if (gg.renderingToExternalGraphics()) this.renderizeSatelliteShadows(gg, xPosition, yPosition, incl_north, incl_up, scale0, oblateness, render.ephem.distance, dlon, dlat);
				} catch (Exception e) {	}
			}
		} else
		{
			if (r < minScale) {
				posx = posx / scaleFactor;
				posy = posy / scaleFactor;
				scaleFactor = 1;
			}
			
			// Minimum size of 30 pixels for grid (planet without textures)
			if (planetVisible) {
				float dpi = 1;
				//if (g.renderingToAndroid()) dpi = 3;
				if (fastGridAndBye) {
					if (scale <= 30*dpi || (render.target == TARGET.SUN && !renderingSky)) {
						int min_r = r;
						if (min_r < 1)
							min_r = 1;
						gg.setColor(render.target == TARGET.SUN ? Graphics.COLOR_ORANGE_Orange : render.foreground, false);
//						gg.setColor(render.foreground, false);
						gg.fillOval(posx - min_r, posy - min_r, 2 * min_r+1, 2 * min_r+1, false);						
					}
					if (scale > 30*dpi)
						drawPlanetGrid(gg, incl_rotation, incl_pole, incl_up, scale, r, oblateness, posx, posy, dubois, refz);
				} else {
					if (scale <= 30*dpi || (render.target == TARGET.SUN && !renderingSky)) {
						int min_r = r;
						if (min_r < 1)
							min_r = 1;
						g.setColor(render.target == TARGET.SUN ? Graphics.COLOR_ORANGE_Orange : render.foreground, false);
	//					g.setColor(render.foreground, false);
						g.fillOval(posx - min_r, posy - min_r, 2 * min_r+1, 2 * min_r+1, false);
					}
					if (scale > 30*dpi)
						drawPlanetGrid(g, incl_rotation, incl_pole, incl_up, scale, r, oblateness, posx, posy, dubois, refz);
				}
			}
		}


		boolean textures = render.textures;
		if (!ringsTexturesVisible) render.textures = false;
		if (fastGridAndBye) {
			this.renderRings(gg, r, dubois, scale, diameter, oblateness, refz, backgroundCol, posx, posy, subslat, dlon, incl_up, incl_pole, distCenter, timesOut, true, true, true);
			render.textures = textures;
			try {
				this.renderizeSatellites(gg, posx, posy, incl_north, incl_up, scale, oblateness, render.ephem.distance, dlon, dlat);
			} catch (Exception e) {	}
			this.renderAxes(g, posx, posy, r, incl_north, incl_up, refz, scaleFactor);
			return;
		} else {
			this.renderRings(g, r, dubois, scale, diameter, oblateness, refz, backgroundCol, posx, posy, subslat, dlon, incl_up, incl_pole, distCenter, timesOut, true, true, true);
			if (gg.renderingToExternalGraphics()) this.renderRings(gg, (int)(r/scaleFactor), dubois, scale0, diameter, oblateness, refz, backgroundCol, xPosition, yPosition, subslat, dlon, incl_up, incl_pole, distCenter, timesOut, true, true, true);
		}
		render.textures = textures;

		int rec[] = g.getClip();
		if (planetVisible && renderingSky && (render.textures || render.target == TARGET.SUN) && scale > minScale) {
			if (!this.isInTheScreen(posx, posy, (int)scale)) {
				lastRenderElement = null;
				lastRender = null;
			} else {
				lastRenderElement = render.clone();
				lastRender = g.getGraphics();
				lastRender.disableInversion();
				lastRender.setColor(backgroundCol, false);
				lastRender.fillRect(0, 0, lastRender.getWidth(), lastRender.getHeight());
				// Render sky (if gg is that) to remove possible background stars inside the planet
				// (it was cleared before, we have to save it again here for fast repaint)
				lastRender.drawImage(g.getRendering(rec[0], rec[1], rec[2], rec[3]), rec[0], rec[1]);
				lastRender.disableAnaglyph();
				lastPosX = posx * scaleFactor;
				lastPosY = posy * scaleFactor;
				lastScale = scale0;
			}
		}

		if (!renderingSky) {
			try {
				this.renderizeSatellites(g, posx, posy, incl_north, incl_up, scale, oblateness, render.ephem.distance, dlon, dlat);
				if (gg.renderingToExternalGraphics()) this.renderizeSatellites(gg, xPosition, yPosition, incl_north, incl_up, scale0, oblateness, render.ephem.distance, dlon, dlat);
			} catch (Exception e) {	}
		}

		if (render.textures && render.difraction)
		{
			g.disableAntialiasing();
			scale = g.getWidth() * 0.25f;
			double difraction_scale_factor = 0;
			int difraction_pattern_field = (int) (150.0 / (double) render.telescope.diameter);
			if (difraction_pattern_field < 1) difraction_pattern_field = 1;
			difraction_scale_factor = (double) difraction_pattern_field * scale * 0.5 / (render.ephem.angularRadius * Constant.RAD_TO_ARCSEC);
			difraction_scale_factor = (double) difraction_pattern_field * 0.5 * g.getWidth() / field;
			// Set the size of the difraction pattern to apply, taking into account
			// the resolution of the telescope
			double difraction_pattern[][] = Difraction.pattern(render.telescope, difraction_pattern_field);

			int screenout[][][] = convolve(difraction_pattern, difraction_scale_factor, difraction_pattern_field, g, dubois, rec);
			int cromatic_aberration = (int) (render.telescope.cromatismLevel * 0.5 * g.getWidth() / field);
			int red0 = g.getRed(render.background),
				//green0 = g.getGreen(render.background),
				blue0 = g.getBlue(render.background);
			int red, green, blue;
			for (int i = rec[0]; i < rec[0]+rec[2]; i++)
			{
				for (int j = rec[1]; j < rec[1]+rec[3]; j++)
				{
					red = red0;
					//green = green0;
					blue = blue0;
					if (j-rec[1] < (rec[3] - cromatic_aberration))
						red = screenout[0][i-rec[0]][j + cromatic_aberration-rec[1]];
					green = screenout[1][i-rec[0]][j-rec[1]];
					if (j-rec[1] >= cromatic_aberration)
						blue = screenout[2][i-rec[0]][j - cromatic_aberration-rec[1]];

					if (red > 255) red = 255;
					if (red < 0) red = 0;
					if (green > 255) green = 255;
					if (green < 0) green = 0;
					if (blue > 255) blue = 255;
					if (blue < 0) blue = 0;
					if ((red + green + blue) > 0)
					{
						g.setColor(red, green,blue, 255);
						if (g.getColor() != render.background) {
							g.fillOval(i, j, 1, 1, false);
						}
					}
				}
			}

			g.enableAntialiasing();
			if (renderingSky && scale > minScale) {
				Object o;
				if (dubois) {
					Object li = g.getImage(rec[0], rec[1], rec[2], rec[3]);
					Object ri = g.getImage2(rec[0], rec[1], rec[2], rec[3]);
					//li = g.getScaledImage(li, gg.getWidth(), gg.getHeight(), true, ALLOW_SPLINE_RESIZING);
					//ri = g.getScaledImage(ri, gg.getWidth(), gg.getHeight(), true, ALLOW_SPLINE_RESIZING);
					o = g.blendImagesToAnaglyphMode(li, ri);
				} else {
					o = g.getRendering(rec[0], rec[1], rec[2], rec[3]);
					//if (hq) o = g.getScaledImage(o, gg.getWidth(), gg.getHeight(), true, ALLOW_SPLINE_RESIZING);
				}
				lastRender.drawImage(o, rec[0], rec[1]);
			}
		}

		// Renderize axes
		if (planetVisible) this.renderAxes(g, posx, posy, r, incl_north, incl_up, refz, scaleFactor);

		if (!renderingSky && !useSkySatPos && render.showLabels && satR.length > 0) {
			int offsetLabel = g.getFont().getSize() * 2 / 3;
			g.setColor(render.foreground, false);
			for (int i=0; i<satR.length; i++) {
				MoonEphemElement m = render.moonephem[satID[i]];
				g.drawString(m.name, (int) satX[i], (int) (satY[i] + 3*satR[i]+offsetLabel), satZ[i]);
				if (gg.renderingToExternalGraphics()) gg.drawString(m.name, (int) satX[i]/scaleFactor, (int) (satY[i] + 3*satR[i]+offsetLabel)/scaleFactor, satZ[i]);
			}
		}

		Object o, o2 = null;
		if (dubois) {
			Object li = g.getImage(rec[0], rec[1], rec[2], rec[3]);
			Object ri = g.getImage2(rec[0], rec[1], rec[2], rec[3]);
			//li = g.getScaledImage(li, gg.getWidth(), gg.getHeight(), true, ALLOW_SPLINE_RESIZING);
			//ri = g.getScaledImage(ri, gg.getWidth(), gg.getHeight(), true, ALLOW_SPLINE_RESIZING);
			//o = g.blendImagesToAnaglyphMode(li, ri);
			g.setAnaglyph(li, ri);
			o = g.getImage(rec[0], rec[1], rec[2], rec[3]);
			o2 = g.getImage2(rec[0], rec[1], rec[2], rec[3]);
		} else {
			o = g.getRendering(rec[0], rec[1], rec[2], rec[3]);

			// I use this code to improve quality in renderings with white background in an external program
			if (FORCE_WHITE_BACKGROUND) {
				int col[] = g.getImageAsPixels(o);
				g.setColor(255, 255, 255, 255);
				int white = g.getColor();
				for (int i=0; i<col.length; i++) {
					int cr = g.getRed(col[i]);
					int cg = g.getGreen(col[i]);
					int cb = g.getBlue(col[i]);
					if (cr == 0 && cb == 0 && cg == 0) {
						col[i] = white;
					}
				}
				o = g.getImage(rec[2], rec[3], col);
			}

			//if (hq) o = g.getScaledImage(o, gg.getWidth(), gg.getHeight(), true, ALLOW_SPLINE_RESIZING);
		}

		// Simulate refraction
		float sf = 1.0f;
		boolean imgResized = false;
//		if (!dubois) {
		if (planetVisible && renderingSky && upperLimbFactor < 1f && lastRender != null) {
			int de = (int) (scale * (1.0 - upperLimbFactor) + 0.5);
			if (de > 1) {
				int cl[] = g.getClip();
				try {
					o = g.getImage(o, cl[0]-rec[0], cl[1]-rec[1], cl[2], cl[3]); // This can launch error (outside raster)
					if (dubois && o2 != null) o2 = g.getImage(o2, cl[0]-rec[0], cl[1]-rec[1], cl[2], cl[3]);
					cl = gg.getInvertedRectangle(cl);
					rec[0] = cl[0];
					rec[1] = cl[1];

					o = gg.getRotatedAndScaledImage(o, (posx-rec[0])*sf, (posy-rec[1])*sf, cenitAngle, 1.0f, 1.0f);
					int sizeo[] = gg.getSize(o);
					o = gg.getScaledImage(o, sizeo[0], (int)(sizeo[1]*upperLimbFactor), false, RenderPlanet.ALLOW_SPLINE_RESIZING);
					o = gg.getRotatedAndScaledImage(o, (posx-rec[0])*sf, (posy-rec[1])*upperLimbFactor*sf, -cenitAngle, 1.0f, 1.0f);
					if (dubois && o2 != null) {
						o2 = gg.getRotatedAndScaledImage(o2, (posx-rec[0])*sf, (posy-rec[1])*sf, cenitAngle, 1.0f, 1.0f);
						int sizeo2[] = gg.getSize(o2);
						o2 = gg.getScaledImage(o2, sizeo2[0], (int)(sizeo2[1]*upperLimbFactor), false, RenderPlanet.ALLOW_SPLINE_RESIZING);
						o2 = gg.getRotatedAndScaledImage(o2, (posx-rec[0])*sf, (posy-rec[1])*upperLimbFactor*sf, -cenitAngle, 1.0f, 1.0f);
					}
					de = (int)((posy-rec[1])*(1.0/upperLimbFactor - 1.0));
					rec[1] += de;

					gg.setColor(backgroundCol, 255);
					gg.fillRect(posx-scale*2, posy-scale*2, scale*4, scale*4);
					if (lastRender == null) {
						if (dubois && o2 != null) {
							sizeo = g.getSize(o);
							int sizeo2[] = g.getSize(o2);
							o = g.getScaledImage(o, (int)(sizeo[0] / sf), (int)(sizeo[1] / sf), true, RenderPlanet.ALLOW_SPLINE_RESIZING);
							o2 = g.getScaledImage(o2, (int)(sizeo2[0] / sf), (int)(sizeo2[1] / sf), true, RenderPlanet.ALLOW_SPLINE_RESIZING);
							gg.setAnaglyph(o, o2, rec[0], rec[1]);
						} else {
							gg.drawImage(o, rec[0], rec[1], 1.0f / sf, 1.0f / sf);
						}
					} else {
						lastRender.setColor(backgroundCol, 255);
						lastRender.disableInversion();
						lastRender.fillRect(0, 0, lastRender.getWidth(), lastRender.getHeight());
						lastRender.enableInversion();
						if (dubois && o2 != null) {
							sizeo = g.getSize(o);
							int sizeo2[] = g.getSize(o2);
							o = g.getScaledImage(o, (int)(sizeo[0] / sf), (int)(sizeo[1] / sf), true, RenderPlanet.ALLOW_SPLINE_RESIZING);
							o2 = g.getScaledImage(o2, (int)(sizeo2[0] / sf), (int)(sizeo2[1] / sf), true, RenderPlanet.ALLOW_SPLINE_RESIZING);
							lastRender.setAnaglyph(o, o2, rec[0], rec[1]);
						} else {
							lastRender.drawImage(o, rec[0], rec[1], 1.0f / sf, 1.0f / sf);
						}
						if (lastRenderElement != null) lastRenderElement.textures = render.textures;
					}
				} catch (Exception exc) {}
			}
		}

		if (o != null) {
			if (!imgResized) {
				int[] sizeo = g.getSize(o);
				o = g.getScaledImage(o, (int)(sizeo[0] / scaleFactor), (int)(sizeo[1] / scaleFactor), true, RenderPlanet.ALLOW_SPLINE_RESIZING);
				if (o2 != null) {
					int[] sizeo2 = g.getSize(o2);
					o2 = g.getScaledImage(o2, (int)(sizeo2[0] / scaleFactor), (int)(sizeo2[1] / scaleFactor), true, RenderPlanet.ALLOW_SPLINE_RESIZING);
				}
			}
			gg.setAnaglyph(o, o2, rec[0] / scaleFactor, rec[1] / scaleFactor); //, 1.0f / scaleFactor, 1.0f / scaleFactor);
		}
//		}

		scale = scale0;
		posx = (int) (xPosition);
		posy = (int) (yPosition);

		if (renderingSky) {
			scaleFactor = 1.0f;
			try {
				this.renderizeSatellites(gg, posx, posy, incl_north, incl_up, scale, oblateness, render.ephem.distance, dlon, dlat);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Re-scale planet size and satellite positions for correct planetographic coordinates from RenderPlanet
		planet_size /= sf;
		if (satX != null) {
			for (int i=0; i<satX.length; i++) {
				satX[i] /= sf;
				satY[i] /= sf;
				satR[i] /= sf;
			}
		}
	}

	static void updateRendering(Graphics gg, float newScale) {
		if (lastRender == null) return;

		if (newScale < lastScale) lastScale = newScale;
		lastRender.disableInversion();
		gg.disableInversion();
		int rec2[] = gg.getClip();
		double scale = (double) lastRender.getWidth() / (double) gg.getWidth();
		if (scale == 1) {
			lastRender.drawImage(gg.getRendering(rec2[0], rec2[1], rec2[2], rec2[3]), rec2[0], rec2[1]);
			//Object o = lastRender.getRendering();
			//gg.setAnaglyph(o, o);
		} else {
			lastRender.drawImage(gg.getRendering(rec2[0], rec2[1], rec2[2], rec2[3]), (int)(rec2[0]*scale), (int)(rec2[1]*scale), scale, scale);
			Object o = gg.getScaledImage(lastRender.getRendering(), gg.getWidth(), gg.getHeight(), true, RenderPlanet.ALLOW_SPLINE_RESIZING);
			gg.setAnaglyph(o, o);
		}
		gg.enableInversion();
		//lastRender.disableAnaglyph();
	}

	private void setPixel(float i, float j, int c, int white,
			double sun_x, double sun_y, double sun_z, double dz0, double r2, int br, int bg, int bb, Graphics g,
			TARGET target, Object img2, int iindex, int jindex) {
		// Treat RGB compounds of the pixel
		int red = 0xff & (c >> 16);
		boolean rgb = ((0xff & (render.foreground >> 16)) <= (0xff & (render.foreground >> 8)));
		int green = 0, blue = 0;
		if (rgb) {
			green = 0xff & (c >> 8);
			blue = 0xff & c;
		}

		if (target != TARGET.SUN && this.showDayAndNight) {
			double ry = 1.0;

			double z02 = ((sun_x - i) * (sun_x - i) + (sun_y - j) * (sun_y - j) + (-sun_z - dz0) * (-sun_z - dz0)) / r2;

			// Apply a model of illumination, more or less based on
			// physical arguments and to achieve a realistic visual impression
			if (z02 <= 4.0) {
				//ry = FastMath.pow((Math.acos(1.0 - z02 * 0.5) / Constant.PI_OVER_TWO), 2) * 0.93;
				if (target != null && (target.compareTo(TARGET.JUPITER) >= 0 && target.compareTo(TARGET.NEPTUNE) <= 0)) {
					ry = z02 * 0.52; // Gaseous planets
				} else {
					ry = z02 * 0.45; // Terrestrial planets
				}
				if (render.background == white) ry = -ry;
			}

			// Simulate Moon illuminated by Earth
			if (illuminateMoonByEarth && target != null && ry > 0.9 && sun_z > 0 && z02 <= 4.0 && (target.isNaturalSatellite() || target == TARGET.Moon)) { // && target == TARGET.Moon) {
				double dang = sun_z / Math.sqrt(r2);
				if (dang > 0.33) {
					ry = 0.9;
				} else {
					ry = ry + (0.9 - ry) * Math.pow(dang * 3.0, 0.2);
				}
			}

			// Decrease/Increase brightness carefully
			red = (red - (int) (red * ry));
			if (rgb) green = (green - (int) (green * ry));
			if (target != null && (target == TARGET.SATURN || target == TARGET.URANUS || target == TARGET.NEPTUNE)) ry = ry * 0.95;
			if (target != null && target == TARGET.JUPITER) ry = ry * 0.85; // 15% more blue in the edge of Jupiter, see HST images
			if (rgb) blue = (blue - (int) (blue * ry));

			// Add Earth lights at night in dark side
			if (img2 != null) {
				int rgbc = g.getRGB(img2, iindex, jindex);
				int red2 = 0xff & (rgbc >> 16);
				red = (int)(red + Math.pow(red2, ry+.5) * ry * 0.5 / 10.0);
				if (rgb) {
					int green2 = 0xff & (rgbc >> 8);
					int blue2 = 0xff & rgbc;
					green = (int)(green + Math.pow(green2, ry+.5) * ry * 0.5 / 10.0);
					blue = (int)(blue + Math.pow(blue2, ry+.5) * ry * 0.5 / 10.0);
				}
			}

			if (red < 1) red = 1;
			if (red > 254) red = 254;
			if (rgb) {
				if (green < 1) green = 1;
				if (blue < 1) blue = 1;
				if (green > 254) green = 254;
				if (blue > 254) blue = 254;
			}
		}

		if (red == br && green == bg && blue == bb) blue ++;

		// Compound RGB color
		g.setColor(red, green, blue, 255);
	}

	private float getDist(double z, double refz) {
		if (Double.isNaN(z)) z = 0;
		// input z > 0 => towards observer, in planetary radii
		//double out = Math.log10(Math.abs(z)) * refz * 0.5;
		//if (z < 0) out = -out;
		return (float) (refz - z);
	}

	boolean isVisible(double x, double y, int size, MoonEphemElement m)
	{
		int px = (int) (x+0.5);
		int py = (int) (y+0.5);
		boolean isVisible = false; //isInTheScreen(px, py, 0);

		if (px >= -size && px < render.width*scaleFactor+size && py >= -size && py < render.height*scaleFactor+size)
			isVisible = true;

		if (m == null) return isVisible;

		if (m.eclipsed || m.occulted) {
			if (!renderingSky || render.textures) isVisible = false;
		}

		return isVisible;
	}

	boolean isInTheScreen(double x, double y, int size)
	{
		if (x > 0 && x < ((render.width-1)*scaleFactor) && y > size && y < ((render.height-1)*scaleFactor))
			return true;

		if (x > size && x < ((render.width-1)*scaleFactor - size) && y > size && y < ((render.height-1)*scaleFactor - size))
			return true;

		return false;
	}

	void drawPoint(double dx, double dy, double dz, boolean duboisBigger, Graphics g, boolean dubois)
	{
		if (isInTheScreen(dx, dy, 0)) {
			if (!dubois) {
				g.fillOval((float)dx, (float)dy, 1, 1, false);
				return;
			}

			if (dubois) {
				float dr = 1;
				if (duboisBigger) dr = 1.05f;
				g.fillOval((float)dx, (float)dy, dr, dr, (float)dz);
			} else {
				g.fillOval((float)dx, (float)dy, 1, 1, false);
			}
		}
	}

	/**
	 * Transforms a given position xyz in planetary radii from Sun-centered coordinates to Earth-centered.
	 * @param xyz Position.
	 * @param dlon Bright limb angle.
	 * @param dlat Phase angle.
	 * @return Earth-centered coordinates.
	 */
	private static double[] fromPlanetEquatorToFromOtherDirection(double xyz[], double dlon, double dlat) {
		double px = xyz[0], py = xyz[1], pz = xyz[2];

		if (dlon != 0.0) {
			double pang = FastMath.atan2_accurate(pz, px); // Note FastMath.atan2 would produce errors when rendering Saturn rings
			double pr = FastMath.hypot(px, pz); //Math.sqrt(px * px + pz * pz);
			double pa = pang + dlon;
			px = pr * FastMath.cos(pa);
			pz = pr * FastMath.sin(pa);
		}

		if (dlat != 0.0) {
			double pang = FastMath.atan2_accurate(py, pz); // Note FastMath.atan2 would produce errors when rendering Saturn rings
			double pr = FastMath.hypot(py, pz);
			double pa = pang + dlat;
			pz = pr * FastMath.cos(pa);
			py = pr * FastMath.sin(pa);
		}

		return new double[] {px, py, pz};
	}

	void drawOvalShadow(double posx, double sx, double posy, double sy, MoonEphemElement m, EphemElement e, double incl_up, double scale, double dr,
			Graphics g, double sun_dr, double dlon, double dlat)
	{
		if (isVisible((int) (posx + sx + 0.5), (int) (posy + sy + 0.5), (int) (dr + sun_dr), null))
		{
			int pixel = g.getColor();
			// Get position of the center of the shadow respect to planet axis
			double salfa = FastMath.atan2_accurate(sy, sx);
			double srr = FastMath.hypot(sx, sy); //Math.sqrt(sx*sx+sy*sy);
			double sdx0 = srr * FastMath.cos(salfa + incl_up);
			double sdy0 = srr * FastMath.sin(salfa + incl_up);

			float refz = render.anaglyphMode.getReferenceZ();
			double sampling = (int) (dr + sun_dr); // size of shadow + penumbra. Penumbra exagerated to render softly the edges
			double scale2 = scale * scale;
			double sampling2 = sampling * sampling / scale2;
			double step = 0.5;

			g.disableInversion();
			Object screenCopy = g.cloneImage(g.getImage(0, 0, g.getWidth(), g.getHeight())), screenCopy2 = null;
			boolean dubois = false;
			if (render.anaglyphMode.isReal3D()) {
				dubois = true;
				screenCopy2 = g.cloneImage(g.getImage2(0, 0, g.getWidth(), g.getHeight()));
			}
			g.enableInversion();
			for (double i = -sampling; i <= sampling; i = i + step)
			{
				double ssx = sx + i;
				boolean first = true;
				for (double j = -sampling; j <= 0; j = j + step)
				{
					double ssy = (sy + j);

					this.doShadow(g, first, ssx, ssy, scale, m, sampling, sampling2, sdx0, sdy0, incl_up, posx, posy, dlon, dlat, refz, dubois, pixel, dr, scale2, screenCopy, screenCopy2);
					first = false;
				}
				first = true;
				for (double j = sampling; j > 0; j = j - step)
				{
					double ssy = (sy + j);

					this.doShadow(g, first, ssx, ssy, scale, m, sampling, sampling2, sdx0, sdy0, incl_up, posx, posy, dlon, dlat, refz, dubois, pixel, dr, scale2, screenCopy, screenCopy2);
					first = false;
				}
			}
		}
	}

	private void doShadow(Graphics g, boolean first, double ssx, double ssy, double scale, MoonEphemElement m,
			double sampling, double sampling2, double sdx0, double sdy0, double incl_up, double posx, double posy,
			double dlon, double dlat, float refz, boolean dubois, int pixel, double dr, double scale2, Object screenCopy,
			Object screenCopy2) {
		// Get position around the center of the shadow respect to planet axis
		double salfa = FastMath.atan2_accurate(ssy, ssx);
		double srr = FastMath.hypot(ssx, ssy); //Math.sqrt(ssx*ssx+ssy*ssy);
		double sdx = (srr * FastMath.cos(salfa + incl_up) - sdx0) / scale;
		double sdy = (srr * FastMath.sin(salfa + incl_up) - sdy0) / scale;
		double sdr = sdx * sdx + sdy * sdy;

		// if we are inside shadow + penumbra ...
		if (sdr <= sampling2) {
			if (first) {
				sdr = sampling;
			} else {
				sdr = Math.sqrt(sdr)*scale;
			}

			// OK, now get position of satellite respect to sun and project it onto
			// planet surface, just calculating the adequate value of z = cos beta (r = 1 on planet).
			double x = m.xPositionFromSun+sdx;
			double y = -m.yPositionFromSun+sdy;
			// not using x^2+y^2+z^2 = 1 neither obtaining cosb from y since y depends on oblateness...
			double a = FastMath.atan2_accurate(y, x);
			double sinb = x / FastMath.cos(a);
			double cosb = Math.sqrt(1.0 - sinb * sinb);

			// Now pass the vision from Sun to from Earth to see where is this point around the shadow center.
			// From Sun the apparent shape is round, but from Earth not necessarily.
			double xyz[] = fromPlanetEquatorToFromOtherDirection(new double[] {x, y, cosb}, dlon, dlat);
			if (xyz[2] < 0.0) return;

			// Rotate back again from planet axis to current orientation, and set position in pixels
			srr = FastMath.hypot(xyz[0], xyz[1])*scale; //Math.sqrt(xyz[0]*xyz[0]+xyz[1]*xyz[1])*scale;
			salfa = FastMath.atan2_accurate(xyz[1], xyz[0]);
			sdx = srr * FastMath.cos(salfa - incl_up);
			sdy = srr * FastMath.sin(salfa - incl_up);

			// Get shadow position
			int shadowX = (int) (posx + sdx + 0.5);
			int shadowY = (int) (posy + sdy + 0.5);

			if (isInTheScreen(shadowX, shadowY, 0)) {
				// After all this fun, now simply draw shadow and penumbra
				float sdz = getDist(Math.sqrt(scale2-srr*srr)/scale, refz);
				if (dubois) {
					try {
						if (g.getRGBLeft(screenCopy, shadowX, shadowY, sdz) == g.getRGBLeft(shadowX, shadowY, sdz)) { // dont process a pixel twice
/*							if (sdr<dr) {
								g.setColor(pixel, false);
								g.fillOvalAnaglyphLeft(shadowX, shadowY, 1, 1, sdz);
								if (texture_step == 1) { // More points to avoid gaps when rendering to a big size
									g.fillOvalAnaglyphLeft(shadowX-1, shadowY, 1, 1, sdz);
									g.fillOvalAnaglyphLeft(shadowX, shadowY-1, 1, 1, sdz);
									g.fillOvalAnaglyphLeft(shadowX-1, shadowY-1, 1, 1, sdz);
								}
							} else {
*/								// Reduce intensity from penumbra to edge softly
								int c = g.getRGBLeft(shadowX, shadowY, sdz);
								int red = g.getRed(c);
								int green = g.getGreen(c);
								int blue = g.getBlue(c);

								float penumbraIntensity = (0.3f + 0.7f * (float) ((sdr-dr)/(sampling-dr)));
								if (penumbraIntensity < 0.0) penumbraIntensity = 0.0f;
								if (penumbraIntensity < 1.0) {
									red = (int) (penumbraIntensity*red);
									green = (int) (penumbraIntensity*green);
									blue = (int) (penumbraIntensity*blue);
								}
								g.setColor(red, green, blue, 255);
								g.fillOvalAnaglyphLeft(shadowX, shadowY, 1, 1, sdz);
								if (texture_step == 1) { // More points to avoid gaps when rendering to a big size
									g.fillOvalAnaglyphLeft(shadowX-1, shadowY, 1, 1, sdz);
									g.fillOvalAnaglyphLeft(shadowX, shadowY-1, 1, 1, sdz);
									g.fillOvalAnaglyphLeft(shadowX-1, shadowY-1, 1, 1, sdz);
								}
//							}
						}
					} catch (Exception exc) {}
					try {
						if (g.getRGBRight(screenCopy2, shadowX, shadowY, sdz) == g.getRGBRight(shadowX, shadowY, sdz)) { // dont process a pixel twice
/*							if (sdr<dr) {
								g.setColor(pixel, false);
								g.fillOvalAnaglyphRight(shadowX, shadowY, 1, 1, sdz);
								if (texture_step == 1) { // More points to avoid gaps when rendering to a big size
									g.fillOvalAnaglyphRight(shadowX-1, shadowY, 1, 1, sdz);
									g.fillOvalAnaglyphRight(shadowX, shadowY-1, 1, 1, sdz);
									g.fillOvalAnaglyphRight(shadowX-1, shadowY-1, 1, 1, sdz);
								}
							} else {
*/								// Reduce intensity from penumbra to edge softly
								int c = g.getRGBRight(shadowX, shadowY, sdz);
								int red = g.getRed(c);
								int green = g.getGreen(c);
								int blue = g.getBlue(c);

								float penumbraIntensity = (0.3f + 0.7f * (float) ((sdr-dr)/(sampling-dr)));
								if (penumbraIntensity < 0.0) penumbraIntensity = 0.0f;
								if (penumbraIntensity < 1.0) {
									red = (int) (penumbraIntensity*red);
									green = (int) (penumbraIntensity*green);
									blue = (int) (penumbraIntensity*blue);
								}
								g.setColor(red, green, blue, 255);
								g.fillOvalAnaglyphRight(shadowX, shadowY, 1, 1, sdz);
								if (texture_step == 1) { // More points to avoid gaps when rendering to a big size
									g.fillOvalAnaglyphRight(shadowX-1, shadowY, 1, 1, sdz);
									g.fillOvalAnaglyphRight(shadowX, shadowY-1, 1, 1, sdz);
									g.fillOvalAnaglyphRight(shadowX-1, shadowY-1, 1, 1, sdz);
								}
//							}
						}
					} catch (Exception exc) {}
				} else {
					if (g.getRGB(screenCopy, shadowX, shadowY) == g.getRGB(shadowX, shadowY)) { // dont process a pixel twice
/*						if (sdr<dr) {
							g.setColor(pixel, false);
							g.fillOval(shadowX, shadowY, 1, 1, sdz);
							if (texture_step == 1) { // More points to avoid gaps when rendering to a big size
								g.fillOval(shadowX-1, shadowY, 1, 1, sdz);
								g.fillOval(shadowX, shadowY-1, 1, 1, sdz);
								g.fillOval(shadowX-1, shadowY-1, 1, 1, sdz);
							}
						} else {
*/							// Reduce intensity from penumbra to edge softly
							int c = g.getRGB(shadowX, shadowY);
							int red = g.getRed(c);
							int green = g.getGreen(c);
							int blue = g.getBlue(c);

							float penumbraIntensity = (0.3f + 0.7f * (float) ((sdr-dr)/(sampling-dr)));
							if (penumbraIntensity < 0.0) penumbraIntensity = 0.0f;
							if (penumbraIntensity < 1.0) {
								red = (int) (penumbraIntensity*red);
								green = (int) (penumbraIntensity*green);
								blue = (int) (penumbraIntensity*blue);
							}
							g.setColor(red, green, blue, 255);
							g.fillOval(shadowX, shadowY, 1, 1, sdz);
//						}
					}
				}
			}
		}
	}
	int[][][] convolve(double pattern[][], double difraction_scale_factor, int difraction_pattern_field,
			Graphics g, boolean dubois, int rec[])
	{
		double field = (double) difraction_pattern_field * 0.5 * g.getWidth() / difraction_scale_factor;
		int[][] screen_in = new int[rec[2]][rec[3]];
		if (!dubois) {
			for (int i=rec[0]; i<rec[0]+rec[2]; i++) {
				for (int j=rec[1]; j<rec[1]+rec[3]; j++) {
					screen_in[i-rec[0]][j-rec[1]] = g.getRGB(i, j);
				}
			}
		} else {
			Object img = g.getRendering();
			for (int i=rec[0]; i<rec[0]+rec[2]; i++) {
				for (int j=rec[1]; j<rec[1]+rec[3]; j++) {
					screen_in[i-rec[0]][j-rec[1]] = g.getRGB(img, i, j);
				}
			}
		}
		return Difraction.convolve(pattern, difraction_pattern_field, screen_in, field, render.background);
	}

 	private void renderizeSatellites(Graphics g, float posx, float posy, double incl_north, double incl_up,
			double scale, double oblateness, double planetDistance,
			double dlon, double dlat) throws Exception {
		// Renderize satellites
		if (render.moonephem != null && render.satellitesMain && (render.target == TARGET.MARS || render.target == TARGET.NEPTUNE || render.target == TARGET.Pluto || render.target == TARGET.JUPITER || render.target == TARGET.SATURN || render.target == TARGET.URANUS))
		{
			MoonEphemElement moon[] = render.moonephem;
			LocationElement loc = new LocationElement(render.ephem.rightAscension, render.ephem.declination,
					render.ephem.distance);
			double plan_pos[] = LocationElement.parseLocationElement(loc);
			float planet_posx = posx;
			float planet_posy = posy;
			double planetSize = render.ephem.angularRadius / scale;
			double dx, dy;
			float refz = render.anaglyphMode.getReferenceZ();
			float planetRadius = (float) (render.target.equatorialRadius / Constant.AU);
			boolean dubois = false;
			if (render.anaglyphMode.isReal3D()) {
				dubois = true;
			}
			try
			{
				if (moon != null && moon.length > 0)
				{
					int isat;
					double brightest = -1;
					double isatOrdered[] = null;
					if (RenderPlanet.isatOrdered != null) isatOrdered = RenderPlanet.isatOrdered.clone();
					if (isatOrdered != null && isatOrdered.length != moon.length) isatOrdered = null;
					if (isatOrdered == null) {
						double isatID[] = new double[moon.length];
						double isatDist[] = new double[moon.length];
						for (int isat0 = 0; isat0 < moon.length; isat0++)
						{
							MoonEphemElement m = moon[isat0];
							isatID[isat0] = isat0;
							isatDist[isat0] = m.distance;
							if (m.magnitude < brightest || brightest == -1) brightest = m.magnitude;
						}
						ArrayList<double[]> al = DataSet.sortInDescent(isatDist, isatID, false);
						isatOrdered = al.get(1);
						RenderPlanet.isatOrdered = isatOrdered.clone();
					} else {
						for (int isat0 = 0; isat0 < moon.length; isat0++)
						{
							MoonEphemElement m = moon[isat0];
							if (m.magnitude < brightest || brightest == -1) brightest = m.magnitude;
						}
					}

					TARGET satTarget = null;
					//int adds = g.renderingToAndroid() ? 2:1;
					for (int isat0 = 0; isat0 < moon.length; isat0++)
					{
						isat = (int) isatOrdered[isat0];
						MoonEphemElement m = moon[isat];
						if (motherBody != null && m.name.equals(motherBody)) continue;
						satTarget = null;

						loc = new LocationElement(m.rightAscension, m.declination, m.distance);
						double pos[] = LocationElement.parseLocationElement(loc);
						double sat_pos[] = Functions.substract(plan_pos, pos);

						if (renderingSky && useSkySatPos)
						{
							float posSat[] = skySatPos.get(isat);
							if (posSat == null) continue;
							dx = posSat[0]*scaleFactor - planet_posx;
							dy = posSat[1]*scaleFactor - planet_posy;
						} else
						{
							double offset[] = MoonEphem.relativePosition(plan_pos, sat_pos);

							dx = -offset[0];
							dy = -offset[1];

							double rr = -FastMath.hypot(dx, dy) / planetSize; //Math.sqrt(dx * dx + dy * dy) / planetSize;
							double alfa = FastMath.atan2_accurate(dy, dx);
							dx = rr * FastMath.cos(alfa + incl_north);
							dy = rr * FastMath.sin(alfa + incl_north);
						}

						float r = (float) (m.angularRadius / planetSize);
						boolean isVisible = isVisible(planet_posx + dx, planet_posy + dy, (int) r, m);
						if (m.eclipsed || m.occulted) {
							if (!renderingSky || render.textures) isVisible = false;
						}


						// Render Satellite
 						float zpos = getDist(((planetDistance-m.distance))/planetRadius, refz);
						float size = r * (g.renderingToAndroid() ? 2:1);
						posx = planet_posx + (int) (dx + 0.5);
						posy = planet_posy + (int) (dy + 0.5);

						double sun_size = FastMath.atan2_accurate(TARGET.SUN.equatorialRadius,
								render.ephem.distanceFromSun * Constant.AU);

						if (isVisible && render.satellitesMain && !renderingSky)
						{
							satX = DataSet.addFloatArray(satX, new float[] {posx});
							satY = DataSet.addFloatArray(satY, new float[] {posy});
							satZ = DataSet.addFloatArray(satZ, new float[] {zpos});
							satR = DataSet.addFloatArray(satR, new float[] {r});
							satDist = DataSet.addFloatArray(satDist, new float[] {(float) m.distance});
							satID = DataSet.addIntegerArray(satID, new int[] {isat});
						}
						if (isVisible && render.satellitesMain)
						{
							int color = render.foreground, alpha = 192;
							if (m.phase < 0.5 && m.elongation-sun_size < m.angularRadius) {
								color = render.background;
								alpha = 255;
							}
							g.setColor(color, renderingSky? alpha:255);
							double mag = m.magnitude;
							double top = brightest + 1;
							if (mag < 1.0) mag = 1.0;
							//if (render.textures && !renderingSky && screen_out[posx][posy] == 0) drawPoint(posx, posy, g, screen_out);
							if (render.textures && !renderingSky && r <= minScale) {
								float size2 = (float) Math.pow(10.0, (top - mag) / 2.5);
								if (isInTheScreen(posx, posy, (int) (size2 + 1)))
									g.fillOval(posx-size2, posy-size2, 2*size2+1, 2*size2+1, zpos);
							}
							if (renderingSky && m.magnitude > 0 && size <= 2) {
								size = RenderSky.getSizeForAGivenMagnitude(m.magnitude, this.renderingSkyMagLim, hugeFactor);
								if (size < 0.5f * (g.renderingToAndroid() ? 2:1) && render.satellitesAll) {
									TARGET sat = Target.getID(m.name);
									if ((sat.getCentralBody() == TARGET.JUPITER && (sat.ordinal()-TARGET.Io.ordinal()) < 4) ||
											(sat.getCentralBody() == TARGET.SATURN && (sat.ordinal()-TARGET.Mimas.ordinal()) < 8) ||
											(sat.getCentralBody() == TARGET.URANUS && (sat.ordinal()-TARGET.Miranda.ordinal()) < 5) ||
											(sat.getCentralBody() == TARGET.NEPTUNE && (sat.ordinal()-TARGET.Triton.ordinal()) < 2))
										size = 0.5f * (g.renderingToAndroid() ? 2:1);
								}
							}
							boolean onlyOval = false;
							if (((r > render.width && !render.highQuality && !RenderPlanet.FORCE_HIGHT_QUALITY) ||
									(renderingSky && r > 900) || // XXX: Disable 4k rendering in sky (slow)
									(r > render.width/1.5 && g.renderingToAndroid())) && render.textures) onlyOval = true;
							if (!render.textures || r <= minScale || onlyOval) {
								if (size == 0 && (scaleFactor > 1 || g.renderingToAndroid())) size = 0.125f*(scaleFactor>1? scaleFactor:1);
								if (g.renderingToAndroid()) size ++;
								int fs = (int) (2*size+1);
								g.fillOval((posx - size), (posy - size), fs, fs, zpos);
								continue;
							}
						}
						if (render.textures && r > minScale && render.satellitesMain)
						{
							if (!isVisible) {
								if (m.eclipsed && !m.occulted) {
									g.setColor(0, 0, 0, 255);
									g.fillOval((posx - size), (posy - size), 2 * size+1, 2 * size+1, zpos);
								}
								continue;
							}
							double incl_pole2 = m.positionAngleOfPole;
							double incl_north2 = m.paralacticAngle;
							// FIXME: possible wrong north angle in case satellite is apparently far from planet
							if (renderingSky) incl_north2 = this.northAngle;
							if (render.northUp) incl_north2 = 0.0;
							double incl_axis2 = m.positionAngleOfAxis;
							double incl_rotation2 = m.longitudeOfCentralMeridian;
							double incl_bright_limb2 = m.brightLimbAngle;
							double phase_angle2 = m.phaseAngle;
							double incl_up2 = incl_axis2 - incl_north2;

							// Get the texture
							String s = Translate.translate(m.name, Translate.getDefaultLanguage(), Translate.LANGUAGE.ENGLISH);
							//int index = DataSet.getIndex(imgs, s);
							Object img = null;
//							if (index < 0) {
								String Iimg = FileIO.DATA_TEXTURES_DIRECTORY + s + ".jpg";
								img = g.getImage(Iimg);
								if (!g.renderingToAndroid() && !DataBase.dataExists(Iimg, true)) g.addToDataBase(g.cloneImage(img), Iimg, 0);
/*								images.add(img);
								imgs = DataSet.addStringArray(imgs, new String[] {s});
							} else {
								img = images.get(index);
							}
*/
							if (img == null) {
								g.fillOval((posx - size), (posy - size), 2 * size+1, 2 * size+1, zpos);
								continue;
							}

							int sizei[] = g.getSize(img);
							int texture_width = sizei[0];
							int texture_height = sizei[1];

							// Obtain sun illumination position
							double dr = (double) r * Math.abs(FastMath.sin(phase_angle2));
							double sun_x = posx + dr * FastMath.cos((-Constant.PI_OVER_TWO - incl_bright_limb2 + incl_north2));
							double sun_y = posy + dr * FastMath.sin((-Constant.PI_OVER_TWO - incl_bright_limb2 + incl_north2));
							double sun_z = r * Math.abs(FastMath.cos(phase_angle2));
							if (m.phase < 0.5) sun_z = -sun_z;

							int texture_step = (int) (texture_height / (3 * r));
							if (texture_step < 1)
								texture_step = 1;

							double a0 = incl_rotation2 - Constant.PI_OVER_TWO;

							float lx = ((int)(posx+0.5 - r - 1));
							float ux = ((int)(posx+0.5 + r + 1));
							float ly = ((int)(posy+0.5 - r - 1));
							float uy = ((int)(posy+0.5 + r + 1));
							int tw = texture_width, th = texture_height;
							int white = ((255&0x0ff)<<24)|((255&0x0ff)<<16)|((255&0x0ff)<<8)|(255&0x0ff);
							int br = g.getRed(render.background), bg = g.getGreen(render.background), bb = g.getBlue(render.background);
							double tw2pi = tw / Constant.TWO_PI;
							double a0minus2pi = Functions.normalizeRadians(Constant.PI_OVER_TWO - a0);
							double r2 = r * r;
							double thpi = (th-1) / Math.PI;
							double sinp = Math.sin(incl_pole2), cosp = Math.cos(incl_pole2);
							double planet_size2 = FastMath.pow((int)planet_size, 2.0);
							boolean pointEclipsed = false;
							double flattening2 = FastMath.pow(render.target.equatorialRadius / render.target.polarRadius, 2.0);
							for (float j=ly; j<=uy; j++) {
								boolean first = true, doit = true;
								if (m.mutualPhenomena != null && !m.mutualPhenomena.equals("")) doit = false;
								int oldi = 0, oldj = 0;
								double oldz = 0, oldtmp = 0;
								for (float i=lx; i<=ux; i++) {
									if (this.isInTheScreen(i, j, 0)) {
										dx = i - posx;
										dy = posy - j;
										double tmp = (dx * dx + dy * dy);

										if (tmp <= r2) {
											pointEclipsed = false;
											double spos[] = new double[] {m.xPosition, m.yPosition, m.zPosition};
											// Check if the point is eclipsed by the planet or another satellite
											double ang = FastMath.atan2_accurate(dy, dx);
											double tmp2 = Math.sqrt(tmp);
											double dx2 = tmp2 * FastMath.cos(ang - incl_up2 - incl_axis2);
											double dy2 = tmp2 * FastMath.sin(ang - incl_up2 - incl_axis2);

											spos[0] += dx2/scale;
											spos[1] += dy2/scale;
											pos = fromPlanetEquatorToFromOtherDirection(spos, dlon, -dlat);
											if (pos[2] > 0.0 && (pos[0]*pos[0]+pos[1]*pos[1]*flattening2) <= 1.0) pointEclipsed = true;
											if (m.mutualPhenomena != null && !m.mutualPhenomena.equals("") && !pointEclipsed) {
												for (int isat1 = 0; isat1 < moon.length; isat1++)
												{
													if (isat1 != isat0) {
														int isat2 = (int) isatOrdered[isat1];
														MoonEphemElement m1 = moon[isat2];
														if (pos[2] > m1.zPositionFromSun) {
															double spos1[] = new double[] {m1.xPositionFromSun, m1.yPositionFromSun, m1.zPositionFromSun};
															double dpx = spos1[0]-pos[0];
															double dpy = spos1[1]-pos[1];
															double d = FastMath.hypot(dpx, dpy)*scale; //Math.sqrt(dpx*dpx+dpy*dpy)*scale;
															float r1 = (float) (m1.angularRadius / planetSize);
															if (d < r1) {
																pointEclipsed = true;
																break;
															}
														}
													}
												}
											}

											double dz0 = Math.sqrt(r2 - tmp);
											if (pointEclipsed) {
												g.setColor(0, 0, 0, 255);
												//setPixel(i, j, g.getColor(), white, sun_x, sun_y, sun_z, -dz0, r2, br, bg, bb, g, satTarget);
												drawPoint(i, j, zpos, false, g, dubois);
												continue;
											}

											tmp = Math.sqrt(tmp) / r;

											// Obtain planetographic position
											dx = -tmp * FastMath.cos(ang - incl_up2);
											dy = -tmp * FastMath.sin(ang - incl_up2);

											double dz = dz0/r;
											tmp2 = dy * sinp + dz * cosp;
											dy = dy * cosp - dz * sinp;
											dz = tmp2;
											double lat = FastMath.atan2_accurate(dx, dz);
											double lon = a0minus2pi - lat;
											if (lon < 0) lon += Constant.TWO_PI;
											lat = FastMath.asin(-dy / Math.sqrt(dx * dx + dy * dy + dz * dz));
											int jindex = (int)(0.5+(Constant.PI_OVER_TWO - lat) * thpi);
											int iindex = (int)(0.5+lon*tw2pi);
											if (iindex >= tw) iindex -= tw;

											boolean both = false;
											if (m.distance > planetDistance) {
												if (dubois) {
													boolean onlyLeft = false, onlyRight = false;
													try {
														if (g.getRGBLeft((int)(i+0.5), (int)(j+0.5), zpos) != render.background)
															onlyLeft = true;
													} catch (Exception exc) {}
													try {
														if (g.getRGBRight((int)(i+0.5), (int)(j+0.5), zpos) != render.background)
															onlyRight = true;
													} catch (Exception exc) {}
													if (onlyLeft || onlyRight) both = true;
												} else {
													if (g.getRGB((int)(i+0.5), (int)(j+0.5)) != render.background) both = true;
												}
											}
											if (both) {
												double dxp = i - planet_posx;
												double dyp = (planet_posy - j);

												double e = FastMath.hypot(dxp, dyp); //Math.sqrt(dxp * dxp + dyp * dyp);
												double t = FastMath.atan2_accurate(dyp, dxp);
												dxp = e * FastMath.cos(t + incl_up);
												dyp = e * FastMath.sin(t + incl_up) * oblateness;
												double distToPlanet = (dxp * dxp + dyp * dyp);
												if (distToPlanet < planet_size2 && both) {
													if (r > 7 && doit && !first && dubois) {
														int oldii = oldi;
														oldi = (oldi + iindex)/2;
														if (Math.abs(oldii-iindex) > tw/10) {
															oldi += tw / 2;
															if (oldi >= tw) oldi -= tw;
														}
														oldj = (oldj + jindex)/2;
														oldz = (oldz + dz0)/2.0;
														setPixel(i-1, j, g.getRGB(img, oldi, oldj), white, sun_x, sun_y, sun_z, -oldz, r2, br, bg, bb, g, satTarget, null, oldi, oldj);
														drawPoint(i-1, j, zpos, false, g, dubois);
													}
													first = true;
													continue;
												} else {
													if (distToPlanet > planet_size2) {
														both = false;
													}
												}
											}

											setPixel(i, j, g.getRGB(img, iindex, jindex), white, sun_x, sun_y, sun_z, -dz0, r2, br, bg, bb, g, satTarget, null, iindex, jindex);
											drawPoint(i, j, zpos, false, g, dubois);

											if (r > 7 && doit) {
												if (first) {
													first = false;
													oldi = iindex;
													oldj = jindex;
													oldz = dz0;
													i++;
												} else {
													int oldii = oldi;
													oldi = (oldi + iindex)/2;
													if (Math.abs(oldii-iindex) > tw/10) {
														oldi += tw / 2;
														if (oldi >= tw) oldi -= tw;
													}
													oldj = (oldj + jindex)/2;
													oldz = (oldz + dz0)/2.0;
													setPixel(i-1, j, g.getRGB(img, oldi, oldj), white, sun_x, sun_y, sun_z, -oldz, r2, br, bg, bb, g, satTarget, null, oldi, oldj);
													drawPoint(i-1, j, zpos, false, g, dubois);
													double ntmp = tmp + (tmp-oldtmp);
													if (tmp < 1.0 && ntmp > 1.0) {
														doit = false;
													} else {
														oldi = iindex;
														oldj = jindex;
														oldz = dz0;
														i++;
													}
												}
												oldtmp = tmp;
											}
										}
									}
								}
							}
						}
					}
				}
			} catch (Exception e)
			{
				e.printStackTrace();
				throw new JPARSECException("invalid satellite ephemeris.", e);
			}
		}
	}

 	private void renderizeSatelliteShadows(Graphics g, float posx, float posy, double incl_north, double incl_up,
			double scale, double oblateness, double planetDistance,
			double dlon, double dlat) throws Exception {
		// Renderize satellites
 		if (render.satellitesMain && render.moonephem != null && scale > 10 && (render.target == TARGET.MARS || render.target == TARGET.NEPTUNE || render.target == TARGET.Pluto || render.target == TARGET.JUPITER || render.target == TARGET.SATURN || render.target == TARGET.URANUS))
		{
			MoonEphemElement moon[] = render.moonephem;
			LocationElement loc = new LocationElement(render.ephem.rightAscension, render.ephem.declination,
					render.ephem.distance);
			double plan_pos[] = LocationElement.parseLocationElement(loc);
			float planet_posx = posx;
			float planet_posy = posy;
			double planetSize = render.ephem.angularRadius / scale;
			double dx, dy;
			try
			{
				if (moon != null && moon.length > 0)
				{
					int isat;
					double brightest = -1;
					double isatOrdered[] = null;
					if (RenderPlanet.isatOrdered != null) isatOrdered = RenderPlanet.isatOrdered.clone();
					if (isatOrdered != null && isatOrdered.length != moon.length) isatOrdered = null;
					if (isatOrdered == null) {
						double isatID[] = new double[moon.length];
						double isatDist[] = new double[moon.length];
						for (int isat0 = 0; isat0 < moon.length; isat0++)
						{
							MoonEphemElement m = moon[isat0];
							isatID[isat0] = isat0;
							isatDist[isat0] = m.distance;
							if (m.magnitude < brightest || brightest == -1) brightest = m.magnitude;
						}
						ArrayList<double[]> al = DataSet.sortInDescent(isatDist, isatID, false);
						isatOrdered = al.get(1);
						//this.isatOrdered = isatOrdered.clone();
					} else {
						for (int isat0 = 0; isat0 < moon.length; isat0++)
						{
							MoonEphemElement m = moon[isat0];
							if (m.magnitude < brightest || brightest == -1) brightest = m.magnitude;
						}
					}

					TARGET satTarget = null;
					for (int isat0 = 0; isat0 < moon.length; isat0++)
					{
						isat = (int) isatOrdered[isat0];
						MoonEphemElement m = moon[isat];
						satTarget = null;

						loc = new LocationElement(m.rightAscension, m.declination, m.distance);
						double pos[] = LocationElement.parseLocationElement(loc);
						double sat_pos[] = Functions.substract(plan_pos, pos);

						if (renderingSky && useSkySatPos)
						{
							float posSat[] = skySatPos.get(isat);
							if (posSat == null) continue;
							dx = posSat[0]*scaleFactor - planet_posx;
							dy = posSat[1]*scaleFactor - planet_posy;
						} else
						{
							double offset[] = MoonEphem.relativePosition(plan_pos, sat_pos);

							dx = -offset[0];
							dy = -offset[1];

							double rr = -FastMath.hypot(dx, dy) / planetSize; //Math.sqrt(dx * dx + dy * dy) / planetSize;
							double alfa = FastMath.atan2_accurate(dy, dx);
							dx = rr * FastMath.cos(alfa + incl_north);
							dy = rr * FastMath.sin(alfa + incl_north);
						}

						float r = (float) (m.angularRadius / planetSize);

						// Render Satellite
						float size = r;
						posx = planet_posx + (int) (dx + 0.5);
						posy = planet_posy + (int) (dy + 0.5);

						double sun_size = FastMath.atan2_accurate(TARGET.SUN.equatorialRadius,
								render.ephem.distanceFromSun * Constant.AU);
						if (m.shadowTransiting && render.textures)
						{
							double sx = m.xPositionFromSun;
							double sy = -m.yPositionFromSun;
							double sr = FastMath.hypot(sx, sy); //Math.sqrt(sx * sx + sy * sy);
							double srr = sr * scale;
							double salfa = FastMath.atan2_accurate(sy, sx);
							double sdx = srr * FastMath.cos(salfa - incl_up);
							double sdy = srr * FastMath.sin(salfa - incl_up);

/*							if (upperLimbFactor < 1) {
								sdx = srr * FastMath.cos(salfa - incl_up + cenitAngle);
								sdy = srr * FastMath.sin(salfa - incl_up + cenitAngle) * upperLimbFactor;
								srr = FastMath.hypot(sdx, sdy);
								salfa = FastMath.atan2_accurate(sdy, sdx);
								sdx = srr * FastMath.cos(salfa - cenitAngle);
								sdy = srr * FastMath.sin(salfa - cenitAngle);
							}
*/
							satTarget = Target.getID(m.name);
							double satRadius = satTarget.equatorialRadius;
							double satPlanDistance = Math.sqrt(sat_pos[0] * sat_pos[0] + sat_pos[1] * sat_pos[1] + sat_pos[2] * sat_pos[2]) * Constant.AU - render.target.equatorialRadius;
							double sat_size = FastMath.atan2_accurate(satRadius,	satPlanDistance);
							float ssize = size;
							if (sat_size < sun_size) ssize = 0;
							double shadow_cone_dist = satRadius / FastMath.tan(sun_size);
							double rr = scale*(satTarget.equatorialRadius/render.target.equatorialRadius);
							double shadow_size = (rr * (1.0 - 0.5 * satPlanDistance / shadow_cone_dist));
							boolean shadow_isVisible = isVisible(planet_posx + sdx, planet_posy + sdy, (int)shadow_size, m);
							if (shadow_isVisible && ssize > 0)
							{
								int col = g.getColor();
								g.setColor(1, 1, 1, 255);
								drawOvalShadow( planet_posx, sdx,
												planet_posy, sdy,
												m,
												render.ephem,
												incl_up,
												scale,
												shadow_size,
												g,
												2 * (rr-shadow_size),
												dlon, dlat);
								g.setColor(col, false);
							}
						}
					}
				}
			} catch (Exception e)
			{
				e.printStackTrace();
				throw new JPARSECException("invalid satellite ephemeris.", e);
			}
		}
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
	 *        usually 2, since the Great Red Spot is in the tropical belt (1
	 *        refers to equatorial belt, and 3 to the rotation of the magnetic
	 *        field).
	 */
	public void setJupiterGRSLongitude(double GRS_lon, int system)
	{
		double lon0 = (270 / 2000.0) * Constant.TWO_PI;
		double offset = lon0 - GRS_lon;

		offsetInLongitudeOfJupiterGRS = offset;
		offsetInLongitudeOfJupiterGRS_system = system;
	}

	/**
	 * Empty constructor to be used from {@linkplain RenderSky} class.
	 */
	public RenderPlanet() {}

	/**
	 * Constructor for a planet render process.
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param render Render object.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public RenderPlanet(TimeElement time, ObserverElement obs, EphemerisElement eph,
			PlanetRenderElement render) throws JPARSECException
	{
		try
		{
			renderSky = new RenderSky(time, obs, eph, render);
			if (eph.targetBody.isNaturalSatellite()) {
				EphemerisElement eph2 = eph.clone();
				eph.algorithm = ALGORITHM.NATURAL_SATELLITE;
				render.ephem = Ephem.getEphemeris(time, obs, eph2, false);
			} else {
				render.ephem = PlanetEphem.MoshierEphemeris(time, obs, eph);
			}

			// Change event definition to limb to draw correctly the satellites when
			// they start to being occulted/eclipsed by the mother planet.
			EVENT_DEFINITION ed = MoonEvent.getEventDefinition();
			MoonEvent.setEventDefinition(EVENT_DEFINITION.AUTOMATIC_FOR_DRAWING);

			if (render.satellitesAll && render.satellitesMain)
			{
				render.moonephem = MoonEphem.calcAllSatellites(time, obs, eph, true);
			} else
			{
				//if (render.satellitesMain)
					render.moonephem = MoonEphem.calcAllSatellites(time, obs, eph, false);
			}

			MoonEvent.setEventDefinition(ed);

			render.target = eph.targetBody;
			EphemerisElement sun_eph = new EphemerisElement(TARGET.SUN, eph.ephemType,
					EphemerisElement.EQUINOX_OF_DATE, eph.isTopocentric, eph.ephemMethod, eph.frame);
			render.ephemSun = PlanetEphem.MoshierEphemeris(time, obs, sun_eph);

			this.render = render;
			this.setJupiterGRSLongitude(jparsec.ephem.event.MainEvents.getJupiterGRSLongitude(
					jparsec.time.TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME)), 2);
			xPosition = render.width / 2f;
			yPosition = render.height / 2f;
			if (obs.getMotherBody() != null) this.motherBody = obs.getMotherBody().getName();
		} catch (JPARSECException ve)
		{
			throw ve;
		}

	}

	/**
	 * Obtains planetographic, selenographic, or heliographic position for
	 * certain screen coordinates. Satellites are optionally considered.
	 *
	 * @param x Screen horizontal position in pixels.
	 * @param y Screen vertical position in pixels.
	 * @param coordinate_system Reference coordinate system, only for giant
	 *        planets: 1 (system I), 2 (system II), 3 (system III).
	 *  @param considerSatellites True to consider satellites. If a satellite is transiting
	 *  on (x, y) then the position on that satellite will be returned.
	 * @return The corresponding position if it is on the planet, or default
	 *         object (zero) else.
	 * @throws JPARSECException If an error occurs.
	 */
	public LocationElement getPlanetographicPosition(int x, int y, int coordinate_system, boolean considerSatellites)
			throws JPARSECException
	{
		if (this.render.anaglyphMode == ANAGLYPH_COLOR_MODE.TRUE_3D_MODE_LEFT_RIGHT_HALF_WIDTH) x /= 2;

		ArrayList<Object> planets = new ArrayList<Object>();
		planets.add(new float[] { xPosition, yPosition, planet_size/scaleFactor, (float) render.ephem.distance });
		planets.add(render.clone());
		if (considerSatellites) {
			for (int i=0; i<satX.length; i++) {
				if (satR[i] > 1) {
					planets.add(new float[] { satX[i]/scaleFactor, satY[i]/scaleFactor, -satR[i]/scaleFactor, satDist[i], satID[i] });
					planets.add(render.clone());
				}
			}
		}
		renderSky.planets = planets;
		renderSky.render = new SkyRenderElement();
		renderSky.render.width = render.width;
		renderSky.render.height = render.height;
		renderSky.render.telescope = render.telescope;
		renderSky.render.anaglyphMode = render.anaglyphMode;
		return renderSky.getPlanetographicPosition(x, y, coordinate_system, considerSatellites);
	}

	/**
	 * Obtain ID constant of an object in certain screen position. For a valid
	 * output it is previously necessary to render the sky, and the object must
	 * be visible.
	 *
	 * @param x Horizontal position in the rendering in pixels.
	 * @param y Horizontal position in the rendering in pixels.
	 * @param considerSatellites True to consider possible transiting
	 *        satellites, false to ignore them.
	 * @param minimumSize Minimum size in pixels of the planet to consider it
	 * as identified for the input coordinates. Set to 0 to allow identifying
	 * planets event when their disks are almost invisible because of their
	 * minimum apparent sizes.
	 * @return ID value of the object, or {@linkplain TARGET#NOT_A_PLANET} if no object is
	 *         found.
	 * @throws JPARSECException If an error occurs.
	 */
	public TARGET getPlanetInScreenCoordinates(int x, int y, boolean considerSatellites, int minimumSize) throws JPARSECException
	{
		ArrayList<Object> planets = new ArrayList<Object>();
		planets.add(new float[] { xPosition, yPosition, planet_size/scaleFactor, (float) render.ephem.distance });
		planets.add(render.clone());
		if (considerSatellites) {
			for (int i=0; i<satX.length; i++) {
				if (satR[i] > 1) {
					planets.add(new float[] { satX[i]/scaleFactor, satY[i]/scaleFactor, -satR[i]/scaleFactor, satDist[i], satID[i] });
					planets.add(render.clone());
				}
			}
		}
		renderSky.planets = planets;
		renderSky.render = new SkyRenderElement();
		renderSky.render.width = render.width;
		renderSky.render.height = render.height;
		renderSky.render.telescope = render.telescope;
		renderSky.render.anaglyphMode = render.anaglyphMode;
		return renderSky.getPlanetInScreenCoordinates(x, y, considerSatellites, minimumSize);
	}

	/**
	 * Obtain ID constant of the solar system object closest to certain screen position. For a valid
	 * output it is previously necessary to render the sky, and the object must
	 * be visible.
	 *
	 * @param x Horizontal position in the rendering in pixels.
	 * @param y Horizontal position in the rendering in pixels.
	 * @param considerSatellites True to consider possible transiting
	 *        satellites, false to ignore them.
	 * @return A array with three values (null if no object is found):<BR>
	 * ID value (TARGET) of the object.<BR>
	 * Minimum distance (Double) to the input position in pixels.<BR>
	 * Radius in pixels (Double) of the planet at input coordinates.
	 * @throws JPARSECException If an error occurs.
	 */
	public Object[] getClosestPlanetInScreenCoordinates(int x, int y, boolean considerSatellites) throws JPARSECException
	{
		ArrayList<Object> planets = new ArrayList<Object>();
		planets.add(new float[] { xPosition, yPosition, planet_size/scaleFactor, (float) render.ephem.distance });
		planets.add(render.clone());
		if (considerSatellites) {
			for (int i=0; i<satX.length; i++) {
				planets.add(new float[] { satX[i]/scaleFactor, satY[i]/scaleFactor, -satR[i]/scaleFactor, satDist[i], satID[i] });
				planets.add(render.clone());
			}
		}
		renderSky.planets = planets;
		renderSky.render = new SkyRenderElement();
		renderSky.render.width = render.width;
		renderSky.render.height = render.height;
		renderSky.render.telescope = render.telescope;
		renderSky.render.anaglyphMode = render.anaglyphMode;
		return renderSky.getClosestPlanetInScreenCoordinates(x, y, considerSatellites);
	}

	/**
	 * Obtain screen coordinates of a given planetographic, heliographic, or
	 * selenographic position in the current rendered body. Satellites are not
	 * considered.
	 *
	 * @param loc Planetographic/heliographic/selenographic coordinates on the
	 *        object.
	 * @param coordinate_system Reference coordinate system of input
	 *        coordinates, only for giant planets: 1 (system I), 2 (system II),
	 *        3 (system III).
	 * @param only_if_visible True for returning coordinates only if the input
	 *        position is visible.
	 * @return Corresponding position in the sky, or (0, 0) if the object is not
	 *         visible or the position is not visible and only_if_visible is set
	 *         to true.
	 * @throws JPARSECException If an error occurs.
	 */
	public float[] getScreenCoordinatesOfPlanetographicPosition(LocationElement loc, int coordinate_system,
			boolean only_if_visible) throws JPARSECException
	{
		ArrayList<Object> planets = new ArrayList<Object>();
		planets.add(new float[] { xPosition, yPosition, planet_size/scaleFactor, (float) render.ephem.distance});
		planets.add(render.clone());
		renderSky.planets = planets;
		renderSky.render = new SkyRenderElement();
		renderSky.render.width = render.width;
		renderSky.render.height = render.height;
		renderSky.render.telescope = render.telescope;
		renderSky.render.anaglyphMode = render.anaglyphMode;
		renderSky.firstTime = false;
		float pos[] = renderSky.getScreenCoordinatesOfPlanetographicPosition(loc, render.target,
				coordinate_system, only_if_visible);
		return pos;
	}

	float[] getScreenCoordinatesOfPlanetographicPositionForEclipse(LocationElement loc) throws JPARSECException
	{
		ArrayList<Object> planets = new ArrayList<Object>();
		planets.add(new float[] { xPosition, yPosition, planet_size/scaleFactor, (float) render.ephem.distance});
		planets.add(render); //.clone());
		renderSky.planets = planets;
		if (renderSky.render == null) renderSky.render = new SkyRenderElement();
		renderSky.render.width = render.width;
		renderSky.render.height = render.height;
		renderSky.render.telescope = render.telescope;
		renderSky.render.anaglyphMode = render.anaglyphMode;
		renderSky.firstTime = true;
		float pos[] = renderSky.getScreenCoordinatesOfPlanetographicPosition(loc, render.target,
				3, true);
		return pos;
	}

	/**
	 * Obtain screen coordinates of a given planetographic, heliographic, or
	 * selenographic position in the selected body.
	 *
	 * @param loc Planetographic/heliographic/selenographic coordinates on the
	 *        object.
	 * @param target The target body, can be a satellite.
	 * @param coordinate_system Reference coordinate system of input
	 *        coordinates, only for giant planets: 1 (system I), 2 (system II),
	 *        3 (system III).
	 * @param only_if_visible True for returning coordinates only if the input
	 *        position is visible.
	 * @return Corresponding position in the sky, or null if the object is not
	 *         visible or the position is not visible and only_if_visible is set
	 *         to true.
	 * @throws JPARSECException If an error occurs.
	 */
	public float[] getScreenCoordinatesOfPlanetographicPosition(LocationElement loc, TARGET target, int coordinate_system,
			boolean only_if_visible) throws JPARSECException
	{
		ArrayList<Object> planets = new ArrayList<Object>();
		planets.add(new float[] { xPosition, yPosition, planet_size/scaleFactor, (float) render.ephem.distance});
		planets.add(render.clone());
		for (int i=0; i<satX.length; i++) {
			planets.add(new float[] { satX[i]/scaleFactor, satY[i]/scaleFactor, -satR[i]/scaleFactor, satDist[i], satID[i] });
			planets.add(render.clone());
		}
		renderSky.planets = planets;
		renderSky.render = new SkyRenderElement();
		renderSky.render.width = render.width;
		renderSky.render.height = render.height;
		renderSky.render.telescope = render.telescope;
		renderSky.render.anaglyphMode = render.anaglyphMode;
		renderSky.firstTime = false;
		float pos[] = renderSky.getScreenCoordinatesOfPlanetographicPosition(loc, target,
				coordinate_system, only_if_visible);
		return pos;
	}


	/**
	 * Identifies a feature in the current rendered body. GRS in Jupiter is
	 * identified in this method.
	 * @param x Window x position.
	 * @param y Window y position.
	 * @param considerSatellites True to consider satellites.
	 * @param minimumSize Minimum size in pixels of the planet to consider it
	 * as identified for the input coordinates. Set to 0 to allow identifying
	 * planets event when their disks are almost invisible because of their
	 * minimum apparent sizes.
	 * @param maxDist Maximum distance to the feature in degrees. In case no
	 * feature is closer to the input position than this value, null will be
	 * returned. Set to 0 (or a very high value) to disable this limitation.
	 * @return Name of the most closer feature, or null if none can be found.
	 * The name is returned as the feature name and some data between brackets,
	 * like feature type, longitude, and latitude.
	 */
	public String identifyFeature(int x, int y, boolean considerSatellites, int minimumSize, double maxDist)
	{
		String feature = null;

		ArrayList<Object> planets = new ArrayList<Object>();
		planets.add(new float[] { xPosition, yPosition, planet_size/scaleFactor, (float) render.ephem.distance });
		planets.add(render.clone());
		if (considerSatellites) {
			for (int i=0; i<satX.length; i++) {
				if (satR[i] > 1) {
					planets.add(new float[] { satX[i]/scaleFactor, satY[i]/scaleFactor, -satR[i]/scaleFactor, satDist[i], satID[i] });
					planets.add(render.clone());
				}
			}
		}

		try {
			renderSky.planets = planets;
			renderSky.render = new SkyRenderElement();
			renderSky.render.width = render.width;
			renderSky.render.height = render.height;
			renderSky.render.telescope = render.telescope;
			TARGET target = renderSky.getPlanetInScreenCoordinates(x, y, considerSatellites, minimumSize);
			LocationElement loc = renderSky.getPlanetographicPosition(x, y, 3, considerSatellites);

			feature = RenderPlanet.identifyFeature(loc, target, maxDist);
			if (feature == null && target == TARGET.JUPITER) {
				double lon0 = (270 / 2000.0) * Constant.TWO_PI;
				double grsLon = lon0 - this.offsetInLongitudeOfJupiterGRS;
				loc = renderSky.getPlanetographicPosition(x, y, 2, considerSatellites);
				LocationElement locGRS = new LocationElement(grsLon, -22.2 * Constant.DEG_TO_RAD, 1);
				double d = LocationElement.getApproximateAngularDistance(loc, locGRS);
				if (d < 3 * Constant.DEG_TO_RAD) feature = Translate.translate(1070).trim();
			}

		} catch (JPARSECException e) {
//			e.printStackTrace();
		}
		return feature;
	}

	/**
	 * Identifies a feature in the current rendered body. GRS in Jupiter is not
	 * identified in this method.
	 * @param loc Planetographic/selenographic/satellitographic position.
	 * @param target Target body.
	 * @param maxDist Maximum distance to the feature in degrees. In case no
	 * feature is closer to the input position than this value, null will be
	 * returned. Set to 0 or lower (or a very high value) to disable this limitation.
	 * @return Name of the most closer feature, or null if none can be found.
	 * The name is returned as the feature name and some data between brackets,
	 * like feature type, longitude, and latitude (deg), size (km), and details.
	 */
	public static String identifyFeature(LocationElement loc, TARGET target, double maxDist)
	{
		return identifyFeature(loc, target, maxDist, 0);
	}
	
	/**
	 * Identifies a feature in the current rendered body. GRS in Jupiter is not
	 * identified in this method.
	 * @param loc Planetographic/selenographic/satellitographic position.
	 * @param target Target body.
	 * @param maxDist Maximum distance to the feature in degrees. In case no
	 * feature is closer to the input position than this value, null will be
	 * returned. Set to 0 or lower (or a very high value) to disable this limitation.
	 * @param minDiameter Minimum diameter in km of the feature identified. Set
	 * to 0 or lower to disable this condition.
	 * @return Name of the most closer feature, or null if none can be found.
	 * The name is returned as the feature name and some data between brackets,
	 * like feature type, longitude, and latitude (deg), size (km), and details.
	 */
	public static String identifyFeature(LocationElement loc, TARGET target, double maxDist,
			double minDiameter)
	{
		if (target == TARGET.EARTH) {
			try {
				CityElement c = City.findNearestCity(loc, null, maxDist * Constant.DEG_TO_RAD);
				String feature = c.name + " ("+Translate.translate(1082).toLowerCase()+
						") ("+Functions.formatValue(c.longitude, 2)+"\u00b0) ("+Functions.formatValue(c.latitude, 2)+"\u00b0) (0 km) ("+c.country+", "+c.height+", "+Functions.formatValue(c.timeZone, 1)+")";
				return feature;
			} catch (Exception e) {
				return null;
			}
		}

		String feature = null;
		double minDist = -1;

		String n = target.getEnglishName();
		try {
			ArrayList<String> v = null;
			if (v == null) { 
				if (!ReadFile.resourceAvailable(FileIO.DATA_SKY_LOCATIONS_DIRECTORY +n+".txt"))
					return null;
				v = ReadFile.readResource(FileIO.DATA_SKY_LOCATIONS_DIRECTORY +n+".txt",
					ReadFile.ENCODING_UTF_8);
			}
			double lon0 = loc.getLongitude() * Constant.RAD_TO_DEG;
			double lat0 = loc.getLatitude() * Constant.RAD_TO_DEG;
			String sep = "\t";
			double cosLat0 = FastMath.cos(lat0);
			double maxDist2 = maxDist / cosLat0, minDist2 = -1;
			for (int i=0; i<v.size(); i++)
			{
				String line = v.get(i);

				String lat = FileIO.getField(3, line, sep, true);
				double latp = DataSet.parseDouble(lat);
				if (minDist > 0 && Math.abs(latp-lat0) > minDist) continue;
				if (maxDist > 0 && Math.abs(latp-lat0) > maxDist) continue;

				String lon = FileIO.getField(4, line, sep, true);
				double lonp = DataSet.parseDouble(lon); // Different criteria, East positive ! ?
				if (target != TARGET.Moon && target != TARGET.MERCURY && target != TARGET.VENUS && target != TARGET.EARTH) lonp = -lonp;
				
				double dlon = Functions.normalizeDegrees(lonp - lon0);
				if (minDist2 > 0 && dlon > minDist2 && dlon < 360.0 - minDist2) continue;
				if (maxDist2 > 0 && dlon > maxDist2 && dlon < 360.0 - maxDist2) continue;

				LocationElement loc0 = new LocationElement(lonp*Constant.DEG_TO_RAD,latp*Constant.DEG_TO_RAD,1.0);
				double dist = LocationElement.getApproximateAngularDistance(loc, loc0);
				if (dist < Constant.DEG_TO_RAD) dist = LocationElement.getAngularDistance(loc, loc0);
				if (dist < minDist || minDist == -1) {
					String size = FileIO.getField(5, line, sep, true);
					if (minDiameter > 0 && DataSet.isDoubleStrictCheck(size)) {
						double s = Double.parseDouble(size);
						if (s < minDiameter) continue;
					}
					minDist = dist;
					minDist2 = dist / cosLat0;
					String name = FileIO.getField(1, line, sep, true);
					String type = FileIO.getField(2, line, sep, true);
					String detail = FileIO.getField(6, line, sep, true);
					detail += ", "+FileIO.getField(7, line, sep, true);
					feature = name + " ("+type+") ("+Functions.formatValue(lonp, 2)+"\u00b0) ("+Functions.formatValue(latp, 2)+"\u00b0) ("+size+" km) ("+detail+")";
				}
			}
		} catch (Exception exc) { 
		}
		if (minDist != -1 && maxDist > 0 && minDist > maxDist * Constant.DEG_TO_RAD) return null;

		return feature;
	}

	/**
	 * Returns the position of a feature in the current rendered body.
	 * @param name Name of the feature.
	 * @param target Target body.
	 * @return Location of the feature, or null if it is not found.
	 */
	public static LocationElement identifyFeature(String name, TARGET target)
	{
		if (name == null || name.trim().equals("")) return null;
		name = name.toLowerCase();
		String feature = null;
		LocationElement out = null;
		try {
			String n = target.getEnglishName();
			if (!ReadFile.resourceAvailable(FileIO.DATA_SKY_LOCATIONS_DIRECTORY +n+".txt"))
				return null;
			ArrayList<String> v = ReadFile.readResource(FileIO.DATA_SKY_LOCATIONS_DIRECTORY +n+".txt",
					ReadFile.ENCODING_UTF_8);
			String sep = UnixSpecialCharacter.UNIX_SPECIAL_CHARACTER.TAB.value;
			for (int i=0; i<v.size(); i++)
			{
				String line = v.get(i);

				feature = FileIO.getField(1, line, sep, true).toLowerCase();
				//String t = FileIO.getField(2, line, sep, true);
				//feature = n + " ("+t+")";
				if (feature.indexOf(name) >= 0) {
					String lat = FileIO.getField(3, line, sep, true);
					String lon = FileIO.getField(4, line, sep, true);

					double lonp = DataSet.parseDouble(lon); // Different criteria, East positive ! ?
					double latp = DataSet.parseDouble(lat);
					if (target != TARGET.Moon && target != TARGET.MERCURY && target != TARGET.VENUS && target == TARGET.EARTH) lonp = -lonp;

					out = new LocationElement(lonp*Constant.DEG_TO_RAD,latp*Constant.DEG_TO_RAD,1.0);
					if (feature.equals(name)) break;
				}
			}
		} catch (Exception exc) { }
		return out;
	}

	/**
	 * Returns a list of features for a given body.
	 * @param target Target body.
	 * @return List of features, or null in case of error.
	 */
	public static String[] getListOfFeatures(TARGET target)
	{
		try {
			String n = target.getEnglishName();
			if (!ReadFile.resourceAvailable(FileIO.DATA_SKY_LOCATIONS_DIRECTORY +n+".txt"))
				return null;
			ArrayList<String> v = ReadFile.readResource(FileIO.DATA_SKY_LOCATIONS_DIRECTORY +n+".txt",
					ReadFile.ENCODING_UTF_8);
			String feature[] = new String[v.size()];
			String sep = UnixSpecialCharacter.UNIX_SPECIAL_CHARACTER.TAB.value;
			for (int i=0; i<v.size(); i++)
			{
				String line = v.get(i);

				String name = FileIO.getField(1, line, sep, true);
				String type = FileIO.getField(2, line, sep, true);
				feature[i] = name + " ("+type+")";
			}
			return feature;
		} catch (Exception exc) {
			return null;
		}
	}
}
