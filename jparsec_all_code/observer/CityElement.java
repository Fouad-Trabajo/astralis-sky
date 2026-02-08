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

import jparsec.util.JPARSECException;

/**
 * An adequate class for city data access.
 * <P>
 * This class is suitable for storing data from the class {@link City}.
 *
 * @see City
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class CityElement implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs an empty city object.
	 */
	public CityElement()
	{
		name = "";
		longitude = 0.0;
		latitude = 0.0;
		height = 0;
		timeZone = 0.0;
		country = "";
	}

	/**
	 * Constructs an explicit city object.
	 *
	 * @param city_name Name of the city.
	 * @param city_lon Longitude in degrees.
	 * @param city_lat Latitude in degrees.
	 * @param city_time Time zone in hours.
	 * @param city_alt Altitude in meters.
	 */
	public CityElement(String city_name, double city_lon, double city_lat, double city_time, int city_alt)
	{
		name = city_name;
		longitude = city_lon;
		latitude = city_lat;
		height = city_alt;
		timeZone = city_time;
		country = "";
	}

	/**
	 * Time zone of the city.
	 */
	public double timeZone;

	/**
	 * Name of the city.
	 */
	public String name;

	/**
	 * Longitude in degrees measured to the east of the city.
	 */
	public double longitude;

	/**
	 * Latitude in degrees of the city.
	 */
	public double latitude;

	/**
	 * Height above sea level in meters of the city.
	 */
	public int height;

	/**
	 * The name of the country.
	 */
	public String country;

	/**
	 * Creates a city object by searching the database for
	 * a certain city by name.
	 * @param name City name.
	 * @throws JPARSECException If an error occurs.
	 */
	public CityElement (String name)
	throws JPARSECException {
		if (name.equals("") || name == null) {
			name = "";
			longitude = 0.0;
			latitude = 0.0;
			height = 0;
			timeZone = 0.0;
			country = "";
		} else {
			CityElement city = City.findCity(name);
			this.name = city.name;
			longitude = city.longitude;
			latitude = city.latitude;
			height = city.height;
			timeZone = city.timeZone;
			country = city.country;
		}
	}
}
