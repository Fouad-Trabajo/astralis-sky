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
package jparsec.io.image;

import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.util.*;

import java.io.Serializable;

/**
 * A class to manage image headers.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class HeaderElement implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Holds the key. In fits files can only have length 8 or lower.
	 */
	public String key;
	/**
	 * Holds the value.
	 */
	public Object value;
	/**
	 * Holds a comment. Used in fits file.
	 */
	public String comment;
	/**
	 * Holds a complete format specification, usually only needed
	 * in binary tables inside fits files. A for String, 
	 * 5A for a String of length 5, A(1,1) for a String 
	 * array of two dimensions, and so on.
	 */
	public String format;

	/**
	 * List of possible field types.
	 */
	public enum FIELD_TYPE {
		/** String format with identifier 'A'. */
		STRING_A ("A"),
		/** Integer format with identifier 'J'. */
		INTEGER_J ("J"),
		/** Double format with identifier 'D'. */
		DOUBLE_D ("D"),
		/** Float format with identifier 'E'. */
		FLOAT_E ("E"),
		/** Boolean format with identifier 'L'. */
		BOOLEAN_L ("L"),
		/** Short format with identifier 'I'. */
		SHORT_I ("I"), 
		/** Complex with floats format with identifier 'C'. */
		COMPLEX_FLOAT_C ("C"),
		/** Complex with doubles format with identifier 'M'. */
		COMPLEX_DOUBLE_M ("M"),
		/** Byte format with identifier 'X'. */
		BYTE_X ("X"),
		/** Byte format with identifier 'B'. */
		BYTE_B ("B");
		
		/** Identifier for the field type. */
		public final String id;
		
		private FIELD_TYPE (String s) {
			id = s;
		}
		
		/**
		 * Returns the enum value for a given format identifier.
		 * @param id The format identifier.
		 * @return The enum, or null if cannot be found.
		 */
		public static FIELD_TYPE getFieldTypeFromID (String id) {
			if (id == null || id.equals("")) return null;
			FIELD_TYPE type[] = FIELD_TYPE.values();
			for (int i=0; i<type.length; i++) {
				if (type[i].id.equals(id)) return type[i];
			}
			if (id.length() > 1) {
				HeaderElement h = new HeaderElement("V", "D");
				h.format = id;
				return FitsBinaryTable.getColumnFormat(h);
			}
			return null;
		}
	}
	
	/**
	 * Constructor with no format.
	 * @param key The key.
	 * @param value The value.
	 * @param comment The comment.
	 */
	public HeaderElement(String key, String value, String comment)
	{
		this.key = key;
		this.value = value;
		this.comment = comment;
	}

	/**
	 * Constructor with no format.
	 * @param key The key.
	 * @param value The value.
	 * @param comment The comment.
	 */
	public HeaderElement(String key, Object value, String comment)
	{
		this.key = key;
		this.value = value;
		this.comment = comment;
	}

	/**
	 * Simple constructor for a float parameter and the key.
	 * Note you must provide the key value, not the description. 
	 * The description field is set to the key value.
	 * @param f Value.
	 * @param k Key value.
	 */
	public HeaderElement(float f, String k)
	{
		key = comment = k;
		value = f;
		format = FIELD_TYPE.FLOAT_E.id;
	}
	/**
	 * Simple constructor for a double parameter and the key.
	 * Note you must provide the key value, not the description. 
	 * The description field is set to the key value.
	 * @param f Value.
	 * @param k Key value.
	 */
	public HeaderElement(double f, String k)
	{
		key = comment = k;
		value = f;
		format = FIELD_TYPE.DOUBLE_D.id;
	}
	/**
	 * Simple constructor for a string parameter and the key.
	 * Note you must provide the key value, not the description. 
	 * The description field is set to the key value.
	 * @param f Value.
	 * @param k Key value.
	 */
	public HeaderElement(String f, String k)
	{
		key = comment = k;
		value = f;
		format = FIELD_TYPE.STRING_A.id;
	}
	/**
	 * Simple constructor for an integer parameter and the key.
	 * Note you must provide the key value, not the description. 
	 * The description field is set to the key value.
	 * @param f Value.
	 * @param k Key value.
	 */
	public HeaderElement(int f, String k)
	{
		key = comment = k;
		value = f;
		format = FIELD_TYPE.INTEGER_J.id;
	}

	/**
	 * Returns the value as an integer.
	 * @return The value.
	 * @throws JPARSECException For a null value.
	 */
	public int getAsInt() throws JPARSECException
	{
		if (this.value == null) throw new JPARSECException("Null value");
		if (value instanceof Integer) return (Integer) value;
		return Integer.parseInt(this.getAsString().trim());
	}
	
	/**
	 * Returns the value as a double.
	 * @return The value.
	 * @throws JPARSECException For a null value.
	 */
	public double getAsDouble() throws JPARSECException
	{
		if (this.value == null) throw new JPARSECException("Null value");
		if (value instanceof Double) return (Double) value;
		return DataSet.parseDouble(getAsString().trim());
	}
	
	/**
	 * Returns the value as a float.
	 * @return The value.
	 * @throws JPARSECException For a null value.
	 */
	public float getAsFloat() throws JPARSECException
	{
		if (this.value == null) throw new JPARSECException("Null value");
		if (value instanceof Float) return (Float) value;
		return DataSet.parseFloat(getAsString().trim());
	}

	/**
	 * Returns the value as a boolean.
	 * @return The value.
	 * @throws JPARSECException For a null value.
	 */
	public boolean getAsBoolean() throws JPARSECException
	{
		if (this.value == null) throw new JPARSECException("Null value");
		if (value instanceof Boolean) return (Boolean) value;
		return Boolean.parseBoolean(this.getAsString().trim());
	}

	/**
	 * Returns the value as a long.
	 * @return The value.
	 * @throws JPARSECException For a null value.
	 */
	public long getAsLong() throws JPARSECException
	{
		if (this.value == null) throw new JPARSECException("Null value");
		if (value instanceof Long) return (Long) value;
		return Long.parseLong(this.getAsString().trim());
	}

	/**
	 * Returns the value as a String.
	 * @return The String, or null.
	 */
	public String getAsString() {
		if (value instanceof String) return (String) value;
		if (value == null) return null;
		return value.toString();
	}

	/**
	 * Returns the field type.
	 * @return Field type, or null.
	 */
	public FIELD_TYPE getFieldType() {
		if (format == null) return null;
		return FIELD_TYPE.getFieldTypeFromID(format);
	}
	
	/**
	 * Parses a given set of columns to produce a header.
	 * @param columns Columns with key + space + value + space(s) + / + comment.
	 * Comment is optional.
	 * @return The header.
	 */
	public static HeaderElement[] parseHeader(String columns[])
	{
		HeaderElement[] header = new HeaderElement[columns.length];
		for (int i=0; i<columns.length; i++)
		{
			String key = FileIO.getField(1, columns[i], " ", true);
			String value = FileIO.getRestAfterField(1, columns[i], " ", true);
			String comment = "";
			int bar = value.indexOf("/");
			if (bar > 0 && bar < value.length()-1) {
				comment = value.substring(bar+1).trim();
				value = value.substring(0, bar).trim();
			}
			header[i] = new HeaderElement(key, value, comment);
		}
		return header;

	}

	/**
	 * Returns the header array for an image.
	 * @param header The header set of objects.
	 * @return The header array.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String[] getHeader(HeaderElement header[])
	throws JPARSECException {
		String k[] = new String[header.length];
		for (int i=0; i<header.length; i++)
		{
			String key = header[i].key;
			if (key == null) key = "";
			key = FileIO.addSpacesAfterAString(key, 25);
			String value = header[i].getAsString();
			if (value == null) value = "";
			value = FileIO.addSpacesAfterAString(value, 25);
			String comment = header[i].comment;
			if (comment == null) comment = "";

			k[i] = key + value + " / " + comment;
		}
		return k;
	}

	/**
	 * Returns the index of a given key.
	 * @param header The header.
	 * @param key The key to search.
	 * @param considerCase True to match case (exactly the input key).
	 * @return The index of the key, or -1 if it is not found.
	 */
	public static int getIndex(HeaderElement header[], String key, boolean considerCase)
	{
		String keyS = key;
		if (!considerCase) keyS = key.toLowerCase();

		int out = -1;
		for (int i=0; i<header.length; i++)
		{
			String keySS = header[i].key;
			if (!considerCase) keySS = keySS.toLowerCase();

			if (keyS.equals(keySS)) {
				out = i;
				break;
			}
		}
		return out;
	}

	/**
	 * Searches for a given {@linkplain HeaderElement} by its description.
	 * @param param The array of {@linkplain HeaderElement}.
	 * @param d The description to search for.
	 * @return The {@linkplain HeaderElement} found, or null if there's no any.
	 */
	public static HeaderElement getByDescription(HeaderElement param[], String d)
	{
		HeaderElement p = null;
		for (int i=0; i<param.length; i++)
		{
			if (param[i].comment.equals(d)) {
				p = param[i].clone();
				break;
			}
		}
		return p;
	}

	/**
	 * Searches for a given {@linkplain HeaderElement} by its key.
	 * @param param The array of {@linkplain HeaderElement}.
	 * @param d The key to search for.
	 * @return The {@linkplain HeaderElement} found, or null if there's no any.
	 */
	public static HeaderElement getByKey(HeaderElement param[], String d)
	{
		HeaderElement p = null;
		for (int i=0; i<param.length; i++)
		{
			if (param[i].key.trim().equals(d.trim())) {
				p = param[i].clone();
				break;
			}
		}
		return p;
	}
	
	/**
	 * Searches for a given {@linkplain HeaderElement} by its key.
	 * @param param The array of {@linkplain HeaderElement}.
	 * @param d The key to search for.
	 * @return The value of the header parameter as a double value.
	 * @throws JPARSECException In case the header key is not found or is not a number.
	 */
	public static double getByKeyAsDouble(HeaderElement param[], String d) throws JPARSECException
	{
		for (int i=0; i<param.length; i++)
		{
			if (param[i].key.trim().equals(d.trim()))
				return param[i].getAsDouble();
		}
		throw new JPARSECException("Key "+d+" does not exists or is not a number");
	}

	/**
	 * Clones this instance.
	 */
	@Override
	public HeaderElement clone()
	{
		HeaderElement i = new HeaderElement(this.key, this.value, this.comment);
		i.format = this.format;
		return i;
	}
	/**
	 * Returns true if this instance is equals to another.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof HeaderElement)) return false;

		HeaderElement that = (HeaderElement) o;

		if (key != null ? !key.equals(that.key) : that.key != null) return false;
		if (value != null ? !value.equals(that.value) : that.value != null) return false;
		if (comment != null ? !comment.equals(that.comment) : that.comment != null) return false;

		return !(format != null ? !format.equals(that.format) : that.format != null);
	}

	@Override
	public int hashCode() {
		int result = key != null ? key.hashCode() : 0;
		result = 31 * result + (value != null ? value.hashCode() : 0);
		result = 31 * result + (comment != null ? comment.hashCode() : 0);
		result = 31 * result + (format != null ? format.hashCode() : 0);
		return result;
	}

	/**
	 * Returns a string representation of this header entry.
	 */
	@Override
	public String toString() {
		String out = this.key+" -> "+this.value;
		if (format != null) out += " ("+format+")";
		if (comment != null && !this.comment.equals("")) out += " // "+comment;
		return out;
	}

	/**
	 * Returns a string representation of an array of headers.
	 * @param input The input object.
	 * @return The String.
	 */
	public static String toString(HeaderElement input[]) {
		StringBuffer out = new StringBuffer("");
		String sep = FileIO.getLineSeparator(), fieldSep = " ";

		int l1 = -1, l2 = -1;
		for (int i=0; i<input.length; i++) {
			if (input[i].key.length() > l1 || l1 == -1) l1 = input[i].key.length();
			if (input[i].getAsString().length() > l2 || l2 == -1) l2 = input[i].getAsString().length();
		}
		for (int i=0; i<input.length; i++) {
			out.append(FileIO.addSpacesAfterAString(input[i].key, l1) + fieldSep + FileIO.addSpacesAfterAString(input[i].getAsString(), l2) + fieldSep + input[i].comment + sep);
		}

		return out.toString();
	}

	/**
	 * Returns the header array for a .fits image.
	 * @param header The header set of objects.
	 * @param l1 Length of column 1 (key name), 25 recommended.
	 * @param l2 Length of column 2 (value), 10 recommended.
	 * @return The header array.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String toString(HeaderElement header[], int l1, int l2)
	throws JPARSECException {
		String sep = FileIO.getLineSeparator();
		StringBuffer out = new StringBuffer("");
		for (int i=0; i<header.length; i++)
		{
			String key = header[i].key;
			if (key == null) key = "";
			key = FileIO.addSpacesAfterAString(key, l1);
			String value = header[i].getAsString();
			if (value == null) value = "";
			value = FileIO.addSpacesAfterAString(value, l2);
			String comment = header[i].comment;
			if (comment == null) comment = "";

			out.append(key + value + " / " + comment + sep);
		}
		return out.toString();
	}

	/**
	 * Constructs a simple header for a fits file using the dimensions
	 * and type supplied from the input data. Cards inserted are BITPIX,
	 * NAXIS, NAXIS1, NAXIS2, and EXTEND (set a T = true to allow extensions).
	 * @param o The data as an array with 2 dimensions, type double, int, float,
	 * short, or byte.
	 * @return The basic header.
	 * @throws JPARSECException If the format of the input data is not recognized.
	 */
	public static HeaderElement[] getFitsHeader(Object o) throws JPARSECException {
		int bp = -1, n1 = -1, n2 = -1;
		try {
			int data[][] = (int[][]) o;
			n1 = data.length;
			n2 = data[0].length;
			bp = 32;
		} catch (Exception exc) {
			try {
				double data[][] = (double[][]) o;
				n1 = data.length;
				n2 = data[0].length;
				bp = -64;
			} catch (Exception exc2) {
				try {
					float data[][] = (float[][]) o;
					n1 = data.length;
					n2 = data[0].length;
					bp = -32;
				} catch (Exception exc3) {
					try {
						short data[][] = (short[][]) o;
						n1 = data.length;
						n2 = data[0].length;
						bp = 16;
					} catch (Exception exc4) {
						try {
							byte data[][] = (byte[][]) o;
							n1 = data.length;
							n2 = data[0].length;
							bp = 8;
						} catch (Exception exc5) {
							throw new JPARSECException("Cannot recognize the format of the data in the input array, must have 2 dimensions and type int, float, double, short, or byte.");
						}
					}
				}
			}
		}
		HeaderElement out[] = new HeaderElement[] {
				new HeaderElement("BITPIX", ""+bp, ""),
				new HeaderElement("NAXIS", "2", "Dimensionality"),
				new HeaderElement("NAXIS1", ""+n1, ""),
				new HeaderElement("NAXIS2", ""+n2, ""),
				new HeaderElement("EXTEND", "T", "Extension permitted")
		};
		return out;
	}

	/**
	 * Adds a specific entry to a header defined by a set of {@linkplain HeaderElement}
	 * objects.
	 * @param input Input header.
	 * @param entry Entry to add. If it is already present, it will be updated.
	 * @return The new header.
	 */
	public static HeaderElement[] addHeaderEntry(HeaderElement input[], HeaderElement entry) {
		int index = getIndex(input, entry.key);
		if (index >= 0) {
			HeaderElement out[] = input.clone();
			out[index] = entry;
			return out;
		}

		Object o[] = null;
		try {
			o = DataSet.addObjectArray(input, new Object[] {entry});
		} catch (Exception exc) {} // will never happen
		HeaderElement out[] = new HeaderElement[input.length+1];
		for (int i=0; i<out.length; i++) {
			out[i] = (HeaderElement) o[i];
		}
		return out;
	}

	/**
	 * Deletes a set of entries from a header defined by a set of {@linkplain HeaderElement}
	 * objects.
	 * @param input Input header.
	 * @param keys Entries to remove if it is present.
	 * @return The new header.
	 */
	public static HeaderElement[] deleteHeaderEntries(HeaderElement input[], String keys[]) {
		for (int i=0; i<keys.length; i++) {
			input = deleteHeaderEntry(input, keys[i]);
		}
		return input;
	}

	/**
	 * Deletes a specific entry to a header defined by a set of {@linkplain HeaderElement}
	 * objects.
	 * @param input Input header.
	 * @param key Entry to remove if it is present.
	 * @return The new header.
	 */
	public static HeaderElement[] deleteHeaderEntry(HeaderElement input[], String key) {
		int index = getIndex(input, key);
		if (index >= 0) {
			HeaderElement[] out = new HeaderElement[0];
			if (index > 0) out = (HeaderElement[]) DataSet.getSubArray(input, 0, index-1);
			if (index < input.length-1) {
				try {
					out = (HeaderElement[]) DataSet.addObjectArray(out, DataSet.getSubArray(input, index + 1, input.length - 1));
				} catch (Exception exc) {} // will never happen
			}
			return out;
		}
		return input.clone();
	}

	/**
	 * Adds some entries to a header defined by a set of {@linkplain HeaderElement}
	 * objects.
	 * @param input Input header.
	 * @param entries Entries to add. Any of them already present in the input header will
	 * be updated.
	 * @return The new header.
	 */
	public static HeaderElement[] addHeaderEntry(HeaderElement input[], HeaderElement entries[]) {
		HeaderElement out[] = input.clone();
		for (int i=0; i<entries.length; i++) {
			out = addHeaderEntry(out, entries[i]);
		}
		return out;
	}

	/**
	 * Checks if a header contains a card or not.
	 * @param input Input header.
	 * @param key The card to check.
	 * @return True if the header contains an entry with the given key, false otherwise.
	 */
	public static boolean contains(HeaderElement input[], String key) {
		for (int i=0; i<input.length; i++) {
			if (input[i].key.equals(key)) return true;
		}
		return false;
	}
	/**
	 * Checks if a header contains a card or not, returning the index position of it.
	 * @param input Input header.
	 * @param key The card to check.
	 * @return The index, or -1 if it is not found.
	 */
	public static int getIndex(HeaderElement input[], String key) {
		for (int i=0; i<input.length; i++) {
			if (input[i].key.equals(key)) return i;
		}
		return -1;
	}
}
