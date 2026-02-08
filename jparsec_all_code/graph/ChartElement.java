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
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import jparsec.ephem.Functions;
import jparsec.graph.chartRendering.AWTGraphics;
import jparsec.graph.chartRendering.Graphics;
import jparsec.io.image.Picture;
import jparsec.math.Constant;
import jparsec.time.AstroDate;
import jparsec.util.JPARSECException;
import jparsec.util.Logger;
import jparsec.util.Logger.LEVEL;

/**
 * Creates a chart element to be later drawn by JFreeChart.<BR>
 *
 * In this object the labels for x and y axes, as well as the title can be
 * encoded following the instructions given in {@linkplain TextLabel} class. This
 * provides some possibilities like to dynamically change color/size, include superscript
 * or subscript text, or greek letters.
 *
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class ChartElement implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Default empty constructor.
	 */
	public ChartElement() { }

	/**
	 * Constructor adequate for any chart. In pie charts only the first series
	 * will be considered.
	 *
	 * @param series Series to draw.
	 * @param charttype Type of chart.
	 * @param chartsubtype Subtype of chart.
	 * @param title Title.
	 * @param x_label X axis label.
	 * @param y_label Y axis label
	 * @param change_orientation True to change orientation.
	 * @throws JPARSECException If an error occurs or chart type and subtype are incompatible.
	 */
	public ChartElement(ChartSeriesElement series[], TYPE charttype, SUBTYPE chartsubtype, String title, String x_label,
			String y_label, boolean change_orientation)
	throws JPARSECException {
		this.series = series;
		this.chartType = charttype;
		this.subType = chartsubtype;
		if ((chartType == TYPE.XY_CHART && subType.name().indexOf("XY") < 0) ||
			(chartType == TYPE.CATEGORY_CHART && subType.name().indexOf("CATEGORY") < 0) ||
			(chartType == TYPE.PIE_CHART && subType.name().indexOf("PIE") < 0))
			throw new JPARSECException("Subtype "+subType.name()+" is not acceptable for chart type "+chartType.name());
		this.xLabel = x_label;
		this.yLabel = y_label;
		this.title = title;
		this.changeOrientationToHorizontal = change_orientation;
		this.imageWidth = 600;
		this.imageHeight = 400;
		if (this.chartType == ChartElement.TYPE.CATEGORY_CHART) this.setCategories();

		double mins[] = new double[series.length];
		for (int i=0; i<mins.length; i++)
		{
			if (series[i] != null) mins[i] = series[i].yMinimumValue;
		}
		try {
			double min = DataSet.getMinimumValue(mins);
			for (int i=0; i<mins.length; i++)
			{
				if (series[i] != null) series[i].yMinimumValue = min;
			}
		} catch (JPARSECException ex) { }
		checkChartElementLegends();
	}

	/**
	 * Constructor adequate for any chart. In pie charts only the first series
	 * will be considered.
	 *
	 * @param series Series to draw.
	 * @param charttype Type of chart.
	 * @param chartsubtype Subtype of chart.
	 * @param title Title.
	 * @param x_label X axis label.
	 * @param y_label Y axis label
	 * @param change_orientation True to change orientation.
	 * @param size_x Chart width.
	 * @param size_y Chart height.
	 * @throws JPARSECException If an error occurs or chart type and subtype are incompatible.
	 */
	public ChartElement(ChartSeriesElement series[], TYPE charttype, SUBTYPE chartsubtype, String title, String x_label,
			String y_label, boolean change_orientation, int size_x, int size_y)
	throws JPARSECException {
		this.series = series;
		this.chartType = charttype;
		this.subType = chartsubtype;
		if ((chartType == TYPE.XY_CHART && subType.name().indexOf("XY") < 0) ||
				(chartType == TYPE.CATEGORY_CHART && subType.name().indexOf("CATEGORY") < 0) ||
				(chartType == TYPE.PIE_CHART && subType.name().indexOf("PIE") < 0))
				throw new JPARSECException("Subtype "+subType.name()+" is not acceptable for chart type "+chartType.name());
		this.xLabel = x_label;
		this.yLabel = y_label;
		this.title = title;
		this.changeOrientationToHorizontal = change_orientation;
		this.imageWidth = size_x;
		this.imageHeight = size_y;
		if (this.chartType == ChartElement.TYPE.CATEGORY_CHART) this.setCategories();

		double mins[] = new double[series.length];
		for (int i=0; i<mins.length; i++)
		{
			if (series[i] != null) mins[i] = series[i].yMinimumValue;
		}
		try {
			double min = DataSet.getMinimumValue(mins);
			for (int i=0; i<mins.length; i++)
			{
				if (series[i] != null) series[i].yMinimumValue = min;
			}
		} catch (JPARSECException ex) { }
		checkChartElementLegends();
	}

	/**
	 * Series to draw. In pie charts only the first series will be considered.
	 */
	public ChartSeriesElement[] series;

	/**
	 * Holds the chart type.
	 */
	public TYPE chartType;

	/**
	 * Label for x axis. Ignored in pie charts.
	 */
	public String xLabel;

	/**
	 * Label for y axis. Ignored in pie charts.
	 */
	public String yLabel;

	/**
	 * Title.
	 */
	public String title;

	/**
	 * Sets all x values for category charts. Adequate values
	 * are automatically set in the constructors.
	 */
	public String[] xForCategoryCharts;

	/**
	 * Change chart orientation to vertical. Ignored in pie charts.
	 */
	public boolean changeOrientationToHorizontal;

	/**
	 * The chart subtype.
	 */
	public SUBTYPE subType;

	/**
	 * X size of the chart in pixels.
	 */
	public int imageWidth;

	/**
	 * Y size of the chart in pixels.
	 */
	public int imageHeight;

	/**
	 * Holds a sub-chart to be drawn inside the main one.
	 */
	public ChartElement subCharts[] = null;
	/**
	 * Holds sub-chart position.
	 */
	public Point subChartPosition[] = null;

	/**
	 * Show x axis in log scale.
	 */
	public boolean xAxisInLogScale = false;

	/**
	 * Show y axis in log scale.
	 */
	public boolean yAxisInLogScale = false;

	/**
	 * Set to true to invert the x axis. Not supported in category
	 * charts.
	 */
	public boolean xAxisInverted = false;

	/**
	 * Set to true to invert the y axis.
	 */
	public boolean yAxisInverted = false;

	/**
	 * Select to show error bars or not.
	 */
	public boolean showErrorBars = true;

	/**
	 * Selects the color/gradient of the background. It is recommended
	 * to use just a color instead a gradient for better compatibility
	 * with output libraries (eps, pdf, svg formats).
	 */
	public Paint backgroundGradient = Color.WHITE;

	/**
	 * Selects the background image.
	 */
	public Image backgroundImage;

	/**
	 * Selects to show or not the background image.
	 */
	public boolean showBackgroundImage = false;

	/**
	 * Selects to show or not the background image only in the data area, not in
	 * the whole chart.
	 */
	public boolean showBackgroundImageOnlyInDataArea = false;

	/**
	 * Sets the tick labels style for x axis. Default value is regular values.
	 */
	public TICK_LABELS xTickLabels = TICK_LABELS.REGULAR_VALUES;

	/**
	 * Sets the tick labels style for y axis. Default value is regular values.
	 */
	public TICK_LABELS yTickLabels = TICK_LABELS.REGULAR_VALUES;

	/**
	 * To clone the object.
	 */
	@Override
	public ChartElement clone()
	{
		ChartElement c = null;
		try {
			ChartSeriesElement newSeries[] = new ChartSeriesElement[this.series.length];
			for (int i=0; i<newSeries.length; i++) {
				newSeries[i] = this.series[i].clone();
			}
			c = new ChartElement(newSeries, this.chartType, this.subType, this.title, this.xLabel,
				this.yLabel, this.changeOrientationToHorizontal, this.imageWidth, this.imageHeight);
			c.backgroundGradient = this.backgroundGradient;
			c.backgroundImage = this.backgroundImage;
			c.showBackgroundImage = this.showBackgroundImage;
			c.showBackgroundImageOnlyInDataArea = this.showBackgroundImageOnlyInDataArea;
			c.showErrorBars = this.showErrorBars;
			c.xTickLabels = this.xTickLabels;
			c.yTickLabels = this.yTickLabels;
			c.xAxisInLogScale = this.xAxisInLogScale;
			c.yAxisInLogScale = this.yAxisInLogScale;
			c.xAxisInverted = this.xAxisInverted;
			c.yAxisInverted = this.yAxisInverted;
			if (this.xForCategoryCharts != null) c.xForCategoryCharts = this.xForCategoryCharts.clone();
			if (this.subCharts != null) c.subCharts = this.subCharts.clone();
			if (this.subChartPosition != null) c.subChartPosition = this.subChartPosition.clone();
		} catch (Exception e)
		{
			Logger.log(LEVEL.ERROR, "Error cloning instance. Message was: "+e.getLocalizedMessage()+". Trace: "+JPARSECException.getTrace(e.getStackTrace()));
		}

		return c;
	}

	/**
	 * Returns true if the input object is equals to this chart object. An hypothetical sub-chart
	 * is not tested for equallity.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ChartElement)) return false;

		ChartElement that = (ChartElement) o;

		if (changeOrientationToHorizontal != that.changeOrientationToHorizontal) return false;
		if (imageWidth != that.imageWidth) return false;
		if (imageHeight != that.imageHeight) return false;
		if (xAxisInLogScale != that.xAxisInLogScale) return false;
		if (yAxisInLogScale != that.yAxisInLogScale) return false;
		if (xAxisInverted != that.xAxisInverted) return false;
		if (yAxisInverted != that.yAxisInverted) return false;
		if (showErrorBars != that.showErrorBars) return false;
		if (showBackgroundImage != that.showBackgroundImage) return false;
		if (showBackgroundImageOnlyInDataArea != that.showBackgroundImageOnlyInDataArea) return false;

		if (!Arrays.equals(series, that.series)) return false;
		if (chartType != that.chartType) return false;
		if (xLabel != null ? !xLabel.equals(that.xLabel) : that.xLabel != null) return false;
		if (yLabel != null ? !yLabel.equals(that.yLabel) : that.yLabel != null) return false;
		if (title != null ? !title.equals(that.title) : that.title != null) return false;

		if (!Arrays.equals(xForCategoryCharts, that.xForCategoryCharts)) return false;
		if (subType != that.subType) return false;

		if (!Arrays.equals(subCharts, that.subCharts)) return false;

		if (!Arrays.equals(subChartPosition, that.subChartPosition)) return false;
		if (backgroundGradient != null ? !backgroundGradient.equals(that.backgroundGradient) : that.backgroundGradient != null)
			return false;
		if (backgroundImage != null ? !backgroundImage.equals(that.backgroundImage) : that.backgroundImage != null)
			return false;
		if (xTickLabels != that.xTickLabels) return false;

		return yTickLabels == that.yTickLabels;
	}

	@Override
	public int hashCode() {
		int result = series != null ? Arrays.hashCode(series) : 0;
		result = 31 * result + (chartType != null ? chartType.hashCode() : 0);
		result = 31 * result + (xLabel != null ? xLabel.hashCode() : 0);
		result = 31 * result + (yLabel != null ? yLabel.hashCode() : 0);
		result = 31 * result + (title != null ? title.hashCode() : 0);
		result = 31 * result + (xForCategoryCharts != null ? Arrays.hashCode(xForCategoryCharts) : 0);
		result = 31 * result + (changeOrientationToHorizontal ? 1 : 0);
		result = 31 * result + (subType != null ? subType.hashCode() : 0);
		result = 31 * result + imageWidth;
		result = 31 * result + imageHeight;
		result = 31 * result + (subCharts != null ? Arrays.hashCode(subCharts) : 0);
		result = 31 * result + (subChartPosition != null ? Arrays.hashCode(subChartPosition) : 0);
		result = 31 * result + (xAxisInLogScale ? 1 : 0);
		result = 31 * result + (yAxisInLogScale ? 1 : 0);
		result = 31 * result + (xAxisInverted ? 1 : 0);
		result = 31 * result + (yAxisInverted ? 1 : 0);
		result = 31 * result + (showErrorBars ? 1 : 0);
		result = 31 * result + (backgroundGradient != null ? backgroundGradient.hashCode() : 0);
		result = 31 * result + (backgroundImage != null ? backgroundImage.hashCode() : 0);
		result = 31 * result + (showBackgroundImage ? 1 : 0);
		result = 31 * result + (showBackgroundImageOnlyInDataArea ? 1 : 0);
		result = 31 * result + (xTickLabels != null ? xTickLabels.hashCode() : 0);
		result = 31 * result + (yTickLabels != null ? yTickLabels.hashCode() : 0);
		return result;
	}
	/**
	 * Transforms a {@linkplain SimpleChartElement} into a {@linkplain ChartElement}.
	 * @param sc A {@linkplain SimpleChartElement}.
	 * @return The {@linkplain ChartElement}.
	 * @throws JPARSECException Is an error occurs.
	 */
	public static ChartElement parseSimpleChartElement(SimpleChartElement sc)
	throws JPARSECException {
		ChartSeriesElement series[] = new ChartSeriesElement[] {
				new ChartSeriesElement(sc.xValues, sc.yValues, null, null, sc.legend, sc.showLegend,
				Color.BLACK, ChartSeriesElement.SHAPE_CIRCLE, ChartSeriesElement.REGRESSION.NONE)};
		series[0].showLines = false;
		if (sc.chartType == ChartElement.TYPE.CATEGORY_CHART && (
				sc.subType == ChartElement.SUBTYPE.CATEGORY_LINE ||
				sc.subType == ChartElement.SUBTYPE.CATEGORY_LINE_3D)) series[0].showLines = true;
		if (sc.chartType == ChartElement.TYPE.XY_CHART && (
				sc.subType == ChartElement.SUBTYPE.XY_LINE ||
				sc.subType == ChartElement.SUBTYPE.XY_STEP)) series[0].showLines = true;
		series[0].stroke = JPARSECStroke.STROKE_DEFAULT_LINE;
		series[0].showShapes = true;
		if (sc.yValues.length > 0) series[0].yMinimumValue = DataSet.getMinimumValue(sc.yValues);

		ChartElement c = new ChartElement(series, sc.chartType, sc.subType, sc.title, sc.xLabel,
				sc.yLabel, sc.changeOrientationToHorizontal, sc.imageWidth, sc.imageHeight);
		c.backgroundGradient = sc.backgroundGradient;
		c.showBackgroundImage = false;
		c.showBackgroundImageOnlyInDataArea = false;
		c.showErrorBars = false;
		c.xTickLabels = ChartElement.TICK_LABELS.REGULAR_VALUES;
		c.yTickLabels = ChartElement.TICK_LABELS.REGULAR_VALUES;
		c.xAxisInLogScale = sc.xAxisInLogScale;
		c.yAxisInLogScale = sc.yAxisInLogScale;
		c.xForCategoryCharts = sc.xValuesForPieAndCategoryCharts;

		return c;
	}

	/**
	 * Adds a new series to the current chart.
	 * @param series Series object.
	 */
	public void addSeries(ChartSeriesElement series)
	{
		ChartSeriesElement s[] = this.series;
		ChartSeriesElement ns[] = new ChartSeriesElement[s.length+1];
		for (int i=0; i<s.length; i++)
		{
			ns[i] = s[i]; //.clone();
		}
		ns[ns.length-1] = series.clone();
		this.series = ns;
	}
	/**
	 * Adds a new series to the current chart.
	 * @param series Series object.
	 * @param index Index for the position of this series.
	 */
	public void addSeries(ChartSeriesElement series, int index)
	{
		ChartSeriesElement s[] = this.series;
		ChartSeriesElement ns[] = new ChartSeriesElement[s.length+1];
		int in = -1;
		for (int i=0; i<ns.length; i++)
		{
			if (i != index) {
				in ++;
				ns[i] = s[in]; //.clone();
			} else {
				ns[i] = series;
			}
		}
		this.series = ns;
	}

	/**
	 * Deletes a series from the current chart.
	 * @param index Series index.
	 */
	public void deleteSeries(int index)
	{
		ChartSeriesElement s[] = this.series;
		ChartSeriesElement ns[] = new ChartSeriesElement[s.length-1];
		int in = -1;
		for (int i=0; i<s.length; i++)
		{
			if (i != index) {
				in ++;
				ns[in] = s[i]; //.clone();
			}
		}
		this.series = ns;
	}

	/**
	 * Deletes a series from the current chart.
	 * @param legend Legend of the series to delete.
	 * @throws JPARSECException If an error occur.
	 */
	public void deleteSeries(String legend)
	throws JPARSECException {
		try {
			ChartSeriesElement s[] = this.series;
			boolean exist = false;
			for (int i=0; i<s.length; i++)
			{
				if (s[i].legend.equals(legend)) {
					exist = true;
				}
			}

			ChartSeriesElement ns[] = new ChartSeriesElement[s.length];
			if (exist) ns = new ChartSeriesElement[s.length-1];
			int in = -1;
			for (int i=0; i<s.length; i++)
			{
				if (!s[i].legend.equals(legend)) {
					in ++;
					ns[in] = s[i]; //.clone();
				}
			}
			this.series = ns;
		} catch (Exception ex)
		{
			throw new JPARSECException(ex);
		}
	}

	/**
	 * Deletes all series from the current chart except one.
	 * @param legend Legend of the series to maintain.
	 * @throws JPARSECException If an error occur.
	 */
	public void deleteAllSeriesExcept(String legend)
	throws JPARSECException {
		try {
			ChartSeriesElement s[] = this.series;
			ChartSeriesElement ns[] = new ChartSeriesElement[1];
			int in = -1;
			for (int i=0; i<s.length; i++)
			{
				if (s[i].legend.equals(legend)) {
					in ++;
					ns[in] = s[i]; //.clone();
					break;
				}
			}
			this.series = ns;
		} catch (Exception ex)
		{
			throw new JPARSECException(ex);
		}
	}

	/**
	 * Returns the index of a point in a category chart.
	 * @param category Value of the point as a category string.
	 * @return Index for this value in array {@linkplain ChartElement#xForCategoryCharts},
	 * or -1 if it is not found.
	 */
	public int getIndexOfCategoryPoint(String category)
	{
		int val = -1;
		for (int jj=0;jj<this.xForCategoryCharts.length; jj++)
		{
			if (this.xForCategoryCharts[jj].equals(category)) {
				val = jj;
				break;
			}
		}
		return val;
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
			double x[] = new double[this.series[i].xValues.length];
			if (this.chartType == ChartElement.TYPE.CATEGORY_CHART) {
				for (int jjj=0;jjj<this.series[i].xValues.length; jjj++)
				{
					x[jjj] = this.getIndexOfCategoryPoint(this.series[i].xValues[jjj]);
				}
			} else {
				x = (double[]) DataSet.getDoubleValuesIncludingLimits(this.series[i].xValues).get(0);
			}

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
			double x[] = new double[this.series[i].xValues.length];
			if (this.chartType == ChartElement.TYPE.CATEGORY_CHART) {
				for (int jjj=0;jjj<this.series[i].xValues.length; jjj++)
				{
					x[jjj] = this.getIndexOfCategoryPoint(this.series[i].xValues[jjj]);
				}
			} else {
				x = (double[]) DataSet.getDoubleValuesIncludingLimits(this.series[i].xValues).get(0);
			}

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
			double x[] = (double[]) DataSet.getDoubleValuesIncludingLimits(this.series[i].yValues).get(0);
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
			double x[] = (double[]) DataSet.getDoubleValuesIncludingLimits(this.series[i].yValues).get(0);
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
	 * Obtains the maximum value of x errors. For category charts the index of the maximum value
	 * will be returned.
	 * @return Maximum x error.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getxErrorMax() throws JPARSECException
	{
		double max = 0.0;
		boolean done = false;
		for (int i = 0; i < this.series.length; i++)
		{
			if (this.series[i].dxValues != null) {
				double m = DataSet.getMaximumValue(this.series[i].dxValues);
				if (m > max || !done)
				{
					max = m;
					done = true;
				}
			}

		}
		return max;
	}

	/**
	 * Obtains the maximum value of y errors.
	 * @return Maximum y error.
	 * @throws JPARSECException If an error occurs.
	 */
	public double getyErrorMax() throws JPARSECException
	{
		double max = 0.0;
		boolean done = false;
		for (int i = 0; i < this.series.length; i++)
		{
			if (this.series[i].dyValues != null) {
				double m = DataSet.getMaximumValue(this.series[i].dyValues);
				if (m > max || !done)
				{
					max = m;
					done = true;
				}
			}
		}
		return max;
	}

	private void setCategories()
	{
		ArrayList<String> v = new ArrayList<String>();
		for (int i=0; i<this.series.length; i++)
		{
			if (!ChartSeriesElement.equalShapes(series[i].shape, ChartSeriesElement.SHAPE_EMPTY)) series[i].showShapes = true;

			for (int j=0; j<this.series[i].xValues.length; j++)
			{
				String cat = this.series[i].xValues[j];
				if (!v.contains(cat)) v.add(cat);
			}
		}
		this.xForCategoryCharts = DataSet.arrayListToStringArray(v);
	}

	private void checkChartElementLegends()
	throws JPARSECException {
		if (this.series.length < 2) return;

		for (int i=0; i<this.series.length-1; i++)
		{
			String si = this.series[i].legend;
			int ai = si.indexOf("*");
			if (ai > 0 && si.endsWith("*")) si = si.substring(0, ai);
			for (int j=i+1; j<this.series.length; j++)
			{
				String sj = this.series[j].legend;
				int aj = sj.indexOf("*");
				if (aj > 0 && sj.endsWith("*")) sj = sj.substring(0, aj);

				if (si.equals(sj))
					throw new JPARSECException("series are not allowed to have similar legend names: "+si+".");
			}
		}
	}

	/**
	 * The set of chart types.
	 */
	public enum TYPE {
		/** Constant ID for an xy chart. */
		XY_CHART,
		/** Constant ID for a category chart. */
		CATEGORY_CHART,
		/** Constant ID for a pie chart. */
		PIE_CHART
	};

	/**
	 * Chart types in the same way as the individual variables are set.
	 */
	public static final String[] TYPES = new String[] {"XY chart", "Category chart", "Pie chart"};

	/**
	 * The set of subtypes for X-Y charts.
	 */
	public enum SUBTYPE {
		/** Selects this subtype of x-y chart. */
		XY_SCATTER,
		/** Selects this subtype of x-y chart. */
		XY_LINE,
		/** Selects this subtype of x-y chart. */
		XY_STEP,
		/** Selects this subtype of x-y chart. */
		XY_AREA,
		/** Selects this subtype of x-y chart. */
		XY_STEP_AREA,
		/** Selects this subtype of x-y chart. */
		XY_POLAR,
		/**
		 *  Selects this subtype of x-y chart. In this case the
		 * x values should be the dates as Julian days.
		 */
		XY_TIME,
		/** Selects this subtype of category chart. */
		CATEGORY_BAR,
		/** Selects this subtype of category chart. */
		CATEGORY_STACKED_BAR,
		/** Selects this subtype of category chart. */
		CATEGORY_BAR_3D,
		/** Selects this subtype of category chart. */
		CATEGORY_STACKED_BAR_3D,
		/** Selects this subtype of category chart. */
		CATEGORY_WATER_FALL,
		/** Selects this subtype of category chart. */
		CATEGORY_AREA,
		/** Selects this subtype of category chart. */
		CATEGORY_STACKED_AREA,
		/** Selects this subtype of category chart. */
		CATEGORY_LINE,
		/** Selects this subtype of category chart. */
		CATEGORY_LINE_3D,
		/** Selects this subtype of pie chart. */
		PIE_DEFAULT,
		/** Selects this subtype of pie chart. */
		PIE_RING,
		/** Selects this subtype of pie chart. */
		PIE_3D
	};

	/**
	 * Chart x-y subtypes in the same way as the individual variables are set.
	 */
	private static final String[] SUBTYPES_XY = new String[] {"Scatter", "Line", "Step",
		"Area", "Step area", "Polar", "Time"};

	/**
	 * Chart category subtypes in the same way as the individual variables are set.
	 */
	private static final String[] SUBTYPES_CATEGORY = new String[] {"Bar", "Stacked bar", "Bar 3d",
		"Stacked bar 3d", "Water fall", "Area", "Stacked area", "Line", "Line 3d"};

	/**
	 * Chart pie subtypes in the same way as the individual variables are set.
	 */
	private static final String[] SUBTYPES_PIE = new String[] {"Normal", "Ring", "3d"};

	/**
	 * Chart subtypes in the same way as the individual variables are set.
	 */
	public static final String[][] SUBTYPES = new String[][] {ChartElement.SUBTYPES_XY, ChartElement.SUBTYPES_CATEGORY,
		ChartElement.SUBTYPES_PIE};

	/**
	 * The set of tick labels types.
	 */
	public enum TICK_LABELS {
		/** Constant ID for regular values in labels. Example: 10 000. */
		REGULAR_VALUES,
		/** Constant ID for exponential values in labels. Example: 1e4. */
		EXPONENTIAL_VALUES,
		/** Constant ID for log10 values in labels. Example: 10^4. */
		LOGARITHM_VALUES
	};

	/**
	 * Returns a simple chart without using JFreeChart of the input chart object.
	 * Only XY scatter (and time) charts are currently supported.
	 * @param chart Chart object.
	 * @return The chart as an image.
	 * @throws JPARSECException If an error occurs.
	 */
	public static BufferedImage getSimpleChart(ChartElement chart) throws JPARSECException {
		if (chart.chartType != TYPE.XY_CHART || (chart.subType != SUBTYPE.XY_SCATTER
				&& chart.subType != SUBTYPE.XY_TIME))
			throw new JPARSECException("Unsupported chart type.");

		int w = chart.imageWidth, h = chart.imageHeight;
		Graphics g = new AWTGraphics(w, h, false, false);
		g.setColor(Color.WHITE.getRGB(), true);
		g.fillRect(0, 0, w, h);

		double xmax = chart.getxMax(), xmin = chart.getxMin();
		double ymax = chart.getyMax(), ymin = chart.getyMin();
		float radiusX = (float) ((xmax - xmin)*0.6f);
		float radiusY = (float) ((ymax - ymin)*0.6f);
		float midX = (float) ((xmax + xmin)*0.5f);
		float midY = (float) ((ymax + ymin)*0.5f);
		int cxmin = (int) Math.floor(xmin - radiusX * 0.);
		int cymin = (int) Math.floor(ymin - radiusY * 0.);
		int cxmax = (int) Math.floor(xmax + radiusX * 0.);
		int cymax = (int) Math.floor(ymax + radiusY * 0.);

		if (w < 280) g.setFont(Graphics.FONT.getDerivedFont(g.getFont(), g.getFont().getSize()-2));
		if (w >= 280 && w < 380) g.setFont(Graphics.FONT.getDerivedFont(g.getFont(), g.getFont().getSize()-1));
		int offy = g.getFont().getSize();
		float scaleX = -(0.7f * w) / (2f * radiusX);
		float scaleY = -(0.7f * h) / (2f * radiusY);
		float x0 = w/2+offy*2, y0 = h/2-offy;
		int r = 3;
		g.setColor(Color.BLACK.getRGB(), true);

		// Draw axes
		int b = 0;
		String lx = chart.xLabel, ly = chart.yLabel;
		g.drawLine(x0 + scaleX * radiusX - b, y0 - scaleY * radiusY + b, x0 - scaleX * radiusX + b, y0 - scaleY * radiusY + b, false);
		g.drawLine(x0 + scaleX * radiusX - b, y0 - scaleY * radiusY + b, x0 + scaleX * radiusX - b, y0 + scaleY * radiusY - b, false);
		int nlab = 5;
		int lstep = (int) (0.5 + (2.0 * radiusX) / (nlab*2));
		if (lstep>5) {
			lstep = (int) (0.5 + (lstep / 5.0)) * 5;
			nlab = (int) (0.5 + radiusX/lstep) + 1;
		}
		b = 20;
		g.drawRotatedString(ly, x0 + scaleX * radiusX - b - offy*3, y0 + offy*2, (float) Constant.PI_OVER_TWO);
		b = 10;
		g.drawString(lx, x0 - g.getStringWidth(lx)/2, y0 - scaleY * radiusY + b + offy*3);
		for (int l=-nlab; l<=nlab; l=l+3) {
			float px = (int) (cxmin + (l+nlab)*(cxmax-cxmin)/(2.0*nlab));
			String label = Functions.formatValue(px, 1);
			px = (float) Double.parseDouble(label);
			if (chart.subType == SUBTYPE.XY_TIME) {
				AstroDate astro = new AstroDate(px);
				double px2 = (float) (astro.getYear() + (astro.getMonth()-1.0+ astro.getDay()/31.0)/12.0);
				label = Functions.formatValue(px2, 1);
			}
			g.drawLine(x0 - scaleX * (px - midX), y0 - scaleY * radiusY + b, x0 - scaleX * (px - midX), y0 - scaleY * radiusY, false);
			float offx = g.getStringWidth(label)/2;
			g.drawString(label, x0 - scaleX * (px - midX) - offx, y0 - scaleY * radiusY + b + offy+offy/2);

			px = (float) (cymin + (l+nlab)*(cymax-cymin)/(2.0*nlab));
			label = Functions.formatValue(px, 1);
			px = (float) Double.parseDouble(label);
			offx = g.getStringWidth(label)/2;
			g.drawLine(x0 + scaleX * radiusX - b, y0 + scaleY * (px - midY), x0 + scaleX * radiusX, y0 + scaleY * (px - midY), false);
			g.drawString(label, x0 + scaleX * radiusX - b - offx*2 - offy/2, y0 + scaleY * (px - midY) + offy/2);
		}
		g.setClip((int) (x0 + scaleX * radiusX - b), 0, (int) (x0 - scaleX * radiusX + b), (int) (y0 - scaleY * radiusY + b));

		if (chart.backgroundImage != null && chart.showBackgroundImage) {
			Picture back = new Picture(Picture.toBufferedImage(chart.backgroundImage));
			int backScaleW = (int) (1 + Math.abs(scaleX) * radiusX * 2);
			int backScaleH = (int) (1 + Math.abs(scaleY) * radiusY * 2);
			back.getScaledInstance(backScaleW, backScaleH, true);
			back = new Picture(Picture.copyWithTransparency(back.getImage()));
			back.setAlphaChannel(190, false);
			int px = (int) (x0 + scaleX * radiusX);
			int py = (int) (y0 + scaleY * radiusY);
			g.drawImage(back.getImage(), px, py);
		}

		int leyendIndex = 1;
		for (int s=0; s<chart.series.length; s++) {
			if (!chart.series[s].enable) continue;
			if (chart.series[s].color instanceof Color) g.setColor(((Color)chart.series[s].color).getRGB(), true);
			double x[] = DataSet.toDoubleValues(chart.series[s].xValues);
			double y[] = DataSet.toDoubleValues(chart.series[s].yValues);
			r = (chart.series[s].shapeSize*2)/3;
			float oldpx = -1, oldpy = -1;
			g.setStroke(chart.series[s].stroke);

			if (chart.series[s].showLegend) {
				leyendIndex ++;
				float tw = g.getStringWidth(chart.series[s].legend);
				g.drawString(chart.series[s].legend, x0 - tw/2, w*0.05f+3+offy*leyendIndex);
			}
			for (int i=0; i<x.length; i++) {
				float newpx = x0 - scaleX * ((float) x[i] - midX);
				float newpy = y0 + scaleY * ((float) y[i] - midY);
				if (chart.series[s].showShapes)
					g.fillOval(newpx-r, newpy-r, 2*r, 2*r, false);
				if (chart.series[s].showLines && i > 0)
					g.drawLine(oldpx, oldpy, newpx, newpy, false);
				oldpx = newpx;
				oldpy = newpy;
			}
		}

		g.setClip(0, 0, w, h);
		g.setFont(Graphics.FONT.getDerivedFont(g.getFont(), g.getFont().getSize()+2));
		offy = g.getFont().getSize();
		String title = chart.title;
		float tw = g.getStringWidth(title);
		g.setColor(Color.BLACK.getRGB(), true);
		if (tw > w) {
			int p = -1, dmin = -1;
			for (int i=0; i<title.length(); i++) {
				if (title.substring(i, i+1).equals(" ")) {
					int d = Math.abs(title.length()/2-i);
					if (d < dmin || dmin == -1) {
						dmin = d;
						p = i;
					}
				}
			}
			String l1 = title.substring(0, p).trim();
			String l2 = title.substring(p).trim();
			tw = g.getStringWidth(l1);
			g.drawString(l1, x0 - tw/2, w*0.05f);
			tw = g.getStringWidth(l2);
			g.setColor(Color.BLACK.getRGB(), true);
			g.drawString(l2, x0 - tw/2, w*0.05f+offy);
		} else {
			g.drawString(title, x0 - tw/2, w*0.05f);
		}

		return (BufferedImage) g.getRendering();
	}
	
	/**
	 * Prepares a given image to be resized and cropped properly to use it as 
	 * background image for the resulting chart within the data area. The resulting
	 * image is set as the background image field of the chart object, replaced the
	 * original one.
	 * @param cchart The chart object including the image as background.
	 * @param minX Minimum value of x in physical units for the limit of the background image.
	 * @param minY Minimum value of y in physical units for the limit of the background image.
	 * @param maxX Maximum value of x in physical units for the limit of the background image.
	 * @param maxY Maximum value of y in physical units for the limit of the background image.
	 * @throws JPARSECException If an error occurs.
	 */
	public static void prepareBackgroundImage(ChartElement cchart,
			double minX, double minY, double maxX, double maxY) throws JPARSECException {
		BufferedImage bi = Picture.toBufferedImage(cchart.backgroundImage);
		Picture picb = new Picture(bi);
		picb.flip(false, true);
		bi = picb.getImage();
		CreateChart ch = new CreateChart(cchart);
		
		double[] limX = ch.getChartLimitsX(), limY = ch.getChartLimitsY();
		double lx = maxX - minX, ly = maxY - minY;;
		double sx = (bi.getWidth()-1.0) / lx;
		double sy = (bi.getHeight()-1.0) / ly;
		int px0 = (int) Math.floor(0.5 + sx * (limX[0] - minX));
		int pxf = (int) Math.floor(0.5 + sx * (limX[1] - minX));
		int py0 = (int) Math.floor(0.5 + sy * (limY[0] - minY));
		int pyf = (int) Math.floor(0.5 + sy * (limY[1] - minY));
		
		if (px0 < 0) {
			BufferedImage newbi = new BufferedImage(bi.getWidth() - px0, bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics2D ng = newbi.createGraphics();
			ng.setColor(new Color(0, 0, 0, 0));
			ng.fillRect(0, 0, newbi.getWidth(), newbi.getHeight());
			ng.drawImage(bi, -px0, 0, null);
			pxf -= px0;
			px0 = 0;
			bi = newbi;
		} else {
			if (px0 > 0) {
				bi = bi.getSubimage(px0, 0, bi.getWidth()-px0, bi.getHeight());
				pxf -= px0;
				px0 = 0;
			}
		}
		if (py0 < 0) {
			BufferedImage newbi = new BufferedImage(bi.getWidth(), bi.getHeight() - py0, BufferedImage.TYPE_INT_ARGB);
			Graphics2D ng = newbi.createGraphics();
			ng.setColor(new Color(0, 0, 0, 0));
			ng.fillRect(0, 0, newbi.getWidth(), newbi.getHeight());
			ng.drawImage(bi, 0, -py0, null);
			pyf -= py0;
			py0 = 0;
			bi = newbi;
		} else {
			if (py0 > 0) {
				bi = bi.getSubimage(0, py0, bi.getWidth(), bi.getHeight()-py0);
				pyf -= py0;
				py0 = 0;
			}
		}
		if (pxf < bi.getWidth()) {
			bi = bi.getSubimage(0, 0, pxf, bi.getHeight());
		} else {
			if (pxf > bi.getWidth()) {
				BufferedImage newbi = new BufferedImage(pxf, bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
				Graphics2D ng = newbi.createGraphics();
				ng.setColor(new Color(0, 0, 0, 0));
				ng.fillRect(0, 0, newbi.getWidth(), newbi.getHeight());
				ng.drawImage(bi, 0, 0, null);
				bi = newbi;
			}					
		}
		if (pyf < bi.getHeight()) {
			bi = bi.getSubimage(0, 0, bi.getWidth(), pyf);
		} else {
			if (pyf > bi.getHeight()) {
				BufferedImage newbi = new BufferedImage(bi.getWidth(), pyf, BufferedImage.TYPE_INT_ARGB);
				Graphics2D ng = newbi.createGraphics();
				ng.setColor(new Color(0, 0, 0, 0));
				ng.fillRect(0, 0, newbi.getWidth(), newbi.getHeight());
				ng.drawImage(bi, 0, 0, null);
				bi = newbi;
			}					
		}
		
		picb = new Picture(bi);
		picb.flip(false, true);
		bi = picb.getImage();
		cchart.backgroundImage = bi;
	}
}
