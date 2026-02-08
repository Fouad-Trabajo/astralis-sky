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

import org.jfree.chart.plot.Marker;

import jparsec.graph.ChartElement;
import jparsec.graph.ChartSeriesElement;
import jparsec.graph.CreateChart;
import jparsec.graph.DataSet;
import jparsec.util.JPARSECException;

/**
 * Provides method to perform numerical integration.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Integration implements Serializable
{
	private static final long serialVersionUID = 1L;

	private double x[], y[], x1, x2;
	private Interpolation interp;
	/**
	 * Default constructor for a given set of x, y values and integration range.
	 * Points are sorted in crescent order and repeated points eliminated.
	 * @param x X values. Should be non null.
	 * @param y Y values. Should be non null.
	 * @param x1 Lower limit of the integration range.
	 * @param x2 Upper limit of the integration range.
	 * @throws JPARSECException If an error occurs.
	 */
	public Integration(double x[], double y[], double x1, double x2) throws JPARSECException {
		if (x != null && y != null) {
			ArrayList<double[]> l = DataSet.sortInCrescent(x, y, true);
			this.x = l.get(0);
			this.y = l.get(1);
		}
		this.x1 = x1;
		this.x2 = x2;
		interp = new Interpolation(x, y, false);
	}

	/**
	 * Simple low accuracy numerical integration method, based on the midpoint
	 * rule.
	 * @param step Step in the bisection between two consecutive points in x.
	 * Recommended values are around (x2-x1)/1.0E5.
	 * @return Integral.
	 * @throws JPARSECException If something goes wrong.
	 */
	public double simpleIntegration(double step)
			throws JPARSECException
	{
		int v = x.length;
		if (x1 < x[0] || x2 > x[v - 1])
			throw new JPARSECException(
					"integration interval seems to be out of range, or perhaps points are not sorted in crescent order.");

		double integral = 0.0;
		double lastx = 0.0;
		for (double px = x1; px < (x2 - step * 0.5); px = px + step)
		{
			lastx = px + step * 0.5;
			double p1 = interp.linearInterpolation(px + step * 0.5);
			integral += p1 * step;
		}
		double dif = x2 - lastx;
		if (dif != 0.0)
		{
			double p1 = interp.linearInterpolation(lastx + dif * 0.5);
			integral += p1 * dif;
		}

		return integral;
	}

	/**
	 * Simple low accuracy numerical integration method, based on the midpoint
	 * rule, using spline interpolation. This is usually a little bit better
	 * when having few points.
	 * @param step Step in the bisection between two consecutive points in x.
	 * Recommended values are around (x2-x1)/1.0E5.
	 * @return Integral.
	 * @throws JPARSECException If something goes wrong.
	 */
	public double simpleIntegrationUsingSpline(double step)
			throws JPARSECException
	{

		int v = x.length;
		if (x1 < x[0] || x2 > x[v - 1])
			throw new JPARSECException(
					"integration interval seems to be out of range, or perhaps points are not sorted in crescent order.");

		double integral = 0.0;
		double lastx = 0.0;
		for (double px = x1; px < (x2 - step * 0.5); px = px + step)
		{
			lastx = px + step * 0.5;
			double p1 = interp.splineInterpolation(px + step * 0.5);
			integral += p1 * step;
		}
		double dif = x2 - lastx;
		if (dif != 0.0)
		{
			double p1 = interp.splineInterpolation(lastx + dif * 0.5);
			integral += p1 * dif;
		}

		return integral;
	}

	/**
	 * Simple low accuracy numerical integration method, based on the midpoint
	 * rule, for x values already sorted in crescent order. This is faster than
	 * using an instance of this class, since the constructor checks the ordering.
	 * The interpolation used is linear.
	 * @param x X values, sorted in crescent order.
	 * @param y Corresponding Y values.
	 * @param x1 Lower limit of the integration range.
	 * @param x2 Upper limit of the integration range.
	 * @param step Step in the bisection between two consecutive points in x.
	 * Recommended values are around (x2-x1)/1.0E5.
	 * @return Integral.
	 * @throws JPARSECException If something goes wrong.
	 */
	public static double simpleIntegrationForSortedX(double x[], double y[], double x1, double x2, double step)
			throws JPARSECException
	{
		Interpolation interp = new Interpolation(x, y, false);
		int v = x.length;
		if (x1 < x[0] || x2 > x[v - 1])
			throw new JPARSECException(
					"integration interval seems to be out of range, or perhaps points are not sorted in crescent order.");

		double integral = 0.0;
		double lastx = 0.0;
		for (double px = x1; px < (x2 - step * 0.5); px = px + step)
		{
			lastx = px + step * 0.5;
			double p1 = interp.linearInterpolation(px + step * 0.5);
			integral += p1 * step;
		}
		double dif = x2 - lastx;
		if (dif != 0.0)
		{
			double p1 = interp.linearInterpolation(lastx + dif * 0.5);
			integral += p1 * dif;
		}

		return integral;
	}

	/**
	 * Returns a chart showing the input data and the integrated region.
	 * @return The chart.
	 * @throws JPARSECException If an error occurs.
	 */
	public CreateChart getChart() throws JPARSECException {
		ChartSeriesElement chartSeries1 = new ChartSeriesElement(x,
				y, null, null, "Y", true, Color.BLACK, ChartSeriesElement.SHAPE_CIRCLE,
				ChartSeriesElement.REGRESSION.SPLINE_INTERPOLATION);
		ChartSeriesElement series[] = new ChartSeriesElement[] {chartSeries1};
		ChartElement chart = new ChartElement(series, ChartElement.TYPE.XY_CHART,
				ChartElement.SUBTYPE.XY_SCATTER,
				"X, Y",
				"X", "Y", false, 800, 600);
		CreateChart ch = new CreateChart(chart);
		Marker marker = new org.jfree.chart.plot.IntervalMarker(x1, x2);
		marker.setPaint(new Color(255, 0, 0, 128));
		((org.jfree.chart.plot.XYPlot) ch.getChart().getPlot()).addDomainMarker(marker);
		return ch;
	}
}
