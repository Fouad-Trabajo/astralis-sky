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
package jparsec.graph;


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import jparsec.io.FileIO;
import jparsec.math.Evaluation;
import jparsec.math.FastMath;
import jparsec.math.MeasureElement;
import jparsec.util.JPARSECException;
import nom.tam.util.ArrayFuncs;

/**
 * A class to perform useful operations with datasets.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class DataSet
{
	// private constructor so that this class cannot be instantiated.
	private DataSet() {}

	/**
	 * Creates an xy dataset of values from a given function.
	 *
	 * @param function Function f(x) as it is written in Java.
	 * @param x0 Initial x value of the dataset.
	 * @param xf Final x value.
	 * @param npoints Number of points.
	 * @return A ArrayList with two arrays of doubles: the x and the y values.
	 * @throws JPARSECException If an error occurs.
	 */
	public static ArrayList<double[]> createXYDataset(String function, double x0, double xf, int npoints)
	throws JPARSECException {
		ArrayList<double[]> v = new ArrayList<double[]>();

		double x[] = new double[npoints];
		double y[] = new double[npoints];
		double step = (xf - x0) / (npoints - 1.0);
		int i = 0;
		for (double px = x0; px <= xf; px = px + step)
		{
			Evaluation eval = new Evaluation(function, new String[] {"x "+px, "y 0.0", "z 0.0"});
			double py = eval.evaluate();

			x[i] = px;
			y[i] = py;
			i++;
		}

		v.add(x);
		v.add(y);
		return v;
	}

	/**
	 * Creates an array of values by applying a math operation to all the
	 * values. For example, a function "5*x" creates a new array with the same
	 * values multiplied by 5.
	 *
	 * @param function Function f(x) as it is written in Java.
	 * @param val Array of values.
	 * @return An array with the result of the operation applied to each value
	 *         in the array.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double[] applyFunction(String function, double val[])
	throws JPARSECException {
		int npoints = val.length;
		double out[] = new double[npoints];

		for (int i = 0; i < npoints; i++)
		{
			Evaluation eval = new Evaluation(function, new String[] {"x "+val[i]});
			double p = eval.evaluate();

			out[i] = p;
		}

		return out;
	}

	/**
	 * Obtains minimum value of an array.
	 * Possible NaN values are ignored.
	 *
	 * @param v Array.
	 * @return Minimum value.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double getMinimumValue(double v[])
	throws JPARSECException {
		if (v == null) throw new JPARSECException("no points in the input data.");
		if (v.length < 1) throw new JPARSECException("no points in the input data.");

		double min = v[0];
		if (v.length > 1) {
			for (int i = 1; i < v.length; i++)
			{
				if ((v[i] < min && (!Double.isNaN(v[i]) && !Double.isInfinite(v[i]))) || (Double.isNaN(min) || Double.isInfinite(min)))
					min = v[i];
			}
		}

		return min;
	}

	/**
	 * Obtains maximum value of an array. Possible NaN values are ignored.
	 *
	 * @param v Array.
	 * @return Maximum value. -1 is returned as an invalid result.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double getMaximumValue(double v[])
	throws JPARSECException {
		if (v == null) throw new JPARSECException("invalid input.");
		if (v.length < 1) throw new JPARSECException("invalid input.");
		double max = v[0];
		if (v.length > 1) {
			for (int i = 1; i < v.length; i++)
			{
				if ((v[i] > max && (!Double.isNaN(v[i]) && !Double.isInfinite(v[i]))) || (Double.isNaN(max) || Double.isInfinite(max)))
					max = v[i];
			}
		}

		return max;
	}
	/**
	 * Obtains maximum value of an array. Possible NaN values are ignored.
	 *
	 * @param data Array.
	 * @return Maximum value. -1 is returned as an invalid result
	 * (if all input values are NaN).
	 * @throws JPARSECException If an error occurs.
	 */
	public static double getMaximumValue(double data[][])
	throws JPARSECException {
		if (data == null) throw new JPARSECException("invalid input.");
		if (data.length < 1) throw new JPARSECException("invalid input.");

		double max = -1;
		for (int i=0; i<data.length; i++) {
			double mmax = DataSet.getMaximumValue(data[i]);
			if ((mmax > max || max == -1) && (!Double.isNaN(mmax) && !Double.isInfinite(mmax))) max = mmax;
		}
		return max;
	}

	/**
	 * Obtains maximum value of an array. Possible NaN values are ignored.
	 *
	 * @param data Array.
	 * @return The index of the maximum for the first and second dimensions
	 * of the array. input[x][y] -&gt; (x, y).
	 * @throws JPARSECException If an error occurs.
	 */
	public static int[] getIndexOfMaximum(double data[][])
	throws JPARSECException {
		if (data == null) throw new JPARSECException("invalid input.");
		if (data.length < 1) throw new JPARSECException("invalid input.");

		double max = -1;
		int x = -1, y = -1;
		for (int i=0; i<data.length; i++) {
			double mmax = DataSet.getMaximumValue(data[i]);
			if ((mmax > max || max == -1) && (!Double.isNaN(mmax) && !Double.isInfinite(mmax))) {
				max = mmax;
				x = i;
				y = DataSet.getIndexOfMaximum(data[i]);
			}
		}
		return new int[] {x, y};
	}

	/**
	 * Obtains minimum value of an array. Possible NaN values are ignored.
	 *
	 * @param data Array.
	 * @return Minimum value. -1 is returned as an invalid result
	 * (if all input values are NaN).
	 * @throws JPARSECException If an error occurs.
	 */
	public static double getMinimumValue(double data[][])
	throws JPARSECException {
		if (data == null) throw new JPARSECException("invalid input.");
		if (data.length < 1) throw new JPARSECException("invalid input.");

		double min = -1;
		for (int i=0; i<data.length; i++) {
			double mmin = DataSet.getMinimumValue(data[i]);
			if ((mmin < min || min == -1) && (!Double.isNaN(mmin) && !Double.isInfinite(mmin))) min = mmin;
		}
		return min;
	}

	/**
	 * Obtains minimum value of an array. Possible NaN values are ignored.
	 *
	 * @param data Array.
	 * @return The index of the minimum for the first and second dimensions
	 * of the array. input[x][y] -&gt; (x, y).
	 * @throws JPARSECException If an error occurs.
	 */
	public static int[] getIndexOfMinimum(double data[][])
	throws JPARSECException {
		if (data == null) throw new JPARSECException("invalid input.");
		if (data.length < 1) throw new JPARSECException("invalid input.");

		double min = -1;
		int x = -1, y = -1;
		for (int i=0; i<data.length; i++) {
			double mmin = DataSet.getMinimumValue(data[i]);
			if ((mmin < min || min == -1) && (!Double.isNaN(mmin) && !Double.isInfinite(mmin))) {
				min = mmin;
				x = i;
				y = DataSet.getIndexOfMinimum(data[i]);
			}
		}
		return new int[] {x, y};
	}

	/**
	 * Obtains index of the minimum value of an array of doubles.
	 * Possible NaN values are ignored.
	 *
	 * @param x Array.
	 * @return Index of the minimum value. -1 is returned as an invalid result.
	 * @throws JPARSECException If an error occurs.
	 */
	public static int getIndexOfMinimum(double x[])
	throws JPARSECException {
		if (x == null) throw new JPARSECException("invalid input.");
		if (x.length < 1) throw new JPARSECException("invalid input.");

		int min = 0;
		if (x.length > 1) {
			for (int i = 1; i < x.length; i++)
			{
				if ((x[i] < x[min] && (!Double.isNaN(x[i]) && !Double.isInfinite(x[i]))) || (Double.isNaN(x[min]) || Double.isInfinite(x[min])))
					min = i;
			}
		}

		return min;
	}

	/**
	 * Obtains index of the maximum value of an array of doubles.
	 * Possible NaN values are ignored.
	 *
	 * @param x Array.
	 * @return Index of the maximum value.
	 * @throws JPARSECException If an error occurs.
	 */
	public static int getIndexOfMaximum(double x[])
	throws JPARSECException {
		if (x == null) throw new JPARSECException("invalid input.");
		if (x.length < 1) throw new JPARSECException("invalid input.");

		int max = 0;
		if (x.length > 1) {
			for (int i = 1; i < x.length; i++)
			{
				if ((x[i] > x[max] && (!Double.isNaN(x[i]) && !Double.isInfinite(x[i]))) || (Double.isNaN(x[max]) || Double.isInfinite(x[max])))
					max = i;
			}
		}

		return max;
	}

	/**
	 * Obtains string array representing an array of double values.
	 *
	 * @param array Input array of doubles.
	 * @return String array.
	 */
	public static String[] toStringValues(double[] array)
	{
		String out[] = new String[array.length];

		for (int i = 0; i < array.length; i++)
		{
			out[i] = "" + array[i];
		}
		return out;
	}

	/**
	 * Obtains string array representing an array of int values.
	 *
	 * @param array Input array of ints.
	 * @return String array.
	 */
	public static String[] toStringValues(int[] array)
	{
		String out[] = new String[array.length];

		for (int i = 0; i < array.length; i++)
		{
			out[i] = "" + array[i];
		}
		return out;
	}

	/**
	 * Obtains string array representing an array of float values.
	 *
	 * @param array Input array of floats.
	 * @return String array.
	 */
	public static String[] toStringValues(float[] array)
	{
		String out[] = new String[array.length];

		for (int i = 0; i < array.length; i++)
		{
			out[i] = "" + array[i];
		}
		return out;
	}

	/**
	 * Obtains double array representing an array of string values. Maths
	 * are allowed.
	 *
	 * @param array Input array of strings.
	 * @return Double array.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double[] toDoubleValues(String[] array) throws JPARSECException
	{
		double out[] = new double[array.length];

		for (int i = 0; i < array.length; i++)
		{
			out[i] = DataSet.getDoubleValueWithoutLimit(array[i]);
		}
		return out;
	}

	/**
	 * Obtains float array representing an array of string values. Maths
	 * are allowed.
	 *
	 * @param array Input array of strings.
	 * @return Float array.
	 * @throws JPARSECException If an error occurs.
	 */
	public static float[] toFloatValues(String[] array) throws JPARSECException
	{
		float out[] = new float[array.length];

		for (int i = 0; i < array.length; i++)
		{
			out[i] = (float) DataSet.getDoubleValueWithoutLimit(array[i]);
		}
		return out;
	}

	/**
	 * Obtains double array representing an array of string values. Maths
	 * are allowed.
	 *
	 * @param array Input array of strings.
	 * @return Double array.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double[][] toDoubleValues(String[][] array) throws JPARSECException
	{
		double out[][] = new double[array.length][];

		for (int i = 0; i < array.length; i++)
		{
			out[i] = toDoubleValues(array[i]);
		}
		return out;
	}

	/**
	 * Obtains a subDataset taking all points with x greater or equal to certain value.
	 *
	 * @param x X values.
	 * @param y Y values.
	 * @param dx X errors. Can be null.
	 * @param dy Y errors. Can be null.
	 * @param xmin X minimum.
	 * @return A ArrayList with x, y (strings), dx, and dy (double) arrays.
	 * @throws JPARSECException If an error occurs.
	 */
	public static ArrayList<Object> subDatasetFromXMinimum(String x[], String y[], double dx[], double dy[], double xmin)
			throws JPARSECException
	{
		double xx[] = (double[]) DataSet.getDoubleValuesIncludingLimits(x).get(0);
		int np = 0;
		for (int i = 0; i < x.length; i++)
		{
			if (xx[i] >= xmin)
				np++;
		}
		String xo[] = new String[np];
		String yo[] = new String[np];
		double dxo[] = new double[np];
		double dyo[] = new double[np];
		np = -1;
		for (int i = 0; i < x.length; i++)
		{
			if (xx[i] >= xmin)
			{
				np++;
				xo[np] = x[i];
				yo[np] = y[i];

				if (dx != null)
					dxo[np] = dx[i];
				if (dy != null)
					dyo[np] = dy[i];
			}
		}

		ArrayList<Object> v = new ArrayList<Object>();
		v.add(xo);
		v.add(yo);
		v.add(dxo);
		v.add(dyo);
		return v;
	}

	/**
	 * Obtains a subDataset taking all points with x lower or equal to certain value.
	 *
	 * @param x X values.
	 * @param y Y values.
	 * @param dx X errors. Can be null.
	 * @param dy Y errors. Can be null.
	 * @param xmax X maximum.
	 * @return A ArrayList with x, y (strings), dx, and dy (double) arrays.
	 * @throws JPARSECException If an error occurs.
	 */
	public static ArrayList<Object> subDatasetFromXMaximum(String x[], String y[], double dx[], double dy[], double xmax)
			throws JPARSECException
	{
		double xx[] = (double[]) DataSet.getDoubleValuesIncludingLimits(x).get(0);
		int np = 0;
		for (int i = 0; i < x.length; i++)
		{
			if (xx[i] <= xmax)
				np++;
		}
		String xo[] = new String[np];
		String yo[] = new String[np];
		double dxo[] = new double[np];
		double dyo[] = new double[np];
		np = -1;
		for (int i = 0; i < x.length; i++)
		{
			if (xx[i] <= xmax)
			{
				np++;
				xo[np] = x[i];
				yo[np] = y[i];

				if (dx != null)
					dxo[np] = dx[i];
				if (dy != null)
					dyo[np] = dy[i];
			}
		}

		ArrayList<Object> v = new ArrayList<Object>();
		v.add(xo);
		v.add(yo);
		v.add(dxo);
		v.add(dyo);
		return v;
	}

	/**
	 * Obtains a subDataset taking all points without limits.
	 *
	 * @param x X values.
	 * @param y Y values.
	 * @param dx X errors. Can be null.
	 * @param dy Y errors. Can be null.
	 * @return A ArrayList with x, y (strings), dx, and dy (double) arrays.
	 * @throws JPARSECException If an error occurs.
	 */
	public static ArrayList<double[]> subDatasetWithoutLimits(String x[], String y[], double dx[], double dy[])
			throws JPARSECException
	{
		int nlimits = 0;
		for (int i = 0; i < x.length; i++)
		{
			String valX = x[i];
			String valY = y[i];
			if (valX.startsWith("<") || valX.startsWith(">") || valY.startsWith("<") || valY.startsWith(">"))
			{
				nlimits++;
			}
		}

		double outX[] = new double[x.length - nlimits];
		double outY[] = new double[x.length - nlimits];
		double outdX[] = new double[x.length - nlimits];
		double outdY[] = new double[x.length - nlimits];
		int n = -1;
		for (int i = 0; i < x.length; i++)
		{
			String valX = x[i];
			String valY = y[i];
			if (!valX.startsWith("<") && !valX.startsWith(">") && !valY.startsWith("<") && !valY.startsWith(">"))
			{
				n++;
				outX[n] = tryToConvertToDouble(valX);
				outY[n] = tryToConvertToDouble(valY);
				outdX[n] = dx[i];
				outdY[n] = dy[i];
			}
		}

		ArrayList<double[]> out = new ArrayList<double[]>();
		out.add(outX);
		out.add(outY);
		out.add(outdX);
		out.add(outdY);

		return out;
	}

	/**
	 * Obtain double precision values of an array of strings, including
	 * possible upper and lower limits, such as "&lt;5" or "&gt;5". Math operations
	 * are allowed, for example "5*3" in any component.
	 *
	 * @param arg Array of strings.
	 * @return A ArrayList with two componentes, the array of double values, and an
	 *         array of int values with 1 for upper limits and -1 for lowers.
	 * @throws JPARSECException If an error occurs.
	 */
	public static ArrayList<Object> getDoubleValuesIncludingLimits(String arg[]) throws JPARSECException
	{
		ArrayList<Object> v = new ArrayList<Object>();
		double out[] = new double[arg.length];
		int limit[] = new int[arg.length];

		for (int i = 0; i < arg.length; i++)
		{
			String val = arg[i];
			limit[i] = 0;
			if (val.startsWith("<") || val.startsWith(">"))
			{
				limit[i] = 1;
				if (val.startsWith(">"))
					limit[i] = -1;
				val = val.substring(1);
			}
			out[i] = tryToConvertToDouble(val);
		}
		v.add(out);
		v.add(limit);
		return v;
	}

	/**
	 * Obtain double precision values of an array of strings, excluding
	 * possible upper and lower limits, such as "&lt;5" or "&gt;5". Math operations
	 * are allowed, for example "5*3" in any component.
	 *
	 * @param arg Array of strings.
	 * @return The array of double values.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double[] getDoubleValuesExcludingLimits(String arg[]) throws JPARSECException
	{
		int nlimits = 0;
		for (int i = 0; i < arg.length; i++)
		{
			String val = arg[i];
			if (val.startsWith("<") || val.startsWith(">"))
			{
				nlimits++;
			}
		}

		double out[] = new double[arg.length - nlimits];
		int n = -1;
		for (int i = 0; i < arg.length; i++)
		{
			String val = arg[i];
			if (!val.startsWith("<") && !val.startsWith(">"))
			{
				n++;
				out[n] = tryToConvertToDouble(val);
			}
		}

		return out;
	}

	/**
	 * Obtain double precision values of an array of strings, excluding
	 * possible upper and lower limits, such as "&lt;5" or "&gt;5". Math operations
	 * are allowed, for example "5*3" in any component.
	 *
	 * @param arg Array of strings, with numbers or math operations, and with or
	 *        without limit information.
	 * @param limit_arg Array of strings to be also checked, if, for example,
	 *        limits are set only in one axis and you want to exclude points
	 *        with limits in the other axis.
	 * @return The array of double values in arg with no limit information.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double[] getDoubleValuesExcludingLimits(String arg[], String limit_arg[]) throws JPARSECException
	{
		int nlimits = 0;
		for (int i = 0; i < arg.length; i++)
		{
			String val = arg[i];
			if (val.startsWith("<") || val.startsWith(">") || limit_arg[i].startsWith("<") || limit_arg[i]
					.startsWith(">"))
			{
				nlimits++;
			}
		}

		double out[] = new double[arg.length - nlimits];
		int n = -1;
		for (int i = 0; i < arg.length; i++)
		{
			String val = arg[i];
			String val_limit = limit_arg[i];
			if (!val.startsWith("<") && !val.startsWith(">") && !val_limit.startsWith("<") && !val_limit
					.startsWith(">"))
			{
				n++;
				out[n] = tryToConvertToDouble(val);
			}
		}

		return out;
	}

	/**
	 * Obtains a subDataset taking all points with x greater or equal to certain value.
	 *
	 * @param x X values.
	 * @param y Y values.
	 * @param dx X errors. Can be null.
	 * @param dy Y errors. Can be null.
	 * @param xmin X minimum.
	 * @return A ArrayList with x, y (doubles), dx, and dy (doubles) arrays.
	 * @throws JPARSECException If an error occurs.
	 */
	public static ArrayList<double[]> subDatasetFromXMinimum(double x[], double y[], double dx[], double dy[], double xmin)
			throws JPARSECException
	{
		int np = 0;
		for (int i = 0; i < x.length; i++)
		{
			if (x[i] >= xmin)
				np++;
		}
		double xo[] = new double[np];
		double yo[] = new double[np];
		double dxo[] = new double[np];
		double dyo[] = new double[np];
		np = -1;
		for (int i = 0; i < x.length; i++)
		{
			if (x[i] >= xmin)
			{
				np++;
				xo[np] = x[i];
				yo[np] = y[i];

				if (dx != null)
					dxo[np] = dx[i];
				if (dy != null)
					dyo[np] = dy[i];
			}
		}

		ArrayList<double[]> v = new ArrayList<double[]>();
		v.add(xo);
		v.add(yo);
		v.add(dxo);
		v.add(dyo);
		return v;
	}

	/**
	 * Obtains a subDataset taking all points with x lower or equal to certain value.
	 *
	 * @param x X values.
	 * @param y Y values.
	 * @param dx X errors. Can be null.
	 * @param dy Y errors. Can be null.
	 * @param xmax X maximum.
	 * @return A ArrayList with x, y (doubles), dx, and dy (doubles) arrays.
	 * @throws JPARSECException If an error occurs.
	 */
	public static ArrayList<double[]> subDatasetFromXMaximum(double x[], double y[], double dx[], double dy[], double xmax)
			throws JPARSECException
	{
		int np = 0;
		for (int i = 0; i < x.length; i++)
		{
			if (x[i] <= xmax)
				np++;
		}
		double xo[] = new double[np];
		double yo[] = new double[np];
		double dxo[] = new double[np];
		double dyo[] = new double[np];
		np = -1;
		for (int i = 0; i < x.length; i++)
		{
			if (x[i] <= xmax)
			{
				np++;
				xo[np] = x[i];
				yo[np] = y[i];

				if (dx != null)
					dxo[np] = dx[i];
				if (dy != null)
					dyo[np] = dy[i];
			}
		}

		ArrayList<double[]> v = new ArrayList<double[]>();
		v.add(xo);
		v.add(yo);
		v.add(dxo);
		v.add(dyo);
		return v;
	}

	/**
	 * Obtains a subDataset taking all points with y greater or equal to certain value.
	 *
	 * @param x X values.
	 * @param y Y values.
	 * @param dx X errors. Can be null.
	 * @param dy Y errors. Can be null.
	 * @param ymin Y minimum.
	 * @return A ArrayList with x, y (doubles), dx, and dy (doubles) arrays.
	 * @throws JPARSECException If an error occurs.
	 */
	public static ArrayList<double[]> subDatasetFromYMinimum(double x[], double y[], double dx[], double dy[], double ymin)
			throws JPARSECException
	{
		int np = 0;
		for (int i = 0; i < x.length; i++)
		{
			if (y[i] >= ymin)
				np++;
		}
		double xo[] = new double[np];
		double yo[] = new double[np];
		double dxo[] = new double[np];
		double dyo[] = new double[np];
		np = -1;
		for (int i = 0; i < x.length; i++)
		{
			if (y[i] >= ymin)
			{
				np++;
				xo[np] = x[i];
				yo[np] = y[i];

				if (dx != null)
					dxo[np] = dx[i];
				if (dy != null)
					dyo[np] = dy[i];
			}
		}

		ArrayList<double[]> v = new ArrayList<double[]>();
		v.add(xo);
		v.add(yo);
		v.add(dxo);
		v.add(dyo);
		return v;
	}

	/**
	 * Obtains a subDataset taking all points with y lower or equal to certain value.
	 *
	 * @param x X values.
	 * @param y Y values.
	 * @param dx X errors. Can be null.
	 * @param dy Y errors. Can be null.
	 * @param ymax Y maximum.
	 * @return A ArrayList with x, y (doubles), dx, and dy (doubles) arrays.
	 * @throws JPARSECException If an error occurs.
	 */
	public static ArrayList<double[]> subDatasetFromYMaximum(double x[], double y[], double dx[], double dy[], double ymax)
			throws JPARSECException
	{
		int np = 0;
		for (int i = 0; i < x.length; i++)
		{
			if (y[i] <= ymax)
				np++;
		}
		double xo[] = new double[np];
		double yo[] = new double[np];
		double dxo[] = new double[np];
		double dyo[] = new double[np];
		np = -1;
		for (int i = 0; i < x.length; i++)
		{
			if (y[i] <= ymax)
			{
				np++;
				xo[np] = x[i];
				yo[np] = y[i];

				if (dx != null)
					dxo[np] = dx[i];
				if (dy != null)
					dyo[np] = dy[i];
			}
		}

		ArrayList<double[]> v = new ArrayList<double[]>();
		v.add(xo);
		v.add(yo);
		v.add(dxo);
		v.add(dyo);
		return v;
	}

	/**
	 * Parses a double to a String. Performance compared to intrinsic
	 * java method varies a lot: between 20% faster to 100% faster
	 * depending on the input values. The trick is to use Long.parseLong,
	 * which is extremely fast. This method calls {@linkplain FastMath#parseDouble(String)}.
	 * @param s The string.
	 * @return The double value.
	 */
	public static double parseDouble(String s) {
		return FastMath.parseDouble(s);
	}

	/**
	 * Parses a float from a String. Performance compared to intrinsic
	 * java method varies a lot: between 20% faster to 400% faster
	 * depending on the input values. This method calls {@linkplain FastMath#parseDouble(String)}.
	 * @param s The string.
	 * @return The double value.
	 */
	public static float parseFloat(String s) {
		return (float) FastMath.parseDouble(s);
	}

	private static double tryToConvertToDouble(String val) throws JPARSECException
	{
		try
		{
			return DataSet.parseDouble(val);
		} catch (Exception e)
		{
			if (!DataSet.isDoubleOrMathOperationFastCheck(val)) return 0;
			try
			{
				return eval.evaluateMathExpression(Evaluation.configureUserFunction(val));
			} catch (JPARSECException e1)
			{
				throw e1;
			} catch (Exception e2)
			{
				throw new JPARSECException(
						"cannot understand " + val + " as number.", e2);
			}
		}
	}

	/**
	 * Return double value without a possible limit. For "5" returns 5, for "&lt;5"
	 * returns also 5.
	 *
	 * @param arg String value.
	 * @return Double value.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double getDoubleValueWithoutLimit(String arg) throws JPARSECException
	{
		String out = arg.trim();
		if (out.startsWith("<") || out.startsWith(">"))
			out = out.substring(1);
		return tryToConvertToDouble(out);
	}

	/**
	 * Obtain string values of an array of strings, including possible upper and
	 * lower limits, such as "&lt;5" or "&gt;5".
	 *
	 * @param arg Array of strings.
	 * @return A ArrayList with two components, the array of strings without
	 *         limits information, and an array of int values with 1 for upper
	 *         limits and -1 for lowers for each point.
	 */
	public static ArrayList<Object> getStringValuesAndLimits(String arg[])
	{
		ArrayList<Object> v = new ArrayList<Object>();
		String out[] = new String[arg.length];
		int limit[] = new int[arg.length];

		for (int i = 0; i < arg.length; i++)
		{
			String val = arg[i];
			limit[i] = 0;
			if (val.startsWith("<") || val.startsWith(">"))
			{
				limit[i] = 1;
				if (val.startsWith(">"))
					limit[i] = -1;
				val = val.substring(1);
			}
			out[i] = val;
		}
		v.add(out);
		v.add(limit);
		return v;
	}

	/**
	 * Obtain string values of two array of x and y values, excluding points
	 * with upper and lower limits, such as "&lt;5" or "&gt;5".
	 *
	 * @param argx Array of x strings.
	 * @param argy Array of y strings.
	 * @return A ArrayList with two components, the array of x and y strings.
	 */
	public static ArrayList<String[]> getStringDatasetExcludingLimits(String argx[], String argy[])
	{
		ArrayList<String[]> v = new ArrayList<String[]>();
		String outx[] = new String[argx.length];
		String outy[] = new String[argy.length];

		int np = -1;
		for (int i = 0; i < argx.length; i++)
		{
			String valx = argx[i];
			String valy = argy[i];
			if (valy.startsWith("<") || valy.startsWith(">") || valx.startsWith("<") || valx.startsWith(">"))
			{
				valx = valx.substring(1);
			} else
			{
				np++;
				outx[np] = valx;
				outy[np] = valy;
			}
		}

		String outxok[] = new String[np + 1];
		String outyok[] = new String[np + 1];
		for (int i = 0; i <= np; i++)
		{
			outxok[i] = outx[i];
			outyok[i] = outy[i];
		}

		v.add(outxok);
		v.add(outyok);
		return v;
	}

	/**
	 * Obtain string values of two array of x and y values, excluding points
	 * with upper and lower limits, such as "&lt;5" or "&gt;5".
	 *
	 * @param argx Array of x strings.
	 * @param argy Array of y strings.
	 * @param argxerror Array of x errors.
	 * @param argyerror Array of y errors.
	 * @return A ArrayList with four components, the array of x and y strings, and
	 *         the corresponding double array errors.
	 */
	public static ArrayList<Object> getStringDatasetExcludingLimits(String argx[], String argy[], double[] argxerror,
			double[] argyerror)
	{
		ArrayList<Object> v = new ArrayList<Object>();
		String outx[] = new String[argx.length];
		String outy[] = new String[argy.length];
		double outxerror[] = new double[argx.length];
		double outyerror[] = new double[argy.length];

		int np = -1;
		for (int i = 0; i < argx.length; i++)
		{
			String valx = argx[i];
			String valy = argy[i];
			if (valy.startsWith("<") || valy.startsWith(">") || valx.startsWith("<") || valx.startsWith(">"))
			{
				valx = valx.substring(1);
			} else
			{
				np++;
				outx[np] = valx;
				outy[np] = valy;
				if (argxerror != null)
					outxerror[np] = argxerror[i];
				if (argyerror != null)
					outyerror[np] = argyerror[i];
			}
		}

		String outxok[] = new String[np + 1];
		String outyok[] = new String[np + 1];
		double outxerrorok[] = new double[np + 1];
		double outyerrorok[] = new double[np + 1];
		for (int i = 0; i <= np; i++)
		{
			outxok[i] = outx[i];
			outyok[i] = outy[i];
			if (argxerror != null)
				outxerrorok[i] = outxerror[i];
			if (argyerror != null)
				outyerrorok[i] = outyerror[i];
		}

		v.add(outxok);
		v.add(outyok);
		v.add(outxerrorok);
		v.add(outyerrorok);
		return v;
	}

	/**
	 * Obtain string values of an array of strings, excluding possible upper and
	 * lower limits, such as "&lt;5" or "&gt;5".
	 *
	 * @param arg Array of strings.
	 * @return The array of string values.
	 */
	public static String[] getStringValuesExcludingLimits(String arg[])
	{
		int nlimits = 0;
		for (int i = 0; i < arg.length; i++)
		{
			String val = arg[i];
			if (val.startsWith("<") || val.startsWith(">"))
			{
				nlimits++;
			}
		}

		String out[] = new String[arg.length - nlimits];
		int n = -1;
		for (int i = 0; i < arg.length; i++)
		{
			String val = arg[i];
			if (!val.startsWith("<") && !val.startsWith(">"))
			{
				n++;
				out[n] = val;
			}
		}

		return out;
	}

	/**
	 * Obtain string values of an array of strings, excluding possible upper and
	 * lower limits, such as "&lt;5" or "&gt;5".
	 *
	 * @param arg Array of strings.
	 * @param limit_arg Array of strings to be also checked, if, for example,
	 *        limits are set only in one axis and arg contains the values
	 *        (without limits) to be obtained.
	 * @return The array of string values.
	 */
	public static String[] getStringValuesExcludingLimits(String arg[], String limit_arg[])
	{
		int nlimits = 0;
		for (int i = 0; i < arg.length; i++)
		{
			String val = arg[i];
			if (val.startsWith("<") || val.startsWith(">") || limit_arg[i].startsWith("<") || limit_arg[i]
					.startsWith(">"))
			{
				nlimits++;
			}
		}

		String out[] = new String[arg.length - nlimits];
		int n = -1;
		for (int i = 0; i < arg.length; i++)
		{
			String val = arg[i];
			String val_limit = limit_arg[i];
			if (!val.startsWith("<") && !val.startsWith(">") && !val_limit.startsWith("<") && !val_limit
					.startsWith(">"))
			{
				n++;
				out[n] = val;
			}
		}

		return out;
	}

	/**
	 * Return string value without a possible limit.
	 *
	 * @param arg String value.
	 * @return String value, skipping a limit if it exists.
	 */
	public static String getStringValueWithoutLimit(String arg)
	{
		String out = arg.trim();
		if (out.startsWith("<") || out.startsWith(">"))
			out = out.substring(1);
		return out;
	}

	/**
	 * Returns if a given set of values are sorted or not.
	 * @param crescent True for crescent order (1, 2, 3, ...), false for the opposite.
	 * @param x_val Set of values.
	 * @return True or false.
	 */
	public static boolean isSorted(boolean crescent, double x_val[]) {
		for (int i=1; i<x_val.length; i++) {
			if (x_val[i] > x_val[i-1] && crescent) continue;
			if (x_val[i] < x_val[i-1] && !crescent) continue;
			return false;
		}
		return true;
	}

	/**
	 * A method for reordering points in x descent order. This method can also
	 * eliminates repeated x points.
	 *
	 * @param x_val X set of values.
	 * @param y_val Y set of values. Can be null.
	 * @param eliminateRepeatedPoints True to eliminate repeated x points.
	 * @return ArrayList with X and Y sets of values.
	 */
	public static ArrayList<double[]> sortInDescent(double[] x_val, double[] y_val, boolean eliminateRepeatedPoints)
	{
		if (DataSet.isSorted(false, x_val)) {
			ArrayList<double[]> v = new ArrayList<double[]>();
			v.add(x_val);
			v.add(y_val);
			return v;
		}

		ArrayList<double[]> v = DataSet.sortInCrescent(x_val, y_val, eliminateRepeatedPoints);
		double x[] = v.get(0);
		double xo[] = new double[x.length];
		double y[] = null, yo[] = null;
		if (y_val != null) {
			y = v.get(1);
			yo = new double[y.length];
		}
		for (int i=0; i<x.length; i++)
		{
			xo[i] = x[x.length-1-i];
			if (y_val != null) yo[i] = y[y.length-1-i];
		}
		ArrayList<double[]> vo = new ArrayList<double[]>();
		vo.add(xo);
		if (y_val != null) vo.add(yo);
		return vo;
	}

	/**
	 * A method for reordering points in x crescent order. This method can also
	 * eliminates repeated x points.
	 *
	 * @param x_val X set of values.
	 * @param y_val Y set of values. Can be null.
	 * @param eliminateRepeatedPoints True to eliminate repeated x points.
	 * @return ArrayList with X and Y sets of values.
	 */
	public static ArrayList<double[]> sortInCrescent(double[] x_val, double[] y_val, boolean eliminateRepeatedPoints)
	{
		if (DataSet.isSorted(true, x_val)) {
			ArrayList<double[]> v = new ArrayList<double[]>();
			v.add(x_val.clone());
			if (y_val != null) v.add(y_val.clone());
			return v;
		}

		// Now lets re-order the points in abscisa crescent order
		int size = x_val.length;
		int min_value = -1;
		int flag_x[] = new int[size];
		for (int i = 1; i < size; i++)
		{
			flag_x[i] = 0;
		}
		double ordered_x[] = new double[size];
		double ordered_y[] = new double[size];
		int np = -1;
		for (int j = 0; j < size; j++)
		{
			min_value = -1;
			for (int i = 0; i < size; i++)
			{
				if (flag_x[i] == 0)
				{
					if (0 <= min_value)
					{
						if (x_val[i] < x_val[min_value])
							min_value = i;
					} else
					{
						min_value = i;
					}
				}
			}
			flag_x[min_value] = 1;

			// Check for repeated points
			if (np == -1)
			{
				np++;
				ordered_x[np] = x_val[min_value];
				if (y_val != null) ordered_y[np] = y_val[min_value];
			} else
			{
				if (ordered_x[np] != x_val[min_value] || !eliminateRepeatedPoints)
				{
					np++;
					ordered_x[np] = x_val[min_value];
					if (y_val != null) ordered_y[np] = y_val[min_value];
				}
			}
		}

		double new_x[] = new double[np + 1];
		double new_y[] = new double[np + 1];
		for (int i = 0; i <= np; i++)
		{
			new_x[i] = ordered_x[i];
			if (y_val != null) new_y[i] = ordered_y[i];
		}

		ArrayList<double[]> v = new ArrayList<double[]>();
		v.add(new_x);
		if (y_val != null) v.add(new_y);

		return v;
	}

	/**
	 * A method for reordering points in x crescent order, including errors. This method can also
	 * eliminates repeated x points.
	 *
	 * @param x_val X set of values.
	 * @param y_val Y set of values
	 * @param dx_val dX set of values. Can be null.
	 * @param dy_val dY set of values Can be null.
	 * @param eliminateRepeatedPoints True to eliminate repeated x points.
	 * @return ArrayList with X, Y, dX, dY sets of values.
	 */
	public static ArrayList<double[]> sortInCrescent(double[] x_val, double[] y_val, double dx_val[], double dy_val[], boolean eliminateRepeatedPoints)
	{
		if (DataSet.isSorted(true, x_val)) {
			ArrayList<double[]> v = new ArrayList<double[]>();
			v.add(x_val);
			v.add(y_val);
			v.add(dx_val);
			v.add(dy_val);
			return v;
		}

		// Now lets re-order the points in abscisa crescent order
		int size = x_val.length;
		int min_value = -1;
		int flag_x[] = new int[size];
		for (int i = 1; i < size; i++)
		{
			flag_x[i] = 0;
		}
		double ordered_x[] = new double[size];
		double ordered_y[] = new double[size];
		double ordered_dx[] = null;
		double ordered_dy[] = null;
		if (dx_val != null) ordered_dx = new double[size];
		if (dy_val != null) ordered_dy = new double[size];
		int np = -1;
		for (int j = 0; j < size; j++)
		{
			min_value = -1;
			for (int i = 0; i < size; i++)
			{
				if (flag_x[i] == 0)
				{
					if (0 <= min_value)
					{
						if (x_val[i] < x_val[min_value])
							min_value = i;
					} else
					{
						min_value = i;
					}
				}
			}
			flag_x[min_value] = 1;

			// Check for repeated points
			if (np == -1)
			{
				np++;
				ordered_x[np] = x_val[min_value];
				ordered_y[np] = y_val[min_value];
				if (dx_val != null) ordered_dx[np] = dx_val[min_value];
				if (dy_val != null) ordered_dy[np] = dy_val[min_value];
			} else
			{
				if (ordered_x[np] != x_val[min_value] || !eliminateRepeatedPoints)
				{
					np++;
					ordered_x[np] = x_val[min_value];
					ordered_y[np] = y_val[min_value];
					if (dx_val != null) ordered_dx[np] = dx_val[min_value];
					if (dy_val != null) ordered_dy[np] = dy_val[min_value];
				}
			}
		}

		double new_x[] = new double[np + 1];
		double new_y[] = new double[np + 1];
		double new_dx[] = null;
		double new_dy[] = null;
		if (dx_val != null) new_dx = new double[size];
		if (dy_val != null) new_dy = new double[size];
		for (int i = 0; i <= np; i++)
		{
			new_x[i] = ordered_x[i];
			new_y[i] = ordered_y[i];
			if (dx_val != null) new_dx[i] = ordered_dx[i];
			if (dy_val != null) new_dy[i] = ordered_dy[i];
		}

		ArrayList<double[]> v = new ArrayList<double[]>();
		v.add(new_x);
		v.add(new_y);
		v.add(new_dx);
		v.add(new_dy);

		return v;
	}

	/**
	 * A method for reordering points in x crescent order, when arrays are given
	 * as strings. This method does not eliminate repeated x points.
	 *
	 * @param x_val X set of values.
	 * @param y_val Y set of values
	 * @param dx X errors. Can be null.
	 * @param dy Y errors. Can be null.
	 * @return ArrayList with X and Y sets of values.
	 * @throws JPARSECException If an error occurs.
	 */
	public static ArrayList<Object> sortInCrescent(String[] x_val, String[] y_val, double dx[], double dy[])
	throws JPARSECException {
		if (DataSet.isSorted(true, DataSet.toDoubleValues(x_val))) {
			ArrayList<Object> v = new ArrayList<Object>();
			v.add(x_val);
			v.add(y_val);
			v.add(dx);
			v.add(dy);
			return v;
		}

		// Now lets re-order the points in abscisa crescent order
		int size = x_val.length;
		int min_value = -1;
		int flag_x[] = new int[size];
		for (int i = 1; i < size; i++)
		{
			flag_x[i] = 0;
		}
		String ordered_x[] = new String[size];
		String ordered_y[] = new String[size];
		double ordered_dx[] = null;
		if (dx != null) ordered_dx = new double[size];
		double ordered_dy[] = null;
		if (dy != null) ordered_dy = new double[size];
		int np = -1;
		for (int j = 0; j < size; j++)
		{
			min_value = -1;
			for (int i = 0; i < size; i++)
			{
				if (flag_x[i] == 0)
				{
					if (0 <= min_value)
					{
						if (DataSet.getDoubleValueWithoutLimit(x_val[i]) < DataSet.getDoubleValueWithoutLimit(x_val[min_value]))
							min_value = i;
					} else
					{
						min_value = i;
					}
				}
			}
			flag_x[min_value] = 1;

			np++;
			ordered_x[np] = x_val[min_value];
			ordered_y[np] = y_val[min_value];
			if (dx != null) ordered_dx[np] = dx[min_value];
			if (dy != null) ordered_dy[np] = dy[min_value];
		}

		String new_x[] = new String[np + 1];
		String new_y[] = new String[np + 1];
		double new_dx[] = null;
		if (dx != null) new_dx = new double[size];
		double new_dy[] = null;
		if (dy != null) new_dy = new double[size];
		for (int i = 0; i <= np; i++)
		{
			new_x[i] = ordered_x[i];
			new_y[i] = ordered_y[i];
			if (ordered_dx != null) new_dx[i] = ordered_dx[i];
			if (ordered_dy != null) new_dy[i] = ordered_dy[i];
		}

		ArrayList<Object> v = new ArrayList<Object>();
		v.add(new_x);
		v.add(new_y);
		v.add(new_dx);
		v.add(new_dy);

		return v;
	}

	/**
	 * A method for reordering points in x crescent order, when arrays are given
	 * as strings. This method does not eliminate repeated x points.
	 *
	 * @param x_val X set of values.
	 * @return New set of values.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String[] sortInCrescent(String[] x_val)
	throws JPARSECException {
		if (DataSet.isSorted(true, DataSet.toDoubleValues(x_val))) {
			return x_val;
		}

		// Now lets re-order the points in abscisa crescent order
		int size = x_val.length;
		int min_value = -1;
		int flag_x[] = new int[size];
		for (int i = 1; i < size; i++)
		{
			flag_x[i] = 0;
		}
		String ordered_x[] = new String[size];
		int np = -1;
		for (int j = 0; j < size; j++)
		{
			min_value = -1;
			for (int i = 0; i < size; i++)
			{
				if (flag_x[i] == 0)
				{
					if (0 <= min_value)
					{
						if (DataSet.getDoubleValueWithoutLimit(x_val[i]) < DataSet.getDoubleValueWithoutLimit(x_val[min_value]))
							min_value = i;
					} else
					{
						min_value = i;
					}
				}
			}
			flag_x[min_value] = 1;

			np++;
			ordered_x[np] = x_val[min_value];
		}

		String new_x[] = new String[np + 1];
		for (int i = 0; i <= np; i++)
		{
			new_x[i] = ordered_x[i];
		}

		return new_x;
	}

	/**
	 * A method for reordering points in certain x order, when arrays are given
	 * as strings. This method does not eliminate repeated x points.
	 *
	 * @param x_val X set of values.
	 * @param y_val Y set of values
	 * @param dx X errors set of values.
	 * @param dy Y errors set of values
	 * @param pattern Pattern of x values to be used in the sort.
	 * @return ArrayList with x, y, dx, dy sets of values.
	 */
	public static ArrayList<String[]> sort(String[] x_val, String[] y_val, String[] dx, String dy[], String[] pattern)
	{
		int size = x_val.length;
		int min_value = -1, min_index = -1;
		int flag_x[] = new int[size];
		for (int i = 1; i < size; i++)
		{
			flag_x[i] = 0;
		}
		String ordered_x[] = new String[size];
		String ordered_y[] = new String[size];
		String ordered_dx[] = new String[size];
		String ordered_dy[] = new String[size];
		int np = -1;
		for (int j = 0; j < size; j++)
		{
			min_value = -1;
			min_index = -1;
			for (int i = 0; i < size; i++)
			{
				if (flag_x[i] == 0)
				{
					if (min_value >= 0)
					{
						int index = DataSet.getIndex(pattern, x_val[i]);
						if (index < min_index) {
							min_value = i;
							min_index = DataSet.getIndex(pattern, x_val[min_value]);
						}
					} else
					{
						min_value = i;
						min_index = DataSet.getIndex(pattern, x_val[min_value]);
					}
				}
			}
			flag_x[min_value] = 1;

			np++;
			ordered_x[np] = x_val[min_value];
			ordered_y[np] = y_val[min_value];
			if (dx != null) ordered_dx[np] = dx[min_value];
			if (dy != null) ordered_dy[np] = dy[min_value];
		}

		String new_x[] = new String[np + 1];
		String new_y[] = new String[np + 1];
		String new_dx[] = new String[np + 1];
		String new_dy[] = new String[np + 1];
		for (int i = 0; i <= np; i++)
		{
			new_x[i] = ordered_x[i];
			new_y[i] = ordered_y[i];
			if (dx != null) new_dx[i] = ordered_dx[i];
			if (dy != null) new_dy[i] = ordered_dy[i];
		}

		ArrayList<String[]> v = new ArrayList<String[]>();
		v.add(new_x);
		v.add(new_y);
		v.add(new_dx);
		v.add(new_dy);

		return v;
	}

	/**
	 * Transforms the contents of a ArrayList to an array of double values.
	 * @param v ArrayList.
	 * @return Double values.
	 */
	public static double[] arrayListToDoubleArray(ArrayList<Double> v)
	{
		int size = v.size();

		double array[] = new double[size];
		for (int i = 0; i < size; i++)
		{
			array[i] = (v.get(i)).doubleValue();
		}

		return array;
	}

	/**
	 * Transforms the contents of a ArrayList to an array of string values.
	 * @param v ArrayList.
	 * @return String values.
	 */
	public static String[] arrayListToStringArray(ArrayList<String> v)
	{
		if (v == null) return null;

		String array[] = new String[v.size()];
		return v.toArray(array);
	}

	/**
	 * Transforms the contents of a string array into a ArrayList.
	 * @param array The array.
	 * @return String values as a ArrayList.
	 */
	public static ArrayList<String> stringArraytoArrayList(String array[])
	{
		return new ArrayList<String>(Arrays.asList(array));
	}
	/**
	 * Transforms the contents of a ArrayList to an array of integer values.
	 * @param v ArrayList.
	 * @return Integer values.
	 */
	public static int[] arrayListToIntegerArray(ArrayList<Integer> v)
	{
		int size = v.size();

		int array[] = new int[size];
		for (int i = 0; i < size; i++)
		{
			array[i] = (v.get(i)).intValue();
		}

		return array;
	}
	/**
	 * Transforms the contents of a ArrayList to an array of long values.
	 * @param v ArrayList.
	 * @return Integer values.
	 */
	public static long[] arrayListToLongArray(ArrayList<Long> v)
	{
		int size = v.size();

		long array[] = new long[size];
		for (int i = 0; i < size; i++)
		{
			array[i] = (v.get(i)).longValue();
		}

		return array;
	}

	/**
	 * Transforms the contents of a ArrayList to a String, using
	 * the system line separator as separator.
	 * @param v ArrayList.
	 * @return String text.
	 */
	public static String arrayListToString(ArrayList<String> v)
	{
		if (v.size() == 0) return "";
		int size = v.size();

		StringBuilder text = new StringBuilder(10 + size*v.get(0).length() / 2);
		String s = jparsec.io.FileIO.getLineSeparator();
		text.append(v.get(0));
		for (int i = 1; i < size; i++)
		{
			text.append(s);
			text.append(v.get(i));
		}

		return text.toString();
	}
	/**
	 * Transforms the contents of an array to a String using line
	 * separators.
	 * @param v String array.
	 * @return String text.
	 */
	public static String stringArrayToString(String v[])
	{
		if (v == null) return null;
		if (v.length == 0) return "";

		int size = v.length;

		StringBuilder text = new StringBuilder(10 + size*v[0].length() / 2);
		String s = jparsec.io.FileIO.getLineSeparator();

		text.append(v[0]);
		for (int i = 1; i < size; i++)
		{
			text.append(s);
			text.append(v[i]);
		}

		return text.toString();
	}

	/**
	 * Returns the index of a given point in the dataset.
	 * @param x X values.
	 * @param y Y values.
	 * @param px X point searched.
	 * @param py Y point searched.
	 * @return index of the point, or -1 if no match is found.
	 */
	public static int getIndex(String x[], String y[], String px, String py)
	{
		int r = -1;
		for (int i=0; i<x.length; i++)
		{
			if (x[i].equals(px) && y[i].equals(py)) {
				r = i;
				break;
			}
		}
		return r;
	}

	/**
	 * Returns the index of a given point in the dataset.
	 * @param x X values.
	 * @param px X point searched.
	 * @return index of the point, or -1 if no match is found.
	 */
	public static int getIndex(String x[], String px)
	{
		for (int i=0; i<x.length; i++)
		{
			if (x[i] != null && x[i].equals(px)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Returns the index of a given point in the dataset.
	 * @param x X values.
	 * @param px X point searched.
	 * @return index of the point, or -1 if no match is found.
	 */
	public static int getIndex(int x[], int px)
	{
		for (int i=0; i<x.length; i++)
		{
			if (x[i] == px) return i;
		}
		return -1;
	}

	/**
	 * Returns the index of a given point in the dataset.
	 * @param x X values.
	 * @param px X point searched.
	 * @return index of the point, or -1 if no match is found.
	 */
	public static int getIndex(Object x[], Object px)
	{
		for (int i=0; i<x.length; i++)
		{
			if (x[i] != null && x[i].equals(px)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Returns the index of a given object in another, returning a value also if the
	 * input object is part of one of those from the input array. This method is not case
	 * sensitive.
	 * @param x Array of strings.
	 * @param px String to search.
	 * @return index of the point, or -1 if no match is found.
	 */
	public static int getIndexContaining(String x[], String px)
	{
		for (int i=0; i<x.length; i++)
		{
			if (x[i] == null) continue;
			if (x[i].indexOf(px) >= 0)
				return i;
		}
		for (int i=0; i<x.length; i++)
		{
			if (x[i] == null) continue;
			if (x[i].toLowerCase().indexOf(px.toLowerCase()) >= 0) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Returns the index of a given object in another, returning a value also if the
	 * input object is part of one of those from the input array. This method is case
	 * sensitive.
	 * @param x Array of strings.
	 * @param px String to search.
	 * @return index of the point, or -1 if no match is found.
	 */
	public static int getIndexContainingCaseSensitive(String x[], String px)
	{
		int max = x.length;
		for (int i=0; i<max; i++)
		{
			if (x[i] == null) continue;
			if (x[i].indexOf(px) >= 0)
				return i;
		}
		return -1;
	}

	/**
	 * Returns the first index of a given object in another, returning a value if any of the
	 * elements in input object starts with the input string. This method is not case
	 * sensitive.
	 * @param x Array of strings.
	 * @param px String to search.
	 * @return index of the point, or -1 if no match is found.
	 */
	public static int getIndexStartingWith(String x[], String px)
	{
		for (int i=0; i<x.length; i++)
		{
			if (x[i] == null) continue;
			if (x[i].startsWith(px)) {
				return i;
			}
		}
		for (int i=0; i<x.length; i++)
		{
			if (x[i] == null) continue;
			if (x[i].toLowerCase().startsWith(px.toLowerCase())) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Returns the index of a given object in another, returning a value if any of the
	 * elements in input object ends with the input string. This method is not case
	 * sensitive.
	 * @param x Array of strings.
	 * @param px String to search.
	 * @return index of the point, or -1 if no match is found.
	 */
	public static int getIndexEndingWith(String x[], String px)
	{
		for (int i=0; i<x.length; i++)
		{
			if (x[i] == null) continue;
			if (x[i].endsWith(px)) {
				return i;
			}
		}
		for (int i=0; i<x.length; i++)
		{
			if (x[i] == null) continue;
			if (x[i].toLowerCase().endsWith(px.toLowerCase())) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Transforms an ArrayList of objects into an array of objects,
	 * conserving the object class in the output array (if possible), so it can
	 * be casted to certain type.
	 * @param a The ArrayList.
	 * @return The object array.
	 */
	public static Object[] toObjectArray(ArrayList<Object> a) {
		Object o[] = null;
		try {
			o = (Object[]) Array.newInstance(Class.forName(a.get(0).getClass().getCanonicalName()), a.size());
		} catch (Exception e) {
			//e.printStackTrace();
			o = new Object[a.size()];
		}
		for (int i=0; i<o.length; i++) {
			o[i] = a.get(i);
		}
		return o;
	}

    /**
     * Transform a primitive array (int[], double[], ...)
     * to an object array (Integer[], Double[]). <P>
     *
     * This method comes from JFreeChart code.
     *
     * @param arr The array.
     *
     * @return An array.
     * @throws JPARSECException If an error occurs.
     */
    public static Object toArray(Object arr)
    throws JPARSECException {

        if (arr == null) {
            return arr;
        }

        Class cls = arr.getClass();
        if (!cls.isArray()) {
            return arr;
        }

        Class compType = cls.getComponentType();
        int dim = 1;
        while (!compType.isPrimitive()) {
            if (!compType.isArray()) {
                return arr;
            }
            else {
                dim++;
                compType = compType.getComponentType();
            }
        }

        int[] length = new int[dim];
        length[0] = Array.getLength(arr);
        Object[] newarr = null;

        try {
            if (compType.equals(Integer.TYPE)) {
                newarr = (Object[]) Array.newInstance(Class.forName("java.lang.Integer"), length);
            }
            else if (compType.equals(Double.TYPE)) {
                newarr = (Object[]) Array.newInstance(Class.forName("java.lang.Double"), length);
            }
            else if (compType.equals(Long.TYPE)) {
                newarr = (Object[]) Array.newInstance(Class.forName("java.lang.Long"), length);
            }
            else if (compType.equals(Float.TYPE)) {
                newarr = (Object[]) Array.newInstance(Class.forName("java.lang.Float"), length);
            }
            else if (compType.equals(Short.TYPE)) {
                newarr = (Object[]) Array.newInstance(Class.forName("java.lang.Short"), length);
            }
            else if (compType.equals(Byte.TYPE)) {
                newarr = (Object[]) Array.newInstance(Class.forName("java.lang.Byte"), length);
            }
            else if (compType.equals(Character.TYPE)) {
                newarr = (Object[]) Array.newInstance(Class.forName("java.lang.Character"), length);
            }
            else if (compType.equals(Boolean.TYPE)) {
                newarr = (Object[]) Array.newInstance(Class.forName("java.lang.Boolean"), length);
            }
        }
        catch (ClassNotFoundException ex) {
            throw new JPARSECException("cannot cast this object.", ex);
        }

        for (int i = 0; i < length[0]; i++) {
            if (dim != 1) {
                newarr[i] = toArray(Array.get(arr, i));
            }
            else {
                newarr[i] = Array.get(arr, i);
            }
        }
        return newarr;
    }
    
    /**
     * Returns the component type of an object that could be an array of multiple 
     * dimensions.
     * @param arr The object.
     * @return The class of the components in the object.
     */
    public static Class getComponentType(Object arr) {
    	Class cls = arr.getClass();
        Class compType = cls.getComponentType();
        while (!compType.isPrimitive()) {
            if (!compType.isArray()) break;
            compType = compType.getComponentType();
        }
        return compType;
    }

    /**
     * Obtains a String representation of an array. See
     * http://www.javapractices.com/topic/TopicAction.do?Id=131
     * <code>aArray</code> is a possibly-null array whose elements are
     * primitives or objects; arrays of arrays are also valid, in which case
     * <code>aArray</code> is rendered in a nested, recursive fashion.
     * @param aArray An array of integers, doubles, Strings, ...
     * @return The String representation.
     * @throws JPARSECException If an error occurs.
     */
     public static String toString(Object aArray) throws JPARSECException{
       if ( aArray == null ) return "null";
       if ( ! aArray.getClass().isArray() ) throw new JPARSECException("object is not an array.");

       StringBuilder result = new StringBuilder( "[" );
       int length = Array.getLength(aArray);
       for ( int idx = 0 ; idx < length ; ++idx ) {
         Object item = Array.get(aArray, idx);
         if ( item != null && item.getClass().isArray() ){
           //recursive call!
           result.append( DataSet.toString(item) );
         }
         else{
           result.append( item );
         }
         if ( idx != length - 1 ) {
           result.append(", ");
         }
       }
       result.append("]");
       return result.toString();
     }

    /**
     * Appends an string array to another array.
     * @param array1 First array.
     * @param array2 Second array to be append at the end of the first one.
     * @return An array.
     */
    public static String[] addStringArray(String[] array1, String[] array2)
    {
    	if (array1 == null) return array2;
    	if (array2 == null) return array1;
    	int a1 = array1.length;
    	String out[] = new String[a1 + array2.length];
    	System.arraycopy(array1, 0, out, 0, a1);
    	System.arraycopy(array2, 0, out, a1, array2.length);
    	return out;
    }

    /**
     * Appends an object array to another array, conserving the class of the
     * objects in the output array.
     * @param array1 First array.
     * @param array2 Second array to be append at the end of the first one.
     * @return An array.
     * @throws JPARSECException In case the class canonical name of both input
     * arrays are not the same.
     */
    public static Object[] addObjectArray(Object[] array1, Object[] array2) throws JPARSECException
    {
    	if (array1 == null) return array2;
    	if (array2 == null) return array1;
    	int a1 = array1.length;
    	//Object out[] = new Object[a1 + array2.length];
    	if (!array1.getClass().getComponentType().getCanonicalName().equals(array1.getClass().getComponentType().getCanonicalName()))
    		throw new JPARSECException("Array 1 has type "+array1.getClass().getComponentType()+", while array2 has type "+array2.getClass().getComponentType()+". They must be the same.");
    	Object out[] = (Object[]) Array.newInstance(array1.getClass().getComponentType(), a1 + array2.length);
    	System.arraycopy(array1, 0, out, 0, a1);
    	System.arraycopy(array2, 0, out, a1, array2.length);
    	return out;
    }
    /**
     * Appends an string to another array.
     * @param array1 First array.
     * @param array2 Second array to be append at the end of the first one,
     * with length 1.
     * @return An array.
     */
    public static String[] addStringArray(String[] array1, String array2)
    {
    	if (array1 == null) return new String[] {array2};
    	if (array2 == null) return array1;
    	int a1 = array1.length;
    	String out[] = new String[a1 + 1];
    	System.arraycopy(array1, 0, out, 0, a1);
    	//System.arraycopy(array2, 0, out, a1, 1);
    	out[a1] = array2;
    	return out;
    }

    /**
     * Appends a double array to another array.
     * @param array1 First array.
     * @param array2 Second array to be append at the end of the first one.
     * @return An array.
     */
    public static double[] addDoubleArray(double[] array1, double[] array2)
    {
    	if (array1 == null) return array2;
    	if (array2 == null) return array1;
    	double out[] = new double[array1.length + array2.length];
    	System.arraycopy(array1, 0, out, 0, array1.length);
    	System.arraycopy(array2, 0, out, array1.length, array2.length);
    	return out;
    }

    /**
     * Appends a boolean array to another array.
     * @param array1 First array.
     * @param array2 Second array to be append at the end of the first one.
     * @return An array.
     */
    public static boolean[] addBooleanArray(boolean[] array1, boolean[] array2)
    {
    	if (array1 == null) return array2;
    	if (array2 == null) return array1;
    	int l1 = array1.length, l2 = array2.length;
    	boolean out[] = new boolean[l1 + l2];
    	System.arraycopy(array1, 0, out, 0, l1);
    	System.arraycopy(array2, 0, out, l1, l2);
    	return out;
    }

    /**
     * Appends a float array to another array.
     * @param array1 First array.
     * @param array2 Second array to be append at the end of the first one.
     * @return An array.
     */
    public static float[] addFloatArray(float[] array1, float[] array2)
    {
    	if (array1 == null) return array2;
    	if (array2 == null) return array1;
    	float out[] = new float[array1.length + array2.length];
    	System.arraycopy(array1, 0, out, 0, array1.length);
    	System.arraycopy(array2, 0, out, array1.length, array2.length);
    	return out;
    }

    /**
     * Appends an integer array to another array.
     * @param array1 First array.
     * @param array2 Second array to be append at the end of the first one.
     * @return An array.
     */
    public static int[] addIntegerArray(int[] array1, int[] array2)
    {
    	if (array1 == null) return array2;
    	if (array2 == null) return array1;
    	int out[] = new int[array1.length + array2.length];
    	System.arraycopy(array1, 0, out, 0, array1.length);
    	System.arraycopy(array2, 0, out, array1.length, array2.length);
    	return out;
    }

    /**
     * Eliminates a row from a table.
     * @param table Table.
     * @param row Row number, starting with 1.
     * @return The same table without the row.
     * @throws JPARSECException If row is out of range.
     */
    public static String[] eliminateRowFromTable(String table[], int row)
    throws JPARSECException {

    	if (row <= 0 || row > table.length) throw new JPARSECException("row "+(row)+" out of range 1-"+(table.length)+".");

    	String out[] = new String[table.length-1];
    	if (row > 1) System.arraycopy(table, 0, out, 0, row - 1);
    	if (row < table.length) System.arraycopy(table, row, out, row - 1, table.length - row);
    	return out;
    }

    /**
     * Formats a table by ordering the elements in ascending/descending mode for a given column,
     * and maintaining the length of each column.
     * @param table Table to format.
     * @param separator Separator used in the table.
     * @param order Field number to order (1 or greater). If positive, that field will be
     * ordered in crescent. If negative, in descent. Set to 0 to maintain
     * ordering as input.
     * @return Formatted table.
     * @throws JPARSECException If an error occurs.
     */
    public static String[] formatTable(String[] table, String separator, int order)
    throws JPARSECException {
    	int n = jparsec.io.FileIO.getNumberOfFields(table[0], separator, true);

    	if (order != 0)
    	{
	    	String newTable[] = new String[table.length];
	    	int newTableIndex = -1;
	    	int tableSize = table.length;
	    	do {
		    	String val[] = new String[table.length];
		    	for (int i=0; i<val.length; i++)
		    	{
		    		val[i] = FileIO.getField(Math.abs(order), table[i], separator, true);
		    	}
		    	int min = 0;
		    	if (order < 0) {
		    		min = DataSet.getIndexOfMaximum((double[]) DataSet.getDoubleValuesIncludingLimits(val).get(0));
		    	} else {
		    		min = DataSet.getIndexOfMinimum((double[]) DataSet.getDoubleValuesIncludingLimits(val).get(0));
		    	}
		    	newTableIndex++;
		    	newTable[newTableIndex] = table[min];
		    	table = DataSet.eliminateRowFromTable(table, min+1);
		    	tableSize = table.length;
	    	} while (tableSize > 0);
	    	table = newTable;
    	}

    	int len[] = new int[n];
    	for (int i=0; i<n; i++)
    	{
    		len[i] = getMaxLength(table, i+1, separator);
    	}

    	String out[] = new String[table.length];
    	for (int i=0; i<out.length; i++)
    	{
    		int fieldN = 0;
    		String field = "h";
    		out[i] = "";
    		do {
    			fieldN ++;
    			field = jparsec.io.FileIO.getField(fieldN, table[i], separator, true);
    			if (fieldN < len.length) {
    				field = jparsec.io.FileIO.addSpacesAfterAString(field, len[fieldN]);
    			}
    			out[i] += field;
    			if (fieldN < len.length) out[i] += separator;
    		} while (fieldN < len.length);
    	}
    	return out;
    }
    private static int getMaxLength(String[] table, int field, String separator)
    {
    	int l = -1;
    	for (int i=0; i<table.length; i++)
    	{
    		int j = jparsec.io.FileIO.getField(field, table[i], separator, true).length();
    		if (j > l) l = j;
    	}
    	return l;
    }
    /**
     * Transforms a string into an array for a given field separator.
     * This functions uses intrinsic {@linkplain java.util.StringTokenizer} to separate
     * the fields. For short Strings it is faster to call {@linkplain #toStringArray(String, String, boolean)}.
     * In addition, take into account that the intrinsic function could skip empty values assuming
     * two consecutive separators as only one.
     * @param text Text.
     * @param separator Separator.
     * @return Array.
     */
    public static String[] toStringArray(String text, String separator)
    {
    	StringTokenizer tok = new StringTokenizer(text, separator);
    	int n = tok.countTokens();
    	String array[] = new String[n];
    	for (int i=0; i<n; i++)
    	{
    		array[i] = tok.nextToken();
    	}
    	return array;
    }

    /**
     * Transforms a string into an array for a given length in each element of the output array.
     * Last element will have a length equal or lower than the others.
     * @param text Text.
     * @param l Length of each element in the output array.
     * @return Array.
     */
    public static String[] toStringArray(String text, int l)
    {
    	if (text == null) return new String[] {text};
    	int ll = text.length();
    	if (ll <= l) return new String[] {text};
    	int n = ll / l;
    	if (ll % l != 0) n ++;
    	String out[] = new String[n];
    	for (int i=0; i<n; i++) {
    		if (text.length() <= l) {
    			out[i] = text;
    			if (i < out.length-1) out = DataSet.getSubArray(out, 0, i);
    			break;
    		} else {
	    		out[i] = text.substring(0, l);
	    		text = text.substring(l);
    		}
    	}
    	return out;
    }

    /**
     * Transforms a string into an array for a given field separator.
     * @param text Text.
     * @param separator Separator.
     * @param skipSeparator True to consider several consecutive separators as only one.
     * @return Array.
     */
    public static String[] toStringArray(String text, String separator, boolean skipSeparator)
    {
//    	if (!skipSeparator) {
    		ArrayList<String> list = new ArrayList<String>();
    		int sl = separator.length();
    		do {
    			int n = text.indexOf(separator);
    			if (n != 0 || !skipSeparator) {
	    			if (n < 0) {
	    				list.add(text);
	    				break;
	    			}
	    			list.add(text.substring(0, n));
    			}
    			if (n == text.length()-sl) {
    				list.add("");
    				break;
    			}
    			text = text.substring(n+sl);
    		} while (text.length() > 0);
    		return DataSet.arrayListToStringArray(list);
/*    	} else {
	    	int n = FileIO.getNumberOfFields(text, separator, skipSeparator);
	    	String array[] = new String[n];
	    	for (int i=0; i<n; i++)
	    	{
	    		array[i] = FileIO.getField(i+1, text, separator, skipSeparator);
	    	}
	    	return array;
    	}
*/
    }

    /**
     * Eliminates a column from a given table.
     * @param table Data for table.
     * @param separator Separator string.
     * @param columnIndex Column index to eliminate, from 0 to number of columns - 1.
     * @return The same data without that column if the column index is valid.
     */
    public static String[] eliminateColumnFromTable(String[] table, String separator, int columnIndex)
    {
    	String out[] = new String[table.length];
    	for (int i=0; i<table.length; i++)
    	{
    		int nc = FileIO.getNumberOfFields(table[i], separator, true);
    		if (nc > columnIndex) {
    			out[i] = "";
    			for (int j=0; j<nc; j++)
    			{
    				if (j != columnIndex) {
	    				out[i] += FileIO.getField(j+1, table[i], separator, true);
	    				if (j < (nc - 1)) out[i] += separator;
    				}
    			}
    		} else {
        		out[i] = table[i];
    		}
    	}
    	return out;
    }
    /**
     * Extracts a column from a given table.
     * @param table Data for table.
     * @param separator Separator string.
     * @param columnIndex Column index to extract, from 0 to number of columns - 1.
     * @return The data for that column if the column index is valid.
     */
    public static String[] extractColumnFromTable(String[] table, String separator, int columnIndex)
    {
    	String out[] = new String[table.length];
    	for (int i=0; i<table.length; i++)
    	{
    		int nc = FileIO.getNumberOfFields(table[i], separator, true);
			out[i] = "";
    		if (nc > columnIndex) {
    			for (int j=0; j<nc; j++)
    			{
    				if (j == columnIndex) {
	    				out[i] = FileIO.getField(j+1, table[i], separator, true);
    				}
    			}
    		}
    	}
    	return out;
    }
    /**
     * Extracts a fraction of a table that matches the value of a
     * given column.
     * @param table Data for table.
     * @param separator Separator string.
     * @param columnIndex Column index, from 0 to number of columns - 1.
     * @param value Value of the column. If the column defined
     * by the previous index is equals to this value, the row
     * will be extracted.
     * @return The data that matches the condition.
     */
    public static String[] extractSubTable(String[] table, String separator, int columnIndex, String value)
    {
    	String subTable[] = new String[table.length];
    	int index = -1;
    	for (int i=0; i<table.length; i++)
    	{
    		int nc = FileIO.getNumberOfFields(table[i], separator, true);
    		if (nc > columnIndex) {
   				if (FileIO.getField(columnIndex+1, table[i], separator, true).equals(value)) {
   					index ++;
    				subTable[index] = table[i];
   				}
    		}
    	}
    	String out[] = new String[index+1];
    	for (int i = 0; i<=index; i++)
    	{
    		out[i] = subTable[i];
    	}
    	return out;
    }
    /**
     * Adds a column in a given table.
     * @param table Data for table.
     * @param separator Separator string.
     * @param newColumn Data for column.
     * @param columnIndex Column index for the new column, from 0 to number of columns.
     * @return The same data including that column if the column index is valid.
     */
    public static String[] addColumnInTable(String[] table, String separator, String[] newColumn, int columnIndex)
    {
    	String out[] = new String[table.length];
    	for (int i=0; i<table.length; i++)
    	{
    		int nc = FileIO.getNumberOfFields(table[i], separator, true);
    		if (nc >= columnIndex) {
    			out[i] = "";
    			for (int j=0; j<=nc; j++)
    			{
    				int field = j+1;
    				if (j > columnIndex) field --;

    				if (j != columnIndex) {
	    				out[i] += FileIO.getField(field, table[i], separator, true);
    				} else {
    					out[i] += newColumn[i];
    				}
    				if (j <= (nc - 1)) out[i] += separator;
    			}
    		} else {
        		out[i] = table[i];
    		}
    	}
    	return out;
    }

    /**
     * Adds a row in a given table. Separator should be the same!
     * @param table Data for table.
     * @param newRow Data for new row.
     * @param rowIndex Row index for the new row, from 0 to number of rows.
     * @return The same data including row column if the row index is valid.
     */
    public static String[] addRowInTable(String[] table, String newRow, int rowIndex)
    {
    	if (rowIndex < 0 || rowIndex > table.length) return table;
    	if (rowIndex == 0) return DataSet.addStringArray(new String[] {newRow}, table);
    	String out[] = DataSet.addStringArray(DataSet.getSubArray(table, 0, rowIndex-1), new String[] {newRow});
    	out = DataSet.addStringArray(out, DataSet.getSubArray(table, rowIndex, table.length-1));
    	return out;
    }

	/**
	 * Replaces all matches of a string with another string.
	 * @param table String to be modified.
	 * @param symbol String to search for.
	 * @param replacement String to replace the last one. If null
	 * the replacement will be with an empty string.
	 * @param hasStrangeSymbols True if the symbol/replacement has
	 * 'strange symbols' like backslashes or so. If true direct String method
	 * will be used (indexOf), if false intrinsic replaceAll will be used (faster).
	 * A basic test that searches for strange symbols is done to try to avoid
	 * problems when using intrinsic {@linkplain String#replaceAll(String, String)}.
	 * @return The new string with the replacements.
	 */
	public static String replaceAll(String table, String symbol, String replacement, boolean hasStrangeSymbols)
	{
		if (table == null) return null;
		if (table.indexOf(symbol) < 0) return table;
		if (replacement == null) replacement = "";
		if (!hasStrangeSymbols) {
			// Basic check
			String strange = ".,\\/\"|#$%~\u00bd&()=?'\u00bf\u00a1!";
			for (int i=0; i<strange.length(); i++) {
				if (symbol.indexOf(strange.substring(i, i+1)) >= 0 ||
						replacement.indexOf(strange.substring(i, i+1)) >= 0) {
					hasStrangeSymbols = true;
					break;
				}
			}
			if (!hasStrangeSymbols) return table.replaceAll(symbol, replacement);
		}
		String array[] = DataSet.toStringArray(table, symbol, false);
		return DataSet.toString(array, replacement);
		// Old code. kept if needed, but it is slower
/*		int a = table.indexOf(symbol);
		if (a >= 0)
		{
			do
			{
				String tmp = table.substring(0, a);
				if (replacement != null) tmp+= replacement;
				tmp += table.substring(a + symbol.length());

				table = tmp;
				a = table.indexOf(symbol);
			} while (a >= 0);
		}

		return table;
*/	}

	/**
	 * Replaces all matches of a string with another string.
	 * This functions works better than the intrinsic
	 * {@linkplain String#replaceFirst(String, String)}.
	 * @param table String to be modified.
	 * @param symbol String to search for.
	 * @param replacement String to replace the last one.
	 * @param pos Position of the coincidence, starting from 1 (first).
	 * @return The new string with the replacements.
	 */
	public static String replaceOne(String table, String symbol, String replacement, int pos)
	{
		int a = table.indexOf(symbol);
		if (a >= 0)
		{
			int posit = 0;
			String tmp = table.substring(0, a);
			do
			{
				posit ++;
				if (replacement != null && posit == pos) {
					tmp+= replacement;
					tmp += table.substring(a + symbol.length());
					break;
				}

				if (table.substring(a+symbol.length()).indexOf(symbol) < 0) {
					a = -1;
				} else {
					int b = a;
					a = a + symbol.length() + table.substring(a+symbol.length()).indexOf(symbol);
					tmp += table.substring(b, a);
				}
			} while (a >= 0);
			table = tmp;
		}

		return table;
	}
	/**
	 * Replaces all matches of a string with another string.
	 * @param array String array to be modified.
	 * @param symbol String to search for.
	 * @param replacement String to replace the last one.
	 * @param hasStrangeSymbols True if the symbol/replacement has
	 * 'strange symbols' like backslashes or so. If true direct String method
	 * will be used (indexOf), if false intrinsic replaceAll will be used (faster).
	 * @return The new string array with the replacements.
	 */
	public static String[] replaceAll(String array[], String symbol, String replacement, boolean hasStrangeSymbols)
	{
		String out[] = new String[array.length];

		for (int i=0; i<array.length; i++)
		{
			out[i] = DataSet.replaceAll(array[i], symbol, replacement, hasStrangeSymbols);
		}

		return out;
	}

	/**
	 * Replaces all matches of a string with another string.
	 * This functions works better than the intrinsic
	 * {@linkplain String#replaceFirst(String, String)}.
	 * @param array String array to be modified by rows.
	 * @param symbol String to search for.
	 * @param replacement String to replace the last one.
	 * @param pos Position of the coincidence, starting from 1 (first).
	 * @return The new string array with the replacements.
	 */
	public static String[] replaceOne(String array[], String symbol, String replacement, int pos)
	{
		String out[] = new String[array.length];

		for (int i=0; i<array.length; i++)
		{
			out[i] = DataSet.replaceOne(array[i], symbol, replacement, pos);
		}

		return out;
	}

	/**
	 * Sorts a given array alphabetically.
	 * @param array The array. Note it is modified, clone it in input to avoid it.
	 * @return The alphabetically sorted array.
	 */
	public static String[] sortAlphabetically(String array[])
	{
		List list = Arrays.asList(array);
		Collections.sort(list);
		return (String[]) list.toArray();
	}

    /**
     * Obtains a sub-dataset.
     * @param v The array of values.
     * @param index0 The starting index.
     * @param indexf The final index.
     * @return The sub-array.
     */
    public static double[] getSubArray(double[] v, int index0, int indexf)
    {
    	int l = indexf - index0 + 1;
    	double o[] = new double[l];
    	System.arraycopy(v, index0, o, 0, l);
    	return o;
    }

    /**
     * Obtains a sub-dataset.
     * @param v The array of values.
     * @param index0 The starting index.
     * @param indexf The final index.
     * @return The sub-array.
     */
    public static int[] getSubArray(int[] v, int index0, int indexf)
    {
    	int l = indexf - index0 + 1;
    	int o[] = new int[l];
    	System.arraycopy(v, index0, o, 0, l);
    	return o;
    }

    /**
     * Obtains a sub-dataset.
     * @param v The array of values.
     * @param index0 The starting index.
     * @param indexf The final index.
     * @return The sub-array.
     */
    public static float[] getSubArray(float[] v, int index0, int indexf)
    {
    	int l = indexf - index0 + 1;
    	float o[] = new float[l];
    	System.arraycopy(v, index0, o, 0, l);
    	return o;
    }

    /**
     * Obtains a sub-dataset.
     * @param v The array of values.
     * @param index0 The starting index.
     * @param indexf The final index.
     * @return The sub-array.
     */
    public static String[] getSubArray(String[] v, int index0, int indexf)
    {
    	int l = indexf - index0 + 1;
    	String o[] = new String[l];
    	System.arraycopy(v, index0, o, 0, l);
    	return o;
    }
    /**
     * Obtains a sub-dataset.
     * @param v The array of objects.
     * @param index0 The starting index.
     * @param indexf The final index.
     * @return The sub-array.
     */
    public static Object[] getSubArray(Object[] v, int index0, int indexf)
    {
    	int l = indexf - index0 + 1;
    	//Object o[] = new Object[l];
    	Object o[] = (Object[]) Array.newInstance(v.getClass().getComponentType(), l);
    	System.arraycopy(v, index0, o, 0, l);
    	return o;
    }

    /**
     * Returns the limit '&lt;' or '&gt;'.
     * @param val The value.
     * @return The limit, or an empty String if it does not exist.
     */
    public static String getLimit(String val)
    {
    	String limit = "";
    	if (val.startsWith("<")) limit = "<";
    	if (val.startsWith(">")) limit = ">";
    	return limit;
    }

    /**
     * Transforms an array to a string.
     * @param array The array.
     * @param separator The separator.
     * @return The output string.
     */
    public static String toString(String[] array, String separator)
    {
    	if (array == null) return null;
    	if (array.length == 0) return "";

    	StringBuilder v = new StringBuilder(10 + array.length * array[0].length() / 2);
		v.append(array[0]);
		String s = "";
		if (separator != null) s = separator;
    	for (int i=1; i<array.length; i++)
    	{
    		v.append(s);
    		v.append(array[i]);
    	}
    	return v.toString();
    }

    /**
     * Returns the closest index to a given value in an array.
     * @param array The array.
     * @param value The value to search for.
     * @return The index of the closest value in the array to the input value.
     */
    public static int getClosestIndex(double[] array, double value)
    {
    	int index = 0;
    	double min = 0.0;
    	for (int i=0; i<array.length; i++)
    	{
    		double v = Math.abs(array[i] - value);
    		if (i==0 || v < min) {
    			min = v;
    			index = i;
    		}
    	}
    	return index;
    }

    /**
     * Returns the indexes of some specific element in a given array of elements.
     * @param val The array.
     * @param value The value to search for.
     * @return The indexes of the value in the array.
     */
    public static int[] getRepeatedElements(String val[], String value)
    {
    	ArrayList<Integer> v = new ArrayList<Integer>();
    	for (int i=0; i<val.length; i++)
    	{
    		if (val[i].equals(value)) v.add(new Integer(i));
    	}
    	int out[] = new int[v.size()];
    	for (int i=0; i<v.size(); i++)
    	{
    		out[i] = (v.get(i)).intValue();
    	}
    	return out;
    }

    /**
     * Returns a given set of indexes in an array.
     * @param val The array.
     * @param indexes The indexes to extract.
     * @return The selected indexes of the array as a new array.
     */
    public static String[] getSubArray(String val[], int indexes[])
    {
    	String out[] = new String[indexes.length];
    	int index = -1;
    	for (int i=0; i<indexes.length; i++)
    	{
    		index ++;
    		out[index] = val[indexes[i]];
    	}
    	return out;
    }

    /**
     * Returns a given set of indexes in an array.
     * @param val The array.
     * @param indexes The indexes to extract.
     * @return The selected indexes of the array as a new array.
     */
    public static double[] getSubArray(double val[], int indexes[])
    {
    	double out[] = new double[indexes.length];
    	int index = -1;
    	for (int i=0; i<indexes.length; i++)
    	{
    		index ++;
    		out[index] = val[indexes[i]];
    	}
    	return out;
    }

    /**
     * Return the different elements contained in an array, where
     * some elements could be repeated.
     * @param val The array of values.
     * @return The array of the individual elements.
     */
    public static String[] getDifferentElements(String val[])
    {
    	if (val.length == 0) return new String[] {};
    	ArrayList<String> v = new ArrayList<String>();
    	v.add(val[0]);
    	for (int i=1; i<val.length; i++)
    	{
    		if (!v.contains(val[i])) v.add(val[i]);
    	}
    	return DataSet.arrayListToStringArray(v);
    }

	/**
	 * Obtains double array 2d representing a 2d array of floats.
	 *
	 * @param array Input array 2d of floats.
	 * @return Double array 2d.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double[][] toDoubleArray(float[][] array) throws JPARSECException
	{
		double out[][] = new double[array.length][array[0].length];

		for (int i = 0; i < array.length; i++)
		{
			for (int j = 0; j < array[0].length; j++)
			{
				out[i][j] = (double) array[i][j];
			}
		}
		return out;
	}

	/**
	 * Obtains double array 2d (matrix) representing a set of double arrays
	 * given as columns.
	 *
	 * @param columns The set of columns, all should have same number of elements, or
	 * at least the first one must be the longest one.
	 * @return Double array 2d.
	 */
	public static double[][] toDoubleArray(ArrayList<double[]> columns)
	{
		double out[][] = new double[columns.size()][columns.get(0).length];

		for (int i = 0; i < columns.size(); i++)
		{
			out[i] = columns.get(i);
		}
		return out;
	}

	/**
	 * Obtains double array 2d representing a 2d array of floats.
	 *
	 * @param array Input array 2d of floats.
	 * @return Double array 2d.
	 * @throws JPARSECException If an error occurs.
	 */
	public static Double[][] toDoubleObjectArray(float[][] array) throws JPARSECException
	{
		Double out[][] = new Double[array.length][array[0].length];

		for (int i = 0; i < array.length; i++)
		{
			for (int j = 0; j < array[0].length; j++)
			{
				out[i][j] = (double) array[i][j];
			}
		}
		return out;
	}

	/**
	 * Obtains short array 2d representing a 2d array of doubles,
	 * using cast.
	 *
	 * @param array Input array 2d of doubles.
	 * @return Short array 2d.
	 * @throws JPARSECException If an error occurs.
	 */
	public static short[][] toShortArray(double[][] array) throws JPARSECException
	{
		short out[][] = new short[array.length][array[0].length];

		for (int i = 0; i < array.length; i++)
		{
			for (int j = 0; j < array[0].length; j++)
			{
				out[i][j] = (short) array[i][j];
			}
		}
		return out;
	}

	/**
	 * Obtains int array 2d representing a 2d array of shorts.
	 *
	 * @param array Input array 2d of shorts.
	 * @param offset Value to be added to the output ints, for instance
	 * +32768.
	 * @return Integer array 2d.
	 * @throws JPARSECException If an error occurs.
	 */
	public static int[][] toIntArray(short[][] array, int offset) throws JPARSECException
	{
		int out[][] = new int[array.length][array[0].length];

		for (int i = 0; i < array.length; i++)
		{
			for (int j = 0; j < array[0].length; j++)
			{
				out[i][j] = array[i][j] + offset;
			}
		}
		return out;
	}

	/**
	 * Obtains short array 2d representing a 2d array of ints.
	 *
	 * @param array Input array 2d of ints.
	 * @param offset Value to be added to the input ints before the cast to short,
	 * for instance -32768.
	 * @return Short array 2d.
	 * @throws JPARSECException If an error occurs.
	 */
	public static short[][] toShortArray(int[][] array, int offset) throws JPARSECException
	{
		short out[][] = new short[array.length][array[0].length];

		for (int i = 0; i < array.length; i++)
		{
			for (int j = 0; j < array[0].length; j++)
			{
				out[i][j] = (short) (array[i][j] + offset);
			}
		}
		return out;
	}

	/**
	 * Obtains int array 2d representing a 2d array of bytes.
	 *
	 * @param array Input array 2d of bytes.
	 * @param offset A constant to add to each output int value,
	 * for instance +128.
	 * @return Integer array 2d.
	 * @throws JPARSECException If an error occurs.
	 */
	public static int[][] toIntArray(byte[][] array, int offset) throws JPARSECException
	{
		int out[][] = new int[array.length][array[0].length];

		for (int i = 0; i < array.length; i++)
		{
			for (int j = 0; j < array[0].length; j++)
			{
				out[i][j] = array[i][j] + offset;
			}
		}
		return out;
	}

	/**
	 * Obtains byte array 2d representing a 2d array of ints.
	 *
	 * @param array Input array 2d of ints.
	 * @param offset A constant to add to each input int value before
	 * the cast to byte, for instance -128.
	 * @return Byte array 2d.
	 * @throws JPARSECException If an error occurs.
	 */
	public static byte[][] toByteArray(int[][] array, int offset) throws JPARSECException
	{
		byte out[][] = new byte[array.length][array[0].length];

		for (int i = 0; i < array.length; i++)
		{
			for (int j = 0; j < array[0].length; j++)
			{
				out[i][j] = (byte) (array[i][j] + offset);
			}
		}
		return out;
	}

	/**
	 * Obtains byte array 2d representing a 2d array of doubles,
	 * using simple cast.
	 *
	 * @param array Input array 2d of doubles.
	 * @return Byte array 2d.
	 * @throws JPARSECException If an error occurs.
	 */
	public static byte[][] toByteArray(double[][] array) throws JPARSECException
	{
		byte out[][] = new byte[array.length][array[0].length];

		for (int i = 0; i < array.length; i++)
		{
			for (int j = 0; j < array[0].length; j++)
			{
				out[i][j] = (byte) array[i][j];
			}
		}
		return out;
	}

	/**
	 * Obtains a byte array 1d representing an array of integers,
	 * using simple cast.
	 *
	 * @param array Input array 1d of integers.
	 * @return Byte array 1d.
	 * @throws JPARSECException If an error occurs.
	 */
	public static byte[] toByteArray(int[] array) throws JPARSECException
	{
		byte out[] = new byte[array.length];

		for (int i = 0; i < array.length; i++)
		{
			out[i] = (byte) array[i];
		}
		return out;
	}

	/**
	 * Obtains float array 2d representing a 2d array of doubles.
	 *
	 * @param array Input array 2d of doubles.
	 * @return Float array 2d.
	 * @throws JPARSECException If an error occurs.
	 */
	public static float[][] toFloatArray(double[][] array) throws JPARSECException
	{
		float out[][] = new float[array.length][array[0].length];

		for (int i = 0; i < array.length; i++)
		{
			for (int j = 0; j < array[0].length; j++)
			{
				out[i][j] = (float) array[i][j];
			}
		}
		return out;
	}

	/**
	 * Obtains float array representing an array of doubles.
	 *
	 * @param array Input array of doubles.
	 * @return Float array.
	 */
	public static float[] toFloatArray(double[] array)
	{
		float out[] = new float[array.length];

		for (int i = 0; i < array.length; i++)
		{
			out[i] = (float) array[i];
		}
		return out;
	}

	/**
	 * Obtains double array representing an array of floats.
	 *
	 * @param array Input array of floats.
	 * @return Doubles array.
	 */
	public static double[] toDoubleArray(float[] array)
	{
		double out[] = new double[array.length];

		for (int i = 0; i < array.length; i++)
		{
			out[i] = array[i];
		}
		return out;
	}

	/**
	 * Obtains an int array 2d representing a 2d array of doubles. The values and rounded
	 * to the nearest integer.
	 *
	 * @param array Input array 2d of doubles.
	 * @return Integer array 2d.
	 * @throws JPARSECException If an error occurs.
	 */
	public static int[][] toIntArray(double[][] array) throws JPARSECException
	{
		int out[][] = new int[array.length][array[0].length];

		for (int i = 0; i < array.length; i++)
		{
			for (int j = 0; j < array[0].length; j++)
			{
				out[i][j] = (int) Math.floor(array[i][j] + 0.5);
			}
		}
		return out;
	}

	/**
	 * Obtains an int array by converting double values to integers. The values and rounded
	 * to the nearest integer.
	 *
	 * @param array Input array of doubles.
	 * @return Integer array.
	 * @throws JPARSECException If an error occurs.
	 */
	public static int[] toIntArray(double[] array) throws JPARSECException
	{
		int out[] = new int[array.length];

		for (int i = 0; i < array.length; i++)
		{
			out[i] = (int) Math.floor(array[i] + 0.5);
		}
		return out;
	}

	/**
	 * Obtains double array 2d representing a 2d array of ints.
	 *
	 * @param array Input array 2d of ints.
	 * @return Double array 2d.
	 * @throws JPARSECException If an error occurs.
	 */
	public static double[][] toDoubleArray(int[][] array) throws JPARSECException
	{
		double out[][] = new double[array.length][array[0].length];

		for (int i = 0; i < array.length; i++)
		{
			for (int j = 0; j < array[0].length; j++)
			{
				out[i][j] = (double) array[i][j];
			}
		}
		return out;
	}

	/**
	 * Obtains an adequate set of values between two points.
	 * @param x0 Initial value.
	 * @param xf Final value.
	 * @param npoints Number of points.
	 * @param logScale True to obtain the values in log scale.
	 * @return Array with the values.
	 */
	public static double[] getSetOfValues(double x0, double xf, int npoints,
			boolean logScale)
	{
		double w[] = new double[npoints];
		if (logScale) {
			x0 = Math.log(x0);
			xf = Math.log(xf);
		}
		double step = (xf - x0) / (npoints - 1.0);
		double lambda = x0;
		int i = -1;
		do
		{
			i++;
			if (logScale) {
				w[i] = Math.pow(Math.E, lambda);
			} else {
				w[i] = lambda;
			}
			lambda = lambda + step;
		} while (i < (npoints - 1));
		return w;
	}

	/**
	 * Obtains an adequate set of values between two points.
	 * @param x0 Initial value.
	 * @param xf Final value.
	 * @param step Step between points.
	 * @return Array with the values.
	 */
	public static double[] getSetOfValues(double x0, double xf, double step)
	{
		int npoints = (int) ((xf - x0) / step + 1);
		double w[] = new double[npoints];
		double lambda = x0;
		int i = -1;
		do
		{
			i++;
			w[i] = lambda;
			lambda = lambda + step;
		} while (i < (npoints - 1));
		return w;
	}
	
	/**
	 * Removes an index from an array.
	 * @param input The array.
	 * @param index The index to remove.
	 * @return The new array or a copy of the input if the index is invalid.
	 */
	public static double[] deleteIndex(double[] input, int index)
	{
		if (index < 0 || index >= input.length) return input.clone();
		double out[] = new double[input.length-1];
		if (index > 0) System.arraycopy(input, 0, out, 0, index);
		if (index < input.length - 1) System.arraycopy(input, index + 1, out, index, input.length - 1 - index);
		return out;
	}

	/**
	 * Removes an index from an array.
	 * @param input The array.
	 * @param index The index to remove.
	 * @return The new array or a copy of the input if the index is invalid.
	 */
	public static int[] deleteIndex(int[] input, int index)
	{
		if (index < 0 || index >= input.length) return input.clone();
		int out[] = new int[input.length-1];
		if (index > 0) System.arraycopy(input, 0, out, 0, index);
		if (index < input.length - 1) System.arraycopy(input, index + 1, out, index, input.length - 1 - index);
		return out;
	}

	/**
	 * Removes an index from an array.
	 * @param input The array.
	 * @param index The index to remove.
	 * @return The new array or a copy of the input if the index is invalid.
	 */
	public static long[] deleteIndex(long[] input, int index)
	{
		if (index < 0 || index >= input.length) return input.clone();
		long out[] = new long[input.length-1];
		if (index > 0) System.arraycopy(input, 0, out, 0, index);
		if (index < input.length - 1) System.arraycopy(input, index + 1, out, index, input.length - 1 - index);
		return out;
	}

	/**
	 * Removes an index from an array.
	 * @param input The array.
	 * @param index The index to remove.
	 * @return The new array or the input one if the index is invalid.
	 */
	public static Object[] deleteIndex(Object[] input, int index)
	{
		if (index < 0 || index >= input.length) return input;
    	Object out[] = (Object[]) Array.newInstance(input.getClass().getComponentType(), input.length-1);
		if (index > 0) System.arraycopy(input, 0, out, 0, index);
		if (index < input.length - 1) System.arraycopy(input, index + 1, out, index, input.length - 1 - index);
		return out;
	}

	/**
	 * Reverses the order of the input array. Input array is not
	 * changed.
	 * @param data Input array.
	 * @return Same data but in inverted ordering.
	 */
	public static double[] reverse(double data[]) {
		double out[] = new double[data.length];
		for (int i=0; i<data.length; i++) {
			out[i] = data[data.length-1-i];
		}
		return out;
	}

	/**
	 * Reverses the order of the input array. Input array IS
	 * changed.
	 * @param in Input/output array.
	 */
	public static void reverse2(double in[]) {
		Collections.reverse(Arrays.asList(in));
	}

	/**
	 * Reverses the order of the input array. Input array IS
	 * changed.
	 * @param in Input/output array.
	 */
	public static void reverse2(Object in[]) {
		Collections.reverse(Arrays.asList(in));
	}

	/**
	 * Reverses the order of the input array.
	 * @param data Input array.
	 * @return Same data but in inverted ordering.
	 */
	public static String[] reverse(String data[]) {
		String out[] = new String[data.length];
		for (int i=0; i<data.length; i++) {
			out[i] = data[data.length-1-i];
		}
		return out;
	}

	/**
	 * A method for reordering objects in crescent order of some values in another
	 * array.
	 *
	 * @param list Array of objects.
	 * @param val Values for each of the objects
	 * @return ArrayList with the objects listed in crescent order of the values.
	 * @throws JPARSECException If an error occurs.
	 */
	public static ArrayList sortInCrescent(ArrayList list, double val[])
	throws JPARSECException {
		if (val == null || list == null) return null;
		ArrayList out = new ArrayList();
		if (val.length == 0 || list.size() == 0) return out;

		ArrayList l = (ArrayList) list.clone();

		double v[] = val.clone();
		do {
			int index = DataSet.getIndexOfMinimum(v);
			out.add(l.get(index));

			v = DataSet.deleteIndex(v, index);
			l.remove(index);
		} while (v.length > 0);
		return out;
	}

	/**
	 * A method for reordering objects in crescent order of some values in another
	 * array.
	 *
	 * @param list Array of objects.
	 * @param val Values for each of the objects
	 * @return Array with the objects listed in crescent order of the values.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String[] sortInCrescent(String list[], double val[])
	throws JPARSECException {
		if (val == null || list == null) return null;
		String[] out = new String[list.length];

		if (val.length == 0 || list.length == 0) return out;

		double v[] = val.clone();
		String l[] = list.clone();
		int oindex = 0;
		do {
			int index = DataSet.getIndexOfMinimum(v);
			out[oindex] = l[index];
			oindex ++;

			v = DataSet.deleteIndex(v, index);
			l = (String[]) DataSet.deleteIndex(l, index);
		} while (oindex < list.length);
		return out;
	}

	/**
	 * A method for reordering objects in crescent order of some values in another
	 * array.
	 *
	 * @param list Array of objects.
	 * @param val Values for each of the objects. Check none is infinity or Double.MAX_VALUE.
	 * @return Array with the objects listed in crescent order of the values.
	 * @throws JPARSECException If an error occurs.
	 */
	public static Object[] sortInCrescent(Object list[], double val[])
	throws JPARSECException {
		if (val == null || list == null) return null;
		//Object[] out = new Object[list.length];
    	Object out[] = (Object[]) Array.newInstance(list.getClass().getComponentType(), list.length);

		if (val.length == 0 || list.length == 0) return out;

		double v[] = val.clone();
		int oindex = 0;
		do {
			int index = DataSet.getIndexOfMinimum(v);
			out[oindex] = list[index];
			oindex ++;

			v[index] = Double.MAX_VALUE;
		} while (oindex < list.length);
		return out;
	}

	/**
	 * A method for reordering objects in descent order of some values in another
	 * array.
	 *
	 * @param list Array of objects.
	 * @param val Values for each of the objects
	 * @return ArrayList with the objects listed in descent order of the values.
	 * @throws JPARSECException If an error occurs.
	 */
	public static ArrayList<String[]> sortInDescent(ArrayList<String[]> list, double val[])
	throws JPARSECException {
		if (val == null || list == null) return null;
		ArrayList<String[]> out = new ArrayList<String[]> ();
		if (val.length == 0 || list.size() == 0) return out;

		ArrayList<String[]> l = (ArrayList<String[]>) list.clone();

		double v[] = val.clone();
		do {
			int index = DataSet.getIndexOfMaximum(v);
			out.add(l.get(index));

			v = DataSet.deleteIndex(v, index);
			l.remove(index);
		} while (v.length > 0);
		return out;
	}

	/**
	 * A method for reordering objects in descent order of some values in another
	 * array.
	 *
	 * @param list Array of objects.
	 * @param val Values for each of the objects
	 * @return Array with the objects listed in descent order of the values.
	 * @throws JPARSECException If an error occurs.
	 */
	public static String[] sortInDescent(String list[], double val[])
	throws JPARSECException {
		if (val == null || list == null) return null;
		String[] out = new String[list.length];

		if (val.length == 0 || list.length == 0) return out;

		double v[] = val.clone();
		String l[] = list.clone();
		int oindex = 0;
		do {
			int index = DataSet.getIndexOfMaximum(v);
			out[oindex] = l[index];
			oindex ++;

			v = DataSet.deleteIndex(v, index);
			l = (String[]) DataSet.deleteIndex(l, index);
		} while (oindex < list.length);
		return out;
	}

	/**
	 * A method for reordering objects in descent order of some values in another
	 * array.
	 *
	 * @param list Array of objects.
	 * @param val Values for each of the objects. Check none of them is -infinity or Double.MIN_VALUE.
	 * @return Array with the objects listed in descent order of the values.
	 * @throws JPARSECException If an error occurs.
	 */
	public static Object[] sortInDescent(Object list[], double val[])
	throws JPARSECException {
		if (val == null || list == null) return null;
		//Object[] out = new Object[list.length];
    	Object out[] = (Object[]) Array.newInstance(list.getClass().getComponentType(), list.length);

		if (val.length == 0 || list.length == 0) return out;

		double v[] = val.clone();
		int oindex = 0;
		do {
			int index = DataSet.getIndexOfMaximum(v);
			out[oindex] = list[index];
			oindex ++;

			v[index] = -Double.MAX_VALUE;
		} while (oindex < list.length);
		return out;
	}

	/**
	 * Returns if the values in two arrays are the same.
	 * @param in1 First array.
	 * @param in2 Second array.
	 * @return True if both arrays have similar values, even
	 * if they are null or empty.
	 */
	public static boolean sameArrayValues(double in1[], double in2[]) {
		if (in1 == null && in2 == null) return true;
		if (in1 == null || in2 == null || in1.length != in2.length) return false;
		if (in1.length == 0 && in2.length == 0) return true;

		for (int i = 0; i < in1.length; i++) {
			if (in1[i] != in2[i]) return false;
		}
		return true;
	}

	/**
	 * Returns if the values in two arrays are the same.
	 * @param in1 First array.
	 * @param in2 Second array.
	 * @return True if both arrays have similar values, even
	 * if they are null or empty.
	 */
	public static boolean sameArrayValues(String in1[], String in2[]) {
		if (in1 == null && in2 == null) return true;
		if (in1.length == 0 && in2.length == 0) return true;
		if (in1 == null || in2 == null || in1.length != in2.length) return false;

		for (int i = 0; i < in1.length; i++) {
			if (!in1[i].equals(in2[i])) return false;
		}
		return true;
	}

  /**
   * Returns the dimensions of the given array. Even though the
   * parameter is of type "Object" one can hand over primitve arrays, e.g.
   * int[3] or double[2][4].
   * <P>Method by Joseph A. Huwaldt.
   *
   * @param array       the array to determine the dimensions for
   * @return            the dimensions of the array
   */
  private static int getArrayDimensions(Class array) {
    if (array.getComponentType().isArray())
      return 1 + getArrayDimensions(array.getComponentType());
    else
      return 1;
  }

/**
   * Returns the dimensions of the given array. Even though the
   * parameter is of type "Object" one can hand over primitve arrays, e.g.
   * int[3] or double[2][4].
   * <P>Method by Joseph A. Huwaldt.
   *
   * @param array       the array to determine the dimensions for
   * @return            the dimensions of the array
   */
  private static int getArrayDimensions(Object array) {
    return getArrayDimensions(array.getClass());
  }

/**
   * Returns the given Array in a string representation. Even though the
   * parameter is of type "Object" one can hand over primitve arrays, e.g.
   * int[3] or double[2][4].
   * <P>Method by Joseph A. Huwaldt.
   *
   * @param array       the array to return in a string representation
   * @return            the array as string
   */
  public static String arrayToString(Object array) {
    String        result;
    int           dimensions;
    int           i;

    result     = "";
    dimensions = getArrayDimensions(array);

    if (dimensions == 0) {
      result = "null";
    }
    else if (dimensions == 1) {
      for (i = 0; i < Array.getLength(array); i++) {
        if (i > 0)
          result += ",";
        if (Array.get(array, i) == null)
          result += "null";
        else
          result += Array.get(array, i).toString();
      }
    }
    else {
      for (i = 0; i < Array.getLength(array); i++) {
        if (i > 0)
          result += ",";
        result += "[" + arrayToString(Array.get(array, i)) + "]";
      }
    }

    return result;
  }

  /**
   * Repeats a given string s n times.
   * @param s The string.
   * @param n Number of times to repeat.
   * @return n times the string s.
   */
  public static String repeatString(String s, int n) {
	  if (n <= 10) {
		  if (n == 0) return "";
		  if (n == 1) return s;
		  String s2 = s + s;
		  if (n == 2) return s2;
		  if (n == 3) return s2+s;
		  if (n == 4) return s2+s2;
		  if (n == 5) return s2+s2+s;
		  String s4 = s2 + s2;
		  if (n == 6) return s4+s2;
		  if (n == 7) return s4+s2+s;
		  if (n == 8) return s4+s4;
		  if (n == 9) return s4+s4+s;
		  if (n == 10) return s4+s4+s2;
/*
		  if (n == 11) return s4+s4+s2+s;
		  if (n == 12) return s4+s4+s4;
		  if (n == 13) return s4+s4+s4+s;
		  if (n == 14) return s4+s4+s4+s2;
		  if (n == 15) return s4+s4+s4+s2+s;
		  if (n == 16) return s4+s4+s4+s4;
		  if (n == 17) return s4+s4+s4+s4+s;
		  if (n == 18) return s4+s4+s4+s4+s2;
*/
	  }
      final StringBuilder sb = new StringBuilder(s);
      for(int i = 1; i < n; i++) {
          sb.append(s);
      }
      return sb.toString();

  }

  private static final String Digits     = "(\\p{Digit}+)";
  private static final String HexDigits  = "(\\p{XDigit}+)";

  // an exponent is 'e' or 'E' followed by an optionally
  // signed decimal integer.
  private static final String Exp        = "[eE][+-]?"+Digits;
  private static final String fpRegex    =
		  ("[\\x00-\\x20]*"+  // Optional leading "whitespace"
				  "[+-]?(" + // Optional sign character
				  "NaN|" +           // "NaN" string
				  "Infinity|" +      // "Infinity" string

             // A decimal floating-point string representing a finite positive
             // number without a leading sign has at most five basic pieces:
             // Digits . Digits ExponentPart FloatTypeSuffix
             //
             // Since this method allows integer-only strings as input
             // in addition to strings of floating-point literals, the
             // two sub-patterns below are simplifications of the grammar
             // productions from the Java Language Specification, 2nd
             // edition, section 3.10.2.

             // Digits ._opt Digits_opt ExponentPart_opt FloatTypeSuffix_opt
             "((("+Digits+"(\\.)?("+Digits+"?)("+Exp+")?)|"+

             // . Digits ExponentPart_opt FloatTypeSuffix_opt
             "(\\.("+Digits+")("+Exp+")?)|"+

       // Hexadecimal strings
       "((" +
        // 0[xX] HexDigits ._opt BinaryExponent FloatTypeSuffix_opt
        "(0[xX]" + HexDigits + "(\\.)?)|" +

        // 0[xX] HexDigits_opt . HexDigits BinaryExponent FloatTypeSuffix_opt
        "(0[xX]" + HexDigits + "?(\\.)" + HexDigits + ")" +

        ")[pP][+-]?" + Digits + "))" +
             "[fFdD]?))" +
             "[\\x00-\\x20]*");// Optional trailing "whitespace"
  private static final String invalidChars = "ABCDFGHIJKLNOPQRSTUVWXYZ/*+()%&<>?=abcdeghijklmnopqrstuvwxyz"; // "0123456789.E+-/*+()%&<>?Math";
  private static final String invalidChars2 = "ABCDFGHJKLNOQRSTUVWXYZjkvwz"; // "0123456789.E+-/*+()%&<>?=MathPI";
  private static final Pattern pattern = Pattern.compile(fpRegex);
  private static Evaluation eval = new Evaluation("", null);

  /**
   * Checks if a given String is a double number or not, using a 'fast
   * check'. Fast check just checks if the String contains some invalid
   * characters, any of these: ABCDFGHIJKLNOPQRSTUVWXYZ/*+()%&amp;&lt;&gt;?abcdeghijklmnopqrstuvwxyz.
   * @param s The string.
   * @return True or false. Note a double string can be valid while starting with a +.
   */
  public static boolean isDoubleFastCheck(String s) {
	  if (s == null || s.isEmpty()) return false;
	  for (int i = 0; i < s.length(); i++)
	  {
		  if (invalidChars.indexOf(s.charAt(i)) >= 0) return false;
	  }
	  return true;
  }

  /**
   * Checks if a given String is a double number or a possibly valid math
   * operation, using a 'fast check'. Fast check just checks if the String
   * contains some invalid characters, any of these: ABCDFGHJKLNOQRSTUVWXYZjkvwz.
   * @param s The string.
   * @return True or false.
   */
  public static boolean isDoubleOrMathOperationFastCheck(String s) {
	  if (s == null || s.isEmpty()) return false;
	  for (int i = 0; i < s.length(); i++)
	  {
		  if (invalidChars2.indexOf(s.charAt(i)) >= 0) return false;
	  }
	  return true;
  }

  /**
   * Checks if a given String is a double number or not, using a 'strict
   * check' and without launching exceptions. Check is similar to what Java
   * internally does, but it is slow.
   * @param s The string.
   * @return True or false.
   */
  public static boolean isDoubleStrictCheck(String s) {
	  if (s == null || s.isEmpty()) return false;
	  if (pattern.matcher(s).matches()) return true;
	  return false;
  }

  /**
   * Capitalize a String by setting the first character to
   * uppercase in the first word and, optionally, also the
   * others.
   * @param s The string.
   * @param all True to capitalize the first character in
   * all words, false for just the first.
   * @return The capitalized string.
   */
  public static String capitalize(String s, boolean all) {
	  if (s == null || s.equals("")) return s;
	  if (s.length() == 1) return s.toUpperCase();
	  if (!all) {
		  s = s.trim();
		  return s.substring(0, 1).toUpperCase() + s.substring(1);
		  /*
		  String f = FileIO.getField(1, s, " ", true);
		  f = f.substring(0, 1).toUpperCase() + f.substring(1);
		  if (FileIO.getNumberOfFields(s, " ", true) > 1) f = f + " " + FileIO.getRestAfterField(1, s, " ", true);
		  return f;
		  */
	  }
	  String val[] = DataSet.toStringArray(s, " ");
	  StringBuffer out = new StringBuffer("");
	  for (int i=0; i<val.length; i++) {
		  out.append(val[i].substring(0, 1).toUpperCase() + val[i].substring(1) + " ");
	  }
	  return out.toString().trim();
  }

  /**
   * Flips the data in a 2d array [axis1][axis2].
   * @param o The input array 2d, type primitive double, int, float, short, or byte.
   * @param flip12 Flips the axis1 and axis2 of the array.
   * @param invert1 True to invert the position of the output values in axis 1 for
   * the output array.
   * @param invert2 True to invert the position of the output values in axis 2 for
   * the output array.
   * @return Flipped array.
   * @throws JPARSECException If the type of the input data cannot be recognized.
   */
  public static Object flip2dArray(Object o, boolean flip12, boolean invert1, boolean invert2) throws JPARSECException {
		if (o == null) return null;
		double out[][];
		try {
			double[][] data = (double[][]) ArrayFuncs.convertArray(o, double.class);
			if (flip12) {
				out = new double[data[0].length][data.length];
			} else {
				out = new double[data.length][data[0].length];				
			}
			for (int i=0; i<data.length; i++)
			{
				int pi = i;
				if (invert1) pi = data.length-1-i;
				for (int j=0; j<data[i].length; j++)
				{
					int pj = j;
					if (invert2) pj = data[0].length-1-j;
					if (flip12) {
						out[pj][pi] = data[i][j];
					} else {
						out[pi][pj] = data[i][j];						
					}
				}
			}
		} catch (Exception exc)
		{
			return null;
		}

		String bc = ArrayFuncs.getBaseClass(o).getName();
		if (bc.equals("double")) return out;
		if (bc.equals("int")) return DataSet.toIntArray(out);
		if (bc.equals("float")) return DataSet.toFloatArray(out);
		if (bc.equals("short")) return DataSet.toShortArray(out);
		if (bc.equals("byte")) return DataSet.toByteArray(out);;
		throw new JPARSECException("Cannot recognize the type of the input data, should be a 2d array of type double, int, float, short, or byte.");
  }

  /**
   * Return the kth smallest value in a set of doubles.
   * @param in Values.
   * @param n Number of values to consider, this will be
   * usually the length of the array in.
   * @param k kth smallest value to return, 0 = first.
   * @return The kth smallest value in array in.
   */
  public static double getKthSmallestValue(double in[], int n, int k)
  {
	  double a[] = in.clone();
      int i,j,l,m ;
      double t, x ;

      l=0 ; m=n-1 ;
      while (l<m) {
          x=a[k] ;
          i=l ;
          j=m ;
          do {
              while (a[i]<x) i++ ;
              while (x<a[j]) j-- ;
              if (i<=j) {
            	  t=a[i];
            	  a[i]=a[j];
            	  a[j]=t;
                  i++ ;
                  j-- ;
              }
          } while (i<=j) ;
          if (j<k) l=i ;
          if (k<i) m=j ;
      }
      return a[k] ;
  }

  /**
   * Return the kth smallest value in a set of measures.
   * @param in Values.
   * @param n Number of values to consider, this will be
   * usually the length of the array in.
   * @param k kth smallest value to return.
   * @return The kth smallest value in array in.
   * @throws JPARSECException If an error occurs.
   */
  public static MeasureElement getKthSmallestValue(MeasureElement in[], int n, int k) throws JPARSECException
  {
	  MeasureElement a[] = in.clone();
      int i,j,l,m ;
      MeasureElement t, x ;

      l=0 ; m=n-1 ;
      while (l<m) {
          x=a[k] ;
          i=l ;
          j=m ;
          do {
              while (a[i].getValue()<x.getValue()) i++ ;
              while (x.getValue()<a[j].getValue()) j-- ;
              if (i<=j) {
            	  t=a[i];
            	  a[i]=a[j];
            	  a[j]=t;
                  i++ ;
                  j-- ;
              }
          } while (i<=j) ;
          if (j<k) l=i ;
          if (k<i) m=j ;
      }
      return a[k] ;
  }

  /**
   * Clones an array of doubles with two dimensions. Java's
   * clone method does not operate with 2 dimensions.
   * @param a The array of doubles.
   * @return A real clone of it.
   */
	public static double[][] cloneArray(double[][] a) {
		double[][] v = new double[a.length][];
		for (int i=0; i<v.length; i++) {
			v[i] = a[i].clone();
		}
		return v;
	}

	/**
	 * Trims a string from the leftmost position, eliminating only
	 * white spaces.
	 * @param s The string.
	 * @return The (left) trimmed string.
	 */
	public static String ltrim(String s) {
	    int i = 0;
	    while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
	        i++;
	    }
	    return s.substring(i);
	}

	/**
	 * Trims a string from the rightmost position, eliminating only
	 * white spaces.
	 * @param s The string.
	 * @return The (right) trimmed string.
	 */
	public static String rtrim(String s) {
	    int i = s.length()-1;
	    while (i >= 0 && Character.isWhitespace(s.charAt(i))) {
	        i--;
	    }
	    return s.substring(0,i+1);
	}

	/**
	 * Converts an array of Doubles to a double[] (primitive array).
	 * @param d The array of Doubles.
	 * @return Its primitive array.
	 */
	public static double[] toPrimitiveArrayDouble(Object[] d) {
		double out[] = new double[d.length];
		for (int i=0; i<d.length; i++) {
			out[i] = ((Double) d[i]).doubleValue();
		}
		return out;
	}

	/**
	 * Converts an array of Floats to a float[] (primitive array).
	 * @param d The array of Floats.
	 * @return Its primitive array.
	 */
	public static float[] toPrimitiveArrayFloat(Object[] d) {
		float out[] = new float[d.length];
		for (int i=0; i<d.length; i++) {
			out[i] = ((Float) d[i]).floatValue();
		}
		return out;
	}

	/**
	 * Converts an array of Integers to an int[] (primitive array).
	 * @param d The array of Integers.
	 * @return Its primitive array.
	 */
	public static int[] toPrimitiveArrayInteger(Object[] d) {
		int out[] = new int[d.length];
		for (int i=0; i<d.length; i++) {
			out[i] = ((Integer) d[i]).intValue();
		}
		return out;
	}

	/**
	 * Returns the number of dimensions in an array.
	 * @param o An object representing and array with an
	 * unknown number of dimensions.
	 * @return The number of dimensions, 0 or greater.
	 */
    public static int getNumberOfDimensions(Object o) {
    	return getNumberOfDimensions(o.getClass());
    }

	/**
	 * Returns the number of dimensions in an array.
	 * @param type An object representing and array with an
	 * unknown number of dimensions.
	 * @return The number of dimensions, 0 or greater.
	 */
    public static int getNumberOfDimensions(Class<?> type) {
        if (type.getComponentType() == null) {
            return 0;
        } else {
            return getNumberOfDimensions(type.getComponentType()) + 1;
        }
    }

    /**
     * Removes a given element value from a double array.
     * @param a The double array. Not changed.
     * @param valueToRemove The value to remove.
     * @return A new double array with the given value removed, if any.
     */
    public static double[] getSubArrayWithoutValue(double a[], double valueToRemove) {
    	double array[] = a.clone();
    	for (int i=array.length-1; i>=0; i--) {
    		if (array[i] == valueToRemove) {
    			if (i == 0) {
    				array = DataSet.getSubArray(array, i+1, array.length-1);
    			} else {
    				if (i == array.length-1) {
    					array = DataSet.getSubArray(array, 0, i-1);
    				} else {
    					array = DataSet.addDoubleArray(DataSet.getSubArray(array, 0, i-1), DataSet.getSubArray(array, i+1, array.length-1));
    				}
    			}
    		}
    	}
    	return array;
    }
}
