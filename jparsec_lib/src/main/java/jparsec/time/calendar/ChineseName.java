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
package jparsec.time.calendar;

import java.io.Serializable;

import jparsec.util.JPARSECException;

/**
 * Support class for Chinese.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class ChineseName implements Serializable
{
	private static final long serialVersionUID = -3639393671583969363L;

	/**
	 * Stem.
	 */
	public int stem = 1;

	/**
	 * Branch.
	 */
	public int branch = 1;

	/**
	 * Constructor.
	 *
	 * @param i Stem.
	 * @param j Branch.
	 *
	 * @throws JPARSECException If input is invalid.
	 */
	public ChineseName(final int i, final int j) throws JPARSECException
	{
		if ((i % 2) == (j % 2)) {
			stem = i;
			branch = j;
		} else {
			throw new JPARSECException("invalid input.");
		}
	}
}
