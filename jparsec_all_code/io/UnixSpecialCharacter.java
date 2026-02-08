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

/**
 * A class to hold special characters.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class UnixSpecialCharacter
{
	// private constructor so that this class cannot be instantiated.
	private UnixSpecialCharacter() {}

	/**
	 * The set of special characters.
	 */
	public enum UNIX_SPECIAL_CHARACTER {
		/** ID constant for the bell. */
		BELL (7, "NULL"),
		/** ID constant for the backspace. */
		BACKSPACE (8, "\b"),
		/** ID constant for the tab. */
		TAB (9, "\t"),
		/** ID constant for the line feed. */
		LINE_FEED (10, "\n"),
		/** ID constant for the form feed. */
		FORM_FEED (12, "\f"),
		/** ID constant for the carriage return. */
		CARRIAGE_RETURN (13, "\r");

		/**
		 * Holds the ASCII code of this special character.
		 */
		public final int asciiCode;
		/**
		 * Holds the values of this special character.
		 */
		public final String value;

		private UNIX_SPECIAL_CHARACTER(int asc, String v) {
			this.asciiCode = asc;
			this.value = v;
		}
	};
}
