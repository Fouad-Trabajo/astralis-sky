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
package jparsec.vo;

import java.io.File;
import java.util.ArrayList;

import jparsec.ephem.Functions;
import jparsec.graph.DataSet;
import jparsec.io.ApplicationLauncher;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.io.WriteFile;
import jparsec.io.image.FitsIO;
import jparsec.io.image.Picture;
import jparsec.math.Constant;
import jparsec.math.FastMath;
import jparsec.math.MeasureElement;
import jparsec.math.Table;
import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Logger.LEVEL;

/**
 * A class to run SExtractor to extract sources from images. In the execution
 * the adequate file default.param is created. Another required file is the
 * machine.config, that can be created with {@linkplain #createMachineConfigFile(String, double, double, double, double, double, int, int)},
 * although if not present a default one is created.
 * Note that in the output positions for the stars from SExtractor the
 * center of the first pixel of the image is at (1, 1), not (0, 0).
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class SExtractor {

	/**
	 * Working directory.
	 */
	public String workingDir;
	/**
	 * Configuration file name, should be located in the working directory.
	 * An additional file named default.param should also be there.
	 */
	public String configFile;

	private MeasureElement[][] sources;
	private double maxFlux, background;

	/**
	 * Default constructor.
	 * @param dir Path to the working directory.
	 * @param config Name of the configuration file (in the
	 * working directory), without path. If null the default
	 * machine.config name will be used. In case it does not
	 * exists, a default one will be created.
	 */
	public SExtractor(String dir, String config)
	{
		if (!dir.equals("") && !dir.endsWith(FileIO.getFileSeparator())) dir += FileIO.getFileSeparator();
		this.workingDir = dir;
		this.configFile = config;
		if (config == null) configFile = "machine.config";
	}

	/**
	 * Process a given .fits file. The method will accept a jpg/png image
	 * and will convert it to fits before starting, using the green channel.
	 * @param file Name of/path to the .fits file.
	 * @throws JPARSECException If an error occurs.
	 */
	public void execute(String file)
	throws JPARSECException {
		execute(file, -1);
	}

	/**
	 * Process a given .fits file. The method will accept a jpg/png image
	 * and will convert it to fits before starting, using the RGB channels
	 * and transforming the image to gray scale.
	 * @param file Name of/path to the .fits file.
	 * @param maxSources Maximum number of sources to return. Set to -1 to
	 * return all sources found.
	 * @throws JPARSECException If an error occurs.
	 */
	public void execute(String file, int maxSources)
	throws JPARSECException {
		// Basic support for non-fits images
		if (file.toLowerCase().endsWith(".png") || file.toLowerCase().endsWith(".jpg") ||
				file.toLowerCase().endsWith(".bmp") || file.toLowerCase().endsWith(".gif") ||
				file.toLowerCase().endsWith(".pgm")) {
			try {
				String ffile = workingDir + FileIO.getFileNameFromPath(file);
				Picture pic = new Picture(ffile);
				pic.toGrayScale();
				FitsIO fio = new FitsIO(pic.getImageAsByteArray(1));
				String path = ffile.substring(0, ffile.lastIndexOf("."))+".fits";
				fio.write(0, path);
				File out = new File(path);
				while (!out.exists()) {
					Thread.sleep(20);
				}
				file = path;
			} catch (Exception exc) {
				exc.printStackTrace();
			}
		}

		String flags = "";

		if (!FileIO.exists(workingDir+"default.param")) {
			String defaultParam[] = new String[] {
					"X_IMAGE",
					"Y_IMAGE",
					"ERRX2_IMAGE",
					"ERRY2_IMAGE",
					"CLASS_STAR", // 1 for star, 0 for extended object
					"A_IMAGE",
					"B_IMAGE",
					"MAG_ISO",
					"MAGERR_ISO",
					"FLUX_ISO",
					"FLUXERR_ISO",
					"FLUX_MAX",
					"XMAX_IMAGE",
					"XMIN_IMAGE",
					"YMAX_IMAGE",
					"YMIN_IMAGE"
			};
			WriteFile.writeAnyExternalFile(workingDir+"default.param", defaultParam);
		}
		if (!FileIO.exists(workingDir+"default.nnw")) {
			String defaultNNW[] = new String[] {
					"NNW",
					"# Neural Network Weights for the SExtractor star/galaxy classifier (V1.3)",
					"# inputs:	9 for profile parameters + 1 for seeing.",
					"# outputs:	``Stellarity index'' (0.0 to 1.0)",
					"# Seeing FWHM range: from 0.025 to 5.5'' (images must have 1.5 < FWHM < 5 pixels)",
					"# Optimized for Moffat profiles with 2<= beta <= 4.",
					"		",
					" 3 10 10  1",
					" ",
					"-1.56604e+00 -2.48265e+00 -1.44564e+00 -1.24675e+00 -9.44913e-01 -5.22453e-01  4.61342e-02  8.31957e-01  2.15505e+00  2.64769e-01",
					" 3.03477e+00  2.69561e+00  3.16188e+00  3.34497e+00  3.51885e+00  3.65570e+00  3.74856e+00  3.84541e+00  4.22811e+00  3.27734e+00",
					" ",
					"-3.22480e-01 -2.12804e+00  6.50750e-01 -1.11242e+00 -1.40683e+00 -1.55944e+00 -1.84558e+00 -1.18946e-01  5.52395e-01 -4.36564e-01 -5.30052e+00",
					" 4.62594e-01 -3.29127e+00  1.10950e+00 -6.01857e-01  1.29492e-01  1.42290e+00  2.90741e+00  2.44058e+00 -9.19118e-01  8.42851e-01 -4.69824e+00",
					"-2.57424e+00  8.96469e-01  8.34775e-01  2.18845e+00  2.46526e+00  8.60878e-02 -6.88080e-01 -1.33623e-02  9.30403e-02  1.64942e+00 -1.01231e+00",
					" 4.81041e+00  1.53747e+00 -1.12216e+00 -3.16008e+00 -1.67404e+00 -1.75767e+00 -1.29310e+00  5.59549e-01  8.08468e-01 -1.01592e-02 -7.54052e+00",
					" 1.01933e+01 -2.09484e+01 -1.07426e+00  9.87912e-01  6.05210e-01 -6.04535e-02 -5.87826e-01 -7.94117e-01 -4.89190e-01 -8.12710e-02 -2.07067e+01",
					"-5.31793e+00  7.94240e+00 -4.64165e+00 -4.37436e+00 -1.55417e+00  7.54368e-01  1.09608e+00  1.45967e+00  1.62946e+00 -1.01301e+00  1.13514e-01",
					" 2.20336e-01  1.70056e+00 -5.20105e-01 -4.28330e-01  1.57258e-03 -3.36502e-01 -8.18568e-02 -7.16163e+00  8.23195e+00 -1.71561e-02 -1.13749e+01",
					" 3.75075e+00  7.25399e+00 -1.75325e+00 -2.68814e+00 -3.71128e+00 -4.62933e+00 -2.13747e+00 -1.89186e-01  1.29122e+00 -7.49380e-01  6.71712e-01",
					"-8.41923e-01  4.64997e+00  5.65808e-01 -3.08277e-01 -1.01687e+00  1.73127e-01 -8.92130e-01  1.89044e+00 -2.75543e-01 -7.72828e-01  5.36745e-01",
					"-3.65598e+00  7.56997e+00 -3.76373e+00 -1.74542e+00 -1.37540e-01 -5.55400e-01 -1.59195e-01  1.27910e-01  1.91906e+00  1.42119e+00 -4.35502e+00",
					"",
					"-1.70059e+00 -3.65695e+00  1.22367e+00 -5.74367e-01 -3.29571e+00  2.46316e+00  5.22353e+00  2.42038e+00  1.22919e+00 -9.22250e-01 -2.32028e+00",
					"",
					"",
					"0.00000e+00",
					"1.00000e+00 ",
			};
			WriteFile.writeAnyExternalFile(workingDir+"default.nnw", defaultNNW);
		}

		// Ensure there's a machine.config file in the working dir
		File config = new File(workingDir+configFile);
		if (!config.exists())
			WriteFile.writeAnyExternalFile(workingDir+configFile, SExtractor.DEFAULT_MACHINE_CONFIG);

		String command = "sextractor "+flags+file+" -c "+configFile;
		Logger.log(LEVEL.TRACE_LEVEL1, "Command to execute: "+command);
		Process p = null;
		if (workingDir.equals("")) {
			String pa = FileIO.getPath(true);
			if (pa.endsWith("jparsec/io/")) pa = pa.substring(0, pa.lastIndexOf("jparsec/io/"));
			p = ApplicationLauncher.executeCommand(command, null, new File(pa));
		} else {
			p = ApplicationLauncher.executeCommand(command, null, new File(workingDir));
		}

		long t0 = System.currentTimeMillis();
		while (true) {
			try {
				long t = System.currentTimeMillis();
				if (t-t0 > 2000) break;
				p.exitValue();
				break;
				// p.waitFor(); // This can lock the system ... !
			} catch (Exception e) {
				//e.printStackTrace();
			}
			try { Thread.sleep(100); } catch (InterruptedException e) {}
		}
		String out = ApplicationLauncher.getConsoleErrorOutputFromProcess(p);
		String[] array = DataSet.toStringArray(out, FileIO.getLineSeparator());
		Logger.log(LEVEL.TRACE_LEVEL3, "Error output: "+out);

		//boolean start = false, end = false;
		ArrayList<MeasureElement[]> sou = new ArrayList<MeasureElement[]>();
		this.maxFlux = 0.0;
		for (int i=0; i<array.length; i++)
		{
			if (array[i].trim().indexOf("Background") > 0) {
				int a = array[i].trim().indexOf("Background");
				background = Double.parseDouble(FileIO.getField(2, array[i].trim().substring(a), " ", true));
				break;
			}
/*			if (array[i].trim().startsWith("1 ")) start = true;
			if (array[i].trim().startsWith("Objects")) end = true;

			if (start && !end) {
				String line = array[i].trim();
				double x = Double.parseDouble(FileIO.getField(2, line, " ", true));
				double y = Double.parseDouble(FileIO.getField(3, line, " ", true));
				double s = Double.parseDouble(FileIO.getField(4, line, " ", true));
				double f = Double.parseDouble(FileIO.getField(6, line, " ", true));
				sou.add(new double[] {x, y, s, f});
				if (f > maxFlux) maxFlux = f;

			}
*/
		}

		array = DataSet.arrayListToStringArray(ReadFile.readAnyExternalFile(workingDir + "coord.cat"));
		for (int i=0; i<array.length; i++)
		{
			String line = array[i].trim();
			double x = Double.parseDouble(FileIO.getField(1, line, " ", true));
			double y = Double.parseDouble(FileIO.getField(2, line, " ", true));
			double dx2 = Double.parseDouble(FileIO.getField(3, line, " ", true));
			double dy2 = Double.parseDouble(FileIO.getField(4, line, " ", true));
			double type = 1;
			try { type = Double.parseDouble(FileIO.getField(5, line, " ", true)); } catch (Exception exc) {} // can be nan sometimes !
			double a = Double.parseDouble(FileIO.getField(6, line, " ", true));
			double b = Double.parseDouble(FileIO.getField(7, line, " ", true));
			double f = Double.parseDouble(FileIO.getField(10, line, " ", true));
			double df = Double.parseDouble(FileIO.getField(11, line, " ", true));
			double m = Double.parseDouble(FileIO.getField(8, line, " ", true));
			double dm = Double.parseDouble(FileIO.getField(9, line, " ", true));
			double fm = Double.parseDouble(FileIO.getField(12, line, " ", true));
			double ap = Double.parseDouble(FileIO.getField(13, line, " ", true))-Double.parseDouble(FileIO.getField(14, line, " ", true));
			double bp = Double.parseDouble(FileIO.getField(15, line, " ", true))-Double.parseDouble(FileIO.getField(16, line, " ", true));
			if (x == 0 && y == 0 && f == 0) {
				System.out.println("Your version of SExtractor seems buggy. Please update it. Workaround used, but results WILL NOT BE COMPLETELY CORRECT");
				x = (Double.parseDouble(FileIO.getField(13, line, " ", true))+Double.parseDouble(FileIO.getField(14, line, " ", true)))*0.5;
				y = (Double.parseDouble(FileIO.getField(15, line, " ", true))+Double.parseDouble(FileIO.getField(16, line, " ", true)))*0.5;
				f = fm;
				if (type == 0) type = 1;
			}
			sou.add(new MeasureElement[] {
					new MeasureElement(x, Math.sqrt(dx2), "pix"),
					new MeasureElement(y, Math.sqrt(dy2), "pix"),
					new MeasureElement(a, b, "pix"),
					new MeasureElement(f, df, "flux"),
					new MeasureElement(m, dm, "mag"),
					new MeasureElement(type, 0, "class"),
					new MeasureElement(fm, 0, "maxFlux"),
					new MeasureElement(ap, bp, "pix"),
					});
			if (f > maxFlux) maxFlux = f;
		}

		int n = sou.size();
		if (maxSources > 0 && maxSources < n) n = maxSources;
		this.sources = new MeasureElement[n][8];
		int index = -1;
		for (int i=0; i<n; i++)
		{
			double max = -1;
			for (int j = 0; j<sou.size(); j++) {
				double val = Double.parseDouble(sou.get(j)[3].value);
				if (max == -1 || val > max) {
					max = val;
					index = j;
				}
			}
			if (max == -1) throw new JPARSECException("ERROR!");
			this.sources[i] = sou.get(index);
			sou.remove(index);
		}
	}

	/**
	 * Removes the temporal files machine.config (optionally), default.param, default.nnw, and coord.cat
	 * from the working directory.
	 * @param alsoConfig True to remove also the configuration file.
	 */
	public void removeTemporalFiles(boolean alsoConfig) {
		String file1 = workingDir+"default.param";
		String file2 = workingDir+"default.nnw";
		String file3 = workingDir+"coord.cat";
		String file4 = workingDir+this.configFile;
		try { FileIO.deleteFile(file1); } catch (Exception exc) {}
		try { FileIO.deleteFile(file2); } catch (Exception exc) {}
		try { FileIO.deleteFile(file3); } catch (Exception exc) {}
		if (alsoConfig) try { FileIO.deleteFile(file4); } catch (Exception exc) {}
	}

	/**
	 * Return the number of sources detected.
	 * @return Number of sources.
	 */
	public int getNumberOfSources()
	{
		return this.sources.length;
	}
	/**
	 * Return the x position of some source. First pixel in the image is 1.
	 * @param s The source index, starting from 0.
	 * @return The x position in pixels.
	 */
	public MeasureElement getX(int s)
	{
		return this.sources[s][0];
	}
	/**
	 * Return the y position of some source. First pixel in the image is 1.
	 * @param s The source index, starting from 0.
	 * @return The y position in pixels.
	 */
	public MeasureElement getY(int s)
	{
		return this.sources[s][1];
	}
	/**
	 * Return the width of some source.
	 * @param s The source index, starting from 0.
	 * @return The width in pixels.
	 */
	public double getWidth(int s)
	{
		return Double.parseDouble(this.sources[s][2].value);
	}
	/**
	 * Return the height of some source.
	 * @param s The source index, starting from 0.
	 * @return The height in pixels.
	 */
	public double getHeight(int s)
	{
		return this.sources[s][2].error;
	}
	/**
	 * Return the width of some source as the number of pixels
	 * where there is detection.
	 * @param s The source index, starting from 0.
	 * @return The width in pixels.
	 */
	public int getDetectionWidth(int s)
	{
		return (int) Double.parseDouble(this.sources[s][7].value);
	}
	/**
	 * Return the height of some source as the number of pixels
	 * where there is detection.
	 * @param s The source index, starting from 0.
	 * @return The height in pixels.
	 */
	public int getDetectionHeight(int s)
	{
		return (int) this.sources[s][7].error;
	}

	/**
	 * Return the flux of some source, corresponding to the FLUX_ISO
	 * keyword of SExtractor.
	 * @param s The source index, starting from 0.
	 * @return The flux (FLUX_ISO).
	 */
	public MeasureElement getFlux(int s)
	{
		return this.sources[s][3];
	}
	/**
	 * Return the magnitude of some source, corresponding to the MAG_ISO
	 * keyword of SExtractor.
	 * @param s The source index, starting from 0.
	 * @return The magnitude (MAG_ISO).
	 */
	public MeasureElement getMagnitude(int s)
	{
		return this.sources[s][4];
	}
	/**
	 * Return the object class of some source.
	 * @param s The source index, starting from 0.
	 * @return The class: close to 1 for point-like (star), close to 0 for extended.
	 */
	public double getClass(int s)
	{
		return Double.parseDouble(this.sources[s][5].value);
	}
	/**
	 * Return the object's peak intensity of some source.
	 * @param s The source index, starting from 0.
	 * @return The peak intensity.
	 */
	public double getPeakIntensity(int s)
	{
		return Double.parseDouble(this.sources[s][6].value);
	}

	/**
	 * Returns the maximum flux (FLUX_ISO) in all detected sources.
	 * Since sources are sorted by flux, this will be the flux of
	 * the first source found.
	 * @return Maximum flux.
	 */
	public double getMaxFlux()
	{
		return this.maxFlux;
	}

	/**
	 * Returns the background flux.
	 * @return Background flux.
	 */
	public double getBackgroundFlux()
	{
		return this.background;
	}

	/**
	 * Returns a list of sources detected. Starting from third line a table
	 * with x position, y, size w x h (pixels), flux, and class is given, including errors.
	 */
	@Override
	public String toString() {
		String sep = FileIO.getLineSeparator();
		StringBuffer out = new StringBuffer("Detected "+getNumberOfSources()+" sources over a background flux of "+getBackgroundFlux()+". Maximum flux: "+getMaxFlux()+sep);
		String c[] = new String[] {"X (pixels)", "Y (pixels)", "size (pixels)", "Flux", "Class"};
		int w = 25;
		for (int j = 0; j < c.length; j ++) {
			String f = FileIO.addSpacesAfterAString(c[j], w);
			out.append(f);
		}
		out.append(sep);
		for (int i=0; i<getNumberOfSources(); i++) {
			for (int j = 0; j< c.length; j++) {
				if (j == 4) {
					c[j] = sources[i][5].value;
				} else {
					c[j] = sources[i][j].toString(true, false);
				}
				if (j == 2) {
					c[j] = DataSet.replaceAll(c[j], " (", "x", true);
					c[j] = DataSet.replaceAll(c[j], ")", "", true);
				}
			}
			for (int j = 0; j < c.length; j ++) {
				String f = FileIO.addSpacesAfterAString(c[j], w);
				out.append(f);
			}
			out.append(sep);
		}
		return out.toString();
	}

	/**
	 * Returns a list of sources detected.
	 * @param ndec Number of decimal places for the numerical values,
	 * without considering the significant digits based on the errors
	 * of the different parameters as in the {@linkplain #toString()} method.
	 * @return A string representation of the results of the source extraction.
	 */
	public String toString(int ndec) {
		String sep = FileIO.getLineSeparator();
		StringBuffer out = new StringBuffer("Detected "+getNumberOfSources()+" sources over a background flux of "+getBackgroundFlux()+". Maximum flux: "+getMaxFlux()+sep);
		String c[] = new String[] {"X (pixels)", "Y (pixels)", "size (pixels)", "Flux"};
		int w = 25;
		for (int j = 0; j < 4; j ++) {
			String f = FileIO.addSpacesAfterAString(c[j], w);
			out.append(f);
		}
		out.append(sep);
		for (int i=0; i<getNumberOfSources(); i++) {
			for (int j = 0; j< c.length; j++) {
				c[j] = sources[i][j].toString(true, ndec);
				if (sources[i][j].unit != null && !sources[i][j].unit.equals(""))
					c[j] = c[j].substring(0, c[j].indexOf(sources[i][j].unit)).trim();
				if (j == 2) {
					c[j] = DataSet.replaceAll(c[j], " (", "x", true);
					c[j] = DataSet.replaceAll(c[j], ")", "", true);
				}
			}
			for (int j = 0; j < c.length; j ++) {
				String f = FileIO.addSpacesAfterAString(c[j], w);
				out.append(f);
			}
			out.append(sep);
		}
		return out.toString();
	}

	/**
	 * Returns a list of sources detected.
	 * @param ndec Number of decimal places for the numerical values,
	 * without considering the significant digits based on the errors
	 * of the different parameters as in the {@linkplain #toString()} method.
	 * @param minArea Minimum area of the star/object in pixels to consider it
	 * as detected. Note this is the same as the input parameter to create the
	 * machine.config file, but it is allowed here also since sometimes
	 * SExtractor returns sources below the provided minimum area.
	 * @return A string representation of the results of the source extraction.
	 */
	public String toString(int ndec, double minArea) {
		String sep = FileIO.getLineSeparator();
		StringBuffer out = new StringBuffer("");
		String c[] = new String[] {"X (pixels)", "Y (pixels)", "size (pixels)", "Flux"};
		int w = 25;
		for (int j = 0; j < 4; j ++) {
			String f = FileIO.addSpacesAfterAString(c[j], w);
			out.append(f);
		}
		out.append(sep);
		int n = 0;
		for (int i=0; i<getNumberOfSources(); i++) {
			boolean skip = false;
			for (int j = 0; j< c.length; j++) {
				c[j] = sources[i][j].toString(true, ndec);
				if (sources[i][j].unit != null && !sources[i][j].unit.equals(""))
					c[j] = c[j].substring(0, c[j].indexOf(sources[i][j].unit)).trim();
				if (j == 2) {
					c[j] = DataSet.replaceAll(c[j], " (", "x", true);
					c[j] = DataSet.replaceAll(c[j], ")", "", true);
					double size1 = Double.parseDouble(FileIO.getField(1, c[j], "x", false));
					double size2 = Double.parseDouble(FileIO.getField(2, c[j], "x", false));
					if (size1 * size2 < minArea) skip = true;
				}
			}
			if (skip) continue;
			n++;
			for (int j = 0; j < c.length; j ++) {
				String f = FileIO.addSpacesAfterAString(c[j], w);
				out.append(f);
			}
			out.append(sep);
		}
		StringBuffer out0 = new StringBuffer("Detected "+n+" sources over a background flux of "+getBackgroundFlux()+". Maximum flux: "+getMaxFlux()+sep);
		return out0.toString()+out.toString();
	}

	private static final String[] DEFAULT_MACHINE_CONFIG = new String[] {
		"# Default configuration file for SExtractor",
		"",
		"#-------------------------------- Catalog ------------------------------------",
		"",
		"CATALOG_NAME	coord.cat	# name of the output catalog",
		"CATALOG_TYPE	ASCII		# no header,  _head includes a header with catalog content",
		"",
		"PARAMETERS_NAME	default.param	# name of the file containing catalog contents",
		"",
		"#------------------------------- Extraction ----------------------------------",
		"",
		"DETECT_TYPE	CCD		# CCD or PHOTO",
		"FLAG_IMAGE	flag.fits	# filename for an input FLAG-image",
		"DETECT_MINAREA  3	# minimum number of pixels for a detection",
		"DETECT_THRESH 	5	# n times rms of background for detection",
		"ANALYSIS_THRESH	1		# '1' relative to background rms, '2' zeropoint magnitude",
		"",
		"FILTER		N		# apply filter for detection (\"Y\" or \"N\")?",
		"FILTER_NAME	fil.conv	# name of the file containing the filter",
		"",
		"DEBLEND_NTHRESH	32		# Number of deblending sub-thresholds",
		"DEBLEND_MINCONT	0.005		# float, 1 no deblending, 0 local peaks seperate detections",
		"",
		"CLEAN		Y		# Clean spurious detections? (Y or N)?",
		"CLEAN_PARAM	4.0		# Cleaning efficiency",
		"",
		"",
		"#------------------------------ Photometry -----------------------------------",
		"",
		"MASK_TYPE	CORRECT		# type of detection MASKing: can be one of",
		"				# NONE: no masking",
		"				# BLANK: neighbor pixels set to zero",
		"				# CORRECT:  replace values of pixels sym. rst source",
		"",
		"PHOT_APERTURES	5,10,20,30		# MAG_APER aperture diameter(s) in pixels",
		"PHOT_AUTOPARAMS	2.5, 3.5	# MAG_AUTO parameters: <Kron_fact>,<min_radius>",
		"",
		"SATUR_LEVEL	16000.0		# level (in ADUs) at which arises saturation",
		"",
		"MAG_ZEROPOINT	0.0		# magnitude zero-point",
		"MAG_GAMMA	4.0		# gamma of emulsion (for photographic scans)",
		"GAIN	        3	# detector gain in e-/ADU, 3 for 12 bit, 0.8 for 14 bit dlsr.",
		"PIXEL_SCALE	0	# size of pixel in arcsec (0=use FITS WCS info).",
		"",
		"#------------------------- Star/Galaxy Separation ----------------------------",
		"",
		"SEEING_FWHM	1	# stellar FWHM in arcsec",
		"STARNNW_NAME	default.nnw	# Neural-Network_Weight table filename",
		"",
		"#------------------------------ Background -----------------------------------",
		"",
		"BACK_SIZE	32		# Background mesh: <size> or <width>,<height>",
		"BACK_FILTERSIZE	1		# Background filter: <size> or <width>,<height>",
		"",
		"BACKPHOTO_TYPE	GLOBAL		# can be \"GLOBAL\" or \"LOCAL\" (*)",
		"BACKPHOTO_THICK	24		# thickness of the background LOCAL annulus (*)",
		"",
		"#------------------------------ Check Image ----------------------------------",
		"",
		"CHECKIMAGE_TYPE	NONE		# can be one of \"NONE\", \"BACKGROUND\",",
		"				# \"MINIBACKGROUND\", \"-BACKGROUND\", \"OBJECTS\",",
		"				# \"-OBJECTS\", \"SEGMENTATION\", \"APERTURES\",",
		"				# or \"FILTERED\" (*)",
		"CHECKIMAGE_NAME	check.fits	# Filename for the check-image (*)",
		"",
		"#--------------------- Memory (change with caution!) -------------------------",
		"",
		"MEMORY_OBJSTACK	1000		# max number of objects in stack",
		"MEMORY_PIXSTACK	1000000		# max number of pixels in stack",
		"MEMORY_BUFSIZE	1000		# number of lines in image",
		"",
		"#----------------------------- Miscellaneous ---------------------------------",
		"",
		"VERBOSE_TYPE	NORMAL		# How much the tractor talks back",
		"                                # QUIET:  Run silent",
		"                                # NORMAL:  disp. warnings",
		"                                # FULL:  disp. more complete info. &principle parameters"
	};

	/**
	 * Creates a compatible machine.config file in a given directory to use it with SExtractor. There
	 * are certain relations between the gain and the magnitude 0 point:
	 * <pre>
	 * Image type         Gain       Magnitude-0
	 * -----------------------------------------
	 *
	 * shot in counts/s   gain*texp  mag0 for 1s exposure (constant value) = mag0(1s)
	 * One shot (counts)  gain       mag0 for that (total) texp = mag0(1s) + 2.5 log10 (texp)
	 * Sum of N frames    gain       mag0(1s) + 2.5 log10 (total texp)
	 * Average N frames   gain*N     mag0(1s) + 2.5 log10 (averaged texp)
	 * Median of N frames 2*gain*N/3 mag0(1s) + 2.5 log10 (averaged texp)
	 * </pre>
	 * Another table with useful info about some detectors:
	 * <pre>
	 * Detector           Saturation Gain (400 ISO)
	 * --------------------------------------------
	 *
	 * 40D                12900      0.84 (Gain at ISO x = (400/x) * Gain400)
	 * 50D                8700       0.57
	 * 5D                 15800      3.99
	 * 5D Mark II         15500      1.01
	 * 1D Mark III                   1.2
	 * QSI 532            85000      1.31
	 * Nikon D200                    2.0
	 * Nikon D70                     2.98
	 * Nikon D50                     3.72
	 * Nikon D3                      2.1
	 * Nikon D300                    0.67
	 * </pre>
	 *
	 * @param path The path to the directory where machine.config will be created.
	 * @param saturation Saturation level in ADUs. For instance 16386 = 2^14 for a 14 bit detector,
	 * although saturation always occurs before that level.
	 * @param gain Detector gain in e-/ADU. Can be set to 12288/2^14 for a 14 bit detector and ISO 400.
	 * @param pixScale The pixel size in arcseconds per pixel. Set to 0 to obtain it from the WCS
	 * data in the fits file.
	 * @param seeing The seeing in arcseconds.
	 * @param mag0 The magnitude 0 point for photometry. Depends on instrument, filter, and camera.
	 * In case of an image with total counts for a given time in seconds (texp) you should add to
	 * that value the result of 2.5 log10 (texp).
	 * @param minArea The minimum number of pixels above sigma to consider a source as detected. Set it between
	 * 3 and 10 depending on how sensitive you want the source detection algorithm to be.
	 * @param sigma How much times the pixels should be above the background emission to consider a
	 * source as detected. Set it between 5 and 10.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void createMachineConfigFile(String path, double saturation, double gain, double pixScale, double seeing,
			double mag0, int minArea, int sigma) throws JPARSECException {
		if (!path.endsWith(FileIO.getFileSeparator())) path += FileIO.getFileSeparator();
		File f = new File(path);
		if (!f.isDirectory()) throw new JPARSECException("The path "+path+" is not a directory.");

		String changeSatur = "SATUR_LEVEL	16000.0", changePixScale = "PIXEL_SCALE	0", changeSeeing = "SEEING_FWHM	1",
				changeGain = "GAIN	        3", changeMag0 = "MAG_ZEROPOINT	0.0", changeMinArea = "DETECT_MINAREA  3", changeSigma = "DETECT_THRESH 	5";
		String changedSatur = "SATUR_LEVEL	"+Functions.formatValue(saturation, 1), changedPixScale = "PIXEL_SCALE	"+Functions.formatValue(pixScale, 5),
				changedSeeing = "SEEING_FWHM	"+Functions.formatValue(seeing, 5), changedGain = "GAIN	        "+Functions.formatValue(gain, 5),
				changedMag0 = "MAG_ZEROPOINT	"+Functions.formatValue(mag0, 5), changedMinArea = "DETECT_MINAREA  "+minArea, changedSigma = "DETECT_THRESH 	"+sigma;
		String text[] = DEFAULT_MACHINE_CONFIG.clone();

		int index = DataSet.getIndexStartingWith(text, changeSatur);
		text[index] = DataSet.replaceAll(text[index], changeSatur, changedSatur, true);
		index = DataSet.getIndexStartingWith(text, changePixScale);
		text[index] = DataSet.replaceAll(text[index], changePixScale, changedPixScale, true);
		index = DataSet.getIndexStartingWith(text, changeSeeing);
		text[index] = DataSet.replaceAll(text[index], changeSeeing, changedSeeing, true);
		index = DataSet.getIndexStartingWith(text, changeGain);
		text[index] = DataSet.replaceAll(text[index], changeGain, changedGain, true);
		index = DataSet.getIndexStartingWith(text, changeMag0);
		text[index] = DataSet.replaceAll(text[index], changeMag0, changedMag0, true);
		index = DataSet.getIndexStartingWith(text, changeMinArea);
		text[index] = DataSet.replaceAll(text[index], changeMinArea, changedMinArea, true);
		index = DataSet.getIndexStartingWith(text, changeSigma);
		text[index] = DataSet.replaceAll(text[index], changeSigma, changedSigma, true);

		WriteFile.writeAnyExternalFile(path + "machine.config", text);
	}

	/**
	 * Performs a cross match of the sources in this instance with the sources in another instance.
	 * @param sex The second SExtractor instance with their sources also solved.
	 * @param maxError The maximum error in pixels as a tolerance for the cross matching, for instance 0.5.
	 * @param sameImageOrientationAndField True in case the solved sources corresponds to images with
	 * the same field and orientation, so that a simple and fast cross match can be performed, false
	 * otherwise.
	 * @param nsources The number of sources to use for the cross matching. Set to 0 or negative to take
	 * all of them, or any other value equal or greater than 4 for the brightest n sources. Other values
	 * will launch an exception in case the images are not taken in the exactly same field and orientation.
	 * @return Value returned is null in case the sources are not solved in one of the instances or the number
	 * of sources is 0. Otherwise, an integer array is returned so that its first value gives the index in
	 * the second instance (provided in the call to this method) of the first source in this instance.
	 * @throws JPARSECException If an error occurs.
	 */
	public int[] crossMatch(SExtractor sex, double maxError, boolean sameImageOrientationAndField, int nsources) throws JPARSECException {
		if (sources == null) return null;
		if (nsources <= 0) nsources = this.getNumberOfSources();
		if (nsources < 4 && !sameImageOrientationAndField) throw new JPARSECException("Cannot use less than 4 sources in this case");
		if (nsources == 0 || sex.sources == null || sex.getNumberOfSources() == 0) return null;

		int n = sex.getNumberOfSources();
		n = Math.min(n, getNumberOfSources());
		if (nsources > 0) n = Math.min(n, nsources);
		double x[] = new double[n], y[] = new double[n];
		for (int i=0; i<n; i++) {
			x[i] = sex.getX(i).getValue();
			y[i] = sex.getY(i).getValue();
		}
		int id[] = new int[n]; // Identify index id[...] with catalog
		for (int i = 0; i < id.length; i++) { id[i] = -1; }

		if (sameImageOrientationAndField) {
			for (int i = 0; i < id.length; i++) {				
				id[i] = identifyStar(this, x, y, i, maxError);
			}
			return id;
		}

		// Make first triangle from brightest source
		int tri = 0, iter = 0;
		scale = 0;
		angle = 0;
		nobs = 0;
		while (true) {
			if (id[tri] < 0 || id[tri+1] < 0 || id[tri+2] < 0) {
				double max = 0, length[] = new double[3], angle = -1;
				for (int i=tri; i<tri+3; i++) {
					int i2 = i + 1;
					if (i == tri + 2) i2 = tri;
					double dx = this.getX(i).getValue() - this.getX(i2).getValue();
					double dy = this.getY(i).getValue() - this.getY(i2).getValue();
					length[i-tri] = FastMath.hypot(dx, dy);
					if (length[i-tri] > max) max = length[i-tri];
					if (i == tri) angle = FastMath.atan2_accurate(dy, dx);
				}
				length[0] = length[0] / max;
				length[1] = length[1] / max;
				length[2] = length[2] / max;

				int tr[][] = findTriangle(x, y, length, maxError, id, tri, max, angle);
				if (tr != null && tr.length == 1) {
					id[tri] = tr[0][0];
					id[tri+1] = tr[0][1];
					id[tri+2] = tr[0][2];
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

		return id;
	}

	private double[] lastX, lastY;
	private SExtractor lastS;
	/**
	 * Performs a cross match of one source in this instance with the sources in another instance. The images
	 * solved in both instances must correspond to the same field of view and orientation.
	 * @param sex The second SExtractor instance with their sources also solved.
	 * @param maxError The maximum error in pixels as a tolerance for the cross matching, for instance 0.5.
	 * @param index The index of the source to cross match in this instance.
	 * @return Value returned is -1 in case the source is not identified. Otherwise the index of that source in
	 * the other instance provided in this call is returned.
	 * @throws JPARSECException If an error occurs.
	 */
	public int crossMatchOneSource(SExtractor sex, double maxError, int index) throws JPARSECException {
		if (sources == null) return -1;
		int nsources = this.getNumberOfSources();
		if (nsources == 0 || sex.sources == null || sex.getNumberOfSources() == 0) return -1;

		if (lastX == null || lastY == null || lastS == null || !sex.equals(lastS)) {
			int n = sex.getNumberOfSources();
			double x[] = new double[n], y[] = new double[n];
			for (int i=0; i<n; i++) {
				x[i] = sex.getX(i).getValue();
				y[i] = sex.getY(i).getValue();
			}

			lastX = x;
			lastY = y;
			lastS = sex;
		}
		return identifyStar(this, lastX, lastY, index, maxError);
	}
	// Identify a star in B in position index with one in V, for a given tolerance. -1 is returned if a match is not found.
	private int identifyStar(SExtractor B, double xv[], double yv[], int index, double tolerance) throws JPARSECException {
		double x = B.getX(index).getValue();
		double y = B.getY(index).getValue();
		double minDist = -1;
		int out = -1;
		for (int i=0; i<xv.length; i++) {
			double dx = Math.abs(x - xv[i]);
			if (dx > tolerance) continue;
			double dy = Math.abs(y - yv[i]);
			if (dy > tolerance) continue;
			double dist = FastMath.hypot(dx, dy);
			if (dist < minDist || minDist == -1) {
				minDist = dist;
				out = i;
			}
		}
		if (minDist > tolerance) return -1;
		return out;
	}

	private double scale = 0, angle = 0, nobs = 0;
	private int[][] findTriangle(double x[], double y[], double l[], double err, int id[], int tri, double maxl, double angle)
			throws JPARSECException {
		int out[][] = null;
		ArrayList<int[]> solution = new ArrayList<int[]>();
		int imin = 0, imax = x.length;
		// Reduce the number of sources to search for better performance, although some could be missed ...
		if ((imax / (tri+1)) > 2) imax = (tri+1)*2;
		int jmin = 0, jmax = imax;
		int kmin = 0, kmax = imax;
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
							if (difScale > 1.1 || difScale < 0.9 || difAngle > 10 * Constant.DEG_TO_RAD) continue;
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
	 * Returns all the sources solved as a Table object in 2d.
	 * The table contains the following columns: x positions,
	 * y positions, widths, heights, fluxes, classes, magnitudes,
	 * detection widths, detection heights.
	 * @return The Table object, or null if it is empty.
	 */
	public Table getSourcesAsTable() {
		int n = getNumberOfSources();
		if (n == 0) return null;
		MeasureElement data[][] = new MeasureElement[n][9];
		for (int i=0; i<n; i++) {
			data[i] = new MeasureElement[] {
					getX(i), getY(i), new MeasureElement(getWidth(i), 0, ""), new MeasureElement(getHeight(i), 0, ""),
					getFlux(i), new MeasureElement(getClass(i), 0, ""), getMagnitude(i),
					new MeasureElement(getPeakIntensity(i), 0, ""), new MeasureElement(getDetectionWidth(i), 0, ""),
					new MeasureElement(getDetectionHeight(i), 0, "")
			};
		}
		return new Table(data);
	}
}
