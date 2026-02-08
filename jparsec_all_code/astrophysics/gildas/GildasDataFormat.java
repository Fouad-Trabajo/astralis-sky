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
package jparsec.astrophysics.gildas;

import java.util.GregorianCalendar;

import jparsec.io.binaryFormat.Convertible;
import jparsec.io.binaryFormat.EEEI2EEEI;
import jparsec.io.binaryFormat.IEEE2EEEI;
import jparsec.io.binaryFormat.VAX2EEEI;
import jparsec.math.Constant;
import jparsec.time.AstroDate;
import jparsec.util.JPARSECException;

/**
 * Provides the appropriate interface to convert the data of .30m files.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class GildasDataFormat
{
	// private constructor so that this class cannot be instantiated.
	private GildasDataFormat() {}

	/**
	 * ID constant for a VAX data type (MULTIPLE, not SINGLE).
	 */
	public static final String VAX_CODE = "1   ";
	/**
	 * ID constant for a IEEE data type (MULTIPLE, not SINGLE).
	 */
	public static final String IEEE_CODE = "1A  ";
	/**
	 * ID constant for a EEEI data type (MULTIPLE, not SINGLE).
	 */
	public static final String EEEI_CODE = "1B  ";

    /**
     * Returns the interface to convert data of .30m files.
     * @param s Code of the header of the .30m file.
     * @return The interface to convert this data.
     */
    public static Convertible getConvertible(String s)
    {
        if(s.compareTo(VAX_CODE) == 0)
            return new VAX2EEEI();
        if(s.compareTo(IEEE_CODE) == 0)
            return new IEEE2EEEI();
        if(s.compareTo(EEEI_CODE) == 0)
            return new EEEI2EEEI();

        if (s.substring(0,1).equals("2")) return null; // new version unsupported

        if (s.substring(1,2).equals(VAX_CODE.substring(1, 2))) return new VAX2EEEI();
        if (s.substring(1,2).equals(IEEE_CODE.substring(1, 2))) return new IEEE2EEEI();
        if (s.substring(1,2).equals(EEEI_CODE.substring(1, 2))) return new EEEI2EEEI();
        return null;
    }

    /**
     * Returns the interface to convert data of images files.
     * @param c Code of the header of the image file.
     * @return The interface to convert this data.
     */
    public static Convertible getConvertibleImage(char c)
    {
        if (c == '_')
            return new VAX2EEEI();
        if (c == '-')
            return new IEEE2EEEI();
        if (c == '.')
            return new EEEI2EEEI();
        try {
			JPARSECException.addWarning("Could not find suitable format reader for this image file, "
					+ "valid format identifiers are _-. and found '"+c+"' ("+(int) c+")");
		} catch (Exception e) { }
        return null;
    }

    /**
     * Returns the date of a .30m file.
     * @param i The date as given by GILDAS.
     * @return The date.
     * @throws JPARSECException If an error occurs.
     */
    public static GregorianCalendar getDate(int i)
    throws JPARSECException {
     	return getAstroDate(i).toGCalendar();
    }

    /**
     * Returns the date of a .30m file.
     * @param i The date as given by GILDAS.
     * @return The date.
     * @throws JPARSECException If an error occurs.
     */
    public static AstroDate getAstroDate(int i)
    throws JPARSECException {
    	double gd = (double) i;
    	if (gd < 0.0) gd = gd + 0.5;
    	double jd = gd + 60549.0 + Constant.JD_MINUS_MJD;
    	jd = (int) (jd - 0.5) + 0.5;
    	AstroDate astro = new AstroDate(jd);
    	return astro;
    }

    /**
     * Returns the GILDAS date corresponding to a given Julian day.
     * @param jd Julian day.
     * @return Date for a GILDAS record.
     */
    public static int getGILDASdate(double jd)
    {
    	jd = (int) (jd - 0.5) + 0.5;
    	double gd = jd - 60549.0 - Constant.JD_MINUS_MJD;
    	if (gd < 0.0) gd = gd - 0.5;
    	return (int) gd;
    }
}
