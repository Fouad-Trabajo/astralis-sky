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
package jparsec.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.util.Logger.LEVEL;

/**
 * Translates library to other languages. This class uses direct
 * translation from/to English and Spanish.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Translate
{
	// private constructor so that this class cannot be instantiated.
	private Translate() {}

	static {
		translations = new String[3][];
		getNumberOfEntries();
	}
	
	/**
	 * The set of languages available in JPARSEC.
	 */
	public enum LANGUAGE {
		/** ID constant for English. */
		ENGLISH,
		/** ID Constant for Spanish. */
		SPANISH,
		/** ID Constant for Italian. */
		ITALIAN
	}

	/**
	 * Selects default language for output.
	 */
	private static LANGUAGE defaultLanguage = LANGUAGE.ENGLISH;

	private static String[][] translations;

	/**
	 * Returns the number of entries.
	 * @return Number of entries.
	 */
	public static int getNumberOfEntries() {
		if (translations[0] == null) {
			try {
				translations[0] = new String[ReadFile.readResourceGetNumberOfLines("jparsec/util/english.txt", ReadFile.ENCODING_ISO_8859)];
				translations[1] = new String[translations[0].length];
				translations[2] = new String[translations[0].length];
			} catch (Exception exc) {
				throw new RuntimeException("Cannot read language files. This error should never happen!");
			}
		}
		return translations[0].length;
	}

	/**
	 * Translates a given value from one language to another.
	 * @param value Value to search.
	 * @param from ID constant of the language of the value.
	 * @param to ID constant of the output language.
	 * @return Output value, or input String if not found.
	 */
	public static String translate(String value, LANGUAGE from, LANGUAGE to)
	{
		if (value == null || "".equals(value)) return null;

 		if (from == to) {
			return DataSet.replaceAll(value, "\\n", FileIO.getLineSeparator(), true);
		}

		String valueFrom;

		for (int id = 0; id < getNumberOfEntries(); id++) {
			valueFrom = translations[from.ordinal()][id];
			if (valueFrom == null) continue;

			// If already read
			if (valueFrom.equals(value)) {
				return DataSet.replaceAll(getEntry(id, to), "\\n", FileIO.getLineSeparator(), true);
			}
		}

		// Set as default output the same input value, in case no translation is found
		String out = value;
		try
		{
			String pathToFile = "jparsec/util/"+from.name().toLowerCase()+".txt";
			String encoding = ReadFile.ENCODING_ISO_8859;
			String file_line = "";
			int index = -1, indexSW = -1, i = -1;
			int RindexSW = -1;

			InputStream is = Translate.class.getClassLoader().getResourceAsStream(pathToFile);
			BufferedReader dis = new BufferedReader(new InputStreamReader(is, encoding));

			while ((file_line = dis.readLine()) != null)
			{
				i ++;
				if (file_line.equals(value)) {
					index = i;
					break;
				}
				if (file_line.toLowerCase().startsWith(value.toLowerCase())) indexSW = i;
				if (value.toLowerCase().startsWith(file_line.toLowerCase())) RindexSW = i;
			}

			// Close file
			dis.close();

			if (index < 0) {
				index = indexSW;
				// FIXME: check if this is happening and maybe correct it
				if (index < 0) index = RindexSW;
				String s = "";
				if (index >= 0) {
					s = Translate.getEntry(index, from).trim().toLowerCase();
					if (!value.trim().toLowerCase().equals(s)) index = -1;
				}
			}
			if (index >= 0) {
				// Read this entry to the translations array if necessary, so there is no 
				// need to read it again
				out = getEntry(index, from);
				out = getEntry(index, to);
			}
		} catch (Exception e1)
		{
			Logger.log(LEVEL.ERROR, "Cannot read language file "+from+". This error should never happen!");
			return null;
		}
		
		out = DataSet.replaceAll(out, "\\n", FileIO.getLineSeparator(), true);
		return out;
	}

	/**
	 * Returns the string entry for a given index.
	 * @param id The index, from 0 to the number of entries-1.
	 * @param language The language to retrieve the entry in, or null
	 * to use the current default language.
	 * @return The entry.
	 * @throws RuntimeException If the language is invalid, something
	 * that should never happen.
	 */
	public static String getEntry(int id, LANGUAGE language) {
		if (language == null) language = defaultLanguage;
		if (translations[language.ordinal()][id] == null) {
			try {
				// Read all entries at once since one by one is too inefficient
				String all[] = DataSet.arrayListToStringArray(ReadFile.readResource("jparsec/util/"+language.name().toLowerCase()+".txt", ReadFile.ENCODING_ISO_8859));
				all = DataSet.replaceAll(all, "\\n", FileIO.getLineSeparator(), true);
				translations[language.ordinal()] = all;
				// Read only this entry to the translations array so there is no need to read it again
				//String out = ReadFile.readResourceSomeLines("jparsec/util/"+language.name().toLowerCase()+".txt", ReadFile.ENCODING_ISO_8859, id, id).get(0);
				//translations[language.ordinal()][id] = DataSet.replaceAll(out, "\\n", FileIO.getLineSeparator(), true);
			} catch (Exception exc) {
				throw new RuntimeException("Cannot read language files. This error should never happen!");
			}
		}
		
		return translations[language.ordinal()][id];
	}
	
	/**
	 * Translates a given value from English into another.
	 * @param value Value to search.
	 * @param to ID constant of the output language.
	 * @return Output value, or empty String if not found.
	 */
	public static String translate(String value, LANGUAGE to)
	{
		if (to == null) to = defaultLanguage;
		if (LANGUAGE.ENGLISH == to) return value;
		return translate(value, LANGUAGE.ENGLISH, to);
	}

	/**
	 * Translates a given value from English into selected language.
	 * @param value Value to search.
	 * @return Output value, or empty String if not found.
	 */
	public static String translate(String value)
	{
		if (LANGUAGE.ENGLISH == defaultLanguage) return value;
		return translate(value, LANGUAGE.ENGLISH, defaultLanguage);
	}

	/**
	 * Translates a given value from English into selected language.
	 * @param id ID of the String to be translated. Constants defined in this class.
	 * @return Output value.
	 */
	public static String translate(int id)
	{
		if (id < 0 || id > getNumberOfEntries()) {
			Logger.log(LEVEL.ERROR, "Cannot read line " + id + " of language file " + defaultLanguage + ". This error should never happen!");
			return null;
		}

		return getEntry(id, defaultLanguage);
	}

	/**
	 * Translates a given array from English into selected language.
	 * @param values Values to be translated.
	 * @return Output value, or empty String if not found.
	 */
	public static String[] translate(String[] values)
	{
		if (LANGUAGE.ENGLISH == defaultLanguage || values == null || values.length == 0) {
			return values;
		}

		String out[] = new String[values.length];
		for (int i = 0; i < out.length; i++) {
			out[i] = translate(values[i], LANGUAGE.ENGLISH, defaultLanguage);
		}

		return out;
	}

	/**
	 * Returns default language.
	 * @return ID constant of the language.
	 */
	public static LANGUAGE getDefaultLanguage()
	{
		return Translate.defaultLanguage;
	}

	/**
	 * Sets default language.
	 * @param language ID constant of the language.
	 */
	public static void setDefaultLanguage(LANGUAGE language)
	{
		Translate.defaultLanguage = language;
	}
}
