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

import static java.lang.Double.NaN;
import static java.lang.Double.isNaN;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toRadians;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import jparsec.astronomy.Astrometry;
import jparsec.astronomy.CoordinateSystem.COORDINATE_SYSTEM;
import jparsec.graph.DataSet;
import jparsec.math.Constant;
import jparsec.observer.LocationElement;
import jparsec.util.JPARSECException;
import jsky.coords.WCSTransform;
import skyview.geometry.CoordinateSystem;
import skyview.geometry.Projection;
import skyview.geometry.Rotater;
import skyview.geometry.Scaler;
import skyview.geometry.TransformationException;

/**
* The World Coordinates System (WCS) describes the coordinates of the
* image. It makes it possible to convert image coordinates to sky coordinates
* and vice versa. This class provides methods for such transformations using
* the SkyView and JSky implementations. SkyView is the default method, with
* the advantage of supporting image distortion poynomials. The JSky implementation
* supports a wider variety of projections, but not distortions, and is used
* instead of SkyView in case the projection is not supported there. The WCS
* consists of the following parameters:
* <ul>
*  <li>crval1 : Right ascension of the center in degrees.</li>
*  <li>crval2 : Declination of the center in degrees.</li>
*  <li>crpix1 : Reference pixel X coordinate.</li>
*  <li>crpix2 : Reference pixel Y coordinate.</li>
*  <li>cdelt1 : Number of degrees per pixel along x-axis.</li>
*  <li>cdelt2 : Number of degrees per pixel along y-axis.</li>
*  <li>Optional image distortion parameters: CD1_1, CD1_2, CD2_1, CD2_2, or PC1_1, PC1_2, PC2_1, PC2_2.</li>
*  <li>ctype1, ctype2 : Projection type name with FITS convention.
*  The first 4 characters are:
*   <ul><li>RA-- and DEC-&nbsp;&nbsp;&nbsp;for equatorial coordinates.</li>
*       <li>GLON and GLAT&nbsp;&nbsp;&nbsp;for galactic coordinates.</li>
* 	 <li>ELON and ELAT&nbsp;&nbsp;&nbsp;for ecliptic coordinates.</li>
*   </ul>
*   The next 4 characters describe the projection. Possibilities are:
*   <ul>
*    <li>-AZP: Zenithal (Azimuthal) Perspective.</li>
*    <li>-SZP: Slant Zenithal Perspective. </li>
*    <li>-TAN: Gnomonic = Tangent Plane. </li>
*    <li>-SIN: Orthographic/synthesis. </li>
*    <li>-STG: Stereographic. </li>
*    <li>-ARC: Zenithal/azimuthal equidistant. </li>
*    <li>-ZPN: Zenithal/azimuthal PolyNomial. </li>
*    <li>-ZEA: Zenithal/azimuthal Equal Area. </li>
*    <li>-AIR: Airy. </li>
*    <li>-CYP: CYlindrical Perspective. </li>
*    <li>-CAR: Cartesian. </li>
*    <li>-MER: Mercator. </li>
*    <li>-COP: COnic Perspective. </li>
*    <li>-COD: COnic equiDistant. </li>
*    <li>-COE: COnic Equal area. </li>
*    <li>-COO: COnic Orthomorphic. </li>
*    <li>-BON: Bonne. </li>
*    <li>-PCO: Polyconic. </li>
*    <li>-SFL: Sanson-Flamsteed. </li>
*    <li>-PAR: Parabolic. </li>
*    <li>-AIT: Hammer-Aitoff equal area all-sky. </li>
*    <li>-MOL: Mollweide. </li>
*    <li>-CSC: COBE quadrilateralized Spherical Cube. </li>
*    <li>-QSC: Quadrilateralized Spherical Cube. </li>
*    <li>-TSC: Tangential Spherical Cube. </li>
*    <li>-NCP: North celestial pole (special case of SIN). </li>
*    <li>-GLS: GLobal Sinusoidal (Similar to SFL). </li>
*   </ul>
*  <li>equinox : Equinox of coordinates, 1950 and 2000 supported.</li>
*  <li>epoch : Epoch of coordinates, used for FK4/FK5 conversion no effect if 0.</li>
*  <li>rotate : Rotation angle (clockwise positive) in degrees.</li>
*  </ul>
*
* Units for the parameters are degrees and pixels, measured from top-left corner (0, 0)
* as usual. In addition, it is necessary to know the width and height of the image.
* Some projections only works correctly (direct transformations giving the same
* unique results as the input for the inverse one) if the field of view is little enough,
* specially in the x axis, since the parameters used by WCS, provided in the .fits
* files and described above, can only be strictly applied to linear transformations.
*
* In case your image has a wide field of view and this is a limitation you can consider
* using {@linkplain Astrometry} class instead.
*
* @author T. Alonso Albi - OAN (Spain)
* @version 1.0
* @see Astrometry
*/
public class WCS implements Serializable
{
	/**
	 * This flags selects if SkyView implementation of WCS should be used (true), or if
	 * the JSky implementation should be used instead (false). Default value is true, but
	 * will be changed to false in case a given projection is not supported in SkyView.
	 */
	public transient boolean useSkyViewImplementation = true;

	  private double crval1;
	  private double crval2;
	  private double crpix1;
	  private double crpix2;
	  private double cdelt1;
	  private double cdelt2;
	  private String ctype1;
	  private String ctype2;
	  private long equinox;
	  private double epoch;
	  private double crota2;
	  private int width;
	  private int height;
	  private transient double pc[] = null;
	  private transient double cd[] = null;
	  private transient skyview.geometry.WCS wcs;
	  private transient double lonpole = NaN;

	  /** Version id for serialization.  */
	  private static final long serialVersionUID = 1L;

	  /**
	   * The set of possible projections.
	   */
	  public enum PROJECTION {
		  /** ID code for Zenithal (Azimuthal) Perspective. */
		  AZP,
		  /** Slant Zenithal Perspective. */
		  SZP,
		  /** Gnomonic = Tangent Plane. */
		  TAN,
		  /** Orthographic/synthesis. */
		  SIN,
		  /** Stereographic. */
		  STG,
		  /** Zenithal/azimuthal equidistant. */
		  ARC,
		  /** Zenithal/azimuthal PolyNomial. */
		  ZPN,
		  /** Zenithal/azimuthal Equal Area. */
		  ZEA,
		  /** Airy. */
		  AIR,
		  /** CYlindrical Perspective. */
		  CYP,
		  /** Cartesian. */
		  CAR,
		  /** Mercator. */
		  MER,
		  /** Cylindrical Equal Area. */
		  CEA,
		  /** COnic Perspective. */
		  COP,
		  /** COnic equiDistant. */
		  COD,
		  /** COnic Equal area. */
		  COE,
		  /** COnic Orthomorphic. */
		  COO,
		  /** Bonne. */
		  BON,
		  /** Polyconic. */
		  PCO,
		  /** Sanson-Flamsteed. */
		  SFL,
		  /** Parabolic. */
		  PAR,
		  /** Hammer-Aitoff equal area all-sky. */
		  AIT,
		  /** Mollweide. */
		  MOL,
		  /** COBE quadrilateralized Spherical Cube. */
		  CSC,
		  /** Quadrilateralized Spherical Cube. */
		  QSC,
		  /** Tangential Spherical Cube. */
		  TSC,
		  /** North celestial pole (special case of SIN). */
		  NCP,
		  /** GLobal Sinusoidal (Similar to SFL). */
		  GLS
	  };

	  /**
	   * The default constructor for the World Coordinate System sets the
	   * following standard values:
	   * <ul>
	   *  <li>crval1 = 0.0.</li>
	   *  <li>crval2 = 0.0.</li>
	   *  <li>cdelt1= 0.000555556.</li>
	   *  <li>cdelt2 = 0.000555556.</li>
	   *  <li>crpix1 = 0.0.</li>
	   *  <li>crpix2 = 0.0.</li>
	   *  <li>crota2 = 0.0.</li>
	   *  <li>equinox = 2000.</li>
	   *  <li>epoch = 2000.0.</li>
	   *  <li>proj = TAN (equatorial coordinates).</li>
	   *  <li>width = 0.</li>
	   *  <li>height = 0.</li>
	   * </ul>
	   */
	  public WCS()
	  {
	    crval1 = 0.0;
	    crval2 = 0.0;
	    crpix1 = 0.0;
	    crpix2 = 0.0;
	    cdelt1 = 0.000555556;
	    cdelt2 = 0.000555556;
	    ctype1 = "RA---TAN";
	    ctype2 = "DEC--TAN";
	    equinox = 2000;
	    epoch = 2000.0;
	    crota2 = 0.0;
	    width = 0;
	    height = 0;
	  }

	  /**
	   * Constructs a custom WCS given some parameters for the corresponding image.
	   * Output WCS is referred to equinox 2000, and projection is TAN.
	   * @param loc The equatorial position of the center of the image (J2000).
	   * @param width Width of the image in pixels.
	   * @param height Height of the image in pixels.
	   * @param field Field of the image in radians.
	   * @param eastLeft True to show East direction towards left, false to show East
	   * rightwards.
	   * @param northUp True to show North upwards, false to show it downwards.
	   * @param epoch Epoch for the coordinates as a year with decimals, for instance 2000.0.
	   */
	  public WCS(LocationElement loc, int width, int height, double field, boolean eastLeft, boolean northUp,
			  double epoch) {
		    crval1 = loc.getLongitude() * Constant.RAD_TO_DEG;
		    crval2 = loc.getLatitude() * Constant.RAD_TO_DEG;
		    crpix1 = width / 2.0 + 0.5;
		    crpix2 = height / 2.0 + 0.5;
		    double degPerPixel = Math.abs(field * Constant.RAD_TO_DEG / width);
		    cdelt1 = degPerPixel;
		    cdelt2 = degPerPixel;
		    if (eastLeft) cdelt1 = -cdelt1;
		    if (northUp) cdelt2 = -cdelt2;
		    ctype1 = "RA---TAN";
		    ctype2 = "DEC--TAN";
		    equinox = 2000;
		    this.epoch = epoch;
		    crota2 = 0.0;
		    this.width = width;
		    this.height = height;
	  }

	  /**
	   * Copies this instance.
	   * @return The copy.
	   */
	  @Override
	  public WCS clone()
	  {
		  WCS w = new WCS();
		  w.cdelt1 = getCdelt1();
		  w.cdelt2 = getCdelt2();
		  w.crota2 = getCrota2();
		  w.crpix1 = getCrpix1();
		  w.crpix2 = getCrpix2();
		  w.crval1 = getCrval1();
		  w.crval2 = getCrval2();
		  w.ctype1 = getCtype1();
		  w.ctype2 = getCtype2();
		  w.epoch = getEpoch();
		  w.equinox = getEquinox();
		  w.width = getWidth();
		  w.height = getHeight();
		  return w;
	  }

	  /**
	   * Imports a fits file from the given filename.
	   * Key values that should be available in the header include
	   * NAXIS1, NAXIS2, CRVAL1, CRVAL2, CRPIX1, CRPIX2, CDELT1, CDELT2,
	   * CTYPE1, CTYPE2, EQUINOX, EPOCH, and optionally CROTA1, CROTA2.
	   * In case any of them is not available it will be initialized to
	   * a default value, no error will be launched.
	   * @param filename The name of the fits file.
	   * @throws JPARSECException Throws an exception if the FITS file could
	   *                                not be read.
	   */
	  public WCS(String filename) throws JPARSECException
	  {
		    FitsIO fio = new FitsIO(filename);
		    HeaderElement header[] = fio.getHeader(0);

		    init(header);
	  }

	  /**
	   * Imports a fits file from the given fits header.
	   * Key values that should be available in the header include
	   * NAXIS1, NAXIS2, CRVAL1, CRVAL2, CRPIX1, CRPIX2, CDELT1, CDELT2,
	   * CTYPE1, CTYPE2, EQUINOX, EPOCH, and optionally CROTA1, CROTA2.
	   * In case any of them is not available it will be initialized to
	   * a default value, no error will be launched.
	   * @param header The header of the fits file.
	   * @throws JPARSECException Throws an exception if the FITS header could not be read.
	   */
	  public WCS(HeaderElement header[]) throws JPARSECException
	  {
		  init(header);
	  }

	  private void init(HeaderElement header[]) throws JPARSECException {
		    // Reading out the WCS information from the fits header
		    this.setWidth(this.getKeywordValue(header, "NAXIS1", 0));
		    this.setHeight(this.getKeywordValue(header, "NAXIS2", 0));
		    this.setCrval1(this.getKeywordValue(header, "CRVAL1", 0.0));
		    this.setCrval2(this.getKeywordValue(header, "CRVAL2", 0.0));
		    this.setCrpix1(this.getKeywordValue(header, "CRPIX1", 0.0));
		    this.setCrpix2(this.getKeywordValue(header, "CRPIX2", 0.0));
		    this.setCdelt1(this.getKeywordValue(header, "CDELT1", 0.0));
		    this.setCdelt2(this.getKeywordValue(header, "CDELT2", 0.0));
		    this.setCtype1(this.getKeywordValue(header, "CTYPE1", "RA---TAN"));
		    this.setCtype2(this.getKeywordValue(header, "CTYPE2", "DEC--TAN"));
		    this.setEquinox(this.getKeywordValue(header, "EQUINOX", 2000));
		    this.setEpoch(this.getKeywordValue(header, "EPOCH", 2000.0));

		    if (HeaderElement.getByKey(header, "CROTA2") != null) {
		    	this.setCrota2(this.getKeywordValue(header, "CROTA2", 0.0));
		    } else {
		    	this.setCrota2(this.getKeywordValue(header, "CROTA1", 0.0));
		    }

		    if (HeaderElement.getByKey(header, "PC1_1") != null) {
		    	pc = new double[] {
		    			getKeywordValue(header, "PC1_1", NaN),
		    			getKeywordValue(header, "PC1_2", NaN),
		    			getKeywordValue(header, "PC2_1", NaN),
		    			getKeywordValue(header, "PC2_2", NaN)
		    	};
		    }
		    if (HeaderElement.getByKey(header, "CD1_1") != null) {
		    	cd = new double[] {
		    			getKeywordValue(header, "CD1_1", NaN),
		    			getKeywordValue(header, "CD1_2", NaN),
		    			getKeywordValue(header, "CD2_1", NaN),
		    			getKeywordValue(header, "CD2_2", NaN)
		    	};
		    }
		    if (HeaderElement.getByKey(header, "LONPOLE") != null) lonpole = getKeywordValue(header, "LONPOLE", NaN);
	  }

	  /**
	   * Returns the WCS instance as a header for a fits file.
	   * @return The header object with the required fields for the WCS.
	   */
	  public HeaderElement[] getAsHeader() {
			HeaderElement out[] = new HeaderElement[] {
					new HeaderElement("NAXIS1", ""+width, ""),
					new HeaderElement("NAXIS2", ""+height, ""),
					new HeaderElement("CRVAL1", ""+crval1, ""),
					new HeaderElement("CRVAL2", ""+crval2, ""),
					new HeaderElement("CRPIX1", ""+crpix1, ""),
					new HeaderElement("CRPIX2", ""+crpix2, ""),
					new HeaderElement("CDELT1", ""+cdelt1, ""),
					new HeaderElement("CDELT2", ""+cdelt2, ""),
					new HeaderElement("CTYPE1", ""+ctype1, ""),
					new HeaderElement("CTYPE2", ""+ctype2, ""),
					new HeaderElement("EQUINOX", ""+equinox, ""),
					new HeaderElement("EPOCH", ""+epoch, ""),
					new HeaderElement("CROTA1", ""+crota2, ""),
					new HeaderElement("CROTA2", ""+crota2, "")
			};
			if (pc != null) {
				out = HeaderElement.addHeaderEntry(out, new HeaderElement[] {
						new HeaderElement("PC1_1", ""+pc[0], ""),
						new HeaderElement("PC1_2", ""+pc[1], ""),
						new HeaderElement("PC2_1", ""+pc[2], ""),
						new HeaderElement("PC2_2", ""+pc[3], "")
				});
			}
			if (cd != null) {
				out = HeaderElement.addHeaderEntry(out, new HeaderElement[] {
						new HeaderElement("CD1_1", ""+cd[0], ""),
						new HeaderElement("CD1_2", ""+cd[1], ""),
						new HeaderElement("CD2_1", ""+cd[2], ""),
						new HeaderElement("CD2_2", ""+cd[3], "")
				});
			}
			if (lonpole != NaN) out = HeaderElement.addHeaderEntry(out, new HeaderElement("LONPOLE", ""+lonpole, ""));
			return out;
	  }

	  private double getKeywordValue(HeaderElement header[], String key, double value) throws JPARSECException
	  {
		  HeaderElement h = HeaderElement.getByKey(header, key);
		  if (h == null) return value;
		  return h.getAsDouble();
	  }
	  private int getKeywordValue(HeaderElement header[], String key, int value) throws JPARSECException
	  {
		  HeaderElement h = HeaderElement.getByKey(header, key);
		  if (h == null) return value;
		  return (int) h.getAsDouble();
	  }
	  private String getKeywordValue(HeaderElement header[], String key, String value)
	  {
		  HeaderElement h = HeaderElement.getByKey(header, key);
		  if (h == null) return value;
		  return h.getAsString();
	  }

	  /**
	   * Sets the projection type name of the x coordinate.
	   *
	   * @param ctype1 The projection type name of the x coordinate (RA--, GLON
	   * 					or ELON).
	   * @return True if the value of the keyword is changed, otherwise false
	   * (if the length of the provided string is not 8).
	   */
	  private boolean setCtype1(String ctype1)
	  {
		  if (ctype1.endsWith("-SIP")) ctype1 = ctype1.substring(0, ctype1.length()-4);
		  if (ctype1.length() != 8) {
			  return false;
		  } else {
			  this.ctype1 = ctype1;
			  wcs = null;
			  return true;
		  }
	  }


	  /**
	   * Sets the projection type name of the y coordinate.
	   *
	   * @param ctype2 The projection type name of the y coordinate (DEC-, GLAT
	   * 					or ELAT).
	   * @return True if the value of the keyword is changed, otherwise false
	   * (if the length of the provided string is not 8).
	   */
	  private boolean setCtype2(String ctype2)
	  {
		  if (ctype2.endsWith("-SIP")) ctype2 = ctype2.substring(0, ctype2.length()-4);
		  if (ctype2.length() != 8) {
			  return false;
		  } else {
			  this.ctype2 = ctype2;
			  wcs = null;
			  return true;
		  }
	  }


	  /**
	   * Returns the projection type name of the x coordinate.
	   *
	   * @return A String describing the projection type name of the x coordinate.
	   */
	  private String getCtype1()
	  {
	    return this.ctype1;
	  }


	  /**
	   * Returns the projection type name of the y coordinate.
	   *
	   * @return A String describing the projection type name of the y coordinate.
	   */
	  private String getCtype2()
	  {
	    return this.ctype2;
	  }


	  /**
	   * Sets the center coordinate as right ascension or longitude.
	   *
	   * @param crval1 The center right ascension or longitude (in degrees).
	   */
	  public void setCrval1(double crval1)
	  {
	    this.crval1 = crval1;
	    wcs = null;
	  }


	  /**
	   * Returns the center right ascension.
	   *
	   * @return The center right ascension (in degrees).
	   */
	  public double getCrval1()
	  {
	    return this.crval1;
	  }


	  /**
	   * Sets the center coordinate as declination or latitude.
	   *
	   * @param crval2 The center declination or latitude (in degrees).
	   */
	  public void setCrval2(double crval2)
	  {
	    this.crval2 = crval2;
	    wcs = null;
	  }


	  /**
	   * Returns the center coordinate as declination or latitude.
	   *
	   * @return The center declination or latitude (in degrees).
	   */
	  public double getCrval2()
	  {
	    return this.crval2;
	  }


	  /**
	   * Sets the center coordinate in pixel coordinates.
	   *
	   * @param crpix1 The center coordinate (X) in pixel coordinates.
	   */
	  public void setCrpix1(double crpix1)
	  {
	    this.crpix1 = crpix1;
	    wcs = null;
	  }


	  /**
	   * Returns the pixel coordinates of the reference point (X) to
	   * which the projection and the rotation refer.
	   *
	   * @return The pixel coordinates of the reference point (X) to which the
	   * projection and the rotation refer.
	   */
	  public double getCrpix1()
	  {
	    return this.crpix1;
	  }


	  /**
	   * Sets the center coordinate in pixel coordinates.
	   *
	   * @param crpix2 The center coordinate (Y) in pixel coordinates.
	   */
	  public void setCrpix2(double crpix2)
	  {
	    this.crpix2 = crpix2;
	    wcs = null;
	  }


	  /**
	   * Returns the pixel coordinates of the reference point (Y) to
	   * which the projection and the rotation refer.
	   *
	   * @return The pixel coordinates of the reference point (Y) to which the
	   * projection and the rotation refer.
	   */
	  public double getCrpix2()
	  {
	    return this.crpix2;
	  }


	  /**
	   * Sets the plate scale in degrees per pixel along the x-axis.
	   *
	   * @param cdelt1 The plate scale in degrees per pixel along the x-axis.
	   */
	  public void setCdelt1(double cdelt1)
	  {
	    this.cdelt1 = cdelt1;
	    wcs = null;
	  }


	  /**
	   * Returns the plate scale in degrees per pixel along the x-axis.
	   *
	   * @return The plate scale in degrees per pixel along the x-axis.
	   */
	  public double getCdelt1()
	  {
	    return this.cdelt1;
	  }


	  /**
	   * Sets the plate scale in degrees per pixel along the y-axis.
	   *
	   * @param cdelt2 The plate scale in degrees per pixel along the y-axis.
	   */
	  public void setCdelt2(double cdelt2)
	  {
	    this.cdelt2 = cdelt2;
	    wcs = null;
	  }


	  /**
	   * Returns the plate scale in degrees per pixel along the y-axis.
	   *
	   * @return The plate scale in degrees per pixel along the y-axis.
	   */
	  public double getCdelt2()
	  {
	    return this.cdelt2;
	  }


	  /**
	   * Sets the rotation of the horizontal and vertical axes in
	   * degrees.
	   *
	   * @param crota2 The rotation of the horizontal and vertical axes in degrees.
	   */
	  public void setCrota2(double crota2)
	  {
	    this.crota2 = crota2;
	    wcs = null;
	  }


	  /**
	   * Returns the rotation of the horizontal and vertical axes in
	   * degrees.
	   *
	   * @return The rotation of the horizontal and vertical axes in degrees.
	   */
	  public double getCrota2()
	  {
	    return this.crota2;
	  }


	  /**
	   * Sets the equinox (1950 and 2000 are supported).
	   *
	   * @param equinox The equinox.
	   */
	  public void setEquinox(int equinox)
	  {
	    this.equinox = equinox;
	    wcs = null;
	  }


	  /**
	   * Returns the equinox (1950 and 2000 are supported).
	   *
	   * @return The equinox.
	   */
	  public int getEquinox()
	  {
	    return (int) this.equinox;
	  }


	  /**
	   * Sets the epoch.
	   *
	   * @param epoch The epoch.
	   */
	  public void setEpoch(double epoch)
	  {
	    this.epoch = epoch;
	    wcs = null;
	  }


	  /**
	   * Returns the epoch.
	   *
	   * @return The epoch.
	   */
	  public double getEpoch()
	  {
	    return this.epoch;
	  }

	  /**
	   * Returns the width in pixels.
	   * @return Width.
	   */
	  public int getWidth()
	  {
		  return this.width;
	  }
	  /**
	   * Returns the height in pixels.
	   * @return Height.
	   */
	  public int getHeight()
	  {
		  return this.height;
	  }
	  /**
	   * Sets the width.
	   * @param w Width in pixels.
	   */
	  public void setWidth(int w)
	  {
		  this.width = w;
	  }
	  /**
	   * Sets the height.
	   * @param h Height in pixels.
	   */
	  public void setHeight(int h)
	  {
		  this.height = h;
	  }

	  /**
	   * Returns the CD polynomial with the distortion.
	   * @return CD1_1, CD1_2, CD2_1, and CD2_2 values.
	   * Default value is null.
	   */
	  public double[] getCD() { return cd; }
	  /**
	   * Returns the PC polynomial with the distortion.
	   * @return PC1_1, PC1_2, PC2_1, and PC2_2 values.
	   * Default value is null.
	   */
	  public double[] getPC() { return pc; }
	  /**
	   * Sets the values of the CD polynomial with the distortions.
	   * @param c CD1_1, CD1_2, CD2_1, and CD2_2 values (or null).
	   */
	  public void setCD(double c[]) {
		  cd = c;
		  wcs = null;
	  }
	  /**
	   * Sets the values of the PC polynomial with the distortions.
	   * @param c PC1_1, PC1_2, PC2_1, and PC2_2 values (or null).
	   */
	  public void setPC(double c[]) {
		  pc = c;
		  wcs = null;
	  }

	  /**
	   * Sets the projection type. The last 4 characters of ctype1 and ctype2
	   * will be adapted to reflect the new projection.
	   *
	   * @param projection A String describing the projection.
	   */
	  public void setProjection(PROJECTION projection)
	  {
		  String p = "-" + projection.name();
	      this.ctype1 = this.ctype1.substring(0, 4) + p;
	      this.ctype2 = this.ctype2.substring(0, 4) + p;
	      wcs = null;
	  }

	  /**
	   * Returns a string representing the projection type (the four last
	   * characters of ctype1).
	   *
	   * @return A string representing the projection type.
	   */
	  public PROJECTION getProjection()
	  {
	    String p =  this.getProjectionAsString().substring(1);
	    return PROJECTION.valueOf(p);
	  }

	  /**
	   * Sets the coordinate system.
	   * @param cs The coordinate system. Note horizontal is not supported.
	   * @throws JPARSECException In case the ctype1 or ctype2 fields
	   * are set to an inconsistent value.
	   */
	  public void setCoordinateSystem(COORDINATE_SYSTEM cs) throws JPARSECException
	  {
		  String csys1 = "RA--", csys2 = "DEC-";
		  switch (cs) {
		  case EQUATORIAL:
			  break;
		  case ECLIPTIC:
			  csys1 = "ELON";
			  csys2 = "ELAT";
			  break;
		  case GALACTIC:
			  csys1 = "GLON";
			  csys2 = "GLAT";
		  	break;
		  default:
			  throw new JPARSECException("This coordinate system is not supported.");
		  }
	      this.ctype1 = csys1 + this.ctype1.substring(4);
	      this.ctype2 = csys2 + this.ctype2.substring(4);
	      wcs = null;
	      if (ctype1.length() != 8 || ctype2.length() != 8)
	    	  throw new JPARSECException("Inconsistent values for ctype1 and/or ctype2: "+ctype1+", "+ctype2);
	  }

	  /**
	   * Returns the coordinate system.
	   * @return The coordinate system.
	   * @throws JPARSECException In case the ctype1 or ctype2 fields
	   * are set to an inconsistent value.
	   */
	  public COORDINATE_SYSTEM getCoordinateSystem() throws JPARSECException
	  {
		  String cs1 = ctype1.substring(0, 4);
		  String cs2 = ctype2.substring(0, 4);
		  COORDINATE_SYSTEM out = null;
		  if (cs1.equals("RA--") && cs2.equals("DEC-")) out = COORDINATE_SYSTEM.EQUATORIAL;
		  if (cs1.equals("ELON") && cs2.equals("ELAT")) out = COORDINATE_SYSTEM.ECLIPTIC;
		  if (cs1.equals("GLON") && cs2.equals("GLAT")) out = COORDINATE_SYSTEM.GALACTIC;
		  if (out == null) throw new JPARSECException("Inconsistent values for ctype1 and/or ctype2: "+ctype1+", "+ctype2);
		  return out;
	  }

	  /**
	   * Returns a string representing the projection type (the four last
	   * characters of ctype1).
	   *
	   * @return A string representing the projection type.
	   */
	  public String getProjectionAsString()
	  {
	    return this.ctype1.substring(4, 8);
	  }


	  /**
	   * Returns a string representation of the values of the WCS object.
	   * The format of this string is undefined and subject to change.
	   *
	   * @return A string representation of the values of the WCS object.
	   */
	  @Override
	  public String toString()
	  {
	    StringBuffer toReturn = new StringBuffer(400);
	    toReturn.append("World Coordinate System\n-----------------------\n");
 	    toReturn.append("crval1     : " + this.getCrval1());
	    toReturn.append("\ncrval2     : " + this.getCrval2());
	    toReturn.append("\ncrpix1     : " + this.getCrpix1());
	    toReturn.append("\ncrpix2     : " + this.getCrpix2());
	    toReturn.append("\ncdelt1     : " + this.getCdelt1());
	    toReturn.append("\ncdelt2     : " + this.getCdelt2());
	    toReturn.append("\nctype1     : " + this.getCtype1());
	    toReturn.append("\nctype2     : " + this.getCtype2());
	    toReturn.append("\nequinox    : " + this.getEquinox());
	    toReturn.append("\nepoch      : " + this.getEpoch());
	    toReturn.append("\ncrota2     : " + this.getCrota2());
	    toReturn.append("\nprojection : " + this.getProjection());
	    toReturn.append("\nwidth : " + this.getWidth());
	    toReturn.append("\nheight : " + this.getHeight());
	    if (pc != null) toReturn.append("\npc : " + pc[0]+", "+pc[1]+", "+pc[2]+", "+pc[3]);
	    if (cd != null) toReturn.append("\ncd : " + cd[0]+", "+cd[1]+", "+cd[2]+", "+cd[3]);

	    return toReturn.toString();
	  }

	 /**
	  * Serialize this WCS instance.
	  *
	  * @throws IOException Throws an exception if something goes wrong with the
	  *                      serialization.
	  * @param os The outputstream.
	  */
	 private void writeObject(ObjectOutputStream os) throws IOException
	 {
	   os.defaultWriteObject();
	   os.writeDouble(this.crval1);
	   os.writeDouble(this.crval2);
	   os.writeDouble(this.crpix1);
	   os.writeDouble(this.crpix2);
	   os.writeDouble(this.cdelt1);
	   os.writeDouble(this.cdelt2);
	   if ((pc == null && cd == null) || !useSkyViewImplementation) {
		   os.writeObject(this.ctype1);
		   os.writeObject(this.ctype2);
		   os.writeLong(this.equinox);
		   os.writeDouble(this.epoch);
		   os.writeDouble(this.crota2);
		   os.writeInt(this.width);
		   os.writeInt(this.height);
	   } else {
		   os.writeObject(new String[] {this.ctype1, ctype2});
		   os.writeObject(new Object[] {pc, cd, lonpole});
		   os.writeLong(this.equinox);
		   os.writeDouble(this.epoch);
		   os.writeDouble(this.crota2);
		   os.writeInt(this.width);
		   os.writeInt(this.height);
	   }
	 }

	 private void readObject(ObjectInputStream is) throws IOException, ClassNotFoundException
	 {
	   is.defaultReadObject();
	   this.setCrval1(is.readDouble());
	   this.setCrval2(is.readDouble());
	   this.setCrpix1(is.readDouble());
	   this.setCrpix2(is.readDouble());
	   this.setCdelt1(is.readDouble());
	   this.setCdelt2(is.readDouble());
	   Object obj = is.readObject();
	   // Very nasty, but is there any alternative ?
	   if (obj instanceof String) {
		   this.setCtype1((String)(obj));
		   this.setCtype2((String)(is.readObject()));
		   this.setEquinox((int) is.readLong());
		   this.setEpoch(is.readDouble());
		   this.setCrota2(is.readDouble());
		   this.setWidth(is.readInt());
		   this.setHeight(is.readInt());
	   } else {
		   this.setCtype1(((String[])obj)[0]);
		   this.setCtype2(((String[])obj)[1]);
		   Object[] obj1 = (Object[]) is.readObject();
		   this.setEquinox((int) is.readLong());
		   this.setEpoch(is.readDouble());
		   this.setCrota2(is.readDouble());
		   this.setWidth(is.readInt());
		   this.setHeight(is.readInt());
		   this.setPC((double[])obj1[0]);
		   this.setCD((double[])obj1[1]);
		   this.lonpole = ((Double)obj1[2]);
	   }
	 }

	 private skyview.geometry.WCS getSkyViewWCS() throws JPARSECException, TransformationException {
        CoordinateSystem coords;

        String coordSym = "";
        if (ctype1.startsWith("RA--") && ctype2.startsWith("DEC-")) {
            coordSym = "J" + equinox;
        } else {
            if (ctype1.charAt(0) != ctype2.charAt(0)) {
                throw new JPARSECException("Inconsistent axes definitions:" + ctype1 + "," + ctype2);
            }

            if (ctype1.equals("GLON")) {
                coordSym = "G";
            } else if (ctype1.equals("ELON")) {
                coordSym = "E" + equinox;
            } else if (ctype1.equals("HLON")) {
                coordSym = "H" + equinox;
            }
        }

        coords = CoordinateSystem.factory(coordSym);

        Projection proj = null;
        Scaler ncpScale = null;

        String lonType = ctype1.substring(5);
        String latType = ctype2.substring(5);
        if (!lonType.equals(latType)) {
            throw new JPARSECException("Inconsistent projection in FITS header: " + lonType + "," + latType);
        }

        if (lonType.equals("AIT")) {
            proj = new Projection("Ait");

        } else if (lonType.equals("CAR")) {
            proj = new Projection("Car");
            // Allow non-central latitudes for the Cartesian projection.
            double lon = this.crval1;
            if (lon != 0) {
                proj.setReference(Math.toRadians(lon), 0);
            }

        } else if (lonType.equals("CSC")) {
            proj = new Projection("Csc");

        } else if (lonType.equals("SFL") || lonType.equals("GLS")) {
            proj = new Projection("Sfl");

        } else if (lonType.equals("TOA")) {
            proj = new Projection("Toa");

        } else {

            if (isNaN(crval1 + crval2)) {
                throw new JPARSECException("Unable to find reference coordinates in FITS header");
            }

            if (lonType.equals("TAN") || lonType.equals("SIN") || lonType.equals("ZEA")
                    || lonType.equals("ARC") || lonType.equals("STG") || lonType.equals("ZPN")) {

                String type = lonType.substring(0, 1) + lonType.substring(1, 3).toLowerCase();
                proj = new Projection(type, new double[]{toRadians(crval1), toRadians(crval2)});

                //  ---- Following is probably erroneous -----
                // The WCS standard indicates that the default LONPOLE for
                // a projection is 180 when the CRVAL latitude is less than
                // the native latitude of the projection (90 degrees for the projections
                // handled here) and 0 otherwise.  This means that for a projection
                // around the pole the default lonpole is 0.  Some data (the SFD surveys)
                // seem to require that we do a rotation of 180 degrees to accommodate
                // this.  However we do not implement this unless the LONPOLE is
                // explicitly given since this seems non-intuitive to me and I suspect
                // that a user who is not careful enough to specify a LONPOLE in this
                // situation probably doesn't understand what is going on anyway.
                // ----- We now assume that our standard processing of
                // ----- zenithal projections handles lonpole of 180 and that
                // ----- this is the default for all zenithal images.
                // ----- Previously we assumed that we were using lonPole=0 at
                // ----- at the poles, but we weren't....
                //

                if (!isNaN(lonpole)) {
                    double lonDefault = 180;
                    if (lonpole != lonDefault) {

                        Rotater r = proj.getRotater();

                        Rotater lon = new Rotater("Z", toRadians(lonpole - lonDefault), 0, 0);
                        if (r != null) {
                            proj.setRotater(r.add(lon));
                        } else {
                            proj.setRotater(lon);
                        }
                    }
                }


            } else if (lonType.equals("NCP")) {

                // Sin projection with projection centered at pole.
                double[] xproj = new double[]{toRadians(crval1), Math.PI / 2};
                if (crval2 < 0) {
                    xproj[1] = -xproj[1];
                }


                double poleOffset = sin(xproj[1] - toRadians(crval2));
                // Have we handled South pole here?

                proj = new Projection("Sin", xproj);

                // NCP scales the Y-axis to accommodate the distortion of the SIN projection away
                // from the pole.
                ncpScale = new Scaler(0, poleOffset, 1, 0, 0, 1);
                ncpScale = ncpScale.add(new Scaler(0., 0., 1, 0, 0, 1 / sin(toRadians(crval2))));

            } else {
                throw new TransformationException("Unsupported projection type:" + lonType);
            }
        }


        // There are three ways that scaling information may be provided:
        //    CDELTn, CRPIXn, and CROTAn
        //    CDm_n, CRPIXn
        //    PCm_n, CDELTn, CRPIXn
        // We look for them in this sequence.
        //

        Scaler s = null;
        // Note that in FITS files, the center of the first pixel is
        // assumed to be at coordinates 1,1.  Thus the corner of the image
        // is at pixels coordinates 1/2, 1/2.
        s = extractScaler2(crpix1-0.5, crpix2-0.5);
        if (s != null) {
            return new skyview.geometry.WCS(coords, proj, s);
        }
        s = extractScaler1(crpix1-0.5, crpix2-0.5);
        if (s != null) {
            return new skyview.geometry.WCS(coords, proj, s);
        }

        // No scaling information found.
        throw new JPARSECException("No scaling information found in FITS header");
	 }

	    /**
	     * Get the scaling when CDELT is specified
	     */
	    private Scaler extractScaler1(double crpix1, double crpix2) throws TransformationException {

	    	boolean matrix = false;

	        // We use 1 indexing to match better with the FITS files.
	        double m11, m12, m21, m22;

	        // We've got minimal information...  We might have more.
	        double crota = this.crota2;
	        if (!isNaN(crota) && (crota != 0 || pc == null)) {
	            crota = toRadians(crota);

	            m11 = cos(crota);
	            m12 = sin(crota);
	            m21 = -sin(crota);
	            m22 = cos(crota);
	            matrix = true;

	        } else {

	        	m11 = m12 = m21 = m22 = NaN;
	        	if (pc != null) {
		            m11 = pc[0];
		            m12 = pc[1];
		            m21 = pc[2];
		            m22 = pc[3];
	        	} else {
	        		return null;
	        	}
	        }


	        // Note that Scaler is defined with parameters t = x0 + a00 x + a01 y; u = y0 + a10 x + a11 y
	        // which is different from what we have here...
	        //    t = scalex (x-x0),  u = scaley (y-y0)
	        //    t = scalex x - scalex x0; u = scaley y - scaley y0
	        // or
	        //    t = scalex [a11 (x-x0) + a12 (y-y0)], u = scaley [a21 (x-x0) + a22 (y-y0)] ->
	        //       t = scalex a11 x - scalex a11 x0 + scalex a12 y + scalex a12 y0         ->
	        //       t = - scalex (a11 x0 + a12 y0) + scalex a11 x + scalex a12 y (and similarly for u)

	        Scaler s;
	        double cdelt1 = toRadians(this.cdelt1);
	        double cdelt2 = toRadians(this.cdelt2);
	        if (!matrix) {
	            s = new Scaler(-cdelt1 * crpix1, -cdelt2 * crpix2,
	                    cdelt1, 0, 0, cdelt2);
	        } else {
	            s = new Scaler(-cdelt1 * (m11 * crpix1 + m12 * crpix2), -cdelt2 * (m21 * crpix1 + m22 * crpix2),
	                    cdelt1 * m11, cdelt1 * m12, cdelt2 * m21, cdelt2 * m22);
	        }

	        // Note that this scaler transforms from pixel coordinates to standard projection
	        // plane coordinates.  We want the inverse transformation as the scaler.
	        s = s.inverse();

	        return s;
	    }

	    /**
	     * Get the scaling when it is described as a matrix
	     */
	    private Scaler extractScaler2(double crpix1, double crpix2) throws TransformationException {

	        // Look for the CD matrix...
	        //
        	double m11 = NaN, m12 = NaN, m21 = NaN, m22 = NaN;
        	if (cd != null) {
	            m11 = cd[0];
	            m12 = cd[1];
	            m21 = cd[2];
	            m22 = cd[3];
        	}

	        boolean matrix = !isNaN(m11 + m12 + m21 + m22);
	        if (!matrix) {
	            return null;
	        }

	        m11 = toRadians(m11);
	        m12 = toRadians(m12);
	        m21 = toRadians(m21);
	        m22 = toRadians(m22);

	        // we have
	        //   t = a11 (x-x0) + a12 (y-y0); u = a21(x-x0) + a22(y-y0)
	        //       t = a11x + a12y - a11 x0 - a12 y0;
	        //
	        Scaler s = new Scaler(-m11 * crpix1 - m12 * crpix2, -m21 * crpix1 - m22 * crpix2,
	                m11, m12, m21, m22);

	        s = s.inverse();

	        return s;
	    }

	  /**
	   * Returns the right ascension and the declination of the given pixel. The
	   * row and column should be given as arguments. These values are typically
	   * measured from the top-left corner of the image, being usually 1 the center of
	   * the first pixel in the image (with index 0 in the Java array). Note the WCS
	   * instance alone cannot know which pixel is the first.
	   *
	   * @param p A 2d-point with the column and row of the pixel for which the
	   * sky coordinates should be calculated.
	   * @return A location object describing the right
	   * ascension and the declination of the given pixel.
	   * @throws JPARSECException If an error occurs.
	   */
	  public LocationElement getSkyCoordinates(Point2D p) throws JPARSECException
	  {
		  if (wcs == null && useSkyViewImplementation) {
			  try {
				  wcs = getSkyViewWCS();
			  } catch (Exception exc) {
				  useSkyViewImplementation = false;
				  JPARSECException.addWarning("Using JSky implementation instead of SkyView's one for WCS, since SkyView does not support this projection. Image distortions will not be considered.");
			  }
		  }
		  if (wcs != null) return wcs.getSkyCoordinates(p);

		  // Use JSky without considering distortions
		  WCSTransform wcstrans = new WCSTransform(
				  this.getCrval1(), this.getCrval2(),
				  this.getCdelt1() * 3600.0,
				  this.getCdelt2() * 3600.0,
				  this.getCrpix1(), this.getCrpix2(), width,
				  height, this.getCrota2(),
				  this.getEquinox(), this.getEpoch(),
				  this.getProjectionAsString());

		  // image coords
		  Point2D.Double pd = new Point2D.Double(p.getX(), p.getY());
		  wcstrans.imageToWorldCoords(pd, false);
		  return new LocationElement(pd.getX() * Constant.DEG_TO_RAD, pd.getY() * Constant.DEG_TO_RAD, 1.0);
	  }

	  /**
	   * Returns the pixel coordinates of the given sky coordinates. The pixel
	   * coordinates are the row and the column of the image. These values are typically
	   * measured from the top-left corner of the image, being usually 1 the center of
	   * the first pixel in the image (with index 0 in the Java array). Note the WCS
	   * instance alone cannot know which pixel is the first.
	   *
	   * @param loc The right ascension and declination.
	   * @return A 2-dimensional point describing the pixel
	   * coordinates of the given right ascension and the declination. In some
	   * cases (large fields and some projections) the point could be outside the
	   * image, specially in x axis. In this case the result must be considered
	   * an error due to a limitation of WCS.
	   * @throws JPARSECException If an error occurs.
	   */
	  public Point2D getPixelCoordinates(LocationElement loc) throws JPARSECException
	  {
		  if (wcs == null && useSkyViewImplementation) {
			  try {
				  wcs = getSkyViewWCS();
			  } catch (Exception exc) {
				  useSkyViewImplementation = false;
				  JPARSECException.addWarning("Using JSky implementation instead of SkyView's one for WCS, since SkyView does not support this projection. Image distortions will not be considered.");
			  }
		  }

		  if (wcs != null) return wcs.getPixelCoordinates(loc);

		  // Use JSky without considering distortions
		  WCSTransform wcstrans = new WCSTransform(
				  this.getCrval1(), this.getCrval2(),
				  this.getCdelt1() * 3600.0,
				  this.getCdelt2() * 3600.0,
				  this.getCrpix1(), this.getCrpix2(), width,
				  height, this.getCrota2(),
				  this.getEquinox(), this.getEpoch(),
				  this.getProjectionAsString());

		  Point2D.Double p;

		  // image coords
		  p = new Point2D.Double(loc.getLongitude()*Constant.RAD_TO_DEG,
				  loc.getLatitude()*Constant.RAD_TO_DEG);
		  wcstrans.worldToImageCoords(p, false);

		  return p;
	  }

	  /**
	   * Removes all WCS entries from the input header.
	   * @param header Input header, left unchanged.
	   * @return Same input but with all WCS entries removed.
	   */
	  public static HeaderElement[] removeWCSentries(HeaderElement header[]) {
			return HeaderElement.deleteHeaderEntries(header, DataSet.toStringArray("CRVAL1,CRVAL2,CRPIX1,CRPIX2,CDELT1,CDELT2,CTYPE1,CTYPE2,EQUINOX,EPOCH,CROTA1,CROTA2,PC1_1,PC1_2,PC2_1,PC2_2,CD1_1,CD1_2,CD2_1,CD2_2,LONPOLE", ",", false));
	  }
}
