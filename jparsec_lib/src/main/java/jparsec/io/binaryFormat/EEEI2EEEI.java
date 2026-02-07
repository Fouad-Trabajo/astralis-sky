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
package jparsec.io.binaryFormat;

import java.io.Serializable;

/**
 * Implementation of the EEEI or big endian format for reading binary files.
 * Write is supported.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class EEEI2EEEI
    implements Convertible, Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor.
	 */
    public EEEI2EEEI() { }

    public short readShort(byte abyte0[], int i)
    {
        return ByteArrayConverter.readShort(abyte0, i);
    }

    public int readInt(byte abyte0[], int i)
    {
        return ByteArrayConverter.readInt(abyte0, i);
    }

    public float readFloat(byte abyte0[], int i)
    {
        float f = ByteArrayConverter.readFloat(abyte0, i);
        return f;
    }

    public double readDouble(byte abyte0[], int i)
    {
        return ByteArrayConverter.readDouble(abyte0, i);
    }

    public void writeShort(byte abyte0[], int i, short value)
    {
        ByteArrayConverter.writeShort(abyte0, i, value);
    }

    public void writeInt(byte abyte0[], int i, int value)
    {
        ByteArrayConverter.writeInt(abyte0, i, value);
    }

    public void writeFloat(byte abyte0[], int i, float value)
    {
        ByteArrayConverter.writeFloat(abyte0, i, value);
    }

    public void writeDouble(byte abyte0[], int i, double value)
    {
        ByteArrayConverter.writeDouble(abyte0, i, value);
    }
}
