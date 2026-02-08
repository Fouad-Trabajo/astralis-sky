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

import java.awt.Color;
import java.util.ArrayList;

import jparsec.astronomy.TelescopeElement;
import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.EphemerisElement.ALGORITHM;
import jparsec.ephem.Functions;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.moons.MoonEphem;
import jparsec.ephem.moons.MoonEphemElement;
import jparsec.ephem.planets.EphemElement;
import jparsec.ephem.planets.PlanetEphem;
import jparsec.ephem.probes.SDP4_SGP4;
import jparsec.ephem.probes.SatelliteEphem;
import jparsec.ephem.probes.SatelliteEphemElement;
import jparsec.ephem.probes.SatelliteOrbitalElement;
import jparsec.graph.chartRendering.SatelliteRenderElement.PLANET_MAP;
import jparsec.io.FileIO;
import jparsec.math.Constant;
import jparsec.math.FastMath;
import jparsec.observer.ExtraterrestrialObserverElement;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.observer.ReferenceEllipsoid;
import jparsec.time.AstroDate;
import jparsec.time.SiderealTime;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.time.TimeScale;
import jparsec.util.Configuration;
import jparsec.util.JPARSECException;

/**
 * A class for satellite rendering.
 * <P>
 * The satellite are rendered with their subEarth positions projected in a
 * cylindrical view of the Earth. Sun and Moon are also shown using image icons.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class RenderSatellite
{
	/**
	 * Render object that holds render parameters.
	 */
	public SatelliteRenderElement render;

	private TimeElement time;
	private ObserverElement obs;
	private EphemerisElement eph;

	private String sat_name = "";
	private boolean showSatellite;

	private EphemElement ephem[];
	private EphemElement ephemSun;
	private EphemElement ephemEarth;
	private SatelliteEphemElement ephemSat[];
	private ArrayList<LocationElement[]> locSat = null;
	private double siderealTime;
	private int[] obsPos;
	private LocationElement obsLoc;
	private RenderPlanet rp = null;

	/** Hold the geographic position of rendered bodies. */
	public LocationElement locSun, locEarth, locSatel[], locMoon[];

	/**
	 * True to allow using an spline technique (in case the instance of
	 * {@linkplain Graphics} supports it) to resize the image in high
	 * quality mode. Default value is true, set to false for better
	 * performance.
	 */
	public static boolean ALLOW_SPLINE_RESIZING = true;

	/**
	 * Renderize a satellite.
	 *
	 * @param g Graphics object.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public synchronized void renderize(Graphics g) throws JPARSECException
	{
		// Define images
		Object sun, moon, earth, sat[] = null;

		// Initial top left corner position for the Earth texture. Will be
		// recentered later.
		int saty = 0;
		int satx = 0;

		// Obtain sub-Earth positions of Sun, Moon, Earth, and satellite
		locSun = new LocationElement(ephemSun.rightAscension - siderealTime,
				ephemSun.declination, 1.0);
		locEarth = null;
		if (ephemEarth != null) locEarth = new LocationElement(ephemEarth.rightAscension - siderealTime,
				ephemEarth.declination, 1.0);
		locSatel = new LocationElement[0];
		if (showSatellite) {
			locSatel = new LocationElement[ephemSat.length];
			for (int i=0; i<locSatel.length; i++) {
				locSatel[i] = new LocationElement(ephemSat[i].subEarthLongitude,
						ephemSat[i].subEarthLatitude, 1.0 + ephemSat[i].subEarthDistance / ReferenceEllipsoid.ELLIPSOID.LATEST.getEquatorialRadius());
			}
		}
		locMoon = null;
		if (ephem != null) {
			locMoon = new LocationElement[ephem.length];
			for (int i=0; i<ephem.length; i++) {
				locMoon[i] = new LocationElement(ephem[i].rightAscension - siderealTime, ephem[i].declination, 1.0);
			}
		}

		// Get the texture and images
		TARGET target = obs.getMotherBody();
		if (target == TARGET.NOT_A_PLANET) throw new JPARSECException("Observer must be on some Solar System body.");

		int targetW = (int)(render.width * render.planetMap.zoomFactor);
		int targetH = (int)(render.height * render.planetMap.zoomFactor);
		String s = target.getEnglishName();
		Object imgNight = null;
		int texturaxNight = 0, texturayNight = 0;
		if (target == TARGET.EARTH) {
			if (target == TARGET.EARTH && targetW <= 640 && targetH <= 320 && !g.renderingToAndroid()) {
				imgNight = g.getImage(FileIO.DATA_TEXTURES_DIRECTORY + s + "_night_low_res.jpg");
			} else {
				if (target == TARGET.EARTH && targetW <= 1536 && targetH <= 768 && !g.renderingToAndroid()) {
					imgNight = g.getImage(FileIO.DATA_TEXTURES_DIRECTORY + s + "_night_med_res.jpg");
				} else {
					imgNight = g.getImage(FileIO.DATA_TEXTURES_DIRECTORY + s + "_night.jpg");
				}
			}
			int size[] = g.getSize(imgNight);
			texturaxNight = size[0];
			texturayNight = size[1];
		}

		if (target == TARGET.EARTH && targetW <= 640 && targetH <= 320 && !g.renderingToAndroid()) {
			s += "_low_res";
		} else {
			if (target == TARGET.EARTH && targetW <= 1536 && targetH <= 768 && !g.renderingToAndroid())
				s += "_med_res";
		}
		Object img = null;
		if (target == TARGET.EARTH && render.planetMap.EarthMapSource != null) {
			img = g.getImage(render.planetMap.EarthMapSource);
		} else {
			img = g.getImage(FileIO.DATA_TEXTURES_DIRECTORY + s + ".jpg");
		}
		int texturax = render.width;
		int texturay = render.height;
		if (img != null) {
			int size[] = g.getSize(img);
			texturax = size[0];
			texturay = size[1];
		}

		sun = g.getImage(FileIO.DATA_ICONS_DIRECTORY + "Sun2.png");
		int we = (20*render.width)/640;
		earth = g.getScaledImage(g.getImage(FileIO.DATA_ICONS_DIRECTORY + "Earth.png"), we, we, true, ALLOW_SPLINE_RESIZING);
		if (img != null) earth = g.getColorInvertedImage(earth);

		// Obtain correct icon for the Moon depending on it's phase
		int satn = 0;
		if (target == TARGET.EARTH && render.showMoon) {
			double MoonElong = Math.PI * ephem[0].phase; //ephem[0].elongation;
			if (ephem[0].phaseAngle > 0.0) MoonElong = Constant.TWO_PI - MoonElong;
			satn = (int) (0.5 + MoonElong * 12.0 / Constant.TWO_PI);
			if (satn == 12) satn = 0;
		}

		moon = g.getImage(FileIO.DATA_ICONS_DIRECTORY + "moon" + satn + ".png");

		String sat_icon[] = new String[locSatel.length];
		sat = new Object[locSatel.length];
		for (int i=0; i<locSatel.length; i++) {
			sat_icon[i] = "sat";
			String sat_name = FileIO.getField(i+1, this.sat_name, " && ", false);
			if (sat_name.toLowerCase().indexOf("iss") >= 0)
				sat_icon[i] = "iss";
			if (sat_name.toLowerCase().indexOf("hst") >= 0)
				sat_icon[i] = "hst";
			if (sat_name.toLowerCase().indexOf("tiangong") >= 0)
				sat_icon[i] = "tiangong1";
			sat[i] = g.getImage(FileIO.DATA_ICONS_DIRECTORY + sat_icon[i] + ".png");
		}

		g.waitUntilImagesAreRead(new Object[] {sun, moon, earth});
		g.waitUntilImagesAreRead(sat);

		// Get image size and grab pixels
		if (texturax > targetW || texturay > targetH) {
			img = g.getScaledImage(img, targetW, targetH, false, ALLOW_SPLINE_RESIZING);
			texturax = targetW;
			texturay = targetH;
		};
		if (target == TARGET.EARTH && (texturaxNight != texturax || texturayNight != texturay)) {
			imgNight = g.getScaledImage(imgNight, texturax, texturay, false, ALLOW_SPLINE_RESIZING);
			texturaxNight = texturax;
			texturayNight = texturay;
		};
		int size[] = g.getSize(sun);
		int iw = (int) (size[0]*render.width/3000f);
		int ih = (int) (size[1]*render.width/3000f);
		sun = g.getScaledImage(sun, iw, ih, true, ALLOW_SPLINE_RESIZING);
		size = g.getSize(moon);
		iw = (int) (size[0]*render.width/3000f);
		ih = (int) (size[1]*render.width/3000f);
		moon = g.getScaledImage(moon, iw, ih, true, ALLOW_SPLINE_RESIZING);

		// Draw map
		obsPos = null;
		obsLoc = unfixLoc(new LocationElement(obs.getLongitudeRad(), obs.getLatitudeRad(), 1.0));
		if (render.planetMap.centralPosition != null) obsLoc = render.planetMap.centralPosition;

		Object renderImg = null;
		if (render.planetMap == PLANET_MAP.MAP_SPHERICAL) {
			if (render.planetMap == PLANET_MAP.MAP_SPHERICAL && rp.render.target != TARGET.Moon && rp.render.target != TARGET.MERCURY && rp.render.target != TARGET.VENUS && rp.render.target != TARGET.EARTH) {
				if (locEarth != null) locEarth.setLongitude(-locEarth.getLongitude());
				if (locSun != null) locSun.setLongitude(-locSun.getLongitude());
				if (obsLoc != null) obsLoc.setLongitude(-obsLoc.getLongitude());
				if (locSatel != null) {
					for (int i=0; i<locSatel.length; i++) {
						locSatel[i].setLongitude(-locSatel[i].getLongitude());
					}
				}
				if (locMoon != null) {
					for (int i=0; i<locMoon.length; i++) {
						locMoon[i].setLongitude(-locMoon[i].getLongitude());
					}
				}
			}

			rp.renderingSky = true;
			rp.renderingSkyMagLim = 6;
			rp.xPosition = render.width * 0.5f;
			rp.yPosition = render.height * 0.5f;
			if (rp.yPosition > rp.xPosition) rp.yPosition = rp.xPosition;
			rp.useSkySatPos = true;
			rp.skySatPos = null;
			rp.render.satellitesMain = false;
			rp.render.satellitesAll = false;
			rp.render.axes = rp.render.axesNOSE = rp.render.difraction = rp.render.showLabels = false;
			rp.render.northUp = true;
			// rp.northAngle = ephemEarth.positionAngleOfAxis;
			//rp.render.target = TARGET.EARTH;
			rp.render.width = render.width;
			rp.render.height = render.height;
			rp.render.anaglyphMode = render.anaglyphMode;
			rp.render.telescope = TelescopeElement.BINOCULARS_11x80;
			rp.render.ephem = ephemEarth;
			rp.render.ephem.angularRadius = (float) (rp.render.telescope.getField() * 0.5 * render.planetMap.zoomFactor);
			rp.render.ephemSun = ephemSun;
			//if (g.renderingToAndroid())
				rp.render.highQuality = false;
			rp.showDayAndNight = render.showDayAndNight;
			rp.earthMap = render.planetMap;
			// XXX Force white background here ?
			//if (render.planetMap.EarthMapSource != null && render.planetMap.EarthMapSource.equals(PLANET_MAP.EARTH_MAP_POLITICAL)) RenderPlanet.FORCE_WHITE_BACKGROUND = true;
			try {
				rp.renderize(g);
				if (render.planetMap.showGrid) {
					rp.render.foreground = render.planetMap.showGridColor;
					rp.render.textures = false;
					rp.render.showLabels = true;
					rp.renderize(g);
				}
			} catch (Exception exc) {
				exc.printStackTrace();
			}
			//RenderPlanet.FORCE_WHITE_BACKGROUND = false;
			renderImg = g.cloneImage(g.getImage(0, 0, g.getWidth(), g.getHeight()));
		}

		obsPos = getPosition(obsLoc);
		int factor = 40;
		double initLightAtElevation = -0.5 * Constant.DEG_TO_RAD, endLightAtElevation = -6.0 * Constant.DEG_TO_RAD;
		double altMoonLimit = 5.0 * Constant.DEG_TO_RAD;
		float ps = (int)(render.planetMap.zoomFactor-1+0.5);
		if (g.renderingToAndroid()) {
			ps = Math.max(ps, (float) targetW / texturax - 1.25f);
			//ps += 1.5f;
			//if (RenderPlanet.MAXIMUM_TEXTURE_QUALITY_FACTOR < 1) ps += 1f;
		}
//		if (render.planetMap == PLANET_MAP.MAP_SPHERICAL) ps += 1.5f;
		float ps2 = 2*ps+1;
		//double t[] = g.getTranslation();
		int tx = 0, ty = 0; //(int) t[0], ty = (int) t[1];
		boolean solarEclipse = false;
		EphemerisElement ephEcl = null;
		EphemElement ephemMoon = null, ephemSun = null;
		ObserverElement obsEcl = null;
		if (obs.getMotherBody() == TARGET.EARTH) {
			try {
				double jdUTC = TimeScale.getJD(time, obs, eph, SCALE.UNIVERSAL_TIME_UTC);
				AstroDate astroUTC = new AstroDate(jdUTC);
				RenderEclipse re = new RenderEclipse(astroUTC);
				solarEclipse = true;
				ephEcl = eph.clone();
				ephEcl.isTopocentric = false;
				ephEcl.targetBody = TARGET.Moon;
				ephemMoon = Ephem.getEphemeris(time, obs, ephEcl, false);
				ephEcl.targetBody = TARGET.SUN;
				ephemSun = Ephem.getEphemeris(time, obs, ephEcl, false);
				obsEcl = obs.clone();
			} catch (Exception exc) {}
		}

		if (render.planetMap == PLANET_MAP.MAP_FLAT) {
			if (texturax != targetW) {
				g.drawImage(img, satx, saty, (float) (targetW / texturax), (float) (targetH / texturay));				
			} else {
				g.drawImage(img, satx, saty);
			}
		}
		int deltaSat = (factor*2)/3;
		double alt_sat[] = new double[locSatel.length];
		double endInit = endLightAtElevation - initLightAtElevation;
		int pos[];
		for (int i = 0; i < (texturax * texturay); i++)
		{
			int posj = i / texturax;
			int posi = i - posj * texturax;

			LocationElement loc = this.getGeographicalPosition(i, texturax, texturay);
			pos = null;
			if (img == null || render.planetMap == PLANET_MAP.MAP_SPHERICAL) {
				pos = this.getPosition(loc);
				if (pos == null) continue;
				if (pos[0] < tx || pos[0] >= g.getWidth()+tx || pos[1] < ty || pos[1] >= g.getHeight()+ty) continue;
			}

			// Obtain RGB compounds
			int red = 255, green = 255, blue = 0;
			if (img != null) {
				if (render.planetMap == PLANET_MAP.MAP_SPHERICAL) {
					int jj = g.getRGB(renderImg, pos[0], pos[1]);
//					if (jj != g.getRGB(pos[0], pos[1])) continue;
					red = 0xff & (jj >> 16);
					green = 0xff & (jj >> 8);
					blue = 0xff & jj;
				} else {
					int jj = g.getRGB(img, posi, posj);

					red = 0xff & (jj >> 16);
					green = 0xff & (jj >> 8);
					blue = 0xff & jj;
//					if (target == TARGET.EARTH && render.planetMap.EarthMapSource == null) {
//						red *= 2;
//						green *= 2;
//						blue *= 2;
//					}
				}
			} else {
				g.setColor(red, green, blue, 255);
				if (pos != null) {
					if (ps2 <= 1) {
						g.fillOval(pos[0] + satx-ps, pos[1] + saty-ps, ps2, ps2, false);
					} else {
						g.fillRect(pos[0] + satx-ps, pos[1] + saty-ps, ps2, ps2);
					}
				}
				continue;
			}

			// Obtain elevation of the satellite and the Sun from the current
			// geographical position loc
			double alt_sun = Constant.PI_OVER_TWO - LocationElement.getApproximateAngularDistance(loc, locSun);

			if (showSatellite) {
				double new_pos[] = LocationElement.parseLocationElementFast(loc);
				for (int j=0; j<locSatel.length; j++) {
					double final_pos[] = Functions.substract(LocationElement.parseLocationElementFast(locSatel[j]), new_pos);
					LocationElement new_loc_sat = LocationElement.parseRectangularCoordinatesFast(final_pos);
					alt_sat[j] = Constant.PI_OVER_TWO - LocationElement.getApproximateAngularDistance(loc, new_loc_sat);
				}
			}

			// Attenuate or intensify brightness whether the sun/satellite is
			// above the horizon or not
			int brightness = 0;
			if (render.highlightMoon) {
				double alt_moon = Constant.PI_OVER_TWO - LocationElement.getApproximateAngularDistance(loc, locMoon[0]);
				if (alt_moon > 0) brightness = 30;
			}
			if (render.showDayAndNight && render.planetMap == PLANET_MAP.MAP_FLAT) {
				if (alt_sun > endLightAtElevation && alt_sun < initLightAtElevation && target == TARGET.EARTH) {
					double delta = 5;
					double alt_moon = Constant.PI_OVER_TWO - LocationElement.getApproximateAngularDistance(loc, locMoon[0]);
					if (satn == 4) {
						if (alt_moon > altMoonLimit) {
							delta = 0;
						} else {
							if (alt_moon > 0.0) {
								delta = delta * (1.0 - alt_moon / altMoonLimit);
							}
						}
					}
					if (satn == 3 || satn == 5) {
						if (alt_moon > altMoonLimit) {
							delta = 2;
						} else {
							if (alt_moon > 0.0) {
								delta = delta - 3.0 * alt_moon / altMoonLimit;
							}
						}
					}
					brightness += (int) (-delta*alt_sun/endLightAtElevation);
				}
				if (alt_sun < initLightAtElevation) {
					int min = Math.min(blue, Math.min(red, green))-5;
					if (min < 100) min = 100;
					int red2 = 5, green2 = 5, blue2 = 5;
					if (target == TARGET.EARTH) {
						int jjNight = g.getRGB(imgNight, posi, posj);

						red2 = 0xff & (jjNight >> 16);
						green2 = 0xff & (jjNight >> 8);
						blue2 = 0xff & jjNight;
					} else {
						if (min > 0) {
							red2 = red-min;
							green2 = green-min;
							blue2 = blue-min;

							if (red2 < 0) red2 = 0;
							if (green2 < 0) green2 = 0;
							if (blue2 < 0) blue2 = 0;
						}
					}
					if (alt_sun < endLightAtElevation) {
						red = red2;
						green = green2;
						blue = blue2;
					} else {
						red = red + (int) ((red2 - red) * (alt_sun-initLightAtElevation) / endInit);
						green = green + (int) ((green2 - green) * (alt_sun-initLightAtElevation) / endInit);
						blue = blue + (int) ((blue2 - blue) * (alt_sun-initLightAtElevation) / endInit);
					}
				}
			}
			if (brightness < 0) {
				int max = -Math.max(blue, Math.max(red, green));
				if (brightness < max) brightness = max;
			}
			for (int j=0; j<locSatel.length; j++) {
				if (alt_sat[j] > 0.0 && showSatellite) brightness += deltaSat;
			}
			if (render.showDayAndNight && solarEclipse &&
					alt_sun > (-0.5*Constant.DEG_TO_RAD) ) {
				obsEcl.setLatitudeRad(loc.getLatitude());
				obsEcl.setLongitudeRad(loc.getLongitude());
				double lst = siderealTime + loc.getLongitude();
				LocationElement locSun = Ephem.fastTopocentricCorrection(time, obsEcl, ephEcl, ephemSun, lst);
				LocationElement locMoon = Ephem.fastTopocentricCorrection(time, obsEcl, ephEcl, ephemMoon, lst);
				double d = LocationElement.getApproximateAngularDistance(locSun, locMoon);

				if (d < (ephemSun.angularRadius + ephemMoon.angularRadius)) {
					brightness -= 10;
					double h = ephemMoon.angularRadius - ephemSun.angularRadius;
					if (h > 0.0 && LocationElement.getAngularDistance(locSun, locMoon) < h) brightness -= 100;
				}
			}
			if (brightness == 0 && render.planetMap == PLANET_MAP.MAP_SPHERICAL) continue;
			if (alt_sun > initLightAtElevation && brightness == 0 && render.planetMap == PLANET_MAP.MAP_FLAT)
				continue;
			
			if (brightness != 0) {
				red += brightness;
				green += brightness;
				blue += brightness;
				if (red < 0) red = 0;
				if (green < 0) green = 0;
				if (blue < 0) blue = 0;
				if (red > 255) red = 255;
				if (green > 255) green = 255;
				if (blue > 255) blue = 255;
			}

			g.setColor(red, green, blue, 255);
			if (render.planetMap == PLANET_MAP.MAP_SPHERICAL) {
				int over = 3;
				for (int xx = -over; xx<=over; xx++) {
					int x = (int) (pos[0]-ps+satx-xx);
					if (x < 0 || x >= g.getWidth()) continue;
					for (int yy = -over; yy<over; yy++) {
						int y = (int) (pos[1]-ps+saty-yy);
						if (y < 0 || y >= g.getHeight()) continue;
						if (FastMath.hypot(xx, yy) > over) continue;
						if (g.getRGB(x, y) == g.getRGB(renderImg, x, y)) {
							int jj = g.getRGB(renderImg, x, y);
							red = 0xff & (jj >> 16);
							green = 0xff & (jj >> 8);
							blue = 0xff & jj;
							red = red + brightness;
							green = green + brightness;
							blue = blue + brightness;
							if (red < 0)
								red = 0;
							if (green < 0)
								green = 0;
							if (blue < 0)
								blue = 0;
							if (red > 255)
								red = 255;
							if (green > 255)
								green = 255;
							if (blue > 255)
								blue = 255;

							g.setColor(red, green, blue, 255);
							g.fillOval(x, y, 1, 1, false);
						}
					}
				}
			} else {
				if (pos == null) pos = this.getPosition(loc);
				if (ps2 <= 1) {
					g.fillOval(pos[0] + satx-ps, pos[1] + saty-ps, ps2, ps2, false);
				} else {
					g.fillRect(pos[0] + satx-ps, pos[1] + saty-ps, ps2, ps2);
				}
			}
		}

		int sun_pos[] = this.getPosition(locSun);
		int earth_pos[] = null;
		if (locEarth != null) earth_pos = this.getPosition(locEarth);
		int moon_pos[] = null;
		if (target == TARGET.EARTH) moon_pos = this.getPosition(locMoon[0]);
		int sat_pos[][] = null;
		float refz = render.anaglyphMode.getReferenceZ();

		if (render.planetMap.showGrid && render.planetMap == PLANET_MAP.MAP_FLAT) {
			g.setColor(render.planetMap.showGridColor, true);

			pos = this.getPosition(new LocationElement(-Math.PI, Constant.PI_OVER_TWO, 1));
			double deg30 = (render.width-1) * render.planetMap.zoomFactor / 12.0;
			for (int i=0; i<7; i++) {
				int py = (int) (pos[1] + deg30 * i); //(i*(render.height-1.0)/6.0+0.5);
				if (py < 0 || py > render.height) continue;
				g.drawLine(0, py, render.width, py, false); // Cannot use fast lines here since there is risk of color change
				if (i==0) py += 10 + g.getFont().getSize();
				//int lat = 90 - 30 * i;
				//s = ""+lat+"\u00b0";
				//g.drawString(s, 15, py-5);
			}
			//int p0[] = this.getTexturePosition(new LocationElement(-Math.PI, 0.0, 1.0));
			//int increment = 1;
			//if (obs.getMotherBody().compareTo(TARGET.EARTH) > 0 && obs.getMotherBody() != TARGET.Moon) increment = -1;
			for (int i=0; i<13; i++) {
				int px = (int) (pos[0] + deg30 * i); //p0[0]+(int)(i*(render.width-1.0)/12.0+0.5);
				if (px > render.width) px -= render.width * render.planetMap.zoomFactor;
				if (px < 0) px += render.width * render.planetMap.zoomFactor;
				if (px < 0 || px > render.width) continue;
				g.drawLine(px, 0, px, render.height-1, false);
				//int lon = -180 + 30 * i * increment;
				//if (lon < -180) lon = lon + 360;
				//s = ""+lon+"\u00b0";
				//int dx = 5;
				//if (px > render.width/2) dx = (int) (-5-g.getStringWidth(s));
				//g.drawString(s, px+dx, render.height-5-g.getFont().getSize());
			}
		}
		if (render.showOrbits && showSatellite) {
			g.setColor(render.showOrbitsColor, true);
			if (locSat != null) {
				for (int i=0; i<locSat.size(); i++) {
					LocationElement loc[] = locSat.get(i);
					int sizeo = 3, sizeo2 = 2*sizeo+1;
					int oldpos[] = null;
					if (i > 0) // && !g.renderingToAndroid()) 
						g.setColor(new Color(g.getColor()).darker().darker().getRGB(), true);
					for (int j=0; j<loc.length; j++) {
						int alpha = (int)(255*(1.0-(1.15*j)/(loc.length-1.0)));
						if (g.renderingToAndroid()) 
							alpha = (int)(255*(1.0-(4.5*j)/(loc.length-1.0)));
						if (alpha < 0) alpha = 0;
						g.setColor(new Color(g.getColor()).getRGB(), alpha);

						if (loc[j] == null) break;
						int satpos[] = this.getPosition(loc[j]);
						if (satpos == null) continue;
						if ((j+1) % 5 == 0) g.fillOval(satpos[0] + satx - sizeo, satpos[1] + saty - sizeo, sizeo2, sizeo2, refz/1.3f);
						if (oldpos != null) {
							double dist = FastMath.hypot(oldpos[0]-satpos[0], oldpos[1]-satpos[1]);
							if (dist < render.width/2) g.drawLine(oldpos[0], oldpos[1], satpos[0], satpos[1], refz/1.3f, refz/1.3f);
							if (j % 15 == 0) {
								double ang = FastMath.atan2_accurate(oldpos[1]-satpos[1], oldpos[0]-satpos[0]) + Constant.PI_OVER_TWO;
								double px = oldpos[0] + dist * FastMath.cos(ang);
								double py = oldpos[1] + dist * FastMath.sin(ang);
								int step = (int)(loc[j-1].getRadius()*1440.0+0.5);
								String l = "+"+step+"^{m}";
								g.drawString(l, (float)px-g.getStringBounds(l).getWidth()/2, (float)py+g.getFont().getSize()/2, refz/1.3f);
							}
						}
						oldpos = satpos;
					}
				}
			}
		}

		if (showSatellite) {
			sat_pos = new int[locSatel.length][];
			for (int j=0; j<locSatel.length; j++) {
				sat_pos[j] = this.getPosition(locSatel[j]);
			}
		}

		if (render.showObserver && !obs.getName().trim().equals("")) {
			LocationElement loc = unfixLoc(new LocationElement(obs.getLongitudeRad(), obs.getLatitudeRad(), 1.0));
			if (render.planetMap == PLANET_MAP.MAP_SPHERICAL && rp.render.target != TARGET.Moon && rp.render.target != TARGET.MERCURY && rp.render.target != TARGET.VENUS && rp.render.target != TARGET.EARTH)
				loc.setLongitude(-loc.getLongitude());
			pos = getPosition(loc);
			g.setColor(render.showObserverColor, true);
			double alt_sun = Constant.PI_OVER_TWO - LocationElement.getApproximateAngularDistance(loc, locSun);
			if (render.observerInRedAtNight && alt_sun < -0.75*Constant.DEG_TO_RAD) g.setColor(255, 0, 0, 255);
			double dr = 20, maxr = -1, mindx = 0, mindy = -20;
			if (target == TARGET.EARTH && pos != null) {
				mindy = 0;
				for (double ang = Math.PI/4.0; ang<Math.PI*3.0/4.0; ang=ang+Math.PI/20.0) {
					double px = pos[0] + dr * FastMath.cos(ang);
					double py = pos[1] + dr * FastMath.sin(ang);
					double delta = 0;
					if (sun_pos != null) delta = FastMath.hypot(px-sun_pos[0], py-sun_pos[1]);
					if (moon_pos != null)
						delta += FastMath.hypot(px-moon_pos[0], py-moon_pos[1]);
					if (showSatellite) {
						for (int j=0; j<locSatel.length; j++) {
							if (sat_pos[j] != null)
								delta += FastMath.hypot(px-sat_pos[j][0], py-sat_pos[j][1]);
						}
					}
					if (delta > maxr || maxr == -1) {
						mindx = px - pos[0];
						mindy = py - pos[1];
						maxr = delta;
					}
				}
				for (double ang = Math.PI+Math.PI/4.0; ang<Math.PI+Math.PI*3.0/4.0; ang=ang+Math.PI/20.0) {
					double px = pos[0] + dr * FastMath.cos(ang);
					double py = pos[1] + dr * FastMath.sin(ang);
					double delta = 0;
					if (sun_pos != null) delta = FastMath.hypot(px-sun_pos[0], py-sun_pos[1]);
					if (moon_pos != null)
						delta += FastMath.hypot(px-moon_pos[0], py-moon_pos[1]);
					if (showSatellite) {
						for (int j=0; j<locSatel.length; j++) {
							if (sat_pos[j] != null)
								delta += FastMath.hypot(px-sat_pos[j][0], py-sat_pos[j][1]);
						}
					}
					if (delta > maxr || maxr == -1) {
						mindx = px - pos[0];
						mindy = py - pos[1];
						maxr = delta;
					}
				}
			}

			String label = obs.getName();
			float w = g.getStringBounds(label).getWidth();
			if (pos != null && (maxr > w || maxr == -1)) {
				int delta = 5;
				if (mindy > 0) delta = -delta;
				g.drawLine(pos[0], pos[1] - delta, pos[0], pos[1]+(int)mindy+delta, true);
				if (mindy > 0) mindy += 10;
				g.drawString(label, pos[0]+(int)mindx-w/2f, pos[1]+(int)mindy);
			}

		}

		// Draw sun, moon, and satellite icons
		if (render.showSun && img != null && sun_pos != null) {
			size = g.getSize(sun);
			g.drawImage(sun, sun_pos[0] + satx - (int) (0.5 * size[0]), sun_pos[1] + saty - (int) (0.5 * size[1]), refz/1.5f);
		}

		if (render.showEarth && earth_pos != null) {
			size = g.getSize(earth);
			g.drawImage(earth, earth_pos[0] + satx - (int) (0.5 * size[0]), earth_pos[1] + saty - (int) (0.5 * size[1]), refz/1.4f);
		}
		if (render.showMoon && locMoon != null && (moon_pos != null || target != TARGET.EARTH)) {
			if (target == TARGET.EARTH) {
				size = g.getSize(moon);
				g.drawImage(moon, moon_pos[0] + satx - (int) (0.5 * size[0]), moon_pos[1] + saty - (int) (0.5 * size[1]), refz/1.3f);
			} else {
				int sizeo = 5, sizeo2 = 2*sizeo+1;
				g.setColor(render.showSatellitesColor, true);
				for (int i=0; i<locMoon.length; i++) {
					int satpos[] = this.getPosition(locMoon[i]);
					if (satpos == null) continue;

					double maxr = -1, mindx = 0, mindy = 0;
					LocationElement loc = unfixLoc(new LocationElement(obs.getLongitudeRad(), obs.getLatitudeRad(), 1.0));
					pos = getPosition(loc);
					for (double ang = Math.PI/4.0; ang<Math.PI+Math.PI*3.0/4.0; ang=ang+Math.PI/20.0) {
						if (ang < Math.PI*3.0/4.0 || ang > Math.PI+Math.PI/4.0) {
							for (double dr = 40; dr >= 20; dr = dr - 10) {
								double px = satpos[0] + dr * FastMath.cos(ang);
								double py = satpos[1] + dr * FastMath.sin(ang);

								double delta = FastMath.hypot(px-sun_pos[0], py-sun_pos[1]);
								delta += FastMath.hypot(px-pos[0], py-pos[1]);
								if (moon_pos != null) delta += FastMath.hypot(px-moon_pos[0], py-moon_pos[1]);
								if (i > 0) {
									for (int ii=0; ii<i; ii++) {
										int satposi[] = this.getPosition(locMoon[ii]);
										delta += FastMath.hypot(px-satposi[0], py-satposi[1]);
									}
								}

								if (showSatellite) {
									for (int j=0; j<locSatel.length; j++) {
										delta += FastMath.hypot(px-sat_pos[j][0], py-sat_pos[j][1]);
									}
								}
								if (delta > maxr || maxr == -1) {
									mindx = px - satpos[0];
									mindy = py - satpos[1] - 20;
									maxr = delta;
								}
							}
						}
					}

					g.fillOval(satpos[0] + satx - sizeo, satpos[1] + saty - sizeo, sizeo2, sizeo2, refz/1.3f);
					g.drawString(ephem[i].name, satpos[0] + satx + (int)mindx - g.getStringBounds(ephem[i].name).getWidth()/2, satpos[1] + saty + 20 + (int)mindy, refz/1.3f);
				}
			}
		}

		if (showSatellite) {
			for (int j=0; j<locSatel.length; j++) {
				if (sat_pos[j] != null) {
					size = g.getSize(sat[j]);
					g.drawImage(sat[j], sat_pos[j][0] + satx - (int) (0.5 * size[0]), sat_pos[j][1] + saty - (int) (0.5 * size[1]), refz/1.1f);
				}
			}
		}
		
		if (render.planetMap.showGrid && render.planetMap == PLANET_MAP.MAP_FLAT) {
			g.setFont(g.getFont().getDerivedFont(g.getFont(), g.getFont().getSize() + 5, 1));
			int over = g.getFont().getSize() * 4;
			Graphics g2 = g.getGraphics(g.getWidth() + over, g.getHeight() + over);
			g2.setFont(g.getFont());
			g2.setColor(0, 0, 0, 255);
			g2.fillRect(0, 0, g2.getWidth(), g2.getHeight());
			g2.setColor(0, 0, 0, 0);
			g2.fillRect(0, 0, g2.getWidth(), g2.getHeight());
			int px0 = over/2+over/6, py0 = over/2-over/6;
			g2.drawImage(g.getRendering(), px0, py0);
			g2.setColor(render.planetMap.showGridColor, true);
			double tr[] = g.getTranslation();
			g2.traslate(tr[0], tr[1]);

			pos = this.getPosition(new LocationElement(-Math.PI, Constant.PI_OVER_TWO, 1));
			double deg30 = (render.width-1) * render.planetMap.zoomFactor / 12.0;
			for (int i=0; i<7; i++) {
				int py = (int) (pos[1] + deg30 * i) + py0 + over/4;
				int lat = 90 - 30 * i;
				s = ""+lat+"\u00b0";
				g2.drawString(s, px0-2-g.getStringWidth(s), py-over/8);
			}
			//int p0[] = this.getTexturePosition(new LocationElement(-Math.PI, 0.0, 1.0));
			int increment = 1;
			if (obs.getMotherBody().compareTo(TARGET.EARTH) > 0 && obs.getMotherBody() != TARGET.Moon) increment = -1;
			for (int i=0; i<13; i++) {
				int px = (int) (pos[0] + deg30 * i); //p0[0]+(int)(i*(render.width-1.0)/12.0+0.5);
				if (px > render.width) px -= render.width * render.planetMap.zoomFactor;
				if (px < 0) px += render.width * render.planetMap.zoomFactor;
				int lon = -180 + 30 * i * increment;
				if (lon < -180) lon = lon + 360;
				s = ""+lon+"\u00b0";
				g2.drawString(s, px-g2.getStringWidth(s)/2+px0, g2.getHeight()-over/4);
			}
			g.drawImage(g.getScaledImage(g2.getRendering(), g.getWidth(), g.getHeight(), false, false), 0, 0);
		}
	}

	private LocationElement fixLoc(LocationElement loc) {
		// double a0 = incl_rotation - Constant.PI_OVER_TWO;
		// Correct for the longitude criteria in Moon: the texture is flipped
		// if (render.target == TARGET.Moon || render.target == TARGET.MERCURY) a0 = -incl_rotation - Constant.PI_OVER_TWO;
		// This is necessary to fix a displacement in the Venus Magellan
		// texture from 0 degrees longitude
		// if (render.target == TARGET.VENUS) a0 = -incl_rotation - Constant.PI_OVER_SIX;

		//if (obs.getMotherBody() != TARGET.SUN) loc.setLongitude(-loc.getLongitude());
		if (obs.getMotherBody().compareTo(TARGET.EARTH) > 0 && obs.getMotherBody() != TARGET.Moon) loc.setLongitude(-loc.getLongitude());
		if (obs.getMotherBody() == TARGET.VENUS) loc.setLongitude(2.0 * Constant.PI_OVER_SIX + loc.getLongitude());
		return loc;
	}

	private LocationElement unfixLoc(LocationElement loc) {
		//if (obs.getMotherBody() != TARGET.SUN) loc.setLongitude(-loc.getLongitude());
		if (obs.getMotherBody().compareTo(TARGET.EARTH) > 0 && obs.getMotherBody() != TARGET.Moon) loc.setLongitude(-loc.getLongitude());
		if (obs.getMotherBody() == TARGET.VENUS) loc.setLongitude(-2.0 * Constant.PI_OVER_SIX + loc.getLongitude());
		return loc;
	}

	/**
	 * Obtain rectangular position on the map corresponding to certain
	 * geographical coordinates. The position is measured from the top left
	 * corner, which is supposed to be geographical coordinates lon = -PI, lat =
	 * PI * 0.5.
	 *
	 * @param loc An object with the longitude and latitude in radians.
	 * @return An array with (x, y) coordinates.
	 */
	public int[] getTexturePosition(LocationElement loc)
	{
		if (loc == null) return null;
		LocationElement locIn = unfixLoc(loc.clone());

		if (render.planetMap == PLANET_MAP.MAP_SPHERICAL) {
			try {
				if (LocationElement.getApproximateAngularDistance(locIn, new LocationElement(rp.render.ephem.longitudeOfCentralMeridian, rp.render.ephem.positionAngleOfPole, 1.0)) > Constant.PI_OVER_TWO) return null;
				rp.renderingSky = false;
				float pos[] = rp.getScreenCoordinatesOfPlanetographicPositionForEclipse(locIn);
				if (pos == null) return null;
				return new int[] {(int)(pos[0]+0.5), (int)(pos[1]+0.5)};
			} catch (Exception exc) {
				return null;
			}
		}

		double lon = locIn.getLongitude();
		double lat = locIn.getLatitude();

		int size_x = render.width;
		int size_y = render.height;

		if (lon >= Math.PI) lon = lon - Constant.TWO_PI;
		float x = (float) (0.5 * size_x + (double) size_x * lon / Constant.TWO_PI + 0.5);
		float y = (float) (0.5 * size_y - (double) size_y * lat / Math.PI + 0.5);

		if (x < 0) x = x + size_x;
		if (x >= size_x) x = x - size_x;

		if (obsPos != null && render.planetMap.zoomFactor > 1.0f) {
			x = (int) (0.5f + (x - obsPos[0]) * render.planetMap.zoomFactor+render.width/2);
			y = (int) (0.5f + (y - obsPos[1]) * render.planetMap.zoomFactor+render.height/2);
			int fac = (int)(size_x * render.planetMap.zoomFactor);
			if (x >= fac) x -= fac;
			if (x < 0) x += fac;
		}

		return new int[] {(int)x, (int)y };
	}

	/**
	 * Obtain rectangular position on the map corresponding to certain
	 * geographical coordinates. The position is measured from the top left
	 * corner, which is supposed to be geographical coordinates lon = -PI, lat =
	 * PI * 0.5.
	 *
	 * @param loc An object with the longitude and latitude in radians.
	 * @return An array with (x, y) coordinates.
	 */
	private int[] getPosition(LocationElement loc)
	{
		if (render.planetMap == PLANET_MAP.MAP_SPHERICAL) {
			try {
				rp.renderingSky = false;
				float pos[] = rp.getScreenCoordinatesOfPlanetographicPositionForEclipse(loc);
				if (pos == null) return null;
				return new int[] {(int)(pos[0]+0.5), (int)(pos[1]+0.5)};
			} catch (Exception exc) {
				return null;
			}
		}

		double lon = loc.getLongitude();
		double lat = loc.getLatitude();

		int size_x = render.width;
		int size_y = render.height;

		if (lon >= Math.PI) lon = lon - Constant.TWO_PI;
		float x = (float) (0.5 * size_x + (double) size_x * lon / Constant.TWO_PI + 0.5);
		float y = (float) (0.5 * size_y - (double) size_y * lat / Math.PI + 0.5);

		if (x < 0) x = x + size_x;
		if (x >= size_x) x = x - size_x;

		if (obsPos != null && render.planetMap.zoomFactor > 1.0f) {
			x = (int) (0.5f + (x - obsPos[0]) * render.planetMap.zoomFactor+render.width/2);
			y = (int) (0.5f + (y - obsPos[1]) * render.planetMap.zoomFactor+render.height/2);
			int fac = (int)(size_x * render.planetMap.zoomFactor);
			if (x >= fac) x -= fac;
			if (x < 0) x += fac;
		}

		return new int[] {(int)x, (int)y };
	}

	/**
	 * Obtains the geographical position corresponding to certain texture position.
	 *
	 * @param i Index of the texture, from 0 to size_x*size_y.
	 * @param size_x Width of the texture in pixels.
	 * @param size_y Height of the texture in pixels.
	 * @return An object with the geographical coordinates. Distance is set to
	 *         unity.
	 */
	private LocationElement getGeographicalPosition(int i, int size_x, int size_y)
	{
		int jj = i / size_x;
		int ii = i - jj * size_x;

		double longe = (double) (ii + 1 - size_x / 2.0) * Constant.TWO_PI / (double) (size_x);
		double latge = (double) (size_y / 2.0 - jj - 1) * Math.PI / (double) (size_y);

		LocationElement loc = new LocationElement(longe, latge, 1.0);

		return loc;
	}

	/**
	 * Obtains the geographical position corresponding to certain map
	 * coordinates.
	 *
	 * @param x X position.
	 * @param y Y position.
	 * @return An object with the geographical coordinates. Distance is set to
	 *         unity.
	 */
	public LocationElement getGeographicalPosition(float x, float y)
	{
 		int size_x = render.width;
		int size_y = render.height;

		if (render.planetMap == PLANET_MAP.MAP_SPHERICAL) {
			try {
				return rp.getPlanetographicPosition((int)(x+0.5), (int)(y+0.5), 3, false);
			} catch (Exception exc) {
				return null;
			}
		}

		if (obsPos != null && render.planetMap.zoomFactor > 1.0f) {
			x = (float) ((x -render.width/2) / render.planetMap.zoomFactor) + obsPos[0] - 0.5f;
			y = (float) ((y -render.height/2) / render.planetMap.zoomFactor) + obsPos[1] - 0.5f;
			if (x >= size_x) x -= size_x;
			if (x < 0) x += size_x;
		}

		double longe = (double) (x - 0.5f - size_x * 0.5) * Constant.TWO_PI / (double) size_x;
		double latge = (double) (size_y * 0.5 - y + 0.5) * Math.PI / (double) size_y;
		if (latge > Constant.PI_OVER_TWO) latge = Math.PI - latge;
		if (latge < -Constant.PI_OVER_TWO) latge = -Math.PI - latge;

		LocationElement loc = fixLoc(new LocationElement(longe, latge, 1.0));

		return loc;
	}

	/**
	 * Constructor for satellite render process.
	 *
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param eph Ephemeris object.
	 * @param render Render object.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public RenderSatellite(TimeElement time, ObserverElement obs, EphemerisElement eph,
			SatelliteRenderElement render) throws JPARSECException {
		this.setSatelliteRenderElement(render, time, obs, eph);
	}
	/**
	 * Sets the satellite render objects.
	 *
	 * @param render Render object.
	 * @param time Time object.
	 * @param obs Observer object.
	 * @param ephIn Ephemeris object.
	 * @throws JPARSECException Thrown if the calculation fails.
	 */
	public void setSatelliteRenderElement(SatelliteRenderElement render, TimeElement time, ObserverElement obs, EphemerisElement ephIn) throws JPARSECException
	{
		try
		{
			if (render.planetMap == PLANET_MAP.MAP_FLAT) {
				render.height = render.width / 2;
				if (render.planetMap.zoomFactor < 1) render.planetMap.zoomFactor = 1;
			}
			this.render = render;
			this.time = time;
			this.obs = obs;
			this.eph = ephIn.clone();
			if (eph.algorithm.ordinal() > ALGORITHM.SERIES96_MOSHIERForMoon.ordinal()) eph.algorithm = ALGORITHM.MOSHIER;

			EphemerisElement sun_eph = new EphemerisElement(TARGET.EARTH, eph.ephemType,
					EphemerisElement.EQUINOX_OF_DATE, eph.isTopocentric, eph.ephemMethod, eph.frame);
			sun_eph.algorithm = EphemerisElement.ALGORITHM.MOSHIER;
			sun_eph.correctForEOP = false;
			sun_eph.correctForPolarMotion = false;
			sun_eph.preferPrecisionInEphemerides = false;
			siderealTime = SiderealTime.greenwichApparentSiderealTime(time, obs, sun_eph);
			ephemEarth = null;
			if (obs.getMotherBody() != TARGET.EARTH) ephemEarth = PlanetEphem.MoshierEphemeris(time, obs, sun_eph);
			if (render.planetMap == PLANET_MAP.MAP_SPHERICAL) { // && obs.getMotherBody() == TARGET.EARTH) {
				EphemerisElement eph1 = ephIn.clone();
				eph1.isTopocentric = true;
				eph1.equinox = EphemerisElement.EQUINOX_OF_DATE;
				eph1.targetBody = obs.getMotherBody();
				ObserverElement obs1 = obs.clone();
				if (render.planetMap.centralPosition != null) {
					obs1.setLatitudeRad(render.planetMap.centralPosition.getLatitude());
					obs1.setLongitudeRad(render.planetMap.centralPosition.getLongitude());
				}
				obs1.setHeight(0, true);
				double clon = obs1.getLongitudeRad();
				double jd_TDB = TimeScale.getJD(time, obs, eph1, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
				// Note it is 'critical' to pass to a non-local time scale when obtaining ephemerides
				// of Earth or any other body for an observer out from the Earth.
				time = new TimeElement(jd_TDB, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
				double delta[] = obs1.topocentricObserverICRF(time, eph1);
				double p[] = Ephem.eclipticToEquatorial(PlanetEphem.getHeliocentricEclipticPositionJ2000(jd_TDB, eph1.targetBody), Constant.J2000, eph1);
				delta = Functions.scalarProduct(delta, 1000);
				p = Functions.sumVectors(p, delta);
				p = new double[] {p[0], p[1], p[2], 0.0, 0.0, 0.0};
				obs1 = ObserverElement.parseExtraterrestrialObserver(new ExtraterrestrialObserverElement("", p));
				if (obs.getMotherBody() == TARGET.EARTH) ephemEarth = PlanetEphem.MoshierEphemeris(time, obs1, sun_eph);
				sun_eph.targetBody = TARGET.SUN;
				ephemSun = PlanetEphem.MoshierEphemeris(time, obs, sun_eph);

				// Little correction to avoid light-time effects and a little
				// lack of accuracy in the calculation of Earth's meridian from
				// an observer out from Earth
				double dlon = (clon - ephemEarth.longitudeOfCentralMeridian);
				ephemEarth.longitudeOfCentralMeridian += dlon;
				ephemEarth.subsolarLongitude += dlon;
				ephemSun.longitudeOfCentralMeridian += dlon;
				ephemSun.subsolarLongitude += dlon;

				PlanetRenderElement planetRender = new PlanetRenderElement();
				planetRender.telescope = TelescopeElement.BINOCULARS_11x80;
				planetRender.highQuality = true;
				planetRender.target = eph1.targetBody;
				sun_eph.targetBody = eph1.targetBody;
				rp = new RenderPlanet(time, obs1, sun_eph, planetRender);
			} else {
				sun_eph.targetBody = TARGET.SUN;
				ephemSun = PlanetEphem.MoshierEphemeris(time, obs, sun_eph);
			}
			if (obs.getMotherBody() != TARGET.EARTH) {
				ObserverElement obs2 = obs.clone();
				obs2.setLongitudeRad(0.0);
				siderealTime = SiderealTime.apparentSiderealTime(time, obs2, sun_eph);
				if (obs.getMotherBody() == TARGET.VENUS) siderealTime = -(siderealTime + 2.0 * Constant.PI_OVER_SIX);
			}
			if (obs.getMotherBody() == TARGET.EARTH) {
				EphemerisElement moon_eph = new EphemerisElement(TARGET.Moon, eph.ephemType,
						EphemerisElement.EQUINOX_OF_DATE, eph.isTopocentric, eph.ephemMethod, eph.frame);
				moon_eph.algorithm = EphemerisElement.ALGORITHM.MOSHIER;
				moon_eph.correctForEOP = false;
				moon_eph.correctForPolarMotion = false;
				moon_eph.preferPrecisionInEphemerides = false;
				ephem = new EphemElement[] { PlanetEphem.MoshierEphemeris(time, obs, moon_eph) };
			} else {
				if (render.showMoon && obs.getMotherBody().isPlanet() && obs.getMotherBody() != TARGET.SUN) {
					MoonEphemElement mephem[] = null;
					switch (obs.getMotherBody())
					{
					case MARS:
						mephem = MoonEphem.martianSatellitesEphemerides_2007(time, obs, eph);
						break;
					case JUPITER:
						mephem = MoonEphem.galileanSatellitesEphemerides_L1(time, obs, eph);
						break;
					case SATURN:
						mephem = MoonEphem.saturnianSatellitesEphemerides_TASS17(time, obs, eph, false);
						break;
					case URANUS:
						mephem = MoonEphem.uranianSatellitesEphemerides_GUST86(time, obs, eph);
						break;
					default:
						break;
					}
					if (mephem != null) {
						ephem = new EphemElement[mephem.length];
						double jd = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
						for (int i=0; i<mephem.length;i++) {
							ephem[i] = EphemElement.parseMoonEphemElement(mephem[i], jd);
						}
					}
				}
			}

			showSatellite = eph.targetBody.getIndex() > 0;
			if (showSatellite) {
				try {
					double jd = TimeScale.getJD(time, obs, eph, SCALE.UNIVERSAL_TIME_UTC);
					AstroDate astro = new AstroDate(jd);
					if (!Configuration.isAcceptableDateForArtificialSatellites(astro) && showSatellite) {
						String sn = SatelliteEphem.getArtificialSatelliteName(eph.targetBody.getIndex());
						String p = Configuration.updateArtificialSatellitesInTempDir(astro);
						if (p == null) {
							showSatellite = false;
						} else {
							eph.targetBody.setIndex(SatelliteEphem.getArtificialSatelliteTargetIndex(sn));
						}
					}
				} catch (Exception exc) {
					showSatellite = false;
				}
			}

			ephemSat = new SatelliteEphemElement[1];
			if (showSatellite) {
				sat_name = SatelliteEphem.getArtificialSatelliteName(eph.targetBody.getIndex());
				ephemSat[0] = SDP4_SGP4.satEphemeris(time, obs, eph, false); //SatelliteEphem.satEphemeris(time, obs, eph, false);
			}
			if (showSatellite && render.showOrbits && eph.targetBody.getIndex() >= 0 && obs.getMotherBody() == TARGET.EARTH) {
				locSat = new ArrayList<LocationElement[]>();
				SatelliteOrbitalElement sat = SatelliteEphem.getArtificialSatelliteOrbitalElement(eph.targetBody.getIndex());
				int np = render.width / 8;
				double period = Constant.TWO_PI / sat.meanMotion;
				double step = (int) (period * 1440.0 / np + 1) / 1440.0;
				LocationElement loc[] = new LocationElement[np];
				TimeElement timeF = time.clone();
				for (int i=0; i<np; i++) {
					timeF.add(step);
					SatelliteEphemElement se = SDP4_SGP4.satEphemeris(timeF, obs, eph, false); //SatelliteEphem.satEphemeris(timeF, obs, eph, false);
					loc[i] = new LocationElement(se.subEarthLongitude, se.subEarthLatitude, step*(1.0+i));
					if (step*(2.0+i) > period) break;
				}
				locSat.add(loc);
			}
			if (showSatellite && !render.showSatellite) showSatellite = render.showSatellite;

		} catch (JPARSECException ve)
		{
			throw ve;
		}
	}

	/**
	 * Adds an artificial Earth satellite to render another one.
	 * @param name The name of the satellite.
	 * @return True if it was added correctly, false otherwise (for example
	 * if the rendering date is more than 30 days far from the date of the
	 * orbital elements).
	 * @throws JPARSECException If the mother body is not the Earth, or if
	 * another error occurs.
	 */
	public boolean addSatellite(String name) throws JPARSECException {
		if (obs.getMotherBody() != TARGET.EARTH)
			throw new JPARSECException("Unsupported for mother body "+obs.getMotherBody().getEnglishName()+".");

		int index = SatelliteEphem.getArtificialSatelliteTargetIndex(name);
		if (showSatellite && index >= 0) {
			sat_name += " && " + SatelliteEphem.getArtificialSatelliteName(index);
			SatelliteEphemElement newSat[] = new SatelliteEphemElement[ephemSat.length+1];
			for (int i=0; i<newSat.length; i++) {
				if (i < ephemSat.length) {
					newSat[i] = ephemSat[i];
				} else {
					eph.targetBody = TARGET.NOT_A_PLANET;
					eph.targetBody.setIndex(index);
					newSat[i] = SDP4_SGP4.satEphemeris(time, obs, eph, false); //SatelliteEphem.satEphemeris(time, obs, eph, false);
				}
			}
			ephemSat = newSat;
		}
		if (index >= 0 && render.showOrbits) {
			if (locSat == null) locSat = new ArrayList<LocationElement[]>();
			SatelliteOrbitalElement sat = SatelliteEphem.getArtificialSatelliteOrbitalElement(index);
			int np = render.width / 8;
			double period = Constant.TWO_PI / sat.meanMotion;
			double step = (int) (period * 1440.0 / np + 1) / 1440.0;
			LocationElement loc[] = new LocationElement[np];
			TimeElement timeF = time.clone();
			eph.targetBody = TARGET.NOT_A_PLANET;
			eph.targetBody.setIndex(index);
			for (int i=0; i<np; i++) {
				timeF.add(step);
				SatelliteEphemElement se = SDP4_SGP4.satEphemeris(timeF, obs, eph, false); //SatelliteEphem.satEphemeris(timeF, obs, eph, false);
				loc[i] = new LocationElement(se.subEarthLongitude, se.subEarthLatitude, step*(1.0+i));
				if (step*(2.0+i) > period) break;
			}
			locSat.add(loc);
		}
		if (showSatellite && index >= 0) return true;

		return false;
	}

	/**
	 * Returns the ephemerides for the artificial satellites.
	 * @return Ephemerides.
	 */
	public SatelliteEphemElement[] getSatelliteEphem() {
		return ephemSat;
	}
	/**
	 * Returns the ephemerides for the Moon or the natural satellites.
	 * @return The ephemerides.
	 */
	public EphemElement[] getMoonEphem() {
		return ephem;
	}
	/**
	 * Returns the ephemerides for the Sun.
	 * @return Ephemerides for Sun.
	 */
	public EphemElement getSunEphem() {
		return ephemSun;
	}
}
