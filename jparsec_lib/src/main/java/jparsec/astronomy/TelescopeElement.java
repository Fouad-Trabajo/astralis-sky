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
import jparsec.math.FastMath;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;

/**
 * A class to hold and to calculate the properties of a telescope. Calculations
 * are based on theoretical statements, so the real life could be somewhat
 * different. This instance allows to attach a camera ({@linkplain CCDElement}),
 * but this should be done only with a null ocular ({@linkplain OcularElement}).
 * The method {@linkplain #attachCCDCamera(CCDElement)} is recommended to change
 * between visual and CCD modes. You can also use {@linkplain CCDElement} directly
 * for quick calculations, including those unsupported here like having an ocular
 * between the telescope and the CCD camera.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class TelescopeElement implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a default TelescopeElement with no information.
	 */
	public TelescopeElement() { name = ""; }

	/**
	 * Creates a Telescope object by giving the values of the fields. The invert horizontal/vertical
	 * flags are set automatically using the name of the telescope. For catadioptric telescopes
	 * {@linkplain #invertHorizontal} is set to true, supposing you are looking using the diagonal
	 * mirror. For Newtonians and refractors both {@linkplain #invertHorizontal} and
	 * {@linkplain #invertVertical} are set to true, supposing you are not using an erecting prism.
	 *
	 * @param n Name of the telescope.
	 * @param focal Focal length of the telescope in mm.
	 * @param diam Diameter in mm.
	 * @param obstruction Central obstruction in mm.
	 * @param spiders Secondary spiders width in mm.
	 * @param cromatism Cromatic aberration level, from 0 to 5.
	 * @param ocl Ocular object.
	 */
	public TelescopeElement(String n, int focal, int diam, int obstruction, int spiders, float cromatism,
			OcularElement ocl)
	{
		focalLength = focal;
		diameter = diam;
		centralObstruction = obstruction;
		spidersSize = spiders;
		ocular = null;
		lastOcular = null;
		if (ocl != null) {
			ocular = ocl.clone();
			lastOcular = ocular.clone();
		}
		name = n;
		cromatismLevel = cromatism;

		this.invertHorizontal = false;
		this.invertVertical = false;
		if (name.toLowerCase().indexOf("binocular") >= 0 || name.toLowerCase().indexOf("human") >= 0 || name.toLowerCase().indexOf("obje") >= 0 ) return;

		boolean sct = false, newton = false, refractor = false;
		if (name.equals("SCT") || name.indexOf("Schmidt-Cassegrain") >= 0 || name.indexOf(" SCT") >= 0 || name.indexOf(" Mak") >= 0 || name.indexOf(" Cass") >= 0) sct = true;
		if (name.indexOf("Newton") >= 0 || name.indexOf(" Newt") >= 0 || name.indexOf(" Dob") >= 0 ) newton = true;
		if (name.equals("Refractor") || name.indexOf(" Refr") >= 0) refractor = true;
		if (!sct && !newton && !refractor) { // Account for scopes in the scope.txt file that doesn't have clear IDs
			if (diameter < 150) {
				refractor = true;
			} else {
				newton = true;
			}
		}

		if (sct && !newton && !refractor) {
			this.invertHorizontal = true; // Only horizontal flip, assuming the diagonal mirror is used. Otherwise it would be like a refractor
		}
		if (!sct && newton && !refractor) { // Classic inverted view of the Newtonians
			this.invertHorizontal = true;
			this.invertVertical = true;
		}
		if (!sct && !newton && refractor) { // Without erecting prism we have an inverted image
			this.invertHorizontal = true;
			this.invertVertical = true;
		}
	}

	/**
	 * Name of the telescope.
	 */
	public String name;

	/**
	 * Focal length of the telescope in mm.
	 */
	public int focalLength;

	/**
	 * Diameter of the telescope in mm.
	 */
	public int diameter;

	/**
	 * Diameter of the central obstruction (secondary in Newton telescopes) in
	 * mm.
	 */
	public int centralObstruction;

	/**
	 * Width of the spiders in mm in Newton telescopes.
	 */
	public int spidersSize;

	/**
	 * Ocular object.
	 */
	public OcularElement ocular;
	private OcularElement lastOcular;

	/**
	 * CCD camera attached to the telescope instead of an ocular. To be used
	 * this value must be non null and ocular must be null;
	 */
	public CCDElement ccd;

	/**
	 * Level of cromatism, from 0 (null) to a recommended maximum value of 2
	 * (medium-high). A level of 2 is defined as a cromatism where the distance
	 * between the red and the blue rays are about 2 arcseconds.
	 */
	public float cromatismLevel;

	/**
	 * True if the telescope inverts the image from East-West.
	 */
	public boolean invertHorizontal;

	/**
	 * True if the telescope inverts the vision from North-South.
	 */
	public boolean invertVertical;

	/**
	 * Returns an adequate object for a typical 20 cm f/10 Schmidt-Cassegrain
	 * reflector telescope with the default ocular.
	 */
	public static final TelescopeElement SCHMIDT_CASSEGRAIN_20cm =
		new TelescopeElement("Schmidt-Cassegrain 20cm f/10", 2003, 200, 80, 0, 0.75f, new OcularElement());


	/**
	 * Returns an adequate object for a typical 40 cm f/10 Schmidt-Cassegrain
	 * reflector telescope with the default ocular.
	 */
	public static final TelescopeElement SCHMIDT_CASSEGRAIN_40cm =
		new TelescopeElement("Schmidt-Cassegrain 40cm f/10", 4000, 400, 150, 0, 0.75f, new OcularElement());

	/**
	 * Returns an adequate object for a semi-professional 80 cm f/10
	 * Schmidt-Cassegrain reflector telescope with the default ocular.
	 */
	public static final TelescopeElement SCHMIDT_CASSEGRAIN_80cm =
		new TelescopeElement("Schmidt-Cassegrain 80cm f/10", 8000, 800, 300, 0, 0.75f, new OcularElement());

	/**
	 * Returns an adequate object for a typical 20 cm f/8 Newton reflector
	 * telescope with the default ocular.
	 */
	public static final TelescopeElement NEWTON_20cm =
		new TelescopeElement("Newton 20cm f/8", 1600, 200, 20, 10, 0.4f, new OcularElement());

	/**
	 * Returns an adequate object for one human eye.
	 */
	public static final TelescopeElement HUMAN_EYE =
		new TelescopeElement("Human eye", 14, 7, 0, 0, 0.3f, null);

	/**
	 * Returns an adequate object for a typical 10 cm f/8 acromatic refractor
	 * telescope with the default ocular.
	 */
	public static final TelescopeElement REFRACTOR_10cm =
		new TelescopeElement("Refractor 10cm f/8", 800, 100, 0, 0, 1.5f, new OcularElement());

	/**
	 * Returns an adequate object for a typical 10 cm f/5 apocromatic refractor
	 * telescope with the default ocular.
	 */
	public static final TelescopeElement REFRACTOR_10cm_f5 =
		new TelescopeElement("Refractor 10cm f/5", 500, 100, 0, 0, 0f, new OcularElement());

	/**
	 * Returns an adequate object for a typical 11x80 binoculars. Reticle size
	 * is set to output pupil size.
	 */
	public static final TelescopeElement BINOCULARS_11x80 =
		new TelescopeElement("Binoculars 11x80", 220, 80, 0, 0, 0,
				new OcularElement("Binoculars", 17, 65 * Constant.DEG_TO_RAD, 7));

	/**
	 * Returns an adequate object for a typical 7x50 binoculars. Reticle size is
	 * set to output pupil size.
	 */
	public static final TelescopeElement BINOCULARS_7x50 =
		new TelescopeElement("Binoculars 7x50", 140, 50, 0, 0, 0,
				new OcularElement("Binoculars", 17, 65 * Constant.DEG_TO_RAD, 7));

	/**
	 * Returns an adequate object for a typical 300 mm f/2.8 apochromatic
	 * objective. Reticle size is set to 43mm.
	 */
	public static final TelescopeElement OBJECTIVE_300mm_f2_8 =
		new TelescopeElement("Objective 300mm f/2.8", 300, 107, 0, 0, 0,
				new OcularElement("Human eye", 38, 65 * Constant.DEG_TO_RAD, 43));

	/**
	 * Returns an adequate object for a typical 50 mm f/1.4
	 * objective. Reticle size is set to 46mm.
	 */
	public static final TelescopeElement OBJECTIVE_50mm_f1_4 =
		new TelescopeElement("Objective 50mm f/1.4", 50, 36, 0, 0, 0,
				new OcularElement("Human eye", 38, 65 * Constant.DEG_TO_RAD, 46));

	/**
	 * Return all intrinsic telescopes. This methods also updates the names of the
	 * static list of telescopes, in case the default language has changed.
	 *
	 * @return Array of Telescope objects.
	 */
	public static TelescopeElement[] getTelescopes()
	{
		TelescopeElement tel[] = new TelescopeElement[]
		{ TelescopeElement.SCHMIDT_CASSEGRAIN_20cm, TelescopeElement.SCHMIDT_CASSEGRAIN_40cm,
				TelescopeElement.SCHMIDT_CASSEGRAIN_80cm, TelescopeElement.NEWTON_20cm,
				TelescopeElement.REFRACTOR_10cm, TelescopeElement.REFRACTOR_10cm_f5, TelescopeElement.OBJECTIVE_300mm_f2_8,
				TelescopeElement.BINOCULARS_11x80, TelescopeElement.BINOCULARS_7x50,
				TelescopeElement.OBJECTIVE_50mm_f1_4, TelescopeElement.HUMAN_EYE };
		if (Translate.getDefaultLanguage() != Translate.LANGUAGE.ENGLISH)  {
			for (int i=6; i<tel.length-1; i++) {
				tel[i].name = Translate.translate(FileIO.getField(1, tel[i].name, " ", true)) + " " + FileIO.getRestAfterField(1, tel[i].name, " ", true);
			}
			tel[tel.length-1].name = Translate.translate(tel[tel.length-1].name);
		}

		for (int i=0; i<tel.length; i++) {
			if (tel[i].ocular != null) {
				tel[i].ocular.name = Translate.translate(tel[i].ocular.name);
			}
		}
		return tel;
	}

	/**
	 * Return all telescopes, intrinsic plus from external file.
	 *
	 * @return Array of Telescope objects.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public static TelescopeElement[] getAllAvailableTelescopes() throws JPARSECException
	{
		TelescopeElement tel[] = getTelescopesFromExternalFile("sky", FileIO.DATA_SKY_DIRECTORY + "scope.txt",
				FileFormatElement.TELESCOPES);
		TelescopeElement tel0[] = getTelescopes();

		int ntel = tel.length + tel0.length;
		TelescopeElement telfull[] = new TelescopeElement[ntel];

		for (int i = 0; i < tel0.length; i++)
		{
			telfull[i] = new TelescopeElement(tel0[i].name, tel0[i].focalLength, tel0[i].diameter,
					tel0[i].centralObstruction, tel0[i].spidersSize, tel0[i].cromatismLevel, tel0[i].ocular);
		}

		OcularElement ocular = new OcularElement();
		for (int i = 0; i < tel.length; i++)
		{
			telfull[i + tel0.length] = new TelescopeElement(tel[i].name, tel[i].focalLength, tel[i].diameter,
					tel[i].centralObstruction, tel[i].spidersSize, tel[i].cromatismLevel, ocular);
		}
		return telfull;
	}
	/**
	 * Return all telescopes, intrinsic plus from external file.
	 *
	 * @return Array of telescope names.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public static String[] getNamesOfAllAvailableTelescopes() throws JPARSECException
	{
		TelescopeElement tel[] = TelescopeElement.getAllAvailableTelescopes();
		String n[] = new String[tel.length];
		for (int i=0; i<n.length;i++)
		{
			n[i] = tel[i].name;
		}
		return n;
	}

	/**
	 * Return all telescopes from an external file. Designed specially for input
	 * file scope.txt in sky.jar.
	 *
	 * @param jarfile Name of the jarfile.
	 * @param jarpath Path to the file.
	 * @param fmt File format array with telescope name as NAME, diameter in mm
	 *        as DIAMETER, and focal ratio as F.
	 * @return Array of Telescope objects.
	 * @see FileFormatElement
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public static TelescopeElement[] getTelescopesFromExternalFile(String jarfile, String jarpath, FileFormatElement[] fmt)
			throws JPARSECException
	{
		ArrayList<String> v = ReadFile.readResource(jarpath);
		TelescopeElement tel[] = new TelescopeElement[v.size()];

		// Default values
		int obstruction = 40; // 40%, only for SC telescopes
		int obstruction_Newton = 20; // 20%, only for Newton telescopes
		int spiders = 5; // 5%, only for Newton telescopes

		OcularElement ocular = new OcularElement();
		ReadFormat rf = new ReadFormat();

		rf.setFormatToRead(fmt);
		for (int i = 0; i < v.size(); i++)
		{
			String name = rf.readString(v.get(i), "NAME");
			int diam = rf.readInteger(v.get(i), "DIAMETER");
			double fr = rf.readDouble(v.get(i), "F");

			int a = name.indexOf("SCT");
			int b = name.indexOf("Newt");
			int c = name.indexOf("Refr");
			float crom = 1.5f;
			int obst = 0;
			if (a >= 0)
			{
				obst = obstruction;
				crom = 0.75f;
			}
			int spid = 0;
			if (b >= 0)
			{
				spid = spiders;
				obst = obstruction_Newton;
				crom = 0.4f;
			}
			if (c >= 0)
			{
				if (name.indexOf("AP ") >= 0)
					crom = 0;
			}

			tel[i] = new TelescopeElement(name, (int) (diam * fr), diam, (int) (diam * obst / 100.0),
					(int) (spid * diam / 100.0), crom, ocular);
		}
		return tel;
	}

	/**
	 * Return certain telescope.
	 *
	 * @param telescope_name Name of the telescope;
	 * @return The required Telescope object, or null if none is found.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public static TelescopeElement getTelescope(String telescope_name) throws JPARSECException
	{
		TelescopeElement telescopes[] = TelescopeElement.getAllAvailableTelescopes();
		TelescopeElement scope = null;

		int what = -1;
		for (int i = 0; i < telescopes.length; i++)
		{
			if (telescopes[i].name.indexOf(telescope_name) >= 0)
				what = i;
		}
		if (what >= 0)
			scope = telescopes[what];

		return scope;
	}

	/**
	 * Returns the angular field of view diameter at prime focus.
	 *
	 * @return The angular field in radians.
	 */
	public double getPrimaryFocusField()
	{
		double reticle = 16;
		if (ocular != null) reticle = ocular.reticleSize * 0.5;
		return 2.0 * Math.atan2(reticle / (double) focalLength, 1.0);
	}

	/**
	 * Returns the angular field of view diameter with the current ocular.
	 * In case a camera is attached (and ocular is null) the returned
	 * value will be the field of view of the camera in the x axis.
	 * For both null camera and ocular the primary focus field is returned.
	 *
	 * @return The angular field in radians.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getField()
	throws JPARSECException {
		if (ocular == null && ccd == null) return getPrimaryFocusField();
		if (ccd != null && ocular == null) return ccd.getFieldX(this);
		double field = ocular.fieldOfView / this.getMagnification();
		return field;
	}

	/**
	 * Returns the ocular focal length to obtain certain field of view in a
	 * given telescope. The ocular object is required
	 *
	 * @param field Desired field of view in radians.
	 * @param telescope Telescope.
	 * @return The adequate ocular focal length in mm, or 0 if the telescope
	 * has no ocular defined.
	 */
	public static float getOcularFocalLengthForCertainField(double field,
			TelescopeElement telescope)
	{
		if (telescope.ocular == null) return 0;

		double mag = telescope.ocular.fieldOfView / field;

		float fl = (float) (telescope.focalLength / mag);

		return fl;
	}

	/**
	 * Obtains the magnification of the telescope using the current ocular.
	 *
	 * @return Magnification factor.
	 * @throws JPARSECException If an error occurs, for instance for a null ocular.
	 */
	public double getMagnification()
	throws JPARSECException {
		return (double) focalLength / (double) ocular.focalLength;
	}

	/**
	 * Obtains the output size of the pupil using the current ocular.
	 *
	 * @return Magnification factor.
	 * @throws JPARSECException If an error occurs, for instance for a null ocular.
	 */
	public double getPupil()
	throws JPARSECException {
		double pupil = (double) diameter / getMagnification();
		return Math.abs(pupil);
	}

	/**
	 * Obtains the focal ratio of the telescope.
	 *
	 * @return Focal ratio.
	 */
	public double getFocalRatio()
	{
		return (double) focalLength / (double) diameter;
	}

	/**
	 * To clone the object.
	 */
	@Override
	public TelescopeElement clone()
	{
		TelescopeElement tel = null;
		if (ocular == null) {
			tel = new TelescopeElement(this.name, this.focalLength, this.diameter,
					this.centralObstruction, this.spidersSize, this.cromatismLevel, null);
		} else {
			tel = new TelescopeElement(this.name, this.focalLength, this.diameter,
				this.centralObstruction, this.spidersSize, this.cromatismLevel, this.ocular.clone());
		}
		if (ccd != null) tel.ccd = this.ccd.clone();
		tel.lastOcular = null;
		if (this.lastOcular != null) tel.lastOcular = this.lastOcular.clone();
		tel.invertHorizontal = this.invertHorizontal;
		tel.invertVertical = this.invertVertical;
		return tel;
	}

	/**
	 * Returns if a given telescope is equals to another.
	 * @param telescope A telescope object.
	 * @return True or false.
	 */
	@Override
	public boolean equals(Object telescope) {
		if (this == telescope) return true;

		if (!(telescope instanceof TelescopeElement)) return false;

		TelescopeElement that = (TelescopeElement) telescope;

		if (focalLength != that.focalLength) return false;
		if (diameter != that.diameter) return false;
		if (centralObstruction != that.centralObstruction) return false;
		if (spidersSize != that.spidersSize) return false;
		if (Float.compare(that.cromatismLevel, cromatismLevel) != 0) return false;
		if (invertHorizontal != that.invertHorizontal) return false;
		if (invertVertical != that.invertVertical) return false;
		if (name != null ? !name.equals(that.name) : that.name != null) return false;
		if (ocular != null ? !ocular.equals(that.ocular) : that.ocular != null) return false;
		if (lastOcular != null ? !lastOcular.equals(that.lastOcular) : that.lastOcular != null) return false;

		return !(ccd != null ? !ccd.equals(that.ccd) : that.ccd != null);
	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + focalLength;
		result = 31 * result + diameter;
		result = 31 * result + centralObstruction;
		result = 31 * result + spidersSize;
		result = 31 * result + (ocular != null ? ocular.hashCode() : 0);
		result = 31 * result + (lastOcular != null ? lastOcular.hashCode() : 0);
		result = 31 * result + (ccd != null ? ccd.hashCode() : 0);
		result = 31 * result + (cromatismLevel != +0.0f ? Float.floatToIntBits(cromatismLevel) : 0);
		result = 31 * result + (invertHorizontal ? 1 : 0);
		result = 31 * result + (invertVertical ? 1 : 0);
		return result;
	}

	/**
	 * Returns the size of an object at film plane in primary focus mode. This
	 * calculation is done for old 35 mm films, for digital cameras you have to
	 * correct by multiplying by sensor size (mm) and dividing by 35.
	 * @param angRadius Angular radius of the object in radians.
	 * @return Size in mm.
	 */
	public double getObjectSizeAtFilmPlane(double angRadius) {
		return 2.0 * this.focalLength * Math.tan(angRadius);
	}
	/**
	 * Returns the size of an object at film plane with a given Barlow lens.  This
	 * calculation is done for old 35 mm films, for digital cameras you have to
	 * correct by multiplying by sensor size (mm) and dividing by 35.
	 * @param angRadius Angular radius of the object in radians.
	 * @param barlowX Amplification factor of the Barlow lens used, 2
	 * for 2x, and so on.
	 * @return Size in mm.
	 */
	public double getObjectSizeAtFilmPlane(double angRadius, float barlowX) {
		return 2.0 * this.focalLength * Math.tan(angRadius) * barlowX * Math.sqrt(barlowX);
	}
	/**
	 * Returns the size of an object at film plane in eyepiece projection mode.
	 * @param angRadius Angular radius of the object in radians.
	 * @param distToFocalPlane Distance to focal plane in mm.
	 * @return Size in mm.
	 */
	public double getObjectSizeAtFilmPlane(double angRadius, double distToFocalPlane) {
		double m = (distToFocalPlane / this.ocular.focalLength) - 1.0;
		double e = ((double) this.focalLength / (double) this.diameter) * m;
		return 2.0 * e * this.diameter * Math.tan(angRadius);
	}
	/**
	 * Returns the approximate limiting magnitude of the telescope.
	 * Returned value is for optical observations even in case a CCD camera is attached.
	 * @return Limiting magnitude.
	 */
	public double getLimitingMagnitude() { //throws JPARSECException {
		//if (ocular == null && ccd != null) return getCCDLimitingMagnitude(10, 19, 7);
		return 8.8 + (5.0 * (Math.log((double) this.diameter / 25.4) * .4342945));
	}
	/**
	 * Returns the approximate limiting magnitude of the telescope with a CCD
	 * camera attached. Based on the original program in BASIC by Bradley E.
	 * Schaefer in "Limiting Magnitudes for CCDs", Sky and Telescope page 117
	 * of the May 1998 issue.
	 * @param s Exposure time in seconds.
	 * @param sb Sky brightness in mag/s^2. A good value is 19. Typical values are
	 * between 16 and 24.
	 * @param rn Readout noise in e- per pixel. Typical values for good CCDs and DLSR
	 * cameras are around 7.
	 * @return Limiting magnitude, defined as the magnitude for which the signal
	 * to noise ratio will be around 3 for the input exposure time.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getCCDLimitingMagnitude(double s,
			double sb, double rn) throws JPARSECException {
		//double sb = 19; // sky brightness in mag/arcsec^2, from 16 to 24
		double ps = ccd.getScaleX(this) * Constant.RAD_TO_ARCSEC;
		double dd = 5; // detection diameter in arcsec
		//double rn = 7; // readout noise, e-/pixel
		double th = 0; // thermal noise e-/arcsec (perfect cooling)
		double ap = this.diameter * 0.1; // cm
		double et = 30; // time since last shot in seconds
		double so = 2000000;
		double ar = Math.PI * FastMath.pow(dd * 0.5, 2.0);
		double px = ar / (ps * ps);
		if (px < 1 && th > 0.0) throw new JPARSECException("detection area "+(float)px+" is lower than 1\". Increase it or decrease pixel size");
		double en = Math.min(dd * 0.1, 1.0);
		double m = this.getLimitingMagnitude(), lastm = m;;
		double sn = 100;
		do {
			lastm = m;
			if (sn != 100.0) {
				if (sn > 10) {
					m += 1.0;
				} else {
					if (sn > 10) {
						m += 0.1;
					} else {
						if (sn < 4) {
							m += 0.001;
						} else {
							m += 0.01;
						}
					}
				}
			}

			double l = 1.0 / Math.pow(10.0, m / 2.5);
			double sg = l * s * so * en * ap * ap;
			double sk = s * so * ar * ap * ap / Math.pow(10.0, sb / 2.5);
			double tt = th * px * s;
			int n = (int) (s / et + 1.0);
			sn = sg / Math.sqrt(sg + tt + sk + n * px * rn * rn);
		} while (sn > 3);
		return m - (m - lastm) * 0.5;
	}

	/**
	 * Returns the theoretical resolution of the telescope in optical
	 * wavelengths. In case a CCD camera is attached the resolution per
	 * pixel is returned.
	 * @return Resolution in radians.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getResolution() throws JPARSECException {
		if (ocular == null && ccd != null) return ccd.getScaleX(this);
		return 4.56 * 25.4 * Constant.ARCSEC_TO_RAD / (double) this.diameter;
	}

	/**
	 * Returns the theoretical resolution of the telescope for a given wavelength,
	 * using the Dawes limit. Returned value is for visual observations, not
	 * affected in case a camera is attached.
	 * @param lambda wavelength in microns. For optical wavelengths you can use
	 * 0.55 microns, resulting in slightly lower resolution compared to
	 * {@linkplain #getResolution()}.
	 * @return Resolution in radians, set as 1.22 * wavelength / diameter.
	 */
	public double getResolution(double lambda) {
		return 1.22 * lambda * 1.0E-3 / (double) this.diameter;
	}

	/**
	 * Returns the theoretical resolution of the telescope for a given wavelength,
	 * using the Dawes limit. Returned value is for visual observations, not
	 * affected in case a camera is attached.
	 * @param lambda wavelength in microns. For optical wavelengths you can use
	 * 0.55 microns, resulting in slightly lower resolution compared to
	 * {@linkplain #getResolution()}.
	 * @param diameter Diameter of the telescope in mm.
	 * @return Resolution in radians, set as 1.22 * wavelength / diameter.
	 */
	public static double getResolution(double lambda, double diameter) {
		return 1.22 * lambda * 1.0E-3 / diameter;
	}

	/**
	 * Attaches a given CCD camera to this telescope. The ocular
	 * is set to null to allow the camera to operate in this instance,
	 * unless the input CCD camera itself is null. In the latter case
	 * the previous ocular is recovered.
	 * @param ccd The CCD camera to attach, or null to detach any previous
	 * CCD camera.
	 */
	public void attachCCDCamera(CCDElement ccd) {
		this.ccd = ccd;
		if (ccd != null) {
			if (ocular != null) lastOcular = ocular.clone();
			ocular = null;
		} else {
			ocular = lastOcular;
		}
	}
}
