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
import java.io.Serializable;
import java.util.ArrayList;

import jparsec.graph.ChartElement;
import jparsec.graph.ChartSeriesElement;
import jparsec.graph.CreateChart;
import jparsec.graph.DataSet;
import jparsec.util.JPARSECException;

/**
 * Performs numerical derivation.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Derivation implements Serializable
{
	private static final long serialVersionUID = 1L;

	private double x[], y[];

	/**
	 * Constructor for a derivation. Points are sorted
	 * in x crescent order, and repeated points are eliminated.
	 * @param x X values.
	 * @param y Y values.
	 */
	public Derivation(double x[], double y[]) {
		if (x != null && y != null) {
			ArrayList<double[]> l = DataSet.sortInCrescent(x, y, true);
			this.x = l.get(0);
			this.y = l.get(1);
		}
	}

	private int geti(double px)
	{
		int i = -1;
		do
		{
			i = i + 1;
		} while (x[i] < px);

		return i;
	}

	/**
	 * Computes derivative by Lagrange's method.
	 * <P>
	 * From Basic Scientific Subroutines, F. R. Ruckdeschel.
	 *
	 * @param px Point to compute derivative. Must be between the first and the
	 *        (last-n) point.
	 * @param n Degree of the polynomial.
	 * @return Derivative.
	 * @throws JPARSECException If the point is not acceptable.
	 */
	public double Lagrange(double px, int n) throws JPARSECException
	{
		int v = x.length;
		if (px <= x[0] || px > x[v - n])
			throw new JPARSECException(
					"interpolation degree is " + n + ", so derivation point must be between the first point and n-" + n + ", being n the last one.");
		int i = 0;

		i = geti(px);
		i = i - 1;
		double ll[] = new double[n + 1];
		double p[][] = new double[n + 1][n + 1];
		for (int j = 0; j <= n; j++)
		{
			ll[j] = 0;
			for (int k = 0; k <= n; k++)
			{
				p[j][k] = 1;
			}
		}
		double yy = 0;
		for (int k = 0; k <= n; k++)
		{
			for (int j = 0; j <= n; j++)
			{
				if (j != k)
				{
					for (int l = 0; l <= n; l++)
					{
						if (l != k)
						{
							if (l != j)
							{
								p[l][k] *= (px - x[j + i]) / (x[i + k] - x[i + j]);
							} else
							{
								p[l][k] /= (x[i + k] - x[i + j]);
							}
						}
					}
				}
			}

			for (int l = 0; l <= n; l++)
			{
				if (l != k)
					ll[k] += p[l][k];
			}
			yy = yy + ll[k] * y[i + k];
		}
		return yy;
	}

	private double[] d2ydx2 = null;
   	private void calcDeriv(){
    	double p = 0.0, qn = 0.0, sig = 0.0, un = 0.0;
    	double[] u = new double[x.length];
    	d2ydx2 = new double[x.length];

        d2ydx2[0] = u[0] = 0.0;
    	for (int i=1; i<=x.length-2; i++){
	    	sig = (x[i] - x[i-1]) / (x[i+1] - x[i-1]);
	    	p = sig * d2ydx2[i-1] + 2.0;
	    	d2ydx2[i] = (sig - 1.0) / p;
	    	u[i] = (y[i+1] - y[i]) / (x[i+1] - x[i]) - (y[i] - y[i-1]) / (x[i] - x[i-1]);
	    	u[i] = (6.0 * u[i] / (x[i+1] - x[i-1]) - sig * u[i-1]) / p;
    	}

	    qn = un = 0.0;
    	d2ydx2[x.length-1] = (un - qn * u[x.length - 2]) / (qn * d2ydx2[x.length - 2] + 1.0);
    	for(int k = x.length - 2; k>=0; k--){
	    	d2ydx2[k] = d2ydx2[k] * d2ydx2[k+1] + u[k];
    	}
	}

	/**
	 * Spline derivative method, up to third order of accuracy. This method
	 * requires that the points are sorted in abscissa crescent order. Method adapted
	 * from the math library by Michael Thomas Flanagan.
	 *
	 * @param xx Interpolation point. Must be between minimum and maximum value
	 *        of x array, or equal to one of them (extrapolation cannot be done).
	 * @return The interpolated value.
	 * @throws JPARSECException In case the input point is outside the valid range and
	 * extrapolation is not allowed.
	 */
	public double splineDerivative(double xx) throws JPARSECException {
		if (xx < x[0] || xx > x[x.length - 1])
			throw new JPARSECException("Input x is outside range.");
		if (d2ydx2 == null) calcDeriv();

        double h = 0.0, b = 0.0, a = 0.0; //, yy = 0.0;
    	int k = 0;
    	int klo = 0;
    	int khi = x.length - 1;
    	while (khi - klo > 1){
	    	k = (khi + klo) >> 1;
	    	if (x[k] > xx){
		    	khi = k;
	    	} else {
		    	klo = k;
	    	}
    	}
    	h = x[khi] - x[klo];

    	if (h == 0.0) throw new JPARSECException("Two values of x are identical: point "+klo+ " ("+x[klo]+") and point "+khi+ " ("+x[khi]+")" );
    	a = (x[khi] - xx) / h;
    	b = (xx - x[klo]) / h;
    	//yy = a * y[klo] + b * y[khi] + ((a * a * a - a) * d2ydx2[klo] + (b * b * b - b) * d2ydx2[khi]) * (h * h) / 6.0;
    	double dydx = (y[khi] - y[klo]) / h - ((3 * a * a - 1.0) * d2ydx2[klo] - (3 * b * b - 1.0) * d2ydx2[khi]) * h / 6.0;
    	return dydx;
	}

	/**
	 * Returns a chart showing the input data and the derived data.
	 * @param useSpline True to use spline for derivation, false for Lagrange of order 3.
	 * @param overSampling The number of points in the derived data respect the original
	 * number of points. 1 for the same number, 2 for twice, ...
	 * @return The chart.
	 * @throws JPARSECException If an error occurs.
	 */
	public CreateChart getChart(boolean useSpline, int overSampling) throws JPARSECException {
		ChartSeriesElement chartSeries1 = new ChartSeriesElement(x,
				y, null, null, "Y", true, Color.BLACK, ChartSeriesElement.SHAPE_CIRCLE,
				ChartSeriesElement.REGRESSION.SPLINE_INTERPOLATION);
		double dy[] = new double[y.length*overSampling], dx[] = new double[dy.length];
		double min = DataSet.getMinimumValue(x), max = DataSet.getMaximumValue(x);
		for (int i=0; i<dy.length; i++) {
			dx[i] = min + (max - min) * i / (dy.length - 1.0);
			if (useSpline) {
				dy[i] = this.splineDerivative(dx[i]);
			} else {
				try { dy[i] = this.Lagrange(dx[i], 3); } catch (Exception exc) { dy[i] = 0.0; }
			}
		}
		ChartSeriesElement chartSeries2 = new ChartSeriesElement(dx,
				dy, null, null, "dY", true, Color.RED, ChartSeriesElement.SHAPE_CIRCLE,
				ChartSeriesElement.REGRESSION.SPLINE_INTERPOLATION);

		ChartSeriesElement series[] = new ChartSeriesElement[] {chartSeries1, chartSeries2};
		ChartElement chart = new ChartElement(series, ChartElement.TYPE.XY_CHART,
				ChartElement.SUBTYPE.XY_SCATTER,
				"X, Y, dY",
				"X", "Y, dY", false, 800, 600);
		CreateChart ch = new CreateChart(chart);
		return ch;
	}
}
