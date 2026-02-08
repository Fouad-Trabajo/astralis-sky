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

import jparsec.graph.ChartElement;
import jparsec.graph.ChartSeriesElement;
import jparsec.graph.CreateChart;
import jparsec.graph.DataSet;
import jparsec.util.JPARSECException;

/**
 * A class for fitting data to an arbitrary function of 3 independent variables.
 * Implementation based on Jean Meeus's Astronomical Algorithms.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class GenericFit implements Serializable
{
	private static final long serialVersionUID = 1L;

	private double x[], y[];
	private String f, f0, f1, f2;
	private double a = 0, b = 0, c = 0;

	/**
	 * Default constructor.
	 * @param x X values.
	 * @param y Y values.
	 * @param f0 The first function f0(x) from 'y = a f0(x) + b f1(x) + c f2(x)',
	 * in Java notation.
	 * @param f1 The second function f1(x) from 'y = a f0(x) + b f1(x) + c f2(x)',
	 * in Java notation.
	 * @param f2 The third function f2(x) from 'y = a f0(x) + b f1(x) + c f2(x)',
	 * in Java notation. You can use '1' or 'x' for a linear fit or if you don't need it,
	 * but must be different from f1 and f0 and cannot be '0' or empty. The only exception
	 * (for f2 to be 0 or empty) is to set f1 to '1' or 'x', in a linear fit.
	 */
	public GenericFit(double x[], double y[], String f0, String f1, String f2)	{
		if (x != null) this.x = x.clone();
		if (y != null) this.y = y.clone();
		this.f0 = f0;
		this.f1 = f1;
		this.f2 = f2;
		f = "a*("+f0+")+b*("+f1+")+c*("+f2+")";
	}

	/**
	 * Default constructor for only one function.
	 * @param x X values.
	 * @param y Y values.
	 * @param f0 The function f0(x) from 'y = a f0(x)',
	 * in Java notation.
	 */
	public GenericFit(double x[], double y[], String f0)	{
		if (x != null) this.x = x.clone();
		if (y != null) this.y = y.clone();
		this.f0 = f0;
		this.f1 = null;
		this.f2 = null;
		f = "a*("+f1+")";
	}


	/**
	 * Evaluate fitting function in some point. A fit should previously be done.
	 *
	 * @param x Point to evaluate.
	 * @return Result of evaluation. 0.0 if fit is invalid.
	 * @throws JPARSECException If an error occurs.
	 */
	public double evaluateFittingFunction(double x) throws JPARSECException
	{
		if (f1 == null && f2 == null) {
			Evaluation eval = new Evaluation(f, new String[] {"x "+x, "a "+a});
			return eval.evaluate();
		} else {
			Evaluation eval = new Evaluation(f, new String[] {"x "+x, "a "+a, "b "+b, "c "+c});
			return eval.evaluate();
		}
	}

	/**
	 * Returns the adequate object to evaluate the fitted function.
	 * A fit should previously be done.
	 *
	 * @return The object with the function and fitting variables.
	 * @throws JPARSECException If an error occurs.
	 */
	public Evaluation getEvaluateObject() throws JPARSECException
	{
		if (f1 == null && f2 == null) {
			return new Evaluation(f, new String[] {"a "+a});
		} else {
			return new Evaluation(f, new String[] {"a "+a, "b "+b, "c "+c});
		}
	}

	/**
	 * Returns the fitting function as a series to draw it in a chart.
	 * @param npoints Number of points in x axis for the series.
	 * @param xlogScale True to sample x axis in log scale.
	 * @return The series, with black color and epmty shape. The legend
	 * is set to the expression of the function.
	 * @throws JPARSECException If an error occurs.
	 */
	public ChartSeriesElement getFittingFunctionAsSeries(int npoints, boolean xlogScale) throws JPARSECException {
		double xmin = DataSet.getMinimumValue(x), xmax = DataSet.getMaximumValue(x);
		double vx[] = DataSet.getSetOfValues(xmin, xmax, npoints, xlogScale);

		double vy[] = new double[vx.length];
		for (int i=0; i<vx.length; i++) {
			vy[i] = this.evaluateFittingFunction(vx[i]);
		}
		ChartSeriesElement chartSeries = new ChartSeriesElement(
				vx,	vy, null, null,
				f, true, Color.BLACK, ChartSeriesElement.SHAPE_EMPTY,
				ChartSeriesElement.REGRESSION.NONE);
		return chartSeries;
	}

	/**
	 * Does the fit.
	 * @return The set of variables a, b, c. If some of them are not
	 * used, its value will be zero.
	 * @throws JPARSECException If the number of points is lower than 3.
	 */
	public double[] fit() throws JPARSECException {
		if (x.length < 3 || x.length != y.length)
			throw new JPARSECException("Invalid input, at least three x and y values and same number of them.");

		double M = 0.0, P = 0.0, Q = 0.0, R = 0.0, S = 0.0, T = 0.0, U = 0.0, V = 0.0, W = 0.0;
		if (f1 == null && f2 == null) {
			for (int i=0; i<x.length; i++) {
				Evaluation eval1 = new Evaluation(f0, new String[] {"x "+x[i]});
				Evaluation eval2 = new Evaluation("("+f0+")*("+f0+")", new String[] {"x "+x[i]});
				M += y[i] * eval1.evaluate();
				R += eval2.evaluate();
			}
			a = M / R;
			return new double[] {a};
		} else {
			// Special case for linear fitting, separated since f2 is inexistent and this cause and error in the generic algorithm for 3 functions
			if (f0.equals("x") && f1.equals("1") && (f2 == null || f2.equals("0") || f2.equals(""))) {
				int n = x.length;
				double sumxy = 0.0, sumx = 0.0, sumy = 0, sumx2 = 0;
				for (int i=0; i<x.length; i++) {
					sumx += x[i];
					sumxy += x[i] * y[i];
					sumy += y[i];
					sumx2 += x[i] * x[i];
				}
				a = (n * sumxy - sumx * sumy) / (n * sumx2 - sumx * sumx);
				b = (sumy * sumx2 - sumx * sumxy) / (n * sumx2 - sumx * sumx);
				c = 0;
			} else {
				for (int i=0; i<x.length; i++) {
					Evaluation eval1 = new Evaluation("("+f0+")*("+f0+")", new String[] {"x "+x[i]});
					Evaluation eval2 = new Evaluation("("+f1+")*("+f1+")", new String[] {"x "+x[i]});
					Evaluation eval3 = new Evaluation("("+f2+")*("+f2+")", new String[] {"x "+x[i]});
					Evaluation eval4 = new Evaluation("("+f0+")*("+f1+")", new String[] {"x "+x[i]});
					Evaluation eval5 = new Evaluation("("+f0+")*("+f2+")", new String[] {"x "+x[i]});
					Evaluation eval6 = new Evaluation("("+f1+")*("+f2+")", new String[] {"x "+x[i]});
					Evaluation eval7 = new Evaluation(f0, new String[] {"x "+x[i]});
					Evaluation eval8 = new Evaluation(f1, new String[] {"x "+x[i]});
					Evaluation eval9 = new Evaluation(f2, new String[] {"x "+x[i]});

					M += eval1.evaluate();
					R += eval2.evaluate();
					T += eval3.evaluate();

					P += eval4.evaluate();
					Q += eval5.evaluate();
					S += eval6.evaluate();

					U += y[i] * eval7.evaluate();
					V += y[i] * eval8.evaluate();
					W += y[i] * eval9.evaluate();
				}
				double D = M * R * T + 2.0 * P * Q * S - M * S * S - R * Q * Q - T * P * P;
				a = (U * (R * T - S * S) + V * (Q * S - P * T) + W * (P * S - Q * R)) / D;
				b = (U * (S * Q - P * T) + V * (M * T - Q * Q) + W * (P * Q - M * S)) / D;
				c = (U * (P * S - R * Q) + V * (P * Q - M * S) + W * (M * R - P * P)) / D;
			}
		}
		return new double[] {a, b, c};
	}

	/**
	 * Returns the complete function.
	 * @return The function.
	 */
	public String getFunction() {
		String out = f;
		if (a != 0.0 || b != 0.0 || c != 0.0) {
			out = DataSet.replaceAll(out, "a*", ""+a+"*", true);
			out = DataSet.replaceAll(out, "b*", ""+b+"*", true);
			out = DataSet.replaceAll(out, "c*", ""+c+"*", true);
		}
		return out;
	}

	/**
	 * Returns a chart showing the input data and the fitted data.
	 * @param overSampling The number of points in the derived data respect the original
	 * number of points. 1 for the same number, 2 for twice, ...
	 * @return The chart.
	 * @throws JPARSECException If an error occurs.
	 */
	public CreateChart getChart(int overSampling) throws JPARSECException {
		ChartSeriesElement chartSeries1 = new ChartSeriesElement(x,
				y, null, null, "Y", true, Color.BLACK, ChartSeriesElement.SHAPE_CIRCLE,
				ChartSeriesElement.REGRESSION.SPLINE_INTERPOLATION);
		double dy[] = new double[y.length*overSampling], dx[] = new double[dy.length];
		double min = DataSet.getMinimumValue(x), max = DataSet.getMaximumValue(x);
		for (int i=0; i<dy.length; i++) {
			dx[i] = min + (max - min) * i / (dy.length - 1.0);
			dy[i] = this.evaluateFittingFunction(dx[i]);
		}
		ChartSeriesElement chartSeries2 = new ChartSeriesElement(dx,
				dy, null, null, "f(x)", true, Color.RED, ChartSeriesElement.SHAPE_CIRCLE,
				ChartSeriesElement.REGRESSION.SPLINE_INTERPOLATION);

		ChartSeriesElement series[] = new ChartSeriesElement[] {chartSeries1, chartSeries2};
		ChartElement chart = new ChartElement(series, ChartElement.TYPE.XY_CHART,
				ChartElement.SUBTYPE.XY_SCATTER,
				"X, Y, f(x)",
				"X", "Y, f(x)", false, 800, 600);
		CreateChart ch = new CreateChart(chart);
		return ch;
	}
}
