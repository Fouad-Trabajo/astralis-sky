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
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import jparsec.io.FileFormatElement;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.io.ReadFormat;
import jparsec.observer.Country.COUNTRY;
import jparsec.observer.ReferenceEllipsoid.ELLIPSOID;
import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Logger.LEVEL;

/**
 * A convenient class for observatory data management. There are two main
 * databases: the one from Sveshnikov, and the official Marsden list. The second
 * one needs an external file located in a dependency.
 * <P>
 * This data from Sveshnikov is the extension of Marsden's list of observatories
 * published in Minor Planet Circulars. Additional data about observatories
 * were taken from other sources to complete a list of 1200 observatories.
 * <P>
 * <I><B>Description of Sveshnikov catalog</B></I>
 * <P>
 * ATTENTION! These coordinates may be used for reduction of 
 * observations with standard accuracy. For reduction of high accuracy
 * observations (CCD, meridian obs., NEO objects) should be used more exact
 * coordinates of instrument.
 * <P>
 * Code / Reference
 * <P>
 * 1. list of MPC: nn.25665-25670, 25993, 26206, 26439, 26781, etc. (list of
 * Marsden)<BR>
 * 2. "The Astronomical Year-Book for the year 1942",1941, p. 355-360, in
 * Russian<BR>
 * 3. "The Astronomical Ephemeris for the year 1980", 1979, p. 472-496<BR>
 * 4. "The Astronomical Almanac for the year 2002", 2000, p. J1-J18<BR>
 * 5. "Connaissance des Temps pour l'an 1912", 1910, p.1*-104*<BR>
 * 6. "The Nautical Almanac for the year 1933", 1932<BR>
 * 7. "The Astronomical Ephemeris for the year 1974", 1973, p. 486-501<BR>
 * 8. D.A.O-Handley, Technical Report 32-1296, NASA, JPL, 1968, p. 2-3<BR>
 * 9. Various ref.: (Berliner AJ, 1949), (Observ. Astr., 1931) etc.
 * <P>
 * <I><B>Format</B></I>
 * <P>
 * Data is organized in arrays of type {@linkplain ObservatoryElement} for each country,
 * containing data objects with the convenient fields. Note that no information
 * about time zone offset exists, so it is set a value of zero as default. So it
 * is not recommended to use local time in ephemeris calculations as time scale.
 * <P>
 * <I><B>Reference</B></I>
 * <P>
 * This data is from M. L. Sveshnikov, IAA RAS. For details, see <A
 * target="_blank" href =
 * "http://www.ipa.nw.ru/PAGE/EDITION/RUS/AE/comment.txt"> this page</A>.
 * <P>
 * It is hard to maintain this list completely updated. Some errors seems to
 * exist, and at this time I have some observatories/instruments, like 291, 703,
 * 678, which are not identified.
 * <P>
 * Some other corrections made by T. Alonso Albi. Mainly names of certain
 * countries, like 'Madagasgar' (Madagascar), 'Tadzhikistan' (Tajikistan),
 * 'Malay' (Singapore), 'Ceylon' (Sri Lanka), 'Salvador' (El Salvador),
 * 'Yugoslavia' (now Serbia) and Korea (now Korea of North and Korea of South).
 * Political changes updated to 2011. I have changed also the location
 * 'Youngchum' to 'Youngchun', in Korea.
 * <P>
 * IMPORTANT: Perhaps some of the methods should be deprecated, since it is hard
 * to update the list. Otherwise, support for the official Marsden list
 * published in MPC is included also. In the Marsden list the reference field of
 * the {@linkplain ObservatoryElement} object is set equal to the original Marsden code,
 * while field code is set to it's integer representation. Note that the
 * position accuracy of the observatories in the Marsden list is not very good,
 * since 5 decimal places (only 4 in Teide observatory!!!) in the parallax
 * constants rho.sin(phi) and rho.cos(phi) can give errors above 200 m in the
 * elevation.
 * <P>
 * For this reason, the Sveshnikov data is still recommended when possible.
 *
 * @see ObservatoryElement
 * @see Country
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public final class Observatory
{
	// private constructor so that this class cannot be instantiated.
	private Observatory() {}

	/**
	 * Returns the number of observatories by Sveshnikov in the database.
	 * @return Number of cities, currently 1215.
	 * @throws JPARSECException If an error occurs reading the observatories file.
	 */
	public static int getNumberOfObservatories() throws JPARSECException {
		int n = 0;

		// Connect to the file
		try
		{
			InputStream is = City.class.getClassLoader().getResourceAsStream("jparsec/observer/observatories.txt");
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
	 * Get the observatories for certain country.
	 *
	 * @param country_ID The ID constant of the country whose observatories are required.
	 * @return The array ObservatoryElement[] containing observatories for the
	 *         selected country, null if none is found.
	 * @throws JPARSECException If an error occurs reading the observatories file.
	 */
	public static final ObservatoryElement[] getObservatoriesByCountry(COUNTRY country_ID)
	throws JPARSECException
	{
		// Define necessary variables
		ArrayList<ObservatoryElement> vec = new ArrayList<ObservatoryElement>();
		String file_line = "";

		FileFormatElement[] format = new FileFormatElement[] {
				new FileFormatElement(1, 70, "name"),
				new FileFormatElement(71, 120, "country"),
				new FileFormatElement(121, 130, "longitude"),
				new FileFormatElement(131, 139, "latitude"),
				new FileFormatElement(140, 145, "code"),
				new FileFormatElement(146, 150, "height"),
				new FileFormatElement(151, 180, "location"),
				new FileFormatElement(181, 183, "reference")
		};
		ReadFormat rf = new ReadFormat(format);

		// Connect to the file
		try
		{
			InputStream is = City.class.getClassLoader().getResourceAsStream("jparsec/observer/observatories.txt");
			BufferedReader dis = new BufferedReader(new InputStreamReader(is, ReadFile.ENCODING_ISO_8859));

			while ((file_line = dis.readLine()) != null)
			{
				ObservatoryElement observatory = new ObservatoryElement();
				observatory.country = rf.readString(file_line, "country");
				if (observatory.country.equals(country_ID.toString())) {
					observatory.name = rf.readString(file_line, "name");
					observatory.longitude = rf.readDouble(file_line, "longitude");
					observatory.latitude = rf.readDouble(file_line, "latitude");
					observatory.code = rf.readInteger(file_line, "code");
					observatory.height = rf.readInteger(file_line, "height");
					observatory.location = rf.readString(file_line, "location");
					observatory.reference = rf.readString(file_line, "reference");
					vec.add(observatory);
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

		ObservatoryElement loc_element[] = new ObservatoryElement[vec.size()];

		for (int i = 0; i < loc_element.length; i++) {
			loc_element[i] = vec.get(i);
		}

		return (loc_element);
	}

	/**
	 * Get all observatories from Sveshnikov, not from Marsden list.
	 *
	 * @return The array containing all the observatories.
	 * @throws JPARSECException If an error occurs reading the observatories file.
	 */
	public static ObservatoryElement[] getAllObservatories() throws JPARSECException
	{
		ObservatoryElement[] loc_element = new ObservatoryElement[getNumberOfObservatories()];

		// Define necessary variables
		String file_line = "";
		int index = -1;

		FileFormatElement[] format = new FileFormatElement[] {
				new FileFormatElement(1, 70, "name"),
				new FileFormatElement(71, 120, "country"),
				new FileFormatElement(121, 130, "longitude"),
				new FileFormatElement(131, 139, "latitude"),
				new FileFormatElement(140, 145, "code"),
				new FileFormatElement(146, 150, "height"),
				new FileFormatElement(151, 180, "location"),
				new FileFormatElement(181, 183, "reference")
		};
		ReadFormat rf = new ReadFormat(format);

		// Connect to the file
		try
		{
			InputStream is = City.class.getClassLoader().getResourceAsStream("jparsec/observer/observatories.txt");
			BufferedReader dis = new BufferedReader(new InputStreamReader(is, ReadFile.ENCODING_ISO_8859));

			while ((file_line = dis.readLine()) != null)
			{
				ObservatoryElement observatory = new ObservatoryElement();
				observatory.country = rf.readString(file_line, "country");
				observatory.name = rf.readString(file_line, "name");
				observatory.longitude = rf.readDouble(file_line, "longitude");
				observatory.latitude = rf.readDouble(file_line, "latitude");
				observatory.code = rf.readInteger(file_line, "code");
				observatory.height = rf.readInteger(file_line, "height");
				observatory.location = rf.readString(file_line, "location");
				observatory.reference = rf.readString(file_line, "reference");

				index ++;
				loc_element[index] = observatory;
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

		return loc_element;
	}

	/**
	 * Find one observatory given the name of the place where it is located or
	 * part of it.
	 *
	 * @param obs_location The full/partial name of the mountain/city where the
	 *        observatory is located.
	 * @return The object ObservatoryElement containing the observatory if it is found.
	 * @throws JPARSECException Thrown if the observatory is not found.
	 */
	public static ObservatoryElement findObservatorybyName(String obs_location) throws JPARSECException
	{
		ObservatoryElement loc_element = null;

		int nfound = 0;
		for (int i = COUNTRY.Afghanistan.ordinal(); i <= COUNTRY.Zimbabwe.ordinal(); i++)
		{
			ObservatoryElement[] loc_element_obs = jparsec.observer.Observatory.getObservatoriesByCountry(COUNTRY.values()[i]);

			if (loc_element_obs != null) {
				try
				{
					for (int j = 0; j < loc_element_obs.length; j++)
					{
						if ((loc_element_obs[j].location.toUpperCase().equals(obs_location.toUpperCase()) || loc_element_obs[j].name
								.toUpperCase().equals(obs_location.toUpperCase())) && nfound > 0)
							throw new JPARSECException("found 2 observatories with the same name '"+obs_location+"'.");

						if (loc_element_obs[j].location.toUpperCase().indexOf(obs_location.toUpperCase()) >= 0 || loc_element_obs[j].name
								.toUpperCase().indexOf(obs_location.toUpperCase()) >= 0)
						{
							loc_element = loc_element_obs[j];
							if (loc_element_obs[j].location.toUpperCase().equals(obs_location.toUpperCase()) || loc_element_obs[j].name
									.toUpperCase().equals(obs_location.toUpperCase()))
								nfound ++;
						}
					}
				} catch (NullPointerException e)
				{
				}
			}
		}

		if (loc_element == null)
			throw new JPARSECException("observatory not found.");

		return (loc_element);
	}

	/**
	 * Find one observatory given the name of the place where it is located or
	 * part of it, and its country.
	 *
	 * @param obs_location The full/partial name of the mountain/city where the
	 *        observatory is located.
	 * @param country The country the desired location belongs to.
	 * @return The object ObservatoryElement containing the observatory if it is found.
	 * @throws JPARSECException Thrown if the observatory is not found.
	 */
	public static ObservatoryElement findObservatorybyName(String obs_location, COUNTRY country) throws JPARSECException
	{
		ObservatoryElement loc_element = null;

		int nfound = 0;
		int i = country.ordinal();
			ObservatoryElement[] loc_element_obs = jparsec.observer.Observatory.getObservatoriesByCountry(COUNTRY.values()[i]);

			if (loc_element_obs != null) {
				try
				{
					for (int j = 0; j < loc_element_obs.length; j++)
					{
						if ((loc_element_obs[j].location.toUpperCase().equals(obs_location.toUpperCase()) || loc_element_obs[j].name
								.toUpperCase().equals(obs_location.toUpperCase())) && nfound > 0)
							throw new JPARSECException("found 2 observatories with the same name '"+obs_location+"'.");

						if (loc_element_obs[j].location.toUpperCase().indexOf(obs_location.toUpperCase()) >= 0 || loc_element_obs[j].name
								.toUpperCase().indexOf(obs_location.toUpperCase()) >= 0)
						{
							loc_element = loc_element_obs[j];
							if (loc_element_obs[j].location.toUpperCase().equals(obs_location.toUpperCase()) || loc_element_obs[j].name
									.toUpperCase().equals(obs_location.toUpperCase()))
								nfound ++;
						}
					}
				} catch (NullPointerException e)
				{
				}
			}


		if (loc_element == null)
			throw new JPARSECException("observatory not found.");

		return (loc_element);
	}

	/**
	 * Search for an observatory by it's geographical position.
	 *
	 * @param loc Approximate position of the observatory.
	 * @return Closest observatory.
	 * @throws JPARSECException If an error occurs reading the observatories file.
	 */
	public static ObservatoryElement findObservatoryByPosition(LocationElement loc) throws JPARSECException
	{
		// Search object
		double distance = Double.MAX_VALUE;

		// Define necessary variables
		String file_line = "";

		FileFormatElement[] format = new FileFormatElement[] {
				new FileFormatElement(1, 70, "name"),
				new FileFormatElement(71, 120, "country"),
				new FileFormatElement(121, 130, "longitude"),
				new FileFormatElement(131, 139, "latitude"),
				new FileFormatElement(140, 145, "code"),
				new FileFormatElement(146, 150, "height"),
				new FileFormatElement(151, 180, "location"),
				new FileFormatElement(181, 183, "reference")
		};
		ReadFormat rf = new ReadFormat(format);
		ObservatoryElement out = null;

		// Connect to the file
		try
		{
			InputStream is = City.class.getClassLoader().getResourceAsStream("jparsec/observer/observatories.txt");
			BufferedReader dis = new BufferedReader(new InputStreamReader(is, ReadFile.ENCODING_ISO_8859));

			while ((file_line = dis.readLine()) != null)
			{
				ObservatoryElement observatory = new ObservatoryElement();
				observatory.longitude = rf.readDouble(file_line, "longitude");
				observatory.latitude = rf.readDouble(file_line, "latitude");

				LocationElement loc1 = LocationElement.parseObservatory(observatory);
				double distance1 = Math.abs(LocationElement.getAngularDistance(loc, loc1));
				if (distance1 < distance)
				{
					observatory.country = rf.readString(file_line, "country");
					observatory.name = rf.readString(file_line, "name");
					observatory.code = rf.readInteger(file_line, "code");
					observatory.height = rf.readInteger(file_line, "height");
					observatory.location = rf.readString(file_line, "location");
					observatory.reference = rf.readString(file_line, "reference");

					distance = distance1;
					out = observatory;
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

		return out;
	}

	/**
	 * Find the country where a given observatory is in.
	 *
	 * @param obs_location The full/partial name of the mountain/city where the
	 *        observatory is located.
	 * @return The ID constant of the country, or null if no match is found.
	 * @throws JPARSECException Thrown if the country cannot be found, and the
	 *         warnings are to be treated as errors.
	 */
	public static COUNTRY findCountry(String obs_location) throws JPARSECException
	{
		COUNTRY result = null;

		for (int i = COUNTRY.Afghanistan.ordinal(); i <= COUNTRY.Zimbabwe.ordinal(); i++)
		{
			ObservatoryElement[] loc_element_obs = jparsec.observer.Observatory.getObservatoriesByCountry(COUNTRY.values()[i]);

			if (loc_element_obs != null) {
				try
				{
					for (int j = 0; j < loc_element_obs.length; j++)
					{
						if (loc_element_obs[j].location.toUpperCase().indexOf(obs_location.toUpperCase()) >= 0 || loc_element_obs[j].name
								.toUpperCase().indexOf(obs_location.toUpperCase()) >= 0)
						{
							result = COUNTRY.values()[i];
							if (loc_element_obs[j].location.toUpperCase().equals(obs_location.toUpperCase()) || loc_element_obs[j].name
									.toUpperCase().equals(obs_location.toUpperCase()))
								break;
						}
					}
				} catch (NullPointerException e)
				{
				}
			}
		}

		if (result == null)
			JPARSECException
					.addWarning("country not found for observatory " + obs_location + ". Daylight Saving Time information could be wrong.");

		return (result);
	}

	/**
	 * Find one observatory given the id code of B. Marsden. Note this method
	 * search for an integer code corresponding to the observatory in the database
	 * contained in JPARSEC (the old catalog by Sveshnikov). Current Marsden
	 * list contains non-integer codes, use the corresponding method to search
	 * in the updated list.
	 *
	 * @param code ID value for this observatory.
	 * @return The object ObservatoryElement containing the observatory if it is found.
	 * @throws JPARSECException Thrown if the observatory is not found.
	 * @deprecated The method {@linkplain #searchByCodeInMarsdenList(String)} should
	 * be used instead.
	 */
	public static ObservatoryElement findObservatorybyCode(int code) throws JPARSECException
	{
		ObservatoryElement loc_element = null;

		for (int i = COUNTRY.Afghanistan.ordinal(); i <= COUNTRY.Zimbabwe.ordinal(); i++)
		{
			ObservatoryElement[] loc_element_obs = jparsec.observer.Observatory.getObservatoriesByCountry(COUNTRY.values()[i]);

			if (loc_element_obs != null) {
				try
				{
					for (int j = 0; j < loc_element_obs.length; j++)
					{
						if (loc_element_obs[j].code == code)
						{
							loc_element = loc_element_obs[j];
							break;
						}
					}
				} catch (NullPointerException e)
				{
				}
			}
		}

		if (loc_element == null)
			throw new JPARSECException("observatory not found.");

		return (loc_element);
	}

	/*
	 * START OF NEW METHODS FOR THE OFFICIAL MPC LIST
	 */

	/**
	 * Holds the official list of observatories defined by Marsden.
	 * Note this array is static and not thread-safe, although this
	 * should not be a problem.
	 */
	public static ArrayList<ObservatoryElement> observatories;

	/**
	 * Path to the file of observatories, including extension.
	 */
	public static final String PATH_TO_OFFICIAL_LIST_OF_OBSERVATORIES_FROM_MPC = FileIO.DATA_ORBITAL_ELEMENTS_DIRECTORY + "MPC_observatory.txt";

	/**
	 * Path to an external file with observatories in the MPC format.
	 * This is optional, since JPARSEC contains an internal catalog from MPC.
	 */
	public static String pathToFile;

	/**
	 * Sets the path to the file of observatories.
	 *
	 * @param path Full path including extension.
	 */
	public static void setPath(String path)
	{
		pathToFile = path;
	}

	/**
	 * Reads an example file with the observatories formatted in the standard
	 * way, established by the Minor Planet Center. This method should be the
	 * first to be called.
	 * <P>
	 * An example of MPC format is:
	 * <P>
	 * <pre>
	 * 000   0.0000 0.62411 +0.77873 Greenwich
	 * <P>
	 * </pre>
	 *
	 * After the file is successfully read, the elements are store in
	 * {@linkplain Observatory#observatories} vector. Note this array is
	 * static and not thread-safe.
	 *
	 * @throws JPARSECException Thrown if the method fails.
	 */
	private static void readFileOfObservatories() throws JPARSECException
	{
		// Define necesary variables
		ArrayList<ObservatoryElement>vec = new ArrayList<ObservatoryElement>();
		String file_line = "";

		// Connect to the file
		try
		{
			InputStream is = Observatory.class.getClassLoader().getResourceAsStream(Observatory.PATH_TO_OFFICIAL_LIST_OF_OBSERVATORIES_FROM_MPC);
			BufferedReader dis = new BufferedReader(new InputStreamReader(is, ReadFile.ENCODING_ISO_8859));

			while ((file_line = dis.readLine()) != null)
			{

				// Obtain object
				ObservatoryElement obs = parseObservatoryFromMarsdenList(file_line);

				// Store object in Vector
				if (obs != null) vec.add(obs);
			}

			// Close file
			dis.close();

			observatories = vec;
		} catch (FileNotFoundException e1)
		{
			throw new JPARSECException("file not found in path " + PATH_TO_OFFICIAL_LIST_OF_OBSERVATORIES_FROM_MPC+".", e1);
		} catch (IOException e2)
		{
			throw new JPARSECException(e2);
		}

	}

	/**
	 * Reads an external file with the observatories formatted in the standard
	 * way, established by the Minor Planet Center. This method should be the
	 * first to be called.
	 * <BR>
	 * An example of MPC format is:
	 * <pre>
	 * <BR>
	 * 000   0.0000 0.62411 +0.77873 Greenwich
	 * <BR>
	 * </pre>
	 *
	 * After the file is successfully read, the elements are store in
	 * {@linkplain Observatory#observatories} vector. Note this array is static
	 * and not thread-safe.
	 *
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public static void readFileOfObservatoriesFromExternalFile() throws JPARSECException
	{
		// Define necesary variables
		ArrayList<ObservatoryElement> vec = new ArrayList<ObservatoryElement>();
		String file_line = "";

		// Connect to the file
		try
		{
			URLConnection Connection = (URLConnection) (new URL("file:" + pathToFile)).openConnection();
			InputStream is = Connection.getInputStream();
			BufferedReader dis = new BufferedReader(new InputStreamReader(is));

			while ((file_line = dis.readLine()) != null)
			{

				// Obtain object
				ObservatoryElement obs = parseObservatoryFromMarsdenList(file_line);

				// Store object in Vector
				if (obs != null) vec.add(obs);
			}

			// Close file
			dis.close();

			observatories = vec;
		} catch (FileNotFoundException e1)
		{
			throw new JPARSECException(
					"file not found in path " + PATH_TO_OFFICIAL_LIST_OF_OBSERVATORIES_FROM_MPC+".", e1);
		} catch (IOException e2)
		{
			throw new JPARSECException(e2);
		}

	}

	/**
	 * Creates an {@linkplain ObservatoryElement} object from a record of the file of
	 * observatories. This method uses the latest reference ellipsoid to
	 * transform from geocentric to geodetic coordinates.
	 *
	 * @param line Record to parse.
	 * @return {@linkplain ObservatoryElement} object, or null if the observatory is not
	 * in Earth.
	 * @throws JPARSECException Thrown if the method fails.
	 */
	public static ObservatoryElement parseObservatoryFromMarsdenList(String line) throws JPARSECException
	{
		FileFormatElement fmt[] =
		{ new FileFormatElement(1, 3, "CODE"), new FileFormatElement(5, 13, "LONGITUDE"), new FileFormatElement(14, 21, "COSINE"),
				new FileFormatElement(22, 30, "SINE"), new FileFormatElement(31, 100, "NAME") };

		ReadFormat rf = new ReadFormat();
		rf.setFormatToRead(fmt);

		// Transform observatory code to integer
		String code = rf.readString(line, "CODE");
		int obs_code = ObservatoryElement.getObservatoryCodeAsInt(code);

		// Read and transform coordinates
		ObserverElement obs = new ObserverElement();
		try {
			double cosine = rf.readDouble(line, "COSINE");
			double sine = rf.readDouble(line, "SINE");
			double lat = Math.atan2(sine, cosine);
			double lon = rf.readDoubleToRadians(line, "LONGITUDE");
			if (cosine == 0.0 && sine == 0.0 && lon == 0.0) throw new Exception("Invalid observatory");
			double rho = Math.sqrt(sine * sine + cosine * cosine);
			LocationElement geodeticLoc = ObserverElement.geocentricToGeodetic(ELLIPSOID.LATEST, lon, lat, rho);
			obs.setLongitudeRad(geodeticLoc.getLongitude());
			obs.setLatitudeRad(geodeticLoc.getLatitude());
			obs.setHeight((int) geodeticLoc.getRadius(), true);
		} catch (Exception exc) {
			return null;
		}

		return new ObservatoryElement(obs_code, "", rf.readString(line, "NAME"), "", obs.getLongitudeDeg(), obs.getLatitudeDeg(), obs.getHeight(), code);
	}

	/**
	 * Obtain number of observatories in the Marsden list.
	 *
	 * @return Number of observatories.
	 */
	public static int getNumberOfObservatoriesInMarsdenList()
	{
		if (observatories == null) {
			try {
				Observatory.readFileOfObservatories();
			} catch (JPARSECException e) {
				Logger.log(LEVEL.ERROR, "Could not read the file of observatories.");
			}
		}

		return observatories.size();
	}

	/**
	 * Obtains one observatory.
	 *
	 * @param index ID value for the observatory. From 0 to
	 *        {@linkplain Observatory#getNumberOfObservatoriesInMarsdenList()}-1.
	 * @return The ObservatoryElement object.
	 * @throws JPARSECException If the index is out of range.
	 */
	public static ObservatoryElement getObservatoryFromMarsdenList(int index) throws JPARSECException
	{
		if (observatories == null) {
			try {
				Observatory.readFileOfObservatories();
			} catch (JPARSECException e) {
				Logger.log(LEVEL.ERROR, "Could not read the file of observatories.");
			}
		}

		if (index < 0 || index >= getNumberOfObservatoriesInMarsdenList())
			throw new JPARSECException("Index out of range 0-"+(getNumberOfObservatoriesInMarsdenList()-1));

		ObservatoryElement obs = observatories.get(index);

		return obs;
	}

	/**
	 * Search for an observatory by it's name.
	 *
	 * @param observatory Name of the observatory to seach for.
	 * @return index value of the observatory. -1 is returned if no match is found.
	 */
	public static int searchByNameInMarsdenList(String observatory)
	{
		if (observatories == null) {
			try {
				Observatory.readFileOfObservatories();
			} catch (JPARSECException e) {
				Logger.log(LEVEL.ERROR, "Could not read the file of observatories.");
			}
		}

		// Search object
		int index = -1;
		for (int i = 0; i < observatories.size(); i++)
		{
			ObservatoryElement obs = observatories.get(i);
			if (obs.name.indexOf(observatory) >= 0)
			{
				index = i;
			}
			if (obs.name.equals(observatory))
			{
				index = i;
				break;
			}
		}

		return index;

	}

	/**
	 * Search for an observatory by it's geographical position in the Marsden list.
	 *
	 * @param loc Approximate position of the observatory.
	 * @return index value of the observatory. -1 is returned if no match is found.
	 */
	public static int searchByPositionInMarsdenList(LocationElement loc)
	{
		if (observatories == null) {
			try {
				Observatory.readFileOfObservatories();
			} catch (JPARSECException e) {
				Logger.log(LEVEL.ERROR, "Could not read the file of observatories.");
			}
		}

		// Search object
		double distance = Double.MAX_VALUE;
		int index = -1;
		for (int i = 0; i < observatories.size(); i++)
		{
			ObservatoryElement obs = observatories.get(i);
			LocationElement loc1 = LocationElement.parseObservatory(obs);
			double distance1 = Math.abs(LocationElement.getAngularDistance(loc, loc1));
			if (distance1 < distance)
			{
				distance = distance1;
				index = i;
			}
		}

		return index;
	}

	/**
	 * Search for an observatory by it's Marsden code. The code is taken from
	 * field reference, since field code is it's integer transformation and not
	 * officially used.
	 *
	 * @param code Name of the observatory to seach for.
	 * @return index value of the observatory. -1 is returned if no match is found.
	 */
	public static int searchByCodeInMarsdenList(String code)
	{
		if (observatories == null) {
			try {
				Observatory.readFileOfObservatories();
			} catch (JPARSECException e) {
				Logger.log(LEVEL.ERROR, "Could not read the file of observatories.");
			}
		}

		// Search object
		int index = -1;
		for (int i = 0; i < observatories.size(); i++)
		{
			ObservatoryElement obs = observatories.get(i);
			if (obs.reference.indexOf(code) >= 0)
			{
				index = i;
			}
			if (obs.reference.equals(code))
			{
				index = i;
				break;
			}
		}

		return index;
	}

	/**
	 * Gets the name of an observatory.
	 *
	 * @param index Index for the observatory.
	 * @return Name of the observatory.
	 * @throws JPARSECException If the index is out of range.
	 */
	public static String getObservatoryNameInMarsdenList(int index) throws JPARSECException
	{
		if (observatories == null) {
			try {
				Observatory.readFileOfObservatories();
			} catch (JPARSECException e) {
				Logger.log(LEVEL.ERROR, "Could not read the file of observatories.");
			}
		}

		if (index < 0 || index >= getNumberOfObservatoriesInMarsdenList())
			throw new JPARSECException("Index out of range 0-"+(getNumberOfObservatoriesInMarsdenList()-1));

		// Obtain object
		ObservatoryElement obs = observatories.get(index);

		return obs.name;
	}
}
