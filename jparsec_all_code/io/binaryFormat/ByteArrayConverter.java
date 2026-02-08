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
 * The ByteArrayConverter class provides methods for writing, and reading
 * different primitive types to, and from an array of bytes.  All methods in
 * this class accept a byte array and an offset to perform the operation.
 * This class is useful for marshalling up arguments for send() calls in an
 * efficient fashion (i.e. as an alternative to sending Strings).<p>
 *
 * K.Galloway comment: Big Endian implementation
 *
 * @author Mark MacBeth
 */
public class ByteArrayConverter
{
	// private constructor so that this class cannot be instantiated.
	private ByteArrayConverter() {}

  /**
   * Reads a short value.
   * @param array The array containing the value.
   * @param offset The offset of the first bit of the value in the array.
   * @return The value.
   */
  public static short readShort(byte[] array, int offset) {
      /*
	every byte being recombined needs to have all high-end bits masked out.
	This is because of the implicit conversion to an integer when it is
	loaded from an array which causes a sign extension.  For instance, if
	the byte being extacted is 0x80, it gets converted into an integer with
	the value 0xffffff80.  If that bare integer value gets | (ORed) with
	the high byte, an unwanted negative sign extension can be introduced.
      */
      return (short)((array[offset] << 8) |
		   (array[offset + 1] & 0xff));
  }

    /**
     * Writes a short value.
     * @param array The array where the value will be written.
     * @param offset The offset of the first bit of the value in the array.
     * @param value The value to write.
     */
    public static void writeShort(byte[] array, int offset, short value) {
    	array[offset] = (byte)(value >> 8);
    	array[offset + 1] = (byte)value;
    }

    /**
     * Reads an integer value.
     * @param array The array containing the value.
     * @param offset The offset of the first bit of the value in the array.
     * @return The value.
     */
    public static int readInt(byte[] array, int offset) {
	return ((array[offset] << 24)              |
		((array[offset + 1] & 0xff) << 16) |
		((array[offset + 2] & 0xff) << 8)  |
		(array[offset + 3] & 0xff));
    }

    /**
     * Writes an integer value.
     * @param array The array where the value will be written.
     * @param offset The offset of the first bit of the value in the array.
     * @param value The value to write.
     */
    public static void writeInt(byte[] array, int offset, int value) {
		array[offset] =     (byte)(value >> 24);
		array[offset + 1] = (byte)(value >> 16);
		array[offset + 2] = (byte)(value >> 8);
		array[offset + 3] = (byte)value;
    }


    /**
     * Reads a float value.
     * @param array The array containing the value.
     * @param offset The offset of the first bit of the value in the array.
     * @return The value.
     */
    public static float readFloat(byte[] array, int offset) {
    	return Float.intBitsToFloat(readInt(array, offset));
    }

    /**
     * Writes a float value.
     * @param array The array where the value will be written.
     * @param offset The offset of the first bit of the value in the array.
     * @param value The value to write.
     */
    public static void writeFloat(byte[] array, int offset, float value) {
    	writeInt(array, offset, Float.floatToIntBits(value));
    }

    /**
     * Reads a double value.
     * @param array The array containing the value.
     * @param offset The offset of the first bit of the value in the array.
     * @return The value.
     */
    public static double readDouble(byte[] array, int offset) {
    	return Double.longBitsToDouble(readLong(array, offset));
    }

    /**
     * Writes a double value.
     * @param array The array where the value will be written.
     * @param offset The offset of the first bit of the value in the array.
     * @param value The value to write.
     */
    public static void writeDouble(byte[] array, int offset, double value) {
    	writeLong(array, offset, Double.doubleToLongBits(value));
    }

    /**
     * Reads a long value.
     * @param array The array containing the value.
     * @param offset The offset of the first bit of the value in the array.
     * @return The value.
     */
    public static long readLong(byte[] array, int offset) {
    	return (((long)array[offset] << 56)              |
    			((long)(array[offset + 1] & 0xff) << 48) |
    			((long)(array[offset + 2] & 0xff) << 40) |
    			((long)(array[offset + 3] & 0xff) << 32) |
    			((long)(array[offset + 4] & 0xff) << 24) |
    			((long)(array[offset + 5] & 0xff) << 16) |
    			((long)(array[offset + 6] & 0xff) << 8)  |
    			((long)array[offset + 7] & 0xff));
    }

    /**
     * Writes a long value.
     * @param array The array where the value will be written.
     * @param offset The offset of the first bit of the value in the array.
     * @param value The value to write.
     */
    public static void writeLong(byte[] array, int offset, long value) {
		array[offset] = (byte)(value >> 56);
		array[offset + 1] = (byte)(value >> 48);
		array[offset + 2] = (byte)(value >> 40);
		array[offset + 3] = (byte)(value >> 32);
		array[offset + 4] = (byte)(value >> 24);
		array[offset + 5] = (byte)(value >> 16);
		array[offset + 6] = (byte)(value >> 8);
		array[offset + 7] = (byte)value;
    }
}
