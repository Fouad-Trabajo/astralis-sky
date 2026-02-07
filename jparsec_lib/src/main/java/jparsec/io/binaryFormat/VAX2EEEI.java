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

import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Logger.LEVEL;

/**
 * Implementation of the VAX format for reading binary files written with those computers.<P>
 * To write double values is not supported in VAX format, since
 * it is not a fully reversible operation and to do that is not recommended.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class VAX2EEEI
    implements Convertible, Serializable
{
	private static final long serialVersionUID = 1L;
	/**
	 * Default constructor.
	 */
    public VAX2EEEI() { }

    public short readShort(byte abyte0[], int i)
    {
        return ByteArrayConverter.readShort(new byte[] {abyte0[i + 1], abyte0[i]}, 0);
    }


    public int readInt(byte abyte0[], int i)
    {
        return ByteArrayConverter.readInt(new byte[] {abyte0[i + 3], abyte0[i + 2], abyte0[i + 1], abyte0[i]}, 0);
    }

    /**
     * Reads a float value.
     */
    public float readFloat(byte abyte0[], int i)
    {
        byte abyte1[] = {
        		abyte0[i + 1], abyte0[i], abyte0[i + 3], abyte0[i + 2]
        };
        float f = ByteArrayConverter.readFloat(abyte1, 0) / 4F;
        return f;
    }
    /**
     * Writes a double value. The double is rounded to a float and it
     * can only be written if it is in the range 0.293873588E-38 to 1.7014117E38
     * (the range of the VAX D float), otherwise it launches a RuntimeException.
     * @param abyte0 The array where the value will be written.
     * @param i The offset of the first bit of the value in the array.
     * @param value The value to write.
     */
    public void writeDouble(byte abyte0[], int i, double value)
    {
    	for (int ii=0; ii<8; ii++) {
    		abyte0[ii] = (byte) 0;
    	}
    	writeFloat(abyte0, 0, (float) value);

    	double check = readDouble(abyte0, i);
    	double dif = Math.abs((check-value) / value);
    	if (dif > 1E-6) {
    		if (Logger.reportJPARSECLogs) {
    			Logger.log(LEVEL.ERROR, "could not convert "+value+" to VAX format.");
    		} else {
    			try {
					JPARSECException.addWarning("could not convert "+value+" to VAX format.");
				} catch (JPARSECException e) {
	    			Logger.log(LEVEL.ERROR, "could not convert "+value+" to VAX format.");
				}
    		}
    	}
    }

    public double readDouble(byte abyte0[], int i)
    {
        byte abyte1[] = new byte[8];
        abyte1[7] = abyte0[i + 6];
        abyte1[6] = abyte0[i + 7];
        abyte1[5] = abyte0[i + 4];
        abyte1[4] = abyte0[i + 5];
        abyte1[3] = abyte0[i + 2];
        abyte1[2] = abyte0[i + 3];
        abyte1[1] = abyte0[i + 0];
        abyte1[0] = abyte0[i + 1];
        if(ByteArrayConverter.readInt(abyte1, 0) == 0 && ByteArrayConverter.readInt(abyte1, 4) == 0)
            return 0.0D;
        if(ByteArrayConverter.readInt(abyte1, 0) != 0)
        {
            short word0 = (short)(ByteArrayConverter.readShort(abyte1, 0) & 0x7f80);
            short word1 = (short)(ByteArrayConverter.readShort(abyte1, 0) & 0xffff8000);
            ByteArrayConverter.writeInt(abyte1, 0, ByteArrayConverter.readInt(abyte1, 0) & 0x7fffff);
            long l = ByteArrayConverter.readLong(abyte1, 0);
            l >>= 3;
            ByteArrayConverter.writeLong(abyte1, 0, l);
            word0 /= 128;
            word0 -= 128;
            word0 += 1024;
            word0 *= 16;
            ByteArrayConverter.writeShort(abyte1, 0, (short)(ByteArrayConverter.readShort(abyte1, 0) | word0));
            ByteArrayConverter.writeShort(abyte1, 0, (short)(ByteArrayConverter.readShort(abyte1, 0) | word1));
        } else
        {
            ByteArrayConverter.writeShort(abyte1, 0, (short)14336);
        }
        return 0.25D * ByteArrayConverter.readDouble(abyte1, 0);
    }

    public void writeShort(byte abyte0[], int i, short value)
    {
        ByteArrayConverter.writeShort(abyte0, i, value);
        byte abyte1[] = {
                abyte0[1], abyte0[0]
            };
        abyte0[0] = abyte1[0];
        abyte0[1] = abyte1[1];
    }

    public void writeInt(byte abyte0[], int i, int value)
    {
        ByteArrayConverter.writeInt(abyte0, i, value);
        byte abyte1[] = {
                abyte0[3 + i], abyte0[2 + i], abyte0[1 + i], abyte0[ i]
            };
        abyte0[0] = abyte1[0];
        abyte0[1] = abyte1[1];
        abyte0[2] = abyte1[2];
        abyte0[3] = abyte1[3];
    }

    public void writeFloat(byte abyte0[], int i, float value)
    {
        ByteArrayConverter.writeFloat(abyte0, i, value*4f);
        byte abyte1[] = {
                abyte0[1+i], abyte0[0+i], abyte0[3+i], abyte0[2+i]
            };
        abyte0[0] = abyte1[0];
        abyte0[1] = abyte1[1];
        abyte0[2] = abyte1[2];
        abyte0[3] = abyte1[3];
    }
}
