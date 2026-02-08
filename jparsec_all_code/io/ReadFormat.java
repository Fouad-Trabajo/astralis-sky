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

import java.util.ArrayList;

import jparsec.graph.DataSet;
import jparsec.io.image.HeaderElement;
import jparsec.math.Constant;
import jparsec.util.JPARSECException;

/**
 * A class to read the format previously defined in {@linkplain FileFormatElement}.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class ReadFormat {

	/**
	 * Holds the {@linkplain FileFormatElement} array which will be read.
	 */
	private FileFormatElement[] formatToRead;
	private ArrayList<String> fieldNames;

	/**
	 * Reads a field.
	 *
	 * @param line Record of the file.
	 * @param fmt Object defining the field to read.
	 * @return A string with the field. An empty string is returned if the
	 *         {@linkplain FileFormatElement} object defines a field outside record limits.
	 */
	public static String readField(String line, FileFormatElement fmt)
	{
		int ini = fmt.startingPosition - 1;
		int fin = fmt.endingPosition;
		if (fin > line.length())
			fin = line.length();
		if (ini >= line.length())
			return "";

		String field = line.substring(ini, fin);

		return field;
	}

	/**
	 * Reads a field.
	 *
	 * @param line Record of the file.
	 * @param field Field name.
	 * @return A string with the field. An empty string is returned if the
	 *         {@linkplain FileFormatElement} object defines a field outside record limits.
	 * @throws JPARSECException If an error occurs.
	 */
	public String readField(String line, String field)
	throws JPARSECException {
		FileFormatElement fmt = this.getField(field);
		int ini = fmt.startingPosition - 1;
		int fin = fmt.endingPosition;
		if (fin > line.length())
			fin = line.length();
		if (ini >= line.length())
			return "";

		String fieldValue = line.substring(ini, fin);

		return fieldValue;
	}

	/**
	 * Sets the format to read.
	 *
	 * @param fmt An array of {@linkplain FileFormatElement} objects defining the format of the
	 *        file.
	 */
	public void setFormatToRead(FileFormatElement[] fmt)
	{
		formatToRead = fmt;
		fieldNames = new ArrayList<String>();
		for (int i = 0; i < formatToRead.length; i++)
		{
			fieldNames.add(formatToRead[i].fieldName);
		}
	}

	/**
	 * Constructor with a given file format array.
	 * @param fmt File format array.
	 */
	public ReadFormat(FileFormatElement[] fmt)
	{
		setFormatToRead(fmt);
	}
	/**
	 * Empty constructor.
	 */
	public ReadFormat()	{}

	/**
	 * Gets the fields from a {@linkplain FileFormatElement} object.
	 *
	 * @param fm A {@linkplain FileFormatElement} object defining the fields.
	 * @return An array of strings with the list of fields.
	 */
	public static String[] getFieldsAsStrings(FileFormatElement[] fm)
	{
		String list[] = new String[fm.length];
		for (int i = 0; i < fm.length; i++)
		{
			list[i] = fm[i].fieldName;
		}

		return list;
	}

	/**
	 * Gets the fields from a {@linkplain FileFormatElement} object.
	 *
	 * @param fm A {@linkplain FileFormatElement} object defining the fields.
	 * @return An array of Header objects with value and description/key
	 * set to the name of each of the fields in the input object.
	 */
	public static HeaderElement[] getFieldsAsParameters(FileFormatElement[] fm)
	{
		HeaderElement list[] = new HeaderElement[fm.length];
		for (int i = 0; i < fm.length; i++)
		{
			list[i] = new HeaderElement(fm[i].fieldName, fm[i].fieldName);
			list[i].key = list[i].comment;
		}

		return list;
	}

	/**
	 * Returns if a given field name exists or not.
	 * @param name Field name.
	 * @return True or false.
	 */
	public boolean fieldExists(String name) {
		return (fieldNames.indexOf(name) >= 0);
	}

	/**
	 * Gets a field from the current format to read.
	 *
	 * @param name The name/description of the field.
	 * @return A {@linkplain FileFormatElement} object defining the field.
	 * @throws JPARSECException Thrown if the field does not exist.
	 */
	public FileFormatElement getField(String name) throws JPARSECException
	{
		int index = fieldNames.indexOf(name);

		if (index == -1)
			throw new JPARSECException("the field " + name + " does not exist.");

		return formatToRead[index];
	}

	/**
	 * Obtains the length of a field.
	 *
	 * @param fmt Field format.
	 * @return Length.
	 */
	public static int getLength(FileFormatElement fmt)
	{
		return fmt.endingPosition - fmt.startingPosition + 1;
	}

	/**
	 * Reads a double precision value.
	 *
	 * @param line The record of the file.
	 * @param field The name/description of the field.
	 * @return A double precision value.
	 * @throws JPARSECException Thrown if the field does not exist.
	 */
	public double readDouble(String line, String field) throws JPARSECException
	{
		return toDouble(this.readField(line, field).trim());
	}

	/**
	 * Reads a float value.
	 *
	 * @param line The record of the file.
	 * @param field The name/description of the field.
	 * @return A float value.
	 * @throws JPARSECException Thrown if the field does not exist.
	 */
	public float readFloat(String line, String field) throws JPARSECException
	{
		return Float.parseFloat(this.readField(line, field).trim());
	}

	/**
	 * Reads an integer value.
	 *
	 * @param line The record of the file.
	 * @param field The name/description of the field.
	 * @return An integer value.
	 * @throws JPARSECException Thrown if the field does not exist.
	 */
	public int readInteger(String line, String field) throws JPARSECException
	{
		return toInt(this.readField(line, field).trim());
	}

	/**
	 * Reads a double precission value and converts it from degrees to radians.
	 *
	 * @param line The record of the file.
	 * @param field The name/description of the field.
	 * @return A double precission value in radians.
	 * @throws JPARSECException Thrown if the field does not exist.
	 */
	public double readDoubleToRadians(String line, String field) throws JPARSECException
	{
		return toDoubleRad(this.readField(line, field).trim());
	}

	/**
	 * Reads a string.
	 *
	 * @param line The record of the file.
	 * @param field The name/description of the field.
	 * @return A string with the field.
	 * @throws JPARSECException Thrown if the field does not exist.
	 */
	public String readString(String line, String field) throws JPARSECException
	{
		return this.readField(line, field).trim();
	}

	private static double toDouble(String field)
	{
		return DataSet.parseDouble(field);
	}

	private static int toInt(String field)
	{
		return Integer.parseInt(field);
	}

	private static double toDoubleRad(String field)
	{
		return (DataSet.parseDouble(field) * Constant.DEG_TO_RAD);
	}
}
