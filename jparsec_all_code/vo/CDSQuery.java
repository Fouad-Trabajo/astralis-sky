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

import java.io.Serializable;

import cds.astro.Astrocoo;
import cds.astro.Astroframe;
import cds.astro.Ecliptic;
import cds.astro.FK4;
import cds.astro.FK5;
import cds.astro.Galactic;
import cds.astro.ICRS;
import cds.astro.Supergal;
import cds.savot.model.TDSet;
import jparsec.ephem.Functions;
import jparsec.io.FileIO;
import jparsec.math.Constant;
import jparsec.observer.LocationElement;
import jparsec.util.JPARSECException;

/**
 * A class to transform coordinates using CDS web service.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class CDSQuery implements Serializable {
	private static final long serialVersionUID = 1L;

	private FRAME frame1, frame2;
	private PRECISION precision;
	private double equinox1, equinox2;
	private LocationElement loc;
	/**
	 * Constructor for a given query.
	 * @param frame1 Input frame.
	 * @param frame2 Output frame.
	 * @param loc Input coordinates.
	 * @param precision Precision id constant.
	 * @param equinox1 Input equinox.
	 * @param equinox2 Output equinox.
	 */
	public CDSQuery(FRAME frame1, FRAME frame2,
			LocationElement loc, PRECISION precision, double equinox1,
			double equinox2)
	{
		this.frame1 = frame1;
		this.frame2 = frame2;
		this.loc = loc;
		this.precision = precision;
		this.equinox1 = equinox1;
		this.equinox2 = equinox2;
	}
	/**
	 * Perform the query.
	 * @return Response from server.
	 * @throws JPARSECException If an error occurs.
	 */
	public LocationElement query()
	throws JPARSECException {
		LocationElement q = query(frame1, frame2, loc, precision, equinox1, equinox2);
		return q;
	}

	/**
	 * The set of frames for the query.
	 */
	public enum FRAME {
		/** FK4 frame. */
		FK4 (0),
		/** Galactic frame. */
		GAL (1),
		/** Supergalactic frame. */
		SGAL (2),
		/** Ecliptic frame. */
		ECL (3),
		/** FK5 frame. */
		FK5 (4),
		/** ICRF frame. */
		ICRS (5);

		private int index;

		private FRAME(int index) {
			this.index = index;
		}

		/**
		 * Returns the index value for this frame. Used
		 * internally.
		 * @return The index to use in the query.
		 */
		public int getIndex() {
			return index;
		}
	};

	/**
	 * The set of precision values for the query.
	 */
	public enum PRECISION {
		/** None precision. */
		NONE (0),
		/** Degree precision. */
		DEG (1),
		/** Arcminute precision. */
		ARCMIN (3),
		/** Arcsecond precision. */
		ARCSEC (5),
		/** Milliarcsecond precision. */
		MAS (8);

		private int index;

		private PRECISION(int index) {
			this.index = index;
		}

		/**
		 * Returns the index value for this precision. Used
		 * internally.
		 * @return The index to use in the query.
		 */
		public int getIndex() {
			return index;
		}
	};

	/**
	 * Converts coordinates.
	 * @param frame1 Frame for input coordinates.
	 * @param frame2 Frame for output coordinates.
	 * @param loc Input coordinates in radians.
	 * @param precision Precision.
	 * @param equinox1 Input equinox in Julian or Besselian years.
	 * @param equinox2 Output equinox.
	 * @return Conversion.
	 * @throws JPARSECException If an error occurs.
	 */
	public static LocationElement query(FRAME frame1, FRAME frame2,
			LocationElement loc, PRECISION precision, double equinox1,
			double equinox2)
	throws JPARSECException {

		try {
			Astroframe aframe1 = null, aframe2 = null;
			switch (frame1) {
			case ICRS:
				aframe1 = new ICRS(equinox1);
				break;
			case ECL:
				aframe1 = new Ecliptic(equinox1);
				break;
			case FK4:
				aframe1 = new FK4(equinox1);
				break;
			case FK5:
				aframe1 = new FK5(equinox1);
				break;
			case GAL:
				aframe1 = new Galactic();
				break;
			case SGAL:
				aframe1 = new Supergal();
				break;
			}
			switch (frame2) {
			case ICRS:
				aframe2 = new ICRS(equinox2);
				break;
			case ECL:
				aframe2 = new Ecliptic(equinox2);
				break;
			case FK4:
				aframe2 = new FK4(equinox2);
				break;
			case FK5:
				aframe2 = new FK5(equinox2);
				break;
			case GAL:
				aframe2 = new Galactic();
				break;
			case SGAL:
				aframe2 = new Supergal();
				break;
			}

			Astrocoo source = new Astrocoo(aframe1, loc.getLongitude() * Constant.RAD_TO_DEG, loc.getLatitude() * Constant.RAD_TO_DEG, equinox1);
			source.setPrecision(precision.getIndex());
		    source.convertTo(aframe2);

		    LocationElement loc_out = new LocationElement(source.getLon() * Constant.DEG_TO_RAD, source.getLat() * Constant.DEG_TO_RAD, loc.getRadius());

	      return loc_out;

		} catch (Exception e)
		{
			throw new JPARSECException(e);
		}
	}

	/**
	 * Transforms CDS output right ascension to radians.<P>
	 * @param coordinate Input right ascension.
	 * @return Right ascension in radians.
	 */
	public static double parseRAtoRadians(String coordinate)
	{
		String ra = FileIO.getField(1, coordinate, " ", true)+"h ";
		ra += FileIO.getField(2, coordinate, " ", true)+"m ";
		ra += FileIO.getField(3, coordinate, " ", true)+"s";

		return Functions.parseRightAscension(ra);
	}

	/**
	 * Transforms CDS output declination to radians.<P>
	 * @param coordinate Input declination.
	 * @return Declination in radians.
	 */
	public static double parseDECtoRadians(String coordinate)
	{
		String dec = FileIO.getField(1, coordinate, " ", true)+"d ";
		dec += FileIO.getField(2, coordinate, " ", true)+"' ";
		dec += FileIO.getField(3, coordinate, " ", true)+"''";

		return Functions.parseDeclination(dec);
	}

	/**
	 * Transform Vizier coordinates into J2000.
	 * @param viz Vizier object.
	 * @param td {@linkplain TDSet} object with a record in the catalog.
	 * @return J2000 coordinates.
	 * @throws JPARSECException If an error occurs.
	 */
	public static LocationElement transformVizierCoordinatesToJ2000(VizierElement viz, TDSet td)
	throws JPARSECException {
		String ra = "", dec = "";

		int p = viz.getVizierFieldIndex("RAJ2000");
		if (p >= 0) {
			ra = td.getContent(p);
			dec = td.getContent(p+1);
		} else {
			p = viz.getVizierFieldIndex("RA_ICRS");
			if (p >= 0)
			{
				ra = td.getContent(p);
				dec = td.getContent(p+1);
			} else {
				p = viz.getVizierFieldIndex("RA1950");
				if (p >= 0)
				{
					ra = td.getContent(p);
					dec = td.getContent(p+1);
				} else {
					throw new JPARSECException("cannot find coordinates in the catalog.");
				}
			}
		}
		return transformVizierCoordinatesToJ2000(viz, ra, dec);
	}

	/**
	 * Transform Vizier coordinates into J2000 using CDS. In case of error,
	 * transformation will be performed using JPARSEC.
	 * @param viz Vizier object.
	 * @param ra Right ascension as given by Vizier (## ## ##.### or ###.######).
	 * @param dec Declination as given by Vizier (+## ## ##.### or ###.####).
	 * @return J2000 coordinates.
	 * @throws JPARSECException If an error occurs.
	 */
	public static LocationElement transformVizierCoordinatesToJ2000(VizierElement viz, String ra, String dec)
	throws JPARSECException {
		double lon = 0.0, lat = 0.0, rad = 1.0;

		// Parse from degrees or vizier format
		int r = ra.indexOf(" ");
		if (r > 0) {
			lon = CDSQuery.parseRAtoRadians(ra);
		} else {
			r = ra.indexOf(":");
			if (r > 0) {
				ra = ra.replaceFirst(":", "h ");
				ra = ra.replaceFirst(":", "m ");
				ra +="s";
				lon = jparsec.ephem.Functions.parseRightAscension(ra);
			} else {
				lon = Double.parseDouble(ra) * Constant.DEG_TO_RAD;
			}
		}

		r = dec.indexOf(" ");
		if (r > 0) {
			lat = CDSQuery.parseDECtoRadians(dec);
		} else {
			r = dec.indexOf(":");
			if (r > 0) {
				dec = dec.replaceFirst(":", "d ");
				dec = dec.replaceFirst(":", "' ");
				dec +="''";
				lat = jparsec.ephem.Functions.parseDeclination(dec);
			} else {
				lat = Double.parseDouble(dec) * Constant.DEG_TO_RAD;
			}
		}

		LocationElement in = new LocationElement(lon, lat, rad);

		FRAME frame1 = FRAME.FK4, frame2 = FRAME.FK4;
		PRECISION precision = CDSQuery.PRECISION.MAS;
		double equinox1 = 2000.0, equinox2 = 2000.0;
		int p = viz.getFieldPosition("RAJ2000");
		if (p >= 0) {
			return in;
		} else {
			p = viz.getFieldPosition("RA_ICRS");
			if (p >= 0)
			{
				frame1 = CDSQuery.FRAME.ICRS;
				frame2 = CDSQuery.FRAME.FK5;
			} else {
				p = viz.getFieldPosition("RA1950");
				if (p >= 0)
				{
					frame1 = CDSQuery.FRAME.FK4; // Not sure, are B1950 ?
					frame2 = CDSQuery.FRAME.FK5;
					equinox1 = 1950.0;
				} else {
					throw new JPARSECException("cannot find coordinates frame.");
				}
			}
		}

		LocationElement out = new LocationElement();
		try {
			out = CDSQuery.query(frame1, frame2, in, precision, equinox1, equinox2);
		} catch (Exception e)
		{
			double vec[] = LocationElement.parseLocationElement(in);
			if (frame1 == CDSQuery.FRAME.ICRS) {
				double J2000[] = jparsec.ephem.Ephem.ICRStoDynamicalFrame(vec);
				out = LocationElement.parseRectangularCoordinates(J2000);
			} else {
				double J2000[] = jparsec.ephem.Precession.FK4_B1950ToFK5_J2000(vec);
				out = LocationElement.parseRectangularCoordinates(J2000);
			}
		}
		return out;
	}
}
