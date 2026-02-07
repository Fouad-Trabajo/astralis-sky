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

import java.io.*;

import jparsec.ephem.Functions;
import jparsec.graph.DataSet;
import jparsec.io.image.HeaderElement;
import jparsec.io.image.Picture;
import jparsec.util.JPARSECException;

/**
 * A class to write data to external files using static methods. Default
 * encoding is ISO 8859-1, but there are methods to include other encodings
 * using the constants of the {@linkplain ReadFile} class.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class WriteFile
{
	// private constructor so that this class cannot be instantiated.
	private WriteFile() {}

	/**
	 * Writes an external text file.
	 *
	 * @param pathToFile Path to the file.
	 * @param text Text to write.
	 * @param encoding The encoding.
	 * @throws JPARSECException If the file cannot be written..
	 */
	public static void writeAnyExternalFile(String pathToFile, String text, String encoding) throws JPARSECException
	{
		try
		{
			BufferedWriter dis = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pathToFile), encoding));

			dis.write(text);
			dis.close();
		} catch (IOException e3)
		{
			throw new JPARSECException(e3);
		}
	}

	/**
	 * Returns a {@linkplain BufferedWriter} object to write a file line by line.
	 * @param pathToFile The path to the file.
	 * @param charset The charset of that file.
	 * @return The {@linkplain BufferedWriter} object.
	 * @throws JPARSECException If an error occurs.
	 */
	public static BufferedWriter getBufferedWriterToAnyExternalFile(String pathToFile, String charset) throws JPARSECException
	{
		try {
			return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pathToFile), charset));
		} catch (IOException e2) {
			throw new JPARSECException("error while reading file " + pathToFile + ".", e2);
		}
	}

	/**
	 * Writes an external text file.
	 *
	 * @param pathToFile Path to the file.
	 * @param text Text to write as an array.
	 * @param encoding The encoding.
	 * @throws JPARSECException If the file cannot be written..
	 */
	public static void writeAnyExternalFile(String pathToFile, String text[], String encoding) throws JPARSECException
	{
		try
		{
			BufferedWriter dis = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pathToFile), encoding));

			for (int i = 0; i< text.length; i++)
			{
				dis.write(text[i] + FileIO.getLineSeparator());
			}
			dis.close();
		} catch (IOException e3)
		{
			throw new JPARSECException(e3);
		}
	}

	/**
	 * Writes an external text file using ISO encoding.
	 *
	 * @param pathToFile Path to the file.
	 * @param text Text to write.
	 * @throws JPARSECException If the file cannot be written..
	 */
	public static void writeAnyExternalFile(String pathToFile, String text) throws JPARSECException
	{
		writeAnyExternalFile(pathToFile, text, ReadFile.ENCODING_ISO_8859);
	}

	/**
	 * Writes an external text file using ISO encoding.
	 *
	 * @param pathToFile Path to the file.
	 * @param text Text to write as an array.
	 * @throws JPARSECException If the file cannot be written..
	 */
	public static void writeAnyExternalFile(String pathToFile, String text[]) throws JPARSECException
	{
		writeAnyExternalFile(pathToFile, text, ReadFile.ENCODING_ISO_8859);
	}

	/**
	 * Writes an image to a file.
	 * @param path The path.
	 * @param bmp The bitmap, must be a BufferedImage.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void writeImage(String path, Object bmp) throws JPARSECException {
		throw new UnsupportedOperationException("Image writing not available on Android");
		pic.write(path);
	}

	/**
	 * Creates a String entry in a given format from a set of header fields containing
	 * the value to be formatted by the format set of objects. Each field in the format array
	 * is identified to its corresponding field in the header set of objects using
	 * the field 'key' of each parameter.
	 * @param p The set of headers. In case you want to add a field called 'NAME' with
	 * a String, use the corresponding constructor and set 'NAME' of the description/key of the
	 * parameter. In case it is a double value, use the adequate constructor and a name
	 * for it as description, for instance 'SEMIMAJOR_AXIS'.
	 * @param format The format of the file. Here 'NAME' or 'SEMIMAJOR_AXIS' will contain
	 * the column interval where this field is located.
	 * @return The corresponding String with all parameters that matched formatted according
	 * to the specified format.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String getFormattedEntry(HeaderElement p[], FileFormatElement format[]) throws JPARSECException {
		String out = "";

		// Sort format by starting position
		double val[] = new double[format.length];
		for (int i=0; i<format.length; i++) {
			val[i] = format[i].startingPosition;
		}

		Object obj[] = DataSet.sortInCrescent(format, val);
		for (int i=0; i<format.length; i++) {
			format[i] = (FileFormatElement) obj[i];
		}

		// Create out string
		for (int i=0; i<format.length; i++) {
			int index = -1;
			for (int j=0; j<p.length; j++) {
				if (p[j].key.equals(format[i].fieldName)) {
					index = j;
					break;
				}
			}
			if (index < 0) continue;

			int l = format[i].endingPosition - format[i].startingPosition + 1;
			int n = format[i].startingPosition - out.length() - 1;
			if (n > 0) out += FileIO.addSpacesAfterAString("", n);

			String field = "";
			switch (p[index].getFieldType()) {
			case DOUBLE_D:
			case FLOAT_E:
				field = Functions.formatValue(p[index].getAsDouble(), l).trim();
				int pp = field.indexOf(".");
				if (pp >= 0) field = Functions.formatValue(p[index].getAsDouble(), l - pp - 1).trim();
				break;
			case INTEGER_J:
				field = ""+p[index].getAsInt();
				break;
			case STRING_A:
				field = p[index].getAsString().trim();
				break;
			}
			if (field.length() < l) field = FileIO.addSpacesAfterAString(field, l);
			if (field.length() > l) field = field.substring(0, l);
			out += field;
		}

		return out;
	}
}
