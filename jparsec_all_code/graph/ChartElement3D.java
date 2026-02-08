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

import java.awt.Color;
import java.io.Serializable;
import java.util.Arrays;

import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Logger.LEVEL;

/**
 * Creates a chart element to be later drawn by JMathPlot.<P>
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class ChartElement3D implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Default empty constructor.
	 */
	public ChartElement3D()	{	}

	/**
	 * Constructor adequate for any chart.
	 *
	 * @param series Series to draw.
	 * @param title Title.
	 * @param x_label X axis label.
	 * @param y_label Y axis label
	 * @param z_label Z axis label
	 */
	public ChartElement3D(ChartSeriesElement3D series[], String title, String x_label,
			String y_label, String z_label)
	{
		this.series = series;
		this.xLabel = x_label;
		this.yLabel = y_label;
		this.zLabel = z_label;
		this.title = title;
		this.imageWidth = 600;
		this.imageHeight = 600;
	}

	/**
	 * Constructor adequate for any chart.
	 *
	 * @param series Series to draw.
	 * @param title Title.
	 * @param x_label X axis label.
	 * @param y_label Y axis label
	 * @param z_label Z axis label
	 * @param width Width in pixels.
	 * @param height Height.
	 */
	public ChartElement3D(ChartSeriesElement3D series[], String title, String x_label,
			String y_label, String z_label, int width, int height)
	{
		this.series = series;
		this.xLabel = x_label;
		this.yLabel = y_label;
		this.zLabel = z_label;
		this.title = title;
		this.imageWidth = width;
		this.imageHeight = height;
	}

	/**
	 * Series to draw.
	 */
	public ChartSeriesElement3D[] series;

	/**
	 * Label for x axis.
	 */
	public String xLabel;

	/**
	 * Label for y axis.
	 */
	public String yLabel;

	/**
	 * Label for z axis.
	 */
	public String zLabel;

	/**
	 * Title.
	 */
	public String title;

	/**
	 * X size of the chart in pixels.
	 */
	public int imageWidth;

	/**
	 * Y size of the chart in pixels.
	 */
	public int imageHeight;

	/**
	 * Show x axis in log scale.
	 */
	public boolean xAxisInLogScale = false;

	/**
	 * Show y axis in log scale.
	 */
	public boolean yAxisInLogScale = false;

	/**
	 * Show y axis in log scale.
	 */
	public boolean zAxisInLogScale = false;

	/**
	 * Select to show error bars or not.
	 */
	public boolean showErrorBars = true;

	/**
	 * Select to show the title or not.
	 */
	public boolean showTitle = true;

	/**
	 * Selects the color of the background.
	 */
	public Color background = Color.WHITE;

	/**
	 * True (default) to show the grid in the x axis.
	 */
	public boolean showGridX = true;
	/**
	 * True (default) to show the grid in the y axis.
	 */
	public boolean showGridY = true;
	/**
	 * True (default) to show the grid in the z axis.
	 */
	public boolean showGridZ = true;
	/**
	 * True (default) to show the legend.
	 */
	public boolean showLegend = true;
	/**
	 * True (default) to show the toolbar.
	 */
	public boolean showToolbar = true;

	/**
	 * To clone the object.
	 */
	@Override
	public ChartElement3D clone()
	{
		ChartElement3D c = null;
		try {
			c = new ChartElement3D(this.series.clone(), this.title, this.xLabel,
				this.yLabel, this.zLabel);
			c.imageHeight = this.imageHeight;
			c.imageWidth = this.imageWidth;
			c.background = this.background;
			c.showErrorBars = this.showErrorBars;
			c.xAxisInLogScale = this.xAxisInLogScale;
			c.yAxisInLogScale = this.yAxisInLogScale;
			c.zAxisInLogScale = this.zAxisInLogScale;
			c.showGridX = this.showGridX;
			c.showGridY = this.showGridY;
			c.showGridZ = this.showGridZ;
			c.showLegend = this.showLegend;
			c.showToolbar = this.showToolbar;
			c.showTitle = this.showTitle;
		} catch (Exception e)
		{
			Logger.log(LEVEL.ERROR, "Error cloning instance. Message was: "+e.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e.getStackTrace()));
		}

		return c;
	}

	/**
	 * Returns true if the input object is equals to this chart object. An hypothetical sub-chart
	 * is not tested for equality.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ChartElement3D)) return false;

		ChartElement3D that = (ChartElement3D) o;

		if (imageWidth != that.imageWidth) return false;
		if (imageHeight != that.imageHeight) return false;
		if (xAxisInLogScale != that.xAxisInLogScale) return false;
		if (yAxisInLogScale != that.yAxisInLogScale) return false;
		if (zAxisInLogScale != that.zAxisInLogScale) return false;
		if (showErrorBars != that.showErrorBars) return false;
		if (showTitle != that.showTitle) return false;
		if (showGridX != that.showGridX) return false;
		if (showGridY != that.showGridY) return false;
		if (showGridZ != that.showGridZ) return false;
		if (showLegend != that.showLegend) return false;
		if (showToolbar != that.showToolbar) return false;

		if (!Arrays.equals(series, that.series)) return false;
		if (xLabel != null ? !xLabel.equals(that.xLabel) : that.xLabel != null) return false;
		if (yLabel != null ? !yLabel.equals(that.yLabel) : that.yLabel != null) return false;
		if (zLabel != null ? !zLabel.equals(that.zLabel) : that.zLabel != null) return false;
		if (title != null ? !title.equals(that.title) : that.title != null) return false;

		return !(background != null ? !background.equals(that.background) : that.background != null);
	}

	@Override
	public int hashCode() {
		int result = series != null ? Arrays.hashCode(series) : 0;
		result = 31 * result + (xLabel != null ? xLabel.hashCode() : 0);
		result = 31 * result + (yLabel != null ? yLabel.hashCode() : 0);
		result = 31 * result + (zLabel != null ? zLabel.hashCode() : 0);
		result = 31 * result + (title != null ? title.hashCode() : 0);
		result = 31 * result + imageWidth;
		result = 31 * result + imageHeight;
		result = 31 * result + (xAxisInLogScale ? 1 : 0);
		result = 31 * result + (yAxisInLogScale ? 1 : 0);
		result = 31 * result + (zAxisInLogScale ? 1 : 0);
		result = 31 * result + (showErrorBars ? 1 : 0);
		result = 31 * result + (showTitle ? 1 : 0);
		result = 31 * result + (background != null ? background.hashCode() : 0);
		result = 31 * result + (showGridX ? 1 : 0);
		result = 31 * result + (showGridY ? 1 : 0);
		result = 31 * result + (showGridZ ? 1 : 0);
		result = 31 * result + (showLegend ? 1 : 0);
		result = 31 * result + (showToolbar ? 1 : 0);
		return result;
	}

	/**
	 * Obtains the maximum value of x. For category charts the index of the maximum value
	 * will be returned.
	 * @return Maximum x.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getxMax() throws JPARSECException
	{
		double max = 0.0;
		boolean done = false;
		for (int i = 0; i < this.series.length; i++)
		{
			double x[] = DataSet.toDoubleValues(this.series[i].xValues);

			double m = DataSet.getMaximumValue(x);
			if (m > max || !done)
			{
				max = m;
				done = true;
			}

		}
		return max;
	}

	/**
	 * Obtains the minimum value of x. For category charts the index of the minimum value
	 * will be returned.
	 * @return Minimum x.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getxMin() throws JPARSECException
	{
		double min = 0.0;
		boolean done = false;
		for (int i = 0; i < this.series.length; i++)
		{
			double x[] = DataSet.toDoubleValues(this.series[i].xValues);

			double m = DataSet.getMinimumValue(x);
			if (m < min || !done)
			{
				min = m;
				done = true;
			}
		}
		return min;
	}

	/**
	 * Obtains the maximum value of y.
	 * @return Maximum y.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getyMax() throws JPARSECException
	{
		double max = 0.0;
		boolean done = false;
		for (int i = 0; i < this.series.length; i++)
		{
			double x[] = DataSet.toDoubleValues(this.series[i].yValues);
			double m = DataSet.getMaximumValue(x);
			if (m > max || !done)
			{
				max = m;
				done = true;
			}
		}
		return max;
	}

	/**
	 * Obtains the minimum value of y.
	 * @return Minimum y.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getyMin() throws JPARSECException
	{
		double min = 0.0;
		boolean done = false;
		for (int i = 0; i < this.series.length; i++)
		{
			double x[] = DataSet.toDoubleValues(this.series[i].yValues);
			double m = DataSet.getMinimumValue(x);
			if (m < min || !done)
			{
				min = m;
				done = true;
			}
		}
		return min;
	}

	/**
	 * Obtains the maximum value of z, supposing they were entered as an
	 * array of doubles.
	 * @return Maximum z.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getzMax() throws JPARSECException
	{
		double max = 0.0;
		boolean done = false;
		for (int i = 0; i < this.series.length; i++)
		{
			double x[] = (double[]) this.series[i].zValues;

			double m = DataSet.getMaximumValue(x);
			if (m > max || !done)
			{
				max = m;
				done = true;
			}

		}
		return max;
	}

	/**
	 * Obtains the minimum value of z, supposing they were entered as an
	 * array of doubles.
	 * @return Minimum z.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getzMin() throws JPARSECException
	{
		double min = 0.0;
		boolean done = false;
		for (int i = 0; i < this.series.length; i++)
		{
			double x[] = (double[]) this.series[i].zValues;

			double m = DataSet.getMinimumValue(x);
			if (m < min || !done)
			{
				min = m;
				done = true;
			}
		}
		return min;
	}

}
