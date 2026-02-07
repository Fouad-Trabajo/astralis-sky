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
package jparsec.astronomy;

import java.io.Serializable;
import java.util.ArrayList;

import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.util.JPARSECException;

/**
 * A class to hold and to calculate the properties of a CCD camera or webcam. Calculations
 * are based on theoretical statements, so the real life could be somewhat
 * different. The CCD camera/webcam is supposed to be attached to a telescope instance,
 * with the possible presence of an ocular between the telescope and the camera. If
 * you want to make calculations for direct focus photography, just set the ocular to null
 * in the telescope instance.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class CCDElement implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a default CCD with the TouCam webcam.
	 */
	public CCDElement()
	{
		try {
			CCDElement TouCam = getCCD("TouCam");
			chipSizeX = TouCam.chipSizeX;
			chipSizeY = TouCam.chipSizeY;
			pixelSizeX = TouCam.pixelSizeX;
			pixelSizeY = TouCam.pixelSizeY;
			binningFactor = 1;
			zoomFactor = 1.0f;
			name = TouCam.name;
		} catch (Exception exc) {
			chipSizeX = 640;
			chipSizeY = 480;
			pixelSizeX = 5.6f;
			pixelSizeY = 5.6f;
			binningFactor = 1;
			zoomFactor = 1.0f;
			name = "TouCam";
		}
	}

	/**
	 * Creates a CCD object by giving the values of the fields.
	 *
	 * @param n Name of the camera.
	 * @param csx Number of pixels in x.
	 * @param csy Number of pixels in y.
	 * @param psx Pixel size in x (microns).
	 * @param psy Pixel size in y (microns).
	 * @param binning Binning factor.
	 */
	public CCDElement(String n, int csx, int csy, float psx, float psy,
			int binning)
	{
		chipSizeX = csx;
		chipSizeY = csy;
		pixelSizeX = psx;
		pixelSizeY = psy;
		binningFactor = binning;
		zoomFactor = 1.0f;
		name = n;
	}

	/**
	 * Name of the CCD.
	 */
	public String name;

	/**
	 * Size of the chip in horizontal, in number of pixels.
	 */
	public int chipSizeX;

	/**
	 * Size of the chip in vertical, in number of pixels.
	 */
	public int chipSizeY;

	/**
	 * Size of a pixel in horizontal, in microns.
	 */
	public float pixelSizeX;

	/**
	 * Size of a pixel in vertical, in microns.
	 */
	public float pixelSizeY;

	/**
	 * Position angle (orientation) of the camera in radians, 0 by default.
	 */
	public float cameraPA = 0;

	/**
	 * Binning factor, for example 2 for a 2x2 binning. Default is 1.
	 */
	public int binningFactor;
	/**
	 * Zoom factor, for example 2 (more zoom, less field) or 0.5. Default if 1.
	 */
	private float zoomFactor;

	/**
	 * To clone the object.
	 */
	@Override
	public CCDElement clone()
	{
		CCDElement ccd = new CCDElement(this.name, this.chipSizeX, this.chipSizeY, this.pixelSizeX,
				this.pixelSizeY, this.binningFactor);
		ccd.cameraPA = this.cameraPA;
		return ccd;
	}

	/**
	 * Returns true if a given CCD is equals to another.
	 * @param ccd A CCD object.
	 * @return True or false.
	 */
	@Override
	public boolean equals(Object ccd) {
		if (this == ccd) return true;

		if (!(ccd instanceof CCDElement)) return false;

		CCDElement that = (CCDElement) ccd;

		if (chipSizeX != that.chipSizeX) return false;
		if (chipSizeY != that.chipSizeY) return false;
		if (Float.compare(that.pixelSizeX, pixelSizeX) != 0) return false;
		if (Float.compare(that.pixelSizeY, pixelSizeY) != 0) return false;
		if (Float.compare(that.cameraPA, cameraPA) != 0) return false;
		if (binningFactor != that.binningFactor) return false;
		if (Float.compare(that.zoomFactor, zoomFactor) != 0) return false;

		return !(name != null ? !name.equals(that.name) : that.name != null);
	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + chipSizeX;
		result = 31 * result + chipSizeY;
		result = 31 * result + (pixelSizeX != +0.0f ? Float.floatToIntBits(pixelSizeX) : 0);
		result = 31 * result + (pixelSizeY != +0.0f ? Float.floatToIntBits(pixelSizeY) : 0);
		result = 31 * result + (cameraPA != +0.0f ? Float.floatToIntBits(cameraPA) : 0);
		result = 31 * result + binningFactor;
		result = 31 * result + (zoomFactor != +0.0f ? Float.floatToIntBits(zoomFactor) : 0);
		return result;
	}
	
	/**
	 * Return all available intrinsic CCD cameras.
	 * @return The cameras.
	 * @throws JPARSECException If an error occurs.
	 */
	public static CCDElement[] getAllAvailableCCDs() throws JPARSECException
	{
		String jarpath = FileIO.DATA_SKY_DIRECTORY + "ccd.txt";
		ArrayList<String> v = ReadFile.readResource(jarpath);
		CCDElement ccd[] = new CCDElement[v.size()];

		for (int i = 0; i < v.size(); i++)
		{
			String data[] = DataSet.toStringArray(v.get(i), " ", true);
			int n = data.length;
			String name = DataSet.toString(DataSet.getSubArray(data, 0, n-5), " ");
			ccd[i] = new CCDElement(name, Integer.parseInt(data[n-4].trim()), Integer.parseInt(data[n-3].trim()),
					Float.parseFloat(data[n-2].trim()), Float.parseFloat(data[n-1].trim()), 1);
		}
		return ccd;
	}

	/**
	 * Return the names of all available intrinsic CCD cameras.
	 * @return The list of cameras.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String[] getNamesOfAllAvailableCCDs() throws JPARSECException
	{
		String jarpath = FileIO.DATA_SKY_DIRECTORY + "ccd.txt";
		ArrayList<String> v = ReadFile.readResource(jarpath);
		String ccd[] = new String[v.size()];

		for (int i = 0; i < v.size(); i++)
		{
			String data[] = DataSet.toStringArray(v.get(i), " ", true);
			int n = data.length;
			String name = DataSet.toString(DataSet.getSubArray(data, 0, n-5), " ");
			ccd[i] = name;
		}
		return ccd;
	}

	/**
	 * Return if a given CCD camera exits or not.
	 * @param name The camera name.
	 * @param caseSensitive Select if the name is case sensitive.
	 * @param partialName Select if input string should be treated strictly or if it is part of the camera name.
	 * @return True or false.
	 * @throws JPARSECException If an error occurs.
	 */
	public static boolean CCDexits(String name, boolean caseSensitive, boolean partialName) throws JPARSECException
	{
		String jarpath = FileIO.DATA_SKY_DIRECTORY + "ccd.txt";
		ArrayList<String> v = ReadFile.readResource(jarpath);

		for (int i = 0; i < v.size(); i++)
		{
			String data[] = DataSet.toStringArray(v.get(i), " ", true);
			int n = data.length;
			String ccd = DataSet.toString(DataSet.getSubArray(data, 0, n-5), " ");
			if (caseSensitive) {
				if (partialName && ccd.indexOf(name) >= 0)
					return true;
				if (ccd.equals(name)) return true;
				continue;
			}
			if (partialName && ccd.toLowerCase().indexOf(name.toLowerCase()) >= 0)
				return true;
			if (ccd.toLowerCase().equals(name.toLowerCase())) return true;
		}
		return false;
	}
	/**
	 * Return certain CCD.
	 *
	 * @param ccd Name of the CCD;
	 * @return The required CCD object, or null if none is found.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public static CCDElement getCCD(String ccd) throws JPARSECException
	{
		CCDElement ccds[] = CCDElement.getAllAvailableCCDs();
		CCDElement out = null;

		int what = -1;
		for (int i = 0; i < ccds.length; i++)
		{
			if (ccds[i].name.toLowerCase().indexOf(ccd.toLowerCase()) >= 0)
				what = i;
			if (ccds[i].name.toLowerCase().equals(ccd.toLowerCase())) break;
		}
		if (what >= 0)
			out = ccds[what];

		return out;
	}

	/**
	 * Returns the scale of the CCD image in x.
	 * @param telescope The telescope where the camera is attached to, including
	 * the ocular (or with a null ocular for direct focus photography).
	 * @return The scale in radians per pixel.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getScaleX(TelescopeElement telescope)
	throws JPARSECException {
		 double fovWidth  = fovSingleDimension(this.pixelSizeX * this.binningFactor, this.chipSizeX, telescope);

		 double arcWidth  = fovWidth / this.chipSizeX;
		 return arcWidth;
	}

	/**
	 * Returns the scale of the CCD image in y.
	 * @param telescope The telescope where the camera is attached to, including
	 * the ocular (or with a null ocular for direct focus photography).
	 * @return The scale in radians per pixel.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getScaleY(TelescopeElement telescope)
	throws JPARSECException {
		 double fovHeight = fovSingleDimension(this.pixelSizeY * this.binningFactor, this.chipSizeY, telescope);

		 double arcHeight = fovHeight / this.chipSizeY;
		 return arcHeight;
	}

	/**
	 * Returns the scale of the CCD image as a mean average of scale in x and y.
	 * @param telescope The telescope where the camera is attached to, including
	 * the ocular (or with a null ocular for direct focus photography).
	 * @return The scale in radians per pixel.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getScale(TelescopeElement telescope)
	throws JPARSECException {
		return 0.5 * (this.getScaleX(telescope) + this.getScaleY(telescope));
	}

	/**
	 * Returns the field of view in x direction.
	 * @param telescope The telescope where the camera is attached to, including
	 * the ocular (or with a null ocular for direct focus photography).
	 * @return Field of view in radians.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getFieldX(TelescopeElement telescope)
	throws JPARSECException {
		double fovWidth  = fovSingleDimension(this.pixelSizeX, this.chipSizeX, telescope);
		return fovWidth;
	}

	/**
	 * Returns the field of view in y direction.
	 * @param telescope The telescope where the camera is attached to, including
	 * the ocular (or with a null ocular for direct focus photography).
	 * @return Field of view in radians.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getFieldY(TelescopeElement telescope)
	throws JPARSECException  {
		double fovHeight = fovSingleDimension(this.pixelSizeY, this.chipSizeY, telescope);
		return fovHeight;
	}

	private double fovSingleDimension(double ps, double cs, TelescopeElement telescope)
	throws JPARSECException {
		this.zoomFactor = 1.0f;
		if (telescope.ocular != null) this.zoomFactor = (float) telescope.getMagnification();
		double fovsingle = 2.0 * Math.atan(((ps * cs) / 2000.0) / (telescope.focalLength * this.zoomFactor));

		return fovsingle;
	}
}
