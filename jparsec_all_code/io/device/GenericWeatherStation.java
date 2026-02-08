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

import jparsec.io.image.HeaderElement;

/**
 * An interface for the commands that should be available to all weather stations.
 * @author T. Alonso Albi - OAN (Spain)
 */
public interface GenericWeatherStation {

	/**
	 * The set of weather stations supported.
	 */
	public enum WEATHER_STATION_MODEL {
		/** A virtual station for testing. */
		VIRTUAL_WEATHER_STATION;

		/**
		 * Returns if this station is virtual or not.
		 * @return True or false.
		 */
		public boolean isVirtual() {
			if (this.name().startsWith("VIRTUAL_")) return true;
			return false;
		}
	}

	/**
	 * The set of possible weather forecast values.
	 */
	public enum WEATHER_FORECAST {
		/** Sunny. */
		SUNNY,
		/** Between sunny and cloudy. */
		SOME_CLOUDS,
		/** Cloudy. */
		CLOUDY,
		/** Rainy. */
		RAINY,
		/** Snowy. */
		SNOWY
	}

	/**
	 * Returns the temperature in C from the sensor outside.
	 * @return Temperature in C.
	 */
	public double getTemperature();
	/**
	 * Returns the temperature in C from the sensor inside.
	 * @return Temperature in C.
	 */
	public double getTemperatureInside();

	/**
	 * Returns the pressure in mbar.
	 * @return Pressure in mbar.
	 */
	public double getPressure();
	/**
	 * Returns the humidity between 0 and 100, from the sensor outside.
	 * @return Humidity, from 0 to 100.
	 */
	public double getHumidity();
	/**
	 * Returns the humidity between 0 and 100, from the sensor inside.
	 * @return Humidity, from 0 to 100.
	 */
	public double getHumidityInside();
	/**
	 * Returns the wind speed in m/s.
	 * @return Wind speed.
	 */
	public double getWindSpeed();
	/**
	 * Returns the wind direction.
	 * @return Wind's direction azimuth, 0 is north.
	 */
	public double getWindDirection();
	/**
	 * Returns if it is raining or not.
	 * @return True or false.
	 */
	public boolean isRaining();
	/**
	 * Returns if this station contains sensors inside, besides outside.
	 * @return True or false.
	 */
	public boolean hasInsideSensors();
	/**
	 * Returns the weather forecast for the following days. This can
	 * be an implementation using of web service. First index is for
	 * today.
	 * @return Values for weather forecast for today and (possibly)
	 * more days. Null is returned in case this is not supported.
	 */
	public WEATHER_FORECAST[] getForecastInFollowingDays();
	/**
	 * Returns the fits header describing the status of this weather station.
	 * The set of entries are:
	 * <pre>
	 * Entry        Value (typical)   Description
	 * ------------------------------------------
	 *
	 * TEMP         10                Temperature (C) outside
	 * PRES         1010              Pressure (mbar) outside
	 * HUM          50                Humidity % (0-100) outside
	 * TEMP_IN      20                Temperature (C) inside
	 * HUM_IN        0                Humidity % (0-100) inside
	 * WIND_SP       5                Wind speed (m/s)
	 * WIND_AZ       0                Wind direction (azimuth, deg)
	 * RAIN          false            Is raining ?
	 * </pre>
	 * @return The set of entries for the fits header.
	 */
	public HeaderElement[] getFitsHeader();
}
