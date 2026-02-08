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

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;

import javax.swing.JOptionPane;

import jparsec.io.*;
import jparsec.util.*;
import jparsec.graph.*;

/**
 * A class to take control of digital cameras using gphoto2 library. This class has been tested to
 * work for gphoto2 version &gt;= 2.4.0, and Canon EOS 300D / 40D. Other cameras (400D, 450D, 500D, but
 * not 60D/600D) should work, as well as with any version of gphoto. Camera should be operated in
 * the 'M' (manual) mode.<P>
 * Possible issues found:<P>
 * 40D (all): Disable the automatic sleep/standby mode of the camera in the menu. After this mode is active
 * the camera will only wake up manually.<P>
 * Use manual focusing in case of shooting with lens. Automatic will work, but could freeze gphoto in
 * case the objective is not opened.<P>
 * 300D: The resolution parameter may not work, but can be left in the desired position manually.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class GPhotoCamera {

	/**
	 * Holds the gphoto version running. Current supported version is 2.4.0 - 2.5.8.
	 * Checks for later version are necessary since the gphoto command line could change.
	 */
	public static String gphotoVersion = ""; // 2.4.0 or later

	/** The set of possible configuration parameters. */
	public enum CAMERA_PARAMETER {
		/** The ISO. */
		ISO,
		/** The shutter speed. */
		SHUTTER_SPEED,
		/** The aperture. */
		APERTURE,
		/** The resolution or image mode. */
		RESOLUTION,
		/** Where to save captures. */
		CAPTURE_TARGET,
		/** Manual focus. */
		FOCUS,
		/** To activate/deactivate the electronic viewfinder. In practice,
		 * for Canon EOS this will lock the mirror up/down. */
		VIEWFINDER,
		/** The zoom function during the live view mode. */
		ZOOM,
		/** The position of the zoom during live view mode. */
		ZOOM_POSITION,
		/** Read only property to set bulb mode. */
		BULB (true),
		/** Read only property to return focal length. */
		FOCAL_LENGTH (true),
		/** Read only property to return battery level. */
		BATTERY_LEVEL (true),
		/** Quality (JPEG, RAW) in Nikons is separated from resolution. */
		NIKON_QUALITY;

		private boolean readOnly = false;
		private Boolean isRange = null;
		private float maxRange, minRange, stepRange;

		/**
		 * Returns if this property is read only or not.
		 * @return True or false.
		 */
		public boolean isReadOnly() {
			return readOnly;
		}

		/**
		 * Returns if this property is of type range.
		 * @return Null if unknown, otherwise true or false.
		 */
		public Boolean isRange() {
			return isRange;
		}

		/**
		 * Returns the minimum value of this property if it is of type range.
		 * @return Minimum possible value.
		 */
		public float minimum() {
			return minRange;
		}

		/**
		 * Returns the maximum value of this property if it is of type range.
		 * @return Maximum possible value.
		 */
		public float maximum() {
			return maxRange;
		}

		/**
		 * Returns the step value of this property if it is of type range.
		 * @return Step value.
		 */
		public float step() {
			return stepRange;
		}

		private CAMERA_PARAMETER() { readOnly = false; };
		private CAMERA_PARAMETER(boolean readOnly) { this.readOnly = readOnly; };
	}

	/** The set of possible cameras. There are two main
	 * implementations: the Canon EOS 40D model, and the
	 * 'custom' model than can be used to support other
	 * models. For this aim, some values like image
	 * resolution or possible zoom values should be set.  */
	public enum CAMERA_ID {
		EOS40D,
		CUSTOM;

		private String configParams[] = new String[] {
			"iso",
			"shutterspeed",
			"aperture,f-number",
			"resolution,imageformat,imagesize", // resolution used in old versions of gphoto2/old EOS cameras
			"capturetarget",
			"manualfocusdrive",
			"eosviewfinder,viewfinder",
			"eoszoom",
			"eoszoomposition",
			"bulb",
			"focallength",
			"batterylevel",
			"imagequality"
		};

		/** The resolution of the images in x axis. */
		public int resolutionX = 3888;
		/** The resolution of the images in y axis. */
		public int resolutionY = 2596;

		/** The resolution of the live view images in x axis. */
		public int liveResolutionX = 800;
		/** The resolution of the live view images in y axis. */
		public int liveResolutionY = 800;

		/** The value to pass to the viewfinder parameter to hold the mirror up. */
		public String viewfinderUp = "1";
		/** The value to pass to the viewfinder parameter to hold the mirror down. */
		public String viewfinderDown = "0";
		/** The value to pass to the bulb parameter to activate it. */
		public String bulbModeEnabled = "1";
		/** The value to pass to the bulb parameter to deactivate it. */
		public String bulbModeDisabled = "0";
		/** The possible zoom values, hardcoded here since --get-config cannot provide these values.
		 * Set to null in case gphoto provides the values. */
		public String zoomValues[] = new String[] {"1", "5"};
		/** Bits per pixel. */
		public int bpp = 14;
		
		/**
		 * Sets the configuration values for a specific camera model.
		 * This method has no effect for the 40D.
		 * @param c The new configuration values.<P>
		 * These are the set of configuration parameters to be checked with gphoto2, like
		 * iso, shutter speed, and so on. To check several values for a given property
		 * set those values separated by ,. An example is index 3, which for the 40D is
		 * set to "resolution,imageformat",
		 * since currently gphoto uses imageformat, but in previous versions (or maybe old cameras
		 * like the 300D) it was resolution. Index 0 corresponds to index 0 of the
		 * enum {@linkplain CAMERA_PARAMETER} (ISO), and so on.
		 */
		public void setConfigurationValues(String c[]) {
			if (this == CUSTOM) {
				configParams = c;
			}
		}
		/**
		 * Returns the configuration parameters for this camera model.
		 * @return The list of parameters.
		 */
		public String[] getConfigurationValues() {
			return configParams.clone();
		}
	};


	/**
	 * Holds if a copy of the images taken should be maintained in the camera. False (No)
	 * is the default recommended value.
	 */
	private boolean maintainCopyInCamera = false;
	/**
	 * Holds the working directory to download the photos to.
	 */
	private String workingDir = "";
	/** The camera model. */
	private CAMERA_ID id;
	/** The possible configuration values for the different parameters and the specific camera. */
	private String[] DEFAULT_CONFIGS;
	/**
	 * Holds the bayer matrix of this camera for raw images, from top-left corner
	 * clockwise.
	 */
	private String BayerMatrix = "RGBG";
	/** Camera model. */
	private String model;
	/** Camera port. */
	private String port;
	/** Holds when the last shot was taken. */
	private long lastShotTime = -1;
	/** Holds the path to the latest image. */
	private String lastShotPath;
	/** Selects exposition time in bulb mode in seconds. */
	private int bulbTime = 60;
	/** If a shot is being taken or not. */
	private boolean isShooting = false;
	/** True to use mirror lock up when shooting. */
	private boolean mirrorLockUp = false;
	/** True to write gphoto2 commands to console. */
	private boolean debug = false;
	/** Detected configuration parameters. */
	private String cameraConfigs[];
	/** Values of the parameters. */
	private String cameraConfigsValues[];
	/** List of all possible values for the parameters. */
	private String cameraPossibleConfigsValues[][];
	/** True or false for the availability of the different configuration parameters. */
	private boolean configParamFound[];
	/** Shot start and end times. */
	private long shotStarted, shotEnded;

	/**
	 * Constructor for a camera with a Bayer matrix RGBG.
	 * @param id The camera identifier to adjust the internal commands for gphoto to operate with it.
	 * @param name The name of the camera, or null to take the first available one.
	 * @param workingDir The working directory to download images to.
	 * @param copyInCamera True to maintain a copy of the image in the camera. False
	 * is strongly recommended.
	 * @throws JPARSECException If an error occurs.
	 */
	public GPhotoCamera(CAMERA_ID id, String name, String workingDir, boolean copyInCamera)
	throws JPARSECException {
		this.id = id;
		if (id == null) this.id = CAMERA_ID.EOS40D;
		DEFAULT_CONFIGS = this.id.configParams;
		cameraConfigs = new String[DEFAULT_CONFIGS.length];
		cameraConfigsValues = new String[DEFAULT_CONFIGS.length];
		cameraPossibleConfigsValues = new String[DEFAULT_CONFIGS.length][];
		for (int i=0; i<DEFAULT_CONFIGS.length; i++) {
			cameraConfigs[i] = "";
			cameraConfigsValues[i] = "0";
		}
		if (!workingDir.endsWith(FileIO.getFileSeparator())) workingDir += FileIO.getFileSeparator();

		String cameras[] = GPhotoCamera.autoDetect();
		boolean ok = false;
		for (int i=0; i<cameras.length; i++)
		{
			int usb = cameras[i].indexOf("usb:");
			String model = cameras[i].substring(0, usb).trim();
			String port = cameras[i].substring(usb);
			if (name == null || name.equals("") || cameras[i].indexOf(name)>=0) {
				this.model = model;
				this.port = port;
				ok = this.checkConfig(true);
				if (ok) break;
			}
		}
		if (!ok) throw new JPARSECException("The camera cannot be found or initialized properly. \nPlease check you have gphoto2 installed, and it is in manual (M) mode. \nTurn it off/on and check again.");
		maintainCopyInCamera = copyInCamera;
		this.workingDir = workingDir;
	}

	/**
	 * Constructor for a camera with a Bayer matrix RGBG.
	 * @param id The camera identifier to adjust the internal commands for gphoto to operate with it.
	 * @param name The name of the camera, or null to take the first available one.
	 * @param workingDir The working directory to download images to.
	 * @param copyInCamera True to maintain a copy of the image in the camera. False
	 * is strongly recommended.
	 * @param debug True to write gphoto2 commands to the console.
	 * @throws JPARSECException If an error occurs.
	 */
	public GPhotoCamera(CAMERA_ID id, String name, String workingDir, boolean copyInCamera, boolean debug)
	throws JPARSECException {
		this.debug = debug;
		this.id = id;
		if (id == null) this.id = CAMERA_ID.EOS40D;
		DEFAULT_CONFIGS = this.id.configParams;
		cameraConfigs = new String[DEFAULT_CONFIGS.length];
		cameraConfigsValues = new String[DEFAULT_CONFIGS.length];
		cameraPossibleConfigsValues = new String[DEFAULT_CONFIGS.length][];
		for (int i=0; i<DEFAULT_CONFIGS.length; i++) {
			cameraConfigs[i] = "";
			cameraConfigsValues[i] = "0";
		}
		if (!workingDir.endsWith(FileIO.getFileSeparator())) workingDir += FileIO.getFileSeparator();

		String cameras[] = GPhotoCamera.autoDetect(debug);
		boolean ok = false;
		for (int i=0; i<cameras.length; i++)
		{
			int usb = cameras[i].indexOf("usb:");
			String model = cameras[i].substring(0, usb).trim();
			String port = cameras[i].substring(usb);
			if (name == null || name.equals("") || cameras[i].indexOf(name)>=0) {
				this.model = model;
				this.port = port;
				ok = this.checkConfig(true);
				if (ok) break;
			}
		}
		if (!ok) throw new JPARSECException("The camera cannot be found or initialized properly. \nPlease check you have gphoto2 installed, and it is in manual (M) mode. \nTurn it off/on and check again.");
		maintainCopyInCamera = copyInCamera;
		this.workingDir = workingDir;
	}
	
	/**
	 * Constructor for a camera in a given port.
	 * @param id The camera identifier to adjust the internal commands for gphoto to operate with it.
	 * @param name The name of the camera, or null to take the first available one.
	 * @param port The port of the camera, to instantiate two identical cameras in
	 * different ports.
	 * @param workingDir The working directory to download images to.
	 * @param copyInCamera True to maintain a copy of the image in the camera. False
	 * is strongly recommended.
	 * @param bayer Bayer matrix for this camera. Default is "RGBG", set to null to use
	 * this default value.
	 * @throws JPARSECException If an error occurs.
	 */
	public GPhotoCamera(CAMERA_ID id, String name, String port, String workingDir, boolean copyInCamera, String bayer)
	throws JPARSECException {
		this.id = id;
		if (id == null) this.id = CAMERA_ID.EOS40D;
		DEFAULT_CONFIGS = this.id.configParams;
		cameraConfigs = new String[DEFAULT_CONFIGS.length];
		cameraConfigsValues = new String[DEFAULT_CONFIGS.length];
		cameraPossibleConfigsValues = new String[DEFAULT_CONFIGS.length][];
		for (int i=0; i<DEFAULT_CONFIGS.length; i++) {
			cameraConfigs[i] = "";
			cameraConfigsValues[i] = "0";
		}
		if (!workingDir.endsWith(FileIO.getFileSeparator())) workingDir += FileIO.getFileSeparator();

		String cameras[] = GPhotoCamera.autoDetect();
		boolean ok = false;
		for (int i=0; i<cameras.length; i++)
		{
			int usb = cameras[i].indexOf("usb:");
			String model = cameras[i].substring(0, usb).trim();
			String cport = cameras[i].substring(usb);
			boolean portOK = false, cameraOK = false;
			if (port == null || port.equals("")) {
				portOK = true;
			} else {
				if (port.indexOf(cport) >= 0) portOK = true;
			}
			if (name == null || name.equals("")) {
				cameraOK = true;
			} else {
				if (name.indexOf(model) >= 0) cameraOK = true;
			}
			if (portOK && cameraOK) {
				this.model = model;
				this.port = cport;
				ok = checkConfig(true);
				if (ok) break;
			}
		}
		if (!ok) throw new JPARSECException("The camera cannot be found or initialized properly. \nPlease check you have gphoto2 installed, and it is in manual (M) mode. \nTurn it off/on and check again.");
		maintainCopyInCamera = copyInCamera;
		this.workingDir = workingDir;
		if (bayer != null) this.BayerMatrix = bayer;
	}

	/**
	 * Returns the id for the camera controlled with this instance.
	 * @return Camera id.
	 */
	public CAMERA_ID getId() {
		return id;
	}

	private static void getVersion(boolean debug)
	throws JPARSECException {
		Process p = ApplicationLauncher.executeCommand("gphoto2 --version");
		if (debug) System.out.println("Executing command: gphoto2 --version");
		String out = ApplicationLauncher.getConsoleOutputFromProcess(p);
		String array[] = DataSet.toStringArray(out, FileIO.getLineSeparator());
		gphotoVersion = FileIO.getField(2, array[0], " ", true);
		int index = DataSet.getIndexStartingWith(array, "libgphoto2");
		if (index > 0) gphotoVersion += " (libgphoto2 "+FileIO.getField(2, array[index], " ", true)+")";
	}

	/**
	 * Take a shot and download it.
	 * @throws JPARSECException If an error occurs.
	 * @return Name of downloaded image. In case there are 2 (jpg + raw),
	 * the path to both files are returned, separated by ,.
	 */
	public String shotAndRetrieveImage()
	throws JPARSECException {
		ShotThread st = new ShotThread();
		st.start();

		do {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while(isShooting);

		shotStarted = st.shotStarted;
		shotEnded = st.shotEnded;
		return lastShotPath;
	}

	/**
	 * Take n shots and download them.
	 * @param frames Number of shots to take.
	 * @throws JPARSECException If an error occurs.
	 * @return Name of downloaded image. The path to the n files
	 * is returned, separated by ,.
	 */
	public String shotAndRetrieveImage(int frames)
	throws JPARSECException {
		ShotThread st = new ShotThread(frames);
		st.start();

		do {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while(isShooting);

		shotStarted = st.shotStarted;
		shotEnded = st.shotEnded;
		return lastShotPath;
	}

	/**
	 * Returns the time the last shot started, ms since 1970.0 UTC.
	 * @return Last shot start time.
	 */
	public long getLastShotStartTime() { return shotStarted; }
	/**
	 * Returns the time the last shot ended, ms since 1970.0 UTC.
	 * @return Last shot end time.
	 */
	public long getLastShotEndTime() { return shotEnded; }
	
	/**
	 * Returns the path to the last shot/s.
	 * @return Last shot path.
	 */
	public String getLastShotPath() {
		return lastShotPath;
	}

	/**
	 * Sets the lath to the last shot/s. This method should 
	 * not be used unless you immediately change the output 
	 * name of the last taken shot externally.
	 * @param path Last shot path.
	 */
	public void setLastShotPath(String path) {
		lastShotPath = path;
	}

	/**
	 * Auto detect current connected cameras.
	 * @return The list of cameras and ports available.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String[] autoDetect() throws JPARSECException {
		return autoDetect(false);
	}
	
	/**
	 * Auto detect current connected cameras.
	 * @param debug True to write gphoto2 commands to console.
	 * @return The list of cameras and ports available.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String[] autoDetect(boolean debug)
	throws JPARSECException {
		if (gphotoVersion.equals("")) getVersion(debug);
		Process p = ApplicationLauncher.executeCommand("gphoto2 --auto-detect");
		if (debug) System.out.println("Executing command: gphoto2 --auto-detect");
		String out = ApplicationLauncher.getConsoleOutputFromProcess(p);
		String array[] = DataSet.toStringArray(out, FileIO.getLineSeparator());
		String cameras = "";
		for (int i=2; i<array.length; i++)
		{
			int usb = array[i].indexOf("usb:");
			String after = array[i].substring(usb+4).trim();
			if (!after.equals("")) {
				if (!cameras.equals("")) cameras += FileIO.getLineSeparator();
				cameras += array[i];
			}
		}
		return DataSet.toStringArray(cameras, FileIO.getLineSeparator());
	}

	private long lastCheck = -1;
	
	/**
	 * Checks the camera by retrieving the list of possible configuration parameters
	 * and checking that list against the configuration parameters that should be available
	 * for configuration to do astronomical imaging.
	 * @param checkCameraStatus True to check camera status (if it is on), false to assume 
	 * it is on and only check for possible new configuration values (apertures in case a new 
	 * lens was attached without switching off the camera, and so on). Note some cameras seem 
	 * to get blocked in case true is used here too often.
	 * @return True if everything is fine, and the basics parameters to do imaging (iso and 
	 * shutter) are available. False otherwise. In case the check camera flag is set to false, true 
	 * is always returned.
	 * @throws JPARSECException If an error occurs.
	 */
	public boolean checkConfig(boolean checkCameraStatus)
	throws JPARSECException {
		boolean all = checkCameraStatus;
		if (isShooting || liveView) {
			lastCheck = System.currentTimeMillis();
			all = false;
		}
		if (lastCheck > 0) {
			double elapsed = (System.currentTimeMillis()-lastCheck)*0.001;
			if (elapsed < 5) all = false;
		}
		lastCheck = System.currentTimeMillis();
		boolean autoDetectionOfParametersSuccesful = true;

		if (all) {
			String command = "gphoto2 --camera \""+this.model+"\" --port "+this.port+" --list-config";
			Process p = ApplicationLauncher.executeCommand(command);
			if (debug) System.out.println("Executing command: "+command);
			try {
				p.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			String out = ApplicationLauncher.getConsoleOutputFromProcess(p);
			String array[] = DataSet.toStringArray(out, FileIO.getLineSeparator());
	
			CAMERA_PARAMETER configParams[] = CAMERA_PARAMETER.values();
			configParamFound = new boolean[configParams.length];
			for (int i=0; i<array.length; i++)
			{
				for (int j=0; j<configParamFound.length; j++) {
					if (configParamFound[j]) continue;
					configParamFound[j] = endsWith(array[i], configParams[j]);
					if (configParamFound[j]) {
						break;
					}
				}
			}
			autoDetectionOfParametersSuccesful = false;
			// ISO and shutter speed must be available. aperture is not required so that
			// direct focus photography is possible
			if (configParamFound[CAMERA_PARAMETER.ISO.ordinal()] && configParamFound[CAMERA_PARAMETER.SHUTTER_SPEED.ordinal()])
				autoDetectionOfParametersSuccesful = true;
		}
		
		CAMERA_PARAMETER configParams[] = CAMERA_PARAMETER.values();
		for (int i=0; i<configParams.length; i++)
		{
			String values[] = getConfig(configParams[i]);
			cameraPossibleConfigsValues[i] = values;
		}
		
		return autoDetectionOfParametersSuccesful;
	}

	private boolean endsWith(String line, CAMERA_PARAMETER c) {
		String values = DEFAULT_CONFIGS[c.ordinal()];
		String val[] = DataSet.toStringArray(values, ",");
		for (int i=0; i<val.length; i++) {
			if (line.endsWith("/"+val[i])) {
				cameraConfigs[c.ordinal()] = line;
				return true;
			}
		}
		return false;
	}

	/**
	 * Lists all configuration parameters. These parameters are automatically
	 * identified to detect which of them correspond to iso, shutter speed, image
	 * quality, and so on. However, this identification could fail since the name of
	 * the parameters are different from camera to camera. This method allows
	 * to obtain the full list as a previous step to modify {@link CAMERA_ID#configParams}
	 * by hand for a given camera.
	 * @return The list of configuration parameters.
	 * @throws JPARSECException If an error occurs.
	 */
	public String[] listAllConfig()
	throws JPARSECException {
		String command = "gphoto2 --camera \""+this.model+"\" --port "+this.port+" --list-config";
		Process p = ApplicationLauncher.executeCommand(command);
		if (debug) System.out.println("Executing command: "+command);
		String out = ApplicationLauncher.getConsoleOutputFromProcess(p);
		return DataSet.toStringArray(out, FileIO.getLineSeparator(), false);
	}

	/**
	 * Resets the USB for the current camera. According to gphoto
	 * it simulates plugging out and in the camera when the protocol locks.
	 * @return The gphoto output from the reset command.
	 * @throws JPARSECException If an error occurs.
	 */
	public String[] reset() throws JPARSECException {
		String command = "gphoto2 --camera \""+this.model+"\" --port "+this.port+" --reset";
		Process p = ApplicationLauncher.executeCommand(command);
		if (debug) System.out.println("Executing command: "+command);
		String out = ApplicationLauncher.getConsoleOutputFromProcess(p);
		return DataSet.toStringArray(out, FileIO.getLineSeparator(), false);
	}

	/**
	 * Returns the possible values of a given configuration parameter.
	 * @param config The configuration parameter.
	 * @return List of possible values, or null in case live view is active.
	 * The index of the value in the output
	 * array is the value to be passed to the gphoto command, although this is
	 * done automatically.
	 * @throws JPARSECException If an error occurs.
	 */
	public String[] getConfig(CAMERA_PARAMETER config)
	throws JPARSECException {
		if (config == CAMERA_PARAMETER.ZOOM && id.zoomValues != null) return id.zoomValues;
		String[] values = cameraPossibleConfigsValues[config.ordinal()];
		if (values != null) return values;

		if (liveView) return null;
		String command = "gphoto2 --camera \""+this.model+"\" --port "+this.port+" --get-config "+cameraConfigs[config.ordinal()];
		Process p = ApplicationLauncher.executeCommand(command);
		if (debug) System.out.println("Executing command: "+command);
		String out = ApplicationLauncher.getConsoleOutputFromProcess(p);
		String array[] = DataSet.toStringArray(out, FileIO.getLineSeparator());

		StringBuffer choices = new StringBuffer("");
		for (int i=0; i<array.length; i++)
		{
			int c = array[i].indexOf("Type:");
			if (c >= 0) {
				c = array[i].toLowerCase().indexOf("range");
				if (c >= 0) {
					config.isRange = true;
					for (int j = i + 1; j < array.length; j ++) {
						c = array[j].toLowerCase().indexOf("bottom:");
						if (c >= 0) config.minRange = Float.parseFloat(DataSet.replaceAll(array[j].substring(c+7).trim(), ",", ".", true));
						c = array[j].toLowerCase().indexOf("top:");
						if (c >= 0) config.maxRange = Float.parseFloat(DataSet.replaceAll(array[j].substring(c+4).trim(), ",", ".", true));
						c = array[j].toLowerCase().indexOf("step:");
						if (c >= 0) config.stepRange = Float.parseFloat(DataSet.replaceAll(array[j].substring(c+5).trim(), ",", ".", true));
					}
					if (config == CAMERA_PARAMETER.FOCUS) {
						int val[] = new int[] {200, 1000, 5000};
						for (int n=0; n<3; n++) {
							if (!choices.equals("")) choices.append(FileIO.getLineSeparator());
							choices.append(""+(int)(val[n]*config.stepRange));
							choices.append(FileIO.getLineSeparator());
							choices.append("-"+(int)(val[n]*config.stepRange));						
						}
					} else {
						for (int n=1; n<=3; n++) {
							if (!choices.equals("")) choices.append(FileIO.getLineSeparator());
							choices.append(""+(n*config.stepRange));
							choices.append(FileIO.getLineSeparator());
							choices.append("-"+(n*config.stepRange));						
						}
					}
				}
			}

			c = array[i].indexOf("Choice:");
			if (c >= 0) {
				if (!choices.equals("")) choices.append(FileIO.getLineSeparator());
				String newChoice = array[i].substring(c+7).trim();
				newChoice = newChoice.substring(newChoice.indexOf(" ")).trim();
				choices.append(newChoice);
			}
		}
		String data = choices.toString();
/*		if ((data == null || data.equals("")) && (config.isRange() != null && config.isRange())) {
			int nstep = (int) (1 + (config.maxRange - config.minRange) / config.stepRange);
			double val[] = DataSet.getSetOfValues(config.minRange, config.maxRange, nstep, false);
			int ival[] = DataSet.toIntArray(val);
			String sval[] = DataSet.toStringValues(ival);
			sval = DataSet.addStringArray(sval, new String[] {"+"+config.stepRange, "-"+config.stepRange});
			if (nstep / 12 > 4) {
				sval = DataSet.addStringArray(sval, new String[] {"+"+(2*config.stepRange), "-"+(2*config.stepRange)});
				if (nstep / 12 > 11)
					sval = DataSet.addStringArray(sval, new String[] {"+"+(3*config.stepRange), "-"+(3*config.stepRange)});
			}
			return sval;
		}
*/		
		return DataSet.toStringArray(data, FileIO.getLineSeparator());
	}

	/**
	 * Returns  if a given configuration parameter is available or not in the current
	 * camera. Note all parameters except the aperture must be available, otherwise the
	 * instance cannot be used. This should be the case when using the manual model. The
	 * availability of the parameters are updated after calling {@linkplain #checkConfig(boolean)},
	 * that will make the aperture available again if that method is called after attaching
	 * an objective to the camera.
	 * @param c The configuration parameter.
	 * @return True or false.
	 */
	public boolean isParameterAvailable(CAMERA_PARAMETER c) {
		return configParamFound[c.ordinal()];
	}

	/**
	 * Sets the value of a given configuration parameter.
	 * @param c The configuration parameter.
	 * @param value The value.
	 * @throws JPARSECException If an error occurs.
	 */
	public void setParameter(CAMERA_PARAMETER c, String value)
	throws JPARSECException {
		if (c.isReadOnly()) return;
		if (c == CAMERA_PARAMETER.FOCUS && (value == null || value.equals(""))) {
			cameraConfigsValues[c.ordinal()] = null;
			foc = null;
			return;
		}
		if (c.isRange != null && c.isRange) {
			if (value.startsWith("+") || value.startsWith("-")) {
				int cval = Integer.parseInt(cameraConfigsValues[c.ordinal()]);
				cval += Integer.parseInt(value);
				value = ""+cval;
			}
			cameraConfigsValues[c.ordinal()] = value;
			if (liveView) {
				if (c == CAMERA_PARAMETER.ISO) iso = ""+value;
				if (c == CAMERA_PARAMETER.SHUTTER_SPEED) ss = ""+value;
				if (c == CAMERA_PARAMETER.APERTURE) aper = ""+value;
				if (c == CAMERA_PARAMETER.RESOLUTION) res = ""+value;
				if (c == CAMERA_PARAMETER.NIKON_QUALITY) res2 = ""+value;
				if (c == CAMERA_PARAMETER.CAPTURE_TARGET) tar = ""+value;
				if (c == CAMERA_PARAMETER.ZOOM) zoo = ""+value;
				if (c == CAMERA_PARAMETER.ZOOM_POSITION) zoo_pos = ""+value;
			}
			if (c == CAMERA_PARAMETER.FOCUS) foc = ""+value;
		} else {
			int cv = DataSet.getIndex(this.cameraPossibleConfigsValues[c.ordinal()], value);
			if (c != CAMERA_PARAMETER.ZOOM_POSITION && cv == -1) throw new JPARSECException("Value "+value+" for parameter "+c.name()+" is not available.");
			cameraConfigsValues[c.ordinal()] = ""+cv;
			if (liveView) {
				if (c == CAMERA_PARAMETER.ISO) iso = ""+value;
				if (c == CAMERA_PARAMETER.SHUTTER_SPEED) ss = ""+value;
				if (c == CAMERA_PARAMETER.APERTURE) aper = ""+value;
				if (c == CAMERA_PARAMETER.RESOLUTION) res = ""+value;
				if (c == CAMERA_PARAMETER.NIKON_QUALITY) res2 = ""+value;
				if (c == CAMERA_PARAMETER.CAPTURE_TARGET) tar = ""+value;
				if (c == CAMERA_PARAMETER.ZOOM) zoo = ""+value;
				if (c == CAMERA_PARAMETER.ZOOM_POSITION) zoo_pos = ""+value;
			}
			if (c == CAMERA_PARAMETER.FOCUS) foc = ""+cv;
		}
	}

	/**
	 * Returns the current selected value for a given parameter.
	 * @param c The parameter.
	 * @return The current selected value.
	 */
	public String getParameter(CAMERA_PARAMETER c) {
		return cameraPossibleConfigsValues[c.ordinal()][Integer.parseInt(cameraConfigsValues[c.ordinal()])];
	}

	/**
	 * Returns the current selected value for a given parameter in the camera itself.
	 * This value can be different from the current selected value in the instance.
	 * @param config The parameter.
	 * @return The current selected value, or null in case the value cannot be found.
	 * For instance, in direct focus photography the aperture returned will be null,
	 * and in live view mode null will be returned always.
	 * @throws JPARSECException If an error occurs.
	 */
	public String getParameterFromCamera(CAMERA_PARAMETER config) throws JPARSECException {
		if (liveView) {
			if (config == CAMERA_PARAMETER.FOCAL_LENGTH && liveFocalLength != null)
				return liveFocalLength;
			return null;
		}
		String command = "gphoto2 --camera \""+this.model+"\" --port "+this.port+" --get-config "+cameraConfigs[config.ordinal()];
		Process p = ApplicationLauncher.executeCommand(command);
		if (debug) System.out.println("Executing command: "+command);
		String out = ApplicationLauncher.getConsoleOutputFromProcess(p);
		String array[] = DataSet.toStringArray(out, FileIO.getLineSeparator());
		String current = null;
		for (int i=0; i<array.length; i++)
		{
			int c = array[i].indexOf("Type:");
			if (c >= 0) {
				c = array[i].toLowerCase().indexOf("range");
				if (c >= 0) {
					config.isRange = true;
					for (int j = i + 1; j < array.length; j ++) {
						c = array[j].toLowerCase().indexOf("bottom:");
						if (c >= 0) config.minRange = Float.parseFloat(DataSet.replaceAll(array[j].substring(c+7).trim(), ",", ".", true));
						c = array[j].toLowerCase().indexOf("top:");
						if (c >= 0) config.maxRange = Float.parseFloat(DataSet.replaceAll(array[j].substring(c+4).trim(), ",", ".", true));
						c = array[j].toLowerCase().indexOf("step:");
						if (c >= 0) config.stepRange = Float.parseFloat(DataSet.replaceAll(array[j].substring(c+5).trim(), ",", ".", true));
					}
				}
			}
			c = array[i].indexOf("Current:");
			if (c >= 0) {
				current = array[i].substring(c);
				current = current.substring(current.indexOf(" ")).trim();
				break;
			}
		}
		return current;
	}

	/**
	 * Returns if a copy of each shot will be also at the camera's card.
	 * @return True or false.
	 */
	public boolean isCopyInCamera() {
		return maintainCopyInCamera;
	}

	/**
	 * Sets if a copy of each shot should be maintained in the camera's memory.
	 * @param copy True or false.
	 */
	public void setCopyInCamera(boolean copy) {
		this.maintainCopyInCamera = copy;
	}

	/**
	 * Returns if mirror lock up is enabled when shooting. Mirror will lock up
	 * before the shot to reduce vibrations.
	 * @return True or false.
	 */
	public boolean isMirrorLockUp() {
		return mirrorLockUp;
	}

	/**
	 * Sets if mirror lock up should be used.
	 * @param lock True or false.
	 */
	public void setMirrorLockUp(boolean lock) {
		this.mirrorLockUp = lock;
	}

	/**
	 * Returns if debug is enabled or not. Debug will write gphoto2 commands to console.
	 * @return True or false.
	 */
	public boolean isDebug() {
		return debug;
	}

	/**
	 * Sets the debug flag option. Debug will write gphoto2 commands to console.
	 * @param b True or false.
	 */
	public void setDebug(boolean b) {
		this.debug = b;
	}
	
	/**
	 * Set the exposition time for bulb mode.
	 * @param seconds Time in seconds.
	 */
	public void setBulbTime(int seconds) {
		this.bulbTime = seconds;
	}

	/**
	 * Returns the exposition time for bulb mode.
	 * @return Time in seconds.
	 */
	public int getBulbTime() {
		return this.bulbTime;
	}

	/**
	 * Returns the Bayer matrix of this camera for raw images, from
	 * top-left corner clockwise.
	 * @return Bayer matrix.
	 */
	public String getBayerMatrix() {
		return BayerMatrix;
	}
	/**
	 * Returns the camera model.
	 * @return Camera model.
	 */
	public String getModel()
	{
		return this.model;
	}
	/**
	 * Returns the camera port.
	 * @return Camera port.
	 */
	public String getPort()
	{
		return this.port;
	}
	/**
	 * Return time since last shot.
	 * @return Time lapse in seconds, or -1 if no first shot exists.
	 */
	public int lapseSinceLastShot()
	{
		if (this.lastShotTime == -1) return -1;
		int lapse = (int) (0.5 + (double) (System.nanoTime()/1000000 - this.lastShotTime) / 1000.0);
		return lapse;
	}

	/**
	 * Returns the current working directory where
	 * images are downloaded from the camera.
	 * @return Working directory.
	 */
	public String getWorkingDirectory() {
		return this.workingDir;
	}
	/**
	 * Sets the working directory to download images.
	 * @param dir New working directory.
	 */
	public void setWorkingDirectory(String dir) {
		this.workingDir = dir;
		if (!dir.endsWith(FileIO.getFileSeparator())) workingDir += FileIO.getFileSeparator();
	}
	/**
	 * Returns if a shot is being taken.
	 * @return True or false.
	 */
	public boolean isShooting() {
		return isShooting;
	}
	/**
	 * Returns if the camera is in bulb mode or not.
	 * @return True or false.
	 */
	public boolean isBulbMode() {
		return cameraPossibleConfigsValues[CAMERA_PARAMETER.SHUTTER_SPEED.ordinal()][Integer.parseInt(cameraConfigsValues[CAMERA_PARAMETER.SHUTTER_SPEED.ordinal()])].startsWith("b");
	}

	/**
	 * Clones this object.
	 */
	@Override
	public Object clone()
	{
		GPhotoCamera cam = null;
		try {
			cam = new GPhotoCamera(this.id, this.model, this.port, this.workingDir, this.maintainCopyInCamera, this.BayerMatrix);
			cam.DEFAULT_CONFIGS = this.DEFAULT_CONFIGS.clone();
			cam.cameraConfigsValues = this.cameraConfigsValues.clone();
			cam.cameraConfigs = this.cameraConfigs.clone();
		} catch (Exception exc) {}
		return cam;
	}

	class ShotThread extends Thread {
		public int frames = 1;
		public long shotStarted, shotEnded;
		
		public ShotThread() {}
		public ShotThread(int f) {
			frames = f;
		}

		@Override
		public void run() {
			isShooting = true;
			try {
				//if (!isStillConnected(true)) throw new JPARSECException("Camera "+model+" is no longer detected on port "+port.trim()+"!");

				if (lastShotPath != null) {
					String val[] = DataSet.toStringArray(lastShotPath, ",");
					for (int i=0; i<val.length; i++) {
						File file = new File(val[i]);
						if (val[i].startsWith("capt") && file.exists()) file.delete();
					}
				}
				File file = new File(workingDir + "capt0000.jpg");
				if (file.exists()) file.delete();
				File file2 = new File(workingDir + "capt0000.cr2");
				if (file2.exists()) file2.delete();
				File file3 = new File(workingDir + "capt0000.nef");
				if (file3.exists()) file3.delete();

				String configCommand = "";

				for (int i=0; i<5; i++)
				{
					if (cameraConfigs[i] != null && !cameraConfigs[i].equals("") &&
							cameraConfigsValues[i] != null && !cameraConfigsValues[i].equals(""))
						configCommand += "--set-config-index "+cameraConfigs[i]+"="+cameraConfigsValues[i]+" ";
				}
				if (getModel().toLowerCase().indexOf("nikon") >= 0) {
					if (cameraConfigs[12] != null && !cameraConfigs[12].equals("") &&
							cameraConfigsValues[12] != null && !cameraConfigsValues[12].equals(""))
								configCommand += "--set-config-index "+cameraConfigs[12]+"="+cameraConfigsValues[12]+" ";
				}
				cameraConfigsValues[CAMERA_PARAMETER.FOCUS.ordinal()] = null;

				String filesBefore[] = FileIO.getFiles(workingDir);
				boolean bulbMode = isBulbMode();
				String command = "";
				String focus = "", wtime = "1";
				if (foc != null) {
					wtime = "0.5";
					focus = "--set-config-index "+cameraConfigs[CAMERA_PARAMETER.FOCUS.ordinal()]+"="+foc+" --wait-event="+wtime+"s ";
					foc = null;
				}
				if (bulbMode) {
					command = "";
					if (mirrorLockUp) command = "--set-config "+cameraConfigs[CAMERA_PARAMETER.VIEWFINDER.ordinal()]+"="+id.viewfinderUp+" --wait-event="+wtime+"s";
					command = "gphoto2 --camera \""+model+"\" --port "+port+" "+configCommand+command+" --set-config "+focus+cameraConfigs[CAMERA_PARAMETER.BULB.ordinal()]+"="+id.bulbModeEnabled+" --wait-event="+bulbTime+"s --set-config "+cameraConfigs[CAMERA_PARAMETER.BULB.ordinal()]+"="+id.bulbModeDisabled+" --wait-event-and-download";
					command += "=5s";
					if (maintainCopyInCamera) {
						command +=" --keep";
					} else {
						command +=" --no-keep";
					}
				} else {
					command = "";
					if (mirrorLockUp) command = "--set-config "+cameraConfigs[CAMERA_PARAMETER.VIEWFINDER.ordinal()]+"="+id.viewfinderUp+" --wait-event="+wtime+"s";
					command = "gphoto2 --camera \""+model+"\" --port "+port+" -F "+frames+" -I 1 "+configCommand+command+" "+focus+"--capture-image-and-download";
					if (maintainCopyInCamera) {
						command +=" --keep";
					} else {
						command +=" --no-keep";
					}
				}
				
				shotStarted = System.currentTimeMillis();
				Process p = ApplicationLauncher.executeCommand(command, null, new File(workingDir));
				if (debug) System.out.println("Executing command (from dir "+workingDir+"): "+command);
				p.waitFor();
				shotEnded = System.currentTimeMillis();
/*				String out = ApplicationLauncher.getConsoleOutputFromProcess(p);
				String array[] = DataSet.toStringArray(out, FileIO.getLineSeparator());

				if (!bulbMode && maintainCopyInCamera) {
					// get downloaded file name
					String newFile = getFileName(array[2]), newFile2 = null;
					String line = getNewFileName(array[2]);
					String sa[] = DataSet.getSubArray(array, 3, array.length-1);
					int index = DataSet.getIndexStartingWith(sa, line);
					if (index >= 0) newFile2 = getFileName(sa[index]);
					lastShotPath = workingDir + newFile;
					if (newFile2 != null) lastShotPath += "," + workingDir + newFile2;

					String folder = getFolderName(array[2]);
					command = "gphoto2 --camera \""+model+"\" --port "+port+" --folder "+folder+" --list-files";
					p = ApplicationLauncher.executeCommand(command);
					out = ApplicationLauncher.getConsoleOutputFromProcess(p);
					array = DataSet.toStringArray(out, FileIO.getLineSeparator());
					index = -1;
					int index2 = -1;
					for (int i=0; i<array.length; i++)
					{
						if (array[i].indexOf(newFile) >= 0) {
							String fieldNum = FileIO.getField(1, array[i], " ", true);
							index = Integer.parseInt(fieldNum.substring(1));
						}
						if (newFile2 != null) {
							if (array[i].indexOf(newFile2) >= 0) {
								String fieldNum = FileIO.getField(1, array[i], " ", true);
								index2 = Integer.parseInt(fieldNum.substring(1));
							}
						}
					}

					if (index > 0) {
						p = ApplicationLauncher.executeCommand(
								"gphoto2 --camera \""+model+"\" --port "+port+" --folder "+folder+" --get-file "+index+"-"+index,
								null, new File( workingDir));
						out = ApplicationLauncher.getConsoleOutputFromProcess(p);
						array = DataSet.toStringArray(out, FileIO.getLineSeparator());
					}
					if (index2 > 0) {
						p = ApplicationLauncher.executeCommand(
								"gphoto2 --camera \""+model+"\" --port "+port+" --folder "+folder+" --get-file "+index2+"-"+index2,
								null, new File( workingDir));
						out = ApplicationLauncher.getConsoleOutputFromProcess(p);
						array = DataSet.toStringArray(out, FileIO.getLineSeparator());
					}
					if (index < 0 && index2 < 0) {
						String img = newFile;
						if (newFile2 != null) img +=", "+newFile2;
						throw new JPARSECException("Could not find image "+img+" in folder "+folder+".");
					}
				}
*/
				String filesAfter[] = FileIO.getFiles(workingDir);
				String lastShotPath = "";
				for (int i=0; i<filesAfter.length; i++) {
					if (DataSet.getIndex(filesBefore, filesAfter[i]) < 0) {
						File f = new File(filesAfter[i]);
						if (f.isDirectory()) continue;
						lastShotPath += filesAfter[i]+",";
					}
				}
				if (lastShotPath.equals("")) {
					lastShotPath = null;
				} else {
					lastShotPath = lastShotPath.substring(0, lastShotPath.length()-1);
				}
				setLastShotPath(lastShotPath);
				lastShotTime = System.nanoTime()/1000000;
			} catch (Exception exc) {
				exc.printStackTrace();
				JOptionPane.showMessageDialog(null, Translate.translate(1177) + DataSet.toString(JPARSECException.toStringArray(exc.getStackTrace()), FileIO.getLineSeparator()),
						Translate.translate(230), JOptionPane.WARNING_MESSAGE);
				lastShotPath = null;
			}
			isShooting = false;
		}
	}
/*
	private String getFileName(String newFile) {
		int begin = newFile.indexOf("/");
		newFile = newFile.substring(begin);
		int end = newFile.indexOf(".");
		newFile = newFile.substring(0, end+4);
		int lastBar = newFile.lastIndexOf("/");
		newFile = newFile.substring(lastBar+1);
		return newFile;
	}
	private String getFolderName(String newFile) {
		int begin = newFile.indexOf("/");
		newFile = newFile.substring(begin);
		int end = newFile.indexOf(".");
		newFile = newFile.substring(0, end+4);
		int lastBar = newFile.lastIndexOf("/");
		return newFile.substring(0, lastBar+1);
	}
	private String getNewFileName(String newFile) {
		int begin = newFile.indexOf("/");
		return newFile.substring(0, begin).trim();
	}
*/

	/**
	 * Returns if the camera is still connected to the same port.
	 * @param fix True to fix the port value in case the camera is attached to the
	 * same usb, but suffered a switch off/on, so that the usb identifier has changed
	 * despite of being connected to the same port.
	 * @return True or false.
	 * @throws JPARSECException If an error occurs.
	 */
	public boolean isStillConnected(boolean fix) throws JPARSECException {
		String cameras[] = GPhotoCamera.autoDetect(debug);
		boolean ok = false;
		String newPort = null;
		for (int i=0; i<cameras.length; i++)
		{
			int usb = cameras[i].indexOf("usb:");
			String model = cameras[i].substring(0, usb).trim();
			String port = cameras[i].substring(usb);

			if (model.equals(this.model) && port.equals(this.port)) {
				ok = true;
				break;
			} else {
				if (fix && model.equals(this.model)) {
					int c = port.indexOf(",");
					if (c > 0 && this.port.startsWith(port.substring(0, c+1))) {
						newPort = port;
					}
				}
			}
		}
		if (newPort != null) {
			this.port = newPort;
			ok = true;
		}
		return ok;
	}

	private Object updatePanel = null;
	/**
	 * Starts live view.
	 * @param panel The panel that will show the live view images. This
	 * object must implement a method repaint(BufferedImage).
	 * @param fps The approximate  number of fps for the updated images.
	 * In slow devices this should be only a few. Between 1 and 30, otherwise
	 * 4 will be used. In case you want to update the image once per 2 seconds,
	 * set this value to -2, -3 for one update per 3 seconds, and so on.
	 */
	public void startLiveView(Object panel, int fps) {
		if (!liveView) {
			liveView = true;
			Thread thread = new Thread(new live());
			thread.start();
			updatePanel = panel;
		}
		this.fps = 4;
		if (fps < 0 || (fps > 0 && fps < 30)) this.fps = fps;
	}

	/**
	 * Starts live shot mode. Live shot mode will use the current settings for
	 * iso, shutter speed, and so on to simulate some kind of 'live view', but
	 * with the complete full resolucion shot.
	 * @param panel The panel that will show the live view images. This
	 * object must implement a method repaint(BufferedImage).
	 * @param fps The approximate  number of fps for the updated images.
	 * In slow devices this should be only a few. Between 1 and 30, otherwise
	 * 4 will be used. In case you want to update the image once per 2 seconds,
	 * set this value to -2, -3 for one update per 3 seconds, and so on.
	 */
	public void startLiveShot(Object panel, int fps) {
		if (!liveView) {
			liveView = true;
			Thread thread = new Thread(new liveShot());
			thread.start();
			updatePanel = panel;
		}
		this.fps = 4;
		if (fps < 0 || (fps > 0 && fps < 30)) this.fps = fps;
	}
	
	/**
	 * Sets an external command to be sent to gphoto during live view in shell mode, 
	 * when live view is paused. Only executed once.
	 * @param command External command for shell mode.
	 */
	public void executeExternalCommandInPausedLiveView(String command) {
		ext = command;
	}

	/**
	 * Returns the external command sent to gphoto during live view in shell mode, 
	 * when live view is paused. The command is reset to null after is executed.
	 * @return The external command or null.
	 */
	public String getExternalCommandInPausedLiveView() {
		return ext;
	}
	
	/**
	 * Sets the fps for live view or live shot modes.
	 * @param fps The approximate  number of fps for the updated images.
	 * In slow devices this should be only a few. Between 1 and 30, otherwise
	 * 4 will be used. In case you want to update the image once per 2 seconds,
	 * set this value to -2, -3 for one update per 3 seconds, and so on.
	 */
	public void setLiveFPS(int fps) {
		this.fps = 4;
		if (fps < 0 || (fps > 0 && fps < 30)) this.fps = fps;
	}

	/**
	 * Stops live view.
	 */
	public void stopLiveView() {
		liveView = false;
	}

	/**
	 * Pauses live view.
	 */
	public void pauseLiveView() {
		if (liveView) liveViewRunning = false;
	}

	/**
	 * Resumes live view.
	 */
	public void resumeLiveView() {
		if (liveView) liveViewRunning = true;
	}

	/**
	 * Returns true when live view (or live shot) is active but paused.
	 * @return True or false.
	 */
	public boolean isLivePaused() {
		if (liveView && !liveViewRunning) return true;
		return false;
	}
	
	/**
	 * Returns if live view or live shot modes are currently working.
	 * @return True or false.
	 */
	public boolean isLive() {
		return liveView | liveViewRunning;
	}

	/**
	 * Sets a time limit for live view and continuous shot modes. Default
	 * value is 0, so that there is no time limit. When live view is used
	 * for too long some cameras could end locked.
	 * @param seconds Maximum time in seconds. Set to &lt;=0 for no limit.
	 */
	public void setTimeLimitForLiveView(int seconds) {
		liveMaxTime = seconds;
	}

	private boolean liveView = false, liveViewRunning = false;
	private String foc = null, zoo = null, zoo_pos = null, iso = null, aper = null, ss = null, 
			res = null, res2 = null, tar = null, liveFocalLength = null, ext = null;
	private int fps, liveMaxTime = 0;
	private long timeLimit;
	class live implements Runnable {
		public void run() {
			liveView = true;
			liveViewRunning = true;
			timeLimit = -1;
			if (liveMaxTime > 0) timeLimit = System.nanoTime()/1000000 + liveMaxTime * 1000;
			boolean started = false;
			String command = null; //, line;
			Process p = null;
			BufferedWriter writer = null;
			liveFocalLength = null;
			try {
				String shell[] = ApplicationLauncher.getShell();
		        ProcessBuilder builder = null;
		        if (shell.length == 1) {
		        	builder = new ProcessBuilder(shell[0]);
		        } else {
		        	builder = new ProcessBuilder(shell[0], shell[1]);
		        }
		        builder.directory(new File(workingDir));
		        builder.redirectErrorStream();
		        p = builder.start();
				if (debug) System.out.println("Entering shell (from dir "+workingDir+"): "+DataSet.toString(shell, " "));

		        OutputStream stdin = p.getOutputStream();
		        InputStream stdout = p.getInputStream();

		        writer = new BufferedWriter(new OutputStreamWriter(stdin));

				String fileName = "capture_preview.jpg";
				command = "gphoto2 --camera \""+getModel()+"\" --port "+getPort()+" --quiet";
				if (maintainCopyInCamera) {
					command +=" --keep";
				} else {
					command +=" --no-keep";
				}
				command += " --shell"+FileIO.getLineSeparator();
				if (debug) System.out.println("Executing shell command: "+command);
		        writer.write(command);
	        	writer.flush();
				Thread.sleep(250);

				if (model.toLowerCase().indexOf("canon") >= 0 && isParameterAvailable(CAMERA_PARAMETER.VIEWFINDER)) {
					command = "set-config "+cameraConfigs[CAMERA_PARAMETER.VIEWFINDER.ordinal()]+"="+id.viewfinderUp+FileIO.getLineSeparator();
					if (debug) System.out.println("Executing shell command: "+command);
			        writer.write(command);
		        	writer.flush();
					Thread.sleep(250);
				}
				started = true;

		    	//byte[] b = new byte[1024*10];
	        	//b = new byte[1024*10];
	        	//stdout.read(b);
	        	//System.out.println(new String(b));

				FileIO.deleteFile(workingDir + fileName);
				Thread.sleep(2000);

				command = "capture-preview"+FileIO.getLineSeparator();
				if (debug) System.out.println("Executing shell command: "+command);
				writer.write(command);
				writer.flush();
				Thread.sleep(2000);

				Method m = null;
				if (updatePanel != null) {
					m = updatePanel.getClass().getDeclaredMethod("repaint", new Class[] {BufferedImage.class});
					m.setAccessible(true);
				}
				long t0 = System.nanoTime()/1000000;
				long lastFL = -1;
				while (liveView && (timeLimit == -1 || System.nanoTime()/1000000 < timeLimit)) {
					if (liveViewRunning) {
						if (isParameterAvailable(CAMERA_PARAMETER.FOCAL_LENGTH)) {
							long now = System.currentTimeMillis();
							double elapsed = (now - lastFL) * 0.001;
							if (lastFL < 0 || elapsed > 3) {
								lastFL = now;
						    	byte[] b = new byte[1024*10];
					        	stdout.read(b);
								command = "get-config "+cameraConfigs[CAMERA_PARAMETER.FOCAL_LENGTH.ordinal()]+FileIO.getLineSeparator();
								if (debug) System.out.println("Executing shell command: "+command);
								writer.write(command);
								writer.flush();
								Thread.sleep(125);
					        	b = new byte[1024*10];
					        	stdout.read(b);
					        	String s = new String(b).trim();
					        	int ci = s.indexOf("Current: ");
					        	if (ci >= 0) s = s.substring(ci + 9).trim();
					        	ci = s.indexOf("Bottom");
					        	if (ci >= 0) s = s.substring(0, ci).trim();
					        	liveFocalLength = s;
							}
						}
						if (foc != null) {
							command = "set-config-index "+cameraConfigs[CAMERA_PARAMETER.FOCUS.ordinal()]+"="+foc+FileIO.getLineSeparator();
							if (debug) System.out.println("Executing shell command: "+command);
							writer.write(command);
							writer.flush();
							Thread.sleep(250);
							foc = null;
						}
						if (zoo != null) {
							command = "set-config "+cameraConfigs[CAMERA_PARAMETER.ZOOM.ordinal()]+"="+zoo+FileIO.getLineSeparator();
							if (debug) System.out.println("Executing shell command: "+command);
							writer.write(command);
							writer.flush();
							Thread.sleep(250);
							zoo = null;
						}
						if (zoo_pos != null) {
							command = "set-config "+cameraConfigs[CAMERA_PARAMETER.ZOOM_POSITION.ordinal()]+"="+zoo_pos+FileIO.getLineSeparator();
							if (debug) System.out.println("Executing shell command: "+command);
							writer.write(command);
							writer.flush();
							Thread.sleep(250);
							zoo_pos = null;
						}
						if (iso != null) {
							command = "set-config "+cameraConfigs[CAMERA_PARAMETER.ISO.ordinal()]+"="+iso+FileIO.getLineSeparator();
							if (debug) System.out.println("Executing shell command: "+command);
							writer.write(command);
							writer.flush();
							Thread.sleep(250);
							iso = null;
						}
						if (aper != null) {
							command = "set-config "+cameraConfigs[CAMERA_PARAMETER.APERTURE.ordinal()]+"="+aper+FileIO.getLineSeparator();
							if (debug) System.out.println("Executing shell command: "+command);
							writer.write(command);
							writer.flush();
							Thread.sleep(250);
							aper = null;
						}
						if (ss != null) {
							command = "set-config "+cameraConfigs[CAMERA_PARAMETER.SHUTTER_SPEED.ordinal()]+"="+ss+FileIO.getLineSeparator();
							writer.write(command);
							writer.flush();
							Thread.sleep(250);
							ss = null;
						}
						if (res != null) {
							command = "set-config "+cameraConfigs[CAMERA_PARAMETER.RESOLUTION.ordinal()]+"="+res+FileIO.getLineSeparator();
							if (debug) System.out.println("Executing shell command: "+command);
							writer.write(command);
							writer.flush();
							Thread.sleep(250);
							res = null;
						}
						if (res2 != null) {
							command = "set-config "+cameraConfigs[CAMERA_PARAMETER.NIKON_QUALITY.ordinal()]+"="+res2+FileIO.getLineSeparator();
							if (debug) System.out.println("Executing shell command: "+command);
							writer.write(command);
							writer.flush();
							Thread.sleep(250);
							res2 = null;
						}
						if (tar != null) {
							command = "set-config "+cameraConfigs[CAMERA_PARAMETER.CAPTURE_TARGET.ordinal()]+"="+tar+FileIO.getLineSeparator();
							if (debug) System.out.println("Executing shell command: "+command);
							writer.write(command);
							writer.flush();
							Thread.sleep(250);
							tar = null;
						}
						FileIO.deleteFile(workingDir + fileName);
						command = "capture-preview"+FileIO.getLineSeparator();
						if (debug) System.out.println("Executing shell command: "+command);
						writer.write(command);
						writer.flush();
						long t1 = System.nanoTime()/1000000, dt;
						if (fps < 0) {
							dt = -1000l * fps;
						} else {
							dt = 1000l/fps;
						}
						if (dt > (t1-t0)) {
							dt -= (t1-t0);
							long target = t1 + dt;
							while (dt > 0) {
								if (dt > 1000) {
									Thread.sleep(1000);
									if (!liveView) break;
								} else {
									Thread.sleep(dt);
								}
								t1 = System.nanoTime()/1000000;
								dt = target - t1;
							}
						}
						t0 = t1;
						lastShotPath = workingDir + fileName;
						if (updatePanel != null && m != null) {
							try {
								BufferedImage img = ReadFile.readImage(workingDir + fileName);
								if (img != null) m.invoke(updatePanel, img);
							} catch (Exception exc) { exc.printStackTrace(); }
						}
					} else {
						if (ext != null) {
							String filesBefore[] = FileIO.getFiles(workingDir);
							command = ext + FileIO.getLineSeparator();
							if (debug) System.out.println("Executing shell command: "+command);
							writer.write(command);
							writer.flush();
							String filesAfter[] = null, fap[] = null;
							while (true) {
								Thread.sleep(2000);
								filesAfter = FileIO.getFiles(workingDir);
								if (fap != null) {
									if (filesAfter.length == fap.length) break;
								}
								fap = filesAfter;
							}
							String lastShotPath = "";
							for (int i=0; i<filesAfter.length; i++) {
								if (DataSet.getIndex(filesBefore, filesAfter[i]) < 0) {
									File f = new File(filesAfter[i]);
									if (f.isDirectory()) continue;
									lastShotPath += filesAfter[i]+",";
								}
							}
							if (lastShotPath.equals("")) {
								lastShotPath = null;
							} else {
								lastShotPath = lastShotPath.substring(0, lastShotPath.length()-1);
							}
							setLastShotPath(lastShotPath);
							ext = null;
						}

						t0 = System.nanoTime()/1000000;
						long dt = 0;
						if (fps < 0) {
							dt = -1000l * fps;
						} else {
							dt = 1000l/fps;
						}
						Thread.sleep(2*dt);
					}
				}
			} catch (Exception exc) {
				exc.printStackTrace();
			}

			try {
				Thread.sleep(500);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			if (started) {
				if (isParameterAvailable(CAMERA_PARAMETER.VIEWFINDER)) {
					command = "set-config "+cameraConfigs[CAMERA_PARAMETER.VIEWFINDER.ordinal()]+"="+id.viewfinderDown+FileIO.getLineSeparator();
					try {
						if (debug) System.out.println("Executing shell command: "+command);
						writer.write(command);
						writer.flush();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}

				command = "exit"+FileIO.getLineSeparator();
				try {
					if (debug) System.out.println("Finishing shell");
					writer.write(command);
					writer.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			if (p != null) p.destroy();
			stopLiveView();
			liveViewRunning = false;
		}
	}

	class liveShot implements Runnable {
		public void run() {
			liveView = true;
			liveViewRunning = true;
			boolean started = false;
			String command = null; //, line;
			Process p = null;
			BufferedWriter writer = null;
			timeLimit = -1;
			if (liveMaxTime > 0) timeLimit = System.nanoTime()/1000000 + liveMaxTime * 1000;
			try {
				if (!isStillConnected(true)) throw new JPARSECException("Camera "+model+" is no longer detected on port "+port.trim()+"!");

				String configCommand = "";

				for (int i=0; i<5; i++)
				{
					if (cameraConfigs[i] != null && !cameraConfigs[i].equals("") &&
							cameraConfigsValues[i] != null && !cameraConfigsValues[i].equals(""))
						configCommand += "--set-config-index "+cameraConfigs[i]+"="+cameraConfigsValues[i]+" ";
				}
				if (getModel().toLowerCase().indexOf("nikon") >= 0) {
					if (cameraConfigs[12] != null && !cameraConfigs[12].equals("") &&
							cameraConfigsValues[12] != null && !cameraConfigsValues[12].equals(""))
								configCommand += "--set-config-index "+cameraConfigs[12]+"="+cameraConfigsValues[12]+" ";
				}
				cameraConfigsValues[CAMERA_PARAMETER.FOCUS.ordinal()] = null;

				String shell[] = ApplicationLauncher.getShell();
		        ProcessBuilder builder = null;
		        if (shell.length == 1) {
		        	builder = new ProcessBuilder(shell[0]);
		        } else {
		        	builder = new ProcessBuilder(shell[0], shell[1]);
		        }
		        builder.directory(new File(workingDir));
		        builder.redirectErrorStream();
		        p = builder.start();
				if (debug) System.out.println("Entering shell (from dir "+workingDir+"): "+DataSet.toString(shell, " "));

		        OutputStream stdin = p.getOutputStream();
		        //InputStream stdout = p.getInputStream();

		        writer = new BufferedWriter(new OutputStreamWriter(stdin));

				String fileName = "capture.jpg";
				if (maintainCopyInCamera) {
					command ="--keep";
				} else {
					command ="--no-keep --filename "+fileName;
				}
				command = "gphoto2 --camera \""+getModel()+"\" --port "+getPort()+" "+command+" --quiet "+configCommand+" --shell"+FileIO.getLineSeparator();
				if (debug) System.out.println("Executing shell command: "+command);
		        writer.write(command);
	        	writer.flush();
				Thread.sleep(250);

				if (model.toLowerCase().indexOf("canon") >= 0 && isParameterAvailable(CAMERA_PARAMETER.VIEWFINDER)) {
					command = "set-config "+cameraConfigs[CAMERA_PARAMETER.VIEWFINDER.ordinal()]+"="+id.viewfinderUp+FileIO.getLineSeparator();
					if (debug) System.out.println("Executing shell command: "+command);
			        writer.write(command);
		        	writer.flush();
					Thread.sleep(250);
				}
				
				command = "no-keep"+FileIO.getLineSeparator();
				if (debug) System.out.println("Executing shell command: "+command);
		        writer.write(command);
	        	writer.flush();
				Thread.sleep(250);
				started = true;

				if (!maintainCopyInCamera) FileIO.deleteFile(workingDir + fileName);
				Thread.sleep(2000);

				command = "capture-preview"+FileIO.getLineSeparator();
				if (debug) System.out.println("Executing shell command: "+command);
				writer.write(command);
				writer.flush();
				Thread.sleep(2000);

				Method m = null;
				if (updatePanel != null) {
	 				m = updatePanel.getClass().getDeclaredMethod("repaint", new Class[] {BufferedImage.class});
					m.setAccessible(true);
				}
				long t0 = System.nanoTime()/1000000;
				while (liveView && (timeLimit == -1 || System.nanoTime()/1000000 < timeLimit)) {
					if (liveViewRunning) {
						if (!maintainCopyInCamera) FileIO.deleteFile(workingDir + fileName);
						if (foc != null) {
							command = "set-config-index "+cameraConfigs[CAMERA_PARAMETER.FOCUS.ordinal()]+"="+foc+FileIO.getLineSeparator();
							if (debug) System.out.println("Executing shell command: "+command);
							writer.write(command);
							writer.flush();
							Thread.sleep(250);
							foc = null;
						}
						if (zoo != null) zoo = null;
						if (zoo_pos != null) zoo_pos = null;
						if (iso != null) {
							command = "set-config "+cameraConfigs[CAMERA_PARAMETER.ISO.ordinal()]+"="+iso+FileIO.getLineSeparator();
							if (debug) System.out.println("Executing shell command: "+command);
							writer.write(command);
							writer.flush();
							Thread.sleep(250);
							iso = null;
						}
						if (aper != null) {
							command = "set-config "+cameraConfigs[CAMERA_PARAMETER.APERTURE.ordinal()]+"="+aper+FileIO.getLineSeparator();
							if (debug) System.out.println("Executing shell command: "+command);
							writer.write(command);
							writer.flush();
							Thread.sleep(250);
							aper = null;
						}
						if (ss != null) {
							command = "set-config "+cameraConfigs[CAMERA_PARAMETER.SHUTTER_SPEED.ordinal()]+"="+ss+FileIO.getLineSeparator();
							if (debug) System.out.println("Executing shell command: "+command);
							writer.write(command);
							writer.flush();
							Thread.sleep(250);
							ss = null;
						}
						if (res != null) {
							command = "set-config "+cameraConfigs[CAMERA_PARAMETER.RESOLUTION.ordinal()]+"="+res+FileIO.getLineSeparator();
							if (debug) System.out.println("Executing shell command: "+command);
							writer.write(command);
							writer.flush();
							Thread.sleep(250);
							res = null;
						}
						if (res2 != null) {
							command = "set-config "+cameraConfigs[CAMERA_PARAMETER.NIKON_QUALITY.ordinal()]+"="+res2+FileIO.getLineSeparator();
							if (debug) System.out.println("Executing shell command: "+command);
							writer.write(command);
							writer.flush();
							Thread.sleep(250);
							res2 = null;
						}
						if (tar != null) {
							command = "set-config "+cameraConfigs[CAMERA_PARAMETER.CAPTURE_TARGET.ordinal()]+"="+tar+FileIO.getLineSeparator();
							if (debug) System.out.println("Executing shell command: "+command);
							writer.write(command);
							writer.flush();
							Thread.sleep(250);
							tar = null;
						}
						String filesBefore[] = null;
						if (maintainCopyInCamera) filesBefore = FileIO.getFiles(workingDir);
						if (isBulbMode()) {
							command = "set-config "+cameraConfigs[CAMERA_PARAMETER.BULB.ordinal()]+"="+id.bulbModeEnabled+FileIO.getLineSeparator();
							if (debug) System.out.println("Executing shell command: "+command);
							writer.write(command);
							writer.flush();
							Thread.sleep(bulbTime*1000l);
							command = "set-config "+cameraConfigs[CAMERA_PARAMETER.BULB.ordinal()]+"="+id.bulbModeDisabled+FileIO.getLineSeparator();
							if (debug) System.out.println("Executing shell command: "+command);
							writer.write(command);
							writer.flush();
							Thread.sleep(250);
							command = "wait-event-and-download 3s"+FileIO.getLineSeparator();
							if (debug) System.out.println("Executing shell command: "+command);
							writer.write(command);
							writer.flush();
						} else {
							command = "capture-image-and-download"+FileIO.getLineSeparator();
						}
						if (debug) System.out.println("Executing shell command: "+command);
						writer.write(command);
						writer.flush();
						double texp = bulbTime;
						try {
							texp = DataSet.getDoubleValueWithoutLimit(getParameter(CAMERA_PARAMETER.SHUTTER_SPEED));
						} catch (Exception exc) {}
						Thread.sleep((int)(2000l+texp*1000l));
	
						String lastShotPath = "";
						if (maintainCopyInCamera) {
							String filesAfter[] = FileIO.getFiles(workingDir);
							lastShotPath = "";
							for (int i=0; i<filesAfter.length; i++) {
								if (DataSet.getIndex(filesBefore, filesAfter[i]) < 0) {
									File f = new File(filesAfter[i]);
									if (f.isDirectory()) continue;
									lastShotPath += filesAfter[i]+",";
									if (filesAfter[i].toLowerCase().endsWith(".jpg") ||
											filesAfter[i].toLowerCase().endsWith(".png")) fileName = filesAfter[i];
								}
							}
							fileName = FileIO.getFileNameFromPath(fileName);
							if (lastShotPath.equals("")) {
								lastShotPath = null;
							} else {
								lastShotPath = lastShotPath.substring(0, lastShotPath.length()-1);
							}
						} else {
							lastShotPath = workingDir + fileName;
						}
						setLastShotPath(lastShotPath);
						lastShotTime = System.nanoTime()/1000000;
						if (updatePanel != null && m != null) {
							try {
								BufferedImage img = ReadFile.readImage(workingDir + fileName);
								if (img != null) m.invoke(updatePanel, img);
							} catch (Exception exc) { exc.printStackTrace(); }
						}
						long t1 = System.nanoTime()/1000000, dt;
						if (fps < 0) {
							dt = -1000l * fps;
						} else {
							dt = 1000l/fps;
						}
						if (dt > (t1-t0)) {
							dt -= (t1-t0);
							long target = t1 + dt;
							while (dt > 0) {
								if (dt > 1000) {
									Thread.sleep(1000);
									if (!liveView) break;
								} else {
									Thread.sleep(dt);
								}
								t1 = System.nanoTime()/1000000;
								dt = target - t1;
							}
						}
						t0 = t1;
					} else {
						t0 = System.nanoTime()/1000000;
					}
				}
			} catch (Exception exc) {
				exc.printStackTrace();
			}

			try {
				Thread.sleep(500);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			if (started) {
				if (isParameterAvailable(CAMERA_PARAMETER.VIEWFINDER)) {
					command = "set-config "+cameraConfigs[CAMERA_PARAMETER.VIEWFINDER.ordinal()]+"="+id.viewfinderDown+FileIO.getLineSeparator();
					try {
						if (debug) System.out.println("Executing shell command: "+command);
						writer.write(command);
						writer.flush();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}

				command = "exit"+FileIO.getLineSeparator();
				try {
					if (debug) System.out.println("Finishing shell");
					writer.write(command);
					writer.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			if (p != null) p.destroy();
			stopLiveView();
			liveViewRunning = false;
		}
	}
}

