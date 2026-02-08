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

import java.util.ArrayList;

import jparsec.ephem.Functions;
import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.io.image.HeaderElement;
import jparsec.math.Interpolation;
import jparsec.time.AstroDate;
import jparsec.vo.GeneralQuery;

/**
 * An implementation of a weather station for testing purposes.
 * @author T. Alonso Albi - OAN (Spain)
 */
public class VirtualWeatherStation implements GenericWeatherStation {

	/** Set to true to bypass the rain sensor, so that it will always return false even if 
	 * it is raining. For testing only, for instance with a virtual telescope and camera. */
	public static boolean BYPASS_RAIN_SENSOR = false;
	
	@Override
	public double getTemperature() {
		getForecastInFollowingDays();
		if (data == null) return 20;
		String currentData = data[0][TEMP];

		try {
			AstroDate astro = new AstroDate();
			double datay[] = DataSet.toDoubleValues(DataSet.toStringArray(currentData, ","));
			datay = DataSet.addDoubleArray(new double[] {datay[datay.length-1]}, datay);
			double datax[] = DataSet.getSetOfValues(0, 24, datay.length, false);

			double x = AstroDate.getDayFraction(astro.jd()) * 24.0;
			Interpolation interp = new Interpolation(datax, datay, false);
			return interp.linearInterpolation(x);
		} catch (Exception exc) {
			exc.printStackTrace();
		}
		return 0;
	}

	@Override
	public double getTemperatureInside() {
		return 0;
	}

	@Override
	public double getPressure() {
		return 1010;
	}

	@Override
	public double getHumidity() {
		getForecastInFollowingDays();
		if (data == null) return 0;
		if (BYPASS_RAIN_SENSOR) return 0;
		String currentData = data[0][HUM];

		try {
			AstroDate astro = new AstroDate();
			double datay[] = DataSet.toDoubleValues(DataSet.toStringArray(currentData, ","));
			datay = DataSet.addDoubleArray(new double[] {datay[datay.length-1]}, datay);
			double datax[] = DataSet.getSetOfValues(0, 24, datay.length, false);

			double x = AstroDate.getDayFraction(astro.jd()) * 24.0;
			Interpolation interp = new Interpolation(datax, datay, false);
			return interp.linearInterpolation(x);
		} catch (Exception exc) {
			exc.printStackTrace();
		}
		return 0;
	}

	@Override
	public double getHumidityInside() {
		return 0;
	}

	@Override
	public double getWindSpeed() {
		return 0;
	}

	@Override
	public double getWindDirection() {
		return 0;
	}

	@Override
	public boolean isRaining() {
		getForecastInFollowingDays();
		if (data == null) return false;
		if (BYPASS_RAIN_SENSOR) return false;
		
		String currentData = data[0][RAIN];

		try {
			AstroDate astro = new AstroDate();
			double datay[] = DataSet.toDoubleValues(DataSet.toStringArray(currentData, ","));
			datay = DataSet.addDoubleArray(new double[] {datay[datay.length-1]}, datay);
			double datax[] = DataSet.getSetOfValues(0, 24, datay.length, false);

			double x = AstroDate.getDayFraction(astro.jd()) * 24.0;
			Interpolation interp = new Interpolation(datax, datay, false);
			return (interp.linearInterpolation(x) > 80); // Rain probability > 80%
		} catch (Exception exc) {
			exc.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean hasInsideSensors() {
		return false;
	}

	@Override
	public WEATHER_FORECAST[] getForecastInFollowingDays() {
		long t = System.currentTimeMillis();
		if (lastForecastTime == -1 || (t - lastForecastTime) > (updateTimeHrs * 3600000)) {
			lastForecastTime = t;

			try {
				String out = GeneralQuery.query(forecastQuery);
				String o[] = DataSet.toStringArray(out, FileIO.getLineSeparator());
				ArrayList<WEATHER_FORECAST> list = new ArrayList<WEATHER_FORECAST>();
				ArrayList<String[]> data = new ArrayList<String[]>();
				int day = DataSet.getIndexContaining(o, "<dia fecha");
				do {
					//String date = getValue(o[day]);
					o = DataSet.getSubArray(o, day + 1, o.length-1);

					String nRain = "", nCloud = "", nTemp = "", nHum = "";
					boolean isT = false, isH = false;
					for (int i=0; i<o.length; i++) {
						if (o[i].indexOf("<dia fecha") >= 0) break;

						if (o[i].indexOf("<temperatura>") >= 0) isT = true;
						if (o[i].indexOf("</temperatura>") >= 0) isT = false;
						if (o[i].indexOf("<humedad_relativa>") >= 0) {
							isH = true;
							isT = false;
						}
						if (o[i].indexOf("</humedad_relativa>") >= 0) isH = false;

						if (o[i].indexOf("<prob_precipitacion") >= 0) nRain += ","+getValue2(o[i]);
						if (o[i].indexOf("<estado_cielo") >= 0) nCloud += ","+getValue(o[i].substring(o[i].indexOf("descripcion")));
						if (o[i].indexOf("<dato hora") >= 0 && isT) nTemp += ","+getValue2(o[i]);
						if (o[i].indexOf("<dato hora") >= 0 && isH) nHum += ","+getValue2(o[i]);
					}
					nCloud = DataSet.replaceAll(nCloud, "Muy nuboso con lluvia escasa", "3", true);
					nCloud = DataSet.replaceAll(nCloud, "Muy nuboso con lluvia", "3", true);
					nCloud = DataSet.replaceAll(nCloud, "Muy nuboso con tormenta", "3", true);
					nCloud = DataSet.replaceAll(nCloud, "Muy nuboso", "2", true);
					nCloud = DataSet.replaceAll(nCloud, "Despejado", "0", true);
					nCloud = DataSet.replaceAll(nCloud, "Poco nuboso", "1", true);
					nCloud = DataSet.replaceAll(nCloud, "Intervalos nubosos con lluvia escasa", "3", true);
					nCloud = DataSet.replaceAll(nCloud, "Intervalos nubosos con lluvia", "3", true);
					nCloud = DataSet.replaceAll(nCloud, "Intervalos nubosos con tormenta", "3", true);
					nCloud = DataSet.replaceAll(nCloud, "Intervalos nubosos", "1", true);
					nCloud = DataSet.replaceAll(nCloud, "Nuboso con tormenta", "3", true);
					nCloud = DataSet.replaceAll(nCloud, "Nuboso con lluvia escasa", "3", true);
					nCloud = DataSet.replaceAll(nCloud, "Nuboso con lluvia", "3", true);
					nCloud = DataSet.replaceAll(nCloud, "Nuboso", "2", true);
					nCloud = DataSet.replaceAll(nCloud, "Nubes altas con lluvia escasa", "3", true);
					nCloud = DataSet.replaceAll(nCloud, "Nubes altas", "1", true);
					nCloud = DataSet.replaceAll(nCloud, "Nubes altas con lluvia", "3", true);
					nCloud = DataSet.replaceAll(nCloud, "Cubierto con lluvia escasa", "3", true);
					nCloud = DataSet.replaceAll(nCloud, "Cubierto con lluvia", "3", true);
					// ... TODO


					if (nRain.equals("")) nRain = " ";
					if (nCloud.equals("")) nCloud = " ";
					if (nTemp.equals("")) nTemp = " ";
					if (nHum.equals("")) nHum = " ";
					try {
						String c[] = DataSet.toStringArray(nCloud, ",");
						double val[] = DataSet.toDoubleValues(c);
						int mean = (int) (Functions.sumComponents(val) / val.length + 0.5);
						list.add(WEATHER_FORECAST.values()[mean]);
					} catch (Exception exc) {
						exc.printStackTrace();
						list.add(WEATHER_FORECAST.SOME_CLOUDS);
					}

					data.add(new String[] {nRain.substring(1), nCloud.substring(1), nTemp.substring(1), nHum.substring(1)});

					day = DataSet.getIndexContaining(o, "<dia fecha");
				} while (day >= 0);

				lastForecast = new WEATHER_FORECAST[list.size()];
				this.data = new String[list.size()][];
				for (int i=0; i<list.size(); i++) {
					lastForecast[i] = list.get(i);
					this.data[i] = data.get(i);
				}
			} catch (Exception exc) {
				exc.printStackTrace();
			}
		}
		return lastForecast;
	}

	private String getValue(String line) {
		int i0 = line.indexOf("\""), i1 = line.lastIndexOf("\"");
		return line.substring(i0+1,  i1);
	}
	private String getValue2(String line) {
		int i0 = line.indexOf(">"), i1 = line.lastIndexOf("</");
		return line.substring(i0+1,  i1);
	}

	@Override
	public HeaderElement[] getFitsHeader() {
		HeaderElement header[] = new HeaderElement[] {
				new HeaderElement("TEMP", Functions.formatValue(getTemperature(), 3), "Temperature (C) outside"),
				new HeaderElement("PRES", Functions.formatValue(getPressure(), 3), "Pressure (mbar) outside"),
				new HeaderElement("HUM", Functions.formatValue(getHumidity(), 3), "Humidity % (0-100) outside"),
				new HeaderElement("TEMP_IN", Functions.formatValue(getTemperatureInside(), 3), "Temperature (C) inside"),
				new HeaderElement("HUM_IN", Functions.formatValue(getHumidityInside(), 3), "Humidity % (0-100) inside"),
				new HeaderElement("WIND_SP", Functions.formatValue(getWindSpeed(), 3), "Wind speed (m/s)"),
				new HeaderElement("WIND_AZ", Functions.formatValue(getWindDirection(), 3), "Wind direction (azimuth, deg)"),
				new HeaderElement("RAIN", ""+this.isRaining(), "Is raining ?")
		};
		return header;
	}

	private long lastForecastTime = -1;
	private WEATHER_FORECAST[] lastForecast = null;
	private String[][] data = null;
	private String forecastQuery;
	private static final int updateTimeHrs = 12, RAIN = 0, TEMP = 2, HUM = 3;

	/**
	 * Constructor for a virtual weather station.
	 * @param postalCode Postal code to obtain weather forecast in Spain.
	 * For other countries set to null and Madrid will be used.
	 */
	public VirtualWeatherStation(String postalCode) {
		String pc = postalCode;
		if (pc == null || pc.length() != 5) pc = "28038";
		forecastQuery = "http://www.aemet.es/xml/municipios/localidad_"+pc+".xml";

		getForecastInFollowingDays();
	}
}
