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

import jparsec.io.image.HeaderElement;
import jparsec.util.Translate;

/**
 * An interface for the commands that should be available to all cameras.
 * Current implementation is focused towards DSLR cameras, although a
 * driver for CCDs could be written as well.
 * @author T. Alonso Albi - OAN (Spain)
 */
public interface GenericCamera {

	/**
	 * The set of camera models supported.
	 */
	public enum CAMERA_MODEL {
		/** Driver for most Canon EOS DSLR cameras. This includes 1000D, 1100D,
		 * 40D, 400D, 450D, 50D, 500D, 550D, 5D Mark II, but not the most recent
		 * ones (60D ...). */
		CANON_EOS_40D_400D_50D_500D_1000D,
		/** Driver for a webcam. */
		WEBCAM,
		/** A virtual camera for testing, emulating the 40D. */
		VIRTUAL_CAMERA,
		/** A virtual camera with a round sensor. */
		VIRTUAL_ROUND_SENSOR;

		/**
		 * Returns if the sensor of this camera is circular or not.
		 * @return True or false.
		 */
		public boolean hasRoundSensor() {
			if (this == VIRTUAL_ROUND_SENSOR) return true;
			return false;
		}

		/**
		 * Returns if this dome is virtual or not.
		 * @return True or false.
		 */
		public boolean isVirtual() {
			if (this.name().startsWith("VIRTUAL_")) return true;
			return false;
		}

		/**
		 * Returns if this camera is a CCD camera, not a DSLR one.
		 * @return True or false.
		 */
		public boolean isCCD() {
			return false;
		}

		/**
		 * Returns if this camera is a webcam one.
		 * @return True or false.
		 */
		public boolean isWebcam() {
			return this.name().startsWith("WEBCAM");
		}

		/**
		 * Returns if this camera is a DLSR one.
		 * @return True or false.
		 */
		public boolean isDLSR() {
			return !isCCD() && !isWebcam();
		}

		/**
		 * Returns a short string describing the camera type.
		 * @return Camera type.
		 */
		public String getType() {
			if (isCCD()) return "CCD";
			if (isWebcam()) return "Webcam";
			if (isDLSR()) return "DLSR";
			return Translate.translate(1187);
		}
	}

	/**
	 * The set of filters supported.
	 */
	public enum FILTER {
		/** ID constant for no filter at all. */
		NO_FILTER("-"),
		/** ID constant for the UV/IR filter of a modified DLSR. */
		FILTER_MODIFIED_DLSR("IR/UV MODIFIED DLSR"),
		/** ID constant for the IR filter used in commercial DSLR cameras/webcams. */
		FILTER_IR_DSLR("IR DLSR/Webcam"),
		/** ID constant for the R filter. */
		FILTER_R("R"),
		/** ID constant for the G filter. */
		FILTER_G("G"),
		/** ID constant for the B filter. */
		FILTER_B("B"),
		/** ID constant for the Halpha filter. */
		FILTER_Halpha("H alpha"),
		/** ID constant for the O III filter. */
		FILTER_OIII("O III"),
		/** ID constant for a generic filter with color 1. */
		FILTER_COLOR1("color 1"),
		/** ID constant for a generic filter with color 2. */
		FILTER_COLOR2("color 2"),
		/** ID constant for a generic filter with color 3. */
		FILTER_COLOR3("color 3");

		private String name;
		private FILTER(String name) {
			this.name = name;
		}

		/**
		 * Returns the name of the filter.
		 * @return Filter name.
		 */
		public String getFilterName() {
			return name;
		}

		/**
		 * Sets the name of the filter for those filters named
		 * 'FILTER_COLORx'. For the others the input name is
		 * ignored.
		 * @param name The filter name.
		 */
		public void setFilterName(String name) {
			if (this.name.startsWith("FILTER_COLOR")) this.name = name;
		}

		/**
		 * Returns the names of all the filters available.
		 * @return Filter names.
		 */
		public static String[] getFilterNames() {
			FILTER f[] = FILTER.values();
			String out[] = new String[f.length];
			for (int i=0; i<f.length; i++) {
				out[i] = f[i].getFilterName();
			}
			return out;
		}
	}

	/**
	 * The set of image types supported.
	 */
	public enum IMAGE_ID {
		/** ID constant for a dark frame. */
		DARK,
		/** ID constant for a flat frame. */
		FLAT,
		/** ID constant for an on source image. */
		ON_SOURCE,
		/** ID constant for a test frame. */
		TEST,
		/** ID constant for a reduced dark frame. */
		REDUCED_DARK,
		/** ID constant for a reduced flat frame. */
		REDUCED_FLAT,
		/** ID constant for a reduced on frame. */
		REDUCED_ON,
		/** ID constant for an stacked frame. */
		STACKED,
		/** ID constant for an averaged frame. */
		AVERAGED;
	}

	/** The set of possible (selectable) image IDs as names. */
	public static final String IMAGE_IDS[] = new String[] {
		"Dark", "Flat", "On", "Test", "Dark", "Flat", "On"
	};

	/** The set of possible (all) image IDs as names. */
	public static final String IMAGE_IDS_ALL[] = new String[] {
		"Dark", "Flat", "On", "Test", "Reduced dark", "Reduced flat", "Reduced on", "Stacked", "Averaged"
	};

	/**
	 * Sets the ISO.
	 * @param iso ISO value.
	 * @return True if the command success.
	 */
	public boolean setISO(String iso);
	/**
	 * Returns the ISO value.
	 * @return ISO value.
	 */
	public String getISO();
	/**
	 * Sets the exposition time.
	 * @param time Time, in seconds.
	 * @return True if the command success.
	 */
	public boolean setExpositionTime(String time);
	/**
	 * Returns the exposition time.
	 * @return Exposition time value, seconds.
	 */
	public String getExpositionTime();
	/**
	 * Sets the resolution mode.
	 * @param mode The mode.
	 * @return True if the command success.
	 */
	public boolean setResolutionMode(String mode);
	/**
	 * Returns the resolution mode.
	 * @return Resolution mode value.
	 */
	public String getResolutionMode();
	/**
	 * Sets the aperture.
	 * @param aperture The aperture.
	 * @return True if the command success.
	 */
	public boolean setAperture(String aperture);
	/**
	 * Returns the aperture value.
	 * @return Aperture value.
	 */
	public String getAperture();
	/**
	 * Takes a shot from the camera and downloads it.
	 * @param keepInCamera True to keep a copy in the camera, false to
	 * remove it after download.
	 * @return True if the command success.
	 */
	public boolean shotAndDownload(boolean keepInCamera);
	/**
	 * Returns true if the camera is shooting now.
	 * @return True or false.
	 */
	public boolean isShooting();
	/**
	 * Sets the camera orientation respect the mount of the telescope.
	 * In case the telescope is in a equatorial mount, 0 means the
	 * camera/image 'up' direction is north (or south if the telescope
	 * inverts the image). This is used for the reduction pipeline later,
	 * the value set here should be as approximate as possible.
	 * @param ang Angle in radians, positive for clockwise rotation of
	 * the camera.
	 * @return True if the command success.
	 */
	public boolean setCameraOrientation(double ang);
	/**
	 * Returns the camera orientation.
	 * @return Angle in radians.
	 */
	public double getCameraOrientation();
	/**
	 * Sets the image identifier for the next frame.
	 * @param img Frame type.
	 * @return True if the command success.
	 */
	public boolean setImageID(IMAGE_ID img);
	/**
	 * Returns the image identifier.
	 * @return Frame identifier.
	 */
	public IMAGE_ID getImageID();
	/**
	 * Returns the path of the last image downloaded. Specific implementation
	 * of this method could return the path of several files, in case more than
	 * one is created (jpg and cr2 in case of Canon cameras with JPG + RAW output
	 * mode). In this case all paths should be separated by comma.
	 * @return The path/s. Null in case of error when shooting last photo.
	 */
	public String getPathOfLastDownloadedImage();
	/**
	 * Sets the download directory for the images.
	 * @param path The path to the directory.
	 * @return True if the command success.
	 */
	public boolean setDownloadDirectory(String path);
	/**
	 * Sets the filter to use for the next frame. This method
	 * is intended to support filter wheels, although current
	 * implementation does not support it. It can be used to
	 * set if a DSLR camera has its default IR filter or not.
	 * @param filter The new filter.
	 * @return True if the command success.
	 */
	public boolean setFilter(FILTER filter);
	/**
	 * Returns the filter currently in use.
	 * @return The filter.
	 */
	public FILTER getFilter();
	/**
	 * Returns all possible ISO values.
	 * @return ISO values.
	 */
	public String[] getPossibleISOs();
	/**
	 * Returns all possible resolution modes.
	 * @return Resolution modes.
	 */
	public String[] getPossibleResolutionModes();
	/**
	 * Returns all possible exposition times.
	 * @return Exposition times.
	 */
	public String[] getPossibleExpositionTimes();
	/**
	 * Returns all possible aperture values.
	 * @return Aperture values.
	 */
	public String[] getPossibleApertures();
	/**
	 * Returns the camera model.
	 * @return Camera model.
	 */
	public CAMERA_MODEL getCameraModel();
	/**
	 * Sets the exposition time in seconds in bulb mode for
	 * DLSR cameras, or for CCD cameras.
	 * @param seconds Time in seconds.
	 */
	public void setCCDorBulbModeTime(int seconds);
	/**
	 * Returns the exposition time in seconds for bulb or CCD.
	 * @return Time in seconds.
	 */
	public int getCCDorBulbModeTime();
	/**
	 * Returns the name of this camera.
	 * @return The name.
	 */
	public String getCameraName();
	/**
	 * Sets the name of the camera.
	 * @param name The new camera name.
	 */
	public void setCameraName(String name);
	/**
	 * Returns the ratio width/height from native camera resolution.
	 * @return The width/height ratio.
	 */
	public double getWidthHeightRatio();
	/**
	 * Returns the inverse electronic gain in e-/ADU, for the
	 * selected ISO.
	 * @return Inverse electronic gain, or 0 in case it is unknown.
	 */
	public double getInverseElectronicGain();
	/**
	 * Returns the saturation level in ADUs.
	 * @return Saturation level, or 0 if it is unknown.
	 */
	public double getSaturationLevelADUs();
	/**
	 * Returns image depth in bits. 8 bits means a maximum
	 * intensity level of 255 in a pixel/color, usual in webcams.
	 * 14 is the usual value in modern DSLRs.
	 * @return Bits per pixel.
	 */
	public int getDepth();
	/**
	 * Returns a set of entries to construct the fits header for the
	 * last shot. The set of entries are quite basic and only contains
	 * those related to the properties of the shot, and not for the
	 * telescope or other hardware. Note typical value of width/height
	 * is 0 since the size of the image is not known until it is
	 * downloaded, so these values should be set later.
	 * The entries contains the following keys:
	 * <pre>
	 * Entry    Value (typical)   Description
	 * --------------------------------------
	 *
	 * BITPIX      32             Bits per data value
	 * NAXIS        2             Dimensionality
	 * NAXIS1       0             Width
	 * NAXIS2       0             Height
	 * EXTEND       T             Extension permitted
	 * MAXCOUNT   255             Max counts per pixel
	 * ISO        100             ISO
	 * TIME         1             Exposure time in s
	 * MODE    jpg...             Resolution mode
	 * RAW      false             True for raw mode
	 * ANGLE        0             Camera orientation angle
	 * INSTRUME  name             Camera name
	 * CAM_MODE  VIRTUAL          Camera model (driver)
	 * APERTURE   2.8             Aperture f/
	 * DEPTH       14             Pixel depth in bits
	 * GAIN       2.8             Gain e-/ADU
	 * BULBTIME    60             Exposure time in bulb mode (s)
	 * MAXADU    4000             Saturation level in ADUs
	 * FILTER      IR             Filter name
	 * BAYER     RGBG             Bayer matrix, top-left and clockwise
	 * IMGID     Dark             Image id: Dark, Flat, On, Test, or Reduced
	 * </pre>
	 * @return The basic header to later create the fits file.
	 */
	public HeaderElement[] getFitsHeaderOfLastImage();
	/**
	 * Returns true if the bulb position is set for this camera.
	 * @return True or false.
	 */
	public boolean isBulb();
	/**
	 * Sets the minimum interval between shots for this camera.
	 * Default value is 0 so that consecutive shots will have
	 * minimum possible lag. Set to 30 or 60 seconds to allow
	 * the camera to get 'cooled'.
	 * @param seconds Time in seconds.
	 */
	public void setMinimumIntervalBetweenShots(int seconds);
	/**
	 * Returns the minimum interval of time allowed between shots.
	 * Default is 0.
	 * @return Time in seconds.
	 */
	public int getMinimumIntervalBetweenShots();
	/**
	 * Get when the last shot started, ms since 1970.0 UTC.
	 * @return Last shot start time.
	 */
	public long getLastShotStartTime();
	/**
	 * Get when the last shot ended, ms since 1970.0 UTC.
	 * @return Last shot end time.
	 */
	public long getLastShotEndTime();
}
