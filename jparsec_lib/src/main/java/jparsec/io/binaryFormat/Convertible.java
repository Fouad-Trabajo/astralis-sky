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

/**
 * Interface to convert data from the format stored in .30m files.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public interface Convertible
{
    /**
     * Reads a short value.
     * @param abyte0 The array containing the value.
     * @param i The offset of the first bit of the value in the array.
     * @return The value
     */
    public abstract short readShort(byte abyte0[], int i);

    /**
     * Reads an integer value.
     * @param abyte0 The array containing the value.
     * @param i The offset of the first bit of the value in the array.
     * @return The value
     */
    public abstract int readInt(byte abyte0[], int i);

    /**
     * Reads a double value.
     * @param abyte0 The array containing the value.
     * @param i The offset of the first bit of the value in the array.
     * @return The value
     */
    public abstract double readDouble(byte abyte0[], int i);

    /**
     * Reads a float value.
     * @param abyte0 The array containing the value.
     * @param i The offset of the first bit of the value in the array.
     * @return The value
     */
    public abstract float readFloat(byte abyte0[], int i);

    /**
     * Writes a short value.
     * @param abyte0 The array where the value will be written.
     * @param i The offset of the first bit of the value in the array.
     * @param value The value to write.
     */
    public abstract void writeShort(byte abyte0[], int i, short value);

    /**
     * Writes an integer value.
     * @param abyte0 The array where the value will be written.
     * @param i The offset of the first bit of the value in the array.
     * @param value The value to write.
     */
    public abstract void writeInt(byte abyte0[], int i, int value);

    /**
     * Writes a double value.
     * @param abyte0 The array where the value will be written.
     * @param i The offset of the first bit of the value in the array.
     * @param value The value to write.
     */
    public abstract void writeDouble(byte abyte0[], int i, double value);

    /**
     * Writes a float value.
     * @param abyte0 The array where the value will be written.
     * @param i The offset of the first bit of the value in the array.
     * @param value The value to write.
     */
    public abstract void writeFloat(byte abyte0[], int i, float value);
}
