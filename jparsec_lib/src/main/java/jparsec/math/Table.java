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
package jparsec.math;

import java.util.Arrays;
import java.util.BitSet;

import jparsec.ephem.Functions;
import jparsec.graph.DataSet;
import jparsec.io.ConsoleReport;
import jparsec.io.FileFormatElement;
import jparsec.io.FileIO;
import jparsec.io.WriteFile;
import jparsec.io.image.HeaderElement;
import jparsec.io.image.ImageSplineTransform;
import jparsec.util.JPARSECException;

/**
 * A handy class to manipulate 1d/2d/3d tables with measures, including
 * errors and a mask of pixels within calculations, optionally. This class
 * does not provide methods for resampling the data or interpolating,
 * operations that can be achieved using {@linkplain ImageSplineTransform}.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 * @see ImageSplineTransform
 */
public class Table {

	private MeasureElement data[][][];
	private boolean hasErrors = false;
	private BitSet mask[][];

	/**
	 * Constructor for a 1d table without errors.
	 * @param v The values.
	 * @param unit The unit for the values.
	 */
	public Table(double v[], String unit) {
		data = new MeasureElement[1][1][v.length];
		for (int i=0; i<v.length; i++) {
			data[0][0][i] = new MeasureElement(v[i], 0, unit);
		}
	}

	/**
	 * Constructor for a 2d table without errors.
	 * @param v The values.
	 * @param unit The unit for the values.
	 */
	public Table(double v[][], String unit) {
		data = new MeasureElement[1][v.length][v[0].length];
		for (int i=0; i<v[0].length; i++) {
			for (int j=0; j<v.length; j++) {
				data[0][j][i] = new MeasureElement(v[j][i], 0, unit);
			}
		}
	}

	/**
	 * Constructor for a 3d table without errors.
	 * @param v The values.
	 * @param unit The unit for the values.
	 */
	public Table(double v[][][], String unit) {
		data = new MeasureElement[v.length][v[0].length][v[0][0].length];
		for (int i=0; i<v[0][0].length; i++) {
			for (int j=0; j<v[0].length; j++) {
				for (int k=0; k<v.length; k++) {
					data[k][j][i] = new MeasureElement(v[k][j][i], 0, unit);
				}
			}
		}
	}

	/**
	 * Constructor for a 1d table with errors.
	 * @param v The values.
	 * @param dv The errors.
	 * @param unit The unit for the values.
	 */
	public Table(double v[], double dv[], String unit) {
		data = new MeasureElement[1][1][v.length];
		for (int i=0; i<v.length; i++) {
			data[0][0][i] = new MeasureElement(v[i], dv[i], unit);
		}
	}

	/**
	 * Constructor for a 2d table with errors.
	 * @param v The values.
	 * @param dv The errors.
	 * @param unit The unit for the values.
	 */
	public Table(double v[][], double dv[][], String unit) {
		data = new MeasureElement[1][v.length][v[0].length];
		for (int i=0; i<v[0].length; i++) {
			for (int j=0; j<v.length; j++) {
				data[0][j][i] = new MeasureElement(v[j][i], dv[j][i], unit);
			}
		}
	}

	/**
	 * Constructor for a 3d table with errors.
	 * @param v The values.
	 * @param dv The errors.
	 * @param unit The unit for the values.
	 */
	public Table(double v[][][], double dv[][][], String unit) {
		data = new MeasureElement[v.length][v[0].length][v[0][0].length];
		for (int i=0; i<v[0][0].length; i++) {
			for (int j=0; j<v[0].length; j++) {
				for (int k=0; k<v.length; k++) {
					data[k][j][i] = new MeasureElement(v[k][j][i], dv[k][j][i], unit);
				}
			}
		}
	}

	/**
	 * Constructor for a 3d table without errors.
	 * @param v The values.
	 * @param unit The unit for the values.
	 */
	public Table(int v[][][], String unit) {
		data = new MeasureElement[v.length][v[0].length][v[0][0].length];
		for (int i=0; i<v[0][0].length; i++) {
			for (int j=0; j<v[0].length; j++) {
				for (int k=0; k<v.length; k++) {
					data[k][j][i] = new MeasureElement(v[k][j][i], 0.0, unit);
				}
			}
		}
	}

	/**
	 * Constructor for a 2d table without errors.
	 * @param v The values.
	 * @param unit The unit for the values.
	 */
	public Table(int v[][], String unit) {
		data = new MeasureElement[1][v.length][v[0].length];
		for (int i=0; i<v[0].length; i++) {
			for (int j=0; j<v.length; j++) {
				data[0][j][i] = new MeasureElement(v[j][i], 0.0, unit);
			}
		}
	}

	/**
	 * Constructor for a 1d table with errors.
	 * @param v The values.
	 */
	public Table(MeasureElement v[]) {
		data = new MeasureElement[1][1][v.length];
		data[0][0] = v.clone();
		hasErrors = true;
	}

	/**
	 * Constructor for a 2d table with errors.
	 * @param v The values.
	 */
	public Table(MeasureElement v[][]) {
		data = new MeasureElement[1][v.length][v[0].length];
		data[0] = v.clone();
		hasErrors = true;
	}

	/**
	 * Constructor for a 3d table with errors.
	 * @param v The values.
	 */
	public Table(MeasureElement v[][][]) {
		data = v.clone();
		hasErrors = true;
	}

	/**
	 * Constructor to read a table from an String array. Values
	 * between the separator can be a number or an expression
	 * like 'a +/- b', 'a (b)', or even 'a b' to include a as
	 * value and b as error.
	 * @param table The array containing the values to be mapped
	 * to a table.
	 * @param separator The separator between the values.
	 * @param unit The unit.
	 */
	public Table(String table[], String separator, String unit) {
		int n = FileIO.getNumberOfFields(table[0], separator, false);
		data = new MeasureElement[0][table.length][n];
		for (int j=0; j<table.length; j++) {
			for (int i=0; i<n; i++) {
				String f = FileIO.getField(i+1, table[j], separator, false);
				int n2 = FileIO.getNumberOfFields(f, " ", false);
				if (n2 < 2) {
					data[0][j][i] = new MeasureElement(DataSet.parseDouble(f), 0, unit);
				} else {
					f = DataSet.replaceAll(f, "(", "", true);
					f = DataSet.replaceAll(f, ")", "", true);
					f = DataSet.replaceAll(f, "+/-", "", true);
					f = DataSet.replaceAll(f, "$\\pm$", "", true);
					f = DataSet.replaceAll(f, "&plusmn;", "", true);
					data[0][j][i] = new MeasureElement(DataSet.parseDouble(FileIO.getField(1, f, " ", true)),
							DataSet.parseDouble(FileIO.getField(2, f, " ", true)), unit);
					hasErrors = true;
				}
			}
		}
	}

	/**
	 * Adds a mask to the current table so that operations will not affect
	 * those values flagged as 'true' in the input mask.
	 * @param mask The input mask.
	 * @throws JPARSECException In case the mask cannot be applied to this table.
	 */
	public void addMask(boolean[] mask) throws JPARSECException {
		int s1 = data.length, s2 = data[0].length, s3 = data[0][0].length;
		if (s1 > 1 || s2 > 1 || s3 != mask.length) throw new JPARSECException("Cannot add this mask to the current table, dimensions/sizes incompatible.");

		this.mask = new BitSet[1][1];
		this.mask[0][0] = new BitSet(s3);
		for (int i=0; i<s3; i++) {
			if (mask[i]) this.mask[0][0].set(i);
		}
	}

	/**
	 * Changes the unit of the values in this Table to another value.
	 * @param unit The new unit.
	 */
	public void setUnit(String unit) {
		for (int i=0; i<data[0][0].length; i++) {
			for (int j=0; j<data[0].length; j++) {
				for (int k=0; k<data.length; k++) {
					data[k][j][i].unit = unit;
				}
			}
		}
	}

	/**
	 * Adds a mask to the current table so that operations will not affect
	 * those values flagged as 'true' in the input mask.
	 * @param mask The input mask.
	 * @param order_ij True in case the input mask array is ordered as
	 * mask[i][j], where i are the different columns and j the different rows.
	 * False for [j][i] ordering. Internally the array is ordered as [j][i].
	 * @throws JPARSECException In case the mask cannot be applied to this table.
	 */
	public void addMask(boolean[][] mask, boolean order_ij) throws JPARSECException {
		int s1 = data.length, s2 = data[0].length, s3 = data[0][0].length;
		if (order_ij) {
			if (s3 > 1 || s2 != mask.length || s1 != mask[0].length) throw new JPARSECException("Cannot add this mask to the current table, dimensions/sizes incompatible.");
		} else {
			if (s1 > 1 || s2 != mask.length || s3 != mask[0].length) throw new JPARSECException("Cannot add this mask to the current table, dimensions/sizes incompatible.");
		}

		this.mask = new BitSet[1][s2];
		for (int j=0; j<s2; j++) {
			this.mask[0][j] = new BitSet(s3);
			for (int i=0; i<s3; i++) {
				if (order_ij) {
					if (mask[i][j]) this.mask[0][j].set(i);
				} else {
					if (mask[j][i]) this.mask[0][j].set(i);
				}
			}
		}
	}

	/**
	 * Adds a mask to the current table so that operations will not affect
	 * those values flagged as 'true' in the input mask.
	 * @param mask The input mask.
	 * @param order_ijk True in case the input mask array is ordered as
	 * mask[i][j][k], where i are the different columns and j the different rows,
	 * while k different planes. False for [k][j][i] ordering. Internally the
	 * array is ordered as [k][j][i].
	 * @throws JPARSECException In case the mask cannot be applied to this table.
	 */
	public void addMask(boolean[][][] mask, boolean order_ijk) throws JPARSECException {
		int s1 = data.length, s2 = data[0].length, s3 = data[0][0].length;
		if (order_ijk) {
			if (s1 != mask[0][0].length || s2 != mask[0].length || s3 != mask.length) throw new JPARSECException("Cannot add this mask to the current table, dimensions/sizes incompatible.");
		} else {
			if (s1 != mask.length || s2 != mask[0].length || s3 != mask[0][0].length) throw new JPARSECException("Cannot add this mask to the current table, dimensions/sizes incompatible.");
		}

		this.mask = new BitSet[s1][s2];
		for (int k=0; k<s1; k++) {
			for (int j=0; j<s2; j++) {
				this.mask[k][j] = new BitSet(s3);
				for (int i=0; i<s3; i++) {
					if (order_ijk) {
						if (mask[i][j][k]) this.mask[k][j].set(i);
					} else {
						if (mask[k][j][i]) this.mask[k][j].set(i);
					}
				}
			}
		}
	}

	/**
	 * Adds a mask for all values in the table within a given range
	 * from a lower to an upper limit.
	 * @param lowerLimit Lower limit.
	 * @param upperLimit Upper limit.
	 * @param includell True to include in the mask values equals to
	 * the lower limit.
	 * @param includeul True to include in the mask values equals to
	 * the upper limit.
	 * @param addMode True to add the input range to the mask, false to
	 * reset the current mask and apply the input.
	 * @throws JPARSECException If an error occurs.
	 */
	public void addMask(double lowerLimit, double upperLimit, boolean
			includell, boolean includeul, boolean addMode) throws JPARSECException {
		int s1 = data.length, s2 = data[0].length, s3 = data[0][0].length;

		if (mask == null || !addMode) this.mask = new BitSet[s1][s2];
		for (int k=0; k<s1; k++) {
			for (int j=0; j<s2; j++) {
				this.mask[k][j] = new BitSet(s3);
				for (int i=0; i<s3; i++) {
					double val = this.data[k][j][i].getValue();
					if (includell && includeul) {
						if (val >= lowerLimit && val <= upperLimit) this.mask[k][j].set(i);
					} else {
						if (!includell && !includeul) {
							if (val > lowerLimit && val < upperLimit) this.mask[k][j].set(i);
						} else {
							if (!includell && includeul) {
								if (val > lowerLimit && val <= upperLimit) this.mask[k][j].set(i);
							} else {
								if (val >= lowerLimit && val < upperLimit) this.mask[k][j].set(i);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Add the values in a table to the current table.
	 * @param table The other table.
	 * @throws JPARSECException If the input table is
	 * not compatible with the current one.
	 */
	public void add(Table table) throws JPARSECException {
		if (!isCompatible(table)) throw new JPARSECException("Cannot make calculations with incompatible tables.");

		for (int i=0; i<data[0][0].length; i++) {
			for (int j=0; j<data[0].length; j++) {
				for (int k=0; k<data.length; k++) {
					if (mask == null || !mask[k][j].get(i)) data[k][j][i].add(table.data[k][j][i]);
				}
			}
		}
	}

	/**
	 * Substract the values in a table to the current table.
	 * @param table The other table.
	 * @throws JPARSECException If the input table is
	 * not compatible with the current one.
	 */
	public void subtract(Table table) throws JPARSECException {
		if (!isCompatible(table)) throw new JPARSECException("Cannot make calculations with incompatible tables.");

		for (int i=0; i<data[0][0].length; i++) {
			for (int j=0; j<data[0].length; j++) {
				for (int k=0; k<data.length; k++) {
					if (mask == null || !mask[k][j].get(i)) data[k][j][i].subtract(table.data[k][j][i]);
				}
			}
		}
	}

	/**
	 * Invert the values in a table.
	 * @throws JPARSECException Should never happen.
	 */
	public void invert() throws JPARSECException {
		for (int i=0; i<data[0][0].length; i++) {
			for (int j=0; j<data[0].length; j++) {
				for (int k=0; k<data.length; k++) {
					if (mask == null || !mask[k][j].get(i)) data[k][j][i].invert();
				}
			}
		}
	}

	/**
	 * Multiply the values in a table with the ones in the current table,
	 * one by one. This is not matrix product.
	 * @param table The other table.
	 * @throws JPARSECException If the input table is
	 * not compatible with the current one.
	 */
	public void multiply(Table table) throws JPARSECException {
		if (!isCompatible(table)) throw new JPARSECException("Cannot make calculations with incompatible tables.");

		for (int i=0; i<data[0][0].length; i++) {
			for (int j=0; j<data[0].length; j++) {
				for (int k=0; k<data.length; k++) {
					if (mask == null || !mask[k][j].get(i)) data[k][j][i].multiply(table.data[k][j][i]);
				}
			}
		}
	}

	/**
	 * Divide the values in a table with the ones in the current table,
	 * one by one.
	 * @param table The other table.
	 * @throws JPARSECException If the input table is
	 * not compatible with the current one.
	 */
	public void divide(Table table) throws JPARSECException {
		if (!isCompatible(table)) throw new JPARSECException("Cannot make calculations with incompatible tables.");

		for (int i=0; i<data[0][0].length; i++) {
			for (int j=0; j<data[0].length; j++) {
				for (int k=0; k<data.length; k++) {
					if (mask == null || !mask[k][j].get(i)) data[k][j][i].divide(table.data[k][j][i]);
				}
			}
		}
	}

	/**
	 * Multiply the values in the current table with a given value.
	 * @param a The value.
	 * @throws JPARSECException Should never happen.
	 */
	public void multiply(double a) throws JPARSECException {
		for (int i=0; i<data[0][0].length; i++) {
			for (int j=0; j<data[0].length; j++) {
				for (int k=0; k<data.length; k++) {
					if (mask == null || !mask[k][j].get(i)) data[k][j][i].multiply(a);
				}
			}
		}
	}

	/**
	 * Raises the values in the current table to the power of the ones
	 * in the input table, one by one.
	 * @param table The other table.
	 * @throws JPARSECException If the input table is
	 * not compatible with the current one.
	 */
	public void pow(Table table) throws JPARSECException {
		if (!isCompatible(table)) throw new JPARSECException("Cannot make calculations with incompatible tables.");

		for (int i=0; i<data[0][0].length; i++) {
			for (int j=0; j<data[0].length; j++) {
				for (int k=0; k<data.length; k++) {
					if (mask == null || !mask[k][j].get(i)) data[k][j][i].pow(table.data[k][j][i]);
				}
			}
		}
	}

	/**
	 * Raises the values in the current table to the power of the input value.
	 * @param a The value.
	 * @throws JPARSECException If the input table is
	 * not compatible with the current one.
	 */
	public void pow(double a) throws JPARSECException {
		for (int i=0; i<data[0][0].length; i++) {
			for (int j=0; j<data[0].length; j++) {
				for (int k=0; k<data.length; k++) {
					if (mask == null || !mask[k][j].get(i)) data[k][j][i].pow(a);
				}
			}
		}
	}

	/**
	 * Checks if this table is compatible with another one for calculations.
	 * Two tables are compatible if they have the same dimensions and sizes,
	 * and their units are convertible.
	 * @param table The other table.
	 * @return True or false.
	 */
	public boolean isCompatible(Table table) {
		int s1 = data.length, s2 = data[0].length, s3 = data[0][0].length;
		int st1 = table.data.length, st2 = table.data[0].length, st3 = table.data[0][0].length;
		if (s1 != st1 || s2 != st2 || s3 != st3) return false;

		if (data[0][0][0].unit == null && table.data[0][0][0].unit == null) return true;
		if (data[0][0][0].unit == null && table.data[0][0][0].unit != null) return false;
		if (data[0][0][0].unit != null && table.data[0][0][0].unit == null) return false;
		if (data[0][0][0].unit.equals(table.data[0][0][0].unit)) return true;
		try {
			Converter converter = new Converter(data[0][0][0].unit, table.data[0][0][0].unit);
			return converter.isConversionSupported();
		} catch (Exception exc) {
			return false;
		}
	}

	/**
	 * Returns if this instance is equals to another.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Table)) return false;

		Table table = (Table) o;

		if (hasErrors != table.hasErrors) return false;
		if (!Arrays.deepEquals(data, table.data)) return false;
		if (!Arrays.deepEquals(mask, table.mask)) return false;
		// Probably incorrect - comparing Object[] arrays with Arrays.equals
		if (!Arrays.equals(ist, table.ist)) return false;

		return Arrays.equals(istErr, table.istErr);
	}

	@Override
	public int hashCode() {
		int result = data != null ? Arrays.deepHashCode(data) : 0;
		result = 31 * result + (hasErrors ? 1 : 0);
		result = 31 * result + (mask != null ? Arrays.deepHashCode(mask) : 0);
		result = 31 * result + (ist != null ? Arrays.hashCode(ist) : 0);
		result = 31 * result + (istErr != null ? Arrays.hashCode(istErr) : 0);
		return result;
	}

	/**
	 * Clones this instance.
	 */
	@Override
	public Table clone()
	{
		Table t = null;
		try {
			t = new Table(this.getValues(), this.getErrors(), this.data[0][0][0].unit);
		} catch (Exception exc) {
			t = new Table(this.data.clone());
		}
		t.hasErrors = this.hasErrors;
		if (mask != null) {
			t.mask = new BitSet[mask.length][mask[0].length];
			for (int k=0; k<mask.length; k++) {
				for (int j=0; j<mask[0].length; j++) {
					t.mask[k][j] = new BitSet(mask[0][0].length());
					for (int i=0; i<mask[0][0].length(); i++) {
						if (mask[k][j].get(i)) t.mask[k][j].set(i);
					}
				}
			}
		}
		return t;
	}

	/**
	 * Returns the values in the table as an array.
	 * @return The array, ordered as k j i.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[][][] getValues() throws JPARSECException {
    	String unit = this.data[0][0][0].unit;
		int s1 = data.length, s2 = data[0].length, s3 = data[0][0].length;
		double out[][][] = new double[s1][s2][s3];
		for (int i=0; i<s3; i++) {
			for (int j=0; j<s2; j++) {
				for (int k=0; k<s1; k++) {
					if (data[k][j][i].unit.equals(unit)) {
						out[k][j][i] = data[k][j][i].getValue();
					} else {
						out[k][j][i] = data[k][j][i].get(unit).getValue();
					}
				}
			}
		}
		return out;
	}

	/**
	 * Returns the unit of the data.
	 * @return The unit for the first element in the table.
	 */
	public String getUnit() {
		return data[0][0][0].unit;
	}

	/**
	 * Returns the values in the table as an array of integers, using
	 * simple casting.
	 * @return The array, ordered as k j i.
	 * @throws JPARSECException If an error occurs.
	 */
	public int[][][] getValuesAsIntegers() throws JPARSECException {
    	String unit = this.data[0][0][0].unit;
		int s1 = data.length, s2 = data[0].length, s3 = data[0][0].length;
		int out[][][] = new int[s1][s2][s3];
		for (int i=0; i<s3; i++) {
			for (int j=0; j<s2; j++) {
				for (int k=0; k<s1; k++) {
					if (data[k][j][i].unit.equals(unit)) {
						out[k][j][i] = (int) data[k][j][i].getValue();
					} else {
						out[k][j][i] = (int) data[k][j][i].get(unit).getValue();
					}
				}
			}
		}
		return out;
	}

	/**
	 * Returns the values in the table as an array in a different unit.
	 * @param unit The new unit.
	 * @return The array.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[][][] getValues(String unit) throws JPARSECException {
		int s1 = data.length, s2 = data[0].length, s3 = data[0][0].length;
		double out[][][] = new double[s1][s2][s3];
		for (int i=0; i<s3; i++) {
			for (int j=0; j<s2; j++) {
				for (int k=0; k<s1; k++) {
					out[k][j][i] = data[k][j][i].get(unit).getValue();
				}
			}
		}
		return out;
	}

	/**
	 * Returns the values in the table as an array.
	 * @return The array.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[][][] getErrors() throws JPARSECException {
    	String unit = this.data[0][0][0].unit;
		int s1 = data.length, s2 = data[0].length, s3 = data[0][0].length;
		double out[][][] = new double[s1][s2][s3];
		for (int i=0; i<s3; i++) {
			for (int j=0; j<s2; j++) {
				for (int k=0; k<s1; k++) {
					if (data[k][j][i].unit.equals(unit)) {
						out[k][j][i] = data[k][j][i].error;
					} else {
						out[k][j][i] = data[k][j][i].get(unit).error;
					}
				}
			}
		}
		return out;
	}

	/**
	 * Returns the values in the table as an array in a different unit.
	 * @param unit The new unit.
	 * @return The array.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[][][] getErrors(String unit) throws JPARSECException {
		int s1 = data.length, s2 = data[0].length, s3 = data[0][0].length;
		double out[][][] = new double[s1][s2][s3];
		for (int i=0; i<s3; i++) {
			for (int j=0; j<s2; j++) {
				for (int k=0; k<s1; k++) {
					out[k][j][i] = data[k][j][i].get(unit).error;
				}
			}
		}
		return out;
	}

	/**
	 * Returns a simple string representation of this table.
	 * Separator is 3 blank spaces.
	 */
	@Override
	public String toString() {
		StringBuffer out = new StringBuffer("");
		String sep = FileIO.getLineSeparator(), fieldSep = "   ";
		for (int k=0; k<data.length; k++) {
			if (data.length > 1) out.append("k = "+k+sep+sep);
			for (int j=0; j<data[0].length; j++) {
				for (int i=0; i<data[0][0].length; i++) {
					if (!hasErrors) {
						out.append(data[k][j][i].value+fieldSep);
					} else {
						out.append(data[k][j][i].toString(true)+fieldSep);
					}
				}
				out.append(sep);
			}
		}
		return out.toString();
	}

	/**
	 * Returns a simple string representation of this table.
	 * @param fieldSep The separator string.
	 * @param format The format to be used to represent double
	 * values, in format conventions. For instance 'f3.6'.
	 * @return The table as a string.
	 */
	public String toString(String fieldSep, String format) {
		StringBuffer out = new StringBuffer("");
		String sep = FileIO.getLineSeparator();
		for (int k=0; k<data.length; k++) {
			if (data.length > 1) out.append("k = "+k+sep+sep);
			for (int j=0; j<data[0].length; j++) {
				for (int i=0; i<data[0][0].length; i++) {
					if (!hasErrors) {
						if (format == null) {
							out.append(data[k][j][i].value+fieldSep);
						} else {
							out.append(ConsoleReport.formatAsFortran(new String[] {data[k][j][i].value}, format, true)+fieldSep);
						}
					} else {
						out.append(data[k][j][i].toString(true)+fieldSep);
					}
				}
				out.append(sep);
			}
		}
		return out.toString();
	}

	/**
	 * Returns a string representation of this table for a given output format.
	 * @param format The output format for each of the values in each row.
	 * @throws JPARSECException If an error occurs.
	 * @return The table as a string.
	 */
	public String toString(FileFormatElement format[]) throws JPARSECException {
		StringBuffer out = new StringBuffer("");
		String sep = FileIO.getLineSeparator();
		for (int k=0; k<data.length; k++) {
			if (data.length > 1) out.append("k = "+k+sep+sep);
			for (int j=0; j<data[0].length; j++) {
				int nout = Math.min(format.length, data[0][0].length);
				HeaderElement p[] = new HeaderElement[nout];
				for (int i=0; i<nout; i++) {
					p[i] = new HeaderElement(DataSet.parseDouble(data[k][j][i].value), format[i].fieldName);
					p[i].key = p[i].comment;
				}
				out.append(WriteFile.getFormattedEntry(p, format) + sep);
			}
		}
		return out.toString();
	}

 	/**
 	 * Convolves the data in the table with a given beam or kernel. This method calls
 	 * {@linkplain #convolveMap(double[][][], double[][], int, int, float[], float, boolean)}
 	 * for both the data and the errors. Mask is not considered in this method.
 	 * @param beam_x Beam major axis size in pixels, for
 	 * {@linkplain #convolveGetGaussianKernel(double, double, double, double, double)}.
 	 * Spatial resolution is set to 1 and sampling to 4.
 	 * @param beam_y Beam minor size in pixels.
 	 * @param beam_pa Beam PA in degrees.
 	 * @throws JPARSECException If an error occurs.
 	 */
 	public void convolveMap(double beam_x, double beam_y, double beam_pa) throws JPARSECException {
 		double kernel[][] = convolveGetGaussianKernel(beam_x, beam_y, beam_pa, 1, 4);
 		double[][][] inCube = this.getValues();
 		double[][][] outCube = new double[inCube.length][inCube[0].length][inCube[0][0].length];
    	for (int y=0; y<outCube[0].length; y++)
		{
    		for (int x=0; x<outCube[0][0].length; x++)
    		{
    			double data[] = convolveMap(inCube, kernel, x, y, null, 0, true);
    			for (int i=0; i<data.length; i++) {
    				outCube[i][y][x] = data[i];
    			}
    		}
		}
    	String unit = this.data[0][0][0].unit;
    	for (int y=0; y<outCube[0].length; y++)
		{
    		for (int x=0; x<outCube[0][0].length; x++)
    		{
    			double data[] = convolveMap(inCube, kernel, x, y, null, 0, true);
    			for (int i=0; i<data.length; i++) {
    				//double val = Double.parseDouble(this.data[i][y][x].value);
    				this.data[i][y][x].value = ""+outCube[i][y][x];
    				this.data[i][y][x].unit = unit;
    				//this.data[i][y][x].error *= outCube[i][y][x] / val; // Simple scaling for errors
    			}
    		}
		}

    	// Same for errors
 		inCube = this.getErrors();
    	for (int y=0; y<outCube[0].length; y++)
		{
    		for (int x=0; x<outCube[0][0].length; x++)
    		{
    			double data[] = convolveMap(inCube, kernel, x, y, null, 0, true);
    			for (int i=0; i<data.length; i++) {
    				this.data[i][y][x].error = data[i];
    			}
    		}
		}
 	}

 	/**
 	 * Returns the maximum value in the table.
 	 * @return Maximum value.
 	 * @throws JPARSECException If an error occurs.
 	 */
 	public MeasureElement getMaximum() throws JPARSECException {
    	String unit = this.data[0][0][0].unit;
 		double[][][] inCube = this.getValues();
 		double[][][] dinCube = this.getErrors();
 		MeasureElement max = null;
		for (int i=0; i<data.length; i++) {
			double m = DataSet.getMaximumValue(inCube[i]);
			if (max == null || m > max.getValue()) {
				int p[] = DataSet.getIndexOfMaximum(inCube[i]);
				double e = dinCube[i][p[0]][p[1]];
				max = new MeasureElement(m, e, unit);
			}
		}
		return max;
 	}
 	/**
 	 * Returns the minimum value in the table.
 	 * @return Minimum value.
 	 * @throws JPARSECException If an error occurs.
 	 */
 	public MeasureElement getMinimum() throws JPARSECException {
    	String unit = this.data[0][0][0].unit;
 		double[][][] inCube = this.getValues();
 		double[][][] dinCube = this.getErrors();
 		MeasureElement min = null;
		for (int i=0; i<data.length; i++) {
			double m = DataSet.getMinimumValue(inCube[i]);
			if (min == null || m < min.getValue()) {
				int p[] = DataSet.getIndexOfMinimum(inCube[i]);
				double e = dinCube[i][p[0]][p[1]];
				min = new MeasureElement(m, e, unit);
			}
		}
		return min;
 	}
 	/**
 	 * Returns the index of the maximum value in the table.
 	 * @param unique True to return null in case there are several
 	 * positions in the table with the same maximum value, false
 	 * to return a result (one of those).
 	 * @return The index positions of the maximum.
 	 * @throws JPARSECException If an error occurs.
 	 */
 	public int[] getMaximumIndex(boolean unique) throws JPARSECException {
 		double[][][] inCube = this.getValues();
 		double max = -1;
 		int p[] = null, imax = -1, c = 0;
		for (int i=0; i<data.length; i++) {
			double m = DataSet.getMaximumValue(inCube[i]);
			if (m > max || max == -1) {
				max = m;
				p = DataSet.getIndexOfMaximum(inCube[i]);
				imax = i;
				c = 0;
				continue;
			}
			if (m == max && max != -1) c ++;
		}
		if (unique && c > 0) return null;
		return new int[] {imax, p[0], p[1]};
 	}

 	/**
 	 * Returns the index of the minimum value in the table.
 	 * @param unique True to return null in case there are several
 	 * positions in the table with the same minimum value, false
 	 * to return a result (one of those).
 	 * @return The index positions of the minimum.
 	 * @throws JPARSECException If an error occurs.
 	 */
 	public int[] getMinimumIndex(boolean unique) throws JPARSECException {
 		double[][][] inCube = this.getValues();
 		double min = -1;
 		int p[] = null, imin = -1, c = 0;
		for (int i=0; i<data.length; i++) {
			double m = DataSet.getMinimumValue(inCube[i]);
			if (m < min || min == -1) {
				min = m;
				p = DataSet.getIndexOfMinimum(inCube[i]);
				imin = i;
				c = 0;
				continue;
			}
			if (m == min && min != -1) c ++;
		}
		if (unique && c > 0) return null;
		return new int[] {imin, p[0], p[1]};
 	}

 	/**
 	 * Sets the value for a given index position. Data array is
 	 * ordered as data[k][j][i].
 	 * @param i Index for x position, last dimension.
 	 * @param j Index for y position, second dimension.
 	 * @param k Index for k (plane) index, first dimension.
 	 * @param m The new data.
 	 */
 	public void set(int i, int j, int k, MeasureElement m) {
 		this.data[k][j][i] = m.clone();
 	}

 	/**
 	 * Returns the value for a given index position. Data array is
 	 * ordered as data[k][j][i].
 	 * @param i Index for x position, last dimension.
 	 * @param j Index for y position, second dimension.
 	 * @param k Index for k (plane) index, first dimension.
 	 * @return data[k][j][i].
 	 */
 	public MeasureElement get(int i, int j, int k) {
 		return this.data[k][j][i];
 	}

 	/**
 	 * Returns the number of columns in the table.
 	 * @return Number of columns.
 	 */
 	public int getNcolumns() {
 		return data[0][0].length;
 	}
 	/**
 	 * Returns the number of rows in the table.
 	 * @return Number of rows.
 	 */
 	public int getNrows() {
 		return data[0].length;
 	}
 	/**
 	 * Returns the number of planes in the table.
 	 * @return Number of planes.
 	 */
 	public int getNPlanes() {
 		return data.length;
 	}

 	/**
 	 * Returns a row of the table.
 	 * @param plane Plane or image index.
 	 * @param row Row index.
 	 * @return The set of values for that row and plane.
 	 */
 	public MeasureElement[] getRowValues(int plane, int row) {
 		return data[plane][row];
 	}

 	/**
 	 * Returns the data in the Table.
 	 * @return The entire table.
 	 */
 	public MeasureElement[][][] getData() {
 		return data;
 	}

 	/**
 	 * Obtains the total flux in a given ring around certain position in the map.
 	 * @param ix Central 'x' position of the ring in the map (last dimension).
 	 * @param iy Central 'y' position of the ring in the map (2nd dimension).
 	 * @param rin Inner radius of the ring in pixels.
 	 * @param rout Outer radius of the ring in pixels.
 	 * @param average True to return the average flux instead of the total flux.
 	 * @return A new table containing one data for each of the planes in the
 	 * original table. In case of a table of size [3][100][100], returned table
 	 * will be of size [1][1][3]. For an unique 2d image it will be a single data,
 	 * size [1][1][1].
 	 * @throws JPARSECException If an error occurs.
 	 */
    public Table getFluxAround(double ix, double iy, double rin, double rout,
    		boolean average) throws JPARSECException {
    	double[][][] inCube = this.getValues();
    	MeasureElement out[] = new MeasureElement[inCube.length];
    	String unit = this.data[0][0][0].unit;
		double n = 0;
		for (int i=0; i<inCube.length; i++) {
			MeasureElement sum = new MeasureElement(0.0, 0.0, unit);
			n = 0;
	    	for (int y=0; y<inCube[0].length; y++)
			{
	    		for (int x=0; x<inCube[0][0].length; x++)
	    		{
	    			double r = FastMath.hypot(ix - x, iy - y);
	    			if (r >= rin && r < rout && (mask == null || !mask[i][y].get(x))) {
	    				sum.add(this.data[i][y][x]);
	    				n ++;
	    			}
	    		}
			}
	    	if (average) sum.multiply(1.0 / n);
	    	out[i] = sum;
		}
		return new Table(out);
    }

	/**
	 * Applies a median NEWS filter to remove noise. Based on code by John Burkardt.
	 * This method can be called few times to remove noise more effectively.
	 * @param aggressive Level of denoise intensity, from 0 to 2. The median is applied
	 * to a greater size around each pixel for level 2 (up to 21 pixels).
	 * @throws JPARSECException  If an error occurs.
	 */
	public void denoise(int aggressive) throws JPARSECException {
	  int i, j;
	  MeasureElement p[] = new MeasureElement[21];

	  int m = data[0].length, n = data[0][0].length;

	  for (int c=0; c<data.length; c++) {
		  MeasureElement rgb2[][] = new MeasureElement[m][n];

		  //  Process the main part of the image:
		  for ( i = 1; i < m - 1; i++ )
		  {
		    for ( j = 1; j < n - 1; j++ )
		    {
		      p[0] = data[c][i-1][j];
		      p[1] = data[c][i+1][j];
		      p[2] = data[c][i][j+1];
		      p[3] = data[c][i][j-1];
		      p[4] = data[c][i][j];

		      if (aggressive > 0 && i > 1 && i < m - 2 && j > 1 && j < n - 2) {
			      p[5] = data[c][i-1][j-1];
			      p[6] = data[c][i+1][j+1];
			      p[7] = data[c][i-1][j+1];
			      p[8] = data[c][i+1][j-1];
			      p[9] = data[c][i-2][j];
			      p[10] = data[c][i+2][j];
			      p[11] = data[c][i][j+2];
			      p[12] = data[c][i][j-2];
			      if (aggressive > 1) {
				      p[13] = data[c][i-2][j+1];
				      p[14] = data[c][i-2][j-1];
				      p[15] = data[c][i+2][j+1];
				      p[16] = data[c][i+2][j-1];
				      p[17] = data[c][i-1][j+2];
				      p[18] = data[c][i-1][j-2];
				      p[19] = data[c][i+1][j+2];
				      p[20] = data[c][i+1][j-2];

			    	  rgb2[i][j] = DataSet.getKthSmallestValue (p, p.length, p.length/2); //i4vec_median ( 5, p );
			      } else {
			    	  rgb2[i][j] = DataSet.getKthSmallestValue (p, 13, 13/2); //i4vec_median ( 5, p );
			      }
		      } else {
			      rgb2[i][j] = DataSet.getKthSmallestValue (p, 5, 5/2); //i4vec_median ( 5, p );
		      }
		    }
		  }
		  //  Process the four borders.
		  //  Get an odd number of data points,
		  for ( i = 1; i < m - 1; i++ )
		  {
			  j = 0;
		      p[0] = data[c][i-1][j];
		      p[1] = data[c][i+1][j];
		      p[2] = data[c][i][j];
		      p[3] = data[c][i][j+1];
		      p[4] = data[c][i][j+2];

		      if (aggressive > 0) {
			      p[5] = data[c][i+1][j+1];
			      p[6] = data[c][i-1][j+1];
			      if (aggressive > 1 && i > 1 && i < m - 2 && j > 1 && j < n - 2) {
				      p[7] = data[c][i+2][j];
				      p[8] = data[c][i-2][j];
				      p[9] = data[c][i+1][j+2];
				      p[10] = data[c][i-1][j+2];
			    	  rgb2[i][j] = DataSet.getKthSmallestValue (p, 11, 11/2);
			      } else {
			    	  rgb2[i][j] = DataSet.getKthSmallestValue (p, 7, 7/2);
			      }
		      } else {
		    	  rgb2[i][j] = DataSet.getKthSmallestValue (p, 5, 5/2);
		      }

		      j = n - 1;
		      p[0] = data[c][i-1][j];
		      p[1] = data[c][i+1][j];
		      p[2] = data[c][i][j-2];
		      p[3] = data[c][i][j-1];
		      p[4] = data[c][i][j];

		      if (aggressive > 0) {
			      p[5] = data[c][i+1][j-1];
			      p[6] = data[c][i-1][j-1];
			      if (aggressive > 1 && i > 1 && i < m - 2 && j > 1 && j < n - 2) {
				      p[7] = data[c][i+2][j];
				      p[8] = data[c][i-2][j];
				      p[9] = data[c][i+1][j-2];
				      p[10] = data[c][i-1][j-2];
			    	  rgb2[i][j] = DataSet.getKthSmallestValue (p, 11, 11/2);
			      } else {
			    	  rgb2[i][j] = DataSet.getKthSmallestValue (p, 7, 7/2);
			      }
		      } else {
		    	  rgb2[i][j] = DataSet.getKthSmallestValue (p, 5, 5/2);
		      }
		  }

		  for ( j = 1; j < n - 1; j++ )
		  {
			  i = 0;
		      p[0] = data[c][i][j];
		      p[1] = data[c][i+1][j];
		      p[2] = data[c][i+2][j];
		      p[3] = data[c][i][j-1];
		      p[4] = data[c][i][j+1];

		      if (aggressive > 0) {
			      p[5] = data[c][i+1][j+1];
			      p[6] = data[c][i+1][j-1];
			      if (aggressive > 1 && i > 1 && i < m - 2 && j > 1 && j < n - 2) {
				      p[7] = data[c][i][j+2];
				      p[8] = data[c][i][j-1];
				      p[9] = data[c][i+2][j+1];
				      p[10] = data[c][i+2][j-1];
			    	  rgb2[i][j] = DataSet.getKthSmallestValue (p, 11, 11/2);
			      } else {
			    	  rgb2[i][j] = DataSet.getKthSmallestValue (p, 7, 7/2);
			      }
		      } else {
		    	  rgb2[i][j] = DataSet.getKthSmallestValue (p, 5, 5/2);
		      }

		      i = m - 1;
		      p[0] = data[c][i-2][j];
		      p[1] = data[c][i-1][j];
		      p[2] = data[c][i][j];
		      p[3] = data[c][i][j-1];
		      p[4] = data[c][i][j+1];

		      if (aggressive > 0) {
			      p[5] = data[c][i-1][j-1];
			      p[6] = data[c][i-1][j+1];
			      if (aggressive > 1 && i > 1 && i < m - 2 && j > 1 && j < n - 2) {
				      p[7] = data[c][i][j+2];
				      p[8] = data[c][i][j-1];
				      p[9] = data[c][i-2][j+1];
				      p[10] = data[c][i-2][j-1];
			    	  rgb2[i][j] = DataSet.getKthSmallestValue (p, 11, 11/2);
			      } else {
			    	  rgb2[i][j] = DataSet.getKthSmallestValue (p, 7, 7/2);
			      }
		      } else {
		    	  rgb2[i][j] = DataSet.getKthSmallestValue (p, 5, 5/2);
		      }
		  }

		  //  Process the four corners.
		  i = 0;
		  j = 0;
	      p[0] = data[c][i+1][j];
	      p[1] = data[c][i][j];
	      p[2] = data[c][i][j+1];
	      if (aggressive > 0) {
		      p[3] = data[c][i+1][j+1];
		      p[4] = data[c][i+2][j];
	    	  rgb2[i][j] = DataSet.getKthSmallestValue (p, 5, 5/2);
	      } else {
	    	  rgb2[i][j] = DataSet.getKthSmallestValue (p, 3, 3/2);
	      }

		  i = 0;
		  j = n - 1;
	      p[0] = data[c][i+1][j];
	      p[1] = data[c][i][j];
	      p[2] = data[c][i][j-1];
	      if (aggressive > 0) {
		      p[3] = data[c][i+1][j-1];
		      p[4] = data[c][i+2][j];
	    	  rgb2[i][j] = DataSet.getKthSmallestValue (p, 5, 5/2);
	      } else {
	    	  rgb2[i][j] = DataSet.getKthSmallestValue (p, 3, 3/2);
	      }
		  i = m - 1;
		  j = 0;
	      p[0] = data[c][i-1][j];
	      p[1] = data[c][i][j];
	      p[2] = data[c][i][j+1];
	      if (aggressive > 0) {
		      p[3] = data[c][i-1][j+1];
		      p[4] = data[c][i-2][j];
	    	  rgb2[i][j] = DataSet.getKthSmallestValue (p, 5, 5/2);
	      } else {
	    	  rgb2[i][j] = DataSet.getKthSmallestValue (p, 3, 3/2);
	      }

		  i = m - 1;
		  j = n - 1;
	      p[0] = data[c][i-1][j];
	      p[1] = data[c][i][j];
	      p[2] = data[c][i][j-1];
	      if (aggressive > 0) {
		      p[3] = data[c][i-1][j-1];
		      p[4] = data[c][i-2][j];
	    	  rgb2[i][j] = DataSet.getKthSmallestValue (p, 5, 5/2);
	      } else {
	    	  rgb2[i][j] = DataSet.getKthSmallestValue (p, 3, 3/2);
	      }

	      data[c] = rgb2;
	  }
	}

 	/**
 	 * Returns the maximum value in the table for a given column (second dimension).
 	 * @param column The column index.
 	 * @return Maximum value.
 	 * @throws JPARSECException If an error occurs.
 	 */
 	public MeasureElement getMaximum(int column) throws JPARSECException {
    	String unit = this.data[0][0][0].unit;
 		double[][][] inCube = this.getValues();
 		double[][][] dinCube = this.getErrors();
 		MeasureElement max = null;
		for (int i=0; i<data.length; i++) {
			double m = DataSet.getMaximumValue(inCube[i][column]);
			if (max == null || m > max.getValue()) {
				int p = DataSet.getIndexOfMaximum(inCube[i][column]);
				double e = dinCube[i][column][p];
				max = new MeasureElement(m, e, unit);
			}
		}
		return max;
 	}
 	/**
 	 * Returns the minimum value in the table for a given column (second dimension).
 	 * @param column The column index.
 	 * @return Minimum value.
 	 * @throws JPARSECException If an error occurs.
 	 */
 	public MeasureElement getMinimum(int column) throws JPARSECException {
    	String unit = this.data[0][0][0].unit;
 		double[][][] inCube = this.getValues();
 		double[][][] dinCube = this.getErrors();
 		MeasureElement min = null;
		for (int i=0; i<data.length; i++) {
			double m = DataSet.getMinimumValue(inCube[i][column]);
			if (min == null || m < min.getValue()) {
				int p = DataSet.getIndexOfMinimum(inCube[i][column]);
				double e = dinCube[i][column][p];
				min = new MeasureElement(m, e, unit);
			}
		}
		return min;
 	}
 	/**
 	 * Returns the index of the maximum value in the table for a given column.
 	 * @param column The column index.
 	 * @param unique True to return null in case there are several
 	 * positions in the table with the same maximum value, false
 	 * to return a result (one of those).
 	 * @return The index positions of the maximum. First value is the plane
 	 * number, usually 0 for single images, the second the row index.
 	 * @throws JPARSECException If an error occurs.
 	 */
 	public int[] getMaximumIndex(int column, boolean unique) throws JPARSECException {
 		double[][][] inCube = this.getValues();
 		double max = -1;
 		int p = -1, imax = -1, c = 0;
		for (int i=0; i<data.length; i++) {
			double m = DataSet.getMaximumValue(inCube[i][column]);
			if (m > max || max == -1) {
				max = m;
				p = DataSet.getIndexOfMaximum(inCube[i][column]);
				imax = i;
				c = 0;
				continue;
			}
			if (m == max && max != -1) c ++;
		}
		if (unique && c > 0) return null;
		return new int[] {imax, p};
 	}

 	/**
 	 * Returns the index of the minimum value in the table for a given column.
 	 * @param column The column index.
 	 * @param unique True to return null in case there are several
 	 * positions in the table with the same minimum value, false
 	 * to return a result (one of those).
 	 * @return The index positions of the minimum. First value is the plane
 	 * number, usually 0 for single images, the second the row index.
 	 * @throws JPARSECException If an error occurs.
 	 */
 	public int[] getMinimumIndex(int column, boolean unique) throws JPARSECException {
 		double[][][] inCube = this.getValues();
 		double min = -1;
 		int p = -1, imin = -1, c = 0;
		for (int i=0; i<data.length; i++) {
			double m = DataSet.getMinimumValue(inCube[i][column]);
			if (m < min || min == -1) {
				min = m;
				p = DataSet.getIndexOfMinimum(inCube[i][column]);
				imin = i;
				c = 0;
				continue;
			}
			if (m == min && min != -1) c ++;
		}
		if (unique && c > 0) return null;
		return new int[] {imin, p};
 	}

 	/**
 	 * Reduces the table to those elements in which the values in certain column are
 	 * within a given range.
 	 * @param plane The plane or image number, 0 for the first (0 always if there's only one).
 	 * @param column The column index.
 	 * @param minValue The minimum value for that column so that the row will be preserved.
 	 * @param maxValue The maximum value for that column so that the row will be preserved.
 	 * @throws JPARSECException If an error occurs.
 	 */
 	public void selectElements(int plane, int column, double minValue, double maxValue) throws JPARSECException {
 		MeasureElement copy[][] = data[plane].clone();
		for (int i=copy.length-1; i>= 0; i--) {
			double v = copy[i][column].getValue();
			if (v < minValue || v > maxValue) {
				copy = (MeasureElement[][]) DataSet.deleteIndex(copy, i);
				if (mask != null) mask[plane] = (BitSet[]) DataSet.deleteIndex(mask[plane], i);
			}
		}
		data[plane] = copy;
 	}

 	/**
 	 * Reduces the table to those elements in which the values in certain column are
 	 * outside a given range.
 	 * @param plane The plane or image number, 0 for the first (0 always if there's only one).
 	 * @param column The column index.
 	 * @param minValue The minimum value for that column so that the row will be removed.
 	 * @param maxValue The maximum value for that column so that the row will be removed.
 	 * @throws JPARSECException If an error occurs.
 	 */
 	public void removeElements(int plane, int column, double minValue, double maxValue) throws JPARSECException {
 		MeasureElement copy[][] = data[plane].clone();
		for (int i=copy.length-1; i>= 0; i--) {
			double v = copy[i][column].getValue();
			if (v >= minValue && v <= maxValue) {
				copy = (MeasureElement[][]) DataSet.deleteIndex(copy, i);
				if (mask != null) mask[plane] = (BitSet[]) DataSet.deleteIndex(mask[plane], i);
			}
		}
		data[plane] = copy;
 	}

 	/**
 	 * Returns a column of the table.
 	 * @param plane Plane or image index.
 	 * @param column Column index.
 	 * @return The set of values for that column and plane.
 	 */
 	public MeasureElement[] getColumnValues(int plane, int column) {
 		int n = data[plane].length;
 		MeasureElement out[] = new MeasureElement[n];
 		for (int i = 0; i < n; i ++) {
 			out[i] = data[plane][i][column];
 		}
 		return out;
 	}

 	/**
 	 * Sets the values for the table in a given plane and column.
 	 * @param plane Plane index.
 	 * @param column Column index.
 	 * @param val The values to set that plane and column to.
 	 */
 	public void setColumnValues(int plane, int column, MeasureElement val[]) {
 		int n = data[plane].length;
 		if (val.length < n) n = val.length;
 		for (int i = 0; i < n; i ++) {
 			data[plane][i][column] = val[i];
 		}
 	}

 	/**
 	 * Returns the mask value at certain position.
 	 * @param plane Plane number of the image, usually 0.
 	 * @param column Column index.
 	 * @param row Row index.
 	 * @return True or false.
 	 */
 	public boolean getMaskValue(int plane, int column, int row) {
 		return mask[plane][row].get(column);
 	}

 	/**
 	 * Returns the number of dimensions in this table.
 	 * @return 1, 2, or 3.
 	 */
 	public int getDimensions() {
 		if (data.length > 1) return 3;
 		if (data[0].length > 1) return 2;
 		return 1;
 	}

 	/**
 	 * Resamples the data to a given number of elements in each of the
 	 * three dimensions. The mask is removed.
 	 * @param max1 New number of dimensions in x.
 	 * @param max2 New number of dimensions in y.
 	 * @param max3 New number of dimensions in z.
 	 * @throws JPARSECException If an error occurs.
 	 */
 	public void resample(int max1, int max2, int max3) throws JPARSECException {
 		// FIXME: flux conserved ?
 		mask = null;
 		MeasureElement data2[][][] = new MeasureElement[max3][max2][max1];
 		for (int z=0;z<max3;z++) {
 			double z0 = 0;
 			if (data.length > 1) z0 = ((z / (max3 - 1.0)) * (data.length - 1.0));
 	 		for (int y=0;y<max2;y++) {
 	 			double y0 = 0;
 	 			if (data[0].length > 1) y0 = ((y / (max2 - 1.0)) * (data[0].length - 1.0));
 	 			double fx0 = 0.0;
 	 			if (data[0][0].length > 1) fx0 = ((1.0 / (max1 - 1.0)) * (data[0][0].length - 1.0));
 	 	 		for (int x=0;x<max1;x++) {
 	 	 			double x0 = (x * fx0);
 	 	 			data2[z][y][x] = this.interpolate(x0, y0, z0);
 	 	 		}
 	 		}
 		}
 		data = data2;
 		ist = null;
 	}

 	private ImageSplineTransform ist[] = null;
 	private ImageSplineTransform istErr[] = null;

 	/**
 	 * Interpolates within the data.
 	 * @param x The x index position.
 	 * @param y The y index position.
 	 * @param z The z index position.
 	 * @return The interpolated value.
 	 * @throws JPARSECException If an error occurs.
 	 */
 	public MeasureElement interpolate(double x, double y, double z) throws JPARSECException {
 		// FIXME
/* 		if (x == (int) x && y == (int) y && z == (int) z) {
 			return data[(int) z][(int) x][(int) y];
 		}
*/
 		if (ist == null) {
 			double[][][] data = this.getValues();
 			double[][][] dataErr = this.getErrors();
 			ist = new ImageSplineTransform[data.length];
 			istErr = new ImageSplineTransform[data.length];
 			for (int i=0; i<data.length; i++) {
 				ist[i] = new ImageSplineTransform(2, data[i]);
 				istErr[i] = new ImageSplineTransform(2, dataErr[i]);
 			}
 		}

 		int prev = (int) z;
 		double prevVal = ist[prev].interpolate(y, x, 3);
 		double prevErr = istErr[prev].interpolate(y, x, 3);
 		if (z == (int) z)
 			return new MeasureElement(prevVal, prevErr, data[0][0][0].unit);
 		int next = prev + 1;
 		double nextVal = ist[next].interpolate(y, x, 3);
 		double nextErr = istErr[next].interpolate(y, x, 3);
 		return new MeasureElement(prevVal + (nextVal-prevVal)*(z-prev), prevErr + (nextErr-prevErr)*(z-prev), data[0][0][0].unit);
 	}
 	
 	/**
 	 * Returns a Gaussian kernel for the convolution.
 	 * @param beam_x Major axis of the beam in arcsec.
 	 * @param beam_y Minor axis of the beam in arcsec.
 	 * @param beam_pa Beam position angle in degrees.
 	 * @param spatialResolution Spatial resolution of the output kernel in arcseconds.
 	 * Since cube and kernel coordinates are mapped by indexes, the resolution of the
 	 * kernel must be the same of that of the cube to convolve.
 	 * @param samplingFactor The radius of the kernel relative to the beam size (HPBW).
 	 * Recommended value is around 4 for an accurate convolution, and should never be below 0.5.
 	 * @return The kernel. Values outside the sampled region are filled with 0.
 	 */
 	public static double[][] convolveGetGaussianKernel(double beam_x, double beam_y, double beam_pa, double spatialResolution,
 			double samplingFactor) {
		// Now we adapt the beam sizes according to GILDAS. Simply
		// take in mind that beam_y should be greater than beam_x.
		// P.A. is measured from y to -x axis sense, but this would
		// be inside user's mind...
		if (beam_y < beam_x)
		{
			double tmp = beam_y;
			beam_y = beam_x;
			beam_x = tmp;
		}

		double distance = 100; // it doesn't matter
		double mapStep = spatialResolution * distance;
		int index_beam_x_max = (int) (Functions.sec2ua(beam_x*samplingFactor, distance) / mapStep + 0.5);
		int index_beam_y_max = (int) (Functions.sec2ua(beam_y*samplingFactor, distance) / mapStep + 0.5);
		double cte = -0.5 * 8.0 * Math.log(2.0);
		int sx = 2 * index_beam_x_max + 1;
		int sy = 2 * index_beam_y_max + 1;
		double kernel[][] = new double[sx][sy];
		for (int ix = 0; ix <= 2*index_beam_x_max; ix++)
		{
			for (int iy = 0; iy <= 2*index_beam_y_max; iy++)
			{
				kernel[ix][iy] = 0.0;

				double dx = Functions.ua2sec((ix-index_beam_x_max) * mapStep, distance);
				double dy = Functions.ua2sec((iy-index_beam_y_max) * mapStep, distance);

				double cosa = Math.cos(-beam_pa * Constant.DEG_TO_RAD);
				double sina = Math.sin(-beam_pa * Constant.DEG_TO_RAD);
				double factor_x = (dx * dx * cosa * cosa + dy * dy * sina * sina - 2.0 * dx * dy * cosa * sina) / (beam_x * beam_x);
				double factor_y = (dy * dy * cosa * cosa + dx * dx * sina * sina + 2.0 * dx * dy * cosa * sina) / (beam_y * beam_y);

				if (FastMath.hypot(factor_x, factor_y) > samplingFactor) continue;
				if (beam_x == 0.0 && beam_y == 0.0) {
					factor_x = 1.0;
					factor_y = 1.0;
				}
				double factor = Math.exp(cte * (factor_x + factor_y));

				kernel[ix][iy] = factor;
			}
		}
		return kernel;
 	}

 	/**
 	 * Convolves a given cube with a given beam or kernel. NaN points are considered as
 	 * points with inexistent data, so that they will not contribute to the output
 	 * intensity or the normalization factor in the convolution process.
 	 * @param cube The cube data to convolve, ordered as levels, DEC, RA.
 	 * @param kernel The kernel to use, for instance a Gaussian profile from
 	 * {@linkplain #convolveGetGaussianKernel(double, double, double, double, double)}.
 	 * @param index_x_obs The position index to apply the convolution in RA.
 	 * @param index_y_obs The position index to apply the convolution in DEC.
 	 * @param emissionZero In case there's information about the flux at offset 0 0 (where
 	 * the cube array has usually no data in case the number of dimensions is even) for each channel,
 	 * set it here, otherwise null.
 	 * @param convolveOutsideMapValue Value to consider as intensity in points outside the input map.
 	 * Set as 0 or positive to consider points outside the map in the convolution process (with flux =
 	 * input value), otherwise set a negative value to avoid convolving outside the map.
 	 * @param fixNaN Set to true to fix all possible NaN values so that the output convolution
 	 * will contain no NaN. Set to false to conserve as NaN those points that are NaN in the input
 	 * cube and are coincident with the convolution point.
 	 * @return The convolved and normalized spectrum data for the input position and each channel.
 	 * @throws JPARSECException If an error occurs.
 	 */
 	public static float[] convolveMap(float[][][] cube, double[][] kernel,
 			int index_x_obs, int index_y_obs, float[] emissionZero, float convolveOutsideMapValue,
 			boolean fixNaN) throws JPARSECException {
		int resolution = cube.length;

		// Declare output spectrum
		float out[] = new float[resolution];
		float[] factor_sum = new float[resolution];
		for (int iz = 0; iz < resolution; iz++)
		{
			out[iz] = 0.0f;
			factor_sum[iz] = 0.0f;
		}

		// Obtain maximum indexes
		int index_x_max = cube[0][0].length-1;
		int index_y_max = cube[0].length-1;

		// Make convolution. Note we calculate the angle (offset observed -
		// certain offset) from the y axis ('north') towards the -x axis
		// ('east'), according to GILDAS. Also, the convolution process takes 4 complete
		// beams around the convolution point (index_x_obs, index_y_obs), doing the
		// appropriate things if we go outside the spectrum map
		int index_beam_x_max = kernel.length / 2;
		int index_beam_y_max = kernel[0].length / 2;

		int dx0 = index_x_obs - index_beam_x_max;
		int dy0 = index_y_obs - index_beam_y_max;
		for (int ix = index_x_obs - index_beam_x_max; ix <= index_x_obs + index_beam_x_max; ix++)
		{
			if (convolveOutsideMapValue < 0 && (ix < 0 || ix > index_x_max)) continue;

			for (int iy = index_y_obs - index_beam_y_max; iy <= index_y_obs + index_beam_y_max; iy++)
			{
				if (ix-dx0 < 0 || ix-dx0 >= kernel.length) continue;
				if (iy-dy0 < 0 || iy-dy0 >= kernel[0].length) continue;

				double factor = kernel[ix-dx0][iy-dy0];
				if (factor == 0.0) continue;

				if (ix >= 0 && ix <= index_x_max && iy >= 0 && iy <= index_y_max)
				{
					for (int iz = 0; iz < resolution; iz++)
					{
						float value = cube[iz][iy][ix];
	        			if (!Float.isInfinite(value) && !Float.isNaN(value)) {
							factor_sum[iz] += factor;
	        				out[iz] += (float) (value * factor);
	        			}
					}
				} else
				{
					if (convolveOutsideMapValue >= 0) // Consider points outside map as points that contributes to normalization factor and (possibly) emission
						for (int iz = 0; iz < resolution; iz++)
						{
							factor_sum[iz] += factor;
	        				out[iz] += (float) (convolveOutsideMapValue * factor);
						}
				}
			}
		}

		// Add offset 0 0 if necessary
		if (emissionZero != null) {
			// Offset 0 0 position as a matrix index (not integer since emissionZero != null when model doesn't cut offset 0 0)
			double index_x0 = index_x_max/2.0;
			double index_y0 = index_y_max/2.0;

			double ix = (index_x0 - index_x_obs) + index_beam_x_max, iy = (index_y0 - index_y_obs) + index_beam_y_max;
			if (ix >= 0 && ix <= kernel.length-1 && iy >= 0 && iy <= kernel[0].length-1) {
				double factor = 0.0;
				if (ix == (int) ix && iy == (int) iy) {
					factor = kernel[(int)ix][(int)iy];
				} else {
					ImageSplineTransform ist = new ImageSplineTransform(3, kernel);
					factor = ist.interpolate(ix, iy);
				}

				for (int iz = 0; iz < resolution; iz++)
				{
					float value = emissionZero[iz];
        			if (!Double.isInfinite(value) && !Double.isNaN(value)) {
						factor_sum[iz] += factor;
						out[iz] = out[iz] + (float) (value * factor);
        			}
				}
			}
		}

		// Normalize output spectrum in each velocity channel
		for (int iz = 0; iz < resolution; iz++)
		{
			out[iz] = out[iz] / factor_sum[iz];
			// Recover NaN value in case most of the contribution comes from a NaN point
			if (!fixNaN && (Double.isInfinite(cube[iz][index_y_obs][index_x_obs]) || Double.isNaN(cube[iz][index_y_obs][index_x_obs])))
				out[iz] = Float.NaN;
		}

		return out;
 	}

 	/**
 	 * Convolves a given cube with a given beam or kernel. NaN points are considered as
 	 * points with inexistent data, so that they will not contribute to the output
 	 * intensity or the normalization factor in the convolution process.
 	 * @param cube The cube data to convolve, ordered as levels, DEC, RA.
 	 * @param kernel The kernel to use, for instance a Gaussian profile from
 	 * {@linkplain #convolveGetGaussianKernel(double, double, double, double, double)}.
 	 * @param index_x_obs The position index to apply the convolution in RA.
 	 * @param index_y_obs The position index to apply the convolution in DEC.
 	 * @param emissionZero In case there's information about the flux at offset 0 0 (where
 	 * the cube array has usually no data in case the number of dimensions is even) for each channel,
 	 * set it here, otherwise null.
 	 * @param convolveOutsideMapValue Value to consider as intensity in points outside the input map.
 	 * Set as 0 or positive to consider points outside the map in the convolution process (with flux =
 	 * input value), otherwise set a negative value to avoid convolving outside the map.
 	 * @param fixNaN Set to true to fix all possible NaN values so that the output convolution
 	 * will contain no NaN. Set to false to conserve as NaN those points that are NaN in the input
 	 * cube and are coincident with the convolution point.
 	 * @return The convolved and normalized spectrum data for the input position and each channel.
 	 * @throws JPARSECException If an error occurs.
 	 */
 	public static double[] convolveMap(double[][][] cube, double[][] kernel,
 			int index_x_obs, int index_y_obs, float[] emissionZero, float convolveOutsideMapValue,
 			boolean fixNaN) throws JPARSECException {
		int resolution = cube.length;

		// Declare output spectrum
		double out[] = new double[resolution];
		double[] factor_sum = new double[resolution];
		for (int iz = 0; iz < resolution; iz++)
		{
			out[iz] = 0.0f;
			factor_sum[iz] = 0.0f;
		}

		// Obtain maximum indexes
		int index_x_max = cube[0][0].length-1;
		int index_y_max = cube[0].length-1;

		// Make convolution. Note we calculate the angle (offset observed -
		// certain offset) from the y axis ('north') towards the -x axis
		// ('east'), according to GILDAS. Also, the convolution process takes 4 complete
		// beams around the convolution point (index_x_obs, index_y_obs), doing the
		// appropriate things if we go outside the spectrum map
		int index_beam_x_max = kernel.length / 2;
		int index_beam_y_max = kernel[0].length / 2;

		int dx0 = index_x_obs - index_beam_x_max;
		int dy0 = index_y_obs - index_beam_y_max;
		for (int ix = index_x_obs - index_beam_x_max; ix <= index_x_obs + index_beam_x_max; ix++)
		{
			if (convolveOutsideMapValue < 0 && (ix < 0 || ix > index_x_max)) continue;

			for (int iy = index_y_obs - index_beam_y_max; iy <= index_y_obs + index_beam_y_max; iy++)
			{
				if (ix-dx0 < 0 || ix-dx0 >= kernel.length) continue;
				if (iy-dy0 < 0 || iy-dy0 >= kernel[0].length) continue;

				double factor = kernel[ix-dx0][iy-dy0];
				if (factor == 0.0) continue;

				if (ix >= 0 && ix <= index_x_max && iy >= 0 && iy <= index_y_max)
				{
					for (int iz = 0; iz < resolution; iz++)
					{
						double value = cube[iz][iy][ix];
	        			if (!Double.isInfinite(value) && !Double.isNaN(value)) {
							factor_sum[iz] += factor;
	        				out[iz] += (value * factor);
	        			}
					}
				} else
				{
					if (convolveOutsideMapValue >= 0) // Consider points outside map as points that contributes to normalization factor and (possibly) emission
						for (int iz = 0; iz < resolution; iz++)
						{
							factor_sum[iz] += factor;
	        				out[iz] += (convolveOutsideMapValue * factor);
						}
				}
			}
		}

		// Add offset 0 0 if necessary
		if (emissionZero != null) {
			// Offset 0 0 position as a matrix index (not integer since emissionZero != null when model doesn't cut offset 0 0)
			double index_x0 = index_x_max/2.0;
			double index_y0 = index_y_max/2.0;

			double ix = (index_x0 - index_x_obs) + index_beam_x_max, iy = (index_y0 - index_y_obs) + index_beam_y_max;
			if (ix >= 0 && ix <= kernel.length-1 && iy >= 0 && iy <= kernel[0].length-1) {
				double factor = 0.0;
				if (ix == (int) ix && iy == (int) iy) {
					factor = kernel[(int)ix][(int)iy];
				} else {
					ImageSplineTransform ist = new ImageSplineTransform(3, kernel);
					factor = ist.interpolate(ix, iy);
				}

				for (int iz = 0; iz < resolution; iz++)
				{
					double value = emissionZero[iz];
        			if (!Double.isInfinite(value) && !Double.isNaN(value)) {
						factor_sum[iz] += factor;
						out[iz] = out[iz] + (value * factor);
        			}
				}
			}
		}

		// Normalize output spectrum in each velocity channel
		for (int iz = 0; iz < resolution; iz++)
		{
			out[iz] = (out[iz] / factor_sum[iz]);
			// Recover NaN value in case most of the contribution comes from a NaN point
			if (!fixNaN && (Double.isInfinite(cube[iz][index_y_obs][index_x_obs]) || Double.isNaN(cube[iz][index_y_obs][index_x_obs])))
				out[iz] = Double.NaN;
		}

		return out;
 	}
}
