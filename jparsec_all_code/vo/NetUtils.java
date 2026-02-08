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
package jparsec.vo;

import java.net.InetAddress;

import jparsec.util.JPARSECException;

/**
 * A class with useful net functions.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class NetUtils
{
	// private constructor so that this class cannot be instantiated.
	private NetUtils() {}

	/**
	 * Returns the local host name.
	 * @return Local host name.
	 * @throws JPARSECException If the local host cannot
	 * be reached.
	 */
	public static String getLocalHostName()
	throws JPARSECException {
		try {
			InetAddress id = InetAddress.getLocalHost();
			return id.getHostName();
		} catch (Exception exc)
		{
			throw new JPARSECException("cannot obtain local host.", exc);
		}
	}

	/**
	 * Returns the local host address.
	 * @return Local host address.
	 * @throws JPARSECException If the local host cannot
	 * be reached.
	 */
	public static String getLocalHostAddress()
	throws JPARSECException {
		try {
			InetAddress id = InetAddress.getLocalHost();
			return id.getHostAddress();
		} catch (Exception exc)
		{
			throw new JPARSECException("cannot obtain local host.", exc);
		}
	}
}
