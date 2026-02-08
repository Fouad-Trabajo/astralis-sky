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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.ArrayList;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.ScrollPaneConstants;

import net.miginfocom.swing.MigLayout;
import nom.tam.fits.BasicHDU;

import jparsec.astronomy.Astrometry;
import jparsec.ephem.Ephem;
import jparsec.ephem.EphemerisElement;
import jparsec.ephem.Functions;
import jparsec.ephem.Target.TARGET;
import jparsec.graph.DataSet;
import jparsec.graph.chartRendering.frame.JTableRendering;
import jparsec.io.ApplicationLauncher;
import jparsec.io.ConsoleReport;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.io.device.GenericCamera.CAMERA_MODEL;
import jparsec.io.device.GenericCamera.IMAGE_ID;
import jparsec.io.device.GenericDome.DOME_MODEL;
import jparsec.io.device.GenericTelescope.MOUNT;
import jparsec.io.device.GenericTelescope.TELESCOPE_MODEL;
import jparsec.io.device.GenericTelescope.TELESCOPE_TYPE;
import jparsec.io.device.GenericWeatherStation.WEATHER_STATION_MODEL;
import jparsec.io.device.implementation.CelestronTelescope;
import jparsec.io.device.implementation.MeadeTelescope;
import jparsec.io.image.FitsBinaryTable;
import jparsec.io.image.FitsIO;
import jparsec.io.image.FitsIO.PICTURE_LEVEL;
import jparsec.io.image.HeaderElement;
import jparsec.io.image.ImageSplineTransform;
import jparsec.io.image.Picture;
import jparsec.io.image.WCS;
import jparsec.math.Constant;
import jparsec.math.FastMath;
import jparsec.math.LinearFit;
import jparsec.math.MeanValue;
import jparsec.observer.LocationElement;
import jparsec.observer.ObserverElement;
import jparsec.observer.ObserverElement.DST_RULE;
import jparsec.time.TimeElement;
import jparsec.time.TimeElement.SCALE;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;
import jparsec.vo.SExtractor;

/**
 * A class to organize the observations in different folders, reduce them, and optionally
 * execute observations in a completely automatic way.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class ObservationManager extends JPanel implements MouseListener, ActionListener, MouseMotionListener, ComponentListener {
	private static final long serialVersionUID = 1L;

	private TELESCOPE_MODEL telescope;
	private CAMERA_MODEL cameras[];
	private DOME_MODEL dome;
	private WEATHER_STATION_MODEL weatherStation;
	private TELESCOPE_TYPE telType;
	private String workingDir, obsDir;

	private String telescopePort, cameraPort[], cameraDir[];
	private int cameraInterval[];
	private double cameraPosErr[];
	private String[] darkDir, flatDir, onDir, reducedDir, stackedDir, averagedDir;
	private boolean doReduce = false, reducePossible = false;

	private String projectName = null, projectDescription = null,
			projectObserver = null, telescopeInstitute = null;

	private BufferedImage lastImage;
	private String lastImagePath, previousSExtractor = null, lastImageTable[][] = null;
	private WCS lastImageWCS = null;
	private Astrometry lastImageAstrometry = null;

	private AVERAGE_NORMALIZATION normalizationMethod = AVERAGE_NORMALIZATION.MINIMUM;
	private COMBINATION_METHOD combineMethod = null;
	private DRIZZLE drizzleMethod = DRIZZLE.NO_DRIZZLE;
	private INTERPOLATION interpolationMethod = INTERPOLATION.BICUBIC;
	private AVERAGE_METHOD averageMethod = AVERAGE_METHOD.PONDERATION;
	private IMAGE_ORIENTATION imageOrientation = IMAGE_ORIENTATION.NOT_INVERTED;
	private boolean autoReduceOnFrames = true;
	private double humidityLimit = 80, windSpeedLimit = 100, maxTemperatureLimit = 50, minTemperatureLimit = -20;
	private LocationElement parkPos = null;
	private BasicHDU binaryTable = null;
	private Point startPt;

	private double minValueObjType = 0.5; // >= 0, => all source types
	private int minArea = 6, sigma = 8, maxSources = 50; // maxS = 0 => all detected, 50 => 50 brightest sources when solving triangles

	/**
	 * Combination methods for individual flat/dark frames.
	 */
	public enum COMBINATION_METHOD {
		/** ID constant for combining flats/darks using the median. */
		MEDIAN,
		/** ID constant for combining flats/darks using the mean average.
		 * Median method is better. */
		MEAN_AVERAGE,
		/** To combine flats/darks using the maximum intensity in each pixel. */
		MAXIMUM,
		/** To combine flats/darks using kappa sigma clipping method. */
		KAPPA_SIGMA;

		/** The value of sigma for the kappa sigma method. Default value is 3. */
		public static double kappaSigmaValue = 3;
	}

	/**
	 * The set of possible image orientations.
	 */
	public enum IMAGE_ORIENTATION {
		/** Inverted horizontally and vertically, north downwards and east towards right. */
		INVERTED_HORIZONTALLY_AND_VERTICALLY,
		/** Inverted horizontally, but N upwards. */
		INVERTED_HORIZONTALLY,
		/** Not inverted, N up and E towards left. */
		NOT_INVERTED
	};

	/**
	 * Drizzle modes for better output image quality.
	 */
	public enum DRIZZLE {
		/** ID constant for no drizzle mode. */
		NO_DRIZZLE,
		/** ID constant for 2x drizzle mode. */
		DRIZZLE_2,
		/** For drizzle 3x mode. */
		DRIZZLE_3,
		/** For drizzle 0.5x mode, reducing the resolution of
		 * the output image respect the original ones. */
		DRIZZLE_HALF
	}

	/**
	 * The different possible interpolation methods to resample images
	 * during the stacking process.
	 */
	public enum INTERPOLATION {
		/** ID constant for nearest neighbor mode. */
		NEAREST_NEIGHBOR,
		/** ID constant for bilinear interpolation. */
		BILINEAR,
		/** For bicubic interpolation. */
		BICUBIC
	}

	/**
	 * The different possible methods to obtain the intensity in a given position
	 * in the output image given a set of intensities interpolated from the stacked
	 * images.
	 */
	public enum AVERAGE_METHOD {
		/** ID constant for a ponderation operation considering all points and their
		 * distances from the interpolated point to the (integer) pixel position in the images. */
		PONDERATION,
		/** ID constant to select as output intensity the closest point (with less distance) to
		 * an integer pixel position. */
		CLOSEST_POINT,
		/** ID constant to use the same method to average stacked frames as the one selected for
		 * darks and flats. */
		USE_COMBINE_METHOD
	}

	/**
	 * The different possible methods to obtain the intensity in a given position
	 * in the output image given a set of intensities interpolated from the stacked
	 * images.
	 */
	public enum AVERAGE_NORMALIZATION {
		/** ID constant to use the minimum exposition time to normalize stacked frames to create the
		 * averaged one. Exposition time of the averaged frame will be the minimum among the input
		 * frames, preventing for overexposition. */
		MINIMUM,
		/** ID constant to use the maximum exposition time to normalize stacked frames to create the
		 * averaged one. Exposition time of the averaged frame will be the maximum among the input
		 * frames, scaling those will less time, something that could produce overexposition, but will
		 * conserve all flux. */
		MAXIMUM,
		/** ID constant to use no scaling in the stacked frames with possibly different exposition times.
		 * This can be useful to combine frames with more and less exposure to try to recover details
		 * in very bright regions of a nebula. */
		NONE
	}

	/**
	 * The names for the set of different combination methods available.
	 */
	public static final String COMBINATION_METHODS[] = new String[] {
		Translate.translate(1207),
		Translate.translate(1208),
		Translate.translate(1022),
		Translate.translate(1229)
	};

	/**
	 * The names for the set of different drizzle methods available.
	 */
	public static final String DRIZZLE_METHODS[] = new String[] {
		Translate.translate(211), "2x", "3x", "0.5x"
	};

	/**
	 * The names for the set of different interpolation methods available.
	 */
	public static final String INTERPOLATION_METHODS[] = new String[] {
		Translate.translate(1249),
		Translate.translate(1250),
		Translate.translate(1251)
	};

	/**
	 * The names for the set of different frame average methods available.
	 */
	public static final String AVERAGE_METHODS[] = new String[] {
		Translate.translate(1252),
		Translate.translate(1253),
		Translate.translate(1288)
	};

	/**
	 * The names for the set of different image orientations available.
	 */
	public static final String IMAGE_ORIENTATIONS[] = new String[] {
		Translate.translate(1269),
		Translate.translate(1268),
		Translate.translate(1270)
	};

	/**
	 * The names for the set of different frame average normalization methods
	 * available.
	 */
	public static final String NORMALIZATION_METHODS[] = new String[] {
		Translate.translate(1289),
		Translate.translate(1290),
		Translate.translate(211)
	};

	/**
	 * Constructor for an observation manager.
	 * @param mainWorkingDir Main working directory where all observations are located.
	 * Can be null, although in this case no reduction will be possible.
	 * @param obsDir New observation directory for the current night, should be a (still) non-existing directory.
	 * You can use the date or a 'project name'. Can be null, although in this case no reduction will be possible.
	 * @param telescope The telescope to use. In case of a real telescope port value will be initially null to
	 * select automatically the first available telescope. You can set later the port with {@linkplain #setTelescopePort(String)}.
	 * @param cameras The camera/s to use. Can be null, although in this case no reduction will be possible. In case
	 * of a real camera port will be initially null to select the first available one, you can later set the port
	 * with {@linkplain #setCameraPort(int, String)}.
	 * @param dome The dome model, or null.
	 * @param weatherStation The weather station model, or null.
	 * @throws JPARSECException In case any of the input values are incorrect.
	 */
	public ObservationManager(String mainWorkingDir, String obsDir, TELESCOPE_MODEL telescope,
			CAMERA_MODEL cameras[], DOME_MODEL dome, WEATHER_STATION_MODEL weatherStation) throws JPARSECException {
		this(mainWorkingDir, obsDir, telescope, cameras, dome, weatherStation, true);
	}
	
	/**
	 * Constructor for an observation manager.
	 * @param mainWorkingDir Main working directory where all observations are located.
	 * Can be null, although in this case no reduction will be possible.
	 * @param obsDir New observation directory for the current night, should be a (still) non-existing directory.
	 * You can use the date or a 'project name'. Can be null, although in this case no reduction will be possible.
	 * @param telescope The telescope to use. In case of a real telescope port value will be initially null to
	 * select automatically the first available telescope. You can set later the port with {@linkplain #setTelescopePort(String)}.
	 * @param cameras The camera/s to use. Can be null, although in this case no reduction will be possible. In case
	 * of a real camera port will be initially null to select the first available one, you can later set the port
	 * with {@linkplain #setCameraPort(int, String)}.
	 * @param dome The dome model, or null.
	 * @param weatherStation The weather station model, or null.
	 * @param createPanel True to create the panel, false to work in background from code.
	 * @throws JPARSECException In case any of the input values are incorrect.
	 */
	public ObservationManager(String mainWorkingDir, String obsDir, TELESCOPE_MODEL telescope,
			CAMERA_MODEL cameras[], DOME_MODEL dome, WEATHER_STATION_MODEL weatherStation, 
			boolean createPanel) throws JPARSECException {
		this.telescope = telescope;
		this.cameras = cameras;
		this.dome = dome;
		this.weatherStation = weatherStation;

		this.workingDir = mainWorkingDir;
		this.obsDir = obsDir;

		doReduce = false;
		if (workingDir != null) {
			File f1 = new File(mainWorkingDir);
			if (!f1.isDirectory()) throw new JPARSECException("Main working directory must be a directory");
			if (!workingDir.endsWith(FileIO.getFileSeparator())) workingDir += FileIO.getFileSeparator();
			if (this.obsDir != null) {
				if (!this.obsDir.endsWith(FileIO.getFileSeparator())) this.obsDir += FileIO.getFileSeparator();
				File f2 = new File(mainWorkingDir + this.obsDir);
				if (f2.exists()) JPARSECException.addWarning("Observation dir '"+f2.getAbsolutePath()+"' already exists"); //throw new JPARSECException("Observation dir '"+f2.getAbsolutePath()+"' already exists");
				doReduce = true;
			}
		}
		reducePossible = doReduce;

		if (telescope == null) throw new JPARSECException("Invalid telescope");
		if (cameras != null) {
			cameraPort = new String[cameras.length];
			cameraDir = new String[cameras.length];
			cameraInterval = new int[cameras.length];
			cameraPosErr = new double[cameras.length];
			darkDir = new String[cameras.length];
			flatDir = new String[cameras.length];
			onDir = new String[cameras.length];
			reducedDir = new String[cameras.length];
			stackedDir = new String[cameras.length];
			averagedDir = new String[cameras.length];
			for (int i=0; i<cameras.length; i++) {
				cameraDir[i] = "camera"+(i+1)+FileIO.getFileSeparator();
				cameraInterval[i] = 0;
				cameraPosErr[i] = 0;
				darkDir[i] = "dark"+FileIO.getFileSeparator();
				flatDir[i] = "flat"+FileIO.getFileSeparator();
				onDir[i] = "on"+FileIO.getFileSeparator();
				reducedDir[i] = "reduced"+FileIO.getFileSeparator();
				stackedDir[i] = "stacked"+FileIO.getFileSeparator();
				averagedDir[i] = "averaged"+FileIO.getFileSeparator();
			}
		}

		worker = new Worker();
		Thread workerThread = new Thread(worker);
		workerThread.start();

		if (createPanel && cameraDir != null && cameraDir.length > 0)
			createPanel();
	}

	/**
	 * Sets the combination method for the individual dark/flat frames.
	 * The median method is better.
	 * @param method The method to use for combining.
	 */
	public void setCombineMethod(COMBINATION_METHOD method) {
		this.combineMethod = method;
	}

	/**
	 * Returns the combination method. Default value is null to force a
	 * selection the first time this value is required.
	 * @return The method to use for combining.
	 */
	public COMBINATION_METHOD getCombineMethod() {
		return combineMethod;
	}

	/**
	 * Sets the interpolation method to resample calibrated on images
	 * during the stacking process.
	 * @param method The method to use for interpolation.
	 */
	public void setInterpolationMethod(INTERPOLATION method) {
		this.interpolationMethod = method;
	}

	/**
	 * Returns the interpolation method. Default value is bicubic.
	 * @return The method to use for interpolation.
	 */
	public INTERPOLATION getInterpolationMethod() {
		return interpolationMethod;
	}

	/**
	 * Sets the stack method to obtain the final intensity in a given
	 * pixel of the output image given the intensities of their corresponding
	 * pixels in each of the stacked images.
	 * @param method The method to use for stacking frames.
	 */
	public void setAverageMethod(AVERAGE_METHOD method) {
		this.averageMethod = method;
	}

	/**
	 * Returns the stack method. Default value is ponseration for better quality.
	 * @return The method to use for stacking frames.
	 */
	public AVERAGE_METHOD getAverageMethod() {
		return averageMethod;
	}

	/**
	 * Sets the normalization method to obtain the final averaged frame from stacked ones.
	 * @param method The method to use for normalization.
	 */
	public void setNormalizationMethod(AVERAGE_NORMALIZATION method) {
		this.normalizationMethod = method;
	}

	/**
	 * Returns the normalization method. Default value is minimum.
	 * @return The method to use for normalization.
	 */
	public AVERAGE_NORMALIZATION getNormalizationMethod() {
		return normalizationMethod;
	}

	/**
	 * Sets the image orientation for the output processed frames.
	 * @param method The method to use for the image orientation.
	 */
	public void setImageOrientation(IMAGE_ORIENTATION method) {
		this.imageOrientation = method;
	}

	/**
	 * Returns the image orientation. Default value is not inverted.
	 * @return The method to use for image orientation.
	 */
	public IMAGE_ORIENTATION getImageOrientation() {
		return imageOrientation;
	}

	/**
	 * Sets the drizzle method to apply to obtain the output image. Drizzling
	 * consists in oversampling the output image to try to get a better quality
	 * output.
	 * @param method The method to use for drizzle.
	 */
	public void setDrizzleMethod(DRIZZLE method) {
		this.drizzleMethod = method;
	}

	/**
	 * Returns the drizzle method. Default value is to not apply a drizzle operation.
	 * @return The method to use for drizzle.
	 */
	public DRIZZLE getDrizzleMethod() {
		return drizzleMethod;
	}
	/**
	 * Sets additional information about the current project/observations.
	 * These values will be written to the header of each .fits file in case
	 * they are not null.
	 * @param name Project name, or null.
	 * @param observer Observer's name, or null.
	 * @param description Project description, or null.
	 */
	public void setProjectInfo(String name, String observer, String description) {
		this.projectName = name;
		this.projectObserver = observer;
		this.projectDescription = description;
	}

	/**
	 * Sets the name of the institute responsible for this telescope.
	 * @param institute Institute responsible for the telescope, or null.
	 */
	public void setTelescopeInstitute(String institute) {
		this.telescopeInstitute = institute;
	}

	/**
	 * Returns the institute responsible for this telescope.
	 * @return Institute responsible for the telescope, or null.
	 */
	public String getTelescopeInstitute() {
		return this.telescopeInstitute;
	}

	/**
	 * Returns the values for the project properties.
	 * @return Project name, observer, and description. Default values
	 * are null for each of them.
	 */
	public String[] getProjectInfo() {
		return new String[] {projectName, projectObserver, projectDescription};
	}

	/**
	 * Sets the conditions for the weather alarm.
	 * @param humLim Maximum humidity outside or inside the observatory in %. Default is 80.
	 * @param windSpeedLim Maximum speed limit in km/s. Default is 100.
	 * @param tempMaxLim Maximum working temperature. Default is 50.
	 * @param tempMinLim Minimum working temperature. Default is -20.
	 */
	public void setWeatherAlarmConditions(double humLim, double windSpeedLim, double tempMaxLim, double tempMinLim) {
		this.humidityLimit = humLim;
		this.windSpeedLimit = windSpeedLim;
		this.maxTemperatureLimit = tempMaxLim;
		this.minTemperatureLimit= tempMinLim;
	}

	/**
	 * Returns the weather alarm conditions.
	 * @return Four doubles with the maximum humidity in %, maximum wind speed in km/s,
	 * maximum temperature in C, and minimum working temperature in C.
	 */
	public double[] getWeatherAlarmConditions() {
		return new double[] {humidityLimit, windSpeedLimit, maxTemperatureLimit, minTemperatureLimit};
	}

	/**
	 * Sets the telescope type.
	 * @param type Telescope type.
	 */
	public void setTelescopeType(TELESCOPE_TYPE type) {
		this.telType = type;
	}

	/**
	 * Returns the telescope type.
	 * @return Telescope type.
	 */
	public TELESCOPE_TYPE getTelescopeType() {
		return telType;
	}

	/**
	 * Sets the values to configure how SExtractor will solve sources.
	 * @param minArea Minimum number of pixels for a detection.
	 * @param sigma Minimum intensity ratio respect background for a detection in a pixel.
	 * @param minObjType Objects are classified from star-like (0) to extended (1). Setting this
	 * to 0 will return all objects, setting it to 0.5 will return only star-like objects (classified from 0 to 0.5).
	 * @param maxSources Maximum number of sources for the astrometric and photometric calibrations. Set to 0 to
	 * use all detected sources.
	 */
	public void setSExtractorValues(int minArea, int sigma, double minObjType, int maxSources) {
		this.minArea = minArea;
		this.sigma = sigma;
		this.minValueObjType = minObjType;
		this.maxSources = maxSources;
	}

	/**
	 * Returns the configuration values for SExtractor: minimum number of pixels for detection, sigma,
	 * object classification limiting value, and maximum number of sources.
	 * @return The 4 values for SExtractor configuration. Values are in a double array despite only the
	 * object classification value is not an integer.
	 */
	public double[] getSExtractorValues() {
		return new double[] {minArea, sigma, minValueObjType, maxSources};
	}

	/**
	 * Sets the telescope port.
	 * @param port Port value (COMx, /dev/tty...).
	 */
	public void setTelescopePort(String port) {
		this.telescopePort = port;
	}

	/**
	 * Sets the camera port.
	 * @param index Index of the camera.
	 * @param port Port value (usb: ... for DSLRs or /dev/videox for webcams).
	 */
	public void setCameraPort(int index, String port) {
		this.cameraPort[index] = port;
	}

	/**
	 * Returns the telescope port.
	 * @return Telescope port (COMx, /dev/tty...).
	 * Null is returned if not set.
	 */
	public String getTelescopePort() {
		return telescopePort;
	}

	/**
	 * Returns the camera port.
	 * @param index Index for the camera.
	 * @return The camera port (usb: ... for DSLRs or /dev/videox for webcams).
	 * Null is returned if not set.
	 */
	public String getCameraPort(int index) {
		if (cameraPort == null) return null;
		return cameraPort[index];
	}

	/**
	 * Sets the directory for the images taken from this camera.
	 * Should not be modified after observation begins.
	 * @param index Camera index.
	 * @param dir Directory name. Default value is 'camerax', where
	 * x is 1+index.
	 */
	public void setCameraDir(int index, String dir) {
		this.cameraDir[index] = dir;
		if (!cameraDir[index].endsWith(FileIO.getFileSeparator())) cameraDir[index] += FileIO.getFileSeparator();
	}

	/**
	 * Sets the park position for the telescope.
	 * @param loc Park position in azimuth/elevation. Set to null for the
	 * default position of the telescope, in case it supports parking. Celestron does
	 * not support parking specifically, but a non-null location can be used to park it.
	 */
	public void setTelescopeParkPosition(LocationElement loc) {
		parkPos = loc;
	}

	/**
	 * Returns the park position for the telescope.
	 * @return Park position.
	 */
	public LocationElement getTelescopeParkPosition() { return parkPos; }

	/**
	 * Sets the minimum time between shots to allow the camera to cool down.
	 * Default is 0.
	 * @param index Index for the camera.
	 * @param seconds Time in seconds.
	 */
	public void setCameraMinimumIntervalBetweenShots(int index, int seconds) {
		cameraInterval[index] = seconds;
	}
	/**
	 * Returns the time to wait to allow the camera to cool down. Default is 0.
	 * @param index Camera index.
	 * @return Time in seconds.
	 */
	public int getCameraMinimumIntervalBetweenShots(int index) {
		return cameraInterval[index];
	}

	/**
	 * Sets the position error of the camera respect the telescope, in case the camera
	 * is piggy-backed and not perfectly aligned.
	 * Default is 0.
	 * @param index Index for the camera.
	 * @param err Position error in radians.
	 */
	public void setCameraPositionError(int index, double err) {
		cameraPosErr[index] = err;
	}
	/**
	 * Returns the position error of the camera respect the telescope, in case the camera
	 * is piggy-backed and not perfectly aligned.
	 * Default is 0.
	 * @param index Index for the camera.
	 * @return Position error in radians.
	 */
	public double getCameraPositionError(int index) {
		return cameraPosErr[index];
	}

	/**
	 * Sets the name of the directory where darks will be saved.
	 * Default value is 'dark'.
	 * Should not be modified after observation begins.
	 * @param index Index for the camera.
	 * @param dir Directory name.
	 */
	public void setDarkDir(int index, String dir) {
		this.darkDir[index] = dir;
		if (!darkDir[index].endsWith(FileIO.getFileSeparator())) darkDir[index] += FileIO.getFileSeparator();
	}

	/**
	 * Sets the name of the directory where flats will be saved.
	 * Default value is 'flat'.
	 * Should not be modified after observation begins.
	 * @param index Index for the camera.
	 * @param dir Directory name.
	 */
	public void setFlatDir(int index, String dir) {
		this.flatDir[index] = dir;
		if (!flatDir[index].endsWith(FileIO.getFileSeparator())) flatDir[index] += FileIO.getFileSeparator();
	}

	/**
	 * Sets the name of the directory where on source shots will be saved.
	 * Default value is 'on'.
	 * Should not be modified after observation begins.
	 * @param index Index for the camera.
	 * @param dir Directory name.
	 */
	public void setOnDir(int index, String dir) {
		this.onDir[index] = dir;
		if (!onDir[index].endsWith(FileIO.getFileSeparator())) onDir[index] += FileIO.getFileSeparator();
	}

	/**
	 * Sets the name of the directory where reduced frames will be saved.
	 * Default value is 'reduced'.
	 * Should not be modified after observation begins.
	 * @param index Index for the camera.
	 * @param dir Directory name.
	 */
	public void setReducedDir(int index, String dir) {
		this.reducedDir[index] = dir;
		if (!reducedDir[index].endsWith(FileIO.getFileSeparator())) reducedDir[index] += FileIO.getFileSeparator();
	}

	/**
	 * Sets the name of the directory where stacked frames will be saved.
	 * Default value is 'stacked'.
	 * Should not be modified after observation begins.
	 * @param index Index for the camera.
	 * @param dir Directory name.
	 */
	public void setStackedDir(int index, String dir) {
		this.stackedDir[index] = dir;
		if (!stackedDir[index].endsWith(FileIO.getFileSeparator())) stackedDir[index] += FileIO.getFileSeparator();
	}

	/**
	 * Sets the name of the directory where averaged frames will be saved.
	 * Default value is 'averaged'.
	 * Should not be modified after observation begins.
	 * @param index Index for the camera.
	 * @param dir Directory name.
	 */
	public void setAveragedDir(int index, String dir) {
		this.averagedDir[index] = dir;
		if (!averagedDir[index].endsWith(FileIO.getFileSeparator())) averagedDir[index] += FileIO.getFileSeparator();
	}

	/**
	 * Returns the directory name for the darks. Default value is 'dark'.
	 * Should not be modified after observation begins.
	 * @param index Index for the camera.
	 * @return Directory name for darks.
	 */
	public String getDarkDir(int index) {
		return darkDir[index];
	}

	/**
	 * Returns the directory name for the flats. Default value is 'flat'.
	 * Should not be modified after observation begins.
	 * @param index Index for the camera.
	 * @return Directory name for flats.
	 */
	public String getFlatDir(int index) {
		return flatDir[index];
	}

	/**
	 * Returns the directory name for the on source frames. Default value is 'on'.
	 * Should not be modified after observation begins.
	 * @param index Index for the camera.
	 * @return Directory name for on source frames.
	 */
	public String getOnDir(int index) {
		return onDir[index];
	}

	/**
	 * Returns the directory name for the reduced frames. Default value is 'reduced'.
	 * Should not be modified after observation begins.
	 * @param index Index for the camera.
	 * @return Directory name for reduced frames.
	 */
	public String getReducedDir(int index) {
		return reducedDir[index];
	}

	/**
	 * Returns the directory name for the stacked frames. Default value is 'stacked'.
	 * Should not be modified after observation begins.
	 * @param index Index for the camera.
	 * @return Directory name for stacked frames.
	 */
	public String getStackedDir(int index) {
		return stackedDir[index];
	}

	/**
	 * Returns the directory name for the averaged frames. Default value is 'averaged'.
	 * Should not be modified after observation begins.
	 * @param index Index for the camera.
	 * @return Directory name for stacked frames.
	 */
	public String getAveragedDir(int index) {
		return averagedDir[index];
	}

	/**
	 * Returns the telescope model.
	 * @return Telescope model.
	 */
	public TELESCOPE_MODEL getTelescope() {
		return telescope;
	}
	
	/**
	 * Sets the telescope model.
	 * @param t Telescope model.
	 */
	public void setTelescope(TELESCOPE_MODEL t) {
		telescope = t;
	}

	/**
	 * Returns the camera/s model.
	 * @return Camera/s model.
	 */
	public CAMERA_MODEL[] getCameras() {
		return cameras;
	}

	/**
	 * Returns the dome model.
	 * @return Dome model.
	 */
	public DOME_MODEL getDome() {
		return dome;
	}

	/**
	 * Returns the weather station model.
	 * @return Weather station model.
	 */
	public WEATHER_STATION_MODEL getWeatherStation() {
		return weatherStation;
	}

	/**
	 * Returns if reduction is enabled or not. By default
	 * it is enabled when working and observation directories
	 * are set correctly (which means that reduction is possible).
	 * Set to false to take shots without reducing them
	 * immediately.
	 * @return True or false.
	 */
	public boolean reductionEnabled() {
		return doReduce;
	}

	/**
	 * Returns if reduction is possible or not. It is true when
	 * working and observation directories are set correctly.
	 * @return True or false.
	 */
	public boolean reductionPossible() {
		return reducePossible;
	}

	/**
	 * Sets if reduction should be enabled or not. This method will
	 * have no effect when reduction is not possible. In case of
	 * enabling reduction and taking, for instance, a dark frame,
	 * the superdark frame will be updated immediately, as well as
	 * all previous on shots. In the first dark/flat reduction the
	 * program will ask for the method to combine the frames.
	 * @param enabled True or false.
	 */
	public void setReductionEnabled(boolean enabled) {
		if (reducePossible) doReduce = enabled;
	}

	/**
	 * Sets if automatic reduction should be enabled or not for on
	 * frames. Default is true, and this means that in each new on
	 * frame the program will search for its corresponding master
	 * dark and flat, creating a reduced on frame in case the dark
	 * exists (using the flat if it exists, but flat is not required).
	 * No stacking or combination is done since this process should
	 * start at the end of the observations (automatically in case the
	 * reduction is enabled, or manually in case of further reducing
	 * a on frame located in the reduced directory). In the first reduction
	 * of an on frame the program will ask for the ratio of frames to be
	 * stacked and then combined.
	 * @param enabled True or false.
	 */
	public void setAutoReduceOnFramesEnabled(boolean enabled) {
		this.autoReduceOnFrames = enabled;
	}

	/**
	 * Returns if on frames should be reduced automatically or not.
	 * @return True or false.
	 */
	public boolean getAutoReduceOnFramesEnabled() {
		return autoReduceOnFrames;
	}

	private int getCameraIndex(String path) {
		int camIndex = -1;
		for (int ii=0; ii<cameraDir.length; ii++) {
//			System.out.println(cameraDir[ii]+"*"+path);
			if (path.indexOf(FileIO.getFileSeparator()+cameraDir[ii]) >= 0) {
				camIndex = ii;
				break;
			}
		}
//		System.out.println(camIndex);
		return camIndex;
	}

	/**
	 * Offers a new frame to this manager. In case of a PNG/JPG file the
	 * new file is copied to the corresponding folder as a fits, using the
	 * current system time (milliseconds from 1970) to set the name of
	 * the new file. In case of a raw file the file is previously converted
	 * to PGM using DCRaw, and then to fits.
	 * @param id The identifier for the new frame: dark, flat, ... A 'test'
	 * frame is not processed.
	 * @param path The path of the new file.
	 * @param fitsHeader The fits header to be included in this image.
	 * @throws JPARSECException If an error occurs.
	 */
	public void offerFrame(IMAGE_ID id, String path, HeaderElement fitsHeader[]) throws JPARSECException {
		if (!reducePossible) return;

		int row = 0;
		//int max = Integer.parseInt(ImageHeaderElement.getByKey(fitsHeader, "IMGDEPTH").value);
		String fpath = null, newPath = null;
		int camIndex = getCameraIndex(path);
		switch (id) {
		case DARK:
			fpath = workingDir + obsDir + cameraDir[camIndex] + darkDir[camIndex];
			fpath = checkDir(fpath);
			newPath = processFrame(path, fpath, fitsHeader);

			updateTable(false);
			if (doReduce && newPath != null) {
				reduce(id, new String[] {newPath}, camIndex, null);
			} else {
				row = DataSet.getIndexContaining(lineTable, newPath);
				if (row >= 0 && lineTable[row].endsWith("0"))
					lineTable[row] = lineTable[row].substring(0, lineTable[row].lastIndexOf("0")) + "1";
			}
			break;
		case FLAT:
			fpath = workingDir + obsDir + cameraDir[camIndex] + flatDir[camIndex];
			fpath = checkDir(fpath);
			newPath = processFrame(path, fpath, null);

			updateTable(false);
			if (doReduce && newPath != null) {
				reduce(id, new String[] {newPath}, camIndex, fitsHeader);
			} else {
				row = DataSet.getIndexContaining(lineTable, newPath);
				if (row >= 0 && lineTable[row].endsWith("0"))
					lineTable[row] = lineTable[row].substring(0, lineTable[row].lastIndexOf("0")) + "1";
			}
			break;
		case ON_SOURCE:
			fpath = workingDir + obsDir + cameraDir[camIndex] + onDir[camIndex];
			fpath = checkDir(fpath);
			newPath = processFrame(path, fpath, fitsHeader);

			if (autoReduceOnFrames && newPath != null) {
				updateTable(false);
				if (doReduce) {
					reduce(id, new String[] {newPath}, camIndex, fitsHeader);
				} else {
					row = DataSet.getIndexContaining(lineTable, newPath);
					if (row >= 0 && lineTable[row].endsWith("0"))
						lineTable[row] = lineTable[row].substring(0, lineTable[row].lastIndexOf("0")) + "1";
				}
			}
			break;
		case TEST:
			newPath = processFrame(path, fpath, fitsHeader);
			break;
		default:
			throw new JPARSECException("This method should never be called for this image id value!");
		}

		updatePanel();
	}

	/**
	 * Offers a new set of frames to this manager. In case of PNG/JPG files the
	 * new files are copied to the corresponding folder as fits, using the
	 * current system time (milliseconds from 1970) to set the names of
	 * the new files. In case of raw files the files are previously converted
	 * to PGM using DCRaw, and then to fits.
	 * @param id The identifier for the new frame: dark, flat, ... A 'test'
	 * frame is not processed.
	 * @param path The path of the new files. All must belong to the same directory
	 * and header, for instance to reduce a JPG+RAW observation.
	 * @param fitsHeader The fits header to be included in these images.
	 * @param camIndex The camera index identifying the camera that produces the images.
	 * @return Paths to the new images generated before reducing, or null. Note this is not the path to the 
	 * reduced image, if any, but to each of the frames copied to the reduced directory.
	 * @throws JPARSECException If an error occurs.
	 */
	public String[] offerFrame(IMAGE_ID id, String path[], HeaderElement fitsHeader[], int camIndex) throws JPARSECException {
		if (!reducePossible) return null;

		//int max = Integer.parseInt(ImageHeaderElement.getByKey(fitsHeader, "MAXCOUNT").value);
		String fpath = null;
		String newPaths[] = new String[path.length];
		for (int i=0; i<path.length; i++) {
			switch (id) {
			case DARK:
				if (i == 0) {
					fpath = workingDir + obsDir + cameraDir[camIndex] + darkDir[camIndex];
					checkDir(fpath);
				}
				newPaths[i] = processFrame(path[i], fpath, fitsHeader);
				break;
			case FLAT:
				if (i == 0) {
					fpath = workingDir + obsDir + cameraDir[camIndex] + flatDir[camIndex];
					checkDir(fpath);
				}
				newPaths[i] = processFrame(path[i], fpath, fitsHeader);
				break;
			case ON_SOURCE:
				if (i == 0) {
					fpath = workingDir + obsDir + cameraDir[camIndex] + onDir[camIndex];
					checkDir(fpath);
				}
				newPaths[i] = processFrame(path[i], fpath, fitsHeader);
				break;
			case TEST:
				if (i == 0) {
					fpath = workingDir + obsDir + cameraDir[camIndex];
					checkDir(fpath);
				}
				if (i == path.length - 1) processFrame(path[i], fpath, fitsHeader);
				newPaths = null;
				break;
			default:
				throw new JPARSECException("This method should never be called for this image id value!");
			}
		}
		autoReduceOnFrames = true;
		if (id == IMAGE_ID.DARK || id == IMAGE_ID.FLAT || (id == IMAGE_ID.ON_SOURCE && autoReduceOnFrames)) {
			if (newPaths != null) {
				if (doReduce) {
					reduce(id, newPaths, camIndex, fitsHeader);
				} else {
					updateTable(false);
					for (int i=0; i<newPaths.length; i++) {
						int row = DataSet.getIndexContaining(lineTable, newPaths[i]);
						if (row >= 0 && lineTable[row].endsWith("0"))
							lineTable[row] = lineTable[row].substring(0, lineTable[row].lastIndexOf("0")) + "1";
					}
				}
			}
		}
		updatePanel();
		return newPaths;
	}

	private String checkDir(String p) {
		String sep = FileIO.getFileSeparator();
		p = DataSet.replaceAll(p, sep+sep, sep, true);
		File f = new File(p);
		if (!f.exists()) FileIO.createDirectories(p);
		return p;
	}

	private String processFrame(String input, String output, HeaderElement fitsHeader[]) throws JPARSECException {
		if (output == null) {
			if (input.endsWith(".png") || input.endsWith(".jpg")) {
				Picture pic = new Picture(input);
				lastImage = pic.getImage();
				lastImagePath = input;
			} else {
				if (!input.endsWith(".pgm")) executeDCRaw(input);
				String newFile = input.substring(0, input.lastIndexOf(".")) + ".pgm";
				lastImage = readPGM(newFile, true);
				lastImagePath = newFile;
			}
			lastImageWCS = null;
			lastImageAstrometry = null;
			lastImageTable = null;
			return null;
		} else {
			boolean exists = true;
			String newOutput;
			do {
				long t = System.currentTimeMillis();
				newOutput = output + t + ".fits";
				File f = new File(newOutput);
				exists = f.exists();
			} while (exists);

			int n = HeaderElement.getIndex(fitsHeader, "RAW");
			boolean raw = fitsHeader[n].getAsBoolean();

			if (!raw) {
				Picture pic = null;
				if (input.endsWith(".png") || input.endsWith(".jpg")) pic = new Picture(input);
				if (input.endsWith(".fit") || input.endsWith(".fits")) {
					try {
						FitsIO fio = new FitsIO(input);
						int data[][] = (int[][]) (fio.getHDU(0).getData()).getData();
						data = (int[][]) DataSet.flip2dArray(data, true, false, false);
						pic = new Picture(data);
					} catch (Exception exc) {
						return null;
					}
				}
				
				byte[][][] rgb = new byte[3][pic.getWidth()][pic.getHeight()];
				int bp = 8, max = FastMath.multiplyBy2ToTheX(1, bp-1);
				fitsHeader = HeaderElement.addHeaderEntry(fitsHeader, new HeaderElement("BITPIX", ""+bp, "Bits per data value"));
				fitsHeader = HeaderElement.addHeaderEntry(fitsHeader, new HeaderElement("BZERO", ""+max, "(minus) data zero value"));
				fitsHeader = HeaderElement.addHeaderEntry(fitsHeader, new HeaderElement("NAXIS1", ""+pic.getWidth(), "Image width"));
				fitsHeader = HeaderElement.addHeaderEntry(fitsHeader, new HeaderElement("NAXIS2", ""+pic.getHeight(), "Image height"));
				BufferedImage img = pic.getImage();
				for (int x=0; x<pic.getWidth(); x++) {
					int argb[] = img.getRGB(x, 0, 1, pic.getHeight(), null, 0, 1);
					for (int y=0; y<pic.getHeight(); y++) {
						rgb[0][x][y] = (byte) (((argb[y] >> 16) & 0xff)-max);
						rgb[1][x][y] = (byte) (((argb[y] >> 8) & 0xff)-max);
						rgb[2][x][y] = (byte) ((argb[y] & 0xff)-max);
					}
				}
				FitsIO fio = new FitsIO(rgb[0]);
				fio.setHeader(0, fitsHeader);
				fio.addHDU(FitsIO.createHDU(rgb[1], fitsHeader));
				fio.addHDU(FitsIO.createHDU(rgb[2], fitsHeader));
				fio.writeEntireFits(newOutput);
				byte[][] r = fio.getPicture(0, plev1, showGrid).getImageAsByteArray(0);
				byte[][] g = fio.getPicture(1, plev1, showGrid).getImageAsByteArray(1);
				byte[][] b = fio.getPicture(2, plev1, showGrid).getImageAsByteArray(2);
				pic = new Picture(r, g, b, null);
				lastImage = pic.getImage();
				lastImagePath = newOutput;
				lastImageWCS = null;
				lastImageAstrometry = null;
				lastImageTable = null;
			} else {
				BufferedImage pgm = null;
				if (input.endsWith(".fit") || input.endsWith(".fits")) {
					try {
						FitsIO fio = new FitsIO(input);
						int data[][] = (int[][]) (fio.getHDU(0).getData()).getData();
						data = (int[][]) DataSet.flip2dArray(data, true, false, false);
						pgm = new BufferedImage(data.length, data[0].length, BufferedImage.TYPE_INT_ARGB);
						WritableRaster raster = pgm.getRaster();
						for (int j=0; j<data[0].length; j++)
						{
							for (int i=0; i<data.length; i++)
							{
								raster.setSample(i, j, 0, data[i][j]);
							}
						}
					} catch (Exception exc) {
						return null;
					}
				} else {
					if (!input.endsWith("pgm")) executeDCRaw(input);
					String newFile = input.substring(0, input.lastIndexOf(".")) + ".pgm";
					//ApplicationLauncher.executeSystemCommand("cp "+newFile+" "+newOutput);
					pgm = readPGM(newFile, false);
				}
				short[][] rgb = new short[pgm.getWidth()][pgm.getHeight()];
				int bp = 16, max = FastMath.multiplyBy2ToTheX(1, bp-1);
				for (int x=0; x<pgm.getWidth(); x++) {
					for (int y=0; y<pgm.getHeight(); y++) {
						rgb[x][y] = (short) (getPixelCount(x, y, pgm) - max);
					}
				}
				fitsHeader = HeaderElement.addHeaderEntry(fitsHeader, new HeaderElement("BITPIX", ""+bp, "Bits per data value"));
				fitsHeader = HeaderElement.addHeaderEntry(fitsHeader, new HeaderElement("BZERO", ""+max, "(minus) data zero value"));
				fitsHeader = HeaderElement.addHeaderEntry(fitsHeader, new HeaderElement("NAXIS1", ""+pgm.getWidth(), "Image width"));
				fitsHeader = HeaderElement.addHeaderEntry(fitsHeader, new HeaderElement("NAXIS2", ""+pgm.getHeight(), "Image height"));
				FitsIO fio = new FitsIO(rgb);
				fio.setHeader(0, fitsHeader);
				fio.write(0, newOutput);
				lastImage = fio.getPicture(0, plev2, showGrid).getImage();
				lastImagePath = newOutput;
				lastImageWCS = null;
				lastImageAstrometry = null;
				lastImageTable = null;
			}
			return newOutput;
		}
	}

	private synchronized void reduce(IMAGE_ID imgID, String newFiles[], int camIndex, 
			HeaderElement masterHeader[]) throws JPARSECException {
		if (imgID == IMAGE_ID.REDUCED_ON) {
			stack(newFiles[0], camIndex);
			return;
		}
		if (imgID == IMAGE_ID.STACKED) {
			average(newFiles[0], camIndex);
			return;
		}
		if (imgID == IMAGE_ID.STACKED || imgID.name().startsWith("REDUCED") || imgID == IMAGE_ID.TEST) return;

		if (combineMethod == null) {
			int result = JOptionPane.showOptionDialog(null, Translate.translate(1210), Translate.translate(1211), JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
					null, //new ImageIcon(ReadFile.readImageResource(FileIO.DATA_IMAGES_DIRECTORY+"planetaryNeb_transparentOK.png")),
					COMBINATION_METHODS, COMBINATION_METHODS[0]);
			this.setCombineMethod(COMBINATION_METHOD.values()[result]);
		}

		//String path = FileIO.getDirectoryFromPath(newFiles[0]);
		String files[] = newFiles.clone(); //FileIO.getFiles(path);
		boolean done[] = new boolean[files.length];
		if (lineTable != null) {
			for (int i=0; i<files.length; i++) {
				done[i] = true;
				int row = DataSet.getIndexContaining(lineTable, files[i]);
				if (row >= 0 && lineTable[row].startsWith("false")) continue;
				if (DataSet.getIndex(newFiles, files[i]) >= 0) done[i] = false;
			}
		}
		for (int i=0; i<files.length; i++) {
			String lineTableCopy[] = null;
			if (lineTable != null) lineTableCopy = lineTable.clone();

			String name = FileIO.getFileNameFromPath(files[i]);
			if (!name.startsWith("super") && name.endsWith(".fits") && !done[i]) {
				String id = getFitsMainData(files[i]);
				boolean raw = isFitsRaw(files[i]);
				done[i] = true;
				int[][][] data = new int[1][][];
				if (!raw) data = new int[3][][];
				data[0] = getFitsData(files[i], 0, raw);
				if (!raw) {
					data[1] = getFitsData(files[i], 1, raw);
					data[2] = getFitsData(files[i], 2, raw);
				}
				int n = 1, row = -1;
				if (lineTable != null) {
					row = DataSet.getIndexContaining(lineTable, files[i]);
					if (row < 0) {
						updateTable(false);
						row = DataSet.getIndexContaining(lineTable, files[i]);
					}
					lineTable[row] = lineTable[row].substring(0, lineTable[row].lastIndexOf(SEPARATOR)+1)+"0";
				}
				HeaderElement header[] = getFitsHeader(files[i], 0);
				String combination = files[i];
				if (imgID != IMAGE_ID.ON_SOURCE) {
					for (int j=0; j<files.length; j++) {
						if (j == i) continue;
						if (lineTable != null) {
							row = DataSet.getIndexContaining(lineTable, files[j]);
							if (row >= 0 && lineTable[row].startsWith("false")) continue;
						}
						String name2 = FileIO.getFileNameFromPath(files[j]);
						if (!name2.startsWith("super") && name2.endsWith(".fits")) {
							String id2 = getFitsMainData(files[j]);
							if (id.equals(id2)) {
								done[j] = true;
								n ++;
								combination += ","+files[j];

								if (lineTable != null) {
									if (row >= 0 && lineTable[row].endsWith("false"))
										lineTable[row] = lineTable[row].substring(0, lineTable[row].lastIndexOf("false"))+"true";
								}
								
								if (combineMethod == COMBINATION_METHOD.MEAN_AVERAGE) {
									data[0] = add(data[0], getFitsData(files[j], 0, raw));
									if (!raw) {
										data[1] = add(data[1], getFitsData(files[j], 1, raw));
										data[2] = add(data[2], getFitsData(files[j], 2, raw));
									}
								}
							}
						}
					}
				}

				String outputFile = FileIO.getDirectoryFromPath(files[i])+"super_"+id+".fits";
				String masterDark = workingDir + obsDir + cameraDir[camIndex] + darkDir[camIndex] + FileIO.getFileNameFromPath(outputFile);
				String masterFlat = workingDir + obsDir + cameraDir[camIndex] + flatDir[camIndex] + FileIO.getFileNameFromPath(outputFile);
				// Search for masterDark and masterFlat images (file names) in the provided header
				if (imgID == IMAGE_ID.ON_SOURCE && masterHeader != null) {
					HeaderElement sdark = HeaderElement.getByKey(masterHeader, "SDARK");
					if (sdark != null) masterDark = FileIO.getDirectoryFromPath(masterDark) + sdark.getAsString();
					HeaderElement sflat = HeaderElement.getByKey(masterHeader, "SFLAT");
					if (sflat != null) masterFlat = FileIO.getDirectoryFromPath(masterFlat) + sflat.getAsString();
				}
				
				double sumFlat[] = new double[] {0, 0, 0};
				if (imgID != IMAGE_ID.DARK) {
					File masterD = new File(masterDark);
					if (!masterD.exists()) {
						System.out.println("Could not find master dark "+masterDark+" for image "+outputFile+". Aborting the creation of this output image.");
						masterDark = null;
						lineTable = lineTableCopy;
						continue;
					}

					if (imgID == IMAGE_ID.ON_SOURCE) {
						File masterF = new File(masterFlat);
						outputFile = workingDir + obsDir + cameraDir[camIndex] + reducedDir[camIndex] + FileIO.getFileNameFromPath(files[i]);
						this.checkDir(FileIO.getDirectoryFromPath(outputFile));
						if (!masterF.exists()) {
							int start = masterFlat.indexOf("_BULBTIME"), end = masterFlat.indexOf("_RAW");
							if (start < 0) start = masterFlat.indexOf("_TIME");
							String starts = masterFlat.substring(0, start+1);
							String ends = masterFlat.substring(end);
							String flats[] = FileIO.getFiles(FileIO.getDirectoryFromPath(masterFlat));
							masterFlat = null;
							if (flats != null) {
								for (int f=0; f<flats.length; f++) {
									if (flats[f].startsWith(starts) && flats[f].endsWith(ends)) {
										masterFlat = flats[f];
									}
								}
							}
							if (masterFlat == null) {
								System.out.println("Could not find a compatible master flat for image "+outputFile+". Since dark was found, reduction will continue.");
								if (lineTable != null)
									lineTable[row] = lineTable[row].substring(0, lineTable[row].lastIndexOf(SEPARATOR)+1)+"2";
							}
						}
					} else {
						masterFlat = null;
					}
				} else {
					masterDark = null;
				}

				if (n == 1) {
					if (imgID != IMAGE_ID.DARK && masterDark != null) {
						data[0] = subtract(data[0], getFitsData(masterDark, 0, raw), 1, raw);
						if (!raw) {
							data[1] = subtract(data[1], getFitsData(masterDark, 1, raw), 1, raw);
							data[2] = subtract(data[2], getFitsData(masterDark, 2, raw), 1, raw);
						}
					}
				} else {
					header = HeaderElement.deleteHeaderEntries(header, new String[] {"AZ", "EL", "AZ0",
							"EL0", "AZ-EFF", "EL-EFF", "DATE0", "DATE-EFF", "DATE-OBS", "TIME_JD", "DOM_AZ",
							"DOM_OPEN", "DOM_MOVI", "DOM_MODE", "TEMP", "PRES", "HUM", "TEMP_IN", "HUM_IN",
							"WIND_SP", "WIND_AZ", "RAIN"});
					switch (combineMethod) {
					case MEAN_AVERAGE:
						if (masterDark != null) data[0] = subtract(data[0], getFitsData(masterDark, 0, raw), n, raw);
						data[0] = multiply(data[0], 1.0 / n);
						if (!raw) {
							if (masterDark != null) data[1] = subtract(data[1], getFitsData(masterDark, 1, raw), n, raw);
							if (masterDark != null) data[2] = subtract(data[2], getFitsData(masterDark, 2, raw), n, raw);
							data[1] = multiply(data[1], 1.0 / n);
							data[2] = multiply(data[2], 1.0 / n);
						}
						break;
					case MEDIAN:
						String f[] = DataSet.toStringArray(combination, ",");
						ArrayList<Object> datas0 = new ArrayList<Object>();
						ArrayList<Object> datas1 = new ArrayList<Object>();
						ArrayList<Object> datas2 = new ArrayList<Object>();
						ArrayList<double[]> sum = new ArrayList<double[]>();
						int w = data[0].length, h = data[0][0].length;
						for (int i1=0; i1<f.length; i1++) {
							FitsIO fio = new FitsIO(f[i1]);
							datas0.add(fio.getData(0));
 							if (!raw) {
								datas1.add(fio.getData(1));
								datas2.add(fio.getData(2));

								if (imgID == IMAGE_ID.FLAT && f.length > 1) {
									double s1 = 0, s2 = 0, s3 = 0;
									Object d1 = datas0.get(datas0.size()-1);
									Object d2 = datas1.get(datas0.size()-1);
									Object d3 = datas2.get(datas0.size()-1);
									for (int x=0; x<w; x++) {
										for (int y=0; y<h; y++) {
											s1 += ((byte[][])d1)[x][y];
											s2 += ((byte[][])d2)[x][y];
											s3 += ((byte[][])d3)[x][y];
										}
									}
									sum.add(new double[] {s1, s2, s3});
								}
							} else {
								if (imgID == IMAGE_ID.FLAT && f.length > 1) {
									double s1 = 0;
									Object d1 = datas0.get(datas0.size()-1);
									for (int x=0; x<w; x++) {
										for (int y=0; y<h; y++) {
											s1 += ((short[][])d1)[x][y];
										}
									}
									sum.add(new double[] {s1});
								}
							}
						}
						double flatScaling = 1.0;
						for (int x=0; x<w; x++) {
							for (int y=0; y<h; y++) {
								double val[] = new double[f.length];
								if (raw) {
									for (int z=0; z<f.length; z++) {
										flatScaling = 1.0;
										if (z > 0 && sum.size() > 1) {
											flatScaling = sum.get(0)[0] / sum.get(z)[0];
										}
										val[z] = flatScaling * ((short[][])datas0.get(z))[x][y];
									}
									double median = DataSet.getKthSmallestValue (val, val.length, val.length/2);
									data[0][x][y] = (int) median;
								} else {
									for (int z=0; z<f.length; z++) {
										flatScaling = 1.0;
										if (z > 0 && sum.size() > 1) {
											flatScaling = sum.get(0)[0] / sum.get(z)[0];
										}
										val[z] = ((byte[][])datas0.get(z))[x][y];
									}
									double median = DataSet.getKthSmallestValue (val, val.length, val.length/2);
									data[0][x][y] = (int) median;

									for (int z=0; z<f.length; z++) {
										flatScaling = 1.0;
										if (z > 0 && sum.size() > 1) {
											flatScaling = sum.get(0)[1] / sum.get(z)[1];
										}
										val[z] = ((byte[][])datas1.get(z))[x][y];
									}
									median = DataSet.getKthSmallestValue (val, val.length, val.length/2);
									data[1][x][y] = (int) median;

									for (int z=0; z<f.length; z++) {
										flatScaling = 1.0;
										if (z > 0 && sum.size() > 1) {
											flatScaling = sum.get(0)[2] / sum.get(z)[2];
										}
										val[z] = ((byte[][])datas2.get(z))[x][y];
									}
									median = DataSet.getKthSmallestValue (val, val.length, val.length/2);
									data[2][x][y] = (int) median;
								}
							}
						}
						if (masterDark != null) {
							data[0] = subtract(data[0], getFitsData(masterDark, 0, raw), 1, raw);
							if (!raw) {
								data[1] = subtract(data[1], getFitsData(masterDark, 1, raw), 1, raw);
								data[2] = subtract(data[2], getFitsData(masterDark, 2, raw), 1, raw);
							}
						}


/*						int[][] data2 = new int[1][];
						if (!raw) data2 = new int[3][];
						int values1[][] = null, values2[][] = null;
						for (int i1=0; i1<data.length; i1++) {
							int values0[][] = new int[f.length][data[0].length];
							if (!raw) {
								values1 = new int[f.length][data[0].length];
								values2 = new int[f.length][data[0].length];
							}
							for (int i2=0; i2<f.length; i2++) {
								data2[0] = getFitsData(f[i2], 0, raw)[i1];
								values0[i2] = data2[0];
								if (!raw) {
									data2[1] = getFitsData(f[i2], 1, raw)[i1];
									data2[2] = getFitsData(f[i2], 2, raw)[i1];
									values1[i2] = data2[1];
									values2[i2] = data2[2];
								}
							}
							for (int i2=0; i2<data[0].length; i2++) {
								double zx[] = new double[f.length];
								for (int i3=0; i3<f.length; i3++) {
									zx[i3] = values0[i3][i2];
								}
								double median = DataSet.getKthSmallestValue (zx, zx.length, zx.length/2);
								data[0][i1][i2] = (int) median;
								if (!raw) {
									for (int i3=0; i3<f.length; i3++) {
										zx[i3] = values1[i3][i2];
									}
									median = DataSet.getKthSmallestValue (zx, zx.length, zx.length/2);
									data[1][i1][i2] = (int) median;

									for (int i3=0; i3<f.length; i3++) {
										zx[i3] = values2[i3][i2];
									}
									median = DataSet.getKthSmallestValue (zx, zx.length, zx.length/2);
									data[2][i1][i2] = (int) median;
								}
							}
						}
*/
						break;
					case MAXIMUM:
						f = DataSet.toStringArray(combination, ",");
						w = data[0].length;
						h = data[0][0].length;
						for (int i1=0; i1<f.length; i1++) {
							FitsIO fio = new FitsIO(f[i1]);
 							if (!raw) {
								byte[][] data1 = (byte[][]) fio.getData(0);
								byte[][] data2 = (byte[][]) fio.getData(1);
								byte[][] data3 = (byte[][]) fio.getData(2);
								for (int x=0; x<w; x++) {
									for (int y=0; y<h; y++) {
										if (data1[x][y] > data[0][x][y]) data[0][x][y] = data1[x][y];
										if (data2[x][y] > data[1][x][y]) data[1][x][y] = data2[x][y];
										if (data3[x][y] > data[2][x][y]) data[2][x][y] = data3[x][y];
									}
								}
							} else {
								short[][] datas = (short[][]) fio.getData(0);
								for (int x=0; x<w; x++) {
									for (int y=0; y<h; y++) {
										if (datas[x][y] > data[0][x][y]) data[0][x][y] = datas[x][y];
									}
								}
							}
						}
						if (masterDark != null) {
							data[0] = subtract(data[0], getFitsData(masterDark, 0, raw), 1, raw);
							if (!raw) {
								data[1] = subtract(data[1], getFitsData(masterDark, 1, raw), 1, raw);
								data[2] = subtract(data[2], getFitsData(masterDark, 2, raw), 1, raw);
							}
						}
						break;
					case KAPPA_SIGMA:
						f = DataSet.toStringArray(combination, ",");
						datas0 = new ArrayList<Object>();
						datas1 = new ArrayList<Object>();
						datas2 = new ArrayList<Object>();
						sum = new ArrayList<double[]>();
						w = data[0].length;
						h = data[0][0].length;
						for (int i1=0; i1<f.length; i1++) {
							FitsIO fio = new FitsIO(f[i1]);
							datas0.add(fio.getData(0));
 							if (!raw) {
								datas1.add(fio.getData(1));
								datas2.add(fio.getData(2));

								if (imgID == IMAGE_ID.FLAT && f.length > 1) {
									double s1 = 0, s2 = 0, s3 = 0;
									Object d1 = datas0.get(datas0.size()-1);
									Object d2 = datas1.get(datas0.size()-1);
									Object d3 = datas2.get(datas0.size()-1);
									for (int x=0; x<w; x++) {
										for (int y=0; y<h; y++) {
											s1 += ((byte[][])d1)[x][y];
											s2 += ((byte[][])d2)[x][y];
											s3 += ((byte[][])d3)[x][y];
										}
									}
									sum.add(new double[] {s1, s2, s3});
								}
							} else {
								if (imgID == IMAGE_ID.FLAT && f.length > 1) {
									double s1 = 0;
									Object d1 = datas0.get(datas0.size()-1);
									for (int x=0; x<w; x++) {
										for (int y=0; y<h; y++) {
											s1 += ((short[][])d1)[x][y];
										}
									}
									sum.add(new double[] {s1});
								}
							}
						}
						flatScaling = 1.0;
						for (int x=0; x<w; x++) {
							for (int y=0; y<h; y++) {
								double val[] = new double[f.length];
								if (raw) {
									for (int z=0; z<f.length; z++) {
										flatScaling = 1.0;
										if (z > 0 && sum.size() > 1) {
											flatScaling = sum.get(0)[0] / sum.get(z)[0];
										}
										val[z] = flatScaling * ((short[][])datas0.get(z))[x][y];
									}
									MeanValue mv = new MeanValue(val, null);
									double median = mv.getAverageUsingKappaSigmaClipping(COMBINATION_METHOD.kappaSigmaValue, 0);
									data[0][x][y] = (int) median;
								} else {
									for (int z=0; z<f.length; z++) {
										flatScaling = 1.0;
										if (z > 0 && sum.size() > 1) {
											flatScaling = sum.get(0)[0] / sum.get(z)[0];
										}
										val[z] = ((byte[][])datas0.get(z))[x][y];
									}
									MeanValue mv = new MeanValue(val, null);
									double median = mv.getAverageUsingKappaSigmaClipping(COMBINATION_METHOD.kappaSigmaValue, 0);
									data[0][x][y] = (int) median;

									for (int z=0; z<f.length; z++) {
										flatScaling = 1.0;
										if (z > 0 && sum.size() > 1) {
											flatScaling = sum.get(0)[1] / sum.get(z)[1];
										}
										val[z] = ((byte[][])datas1.get(z))[x][y];
									}
									mv = new MeanValue(val, null);
									median = mv.getAverageUsingKappaSigmaClipping(COMBINATION_METHOD.kappaSigmaValue, 0);
									data[1][x][y] = (int) median;

									for (int z=0; z<f.length; z++) {
										flatScaling = 1.0;
										if (z > 0 && sum.size() > 1) {
											flatScaling = sum.get(0)[2] / sum.get(z)[2];
										}
										val[z] = ((byte[][])datas2.get(z))[x][y];
									}
									mv = new MeanValue(val, null);
									median = mv.getAverageUsingKappaSigmaClipping(COMBINATION_METHOD.kappaSigmaValue, 0);
									data[2][x][y] = (int) median;
								}
							}
						}
						if (masterDark != null) {
							data[0] = subtract(data[0], getFitsData(masterDark, 0, raw), 1, raw);
							if (!raw) {
								data[1] = subtract(data[1], getFitsData(masterDark, 1, raw), 1, raw);
								data[2] = subtract(data[2], getFitsData(masterDark, 2, raw), 1, raw);
							}
						}
						break;
					}
				}

				// Apply flat field and solve WCS
				binaryTable = null;
				if (imgID == IMAGE_ID.ON_SOURCE) {
					if (masterFlat != null) {
						int[][][] data2 = new int[1][][];
						if (!raw) data2 = new int[3][][];
						data2[0] = getFitsData(masterFlat, 0, raw);
						sumFlat = new double[] {
								this.average(data2[0])
						};
						if (!raw) {
							data2[1] = getFitsData(masterFlat, 1, raw);
							data2[2] = getFitsData(masterFlat, 2, raw);
							sumFlat = new double[] {
									this.average(data2[0]),
									this.average(data2[1]),
									this.average(data2[2])
							};
						}
						for (int x=0;x<data[0].length; x++) {
							for (int y=0;y<data[0][0].length; y++) {
								data[0][x][y] = (int) (0.5 + data[0][x][y] / (data2[0][x][y] / sumFlat[0]));
								if (!raw) {
									data[1][x][y] = (int) (0.5 + data[1][x][y] / (data2[1][x][y] / sumFlat[1]));
									data[2][x][y] = (int) (0.5 + data[2][x][y] / (data2[2][x][y] / sumFlat[2]));
								}
							}
						}
					}


					if (raw) {
						header = solveWCS(DataSet.toShortArray(data[0], -FastMath.multiplyBy2ToTheX(1, 16-1)), header, raw, null, null); // RGB in separated pixels or R/G/B in CCD
					} else {
						int added[][] = new int[data[0].length][data[0][0].length];
						for (int x=0; x<added.length; x++) {
							for (int y=0; y<added[0].length; y++) {
								added[x][y] = data[0][x][y] + data[1][x][y] + data[2][x][y];
							}
						}

						// Hack header to allow 16 bit data by adding RGB channels for better photometry and astrometry
						int bp = header[HeaderElement.getIndex(header, "BITPIX")].getAsInt();
						int bz = header[HeaderElement.getIndex(header, "BZERO")].getAsInt();
						header = HeaderElement.addHeaderEntry(header, new HeaderElement("BITPIX", 16, "Bits per data value"));
						header = HeaderElement.addHeaderEntry(header, new HeaderElement("BZERO", FastMath.multiplyBy2ToTheX(1, 16-1), "(minus) data zero value"));
						header = solveWCS(DataSet.toShortArray(added, -FastMath.multiplyBy2ToTheX(1, 16-1)), header, raw, null, data); // RGB
						header[HeaderElement.getIndex(header, "BITPIX")].value = bp;
						header[HeaderElement.getIndex(header, "BZERO")].value = bz;
					}
				}


				header = HeaderElement.addHeaderEntry(header, new HeaderElement("DATE", ""+(new TimeElement()).toString(), "fits file creation date and time"));
				header = HeaderElement.addHeaderEntry(header, new HeaderElement("IMGID", "Reduced "+GenericCamera.IMAGE_IDS[imgID.ordinal()].toLowerCase(), "Image id: Dark, Flat, On, Test, or Reduced"));
				if (!raw) {
					int bp = 8, maxV = FastMath.multiplyBy2ToTheX(1, bp-1);
					FitsIO fio = new FitsIO(DataSet.toByteArray(data[0], -maxV));
					fio.setHeader(0, header);
					fio.addHDU(FitsIO.createHDU(DataSet.toByteArray(data[1], -maxV), header));
					fio.addHDU(FitsIO.createHDU(DataSet.toByteArray(data[2], -maxV), header));
					if (binaryTable != null) fio.addHDU(binaryTable);
					fio.writeEntireFits(outputFile);
				} else {
					int bp = 16, maxV = FastMath.multiplyBy2ToTheX(1, bp-1);
					FitsIO fio = new FitsIO(DataSet.toShortArray(data[0], -maxV));
					fio.setHeader(0, header);
					if (binaryTable != null) fio.addHDU(binaryTable);
					fio.writeEntireFits(outputFile);
				}
			}
		}
	}

	private HeaderElement[] getFitsHeader(String path, int n) throws JPARSECException {
		FitsIO fio = new FitsIO(path);
		return fio.getHeader(n);
	}
	private int[][] getFitsData(String path, int n, boolean raw) throws JPARSECException {
		FitsIO fio = new FitsIO(path);
		if (!raw) return DataSet.toIntArray((byte[][])fio.getData(n), 0);
		return DataSet.toIntArray((short[][]) fio.getData(n), 0);
	}
	private String getFitsMainData(String path) throws JPARSECException {
		FitsIO fio = new FitsIO(path);
		HeaderElement header[] = fio.getHeader(0);

		int n = HeaderElement.getIndex(header, "ISO");
		String out = header[n].key+header[n].getAsString()+"_";
		n = HeaderElement.getIndex(header, "TIME");
		if (!DataSet.isDoubleFastCheck(header[n].getAsString())) n = HeaderElement.getIndex(header, "BULBTIME");
		out += header[n].key+header[n].getAsString()+"_";
		n = HeaderElement.getIndex(header, "RAW");
		out += header[n].key+header[n].getAsString();
		return out;
	}
	private String getFitsMainDataWithSource(String path, boolean includeTime) throws JPARSECException {
		FitsIO fio = new FitsIO(path);
		HeaderElement header[] = fio.getHeader(0);

		int n = HeaderElement.getIndex(header, "ISO");
		String out = header[n].key+header[n].getAsString()+"_";
		if (includeTime) {
			n = HeaderElement.getIndex(header, "TIME");
			if (!DataSet.isDoubleFastCheck(header[n].getAsString())) n = HeaderElement.getIndex(header, "BULBTIME");
			out += header[n].key+header[n].getAsString()+"_";
		}
		n = HeaderElement.getIndex(header, "RAW");
		out += header[n].key+header[n].getAsString()+"_";
		n = HeaderElement.getIndex(header, "FILTER");
		out += header[n].key+header[n].getAsString()+"_";
		n = HeaderElement.getIndex(header, "OBJECT");
		out += header[n].key+header[n].getAsString()+"_";
		n = HeaderElement.getIndex(header, "IMGID");
		out += header[n].key+header[n].getAsString();
		return out;
	}
	private boolean isFitsRaw(String path) throws JPARSECException {
		FitsIO fio = new FitsIO(path);
		HeaderElement header[] = fio.getHeader(0);

		int n = HeaderElement.getIndex(header, "RAW");
		return header[n].getAsBoolean();
	}
	private int[][] add(int[][] data, int[][] data2) {
		for (int x=0; x<data.length; x++) {
			for (int y=0; y<data[0].length; y++) {
				data[x][y] += data2[x][y];
			}
		}
		return data;
	}
	private int[][] subtract(int[][] data, int[][] data2, int multFactor, boolean raw) {
		int bp = 16, minV = 0;
		if (!raw) bp = 8;
		minV = FastMath.multiplyBy2ToTheX(1, bp-1);
		for (int x=0; x<data.length; x++) {
			for (int y=0; y<data[0].length; y++) {
				if (data[x][y] > data2[x][y] || data[x][y] < 0) {
					data[x][y] -= (data2[x][y] * multFactor);
					if (data[x][y] < -minV) data[x][y] += 2*minV;
				} else {
					data[x][y] = 0;
				}
			}
		}
		return data;
	}
	private int[][] multiply(int[][] data, double v) {
		for (int x=0; x<data.length; x++) {
			for (int y=0; y<data[0].length; y++) {
				data[x][y] = (int) (data[x][y] / v + 0.5);
			}
		}
		return data;
	}
	private double average(int[][] data) {
		double average = 0;
		for (int x=0; x<data.length; x++) {
			for (int y=0; y<data[0].length; y++) {
				average += data[x][y];
			}
		}
		return average / (data.length * data[0].length);
	}

	private void stack(String file, int camIndex) throws JPARSECException {
		if (!file.endsWith(".fits")) {
			System.out.println("Cannot stack the non fits image "+file);
			return;
		}
		System.out.println("STACK");

		String path = workingDir + obsDir + cameraDir[camIndex] + reducedDir[camIndex];
		String files[] = FileIO.getFiles(path);
		String id = this.getFitsMainDataWithSource(file, false);
		for (int i=files.length-1; i>=0; i--) {
			String id2 = this.getFitsMainDataWithSource(files[i], false);
			//if (id2.indexOf(GenericCamera.IMAGE_IDS_ALL[7]) >= 0) stacked += s[i]+",";
			if (!id2.equals(id)) {
				files = DataSet.eliminateRowFromTable(files, i+1);
				continue;
			}
			int index = DataSet.getIndexContaining(lineTable, files[i]);
			if (index >= 0) {
				if (lineTable[index].toLowerCase().startsWith("false")) {
					files = DataSet.eliminateRowFromTable(files, i+1);
					continue;
				}
			}

		}

		if (files == null || files.length < 1) {
			System.out.println("There are no files to stack");
			return;
		}

		// Get list of previously stacked and enabled frames
		String s[] = FileIO.getFiles(workingDir + obsDir + cameraDir[camIndex] + stackedDir[camIndex]);
		if (s != null && s.length > 0) {
			//String stacked = "";
			for (int i=s.length-1; i>=0; i--) {
				//String id2 = this.getFitsMainDataWithSource(s[i], true);
				//if (id2.indexOf(GenericCamera.IMAGE_IDS_ALL[7]) >= 0) stacked += s[i]+",";
				//if (!id2.equals(id)) {
				//	s = DataSet.eliminateRowFromTable(s, i+1);
				//	continue;
				//}
				int index = DataSet.getIndexContaining(lineTable, s[i]);
				if (index >= 0) {
					if (lineTable[index].toLowerCase().startsWith("false")) {
						s = DataSet.eliminateRowFromTable(s, i+1);
						continue;
					}
				}

			}
		}

		// Check and remove frames already used for stacked frames in previous calls
		if (s != null && s.length > 0 && files != null && files.length >= 2) {
			//stacked = stacked.substring(0, stacked.length()-1);
			//String s[] = DataSet.toStringArray(stacked, ",");
			for (int i=0; i<s.length; i++) {
				FitsIO fio = new FitsIO(s[i]);
				HeaderElement header[] = fio.getHeader(0);
				int n = HeaderElement.getIndex(header, "STACKED");
				if (n >= 0) {
					int nf = header[n].getAsInt();
					for (int j=0; j<nf; j++) {
						n = HeaderElement.getIndex(header, "STACK"+j);
						String fn = FileIO.getFileSeparator() + header[n].getAsString();
						for (int k=files.length-1; k>= 0; k--) {
							if (files[k].endsWith(fn)) {
								files = DataSet.eliminateRowFromTable(files, k+1);
								if (files.length < 1) break;
							}
						}
						if (files.length < 1) break;
					}
					if (files.length < 1) break;
				}
			}
		}

		if (files == null || files.length < 2) {
			if (files != null && files.length == 1) {
				System.out.println("Only 1 image");
			} else {
				System.out.println("There are no new files to stack");
				return;
			}
		}

		boolean raw = isFitsRaw(file);
		HeaderElement header[] = getFitsHeader(file, 0);
		boolean exists = true;
		String outputFile;
		do {
			long t = System.currentTimeMillis();
			outputFile = workingDir + obsDir + cameraDir[camIndex] + stackedDir[camIndex] + t + ".fits";
			File f = new File(outputFile);
			exists = f.exists();
		} while (exists);
		int width = HeaderElement.getByKey(header, "NAXIS1").getAsInt();
		int height = HeaderElement.getByKey(header, "NAXIS2").getAsInt();
		if (drizzleMethod == DRIZZLE.DRIZZLE_2) {
			width *= 2;
			height *= 2;
		}
		if (drizzleMethod == DRIZZLE.DRIZZLE_3) {
			width *= 3;
			height *= 3;
		}
		if (drizzleMethod == DRIZZLE.DRIZZLE_HALF) {
			width /= 2;
			height /= 2;
		}
		header = HeaderElement.addHeaderEntry(header, new HeaderElement("NAXIS1", ""+width, "Width in pixels"));
		header = HeaderElement.addHeaderEntry(header, new HeaderElement("NAXIS2", ""+height, "Height in pixels"));
		int[][][] data = new int[1][width][height];
		if (!raw) data = new int[3][width][height];
		boolean eastLeft = true, northUp = true;
		eastLeft = (imageOrientation == IMAGE_ORIENTATION.NOT_INVERTED);
		northUp = (imageOrientation != IMAGE_ORIENTATION.INVERTED_HORIZONTALLY_AND_VERTICALLY);
		TELESCOPE_MODEL teles = getTelescope(); //TELESCOPE_MODEL.VIRTUAL_TELESCOPE_EQUATORIAL_MOUNT;
		GenericTelescope tel = new VirtualTelescope(teles);
		if (northUp && eastLeft) tel.setTelescopeType(TELESCOPE_TYPE.REFRACTOR_WITH_ERECTING_PRISM);
		if (northUp && !eastLeft) tel.setTelescopeType(TELESCOPE_TYPE.SCHMIDT_CASSEGRAIN);
		if (!northUp && eastLeft) tel.setTelescopeType(TELESCOPE_TYPE.SC_VERTICALLY_INVERTED);
		if (!northUp && !eastLeft) tel.setTelescopeType(TELESCOPE_TYPE.REFRACTOR);

		double epoch = 2000;
		if (HeaderElement.getByKey(header, "EPOCH") != null) epoch = HeaderElement.getByKey(header, "EPOCH").getAsDouble();
		FitsIO ifio[] = new FitsIO[files.length];
		for (int i=0; i<files.length; i++) {
			ifio[i] = new FitsIO(files[i]);
		}
		HeaderElement astrometry = HeaderElement.getByKey(header, "PLATE_A");
		LocationElement loc = ifio[0].getWCS(0).getSkyCoordinates(new Point2D.Double(width/2.0, height/2.0));
		if (astrometry == null) {
			double ra = HeaderElement.getByKey(header, "DECJ2000").getAsDouble() * Constant.DEG_TO_RAD;
			double dec = HeaderElement.getByKey(header, "DECJ2000").getAsDouble() * Constant.DEG_TO_RAD;
			loc = new LocationElement(ra, dec, 1);
		}
		//double saturation = Double.parseDouble(header[ImageHeaderElement.getIndex(header, "MAXADU")].value);
		double field = HeaderElement.getByKey(header, "FIELD").getAsDouble() * Constant.DEG_TO_RAD;
		//double dec = Double.parseDouble(ImageHeaderElement.getByKey(header, "DECJ2000").value);
		//double field = Double.parseDouble(ImageHeaderElement.getByKey(header, "FIELD").value) * Constant.DEG_TO_RAD;
		//LocationElement loc = new LocationElement(ra, dec, 1.0);
		boolean DLSRandRAW = false;
		if (raw) {
			String model = HeaderElement.getByKey(header, "CAM_MODE").getAsString();
			CAMERA_MODEL camModel = CAMERA_MODEL.valueOf(model);
			if (camModel.isDLSR()) {
				DLSRandRAW = true;
				data = new int[4][width][height];
			}
		}

		System.out.println("Will stack "+files.length+" frames. DLSR and RAW: "+DLSRandRAW);
		WCS wcs = new WCS(loc, width, height, field, eastLeft, northUp, epoch);
		WCS wcsList[] = new WCS[files.length];
		double time[] = new double[files.length];
		double tjd = 0;
		int notSolved = 0;
		for (int i=0; i<files.length; i++) {
			wcsList[i] = ifio[i].getWCS(0);
			wcsList[i].useSkyViewImplementation = false;
			if (i == 0) {
				wcs.setCdelt1(FastMath.sign(wcs.getCdelt1()) * Math.abs(wcsList[i].getCdelt1()));
				wcs.setCdelt2(FastMath.sign(wcs.getCdelt2()) * Math.abs(wcsList[i].getCdelt1()));
				//if (wcsList[i].getCD() != null) wcs.setCD(wcsList[i].getCD());
				//if (wcsList[i].getPC() != null) wcs.setPC(wcsList[i].getPC());
			}
			HeaderElement header0[] = ifio[i].getHeader(0);
			astrometry = HeaderElement.getByKey(header0, "PLATE_A");
			if (astrometry == null) notSolved ++;
			tjd += header0[HeaderElement.getIndex(header0, "TIME_JD")].getAsDouble();
			time[i] = header0[HeaderElement.getIndex(header0, "TIME")].getAsDouble();
		}
		boolean addWCS = true;
		if (notSolved == files.length) { // All images to be stacked are not astrometrically solved, force a default WCS
			for (int i=0; i<files.length; i++) {
				wcsList[i] = wcs;
			}
			addWCS = false;
		} else {
			if (notSolved > 0) {
				JOptionPane.showMessageDialog(null, Translate.translate(1283), Translate.translate(230), JOptionPane.ERROR_MESSAGE);
				return;
			}
		}

		tjd /= files.length;
		int interpDeg = 3;
		if (interpolationMethod == INTERPOLATION.BICUBIC) interpDeg = 2;

		StringBuffer val[][] = new StringBuffer[height][data.length];
		String dval[] = new String[height];
		LocationElement l[] = new LocationElement[height];
		ImageSplineTransform ist = null;
		int step = 1, noData = -9999, step2 = 1;
		if (DLSRandRAW) step2 = 2;
		String noDataS = ""+noData, noDataSC = noDataS+",";
		//if (DLSRandRAW) step = 2;
		for (int x=0; x<width; x += step) {
			System.out.println("Stacking column #"+x+"/"+(width-1));

			for (int y=0; y<height; y += step) {
				l[y] = wcs.getSkyCoordinates(new Point2D.Double(x+1, y+1));
				dval[y] = "";
				if (val[y][0] != null) {
					val[y][0].delete(0, val[y][0].length());
					if (!raw || DLSRandRAW) {
						val[y][1].delete(0, val[y][1].length());
						val[y][2].delete(0, val[y][2].length());
						if (data.length == 4) val[y][3].delete(0, val[y][3].length());
					}
				} else {
					val[y][0] = new StringBuffer();
					if (!raw || DLSRandRAW) {
						val[y][1] = new StringBuffer();
						val[y][2] = new StringBuffer();
						if (data.length == 4) val[y][3] = new StringBuffer();
					}
				}
			}
			for (int i=0; i<files.length; i++) {
				int wi = ifio[i].getWidth(0);
				int hi = ifio[i].getHeight(0);
				for (int index=0; index<data.length; index ++) {
/*					int offset = 0;
					if (x >= BORDER) {
						offset = x-BORDER;
						int xf = x + BORDER + 1;
						if (xf >= wi) {
							int dif = xf - wi + 1;
							offset -= dif;
						}
					}
*/

					if (DLSRandRAW && index > 0) continue;
					for (int y=0; y<height; y += step) {
						Point2D p = wcsList[i].getPixelCoordinates(l[y]);

						if (index == 0) {
							String addDVAL = noDataSC;
							if (!DLSRandRAW) {
								double dx = p.getX() - (int) p.getX();
								double dy = p.getY() - (int) p.getY();
								if (dx > 0.5 && p.getX() < width) dx = 1.0 - dx;
								if (dy > 0.5 && p.getY() < height) dy = 1.0 - dy;
								addDVAL = ""+FastMath.hypot(dx, dy)+",";
							}
							dval[y] += addDVAL;
						}

						int x0 = (int) (p.getX() - BORDER);
						int y0 = (int) (p.getY() - BORDER);
						if (x0 < 0) x0 = 0;
						if (y0 < 0) y0 = 0;
						int iv = index;
						if (DLSRandRAW) {
							int xi = (int) (p.getX() - 1);
							int yi = (int) (p.getY() - 1);
							iv = 0;
							if (xi >= 0 && yi >= 0 && xi < wi && yi < hi) {
								boolean row1 = false, col1 = false;
								if (xi/2.0 == xi/2) col1 = true;
								if (yi/2.0 == yi/2) row1 = true;
								if (!col1) iv = 1;
								if (!row1) {
									iv = 2;
									if (col1) iv = 3;
								}

								if (!col1) x0 ++;
								if (!row1) y0 ++;
							} else {
								val[y][iv].append(noDataSC);
								continue;
							}
						}

						ist = getIST(x0, y0, iv, interpDeg, DLSRandRAW, raw, height, files[i], ifio[i], 1.0);

						p.setLocation(p.getX()-x0, p.getY()-y0);
						if (raw) {
							if (!ist.isOutOfImage((p.getX()-1)/step2, (p.getY()-1)/step2)) {
								try {
									if (DLSRandRAW) {
										if (interpolationMethod == INTERPOLATION.NEAREST_NEIGHBOR) {
											val[y][iv].append(""+(float)ist.getImage()[(int) (p.getX()/2.0)][(int) (p.getY()/2.0)]+",");
										} else {
											val[y][iv].append(""+(float)ist.interpolate(p.getX()/2.0, p.getY()/2.0)+",");
										}
/*										if (interpolationMethod == INTERPOLATION.NEAREST_NEIGHBOR) {
											if (index == 0) val[y][0].append(""+(float)ist.getImage()[(int) (p.getX()/2.0)][(int) (p.getY()/2.0)]+",");
											if (index == 1) val[y][1].append(""+(float)ist.getImage()[(int) (p.getX()/2.0 - 0.5)][(int) (p.getY()/2.0)]+",");
											if (index == 2) val[y][2].append(""+(float)ist.getImage()[(int) (p.getX()/2.0 - 0.5)][(int) (p.getY()/2.0 - 0.5)]+",");
											if (index == 3) val[y][3].append(""+(float)ist.getImage()[(int) (p.getX()/2.0)][(int) (p.getY()/2.0 - 0.5)]+",");
										} else {
											if (index == 0) val[y][0].append(""+(float)ist.interpolate(p.getX()/2.0-0.5, p.getY()/2.0-0.5)+",");
											if (index == 1) val[y][1].append(""+(float)ist.interpolate(Math.max(0.0, p.getX()/2.0-1.0), p.getY()/2.0-0.5)+",");
											if (index == 2) val[y][2].append(""+(float)ist.interpolate(Math.max(0.0, p.getX()/2.0-1.0), Math.max(0.0, p.getY()/2.0-1.0))+",");
											if (index == 3) val[y][3].append(""+(float)ist.interpolate(p.getX()/2.0-0.5, Math.max(0.0, p.getY()/2.0-1.0))+",");
										}
*/
									} else {
										if (interpolationMethod == INTERPOLATION.NEAREST_NEIGHBOR) {
											if (index == 0) val[y][0].append(""+(float)ist.getImage()[(int) (p.getX() - 0.5)][(int) (p.getY() - 0.5)]+",");
										} else {
											if (index == 0) val[y][0].append(""+(float)ist.interpolate(p.getX()-1, p.getY()-1)+",");
										}
									}
								} catch (Exception exc) {
									val[y][iv].append(noDataSC);
								}
							} else {
								val[y][iv].append(noDataSC);
							}
						} else {
							if (!ist.isOutOfImage(p.getX()-1, p.getY()-1)) {
								try {
									if (interpolationMethod == INTERPOLATION.NEAREST_NEIGHBOR) {
										val[y][index].append(""+(float)ist.getImage()[(int) (p.getX() - 0.5)][(int) (p.getY() - 0.5)]+",");
									} else {
										val[y][index].append(""+(float)ist.interpolate(p.getX()-1, p.getY()-1)+",");
									}
								} catch (Exception exc) {
									val[y][index].append(noDataSC);
								}
							} else {
								val[y][index].append(noDataSC);
							}
						}
					}
				}
				if (DLSRandRAW) {
					for (int y=0; y<height; y += step) {
						if (val[y][0].length() == 0) val[y][0].append(noDataSC);
						if (val[y][1].length() == 0) val[y][1].append(noDataSC);
						if (val[y][2].length() == 0) val[y][2].append(noDataSC);
						if (val[y][3].length() == 0) val[y][3].append(noDataSC);
					}
				}
			}

			for (int y=0; y<height; y +=step) {
				double dnv[] = toDoubleValues(DataSet.toStringArray(dval[y].substring(0, dval[y].length()-1), ",", false));
				for (int i=0; i<data.length; i++) {
					double nval[] = toDoubleValues(DataSet.toStringArray(val[y][i].substring(0, val[y][i].length()-1), ",", false)), dnval[] = dnv.clone();

					int n = DataSet.getIndexOfMinimum(nval);
					if (nval[n] == noData) {
						while (true) {
							if (nval[n] != noData) break;
							nval = DataSet.deleteIndex(nval, n);
							dnval = DataSet.deleteIndex(dnval, n);
							if (nval.length < 1) break;
							n = DataSet.getIndexOfMinimum(nval);
						}
					}

					data[i][x][y] = 0;
					if (nval.length > 1) {
						double value = 0;
						for (int mi=0;mi<nval.length; mi++) {
							value += nval[mi];
						}
						data[i][x][y] = (int) (value + 0.5);
						//if (data[i][x][y] > saturation) data[i][x][y] = (int) saturation;
					} else {
						if (nval.length == 1) data[i][x][y] = (int) (nval[0] + 0.5);
					}
					//if (data[i][x][y] <= 0 && y > 800 && y < 1100 && x > 1500)
					//	System.out.println(nval.length+"/"+y+"/"+DataSet.toString(DataSet.toStringValues(nval), ",")+"/"+DataSet.toString(DataSet.toStringValues(dnval), ",")+"/"+data[i][x][y]+"/"+data[i][x][y-1]+"/"+data[i][x-1][y]);
					if (data[i][x][y] < 0) data[i][x][y] = 0;
					if (!raw && data[i][x][y] > 255) data[i][x][y] = 255;
					if (raw && data[i][x][y] > 32767) data[i][x][y] = 32767;
				}
			}
		}

		// Combine G and G bis to obtain one G
		if (DLSRandRAW) {
			HeaderElement b = HeaderElement.getByKey(header, "BAYER");
			String bayer = "RGBG";
			if (b != null) bayer = b.getAsString();
			int i0 = bayer.indexOf("G"), i1 = bayer.lastIndexOf("G");
			if (i0 >= 0 && i1 >= 0 && i0 != i1) {
				data[i0] = this.add(data[i0], data[i1]);
				data[i0] = this.multiply(data[i0], 0.5);
				int out[][][] = new int[3][data[0].length][data[0][0].length];
				out[0] = data[bayer.indexOf("R")];
				out[1] = data[i0];
				out[2] = data[bayer.indexOf("B")];
				data = out;
			}
		}

		header = HeaderElement.addHeaderEntry(header, new HeaderElement[] {
				new HeaderElement("TIME", ""+Functions.sumComponents(time), "Exposure time in s"),
				new HeaderElement("RAW", ""+false, "True for raw mode"),
				new HeaderElement("MOUNT", "EQUATORIAL", "Telescope mount"),
				new HeaderElement("ANGLE", "0", "Camera orientation"),
				new HeaderElement("TIME_JD", ""+tjd, "(Average) Date and time as JD, in UT1"),
				new HeaderElement("DATE-EFF", (new TimeElement(tjd, SCALE.UNIVERSAL_TIME_UT1)).toString(), "(Average) Date and time for the middle of the observation"),
				new HeaderElement("COMBINE", getCombineMethod().name(), "Combination method for darks/flats"),
				new HeaderElement("ORIENT", getImageOrientation().name(), "Image orientation after stack"),
				new HeaderElement("INTERP", getInterpolationMethod().name(), "Interpolation method when resampling frames"),
				new HeaderElement("DRIZZLE", getDrizzleMethod().name(), "Drizzle method")
		});

		header = HeaderElement.deleteHeaderEntries(header, DataSet.toStringArray("AZ,EL,AZ0,EL0,AZ-EFF,EL-EFF,DATE0,DOM_AZ,DOM_OPEN,DOM_MOVI,DOM_MODE,TEMP,PRES,HUM,TEMP_IN,HUM_IN,WIND_SP,WIND_AZ,RAIN", ",", false));
		if (addWCS) {
/*			if (raw && !DLSRandRAW) {
				header = solveWCS(DataSet.toShortArray(data[0], -FastMath.multiplyBy2ToTheX(1, 16-1)), header, raw, tel, null);
			} else {
				// Hack header to allow 16 bit data by adding RGB channels for better photometry and astrometry
				String bp = header[ImageHeaderElement.getIndex(header, "BITPIX")].value;
				String bz = header[ImageHeaderElement.getIndex(header, "BZERO")].value;
				header = ImageHeaderElement.addHeaderEntry(header, new ImageHeaderElement("BITPIX", "16", "Bits per data value"));
				header = ImageHeaderElement.addHeaderEntry(header, new ImageHeaderElement("BZERO", ""+FastMath.multiplyBy2ToTheX(1, 16-1), "(minus) data zero value"));
				int added[][] = new int[data[0].length][data[0][0].length];
				for (int x=0; x<added.length; x++) {
					for (int y=0; y<added[0].length; y++) {
						added[x][y] = data[0][x][y] + data[1][x][y] + data[2][x][y];
					}
				}
				header = solveWCS(DataSet.toShortArray(added, -FastMath.multiplyBy2ToTheX(1, 16-1)), header, raw, tel, data); // RGB
				header[ImageHeaderElement.getIndex(header, "BITPIX")].value = bp;
				header[ImageHeaderElement.getIndex(header, "BZERO")].value = bz;
			}
*/
			header = HeaderElement.addHeaderEntry(header, wcs.getAsHeader());
		} else {
			header = WCS.removeWCSentries(header);
		}

		header = HeaderElement.addHeaderEntry(header, new HeaderElement("DATE", ""+(new TimeElement()).toString(), "fits file creation date and time"));
		header = HeaderElement.addHeaderEntry(header, new HeaderElement("IMGID", GenericCamera.IMAGE_IDS_ALL[7], "Image id: Dark, Flat, On, Test, or Reduced"));
		header = HeaderElement.addHeaderEntry(header, new HeaderElement("STACKED", ""+files.length, "Number of source files stacked"));
		for (int i=0; i<files.length; i++) {
			header = HeaderElement.addHeaderEntry(header, new HeaderElement("STACK"+i, FileIO.getFileNameFromPath(files[i]), "Source file stacked"));
		}
		checkDir(FileIO.getDirectoryFromPath(outputFile));
		if (!raw) {
			int bp = 8, maxV = FastMath.multiplyBy2ToTheX(1, bp-1);
			FitsIO fio = new FitsIO(DataSet.toByteArray(data[0], -maxV));
			fio.setHeader(0, header);
			if (addWCS) fio.setWCS(0, wcs);
			fio.addHDU(FitsIO.createHDU(DataSet.toByteArray(data[1], -maxV), header));
			fio.addHDU(FitsIO.createHDU(DataSet.toByteArray(data[2], -maxV), header));
			if (addWCS) fio.setWCS(1, wcs);
			if (addWCS) fio.setWCS(2, wcs);
			if (binaryTable != null) fio.addHDU(binaryTable);
			fio.writeEntireFits(outputFile);
		} else {
			int bp = 16, maxV = FastMath.multiplyBy2ToTheX(1, bp-1);
			FitsIO fio = new FitsIO(DataSet.toShortArray(data[0], -maxV));
			fio.setHeader(0, header);
			if (addWCS) fio.setWCS(0, wcs);
			if (DLSRandRAW) {
				fio.addHDU(FitsIO.createHDU(DataSet.toShortArray(data[1], -maxV), header));
				if (addWCS) fio.setWCS(1, wcs);
				fio.addHDU(FitsIO.createHDU(DataSet.toShortArray(data[2], -maxV), header));
				if (addWCS) fio.setWCS(2, wcs);
				if (data.length > 3) {
					fio.addHDU(FitsIO.createHDU(DataSet.toShortArray(data[3], -maxV), header));
					if (addWCS) fio.setWCS(3, wcs);
				}
			}
			if (binaryTable != null) fio.addHDU(binaryTable);
			fio.writeEntireFits(outputFile);
		}
	}

	private void average(String file, int camIndex) throws JPARSECException {
		if (!file.endsWith(".fits")) {
			System.out.println("Cannot average the non fits image "+file);
			return;
		}
		System.out.println("AVERAGE");

		String path = workingDir + obsDir + cameraDir[camIndex] + stackedDir[camIndex];
		String files[] = FileIO.getFiles(path);
		String id = this.getFitsMainDataWithSource(file, false);
		if (files == null || files.length < 1) {
			System.out.println("There are no files to average");
			return;
		}

		// Get list of compatible files to average
		for (int i=files.length-1; i>=0; i--) {
			String id2 = this.getFitsMainDataWithSource(files[i], false);
			if (!id2.equals(id)) {
				files = DataSet.eliminateRowFromTable(files, i+1);
				continue;
			}
			int index = DataSet.getIndexContaining(lineTable, files[i]);
			if (index >= 0) {
				if (lineTable[index].toLowerCase().startsWith("false")) {
					files = DataSet.eliminateRowFromTable(files, i+1);
					continue;
				}
			}
		}

		if (files == null || files.length < 1) {
			System.out.println("There are no files to average");
			return;
		}

		// Get list of previously averaged and enabled frames
		String s[] = FileIO.getFiles(workingDir + obsDir + cameraDir[camIndex] + averagedDir[camIndex]);
		if (s != null && s.length > 0) {
			for (int i=s.length-1; i>=0; i--) {
				int index = DataSet.getIndexContaining(lineTable, s[i]);
				if (index >= 0) {
					if (lineTable[index].toLowerCase().startsWith("false")) {
						s = DataSet.eliminateRowFromTable(s, i+1);
						continue;
					}
				}

			}
		}

		// Check and remove frames already used for stacked frames in previous calls
		if (s != null && s.length > 0 && files != null && files.length >= 1) {
			for (int i=0; i<s.length; i++) {
				FitsIO fio = new FitsIO(s[i]);
				HeaderElement header[] = fio.getHeader(0);
				int n = HeaderElement.getIndex(header, "AVERAGED");
				if (n >= 0) {
					int nf = header[n].getAsInt();
					for (int j=0; j<nf; j++) {
						n = HeaderElement.getIndex(header, "AVERAG"+j);
						String fn = FileIO.getFileSeparator() + header[n].getAsString();
						for (int k=files.length-1; k>= 0; k--) {
							if (files[k].endsWith(fn)) {
								files = DataSet.eliminateRowFromTable(files, k+1);
								if (files.length < 1) break;
							}
						}
						if (files.length < 1) break;
					}
					if (files.length < 1) break;
				}
			}
		}

		if (files == null || files.length < 2) {
			if (files != null && files.length == 1) {
				System.out.println("Only 1 image");
			} else {
				System.out.println("There are no new files to average");
				return;
			}
		}

		boolean raw = isFitsRaw(file);
		HeaderElement header[] = getFitsHeader(file, 0);
		boolean exists = true;
		String outputFile;
		do {
			long t = System.currentTimeMillis();
			outputFile = workingDir + obsDir + cameraDir[camIndex] + averagedDir[camIndex] + t + ".fits";
			File f = new File(outputFile);
			exists = f.exists();
		} while (exists);
		int width = HeaderElement.getByKey(header, "NAXIS1").getAsInt();
		int height = HeaderElement.getByKey(header, "NAXIS2").getAsInt();
		int[][][] data = new int[1][width][height];
		if (!raw) data = new int[3][width][height];
		boolean eastLeft = true, northUp = true;
		eastLeft = (imageOrientation == IMAGE_ORIENTATION.NOT_INVERTED);
		northUp = (imageOrientation != IMAGE_ORIENTATION.INVERTED_HORIZONTALLY_AND_VERTICALLY);
		TELESCOPE_MODEL teles = getTelescope(); //TELESCOPE_MODEL.VIRTUAL_TELESCOPE_EQUATORIAL_MOUNT;
		GenericTelescope tel = new VirtualTelescope(teles);
		if (northUp && eastLeft) tel.setTelescopeType(TELESCOPE_TYPE.REFRACTOR_WITH_ERECTING_PRISM);
		if (northUp && !eastLeft) tel.setTelescopeType(TELESCOPE_TYPE.SCHMIDT_CASSEGRAIN);
		if (!northUp && eastLeft) tel.setTelescopeType(TELESCOPE_TYPE.SC_VERTICALLY_INVERTED);
		if (!northUp && !eastLeft) tel.setTelescopeType(TELESCOPE_TYPE.REFRACTOR);

		double epoch = 2000;
		if (HeaderElement.getByKey(header, "EPOCH") != null) epoch = HeaderElement.getByKey(header, "EPOCH").getAsDouble();
		FitsIO ifio[] = new FitsIO[files.length];
		for (int i=0; i<files.length; i++) {
			ifio[i] = new FitsIO(files[i]);
		}
		HeaderElement astrometry = HeaderElement.getByKey(header, "PLATE_A");
		LocationElement loc = ifio[0].getWCS(0).getSkyCoordinates(new Point2D.Double(width/2.0, height/2.0));
		if (astrometry == null) {
			double ra = HeaderElement.getByKey(header, "DECJ2000").getAsDouble() * Constant.DEG_TO_RAD;
			double dec = HeaderElement.getByKey(header, "DECJ2000").getAsDouble() * Constant.DEG_TO_RAD;
			loc = new LocationElement(ra, dec, 1);
		}
		//double saturation = Double.parseDouble(header[ImageHeaderElement.getIndex(header, "MAXADU")].value);
		double field = HeaderElement.getByKey(header, "FIELD").getAsDouble() * Constant.DEG_TO_RAD;
		//double dec = Double.parseDouble(ImageHeaderElement.getByKey(header, "DECJ2000").value);
		//double field = Double.parseDouble(ImageHeaderElement.getByKey(header, "FIELD").value) * Constant.DEG_TO_RAD;
		//LocationElement loc = new LocationElement(ra, dec, 1.0);
		boolean DLSRandRAW = false;
		if (raw) {
			String model = HeaderElement.getByKey(header, "CAM_MODE").getAsString();
			CAMERA_MODEL camModel = CAMERA_MODEL.valueOf(model);
			if (camModel.isDLSR()) {
				DLSRandRAW = true;
				data = new int[4][width][height];
			}
		}

		System.out.println("Will average "+files.length+" frames. DLSR and RAW: "+DLSRandRAW);
		WCS wcs = new WCS(loc, width, height, field, eastLeft, northUp, epoch);
		WCS wcsList[] = new WCS[files.length];
		double time[] = new double[files.length];
		double gain[] = new double[files.length];
		double tjd = 0;
		int notSolved = 0;
		for (int i=0; i<files.length; i++) {
			wcsList[i] = ifio[i].getWCS(0);
			wcsList[i].useSkyViewImplementation = false;
			if (i == 0) {
				wcs.setCdelt1(FastMath.sign(wcs.getCdelt1()) * Math.abs(wcsList[i].getCdelt1()));
				wcs.setCdelt2(FastMath.sign(wcs.getCdelt2()) * Math.abs(wcsList[i].getCdelt1()));
				//if (wcsList[i].getCD() != null) wcs.setCD(wcsList[i].getCD());
				//if (wcsList[i].getPC() != null) wcs.setPC(wcsList[i].getPC());
			}
			HeaderElement header0[] = ifio[i].getHeader(0);
			astrometry = HeaderElement.getByKey(header0, "PLATE_A");
			if (astrometry == null) notSolved ++;
			tjd += header0[HeaderElement.getIndex(header0, "TIME_JD")].getAsDouble();
			time[i] = header0[HeaderElement.getIndex(header0, "TIME")].getAsDouble();
			gain[i] = header0[HeaderElement.getIndex(header0, "GAIN")].getAsDouble();
		}
		double normalizeTime = -1;
		if (normalizationMethod == AVERAGE_NORMALIZATION.MINIMUM) normalizeTime = DataSet.getMinimumValue(time);
		if (normalizationMethod == AVERAGE_NORMALIZATION.MAXIMUM) normalizeTime = DataSet.getMaximumValue(time);
		boolean addWCS = true;
		if (notSolved == files.length) { // All images to be averaged are not astrometrically solved, force a default WCS
			for (int i=0; i<files.length; i++) {
				wcsList[i] = wcs;
			}
			addWCS = false;
		} else {
			if (notSolved > 0) {
				JOptionPane.showMessageDialog(null, Translate.translate(1283), Translate.translate(230), JOptionPane.ERROR_MESSAGE);
				return;
			}
		}

		tjd /= files.length;
		int interpDeg = 3;
		if (interpolationMethod == INTERPOLATION.BICUBIC) interpDeg = 2;

		StringBuffer val[][] = new StringBuffer[height][data.length];
		String dval[] = new String[height];
		LocationElement l[] = new LocationElement[height];
		ImageSplineTransform ist = null;
		int step = 1, noData = -9999, step2 = 1;
		if (DLSRandRAW) step2 = 2;
		String noDataS = ""+noData, noDataSC = noDataS+",";
		//if (DLSRandRAW) step = 2;
		boolean warnings = JPARSECException.DISABLE_WARNINGS;
		JPARSECException.DISABLE_WARNINGS = true;
		for (int x=0; x<width; x += step) {
			System.out.println("Averaging column #"+x+"/"+(width-1));

			for (int y=0; y<height; y += step) {
				l[y] = wcs.getSkyCoordinates(new Point2D.Double(x+1, y+1));
				dval[y] = "";
				if (val[y][0] != null) {
					val[y][0].delete(0, val[y][0].length());
					if (!raw || DLSRandRAW) {
						val[y][1].delete(0, val[y][1].length());
						val[y][2].delete(0, val[y][2].length());
						if (data.length == 4) val[y][3].delete(0, val[y][3].length());
					}
				} else {
					val[y][0] = new StringBuffer();
					if (!raw || DLSRandRAW) {
						val[y][1] = new StringBuffer();
						val[y][2] = new StringBuffer();
						if (data.length == 4) val[y][3] = new StringBuffer();
					}
				}
			}
			for (int i=0; i<files.length; i++) {
				int wi = ifio[i].getWidth(0);
				int hi = ifio[i].getHeight(0);
				for (int index=0; index<data.length; index ++) {
/*					int offset = 0;
					if (x >= BORDER) {
						offset = x-BORDER;
						int xf = x + BORDER + 1;
						if (xf >= wi) {
							int dif = xf - wi + 1;
							offset -= dif;
						}
					}

					ist = getIST(x, index, interpDeg, DLSRandRAW, raw, height, files[i], ifio[i], normalizeTime / time[i]);
*/
					if (DLSRandRAW && index > 0) continue;
					for (int y=0; y<height; y +=step) {
						Point2D p = wcsList[i].getPixelCoordinates(l[y]);

						if (index == 0) {
							String addDVAL = noDataSC;
							if (!DLSRandRAW) {
								double dx = p.getX() - (int) p.getX();
								double dy = p.getY() - (int) p.getY();
								if (dx > 0.5 && p.getX() < width) dx = 1.0 - dx;
								if (dy > 0.5 && p.getY() < height) dy = 1.0 - dy;
								addDVAL = ""+FastMath.hypot(dx, dy)+",";
							}
							dval[y] += addDVAL;
						}

						int x0 = (int) (p.getX() - BORDER);
						int y0 = (int) (p.getY() - BORDER);
						if (x0 < 0) x0 = 0;
						if (y0 < 0) y0 = 0;
						int iv = index;
						if (DLSRandRAW) {
							int xi = (int) (p.getX() - 1);
							int yi = (int) (p.getY() - 1);
							iv = 0;
							if (xi >= 0 && yi >= 0 && xi < wi && yi < hi) {
								boolean row1 = false, col1 = false;
								if (xi/2.0 == xi/2) col1 = true;
								if (yi/2.0 == yi/2) row1 = true;
								if (!col1) iv = 1;
								if (!row1) {
									iv = 2;
									if (col1) iv = 3;
								}

								if (!col1) x0 ++;
								if (!row1) y0 ++;
							} else {
								val[y][index].append(noDataSC);
								continue;
							}
						}
						ist = getIST(x0, y0, iv, interpDeg, DLSRandRAW, raw, height, files[i], ifio[i], 1.0);

						p.setLocation(p.getX()-x0, p.getY()-y0);
						if (raw) {
							if (!ist.isOutOfImage((p.getX()-1)/step2, (p.getY()-1)/step2)) {
								try {
									if (DLSRandRAW) {
										if (interpolationMethod == INTERPOLATION.NEAREST_NEIGHBOR) {
											val[y][iv].append(""+(float)ist.getImage()[(int) (p.getX()/2.0)][(int) (p.getY()/2.0)]+",");
										} else {
											val[y][iv].append(""+(float)ist.interpolate(p.getX()/2.0, p.getY()/2.0)+",");
										}

/*										if (interpolationMethod == INTERPOLATION.NEAREST_NEIGHBOR) {
											if (index == 0) val[y][0].append(""+(float)ist.getImage()[(int) (p.getX()/2.0)][(int) (p.getY()/2.0)]+",");
											if (index == 1) val[y][1].append(""+(float)ist.getImage()[(int) (p.getX()/2.0 - 0.5)][(int) (p.getY()/2.0)]+",");
											if (index == 2) val[y][2].append(""+(float)ist.getImage()[(int) (p.getX()/2.0 - 0.5)][(int) (p.getY()/2.0 - 0.5)]+",");
											if (index == 3) val[y][3].append(""+(float)ist.getImage()[(int) (p.getX()/2.0)][(int) (p.getY()/2.0 - 0.5)]+",");
										} else {
											if (index == 0) val[y][0].append(""+(float)ist.interpolate(p.getX()/2.0-0.5, p.getY()/2.0-0.5)+",");
											if (index == 1) val[y][1].append(""+(float)ist.interpolate(Math.max(0.0, p.getX()/2.0-1.0), p.getY()/2.0-0.5)+",");
											if (index == 2) val[y][2].append(""+(float)ist.interpolate(Math.max(0.0, p.getX()/2.0-1.0), Math.max(0.0, p.getY()/2.0-1.0))+",");
											if (index == 3) val[y][3].append(""+(float)ist.interpolate(p.getX()/2.0-0.5, Math.max(0.0, p.getY()/2.0-1.0))+",");
										}
*/									} else {
										if (interpolationMethod == INTERPOLATION.NEAREST_NEIGHBOR) {
											if (index == 0) val[y][0].append(""+(float)ist.getImage()[(int) (p.getX() - 0.5)][(int) (p.getY() - 0.5)]+",");
										} else {
											if (index == 0) val[y][0].append(""+(float)ist.interpolate(p.getX()-1, p.getY()-1)+",");
										}
									}
								} catch (Exception exc) {
									val[y][iv].append(noDataSC);
								}
							} else {
								val[y][iv].append(noDataSC);
							}
						} else {
							if (!ist.isOutOfImage(p.getX()-1, p.getY()-1)) {
								try {
									if (interpolationMethod == INTERPOLATION.NEAREST_NEIGHBOR) {
										val[y][index].append(""+(float)ist.getImage()[(int) (p.getX() - 0.5)][(int) (p.getY() - 0.5)]+",");
									} else {
										val[y][index].append(""+(float)ist.interpolate(p.getX()-1, p.getY()-1)+",");
									}
								} catch (Exception exc) {
									val[y][index].append(noDataSC);
								}
							} else {
								val[y][index].append(noDataSC);
							}
						}
					}
				}
				if (DLSRandRAW) {
					for (int y=0; y<height; y += step) {
						if (val[y][0].length() == 0) val[y][0].append(noDataSC);
						if (val[y][1].length() == 0) val[y][1].append(noDataSC);
						if (val[y][2].length() == 0) val[y][2].append(noDataSC);
						if (val[y][3].length() == 0) val[y][3].append(noDataSC);
					}
				}
			}

			for (int y=0; y<height; y += step) {
				double dnv[] = toDoubleValues(DataSet.toStringArray(dval[y].substring(0, dval[y].length()-1), ",", false));
				for (int i=0; i<data.length; i++) {
					double nval[] = toDoubleValues(DataSet.toStringArray(val[y][i].substring(0, val[y][i].length()-1), ",", false)), dnval[] = dnv.clone();

					int n = DataSet.getIndexOfMinimum(nval);
					if (nval[n] == noData) {
						while (true) {
							if (nval[n] != noData) break;
							nval = DataSet.deleteIndex(nval, n);
							dnval = DataSet.deleteIndex(dnval, n);
							if (nval.length < 1) break;
							n = DataSet.getIndexOfMinimum(nval);
						}
					}

					data[i][x][y] = 0;
					if (nval.length > 1) {
						if (averageMethod == AVERAGE_METHOD.CLOSEST_POINT) {
							int index = DataSet.getIndexOfMinimum(dnval);
							data[i][x][y] = (int) (nval[index] + 0.5);
						} else {
							MeanValue mv = new MeanValue(nval, dnval);
							if (averageMethod == AVERAGE_METHOD.PONDERATION) {
								mv.ponderate();
								data[i][x][y] = (int) (mv.getMeanValue() + 0.5);
							} else {
								if (combineMethod == COMBINATION_METHOD.MEAN_AVERAGE) {
									data[i][x][y] = (int) (mv.getAverageValue() + 0.5);
								} else {
									if (combineMethod == COMBINATION_METHOD.MEDIAN) {
										data[i][x][y] = (int) (mv.getMedian() + 0.5);
									} else {
										if (combineMethod == COMBINATION_METHOD.MAXIMUM) {
											data[i][x][y] = (int) (DataSet.getMaximumValue(nval) + 0.5);
										} else {
											data[i][x][y] = (int) (mv.getAverageUsingKappaSigmaClipping(COMBINATION_METHOD.kappaSigmaValue, 0) + 0.5);
										}
									}
								}
							}
						}
					} else {
						if (nval.length == 1) data[i][x][y] = (int) (nval[0] + 0.5);
					}
					//if (data[i][x][y] <= 0 && y > 800 && y < 1100 && x > 1500)
					//	System.out.println(nval.length+"/"+y+"/"+DataSet.toString(DataSet.toStringValues(nval), ",")+"/"+DataSet.toString(DataSet.toStringValues(dnval), ",")+"/"+data[i][x][y]+"/"+data[i][x][y-1]+"/"+data[i][x-1][y]);
					if (data[i][x][y] < 0) data[i][x][y] = 0;
					if (!raw && data[i][x][y] > 255) data[i][x][y] = 255;
					if (raw && data[i][x][y] > 32767) data[i][x][y] = 32767;
				}
			}
		}

		double texp = normalizeTime;
		if (normalizeTime < 0) texp = Functions.sumComponents(time) / time.length;
		header = HeaderElement.addHeaderEntry(header, new HeaderElement[] {
				new HeaderElement("TIME", ""+texp, "Exposure time in s"),
				new HeaderElement("RAW", ""+false, "True for raw mode"),
				new HeaderElement("MOUNT", "EQUATORIAL", "Telescope mount"),
				new HeaderElement("ANGLE", "0", "Camera orientation"),
				new HeaderElement("TIME_JD", ""+tjd, "(Average) Date and time as JD, in UT1"),
				new HeaderElement("DATE-EFF", (new TimeElement(tjd, SCALE.UNIVERSAL_TIME_UT1)).toString(), "(Average) Date and time for the middle of the observation"),
				new HeaderElement("COMBINE", getCombineMethod().name(), "Combination method for darks/flats"),
				new HeaderElement("ORIENT", getImageOrientation().name(), "Image orientation after stack"),
				new HeaderElement("AVERAGE", getAverageMethod().name(), "Average method"),
				new HeaderElement("INTERP", getInterpolationMethod().name(), "Interpolation method when resampling frames"),
				new HeaderElement("DRIZZLE", getDrizzleMethod().name(), "Drizzle method"),
				new HeaderElement("GAIN", ""+Functions.sumComponents(gain), "Gain e-/ADU")
		});

		header = HeaderElement.deleteHeaderEntries(header, DataSet.toStringArray("AZ,EL,AZ0,EL0,AZ-EFF,EL-EFF,DATE0,DOM_AZ,DOM_OPEN,DOM_MOVI,DOM_MODE,TEMP,PRES,HUM,TEMP_IN,HUM_IN,WIND_SP,WIND_AZ,RAIN", ",", false));
		if (addWCS) {
/*			if (raw && !DLSRandRAW) {
				header = solveWCS(DataSet.toShortArray(data[0], -FastMath.multiplyBy2ToTheX(1, 16-1)), header, raw, tel, null);
			} else {
				int added[][] = new int[data[0].length][data[0][0].length];
				for (int x=0; x<added.length; x++) {
					for (int y=0; y<added[0].length; y++) {
						added[x][y] = data[0][x][y] + data[1][x][y] + data[2][x][y];
					}
				}

				// Hack header to allow 16 bit data by adding RGB channels for better photometry and astrometry
				String bp = header[ImageHeaderElement.getIndex(header, "BITPIX")].value;
				String bz = header[ImageHeaderElement.getIndex(header, "BZERO")].value;
				header = ImageHeaderElement.addHeaderEntry(header, new ImageHeaderElement("BITPIX", "16", "Bits per data value"));
				header = ImageHeaderElement.addHeaderEntry(header, new ImageHeaderElement("BZERO", ""+FastMath.multiplyBy2ToTheX(1, 16-1), "(minus) data zero value"));
				header = solveWCS(DataSet.toShortArray(added, -FastMath.multiplyBy2ToTheX(1, 16-1)), header, raw, tel, data); // RGB
				header[ImageHeaderElement.getIndex(header, "BITPIX")].value = bp;
				header[ImageHeaderElement.getIndex(header, "BZERO")].value = bz;
			}
*/
			header = HeaderElement.addHeaderEntry(header, wcs.getAsHeader());
		} else {
			header = WCS.removeWCSentries(header);
		}

		header = HeaderElement.addHeaderEntry(header, new HeaderElement("DATE", ""+(new TimeElement()).toString(), "fits file creation date and time"));
		header = HeaderElement.addHeaderEntry(header, new HeaderElement("IMGID", GenericCamera.IMAGE_IDS_ALL[8], "Image id: Dark, Flat, On, Test, or Reduced"));
		header = HeaderElement.addHeaderEntry(header, new HeaderElement("AVERAGED", ""+files.length, "Number of source files averaged"));
		for (int i=0; i<files.length; i++) {
			header = HeaderElement.addHeaderEntry(header, new HeaderElement("AVERAG"+i, FileIO.getFileNameFromPath(files[i]), "Source file averaged"));
		}
		checkDir(FileIO.getDirectoryFromPath(outputFile));
		JPARSECException.DISABLE_WARNINGS = warnings;
		if (!raw) {
			int bp = 8, maxV = FastMath.multiplyBy2ToTheX(1, bp-1);
			FitsIO fio = new FitsIO(DataSet.toByteArray(data[0], -maxV));
			fio.setHeader(0, header);
			if (addWCS) fio.setWCS(0, wcs);
			fio.addHDU(FitsIO.createHDU(DataSet.toByteArray(data[1], -maxV), header));
			fio.addHDU(FitsIO.createHDU(DataSet.toByteArray(data[2], -maxV), header));
			if (addWCS) fio.setWCS(1, wcs);
			if (addWCS) fio.setWCS(2, wcs);
			if (binaryTable != null) fio.addHDU(binaryTable);
			fio.writeEntireFits(outputFile);
		} else {
			int bp = 16, maxV = FastMath.multiplyBy2ToTheX(1, bp-1);
			FitsIO fio = new FitsIO(DataSet.toShortArray(data[0], -maxV));
			fio.setHeader(0, header);
			if (addWCS) fio.setWCS(0, wcs);
			if (DLSRandRAW) {
				fio.addHDU(FitsIO.createHDU(DataSet.toShortArray(data[1], -maxV), header));
				if (addWCS) fio.setWCS(1, wcs);
				fio.addHDU(FitsIO.createHDU(DataSet.toShortArray(data[2], -maxV), header));
				if (addWCS) fio.setWCS(2, wcs);
				if (data.length > 3) {
					fio.addHDU(FitsIO.createHDU(DataSet.toShortArray(data[3], -maxV), header));
					if (addWCS) fio.setWCS(3, wcs);
				}
			}
			if (binaryTable != null) fio.addHDU(binaryTable);
			fio.writeEntireFits(outputFile);
		}
	}

	// RG
	// GB
	private double[][] getImageColor(double img[][], int offx, int offy) {
		int w = img.length, h = img[0].length;
		double out[][] = new double[w/2][h/2];
		int ix = -1;
		offx = 0;
		offy = 0;
		for (int i=offx; i<w; i = i + 2) {
			ix ++;
			if (ix < out.length) {
				int iy = -1;
				for (int j=offy; j<h; j = j + 2) {
					iy ++;
					if (iy < out[0].length) out[ix][iy] = img[i][j];
				}
			}
		}
		return out;
	}

	private static final int BORDER = 4;
	private ImageSplineTransform getIST(int x0, int y0, int index, int interpDeg, boolean DLSRandRAW, boolean raw, int height, String file, FitsIO ifio, double iscale) throws JPARSECException {
		int plain = raw ? 0: index;
		double[][] img = ifio.getDataAsDoubleArray(plain, x0, x0+2*BORDER, y0, y0+2*BORDER);
		if (iscale > 0 && iscale != 1.0) {
			for (int i=0; i<img.length; i++) {
				for (int j=0; j<img[0].length; j++) {
					img[i][j] *= iscale;
				}
			}
		}
		if (DLSRandRAW) {
			if (index == 0) img = getImageColor(img, 0, 0); // R
			if (index == 1) img = getImageColor(img, 1, 0); // G
			if (index == 2) img = getImageColor(img, 1, 1); // B
			if (index == 3) img = getImageColor(img, 0, 1); // G bis
		}
		ImageSplineTransform ist = new ImageSplineTransform(interpDeg, img);
		return ist;
	}

	private HeaderElement[] solveWCS(Object data, HeaderElement header[], boolean raw, GenericTelescope tel, Object dataRGB) {
		try {
			// First get a star catalog around the image coordinates
			double ra = header[HeaderElement.getIndex(header, "RA")].getAsDouble();
			double dec = header[HeaderElement.getIndex(header, "DEC")].getAsDouble();
			LocationElement eq = new LocationElement(ra, dec, 2062650);
			double field = header[HeaderElement.getIndex(header, "FIELD")].getAsDouble() * Constant.DEG_TO_RAD;
			int w = header[HeaderElement.getIndex(header, "NAXIS1")].getAsInt();
			int h = header[HeaderElement.getIndex(header, "NAXIS2")].getAsInt();
			double orientation  = header[HeaderElement.getIndex(header, "ANGLE")].getAsDouble();
			double maglim = 15;
			String mount = header[HeaderElement.getIndex(header, "MOUNT")].getAsString();
			MOUNT m = MOUNT.EQUATORIAL;
			if (!mount.equals("EQUATORIAL")) m = MOUNT.AZIMUTHAL;
			//double jdut1  = Double.parseDouble(header[ImageHeaderElement.getIndex(header, "TIME_JD")].value);
			//TimeElement time = new TimeElement(jdut1, SCALE.UNIVERSAL_TIME_UT1);
			TimeElement time = new TimeElement(header[HeaderElement.getIndex(header, "DATE-EFF")].getAsString());
			EphemerisElement eph = new EphemerisElement(TARGET.NOT_A_PLANET, EphemerisElement.COORDINATES_TYPE.APPARENT,
					EphemerisElement.EQUINOX_OF_DATE, EphemerisElement.TOPOCENTRIC, EphemerisElement.REDUCTION_METHOD.IAU_2006,
					EphemerisElement.FRAME.DYNAMICAL_EQUINOX_J2000,	EphemerisElement.ALGORITHM.MOSHIER);
			eph.correctForEOP = false;
			eph.correctForPolarMotion = false;
			eph.preferPrecisionInEphemerides = false;
			double tz  = 0.0;
			try { tz = header[HeaderElement.getIndex(header, "OBS_TZ")].getAsDouble(); } catch (Exception exc) {}
			String dst = DST_RULE.NONE.name();
			try { dst = header[HeaderElement.getIndex(header, "OBS_DST")].getAsString(); } catch (Exception exc) {}
			ObserverElement obs = new ObserverElement(
					header[HeaderElement.getIndex(header, "OBS_NAME")].getAsString(),
					header[HeaderElement.getIndex(header, "OBS_LON")].getAsDouble() * Constant.DEG_TO_RAD,
					header[HeaderElement.getIndex(header, "OBS_LAT")].getAsDouble() * Constant.DEG_TO_RAD,
					0,
					tz,
					DST_RULE.valueOf(dst)
			);

			// Now get a list of sources in the image
			FitsIO fio = new FitsIO(data);
			fio.setHeader(0, header);
			int minArea = this.minArea, sigma = this.sigma;
			if (!raw) { // reduce spurious star detections in jpg mode. FIXME: maybe eliminate this ?
				minArea = 8;
				sigma = 10;
			}

			SExtractor sex = fio.solveSources(0, minArea, sigma);
			System.out.println("sextractor nstars: "+sex.getNumberOfSources());
			String out[] = DataSet.toStringArray(sex.toString(), FileIO.getLineSeparator());
			ArrayList<String> list = new ArrayList<String>();
			for (int i=2; i<out.length; i++) {
				String line = FileIO.getField(1, out[i], " ", true) + " " + FileIO.getField(3, out[i], " ", true) + " " + FileIO.getField(6, out[i], " ", true);
				double cl = Double.parseDouble(FileIO.getField(8, out[i], " ", true));
				if (!list.contains(line) && cl >= minValueObjType) // cl > 5 => Only stars
					list.add(line);
			}
			String sources[] = DataSet.arrayListToStringArray(list);
			System.out.println("unique sextractor nstars: "+sources.length);
			int nsources = sources.length;
			if (maxSources > 0 && nsources > maxSources) nsources = maxSources;

			// Now get catalog sources
			if (tel == null) {
				if (telescope.isMeade()) tel = new MeadeTelescope(telescope, telescopePort);
				if (telescope.isCelestron()) tel = new CelestronTelescope(telescope, telescopePort);
				if (telescope.isVirtual()) tel = new VirtualTelescope(telescope);
				tel.setTelescopeType(getTelescopeType());
			}
			double cameraPosErr = 0;
			int cami = HeaderElement.getIndex(header, "CAMPOSER"); // Camera may be piggy-backed and not perfectly aligned
			if (cami >= 0) cameraPosErr = header[cami].getAsDouble();
			float factor = (float) (1.0 + (cameraPosErr + telescope.getExpectedErrorWhenCenteringObjects()) / field); // Consider a possible pointing error
			if (factor > 1) {
//				factor = Math.min(factor, 1.5f);
				field *= factor;
				w *= factor;
				h *= factor;
			}
			// Adjust catalog density slightly above sources
			int nstars = (int) (sources.length * factor * factor);
			nstars = Math.max((int) (nstars*1.2), nstars + 10);
/*			TimeElement time2 = new TimeElement(Constant.J2000, SCALE.BARYCENTRIC_DYNAMICAL_TIME);
			EphemerisElement eph2 = eph.clone();
			eph2.ephemType = COORDINATES_TYPE.ASTROMETRIC;
			eph2.isTopocentric = false;
			eq = Ephem.toMeanEquatorialJ2000(eq, time, obs, eph);
			String catalog[] = VirtualCamera.getStarCatalog(eq, field, w, h, orientation, m, maglim, tel, time2, obs, eph2, nstars);
*/
			String catalog[] = VirtualCamera.getStarCatalog(eq, field, w, h, orientation, m, maglim, tel, time, obs, eph, nstars, true);

			System.out.println("Catalog");
			ConsoleReport.stringArrayReport(catalog);

			System.out.println("Sources");
			ConsoleReport.stringArrayReport(sources);

			// Now we have to correlate sources with catalog to derive WCS
			if (catalog.length < 4 || sources.length < 4) return header;

			String sep = ",", seps = " ";
			double sourceX[] = DataSet.toDoubleValues(DataSet.extractColumnFromTable(sources, seps, 0));
			double sourceY[] = DataSet.toDoubleValues(DataSet.extractColumnFromTable(sources, seps, 1));
			double sourceFlux[] = DataSet.toDoubleValues(DataSet.extractColumnFromTable(sources, seps, 2));
			double catalogX[] = DataSet.toDoubleValues(DataSet.extractColumnFromTable(catalog, sep, 0));
			double catalogY[] = DataSet.toDoubleValues(DataSet.extractColumnFromTable(catalog, sep, 1));
			double catalogMag[] = DataSet.toDoubleValues(DataSet.extractColumnFromTable(catalog, sep, 2));
			double catalogRA[] = DataSet.toDoubleValues(DataSet.extractColumnFromTable(catalog, sep, 3));
			double catalogDEC[] = DataSet.toDoubleValues(DataSet.extractColumnFromTable(catalog, sep, 4));
			String catalogVar[] = DataSet.extractColumnFromTable(catalog, sep, 5);
			String catalogSp[] = DataSet.extractColumnFromTable(catalog, sep, 6);
			String catalogName[] = DataSet.extractColumnFromTable(catalog, sep, 7);
			int id[] = new int[nsources]; // Identify index id[...] with catalog
			for (int i = 0; i < id.length; i++) { id[i] = -1; }
			Astrometry preliminarAstrometry = null;

			// Make first triangle from brightest source
			double seeing = Math.max(5, 3 * field * Constant.RAD_TO_ARCSEC / w); // Seeing = 5" or 3 px of error
			double maxError = seeing / (field * Constant.RAD_TO_ARCSEC / w); // number of pixels in 5" (approx. seeing)
			System.out.println("Maximum error when identifying stars in px "+maxError);
			int tri = 0, iter = 0;
			this.scale = 0;
			this.angle = 0;
			this.nobs = 0;
			int ntrisolved = 0;
			w /= factor;
			h /= factor;
			field /= factor;
			while (true) {
				if (id[tri] < 0 || id[tri+1] < 0 || id[tri+2] < 0) {
					double max = 0, length[] = new double[3], angle = -1;
					for (int i=tri; i<tri+3; i++) {
						int i2 = i + 1;
						if (i == tri + 2) i2 = tri;
						double dx = sourceX[i] - sourceX[i2];
						double dy = sourceY[i] - sourceY[i2];
						length[i-tri] = FastMath.hypot(dx, dy);
						if (length[i-tri] > max) max = length[i-tri];
						if (i == tri) angle = FastMath.atan2_accurate(dy, dx);
					}
					length[0] = length[0] / max;
					length[1] = length[1] / max;
					length[2] = length[2] / max;

					double copy[] = new double[] {this.nobs, this.scale, this.angle};
					int tr[][] = findTriangle(catalogX, catalogY, length, maxError, id, tri, max, angle);
					if (tr != null && tr.length == 1) {
						int lastID[] = id.clone();
						id[tri] = tr[0][0];
						id[tri+1] = tr[0][1];
						id[tri+2] = tr[0][2];

						ntrisolved ++;
						if (ntrisolved > 1) {

							int nsolved = 0;
							for (int i=0; i<id.length; i++) { if (id[i] >= 0) nsolved ++; }
							LocationElement loc[] = new LocationElement[nsolved];
							Point2D p[] = new Point2D[nsolved];
							int index = 0;
							double minDist = -1;
							int centralStar = -1;
							for (int i=0; i<id.length; i++) {
								if (id[i] >= 0) {
									loc[index] = new LocationElement(catalogRA[id[i]], catalogDEC[id[i]], 1.0);
									p[index] = new Point2D.Double(sourceX[i], sourceY[i]);
									double d = FastMath.hypot(w/2.0-sourceX[i], h/2.0-sourceY[i]);
									if (d < minDist || minDist == -1) {
										minDist = d;
										centralStar = index;
									}
									index ++;
								}
							}
							preliminarAstrometry = new Astrometry(loc[centralStar], loc, p);
							double res[] = preliminarAstrometry.getPlatePositionResidual();
							if (res[0] > seeing * Constant.ARCSEC_TO_RAD || res[1] > seeing * Constant.ARCSEC_TO_RAD) {
								if (ntrisolved <= 2) {
									// Discard all previously solved triangles
									System.out.println("Discarding all triangles");
									for (int i=0; i<id.length; i++) { id[i] = -1; }
									ntrisolved = 0;
									this.scale = 0;
									this.angle = 0;
									this.nobs = 0;
								} else {
									// Discard only the last triangle
									System.out.println("Discarding last triangles");
									id = lastID;
									ntrisolved --;
									this.nobs = copy[0];
									this.scale = copy[1];
									this.angle = copy[2];
								}
							}
						}
					}
				}
				if (tri + 3 >= id.length) {
					if (iter >= 10) break;
					boolean nextIter = false;
					for (int i=0; i<id.length; i++) {
						if (id[i] < 0) {
							nextIter = true;
							break;
						}
					}
					if (!nextIter) break; // All stars identified
					iter ++;
					tri = -1;
				}
				tri ++;
			}

			// Solve WCS and photometry
			int nsolved = 0;
			for (int i=0; i<id.length; i++) {
				if (id[i] >= 0) {
					System.out.println("Source star #"+i+" ("+sources[i]+") is identified with catalog star #"+id[i]+" ("+catalog[id[i]]+")");
					nsolved ++;
				}
			}
			if (nsolved < 4) return header;

			double solvedX[] = new double[sources.length];
			double solvedY[] = new double[sources.length];
			double solvedMag[] = new double[sources.length];
			double solvedFlux[] = new double[sources.length];
			double solvedRA[] = new double[sources.length];
			double solvedDEC[] = new double[sources.length];
			String solvedVar[] = new String[sources.length];
			String solvedSp[] = new String[sources.length];
			String solvedName[] = new String[sources.length];
			int index = 0;
			int centralStar = -1;
			double minDist = -1;
			int novar = 0, var = 0;
			for (int i=0; i<id.length; i++) {
				if (id[i] >= 0) {
					solvedX[index] = sourceX[i];
					solvedY[index] = sourceY[i];
					solvedMag[index] = catalogMag[id[i]];
					solvedSp[index] = catalogSp[id[i]];
					solvedName[index] = catalogName[id[i]];
					solvedFlux[index] = sourceFlux[i];
					solvedVar[index] = catalogVar[id[i]];
					LocationElement loc = new LocationElement(catalogRA[id[i]], catalogDEC[id[i]], 2062650);
					loc = Ephem.toMeanEquatorialJ2000(loc, time, obs, eph);
					solvedRA[index] = loc.getLongitude();
					solvedDEC[index] = loc.getLatitude();
					//solvedRA[index] = catalogRA[id[i]];
					//solvedDEC[index] = catalogDEC[id[i]];
					if (solvedVar[index].equals("N")) novar ++;
					if (solvedVar[index].equals("V")) var ++;

					double d = FastMath.hypot(w/2.0-solvedX[index], h/2.0-solvedY[index]);
					if (d < minDist || minDist == -1) {
						minDist = d;
						centralStar = index;
					}
					index ++;
				}
			}
			for (int i=0; i<id.length; i++) {
				if (i >= id.length || id[i] < 0) {
					solvedX[index] = sourceX[i];
					solvedY[index] = sourceY[i];
					solvedMag[index] = 100;
					solvedSp[index] = "";
					solvedName[index] = "";
					solvedFlux[index] = sourceFlux[i];
					solvedVar[index] = "";
					solvedRA[index] = -1;
					solvedDEC[index] = -1;
					index ++;
				}
			}

			LocationElement loc[] = new LocationElement[nsolved];
			Point2D p[] = new Point2D[nsolved];
			for (int i=0; i<nsolved; i++) {
				loc[i] = new LocationElement(solvedRA[i], solvedDEC[i], 1.0);
				p[i] = new Point2D.Double(solvedX[i], solvedY[i]);
				System.out.println(p[i].getX()+", "+p[i].getY()+", "+loc[i].toStringAsEquatorialLocation());
			}

			// Do astrometric fit and eliminate all possible bad stars to repeat the astrometry if necessary
			LocationElement loc0;
			Astrometry astrometry = null;
			double res[] = null;
			while (true) {
				loc0 = loc[centralStar];
				nsolved = loc.length;
				astrometry = new Astrometry(loc0, loc, p);
				res = astrometry.getResiduals();
				for (int i=0; i<res.length; i++) {
					res[i] = Math.abs(res[i]);
				}
				double sumErrRA = Functions.sumComponents(DataSet.getSubArray(res, 0, nsolved-1));
				double sumErrDEC = Functions.sumComponents(DataSet.getSubArray(res, nsolved, 2*nsolved-1));
				int removeIndex = -1;
				for (int i=0; i<nsolved; i++) {
					if (res[i] > sumErrRA * 0.3) {
						System.out.println("RA: "+res[i]+"/"+sumErrRA);
						removeIndex = i;
						break;
					}
					if (res[nsolved+i] > sumErrDEC * 0.3) {
						System.out.println("DEC: "+res[nsolved+i]+"/"+sumErrDEC);
						removeIndex = i;
						break;
					}
				}
				if (removeIndex == -1 || loc.length <= 4) break;

				System.out.println("Removing star "+removeIndex+" from the astrometric fit");
				loc = (LocationElement[]) DataSet.deleteIndex(loc, removeIndex);
				p = (Point2D[]) DataSet.deleteIndex(p, removeIndex);

				if (centralStar >= loc.length) centralStar = 0;
			}

			ConsoleReport.doubleArrayReport(res, "f3.6");

			double c[] = astrometry.getPlateConstants();
			WCS wcs = astrometry.getAsWCS(true);
			double epoch = time.astroDate.getYear() + (time.astroDate.getMonth() - 1.0 + time.astroDate.getDay() / 30.0) / 12.0;
			wcs.setEpoch(epoch);
			wcs.setEquinox(2000);
			res = astrometry.getPlatePositionResidual();
			System.out.println("RA residual (\"):  "+res[0]*Constant.RAD_TO_ARCSEC);
			System.out.println("DEC residual (\"): "+res[1]*Constant.RAD_TO_ARCSEC);
			HeaderElement header2[] = wcs.getAsHeader();
			header2 = HeaderElement.deleteHeaderEntries(header2, new String[] {"NAXIS1", "NAXIS2"});
			double mjdUT = HeaderElement.getByKey(header, "TIME_JD").getAsDouble() - Constant.JD_MINUS_MJD;
			header2 = HeaderElement.addHeaderEntry(header2, new HeaderElement[] {
					new HeaderElement("MINAREA", ""+this.minArea, "SExtractor minimum detection area"),
					new HeaderElement("SIGMA", ""+this.sigma, "SExtractor sigma for detection"),
					new HeaderElement("OBJTYPE", ""+this.minValueObjType, "SExtractor minimum object type (close to 0 are extended sources, close to 1 for stars)"),
					new HeaderElement("MAXSOU", ""+this.maxSources, "SExtractor max number sources for photometry/astrometry"),
					new HeaderElement("RADESYS", "", "Coordinate frame"),
					new HeaderElement("MJD-OBS", ""+mjdUT, "Modified Julian day of start of observation"),
					new HeaderElement("TIMESYS", "UT", "Time scale for MJD-OBS"),
					new HeaderElement("PLATE_A", ""+c[0], "PLATE A SOLUTION"),
					new HeaderElement("PLATE_B", ""+c[1], "PLATE B SOLUTION"),
					new HeaderElement("PLATE_C", ""+c[2], "PLATE C SOLUTION"),
					new HeaderElement("PLATE_D", ""+c[3], "PLATE D SOLUTION"),
					new HeaderElement("PLATE_E", ""+c[4], "PLATE E SOLUTION"),
					new HeaderElement("PLATE_F", ""+c[5], "PLATE F SOLUTION"),
					new HeaderElement("PLATE_G", ""+loc0.getLongitude(), "PLATE REFERENCE LONGITUDE"),
					new HeaderElement("PLATE_H", ""+loc0.getLatitude(), "PLATE REFERENCE LATITUDE"),
					new HeaderElement("PLATE_I", ""+(res[0]*Constant.RAD_TO_ARCSEC), "PLATE FIT RESIDUAL IN RA (ARCSEC)"),
					new HeaderElement("PLATE_J", ""+(res[1]*Constant.RAD_TO_ARCSEC), "PLATE FIT RESIDUAL IN DEC (ARCSEC)")
			});

			// Photometric fit
			int unk = solvedVar.length - var - novar;
			if (novar < 1 && unk < 1) {
				header2 = HeaderElement.addHeaderEntry(header2, new HeaderElement[] {
						new HeaderElement("REF_MAG", "", "Reference magnitude (photometric fit, blank = not available)"),
						new HeaderElement("REF_FLUX", "", "Reference flux (photometric fit, blank = not available)")
				});
			} else {
				ArrayList<String> val = new ArrayList<String>();
				double minMag = -10, minFlux = -1;
				double xpos = 0, ypos = 0;
				for (int i=0; i<solvedMag.length; i++) {
					String line = solvedMag[i]+" "+solvedFlux[i];
					if (novar > 0) {
						if (solvedVar[i].equals("N")) {
							val.add(line);
							if (solvedMag[i] > minMag) {
								minMag = solvedMag[i];
								minFlux = solvedFlux[i];
								xpos = solvedX[i];
								ypos = solvedY[i];
							}
						}
					} else {
						if (solvedVar[i].equals("-")) {
							val.add(line);
							if (solvedMag[i] > minMag) {
								minMag = solvedMag[i];
								minFlux = solvedFlux[i];
								xpos = solvedX[i];
								ypos = solvedY[i];
							}
						}
					}
				}
				LinearFit lf = null;
				if (val.size()  < 2) {
					header2 = HeaderElement.addHeaderEntry(header2, new HeaderElement[] {
							new HeaderElement("REF_MAG", ""+minMag, "Reference magnitude (photometric fit, 0 = not available)"),
							new HeaderElement("REF_MAGX", ""+xpos, "X position in image of reference star (photometric fit)"),
							new HeaderElement("REF_MAGY", ""+ypos, "Y position in image of reference star (photometric fit)"),
							new HeaderElement("REF_FLUX", ""+minFlux, "Reference flux (photometric fit, 0 = not available)")
					});
					// mag * = REF_MAG - 2.5 log10 (flux * / flux * ref)
					//    flux * ref should be REF_FLUX if using SExtractor, otherwise the flux of the * at (REF_MAGX, REF_MAGY)
				} else {
					double x[] = new double[val.size()];
					double y[] = new double[val.size()];
					for (int i=0; i<x.length; i++) {
						y[i] = Double.parseDouble(FileIO.getField(1, val.get(i), " ", true)) - minMag;
						x[i] = -2.5*Math.log10(Double.parseDouble(FileIO.getField(2, val.get(i), " ", true)) / minFlux);
						System.out.println("photo fit "+x[i]+"/"+y[i]);
					}
					// Get linear fit and remove all possible points outside the best fitting line
					while (true) {
						lf = new LinearFit(x, y);
						// FIXME: force slope ? at least forces a fit assuming linear behavior of detector
						lf.forceSlopeToValue = 1.0 / 2.5;
						lf.forceSlope = true;
						lf.linearFit();
						// To eliminate points generally reduces correlation, but saturated stars must be eliminated ...
						int badPoints[] = lf.getInvalidPoints(true, false);
						if (badPoints != null && x.length > 3 && badPoints.length > 0) {
							double badPointsErrors[] = lf.getInvalidPointsErrors(true, false);
							double err0 = badPointsErrors[0], mean = Functions.sumComponents(DataSet.getSubArray(badPointsErrors, 1, badPointsErrors.length-1)) / (badPointsErrors.length-1.0);
							if (err0 > 3 * mean) {
								System.out.println("Removing point "+badPoints[0]+" ("+x[badPoints[0]]+", "+y[badPoints[0]]+") from the photometric fit");
								x = DataSet.deleteIndex(x, badPoints[0]);
								y = DataSet.deleteIndex(y, badPoints[0]);
								continue;
							}
						}
						break;
					}

					System.out.println("Photometric fit: found slope "+lf.slope+" and n = "+lf.valueInXEqualToZero+", with correlation "+lf.correlation);

					header2 = HeaderElement.addHeaderEntry(header2, new HeaderElement[] {
							new HeaderElement("REF_MAG", ""+minMag, "Reference magnitude of faintest star (photometric fit, 0 = not available)"),
							new HeaderElement("REF_MAGX", ""+xpos, "X position in image of reference star (photometric fit)"),
							new HeaderElement("REF_MAGY", ""+ypos, "Y position in image of reference star (photometric fit)"),
							new HeaderElement("REF_DM", ""+lf.slope, "Slope m in photometric fit (delta mag = m * (observed delta mag) + n). Should be close to 1"),
							new HeaderElement("REF_DN", ""+lf.valueInXEqualToZero, "Value n in x=0 in photometric fit (delta mag = m * (observed delta mag) + n). Should be close to 0"),
							new HeaderElement("REF_FLUX", ""+minFlux, "Reference flux of faintest star (photometric fit, 0 = not available)")
					});
					// dmag real = (dmag theoric - n) / m    =>    mag * = REF_MAG + (dmag theoric * - n) / m
					//                                             dmag theoric * = -2.5 log10 (flux * / flux * ref)
				}
				// dmagFlux = m * dmagMag + n = m * (magcat - magmin) + n = -2.5 log10(fluxobs / fluxmin) = thdmag

				String table[][] = new String[solvedX.length+1][10];
				table[0] = new String[] {"X", "Y", "FLUX", "MAG", "CAT_RA", "CAT_DEC", "CAT_MAG", "VAR", "SP_TYPE", "NAME"};
				for (int i=0; i<solvedX.length; i++) {
					double mag = 0, thdmag = -2.5*Math.log10(solvedFlux[i] / minFlux);
					if (val.size()  < 2) {
						mag = minMag + thdmag;
					} else {
						double realdmag = lf.slope * thdmag + lf.valueInXEqualToZero;
						mag = minMag + realdmag;
					}
					String line1 = ""+solvedX[i]+"***"+solvedY[i]+"***"+solvedFlux[i]+"***"+mag+"***"+solvedRA[i]+"***"+solvedDEC[i]+"***"+solvedMag[i]+"***"+solvedVar[i]+"***"+solvedSp[i]+"***"+solvedName[i];
					table[i+1] = DataSet.toStringArray(line1, "***", false);
				}
				HeaderElement tableHeader[] = FitsBinaryTable.parseHeader(new String[] {
			        	"EXTNAME  A  SOURCES",
			        	"TABLEREV  I  1",
			        	"NSOLVED  I  "+nsolved
				});
				this.binaryTable = FitsBinaryTable.createBinaryTable(tableHeader, table);
			}
			header = HeaderElement.addHeaderEntry(header, header2);
		} catch (Exception exc) {
			exc.printStackTrace();
		}

		return header;
	}

	private double scale = 0, angle = 0, nobs = 0;
	private int[][] findTriangle(double x[], double y[], double l[], double err, int id[], int tri, double maxl, double angle) {
		int out[][] = null;
		ArrayList<int[]> solution = new ArrayList<int[]>();
		int imin = 0, imax = x.length;
		int jmin = 0, jmax = x.length;
		int kmin = 0, kmax = x.length;
		if (id[tri] >= 0) {
			imin = id[tri];
			imax = imin + 1;
		}
		if (id[tri+1] >= 0) {
			jmin = id[tri+1];
			jmax = jmin + 1;
		}
		if (id[tri+2] >= 0) {
			kmin = id[tri+2];
			kmax = kmin + 1;
		}
		for (int i=imin; i<imax; i++) {
			for (int j=jmin; j<jmax; j++) {
				if (j == i) continue;
				for (int k=kmin; k<kmax; k++) {
					if (k == i || k == j) continue;

					int nsolved = 0;
					for (int m=0; m<id.length; m++) {
						if (id[m] == i && tri != m) nsolved ++;
						if (id[m] == j && (tri+1) != m) nsolved ++;
						if (id[m] == k && (tri+2) != m) nsolved ++;
					}
					if (nsolved > 0) continue;

					double max = 0, length[] = new double[3], ang = -1;
					double dx = x[i] - x[j];
					double dy = y[i] - y[j];
					length[0] = FastMath.hypot(dx, dy);
					if (length[0] > max) max = length[0];
					ang = FastMath.atan2_accurate(dy, dx);
					dx = x[j] - x[k];
					dy = y[j] - y[k];
					length[1] = FastMath.hypot(dx, dy);
					if (length[1] > max) max = length[1];
					dx = x[k] - x[i];
					dy = y[k] - y[i];
					length[2] = FastMath.hypot(dx, dy);
					if (length[2] > max) max = length[2];

					length[0] = length[0] / max;
					length[1] = length[1] / max;
					length[2] = length[2] / max;

					// Triangle size must be within a factor 2.5 from source triangle and
					// inclined respect it less than 90 deg.
					double triangleScaleRatio = max / maxl;
					if (triangleScaleRatio > 2.5 || triangleScaleRatio < 0.4) continue;
					double orientationDifference = Functions.normalizeRadians(angle - ang);
					if (orientationDifference > Math.PI) orientationDifference = Math.abs(orientationDifference - Constant.TWO_PI);
					if (orientationDifference > Constant.PI_OVER_TWO) continue;

					if (isSimilar(l, length, 0, 1, 2, err / Math.max(max, maxl))) {
						if (nobs > 1) {
							double meanScale = scale / nobs, meanAngle = this.angle / nobs;
							double difScale = meanScale / triangleScaleRatio;
							double difAngle = Functions.normalizeRadians(meanAngle - orientationDifference);
							if (difAngle > Math.PI) difAngle = Math.abs(difAngle - Constant.TWO_PI);
							if (difScale > 1.2 || difScale < 0.8 || difAngle > 5 * Constant.DEG_TO_RAD) continue;
						}
						nobs ++;
						scale += triangleScaleRatio;
						this.angle += orientationDifference;

						solution.add(new int[] {i, j, k});
					}
/*					if (isSimilar(l, length, 0, 2, 1, err / Math.max(max, maxl))) solution.add(new int[] {i, k, j});
					if (isSimilar(l, length, 1, 2, 0, err / Math.max(max, maxl))) solution.add(new int[] {j, k, i});
					if (isSimilar(l, length, 1, 0, 2, err / Math.max(max, maxl))) solution.add(new int[] {j, i, k});
					if (isSimilar(l, length, 2, 1, 0, err / Math.max(max, maxl))) solution.add(new int[] {k, j, i});
					if (isSimilar(l, length, 2, 0, 1, err / Math.max(max, maxl))) solution.add(new int[] {k, i, j});
*/
				}
			}
		}

		if (solution.size() > 0) {
			out = new int[solution.size()][3];
			for (int i = 0; i < solution.size(); i++) {
				out[i] = solution.get(i);
			}
		}
		return out;
	}

	private boolean isSimilar(double l1[], double l2[], int i1, int i2, int i3, double err) {
		double ratio1 = Math.abs(l1[0] - l2[i1]) / Math.min(l1[0], l2[i1]);
		double ratio2 = Math.abs(l1[1] - l2[i2]) / Math.min(l1[1], l2[i2]);
		double ratio3 = Math.abs(l1[2] - l2[i3]) / Math.min(l1[2], l2[i3]);
		if (ratio1 < err && ratio2 < err && ratio3 < err) return true;
		return false;
	}

	/**
	 * Process a given raw file using DCRaw. Parameters passed to DCRaw are
	 * fixed to obtain a PGM file as output. This method is called internally
	 * and you should have no need to call it manually.
	 * @param file Path to a raw file.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void executeDCRaw(String file)
	throws JPARSECException {
		// -D -> 14 bits, no scale to 16. -d => dark substraction + 16 bit scaling
		// -o 0 -> raw color
		// -q 3 -> max. interp. quality
		// -4 -> 16 bit output (PGM). Better not include this to preserves camera's original output
		// -r 1 1 1 1 -> no white balance
		// -w -> white balance from camera
		// -t 0, -W, -j, -f -> no flip, autobrighten, rotate pixels, RGGB as 4 colors
		String flags = "-w -H 0 -o 0 -q 3 -t 0 -j -f -W -r 1 1 1 1 -D "; // -f => RGBG as 4 colors. -H 0 no estoy seguro, puede que no afecte
		flags = "-D -4 -t 0 -o 0 ";

		Process p = ApplicationLauncher.executeCommand("dcraw "+flags+file);
		try {
			p.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static BufferedImage pgm(int width, int height, int maxcolval, byte[] data, boolean scale) {
		if (maxcolval < 256){
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
			WritableRaster raster = image.getRaster();
			int k = 0, pixel;
			if (maxcolval == 255 || !scale) {                                      // don't scale
				for (int y = 0; y < height; y++) {
					for (int x = 0; (x < width) && (k < data.length); x++) {
						raster.setSample(x, y, 0, data[k++] & 0xFF);
					}
				}
			} else {
				for (int y = 0; y < height; y++) {
					for (int x = 0; (x < width) && (k < data.length); x++) {
						pixel = (((data[k++] & 0xFF) * 255) + (maxcolval>>1)) / maxcolval;  // scale to 0..255 range
						raster.setSample(x, y, 0, pixel);
					}
				}
			}
			return image;
		} else {                                                     // 16 bit gray scale image
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
			WritableRaster raster = image.getRaster();
			int k = 0, sample, pixel;
			if (maxcolval == 65535 || (maxcolval < 65535 && !scale)) { // don't scale
				for (int y = 0; y < height; y++) {
					for (int x = 0; (x < width) && (k < data.length - 1); x++) {
						sample = (data[k++] & 0xFF) | ((data[k++] & 0xFF) << 8);
						raster.setSample(x, y, 0, sample);
					}
				}
			} else {
				for (int y = 0; y < height; y++) {
					for (int x = 0; (x < width) && (k < data.length - 1); x++) {
						sample = (data[k++] & 0xFF) | ((data[k++] & 0xFF) << 8);
						pixel = ((sample * 65535) + (maxcolval>>1)) / maxcolval;   // scale to 0..65535 range
						raster.setSample(x, y, 0, pixel);
					}
				}
			}
			return image;
		}
	}

	private static BufferedImage pgm(int width, int height, int maxcolval, char[] data, boolean scale) {
		if (maxcolval < 256){
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
			WritableRaster raster = image.getRaster();
			int k = 0, pixel;
			if (maxcolval == 255 || !scale) {                                      // don't scale
				for (int y = 0; y < height; y++) {
					for (int x = 0; (x < width) && (k < data.length); x++) {
						raster.setSample(x, y, 0, data[k++] & 0xFF);
					}
				}
			} else {
				for (int y = 0; y < height; y++) {
					for (int x = 0; (x < width) && (k < data.length); x++) {
						pixel = (((data[k++] & 0xFF) * 255) + (maxcolval>>1)) / maxcolval;  // scale to 0..255 range
						raster.setSample(x, y, 0, pixel);
					}
				}
			}
			return image;
		} else {                                                     // 16 bit gray scale image
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
			WritableRaster raster = image.getRaster();
			int k = 0, sample, pixel;
			if (maxcolval == 65535 || (maxcolval < 65535 && !scale)) { // don't scale
				for (int y = 0; y < height; y++) {
					for (int x = 0; (x < width) && (k < data.length - 1); x++) {
						sample = data[k++]; //(data[k++] & 0xFF) | ((data[k++] & 0xFF) << 8);
						raster.setSample(x, y, 0, sample);
					}
				}
			} else {
				for (int y = 0; y < height; y++) {
					for (int x = 0; (x < width) && (k < data.length - 1); x++) {
						sample = data[k++]; //(data[k++] & 0xFF) | ((data[k++] & 0xFF) << 8);
						pixel = ((sample * 65535) + (maxcolval>>1)) / maxcolval;   // scale to 0..65535 range
						raster.setSample(x, y, 0, pixel);
					}
				}
			}
			return image;
		}
	}

	private static BufferedImage pgm(int width, int height, int maxcolval, int[] data, boolean scale) {
		if (maxcolval < 256){
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
			WritableRaster raster = image.getRaster();
			int k = 0, pixel;
			if (maxcolval == 255 || !scale) {                                      // don't scale
				for (int y = 0; y < height; y++) {
					for (int x = 0; (x < width) && (k < data.length); x++) {
						raster.setSample(x, y, 0, data[k++] & 0xFF);
					}
				}
			} else {
				for (int y = 0; y < height; y++) {
					for (int x = 0; (x < width) && (k < data.length); x++) {
						pixel = (((data[k++] & 0xFF) * 255) + (maxcolval>>1)) / maxcolval;  // scale to 0..255 range
						raster.setSample(x, y, 0, pixel);
					}
				}
			}
			return image;
		} else {                                                     // 16 bit gray scale image
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
			WritableRaster raster = image.getRaster();
			int k = 0, sample, pixel;
			if (maxcolval == 65535 || (maxcolval < 65535 && !scale)) { // don't scale
				for (int y = 0; y < height; y++) {
					for (int x = 0; (x < width) && (k < data.length - 1); x++) {
						sample = data[k++]; //(data[k++] & 0xFF) | ((data[k++] & 0xFF) << 8);
						raster.setSample(x, y, 0, sample);
					}
				}
			} else {
				for (int y = 0; y < height; y++) {
					for (int x = 0; (x < width) && (k < data.length - 1); x++) {
						sample = data[k++]; //(data[k++] & 0xFF) | ((data[k++] & 0xFF) << 8);
						pixel = ((sample * 65535) + (maxcolval>>1)) / maxcolval;   // scale to 0..65535 range
						raster.setSample(x, y, 0, pixel);
					}
				}
			}
			return image;
		}
	}

	/**
	 * Creates a gray image from the data contained in a integer array.
	 * @param maxcolval The maximum intensity in the input array, 255 for
	 * an 8 bit image, 65535 for a 16 bit image.
	 * @param data The image data.
	 * @param scale True to scale the data to 255 or 65535 in case the maximum
	 * intensity is not any of those 2 values. True is recommended unless the
	 * original count numbers should be preserved.
	 * @return The image.
	 */
	public static BufferedImage pgm(int maxcolval, int[][] data, boolean scale){
		int width = data.length, height = data[0].length;
		if (maxcolval < 256){
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
			WritableRaster raster = image.getRaster();
			int pixel;
			if (maxcolval == 255 || !scale) {                                      // don't scale
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						raster.setSample(x, y, 0, data[x][y] & 0xFF);
					}
				}
			} else {
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						pixel = (((data[x][y] & 0xFF) * 255) + (maxcolval>>1)) / maxcolval;  // scale to 0..255 range
						raster.setSample(x, y, 0, pixel);
					}
				}
			}
			return image;
		} else {                                                     // 16 bit gray scale image
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
			WritableRaster raster = image.getRaster();
			int sample, pixel;
			if (maxcolval == 65535 || (maxcolval < 65535 && !scale)) { // don't scale
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						sample = data[x][y]; //(data[x][y] & 0xFF) | ((data[x][y] & 0xFF) << 8);
						raster.setSample(x, y, 0, sample);
					}
				}
			} else {
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						sample = data[x][y]; //(data[x][y] & 0xFF) | ((data[x][y] & 0xFF) << 8);
						pixel = ((sample * 65535) + (maxcolval>>1)) / maxcolval;   // scale to 0..65535 range
						raster.setSample(x, y, 0, pixel);
					}
				}
			}
			return image;
		}
	}

	/**
	 * Returns the counts in a given pixel position for a gray image.
	 * @param x X position.
	 * @param y Y position.
	 * @param img The image.
	 * @return The count number.
	 */
	public static int getPixelCount(int x, int y, BufferedImage img) {
        return img.getRaster().getSample(x, y, 0);
	}

	/**
	 * Reads a PGM file from disk.
	 * @param file The path to the file.
	 * @param scale True to scale values in the file to 255 (8 bit) or 65535 (16 bit).
	 * True is recommended unless the original count numbers should be preserved.
	 * @return The image.
	 * @throws JPARSECException If an error occurs.
	 */
	public static BufferedImage readPGM(String file, boolean scale) throws JPARSECException {
		try {
			ImageInputStream iis = new FileImageInputStream(new File(file));
	        iis.readLine();    // first line contains P2 or P5
	        String line = iis.readLine();     // second line contains height and width
	        while (line.startsWith("#")) {
	            line = iis.readLine();
	        }
	        int width = Integer.parseInt(FileIO.getField(1, line, " ", true));
	        int height = Integer.parseInt(FileIO.getField(2, line, " ", true));
	        line = iis.readLine();// third line contains maxVal/depth
	        int depth = Integer.parseInt(FileIO.getField(1, line, " ", true));
	        if (depth < 256) {
		        byte data[] = new byte[width*height];
		        iis.readFully(data);
		        return ObservationManager.pgm(width, height, depth, data, scale);
	        } else {
	        	if (depth < 65536) {
			        char data[] = new char[width*height];
			        iis.readFully(data, 0, data.length);
			        return ObservationManager.pgm(width, height, depth, data, scale);
	        	} else {
			        int data[] = new int[width*height];
			        iis.readFully(data, 0, data.length);
			        return ObservationManager.pgm(width, height, depth, data, scale);
	        	}
	        }
		} catch (Exception exc) {
			throw new JPARSECException("Could not load the file "+file, exc);
		}
	}

	/**
	 * Writes a PGM file to disk, as a P5 file.
	 * @param file The path to the file.
	 * @param img The image to write, with the counts at pixel [x][y].
	 * @param max The maximum number of counts allowed in the image.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void writePGM(String file, int[][] img, int max) throws JPARSECException {
		try {
			ImageOutputStream iis = new FileImageOutputStream(new File(file));
			String sep = FileIO.getLineSeparator();
			iis.writeBytes("P5"+sep);
			iis.writeBytes(""+img.length+" "+img[0].length+sep);
			iis.writeBytes(""+max+sep);
			for (int y=0;y<img[0].length;y++) {
				for (int x=0;x<img.length;x++) {
					if (max < 256) {
						iis.write(img[x][y]);
					} else {
						if (max < 65536) {
							iis.writeChar(img[x][y]);
						} else {
							iis.writeInt(img[x][y]); // Non standard
						}
					}
				}
			}
		} catch (Exception exc) {
			throw new JPARSECException("Could not write to the file "+file, exc);
		}
	}

	private JLabel img;
	private JScrollPane imgScroll, imgInfoScroll, tableScroll;
	private int scaleMode = 100; // 100%, 50%, 0 = fit
	private int colorMode = 0; // 0 = auto (combine RGB), 1 = R, 2 = G, 3 = B
	private JCheckBox autoReduce, focusAssist;
	private JButton reduceButton, deleteButton;
	private JTextArea imgInfo;
	private JTableRendering table;
	private boolean showGrid = false, linearIntensityScale = false;
	private String lineTable[] = null;
	private final String SEPARATOR = ";";
	private JTextField pxt, pyt, rat, dect;
	private PICTURE_LEVEL plev1 = PICTURE_LEVEL.ORIGINAL, plev2 = PICTURE_LEVEL.LOG_SCALE;

	private void createPanel() throws JPARSECException {
		int vgap = 25;
		Color fc = Color.lightGray;

		// Observation panel
		MigLayout imgLayout = new MigLayout("wrap 8");
		JPanel imgPanel = new JPanel(imgLayout);
		imgPanel.setBackground(null);
		imgPanel.setBorder(TelescopeControlPanel.getBorder(Translate.translate(1189)));
		img = new JLabel();
		img.setForeground(null);
		img.setBackground(null);
		imgScroll = new JScrollPane(img);
		imgPanel.add(imgScroll, "span,width 100%, height 90%");
		imgScroll.setBackground(null);
		imgScroll.getVerticalScrollBar().setUnitIncrement(16);
		imgScroll.getHorizontalScrollBar().setUnitIncrement(16);
		img.addMouseListener(this);
		img.addMouseMotionListener(this);
		JLabel pxl = new JLabel("px");
		JLabel pyl = new JLabel("py");
		JLabel ral = new JLabel(Translate.translate(912) + " (J2000)");
		JLabel decl = new JLabel(Translate.translate(913));
		pxt = new JTextField();
		pyt = new JTextField();
		rat = new JTextField();
		dect = new JTextField();
		pxt.setFocusable(false);
		pxt.setEditable(false);
		pyt.setFocusable(false);
		pyt.setEditable(false);
		rat.setFocusable(false);
		rat.setEditable(false);
		dect.setFocusable(false);
		dect.setEditable(false);
		imgPanel.add(pxl, "width 9%");
		imgPanel.add(pxt, "width 9%");
		imgPanel.add(pyl, "width 9%");
		imgPanel.add(pyt, "width 9%");
		imgPanel.add(ral, "width 9%");
		imgPanel.add(rat, "width 23%");
		imgPanel.add(decl, "width 9%");
		imgPanel.add(dect, "width 23%");

		// Image info panel
		MigLayout infoLayout = new MigLayout("wrap 2", "[50%][50%]", "[]"+vgap+"[]");
		JPanel infoPanel = new JPanel(infoLayout);
		infoPanel.setBackground(null);
		infoPanel.setBorder(TelescopeControlPanel.getBorder(Translate.translate(1194)));
		autoReduce = new JCheckBox(Translate.translate(Translate.translate(1195)), doReduce);
		focusAssist = new JCheckBox(Translate.translate(Translate.translate(1196)), false);
		autoReduce.setBackground(null);
		focusAssist.setBackground(null);
		autoReduce.setForeground(fc);
		focusAssist.setForeground(fc);
		reduceButton = new JButton(Translate.translate(1197));
		deleteButton = new JButton(Translate.translate(1198));
		autoReduce.addActionListener(this);
		focusAssist.addActionListener(this);
		reduceButton.addActionListener(this);
		deleteButton.addActionListener(this);

		imgInfo = new JTextArea(12, 50);
		Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 11);
		imgInfo.setFont(mono);
		imgInfo.setEditable(false);
		imgInfoScroll = new JScrollPane(imgInfo);
/*
		imgInfo.setBackground(null);
		imgInfoScroll.setBackground(null);
		imgInfo.setForeground(fc);
		imgInfoScroll.setForeground(fc);
*/
		infoPanel.add(imgInfoScroll, "span,wrap,width 100%,height 80%");
		infoPanel.add(autoReduce, "align center");
		infoPanel.add(focusAssist, "align center");
		infoPanel.add(reduceButton, "align center");
		infoPanel.add(deleteButton, "align center");

		// Table panel
		MigLayout tableLayout = new MigLayout("");
		JPanel tablePanel = new JPanel(tableLayout);
		tablePanel.setBackground(null);
		tablePanel.setBorder(TelescopeControlPanel.getBorder(Translate.translate(1199)));
		createTable();
		tableScroll = new JScrollPane(table.getComponent());
		tableScroll.setAutoscrolls(true);
		tablePanel.add(tableScroll, "span,wrap,height 100%, width 100%");

		// Global panels
		String constrainColumn = "";
		MigLayout globalLayout = new MigLayout("fillx", constrainColumn);
		setPreferredSize(new Dimension(1000, 500));
		this.setLayout(globalLayout);
		this.setBackground(Color.black);
		this.add(imgPanel, "span,grow,dock west, width 60%");
		this.add(infoPanel, "span,grow,wrap,width 40%,height 60%");
		this.add(tablePanel, "span,grow,height 40%");

		addComponentListener(this);
		updatePanel();
	}

	private void updateImage() {
		if (lastImagePath != null && lastImagePath.endsWith(".fits")) {
       		try {
				FitsIO fio = new FitsIO(lastImagePath);
				HeaderElement header[] = fio.getHeader(0);
				boolean raw = header[HeaderElement.getIndex(header, "RAW")].getAsBoolean();
				if (raw) {
					lastImage = fio.getPicture(0, plev2, showGrid).getImage();
				} else {
					byte[][] r = fio.getPicture(0, plev1, showGrid).getImageAsByteArray(0);
					byte[][] g = fio.getPicture(1, plev1, showGrid).getImageAsByteArray(1);
					byte[][] b = fio.getPicture(2, plev1, showGrid).getImageAsByteArray(2);
					Picture pic = new Picture(r, g, b, null);
					lastImage = pic.getImage();
				}

				if (lastImageTable == null) {
					int sou = -1;
					for (int i=0; i<fio.getNumberOfPlains(); i++) {
						if (fio.isBinaryTable(i)) {
							header = fio.getHeader(i);
							int tableID = HeaderElement.getIndex(header, "EXTNAME");
							if (tableID >= 0) {
								if (header[tableID].getAsString().trim().equals("SOURCES")) {
									sou = i;
									break;
								}
							}
						}
					}
					if (sou < 0) return;

					lastImageTable = FitsBinaryTable.getBinaryTable(fio.getHDU(sou));
				}
       		} catch (Exception exc) { exc.printStackTrace(); }
		}
	}

	private void updatePanel() {
		if (!this.reducePossible || imgScroll == null) return;

		ImageIcon img = new ImageIcon();
		if (lastImage != null) {
			if (scaleMode == 200 || scaleMode == 50 || scaleMode == 0) {
				Picture pic = new Picture(lastImage);

				if (scaleMode == 200) {
					pic.scaleImage(pic.getWidth()*2, pic.getHeight()*2);
					imgScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
					imgScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
				} else {
					if (scaleMode == 50) {
						pic.scaleImage(pic.getWidth()/2, pic.getHeight()/2);
						imgScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
						imgScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
					} else {
						pic.scaleMaintainingImageRatio(imgScroll.getWidth(), 0);
						imgScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
						imgScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
					}
				}
				if (colorMode > 0) {
					byte[][] pixels = pic.getImageAsByteArray(colorMode-1);
					pic = new Picture(pixels, pixels, pixels, null);
				}
				img.setImage(pic.getImage());
			} else {
				imgScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
				imgScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
				Picture pic = new Picture(lastImage);
				if (colorMode > 0) {
					byte[][] pixels = pic.getImageAsByteArray(colorMode-1);
					pic = new Picture(pixels, pixels, pixels, null);
				}

				img.setImage(pic.getImage());
			}
		}

		if (autoReduce.isSelected() && !this.reductionEnabled()) autoReduce.setSelected(false);
		if (!autoReduce.isSelected() && this.reductionEnabled()) autoReduce.setSelected(true);

		if (lastImagePath == null) {
			imgInfo.setText("");
			try {
				updateTable(true);
			} catch (Exception exc) {
				exc.printStackTrace();
			}
			this.img.setIcon(img);
			return;
		}

		if (focusAssist.isSelected()) {
			if (lastImagePath.endsWith(".fits") || lastImagePath.endsWith(".jpg") ||
					lastImagePath.endsWith(".png") || lastImagePath.endsWith(".pgm")) {
				if (lastImageTable != null) {
					StringBuffer s = new StringBuffer("");
					BufferedImage image = null;
					Graphics2D g = null;
					double scale = 1;
					int r = 8, r2 = 2*r;
					try {
						image = Picture.copy(Picture.toBufferedImage(img.getImage()));
						g = image.createGraphics();
						g.setColor(Color.WHITE);
						scale = image.getWidth() / (double) lastImage.getWidth();
					} catch (Exception exc) { exc.printStackTrace(); }
					for (int i=0; i<lastImageTable.length; i++) {
						for (int j=0; j<lastImageTable[0].length;j++) {
							if (i == 0 || !DataSet.isDoubleStrictCheck(lastImageTable[i][j])) {
								s.append(FileIO.addSpacesBeforeAString(lastImageTable[i][j], 12));
							} else {
								double f = 1.0;
								int ndec = 3;
								if (j == 4) f = Constant.RAD_TO_HOUR;
								if (j == 5) f = Constant.RAD_TO_DEG;
								if (j == 4 || j == 5) ndec = 6;
								String v = Functions.formatValue(f * Double.parseDouble(lastImageTable[i][j]), ndec);
								v = DataSet.replaceAll(v, ".000", "", true);

								if ((j == 4 || j == 5 || j == 6) && (lastImageTable[i][4].equals("-1.0") && lastImageTable[i][5].equals("-1.0"))) {
									s.append(FileIO.addSpacesBeforeAString("-", 12));
								} else {
									s.append(FileIO.addSpacesBeforeAString(v, 12));
								}
							}
						}
						s.append(FileIO.getLineSeparator());

						if (i > 0 && g != null) {
							int x = (int) ((Double.parseDouble(lastImageTable[i][0]) - 1) * scale);
							int y = (int) ((Double.parseDouble(lastImageTable[i][1]) - 1) * scale);
							g.drawOval(x-r, y-r, r2, r2);
							String name = lastImageTable[i][9].trim();
							if (name == null || name.isEmpty() || name.equals("-")) {
								name = Functions.formatValue(Double.parseDouble(lastImageTable[i][6]), 3);
								if (lastImageTable[i][4].equals("-1.0") && lastImageTable[i][5].equals("-1.0")) name = "~"+Functions.formatValue(Double.parseDouble(lastImageTable[i][3]), 3);
							}
							g.drawString(name, x - g.getFontMetrics().stringWidth(name) / 2, y + 30);
						}
					}
					imgInfo.setText(s.toString());
					if (image != null) img.setImage(image);
				} else {
					String path = FileIO.getDirectoryFromPath(lastImagePath);
					try {
						double seeing = 1.0, mag0 = 0.0, pixScale = 0, gain = 0.0, saturation = 0.0;
						if (lastImagePath.endsWith(".fits")) {
								FitsIO fio = new FitsIO(lastImagePath);
								HeaderElement header[] = fio.getHeader(0);
								try { gain = header[HeaderElement.getIndex(header, "GAIN")].getAsDouble(); } catch (Exception exc) {}
								try { saturation = header[HeaderElement.getIndex(header, "MAXADU")].getAsDouble(); } catch (Exception exc) {}
								if (saturation == 0) {
									int depth = 0;
									try { depth = header[HeaderElement.getIndex(header, "DEPTH")].getAsInt(); } catch (Exception exc) {}
									if (depth > 0) saturation = FastMath.multiplyBy2ToTheX(1, depth-1);
								}
								int index = HeaderElement.getIndex(header, "CRPIX1");
								if (index < 0) {
									double field = 0.0;
									try { field = header[HeaderElement.getIndex(header, "FIELD")].getAsDouble(); } catch (Exception exc) {}
									if (field > 0) {
										double resolution = header[HeaderElement.getIndex(header, "NAXIS1")].getAsDouble();
										pixScale = field * 3600.0 / resolution;
									}
								}
						} else {
							saturation = 255;
						}
						if (path.equals("")) path = ".";
						if (gain == 0) gain = 1;
						SExtractor.createMachineConfigFile(path, saturation, gain, pixScale, seeing, mag0, minArea, sigma);
					} catch (Exception exc) {
						exc.printStackTrace();
					}
					SExtractor sex = new SExtractor(path, "machine.config");
					try {
						sex.execute(FileIO.getFileNameFromPath(lastImagePath));
						String newText = sex.toString(4, minArea);
						if (previousSExtractor != null) {
							String t = newText;
							newText += FileIO.getLineSeparator() + Translate.translate(1217) + FileIO.getLineSeparator() + previousSExtractor;
							previousSExtractor = t;
						} else {
							previousSExtractor = newText;
						}
						imgInfo.setText(newText);
					} catch (Exception exc) {
						imgInfo.setText("ERROR!!!");
						exc.printStackTrace();
					}
					sex.removeTemporalFiles(true);
				}
			} else {
				imgInfo.setText(Translate.translate(1206));
			}
		} else {
			int w = 0, h = 0;
			try {
				if (lastImagePath.endsWith(".fits")) {
					FitsIO fio = new FitsIO(lastImagePath);
					String data[] = new String[] {
							"Path: "+lastImagePath,
							"",
							"Contents of this fits file:"
					};
					String header = "";
					for (int i=0; i<fio.getNumberOfPlains(); i++) {
						header += FileIO.getLineSeparator() + "Header of plain #"+i + FileIO.getLineSeparator() + HeaderElement.toString(fio.getHeader(i), 25, 10);
					}
					imgInfo.setText(DataSet.toString(data, FileIO.getLineSeparator()) + FileIO.getLineSeparator() +
							fio.toString() + header);
				} else {
					Picture pic;
					if (lastImagePath.endsWith(".pgm")) {
						pic = new Picture(readPGM(lastImagePath, true));
						w = pic.getWidth();
						h = pic.getHeight();
					} else {
						pic = new Picture(lastImagePath);
						w = pic.getWidth();
						h = pic.getHeight();
					}
					String data[] = new String[] {
							"Path: "+lastImagePath,
							"Width: "+w,
							"Height: "+h
					};
					imgInfo.setText(DataSet.toString(data, FileIO.getLineSeparator()));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		this.img.setIcon(img);
		imgInfo.setCaretPosition(0);
		try {
			updateTable(true);
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		worker.addToQueue(e);
	}
	Popup popup = null;

	@Override
	public void mouseEntered(MouseEvent e) {}
	@Override
	public void mouseExited(MouseEvent e) {}
	@Override
	public void mousePressed(MouseEvent e) {
		if (e.getSource() == img) startPt = e.getPoint();
	}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (e.getSource() == img) {
		    img.setLocation(img.getX()+e.getX()-startPt.x, img.getY()+e.getY()-startPt.y);
		    ((JComponent)img.getParent()).scrollRectToVisible(img.getBounds());
		    //imgScroll.getHorizontalScrollBar().setValues(imgScroll.getHorizontalScrollBar().getValue()+(e.getX()-startPt.x)/2, img.getWidth()/10, 0, img.getWidth());
		    //imgScroll.getVerticalScrollBar().setValues(imgScroll.getVerticalScrollBar().getValue()+(e.getY()-startPt.y)/2, img.getHeight()/10, 0, img.getHeight());
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (e.getSource() == img) {
			if (popup != null) popup.hide();

			if (lastImage == null || lastImagePath == null || lastImagePath.equals("null")) {
				pxt.setText("");
				pyt.setText("");
				rat.setText("");
				dect.setText("");
				return;
			}

			// First pixel should be (1, 1) as in SExtractor
			int px = e.getX() + 1, py = e.getY() + 1;
			if (scaleMode == 200) {
				px /= 2;
				py /= 2;
			} else {
				if (scaleMode == 50) {
					px *= 2;
					py *= 2;
				} else {
					if (scaleMode == 0) {
						double scaleX = lastImage.getWidth() / (double) img.getWidth();
						double scaleY = scaleX; // lastImage.getHeight() / (double) img.getHeight();
						double dy = (img.getHeight() - lastImage.getHeight() / scaleY) / 2;
						px = (int) ((px * scaleX) + 0.5);
						py = (int) (((py - dy) * scaleY) + 0.5);
					}
				}
			}
			pxt.setText(""+px);
			pyt.setText(""+py);
			rat.setText("");
			dect.setText("");
			if (!lastImagePath.endsWith(".fits") || lastImageWCS == null || lastImageAstrometry == null) return;
			try {
				//LocationElement loc = lastImageWCS.getSkyCoordinates(new Point2D.Double(px, py));
				LocationElement loc = lastImageAstrometry.getPlatePosition(px, py);
				rat.setText(Functions.formatRA(loc.getLongitude()));
				dect.setText(Functions.formatDEC(loc.getLatitude()));
			} catch (Exception exc) {
				exc.printStackTrace();
			}

		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		worker.addToQueue(e);
	}

	private void createTable() throws JPARSECException {
		updateTable(false);

		final String columnNames[] = new String[] {
				Translate.translate(1200)+" ?", Translate.translate(506), Translate.translate(1201), "ISO", Translate.translate(180)+" (s)", Translate.translate(1202)
		};
		final Class<?> columnClasses[] = new Class[] {
			Boolean.class, String.class, String.class, Integer.class, null, null
		};
		table = new JTableRendering(columnNames, columnClasses, null, toTable(lineTable));
		table.setColumnWidth(new int[] {30, 50, 100, 30, 20});
		table.setRowColor(6, new String[] {"1", "2", "3"}, new Color[] {Color.RED, Color.YELLOW, Color.CYAN});
		table.getComponent().addMouseListener(this);
		table.getComponent().getTableHeader().addMouseListener(this);
	}

	private String[][] toTable(String t[]) {
		String out[][] = new String[t.length][7];
		for (int i=0; i<t.length; i++) {
			out[i] = DataSet.toStringArray(t[i], SEPARATOR);
			out[i][1] = FileIO.getFileNameFromPath(out[i][1]);
		}
		return out;
	}
	private void updateTable(boolean show) throws JPARSECException {
		ArrayList<String> list = new ArrayList<String>();
		String dir = "";
		int index = -1;
		String lt[][] = null;
		if (lineTable != null) lt = table.getTableData();
		for (int camIndex=0; camIndex<cameraDir.length;camIndex ++) {
			for (int id=0; id<=5; id++) {
				if (id == 0) dir = workingDir + obsDir + cameraDir[camIndex] + darkDir[camIndex];
				if (id == 1) dir = workingDir + obsDir + cameraDir[camIndex] + flatDir[camIndex];
				if (id == 2) dir = workingDir + obsDir + cameraDir[camIndex] + onDir[camIndex];
				if (id == 3) dir = workingDir + obsDir + cameraDir[camIndex] + reducedDir[camIndex];
				if (id == 4) dir = workingDir + obsDir + cameraDir[camIndex] + stackedDir[camIndex];
				if (id == 5) dir = workingDir + obsDir + cameraDir[camIndex] + averagedDir[camIndex];

				String files[] = FileIO.getFiles(dir);
				if (files == null) continue;
				for (int j=0; j<files.length; j++) {
					if (!files[j].endsWith(".fits")) continue;
					FitsIO fio = new FitsIO(files[j]);
					HeaderElement header[] = fio.getHeader(0);

					boolean raw = header[HeaderElement.getIndex(header, "RAW")].getAsBoolean();
					String type = "raw";
					if (!raw) type = "rgb";
					int ISO = header[HeaderElement.getIndex(header, "ISO")].getAsInt();
					String date = "-";
					int datei = HeaderElement.getIndex(header, "DATE-OBS");
					if (datei >= 0 && id < 3) {
						date = header[datei].getAsString();
					} else {
						datei = HeaderElement.getIndex(header, "DATE");
						if (datei >= 0) date = header[datei].getAsString();
					}
					String source = header[HeaderElement.getIndex(header, "OBJECT")].getAsString();
					String imgid = header[HeaderElement.getIndex(header, "IMGID")].getAsString();
					if (imgid.equals(IMAGE_ID.ON_SOURCE.name())) imgid = GenericCamera.IMAGE_IDS[IMAGE_ID.ON_SOURCE.ordinal()];
					if (imgid.equals(IMAGE_ID.DARK.name())) imgid = GenericCamera.IMAGE_IDS[IMAGE_ID.DARK.ordinal()];
					if (imgid.equals(IMAGE_ID.FLAT.name())) imgid = GenericCamera.IMAGE_IDS[IMAGE_ID.FLAT.ordinal()];
					String time = header[HeaderElement.getIndex(header, "TIME")].getAsString();
					if (!DataSet.isDoubleFastCheck(time)) time = header[HeaderElement.getIndex(header, "BULBTIME")].getAsString();

					boolean enabled = true;
					int status = 0;
					if (lineTable != null) {
						int tindex = DataSet.getIndexContaining(lineTable, files[j]);
						if (tindex >= 0) {
							enabled = Boolean.parseBoolean(lt[tindex][0]);
							// enabled = Boolean.parseBoolean(FileIO.getField(1, lineTable[tindex], SEPARATOR, false));
							status = Integer.parseInt(FileIO.getField(7, lineTable[tindex], SEPARATOR, false));
						}
					}

					if (imgid.equals(GenericCamera.IMAGE_IDS[IMAGE_ID.DARK.ordinal()]) ||
							imgid.equals(GenericCamera.IMAGE_IDS[IMAGE_ID.FLAT.ordinal()])) {
						String fitsid = getFitsMainData(files[j]);
						String outputFile = FileIO.getDirectoryFromPath(files[j])+"super_"+fitsid+".fits";
						File master = new File(outputFile);
						if (status == 1) status = 0;
						if (status == 0 && !master.exists()) status = 1;
					} else {
						if (id == 2 && imgid.equals(GenericCamera.IMAGE_IDS[IMAGE_ID.ON_SOURCE.ordinal()])) {
							String outputFile = workingDir + obsDir + cameraDir[camIndex] + reducedDir[camIndex] + FileIO.getFileNameFromPath(files[j]);
							File master = new File(outputFile);
							if (status == 0 && !master.exists()) status = 1;
						}
						if (id >= 3) {
							if (status == 3) status = 0;
							if (HeaderElement.getByKey(header, "CRVAL1") == null) status = 3;
						}
					}

					// XXX Values of status:
					// 0 => everything ok, completely reduced if it is a reduced observation
					// 1 => master not found (master dark / master flat / reduced frame) => not reduced => in red
					// 2 => master flat not applied in reduced on observation => not completely reduced => in yellow
					// 3 => WCS not solved for a reduced on observation => not completely/correctly solved => in blue

					if (lastImagePath != null && lastImagePath.equals(files[j])) {
						index = list.size();
						reduceButton.setText(Translate.translate(1197));
						reduceButton.setEnabled(true);
						if (imgid.equals(GenericCamera.IMAGE_IDS_ALL[6])) reduceButton.setText(Translate.translate(1230));
						if (imgid.equals(GenericCamera.IMAGE_IDS_ALL[7])) reduceButton.setText(Translate.translate(1284));
						if (!enabled || imgid.equals(GenericCamera.IMAGE_IDS_ALL[8])) reduceButton.setEnabled(false);
					}
					type = " ("+type+")";
					if (imgid.equals(GenericCamera.IMAGE_IDS_ALL[IMAGE_ID.AVERAGED.ordinal()]) ||
							imgid.equals(GenericCamera.IMAGE_IDS_ALL[IMAGE_ID.STACKED.ordinal()]) ||
							imgid.equals(GenericCamera.IMAGE_IDS_ALL[IMAGE_ID.ON_SOURCE.ordinal()]) ||
							imgid.equals(GenericCamera.IMAGE_IDS_ALL[IMAGE_ID.REDUCED_ON.ordinal()])) type = " " + source + type;
					list.add(""+enabled+SEPARATOR+files[j]+SEPARATOR+imgid+type+SEPARATOR+ISO+SEPARATOR+time+SEPARATOR+date+SEPARATOR+status);
				}
			}
		}
		lineTable = DataSet.arrayListToStringArray(list);
		if (table != null) table.updateTable(toTable(lineTable), show);

		String element = null;
		if (index >= 0) element = lineTable[index];
        if (show) {
    		if (index >= 0) {
    			index = DataSet.getIndex(lineTable, element);
    			table.getComponent().setRowSelectionInterval(index, index);
    		} else {
    			table.getComponent().clearSelection();
    		}
	    	table.getComponent().repaint();
        }
	}
	@Override
	public void componentHidden(ComponentEvent arg0) {
	}

	@Override
	public void componentMoved(ComponentEvent arg0) {
	}

	@Override
	public void componentResized(ComponentEvent arg0) {
		updatePanel();
	}

	@Override
	public void componentShown(ComponentEvent arg0) {
	}

	private JTextField prName, prObs, weaMaxH, weaMaxW, weaMaxT, weaMinT,
		sexMA, sexSI, sexOT, sexMS, camPE[], camIS[], camDD[], camFD[], camOD[], camRD[], camSD[], camAD[];
	private JTextArea prDes;
	private JList telList, proCom, proOri, proAve, proInt, proDr, proNor;
	private JCheckBox texDSO, texPl;
	private boolean optionsPanelShown = false;
	private JPanel createOptionsPanel() throws JPARSECException {
		Color fc = Color.lightGray;

		// Project info panel
		String data[] = this.getProjectInfo();
		prName = new JTextField(data[0], 15);
		prObs = new JTextField(data[1], 15);
		prDes = new JTextArea(data[2], 5, 15);
		prDes.setLineWrap(true);
		MigLayout prLayout = new MigLayout("wrap 6"); //, "[50%][50%]", "[]"+vgap+"[]");
		JPanel prPanel = new JPanel(prLayout);
		prPanel.setBackground(null);
		prPanel.setBorder(TelescopeControlPanel.getBorder(Translate.translate(1232)));
		JLabel label1 = new JLabel(Translate.translate(506));
		label1.setForeground(fc);
		JLabel label2 = new JLabel(Translate.translate(1233));
		label2.setForeground(fc);
		JLabel label3 = new JLabel(Translate.translate(1234));
		label3.setForeground(fc);
		prPanel.add(label1, "align left");
		prPanel.add(prName, "align left");
		prPanel.add(label2, "align left");
		prPanel.add(prObs, "align left");
		prPanel.add(label3, "align left");
		prPanel.add(prDes, "align left,width 50%");

		// Telescope/camera panel (Park position ?)
		TELESCOPE_TYPE type = this.getTelescopeType();
		boolean texturesDSO = VirtualCamera.DRAW_DSO_TEXTURES;
		boolean texturesPlanets = VirtualCamera.DRAW_PLANETARY_TEXTURES;
		MigLayout telLayout = new MigLayout("wrap 3"); //, "[50%][50%]", "[]"+vgap+"[]");
		JPanel telPanel = new JPanel(telLayout);
		telPanel.setBackground(null);
		telPanel.setBorder(TelescopeControlPanel.getBorder(Translate.translate(1235)));
		telList = new JList(GenericTelescope.TELESCOPE_TYPES);
		telList.setSelectedIndex(type.ordinal());
		texDSO = new JCheckBox(Translate.translate(38), texturesDSO);
		texPl = new JCheckBox(Translate.translate(1237), texturesPlanets);
		texDSO.setForeground(fc);
		texDSO.setBackground(Color.BLACK);
		texPl.setForeground(fc);
		texPl.setBackground(Color.BLACK);
		JLabel label4 = new JLabel(Translate.translate(478));
		label4.setForeground(fc);
		JLabel label5 = new JLabel(Translate.translate(1236));
		label5.setForeground(fc);
		telPanel.add(label4, "align left");
		telPanel.add(telList, "align left,span,wrap");
		telPanel.add(label5, "align left");
		telPanel.add(texDSO, "align left,wrap");
		telPanel.add(texPl, "skip,align left,wrap");

		// Weather alarm panel
		double w[] = getWeatherAlarmConditions();
		MigLayout weaLayout = new MigLayout("wrap 4"); //, "[50%][50%]", "[]"+vgap+"[]");
		JPanel weaPanel = new JPanel(weaLayout);
		weaPanel.setBackground(null);
		weaPanel.setBorder(TelescopeControlPanel.getBorder(Translate.translate(1238)));
		JLabel label6 = new JLabel(Translate.translate(1239));
		label6.setForeground(fc);
		JLabel label7 = new JLabel(Translate.translate(1240));
		label7.setForeground(fc);
		JLabel label8 = new JLabel(Translate.translate(1241));
		label8.setForeground(fc);
		JLabel label9 = new JLabel(Translate.translate(1242));
		label9.setForeground(fc);
		weaMaxH = new JTextField(""+(float)w[0], 15);
		weaMaxW = new JTextField(""+(float)w[1], 15);
		weaMaxT = new JTextField(""+(float)w[2], 15);
		weaMinT = new JTextField(""+(float)w[3], 15);
		weaPanel.add(label6, "align left");
		weaPanel.add(weaMaxH, "align left");
		weaPanel.add(label7, "align left");
		weaPanel.add(weaMaxW, "align left");
		weaPanel.add(label8, "align left");
		weaPanel.add(weaMaxT, "align left");
		weaPanel.add(label9, "align left");
		weaPanel.add(weaMinT, "align left");

		// Image processing panel
		MigLayout proLayout = new MigLayout("wrap 4"); //, "[50%][50%]", "[]"+vgap+"[]");
		JPanel proPanel = new JPanel(proLayout);
		proPanel.setBackground(null);
		proPanel.setBorder(TelescopeControlPanel.getBorder(Translate.translate(1243)));
		JLabel label10 = new JLabel(Translate.translate(1244));
		label10.setForeground(fc);
		JLabel label11 = new JLabel(Translate.translate(1245));
		label11.setForeground(fc);
		JLabel label12 = new JLabel(Translate.translate(1246));
		label12.setForeground(fc);
		JLabel label13 = new JLabel(Translate.translate(1247));
		label13.setForeground(fc);
		JLabel label14 = new JLabel(Translate.translate(1248));
		label14.setForeground(fc);
		JLabel label14b = new JLabel(Translate.translate(1291));
		label14b.setForeground(fc);
		proCom = new JList(COMBINATION_METHODS);
		proOri = new JList(IMAGE_ORIENTATIONS);
		proAve = new JList(AVERAGE_METHODS);
		proNor = new JList(NORMALIZATION_METHODS);
		proInt = new JList(INTERPOLATION_METHODS);
		proDr = new JList(DRIZZLE_METHODS);
		proOri.setSelectedIndex(getImageOrientation().ordinal());
		proCom.setSelectedIndex(getCombineMethod().ordinal());
		proAve.setSelectedIndex(getAverageMethod().ordinal());
		proNor.setSelectedIndex(getNormalizationMethod().ordinal());
		proInt.setSelectedIndex(getInterpolationMethod().ordinal());
		proDr.setSelectedIndex(getDrizzleMethod().ordinal());
		proPanel.add(label10, "align left");
		proPanel.add(proCom, "align left");
		proPanel.add(label11, "align left");
		proPanel.add(proOri, "align left");
		proPanel.add(label12, "align left");
		proPanel.add(proAve, "align left");
		proPanel.add(label13, "align left");
		proPanel.add(proInt, "align left");
		proPanel.add(label14, "align left");
		proPanel.add(proDr, "align left");
		proPanel.add(label14b, "align left");
		proPanel.add(proNor, "align left");

		// SExtractor panel. Independent selection of minArea/sigam for RAW/JPG modes ?
		double s[] = new double[] {minArea, sigma, minValueObjType, maxSources};
		MigLayout sexLayout = new MigLayout("wrap 4"); //, "[50%][50%]", "[]"+vgap+"[]");
		JPanel sexPanel = new JPanel(sexLayout);
		sexPanel.setBackground(null);
		sexPanel.setBorder(TelescopeControlPanel.getBorder(Translate.translate(1254)));
		JLabel label15 = new JLabel(Translate.translate(1255));
		label15.setForeground(fc);
		JLabel label16 = new JLabel(Translate.translate(1256));
		label16.setForeground(fc);
		JLabel label17 = new JLabel(Translate.translate(1257));
		label17.setForeground(fc);
		JLabel label18 = new JLabel(Translate.translate(1258));
		label18.setForeground(fc);
		sexMA = new JTextField(""+(int)s[0], 15);
		sexSI = new JTextField(""+(int)s[1], 15);
		sexOT = new JTextField(""+(float)s[2], 15);
		sexMS = new JTextField(""+(int)s[3], 15);
		sexPanel.add(label15, "align left");
		sexPanel.add(sexMA, "align left");
		sexPanel.add(label17, "align left");
		sexPanel.add(sexOT, "align left");
		sexPanel.add(label16, "align left");
		sexPanel.add(sexSI, "align left");
		sexPanel.add(label18, "align left");
		sexPanel.add(sexMS, "align left");

		// Global panels
		String constrainColumn = "";
		JPanel panel = new JPanel(null);
		MigLayout globalLayout = new MigLayout("fillx", constrainColumn);
		setPreferredSize(new Dimension(1200, 500));
		panel.setLayout(globalLayout);
		panel.setBackground(Color.black);
		panel.add(prPanel, "width 100%,span,wrap");
		panel.add(proPanel, "width 60%");
		panel.add(telPanel, "width 40%,wrap");
		panel.add(weaPanel, "width 60%");
		panel.add(sexPanel, "width 40%,wrap");

		// Camera panel
		int nc = getCameras().length;
		camPE = new JTextField[nc];
		camIS = new JTextField[nc];
		camDD = new JTextField[nc];
		camFD = new JTextField[nc];
		camOD = new JTextField[nc];
		camRD = new JTextField[nc];
		camSD = new JTextField[nc];
		camAD = new JTextField[nc];
		for (int index = 0; index < nc; index ++) {
			String c[] = new String[] {""+(float)(getCameraPositionError(index) * Constant.RAD_TO_DEG), ""+getCameraMinimumIntervalBetweenShots(index),
					getDarkDir(index), getFlatDir(index), getOnDir(index), getReducedDir(index), getStackedDir(index), getAveragedDir(index)};
			MigLayout camLayout = new MigLayout("wrap 8"); //, "[50%][50%]", "[]"+vgap+"[]");
			JPanel camPanel = new JPanel(camLayout);
			camPanel.setBackground(null);
			camPanel.setBorder(TelescopeControlPanel.getBorder(DataSet.replaceAll(Translate.translate(1259), "#", ""+(index+1)+" ("+cameras[index].getType()+")", true)));
			JLabel label19 = new JLabel(Translate.translate(1260));
			label19.setForeground(fc);
			JLabel label20 = new JLabel(Translate.translate(1261));
			label20.setForeground(fc);
			JLabel label21 = new JLabel(Translate.translate(1262));
			label21.setForeground(fc);
			JLabel label22 = new JLabel(Translate.translate(1263));
			label22.setForeground(fc);
			JLabel label23 = new JLabel(Translate.translate(1264));
			label23.setForeground(fc);
			JLabel label24 = new JLabel(Translate.translate(1265));
			label24.setForeground(fc);
			JLabel label25 = new JLabel(Translate.translate(1266));
			label25.setForeground(fc);
			JLabel label26 = new JLabel(Translate.translate(1285));
			label26.setForeground(fc);
			camPE[index] = new JTextField(""+c[0], 15);
			camIS[index] = new JTextField(""+c[1], 15);
			camDD[index] = new JTextField(""+c[2], 15);
			camFD[index] = new JTextField(""+c[3], 15);
			camOD[index] = new JTextField(""+c[4], 15);
			camRD[index] = new JTextField(""+c[5], 15);
			camSD[index] = new JTextField(""+c[6], 15);
			camAD[index] = new JTextField(""+c[7], 15);
			camPanel.add(label19, "align left");
			camPanel.add(camPE[index], "align left");
			camPanel.add(label20, "align left");
			camPanel.add(camIS[index], "align left");
			camPanel.add(label21, "align left");
			camPanel.add(camDD[index], "align left");
			camPanel.add(label22, "align left");
			camPanel.add(camFD[index], "align left");
			camPanel.add(label23, "align left");
			camPanel.add(camOD[index], "align left");
			camPanel.add(label24, "align left");
			camPanel.add(camRD[index], "align left");
			camPanel.add(label25, "align left");
			camPanel.add(camSD[index], "align left");
			camPanel.add(label26, "align left");
			camPanel.add(camAD[index], "align left");
			panel.add(camPanel, "width 100%,span,wrap");
		}

		panel.addComponentListener(this);
		return panel;
	}

	private static double[] toDoubleValues(String[] array) throws JPARSECException
	{
		double out[] = new double[array.length];

		for (int i = 0; i < array.length; i++)
		{
			out[i] = DataSet.parseDouble(array[i]);
		}
		return out;
	}

	private Worker worker = null;
	private class Worker implements Runnable {
		private ArrayList<Object> queue = new ArrayList<Object>();
		public Worker() { }

		public void addToQueue(Object event) {
			queue.add(event);
		}

		public void run() {
			while (true) {
				try {
					if (queue.size() == 0) {
						Thread.sleep(500);
						continue;
					}

					Object e = queue.get(0);
					if (e instanceof MouseEvent) {
						processEvent((MouseEvent)e);
						queue.remove(0);
						continue;
					}
					if (e instanceof ActionEvent) {
						processEvent((ActionEvent)e);
						queue.remove(0);
						continue;
					}
				} catch (Exception exc) {
					exc.printStackTrace();
				}
			}
		}

		private void processEvent(MouseEvent e) {
	  		if (e.getSource() == table.getComponent()) {
				int row = table.getComponent().getSelectedRow(), column = table.getComponent().getSelectedColumn();
				if (row < 0) {
					return;
				}
				row = table.convertRowIndexToModel(row);
	       		String lastImagePath2 = FileIO.getField(2, lineTable[row], SEPARATOR, false);
	       		if (lastImagePath2.equals(lastImagePath) || table.getComponent().convertColumnIndexToModel(column) == 0) {
					try {
						updateTable(true);
					} catch (JPARSECException e2) {
						e2.printStackTrace();
					}
	       			return;
	       		}
	       		lastImagePath = lastImagePath2;
	       		lastImageWCS = null;
	       		lastImageAstrometry = null;
				lastImageTable = null;
	       		try {
					FitsIO fio = new FitsIO(lastImagePath);
					HeaderElement header[] = fio.getHeader(0);
					updateImage();
					if (HeaderElement.getByKey(header, "CRVAL1") != null) {
						lastImageWCS = fio.getWCS(0);
						try {
							double c[] = new double[6];
							c[0] = HeaderElement.getByKey(header, "PLATE_A").getAsDouble();
							c[1] = HeaderElement.getByKey(header, "PLATE_B").getAsDouble();
							c[2] = HeaderElement.getByKey(header, "PLATE_C").getAsDouble();
							c[3] = HeaderElement.getByKey(header, "PLATE_D").getAsDouble();
							c[4] = HeaderElement.getByKey(header, "PLATE_E").getAsDouble();
							c[5] = HeaderElement.getByKey(header, "PLATE_F").getAsDouble();
							LocationElement loc0 = new LocationElement(
									HeaderElement.getByKey(header, "PLATE_G").getAsDouble(),
									HeaderElement.getByKey(header, "PLATE_H").getAsDouble(), 1.0
									);
							double res[] = new double[] {
									HeaderElement.getByKey(header, "PLATE_I").getAsDouble() * Constant.ARCSEC_TO_RAD,
									HeaderElement.getByKey(header, "PLATE_J").getAsDouble() * Constant.ARCSEC_TO_RAD
							};
							lastImageAstrometry = new Astrometry(loc0, c, res);
						} catch (Exception exc) {}
					}
				} catch (Exception e1) {
					e1.printStackTrace();
					try {
						updateTable(true);
					} catch (JPARSECException e2) {
						e2.printStackTrace();
					}
				}
	       		updatePanel();
	       		return;
	  		}
	  		if (e.getButton() == MouseEvent.BUTTON3 && (e.getSource() == img || e.getSource() == imgScroll)) {
	  			// Pop up for the image panel
	  			JPopupMenu popupMenu = new JPopupMenu();
	  			JMenu menu1 = new JMenu(Translate.translate(1193));
	 			 JMenuItem menu10 = new JMenuItem("200%");
	  			 JMenuItem menu11 = new JMenuItem("100%");
	  			 JMenuItem menu12 = new JMenuItem("50%");
	  			 JMenuItem menu13 = new JMenuItem(Translate.translate(1190));
	  			 Font f = menu11.getFont();
	  			 f = f.deriveFont((float) (f.getSize() + 6));
	  			 if (scaleMode == 200) menu10.setFont(f);
	  			 if (scaleMode == 100) menu11.setFont(f);
	  			 if (scaleMode == 50) menu12.setFont(f);
	  			 if (scaleMode == 0) menu13.setFont(f);

	  			 menu1.add(menu10);
	  			 menu1.add(menu11);
	  			 menu1.add(menu12);
	  			 menu1.add(menu13);
	  			 menu10.addActionListener(new ActionListener() {
	  				 @Override
	  				public void actionPerformed(ActionEvent e) {
	  					scaleMode = 200;
	  					updatePanel();
	  				}
	  			 });
	  			 menu11.addActionListener(new ActionListener() {
	  				 @Override
	  				public void actionPerformed(ActionEvent e) {
	  					scaleMode = 100;
	  					updatePanel();
	  				}
	  			 });
	  			 menu12.addActionListener(new ActionListener() {
	  				 @Override
	  				public void actionPerformed(ActionEvent e) {
	  					scaleMode = 50;
	  					updatePanel();
	  				}
	  			 });
	  			 menu13.addActionListener(new ActionListener() {
	  				 @Override
	  				public void actionPerformed(ActionEvent e) {
	  					scaleMode = 0;
	  					updatePanel();
	  				}
	  			 });
	  			popupMenu.add(menu1);
	  			JMenu menu2 = new JMenu(Translate.translate(1191));
	  			 JMenuItem menu21 = new JMenuItem(Translate.translate(1192));
	  			 JMenuItem menu22 = new JMenuItem("R");
	  			 JMenuItem menu23 = new JMenuItem("G");
	  			 JMenuItem menu24 = new JMenuItem("B");
	  			 if (colorMode == 0) menu21.setFont(f);
	  			 if (colorMode == 1) menu22.setFont(f);
	  			 if (colorMode == 2) menu23.setFont(f);
	  			 if (colorMode == 3) menu24.setFont(f);

	  			 menu2.add(menu21);
	  			 menu2.add(menu22);
	  			 menu2.add(menu23);
	  			 menu2.add(menu24);
	  			 menu21.addActionListener(new ActionListener() {
	  				 @Override
	  				public void actionPerformed(ActionEvent e) {
	  					 if (colorMode != 0) {
	  						colorMode = 0;
	  						updatePanel();
	  					 }
	  				}
	  			 });
	  			 menu22.addActionListener(new ActionListener() {
	  				 @Override
	  				public void actionPerformed(ActionEvent e) {
	  					 if (colorMode != 1) {
	  						colorMode = 1;
	  						updatePanel();
	  					 }
	  				}
	  			 });
	  			 menu23.addActionListener(new ActionListener() {
	  				 @Override
	  				public void actionPerformed(ActionEvent e) {
	  					 if (colorMode != 2) {
	  						colorMode = 2;
	  						updatePanel();
	  					 }
	  				}
	  			 });
	  			 menu24.addActionListener(new ActionListener() {
	  				 @Override
	  				public void actionPerformed(ActionEvent e) {
	  					 if (colorMode != 3) {
	  						colorMode = 3;
	  						updatePanel();
	  					 }
	  				}
	  			 });
	  			popupMenu.add(menu2);

	  			String add = Translate.translate(1200);
	  			if (!showGrid) add = Translate.translate(1228);
	 			 JMenuItem menu31 = new JMenuItem(Translate.translate(1203) + " ("+add+")");
	  			 menu31.addActionListener(new ActionListener() {
	  				 @Override
	  				public void actionPerformed(ActionEvent e) {
	  					 showGrid = !showGrid;
	  					 updateImage();
	  					 updatePanel();
	  				}
	  			 });
	   			popupMenu.add(menu31);

	   			add = "Log";
	   			if (linearIntensityScale) add = Translate.translate(212);;
				 JMenuItem menu41 = new JMenuItem(Translate.translate(1226) + " ("+add+")");
	  			 menu41.addActionListener(new ActionListener() {
	  				 @Override
	  				public void actionPerformed(ActionEvent e) {
	  					linearIntensityScale = !linearIntensityScale;
	  					if (!linearIntensityScale) {
	  						plev1 = PICTURE_LEVEL.ORIGINAL;
	  						plev2 = PICTURE_LEVEL.LOG_SCALE;
	  					} else {
	  						plev1 = PICTURE_LEVEL.EXP_SCALE;
	  						plev2 = PICTURE_LEVEL.LINEAR_INTERPOLATION;
	  					}
	  					updateImage();
						updatePanel();
	  				}
	  			 });
				if (lastImagePath != null && lastImagePath.endsWith(".fits"))
					popupMenu.add(menu41);

				 JMenuItem menu51 = new JMenuItem(Translate.translate(950));
	  			 menu51.addActionListener(new ActionListener() {
	  				 @Override
	  				public void actionPerformed(ActionEvent e) {
	  					 try {
		  					 String path = FileIO.fileChooser(false);
		  					 if (path != null) {
			  					 if (!Picture.formatSupported(path)) path = path + ".png";
			  					 Picture pic = new Picture(lastImage);
			  					 pic.write(path);
		  					 }
	  					 } catch (Exception exc) {
	  						 exc.printStackTrace();
	  					 }
	  				}
	  			 });
				if (lastImage != null) popupMenu.add(menu51);
				popupMenu.addSeparator();

				 JMenuItem menu61 = new JMenuItem(Translate.translate(1231));
	  			 menu61.addActionListener(new ActionListener() {
	  				 @Override
	  				public void actionPerformed(ActionEvent e) {
	  					 try {
	  						if (optionsPanelShown) return;
	  						final JFrame frame = new JFrame(Translate.translate(1231));
	  						frame.setName("NOT OK");
	  						frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	  						int h = 660 + (getCameras().length - 1) * 140;
	  						frame.setPreferredSize(new Dimension(1000, h));
	  						JPanel panel = createOptionsPanel();
	  						JButton closeButton = new JButton(Translate.translate(234));
	  						closeButton.addActionListener(new ActionListener() {
	  							@Override
	  							public void actionPerformed(ActionEvent e) {
	  								frame.setName("OK");
	  								frame.dispose();
	  							}
	  						});
	  						panel.add(closeButton, "width 10%,span,align center,wrap");
	  						frame.add(panel);
	  						frame.setIconImage(ReadFile.readImageResource(FileIO.DATA_IMAGES_DIRECTORY+"telescope_transparentOK.png"));
	  						frame.pack();
	  						frame.setResizable(false);
	  						frame.setVisible(true);
	  						optionsPanelShown = true;
	  						frame.addWindowListener(new WindowListener() {
	  							@Override
	  							public void windowClosed(WindowEvent e) {
	  								optionsPanelShown = false;
	  								if (frame.getName().equals("OK")) {
		  								setProjectInfo(prName.getText(), prObs.getText(), prDes.getText());
		  								setTelescopeType(TELESCOPE_TYPE.values()[telList.getSelectedIndex()]);
		  								VirtualCamera.DRAW_DSO_TEXTURES = texDSO.isSelected();
		  								VirtualCamera.DRAW_PLANETARY_TEXTURES = texPl.isSelected();
		  								double w[] = getWeatherAlarmConditions();
		  								double humLim = w[0], windSpeedLim = w[1], tempMaxLim = w[2], tempMinLim = w[3];
		  								String err = "";
		  								try { humLim = Double.parseDouble(weaMaxH.getText()); } catch (Exception exc) { err += "MAX_HUM,"; }
		  								try { windSpeedLim = Double.parseDouble(weaMaxW.getText()); } catch (Exception exc) { err += "MAX_WIND_SPEED,"; }
		  								try { tempMaxLim = Double.parseDouble(weaMaxT.getText()); } catch (Exception exc) { err += "MAX_TEMP,"; }
		  								try { tempMinLim = Double.parseDouble(weaMinT.getText()); } catch (Exception exc) { err += "MIN_TEMP,"; }
		  								setWeatherAlarmConditions(humLim, windSpeedLim, tempMaxLim, tempMinLim);
		  								setInterpolationMethod(INTERPOLATION.values()[proInt.getSelectedIndex()]);
		  								setAverageMethod(AVERAGE_METHOD.values()[proAve.getSelectedIndex()]);
		  								setNormalizationMethod(AVERAGE_NORMALIZATION.values()[proNor.getSelectedIndex()]);
		  								setDrizzleMethod(DRIZZLE.values()[proDr.getSelectedIndex()]);
		  								setCombineMethod(COMBINATION_METHOD.values()[proCom.getSelectedIndex()]);
		  								setImageOrientation(IMAGE_ORIENTATION.values()[proOri.getSelectedIndex()]);
		  								try { minArea = Integer.parseInt(sexMA.getText()); } catch (Exception exc) { err += "MIN_AREA,"; }
		  								try { sigma = Integer.parseInt(sexSI.getText()); } catch (Exception exc) { err += "SIGMA,"; }
		  								try { minValueObjType = Double.parseDouble(sexOT.getText()); } catch (Exception exc) { err += "MIN_OBJ_TYPE_VALUE,"; }
		  								try { maxSources = Integer.parseInt(sexMS.getText()); } catch (Exception exc) { err += "MAX_SOURCES,"; }
		  								for (int i=0; i<getCameras().length; i++) {
		  									try { setCameraPositionError(i, Constant.DEG_TO_RAD * Double.parseDouble(camPE[i].getText())); } catch (Exception exc) { err += "CAM_POS_ERR,"; }
		  									try { setCameraMinimumIntervalBetweenShots(i, Integer.parseInt(camIS[i].getText())); } catch (Exception exc) { err += "CAM_INTERVAL_SHOTS,"; }
		  									try { setDarkDir(i, camDD[i].getText()); } catch (Exception exc) { err += "CAM"+(1+i)+"_DARK_DIR,"; }
		  									try { setFlatDir(i, camFD[i].getText()); } catch (Exception exc) { err += "CAM"+(1+i)+"_FLAT_DIR,"; }
		  									try { setOnDir(i, camOD[i].getText()); } catch (Exception exc) { err += "CAM"+(1+i)+"_ON_DIR,"; }
		  									try { setReducedDir(i, camRD[i].getText()); } catch (Exception exc) { err += "CAM"+(1+i)+"_REDUCED_DIR,"; }
		  									try { setStackedDir(i, camSD[i].getText()); } catch (Exception exc) { err += "CAM"+(1+i)+"_STACKED_DIR,"; }
		  									try { setAveragedDir(i, camAD[i].getText()); } catch (Exception exc) { err += "CAM"+(1+i)+"_AVERAGED_DIR,"; }
		  								}
		  								if (!err.equals("")) {
		  									err = err.substring(0, err.length()-1);
		  									JOptionPane.showMessageDialog(null, err, Translate.translate(1267), JOptionPane.WARNING_MESSAGE);
		  								}
	  								}
	  							}
	  							@Override
	  							public void windowActivated(WindowEvent e) { }
	  							@Override
	  							public void windowClosing(WindowEvent e) { }
	  							@Override
	  							public void windowDeactivated(WindowEvent e) { }
	  							@Override
	  							public void windowDeiconified(WindowEvent e) { }
	  							@Override
	  							public void windowIconified(WindowEvent e) { }
	  							@Override
	  							public void windowOpened(WindowEvent e) { }
	  						});
	  					 } catch (Exception exc) {
	  						 exc.printStackTrace();
	  					 }
	  				}
	  			 });
	 			popupMenu.add(menu61);

	  			try {
	  				popupMenu.show(img, e.getX(), e.getY());
	  			} catch (Exception exc) {
	  				exc.printStackTrace();
	  			}
	  			return;
	  		}
	  		if (e.getSource() == img || e.getSource() == imgScroll) {
				if (lastImagePath == null || !lastImagePath.endsWith(".fits") || lastImageWCS == null || lastImageAstrometry == null || lastImageTable == null) return;

				try {
					double d = -1, x = e.getX(), y = e.getY();
					int index = -1;
					for (int i=1; i<lastImageTable.length; i++) {
						double dist = FastMath.hypot(x - Double.parseDouble(lastImageTable[i][0]), y - Double.parseDouble(lastImageTable[i][1]));
						if (dist < d || d == -1) {
							d = dist;
							index = i;
						}
					}
					if (d >= 0 && d < 20) {
						// solvedX[i]+" "+solvedY[i]+" "+solvedFlux[i]+" "+mag+" "+solvedRA[i]+" "+solvedDEC[i]+" "+solvedMag[i]+" "+solvedVar[i]+" "+solvedSp[i]+" "+solvedName[i];
						String sv = Translate.translate(1224);
						String v = lastImageTable[index][7];
						if (v.startsWith("V")) sv = Translate.translate(1223);
						if (v.startsWith("-")) sv = Translate.translate(819).toLowerCase();
						if (lastImageTable[index][4].equals("-1.0") && lastImageTable[index][5].equals("-1.0")) sv = Translate.translate(819).toLowerCase();
						LocationElement loc = lastImageAstrometry.getPlatePosition(Double.parseDouble(lastImageTable[index][0]), Double.parseDouble(lastImageTable[index][1]));
						double rms[] = lastImageAstrometry.getPlatePositionResidual();
						String message[] = new String[] {
								Translate.translate(79)+": "+ DataSet.toString(DataSet.getSubArray(lastImageTable[index], 9, lastImageTable[index].length-1), " "),
								Translate.translate(1218)+": "+lastImageTable[index][0]+", "+lastImageTable[index][1],
								Translate.translate(1219)+": "+Functions.formatRA(Double.parseDouble(lastImageTable[index][4]))+", "+Functions.formatDEC(Double.parseDouble(lastImageTable[index][5])),
								Translate.translate(1220)+": "+Functions.formatRA(loc.getLongitude(), 2)+", "+Functions.formatDEC(loc.getLatitude(), 1),
								Translate.translate(1221)+": "+(float) (rms[0] * Constant.RAD_TO_ARCSEC)+", "+(float) (rms[1] * Constant.RAD_TO_ARCSEC),
								Translate.translate(675)+": "+lastImageTable[index][8],
								Translate.translate(1222)+": "+(int) Double.parseDouble(lastImageTable[index][2])+", "+Functions.formatValue(Double.parseDouble(lastImageTable[index][6]), 3)+", "+Functions.formatValue(Double.parseDouble(lastImageTable[index][3]), 3),
								Translate.translate(33)+" ?: "+sv
						};
						if (lastImageTable[index][4].equals("-1.0") && lastImageTable[index][5].equals("-1.0")) {
							message[2] = Translate.translate(1219)+": -";
							message[6] = Translate.translate(1222)+": "+(int) Double.parseDouble(lastImageTable[index][2])+", -, "+Functions.formatValue(Double.parseDouble(lastImageTable[index][3]), 3);
						}
						if (popup != null) popup.hide();
						PopupFactory popupF = PopupFactory.getSharedInstance();
	  					JTextArea ta = new JTextArea(DataSet.toString(message, FileIO.getLineSeparator()), 8, 30);
	  					Point p = e.getLocationOnScreen();
	  					popup = popupF.getPopup(img, ta, p.x, p.y);
	  					popup.show();
					}
				} catch (Exception exc) {
					exc.printStackTrace();
				}
	  		}
		}

		private void processEvent(ActionEvent e) {
			if (e.getSource() == autoReduce || e.getSource() == focusAssist) {
				if (e.getSource() == autoReduce) {
					setReductionEnabled(autoReduce.isSelected());
					updatePanel();
					return;
				}
				if (!focusAssist.isSelected()) {
					updatePanel();
					return;
				}
				if (lastImagePath == null || lastImagePath.equals("")) {
					focusAssist.setSelected(false);
				} else {
					try {
						if (lastImagePath != null && lastImagePath.endsWith(".fits")) {
							FitsIO fio = new FitsIO(lastImagePath);
							HeaderElement header[] = fio.getHeader(0);
							String imgid = header[HeaderElement.getIndex(header, "IMGID")].getAsString();
							int index = DataSet.getIndex(GenericCamera.IMAGE_IDS_ALL, imgid);
							if (index < 0) {
								if (imgid.equals(IMAGE_ID.ON_SOURCE.name())) imgid = GenericCamera.IMAGE_IDS[IMAGE_ID.ON_SOURCE.ordinal()];
								if (imgid.equals(IMAGE_ID.DARK.name())) imgid = GenericCamera.IMAGE_IDS[IMAGE_ID.DARK.ordinal()];
								if (imgid.equals(IMAGE_ID.FLAT.name())) imgid = GenericCamera.IMAGE_IDS[IMAGE_ID.FLAT.ordinal()];
								index = DataSet.getIndex(GenericCamera.IMAGE_IDS, imgid);
							}
							if (index < 0 && imgid.startsWith("Reduced")) index = 4;
							if (index >= 0) {
								IMAGE_ID imgID = IMAGE_ID.values()[index];
								if (imgID != IMAGE_ID.ON_SOURCE && imgID != IMAGE_ID.TEST && imgID != IMAGE_ID.REDUCED_ON &&
										imgID != IMAGE_ID.STACKED && imgID != IMAGE_ID.AVERAGED) {
									focusAssist.setSelected(false);
								}
							} else {
								focusAssist.setSelected(false);
							}
						}
					} catch (Exception exc) {
						exc.printStackTrace();
					}
				}
				updatePanel();
			}
			if (e.getSource() == reduceButton) {
				try {
					if (lastImagePath != null && lastImagePath.endsWith(".fits")) {
						FitsIO fio = new FitsIO(lastImagePath);
						HeaderElement header[] = fio.getHeader(0);
						String imgid = header[HeaderElement.getIndex(header, "IMGID")].getAsString();
						int index = DataSet.getIndex(GenericCamera.IMAGE_IDS_ALL, imgid);
						if (index < 0) {
							if (imgid.equals(IMAGE_ID.ON_SOURCE.name())) imgid = GenericCamera.IMAGE_IDS[IMAGE_ID.ON_SOURCE.ordinal()];
							if (imgid.equals(IMAGE_ID.DARK.name())) imgid = GenericCamera.IMAGE_IDS[IMAGE_ID.DARK.ordinal()];
							if (imgid.equals(IMAGE_ID.FLAT.name())) imgid = GenericCamera.IMAGE_IDS[IMAGE_ID.FLAT.ordinal()];
							index = DataSet.getIndex(GenericCamera.IMAGE_IDS, imgid);
						}
						if (index < 0 && imgid.startsWith("Reduced")) index = 4;
						int row = DataSet.getIndexContaining(lineTable, lastImagePath);
						if (lineTable[row].startsWith("false")) {
							JOptionPane.showMessageDialog(null, Translate.translate(1205), Translate.translate(1204), JOptionPane.WARNING_MESSAGE);
						} else {
							if (index >= 0) {
								reduce(IMAGE_ID.values()[index], new String[] {lastImagePath}, getCameraIndex(lastImagePath), null);
								row = DataSet.getIndexContaining(lineTable, lastImagePath);
								lineTable[row] = lineTable[row].substring(0, lineTable[row].lastIndexOf(SEPARATOR) + 1) + "0";
								updatePanel();
							}
						}
					}
				} catch (Exception exc) {
					exc.printStackTrace();
				}
			}
			if (e.getSource() == deleteButton) {
				try {
					if (lastImagePath == null) return;
					int ask = JOptionPane.showConfirmDialog(null, DataSet.replaceAll(Translate.translate(1287), "%file", lastImagePath, true), Translate.translate(1286), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
					if (ask == JOptionPane.YES_OPTION) {
						int camIndex = getCameraIndex(lastImagePath);
						String ondir = workingDir + obsDir + cameraDir[camIndex] + onDir[camIndex] + FileIO.getFileNameFromPath(lastImagePath);
						String reduceddir = workingDir + obsDir + cameraDir[camIndex] + reducedDir[camIndex] + FileIO.getFileNameFromPath(lastImagePath);
						String stackeddir = workingDir + obsDir + cameraDir[camIndex] + stackedDir[camIndex] + FileIO.getFileNameFromPath(lastImagePath);
						String averageddir = workingDir + obsDir + cameraDir[camIndex] + averagedDir[camIndex] + FileIO.getFileNameFromPath(lastImagePath);
						if (lastImagePath.equals(ondir)) {
							FileIO.deleteFile(reduceddir);
						} else {
							if (lastImagePath.equals(reduceddir)) {
								int index = DataSet.getIndexContaining(lineTable, ondir);
								if (index >= 0 && lineTable[index].endsWith("0")) lineTable[index] = lineTable[index].substring(0, lineTable[index].lastIndexOf("0")) + "1";
							} else {
								if (lastImagePath.equals(stackeddir) || lastImagePath.equals(averageddir)) {
									FileIO.deleteFile(stackeddir);
								} else {
									String fitsid = getFitsMainData(lastImagePath);
									String outputFile = FileIO.getDirectoryFromPath(lastImagePath)+"super_"+fitsid+".fits";
									File master = new File(outputFile);
									if (master.exists()) FileIO.deleteFile(outputFile);
								}
							}
						}
						FileIO.deleteFile(lastImagePath);

						lastImagePath = null;
						lastImage = null;
						lastImageWCS = null;
						lastImageAstrometry = null;
						lastImageTable = null;
						updatePanel();
					}
				} catch (Exception exc) {
					exc.printStackTrace();
				}
			}
		}
	}
}
