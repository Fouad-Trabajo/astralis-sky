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
package jparsec.io.device;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import jparsec.astronomy.Constellation;
import jparsec.astronomy.CoordinateSystem;
import jparsec.astronomy.Constellation.CONSTELLATION_NAME;
import jparsec.astronomy.Difraction;
import jparsec.astronomy.Star;
import jparsec.astronomy.TelescopeElement;
import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.Functions;
import jparsec.ephem.IAU2006;
import jparsec.ephem.EphemerisElement.COORDINATES_TYPE;
import jparsec.ephem.Nutation;
import jparsec.ephem.Precession;
import jparsec.ephem.Target.TARGET;
import jparsec.ephem.moons.MoonEphem;
import jparsec.ephem.moons.MoonEphemElement;
import jparsec.ephem.planets.EphemElement;
import jparsec.ephem.planets.PlanetEphem;
import jparsec.ephem.stars.StarElement;
import jparsec.graph.DataSet;
import jparsec.graph.chartRendering.AWTGraphics;
import jparsec.graph.chartRendering.PlanetRenderElement;
import jparsec.graph.chartRendering.RenderSky;
import jparsec.graph.chartRendering.frame.PlanetaryRendering;
import jparsec.io.FileFormatElement;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.io.ReadFormat;
import jparsec.io.UnixSpecialCharacter;
import jparsec.io.device.GenericTelescope.MOUNT;
import jparsec.io.image.HeaderElement;
import jparsec.io.image.ImageSplineTransform;
import jparsec.io.image.Picture;
import jparsec.math.Constant;
import jparsec.math.FastMath;
import jparsec.math.matrix.Matrix;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.time.AstroDate;
import jparsec.time.SiderealTime;
import jparsec.time.TimeElement;
import jparsec.time.TimeScale;
import jparsec.time.TimeElement.SCALE;
import jparsec.util.DataBase;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;

/**
 * An implementation of the camera interface for virtual cameras.
 * @author T. Alonso Albi - OAN (Spain)
 */
public class VirtualCamera implements GenericCamera {

	/**
	 * Set to true (default value) to allow the rendering of deep sky objects as
	 * textures, or to false to show only stars.
	 */
	public static boolean DRAW_DSO_TEXTURES = true;
	/**
	 * Set to true (default value) to allow the rendering of planetary textures.
	 */
	public static boolean DRAW_PLANETARY_TEXTURES = true;

	@Override
	public boolean setISO(String iso) {
		this.iso = iso;
		return true;
	}

	@Override
	public String getISO() {
		return iso;
	}

	@Override
	public boolean setExpositionTime(String time) {
		texp = time;
		return true;
	}

	@Override
	public String getExpositionTime() {
		return texp;
	}

	@Override
	public boolean setResolutionMode(String mode) {
		this.res = mode;
		raw = false;
		if (res.startsWith("RAW")) raw = true;
		return true;
	}

	@Override
	public String getResolutionMode() {
		return res;
	}

	@Override
	public boolean setAperture(String aperture) {
		this.aperture = aperture;
		return true;
	}

	@Override
	public String getAperture() {
		return aperture;
	}

	@Override
	public boolean shotAndDownload(boolean keepInCamera) {
		if (shooting) return false;

		shooting = true;
		lastImage = path + "capt0000.jpg";
		if (raw) lastImage = path + "capt0000.pgm";

		ShotThread st = new ShotThread();
		st.start();

		return true;
	}

	@Override
	public boolean isShooting() {
		return shooting;
	}

	@Override
	public boolean setCameraOrientation(double ang) {
		orientation = ang;
		return true;
	}

	@Override
	public double getCameraOrientation() {
		return orientation;
	}

	@Override
	public boolean setImageID(IMAGE_ID img) {
		this.id = img;
		return true;
	}

	@Override
	public IMAGE_ID getImageID() {
		return id;
	}

	@Override
	public String getPathOfLastDownloadedImage() {
		return lastImage;
	}

	@Override
	public boolean setDownloadDirectory(String path) {
		this.path = path;
		return true;
	}

	@Override
	public boolean setFilter(FILTER filter) {
		this.filter = filter;
		return true;
	}

	@Override
	public FILTER getFilter() {
		return filter;
	}

	@Override
	public String[] getPossibleISOs() {
		return new String[] {"100", "800", "1600"};
	}

	@Override
	public String[] getPossibleResolutionModes() {
		return new String[] {"RAW "+w+"x"+h, "JPG "+w+"x"+h};
	}

	@Override
	public String[] getPossibleExpositionTimes() {
		return new String[] {"1", "30", "60", Translate.translate(1180)};
	}

	@Override
	public boolean isBulb() {
		return texp.equals(Translate.translate(1180));
	}

	@Override
	public String[] getPossibleApertures() {
		return new String[] {};
	}

	@Override
	public CAMERA_MODEL getCameraModel() {
		return model;
	}

	@Override
	public void setCCDorBulbModeTime(int seconds) {
		bulbTime = seconds;
	}

	@Override
	public int getCCDorBulbModeTime() {
		return bulbTime;
	}

	@Override
	public String getCameraName() {
		return camName;
	}

	@Override
	public void setCameraName(String name) {
		camName = name;
	}

	@Override
	public double getWidthHeightRatio() {
		return 1.5;
	}

	@Override
	public double getInverseElectronicGain() {
		return 0.84 * 400.0 / Double.parseDouble(this.getISO());
	}

	@Override
	public double getSaturationLevelADUs() {
		return 12900;
	}

	@Override
	public int getDepth() {
		return 14;
	}

	@Override
	public HeaderElement[] getFitsHeaderOfLastImage() {
		int max = FastMath.multiplyBy2ToTheX(1, getDepth()) - 1;
		double jd0 = -1, jd1 = -1, jde = -1;
		try {
			AstroDate astro0 = new AstroDate(getLastShotStartTime());
			jd0 = astro0.jd();
			AstroDate astro1 = new AstroDate(getLastShotEndTime());
			jd1 = astro1.jd();
			
			double off = (jd1 - jd0) - Double.parseDouble(getExpositionTime()) / Constant.SECONDS_PER_DAY;
			if (off >= 0) jde = (jd0 + jd1 - off) * 0.5;
		} catch (Exception exc) {}
		
		HeaderElement header[] = new HeaderElement[] {
				new HeaderElement("BITPIX", "32", "Bits per data value"),
				new HeaderElement("NAXIS", "2", "Dimensionality"),
				new HeaderElement("NAXIS1", ""+w, "Width"),
				new HeaderElement("NAXIS2", ""+h, "Height"),
				new HeaderElement("EXTEND", "T", "Extension permitted"),
				new HeaderElement("MAXCOUNT", ""+max, "Max counts per pixel"),
				new HeaderElement("ISO", getISO(), "ISO"),
				new HeaderElement("TIME", getExpositionTime(), "Exposure time in s"),
				new HeaderElement("TSTART", ""+jd0, "Shot start time as JD UTC"),
				new HeaderElement("TEND", ""+jd1, "Shot end time as JD UTC"),
				new HeaderElement("TEFF", ""+jde, "Shot effective time as JD UTC"),
				new HeaderElement("MODE", getResolutionMode(), "Resolution mode"),
				new HeaderElement("RAW", ""+raw, "True for raw mode"),
				new HeaderElement("ANGLE", ""+getCameraOrientation(), "Camera orientation angle (radians)"),
				new HeaderElement("CAMERA", getCameraName(), "Camera name"),
				new HeaderElement("CAM_MODE", this.model.name(), "Camera model (driver)"),
				new HeaderElement("APERTURE", getAperture(), "Aperture f/"),
				new HeaderElement("DEPTH", ""+getDepth(), "Pixel depth in bits"),
				new HeaderElement("GAIN", ""+getInverseElectronicGain(), "Gain e-/ADU"),
				new HeaderElement("BULBTIME", ""+getCCDorBulbModeTime(), "Exposure time in bulb mode (s)"),
				new HeaderElement("MAXADU", ""+getSaturationLevelADUs(), "Saturation level in ADUs"),
				new HeaderElement("FILTER", getFilter().getFilterName(), "Filter name"),
				new HeaderElement("BAYER", "RGBG", "Bayer matrix, top-left and clockwise"),
				new HeaderElement("IMGID", GenericCamera.IMAGE_IDS[getImageID().ordinal()], "Image id: Dark, Flat, On, Test, or Reduced")
		};
		return header;
	}

	@Override
	public void setMinimumIntervalBetweenShots(int seconds) {
		minInterval = seconds;
	}

	@Override
	public int getMinimumIntervalBetweenShots() {
		return minInterval;
	}

	@Override
	public long getLastShotStartTime() {
		return lastShotStarted;
	}

	@Override
	public long getLastShotEndTime() {
		return lastShotEnded;
	}
	
	private CAMERA_MODEL model;
	private int w = 1500, h = 1000, index;
	private boolean raw = false;
	private final GenericTelescope telescope;

	private double orientation = 0;
	private IMAGE_ID id = IMAGE_ID.TEST;
	private String lastImage = null;
	private FILTER filter = FILTER.FILTER_IR_DSLR;
	private int bulbTime = 60;
	private String path = "";
	private boolean shooting = false;
	private String iso = "100", aperture = "", texp = "1", res = "JPG "+w+"x"+h, camName;
	private int[][] img = new int[w][h];
	private boolean[][] hotPixel = null;
	private int minInterval = 0;
	private long lastShotStarted = 0, lastShotEnded = 0;
	private boolean forceSeveralShots = false;
	private ArrayList<String> listUCAC4 = null;
	private LocationElement posUCAC4 = null;

	/**
	 * The constructor for a virtual camera.
	 * @param model The camera model.
	 * @param telescope The telescope instance, required to know the
	 * shooting position and the field of view.
	 * @param index The index of the camera (count number, starting with 0) in this telescope.
	 * @throws JPARSECException If the camera is not virtual.
	 */
	public VirtualCamera(CAMERA_MODEL model, GenericTelescope telescope, int index) throws JPARSECException {
		if (!model.isVirtual()) throw new JPARSECException("Camera must be a virtual one!");

		this.model = model;
		this.telescope = telescope;
		this.index = index;
		
		camName = Translate.translate(1187)+" #"+(index+1);
	}

	/**
	 * Select if the shots taken by the virtual camera should be done considering
	 * the possible movement of the telescope or field rotation. Default is to not 
	 * consider this unless the mount is not equatorial and exposition time above 5s.
	 * @param force True or false.
	 */
	public void forceSeveralShots(boolean force) {
		forceSeveralShots = force;
	}
	
	class ShotThread extends Thread {
		public boolean shouldStop = false;
		public ShotThread() {}
		@Override
		public void run() {
			shooting = true;

			// Control minimum time between shots
			if (lastShotStarted > 0) {
				long now = System.currentTimeMillis();
				try {
					int dt = (int) (minInterval - (now - lastShotEnded) * 0.001);
					if (dt > 0.0) {
						System.out.println("Waiting "+dt+" seconds to allow the camera to cool down ...");
						Thread.sleep(dt * 1000);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			lastShotStarted = System.currentTimeMillis();
			try {
				createImage();
			} catch (Exception exc) {
				img = null;
				exc.printStackTrace();
				System.out.println("ERROR USING THE VIRTUAL CAMERA !");
			}
			lastShotEnded = System.currentTimeMillis();
/*			String t = getExpositionTime();
			if (t.equals(Translate.translate(1180))) t = ""+getCCDorBulbModeTime();
			try {
				double timeS = DataSet.getDoubleValueWithoutLimit(t);
				lastShot.add(timeS / Constant.SECONDS_PER_DAY); } catch (JPARSECException e) { e.printStackTrace(); }
*/
			shouldStop = true;
			shooting = false;
		}

		/** Returns if the thread is working or not. */
		public boolean isWorking() {
			return !shouldStop;
		}

		private void createImage() throws JPARSECException {
			// Input data
			field = telescope.getFieldOfView(index); // / 1.6; // Camera multiplication factor
			MOUNT m = telescope.getMount();
			equatorial = (m == MOUNT.EQUATORIAL);

			obs = telescope.getObserver();
			eph = new EphemerisElement(TARGET.NOT_A_PLANET, EphemerisElement.COORDINATES_TYPE.APPARENT,
					EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.TOPOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
					EphemerisElement.FRAME.DYNAMICAL_EQUINOX_J2000,
					EphemerisElement.ALGORITHM.MOSHIER);
			eph.preferPrecisionInEphemerides = false;
			eph.correctForEOP = false;
			eph.correctForPolarMotion = false;

			String t = getExpositionTime();
			if (t.equals(Translate.translate(1180))) t = ""+getCCDorBulbModeTime();
			double timeS = Double.parseDouble(t) * 1000;
			int nimg = 1;
			double stepSeconds = 5;
			if (id != IMAGE_ID.DARK && id != IMAGE_ID.FLAT) {
				if (forceSeveralShots) stepSeconds = 0.2;
				if ((!equatorial || forceSeveralShots) && timeS > stepSeconds) 
					nimg = (int) (timeS / stepSeconds);
			}
			if (nimg > 10) nimg = 10;
			stepSeconds = timeS / nimg;
			timeS = (int) (timeS * Double.parseDouble(getISO()) / 400.0);
			double maglim = 6.0 + timeS / 40.0;
			if (maglim > 15) maglim = 15;

			while (nimg > 50) {
				nimg /= 2;
				stepSeconds *= 2;
			};
			timeS /= nimg;
			time = telescope.getTime().clone();
			startTimeOfLastShot = time.clone();
			if (nimg > 1) time.add(-stepSeconds / Constant.SECONDS_PER_DAY);
			if (img == null) img = new int[w][h];
			if (hotPixel == null) {
				double hotPixelThreshold = 0.00001;
				hotPixel = new boolean[w][h];
				for (int i=0; i<w; i++) {
					for (int j=0; j<h; j++) {
						hotPixel[i][j] = false;
						if (Math.random() < hotPixelThreshold) hotPixel[i][j] = true;
					}
				}
			}

			// Set image to blank with some hot pixels/bad columns
			int nhotp = 20;
			if (model.isCCD()) nhotp = 2;
			int hotpx[] = new int[nhotp];
			int hotpy[] = new int[nhotp];
			for (int i=0; i<nhotp; i++) {
				hotpx[i] = (int) (Math.random() * w);
				hotpy[i] = (int) (Math.random() * h);
			}
			int max = 16384-1; // 14 bit
			if (!raw) max = 255;
			for (int i=0; i<w; i++) {
				for (int j=0; j<h; j++) {
					img[i][j] = 0;
					for (int k=0; k<nhotp; k++) {
						if (i == hotpx[k] && j == hotpy[k]) {
							//System.out.println("HOT PIXEL AT "+i+"/"+j);
							if (model.isCCD()) {
								for (int l=0; l<=j; l++) {
									img[i][l] = max;
								}
							} else {
								img[i][j] = max;
							}
						}
					}
				}
			}
			LocationElement eq = telescope.getApparentEquatorialPosition();
			loc0J2000 = LocationElement.parseRectangularCoordinates(Precession.precessToJ2000(time.astroDate.jd(), eq.getRectangularCoordinates(), eph));
			pixels_per_radian = w / field;
			pixels_per_degree = pixels_per_radian / Constant.RAD_TO_DEG;
			int seeingField = 2; // arcsec, actual seeing is around 1 due to the telescope resolution
			int starSize = (int) ((pixels_per_degree / 3600.0) * seeingField + 0.5);
			if (starSize < 3) starSize = 3;
			if (starSize/2.0 == starSize/2) starSize ++;
			double pattern[][] = Difraction.pattern(TelescopeElement.REFRACTOR_10cm, seeingField);
			ImageSplineTransform ist = new ImageSplineTransform(pattern);
			//pattern = ist.getResizedData(starSize, starSize);
			int patternR = pattern.length/2;

			double pang = 0;
			if (!equatorial) {
				double ast = SiderealTime.apparentSiderealTime(time, obs, eph);
				double angh = ast - eq.getLongitude();
				double sinlat = Math.sin(obs.getLatitudeRad());
				double coslat = Math.cos(obs.getLatitudeRad());
				double sindec = Math.sin(eq.getLatitude()), cosdec = Math.cos(eq.getLatitude());
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

				pang = p;
			}

			for (int n=0; n<nimg; n++) {
				if (nimg > 1) {
					eq = telescope.getApparentEquatorialPosition();
					time.add(stepSeconds / Constant.SECONDS_PER_DAY);
				}

				// Projection values
				centerX = w/2.0;
				centerY = h/2.0;
				poleAngle = orientation;
				if (equatorial) {
					centralLongitude = eq.getLongitude();
					centralLatitude = eq.getLatitude();
				} else {
					LocationElement hz = CoordinateSystem.equatorialToHorizontal(eq, time, obs, eph);
					centralLongitude = hz.getLongitude();
					centralLatitude = hz.getLatitude();
				}

				sin_lat0 = (float) Math.sin(centralLatitude);
				cos_lat0 = (float) Math.cos(centralLatitude);

				double sx = 1.0f;
				if (m == MOUNT.AZIMUTHAL) sx = -1.0f;
				double stx = sx * (float) Math.PI * 1.275;

				double stxx = stx / field;
				stxxTimesCenterX = stxx * centerX;
				cos_lat0_times_sy = cos_lat0 * Math.abs(stxxTimesCenterX);
				sin_lat0_times_sy = sin_lat0 * Math.abs(stxxTimesCenterX);

				// Star reading
				if (re_star == null) { // || (lastEq != null && !lastEq.equals(eq))) {
					re_star = new ReadFile();
					re_star.setThreadName("VirtualCam"+index);
					re_star.setPath(FileIO.DATA_STARS_SKY2000_DIRECTORY + "JPARSEC_Sky2000.txt");
					re_star.setFormat(ReadFile.FORMAT.JPARSEC_SKY2000);
					readFileOfStars(6.5, re_star, time, obs, eph, w, h, equatorial);
					re_star.pathToFile = FileIO.DATA_STARS_SKY2000_DIRECTORY + "JPARSEC_Sky2000_plus.txt";
					readFileOfStars(9.5, re_star, time, obs, eph, w, h, equatorial);
				}
				lastEq = eq;

				Object[] readStars = re_star.getReadElements();
				int maxStars = readStars.length;
				int noise = (int) (((timeS / 50.0) * max) / 100);
				if (id == IMAGE_ID.FLAT) {
					for (int i=0; i<w; i++) {
						for (int j=0; j<h; j++) {
							if (img[i][j] != 0) continue;
							if (raw) {
								if (hotPixel[i][j]) { // hot pixel
									img[i][j] = max;
									continue;
								}
								img[i][j] = (int) (noise * nimg * 0.005 * Math.random());
								img[i][j] += max/100;
							} else {
								img[i][j] = getStarCol(-1, hotPixel[i][j] ? max : max/2); // + (int) (3 * Math.random()));
							}
						}
					}
				} else {
					if (n == 0) {
						for (int i=0; i<w; i++) {
							for (int j=0; j<h; j++) {
								if (img[i][j] != 0) continue;
								if (raw) {
									if (hotPixel[i][j]) { // hot pixel
										img[i][j] = max;
										continue;
									}
									img[i][j] = (int) (noise * nimg * 0.005 * Math.random());
								} else {
									img[i][j] = getStarCol(-1, hotPixel[i][j] ? max : (int) (noise * nimg * Math.random()));
								}
							}
						}
					}
					if (id != IMAGE_ID.DARK) {
						if (DRAW_DSO_TEXTURES) drawDSOImage(maglim, eph, !equatorial, eq, max);

						double noise0 = (int) (noise * nimg * 0.005 * Math.random());
						for (int iii = 0; iii < maxStars; iii++)
						{
							if (readStars[iii] != null) {
								StarData sd = (StarData)readStars[iii];
								if (sd == null) continue;
								sd.pos = null;
								if (sd.mag0 > maglim) {
									maxStars = iii;
									break;
								}

								float pos[];
								LocationElement loc = new LocationElement(sd.ra, sd.dec, 1.0);
								loc = correctEquatorialCoordinatesForRefraction(time, obs, eph, loc);
								if (equatorial) {
									pos = stereographic(loc, telescope, w, h);
								} else {
									LocationElement ll = CoordinateSystem.equatorialToHorizontal(loc, time, obs, eph);
									pos = stereographic(ll, telescope, w, h);
								}

								if (pos != null && (pos[0] <= 0 || pos[0] >= (w-1) || pos[1] <= 0 || pos[1] >= (h-1))) pos = null;
								sd.pos = pos;
							}
						}

						for (int i = 0; i < maxStars; i++)
						{
							StarData sd = (StarData) readStars[i];
							if (sd == null || sd.pos == null) continue;
							float[] pos = sd.pos;
							drawStar(pos, max, maglim, timeS, sd.mag, sd.sp, sd.spi, patternR, pattern, starSize, ist, noise0);
						}

						if (maglim >= 10 && field * Constant.RAD_TO_DEG < 3) {
							eq.setRadius(2062650);
							LocationElement eq2000 = Ephem.toMeanEquatorialJ2000(eq, time, obs, eph);
							if (listUCAC4 == null || (n == 0 && LocationElement.getAngularDistance(eq2000, posUCAC4) > field*2)) {
								listUCAC4 = RenderSky.queryUCAC(eq2000, field * Constant.RAD_TO_DEG, Math.min(14, maglim), 30, time, obs, eph);
								posUCAC4 = eq2000;
							}
							float pos[];
							for (int i=0; i<listUCAC4.size(); i++) {
								String s = listUCAC4.get(i);
								LocationElement loc = new LocationElement(
										Double.parseDouble(FileIO.getField(1, s, " ", true)),
										Double.parseDouble(FileIO.getField(2, s, " ", true)), 1.0);
								loc = correctEquatorialCoordinatesForRefraction(time, obs, eph, loc);

								if (equatorial) {
									pos = stereographic(loc, telescope, w, h);
								} else {
									LocationElement ll = CoordinateSystem.equatorialToHorizontal(loc, time, obs, eph);
									pos = stereographic(ll, telescope, w, h);
								}

								if (pos != null && (pos[0] <= 0 || pos[0] >= (w-1) || pos[1] <= 0 || pos[1] >= (h-1))) pos = null;
								drawStar(pos, max, maglim, timeS, Double.parseDouble(FileIO.getField(3, s, " ", true)), "A", 2, patternR, pattern, starSize, ist, noise0);
							}
						}

						EphemerisElement eph0 = eph.clone();
						TARGET tt[] = new TARGET[] {
								TARGET.Pluto,
								TARGET.NEPTUNE, 
								TARGET.URANUS, 
								TARGET.SATURN,
								TARGET.JUPITER, 
								TARGET.MARS, 
								TARGET.SUN, 
								TARGET.MERCURY, 
								TARGET.VENUS, 
								TARGET.Moon
						};
						float pos[] = null;
						for (int i=0; i<tt.length; i++) {
							eph0.targetBody = tt[i];

							EphemElement ephem = Ephem.getEphemeris(time, obs, eph0, false);
							double d = LocationElement.getApproximateAngularDistance(eq, ephem.getEquatorialLocation());
							if (d < 2 * Constant.DEG_TO_RAD) {
								LocationElement loc2 = correctEquatorialCoordinatesForRefraction(time, obs, eph, ephem.getEquatorialLocation());
								if (equatorial) {
									pos = stereographic(loc2, telescope, w, h);
								} else {
									LocationElement ll = CoordinateSystem.equatorialToHorizontal(loc2, time, obs, eph);
									pos = stereographic(ll, telescope, w, h);
								}
								if (pos != null && (pos[0] <= 0 || pos[0] >= (w-1) || pos[1] <= 0 || pos[1] >= (h-1))) pos = null;
								if (pos != null) {
									float radius = (float) (0.5 + ephem.angularRadius * pixels_per_radian);
									if (radius < 5 || !DRAW_PLANETARY_TEXTURES) {
										drawStar(pos, max, maglim, timeS, ephem.magnitude, "A", 2, patternR, pattern, starSize, ist, noise0);
									} else {
										int oversample = 5; // Reach Saturn rings
										if (tt[i] != TARGET.SATURN && tt[i] != TARGET.URANUS && tt[i] != TARGET.NEPTUNE) oversample = 2;
										PlanetRenderElement render = new PlanetRenderElement((int)(radius*oversample), (int)(radius*oversample), false, true, false, false, true, true);
										render.highQuality = false;
										TelescopeElement tel = TelescopeElement.SCHMIDT_CASSEGRAIN_20cm;
										tel.ocular.focalLength = TelescopeElement.getOcularFocalLengthForCertainField(ephem.angularRadius*oversample, tel);
										tel.invertHorizontal = telescope.invertHorizontally();
										tel.invertVertical = telescope.invertVertically();
										render.telescope = tel;
										eph0 = eph.clone();
										eph0.targetBody = tt[i];
										PlanetaryRendering renderPlanet = new PlanetaryRendering(time, obs, eph0, render, "Planet rendering");
										BufferedImage img0 = renderPlanet.createBufferedImage();
										double ang2 = -poleAngle;
										if (!equatorial) ang2 += pang;
										Picture pic = new Picture(img0);
										pic.rotate(ang2, img0.getWidth()/2, img0.getHeight()/2);
										img0 = pic.getImage();
										Graphics2D g = img0.createGraphics();
										int x0 = (int) (pos[0] - img0.getWidth()/2.0);
										int y0 = (int) (pos[1] - img0.getHeight()/2.0);
										for (int x = x0; x < x0 + img0.getWidth(); x ++) {
											if (x < 0) continue;
											if (x >= w) continue;
											for (int y = y0; y < y0 + img0.getHeight(); y ++) {
												if (y < 0) continue;
												if (y >= h) continue;

												if (!raw) {
													int colBack[] = Functions.getColorComponents(img[x][y]);
													int colPlan[] = Functions.getColorComponents(img0.getRGB(x-x0, y-y0));
													int pt = colPlan[0] + colPlan[1] + colPlan[2];
													if (pt > 0) {
														g.setColor(new Color(colPlan[0], colPlan[1], colPlan[2], 255));
														g.fillRect(x-x0, y-y0, 1, 1);
														int pn = colBack[0] + colBack[1] + colBack[2];
														if (pn < noise * 3 * nimg) {
															g.setColor(new Color(colBack[0], colBack[1], colBack[2], 64));
															g.fillRect(x-x0, y-y0, 1, 1);
														}
													} else {
														g.setColor(new Color(colBack[0], colBack[1], colBack[2], 255));
														g.fillRect(x-x0, y-y0, 1, 1);														
													}
												}
												img[x][y] = img0.getRGB(x-x0, y-y0);
												if (raw) {
													int comp = x % 2 + y % 2;
													int cc = img[x][y];
													if (comp == 0) cc = (cc>>16)&255;
													if (comp == 1) cc = (cc>>8)&255;
													if (comp == 2) cc = (cc)&255;
													img[x][y] = (cc * max) / 255;
													if (img[x][y] > max) img[x][y] = max;
												}
											}
										}
										g.dispose();
									}
								}


								// Moons
								MoonEphemElement mephem[] = null;
								try {
									if (tt[i] == TARGET.MARS) mephem = MoonEphem.martianSatellitesEphemerides_2007(time, obs, eph0);
									if (tt[i] == TARGET.JUPITER) mephem = MoonEphem.galileanSatellitesEphemerides_L1(time, obs, eph0);
									if (tt[i] == TARGET.SATURN) mephem = MoonEphem.saturnianSatellitesEphemerides_TASS17(time, obs, eph0, false);
									if (tt[i] == TARGET.URANUS) mephem = MoonEphem.uranianSatellitesEphemerides_GUST86(time, obs, eph0);
									if (tt[i] == TARGET.NEPTUNE) mephem = new MoonEphemElement[] {MoonEphem.calcJPLSatellite(time, obs, eph0, TARGET.Triton.getName())};
									if (tt[i] == TARGET.Pluto) mephem = new MoonEphemElement[] {MoonEphem.calcJPLSatellite(time, obs, eph0, TARGET.Charon.getName())};
								} catch (Exception exc) {}
								if (mephem == null) continue;
								for (int j=0; j<mephem.length; j++) {
									if (mephem[j].eclipsed || mephem[j].occulted) continue;
									loc2 = correctEquatorialCoordinatesForRefraction(time, obs, eph, mephem[j].getEquatorialLocation());
									if (equatorial) {
										pos = stereographic(loc2, telescope, w, h);
									} else {
										LocationElement ll = CoordinateSystem.equatorialToHorizontal(loc2, time, obs, eph);
										pos = stereographic(ll, telescope, w, h);
									}
									if (pos != null && (pos[0] <= 0 || pos[0] >= (w-1) || pos[1] <= 0 || pos[1] >= (h-1))) pos = null;
									if (pos != null)
										drawStar(pos, max, maglim, timeS, mephem[j].magnitude, "A", 2, patternR, pattern, starSize, ist, noise0);
								}
							}
						}

						// Asteroids and comets
						Object o = DataBase.getDataForAnyThread("asterEphem", true);
						if (o != null) {
							EphemElement aster[] = (EphemElement[]) o;
							for (int j=0; j<aster.length; j++) {
								LocationElement loc2 = correctEquatorialCoordinatesForRefraction(time, obs, eph, aster[j].getEquatorialLocation());
								if (equatorial) {
									pos = stereographic(loc2, telescope, w, h);
								} else {
									LocationElement ll = CoordinateSystem.equatorialToHorizontal(loc2, time, obs, eph);
									pos = stereographic(ll, telescope, w, h);
								}
								if (pos != null && (pos[0] <= 0 || pos[0] >= (w-1) || pos[1] <= 0 || pos[1] >= (h-1))) pos = null;
								if (pos != null)
									drawStar(pos, max, maglim, timeS, aster[j].magnitude, "A", 2, patternR, pattern, starSize, ist, noise0);
							}
						}
						o = DataBase.getDataForAnyThread("cometEphem", true);
						if (o != null) {
							EphemElement comet[] = (EphemElement[]) o;
							for (int j=0; j<comet.length; j++) {
								LocationElement loc2 = correctEquatorialCoordinatesForRefraction(time, obs, eph, comet[j].getEquatorialLocation());
								if (equatorial) {
									pos = stereographic(loc2, telescope, w, h);
								} else {
									LocationElement ll = CoordinateSystem.equatorialToHorizontal(loc2, time, obs, eph);
									pos = stereographic(ll, telescope, w, h);
								}
								if (pos != null && (pos[0] <= 0 || pos[0] >= (w-1) || pos[1] <= 0 || pos[1] >= (h-1))) pos = null;
								if (pos != null)
									drawStar(pos, max, maglim, timeS, comet[j].magnitude, "A", 2, patternR, pattern, starSize, ist, noise0);
							}
						}						
					}
				}
			}
			if (raw) {
				ObservationManager.writePGM(lastImage, img, max);
			} else {
				Picture pic = new Picture(img);
				pic = new Picture(pic.getImageAsByteArray(0), pic.getImageAsByteArray(1), pic.getImageAsByteArray(2), null);
				pic.write(lastImage);
			}
		}

		private void drawStar(float[] pos, int max, double maglim, double timeS, double mag, String sp, int spi,
				int patternR, double pattern[][], int starSize, ImageSplineTransform ist, double noise) {
			if (pos != null && pos[0] >= 0 && pos[0] < w && pos[1] >= 0 && pos[1] < h) {
				if (mag > maglim) return;
				double nc = Math.pow(10.0, 1 + maglim - mag); // * max / 1E10;
				if (nc > max) nc = max;
				double ss = 1.0/starSize;
				if (!raw) {
					max = 255;
					nc = (timeS / 1.0) * max * (1.0 - (mag + 2.0) / (2.0 + maglim));
				}
				ss *= 5 * (1.0 + maglim - mag) / (maglim + 10.0);

				double frac[] = new double[] {1.0, 1.0, 1.0};
				if (!sp.isEmpty() && spi >= 0) {
					int fc[] = Functions.getColorComponents(getStarCol(spi, 255));
					frac = new double[] {fc[0]/255.0, fc[1]/255.0, fc[2]/255.0};
				}

				int x = (int) (pos[0] + 0.5), y = (int) (pos[1] + 0.5);
				for (int rx = x-patternR; rx<=x+patternR; rx++) {
					if (rx < 0 || rx >= w) continue;
					double vx = patternR + ((rx-(pos[0])))/ss;
					for (int ry = y-patternR; ry<=y+patternR; ry++) {
						if (ry < 0 || ry >= h) continue;

						double vy = patternR + ((ry-(pos[1])))/ss;
						try {
							if (vx < 0 || vy < 0 || vx > ist.getWidth()-1 || vy > ist.getHeight()-1) continue;
							if (FastMath.hypot(vx-patternR, vy-patternR) > ist.getWidth()*0.5) continue;
							double v = ist.interpolate(vx, vy);
							if (!raw) {
								int coli = (int) (nc * v);
								int c[] = Functions.getColorComponents(img[rx][ry]);
								if (!sp.isEmpty() && spi >= 0) {
									img[rx][ry] = getStarCol(spi, coli);
								} else {
									img[rx][ry] = getStarCol(-1, coli);
								}
								int cnew[] = Functions.getColorComponents(img[rx][ry]);
								img[rx][ry] = Functions.getColor(Math.min(max, cnew[0] + c[0]), Math.min(max, cnew[1] + c[1]), Math.min(max, cnew[2] + c[2]), 255);
							} else {
								int px = rx % 2, py = ry % 2;
								int coli = (int) (nc * v * frac[px+py]); // RGGB matrix
								img[rx][ry] += coli;
								if (img[rx][ry] > max) img[rx][ry] = max;
								if (img[rx][ry] < 0) img[rx][ry] = 0;
							}
						} catch (Exception exc) { exc.printStackTrace(); }
					}
				}
			}
		}

		private int getStarCol(int index, int v) {
			if (v > 255) v = 255;
			int v2 = (v * 2) / 3;
			int min = Math.min(v2, 100);
			if (index == 0) return 255<<24 | min<<16 | min<<8 | v2; // O
			if (index == 1) return 255<<24 | min<<16 | min<<8 | v; // B
			if (index == 2) return 255<<24 | v<<16 | v<<8 | v; // A
			if (index == 3) return 255<<24 | min<<16 | v<<8 | min; // F
			if (index == 4) return 255<<24 | v<<16 | v<<8 | min; // G
			if (index == 5) return 255<<24 | v<<16 | v2<<8 | min; // K
			if (index == 6) return 255<<24 | v<<16 | min<<8 | min; // M
			return 255<<24 | v<<16 | v<<8 | v;
		}

		private void drawDSOImage(double maglim, EphemerisElement eph, boolean horizontal, LocationElement locEq, int max)
				throws JPARSECException {
			Object o = DataBase.getDataForAnyThread("objects", true);
			if (o == null) JPARSECException.addWarning("Could not find objects in the database, a sky rendering should be performed first");
			if (o == null) return;
			ArrayList<Object> objects = new ArrayList<Object>(Arrays.asList((Object[]) o));
			Object oo = DataBase.getData("objectsJ2000", null, true);
			if (oo == null) JPARSECException.addWarning("Could not find objects in the database, a sky rendering should be performed first");
			if (oo == null) return;
			ArrayList<Object> objectsJ2000 = new ArrayList<Object>(Arrays.asList((Object[]) oo));

			float size_xy[], size0, pos0[], size;
			String name;
			LocationElement loc;
			double jd = TimeScale.getJD(time, obs, eph, SCALE.TERRESTRIAL_TIME);
			double baryc[] = null, equinox = eph.getEpoch(jd), pang = 0;
			if (horizontal) {
				double ast = SiderealTime.apparentSiderealTime(time, obs, eph);
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

				pang = p;
			}

			for (Iterator<Object> itr = objects.iterator();itr.hasNext();)
			{
				Object[] obj = (Object[]) itr.next();
				if (obj.length == 9) break; // external catalog

				float mag;
				if (obj.length <= 3) {
					loc = ((LocationElement) obj[1]);
					if (obj.length == 3) {
						mag = (Float) obj[2];
						obj = (Object[]) objectsJ2000.get((Integer) obj[0]);
					} else {
						obj = (Object[]) objectsJ2000.get((Integer) obj[0]);
						mag = (Float) obj[4];
					}
					
					// Ensure loc are equatorial coordinates
					loc = ((LocationElement) obj[3]);
					loc.setRadius(1000 * Constant.RAD_TO_ARCSEC); // 1000 pc
					double light_time = loc.getRadius() * Constant.LIGHT_TIME_DAYS_PER_AU;
					double r[] = Ephem.aberration(loc.getRectangularCoordinates(), baryc, light_time);

					r = Precession.precessFromJ2000(equinox, r, eph);
					loc = LocationElement.parseRectangularCoordinates(Nutation.nutateInEquatorialCoordinates(equinox, eph, r, true));
				} else {
					//mag = (Float) obj[4];
					//loc = ((LocationElement) obj[3]);
					continue;
				}
				
				String messier = (String) obj[1];
				if (mag > maglim) break;


				int type = (Byte) obj[2];
				if (maglim > 12 && (type == 4 || type == 5)) { // mag components around cluster mag + 3
					// Don't draw the glob cl itself, when the cluster is partially
					// resolved in stars
					size_xy = (float[]) obj[5];
					if (size_xy[0] > 0.1) type = -type; // size greater than 0.1 deg = 6'
				}

				size_xy = (float[]) obj[5];
				size0 = (float) (size_xy[0] * pixels_per_degree) + 1;
				
				
				loc = correctEquatorialCoordinatesForRefraction(time, obs, eph, loc);
				if (equatorial) {
					pos0 = stereographic(loc, telescope, w, h);
				} else {
					LocationElement ll = CoordinateSystem.equatorialToHorizontal(loc, time, obs, eph);
					pos0 = stereographic(ll, telescope, w, h);
				}

				if (pos0 != null && (pos0[0] <= 0 || pos0[0] >= (w-1) || pos0[1] <= 0 || pos0[1] >= (h-1))) pos0 = null;
				if (pos0 == null) continue;

				name = (String) obj[0];
				size = Math.max(size0, 3);

				if (size < 15) continue;

				String file = name.toLowerCase() + ".jpg";

				if (file.indexOf("caldwell")>=0) file = ""; // FIXME Unsupported
				if (DataSet.isDoubleFastCheck(name) || (name.length() > 4 && DataSet.isDoubleFastCheck(name.substring(0, 4))))
					file = "ngc "+name+".jpg";
				if (name.startsWith("I.")) file = "ic "+name.substring(2).toLowerCase()+".jpg";
				if (name.startsWith("QSO")) file = name.toLowerCase()+".png";
				if (!messier.isEmpty() && !messier.startsWith("C")) file = messier.toLowerCase()+".jpg";
				String file0 = file;
				double fieldDeg = field * Constant.RAD_TO_DEG;
				if (	(file0.equals("m43.jpg") && fieldDeg > 1)
						//|| (file0.equals("ngc 2244.jpg") && fieldDeg > 0.44)
						|| ((file0.equals("m110.jpg") || file0.equals("m32.jpg")) && fieldDeg > 3)
						|| (file0.equals("ngc 5195.jpg") && fieldDeg > 0.0)
//						|| (Math.abs(jd-Constant.J2000) > 365250 && (Math.abs(type) == 2 || Math.abs(type) == 4))
					    // In epochs far from J2000 stars in clusters/nebula will appear moved due to proper motions
						) {
					continue;
				} else {
					file = FileIO.DATA_TEXTURES_DIRECTORY + "deepsky/"+file;
					BufferedImage img0 = null;
					try { img0 = ReadFile.readImageResource(file); } catch (Exception exc) {}
					if (img0 != null) {
						o = DataBase.getDataForAnyThread("deepSkyTextures", true);
						String list[] = null;
						if (o == null) {
							String file2 = FileIO.DATA_TEXTURES_DIRECTORY + "deepsky/nebula_textures.fab";
							list = DataSet.arrayListToStringArray(ReadFile.readResource(file2));
							DataBase.addData("deepSkyTextures", Thread.currentThread().getName(), list, true);
						} else {
							list = (String[])o;
						}
						//if (file0.equals("ic 434.jpg")) file0 = "ic434.png";
						if (file0.equals("ngc 1499.jpg")) file0 = "ngc1499.png";
						if (file0.equals("m27.jpg")) file0 = "m27.png";
						if (file0.equals("m57.jpg")) file0 = "m57.png";
						if (file0.equals("m42.jpg")) file0 = "m42.png";
						if (file0.equals("m37.jpg")) file0 = "m37.png";
						int index = DataSet.getIndexContaining(list, file0);
						if (index >= 0 && !list[index].startsWith("#")) {
							int c = 255 << 24 | 0<<16 | 0<<8 | 0;
							img0 = Picture.makeTransparent(img0, new Color(c), new Color(10, 10, 10, 255), 0);

							double sc = Double.parseDouble(FileIO.getField(5, list[index].trim(), UnixSpecialCharacter.UNIX_SPECIAL_CHARACTER.TAB.value, true));
							double rot = Double.parseDouble(FileIO.getField(6, list[index].trim(), UnixSpecialCharacter.UNIX_SPECIAL_CHARACTER.TAB.value, true));
							float scale = (float) ((sc * pixels_per_degree / 60.0) / img0.getWidth()), ang = (float)(rot * Constant.DEG_TO_RAD);

							float radius_x = img0.getWidth() * scale * 0.5f;
							float radius_y = img0.getHeight() * scale * 0.5f;

							if (radius_x < 2*w) {
								if (telescope.invertHorizontally() || telescope.invertVertically()) {
									int w = 1, h = 1;
									if (telescope.invertHorizontally()) {
										w = -1;
										ang = (float) (Math.PI - ang);
									}
									if (telescope.invertVertically()) {
										h = -1;
										ang = -ang;
									}
									Picture pic = new Picture(img0);
									pic.getScaledInstance(img0.getWidth()*w, img0.getHeight()*h, true);
									img0 = pic.getImage();
								}


								double ra = Double.parseDouble(FileIO.getField(2, list[index].trim(), UnixSpecialCharacter.UNIX_SPECIAL_CHARACTER.TAB.value, true)) * Constant.DEG_TO_RAD;
								double dec = Double.parseDouble(FileIO.getField(3, list[index].trim(), UnixSpecialCharacter.UNIX_SPECIAL_CHARACTER.TAB.value, true)) * Constant.DEG_TO_RAD;
									loc = new LocationElement(ra, dec, 1.0);
									if (equinox != Constant.J2000) {
										// Correct for aberration, precession, and nutation
										if (eph.ephemType == COORDINATES_TYPE.APPARENT) {
											loc.setRadius(1000 * Constant.RAD_TO_ARCSEC); // 1000 pc
											double light_time = loc.getRadius() * Constant.LIGHT_TIME_DAYS_PER_AU;
											if (baryc == null)
												baryc = Ephem.eclipticToEquatorial(PlanetEphem.getGeocentricPosition(equinox, TARGET.Solar_System_Barycenter, 0.0, false, obs), Constant.J2000, eph);

											double r[] = Ephem.aberration(loc.getRectangularCoordinates(), baryc, light_time);

											r = Precession.precessFromJ2000(equinox, r, eph);
											loc = LocationElement.parseRectangularCoordinates(Nutation.nutateInEquatorialCoordinates(equinox, eph, r, true));
										} else {
											loc = LocationElement.parseRectangularCoordinates(Precession.precessFromJ2000(equinox,
													LocationElement.parseLocationElement(loc), eph));
										}
									}

									if (loc == null) continue;

								double ang2 = -poleAngle;
								if (horizontal) {
									loc = CoordinateSystem.equatorialToHorizontal(loc, time, obs, eph);
									loc.setLatitude(Ephem.getApparentElevation(eph, obs, loc.getLatitude(), 5));
									ang2 += pang;
								} else {
									loc = correctEquatorialCoordinatesForRefraction(time, obs, eph, loc);
								}

								float[] pos00 = stereographic(loc, telescope, w, h);
								if (pos00 != null && (pos00[0] <= -w || pos00[0] >= 2*(w-1) || pos00[1] <= -h || pos00[1] >= 2*(h-1))) pos00 = null;
								if (pos00 == null) continue;
								if (telescope.invertHorizontally() || telescope.invertVertically()) {
									if (telescope.invertHorizontally()) {
										ang2 = (Math.PI - ang2);
									}
									if (telescope.invertVertically()) {
										ang2 = -ang2;
									}
								}
								ang += ang2;

								Picture pic = new Picture(img0);
								pic.getScaledInstance(2*(int)radius_x, 2*(int)radius_y, true);
								pic.rotate(ang, (int)radius_x, (int)radius_y);
								img0 = pic.getImage();
								Graphics2D g = img0.createGraphics();
								AWTGraphics.disableAntialiasing(g);
								int x0 = (int) (pos00[0] - radius_x), y0 = (int) (pos00[1] - radius_y);
								for (int x = x0; x < x0 + img0.getWidth(); x ++) {
									if (x < 0) continue;
									if (x >= w) break;
									for (int y = y0; y < y0 + img0.getHeight(); y ++) {
										if (y < 0) continue;
										if (y >= h) break;

										int px = x - x0, py = y - y0;
										if (!raw) {
											int colBack[] = Functions.getColorComponents(img[x][y]);
											int colPlan[] = Functions.getColorComponents(img0.getRGB(px, py));
											int pt = colPlan[0] + colPlan[1] + colPlan[2];
											if (pt > 105) {
												//g.setColor(new Color(colPlan[0], colPlan[1], colPlan[2], 255));
												//g.fillRect(px, py, 1, 1);
												g.setColor(new Color(colBack[0], colBack[1], colBack[2], 100));
												g.fillRect(px, py, 1, 1);
											} else {
												g.setColor(new Color(colBack[0], colBack[1], colBack[2], 255));
												g.fillRect(px, py, 1, 1);														
											}
										}
										img[x][y] = img0.getRGB(px, py);
										if (raw) {
											int comp = x % 2 + y % 2;
											int cc = img[x][y];
											if (comp == 0) cc = (cc>>16)&255;
											if (comp == 1) cc = (cc>>8)&255;
											if (comp == 2) cc = (cc)&255;
											img[x][y] = (cc * max) / 255;
											if (img[x][y] > max) img[x][y] = max;
										}
									}
								}
								g.dispose();
							}
						}
					}
				}
			}
		}
	}

	private static double lastLST = 0, lastJD = 0;
	private static LocationElement correctEquatorialCoordinatesForRefraction(TimeElement time, ObserverElement obs,
			EphemerisElement eph, LocationElement loc) throws JPARSECException {
		double lst = lastLST;
		double jd = TimeScale.getJD(time, obs, eph, SCALE.TERRESTRIAL_TIME);
		if (Math.abs(jd-lastJD) > 0.001 / Constant.SECONDS_PER_DAY) {
			lst = SiderealTime.apparentSiderealTime(time, obs, eph);
			lastLST = lst;
			lastJD = jd;
		}
		
		// Can be done in two ways: correcting equatorial position as explained by NOVAS (below),
		// or doing direct/inverse transformations. Unless there's an error in the algorithm below
		// that reduces accuracy, this option seems more consistent and accurate.
		LocationElement out = CoordinateSystem.equatorialToHorizontal(loc, lst, obs, eph, true, false);
		return CoordinateSystem.horizontalToEquatorial(out, lst, obs.getLatitudeRad(), false);
	}
	
	private double centralLongitude, centralLatitude;
	private double centerX;
	private double centerY;
	private double poleAngle;
	private double sin_lat0;
	private double cos_lat0;
	private double cos_lat0_times_sy;
	private double sin_lat0_times_sy;
	private double stxxTimesCenterX;
	private double field;
	private double pixels_per_degree;
	private double pixels_per_radian;
	private LocationElement loc0J2000, lastEq;
	private TimeElement time;
	/** Start of last shot. */
	public TimeElement startTimeOfLastShot;
	private ObserverElement obs;
	private EphemerisElement eph;
	private boolean equatorial;
	private ReadFile re_star = null;
	private float[] stereographic(LocationElement loc, GenericTelescope telescope, int w, int h)
	{
		double sin_lat = FastMath.sin(loc.getLatitude());
		double cos_lat = FastMath.cos(loc.getLatitude());
		double dlon = loc.getLongitude() - centralLongitude;
		if (dlon < 0 || dlon > Constant.TWO_PI) dlon = Functions.normalizeRadians(dlon);

		double hh = cos_lat * FastMath.cos(dlon);
		double div = (1.0f + sin_lat0 * sin_lat + cos_lat0 * hh);
		if (div == 0) return null;

		double pox = (centerX - stxxTimesCenterX * (cos_lat * FastMath.sin(dlon) / div));
		double poy = (centerY - (cos_lat0_times_sy * sin_lat - sin_lat0_times_sy * hh) / div);

		if (telescope.invertHorizontally()) pox = w - pox;
		if (telescope.invertVertically()) poy = h - poy;

		if (poleAngle != 0.0)
		{
			double dx = pox - centerX;
			double dy = poy - centerY;

			double r = FastMath.hypot(dx, dy);
			double ang = FastMath.atan2_accurate(dy, dx);

			pox = centerX + (r * FastMath.cosf(ang + poleAngle));
			poy = centerY + (r * FastMath.sinf(ang + poleAngle));
		}

		return new float[]{(float) pox, (float) poy};
	}
	private void readFileOfStars(double maglim, ReadFile re, TimeElement time, ObserverElement obs,
			EphemerisElement eph, int w, int h, boolean equatorial) throws JPARSECException
	{
		// Define necesary variables 
		String file_line = "";

		ArrayList<StarData> list = new ArrayList<StarData>();
		Object o[] = re.getReadElements();

		ReadFormat rf = new ReadFormat();
		rf.setFormatToRead(FileFormatElement.JPARSEC_SKY2000_FORMAT);
		String greek = "AlpBetGamDelEpsZetEtaTheIotKapLamMu Nu Xi OmiPi RhoSigTauUpsPhiChiPsiOme";
		double lim = Constant.PI_OVER_TWO + 0.6 * Constant.DEG_TO_RAD; // aberration + nutation + refraction
		double jd = TimeScale.getJD(time, obs, eph, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
		double cte = 100.0 * Math.abs(Functions.toCenturies(jd));
		int CRITICAL_NSTARS = 4255;
		int index = -1;
		if (o != null) index = 8883;
		double[] baryc = Ephem.eclipticToEquatorial(PlanetEphem.getGeocentricPosition(jd, TARGET.Solar_System_Barycenter, 0.0, false, obs), Constant.J2000, eph);

		// Connect to the file
		try
		{
			InputStream is = VirtualCamera.class.getClassLoader().getResourceAsStream(re.pathToFile);
			BufferedReader dis = new BufferedReader(new InputStreamReader(is));

			while ((file_line = dis.readLine()) != null)
			{
				// Obtain fields
				StarData star = parseJPARSECfile(file_line, rf, greek, eph, lim, cte, baryc, jd, time, obs, w, h, equatorial);
				index ++;
				if (star != null) {
					if (star.mag > maglim) break;
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
		} catch (Exception e2)
		{
			throw new JPARSECException(
					"error while reading file " + re.pathToFile, e2);
		}
	}
	private StarData parseJPARSECfile(String line, ReadFormat rf, String greek,
			EphemerisElement eph, double lim, double cte, double baryc[], double jd,
			TimeElement time, ObserverElement obs, int w, int h, boolean equatorial) throws JPARSECException
	{
		try {
			StarElement star = new StarElement();

			star.rightAscension = rf.readDouble(line, "RA");
			star.rightAscension = star.rightAscension / Constant.RAD_TO_HOUR;
			star.declination = rf.readDouble(line, "DEC");
			star.declination = star.declination * Constant.DEG_TO_RAD;

			LocationElement locStar0 = new LocationElement(star.rightAscension, star.declination, 1.0);
			double approxAngDist = LocationElement.getApproximateAngularDistance(loc0J2000, locStar0) 
					+ 0.5 * Constant.DEG_TO_RAD;
			if (approxAngDist > field*2) return null;

			star.properMotionRA = (float) (rf.readDouble(line, "RA_PM") * 15.0 * Constant.ARCSEC_TO_RAD);
			star.properMotionDEC = (float) (rf.readDouble(line, "DEC_PM") * Constant.ARCSEC_TO_RAD);

			double properM = Math.max(Math.abs(star.properMotionDEC), Math.abs(star.properMotionRA / FastMath.cos(star.declination)));

			star.name = rf.readString(line, "NAME");
			star.spectrum = rf.readString(line, "SPECTRUM");
			star.type = rf.readString(line, "TYPE")+";"+rf.readString(line, "DATA");
			star.magnitude = (float) rf.readDouble(line, "MAG");
			star.properMotionRadialV = 0.0f;
			if (!rf.readString(line, "RADIAL_VELOCITY").isEmpty())
				star.properMotionRadialV = (float) rf.readDouble(line, "RADIAL_VELOCITY");
			double parallax = rf.readDouble(line, "PARALLAX");
			star.parallax = parallax;
			star.equinox = Constant.J2000;
			star.frame = EphemerisElement.FRAME.ICRF;

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
				try
				{
					constell = Constellation.getConstellation(Constellation.getConstellationName(star.rightAscension, star.declination, Constant.J2000,
							eph), CONSTELLATION_NAME.ABREVIATED);
				} catch (JPARSECException ve)
				{
					constell = "";
				}
				if (!constell.isEmpty()) {
					constel = code + " " + constell.substring(0, 3);
				} else {
					constel = code;
				}
				if (!constel.isEmpty())
					star.name += " (" + constel + ") (" + id + ")";
			}

			StarData sd = null;

			double cte1 = 30 * Constant.ARCSEC_TO_DEG * pixels_per_degree; // aberration + nutation
			double cte11 = cte1 / pixels_per_radian;
			double r[];
			double fieldLimit = field * 0.5;
			fieldLimit = 1.05*fieldLimit*Math.sqrt(1.0+(h/(double)w));
			if (fieldLimit < Constant.DEG_TO_RAD) fieldLimit = Constant.DEG_TO_RAD;
			double jYearsFromJ2000 = Math.abs(jd - Constant.J2000) / 365.25;

			properM = Math.max(Math.abs(star.properMotionDEC), Math.abs(star.properMotionRA / FastMath.cos(star.declination)));

			LocationElement l = locStar0.clone();

			/* space motion */
			if (properM > 0 && approxAngDist < (fieldLimit + properM * jYearsFromJ2000 + cte11) && eph.ephemType != COORDINATES_TYPE.GEOMETRIC)
			{
				double q[] = LocationElement.parseLocationElement(l), p[] = new double[3];
				double relativisticFactor = 1.0 / (1.0 - star.properMotionRadialV / Constant.SPEED_OF_LIGHT);
				double sindec = Math.sin(star.declination);
				double cosdec = Math.cos(star.declination);
				double cosra = Math.cos(star.rightAscension);
				double sinra = Math.sin(star.rightAscension);
				double cte2 = 0.21094952658238966; // Constant.SECONDS_PER_DAY * Constant.JULIAN_DAYS_PER_CENTURY * 0.01 / Constant.AU;
				double vpi = cte2 * star.properMotionRadialV * star.parallax * 0.001 / Constant.RAD_TO_ARCSEC;
				double m[] = new double[3];
				m[0] = (-star.properMotionRA * cosdec * sinra - star.properMotionDEC * sindec * cosra + vpi * q[0]) * relativisticFactor;
				m[1] = (star.properMotionRA * cosdec * cosra - star.properMotionDEC * sindec * sinra + vpi * q[1]) * relativisticFactor;
				m[2] = (star.properMotionDEC * cosdec + vpi * q[2]) * relativisticFactor;
				double T = (jd - star.equinox) * 100.0 / Constant.JULIAN_DAYS_PER_CENTURY;
				for (int i = 0; i < 3; i++)
				{
					p[i] = q[i] + T * m[i] + baryc[i] * star.parallax * 0.001 / Constant.RAD_TO_ARCSEC;
				}
				l = LocationElement.parseRectangularCoordinates(p);
			}

			l.setRadius(l.getRadius() * star.getDistance() * Constant.RAD_TO_ARCSEC);
			//if (jd != Constant.J2000) {
				// Correct for aberration, precession, and nutation
				if (eph.ephemType == COORDINATES_TYPE.APPARENT) {
					double light_time = l.getRadius() * Constant.LIGHT_TIME_DAYS_PER_AU;
					r = Ephem.aberration(l.getRectangularCoordinates(), baryc, light_time);

					r = Ephem.toOutputFrame(r, star.frame, eph.frame);
					r = Precession.precessFromJ2000(jd, r, eph);
					r = Nutation.nutateInEquatorialCoordinates(jd, eph, r, true);

					// Correct for polar motion
					if (eph.ephemType == EphemerisElement.COORDINATES_TYPE.APPARENT &&
							eph.correctForPolarMotion)
					{
						double gast = SiderealTime.greenwichApparentSiderealTime(time, obs, eph);
						r = Functions.rotateZ(r, -gast);
						Matrix mat = IAU2006.getPolarMotionCorrectionMatrix(time, obs, eph);
						r = mat.times(new Matrix(r)).getColumn(0);
						r = Functions.rotateZ(r, gast);
					}

					l = LocationElement.parseRectangularCoordinates(r);
				} else {
					l = LocationElement.parseRectangularCoordinates(Precession.precessFromJ2000(jd,
							LocationElement.parseLocationElement(l), eph));
				}
			//}

			if (equatorial) {
				sd = new StarData(l, star.magnitude, star.spectrum, star.type);
				sd.loc = correctEquatorialCoordinatesForRefraction(time, obs, eph, l);
			} else {
				LocationElement ll = CoordinateSystem.equatorialToHorizontal(l, time, obs, eph);
				ll.setLatitude(Ephem.getApparentElevation(eph, obs, ll.getLatitude(), 5));
				sd = new StarData(ll, star.magnitude, star.spectrum, star.type);
			}
			sd.mag0 = star.magnitude;
			sd.ra = l.getLongitude();
			sd.dec = l.getLatitude();

			String spectrum = "OBAFGKM";
			sd.spi = -1;
			if (!sd.sp.equals("")) sd.spi = (short) spectrum.indexOf(sd.sp.substring(0, 1));
			int bracket1 = star.name.indexOf("(");
			int bracket2 = star.name.indexOf(")");
			if (bracket1 >= 0 && bracket2 >= 0) {
				sd.nom2 = star.name.substring(bracket1 + 1, bracket2);

				bracket1 = star.name.lastIndexOf("(");
				bracket2 = star.name.lastIndexOf(")");
				String name2 = star.name.substring(bracket1 + 1, bracket2);
				String name3 = "";
				int n3 = name2.indexOf("-");
				if (n3 >= 0) {
					name3 = "^{"+name2.substring(n3+1)+"}";
					name2 = name2.substring(0, n3);
				}
				//label = "@SIZE+4"+greek[Integer.parseInt(name2) - 1]+name3+"@SIZE-4";
				sd.properName = name3;
			}

			return sd;
		} catch (Exception exc) {
			exc.printStackTrace();
			JPARSECException.addWarning("Could not parse this star in JPARSEC format, returning null. Details: "+exc.getLocalizedMessage()+". Line to parse was: "+line);
		}
		return null;
	}

	/**
	 * Returns a catalog of stars in a given sky direction. This catalog can be later used to identify
	 * stars in a given field using real observations. Note that compared to SExtractor, first pixel
	 * here is (0, 0) and in SExtractor is (1, 1).
	 * @param eq The apparent equatorial position of the center of the field.
	 * @param field The field of view in radians.
	 * @param w Width in pixels of the image.
	 * @param h Height in pixels of the image.
	 * @param orientation The orientation of the field respect north or zenith (depending on the mount).
	 * @param m The mount type, equatorial or azimuthal.
	 * @param maglim The limiting magnitude. In case of a value greater than 10 the UCAC3 catalog will
	 * be used for stars fainter than magnitude 9.5.
	 * @param telescope The telescope to use. The field will be inverted in horizontal or vertical
	 * axis depending on the telescope.
	 * @param time The time object.
	 * @param obs The observer object.
	 * @param eph The ephemeris object.
	 * @param nstars The number of stars to return. -1 for unlimited number.
	 * @param includePlanets Set to true to include planets and satellites in the output catalog.
	 * @return A list containing all stars in the field: x position, y position, magnitude, RA, DEC (apparent),
	 * variability flag. Column separator is a comma. RA and DEC are given in radians. Variability flag
	 * is V for variable stars, N for non variable stars, and - if unknown. Additional fields include spectral
	 * type and name ("-" if unknown).
	 * @throws JPARSECException If an error occurs.
	 */
	public static String[] getStarCatalog(LocationElement eq, double field, int w, int h,
			double orientation, MOUNT m, double maglim, GenericTelescope telescope,
			TimeElement time, ObserverElement obs, EphemerisElement eph, int nstars, boolean includePlanets) throws JPARSECException {
		VirtualCamera vc = new VirtualCamera(CAMERA_MODEL.VIRTUAL_CAMERA, telescope, 1);
		vc.centerX = w / 2.0;
		vc.centerY = h / 2.0;
		vc.poleAngle = orientation;
		vc.field = field;
		vc.orientation = orientation;
		boolean equatorial = (m == MOUNT.EQUATORIAL);
		if (equatorial) {
			vc.centralLongitude = eq.getLongitude();
			vc.centralLatitude = eq.getLatitude();
		} else {
			LocationElement hz = CoordinateSystem.equatorialToHorizontal(eq, time, obs, eph);
			vc.centralLongitude = hz.getLongitude();
			vc.centralLatitude = hz.getLatitude();
		}
		vc.sin_lat0 = (float) Math.sin(vc.centralLatitude);
		vc.cos_lat0 = (float) Math.cos(vc.centralLatitude);

		vc.loc0J2000 = LocationElement.parseRectangularCoordinates(Precession.precessToJ2000(time.astroDate.jd(), eq.getRectangularCoordinates(), eph));
		vc.pixels_per_radian = w / field;
		vc.pixels_per_degree = vc.pixels_per_radian / Constant.RAD_TO_DEG;

		double sx = 1.0f;
		if (m == MOUNT.AZIMUTHAL) sx = -1.0f;
		double stx = sx * (float) Math.PI * 1.3333;

		double stxx = stx / field;
		vc.stxxTimesCenterX = stxx * vc.centerX;
		vc.cos_lat0_times_sy = vc.cos_lat0 * Math.abs(vc.stxxTimesCenterX);
		vc.sin_lat0_times_sy = vc.sin_lat0 * Math.abs(vc.stxxTimesCenterX);

		// Star reading
		if (vc.re_star == null || (vc.lastEq != null && !vc.lastEq.equals(eq))) {
			vc.re_star = new ReadFile();
			vc.re_star.setPath(FileIO.DATA_STARS_SKY2000_DIRECTORY + "JPARSEC_Sky2000.txt");
			vc.re_star.setFormat(ReadFile.FORMAT.JPARSEC_SKY2000);
			vc.readFileOfStars(6.5, vc.re_star, time, obs, eph, w, h, equatorial);
			vc.re_star.pathToFile = FileIO.DATA_STARS_SKY2000_DIRECTORY + "JPARSEC_Sky2000_plus.txt";
			vc.readFileOfStars(9.5, vc.re_star, time, obs, eph, w, h, equatorial);
		}
		vc.lastEq = eq;

		Object[] readStars = vc.re_star.getReadElements();
		int maxStars = readStars.length;

		StringBuffer out = new StringBuffer("");
		String sep = ",";
		int ns = 0;

		if (includePlanets) {
			EphemerisElement eph0 = eph.clone();
			TARGET t[] = new TARGET[] {
					TARGET.MERCURY, TARGET.VENUS, TARGET.MARS, TARGET.JUPITER, TARGET.SATURN, TARGET.URANUS, TARGET.NEPTUNE, TARGET.Pluto
			};
			float pos[] = null;
			for (int i=0; i<t.length; i++) {
				eph0.targetBody = t[i];

				EphemElement ephem = Ephem.getEphemeris(time, obs, eph0, false);
				double d = LocationElement.getApproximateAngularDistance(eq, ephem.getEquatorialLocation());
				if (d < 2 * Constant.DEG_TO_RAD) {
					LocationElement loc2 = Ephem.correctEquatorialCoordinatesForRefraction(time, obs, eph, ephem.getEquatorialLocation());
					if (equatorial) {
						pos = vc.stereographic(loc2, telescope, w, h);
					} else {
						LocationElement ll = CoordinateSystem.equatorialToHorizontal(loc2, time, obs, eph);
						pos = vc.stereographic(ll, telescope, w, h);
					}
					if (pos != null && (pos[0] <= 0 || pos[0] >= (w-1) || pos[1] <= 0 || pos[1] >= (h-1))) pos = null;
					if (pos != null)
						out.append("" + pos[0] + sep + pos[1] + sep + ephem.magnitude + sep + ephem.rightAscension + sep + ephem.declination + sep + "-" + sep + "-" + sep + ephem.name + FileIO.getLineSeparator());

					if (i >= 2) { // Moons
						MoonEphemElement mephem[] = null;
						if (i == 2) mephem = MoonEphem.martianSatellitesEphemerides_2007(time, obs, eph0);
						if (i == 3) mephem = MoonEphem.galileanSatellitesEphemerides_L1(time, obs, eph0);
						if (i == 4) mephem = MoonEphem.saturnianSatellitesEphemerides_TASS17(time, obs, eph0, false);
						if (i == 5) mephem = MoonEphem.uranianSatellitesEphemerides_GUST86(time, obs, eph0);
						if (i == 6) mephem = new MoonEphemElement[] {MoonEphem.calcJPLSatellite(time, obs, eph0, TARGET.Triton.getName())};
						if (i == 7) mephem = new MoonEphemElement[] {MoonEphem.calcJPLSatellite(time, obs, eph0, TARGET.Charon.getName())};
						if (mephem == null) continue;
						for (int j=0; j<mephem.length; j++) {
							if (mephem[j].eclipsed || mephem[j].occulted) continue;
							loc2 = Ephem.correctEquatorialCoordinatesForRefraction(time, obs, eph, mephem[j].getEquatorialLocation());
							if (equatorial) {
								pos = vc.stereographic(loc2, telescope, w, h);
							} else {
								LocationElement ll = CoordinateSystem.equatorialToHorizontal(loc2, time, obs, eph);
								pos = vc.stereographic(ll, telescope, w, h);
							}
							if (pos != null && (pos[0] <= 0 || pos[0] >= (w-1) || pos[1] <= 0 || pos[1] >= (h-1))) pos = null;
							if (pos != null)
								out.append("" + pos[0] + sep + pos[1] + sep + mephem[j].magnitude + sep + mephem[j].rightAscension + sep + mephem[j].declination + sep + "-" + sep + "-" + sep + mephem[j].name + FileIO.getLineSeparator());
						}
					}
				}
			}
		}

		for (int iii = 0; iii < maxStars; iii++)
		{
			if (readStars[iii] != null) {
				StarData sd = (StarData)readStars[iii];
				if (sd.mag0 > maglim) {
					maxStars = iii;
					break;
				}

				float pos[] = vc.stereographic(sd.loc, telescope, w, h);
				if (pos != null && (pos[0] <= 0 || pos[0] >= (w-1) || pos[1] <= 0 || pos[1] >= (h-1))) pos = null;
				if (pos != null) {
					String var = "N";
					if (sd.type.startsWith("V") || sd.type.startsWith("B")) var = "V";
					String name = "-";
					if (sd.sp == null || sd.sp.equals("")) sd.sp = "-";
					//if (sd.type == null || sd.type.equals("")) sd.type = "-";
					//sd.type = DataSet.replaceAll(sd.type, " ", "", true);
					sd.sp = DataSet.replaceAll(sd.sp, " ", "", true);
					if (sd.nom2 != null && !sd.nom2.equals("")) {
						name = sd.nom2;
						if (sd.properName != null && !sd.properName.equals("")) name = sd.properName;
					}
					out.append("" + pos[0] + sep + pos[1] + sep + sd.mag + sep + sd.ra + sep + sd.dec + sep + var + sep + sd.sp + sep + name + FileIO.getLineSeparator());
					ns ++;
					if (ns >= nstars && nstars > 0) break;
				}
			}
		}

		if (maglim >= 10 && (nstars == -1 || ns < nstars)) {
			if (nstars > 0 && nstars > ns && ns > 0) {
				int factor = 1 + (nstars - ns) / (2 * ns);
				double newMaglim = 9.5 + factor;
				if (newMaglim < maglim) maglim = newMaglim;
			}
			eq.setRadius(2062650);
			LocationElement eq2000 = Ephem.toMeanEquatorialJ2000(eq, time, obs, eph);
			ArrayList<String> list = RenderSky.queryUCAC(eq2000, field * Constant.RAD_TO_DEG, maglim, 30, time, obs, eph);
			float pos[];
			for (int i=0; i<list.size(); i++) {
				String s = list.get(i);
				LocationElement loc = new LocationElement(
						Double.parseDouble(FileIO.getField(1, s, " ", true)),
						Double.parseDouble(FileIO.getField(2, s, " ", true)), 1.0);
				LocationElement loc2 = Ephem.correctEquatorialCoordinatesForRefraction(time, obs, eph, loc);
				if (equatorial) {
					pos = vc.stereographic(loc2, telescope, w, h);
				} else {
					LocationElement ll = CoordinateSystem.equatorialToHorizontal(loc2, time, obs, eph);
					pos = vc.stereographic(ll, telescope, w, h);
				}

				if (pos != null && (pos[0] <= 0 || pos[0] >= (w-1) || pos[1] <= 0 || pos[1] >= (h-1))) pos = null;
				if (pos != null) {
					out.append("" + pos[0] + sep + pos[1] + sep + Double.parseDouble(FileIO.getField(3, s, " ", true)) + sep + loc.getLongitude() + sep + loc.getLatitude() + sep + "-" + sep + "-" + sep + "-" + FileIO.getLineSeparator());
					ns ++;
					if (ns >= nstars && nstars > 0) break;
				}
			}
		}

		// 2 or more stars closer than resolution must appear in the catalog as one with combined magnitudes
		String outData[] = DataSet.toStringArray(out.toString(), FileIO.getLineSeparator());
		double xp[] = DataSet.toDoubleValues(DataSet.extractColumnFromTable(outData, sep, 0));
		double yp[] = DataSet.toDoubleValues(DataSet.extractColumnFromTable(outData, sep, 1));
		for (int i=0; i<outData.length; i++) {
			for (int j=i+1; j<outData.length; j++) {
				if (j == i) continue;

				double d = FastMath.hypot(xp[i] - xp[j], yp[i] -yp[j]);
				if (d < 1) {
					double mag1 = Double.parseDouble(FileIO.getField(3, outData[i], sep, false));
					double mag2 = Double.parseDouble(FileIO.getField(3, outData[j], sep, false));
					double mag = Star.combinedMagnitude(mag1, mag2);
					double lr = Star.luminosityRatio(mag2, mag1);
					if (lr < 1E-10) lr = 1.0E-10;
					if (lr > 1E10) lr = 1.0E10;
					lr = Math.pow(2.0, -1.0 / lr);
					double dec1 = Double.parseDouble(FileIO.getField(5, outData[i], sep, false));
					double dec2 = Double.parseDouble(FileIO.getField(5, outData[j], sep, false));
					double dec = dec1 + (dec2 - dec1) * lr;
					double ra1 = Double.parseDouble(FileIO.getField(4, outData[i], sep, false));
					double ra2 = Double.parseDouble(FileIO.getField(4, outData[j], sep, false));
					if (Math.abs(ra2 - ra1) > Math.PI) {
						if (ra2 < ra1) {
							ra2 += Constant.TWO_PI;
						} else {
							ra1 += Constant.TWO_PI;
						}
						if (Math.abs(ra2 - ra1) > Math.PI) {
							System.out.println("*** ERROR *** RA OF COMBINED DOUBLE CANNOT BE CALCULATED. SHOULD NEVER HAPPEN");
							ra2 = ra1;
						}
					}
					double ra = ra1 + (ra2 - ra1) * lr;
					double px = xp[i] + (xp[j] - xp[i]) * lr;
					double py = yp[i] + (yp[j] - yp[i]) * lr;
					String var1 = FileIO.getField(6, outData[i], sep, false);
					String var2 = FileIO.getField(6, outData[j], sep, false);
					String var = var1;
					if (var1.equals("N") && !var2.equals(var1)) var = var2;
					String nom1 = FileIO.getField(8, outData[i], sep, false);
					String nom2 = FileIO.getField(8, outData[j], sep, false);
					String nom = nom1;
					if (!nom2.equals("-")) nom += " + "+nom2;
					String sp = FileIO.getField(7, outData[i], sep, false);
					if (mag2 < mag1) sp = FileIO.getField(7, outData[j], sep, false);

					String newLine = "" + px + sep + py + sep + mag + sep + ra + sep + dec + sep + var + sep + sp + sep + nom;
					//System.out.println("Replacing "+outData[i]+"/"+outData[j]+"  by  "+newLine);
					outData[i] = newLine;
					outData = DataSet.eliminateRowFromTable(outData, j+1);
				}
			}
		}
		return outData;
	}
}

class StarData {
	public LocationElement loc;
	public double ra, dec;
	public float mag, mag0, pos[];
	public String sp, type, nom2, properName;
	public short spi;

	public StarData(LocationElement loc, float mag, String sp, String type) {
		this.loc = loc;
		this.mag = mag;
		this.sp = sp;
		this.type = type;
	}
}
