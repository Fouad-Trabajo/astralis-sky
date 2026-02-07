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
package jparsec.io;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


import jparsec.graph.DataSet;
import jparsec.util.Configuration;
import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Logger.LEVEL;

/**
 * Miscellaneous utility functions for file I/O.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class FileIO
{
	// private constructor so that this class cannot be instantiated.
	private FileIO() {}

	/**
	 * Obtains path of the working directory.
	 *
	 * @param clean True to remove file:/ from the
	 * beginning of the output string. True recommended
	 * since it depends on the operating system.
	 * @return Full path.
	 */
	public static String getPath(boolean clean)
	{
		if (Configuration.FORCE_JPARSEC_LIB_DIRECTORY != null) {
			String p = Configuration.FORCE_JPARSEC_LIB_DIRECTORY;
			if (!p.endsWith(FileIO.getFileSeparator())) p += FileIO.getFileSeparator();
			return p;
		}

		URL url = FileIO.class.getClassLoader().getResource(LIBRARY_ROOT_NAME + "/io/package.html");
		if (url == null) return null;
		String path = getPathFromURL(url.toString());
		if (clean) {
			try {
				path = new URL(path).getFile();
				path = DataSet.replaceAll(path, "%20", " ", true);
			} catch (Exception exc) {
				Logger.log(LEVEL.ERROR, "Could not clean the url/path "+path);
			}
		}

		return path;
	}

	/**
	 * Returns the path from a given URL.
	 * @param path The URL as a String.
	 * @return The path as a String.
	 */
	public static String getPathFromURL(String path)
	{
		int a = path.indexOf("file:");
		int b = path.indexOf(".jar");
		if (a < 0) a = 0;
		if (b < 0) b = path.length();
		path = path.substring(a, b);
		b = path.lastIndexOf("/") + 1;
		path = path.substring(0, b);
		if (path.startsWith("jar:")) path = path.substring(4);
		return path;
	}

	/**
	 * Adds spaces to a string to get certain length, after the text.
	 *
	 * @param field Text.
	 * @param length Desired length.
	 * @return New string with the necessary spaces after the text.
	 */
	public static String addSpacesAfterAString(String field, int length)
	{
		int a = length - field.length();
		String post = "";
		if (a > 0)
		{
			for (int i = 0; i < a; i++)
			{
				post += " ";
			}
		}
		return field + post;
	}

	/**
	 * Adds spaces to a string to get certain length, before the text.
	 *
	 * @param field Text.
	 * @param length Desired length.
	 * @return New string with the necessary spaces before the text.
	 */
	public static String addSpacesBeforeAString(String field, int length)
	{
		int a = length - field.length();
		String pre = "";
		if (a > 0)
		{
			for (int i = 0; i < a; i++)
			{
				pre += " ";
			}
		}
		return pre + field;
	}

	/**
	 * Get rest of the string after something.
	 *
	 * @param line_file Input string.
	 * @param what The text just before what you want.
	 * @return Output String, or empty String if fails.
	 */
	public static String getRestAfter(String line_file, String what)
	{
		int space;
		String out = "";

		if (line_file.equals(""))
		{
			space = 0;
		} else
		{
			space = line_file.indexOf(what);
			if (space >= 0)
				out = line_file.substring(space+what.length());
		}

		return out;
	}

	/**
	 * Obtains the name of a file from a given path.
	 * @param path The path.
	 * @return The file name.
	 */
	public static String getFileNameFromPath(String path)
	{
		  String name = path;
		  if (path.indexOf(FileIO.getFileSeparator()) >= 0)
			  name = path.substring(path.lastIndexOf(FileIO.getFileSeparator()) + 1);
		  return name;
	}

	/**
	 * Obtains the directory from a given path, including the latest file separator.
	 * @param path The path.
	 * @return The directory.
	 */
	public static String getDirectoryFromPath(String path)
	{
		  String name = "";
		  if (path.indexOf(FileIO.getFileSeparator()) >= 0)
			  name = path.substring(0, path.lastIndexOf(FileIO.getFileSeparator()) + 1);
		  return name;
	}

	/**
	 * Get text of the string before next two spaces.
	 *
	 * @param line_file Input string.
	 * @return Output String.
	 */
	public static String getTextBeforeNextSpace(String line_file)
	{
		int space;
		String out = "";

		if (line_file.equals(""))
		{
			space = 0;
		} else
		{
			space = line_file.indexOf("  ");
			if (space < 0)
			{
				out = line_file.trim();
			} else
			{
				out = line_file.substring(0, space).trim();
			}
		}

		return out;
	}

	/**
	 * To obtain a list of all files available in certain path. Files
	 * are usually sorted by name, but this is not guaranteed.
	 *
	 * @param path A directory path.
	 * @return List of files.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String[] getFiles(String path) throws JPARSECException
	{
		try
		{
			boolean isuri = false;
			if (path.startsWith("file:"))
				isuri = true;
			File f = null;
			if (isuri)
			{
				URI uri = new URI(path);
				f = new File(uri);
			} else
			{
				f = new File(path);
			}
			File ff[];
			if (f.isDirectory())
			{
				ff = f.listFiles();
			} else
			{
				return null;
			}

			int n = 0;
			for (int i = 0; i < ff.length; i++)
			{
				if (!ff[i].isDirectory())
					n++;
			}

			String files[] = new String[n];
			n = -1;
			for (int i = 0; i < ff.length; i++)
			{
				if (!ff[i].isDirectory())
				{
					n++;
					files[n] = ff[i].getPath();
				}
			}

			return files;
		} catch (Exception e)
		{
			throw new JPARSECException(e);
		}
	}

	/**
	 * To obtain a list of all files available in certain path,
	 * ordered by date.
	 *
	 * @param path A directory path.
	 * @param recentFilesFirst True to return the most recent file
	 * in the first index of the array, false to return the most recent
	 * file at the end of the array.
	 * @return List of files.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String[] getFilesByDate(String path, boolean recentFilesFirst) throws JPARSECException
	{
		String files[] = FileIO.getFiles(path);
		if (files == null) return null;

		double dates[] = new double[files.length];
		for (int i=0; i<files.length; i++) {
			File f = new File(files[i]);
			dates[i] = f.lastModified();
		}

		if (recentFilesFirst) {
			return DataSet.sortInDescent(files, dates);
		} else {
			return DataSet.sortInCrescent(files, dates);
		}
	}

	/**
	 * To obtain a list of all subdirectories available in certain path.
	 *
	 * @param path A directory path.
	 * @return List of subdirectories.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String[] getSubdirectories(String path) throws JPARSECException
	{
		try
		{
			boolean isuri = false;
			if (path.startsWith("file:"))
				isuri = true;
			File f = null;
			if (isuri)
			{
				URI uri = new URI(path);
				f = new File(uri);
			} else
			{
				f = new File(path);
			}
			File ff[];
			if (f.isDirectory())
			{
				ff = f.listFiles();
			} else
			{
				return null;
			}

			int n = 0;
			for (int i = 0; i < ff.length; i++)
			{
				if (ff[i].isDirectory())
					n++;
			}

			String files[] = new String[n];
			n = -1;
			for (int i = 0; i < ff.length; i++)
			{
				if (ff[i].isDirectory())
				{
					n++;
					files[n] = ff[i].getPath();
				}
			}

			return files;
		} catch (Exception e)
		{
			throw new JPARSECException(e);
		}
	}

	/**
	 * Get how many times an expression is repeated in an argument.
	 *
	 * @param arg Argument.
	 * @param expression Expression.
	 * @return Repeated times of expression in argument.
	 */
	public static int getRepeatedTimes(String arg, String expression)
	{
		int n = 0;
		for (int i = 0; i < arg.length(); i++)
		{
			int end = i + expression.length();
			if (end > arg.length()) break;

			String compare = arg.substring(i, end);
			if (compare.equals(expression))
				n++;
		}
		return n;
	}

	/**
	 * Get last field of a string. Fields are supposed to be separated by a
	 * string called separator.
	 *
	 * @param arg String with fields.
	 * @param separator String that defines the separator.
	 * @param skip True to skip several consecutive separators.
	 * @return The field in the desired position number.
	 */
	public static String getLastField(String arg, String separator, boolean skip)
	{
		int n = FileIO.getNumberOfFields(arg, separator, skip);
		return FileIO.getField(n, arg, separator, skip);
	}

	/**
	 * Get certain field of a string. Fields are supposed to be separated by a
	 * string called separator. If the separator is a blank space or tab several
	 * consecutive separators can be treated as one separator only if skip is
	 * set to true, which is the default value for other methods in the library.
	 *
	 * @param field Number of field. 1, 2, ...
	 * @param text String with fields.
	 * @param separator String that defines the separator.
	 * @param skip True to consider several consecutive separators as one only if separator
	 * is one or several blank spaces. False has 50% better performance in this case.
	 * @return The field in the desired position number.
	 */
	public static String getField(int field, String text, String separator, boolean skip)
	{
		if (text == null || field < 1) return null;
		if (!separator.trim().equals("")) skip = false;
		if (skip) text = text.trim();
		String myfield = "";

		int space;
		int err = 0;
		for (int i = 0; i < field; i++)
		{
			space = text.indexOf(separator);

			if (space >= 0)
			{
				myfield = text.substring(0, space);
				if (skip) myfield = myfield.trim();
				text = text.substring(space + separator.length());
				if (skip) text = text.trim();
			} else {
				space = text.length();
				myfield = text.substring(0, space);
				if (skip) myfield = myfield.trim();
				text = "";
			}

			if (space < 0) {
				err = 1;
				break;
			}
		}

		if (err == 1) myfield = null;

		return myfield;
	}

	/**
	 * Returns the number of fields in the argument.
	 * @param arg String with fields.
	 * @param separator String that defines the separator.
	 * @param skip True to skip several consecutive separators.
	 * @return The number of fields in the string.
	 */
	public static int getNumberOfFields(String arg, String separator, boolean skip)
	{
		// The indexOf approach is faster and more stable compared to intrinsic
		// String.split or StringTokenizer
		if (arg == null || arg.equals("")) return 0;
		if (!separator.trim().equals("")) skip = false;
		if (skip) arg = arg.trim();
		int n = 1;
		int space = arg.indexOf(separator) + 1;
		if (space <= 0) return n;
		do {
			n ++;
			arg = arg.substring(space);
			if (skip) arg = arg.trim();
			space = arg.indexOf(separator) + 1;
		} while (space > 0);
		return n;
	}

	/**
	 * Reads contents after certain field in a formatted line with fields
	 * separated by some separator.
	 *
	 * @param field Field number: 1, 2, ...
	 * @param text Formatted string
	 * @param separator Separator string.
	 * @param skip True to skip several consecutive separators.
	 * @return The desired field.
	 */
	public static String getRestAfterField(int field, String text, String separator, boolean skip)
	{
		if (field >= getNumberOfFields(text, separator, skip)) return "";
		int space;
		int err = 0;
		for (int i = 0; i < field; i++)
		{
			space = text.indexOf(separator) + 1;
			if (space <= 0)
				space = text.length();

			if (space > 0)
			{
				text = text.substring(space + separator.length() - 1);
				if (skip) text = text.trim();
			} else {
				err = 1;
				break;
			}
		}

		if (err == 1) text = "";

		return text;
	}

	/**
	 * Reads contents before certain field in a formatted line with fields
	 * separated by some separator.
	 *
	 * @param field Field number: 2, 3, ...
	 * @param text Formatted string
	 * @param separator Separator string.
	 * @param skip True to skip several consecutive separators.
	 * @return The desired fields before the given one.
	 */
	public static String getTextBeforeField(int field, String text, String separator, boolean skip)
	{
		if (field <= 1) return "";
		String t="";
		for (int i=1; i<field; i++) {
			t += FileIO.getField(i, text, separator, skip);
			if (i<field-1) t+= separator;
		}
		return t;
	}

	/**
	 * Get the system file separator ("/" or "\").
	 *
	 * @return "/" or "\".
	 */
	public static String getFileSeparator()
	{
		return System.getProperty("file.separator");
	}

	/**
	 * Get the system line separator.
	 *
	 * @return Line separator.
	 */
	public static String getLineSeparator()
	{
		return System.getProperty("line.separator");
	}

	/**
	 * Get the current working directory.
	 *
	 * @return The path.
	 */
	public static String getWorkingDirectory()
	{
		String wd = System.getProperty("user.dir");
		if (!wd.endsWith(FileIO.getFileSeparator())) wd += FileIO.getFileSeparator();
		return wd;
	}

	/**
	 * Get the OS current temporal directory.
	 *
	 * @return The path.
	 */
	public static String getTemporalDirectory()
	{
		if (Configuration.FORCE_TEMP_DIRECTORY != null) {
			String s = Configuration.FORCE_TEMP_DIRECTORY;
			if (!s.endsWith(FileIO.getFileSeparator())) s += FileIO.getFileSeparator();
			return s;
		}

		String s = System.getProperty("java.io.tmpdir");
		if (!s.endsWith(FileIO.getFileSeparator())) s += FileIO.getFileSeparator();
		return s;
	}

	/**
	 * Get the user name.
	 *
	 * @return The name.
	 */
	public static String getUserName()
	{
		return System.getProperty("user.name");
	}
	/**
	 * Get the user home directory.
	 *
	 * @return The path.
	 */
	public static String getUserHomeDirectory()
	{
		String home = System.getProperty("user.home");
		if (!home.endsWith(FileIO.getFileSeparator())) home += FileIO.getFileSeparator();
		return home;
	}

	/**
	 * Determine if the specified file exists and is readable.
	 *
	 * @param filespec the file name to test.
	 * @return true if the specified file exists and is readable,
	 *         false otherwise.
	 */
	public static boolean isReadable(String filespec)
	{
		File fd = new File(filespec);
		return fd.canRead();
	}
	/**
	 * Determine if the specified file/directory exists.
	 *
	 * @param filespec the file name to test.
	 * @return true if the specified file/directory exists,
	 *         false otherwise.
	 */
	public static boolean exists(String filespec)
	{
		File fd = new File(filespec);
		return fd.exists();
	}

	// Separator for path into main .jar file
	private static String separator = Zip.ZIP_SEPARATOR;

	/**
	 * Retrieve name of the root directory of the packages.
	 */
	public static final String LIBRARY_ROOT_NAME = jparsec.util.Version.PACKAGE_NAME.toLowerCase();

	/**
	 * Retrieve path of the IUE data directory.
	 */
	public static final String DATA_IUE_DIRECTORY = LIBRARY_ROOT_NAME + separator + "data" + separator + "iuepms" + separator;

	/**
	 * Retrieve path of the Wendker data directory.
	 */
	public static final String DATA_Wendker_DIRECTORY = LIBRARY_ROOT_NAME + separator + "data" + separator + "ii199a" + separator;

	/**
	 * Retrieve path of the sky data directory.
	 */
	public static final String DATA_SKY_DIRECTORY = LIBRARY_ROOT_NAME + separator + "data" + separator + "sky" + separator;

	/**
	 * Retrieve path of the planetary locations data directory.
	 */
	public static final String DATA_SKY_LOCATIONS_DIRECTORY = LIBRARY_ROOT_NAME + separator + "data" + separator + "sky" + separator + "locations" + separator;

	/**
	 * Retrieve path of the orbital elements directory.
	 */
	public static final String DATA_ORBITAL_ELEMENTS_DIRECTORY = LIBRARY_ROOT_NAME + separator + "data" + separator + "orbital_elements" + separator;

	/**
	 * Retrieve path of the data directory.
	 */
	public static final String DATA_DIRECTORY = LIBRARY_ROOT_NAME + separator + "data" + separator;

	/**
	 * Retrieve path of the sun data directory.
	 */
	public static final String DATA_SUNSPOT_DIRECTORY = LIBRARY_ROOT_NAME + separator + "data" + separator + "sun" + separator;

	/**
	 * Retrieve name of the orbital elements jar file.
	 */
	public static final String DATA_ORBITAL_ELEMENTS_JARFILE = "orbital_elements";

	/**
	 * Retrieve path of the BSC5 directory.
	 */
	public static final String DATA_STARS_BSC5_DIRECTORY = LIBRARY_ROOT_NAME + separator + "data" + separator + "stars" + separator + "bsc5" + separator;

	/**
	 * Retrieve name of the BSC5 jar file.
	 */
	public static final String DATA_STARS_BSC5_JARFILE = "bsc5";

	/**
	 * Retrieve path of the Sky2000 directory.
	 */
	public static final String DATA_STARS_SKY2000_DIRECTORY = LIBRARY_ROOT_NAME + separator + "data" + separator + "stars" + separator + "sky2000" + separator;

	/**
	 * Retrieve name of the Sky2000 jar file.
	 */
	public static final String DATA_STARS_SKY2000_JARFILE = "sky2000";

	/**
	 * Retrieve path of the telescopes directory.
	 */
	public static final String DATA_TELESCOPES_DIRECTORY = LIBRARY_ROOT_NAME + separator + "data" + separator + "telescopes" + separator;

	/**
	 * Retrieve name of the telescopes jar file.
	 */
	public static final String DATA_TELESCOPES_JARFILE = "telescopes";

	/**
	 * Retrieve path of the EOP directory.
	 */
	public static final String DATA_EOP_DIRECTORY = LIBRARY_ROOT_NAME + separator + "data" + separator + "eop" + separator;

	/**
	 * Retrieve name of the EOP jar file.
	 */
	public static final String DATA_EOP_JARFILE = "eop";

	/**
	 * Retrieve path of the textures directory.
	 */
	public static final String DATA_TEXTURES_DIRECTORY = LIBRARY_ROOT_NAME + separator + "data" + separator + "textures" + separator;

	/**
	 * Retrieve name of the textures jar file.
	 */
	public static final String DATA_TEXTURES_JARFILE = "textures";

	/**
	 * Retrieve path of the icons directory.
	 */
	public static final String DATA_ICONS_DIRECTORY = LIBRARY_ROOT_NAME + separator + "data" + separator + "icons" + separator;

	/**
	 * Retrieve name of the icons jar file.
	 */
	public static final String DATA_ICONS_JARFILE = "images";

	/**
	 * Retrieve path of the dust directory.
	 */
	public static final String DATA_DUST_DIRECTORY = LIBRARY_ROOT_NAME + separator + "data" + separator + "dust" + separator;

	/**
	 * Retrieve path of the Draine dust directory.
	 */
	public static final String DATA_DUST_DRAINE_DIRECTORY = LIBRARY_ROOT_NAME + separator + "data" + separator + "dust" + separator + "draine" + separator;

	/**
	 * Retrieve name of the dust jar file.
	 */
	public static final String DATA_DUST_JARFILE = "dust";

	/**
	 * Retrieve path of the landscapes directory.
	 */
	public static final String DATA_LANDSCAPES_DIRECTORY = LIBRARY_ROOT_NAME + separator + "data" + separator + "landscapes" + separator;

	/**
	 * Retrieve name of the landscapes jar file.
	 */
	public static final String DATA_LANDSCAPES_JARFILE = "landscapes";

	/**
	 * Retrieve path of the Series96 directory.
	 */
	public static final String SERIES96_DIRECTORY = LIBRARY_ROOT_NAME + separator + "data" + separator + "planets" + separator + "series96" + separator;

	/**
	 * Retrieve name of the Series96 jar file.
	 */
	public static final String SERIES96_JARFILE = "series96";

	/**
	 * Retrieve path of the Vsop87 directory.
	 */
	public static final String VSOP87_DIRECTORY = LIBRARY_ROOT_NAME + separator + "data" + separator + "planets" + separator + "vsop87" + separator;

	/**
	 * Retrieve name of the Vsop87 jar file.
	 */
	public static final String VSOP87_JARFILE = "vsop87";

	/**
	 * Retrieve path of the Kurucz directory.
	 */
	public static final String DATA_KURUCZ_DIRECTORY = LIBRARY_ROOT_NAME + separator + "data" + separator + "Kurucz" + separator;

	/**
	 * Retrieve name of the Kurucz jar file.
	 */
	public static final String DATA_KURUCZ_JARFILE = "kurucz";

	/**
	 * Retrieve path of the Siess directory.
	 */
	public static final String DATA_SIESS_DIRECTORY = LIBRARY_ROOT_NAME + separator + "data" + separator + "Siess" + separator;

	/**
	 * Retrieve name of the Siess jar file.
	 */
	public static final String DATA_SIESS_JARFILE = "siess";

	/**
	 * Retrieve path of the RADEX directory.
	 */
	public static final String DATA_RADEX_DIRECTORY = LIBRARY_ROOT_NAME + separator + "data" + separator + "radex" + separator;

	/**
	 * Retrieve name of the RADEX jar file.
	 */
	public static final String DATA_RADEX_JARFILE = "radex";

	/**
	 * Retrieve name of the sky files jar file.
	 */
	public static final String DATA_SKY_JARFILE = "sky";

	/**
	 * Retrieve name of the Sun spot jar file.
	 */
	public static final String DATA_SUNSPOT_JARFILE = "sunspot";

	/**
	 * Retrieve path of the small images directory.
	 */
	public static final String DATA_IMAGES_SMALL_DIRECTORY =  LIBRARY_ROOT_NAME + separator + "data" + separator + "images" + separator + "small" + separator;
	/**
	 * Retrieve path of the images directory.
	 */
	public static final String DATA_IMAGES_DIRECTORY =  LIBRARY_ROOT_NAME + separator + "data" + separator + "images" + separator;
	/**
	 * Retrieve names of the images jar file.
	 */
	public static final String DATA_IMAGES_JARFILE = "images";
	/**
	 * Retrieve path of the JPL ephemeris directory.
	 */
	public static final String DATA_JPL_EPHEM_DIRECTORY =  LIBRARY_ROOT_NAME + separator + "data" + separator + "ephem" + separator +
		"jpl" + separator;
	/**
	 * Retrieve names of the JPL ephemeris jar file.
	 */
	public static final String DATA_JPL_EPHEM_JARFILE = "jpl_ephem";

	/**
	 * Selects a directory using the default graphical interface.
	 * @param initialPath Initial path to start search. Can be null.
	 * @param open True to show an open dialog, false for a save dialog.
	 * @return Directory path. Null if cancel is pressed.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String directoryChooser(String initialPath, boolean open)
	}

	/**
	 * Selects a file/directory using the default graphical interface.
	 * @return File path. Null if cancel is pressed.
	 * @param open True to show an open dialog, false for a save dialog.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String fileChooser(boolean open)
	throws JPARSECException {
		throw new UnsupportedOperationException("File chooser not available on Android");
	}

	/**
	 * Selects a file using the default graphical interface and a file
	 * filter.
	 * @param fileFilter Extension of the files. Can be null.
	 * @param initialPath Initial path to start search. Can be null.
	 * @param open True to show an open dialog, false for a save dialog.
	 * @return File path. Null if cancel is pressed.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String fileChooser(String fileFilter, String initialPath, boolean open)
	throws JPARSECException {
		throw new UnsupportedOperationException("File chooser not available on Android");
	}

	/**
	 * Deletes a file or directory.
	 * @param path Path of the file/directory to delete.
	 * @throws JPARSECException If the file/directory cannot be deleted, for example
	 * because it contains protected/opened files/subdirectories.
	 */
	public static void deleteFile(String path)
	throws JPARSECException {
		File f = new File(path);
		if (!f.exists()) return;
		boolean success = true;
		if (f.isDirectory()) {
			String files[] = FileIO.getSubdirectories(path);
			for (int i=0; i<files.length; i++)
			{
				File ff = new File(files[i]);
				success = ff.delete();
				if (!success) throw new JPARSECException("could not delete file/directory "+files[i]);
			}
			files = FileIO.getFiles(path);
			for (int i=0; i<files.length; i++)
			{
				File ff = new File(files[i]);
				success = ff.delete();
				if (!success) throw new JPARSECException("could not delete file/directory "+files[i]);
			}
		}
		if (success) success = f.delete();
		if (!success) throw new JPARSECException("could not delete file/directory "+path);
	}

	/**
	 * Create a given directory.
	 * @param path Path of the directory.
	 * @return True if the directory is correctly created, false otherwise.
	 */
	public static boolean createDirectory(String path)
	{
		boolean success = (new File(path)).mkdir();
		return success;
	}
	/**
	 * Create a given directory and all necessary parent directories.
	 * @param path Path of the directory.
	 * @return True if the directories are correctly created, false otherwise.
	 */
	public static boolean createDirectories(String path)
	{
		boolean success = (new File(path)).mkdirs();
		return success;
	}

	/**
	 * Sets a field to a given value.
	 * @param field The field position, starting from 1.
	 * @param row The set of fields.
	 * @param separator The field separator.
	 * @param value The value.
	 * @return The new set of fields.
	 */
    public static String setField(int field, String row, String separator, String value)
    {
	   	 String r = "";
		 for (int i=1; i<field; i++)
		 {
			 r += FileIO.getField(i, row, separator, true) + separator;
		 }
		 r += value;
		 for (int i=field+1; i<FileIO.getNumberOfFields(row, separator, true); i++)
		 {
			 r += separator + FileIO.getField(i, row, separator, true);
		 }
		 return r;
    }

    /**
     * Returns the time a given resource was modified for the last time.
     * @param jarFile The name of the .jar file in the lib subdirectory (same directory
     * as jparsec.jar), without extension.
     * @param jarPath The path to a given given directory inside the .jar file.
     * @param fileName The name of the a file inside that .jar and directory.
     * @return Last modified time of the file inside the .jar, given as milliseconds
     * from 1970 January 1 at 0h UTC, or -1 if an error occurs.
     */
    public static long getLastModifiedTimeOfResource(String jarFile, String jarPath, String fileName) {
		try
		{
			String path = FileIO.class.getClassLoader().getResource(jarPath+fileName).getPath();
			if (path.startsWith("file:")) path = path.substring(5);
			int p = path.indexOf(".jar");
			if (p < 0) {
				File file = new File(path);
				return file.lastModified();
			}
			path = path.substring(0, p+4);

			JarFile jarfile = new JarFile(path);
			Enumeration<JarEntry> jarEnum = jarfile.entries();
			JarEntry entry;
			while(jarEnum.hasMoreElements()) {
				entry = jarEnum.nextElement();
				if (entry.getName().endsWith(fileName)) return entry.getTime();
			}

			return -1;
		} catch (Exception exc) {
			Logger.log(LEVEL.ERROR, "Could not locate resource "+fileName+". Details: "+exc.getLocalizedMessage());
			return -1;
		}
    }
    /**
     * Eliminates leading 0 from a given input string.
     * @param value The string value.
     * @param keepLast True to keep last 0 before a decimal
     * point.
     * @return Same value without any leading 0's.
     */
	public static String eliminateLeadingZeros(String value,
			boolean keepLast) {
		if (value == null || value.equals("")) return value;
		if (value.startsWith("0")) {
			int index = 0;
			while (value.substring(index, index + 1).equals("0")) {
				index ++;
				if (index == value.length()) {
					if (keepLast) return "0";
					return "";
				}
			}
			if (keepLast && index < value.length() && value.substring(index, index + 1).equals(".")) index --;
			return value.substring(index);
		} else {
			return value;
		}
	}
}

