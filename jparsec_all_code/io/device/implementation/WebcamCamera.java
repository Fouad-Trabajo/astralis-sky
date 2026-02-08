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
package jparsec.io.device.implementation;

import java.io.File;

import jparsec.graph.DataSet;
import jparsec.io.ApplicationLauncher;
import jparsec.io.FileIO;
import jparsec.io.device.GenericCamera;
import jparsec.io.image.HeaderElement;
import jparsec.math.Constant;
import jparsec.math.FastMath;
import jparsec.time.AstroDate;
import jparsec.util.JPARSECException;

/**
 * An implementation of the cameras interface for a webcam. This requires
 * mplayer to run.
 * @author T. Alonso Albi - OAN (Spain)
 */
public class WebcamCamera implements GenericCamera {

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
		WebcamShotThread st = new WebcamShotThread();
		st.start();
		return true;
	}

	@Override
	public boolean isShooting() {
		return shooting;
	}

	@Override
	public boolean isBulb() {
		return false;
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
		return new String[] {};
	}

	@Override
	public String[] getPossibleResolutionModes() {
		return new String[] {};
	}

	@Override
	public String[] getPossibleExpositionTimes() {
		return new String[] {};
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
		return 1.333;
	}

	@Override
	public double getInverseElectronicGain() {
		return 0;
	}

	@Override
	public double getSaturationLevelADUs() {
		return 0;
	}

	@Override
	public int getDepth() {
		return 8;
	}

	@Override
	public HeaderElement[] getFitsHeaderOfLastImage() {
		int max = FastMath.multiplyBy2ToTheX(1, getDepth()) - 1;
		boolean raw = true;
		String li = getPathOfLastDownloadedImage();
		if (li != null && (li.endsWith(".jpg") || li.endsWith(".png"))) raw = false;
		
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
				new HeaderElement("NAXIS1", "0", "Width"),
				new HeaderElement("NAXIS2", "0", "Height"),
				new HeaderElement("EXTEND", "T", "Extension permitted"),
				new HeaderElement("MAXCOUNT", ""+max, "Max counts per pixel"),
				new HeaderElement("ISO", getISO(), "ISO"),
				new HeaderElement("TIME", getExpositionTime(), "Exposure time in s"),
				new HeaderElement("TSTART", ""+jd0, "Shot start time as JD UTC"),
				new HeaderElement("TEND", ""+jd1, "Shot end time as JD UTC"),
				new HeaderElement("TEFF", ""+jde, "Shot effective time as JD UTC"),
				new HeaderElement("MODE", getResolutionMode(), "Resolution mode"),
				new HeaderElement("RAW", (""+raw), "True for raw mode"),
				new HeaderElement("ANGLE", ""+getCameraOrientation(), "Camera orientation angle (radians)"),
				new HeaderElement("CAMERA", getCameraName(), "Camera name"),
				new HeaderElement("CAM_MODEL", this.model.name(), "Camera model (driver)"),
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

	private double orientation = 0;
	private IMAGE_ID id = IMAGE_ID.TEST;
	private String lastImage = null;
	private FILTER filter = FILTER.FILTER_IR_DSLR;
	private int bulbTime = 1;
	private String path = "", device = "", camName;
	private boolean shooting = false;
	private String iso = "100", aperture = "", texp = "1", res = "";
	private int minInterval = 0;
	private long lastShotStarted = 0, lastShotEnded = 0;

	// Number of images to skip before starting, since some webcams require some time
	// to give an output with correct brightness
	private static final int SKIP_SHOTS = 10;

	/**
	 * The constructor for a webcam.
	 * @param model The camera model.
	 * @param device The device to open, for instance /dev/video0 on Linux,
	 * which is the default value in case input device is set to null.
	 */
	public WebcamCamera(CAMERA_MODEL model, String device) {
		this.model = model;
		this.device = device;
		if (device == null) this.device = "/dev/video0";
		path = FileIO.getTemporalDirectory();
		
		camName = "Webcam ("+device+")";
	}

	class WebcamShotThread extends Thread {
		public boolean shouldStop = false;
		public WebcamShotThread() {}
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
				exc.printStackTrace();
				System.out.println("ERROR USING THE VIRTUAL CAMERA !");
			}
			lastShotEnded = System.currentTimeMillis();
			shouldStop = true;
			shooting = false;
		}

		/** Returns if the thread is working or not. */
		public boolean isWorking() {
			return !shouldStop;
		}

		private void createImage() throws JPARSECException {
			String command = "mplayer -vo png -frames "+(bulbTime+SKIP_SHOTS)+" -nosound tv:// -tv driver=v4l2:device="+device;
			Process pr = ApplicationLauncher.executeCommand(command, null, new File(path));
			try {
				pr.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			StringBuffer out = new StringBuffer("");
			for (int i = 1; i <= bulbTime; i++) {
				String img = ""+(i+SKIP_SHOTS);
				out.append(path + DataSet.repeatString("0", 8-img.length()) + img + ".png");
				if (i < bulbTime) out.append(",");
			}
			lastImage = out.toString();
		}
	}
}
