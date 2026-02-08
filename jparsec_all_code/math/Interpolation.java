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

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;

import jparsec.graph.ChartElement;
import jparsec.graph.ChartSeriesElement;
import jparsec.graph.CreateChart;
import jparsec.graph.DataSet;
import jparsec.graph.GridChartElement;
import jparsec.io.FileIO;
import jparsec.util.JPARSECException;
import jparsec.util.Translate;

/**
 * Tools for linear and spline interpolation.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Interpolation implements Serializable
{
	private static final long serialVersionUID = 1L;

	private double x_val[], y_val[], z_val[];
	private boolean allowExtrapolation;
	private double min, max;

	/**
	 * Constructor for a 2d interpolation. Points are sorted
	 * in abscissa crescent order, and repeated points are eliminated.
	 * @param x X values.
	 * @param y Y values.
	 * @param allowExtrapolation True to allow extrapolation.
	 * @throws JPARSECException If an error occurs.
	 */
	public Interpolation(double x[], double y[], boolean allowExtrapolation) throws JPARSECException {
		init2d(x, y, allowExtrapolation);
	}

	/**
	 * Constructor for a 3d interpolation.
	 * @param x X values.
	 * @param y Y values.
	 * @param z Z values.
	 * @param allowExtrapolation True to allow extrapolation.
	 * @throws JPARSECException If an error occurs.
	 * @deprecated Class {@linkplain GridChartElement}
	 * should be used instead, since it allows to use an irregurarly-sampled grid of points
	 * and its implementation has much more physical sense.
	 */
	public Interpolation(double x[], double y[], double z[], boolean allowExtrapolation) throws JPARSECException {
		if (z == null) {
			init2d(x, y, allowExtrapolation);
		} else {
			if (x != null) x_val = x.clone();
			if (y != null) y_val = y.clone();
			if (z != null) z_val = z.clone();
			this.allowExtrapolation = allowExtrapolation;
		}
	}

	private void init2d(double x[], double y[], boolean allowExtrapolation) throws JPARSECException {
		if (x != null && y != null) {
			ArrayList<double[]> l = DataSet.sortInCrescent(x, y, true);
			x_val = l.get(0);
			y_val = l.get(1);
		}
		this.allowExtrapolation = allowExtrapolation;
		min = DataSet.getMinimumValue(x_val);
		max = DataSet.getMaximumValue(x_val);

	}

	/**
	 * Linear interpolation method.
	 * @param x_point Interpolation point.
	 * @return The interpolated value.
	 * @throws JPARSECException If {@linkplain #allowExtrapolation} is false and the point is
	 *         outside the range.
	 */
	public double linearInterpolation(double x_point)
			throws JPARSECException
	{
		double x_prev, y_prev, x_next, y_next, slope;
		int v;

		// Get number of points
		v = x_val.length;

		// Obtain previous and next point
		x_prev = x_point;
		x_next = x_point;
		y_prev = 0.0;
		y_next = 0.0;

		int iprev = 0, inext = 0;
		for (int i = 0; i < v; i++)
		{
			if (x_val[i] == x_point)
				return y_val[i];

			if (x_val[i] < x_point && (x_val[i] > x_prev || x_prev == x_point))
			{
				x_prev = x_val[i];
				y_prev = y_val[i];
				iprev = i;

			}

			if (x_val[i] > x_point && (x_val[i] < x_next || x_next == x_point))
			{
				x_next = x_val[i];
				y_next = y_val[i];
				inext = i;
			}

		}

		// Correct values if no previous or next point exist, if extrapolation
		// is allowed
		if (allowExtrapolation)
		{
			if (x_prev == x_point && inext < x_val.length-1)
			{
				x_prev = x_val[inext + 1];
				y_prev = y_val[inext + 1];
			}
			if (x_next == x_point && iprev > 0)
			{
				x_next = x_val[iprev - 1];
				y_next = y_val[iprev - 1];
			}
		} else
		{
			if (x_prev == x_point || x_next == x_point)
			{
				throw new JPARSECException(
						"interpolation point "+x_point+" out of range "+DataSet.getMinimumValue(x_val)+"-"+DataSet.getMaximumValue(x_val)+", extrapolation required.");
			}
		}

		// Interpolate
		slope = 0.0;
		if (!(x_next == x_prev))
			slope = (y_next - y_prev) / (x_next - x_prev);
		double y_point = y_prev + slope * (x_point - x_prev);

		return y_point;
	}

	private double[] d2ydx2 = null;
   	private void calcDeriv(){
    	double p = 0.0, qn = 0.0, sig = 0.0, un = 0.0;
    	double[] u = new double[x_val.length];
    	d2ydx2 = new double[x_val.length];

        d2ydx2[0] = u[0] = 0.0;
    	for (int i=1; i<=x_val.length-2; i++){
	    	sig = (x_val[i] - x_val[i-1]) / (x_val[i+1] - x_val[i-1]);
	    	p = sig * d2ydx2[i-1] + 2.0;
	    	d2ydx2[i] = (sig - 1.0) / p;
	    	u[i] = (y_val[i+1] - y_val[i]) / (x_val[i+1] - x_val[i]) - (y_val[i] - y_val[i-1]) / (x_val[i] - x_val[i-1]);
	    	u[i] = (6.0 * u[i] / (x_val[i+1] - x_val[i-1]) - sig * u[i-1]) / p;
    	}

	    qn = un = 0.0;
    	d2ydx2[x_val.length-1] = (un - qn * u[x_val.length - 2]) / (qn * d2ydx2[x_val.length - 2] + 1.0);
    	for(int k = x_val.length - 2; k>=0; k--){
	    	d2ydx2[k] = d2ydx2[k] * d2ydx2[k+1] + u[k];
    	}
	}

   	/**
   	 * Returns if a given value is outside the range of x values allowed
   	 * for interpolation. In case extrapolation is allowed this range
   	 * is unlimited.
   	 * @param x The x value.
   	 * @return True if the input value is outside the range of x values
   	 * defined by the input array.
   	 */
   	public boolean isOutsideRange(double x) {
		if (x < min || x > max) return true;
		return false;
   	}

	/**
	 * Spline interpolation method. This method requires that the points are sorted in
	 * abscissa crescent order. Method adapted from the math library by Michael Thomas
	 * Flanagan.
	 *
	 * @param xx Interpolation point.
	 * @return The interpolated value. In case the input value is close enough to the
	 * limit of the dataset (possible wrong output value) the spline interpolation with
	 * 3rd order is called, since this method requires less points before/after the input
	 * one. In case that method cannot be trusted, linear interpolation is returned.
	 * @throws JPARSECException In case the input point is outside the valid range and
	 * extrapolation is not allowed.
	 */
	public double splineInterpolation(double xx) throws JPARSECException {
		if (!this.allowExtrapolation) {
			if (isOutsideRange(xx))
		    	throw new JPARSECException("Input value ("+xx+") is outside valid range ("+min+" to "+max+"), extrapolation required.");
		}

		boolean canBeWrong = false;
		int nlow = 0, nup = 0;
		for (int i=0; i<x_val.length; i++) {
			if (x_val[i] < xx) nlow ++;
			if (x_val[i] > xx) nup ++;
		}
		if (nlow < 8 || nup < 8) canBeWrong = true;


		if (d2ydx2 == null) calcDeriv();

        double h = 0.0, b = 0.0, a = 0.0, yy = 0.0;
    	int k = 0;
    	int klo = 0;
    	int khi = x_val.length - 1;
    	while (khi - klo > 1){
	    	k = (khi + klo) >> 1;
	    	if (x_val[k] > xx){
		    	khi = k;
	    	} else {
		    	klo = k;
	    	}
    	}
    	h = x_val[khi] - x_val[klo];

    	if (h == 0.0) throw new JPARSECException("Two values of x are identical: point "+klo+ " ("+x_val[klo]+") and point "+khi+ " ("+x_val[khi]+")" );
    	a = (x_val[khi] - xx) / h;
    	b = (xx - x_val[klo]) / h;
    	yy = a * y_val[klo] + b * y_val[khi] + ((a * a * a - a) * d2ydx2[klo] + (b * b * b - b) * d2ydx2[khi]) * (h * h) / 6.0;

    	if (canBeWrong) {
    		double l = this.linearInterpolation(xx);
    		double dif = Math.abs((yy - l) / l);
    		if (dif > 2) {
    			if (nlow >= 2 || nup >= 2) return this.splineInterpolation3rdOrder(xx, true); // Use 3rd order if possible
    			throw new JPARSECException("Cannot calculate spline interpolation so close to the range of x values.");
    		}
    	}
    	return yy;
	}

	/**
	 * Returns the number of x values in the dataset that are greater than the
	 * input value.
	 * @param xx Input x value.
	 * @return The number of x values above the input x. In case it is 2 or greater
	 * the spline interpolation with 3rd order of accuracy can be used. In case it is
	 * 8 or greater the other spline interpolation method can be used for points close
	 * to the upper limit of the dataset.
	 */
	public int getCountValuesAbove(double xx) {
		int nup = 0;
		for (int i=0; i<x_val.length; i++) {
			if (x_val[i] > xx) nup ++;
		}
		return nup;
	}
	/**
	 * Returns the number of x values in the dataset that are lower than the
	 * input value.
	 * @param xx Input x value.
	 * @return The number of x values below the input x. In case it is 2 or greater
	 * the spline interpolation with 3rd order of accuracy can be used. In case it is
	 * 8 or greater the other spline interpolation method can be used for points close
	 * to the lower limit of the dataset.
	 */
	public int getCountValuesBelow(double xx) {
		int nlow = 0;
		for (int i=0; i<x_val.length; i++) {
			if (x_val[i] < xx) nlow ++;
		}
		return nlow;
	}

	/**
	 * Spline interpolation method, up to third order of accuracy. This method
	 * requires that the points are sorted in abscisa crescent order.
	 * <P>
	 * Reference:
	 * <P>
	 * <I>Basic Scientific Subroutines</I>, F. R. Ruckdeschel. 1982.
	 *
	 * @param px Interpolation point. Must be between minimum and maximum value
	 *        of x array, or equal to one of them.
	 * @param fixWrongValue It is well known that for certain datasets spline
	 * interpolation can give wrong values for interpolation points close to the
	 * limit of the dataset. Set this flag to true to check for this and return
	 * the linear interpolation result in that case. Otherwise, an error will be
	 * launched.
	 * @return The interpolated value.
	 * @throws JPARSECException In case the output value is probably wrong and
	 * fixWrongValue is set to false.
	 */
	public double splineInterpolation3rdOrder(double px, boolean fixWrongValue) throws JPARSECException {
		double z[] = new double[x_val.length + 3];
		double mm[] = new double[x_val.length + 3];
		double cqc, a, B, py;
		int i, v;

		boolean canBeWrong = false;
		int nlow = 0, nup = 0;
		for (i=0; i<x_val.length; i++) {
			if (x_val[i] < px) nlow ++;
			if (x_val[i] > px) nup ++;
		}
		if (nlow < 2 || nup < 2) canBeWrong = true;

		v = x_val.length - 1;
		if (px == x_val[v])
			return y_val[v]; // Solve ArrayIndexOutOfBounds when px = max x.
		for (i = 0; i < v; i++)
		{
			cqc = x_val[i + 1] - x_val[i];
			mm[i + 2] = (y_val[i + 1] - y_val[i]) / cqc;
		}

		mm[v + 2] = 2.0 * mm[v + 1] - mm[v];
		mm[v + 3] = 2.0 * mm[v + 2] - mm[v + 1];
		mm[2] = 2.0 * mm[3] - mm[4];
		mm[1] = 2.0 * mm[2] - mm[3];

		for (i = 0; i < v; i++)
		{
			a = Math.abs(mm[i + 3] - mm[i + 2]);
			B = Math.abs(mm[i + 1] - mm[i]);
			if ((a + B) == 0.0)
			{
				z[i] = (mm[i + 2] + mm[i + 1]) / 2.0;
			} else
			{
				cqc = 1.0 + B;
				if (cqc == 0.0)
				{
					cqc = 1.0E-30;
				}
				z[i] = (a * mm[i + 1] + B * mm[i + 2]) / cqc;
			}
		}
		i = 1;

		while (px >= x_val[i] && i < v)
		{
			i = i + 1;
		}

		i = i - 1;
		B = x_val[i + 1] - x_val[i];
		a = px - x_val[i];
		py = y_val[i] + z[i] * a + (3.0 * mm[i + 2] - 2.0 * z[i] - z[i + 1]) * a * a / B;
		py = py + (z[i] + z[i + 1] - 2.0 * mm[i + 2]) * a * a * a / (B * B);

    	if (canBeWrong) {
    		double l = this.linearInterpolation(px);
    		double dif = Math.abs((py - l) / l);
    		if (dif > 2) {
    			if (fixWrongValue) return l;
    			throw new JPARSECException("Cannot calculate spline interpolation so close to the edge of the range of x values.");
    		}
    	}

		return py;
	}

	/**
	 * Linear interpolation method.
	 * @param x_val X values.
	 * @param y_val Y values.
	 * @param x_point Interpolation point.
	 * @param allowExtrapolation True to allow extrapolation.
	 * @return The interpolated value.
	 * @throws JPARSECException If allowExtrapolation is false and the point is
	 *         outside the range.
	 */
	public static double linearInterpolation(double x_val[], double y_val[], double x_point, boolean allowExtrapolation)
			throws JPARSECException
	{
		double x_prev, y_prev, x_next, y_next, slope;
		int v;

		// Get number of points
		v = x_val.length;

		// Obtain previous and next point
		x_prev = x_point;
		x_next = x_point;
		y_prev = 0.0;
		y_next = 0.0;

		int iprev = 0, inext = 0;
		for (int i = 0; i < v; i++)
		{
			if (x_val[i] == x_point) return y_val[i];

			if (x_val[i] < x_point && (x_val[i] > x_prev || x_prev == x_point))
			{
				x_prev = x_val[i];
				y_prev = y_val[i];
				iprev = i;
			}

			if (x_val[i] > x_point && (x_val[i] < x_next || x_next == x_point))
			{
				x_next = x_val[i];
				y_next = y_val[i];
				inext = i;
			}
		}

		// Correct values if no previous or next point exist, if extrapolation
		// is allowed
		if (allowExtrapolation)
		{
			if (x_prev == x_point)
			{
				x_prev = x_val[inext + 1];
				y_prev = y_val[inext + 1];
			}
			if (x_next == x_point)
			{
				x_next = x_val[iprev - 1];
				y_next = y_val[iprev - 1];
			}
		} else
		{
			if (x_prev == x_point || x_next == x_point)
			{
				throw new JPARSECException(
						"interpolation point out of range, extrapolation required.");
			}
		}

		// Interpolate
		slope = 0.0;
		if (!(x_next == x_prev))
			slope = (y_next - y_prev) / (x_next - x_prev);
		double y_point = y_prev + slope * (x_point - x_prev);

		return y_point;
	}

	/**
	 * Linear interpolation method, but applying natural logarithm to x and y
	 * values previous to calculations.
	 * <P>
	 *
	 * @param x_point Interpolation point.
	 * @return The interpolated value.
	 * @throws JPARSECException If allowExtrapolation is false and the point is
	 *         outside the range.
	 */
	public double linearInterpolationInLogScale(double x_point) throws JPARSECException
	{
		double x[] = new double[x_val.length];
		double y[] = new double[x_val.length];
		for (int i = 0; i < x_val.length; i++)
		{
			x[i] = Math.log(x_val[i]);
			y[i] = Math.log(y_val[i]);
		}
		double px = Math.log(x_point);
		double py = Math.exp(Interpolation.linearInterpolation(x, y, px, allowExtrapolation));

		return py;
	}

	/**
	 * Linear interpolation method in 3d. It is supposed that the x array contains
	 * several repeated points, each of them with a different value for the y
	 * coordinate, and also with some value for the z coordinates. Obviously, now
	 * the interpolation 'point' is the plain defined by an (x, z) point.
	 *
	 * @param x_point X interpolation point.
	 * @param z_point Z interpolation point.
	 * @return The interpolated value.
	 * @throws JPARSECException If an error occurs.
	 * @deprecated The method {@linkplain GridChartElement#getSurfaceFromPoints(jparsec.math.DoubleVector[], int)}
	 * should be used instead, since it allows to use an irregurarly-sampled grid of points
	 * and its implementation has much more physical sense.
	 */
	public double linearInterpolation3d(double x_point, double z_point)
			throws JPARSECException
	{
		// Reduce to simple case if we have no z values
		if (z_val == null) return this.linearInterpolation(x_point);

		double min = DataSet.getMinimumValue(x_val);
		double max = DataSet.getMaximumValue(x_val);

		if (x_point < min || x_point > max)
			throw new JPARSECException("the interpolation x point is outside the x domain.");

		// Obtain immediately previous and later x values from the x interpolation point
		double lowerX = 0.0, greaterX = 0.0;
		boolean lower = false, greater = false;
		for (int i=0; i<x_val.length; i++)
		{
			if (x_val[i] <= x_point && (x_val[i] > lowerX || !lower))
			{
				lower = true;
				lowerX = x_val[i];
			}
			if (x_val[i] >= x_point && (x_val[i] < greaterX || !greater))
			{
				greater = true;
				greaterX = x_val[i];
			}
		}

		// If both are equal, we reduce it again to the simple case (2d)
		if (greaterX == lowerX) {
			int index = 0;
			for (int i=0; i<x_val.length; i++)
			{
				if (x_val[i] == lowerX) index ++;
			}
			double newY[] = new double[index];
			double newZ[] = new double[index];
			index = -1;
			for (int i=0; i<x_val.length; i++)
			{
				if (x_val[i] == lowerX) {
					index ++;
					newY[index] = y_val[i];
					newZ[index] = z_val[i];
				}
			}
			return Interpolation.linearInterpolation(newZ, newY, z_point, allowExtrapolation);
		}

		// Obtain number of z (and y) values available for the immediately lower and later x values
		int nlow = 0, nup = 0;
		for (int i=0; i<x_val.length; i++)
		{
			if (x_val[i] == lowerX) nlow ++;
			if (x_val[i] == greaterX) nup ++;
		}

		// Obtain y and z values for those plains x = lowerX, x = greaterX
		double lowy[] = new double[nlow];
		double lowz[] = new double[nlow];
		double upy[] = new double[nup];
		double upz[] = new double[nup];
		nlow = -1;
		nup = -1;
		for (int i=0; i<x_val.length; i++)
		{
			if (x_val[i] == lowerX) {
				nlow ++;
				lowy[nlow] = y_val[i];
				lowz[nlow] = z_val[i];
			}
			if (x_val[i] == greaterX) {
				nup ++;
				upy[nup] = y_val[i];
				upz[nup] = z_val[i];
			}
		}

		if (nlow < 1 || nup < 1)
			throw new JPARSECException("the z domain axis contains no points.");

		// Obtain the maximum and minimum values of z in those datasets
		int greatest_z1 = (int) DataSet.getMaximumValue(lowz);
		int greatest_z2 = (int) DataSet.getMaximumValue(upz);
		int greatest_z = greatest_z1;
		if (greatest_z2 > greatest_z1) greatest_z = greatest_z2;

		int lowest_z1 = (int) DataSet.getMinimumValue(lowz);
		int lowest_z2 = (int) DataSet.getMinimumValue(upz);
		int lowest_z = lowest_z1;
		if (lowest_z2 > lowest_z1) lowest_z = lowest_z2;

		// Sample the z axis using enough points
		int np = 2 * (int) DataSet.getMaximumValue(new double[] {nlow, nup});

		// Reduce the z axis, using the same values for both datasets (they are
		// initially supposed to be different)
		double zz[] = new double[np];
		double yy[] = new double[np];
		double frac = (x_point - lowerX) / (greaterX - lowerX);
		for (int i=0; i<np; i++)
		{
			double z = lowest_z + (double) i * (greatest_z - lowest_z) / ((double) (np - 1));
			zz[i] = z;

			double yy1 = Interpolation.linearInterpolation(lowz, lowy, z, true);
			double yy2 = Interpolation.linearInterpolation(upz, upy, z, true);

			// Interpolate linearly using a weight defined by the distance of the
			// x interpolation point to both lowerX and greaterX
			yy[i] = yy1 + frac * (yy2 - yy1);
		}

		return Interpolation.linearInterpolation(zz, yy, z_point, allowExtrapolation);
	}

	/**
	 * Linear interpolation method in 3d in log scale. It is supposed that the x array contains
	 * several repeated points, each of them with a different value for the y
	 * coordinate, and also with some value for the z coordinates. Obviously, now
	 * the interpolation 'point' is the plain defined by an (x, z) point.
	 *
	 * @param x_point X interpolation point.
	 * @param z_point Z interpolation point.
	 * @return The interpolated value.
	 * @throws JPARSECException If an error occurs.
	 * @deprecated The method {@linkplain GridChartElement#getSurfaceFromPoints(jparsec.math.DoubleVector[], int)}
	 * should be used instead, since it allows to use an irregurarly-sampled grid of points
	 * and its implementation has much more physical sense.
	 */
	public double linearInterpolation3dInLogScale(double x_point, double z_point)
			throws JPARSECException
	{
		double bx[] = x_val.clone();
		double by[] = y_val.clone();
		double bz[] = z_val.clone();

		for (int i = 0; i < x_val.length; i++)
		{
			x_val[i] = Math.log(x_val[i]);
			y_val[i] = Math.log(y_val[i]);
			z_val[i] = Math.log(z_val[i]);
		}
		double px = Math.log(x_point);
		double pz = Math.log(z_point);
		double py = Math.exp(linearInterpolation3d(px, pz));

		x_val = bx;
		y_val = by;
		z_val = bz;
		return py;
	}

	/**
	 * Performs interpolation as implemented by Meeus, Astronomical Algorithms,
	 * chapter 3. Number of points must be 2, 3, or 5, and sampling (separation
	 * between consecutive x points) must be constant.
	 * @param x_point The point to interpolate. Should be as close as possible
	 * to the intermediate point.
	 * @return The result.
	 * @throws JPARSECException If the number of points is not 2, 3, neither 5.
	 */
	public double MeeusInterpolation(double x_point) throws JPARSECException {
		switch (x_val.length) {
		case 2:
			double a = (y_val[1] - y_val[0]) / (x_val[1] - x_val[0]);
			double b = y_val[0] - a * x_val[0];
			return a * x_point + b;
		case 3:
			a = y_val[1] - y_val[0];
			b = y_val[2] - y_val[1];
			double c = y_val[0] + y_val[2] - 2.0 * y_val[1];
			double n = (x_point - x_val[1]) / (x_val[2] - x_val[1]);
			return y_val[1] + n * 0.5 * (a + b + n * c);
		case 5:
			a = y_val[1] - y_val[0];
			b = y_val[2] - y_val[1];
			c = y_val[3] - y_val[2];
			double d = y_val[4] - y_val[3];
			double e = b - a, f = c - b, g = d - c;
			double h = f - e, j = g - f, k = j - h;
			n = (x_point - x_val[2]) / (x_val[2] - x_val[1]);

			return y_val[2] + n * ((b + c) * 0.5 - (h + j) / 12.0) + n * n * (f * 0.5 - k / 24.0) +
				n * n * n * (h + j) / 12.0 + n * n * n * n * k / 24.0;
		default:
			throw new JPARSECException("Number of points must be 2, 3, or 5.");
		}
	}

	/**
	 * Computes the (x, y) value of the extremum for a set of (x, y) pairs. The pairs
	 * should be selected so that the extreme is as close as possible to the intermediate
	 * point. See Astronomical Algorithms, chapter 3.
	 * @return The point of the extremum, or null if it is not found. The null case occurs
	 * when the number of points is 5 and the iteration process to get the extremum does
	 * not converge.
	 * @throws JPARSECException If the number of points is not 3 neither 5.
	 */
	public Point2D MeeusExtremum() throws JPARSECException {
		switch (x_val.length) {
		case 3:
			double a = y_val[1] - y_val[0];
			double b = y_val[2] - y_val[1];
			double c = y_val[0] + y_val[2] - 2.0 * y_val[1];

			double ym = y_val[1] - (a + b) * (a + b) / (8.0 * c);
			double xm = x_val[1] - (x_val[1] - x_val[0]) * (a + b) / (2.0 * c);
			return new Point2D.Double(xm, ym);
		case 5:
			a = y_val[1] - y_val[0];
			b = y_val[2] - y_val[1];
			c = y_val[3] - y_val[2];
			double d = y_val[4] - y_val[3];
			double e = b - a, f = c - b, g = d - c;
			double h = f - e, j = g - f, k = j - h;
			double n = 0.0, dn = 1.0;
			int niter = 0;
			do {
				double newN = (6.0 * b + 6.0 * c - h - j + 3.0 * n * n * (h + j) + 2.0 * n * n * n * k) / (k - 12.0 * f);
				dn = newN - n;
				n = newN;
				niter ++;
			} while (Math.abs(dn) > 1.0E-10 && niter < 100);
			if (Math.abs(dn) > 1.0E-10) return null;

			double x_point = n * (x_val[2] - x_val[1]) + x_val[2];
			double y_point = y_val[2] + n * ((b + c) * 0.5 - (h + j) / 12.0) + n * n * (f * 0.5 - k / 24.0) +
				n * n * n * (h + j) / 12.0 + n * n * n * n * k / 24.0;
			return new Point2D.Double(x_point, y_point);
		default:
			throw new JPARSECException("Number of points must be 3 or 5.");
		}
	}

	/**
	 * Computes the zero of a set of (x, y) pairs by interpolation, following
	 * Meeus (Astronomical Algorithms, chapter 3).
	 * @return The x value so that y = 0.
	 * @throws JPARSECException If the number of points is not 2, 3, neither 5,
	 * or if the number of points is 3 or 5 but iteration did not converge.
	 */
	public double MeeusZero() throws JPARSECException {
		switch (x_val.length) {
		case 2:
			double a = (y_val[1] - y_val[0]) / (x_val[1] - x_val[0]);
			double b = y_val[0] - a * x_val[0];
			return -b / a;
		case 3:
			a = y_val[1] - y_val[0];
			b = y_val[2] - y_val[1];
			double c = y_val[0] + y_val[2] - 2.0 * y_val[1];
			double n = 0.0, dn = 1.0;
			int iter = 0;
			do {
				n = -2.0 * y_val[1] / (a + b + c * n);
				dn = - (2.0 * y_val[1] + n * (a + b + c * n)) / (a + b + 2.0 * c * n);
				n += dn;
				iter ++;
			} while (Math.abs(dn) > 1.0E-10 && iter < 100);
			if (Math.abs(dn) > 1.0E-10) throw new JPARSECException("Iteration did not converge.");
			return n;
		case 5:
			a = y_val[1] - y_val[0];
			b = y_val[2] - y_val[1];
			c = y_val[3] - y_val[2];
			double d = y_val[4] - y_val[3];
			double e = b - a, f = c - b, g = d - c;
			double h = f - e, j = g - f, k = j - h;
			n = 0.0;
			dn = 1.0;
			iter = 0;
			do {
				double newN = (-24.0 * y_val[2] + n * n * (k - 12.0 * f) - 2.0 * n * n * n * (h + j) - n * n * n * n * k) / (2.0 * (6.0 * b + 6.0 * c - h - j));
				double M = k / 24.0, N = (h + j) / 12.0, P = f * 0.5 - M, Q = (b + c) * 0.5 - N;
				dn = - (M * newN * newN * newN * newN + N * newN * newN * newN + P * newN * newN + Q * newN + y_val[2]) / (4 * M * newN * newN * newN + 2.0 * N * newN * newN + 2.0 * P * newN + Q);
				n = newN + dn;
				iter ++;
			} while (Math.abs(dn) > 1.0E-10 && iter < 100);
			if (Math.abs(dn) > 1.0E-10) throw new JPARSECException("Iteration did not converge.");

			double x_point = n * (x_val[2] - x_val[1]) + x_val[2];
			return x_point;
		default:
			throw new JPARSECException("Number of points must be 2, 3, or 5.");
		}
	}

	/**
	 * Performs Lagrange interpolation as given by Meeus in Astronomical
	 * Algorithms, chapter 3.
	 * @param x The interpolation point, should be within the range from minimum to
	 * maximum input x values for a correct output.
	 * @return The value. No check is done, so use with care, since for certain datasets
	 * and close to the range limit of x values the output can be wrong.
	 * @throws JPARSECException If the input point is outside range and extrapolation
	 * is not allowed.
	 */
	public double LagrangeInterpolation(double x) throws JPARSECException {
		if (!this.allowExtrapolation) {
			if (x < min || x > max)
		    	throw new JPARSECException("Input value ("+x+") is outside valid range ("+min+" to "+max+"), extrapolation required.");
		}

		double v = 0.0;
		for (int i=0; i<x_val.length; i++) {
			double c = 1.0;
			for (int j=0; j<x_val.length; j++) {
				if (j != i) c *= (x - x_val[j]) / (x_val[i] - x_val[j]);
			}
			v += c * y_val[i];
		}
		return v;
	}

	/**
	 * Returns a string with the list of points.
	 */
	@Override
	public String toString() {
		StringBuffer s = new StringBuffer("");
		String lsep = FileIO.getLineSeparator();
		int l = 22;
		for (int i=0; i<x_val.length; i++) {
			String x = FileIO.addSpacesAfterAString(""+x_val[i], l);
			String y = FileIO.addSpacesAfterAString(""+y_val[i], l);
			s.append(x+y);
			if (z_val != null) {
				String z = FileIO.addSpacesAfterAString(""+z_val[i], l);
				s.append(z);
			}
			s.append(lsep);
		}
		return s.toString();
	}

	/**
	 * Returns a chart showing the input data and the interpolated data.
	 * @param useSpline True to use spline for interpolation, false for linear.
	 * @param overSampling The number of points in the interpolated data respect the original
	 * number of points. 1 for the same number, 2 for twice, ...
	 * @return The chart.
	 * @throws JPARSECException If an error occurs.
	 */
	public CreateChart getChart(boolean useSpline, int overSampling) throws JPARSECException {
		ChartSeriesElement chartSeries1 = new ChartSeriesElement(x_val,
				y_val, null, null, "Y", true, Color.BLACK, ChartSeriesElement.SHAPE_CIRCLE,
				ChartSeriesElement.REGRESSION.SPLINE_INTERPOLATION);
		String label = Translate.translate(214);
		if (useSpline) label = Translate.translate(213);
		double dy[] = new double[y_val.length*overSampling], dx[] = new double[dy.length];
		double min = DataSet.getMinimumValue(x_val), max = DataSet.getMaximumValue(x_val);
		for (int i=0; i<dy.length; i++) {
			dx[i] = min + (max - min) * i / (dy.length - 1.0);
			if (useSpline) {
				dy[i] = this.splineInterpolation(dx[i]);
			} else {
				dy[i] = this.linearInterpolation(dx[i]);
			}
		}
		ChartSeriesElement chartSeries2 = new ChartSeriesElement(dx,
				dy, null, null, label, true, Color.RED, ChartSeriesElement.SHAPE_CIRCLE,
				ChartSeriesElement.REGRESSION.SPLINE_INTERPOLATION);

		ChartSeriesElement series[] = new ChartSeriesElement[] {chartSeries1, chartSeries2};
		ChartElement chart = new ChartElement(series, ChartElement.TYPE.XY_CHART,
				ChartElement.SUBTYPE.XY_SCATTER,
				"X, Y, "+label,
				"X", "Y, "+label, false, 800, 600);
		CreateChart ch = new CreateChart(chart);
		return ch;
	}
}
