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

import jparsec.io.FileFormatElement;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.io.ReadFormat;
import jparsec.math.Constant;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;

/**
 * A class to hold and to calculate the properties of an ocular. Calculations
 * are based on theoretical statements, so the real life could be somewhat
 * different.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class OcularElement implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a default OcularElement with focal length 20 mm, field of view 50 degrees,
	 * reticle 32 mm.
	 */
	public OcularElement()
	{
		focalLength = 20;
		fieldOfView = 50.0 * Constant.DEG_TO_RAD;
		reticleSize = 32;
		name = Translate.translate(64)+" 1-1/4, 20mm, 50\u00b0";
	}

	/**
	 * Creates an Ocular object by giving the values of the fields.
	 *
	 * @param n Name of the ocular.
	 * @param focal Focal length of the ocular in mm.
	 * @param field Apparent field of view in radians.
	 * @param reticle Size of the reticle in mm.
	 */
	public OcularElement(String n, float focal, double field, int reticle)
	{
		focalLength = focal;
		fieldOfView = field;
		reticleSize = reticle;
		name = n;
	}

	/**
	 * Name of the ocular.
	 */
	public String name;

	/**
	 * Focal length of the ocular in mm. When rendering planets, 0 can be passed
	 * to view the disk occupying 50% of the width.
	 */
	public float focalLength;

	/**
	 * Apparent field of view in radians.
	 */
	public double fieldOfView;

	/**
	 * Diameter of the reticle in mm. Typical values are 32 mm (1.25 inch
	 * oculars) and 52 mm (2.0 inch oculars).
	 */
	public int reticleSize;

	/**
	 * To clone the object.
	 */
	@Override
	public OcularElement clone()
	{
		OcularElement ocular = new OcularElement(this.name, this.focalLength, this.fieldOfView, this.reticleSize);
		return ocular;
	}

	/**
	 * Returns true if a given ocular is equals to another.
	 * @param ocular An ocular object.
	 * @return True or false.
	 */
	@Override
	public boolean equals(Object ocular) {
		if (this == ocular) return true;

		if (!(ocular instanceof OcularElement)) return false;

		OcularElement that = (OcularElement) ocular;

		if (Float.compare(that.focalLength, focalLength) != 0) return false;
		if (Double.compare(that.fieldOfView, fieldOfView) != 0) return false;
		if (reticleSize != that.reticleSize) return false;

		return !(name != null ? !name.equals(that.name) : that.name != null);
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		result = name != null ? name.hashCode() : 0;
		result = 31 * result + (focalLength != +0.0f ? Float.floatToIntBits(focalLength) : 0);
		temp = Double.doubleToLongBits(fieldOfView);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + reticleSize;
		return result;
	}
	/**
	 * Return all oculars from external file.
	 *
	 * @return Array of Ocular objects.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public static OcularElement[] getAllAvailableOculars() throws JPARSECException
	{
		OcularElement oculars[] = getOcularsFromExternalFile(FileIO.DATA_SKY_DIRECTORY + "eyepiece.txt",
				FileFormatElement.OCULARS);

		return oculars;
	}
	/**
	 * Return all oculars from external file.
	 *
	 * @return Array of ocular names.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public static String[] getNamesOfAllAvailableOculars() throws JPARSECException
	{
		OcularElement oculars[] = getAllAvailableOculars();
		String n[] = new String[oculars.length];
		for (int i=0; i<n.length;i++)
		{
			n[i] = oculars[i].name;
		}
		return n;
	}

	/**
	 * Return all oculars from an external file. Designed specially for input
	 * file eyepiece.txt in sky.jar.
	 *
	 * @param jarpath Path to the file.
	 * @param fmt File format array with ocular name as NAME, focal length in mm
	 *        as FOCAL, and field of view as FIELD in degrees.
	 * @return Array of Ocular objects.
	 * @see FileFormatElement
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public static OcularElement[] getOcularsFromExternalFile(String jarpath, FileFormatElement[] fmt)
			throws JPARSECException
	{
		ArrayList<String> v = ReadFile.readResource(jarpath);
		OcularElement ocl[] = new OcularElement[v.size()];

		// Default value, 1 1/4 inch
		int reticle = 32;

		ReadFormat rf = new ReadFormat();

		rf.setFormatToRead(fmt);
		for (int i = 0; i < v.size(); i++)
		{
			String name = rf.readString(v.get(i), "NAME");
			float focal = (float) rf.readDouble(v.get(i), "FOCAL");
			double field = rf.readDouble(v.get(i), "FIELD") * Constant.DEG_TO_RAD;

			ocl[i] = new OcularElement(name, focal, field, reticle);
		}
		return ocl;
	}

	/**
	 * Return certain ocular.
	 *
	 * @param ocular_name Name of the ocular;
	 * @return The required Ocular object, or null if none is found.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public static OcularElement getOcular(String ocular_name) throws JPARSECException
	{
		OcularElement oculars[] = OcularElement.getAllAvailableOculars();
		OcularElement ocular = null;

		int what = -1;
		for (int i = 0; i < oculars.length; i++)
		{
			if (oculars[i].name.indexOf(ocular_name) >= 0)
				what = i;
		}
		if (what >= 0)
			ocular = oculars[what];

		return ocular;
	}
}
