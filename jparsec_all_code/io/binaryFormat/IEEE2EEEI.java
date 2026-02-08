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
 * Implementation of the IEEE or little endian format for reading binary files.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class IEEE2EEEI
    implements Convertible, Serializable
{

	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor.
	 */
    public IEEE2EEEI() { }

    public short readShort(byte abyte0[], int i)
    {
        return ByteArrayConverter.readShort(new byte[] {abyte0[i + 1], abyte0[i]}, 0);
    }

    public int readInt(byte abyte0[], int i)
    {
        return ByteArrayConverter.readInt(new byte[] {abyte0[i + 3], abyte0[i + 2], abyte0[i + 1], abyte0[i]}, 0);
    }

    public float readFloat(byte abyte0[], int i)
    {
        float f = ByteArrayConverter.readFloat(new byte[] {abyte0[i + 3], abyte0[i + 2], abyte0[i + 1], abyte0[i]}, 0);
        return f;
    }

    public void writeFloat(byte abyte0[], int i, float value)
    {
        ByteArrayConverter.writeFloat(abyte0, i, value);
        byte abyte1[] = {
        		abyte0[i+3], abyte0[i+2], abyte0[i+1], abyte0[i]
        };
        abyte0[0] = abyte1[0];
        abyte0[1] = abyte1[1];
        abyte0[2] = abyte1[2];
        abyte0[3] = abyte1[3];
    }

    public double readDouble(byte abyte0[], int i)
    {
        byte abyte1[] = new byte[8];
        abyte1[7] = abyte0[i + 0];
        abyte1[6] = abyte0[i + 1];
        abyte1[5] = abyte0[i + 2];
        abyte1[4] = abyte0[i + 3];
        abyte1[3] = abyte0[i + 4];
        abyte1[2] = abyte0[i + 5];
        abyte1[1] = abyte0[i + 6];
        abyte1[0] = abyte0[i + 7];
        return ByteArrayConverter.readDouble(abyte1, 0);
    }

    public void writeShort(byte abyte0[], int i, short value)
    {
        ByteArrayConverter.writeShort(abyte0, i, value);
        byte abyte1[] = {
                abyte0[i+1], abyte0[i]
            };
        abyte0[0] = abyte1[0];
        abyte0[1] = abyte1[1];
    }

    public void writeInt(byte abyte0[], int i, int value)
    {
        ByteArrayConverter.writeInt(abyte0, i, value);
        byte abyte1[] = {
                abyte0[i+3], abyte0[i+2], abyte0[i+1], abyte0[i+0]
        };
        abyte0[0] = abyte1[0];
        abyte0[1] = abyte1[1];
        abyte0[2] = abyte1[2];
        abyte0[3] = abyte1[3];
    }

    public void writeDouble(byte abyte0[], int i, double value)
    {
        ByteArrayConverter.writeDouble(abyte0, i, value);
        byte abyte1[] = {
                abyte0[i+7], abyte0[i+6], abyte0[i+5], abyte0[i+4],
                abyte0[i+3], abyte0[i+2], abyte0[i+1], abyte0[i+0]
        };
        abyte0[0] = abyte1[0];
        abyte0[1] = abyte1[1];
        abyte0[2] = abyte1[2];
        abyte0[3] = abyte1[3];
        abyte0[4] = abyte1[4];
        abyte0[5] = abyte1[5];
        abyte0[6] = abyte1[6];
        abyte0[7] = abyte1[7];
    }
}
