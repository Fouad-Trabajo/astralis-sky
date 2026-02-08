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

//import javax.vecmath.Point3d;
import java.awt.Color;
import java.io.Serializable;
import java.util.Arrays;

import jparsec.io.Serialization;
import jparsec.io.image.ImageSplineTransform;
import jparsec.math.DoubleVector;
import jparsec.util.JPARSECException;

/**
 * Creates a series for later use in JMathPlot.<P>
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class ChartSeriesElement3D implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Default empty constructor.
	 */
	public ChartSeriesElement3D() { }

	/**
	 * Simple constructor for a set of points.
	 *
	 * @param x X values.
	 * @param y Y Values.
	 * @param z Z Values.
	 * @param legend Legend.
	 */
	public ChartSeriesElement3D(double x[], double y[], double z[], String legend)
	{
		this.xValues = DataSet.toStringValues(x);
		this.yValues = DataSet.toStringValues(y);
		this.zValues = z;
		this.legend = legend;
	}

	/**
	 * Simple constructor for a surface.
	 *
	 * @param x X values.
	 * @param y Y Values.
	 * @param z Z Values.
	 * @param legend Legend.
	 */
	public ChartSeriesElement3D(double x[], double y[], double z[][], String legend)
	{
		this.xValues = DataSet.toStringValues(x);
		this.yValues = DataSet.toStringValues(y);
		this.zValues = z;
		this.legend = legend;
		this.isSurface = true;
	}

	/**
	 * Simple constructor for a set of points with limits.
	 *
	 * @param x X values.
	 * @param y Y Values.
	 * @param z Z Values.
	 * @param legend Legend.
	 */
	public ChartSeriesElement3D(String x[], String y[], String z[], String legend)
	{
		this.xValues = x;
		this.yValues = y;
		this.zValues = z;
		this.legend = legend;
	}

	/**
	 * Constructor adequate for an xyz chart with bar errors.
	 *
	 * @param x X values.
	 * @param y Y Values.
	 * @param z Z Values.
	 * @param dx X error values.
	 * @param dy Y error values.
	 * @param dz Z error values.
	 * @param legend Legend.
	 * @param color Color.
	 */
	public ChartSeriesElement3D(double x[], double y[], double z[], double dx[], double dy[], double dz[], String legend,
			Color color)
	{
		this.xValues = DataSet.toStringValues(x);
		this.yValues = DataSet.toStringValues(y);
		this.zValues = z;
		this.dxValues = dx;
		this.dyValues = dy;
		this.dzValues = dz;
		this.legend = legend;
		this.color = color;
	}

	/**
	 * Transforms a grid chart into a 3d series.
	 * @param chart The grid chart object.
	 */
	public ChartSeriesElement3D(GridChartElement chart)
	{
        double sx = (chart.limits[1] - chart.limits[0]) / ((double) chart.data.length - 1.0);
        double sy = (chart.limits[3] - chart.limits[2]) / ((double) chart.data[0].length - 1.0);
        double x[] = new double[chart.data.length];
        double y[] = new double[chart.data[0].length];
        double z[][] = new double[chart.data.length][chart.data[0].length];

        for (int i=0; i<x.length; i++)
        {
        	x[i] = chart.limits[0] + sx * (double) i;
        }
        for (int i=0; i<y.length; i++)
        {
        	y[i] = chart.limits[2] + sy * (double) i;
        }
        for (int i=0; i<x.length; i++)
        {
            for (int j=0; j<y.length; j++)
            {
            	z[i][j] = chart.data[i][j];
            }
        }

		this.xValues = DataSet.toStringValues(x);
		this.yValues = DataSet.toStringValues(y);
		this.zValues = z;
		this.legend = chart.legend;
		this.color = Color.BLUE;
		this.isSurface = true;
	}

	/**
	 * Returns the intensity at certain position using 2d spline interpolation.
	 * @param x X position in physical units.
	 * @param y Y position in physical units.
	 * @return The intensity.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getIntensityAt(double x, double y)
	throws JPARSECException
	{
		GridChartElement gridChart;
		try {
			gridChart = new GridChartElement("", "", "", "",
				GridChartElement.COLOR_MODEL.BLACK_TO_WHITE,
				GridChartElement.getLimitsFromDataSet(
						DataSet.getDoubleValuesExcludingLimits(xValues),
						DataSet.getDoubleValuesExcludingLimits(yValues)),
						(double[][]) zValues,
				null, 600
				);
		} catch (Exception exc) {
			throw new JPARSECException("error while parsing the series to a surface. Is indeed a surface?", exc);
		}

		ImageSplineTransform t = new ImageSplineTransform(GridChartElement.ObjectToDoubleArray(gridChart.data));
		int pointsX = gridChart.data.length;
		int pointsY = gridChart.data[0].length;
		double px = (x - gridChart.limits[0]) * (pointsX - 1.0) / (gridChart.limits[1] - gridChart.limits[0]);
		double py = (y - gridChart.limits[2]) * (pointsY - 1.0) / (gridChart.limits[3] - gridChart.limits[2]);
		double data = t.interpolate(px, py);
		return data;
	}

	/**
	 * Array of x values.
	 */
	public String[] xValues;

	/**
	 * Array of y values.
	 */
	public String[] yValues;

	/**
	 * Array of z values.
	 */
	public Object zValues;

	/**
	 * Array of dx values.
	 */
	public double[] dxValues;

	/**
	 * Array of dy values.
	 */
	public double[] dyValues;

	/**
	 * Array of dz values.
	 */
	public double[] dzValues;

	/**
	 * Legend.
	 */
	public String legend;

	/**
	 * Sets the text to pointers in an x-y chart. Format is point number (in the
	 * order defined by the x array, being 1 the first point) plus the text to
	 * write. Pointers and labels are set automatically.
	 */
	public String pointers[] = new String[0];

	/**
	 * Selects the color of the series.
	 */
	public Color color;

	/**
	 * True (default) to show error bars.
	 */
	public boolean showErrorBars = true;

	/**
	 * True (default false) to treat the data as a surface.
	 */
	public boolean isSurface = false;

	/**
	 * True (default false) to draw segments instead of the points,
	 * only when the data is not a surface.
	 */
	public boolean drawLines = false;

	/**
	 * True (default false) to draw segments from each point to the
	 * base of the chart, only when the data is not a surface and
	 * {@linkplain ChartSeriesElement3D#drawLines} is false.
	 */
	public boolean isBarPlot = false;

	/**
	 * To clone the object.
	 */
	@Override
	public ChartSeriesElement3D clone()
	{
		ChartSeriesElement3D c = new ChartSeriesElement3D();
		c.color = new Color(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), this.color.getAlpha());
		if (dxValues != null) c.dxValues = this.dxValues.clone();
		if (dyValues != null) c.dyValues = this.dyValues.clone();
		if (dzValues != null) c.dzValues = this.dzValues.clone();
		c.legend = this.legend;
		c.showErrorBars = this.showErrorBars;
		if (xValues != null) c.xValues = this.xValues.clone();
		if (yValues != null) c.yValues = this.yValues.clone();
		if (this.zValues !=  null) c.zValues = Serialization.copy(this.zValues);
		c.drawLines = this.drawLines;
		c.isSurface = this.isSurface;
		c.isBarPlot = this.isBarPlot;
		if (pointers != null) c.pointers = this.pointers.clone();
		return c;
	}

	/**
	 * Returns true if the input object is equals to this chart object.
	 * The z array is not checked.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ChartSeriesElement3D)) return false;

		ChartSeriesElement3D that = (ChartSeriesElement3D) o;

		if (showErrorBars != that.showErrorBars) return false;
		if (isSurface != that.isSurface) return false;
		if (drawLines != that.drawLines) return false;
		if (isBarPlot != that.isBarPlot) return false;

		if (!Arrays.equals(xValues, that.xValues)) return false;

		if (!Arrays.equals(yValues, that.yValues)) return false;
		//if (zValues != null ? !zValues.equals(that.zValues) : that.zValues != null) return false;
		if (!Arrays.equals(dxValues, that.dxValues)) return false;
		if (!Arrays.equals(dyValues, that.dyValues)) return false;
		if (!Arrays.equals(dzValues, that.dzValues)) return false;
		if (legend != null ? !legend.equals(that.legend) : that.legend != null) return false;

		if (!Arrays.equals(pointers, that.pointers)) return false;

		return !(color != null ? !color.equals(that.color) : that.color != null);
	}

	@Override
	public int hashCode() {
		int result = xValues != null ? Arrays.hashCode(xValues) : 0;
		result = 31 * result + (yValues != null ? Arrays.hashCode(yValues) : 0);
		//result = 31 * result + (zValues != null ? zValues.hashCode() : 0);
		result = 31 * result + (dxValues != null ? Arrays.hashCode(dxValues) : 0);
		result = 31 * result + (dyValues != null ? Arrays.hashCode(dyValues) : 0);
		result = 31 * result + (dzValues != null ? Arrays.hashCode(dzValues) : 0);
		result = 31 * result + (legend != null ? legend.hashCode() : 0);
		result = 31 * result + (pointers != null ? Arrays.hashCode(pointers) : 0);
		result = 31 * result + (color != null ? color.hashCode() : 0);
		result = 31 * result + (showErrorBars ? 1 : 0);
		result = 31 * result + (isSurface ? 1 : 0);
		result = 31 * result + (drawLines ? 1 : 0);
		result = 31 * result + (isBarPlot ? 1 : 0);
		return result;
	}


	/**
	 * Returns the characteristic name of this instance.
	 * @return The characteristic name, currently equals to the legend.
	 */
	public String getInstanceName()
	{
		return this.legend;
	}

	/**
	 * Transform a dataset from a {@link GridChartElement} to a set of 3d points.
	 * @param data The data as a regural array.
	 * @param limits The limits: minX, maxX, minY, maxY.
	 * @return The series.
	 */
	public static ChartSeriesElement3D getPointsFromDataSet(double data[][], double limits[])
	{
        double sx = (limits[1] - limits[0]) / ((double) data.length - 1.0);
        double sy = (limits[3] - limits[2]) / ((double) data[0].length - 1.0);
        int size = data.length * data[0].length;
        double x[] = new double[size];
        double y[] = new double[size];
        double z[] = new double[size];

        int index = 0;
        for (int i=0; i<x.length; i++)
        {
        	double px = limits[0] + sx * (double) i;
            for (int j=0; j<x.length; j++)
            {
            	index ++;
            	double py = limits[2] + sy * (double) j;
            	x[index] = px;
            	y[index] = py;
            	z[index] = data[i][j];
            }
        }
		ChartSeriesElement3D s = new ChartSeriesElement3D(x, y, z, "");
		return s;
	}

	/**
	 * Transform a dataset from a {@link GridChartElement} to a set of 3d points.
	 * @param data The data as a regural array.
	 * @param limits The limits: minX, maxX, minY, maxY.
	 * @return The points.
	 */
	public static DoubleVector[] get3dPointsFromDataSet(double data[][], double limits[])
	{
        double sx = (limits[1] - limits[0]) / ((double) data.length - 1.0);
        double sy = (limits[3] - limits[2]) / ((double) data[0].length - 1.0);
        int size = 1+data.length * data[0].length;
        double x[] = new double[size];
        double y[] = new double[size];
        double z[] = new double[size];

        int index = 0;
        for (int i=0; i<data.length; i++)
        {
        	double px = limits[0] + sx * (double) i;
            for (int j=0; j<data[0].length; j++)
            {
            	index ++;
            	double py = limits[2] + sy * (double) j;
            	x[index] = px;
            	y[index] = py;
            	z[index] = data[i][j];
            }
        }

        DoubleVector points[] = new DoubleVector[x.length];
        for (int i=0; i<x.length; i++)
        {
        	points[i] = new DoubleVector(new double[] {x[i], y[i], z[i]});
        }
		return points;
	}

	/**
	 * Creates a surface series given an irregular set of points that lies on it.
	 * This method uses an interpolation algorithm similar to a discrete
	 * Fourier transform, but more adequate to properly treat the 'holes'.
	 * If the points contain no holes and are sampled regularly it is recommended
	 * to use 2d spline interpolation with class {@linkplain ImageSplineTransform}.
	 * @param points The array of points.
	 * @param n The number of points in the x and y axis of the
	 * output regular profile.
	 * @return The series, with empty string for the legend.
	 * @throws JPARSECException If an error occurs.
	 */
	public static ChartSeriesElement3D getSurfaceFromPoints(DoubleVector points[], int n)
	throws JPARSECException {

		double xs[] = new double[points.length];
		double ys[] = new double[points.length];
		double zs[] = new double[points.length];
		for (int i=0; i<points.length; i++)
		{
			xs[i] = points[i].get(0);
			ys[i] = points[i].get(1);
			zs[i] = points[i].get(2);
		}
		double xmin = DataSet.getMinimumValue(xs);
		double xmax = DataSet.getMaximumValue(xs);
		double ymin = DataSet.getMinimumValue(ys);
		double ymax = DataSet.getMaximumValue(ys);

		 // Adequate value to fully fill the beam with the sample => correct normalization factor
		double beam_x = (xmax - xmin) / Math.sqrt(points.length);
		double beam_y = (ymax - ymin) / Math.sqrt(points.length);
		double data[][] = new double[n][n];
		double scaleX = (xmax - xmin) / (double) (n - 1);
		double scaleY = (ymax - ymin) / (double) (n - 1);
		double factor_sum[][] = new double[n][n];
		double outX[] = new double[n];
		double outY[] = new double[n];
		for (int ix=0; ix<n; ix++)
		{
			outX[ix] = xmin + scaleX * (double) ix;
			for (int iy=0; iy<n; iy++)
			{
				outY[iy] = ymin + scaleY * (double) iy;
				data[ix][iy] = 0.0;
				factor_sum[ix][iy] = 0.0;

				for (int i=0; i<points.length; i++)
				{
					double dx = xs[i] - outX[ix];
					double dy = ys[i] - outY[iy];

					double factor_x = (dx * dx) / (beam_x * beam_x);
					double factor_y = (dy * dy) / (beam_y * beam_y);

					// This profile is beam-dependent.
					double factor = Math.exp(-0.5 * 8.0 * Math.log(2.0) * (factor_x + factor_y));

					data[ix][iy] += factor * zs[i];
					factor_sum[ix][iy] += factor;
				}
			}
		}

		// Normalize
		for (int ix=0; ix<n; ix++)
		{
			for (int iy=0; iy<n; iy++)
			{
				data[ix][iy] = data[ix][iy] / factor_sum[ix][iy];
			}
		}

		ChartSeriesElement3D out = new ChartSeriesElement3D(outX, outY, data, "");
		return out;
	}

	/**
	 * Returns the limits of the dataset in this series.
	 * @return Minimum x, maximum x, minimum y, maximum y.
	 * @throws JPARSECException If an error occurs.
	 */
	public double[] getLimits() throws JPARSECException {
		double px[] = DataSet.toDoubleValues(xValues);
		double py[] = DataSet.toDoubleValues(yValues);
		return new double[] {DataSet.getMinimumValue(px), DataSet.getMaximumValue(px), DataSet.getMinimumValue(py),
				DataSet.getMaximumValue(py)};
	}
}
