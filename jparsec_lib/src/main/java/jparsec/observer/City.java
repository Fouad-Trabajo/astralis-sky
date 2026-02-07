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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import jparsec.ephem.Functions;
import jparsec.graph.DataSet;
import jparsec.io.FileFormatElement;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.io.ReadFormat;
import jparsec.math.Constant;
import jparsec.observer.Country.COUNTRY;
import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Logger.LEVEL;

/**
 * A convenient class for city data management.
 * <BR>
 * <I><B>Description</B></I>
 * <BR>
 * This class contains full data of about 4200 cities/places around the world,
 * providing a suitable way of getting coordinates of certain location to
 * calculate ephemeris. Methods starting with 'get' perform the search for
 * the main database of cities around the world, methods starting with 'find'
 * extend the search for 8000 additional locations in Spain.
 * <BR>
 * <I><B>Format</B></I>
 * <BR>
 * Data is organized in arrays of type {@linkplain CityElement} for each
 * country, containing data objets with the convenient fields.
 * <BR>
 * <I><B>Reference</B></I>
 * <BR>
 * Data has been compiled using different sources and medias. Different catalogs
 * have been used, and a lot of changes and updates were made manually. The
 * height above see level was computed using the 4k elevation image at <A
 * target="_blank" href="http://www.space-graphics.com/e43_elevation1.htm">space-graphics.com</A>. See
 * it for details. The calculation made is an averaged measure of the altitude
 * in a circle of radius r &lt;= 5 km (5 km for the equator). In plane terrains
 * maximum theoretical error is 15m. Typical errors will be about 50m.
 *
 * @see CityElement
 * @see Country
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public final class City
{
	// private constructor so that this class cannot be instantiated.
	private City() {}

	/**
	 * The list of capitals for each of the Spanish provinces.
	 */
	public static final String SpainCapitals[] = new String[] {
			"Girona", "Ourense", "Donostia-San Sebasti\u00e1n", "Lleida", "Castell\u00f3n de la Plana",
			"Pamplona", "Vitoria-Gasteiz",
			"Albacete", "Alicante", "Almer\u00eda", "Oviedo", "\u00c1vila", "Badajoz",
			"Palma de Mallorca", "Barcelona", "Burgos", "C\u00e1ceres", "C\u00e1diz", "Santander",
			"Ceuta", "Ciudad Real", "C\u00f3rdoba", "Cuenca", "Zaragoza",
			"Granada", "Guadalajara", "Huelva", "Huesca", "Ja\u00e9n", "La Coru\u00f1a",
			"Logro\u00f1o", "Las Palmas de Gran Canaria", "Le\u00f3n", "Lugo", "Madrid", "M\u00e1laga",
			"Melilla", "M\u00e9rida", "Murcia", "Palencia", "Pontevedra",
			"Salamanca", "Santa Cruz de Tenerife", "Santiago de Compostela", "Segovia", "Sevilla",
			"Soria", "Tarragona", "Teruel", "Toledo", "Valencia", "Valladolid", "Bilbao", "Zamora",
	};

	/**
	 * The list of Spanish provinces (regions) in Spain.
	 */
	public static final String SpainProvinces[] = new String[] {
			"Girona", "Orense", "Guip\u00fazcoa", "Lleida", "Castell\u00f3n",
			"Navarra", "\u00c1lava",
			"Albacete", "Alicante", "Almer\u00eda", "Asturias", "\u00c1vila", "Badajoz",
			"Baleares", "Barcelona", "Burgos", "C\u00e1ceres", "C\u00e1diz", "Santander",
			"Ceuta", "Ciudad Real", "C\u00f3rdoba", "Cuenca", "Zaragoza",
			"Granada", "Guadalajara", "Huelva", "Huesca", "Ja\u00e9n", "La Coru\u00f1a",
			"La Rioja", "Las Palmas", "Le\u00f3n", "Lugo", "Madrid", "M\u00e1laga",
			"Melilla", "M\u00e9rida", "Murcia", "Palencia", "Pontevedra",
			"Salamanca", "Santa Cruz de Tenerife", "Santiago de Compostela", "Segovia", "Sevilla",
			"Soria", "Tarragona", "Teruel", "Toledo", "Valencia", "Valladolid", "Vizcaya", "Zamora",
	};

	/**
	 * Returns the number of cities in the database.
	 * @return Number of cities, currently 4191.
	 * @throws JPARSECException If an error occurs reading the cities file.
	 */
	public static int getNumberOfCities() throws JPARSECException {
		int n = 0;

		// Connect to the file
		try
		{
			InputStream is = City.class.getClassLoader().getResourceAsStream("jparsec/observer/cities.txt");
			BufferedReader dis = new BufferedReader(new InputStreamReader(is, ReadFile.ENCODING_ISO_8859));

			while ((dis.readLine()) != null)
			{
				n++;
			}

			// Close file
			dis.close();

		} catch (FileNotFoundException e1)
		{
			throw new JPARSECException("cities file not found in path jparsec/observer/cities.txt.", e1);
		} catch (IOException e2)
		{
			throw new JPARSECException(
					"error while reading cities file.", e2);
		}

		return n;
	}
	/**
	 * Get the cities for certain country.
	 *
	 * @param country_ID The ID constant of the country whose cities are required.
	 * @return The array CityElement[] containing cities for the selected
	 *         country, or null if the database has no cities for a given country.
	 * @throws JPARSECException If an error occurs reading the cities file.
	 */
	public static CityElement[] getCities(COUNTRY country_ID) throws JPARSECException {
		// Define necessary variables
		ArrayList<CityElement> vec = new ArrayList<CityElement>();
		String file_line = "";

		FileFormatElement[] format = new FileFormatElement[] {
				new FileFormatElement(1, 70, "name"),
				new FileFormatElement(71, 120, "country"),
				new FileFormatElement(121, 130, "longitude"),
				new FileFormatElement(131, 139, "latitude"),
				new FileFormatElement(140, 145, "timeZone"),
				new FileFormatElement(146, 149, "height")
		};
		ReadFormat rf = new ReadFormat(format);

		String cc = country_ID.toString();

		// Connect to the file
		try
		{
			InputStream is = City.class.getClassLoader().getResourceAsStream("jparsec/observer/cities.txt");
			BufferedReader dis = new BufferedReader(new InputStreamReader(is, ReadFile.ENCODING_ISO_8859));

			while ((file_line = dis.readLine()) != null)
			{
				CityElement city = new CityElement();
				city.country = rf.readString(file_line, "country");
				if (city.country.equals(cc)) {
					city.name = rf.readString(file_line, "name");
					city.longitude = rf.readDouble(file_line, "longitude");
					city.latitude = rf.readDouble(file_line, "latitude");
					city.timeZone = rf.readDouble(file_line, "timeZone");
					city.height = rf.readInteger(file_line, "height");
					vec.add(city);
				}
			}

			// Close file
			dis.close();

		} catch (FileNotFoundException e1)
		{
			throw new JPARSECException("cities file not found in path jparsec/observer/cities.txt.", e1);
		} catch (IOException e2)
		{
			throw new JPARSECException(
					"error while reading cities file.", e2);
		}

		if (vec.size() == 0) return null;
		CityElement loc_element[] = new CityElement[vec.size()];

		try {
			for (int i = 0; i < loc_element.length; i++) {
				loc_element[i] = vec.get(i);
				loc_element[i].country = cc;
			}
		} catch (Exception exc) {
			Logger.log(LEVEL.ERROR, "Input country not found. This should never happen.");
		}

		return (loc_element);
	}

	/**
	 * Get the cities for a certain name.
	 *
	 * @param name Name of the city or part of it.
	 * @return The array CityElement[] containing cities for the selected
	 *         name, or null if the database has no cities with that name.
	 * @throws JPARSECException If an error occurs reading the cities file.
	 */
	public static CityElement[] getCities(String name) throws JPARSECException {
		// Define necessary variables
		ArrayList<CityElement> vec = new ArrayList<CityElement>();
		String file_line = "";

		FileFormatElement[] format = new FileFormatElement[] {
				new FileFormatElement(1, 70, "name"),
				new FileFormatElement(71, 120, "country"),
				new FileFormatElement(121, 130, "longitude"),
				new FileFormatElement(131, 139, "latitude"),
				new FileFormatElement(140, 145, "timeZone"),
				new FileFormatElement(146, 149, "height")
		};
		ReadFormat rf = new ReadFormat(format);

		// Connect to the file
		try
		{
			InputStream is = City.class.getClassLoader().getResourceAsStream("jparsec/observer/cities.txt");
			BufferedReader dis = new BufferedReader(new InputStreamReader(is, ReadFile.ENCODING_ISO_8859));

			name = name.toLowerCase();
			while ((file_line = dis.readLine()) != null)
			{
				CityElement city = new CityElement();
				city.name = rf.readString(file_line, "name");
				if (city.name.toLowerCase().indexOf(name) >= 0) {
					city.country = rf.readString(file_line, "country");
					city.longitude = rf.readDouble(file_line, "longitude");
					city.latitude = rf.readDouble(file_line, "latitude");
					city.timeZone = rf.readDouble(file_line, "timeZone");
					city.height = rf.readInteger(file_line, "height");
					vec.add(city);
				}
			}

			// Close file
			dis.close();

		} catch (FileNotFoundException e1)
		{
			throw new JPARSECException("cities file not found in path jparsec/observer/cities.txt.", e1);
		} catch (IOException e2)
		{
			throw new JPARSECException(
					"error while reading cities file.", e2);
		}

		if (vec.size() == 0) return null;
		CityElement loc_element[] = new CityElement[vec.size()];

		try {
			for (int i = 0; i < loc_element.length; i++) {
				loc_element[i] = vec.get(i);
			}
		} catch (Exception exc) {
			Logger.log(LEVEL.ERROR, "Input country not found. This should never happen.");
		}

		return (loc_element);
	}

	/**
	 * Get the cities for certain country.
	 *
	 * @param country_ID The ID constant of the country whose cities are required.
	 * @param SpainInDetail True to return a detailed list of Spanish locations,
	 * instead of the main ones, in case the input country is Spain.
	 * @return The array CityElement[] containing cities for the selected
	 *         country, or null if the database has no cities for a given country.
	 * @throws JPARSECException If an error occurs reading the cities file.
	 */
	public static CityElement[] getCities(COUNTRY country_ID, boolean SpainInDetail) throws JPARSECException {
		// Define necessary variables
		ArrayList<CityElement> vec = new ArrayList<CityElement>();
		String cc = null;

		if (country_ID == COUNTRY.Spain && SpainInDetail) {
			ArrayList<String> file = ReadFile.readResource("jparsec/observer/SpainCities.txt", ReadFile.ENCODING_ISO_8859);
			for (int i = 0; i < file.size(); i = i + 2) {
				String fi = file.get(i);
				String fip = file.get(i + 1);
				String n = fi.substring(0, 48).trim();
				String lats = fip.substring(0, 21).trim();
				String lons = (fip.substring(21, 22) + fip
						.substring(22, 31).trim()).trim();
				String alt = fip.substring(42).trim();
				double lo = Functions.parseDeclination(lons);
				double la = Functions.parseDeclination(lats);

				double lat = la * Constant.RAD_TO_DEG;
				CityElement loc_element = new CityElement(n, lo
						* Constant.RAD_TO_DEG, lat, 1.0, Integer.parseInt(alt));
				if (lat < 32) loc_element.timeZone = 0;
				loc_element.country = "Spain";

				vec.add(loc_element);
			}
			cc = country_ID.toString();
		} else {
			String file_line = "";

			FileFormatElement[] format = new FileFormatElement[] {
					new FileFormatElement(1, 70, "name"),
					new FileFormatElement(71, 120, "country"),
					new FileFormatElement(121, 130, "longitude"),
					new FileFormatElement(131, 139, "latitude"),
					new FileFormatElement(140, 145, "timeZone"),
					new FileFormatElement(146, 149, "height")
			};
			ReadFormat rf = new ReadFormat(format);

			cc = country_ID.toString();

			// Connect to the file
			try
			{
				InputStream is = City.class.getClassLoader().getResourceAsStream("jparsec/observer/cities.txt");
				BufferedReader dis = new BufferedReader(new InputStreamReader(is, ReadFile.ENCODING_ISO_8859));

				while ((file_line = dis.readLine()) != null)
				{
					CityElement city = new CityElement();
					city.country = rf.readString(file_line, "country");
					if (city.country.equals(cc)) {
						city.name = rf.readString(file_line, "name");
						city.longitude = rf.readDouble(file_line, "longitude");
						city.latitude = rf.readDouble(file_line, "latitude");
						city.timeZone = rf.readDouble(file_line, "timeZone");
						city.height = rf.readInteger(file_line, "height");
						vec.add(city);
					}
				}

				// Close file
				dis.close();

			} catch (FileNotFoundException e1)
			{
				throw new JPARSECException("cities file not found in path jparsec/observer/cities.txt.", e1);
			} catch (IOException e2)
			{
				throw new JPARSECException(
						"error while reading cities file.", e2);
			}
			}

		if (vec.size() == 0) return null;
		CityElement loc_element[] = new CityElement[vec.size()];

		try {
			for (int i = 0; i < loc_element.length; i++) {
				loc_element[i] = vec.get(i);
				loc_element[i].country = cc;
			}
		} catch (Exception exc) {
			Logger.log(LEVEL.ERROR, "Input country not found. This should never happen.");
		}

		return (loc_element);
	}

	/**
	 * Get all cities.
	 *
	 * @return The array CityElement[] containing all the cities.
	 * @throws JPARSECException If an error occurs reading the cities file.
	 */
	public static CityElement[] getAllCities() throws JPARSECException {
		CityElement[] loc_element = new CityElement[City.getNumberOfCities()];

		int country = -1;
		for (int i = COUNTRY.Afghanistan.ordinal(); i <= COUNTRY.Zimbabwe.ordinal(); i++) {
			CityElement[] loc_element_city = jparsec.observer.City.getCities(COUNTRY.values()[i]);

			String c = "";
			try {
				c = COUNTRY.values()[i].toString();
			} catch (Exception exc) {
				Logger.log(LEVEL.ERROR, "Country "+i+" not found. This should never happen.");
			}


			if (loc_element_city == null) continue;
//				Logger.log(LEVEL.ERROR, "No cities found for country "+c+". This should never happen.");

			for (int j = 0; j < loc_element_city.length; j++) {
				country++;
				loc_element[country] = loc_element_city[j];
				loc_element[country].country = c;
			}
		}

		return (loc_element);
	}

	/**
	 * Find all cities given it's name or part of it. Returned array is ordered
	 * in descent order respect the relevance of the city found.
	 *
	 * @param city_location The name of the city or representative part of it's name.
	 * @return The list of cities, or null if none is found.
	 * @throws JPARSECException If an error occurs.
	 */
	public static CityElement[] findAllCities(String city_location) throws JPARSECException {
		return findAllCities(city_location, 0);
	}

	/**
	 * Find all cities given it's name or part of it. Returned array is ordered
	 * in descent order respect the relevance of the city found.
	 *
	 * @param city_location The name of the city or representative part of it's name.
	 * @param max Maximum number of locations to return. 0 or negative will return all found.
	 * @return The list of cities, or null if none is found.
	 * @throws JPARSECException If an error occurs.
	 */
	public static CityElement[] findAllCities(String city_location, int max)
			throws JPARSECException {
		ArrayList<CityElement> list = new ArrayList<CityElement>();
		ArrayList<String> listQ = new ArrayList<String>();

		boolean equals = false;

		String record = city_location.toLowerCase();
		record = record.substring(0, 1).toUpperCase() + record.substring(1);
		ArrayList<String> file = readResourceContainingOnlyForSpanishCities("jparsec/observer/SpainCities.txt", ReadFile.ENCODING_ISO_8859, record);
		//ArrayList<String> file = ReadFile.readResource("jparsec/observer/SpainCities.txt");
		for (int i = 0; i < file.size(); i = i + 2) {
			String fi = file.get(i);
			String fip = file.get(i + 1);
			String n = fi.substring(0, 48).trim();
			String lats = fip.substring(0, 21).trim();
			String lons = (fip.substring(21, 22) + fip
					.substring(22, 31).trim()).trim();
			String alt = fip.substring(42).trim();
			double lo = Functions.parseDeclination(lons);
			double la = Functions.parseDeclination(lats);

			if (n.toUpperCase().indexOf(city_location.toUpperCase()) >= 0) {
				double lat = la * Constant.RAD_TO_DEG;
				CityElement loc_element = new CityElement(n, lo
						* Constant.RAD_TO_DEG, lat, 1.0, Integer.parseInt(alt));
				if (lat < 32)
					loc_element.timeZone = 0;
				loc_element.country = "Spain";

				list.add(loc_element);
				
				boolean equalLoc = n.toUpperCase().equals(city_location.toUpperCase());
				if (!equalLoc && n.indexOf("/") > 0) {
					String names[] = DataSet.toStringArray(n, "/", false);
					for (int j=0; j<names.length; j++) {
						if (names[j].toUpperCase().equals(city_location.toUpperCase())) {
							equalLoc = true;
							break;
						}
					}
				}
				if (equalLoc) {
					listQ.add("0-" + file.size() / 2);
					equals = true;
					if (max > 0 && list.size() >= max) break;
				} else {
					listQ.add("1-" + file.size() / 2);
				}

			}
		}

		if (max <= 0 || !equals) {
			CityElement[] loc_element_city = jparsec.observer.City.getCities(city_location);
			try {
				for (int j = 0; j < loc_element_city.length; j++) {
					CityElement loc_element = loc_element_city[j];
					list.add(loc_element);
					if (loc_element_city[j].name.toUpperCase().equals(
							city_location.toUpperCase())) {
						equals = true;
						listQ.add("0-" + loc_element_city.length);
						if (max > 0 && list.size() >= max) break;
					} else {
						listQ.add("1-" + loc_element_city.length);
					}
				}
			} catch (NullPointerException e) {	}
		}

		if (list.size() == 0)
			return null;

		if (equals) {
			for (int i=listQ.size()-1;i>=0;i--) {
				String l = listQ.get(i);
				if (l.startsWith("1-")) {
					listQ.remove(i);
					list.remove(i);
				}
			}
		}

		CityElement city[] = new CityElement[list.size()];
		int index = 0;
		do {
			CityElement best = null;
			int bestQ1 = 0;
			int bestQ2 = 0;
			int bestI = 0;
			for (int i = 0; i < list.size(); i++) {
				CityElement c = list.get(i);
				String q = listQ.get(i);
				int q1 = Integer.parseInt(FileIO.getField(1, q, "-", true));
				int q2 = Integer.parseInt(FileIO.getField(2, q, "-", true));
				if (q1 <= bestQ1 && q2 >= bestQ2 || best == null) {
					best = c;
					bestQ1 = q1;
					bestQ2 = q2;
					bestI = i;
				}
			}
			city[index] = best;
			index++;
			list.remove(bestI);
			listQ.remove(bestI);
		} while (list.size() > 0);

		return city;
	}

	/**
	 * Find all cities given it's name or part of it. Returned array is ordered
	 * in descent order respect the relevance of the city found.
	 *
	 * @param city_location The name of the city or representative part of it's name.
	 * @param SpainInDetail True to use the detailed file for Spanish cities. False
	 * recommended for Android systems.
	 * @return The list of cities, or null if none is found.
	 * @throws JPARSECException If an error occurs.
	 */
	public static CityElement[] findAllCities(String city_location, boolean SpainInDetail)
			throws JPARSECException {
		ArrayList<CityElement> list = new ArrayList<CityElement>();
		ArrayList<String> listQ = new ArrayList<String>();

		boolean equals = false;

		String record = city_location.toLowerCase();
		record = record.substring(0, 1).toUpperCase() + record.substring(1);
		if (SpainInDetail) {
			ArrayList<String> file = readResourceContainingOnlyForSpanishCities("jparsec/observer/SpainCities.txt", ReadFile.ENCODING_ISO_8859, record);
			//ArrayList<String> file = ReadFile.readResource("jparsec/observer/SpainCities.txt");
			for (int i = 0; i < file.size(); i = i + 2) {
				String fi = file.get(i);
				String fip = file.get(i + 1);
				String n = fi.substring(0, 48).trim();
				String lats = fip.substring(0, 21).trim();
				String lons = (fip.substring(21, 22) + fip
						.substring(22, 31).trim()).trim();
				String alt = fip.substring(42).trim();
				double lo = Functions.parseDeclination(lons);
				double la = Functions.parseDeclination(lats);

				if (n.toUpperCase().indexOf(city_location.toUpperCase()) >= 0) {
					double lat = la * Constant.RAD_TO_DEG;
					CityElement loc_element = new CityElement(n, lo
							* Constant.RAD_TO_DEG, lat, 1.0, Integer.parseInt(alt));
					if (lat < 32)
						loc_element.timeZone = 0;
					loc_element.country = "Spain";

					list.add(loc_element);
					boolean equalLoc = n.toUpperCase().equals(city_location.toUpperCase());
					if (!equalLoc && n.indexOf("/") > 0) {
						String names[] = DataSet.toStringArray(n, "/", false);
						for (int j=0; j<names.length; j++) {
							if (names[j].toUpperCase().equals(city_location.toUpperCase())) {
								equalLoc = true;
								break;
							}
						}
					}
					if (equalLoc) {
						listQ.add("0-" + file.size() / 2);
						equals = true;
					} else {
						listQ.add("1-" + file.size() / 2);
					}
				}
			}
		}

		CityElement[] loc_element_city = jparsec.observer.City.getCities(city_location);
		if (loc_element_city != null) {
			for (int j = 0; j < loc_element_city.length; j++) {
				CityElement loc_element = loc_element_city[j];
				list.add(loc_element);
				if (loc_element_city[j].name.toUpperCase().equals(
						city_location.toUpperCase())) {
					equals = true;
					listQ.add("0-" + loc_element_city.length);
				} else {
					listQ.add("1-" + loc_element_city.length);
				}
			}
		}

		if (list.size() == 0)
			return null;

		if (equals) {
			for (int i=listQ.size()-1;i>=0;i--) {
				String l = listQ.get(i);
				if (l.startsWith("1-")) {
					listQ.remove(i);
					list.remove(i);
				}
			}
		}

		CityElement city[] = new CityElement[list.size()];
		int index = 0;
		do {
			CityElement best = null;
			int bestQ1 = 0;
			int bestQ2 = 0;
			int bestI = 0;
			for (int i = 0; i < list.size(); i++) {
				CityElement c = list.get(i);
				String q = listQ.get(i);
				int q1 = Integer.parseInt(FileIO.getField(1, q, "-", true));
				int q2 = Integer.parseInt(FileIO.getField(2, q, "-", true));
				if (q1 <= bestQ1 && q2 >= bestQ2 || best == null) {
					best = c;
					bestQ1 = q1;
					bestQ2 = q2;
					bestI = i;
				}
			}
			city[index] = best;
			index++;
			list.remove(bestI);
			listQ.remove(bestI);
		} while (list.size() > 0);

		return city;
	}

	static protected CityElement c_Madrid = null;

	/**
	 * Find one city given it's name or part of it. When there are several cities with 
	 * the same same it is possible to avoid confusion with two options: adding the country 
	 * name between parenthesis, like Roma (Italy), or adding an integer in brackets representing which 
	 * to return in the order given in the database, like Roma[0] for the first result.
	 *
	 * @param city_location The name of the city or representative part of it's name.
	 * In case several locations could exists with the same name, you can use the
	 * suffix [0] to get the first, [1] for the second, and so on. Or you can add the country 
	 * between parenthesis to avoid confusion. The complete
	 * list of locations with a given name is provided by {@linkplain City#findAllCities(String)}.
	 * @return The object CityElement containing the first city that matches the
	 *         search, or the best match. Null if none is found. In case several are found with the
	 *         provided name the most relevant one is returned (for Spanish cities the one using
	 *         the detailed file), equivalent to adding the suffix [0] to the name (first result).
	 * @throws JPARSECException If an error occurs.
	 */
	public static CityElement findCity(String city_location)
			throws JPARSECException {
		if (c_Madrid != null && city_location.equals("Madrid")) return c_Madrid;
		int b = city_location.indexOf("[");
		int ba = city_location.lastIndexOf("(");
		int n = 0;
		String country = null;
		boolean specified = false;
		if (b > 0) {
			int bb = city_location.indexOf("]");
			if (bb > b + 1) {
				n = Integer.parseInt(city_location.substring(b+1, bb));
				city_location = city_location.substring(0, b);
				specified = true;
			}
		} else {
			if (ba > 0) {
				String co = city_location.substring(ba+1);
				String cl = city_location.substring(0, ba).trim();
				int baf = co.indexOf(")");
				if (baf > 0) co = co.substring(0, baf).trim();
				country = co.toLowerCase();
				
				// Since there are some places in the list of Spain locations with 
				// same names and they are discriminated by the province name between 
				// parenthesis, like Cieza (Murcia) and Cieza (Cantabria), first 
				// be sure the input string between parenthesis belongs to a country
				// name. Otherwise, the text between parenthesis is considered to be 
				// part of the location name.
				COUNTRY cy[] = COUNTRY.values();
				boolean countryFound = false;
				for (int i=0; i<cy.length; i++) {
					if (cy[i].toString().toLowerCase().equals(country)) {
						countryFound = true;
						break;
					}
				}
				if (countryFound) {
					city_location = cl;
					specified = true;
				}
			}
		}

		CityElement c[] = findAllCities(city_location, specified ? 0 : 1);
		if (c != null) {
			if (c.length > 1 && !specified) JPARSECException.addWarning("Found "+c.length+" locations named '"+city_location+"'. Selecting the one with index number "+n+".");
			if (c_Madrid == null && city_location.equals("Madrid")) c_Madrid = c[n];
			if (country != null) {
				for (int i=0; i<c.length; i++) {
					if (c[i].country.toLowerCase().equals(country)) return c[i];
				}
			}
			if (n >= 0) return c[n];
		}

		return null;
	}

	/**
	 * Find the nearest city to certain coordinates.
	 *
	 * @param loc The approximate coordinates of the city.
	 * @return The object CityElement containing the city that best matches the search.
	 * @throws JPARSECException Thrown if the city is not found.
	 */
	public static CityElement findNearestCity(LocationElement loc)
			throws JPARSECException {
		CityElement loc_element = null;
		double distance = 2.0;

		// Define necessary variables
		String file_line = "";

		FileFormatElement[] format = new FileFormatElement[] {
				new FileFormatElement(1, 70, "name"),
				new FileFormatElement(71, 120, "country"),
				new FileFormatElement(121, 130, "longitude"),
				new FileFormatElement(131, 139, "latitude"),
				new FileFormatElement(140, 145, "timeZone"),
				new FileFormatElement(146, 149, "height")
		};
		ReadFormat rf = new ReadFormat(format);

		// Connect to the file
		try
		{
			InputStream is = City.class.getClassLoader().getResourceAsStream("jparsec/observer/cities.txt");
			BufferedReader dis = new BufferedReader(new InputStreamReader(is, ReadFile.ENCODING_ISO_8859));

			while ((file_line = dis.readLine()) != null)
			{
				CityElement city = new CityElement();
				city.longitude = rf.readDouble(file_line, "longitude");
				city.latitude = rf.readDouble(file_line, "latitude");

				LocationElement loc1 = LocationElement.parseCity(city);
				double distance1 = Math.abs(LocationElement
						.getLinearDistance(loc, loc1));
				if (distance1 < distance) {
					distance = distance1;
					city.name = rf.readString(file_line, "name");
					city.country = rf.readString(file_line, "country");
					city.timeZone = rf.readDouble(file_line, "timeZone");
					city.height = rf.readInteger(file_line, "height");
					loc_element = city;
				}
			}

			// Close file
			dis.close();

		} catch (FileNotFoundException e1)
		{
			throw new JPARSECException("cities file not found in path jparsec/observer/cities.txt.", e1);
		} catch (IOException e2)
		{
			throw new JPARSECException(
					"error while reading cities file.", e2);
		}


		ArrayList<String> file = ReadFile.readResource("jparsec/observer/SpainCities.txt");
		for (int i = 0; i < file.size(); i = i + 2) {
			String li = file.get(i), lip = file.get(i + 1);
			String n = li.substring(0, 48).trim();
			String lats = lip.substring(0, 21).trim();
			String lons = (lip.substring(21, 22) + lip.substring(23, 31)).trim();
			String alt = lip.substring(42).trim();
			double lo = Functions.parseDeclination(lons);
			double la = Functions.parseDeclination(lats);
			LocationElement loc1 = new LocationElement(lo, la, 1.0);
			double r = Math.abs(LocationElement.getLinearDistance(loc, loc1));
			if (r < distance) {
				distance = r;
				double lat = la * Constant.RAD_TO_DEG;
				loc_element = new CityElement(n, lo * Constant.RAD_TO_DEG, lat,
						1.0, Integer.parseInt(alt));
				if (lat < 32)
					loc_element.timeZone = 0;
				loc_element.country = "Spain";
			}
		}

		if (loc_element == null)
			throw new JPARSECException("city not found.");

		return (loc_element);
	}

	/**
	 * Find the nearest city to certain coordinates.
	 *
	 * @param loc The approximate coordinates of the city.
	 * @param country The country where this position is located. Maybe null to search in all
	 * countries, but in this case the special file with the detailed locations
	 * in Spain will not be read.
	 * @param radius The radius of the search in radians, for instance 10 degrees (in radians).
	 * @return The object CityElement containing the city that best matches the search, or null
	 * if no one can be found within the radius.
	 * @throws JPARSECException Thrown if an error occurs.
	 */
	public static CityElement findNearestCity(LocationElement loc, COUNTRY country, double radius)
			throws JPARSECException {
		CityElement loc_element = null;
		double distance = 2.0;

		String c = "";
		try {
			c = country.toString();
		} catch (Exception exc) {
		}

		if (country == null) {
			for (int i = COUNTRY.Afghanistan.ordinal(); i <= COUNTRY.Zimbabwe.ordinal(); i++) {
				CityElement[] loc_element_city = jparsec.observer.City.getCities(COUNTRY.values()[i]);
				if (loc_element_city == null || loc_element_city.length == 0) continue;

				try {
					for (int j = 0; j < loc_element_city.length; j++) {
						CityElement city = loc_element_city[j];
						LocationElement loc1 = LocationElement.parseCity(city);
						loc1.setRadius(1.0);
						loc.setRadius(1.0);
						double distance1 = Math.abs(LocationElement
								.getLinearDistance(loc, loc1));
						if (distance1 < distance) {
							distance = distance1;
							loc_element = city;
							loc_element.country = c;
						}
					}
				} catch (NullPointerException e) {
				}
			}

			//if (loc_element.country != COUNTRY.Spain.toString())
				return loc_element;
			//country = COUNTRY.Spain;
		} else {
			CityElement[] loc_element_city = jparsec.observer.City.getCities(country);

			try {
				for (int j = 0; j < loc_element_city.length; j++) {
					CityElement city = loc_element_city[j];
					LocationElement loc1 = LocationElement.parseCity(city);
					loc1.setRadius(1.0);
					loc.setRadius(1.0);
					double distance1 = Math.abs(LocationElement
							.getLinearDistance(loc, loc1));
					if (distance1 < distance) {
						distance = distance1;
						loc_element = city;
						loc_element.country = c;
					}
				}
			} catch (NullPointerException e) {
			}
		}

		if (country != null && country == COUNTRY.Spain) {
			ArrayList<String> file = ReadFile.readResource("jparsec/observer/SpainCities.txt");
			for (int i = 0; i < file.size(); i = i + 2) {
				String li = file.get(i), lip = file.get(i + 1);
				String n = li.substring(0, 48).trim();
				String lats = lip.substring(0, 21).trim();
				String lons = (lip.substring(21, 22) + lip.substring(23, 31)).trim();
				String alt = lip.substring(42).trim();
				double lo = Functions.parseDeclination(lons);
				double la = Functions.parseDeclination(lats);
				LocationElement loc1 = new LocationElement(lo, la, 1.0);
				double r = Math.abs(LocationElement.getLinearDistance(loc, loc1));
				if (r < distance) {
					distance = r;
					double lat = la * Constant.RAD_TO_DEG;
					loc_element = new CityElement(n, lo * Constant.RAD_TO_DEG, lat,
							1.0, Integer.parseInt(alt));
					if (lat < 32)
						loc_element.timeZone = 0;
					loc_element.country = "Spain";
				}
			}
		}

		if (distance > radius) return null;
		return (loc_element);
	}

	/**
	 * Find the country where a given city is in.
	 *
	 * @param city_location The name of the city or representative part of it's name.
	 * @return The ID constant of the country.
	 * @throws JPARSECException Thrown if the city is not found.
	 */
	public static COUNTRY findCountry(String city_location) throws JPARSECException {
		COUNTRY result = null;

		CityElement[] loc_element_city = jparsec.observer.City.getCities(city_location);
		if (loc_element_city != null) {
			for (int j = 0; j < loc_element_city.length; j++) {
					result = Country.getID(loc_element_city[j].country);
					if (loc_element_city[j].name.toUpperCase().equals(
							city_location.toUpperCase()))
						return result;
			}
		}

		if (result == null) {
			String record = city_location.toLowerCase();
			record = record.substring(0, 1).toUpperCase() + record.substring(1);
			ArrayList<String> file = readResourceContainingOnlyForSpanishCities("jparsec/observer/SpainCities.txt", ReadFile.ENCODING_ISO_8859, record);
			//ArrayList<String> file = ReadFile.readResource("jparsec/observer/SpainCities.txt");
			for (int i = 0; i < file.size(); i = i + 2) {
				String n = file.get(i).substring(0, 48).trim();
				
				boolean equalLoc = n.toUpperCase().equals(city_location.toUpperCase());
				if (!equalLoc && n.indexOf("/") > 0) {
					String names[] = DataSet.toStringArray(n, "/", false);
					for (int j=0; j<names.length; j++) {
						if (names[j].toUpperCase().equals(city_location.toUpperCase())) {
							equalLoc = true;
							break;
						}
					}
				}
				if (equalLoc) {
					result = COUNTRY.Spain;
					break;
				}
			}
		}

		if (result == null)
			throw new JPARSECException("city not found.");

		return (result);
	}

	/**
	 * Reads a resource and returns it contents as an array of strings. Path is taken from
	 * the current established path in this instance, and this path should be a local resource
	 * available. Only lines containing a given record are returned.
	 *
	 * @param record Something to search for in each line of the file.
	 * @return Array of strings.
	 * @throws JPARSECException If an error occurs accessing the resource.
	 */
	private static ArrayList<String> readResourceContainingOnlyForSpanishCities(String pathToFile, String encoding, String record) throws JPARSECException
	{
		// Define necessary variables
		ArrayList<String> vec = new ArrayList<String>();
		String file_line = "";

		// Connect to the file
		record = record.toLowerCase();
		try
		{
			InputStream is = City.class.getClassLoader().getResourceAsStream(pathToFile);
			BufferedReader dis = new BufferedReader(new InputStreamReader(is, encoding));

			while ((file_line = dis.readLine()) != null)
			{
				// Store object in ArrayList
				if (file_line.toLowerCase().indexOf(record) >= 0) {
					vec.add(file_line);
					vec.add(dis.readLine());
				}
			}

			// Close file
			dis.close();

			return vec;
		} catch (FileNotFoundException e1)
		{
			throw new JPARSECException("file not found in path " + pathToFile+".", e1);
		} catch (IOException e2)
		{
			throw new JPARSECException(
					"error while reading file " + pathToFile + ".", e2);
		}
	}
}
