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
package jparsec.observer;

import java.io.Serializable;

import jparsec.ephem.Functions;
import jparsec.util.JPARSECException;

/**
 * An adequate class for storing data from an observatory. <P>
 * This class is suitable for storing data from the {@linkplain Observatory} class. Note that there's no
 * time zone field, since observatory data has no information about this. So it
 * is necessary to avoid local time in ephemerides calculations if you set an
 * observatory as the observer's location, or to set that value after parsing
 * this object to an {@linkplain ObserverElement}.
 *
 * @see Observatory
 * @see ObserverElement
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class ObservatoryElement implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs an empty observatory, with numerical fields equal to
	 * 0.0 and strings equal to "".
	 */
	public ObservatoryElement()
	{
		code = 0;
		location = "";
		name = "";
		country = "";
		longitude = 0.0;
		latitude = 0.0;
		height = 0;
		reference = "";
	}

	/**
	 * Constructs an observatory object giving the values of the fields.
	 *
	 * @param obs_num Number of this observatory in the Marsden official list.
	 * @param obs_loc Place where the observatory is located.
	 * @param obs_name Name of the observatory.
	 * @param obs_country The country the observatory belongs to.
	 * @param obs_lon Longitude in degrees.
	 * @param obs_lat Latitude in degrees.
	 * @param obs_alt Altitude above sea level in meters.
	 * @param obs_ref Reference code to the observatory.
	 */
	public ObservatoryElement(int obs_num, String obs_loc, String obs_name, String obs_country, double obs_lon, double obs_lat,
			int obs_alt, String obs_ref)
	{
		code = obs_num;
		location = obs_loc;
		name = obs_name;
		longitude = obs_lon;
		latitude = obs_lat;
		height = obs_alt;
		reference = obs_ref;
		country = obs_country;
	}

	/**
	 * Marsden numerical code for the observatory.
	 */
	public int code;

	/**
	 * Location of the observatory.
	 */
	public String location;

	/**
	 * Name of the observatory.
	 */
	public String name;

	/**
	 * The country the observatory belongs to.
	 */
	public String country;

	/**
	 * Longitude in degrees measured to the east of the observatory.
	 */
	public double longitude;

	/**
	 * Latitude in degrees of the observatory.
	 */
	public double latitude;

	/**
	 * Height above sea level in meters of the observatory.
	 */
	public int height;

	/**
	 * Code of the publication reference for the data of this observatory if it
	 * is set from data by Sveshnikov, otherwise the original Marsden code of
	 * the observatory.
	 */
	public String reference;

	/**
	 * Creates an observatory object by searching the database for
	 * a certain observatory by name.
	 * @param name Observatory name.
	 * @throws JPARSECException If an error occurs.
	 */
	public ObservatoryElement (String name)
	throws JPARSECException {
		if (name.equals("") || name == null)
		{
			code = 0;
			location = "";
			name = "";
			longitude = 0.0;
			latitude = 0.0;
			height = 0;
			reference = "";
			country = "";
		} else {
			ObservatoryElement obs = Observatory.findObservatorybyName(name);
			this.name = obs.name;
			longitude = obs.longitude;
			latitude = obs.latitude;
			height = obs.height;
			code = obs.code;
			location = obs.location;
			reference = obs.reference;
			country = obs.country;
		}
	}

	/**
	 * Transforms the string code of an observatory to an integer to be saved
	 * in an instance of {@linkplain ObservatoryElement}.
	 * @param code The string code.
	 * @return The integer equivalent.
	 */
	public static int getObservatoryCodeAsInt(String code) {
		int cent = Integer.parseInt(code.substring(0, 1), Character.MAX_RADIX);
		int obs_code = cent * 100 + Integer.parseInt(code.substring(1));
		return obs_code;
	}

	/**
	 * Transforms the integer code of an observatory saved in an instance of
	 * {@linkplain ObservatoryElement} to its string equivalent.
	 * @param code The observatory code as an integer.
	 * @return The string equivalent.
	 */
	public static String getObservatoryCodeAsString(int code) {
		int cent = code / 100;
		int value = code - cent * 100;
		return Integer.toString(cent, Character.MAX_RADIX).toUpperCase()+Functions.fmt(value, 2);
	}
}
