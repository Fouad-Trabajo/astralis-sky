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
package jparsec.io.image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import nom.tam.fits.*;
import nom.tam.image.ImageTiler;
import nom.tam.util.*;
import jparsec.ephem.Functions;
import jparsec.graph.DataSet;
import jparsec.io.FileFormatElement;
import jparsec.io.FileIO;
import jparsec.io.ReadFormat;
import jparsec.io.WriteFile;
import jparsec.math.Constant;
import jparsec.math.Evaluation;
import jparsec.math.FastMath;
import jparsec.observer.LocationElement;
import jparsec.util.*;
import jparsec.vo.SExtractor;

/**
 * A class to read and write .fits files.<P>
 *
 * The data array used for input and output is modified
 * respect to the original .fits format. The new format
 * is the standard (x, y) coordinate system for the first
 * [...][] and second [][...] dimensions in the array.
 * Position (0, 0) of the array is the top left corner
 * of the image (the center of the first pixel), while
 * the (width-1, height-1) is the bottom right corner.
 * For the position of the sources solved by SExtractor
 * the center of the first pixel is at (1, 1).
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class FitsIO {

	private BasicHDU<?>[] hdus;
	private Fits f;

	/**
	 * Constructor for a fits image.
	 * @param path The path to a .fits file.
	 * @throws JPARSECException If an error occurs.
	 */
	public FitsIO(String path)
	throws JPARSECException {
		try {
			f = new Fits(path);
			hdus = f.read();
		} catch (Exception e) {
			throw new JPARSECException("Cannot read the fits image "+path+".", e);
		}
	}

	/**
	 * Constructor for a given data and 1 plain.
	 * @param data The data for the image.
	 * @throws JPARSECException If the fits HDU cannot be created from input data.
	 */
	public FitsIO(double[][] data) throws JPARSECException
	{
		hdus = new BasicHDU[1];
		try {
			hdus[0] = FitsIO.createHDU(data, null);
		} catch (Exception exc) {
			throw new JPARSECException("Cannot create the HDU", exc);
		}
	}

	/**
	 * Constructor for a given data and 1 plain.
	 * @param data The data for the image.
	 * @throws JPARSECException If the fits HDU cannot be created from input data.
	 */
	public FitsIO(Object data) throws JPARSECException
	{
		hdus = new BasicHDU[1];
		try {
			hdus[0] = FitsIO.createHDU(data, null);
		} catch (Exception exc) {
			throw new JPARSECException("Cannot create the HDU", exc);
		}
	}

	/**
	 * Constructor for a given HDU (1 plain).
	 * @param hdu The HDU.
	 */
	public FitsIO(BasicHDU<?> hdu)
	{
		hdus = new BasicHDU[1];
		hdus[0] = hdu;
	}

	/**
	 * Constructor for a given data and 1 plain.
	 * @param data The data for the image.
	 * @throws JPARSECException If the fits HDU cannot be created from input data.
	 */
	public FitsIO(int[][] data) throws JPARSECException
	{
		hdus = new BasicHDU[1];
		try {
			hdus[0] = FitsIO.createHDU(data, null);
		} catch (Exception exc) {
			throw new JPARSECException("Cannot create the HDU", exc);
		}
	}

	/**
	 * Constructor for an empty fits.
	 */
	public FitsIO() {
		hdus = new BasicHDU[0];
	}

	/**
	 * Close a fits file if the file was read from a given path.
	 * @throws Exception If an error occurs.
	 */
	public void closeFits() throws Exception {
		if (f != null) f.close();
	}
	
	/**
	 * Returns the number of plains or HDUs in the image.
	 * @return The number of plains.
	 */
	public int getNumberOfPlains()
	{
		return hdus.length;
	}

	/**
	 * Writes the image.
	 * @param n The index of the plain to write.
	 * @param path The path.
	 * @throws JPARSECException If an error occurs.
	 */
	public void write(int n, String path)
	throws JPARSECException {
		FitsIO.writeEntireFits(path, new BasicHDU[] {hdus[n]});
	}

	/**
	 * Writes the entire fits with all images.
	 * @param path The path.
	 * @throws JPARSECException If an error occurs.
	 */
	public void writeEntireFits(String path)
	throws JPARSECException {
		FitsIO.writeEntireFits(path, hdus);
	}

	/**
	 * Sets the header.
	 * @param n The index of the plain.
	 * @param header The header.
	 * @throws JPARSECException If an error occurs.
	 */
	public void setHeader(int n, HeaderElement header[])
	throws JPARSECException {
		FitsIO.setHeader(hdus[n], header);
	}

	/**
	 * Returns the header.
	 * @param n The plain index.
	 * @return The header.
	 * @throws JPARSECException If the header cannot be read.
	 */
	public HeaderElement[] getHeader(int n) throws JPARSECException
	{
		return FitsIO.getHeader(hdus[n]);
	}

	/**
	 * The set of options to obtain the picture of a fits file.
	 */
	public enum PICTURE_LEVEL {
		/** Shows the original levels in the image, after
		 * rounding them to integers.
		 * Note this could launch an error for levels outside range 0-255. */
		ORIGINAL,
		/** Performs linear interpolation between minimum and maximum intensities, setting
		 * output values between 0 and 255. */
		LINEAR_INTERPOLATION,
		/** Shows the image using a logarithmic scaling for the intensities. */
		LOG_SCALE,
		/** Shows the image using a exponential scaling for the intensities. */
		EXP_SCALE,
		/** Custom formulae, to be set in the instance. */
		CUSTOM;

		/** The value in the picture for an intensity equal to NaN.
		 * Set it between 0 and 255. Default value is -1 to avoid checking
		 * the image for a NaN value, which will set all them to 0. */
		public int NaN = 0;
		/** The formula to be used to transform intensities to integers
		 * in the range 0-255, using 'x' to reference the intensity.
		 * For instance '5*x'. Java operations like 'Math.sin()' and all
		 * others are allowed. Other special values are 'max' for the
		 * maximum intensity in the image, and 'min' for the minimum one.*/
		public String formula = "x";
	};

	/**
	 * Returns the picture.
	 * @param n The plain.
	 * @param level The operation to be performed to map
	 * intensities to 0-255 range.
	 * @param grid True to draw a grid in the image in case a WCS
	 * instance is available.
	 * @return A picture object.
	 * @throws JPARSECException If an error occurs.
	 */
	public Picture getPicture(int n, PICTURE_LEVEL level, boolean grid)
	throws JPARSECException {
		return FitsIO.getPicture(hdus[n], level, grid);
	}

	/**
	 * Returns the picture for a given image.
	 * @param hdu The image.
	 * @param level The operation to be performed to map
	 * intensities to 0-255 range.
	 * @param grid True to draw a grid in the image in case a WCS
	 * instance is available.
	 * @return The picture.
	 * @throws JPARSECException If an error occurs.
	 */
	public static Picture getPicture(BasicHDU<?> hdu, PICTURE_LEVEL level, boolean grid)
	throws JPARSECException {
		double data[][] = (double[][]) FitsIO.getData(hdu, true, -1);

		// Prepare interpolation between min and max values
		Evaluation eval = null;
		double max = -1, min = -1;
		if (level == PICTURE_LEVEL.LINEAR_INTERPOLATION || level == PICTURE_LEVEL.CUSTOM || level == PICTURE_LEVEL.LOG_SCALE
				|| level == PICTURE_LEVEL.EXP_SCALE) {
			if (level == PICTURE_LEVEL.LOG_SCALE || level == PICTURE_LEVEL.EXP_SCALE) {
				for (int i=0; i<data.length; i++)
				{
					for (int j=0; j<data[i].length; j++)
					{
						if (level == PICTURE_LEVEL.LOG_SCALE) {
							data[i][j] = FastMath.log(data[i][j]);
						} else {
							data[i][j] = FastMath.exp(data[i][j]);
						}
					}
				}
			}

			max = DataSet.getMaximumValue(data);
			min = DataSet.getMinimumValue(data);

			if (level == PICTURE_LEVEL.CUSTOM)
				eval = new Evaluation(level.formula, new String[] {"x "+0, "max "+max, "min "+min});
		}

		Picture pic = new Picture(data.length+1, data[0].length+1);
		int val = 0;
		for (int i=0; i<data.length; i++)
		{
			for (int j=0; j<data[i].length; j++)
			{
				if (level != null && level.NaN >= 0 && (Double.isNaN(data[i][j]) || Double.isInfinite(data[i][j]))) {
					val = level.NaN;
				} else {
					if (level == PICTURE_LEVEL.LINEAR_INTERPOLATION || level == PICTURE_LEVEL.LOG_SCALE || level == PICTURE_LEVEL.EXP_SCALE) {
						val = (int)(255 * (data[i][j] - min) / (max - min)); // interp.linearInterpolation(val);
					} else {
						if (level == PICTURE_LEVEL.CUSTOM && level.formula != null) {
							eval.resetVariable("x", data[i][j]);
							val = (int) eval.evaluate();
						} else {
							val = (int) data[i][j];
						}
						if (val > 255) val = 255;
						if (val < 0) val = 0;
					}
				}
				if (val >= 0) pic.setColor(i, j, Functions.getColor(val, val, val, 255));
			}
		}

		if (grid) {
			try {
				HeaderElement header[] = getHeader(hdu);
				int index = HeaderElement.getIndex(header, "CRPIX1");
				if (index < 0) return pic;
				WCS wcs = new WCS(header);
				if (wcs.getWidth() > 0 && wcs.getHeight() > 0) {
					Graphics2D g = (Graphics2D) pic.getImage().getGraphics();
					g.setColor(Color.RED);
					LocationElement loc00 = wcs.getSkyCoordinates(new Point2D.Double(0, 0));
					LocationElement locwh = wcs.getSkyCoordinates(new Point2D.Double(pic.getWidth()-1, pic.getHeight()-1));
					double dlon = (locwh.getLongitude() - loc00.getLongitude()) * Constant.RAD_TO_DEG;
					double step = 1.0; // deg
					if (Math.abs(dlon) < 5) step = 0.5;
					if (Math.abs(dlon) < 1) step = 1.0 / 6.0;
					if (Math.abs(dlon) < 0.5) step = 1.0 / 12.0;
					if (Math.abs(dlon) < 1.0 / 6.0) step = 1.0 / 30.0;
					if (Math.abs(dlon) < 1.0 / 12.0) step = 1.0 / 60.0;
					if (Math.abs(dlon) < 1.0 / 30.0) step = 1.0 / 120.0;
					if (Math.abs(dlon) < 1.0 / 60.0) step = 1.0 / 240.0;
					if (Math.abs(dlon) < 1.0 / 120.0) step = 1.0 / 720.0;
					if (Math.abs(dlon) < 1.0 / 360.0) step = 1.0 / 1800.0;
					if (Math.abs(dlon) < 1.0 / 1800.0) step = 1.0 / 3600.0;
					step *= Constant.DEG_TO_RAD;

					double ra0 = loc00.getLongitude(), dec0 = loc00.getLatitude();
					double ra1 = locwh.getLongitude(), dec1 = locwh.getLatitude();
					if (ra1 < ra0) {
						double tmp = ra1;
						ra1 = ra0;
						ra0 = tmp;
					}
					if (dec1 > dec0) {
						double tmp = dec1;
						dec1 = dec0;
						dec0 = tmp;
					}
					ra0 -= Functions.module(ra0, step) + step;
					dec0 -= Functions.module(dec0, step) - step;
					for (double ra = ra0; ra <= ra1 + step; ra += step) {
						Point2D pLast = null;
						for (double dec = dec0; dec >= dec1 - step; dec -= step) {
							Point2D p = wcs.getPixelCoordinates(new LocationElement(ra, dec, 1));
							if (pLast != null) {
								g.drawLine((int)(p.getX()+0.5), (int)(p.getY()+0.5), (int)(pLast.getX()+0.5), (int)(pLast.getY()+0.5));
							}
							pLast = p;
						}
						g.drawString(Functions.formatRA(ra), (int)pLast.getX()+ 5, pic.getHeight() - 20);
					}
					for (double dec = dec0; dec >= dec1 - step; dec -= step) {
						Point2D pLast = null;
						for (double ra = ra0; ra <= ra1 + step; ra += step) {
							Point2D p = wcs.getPixelCoordinates(new LocationElement(ra, dec, 1));
							if (pLast != null) {
								g.drawLine((int)(p.getX()+0.5), (int)(p.getY()+0.5), (int)(pLast.getX()+0.5), (int)(pLast.getY()+0.5));
							}
							pLast = p;
						}
						g.drawString(Functions.formatDEC(dec), 5, (int)pLast.getY() - 20);
					}
					g.dispose();
				}
			} catch (Exception exc) {
				exc.printStackTrace();
			}
		}
		return pic;
	}

	/**
	 * Returns the image data as an array.
	 * @param n The plain.
	 * @return The data.
	 * @throws JPARSECException If an error occurs.
	 */
	public Object getData(int n)
	throws JPARSECException {
		return FitsIO.getData(hdus[n], false, -1);
	}

	/**
	 * Returns the image data as an array of type double.
	 * @param n The plain.
	 * @return The data.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[][] getDataAsDoubleArray(int n)
	throws JPARSECException {
		return (double[][]) FitsIO.getData(hdus[n], true, -1);
	}

	/**
	 * Returns the image data as an array of type double.
	 * @param n The plain.
	 * @param max Offset of the minimum value respect the BZERO
	 * value included in the header. Set to -1 to compute it
	 * automatically, and to 0 to recover the data you entered
	 * in case {@linkplain #setData(Object, int, double, double)} was called.
	 * The adequate value depends on the criteria used to store the image.
	 * @return The data.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[][] getDataAsDoubleArray(int n, double max)
	throws JPARSECException {
		return (double[][]) FitsIO.getData(hdus[n], true, max);
	}

	/**
	 * Returns the image data as an array, for a given rectangle.
	 * @param n The plain.
	 * @param x0 The x index (horizontal position) of the first pixel, 0 is first.
	 * @param xf The x index (horizontal position) of the last pixel in the rectangle.
	 * @param y0 The y index (vertical position) of the first pixel, 0 is first.
	 * @param yf The y index (vertical position) of the first pixel in the rectangle.
	 * @return The data.
	 * @throws JPARSECException If an error occurs.
	 */
	public Object getData(int n, int x0, int xf, int y0, int yf)
	throws JPARSECException {
		try {
			if (x0 < 0) x0 = 0;
			if (y0 < 0) y0 = 0;
			int[] size = hdus[n].getAxes();
			if (xf > size[1]-1) xf = size[1]-1;
			if (yf > size[0]-1) yf = size[0]-1;
		} catch (FitsException e) {
			e.printStackTrace();
		}
		return FitsIO.getData(hdus[n], false, x0, xf, y0, yf);
	}

	/**
	 * Returns the image data as an array of type double, for a given
	 * rectangle.
	 * @param n The plain.
	 * @param x0 The x index (horizontal position) of the first pixel, 0 is first.
	 * @param xf The x index (horizontal position) of the last pixel in the rectangle.
	 * @param y0 The y index (vertical position) of the first pixel, 0 is first.
	 * @param yf The y index (vertical position) of the first pixel in the rectangle.
	 * @return The data.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[][] getDataAsDoubleArray(int n, int x0, int xf, int y0, int yf)
	throws JPARSECException {
		try {
			if (x0 < 0) x0 = 0;
			if (y0 < 0) y0 = 0;
			int[] size = hdus[n].getAxes();
			if (xf > size[1]-1) xf = size[1]-1;
			if (yf > size[0]-1) yf = size[0]-1;
		} catch (FitsException e) {
			e.printStackTrace();
		}
		return (double[][]) FitsIO.getData(hdus[n], true, x0, xf, y0, yf);
	}

	/**
	 * Returns the width of a given image HDU.
	 * @param n The HDU index.
	 * @return Width in pixels.
	 * @throws JPARSECException If an error occurs.
	 */
	public int getWidth(int n) throws JPARSECException {
		try  {
			int[] size = hdus[n].getAxes();
			return size[1];
		} catch (Exception exc) {
			throw new JPARSECException(exc);
		}
	}

	/**
	 * Returns the height of a given image HDU.
	 * @param n The HDU index.
	 * @return Height in pixels.
	 * @throws JPARSECException If an error occurs.
	 */
	public int getHeight(int n) throws JPARSECException {
		try  {
			int[] size = hdus[n].getAxes();
			return size[0];
		} catch (Exception exc) {
			throw new JPARSECException(exc);
		}
	}

	/**
	 * Returns the BZero value for a given image.
	 * @param n The image index.
	 * @return The BZero value.
	 */
	public double getBZero(int n) {
		return hdus[n].getBZero();
	}
	/**
	 * Returns the BScale value for a given image.
	 * @param n The image index.
	 * @return The BScale value.
	 */
	public double getBScale(int n) {
		return hdus[n].getBScale();
	}
	/**
	 * Returns the bitpix value for a given image.
	 * @param n The image index.
	 * @return The bitpix value.
	 * @throws FitsException Exception launched by nom.tam.fits library.
	 */
	public int getBitPix(int n) throws FitsException {
		return hdus[n].getBitPix();
	}

	/**
	 * Writes a fits file.
	 * @param name The file name.
	 * @param hdus The set of images.
	 * @throws JPARSECException If an error occurs.
	 */
	private static void writeEntireFits(String name, BasicHDU<?> hdus[])
	throws JPARSECException {
		Fits f = new Fits();

		try {
			for (int i=0; i< hdus.length; i++) {
				f.addHDU(hdus[i]);
			}

			// Write a FITS file.
			BufferedFile bf = new BufferedFile(name, "rw");
			f.write(bf);
			bf.flush();
			bf.close();
			f.close();
		} catch (Exception e)
		{
			throw new JPARSECException("Cannot write the fits file.", e);
		}
	}

	/**
	 * Sets the header of a fits.
	 * @param hdu The HDU.
	 * @param header The header.
	 * @throws JPARSECException If an error occurs.
	 */
	private static void setHeader(BasicHDU<?> hdu, HeaderElement header[])
	throws JPARSECException {
		String key = null;
		int i = 0;
		try {
			Header h = new Header();
			try {
				h = hdu.getHeader();
				if (h != null)
				{
					int n = h.getNumberOfCards();
					for (i=0; i<n; i++)
					{
						h.deleteKey(h.getKey(i));
					}
				}
			} catch (NullPointerException e) {
				h = ImageHDU.manufactureHeader(hdu.getData());
			}

			for (i=0; i<header.length; i++)
			{
				key = header[i].key;
				double val = 0.0;
				try {
					val = header[i].getAsLong();
					h.addValue(header[i].key, (long) val, header[i].comment);
				} catch (Exception e)
				{
					try {
						val = header[i].getAsDouble();
						h.addValue(header[i].key, val, header[i].comment);
					} catch (Exception e2) {
						h.addValue(header[i].key, header[i].getAsString(), header[i].comment);
					}
				}
			}
		} catch (Exception e)
		{
			if (e.getMessage().equals("Value too long") && i >= 0 && key != null) {
				JPARSECException.addWarning("Could not write value of card "+key+", value is too long ("+header[i].getAsString()+")");
			} else {
				if (key == null) {
					throw new JPARSECException(e);
				} else {
					throw new JPARSECException("("+key+")", e);
				}
			}
		}
	}

	/**
	 * Returns the header in a two/three column dataset, with
	 * each key and the value, and some times a comment.
	 * @param hdu The HDU.
	 * @return The header.
	 * @throws JPARSECException If the header cannot be read.
	 */
	private static HeaderElement[] getHeader(BasicHDU<?> hdu) throws JPARSECException
	{
		Header header = null;
		try {
			header = hdu.getHeader();
		} catch (NullPointerException e)
		{
			throw new JPARSECException("Cannot read the header.", e);
		}
		return getHeader(header);
	}

	/**
	 * Returns the header data from a nom.tam.fits Header object.
	 * @param header The header object used in nom.tam.fits.
	 * @return The header set of objects used in JPARSEC.
	 * @throws JPARSECException If the header cannot be read.
	 */
	public static HeaderElement[] getHeader(Header header) throws JPARSECException
	{
		int n = header.getNumberOfCards();
		HeaderElement head[] = new HeaderElement[n-1];
		for (int i=0; i<n-1; i++)
		{
			String item = header.getCard(i);
			int e = item.indexOf("  ");
			int ee = item.indexOf("=");
			if (ee >= 0) e = ee;
			int b = item.indexOf(" / ");
			if (e > 9 || e < 0) e = 7;
			if (b > e)
			{
				head[i] = new HeaderElement(
					item.substring(0, e).trim(),
					item.substring(e+1, b).trim(),
					item.substring(b+1).trim()
				);
			} else {
				head[i] = new HeaderElement(
						item.substring(0, e).trim(),
						item.substring(e+1).trim(),
						""
					);
			}
			if (head[i].getAsString().startsWith("'") &&
					head[i].getAsString().endsWith("'"))
				head[i].value = head[i].getAsString().substring(1, head[i].getAsString().length()-1).trim();
		}
		return head;
	}

	/**
	 * Returns the data in an image.
	 * @param hdu The HDU.
	 * @param asDouble True to return double[][], else for the
	 * original data type.
	 * @param max Maximum value. -1 to compute it automatically.
	 * @return The data, or null if no data exists.
	 * @throws JPARSECException If an error occurs.
	 */
	private static Object getData(BasicHDU<?> hdu, boolean asDouble, double max)
	throws JPARSECException {
		try {
			Object o = (hdu.getData()).getData();
			return getData(hdu, o, asDouble, max);
		} catch (Exception exc) {
			try {
				Object oo = (hdu.getData()).getData();
				if (oo == null) return null;
				return oo;
			} catch (Exception ee)
			{
				throw new JPARSECException(ee);
			}
		}
	}

	/**
	 * Returns the data in an image.
	 * @param hdu The HDU.
	 * @param asDouble True to return double[][], else for the
	 * original data type.
	 * @return The data, or null if no data exists.
	 * @throws JPARSECException If an error occurs.
	 */
	private static Object getData(BasicHDU<?> hdu, boolean asDouble, int x0, int xf, int y0, int yf)
	throws JPARSECException {
		try {
			if (x0 < 0) x0 = 0;
			if (y0 < 0) y0 = 0;
			int[] size = hdu.getAxes();
			if (xf > size[1]-1) xf = size[1]-1;
			if (yf > size[0]-1) yf = size[0]-1;
		} catch (FitsException e) {
			e.printStackTrace();
		}

		try {
			ImageTiler tiler = ((ImageHDU) hdu).getTiler();
			int dx = xf-x0+1, dy = yf-y0+1;
			Object o = tiler.getTile(new int[] {y0, x0}, new int[] {dy, dx});
			if (!o.getClass().getComponentType().isArray()) {
				int bp = hdu.getBitPix();
				if (bp == 8) {
					byte oo[][] = new byte[dy][dx];
					byte oo0[] = (byte[]) o;
					for (int y=0; y < oo.length; y++) {
						int offset = y * oo[0].length;
						System.arraycopy(oo0, offset, oo[y], 0, oo[0].length);
					}
					o = oo;
				} else {
					if (bp == 16) {
						short oo[][] = new short[dy][dx];
						short oo0[] = (short[]) o;
						for (int y=0; y < oo.length; y++) {
							int offset = y * oo[0].length;
							System.arraycopy(oo0, offset, oo[y], 0, oo[0].length);
						}
						o = oo;
					} else {
						if (bp == 32) {
							int oo[][] = new int[dy][dx];
							int oo0[] = (int[]) o;
							for (int y=0; y < oo.length; y++) {
								int offset = y * oo[0].length;
								System.arraycopy(oo0, offset, oo[y], 0, oo[0].length);
/*								for (int x=0; x < oo[0].length; x++) {
									oo[y][x] = oo0[x + offset];
								}
*/							}
							o = oo;
						} else {
							if (bp == -32) {
								float oo[][] = new float[dy][dx];
								float oo0[] = (float[]) o;
								for (int y=0; y < oo.length; y++) {
									int offset = y * oo[0].length;
									System.arraycopy(oo0, offset, oo[y], 0, oo[0].length);
								}
								o = oo;
							} else {
								if (bp == 64) {
									double oo[][] = new double[dy][dx];
									double oo0[] = (double[]) o;
									for (int y=0; y < oo.length; y++) {
										int offset = y * oo[0].length;
										System.arraycopy(oo0, offset, oo[y], 0, oo[0].length);
									}
									o = oo;
								}
							}
						}
					}
				}
			}
			return getData(hdu, o, asDouble, -1);
		} catch (Exception exc) {
			exc.printStackTrace();
			return null;
		}
	}

	private static Object getData(BasicHDU<?> hdu, Object o, boolean asDouble, double max)
	throws JPARSECException {
		try {
			if (o == null) return null;
			double out[][];

			int bp = hdu.getBitPix();
			if (max == -1) max = 0; //(Math.pow(2.0, Math.abs(bp)-1)); // FastMath.multiplyBy2ToTheX(1, Math.abs(bp)-1);
			/*
			if (bp > 0 && bp <= 32) max = Math.pow(2.0, bp-1);
			if (bp == -32) max = Math.pow(2.0, 128-1);
			if (Math.abs(bp) == 64) max = Math.pow(2.0, 1024-1);
			*/
			double bZero = ((ImageHDU) hdu).getBZero();
			double bScale = ((ImageHDU) hdu).getBScale();
			//Object dataR = ((ImageHDU) hdu).getData().getData();

			int ndim = DataSet.getNumberOfDimensions(o);
			if (ndim > 2) {
				if (ndim > 4) throw new JPARSECException("Input data has more than 4 dimensions.");
				if (ndim == 4) {
					if (bp == 8) {
						byte data[][][][] = (byte[][][][]) o;
						o = data[0][0];
					} else {
						if (bp == 16) {
							short data[][][][] = (short[][][][]) o;
							o = data[0][0];
						} else {
							if (bp == 32) {
								int data[][][][] = (int[][][][]) o;
								o = data[0][0];
							} else {
								if (bp == -32) {
									float data[][][][] = (float[][][][]) o;
									o = data[0][0];
								} else {
									if (bp == 64) {
										double data[][][][] = (double[][][][]) o;
										o = data[0][0];
									} else {
										double[][][][] data = (double[][][][]) ArrayFuncs.convertArray(o, double.class);
										o = data[0][0];
									}
								}
							}
						}
					}
				} else {
					if (bp == 8) {
						byte data[][][] = (byte[][][]) o;
						o = data[0];
					} else {
						if (bp == 16) {
							short data[][][] = (short[][][]) o;
							o = data[0];
						} else {
							if (bp == 32) {
								int data[][][] = (int[][][]) o;
								o = data[0];
							} else {
								if (bp == -32) {
									float data[][][] = (float[][][]) o;
									o = data[0];
								} else {
									if (bp == 64) {
										double data[][][] = (double[][][]) o;
										o = data[0];
									} else {
										double[][][] data = (double[][][]) ArrayFuncs.convertArray(o, double.class);
										o = data[0];
									}
								}
							}
						}
					}
				}
				JPARSECException.addWarning("Input data has more than 2 dimensions. Only the last two dimensions will be considered.");
			}

			String name = o.getClass().getComponentType().getComponentType().getName();
			if (name.equals("double") && bp == -32) bp = 64;

			try {
				if (bp == 8) {
					byte data[][] = (byte[][]) o;
					out = new double[data[0].length][data.length];
					for (int j=0; j<data[0].length; j++) {
						out[j] = FitsIO.getOriginalValue(data, bZero - max, bScale, j);
					}
				} else {
					if (bp == 16) {
						short data[][] = (short[][]) o;
						out = new double[data[0].length][data.length];
						for (int j=0; j<data[0].length; j++) {
							out[j] = FitsIO.getOriginalValue(data, bZero - max, bScale, j);
						}
					} else {
						if (bp == 32) {
							int data[][] = (int[][]) o;
							out = new double[data[0].length][data.length];
							for (int j=0; j<data[0].length; j++) {
								out[j] = FitsIO.getOriginalValue(data, bZero - max, bScale, j);
							}
						} else {
							if (bp == -32) {
								float data[][] = (float[][]) o;
								out = new double[data[0].length][data.length];
								for (int j=0; j<data[0].length; j++) {
									out[j] = FitsIO.getOriginalValue(data, bZero - max, bScale, j);
								}
							} else {
								if (bp == 64) {
									double data[][] = (double[][]) o;
									out = new double[data[0].length][data.length];
									for (int j=0; j<data[0].length; j++) {
										out[j] = FitsIO.getOriginalValue(data, bZero - max, bScale, j);
									}
								} else {
									double[][] data = (double[][]) ArrayFuncs.convertArray(o, double.class);
									out = new double[data[0].length][data.length];
									for (int j=0; j<data[0].length; j++) {
										out[j] = FitsIO.getOriginalValue(data, bZero - max, bScale, bp, j);
									}
								}
							}
						}
					}
				}
			} catch (Exception exc)
			{
				exc.printStackTrace();
				double[][][][] data = (double[][][][]) ArrayFuncs.convertArray(o, double.class);
				out = new double[data[0][0][0].length][data[0][0].length];
				for (int j=0; j<data[0][0][0].length; j++) {
					out[j] = FitsIO.getOriginalValue4d(data, bZero - max, bScale, bp, j);
				}
			}

			if (!asDouble) {
				if (bp == 32) return DataSet.toIntArray(out);
				if (bp == -32) return DataSet.toFloatArray(out);
				if (bp == 16) return DataSet.toShortArray(out);
				if (bp == 8) return DataSet.toByteArray(out);
			}
			return out;
		} catch (Exception e)
		{
			try {
				Object oo = (hdu.getData()).getData();
				if (oo == null) return null;
				return oo;
			} catch (Exception ee)
			{
				throw new JPARSECException(ee);
			}
		}
	}

	/**
	 * Sets the image data.
	 * @param data The data.
	 * @param n The plain.
	 * @throws JPARSECException If an error occurs.
	 */
	public void setData(Object data, int n)
	throws JPARSECException {
		try {
			BasicHDU<?> hdu = FitsIO.createHDU(data, this.getHeader(n));
			//ImageData newData = new ImageData(data);
			//ImageHDU hdu = new ImageHDU(hdus[n].getHeader(), newData);

			hdus[n] = hdu;
		} catch (Exception exc) {
			throw new JPARSECException("Could not set the image data", exc);
		}
	}

	/**
	 * Sets the image data.
	 * @param data The data.
	 * @param n The plain.
	 * @param bzero The new bzero value. This value is expressed respect
	 * the 0-level used internally to represent the lowest intensity. This value
	 * is usually -2^(Math.abs(bitpix)-1). In case you set bzero to 0, first
	 * intensity will be -2^(Math.abs(bitpix)-1). To obtain 0 as lowest intensity,
	 * set bzero to +2^(Math.abs(bitpix)-1).
	 * @param bscale The new bscale value. To conserve the scale set this to 1.
	 * @throws JPARSECException If an error occurs.
	 */
	public void setData(Object data, int n, double bzero, double bscale)
	throws JPARSECException {
		try {
			HeaderElement[] header = this.getHeader(n);
			header = HeaderElement.addHeaderEntry(header, new HeaderElement("BZERO", ""+bzero, ""));
			header = HeaderElement.addHeaderEntry(header, new HeaderElement("BSCALE", ""+bscale, ""));

			BasicHDU<?> hdu = null;
			if (data == null) {
				hdu = Fits.makeHDU(FitsBinaryTable.createHeader(header));
				FitsIO.setHeader(hdu, header);
			} else {
				hdu = FitsFactory.HDUFactory(data);
				hdu = FitsFactory.HDUFactory(getData(hdu, false, 0));
				if (header != null) FitsIO.setHeader(hdu, header);
			}

			hdus[n] = hdu;
		} catch (Exception exc) {
			throw new JPARSECException("Could not set the image data", exc);
		}
	}

	/**
	 * Returns the data for a given HDU applying a given scaling operation in the flux.
	 * @param hdu The hdu index.
	 * @param level The operation.
	 * @return The scaled data.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[][] getScaledData(int hdu, PICTURE_LEVEL level) throws JPARSECException {
		double data[][] = (double[][]) FitsIO.getData(this.getHDU(hdu), true, -1);

		// Prepare interpolation between min and max values
		Evaluation eval = null;
		double max = -1, min = -1;
		if (level == PICTURE_LEVEL.LINEAR_INTERPOLATION || level == PICTURE_LEVEL.CUSTOM || level == PICTURE_LEVEL.LOG_SCALE
				|| level == PICTURE_LEVEL.EXP_SCALE) {
			if (level == PICTURE_LEVEL.LOG_SCALE || level == PICTURE_LEVEL.EXP_SCALE) {
				for (int i=0; i<data.length; i++)
				{
					for (int j=0; j<data[i].length; j++)
					{
						if (level == PICTURE_LEVEL.LOG_SCALE) {
							data[i][j] = FastMath.log(data[i][j]);
						} else {
							data[i][j] = FastMath.exp(data[i][j]);
						}
					}
				}
			}

			max = DataSet.getMaximumValue(data);
			min = DataSet.getMinimumValue(data);

			if (level == PICTURE_LEVEL.CUSTOM)
				eval = new Evaluation(level.formula, new String[] {"x "+0, "max "+max, "min "+min});
		}

		double val = 0;
		for (int i=0; i<data.length; i++)
		{
			for (int j=0; j<data[i].length; j++)
			{
				if (level != null && level.NaN >= 0 && (Double.isNaN(data[i][j]) || Double.isInfinite(data[i][j]))) {
					val = level.NaN;
				} else {
					if (level == PICTURE_LEVEL.LINEAR_INTERPOLATION || level == PICTURE_LEVEL.LOG_SCALE || level == PICTURE_LEVEL.EXP_SCALE) {
						val = (255 * (data[i][j] - min) / (max - min));
					} else {
						if (level == PICTURE_LEVEL.CUSTOM && level.formula != null) {
							eval.resetVariable("x", data[i][j]);
							val = eval.evaluate();
						} else {
							val = data[i][j];
						}
					}
				}
				data[i][j] = val;
			}
		}

		return data;
	}

  /**
   * Returns the intensity at a given point row.
   * @return The actual data values at position (..., y), with bZero and bScale
   * applied.
   */
	private static double[] getOriginalValue(double[][] data, double bZero, double bScale, int bp, int x) throws FitsException
	{
	   switch(bp)
	   {
	   case 8:
		   double rb[] = new double[data.length];
		   for (int y=0; y<rb.length; y++) {
			   rb[y] = bZero + bScale * (data[y][x]);
		   }
		   return rb;
	   case 16:
		   double rs[] = new double[data.length];
		   for (int y=0; y<rs.length; y++) {
			   rs[y] = bZero + bScale * data[y][x];
		   }
		   return rs;
	   case 32:
		   double ri[] = new double[data.length];
		   for (int y=0; y<ri.length; y++) {
			   ri[y] = bZero + bScale * data[y][x];
		   }
		   return ri;
	   case -32:
		   double rf[] = new double[data.length];
		   for (int y=0; y<rf.length; y++) {
			   rf[y] = bZero + bScale * data[y][x];
		   }
		   return rf;
	   case -64:
		   double rd[] = new double[data.length];
		   for (int y=0; y<rd.length; y++) {
			   rd[y] = bZero + bScale * data[y][x];
		   }
		   return rd;
	   default:
	     break;
	   }
	   return null;
	}

	  /**
	   * Returns the intensity at a given point row.
	   * @return The actual data values at position (..., y), with bZero and bScale
	   * applied.
	   */
		private static double[] getOriginalValue(double[][] data, double bZero, double bScale, int x) throws FitsException
		{
		   double rd[] = new double[data.length];
		   if (bZero == 0 && bScale == 1) {
			   for (int y=0; y<rd.length; y++) {
				   rd[y] = data[y][x];
			   }
		   } else {
			   for (int y=0; y<rd.length; y++) {
				   rd[y] = bZero + bScale * data[y][x];
			   }
		   }
		   return rd;
		}

	  /**
	   * Returns the intensity at a given point row.
	   * @return The actual data values at position (..., y), with bZero and bScale
	   * applied.
	   */
		private static double[] getOriginalValue(float[][] data, double bZero, double bScale, int x) throws FitsException
		{
		   double rf[] = new double[data.length];
		   if (bZero == 0 && bScale == 1) {
			   for (int y=0; y<rf.length; y++) {
				   rf[y] = data[y][x];
			   }
		   } else {
			   for (int y=0; y<rf.length; y++) {
				   rf[y] = bZero + bScale * data[y][x];
			   }
		   }
		   return rf;
		}

	  /**
	   * Returns the intensity at a given point row.
	   * @return The actual data values at position (..., y), with bZero and bScale
	   * applied.
	   */
		private static double[] getOriginalValue(int[][] data, double bZero, double bScale, int x) throws FitsException
		{
		   double ri[] = new double[data.length];
		   if (bZero == 0 && bScale == 1) {
			   for (int y=0; y<ri.length; y++) {
				   ri[y] = data[y][x];
			   }
		   } else {
			   for (int y=0; y<ri.length; y++) {
				   ri[y] = bZero + bScale * data[y][x];
			   }
		   }
		   return ri;
		}

	  /**
	   * Returns the intensity at a given point row.
	   * @return The actual data values at position (..., y), with bZero and bScale
	   * applied.
	   */
		private static double[] getOriginalValue(short[][] data, double bZero, double bScale, int x) throws FitsException
		{
		   double rs[] = new double[data.length];
		   if (bZero == 0 && bScale == 1) {
			   for (int y=0; y<rs.length; y++) {
				   rs[y] = data[y][x];
			   }
		   } else {
			   for (int y=0; y<rs.length; y++) {
				   rs[y] = bZero + bScale * data[y][x];
			   }
		   }
		   return rs;
		}

	  /**
	   * Returns the intensity at a given point row.
	   * @return The actual data values at position (..., y), with bZero and bScale
	   * applied.
	   */
		private static double[] getOriginalValue(byte[][] data, double bZero, double bScale, int x) throws FitsException
		{
		   double rb[] = new double[data.length];
		   if (bZero == 0 && bScale == 1) {
			   for (int y=0; y<rb.length; y++) {
				   rb[y] = data[y][x];
			   }
		   } else {
			   for (int y=0; y<rb.length; y++) {
				   rb[y] = bZero + bScale * (data[y][x]);
			   }
		   }
		   return rb;
		}


	  /**
	   * Returns the intensity at a given point.
	   * @return The actual data value at position (x, y), with bZero and bScale
	   * applied.
	   */
/*		private static double getOriginalValue(double[][] data, double bZero, double bScale, int bp, int x, int y) throws FitsException
		{
		   double result = Double.NaN;

		   switch(bp)
		   {
		   case 8:
		     int dataVal = (int) (data[y][x] + 128); // workaround to apply bscale correctly
		     result = bZero + bScale * dataVal;
		     break;
		   case 16:
			   // Since data for bitpix != 8 starts at 0, bscale is applied here correctly
		     result = bZero + bScale * data[y][x];
		     break;
		   case 32:
		     result = bZero + bScale * data[y][x];
		     break;
		   case -32:
		     result = bZero + bScale * data[y][x];
		     break;
		   case -64:
		     result = bZero + bScale * data[y][x];
		     break;
		   default:
		     break;
		   }

		   return result;
		}
*/
		  /**
		   * Returns the intensity at a given point row.
		   * @return The actual data values at position (..., y), with bZero and bScale
		   * applied.
		   */
			private static double[] getOriginalValue4d(double[][][][] data, double bZero, double bScale, int bp, int x) throws FitsException
			{
			   switch(bp)
			   {
			   case 8:
				   double rb[] = new double[data.length];
				   for (int y=0; y<rb.length; y++) {
					   rb[y] = bZero + bScale * (data[0][0][y][x]);
				   }
				   return rb;
			   case 16:
				   double rs[] = new double[data.length];
				   for (int y=0; y<rs.length; y++) {
					   rs[y] = bZero + bScale * data[0][0][y][x];
				   }
				   return rs;
			   case 32:
				   double ri[] = new double[data.length];
				   for (int y=0; y<ri.length; y++) {
					   ri[y] = bZero + bScale * data[0][0][y][x];
				   }
				   return ri;
			   case -32:
				   double rf[] = new double[data.length];
				   for (int y=0; y<rf.length; y++) {
					   rf[y] = bZero + bScale * data[0][0][y][x];
				   }
				   return rf;
			   case -64:
				   double rd[] = new double[data.length];
				   for (int y=0; y<rd.length; y++) {
					   rd[y] = bZero + bScale * data[0][0][y][x];
				   }
				   return rd;
			   default:
			     break;
			   }
			   return null;
			}

	  /**
	   * Returns the intensity at a given point.
	   * @return The actual data value at postion (x, y), with bZero and bScale
	   * applied.
	   */
/*		private static double getOriginalValue4d(double[][][][] data, double bZero, double bScale, int bp, int x, int y) throws FitsException
		{
		   double result = Double.NaN;

		   switch(bp)
		   {
		   case 8:
		     int dataVal = (int) (data[0][0][y][x] + 128);
		     result = bZero + bScale * dataVal;
		     break;
		   case 16:
		     result = bZero + bScale * data[0][0][y][x];
		     break;
		   case 32:
		     result = bZero + bScale * data[0][0][y][x];
		     break;
		   case -32:
		     result = bZero + bScale * data[0][0][y][x];
		     break;
		   case -64:
		     result = bZero + bScale * data[0][0][y][x];
		     break;
		   default:
		     break;
		   }

		   return result;
		}
*/

	/**
	 * Creates a simple fits HDU.
	 * @param data The data. The raw values are modified considering the BZERO and BSCALE
	 * in the provided header. You may want to call after {@linkplain #setData(Object, int, double, double)},
	 * providing the values 0 for BZERO and 1 for BSCALE, so that the raw values are conserved.
	 * @param header The header, or null to set no one.
	 * @return The HDU.
	 * @throws JPARSECException If an error occurs.
	 */
	public static BasicHDU<?> createHDU(Object data, HeaderElement header[]) throws JPARSECException {
		try {
			if (data == null) {
				BasicHDU<?> hdu = Fits.makeHDU(FitsBinaryTable.createHeader(header));
				FitsIO.setHeader(hdu, header);
				return hdu;
			} else {
				BasicHDU<?> hdu = FitsFactory.HDUFactory(data);
				hdu = FitsFactory.HDUFactory(getData(hdu, false, -1));
				if (header != null) FitsIO.setHeader(hdu, header);
				return hdu;
			}
		} catch (Exception exc) {
			throw new JPARSECException("Cannot create the HDU", exc);
		}
	}

	/**
	 * Returns the HDU image for a given index.
	 * @param n The index of the image.
	 * @return The HDU.
	 */
	public BasicHDU<?> getHDU(int n) {
		return this.hdus[n];
	}

	/**
	 * Adds a new HDU to the current instance.
	 * @param hdu The new HDU.
	 */
	public void addHDU(BasicHDU<?> hdu) {
		BasicHDU<?> newhdus[] = new BasicHDU[hdus.length+1];
		for (int i=0; i<hdus.length; i++) {
			newhdus[i] = hdus[i];
		}
		newhdus[hdus.length] = hdu;
		hdus = newhdus;
	}

	/**
	 * Removes an HDU to the current instance.
	 * @param n The index of the HDU to remove.
	 */
	public void removeHDU(int n) {
		BasicHDU<?> newhdus[] = new BasicHDU[hdus.length-1];
		int index = -1;
		boolean removed = false;
		for (int i=0; i<hdus.length; i++) {
			if (i != n) {
				index ++;
				newhdus[index] = hdus[i];
			} else {
				removed = true;
			}
		}
		if (removed) hdus = newhdus;
	}

	/**
	 * Replaces an HDU.
	 * @param n The index of the HDU to replace.
	 * @param newHDU The new HDU.
	 */
	public void replaceHDU(int n, BasicHDU<?> newHDU) {
		if (n >= 0 && n < hdus.length) hdus[n] = newHDU;
	}

	/**
	 * Returns if a given HDU is a binary table.
	 * @param n The index of the HDU.
	 * @return True or false.
	 */
	public boolean isBinaryTable(int n) {
		return isBinaryTable(getHDU(n));
	}

	/**
	 * Returns if a given HDU is an Ascii table.
	 * @param n The index of the HDU.
	 * @return True or false.
	 */
	public boolean isAsciiTable(int n) {
		return isAsciiTable(getHDU(n));
	}

	/**
	 * Returns if a given HDU is a binary table.
	 * @param hdu The HDU.
	 * @return True or false.
	 */
	public static boolean isBinaryTable(BasicHDU<?> hdu) {
		return hdu.getClass().getSimpleName().equals("BinaryTableHDU");
	}

	/**
	 * Returns if a given HDU is an Ascii table.
	 * @param hdu The HDU.
	 * @return True or false.
	 */
	public static boolean isAsciiTable(BasicHDU<?> hdu) {
		return hdu.getClass().getSimpleName().equals("AsciiTableHDU");
	}

	/**
	 * Returns a String representation of this object.
	 * In case of error getting the information from the
	 * fits, null will be returned.
	 */
	@Override
	public String toString() {
		String sep = FileIO.getLineSeparator();
		int nameAdd = 4;
		int add = 4 + nameAdd;
		FileFormatElement format[] = new FileFormatElement[] {
				new FileFormatElement(1, 3, "No."),
				new FileFormatElement(5, 9+nameAdd, "Name"),
				new FileFormatElement(11+nameAdd, 20+add, "Type"),
				new FileFormatElement(22+add, 26+add, "Cards"),
				new FileFormatElement(28+add, 37+add, "Dimensions"),
				new FileFormatElement(39+add, 47+add, "Format")
		};
		HeaderElement p[] = ReadFormat.getFieldsAsParameters(format);
		try {
			StringBuffer out = new StringBuffer(WriteFile.getFormattedEntry(p, format)+sep);
			for (int i=0; i<hdus.length; i++) {
				String type = hdus[i].getClass().toString();
				int axes[] = hdus[i].getAxes();
				type = type.substring(type.lastIndexOf(".")+1);
				String dataFormat = "";
				int bitPix = hdus[i].getBitPix();
				if (bitPix == 8) dataFormat = "byte8";
				if (bitPix == 16) dataFormat = "short16";
				if (bitPix == 32) dataFormat = "long32";
				if (bitPix == -32) dataFormat = "float32";
				if (bitPix == 64) dataFormat = "double64";
				p[0] = new HeaderElement(""+(i), format[0].fieldName);
				p[1] = new HeaderElement(""+hdus[i].getObject(), format[1].fieldName);
				HeaderElement h[] = FitsIO.getHeader(hdus[i].getHeader());
				int tableID = HeaderElement.getIndex(h, "EXTNAME");
				if (hdus[i].getObject() == null && isBinaryTable(hdus[i]) && tableID >= 0)
					p[1] = new HeaderElement(h[tableID].getAsString(), format[1].fieldName);
				p[2] = new HeaderElement(type, format[2].fieldName);
				p[3] = new HeaderElement(""+hdus[i].getHeader().getNumberOfCards(), format[3].fieldName);
				if (axes == null) {
					p[4] = new HeaderElement("-", format[4].fieldName);
				} else {
					p[4] = new HeaderElement(""+DataSet.toString(DataSet.reverse(DataSet.toStringValues(axes)), "x"), format[4].fieldName);
				}
				p[5] = new HeaderElement(dataFormat, format[5].fieldName);
				for (int j=0; j<=5; j++) {
					p[j].key = p[j].comment;
				}
				out.append(WriteFile.getFormattedEntry(p, format)+sep);
			}
			return out.toString();
		} catch (Exception exc) {
			exc.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns the WCS instance for a given HDU.
	 * @param n The index, from 0 to number of HDUs-1.
	 * @return The WCS instance.
	 * @throws JPARSECException If an error occurs.
	 */
	public WCS getWCS (int n) throws JPARSECException {
		return new WCS(this.getHeader(n));
	}
	/**
	 * Sets the WCS of a given HDU, by updating its header.
	 * @param n The index of the HDU in the current file,
	 * starting from 0.
	 * @param wcs The new WCS instance.
	 * @throws JPARSECException If an error occurs.
	 */
	public void setWCS(int n, WCS wcs) throws JPARSECException {
		HeaderElement newHeader[] = this.getHeader(n);
		newHeader = HeaderElement.addHeaderEntry(newHeader, wcs.getAsHeader());
		this.setHeader(n, newHeader);
	}

	/**
	 * Launches SExtractor tool and retrieve the results of solving the sources
	 * in the current image. SExtractor is launched using the settings contained in
	 * the header, or with its default settings if those settings are not found.
	 * The minimum detection area is set to 3 pixels, and the threshold (sigma) to 5.
	 * @param n The HDU index to process.
	 * @param minArea The minimum number of pixels above sigma to consider a source as detected. Set it between
	 * 3 and 10 depending on how sensitive you want the source detection algorithm to be.
	 * @param sigma How much times the pixels should be above the background emission to consider a
	 * source as detected. Set it between 5 and 10.
	 * @return The instance with the results.
	 * @throws JPARSECException If an error occurs.
	 */
	public SExtractor solveSources(int n, int minArea, int sigma) throws JPARSECException {
		String path = FileIO.getTemporalDirectory();
		String lastImagePath = path+"img.fits";
		this.writeEntireFits(lastImagePath);
		try {
			double seeing = 1.0, mag0 = 0.0, pixScale = 0;
			FitsIO fio = new FitsIO(lastImagePath);
			HeaderElement header[] = fio.getHeader(0);
			double gain = 0.0;
			try { gain = header[HeaderElement.getIndex(header, "GAIN")].getAsDouble(); } catch (Exception exc) {}
			if (gain == 0) gain = 1;
			double saturation = 0.0;
			try { saturation = header[HeaderElement.getIndex(header, "MAXADU")].getAsDouble(); } catch (Exception exc) {}
			if (saturation == 0) {
				int depth = 0;
				try { depth = header[HeaderElement.getIndex(header, "DEPTH")].getAsInt(); } catch (Exception exc) {}
				if (depth > 0) saturation = Math.pow(2.0, depth-1) - 1;
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
			SExtractor.createMachineConfigFile(path, saturation, gain, pixScale, seeing, mag0, minArea, sigma);
		} catch (Exception exc) {
			exc.printStackTrace();
		}
		SExtractor sex = new SExtractor(path, "machine.config");
		sex.execute("img.fits");
		return sex;
	}
}
